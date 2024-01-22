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
    @Shadow
    private ServerPlayerEntity owner; // allows getting player data
    @Shadow
    public abstract AdvancementProgress getProgress(AdvancementEntry advancement);

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
        /*
        Advancement advancementClass = advancement.value();

        if (advancement != null && advancementClass.display() != null && advancementClass.display().shouldAnnounceToChat()) {
            .replace("%uuid%", owner.getUuid().toString())
            .replace("%uuid_dashless%", owner.getUuid().toString().replace("-", ""))
            .replace("%name%", FabricMessageUtils.formatPlayerName(owner))
            .replace("%randomUUID%", UUID.randomUUID().toString())
            .replace("%avatarURL%", avatarURL)
            .replace("%advName%", Formatting.strip(advancement.getDisplay().getTitle().getString()))
            .replace("%advDesc%", Formatting.strip(advancement.getDisplay().getDescription().getString()))
            .replace("%advNameURL%",
                URLEncoder.encode(Formatting.strip(advancement.getDisplay().getTitle().getString()),
                StandardCharsets.UTF_8))
            .replace("%advDescURL%",
                URLEncoder.encode(Formatting.strip(advancement.getDisplay().getDescription().getString()),
                StandardCharsets.UTF_8))
            .replace("%avatarURL%", avatarURL)

            .replace("%advName%",
                Formatting.strip(advancement
                    .getDisplay()
                    .getTitle()
                    .getString()))
            .replace("%advDesc%",
                Formatting.strip(advancement
                    .getDisplay()
                    .getDescription()
                    .getString()))
            .replace("\\n", "\n")
            .replace("%advNameURL%",
                URLEncoder.encode(Formatting.strip(advancement.getDisplay().getTitle().getString()),
                StandardCharsets.UTF_8))
            .replace("%advDescURL%",
                URLEncoder.encode(Formatting.strip(advancement.getDisplay().getDescription().getString()),
                StandardCharsets.UTF_8))
        }
        */
    }
}

