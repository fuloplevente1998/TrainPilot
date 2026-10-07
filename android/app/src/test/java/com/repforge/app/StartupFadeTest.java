package com.repforge.app;

import android.animation.ValueAnimator;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class StartupFadeTest {
    @Test public void iconAndBackgroundFadeTogetherAndAreRemovedOnce() {
        FrameLayout background = new FrameLayout(RuntimeEnvironment.getApplication());
        View icon = new View(RuntimeEnvironment.getApplication());
        background.addView(icon);
        AtomicInteger removed = new AtomicInteger();
        ValueAnimator fade = StartupFade.start(background, icon, removed::incrementAndGet, 180);
        fade.setCurrentFraction(.5f);
        assertTrue(background.getAlpha() > 0 && background.getAlpha() < 1);
        assertEquals("An independently composited icon must also fade", background.getAlpha(), icon.getAlpha(), .001f);
        assertTrue(icon.getScaleX() > 1 && icon.getScaleX() < 1.08f);
        assertEquals(0, removed.get());
        fade.end();
        assertEquals(0f, icon.getAlpha(), .001f);
        assertEquals(0f, background.getAlpha(), .001f);
        assertEquals(1, removed.get());
        fade.cancel();
        assertEquals(1, removed.get());
    }

    @Test @Config(sdk = 30) public void unattachedSurfaceAndActivityCancellationStillReleaseTheOverlay() {
        View background = new View(RuntimeEnvironment.getApplication());
        SurfaceView icon = new SurfaceView(RuntimeEnvironment.getApplication());
        AtomicInteger removed = new AtomicInteger();
        ValueAnimator fade = StartupFade.start(background, icon, removed::incrementAndGet, 180);
        fade.setCurrentFraction(.5f);
        assertTrue(icon.getAlpha() > 0 && icon.getAlpha() < 1);
        fade.cancel();
        assertEquals(1, removed.get());
        fade.end();
        assertEquals(1, removed.get());
    }
}
