package com.alfred.ai.mixin.client;

import com.alfred.ai.MCAIModClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatMessagesMixin {
    @Inject(method = "sendMessage", at = @At("HEAD"))
    private void sendMessage(String chatText, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) {
        //MCAIMod.onChatMessage(chatText, MinecraftClient.getInstance().player);
        MCAIModClient.onChatMessage(chatText);
    }
}