const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];page.on('pageerror',e=>errors.push(String(e)));
 await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilotPhase3?.version==='phase3-29-30-r2');
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
 const performance=(await page.locator('.tp3-history-performance-list').innerText()).replace(/\s+/g,' ');
 assert.match(performance,/Guggolás 2 kézisúlyzóval.*8 kg\/kar × 10 ism\..*8 kg\/kar × 9 ism\./,'collapsed Journal card must show the performed load/reps');
 assert.match(performance,/Oldalsó plank.*Bal 20 mp.*Jobb 20 mp/,'collapsed Journal card must show bilateral performed time');
 assert.equal(await page.getByRole('button',{name:'Edzés módosítása'}).count(),0,'separate edit action must be removed after summary/editor merge');
 await page.locator('details.rf263-history>summary').click();
 await page.waitForSelector('details.rf263-history[open] .tp3-history-editor');
 const photos=page.locator('details.tp3-photo-section');
 assert.equal(await photos.count(),1,'compact photo disclosure missing');
 assert.equal(await photos.getAttribute('open'),null,'photo section must start collapsed');
 assert.match((await photos.locator('summary').innerText()).replace(/\s+/g,' '),/Edzésfotók.*2 fotó/,'photo summary must show count');
 await photos.locator('summary').click();assert.notEqual(await photos.getAttribute('open'),null);assert.equal(await photos.locator('.rf130-photo-card').count(),2,'opening photo section must preserve existing photo functions/cards');
 const editors=page.locator('details.tp3-history-ex');assert.equal(await editors.count(),2);
 assert.equal(await page.locator('details.tp3-history-ex[open]').count(),0,'all exercise editors must start collapsed on fresh Journal edit');
 const side=editors.nth(1);await side.locator('summary').click();
 const sideLabels=(await side.locator('label').allInnerTexts()).join(' | ');assert.match(sideLabels,/Bal oldal – mp/);assert.match(sideLabels,/Jobb oldal – mp/);
 const sideInputs=side.locator('.tp3-side input');assert.equal(await sideInputs.count(),2);assert.equal(await sideInputs.nth(0).inputValue(),'20');assert.equal(await sideInputs.nth(1).inputValue(),'20','legacy mp/oldal must map compatibly to both side fields');
 await sideInputs.nth(0).fill('23');await sideInputs.nth(1).fill('19');
 await page.locator('.tp3-save-actions .btn').first().click();await page.waitForTimeout(80);
 const notice=page.locator('#tp2628Dialog');if(await notice.count())await notice.locator('[data-tp2628-ok]').click();
 const persisted=await page.evaluate(()=>{const e=history()[0].exercises.find(x=>x.id==='side-plank'),s=e.sets[0];return {left:s.leftSeconds,right:s.rightSeconds,reps:s.reps}});
 assert.deepEqual(persisted,{left:23,right:19,reps:'23'},'bilateral journal values must save separately while preserving legacy reps compatibility');

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
 assert.deepEqual(errors,[],'page errors: '+errors.join('\n'));
 console.log('PASS Phase 3 browser: combined Journal summary/editor with performed-set stats, compact photos, bilateral side edit/save, inline Statistics + Back, unified reference chevrons.');
}finally{await browser?.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});