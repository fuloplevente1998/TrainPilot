'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';
 const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}
 fs.readFile(file,(err,data)=>{if(err){res.writeHead(404);return res.end()}
  res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(data);
 });
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 let browser;try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
  page.on('pageerror',err=>errors.push(String(err.message||err)));page.on('dialog',dialog=>dialog.accept());
  await page.addInitScript(()=>{try{localStorage.setItem("repforge:onboarding128",JSON.stringify("skipped"))}catch(_){}});await page.goto('http://127.0.0.1:'+server.address().port+'/');
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilot67?.threeJournalTabs);
  await page.evaluate(()=>{
   db.set('language','hu');state.session=null;state.workout=null;db.set('draft',null);
   const date=days=>{const d=new Date();d.setDate(d.getDate()+days);d.setHours(16,0,0,0);return d.toISOString()};
   const workout=(id,days,weight)=>({id,workout:'A',dayId:'A',programId:'home-basic',started:date(days),finished:date(days),exercises:[
    {id:'db-floor-press',repUnit:'ism.',sets:[{set:1,weight,reps:'10',done:true}]}
   ]});
   db.set('history',[workout('issue67-old',-35,10),workout('issue67-new',-9,14)]);
   go('history');
  });
  await page.waitForSelector('.tp155-journal-tabs');
  assert.equal(await page.locator('.tp155-journal-tabs button').count(),3,'Journal exposes Workout Log, Progress and Statistics');
  assert.match((await page.locator('.tp155-journal-tabs').innerText()).replace(/\s+/g,' '),/Edzésnapló.*Fejlődés/);
  await page.getByRole('tab',{name:'Fejlődés'}).click();await page.waitForSelector('main.tp177-progress');
  assert.equal(await page.locator('.tp155-journal-tabs button').count(),3,'Progress also uses three tabs');
  await page.locator('.tp177-periods button[data-period="1d"]').click();
  await page.locator('.tp177-metric[data-metric="pr"]').click();
  await page.waitForSelector('#tp177MetricDetails .tp177-inline-record');
  assert.match(await page.locator('#tp177MetricDetails').innerText(),/Teljes rekordlista/);
  assert.equal(await page.locator('#tp177MetricDetails .tp177-inline-record').count()>0,true,'PR card shows the full history even when today has no PR');
  // Full historic exercise statistics now live in the shared Journal destination.
  assert.equal(await page.locator('.tp67-full-stats-entry').count(),0,'separate Progress entry is replaced by the Statistics tab');
  await page.getByRole('tab',{name:'Statisztikák',exact:true}).click();
  const stats=page.locator('main.tp107-statistics');
  await stats.locator('.tp4-stat-row').first().waitFor();
  assert.equal(await page.evaluate(()=>state.tab),'history');
  assert.equal(await page.evaluate(()=>state.tp177JournalView),'stats');
  assert.equal(await stats.locator('.tp4-stat-row').count(),1,'all historic exercise statistics remain available');
  assert.equal(await page.locator('#tp155R4PanelHost').count(),0,'statistics do not require another popup');
  await stats.locator('.tp4-stat-row summary').first().click();
  assert.equal(await stats.locator('.tp4-stat-row[open] .stat').count(),4,'load, 1RM, best set and volume remain available');
  assert.equal(await page.evaluate(()=>window.TrainPilotAndroidBack()),true,'Android Back collapses expanded statistics first');
  assert.equal(await stats.locator('.tp4-stat-row[open]').count(),0);
  await page.setViewportSize({width:320,height:740});
  assert.equal(await stats.evaluate(el=>el.scrollWidth<=el.clientWidth+1),true,'Statistics fit the 320px phone width');
  await page.setViewportSize({width:393,height:873});
  await page.getByRole('tab',{name:'Fejlődés',exact:true}).click();

  await page.evaluate(()=>window.tp155R4OpenPanel('coach',document.activeElement));
  await page.waitForSelector('#tp155R4PanelHost[data-panel="coach"] .tp67-coach-progress');
  assert.equal(await page.locator('#tp155R4PanelHost main.tp151-coach').locator(':scope > .tp67-coach-progress').count(),1);
  assert.equal(await page.locator('#tp155R4PanelHost main.tp151-coach').locator(':scope > .card:last-child').count(),0,'Coach duplicate PR card is replaced with a single navigation button');
  await page.locator('#tp155R4PanelHost .tp67-coach-progress').click();
  await page.waitForSelector('main.tp177-progress');
  assert.equal(await page.locator('#tp155R4PanelHost').count(),0,'Coach closes before Progress navigation');
  assert.equal(await page.getByRole('tab',{name:'Fejlődés'}).getAttribute('aria-selected'),'true');

  await page.evaluate(()=>profileScreen());
  await page.waitForSelector('#tp3PlannerPanelHost input[name="pfGear"]',{state:'attached'});
  await page.evaluate(()=>{
   for(const name of ['pfGear','pfAvoidArea','pfExclude']){
    const input=document.querySelector('#tp3PlannerPanelHost input[name="'+name+'"]');
    for(let node=input;node;node=node.parentElement)if(node.tagName==='DETAILS')node.open=true;
   }
  });
  for(const name of ['pfGear','pfAvoidArea','pfExclude']){
   const input=page.locator('#tp3PlannerPanelHost input[name="'+name+'"]').first();
   assert.equal(await input.count(),1,name+' selector exists');
   await input.uncheck({force:true});
   const plain=await input.evaluate(el=>({appearance:getComputedStyle(el).appearance,input:getComputedStyle(el).backgroundColor,row:getComputedStyle(el.closest('label')).backgroundColor}));
   assert.equal(plain.appearance,'none',name+' uses the themed checkbox instead of the native white square');
   await input.check({force:true});
   assert.equal(await input.isChecked(),true,name+' remains operable');
   const selected=await input.evaluate(el=>({input:getComputedStyle(el).backgroundColor,row:getComputedStyle(el.closest('label')).backgroundColor}));
   assert.notEqual(selected.input,plain.input,name+' checked state changes checkbox color');
   assert.notEqual(selected.row,plain.row,name+' selection highlights the row');
   await input.uncheck({force:true});assert.equal(await input.isChecked(),false);
  }
  assert.deepEqual(errors,[],'no new browser exceptions');
  console.log('PASS #67: three Journal tabs, complete inline Statistics, Coach link, PR list and themed checkboxes.');
 }finally{if(browser)await browser.close();server.close()}
})().catch(err=>{console.error(err);process.exit(1)});
