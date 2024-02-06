package com.alfred.ai.mixin;

import com.alfred.ai.MCAIMod;
import com.alfred.ai.MCAIConfig;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// first iteration
//  attempt to intercept messages sent from the client to the rest of the server and disable them if they were directed at an AI
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ChatMessageC2SPacket) {
            ChatMessageC2SPacket chatPacket = (ChatMessageC2SPacket) packet;
            if (true && !onServer) {
                ci.cancel();
                System.out.println("Outgoing chat message intercepted and canceled!");
                String text = packet.getSignedContent();
				for (MCAIConfig.CharacterTuple tuple : config.AIs) {
					if (tuple.disabled) // ignore disabled AIs
						continue;
					List<String> list = new java.util.ArrayList<>(Arrays.stream(tuple.aliases).toList());
					list.add(0, tuple.name);
					String[] arr = list.toArray(new String[] {});
					for (String name : arr) {
						if (text.toLowerCase().contains(String.format("@%s", name.toLowerCase())) || (tuple.randomTalkChance > random.nextFloat() && !config.General.disableRandomResponses)) {
							if (text.toLowerCase().startsWith(String.format("@%s", name.toLowerCase())))
								text = text.substring(name.length() + 1); // chop off starting ping
							MCAIMod.sendAIMessage(
									text, tuple, sender != null ? sender.getName().getLiteralString() : "Anonymous",
									config.General.format, config.General.replyFormat, sender != null ? sender.getServer() : null);
							tuple.setLastCommunicatedWith();
							MCAIConfig.save();
							MCAIConfig.load();
							break;
						}
					}
				}
            }
        }
    }
}
