# Changelog

## 1.47.0
### Added
- `jei.ArchaicJei.registerInfoCategory`: JEI/HEI info pages (InfoEntry/Layer) - structure pattern layers (cycled, hover tooltips), rotating entity preview, auto-counted materials, extra item slots and wrapped description text. For mobs built from blocks, shrines, NPC jobs.

## 1.46.1
### Fixed
- `LegacyChunkGenerator`: crash (ConcurrentModificationException in StructureStart.generateStructure) when entering a legacy dimension with structures (e.g. Shadow World): re-entrant populate calls from cascading chunk loads are now deferred until the outer structure pass finishes.

## 1.46.0
### Added
- `world.ParallelRays`: runs an old explosion's ray-cast slices on all cores (read-only phase; slices touching unloaded chunks are replayed on the server thread, result identical).
- `BlockPosSet` accepts adds from worker threads (per-thread shards) and skips consecutive duplicate ray steps.

## 1.45.0
### Added
- `util.BlockPosSet`: long-keyed `Set<BlockPos>` (1.12 BlockPos.hashCode collides badly; big blasts spent seconds in HashSet.add).
- `BlastBatch.packetSample(list)`: caps the block list sent in `SPacketExplosion` (client froze breaking 100k+ blocks with particles).

## 1.44.1
### Changed
- `BlastBatch` resends touched chunks once at the end of the server tick (TNT chains no longer resend the same chunks per blast).
### Added
- `BlastBatch.rayGrid(configured, power, stepScale)`: caps an old explosion's ray grid to what its radius needs.

## 1.44.0
### Added
- `world.BlastBatch`: open/close around a mass block edit (old copy-pasted explosions) - setBlockState writes straight into chunks without per-block lighting, neighbour cascades or block packets; on close one border neighbour pass, lazy relight and whole-chunk resend. Backed by a `World.setBlockState` mixin.

## 1.43.0
### Added
- `legacy.world.LegacyStructureComponent`: the 1.7 `StructureComponent` API (int `coordBaseMode`, Block+meta placement without 1.12 rotation, `fillWithBlocks`/`fillWithMetadataBlocks`/`fillWithRandomizedBlocks`, the 1.7 `getMetadataWithOffset`, `BlockSelector`, chest/door helpers) so decompiled 1.7 villages/strongholds port line by line.
- `legacy.world.LegacyChestContent`: 1.7 `WeightedRandomChestContent` + its chest fill algorithm.
- `LegacyChunkGenerator.structure(MapGenStructure, aliases...)`: mod structures (1.7 villageGenerator etc.) recorded per chunk, placed in populate, found by `/locate` and `findNearestStructure` (1.7 names as aliases).
- `LegacyClassStructure` now also records the chest and spawner set-up of replayed 1.7 schematics (`getTileEntity` -> `generateChestContents(rand, Loot, te, n)` / `setEntityName("Mob")`) and hands them to a new `TileListener` after placing the blocks.
- `client.texture.AngleSprite`: 1.7 custom-compass sprite (animated strip whose frame follows an angle, with the 1.7 needle damping).
- `legacy.world.LegacyChunkedStructure`: one or more replayed `LegacyClassStructure`s at a fixed origin, pre-split per chunk and written straight into each chunk's `ChunkPrimer` during generation (no cascading loads) - for giant 1.7 schematics that the original placed from a single populate call.
- `IArchaicRenderBlock.RENDER_LADDER` (1.7 render type 8): a double-sided plane 0.05 from the wall picked by the 1.7 meta 2..5 (signs, notes, ladders and other wall-hung flat blocks).

