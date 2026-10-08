package com.repforge.app.wear

import androidx.wear.tiles.TileService
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.ResourceBuilders
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class TrainPilotTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val glance=WearSurfaces.glance(this)
        val activity=ActionBuilders.AndroidActivity.Builder().setPackageName(packageName).setClassName(MainActivity::class.java.name)
            .addKeyToExtraMapping("destination",ActionBuilders.AndroidStringExtra.Builder().setValue(glance.destination).build()).build()
        val clickable=ModifiersBuilders.Clickable.Builder().setId("trainpilot-open").setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(activity).build()).build()
        val content=LayoutElementBuilders.Column.Builder().setWidth(DimensionBuilders.expand()).setHeight(DimensionBuilders.wrap())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(clickable)
                .setBackground(ModifiersBuilders.Background.Builder().setColor(ColorBuilders.argb(0xFF000000.toInt())).build())
                .setPadding(ModifiersBuilders.Padding.Builder().setAll(DimensionBuilders.dp(24f)).build()).build())
            .addContent(text("TRAINPILOT",11f,0xFFF2BD45.toInt()))
            .addContent(space(8f)).addContent(text(glance.title,18f,0xFFF6F8FB.toInt()))
            .addContent(space(6f)).addContent(text(glance.detail,12f,0xFF9AA5B6.toInt()))
            .addContent(space(10f)).addContent(text(if(glance.destination=="workout")"▶ Folytatás" else "▶ Megnyitás",14f,0xFFF2BD45.toInt())).build()
        val root=LayoutElementBuilders.Box.Builder().setWidth(DimensionBuilders.expand()).setHeight(DimensionBuilders.expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER).setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER).addContent(content).build()
        val layout=LayoutElementBuilders.Layout.Builder().setRoot(root).build()
        val timeline=TimelineBuilders.Timeline.Builder().addTimelineEntry(TimelineBuilders.TimelineEntry.Builder().setLayout(layout).build()).build()
        return Futures.immediateFuture(TileBuilders.Tile.Builder().setResourcesVersion("1").setTileTimeline(timeline).setFreshnessIntervalMillis(30000).build())
    }
    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(ResourceBuilders.Resources.Builder().setVersion("1").build())
    private fun text(value: String,size: Float,color: Int) = LayoutElementBuilders.Text.Builder().setText(value).setMaxLines(2)
        .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
        .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setSize(DimensionBuilders.sp(size)).setColor(ColorBuilders.argb(color)).build()).build()
    private fun space(height: Float) = LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(height)).build()
}
