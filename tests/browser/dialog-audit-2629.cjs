const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');

(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
  fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)});
 });
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:700},locale:'hu-HU'});
  const nativeDialogs=[];
  page.on('dialog',async d=>{nativeDialogs.push({type:d.type(),message:d.message()});await d.dismiss()});
  await page.goto(`http://127.0.0.1:${server.address().port}`);
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.waitForTimeout(100);
  assert.equal(await page.evaluate(()=>window.TrainPilotDialogAudit?.version),'2629');

  const cancel=async()=>{const m=page.locator('#tp2628Dialog');await m.waitFor({state:'visible'});await m.locator('[data-tp2628-cancel]').click();await m.waitFor({state:'detached'})};
  const expectCancel=async re=>{const m=page.locator('#tp2628Dialog');await m.waitFor({state:'visible'});assert.match(await m.innerText(),re);await cancel()};

  // Manual weight edit/delete.
  const date=new Date().toISOString();
  await page.evaluate(d=>db.set('weights',[{kg:66.5,date:d}]),date);
  await page.evaluate(d=>{void rf215Edit(d)},date);
  await page.locator('#tp2628Dialog [data-tp2629-input]').waitFor({state:'visible'});
  assert.match(await page.locator('#tp2628Dialog').innerText(),/Testsúly|Body weight|Gewicht|greutate/i);await cancel();
  await page.evaluate(d=>rf215Del(d),date);await expectCancel(/Testsúlyadat törlése/);
  assert.equal(await page.evaluate(()=>weights().length),1,'cancel keeps manual weight row');

  // Starting over a saved draft.
  await page.evaluate(()=>db.set('draft',{session:{programName:'Teszt'},workout:'A',current:0}));
  await page.evaluate(()=>startWorkout(nextWorkout()));await expectCancel(/Új edzés indítása/);

  // Partial workout finish.
  await page.evaluate(()=>{
   state.workout='A';state.tab='plan';state.session={workout:'A',programId:'home-basic',programName:'Teszt',dayId:'A',started:new Date().toISOString(),exercises:[{id:'db-squat',hu:'Teszt',sets:[{set:1,weight:1,reps:'1',done:true},{set:2,weight:1,reps:'',done:false}]}]};
   finishWorkout();
  });
  await expectCancel(/Részleges edzés mentése/);
  assert.ok(await page.evaluate(()=>!!state.session),'cancel keeps partial workout active');
  await page.evaluate(()=>{state.session=null;state.workout=null;db.set('draft',null)});

  // Settings resets.
  await page.evaluate(()=>resetPlan());await expectCancel(/Alapértékek visszaállítása/);
  await page.evaluate(()=>resetStarter());await expectCancel(/Kímélő profil/);

  // Program activation and custom-program creation/deletion.
  const alt=await page.evaluate(()=>programs().find(p=>p.id!==activeProgramId())?.id||null);
  if(alt){await page.evaluate(id=>activateProgram(id),alt);await expectCancel(/Program aktiválása/)}
  await page.evaluate(()=>{void createCustomProgram()});
  await page.locator('#tp2628Dialog [data-tp2629-input]').waitFor({state:'visible'});await expectCancel(/Új saját program/);
  await page.evaluate(()=>{const ps=programs();ps.push({id:'tp2629-custom',name:'Dialog teszt',builtin:false,days:[{id:'A',name:'A',exercises:['db-squat']}],updatedAt:Date.now()});db.set('programs',ps);deleteCustomProgram('tp2629-custom')});
  await expectCancel(/Saját program törlése/);

  // Custom exercise deletion.
  await page.evaluate(()=>{db.set('exercises',[...exercises(),{id:'tp2629-ex',hu:'Teszt',en:'Test',custom:true,sets:1,reps:'1',weight:0,loadType:'bodyweight'}]);deleteCustomExercise14('tp2629-ex')});
  await expectCancel(/Saját gyakorlat törlése/);

  // History deletion.
  const historyKey=await page.evaluate(()=>{const end=new Date(),start=new Date(end.getTime()-30*60000);const row={id:'tp2629-history',started:start.toISOString(),finished:end.toISOString(),programId:'home-basic',programName:'Teszt',dayId:'A',workout:'A',exercises:[]};db.set('history',[row,...history()]);deleteHistoryWorkout(rf142WorkoutKey(row));return rf142WorkoutKey(row)});
  await expectCancel(/Edzés törlése/);
  assert.ok(await page.evaluate(k=>history().some(x=>rf142WorkoutKey(x)===k),historyKey),'cancel keeps history workout');

  // Editable-plan reset/remove confirmations.
  await page.evaluate(()=>rf153ResetDay('A'));await expectCancel(/Programnap visszaállítása/);
  await page.evaluate(()=>rf154Remove('A',0));await expectCancel(/Gyakorlat eltávolítása/);

  // Schedule moving prompt.
  const sid=await page.evaluate(()=>{const id=crypto.randomUUID(),st=new Date(Date.now()+86400000),en=new Date(st.getTime()+45*60000);db.set('scheduled',[{id,programId:'home-basic',dayId:'A',workout:'A',start:st.toISOString(),end:en.toISOString(),updatedAt:Date.now(),cancelled:false,status:'planned'}]);return id});
  await page.evaluate(id=>{void editSchedulePrompt(id)},sid);
  await page.locator('#tp2628Dialog [data-tp2629-input]').waitFor({state:'visible'});await expectCancel(/Edzés áthelyezése/);

  // Backup restore.
  const backup=await page.evaluate(()=>JSON.stringify(makeBackup()));
  await page.evaluate(text=>restoreText(text),backup);await expectCancel(/Biztonsági mentés visszatöltése/);

  // Google connect/disconnect show themed confirmation before bridge work.
  await page.evaluate(()=>{window.__tp2629OldNative=isNative;isNative=()=>true;cloudBusy=false;void connectGoogle()});
  await expectCancel(/Google-fiók kapcsolása/);
  await page.evaluate(()=>{cloudProfile={sub:'tp2629',name:'Teszt',email:'test@example.invalid'};cloudBusy=false;void disconnectGoogle()});
  await expectCancel(/Google-fiók leválasztása/);
  await page.evaluate(()=>{cloudProfile=null;isNative=window.__tp2629OldNative;delete window.__tp2629OldNative});

  assert.deepEqual(nativeDialogs,[],'2629 audited paths must never open native WebView confirm/prompt dialogs');
  console.log('PASS: 2629 active confirm/prompt paths use app-owned dialogs; no native browser dialogs.');
 }finally{
  if(browser)await browser.close();
  await new Promise(r=>server.close(r));
 }
})().catch(e=>{console.error(e);process.exit(1)});
