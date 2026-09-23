const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
  fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(d)})
 });
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:790},locale:'hu-HU'});
  const errors=[];page.on('pageerror',e=>errors.push(String(e)));
  await page.goto('http://127.0.0.1:'+server.address().port);
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilotDemo15);
  assert.equal(await page.evaluate(()=>TrainPilotDemo15.version),'15');
  const id=await page.evaluate(()=>{
   db.set('language','hu');document.documentElement.lang='hu';
   state.session=null;state.workout=null;
   const program=activeProgram(),day=program.days[0];
   startWorkout(day.id,null,program.id);
   const index=state.session.exercises.findIndex(x=>demoInfo(x.id));
   if(index<0)throw Error('No demo-enabled workout exercise');
   state.current=index;renderWorkout();
   return state.session.exercises[index].id;
  });
  await page.waitForSelector('.tp15-demo-launch');
  const launch=page.locator('main.tp153-workout .tp15-demo-launch');
  assert.equal(await launch.count(),1,'one workout demo action is visible');
  assert.equal(await page.locator('main.tp153-workout .tp154-workout-guide').count(),0,'intermediate disclosure must be absent');
  assert.equal(await launch.innerText(),'Bemutató / leírás');
  const note=await page.evaluate(id=>{const e=byId(id);return typeof tp149ExerciseNote==='function'?tp149ExerciseNote(e):String(e.notes||'')},id);
  await launch.click();const modal=page.locator('#videoModal');await modal.waitFor({state:'visible'});
  assert.equal(await modal.locator('.video-player iframe').count(),1,'one-tap must show embedded video');
  assert.equal(await modal.locator('.tp15-demo-guide').count(),1,'same panel must include instructions');
  if(note)assert.ok((await modal.locator('.tp15-demo-guide p').innerText()).includes(note),'panel must include exercise-specific instructions');
  assert.equal(await modal.locator('.tp15-demo-header h2').count(),1,'title must sit beside close');
  const close=modal.locator('.tp15-demo-close');assert.equal(await close.count(),1);
  assert.equal(await close.getAttribute('aria-label'),'Bemutató bezárása');
  assert.equal(await modal.locator('.tp15-demo-external').count(),1,'one external video action');
  assert.equal(await modal.locator('.video-dialog > button').count(),1,'duplicate external navigation must be removed');
  assert.match(await modal.locator('.small.muted').innerText(),/Forrás:/);
  assert.doesNotMatch(await modal.innerText(),/Forrás és útmutató/,'second external action must be gone');
  const geometry=await modal.evaluate(el=>{
   const dialog=el.querySelector('.tp15-demo-dialog'),close=el.querySelector('.tp15-demo-close'),head=el.querySelector('.tp15-demo-header'),iframe=el.querySelector('.video-player iframe');
   const dr=dialog.getBoundingClientRect(),cr=close.getBoundingClientRect(),ir=iframe.getBoundingClientRect();
   return {width:dr.width,height:dr.height,closeWidth:cr.width,closeHeight:cr.height,closeX:cr.x,dialogRight:dr.right,iframeWidth:ir.width,iframeHeight:ir.height,shadow:getComputedStyle(dialog).boxShadow,sticky:getComputedStyle(head).position,closeColor:getComputedStyle(close).backgroundColor,overflow:dialog.scrollWidth-dialog.clientWidth,docOverflow:document.documentElement.scrollWidth-document.documentElement.clientWidth}
  });
  assert.equal(geometry.sticky,'sticky','video title and red close must remain sticky');
  assert.ok(geometry.closeWidth>=33&&geometry.closeHeight>=33,'close target must be comfortably tappable');
  assert.ok(geometry.closeX>geometry.dialogRight-65,'red close belongs at upper right');
  assert.equal(geometry.closeColor,'rgb(71, 37, 41)','demo X must use the shared TrainPilot dark-red close treatment');
  assert.ok(geometry.iframeHeight>130&&geometry.iframeWidth>260,'video must remain visible');
  assert.ok(geometry.overflow<=1&&geometry.docOverflow<=1,'393px panel must not overflow: '+JSON.stringify(geometry));
  await close.click();await modal.waitFor({state:'detached'});
  assert.equal(await launch.evaluate(el=>document.activeElement===el),true,'close must restore opener focus');
  await launch.click();await modal.waitFor({state:'visible'});
  assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true,'Android Back consumes open demo');
  assert.equal(await modal.count(),0,'Android Back closes demo before navigating');
  assert.equal(await page.locator('main.tp153-workout').count(),1,'closing leaves workout active');
  await launch.click();await modal.waitFor({state:'visible'});await page.keyboard.press('Escape');
  await modal.waitFor({state:'detached'});
  await page.setViewportSize({width:320,height:640});await launch.click();await modal.waitFor({state:'visible'});
  const compact=await modal.evaluate(el=>({width:el.querySelector('.video-dialog').getBoundingClientRect().width,sw:el.querySelector('.video-dialog').scrollWidth,cw:el.querySelector('.video-dialog').clientWidth,docSw:document.documentElement.scrollWidth,docCw:document.documentElement.clientWidth}));
  assert.ok(compact.sw<=compact.cw+1&&compact.docSw<=compact.docCw+1,'320px mobile overflow: '+JSON.stringify(compact));
  await close.click();
  await page.evaluate(()=>{db.set('language','en');document.documentElement.lang='en';renderWorkout()});
  const english=page.locator('.tp15-demo-launch');assert.equal(await english.innerText(),'Demo / instructions');
  await english.click();await page.waitForSelector('#videoModal');
  assert.equal(await page.locator('.tp15-demo-guide h3').innerText(),'Instructions');
  assert.equal(await page.locator('.tp15-demo-close').getAttribute('aria-label'),'Close exercise demo');
  assert.deepEqual(errors,[],'browser page errors');
  console.log('PASS #15 single-tap demo, video+instructions, red sticky X, one external action, Android Back, focus, 320px, EN');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
