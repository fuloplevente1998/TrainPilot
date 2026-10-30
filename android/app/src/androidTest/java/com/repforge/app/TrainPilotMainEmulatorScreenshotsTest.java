package com.repforge.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.webkit.WebView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * One-off evidence capture. Android's UiAutomation takes screenshots of the actual
 * installed Capacitor app in a phone emulator. This test is NOT a Samsung SDK test.
 * No private/user health records are used.
 */
@RunWith(AndroidJUnit4.class)
public class TrainPilotMainEmulatorScreenshotsTest {
    private final StringBuilder index = new StringBuilder("TrainPilot Android emulator screenshots\nSource main: 5f026a56b9b6d33c84afa77eb3927b11f0bd6f6a\nDevice override: 1080x2392, 450 dpi, Android 15/API 35\nData: emulator-local only, no personal health records\n\n");
    private File dir;
    private int captured = 0;

    private String eval(WebView web, String js) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> answer = new AtomicReference<>("");
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
            web.evaluateJavascript(js, value -> { answer.set(value); latch.countDown(); }));
        assertTrue("evaluateJavascript timeout", latch.await(15, TimeUnit.SECONDS));
        return answer.get();
    }

    private void capture(WebView web, String name, String code) throws Exception {
        final String wrapped = "(function(){try{"
          + "if(window.tp155R4ClosePanel)window.tp155R4ClosePanel(false);"
          + "if(window.tp3ClosePlannerPanel)window.tp3ClosePlannerPanel(false);"
          + "window.scrollTo(0,0);"
          + code
          + ";return 'OK';}catch(error){return 'ERR: '+String(error&&error.stack||error);}})()";
        String outcome = eval(web, wrapped);
        Thread.sleep(1800);
        // SwiftShader/SurfaceFlinger can show the previous frame on the first read.
        Bitmap priming = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        if(priming != null) priming.recycle();
        Thread.sleep(500);
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        if(bitmap == null) throw new AssertionError("Android emulator screenshot returned null: " + name);
        File image = new File(dir, name + ".png");
        try(FileOutputStream stream = new FileOutputStream(image)) {
            assertTrue("PNG encoding failed", bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        } finally {
            bitmap.recycle();
        }
        captured++;
        index.append(name).append(".png | ").append(outcome).append("\n");
    }

    @Test public void captureEveryAvailablePhoneView() throws Exception {
        Intent start = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), MainActivity.class)
           .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity activity = (MainActivity) InstrumentationRegistry.getInstrumentation().startActivitySync(start);
        AtomicReference<WebView> ref = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> ref.set(activity.getBridge().getWebView()));
        WebView web = ref.get();
        assertNotNull(web);
        long end = System.currentTimeMillis() + 55000;
        while(System.currentTimeMillis()<end) {
            if("true".equals(eval(web, "Boolean(window.TrainPilotBoot?.finished)"))) break;
            Thread.sleep(250);
        }
        assertEquals("App did not complete startup", "true", eval(web, "Boolean(window.TrainPilotBoot?.finished)"));
        dir = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null), "main-emulator-5f026a5");
        assertTrue(dir.exists() || dir.mkdirs());

        // New install state and main pages. All screens are recorded at *native*
        // emulator resolution with Android status/navigation bars intact.
        capture(web,"00-startup-first-run","state.tab='home';render()");
        eval(web,"(function(){try{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));db.set('language','hu');const sample=byId('db-floor-press');if(sample){const hs=[];for(let i=0;i<4;i++){const day=12+i*3;hs.push({id:'emulator-fixture-'+i,programId:'home-basic',programName:'Bemutató edzés',dayId:'A',workout:'A',started:'2026-10-'+day+'T10:00:00Z',finished:'2026-10-'+day+'T10:42:00Z',exercises:[{...sample,sets:[{set:1,setId:'fixture-'+i+'-1',reps:'8',weight:20+i*2,done:true},{set:2,setId:'fixture-'+i+'-2',reps:'8',weight:20+i*2,done:true}]}]});}db.set('history',hs);}state.session=null;state.workout=null;state.tab='home';render();return true}catch(e){return String(e)}})()");
        capture(web,"01-kezdolap","go('home')");
        capture(web,"02-edzes","go('plan')");
        capture(web,"03-naptar","go('calendar')");
        capture(web,"04-programok","go('programs')");
        capture(web,"05-naplo","go('history')");
        capture(web,"06-egeszseg","go('health')");
        capture(web,"07-fejlodes","go('progress')");
        capture(web,"08-beallitasok","go('settings')");

        // Exercise and program setup, including navigation drawers.
        capture(web,"09-gyors-edzes","go('plan');tp155R4OpenPanel('quick')");
        capture(web,"10-gyakorlat-konyvtar","go('plan');tp155R4OpenPanel('exercises')");
        capture(web,"11-sajat-gyakorlat","go('plan');customExerciseScreen()");
        capture(web,"12-sajat-program","go('programs');createCustomProgram()");
        capture(web,"13-edzesi-celok","go('home');tp155R4OpenPanel('goals')");
        capture(web,"14-naptar-ujratervezes","go('home');tp155R4OpenPanel('replan')");
        capture(web,"15-coach","go('home');rf233CoachScreen()");

        // Real app screens, external providers remain disconnected in this fixture.
        capture(web,"16-health-connect-attekintes","go('health')");
        capture(web,"17-health-connect-reszletek","go('health');healthScreen()");
        capture(web,"18-egeszsegnaplo","go('health');tp155R4OpenPanel('health-journal')");
        capture(web,"19-testsuly-naplo","go('health');rf215WeightScreen()");
        capture(web,"20-profil","go('home');profileScreen()");
        capture(web,"21-statisztika","go('history');statsScreen()");

        // Training configuration and exercise cards.
        capture(web,"22-programnap","go('plan');document.querySelector('.tp152-day > summary')?.click()");
        capture(web,"23-program-reszletek","go('programs');document.querySelector('[data-tp7-program-id] > summary')?.click()");
        capture(web,"24-edzes-beallitasok","go('plan');document.querySelector('.tp1511-training > summary')?.click()");
        capture(web,"25-naplo-statisztikak","go('history');if(typeof tp155OpenJournalStats==='function')tp155OpenJournalStats()");
        capture(web,"26-egeszseg-meresi-reszletek","go('health');document.querySelector('.tp168-sync-tail summary')?.click()");

        // Capture provider and settings states before opening an active workout.
        capture(web,"32-health-connect-kapcsolat","go('health');const p=document.querySelector('.tp168-connect-panel');if(p)p.open=true;window.scrollTo(0,180)");
        capture(web,"33-samsung-health-adatforras","go('health');const p=document.querySelector('.tp168-connect-panel');if(p)p.open=true;const provider=document.querySelector('[data-health-provider]');if(provider)provider.scrollIntoView({block:'center'})");
        capture(web,"34-samsung-health-megjelenites","go('health');if(window.TrainPilotSamsungHealth)TrainPilotSamsungHealth.decorate();const a=document.querySelector('[data-health-provider]');if(a)a.scrollIntoView({block:'center'})");
        capture(web,"35-health-naplo-adatforras","go('health');tp155R4OpenPanel('health-journal');document.querySelector('[data-health-provider]')?.scrollIntoView({block:'center'})");
        capture(web,"36-beallitasok-szinkron","go('settings');document.querySelector('#tp155R4PanelHost .tp155-r4-panel')?.scrollTo(0,600)");
        capture(web,"37-beallitasok-mentes","go('settings');document.querySelector('#tp155R4PanelHost .tp155-r4-panel')?.scrollTo(0,1250)");
        capture(web,"38-beallitasok-vege","go('settings');document.querySelector('#tp155R4PanelHost .tp155-r4-panel')?.scrollTo(0,2100)");
        capture(web,"39-naplo-adatokkal","go('history');window.scrollTo(0,350)");
        capture(web,"40-statisztikak-adatokkal","go('history');statsScreen()");


        // The active workout is populated using the installed application's
        // exercise library; no Health Connect or Samsung data are invented.
        capture(web,"27-aktiv-edzes","go('plan');const p=activeProgram();const d=p?.days?.[0];if(d&&typeof startWorkout==='function')startWorkout(d.id)");
        capture(web,"28-aktiv-edzes-also-resz","if(state.session){renderWorkout();window.scrollTo(0,650)}");
        capture(web,"29-aktiv-edzes-idozito","if(state.session){renderWorkout();document.querySelector('main details > summary')?.click()}");
        capture(web,"30-felbe-hagyott-edzes","if(state.session){persistDraft();state.session=null;state.workout=null};go('home')");
        capture(web,"31-edzes-folytatasa","if(db.get('draft',null)&&typeof resumeDraft==='function')resumeDraft()");

        // Persist evidence index before test ends so adb can collect even if a
        // later screenshot fails.
        try(FileOutputStream s = new FileOutputStream(new File(dir,"CAPTURE_INDEX.txt"))) {
            s.write(index.toString().getBytes(StandardCharsets.UTF_8));
        }
        assertTrue("Insufficient emulator screenshots", captured >= 20);
    }
}
