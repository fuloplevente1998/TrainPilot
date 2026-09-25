/* Visual-only contract: CSS may recolor surfaces and 1px borders,
   but must not alter the navigation, card grids, button hit targets,
   overlay X positions or event handling inherited from 1.7.6. */
const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const css=fs.readFileSync(path.join(root,'global-uiux-177.css'),'utf8');
const ownRules=css.replace(/\/\*[\s\S]*?\*\//g,'').replace(/--[\w-]+\s*:\s*[^;]+;/g,'');
// Only three explicit border-width compensations may alter padding; no
// page geometry, button spacing, grid, navigation or overlay layout edits.
const allowedPadding=[...ownRules.matchAll(/\\bpadding(?:-left)?\\s*:\\s*([^;]+);/g)].map(m=>m[0]);
assert.deepEqual(allowedPadding,['padding:11px 13px!important;','padding:1px!important;','padding-left:2px!important;']);
for(const prop of ['display','grid-template','grid-auto','flex-direction','position','top','right','bottom','left','margin','width','height','min-height','max-height','transform','gap']){
 assert.equal(new RegExp('(^|[;{\\s])'+prop+'\\s*:','m').test(ownRules),false,'CSS-only styling must not override geometry: '+prop);
}
const server=http.createServer((req,res)=>{
 let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';
 const f=path.resolve(root,'.'+p);
 if(!f.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
 fs.readFile(f,(e,d)=>{if(e){res.writeHead(404);res.end();return}
  res.setHeader('Content-Type',f.endsWith('.js')?'text/javascript':f.endsWith('.css')?'text/css':f.endsWith('.html')?'text/html':'text/plain');res.end(d);
 });
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  for(const width of [320,360,393,412]){
   const page=await browser.newPage({viewport:{width,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'});
   const errors=[];page.on('pageerror',e=>errors.push(String(e.message||e)));page.on('dialog',d=>d.accept());
   await page.goto('http://127.0.0.1:'+server.address().port+'/');
   await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
   assert.equal(await page.evaluate(()=>!!window.TrainPilotGlobalUi177),false,'Do not install new JS or change toggle logic');
   for(const theme of ['classicBlue','blue']){
    await page.evaluate(t=>{state.session=null;state.workout=null;window.tp155R4ClosePanel?.(false);rf200SetTheme(t);go('home')},theme);
    const family=theme==='blue'?'vivid':'basic';
    assert.equal(await page.evaluate(()=>document.documentElement.dataset.tpThemeFamily),family);
    for(const route of ['home','plan','programs','history','health']){
     await page.evaluate(x=>go(x),route);
     // Let the existing route entrance transition settle before comparing
     // visual CSS on/off; do not misdiagnose a transient animation as reflow.
     await page.waitForTimeout(350);
     const result=await page.evaluate(async()=>{
      const link=document.querySelector('link[href="global-uiux-177.css"]'),main=document.querySelector('#app main');
      const nav=document.querySelector('.top.tp154-nav-grid'),buttons=[...nav.querySelectorAll('.tp154-nav-cell')];
      const snap=()=>{
       const rect=e=>{if(!e)return null;const r=e.getBoundingClientRect(),s=getComputedStyle(e);return {x:r.x,y:r.y,w:r.width,h:r.height,padding:s.padding,gap:s.gap,fontSize:s.fontSize,fontFamily:s.fontFamily,grid:s.gridTemplateColumns}};
       const regular=main.querySelector('.card:not(.tp155-home-active-card):not(.tp168-today-card):not(.tp153-workout-head):not(.tp152-active-program),.tp152-program-card:not(.active-program),.stat:not(.tp168-today-card)');
       const programCard=main.querySelector('.tp152-program-card');
       const heroInner=main.querySelector('.tp155-home-active-grid,.tp152-active-program > summary');
       const hero=main.matches('.rf221-home')?main.querySelector(':scope>.hero.tp155-home-active-card'):main.matches('.tp152-plan')?main.querySelector(':scope>.tp152-active-program'):main.matches('.tp153-workout')?main.querySelector('.tp153-workout-head'):main.matches('.tp168-health,.rf263-health')?main.querySelector('.tp168-today-card,.tp151-health-card'):null;
       const rs=regular&&getComputedStyle(regular),hs=hero&&getComputedStyle(hero);
       return {nav:rect(nav),buttons:buttons.map(rect),main:rect(main),regularRect:rect(regular),heroRect:rect(hero),heroInnerRect:rect(heroInner),programCardRect:rect(programCard),programCardContentRect:rect(programCard?.querySelector(':scope > summary')) ,body:getComputedStyle(document.body).backgroundColor,regular:rs?{bg:rs.backgroundColor,border:rs.borderTopColor,shadow:rs.boxShadow}:null,hero:hs?{borderImage:hs.backgroundImage,border:hs.borderTopWidth,shadow:hs.boxShadow}:null};
      };
      // The existing app animates some background/outline changes. Compare
      // the fully settled styles instead of intermediate oklab colors.
      const settled=()=>new Promise(r=>setTimeout(()=>requestAnimationFrame(()=>requestAnimationFrame(r)),300));
      link.disabled=true;await settled();const before=snap();
      link.disabled=false;await settled();const after=snap();
      return {before,after};
     });
     assert.deepEqual(result.after.buttons,result.before.buttons,width+'/'+theme+'/'+route+': nav buttons geometry/typography unchanged');
     assert.deepEqual(result.after.regularRect,result.before.regularRect,width+'/'+theme+'/'+route+': secondary card geometry unchanged');
     assert.deepEqual(result.after.heroRect,result.before.heroRect,width+'/'+theme+'/'+route+': highlighted card geometry unchanged');
     assert.deepEqual(result.after.heroInnerRect,result.before.heroInnerRect,width+'/'+theme+'/'+route+': hero content geometry unchanged');
     assert.deepEqual(result.after.programCardRect,result.before.programCardRect,width+'/'+theme+'/'+route+': program card frame unchanged');
     assert.deepEqual(result.after.programCardContentRect,result.before.programCardContentRect,width+'/'+theme+'/'+route+': program card summary unchanged');
     assert.deepEqual(result.after.nav,result.before.nav,width+'/'+theme+'/'+route+': full navbar layout unchanged');
     for(const k of ['x','y','w','h','padding','gap','fontSize','fontFamily','grid'])assert.equal(result.after.main[k],result.before.main[k],width+'/'+theme+'/'+route+': main layout '+k);
     assert.equal(result.after.body,'rgb(14, 16, 21)');
     if(result.after.regular){
      assert.equal(result.after.regular.bg,'rgb(26, 30, 37)',route+': Health card fill');
      assert.equal(result.after.regular.border,'rgb(44, 52, 64)',route+': neutral 1px border');
      assert.equal(result.after.regular.shadow,'none',route+': plain cards have no shadow');
     }
     if(result.after.hero){
      assert.equal(result.after.hero.border,'1px',route+': Health-reference Hero has a truly 1px outline');
      assert.ok(result.after.hero.borderImage.includes('gradient'),route+': hero has visual gradient border');
      assert.equal(result.after.hero.shadow==='none',family==='basic',route+': glow only for vivid theme');
     }
     if(route==='programs'){
      const c=await page.locator('.tp152-program-card:not(.active-program)').first().evaluate(e=>{const s=getComputedStyle(e);return {w:s.borderLeftWidth,c:s.borderLeftColor}});
      assert.equal(c.w,'1px','Program card left outline must visually match Health 1px');
      assert.equal(c.c,'rgb(44, 52, 64)','Program border is now neutral instead of blue-gray');
      const frame=await page.locator('main.tp150-programs-compact .tp150-programs-frame').evaluate(e=>{const s=getComputedStyle(e);return {width:s.borderTopWidth,bg:s.backgroundColor}});
      assert.equal(frame.width,'1px','Program list grouping keeps one closed 1px frame');
      assert.equal(frame.bg,'rgb(26, 30, 37)','Program grouping matches Health card fill');
     }
    }
    // Current workout set rows get an external visual-only closed outline.
    await page.evaluate(()=>{go('plan');const p=activeProgram();if(p?.days?.length)startWorkout(p.days[0].id)});
    if(await page.locator('main.tp153-workout .tp153-set-row').count()){
      const rowStyle=await page.locator('main.tp153-workout .tp153-set-row').first().evaluate(e=>{
        const s=getComputedStyle(e),r=e.getBoundingClientRect();
        return {width:s.outlineWidth,style:s.outlineStyle,color:s.outlineColor,height:r.height};
      });
      assert.equal(rowStyle.width,'1px','Each active set is a separate closed 1px interaction unit');
      assert.equal(rowStyle.style,'solid');
      assert.equal(rowStyle.color,'rgb(44, 52, 64)');
      assert.ok(rowStyle.height>30,'Workout set tap targets remain usable');
    }
    // Original 1.7.6 overlay geometry: Coach only is lifted; neither
    // Calendar nor Settings moves during this separate visual-only pass.
    await page.evaluate(()=>go('home'));
    const positions=[];
    for(const panel of ['calendar','coach','settings']){
     const b=panel==='calendar'?'.tp151-nav-item[onclick*="calendar"]':'.tp154-'+panel+'-action';
     await page.locator('.top.tp154-nav-grid '+b).click();
     assert.equal(await page.locator('#tp155R4PanelHost').getAttribute('data-panel'),panel);
     const x=await page.evaluate(()=>{
      const h=document.querySelector('#tp155R4PanelHost'),p=h.querySelector('.tp155-r4-panel'),e=h.querySelector('.tp155-r4-panel-close'),s=getComputedStyle(e),r=e.getBoundingClientRect();
      return {top:r.top-p.getBoundingClientRect().top,w:r.width,h:r.height,bg:s.backgroundColor,border:s.borderTopColor,radius:s.borderTopLeftRadius};
     });
     positions.push({panel,...x});
     if(panel==='coach'){
       const tile=page.locator('#tp155R4PanelHost[data-panel="coach"] .tp153-coach-metric').first();
       if(await tile.count()){
         const s=await tile.evaluate(e=>{const c=getComputedStyle(e),r=e.getBoundingClientRect();return {width:c.outlineWidth,color:c.outlineColor,w:r.width,h:r.height}});
         assert.equal(s.width,'1px','Coach metric tile gets a closed Health-style outline');
         assert.equal(s.color,'rgb(44, 52, 64)');
         assert.ok(s.w>30&&s.h>30,'Coach metric hit target unchanged');
       }
     }
     await page.locator('.top.tp154-nav-grid '+b).click();
     assert.equal(await page.locator('#tp155R4PanelHost').count(),0,panel+': existing retap closes panel');
     assert.equal(await page.evaluate(()=>state.tab),'home',panel+': existing parent route unchanged');
    }
    assert.ok(positions.find(x=>x.panel==='coach').top<positions[0].top,'Coach X stays raised, others remain at their old positions');
    for(const x of positions.slice(1))for(const k of ['w','h','bg','border','radius'])assert.equal(x[k],positions[0][k],'Same X appearance; no geometry edits on global branch: '+k);
   }
   assert.deepEqual(errors,[],width+': no page errors');
   await page.close();
  }
  console.log('PASS #58 visual-only: exact nav/main geometry, existing panel X and toggle behavior, Health tones, Hero and themes at 320/360/393/412px');
 }finally{await browser?.close();server.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
