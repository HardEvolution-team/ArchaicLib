# Changelog

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
