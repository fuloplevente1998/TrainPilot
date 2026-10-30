from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]

app = ROOT / 'www/app.js'
s = app.read_text(encoding='utf-8')
anchor = "window.TrainPilotInlineCompact={version:'2630'};\nconst TP146_UI_VERSION='1.4.6';"
if anchor not in s:
    raise SystemExit('1.4.6 insertion anchor missing')

code = r'''// 1.4.6 main: unified compact cards, consistent program/day titles and final WebView dialog audit.
var tp146ProgramDayTitle=function tp146ProgramDayTitle(program,day){
 const name=String(program?.name||'Program').replace(/\s*[–—]\s*/g,' - ').replace(/\s+-\s+/g,' - ').trim();
 const d=String(day?.name||day?.id||'').trim();
 return d?`${name} - ${d}`:name;
};
workoutTitle=function(programId,dayId){const p=programById(programId)||activeProgram(),d=programDay(p,dayId);return tp146ProgramDayTitle(p,d)};

planScreen=function(){
 const p=activeProgram();
 if(!p)return shell(`<main><div class="hero"><h1>Nincs aktív program</h1><div class="muted">Válassz programot a Programok fülön.</div></div><button class="btn block" onclick="go('programs')">Program választása</button></main>`);
 return shell(`<main class="tp146-plan"><div class="hero"><span class="badge">AKTÍV PROGRAM</span><h1>${esc(p.name)}</h1><div class="muted">${esc(p.location||'')} ${p.level?`• ${esc(p.level)}`:''} • ${p.days?.length||0} napos ciklus</div><br><button class="btn secondary block" onclick="go('programs')">Másik program választása</button></div>${(p.days||[]).map(d=>`<div class="card tp146-day-head"><div class="tp146-day-copy"><span class="badge">EDZÉSNAP</span><h2 class="tp146-day-title">${esc(tp146ProgramDayTitle(p,d))}</h2><div class="small muted">${d.exercises?.length||0} gyakorlat • koppints a részletekhez</div></div><span class="tp146-day-letter" aria-label="${esc(d.name||d.id)}">${esc(d.id||d.name||'')}</span></div>${(d.exercises||[]).map((id,i)=>rf201ExerciseRow(d.id,id,i)).join('')}<button class="btn secondary block" onclick="rf154AddScreen('${esc(d.id)}')">+ Gyakorlat hozzáadása</button><br><button class="btn block" onclick="startWorkout('${esc(d.id)}')">Edzés indítása</button>`).join('<br>')}</main>`);
};
showWorkout=function(dayId){
 state.workout=dayId;state.tab='plan';const p=activeProgram(),d=programDay(p,dayId);if(!p||!d){render();return;}
 render(shell(`<main class="tp146-plan"><button class="btn secondary" onclick="go('plan')">← ${esc(p.name)}</button><div class="hero"><span class="badge">AKTÍV PROGRAM</span><h1>${esc(tp146ProgramDayTitle(p,d))}</h1><div class="muted">${d.exercises?.length||0} gyakorlat</div></div>${(d.exercises||[]).map((id,i)=>rf201ExerciseRow(d.id,id,i)).join('')}<br><button class="btn secondary block" onclick="rf154AddScreen('${esc(d.id)}')">+ Gyakorlat hozzáadása</button><br><button class="btn block" onclick="startWorkout('${esc(d.id)}')">Edzés indítása</button></main>`));
};

const tp146HomeBase=home;
home=function(){
 let html=tp146HomeBase();const p=activeProgram(),np=nextPlanned(),next=nextWorkout(),targetProgram=np?programById(np.programId||p?.id):p,targetDay=programDay(targetProgram,np?(np.dayId||np.workout):next);
 if(!targetProgram||!targetDay)return html;
 const title=esc(tp146ProgramDayTitle(targetProgram,targetDay)),when=np?esc(fmtDate(np.start)):`${targetDay.exercises?.length||0} gyakorlat`;
 const block=`<div class="tp146-next"><span class="small muted">Következő</span><strong class="tp146-next-title">${title}</strong><span class="small muted tp146-next-time">${when}</span></div><br>`;
 html=html.replace(/<div class="muted">Következő:[\s\S]*?<\/div><br>/,block).replace(/<div class="muted">Következő ciklusnap:[\s\S]*?<\/div><br>/,block);
 return html;
};

scheduleListHtml=function(){
 const isDone=x=>typeof rf209ScheduleDone==='function'?rf209ScheduleDone(x):(x.status==='completed'||history().some(h=>h.scheduleId===x.id));
 const items=scheduled().filter(x=>!x.cancelled&&!isDone(x)).sort((a,b)=>Date.parse(a.start)-Date.parse(b.start)).slice(-120);
 return items.length?items.map(x=>{const p=programById(x.programId||'home-basic'),d=programDay(p,x.dayId||x.workout),skipped=x.status==='skipped',moving=state?.rf260MovingScheduleId===x.id;return `<div class="card schedule-card tp146-schedule-card"><div class="tp146-schedule-copy"><strong class="tp146-schedule-title">${esc(tp146ProgramDayTitle(p,d))}</strong><div class="small muted">${esc(fmtDate(x.start))} • ${skipped?'Kihagyva':'Tervezett'}</div></div><div class="schedule-actions tp146-schedule-actions"><div class="tp146-schedule-primary"><button class="btn" onclick="startScheduledById('${esc(x.id)}')">Indítás</button><button class="btn secondary ${moving?'tp-action-active':''}" data-tp-move-id="${esc(x.id)}" aria-pressed="${moving?'true':'false'}" onclick="rf260OpenScheduleMove('${esc(x.id)}',this)">Áthelyezés</button></div><div class="tp146-schedule-side"><button class="btn danger" onclick="rf209DeleteSchedule('${esc(x.id)}')">Törlés</button><button class="btn secondary ${skipped?'tp-selected':''}" aria-pressed="${skipped?'true':'false'}" onclick="skipSchedule('${esc(x.id)}')">${skipped?'Vissza':'Kihagyás'}</button></div></div></div>`}).join(''):'<div class="muted">Még nincs tervezett edzés.</div>';
};

var tp146CompactHealthCards=function tp146CompactHealthCards(){
 const main=document.querySelector?.('main.rf263-health');if(!main)return;
 main.querySelectorAll?.(':scope > .card')?.forEach(card=>{const h=card.querySelector?.(':scope > h2');if(!h)return;const text=String(h.textContent||'').trim();if(/^(Mai állapot|Today status|Heutiger Status|Starea de azi|Test és fittség|Body and fitness|Körper und Fitness|Corp și fitness)$/i.test(text))card.classList.add('tp146-health-compact')});
};
if(typeof rf263HealthHub==='function'){
 const tp146HealthHubBase=rf263HealthHub;
 rf263HealthHub=function(){const r=tp146HealthHubBase.apply(this,arguments);tp146CompactHealthCards();return r};
}

if(typeof rf130DeletePhoto==='function'){
 rf130DeletePhoto=async function(key,id){
  const q=rf130FindPhoto(key,id);if(!q.p||q.p.deletedAt)return;
  const ok=await tp2628Confirm('Törlöd ezt a naplófotót? A következő Drive-szinkron a felhőből is eltávolítja.',{title:'Naplófotó törlése',confirmText:'Törlés',danger:true});
  if(!ok)return;
  try{await rf130PhotoPlugin()?.delete?.({id})}catch(_){}
  q.p.deletedAt=new Date().toISOString();q.p.updatedAt=Date.now();db.set('history',q.h);cloudChanged();render();
 };
}

window.TrainPilotUiUnify={version:'2630-main',nativeDialogAudit:true};
window.TrainPilotInlineCompact={version:'2630'};
const TP146_UI_VERSION='1.4.6';'''

