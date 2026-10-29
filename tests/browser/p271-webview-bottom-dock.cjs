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
    for(const width of [320,360,393,412]){
      const page=await browser.newPage({viewport:{width,height:width===320?740:873},locale:'hu-HU',timezoneId:'Europe/Budapest'});
      const errors=[];page.on('pageerror',error=>errors.push(String(error?.message||error)));
      page.on('dialog',dialog=>dialog.accept());
      await page.addInitScript(()=>{try{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'))}catch(_){}});
      const url='http://127.0.0.1:'+server.address().port+'/';
      await page.goto(url);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
      assert.equal(await page.locator('.top.tp154-nav-grid .tp154-nav-cell:visible').count(),8,width+': default eight-button navigation');
      await page.goto(url+'?tp271=1');
      await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilotMockup271?.enabled);
      const nav=page.locator('.top.tp154-nav-grid');
      assert.equal(await nav.locator('.tp154-nav-cell:visible').count(),8,width+': mockup retains all eight original destinations');
      assert.equal(await page.locator('#tp271Dock,#tp271Header,#tp271WorkoutQuickLinks').count(),0,width+': no parallel or buried replacement navigation');
      const grid=await nav.evaluate(el=>({columns:getComputedStyle(el).gridTemplateColumns.split(' ').length,overflow:document.documentElement.scrollWidth-innerWidth}));
      assert.equal(grid.columns,4,width+': original 2x4 layout');
      assert.ok(grid.overflow<=1,width+': navigation fits viewport');
      for(const key of ['plan','programs','history','health','home']){
        await nav.locator('.tp151-nav-item[onclick="go(\''+key+'\')"]').click();
        assert.equal(await page.evaluate(()=>state.tab),key,width+'/'+key+': original route executes');
      }
      for(const [selector,panel] of [['.tp151-nav-item[onclick="go(\'calendar\')"]','calendar'],['.tp154-coach-action','coach'],['.tp154-settings-action','settings']]){
        await nav.locator(selector).click();
        await page.locator('#tp155R4PanelHost[data-panel="'+panel+'"]').waitFor();
        await page.evaluate(()=>tp155R4ClosePanel(false));
      }
      await nav.locator('.tp151-nav-item[onclick="go(\'history\')"]').click();
      await page.locator('.tp177-journal-tabs button[onclick="tp177OpenProgress()"]:visible').click();
      await page.waitForSelector('main.tp177-progress');
      assert.equal(await page.evaluate(()=>state.tp177JournalView),'progress',width+': Progress remains in Journal');
      await page.evaluate(()=>{go('plan');startWorkout(activeProgram().days[0].id)});
      await page.waitForFunction(()=>document.documentElement.classList.contains('tp271-has-workout'));
      const workout=await page.evaluate(()=>({height:document.querySelector('.top').getBoundingClientRect().height,top:document.querySelector('main.tp153-workout').getBoundingClientRect().top,session:!!state.session}));
      assert.equal(workout.height,0,width+': no empty workout header');
      assert.ok(workout.top<=24&&workout.session,width+': real workout starts near top');
      if(width===393){fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/pr271-active-workout-no-header-gap-393.png',animations:'disabled'});}
      await page.evaluate(()=>{state.session=null;state.workout=null;db.set('draft',null);go('home')});
      await page.waitForFunction(()=>!document.documentElement.classList.contains('tp271-has-workout'));
      assert.equal(await nav.locator('.tp154-nav-cell:visible').count(),8,width+': navigation returns after workout');
      assert.equal(await page.locator('#rf223Today .tp5-today-metric').count(),6,width+': six real Home values remain');
      for(const lang of ['hu','en','de','ro']){
        await page.evaluate(l=>{rf212SetLang(l);go('home')},lang);
        assert.equal(await nav.locator('.tp154-nav-cell:visible').count(),8,width+'/'+lang+': all destinations localized and reachable');
      }
      await page.evaluate(()=>{rf212SetLang('hu');go('plan')});
      await page.evaluate(async()=>{await Promise.all(document.querySelector('#app main').getAnimations().filter(a=>a.effect.getTiming().iterations!==Infinity).map(a=>a.finished));});
      if(width===393)await page.screenshot({path:'ui-evidence/pr271-original-navigation-mockup-393.png',animations:'disabled'});
      const frames=await page.evaluate(()=>{go('home');return document.querySelector('#app main').getAnimations().find(a=>a.animationName==='tp271PageFade')?.effect.getKeyframes()});
      assert.ok(frames?.length>=2&&Number(frames[0].opacity)===0&&Number(frames.at(-1).opacity)===1,width+': opacity fade retained');
      await page.emulateMedia({reducedMotion:'reduce'});await page.evaluate(()=>go('plan'));
      assert.equal(await page.locator('#app main').evaluate(el=>getComputedStyle(el).animationName),'none',width+': reduced motion preserved');
      assert.deepEqual(errors,[],width+': no runtime errors');
      await page.close();
    }
    console.log('PASS PR271: original 2x4 navigation, eight real routes, no workout gap, localized Home and fade.');
  }finally{await browser?.close();await new Promise(resolve=>server.close(resolve))}
})().catch(e=>{console.error(e);process.exitCode=1});
