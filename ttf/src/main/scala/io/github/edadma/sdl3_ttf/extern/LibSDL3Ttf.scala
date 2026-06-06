package io.github.edadma.sdl3_ttf.extern

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

/** Raw `@extern` bindings to SDL_ttf 3 — the only place Scala Native FFI types
  * appear. Consumers use the pure-Scala layer in the `io.github.edadma.sdl3_ttf`
  * package. `@link("SDL3_ttf")` pulls in libSDL3_ttf; libSDL3 itself comes in
  * transitively from the `sdl3` dependency's `@link("SDL3")`.
  *
  * Compared with SDL2_ttf: `TTF_Init` returns `bool`, point sizes are `float`,
  * and every render call takes a `length` (0 means null-terminated). `SDL_Color`
  * is still a 4-byte struct passed **by value** — a `CStruct4[UByte, …]` built
  * on the stack and loaded with `!` at the call site.
  */
@link("SDL3_ttf")
@extern
object LibSDL3Ttf:
  type TTF_Font    = Ptr[Byte]
  type SDL_Surface = Ptr[Byte]
  type SDL_Color   = CStruct4[UByte, UByte, UByte, UByte] // r, g, b, a

  def TTF_Init(): CBool = extern
  def TTF_Quit(): Unit  = extern

  def TTF_OpenFont(file: CString, ptsize: Float): TTF_Font  = extern
  def TTF_CloseFont(font: TTF_Font): Unit                   = extern
  def TTF_SetFontSize(font: TTF_Font, ptsize: Float): CBool = extern

  def TTF_SetFontStyle(font: TTF_Font, style: UInt): Unit = extern
  def TTF_GetFontStyle(font: TTF_Font): UInt              = extern

  def TTF_GetFontHeight(font: TTF_Font): CInt   = extern
  def TTF_GetFontAscent(font: TTF_Font): CInt   = extern
  def TTF_GetFontDescent(font: TTF_Font): CInt  = extern
  def TTF_GetFontLineSkip(font: TTF_Font): CInt = extern
  def TTF_GetStringSize(font: TTF_Font, text: CString, length: CSize, w: Ptr[CInt], h: Ptr[CInt]): CBool = extern

  def TTF_RenderText_Solid(font: TTF_Font, text: CString, length: CSize, fg: SDL_Color): SDL_Surface = extern
  def TTF_RenderText_Shaded(font: TTF_Font, text: CString, length: CSize, fg: SDL_Color, bg: SDL_Color): SDL_Surface = extern
  def TTF_RenderText_Blended(font: TTF_Font, text: CString, length: CSize, fg: SDL_Color): SDL_Surface = extern
  def TTF_RenderText_Blended_Wrapped(font: TTF_Font, text: CString, length: CSize, fg: SDL_Color, wrapWidth: CInt): SDL_Surface = extern
