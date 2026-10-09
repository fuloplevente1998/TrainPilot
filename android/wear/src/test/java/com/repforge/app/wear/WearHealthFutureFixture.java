package com.repforge.app.wear;

import com.google.common.util.concurrent.SettableFuture;

/** Java permits the null completion that Health Services' Void commands actually return. */
public final class WearHealthFutureFixture {
    private WearHealthFutureFixture() {}

    public static void completeWithNull(SettableFuture<?> future) {
        future.set(null);
    }
}
