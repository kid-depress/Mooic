package com.rcmiku.music.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Download: ImageVector
    get() {
        if (_Download != null) {
            return _Download!!
        }
        _Download = ImageVector.Builder(
            name = "Download",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF1F1F1F))) {
                moveTo(5f, 20f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(-14f)
                close()
                moveTo(13f, 2f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(11.17f)
                lineTo(8.41f, 10.59f)
                lineTo(7f, 12f)
                lineToRelative(5f, 5f)
                lineToRelative(5f, -5f)
                lineToRelative(-1.41f, -1.41f)
                lineTo(13f, 13.17f)
                close()
            }
        }.build()

        return _Download!!
    }

@Suppress("ObjectPropertyName")
private var _Download: ImageVector? = null
