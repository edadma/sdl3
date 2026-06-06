package io.github.edadma.sdl3_mixer

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Tests extends AnyFreeSpec with Matchers:

  "LOOP_FOREVER is -1" in {
    LOOP_FOREVER shouldBe -1
  }

  "mixInit then mixQuit completes" in {
    // Forces the SDL3_mixer link and exercises the init/quit FFI path without
    // opening a device (so it runs headless).
    mixInit()
    mixQuit()
    succeed
  }
