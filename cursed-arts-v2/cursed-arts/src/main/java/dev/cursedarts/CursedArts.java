package dev.cursedarts;

import dev.cursedarts.fx.Fx;
import dev.cursedarts.network.CeSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;

public class CursedArts implements ModInitializer {
    public static final String MOD_ID = "cursedarts";

    /** /gamerule cursedartsBlockDamage false  turns off all block destruction. */
    public static final GameRules.Key<GameRules.BooleanRule> BLOCK_DAMAGE = GameRuleRegistry.register(
            "cursedartsBlockDamage", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CursedEnergy.init();
        ModBlocks.init();
        ModItems.init();

        PayloadTypeRegistry.playS2C().register(CeSyncPayload.ID, CeSyncPayload.CODEC);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            CursedEnergy.tick(server);
            TickScheduler.tick();
            Fx.tick();
        });

        // Clean up effect entities / domain blocks if the server shuts down mid-technique.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            TickScheduler.cleanupAll();
            Fx.killAll();
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> Fx.onEntityLoad(entity));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> CursedEnergy.sync(handler.getPlayer()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> CursedEnergy.sync(newPlayer));
    }
}
