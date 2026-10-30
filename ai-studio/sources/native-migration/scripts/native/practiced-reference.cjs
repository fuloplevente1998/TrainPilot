'use strict';
// Data fixtures from the pinned main function; never bundled as an app runtime.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const {execFileSync}=require('node:child_process');
const ref='7473b30:www/app.js';
const blob=execFileSync('git',['rev-parse',ref],{encoding:'utf8'}).trim();
assert.equal(blob,'b5567576bed9a8f93608a6bf1ed8db1dee4723f4');
const source=execFileSync('git',['show',ref],{encoding:'utf8',maxBuffer:8*1024*1024});
const definitions=source.slice(source.indexOf('var tp128Finished=function'),source.indexOf('// Read history once when generating a preview.'));
assert.ok(definitions.includes('var tp128PracticedIds=function'));
const now=Date.parse('2026-10-25T00:00:00Z');
class Clock extends Date {static now(){return now;}}
const library={id:'barbell-row',loadType:'total',repUnit:'ism.',custom:false};
const cases=[
 ['complete',{},{}],['partial',{done:false},{}],['pain',{effort:'pain'},{}],
 ['workout-pain',{}, {feedback:{rating:'pain'}}],['other-units',{repUnit:'mp'},{}],
 ['other-load',{loadType:'per_hand'},{}],['other-variation',{variation:'paused'},{}],
 ['null-weight',{weight:null},{}],['empty-weight',{weight:''},{}],['nonfinite-weight',{weight:'NaN'},{}],
 ['old',{}, {ageDays:91}],['future',{}, {ageDays:-1}]
].map(([name,change,workoutChange])=>{
 const set={done:true,reps:'12',weight:20,...change};
 const exercise={...library,effort:change.effort||'good',loadType:change.loadType||'total',repUnit:change.repUnit||'ism.',
  prescription:{variation:change.variation||''},sets:[set,{...set}]};
 const end=now-(workoutChange.ageDays??1)*86400000;
 const workout={id:name,programId:'unrelated-workout',dayId:'custom',started:new Date(end-600000).toISOString(),
  finished:new Date(end).toISOString(),feedback:workoutChange.feedback||{},exercises:[exercise]};
 const context=vm.createContext({Date:Clock,exercises:()=>[library],history:()=>[workout]});
 const expected=vm.runInContext(definitions+'\ntp128PracticedIds().has("barbell-row")',context);
 return {name,workout,expected};
});
assert.equal(cases.filter(c=>c.expected).length,1);
fs.writeFileSync('android/app/src/test/resources/native-practiced-main-7473.json',JSON.stringify({ref,blob,asOf:new Date(now).toISOString(),cases},null,2)+'\n');
process.stdout.write(`Generated ${cases.length} actual-main practiced fixtures; Kotlin/Android not executed.\n`);
