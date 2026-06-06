package io.github.edadma.sdl3_ttf.extern

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

/** Raw `@extern` bindings to SDL_ttf 3 — the only place Scala Native FFI types
  * appear. Consumers use the pure-Scala layer in the `io.github.edadma.sdl3_ttf`
  * package. `@link("SDL3_ttf")` pulls in libSDL3_ttf; libSDL3 itself comes in
  * transitively from the `sdl3` dependency's `@link("SDL3")`.
  *
  * Compared with SDL2_ttf: `TTF_Init` returns `bool`, point sizes are `float`,
  * and every render call takes a `length` (0 means null-terminated).
  *
  * `SDL_Color` is a 4-byte struct (`Uint8 r, g, b, a`) passed **by value** in C.
  * Scala Native mis-marshals a small by-value struct argument here — the callee
  * receives bytes derived from the argument pointer rather than the struct's
  * contents — so the colour is passed instead as a packed 32-bit integer. That is
  * ABI-identical: an integer-class aggregate of four bytes and a `uint32` are
  * passed in the same register on the SysV-AMD64 and AArch64 C ABIs (the platforms
  * this binding targets). The wrapper layer packs the channels little-endian
  * (`r | g<<8 | b<<16 | a<<24`), matching the in-memory order of `SDL_Color` on a
  * little-endian host, so C reinterprets the register's bytes as the right struct.
  */
@link("SDL3_ttf")
@extern
object LibSDL3Ttf:
  type TTF_Font    = Ptr[Byte]
  type SDL_Surface = Ptr[Byte]

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

  // `fg`/`bg` are SDL_Color passed as a packed little-endian uint32 — see the note above.
  def TTF_RenderText_Solid(font: TTF_Font, text: CString, length: CSize, fg: CUnsignedInt): SDL_Surface = extern
  def TTF_RenderText_Shaded(font: TTF_Font, text: CString, length: CSize, fg: CUnsignedInt, bg: CUnsignedInt): SDL_Surface = extern
  def TTF_RenderText_Blended(font: TTF_Font, text: CString, length: CSize, fg: CUnsignedInt): SDL_Surface = extern
  def TTF_RenderText_Blended_Wrapped(font: TTF_Font, text: CString, length: CSize, fg: CUnsignedInt, wrapWidth: CInt): SDL_Surface = extern
