const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];page.on('pageerror',e=>errors.push(String(e)));
 await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilotPhase3?.version==='phase3-29-30-37-r6');
 const key=await page.evaluate(()=>{
  db.set('language','hu');
  const start='2026-09-22T09:36:00.000Z',finish='2026-09-22T10:20:00.000Z';
  const row={id:'tp3-history',workout:'A',dayId:'A',programId:'home-basic',programName:'Otthoni A/B – Alap',started:start,finished:finish,
   exercises:[
    {id:'db-squat',hu:'Guggolás 2 kézisúlyzóval',en:'Dumbbell Squat',loadType:'per_hand',repUnit:'ism.',sets:[{set:1,weight:8,reps:'10',done:true},{set:2,weight:8,reps:'9',done:true}]},
    {id:'side-plank',hu:'Oldalsó plank',en:'Side Plank',loadType:'bodyweight',repUnit:'mp/oldal',sets:[{set:1,weight:0,reps:'20',done:true}]}
   ],
   photos:[
    {id:'tp3-photo-a',label:'before',createdAt:'2026-09-22T09:30:00.000Z',updatedAt:1,driveFileId:'drive-a'},
    {id:'tp3-photo-b',label:'after',createdAt:'2026-09-22T10:22:00.000Z',updatedAt:2,driveFileId:'drive-b'}
   ]};
  db.set('history',[row]);state.session=null;state.tab='history';render();return rf142WorkoutKey(row);
 });
 await page.waitForSelector('details.rf263-history');
 const journalChevron=page.locator('.rf103-history-chevron').first();
 assert.equal((await journalChevron.innerText()).trim(),'›','Journal row must use accepted right chevron');
 const journalStyle=await journalChevron.evaluate(e=>{const s=getComputedStyle(e),b=getComputedStyle(e,'::before');return {border:s.borderTopWidth,color:b.color,size:b.fontSize,weight:b.fontWeight}});
 assert.equal(journalStyle.border,'0px','unified chevron must not use the old boxed/circled border');
 const collapsedText=(await page.locator('details.rf263-history>summary').innerText()).replace(/\s+/g,' ');
 assert.doesNotMatch(collapsedText,/Guggolás 2 kézisúlyzóval|Oldalsó plank/,'collapsed Journal row must stay compact without concrete exercise performance');
 assert.equal(await page.getByRole('button',{name:'Edzés módosítása'}).count(),0,'legacy separate edit action must stay removed');
 const beforeOpenY=await page.evaluate(()=>scrollY);
 await page.locator('details.rf263-history>summary').click();
 await page.waitForSelector('details.rf263-history[open] .tp3-workout-summary');
 const afterOpenY=await page.evaluate(()=>scrollY);
 assert.ok(Math.abs(afterOpenY-beforeOpenY)<8,'opening a Journal card must not jump down to exercises');
 assert.equal(await page.locator('details.rf263-history[open] .tp3-history-editor').count(),0,'opening a workout must show summary first, not auto-enter edit mode');
 const summaryText=(await page.locator('.tp3-workout-summary').innerText()).replace(/\s+/g,' ');
 assert.match(summaryText,/Edzés összegzés/);
 assert.match(summaryText,/Mit edzettél\?/);
 assert.match(summaryText,/Guggolás 2 kézisúlyzóval.*8 kg\/kar × 10 ism\..*8 kg\/kar × 9 ism\./,'expanded workout summary must contain logged load/reps');
 assert.match(summaryText,/Oldalsó plank.*Bal 20 mp.*Jobb 20 mp/,'expanded workout summary must contain bilateral performed time');
 assert.equal(await page.locator('.tp3-summary-edit').count(),0,'accepted Journal design must not show a top-level Edit button');
 assert.equal(await page.locator('.rf-history-health-panel').count(),1,'Health data must remain inside the expanded Journal card');
 const healthArrowLayout=await page.evaluate(()=>{const h=document.querySelector('.rf-history-health-toggle'),ha=h?.querySelector('.rf-history-health-chevron'),p=document.querySelector('.tp3-photo-section>summary'),pa=p?.querySelector('.tp3-unified-chevron');if(!h||!ha||!p||!pa)return null;const hr=h.getBoundingClientRect(),har=ha.getBoundingClientRect(),pr=p.getBoundingClientRect(),par=pa.getBoundingClientRect();return {healthRight:hr.right-har.right,photoRight:pr.right-par.right,healthDisplay:getComputedStyle(h).display,justify:getComputedStyle(h).justifyContent}}); 
 assert.ok(healthArrowLayout&&Math.abs(healthArrowLayout.healthRight-healthArrowLayout.photoRight)<=3,'Health chevron must align to the right like Workout photos: '+JSON.stringify(healthArrowLayout));
 assert.equal(healthArrowLayout.healthDisplay,'flex');assert.equal(healthArrowLayout.justify,'space-between');
 const metrics=page.locator('.tp3-workout-summary .tp3-summary-metrics>div');
 assert.equal(await metrics.count(),4,'accepted summary must expose four compact metrics');
 const metricLayout=await page.locator('.tp3-summary-metrics').evaluate(e=>{const cs=getComputedStyle(e),children=[...e.children].map(x=>x.getBoundingClientRect());return {cols:cs.gridTemplateColumns.split(' ').length,tops:children.map(x=>Math.round(x.top)),overflow:e.scrollWidth-e.clientWidth,icons:e.querySelectorAll('.tp3-metric-icon').length}});
 assert.equal(metricLayout.cols,4,'phone Journal metrics must remain one 1x4 row');
 assert.equal(new Set(metricLayout.tops).size,1,'all four Journal metrics must share the same row');
 assert.ok(metricLayout.overflow<=1,'1x4 Journal metrics must not overflow horizontally');
 assert.equal(metricLayout.icons,4,'each Journal metric must have an icon');

 const rows=page.locator('details.tp3-history-ex-item');
 assert.equal(await rows.count(),2,'each logged exercise must be its own collapsible Journal row');
 assert.equal(await page.locator('details.tp3-history-ex-item[open]').count(),0,'logged exercises must start collapsed');
 await page.locator('details.rf263-history').evaluate(e=>e.dataset.tp3NoRenderProbe='kept');
 const side=rows.nth(1);await side.locator('summary').click();
 await page.waitForSelector('details.tp3-history-ex-item[open] .tp3-exercise-inline-editor');
 assert.equal(await page.locator('details.tp3-history-ex-item[open]').count(),1,'only the selected exercise should be open for editing');
 assert.equal(await page.locator('details.rf263-history').getAttribute('data-tp3-no-render-probe'),'kept','opening an exercise editor must not re-render the whole Journal card');
 const sideLabels=(await side.locator('label').allInnerTexts()).join(' | ');assert.match(sideLabels,/Bal oldal – mp/);assert.match(sideLabels,/Jobb oldal – mp/);
 const sideInputs=side.locator('.tp3-side input');assert.equal(await sideInputs.count(),2);assert.equal(await sideInputs.nth(0).inputValue(),'20');assert.equal(await sideInputs.nth(1).inputValue(),'20','legacy mp/oldal must map compatibly to both side fields');
 await sideInputs.nth(0).fill('23');await sideInputs.nth(1).fill('19');
 assert.equal(await side.locator('.tp3-exercise-actions .btn').count(),3,'exercise editor must expose Save, Cancel and Delete');
 const beforeSetCount=await side.locator('.tp3-set-row').count();
 await side.getByRole('button',{name:/Sorozat hozzáadása/}).click();
 assert.equal(await side.locator('.tp3-set-row').count(),beforeSetCount+1,'existing logged exercise must support adding a set');
 await side.locator('.tp3-mini-delete').last().click();
 assert.equal(await side.locator('.tp3-set-row').count(),beforeSetCount,'newly added set must be removable before save');
 await side.locator('.tp3-ex-save').click();await page.waitForTimeout(80);
 assert.notEqual(await page.locator('details.rf263-history').getAttribute('open'),null,'saving exercise must keep the workout card open');
 assert.equal(await page.locator('details.tp3-history-ex-item[open]').count(),0,'saving exercise must return the exercise row to compact state');
 const persisted=await page.evaluate(()=>{const e=history()[0].exercises.find(x=>x.id==='side-plank'),s=e.sets[0];return {left:s.leftSeconds,right:s.rightSeconds,reps:s.reps}});
 assert.deepEqual(persisted,{left:23,right:19,reps:'23'},'bilateral exercise edit must save separately while preserving legacy reps compatibility');

 const add=page.locator('details.tp3-add-exercise');assert.equal(await add.count(),1,'Mit edzettél list must expose + Exercise');
 await add.locator('summary').click();
 await add.locator('select').selectOption('pushup');
 await add.getByRole('button',{name:/Gyakorlat hozzáadása/}).click();
 await page.waitForSelector('details.tp3-history-ex-new[open] .tp3-exercise-inline-editor');
 assert.equal(await page.locator('details.tp3-history-ex-new .tp3-set-row').count(),1,'new exercise must start with at least one editable set');
 await page.locator('details.tp3-history-ex-new .tp3-ex-save').click();await page.waitForTimeout(80);
 assert.equal(await page.evaluate(()=>history()[0].exercises.some(e=>e.id==='pushup')),true,'new exercise must persist after Save');

 const photos=page.locator('details.tp3-photo-section');
 assert.equal(await photos.count(),1,'compact photo disclosure missing');
 assert.equal(await photos.getAttribute('open'),null,'photo section must start collapsed');
 assert.match((await photos.locator('summary').innerText()).replace(/\s+/g,' '),/Edzésfotók.*2 fotó/,'photo summary must show count');

 await page.evaluate(()=>tp155OpenJournalStats());await page.waitForFunction(()=>state.tab==='stats');await page.waitForSelector('.tp3-stat-row');
 assert.equal(await page.locator('button[onclick*="rf220Exercise"],[role="button"][onclick*="rf220Exercise"]').count(),0,'Statistics must not navigate to old separate exercise detail screen');
 const stat=page.locator('details.tp3-stat-row').first();assert.equal(await stat.getAttribute('open'),null);await stat.locator('summary').click();assert.notEqual(await stat.getAttribute('open'),null);
 const statChevron=stat.locator('.tp3-unified-chevron');const statStyle=await statChevron.evaluate(e=>{const s=getComputedStyle(e),b=getComputedStyle(e,'::before');return {border:s.borderTopWidth,color:b.color,size:b.fontSize,weight:b.fontWeight}});
 assert.equal(statStyle.border,'0px');assert.equal(statStyle.color,journalStyle.color);assert.equal(statStyle.size,journalStyle.size);assert.equal(statStyle.weight,journalStyle.weight);
 assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true,'Android Back must consume open Statistics disclosure');
 assert.equal(await stat.getAttribute('open'),null,'Android Back must close inline Statistics details first');
 assert.equal(await page.evaluate(()=>state.tab),'stats','closing Statistics disclosure must not navigate to Coach or another route');

 await page.evaluate(()=>go('home'));await page.waitForTimeout(120);
 const homeArrow=page.locator('.tp166-home-coach-chevron').first();assert.equal(await homeArrow.count(),1,'Home Mai javaslat reference chevron missing');
 const homeStyle=await homeArrow.evaluate(e=>{const s=getComputedStyle(e),b=getComputedStyle(e,'::before');return {border:s.borderTopWidth,color:b.color,size:b.fontSize,weight:b.fontWeight,text:e.textContent.trim()}});
 assert.deepEqual({border:homeStyle.border,color:homeStyle.color,size:homeStyle.size,weight:homeStyle.weight},{border:journalStyle.border,color:journalStyle.color,size:journalStyle.size,weight:journalStyle.weight},'Journal/Statistics chevrons must match Home reference arrow styling');
 assert.equal(homeStyle.text,'›');

 // Phone follow-up: Journal delete must complete from one physical tap and stay deleted
 // even if a stale current Drive snapshot still contains the workout.
 const deleted=await page.evaluate(()=>{
  const now=new Date(),row={id:'tp3-delete-phone',workout:'A',dayId:'A',programId:'home-basic',programName:'Törlendő teszt',started:new Date(now.getTime()-5*60000).toISOString(),finished:now.toISOString(),exercises:[{id:'db-squat',hu:'Guggolás',sets:[{set:1,weight:5,reps:'5',done:true}]}]};
  db.set('history',[row,...history()]);state.tab='history';render();return {key:rf142WorkoutKey(row),row};
 });
 await page.evaluate(k=>tp3DeleteHistoryWorkout(k),deleted.key);
 const deleteDialog=page.locator('#tp2628Dialog');await deleteDialog.waitFor({state:'visible'});
 await deleteDialog.locator('[data-tp2628-confirm]').dispatchEvent('pointerup',{pointerType:'touch',isPrimary:true});
 await deleteDialog.waitFor({state:'detached'});
 assert.equal(await page.evaluate(k=>history().some(x=>rf142WorkoutKey(x)===k),deleted.key),false,'one pointer tap must delete the Journal workout');
 const deletionState=await page.evaluate(row=>{
  const shell=(history=[])=>({history,weights:[],scheduled:[],settings:{rest:90},exercises:[],plan:{A:[],B:[]},programs:[],activeProgramId:'home-basic',plannerSettings:{}});
  const merged=TrainPilotIssue18.mergeSync(shell([]),[shell([row])],shell([row]),()=>{throw Error('deleted Journal row must be filtered before conflict resolution')},[]);
  return {tombstones:db.get('phase3HistoryTombstones',[]).length,merged:merged.history.length,isDeleted:tp3HistoryIsDeleted(row)};
 },deleted.row);
 assert.ok(deletionState.tombstones>=1,'deleting a workout must persist a local tombstone');
 assert.equal(deletionState.isDeleted,true,'deleted workout identity must be recognized later');
 assert.equal(deletionState.merged,0,'stale current Drive/cloudBase snapshots must not resurrect a deleted workout');

 // #37: Personal planner remains over the Home route, uses compact panel chrome and
 // survives the generator preview without falling back to a separate page.
 await page.evaluate(()=>go('home'));await page.waitForTimeout(80);
 const backgroundBefore=await page.evaluate(()=>state.tab);
 await page.getByRole('button',{name:/Segíts elkezdeni|Help me get started/i}).click();
 const planner=page.locator('#tp3PlannerPanelHost');await planner.waitFor({state:'visible'});
 assert.equal(await page.evaluate(()=>state.tab),backgroundBefore,'opening personal planner must not change the background route');
 assert.equal(await page.locator('main.rf221-home').count(),1,'Home must remain mounted behind the personal-planner panel');
 assert.equal(await planner.locator('main.tp152-profile').count(),1,'personal planner form must render inside the panel');
 assert.equal(await planner.locator('main.tp152-profile > button').filter({hasText:'←'}).count(),0,'separate in-page Back button must be removed');
 assert.equal(await planner.locator('.tp3-planner-close').innerText(),'×','panel must expose the unified red X close action');
 assert.equal(await planner.locator('.tp3-planner-group-icon').count(),5,'five planner sections must get compact health-style icon anchors');
 const primary=planner.locator('.tp3-planner-primary').first();
 const centering=await primary.evaluate((e)=>{const r=e.getBoundingClientRect(),p=e.closest('.tp3-planner-panel').getBoundingClientRect();return {w:r.width,delta:Math.abs((r.left+r.width/2)-(p.left+p.width/2))}});
 assert.ok(centering.w<=361&&centering.delta<4,'primary planner action must be compact and centered: '+JSON.stringify(centering));
 for(const width of [320,393,412]){
  await page.setViewportSize({width,height:873});await page.waitForTimeout(25);
  const fit=await planner.evaluate(e=>{const p=e.querySelector('.tp3-planner-panel'),m=e.querySelector('.tp3-planner-main');return {panel:p.scrollWidth-p.clientWidth,main:m.scrollWidth-m.clientWidth}});
  assert.ok(fit.panel<=1&&fit.main<=1,'planner must not overflow horizontally at '+width+'px: '+JSON.stringify(fit));
 }
 await page.setViewportSize({width:393,height:873});
 await page.evaluate(()=>{document.getElementById('pfAge').value='28';document.getElementById('pfHeight').value='180';document.getElementById('pfWeight').value='80';previewProfile()});
 await page.waitForSelector('#tp3PlannerPanelHost main.tp3-planner-preview');
 assert.equal(await page.evaluate(()=>state.tab),backgroundBefore,'planner preview must stay over the unchanged background route');
 assert.equal(await planner.locator('main.tp3-planner-preview > button').filter({hasText:'←'}).count(),0,'preview must not reintroduce the separate Back button');
 assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true,'Android Back must consume and close the personal-planner panel');
 await planner.waitFor({state:'detached'});
 assert.equal(await page.evaluate(()=>state.tab),backgroundBefore,'closing the planner must restore the unchanged Home route');

 assert.deepEqual(errors,[],'page errors: '+errors.join('\n'));
 console.log('PASS Phase 3 browser: Journal edit/stats + persistent single-tap deletion, unified chevrons, and #37 personal-planner panel/Back/mobile fit.');
}finally{await browser?.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});