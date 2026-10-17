package dev.cursedarts;

import dev.cursedarts.network.CeSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;

public class CursedArts implements ModInitializer {
    public static final String MOD_ID = "cursedarts";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CursedEnergy.init();
        ModItems.init();

        PayloadTypeRegistry.playS2C().register(CeSyncPayload.ID, CeSyncPayload.CODEC);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            CursedEnergy.tick(server);
            TickScheduler.tick();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> CursedEnergy.sync(handler.getPlayer()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> CursedEnergy.sync(newPlayer));
    }
}
