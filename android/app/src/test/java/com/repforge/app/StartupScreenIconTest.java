package com.repforge.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import androidx.core.splashscreen.SplashScreenViewProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class StartupScreenIconTest {
    @Test @Config(sdk = 31)
    public void coldExternalLaunchWithoutSystemIconDoesNotCrashTheProvider() throws Exception {
        Intent request = new Intent(Intent.ACTION_VIEW, Uri.parse("trainpilot://wear/open"))
                .addCategory(Intent.CATEGORY_BROWSABLE);
        var controller = Robolectric.buildActivity(Activity.class, request).setup();
        try {
            Activity activity = controller.get();
            // A solid-colour platform splash has no icon. The old 1.0.1 getter
            // dereferences this null before our exit animation can run.
            Class<?> platformType = Class.forName("android.window.SplashScreenView");
            Object platform = platformType.getConstructor(Context.class).newInstance(activity);
            assertNull(platformType.getMethod("getIconView").invoke(platform));
            var constructor = SplashScreenViewProvider.class.getDeclaredConstructor(platformType, Activity.class);
            constructor.setAccessible(true);
            var provider = (SplashScreenViewProvider) constructor.newInstance(platform, activity);
            View icon = provider.getIconView();
            assertNotNull(icon);
            float[] origin = StartupScreen.loadingOrigin(new View(activity), icon);
            assertTrue(Float.isNaN(origin[0]));
            assertTrue(Float.isNaN(origin[1]));
            assertFalse(activity.isFinishing());
        } finally {
            controller.pause().stop().destroy();
        }
    }

    @Test public void missingOrUnmeasuredIconKeepsOurLogoCentredAfterLayout() {
        View overlay = new View(RuntimeEnvironment.getApplication());
        for (View icon : new View[]{null, new View(RuntimeEnvironment.getApplication())}) {
            float[] origin = StartupScreen.loadingOrigin(overlay, icon);
            assertTrue(Float.isNaN(origin[0]));
            assertTrue(Float.isNaN(origin[1]));
        }
    }

    @Test public void normalLauncherIconPreservesItsScreenPosition() {
        View overlay = positioned(20, 40, 393, 873);
        View icon = positioned(100, 200, 48, 48);
        assertArrayEquals(new float[]{104f, 184f}, StartupScreen.loadingOrigin(overlay, icon), .001f);
    }

    private View positioned(int x, int y, int width, int height) {
        View view = new View(RuntimeEnvironment.getApplication()) {
            @Override public void getLocationOnScreen(int[] position) {
                position[0] = x; position[1] = y;
            }
        };
        view.layout(0, 0, width, height);
        return view;
    }
}
