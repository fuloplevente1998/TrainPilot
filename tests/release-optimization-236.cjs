'use strict';
// #236 P1.5: the signed phone release must request R8 + resource shrinking,
// keep reflection contracts, and bundle an installable baseline profile.
const assert=require('node:assert/strict'),fs=require('node:fs');
const read=path=>fs.readFileSync(path,'utf8');
const gradle=read('android/app/build.gradle');
const wear=read('android/wear/build.gradle');
const rules=read('android/app/proguard-rules.pro');
const profile=read('android/app/src/main/baseline-prof.txt');
assert.match(gradle,/release\s*\{[\s\S]*?minifyEnabled true\s*\n\s*shrinkResources true/);
assert.match(gradle,/proguard-android-optimize\.txt/);
assert.match(gradle,/androidx\.profileinstaller:profileinstaller:1\.4\.1/);
for(const name of ['com.repforge.app','com.repforge.shared','com.getcapacitor','com.samsung.android.sdk.health.data','com.google.android.gms.wearable']){
 assert.ok(rules.includes('-keep class '+name+'.** { *; }'),'required reflective/native keep '+name);
}
assert.match(rules,/@com\.google\.gson\.annotations\.SerializedName/);
for(const name of ['MainActivity','StartupScreen','StartupPaint','WearSyncPlugin','HealthBridgePlugin','HealthJournalPlugin','GoogleSyncPlugin']){
 assert.ok(profile.includes('Lcom/repforge/app/'+name+';'),'startup baseline profile '+name);
}
assert.match(profile,/HSPLcom\/repforge\/app\/MainActivity;->onCreate\(Landroid\/os\/Bundle;\)V/);
for(const line of profile.split(/\r?\n/).filter(x=>x&&!x.startsWith('#'))){
 assert.match(line,/^(?:L[^\s]+;|HSPL[^\s]+;->[^\s]+)$/,'baseline profile rule format: '+line);
}
assert.match(wear,/minifyEnabled true/,'Wear R8 settings remain unchanged');
console.log('PASS #236 P1.5: release R8/shrink, ProfileInstaller, reflection guards and startup profile');
