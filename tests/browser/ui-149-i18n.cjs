const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');

const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';
 const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
 fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}
  res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d);
 });
});
const listen=()=>new Promise(r=>server.listen(0,'127.0.0.1',r));
const base=()=>'http://127.0.0.1:'+server.address().port+'/';
const huLeak=/[őűŐŰ]|\b(Kezdőlap|Edzés|Naptár|Programok|Napló|Egészség|Beállítások|gyakorlat|sorozat|testsúly|Vissza|Keresés|Felszerelés|Izomcsoport|Kerülendő|Egyedi|Saját|Mentés|Törlés|Hozzáadás|Következő|Pihenő|Alvás|Pulzus|Regeneráció|Nincs|Tervezett|Teljesítve|Kihagyott|Fókusz|Tapasztalat|Beosztás|Edzésfelosztás|Guggolás|Törzs|Láb|Mell|Hát|Váll)\b/i;
const problems=[];
const expected={
 en:['Home','Workout','Calendar','Programs','Log','Health','Settings','Areas to avoid','Individual exercise exclusions','Exercise library'],
 de:['Start','Training','Kalender','Programme','Protokoll','Gesundheit','Einstellungen','Zu vermeidende Bereiche','Einzelne Übungsausschlüsse','Übungsbibliothek'],
 ro:['Acasă','Antrenament','Calendar','Programe','Jurnal','Sănătate','Setări','Zone de evitat','Excluderi individuale de exerciții','Bibliotecă de exerciții']
};
async function boot(page){await page.goto(base());await page.waitForFunction(()=>window.TrainPilotBoot?.finished);await page.waitForTimeout(80)}
async function setLang(page,lang){await page.evaluate(lang=>{db.set('language',lang);document.documentElement.lang=rf212Lang();render();},lang);await page.waitForTimeout(80)}
async function text(page){return (await page.locator('body').innerText()).replace(/\s+/g,' ').trim()}
async function audit(page,label){
 await page.waitForTimeout(40);
 const data=await page.evaluate(()=>{
  const doc=document.documentElement,off=[];
  for(const e of document.querySelectorAll('.hero,.card,.setting,.program-card,.exercise,.top,.tabs,.rf148-profile-accordion,.tp146-day-head')){
   const s=getComputedStyle(e),r=e.getBoundingClientRect();if(s.display==='none'||s.visibility==='hidden'||r.width===0||r.height===0)continue;
   if(e.scrollWidth>e.clientWidth+4)off.push({cls:e.className,text:(e.textContent||'').trim().slice(0,100),sw:e.scrollWidth,cw:e.clientWidth});
  }
  return {lang:document.documentElement.lang,overflow:doc.scrollWidth-innerWidth,off};
 });
 if(data.overflow>4)problems.push(label+' page overflow '+JSON.stringify(data));
 if(data.off.length)problems.push(label+' local overflow '+JSON.stringify(data.off,null,2));
 const t=await text(page);
 const leak=t.match(huLeak);if(leak)problems.push(label+' Hungarian leak ['+leak[0]+']: '+t.slice(0,2200));
 if(/\b(?:nav|common|language|workout|planner|backup|exercise|settings)\.[a-z][a-z0-9_.-]*\b/i.test(t))problems.push(label+' raw translation key visible: '+t.slice(0,2200));
 if(/\bundefined\b/i.test(t))problems.push(label+' contains undefined: '+t.slice(0,2200));
 const empty=await page.evaluate(()=>[...document.querySelectorAll('button')].filter(b=>{
   const r=b.getBoundingClientRect(),cs=getComputedStyle(b);
   if(cs.display==='none'||cs.visibility==='hidden'||r.width===0||r.height===0)return false;
   return !(b.textContent||'').trim()&&!b.getAttribute('aria-label')&&!b.getAttribute('title');
 }).map(b=>b.outerHTML.slice(0,220)));
 if(empty.length)problems.push(label+' empty visible button: '+empty.join(' | '));
 return t;
}
(async()=>{
 await listen();let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  for(const viewport of [{width:320,height:640},{width:360,height:800},{width:393,height:873},{width:412,height:915},{width:640,height:360}]){
   const page=await browser.newPage({viewport,locale:'hu-HU'});
   page.on('dialog',d=>d.dismiss());await boot(page);
   const engine=await page.evaluate(()=>({
    missing:window.TrainPilotI18n?.audit?.().missing||['engine-missing'],
    exerciseNames:window.TrainPilotI18n?.exerciseNameAudit?.()||{total:0,missing:{engine:['missing']}},
    programs:window.TrainPilotI18n?.programAudit?.()||{total:0,missing:['engine-missing']},
    planner:window.TrainPilotI18n?.plannerAudit?.()||{missing:['engine-missing']},
    en:window.t?.('workout.start',{},'en'),
    de:window.t?.('workout.start',{},'de'),
    ro:window.t?.('workout.start',{},'ro')
   }));
   assert.deepEqual(engine.missing,[],'key catalog must be complete in all four languages');
   assert.equal(engine.exerciseNames.total,100,'built-in exercise library must contain exactly 100 exercises');
   for(const [field,missing] of Object.entries(engine.exerciseNames.missing))assert.deepEqual(missing,[],'missing exercise localization for '+field);
   assert.equal(engine.programs.total,13,'built-in program library must contain exactly 13 programs');
   assert.deepEqual(engine.programs.missing,[],'built-in program localization must cover every day');
   assert.deepEqual(engine.planner.missing,[],'planner semantic dictionaries must be complete');
   const calendarJournalAudit=await page.evaluate(()=>TrainPilotI18n.calendarJournalAudit?.());
   for(const [lang,missing] of Object.entries(calendarJournalAudit||{}))assert.deepEqual(missing,[],'calendar/journal catalog missing keys for '+lang);
   const healthCoachAudit=await page.evaluate(()=>TrainPilotI18n.healthCoachAudit?.());
   for(const [lang,missing] of Object.entries(healthCoachAudit||{}))assert.deepEqual(missing,[],'health/coach catalog missing keys for '+lang);
   const workoutAudit=await page.evaluate(()=>TrainPilotI18n.workoutAudit?.());
   for(const [lang,missing] of Object.entries(workoutAudit||{}))assert.deepEqual(missing,[],'workout/progression catalog missing keys for '+lang);
   const workoutWords=await page.evaluate(()=>{
     const out={};
     for(const lang of ['hu','en','de','ro'])out[lang]={next:t('workout.next',{},lang),finish:t('workout.finish',{},lang),progress:t('progress.title',{},lang),rest:t('workout.rest',{},lang)};
     return out;
   });
   assert.equal(workoutWords.en.next,'Next exercise');
   assert.equal(workoutWords.de.finish,'Training beenden');
   assert.equal(workoutWords.ro.rest,'Pauză');
   const cloudAudit=await page.evaluate(()=>TrainPilotI18n.cloudAudit?.());
   for(const [lang,missing] of Object.entries(cloudAudit||{}))assert.deepEqual(missing,[],'cloud/calendar-sync catalog missing keys for '+lang);
   const cloudWords=await page.evaluate(()=>({en:{title:t('cloud.title',{},'en'),drive:t('cloud.syncDriveNow',{},'en')},de:{conflict:t('cloud.conflictTitle',{},'de')},ro:{calendar:t('cloud.syncCalendarNow',{},'ro')}}));
   assert.equal(cloudWords.en.title,'Google account and sync');
   assert.equal(cloudWords.de.conflict,'Synchronisierungskonflikt');
   assert.equal(cloudWords.ro.calendar,'Sincronizează calendarul acum');
   const dialogAudit=await page.evaluate(()=>TrainPilotI18n.dialogAudit?.());
   for(const [lang,missing] of Object.entries(dialogAudit||{}))assert.deepEqual(missing,[],'dialog catalog missing keys for '+lang);
   const dialogWords=await page.evaluate(()=>({en:t('dialog.newWorkout.message',{},'en'),de:t('dialog.weightEdit.title',{},'de'),ro:t('dialog.scheduleMove.title',{},'ro')}));
   assert.match(dialogWords.en,/Start a new workout/);
   assert.equal(dialogWords.de,'Körpergewicht bearbeiten');
   assert.equal(dialogWords.ro,'Mută antrenamentul');
   const weightAudit=await page.evaluate(()=>TrainPilotI18n.weightAudit?.());
   for(const [lang,missing] of Object.entries(weightAudit||{}))assert.deepEqual(missing,[],'weight catalog missing keys for '+lang);
   assert.equal(await page.evaluate(()=>t('weight.title',{},'de')),'Körpergewichtsprotokoll');
   const settingsAudit=await page.evaluate(()=>TrainPilotI18n.settingsAudit?.());
   for(const [lang,missing] of Object.entries(settingsAudit||{}))assert.deepEqual(missing,[],'settings/backup catalog missing keys for '+lang);
   const settingsWords=await page.evaluate(()=>({en:t('settings.subtitle',{},'en'),de:t('settings.progression',{},'de'),ro:t('backup.help',{},'ro')}));
   assert.match(settingsWords.en,/Personalization/);
   assert.match(settingsWords.de,/Progressives Training/);
   assert.equal(settingsWords.ro,'Salvează datele aplicației într-un fișier sau restaurează o copie anterioară.');
   const shellAudit=await page.evaluate(()=>TrainPilotI18n.shellAudit?.());
   for(const [lang,missing] of Object.entries(shellAudit||{}))assert.deepEqual(missing,[],'shell catalog missing keys for '+lang);
   assert.equal(await page.evaluate(()=>t('brand.tagline',{},'ro')),'planificator inteligent de antrenament');
   const photoAudit=await page.evaluate(()=>TrainPilotI18n.photoAudit?.());
   for(const [lang,missing] of Object.entries(photoAudit||{}))assert.deepEqual(missing,[],'photo catalog missing keys for '+lang);
   assert.equal(await page.evaluate(()=>t('photo.after',{},'en')),'After workout');
   const videoAudit=await page.evaluate(()=>TrainPilotI18n.videoAudit?.());
   for(const [lang,missing] of Object.entries(videoAudit||{}))assert.deepEqual(missing,[],'video catalog missing keys for '+lang);
   const homeProgramAudit=await page.evaluate(()=>TrainPilotI18n.homeProgramAudit?.());
   for(const [lang,missing] of Object.entries(homeProgramAudit||{}))assert.deepEqual(missing,[],'home/program editor catalog missing keys for '+lang);
   const homeWords=await page.evaluate(()=>({en:t('home.activeProgram',{},'en'),de:t('programs.title',{},'de'),ro:t('customExercise.save',{},'ro')}));
   assert.equal(homeWords.en,'Active program');
   assert.equal(homeWords.de,'Trainingsprogramme');
   assert.equal(homeWords.ro,'Salvează exercițiul');
   assert.equal(engine.en,'Start workout');
   assert.equal(engine.de,'Training starten');
   assert.equal(engine.ro,'Pornește antrenamentul');

   await page.evaluate(()=>{db.set('language','hu');document.documentElement.lang=rf212Lang();go('settings');});
   await page.waitForTimeout(60);
   assert.equal(await page.locator('.rf212-language').count(),1,'Hungarian Settings must retain the language selector');
   assert.equal(await page.locator('.tp1511-settings > .rf212-language').count(),1,'Hungarian language selector must use the compact 1.5.1 Settings placement');
   const exerciseFields=await page.evaluate(()=>{
    const e=exercises().find(x=>x.id==='pullup');
    const custom={id:'custom-audit',custom:true,hu:'Piros kézisúlyzós gyakorlat',equipment:'Saját különleges eszköz',notes:'Saját jegyzet'};
    return {
     en:{name:TrainPilotI18n.exerciseName(e,'en'),note:TrainPilotI18n.exerciseNote(e,'en'),equipment:TrainPilotI18n.equipment(e,'en'),target:TrainPilotI18n.target(e,'en')},
     de:{name:TrainPilotI18n.exerciseName(e,'de'),note:TrainPilotI18n.exerciseNote(e,'de'),equipment:TrainPilotI18n.equipment(e,'de'),target:TrainPilotI18n.target(e,'de')},
     ro:{name:TrainPilotI18n.exerciseName(e,'ro'),note:TrainPilotI18n.exerciseNote(e,'ro'),equipment:TrainPilotI18n.equipment(e,'ro'),target:TrainPilotI18n.target(e,'ro')},
     custom:{
      deName:TrainPilotI18n.exerciseName(custom,'de'),deNote:TrainPilotI18n.exerciseNote(custom,'de'),deEquipment:TrainPilotI18n.equipment(custom,'de'),
      roName:TrainPilotI18n.exerciseName(custom,'ro'),roNote:TrainPilotI18n.exerciseNote(custom,'ro')
     }
    };
   });
   assert.equal(exerciseFields.en.name,'Pull-Up');
   assert.equal(exerciseFields.de.name,'Klimmzug');
   assert.equal(exerciseFields.ro.name,'Tracțiuni');
   assert.ok(exerciseFields.en.note.includes('stable bar'));
   assert.ok(exerciseFields.de.note.includes('stabilen Stange'));
   assert.ok(exerciseFields.ro.note.includes('bară stabilă'));
   assert.equal(exerciseFields.custom.deName,'Piros kézisúlyzós gyakorlat');
   assert.equal(exerciseFields.custom.deNote,'Saját jegyzet');
   assert.equal(exerciseFields.custom.deEquipment,'Saját különleges eszköz');
   assert.equal(exerciseFields.custom.roName,'Piros kézisúlyzós gyakorlat');
   assert.equal(exerciseFields.custom.roNote,'Saját jegyzet');
   const plannerFields=await page.evaluate(()=>{
    const p=generatePersonalProgram({
     age:28,height:180,weight:80,goal:'fitness',experience:'intermediate',activity:'mixed',
     location:'home',gear:Object.keys(GEAR132),minutes:45,cadence:'alternate',split:'upperlower',
     excluded:[],focus:'balanced',avoidAreas:[]
    });
    const custom={id:'custom-program-audit',name:'Piros Program',location:'Saját hely',level:'Saját szint',builtin:false,generated:false,days:[{id:'A',name:'Piros nap',exercises:[]}]};
    return {
     en:{name:TrainPilotI18n.programMeta(p,'name','en'),day:TrainPilotI18n.programDayName(p,p.days[0],'en'),reasons:TrainPilotI18n.generatedReasons(p,'en')},
     de:{name:TrainPilotI18n.programMeta(p,'name','de'),day:TrainPilotI18n.programDayName(p,p.days[0],'de'),reasons:TrainPilotI18n.generatedReasons(p,'de')},
     ro:{name:TrainPilotI18n.programMeta(p,'name','ro'),day:TrainPilotI18n.programDayName(p,p.days[0],'ro'),reasons:TrainPilotI18n.generatedReasons(p,'ro')},
     custom:{
      deName:TrainPilotI18n.programMeta(custom,'name','de'),
      roName:TrainPilotI18n.programMeta(custom,'name','ro'),
      deDay:TrainPilotI18n.programDayName(custom,custom.days[0],'de')
     }
    };
   });
   assert.ok(plannerFields.en.name.startsWith('Personal plan'));
   assert.ok(plannerFields.de.name.startsWith('Persönlicher Plan'));
   assert.ok(plannerFields.ro.name.startsWith('Plan personal'));
   assert.equal(plannerFields.en.day,'Upper body');
   assert.equal(plannerFields.de.day,'Oberkörper');
   assert.equal(plannerFields.ro.day,'Partea superioară');
   assert.ok(plannerFields.en.reasons.some(x=>x.startsWith('Available:')));
   assert.ok(plannerFields.de.reasons.some(x=>x.startsWith('Verfügbar:')));
   assert.ok(plannerFields.ro.reasons.some(x=>x.startsWith('Disponibil:')));
   assert.equal(plannerFields.custom.deName,'Piros Program');
   assert.equal(plannerFields.custom.roName,'Piros Program');
   assert.equal(plannerFields.custom.deDay,'Piros nap');
   for(const lang of ['en','de','ro']){
    await setLang(page,lang);
    assert.equal(await page.evaluate(()=>document.documentElement.lang),lang);
    for(const route of ['home','plan','calendar','programs','history','health','settings']){
      await page.evaluate(r=>go(r),route);const t=await audit(page,lang+':'+route+':'+viewport.width+'x'+viewport.height);
      if(route==='home')assert.ok(t.includes(expected[lang][0]),lang+' missing '+expected[lang][0]);
    }
    await page.evaluate(()=>profileScreen());const pt=await audit(page,lang+':profile:'+viewport.width+'x'+viewport.height);
    for(const x of expected[lang].slice(7,9))assert.ok(pt.includes(x),lang+' profile missing '+x);

    await page.evaluate(()=>rf203ExerciseLibrary());const lt=await audit(page,lang+':library:'+viewport.width+'x'+viewport.height);
    assert.ok(lt.toLocaleLowerCase().includes(expected[lang][9].toLocaleLowerCase()),lang+' library title missing');
    const firstNames=await page.locator('.exercise .ex-name, #libraryResults .tp-library-card h3').allTextContents();
    assert.equal(firstNames.length,100,lang+' should render the complete 100-exercise library');
    const badName=firstNames.find(n=>huLeak.test(n));if(badName)problems.push(lang+' untranslated exercise name: '+badName);

    await page.evaluate(()=>customExerciseScreen());await audit(page,lang+':custom:'+viewport.width+'x'+viewport.height);

    await page.evaluate(()=>{tp2628Confirm('Kilépsz a folyamatban lévő edzésből? A mentett vázlat megmarad, később folytathatod.',{title:'Edzés elhagyása',confirmText:'Kilépés'});});
    await page.waitForTimeout(80);
    const dt=(await page.locator('#tp2628Dialog').innerText()).replace(/\s+/g,' ');
    if(huLeak.test(dt))problems.push(lang+' dialog untranslated: '+dt);
    await page.locator('#tp2628Dialog [data-tp2628-cancel]').click();
   }
   await page.close();
  }

  const persist=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
  await boot(persist);
  await persist.evaluate(()=>{db.set('language','de');render()});await persist.waitForTimeout(50);
  await persist.reload();await persist.waitForFunction(()=>window.TrainPilotBoot?.finished);
  assert.equal(await persist.evaluate(()=>rf212LangSetting()),'de','manual language must survive reload');
  const backupText=await persist.evaluate(()=>{db.set('language','ro');document.documentElement.lang=rf212Lang();return JSON.stringify(makeBackup())});
  assert.equal(JSON.parse(backupText).language,'ro','backup must persist language selection');
  await persist.evaluate(()=>{db.set('language','en');document.documentElement.lang=rf212Lang();render()});
  await persist.evaluate(text=>restoreText(text),backupText);
  await persist.waitForSelector('#tp2628Dialog [data-tp2628-confirm]');
  await persist.locator('#tp2628Dialog [data-tp2628-confirm]').click();
  await persist.waitForFunction(()=>rf212LangSetting()==='ro');
  assert.equal(await persist.evaluate(()=>rf212Lang()),'ro','restore must restore language selection');
  await persist.close();

  const unsupported=await browser.newPage({viewport:{width:393,height:873},locale:'fr-FR'});
  await boot(unsupported);await unsupported.evaluate(()=>{db.set('language','system');render()});await unsupported.waitForTimeout(60);
  assert.equal(await unsupported.evaluate(()=>rf212Lang()),'en','unsupported system language must fall back to English');
  assert.ok((await text(unsupported)).includes('Home'),'unsupported system locale should render English');
  await unsupported.close();

  const sys=await browser.newPage({viewport:{width:393,height:873},locale:'de-DE'});
  await boot(sys);await sys.evaluate(()=>{db.set('language','system');render()});await sys.waitForTimeout(60);
  assert.equal(await sys.evaluate(()=>rf212Lang()),'de');assert.equal(await sys.evaluate(()=>document.documentElement.lang),'de');
  assert.ok((await text(sys)).includes('Start'),'system language should follow de-DE');
  await sys.close();
  if(problems.length){console.error('I18N_PROBLEMS='+problems.length);for(const p of [...new Set(problems)])console.error('I18N_PROBLEM '+p);assert.fail('1.4.9 i18n audit found '+problems.length+' untranslated surfaces');}
  console.log('PASS: TrainPilot 1.4.9 EN/DE/RO coverage, system-language detection and long-label layout audit.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
