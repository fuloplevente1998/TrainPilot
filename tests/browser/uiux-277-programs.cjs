'use strict';
// PR #277 first implementation slice: compact Programs cards, theme glyphs,
// preserved program selection/expansion and existing 2x4 navigation.
const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const css=fs.readFileSync(path.join(root,'uiux-277.css'),'utf8');
const html=fs.readFileSync(path.join(root,'index.html'),'utf8');
assert.match(html,/href="uiux-277\.css"/,'Stylesheet must be loaded by the shipped page');
assert.match(css,/main\.tp150-programs-compact/,'Styles must be scoped to existing Programs page');
assert.doesNotMatch(css,/(?:^|\n)\s*(?:\.top|\.timer|main\s*\{|#app\s*\{)/,'No shared navigation/timer changes');
const server=http.createServer((req,res)=>{
 let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';
 const file=path.resolve(root,'.'+p);
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}
 fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}
  res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d);
 });
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  for(const width of [320,360,393]){
   const page=await browser.newPage({viewport:{width,height:873},locale:'hu-HU'});
   const errors=[];page.on('pageerror',e=>errors.push(e.message));
   page.on('dialog',d=>d.accept());
   await page.addInitScript(()=>{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'))});
   await page.goto('http://127.0.0.1:'+server.address().port+'/');
   await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
   for(const theme of ['classicBlue','blue']){
    await page.evaluate(t=>{rf200SetTheme(t);go('programs')},theme);
    await page.waitForSelector('main.tp150-programs-compact details.tp152-program-card');
    const snapshot=await page.evaluate(()=>{
     const cards=[...document.querySelectorAll('main.tp150-programs-compact details.tp152-program-card')];
     const nav=document.querySelector('.top.tp154-nav-grid');
     const icon=e=>e.querySelector('.tp277-program-icon');
     const selected=cards.filter(e=>e.classList.contains('active-program'));
     return {count:cards.length,active:selected.length,navCount:nav?.querySelectorAll('.tp154-nav-cell').length,
       icons:cards.map(e=>({kind:icon(e)?.dataset.kind,svg:icon(e)?.querySelector('svg')?.getAttribute('viewBox'),
         stroke:icon(e)?.querySelector('svg')&&getComputedStyle(icon(e)).color,
         buttonH:e.querySelector('.rf103-program-activate')?.getBoundingClientRect().height,
         width:e.getBoundingClientRect().width,
         overflow:e.scrollWidth-e.clientWidth})),
       bodyOverflow:document.documentElement.scrollWidth-document.documentElement.clientWidth};
    });
    assert.ok(snapshot.count>=3,width+'/'+theme+': should show program cards');
    assert.equal(snapshot.navCount,8,width+'/'+theme+': original 2x4 nav remains');
    assert.equal(snapshot.active,1,width+'/'+theme+': current program remains highlighted');
    assert.ok(snapshot.bodyOverflow<=2,width+'/'+theme+': no horizontal viewport overflow');
    for(const row of snapshot.icons){
     assert.ok(['general','strength','muscle','functional','endurance'].includes(row.kind), 'typed fallback icon');
     assert.equal(row.svg,'0 0 24 24','semantic SVG must be present');
     assert.ok(row.stroke,'icon must inherit a theme-compatible color');
     assert.ok(row.buttonH>=43,width+'/'+theme+': activation remains tappable');
     assert.ok(row.overflow<=2,width+'/'+theme+': program card must not overflow');
    }
    const card=page.locator('details.tp152-program-card').first();
    await card.locator(':scope > summary').click();
    assert.ok(await card.locator('.tp152-program-card-body[data-tp7-hydrated="true"]').count(), 'tap must still open lazily hydrated card');
    await card.locator(':scope > summary').click();
   }
   assert.equal(errors.length,0,'browser errors: '+errors.join('; '));
   await page.close();
  }
  console.log('PASS PR277 compact Programs cards and theme icons at 320/360/393 px');
 }finally{await browser?.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exitCode=1});
