'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((q,r)=>{const name=new URL(q.url,'http://local').pathname,file=path.resolve(root,'.'+(name==='/'?'/index.html':name));if(!file.startsWith(root+path.sep)){r.writeHead(403);return r.end();}fs.readFile(file,(e,d)=>{if(e){r.writeHead(404);return r.end();}r.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');r.end(d);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let b;try{
 b=await chromium.launch({args:['--no-sandbox']});const p=await b.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];p.on('pageerror',e=>errors.push(String(e)));p.on('dialog',d=>d.accept());
 await p.addInitScript(()=>{try{localStorage.setItem("repforge:onboarding128",JSON.stringify("skipped"))}catch(_){}});await p.goto('http://127.0.0.1:'+server.address().port);await p.waitForFunction(()=>TrainPilotBoot.finished);fs.mkdirSync('ui-evidence',{recursive:true});
 await p.evaluate(()=>{db.set('settings',{...settings(),profile:{age:30,goal:'fitness',experience:'beginner',minutes:45}});const at=days=>{const d=new Date();d.setHours(12,0,0,0);d.setDate(d.getDate()+days);return d.toISOString();};db.set('history',[-1,-2,-8].map((days,i)=>({id:'frame-'+i,started:at(days),finished:new Date(+new Date(at(days))+1800000).toISOString(),workout:'A',programId:'home-basic',exercises:[{...byId('db-floor-press'),sets:[{weight:10,reps:'10',done:true}]}]})));});
 const geometry=()=>p.evaluate(()=>{const root=document.querySelector('#tp155R4PanelHost .tp155-r4-panel')||document.querySelector('#app main'),main=document.querySelector('#tp155R4PanelHost main')||root,c=getComputedStyle(root),r=root.getBoundingClientRect(),m=getComputedStyle(main);return {x:r.x,width:r.width,border:c.borderTopWidth,color:c.borderTopColor,radius:c.borderRadius,shadow:c.boxShadow,contentLeft:main.getBoundingClientRect().x+parseFloat(m.borderLeftWidth)+parseFloat(m.paddingLeft),overflow:document.documentElement.scrollWidth-innerWidth};});
 const open=async route=>{await p.evaluate(route=>{tp155R4ClosePanel(false);if(['calendar','coach','settings'].includes(route)){go('home');tp155R4OpenPanel(route,document.activeElement);}else if(route==='progress')tp177OpenProgress();else if(route==='stats')tp177OpenStatistics();else go(route);},route);await p.waitForTimeout(160);};
 for(const width of [320,360,393,412])for(const lang of ['hu','en','de','ro'])for(const theme of ['classicBlue','blue']){
  const label=width+'/'+lang+'/'+theme;await p.setViewportSize({width,height:width===320?740:873});await p.evaluate(({lang,theme})=>{db.set('language',lang);state.session=null;db.set('draft',null);rf200SetTheme(theme);},{lang,theme});
  await open('coach');const reference=await geometry();
  for(const route of ['home','plan','health','programs','history','progress','stats','calendar','settings']){
   await open(route);const actual=await geometry();for(const key of ['x','width','border','color','radius','shadow','contentLeft'])assert.equal(actual[key],reference[key],label+'/'+route+' shared '+key);assert.ok(actual.overflow<=1,label+'/'+route+' no horizontal overflow');
   if(width===393&&lang==='hu'&&theme==='blue'&&['home','plan','health','progress','stats'].includes(route))await p.screenshot({path:'ui-evidence/page-frame-'+route+'-2697.png'});
  }
  await open('history');const tabs=p.locator('.tp177-journal-tabs');const pos=await tabs.boundingBox();
  for(const action of ['tp177OpenProgress()','tp177OpenStatistics()',"go('history')"]){
   await p.locator('.tp177-journal-tabs button[onclick="'+action+'"]').click();
   for(const wait of [0,180]){if(wait)await p.waitForTimeout(wait);const next=await tabs.boundingBox();for(const key of ['x','y','width','height'])assert.equal(next[key],pos[key],label+' Journal tabs stay fixed '+action+'/'+wait+'/'+key);}
  }
  await open('progress');const workouts=p.locator('.tp177-metric[data-metric="workouts"]');if(await workouts.getAttribute('aria-expanded')!=='true')await workouts.click();await p.waitForSelector('.tp177-inline-row');
  const arrow=p.locator('.tp177-inline-row>summary>i').first();assert.equal(await arrow.getAttribute('aria-hidden'),'true');assert.ok((await arrow.getAttribute('class')).includes('tp-global-chevron'));
  const paint=()=>arrow.evaluate(e=>{const c=getComputedStyle(e,'::before');return {content:c.content,font:c.font,color:c.color,clip:c.clipPath,rotation:getComputedStyle(e).transform};});
  const closed=await paint();assert.ok(closed.content.includes('›'));assert.equal(closed.clip,'none');
  await p.locator('.tp177-inline-row>summary').first().click();await p.waitForTimeout(180);assert.equal((await paint()).rotation,'matrix(0, 1, -1, 0, 0, 0)',label+' shared downward chevron');
  await p.locator('.tp177-inline-row>summary').first().click();await p.waitForTimeout(180);assert.equal((await paint()).rotation,closed.rotation,label+' shared chevron closes');
  if(width===393&&lang==='hu'&&theme==='blue')await p.screenshot({path:'ui-evidence/progress-details-arrows-2697.png'});
  await open('health');const health=await p.locator('.tp169-pulse-summary>.tp-global-chevron').evaluate(e=>{const c=getComputedStyle(e,'::before');return {content:c.content,font:c.font,color:c.color,clip:c.clipPath};});const {rotation,...progress}=closed;assert.deepEqual(progress,health,label+' detail arrow matches Health');
 }
 assert.deepEqual(errors,[]);console.log('PASS #101: equal Coach-reference frames/content gutters across all main pages, stable Journal tab position immediately and after animation, shared opening/closing detail chevrons across four sizes/languages and matte/vivid themes');
}finally{await b?.close();await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exitCode=1;});
