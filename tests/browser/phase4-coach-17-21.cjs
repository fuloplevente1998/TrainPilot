const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const browser=await chromium.launch({headless:true}),page=await browser.newPage({viewport:{width:393,height:852}});
 try{
  await page.goto('http://127.0.0.1:'+server.address().port+'/');
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  const setup=await page.evaluate(()=>{
   db.set('language','hu');document.documentElement.lang='hu';
   const now=Date.now(),started=new Date(now-3600000).toISOString(),finished=new Date(now-1800000).toISOString();
   db.set('history',[{id:'tp4-history',programId:'tp4-program',programName:'Phase4 tesztprogram',dayId:'A',workout:'A',started,finished,exercises:[
    {id:'db-floor-press',loadType:'per_hand',repUnit:'ism.',sets:[{set:1,weight:12,reps:'10',done:true}]},
    {id:'pushup',loadType:'bodyweight',repUnit:'ism.',sets:[{set:1,weight:0,reps:'15',done:true}]},
    {id:'plank',loadType:'bodyweight',repUnit:'mp',sets:[{set:1,weight:0,reps:'45',done:true}]},
    {id:'side-plank',loadType:'bodyweight',repUnit:'mp/oldal',sets:[{set:1,weight:0,reps:'30',leftSeconds:30,rightSeconds:24,done:true}]}
   ]}]);
   const p={id:'tp4-program',name:'Phase4 tesztprogram',builtin:false,days:[{id:'A',name:'A',exercises:['db-floor-press','pushup','plank','side-plank']}]};
   db.set('programs',[...programs().filter(x=>x.id!==p.id),p]);db.set('activeProgramId',p.id);
   const start=new Date(Date.now()+2*86400000);start.setHours(18,30,0,0);
   db.set('scheduled',[{id:'tp4-next',programId:p.id,dayId:'A',workout:'A',start:start.toISOString(),end:new Date(start.getTime()+3600000).toISOString(),status:'planned',cancelled:false,updatedAt:Date.now()}]);
   state.tab='stats';rf221StatsScreen();
   return {start:start.toISOString()};
  });
  await page.waitForSelector('.tp4-stats-card');
  const stats=await page.evaluate(()=>({
   rows:[...document.querySelectorAll('.tp4-stat-row')].map(x=>({id:x.dataset.tp3Stat,kind:x.dataset.tp4Kind,text:(x.innerText||'').replace(/\s+/g,' ').trim()})),
   progress:rf220ProgressRows().map(x=>({id:x.id,kind:x.s.kind,count:x.s.count}))
  }));
  const ids=stats.rows.map(x=>x.id).sort();
  assert.deepEqual(ids,['db-floor-press','plank','pushup','side-plank'].sort(),'Statistics must include every logged exercise type');
  assert.equal(stats.rows.find(x=>x.id==='db-floor-press').kind,'weighted');
  assert.equal(stats.rows.find(x=>x.id==='pushup').kind,'reps');
  assert.equal(stats.rows.find(x=>x.id==='plank').kind,'timed');
  assert.equal(stats.rows.find(x=>x.id==='side-plank').kind,'bilateral');
  assert.match(stats.rows.find(x=>x.id==='side-plank').text,/30\s*\/\s*24\s*mp/,'bilateral stats must show left/right time');

  await page.evaluate(()=>{state.tab='home';render();window.tp155R4OpenPanel('coach',document.activeElement)});
  await page.waitForSelector('#tp155R4PanelHost[data-panel="coach"] .tp151-coach');
  const coach=await page.evaluate(()=>({
   ids:rf233CoachPlan().exercises.map(x=>x.id),
   advice:rf233CoachPlan().exercises.map(x=>({id:x.id,action:x.advice?.action,text:x.advice?.text||''})),
   target:(document.querySelector('#tp155R4PanelHost .tp153-coach-next strong')?.textContent||'').trim(),
   duplicateHidden:!!document.querySelector('#tp155R4PanelHost .tp4-next-duplicate[hidden]'),
   cards:document.querySelectorAll('#tp155R4PanelHost .tp155-coach-ex').length
  }));
  assert.deepEqual(coach.ids,['db-floor-press','pushup','plank','side-plank'],'Coach must produce advice for every target-day exercise');
  assert.equal(coach.cards,4,'Coach UI must render all target-day exercise recommendations');
  for(const a of coach.advice){assert.ok(a.action&&a.text,'every Coach exercise needs a meaningful advice state: '+JSON.stringify(a))}
  assert.match(coach.target,/Phase4 tesztprogram/);
  assert.match(coach.target,/\bA\b/);
  assert.notEqual(coach.target,'A','next workout main value must not be a context-free day letter');
  assert.equal(coach.duplicateHidden,true,'duplicated target metadata row must be removed');

  for(const width of [320,412]){
   await page.setViewportSize({width,height:760});await page.waitForTimeout(50);
   const geo=await page.evaluate(()=>{const e=document.querySelector('#tp155R4PanelHost .tp153-coach-next'),s=e?.querySelector('strong');return {page:document.documentElement.scrollWidth-document.documentElement.clientWidth,metric:e?e.scrollWidth-e.clientWidth:999,strong:s?s.scrollWidth-s.clientWidth:999,white:s?getComputedStyle(s).whiteSpace:''}});
   assert.ok(geo.page<=1,'Coach page overflow at '+width+': '+JSON.stringify(geo));
   assert.ok(geo.metric<=1,'next-workout metric overflow at '+width+': '+JSON.stringify(geo));
   assert.equal(geo.white,'normal','next-workout summary must wrap on phone');
  }
  console.log('PASS Phase 4 browser: all Coach/stat exercise types + complete responsive next-workout summary.');
 }finally{await browser.close();server.close()}
})().catch(e=>{console.error(e);process.exit(1)});
