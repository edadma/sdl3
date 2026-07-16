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

  // ---- pixel formats ----
  /** 32-bit packed ARGB, 8 bits per channel. On a little-endian host the bytes are laid
    * out B, G, R, A — byte-for-byte identical to a Cairo `Format.ARGB32` image surface — so
    * a Cairo buffer uploads straight into a streaming texture of this format via
    * [[Texture.update]]. The value is SDL's `SDL_DEFINE_PIXELFORMAT` encoding for
    * PACKED32 / ARGB / 8888 / 32-bit. */
  val PIXELFORMAT_ARGB8888 = 0x16362004

  // ---- YUV pixel formats ----
  //
  // The formats a video decoder actually produces. A texture in one of these takes the decoder's
  // planes directly ([[Texture.updateYUV]] / [[Texture.updateNV]]) and the renderer converts to
  // RGB in the blit's shader — so there is no CPU colour conversion on the frame path, and the
  // scale to the destination rectangle is free. Each value is SDL's `SDL_DEFINE_PIXELFOURCC`
  // encoding, i.e. the four ASCII bytes packed little-endian.
  //
  // Chroma is half resolution on both axes in every planar format here (4:2:0), so the U and V
  // planes are (width/2) x (height/2).

  /** Planar Y + U + V, three planes. libavcodec's `AV_PIX_FMT_YUV420P` — the common output of an
    * H.264/HEVC decode, and what [[Texture.updateYUV]] expects. */
  val PIXELFORMAT_IYUV = 0x56555949

  /** Planar Y + V + U, three planes — IYUV with the chroma planes swapped. */
  val PIXELFORMAT_YV12 = 0x32315659

  /** Planar Y + interleaved U/V, two planes. What hardware decoders (VideoToolbox, VAAPI) hand
    * back; feed it with [[Texture.updateNV]]. */
  val PIXELFORMAT_NV12 = 0x3231564e

  /** Planar Y + interleaved V/U, two planes — NV12 with the chroma order swapped. */
  val PIXELFORMAT_NV21 = 0x3132564e

  // ---- colorspaces ----
  //
  // Only meaningful for a YUV texture, and settable only at creation, through
  // [[Renderer.createYUVTexture]]. SDL defaults a YUV texture to BT.601 limited, so HD footage
  // created without an explicit colorspace decodes to visibly wrong colour — greens and reds
  // shifted. Match what the file declares: BT.709 for HD, BT.601 for SD, JPEG for full-range.

  /** BT.709, limited (studio) range — the colorspace of essentially all HD video. */
  val COLORSPACE_BT709_LIMITED = 0x21100421

  /** BT.601, limited (studio) range — SD video. SDL's default for YUV when unspecified. */
  val COLORSPACE_BT601_LIMITED = 0x211018c6

  /** Full-range YUV, as produced by JPEG and by some camera and screen-capture sources. */
  val COLORSPACE_JPEG = 0x220004c6

  val BLENDMODE_NONE          = 0x00000000
  val BLENDMODE_BLEND         = 0x00000001
  val BLENDMODE_ADD           = 0x00000002
  val BLENDMODE_MOD           = 0x00000004
  val BLENDMODE_MUL           = 0x00000008

  // ---- event types (SDL_EVENT_*) ----
  val QUIT              = 0x100
  // Window events. RESIZED fires when the logical size changes (a user drag, a programmatic
  // resize); PIXEL_SIZE_CHANGED fires when the backbuffer's pixel size changes — the same
  // moment on a 1× display, and also when the window moves between displays of different
  // density. A renderer that owns a sized backbuffer rebuilds it on the latter.
  val WINDOW_RESIZED            = 0x202
  val WINDOW_PIXEL_SIZE_CHANGED = 0x208
  val KEY_DOWN          = 0x300
  val KEY_UP            = 0x301
  val TEXT_EDITING      = 0x304
  val TEXT_INPUT        = 0x303
  val MOUSE_MOTION      = 0x400
  val MOUSE_BUTTON_DOWN = 0x401
  val MOUSE_BUTTON_UP   = 0x402
  val MOUSE_WHEEL       = 0x403

  // ---- mouse button masks (SDL_GetMouseState, SDL_BUTTON_*MASK) ----
  val BUTTON_LMASK = 1
  val BUTTON_MMASK = 2
  val BUTTON_RMASK = 4

  // ---- key modifiers (SDL_Keymod) ----
  // The modifier bitmask carried by a keyboard event (see [[Event.keyMod]]); the `*_SHIFT`
  // / `*_CTRL` / `*_ALT` / `*_GUI` aliases match either side. These are the SDL3
  // `SDL_KMOD_*` values.
  val KMOD_NONE   = 0x0000
  val KMOD_LSHIFT = 0x0001
  val KMOD_RSHIFT = 0x0002
  val KMOD_LCTRL  = 0x0040
  val KMOD_RCTRL  = 0x0080
  val KMOD_LALT   = 0x0100
  val KMOD_RALT   = 0x0200
  val KMOD_LGUI   = 0x0400
  val KMOD_RGUI   = 0x0800
  val KMOD_SHIFT  = KMOD_LSHIFT | KMOD_RSHIFT
  val KMOD_CTRL   = KMOD_LCTRL | KMOD_RCTRL
  val KMOD_ALT    = KMOD_LALT | KMOD_RALT
  val KMOD_GUI    = KMOD_LGUI | KMOD_RGUI

  // ---- hint names ----
  val HINT_RENDER_VSYNC = "SDL_RENDER_VSYNC"

  /** Selects a file-dialog backend by name. Only Linux has more than one to choose between
    * ("portal", "zenity"); every other platform has exactly one and **fails the dialog** if this
    * is set to anything at all. That makes it a way to exercise a dialog's whole path — filters,
    * properties, callback — without a panel appearing, which is what the tests use it for. */
  val HINT_FILE_DIALOG_DRIVER = "SDL_FILE_DIALOG_DRIVER"

  // ---- audio ----
  /** `SDL_AUDIO_F32LE` — 32-bit little-endian float samples in [-1, 1], the natural format for
    * synthesised PCM. */
  val AUDIO_F32 = 0x8120
  /** `SDL_AUDIO_DEVICE_DEFAULT_PLAYBACK` — the special device id selecting the system's default
    * output. It is `(SDL_AudioDeviceID)0xFFFFFFFF`; pass it to [[openAudioStream]]. */
  val AUDIO_DEVICE_DEFAULT_PLAYBACK = 0xffffffff

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

  /** Return a hint to its default, as though [[setHint]] had never been called for it. Not the
    * same as setting it to `""`, which is a value like any other. */
  def resetHint(name: String): Boolean = Zone(sdl.SDL_ResetHint(toCString(name)))

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

  // ---- displays ----

  /** The id of the primary display, or `0` if none is reported. */
  def getPrimaryDisplay: Int = sdl.SDL_GetPrimaryDisplay().toInt

  /** The id of the display a window is mostly on, or `0` if SDL can't tell. */
  def getDisplayForWindow(window: Window): Int = sdl.SDL_GetDisplayForWindow(window.ptr).toInt

  /** The usable bounds `(x, y, width, height)` of a display in screen coordinates — the
    * desktop area minus space the system reserves (a menu bar, a taskbar/dock) — or `None`
    * if SDL can't report them. A window sized within `width`×`height` fits fully on-screen. */
  def displayUsableBounds(displayID: Int): Option[(Int, Int, Int, Int)] =
    val r = stackalloc[CInt](4)
    if sdl.SDL_GetDisplayUsableBounds(displayID.toUInt, r) then Some((r(0), r(1), r(2), r(3)))
    else None

  // ---- clipboard ----

  /** The clipboard's text, or `""` when it holds none. SDL hands back a freshly allocated string;
    * it is copied into a Scala `String` and the native buffer freed before returning. */
  def getClipboardText: String =
    val p = sdl.SDL_GetClipboardText()
    try fromCString(p)
    finally sdl.SDL_free(p.asInstanceOf[Ptr[Byte]])

  /** Replace the clipboard's text; `true` on success. */
  def setClipboardText(text: String): Boolean = Zone(sdl.SDL_SetClipboardText(toCString(text)))

  /** Whether the clipboard currently holds non-empty text. */
  def hasClipboardText: Boolean = sdl.SDL_HasClipboardText()

  // ---- threads ----

  /** Whether the caller is running on SDL's main thread — the one SDL was initialised on, and
    * the only one that may create a window, touch a renderer, or pump events. Useful as an
    * assertion at the head of anything a worker thread must hand off rather than do itself. */
  def isMainThread: Boolean = sdl.SDL_IsMainThread()

  // ---- file dialogs ----
  //
  // The system's own file chooser — a real Finder/Explorer panel, not something drawn by the app.
  //
  // Threading: SDL documents that the callback "may be called from a different thread", but on
  // macOS it never is. The Cocoa backend either runs the panel as a sheet on the parent window,
  // whose completion handler the main run loop invokes during event pumping, or — with no parent
  // window — calls `runModal`, which blocks and invokes the callback before the call even
  // returns. Both are the main thread, so a Scala closure is safe here, exactly as it is for an
  // event watch. The same holds for Linux's XDG-portal backend (DBus messages, delivered while
  // pumping). Linux's zenity fallback is the exception: it runs the callback on an SDL-created
  // worker thread, which Scala Native's GC knows nothing about. Revisit before relying on this
  // on a portal-less Linux.
  //
  // Note the consequence of that `runModal` path: without a `window` the call BLOCKS until the
  // user chooses, so an app that draws its own frames stops drawing them. Pass the window.
  //
  // One SDL wart to know about: the Cocoa sheet path answers only NSModalResponseOK and
  // NSModalResponseCancel, so a sheet ended any other way (dismissed programmatically) never
  // calls back at all. This binding's per-dialog callback and filter array are freed by the
  // callback, so such a dialog strands both. It takes a deliberate act to provoke and leaks a
  // closure and a few bytes, so it is left as SDL has it rather than guessed around.

  /** The kind of panel [[showFileDialog]] presents. */
  val FILEDIALOG_OPENFILE   = 0
  val FILEDIALOG_SAVEFILE   = 1
  val FILEDIALOG_OPENFOLDER = 2

  /** One entry in a file dialog's filter list. `name` is what the user reads ("Video files");
    * `pattern` is a semicolon-separated extension list ("mp4;mov;mkv"), with no dots and no
    * globs — or the single string `"*"`, which means "all files".
    *
    * Filters are advisory: not every platform honours them, and those that do may still let the
    * user defeat them. Never treat a returned path as matching. */
  final case class FileFilter(name: String, pattern: String)

  /** What the user did with a file dialog. Cancelling is a normal outcome and a distinct one
    * from failure — SDL reports them differently and so does this. */
  enum DialogResult:
    /** One or more chosen paths; never empty. A save dialog's path may not exist yet, and on
      * every platform the file may have been deleted or replaced since. */
    case Chosen(paths: Seq[String])

    /** The user dismissed the dialog without choosing. */
    case Cancelled

    /** The dialog could not be shown, or failed while up; `message` is SDL's error text. */
    case Failed(message: String)

  // A dialog's Scala closure and its C filter array both have to outlive the call that starts
  // it, so they are held here under an id that SDL carries as opaque `userdata` and hands back.
  // Same trampoline pattern as the event watches, for the same reason: SDL takes a C function
  // pointer, so the closure must be found again rather than passed. The trampoline is what
  // empties both maps; that they do end up empty is the "nothing leaked" invariant the tests
  // assert, which is why these are package-private rather than private.
  private[sdl3] val dialogCallbacks  = mutable.HashMap[Int, DialogResult => Unit]()
  private[sdl3] val dialogFilterBufs = mutable.HashMap[Int, (Ptr[sdl.SDL_DialogFileFilter], Int)]()
  private var nextDialogId           = 0

  /** A C copy of `s` that outlives the call, for the filter strings SDL holds by pointer until
    * the callback. `Zone`/`toCString` would free them at the end of the enclosing block, which
    * for a sheet is long before the user has chosen anything. */
  private def cstrdup(s: String): CString =
    val bytes = s.getBytes("UTF-8")
    val p     = stdlib.malloc((bytes.length + 1).toUSize)
    var i     = 0
    while i < bytes.length do
      p(i) = bytes(i)
      i += 1
    p(bytes.length) = 0.toByte
    p

  private def freeFilters(buf: (Ptr[sdl.SDL_DialogFileFilter], Int)): Unit =
    val (arr, n) = buf
    var i        = 0
    while i < n do
      stdlib.free((arr + i)._1)
      stdlib.free((arr + i)._2)
      i += 1
    stdlib.free(arr.asInstanceOf[Ptr[Byte]])

  private val dialogTrampoline: sdl.SDL_DialogFileCallback =
    (userdata: Ptr[Byte], filelist: Ptr[CString], _: CInt) =>
      val id = !userdata.asInstanceOf[Ptr[CInt]]
      stdlib.free(userdata)
      val callback = dialogCallbacks.remove(id)
      dialogFilterBufs.remove(id).foreach(freeFilters)
      callback.foreach { f =>
        f(
          if filelist == null then DialogResult.Failed(error)
          else
            val paths = Seq.newBuilder[String]
            var i     = 0
            while filelist(i) != null do
              paths += fromCString(filelist(i))
              i += 1
            val chosen = paths.result()
            // A pointer to null is SDL's "the user cancelled" — distinct from the null list
            // that means the dialog itself failed.
            if chosen.isEmpty then DialogResult.Cancelled else DialogResult.Chosen(chosen),
        )
      }

  /** Show a system file dialog and deliver the outcome to `callback`.
    *
    * `dialogType` is one of [[FILEDIALOG_OPENFILE]], [[FILEDIALOG_SAVEFILE]] or
    * [[FILEDIALOG_OPENFOLDER]]; the convenience wrappers [[showOpenFileDialog]],
    * [[showSaveFileDialog]] and [[showOpenFolderDialog]] name them.
    *
    * Pass `window` to get a sheet attached to it, which leaves the app running while the dialog
    * is up; without one the call blocks until the user is done (see the note above). Everything
    * else is a hint that a platform may ignore: `defaultLocation` (a directory if it ends in a
    * separator, otherwise a directory and a pre-filled name), `allowMany`, and the `title` /
    * `accept` / `cancel` labels.
    *
    * The callback runs once and is then forgotten. Several dialogs may be open at a time; each
    * has its own id. */
  def showFileDialog(
      dialogType:      Int,
      window:          Window = new Window(null),
      filters:         Seq[FileFilter] = Nil,
      defaultLocation: String = null,
      allowMany:       Boolean = false,
      title:           String = null,
      accept:          String = null,
      cancel:          String = null,
  )(callback: DialogResult => Unit): Unit =
    val id = nextDialogId
    nextDialogId += 1
    dialogCallbacks(id) = callback

    if filters.nonEmpty then
      val n   = filters.length
      val arr = stdlib.malloc((n * sizeof[sdl.SDL_DialogFileFilter].toInt).toUSize)
        .asInstanceOf[Ptr[sdl.SDL_DialogFileFilter]]
      for (f, i) <- filters.zipWithIndex do
        (arr + i)._1 = cstrdup(f.name)
        (arr + i)._2 = cstrdup(f.pattern)
      dialogFilterBufs(id) = (arr, n)

    // SDL hands `userdata` back untouched; a malloc'd int is the simplest thing to key the map
    // on that does not depend on casting an integer to a pointer. The trampoline frees it.
    val userdata = stdlib.malloc(4.toUSize).asInstanceOf[Ptr[CInt]]
    !userdata = id

    val props = sdl.SDL_CreateProperties()
    // The property strings are copied by SDL, so the zone may reclaim them on the way out. The
    // filters array is NOT copied — hence the malloc above and the free in the trampoline.
    Zone {
      dialogFilterBufs.get(id).foreach { (arr, n) =>
        sdl.SDL_SetPointerProperty(props, toCString("SDL.filedialog.filters"), arr.asInstanceOf[Ptr[Byte]])
        sdl.SDL_SetNumberProperty(props, toCString("SDL.filedialog.nfilters"), n.toLong)
      }
      if !window.isNull then
        sdl.SDL_SetPointerProperty(props, toCString("SDL.filedialog.window"), window.ptr)
      if defaultLocation != null then
        sdl.SDL_SetStringProperty(props, toCString("SDL.filedialog.location"), toCString(defaultLocation))
      if allowMany then sdl.SDL_SetBooleanProperty(props, toCString("SDL.filedialog.many"), true)
      if title != null then sdl.SDL_SetStringProperty(props, toCString("SDL.filedialog.title"), toCString(title))
      if accept != null then sdl.SDL_SetStringProperty(props, toCString("SDL.filedialog.accept"), toCString(accept))
      if cancel != null then sdl.SDL_SetStringProperty(props, toCString("SDL.filedialog.cancel"), toCString(cancel))
      sdl.SDL_ShowFileDialogWithProperties(dialogType, dialogTrampoline, userdata.asInstanceOf[Ptr[Byte]], props)
    }
    sdl.SDL_DestroyProperties(props)

  /** Ask the user to pick an existing file (or several, with `allowMany`). See [[showFileDialog]]. */
  def showOpenFileDialog(
      window:          Window = new Window(null),
      filters:         Seq[FileFilter] = Nil,
      defaultLocation: String = null,
      allowMany:       Boolean = false,
      title:           String = null,
      accept:          String = null,
      cancel:          String = null,
  )(callback: DialogResult => Unit): Unit =
    showFileDialog(FILEDIALOG_OPENFILE, window, filters, defaultLocation, allowMany, title, accept, cancel)(callback)

  /** Ask the user to name a file to write. The chosen path need not exist, and the panel does the
    * "already exists — overwrite?" prompt itself. See [[showFileDialog]]. */
  def showSaveFileDialog(
      window:          Window = new Window(null),
      filters:         Seq[FileFilter] = Nil,
      defaultLocation: String = null,
      title:           String = null,
      accept:          String = null,
      cancel:          String = null,
  )(callback: DialogResult => Unit): Unit =
    showFileDialog(FILEDIALOG_SAVEFILE, window, filters, defaultLocation, false, title, accept, cancel)(callback)

  /** Ask the user to pick a folder. Filters do not apply. See [[showFileDialog]]. */
  def showOpenFolderDialog(
      window:          Window = new Window(null),
      defaultLocation: String = null,
      allowMany:       Boolean = false,
      title:           String = null,
      accept:          String = null,
      cancel:          String = null,
  )(callback: DialogResult => Unit): Unit =
    showFileDialog(FILEDIALOG_OPENFOLDER, window, Nil, defaultLocation, allowMany, title, accept, cancel)(callback)

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
    /** Set the window's title bar text. */
    def setTitle(title: String): Unit = Zone(sdl.SDL_SetWindowTitle(ptr, toCString(title)))
    def pixelFormat: Int                  = sdl.SDL_GetWindowPixelFormat(ptr).toInt

    /** Begin delivering text-input events for this window — `TEXT_INPUT` events
      * (and the on-screen/IME keyboard where the platform has one). The typed text
      * is read with [[Event.text]]. Pair with [[stopTextInput]]; returns true on
      * success. A text field enables this while focused and disables it on blur. */
    def startTextInput(): Boolean = sdl.SDL_StartTextInput(ptr)

    /** Stop delivering text-input events for this window. */
    def stopTextInput(): Boolean = sdl.SDL_StopTextInput(ptr)
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

    /** Blit a sub-rectangle of a texture into a destination rectangle, both in pixels — the
      * general form. `src` selects the region of the texture to read (an atlas entry, a frame's
      * visible area inside a padded decode buffer); `dst` is where it lands on the target, with
      * any scale between them done by the renderer. */
    def copy(t: Texture, src: (Double, Double, Double, Double), dst: (Double, Double, Double, Double)): Unit =
      // The two rects must come from different scratch buffers — see `frect`/`frectSrc`.
      val s = frectSrc(src._1, src._2, src._3, src._4)
      val d = frect(dst._1, dst._2, dst._3, dst._4)
      sdl.SDL_RenderTexture(ptr, t.ptr, s, d)

    def createTexture(format: Int, access: Int, w: Int, h: Int): Texture =
      new Texture(sdl.SDL_CreateTexture(ptr, format.toUInt, access, w, h))

    /** Create a YUV texture that declares its `colorspace` — the only way to set one, since it is
      * fixed at creation and [[createTexture]] has no parameter for it.
      *
      * This matters: SDL assumes [[COLORSPACE_BT601_LIMITED]] for a YUV texture created without
      * one, so HD footage (which is [[COLORSPACE_BT709_LIMITED]]) comes out with shifted colour —
      * a quiet, wrong-looking result rather than an error. Pass what the source declares.
      *
      * `format` should be one of the YUV formats ([[PIXELFORMAT_IYUV]], [[PIXELFORMAT_NV12]], …)
      * and `access` is normally [[TEXTUREACCESS_STREAMING]], the mode for a texture re-uploaded
      * every frame. Returns a null texture on failure, as [[createTexture]] does. */
    def createYUVTexture(format: Int, access: Int, w: Int, h: Int, colorspace: Int): Texture =
      val props = sdl.SDL_CreateProperties()
      if props == 0.toUInt then new Texture(null)
      else
        Zone {
          sdl.SDL_SetNumberProperty(props, toCString("SDL.texture.create.format"), format.toLong)
          sdl.SDL_SetNumberProperty(props, toCString("SDL.texture.create.access"), access.toLong)
          sdl.SDL_SetNumberProperty(props, toCString("SDL.texture.create.width"), w.toLong)
          sdl.SDL_SetNumberProperty(props, toCString("SDL.texture.create.height"), h.toLong)
          sdl.SDL_SetNumberProperty(props, toCString("SDL.texture.create.colorspace"), colorspace.toLong)
        }
        val t = sdl.SDL_CreateTextureWithProperties(ptr, props)
        sdl.SDL_DestroyProperties(props)
        new Texture(t)
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

  // Scratch SDL_FRects ({float x, y, w, h}) for the rect-taking render calls. Kept on
  // the heap for the process lifetime rather than `stackalloc`'d: stack memory belongs
  // to the frame that allocates it, so a pointer returned from this helper would dangle
  // the moment the helper returns and SDL would read garbage. Refilled per call and
  // reused; safe because SDL reads them synchronously within the call and rendering is
  // single-threaded.
  //
  // There are TWO, because `copy(texture, src, dst)` needs both live at once. They are
  // separate helpers rather than one with an index so that the "which buffer am I in"
  // question cannot be got wrong at a call site: a source rect uses `frectSrc`, every
  // other rect uses `frect`. Filling one buffer twice for one call silently aliases the
  // two arguments — SDL then reads the destination rect as the source and draws nothing.
  private val frectBuf: Ptr[Float] = stdlib.malloc(16.toUSize).asInstanceOf[Ptr[Float]] // 4 × f32
  private[sdl3] def frect(x: Double, y: Double, w: Double, h: Double): Ptr[Float] =
    frectBuf(0) = x.toFloat; frectBuf(1) = y.toFloat; frectBuf(2) = w.toFloat; frectBuf(3) = h.toFloat
    frectBuf

  private val frectSrcBuf: Ptr[Float] = stdlib.malloc(16.toUSize).asInstanceOf[Ptr[Float]] // 4 × f32
  private[sdl3] def frectSrc(x: Double, y: Double, w: Double, h: Double): Ptr[Float] =
    frectSrcBuf(0) = x.toFloat; frectSrcBuf(1) = y.toFloat; frectSrcBuf(2) = w.toFloat; frectSrcBuf(3) = h.toFloat
    frectSrcBuf

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

    /** How this texture's pixels combine with what is already in the render target — one of the
      * `BLENDMODE_*` values. A texture defaults to [[BLENDMODE_NONE]], which *replaces* the
      * target; set [[BLENDMODE_BLEND]] to have its alpha respected, as a UI layer composited
      * over content beneath it must. */
    def setBlendMode(mode: Int): Unit = sdl.SDL_SetTextureBlendMode(ptr, mode.toUInt)

    /** Upload a 3-plane YUV frame (an [[PIXELFORMAT_IYUV]] or [[PIXELFORMAT_YV12]] texture),
      * replacing all of it. Each plane is a pointer to its first byte and a pitch in **bytes per
      * row** — which is a decoder's stride, not necessarily the frame width, since planes are
      * commonly padded for alignment. Chroma planes are half-size on both axes (4:2:0).
      *
      * Pass the planes in Y, U, V order regardless of the texture's format: for a YV12 texture
      * SDL swaps them itself. Mapping from libavcodec, `data(0)/linesize(0)` is Y, `1` is U and
      * `2` is V. Returns true on success. */
    def updateYUV(
        y:      Ptr[Byte],
        yPitch: Int,
        u:      Ptr[Byte],
        uPitch: Int,
        v:      Ptr[Byte],
        vPitch: Int,
    ): Boolean =
      sdl.SDL_UpdateYUVTexture(ptr, null, y, yPitch, u, uPitch, v, vPitch)

    /** Upload a 2-plane YUV frame (an [[PIXELFORMAT_NV12]] or [[PIXELFORMAT_NV21]] texture),
      * replacing all of it — the layout hardware decoders produce, where the two chroma channels
      * are interleaved into one plane. `uvPitch` counts bytes per row of that combined plane, so
      * for 4:2:0 it spans width/2 U/V *pairs* and is typically the same as `yPitch`. */
    def updateNV(y: Ptr[Byte], yPitch: Int, uv: Ptr[Byte], uvPitch: Int): Boolean =
      sdl.SDL_UpdateNVTexture(ptr, null, y, yPitch, uv, uvPitch)

    /** Upload a CPU pixel buffer into this (STREAMING) texture, replacing all of it.
      * `pixels` points at the source bytes and `pitch` is the number of bytes per row
      * (e.g. a Cairo image surface's `getData` and `getStride`). The buffer's pixel layout
      * must match the texture's format — pair a [[PIXELFORMAT_ARGB8888]] texture with a
      * Cairo `Format.ARGB32` surface. Returns true on success. */
    def update(pixels: Ptr[Byte], pitch: Int): Boolean =
      sdl.SDL_UpdateTexture(ptr, null, pixels, pitch)
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

  // ---- audio (push/queue model) ----
  //
  // These wrap SDL's "open a device stream with a null callback" path: SDL runs its own audio
  // thread and pulls from the stream's internal queue, so the caller never supplies a callback
  // and never starts a thread — it synthesises a finished buffer and pushes it with
  // [[AudioStream.put]]. Ideal for short one-shot effects whose samples are known up front.

  /** Bring up the audio subsystem (independent of video, so it can follow window creation).
    * `true` on success. */
  def initAudio(): Boolean = sdl.SDL_InitSubSystem(INIT_AUDIO.toUInt)

  def quitAudio(): Unit = sdl.SDL_QuitSubSystem(INIT_AUDIO.toUInt)

  /** Open a playback stream on the default device for float32 PCM at `freq` Hz and `channels`
    * channels, and start it. Push samples with [[AudioStream.put]]; check [[AudioStream.isNull]]
    * for failure. Opening several streams on the default device is fine — SDL mixes them — which
    * is how overlapping effects play at once. */
  def openAudioStream(freq: Int, channels: Int = 1): AudioStream =
    val spec = stackalloc[CInt](3) // {SDL_AudioFormat format; int channels; int freq}
    spec(0) = AUDIO_F32
    spec(1) = channels
    spec(2) = freq
    val s = new AudioStream(
      sdl.SDL_OpenAudioDeviceStream(AUDIO_DEVICE_DEFAULT_PLAYBACK.toUInt, spec.asInstanceOf[Ptr[Byte]], null, null),
    )
    if !s.isNull then s.resume()
    s

  /** A float32 PCM playback stream. Pointer-wrapping AnyVal, like the other handles. */
  implicit class AudioStream(val ptr: sdl.SDL_AudioStream) extends AnyVal:
    def isNull: Boolean   = ptr == null
    def resume(): Boolean = sdl.SDL_ResumeAudioStreamDevice(ptr)
    def pause(): Boolean  = sdl.SDL_PauseAudioStreamDevice(ptr)

    /** Queue float32 samples (each in [-1, 1]) for playback. SDL copies them synchronously, so
      * the array can be reused or collected immediately after. */
    def put(samples: Array[Float]): Boolean =
      val n = samples.length
      if n == 0 then true
      else
        val buf = stdlib.malloc((n * 4).toUSize).asInstanceOf[Ptr[Float]]
        var i   = 0
        while i < n do
          buf(i) = samples(i)
          i += 1
        val ok = sdl.SDL_PutAudioStreamData(ptr, buf.asInstanceOf[Ptr[Byte]], n * 4)
        stdlib.free(buf.asInstanceOf[Ptr[Byte]])
        ok

    /** Bytes still queued but not yet consumed by the device — 0 means the voice is idle, which
      * lets a player pick a free stream to avoid cutting off a sound that is still playing. */
    def queued: Int      = sdl.SDL_GetAudioStreamQueued(ptr)
    def clear(): Boolean = sdl.SDL_ClearAudioStream(ptr)
    def destroy(): Unit  = sdl.SDL_DestroyAudioStream(ptr)

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
    private def u16(off: Int): Int      = u8(off) | (u8(off + 1) << 8)
    private def bool(off: Int): Boolean = !(ptr + off) != 0

    def kind: Int = (!ptr.asInstanceOf[Ptr[UInt]]).toInt

    /** Keyboard events: the physical key (an SDL scancode) and key-repeat flag. */
    def keyScancode: Int   = i32(24)
    def keyRepeat: Boolean = bool(37)
    /** Keyboard events: the active modifier keys, a bitmask of the `KMOD_*` values, read
      * from `SDL_KeyboardEvent.mod` (a `Uint16` at offset 32 in the 64-bit layout). Test a
      * side-agnostic modifier with the combined alias, e.g. `(e.keyMod & KMOD_SHIFT) != 0`. */
    def keyMod: Int = u16(32)
    /** Mouse motion events: cursor position. */
    def mouseX: Double = f32(28).toDouble
    def mouseY: Double = f32(32).toDouble
    /** Mouse button events: which button (1=left, 2=middle, 3=right). */
    def mouseButton: Int = u8(24)
    /** Mouse wheel events: scroll amounts (positive y = away from the user). */
    def wheelX: Double = f32(24).toDouble
    def wheelY: Double = f32(28).toDouble

    /** Text-input events (`TEXT_INPUT`): the typed text, UTF-8. SDL3's
      * `SDL_TextInputEvent.text` is a `const char *` at offset 24 in the 64-bit
      * layout, so read the pointer and copy the string out. */
    def text: String =
      val s = !((ptr + 24).asInstanceOf[Ptr[CString]])
      if s == null then "" else fromCString(s)

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
