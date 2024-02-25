package com.alfred.ai.mixin;

import com.alfred.ai.MCAIConfig;
import com.alfred.ai.MCAIMod;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void onDisconnected(Text reason, CallbackInfo ci) {
        if (!MCAIMod.CONFIG.general.disableLeaveResponses && this.player.server.getPlayerManager().getPlayerList().size() - 1 > 0) { // don't respond when nobody is left in the server
            for (MCAIConfig.CharacterTuple tuple : MCAIMod.CONFIG.ais) {
                if (tuple.disabled)
                    continue;
                if (tuple.leaveResponseChance > Random.create().nextFloat())
                    MCAIMod.sendAIMessage(' ' + MCAIMod.CONFIG.general.leaveMessage.replace("{player}", this.player.getName().getString()), tuple, MCAIMod.CONFIG.general.systemName, MCAIMod.CONFIG.general.format, MCAIMod.CONFIG.general.replyFormat, this.player.server);
            }
        }
    }
}
