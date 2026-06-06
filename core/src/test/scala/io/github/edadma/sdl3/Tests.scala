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
