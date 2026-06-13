---
title: "sdl3 — core"
weight: 1
---

```scala
import io.github.edadma.sdl3.*
```

The core artifact: lifecycle, a window, the 2D renderer, textures and surfaces, filled
geometry, events, and live input state. Everything here lives in the
`io.github.edadma.sdl3` package object.

## Lifecycle

```scala
setMainReady()                  // tell SDL you provide main (call before init)
val ok: Boolean = init(INIT_VIDEO)
val msg: String = error         // last SDL error message
delay(16)                       // sleep, milliseconds
setHint(HINT_RENDER_VSYNC, "1")
quit()
```

`init` takes a bitmask of subsystem flags: `INIT_TIMER`, `INIT_AUDIO`, `INIT_VIDEO`,
`INIT_EVENTS` (default `INIT_VIDEO`). It returns `true` on success; on failure read
`error`.

## Window

```scala
val window = createWindow("title", 640, 480)          // flags default to 0
val window = createWindow("title", 640, 480, WINDOW_RESIZABLE)
```

Window flags: `WINDOW_FULLSCREEN`, `WINDOW_OPENGL`, `WINDOW_HIDDEN`, `WINDOW_BORDERLESS`,
`WINDOW_RESIZABLE`, `WINDOW_HIGH_PIXEL_DENSITY`. A `Window` is an `AnyVal` over the SDL
handle:

```scala
window.isNull                       // creation failed?
window.createRenderer()             // or createRenderer("metal") to pick a driver
window.setPosition(x, y)            // SDL3 has no creation-time position
window.pixelFormat                  // Int
window.size            : (Int, Int) // logical size, in points
window.sizeInPixels    : (Int, Int) // backbuffer size, in pixels (≠ size on HiDPI)
window.destroy()
```

A window is **not** high-DPI unless created with `WINDOW_HIGH_PIXEL_DENSITY`; otherwise
`sizeInPixels == size`.

### Displays

Query the desktop so a window opens fully on-screen rather than spilling off a panel
smaller than the requested size:

```scala
val display = getPrimaryDisplay                 // primary display id (0 if none)
getDisplayForWindow(window)                     // the display a window is mostly on
displayUsableBounds(display)                    // Option[(x, y, w, h)]
```

The usable bounds are the desktop area minus space the system reserves — the menu bar, a
taskbar or dock — so clamping a `createWindow` size to the returned `w`×`h` (and placing
the window at `(x, y)`) keeps the whole window, including content at its bottom and right
edges, on-screen. `None` means SDL could not report the bounds.

## Renderer

The renderer is the 2D drawing context. Coordinates are `Double`.

```scala
val r = window.createRenderer()
r.setVSync(true)                    // sync present to the display refresh

r.setDrawColor(Color(247, 103, 7))  // or setDrawColor(r, g, b, a)
r.setBlendMode(BLENDMODE_BLEND)     // NONE / BLEND / ADD / MOD / MUL

r.clear()                           // clear to the current draw colour
r.clear(Color(24, 24, 28))          // set colour and clear

r.drawPoint(x, y)
r.drawLine(x1, y1, x2, y2)
r.drawRect(x, y, w, h)              // outline
r.fillRect(x, y, w, h)              // filled

r.present()                         // show the frame
r.destroy()
```

### Filled geometry

Shapes SDL has no primitive for are drawn as triangle meshes via `SDL_RenderGeometry`:

```scala
r.fillCircle(cx, cy, radius, Color.White)
r.thickLine(x1, y1, x2, y2, width = 4.0, Color(247, 103, 7))
r.fillConvexPolygon(Array(x0, y0, x1, y1, x2, y2, /* … */), Color.White)
```

`fillConvexPolygon` takes a flat `Array[Double]` of `x, y` pairs (a triangle fan from the
first vertex). The mesh builders are pure and unit-tested.

### Render targets

