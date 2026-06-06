---
title: "Design"
weight: 1
---

The binding is deliberately two-layered, and follows a few consistent conventions. You
rarely need to know this to *use* it, but it explains why the API looks the way it does
and is essential if you extend it.

## Two layers

**The `@extern` layer** is the raw FFI: one object per native library
(`extern.LibSDL3`, `extern.LibSDL3Ttf`, …) that declares the C functions with Scala
Native FFI types and `@link`s the shared library. This is the only place `Ptr`,
`CStruct`, `CString`, and `UInt` appear. You never import it.

**The pure-Scala layer** is the package object you do import (`io.github.edadma.sdl3`,
`io.github.edadma.sdl3_ttf`, …). It speaks in `Int`, `Double`, `Boolean`, `String`, a
`Color` case class, and wrapped handles, and it does the marshalling — allocating C
strings in a `Zone`, converting numbers, packing structs — so callers never touch FFI
types.

This split keeps the unsafe surface small and auditable, and lets the ergonomic layer
evolve without changing the ABI declarations.

## Handle wrappers are zero-cost

SDL hands back opaque pointers — `SDL_Window *`, `SDL_Renderer *`, `SDL_Texture *`,
`SDL_Surface *`. Each is wrapped in an `AnyVal` value class:

```scala
implicit class Renderer(val ptr: sdl.SDL_Renderer) extends AnyVal:
  def clear(c: Color): Unit = ...
  def present(): Unit       = ...
```

Because they extend `AnyVal`, the wrapper is erased at runtime — a `Renderer` *is* the
pointer, with methods. You get `renderer.present()` instead of `SDL_RenderPresent(ptr)`
at no allocation cost. Each wrapper carries an `isNull` check for the common "did this
fail?" test.

## Colours

`Color(r, g, b, a)` is a plain case class with channels `0–255` (`a` defaults to 255).
`Color.fromRGB(0xRRGGBB)` unpacks a packed literal, and `Color.blend(a, b, t)` does a
clamped linear interpolation. The renderer and text APIs take `Color` directly.

## Conventions worth knowing

These are the rules the binding holds to internally. They matter if you read the source
or add functions.

### Structs are passed by pointer

SDL functions that take a struct (`SDL_FRect`, `SDL_Vertex`, …) are called with a
**pointer** to a buffer the wrapper fills, never a by-value Scala `CStruct`. Scala
Native's marshalling of small by-value struct *arguments* is unreliable, so the binding
avoids it entirely. Where a C API insists on a by-value struct, the wrapper passes it in
an ABI-equivalent form instead — for example `SDL_Color` (four bytes) is passed to the
SDL_ttf render functions as a packed little-endian `uint32`, which lands in the same
register as the 4-byte struct on the SysV-AMD64 and AArch64 ABIs.

### Scratch buffers outlive the call, not the helper

A C call that needs a temporary struct (a rect for `SDL_RenderFillRect`, an event union
for `SDL_PollEvent`) reads it from a **persistent heap buffer** owned by the package
object, refilled per call. It is never `stackalloc`'d in a helper that returns the
pointer: stack memory belongs to the frame that allocates it, so such a pointer would
dangle the instant the helper returns and SDL would read garbage. Reuse is safe because
SDL reads the buffer synchronously within the call and rendering is single-threaded.

### Events are a view over a union

`pollEvent()` fills one reusable `SDL_Event` buffer and returns an `Event` view. The
field accessors (`kind`, `keyScancode`, `mouseX`, `wheelY`, …) are only meaningful for
the matching `kind`, because the underlying struct is a union. Read `kind` first, then
the fields for that event type.

### Geometry is floating-point

SDL3's render API takes `float` coordinates — the SDL2-era 16-bit clamping is gone. The
binding exposes `Double` throughout and narrows to `float` at the FFI boundary. Filled
shapes SDL has no primitive for (circles, thick lines) are built as triangle meshes and
drawn through `SDL_RenderGeometry`; the mesh builders are unit-tested headlessly.
