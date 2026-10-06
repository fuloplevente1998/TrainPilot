'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((q,r)=>{
 const file=path.resolve(root,'.'+(new URL(q.url,'http://local').pathname==='/'?'/index.html':new URL(q.url,'http://local').pathname));
 if(!file.startsWith(root+path.sep)){r.writeHead(403);return r.end();}
 fs.readFile(file,(e,d)=>{if(e){r.writeHead(404);return r.end();}r.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');r.end(d);});
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({args:['--no-sandbox']});const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));page.on('dialog',d=>d.accept());fs.mkdirSync('ui-evidence',{recursive:true});
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(()=>{go('plan');startWorkout(activeProgram().days[0].id);state.session.exercises.forEach(e=>e.sets.forEach(s=>{s.done=true;s.reps='10';}));finishWorkout();});
  await page.waitForFunction(()=>!state.session&&history().length===1);
  const started=await page.evaluate(()=>history()[0].started),programsBefore=await page.evaluate(()=>JSON.stringify(programs()));
  assert.equal(await page.locator('#rf220CoachCard,#rf223Today,.tp120-home-goals').count(),0,'saved-workout feedback must not receive Home dashboard cards');
  assert.equal(await page.evaluate(()=>document.body.classList.contains('tp107-fixed-view')),false,'feedback must release the fixed Home viewport');
  for(const lang of ['hu','en','de','ro'])for(const [width,height,large] of [[393,873,false],[320,568,false],[873,393,false],[320,568,true]]){
   await page.setViewportSize({width,height});await page.evaluate(({lang,started})=>{db.set('language',lang);go('home');feedbackScreen(started);window.scrollTo(0,0);},{lang,started});
   const scale=await page.addStyleTag({content:large?'main.tp122-feedback :is(h1,h2,p,.btn){font-size:150%!important}':'/* default text size */'});
   const main=page.locator('#app main'),ratings=main.locator('button[onclick^="saveFeedback"]'),home=main.locator('button[onclick="go(\'home\')"]');
   assert.equal(await ratings.count(),4);assert.equal(await page.locator('#rf220CoachCard,#rf223Today,.tp120-home-goals').count(),0);
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,'feedback must not overflow horizontally '+lang+'/'+width+'/'+large);
   for(const button of await ratings.all())assert.ok((await button.boundingBox()).height>=40,'feedback touch targets stay usable');
   const initialBounds=await home.boundingBox(),bottom=initialBounds.y+initialBounds.height;
   if(width===393&&!large)assert.ok(bottom<=height,'normal phone feedback fits without scrolling '+lang+' '+bottom);
   if(bottom>height){await page.mouse.move(width/2,height-40);await page.mouse.wheel(0,2000);await page.waitForTimeout(60);assert.ok(await page.evaluate(()=>scrollY)>0,'overflowing feedback can really scroll');}
   await home.scrollIntoViewIfNeeded();const bounds=await home.boundingBox();assert.ok(bounds.y>=0&&bounds.y+bounds.height<=height+1,'Home remains reachable after scrolling');
   if(lang==='hu'&&width===393)await page.screenshot({path:'ui-evidence/post-workout-feedback-122.png'});
   if(lang==='de'&&large)await page.screenshot({path:'ui-evidence/post-workout-feedback-122-large-font.png'});
   await scale.evaluate(e=>e.remove());await home.click();assert.equal(await page.locator('main.rf221-home').count(),1,'return to Home restores its dashboard');
  }
  for(const rating of ['easy','right','hard','pain']){
   await page.evaluate(started=>feedbackScreen(started),started);await page.locator('button[onclick="saveFeedback(\''+rating+'\')"]').click();
   assert.equal(await page.evaluate(()=>history()[0].feedback.rating),rating,'rating is saved to the completed workout');
   assert.equal(await page.evaluate(()=>document.body.classList.contains('tp107-fixed-view')),false,'saved feedback also remains scrollable');
   const home=page.locator('#app main button[onclick="go(\'home\')"]');await home.scrollIntoViewIfNeeded();await home.click();
  }
  assert.equal(await page.evaluate(()=>JSON.stringify(programs())),programsBefore,'feedback does not automatically change program recipes');
  await page.reload();await page.waitForFunction(()=>window.TrainPilotBoot?.finished);assert.equal(await page.evaluate(()=>history()[0].feedback.rating),'pain','feedback survives restart');
  await page.evaluate(()=>{go('history');feedbackFromHistory(0);});assert.equal(await page.locator('main.tp122-feedback').count(),1,'history reopens the same compact feedback screen');
  await page.locator('#app main button[onclick="healthScreen()"]').click();assert.equal(await page.locator('main.tp122-feedback').count(),0,'Health action remains functional');
  assert.deepEqual(errors,[]);console.log('PASS #122: real workout completion, compact feedback, actual overflow scrolling, four languages, portrait/landscape/large text, saved ratings/restart and navigation');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
