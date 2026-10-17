package dev.cursedarts.client;

import dev.cursedarts.network.CeSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class CursedArtsClient implements ClientModInitializer {
    private static int ce = 100;
    private static int max = 100;
    /** Smoothly animated fill so the bar glides instead of jumping. */
    private static float shown = 1f;
    /** Bright flash on the bar when energy jumps up (e.g. Black Flash, talisman). */
    private static float flash = 0f;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(CeSyncPayload.ID, (payload, context) -> {
            if (payload.ce() - ce >= 10) flash = 1f;
            ce = payload.ce();
            max = payload.max();
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) return;
            render(ctx, mc.textRenderer, ctx.getScaledWindowHeight());
        });
    }

    private static void render(DrawContext ctx, TextRenderer font, int screenH) {
        float target = ce / (float) Math.max(1, max);
        shown += (target - shown) * 0.15f;
        flash = Math.max(0f, flash - 0.03f);

        int x = 8, w = 100, h = 7;
        int y = screenH - 16;
        long time = System.currentTimeMillis();

        // frame
        ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xE0100616);
        ctx.fill(x - 1, y - 1, x + w + 1, y, 0xFF5A2A86);
        ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFF2A0F40);
        ctx.fill(x - 1, y, x, y + h, 0xFF5A2A86);
        ctx.fill(x + w, y, x + w + 1, y + h, 0xFF2A0F40);
        ctx.fill(x, y, x + w, y + h, 0xFF1A0B26);

        // fill gradient
        int fw = Math.round(w * Math.min(1f, shown));
        if (fw > 0) {
            ctx.fillGradient(x, y, x + fw, y + h, 0xFFC27BFF, 0xFF5B1C9E);
            ctx.fill(x, y, x + fw, y + 1, 0x80FFFFFF);
            // drifting shimmer
            int sx = x + (int) ((time / 12) % (w + 30)) - 30;
            for (int i = 0; i < 14; i++) {
                int px = sx + i;
                if (px >= x && px < x + fw) {
                    int alpha = (int) (60 * Math.sin(Math.PI * i / 14));
                    ctx.fill(px, y + 1, px + 1, y + h - 1, (alpha << 24) | 0xFFFFFF);
                }
            }
            // bright leading edge
            ctx.fill(x + fw - 1, y, x + fw, y + h, 0xFFF0DCFF);
        }
        if (flash > 0) {
            int a = (int) (flash * 160);
            ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, (a << 24) | 0xFFFFFF);
        }

        // label
        String label = "✦ Cursed Energy";
        String value = ce + " / " + max;
        ctx.drawText(font, Text.literal(label), x, y - 11, 0xFFE4C8FF, true);
        ctx.drawText(font, Text.literal(value), x + w - font.getWidth(value), y - 11, 0xFFB08ED6, true);
    }
}
