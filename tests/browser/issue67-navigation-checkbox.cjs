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
  await page.goto('http://127.0.0.1:'+server.address().port+'/');
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilot67?.twoJournalTabs);
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
  assert.equal(await page.locator('.tp155-journal-tabs button').count(),2,'Journal exposes only Workout Log and Progress');
  assert.match((await page.locator('.tp155-journal-tabs').innerText()).replace(/\s+/g,' '),/Edzésnapló.*Fejlődés/);
  await page.getByRole('tab',{name:'Fejlődés'}).click();await page.waitForSelector('main.tp177-progress');
  assert.equal(await page.locator('.tp155-journal-tabs button').count(),2,'Progress also uses two tabs');
  await page.locator('.tp177-periods button[data-period="1d"]').click();
  await page.locator('.tp177-metric[data-metric="pr"]').click();
  await page.waitForSelector('#tp177MetricDetails .tp177-inline-record');
  assert.match(await page.locator('#tp177MetricDetails').innerText(),/Teljes rekordlista/);
  assert.equal(await page.locator('#tp177MetricDetails .tp177-inline-record').count()>0,true,'PR card shows the full history even when today has no PR');

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
  console.log('PASS #67: two Journal tabs, Coach deep link, all-time PR list, themed Gear/Exclusion checkboxes.');
 }finally{if(browser)await browser.close();server.close()}
})().catch(err=>{console.error(err);process.exit(1)});
