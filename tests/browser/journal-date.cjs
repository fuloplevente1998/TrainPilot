const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const http=require('node:http');
const {chromium}=require('playwright');
(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
  fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(data)});
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const sizes=[[320,640],[360,640],[390,844],[412,915],[768,1024]];
  for(const [w,h] of sizes)for(const [width,height] of [[w,h],[h,w]]){
   const context=await browser.newContext({viewport:{width,height},timezoneId:'Europe/Budapest',locale:'hu-HU'});
   const page=await context.newPage(),errors=[];page.on('pageerror',e=>{errors.push(e.message);console.error(e.stack)});
   await page.goto(`http://127.0.0.1:${server.address().port}`);
   await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
   await page.evaluate(()=>{
    const rows=['2026-09-15','2026-09-10','2026-09-01'].map((date,i)=>({id:'journal-test-'+i,workout:'A',started:date+'T12:00:00+02:00',finished:date+'T12:30:00+02:00',exercises:[],photos:i===0?[{id:'123e4567-e89b-12d3-a456-426614174000',label:'after',createdAt:date+'T12:31:00+02:00',updatedAt:1,driveFileId:'drive-test-id',deletedAt:null}]:[]}));
    db.set('history',rows);go('history');
   });
   for(const route of ['home','plan','calendar','programs','history','health','settings']){
    await page.evaluate(route=>go(route),route);
    await page.waitForFunction(()=>!document.documentElement.classList.contains('tp-route-switching'));
    const surface=route==='calendar'?'#tp155R4PanelHost[data-panel="calendar"] main':route==='settings'?'#tp155R4PanelHost[data-panel="settings"] main':'#app main';
    await page.waitForSelector(surface);
    assert.ok((await page.locator(surface).innerText()).length>0,route+' must render its active surface');
    assert.equal(await page.locator('select.field:not(.tp-native-select)').count(),0,route+' must not expose a native WebView select');
    assert.equal(await page.locator('input.field[type=date],input.field[type=time],input.field[type=datetime-local]').count(),0,route+' must not expose native WebView date/time controls');
    const temporal=page.locator('input.field[data-tp-temporal-type]');
    for(let i=0;i<await temporal.count();i++){
     const input=temporal.nth(i);assert.equal(await input.getAttribute('type'),'hidden',route+' temporal backing input must be hidden');
     assert.equal(await input.locator('xpath=..').locator('.tp-temporal-trigger').count(),1,route+' temporal input needs a TrainPilot trigger');
    }
   }
   // Clean runtime: after go() returns there must be no delayed legacy DOM rewrite.
   const cleanPaint=await page.evaluate(async()=>{
    go('home');const target=document.querySelector('#app');let mutations=0;
    const observer=new MutationObserver(()=>{mutations++});observer.observe(target,{childList:true,subtree:true,attributes:true});
    await new Promise(resolve=>setTimeout(resolve,100));observer.disconnect();
    return {mutations,masked:document.documentElement.classList.contains('tp-route-switching'),guard:!!document.querySelector('#rf112PaintGuard')};
   });
   assert.deepEqual(cleanPaint,{mutations:0,masked:false,guard:false},'clean route must be paint-stable immediately without the 1.1.2 mask');

   // Automatic Health sync must not repaint the active route. Manual Health sync still may repaint.
   const healthPaint=await page.evaluate(()=>{
    let calls=0;const savedRender=render;render=()=>{calls++};
    rf110AutoHealthFlight=Promise.resolve();rf250ActiveManual=false;rf245Paint();const automatic=calls;
    rf250ActiveManual=true;rf245Paint();const manual=calls;
    render=savedRender;rf250ActiveManual=false;rf110AutoHealthFlight=null;
    return {automatic,manual};
   });
   assert.deepEqual(healthPaint,{automatic:0,manual:1},'automatic Health refresh must stay visually silent while manual sync may repaint');

   // Program cards: activation stays beside the program summary so built-in cards remain compact.
   await page.evaluate(()=>go('programs'));
   await page.waitForFunction(()=>!document.documentElement.classList.contains('tp-route-switching'));
   const compactProgram=page.locator('.rf103-program-card').first();
   assert.equal(await compactProgram.locator('.rf103-program-main > .rf103-program-activate').count(),1,'activation button must live beside the program summary');
   const programLayout=await compactProgram.locator('.rf103-program-main').evaluate(e=>getComputedStyle(e).display);
   assert.equal(programLayout,'grid','program summary and activation must use a side-by-side layout');

   // Programok → Gyakorlatok: exercise descriptions stay compact until the user expands one.
   await page.evaluate(()=>muscleLibrary());
   const libraryCards=page.locator('#libraryResults details.tp-library-card');
   assert.equal(await libraryCards.count(),await page.evaluate(()=>exercises().length),'all exercise cards must render as collapsible details');
   assert.equal(await page.locator('#libraryResults details.tp-library-card[open]').count(),0,'exercise descriptions must start collapsed');
   const firstLibraryCard=libraryCards.first();
   assert.equal(await firstLibraryCard.locator('.tp-library-body').isVisible(),false,'collapsed description body must stay hidden');
   const libraryArrowStyle=async()=>firstLibraryCard.locator('summary').evaluate(e=>{const s=getComputedStyle(e,'::after');return {border:s.borderTopWidth,bg:s.backgroundImage,fill:s.backgroundColor,width:parseFloat(s.width),height:parseFloat(s.height),clip:s.clipPath,transform:s.transform}});
   const libraryClosed=await libraryArrowStyle();
   assert.equal(libraryClosed.border,'0px','exercise disclosure triangle must be borderless');
   assert.equal(libraryClosed.bg,'none','exercise disclosure must not regain the legacy gradient box');
   assert.ok(libraryClosed.width<=9.6&&libraryClosed.height<=12.6,'exercise disclosure triangle must remain compact: '+JSON.stringify(libraryClosed));
   assert.match(libraryClosed.clip,/polygon/i,'exercise disclosure must be a CSS triangle');
   assert.notEqual(libraryClosed.fill,'rgba(0, 0, 0, 0)','exercise disclosure triangle must be visible');
   console.log('LIBRARY_LAYOUT',await page.evaluate(()=>['#tp155R4PanelHost','.tp155-r4-panel','.tp155-r4-panel-content','.tp155-exercise-panel','.tp155-library-filters','#libraryResults','#libraryResults summary'].map(sel=>{const e=document.querySelector(sel),r=e.getBoundingClientRect(),s=getComputedStyle(e);return {sel,rect:r.toJSON(),height:s.height,overflow:s.overflow,display:s.display,scrollTop:e.scrollTop,scrollHeight:e.scrollHeight,hit:document.elementFromPoint(r.x+r.width/2,r.y+r.height/2)?.outerHTML.slice(0,180)}})));
   await firstLibraryCard.locator('summary').click();await page.waitForTimeout(180);
   const libraryOpen=await libraryArrowStyle();
   assert.notEqual(libraryOpen.transform,libraryClosed.transform,'exercise disclosure triangle must rotate from right to down when opened');
   assert.equal(await firstLibraryCard.locator('.tp-library-body').isVisible(),true,'tapping the exercise must reveal its description');
   assert.equal(await firstLibraryCard.locator('.tp-library-body > button.btn.secondary.block').count(),1,'add-to-program action must remain available');

   // Timed exercises: TrainPilot stopwatch writes elapsed seconds into the current set.
   await page.evaluate(()=>{
    const e=byId('plank');
    state.session={workout:'A',programName:'Stopper teszt',dayId:'A',started:new Date().toISOString(),exercises:[{id:e.id,hu:e.hu,en:e.en,loadType:e.loadType,repUnit:e.repUnit||'mp',sets:[{set:1,weight:0,reps:'',done:false}]}]};
    state.workout='A';state.current=0;renderWorkout();
   });
   await page.locator('#rf110Stopwatch').waitFor({state:'visible'});
   assert.equal(await page.locator('#rf110StopwatchToggle').innerText(),'Indítás');
   assert.equal(await page.evaluate(()=>{const card=document.querySelector('#rf110Stopwatch'),row=document.querySelector('main .row');return !!(card&&row&&(card.compareDocumentPosition(row)&Node.DOCUMENT_POSITION_FOLLOWING))}),true,'stopwatch must sit above the active set rows');
   await page.locator('#rf110StopwatchToggle').click();
   assert.equal(await page.locator('#rf110StopwatchToggle').innerText(),'Stop és rögzítés');
   await page.waitForTimeout(1200);
   const timedValue=Number(await page.locator('main .row input.field').last().inputValue());
   assert.ok(timedValue>=1,'stopwatch must write elapsed seconds into the timed set');
   await page.locator('#rf110StopwatchToggle').click();
   assert.equal(await page.locator('#rf110StopwatchToggle').innerText(),'Folytatás');
   assert.match(await page.locator('#rf142StopwatchStatus').innerText(),/rögzítve/i,'stopwatch must visibly confirm the recorded set duration');
   await page.locator('#rf110Stopwatch .btn.secondary').click();
   assert.equal(await page.locator('main .row input.field').last().inputValue(),'','reset must clear the timed set value');
   await page.evaluate(()=>{rf110StopwatchHalt(true);state.session=null;state.workout=null;go('home')});

   // Exercise all three app-level temporal control types inside the Calendar dropdown panel.
   await page.evaluate(()=>{go('calendar');const host=document.querySelector('#tp155R4PanelHost main');for(const [id,type,value] of [['tp-test-date','date','2026-09-15'],['tp-test-time','time','18:00'],['tp-test-datetime','datetime-local','2026-09-15T18:00']]){const i=document.createElement('input');i.id=id;i.className='field';i.type=type;i.value=value;host.appendChild(i)}rf260EnhanceTemporalFields()});
   assert.equal(await page.locator('#tp-test-date').getAttribute('type'),'hidden');
   await page.evaluate(()=>document.querySelector('#tp-test-date').closest('.tp-temporal').querySelector('.tp-temporal-trigger').click());
   await page.evaluate(()=>{rf260TemporalPicker.month=new Date(2026,8,1,12);rf260TemporalPaintDate()});
   await page.locator('#tpTemporalPicker [data-tp-date="2026-09-18"]').click();
   assert.equal(await page.locator('#tp-test-date').inputValue(),'2026-09-18');
   await page.evaluate(()=>document.querySelector('#tp-test-time').closest('.tp-temporal').querySelector('.tp-temporal-trigger').click());
   assert.equal(await page.locator('#tpTemporalPicker .tp155-time-option').count(),84,'time picker uses the new scrollable hour/minute lists');
   await page.locator('#tpTemporalPicker [data-tp-hour="19"]').click();
   await page.locator('#tpTemporalPicker [data-tp-minute="17"]').click();
   await page.locator('#tp155R3TimeSave').click();
   assert.equal(await page.locator('#tp-test-time').inputValue(),'19:17');
   await page.evaluate(()=>document.querySelector('#tp-test-datetime').closest('.tp-temporal').querySelector('.tp-temporal-trigger').click());
   await page.evaluate(()=>{rf260TemporalPicker.month=new Date(2026,8,1,12);rf260TemporalPaintDate()});
   await page.locator('#tpTemporalPicker [data-tp-date="2026-09-20"]').click();
   await page.locator('#tpTemporalPicker [data-tp-hour="7"]').click();
   await page.locator('#tpTemporalPicker [data-tp-minute="23"]').click();
   await page.locator('#tp155R3TimeSave').click();
   assert.equal(await page.locator('#tp-test-datetime').inputValue(),'2026-09-20T07:23');
   await page.evaluate(()=>go('history'));
   await page.waitForFunction(()=>!document.documentElement.classList.contains('tp-route-switching'));
   assert.equal(await page.locator('details.rf263-history').count(),3);
   assert.equal(await page.locator('details.rf263-history[open]').count(),0,'journal workouts must start collapsed');
   const firstHistory=page.locator('details.rf263-history').first();
   const historyArrow=firstHistory.locator('.rf103-history-side .rf103-history-chevron');
   assert.equal(await historyArrow.count(),1,'journal disclosure arrow must sit beside the header under the exercise count');
   const historyArrowStyle=async()=>historyArrow.evaluate(e=>{const s=getComputedStyle(e),b=getComputedStyle(e,'::before');return {font:parseFloat(s.fontSize),border:s.borderTopWidth,bg:s.backgroundImage,width:parseFloat(s.width),height:parseFloat(s.height),fill:b.backgroundColor,clip:b.clipPath,transform:b.transform}});
   const historyClosed=await historyArrowStyle();
   assert.equal(historyClosed.font,0,'journal must hide the legacy chevron glyph');
   assert.equal(historyClosed.border,'0px','journal disclosure triangle must be borderless');
   assert.equal(historyClosed.bg,'none','journal disclosure must not regain the legacy gradient box');
   assert.ok(historyClosed.width<=18.6&&historyClosed.height<=18.6,'journal disclosure host must remain compact: '+JSON.stringify(historyClosed));
   assert.match(historyClosed.clip,/polygon/i,'journal disclosure must be one CSS triangle');
   assert.notEqual(historyClosed.fill,'rgba(0, 0, 0, 0)','journal disclosure triangle must be visible');
   const historyHeadRadius=await firstHistory.locator('.history-head').evaluate(e=>parseFloat(getComputedStyle(e).borderTopLeftRadius));
   assert.ok(historyHeadRadius>=12,'journal inner frame must have rounded corners');
   await firstHistory.locator(':scope > summary').click();await page.waitForTimeout(180);
   const historyOpen=await historyArrowStyle();
   assert.notEqual(historyOpen.transform,historyClosed.transform,'journal disclosure triangle must rotate from right to down when opened');
   assert.equal(await page.locator('details.rf263-history[open]').count(),1,'journal workout must expand on tap');
   const healthToggle=firstHistory.locator('.rf-history-health-toggle'),healthArrow=healthToggle.locator('.rf-history-health-chevron');
   const healthArrowTransform=async()=>healthArrow.evaluate(e=>getComputedStyle(e,'::before').transform);
   const healthOpenTransform=await healthArrowTransform();
   assert.equal(await healthToggle.getAttribute('aria-expanded'),'true','Health panel starts expanded inside an opened journal workout');
   await healthToggle.click();await page.waitForTimeout(180);
   assert.equal(await healthToggle.getAttribute('aria-expanded'),'false','Health panel collapse updates accessibility state');
   assert.notEqual(await healthArrowTransform(),healthOpenTransform,'Health disclosure triangle rotates between expanded and collapsed states');
   await healthToggle.click();await page.waitForTimeout(180);
   assert.equal(await firstHistory.locator('.rf130-photo-section').count(),1,'expanded workout must expose the photo journal section');
   assert.equal(await firstHistory.locator('[data-rf130-photo-id]').count(),1,'photo metadata must render only inside the expanded workout');
   assert.match(await firstHistory.locator(':scope > summary').innerText(),/1 fotó/,'collapsed journal header must show photo count');
   const phase3Photo=firstHistory.locator('details.tp3-photo-section');if(await phase3Photo.count()&&await phase3Photo.getAttribute('open')===null)await phase3Photo.locator(':scope > summary').click();
   await page.evaluate(()=>{isNative=()=>true;rf130PhotoPlugin=()=>({pick:async()=>({cancelled:true}),capture:async()=>({cancelled:true})})});
   await firstHistory.locator('.rf130-photo-head button').click();
   await page.locator('#rf130PhotoModal').waitFor({state:'visible'});
   await page.locator('#rf130PhotoModal [data-rf130-label="before"]').click();
   assert.equal(await page.locator('#rf130PhotoModal [data-rf130-label="before"]').evaluate(e=>e.classList.contains('active')),true,'photo label buttons must react');
   await page.locator('#rf130PhotoModal [data-rf130-source="pick"]').click();
   assert.match(await page.locator('#rf130PhotoModal [data-rf130-status]').innerText(),/megszakítva/i,'photo picker button must invoke its handler');
   await page.locator('#rf130PhotoModal [data-rf130-action="close"]').click();
   assert.equal(await page.locator('#rf130PhotoModal').count(),0,'photo modal close button must react');
   assert.equal(await page.locator('.rf263-history-filter').count(),0,'legacy two-boundary filter is removed');
   const inlineFilter=page.locator('.tp155-journal-filter');
   assert.equal(await inlineFilter.count(),1,'journal has one inline date filter');
   assert.equal(await inlineFilter.getAttribute('open'),null,'inline date filter starts collapsed');
   await inlineFilter.locator('summary').click();
   assert.ok(await inlineFilter.getAttribute('open')!==null,'inline calendar expands on tap');
   assert.equal(await page.locator('.rf263-date-dialog').count(),0,'journal no longer opens the old modal date picker');
   const calendarDims=await inlineFilter.locator('.tp155-journal-calendar').evaluate(e=>{const r=e.getBoundingClientRect();return {left:r.left,right:r.right,width:r.width,scrollWidth:e.scrollWidth,clientWidth:e.clientWidth}});
   assert.ok(calendarDims.left>=0&&calendarDims.right<=width&&calendarDims.scrollWidth<=calendarDims.clientWidth,JSON.stringify({width,height,calendarDims}));
   await page.evaluate(()=>tp155JournalSetMonth('2026-09-01',true));
   await page.locator('.tp155-journal-filter[open] [data-date="2026-09-10"]').click();
   assert.equal(await page.locator('details.rf263-history').count(),3,'first range endpoint must not filter the Journal before Eddig is selected');
   assert.deepEqual(await page.evaluate(()=>({filter:rf263HistoryFilter,start:state.tp155JournalRangeStart})),{filter:{from:'',to:''},start:'2026-09-10'},'first selected date is the pending Ettől boundary');
   assert.ok(await page.locator('.tp155-journal-filter').getAttribute('open')!==null,'range calendar remains open for the second endpoint');
   await page.locator('.tp155-journal-filter[open] [data-date="2026-09-15"]').click();
   assert.equal(await page.locator('details.rf263-history').count(),2,'inclusive Ettől–Eddig range must show both boundary workouts');
   assert.deepEqual(await page.evaluate(()=>rf263HistoryFilter),{from:'2026-09-10',to:'2026-09-15'},'two selected dates become the inclusive Journal range');
   const rangeStart=page.locator('.tp155-journal-filter [data-date="2026-09-10"]'),rangeEnd=page.locator('.tp155-journal-filter [data-date="2026-09-15"]');
   assert.equal(await rangeStart.getAttribute('aria-pressed'),'true','Ettől endpoint remains selected');
   assert.equal(await rangeEnd.getAttribute('aria-pressed'),'true','Eddig endpoint remains selected');
   assert.equal(await page.locator('.tp155-journal-filter [data-date="2026-09-12"].is-range').count(),1,'intermediate days must be visibly part of the selected range');
   const matteRange=await page.locator('.tp155-journal-filter.tp155-range-active').evaluate(e=>({shadow:getComputedStyle(e).boxShadow,border:getComputedStyle(e).borderTopColor}));
   assert.equal(matteRange.shadow,'none','Basic active Journal range must remain matte without glow');
   await page.locator('.tp155-journal-actions .btn').first().click();
   assert.equal(await page.locator('details.rf263-history').count(),3,'clear filter must restore all journal workouts');
   assert.deepEqual(await page.evaluate(()=>rf263HistoryFilter),{from:'',to:''});
   assert.equal(await page.locator('.tp155-journal-filter').getAttribute('open'),null,'clear filter collapses the calendar');
   await page.evaluate(()=>rf200SetTheme('blue'));
   await page.locator('.tp155-journal-filter summary').click();
   await page.evaluate(()=>tp155JournalSetMonth('2026-09-01',true));
   await page.locator('.tp155-journal-filter[open] [data-date="2026-09-01"]').click();
   await page.locator('.tp155-journal-filter[open] [data-date="2026-09-15"]').click();
   const neonSelected=await page.locator('.tp155-journal-filter [data-date="2026-09-15"]').evaluate(e=>({background:getComputedStyle(e).backgroundColor,border:getComputedStyle(e).borderTopColor}));
   assert.notEqual(neonSelected.background,'rgba(0, 0, 0, 0)','range endpoint must remain visibly highlighted in Neon themes');
   if(process.env.TRAINPILOT_SCREENSHOT&&width===390&&height===844)await page.screenshot({path:process.env.TRAINPILOT_SCREENSHOT});
   await page.evaluate(()=>{document.documentElement.style.fontSize='20px'});
   assert.ok(await page.locator('.tp155-journal-calendar').evaluate(e=>e.scrollWidth<=e.clientWidth),'inline calendar must not overflow with enlarged text');
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'horizontal overflow');
   assert.deepEqual(errors,[]);
   console.log(`PASS journal calendar ${width}x${height}`);
   await context.close();
  }
 }finally{if(browser)await browser.close();await new Promise(resolve=>server.close(resolve))}
})().catch(e=>{console.error(e);process.exitCode=1});
