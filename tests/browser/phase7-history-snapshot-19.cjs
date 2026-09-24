const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let b;try{
 b=await chromium.launch({headless:true,args:['--no-sandbox']});const p=await b.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
 await p.goto('http://127.0.0.1:'+server.address().port+'/');await p.waitForFunction(()=>window.TrainPilotBoot?.finished);
 const makeRows=n=>p.evaluate(n=>{const rows=[];for(let i=0;i<n;i++){const end=new Date(Date.now()-i*86400000),start=new Date(end.getTime()-3600000);rows.push({id:'p7-snap-'+i,workout:i%2?'B':'A',dayId:i%2?'B':'A',programId:'home-basic',programName:'Snapshot',started:start.toISOString(),finished:end.toISOString(),exercises:[{id:'db-squat',loadType:'per_hand',repUnit:'ism.',effort:i===0?'easy':null,sets:[{set:1,weight:10,reps:'10',done:true}]}],photos:[]})}db.set('history',rows);state.session=null},n);
 await makeRows(120);
 const homeCalls=await p.evaluate(()=>{let n=0;const base=window.history;window.history=function(){n++;return base.apply(this,arguments)};go('home');return n});
 assert.ok(homeCalls<=4,'Home should use one operation-scoped history snapshot, calls='+homeCalls);
 const coach=await p.evaluate(()=>{let n=0;const live=window.history;window.history=function(){n++;return live.apply(this,arguments)};window.tp155R4OpenPanel('coach',document.activeElement);return {calls:n,text:document.querySelector('#tp155R4PanelHost .tp151-coach')?.innerText||'',flag:window.TrainPilotPhase7Performance}});
 assert.ok(coach.calls<=4,'Coach open should reuse one history snapshot, calls='+coach.calls);assert.match(coach.text,/TrainPilot Coach/);assert.equal(coach.flag.historySnapshot,'operation-scoped');assert.equal(coach.flag.persistentCache,false);
 await p.evaluate(()=>window.tp155R4ClosePanel?.(false));
 await makeRows(2);const after=await p.evaluate(()=>{go('home');return history().map(x=>x.id)});
 assert.deepEqual(after,['p7-snap-0','p7-snap-1'],'a new operation must observe newly stored history; no persistent cache allowed');
 console.log('PASS Phase 7 operation-scoped history snapshot, Coach/Home freshness preserved');
}finally{await b?.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
