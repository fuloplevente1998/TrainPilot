'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
 page.on('pageerror',e=>errors.push(String(e.message||e)));page.on('dialog',d=>d.accept());
 await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
 await page.evaluate(()=>{
  db.set('language','hu');state.session=null;state.workout=null;db.set('draft',null);
  const at=(days,hour)=>{const d=new Date();d.setHours(hour,0,0,0);d.setDate(d.getDate()+days);return d.toISOString()};
  const w=(id,days,exercises)=>({id,workout:'A',dayId:'A',programId:'home-basic',programName:'Progress teszt',started:at(days,17),finished:at(days,18),exercises,photos:[]});
  const press=(kg,reps)=>({id:'db-floor-press',repUnit:'ism.',sets:[{set:1,weight:kg,reps:String(reps),done:true}]});
  const side=(l,r)=>({id:'side-plank',repUnit:'mp/oldal',sets:[{set:1,leftSeconds:l,rightSeconds:r,reps:String(Math.min(l,r)),done:true}]});
  db.set('history',[w('tp177-1',-1,[side(35,32)]),w('tp177-2',-2,[press(12,12)]),w('tp177-3',-5,[press(12,10)]),w('tp177-base',-10,[press(10,10)])]);
  go('history');
 });
 await page.waitForSelector('.tp155-journal-tabs');
 assert.equal(await page.locator('.tp155-journal-tabs button').count(),3,'Journal must have Log / Statistics / Progress tabs');
 assert.match((await page.locator('.tp155-journal-tabs').innerText()).replace(/\s+/g,' '),/Edzésnapló.*Statisztikák.*Fejlődés/);
 await page.getByRole('tab',{name:'Fejlődés'}).click();await page.waitForSelector('main.tp177-progress');
 assert.equal(await page.locator('.tp177-metric').count(),4,'reference layout keeps four summary cards');
 assert.equal(await page.locator('.tp177-muscle-row').count(),6,'six main muscle group rows expected');
 const text=(await page.locator('main.tp177-progress').innerText()).replace(/\s+/g,' ');
 assert.doesNotMatch(text,/Átlag.?intenzitás|8[,.]4\s*\/\s*10/i,'no invented workout intensity score');
 assert.match(text,/Átlagos volumen/);
 assert.equal(await page.locator('.tp177-chart-column').count(),13,'3 month view uses weekly vertical columns');
 const vertical=await page.evaluate(()=>{
  const cols=document.querySelector('.tp177-chart-columns'),positive=[...document.querySelectorAll('.tp177-chart-column')].find(x=>parseFloat(x.querySelector('.tp177-bar')?.style.height||'0')>0);
  const rail=positive?.querySelector('.tp177-bar-rail'),bar=positive?.querySelector('.tp177-bar');
  if(!cols||!positive||!rail||!bar)return null;
  const r=rail.getBoundingClientRect(),b=bar.getBoundingClientRect();
  return {display:getComputedStyle(cols).display,align:getComputedStyle(rail).alignItems,railH:r.height,barH:b.height,barW:b.width};
 });
 assert.ok(vertical,'at least one real volume bar expected');
 assert.equal(vertical.display,'grid');assert.equal(vertical.align,'flex-end');
 assert.ok(vertical.railH>100&&vertical.barH>0&&vertical.barW<=40,'volume must render as vertical columns: '+JSON.stringify(vertical));
 for(const [period,count] of [['7d',7],['30d',5],['3m',13]]){
  await page.locator('.tp177-periods button[data-period="'+period+'"]').click();
  await page.waitForFunction(n=>document.querySelectorAll('.tp177-chart-column').length===n,count);
  assert.equal(await page.locator('.tp177-periods button[data-period="'+period+'"]').getAttribute('aria-pressed'),'true');
 }
 await page.locator('.tp177-periods button[data-period="7d"]').click();await page.waitForFunction(()=>document.querySelectorAll('.tp177-chart-column').length===7);
 await page.evaluate(()=>{const b=[...document.querySelectorAll('.tp177-chart-column')].find(x=>parseFloat(x.querySelector('.tp177-bar')?.style.height||'0')>0);b?.click()});
 await page.waitForSelector('#tp177Dialog .tp177-dialog-card');assert.match(await page.locator('#tp177Dialog').innerText(),/kg/);
 await page.locator('.tp177-dialog-close').click();await page.waitForSelector('#tp177Dialog',{state:'detached'});
 await page.locator('.tp177-metric').nth(1).click();await page.waitForSelector('#tp177StatsFocus');
 assert.match(await page.locator('#tp177StatsFocus').innerText(),/Edzések száma/);
 assert.equal(await page.locator('.tp155-journal-tabs button').count(),3);
 assert.equal(await page.getByRole('tab',{name:'Statisztikák'}).getAttribute('aria-selected'),'true');
 await page.getByRole('tab',{name:'Fejlődés'}).click();await page.waitForSelector('main.tp177-progress');
 for(const width of [320,393,412]){await page.setViewportSize({width,height:873});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+1),true,'Progress must not overflow body at '+width+'px')}
 assert.deepEqual(errors,[],'page errors: '+errors.join('\n'));
 console.log('PASS #60 Progress UI: 3 Journal tabs, vertical bars, period filters, four cards, drilldowns, Stats handoff, mobile widths and no invented intensity.');
}finally{await browser?.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});