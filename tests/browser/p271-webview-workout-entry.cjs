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
  for(const width of [320,393]){
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
        button=quick.querySelector('button'),dock=document.getElementById('tp271Dock');
      const q=quick.getBoundingClientRect(),fr=frame.getBoundingClientRect(),nav=dock.getBoundingClientRect(),tap=button.getBoundingClientRect();
      return {
       quick:!!quick,training:!!training,program:!!program,days:program.querySelectorAll('.tp152-day').length,
       startText:button.textContent.trim(),startAction:button.getAttribute('onclick'),
       choice:program.querySelector('.tp150-change-program')?.getAttribute('onclick'),
       dayStart:day.querySelector('.tp152-start-day')?.getAttribute('onclick'),
       quickTop:q.top,frameTop:fr.top,buttonHeight:tap.height,navTop:nav.top,
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

    const first=page.locator('main.tp152-plan .tp152-day').first();
    await first.locator(':scope > summary').click();
    assert.equal(await first.locator('.tp152-start-day').isVisible(),true,width+'/'+theme+': real day start CTA');
    const editor=first.locator('.tp152-exercise').first();
    await editor.locator(':scope > summary').click();
    assert.equal(await editor.locator('.tp152-edit-grid').isVisible(),true,width+'/'+theme+': real sets/reps prescription editor');
    const editorValues=await editor.locator('.tp152-edit-grid input').count();
    assert.ok(editorValues>=2,width+'/'+theme+': real editable fields retained');
    await editor.locator(':scope > summary').click();
    await first.locator(':scope > summary').click();

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
    assert.equal(await page.locator('#tp271Dock:visible').count(),1,width+'/'+theme+': dock restores after closing Quick panel');
    await page.evaluate(()=>go('plan'));

    // P2b: Studio exercise library styling is applied to the real filterable
    // catalogue panel. Verify source-backed search, all editor callbacks,
    // video controls when available and no lost bottom navigation.
    await page.evaluate(()=>window.tp155R4OpenPanel('exercises', document.activeElement));
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
    assert.equal(await page.locator('#tp271Dock:visible').count(),1,width+'/'+theme+': bottom navigation returns from catalogue');
    await page.evaluate(()=>go('plan'));

    // Start a real program day (without finalizing it) to prove engine
    // ownership remains canonical and the dock cannot obscure finish/timer.
    await page.evaluate(()=>startWorkout(activeProgram().days[0].id));
    await page.waitForSelector('main.tp153-workout .tp16-workout-actions');
    const live=await page.evaluate(()=>({session:!!state.session,count:state.session?.exercises?.length||0,
      dock:getComputedStyle(document.getElementById('tp271Dock')).display,
      actions:!!document.querySelector('main.tp153-workout .tp16-workout-actions')}));
    assert.ok(live.session&&live.count>0&&live.actions,width+'/'+theme+': real workout engine/session still runs');
    assert.equal(live.dock,'none',width+'/'+theme+': fixed mockup dock hidden during workout');
    await page.evaluate(()=>{state.session=null;state.workout=null;db.set('draft',null);go('home')});
    if(width===393&&theme==='yellow'){
      await page.evaluate(()=>go('plan'));
      fs.mkdirSync('ui-evidence',{recursive:true});
      await page.screenshot({path:'ui-evidence/pr271-workout-entry-393.png'});
    }
   }
   assert.deepEqual(errors,[],width+': no JavaScript errors');
   await page.close();
  }
  console.log('PASS PR271 P2b: preview Workout and exercise library mockup, genuine quick-search panel, nested program days/editor, original engine, 320/393px & basic/neon themes');
 }finally{await browser?.close();await new Promise(ok=>server.close(ok))}
})().catch(err=>{console.error(err);process.exitCode=1});
