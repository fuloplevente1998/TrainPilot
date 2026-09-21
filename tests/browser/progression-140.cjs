const assert=require('node:assert/strict');
const fs=require('node:fs');const path=require('node:path');const http=require('node:http');const {chromium}=require('playwright');
(async()=>{const root=path.resolve('www');const server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});const page=await browser.newPage({locale:'hu-HU'});await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
 const out=await page.evaluate(()=>{
  const mk=(id,reps,weight,effort='good',programId='home-basic')=>({programId,started:new Date(Date.now()-Math.random()*1e8).toISOString(),finished:new Date().toISOString(),exercises:[{id,loadType:byId(id)?.loadType,effort,sets:reps.map((r,i)=>({set:i+1,reps:r,weight,done:true}))}]});
  const rec=(id,target,rows)=>{db.set('history',rows);state.session={programId:'home-basic',exercises:[]};const e={id,prescription:{sets:rows[0]?.exercises?.[0]?.sets?.length||2,reps:target},sets:[]};const r=rf152Recommendation(id,e);state.session=null;return r};
  return {
   rir:tp140ParseTarget('2–3 RIR'),
   bulgarian:rec('bulgarian-split-squat','8–12',[mk('bulgarian-split-squat',[10,10,8],5,'good')]),
   pulloverOne:rec('db-pullover','10–15',[mk('db-pullover',[16,16,16],5,'light')]),
   pulloverStable:rec('db-pullover','10–15',[mk('db-pullover',[16,16,16],5,'light'),mk('db-pullover',[15,15,15],5,'good')]),
   lateral:rec('lateral-raise','12–15',[mk('lateral-raise',[12,12],5,'good')]),
   triceps:rec('oh-triceps','10–15',[mk('oh-triceps',[10,12],5,'good')]),
   row:rec('barbell-row','8–12',[mk('barbell-row',[15,15],10,'good')]),
   pushup:rec('pushup','2–3 RIR',[mk('pushup',[8,8],0,'good')]),
   crunch:rec('crunch','15–20',[mk('crunch',[17,17],0,'good')]),
   pain:rec('db-pullover','10–15',[mk('db-pullover',[16,16,16],5,'pain')]),
   hard:rec('db-pullover','10–15',[mk('db-pullover',[8,8,8],5,'hard')])
  };
 });
 assert.equal(out.rir.kind,'rir');assert.equal(out.rir.min,null);assert.equal(out.rir.max,null);assert.equal(out.rir.rirMin,2);assert.equal(out.rir.rirMax,3);
 assert.equal(out.bulgarian.action,'reps');assert.equal(out.bulgarian.weight,5);assert.equal(out.bulgarian.autoApply,false);
 assert.equal(out.pulloverOne.action,'increase');assert.equal(out.pulloverOne.autoApply,false);assert.ok(out.pulloverOne.weight>5);
 assert.equal(out.pulloverStable.action,'increase');assert.equal(out.pulloverStable.autoApply,true);assert.ok(out.pulloverStable.weight>5);
 assert.equal(out.lateral.action,'reps');assert.equal(out.lateral.weight,5);
 assert.equal(out.triceps.action,'reps');assert.equal(out.triceps.weight,5);
 assert.equal(out.row.action,'increase');assert.equal(out.row.autoApply,false);assert.ok(out.row.weight>10);
 assert.notEqual(out.pushup.action,'increase');assert.equal(out.pushup.target.kind,'rir');assert.match(out.pushup.text,/RIR/);
 assert.equal(out.crunch.action,'reps');assert.equal(out.pain.action,'pain');assert.notEqual(out.hard.action,'increase');
 console.log('PASS: TrainPilot 1.4.0 Progressive Overload 2.0 real decision cases.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exit(1)});
