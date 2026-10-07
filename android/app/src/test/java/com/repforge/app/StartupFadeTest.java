package com.repforge.app;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class StartupFadeTest {
    private StartupFade.LogoView overlay(int width, int height) {
        StartupFade.LogoView view = new StartupFade.LogoView(RuntimeEnvironment.getApplication(),
                new ColorDrawable(0xffffcc44), 288);
        view.layout(0, 0, width, height);
        return view;
    }

    @Test public void loadingKeepsGrowingEvenWhenTheWebViewTakesLonger() {
        assertEquals(.55f, StartupFade.loadingScale(0), .0001f);
        float previous = 0;
        for (long time : new long[]{0, 100, 500, 1000, 2000, 4000, 8000}) {
            float scale = StartupFade.loadingScale(time);
            assertTrue("No static hold or loop while waiting for local readiness", scale > previous);
            previous = scale;
        }
    }

    @Test public void readyZoomContinuesFromLiveScaleAndFillsPortraitAndLandscape() {
        for (int[] size : new int[][]{{393,873},{320,740},{873,393}}) {
            StartupFade.LogoView view = overlay(size[0], size[1]);
            float from = view.scale = StartupFade.loadingScale(2000);
            AtomicInteger removed = new AtomicInteger();
            ValueAnimator zoom = StartupFade.start(view, removed::incrementAndGet, 480);
            assertEquals("No shrink at the loading/ready boundary", from, view.scale, .001f);
            zoom.setCurrentFraction(.3f);
            assertTrue(view.scale > from);
            assertEquals("Stay visible during the first part of the zoom", 1f, view.opacity, .001f);
            float middle = view.scale;
            zoom.setCurrentFraction(.7f);
            assertTrue(view.scale > middle);
            assertTrue(view.opacity > 0 && view.opacity < 1);
            assertEquals("No full-window alpha layer during fade", 1f, view.getAlpha(), .001f);
            assertEquals(0, removed.get());
            zoom.end();
            assertTrue("The visible card must exceed the entire screen", view.scale * 288f * .5f > Math.max(size[0],size[1]));
            assertEquals(0f, view.opacity, .001f);
            assertEquals(1, removed.get());
            zoom.cancel();
            assertEquals(1, removed.get());
        }
    }

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void directFadeHasUniformAlphaWithoutAnIntermediateViewLayer() {
        StartupFade.LogoView view = overlay(393,873);
        view.opacity = .4f;
        Bitmap frame = Bitmap.createBitmap(393,873,Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(frame));
        assertEquals("The native View must remain opaque to avoid an offscreen alpha layer", 1f, view.getAlpha(), .001f);
        assertFalse(view.hasOverlappingRendering());
        assertEquals(102, Color.alpha(frame.getPixel(5,5)));
        assertEquals("Logo pixels must not receive a second background alpha", 102, Color.alpha(frame.getPixel(196,436)));
        for (int x : new int[]{51,52,53,339,340,341}) {
            assertEquals("No doubled alpha or gap at the sprite/background edge",102,Color.alpha(frame.getPixel(x,436)));
        }
        view.opacity = 0f;
        frame.eraseColor(Color.TRANSPARENT);
        view.draw(new Canvas(frame));
        assertEquals(0,Color.alpha(frame.getPixel(5,5)));
        assertEquals(0,Color.alpha(frame.getPixel(196,436)));
    }

    @Test public void vectorIsRasterizedOnceInsteadOfOncePerZoomFrame() {
        AtomicInteger draws = new AtomicInteger();
        ColorDrawable drawable = new ColorDrawable(0xffffcc44) {
            @Override public void draw(Canvas canvas) {
                draws.incrementAndGet();
                super.draw(canvas);
            }
        };
        StartupFade.LogoView view = new StartupFade.LogoView(RuntimeEnvironment.getApplication(), drawable, 288);
        view.layout(0,0,393,873);
        view.beginLoading(196.5f,436.5f);
        Canvas canvas = new Canvas(Bitmap.createBitmap(393,873,Bitmap.Config.ARGB_8888));
        view.draw(canvas);
        view.scale = 5f;
        view.draw(canvas);
        assertEquals("The animation should only rescale one cached texture", 1, draws.get());
        view.stop();
        view.draw(canvas);
        assertEquals(1, draws.get());
    }

    @Test @Config(sdk = 30) public void activityCancellationRemovesTheLayerExactlyOnce() {
        StartupFade.LogoView view = overlay(393,873);
        view.beginLoading(196.5f,436.5f);
        AtomicInteger removed = new AtomicInteger();
        ValueAnimator zoom = StartupFade.start(view, removed::incrementAndGet, 480);
        zoom.setCurrentFraction(.5f);
        zoom.cancel();
        assertEquals(1, removed.get());
        zoom.end();
        assertEquals(1, removed.get());
    }
}
