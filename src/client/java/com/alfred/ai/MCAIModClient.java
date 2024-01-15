package com.alfred.ai;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;

import static com.alfred.ai.MCAIMod.modServer;

public class MCAIModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		ClientPlayNetworking.registerGlobalReceiver(MCAIMod.CHAT, (client, handler, buf, responseSender) -> {
			client.execute(() -> {
				sendChatMessage(buf.readString());
			});
		});
		ServerLifecycleEvents.SERVER_STARTED.register((server -> {
			modServer = modServer == null ? server : modServer;
		}));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {}); // register client-side commands
	}

	public static void sendChatMessage(String text) {
		MinecraftClient.getInstance().player.sendMessage(Text.literal(text));
		//MinecraftClient.getInstance().getMessageHandler().onGameMessage(Text.literal(text), false);
		//MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal(text), null, MessageIndicator.notSecure());

	}
}