package com.alfred.ai;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record EchoPayload() implements CustomPayload {
    public static final CustomPayload.Id<EchoPayload> ID = new CustomPayload.Id<>(MCAIMod.ON_SERVER_PACKET_ID);

    // PacketCodec using PacketByteBuf - encoder (write) and decoder (read constructor)
    public static final PacketCodec<PacketByteBuf, EchoPayload> CODEC =
            PacketCodec.unit(new EchoPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
