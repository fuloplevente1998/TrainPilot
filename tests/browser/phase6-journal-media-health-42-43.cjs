const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
  fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(data)});
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  for(const width of [320,360,390,412]){
   const context=await browser.newContext({viewport:{width,height:844},timezoneId:'Europe/Budapest',locale:'hu-HU'});
   const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
   await page.goto(`http://127.0.0.1:${server.address().port}`);
   await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
   await page.evaluate(()=>{
    const row={id:'phase6-health-photo',workout:'A',programName:'Phase 6 teszt',started:'2026-09-24T10:00:00+02:00',finished:'2026-09-24T11:00:00+02:00',exercises:[],photos:[]};
    db.set('history',[row]);rfHistoryHealthCache.clear();
    window.__tp6HealthResolvers=[];
    healthPlugin=()=>({readWorkout:()=>new Promise(resolve=>window.__tp6HealthResolvers.push(resolve))});
    go('history');
   });
   const workout=page.locator('details.rf263-history').first();
   await workout.locator(':scope > summary').click();
   await page.waitForFunction(()=>window.__tp6HealthResolvers.length===1);
   assert.notEqual(await workout.getAttribute('open'),null,'workout must stay expanded during automatic Health read');
   const panel=workout.locator('.rf-history-health-panel'),health=panel.locator('[data-rf-history-health]'),refresh=panel.locator('.rf-history-health-refresh');
   assert.equal(await panel.locator('.rf-history-health-toggle').getAttribute('aria-expanded'),'true');
   assert.doesNotMatch(await health.innerText(),/Health Connect lekérdezés|Health Connect adatok lekérése/i,'inline Health must not flash loading copy');
   assert.equal(await refresh.getAttribute('aria-busy'),'true');
   await page.evaluate(()=>window.__tp6HealthResolvers[0]({activeCalories:111,averageHeartRate:123,source:'all'}));
   await page.waitForFunction(()=>document.querySelector('[data-rf-history-health]')?.innerText.includes('111'));
   assert.equal(await refresh.isDisabled(),false);
   const settle=()=>page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
   await settle();

   await page.evaluate(()=>{document.body.style.minHeight='2400px';window.scrollTo(0,Math.min(260,document.documentElement.scrollHeight-innerHeight))});
   await settle();
   const scrollBefore=await page.evaluate(()=>window.scrollY);
   await page.evaluate(()=>document.querySelector('.rf-history-health-refresh').click());
   await page.waitForFunction(()=>window.__tp6HealthResolvers.length===2);
   await settle();
   assert.match(await health.innerText(),/111/,'previous Health result must remain visible while refresh is running');
   assert.doesNotMatch(await health.innerText(),/Health Connect lekérdezés|Health Connect adatok lekérése/i);
   assert.notEqual(await workout.getAttribute('open'),null,'workout expansion must survive inline refresh');
   assert.equal(await panel.locator('.rf-history-health-toggle').getAttribute('aria-expanded'),'true','Health panel expansion must survive inline refresh');
   const scrollPending=await page.evaluate(()=>window.scrollY);
   assert.ok(Math.abs(scrollPending-scrollBefore)<=1,`inline refresh must not jump scroll before completion: before=${scrollBefore}, pending=${scrollPending}`);
   await page.evaluate(()=>window.__tp6HealthResolvers[1]({activeCalories:222,averageHeartRate:129,source:'all'}));
   await page.waitForFunction(()=>document.querySelector('[data-rf-history-health]')?.innerText.includes('222'));
   await settle();
   const scrollAfter=await page.evaluate(()=>window.scrollY);
   assert.ok(Math.abs(scrollAfter-scrollBefore)<=1,`inline refresh must not jump scroll after completion: before=${scrollBefore}, after=${scrollAfter}`);

   await page.evaluate(()=>{
    rf200SetTheme('classicGreen');go('history');
    isNative=()=>true;
    rf130PhotoPlugin=()=>({pick:async()=>({cancelled:true}),capture:async()=>({cancelled:true})});
    rf130OpenAddPhoto(rf142WorkoutKey(history()[0]));
   });
   const modal=page.locator('#rf130PhotoModal'),dialog=modal.locator('.tp6-photo-dialog'),close=dialog.locator('.tp6-photo-close');
   await modal.waitFor({state:'visible'});
   assert.equal(await close.innerText(),'×','old left-side Bezárás text must be replaced by a compact X');
   assert.equal(await close.getAttribute('aria-label'),'Bezárás');
   assert.equal(await dialog.locator('[data-rf130-label]').count(),3,'all photo labels must remain');
   assert.equal(await dialog.locator('[data-rf130-source]').count(),2,'camera and picker choices must remain');
   assert.equal(await dialog.locator('[data-rf130-label="after"]').getAttribute('aria-pressed'),'true','default After label must remain selected');
   const layout=await dialog.evaluate(d=>{
    const dr=d.getBoundingClientRect(),c=d.querySelector('.tp6-photo-close'),cr=c.getBoundingClientRect(),cs=getComputedStyle(c),ds=getComputedStyle(d);
    return {left:dr.left,right:dr.right,width:dr.width,scrollWidth:d.scrollWidth,clientWidth:d.clientWidth,closeLeft:cr.left,closeRight:cr.right,closeBg:cs.backgroundColor,shadow:ds.boxShadow,family:document.documentElement.dataset.tpThemeFamily};
   });
   assert.ok(layout.left>=0&&layout.right<=width+1&&layout.scrollWidth<=layout.clientWidth,'photo modal overflow '+JSON.stringify({width,layout}));
   assert.ok(layout.closeLeft>layout.left+layout.width/2&&layout.closeRight<=layout.right+1,'red X must be right-aligned');
   assert.notEqual(layout.closeBg,'rgba(0, 0, 0, 0)','red X must have visible background');
   assert.equal(layout.family,'basic');assert.equal(layout.shadow,'none','basic theme photo modal must remain matte');
   await dialog.locator('[data-rf130-source="pick"]').click();
   assert.match(await dialog.locator('[data-rf130-status]').innerText(),/Művelet megszakítva/i,'true picker cancellation must remain visible');
   await close.click();assert.equal(await modal.count(),0,'top-right X must close the photo modal');

   assert.deepEqual(errors,[]);
   console.log(`PASS Phase 6 Journal/media/Health UI ${width}px`);
   await context.close();
  }
 }finally{if(browser)await browser.close();await new Promise(resolve=>server.close(resolve))}
})().catch(e=>{console.error(e);process.exitCode=1});