s = s.replace(anchor, code, 1)
app.write_text(s, encoding='utf-8')

cssp = ROOT / 'www/styles.css'
css = cssp.read_text(encoding='utf-8')
marker = '/* TrainPilot 1.4.6 main unified compact UI */'
if marker not in css:
    css += r'''

/* TrainPilot 1.4.6 main unified compact UI */
.tp146-day-head{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:10px 12px!important;margin:10px 0 6px!important;border-radius:15px!important}
.tp146-day-copy{min-width:0}.tp146-day-title{font-size:18px!important;line-height:1.18;margin:5px 0 2px!important}.tp146-day-letter{display:grid;place-items:center;flex:0 0 42px;width:42px;height:42px;border-radius:13px;border:1px solid color-mix(in srgb,var(--accent) 72%,var(--line));background:color-mix(in srgb,var(--accent) 14%,var(--card));color:var(--accent2);font-size:22px;font-weight:950}
.tp146-next{display:flex;flex-direction:column;gap:2px;margin-top:4px}.tp146-next-title{display:block;font-size:18px;line-height:1.2;color:var(--text)}.tp146-next-time{font-size:12px!important}
.tp146-schedule-card{padding:10px 11px!important;margin:6px 0!important;gap:8px!important;border-radius:15px!important}.tp146-schedule-title{font-size:15px;line-height:1.18}.tp146-schedule-copy{min-width:0}.tp146-schedule-actions{display:grid!important;grid-template-columns:minmax(0,1fr) auto;gap:7px!important;align-items:stretch}.tp146-schedule-primary{display:grid;grid-template-columns:1fr 1fr;gap:6px}.tp146-schedule-side{display:grid;grid-template-rows:1fr 1fr;gap:6px}.tp146-schedule-actions .btn{min-height:36px!important;padding:7px 9px!important;margin:0!important}.tp146-schedule-side .btn{min-width:88px}
.tp146-health-compact{padding:10px 11px!important;margin:7px 0!important;border-radius:15px!important}.tp146-health-compact>h2{font-size:18px!important;line-height:1.18;margin:0 0 7px!important}.tp146-health-compact>.grid2{gap:6px!important}.tp146-health-compact .stat{padding:7px 8px!important;min-height:0!important}.tp146-health-compact .stat small{font-size:10px!important}.tp146-health-compact .stat strong{font-size:17px!important}.tp146-health-compact .rf251-weight-btn{margin-top:7px!important;min-height:38px!important;padding:7px 9px!important}
@media(max-width:380px){.tp146-schedule-actions{grid-template-columns:1fr auto}.tp146-schedule-primary{grid-template-columns:1fr}.tp146-schedule-side .btn{min-width:82px}.tp146-day-title{font-size:17px!important}.tp146-day-letter{width:38px;height:38px;flex-basis:38px;font-size:20px}.tp146-next-title{font-size:17px}}
'''
cssp.write_text(css, encoding='utf-8')

