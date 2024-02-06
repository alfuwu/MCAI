package com.alfred.ai;

import com.fasterxml.jackson.databind.JsonNode;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.alfred.ai.MCAIMod.characterAI;
import static com.alfred.ai.MCAIMod.sendMessage;

public class MCAIMod implements ClientModInitializer {
	public static final String MODID = "mcai";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	public static JavaCAI characterAI;
	public static boolean onServer = false;
	public static final Identifier ON_SERVER_PACKET_ID = new Identifier(MODID, "is_on_server_question_mark");
	public static Map<MCAIConfig.CharacterTuple, ZonedDateTime> lastTalkedTo = new HashMap<>();

	@Override
	public void onInitializeClient() {
		// Register the config file
		AutoConfig.register(MCAIConfig.class, Toml4jConfigSerializer::new);
		MCAIConfig config = MCAIConfig.getInstance();
		// Create a C.AI instance
		characterAI = new JavaCAI(config.General.authorization);
		Random random = new Random();

		// if the server echos an empty packet through the specified networking ID, assume MCAI is on the server
		// if MCAI is on the server, the client-sided version will do nothing
		ClientPlayNetworking.registerGlobalReceiver(ON_SERVER_PACKET_ID, (client, handler, buf, responseSender) -> onServer = buf.equals(PacketByteBufs.empty()));

		// send a packet to the server when client joins a server
		ClientPlayConnectionEvents.JOIN.register((networkHandler, sender, client) -> {
			onServer = false; // reset onServer
			ClientPlayNetworking.send(ON_SERVER_PACKET_ID, PacketByteBufs.empty());
			if (onServer)
				System.out.println("MCAI IS ON SERVER");
		});

		ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> MCAICommands.register(dispatcher)));
		ClientTickEvents.START_WORLD_TICK.register(clientWorld -> {
			if (!config.General.disableRandomTalking && !onServer && !MinecraftClient.getInstance().isPaused()) {
				Double globalLastTalkedWith = null;

				for (MCAIConfig.CharacterTuple tuple : config.AIs) {
					double lastTalkedWith = getLastCommunicatedWith(ZonedDateTime.now(), tuple);
					if (globalLastTalkedWith == null || lastTalkedWith < globalLastTalkedWith)
						globalLastTalkedWith = lastTalkedWith;
				}
				for (MCAIConfig.CharacterTuple tuple : config.AIs) {
					//System.out.println(String.format("%s: %f %f %f", tuple.name, globalLastTalkedWith, tuple.randomTalkChance, random.nextFloat()));
					if (globalLastTalkedWith >= tuple.minimumSecondsBeforeRandomTalking && tuple.randomTalkChance > random.nextFloat()) {
						sendAIMessage(
								' ' + config.General.randomTalkMessage.replace("{time}", generalizeNumberToTime(tuple.talkIntervalSpecificity, globalLastTalkedWith)),
								tuple, config.General.systemName, config.General.format, config.General.replyFormat);
					}
				}
			}
		});
	}

	public static Tuple<Double> divmod(double num, double div) {
		double quotient = Math.floor(num / div);
		double remainder = num % div;
		return new Tuple(quotient, remainder);
	}


	public static ZonedDateTime getLastCommunicatedWith(MCAIConfig.CharacterTuple tuple) {
		return lastTalkedTo.computeIfAbsent(tuple, key -> null);
	}

	public static double getLastCommunicatedWith(ZonedDateTime time, MCAIConfig.CharacterTuple tuple) {
		if (getLastCommunicatedWith(tuple) == null)
			return -1; // can't return null because AAAAAAAAA
		else
			return Duration.between(getLastCommunicatedWith(tuple).toInstant(), time.toInstant()).toMillis() / 1000.0d;
	}

	public static void setLastCommunicatedWith(MCAIConfig.CharacterTuple tuple) {
		setLastCommunicatedWith(ZonedDateTime.now(), tuple);
	}

	public static void setLastCommunicatedWith(ZonedDateTime time, MCAIConfig.CharacterTuple tuple) {
		lastTalkedTo.put(tuple, time);
	}

	public static String generalizeNumberToTime(double value, double number) {
		if (value >= 1.0) {
			return formatDouble(number) + " seconds"; // add seconds part
		} else if (value <= 0.0) {
			return "some time";
		} else {
			double powerOf10 = Math.pow(10, (int) (-value * 10) + 1);
			return formatDouble(Math.round(number / powerOf10) * powerOf10) + " seconds";
		}
	}

	public static String formatDouble(double number) {
		return new DecimalFormat("#,###.######").format(number);
	}

	public static String oldFormatDouble(double number) {
		return String.format("%,f", number).substring(0, String.format("%,f", number).length() - 1) // remove last digit to fix precision
				.replaceAll("0*$", "") // strip trailing zeroes
				.replaceAll("\\.$", ""); // remove decimal point if number was an integer
	}

	public static void sendMessage(String message) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null)
			client.player.sendMessage(Text.literal(message));
	}
	public static void sendAIMessage(String text, MCAIConfig.CharacterTuple tuple, String name, String format, String replyFormat) {
		Runnable task = new AIResponse(tuple, text, name, format, replyFormat);
		Thread thread = new Thread(null, task, "HTTP thread");
		thread.start();
	}
}

class AIResponse implements Runnable {
	private final MCAIConfig.CharacterTuple tuple;
	public final String text;
	public final String playerName;
	public final String format;
	public final String replyFormat;

	public AIResponse(MCAIConfig.CharacterTuple tuple, String text, String playerName, String format, String replyFormat) {
		this.tuple = tuple;
		this.text = text;
		this.playerName = playerName == null ? "Anonymous" : playerName;
		this.format = format;
		this.replyFormat = replyFormat;
	}

	@Override
	public void run() {
		try {
			JsonNode chat = (tuple.historyID == null || tuple.historyID.strip().equals("")) ? characterAI.chat.newChat(tuple.ID) : characterAI.chat.getChat(tuple.ID);
			String historyID = chat.get("external_id").asText();
			if (!tuple.historyID.equals(historyID)) {
				tuple.historyID = historyID;
				MCAIConfig.save();
				MCAIConfig.load();
			}
			String tgt = characterAI.chat.getTgt(tuple.ID);
			JsonNode reply = characterAI.chat.sendMessage(
					historyID,
					format
							.replace("{user}", playerName)
							.replace("{message}", text),
					tgt);
			sendMessage(replyFormat
					.replace("{char}", reply.get("src_char").get("participant").get("name").asText())
					.replace("{message}", reply.get("replies").get(0).get("text").asText())
					.replace("\n\n", "\n"));
		} catch (IOException ignored) { }
	}
}