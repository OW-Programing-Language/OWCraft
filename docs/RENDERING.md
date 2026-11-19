# OWCraft PR #4 — visible-face meshing and GPU terrain rendering

## Architecture and separation

```text
OWCraft WorldStore (16³ block IDs, seed=43)
    -> ChunkMesher (six-face neighbor checks, cross-chunk queries)
    -> ChunkMesh (XYZ, UV, normals, indexed triangles)
    -> OW Graphics GRAPHICS_MESH.create_lit_indexed()
    -> OW Engine RenderWorld (one ECS entity + opaque mesh handle per chunk)
    -> OW Graphics GLFW/OpenGL 3.3 renderer, camera and GLSL
```

This is the first **real 3D terrain window** in OWCraft. All chunk meshing and block color palette logic stays in OWCraft. OW-Engine owns entity/scene composition and render submission. OW-Graphics owns OpenGL/GLFW resources and matrices, shaders, camera and the renderer. flOW controls their module startup/shutdown.

**Important:** blocks are not ECS entities; each *chunk* is represented by one renderable entity referencing its Graphics mesh handle.

## CPU meshing

Each non-air block checks its neighbors at +/-X, +/-Y and +/-Z. The mesher emits a quad only when the neighbor does not occlude the face:

- Opaque stone/dirt/grass adjacent to opaque blocks: the internal face is omitted.
- Air or missing neighbor chunk: the face is emitted.
- Water/glass against their own block ID: internal faces are omitted.
- An opaque block next to transparent-marked water/glass: the opaque face is kept. The water/glass side against opaque is omitted.

A generated quad has four CCW vertices, six indices, flat normal and a UV pointing at the block ID's color in an 8×1 nearest-filtered texture palette. Meshing uses **two passes**: face counting followed by exact-size CPU buffer allocation. This avoids reliance on an OW array-append builtin and avoids uploading unused array capacity to OpenGL.

`ChunkMesher.count_faces(cx,cy,cz)` returns the visible face count or -1 if absent. `ChunkMesher.build(cx,cy,cz)` returns a `ChunkMesh` or 0 if absent / above its 12,000-face limit. An all-air chunk produces an empty mesh; skip GPU upload for it because OW Graphics requires nonempty indexed geometry.

The current Mesh API can accept up to 100k vertices and 300k indices; OWCraft's 12k-face limit caps geometry to 48k vertices / 72k indices per chunk. Mesh data is in absolute **world block coordinates**, and the ECS chunk entity is placed at origin to avoid double translation.

### Dirty chunk handling

The demo only renders one 16×16 column (two chunks with Y=0 and Y=1). It records each chunk's `version()`, and on modification of either chunk, **rebuilds both chunk GPU meshes**. This accounts for neighbor changes at the Y=16 boundary. Replacement uploads the new handle first, updates OW Engine's RenderWorld reference, then destroys the old Graphics mesh.

For future multi-column terrain rendering, implement a bounded chunk mesh cache and invalidate the changed chunk **and every loaded neighbor sharing an edited border**. The current demo is deliberately not a chunk streaming renderer.

## Run the GPU terrain viewer

JDK 17+, Maven, Git, an OpenGL 3.3 desktop and native LWJGL dependencies required. Keep OW-Engine and OW-Graphics as separate source dependencies; do not copy their code.

```sh
git clone https://github.com/OW-Programing-Language/ow-lang.git
git clone https://github.com/OW-Programing-Language/OW-Graphics.git
git clone https://github.com/OW-Programing-Language/OWCraft.git
mvn -f ow-lang/pom.xml -DskipTests install
mvn -f OW-Graphics/native-bridge/pom.xml package dependency:copy-dependencies -DoutputDirectory=target/lib
cd OWCraft
java -jar ../ow-lang/target/OwIDE.jar deps install
java -jar ../ow-lang/target/OwIDE.jar check examples/terrain_viewer/main.ow
java -cp "../ow-lang/target/OwIDE.jar:../OW-Graphics/native-bridge/target/classes:../OW-Graphics/native-bridge/target/lib/*" dev.oreo.Main run examples/terrain_viewer/main.ow
```

**Controls:** W/A/S/D move the camera across the generated scene; Escape closes the window. Movement is a fixed per-frame step, not yet delta-time based. On Windows, use semicolon classpath separators and the appropriate LWJGL native classifier.

### GPU CI

GitHub Actions builds the actual native provider at pinned SHA `4f00a5b4`, starts Xvfb with Mesa software GL, runs the real OW/Engine/Graphics composition, then samples **GL_BACK** using a test-only `craft.test.framebuffer` Java runtime provider.

The GPU test verifies terrain pixels differ from the cleared background, that green grass surfaces appear, that mesh edits trigger rebuilt GPU handles, and that the updated geometry remains visible. The test-only Java code is under `tests/gpu/`; applications never require it.

## Current scope and limitations

- Real 3D **one-column** proof of concept, not infinite terrain.
- Six-face culling is **not greedy meshing** and does not merge neighboring coplanar quads.
- Palette colors, not detailed texture atlas or PBR; water/glass are rendered **opaque**, without transparency sorting/blending.
- No user-facing block-picking ray cast, build controls or inventory yet.
- CPU meshing scans each block in a chunk twice. Streaming, priority queues, cross-chunk cache invalidation, worker generation and performance budgets come later.
- World chunks are still in-memory and evictions discard unsaved edits.
- GPU visual validation uses Mesa/Xvfb and a small sample grid, not multi-platform performance tests.