Draw into an off-screen texture, then blit it to the window — useful for supersampling or
flicker-free compositing:

```scala
val target = r.createTexture(window.pixelFormat, TEXTUREACCESS_TARGET, w, h)
r.setTarget(target)
// … draw …
r.resetTarget()
r.copy(target)                      // blit the whole texture across the window
r.present()
```

Texture access modes: `TEXTUREACCESS_STATIC`, `TEXTUREACCESS_STREAMING`,
`TEXTUREACCESS_TARGET`.

### Uploading a CPU pixel buffer

To put pixels produced on the CPU — by a 2D engine such as Cairo, or any code that fills a
raw buffer — onto the screen, create a `STREAMING` texture and replace its contents each
frame:

```scala
val tex = r.createTexture(PIXELFORMAT_ARGB8888, TEXTUREACCESS_STREAMING, w, h)

// each frame, after rendering into `buffer` (a Ptr[Byte]) whose rows are `pitch` bytes:
tex.update(buffer, pitch)
r.copy(tex)
r.present()
```

`PIXELFORMAT_ARGB8888` is laid out B, G, R, A on a little-endian host — byte-for-byte
identical to a Cairo `Format.ARGB32` image surface — so a Cairo buffer (its `getData` and
`getStride`) uploads with no conversion. `update` replaces the whole texture; `pitch` is the
source buffer's row length in bytes, which may exceed `width * 4` if the producer pads rows.

## Textures and surfaces

A **surface** is CPU pixels; a **texture** is GPU pixels. Upload a surface (from
[sdl3_ttf](/modules/ttf/) or [sdl3_image](/modules/image/)) to a texture, then draw it:

```scala
val tex = r.createTextureFromSurface(surface)
tex.setScaleMode(SCALEMODE_LINEAR)  // NEAREST / LINEAR / PIXELART
val (w, h) = tex.size

r.copy(tex)                         // fill the whole target
r.copy(tex, x, y)                   // at (x, y), the texture's own size
r.copy(tex, x, y, w, h)            // into a destination rect

tex.destroy()

surface.width; surface.height
surface.free()
```

## Events

`pollEvent()` returns `Option[Event]`; drain it each frame. An `Event` is a view over a
reusable union buffer, so read `kind` first, then only the fields valid for that kind:

```scala
var e = pollEvent()
while e.isDefined do
  val ev = e.get
  ev.kind match
    case QUIT              => running = false
    case KEY_DOWN          => onKey(ev.keyScancode, ev.keyRepeat)
    case MOUSE_BUTTON_DOWN => onClick(ev.mouseX, ev.mouseY, ev.mouseButton)
    case MOUSE_MOTION      => onMove(ev.mouseX, ev.mouseY)
    case MOUSE_WHEEL       => onScroll(ev.wheelX, ev.wheelY)
    case _                 => ()
  e = pollEvent()
```

Event kinds: `QUIT`, `KEY_DOWN`, `KEY_UP`, `TEXT_INPUT`, `MOUSE_MOTION`, `MOUSE_BUTTON_DOWN`,
`MOUSE_BUTTON_UP`, `MOUSE_WHEEL`, `WINDOW_RESIZED` (logical size changed), and
`WINDOW_PIXEL_SIZE_CHANGED` (backbuffer pixel size changed — the same moment on a 1× display,
and also when a window moves between displays of differing density; re-query `window.size` /
`window.sizeInPixels` and rebuild any sized backbuffer). Field accessors: `keyScancode`, `keyRepeat`, `keyMod` (the
active modifier bitmask — test with `KMOD_SHIFT` / `KMOD_CTRL` / `KMOD_ALT` / `KMOD_GUI`),
`mouseX`, `mouseY`, `mouseButton` (1 = left, 2 = middle, 3 = right), `wheelX`, `wheelY`
(positive y = away from the user), `text` (for `TEXT_INPUT`).

### Text input

