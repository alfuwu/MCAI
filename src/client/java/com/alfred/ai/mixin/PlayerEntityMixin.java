package com.alfred.ai.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.encryption.PublicPlayerSession;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Shadow
    public abstract Text getPlayerListName();

    @Inject(method = "onDeath", at = @At("HEAD"))
    public void onPlayerDeath(DamageSource damageSource, CallbackInfo ci) {
        System.out.println(this.getPlayerListName());
        //System.out.println(damageSource.getDeathMessage(this));
    }
}