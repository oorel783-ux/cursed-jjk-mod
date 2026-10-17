package dev.cursedarts.fx;

import dev.cursedarts.CursedArts;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Visual helpers. Orbs are vanilla item_display entities showing our custom 3D glowing models,
 * so they render smoothly with no client-side rendering code.
 */
public final class Fx {
    public static final String TAG = "cursedarts_fx";
    private static final Set<UUID> LIVE = new HashSet<>();
    private static final List<Entity> LIVE_ENTITIES = new ArrayList<>();
    private static final List<Entity> TO_DISCARD = new ArrayList<>();

    private Fx() {}

    // ------------------------------------------------------------------ orb display entities

    public static Entity orb(ServerWorld w, Vec3d pos, Item model, float scale) {
        NbtCompound n = new NbtCompound();
        n.putString("id", "minecraft:item_display");

        NbtCompound stack = new NbtCompound();
        stack.putString("id", Registries.ITEM.getId(model).toString());
        stack.putInt("count", 1);
        n.put("item", stack);
        n.putString("item_display", "none");

        NbtCompound brightness = new NbtCompound();
        brightness.putInt("sky", 15);
        brightness.putInt("block", 15);
        n.put("brightness", brightness);

        n.put("transformation", transformation(scale, 0f));
        n.putInt("teleport_duration", 1);
        n.putFloat("view_range", 4f);
        n.putFloat("shadow_radius", 0f);

        NbtList tags = new NbtList();
        tags.add(NbtString.of(TAG));
        n.put("Tags", tags);

        Entity e = EntityType.loadEntityWithPassengers(n, w, ent -> {
            ent.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0f, 0f);
            return ent;
        });
        if (e == null) return null;
        LIVE.add(e.getUuid());
        LIVE_ENTITIES.add(e);
        w.spawnEntity(e);
        return e;
    }

    /** Smoothly scale + spin an orb over {@code ticks} ticks. */
    public static void shape(Entity e, float scale, float spinRadians, int ticks) {
        if (e == null || e.isRemoved()) return;
        NbtCompound n = e.writeNbt(new NbtCompound());
        n.put("transformation", transformation(scale, spinRadians));
        n.putInt("start_interpolation", 0);
        n.putInt("interpolation_duration", ticks);
        e.readNbt(n);
    }

    public static void move(Entity e, Vec3d pos) {
        if (e != null && !e.isRemoved()) e.setPosition(pos.x, pos.y, pos.z);
    }

    public static void kill(Entity e) {
        if (e == null) return;
        LIVE.remove(e.getUuid());
        LIVE_ENTITIES.remove(e);
        e.discard();
    }

    public static void killAll() {
        for (Entity e : new ArrayList<>(LIVE_ENTITIES)) e.discard();
        LIVE.clear();
        LIVE_ENTITIES.clear();
    }

    /** Leftover orbs saved into a chunk (e.g. after a crash) are removed when they load. */
    public static void onEntityLoad(Entity e) {
        if (e.getCommandTags().contains(TAG) && !LIVE.contains(e.getUuid())) TO_DISCARD.add(e);
    }

    public static void tick() {
        if (TO_DISCARD.isEmpty()) return;
        for (Entity e : TO_DISCARD) if (!e.isRemoved()) e.discard();
        TO_DISCARD.clear();
    }

    private static NbtCompound transformation(float scale, float spin) {
        NbtCompound t = new NbtCompound();
        t.put("left_rotation", floats(0f, (float) Math.sin(spin / 2), 0f, (float) Math.cos(spin / 2)));
        t.put("right_rotation", floats((float) Math.sin(spin / 3), 0f, 0f, (float) Math.cos(spin / 3)));
        t.put("translation", floats(0f, 0f, 0f));
        t.put("scale", floats(scale, scale, scale));
        return t;
    }

    private static NbtList floats(float... values) {
        NbtList list = new NbtList();
        for (float f : values) list.add(NbtFloat.of(f));
        return list;
    }

    // ------------------------------------------------------------------ particles

    public static void at(ServerWorld w, ParticleEffect p, Vec3d c, int count, double spread, double speed) {
        w.spawnParticles(p, c.x, c.y, c.z, count, spread, spread, spread, speed);
    }

    /** Particles flying outward along a horizontal ring (shockwave). */
    public static void ring(ServerWorld w, ParticleEffect p, Vec3d c, double radius, int points, double outSpeed) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            double cx = Math.cos(a), cz = Math.sin(a);
            w.spawnParticles(p, c.x + cx * radius, c.y, c.z + cz * radius, 0, cx, 0, cz, outSpeed);
        }
    }

    /** Particles flying outward in every direction (burst). */
    public static void burst(ServerWorld w, ParticleEffect p, Vec3d c, int points, double speed) {
        for (int i = 0; i < points; i++) {
            double u = w.random.nextDouble() * 2 - 1;
            double th = w.random.nextDouble() * Math.PI * 2;
            double s = Math.sqrt(1 - u * u);
            w.spawnParticles(p, c.x, c.y, c.z, 0, s * Math.cos(th), u, s * Math.sin(th), speed);
        }
    }

    /** Particles on the surface of a sphere, drifting inward (charging). */
    public static void converge(ServerWorld w, ParticleEffect p, Vec3d c, double radius, int points, double speed) {
        for (int i = 0; i < points; i++) {
            double u = w.random.nextDouble() * 2 - 1;
            double th = w.random.nextDouble() * Math.PI * 2;
            double s = Math.sqrt(1 - u * u);
            double dx = s * Math.cos(th), dy = u, dz = s * Math.sin(th);
            w.spawnParticles(p, c.x + dx * radius, c.y + dy * radius, c.z + dz * radius, 0, -dx, -dy, -dz, speed);
        }
    }

    public static void line(ServerWorld w, ParticleEffect p, Vec3d a, Vec3d b, double step) {
        Vec3d d = b.subtract(a);
        double len = d.length();
        if (len < 1e-4) return;
        Vec3d dir = d.multiply(1 / len);
        for (double t = 0; t <= len; t += step) {
            Vec3d q = a.add(dir.multiply(t));
            w.spawnParticles(p, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    public static void sound(ServerWorld w, Vec3d pos, SoundEvent s, float volume, float pitch) {
        w.playSound(null, pos.x, pos.y, pos.z, s, SoundCategory.PLAYERS, volume, pitch);
    }

    // ------------------------------------------------------------------ block damage

    public static boolean blockDamage(ServerWorld w) {
        return w.getGameRules().getBoolean(CursedArts.BLOCK_DAMAGE);
    }

    /** True for blocks a technique may destroy: not air, not unbreakable, no chests/containers, not too hard. */
    public static boolean breakable(ServerWorld w, BlockPos pos, BlockState s, float maxHardness) {
        if (s.isAir() || s.hasBlockEntity()) return false;
        if (!s.getFluidState().isEmpty() && s.getBlock() == s.getFluidState().getBlockState().getBlock()) return true;
        float h = s.getHardness(w, pos);
        return h >= 0 && h <= maxHardness;
    }

    /** Erases (no drops) every breakable block within radius. Returns how many were erased. */
    public static int eraseSphere(ServerWorld w, Vec3d c, double r, float maxHardness) {
        int n = 0;
        int ir = (int) Math.ceil(r);
        BlockPos center = BlockPos.ofFloored(c);
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -ir; dx <= ir; dx++)
            for (int dy = -ir; dy <= ir; dy++)
                for (int dz = -ir; dz <= ir; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r) continue;
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (w.isOutOfHeightLimit(m)) continue;
                    BlockState s = w.getBlockState(m);
                    if (!breakable(w, m, s, maxHardness)) continue;
                    if (n < 6 && w.random.nextInt(4) == 0) {
                        w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, s),
                                m.getX() + 0.5, m.getY() + 0.5, m.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.1);
                    }
                    w.setBlockState(m, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                    n++;
                }
        return n;
    }
}
