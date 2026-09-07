package com.rcmiku.music.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Comment: ImageVector
    get() {
        if (_Comment != null) {
            return _Comment!!
        }
        _Comment = ImageVector.Builder(
            name = "Comment",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF5F6368))) {
                moveTo(21.99f, 4f)
                curveToRelative(0f, -1.1f, -0.89f, -2f, -1.99f, -2f)
                lineTo(4f, 2f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                lineToRelative(4f, 4f)
                lineTo(21.99f, 4f)
                close()
                moveTo(20f, 4f)
                verticalLineToRelative(13.17f)
                lineTo(18.83f, 16f)
                lineTo(4f, 16f)
                lineTo(4f, 4f)
                horizontalLineToRelative(16f)
                close()
            }
        }.build()

        return _Comment!!
    }

@Suppress("ObjectPropertyName")
private var _Comment: ImageVector? = null
