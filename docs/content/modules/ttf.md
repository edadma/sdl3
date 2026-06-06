---
title: "sdl3_ttf — text"
weight: 2
---

```scala
import io.github.edadma.sdl3_ttf.*
```

TrueType text rendering, layered on the core binding. Pulls in `sdl3` transitively and
adds `@link("SDL3_ttf")`; install the native library with `brew install sdl3_ttf` (or your
platform's package). Colours are the core `sdl3.Color`.

## Lifecycle and fonts

```scala
ttfInit()                                       // initialise SDL_ttf
val font = openFont("/path/to/font.ttf", 24.0)  // path, point size
// …
font.close()
ttfQuit()
```

A `Font` is an `AnyVal` over the SDL_ttf handle:

```scala
font.isNull
font.setSize(18.0)              // re-set point size
font.style = STYLE_BOLD | STYLE_ITALIC
font.style                      // current style bitmask

font.height                    // line height, pixels
font.ascent
font.descent
font.lineSkip                  // recommended line spacing
```

Style flags: `STYLE_NORMAL`, `STYLE_BOLD`, `STYLE_ITALIC`, `STYLE_UNDERLINE`,
`STYLE_STRIKETHROUGH` (combine with `|`).

## Measuring

Get the pixel size a string would occupy without rendering it — the basis for laying text
out before you draw:

```scala
val (w, h): (Int, Int) = font.size("Hello, SDL3")
```

## Rendering

Each render call produces an `sdl3.Surface`. The quality/cost ladder:

```scala
font.renderSolid(text, fg)                    // fast, aliased, transparent bg
font.renderShaded(text, fg, bg)               // antialiased on a solid box
font.renderBlended(text, fg)                  // antialiased with alpha — the usual choice
font.renderBlendedWrapped(text, fg, wrapPixels) // word-wrapped; 0 wraps only on newlines
```

Upload the surface to a texture and free it, or let `texture` do both:

```scala
val tex = font.texture(renderer, "Hello, SDL3", Color(241, 243, 245))
renderer.copy(tex, 20, 20)
tex.destroy()
```

For text that doesn't change every frame, **cache the texture** rather than rebuilding it
each frame — `texture` allocates a surface, uploads it, and frees the surface on every
call.

## A note on colour passing

SDL_ttf's render functions take `SDL_Color` by value in C. Scala Native mis-marshals a
small by-value struct argument, so the binding passes the colour as a packed little-endian
`uint32` instead — ABI-identical to the 4-byte struct on the supported platforms. This is
internal; you just pass an `sdl3.Color`.
