const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
const listen=()=>new Promise(r=>server.listen(0,'127.0.0.1',r)),base=()=>'http://127.0.0.1:'+server.address().port+'/';
(async()=>{await listen();let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
 const errors=[];page.on('pageerror',e=>errors.push(String(e?.stack||e)));page.on('console',m=>{if(m.type()==='error')errors.push('console: '+m.text())});page.on('dialog',d=>d.accept());
 await page.addInitScript(()=>{
  localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));
  window.journalProjection={version:1,days:{},preferences:{autoOnOpen:false}};
  window.Capacitor={isNativePlatform:()=>true,Plugins:{HealthJournal:{
   pendingRestore:async()=>({}),initialize:()=>new Promise(resolve=>window.finishJournalInitialization=()=>resolve(window.journalProjection)),
   getProjection:async()=>window.journalProjection,readPage:async()=>({records:[],days:[],nextCursor:0,hasMore:false})
  }}};
 });await page.goto(base());await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.finishJournalInitialization);
 const recoveryState=await page.evaluate(()=>{
   const e=byId('side-plank'),now=new Date().toISOString();if(!e)throw Error('side-plank missing');
   const valid={programId:'home-level2',programName:'Otthoni A/B – Haladó',dayId:'A',workout:'A',started:now,finished:now,exercises:[{id:e.id,hu:e.hu,en:e.en,loadType:e.loadType,repUnit:e.repUnit,effort:'good',sets:[{set:1,weight:0,reps:'16',leftSeconds:20,rightSeconds:16,done:true}]}]};
   db.set('history',[
    valid,
    null,
    {started:now,finished:now,workout:'A',exercises:null},
    {started:now,finished:now,workout:'B',exercises:[null,{id:'side-plank',repUnit:'mp/oldal',sets:[null,{leftSeconds:'12',rightSeconds:'10',done:true}]}]}
   ]);
   state.tab='home';state.session=null;render();
   const hs=history();
   const recent=tp140Recent('side-plank','home-level2',3);
   const coach=rf152Recommendation('side-plank');
   return {len:hs.length,secondExercises:Array.isArray(hs[1]?.exercises),thirdExercises:Array.isArray(hs[2]?.exercises),recent:recent.length,coachText:String(coach?.text||'')};
 });
 assert.deepEqual({len:recoveryState.len,secondExercises:recoveryState.secondExercises,thirdExercises:recoveryState.thirdExercises},{len:4,secondExercises:true,thirdExercises:true},'runtime history reads must normalize malformed rows without changing row indexes');
 assert.ok(recoveryState.recent>=1,'progression history lookup must survive malformed rows');
 assert.ok(recoveryState.coachText.length>0,'Coach recommendation lookup must survive malformed rows');
 const nav=page.getByRole('button',{name:'Napló',exact:true}).first();assert.equal(await nav.count(),1,'Napló navigation button missing');
 await nav.click();await page.waitForTimeout(80);
 assert.equal(errors.length,0,'opening Journal raised runtime error: '+errors.join('\n'));
 assert.equal(await page.getByText('Edzésnapló',{exact:true}).count()>0,true,'Journal title must render after clicking Napló');
 assert.equal(await page.locator('details.rf263-history').count(),4,'legacy and partial rows must not prevent Journal rendering');
 const first=page.locator('details.rf263-history').first();assert.equal(await first.count(),1,'saved workout must appear in Journal');
 await first.locator(':scope > summary').click();await page.waitForTimeout(60);
 const detail=await first.innerText();assert.match(detail,/Oldalsó plank/);assert.match(detail,/Bal 20 mp/);assert.match(detail,/Jobb 16 mp/);

 // Health data can arrive after the user has opened a saved workout.
 const savedKey=await first.getAttribute('data-tp152-history-key');
 await first.locator('.rf-history-health-toggle').click();
 assert.equal(await first.locator('.rf-history-health-toggle').getAttribute('aria-expanded'),'false','health card closes when tapped');
 await page.evaluate(()=>{window.scrollTo(0,120);window.journalScroll=scrollY;});
 await page.evaluate(()=>finishJournalInitialization());
 await page.evaluate(()=>TrainPilotHealthJournal.ready());await page.waitForTimeout(100);
 assert.equal(await first.evaluate(node=>node.open),true,'background measurement refresh must keep the saved workout open');
 assert.equal(await first.locator('.rf-history-health-toggle').getAttribute('aria-expanded'),'false','background refresh retains the collapsed health card');
 assert.equal(await page.evaluate(()=>scrollY),await page.evaluate(()=>journalScroll),'background refresh retains the journal scroll position');
 await page.evaluate(()=>{journalProjection.preferences.autoOnOpen=true;});
 await page.evaluate(()=>TrainPilotHealthJournal.auto());await page.evaluate(()=>TrainPilotHealthJournal.auto());
 assert.equal(await first.evaluate(node=>node.open),true,'automatic import and its cooldown refresh keep the workout open');
 assert.equal(await first.locator('.rf-history-health-toggle').getAttribute('aria-expanded'),'false','automatic import retains health-card disclosure');

 // A sync inserts a new row: restore by identity, not by its old list index.
 const anchorTop=await first.evaluate(node=>node.getBoundingClientRect().top);
 await page.evaluate(()=>{const rows=history();db.set('history',[{...rows[0],id:'new-watch-workout',started:new Date().toISOString()},...rows]);TrainPilotUIRefresh.refresh();});
 const saved=page.locator('details.rf263-history[data-tp152-history-key='+JSON.stringify(savedKey)+']');
 assert.equal(await saved.getAttribute('data-tp152-history-key'),savedKey);
 assert.equal(await saved.evaluate(node=>node.open),true,'the same workout stays open after its index changes');
 assert.equal(await first.evaluate(node=>node.open),false,'a newly synced workout must not inherit the old open state');
 assert.equal(await saved.locator('.rf-history-health-toggle').getAttribute('aria-expanded'),'false','health-card state follows the workout identity');
 assert.ok(Math.abs(await saved.evaluate(node=>node.getBoundingClientRect().top)-anchorTop)<1,'sync preserves the visible workout position');
 await saved.locator('.rf-history-health-toggle').click();
 assert.equal(await saved.locator('.rf-history-health-toggle').getAttribute('aria-expanded'),'true','controls still target the correct row after sync');
 await saved.locator('.tp3-photo-section > summary').click();
 await page.evaluate(()=>{
  const rows=history();rows[1].healthWear129={source:'wear_health_services',workoutId:rows[1].started,watchId:'journal-regression-watch',revision:1,averageHeartRate:93,totalCalories:2};
  db.set('history',rows);TrainPilotUIRefresh.refresh();
 });
 assert.equal(await saved.evaluate(node=>node.open),true,'new measurement data must not close the workout');
 assert.match(await saved.locator('.rf-history-health').innerText(),/93.*bpm/,'background refresh still updates actual measurement values');
 assert.match(await saved.locator('.rf-history-health').innerText(),/2 kcal/);
 assert.equal(await saved.locator('.tp3-photo-section').evaluate(node=>node.open),true,'nested workout photos retain their disclosure state');
 await saved.locator(':scope > summary').click();
 await page.evaluate(()=>TrainPilotUIRefresh.refresh());
 assert.equal(await saved.evaluate(node=>node.open),false,'explicitly closed workout must stay closed');
 await saved.locator(':scope > summary').click();
 const exercise=saved.locator('.tp3-history-ex-item').first();await exercise.locator('summary').click();
 const draft=exercise.locator('.tp3-side input').first();await draft.fill('23');await draft.evaluate(node=>{node.blur();window.journalDraft=node;});
 await page.evaluate(()=>TrainPilotUIRefresh.refresh());
 assert.equal(await draft.inputValue(),'23','background refresh preserves an unfocused journal edit');
 assert.equal(await draft.evaluate(node=>node===journalDraft),true,'unfinished journal editor retains its controls');
 await page.evaluate(key=>tp3CancelExerciseEdit(key),savedKey);

 // A refresh can occur in the same task as opening, before the toggle event
 // records that state. Removed nodes must not overwrite the replacement view.
 await saved.locator(':scope > summary').click();
 await page.evaluate(key=>{const row=[...document.querySelectorAll('[data-tp152-history-key]')].find(el=>el.dataset.tp152HistoryKey===key);row.open=true;TrainPilotUIRefresh.refresh();},savedKey);
 assert.equal(await saved.evaluate(node=>node.open),true,'opening and refreshing in one task retains the workout');
 assert.equal(await saved.locator('.tp3-workout-summary').count(),1,'restored open workout is hydrated synchronously');
 await page.waitForTimeout(100);
 assert.equal(await saved.evaluate(node=>node.open),true,'queued old disclosure events cannot close the replacement');

 // Active-workout navigation must still be able to reach Journal after confirmation.
 await page.evaluate(()=>{const e=byId('plank');state.tab='plan';state.workout='A';state.session={workout:'A',dayId:'A',started:new Date().toISOString(),exercises:[{id:e.id,hu:e.hu,en:e.en,loadType:e.loadType,repUnit:e.repUnit||'mp',sets:[{set:1,weight:0,reps:'10',done:false}]}]};renderWorkout();});
 const workoutNav=page.getByRole('button',{name:'Napló',exact:true}).first();await workoutNav.click();await page.waitForTimeout(80);
 assert.equal(await page.locator('#tp2628Dialog').count(),0,'Journal detour from active workout must not get stuck behind a leave-workout confirmation');
 assert.equal(await page.getByText('Edzésnapló',{exact:true}).count()>0,true,'Journal must open immediately from an active workout');
 assert.equal(await page.evaluate(()=>state.session===null),true,'active in-memory session must be parked before Journal opens');
 assert.equal(await page.evaluate(()=>!!db.get('draft',null)?.session),true,'active workout must be preserved as a resumable draft before Journal opens');
 assert.equal(errors.length,0,'Journal navigation from active workout raised runtime error: '+errors.join('\n'));
 fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/journal-background-refresh.png',animations:'disabled'});
 console.log('PASS: Journal opens with bilateral/legacy history; late initialization, automatic refresh, new measurements and inserted workouts retain disclosures, scroll and unfinished edits; same-task opening and active-workout navigation remain safe.');
 await page.close();
}finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
