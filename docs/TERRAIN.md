# OWCraft — PR #3: deterministic seeded terrain

This feature adds real OW code for game-specific terrain. It uses **OW-Engine's game bootstrap and flOW**, but the heightfield and block placement logic stays in OWCraft. The entire feature works without OpenGL.

## Terrain model

`TerrainGenerator` uses a deterministic integer lattice value-noise model:

1. Divide X/Z world block coordinates into **8×8 cells**, using signed floor division.
2. Hash the four integer lattice corners with a bounded, seed-mixed integer function.
3. Bilinearly interpolate the heights at 1-block resolution.
4. Fill chunks from Y=0 through Y=31 with a surface height of **5–19** inclusive.

There is **no dependence on system time, random state or platform-specific floating-point math**. The same seed and (X,Z) produce the same height, including across negative coordinates and adjacent chunk boundaries.

Terrain material assignment:
- **Grass (ID 1)** at surface Y = height.
- **Dirt (ID 2)** for the three layers immediately below grass.
- **Stone (ID 3)** underneath those layers, down to Y=0.
- **Water (ID 5)** fills empty lowlands up to Y=8 (where the surface is lower).
- **Air (ID 0)** above the surface and water.

The current implementation is an intentionally simple, game-specific **heightfield**, not Perlin/Simplex noise, overhangs, caves or biomes.

## APIs

```ow
set game: new CraftGame()
call: assert(game.start() == 1)
call: assert(game.set_world_seed(42) == 1)
call: assert(game.terrain_height(0, 0) == 12)
call: assert(game.generate_terrain_column(0, 0) == 1)
call: assert(game.get_block(0, 12, 0) == 1)
call: assert(game.place_block(0, 12, 0, 4) == 1)
call: assert(game.generate_terrain_column(0, 0) == 0)
call: assert(game.get_block(0, 12, 0) == 4)
call: assert(game.stop() == 1)
```

The world seed is configured during game startup by `CraftTerrainModule` (default **12345**) and may be changed to any integer from **0 through 1,000,000** while the world has no chunks. Seed changes after any chunk allocation are rejected to avoid mixing terrain regimes.

- `terrain_height(world_x,world_z)` returns an integer height or -1 if out of bounds.
- `terrain_generator().material_at(x,y,z)` calculates a virtual block ID without loading chunks; allowed terrain Y 0..31.
- `generate_terrain_chunk(cx,cy,cz)` generates one previously-unloaded chunk. Only `cy=0` or `cy=1` is supported.
- `generate_terrain_column(cx,cz)` generates both vertical chunks. Preflight requires two vacant slots and neither existing.
- `generate_terrain_region(cx,cz,width,depth)` generates a bounded rectangular grid of columns, with width/depth **1–3**. Preflight rejects invalid dimensions, collisions or inadequate capacity **before any chunk is written**.
- All generator methods return 1 on successful creation and 0 on rejection. They do **not** regenerate already loaded chunks; this preserves modifications such as manually placed or broken blocks.

**Capacity:** `WorldStore` currently holds 32 resident 16³ chunks. A 3×3 region allocates 18 chunks, and only if enough slots are free. A missing/unloaded chunk remains distinct from loaded air.

## Try it

```sh
ow deps install
ow run examples/terrain_demo/main.ow
ow run tests/headless_terrain_height.ow
ow run tests/headless_terrain_chunks.ow
ow run tests/headless_terrain_region.ow
ow run tests/headless_terrain_game.ow
```

The reference tests pin expected heights for seed 42 and seed 43, verify reproducibility across negative X/Z, material transitions, water, invalid seeds, edits surviving attempted regeneration, region preflights and two-slot column capacity. Existing headless ECS/graphics tests still run.

## Not implemented yet

**No actual streaming or persistence is claimed.** `generate_terrain_region` is a synchronous, bounded call and does not unload/reload chunks according to player position. Evicting a chunk loses edits unless a future persistence system saves it first. Also not yet implemented: optimized meshing, real voxel GPU drawing, caves/ores, biome maps, physics, server networking or generation off the main thread.

Next: PR #4 — **visible-face chunk meshing** and real terrain rendering through OW-Graphics, with geometry regeneration driven by chunk revision values.
