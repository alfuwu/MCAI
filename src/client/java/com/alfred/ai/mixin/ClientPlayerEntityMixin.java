package com.alfred.ai.mixin;

import com.alfred.ai.MCAIConfig;
import com.alfred.ai.MCAIMod;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        if (!MCAIMod.CONFIG.general.disableDeathMessageResponses && !MCAIMod.onServer) {
            for (MCAIConfig.CharacterTuple tuple : MCAIMod.CONFIG.ais) {
                if (tuple.disabled)
                    continue;
                if (tuple.deathMessageResponseChance < this.random.nextFloat())
                    MCAIMod.sendAIMessage(' ' + damageSource.getDeathMessage(this).getString(), tuple, MCAIMod.CONFIG.general.systemName, MCAIMod.CONFIG.general.format, MCAIMod.CONFIG.general.replyFormat);
            }
        }
        super.onDeath(damageSource);
    }
}