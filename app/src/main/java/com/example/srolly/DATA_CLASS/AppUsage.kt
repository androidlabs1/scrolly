package com.example.srolly.DATA_CLASS

import android.graphics.drawable.Drawable

data class AppUsage(
    val packageName : String,
    val appName : String,
    val icon : Drawable,
    val totalTimeUsed : Long,
    val launchCount : Int
)

