package com.alfred.ai.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.advancement.criterion.CriterionProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;

@Mixin(PlayerAdvancementTracker.class)
public abstract class AdvancementMixin {
    @Shadow public abstract AdvancementProgress getProgress(AdvancementEntry advancement);

    @Inject(method = "grantCriterion", at = @At("HEAD"))
    private void beforeGrantCriterion(AdvancementEntry advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        AdvancementProgress advancementProgress = getProgress(advancement);
        boolean wasAlreadyCompleted = advancementProgress.isDone();
        if (advancementProgress.obtain(criterionName)) {
            if (!wasAlreadyCompleted && advancementProgress.isDone()) {
                // execute advancement code here
                //advancement.id().getPath().startsWith("recipes"); // recipe advancements
            }
            advancementProgress.reset(criterionName); // reset so that actual grantCriterion code can run properly
        }
    }
}