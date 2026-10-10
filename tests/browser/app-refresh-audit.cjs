const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');

(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}
  fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);res.end();return;}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(data);});
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];
  page.on('pageerror',error=>errors.push(error.stack||error.message));page.on('dialog',dialog=>dialog.dismiss());
  await page.addInitScript(()=>{
   localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));
   window.auditProjection={version:1,days:{},preferences:{autoOnOpen:false}};
   window.Capacitor={isNativePlatform:()=>true,Plugins:{HealthJournal:{
    pendingRestore:async()=>({}),initialize:()=>new Promise(resolve=>window.auditFinishInitialization=()=>resolve(window.auditProjection)),getProjection:async()=>window.auditProjection,
    readPage:async()=>({records:[],days:[],nextCursor:0,hasMore:false})
   }}};
  });
  await page.goto('http://127.0.0.1:'+server.address().port);
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.auditFinishInitialization);
  await page.evaluate(()=>go('plan'));
  await page.locator('.tp150-quick-entry button').click();
  await page.waitForSelector('#tp155R4PanelHost[data-panel="quick"]');
  const floor=page.locator('#tp150QuickResults [data-exercise-id="db-floor-press"]');
  await floor.locator('summary').click();
  await floor.locator('[data-tp152-qsets]').selectOption('4');await floor.locator('[data-tp152-qweight]').fill('20');
  await page.evaluate(()=>auditFinishInitialization());
  await page.evaluate(()=>TrainPilotHealthJournal.ready());await page.waitForTimeout(100);
  assert.equal(await page.locator('#tp155R4PanelHost[data-panel="quick"]').count(),1,'late native Health Journal initialization must not dismiss the real Quick Workout panel');
  assert.equal(await floor.locator('[data-tp152-qsets]').inputValue(),'4','late initialization must retain configured sets');
  assert.equal(await floor.locator('[data-tp152-qweight]').inputValue(),'20','late initialization must retain configured weight');
  assert.equal(await floor.evaluate(node=>node.open),true,'expanded exercise must remain open');
  fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/app-refresh-quick.png',animations:'disabled'});
  await page.evaluate(()=>{auditProjection.preferences.autoOnOpen=true;});
  await page.evaluate(()=>TrainPilotHealthJournal.auto());await page.evaluate(()=>TrainPilotHealthJournal.auto());
  assert.equal(await floor.locator('[data-tp152-qweight]').inputValue(),'20','automatic projection refresh and its cooldown path retain quick configuration');
  await page.evaluate(()=>render());
  assert.equal(await page.locator('#tp155R4PanelHost[data-panel="quick"]').count(),1,'ordinary page redraw must not dismiss Quick Workout');
  assert.equal(await floor.locator('[data-tp152-qweight]').inputValue(),'20','ordinary redraw retains quick configuration too');
  assert.equal(await floor.evaluate(node=>node.open),true,'ordinary redraw retains the expanded exercise');
  await page.locator('#tp155R4PanelHost .tp155-r4-panel-close').click();
  assert.equal(await page.locator('#tp155R4PanelHost').count(),0,'explicit close still dismisses the picker');

  const checks=[];
  for(const [type,open,input,value] of [
   ['exercises','muscleLibrary()','#libraryQuery','press'],
   ['custom-exercise','customExerciseScreen()','#ceHu','Próbagyakorlat'],
   ['custom-program','createCustomProgram()','#tp106ProgramName','Próbaprogram'],
   ['goals',"tp155R4OpenPanel('goals')",null,null],
   ['calendar',"go('calendar')",null,null],
   ['settings',"go('settings')",null,null],
   ['coach','rf233CoachScreen()',null,null],
   ['health-journal',"tp155R4OpenPanel('health-journal')",null,null]
  ]){
   await page.evaluate(()=>tp155R4ClosePanel(false));await page.evaluate(code=>(0,eval)(code),open);
   const host=page.locator('#tp155R4PanelHost[data-panel="'+type+'"]');await host.waitFor();
   if(input)await page.locator(input).fill(value);
   await host.evaluate(node=>{window.auditPanelNode=node;window.auditPanelContent=node.querySelector('.tp155-r4-panel-content');});
   await page.evaluate(()=>TrainPilotUIRefresh.refresh());await page.evaluate(()=>TrainPilotHealthJournal.auto());
   assert.equal(await host.count(),1,type+' survives background data');
   assert.equal(await page.evaluate(()=>auditPanelNode===document.getElementById('tp155R4PanelHost')&&auditPanelContent===auditPanelNode.querySelector('.tp155-r4-panel-content')),true,type+' retains its existing controls');
   if(input)assert.equal(await page.locator(input).inputValue(),value,type+' retains unsaved input');
   checks.push({panel:type,backgroundRefresh:'pass'});
  }
  await page.evaluate(()=>{tp155R4ClosePanel(false);go('home');profileScreen();});
  await page.waitForSelector('#pfAge');await page.locator('#pfAge').fill('35');
  await page.evaluate(()=>TrainPilotUIRefresh.refresh());assert.equal(await page.locator('#pfAge').inputValue(),'35','personal planner retains its draft');
  await page.evaluate(()=>window.tp3ClosePlannerPanel?.(false));
  // Audit the five full pages and three navigation panels across mobile sizes,
  // languages and themes. Every case must remain navigable without page errors.
  for(const width of [320,393,412])for(const lang of ['hu','en','de','ro'])for(const theme of ['classicBlue','blue']){
   await page.setViewportSize({width,height:width===320?740:width===412?915:873});
   await page.evaluate(({lang,theme})=>{window.tp3ClosePlannerPanel?.(false);tp155R4ClosePanel(false);state.session=null;state.workout=null;db.set('language',lang);rf200SetTheme(theme);},{lang,theme});
   for(const route of ['home','plan','health','programs','history','calendar','settings','coach']){
    await page.evaluate(()=>tp155R4ClosePanel(false));
    await page.evaluate(route=>{if(route==='coach')rf233CoachScreen();else go(route);},route);
    await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
    const layout=await page.evaluate(()=>({main:!!document.querySelector('#app main'),nav:!!document.querySelector('.top.tp154-nav-grid'),overflow:document.documentElement.scrollWidth>innerWidth+1,locked:document.body.style.overflow==='hidden',panel:document.getElementById('tp155R4PanelHost')?.dataset.panel}));
    assert.equal(layout.main,true,route+' has a page');assert.equal(layout.nav,true,route+' retains navigation');assert.equal(layout.overflow,false,route+' has no horizontal page overflow at '+width+'/'+lang+'/'+theme);
    if(route==='health'){
     const healthBottom=await page.locator('#app main').evaluate(main=>Math.max(...[...main.children].map(node=>node.getBoundingClientRect().bottom))-innerHeight);
     assert.ok(healthBottom<=1,'Closed Health content must fit the viewport at '+width+'/'+lang+'/'+theme+' (bottom overflow '+healthBottom+')');
     const splitWords=await page.locator('.tp168-sync-tail .rf263-sync-actions').evaluate(host=>{
      const failures=[];for(const button of host.querySelectorAll('.btn')){
       if(button.scrollWidth>button.clientWidth+1)failures.push('overflow:'+button.textContent);
       const walker=document.createTreeWalker(button,NodeFilter.SHOW_TEXT);let node;
       while(node=walker.nextNode())for(const match of node.textContent.matchAll(/\S+/g)){const range=document.createRange();range.setStart(node,match.index);range.setEnd(node,match.index+match[0].length);if(range.getClientRects().length>1)failures.push(match[0]);}
      }return failures;
     });assert.deepEqual(splitWords,[],'Health action labels must fit without splitting words at '+width+'/'+lang+'/'+theme);
    }
    if(route==='settings')assert.doesNotMatch(await page.locator('#tp155R4PanelHost').innerText(),/RepForge-profilhoz/,'visible account hint uses TrainPilot');
    if(['calendar','settings','coach'].includes(route))assert.equal(layout.panel,route);else assert.equal(layout.locked,false,route+' is not left scroll-locked');
    checks.push({width,lang,theme,route,layout:'pass'});
    if(width===393&&lang==='hu'&&theme==='classicBlue')await page.screenshot({path:'ui-evidence/app-audit-'+route+'.png',animations:'disabled'});
   }
  }
  fs.mkdirSync('ui-results',{recursive:true});fs.writeFileSync('ui-results/app-audit.json',JSON.stringify(checks,null,2));
  await page.evaluate(()=>{tp155R4ClosePanel(false);go('plan');});
  await page.locator('.tp150-quick-entry button').click();
  const start=page.locator('#tp150QuickResults [data-exercise-id="db-floor-press"]');
  await start.locator('summary').click();await start.locator('.tp150-quick-start').click();
  await page.waitForFunction(()=>state.session?.type==='quick');
  assert.equal(await page.locator('#tp155R4PanelHost').count(),0,'starting the workout closes the picker');
  await page.evaluate(()=>tp155R4OpenPanel('quick'));await page.locator('#tp155QuickQuery').fill('press');
  await page.evaluate(()=>go('home'));await page.locator('#tp2628Dialog').waitFor();
  await page.locator('#tp2628Dialog [data-tp2628-cancel]').click();
  assert.equal(await page.locator('#tp155R4PanelHost[data-panel="quick"]').count(),1,'canceling navigation keeps the workout picker');
  assert.equal(await page.locator('#tp155QuickQuery').inputValue(),'press','canceling navigation retains search');
  assert.equal(await page.evaluate(()=>state.session.type),'quick');
  await page.evaluate(()=>TrainPilotUIRefresh.refresh());assert.equal(await page.locator('#tp155R4PanelHost[data-panel="quick"]').count(),1,'background data cannot replace an active workout view');
  await page.evaluate(()=>go('home'));await page.locator('#tp2628Dialog [data-tp2628-confirm]').click();
  await page.waitForFunction(()=>state.tab==='home'&&!state.session);
  assert.equal(await page.locator('#tp155R4PanelHost').count(),0,'confirmed navigation releases the picker');
  assert.equal(await page.evaluate(()=>!!db.get('draft',null)),true,'confirmed navigation preserves the workout draft');
  assert.deepEqual(errors,[]);
  console.log('PASS app audit: late native initialization and automatic refresh preserve Quick Workout; eight panels and planner retain edits; 192 mobile page/panel/language/theme cases remain navigable without page errors or horizontal overflow');
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1;});
