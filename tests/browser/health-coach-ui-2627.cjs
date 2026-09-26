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
  const page=await browser.newPage({viewport:{width:393,height:540},locale:'hu-HU'});
  await page.goto(`http://127.0.0.1:${server.address().port}`);
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.waitForTimeout(100);

  const makeScrollable=async()=>{
   await page.evaluate(()=>{const main=document.querySelector('main');const f=document.createElement('div');f.id='tp2627ScrollProbe';f.style.height='1600px';f.setAttribute('aria-hidden','true');main?.appendChild(f);window.scrollTo(0,9999)});
   await page.waitForTimeout(80);
   return page.evaluate(()=>window.scrollY);
  };

  // #13: the whole Home Coach card opens the shared Coach panel while preserving Home underneath.
  await page.evaluate(()=>{state.tab='home';render();window.scrollTo(0,0)});
  await page.waitForTimeout(80);
  assert.equal(await page.locator('#rf220CoachCard button').count(),0,'#13 Home Coach must not restore separate action buttons');
  await page.click('#rf220CoachCard');
  await page.waitForSelector('#tp155R4PanelHost[data-panel="coach"] .tp151-coach');
  const coach=await page.evaluate(()=>(
   {tab:state.tab,homeClass:document.querySelector('#app main')?.classList.contains('rf221-home'),panelOpen:document.body.classList.contains('tp155-r4-panel-open'),text:document.querySelector('#tp155R4PanelHost .tp151-coach')?.innerText||'',active:document.querySelector('.tp154-coach-action')?.classList.contains('active')}
  ));
  assert.equal(coach.tab,'home');
  assert.equal(coach.homeClass,true);
  assert.equal(coach.panelOpen,true);
  assert.match(coach.text,/TrainPilot Coach/);
  assert.equal(coach.active,true);
  const coachScroll=await page.evaluate(async()=>{const panel=document.querySelector('#tp155R4PanelHost .tp155-r4-panel'),probe=document.createElement('div');probe.style.height='1600px';panel.querySelector('.tp155-r4-panel-content')?.appendChild(probe);panel.scrollTop=9999;await new Promise(r=>setTimeout(r,50));const y=panel.scrollTop;probe.remove();return y});
  assert.ok(coachScroll>0,'Coach panel must scroll internally when content is taller than viewport');

  // #67: Home has no separate Statistics button; the canonical Progress view
  // opens within Journal and must release the Coach panel scroll lock.
  await page.evaluate(()=>{window.tp155R4ClosePanel?.(false);go('progress');window.scrollTo(0,0)});
  await page.waitForTimeout(80);
  const stats=await page.evaluate(()=>(
   {tab:state.tab,homeClass:document.querySelector('main')?.classList.contains('rf221-home'),overflow:getComputedStyle(document.body).overflow,text:document.querySelector('main')?.innerText||'',active:document.querySelector('.tab.active .tp151-nav-label')?.textContent?.trim()||[...document.querySelectorAll('.tab')].find(x=>x.classList.contains('active'))?.textContent?.trim()}
  ));
  assert.equal(stats.tab,'history','Progress route resolves to the Journal container');
  assert.equal(await page.evaluate(()=>state.tp177JournalView),'progress');
  assert.equal(stats.homeClass,false);
  assert.notEqual(stats.overflow,'hidden');
  assert.match(stats.text,/Fejlődés|Progress/);
  assert.match(stats.active||'',/^(Napló|Log|Journal)$/,'Progress must identify Journal as its navigation parent');
  assert.ok((await makeScrollable())>0,'Progress screen must scroll when content is taller than viewport');

  // Legacy router alias must reopen the same canonical Progress screen.
  await page.evaluate(()=>{go('home');go('progress')});
  await page.waitForSelector('main.tp177-progress');
  assert.equal(await page.evaluate(()=>state.tab),'history');
  assert.equal(await page.evaluate(()=>state.tp177JournalView),'progress');

  console.log('PASS: TrainPilot 2627 #13 Coach entry + canonical Progress route release scroll lock deterministically.');
 }finally{
  if(browser)await browser.close();
  await new Promise(r=>server.close(r));
 }
})().catch(e=>{console.error(e);process.exit(1)});
