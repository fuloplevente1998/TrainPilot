const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');

(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
  fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)});
 });
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:720},locale:'hu-HU'});
  await page.goto(`http://127.0.0.1:${server.address().port}`);
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.waitForTimeout(120);

  assert.equal(await page.evaluate(()=>window.TrainPilotInlineCompact?.version),'2630');

  // Edzés: tapping a current exercise expands it in place instead of navigating
  // to the separate editor screen.
  await page.evaluate(()=>{state.session=null;state.tab='plan';render()});
  await page.waitForTimeout(80);
  const exercises=page.locator('.tp152-day[open] details.tp152-exercise');
  assert.ok(await exercises.count()>0,'Edzés must render inline exercise accordions');
  const first=exercises.first();
  const summary=first.locator('summary');
  const toggleTarget=first.locator('.tp146-exercise-copy');
  assert.equal(await first.getAttribute('open'),null,'exercise starts collapsed');
  const beforeTitle=await page.locator('.tp150-active-program h2').first().innerText();
  await toggleTarget.click();
  await page.waitForTimeout(40);
  assert.notEqual(await first.getAttribute('open'),null,'exercise opens inline');
  assert.equal(await page.locator('#rf154Replacement').count(),0,'inline open must not navigate to editor');
  assert.equal(await page.locator('.tp150-active-program h2').first().innerText(),beforeTitle,'screen context stays on the same workout');
  assert.equal(await first.locator('.tp152-edit-grid').isVisible(),true,'expanded exercise exposes the 1.5.2 inline editor');

  // Video action remains independent: it must not toggle the accordion.
  await toggleTarget.click();
  assert.equal(await first.getAttribute('open'),null,'exercise can close again');
  await page.evaluate(()=>{window.__tp146Video=null;window.openDemo=id=>{window.__tp146Video=id}});
  const play=first.locator('.tp-play-btn');
  if(await play.count()){
   await play.click();
   await page.waitForTimeout(20);
   assert.equal(await first.getAttribute('open'),null,'video button must not expand the exercise');
   assert.ok(await page.evaluate(()=>!!window.__tp146Video),'video action still fires');
  }

  // 1.5.2 removes the separate exercise editor: editing remains inside the expanded card.
  await toggleTarget.click();
  assert.notEqual(await first.getAttribute('open'),null,'exercise reopens inline for editing');
  assert.equal(await first.locator('.tp152-edit-grid').isVisible(),true,'exercise editor stays inside the workout hierarchy');
  assert.equal(await page.locator('#rf154Replacement').count(),0,'separate exercise editor must not be opened');

  // Napló: collapsed entries should be materially thinner, with secondary stats
  // hidden until the entry is opened.
  await page.evaluate(()=>{
   const e=byId('db-squat')||exercises()[0];
   const started=new Date(Date.now()-45*60000),finished=new Date();
   db.set('history',[{
    workout:'A',dayId:'A',programId:'home-basic',programName:'Otthoni A/B – Alap',
    started:started.toISOString(),finished:finished.toISOString(),
    exercises:[{id:e.id,hu:e.hu,en:e.en,loadType:e.loadType,repUnit:e.repUnit,sets:[{set:1,weight:5,reps:'10',done:true}]}]
   }]);
   state.session=null;go('history');
  });
  await page.waitForTimeout(100);
  const h=page.locator('details.rf263-history').first();
  assert.equal(await h.count(),1,'journal entry must exist');
  const metrics=await h.evaluate(el=>{
   const summary=el.querySelector('summary'),head=el.querySelector('.history-head'),badges=el.querySelector('.history-badges');
   return {
    open:el.open,
    height:summary.getBoundingClientRect().height,
    summaryPadding:getComputedStyle(summary).paddingTop,
    headPadding:getComputedStyle(head).paddingTop,
    badgesDisplay:getComputedStyle(badges).display
   };
  });
  assert.equal(metrics.open,false);
  assert.ok(metrics.height<95,`collapsed journal row should be compact, got ${metrics.height}px`);
  assert.equal(metrics.summaryPadding,'5px');
  assert.equal(metrics.headPadding,'8px');
  assert.equal(metrics.badgesDisplay,'none','secondary journal stats stay hidden while collapsed');

  await h.locator('summary').click();
  await page.waitForTimeout(30);
  const opened=await h.evaluate(el=>({open:el.open,badges:getComputedStyle(el.querySelector('.history-badges')).display}));
  assert.equal(opened.open,true);
  assert.notEqual(opened.badges,'none','secondary stats return when journal entry opens');

  console.log('PASS: 2630 inline exercise accordions and compact journal rows.');
 }finally{
  if(browser)await browser.close();
  await new Promise(r=>server.close(r));
 }
})().catch(e=>{console.error(e);process.exit(1)});
