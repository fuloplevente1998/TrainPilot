'use strict';
// #236 Settings must become visible with the tap, not after a panel/selection slide.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{
 const p=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
 if(!p.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(p,(err,bytes)=>{if(err){res.writeHead(404);return res.end();}res.setHeader('Content-Type',p.endsWith('.js')?'text/javascript':p.endsWith('.css')?'text/css':'text/html');res.end(bytes)});
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
 browser=await chromium.launch({args:['--no-sandbox']});
 const p=await browser.newPage({viewport:{width:393,height:873},isMobile:true,hasTouch:true,locale:'hu-HU'});
 const errors=[];p.on('pageerror',e=>errors.push(e.message));
 await p.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
 await p.goto('http://127.0.0.1:'+server.address().port+'/');await p.waitForFunction(()=>window.TrainPilotBoot?.finished);
 for(const width of [320,393]){
  await p.setViewportSize({width,height:873});
  for(const previous of ['home','calendar','coach']){
   const result=await p.evaluate(previous=>{
    window.tp155R4ClosePanel?.(false);go(previous);
    // Directly test in the same task as the Settings navigation.
    go('settings');
    const host=document.querySelector('#tp155R4PanelHost'),panel=host?.querySelector('.tp155-r4-panel'),main=panel?.querySelector('main'),marker=document.querySelector('.top .tp137-selection');
    return {which:host?.dataset.panel,settled:panel?.classList.contains('tp155-r4-panel-settled'),
      page:!!main,animations:host?.getAnimations({subtree:true}).filter(a=>a.playState==='running').map(a=>a.animationName)||[],
      marker:marker?.getAnimations().filter(a=>a.playState==='running').length||0,
      transform:panel?getComputedStyle(panel).transform:null,overflow:document.documentElement.scrollWidth-innerWidth};
   },previous);
   assert.equal(result.which,'settings',JSON.stringify(result));
   assert.equal(result.settled,true,JSON.stringify(result));
   assert.equal(result.page,true,JSON.stringify(result));
   assert.deepEqual(result.animations,[],JSON.stringify(result));
   assert.equal(result.marker,0,JSON.stringify(result));
   assert.equal(result.transform,'none',JSON.stringify(result));
   assert.ok(result.overflow<=1,JSON.stringify(result));
   await p.evaluate(()=>go('settings'));
   assert.equal(await p.locator('#tp155R4PanelHost').count(),0,'second tap closes Settings immediately');
  }
 }
 assert.deepEqual(errors,[]);
 console.log('PASS #236: Settings immediate after Home/Calendar/Coach at 320/393, no in-flight reveal or nav slide, closes on second tap');
 }finally{await browser?.close();server.close();}
})().catch(e=>{console.error(e);process.exitCode=1;server.close();});
