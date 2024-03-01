package com.alfred.ai.mixin;

import com.alfred.ai.MCAIConfig;
import com.alfred.ai.MCAIMod;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.advancement.AdvancementEntry;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(PlayerAdvancementTracker.class)
public abstract class AdvancementMixin {
    @Shadow private ServerPlayerEntity owner; // allows getting player data

    @Inject(method = "grantCriterion", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancement/PlayerAdvancementTracker;onStatusUpdate(Lnet/minecraft/advancement/AdvancementEntry;)V"))
    private void sendAdvancementMessage(AdvancementEntry advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        if (advancement.value().display().isPresent() && !MCAIMod.onServer && (advancement.value().display().get().shouldAnnounceToChat() || (advancement.id().getPath().startsWith("recipes") && !MCAIMod.CONFIG.general.disableRecipeResponses) && !MCAIMod.CONFIG.general.disableAdvancementResponses)) {
            for (MCAIConfig.CharacterTuple tuple : MCAIMod.CONFIG.ais) {
                if (tuple.disabled)
                    continue;
                Float chance = null;
                for (Map.Entry<String, Float> advancementOverride : tuple.advancementResponseOverrideChances.entrySet()) {
                    if (advancementOverride.getKey().equals(advancement.id().toShortTranslationKey()) || advancementOverride.getKey().equals(advancement.id().toTranslationKey())) {
                        chance = advancementOverride.getValue();
                        break;
                    }
                }
                if (chance == null)
                    chance = tuple.advancementResponseChance;
                if (chance > Random.create().nextFloat())
                    MCAIMod.sendAIMessage(' ' + MCAIMod.CONFIG.general.advancementMessage
                            .replace("{player}", this.owner.getName().getString())
                            .replace("{advancement}", advancement.value().display().get().getTitle().getString())
                            .replace("{advancement_desc}", advancement.value().display().get().getDescription().getString()),
                            tuple, MCAIMod.CONFIG.general.systemName, MCAIMod.CONFIG.general.format, MCAIMod.CONFIG.general.replyFormat);
            }
        }
    }
}