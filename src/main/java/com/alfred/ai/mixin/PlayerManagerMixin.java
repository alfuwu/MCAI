package com.alfred.ai.mixin;

import com.alfred.ai.MCAIConfig;
import com.alfred.ai.MCAIMod;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Shadow @Final private MinecraftServer server;

    @Inject(method = "onPlayerConnect", at = @At("HEAD"))
    private void sendJoinedMessage(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        if (!MCAIMod.CONFIG.general.disableJoinResponses) {
            for (MCAIConfig.CharacterTuple tuple : MCAIMod.CONFIG.ais) {
                if (tuple.disabled)
                    continue;
                if (tuple.joinResponseChance > Random.create().nextFloat())
                    MCAIMod.sendAIMessage(MCAIMod.CONFIG.general.joinMessage.replace("{player}", player.getName().getString()), tuple, MCAIMod.CONFIG.general.systemName, MCAIMod.CONFIG.general.format, MCAIMod.CONFIG.general.replyFormat, this.server);
            }
        }
    }
}
