package io.github.edadma

import scala.scalanative.unsafe.*
import io.github.edadma.sdl3.{Renderer, Surface, Texture}

/** Pure-Scala SDL_image 3 layer — the only package consumers import. Decodes
  * image files (PNG, JPEG, etc.) into an `sdl3.Surface` (upload to a `Texture`
  * with `renderer.createTextureFromSurface`) or straight into a ready-to-draw
  * `sdl3.Texture` via [[loadTexture]].
  *
  * SDL_image 3 needs no init/quit. It is Native-only, built on the
  * `io.github.edadma::sdl3` binding.
  */
package object sdl3_image:

  import extern.{LibSDL3Image => img}

  /** Decode an image file into a CPU surface. Check `isNull`; the message is
    * available from `io.github.edadma.sdl3.error`.
    */
  def load(path: String): Surface =
    Zone(new Surface(img.IMG_Load(toCString(path))))

  /** Decode an image file straight into a GPU texture for the given renderer. */
  def loadTexture(renderer: Renderer, path: String): Texture =
    Zone(new Texture(img.IMG_LoadTexture(renderer.ptr, toCString(path))))

  /** Save a surface to a PNG file; `true` on success. */
  def savePNG(surface: Surface, path: String): Boolean =
    Zone(img.IMG_SavePNG(surface.ptr, toCString(path)))
