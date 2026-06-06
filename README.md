# sdl3

Scala Native bindings for [SDL3](https://www.libsdl.org/) and its satellite
libraries, as a monorepo of one-library-per-module bindings. Each module is a
thin two-layer binding: a raw `@extern` FFI layer (the only place Scala Native
types appear) and a pure-Scala wrapper that speaks in `Int`, `Double`,
`Boolean`, `String`, `Color`, and zero-cost `AnyVal`-wrapped handles.

These are **Native-only** artifacts — there is no JVM or JS build.

## Modules

| Module   | Artifact                       | System library | Description |
|----------|--------------------------------|----------------|-------------|
| `core`   | `io.github.edadma::sdl3`       | `SDL3`         | Window, float render API (incl. `RenderGeometry` fills), textures, surfaces, events, keyboard/mouse, hints, render targets |
| `ttf`    | `io.github.edadma::sdl3_ttf`   | `SDL3_ttf`     | Font loading and text rendering (built on `core`) |
| `image`  | `io.github.edadma::sdl3_image` | `SDL3_image`   | Image decoding to surfaces/textures (built on `core`) |
| `mixer`  | `io.github.edadma::sdl3_mixer` | `SDL3_mixer`   | Audio playback — sound effects and music (built on `core`) |

The satellite modules depend on `core` directly within the build, so the suite
can be developed and tested without publishing intermediate versions.

## Why SDL3

Versus SDL2, the SDL3 render API takes floating-point coordinates (no more
16-bit clamping), most calls return `Boolean` success, and SDL_ttf 3 brings a
HarfBuzz text-shaping engine. Antialiased/filled primitives that used to need
SDL2_gfx are covered by the float `SDL_RenderLine`/`SDL_RenderGeometry` calls
plus supersampled render targets, so there is no `gfx` module.

## Prerequisites

Install the native libraries (macOS / Homebrew):

```sh
brew install sdl3 sdl3_ttf sdl3_image sdl3_mixer
```

## Usage

Add the module(s) you need (Scala Native):

```scala
libraryDependencies += "io.github.edadma" %%% "sdl3"       % "0.2.0"
libraryDependencies += "io.github.edadma" %%% "sdl3_ttf"   % "0.2.0"
libraryDependencies += "io.github.edadma" %%% "sdl3_image" % "0.2.0"
libraryDependencies += "io.github.edadma" %%% "sdl3_mixer" % "0.2.0"
```

```scala
import io.github.edadma.sdl3.*

setMainReady()
init(INIT_VIDEO)

val window   = createWindow("Hello SDL3", 800, 600)
val renderer = window.createRenderer()

renderer.clear(Color.Black)
renderer.setDrawColor(Color.fromRGB(0x4dabf7))
renderer.fillRect(100, 100, 200, 150)
renderer.fillCircle(400, 300, 60, Color.fromRGB(0xff6b6b))   // triangulated fill
renderer.thickLine(0, 0, 800, 600, 3, Color.White)
renderer.present()

delay(2000)

renderer.destroy()
window.destroy()
quit()
```

## Building

```sh
sbt core/test    # build, link against libSDL3, run tests
sbt ttf/test
sbt image/test
```

## License

ISC — see [LICENSE](LICENSE).
