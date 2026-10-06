'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data);});
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));
  await page.clock.install({time:new Date('2026-10-06T13:31:00+02:00')});
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>TrainPilotBoot.finished);
  const apply=page.locator('[data-tp110-apply]'),status=page.locator('[data-tp110-replan-status]');
  const choose=async(id,value)=>{
   const wrap=page.locator('#'+id).locator('..');await wrap.locator('.tp-select-trigger').click();
   await wrap.locator('.tp-select-option[data-value="'+value+'"]').click();
  };
  const fixture=async kind=>page.evaluate(kind=>{
   tp155R4ClosePanel(false);state.session=null;db.set('draft',null);db.set('history',[]);
   const p=activeProgram(),row=(id,date,day='A')=>({...makeScheduleItem(p,date,'08:00',45,day),id});
   const old=row('old-'+kind,'2026-10-03'),future=row('future-'+kind,'2026-10-15','B');
   const log={id:'log-'+kind,scheduleId:old.id,started:old.start,finished:old.end,exercises:[{...byId('db-floor-press'),sets:[{done:true,reps:10}]}]};
   let rows=[];
   if(kind==='future')rows=[future];
   if(['elapsed','deleted','completed','skipped'].includes(kind))rows=[old];
   if(kind==='completed'){old.status='completed';db.set('history',[log]);}
   if(kind==='skipped')old.status='skipped';
   if(kind==='log-only')db.set('history',[log]);
   if(kind==='mixed'){
    const cancelled={...row('deleted-mixed','2026-10-04'),cancelled:true,status:'cancelled'},draft=row('draft-mixed','2026-10-05','B'),other={...row('other-mixed','2026-10-02'),programId:'gym-ppl'};
    old.status='completed';rows=[old,cancelled,future,draft,other];db.set('history',[log]);db.set('draft',{session:{scheduleId:draft.id}});
   }
   db.set('scheduled',rows);db.set('plannerSettings',{...plannerSettings(),mode:'alternate',time:'18:00',minutes:45});go('calendar');
   return {program:p,rows,logs:history(),draft:db.get('draft',null)};
  },kind);
  for(const kind of ['empty','future','elapsed','skipped','deleted','completed','log-only','mixed']){
   const initial=await fixture(kind);
   // Exercise the real calendar deletion handler and confirmation, not only a synthetic tombstone.
   if(kind==='deleted'){
    await page.evaluate(id=>rf209DeleteSchedule(id),initial.rows[0].id);
    await page.locator('[data-tp2628-confirm]').click();await page.waitForFunction(()=>scheduled()[0].cancelled);
   }
   const before=await page.evaluate(()=>({rows:scheduled(),logs:history(),draft:db.get('draft',null)}));
   await page.locator('.tp110-replan-link').click();assert.equal(await apply.isEnabled(),true,kind+' is replannable');
   await choose('tp110ReplanDay','B');
   await page.locator('#tp110ReplanDate').locator('..').locator('.tp-temporal-trigger').click();
   await page.locator('#tpTemporalPicker [data-tp-date="2026-10-01"]').click();
   assert.equal(await page.locator('#tp110ReplanDate').inputValue(),'2026-10-01','past dates available');
   const expected=['future','elapsed','skipped','deleted'].includes(kind)?1:2;
   assert.equal(await page.locator('.tp110-replan-row').count(),expected,kind+' preview');
   assert.deepEqual(await page.evaluate(()=>scheduled()),before.rows,'review does not save '+kind);
   await apply.click();assert.equal(await status.getAttribute('data-kind'),'success',kind+' saves');
   const saved=await page.evaluate(()=>scheduled());
   const editable=saved.filter(x=>x.programId===initial.program.id&&!x.cancelled&&x.status==='planned'&&x.id!=='draft-mixed').sort((a,b)=>Date.parse(a.start)-Date.parse(b.start));
   assert.equal(editable.length,expected);assert.equal(editable[0].dayId,'B');assert.equal(editable[0].workout,'B');
   assert.equal(editable[0].start,'2026-10-01T'+(['empty','completed','log-only'].includes(kind)?'16':'06')+':00:00.000Z');
   for(const row of before.rows){
    if(row.cancelled||row.status==='completed'||row.id==='draft-mixed'||row.programId!==initial.program.id)assert.deepEqual(saved.find(x=>x.id===row.id),row,'protected '+row.id);
   }
   if(kind==='deleted'||kind==='mixed'){
    const tombstone=before.rows.find(x=>x.cancelled),restored=saved.filter(x=>x.replannedFromId===tombstone.id);
    assert.equal(restored.length,1);assert.notEqual(restored[0].id,tombstone.id);
   }
   assert.deepEqual(await page.evaluate(()=>history()),before.logs);assert.deepEqual(await page.evaluate(()=>db.get('draft',null)),before.draft);
   await apply.click();assert.deepEqual(await page.evaluate(()=>scheduled()),saved,'repeated apply is idempotent '+kind);
   await page.reload();await page.waitForFunction(()=>TrainPilotBoot.finished);assert.deepEqual(await page.evaluate(()=>scheduled()),saved,'restart preserves '+kind);
  }
  // Switching programs rebuilds its day selector and never edits the other program.
  await fixture('future');await page.locator('.tp110-replan-link').click();
  const original=await page.evaluate(()=>scheduled());await choose('tp110ReplanProgram','gym-ppl');
  assert.deepEqual(await page.locator('#tp110ReplanDay option').evaluateAll(nodes=>nodes.map(n=>n.value)),['','A','B','C']);
  await choose('tp110ReplanDay','C');assert.equal(await page.locator('.tp110-replan-row').count(),3);await apply.click();
  const programSaved=await page.evaluate(()=>scheduled());assert.deepEqual(programSaved[0],original[0]);assert.deepEqual(programSaved.slice(1).map(x=>x.dayId),['C','A','B']);
  // Review detects source history/draft/cadence changes; quota failure retains all data.
  await fixture('empty');await page.locator('.tp110-replan-link').click();
  await page.evaluate(()=>db.set('history',[{id:'new-history',started:'2026-10-07T06:00:00Z',finished:'2026-10-07T07:00:00Z',exercises:[{...byId('db-floor-press'),sets:[{done:true,reps:10}]}]}]));
  await apply.click();assert.equal(await status.getAttribute('data-kind'),'pending');assert.deepEqual(await page.evaluate(()=>scheduled()),[]);
  await page.evaluate(()=>db.set('draft',{session:{scheduleId:'new-draft'}}));await apply.click();assert.equal(await status.getAttribute('data-kind'),'pending');
  await page.evaluate(()=>db.set('plannerSettings',{...plannerSettings(),mode:'daily'}));await apply.click();assert.equal(await status.getAttribute('data-kind'),'pending');
  await page.evaluate(()=>{window.__originalSet=db.set;db.set=(key,value)=>{if(key==='scheduled')throw Error('quota');return window.__originalSet(key,value);};});
  await apply.click();assert.equal(await status.getAttribute('data-kind'),'error');assert.deepEqual(await page.evaluate(()=>scheduled()),[]);
  await page.evaluate(()=>db.set=window.__originalSet);await apply.click();assert.equal(await status.getAttribute('data-kind'),'success');assert.equal((await page.evaluate(()=>scheduled())).length,2);
  // The fallback cycle and newly added controls fit the supported phone/language sizes.
  for(const [width,height] of [[320,568],[393,873],[873,393]])for(const lang of ['hu','en','de','ro']){
   await page.setViewportSize({width,height});await fixture('empty');
   await page.evaluate(lang=>{db.set('language',lang);rf200SetTheme('gold');render();},lang);
   await page.locator('.tp110-replan-link').click();assert.equal(await apply.isEnabled(),true);assert.equal(await page.locator('.tp110-replan-row').count(),2);
   for(const id of ['tp110ReplanProgram','tp110ReplanDay','tp110ReplanDate']){
    const trigger=page.locator('#'+id).locator('..').locator('.tp-select-trigger');await trigger.scrollIntoViewIfNeeded();
    const control=await trigger.boundingBox(),main=await page.locator('.tp110-replan').boundingBox();
    assert.ok(control.x>=main.x&&control.x+control.width<=main.x+main.width+1,'picker fits panel '+id+'/'+width+'/'+lang);
   }
   await apply.scrollIntoViewIfNeeded();const b=await apply.boundingBox();assert.ok(b.y+b.height<=height+1);assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true);
   if(width===393&&lang==='hu'){fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/replan-all-123.png'});}
   await page.locator('.tp106-builder-header button').click();assert.equal(await page.locator('#tp155R4PanelHost').getAttribute('data-panel'),'calendar');
  }
  assert.deepEqual(errors,[]);
  console.log('PASS #123 all states: empty/future/elapsed/skipped/actual deletion/completed/log-only/mixed; first day and past date; immutable history/tombstones/draft/other program; repeat/restart; program switching; stale history/draft/cadence; quota retry; 12 phone/language cases');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
