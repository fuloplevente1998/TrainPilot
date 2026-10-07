package com.repforge.app;

import android.webkit.WebView;
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
public class StartupPaintTest {
    private static final class FrameWebView extends WebView {
        VisualStateCallback pending;
        boolean detached;
        FrameWebView() { super(RuntimeEnvironment.getApplication()); }
        @Override public void postVisualStateCallback(long id, VisualStateCallback callback) {
            if (detached) throw new IllegalStateException("detached WebView");
            pending = callback;
        }
    }

    @Test public void domReadyDoesNotStartExitUntilTheCompositorFrameIsReady() {
        FrameWebView web = new FrameWebView();
        AtomicInteger exits = new AtomicInteger();
        StartupPaint.await(web, exits::incrementAndGet);
        assertNotNull(web.pending);
        assertEquals("DOM readiness alone must not race the first WebView paint",0,exits.get());
        web.pending.onComplete(0);
        assertEquals(1,exits.get());
        web.pending.onComplete(0);
        assertEquals("Duplicate frame callbacks cannot replay the exit",1,exits.get());
        web.destroy();
    }

    @Test public void destroyedActivityIgnoresALateFrameCallback() {
        FrameWebView web = new FrameWebView();
        AtomicInteger exits = new AtomicInteger();
        StartupPaint wait = StartupPaint.await(web, exits::incrementAndGet);
        wait.cancel();
        web.pending.onComplete(0);
        assertEquals(0,exits.get());
        web.destroy();
    }

    @Test public void unavailableCompositorCannotLeaveAnUnreachableStartupCover() {
        FrameWebView web = new FrameWebView();
        web.detached = true;
        AtomicInteger exits = new AtomicInteger();
        StartupPaint.await(web, exits::incrementAndGet);
        assertEquals(1,exits.get());
        web.destroy();
    }
}
