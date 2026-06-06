package io.github.edadma

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import io.github.edadma.sdl3.{Color, Renderer, Surface, Texture}

/** Pure-Scala SDL_ttf 3 layer — the only package consumers import. Text is
  * rendered with the familiar `sdl3.Color`; results come back as `sdl3.Surface`
  * (upload to a `Texture` with `renderer.createTextureFromSurface`) or, via
  * `Font.texture`, straight as a ready-to-draw `Texture`.
  *
  * `ttfInit`/`ttfQuit` are named to avoid clashing with sdl3's `init`/`quit`
  * when both packages are imported. SDL_ttf 3 is Native-only, built on the
  * `io.github.edadma::sdl3` binding.
  */
package object sdl3_ttf:

  import extern.{LibSDL3Ttf => ttf}

  // font style flags (combine with |)
  val STYLE_NORMAL        = 0x00
  val STYLE_BOLD          = 0x01
  val STYLE_ITALIC        = 0x02
  val STYLE_UNDERLINE     = 0x04
  val STYLE_STRIKETHROUGH = 0x08

  // Render calls take a byte length; 0 tells SDL_ttf the text is null-terminated.
  private val NUL_TERMINATED: CSize = 0.toUSize

  /** Initialise SDL_ttf; `true` on success. Call after SDL is initialised. */
  def ttfInit(): Boolean = ttf.TTF_Init()
  def ttfQuit(): Unit    = ttf.TTF_Quit()

  /** Open a font at a point size. Check `isNull` on the result; the message is
    * available from `io.github.edadma.sdl3.error`.
    */
  def openFont(path: String, ptSize: Double): Font =
    Zone(new Font(ttf.TTF_OpenFont(toCString(path), ptSize.toFloat)))

  // Build an SDL_Color in the current zone for by-value passing. Zone-allocated
  // (not stackalloc) so the pointer stays valid until the enclosing render call.
  private def sdlColor(c: Color)(using Zone): Ptr[ttf.SDL_Color] =
    val p = alloc[ttf.SDL_Color]()
    p._1 = c.r.toUByte; p._2 = c.g.toUByte; p._3 = c.b.toUByte; p._4 = c.a.toUByte
    p

  /** A loaded font. Render methods produce an `sdl3.Surface`; the caller uploads
    * it to a texture and frees it (or uses [[Font.texture]] to do both).
    */
  implicit class Font(val ptr: ttf.TTF_Font) extends AnyVal:
    def isNull: Boolean = ptr == null
    def close(): Unit   = ttf.TTF_CloseFont(ptr)

    def style: Int                = ttf.TTF_GetFontStyle(ptr).toInt
    def style_=(s: Int): Unit     = ttf.TTF_SetFontStyle(ptr, s.toUInt)
    def setSize(ptSize: Double): Unit = ttf.TTF_SetFontSize(ptr, ptSize.toFloat)

    def height: Int   = ttf.TTF_GetFontHeight(ptr)
    def ascent: Int   = ttf.TTF_GetFontAscent(ptr)
    def descent: Int  = ttf.TTF_GetFontDescent(ptr)
    def lineSkip: Int = ttf.TTF_GetFontLineSkip(ptr)

    /** Pixel `(width, height)` a string would occupy, without rendering it. */
    def size(text: String): (Int, Int) = Zone {
      val w = stackalloc[CInt]()
      val h = stackalloc[CInt]()
      ttf.TTF_GetStringSize(ptr, toCString(text), NUL_TERMINATED, w, h)
      (!w, !h)
    }

    /** Fast, aliased text on a transparent background. */
    def renderSolid(text: String, fg: Color): Surface = Zone {
      new Surface(ttf.TTF_RenderText_Solid(ptr, toCString(text), NUL_TERMINATED, !sdlColor(fg)))
    }
    /** Antialiased text on a solid `bg` box. */
    def renderShaded(text: String, fg: Color, bg: Color): Surface = Zone {
      new Surface(ttf.TTF_RenderText_Shaded(ptr, toCString(text), NUL_TERMINATED, !sdlColor(fg), !sdlColor(bg)))
    }
    /** Antialiased text with an alpha channel — the usual choice for a HUD. */
    def renderBlended(text: String, fg: Color): Surface = Zone {
      new Surface(ttf.TTF_RenderText_Blended(ptr, toCString(text), NUL_TERMINATED, !sdlColor(fg)))
    }
    /** Antialiased, word-wrapped at `wrapPixels` (0 wraps only on newlines). */
    def renderBlendedWrapped(text: String, fg: Color, wrapPixels: Int): Surface = Zone {
      new Surface(ttf.TTF_RenderText_Blended_Wrapped(ptr, toCString(text), NUL_TERMINATED, !sdlColor(fg), wrapPixels))
    }

    /** Render blended text straight to a GPU texture. The caller draws it with
      * `renderer.copy(tex, x, y)` and `destroy`s it (or caches it).
      */
    def texture(renderer: Renderer, text: String, fg: Color): Texture =
      val s = renderBlended(text, fg)
      val t = renderer.createTextureFromSurface(s)
      s.free()
      t
