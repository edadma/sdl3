package io.github.edadma.sdl3_ttf

import scala.scalanative.unsigned.*
import io.github.edadma.sdl3.Color
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Tests extends AnyFreeSpec with Matchers:

  "font style flags are distinct bits" in {
    (STYLE_BOLD | STYLE_ITALIC) shouldBe 0x03
    (STYLE_NORMAL | STYLE_UNDERLINE) shouldBe STYLE_UNDERLINE
    Seq(STYLE_BOLD, STYLE_ITALIC, STYLE_UNDERLINE, STYLE_STRIKETHROUGH).distinct.length shouldBe 4
  }

  // The render externs take SDL_Color as a packed little-endian uint32 (r in the low
  // byte) rather than a by-value struct, which Scala Native mis-marshals. This pins the
  // channel order so the C side reinterprets the register's bytes as the right colour.
  "packColor lays the channels out little-endian (r in the low byte)" in {
    val packed = packColor(Color(200, 100, 50, 255)).toLong & 0xffffffffL
    (packed & 0xff) shouldBe 200L         // r
    ((packed >> 8) & 0xff) shouldBe 100L  // g
    ((packed >> 16) & 0xff) shouldBe 50L  // b
    ((packed >> 24) & 0xff) shouldBe 255L // a
  }
