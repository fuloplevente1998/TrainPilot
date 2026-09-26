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
  // Restore the complete legacy exercise statistics inside the shared floating panel.
  assert.equal(await page.locator('.tp67-full-stats-entry').count(),1,'Progress offers detailed exercise statistics');
  await page.locator('.tp67-full-stats-entry').click();
  const stats=page.locator('#tp155R4PanelHost[data-panel="stats"]');
  await stats.locator('.tp67-full-stats-panel .tp4-stat-row').first().waitFor();
  assert.equal(await page.evaluate(()=>state.tab),'history','Statistics popup must preserve Journal route');
  assert.equal(await page.evaluate(()=>state.tp177JournalView),'progress','Statistics popup must preserve the Progress tab');
  assert.equal(await page.locator('#app main.tp177-progress .tp155-journal-tabs button').count(),2,'the main Journal keeps two tabs');
  assert.equal(await stats.locator('.tp4-stat-row').count(),1,'all historic exercise statistics remain available');
  const redClose=stats.locator('.tp155-r4-panel-close');
  assert.equal(await redClose.isVisible(),true,'popup uses the shared global red X');
  const closeStyle=await redClose.evaluate(el=>{
   const box=el.getBoundingClientRect(),parent=el.closest('.tp155-r4-panel').getBoundingClientRect(),cs=getComputedStyle(el);
   return {background:cs.backgroundColor,width:box.width,height:box.height,right:parent.right-box.right,top:box.top-parent.top};
  });
  assert.equal(closeStyle.background,'rgb(71, 37, 41)','global red close background is unchanged');
  assert.ok(closeStyle.right>=0&&closeStyle.right<=24&&closeStyle.top>=0&&closeStyle.top<=38,'the red X remains in the top-right corner: '+JSON.stringify(closeStyle));
  assert.ok(Math.abs(closeStyle.width-closeStyle.height)<=1,'the shared global close control remains square');
  await stats.locator('.tp4-stat-row summary').first().click();
  assert.equal(await stats.locator('.tp4-stat-row[open] .stat').count(),4,'max load, estimated 1RM, best set and volume remain available');
  assert.equal(await page.evaluate(()=>window.TrainPilotAndroidBack()),true,'Android Back collapses expanded statistics first');
  assert.equal(await stats.locator('.tp4-stat-row[open]').count(),0);
  assert.equal(await page.evaluate(()=>window.TrainPilotAndroidBack()),true,'next Android Back closes the popup');
  assert.equal(await page.locator('#tp155R4PanelHost[data-panel="stats"]').count(),0);
  assert.equal(await page.locator('main.tp177-progress .tp67-full-stats-entry').count(),1,'closing returns to the same Progress page');
  await page.locator('.tp67-full-stats-entry').click();
  await stats.locator('.tp155-r4-panel-close').click();
  assert.equal(await page.locator('#tp155R4PanelHost').count(),0,'tapping global red X closes the popup');
  assert.equal(await page.locator('main.tp177-progress').count(),1);
  assert.equal(await page.locator('.tp67-full-stats-entry:focus').count(),1,'global close restores focus to the opener');
  await page.setViewportSize({width:320,height:740});
  await page.locator('.tp67-full-stats-entry').click();
  await stats.locator('.tp155-r4-panel-close').waitFor();
  assert.equal(await stats.evaluate(el=>el.scrollWidth<=el.clientWidth+1),true,'popup fits the 320px phone width');
  await stats.locator('.tp155-r4-panel-close').click();
  await page.setViewportSize({width:393,height:873});

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
  console.log('PASS #67: two Journal tabs, complete popup Statistics with global red X, Coach link, PR list and themed checkboxes.');
 }finally{if(browser)await browser.close();server.close()}
})().catch(err=>{console.error(err);process.exit(1)});
