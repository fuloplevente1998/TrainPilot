const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';
 const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
 fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}
  res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d);
 });
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
  page.on('dialog',d=>d.accept());
  await page.goto('http://127.0.0.1:'+server.address().port+'/');
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  const baseline=await page.evaluate(()=>{
   db.set('language','hu');db.set('settings',{...settings(),profile:{age:30,goal:'fitness',minutes:45}});db.set('draft',null);state.session=null;state.workout=null;go('home');
   const main=document.querySelector('main.rf221-home');return {compact:main.classList.contains('tp-home-draft-compact'),onboarding:main.querySelector('.onboarding').getBoundingClientRect().height,coach:main.querySelector('#rf220CoachCard').getBoundingClientRect().height};
  });
  assert.equal(baseline.compact,false);
  const normalPlanner=async()=>page.locator('main>.onboarding').evaluate(card=>{const h=card.querySelector('h2'),p=card.querySelector('p'),b=card.querySelector('button');return {heading:getComputedStyle(h).fontSize,copy:getComputedStyle(p).fontSize,height:card.getBoundingClientRect().height,buttonWidth:b.getBoundingClientRect().width,stacked:b.getBoundingClientRect().top>=p.getBoundingClientRect().bottom-1};});
  const normal=await normalPlanner();
  assert.equal(normal.heading,'24px');assert.equal(normal.copy,'16px');assert.equal(normal.stacked,true,'normal planner keeps its full-width button below the copy');
  assert.ok(normal.height>=110,'normal planner retains its full-size card');
  await page.evaluate(()=>{
   startWorkout(activeProgram().days[0].id);persistDraft();state.session=null;state.workout=null;go('home');
  });
  await page.waitForFunction(()=>document.querySelector('main.rf221-home.tp-home-draft-compact'));
  const layout=await page.evaluate(()=>{
   const main=document.querySelector('main.rf221-home'),cards=[...main.children].filter(el=>el.matches('.card,.hero,.grid2'));
   const visible=el=>{const r=el.getBoundingClientRect(),s=getComputedStyle(el);return s.display!=='none'&&s.visibility!=='hidden'&&r.width>0&&r.height>0};
   const draft=main.querySelector('.card:has(button[onclick*="resumeDraft"])');
   return {onboarding:main.querySelector('.onboarding').getBoundingClientRect().height,coach:main.querySelector('#rf220CoachCard').getBoundingClientRect().height,
    cards:cards.length,allVisible:cards.every(visible),draftButtons:[...draft.querySelectorAll('button')].map(b=>({text:b.textContent.trim(),visible:visible(b)})),
    coachCopy:visible(main.querySelector('#rf220CoachCard .tp166-home-coach-copy p')),
    bottom:Math.max(...cards.map(el=>el.getBoundingClientRect().bottom)),viewport:innerHeight,
    horizontalOverflow:document.documentElement.scrollWidth>innerWidth+1};
  });
  assert.ok(layout.cards>=5&&layout.allVisible,'all Home cards remain visible: '+JSON.stringify(layout));
  assert.ok(layout.draftButtons.some(b=>b.text.includes('Edzés folytatása')&&b.visible));
  assert.ok(layout.draftButtons.some(b=>b.text.includes('törlése')&&b.visible));
  assert.equal(layout.coachCopy,true,'Coach text remains visible');
  assert.ok(layout.onboarding<baseline.onboarding&&layout.coach<baseline.coach,'other Home cards shrink when the draft appears: '+JSON.stringify({baseline,layout}));
  assert.equal(layout.horizontalOverflow,false);
  assert.ok(layout.bottom<=layout.viewport+1,'Home cards fit without scrolling on a 393×873 phone: '+JSON.stringify(layout));
  await page.evaluate(()=>{db.set('draft',null);state.session=null;state.workout=null;go('home')});
  assert.equal(await page.locator('main.rf221-home.tp-home-draft-compact').count(),0,'normal Home layout returns after draft removal');
  assert.deepEqual(await normalPlanner(),normal,'removing the draft restores the same full planner');
  await page.evaluate(()=>{go('plan');go('home');rf200SetTheme('green');});
  assert.deepEqual(await normalPlanner(),normal,'navigation and theme change preserve the normal planner');
  await page.reload();await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  assert.deepEqual(await normalPlanner(),normal,'refresh preserves the normal planner');
  console.log('PASS: draft Home is compact, complete, and fits one phone viewport; normal Home is restored.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exitCode=1});
