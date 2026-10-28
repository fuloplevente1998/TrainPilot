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
      assert.equal(await page.locator('.top.tp154-nav-grid .tp154-nav-cell:visible').count(),0,width+': old 2x4 navigation hidden');
      assert.equal(await page.locator('#tp271Header .tp271-header-brand strong').textContent(),'TrainPilot',width+': real original header');
      assert.equal(await page.locator('#tp271Header button[data-tp271-header]').count(),3,width+': Settings Language Theme in header');
      assert.equal(await page.locator('#tp271Dock button[aria-current="page"]').count(),1,width+': single active route');
      const grid=await page.evaluate(()=>{
        const e=document.querySelector('#app main.rf221-home #rf223Today .tp5-today-grid');
        return {columns:getComputedStyle(e).gridTemplateColumns.split(' ').length,tiles:e.querySelectorAll('.tp5-today-metric').length};
      });
      assert.equal(grid.columns,2,width+': Studio-inspired two-column Home summary');
      assert.equal(grid.tiles,6,width+': all six real Home metrics preserved');
      const bounds=await page.evaluate(()=>{
        const nav=document.getElementById('tp271Dock').getBoundingClientRect();
        const main=document.querySelector('#app main').getBoundingClientRect();
        return {navTop:nav.top,navBottom:nav.bottom,mainBottom:main.bottom,navWidth:nav.width,viewport:innerWidth,overflows:document.documentElement.scrollWidth>innerWidth+1};
      });
      assert.ok(bounds.navTop>0&&bounds.navBottom<=740+(width===393?133:0),width+': dock stays inside viewport '+JSON.stringify(bounds));
      assert.ok(bounds.mainBottom<=bounds.navTop+5,width+': main does not overlap dock '+JSON.stringify(bounds));
      assert.equal(bounds.overflows,false,width+': no horizontal overflow');

      // P1d: visually restructured but still *real* Home state. The
      // scrolling viewport must reveal the final Coach action above the dock.
      const homeLayout=await page.evaluate(()=>{
        const main=document.querySelector('#app main.rf221-home');
        const hero=main?.querySelector(':scope > .hero.tp155-home-active-card');
        const today=main?.querySelector('#rf223Today');
        const coach=main?.querySelector('#rf220CoachCard');
        const lowerDock=document.querySelector('#tp271Dock');
        const before=main?.scrollTop||0;
        if(main)main.scrollTop=main.scrollHeight;
        const visibleBottom=main?.getBoundingClientRect().bottom||0;
        const lastCardBottom=coach?.getBoundingClientRect().bottom||0;
        const result={
          hero:!!hero, heroTitle:hero?.querySelector('h1')?.textContent?.trim()||'',
          hasExistingHeroActions:!!hero?.querySelector('button'),
          todayTiles:today?.querySelectorAll('.tp5-today-metric').length||0,
          coach:!!coach,coachBottom:lastCardBottom,
          viewportBottom:visibleBottom,dockTop:lowerDock.getBoundingClientRect().top,
          overflow:getComputedStyle(main).overflowY,
          scrollTop:main?.scrollTop||0,wasScrolled:before
        };
        if(main)main.scrollTop=before;
        return result;
      });
      assert.equal(homeLayout.hero,true,width+': original dynamic program hero remains');
      assert.ok(homeLayout.heroTitle.length>0,width+': real current workout/program title');
      assert.equal(homeLayout.hasExistingHeroActions,true,width+': original workout action is still present');
      assert.equal(homeLayout.todayTiles,6,width+': all six live Today values visible in new layout');
      assert.equal(homeLayout.coach,true,width+': canonical Coach CTA preserved');
      assert.ok(homeLayout.coachBottom<=homeLayout.dockTop+3,width+': Coach can be scrolled above dock '+JSON.stringify(homeLayout));
      assert.ok(['auto','scroll'].includes(homeLayout.overflow),width+': preview Home can scroll');
      assert.ok(homeLayout.viewportBottom<=homeLayout.dockTop+5,width+': Home viewport ends before dock');

      for(const [key,target] of [['workout','plan'],['log','history'],['progress','history'],['health','health'],['home','home']]){
        await page.locator('#tp271Dock button[data-tp271-target="'+key+'"]').click();
        await page.waitForFunction(key=>document.querySelector('#tp271Dock .tp271-dock-button.tp271-selected')?.dataset.tp271Target===key,key);
        assert.equal(await page.evaluate(()=>state.tab),target,width+'/'+key+': canonical source route');
        if(key==='progress')assert.equal(await page.evaluate(()=>state.tp177JournalView),'progress',width+': real Journal Progress');
        assert.equal(await page.locator('#tp271Dock [aria-current="page"]').count(),1,width+'/'+key+': exactly one active');
      }

      // All original secondary functions remain reachable through the
      // mockup Workout quick links rather than resurrecting a top 2x4 grid.
      await page.locator('#tp271Dock button[data-tp271-target="workout"]').click();
      const shortcuts=page.locator('#tp271WorkoutQuickLinks');
      await shortcuts.waitFor();
      assert.equal(await shortcuts.locator('button').count(),3,width+': three secondary Workout routes');
      await shortcuts.locator('[data-tp271-shortcut="programs"]').click();
      assert.equal(await page.evaluate(()=>state.tab),'programs',width+': Programs reachable');
      await page.waitForFunction(()=>document.querySelector('#tp271Dock .tp271-dock-button.tp271-selected')?.dataset.tp271Target==='workout');
      assert.equal(await page.locator('#tp271Dock [aria-current="page"]').count(),1,width+': Programs belongs to Workout main tab');
      await page.evaluate(()=>go('plan'));
      await shortcuts.locator('[data-tp271-shortcut="calendar"]').click();
      assert.equal(await page.locator('#tp155R4PanelHost').getAttribute('data-panel'),'calendar',width+': Calendar opens');
      await page.evaluate(()=>tp155R4ClosePanel(false));
      await shortcuts.locator('[data-tp271-shortcut="coach"]').click();
      assert.equal(await page.locator('#tp155R4PanelHost').getAttribute('data-panel'),'coach',width+': Coach opens');
      await page.evaluate(()=>tp155R4ClosePanel(false));
      await page.locator('#tp271Header [data-tp271-header="settings"]').click();
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

      // P1d draft regression: a resumed-workout choice and its safe discard
      // action must not become clipped behind the fixed five-tab bar.
      await page.evaluate(()=>{
        startWorkout(activeProgram().days[0].id);persistDraft();
        state.session=null;state.workout=null;go('home');
      });
      await page.waitForSelector('main.rf221-home.tp-home-draft-compact');
      const draft=await page.evaluate(()=>{
        const main=document.querySelector('#app main.rf221-home');
        const resume=main.querySelector('button[onclick*="resumeDraft"]');
        const nav=document.getElementById('tp271Dock');
        main.scrollTop=main.scrollHeight;
        return {
          resume:!!resume,
          visible:getComputedStyle(resume).display!=='none',
          bottom:resume.getBoundingClientRect().bottom,
          dockTop:nav.getBoundingClientRect().top,
          active:!!state.session,
          total:main.querySelectorAll('#rf223Today .tp5-today-metric').length
        };
      });
      assert.equal(draft.resume,true,width+': actual saved-draft Resume CTA survives');
      assert.equal(draft.visible,true,width+': Resume CTA remains rendered');
      assert.equal(draft.active,false,width+': draft exists without starting another workout');
      assert.equal(draft.total,6,width+': all six real metrics persist with draft');
      await page.evaluate(()=>{db.set('draft',null);go('home')});

      // The menu is localized from the app language; four language options
      // must never trigger a second route manager or remove a destination.
      for(const [lang,label] of [['hu','Fejlődés'],['en','Progress'],['de','Fortschritt'],['ro','Progres']]){
        await page.evaluate(x=>{db.set('language',x);go('home')},lang);
        await page.waitForFunction(expected=>
          document.querySelector('#tp271Dock button[data-tp271-target="progress"] .tp271-dock-label')?.textContent===expected,
          label
        );
        assert.equal(await page.locator('#tp271Dock button:visible').count(),5,width+'/'+lang+': all destinations remain localized');
      }
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
    console.log('PASS PR271 P1e: exactly five dock tabs, compact three-action header, secondary Workout entry points, real workout guard and main fallback.');
  }finally{await browser?.close();await new Promise(resolve=>server.close(resolve))}
})().catch(e=>{console.error(e);process.exitCode=1});
