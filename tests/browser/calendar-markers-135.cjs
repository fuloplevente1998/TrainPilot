'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{
 const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data);});
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
  const red={background:'rgb(71, 37, 41)',border:'rgb(116, 64, 72)',color:'rgb(255, 217, 220)'},green={background:'rgb(39, 75, 56)',border:'rgb(63, 118, 87)',color:'rgb(223, 247, 232)'};
  async function palette(selector,expected){
   await page.waitForFunction(({selector,background})=>getComputedStyle(document.querySelector(selector)).backgroundColor===background,{selector,background:expected.background});
   assert.deepEqual(await page.locator(selector).evaluate(e=>{const s=getComputedStyle(e);return {background:s.backgroundColor,border:s.borderTopColor,color:s.color}}),expected,selector);
  }
  page.on('pageerror',e=>errors.push(String(e)));await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>TrainPilotBoot.finished);fs.mkdirSync('ui-evidence',{recursive:true});await page.evaluate(()=>document.fonts.ready);
  const initial=await page.evaluate(()=>{
   const p={...tp128Template('home-basic'),id:'calendar-135',builtin:false,name:'My calendar program'};
   p.days.push({...JSON.parse(JSON.stringify(p.days[0])),id:'C',name:'Long custom evening workout'});db.set('programs',[p]);db.set('activeProgramId',p.id);
   const rows=['2021-02','2026-10','2026-08'].flatMap(month=>[1,10,11,12,22].map((n,i)=>({...makeScheduleItem(p,month+'-'+String(n).padStart(2,'0'),'08:00',45,['A','A','B','C','B'][i]),id:month+'-'+n,status:i===0?'completed':'planned'})));
   db.set('scheduled',rows);db.set('history',[]);return {rows,programs:programs()};
  });
  for(const [width,height] of [[320,568],[320,740],[393,873],[873,393]])for(const lang of ['hu','en','de','ro'])for(const month of ['2021-02','2026-10','2026-08']){
   await page.setViewportSize({width,height});await page.evaluate(({lang,month})=>{tp155R4ClosePanel(false);db.set('language',lang);rf200SetTheme(lang==='hu'?'yellow':'classicBlue');state.calendarMonth=month;state.rf2211CalendarDate='';go('calendar');},{lang,month});
   const audit=await page.locator('.calendar-grid').evaluate(grid=>{
    const cells=[...grid.children],marks=[];
    for(const cell of cells){const badge=cell.querySelector('b');if(!badge)continue;const date=cell.querySelector('span'),a=date.getBoundingClientRect(),b=badge.getBoundingClientRect(),c=cell.getBoundingClientRect(),range=document.createRange();range.selectNodeContents(badge);const glyph=range.getBoundingClientRect();marks.push({label:badge.textContent,width:b.width,glyphWidth:glyph.width,size:parseFloat(getComputedStyle(badge).fontSize),fits:b.left>=c.left&&b.right<=c.right+1&&b.top>=c.top&&b.bottom<=c.bottom+1,overlap:Math.min(a.right,b.right)-Math.max(a.left,b.left)>1&&Math.min(a.bottom,b.bottom)-Math.max(a.top,b.top)>1,readable:badge.textContent.length>1||glyph.width<=b.width+1});}
    return {heights:cells.map(c=>c.getBoundingClientRect().height),rows:Math.ceil(cells.length/7),marks,overflow:grid.scrollWidth-grid.clientWidth,planner:document.querySelector('#tp155R4PanelHost .rf2211-planner').getBoundingClientRect().bottom,frame:document.querySelector('#tp155R4PanelHost .tp155-r4-panel').getBoundingClientRect().bottom};
   });
   const label=width+'/'+height+'/'+lang+'/'+month;
   assert.ok(Math.max(...audit.heights)-Math.min(...audit.heights)<1,'equal date heights '+label+' '+JSON.stringify(audit.heights));
   assert.equal(audit.rows,month==='2021-02'?4:month==='2026-10'?5:6);assert.ok(audit.overflow<=1,label);
   if(height>640)assert.ok(audit.planner<=audit.frame+1,'whole planner fits '+label+' '+JSON.stringify(audit));
   assert.deepEqual(audit.marks.map(m=>m.label),['✓','A','B','Long custom evening workout','B']);
   for(const mark of audit.marks){assert.ok(mark.size>=12,'larger mark '+label);assert.equal(mark.fits,true,'mark inside cell '+label);assert.equal(mark.overlap,false,'no date overlap '+label);assert.equal(mark.readable,true,'single mark is not clipped '+label+' '+JSON.stringify(mark));}
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,label);
   if(width===393&&lang==='hu'&&month==='2026-10')await page.screenshot({path:'ui-evidence/calendar-markers-135.png'});
   const cell=page.locator('.cal-cell[onclick*="'+month+'-10"]');await cell.click();await page.locator('#tp108CalendarDay .tp108-day-close').click();assert.equal(await cell.getAttribute('aria-pressed'),'true','selection retained');
   if(month==='2026-10'){
    const value=await page.locator('#rf2211Start').inputValue();
    await page.evaluate(()=>document.querySelector('#rf2211Start').closest('.tp-temporal').querySelector('.tp-temporal-trigger').click());
    await palette('#tpTemporalPicker .tp-modal-close',red);
    await palette('#tpTemporalPicker .tp-temporal-actions .btn:first-child',red);
    await palette('#tpTemporalPicker .tp-temporal-actions .btn:last-child',green);
    assert.notEqual(await page.locator('#tpTemporalPicker .tp-temporal-month .btn').first().evaluate(e=>getComputedStyle(e).backgroundColor),red.background,'month navigation stays neutral');
    if(width===393&&lang==='hu')await page.screenshot({path:'ui-evidence/date-action-colors-135.png'});
    await page.locator('#tpTemporalPicker .tp-modal-close').click();assert.equal(await page.locator('#rf2211Start').inputValue(),value,'close preserves start date');
    await page.evaluate(()=>{const input=document.createElement('input');input.id='color-test-date';input.className='field';input.type='date';input.value='2026-09-15';document.querySelector('#tp155R4PanelHost main').append(input);rf260EnhanceTemporalFields()});
    const openDate=()=>page.evaluate(()=>document.querySelector('#color-test-date').closest('.tp-temporal').querySelector('.tp-temporal-trigger').click());
    const today=await page.evaluate(()=>rf263DateKey(new Date()));
    await openDate();await page.locator('#tpTemporalPicker .tp-temporal-actions .btn:last-child').click();assert.equal(await page.locator('#color-test-date').inputValue(),today,'Today selects local date');
    await openDate();await page.locator('#tpTemporalPicker .tp-temporal-actions .btn:first-child').click();assert.equal(await page.locator('#color-test-date').inputValue(),'','Delete clears date');
    await page.evaluate(()=>document.querySelector('#color-test-date').min='2050-01-01');await openDate();
    await palette('#tpTemporalPicker .tp-temporal-actions .btn:last-child',green);assert.equal(await page.locator('#tpTemporalPicker .tp-temporal-actions .btn:last-child').isDisabled(),true,'Today respects minimum date');await page.locator('#tpTemporalPicker .tp-modal-close').click();
    await page.evaluate(()=>{tp155R4ClosePanel(false);go('history')});await page.locator('.tp155-journal-filter summary').click();
    await palette('.tp155-journal-actions .btn:first-child',red);await palette('.tp155-journal-actions .btn:last-child',green);
    assert.equal(await page.locator('.tp155-journal-actions .btn:first-child').isDisabled(),true,'empty filter cannot be cleared');
    await page.locator('.tp155-journal-actions .btn:last-child').click();assert.deepEqual(await page.evaluate(()=>rf263HistoryFilter),{from:today,to:today},'Journal Today selects inclusive local-day filter');
    await palette('.tp155-journal-actions .btn:first-child',red);await palette('.tp155-journal-actions .btn:last-child',green);
    if(width===393&&lang==='hu')await page.screenshot({path:'ui-evidence/journal-action-colors-135.png'});
    await page.locator('.tp155-journal-actions .btn:first-child').click();assert.deepEqual(await page.evaluate(()=>rf263HistoryFilter),{from:'',to:''},'clear restores unfiltered Journal');assert.equal(await page.locator('.tp155-journal-filter').getAttribute('open'),null,'clear collapses calendar');
   }
  }
  assert.deepEqual(await page.evaluate(()=>({rows:scheduled(),programs:programs()})),initial,'visual operations preserve schedules and programs');
  await page.reload();await page.waitForFunction(()=>TrainPilotBoot.finished);assert.deepEqual(await page.evaluate(()=>({rows:scheduled(),programs:programs()})),initial,'restart retains schedules');assert.deepEqual(errors,[]);
  console.log('PASS #135: equal 4/5/6 week rows; readable markers; 48 language/phone/month cases, portrait/landscape, yellow/basic themes; red close/delete and green Today in picker/Journal; date constraints and filter actions; immutable data and restart');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
