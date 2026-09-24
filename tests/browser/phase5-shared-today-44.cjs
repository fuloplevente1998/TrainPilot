const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:852},locale:'hu-HU'});
  await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(()=>{
   db.set('language','hu');state.health=state.health||{};
   const now=new Date(),key=rf240DayKey(now),yesterday=new Date(now);yesterday.setDate(now.getDate()-1);
   const days={[key]:{averageHeartRate:74,restingHeartRate:57,steps:9421}};
   rf240Ledger=()=>({version:1,days,lastSyncAt:new Date().toISOString()});
   const rec={day:key,sleepMinutes:450,hrvRmssdMs:54,hrvBaselineMs:48,sleepEnd:new Date().toISOString()};
   state.health.recovery=rec;db.set('recoveryHistory',[rec]);
   db.set('history',[
    {id:'today-1',started:new Date(now.getTime()-5400000).toISOString(),finished:new Date(now.getTime()-4500000).toISOString(),exercises:[]},
    {id:'today-2',started:new Date(now.getTime()-3200000).toISOString(),finished:new Date(now.getTime()-3000000).toISOString(),exercises:[]},
    {id:'yesterday',started:new Date(yesterday.getTime()-4000000).toISOString(),finished:new Date(yesterday.getTime()-3000000).toISOString(),exercises:[]},
    {id:'invalid',started:new Date(now.getTime()-200000).toISOString(),finished:new Date(now.getTime()-900000).toISOString(),exercises:[]}
   ]);
   go('home');
  });
  await page.waitForSelector('#rf223Today.tp5-home-today');
  const keys=['sleep','hrv','pulse','steps','workout','recovery'];
  const read=async scope=>scope.locator('.tp5-today-metric').evaluateAll(nodes=>nodes.map(n=>({key:[...n.classList].find(x=>x.startsWith('tp5-today-')&&x!=='tp5-today-metric'),label:n.querySelector('.tp5-today-label')?.textContent,value:n.querySelector('.tp5-today-value')?.textContent})));
  const home=page.locator('#rf223Today'),homeTiles=await read(home);
  assert.equal(homeTiles.length,6,'Home must show six Today tiles');
  assert.deepEqual(homeTiles.map(x=>x.key),keys.map(x=>'tp5-today-'+x),'Home Today order must be 2x3');
  assert.equal(homeTiles[4].value,'2','Today workouts must count only completed, valid logged sessions');
  assert.equal(homeTiles[2].value,'74 bpm');
  const position=await home.evaluate(e=>!!(e.compareDocumentPosition(document.querySelector('#rf220CoachCard'))&Node.DOCUMENT_POSITION_FOLLOWING));
  assert.ok(position,'Today must keep its original position before Coach');
  for(const width of [320,360,393,412]){
   await page.setViewportSize({width,height:760});await page.waitForTimeout(25);
   const state=await page.evaluate(()=>({sw:document.documentElement.scrollWidth,cw:document.documentElement.clientWidth,cols:getComputedStyle(document.querySelector('.tp5-today-grid')).gridTemplateColumns.split(' ').length}));
   assert.equal(state.cols,3,'Home must show 3 columns at '+width);
   assert.ok(state.sw<=state.cw+1,'Home overflow at '+width+': '+JSON.stringify(state));
  }
  await page.evaluate(()=>go('health'));await page.waitForSelector('main.rf263-health .tp5-today-full');
  const health=page.locator('main.tp168-health .tp5-today-grid'),healthTiles=await read(health);
  assert.deepEqual(healthTiles,homeTiles,'Home and Health must show identical six values and labels for the same day');
  assert.equal(await page.locator('main.tp168-health .tp168-resting').count(),0,'Resting pulse standalone shortcut must be removed');
  const widths=await page.evaluate(()=>({homeDay:document.querySelector('.tp5-today-grid')?.dataset.tp5Day,day:rf240DayKey(new Date())}));
  assert.equal(widths.homeDay,widths.day,'Today must use the local calendar day');
  for(const width of [320,360,393,412]){
   await page.setViewportSize({width,height:760});await page.waitForTimeout(25);
   const state=await page.evaluate(()=>({sw:document.documentElement.scrollWidth,cw:document.documentElement.clientWidth,cols:getComputedStyle(document.querySelector('.tp5-today-grid')).gridTemplateColumns.split(' ').length}));
   assert.equal(state.cols,3,'Health must show 3 columns at '+width);
   assert.ok(state.sw<=state.cw+1,'Health overflow at '+width+': '+JSON.stringify(state));
  }
  await page.evaluate(()=>{
   const k=rf240DayKey(new Date());rf240Ledger=()=>({version:1,days:{[k]:{averageHeartRate:0,restingHeartRate:0,steps:null}}});
   state.health.recovery={day:'2000-01-01',sleepMinutes:420,hrvRmssdMs:51};
   db.set('recoveryHistory',[]);TrainPilot168Health.decorate();
  });
  const missing=await read(page.locator('main.tp168-health .tp5-today-grid'));
  assert.equal(missing[0].value,'—','Stale sleep must not be shown as today’s data');
  assert.equal(missing[1].value,'—','Stale HRV must not be shown as today’s data');
  assert.equal(missing[2].value,'—','Zero pulse must be treated as missing');
  assert.equal(missing[3].value,'—','Null steps must not turn into zero');
  assert.equal(missing[5].value,'—','Unknown recovery must not present default readiness as a measured value');
  console.log('PASS Phase 5 #44 shared 2x3 Today model, workout provenance, missing data, layouts and placement');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
