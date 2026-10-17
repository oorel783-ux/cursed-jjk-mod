package dev.cursedarts.item;

import dev.cursedarts.CursedEnergy;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.List;

/** Melee weapon: each hit has a chance to trigger a Black Flash (big bonus damage + CE refund). */
public class BlackFlashKnucklesItem extends SwordItem {
    private static final DustParticleEffect BLACK = new DustParticleEffect(new Vector3f(0.02f, 0.02f, 0.02f), 2.2f);
    private static final DustParticleEffect RED = new DustParticleEffect(new Vector3f(0.9f, 0.05f, 0.1f), 1.4f);

    public BlackFlashKnucklesItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayerEntity p && p.getWorld() instanceof ServerWorld w
                && CursedEnergy.get(p) >= 5 && w.random.nextFloat() < 0.2f) {
            target.timeUntilRegen = 0;
            target.damage(w.getDamageSources().playerAttack(p), 14f);
            Vec3d look = p.getRotationVec(1f);
            target.addVelocity(look.x * 1.5, 0.4, look.z * 1.5);
            target.velocityModified = true;

            Vec3d c = target.getBoundingBox().getCenter();
            w.spawnParticles(BLACK, c.x, c.y, c.z, 60, 0.5, 0.6, 0.5, 0);
            w.spawnParticles(RED, c.x, c.y, c.z, 40, 0.7, 0.7, 0.7, 0);
            w.spawnParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
            w.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 1f, 1.6f);

            CursedEnergy.set(p, CursedEnergy.get(p) + 20);
            p.sendMessage(Text.translatable("msg.cursedarts.black_flash").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.cursedarts.black_flash_knuckles.desc").formatted(Formatting.GRAY));
    }
}
