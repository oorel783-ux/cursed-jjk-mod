package dev.cursedarts.fx;

import dev.cursedarts.ModBlocks;
import dev.cursedarts.TickScheduler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Domain Expansion: seals a 12-block dome of void blocks around the caster, clears the inside into a
 * starfield, and makes every attack inside a sure hit. Every block it touches is saved and put back.
 */
public final class DomainExpansion implements TickScheduler.Task {
    private static final int R = 12;
    private static final int BUILD = 16;      // ticks for the dome to rise and seal
    private static final int ACTIVE = 200;    // 10 seconds sealed
    private static final int COLLAPSE = 16;   // ticks for the dome to shatter
    private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

    /** Blocks currently owned by any domain, so two domains never fight over a block. */
    private static final Set<BlockPos> CLAIMED = new HashSet<>();

    private static final DustColorTransitionParticleEffect NEBULA = new DustColorTransitionParticleEffect(
            new Vector3f(0.85f, 0.8f, 1f), new Vector3f(0.25f, 0.05f, 0.45f), 3.0f);
    private static final DustColorTransitionParticleEffect NEBULA_BLUE = new DustColorTransitionParticleEffect(
            new Vector3f(0.6f, 0.9f, 1f), new Vector3f(0.05f, 0.1f, 0.35f), 2.5f);

    private final ServerPlayerEntity owner;
    private final ServerWorld w;
    private final BlockPos center;
    private final Vec3d c;

    private final List<BlockPos> shell = new ArrayList<>();
    private final List<BlockPos> floor = new ArrayList<>();
    private final List<BlockPos> interior = new ArrayList<>();
    private final Map<BlockPos, BlockState> saved = new HashMap<>();

    private int placed = 0;
    private int collapseAt = -1;
    private int removed = 0;

    private DomainExpansion(ServerPlayerEntity owner, ServerWorld w) {
        this.owner = owner;
        this.w = w;
        this.center = owner.getBlockPos();
        this.c = Vec3d.ofBottomCenter(center).add(0, 0.5, 0);
    }

    public static void start(ServerPlayerEntity owner, ServerWorld w) {
        DomainExpansion d = new DomainExpansion(owner, w);
        d.scan();
        TickScheduler.add(d);
    }

    private void scan() {
        for (int dx = -R - 1; dx <= R + 1; dx++)
            for (int dy = -R - 1; dy <= R + 1; dy++)
                for (int dz = -R - 1; dz <= R + 1; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (d >= R + 0.5) continue;
                    BlockPos pos = center.add(dx, dy, dz);
                    if (w.isOutOfHeightLimit(pos) || CLAIMED.contains(pos)) continue;
                    BlockState s = w.getBlockState(pos);
                    if (s.hasBlockEntity()) continue;                       // never touch chests etc.
                    if (!s.isAir() && s.getHardness(w, pos) < 0) continue;  // bedrock, other barriers

                    if (d >= R - 0.5) shell.add(pos);
                    else if (dy == -1) floor.add(pos);
                    else if (dy >= 0 && !s.isAir()) interior.add(pos);
                }
        shell.sort(Comparator.comparingInt(BlockPos::getY));
    }

    // ------------------------------------------------------------------ block bookkeeping

    private void replace(BlockPos pos, BlockState state) {
        saved.putIfAbsent(pos, w.getBlockState(pos));
        CLAIMED.add(pos);
        w.setBlockState(pos, state, FLAGS);
    }

    private void restore(BlockPos pos) {
        BlockState old = saved.remove(pos);
        if (old != null) w.setBlockState(pos, old, FLAGS);
        CLAIMED.remove(pos);
    }

    @Override
    public void cleanup() {
        for (BlockPos pos : new ArrayList<>(saved.keySet())) restore(pos);
    }

    // ------------------------------------------------------------------ timeline

    @Override
    public boolean tick(int age) {
        if (age == 0) open();

        if (age < BUILD) {
            raise(age);
            return true;
        }

        boolean ownerGone = !owner.isAlive() || owner.getWorld() != w || owner.getPos().distanceTo(c) > R + 1;
        if (collapseAt < 0 && (age >= BUILD + ACTIVE || ownerGone)) {
            collapseAt = age;
            Fx.sound(w, c, SoundEvents.BLOCK_BEACON_DEACTIVATE, 3f, 0.5f);
            Fx.sound(w, c, SoundEvents.BLOCK_GLASS_BREAK, 3f, 0.5f);
        }

        if (collapseAt < 0) {
            sealed(age);
            return true;
        }
        return collapse(age - collapseAt);
    }

    private void open() {
        Text title = Text.translatable("title.cursedarts.domain").formatted(Formatting.DARK_PURPLE, Formatting.BOLD);
        Text sub = Text.translatable("title.cursedarts.domain.sub").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC);
        for (ServerPlayerEntity sp : w.getPlayers()) {
            if (sp.getPos().distanceTo(c) > 48) continue;
            sp.networkHandler.sendPacket(new TitleFadeS2CPacket(4, 50, 14));
            sp.networkHandler.sendPacket(new SubtitleS2CPacket(sub));
            sp.networkHandler.sendPacket(new TitleS2CPacket(title));
        }

        Fx.sound(w, c, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 2.5f, 0.5f);
        Fx.sound(w, c, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.6f);
        Fx.sound(w, c, SoundEvents.BLOCK_CONDUIT_ACTIVATE, 3f, 0.5f);
        Fx.burst(w, ParticleTypes.SQUID_INK, c.add(0, 1, 0), 250, 0.9);
        Fx.ring(w, ParticleTypes.SQUID_INK, c, 1, 80, 1.2);
        Fx.at(w, ParticleTypes.FLASH, c.add(0, 1, 0), 1, 0, 0);

