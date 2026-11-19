# OWCraft

**An open-source voxel sandbox game written in OW, powered by OW-Engine, OW-Graphics and flOW.**

OWCraft is an early-stage **game project**, separate from [OW-Engine](https://github.com/OW-Programing-Language/OW-Engine). The engine remains a reusable general-purpose 2D/3D game framework; OWCraft implements voxel-specific gameplay on top of it.

## Status — first foundation PR

The initial version is **headless**. It does not render a voxel world yet.

- Real OW game bootstrap using **flOW**, OW-Engine's ECS, named scenes, deterministic fixed-step runtime and movement system.
- OWCraft-owned block catalog with stable block IDs: air, grass, dirt, stone, glass and water.
- Source dependencies managed by **SHA-pinned `dependencies.yml`**; no engine or graphics code copied into this repo.
- Optional **headless OW-Graphics** integration test through OW-Engine's RenderWorld and camera contracts.
- GitHub Actions builds and checks the code on Java 17.

## Repository boundaries

| Repository | Responsibility |
| --- | --- |
| [OW language](https://github.com/OW-Programing-Language/ow-lang) | OW compiler/interpreter and dependencies |
| [flOW](https://github.com/OW-Programing-Language/flOW---OW-Modular-Framework) | Module lifecycle, interfaces and dependency injection |
| [OW Graphics](https://github.com/OW-Programing-Language/OW-Graphics) | Renderer, graphics window, camera, textures, meshes |
| [OW Engine](https://github.com/OW-Programing-Language/OW-Engine) | ECS, scene lifecycle, physics foundations and game loop |
| **OWCraft** | Blocks, chunks, terrain, building and voxel gameplay |

**Important:** individual voxels are *not* OW-Engine ECS entities. Chunks will store thousands of compact block IDs and create only the necessary Graphics mesh resources. Player, NPCs and other dynamic objects can use ECS entities.

## Run (OW interpreter)

Prerequisites: JDK 17+, Maven, Git and a built OW compiler from [ow-lang](https://github.com/OW-Programing-Language/ow-lang).

```sh
ow deps install
ow check src/main.ow
ow run src/main.ow

ow run tests/headless_boot.ow
ow run tests/headless_blocks.ow
ow run tests/headless_graphics.ow
```

The headless tests need **no GPU, OpenGL or native bridge**. With a source-built compiler you can use `java -jar /path/to/ow-lang/target/OwIDE.jar` in place of `ow`.

## Project structure

```text
dependencies.yml                   # SHA-pinned OW ecosystem
src/
  main.ow                          # headless application entry point
  craft/
    CraftGame.ow                   # flOW + Engine game composition
    blocks/BlockCatalog.ow        # block materials and stable IDs
    modules/CraftBlocksModule.ow  # flOW service "craft.blocks"
tests/
  headless_boot.ow                 # ECS world, movement and lifecycle
  headless_blocks.ow               # block type validation
  headless_graphics.ow             # OW Engine / Graphics integration
.github/workflows/verify.yml       # CI
```

## Roadmap

1. **PR #1 — Foundation:** flOW, engine/graphics dependencies, initial world and player, block catalog, CI.
2. **PR #2 — Voxels:** compact chunk storage (16×16×16), chunk coordinates, placement/removal and boundary tests.
3. **PR #3 — Terrain:** deterministic seeded terrain and region streaming.
4. **PR #4 — Rendering:** visible-face meshing, chunk draw batching, terrain rendered via OW Graphics.
5. **PR #5 — Gameplay:** block interaction, ray casting, inventory and survival mechanics.
6. **Later:** robust saves, world streaming, performance, multiplayer, cross-platform packaging.

Physics, collision and general-purpose input capabilities should be implemented in OW-Engine, not duplicated here. The roadmap is aspirational: performance and compiler constraints will be measured as features land.

## License

No license is granted by this README alone. Add an explicit LICENSE file once a license is chosen.
