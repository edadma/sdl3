---
title: "sdl3_mixer — audio"
weight: 4
---

```scala
import io.github.edadma.sdl3_mixer.*
```

Audio playback, layered on the core binding. Pulls in `sdl3` transitively and adds
`@link("SDL3_mixer")`; install the native library with `brew install sdl3_mixer` (or your
platform's package).

SDL3_mixer's model is a **mixer device** that plays **audio** clips, optionally through
named **tracks** you can control individually (gain, pause, fades, looping).

## Setup

```scala
mixInit()
val mixer = openMixer()        // default playback device
// …
mixer.destroy()
mixQuit()
```

## Fire-and-forget playback

The simplest path — load a clip and play it once on the device:

```scala
val clip = mixer.loadAudio("assets/blip.wav")   // predecode = true by default
mixer.play(clip)
// …
clip.destroy()
```

`loadAudio(path, predecode = false)` streams from disk instead of decoding up front —
prefer it for long music tracks.

## Tracks: control a sound over time

For looping, fades, pausing, or per-sound gain, play through a `Track`:

```scala
val track = mixer.createTrack()
track.setAudio(clip)

track.play()                   // play once
track.play(loops = 2)          // play, then repeat twice more
track.play(loops = LOOP_FOREVER)

track.setGain(0.5)             // 0.0 … 1.0
track.pause(); track.resume()
track.playing                  // Boolean

track.stop()                   // stop now
track.stop(fadeOutFrames = 4096) // fade out over N sample frames

track.destroy()
```

`LOOP_FOREVER` is `-1`. Most calls return `Boolean` success. Destroy tracks and audio
clips when done, then the mixer, then `mixQuit()`.
