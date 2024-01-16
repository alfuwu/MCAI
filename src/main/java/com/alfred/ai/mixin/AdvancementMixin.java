package com.alfred.ai.mixin;

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
public class AdvancementMixin {
    @Shadow
    private ServerPlayerEntity owner;

    @Unique
    public boolean obtain(AdvancementProgress advancementProgress, String name) {
        CriterionProgress criterionProgress = advancementProgress.getCriterionProgress(name);
        //criterionProgress.obtain();
        return criterionProgress != null && !criterionProgress.isObtained();
    }

    @Inject(method = "grantCriterion", at = @At("HEAD"))
    private void beforeGrantCriterion(AdvancementEntry advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        AdvancementProgress advancementProgress = ((PlayerAdvancementTracker)(Object)this).getProgress(advancement);
        boolean wasAlreadyCompleted = advancementProgress.isDone();
        if (obtain(advancementProgress, criterionName)) {
            if (!wasAlreadyCompleted) {
                System.out.println(String.format("%s advancement obtained!", advancement.toString()));
            }
        }
    }
}
