package com.repforge.app;
/** Both the diagnostic trial and background service share one GATT ownership gate. */
final class BleConnectionLease {
 private static Object owner;
 static synchronized boolean acquire(Object candidate){if(owner!=null&&owner!=candidate)return false;owner=candidate;return true;}
 static synchronized void release(Object candidate){if(owner==candidate)owner=null;}
}
