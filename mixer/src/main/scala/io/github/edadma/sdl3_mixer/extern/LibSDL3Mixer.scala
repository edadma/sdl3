package io.github.edadma.sdl3_mixer.extern

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

/** Raw `@extern` bindings to SDL_mixer 3 — the only place Scala Native FFI types
  * appear. Consumers use the pure-Scala layer in the
  * `io.github.edadma.sdl3_mixer` package. `@link("SDL3_mixer")` pulls in
  * libSDL3_mixer; libSDL3 itself comes in transitively from the `sdl3`
  * dependency's `@link("SDL3")`.
  *
  * SDL_mixer 3 is a redesign of SDL2_mixer: a `MIX_Mixer` opens a device, a
  * single `MIX_Audio` type covers both sound effects and music, and a
  * `MIX_Track` is a controllable playback voice. Loop counts are passed through
  * an `SDL_PropertiesID` (see [[LibSDLProps]]).
  */
@link("SDL3_mixer")
@extern
object LibSDL3Mixer:
  type MIX_Mixer = Ptr[Byte]
  type MIX_Audio = Ptr[Byte]
  type MIX_Track = Ptr[Byte]

  def MIX_Init(): CBool = extern
  def MIX_Quit(): Unit  = extern

  // devid is an SDL_AudioDeviceID (Uint32); spec is a `const SDL_AudioSpec *`,
  // passed null for the device's preferred format.
  def MIX_CreateMixerDevice(devid: UInt, spec: Ptr[Byte]): MIX_Mixer = extern
  def MIX_DestroyMixer(mixer: MIX_Mixer): Unit                       = extern

  def MIX_LoadAudio(mixer: MIX_Mixer, path: CString, predecode: CBool): MIX_Audio = extern
  def MIX_DestroyAudio(audio: MIX_Audio): Unit                                    = extern

  def MIX_PlayAudio(mixer: MIX_Mixer, audio: MIX_Audio): CBool = extern

  def MIX_CreateTrack(mixer: MIX_Mixer): MIX_Track       = extern
  def MIX_DestroyTrack(track: MIX_Track): Unit           = extern
  def MIX_SetTrackAudio(track: MIX_Track, audio: MIX_Audio): CBool = extern
  // options is an SDL_PropertiesID (Uint32); 0 means "no options" (play once).
  def MIX_PlayTrack(track: MIX_Track, options: UInt): CBool = extern
  def MIX_StopTrack(track: MIX_Track, fadeOutFrames: Long): CBool = extern
  def MIX_PauseTrack(track: MIX_Track): CBool            = extern
  def MIX_ResumeTrack(track: MIX_Track): CBool           = extern
  def MIX_TrackPlaying(track: MIX_Track): CBool          = extern
  def MIX_SetTrackGain(track: MIX_Track, gain: Float): CBool = extern

/** The few SDL3 core property calls needed to request a loop count for
  * [[LibSDL3Mixer.MIX_PlayTrack]]. These live in libSDL3, so this object links
  * `SDL3` directly. `SDL_PropertiesID` is a `Uint32` handle.
  */
@link("SDL3")
@extern
object LibSDLProps:
  def SDL_CreateProperties(): UInt                                       = extern
  def SDL_SetNumberProperty(props: UInt, name: CString, value: Long): CBool = extern
  def SDL_DestroyProperties(props: UInt): Unit                          = extern
