package io.github.edadma.sdl3_image

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Tests extends AnyFreeSpec with Matchers:

  "loading a missing file yields a null surface" in {
    // Exercises the IMG_Load FFI path and the sdl3.Surface wrapper without
    // needing a display: a nonexistent file decodes to NULL.
    val s = load("/nonexistent/path/does-not-exist.png")
    s.isNull shouldBe true
  }