lockp = ROOT / 'package-lock.json'
lock = json.loads(lockp.read_text(encoding='utf-8'))
lock['version'] = '1.4.6'
if '' in lock.get('packages', {}):
    lock['packages']['']['version'] = '1.4.6'
lockp.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

pkgp = ROOT / 'package.json'
pkg = json.loads(pkgp.read_text(encoding='utf-8'))
ui = pkg.setdefault('scripts', {}).get('test:ui', '')
for test in ['node tests/browser/inline-compact-2630.cjs', 'node tests/browser/ui-unify-dialogs-2630.cjs']:
    if test not in ui:
        ui = (ui + ' && ' + test).strip(' &')
pkg['scripts']['test:ui'] = ui
pkgp.write_text(json.dumps(pkg, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

test = r'''const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
(async()=>{
 const root=path.resolve('www');const server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox']});const page=await browser.newPage({viewport:{width:393,height:720}});const nativeDialogs=[];page.on('dialog',async d=>{nativeDialogs.push({type:d.type(),message:d.message()});await d.dismiss()});
  await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);await page.waitForTimeout(120);
  assert.equal(await page.evaluate(()=>window.TrainPilotUiUnify?.version),'2630-main');
  assert.equal(await page.evaluate(()=>makeBackup().appVersion),'1.4.6');

  await page.evaluate(()=>{db.set('activeProgramId','home-basic');state.session=null;state.tab='plan';render()});await page.waitForTimeout(60);
  const dayTitle=(await page.locator('.tp146-day-title').first().innerText()).replace(/\s+/g,' ');assert.match(dayTitle,/Otthoni A\/B - Alap - A/);
  const ex=page.locator('details.tp146-exercise').first();assert.equal(await ex.count(),1);await ex.locator('summary').click();assert.notEqual(await ex.getAttribute('open'),null,'exercise must expand in place');

  const sid=await page.evaluate(()=>{const p=activeProgram(),d=p.days[0],id=crypto.randomUUID(),st=new Date(Date.now()+86400000),en=new Date(st.getTime()+45*60000);db.set('scheduled',[{id,programId:p.id,dayId:d.id,workout:d.id,start:st.toISOString(),end:en.toISOString(),updatedAt:Date.now(),cancelled:false,status:'planned'}]);state.tab='home';render();return id});await page.waitForTimeout(80);
  const next=page.locator('.tp146-next-title');assert.equal(await next.count(),1);assert.match((await next.innerText()).replace(/\s+/g,' '),/Otthoni A\/B - Alap - A/);assert.ok(parseFloat(await next.evaluate(e=>getComputedStyle(e).fontSize))>=17,'next workout title must be emphasized');

  await page.evaluate(()=>go('calendar'));await page.waitForTimeout(80);const card=page.locator('.tp146-schedule-card').first();assert.equal(await card.count(),1);assert.match((await card.locator('.tp146-schedule-title').innerText()).replace(/\s+/g,' '),/Otthoni A\/B - Alap - A/);const side=await card.locator('.tp146-schedule-side button').allInnerTexts();assert.deepEqual(side.slice(0,2),['Törlés','Kihagyás'],'delete must sit above skip and keep its danger styling');assert.ok((await card.evaluate(e=>e.getBoundingClientRect().height))<150,'planned workout card must stay compact');

  await page.evaluate(()=>rf263HealthHub());await page.waitForTimeout(80);assert.equal(await page.locator('.tp146-health-compact').count(),2,'Today and Body/Fitness cards must use compact styling');

  const photo=await page.evaluate(()=>{const now=new Date(),start=new Date(now.getTime()-30*60000);const row={id:'tp146-photo-workout',workout:'A',dayId:'A',programId:'home-basic',programName:'Otthoni A/B – Alap',started:start.toISOString(),finished:now.toISOString(),exercises:[],photos:[{id:'tp146-photo',label:'after',createdAt:now.toISOString(),updatedAt:Date.now(),driveFileId:'drive-test'}]};db.set('history',[row]);return {key:rf142WorkoutKey(row),id:'tp146-photo'} });
  await page.evaluate(x=>{void rf130DeletePhoto(x.key,x.id)},photo);const modal=page.locator('#tp2628Dialog');await modal.waitFor({state:'visible'});assert.match(await modal.innerText(),/Naplófotó törlése/);await modal.locator('[data-tp2628-cancel]').click();await modal.waitFor({state:'detached'});assert.equal(await page.evaluate(x=>!!rf130FindPhoto(x.key,x.id).p?.deletedAt,photo),false,'cancel must preserve photo');
  await page.evaluate(x=>{void rf130DeletePhoto(x.key,x.id)},photo);await modal.waitFor({state:'visible'});await modal.locator('[data-tp2628-confirm]').click();await page.waitForFunction(x=>!!rf130FindPhoto(x.key,x.id).p?.deletedAt,photo);

  const suspects=await page.evaluate(()=>Object.getOwnPropertyNames(window).flatMap(name=>{if(name==='confirm'||name==='prompt')return [];let fn;try{fn=window[name]}catch(_){return []}if(typeof fn!=='function')return [];let src='';try{src=Function.prototype.toString.call(fn)}catch(_){return []}return /\b(?:confirm|prompt)\s*\(/.test(src)?[name]:[]}));
  assert.deepEqual(suspects,[],'no active global app function may call native confirm()/prompt() directly');
  assert.deepEqual(nativeDialogs,[],'audited UI paths must not open native WebView dialogs');
  console.log('PASS: 1.4.6 compact UI unification + photo delete + global WebView dialog audit.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
'''
(ROOT / 'tests/browser/ui-unify-dialogs-2630.cjs').write_text(test, encoding='utf-8')

rp = ROOT / 'README.md'
rs = rp.read_text(encoding='utf-8')
rs = rs.replace('# TrainPilot 1.4.5', '# TrainPilot 1.4.6', 1).replace('Aktuális verzió: **1.4.5**, Android `versionCode`: **2629**', 'Aktuális verzió: **1.4.6**, Android `versionCode`: **2630**', 1)
rp.write_text(rs, encoding='utf-8')

notes = ROOT / 'RELEASE_NOTES.md'
ns = notes.read_text(encoding='utf-8')
section = '''# TrainPilot 1.4.6 – kompakt, egységesített felület\n\n- Az Edzés fül gyakorlatrészletei helyben lenyílnak; az A/B edzésnap címe teljes programnévvel jelenik meg.\n- A Napló összecsukott edzései és a Naptár tervezett edzései kompaktabb kártyákat kaptak.\n- A Naptárban a Törlés a Kihagyás fölé került, az eredeti veszély/szekunder színekkel.\n- A Kezdőlapon a következő edzés teljes programnap-neve nagyobb hangsúlyt kap.\n- Az Egészség / Mai állapot és Test és fittség kártyák kompaktabbak.\n- A naplófotó törlése már TrainPilot-saját megerősítő ablakot használ; a globális aktív confirm/prompt útvonalakat Chromium teszt ellenőrzi, hogy ne nyissanak natív WebView dialógust.\n- Android `versionCode 2630`, `versionName 1.4.6`.\n\n'''
if not ns.startswith('# TrainPilot 1.4.6'):
    notes.write_text(section + ns, encoding='utf-8')

print('Prepared TrainPilot 1.4.6 main UI unification + WebView audit')
