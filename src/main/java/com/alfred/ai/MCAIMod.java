package com.alfred.ai;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.buffer.Unpooled;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.alfred.ai.MCAIMod.characterAI;

public class MCAIMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("mcai");
	public static final Identifier CHAT = new Identifier("mcai", "chat");
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
		// Create a C.AI instance
		characterAI = new JavaCAI(MCAIConfig.getInstance().General.authorization);

		// Register the network channel
		ServerPlayNetworking.registerGlobalReceiver(CHAT, (server, player, handler, buf, responseSender) -> {
			// Handle received message
		});

		ServerLifecycleEvents.SERVER_STARTED.register((server -> modServer = modServer == null ? server : modServer)); // set modServer to the server on startup
		ServerTickEvents.START_SERVER_TICK.register((server -> modServer = modServer == null ? server : modServer));
		CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> MCAICommands.register(dispatcher)));

	}

	public static void sendPrivateMessage(String message, ServerPlayerEntity player) {
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeString(message);

		ServerPlayNetworking.send(player, CHAT, buf);
	}

	public static void sendGlobalMessage(String text) {
		modServer.getPlayerManager().getPlayerList().forEach(player -> sendPrivateMessage(text, player));
	}

	public static void sendGlobalMessage(String text, MinecraftServer server) {
		try {
			server.getPlayerManager().getPlayerList().forEach(player -> sendPrivateMessage(text, player));
		} catch (Exception e) {
			sendGlobalMessage(text);
		}
	}

	public static void sendGlobalMessage(String text, List<ServerPlayerEntity> players) {
		try {
			players.forEach(player -> sendPrivateMessage(text, player));
		} catch (Exception e) {
			sendGlobalMessage(text);
		}
	}

	/*/ public static ServerPlayerEntity convertClientPlayerEntityToServerPlayerEntity(PlayerEntity clientPlayerEntity) {
		if (clientPlayerEntity.getClass() != ServerPlayerEntity.class && modServer != null) {
			clientPlayerEntity = modServer.getPlayerManager().getPlayer(clientPlayerEntity.getUuid());
		}
		return (ServerPlayerEntity) clientPlayerEntity;
	} /*/

	public static void onChatMessage(String text, PlayerEntity player) throws IOException {
		/*/ if (player.getClass() != ServerPlayerEntity.class && modServer != null) {
			player = modServer.getPlayerManager().getPlayer(player.getUuid());
		} /*/
		//System.out.println(String.format("Received message \"%s\" from %s", text, player.getName().getLiteralString()));
		MCAIConfig config = MCAIConfig.getInstance();
		List<MCAIConfig.CharacterTuple> characters = config.AIs;
		for (MCAIConfig.CharacterTuple tuple : characters) {
			if (tuple.disabled)
				continue;
			String[] arr = new String[tuple.aliases.length + 1];
			System.arraycopy(tuple.aliases, 0, arr, 0, tuple.aliases.length);
			arr[arr.length - 1] = tuple.name;
			for (String name : arr) {
				if (text.toLowerCase().contains(String.format("@%s", name.toLowerCase()))) {
					if (text.toLowerCase().startsWith(String.format("@%s", name.toLowerCase())))
						text = text.substring(String.format("@%s", name).length()); // chop off starting ping
					Runnable task = new AIResponse(
							tuple,
							text,
							player.getName().getLiteralString(),
							config.General.format,
							config.General.replyFormat);
					Thread thread = new Thread(task);
					thread.start();
					break;
				}
			}
		}
		//sendGlobalMessage("<Echo> " + text);
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
			MCAIMod.sendGlobalMessage(replyFormat
					.replace("{char}", reply.get("src_char").get("participant").get("name").asText())
					.replace("{message}", reply.get("replies").get(0).get("text").asText()));
		} catch (IOException ignored) { }
	}
}