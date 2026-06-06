---
title: "Quick Start"
weight: 2
---

A complete program: open a window, run an event loop, and draw an animated frame.

## A window and a frame loop

```scala
import io.github.edadma.sdl3.*

@main def main(): Unit =
  setMainReady()
  if !init(INIT_VIDEO) then
    System.err.println(s"SDL_Init failed: $error")
    return

  val window = createWindow("sdl3 quick start", 640, 480)
  if window.isNull then
    System.err.println(s"createWindow failed: $error")
    return

  val renderer = window.createRenderer()
  renderer.setVSync(true) // throttle the loop to the display refresh

  var running = true
  while running do
    // Drain the event queue each frame.
    var e = pollEvent()
    while e.isDefined do
      val ev = e.get
      if ev.kind == QUIT then running = false
      e = pollEvent()

    // Clear, draw, present.
    renderer.clear(Color(24, 24, 28))
    renderer.setDrawColor(Color(247, 103, 7))
    renderer.fillRect(270, 190, 100, 100)
    renderer.fillCircle(320, 240, 40, Color(255, 255, 255))
    renderer.present()

  renderer.destroy()
  window.destroy()
  quit()
```

Build and run it with `sbt run` (it blocks on the window). `sbt nativeLink` just produces
the binary.

## Reading input

Poll discrete events from the queue, or read the live device state at any time:

```scala
// Event-driven: react to a key press as it happens.
val ev = pollEvent().get
if ev.kind == KEY_DOWN && ev.keyScancode == Scancode.Escape then running = false

// State-driven: query held keys / the mouse each frame.
val keys = Keyboard.state
if keys(Scancode.Space) then jump()

val mouse = Mouse.state
if mouse.left then paint(mouse.x, mouse.y)
```

## Adding text

Bring in [`sdl3_ttf`](/modules/ttf/) to draw a label. Initialise it alongside SDL, open a
font once, and blit rendered text as a texture:

```scala
import io.github.edadma.sdl3.*
import io.github.edadma.sdl3_ttf.*

ttfInit()
val font = openFont("/System/Library/Fonts/Helvetica.ttc", 24)

// each frame, after clear:
val tex = font.texture(renderer, "Hello, SDL3", Color(241, 243, 245))
renderer.copy(tex, 20, 20)
tex.destroy()

// at shutdown:
font.close()
ttfQuit()
```

For real apps, cache the text texture instead of rebuilding it every frame. See the
[modules](/modules/) section for each library's full surface.
