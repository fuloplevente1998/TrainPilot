const assert=require('node:assert/strict'),fs=require('node:fs');
const index=fs.readFileSync('www/index.html','utf8');
const java=fs.readFileSync('android/app/src/main/java/com/repforge/app/WorkoutPhotosPlugin.java','utf8');
const manifest=fs.readFileSync('android/app/src/main/AndroidManifest.xml','utf8');
const paths=fs.readFileSync('android/app/src/main/res/xml/file_paths.xml','utf8');
const pkg=require('../package.json'),src=require('../SOURCE_VERSION.json'),gradle=fs.readFileSync('android/app/build.gradle','utf8');

assert.equal(pkg.version,'1.0.0');assert.equal(src.version,'1.0.0');assert.equal(src.versionCode,2671);
assert.equal(src.baselineCommit,'d0a3881ed9d8ec9d1238a914b6e6cab5da1e7c3e');
assert.match(gradle,/versionCode\s+2671/);assert.match(gradle,/versionName\s+"1\.0\.0"/);

for(const marker of ['TrainPilotPhase6','phase6-journal-media-health-42-43-r1','tp6-photo-dialog','tp6-photo-close','silentInlineHealthRefresh:true'])
 assert.ok(index.includes(marker),'missing Phase 6 UI marker '+marker);
const phase=index.slice(index.indexOf("const TP6_VERSION='phase6-journal-media-health-42-43-r1'"));
assert.ok(phase.includes('tp6PhotoOpenBase.apply(this,arguments)'),'photo modal must preserve existing acquisition wiring');
assert.ok(phase.includes("close.textContent='×'")&&phase.includes("close.setAttribute('aria-label','Bezárás')"),'compact photo dialog must use the top-right accessible X');
assert.ok(phase.includes("grid-template-columns:repeat(3,minmax(0,1fr))"),'three photo labels must stay compact on narrow screens');
assert.ok(phase.includes('tp6HistoryHealthFlights'),'Journal Health refresh must deduplicate in-flight reads');
assert.ok(phase.includes("refresh.setAttribute('aria-busy','true')"),'inline refresh needs non-modal busy state');
assert.ok(!phase.includes("rfHistoryHealthCache.set(h.started,{loading:true})"),'Phase 6 must not paint the transient Journal Health loading state');
const awaitAt=phase.indexOf("await healthPlugin().readWorkout"),finalPaintAt=phase.indexOf("rfHistoryHealthPaint(i);",awaitAt);
assert.ok(awaitAt>=0&&finalPaintAt>awaitAt,'Journal Health must paint only after the read finishes');

assert.ok(java.includes('cameraTemp = File.createTempFile("trainpilot-camera-", ".jpg"'),'camera must target private app cache');
assert.ok(java.includes('cameraUri = FileProvider.getUriForFile'),'camera must expose only a temporary FileProvider Uri');
assert.ok(java.includes('waitForFileData(outputFile)'),'camera callback must allow a short OEM file flush');
assert.ok(java.includes('returnedUri != null && uriHasData(returnedUri)'),'camera callback must accept returned content Uri fallback');
assert.ok(java.includes('returnedExtra instanceof Bitmap'),'camera callback must accept legacy Bitmap fallback');
assert.ok(java.indexOf('waitForFileData(outputFile)')<java.indexOf('resultCode == Activity.RESULT_CANCELED'),'non-empty image must beat an erroneous RESULT_CANCELED');
assert.ok(java.includes('else call.reject("A kamera nem adott vissza érvényes képet.")'),'successful callback without image must not masquerade as user cancellation');
assert.ok(!java.includes('createMediaStoreCameraUri')&&!java.includes('cameraUsesMediaStore'),'public MediaStore temp capture path must be removed');
assert.ok(paths.includes('<cache-path name="trainpilot_camera_cache" path="."'));
assert.ok(!manifest.includes('android.permission.CAMERA')&&!manifest.includes('READ_MEDIA_IMAGES')&&!manifest.includes('READ_EXTERNAL_STORAGE'));

console.log('PASS Phase 6 static: #42 private camera return handling + compact modal, #43 silent inline Health refresh.');
