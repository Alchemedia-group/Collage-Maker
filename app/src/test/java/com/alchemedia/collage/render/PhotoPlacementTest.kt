package com.alchemedia.collage.render

import com.alchemedia.collage.model.CellContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
class PhotoPlacementTest {
    @Test fun anUnturnedPhotoCoversTheCellLikeBefore() {
        val pl = CollageRenderer.photoPlacement(0f, 0f, 200f, 100f, CellContent(), 400, 400)
        assertEquals(200f, pl.width, .01f)
        assertEquals(200f, pl.height, .01f)
        assertEquals(100f, pl.cx, .01f)
        assertEquals(50f, pl.cy, .01f)
        assertEquals(0f, pl.slackX, .01f)
        assertEquals(50f, pl.slackY, .01f)
    }

    @Test fun aQuarterTurnSwapsWhichAxisHasRoomToSlide() {
        val pl = CollageRenderer.photoPlacement(0f, 0f, 200f, 100f, CellContent(turns = 1), 400, 400)
        assertEquals(90f, pl.rotation, 0f)
        assertEquals(50f, pl.slackX, .01f)
        assertEquals(0f, pl.slackY, .01f)
    }

    @Test fun aPortraitPhotoTurnedSidewaysStillFillsALandscapeCell() {
        // 100 x 200 photo turned a quarter: its long side must run along the cell's long side.
        val pl = CollageRenderer.photoPlacement(0f, 0f, 300f, 200f, CellContent(turns = 1), 100, 200)
        assertEquals(200f, pl.width, .01f)
        assertEquals(400f, pl.height, .01f)
    }

    /** However the photo is turned, zoomed and slid, all four corners of the cell stay inside the photo. */
    @Test fun aTurnedPhotoNeverLeavesAGapInTheCell() {
        val angles = listOf(0f, 10f, -17f, 30f, 45f, -45f)
        for (turns in 0..3) for (angle in angles) for (zoom in listOf(1f, 2.5f)) for (pan in listOf(-1f, 0f, 1f)) {
            val cell = CellContent(zoom = zoom, panX = pan, panY = -pan, turns = turns, angle = angle)
            val (rx, ry, rw, rh) = listOf(30f, 20f, 260f, 140f)
            val pl = CollageRenderer.photoPlacement(rx, ry, rw, rh, cell, 640, 427)
            val a = Math.toRadians(pl.rotation.toDouble())
            for ((x, y) in listOf(rx to ry, rx + rw to ry, rx to ry + rh, rx + rw to ry + rh)) {
                val dx = x - pl.cx
                val dy = y - pl.cy
                // into the photo's own (turned) frame
                val lx = dx * cos(a) + dy * sin(a)
                val ly = -dx * sin(a) + dy * cos(a)
                val msg = "turns=$turns angle=$angle zoom=$zoom pan=$pan corner=($x,$y) local=($lx,$ly) photo=${pl.width}x${pl.height}"
                assertTrue(msg, abs(lx) <= pl.width / 2 + .05)
                assertTrue(msg, abs(ly) <= pl.height / 2 + .05)
            }
        }
    }

    @Test fun rotationCombinesQuarterTurnsAndStraightening() {
        assertEquals(97f, CellContent(turns = 1, angle = 7f).rotation, 0f)
        assertEquals(263f, CellContent(turns = 3, angle = -7f).rotation, 0f)
        assertEquals(0f, CellContent(turns = 4).rotation, 0f)
    }

    @Test fun resettingKeepsTheLightSettings() {
        val c = CellContent(photoId = "p", zoom = 3f, panX = .4f, turns = 2, angle = 5f, flipH = true, brightness = .3f)
        val r = c.resetPosition()
        assertEquals(CellContent(photoId = "p", brightness = .3f), r)
        assertEquals(CellContent(photoId = "p", zoom = 3f, panX = .4f, turns = 2, angle = 5f, flipH = true), c.resetLook())
    }
}
