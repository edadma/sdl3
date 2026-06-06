package io.github.edadma

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

/** Pure-Scala SDL_mixer 3 layer — the only package consumers import. A [[Mixer]]
  * opens an audio device, [[Audio]] is a loaded sound (effect or music), and a
  * [[Track]] is a controllable playback voice (loop, pause, gain, stop).
  *
  * `mixInit`/`mixQuit` are named to avoid clashing with sdl3's `init`/`quit`.
  * SDL_mixer 3 is Native-only, built on the `io.github.edadma::sdl3` binding.
  *
  * {{{
  * import io.github.edadma.sdl3.*
  * import io.github.edadma.sdl3_mixer.*
  *
  * init(INIT_AUDIO); mixInit()
  * val mixer = openMixer()
  * val music = mixer.loadAudio("song.ogg")
  * val track = mixer.createTrack()
  * track.setAudio(music)
  * track.play(LOOP_FOREVER)
  * }}}
  */
package object sdl3_mixer:

  import extern.{LibSDL3Mixer => mix, LibSDLProps => props}

  /** Pass to [[Track.play]] to loop a track endlessly. */
  val LOOP_FOREVER = -1

  // SDL_AUDIO_DEVICE_DEFAULT_PLAYBACK — let SDL pick the default output device.
  private val DEFAULT_PLAYBACK: UInt = 0xffffffff.toUInt

  /** Initialise SDL_mixer; `true` on success. Call after `sdl3.init(INIT_AUDIO)`. */
  def mixInit(): Boolean = mix.MIX_Init()
  def mixQuit(): Unit    = mix.MIX_Quit()

  /** Open the system's default playback device with its preferred format. Check
    * `isNull`; the message is available from `io.github.edadma.sdl3.error`.
    */
  def openMixer(): Mixer = new Mixer(mix.MIX_CreateMixerDevice(DEFAULT_PLAYBACK, null))

  implicit class Mixer(val ptr: mix.MIX_Mixer) extends AnyVal:
    def isNull: Boolean = ptr == null
    /** Load a sound or music file. `predecode` decodes it fully up front (good
      * for short effects); leave it on for music too unless memory is tight. */
    def loadAudio(path: String, predecode: Boolean = true): Audio =
      Zone(new Audio(mix.MIX_LoadAudio(ptr, toCString(path), predecode)))
    /** Create a playback voice on this mixer. */
    def createTrack(): Track = new Track(mix.MIX_CreateTrack(ptr))
    /** Fire-and-forget one-shot playback of a sound on a transient track. */
    def play(audio: Audio): Boolean = mix.MIX_PlayAudio(ptr, audio.ptr)
    def destroy(): Unit = mix.MIX_DestroyMixer(ptr)

  /** A loaded sound or music clip. Reusable across tracks and plays. */
  implicit class Audio(val ptr: mix.MIX_Audio) extends AnyVal:
    def isNull: Boolean = ptr == null
    def destroy(): Unit = mix.MIX_DestroyAudio(ptr)

  /** A controllable playback voice. Assign an [[Audio]], then play/pause/stop it
    * and adjust its gain. */
  implicit class Track(val ptr: mix.MIX_Track) extends AnyVal:
    def isNull: Boolean = ptr == null
    def setAudio(audio: Audio): Boolean = mix.MIX_SetTrackAudio(ptr, audio.ptr)

    /** Start playback. `loops` is the number of *repeats* after the first play
      * (0 plays once, [[LOOP_FOREVER]] loops endlessly). */
    def play(loops: Int = 0): Boolean =
      if loops == 0 then mix.MIX_PlayTrack(ptr, 0.toUInt)
      else
        val p = props.SDL_CreateProperties()
        props.SDL_SetNumberProperty(p, c"SDL_mixer.play.loops", loops.toLong)
        val ok = mix.MIX_PlayTrack(ptr, p)
        props.SDL_DestroyProperties(p)
        ok

    /** Stop the track, optionally fading out over `fadeOutFrames` sample frames. */
    def stop(fadeOutFrames: Long = 0): Boolean = mix.MIX_StopTrack(ptr, fadeOutFrames)
    def pause(): Boolean                        = mix.MIX_PauseTrack(ptr)
    def resume(): Boolean                       = mix.MIX_ResumeTrack(ptr)
    def playing: Boolean                        = mix.MIX_TrackPlaying(ptr)
    /** Set per-track gain (1.0 = unity, 0.0 = silent). */
    def setGain(gain: Double): Boolean = mix.MIX_SetTrackGain(ptr, gain.toFloat)
    def destroy(): Unit                = mix.MIX_DestroyTrack(ptr)
