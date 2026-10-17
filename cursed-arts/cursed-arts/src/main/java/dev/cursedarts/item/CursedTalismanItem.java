package dev.cursedarts.item;

import dev.cursedarts.CursedEnergy;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** Consumable: +25 max cursed energy (up to 500) and a full refill. */
public class CursedTalismanItem extends Item {
    public CursedTalismanItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);

        ServerPlayerEntity p = (ServerPlayerEntity) user;
        int max = CursedEnergy.max(p);
        if (max >= CursedEnergy.HARD_CAP && CursedEnergy.get(p) >= max) {
            p.sendMessage(Text.translatable("msg.cursedarts.maxed").formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }
        CursedEnergy.setMax(p, max + 25);
        CursedEnergy.set(p, CursedEnergy.max(p));
        stack.decrementUnlessCreative(1, user);

        ServerWorld w = (ServerWorld) world;
        w.spawnParticles(ParticleTypes.WITCH, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.1);
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1f, 0.8f);
        p.sendMessage(Text.translatable("msg.cursedarts.max_up", CursedEnergy.max(p)).formatted(Formatting.LIGHT_PURPLE), true);
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.cursedarts.cursed_talisman.desc").formatted(Formatting.GRAY));
    }
}
