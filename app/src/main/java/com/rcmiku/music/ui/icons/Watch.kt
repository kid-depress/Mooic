package com.rcmiku.music.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Watch: ImageVector
    get() {
        if (_Watch != null) {
            return _Watch!!
        }
        _Watch = ImageVector.Builder(
            name = "Watch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFF1F1F1F))) {
                moveTo(360f, 840f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(320f, 800f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(-40f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(200f, 640f)
                verticalLineToRelative(-320f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(280f, 240f)
                horizontalLineToRelative(40f)
                verticalLineToRelative(-80f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(360f, 120f)
                horizontalLineToRelative(240f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(640f, 160f)
                verticalLineToRelative(80f)
                horizontalLineToRelative(40f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(760f, 320f)
                verticalLineToRelative(320f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(680f, 720f)
                horizontalLineToRelative(-40f)
                verticalLineToRelative(80f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(600f, 840f)
                lineTo(360f, 840f)
                close()
                moveTo(280f, 640f)
                horizontalLineToRelative(400f)
                verticalLineToRelative(-320f)
                lineTo(280f, 320f)
                verticalLineToRelative(320f)
                close()
                moveTo(480f, 600f)
                quadToRelative(-8f, 0f, -14f, -6f)
                reflectiveQuadToRelative(-6f, -14f)
                verticalLineToRelative(-100f)
                lineToRelative(-70f, -70f)
                quadToRelative(-6f, -6f, -6f, -14f)
                reflectiveQuadToRelative(6f, -14f)
                quadToRelative(6f, -6f, 14f, -6f)
                reflectiveQuadToRelative(14f, 6f)
                lineToRelative(80f, 80f)
                verticalLineToRelative(118f)
                quadToRelative(0f, 8f, -6f, 14f)
                reflectiveQuadToRelative(-14f, 6f)
                close()
            }
        }.build()

        return _Watch!!
    }

@Suppress("ObjectPropertyName")
private var _Watch: ImageVector? = null