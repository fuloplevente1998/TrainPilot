const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({headless:true,args:['--no-sandbox']});const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
 await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
 const calls=await page.evaluate(()=>{
  const rows=[];for(let i=0;i<120;i++){const end=new Date(Date.now()-i*86400000),start=new Date(end.getTime()-3600000);rows.push({id:'p7-lazy-'+i,workout:'A',dayId:'A',programId:'home-basic',programName:'Phase 7 lazy',started:start.toISOString(),finished:end.toISOString(),exercises:[{id:'db-squat',loadType:'per_hand',repUnit:'ism.',sets:[{set:1,weight:10,reps:'10',done:true},{set:2,weight:10,reps:'9',done:true}]}],photos:[]})}
  db.set('history',rows);state.session=null;let n=0;const base=window.history;window.history=function(){n++;return base.apply(this,arguments)};go('history');return n;
 });
 await page.waitForSelector('details.rf263-history');
 assert.equal(await page.locator('details.rf263-history').count(),120);
 assert.equal(await page.locator('.tp3-combined-history-body[data-tp3-hydrated="true"]').count(),0,'collapsed Journal rows must not eagerly hydrate detail bodies');
 assert.equal(await page.locator('.tp3-workout-summary').count(),0,'collapsed Journal rows must not build workout summaries');
 assert.ok(calls<20,'collapsed Journal render must not normalize full history once per row; calls='+calls);
 const first=page.locator('details.rf263-history').first();await first.locator(':scope > summary').click();
 await page.waitForSelector('details.rf263-history[open] .tp3-workout-summary');
 assert.equal(await page.locator('.tp3-combined-history-body[data-tp3-hydrated="true"]').count(),1,'only opened workout should hydrate');
 assert.equal(await first.locator('.rf-history-health-panel').count(),1,'Health panel must appear after hydration');
 assert.equal(await first.locator('.tp3-photo-section').count(),1,'Workout photos must appear after hydration');
 const marker=await first.evaluate(el=>{el.dataset.tp3NoRenderProbe='kept';return el.dataset.tp3NoRenderProbe});assert.equal(marker,'kept');
 await first.locator('details.tp3-history-ex-item>summary').first().click();
 await page.waitForSelector('details.tp3-history-ex-item[open] .tp3-exercise-inline-editor');
 assert.equal(await first.getAttribute('data-tp3-no-render-probe'),'kept','exercise disclosure must remain local and avoid whole Journal re-render');
 await page.evaluate(()=>{const now=new Date(),h={id:'p7-empty',workout:'A',dayId:'A',programId:'home-basic',programName:'Phase 7 empty',started:now.toISOString(),finished:new Date(now.getTime()+1800000).toISOString(),exercises:[],photos:[]};db.set('history',[h]);state.tp3HistoryOpenKey=null;go('history')});
 const empty=page.locator('[data-tp152-history-key="p7-empty"]');await empty.locator(':scope > summary').click();
 assert.equal(await empty.locator('.tp155-history-delete').count(),1,'opening a lazy Journal row must synchronously expose the accepted delete-workout action');
 assert.equal(await empty.locator('.tp3-combined-history-body').getAttribute('data-tp3-hydrated'),'true','empty legacy-compatible workout must hydrate on opening click');
 console.log('PASS Phase 7 Journal lazy hydration and accepted local editor behavior');
}finally{await browser?.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
