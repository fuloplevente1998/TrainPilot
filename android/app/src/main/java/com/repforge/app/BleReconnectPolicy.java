package com.repforge.app;
final class BleReconnectPolicy {
 static long delay(int failure){return Math.min(300000L,15000L<<Math.max(0,Math.min(failure,5)));}
}
