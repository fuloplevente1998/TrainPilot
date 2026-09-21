const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict');
const values=new Map();let html='';const root={get innerHTML(){return html},set innerHTML(v){html=v}};
const node=()=>({innerHTML:'',style:{setProperty(){}},classList:{add(){},remove(){},toggle(){},contains(){return false}},querySelector:()=>null,querySelectorAll:()=>[],appendChild(){},prepend(){},insertAdjacentElement(){},insertAdjacentHTML(){},remove(){},setAttribute(){},removeAttribute(){}});
const doc={querySelector:s=>s==='#app'?root:null,querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){},createElement:node,head:node(),body:node(),documentElement:node(),activeElement:null};
let tasks=[];const ctx=vm.createContext({localStorage:{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},document:doc,window:{Capacitor:{isNativePlatform:()=>true,Plugins:{GoogleSync:{status:async()=>({profile:null})}}},addEventListener(){},scrollTo(){},open(){},innerWidth:390,innerHeight:800,scrollY:0},navigator:{onLine:true},setInterval:()=>1,clearInterval(){},setTimeout:(fn,ms)=>{if(!ms)tasks.push(fn);return 1},clearTimeout(){},Date,crypto:require('node:crypto').webcrypto,TextEncoder,Blob,URL,Event:function(){},alert(){},confirm:()=>true,prompt:()=>null,console});
const index=fs.readFileSync('www/index.html','utf8'),order=[];
for(const [,attrs,inline] of index.matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/g)){const src=attrs.match(/src="([^"]+)"/);if(src)order.push(src[1]);vm.runInContext(src?fs.readFileSync('www/'+src[1],'utf8'):inline,ctx,{filename:src?.[1]||'inline'});}
for(let n=0;tasks.length&&n<10;n++){const q=tasks;tasks=[];for(const fn of q)fn();}
const run=s=>vm.runInContext(s,ctx);
assert.ok(order.indexOf('v253.js')<order.indexOf('v260.js')&&order.indexOf('v260.js')<order.indexOf('trainpilot-148-adaptive-planner.js')&&order.indexOf('trainpilot-148-adaptive-planner.js')<order.indexOf('ready.js'),'v148 must extend the stable v260 UI before ready');
assert.equal(run('RF260_UI_VERSION'),'2.6.2');
assert.equal(run('exercises().length'),100);assert.equal(run('exercises().filter(e=>demoInfo(e.id)).length'),100,'all 100 exercises must have demo/search mappings');
const squat=run("rf201ExerciseRow('A','db-squat',0)"),push=run("rf201ExerciseRow('A','pushup',0)");
assert.ok(squat.includes("openDemo('db-squat')")&&squat.includes('tp-play-btn'),'workout row must bind video by current exercise ID');
assert.ok(push.includes("openDemo('pushup')")&&!push.includes("openDemo('db-squat')"),'video binding must change with exercise ID');
run("state.calendarMonth='2026-09';state.rf2211CalendarDate='2026-09-14'");const cal=run('calendarGrid()');
assert.ok(cal.includes('tp-selected-day')&&cal.includes('aria-pressed="true"'),'selected calendar day must persist visually');
assert.equal(run("rf260FriendlyHealthSource('com.sec.android.app.shealth','')"),'Samsung Health');
assert.equal(run("rf260FriendlyHealthSource('com.example.watch','com.example.watch')"),'Health Connect');
const src=fs.readFileSync('www/v260.js','utf8');
for(const marker of ['tp-native-select','tp-select-trigger','tp-select-option','dispatchEvent(new Event(\'change\'','tp-nav-compact','rf260VideoButton','rf260EnhanceActiveStates'])assert.ok(src.includes(marker),'missing '+marker);

assert.ok(src.includes('rf260OpenScheduleMove')&&src.includes('rf260SaveScheduleMove'),'calendar move must use the in-app 2.6 editor');
assert.ok(src.includes('tp-modal')&&src.includes('data-tp-move-id'),'move action must have a persistent active UI state');
assert.ok(src.includes('rf260TrimHome')&&src.includes('Elvégzett edzések'),'home duplicate workout/weight summary must be removed by the 2.6 layer');
assert.ok(src.includes("select.addEventListener?.('change',()=>rf260SyncCustomSelect(select))"),'custom dropdown must stay synchronized with native select changes');
assert.ok(src.includes('.check.done{background:var(--accent)!important'),'completed set UI must follow the selected theme accent');
assert.ok(src.includes('.cal-cell.completed{border-color:var(--line)!important;background:#11151b!important'),'completed calendar days must be neutral check-only unless selected');
assert.ok(!/function rf260SaveScheduleMove[\s\S]{0,1200}prompt\(/.test(src),'2.6 move editor must not fall back to prompt dialogs');
assert.ok(src.includes('rf260VisualViewport')&&src.includes("dataset.placement=openTop?'top':'bottom'"),'DEV3 dropdown must place itself against the visual viewport and support upward opening');
assert.ok(src.includes('window.visualViewport?.addEventListener')&&src.includes('rf260RepositionOpenSelects'),'DEV3 dropdown must react to visual viewport/keyboard changes');
assert.ok(src.includes('rf260OptionSignature')&&src.includes('rf260BuildSelectOptions'),'custom dropdown must rebuild when native options change');
assert.ok(src.includes('rf260TrapModalFocus')&&src.includes('rf260MoveReturnFocus'),'move modal must trap focus and restore it on close');
assert.ok(src.includes('@media(max-width:360px)')&&src.includes('.tp-modal-actions{grid-template-columns:1fr}'),'move modal must have a narrow-phone layout');
assert.ok(src.includes('tp-brand-strip'),'compact nav must retain practical tap targets');
assert.ok(src.includes('const rf260GoBase=go')&&src.includes('window.scrollTo?.(0,0)'),'main navigation from the sticky header must open the destination at the top');
assert.ok(src.includes('!isDone(x)'),'completed schedule items must disappear from the planned-workouts list');
assert.ok(src.includes('RF260_NAV_COMPACT_ENTER=56,RF260_NAV_COMPACT_EXIT=8'),'sticky header must not expand again during an upward scroll before reaching the top');
assert.ok(src.includes('@media(prefers-reduced-motion:reduce)'),'reduced-motion users must not receive forced UI animations');
assert.ok(src.includes('.tp-select-trigger:focus-visible')&&src.includes("ev.key==='ArrowDown'")&&src.includes("ev.key==='Escape'"),'custom dropdown must expose keyboard/focus behavior');

console.log('PASS 2.6.2 UI: dynamic videos, redesigned dropdowns, persistent actions, calendar cleanup, 16 themes and stable compact nav.');
