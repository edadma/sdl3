package io.github.edadma.sdl3

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Tests extends AnyFreeSpec with Matchers:

  "Color.fromRGB unpacks channels" in {
    val c = Color.fromRGB(0x4dabf7)
    c.r shouldBe 0x4d
    c.g shouldBe 0xab
    c.b shouldBe 0xf7
    c.a shouldBe 255
  }

  "Color carries an explicit alpha" in {
    Color(10, 20, 30, 40).a shouldBe 40
  }

  "Color.blend interpolates and clamps" in {
    Color.blend(Color(0, 0, 0), Color(100, 200, 40), 0.5) shouldBe Color(50, 100, 20)
    Color.blend(Color(10, 10, 10), Color(20, 20, 20), 0.0) shouldBe Color(10, 10, 10)
    Color.blend(Color(0, 0, 0), Color(255, 255, 255), 2.0) shouldBe Color(255, 255, 255)
  }
