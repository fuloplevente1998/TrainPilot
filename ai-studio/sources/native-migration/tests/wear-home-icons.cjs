'use strict';
const fs=require('node:fs'),assert=require('node:assert/strict');
const html=fs.readFileSync('www/index.html','utf8');
const start=html.indexOf(' const icons={"home"'),end=html.indexOf(';',start);
const icons=JSON.parse(html.slice(start,end).split('=').slice(1).join('='));
for(const [wear,key] of Object.entries({workout:'plan',journal:'history',settings:'settings',heart:'health',calendar:'calendar'})){
 const native=fs.readFileSync('android/wear/src/main/res/drawable/ic_tp_'+wear+'.xml','utf8');
 for(const path of icons[key].matchAll(/<path d="([^"]+)"/g))assert.ok(native.includes('android:pathData="'+path[1]+'"'),wear+' must use the original phone path');
 assert.ok(native.includes('android:viewportWidth="24"'));
 assert.ok(native.includes('android:strokeWidth="2"'));
}
console.log('PASS Wear home original phone navigation icon paths');
