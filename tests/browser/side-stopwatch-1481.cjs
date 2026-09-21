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
const listen=()=>new Promise(r=>server.listen(0,'127.0.0.1',r));
const base=()=>'http://127.0.0.1:'+server.address().port+'/';

(async()=>{
 await listen();let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
  page.on('dialog',d=>d.accept());
  await page.goto(base());await page.waitForFunction(()=>window.TrainPilotBoot?.finished);

  await page.evaluate(()=>{
   db.set('activeProgramId','home-level2');
   startWorkout('A',null,'home-level2');
   const i=state.session.exercises.findIndex(e=>e.id==='side-plank');
   if(i<0)throw Error('side-plank missing from home-level2/A');
   state.current=i;renderWorkout();
  });

  assert.equal(await page.locator('#tp1481SideToggle-left').count(),1,'left-side timer missing');
  assert.equal(await page.locator('#tp1481SideToggle-right').count(),1,'right-side timer missing');
  assert.equal(await page.locator('#rf110StopwatchToggle').count(),0,'legacy single timer must not be shown for mp/oldal');
  assert.equal(await page.locator('#rf110Stopwatch.tp161-side-stopwatch').count(),1,'1.6.1 bilateral wrapper missing');
  assert.equal(await page.locator('#rf110Stopwatch.tp161-side-stopwatch.card').count(),0,'bilateral stopwatch must not remain one combined outer card');
  assert.equal(await page.locator('#rf110Stopwatch .tp161-side-window').count(),2,'left and right timers must be two distinct small cards');
  const sideLayout=await page.evaluate(()=>{const card=document.querySelector('#rf110Stopwatch'),actions=document.querySelector('.tp153-workout-actions');if(!card||!actions)return null;const c=card.getBoundingClientRect(),a=actions.getBoundingClientRect(),cs=getComputedStyle(card),as=getComputedStyle(actions);return {parent:card.parentElement?.className||'',cardTop:c.top,cardBottom:c.bottom,actionsTop:a.top,actionsBottom:a.bottom,cardLeft:c.left,cardRight:c.right,actionsLeft:a.left,actionsRight:a.right,position:cs.position,zIndex:cs.zIndex,actionsPosition:as.position,overflow:document.documentElement.scrollWidth-innerWidth}});
  assert.ok(sideLayout,'side-plank stopwatch layout must render');
  assert.ok(!String(sideLayout.parent).includes('tp153-workout-actions'),'side-plank stopwatch must not be inserted inside the two-column workout action grid');
  assert.equal(sideLayout.position,'static','side-plank stopwatch must stay in normal document flow and never overlay workout navigation');
  assert.ok(sideLayout.cardBottom<=sideLayout.actionsTop+1,'side-plank stopwatch must sit above Finish/Previous actions without overlapping them');
  assert.ok(Math.abs(sideLayout.cardLeft-sideLayout.actionsLeft)<3&&Math.abs(sideLayout.cardRight-sideLayout.actionsRight)<3,'side-plank stopwatch must align with the full workout action/content lane');
  assert.ok(sideLayout.overflow<=3,'side-plank layout must not overflow horizontally');

  await page.locator('#tp1481SideToggle-left').click();
  await page.waitForTimeout(1150);
  await page.locator('#tp1481SideToggle-left').click();
  const left=await page.evaluate(()=>{
   const e=state.session.exercises[state.current],s=e.sets.find(x=>!x.done);return Number(s.leftSeconds||0);
  });
  assert.ok(left>=1,'left-side timer must persist seconds');
  await page.evaluate(()=>renderWorkout());
  assert.equal(Number(await page.locator('#tp1481SideInput-left').inputValue()),left,'left-side value must survive workout rerender');

  await page.locator('#tp1481SideToggle-right').click();
  await page.waitForTimeout(1150);
  await page.locator('#tp1481SideToggle-right').click();
  const beforeDone=await page.evaluate(()=>{
   const e=state.session.exercises[state.current],s=e.sets.find(x=>!x.done);
   return {left:Number(s.leftSeconds||0),right:Number(s.rightSeconds||0),reps:s.reps,done:s.done};
  });
  assert.ok(beforeDone.left>=1&&beforeDone.right>=1,'both sides must be stored separately');
  assert.equal(Number(beforeDone.reps),Math.min(beforeDone.left,beforeDone.right),'compatibility reps must use the weaker side');
  assert.equal(beforeDone.done,false);
  const rowValue=Number(await page.locator('main .row').first().locator('input.field').last().inputValue());
  assert.equal(rowValue,Math.min(beforeDone.left,beforeDone.right),'visible set row must immediately reflect the stored bilateral compatibility value');

  await page.locator('#rf110Stopwatch .tp161-side-window').first().getByRole('button',{name:'Nullázás'}).click();
  const afterLeftReset=await page.evaluate(()=>{const s=state.session.exercises[state.current].sets.find(x=>!x.done);return {left:Number(s.leftSeconds||0),right:Number(s.rightSeconds||0),reps:s.reps};});
  assert.equal(afterLeftReset.left,0,'resetting left must clear only left');
  assert.equal(afterLeftReset.right,beforeDone.right,'resetting left must preserve right');
  await page.locator('#rf110Stopwatch .tp161-side-window').first().getByRole('button',{name:'Szerkesztés'}).click();
  await page.locator('#tp1481SideInput-left').fill(String(beforeDone.left));
  const restored=await page.evaluate(()=>{const s=state.session.exercises[state.current].sets.find(x=>!x.done);return {left:Number(s.leftSeconds||0),right:Number(s.rightSeconds||0),reps:Number(s.reps||0)};});
  assert.equal(restored.left,beforeDone.left,'manual left restore must persist');
  assert.equal(restored.right,beforeDone.right,'manual left restore must not overwrite right');
  assert.equal(restored.reps,Math.min(restored.left,restored.right),'compatibility reps must resync after side edit');
  await page.evaluate(()=>renderWorkout());
  assert.equal(Number(await page.locator('#tp1481SideInput-left').inputValue()),restored.left,'left must persist after second rerender');
  assert.equal(Number(await page.locator('#tp1481SideInput-right').inputValue()),restored.right,'right must persist after second rerender');

  await page.locator('.row .check').first().click();
  const completed=await page.evaluate(()=>{
   const e=state.session.exercises[state.current],s=e.sets[0];
   return {left:Number(s.leftSeconds||0),right:Number(s.rightSeconds||0),reps:Number(s.reps||0),done:s.done};
  });
  assert.equal(completed.done,true,'per-side set must complete after both sides are recorded');
  assert.equal(completed.reps,Math.min(completed.left,completed.right));

  await page.evaluate(()=>{
   const e=byId('plank');if(!e)throw Error('plank exercise missing from catalog');
   state.tab='plan';state.workout='A';state.current=0;
   state.session={programId:activeProgram()?.id||'home-basic',programName:'Timer parity',dayId:'A',workout:'A',started:new Date().toISOString(),exercises:[{id:e.id,hu:e.hu,en:e.en,loadType:e.loadType,repUnit:e.repUnit,sets:[{set:1,weight:0,reps:'',done:false}]}]};
   renderWorkout();
  });
  assert.equal(await page.locator('#rf110StopwatchToggle').count(),1,'normal mp exercise must keep the legacy single stopwatch');
  assert.equal(await page.locator('#tp1481SideToggle-left').count(),0,'normal plank must not show per-side controls');

  await page.close();
  console.log('PASS: 1.4.8.1 mp/oldal uses independent left/right timers; normal mp stopwatch is unchanged.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
