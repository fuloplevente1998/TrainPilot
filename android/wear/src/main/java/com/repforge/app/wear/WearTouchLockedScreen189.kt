package com.repforge.app.wear

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import org.json.JSONObject

@Composable
internal fun WearLockIcon189(color: Color,modifier: Modifier=Modifier) {
    Canvas(modifier) {
        val stroke=Stroke(size.minDimension*.09f)
        drawArc(color,180f,180f,false,Offset(size.width*.3f,size.height*.1f),Size(size.width*.4f,size.height*.5f),style=stroke)
        drawRoundRect(color,Offset(size.width*.2f,size.height*.4f),Size(size.width*.6f,size.height*.5f),androidx.compose.ui.geometry.CornerRadius(size.width*.07f),style=stroke)
    }
}

/** The normal pager, editors and confirmation dialogs are absent while locked. */
@Composable
internal fun WearTouchLockedScreen189(workout: WearWorkout,restRemaining: Int,health: JSONObject?,copy: WearTouchCopy189,onUnlock: ()->Unit,
    nowMillis: ()->Long=SystemClock::uptimeMillis) {
    val gesture=remember(workout.workoutId,workout.started) { WearUnlockGesture189() }
    val unlock by rememberUpdatedState(onUnlock)
    val clock by rememberUpdatedState(nowMillis)
    var now by remember { mutableLongStateOf(clock()) }
    val owner=LocalView.current.findViewTreeLifecycleOwner()
    var foreground by remember(owner) { mutableStateOf(owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED)!=false) }
    DisposableEffect(owner) {
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_PAUSE||event==Lifecycle.Event.ON_STOP){gesture.cancel();foreground=false}
            if(event==Lifecycle.Event.ON_RESUME){gesture.cancel();foreground=true}
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { gesture.cancel();owner?.lifecycle?.removeObserver(observer) }
    }
    LaunchedEffect(foreground,gesture) {
        if(foreground)while(true){
            now=clock()
            if(gesture.tryUnlock(now))unlock()
            delay(if(gesture.holding)32 else 250)
        }
    }
    val progress=gesture.progress(now)
    val hint=copy.hold
    val color=LocalWearHomeTheme.current.accentColor
    val slop=LocalViewConfiguration.current.touchSlop
    val fontScale=LocalDensity.current.fontScale
    val automatic=workout.automation
    val context=LocalContext.current
    val manual=if(automatic?.enabled!=true&&workout.exercise?.repUnit?.startsWith("mp")==true)runCatching {
        val prefix="${workout.workoutId}:${workout.exercise?.id}:${workout.currentSet?.number}"
        WearStopwatchStore.currentField(context,prefix)?.takeIf { it in listOf("reps","leftSeconds","rightSeconds") }
            ?.let { WearStopwatchStore.load(context,"$prefix:$it") }?.takeIf { it.startedAt>0||it.elapsedMillis>0 }
    }.getOrNull() else null
    val status=when {workout.workoutPaused||automatic?.paused==true->copy.paused;restRemaining>0||automatic?.enabled==true&&automatic.phase=="rest"->copy.rest
        automatic?.enabled==true&&automatic.phase=="work"->copy.work;workout.exerciseComplete->copy.saved
        manual?.startedAt?.let { it>0 }==true->copy.work;manual!=null->copy.paused;else->copy.ready}
    val remaining=if(automatic?.enabled==true&&automatic.counting)((automatic.remaining(SystemClock.elapsedRealtime())+999)/1000).toInt() else restRemaining
    val counter=if(remaining>0||automatic?.counting==true)formatSeconds(remaining)
        else if(manual!=null)formatSeconds((manual.elapsed()/1000).toInt())
        else "${workout.currentExercise+1}/${workout.exercises.size} · ${workout.currentSetIndex+1}/${workout.exercise?.sets?.size ?: 0}"
    val rows=wearLiveWorkoutRows(health,workout.workoutId,System.currentTimeMillis())
    Box(Modifier.fillMaxSize().testTag("wear-touch-locked")) {
        FixedRoundPanel(workout.exercise?.name.orEmpty(),count=1,footer={
            Box(Modifier.size(48.dp).background(Color(0xFF232933),CircleShape).testTag("wear-touch-unlock")
                .semantics { role=Role.Button;contentDescription=copy.locked+". "+copy.hold;stateDescription=hint
                    progressBarRangeInfo=ProgressBarRangeInfo(progress,0f..1f) }
                .pointerInput(gesture) {
                    awaitEachGesture {
                        // Input, progress and automatic completion share one monotonic clock.
                        // Pointer timestamps may use a different epoch in injected/platform events.
                        val first=awaitFirstDown(requireUnconsumed=false);first.consume();gesture.down(clock());now=clock()
                        var released=false;var cancelled=false
                        try {
                            while(true){
                                val event=awaitPointerEvent();val current=event.changes.firstOrNull { it.id==first.id }
                                if(event.changes.size!=1||current==null||(current.position-first.position).getDistance()>slop||current.position.x !in 0f..size.width.toFloat()||current.position.y !in 0f..size.height.toFloat()){
                                    cancelled=true;gesture.cancel()
                                }
                                event.changes.forEach { it.consume() }
                                if(current==null||!current.pressed){
                                    val unlocked=!cancelled&&current!=null&&gesture.up(clock())
                                    released=true;now=clock();if(unlocked)unlock();break
                                }
                            }
                        }finally{if(!released)gesture.cancel()}
                    }
                },contentAlignment=Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(2.dp)) { drawArc(color,-90f,360f*progress,false,style=Stroke(3.dp.toPx())) }
                WearLockIcon189(color,Modifier.size(23.dp))
            }
        }) {
            val scale=fontScale.coerceAtMost(if(it)1f else 1.2f)/fontScale
            Text(copy.locked+" · "+(if(automatic?.side192?.isNotBlank()==true)WearSideCopy192.language(java.util.Locale.getDefault().language).side(automatic.side192)+" · "+status else status),color=color,fontSize=(8*scale).sp,lineHeight=(10*scale).sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(counter,color=Color(0xFFF6F8FB),fontSize=(18*scale).sp,lineHeight=(20*scale).sp,fontWeight=FontWeight.SemiBold,
                modifier=Modifier.testTag("wear-locked-clock"),maxLines=1)
            Text(rows.firstOrNull() ?: "—",color=color,fontSize=(8*scale).sp,lineHeight=(10*scale).sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(hint,color=Color(0xFF9AA5B6),fontSize=(8*scale).sp,lineHeight=(10*scale).sp,textAlign=TextAlign.Center,maxLines=2,
                modifier=Modifier.testTag("wear-touch-hint"))
        }
    }
}
