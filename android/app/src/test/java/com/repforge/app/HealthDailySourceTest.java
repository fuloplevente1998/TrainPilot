package com.repforge.app;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class HealthDailySourceTest {
 @Test public void autoPrefersOnlyAnAvailableSamsungStepsOrigin(){assertEquals(HealthDailySource.SAMSUNG,HealthDailySource.choose("","auto",new HashSet<>(Arrays.asList("phone",HealthDailySource.SAMSUNG))));assertEquals("",HealthDailySource.choose("","auto",Collections.singleton("phone")));}
 @Test public void explicitSourceNeverFallsBackOrAddsAnotherOrigin(){assertEquals("phone",HealthDailySource.choose("phone","auto",Collections.singleton(HealthDailySource.SAMSUNG)));assertEquals("missing",HealthDailySource.choose("","missing",Collections.singleton("phone")));assertEquals("",HealthDailySource.choose("","priority",Collections.singleton(HealthDailySource.SAMSUNG)));}
 @Test public void diagnosticAndBackgroundOwnersCannotOverlap(){Object service=new Object(),trial=new Object();try{assertTrue(BleConnectionLease.acquire(service));assertFalse(BleConnectionLease.acquire(trial));BleConnectionLease.release(trial);assertFalse(BleConnectionLease.acquire(trial));BleConnectionLease.release(service);assertTrue(BleConnectionLease.acquire(trial));assertFalse(BleConnectionLease.acquire(service));}finally{BleConnectionLease.release(service);BleConnectionLease.release(trial);}}
 @Test public void reconnectDelayIsBoundedAndDoesNotSpinWhenWatchIsMissing(){assertEquals(15000,BleReconnectPolicy.delay(0));assertEquals(30000,BleReconnectPolicy.delay(1));assertEquals(60000,BleReconnectPolicy.delay(2));assertEquals(300000,BleReconnectPolicy.delay(Integer.MAX_VALUE));}
}
