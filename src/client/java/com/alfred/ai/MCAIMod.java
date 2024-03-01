package com.alfred.ai;

import com.fasterxml.jackson.databind.JsonNode;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

public class MCAIMod implements ClientModInitializer {
	public static final String MOD_ID = "mcai";
	public static final Logger LOGGER = LoggerFactory.getLogger("MCAI");
	public static JavaCAI CHARACTER_AI;
	public static boolean onServer = false;
	public static final Identifier ON_SERVER_PACKET_ID = new Identifier(MOD_ID, "is_on_server_question_mark");
	public static MCAIConfig CONFIG;
	public static Map<MCAIConfig.CharacterTuple, ZonedDateTime> lastTalkedTo = new HashMap<>();

	@Override
	public void onInitializeClient() {
		// Register the config file
		CONFIG = AutoConfig.register(MCAIConfig.class, GsonConfigSerializer::new).getConfig();
		// Create a C.AI instance
		CHARACTER_AI = new JavaCAI(CONFIG.general.authorization);
		Random random = new Random();

		// if the server echos the empty packet through the specified networking ID, assume MCAI is on the server
		// if MCAI is on the server, the client-sided version will do nothing (if the config file says so)
		ClientPlayNetworking.registerGlobalReceiver(ON_SERVER_PACKET_ID, (client, handler, buf, responseSender) -> onServer = buf.equals(PacketByteBufs.empty()) && !CONFIG.general.ignoreOnServer);

		// send a packet to the server when client joins a server
		ClientPlayConnectionEvents.JOIN.register((networkHandler, sender, client) -> {
			onServer = false; // reset onServer
			if (!CONFIG.general.ignoreOnServer)
				ClientPlayNetworking.send(ON_SERVER_PACKET_ID, PacketByteBufs.empty());
			if (!MCAIMod.CONFIG.general.disableJoinResponses && !onServer && client.player != null) {
				for (MCAIConfig.CharacterTuple tuple : MCAIMod.CONFIG.ais) {
					if (tuple.disabled)
						continue;
					if (tuple.joinResponseChance > random.nextFloat())
						MCAIMod.sendAIMessage(MCAIMod.CONFIG.general.joinMessage.replace("{player}", client.player.getName().getString()), tuple, MCAIMod.CONFIG.general.systemName, MCAIMod.CONFIG.general.format, MCAIMod.CONFIG.general.replyFormat);
				}
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((networkHandler, client) -> {
			onServer = false;
			if (!MCAIMod.CONFIG.general.disableLeaveResponses && client.player != null) {
				for (MCAIConfig.CharacterTuple tuple : MCAIMod.CONFIG.ais) {
					if (tuple.disabled)
						continue;
					if (tuple.leaveResponseChance > net.minecraft.util.math.random.Random.create().nextFloat())
						MCAIMod.sendAIMessage(' ' + MCAIMod.CONFIG.general.leaveMessage.replace("{player}", client.player.getName().getString()), tuple, MCAIMod.CONFIG.general.systemName, MCAIMod.CONFIG.general.format, MCAIMod.CONFIG.general.replyFormat);
				}
			}
		});

		ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> MCAICommands.register(dispatcher)));
		ClientTickEvents.START_WORLD_TICK.register(clientWorld -> {
			if (!CONFIG.general.disableRandomTalking && (!onServer) && !MinecraftClient.getInstance().isPaused()) {
				Double globalLastTalkedWith = null;

				for (MCAIConfig.CharacterTuple tuple : CONFIG.ais) {
					double lastTalkedWith = getLastCommunicatedWith(ZonedDateTime.now(), tuple);
					if (globalLastTalkedWith == null || lastTalkedWith < globalLastTalkedWith)
						globalLastTalkedWith = lastTalkedWith;
				}
				for (MCAIConfig.CharacterTuple tuple : CONFIG.ais) {
					if (globalLastTalkedWith >= tuple.minimumSecondsBeforeRandomTalking && tuple.randomTalkChance > random.nextFloat()) {
						sendAIMessage(
								' ' + CONFIG.general.randomTalkMessage.replace("{time}", generalizeNumberToTime(tuple.talkIntervalSpecificity, globalLastTalkedWith)),
								tuple, CONFIG.general.systemName, CONFIG.general.format, CONFIG.general.replyFormat);
					}
				}
			}
		});
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

	public static void sendMessage(String message) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null)
			client.player.sendMessage(Text.literal(message));
	}

	public static void sendAIMessage(String text, MCAIConfig.CharacterTuple tuple, String name, String format, String replyFormat) {
		Thread thread = new Thread(null, () -> {
			try {
				JsonNode chat = (tuple.historyId == null || tuple.historyId.strip().equals("")) ? CHARACTER_AI.chat.newChat(tuple.id) : CHARACTER_AI.chat.getChat(tuple.id);
				String historyID = chat.get("external_id").asText();
				if (!tuple.historyId.equals(historyID)) {
					tuple.historyId = historyID;
					MCAIConfig.save();
				}
				String tgt = CHARACTER_AI.chat.getTgt(tuple.id);
				JsonNode reply = CHARACTER_AI.chat.sendMessage(
						historyID,
						format
								.replace("{user}", name)
								.replace("{message}", text),
						tgt);
				setLastCommunicatedWith(tuple);
				String replyText = replyFormat
						.replace("{char}", reply.get("src_char").get("participant").get("name").asText())
						.replace("{message}", reply.get("replies").get(0).get("text").asText())
						.replace("\n\n", "\n");
				LOGGER.info(replyText);
				sendMessage(replyText);
			} catch (IOException ignored) { }
		}, "HTTP thread");
		thread.start();
	}
}