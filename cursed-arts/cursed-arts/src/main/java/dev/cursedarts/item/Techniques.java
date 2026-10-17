package dev.cursedarts.item;

import dev.cursedarts.TickScheduler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class Techniques {
    private static final DustParticleEffect RED_DUST = new DustParticleEffect(new Vector3f(1f, 0.1f, 0.15f), 1.6f);
    private static final DustParticleEffect BLUE_DUST = new DustParticleEffect(new Vector3f(0.15f, 0.45f, 1f), 1.6f);
    private static final DustParticleEffect PURPLE_DUST = new DustParticleEffect(new Vector3f(0.6f, 0.1f, 1f), 2.5f);
    private static final DustParticleEffect DOMAIN_DUST = new DustParticleEffect(new Vector3f(0.05f, 0.0f, 0.1f), 2.0f);

    private Techniques() {}

    // ---------------------------------------------------------------- helpers

    private static List<LivingEntity> livingNear(ServerWorld w, Entity exclude, Vec3d c, double r) {
        Box box = new Box(c.subtract(r, r, r), c.add(r, r, r));
        return w.getEntitiesByClass(LivingEntity.class, box,
                e -> e != exclude && e.isAlive() && e.squaredDistanceTo(c) <= r * r);
    }

    private static void sound(ServerWorld w, Vec3d pos, SoundEvent s, float volume, float pitch) {
        w.playSound(null, pos.x, pos.y, pos.z, s, SoundCategory.PLAYERS, volume, pitch);
    }

    private static void hit(LivingEntity target, ServerPlayerEntity caster, ServerWorld w, float amount) {
        target.timeUntilRegen = 0;
        target.damage(w.getDamageSources().indirectMagic(caster, caster), amount);
    }

    // ---------------------------------------------------------------- RED: repulsion blast

    static void red(ServerPlayerEntity p, ServerWorld w) {
        Vec3d look = p.getRotationVec(1f);
        Vec3d c = p.getEyePos().add(look.multiply(3));

        w.spawnParticles(RED_DUST, c.x, c.y, c.z, 120, 0.8, 0.8, 0.8, 0);
        w.spawnParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 2, 0.3, 0.3, 0.3, 0);
        sound(w, c, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.2f, 1.4f);

        for (LivingEntity e : livingNear(w, p, c, 6)) {
            Vec3d push = e.getPos().subtract(p.getPos()).normalize();
            hit(e, p, w, 8f);
            e.addVelocity(push.x * 2.8, 0.7, push.z * 2.8);
            e.velocityModified = true;
        }
        // small recoil on the caster
        p.addVelocity(-look.x * 0.5, 0.1, -look.z * 0.5);
        p.velocityModified = true;
    }

    // ---------------------------------------------------------------- BLUE: attraction singularity

    static void blue(ServerPlayerEntity p, ServerWorld w) {
        Vec3d point = p.raycast(18, 1f, false).getPos();
        sound(w, point, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.2f, 0.6f);

        TickScheduler.add(age -> {
            if (age > 60 || p.isRemoved()) return false;

            double angle = age * 0.6;
            for (int i = 0; i < 4; i++) {
                double a = angle + i * Math.PI / 2;
                double r = 2.5 - (age % 20) * 0.1;
                w.spawnParticles(BLUE_DUST, point.x + Math.cos(a) * r, point.y + Math.sin(a * 0.5) * 0.6,
                        point.z + Math.sin(a) * r, 2, 0.05, 0.05, 0.05, 0);
            }
            w.spawnParticles(ParticleTypes.REVERSE_PORTAL, point.x, point.y, point.z, 6, 0.3, 0.3, 0.3, 0.05);

            Box box = new Box(point.subtract(8, 8, 8), point.add(8, 8, 8));
            for (Entity e : w.getOtherEntities(p, box, e -> e instanceof LivingEntity || e instanceof ItemEntity)) {
                Vec3d pull = point.subtract(e.getPos());
                double dist = pull.length();
                if (dist < 0.4 || dist > 8) continue;
                Vec3d v = pull.normalize().multiply(Math.min(0.55, 1.6 / dist));
                e.setVelocity(e.getVelocity().multiply(0.6).add(v));
                e.velocityModified = true;
                if (age % 10 == 0 && e instanceof LivingEntity le && dist < 2.5) hit(le, p, w, 3f);
            }
            if (age % 15 == 0) sound(w, point, SoundEvents.BLOCK_PORTAL_AMBIENT, 0.6f, 1.8f);
            return true;
        });
    }

    // ---------------------------------------------------------------- HOLLOW PURPLE: imaginary-mass beam

    static void hollowPurple(ServerPlayerEntity p, ServerWorld w) {
        Vec3d start = p.getEyePos();
        Vec3d dir = p.getRotationVec(1f);
        Set<LivingEntity> already = new HashSet<>();

        sound(w, start, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 2f, 0.5f);
        sound(w, start, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.6f);

        Vec3d pos = start;
        for (double d = 1; d <= 48; d += 0.75) {
            pos = start.add(dir.multiply(d));
            w.spawnParticles(PURPLE_DUST, pos.x, pos.y, pos.z, 4, 0.4, 0.4, 0.4, 0);
            if (((int) (d * 4)) % 6 == 0) w.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 2, 0.6, 0.6, 0.6, 0.02);
            for (LivingEntity e : livingNear(w, p, pos, 2.2)) {
                if (already.add(e)) {
                    hit(e, p, w, 26f);
                    e.addVelocity(dir.x * 2, 0.5, dir.z * 2);
                    e.velocityModified = true;
                }
            }
        }
        // Explosion at the end of the beam. ExplosionSourceType.NONE = no block damage.
        w.createExplosion(p, pos.x, pos.y, pos.z, 3f, World.ExplosionSourceType.NONE);
    }

    // ---------------------------------------------------------------- DISMANTLE: invisible slashes

    static void dismantle(ServerPlayerEntity p, ServerWorld w) {
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1f);
        sound(w, eye, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);

        for (int i = 1; i <= 10; i++) {
            Vec3d at = eye.add(look.multiply(i));
            w.spawnParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0.3, 0.3, 0.3, 0);
        }
        for (LivingEntity e : livingNear(w, p, eye, 10)) {
            Vec3d to = e.getBoundingBox().getCenter().subtract(eye).normalize();
            if (to.dotProduct(look) < 0.82) continue; // narrow cone in front of you
            hit(e, p, w, 10f);
            Vec3d c = e.getBoundingBox().getCenter();
            w.spawnParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 3, 0.4, 0.5, 0.4, 0);
            w.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, c.x, c.y, c.z, 6, 0.3, 0.3, 0.3, 0.1);
            sound(w, c, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.3f);
        }
    }

    // ---------------------------------------------------------------- REVERSE CURSED TECHNIQUE: healing

    static void reverseCursedTechnique(ServerPlayerEntity p, ServerWorld w) {
        p.heal(10f);
        p.removeStatusEffect(StatusEffects.POISON);
        p.removeStatusEffect(StatusEffects.WITHER);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
        Vec3d c = p.getPos().add(0, 1, 0);
        w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y, c.z, 30, 0.5, 0.8, 0.5, 0);
        w.spawnParticles(ParticleTypes.ENCHANT, c.x, c.y, c.z, 60, 0.6, 0.9, 0.6, 0.5);
        sound(w, c, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 0.6f, 1.8f);
    }

    // ---------------------------------------------------------------- DOMAIN EXPANSION: sure-hit barrier

    static void domainExpansion(ServerPlayerEntity p, ServerWorld w) {
        final Vec3d center = p.getPos();
        final double radius = 14;
        final int duration = 200; // 10 seconds

        sound(w, center, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 2f, 0.5f);
        sound(w, center, SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 2f, 0.6f);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, 1));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 1));

        TickScheduler.add(age -> {
            if (age > duration || !p.isAlive() || p.getWorld() != w) return false;

            // barrier shell
            int pts = age < 20 ? 200 : 60;
            for (int i = 0; i < pts; i++) {
                double u = w.random.nextDouble() * 2 - 1;
                double th = w.random.nextDouble() * Math.PI * 2;
                double s = Math.sqrt(1 - u * u);
                double x = center.x + radius * s * Math.cos(th);
                double y = center.y + radius * u;
                double z = center.z + radius * s * Math.sin(th);
                w.spawnParticles(DOMAIN_DUST, x, y, z, 1, 0, 0, 0, 0);
            }
            if (age % 4 == 0) {
                w.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 0.2, center.z, 10, radius * 0.5, 0.1, radius * 0.5, 0.02);
            }

            // sure-hit: everyone inside except the caster
            if (age % 20 == 0) {
                for (LivingEntity e : livingNear(w, p, center, radius)) {
                    hit(e, p, w, 5f);
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 5));
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 2));
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 30, 2));
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 40, 0));
                    Vec3d c = e.getBoundingBox().getCenter();
                    w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y, c.z, 8, 0.3, 0.5, 0.3, 0.02);
                }
                sound(w, center, SoundEvents.BLOCK_BEACON_AMBIENT, 1.5f, 0.5f);
            }
            if (age == duration) sound(w, center, SoundEvents.BLOCK_BEACON_DEACTIVATE, 2f, 0.6f);
            return true;
        });
    }
}
