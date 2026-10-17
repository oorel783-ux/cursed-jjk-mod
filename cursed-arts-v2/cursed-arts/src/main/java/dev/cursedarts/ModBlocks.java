package dev.cursedarts;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

/** Temporary blocks used by Domain Expansion. Unbreakable, never dropped; always restored afterwards. */
public final class ModBlocks {
    public static final Block DOMAIN_BARRIER = register("domain_barrier", new Block(AbstractBlock.Settings.create()
            .strength(-1.0f, 3600000.0f)
            .dropsNothing()
            .luminance(state -> 2)
            .sounds(BlockSoundGroup.AMETHYST_BLOCK)));

    public static final Block DOMAIN_FLOOR = register("domain_floor", new Block(AbstractBlock.Settings.create()
            .strength(-1.0f, 3600000.0f)
            .dropsNothing()
            .luminance(state -> 6)
            .sounds(BlockSoundGroup.AMETHYST_BLOCK)));

    private ModBlocks() {}

    public static void init() {}

    private static Block register(String name, Block block) {
        return Registry.register(Registries.BLOCK, CursedArts.id(name), block);
    }
}
