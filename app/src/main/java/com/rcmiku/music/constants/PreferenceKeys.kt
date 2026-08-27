package com.rcmiku.music.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val ncmCookieKey = stringPreferencesKey("ncmCookie")
val use40DpIconKey = booleanPreferencesKey("use40DpIcon")
val currentPlayMediaIdKey = longPreferencesKey("currentPlayMediaId")
val autoSkipNextOnErrorKey = booleanPreferencesKey("autoSkipNextOnError")
val audioQualityKey = stringPreferencesKey("audioQuality")
val dynamicThemeColorKey = booleanPreferencesKey("dynamicThemeColor")
val themeSeedColorKey = stringPreferencesKey("themeSeedColor")
val userIdKye = longPreferencesKey("userId")
val apiBaseUrlKey = stringPreferencesKey("apiBaseUrl")
val unblockBaseUrlKey = stringPreferencesKey("unblockBaseUrl")
val theme = intPreferencesKey("theme")
val watchMode = booleanPreferencesKey("watch_mode")
