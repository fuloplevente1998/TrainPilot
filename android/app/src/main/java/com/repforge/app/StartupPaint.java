package com.repforge.app;

import android.webkit.WebView;

/** DOM readiness precedes the WebView compositor's first usable frame. */
final class StartupPaint {
    private boolean completed;
    private boolean cancelled;

    static StartupPaint await(WebView webView, Runnable afterPaint) {
        StartupPaint wait = new StartupPaint();
        Runnable finish = () -> {
            if (wait.completed || wait.cancelled) return;
            wait.completed = true;
            afterPaint.run();
        };
        try {
            webView.postVisualStateCallback(0, new WebView.VisualStateCallback() {
                @Override public void onComplete(long requestId) { finish.run(); }
            });
        } catch (RuntimeException unavailable) {
            // Detached/destroying WebView: do not leave an unreachable startup cover.
            finish.run();
        }
        return wait;
    }

    void cancel() { cancelled = true; }
}