## 1.42.0
### Added
- `original.OriginalDownloader`: ARR ports no longer need the player to hunt for the original mod file. Every port ships `archaic_originals.json` (file name, SHA-256, size, original page, download mirrors - CurseForge CDN first, then the authors' official pages/GitHub/MCArchive/Wayback copies of the official site). At start-up ArchaicLib downloads missing files into `archaic_originals/`, checks size + SHA-256, and tries the next mirror on failure.
- A mirror may serve a zip: the manifest's `inner` names the file to extract from it.
- `alternatives` in a manifest entry: another acceptable build (own hash/mirrors) tried when all mirrors of the main file fail.
- Mirrors may be `scrape:<page> <regex>`: the page is opened and the current download link (sites with expiring links) is taken from it; the SHA-256 check still decides.
- Short stall timeout (`stallTimeoutSeconds`, default 5): a dead link is skipped after 5 s of silence, a slow but steady download is never cut off.
- Loading-screen progress bar while downloading (file name and MB done / total).
- Per-mod editable sources: the manifest is copied to `config/archaiclib/originals/<port>.json`; edit links there (set `"locked": true` to keep your edits across updates). `config/archaiclib/originals.cfg`: `autoDownload` (default true), `stallTimeoutSeconds`.
- Main-menu banner when something could not be downloaded; click it for the list with "Download" links, "Open folder" and "Retry download". One chat message with clickable links after joining a world.
### Changed
- The missing-originals screen no longer pops up by itself on the main menu; it opens from the banner.

## 1.41.0
### Added
- `legacy.world.LegacyChestGenHooks`: 1.7 ChestGenHooks.addItem categories (dungeonChest, pyramidJungleChest, ...) on 1.12 loot tables, plus `fillChest` for 1.7 generateChestContents in world gen.
- `LegacyArrows.shot(world, shooter, velocity)`: 1.7 `new EntityArrow(world, shooter, velocity)` bow shot.

## 1.40.1
### Fixed
- `LegacyModSounds.autoRegister` crashed ("missing /assets/<modid>/sounds.json") for ports that ship no original assets: sounds.json is now read from the original jar in `archaic_originals`; if it is nowhere, a warning is logged instead of crashing.

## 1.40.0
### Added
- `util.FastRandom`: drop-in `java.util.Random` (SplitMix64, no atomic CAS per call) + `FastRandom.shared()` per-thread instance; ported mods use it in place of `new Random()`.
- `LegacyWorld.rpos(x,y,z)`: per-thread reusable read-only position.
### Changed (performance)
- `LegacyBlockIds.state(block, meta)` caches the 16 meta states per block (no getStateFromMeta/try-catch per setBlock).
- `LegacyWorld.getBlock/getBlockMetadata/isAirBlock/blockExists` and `LegacyBlockIds.getBlock/getMeta` no longer allocate a BlockPos.
- `LegacyWorld.setBlock` rejects y outside 0..255 before allocating; `LegacyBlockIds.setBlock` routes through it.
- `LegacySounds.get` caches registry lookups of non-legacy sound names.

## 1.39.1
### Changed
- Staged sapling/grass growth runs the real vanilla generators (`ChunkStagedGen.deferOp`) instead of a simplified copy.

## 1.39.0
### Added
- `ChunkStagedGen.registerOp/deferOp`: persistent deferred real-world operations (vanilla generators) run as soon as their whole area is loaded and has its staged blocks; never load a chunk.
- `world.StagedWorld`: drop-in `LegacyWorld` facade for old structure generators; inside a `ChunkStagedGen` job all reads/writes (blocks, tile entities, height, top solid block, sky, biome, freeze); sapling/grass growth uses the real vanilla generators via `deferOp` go to the staged session, outside it behaves like `LegacyWorld`.
- `ChunkStagedGen.spawnEntity`: structure entities are spawned only when their chunk really exists (stored as NBT with the chunk's pending blocks and spawned right after them); never loads a chunk.

## 1.38.0
### Added
- `client.skin.LegacyPlayerSkins`: skin of any account by player name (Mojang profile service + vanilla SkinManager, 64x64/slim, cached; default Steve/Alex while loading/offline) - replacement for the dead skins.minecraft.net / s3 MinecraftSkins downloads; `isModernLayout(texture)` (64x64 vs 64x32 skin).
- `world.DeferredWorldGen`: run old populate-time structure code at the end of the server tick (no cascading worldgen).
- `LegacyWorld`: getHeightValue, getTopBlock, getTopSolidOrLiquidBlock, canBlockSeeTheSky, canBlockFreeze, doesBlockHaveSolidTopSurface, growSapling, grow, newPainting (1.7 direction).
- `LegacyEntityHelper.copyDataFrom` (1.7 Entity.copyDataFrom).
- `ArchaicItem.registerMetaTextureLayers`: multi-layer generated item models (1.7 multi render pass items, spawn eggs).
### Fixed
- `LegacyRecipes.addRecipe`: 1.7-style sloppy shaped recipes (undefined pattern chars = empty slot, unused/null keys) no longer crash on 1.12.

## 1.37.0
### Added
- `OriginalModAssets.mapDomain(portDomain, originalDomain)`: serve an original jar domain under the port's own modid (the original domain is no longer exposed, so it cannot clash with the real mod).

## 1.24.2
### Fixed
- Villagers walked into door frames and then popped onto the wall top: the 1.24.1 "passed the centre" arrival now only applies to really fast mobs (>0.22 b/t) and never at doors, gates or 1-wide gaps; `SmartGroundNavigator` pop-out ignores doors/gates/fences closing on the mob (only full solid blocks count), searches at most 1 block up and prefers the same level.

## 1.24.1
### Fixed
- `ai.SmartMoveHelper`: walking speed is capped at speed x BASE movement attribute x 1.25 (speed potions/modifiers no longer make mobs run past waypoints and dither), and mobs brake near the target instead of overshooting. Optional absolute cap `setMaxAiSpeed`.
- `ai.path.PathAgent`: a mob that ran past a cell centre counts as arrived (no turning back); the path index catches up when it is already in the next cell; obstacles are dug only while the mob stands at the move's start/end cell, and it stands still while digging (fast tools no longer break blocks that are not in its way).

## 1.24.0
### Added
- JEI: recipes registered with `LegacyRecipes.addConditionalRecipe` / `addConditionalShapelessRecipe` are hidden while their condition is false (old mods swapped recipes at runtime, e.g. easy/hard variants) - no more two recipes for one item where only one works.
- `archaic_originals` now also accepts `.pak` files (zip sound packs the original mods downloaded).

## 1.23.2
### Fixed
- `ai.path.PathSearch`: short trips round a long wall (a row of houses with all doors on the far side) no longer end in "no path": the node floor is now 8000 (10000 for goals above), and a search may run 1200 ticks under the shared time budget instead of 400.

## 1.23.1
### Fixed
- VintageFix compatibility: textures served by ArchaicLib resource packs (original-jar assets, 1.8 model converter) are now listed for VintageFix's dynamic texture pass - items/blocks of ported mods are no longer purple-black with VintageFix installed.

## 1.23.0
### Fixed
- `ai.path.PathAgent`: a mob no longer stands forever digging at a block that never goes (dig work per move is limited, then the cell is avoided and the path planned anew), nor pushes for the full move timeout against something the plan did not see (no step closer for 30 ticks: one jump, then replan).
- Cells that keep failing get dearer each time (40, 160, 640... ticks), so the next plans really go round them instead of back into the same corner.
- Standing on a slab, the lower half of a stair or a bed: the path is planned from the cell above that block, not from inside it.
- Paths never dig a block that a torch, lever, button, sign, ladder, trapdoor, door, rail, carpet or a small tile-entity block (mod markers) is fixed to. New `TerrainAssist.supportsSomething(world, pos)`.
- Goals above the mob get a bigger node budget (4000) so pillars and steps are actually tried before "no path".
- No cell to plan from for 3 seconds (wedged, hanging): the agent reports FAILED instead of searching forever.

## 1.22.0
### Added
- `world.ChunkBlast`: huge spherical explosions without freezing the server - carved chunk by chunk (nearest first) under a per-tick time budget, so it spreads as a visible shock wave. Whole sections are dropped at once, edge sections cleared without neighbour updates; liquids and tile entities go too, bedrock-hard blocks optionally kept; saved-but-unloaded chunks are loaded from disk and wiped (never generated); entities in the volume go to a callback; wave-front particles for nearby players.

## 1.21.0
### Added
- `ai.path`: a path planner and follower for ground mobs that dig and build (the approach of Baritone, written anew for 1.12):
  - `PathSearch`: incremental A* over moves (walk, diagonal without corner clipping, jump up, step down, fall, ladders, water surface, dig down, pillar up). Costs are in ticks, so a block's real dig time competes with walking around it. Doors and gates are passed square-on only. Never loads chunks. All searches share a 3 ms per tick budget. Far goals get the best partial path and the next piece from its end; a near goal that is walled in gets "no path", not a walk into the nearest corner.
  - `PathAgent`: walks the path cell centre to cell centre, digs and places through `Hooks`, re-checks the next moves against the world, replans when pushed off or blocked, and makes a cell that keeps failing expensive for a while.
  - `PathRules` (what the mob may break or place, penalties: farmland, crops, tree tops), `Goal.near/at`, `CellCache`, `PathCosts`.
### Changed
- `ai.SmartGroundNavigator`: own path following - doors, gates, trapdoors and one-wide gaps are entered square-on (the mob lines up with the cell centre first, no shortcut past or diagonally into them); shortcuts must be clear along both body edges, not only the centre line. Fixes mobs wedged in door frames and on corners.
### Added
- `SmartGroundNavigator.isPassageBlock`, `TerrainAssist.isHarmlessPlacement(world, pos[, checkFloor])`: no helper blocks near doors/gates/ladders/beds/stairs/paths/farmland or on non-natural floors.

## 1.20.0
### Added
- `client.texture.CompositeTexture`: runtime texture built from another (e.g. another mod's) texture - bigger canvas, copy/recolor regions and `box(...)` that paints a full ModelBox UV layout sampled from a base region (saturation/brightness/noise, shaded faces, outlined edges). Lets a port add bones to a foreign model without shipping images; rebuilt on resource reload, HD-pack aware.
- `client.model.ModelAnim`: procedural animation helpers (ease/window/pulse, attack wind-up/strike/recover weights, additive and mirrored rotations).
- `client.model.ScalableModelRenderer`: ModelRenderer with its own scale around its pivot.

## 1.18.0
### Added
- `structure.Blueprint`: buildings in the vanilla structure template format (structure block `.nbt`): read/write, capture from the world, `Placement` (rotation + mirror like vanilla templates, "in front of an anchor" placement, ground layer), optional `archaicFront`/`archaicGround` keys.
- `structure.BlockItems`: survival building rules per block state - item it takes (double slabs, buckets, seeds, dirt for grass/farmland), lenient "already built" matching, build phase (solid / attached / liquid), companion placement (doors, beds, tall plants).
- `BlockItems.isFarmCrop`, `placedState` (dirt for farmland/path/grass), `isSource`, `isInfiniteWater`; `Blueprint.Entry` constructor is public.
- `client.GhostBlockRenderer`: see-through hologram of blocks baked into a display list (blueprint projections).

## 1.17.0
### Added
- `world.ToolChoice`: best tool from a bag for a block (drops first, then speed), `isShovelBlock`, effective dig speed.
- `ai.TerrainAssist.sightBlocker(mob, pos)`: first block between the mob's eyes and a target block (dig-through for ore behind a wall).

## 1.16.0
### Fixed
- Item models of normal 1.12 mods were rendered through the 1.8 legacy engine when the mod jar has no `pack.mcmeta` (blocks on the ground too big, swords/buckets wrong in hand).
- 1.7 `IItemRenderer` items (scythes, portal guns, other 3D items) now use the exact 1.7.10 transforms in first person, third person, GUI and on the ground, including the non-helper equipped path; the holder entity is passed to renderers again. Left hand is a mirror of the right.

## 1.15.0
### Added
- `ai.SmartMoveHelper`: move helper that jumps when a step-up is blocked (vanilla never jumps with stepHeight 1), no spinning around a target straight above/below, paddles against water currents.
- `ai.TerrainAssist`: plans one block action (break / place step / bridge / pillar) to get a stuck ground mob toward its target; `isNaturalTerrain` filter for world-generated blocks.

## 1.14.0
Big update since 1.5.1 (everything below is now available to ports).

### Added
- **1.14.0** - `LegacyWorld` / `LegacyMOP` (1.7 world and ray-trace calls), `LegacyTileEntity`, `LegacyItem`, `LegacyEntityHelper` (look vector, riders, death sounds), `LegacyRenderBlocks`, conditional recipes, held-item renderers (3D model only in hand).
- **1.13.0** - `LegacyMetaBlock` (raw 0..15 metadata blocks with per-meta bounds), `LegacyTechneTESR`, `LegacyBiomes` (server-safe biome names).
- **1.12.0** - `LegacyFluids` and `LegacyBucketHandler` (1.7 fluids and buckets).
- **1.11.0** - legacy particle names, chat broadcast, numeric block ids + meta, server-safe reading of original-mod data files; more 1.7 sound names.
- **1.10.0** - automatic SoundEvent registration from sounds.json, legacy spawn helpers, generic textured living renderer, spawn eggs in the entity registry helper.
- **1.9.0** - `DigSafety` for digging NPCs (liquids, lava, safe standing spots, quench, harvest check).
- **1.8.0** / **1.7.0** - `RecipeGrid` (real crafting with remaining items), ghost recipe grid + JEI/HEI transfer.
- **1.6.0** - smart ground navigator (doors, fence gates, leaves, stuck watchdog).

### Fixed
- 1.7 item renderers now face the right way in the inventory (yaw 225 -> -45).
