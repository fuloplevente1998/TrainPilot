const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
const listen=()=>new Promise(r=>server.listen(0,'127.0.0.1',r)),base=()=>'http://127.0.0.1:'+server.address().port+'/';
(async()=>{await listen();let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
 const errors=[];page.on('pageerror',e=>errors.push(String(e?.stack||e)));page.on('console',m=>{if(m.type()==='error')errors.push('console: '+m.text())});page.on('dialog',d=>d.accept());
 await page.goto(base());await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
 const recoveryState=await page.evaluate(()=>{
   const e=byId('side-plank'),now=new Date().toISOString();if(!e)throw Error('side-plank missing');
   const valid={programId:'home-level2',programName:'Otthoni A/B – Haladó',dayId:'A',workout:'A',started:now,finished:now,exercises:[{id:e.id,hu:e.hu,en:e.en,loadType:e.loadType,repUnit:e.repUnit,effort:'good',sets:[{set:1,weight:0,reps:'16',leftSeconds:20,rightSeconds:16,done:true}]}]};
   db.set('history',[
    valid,
    null,
    {started:now,finished:now,workout:'A',exercises:null},
    {started:now,finished:now,workout:'B',exercises:[null,{id:'side-plank',repUnit:'mp/oldal',sets:[null,{leftSeconds:'12',rightSeconds:'10',done:true}]}]}
   ]);
   state.tab='home';state.session=null;render();
   const hs=history();
   const recent=tp140Recent('side-plank','home-level2',3);
   const coach=rf152Recommendation('side-plank');
   return {len:hs.length,secondExercises:Array.isArray(hs[1]?.exercises),thirdExercises:Array.isArray(hs[2]?.exercises),recent:recent.length,coachText:String(coach?.text||'')};
 });
 assert.deepEqual({len:recoveryState.len,secondExercises:recoveryState.secondExercises,thirdExercises:recoveryState.thirdExercises},{len:4,secondExercises:true,thirdExercises:true},'runtime history reads must normalize malformed rows without changing row indexes');
 assert.ok(recoveryState.recent>=1,'progression history lookup must survive malformed rows');
 assert.ok(recoveryState.coachText.length>0,'Coach recommendation lookup must survive malformed rows');
 const nav=page.getByRole('button',{name:'Napló',exact:true}).first();assert.equal(await nav.count(),1,'Napló navigation button missing');
 await nav.click();await page.waitForTimeout(80);
 assert.equal(errors.length,0,'opening Journal raised runtime error: '+errors.join('\n'));
 assert.equal(await page.getByText('Edzésnapló',{exact:true}).count()>0,true,'Journal title must render after clicking Napló');
 assert.equal(await page.locator('details.rf263-history').count(),4,'legacy and partial rows must not prevent Journal rendering');
 const first=page.locator('details.rf263-history').first();assert.equal(await first.count(),1,'saved workout must appear in Journal');
 await first.locator('summary').click();await page.waitForTimeout(60);
 const detail=await first.innerText();assert.match(detail,/Oldalsó plank/);assert.match(detail,/Bal 20 mp/);assert.match(detail,/Jobb 16 mp/);

 // Active-workout navigation must still be able to reach Journal after confirmation.
 await page.evaluate(()=>{const e=byId('plank');state.tab='plan';state.workout='A';state.session={workout:'A',dayId:'A',started:new Date().toISOString(),exercises:[{id:e.id,hu:e.hu,en:e.en,loadType:e.loadType,repUnit:e.repUnit||'mp',sets:[{set:1,weight:0,reps:'10',done:false}]}]};renderWorkout();});
 const workoutNav=page.getByRole('button',{name:'Napló',exact:true}).first();await workoutNav.click();await page.waitForTimeout(80);
 assert.equal(await page.locator('#tp2628Dialog').count(),0,'Journal detour from active workout must not get stuck behind a leave-workout confirmation');
 assert.equal(await page.getByText('Edzésnapló',{exact:true}).count()>0,true,'Journal must open immediately from an active workout');
 assert.equal(await page.evaluate(()=>state.session===null),true,'active in-memory session must be parked before Journal opens');
 assert.equal(await page.evaluate(()=>!!db.get('draft',null)?.session),true,'active workout must be preserved as a resumable draft before Journal opens');
 assert.equal(errors.length,0,'Journal navigation from active workout raised runtime error: '+errors.join('\n'));
 console.log('PASS: Journal opens with bilateral, legacy/partial history and active-workout navigation.');
 await page.close();
}finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
