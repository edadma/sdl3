---
title: SDL3 for Scala Native
splash: true
heroTitle: SDL3 bindings for
heroHighlight: Scala Native
summary: Open a window, draw with the floating-point 2D renderer, read input, render text, load images, and play audio — SDL3 from idiomatic Scala. A thin pure-Scala layer wraps the raw FFI, so you work in Int, Double, String, and Color, never in Native pointer types.
---

## What this is

A set of [Scala Native](https://scala-native.org/) bindings for **SDL3** and its
satellite libraries, published under `io.github.edadma`. SDL3 gives you a window, a
hardware-accelerated 2D renderer, and input across macOS, Linux, and Windows; these
bindings make it usable from Scala without touching the C FFI.

```scala
import io.github.edadma.sdl3.*

setMainReady()
init(INIT_VIDEO)

val window   = createWindow("hello", 640, 480)
val renderer = window.createRenderer()
renderer.setVSync(true)

var running = true
while running do
  var e = pollEvent()
  while e.isDefined do
    if e.get.kind == QUIT then running = false
    e = pollEvent()

  renderer.clear(Color(24, 24, 28))
  renderer.fillCircle(320, 240, 64, Color(247, 103, 7))
  renderer.present()

renderer.destroy(); window.destroy(); quit()
```

## Why a pure-Scala layer

The raw `@extern` bindings speak in Native FFI types — `Ptr`, `CStruct`, `UInt`,
zone-allocated C strings. That surface is hidden. The package you import speaks in
ordinary Scala: `Int`, `Double`, `Boolean`, `String`, a `Color` case class, and
zero-cost `AnyVal` wrappers for the SDL handles (`Window`, `Renderer`, `Texture`,
`Surface`). You never write a `Zone` or convert a `CString` yourself.

## The modules

Four artifacts, one per SDL library. Each is added independently; the satellites pull
the core in transitively.

- **[sdl3](/modules/core/)** — window, renderer, 2D drawing (rects, lines, circles,
  textures), filled geometry, events, keyboard and mouse state.
- **[sdl3_ttf](/modules/ttf/)** — TrueType text rendering to surfaces and textures.
- **[sdl3_image](/modules/image/)** — load PNG/JPEG/etc. into surfaces or textures, save PNG.
- **[sdl3_mixer](/modules/mixer/)** — audio playback: load clips, play on tracks, gain and fades.

## Where to go next

- **[Getting Started](/getting-started/)** — install the libraries (incl. the native SDL3
  packages) and draw your first frame.
- **[Guide](/guide/)** — the design of the binding: the two layers, handle wrappers,
  pointer and memory conventions, and the ABI notes worth knowing.
- **[Modules](/modules/)** — the API of each artifact, with examples.
