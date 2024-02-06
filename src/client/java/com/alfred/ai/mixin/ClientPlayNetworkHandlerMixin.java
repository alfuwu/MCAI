package com.alfred.ai.mixin;

import com.alfred.ai.MCAIConfig;
import com.alfred.ai.MCAIMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.message.*;
import net.minecraft.text.Decoration;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.alfred.ai.MCAIMod.onServer;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void catchChatMessage(String content, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!onServer && client.player != null) {
            MCAIConfig config = MCAIConfig.getInstance();
            for (MCAIConfig.CharacterTuple tuple : config.AIs) {
                if (tuple.disabled) // ignore disabled AIs
                    continue;
                List<String> list = new ArrayList<>(Arrays.stream(tuple.aliases).toList());
                list.add(0, tuple.name);
                String[] arr = list.toArray(new String[] {});
                for (String name : arr) {
                    if (content.toLowerCase().contains(String.format("@%s", name.toLowerCase())) || (tuple.randomTalkChance > Random.create().nextFloat() && !config.General.disableRandomResponses)) {
                        MessageType type = new MessageType(MessageType.CHAT_TEXT_DECORATION, Decoration.ofChat("chat.type.text.narrate"));
                        MessageType.Parameters params = new MessageType.Parameters(type, client.player.getName(), null);
                        Text textDecorated = params.applyChatDecoration(Text.literal(content));
                        client.inGameHud.getChatHud().addMessage(textDecorated, null, null);

                        ci.cancel();
                        if (content.toLowerCase().startsWith(String.format("@%s", name.toLowerCase())))
                            content = content.substring(name.length() + 1); // chop off starting ping
                        MCAIMod.sendAIMessage(
                                content, tuple, client.player.getName() != null ? client.player.getName().getString() : "Anonymous",
                                config.General.format, config.General.replyFormat);
                        MCAIMod.setLastCommunicatedWith(tuple);
                        MCAIConfig.save();
                        MCAIConfig.load();
                        break;
                    }
                }
            }
        }
    }
}
