package io.github.edadma.sdl3.extern

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

/** Raw `@extern` bindings to SDL3 — the only place Scala Native FFI types
  * appear. Consumers never import this; they use the pure-Scala layer in the
  * `io.github.edadma.sdl3` package, which wraps these in `AnyVal` types and
  * Scala-native values. `@link("SDL3")` makes the native linker pull in libSDL3.
  *
  * Signatures track SDL 3.2+: `SDL_Init` and the render/query calls return
  * `bool`; window flags are 64-bit; the render API takes `float` coordinates
  * (`SDL_FRect`/`SDL_FPoint` are laid out as contiguous `float`s here).
  */
@link("SDL3")
@extern
object LibSDL3:
  // Opaque handles — pointers the high-level layer wraps in AnyVal types.
  type SDL_Window   = Ptr[Byte]
  type SDL_Renderer = Ptr[Byte]
  type SDL_Texture  = Ptr[Byte]
  type SDL_Surface  = Ptr[Byte]

  // SDL_Event is a ~128-byte union; the high-level layer keeps one buffer and
  // reads fields by their (ABI-stable, 64-bit) offsets.
  type SDL_Event = Ptr[Byte]

  // bool (*)(void *userdata, SDL_Event *event) — used by event filters/watches.
  type SDL_EventFilter = CFuncPtr2[Ptr[Byte], Ptr[Byte], CBool]

  def SDL_SetMainReady(): Unit                            = extern
  def SDL_Init(flags: UInt): CBool                        = extern
  def SDL_Quit(): Unit                                    = extern
  def SDL_GetError(): CString                             = extern
  def SDL_Delay(ms: UInt): Unit                           = extern
  def SDL_SetHint(name: CString, value: CString): CBool   = extern

  // SDL_WindowFlags is a 64-bit mask in SDL3; position is set separately.
  def SDL_CreateWindow(title: CString, w: CInt, h: CInt, flags: ULong): SDL_Window = extern
  def SDL_DestroyWindow(window: SDL_Window): Unit                                   = extern
  def SDL_SetWindowPosition(window: SDL_Window, x: CInt, y: CInt): CBool            = extern
  def SDL_GetWindowSize(window: SDL_Window, w: Ptr[CInt], h: Ptr[CInt]): CBool      = extern
  def SDL_GetWindowSizeInPixels(window: SDL_Window, w: Ptr[CInt], h: Ptr[CInt]): CBool = extern
  def SDL_GetWindowPixelFormat(window: SDL_Window): UInt                            = extern

  def SDL_CreateRenderer(window: SDL_Window, name: CString): SDL_Renderer = extern
  def SDL_DestroyRenderer(renderer: SDL_Renderer): Unit                   = extern
  def SDL_SetRenderDrawColor(renderer: SDL_Renderer, r: UByte, g: UByte, b: UByte, a: UByte): CBool = extern
  def SDL_SetRenderDrawColorFloat(renderer: SDL_Renderer, r: Float, g: Float, b: Float, a: Float): CBool = extern
  def SDL_SetRenderDrawBlendMode(renderer: SDL_Renderer, blendMode: UInt): CBool   = extern
  def SDL_RenderClear(renderer: SDL_Renderer): CBool                               = extern
  def SDL_RenderPoint(renderer: SDL_Renderer, x: Float, y: Float): CBool           = extern
  def SDL_RenderLine(renderer: SDL_Renderer, x1: Float, y1: Float, x2: Float, y2: Float): CBool = extern
  // SDL_FRect* — a pointer to {float x, y, w, h}.
  def SDL_RenderRect(renderer: SDL_Renderer, rect: Ptr[Float]): CBool              = extern
  def SDL_RenderFillRect(renderer: SDL_Renderer, rect: Ptr[Float]): CBool          = extern
  def SDL_RenderTexture(renderer: SDL_Renderer, texture: SDL_Texture, srcrect: Ptr[Float], dstrect: Ptr[Float]): CBool = extern
  // SDL_Vertex is {SDL_FPoint position; SDL_FColor color; SDL_FPoint tex_coord} —
  // 8 contiguous floats (pos.xy, color.rgba, tex.uv), 32 bytes, no padding. The
  // high-level layer builds the vertex array as raw floats and passes it here.
  def SDL_RenderGeometry(renderer: SDL_Renderer, texture: SDL_Texture, vertices: Ptr[Float], numVertices: CInt, indices: Ptr[CInt], numIndices: CInt): CBool = extern
  def SDL_RenderPresent(renderer: SDL_Renderer): CBool                             = extern
  def SDL_SetRenderTarget(renderer: SDL_Renderer, texture: SDL_Texture): CBool     = extern
  def SDL_SetRenderVSync(renderer: SDL_Renderer, vsync: CInt): CBool               = extern

  def SDL_CreateTexture(renderer: SDL_Renderer, format: UInt, access: CInt, w: CInt, h: CInt): SDL_Texture = extern
  def SDL_CreateTextureFromSurface(renderer: SDL_Renderer, surface: SDL_Surface): SDL_Texture = extern
  def SDL_UpdateTexture(texture: SDL_Texture, rect: Ptr[Byte], pixels: Ptr[Byte], pitch: CInt): CBool = extern
  def SDL_DestroyTexture(texture: SDL_Texture): Unit                               = extern
  def SDL_SetTextureScaleMode(texture: SDL_Texture, scaleMode: CInt): CBool        = extern
  def SDL_GetTextureSize(texture: SDL_Texture, w: Ptr[Float], h: Ptr[Float]): CBool = extern
  def SDL_DestroySurface(surface: SDL_Surface): Unit                               = extern

  def SDL_PollEvent(event: SDL_Event): CBool                                       = extern
  // const bool* — one byte per scancode in SDL3 (was Uint8* in SDL2).
  def SDL_GetKeyboardState(numkeys: Ptr[CInt]): Ptr[CBool]                         = extern
  def SDL_GetMouseState(x: Ptr[Float], y: Ptr[Float]): UInt                        = extern
  def SDL_AddEventWatch(filter: SDL_EventFilter, userdata: Ptr[Byte]): CBool       = extern
  def SDL_RemoveEventWatch(filter: SDL_EventFilter, userdata: Ptr[Byte]): Unit     = extern
