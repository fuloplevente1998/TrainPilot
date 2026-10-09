'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const app=fs.readFileSync('www/app.js','utf8');
const source=fs.existsSync('www/wear-journal-sources-160.js')?fs.readFileSync('www/wear-journal-sources-160.js','utf8'):app.match(/\/\/ @section wear-journal-sources-160\.js\n([\s\S]*?)\/\/ @endsection wear-journal-sources-160\.js/)?.[1];
assert.ok(source,'metric-source presenter must be a standalone shipped runtime section');
const workout={
 started:'2026-10-09T20:00:00Z',finished:'2026-10-09T20:45:00Z',syncId:'watch-001',
 healthWear129:{source:'wear_health_services',workoutId:'watch-001',watchId:'watch7',revision:4,
 totalCalories:390,averageHeartRate:121.2,maxHeartRate:158,steps:0,activeDurationSeconds:2700},
 health240:{windowStart:'2026-10-09T20:00:00Z',windowEnd:'2026-10-09T20:45:00Z',
 totalCalories:49,activeCalories:1}
};
const rows=[workout],window={};
let paints=0;
const document={head:{appendChild(){}},createElement(){return {id:'',textContent:''}}};
const context=vm.createContext({window,document,history:()=>rows,
 rfHistoryHealthHtml:i=>'<div class="hc-only">HC: '+rows[i].health240.totalCalories+' kcal</div>',
 rfHistoryHealthPaint:()=>paints++,esc:value=>String(value),
 console,Date,Number});
vm.runInContext(source,context);
const output=()=>vm.runInContext('rfHistoryHealthHtml(0)',context);
assert.match(output(),/TrainPilot óra/);
assert.match(output(),/390 kcal/,'wear total must be the primary choice');
assert.doesNotMatch(output(),/HC: 49 kcal/,'HC window sum is never presented as watch workout total');
assert.match(output(),/0/,'a real zero is not missing');
assert.equal(window.TrainPilotWearJournalSource.choose(0,'health_connect'),true);
assert.match(output(),/HC: 49 kcal/,'manual HC source is shown without changing saved data');
assert.doesNotMatch(output(),/390 kcal/);
assert.equal(rows[0].healthWear129.totalCalories,390,'source selection must not mutate the source');
assert.equal(rows[0].health240.totalCalories,49);
assert.equal(window.TrainPilotWearJournalSource.choose(0,'wear'),true);
assert.match(output(),/390 kcal/);
assert.equal(paints,2);
rows[0].healthWear129={source:'wear_health_services',workoutId:'unrelated',watchId:'watch7',revision:5,totalCalories:500};
assert.equal(window.TrainPilotWearJournalSource.measured(rows[0]),null,'foreign workout metrics may not leak into history');
assert.equal(output(),'<div class="hc-only">HC: 49 kcal</div>','without valid watch data, retain the Health Connect display');
console.log('PASS Journal provenance: Wear 390 vs HC 49, explicit selection, canonical row immutability, identity checks and no invented calories');
