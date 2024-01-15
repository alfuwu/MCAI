package com.alfred.ai;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

public class MCAIModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		ClientPlayNetworking.registerGlobalReceiver(MCAIMod.CHAT, (client, handler, buf, responseSender) -> {
			client.execute(() -> {
				sendChatMessage(buf.readString());
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(MCAIMod.QUERY_AI_GLOBAL, (client, handler, buf, responseSender) -> {
			// client-side stuff goes here
		});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {}); // register client-side commands
	}

	public static void sendChatMessage(String text) {
		MinecraftClient.getInstance().player.sendMessage(Text.literal(text));
		/*/
		MinecraftClient.getInstance().getMessageHandler().onGameMessage(Text.literal(text), false);
		MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal(text), null, MessageIndicator.notSecure());
		/*/
	}

	public static void onChatMessage(String text) {
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeString(text);

		ClientPlayNetworking.send(MCAIMod.QUERY_AI_GLOBAL, buf);
	}
}