const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
(async()=>{
 const root=path.resolve('www');const server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox']});const page=await browser.newPage({viewport:{width:393,height:720},locale:'hu-HU'});const nativeDialogs=[];page.on('dialog',async d=>{nativeDialogs.push({type:d.type(),message:d.message()});await d.dismiss()});
  await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilotUiHotfix?.version==='2630b');await page.waitForTimeout(120);
  assert.equal(await page.evaluate(()=>window.TrainPilotUiUnify?.version),'2630-main');
  assert.equal(await page.evaluate(()=>window.TrainPilotUiHotfix?.calendarActionsRestored),true);
  assert.equal(await page.evaluate(()=>makeBackup().appVersion),'1.4.6');

  await page.evaluate(()=>{db.set('language','hu');db.set('activeProgramId','home-basic');state.session=null;state.tab='plan';render()});await page.waitForTimeout(60);
  const day=page.locator('.tp146-day-head').first();
  const dayTitle=(await day.locator('.tp146-day-title').innerText()).replace(/\s+/g,' ');assert.match(dayTitle,/Otthoni A\/B\s*[–-]\s*Alap.*A/);
  assert.equal(await day.locator('.tp146-day-title .tp146-day-letter').count(),1,'A/B marker must be inside the workout title');
  assert.equal(await day.locator(':scope > .tp146-day-letter').count(),0,'A/B marker must not be duplicated at the far edge');
  assert.equal((await day.locator('.tp146-day-letter').innerText()).trim(),'A');
  await page.evaluate(()=>{db.set('language','en');state.tab='plan';render()});await page.waitForTimeout(40);
  const enDayTitle=(await page.locator('.tp146-day-title').first().innerText()).replace(/\s+/g,' ');assert.match(enDayTitle,/Home A\/B - Basic - A/);assert.ok(!/Homei/.test(enDayTitle),'English title must not partially translate Otthoni');
  await page.evaluate(()=>{db.set('language','hu');state.tab='plan';render()});await page.waitForTimeout(40);
  const ex=page.locator('details.tp146-exercise').first();assert.equal(await ex.count(),1);await ex.locator('summary').click();assert.notEqual(await ex.getAttribute('open'),null,'exercise must expand in place');

  await page.evaluate(()=>{const p=activeProgram(),d=p.days[0],id=crypto.randomUUID(),st=new Date(Date.now()+86400000),en=new Date(st.getTime()+45*60000);db.set('scheduled',[{id,programId:p.id,dayId:d.id,workout:d.id,start:st.toISOString(),end:en.toISOString(),updatedAt:Date.now(),cancelled:false,status:'planned'}]);state.tab='home';render();return id});await page.waitForTimeout(80);
  const next=page.locator('.tp146-next-title');assert.equal(await next.count(),1);assert.match((await next.innerText()).replace(/\s+/g,' '),/Otthoni A\/B\s*[–-]\s*Alap.*A/);assert.ok(parseFloat(await next.evaluate(e=>getComputedStyle(e).fontSize))>=17,'next workout title must be emphasized');

  await page.evaluate(()=>{go('calendar');state.rf2211CalendarDate=localDateKey(new Date(scheduled()[0].start));render()});await page.waitForTimeout(80);
  assert.equal(await page.locator('.tp151-calendar-frame').count(),1,'calendar must use the unified framed layout');
  assert.equal(await page.locator('.tp151-day-panel').count(),1,'selected day details must render directly below the calendar');
  assert.equal(await page.locator('.tp146-schedule-card').count(),0,'separate legacy planned-workout cards must not be rendered');
  const card=page.locator('.tp151-calendar-item').first();assert.equal(await card.count(),1);assert.match((await card.innerText()).replace(/\s+/g,' '),/Otthoni A\/B\s*[–-]\s*Alap.*A/);
  const actions=await card.locator('.tp151-action-row button').allInnerTexts();
  for(const label of ['Indítás','Módosítás','Áthelyezés','Elvégezve','Kihagyás','Törlés'])assert.ok(actions.includes(label),'selected-day action missing: '+label);
  assert.ok((await card.locator('button.danger').innerText()).includes('Törlés'),'delete keeps danger styling');
  assert.ok((await card.evaluate(e=>e.getBoundingClientRect().height))<240,'expanded selected-day card must stay compact');

  await page.evaluate(()=>rf263HealthHub());await page.waitForTimeout(80);
  assert.equal(await page.locator('.tp151-health-card').count(),1,'Health must keep one Today card after Body/Fitness is embedded in the weight journal');
  assert.equal(await page.locator('.tp151-health > .hero').count(),0,'Health must not restore the oversized explanatory hero');
  assert.equal(await page.locator('#rf235HealthCoach,.rf235-health-coach,.tp151-health-coach').count(),0,'#23 Health must not render a duplicate Coach card');
  const bottomPanels=page.locator('main.rf263-health > details.tp155-health-bottom-panel');assert.equal(await bottomPanels.count(),2,'Health bottom area must keep only More Health and Health Connect after Pulse is embedded');
  const bottomGeometry=await bottomPanels.evaluateAll(es=>es.map(e=>{const r=e.getBoundingClientRect(),s=getComputedStyle(e);return {x:r.x,w:r.width,paddingLeft:s.paddingLeft,paddingRight:s.paddingRight}}));
  assert.ok(bottomGeometry.every(x=>Math.abs(x.x-bottomGeometry[0].x)<1),'Health bottom panels must start at the same left edge: '+JSON.stringify(bottomGeometry));
  assert.ok(bottomGeometry.every(x=>Math.abs(x.w-bottomGeometry[0].w)<1),'Health bottom panels must have the same width: '+JSON.stringify(bottomGeometry));
  assert.ok(bottomGeometry.every(x=>x.paddingLeft===bottomGeometry[0].paddingLeft&&x.paddingRight===bottomGeometry[0].paddingRight),'Health bottom panels must use the same inner padding');

  const photo=await page.evaluate(()=>{const now=new Date(),start=new Date(now.getTime()-30*60000);const row={id:'tp146-photo-workout',workout:'A',dayId:'A',programId:'home-basic',programName:'Otthoni A/B – Alap',started:start.toISOString(),finished:now.toISOString(),exercises:[],photos:[{id:'tp146-photo',label:'after',createdAt:now.toISOString(),updatedAt:Date.now(),driveFileId:'drive-test'}]};db.set('history',[row]);return {key:rf142WorkoutKey(row),id:'tp146-photo'} });
  await page.evaluate(x=>{void rf130DeletePhoto(x.key,x.id)},photo);const modal=page.locator('#tp2628Dialog');await modal.waitFor({state:'visible'});assert.match(await modal.innerText(),/Naplófotó törlése/);await modal.locator('[data-tp2628-cancel]').click();await modal.waitFor({state:'detached'});assert.equal(await page.evaluate(x=>!!rf130FindPhoto(x.key,x.id).p?.deletedAt,photo),false,'cancel must preserve photo');
  await page.evaluate(x=>{void rf130DeletePhoto(x.key,x.id)},photo);await modal.waitFor({state:'visible'});await modal.locator('[data-tp2628-confirm]').click();await page.waitForFunction(x=>!!rf130FindPhoto(x.key,x.id).p?.deletedAt,photo);

  const suspects=await page.evaluate(()=>Object.getOwnPropertyNames(window).flatMap(name=>{if(name==='confirm'||name==='prompt')return [];let fn;try{fn=window[name]}catch(_){return []}if(typeof fn!=='function')return [];let src='';try{src=Function.prototype.toString.call(fn)}catch(_){return []}return /\b(?:confirm|prompt)\s*\(/.test(src)?[name]:[]}));
  assert.deepEqual(suspects,[],'no active global app function may call native confirm()/prompt() directly');
  assert.deepEqual(nativeDialogs,[],'audited UI paths must not open native WebView dialogs');
  console.log('PASS: 1.4.6 compact UI + restored calendar actions + inline A/B marker + WebView dialog audit.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
