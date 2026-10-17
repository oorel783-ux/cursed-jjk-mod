# Cursed Arts — a JJK-inspired magic mod (Fabric 1.21.1)

Every player has **Cursed Energy (CE)**: 100 to start, regenerates 2/sec, shown in a purple bar bottom-left.
Right-click a technique to cast it. All items are in the **Cursed Arts** creative tab.

| Item | CE | Cooldown | What it does |
|---|---|---|---|
| Red | 25 | 1.5s | Repulsion blast, 8 dmg, launches mobs away |
| Blue | 25 | 3s | Singularity where you look, pulls mobs and items in for 3s |
| Hollow Purple | 80 | 10s | 48-block beam, 26 dmg, explosion at the end (no block damage) |
| Dismantle | 15 | 0.75s | Invisible slashes in a cone, 10 dmg |
| Reverse Cursed Technique | 30 | 3s | Heal 5 hearts, Regen II, cures poison and wither |
| Domain Expansion | 100 | 60s | 14-block barrier for 10s: sure-hit damage plus slowness and darkness on everyone inside; you get Strength II and Resistance II |
| Black Flash Knuckles | — | — | Netherite-tier weapon, 20% chance per hit: +14 dmg, +20 CE |
| Cursed Talisman | — | — | Consumable: +25 max CE (cap 500), full refill |

## Recipes
- **Cursed Talisman** (shapeless): paper + amethyst shard + ink sac + lapis
- **Red**: amethyst shards around a redstone block, ender eye below
- **Blue**: amethyst shards around a lapis block, ender eye below
- **Hollow Purple** (shapeless): Red + Blue + 2 echo shards
- **Dismantle**: amethyst shards around a diamond sword, ender eye below
- **Reverse Cursed Technique**: amethyst shards around a golden apple, talisman below
- **Domain Expansion**: 8 crying obsidian around a nether star
- **Black Flash Knuckles**: `B N B / B T B` (blaze powder, netherite ingot, talisman)

## Building the jar
**Easiest (no installs): GitHub**
1. Make a new GitHub repo and upload this whole folder (including `.github`).
2. Open the **Actions** tab. The "Build mod" workflow runs automatically.
3. When it finishes, download **cursed-arts-jar**. The jar is inside.

**Locally:** install JDK 21 and Gradle 8.10, then run `gradle build` in this folder. The jar ends up in `build/libs/`.
You can also open the folder in IntelliJ IDEA with the Minecraft Development plugin.

## Installing
Install Fabric Loader for 1.21.1. Put `cursed-arts-1.0.0.jar` and **Fabric API** into `.minecraft/mods`.
