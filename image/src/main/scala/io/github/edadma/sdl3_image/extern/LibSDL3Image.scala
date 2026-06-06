package io.github.edadma.sdl3_image.extern

import scala.scalanative.unsafe.*

/** Raw `@extern` bindings to SDL_image 3 — the only place Scala Native FFI
  * types appear. Consumers use the pure-Scala layer in the
  * `io.github.edadma.sdl3_image` package. `@link("SDL3_image")` pulls in
  * libSDL3_image; libSDL3 itself comes in transitively from the `sdl3`
  * dependency's `@link("SDL3")`.
  *
  * SDL_image 3 has no init/quit step — decoders are loaded on demand, so the
  * surface/texture loaders below are the whole surface.
  */
@link("SDL3_image")
@extern
object LibSDL3Image:
  type SDL_Surface  = Ptr[Byte]
  type SDL_Renderer = Ptr[Byte]
  type SDL_Texture  = Ptr[Byte]

  def IMG_Load(file: CString): SDL_Surface                            = extern
  def IMG_LoadTexture(renderer: SDL_Renderer, file: CString): SDL_Texture = extern
  def IMG_SavePNG(surface: SDL_Surface, file: CString): CBool         = extern
