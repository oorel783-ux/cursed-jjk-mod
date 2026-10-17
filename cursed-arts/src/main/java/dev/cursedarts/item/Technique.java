package dev.cursedarts.item;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.function.BiConsumer;

/** Each technique: cursed energy cost, cooldown in ticks, and what it does. */
public enum Technique {
    RED(25, 30, Techniques::red),
    BLUE(25, 60, Techniques::blue),
    HOLLOW_PURPLE(80, 200, Techniques::hollowPurple),
    DISMANTLE(15, 15, Techniques::dismantle),
    REVERSE_CURSED_TECHNIQUE(30, 60, Techniques::reverseCursedTechnique),
    DOMAIN_EXPANSION(100, 1200, Techniques::domainExpansion);

    public final int cost;
    public final int cooldown;
    private final BiConsumer<ServerPlayerEntity, ServerWorld> action;

    Technique(int cost, int cooldown, BiConsumer<ServerPlayerEntity, ServerWorld> action) {
        this.cost = cost;
        this.cooldown = cooldown;
        this.action = action;
    }

    public void cast(ServerPlayerEntity player, ServerWorld world) {
        action.accept(player, world);
    }
}