Physical key events (`KEY_DOWN`) give you scancodes; to receive the actual *text* a user
types — respecting their keyboard layout, and the IME on platforms that have one — enable
text input on the window, then read `TEXT_INPUT` events:

```scala
window.startTextInput()          // begin delivering TEXT_INPUT events
// ... in the event loop:
if ev.kind == TEXT_INPUT then field += ev.text   // ev.text is the typed UTF-8 string
// ...
window.stopTextInput()           // when the field loses focus
```

A text field typically calls `startTextInput()` when focused and `stopTextInput()` on blur.
`ev.text` is the UTF-8 string for that event (often a single character, but a composed
sequence under an IME).

### Event watches

Register a callback fired for every event as it is pumped (handy for resize/expose
without restructuring the loop):

```scala
val id = addEventWatch { ev => if ev.kind == QUIT then save() }
removeEventWatch(id)
```

## Live input state

Instead of (or alongside) events, read the current device state directly:

```scala
val keys = Keyboard.state
if keys(Scancode.Space) then jump()
if keys(Scancode.Escape) then running = false

val m = Mouse.state          // MouseState(buttons, x, y)
if m.left then paint(m.x, m.y)
m.middle; m.right
```

`Scancode` names the physical keys: letters `A`–`Z`, digits `Num0`–`Num9`, `Return`,
`Escape`, `Backspace`, `Tab`, `Space`, `Minus`, `Equals`, `LeftBracket`, `RightBracket`,
and the arrows `Left`, `Right`, `Up`, `Down`. These are the standard USB-HID scancodes
SDL reports.

## Audio

PCM playback with no callback and no thread of your own. You open a stream, then **push**
finished buffers of float32 samples; SDL runs its own audio thread that pulls from the
stream's queue and feeds the device. This suits short, fully-known sounds — synthesised
effects, decoded one-shots — where you can hand over a complete buffer at the moment you
need it. (For *continuous* generation you would instead pass a callback to SDL; this layer
exposes the simpler push model.)

Audio is a separate subsystem from video, so bring it up after the window exists:

```scala
if initAudio() then              // SDL_InitSubSystem(SDL_INIT_AUDIO)
  val voice = openAudioStream(44100)        // float32, mono (channels defaults to 1)
  if !voice.isNull then
    voice.put(samples)           // samples: Array[Float], each in [-1, 1]
```

`openAudioStream(freq, channels = 1)` opens the default playback device for `AUDIO_F32`
samples and starts it; the returned `AudioStream` is an `AnyVal` over the SDL handle:

```scala
voice.isNull                     // open failed?
voice.put(samples: Array[Float]) // queue samples (SDL copies them; reuse the array freely)
voice.queued     : Int           // bytes still to play — 0 means idle
voice.resume(); voice.pause()    // device-side play/pause
voice.clear()                    // drop anything queued but not yet played
voice.destroy()
```

Each `put` appends to the stream's queue, so successive sounds on one stream play
back-to-back. To **overlap** effects, open several streams on the default device — SDL mixes
them — and send each new sound to the most idle one (`queued == 0`):

```scala
val voices = Array.fill(8)(openAudioStream(44100))
def play(buf: Array[Float]): Unit =
  voices.minBy(_.queued).put(buf)           // lands on a free voice, doesn't cut one off
```

A minimal synth — a 0.1 s sine "beep":

```scala
val rate = 44100
val n    = rate / 10
val beep = Array.tabulate(n) { i =>
  val t = i.toDouble / rate
  (math.sin(2 * math.Pi * 440 * t) * math.exp(-6 * t)).toFloat   // 440 Hz, decaying
}
play(beep)
```

Constants: `AUDIO_F32` (32-bit little-endian float samples) and
`AUDIO_DEVICE_DEFAULT_PLAYBACK` (the default output device id). Call `quitAudio()` to tear
the subsystem down, or just let `quit()` do it.
