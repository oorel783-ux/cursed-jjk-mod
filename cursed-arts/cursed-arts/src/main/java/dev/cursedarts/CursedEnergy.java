package dev.cursedarts;

import com.mojang.serialization.Codec;
import dev.cursedarts.network.CeSyncPayload;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Cursed energy (CE): a per-player mana pool, saved with the player and synced to the HUD. */
public final class CursedEnergy {
    public static final int START_MAX = 100;
    public static final int HARD_CAP = 500;

    public static final AttachmentType<Integer> CE = AttachmentRegistry.<Integer>builder()
            .persistent(Codec.INT)
            .copyOnDeath()
            .initializer(() -> START_MAX)
            .buildAndRegister(CursedArts.id("cursed_energy"));

    public static final AttachmentType<Integer> MAX = AttachmentRegistry.<Integer>builder()
            .persistent(Codec.INT)
            .copyOnDeath()
            .initializer(() -> START_MAX)
            .buildAndRegister(CursedArts.id("max_cursed_energy"));

    private CursedEnergy() {}

    public static void init() {}

    public static int get(PlayerEntity p) {
        return p.getAttachedOrCreate(CE);
    }

    public static int max(PlayerEntity p) {
        return p.getAttachedOrCreate(MAX);
    }

    public static void set(ServerPlayerEntity p, int value) {
        p.setAttached(CE, Math.max(0, Math.min(value, max(p))));
        sync(p);
    }

    public static void setMax(ServerPlayerEntity p, int value) {
        p.setAttached(MAX, Math.max(1, Math.min(value, HARD_CAP)));
        set(p, get(p));
    }

    public static boolean tryConsume(ServerPlayerEntity p, int amount) {
        if (p.isCreative()) return true;
        int current = get(p);
        if (current < amount) {
            p.sendMessage(Text.translatable("msg.cursedarts.not_enough", amount).formatted(Formatting.DARK_RED), true);
            return false;
        }
        set(p, current - amount);
        return true;
    }

    public static void sync(ServerPlayerEntity p) {
        ServerPlayNetworking.send(p, new CeSyncPayload(get(p), max(p)));
    }

    /** Regenerates 1 CE every half second. */
    public static void tick(MinecraftServer server) {
        if (server.getTicks() % 10 != 0) return;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            int c = get(p);
            if (c < max(p)) set(p, c + 1);
        }
    }
}
