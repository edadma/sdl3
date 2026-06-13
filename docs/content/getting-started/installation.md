---
title: "Installation"
weight: 1
---

These are Scala Native bindings, so there are two halves: the **native SDL3 libraries**
(C shared objects on your system) and the **Scala artifacts** (added to your build).

## Requirements

- Scala 3
- sbt with the `sbt-scala-native` plugin
- LLVM/Clang (the Scala Native toolchain)
- The SDL3 shared libraries on your system

## Install the native SDL3 libraries

The Scala layer links against the system SDL3 libraries via `@link`, so they must be
installed and discoverable by the linker. On macOS with Homebrew:

```bash
brew install sdl3 sdl3_ttf sdl3_image sdl3_mixer
```

On Linux, install the SDL3 development packages from your distribution (or build from
source). You only need the satellite libraries for the modules you use — `sdl3` alone
needs just SDL3 itself.

## Enable Scala Native

In `project/plugins.sbt`:

```scala
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.12")
```

## Add the dependencies

Each module is published for Scala Native under `io.github.edadma`. Enable the Scala
Native plugin on your module and add the artifacts you need:

```scala
lazy val app = project
  .in(file("app"))
  .enablePlugins(ScalaNativePlugin)
  .settings(
    libraryDependencies += "io.github.edadma" %%% "sdl3"       % "0.2.7",
    libraryDependencies += "io.github.edadma" %%% "sdl3_ttf"   % "0.2.7",
    libraryDependencies += "io.github.edadma" %%% "sdl3_image" % "0.2.7",
    libraryDependencies += "io.github.edadma" %%% "sdl3_mixer" % "0.2.7",
  )
```

The satellites (`sdl3_ttf`, `sdl3_image`, `sdl3_mixer`) depend on `sdl3` transitively, so
you do not list the core separately when you use one of them. Each satellite also adds its
own `@link` to the matching native library (`SDL3_ttf`, etc.), pulling `SDL3` in with it.

No `linkingOptions` are needed: Scala Native finds the Homebrew-installed libraries
through each binding's `@link("SDL3")` / `@link("SDL3_ttf")` declaration.

## Provide a `main`

SDL apps call `SDL_SetMainReady` so SDL knows you own `main`. The binding exposes it as
`setMainReady()`; call it before `init`. Continue to the
[quick start](/getting-started/quick-start/) for a full window-and-loop example.
