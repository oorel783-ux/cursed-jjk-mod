# Cursed Arts — a JJK-inspired magic mod (Fabric 1.21.1)

Every player has **Cursed Energy (CE)**: 100 to start, regenerates 2/sec, shown in an animated bar bottom-left.
Right-click a technique to cast it. Everything is in the **Cursed Arts** creative tab.

| Technique | CE | Cooldown | What it does |
|---|---|---|---|
| Red | 25 | 1.5s | Charges a glowing red orb in your hand, fires it, and it detonates on impact (blows up terrain, launches mobs) |
| Blue | 25 | 3s | Opens a spinning singularity: tears blocks out of the ground, pulls mobs, items and debris in, then implodes |
| Hollow Purple | 80 | 10s | Red and Blue orbit and fuse in front of you, then a giant purple orb flies ~120 blocks, erasing a tunnel through everything (34 dmg) |
| Dismantle | 15 | 0.6s | Three invisible slashes that cut mobs, trees and stone |
| Reverse Cursed Technique | 30 | 3s | Heal 5 hearts plus regen and absorption, with a healing helix |
| Domain Expansion | 100 | 60s | A void dome rises and seals a 12-block sphere around you. Inside it's a starfield; everyone but you is frozen, blinded and takes sure-hit damage. After 10s it shatters and the land is restored exactly |
| Black Flash Knuckles | — | — | Netherite-tier fist weapon, 20% chance per hit: black lightning, +14 dmg, +20 CE |
| Cursed Talisman | — | — | Consumable: +25 max CE (cap 500), full refill |

**Block destruction** is on by default. Turn it off with `/gamerule cursedartsBlockDamage false`.
Chests and other containers are never destroyed. Domain Expansion always puts every block back.

## Recipes
- **Cursed Talisman** (shapeless): paper + amethyst shard + ink sac + lapis
- **Red**: amethyst shards around a redstone block, ender eye below
- **Blue**: amethyst shards around a lapis block, ender eye below
- **Hollow Purple** (shapeless): Red + Blue + 2 echo shards
- **Dismantle**: amethyst shards around a diamond sword, ender eye below
- **Reverse Cursed Technique**: amethyst shards around a golden apple, talisman below
- **Domain Expansion**: 8 crying obsidian around a nether star
- **Black Flash Knuckles**: `B N B / B T B` (blaze powder, netherite ingot, talisman)

## Building
Push to GitHub and the **Actions** tab builds the jar. Or run `gradle build` locally with JDK 21 and Gradle 8.10.

## Installing
Fabric Loader 1.21.1, plus Fabric API, plus this jar in `.minecraft/mods`.
