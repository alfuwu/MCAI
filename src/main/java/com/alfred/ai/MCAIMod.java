package com.alfred.ai;

import com.fasterxml.jackson.databind.JsonNode;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.util.*;

import static com.alfred.ai.MCAIMod.characterAI;
import static com.alfred.ai.MCAIMod.sendGlobalMessage;

public class MCAIMod implements ModInitializer {
	public static final String MODID = "mcai";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	public static JavaCAI characterAI;

	@Override
	public void onInitialize() {
		// Register the config file
		AutoConfig.register(MCAIConfig.class, Toml4jConfigSerializer::new);
		MCAIConfig config = MCAIConfig.getInstance();
		// Create a C.AI instance
		characterAI = new JavaCAI(config.General.authorization);
		Random random = new Random();

		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			String text = message.getSignedContent();
			for (MCAIConfig.CharacterTuple tuple : config.AIs) {
				if (tuple.disabled) // ignore disabled AIs
					continue;
				List<String> list = new java.util.ArrayList<>(Arrays.stream(tuple.aliases).toList());
				list.add(0, tuple.name);
				String[] arr = list.toArray(new String[] {});
				for (String name : arr) {
					if (text.toLowerCase().contains(String.format("@%s", name.toLowerCase())) || (tuple.randomTalkChance > random.nextFloat() && !config.General.disableRandomResponses)) {
						if (text.toLowerCase().startsWith(String.format("@%s", name.toLowerCase())))
							text = text.substring(name.length() + 1); // chop off starting ping
						sendAIMessage(
								text, tuple, sender != null ? sender.getName().getLiteralString() : "Anonymous",
								config.General.format, config.General.replyFormat, sender != null ? sender.getServer() : null);
						tuple.setLastCommunicatedWith();
						MCAIConfig.save();
						MCAIConfig.load();
						break;
					}
				}
			}
		});
		CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> MCAICommands.register(dispatcher)));
		ServerTickEvents.START_SERVER_TICK.register((server -> {
			if (!config.General.disableRandomTalking) {
				Double globalLastTalkedWith = null;

				for (MCAIConfig.CharacterTuple tuple : config.AIs) {
					double lastTalkedWith = tuple.getLastCommunicatedWith(ZonedDateTime.now());
					if (globalLastTalkedWith == null || lastTalkedWith < globalLastTalkedWith)
						globalLastTalkedWith = lastTalkedWith;
				}
				for (MCAIConfig.CharacterTuple tuple : config.AIs) {
					//System.out.println(String.format("%s: %f %f %f", tuple.name, globalLastTalkedWith, tuple.randomTalkChance, random.nextFloat()));
					if (globalLastTalkedWith >= tuple.minimumSecondsBeforeRandomTalking && tuple.randomTalkChance > random.nextFloat()) {
						sendAIMessage(
								' ' + config.General.randomTalkMessage.replace("{time}", generalizeNumberToTime(tuple.talkIntervalSpecificity, globalLastTalkedWith)),
								tuple, config.General.systemName, config.General.format, config.General.replyFormat, server);
					}
				}
			}
		}));
	}

	public static Tuple<Double> divmod(double num, double div) {
		double quotient = Math.floor(num / div);
		double remainder = num % div;
		return new Tuple(quotient, remainder);
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

	public static void sendPrivateMessage(String message, ServerPlayerEntity player) {
		player.sendMessage(Text.literal(message));
	}

	public static void sendGlobalMessage(String text, MinecraftServer server) {
		sendGlobalMessage(text, server.getPlayerManager().getPlayerList());
	}

	public static void sendGlobalMessage(String text, List<ServerPlayerEntity> players) {
		players.forEach(player -> sendPrivateMessage(text, player));
	}

	public static void sendAIMessage(String text, MCAIConfig.CharacterTuple tuple, String name, String format, String replyFormat, MinecraftServer server) {
		Runnable task = new AIResponse(tuple, text, name, format, replyFormat, server);
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
	public final MinecraftServer server;

	public AIResponse(MCAIConfig.CharacterTuple tuple, String text, String playerName, String format, String replyFormat, MinecraftServer server) {
		this.tuple = tuple;
		this.text = text;
		this.playerName = playerName == null ? "Anonymous" : playerName;
		this.format = format;
		this.replyFormat = replyFormat;
		this.server = server;
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
			sendGlobalMessage(replyFormat
					.replace("{char}", reply.get("src_char").get("participant").get("name").asText())
					.replace("{message}", reply.get("replies").get(0).get("text").asText())
					.replace("\n\n", "\n"), server);
		} catch (IOException ignored) { }
	}
}