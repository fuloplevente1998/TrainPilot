const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict');
const values=new Map();let writes=0,html='';const root={get innerHTML(){return html},set innerHTML(v){writes++;html=v}};
const node=()=>({innerHTML:'',style:{setProperty(){}},classList:{add(){},remove(){}},querySelector:()=>null,querySelectorAll:()=>[],appendChild(){},prepend(){},insertAdjacentElement(){},insertAdjacentHTML(){},remove(){},setAttribute(){}});
const doc={querySelector:s=>s==='#app'?root:null,querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){},createElement:node,head:node(),body:node(),documentElement:node()};
let tasks=[],cloudChecks=0;const errors=[];const ctx=vm.createContext({localStorage:{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},document:doc,window:{Capacitor:{isNativePlatform:()=>true,Plugins:{GoogleSync:{status:async()=>{cloudChecks++;return {profile:null}}}}},addEventListener:(name,fn)=>{if(name==='error')errors.push(fn)},scrollTo(){},open(){}},navigator:{onLine:true},setInterval:()=>1,clearInterval(){},setTimeout:(fn,ms)=>{if(!ms)tasks.push(fn);return 1},clearTimeout(){},Date,crypto:require('node:crypto').webcrypto,TextEncoder,Blob,URL,alert(){},confirm:()=>true,prompt:()=>null,console});
const index=fs.readFileSync('www/index.html','utf8');
const order=[];
for(const [,attrs,inline] of index.matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/g)){
 const src=attrs.match(/src="([^"]+)"/);if(src){order.push(src[1]);if(src[1]!=='startup.js')assert.ok(attrs.includes('defer'),'feature scripts must defer');}
 vm.runInContext(src?fs.readFileSync('www/'+src[1],'utf8'):inline,ctx,{filename:src?.[1]||'inline'});
 if(src&&src[1]!=='ready.js')assert.equal(writes,0,'no partial screen before the final layer');
 assert.equal(cloudChecks,0,'cloud must not block initial UI');
}
const run=s=>vm.runInContext(s,ctx);


assert.equal(order[0],'startup.js');assert.equal(order.at(-1),'ready.js');
assert.ok(order.indexOf('v228c.js')<order.indexOf('ready.js'));
assert.ok(order.indexOf('v229.js')<order.indexOf('ready.js'));
assert.equal(writes,1,'one final startup render');
assert.equal(run('makeBackup().appVersion'),'1.4.6');
for(let n=0;tasks.length&&n<10;n++){const q=tasks;tasks=[];for(const fn of q)fn();}
assert.equal(cloudChecks,1);assert.equal(run('window.TrainPilotBoot.finished'),true);
run('window.TrainPilotBoot.finish()');assert.equal(writes,1,'finish is idempotent');
assert.equal(run("programById('home-basic').days[0].exercises.length"),7);
assert.ok(run('calendarScreen()').includes('rf230Mode'));assert.ok(run('rf2211Planner()').includes('tp155-planner-always-open'));assert.ok(!run('rf2211Planner()').includes('<summary>'));
assert.equal(run('rf229Median([null,40,60,undefined,""])'),50);
assert.equal(run('rf229Median([null,undefined])'),null);
run("go('health')");const healthHtml=html;run('render()');assert.equal(html,healthHtml);
run("go('calendar')");assert.ok(html.includes('rf230Mode'));
// A failed script must retain the boot overlay and refuse to show a partial app.
run('window.TrainPilotBoot.finished=false;window.TrainPilotBoot.loading=true');
for(const fn of errors)fn();const before=writes;run('window.TrainPilotBoot.finish()');
assert.equal(run('window.TrainPilotBoot.failed'),true);assert.equal(writes,before);
console.log('PASS startup: one final render, deferred cloud, preserved planner/Health/A-B, missing-data baseline and boot failure');
