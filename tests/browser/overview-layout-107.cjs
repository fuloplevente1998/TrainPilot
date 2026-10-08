'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');const server=http.createServer((q,r)=>{const file=path.resolve(root,'.'+(new URL(q.url,'http://local').pathname==='/'?'/index.html':new URL(q.url,'http://local').pathname));if(!file.startsWith(root+path.sep)){r.writeHead(403);return r.end();}fs.readFile(file,(e,d)=>{if(e){r.writeHead(404);return r.end();}r.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');r.end(d);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let b;try{
 b=await chromium.launch({args:['--no-sandbox']});const p=await b.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];p.on('pageerror',e=>errors.push(String(e)));p.on('dialog',d=>d.accept());
 fs.mkdirSync('ui-evidence',{recursive:true});
 await p.addInitScript(()=>{try{localStorage.setItem("repforge:onboarding128",JSON.stringify("skipped"))}catch(_){}});await p.goto('http://127.0.0.1:'+server.address().port);await p.waitForFunction(()=>window.TrainPilotBoot?.finished);

 await p.evaluate(()=>{
  db.set('language','hu');const now=new Date(),days={};for(let i=6;i>=0;i--){const d=new Date(now);d.setDate(d.getDate()-i);days[rf240DayKey(d)]={averageHeartRate:88-i*3,restingHeartRate:58,maxHeartRate:109,minHeartRate:65,steps:1280,sleepMinutes:357};}
  rf240Ledger=()=>({days,lastSyncAt:new Date().toISOString()});db.set('weights',[{kg:66.5,date:now.toISOString()},{kg:66,date:new Date(Date.now()-86400000*5).toISOString()}]);
 });
 const rail=()=>p.evaluate(()=>{const main=document.querySelector('#app main.tp168-health'),card=main.querySelector('.tp168-today-card');return [main,card,...main.querySelectorAll(':scope>details,:scope>.tp168-sync-tail,:scope>.tp168-health-note')].map(e=>({c:e.className.replace(/tp107-fit-health/g,'').replace(/\s+/g,' ').trim(),x:e.getBoundingClientRect().x,width:e.getBoundingClientRect().width}));});
 for(const [width,height] of [[320,740],[360,800],[393,823],[393,873],[412,915]])for(const theme of ['classicBlue','yellow']){
  await p.setViewportSize({width,height});await p.evaluate(theme=>{go('home');rf200SetTheme(theme);go('health')},theme);await p.waitForTimeout(100);
  const normal=await rail();
  for(const selector of ['.tp169-pulse-summary','.tp168-weight-summary','.tp168-more-panel>summary','.tp168-connect-panel>summary']){
   await p.locator(selector).click();await p.waitForTimeout(60);assert.deepEqual(await rail(),normal,'Health keeps its card/column widths when '+selector+' opens at '+width+'/'+theme);
   assert.equal(await p.evaluate(()=>document.body.classList.contains('tp107-fixed-view')),false);
   await p.mouse.move(width/2,height-45);await p.mouse.wheel(0,500);await p.waitForTimeout(70);assert.ok(await p.evaluate(()=>scrollY)>0,'expanded Health can actually scroll '+selector);
   await p.locator(selector).click();await p.waitForTimeout(60);assert.deepEqual(await rail(),normal,'closing Health restores the same widths');assert.equal(await p.evaluate(()=>document.body.classList.contains('tp107-fixed-view')),true,'Closing details restores compact Health');assert.equal(await p.evaluate(()=>scrollY),0);
  }
  if(width===393&&height===823&&theme==='yellow')await p.screenshot({path:'ui-evidence/health-stable-width-107.png'});
  await p.evaluate(()=>go('home'));
  for(const month of ['2026-10','2026-08']){
   await p.evaluate(month=>{
    state.calendarMonth=month;state.rf2211CalendarDate='';
    const program=activeProgram();db.set('scheduled',Array.from({length:31},(_,i)=>{const start=new Date(month+'-'+String(i+1).padStart(2,'0')+'T08:00:00');return {id:'layout-'+month+'-'+i,programId:program.id,dayId:program.days[i%program.days.length].id,workout:program.days[i%program.days.length].id,start:start.toISOString(),end:new Date(start.getTime()+3000000).toISOString(),status:'planned',cancelled:false,updatedAt:Date.now()};}));
    go('calendar');
   },month);await p.waitForTimeout(150);
   assert.equal(await p.evaluate(()=>scheduled().length),31);assert.ok(await p.locator('.cal-cell.planned b').count()>=31,'planned day badges are rendered');
   const geometry=await p.evaluate(()=>{const panel=document.querySelector('#tp155R4PanelHost .tp155-r4-panel'),r=panel.getBoundingClientRect(),planner=panel.querySelector('.rf2211-planner').getBoundingClientRect(),main=panel.querySelector('main');return {nav:document.querySelector('.top').getBoundingClientRect().bottom,top:r.top,bottom:r.bottom,plannerBottom:planner.bottom,emptyTail:r.bottom-planner.bottom,overflow:panel.scrollHeight-panel.clientHeight,mainOverflow:main.scrollHeight-main.clientHeight,hit:document.elementFromPoint(r.x+r.width/2,r.y+1)?.classList.contains('tp155-r4-panel'),border:parseFloat(getComputedStyle(panel).borderTopWidth)};});
   assert.ok(geometry.top>=geometry.nav+1&&geometry.top<=geometry.nav+4&&geometry.hit&&geometry.border>=1,'Calendar top border is visible below navigation '+JSON.stringify(geometry));
   assert.ok(geometry.emptyTail<=20&&geometry.overflow<=1&&geometry.mainOverflow<=1&&geometry.bottom<=height,'Calendar frame fits its populated month/planner without an empty tail '+width+'/'+month+' '+JSON.stringify(geometry));
   for(const mode of ['weekly','custom','alternate']){
    await p.locator('#rf230Mode').locator('..').locator('.tp-select-trigger').click();await p.locator('#rf230Mode').locator('..').locator('.tp-select-menu [data-value="'+mode+'"]').click();await p.waitForTimeout(60);
    const fit=await p.evaluate(()=>{const panel=document.querySelector('#tp155R4PanelHost .tp155-r4-panel'),main=panel.querySelector('main'),planner=main.querySelector('.rf2211-planner');return {overflow:main.scrollHeight-main.clientHeight,panelBottom:panel.getBoundingClientRect().bottom,plannerBottom:planner.getBoundingClientRect().bottom};});
    assert.ok(fit.overflow<=1&&fit.plannerBottom<=fit.panelBottom&&fit.panelBottom<=height,'populated Calendar remains fixed in '+width+'/'+month+'/'+mode+' '+JSON.stringify(fit));
   }
   if(width===393&&height===823&&theme==='yellow'&&month==='2026-10')await p.screenshot({path:'ui-evidence/calendar-visible-frame-107.png'});
   for(const selector of ['.tp154-coach-action','.tp154-settings-action']){
    await p.locator(selector).click();await p.waitForTimeout(100);
    const panel=p.locator('#tp155R4PanelHost .tp155-r4-panel');assert.equal(await p.locator('#tp155R4PanelHost').evaluate(e=>e.classList.contains('tp107-fit-calendar')),false,'calendar sizing is cleared on panel navigation');
    assert.equal(await panel.evaluate(e=>getComputedStyle(e).overflowY),'auto');
    await panel.evaluate(e=>Promise.all(e.getAnimations({subtree:true}).filter(a=>a.effect.getComputedTiming().iterations!==Infinity).map(a=>a.finished.catch(()=>{}))));
    const scrollContext=await panel.evaluate(e=>({height:e.clientHeight,scroll:e.scrollHeight}));assert.ok(scrollContext.scroll>scrollContext.height,'scrollable panel '+width+'/'+height+'/'+theme+'/'+selector+' '+JSON.stringify(scrollContext));
    await panel.evaluate(e=>{e.scrollTop=0});await panel.hover();await p.mouse.wheel(0,500);
    await p.waitForFunction(()=>document.querySelector('#tp155R4PanelHost .tp155-r4-panel')?.scrollTop>0,null,{timeout:2000});
    assert.ok(await panel.evaluate(e=>e.scrollTop)>0,'Calendar -> '+selector+' actually scrolls '+width+'/'+height+'/'+theme);
    if(selector==='.tp154-coach-action'&&width===393&&height===823&&theme==='yellow')await p.screenshot({path:'ui-evidence/calendar-to-coach-scroll-107.png'});
   }
   await p.locator('.tp151-nav-item[onclick*="calendar"]').click();await p.waitForTimeout(70);assert.equal(await p.locator('#tp155R4PanelHost').evaluate(e=>e.classList.contains('tp107-fit-calendar')),true,'returning to Calendar restores only its own fixed layout');
   await p.evaluate(()=>tp155R4ClosePanel(false));
  }
 }
 assert.deepEqual(errors,[]);console.log('PASS overview feedback: visible content-sized Calendar frame with planned days, stable Health disclosure widths and real scrolling after Calendar -> Coach/Settings, phone sizes/two theme families');
}finally{await b?.close();await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exitCode=1;});
