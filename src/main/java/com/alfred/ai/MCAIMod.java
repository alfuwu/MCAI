package com.alfred.ai;

import com.fasterxml.jackson.databind.JsonNode;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

public class MCAIMod implements ModInitializer {
	public static final String MOD_ID = "mcai";
	public static final Logger LOGGER = LoggerFactory.getLogger("MCAI");
	public static JavaCAI CHARACTER_AI;
	public static final Identifier ON_SERVER_PACKET_ID = new Identifier(MOD_ID, "is_on_server_question_mark");
	public static MCAIConfig CONFIG;
	public static Map<MCAIConfig.CharacterTuple, ZonedDateTime> lastTalkedTo = new HashMap<>();

	@Override
	public void onInitialize() {
		// Register the CONFIG
		CONFIG = AutoConfig.register(MCAIConfig.class, GsonConfigSerializer::new).getConfig();
		// Create a C.AI instance
		CHARACTER_AI = new JavaCAI(CONFIG.general.authorization);
		Random random = new Random();

		ServerPlayNetworking.registerGlobalReceiver(ON_SERVER_PACKET_ID, (server, player, handler, buf, responseSender) -> {
			ServerPlayNetworking.send(player, ON_SERVER_PACKET_ID, PacketByteBufs.empty()); // echo
		});

		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			String text = message.getSignedContent();
			for (MCAIConfig.CharacterTuple tuple : CONFIG.ais) {
				if (tuple.disabled) // ignore disabled AIs
					continue;
				List<String> list = new java.util.ArrayList<>(Arrays.stream(tuple.aliases).toList());
				list.add(0, tuple.name);
				String[] arr = list.toArray(String[]::new);
				for (String name : arr) {
					if (text.toLowerCase().contains(String.format("@%s", name.toLowerCase())) || (tuple.randomResponseChance > random.nextFloat() && !CONFIG.general.disableRandomResponses) || (!CONFIG.general.disableEveryonePing && (text.toLowerCase().contains("@everyone ") || text.toLowerCase().contains("@ai ")))) {
						if (CONFIG.general.authorization.strip().equals("")) {
							sender.sendMessage(Text.translatable("mcai.errors.no_authorization_token").withColor(0xFF1111));
							return;
						}
						if (text.toLowerCase().startsWith(String.format("@%s", name.toLowerCase())))
							text = text.substring(name.length() + 1); // chop off starting ping
						sendAIMessage(
								text, tuple, sender.getName().getString(),
								CONFIG.general.format, CONFIG.general.replyFormat, sender.getServer());
					}
				}
			}
		});
		CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> MCAICommands.register(dispatcher)));
		ServerTickEvents.START_SERVER_TICK.register((server -> {
			if (!CONFIG.general.disableRandomTalking) {
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
								tuple, CONFIG.general.systemName, CONFIG.general.format, CONFIG.general.replyFormat, server);
					}
				}
			}
		}));
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
		setLastCommunicatedWith(tuple, ZonedDateTime.now());
	}

	public static void setLastCommunicatedWith(MCAIConfig.CharacterTuple tuple, ZonedDateTime time) {
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

	public static void sendPrivateMessage(String message, ServerPlayerEntity player) {
		player.sendMessage(Text.literal(message));
	}

	public static void sendGlobalMessage(String text, MinecraftServer server) {
		if (server == null)
			return;
		sendGlobalMessage(text, server.getPlayerManager().getPlayerList());
	}

	public static void sendGlobalMessage(String text, List<ServerPlayerEntity> players) {
		players.forEach(player -> sendPrivateMessage(text, player));
	}

	public static void sendAIMessage(String text, MCAIConfig.CharacterTuple tuple, String name, String format, String replyFormat, MinecraftServer server) {
		Thread thread = new Thread(null, () -> {
			try {
				JsonNode chat = (tuple.historyId == null || tuple.historyId.strip().equals("")) ? CHARACTER_AI.chat.newChat(tuple.id) : CHARACTER_AI.chat.getChat(tuple.id);
				String historyId = chat.get("external_id").asText();
				if (!tuple.historyId.equals(historyId)) {
					tuple.historyId = historyId;
					MCAIConfig.save();
				}
				String tgt = CHARACTER_AI.chat.getTgt(tuple.id);
				JsonNode reply = CHARACTER_AI.chat.sendMessage(historyId,
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
				sendGlobalMessage(replyText, server);
			} catch (IOException ignored) { }
		}, "HTTP thread");
		thread.start();
	}
}