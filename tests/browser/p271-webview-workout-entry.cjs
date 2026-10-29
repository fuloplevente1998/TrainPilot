'use strict';
// PR #271 P2a – real Workout entry, native-preserving design test.
// Run the canonical app in both legacy and opt-in mockup shells.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const raw=new URL(req.url,'http://localhost'),file=path.resolve(root,'.'+(raw.pathname==='/'?'/index.html':raw.pathname));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
 fs.readFile(file,(err,b)=>{if(err){res.writeHead(404);res.end();return}
 res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(b)});
});
(async()=>{
 await new Promise(ok=>server.listen(0,'127.0.0.1',ok));
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  for(const width of [320,360,393,412]){
   const page=await browser.newPage({viewport:{width,height:width===320?740:873},locale:'hu-HU',timezoneId:'Europe/Budapest'});
   const errors=[];page.on('pageerror',err=>errors.push(err.message||String(err)));page.on('dialog',dialog=>dialog.accept());
   await page.addInitScript(()=>{try{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'))}catch(_){}});
   const url='http://127.0.0.1:'+server.address().port+'/';
   // Legacy mode is intentionally identical even with P2a CSS loaded.
   await page.goto(url);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
   await page.evaluate(()=>{go('plan')});
   await page.waitForSelector('main.tp152-plan');
   assert.equal(await page.locator('#tp271Dock').count(),0,'baseline: no new dock');
   assert.equal(await page.locator('main.tp152-plan .tp150-quick-entry-minimal').count(),1,'baseline: canonical quick entry untouched');
   await page.goto(url+'?tp271=1');await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilotMockup271?.enabled);
   for(const theme of ['classicBlue','yellow']){
    await page.evaluate(t=>{rf200SetTheme(t);state.session=null;state.workout=null;db.set('draft',null);go('plan')},theme);
    await page.waitForSelector('main.tp152-plan .tp152-active-program');
    const plan=await page.evaluate(()=>{
      const main=document.querySelector('#app main.tp152-plan'),quick=main.querySelector('.tp150-quick-entry-minimal'),
        frame=main.querySelector('.tp124-active-program-frame'),training=frame.querySelector('.tp1511-training'),
        program=frame.querySelector('.tp152-active-program'),day=program.querySelector('.tp152-day'),
        button=quick.querySelector('button');
      const q=quick.getBoundingClientRect(),fr=frame.getBoundingClientRect(),tap=button.getBoundingClientRect();
      return {
       quick:!!quick,training:!!training,program:!!program,days:program.querySelectorAll('.tp152-day').length,
       frameBorder:getComputedStyle(frame).borderTopWidth,
       frameFill:getComputedStyle(frame).backgroundColor,
       programBorder:getComputedStyle(program).borderTopWidth,
       headingFill:getComputedStyle(program.querySelector(':scope > summary')).backgroundColor,
       dayWidth:day.getBoundingClientRect().width,frameWidth:fr.width,
       startText:button.textContent.trim(),startAction:button.getAttribute('onclick'),
       choice:program.querySelector('.tp150-change-program')?.getAttribute('onclick'),
       dayStart:day.querySelector('.tp152-start-day')?.getAttribute('onclick'),
       quickTop:q.top,frameTop:fr.top,buttonHeight:tap.height,viewportBottom:innerHeight,
       overflow:document.documentElement.scrollWidth-innerWidth,
       background:getComputedStyle(quick).backgroundColor,
       quickColumns:getComputedStyle(quick).gridTemplateColumns
      };
    });
    assert.ok(plan.quick&&plan.training&&plan.program,width+'/'+theme+': original training/quick/program present');
    assert.ok(plan.days>=1,width+'/'+theme+': canonical workout days remain');
    assert.ok(plan.startText.length>0&&/tp155R4TogglePanel/.test(plan.startAction),width+'/'+theme+': original Quick panel action');
    assert.ok(/go\('programs'\)/.test(plan.choice),width+'/'+theme+': program chooser accessible');
    assert.ok(/startWorkout/.test(plan.dayStart),width+'/'+theme+': program day starts via original motor');
    assert.ok(plan.buttonHeight>=44,width+'/'+theme+': main quick CTA >=44 px');
    assert.ok(plan.frameTop>plan.quickTop,width+'/'+theme+': mockup quick card precedes active program');
    assert.ok(plan.overflow<=1,width+'/'+theme+': no horizontal overflow '+JSON.stringify(plan));
    assert.equal(plan.frameBorder,'0px',width+'/'+theme+': redundant outer program frame removed');
    assert.equal(plan.frameFill,'rgba(0, 0, 0, 0)',width+'/'+theme+': program section uses page canvas');
    assert.equal(plan.programBorder,'0px',width+'/'+theme+': no second nested program border');
    assert.equal(plan.headingFill,'rgba(0, 0, 0, 0)',width+'/'+theme+': program heading stays on canvas');
    assert.ok(Math.abs(plan.dayWidth-plan.frameWidth)<=1,width+'/'+theme+': day cards use full section width');

    // Presentation must preserve both original details controls, including
    // hidden content when collapsed and all five original settings inputs.
    const training=page.locator('main.tp152-plan .tp1511-training');
    await training.locator(':scope > summary').click();
    await training.locator('#tp1511Rest').waitFor({state:'visible'});
    assert.equal(await training.locator('input:is(#tp1511Rest,#tp1511Iso,#tp1511Per,#tp1511Single,#tp1511Total):visible').count(),5,width+'/'+theme+': rest and four progression fields remain reachable');
    assert.equal(await training.locator('button[onclick="tp1511SaveTraining()"]:visible').count(),1,width+'/'+theme+': original settings save action');
    if(width===393&&theme==='yellow'){
      fs.mkdirSync('ui-evidence',{recursive:true});
      await page.screenshot({path:'ui-evidence/pr271-workout-settings-expanded-393.png'});
    }
    await training.locator(':scope > summary').click();
    await training.locator('#tp1511Rest').waitFor({state:'hidden'});
    const programDetails=page.locator('main.tp152-plan .tp152-active-program');
    await programDetails.locator(':scope > summary').click();
    await programDetails.locator('.tp152-day').first().waitFor({state:'hidden'});
    await programDetails.locator(':scope > summary').click();
    await programDetails.locator('.tp152-day').first().waitFor({state:'visible'});
    await programDetails.locator('.tp150-change-program').click();
    assert.equal(await page.evaluate(()=>state.tab),'programs',width+'/'+theme+': actual chooser route still works');
    await page.evaluate(()=>go('plan'));



    // P2c: both Studio entry cards call the real panel APIs. No fake GPS
    // initiation, no second workout engine and no lost existing data.
    const launchers=page.locator('main.tp152-plan #tp271WorkoutLaunchers button');
    await page.waitForFunction(()=>document.querySelectorAll('#tp271WorkoutLaunchers button').length===2);
    assert.equal(await launchers.count(),2,width+'/'+theme+': two actual Workout shortcuts');
    // Muscle-only filtering must begin with an empty search, because the
    // original library intentionally remembers searches across theme changes.
    await page.evaluate(()=>{state.libraryFilter={group:'all',gear:'all',q:''}});
    await page.locator('#tp271WorkoutLaunchers button[data-tp271-launcher="library"]').click();
    await page.waitForSelector('#tp155R4PanelHost[data-panel="exercises"] .tp271-muscle-chips');
    const muscleChips=page.locator('#tp155R4PanelHost[data-panel="exercises"] .tp271-muscle-chip');
    assert.ok(await muscleChips.count()>=6,width+'/'+theme+': real muscle options exposed as chips');
    assert.equal(await page.locator('#tp155R4PanelHost[data-panel="exercises"] #libraryMuscle').count(),1,width+'/'+theme+': native muscle select preserved');
    const compact=await page.locator('#tp155R4PanelHost[data-panel="exercises"]').evaluate(el=>{
      const title=el.querySelector('.hero h1'),results=el.querySelector('#libraryResults'),panel=el.querySelector('main');
      return {font:parseFloat(getComputedStyle(title).fontSize),
        stack:results.getBoundingClientRect().top-panel.getBoundingClientRect().top,
        resultHeight:results.getBoundingClientRect().height,
        viewport:innerHeight,
        chips:[...el.querySelectorAll('.tp271-muscle-chip')].every(c=>c.getBoundingClientRect().height>=44)};
    });
    assert.ok(compact.font<=18,width+'/'+theme+': compact catalogue title');
    assert.ok(compact.stack<=300&&compact.resultHeight>=compact.viewport*.4,width+'/'+theme+': catalogue reserves space for results '+JSON.stringify(compact));
    assert.ok(compact.chips,width+'/'+theme+': compact chips retain 44px tap height');
    if(width===393&&theme==='yellow')await page.screenshot({path:'ui-evidence/pr271-catalogue-compact-393.png',animations:'disabled'});

    await page.locator('#tp155R4PanelHost[data-panel="exercises"] button[data-tp271-muscle="legs"]').click();
    await page.waitForFunction(()=>document.querySelector('#tp155R4PanelHost[data-panel="exercises"] #libraryMuscle')?.value==='legs');
    assert.equal(await page.evaluate(()=>state.libraryFilter.group),'legs',width+'/'+theme+': original catalogue filter executes');
    assert.equal(await page.locator('#tp155R4PanelHost[data-panel="exercises"] button[data-tp271-muscle="legs"][aria-pressed="true"]').count(),1,width+'/'+theme+': chip mirrors real selected group');
    assert.ok(await page.locator('#tp155R4PanelHost[data-panel="exercises"] #libraryResults .tp-library-card').count()>0,width+'/'+theme+': real filtered catalogue');
    await page.locator('#tp155R4PanelHost[data-panel="exercises"] button[data-tp271-muscle="all"]').click();
    await page.waitForFunction(()=>state.libraryFilter.group==='all');
    await page.evaluate(()=>window.tp155R4ClosePanel(false));
    await page.waitForFunction(()=>!document.querySelector('#tp155R4PanelHost'));
    await page.locator('#tp271WorkoutLaunchers button[data-tp271-launcher="outdoor"]').click();
    await page.waitForSelector('#tp155R4PanelHost[data-panel="quick"] .tp271-muscle-chips');
    assert.equal(await page.locator('#tp155R4PanelHost[data-panel="quick"] #tp155QuickQuery').inputValue(),'running',width+'/'+theme+': outdoor entry prefilters real running exercise');
    assert.equal(await page.locator('#tp155R4PanelHost[data-panel="quick"] .tp152-quick-row[data-exercise-id="running"]').count(),1,width+'/'+theme+': real distance activity offered');
    assert.equal(await page.evaluate(()=>!!state.session),false,width+'/'+theme+': entering Outdoor does not auto-start workout or GPS');
    assert.equal(await page.locator('#tp155R4PanelHost[data-panel="quick"] button[data-tp271-muscle="all"][aria-pressed="true"]').count(),1,width+'/'+theme+': quick muscle shortcuts share real filter');
    await page.evaluate(()=>window.tp155R4ClosePanel(false));
    await page.waitForFunction(()=>!document.querySelector('#tp155R4PanelHost'));

    const first=page.locator('main.tp152-plan .tp152-day').first();
    // The summary contains a separate Order button. Its center may be the
    // summary's center on narrow screens; exercise both targets explicitly.
    const orderedIds=await page.evaluate(()=>activeProgram().days[0].exercises.slice());
    const dayWasOpen=await first.evaluate(el=>el.open);
    await first.locator(':scope > summary .tp128-order-open').click();
    await page.locator('#tp2628Dialog .tp128-order-list').waitFor({state:'visible'});
    assert.equal(await first.evaluate(el=>el.open),dayWasOpen,width+'/'+theme+': Order does not toggle day disclosure');
    await page.locator('#tp2628Dialog [data-tp2628-cancel]').click();
    await page.locator('#tp2628Dialog').waitFor({state:'hidden'});
    assert.deepEqual(await page.evaluate(()=>activeProgram().days[0].exercises),orderedIds,width+'/'+theme+': cancelling Order leaves exercises unchanged');
    const daySummary=first.locator(':scope > summary');
    if(await first.evaluate(el=>el.open))await daySummary.click({position:{x:8,y:8}});
    await daySummary.click({position:{x:8,y:8}});
    await first.locator('.tp152-start-day').waitFor({state:'visible'});
    assert.equal(await first.evaluate(el=>el.open),true,width+'/'+theme+': original day disclosure opens');
    assert.equal(await first.locator('.tp152-start-day').isVisible(),true,width+'/'+theme+': real day start CTA');
    const editor=first.locator('.tp152-exercise').first();
    await editor.locator(':scope > summary').click();
    assert.equal(await editor.locator('.tp152-edit-grid').isVisible(),true,width+'/'+theme+': real sets/reps prescription editor');
    const editorValues=await editor.locator('.tp152-edit-grid input').count();
    assert.ok(editorValues>=2,width+'/'+theme+': real editable fields retained');
    await editor.locator(':scope > summary').click();
    await daySummary.click({position:{x:8,y:8}});

    await page.locator('main.tp152-plan .tp150-quick-entry-minimal button').click();
    await page.waitForSelector('#tp155R4PanelHost[data-panel="quick"] #tp155QuickQuery');
    assert.equal(await page.locator('#tp271Dock:visible').count(),0,width+'/'+theme+': dock never covers Quick panel');
    const search=page.locator('#tp155R4PanelHost[data-panel="quick"] #tp155QuickQuery');
    await search.fill('plank');
    const quick=page.locator('#tp155R4PanelHost[data-panel="quick"] #tp150QuickResults .tp152-quick-row[data-exercise-id="plank"]');
    await quick.waitFor();
    assert.equal(await quick.count(),1,width+'/'+theme+': real search finds Plank');
    await quick.locator(':scope > summary').click();
    assert.equal(await quick.locator('.tp152-quick-body').isVisible(),true,width+'/'+theme+': existing quick configuration opens');
    assert.ok(await quick.locator('.tp150-quick-start').count()===1,width+'/'+theme+': real quick-start button retained');
    await page.evaluate(()=>window.tp155R4ClosePanel(false));
    await page.waitForFunction(()=>!document.getElementById('tp155R4PanelHost'));
    assert.equal(await page.locator('.top.tp154-nav-grid .tp154-nav-cell:visible').count(),8,width+'/'+theme+': original navigation restores after closing Quick panel');
    await page.evaluate(()=>go('plan'));

    // P2b: Studio exercise library styling is applied to the real filterable
    // catalogue panel. Verify source-backed search, all editor callbacks,
    // video controls when available and no lost bottom navigation.
    await page.evaluate(()=>{state.libraryFilter={group:'all',gear:'all',q:''};window.tp155R4OpenPanel('exercises', document.activeElement)});
    await page.waitForSelector('#tp155R4PanelHost[data-panel="exercises"] #libraryQuery');
    const library=page.locator('#tp155R4PanelHost[data-panel="exercises"]');
    assert.equal(await page.locator('#tp271Dock:visible').count(),0,width+'/'+theme+': dock hidden behind exercise catalogue');
    assert.ok(await library.locator('#libraryResults .tp-library-card').count()>=100,width+'/'+theme+': full original exercise catalogue');
    assert.equal(await library.locator('#libraryMuscle').count(),1,width+'/'+theme+': original muscle filter remains');
    assert.equal(await library.locator('#libraryGear').count(),1,width+'/'+theme+': original equipment filter remains');
    const query=library.locator('#libraryQuery');
    await query.fill('plank');
    await page.waitForFunction(()=>document.querySelectorAll('#tp155R4PanelHost[data-panel="exercises"] #libraryResults .tp-library-card').length>0);
    const found=library.locator('#libraryResults .tp-library-card').first();
    assert.ok((await found.innerText()).toLowerCase().includes('plank'),width+'/'+theme+': real library search filters rows');
    const geometry=await library.evaluate(panel=>{
      const filter=panel.querySelector('.tp155-library-filters'),input=panel.querySelector('#libraryQuery'),
        card=panel.querySelector('#libraryResults .tp-library-card');
      return {filterRadius:getComputedStyle(filter).borderTopLeftRadius,
        searchHeight:input.getBoundingClientRect().height,
        cardHeight:card.getBoundingClientRect().height,
        width:card.getBoundingClientRect().width,
        viewportWidth:innerWidth,
        pageOverflow:document.documentElement.scrollWidth-innerWidth};
    });
    assert.ok(geometry.searchHeight>=44&&geometry.cardHeight>=55,width+'/'+theme+': accessible library controls '+JSON.stringify(geometry));
    assert.ok(geometry.width<=geometry.viewportWidth&&geometry.pageOverflow<=1,width+'/'+theme+': no library overflow '+JSON.stringify(geometry));
    await found.locator(':scope > summary').click();
    assert.equal(await found.locator('.tp-library-body').isVisible(),true,width+'/'+theme+': original library detail expands');
    assert.ok(await found.locator('.tp150-library-quick').count()>0,width+'/'+theme+': original quick action intact');
    assert.ok(await found.locator('button[onclick*="chooseLibraryPlacement"]').count()>0,width+'/'+theme+': original add-to-program action intact');
    if(width===393&&theme==='yellow'){
      fs.mkdirSync('ui-evidence',{recursive:true});
      await page.screenshot({path:'ui-evidence/pr271-exercise-catalogue-393.png'});
    }
    await page.evaluate(()=>window.tp155R4ClosePanel(false));
    await page.waitForFunction(()=>!document.querySelector('#tp155R4PanelHost'));
    assert.equal(await page.locator('.top.tp154-nav-grid .tp154-nav-cell:visible').count(),8,width+'/'+theme+': bottom navigation returns from catalogue');
    await page.evaluate(()=>go('plan'));

    // Start a real program day (without finalizing it) to prove engine
    // ownership remains canonical and the dock cannot obscure finish/timer.
    await page.evaluate(()=>startWorkout(activeProgram().days[0].id));
    await page.waitForSelector('main.tp153-workout .tp16-workout-actions');
    const live=await page.evaluate(()=>({session:!!state.session,count:state.session?.exercises?.length||0,
      headerHeight:document.querySelector('.top.tp154-nav-grid').getBoundingClientRect().height,
      actions:!!document.querySelector('main.tp153-workout .tp16-workout-actions')}));
    assert.ok(live.session&&live.count>0&&live.actions,width+'/'+theme+': real workout engine/session still runs');
    assert.equal(live.headerHeight,0,width+'/'+theme+': empty header collapsed during workout');
    await page.evaluate(()=>{state.session=null;state.workout=null;db.set('draft',null);go('home')});
    if(width===393&&theme==='yellow'){
      await page.evaluate(()=>go('plan'));
      await page.waitForSelector('main.tp152-plan #tp271WorkoutLaunchers .tp271-workout-launcher');
      // Capture the complete destination after its existing finite reveal,
      // not the intermediate paint immediately following go('plan').
      await page.evaluate(async()=>{
        const animations=document.getAnimations().filter(a=>
          a.effect?.target?.closest?.('#app main')&&a.effect.getTiming().iterations!==Infinity);
        await Promise.all(animations.map(a=>a.finished.catch(()=>{})));
      });
      fs.mkdirSync('ui-evidence',{recursive:true});
      await page.screenshot({path:'ui-evidence/pr271-workout-entry-393.png',animations:'disabled'});
    }
   }
   assert.deepEqual(errors,[],width+': no JavaScript errors');
   await page.close();
  }
  console.log('PASS PR271 P2c: preview Workout, outdoor entry and real muscle-filter chips, genuine quick-search panel, nested program days/editor, original engine, 320/360/393/412px & basic/neon themes');
 }finally{await browser?.close();await new Promise(ok=>server.close(ok))}
})().catch(err=>{console.error(err);process.exitCode=1});
