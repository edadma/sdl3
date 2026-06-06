---
title: "sdl3_image — images"
weight: 3
---

```scala
import io.github.edadma.sdl3_image.*
```

Image loading and PNG saving, layered on the core binding. Pulls in `sdl3` transitively
and adds `@link("SDL3_image")`; install the native library with `brew install sdl3_image`
(or your platform's package). SDL_image handles PNG, JPEG, and the other formats SDL3
supports.

## Loading

Load straight to a GPU texture (the common case — ready to draw), or to a CPU surface when
you need to inspect or transform pixels first:

```scala
// To a texture, ready for renderer.copy:
val tex = loadTexture(renderer, "assets/sprite.png")
val (w, h) = tex.size
renderer.copy(tex, 100, 100)
tex.destroy()

// To a surface (CPU pixels):
val surface = load("assets/sprite.png")
surface.width; surface.height
val gpu = renderer.createTextureFromSurface(surface)
surface.free()
```

Both return the core `Texture` / `Surface` handles, so everything in
[sdl3](/modules/core/) applies. Check `isNull` after loading to detect failure (a missing
or unreadable file).

## Saving

Write a surface out as a PNG:

```scala
val ok: Boolean = savePNG(surface, "out.png")
```
