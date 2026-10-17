package dev.cursedarts.network;

import dev.cursedarts.CursedArts;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record CeSyncPayload(int ce, int max) implements CustomPayload {
    public static final CustomPayload.Id<CeSyncPayload> ID = new CustomPayload.Id<>(CursedArts.id("ce_sync"));
    public static final PacketCodec<RegistryByteBuf, CeSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, CeSyncPayload::ce,
            PacketCodecs.VAR_INT, CeSyncPayload::max,
            CeSyncPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
