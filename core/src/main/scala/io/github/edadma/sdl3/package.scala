package io.github.edadma

import scala.collection.mutable
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import scala.scalanative.libc.stdlib

/** Pure-Scala SDL3 layer. This is the only package consumers import — it speaks
  * in `Int`, `Double`, `Boolean`, `String`, `Color`, and `AnyVal`-wrapped
  * handles, never in Scala Native FFI types. Native pointers are wrapped in
  * `AnyVal` value classes (`Window`, `Renderer`, `Texture`, `Surface`), so the
  * abstraction is free.
  *
  * SDL is Native-only; this artifact has no JVM/JS build. Compared with the
  * SDL2 binding, the render API here takes floating-point coordinates (no more
  * 16-bit clamping) and most calls return `Boolean` success directly.
  */
package object sdl3:

  import extern.{LibSDL3 => sdl}

  // ---- subsystem init flags (SDL_INIT_*) ----
  val INIT_TIMER  = 0x00000001
  val INIT_AUDIO  = 0x00000010
  val INIT_VIDEO  = 0x00000020
  val INIT_EVENTS = 0x00004000

  // ---- window position / flags ----
  // SDL_WindowFlags is a 64-bit mask in SDL3, so these are `Long`. There is no
  // SDL_WINDOW_SHOWN: a window is visible unless created HIDDEN.
  val WINDOWPOS_CENTERED  = 0x2fff0000
  val WINDOW_FULLSCREEN   = 0x00000001L
  val WINDOW_OPENGL       = 0x00000002L
  val WINDOW_HIDDEN       = 0x00000008L
  val WINDOW_BORDERLESS   = 0x00000010L
  val WINDOW_RESIZABLE    = 0x00000020L
  val WINDOW_HIGH_PIXEL_DENSITY = 0x00002000L

  // ---- texture access / scale mode / blend mode ----
  val TEXTUREACCESS_STATIC    = 0
  val TEXTUREACCESS_STREAMING = 1
  val TEXTUREACCESS_TARGET    = 2
  val SCALEMODE_NEAREST       = 0
  val SCALEMODE_LINEAR        = 1
  val SCALEMODE_PIXELART      = 2
  val BLENDMODE_NONE          = 0x00000000
  val BLENDMODE_BLEND         = 0x00000001
  val BLENDMODE_ADD           = 0x00000002
  val BLENDMODE_MOD           = 0x00000004
  val BLENDMODE_MUL           = 0x00000008

  // ---- event types (SDL_EVENT_*) ----
  val QUIT              = 0x100
  val KEY_DOWN          = 0x300
  val KEY_UP            = 0x301
  val MOUSE_MOTION      = 0x400
  val MOUSE_BUTTON_DOWN = 0x401
  val MOUSE_BUTTON_UP   = 0x402
  val MOUSE_WHEEL       = 0x403

  // ---- mouse button masks (SDL_GetMouseState, SDL_BUTTON_*MASK) ----
  val BUTTON_LMASK = 1
  val BUTTON_MMASK = 2
  val BUTTON_RMASK = 4

  // ---- hint names ----
  val HINT_RENDER_VSYNC = "SDL_RENDER_VSYNC"

  /** An RGBA colour, 0–255 per channel. Not a pointer, so a plain case class. */
  final case class Color(r: Int, g: Int, b: Int, a: Int = 255)

  object Color:
    /** From a packed `0xRRGGBB` value, fully opaque. */
    def fromRGB(rgb: Int): Color = Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff)
    val Black = Color(0, 0, 0)
    val White = Color(255, 255, 255)

    /** Linear interpolation from `a` (t=0) to `b` (t=1), clamped — handy for
      * fades (e.g. trails blending toward the background).
      */
    def blend(a: Color, b: Color, t: Double): Color =
      val u = if t < 0.0 then 0.0 else if t > 1.0 then 1.0 else t
      Color(
        (a.r + (b.r - a.r) * u).toInt,
        (a.g + (b.g - a.g) * u).toInt,
        (a.b + (b.b - a.b) * u).toInt,
        (a.a + (b.a - a.a) * u).toInt,
      )

  // ---- top-level lifecycle ----

  /** Tell SDL we provide our own `main` (required when not using SDL's main shim). */
  def setMainReady(): Unit = sdl.SDL_SetMainReady()

  /** Initialise SDL subsystems; `true` on success. */
  def init(flags: Int = INIT_VIDEO): Boolean = sdl.SDL_Init(flags.toUInt)

  def quit(): Unit = sdl.SDL_Quit()

  /** The last SDL error message. */
  def error: String = fromCString(sdl.SDL_GetError())

  def delay(ms: Int): Unit = sdl.SDL_Delay(ms.toUInt)

  def setHint(name: String, value: String): Boolean =
    Zone(sdl.SDL_SetHint(toCString(name), toCString(value)))

  /** Create a window. SDL copies the title, so it is freed when the zone closes.
    * SDL3 has no creation-time position; use [[Window.setPosition]] afterwards.
    */
  def createWindow(
      title: String,
      width: Int,
      height: Int,
      flags: Long = 0L,
  ): Window =
    Zone(new Window(sdl.SDL_CreateWindow(toCString(title), width, height, flags.toULong)))

  // ---- handle wrappers (AnyVal — pointers, zero-cost) ----

  implicit class Window(val ptr: sdl.SDL_Window) extends AnyVal:
    def isNull: Boolean = ptr == null
    /** Create a renderer for this window. `name` selects a render driver, or
      * `null` lets SDL pick the best one.
      */
    def createRenderer(name: String = null): Renderer =
      if name == null then new Renderer(sdl.SDL_CreateRenderer(ptr, null))
      else Zone(new Renderer(sdl.SDL_CreateRenderer(ptr, toCString(name))))
    def setPosition(x: Int, y: Int): Unit = sdl.SDL_SetWindowPosition(ptr, x, y)
    def pixelFormat: Int                  = sdl.SDL_GetWindowPixelFormat(ptr).toInt
    /** Logical window size in screen coordinates. */
    def size: (Int, Int) =
      val w = stackalloc[CInt]()
      val h = stackalloc[CInt]()
      sdl.SDL_GetWindowSize(ptr, w, h)
      (!w, !h)
    /** Window size in physical pixels (differs from [[size]] on high-DPI displays). */
    def sizeInPixels: (Int, Int) =
      val w = stackalloc[CInt]()
      val h = stackalloc[CInt]()
      sdl.SDL_GetWindowSizeInPixels(ptr, w, h)
      (!w, !h)
    def destroy(): Unit = sdl.SDL_DestroyWindow(ptr)

  implicit class Renderer(val ptr: sdl.SDL_Renderer) extends AnyVal:
    def isNull: Boolean = ptr == null
    def setDrawColor(r: Int, g: Int, b: Int, a: Int = 255): Unit =
      sdl.SDL_SetRenderDrawColor(ptr, r.toUByte, g.toUByte, b.toUByte, a.toUByte)
    def setDrawColor(c: Color): Unit  = setDrawColor(c.r, c.g, c.b, c.a)
    def setBlendMode(mode: Int): Unit = sdl.SDL_SetRenderDrawBlendMode(ptr, mode.toUInt)
    def clear(): Unit                 = sdl.SDL_RenderClear(ptr)
    def clear(c: Color): Unit         = { setDrawColor(c); sdl.SDL_RenderClear(ptr) }
    def drawPoint(x: Double, y: Double): Unit = sdl.SDL_RenderPoint(ptr, x.toFloat, y.toFloat)
    def drawLine(x1: Double, y1: Double, x2: Double, y2: Double): Unit =
      sdl.SDL_RenderLine(ptr, x1.toFloat, y1.toFloat, x2.toFloat, y2.toFloat)
    def drawRect(x: Double, y: Double, w: Double, h: Double): Unit =
      sdl.SDL_RenderRect(ptr, frect(x, y, w, h))
    def fillRect(x: Double, y: Double, w: Double, h: Double): Unit =
      sdl.SDL_RenderFillRect(ptr, frect(x, y, w, h))
    def present(): Unit             = sdl.SDL_RenderPresent(ptr)
    /** Synchronise `present` to the display refresh (`true`) or run unthrottled
      * (`false`). SDL3 renderers default to vsync disabled. */
    def setVSync(enabled: Boolean): Unit = sdl.SDL_SetRenderVSync(ptr, if enabled then 1 else 0)
    def setTarget(t: Texture): Unit = sdl.SDL_SetRenderTarget(ptr, t.ptr)
    def resetTarget(): Unit         = sdl.SDL_SetRenderTarget(ptr, null)
    /** Blit a whole texture across the entire render target. */
    def copy(t: Texture): Unit = sdl.SDL_RenderTexture(ptr, t.ptr, null, null)
    /** Blit a texture into a destination rectangle, in target pixels. */
    def copy(t: Texture, x: Double, y: Double, w: Double, h: Double): Unit =
      sdl.SDL_RenderTexture(ptr, t.ptr, null, frect(x, y, w, h))
    /** Blit a texture at `(x, y)` using its own pixel size. */
    def copy(t: Texture, x: Double, y: Double): Unit =
      val (w, h) = t.size
      copy(t, x, y, w.toDouble, h.toDouble)
    def createTexture(format: Int, access: Int, w: Int, h: Int): Texture =
      new Texture(sdl.SDL_CreateTexture(ptr, format.toUInt, access, w, h))
    /** Upload a CPU surface (e.g. from SDL_ttf or SDL_image) to a GPU texture. */
    def createTextureFromSurface(s: Surface): Texture =
      new Texture(sdl.SDL_CreateTextureFromSurface(ptr, s.ptr))

    /** A filled, solid-colour circle, triangulated as a fan and drawn through
      * `SDL_RenderGeometry` — the SDL3 replacement for SDL2_gfx's filled circle.
      * Edge smoothness comes from the segment count (scaled to the radius) plus
      * any supersampling the caller renders into.
      */
    def fillCircle(cx: Double, cy: Double, radius: Double, color: Color): Unit =
      val segs   = circleSegments(radius)
      val nVerts = segs + 1
      val v      = stackalloc[Float](nVerts * 8)
      val idx    = stackalloc[CInt](segs * 3)
      buildCircle(v, idx, cx, cy, radius, color, segs)
      sdl.SDL_RenderGeometry(ptr, null, v, nVerts, idx, segs * 3)

    /** A line with thickness, drawn as a quad of two triangles through
      * `SDL_RenderGeometry`. A zero-length line draws nothing. */
    def thickLine(x1: Double, y1: Double, x2: Double, y2: Double, width: Double, color: Color): Unit =
      val v   = stackalloc[Float](4 * 8)
      val idx = stackalloc[CInt](6)
      if buildThickLine(v, idx, x1, y1, x2, y2, width, color) then
        sdl.SDL_RenderGeometry(ptr, null, v, 4, idx, 6)

    /** A filled convex polygon (3+ points as `x0, y0, x1, y1, …`), triangulated
      * as a fan from the first vertex. */
    def fillConvexPolygon(coords: Array[Double], color: Color): Unit =
      val n = coords.length / 2
      if n >= 3 then
        val v   = stackalloc[Float](n * 8)
        val idx = stackalloc[CInt]((n - 2) * 3)
        var i   = 0
        while i < n do
          putVertex(v, i, coords(i * 2), coords(i * 2 + 1), color)
          i += 1
        i = 0
        while i < n - 2 do
          idx(i * 3) = 0; idx(i * 3 + 1) = i + 1; idx(i * 3 + 2) = i + 2
          i += 1
        sdl.SDL_RenderGeometry(ptr, null, v, n, idx, (n - 2) * 3)

    def destroy(): Unit = sdl.SDL_DestroyRenderer(ptr)

  // A scratch SDL_FRect ({float x, y, w, h}) for the rect-taking render calls.
  // Reused per draw; safe because SDL reads it synchronously within the call.
  private def frect(x: Double, y: Double, w: Double, h: Double): Ptr[Float] =
    val r = stackalloc[Float](4)
    r(0) = x.toFloat; r(1) = y.toFloat; r(2) = w.toFloat; r(3) = h.toFloat
    r

  // ---- geometry buffer builders (shared by the RenderGeometry helpers) ----
  //
  // An SDL_Vertex is 8 contiguous floats: position (x, y), colour (r, g, b, a)
  // in 0–1, and texture coords (u, v). With a null texture the coords are
  // ignored, so they are written as 0. These builders are package-private so the
  // geometry logic can be unit-tested headlessly, without a live renderer.

  private[sdl3] def putVertex(v: Ptr[Float], i: Int, x: Double, y: Double, color: Color): Unit =
    val b = i * 8
    v(b) = x.toFloat; v(b + 1) = y.toFloat
    v(b + 2) = color.r / 255f; v(b + 3) = color.g / 255f; v(b + 4) = color.b / 255f; v(b + 5) = color.a / 255f
    v(b + 6) = 0f; v(b + 7) = 0f

  /** Segment count for a filled circle — more segments as the radius grows, so
    * small dots stay cheap and large discs stay smooth. */
  private[sdl3] def circleSegments(radius: Double): Int =
    math.max(12, math.min(64, radius.toInt + 12))

  /** Fill `v` (`(segs+1)*8` floats) and `idx` (`segs*3` ints) with a triangle-fan
    * circle: vertex 0 at the centre, `segs` vertices around the rim. */
  private[sdl3] def buildCircle(
      v: Ptr[Float],
      idx: Ptr[CInt],
      cx: Double,
      cy: Double,
      radius: Double,
      color: Color,
      segs: Int,
  ): Unit =
    putVertex(v, 0, cx, cy, color)
    var i = 0
    while i < segs do
      val ang = (i.toDouble / segs) * 2.0 * math.Pi
      putVertex(v, i + 1, cx + math.cos(ang) * radius, cy + math.sin(ang) * radius, color)
      i += 1
    i = 0
    while i < segs do
      idx(i * 3) = 0
      idx(i * 3 + 1) = i + 1
      idx(i * 3 + 2) = if i + 1 < segs then i + 2 else 1
      i += 1

  /** Fill `v` (4 vertices) and `idx` (6 ints) with the quad for a thick line.
    * Returns `false` (drawing nothing) for a zero-length line. */
  private[sdl3] def buildThickLine(
      v: Ptr[Float],
      idx: Ptr[CInt],
      x1: Double,
      y1: Double,
      x2: Double,
      y2: Double,
      width: Double,
      color: Color,
  ): Boolean =
    val dx  = x2 - x1
    val dy  = y2 - y1
    val len = math.hypot(dx, dy)
    if len == 0.0 then false
    else
      val nx = -dy / len * (width / 2.0)
      val ny = dx / len * (width / 2.0)
      putVertex(v, 0, x1 + nx, y1 + ny, color)
      putVertex(v, 1, x2 + nx, y2 + ny, color)
      putVertex(v, 2, x2 - nx, y2 - ny, color)
      putVertex(v, 3, x1 - nx, y1 - ny, color)
      idx(0) = 0; idx(1) = 1; idx(2) = 2
      idx(3) = 0; idx(4) = 2; idx(5) = 3
      true

  implicit class Texture(val ptr: sdl.SDL_Texture) extends AnyVal:
    def isNull: Boolean               = ptr == null
    def setScaleMode(mode: Int): Unit = sdl.SDL_SetTextureScaleMode(ptr, mode)
    /** The texture's `(width, height)` in pixels. */
    def size: (Int, Int) =
      val w = stackalloc[Float]()
      val h = stackalloc[Float]()
      sdl.SDL_GetTextureSize(ptr, w, h)
      ((!w).toInt, (!h).toInt)
    def destroy(): Unit = sdl.SDL_DestroyTexture(ptr)

  /** A CPU-side pixel buffer — produced by SDL_ttf text rendering or SDL_image
    * decoding, then uploaded to a [[Texture]] via
    * [[Renderer.createTextureFromSurface]] and freed. `width`/`height` read the
    * SDL3 `SDL_Surface` struct (`w` at offset 8, `h` at offset 12).
    */
  implicit class Surface(val ptr: sdl.SDL_Surface) extends AnyVal:
    def isNull: Boolean = ptr == null
    def width: Int      = !((ptr + 8).asInstanceOf[Ptr[CInt]])
    def height: Int     = !((ptr + 12).asInstanceOf[Ptr[CInt]])
    def free(): Unit    = sdl.SDL_DestroySurface(ptr)

  // ---- events ----

  // One reusable heap buffer for the SDL_Event union (kept for the process
  // lifetime). Each poll overwrites it, which is fine for the usual
  // poll-then-handle loop. The SDL3 union is up to 128 bytes.
  private val eventBuf: Ptr[Byte] = stdlib.malloc(128.toUSize)

  /** Pull the next pending event, or `None` if the queue is empty. */
  def pollEvent(): Option[Event] =
    if sdl.SDL_PollEvent(eventBuf) then Some(new Event(eventBuf)) else None

  /** A view over the current SDL_Event buffer. Field accessors are only
    * meaningful for the matching `kind` (the underlying struct is a union).
    * Offsets are the stable SDL3 64-bit ABI layout — every event begins with
    * `type`(4) + `reserved`(4) + `timestamp`(8) + `windowID`(4).
    */
  implicit class Event(val ptr: Ptr[Byte]) extends AnyVal:
    private def i32(off: Int): Int     = !((ptr + off).asInstanceOf[Ptr[Int]])
    private def f32(off: Int): Float   = !((ptr + off).asInstanceOf[Ptr[Float]])
    private def u8(off: Int): Int      = (!(ptr + off)).toInt & 0xff
    private def bool(off: Int): Boolean = !(ptr + off) != 0

    def kind: Int = (!ptr.asInstanceOf[Ptr[UInt]]).toInt

    /** Keyboard events: the physical key (an SDL scancode) and key-repeat flag. */
    def keyScancode: Int   = i32(24)
    def keyRepeat: Boolean = bool(37)
    /** Mouse motion events: cursor position. */
    def mouseX: Double = f32(28).toDouble
    def mouseY: Double = f32(32).toDouble
    /** Mouse button events: which button (1=left, 2=middle, 3=right). */
    def mouseButton: Int = u8(24)
    /** Mouse wheel events: scroll amounts (positive y = away from the user). */
    def wheelX: Double = f32(24).toDouble
    def wheelY: Double = f32(28).toDouble

  // ---- event watches: the libuv-style callback map pattern ----
  //
  // SDL invokes a C function pointer for each event during pumping. We register
  // ONE static trampoline with SDL and keep the Scala watchers in a map, so
  // consumers pass ordinary closures. Watches fire on the thread that pumps
  // events (the main thread here), so this stays clear of Scala Native's GC.

  type EventWatch = Event => Unit

  private val eventWatches    = mutable.HashMap[Int, EventWatch]()
  private var nextWatchId     = 0
  private var watchRegistered = false

  private val eventWatchTrampoline: sdl.SDL_EventFilter =
    (_: Ptr[Byte], event: Ptr[Byte]) =>
      val e = new Event(event)
      eventWatches.valuesIterator.foreach(_(e))
      true

  /** Register a watcher invoked for every event as it is pumped. Returns an id
    * for [[removeEventWatch]].
    */
  def addEventWatch(watch: EventWatch): Int =
    if !watchRegistered then
      sdl.SDL_AddEventWatch(eventWatchTrampoline, null)
      watchRegistered = true
    val id = nextWatchId
    nextWatchId += 1
    eventWatches(id) = watch
    id

  def removeEventWatch(id: Int): Unit =
    eventWatches -= id
    if eventWatches.isEmpty && watchRegistered then
      sdl.SDL_RemoveEventWatch(eventWatchTrampoline, null)
      watchRegistered = false

  // ---- keyboard / mouse polling ----

  object Keyboard:
    /** A snapshot of held keys, indexed by [[Scancode]]. */
    def state: KeyboardState = new KeyboardState(sdl.SDL_GetKeyboardState(null))

  /** SDL3's keyboard state is a `const bool*` — one byte per scancode. */
  implicit class KeyboardState(val ptr: Ptr[CBool]) extends AnyVal:
    def apply(scancode: Int): Boolean = !(ptr + scancode)

  final case class MouseState(buttons: Int, x: Double, y: Double):
    def left: Boolean   = (buttons & BUTTON_LMASK) != 0
    def middle: Boolean = (buttons & BUTTON_MMASK) != 0
    def right: Boolean  = (buttons & BUTTON_RMASK) != 0

  object Mouse:
    def state: MouseState =
      val x    = stackalloc[Float]()
      val y    = stackalloc[Float]()
      val mask = sdl.SDL_GetMouseState(x, y)
      MouseState(mask.toInt, (!x).toDouble, (!y).toDouble)

  /** A useful subset of SDL physical scancodes (USB HID usage IDs), which is
    * what [[KeyboardState.apply]] is indexed by.
    */
  object Scancode:
    val A = 4; val B = 5; val C = 6; val D = 7; val E = 8; val F = 9; val G = 10
    val H = 11; val I = 12; val J = 13; val K = 14; val L = 15; val M = 16; val N = 17
    val O = 18; val P = 19; val Q = 20; val R = 21; val S = 22; val T = 23; val U = 24
    val V = 25; val W = 26; val X = 27; val Y = 28; val Z = 29
    val Num1 = 30; val Num2 = 31; val Num3 = 32; val Num4 = 33; val Num5 = 34
    val Num6 = 35; val Num7 = 36; val Num8 = 37; val Num9 = 38; val Num0 = 39
    val Return = 40; val Escape = 41; val Backspace = 42; val Tab = 43; val Space = 44
    val Minus = 45; val Equals = 46; val LeftBracket = 47; val RightBracket = 48
    val Right = 79; val Left = 80; val Down = 81; val Up = 82
