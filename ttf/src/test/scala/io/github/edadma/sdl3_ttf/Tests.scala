package io.github.edadma.sdl3_ttf

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import io.github.edadma.sdl3_ttf.extern.LibSDL3Ttf
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Tests extends AnyFreeSpec with Matchers:

  "font style flags are distinct bits" in {
    (STYLE_BOLD | STYLE_ITALIC) shouldBe 0x03
    (STYLE_NORMAL | STYLE_UNDERLINE) shouldBe STYLE_UNDERLINE
    Seq(STYLE_BOLD, STYLE_ITALIC, STYLE_UNDERLINE, STYLE_STRIKETHROUGH).distinct.length shouldBe 4
  }

  "SDL_Color is a 4-byte by-value struct that round-trips its channels" in {
    val (r, g, b, a, sz) = Zone {
      val p = alloc[LibSDL3Ttf.SDL_Color]()
      p._1 = 200.toUByte; p._2 = 100.toUByte; p._3 = 50.toUByte; p._4 = 255.toUByte
      (p._1.toInt, p._2.toInt, p._3.toInt, p._4.toInt, sizeof[LibSDL3Ttf.SDL_Color].toInt)
    }
    r shouldBe 200
    g shouldBe 100
    b shouldBe 50
    a shouldBe 255
    sz shouldBe 4
  }
