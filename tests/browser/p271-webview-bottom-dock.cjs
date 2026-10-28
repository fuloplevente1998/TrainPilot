'use strict';
const assert = require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
  const uri=new URL(req.url,'http://local'),name=uri.pathname==='/'?'/index.html':uri.pathname,file=path.resolve(root,'.'+name);
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
  fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);res.end();return}
    res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(data);
  });
});
(async()=>{
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  let browser;
  try{
    browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
    for(const width of [320,393]){
      const page=await browser.newPage({viewport:{width,height:width===320?740:873},locale:'hu-HU',timezoneId:'Europe/Budapest'});
      const errors=[];page.on('pageerror',error=>errors.push(String(error?.message||error)));
      page.on('dialog',dialog=>dialog.accept());
      await page.addInitScript(()=>{try{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'))}catch(_){}});
      const url='http://127.0.0.1:'+server.address().port+'/';
      // With no flag the stable 8-button main navigation must remain unchanged.
      await page.goto(url);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
      assert.equal(await page.locator('#tp271Dock').count(),0,width+': default has no experimental dock');
      assert.equal(await page.locator('.top.tp154-nav-grid .tp154-nav-cell:visible').count(),8,width+': all eight existing controls remain visible by default');

      // Opt-in preview: the same app and storage, just an alternate shell.
      await page.goto(url+'?tp271=1');
      await page.waitForFunction(()=>window.TrainPilotBoot?.finished && window.TrainPilotMockup271?.enabled);
      await page.locator('#tp271Dock .tp271-dock-button').first().waitFor();
      assert.equal(await page.locator('#tp271Dock .tp271-dock-button:visible').count(),5,width+': five lower buttons');
      assert.equal(await page.locator('.top.tp154-nav-grid .tp154-nav-cell:visible').count(),4,width+': four upper actions remain');
      assert.equal(await page.locator('#tp271Dock button[aria-current="page"]').count(),1,width+': single active route');
      const bounds=await page.evaluate(()=>{
        const nav=document.getElementById('tp271Dock').getBoundingClientRect();
        const main=document.querySelector('#app main').getBoundingClientRect();
        return {navTop:nav.top,navBottom:nav.bottom,mainBottom:main.bottom,navWidth:nav.width,viewport:innerWidth,overflows:document.documentElement.scrollWidth>innerWidth+1};
      });
      assert.ok(bounds.navTop>0&&bounds.navBottom<=740+(width===393?133:0),width+': dock stays inside viewport '+JSON.stringify(bounds));
      assert.ok(bounds.mainBottom<=bounds.navTop+5,width+': main does not overlap dock '+JSON.stringify(bounds));
      assert.equal(bounds.overflows,false,width+': no horizontal overflow');

      for(const [key,target] of [['workout','plan'],['log','history'],['progress','history'],['health','health'],['home','home']]){
        await page.locator('#tp271Dock button[data-tp271-target="'+key+'"]').click();
        await page.waitForFunction(key=>document.querySelector('#tp271Dock .tp271-dock-button.tp271-selected')?.dataset.tp271Target===key,key);
        assert.equal(await page.evaluate(()=>state.tab),target,width+'/'+key+': canonical source route');
        if(key==='progress')assert.equal(await page.evaluate(()=>state.tp177JournalView),'progress',width+': real Journal Progress');
        assert.equal(await page.locator('#tp271Dock [aria-current="page"]').count(),1,width+'/'+key+': exactly one active');
      }

      // All original secondary functions stay reachable in the compact top row.
      await page.locator('.top .tp151-nav-item[onclick*="programs"]').click();
      assert.equal(await page.evaluate(()=>state.tab),'programs',width+': Programs reachable');
      await page.locator('.top .tp151-nav-item[onclick*="calendar"]').click();
      assert.equal(await page.locator('#tp155R4PanelHost').getAttribute('data-panel'),'calendar',width+': Calendar opens');
      await page.evaluate(()=>tp155R4ClosePanel(false));
      await page.locator('.top .tp154-coach-action').click();
      assert.equal(await page.locator('#tp155R4PanelHost').getAttribute('data-panel'),'coach',width+': Coach opens');
      await page.evaluate(()=>tp155R4ClosePanel(false));
      await page.locator('.top .tp154-settings-action').click();
      assert.equal(await page.locator('#tp155R4PanelHost').getAttribute('data-panel'),'settings',width+': Settings opens');
      await page.evaluate(()=>tp155R4ClosePanel(false));

      // The dock never covers ongoing workout controls; the existing engine
      // remains the only author of the live session.
      await page.evaluate(()=>{go('plan');startWorkout(activeProgram().days[0].id)});
      await page.waitForFunction(()=>document.documentElement.classList.contains('tp271-has-workout'));
      assert.equal(await page.locator('#tp271Dock:visible').count(),0,width+': dock hidden during live workout');
      assert.ok(await page.evaluate(()=>!!state.session),width+': real workout session survives');
      await page.evaluate(()=>{state.session=null;state.workout=null;db.set('draft',null);go('home')});
      await page.waitForFunction(()=>!document.documentElement.classList.contains('tp271-has-workout'));
      assert.equal(await page.locator('#tp271Dock:visible').count(),1,width+': dock returns after workout');

      await page.evaluate(()=>{db.set('language','en');go('home')});
      await page.waitForFunction(()=>document.querySelector('#tp271Dock .tp271-dock-button[data-tp271-target="progress"] .tp271-dock-label')?.textContent==='Progress');
      assert.equal(await page.locator('#tp271Dock .tp271-dock-button:visible').count(),5,width+': translated five buttons');
      if(width===393){
        fs.mkdirSync('ui-evidence',{recursive:true});
        await page.screenshot({path:'ui-evidence/pr271-opt-in-bottom-dock-home-393.png'});
      }
      assert.deepEqual(errors,[],width+': no JS runtime errors');
      await page.close();
    }
    console.log('PASS PR271 P1b: opt-in lower five-tab dock, all secondary entries, real Progress state, live workout guard, language and default main fallback.');
  }finally{await browser?.close();await new Promise(resolve=>server.close(resolve))}
})().catch(e=>{console.error(e);process.exitCode=1});
