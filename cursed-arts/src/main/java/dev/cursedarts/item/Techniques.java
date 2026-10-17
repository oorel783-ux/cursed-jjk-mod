package dev.cursedarts.item;

import dev.cursedarts.ModItems;
import dev.cursedarts.TickScheduler;
import dev.cursedarts.fx.DomainExpansion;
import dev.cursedarts.fx.Fx;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class Techniques {
    static final DustParticleEffect RED_DUST = new DustParticleEffect(new Vector3f(1f, 0.12f, 0.18f), 1.8f);
    static final DustColorTransitionParticleEffect RED_TRAIL = new DustColorTransitionParticleEffect(
            new Vector3f(1f, 0.35f, 0.25f), new Vector3f(0.35f, 0f, 0.05f), 2.4f);
    static final DustParticleEffect BLUE_DUST = new DustParticleEffect(new Vector3f(0.25f, 0.6f, 1f), 1.8f);
    static final DustColorTransitionParticleEffect BLUE_TRAIL = new DustColorTransitionParticleEffect(
            new Vector3f(0.6f, 0.9f, 1f), new Vector3f(0.05f, 0.1f, 0.5f), 2.0f);
    static final DustParticleEffect PURPLE_DUST = new DustParticleEffect(new Vector3f(0.65f, 0.2f, 1f), 3.0f);
    static final DustColorTransitionParticleEffect PURPLE_TRAIL = new DustColorTransitionParticleEffect(
            new Vector3f(0.95f, 0.7f, 1f), new Vector3f(0.3f, 0f, 0.5f), 4.0f);
    static final DustParticleEffect SLASH_DUST = new DustParticleEffect(new Vector3f(1f, 1f, 1f), 0.7f);
    static final DustColorTransitionParticleEffect HEAL_DUST = new DustColorTransitionParticleEffect(
            new Vector3f(0.6f, 1f, 0.7f), new Vector3f(1f, 1f, 1f), 1.2f);

    private Techniques() {}

    // ================================================================ helpers

    static List<LivingEntity> livingNear(ServerWorld w, Entity exclude, Vec3d c, double r) {
        Box box = new Box(c.subtract(r, r, r), c.add(r, r, r));
        return w.getEntitiesByClass(LivingEntity.class, box,
                e -> e != exclude && e.isAlive() && e.getBoundingBox().getCenter().squaredDistanceTo(c) <= r * r);
    }

    static void hit(LivingEntity target, ServerPlayerEntity caster, ServerWorld w, float amount) {
        target.timeUntilRegen = 0;
        target.damage(w.getDamageSources().indirectMagic(caster, caster), amount);
    }

    static Vec3d rightOf(ServerPlayerEntity p) {
        return Vec3d.fromPolar(0f, p.getYaw() + 90f);
    }

    static Vec3d upOf(ServerPlayerEntity p) {
        Vec3d look = p.getRotationVec(1f);
        Vec3d up = rightOf(p).crossProduct(look);
        return up.lengthSquared() < 1e-4 ? new Vec3d(0, 1, 0) : up.normalize();
    }

    static void knock(Entity e, Vec3d dir, double strength, double lift) {
        e.addVelocity(dir.x * strength, lift, dir.z * strength);
        e.velocityModified = true;
    }

    // ================================================================ RED — reversal: a repelling orb that detonates

    static void red(ServerPlayerEntity p, ServerWorld w) {
        Fx.sound(w, p.getEyePos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, 1f, 1.8f);
        TickScheduler.add(new TickScheduler.Task() {
            static final int CHARGE = 6;
            Entity orb;
            Vec3d pos;
            Vec3d vel;

            @Override
            public boolean tick(int age) {
                if (age < CHARGE) {
                    pos = p.getEyePos().add(p.getRotationVec(1f).multiply(1.4))
                            .add(rightOf(p).multiply(0.35)).add(0, -0.25, 0);
                    if (orb == null) {
                        orb = Fx.orb(w, pos, ModItems.FX_RED_ORB, 0.05f);
                        Fx.shape(orb, 1.0f, 0f, CHARGE);
                    } else {
                        Fx.move(orb, pos);
                    }
                    Fx.converge(w, RED_DUST, pos, 1.6, 8, 0.15);
                    Fx.converge(w, ParticleTypes.ELECTRIC_SPARK, pos, 1.2, 2, 0.12);
                    return true;
                }
                if (age == CHARGE) {
                    vel = p.getRotationVec(1f).multiply(1.9);
                    Fx.sound(w, pos, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 0.7f, 2f);
                }

                Vec3d next = pos.add(vel);
                BlockHitResult bh = w.raycast(new RaycastContext(pos, next,
                        RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
                boolean hitBlock = bh.getType() != HitResult.Type.MISS;
                Vec3d end = hitBlock ? bh.getPos() : next;
                boolean hitEntity = !livingNear(w, p, end, 1.5).isEmpty()
                        || !livingNear(w, p, pos.add(end).multiply(0.5), 1.5).isEmpty();

                Fx.line(w, RED_TRAIL, pos, end, 0.35);
                w.spawnParticles(ParticleTypes.FLAME, end.x, end.y, end.z, 3, 0.15, 0.15, 0.15, 0.01);
                pos = end;
                Fx.move(orb, pos);
                Fx.shape(orb, 1.0f, age * 0.9f, 1);

                if (hitBlock || hitEntity || age > CHARGE + 22) {
                    Fx.kill(orb);
                    redBlast(p, w, pos);
                    return false;
                }
                return true;
            }

            @Override
            public void cleanup() {
                Fx.kill(orb);
            }
        });
    }

    static void redBlast(ServerPlayerEntity p, ServerWorld w, Vec3d c) {
        Entity flash = Fx.orb(w, c, ModItems.FX_RED_ORB, 1f);
        Fx.shape(flash, 6f, 1.5f, 4);
        TickScheduler.add(age -> {
            if (age == 4) Fx.shape(flash, 0.01f, 3f, 3);
            if (age >= 7) {
                Fx.kill(flash);
                return false;
            }
            return true;
        });

        Fx.at(w, RED_DUST, c, 160, 1.6, 0);
        Fx.burst(w, ParticleTypes.FLAME, c, 60, 0.45);
        Fx.burst(w, ParticleTypes.CRIT, c, 50, 0.9);
        Fx.ring(w, ParticleTypes.CLOUD, c, 0.6, 56, 0.7);
        Fx.at(w, ParticleTypes.FLASH, c, 1, 0, 0);
        Fx.sound(w, c, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, 2f, 0.8f);
        Fx.sound(w, c, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 0.6f);

        if (Fx.blockDamage(w)) {
            w.createExplosion(p, c.x, c.y, c.z, 3.8f, World.ExplosionSourceType.TNT);
        }
        for (LivingEntity e : livingNear(w, p, c, 7)) {
            Vec3d dir = e.getPos().subtract(c);
            dir = dir.lengthSquared() < 1e-4 ? p.getRotationVec(1f) : dir.normalize();
            hit(e, p, w, 11f);
            knock(e, dir, 3.2, 0.8);
        }
    }

    // ================================================================ BLUE — lapse: a singularity that devours terrain

    static void blue(ServerPlayerEntity p, ServerWorld w) {
        Vec3d look = p.getRotationVec(1f);
        Vec3d point = p.raycast(22, 1f, false).getPos().subtract(look.multiply(0.5));

        TickScheduler.add(new TickScheduler.Task() {
            static final int DURATION = 80;
            Entity orb;
            final Set<FallingBlockEntity> debris = new HashSet<>();

            @Override
            public boolean tick(int age) {
                if (age == 0) {
                    orb = Fx.orb(w, point, ModItems.FX_BLUE_ORB, 0.05f);
                    Fx.shape(orb, 2.6f, 0f, 10);
                    Fx.sound(w, point, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);
                    Fx.sound(w, point, SoundEvents.BLOCK_PORTAL_TRIGGER, 0.5f, 2f);
                    Fx.burst(w, ParticleTypes.END_ROD, point, 40, 0.3);
                }
                if (age == DURATION) {
                    Fx.shape(orb, 0.01f, 6f, 6);
                    Fx.sound(w, point, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
                }
                if (age >= DURATION + 6) {
                    Fx.kill(orb);
                    for (FallingBlockEntity fb : debris) crush(fb);
                    debris.clear();
                    Fx.at(w, ParticleTypes.FLASH, point, 1, 0, 0);
                    Fx.burst(w, ParticleTypes.END_ROD, point, 80, 0.6);
                    Fx.ring(w, ParticleTypes.CLOUD, point, 0.5, 40, 0.6);
                    Fx.sound(w, point, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 1.3f);
                    for (LivingEntity e : livingNear(w, p, point, 4)) hit(e, p, w, 8f);
                    return false;
                }

                // spin / pulse
                if (age > 10 && age % 5 == 0 && age < DURATION) {
                    Fx.shape(orb, 2.6f + 0.25f * (float) Math.sin(age * 0.4), age * 0.6f, 5);
                }

                // swirling arms spiralling inward
                for (int arm = 0; arm < 5; arm++) {
                    double a = age * 0.45 + arm * Math.PI * 2 / 5;
                    double r = 4.5 - (age % 12) * 0.35;
                    w.spawnParticles(BLUE_TRAIL, point.x + Math.cos(a) * r, point.y + Math.sin(a * 0.7) * 1.2,
                            point.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                }
                Fx.converge(w, ParticleTypes.END_ROD, point, 5, 3, 0.28);
                Fx.at(w, ParticleTypes.REVERSE_PORTAL, point, 8, 0.6, 0.05);
                if (age % 15 == 0) Fx.sound(w, point, SoundEvents.BLOCK_PORTAL_AMBIENT, 0.7f, 1.8f);

                // tear blocks out of the ground and suck them in
                if (Fx.blockDamage(w) && age % 2 == 0 && age < DURATION - 8) {
                    for (int i = 0; i < 4; i++) {
                        BlockPos bp = BlockPos.ofFloored(point.add(
                                (w.random.nextDouble() * 2 - 1) * 5.5,
                                (w.random.nextDouble() * 2 - 1) * 5.5,
                                (w.random.nextDouble() * 2 - 1) * 5.5));
                        if (bp.toCenterPos().squaredDistanceTo(point) > 30) continue;
                        BlockState s = w.getBlockState(bp);
                        if (!s.getFluidState().isEmpty() || !Fx.breakable(w, bp, s, 30f)) continue;
                        FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(w, bp, s);
                        fb.setNoGravity(true);
                        debris.add(fb);
                    }
                }

                // gravity well
                Box box = new Box(point.subtract(11, 11, 11), point.add(11, 11, 11));
                for (Entity e : w.getOtherEntities(p, box,
                        e -> e instanceof LivingEntity || e instanceof ItemEntity || e instanceof FallingBlockEntity)) {
                    Vec3d pull = point.subtract(e.getPos());
                    double dist = pull.length();
                    if (dist > 11) continue;
                    if (e instanceof FallingBlockEntity fb && dist < 1.4) {
                        crush(fb);
                        debris.remove(fb);
                        continue;
                    }
                    if (dist < 0.3) continue;
                    Vec3d v = pull.normalize().multiply(Math.min(0.7, 2.2 / dist));
                    e.setVelocity(e.getVelocity().multiply(0.55).add(v));
                    e.velocityModified = true;
                    if (age % 10 == 0 && e instanceof LivingEntity le && dist < 3) hit(le, p, w, 4f);
                }
                return true;
            }

            void crush(FallingBlockEntity fb) {
                if (fb.isRemoved()) return;
                Vec3d c = fb.getPos();
                w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, fb.getBlockState()),
                        c.x, c.y + 0.5, c.z, 12, 0.3, 0.3, 0.3, 0.15);
                fb.discard();
            }

            @Override
            public void cleanup() {
                Fx.kill(orb);
                for (FallingBlockEntity fb : debris) fb.discard();
            }
        });
    }

    // ================================================================ HOLLOW PURPLE — red + blue collide, then erase everything

    static void hollowPurple(ServerPlayerEntity p, ServerWorld w) {
        TickScheduler.add(new TickScheduler.Task() {
            static final int MERGE = 26;   // red + blue fuse at this tick
            static final int FIRE = 34;    // purple launches at this tick
            static final int FLIGHT = 70;  // ticks of travel
            static final float SIZE = 11f;
            Entity red, blue, purple;
            Vec3d pos, dir;
            final Set<LivingEntity> struck = new HashSet<>();

            @Override
            public boolean tick(int age) {
                if (!p.isAlive() || p.getWorld() != w) {
                    cleanup();
                    return false;
                }
                Vec3d look = p.getRotationVec(1f);
                Vec3d front = p.getEyePos().add(look.multiply(2.4));

                if (age == 0) {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, FIRE, 3, false, false));
                    Fx.sound(w, front, SoundEvents.BLOCK_CONDUIT_ACTIVATE, 2f, 0.6f);
                    Fx.sound(w, front, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 1.5f);
                }

                if (age < MERGE) {
                    double t = Math.min(1, age / (double) (MERGE - 2));
                    double sep = 2.4 * (1 - t * t);
                    double orbit = age * 0.25;
                    Vec3d off = rightOf(p).multiply(Math.cos(orbit) * sep).add(upOf(p).multiply(Math.sin(orbit) * sep * 0.35));
                    Vec3d rp = front.add(off), bp = front.subtract(off);
                    if (red == null) {
                        red = Fx.orb(w, rp, ModItems.FX_RED_ORB, 0.05f);
                        blue = Fx.orb(w, bp, ModItems.FX_BLUE_ORB, 0.05f);
                        Fx.shape(red, 0.9f, 0f, 8);
                        Fx.shape(blue, 0.9f, 0f, 8);
                    } else {
                        Fx.move(red, rp);
                        Fx.move(blue, bp);
                    }
                    Fx.converge(w, RED_DUST, rp, 1.2, 4, 0.1);
                    Fx.converge(w, BLUE_DUST, bp, 1.2, 4, 0.1);
                    if (age > 6) Fx.line(w, ParticleTypes.WITCH, rp, bp, 0.3);
                    if (age % 2 == 0) Fx.at(w, ParticleTypes.ELECTRIC_SPARK, front, 6, 0.3, 0.2);
                    if (age % 4 == 0) Fx.sound(w, front, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1f, 0.5f + age * 0.05f);
                    return true;
                }

                if (age == MERGE) {
                    Fx.kill(red);
                    Fx.kill(blue);
                    red = blue = null;
                    purple = Fx.orb(w, front, ModItems.FX_PURPLE_ORB, 0.4f);
                    Fx.shape(purple, 2.5f, 1f, 5);
                    Fx.at(w, ParticleTypes.FLASH, front, 1, 0, 0);
                    Fx.burst(w, ParticleTypes.END_ROD, front, 70, 0.5);
                    Fx.ring(w, ParticleTypes.CLOUD, front, 0.4, 40, 0.5);
                    Fx.sound(w, front, SoundEvents.BLOCK_END_PORTAL_SPAWN, 1.2f, 1.5f);
                }

                if (age < FIRE) {
                    Fx.move(purple, front);
                    Fx.converge(w, PURPLE_DUST, front, 2.5, 14, 0.25);
                    Fx.at(w, ParticleTypes.REVERSE_PORTAL, front, 10, 0.4, 0.1);
                    return true;
                }

                if (age == FIRE) {
                    dir = look;
                    pos = p.getEyePos().add(dir.multiply(4));
                    Fx.shape(purple, SIZE, 2f, 5);
                    Fx.at(w, ParticleTypes.SONIC_BOOM, front, 1, 0, 0);
                    Fx.burst(w, ParticleTypes.END_ROD, front, 120, 0.9);
                    Fx.sound(w, front, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 3f, 0.5f);
                    Fx.sound(w, front, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.8f);
                    Fx.sound(w, front, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.6f);
                    knock(p, dir.multiply(-1), 0.9, 0.15);
                }

                int f = age - FIRE;
                if (f > FLIGHT) {
                    Fx.shape(purple, 0.01f, 4f, 4);
                    Fx.at(w, ParticleTypes.FLASH, pos, 1, 0, 0);
                    Fx.burst(w, ParticleTypes.END_ROD, pos, 120, 1.0);
                    Fx.sound(w, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, 3f, 0.5f);
                    Entity dying = purple;
                    purple = null;
                    TickScheduler.add(a -> {
                        if (a >= 4) {
                            Fx.kill(dying);
                            return false;
                        }
                        return true;
                    });
                    return false;
                }

                pos = pos.add(dir.multiply(1.7));
                Fx.move(purple, pos);
                if (f % 4 == 0) Fx.shape(purple, SIZE + (float) Math.sin(f * 0.5), 2f + f * 0.4f, 4);

                if (Fx.blockDamage(w)) Fx.eraseSphere(w, pos, 3.8, 60f);

                for (LivingEntity e : livingNear(w, p, pos, 5)) {
                    if (struck.add(e)) {
                        hit(e, p, w, 34f);
                        knock(e, dir, 2.5, 0.5);
                    }
                }

                w.spawnParticles(PURPLE_TRAIL, pos.x, pos.y, pos.z, 30, 2.4, 2.4, 2.4, 0);
                Fx.at(w, ParticleTypes.REVERSE_PORTAL, pos, 25, 2.2, 0.3);
                if (f % 2 == 0) Fx.ring(w, ParticleTypes.END_ROD, pos, 3.8, 24, 0.05);
                if (f % 8 == 0) Fx.sound(w, pos, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 0.4f);
                return true;
            }

            @Override
            public void cleanup() {
                Fx.kill(red);
                Fx.kill(blue);
                Fx.kill(purple);
            }
        });
    }

    // ================================================================ DISMANTLE — three invisible slashes that cut terrain

    static void dismantle(ServerPlayerEntity p, ServerWorld w) {
        for (int k = 0; k < 3; k++) {
            final int delay = k * 3;
            final double tilt = (w.random.nextDouble() * 2 - 1) * 1.0;
            TickScheduler.add(age -> {
                if (age < delay) return true;
                if (p.isAlive()) slash(p, w, tilt);
                return false;
            });
        }
    }

    static void slash(ServerPlayerEntity p, ServerWorld w, double tilt) {
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1f);
        Vec3d along = rightOf(p).multiply(Math.cos(tilt)).add(upOf(p).multiply(Math.sin(tilt)));
        boolean breakBlocks = Fx.blockDamage(w);
        Set<LivingEntity> struck = new HashSet<>();

        Fx.sound(w, eye, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.4f, 0.55f + w.random.nextFloat() * 0.4f);
        Fx.sound(w, eye, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.6f);

        int step = 0;
        for (double d = 1.5; d <= 18; d += 0.8, step++) {
            double half = 1.0 + d * 0.13;
            Vec3d mid = eye.add(look.multiply(d));
            for (double t = -half; t <= half; t += 0.4) {
                Vec3d q = mid.add(along.multiply(t));
                w.spawnParticles(SLASH_DUST, q.x, q.y, q.z, 1, 0, 0, 0, 0);
                if (breakBlocks) {
                    BlockPos bp = BlockPos.ofFloored(q);
                    BlockState s = w.getBlockState(bp);
                    if (s.getFluidState().isEmpty() && Fx.breakable(w, bp, s, 5f)) w.breakBlock(bp, false);
                }
            }
            if (step % 3 == 0) w.spawnParticles(ParticleTypes.SWEEP_ATTACK, mid.x, mid.y, mid.z, 1, 0, 0, 0, 0);

            for (LivingEntity e : livingNear(w, p, mid, half + 0.4)) {
                if (!struck.add(e)) continue;
                hit(e, p, w, 9f);
                Vec3d c = e.getBoundingBox().getCenter();
                w.spawnParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 2, 0.3, 0.4, 0.3, 0);
                w.spawnParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 20, 0.4, 0.5, 0.4, 0.4);
                w.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, c.x, c.y, c.z, 6, 0.3, 0.3, 0.3, 0.1);
                Fx.sound(w, c, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.3f);
            }
        }
    }

    // ================================================================ REVERSE CURSED TECHNIQUE — healing helix

    static void reverseCursedTechnique(ServerPlayerEntity p, ServerWorld w) {
        p.heal(10f);
        p.removeStatusEffect(StatusEffects.POISON);
        p.removeStatusEffect(StatusEffects.WITHER);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 300, 0));

        Vec3d c0 = p.getPos().add(0, 1, 0);
        Fx.burst(w, ParticleTypes.TOTEM_OF_UNDYING, c0, 50, 0.35);
        Fx.sound(w, c0, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 1.9f);
        Fx.sound(w, c0, SoundEvents.BLOCK_BEACON_ACTIVATE, 1f, 1.8f);

        TickScheduler.add(age -> {
            if (age > 30 || !p.isAlive()) return false;
            Vec3d base = p.getPos();
            for (int strand = 0; strand < 3; strand++) {
                double a = age * 0.55 + strand * Math.PI * 2 / 3;
                double y = (age % 30) * 0.075;
                w.spawnParticles(HEAL_DUST, base.x + Math.cos(a) * 0.9, base.y + y, base.z + Math.sin(a) * 0.9,
                        1, 0, 0, 0, 0);
            }
            if (age % 3 == 0) w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, base.x, base.y + 1, base.z, 2, 0.5, 0.7, 0.5, 0);
            return true;
        });
    }

    // ================================================================ DOMAIN EXPANSION

    static void domainExpansion(ServerPlayerEntity p, ServerWorld w) {
        DomainExpansion.start(p, w);
    }
}
