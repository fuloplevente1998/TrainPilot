'use strict';
// #236 P1.2: batch first fit after final startup render; don't refit on
// the ResizeObserver's initial notification. Later real resizes must still fit.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),source=fs.readFileSync(path.join(root,'app.js'),'utf8');
assert.ok(source.includes('tp107ScheduleFit();\n});\n// @endsection training-107.js'));
const instrumented=source.replace('var tp107FitViews=function(){','var tp107FitViews=function(){window.__tp107FitCalls=(window.__tp107FitCalls||0)+1;');
assert.notEqual(instrumented,source);
const server=http.createServer((q,r)=>{
 const file=path.resolve(root,'.'+new URL(q.url,'http://local').pathname.replace(/\/$/,'/index.html'));
 if(!file.startsWith(root+path.sep)){r.writeHead(403);return r.end();}
 if(file.endsWith('/app.js')){r.setHeader('Content-Type','text/javascript');return r.end(instrumented);}
 fs.readFile(file,(e,b)=>{if(e){r.writeHead(404);return r.end();}r.setHeader('Content-Type',file.endsWith('.css')?'text/css':file.endsWith('.js')?'text/javascript':'text/html');r.end(b)});
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
  browser=await chromium.launch({args:['--no-sandbox']});
  for(const count of [0,200]){
   const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
   const errors=[];page.on('pageerror',e=>errors.push(e.message));
   await page.addInitScript(count=>{
    localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));
    localStorage.setItem('repforge:history',JSON.stringify(Array.from({length:count},(_,i)=>({id:'fit-'+i,started:new Date(Date.now()-86400000*(i+1)).toISOString(),finished:new Date(Date.now()-86400000*i).toISOString(),exercises:[]}))));
   },count);
   await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
   await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(()=>requestAnimationFrame(resolve)))));
   const initial=await page.evaluate(()=>({calls:window.__tp107FitCalls||0,home:!!document.querySelector('#app main.rf221-home'),nav:document.documentElement.style.getPropertyValue('--tp107-nav-height')}));
   assert.ok(initial.calls<=1,'at most one initial layout fit: '+JSON.stringify(initial));
   assert.ok(initial.calls>=1,'at least one actual fit: '+JSON.stringify(initial));
   assert.ok(initial.nav.endsWith('px'),'nav height measured: '+JSON.stringify(initial));
   await page.setViewportSize({width:360,height:740});
   await page.waitForFunction(initial=>window.__tp107FitCalls>initial,{timeout:3000},initial.calls);
   assert.deepEqual(errors,[]);await page.close();
  }
  console.log('PASS #236 P1.2: one startup fit, no initial ResizeObserver refit, resize still handled, empty/200 history');
 }finally{await browser?.close();server.close();}
})().catch(e=>{console.error(e);process.exitCode=1;server.close()});
