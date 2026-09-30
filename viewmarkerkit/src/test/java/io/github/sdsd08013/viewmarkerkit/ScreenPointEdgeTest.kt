package io.github.sdsd08013.viewmarkerkit

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ScreenPointEdgeTest : BehaviorSpec({

    Given("toScreenEdgeIfOutside function") {
        val screenWidth = 1080
        val screenHeight = 1920
        val marginPx = 60
        val center = ScreenPoint(screenWidth / 2, screenHeight / 2)

        When("point is inside the screen boundaries") {
            Then("returns the original point with false") {
                val point = ScreenPoint(540, 960) // Center of screen
                val (result, isAtEdge) = point.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight)

                result shouldBe point
                isAtEdge shouldBe false
            }

            Then("handles points near but inside the margin") {
                val pointNearLeftEdge = ScreenPoint(marginPx + 1, 960)
                val (result1, isAtEdge1) = pointNearLeftEdge.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight)

                result1 shouldBe pointNearLeftEdge
                isAtEdge1 shouldBe false

                val pointNearRightEdge = ScreenPoint(screenWidth - marginPx - 1, 960)
                val (result2, isAtEdge2) = pointNearRightEdge.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight)

                result2 shouldBe pointNearRightEdge
                isAtEdge2 shouldBe false
            }
        }

        When("point is outside screen on the left side") {
            Then("returns edge point with true") {
                val point = ScreenPoint(-100, 960)
                val (result, isAtEdge) = point.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight)

                result.x shouldBe marginPx
                result.y shouldBe 960
                isAtEdge shouldBe true
            }

            Then("handles points just outside the margin") {
                val point = ScreenPoint(marginPx - 1, 960)
                val (result, isAtEdge) = point.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight)

                result.x shouldBe marginPx
                result.y shouldBe 960
                isAtEdge shouldBe true
            }
        }

        When("point is outside screen on the right side") {
            Then("returns edge point with true") {
                val point = ScreenPoint(1200, 960)
                val (result, isAtEdge) = point.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight)

                result.x shouldBe (screenWidth - marginPx)
                result.y shouldBe 960
                isAtEdge shouldBe true
            }
        }

        When("point is outside screen on the top side") {
            Then("returns edge point with true") {
                val point = ScreenPoint(540, -100)
                val (result, isAtEdge) = point.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight)

                result.x shouldBe 540
                result.y shouldBe marginPx
                isAtEdge shouldBe true
            }
        }

        When("point is outside screen on the bottom side") {
            Then("returns edge point with true") {
                val point = ScreenPoint(540, 2000)
                val (result, isAtEdge) = point.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight)

                result.x shouldBe 540
                result.y shouldBe (screenHeight - marginPx)
                isAtEdge shouldBe true
            }
        }

        When("point is outside screen diagonally") {
            Then("returns edge point on the correct side based on angle") {
                // Upper-left quadrant - shallow angle (more horizontal)
                val point1 = ScreenPoint(-200, center.y - 50)
                val (result1, isAtEdge1) = point1.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight)

                result1.x shouldBe marginPx
                isAtEdge1 shouldBe true

                // Upper-right quadrant - steep angle (more vertical)
                val point2 = ScreenPoint(center.x + 50, -200)
                val (result2, isAtEdge2) = point2.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight)

                result2.y shouldBe marginPx
                isAtEdge2 shouldBe true

                // Lower-left quadrant - steep angle (more vertical)
                val point3 = ScreenPoint(center.x - 50, 2200)
                val (result3, isAtEdge3) = point3.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight)

                result3.y shouldBe (screenHeight - marginPx)
                isAtEdge3 shouldBe true

                // Lower-right quadrant - shallow angle (more horizontal)
                val point4 = ScreenPoint(1300, center.y + 50)
                val (result4, isAtEdge4) = point4.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight)

                result4.x shouldBe (screenWidth - marginPx)
                isAtEdge4 shouldBe true
            }
        }

        When("margin is different") {
            Then("adjusts boundaries accordingly") {
                val largeMargin = 120
                val smallMargin = 10
                val point = ScreenPoint(50, 960)

                val (result1, isAtEdge1) = point.toScreenEdgeIfOutside(largeMargin, screenWidth, screenHeight)
                isAtEdge1 shouldBe true // Outside with large margin
                result1.x shouldBe largeMargin

                val (result2, isAtEdge2) = point.toScreenEdgeIfOutside(smallMargin, screenWidth, screenHeight)
                isAtEdge2 shouldBe false // Inside with small margin
                result2 shouldBe point
            }
        }

        When("point is exactly on the margin boundary") {
            Then("treats it as outside") {
                val pointOnLeftBoundary = ScreenPoint(marginPx, 960)
                val (result1, isAtEdge1) = pointOnLeftBoundary.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight)

                result1 shouldBe pointOnLeftBoundary
                isAtEdge1 shouldBe false // On boundary is considered inside

                val pointOnRightBoundary = ScreenPoint(screenWidth - marginPx, 960)
                val (result2, isAtEdge2) = pointOnRightBoundary.toScreenEdgeIfOutside(marginPx, screenWidth, screenHeight)

                result2 shouldBe pointOnRightBoundary
                isAtEdge2 shouldBe false // On boundary is considered inside
            }
        }
    }
})