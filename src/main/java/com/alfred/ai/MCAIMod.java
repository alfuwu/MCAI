package com.alfred.ai;

import com.fasterxml.jackson.databind.JsonNode;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.alfred.ai.MCAIMod.characterAI;
import static com.alfred.ai.MCAIMod.sendGlobalMessage;

public class MCAIMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final String MODID = "mcai";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	public static MinecraftServer modServer = null;
	public static JavaCAI characterAI;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		//JavaCAI.example();

		// Register the config file
		AutoConfig.register(MCAIConfig.class, Toml4jConfigSerializer::new);
		MCAIConfig config = MCAIConfig.getInstance();
		// Create a C.AI instance
		characterAI = new JavaCAI(config.General.authorization);

		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			String text = message.getSignedContent();
			for (MCAIConfig.CharacterTuple tuple : config.AIs) {
				if (tuple.disabled) // ignore disabled AIs
					continue;
				List<String> list = new java.util.ArrayList<>(Arrays.stream(tuple.aliases).toList());
				list.add(0, tuple.name);
				String[] arr = list.toArray(new String[] {});
				/*/
				System.out.println(Arrays.toString(arr));
				System.arraycopy(tuple.aliases, 0, arr, 0, tuple.aliases.length);
				/*/
				for (String name : arr) {
					if (text.toLowerCase().contains(String.format("@%s", name.toLowerCase()))) {
						if (text.toLowerCase().startsWith(String.format("@%s", name.toLowerCase())))
							text = text.substring(name.length() + 1); // chop off starting ping
						sendAIMessage(
								text, tuple, sender != null ? sender.getName().getLiteralString() : "Anonymous",
								config.General.format, config.General.replyFormat, sender != null ? sender.getServer() : modServer);
						break;
					}
				}
			}
		});

		ServerLifecycleEvents.SERVER_STARTED.register((server -> modServer = modServer == null ? server : modServer)); // set modServer to the server on startup
		CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> MCAICommands.register(dispatcher)));
		ServerMessageEvents.GAME_MESSAGE.register(((server, message, bool) -> {

		}));
		ServerTickEvents.START_SERVER_TICK.register((server -> {
			for (MCAIConfig.CharacterTuple tuple : config.AIs) {

			}
		}));
	}

	public static Tuple<Double> divmod(double num, double div) {
		double quotient = Math.floor(num / div);
		double remainder = num % div;
		return new Tuple(quotient, remainder);
	}

	public static Object generalizeNumber(double value, double number) {
		if (value == 1.0) {
			return number;
		} else if (value == 0.0) {
			return "some number";
		} else {
			double powerOf10 = Math.pow(10, (int) (-value * 10) + 1);
			return Math.round(number / powerOf10) * powerOf10;
		}
	}

	public static void sendPrivateMessage(String message, ServerPlayerEntity player) {
		player.sendMessage(Text.literal(message));
	}

	public static void sendGlobalMessage(String text, MinecraftServer server) {
		server.getPlayerManager().getPlayerList().forEach(player -> sendPrivateMessage(text, player));
	}

	public static void sendGlobalMessage(String text, List<ServerPlayerEntity> players) {
		players.forEach(player -> sendPrivateMessage(text, player));
	}

	public static void sendAIMessage(String text, MCAIConfig.CharacterTuple tuple, String name, String format, String replyFormat, MinecraftServer server) {
		Runnable task = new AIResponse(tuple, text, name, format, replyFormat, server);
		Thread thread = new Thread(null, task, "HTTP thread");
		thread.start();
	}

	/*/ public static ServerPlayerEntity convertClientPlayerEntityToServerPlayerEntity(PlayerEntity clientPlayerEntity) {
		if (clientPlayerEntity.getClass() != ServerPlayerEntity.class && modServer != null)
			clientPlayerEntity = modServer.getPlayerManager().getPlayer(clientPlayerEntity.getUuid());
		return (ServerPlayerEntity) clientPlayerEntity;
	} /*/
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
					.replace("{message}", reply.get("replies").get(0).get("text").asText()), server);
		} catch (IOException ignored) { }
	}
}