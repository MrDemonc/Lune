package com.demonlab.lune.tools

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset

object ImageAnalyzer {
    fun findFocalPoint(bitmap: Bitmap): Offset {
        val width = bitmap.width
        val height = bitmap.height
        val gridCount = 4
        val cellWidth = width / gridCount
        val cellHeight = height / gridCount
        
        var maxContrast = -1f
        var bestCellX = gridCount / 2
        var bestCellY = gridCount / 2
        
        for (gy in 0 until gridCount) {
            for (gx in 0 until gridCount) {
                val contrast = calculateVariance(bitmap, gx * cellWidth, gy * cellHeight, cellWidth, cellHeight)
                if (contrast > maxContrast) {
                    maxContrast = contrast
                    bestCellX = gx
                    bestCellY = gy
                }
            }
        }
        
        return Offset(
            (bestCellX + 0.5f) / gridCount,
            (bestCellY + 0.5f) / gridCount
        )
    }

    private fun calculateVariance(bitmap: Bitmap, x: Int, y: Int, w: Int, h: Int): Float {
        var sum = 0L
        var sumSq = 0L
        val n = w * h
        
        if (n == 0) return 0f

        for (i in x until (x + w).coerceAtMost(bitmap.width)) {
            for (j in y until (y + h).coerceAtMost(bitmap.height)) {
                val pixel = bitmap.getPixel(i, j)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val gray = (r + g + b) / 3
                sum += gray
                sumSq += (gray * gray).toLong()
            }
        }
        
        return (sumSq.toFloat() / n) - (sum.toFloat() / n) * (sum.toFloat() / n)
    }
}
