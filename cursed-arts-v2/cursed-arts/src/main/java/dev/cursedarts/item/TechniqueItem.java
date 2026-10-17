package dev.cursedarts.item;

import dev.cursedarts.CursedEnergy;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class TechniqueItem extends Item {
    private final Technique technique;

    public TechniqueItem(Settings settings, Technique technique) {
        super(settings);
        this.technique = technique;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        if (!CursedEnergy.tryConsume(player, technique.cost)) return TypedActionResult.fail(stack);

        technique.cast(player, (ServerWorld) world);
        user.getItemCooldownManager().set(this, technique.cooldown);
        return TypedActionResult.success(stack);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable(getTranslationKey() + ".desc").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.cursedarts.cost", technique.cost).formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.translatable("tooltip.cursedarts.cooldown",
                String.format("%.1f", technique.cooldown / 20f)).formatted(Formatting.DARK_GRAY));
    }
}