        // hollow out the inside and lay the glowing floor
        for (BlockPos pos : interior) replace(pos, Blocks.AIR.getDefaultState());
        BlockState floorState = ModBlocks.DOMAIN_FLOOR.getDefaultState();
        for (BlockPos pos : floor) replace(pos, floorState);

        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, BUILD + ACTIVE, 1));
        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, BUILD + ACTIVE, 1));
        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, BUILD + ACTIVE, 1));
    }

    /** The dome rises from the ground and closes over the top. */
    private void raise(int age) {
        BlockState barrier = ModBlocks.DOMAIN_BARRIER.getDefaultState();
        int target = (int) Math.ceil(shell.size() * (age + 1) / (double) BUILD);
        while (placed < target && placed < shell.size()) {
            BlockPos pos = shell.get(placed++);
            replace(pos, barrier);
            if (w.random.nextInt(8) == 0) {
                w.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        4, 0.3, 0.3, 0.3, 0.05);
            }
        }
        if (age % 3 == 0) Fx.sound(w, c, SoundEvents.BLOCK_BEACON_POWER_SELECT, 2f, 0.4f + age * 0.06f);
        if (age == BUILD - 1) {
            Fx.sound(w, c, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 3f, 0.4f);
            Fx.sound(w, c, SoundEvents.BLOCK_END_PORTAL_SPAWN, 3f, 0.7f);
        }
    }

    /** Inside the sealed domain: a silent galaxy, and nobody but the owner can move. */
    private void sealed(int age) {
        // starfield
        for (int i = 0; i < 22; i++) {
            Vec3d q = randomInside(R - 1.5);
            w.spawnParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
        for (int i = 0; i < 6; i++) {
            Vec3d q = randomInside(R - 2);
            w.spawnParticles(i % 2 == 0 ? NEBULA : NEBULA_BLUE, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
        // slowly turning galaxy disk above the owner
        for (int arm = 0; arm < 4; arm++) {
            double a = age * 0.05 + arm * Math.PI / 2;
            for (double r = 1; r < R - 2; r += 1.5) {
                double ang = a + r * 0.35;
                w.spawnParticles(NEBULA, c.x + Math.cos(ang) * r, c.y + R * 0.55, c.z + Math.sin(ang) * r,
                        1, 0.1, 0.1, 0.1, 0);
            }
        }
        if (age % 3 == 0) {
            w.spawnParticles(ParticleTypes.WAX_OFF, c.x, c.y + 0.1, c.z, 8, R * 0.4, 0.05, R * 0.4, 0);
        }

        // owner keeps buffs while inside
        if (age % 20 == 0) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 1));
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 1));
            Fx.sound(w, c, SoundEvents.BLOCK_BEACON_AMBIENT, 2f, 0.5f);
        }

        // sure-hit on everyone else
        Box box = new Box(c.subtract(R, R, R), c.add(R, R, R));
        List<LivingEntity> victims = w.getEntitiesByClass(LivingEntity.class, box,
                e -> e != owner && e.isAlive() && e.getPos().squaredDistanceTo(c) < (R - 0.5) * (R - 0.5));
        for (LivingEntity e : victims) {
            Vec3d v = e.getVelocity();
            e.setVelocity(0, Math.min(0, v.y), 0);
            e.velocityModified = true;
            if (age % 10 == 0) {
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 7, false, false));
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 2, false, false));
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 30, 2, false, false));
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0, false, false));
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80, 0, false, false));
            }
            // "infinite information" pouring into their head
            Vec3d head = e.getEyePos();
            for (int i = 0; i < 4; i++) {
                w.spawnParticles(ParticleTypes.ENCHANT, head.x, head.y, head.z, 0,
                        w.random.nextGaussian() * 1.5, w.random.nextGaussian() * 1.5, w.random.nextGaussian() * 1.5, 1);
            }
            if (age % 20 == 0) {
                e.timeUntilRegen = 0;
                e.damage(w.getDamageSources().indirectMagic(owner, owner), 5f);
                w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, head.x, head.y, head.z, 10, 0.3, 0.4, 0.3, 0.02);
            }
        }
    }

    /** The dome shatters from the top down, then the land returns to how it was. */
    private boolean collapse(int t) {
        int target = (int) Math.ceil(shell.size() * Math.min(1.0, (t + 1) / (double) COLLAPSE));
        BlockState barrier = ModBlocks.DOMAIN_BARRIER.getDefaultState();
        while (removed < target && removed < placed) {
            BlockPos pos = shell.get(placed - 1 - removed++);
            restore(pos);
            if (w.random.nextInt(6) == 0) {
                w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, barrier),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.2);
            }
        }
        if (t % 4 == 0) Fx.sound(w, c, SoundEvents.BLOCK_GLASS_BREAK, 2f, 0.4f + t * 0.03f);
        if (removed < placed) return true;

        for (BlockPos pos : floor) restore(pos);
        for (BlockPos pos : interior) restore(pos);
        cleanup();
        Fx.burst(w, ParticleTypes.END_ROD, c.add(0, 2, 0), 120, 0.6);
        return false;
    }

    private Vec3d randomInside(double radius) {
        double u = w.random.nextDouble();             // upper hemisphere only
        double th = w.random.nextDouble() * Math.PI * 2;
        double s = Math.sqrt(1 - u * u);
        double r = radius * Math.cbrt(w.random.nextDouble());
        return c.add(r * s * Math.cos(th), r * u, r * s * Math.sin(th));
    }
}
