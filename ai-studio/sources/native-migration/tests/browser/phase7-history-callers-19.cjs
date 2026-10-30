const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let b;try{
 b=await chromium.launch({headless:true,args:['--no-sandbox']});const p=await b.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
 await p.goto('http://127.0.0.1:'+server.address().port+'/');await p.waitForFunction(()=>window.TrainPilotBoot?.finished);
 const result=await p.evaluate(()=>{
  const rows=[];for(let i=0;i<240;i++){const end=new Date(Date.now()-i*86400000),start=new Date(end.getTime()-3600000);rows.push({id:'p7-call-'+i,workout:'A',dayId:'A',programId:'home-basic',programName:'Phase 7',started:start.toISOString(),finished:end.toISOString(),exercises:[{id:'db-squat',loadType:'per_hand',repUnit:'ism.',sets:[{set:1,weight:10,reps:'10',done:true}]}],photos:[]})}db.set('history',rows);state.session=null;
  const base=window.history,counts={};window.history=function(){const stack=String(new Error().stack||'').split('\n').slice(2,6).map(x=>x.trim()).join(' <= ');counts[stack]=(counts[stack]||0)+1;return base.apply(this,arguments)};
  go('home');const home=Object.entries(counts).sort((a,b)=>b[1]-a[1]).slice(0,20);for(const k of Object.keys(counts))delete counts[k];
  window.tp155R4OpenPanel('coach',document.activeElement);const coach=Object.entries(counts).sort((a,b)=>b[1]-a[1]).slice(0,20);
  return {home,coach};
 });
 console.log('PHASE7_HISTORY_CALLERS '+JSON.stringify(result));
}finally{await b?.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