/*
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayNetworkHandler.class)
public class NetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void onDisconnect(final Text textComponent, CallbackInfo ci) {
        if (textComponent.equals(Text.translatable("disconnect.timeout")))
            DiscordIntegrationMod.timeouts.add(this.player.getUuid());
    }

    @Inject(at = @At(value = "HEAD"), method = "onDisconnected")
    private void onPlayerLeave(Text reason, CallbackInfo info) {
        if (DiscordIntegrationMod.stopped) return; //Try to fix player leave messages after stop!
        if (LinkManager.isPlayerLinked(player.getUuid()) && LinkManager.getLink(null, player.getUuid()).settings.hideFromDiscord)
            return;
        final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", player.getUuid().toString()).replace("%uuid_dashless%", player.getUuid().toString().replace("-", "")).replace("%name%", player.getName().getString()).replace("%randomUUID%", UUID.randomUUID().toString());
        if (DiscordIntegration.INSTANCE != null && !DiscordIntegrationMod.timeouts.contains(player.getUuid())) {
            if (!Localization.instance().playerLeave.isBlank()) {
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.playerLeaveMessages.asEmbed) {
                    if (!Configuration.instance().embedMode.playerLeaveMessages.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerLeaveMessages.toEmbedJson(Configuration.instance().embedMode.playerLeaveMessages.customJSON
                                .replace("%uuid%", player.getUuid().toString())
                                .replace("%uuid_dashless%", player.getUuid().toString().replace("-", ""))
                                .replace("%name%", FabricMessageUtils.formatPlayerName(player))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(player.getUuid()).getRGB())
                        );
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                    } else {
                        EmbedBuilder b = Configuration.instance().embedMode.playerLeaveMessages.toEmbed();
                        b = b.setAuthor(FabricMessageUtils.formatPlayerName(player), null, avatarURL)
                                .setDescription(Localization.instance().playerLeave.replace("%player%", FabricMessageUtils.formatPlayerName(player)));
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                    }
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().playerLeave.replace("%player%", FabricMessageUtils.formatPlayerName(player)),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
            }
        } else if (DiscordIntegration.INSTANCE != null && DiscordIntegrationMod.timeouts.contains(player.getUuid())) {
            if (!Localization.instance().playerTimeout.isBlank()) {
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.playerLeaveMessages.asEmbed) {
                    EmbedBuilder b = Configuration.instance().embedMode.playerLeaveMessages.toEmbed();
                    b = b.setAuthor(FabricMessageUtils.formatPlayerName(player), null, avatarURL)
                            .setDescription(Localization.instance().playerTimeout.replace("%player%", FabricMessageUtils.formatPlayerName(player)));
                    DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().playerTimeout.replace("%player%", FabricMessageUtils.formatPlayerName(player)),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
            }
            DiscordIntegrationMod.timeouts.remove(player.getUuid());
        }
    }
}

import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.INSTANCE;


@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(method = "checkCanJoin", at = @At("HEAD"), cancellable = true)
    public void canJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        if (DiscordIntegration.INSTANCE == null) return;
        LinkManager.checkGlobalAPI(profile.getId());
        if (Configuration.instance().linking.whitelistMode && DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode()) {
            try {
                if (!LinkManager.isPlayerLinked(profile.getId())) {
                    cir.setReturnValue(Text.of(Localization.instance().linking.notWhitelistedCode.replace("%code%", "" + LinkManager.genLinkNumber(profile.getId()))));
                } else if (!DiscordIntegration.INSTANCE.canPlayerJoin(profile.getId())) {
                    cir.setReturnValue(Text.of(Localization.instance().linking.notWhitelistedRole));
                }
            } catch (IllegalStateException e) {
                cir.setReturnValue(Text.of("An error occured\nPlease check Server Log for more information\n\n" + e));
                e.printStackTrace();
            }
        }
    }

    @Inject(at = @At(value = "TAIL"), method = "onPlayerConnect")
    private void onPlayerJoin(ClientConnection conn, ServerPlayerEntity p, CallbackInfo ci) {
        if (DiscordIntegration.INSTANCE != null) {
            if (LinkManager.isPlayerLinked(p.getUuid()) && LinkManager.getLink(null, p.getUuid()).settings.hideFromDiscord)
                return;
            LinkManager.checkGlobalAPI(p.getUuid());
            if (!Localization.instance().playerJoin.isBlank()) {
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.playerJoinMessage.asEmbed) {
                    final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", p.getUuid().toString()).replace("%uuid_dashless%", p.getUuid().toString().replace("-", "")).replace("%name%", p.getName().getString()).replace("%randomUUID%", UUID.randomUUID().toString());
                    if (!Configuration.instance().embedMode.playerJoinMessage.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerJoinMessage.toEmbedJson(Configuration.instance().embedMode.playerJoinMessage.customJSON
                                .replace("%uuid%", p.getUuid().toString())
                                .replace("%uuid_dashless%", p.getUuid().toString().replace("-", ""))
                                .replace("%name%", FabricMessageUtils.formatPlayerName(p))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(p.getUuid()).getRGB())
                        );
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    } else {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerJoinMessage.toEmbed();
                        b.setAuthor(FabricMessageUtils.formatPlayerName(p), null, avatarURL)
                                .setDescription(Localization.instance().playerJoin.replace("%player%", FabricMessageUtils.formatPlayerName(p)));
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                    }
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().playerJoin.replace("%player%", FabricMessageUtils.formatPlayerName(p)),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
            }
            // Fix link status (if user does not have role, give the role to the user, or vice versa)
            WorkThread.executeJob(() -> {
                if (Configuration.instance().linking.linkedRoleID.equals("0")) return;
                final UUID uuid = p.getUuid();
                if (!LinkManager.isPlayerLinked(uuid)) return;
                final Guild guild = DiscordIntegration.INSTANCE.getChannel().getGuild();
                final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);
                if (LinkManager.isPlayerLinked(uuid)) {
                    final Member member = DiscordIntegration.INSTANCE.getMemberById(LinkManager.getLink(null, uuid).discordID);
                    if (!member.getRoles().contains(linkedRole))
                        guild.addRoleToMember(member, linkedRole).queue();
                }
            });
        }
    }
}
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Inject(at = @At(value = "TAIL"), method = "onDeath")
    private void onPlayerDeath(DamageSource s, CallbackInfo info) {
        ServerPlayerEntity p = (ServerPlayerEntity) (Object) this;

        if (DiscordIntegration.INSTANCE != null) {
            if (LinkManager.isPlayerLinked(p.getUuid()) && LinkManager.getLink(null, p.getUuid()).settings.hideFromDiscord)
                return;
            final Text deathMessage = s.getDeathMessage(p);
            final MessageEmbed embed = FabricMessageUtils.genItemStackEmbedIfAvailable(deathMessage);
            if (!Localization.instance().playerDeath.isBlank())
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.deathMessage.asEmbed) {
                    final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", p.getUuid().toString()).replace("%uuid_dashless%", p.getUuid().toString().replace("-", "")).replace("%name%", p.getName().getString()).replace("%randomUUID%", UUID.randomUUID().toString());
                    if(!Configuration.instance().embedMode.deathMessage.customJSON.isBlank()){
                        final EmbedBuilder b = Configuration.instance().embedMode.deathMessage.toEmbedJson(Configuration.instance().embedMode.deathMessage.customJSON
                                .replace("%uuid%", p.getUuid().toString())
                                .replace("%uuid_dashless%", p.getUuid().toString().replace("-", ""))
                                .replace("%name%", FabricMessageUtils.formatPlayerName(p))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%deathMessage%", Formatting.strip(deathMessage.getString()).replace(FabricMessageUtils.formatPlayerName(p) + " ", ""))
                                .replace("%playerColor%", ""+ TextColors.generateFromUUID(p.getUuid()).getRGB())
                        );
                        if (embed != null) {
                            b.addBlankField(false);
                            b.addField(embed.getTitle() + " *(" + embed.getFooter().getText() + ")*", embed.getDescription(), false);
                        }
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()),INSTANCE.getChannel(Configuration.instance().advanced.deathsChannelID));
                    }else {
                        final EmbedBuilder b = Configuration.instance().embedMode.deathMessage.toEmbed();
                        b.setDescription(":skull: " + Localization.instance().playerDeath.replace("%player%", FabricMessageUtils.formatPlayerName(p)).replace("%msg%", Formatting.strip(deathMessage.getString()).replace(FabricMessageUtils.formatPlayerName(p) + " ", "")));
                        if (embed != null) {
                            b.addBlankField(false);
                            b.addField(embed.getTitle() + " *(" + embed.getFooter().getText() + ")*", embed.getDescription(), false);
                        }
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()), DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.deathsChannelID));
                    }
                } else
                    DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(embed, Localization.instance().playerDeath.replace("%player%", FabricMessageUtils.formatPlayerName(p)).replace("%msg%", Formatting.strip(deathMessage.getString()).replace(FabricMessageUtils.formatPlayerName(p) + " ", ""))), DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.deathsChannelID));
        }
    }
}
*/
