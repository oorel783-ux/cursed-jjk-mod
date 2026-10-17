package dev.cursedarts.client;

import dev.cursedarts.network.CeSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class CursedArtsClient implements ClientModInitializer {
    private static int ce = 100;
    private static int max = 100;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(CeSyncPayload.ID, (payload, context) -> {
            ce = payload.ce();
            max = payload.max();
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) return;

            int x = 10;
            int y = ctx.getScaledWindowHeight() - 18;
            int w = 90;
            int filled = (int) (w * (ce / (float) Math.max(1, max)));

            ctx.fill(x - 1, y - 1, x + w + 1, y + 6, 0xCC000000);
            ctx.fill(x, y, x + filled, y + 5, 0xFF7B2CBF);
            ctx.fill(x, y, x + filled, y + 2, 0xFFB15CFF);
            ctx.drawText(mc.textRenderer, Text.literal("Cursed Energy " + ce + "/" + max), x, y - 10, 0xFFD9B3FF, true);
        });
    }
}
