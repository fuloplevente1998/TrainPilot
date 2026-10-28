'use strict';
// PR #271 P3a: real Journal and Progress in the opt-in visual shell.
// No production data, mocked stat source or new chart engine. The existing
// history rendering, period calculations and muscle illustrations remain.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const u=new URL(req.url,'http://localhost'),file=path.resolve(root,'.'+(u.pathname==='/'?'/index.html':u.pathname));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
 fs.readFile(file,(e,b)=>{if(e){res.writeHead(404);res.end();return}
 res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(b)});
});
(async()=>{
 await new Promise(ok=>server.listen(0,'127.0.0.1',ok));
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  for(const width of [320,393]){
   const page=await browser.newPage({viewport:{width,height:width===320?740:873},locale:'hu-HU',timezoneId:'Europe/Budapest'});
   const errors=[];page.on('pageerror',e=>errors.push(e.message||String(e)));
   page.on('dialog',dialog=>dialog.accept());
   await page.addInitScript(()=>{try{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'))}catch(_){}});
   const url='http://127.0.0.1:'+server.address().port+'/';
   await page.goto(url);
   await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
   await page.evaluate(()=>go('history'));
   assert.equal(await page.locator('#tp271Dock').count(),0,width+': default main unchanged');
   await page.goto(url+'?tp271=1');
   await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilotMockup271?.enabled);
   await page.evaluate(()=>{
     state.session=null;state.workout=null;db.set('draft',null);
     const at=(offset,hour)=>{
       const d=new Date();d.setHours(hour,0,0,0);d.setDate(d.getDate()+offset);
       return d.toISOString();
     };
     const lift=(weight,reps)=>({id:'db-floor-press',repUnit:'ism.',sets:[{set:1,weight,reps:String(reps),done:true}]});
     const row=(id,offset,weight)=>({id,workout:'A',dayId:'A',programId:'home-basic',
       programName:'Valódi tesztprogram',started:at(offset,17),finished:at(offset,18),
       exercises:[lift(weight,10)],photos:[]});
     db.set('history',[row('p271-journal-1',-1,12),row('p271-journal-2',-2,11),row('p271-journal-3',-5,10)]);
   });
   for(const theme of ['classicBlue','yellow']){
     await page.evaluate(t=>{rf200SetTheme(t);state.tp177JournalView='log';go('history')},theme);
     await page.waitForSelector('main.tp150-journal-compact .tp155-journal-filter');
     const journal=page.locator('main.tp150-journal-compact');
     assert.equal(await journal.locator('.tp177-journal-tabs button').count(),3,width+'/'+theme+': three real Journal destinations');
     assert.equal(await journal.locator('details.rf263-history').count(),3,width+'/'+theme+': actual saved workouts remain');
     const filtered=journal.locator('.tp155-journal-filter');
     assert.equal(await filtered.count(),1,width+'/'+theme+': canonical calendar survives');
     const first=journal.locator('details.rf263-history').first();
     assert.equal(await first.getAttribute('open'),null,width+'/'+theme+': real workouts start compact');
     await first.locator(':scope > summary').click();
     assert.equal(await first.locator('.history-body').isVisible(),true,width+'/'+theme+': workout sets/details still expand');
     assert.ok(await first.locator('.history-body').innerText().then(t=>t.length>10),width+'/'+theme+': real workout detail, not empty card');
     await first.locator(':scope > summary').click();
     await filtered.locator(':scope > summary').click();
     assert.equal(await journal.locator('.tp155-journal-calendar').isVisible(),true,width+'/'+theme+': date filter opens');
     assert.equal(await journal.locator('.tp155-journal-day').count()>=28,true,width+'/'+theme+': actual month dates');
     const date=await journal.locator('.tp155-journal-day[aria-current="date"]').getAttribute('data-date');
     assert.ok(date,width+'/'+theme+': real today is indicated');
     await journal.locator('.tp155-journal-day[aria-current="date"]').click();
     assert.equal(await page.evaluate(()=>rf263HistoryFilter.from===rf263HistoryFilter.to&&!!rf263HistoryFilter.from),true,width+'/'+theme+': date filter updates original range state');
     await page.evaluate(()=>{if(typeof window.rf263ClearHistoryFilter==='function')window.rf263ClearHistoryFilter();go('history')});
     await page.waitForSelector('main.tp150-journal-compact');
     const previewMetrics=await page.evaluate(()=>{
       const main=document.querySelector('#app main.tp150-journal-compact'),dock=document.getElementById('tp271Dock');
       document.scrollingElement.scrollTop=document.scrollingElement.scrollHeight;
       const last=main.querySelector('details.rf263-history:last-of-type');
       return {clearance:parseFloat(getComputedStyle(main).paddingBottom),
         lastBottom:last?.getBoundingClientRect().bottom,
         dockTop:dock.getBoundingClientRect().top,
         overflows:document.documentElement.scrollWidth>innerWidth+1};
     });
     assert.ok(previewMetrics.clearance>=80,width+'/'+theme+': Journal leaves bottom-dock scroll clearance');
     assert.equal(previewMetrics.overflows,false,width+'/'+theme+': no horizontal Journal overflow');

     await page.locator('#tp271Dock button[data-tp271-target="progress"]').click();
     await page.waitForSelector('main.tp177-progress .tp177-volume-panel');
     assert.equal(await page.evaluate(()=>state.tp177JournalView),'progress',width+'/'+theme+': canonical Journal Progress state');
     const progress=page.locator('main.tp177-progress');
     assert.equal(await progress.locator('.tp177-journal-tabs button').count(),3,width+'/'+theme+': three original tabs retained');
     assert.equal(await progress.locator('.tp177-metric').count(),4,width+'/'+theme+': four actual metrics retained');
     assert.equal(await progress.locator('.tp177-chart-axis').count(),1,width+'/'+theme+': original chart axis preserved');
     assert.equal(await progress.locator('.tp177-anatomy').count(),1,width+'/'+theme+': original muscle anatomy retained');
     assert.equal(await progress.locator('.tp177-coach').count(),1,width+'/'+theme+': actual Coach entry retained');
     const card=progress.locator('.tp177-metric[data-metric="workouts"]');
     await card.click();
     assert.equal(await card.getAttribute('aria-expanded'),'true',width+'/'+theme+': original metric disclosure');
     assert.equal(await progress.locator('#tp177MetricDetails').isVisible(),true,width+'/'+theme+': real detailed view opens');
     await card.click();
     assert.equal(await card.getAttribute('aria-expanded'),'false',width+'/'+theme+': same detail can close');
     const chart=progress.locator('.tp177-volume-toggle');
     await chart.click();
     assert.equal(await chart.getAttribute('aria-expanded'),'true',width+'/'+theme+': volume expands original graph');
     const seven=progress.locator('.tp177-periods button[data-period="7d"]').first();
     await seven.click();
     assert.equal(await seven.getAttribute('aria-pressed'),'true',width+'/'+theme+': original seven-day period state');
     const boxes=await progress.evaluate(el=>{
       const dock=document.getElementById('tp271Dock'),metric=el.querySelector('.tp177-metric'),anatomy=el.querySelector('.tp177-anatomy');
       document.scrollingElement.scrollTop=document.scrollingElement.scrollHeight;
       return {columns:getComputedStyle(el.querySelector('.tp177-metric-grid')).gridTemplateColumns.split(' ').length,
         cardHeight:metric.getBoundingClientRect().height,
         lastPadding:parseFloat(getComputedStyle(el).paddingBottom),
         dockTop:dock.getBoundingClientRect().top,
         coachBottom:el.querySelector('.tp177-coach')?.getBoundingClientRect().bottom,
         anatomyWidth:anatomy.getBoundingClientRect().width,
         axisPosition:getComputedStyle(el.querySelector('.tp177-chart-axis')).position,
         overflow:document.documentElement.scrollWidth-innerWidth};
     });
     assert.equal(boxes.columns,2,width+'/'+theme+': Studio-like two-column real metric grid');
     assert.ok(boxes.cardHeight>=80,width+'/'+theme+': reachable cards '+JSON.stringify(boxes));
     assert.ok(boxes.coachBottom<=boxes.dockTop+5,width+'/'+theme+': final Coach action accessible above dock '+JSON.stringify(boxes));
     assert.ok(boxes.lastPadding>=80,width+'/'+theme+': scroll-safe footer clearance');
     assert.ok(boxes.anatomyWidth>0&&boxes.axisPosition==='sticky',width+'/'+theme+': original anatomy and sticky chart axis');
     assert.ok(boxes.overflow<=1,width+'/'+theme+': no horizontal Progress overflow '+JSON.stringify(boxes));
     if(width===393&&theme==='yellow'){
       fs.mkdirSync('ui-evidence',{recursive:true});
       await page.screenshot({path:'ui-evidence/pr271-progress-393.png'});
       await page.evaluate(()=>{state.tp177JournalView='log';go('history')});
       await page.screenshot({path:'ui-evidence/pr271-journal-393.png'});
     }
   }
   assert.deepEqual(errors,[],width+': no runtime errors');
   await page.close();
  }
  console.log('PASS PR271 P3a: real Journal history/date and Progress volume, periods, four metrics, anatomy and Coach, 320/393px & two themes');
 }finally{await browser?.close();await new Promise(ok=>server.close(ok))}
})().catch(e=>{console.error(e);process.exitCode=1});
