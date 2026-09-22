const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
 await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished);

 // #22 Home Coach: recommendation + readiness only, no duplicated target/program row.
 await page.evaluate(()=>{db.set('language','hu');state.session=null;go('home')});await page.waitForTimeout(100);
 const card=page.locator('#rf220CoachCard.tp166-home-coach');
 assert.equal(await card.count(),1,'Home Coach card missing');
 assert.equal(await card.locator('.tp151-coach-target>span').count(),1,'Home Coach must keep only readiness metadata');
 assert.equal(await card.locator('.tp166-home-coach-sub').count(),0,'Home Coach must not repeat program/due text');
 assert.equal(await card.evaluate(el=>el.scrollWidth>el.clientWidth+1),false,'Home Coach must fit mobile width');

 // #23 Health: no duplicate Coach card, health data remains.
 await page.evaluate(()=>go('health'));await page.waitForTimeout(100);
 assert.equal(await page.locator('#rf235HealthCoach,.rf235-health-coach').count(),0,'Health must not render Coach card');
 assert.ok(await page.locator('main.rf263-health,main.tp151-health').count(),'Health page must still render');
 assert.ok(await page.locator('main .stat').count()>=2,'Health data cards must remain');

 // #24 Journal: no 1970 fallback; recover finished date when possible, neutral label otherwise.
 const result=await page.evaluate(()=>{
   const valid='2026-09-21T18:30:00.000Z';
   const a=tp165SafeHistoryRecord({workout:'A',started:'1970-01-01T00:00:00.000Z',exercises:[]},0);
   const b=tp165SafeHistoryRecord({workout:'B',finished:valid,exercises:[]},1);
   const c=tp165SafeHistoryRecord({workout:'A',exercises:[]},2);
   return {aStarted:a.started,bStarted:b.started,cStarted:c.started,bText:fmtDate(b.started),cText:fmtDate(c.started),aText:fmtDate(a.started)};
 });
 assert.equal(result.aStarted,'','Epoch input must be rejected');
 assert.match(result.bStarted,/^2026-09-21T18:30:00/,'finished date must recover missing started');
 assert.equal(result.cStarted,'','missing date must stay missing');
 assert.doesNotMatch(result.aText,/1970/);
 assert.doesNotMatch(result.cText,/1970/);
 assert.match(result.aText,/Dátum nem elérhető/);
 assert.match(result.cText,/Dátum nem elérhető/);
 assert.match(result.bText,/2026/);

 await page.evaluate(()=>{
   db.set('history',[
    {id:'epoch-row',workout:'A',started:'1970-01-01T00:00:00.000Z',exercises:[]},
    {id:'missing-row',workout:'B',exercises:[]},
    {id:'recover-row',workout:'A',finished:'2026-09-21T18:30:00.000Z',exercises:[]}
   ]);
   go('history');
 });await page.waitForTimeout(100);
 const journalText=await page.locator('#app').innerText();
 assert.doesNotMatch(journalText,/1970\.\s*01\.\s*01|1970-01-01/,'Journal UI must never show epoch fallback');
 assert.match(journalText,/Dátum nem elérhető/,'Journal must label missing dates neutrally');

 await page.setViewportSize({width:320,height:740});await page.evaluate(()=>go('home'));await page.waitForTimeout(80);
 assert.equal(await page.locator('#rf220CoachCard').evaluate(el=>el.scrollWidth>el.clientWidth+1),false,'#22 must fit 320px');
 console.log('PASS: 1.6.7 #22 compact Home Coach, #23 Health Coach removal, #24 safe Journal dates.');
}finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
