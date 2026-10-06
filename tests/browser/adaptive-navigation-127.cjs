'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{const f=path.resolve(root,'.'+(new URL(req.url,'http://local').pathname==='/'?'/index.html':new URL(req.url,'http://local').pathname));if(!f.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(f,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',f.endsWith('.js')?'text/javascript':f.endsWith('.css')?'text/css':f.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];page.on('pageerror',e=>errors.push(String(e)));page.on('dialog',d=>d.accept());
 const url='http://127.0.0.1:'+server.address().port;await page.goto(url);await page.waitForFunction(()=>TrainPilotBoot.finished&&window.TrainPilotNavigation127);
 await page.evaluate(()=>{rf200SetTheme('yellow');go('programs')});
 const settle=()=>page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));
 async function geometry(expected){await settle();const g=await page.evaluate(()=>{
  const nav=document.querySelector('.top.tp154-nav-grid'),main=document.querySelector('#tp155R4PanelHost main,#app main'),tools=main.querySelector('.tp127-page-tools'),rect=e=>e?.getBoundingClientRect().toJSON();
  return {edge:TrainPilotNavigation127.edge,nav:rect(nav),tools:rect(tools),main:rect(main),cells:[...nav.querySelectorAll('.tp154-nav-cell')].map(e=>({...rect(e),order:Number(getComputedStyle(e).order),text:e.querySelector('.tp151-nav-label').textContent,svg:!!e.querySelector('.tp127-icon')})),w:innerWidth,h:innerHeight,overflow:document.documentElement.scrollWidth-innerWidth};
 });assert.equal(g.edge,expected);assert.equal(g.cells.length,8);assert.ok(g.cells.every(c=>c.svg&&c.width>=44&&c.height>=44),'labeled SVG touch targets');assert.ok(g.cells.every(c=>c.x>=-1&&c.right<=g.w+1&&c.y>=-1&&c.bottom<=g.h+1),'all eight destinations visible');assert.ok(g.overflow<=1,'no horizontal document overflow');
 assert.deepEqual([...g.cells].sort((a,b)=>a.order-b.order).map(c=>c.text),['Kezdőlap','Edzés','Egészség','Programok','Napló','Naptár','Coach','Beállítások']);
 if(expected==='bottom'){assert.ok(Math.abs(g.nav.bottom-g.h)<1);assert.ok(Math.abs(g.tools.bottom-g.nav.top)<2,'local tools directly above nav');}
 if(expected==='top'){assert.ok(g.nav.top<1);assert.ok(Math.abs(g.tools.top-g.nav.bottom)<2,'local tools directly below nav');}
 if(expected==='right'){assert.ok(Math.abs(g.nav.right-g.w)<1);assert.ok(Math.abs(g.tools.right-g.nav.left)<2,'local tools to left of nav');assert.ok(g.main.right<=g.tools.left+1);}
 if(expected==='left'){assert.ok(g.nav.left<1);assert.ok(Math.abs(g.tools.left-g.nav.right)<2,'local tools to right of nav');assert.ok(g.main.left>=g.tools.right-1);}
 return g;
 }
 await geometry('bottom');
 for(const width of [320,360,393,412]){await page.setViewportSize({width,height:873});await geometry('bottom');}
 await page.setViewportSize({width:393,height:873});
 // Layout changes keep actual DOM nodes, open lazy disclosures, and an unsaved input.
 const program=page.locator('.tp152-program-card').first();await program.locator(':scope > summary').click();const day=program.locator('.tp152-program-day').first();await day.locator(':scope > summary').click();const row=day.locator('.tp107-program-editor').first();await row.locator(':scope > summary').click();await row.locator('[data-tp107-rx="reps"]').fill('11–13');
 await page.evaluate(()=>{window.tp127OriginalMain=document.querySelector('#app main');window.tp127OriginalField=document.querySelector('[data-tp107-rx="reps"]');});
 const bottom=await row.evaluate(e=>({summary:e.querySelector('summary').getBoundingClientRect().top,body:e.querySelector('.tp107-editor-body').getBoundingClientRect().bottom}));assert.ok(bottom.body<=bottom.summary+2,'disclosure body grows above its summary');
 await page.locator('.tp127-edge-switch').click();await geometry('top');
 await page.setViewportSize({width:873,height:393});await geometry('right');await page.locator('.tp127-edge-switch').click();await geometry('left');
 assert.equal(await page.evaluate(()=>document.querySelector('#app main')===tp127OriginalMain&&document.querySelector('[data-tp107-rx="reps"]')===tp127OriginalField&&tp127OriginalField.value==='11–13'),true,'edge/rotation preserve editor identity and unsaved values');
 await page.reload();await page.waitForFunction(()=>TrainPilotBoot.finished);await page.evaluate(()=>go('programs'));await geometry('left');await page.setViewportSize({width:393,height:873});await geometry('top');await page.locator('.tp127-edge-switch').click();await geometry('bottom');
 // Overlay routes use the free area and restore focus to the docked opener.
 const opener=page.locator('button[onclick="customExerciseScreen()"]');await opener.click();await settle();await page.locator('#ceHu').fill('Uncommitted draft');await page.setViewportSize({width:873,height:393});await settle();assert.equal(await page.locator('#ceHu').inputValue(),'Uncommitted draft');
 const bounds=await page.locator('#tp155R4PanelHost').boundingBox(),nav=await page.locator('.top').boundingBox();assert.ok(bounds.x>=nav.x+nav.width-1,'left-anchored overlay begins after nav');await page.locator('.tp155-r4-panel-close').click();assert.equal(await opener.evaluate(e=>document.activeElement===e),true);
 for(const route of ['calendar','coach','settings']){await page.evaluate(r=>go(r),route);await settle();assert.ok(await page.locator('#tp155R4PanelHost').isVisible());const p=await page.locator('#tp155R4PanelHost').boundingBox();assert.ok(p.x>=nav.x+nav.width-1&&p.x+p.width<=873);assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true);}
 // Both landscape rails expose library filters into the free content lane.
 for(const side of ['left','right']){
  await page.evaluate(side=>{TrainPilotNavigation127.setEdge(side);go('programs');muscleLibrary();},side);await settle();
  const picker=page.locator('#libraryMuscle').locator('..');await picker.locator('.tp-select-trigger').click();await settle();
  const menu=await picker.locator('.tp-select-menu').boundingBox(),tools=await page.locator('#tp155R4PanelHost .tp127-page-tools').boundingBox();
  assert.ok(menu.x>=0&&menu.y>=0&&menu.x+menu.width<=873&&menu.y+menu.height<=394,'rail filter list fits the viewport');
  assert.ok(side==='left'?menu.x>=tools.x+tools.width-1:menu.x+menu.width<=tools.x+1,'rail filter opens towards content');
  await picker.locator('[data-value="all"]').click();await page.evaluate(()=>tp155R4ClosePanel(false));
 }
 await page.setViewportSize({width:393,height:873});await page.evaluate(()=>{go('plan');startWorkout('A')});await page.waitForSelector('.tp153-workout');const session=await page.evaluate(()=>({id:state.session.id,started:state.session.started,count:state.session.exercises.length}));
 await page.locator('.tp127-edge-switch').click();await page.setViewportSize({width:873,height:393});await settle();assert.deepEqual(await page.evaluate(()=>({id:state.session.id,started:state.session.started,count:state.session.exercises.length})),session,'rotation preserves workout');
 const workout=await page.locator('.tp127-page-tools .tp16-workout-actions').boundingBox();assert.ok(workout&&workout.width>0,'workout controls join the dock');
 // IME viewport shrink keeps portrait layout; controls/inputs remain in natural form flow.
 await page.evaluate(()=>{state.session=null;state.workout=null;go('programs')});await page.setViewportSize({width:393,height:873});await page.evaluate(()=>{TrainPilotNavigation127.setEdge('bottom');tp155R4OpenPanel('quick');});await page.locator('#tp155QuickQuery').focus();await page.evaluate(()=>{window.tp127RealViewport=window.visualViewport;Object.defineProperty(window,'visualViewport',{configurable:true,value:{height:450,width:393,offsetTop:0}});window.dispatchEvent(new Event('resize'));});await settle();assert.equal(await page.evaluate(()=>TrainPilotNavigation127.orientation),'portrait');assert.equal(await page.evaluate(()=>TrainPilotNavigation127.keyboard),true);assert.equal(await page.locator('.top').isVisible(),false);await page.locator('#tp155QuickQuery').fill('squat');
 await page.evaluate(()=>{Object.defineProperty(window,'visualViewport',{configurable:true,value:tp127RealViewport});document.activeElement.blur();window.dispatchEvent(new Event('resize'));});await settle();assert.ok(await page.locator('.top').isVisible());
 await page.evaluate(()=>{tp155R4ClosePanel(false);go('programs')});await settle();await page.evaluate(()=>TrainPilotNavigation127.setEdge('bottom'));await geometry('bottom');fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/adaptive-navigation-127-portrait.png'});await page.setViewportSize({width:873,height:393});await settle();await page.evaluate(()=>TrainPilotNavigation127.setEdge('right'));await geometry('right');await page.screenshot({path:'ui-evidence/adaptive-navigation-127-landscape.png'});
 assert.deepEqual(errors,[]);console.log('PASS #127: four menu edges, attached page controls, SVG icons, narrow widths, upward disclosure, preferences, editor/workout identity, overlays/back/focus and IME.');
}finally{await browser?.close();await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exitCode=1});
