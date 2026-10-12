'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}
 fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data)});
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
  browser=await chromium.launch({args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];
  page.on('pageerror',error=>errors.push(error.message));
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>TrainPilotBoot.finished);
  await page.evaluate(()=>{
   const ids=['db-squat','db-floor-press','one-arm-row','rdl','db-curl','pushup','plank','side-plank'];
   const rows=Array.from({length:240},(_,i)=>{
    const end=new Date(Date.UTC(2026,9,11,15)-i*86400000);
    return {id:'disclosure171-'+i,programId:'home-basic',dayId:'A',workout:'A',started:new Date(end-2700000).toISOString(),finished:end.toISOString(),exercises:ids.map((id,j)=>({...byId(id),sets:[1,2,3,4].map(set=>({set,weight:10+j,reps:'10',done:true}))})),photos:[]};
   });
   db.set('history',rows);db.set('language','hu');state.session=null;
   window.disclosure171={reads:{},fits:0};
   const get=db.get;db.get=function(key){disclosure171.reads[key]=(disclosure171.reads[key]||0)+1;return get.apply(this,arguments)};
   const fit=tp107FitViews;tp107FitViews=function(){disclosure171.fits++;return fit.apply(this,arguments)};
  });
  const settle=()=>page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
  const reset=()=>page.evaluate(()=>{disclosure171.reads={};disclosure171.fits=0});
  const stats=()=>page.evaluate(()=>({...disclosure171,reads:{...disclosure171.reads}}));
  const click=async selector=>{await page.evaluate(selector=>document.querySelector(selector).click(),selector);await settle()};
  const checks=[];
  for(const [name,route,selector,prepare] of [
   ['training-settings','plan','.tp1511-training > summary',null],
   ['training-day','plan','.tp152-day > summary',null],
   ['training-exercise','plan','.tp107-program-editor > summary',"document.querySelector('.tp152-day').open=true"],
   ['program-card','programs','[data-tp7-program-id] > summary',null],
   ['journal-workout','history','[data-tp152-history-key] > summary',null],
   ['journal-exercise','history','.tp3-history-ex-item > summary',"document.querySelector('[data-tp152-history-key] > summary').click()"]
  ]){
   await page.evaluate(({route,prepare})=>{state.tp3HistoryOpenKey=null;state.tp3HistoryExerciseEdit=null;go(route);if(prepare)(0,eval)(prepare)},{route,prepare});await settle();await page.waitForTimeout(60);
   assert.equal(await page.locator(selector).first().isVisible(),true,name+' disclosure is reachable');
   await page.evaluate(()=>window.disclosureMain=document.querySelector('#app main'));await reset();await click(selector);
   const first=await stats(),historyBudget=name==='journal-workout'?2:name==='journal-exercise'?1:0;
   assert.ok((first.reads.history||0)<=historyBudget,name+' full Journal reads: '+JSON.stringify(first));
   assert.ok((first.reads.language||0)<=6,name+' must not repeatedly parse language settings');
   assert.equal(first.fits,0,name+' must not remeasure unchanged page/navigation geometry');
   assert.equal(await page.evaluate(()=>disclosureMain===document.querySelector('#app main')),true,name+' keeps the page mounted');
   if(name==='training-exercise'){
    assert.equal(await page.locator('.tp107-editor-body .tp-select-trigger').count(),1,'new replacement picker is enhanced locally');
    await page.locator('.tp107-editor-body input[data-tp107-rx="weight"]').first().fill('19.5');
   }
   await click(selector);await reset();await click(selector);const reopened=await stats();
   assert.ok((reopened.reads.history||0)<=(name==='journal-exercise'?1:0),name+' reopening avoids redundant Journal reads');
   assert.equal(reopened.fits,0,name+' reopening performs no layout-fitting pass');
   if(name==='training-exercise')assert.equal(await page.locator('.tp107-editor-body input[data-tp107-rx="weight"]').first().inputValue(),'19.5','collapsing preserves an unsaved prescription');
   checks.push({name,first,reopened});
  }
  // An editor opened after a storage update must see current data, even if the
  // outer workout disclosure is still mounted. No cross-operation cache.
  await click('.tp3-history-ex-item > summary');
  await page.evaluate(()=>{const rows=history();rows[0].exercises[0].sets[0].weight=77;db.set('history',rows)});
  await reset();await click('.tp3-history-ex-item > summary');
  assert.equal(await page.evaluate(()=>state.tp3HistoryExerciseEdit.draft.sets[0].weight),77,'new editor sees imported/edited Journal data');
  assert.equal((await stats()).reads.history,1,'editor shares one fresh Journal snapshot');
  assert.equal(await page.evaluate(()=>history()[0].exercises[0].sets[0].weight),77,'opening an editor preserves stored measurements');

  await page.evaluate(()=>go('health'));await settle();await page.waitForTimeout(60);
  await page.evaluate(()=>{window.disclosureMain=document.querySelector('#app main');window.disclosurePulse=document.querySelector('.tp169-pulse-journal')});
  await reset();await click('.tp168-weight-summary');
  assert.equal((await stats()).reads.history||0,0,'weight disclosure does not rebuild the Health overview');
  await click('.tp169-weight-entry > summary');await page.locator('#tp168WeightKg').fill('79.4');await page.locator('#tp168WeightKg').evaluate(node=>node.blur());
  await click('.tp168-weight-summary');await reset();await click('.tp168-weight-summary');
  assert.equal(await page.locator('#tp168WeightKg').inputValue(),'79.4','closing/reopening retains the measurement draft');
  assert.equal(await page.locator('.tp169-weight-entry').evaluate(node=>node.open),true,'nested disclosure state is retained');
  assert.equal(await page.evaluate(()=>disclosureMain===document.querySelector('#app main')&&disclosurePulse===document.querySelector('.tp169-pulse-journal')),true,'weight toggle keeps Health and decorated pulse subtree mounted');
  assert.equal((await stats()).reads.history||0,0,'reopening weight makes no Journal read');
  assert.equal((await stats()).fits,1,'Health click/toggle fitting is coalesced');
  await click('.tp168-weight-save');
  assert.equal(await page.evaluate(()=>weights().some(row=>row.kg===79.4)),true,'explicit Save still persists the measurement');
  await page.locator('.tp168-weight-range button').first().click();await settle();
  assert.equal(await page.evaluate(()=>state.weightRange),'7','explicit range change still updates Health');
  assert.equal(await page.locator('.tp168-weight-summary').getAttribute('aria-expanded'),'true');

  // Navigation height is cached only while geometry is unchanged. Observe text
  // sizing and resize/rotation, and reject an observer -> layout feedback loop.
  await page.evaluate(()=>go('home'));await settle();await page.waitForTimeout(60);
  await reset();await page.evaluate(()=>document.querySelector('.top').style.paddingBottom='42px');
  await page.waitForTimeout(100);await settle();
  const navigation=()=>page.evaluate(()=>({actual:Math.ceil(document.querySelector('.top').getBoundingClientRect().bottom),fitted:parseInt(document.documentElement.style.getPropertyValue('--tp107-nav-height'),10)}));
  const resized=await navigation();assert.equal(resized.fitted,resized.actual,'navigation content resize refreshes the fitted inset');
  await reset();await page.waitForTimeout(120);assert.equal((await stats()).fits,0,'unchanged navigation never spins ResizeObserver');
  for(const viewport of [{width:280,height:720},{width:873,height:393},{width:393,height:873}]){
   await page.setViewportSize(viewport);await page.waitForTimeout(100);await settle();
   const nav=await navigation();assert.equal(nav.fitted,nav.actual,'fitted inset follows viewport changes');
  }
  assert.deepEqual(errors,[],'actual disclosure flows produce no browser errors');
  fs.mkdirSync('ui-evidence',{recursive:true});fs.writeFileSync('ui-evidence/disclosure-performance-171.json',JSON.stringify(checks,null,2)+'\n');
  console.log('PASS #171: bounded cold/warm disclosure reads, no unrelated fitting, local pickers, preserved drafts, live data, weight save/range, responsive navigation without observer loop');
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve))}
})().catch(error=>{console.error(error);process.exitCode=1});
