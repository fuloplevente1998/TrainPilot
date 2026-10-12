package com.repforge.app.wear

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.*
import androidx.wear.compose.material3.Text
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay
import org.json.JSONObject

private val HomeWhite = Color(0xFFF6F8FB)
private val HomeMuted = Color(0xFF9AA5B6)
private val HomeLine = Color(0xFF2C3440)

@Composable
internal fun WearHomeDashboard(
    home: WatchHomeSnapshot?,
    active: WearWorkout?,
    closure: WearClosure?,
    health: JSONObject?,
    enabled: Boolean,
    phoneConnected: Boolean?,
    battery: Int?,
    onStart: (WatchHomeDay, String) -> Unit,
    onResume: () -> Unit,
    onDays: () -> Unit,
    onResult: () -> Unit,
    onJournal: () -> Unit,
    onDiscard: () -> Unit,
    onSettings: () -> Unit,
    onMenu: () -> Unit,
    onOpenPhone: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active?.workoutId) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L - now % 1000L)
        }
    }
    val model = wearHomeDashboardState(home, active, closure, health, enabled, now)
    val theme = home?.theme ?: WearHomeTheme.DEFAULT
    val onPrimary = {
        when (model.action) {
            HomeAction.START -> home?.recommended?.let { onStart(it.day, it.scheduleId) }
            HomeAction.RESUME -> onResume()
            HomeAction.DAYS -> onDays()
            HomeAction.RESULT -> onResult()
            HomeAction.PHONE -> onOpenPhone()
        }
        Unit
    }
    val primaryDescription =
        when (model.action) {
            HomeAction.START -> "Edzés indítása"
            HomeAction.RESUME -> "Edzés folytatása"
            HomeAction.DAYS -> "Program napjai"
            HomeAction.RESULT -> "Edzés mentésének állapota"
            HomeAction.PHONE -> "Telefon megnyitása a folytatáshoz"
        }
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(Color(0xFF0E1015))
            .testTag("wear-round-list")
            .pointerInput(onMenu) {
                var distance = 0f
                var fromBottom = false
                detectVerticalDragGestures(
                    onDragStart = {
                        distance = 0f
                        fromBottom = it.y >= size.height * .6f
                    },
                    onVerticalDrag = { change, amount ->
                        distance += amount
                        if (fromBottom) change.consume()
                    },
                    onDragEnd = { if (fromBottom && distance <= -40.dp.toPx()) onMenu() },
                )
            }
    ) {
        val diameter = minOf(maxWidth, maxHeight)
        val origin = (maxHeight - diameter) / 2
        val tiny = diameter < 180.dp
        val large = diameter >= 210.dp
        val arc = wearArcLayout(maxWidth.value, maxHeight.value)
        val base = wearFixedPageLayout(maxWidth.value, maxHeight.value, true)
        val footerTop = base.footerTop - 16f
        val bodyTop = origin + if (tiny) 24.dp else 32.dp
        val bodyHeight = (footerTop.dp - bodyTop - 4.dp).coerceAtLeast(1.dp)
        val connectedLabel =
            when (phoneConnected) {
                true -> "Telefon elérhető"
                false -> "Telefon nincs kapcsolatban"
                null -> "Kapcsolat ellenőrzése"
            }
        val connectionColor =
            when (phoneConnected) {
                true -> Color(0xFF62D08A)
                false -> HomeMuted
                null -> theme.secondaryColor
            }
        val curveSize = diameter - 12.dp
        val fontScale = LocalDensity.current.fontScale
        val sideLabel =
            if (large) model.measurement
            else
                when (model.measurement) {
                    "Mérés folyamatban" -> "Mérés aktív"
                    "Mérés bekapcsolva" -> "Mérés be"
                    "Mérés kikapcsolva" -> "Mérés ki"
                    "Legutóbbi mérés" -> "Korábbi adat"
                    "Mérés lezárása" -> "Lezárás"
                    "Mérés lezárva" -> "Lezárva"
                    else -> model.measurement
                }
        CurvedLayout(Modifier.align(Alignment.Center).size(curveSize)) {
            curvedComposable {
                Image(
                    painterResource(R.drawable.ic_tp_phone),
                    null,
                    Modifier.size(11.dp),
                    colorFilter = ColorFilter.tint(HomeWhite),
                )
            }
            basicCurvedText(
                "  ● ",
                style = CurvedTextStyle(color = connectionColor, fontSize = (9f / fontScale).sp),
            )
            basicCurvedText(
                when (phoneConnected) {
                    true -> "Telefon ↗"
                    false -> "Telefon ↗ · nincs"
                    null -> "Telefon ↗ · …"
                },
                style = CurvedTextStyle(color = HomeWhite, fontSize = (10f / fontScale).sp),
            )
        }
        Box(
            Modifier.align(Alignment.TopCenter)
                .offset(y = origin)
                .width(diameter * .65f)
                .height(if (tiny) 24.dp else 32.dp)
                .clickable(role = Role.Button, onClick = onOpenPhone)
                .semantics {
                    contentDescription = "Megnyitás telefonon"
                    stateDescription = "$connectedLabel · ${model.source}"
                }
        )
        Canvas(Modifier.align(Alignment.Center).size(diameter)) {
            val inset = 7.dp.toPx()
            for (i in 0 until 12) drawArc(
                if (battery != null && i < battery.coerceIn(0, 100) * 12 / 100f) theme.accentColor
                else HomeLine,
                145f + i * 6f,
                3.5f,
                false,
                Offset(inset, inset),
                androidx.compose.ui.geometry.Size(size.width - 2 * inset, size.height - 2 * inset),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        CurvedLayout(
            Modifier.align(Alignment.Center).size(diameter - 30.dp),
            anchor = 146f,
            angularDirection = CurvedDirection.Angular.Reversed,
        ) {
            basicCurvedText(
                battery?.let { "$it%" } ?: "—",
                style = CurvedTextStyle(color = HomeWhite, fontSize = (8f / fontScale).sp),
            )
        }
        CurvedLayout(
            Modifier.align(Alignment.Center).size(diameter - 18.dp),
            anchor = if (large) 0f else 335f,
        ) {
            curvedComposable {
                Image(
                    painterResource(R.drawable.ic_tp_heart),
                    null,
                    Modifier.size(if (tiny) 7.dp else 9.dp),
                    colorFilter = ColorFilter.tint(theme.secondaryColor),
                )
            }
            basicCurvedText(
                " $sideLabel",
                modifier = CurvedModifier.sizeIn(maxSweepDegrees = if (large) 72f else 60f),
                style =
                    CurvedTextStyle(
                        color = theme.secondaryColor,
                        fontSize = ((if (tiny) 6.5f else 8f) / fontScale).sp,
                    ),
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            Modifier.align(Alignment.TopCenter)
                .offset(y = bodyTop)
                .width(diameter - 50.dp)
                .height(bodyHeight)
                .testTag("wear-fixed-body"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (tiny) 0.dp else 1.dp),
        ) {
            if (large)
                HomeFitText(
                    if (model.active) "${model.phase} · ${model.detail}"
                    else {
                        val date = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
                        "${listOf("HÉT","KEDD","SZE","CSÜT","PÉN","SZO","VAS")[date.dayOfWeek.value-1]} ${date.dayOfMonth} · ${model.phase}"
                    },
                    Modifier.fillMaxWidth().height(9.dp),
                    7.5f,
                    theme.accentColor,
                )
            HomeFitText(
                model.clock,
                Modifier.fillMaxWidth().weight(1f).testTag("wear-home-clock"),
                if (large) 32f else if (tiny) 16f else 24f,
                HomeWhite,
                weight = FontWeight.SemiBold,
                mono = true,
            )
            HomeFitText(
                if (model.phase == "PIHENŐ" && !large) "Pihenő · ${model.title}" else model.title,
                Modifier.fillMaxWidth()
                    .height(
                        if (model.active) {
                            if (large) 32.dp else if (tiny) 16.dp else 24.dp
                        } else if (large) 16.dp else if (tiny) 8.dp else 12.dp
                    )
                    .testTag("wear-home-title"),
                if (model.active) {
                    if (large) 17f else if (tiny) 10f else 13f
                } else if (large) 14f else if (tiny) 7f else 10.5f,
                HomeWhite,
                weight = FontWeight.SemiBold,
                maxLines = if (model.active) 2 else 1,
            )
            if (!tiny) {
                if (model.active) {
                    if (large)
                        HomeProgress(
                            model.completed,
                            model.total,
                            theme,
                            Modifier.fillMaxWidth().height(3.dp),
                        )
                } else {
                    HomeFitText(
                        model.subtitle,
                        Modifier.fillMaxWidth().height(10.dp),
                        8f,
                        HomeMuted,
                    )
                    if (large)
                        HomeFitText(
                            "${model.detail} · ${model.source}",
                            Modifier.fillMaxWidth().height(9.dp),
                            7.5f,
                            HomeMuted,
                        )
                }
            }
            if (model.active) {
                val rows = model.metrics.take(if (large) 2 else 1)
                if (rows.isEmpty())
                    HomeFitText(
                        model.measurement,
                        Modifier.fillMaxWidth().height(if (tiny) 8.dp else 10.dp),
                        if (tiny) 6.5f else 8f,
                        theme.secondaryColor,
                    )
                rows.forEach {
                    HomeFitText(
                        it,
                        Modifier.fillMaxWidth().height(if (tiny) 8.dp else 10.dp),
                        if (tiny) 6.5f else 8f,
                        theme.secondaryColor,
                    )
                }
            } else if (large) {
                if (model.total > 0)
                    HomeProgress(0, model.total, theme, Modifier.fillMaxWidth().height(3.dp))
            }
        }
        // Same circular targets as the accepted short-page layout; the menu has its own rim strip.
        Box(
            Modifier.align(Alignment.TopCenter)
                .offset(y = footerTop.dp)
                .width(arc.width.dp)
                .height(arc.height.dp)
                .testTag("wear-fixed-actions")
        ) {
            HomeQuickButton(
                if (model.active) R.drawable.ic_tp_delete else R.drawable.ic_tp_journal,
                if (model.active) "Törlés" else "Napló",
                if (model.active) "Megkezdett edzés törlése" else "Edzésnapló",
                theme,
                false,
                if (model.active) onDiscard else onJournal,
                danger = model.active,
            )
            Box(Modifier.offset(x = arc.sideOffset.dp, y = arc.centerTop.dp)) {
                HomeQuickButton(
                    if (model.action == HomeAction.PHONE) R.drawable.ic_tp_phone
                    else R.drawable.ic_tp_workout,
                    model.actionLabel,
                    primaryDescription,
                    theme,
                    true,
                    onPrimary,
                )
            }
            Box(Modifier.offset(x = (2 * arc.sideOffset).dp)) {
                HomeQuickButton(
                    R.drawable.ic_tp_settings,
                    "Beállít.",
                    "Beállítások",
                    theme,
                    false,
                    onSettings,
                )
            }
        }
        CurvedLayout(
            Modifier.align(Alignment.Center).size(diameter - 12.dp),
            anchor = 90f,
            angularDirection = CurvedDirection.Angular.Reversed,
        ) {
            basicCurvedText(
                "··· Menü",
                style = CurvedTextStyle(color = HomeWhite, fontSize = (10f / fontScale).sp),
            )
        }
        Box(
            Modifier.align(Alignment.BottomCenter)
                .offset(y = -origin)
                .width(diameter * .6f)
                .height(24.dp)
                .clickable(role = Role.Button, onClick = onMenu)
                .semantics { contentDescription = "Főmenü" }
        )
    }
}

@Composable
private fun HomeQuickButton(
    icon: Int,
    caption: String,
    description: String,
    theme: WearHomeTheme,
    primary: Boolean,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    Column(
        Modifier.size(48.dp)
            .background(
                if (danger) Color(0xFF3B232A) else if (primary) theme.button else Color(0xFF232933),
                CircleShape,
            )
            .border(
                1.dp,
                if (danger) Color(0xFF7D4653) else if (primary) theme.frame else HomeLine,
                CircleShape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painterResource(icon),
            null,
            Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(
                if (danger) Color(0xFFEFBCC3) else if (primary) theme.secondaryColor else HomeWhite
            ),
        )
        HomeFitText(caption, Modifier.width(44.dp).height(13.dp), 8.5f, HomeWhite)
    }
}

@Composable
private fun HomeProgress(completed: Int, total: Int, theme: WearHomeTheme, modifier: Modifier) {
    Canvas(modifier) {
        val count = total.coerceIn(1, 12)
        val gap = 2.dp.toPx()
        val width = (size.width - (count - 1) * gap) / count
        val fraction = completed.toFloat() / total.coerceAtLeast(1)
        repeat(count) { i ->
            val left = i * (width + gap)
            drawRoundRect(
                HomeLine,
                Offset(left, 0f),
                androidx.compose.ui.geometry.Size(width, size.height),
                androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            )
            val amount = (fraction * count - i).coerceIn(0f, 1f)
            if (amount > 0)
                drawRoundRect(
                    theme.accentColor,
                    Offset(left, 0f),
                    androidx.compose.ui.geometry.Size(width * amount, size.height),
                    androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                )
        }
    }
}

/** Fit actual measured text into a bounded slot, retaining full accessibility text. */
@Composable
private fun HomeFitText(
    text: String,
    modifier: Modifier,
    requested: Float,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    mono: Boolean = false,
    maxLines: Int = 1,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val font = if (mono) FontFamily.Monospace else FontFamily.SansSerif
        val fitted =
            remember(
                text,
                constraints.maxWidth,
                constraints.maxHeight,
                density,
                requested,
                weight,
                font,
                maxLines,
            ) {
                var size = requested * density.fontScale.coerceAtMost(1.1f)
                repeat(8) {
                    val result =
                        measurer.measure(
                            text,
                            TextStyle(
                                fontSize = (size / density.fontScale).sp,
                                lineHeight = (size * 1.12f / density.fontScale).sp,
                                fontWeight = weight,
                                fontFamily = font,
                            ),
                            constraints = Constraints(maxWidth = constraints.maxWidth),
                            maxLines = maxLines,
                            softWrap = maxLines > 1,
                        )
                    if (result.didOverflowWidth || result.size.height > constraints.maxHeight)
                        size *= .88f
                }
                size.coerceAtLeast(5.5f)
            }
        Text(
            text,
            color = color,
            fontSize = (fitted / density.fontScale).sp,
            lineHeight = (fitted * 1.12f / density.fontScale).sp,
            fontFamily = font,
            fontWeight = weight,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            softWrap = maxLines > 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
