package io.github.edadma.sdl3

import scala.scalanative.unsafe.*
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

  "circleSegments scales with radius and stays bounded" in {
    circleSegments(0.0) shouldBe 12   // floor
    circleSegments(5.0) shouldBe 17
    circleSegments(1000.0) shouldBe 64 // ceiling
    circleSegments(20.0) should (be >= 12 and be <= 64)
  }

  "putVertex writes position and 0–1 colour into an 8-float SDL_Vertex slot" in {
    val (x, y, r, g, b, a, u, w) = Zone {
      val v = stackalloc[Float](8)
      putVertex(v, 0, 12.0, 34.0, Color(255, 0, 51, 255))
      (v(0), v(1), v(2), v(3), v(4), v(5), v(6), v(7))
    }
    x shouldBe 12.0f
    y shouldBe 34.0f
    r shouldBe 1.0f
    g shouldBe 0.0f
    (b * 255f).round shouldBe 51
    a shouldBe 1.0f
    u shouldBe 0.0f
    w shouldBe 0.0f
  }

  "buildCircle places the centre at vertex 0 and the rim at radius" in {
    val (cx, cy, rimX, rimY, i0, i1, i2, lastWrap) = Zone {
      val segs = 12
      val v    = stackalloc[Float](((segs + 1) * 8))
      val idx  = stackalloc[CInt](segs * 3)
      buildCircle(v, idx, 100.0, 100.0, 10.0, Color.White, segs)
      // vertex 0 = centre; vertex 1 = first rim point (angle 0 → +x).
      (v(0), v(1), v(8), v(9), idx(0), idx(1), idx(2), idx((segs - 1) * 3 + 2))
    }
    cx shouldBe 100.0f
    cy shouldBe 100.0f
    rimX shouldBe 110.0f +- 0.001f
    rimY shouldBe 100.0f +- 0.001f
    i0 shouldBe 0
    i1 shouldBe 1
    i2 shouldBe 2
    lastWrap shouldBe 1 // last triangle wraps back to the first rim vertex
  }

  "frect returns a buffer that stays valid after the call returns" in {
    // Regression: frect must not hand back `stackalloc` memory. Stack memory belongs to
    // the frame that allocates it, so a pointer returned from frect would dangle the
    // instant it returns, and the rect-taking render calls (fillRect/drawRect/copy) would
    // read garbage coordinates — drawing nothing. Read the rect back after frect returns,
    // with intervening stack churn that would clobber a reused stack slot, to prove the
    // buffer survives.
    val p = frect(12.0, 34.0, 56.0, 78.0)
    Zone {
      val churn = stackalloc[Float](256)
      var i     = 0
      while i < 256 do { churn(i) = -1.0f; i += 1 }
    }
    p(0) shouldBe 12.0f
    p(1) shouldBe 34.0f
    p(2) shouldBe 56.0f
    p(3) shouldBe 78.0f
  }

  "frect and frectSrc hand back independent buffers" in {
    // Regression: `copy(texture, src, dst)` needs both rects live at once. Filling one shared
    // scratch buffer twice aliases the arguments — SDL reads the destination rect as the source,
    // reads a nonsense region of the texture, and draws nothing at all. A blank picture, no error.
    val s = frectSrc(1.0, 2.0, 3.0, 4.0)
    val d = frect(10.0, 20.0, 30.0, 40.0)
    // Filling the second must not disturb the first.
    s(0) shouldBe 1.0f
    s(1) shouldBe 2.0f
    s(2) shouldBe 3.0f
    s(3) shouldBe 4.0f
    d(0) shouldBe 10.0f
    d(3) shouldBe 40.0f
    // And they really are two buffers, not one.
    (s == d) shouldBe false
  }

  "PIXELFORMAT_ARGB8888 matches SDL's pixel-format encoding" in {
    // SDL_DEFINE_PIXELFORMAT(PACKED32=6, ARGB=3, 8888=6, bits=32, bytes=4):
    //   (1<<28) | (6<<24) | (3<<20) | (6<<16) | (32<<8) | 4 = 0x16362004
    val expected = (1 << 28) | (6 << 24) | (3 << 20) | (6 << 16) | (32 << 8) | 4
    PIXELFORMAT_ARGB8888 shouldBe expected
    PIXELFORMAT_ARGB8888 shouldBe 0x16362004
  }

  "the YUV pixel formats match SDL's FOURCC encoding" in {
    // SDL_DEFINE_PIXELFOURCC(A,B,C,D) packs the four ASCII bytes little-endian:
    //   (A<<0) | (B<<8) | (C<<16) | (D<<24)
    // Worth pinning: a mistyped digit here is not an error at runtime — the texture is simply
    // created in the wrong format and the frame decodes to garbage or wrong colour.
    def fourcc(s: String): Int =
      (s(0).toInt << 0) | (s(1).toInt << 8) | (s(2).toInt << 16) | (s(3).toInt << 24)

    PIXELFORMAT_IYUV shouldBe fourcc("IYUV")
    PIXELFORMAT_YV12 shouldBe fourcc("YV12")
    PIXELFORMAT_NV12 shouldBe fourcc("NV12")
    PIXELFORMAT_NV21 shouldBe fourcc("NV21")

    // The literals as SDL_pixels.h states them.
    PIXELFORMAT_IYUV shouldBe 0x56555949
    PIXELFORMAT_YV12 shouldBe 0x32315659
    PIXELFORMAT_NV12 shouldBe 0x3231564e
    PIXELFORMAT_NV21 shouldBe 0x3132564e

    // IYUV/YV12 and NV12/NV21 are each other's chroma-swapped twins, so the pairs must stay
    // distinct — swapping them turns blue people orange rather than failing.
    Set(PIXELFORMAT_IYUV, PIXELFORMAT_YV12, PIXELFORMAT_NV12, PIXELFORMAT_NV21).size shouldBe 4
  }

  "the YUV colorspaces match SDL's SDL_Colorspace values" in {
    // These decide how the renderer's shader converts YUV to RGB. SDL assumes BT.601 limited for
    // a YUV texture created without one, so an HD (BT.709) frame left at the default comes out
    // with shifted colour — quietly wrong, never an error. Pin the values.
    COLORSPACE_BT709_LIMITED shouldBe 0x21100421
    COLORSPACE_BT601_LIMITED shouldBe 0x211018c6
    COLORSPACE_JPEG shouldBe 0x220004c6
    COLORSPACE_BT709_LIMITED should not be COLORSPACE_BT601_LIMITED
  }

  "text-input event kinds match the SDL_EVENT_* values" in {
    TEXT_INPUT shouldBe 0x303
    TEXT_EDITING shouldBe 0x304
  }

  "key modifier aliases combine the left/right sides" in {
    KMOD_SHIFT shouldBe (KMOD_LSHIFT | KMOD_RSHIFT)
    KMOD_CTRL shouldBe (KMOD_LCTRL | KMOD_RCTRL)
    KMOD_SHIFT shouldBe 0x0003
    KMOD_CTRL shouldBe 0x00c0
  }

  "keyMod reads the modifier bitmask from a keyboard event" in {
    // SDL_KeyboardEvent.mod is a Uint16 at offset 32, little-endian. Fabricate an event
    // buffer with left-shift + left-ctrl set and read it back through the accessor.
    val mods = Zone {
      val buf = stackalloc[Byte](128)
      var i   = 0
      while i < 128 do { buf(i) = 0.toByte; i += 1 }
      val m = KMOD_LSHIFT | KMOD_LCTRL
      buf(32) = (m & 0xff).toByte
      buf(33) = ((m >> 8) & 0xff).toByte
      new Event(buf).keyMod
    }
    (mods & KMOD_SHIFT) should not be 0
    (mods & KMOD_CTRL) should not be 0
    (mods & KMOD_ALT) shouldBe 0
  }

  "buildThickLine makes a width-wide quad and rejects zero length" in {
    val (ax, ay, bx, by, ok, degenerate) = Zone {
      val v   = stackalloc[Float](4 * 8)
      val idx = stackalloc[CInt](6)
      // Horizontal line y=0, width 4 → corners offset ±2 in y.
      val ok         = buildThickLine(v, idx, 0.0, 0.0, 10.0, 0.0, 4.0, Color.White)
      val degenerate = buildThickLine(v, idx, 5.0, 5.0, 5.0, 5.0, 4.0, Color.White)
      (v(0), v(1), v(8), v(9), ok, degenerate)
    }
    ok shouldBe true
    degenerate shouldBe false
    ax shouldBe 0.0f
    ay shouldBe 2.0f   // first corner offset +width/2 in y
    bx shouldBe 10.0f
    by shouldBe 2.0f
  }
