# OWCraft — PR #2: Chunked voxel storage

## Layout and model

Each loaded chunk contains **4096 integer IDs** in a flat OW array (16×16×16). Local block index:

```text
index = y * 256 + z * 16 + x
0 <= x,y,z <= 15
```

Air has ID 0. Non-air block IDs range from 1 to 63, and must be defined in `BlockCatalog`. The current catalog includes grass, dirt, stone, glass, water. Blocks are **not ECS entities**. ECS is reserved for dynamic objects (player, NPCs, drops).

`WorldStore` holds a sparse registry of up to **32 resident chunks**, each addressed by signed `(cx,cy,cz)`. This is a deliberately bounded proof-of-concept; the first implementation uses dense 4096-ID arrays and a linear search over loaded chunks rather than a compressed palette or a hash map. Mesh optimization, unbounded streaming and region persistence are later phases.

## Signed coordinates

OW integer division truncates toward zero. `WorldStore.chunk_coordinate(value)` corrects negative nonmultiples of 16 to floor division, and `local_coordinate(value)` always yields 0–15 for valid world coordinates:

| World | Chunk | Local |
| ---: | ---: | ---: |
| -17 | -2 | 15 |
| -16 | -1 | 0 |
| -1 | -1 | 15 |
| 0 | 0 | 0 |
| 15 | 0 | 15 |
| 16 | 1 | 0 |
| 31 | 1 | 15 |

All world block coordinates must be signed integers in `[-1048576,1048575]` per axis to avoid arithmetic overflow. This will evolve with world streaming.

## Return contracts

- `get_block(x,y,z)` returns **-1** for unloaded/invalid coordinates, **0** for air in a loaded chunk, or a positive material ID.
- `place_block(x,y,z,id)` accepts a registered, non-air block ID and lazily creates missing chunks. Returns **1** on success (including no-op), **0** if invalid or full.
- `break_block(x,y,z)` returns **1** for actually removing a non-air block, **0** for air/invalid/unloaded coordinates. It never creates chunks.
- `create_chunk(cx,cy,cz)` returns **1** if a chunk was newly created; **0** on duplicate, invalid coordinates or capacity exhaustion.
- `remove_chunk(cx,cy,cz)` evicts a chunk without saving it and returns **1** when present, **0** otherwise.
- `chunk_at(cx,cy,cz)` returns the actual Chunk object or `0` when missing.
- `chunk_count`, `max_chunks`, `occupied_blocks` provide occupancy statistics. `Chunk.occupied_count()` includes **water** because it counts non-air blocks, not collidable blocks. For collision solidity, use `BlockCatalog.is_solid()`.
- `Chunk.version()` increments only for actual block changes, useful for later mesh invalidation.

## Game example

```ow
import "craft/CraftGame.ow"

main:
  set game: new CraftGame()
  call: assert(game.start() == 1)
  call: assert(game.place_block(0, 0, 0, game.block_catalog().id_of("grass")) == 1)
  call: assert(game.place_block(0 - 1, 0, 0, game.block_catalog().id_of("stone")) == 1)
  call: assert(game.world_store().chunk_count() == 2)
  call: assert(game.break_block(0, 0, 0) == 1)
  call: assert(game.stop() == 1)
```

`CraftWorldModule` registers service `craft.world` with contract `CRAFT_WORLD` and requires `craft.blocks` / `CRAFT_BLOCKS` through flOW. `CraftGame` exposes `world_store`, `get_block`, `place_block` and `break_block` after startup.

## Verification and limitations

Tests cover all 4096-slot boundaries, negative chunk lookup on X/Y/Z, adjacent chunk edits, unloaded-vs-air distinction, material validation, duplicate writes, non-air counts, revision tracking, capacity exhaustion, eviction/reuse, and compatibility with the original ECS fixed-step loop.

This phase is **headless** and does not generate terrain, build/render chunk meshes, handle ray casting, provide multiplayer or save chunk data to disk. OWCraft must remain game-specific; general physics and player collision belong in OW Engine.
