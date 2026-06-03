package com.anthonyla.paperize.feature.wallpaper.glance_widget
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class AutoWallpaperChangerProWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AutoWallpaperChangerProWidget()
}