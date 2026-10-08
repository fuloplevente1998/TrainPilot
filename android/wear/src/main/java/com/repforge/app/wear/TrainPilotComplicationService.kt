package com.repforge.app.wear

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.*

class TrainPilotComplicationService : ComplicationDataSourceService() {
    override fun onComplicationRequest(request: ComplicationRequest, listener: ComplicationRequestListener) {
        val state=WearSurfaces.glance(this)
        listener.onComplicationData(data(request.complicationType,state,true))
    }
    override fun getPreviewData(type: ComplicationType): ComplicationData? = data(type,WearGlance("Alap A","3 rögzített sorozat","2/7","workout"),false)
    private fun data(type: ComplicationType,state: WearGlance,tap: Boolean): ComplicationData? {
        val description=PlainComplicationText.Builder("TrainPilot: ${state.title}, ${state.detail}").build()
        val action=if(tap)WearSurfaces.launch(this)else null
        return when(type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(PlainComplicationText.Builder(state.short.take(7)).build(),description)
                .setTitle(PlainComplicationText.Builder("TP").build()).setTapAction(action).build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(PlainComplicationText.Builder("${state.title} · ${state.detail}").build(),description)
                .setTitle(PlainComplicationText.Builder("TrainPilot").build()).setTapAction(action).build()
            else -> null
        }
    }
}
