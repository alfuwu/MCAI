package com.alfred.ai.mixin.client;

import com.alfred.ai.MCAIMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Mixin(ChatScreen.class)
public class ChatMessagesMixin {
    @Inject(at = @At("HEAD"), method = "sendMessage")
    private void sendMessage(String chatText, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) throws IOException {
        MCAIMod.onChatMessage(chatText, MinecraftClient.getInstance().player);
    }
}