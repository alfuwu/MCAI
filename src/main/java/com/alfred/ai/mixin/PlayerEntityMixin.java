package com.alfred.ai.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Shadow
    public abstract GameProfile getGameProfile();

    @Inject(method = "onDeath", at = @At("HEAD"))
    public void onPlayerDeath(DamageSource damageSource, CallbackInfo ci) {
        System.out.println(getGameProfile().getName() + " died!");
    }
}