package dev.cursedarts.item;

import dev.cursedarts.CursedEnergy;
import dev.cursedarts.ModItems;
import dev.cursedarts.TickScheduler;
import dev.cursedarts.fx.Fx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.List;

/** Melee weapon: each hit has a chance to trigger a Black Flash (big bonus damage + CE refund). */
public class BlackFlashKnucklesItem extends SwordItem {
    private static final DustParticleEffect BLACK = new DustParticleEffect(new Vector3f(0.01f, 0.01f, 0.01f), 1.6f);
    private static final DustParticleEffect RED = new DustParticleEffect(new Vector3f(1f, 0.05f, 0.12f), 1.2f);

    public BlackFlashKnucklesItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayerEntity p && p.getWorld() instanceof ServerWorld w
                && CursedEnergy.get(p) >= 5 && w.random.nextFloat() < 0.2f) {
            blackFlash(p, w, target);
        }
        return super.postHit(stack, target, attacker);
    }

    private static void blackFlash(ServerPlayerEntity p, ServerWorld w, LivingEntity target) {
        target.timeUntilRegen = 0;
        target.damage(w.getDamageSources().playerAttack(p), 14f);
        Vec3d look = p.getRotationVec(1f);
        target.addVelocity(look.x * 1.8, 0.45, look.z * 1.8);
        target.velocityModified = true;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 5, false, false));

        Vec3d c = target.getBoundingBox().getCenter();

        // distorted space: a black-red flash orb that bursts and vanishes
        Entity spark = Fx.orb(w, c, ModItems.FX_BLACK_SPARK, 0.3f);
        Fx.shape(spark, 3.2f, 2f, 2);
        TickScheduler.add(age -> {
            if (age == 3) Fx.shape(spark, 0.01f, 4f, 3);
            if (age >= 6) {
                Fx.kill(spark);
                return false;
            }
            return true;
        });

        // black lightning arcing out of the impact
        for (int bolt = 0; bolt < 7; bolt++) {
            Vec3d pos = c;
            Vec3d dir = new Vec3d(w.random.nextGaussian(), w.random.nextGaussian() * 0.6, w.random.nextGaussian()).normalize();
            for (int seg = 0; seg < 6; seg++) {
                Vec3d jitter = new Vec3d(w.random.nextGaussian(), w.random.nextGaussian(), w.random.nextGaussian()).multiply(0.35);
                Vec3d next = pos.add(dir.multiply(0.55)).add(jitter);
                Fx.line(w, BLACK, pos, next, 0.12);
                if (seg % 2 == 1) w.spawnParticles(RED, next.x, next.y, next.z, 2, 0.05, 0.05, 0.05, 0);
                pos = next;
            }
        }
        Fx.ring(w, ParticleTypes.SQUID_INK, c, 0.4, 40, 0.5);
        Fx.burst(w, ParticleTypes.CRIT, c, 40, 0.8);
        w.spawnParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);

        Fx.sound(w, c, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 1.4f, 1.5f);
        Fx.sound(w, c, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 1.5f, 0.5f);
        Fx.sound(w, c, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 2f);

        CursedEnergy.set(p, CursedEnergy.get(p) + 20);
        p.sendMessage(Text.translatable("msg.cursedarts.black_flash").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.cursedarts.black_flash_knuckles.desc").formatted(Formatting.GRAY));
    }
}
