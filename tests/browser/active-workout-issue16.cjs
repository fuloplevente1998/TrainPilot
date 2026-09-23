const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
  fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(d)});
 });
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:790},locale:'hu-HU'});const errors=[];page.on('pageerror',e=>errors.push(String(e)));
  await page.goto('http://127.0.0.1:'+server.address().port);
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilotIssue16&&window.TrainPilotDemo15);
  await page.evaluate(()=>{
   db.set('language','hu');document.documentElement.lang='hu';state.session=null;state.workout=null;
   const p=activeProgram(),d=p.days[0];startWorkout(d.id,null,p.id);state.current=0;renderWorkout();
  });
  await page.waitForSelector('main.tp153-workout .tp16-workout-actions');
  const initial=await page.evaluate(()=>{
   const a=document.querySelector('.tp16-workout-actions'),p=a.querySelector('.tp16-workout-prev'),n=a.querySelector('.tp16-workout-next');
   const ar=a.getBoundingClientRect(),pr=p.getBoundingClientRect(),nr=n.getBoundingClientRect();
   return {position:getComputedStyle(a).position,prev:p.textContent.trim(),next:n.textContent.trim(),prevDisabled:p.disabled,prevH:pr.height,nextH:nr.height,gap:nr.left-pr.right,order:pr.left<nr.left,actionBottom:ar.bottom,vh:innerHeight,overflow:document.documentElement.scrollWidth-innerWidth};
  });
  assert.equal(initial.position,'fixed','workout controls must stay viewport-fixed');
  assert.equal(initial.prev,'Előző gyakorlat');assert.equal(initial.next,'Következő gyakorlat');assert.equal(initial.prevDisabled,true);
  assert.ok(initial.prevH>=48&&initial.nextH>=48,'phone controls must retain large tap targets: '+JSON.stringify(initial));
  assert.ok(initial.gap>=5&&initial.order,'Previous and Next must be separate left/right controls: '+JSON.stringify(initial));
  assert.ok(initial.overflow<=1,'393px layout must not overflow');

  await page.evaluate(()=>{state.timer=75;state.restEndAt=Date.now()+75000;renderWorkout()});
  await page.waitForSelector('.timer.tp16-rest-timer.tp16-rest-docked');
  const docked=await page.evaluate(()=>{
   const t=document.querySelector('.tp16-rest-timer'),h=document.querySelector('.tp16-workout-head'),b=t.querySelector('button'),s=t.querySelector('strong');
   const tr=t.getBoundingClientRect(),hr=h.getBoundingClientRect(),br=b.getBoundingClientRect();
   return {position:getComputedStyle(t).position,parent:t.parentElement===h,text:s.textContent.trim(),font:getComputedStyle(s).fontSize,skip:b.textContent.trim(),skipH:br.height,w:tr.width,inside:tr.top>=hr.top&&tr.bottom<=hr.bottom+2};
  });
  assert.equal(docked.position,'relative');assert.equal(docked.parent,true);assert.equal(docked.skip,'Kihagyás');assert.equal(docked.font,'22px');assert.ok(docked.skipH>=34);assert.ok(docked.inside,'rest capsule should dock into visible workout header');assert.ok(docked.w<270,'rest capsule must stay compact');

  await page.evaluate(()=>{const h=document.querySelector('.tp16-workout-head'),sp=document.createElement('div');sp.id='tp16ScrollProbe';sp.style.height='1400px';h.insertAdjacentElement('afterend',sp);window.scrollTo(0,900)});
  await page.waitForFunction(()=>document.querySelector('.tp16-rest-timer')?.classList.contains('tp16-rest-floating'));
  const floating=await page.evaluate(()=>{
   const t=document.querySelector('.tp16-rest-timer'),top=document.querySelector('.top'),a=document.querySelector('.tp16-workout-actions');
   const tr=t.getBoundingClientRect(),nr=top.getBoundingClientRect(),ar=a.getBoundingClientRect();
   return {position:getComputedStyle(t).position,top:tr.top,navBottom:nr.bottom,bottom:tr.bottom,actionsTop:ar.top,overflow:document.documentElement.scrollWidth-innerWidth};
  });
  assert.equal(floating.position,'fixed');assert.ok(floating.top>=floating.navBottom+4,'floating rest timer must remain below top navigation: '+JSON.stringify(floating));assert.ok(floating.bottom<floating.actionsTop,'top rest timer and bottom controls must not overlap');assert.ok(floating.overflow<=1);

  await page.locator('.tp16-rest-skip').click();await page.waitForFunction(()=>!document.querySelector('.tp16-rest-timer'));
  assert.equal(await page.locator('main.tp153-workout').count(),1,'Skip must preserve active workout');

  await page.evaluate(()=>{document.getElementById('tp16ScrollProbe')?.remove();state.current=state.session.exercises.length-1;renderWorkout()});
  const last=await page.evaluate(()=>({finish:document.querySelector('.tp16-workout-next')?.textContent.trim(),prev:document.querySelector('.tp16-workout-prev')?.textContent.trim(),finishClass:document.querySelector('.tp16-workout-next')?.classList.contains('tp16-workout-finish')}));
  assert.equal(last.finish,'Edzés befejezése');assert.equal(last.prev,'Előző gyakorlat');assert.equal(last.finishClass,true);

  await page.setViewportSize({width:320,height:640});await page.evaluate(()=>{state.current=Math.min(1,state.session.exercises.length-1);renderWorkout()});await page.waitForTimeout(60);
  const narrow=await page.evaluate(()=>{
   const a=document.querySelector('.tp16-workout-actions'),p=document.querySelector('.tp16-workout-prev'),n=document.querySelector('.tp16-workout-next'),pr=p.getBoundingClientRect(),nr=n.getBoundingClientRect();
   return {overflow:document.documentElement.scrollWidth-innerWidth,prevH:pr.height,nextH:nr.height,stacked:nr.top>pr.bottom-1,order:nr.top>=pr.top};
  });
  assert.ok(narrow.overflow<=1,'320px workout must not overflow: '+JSON.stringify(narrow));assert.ok(narrow.prevH>=48&&narrow.nextH>=48);assert.equal(narrow.stacked,true,'320px may stack controls for full tap targets');assert.equal(narrow.order,true,'stacked order must remain Previous then Next');
  assert.deepEqual(errors,[],'browser page errors');
  console.log('PASS #16 sticky large workout controls, docked/floating compact rest timer, skip, finish and 320px layout');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
