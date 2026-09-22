Warning: truncated output (original token count: 364400)
... 409021 bytes omitted ...

/* TrainPilot 1.0.2 — canonical application source.
 * Sections retain the tested initialization order of test2.
 * Edit this file directly; no generated version layers or build-time patches.
 */

// @section startup.js

/* TrainPilot 2.3.2: blank fast boot overlay + measured single final render. */
(function(){
 const boot=window.TrainPilotBoot={loading:true,failed:false,finished:false};
 let watchdog;
 const now=()=>typeof performance!=='undefined'?performance.now():Date.now();
 boot.startedAt=now();
 boot.durationMs=null;
 boot.fail=function(){
  if(boot.failed||boot.finished)return;
  boot.failed=true;clearTimeout(watchdog);
  const overlay=document.getElementById('tpBoot');
  if(!overlay)return;
  overlay.textContent='Az alkalmazás betöltése nem sikerült. ';
  const retry=document.createElement('button');retry.type='button';retry.textContent='Újrapróbálás';
  retry.onclick=()=>window.location.reload();overlay.appendChild(retry);
 };
 window.addEventListener?.('error',()=>{if(boot.loading)boot.fail();},true);
 boot.finish=function(){
  if(boot.finished||boot.failed)return;
  boot.loading=false;
  try{
   render();
   if(!document.querySelector('#app')?.innerHTML)throw Error('Empty initial screen');
  }catch(e){boot.fail();throw e;}
  // 1.2 clean runtime renders the final DOM synchronously; reveal on the next frame.
  const reveal=()=>{
   if(boot.failed)return;
   document.documentElement.classList.remove('tp-booting');
   document.documentElement.classList.add('tp-ready');
   document.getElementById('tpBoot')?.remove();
   boot.finished=true;boot.readyAt=now();boot.durationMs=Math.max(0,boot.readyAt-boot.startedAt);clearTimeout(watchdog);
   try{console.info(`[TrainPilot] local UI ready in ${Math.round(boot.durationMs)} ms`)}catch(_){}
   // Account and cloud work stays off the critical first-paint path.
   setTimeout(()=>{if(typeof initCloud==='function')initCloud();},0);
  };
  const frame=typeof requestAnimationFrame==='function'?requestAnimationFrame:(fn)=>setTimeout(fn,0);
  frame(reveal);
 };
 watchdog=setTimeout(()=>boot.fail(),30000);
})();


// @endsection startup.js

// @section backup.js
var isNative = function isNative(){return !!window.Capacitor?.isNativePlatform?.();};
var nativeFiles = function nativeFiles(){if(!filesPlugin)filesPlugin=window.Capacitor?.registerPlugin?.('NativeFiles')||window.Capacitor?.Plugins?.NativeFiles;if(!filesPlugin)throw Error('A natív fájlkezelő nem érhető el.');return filesPlugin;};
var backupStatus = function backupStatus(){const x=db.get('lastExport',null);return x?`Utolsó ellenőrzött mentés: ${x.name} • ${fmtDate(x.date)}`:'Még nincs ellenőrzött fájlmentés.';};
var makeBackup = function makeBackup(){return {app:'RepForge',version:2,appVersion:'1.1.5',exportedAt:new Date().toISOString(),history:history(),weights:weights(),settings:settings(),plan:plan(),exercises:exercises(),scheduled:typeof scheduled==='function'?scheduled():[]};};
var exportData = async function exportData(){
 try{
  const data=JSON.stringify(makeBackup(),null,2),name='repforge-backup-'+new Date().toISOString().replace(/[:.]/g,'-')+'.json';
  if(isNative()){
   const r=await nativeFiles().save({name,data});if(r.cancelled)return;
   if(!r.verified)throw Error('A fájl ellenőrzése nem sikerült.');
   db.set('lastExport',{name:r.name,date:new Date().toISOString(),bytes:r.bytes});
   alert(`Mentés kész és visszaolvasással ellenőrizve.\n${r.name}\n${r.bytes} bájt\nA fájl a kiválasztott helyen található.`);render();
  }else{
   const b=new Blob([data],{type:'application/json'}),url=URL.createObjectURL(b),a=document.createElement('a');
   a.href=url;a.download=name;document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),60000);
   alert('A letöltés elindult. Ellenőrizd a böngésző Letöltések listájában; a böngészős fájlírást az app nem tudja visszaellenőrizni.');
  }
 }catch(e){alert('Nem készült ellenőrzött mentés.\n'+(e.message||e));}
};
var chooseImport = async function chooseImport(){
 if(state.session){alert('Előbb fejezd be az edzést.');return;}
 if(!isNative()){document.getElementById('importFile').click();return;}
 try{const r=await nativeFiles().open();if(!r.cancelled)restoreText(r.data);}catch(e){alert('Nem sikerült visszatölteni.\n'+(e.message||e));}
};
var importData = async function importData(file){if(!file)return;try{if(file.size>20*1024*1024)throw Error('Maximum 20 MB.');restoreText(await file.text());}catch(e){alert('Hibás mentésfájl.\n'+e.message);}finally{const e=document.getElementById('importFile');if(e)e.value='';}};
var validateBackup = function validateBackup(d){
 const fail=()=>{throw Error('A fájl szerkezete nem érvényes RepForge-mentés.');};
 const obj=x=>x&&typeof x==='object'&&!Array.isArray(x),str=x=>typeof x==='string'&&x.length<=2000,num=x=>typeof x==='number'&&Number.isFinite(x)&&x>=0,id=x=>typeof x==='string'&&/^[a-z0-9-]{1,80}$/.test(x);
 if(!obj(d)||![1,2].includes(d.version)||(d.app!=null&&d.app!=='RepForge'))fail();
 if(!Array.isArray(d.exercises)||!d.exercises.length||d.exercises.length>300||!Array.isArray(d.history)||!Array.isArray(d.weights)||!obj(d.plan)||!obj(d.settings))fail();
 const ids=new Set();
 for(const e of d.exercises){
  if(!obj(e)||!id(e.id)||ids.has(e.id)||!str(e.hu)||!str(e.en)||!str(e.reps)||!Number.isInteger(e.sets)||e.sets<1||e.sets>10||!num(e.weight))fail();
  if(e.loadType!=null&&!['per_hand','total','single_dumbbell','bodyweight'].includes(e.loadType))fail();
  if(e.repUnit!=null&&!['ism.','/kar','/láb','mp'].includes(e.repUnit))fail();
  ids.add(e.id);
 }
 for(const w of ['A','B'])if(!Array.isArray(d.plan[w])||!d.plan[w].length||d.plan[w].length>30||d.plan[w].some(x=>!ids.has(x)))fail();
 if(!num(d.settings.rest)||d.settings.rest<30||d.settings.rest>300)fail();
 for(const h of d.history){
  if(!obj(h)||!['A','B'].includes(h.workout)||!str(h.started)||!Number.isFinite(Date.parse(h.started))||!Array.isArray(h.exercises))fail();
  for(const e of h.exercises){
   if(!obj(e)||!id(e.id)||!str(e.hu)||!Array.isArray(e.sets))fail();
   for(const s of e.sets)if(!obj(s)||!num(s.weight)||!['string','number'].includes(typeof s.reps)||typeof s.done!=='boolean')fail();
  }
 }
 for(const w of d.weights)if(!obj(w)||!num(w.kg)||!Number.isFinite(Date.parse(w.date)))fail();
 if(d.scheduled!=null){if(!Array.isArray(d.scheduled)||d.scheduled.length>500)fail();for(const p of d.scheduled)if(!obj(p)||!id(p.id)||!["A","B"].includes(p.workout)||!Number.isFinite(Date.parse(p.start))||!Number.isFinite(Date.parse(p.end))||Date.parse(p.end)<=Date.parse(p.start)||!num(p.updatedAt)||typeof p.cancelled!=="boolean")fail();}
 return d;
};
var restoreText = function restoreText(text){
 if(text.length>20*1024*1024)throw Error('Maximum 20 MB.');
 const d=validateBackup(JSON.parse(text.replace(/^\uFEFF/,'')));
 if(!confirm(`${d.history.length} edzés és ${d.weights.length} testsúlyadat visszatöltése?\nEz lecseréli az app jelenlegi naplóját és tervét. A félbehagyott edzés törlődik.`))return false;
 const keys=['history','weights','settings','plan','exercises','scheduled','draft'],old=keys.map(k=>localStorage.getItem('repforge:'+k));
 try{for(const k of keys)db.set(k,k==='draft'?null:k==='scheduled'?(d.scheduled||[]):d[k]);}
 catch(e){for(const k of keys)localStorage.removeItem('repforge:'+k);keys.forEach((k,i)=>{if(old[i]!==null)localStorage.setItem('repforge:'+k,old[i]);});throw Error('Nincs elég tárhely. A korábbi adatokat visszaállítottam.');}
 alert('Visszatöltés kész.');render();return true;
};
var persistDraft = function persistDraft(){if(state.session)db.set('draft',{session:state.session,workout:state.workout,current:state.current,restEndAt:state.restEndAt||null});};
var resumeDraft = function resumeDraft(){
 const d=db.get('draft',null);if(!d)return;
 if(!d.session||!['A','B'].includes(d.workout)||!Array.isArray(d.session.exercises)||!d.session.exercises.length||d.session.exercises.some(e=>!byId(e.id))){alert('A félbehagyott edzés nem állítható vissza a jelenlegi tervvel.');return;}
 stopTimer();state.tab='plan';state.session=d.session;state.workout=d.workout;state.current=Math.max(0,Math.min(d.current||0,d.session.exercises.length-1));
 if(d.restEndAt>Date.now()){state.restEndAt=d.restEndAt;state.timer=Math.ceil((d.restEndAt-Date.now())/1000);state.timerId=setInterval(tickRest,250);}
 renderWorkout();
};
let filesPlugin;


document.addEventListener('visibilitychange',()=>{if(document.hidden)persistDraft();});


// @endsection backup.js

// @section demos.js
var demoInfo = function demoInfo(id){const d=DEMOS[id];return d?{provider:d[1],videoId:d[2],credit:d[3]||(d[0]?'Muscle & Strength':'Zi workout'),source:d[0]?'https://www.muscleandstrength.com/exercises/'+d[0]:'https://www.youtube.com/watch?v='+d[2]}:null;};
var demoCard = function demoCard(id){const d=demoInfo(id);if(!d)return '';return `<div class="video-card"><span class="badge">VIDEÓBEMUTATÓ • INTERNET</span><p class="small muted">${esc(d.credit)} • ${d.provider?'külön lejátszó':'videó a forrásoldalon'}</p><button class="btn block" onclick="openDemo('${id}')">${d.provider?'▶ Bemutató megnyitása':'▶ Videó a böngészőben'}</button></div>`;};
var closeDemo = function closeDemo(){document.getElementById('videoModal')?.remove();};
var openDemo = function openDemo(id){
 const d=demoInfo(id);if(!d)return;
 if(navigator.onLine===false){alert('A videóhoz internet kell. Az edzés és a szöveges útmutató offline is használható.');return;}
 if(!d.provider){openVideoLink(d.source);return;}
 closeDemo();
 const modal=document.createElement('div');modal.id='videoModal';modal.className='video-modal';
 const src=d.provider==='youtube'?`https://www.youtube.com/embed/${d.videoId}?playsinline=1&rel=0`:`https://player.vimeo.com/video/${d.videoId}?playsinline=1`;
 const external=d.provider==='youtube'?'https://www.youtube.com/watch?v='+d.videoId:d.source;
 modal.innerHTML=`<div class="video-dialog" role="dialog" aria-modal="true" aria-label="Gyakorlatbemutató"><button class="btn secondary" onclick="closeDemo()">✕ Bezárás</button><h2>${esc(byId(id)?.hu||'Bemutató')}</h2><div class="video-player"><iframe title="${esc(byId(id)?.hu)}" src="${src}" allow="autoplay; encrypted-media; fullscreen; picture-in-picture" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe></div><p class="small muted">Forrás: ${esc(d.credit)}. Ha a lejátszó nem indul, nyisd meg külső alkalmazásban.</p><button class="btn block" onclick="openVideoLink('${external}')">Megnyitás külső alkalmazásban</button><button class="btn secondary block" onclick="openVideoLink('${d.source}')">Forrás és útmutató</button></div>`;
 document.body.appendChild(modal);modal.querySelector('button').focus();
};
var openVideoLink = async function openVideoLink(url){persistDraft();try{if(isNative())await nativeFiles().openVideo({url});else window.open(url,'_blank','noopener,noreferrer');}catch(e){alert(e.message||'Nem sikerült megnyitni a videót.');}};
// Video mappings: original links plus v1.2.1 YouTube additions (2026-09-10).
// Online players; media is not downloaded or bundled into the APK.
// 2026-09-10: all built-in exercises use YouTube embeds. Device playback remains to be tested.
const DEMOS = {
 'goblet-squat':[null,'youtube','CkFzgR55gho','YouTube – Goblet Squat'],
 'bulgarian-split-squat':[null,'youtube','vLuhN_glFZ8','YouTube – Bulgarian Split Squat'],
 'db-pullover':[null,'youtube','a3cF2sS5v6I','YouTube – Dumbbell Pullover on Floor'],
 'lateral-raise':[null,'youtube','3VcKaXpzqRo','ScottHermanFitness'],
 'hammer-curl':[null,'youtube','zC3nLlEvin4','ScottHermanFitness'],
 'close-pushup':[null,'youtube','W3gBdsTzDrk','Team Evolve'],
 'leg-press':[null,'youtube','yZmx_Ac3880','Renaissance Periodization'],
 'machine-chest-press':[null,'youtube','NwzUje3z0qY','Renaissance Periodization'],
 'lat-pulldown':[null,'youtube','CAwf7n6Luuc','ScottHermanFitness'],
 'seated-cable-row':[null,'youtube','UCXxvVItLoM','Renaissance Periodization'],
 'leg-curl':[null,'youtube','ELOCsoDSmrg','ScottHermanFitness'],
 'machine-shoulder-press':[null,'youtube','WvLMauqrnK8','Renaissance Periodization'],
 'cable-triceps':[null,'youtube','_w-HpW70nSQ','ScottHermanFitness'],
 'cable-curl':[null,'youtube','0QjH28wli5Q','YouTube – Straight Bar Cable Curl'],

 'db-squat':[null,'youtube','v_c67Omje48','Mike Hildebrandt'],
 'db-floor-press':[null,'youtube','uUGDRwge4F8','ScottHermanFitness'],
 'one-arm-row':['one-arm-dumbbell-row.html','youtube','YZgVEy6cmaY'],
 'rdl':['romanian-deadlift','youtube','-m45n1_x32E'],
 'db-curl':[null,'youtube','sAq_ocpRh_I','ScottHermanFitness'],
 'plank':[null,'youtube','pSHjTRCQxIw','ScottHermanFitness'],
 'reverse-lunge':[null,'youtube','sjlsISvHyZs','ScottHermanFitness'],
 'db-ohp':[null,'youtube','Raemd3qWgJc','Renaissance Periodization'],
 'barbell-row':[null,'youtube','fpgfWbe7yhc'],
 'pushup':['push-up.html','youtube','KEFQyLkDYtI'],
 'oh-triceps':['two-arm-standing-dumbbell-extension.html','youtube','VjmgzEmODnI'],
 'crunch':['ab-crunch','youtube','Plh1CyiPE_Y']
};


document.addEventListener('keydown',e=>{if(e.key==='Escape')closeDemo();});


// @endsection demos.js

// @section cloud.js
var googleBridge = function googleBridge(){return window.Capacitor?.Plugins?.GoogleSync||window.Capacitor?.registerPlugin?.('GoogleSync');};
var scheduled = function scheduled(){return db.get('scheduled',[]);};
var cloudPrefs = function cloudPrefs(){return db.get('cloudPrefs',{drive:false,calendar:false});};
var cloudStatus = function cloudStatus(){const s=db.get('cloudStatus',{});return `<div id="cloudStatus" class="small muted">${esc(cloudMessage)}${s.drive?'<p>Utolsó Drive-szinkron: '+esc(fmtDate(s.drive))+'</p>':''}${s.calendar?'<p>Utolsó naptárszinkron: '+esc(fmtDate(s.calendar))+'</p>':''}</div>`;};
var cloudPanel = function cloudPanel(){const p=cloudPrefs();return `<div class="setting"><label>Google-fiók és szinkron</label><p class="small muted">${cloudProfile?esc(cloudProfile.name||cloudProfile.email)+' • '+esc(cloudProfile.email):'Kapcsold a Google-fiókodat a RepForge-profilhoz. A kiadás Google Cloud-beállítása szükséges.'}</p>${cloudProfile?`<button class="btn secondary block" onclick="disconnectGoogle()">Kijelentkezés</button><br><label><input type="checkbox" ${p.drive?'checked':''} onchange="setCloudOption('drive',this.checked)"> Automatikus Drive-szinkron</label><label><input type="checkbox" ${p.calendar?'checked':''} onchange="setCloudOption('calendar',this.checked)"> Automatikus edzésnaptár</label><p class="small muted">Az app megnyitásakor és adatváltozás után, internet mellett. Bezárt appban nincs háttérszinkron. A naptárkapcsolat iránya: RepForge → Google Naptár.</p><button class="btn block" onclick="syncCloud(false)">Drive-szinkron most</button><br><button class="btn secondary block" onclick="syncCalendar(false)">Naptárszinkron most</button>`:`<button class="btn block" onclick="connectGoogle()">Google-fiók kapcsolása</button>`}${cloudStatus()}</div>`;};
var showCloudMessage = function showCloudMessage(t){cloudMessage=t||'';const e=document.getElementById('cloudStatus');if(e)e.textContent=cloudMessage;};
var finishCloud = function finishCloud(){cloudBusy=false;if(cloudDirty){cloudDirty=false;cloudChanged();}};
var initCloud = async function initCloud(){if(!isNative()||!googleBridge()?.status)return;try{const r=await googleBridge().status();cloudProfile=r.profile?JSON.parse(r.profile):null;if(state.tab==='settings')render();cloudChanged();}catch(e){showCloudMessage(e.message);}};
var connectGoogle = async function connectGoogle(){if(cloudBusy)return;if(!isNative()){alert('A Google-kapcsolat Androidon érhető el.');return;}if(!confirm('A kiválasztott Google-fiókhoz kapcsolod a RepForge-ot? A szinkron későbbi bekapcsolása a telefon jelenlegi edzésadatait is ebbe a fiókba menti.'))return;cloudBusy=true;try{const r=await googleBridge().connect();cloudProfile=JSON.parse(r.profile);db.set('cloudPrefs',{drive:false,calendar:false});db.set('cloudStatus',{});render();alert('Google-profil kapcsolva. A Drive és a naptár engedélyét külön kérjük, a funkció bekapcsolásakor.');}catch(e){alert(e.message);}finally{finishCloud();}};
var disconnectGoogle = async function disconnectGoogle(){if(cloudBusy){alert('Várd meg a szinkron végét.');return;}if(!confirm('Kijelentkezel? A szinkron leáll, a helyi és a Google-ban tárolt adatok megmaradnak.'))return;try{await googleBridge().disconnect();cloudProfile=null;db.set('cloudPrefs',{drive:false,calendar:false});db.set('cloudStatus',{});cloudMessage='';render();}catch(e){alert(e.message);}};
var setCloudOption = async function setCloudOption(key,on){const p=cloudPrefs();p[key]=on;db.set('cloudPrefs',p);if(on){if(key==='drive')await syncCloud(false);else await syncCalendar(false);}};
var cloudChanged = function cloudChanged(){if(!cloudProfile||typeof setTimeout==='undefined')return;if(cloudBusy){cloudDirty=true;return;}clearTimeout(cloudTimer);cloudTimer=setTimeout(async()=>{if(cloudBusy){cloudDirty=true;return;}if(cloudPrefs().drive)await syncCloud(true);if(cloudPrefs().calendar)await syncCalendar(true);},1800);};
var canonical = function canonical(x){if(Array.isArray(x))return '['+x.map(canonical).join(',')+']';if(x&&typeof x==='object')return '{'+Object.keys(x).sort().map(k=>JSON.stringify(k)+':'+canonical(x[k])).join(',')+'}';return JSON.stringify(x);};
var canonicalSyncData = function canonicalSyncData(d){return canonical({history:d.history,weights:d.weights,settings:d.settings,exercises:d.exercises,plan:d.plan,scheduled:d.scheduled});};
var syncData = function syncData(){return {...makeBackup(),scheduled:scheduled()};};
var recordKey = function recordKey(kind,x){return kind==='history'?(x.id||x.workout+'|'+x.started):kind==='weights'?(x.id||x.date):x.id;};
var mergeSync = function mergeSync(local,remotes,base,choose){
 const out=JSON.parse(JSON.stringify(local));
 for(const kind of ['history','weights','scheduled']){
  const map=new Map((local[kind]||[]).map(x=>[recordKey(kind,x),x]));
  for(const remote of remotes)for(const x of remote[kind]||[]){const key=recordKey(kind,x),old=map.get(key);if(!old){map.set(key,x);continue;}if(canonical(old)===canonical(x))continue;
   if(kind==='scheduled'&&(old.updatedAt||0)!==(x.updatedAt||0)){map.set(key,(old.updatedAt||0)>(x.updatedAt||0)?old:x);continue;}
   if(kind==='history'&&typeof rf130MergeHistoryPhotos==='function'){const photoMerged=rf130MergeHistoryPhotos(old,x);if(photoMerged){map.set(key,photoMerged);continue;}}
   map.set(key,choose(kind,old,x));
  }out[kind]=[...map.values()].sort((a,b)=>String(b.started||b.date||b.start).localeCompare(String(a.started||a.date||a.start)));
 }
 for(const key of ['settings','exercises','plan']){
  let selected=local[key];for(const remote of remotes){const x=remote[key];if(canonical(selected)===canonical(x))continue;if(base&&canonical(x)===canonical(base[key]))continue;if(base&&canonical(selected)===canonical(base[key]))selected=x;else selected=choose(key,selected,x);}out[key]=selected;
 }return out;
};
var validateSync = function validateSync(d){validateBackup(d);if(!Array.isArray(d.scheduled)||d.scheduled.length>500)throw Error('Hibás tervezett edzések.');for(const p of d.scheduled)if(!p||typeof p.id!=='string'||!/^[a-z0-9-]{8,80}$/.test(p.id)||!['A','B'].includes(p.workout)||!Number.isFinite(Date.parse(p.start))||!Number.isFinite(Date.parse(p.end))||Date.parse(p.end)<=Date.parse(p.start)||typeof p.updatedAt!=='number'||!Number.isFinite(p.updatedAt)||typeof p.cancelled!=='boolean')throw Error('Hibás tervezett edzés.');return d;};
var storeMerged = function storeMerged(d){const keys=['history','weights','settings','exercises','plan','scheduled'],old=keys.map(k=>localStorage.getItem('repforge:'+k));try{for(const k of keys)localStorage.setItem('repforge:'+k,JSON.stringify(d[k]));}catch(e){for(const k of keys)localStorage.removeItem('repforge:'+k);keys.forEach((k,i)=>{if(old[i]!=null)localStorage.setItem('repforge:'+k,old[i]);});throw Error('Nincs elég hely a szinkronadatoknak.');}};
var syncCloud = async function syncCloud(silent=false){
 if(cloudBusy||!cloudProfile)return;if(state.session){showCloudMessage('Drive-szinkron az edzés befejezése után.');return;}if(navigator.onLine===false){showCloudMessage('Offline: a helyi adatok megvannak, a szinkron internetre vár.');return;}
 cloudBusy=true;showCloudMessage('Drive-szinkron…');
 try{
  const owner=cloudProfile.sub,bridge=googleBridge(),local=validateSync(syncData()),startState=canonicalSyncData(local),files=(await bridge.driveList({silent})).files||[];
  const latest=new Map();for(const f of files){const m=/^repforge-sync-([a-z0-9-]{36})-/.exec(f.name);if(!m)continue;const old=latest.get(m[1]);if(!old||String(f.createdTime)>String(old.createdTime))latest.set(m[1],f);}
  const remotes=[];for(const f of [...latest.values()].sort((a,b)=>String(a.createdTime).localeCompare(String(b.createdTime)))){const snap=JSON.parse((await bridge.driveRead({id:f.id,silent})).data);if(snap.app!=='RepForgeSync'||snap.schema!==1||snap.owner!==owner)throw Error('Ismeretlen vagy más profilhoz tartozó felhőmentés.');remotes.push(validateSync(snap.data));}
  const base=db.get('cloudBase:'+owner,null);let decision=null;
  const choose=(key,a,b)=>{if(silent)throw Error('Eltérő adatok vannak a felhőben. Indíts kézi szinkront az egyeztetéshez.');if(decision===null)decision=confirm('Eltérő adatok vannak a telefonon és a felhőben. Az ütköző értékeknél a TELEFON változata maradjon?\nOK: telefon • Mégse: felhő. Az egyedi edzések mindkét helyről megmaradnak.');return decision?a:b;};
  const merged=validateSync(mergeSync(local,remotes,base,choose));
  if(typeof rf130SyncWorkoutPhotos==='function')await rf130SyncWorkoutPhotos(merged,bridge,silent);
  if(state.session||canonicalSyncData(syncData())!==startState)throw Error('Közben változtak az adatok vagy edzés indult. Indítsd újra a szinkront.');
  let device=db.get('cloudDevice',null);if(!device){device=crypto.randomUUID();db.set('cloudDevice',device);}
  if(!base||canonicalSyncData(merged)!==canonicalSyncData(base)||!files.length){const r=await bridge.driveWrite({silent,data:JSON.stringify({app:'RepForgeSync',schema:1,owner,device,data:merged})});if(!r.verified)throw Error('A Drive-mentés ellenőrzése nem sikerült.');}
  if(state.session||canonicalSyncData(syncData())!==startState)throw Error('Közben változtak az adatok. A helyi változásokat a következő szinkron egyesíti.');
  storeMerged(merged);db.set('cloudBase:'+owner,merged);db.set('cloudStatus',{...db.get('cloudStatus',{}),drive:new Date().toISOString()});showCloudMessage('Drive-szinkron kész.');render();
 }catch(e){showCloudMessage(e.message);if(!silent)alert(cloudMessage);}finally{finishCloud();}
};
var eventId = async function eventId(key){const hash=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(key));return 'rf'+Array.from(new Uint8Array(hash),x=>x.toString(16).padStart(2,'0')).join('');};
var workoutDescription = function workoutDescription(h){return 'RepForge – teljesített edzés\n'+h.exercises.map(e=>e.hu+': '+e.sets.filter(s=>s.done).map(s=>formatSet(e,s)).join('; ')).join('\n');};
var calendarEvents = async function calendarEvents(){const events=new Map();for(const p of scheduled()){const id=await eventId('plan:'+p.id);events.set(id,p.cancelled?{id,cancelled:true}:{id,summary:'RepForge – Full Body '+p.workout,description:'Tervezett edzés',start:{dateTime:p.start},end:{dateTime:p.end},reminders:{useDefault:false,overrides:[{method:'popup',minutes:30}]}});}for(const h of history()){if(!Number.isFinite(Date.parse(h.finished))||!Number.isFinite(Date.parse(h.started))||Date.parse(h.finished)<=Date.parse(h.started))continue;const id=await eventId(h.scheduleId?'plan:'+h.scheduleId:'workout:'+recordKey('history',h));events.set(id,{id,summary:'✓ RepForge – Full Body '+h.workout,description:workoutDescription(h),start:{dateTime:h.started},end:{dateTime:h.finished},reminders:{useDefault:false}});}return [...events.values()];};
var syncCalendar = async function syncCalendar(silent=false){if(cloudBusy||!cloudProfile)return;if(navigator.onLine===false){showCloudMessage('Naptárszinkron internetre vár.');return;}cloudBusy=true;try{const events=await calendarEvents();if(!events.length){showCloudMessage('Még nincs naptárba küldhető edzés.');return;}for(let i=0;i<events.length;i+=100)await googleBridge().calendarSync({silent,events:JSON.stringify(events.slice(i,i+100))});db.set('cloudStatus',{...db.get('cloudStatus',{}),calendar:new Date().toISOString()});showCloudMessage('Naptárszinkron kész.');}catch(e){showCloudMessage(e.message);if(!silent)alert(e.message);}finally{finishCloud();}};
var calendarIntent = async function calendarIntent(index,planned=false){try{const x=planned?scheduled()[index]:history()[index];if(!x)throw Error('Nincs ilyen edzés.');const start=Date.parse(planned?x.start:x.started),end=Date.parse(planned?x.end:x.finished);if(!Number.isFinite(start)||!Number.isFinite(end)||end<=start)throw Error('Ehhez az edzéshez nincs érvényes kezdési/befejezési idő.');if(!isNative())throw Error('A naptárgomb Androidon használható.');await nativeFiles().addCalendarEvent({title:'RepForge – Full Body '+x.workout,start,end,description:planned?'Tervezett edzés':workoutDescription(x)});alert('A naptár ablaka megnyílt. Válaszd ki a naptárat, és ott mentsd el. A kézi bejegyzés nem kapcsolódik az automatikus szinkronhoz.');}catch(e){alert(e.message);}};
var plannerPanel = function plannerPanel(){return `<div class="setting"><label>Edzések tervezése</label><p class="small muted">Válassz kezdőnapot, időpontot és heti napokat. Az alkalmak A/B sorrendben váltakoznak.</p><label>Kezdőnap<input class="field" type="date" id="planDate"></label><label>Időpont<input class="field" type="time" id="planTime"></label><label>Időtartam (perc)<input class="field" type="number" id="planMinutes" min="10" max="240" value="45"></label><label>Hetek száma<input class="field" type="number" id="planWeeks" min="1" max="12" value="4"></label><label>Első edzés<select class="field" id="planFirst"><option>A</option><option>B</option></select></label><p>${['V','H','K','Sze','Cs','P','Szo'].map((d,i)=>`<label style="display:inline-block;margin:6px"><input type="checkbox" name="planDay" value="${i}">${d}</label>`).join('')}</p><button class="btn block" onclick="createSchedule()">Edzések megtervezése</button><br>${scheduled().map((p,i)=>p.cancelled?'':`<div class="card"><strong>Full Body ${p.workout}</strong><p>${esc(fmtDate(p.start))}${history().some(h=>h.scheduleId===p.id)?' • Teljesítve':''}</p><button class="btn secondary" onclick="calendarIntent(${i},true)">Naptárba</button> <button class="btn secondary" onclick="cancelSchedule(${i})">Törlés</button>${!history().some(h=>h.scheduleId===p.id)?` <button class="btn" onclick="startWorkout('${p.workout}','${p.id}')">Indítás</button>`:''}</div>`).join('')}</div>`;};
var buildSchedule = function buildSchedule(date,time,days,weeks,minutes,first){
 if(!/^\d{4}-\d{2}-\d{2}$/.test(date)||!/^\d{2}:\d{2}$/.test(time)||!days.length||days.some(x=>!Number.isInteger(x)||x<0||x>6)||!Number.isInteger(weeks)||weeks<1||weeks>12||!Number.isInteger(minutes)||minutes<10||minutes>240||!['A','B'].includes(first))throw Error('Adj meg érvényes napot, időpontot, heti napokat és időtartamot.');
 const start=new Date(date+'T'+time);if(!Number.isFinite(start.getTime()))throw Error('Érvénytelen dátum.');const result=[];let w=first;
 for(let n=0;n<weeks*7;n++){const d=new Date(start);d.setDate(start.getDate()+n);if(!days.includes(d.getDay()))continue;result.push({id:crypto.randomUUID(),workout:w,start:d.toISOString(),end:new Date(d.getTime()+minutes*60000).toISOString(),updatedAt:Date.now(),cancelled:false});w=w==='A'?'B':'A';}return result;
};
var createSchedule = function createSchedule(){try{const next=buildSchedule($('#planDate').value,$('#planTime').value,[...document.querySelectorAll('input[name=planDay]:checked')].map(e=>Number(e.value)),Number($('#planWeeks').value),Number($('#planMinutes').value),$('#planFirst').value),current=scheduled(),unique=next.filter(x=>!current.some(p=>!p.cancelled&&p.start===x.start));if(current.length+unique.length>500)throw Error('Maximum 500 tervezett alkalom tárolható.');if(!unique.length){alert('Ezek az időpontok már szerepelnek.');return;}db.set('scheduled',[...current,...unique]);render();alert(unique.length+' edzés megtervezve. A naptárhoz használd a Naptárba gombot, vagy az automatikus naptárszinkront.');}catch(e){alert(e.message);}};
var cancelSchedule = function cancelSchedule(i){const s=scheduled();if(!s[i]||!confirm('Törlöd ezt a tervezett alkalmat? A teljesített edzésnapló megmarad.'))return;s[i]={...s[i],cancelled:true,updatedAt:Date.now()};db.set('scheduled',s);render();};
// Native Google authorization: access tokens never enter JavaScript or backups.
let cloudProfile=null,cloudBusy=false,cloudMessage='',cloudTimer=null,cloudDirty=false;


document.addEventListener('visibilitychange',()=>{if(!document.hidden)cloudChanged();});


// @endsection cloud.js

// @section app.js
var migrateLunge114 = function migrateLunge114(){
 if(db.get('lunge114',false))return;
 const saved=db.get('exercises',null);
 if(saved)db.set('exercises',saved.map(e=>e.id==='reverse-lunge'?{...e,en:'Dumbbell Reverse Lunge',equipment:'2 kézisúlyzó',loadType:'per_hand',unit:'kg/kar',weight:e.weight>0?e.weight:5,notes:DEFAULT_EXERCISES.find(x=>x.id===e.id).notes}:e));
 db.set('lunge114',true);
};
var settings = function settings(){return db.get("settings",{rest:90})};
var exerciseMeta = function exerciseMeta(e){
 const loadType=e.loadType || ({"db-squat":"per_hand","db-floor-press":"per_hand","db-curl":"per_hand","db-ohp":"per_hand","one-arm-row":"single_dumbbell","oh-triceps":"single_dumbbell","rdl":"total","barbell-row":"total"}[e.id]||"bodyweight");
 const repUnit=e.repUnit || ({"one-arm-row":"/kar","reverse-lunge":"/láb","plank":"mp"}[e.id]||"ism.");
 return {...e,loadType,repUnit,unit:loadLabel(loadType)};
};
var loadLabel = function loadLabel(t){return ({per_hand:"kg/kar",total:"kg összesen",single_dumbbell:"kg (1 súlyzó)",bodyweight:"testsúly"})[t]||"kg"};
var exercises = function exercises(){return db.get("exercises",DEFAULT_EXERCISES).map(exerciseMeta)};
var plan = function plan(){return db.get("plan",DEFAULT_PLAN)};
function history(){return db.get("history",[])}
var weights = function weights(){return db.get("weights",[])};
var fmtDate = function fmtDate(s){return new Date(s).toLocaleString("hu-HU",{year:"numeric",month:"2-digit",day:"2-digit",hour:"2-digit",minute:"2-digit"})};
var shell = function shell(content){
return `<div>
  <div class="top"><div class="brand">REPFORGE</div><div class="tag">offline edzésnapló • reklám nélkül</div>
  <div class="tabs">
  ${tab("home","Kezdőlap")}${tab("plan","Terv")}${tab("history","Napló")}${tab("weight","Testsúly")}${tab("settings","Beállítás")}
  </div></div>${content}${state.session&&state.timer>0?timerHtml():""}</div>`};
var tab = function tab(id,label){return `<button class="tab ${state.tab===id?"active":""}" onclick="go('${id}')">${label}</button>`};
var go = function go(t){if(state.session && !confirm("Kilépsz a folyamatban lévő edzésből? A még el nem mentett edzés elvész."))return;if(state.session)db.set("draft",null);stopTimer();state.tab=t;state.session=null;state.workout=null;render()};
var nextWorkout = function nextWorkout(){const h=history();return !h.length?"A":h[0].workout==="A"?"B":"A"};
var home = function home(){
 const h=history(), w=weights(), nw=nextWorkout();
 const latestW=w.length?w[0].kg:"—";const draft=db.get("draft",null);
 return shell(`<main>
  ${draft?`<div class="card"><strong>Félbehagyott edzés</strong><p class="small muted">${esc(draft.session?.workout||"")} • folytathatod a mentett sorozatokkal.</p><button class="btn block" onclick="resumeDraft()">Edzés folytatása</button></div><br>`:""}
  <div class="hero"><span class="badge">Következő</span><h1>FULL BODY ${nw}</h1><div class="muted">${plan()[nw].length} gyakorlat • heti 3 alkalom • saját A/B terv</div><br><button class="btn block" onclick="startWorkout('${nw}')">Edzés indítása</button></div>
  <div class="grid2"><div class="stat"><small>Elvégzett edzések</small><strong>${h.length}</strong></div><div class="stat"><small>Legutóbbi testsúly</small><strong>${latestW}${latestW==="—"?"":" kg"}</strong></div></div>
  <div class="section">Gyors választás</div><div class="grid2"><button class="btn secondary" onclick="showWorkout('A')">Full Body A</button><button class="btn secondary" onclick="showWorkout('B')">Full Body B</button></div>
 </main>`)};
var exCard = function exCard(e,i,w){return `<div class="exercise" onclick="showExercise('${w}',${i})"><div class="num">${i+1}</div><div><div class="ex-name">${esc(e.hu)}</div><div class="en">${esc(e.en)}</div><div class="meta">${e.sets} × ${esc(e.reps)} • ${e.weight?e.weight+" "+esc(e.unit):esc(e.unit)}</div></div><div class="chev">›</div></div>`};
var planScreen = function planScreen(){
 const p=plan();
 return shell(`<main><div class="hero"><h1>A/B edzésterv</h1><div class="muted">1. hét: A • B • A &nbsp; | &nbsp; 2. hét: B • A • B</div></div>
 ${["A","B"].map(w=>`<div class="section">FULL BODY ${w}</div>${p[w].map((id,i)=>exCard(byId(id),i,w)).join("")}<button class="btn secondary block" onclick="editPlan('${w}')">Terv szerkesztése</button>`).join("")}</main>`)};
var showWorkout = function showWorkout(w){state.workout=w;state.tab="plan";render(shell(`<main><div class="hero"><h1>FULL BODY ${w}</h1><div class="muted">${plan()[w].length} gyakorlat</div></div>${plan()[w].map((id,i)=>exCard(byId(id),i,w)).join("")}<button class="btn block" onclick="startWorkout('${w}')">Edzés indítása</button></main>`))};
var showExercise = function showExercise(w,i){const e=byId(plan()[w][i]);state.tab="plan";render(shell(`<main><button class="btn secondary" onclick="showWorkout('${w}')">← Vissza</button><br><br><div class="card detail"><h2>${esc(e.hu)}</h2><div class="en">${esc(e.en)}</div><div class="meta">${e.sets} × ${esc(e.reps)} • ${e.weight?e.weight+" "+esc(e.unit):esc(e.unit)}</div><br><span class="badge">${esc(e.target)}</span><span class="badge">${esc(e.equipment)}</span>${motionDemo(e.id)}<p class="note">${esc(e.notes)}</p></div></main>`))};
var editPlan = function editPlan(w){
 const p=plan(), exs=exercises();
 render(shell(`<main><button class="btn secondary" onclick="go('plan')">← Vissza</button><div class="section">Full Body ${w} szerkesztése</div>
 ${p[w].map((id,i)=>{const e=byId(id);return `<div class="setting">
   <label>${i+1}. gyakorlat</label>
   <select class="field" onchange="changePlan('${w}',${i},this.value)">${exs.map(x=>`<option value="${x.id}" ${x.id===id?"selected":""}>${esc(x.hu)} — ${esc(x.en)}</option>`).join("")}</select>
   <div style="height:10px"></div>
   <label>Célismétlés / időtartam</label>
   <input class="field" value="${esc(e.reps)}" onchange="changeExerciseValue('${id}','reps',this.value)">
   <label>Terhelés értelmezése</label>
   <select class="field" onchange="changeExerciseValue('${id}','loadType',this.value);editPlan('${w}')">${['per_hand','total','single_dumbbell','bodyweight'].map(t=>`<option value="${t}" ${e.loadType===t?'selected':''}>${loadLabel(t)}</option>`).join('')}</select>
   ${e.loadType!=='bodyweight'?`<label>Induló súly (${loadLabel(e.loadType)})</label><input class="field" inputmode="decimal" value="${e.weight}" onchange="changeExerciseValue('${id}','weight',this.value)">`:''}
   <label>Sorozatok száma</label>
   <select class="field" onchange="changeExerciseSets('${id}',this.value)">
     ${Array.from({length:10},(_,n)=>n+1).map(n=>`<option value="${n}" ${Number(e.sets)===n?"selected":""}>${n} sorozat</option>`).join("")}
   </select>
   <div class="small muted" style="margin-top:7px">Az edzés indításakor pontosan ennyi sorozat jelenik meg ennél a gyakorlatnál.</div>
 </div>`}).join("")}
 </main>`))};
var changePlan = function changePlan(w,i,id){const p=plan();p[w][i]=id;db.set("plan",p);editPlan(w)};
var changeExerciseValue = function changeExerciseValue(id,key,value){
 const exs=exercises(),e=exs.find(x=>x.id===id);if(!e)return;
 e[key]=key==='weight'?Math.max(0,Number(String(value).replace(',','.'))||0):value;
 db.set('exercises',exs);
};
var changeExerciseSets = function changeExerciseSets(id,value){
 const exs=exercises();
 const e=exs.find(x=>x.id===id);
 if(!e)return;
 e.sets=Math.max(1,Math.min(10,Number(value)||2));
 db.set("exercises",exs);
};
var startWorkout = function startWorkout(w,scheduleId=null){
 if(db.get("draft",null)&&!confirm("Új edzést indítasz? A félbehagyott edzés helyére ez kerül."))return;
 state.workout=w;state.current=0;
 const p=plan();
 state.session={workout:w,...(scheduleId?{scheduleId}:{}),started:new Date().toISOString(),exercises:p[w].map(id=>{const e=byId(id);const prev=findLastExercise(id);return {id,hu:e.hu,en:e.en,loadType:e.loadType,repUnit:e.repUnit,sets:Array.from({length:e.sets},(_,i)=>({set:i+1,weight:prev?.loadType===e.loadType?(prev?.sets?.[i]?.weight??e.weight):e.weight,reps:"",done:false}))}})};
 renderWorkout()
};
var findLastExercise = function findLastExercise(id){for(const h of history()){const x=h.exercises.find(e=>e.id===id);if(x)return x}return null};
var renderWorkout = function renderWorkout(){
 persistDraft();
 const i=state.current,p=state.session.exercises.map(x=>x.id),e=byId(p[i]),se=state.session.exercises[i],pct=Math.round((i/p.length)*100);
 render(shell(`<main><div class="hero"><span class="badge">FULL BODY ${state.workout}</span><h1>${i+1}/${p.length}</h1><div class="muted">${esc(e.hu)}</div><div class="progress"><div style="width:${pct}%"></div></div></div>
 <div class="card detail"><h2>${esc(e.hu)}</h2><div class="en">${esc(e.en)}</div><div class="meta">Cél: ${e.sets} × ${esc(e.reps)}</div>${motionDemo(e.id)}<p class="note">${esc(e.notes)}</p></div>
 <div class="section">Sorozatok</div>${se.sets.map((s,si)=>`<div class="row"><div class="num">${si+1}</div>${e.loadType==='bodyweight'?'<span class="small muted">Testsúly</span>':`<label class="small">${loadLabel(e.loadType)}<input class="field" aria-label="${loadLabel(e.loadType)}" inputmode="decimal" value="${s.weight}" oninput="upd(${i},${si},'weight',this.value)"></label>`}<label class="small">${e.repUnit}<input class="field" inputmode="numeric" aria-label="${e.repUnit}" placeholder="${esc(e.reps)}" value="${esc(s.reps)}" oninput="upd(${i},${si},'reps',this.value)"></label><button class="check ${s.done?"done":""}" onclick="toggleSet(${i},${si})">${s.done?"✓":"OK"}</button></div>`).join("")}
 <br><button class="btn block" onclick="nextExercise()">${i===p.length-1?"Edzés befejezése":"Következő gyakorlat"}</button><br><br><button class="btn secondary block" onclick="prevExercise()" ${i===0?"disabled":""}>Előző</button></main>`))};
var upd = function upd(ei,si,k,v){state.session.exercises[ei].sets[si][k]=k==="weight"?Math.max(0,(Number(String(v).replace(",","."))||0)):v;persistDraft()};
var toggleSet = function toggleSet(ei,si){const s=state.session.exercises[ei].sets[si];if(!s.done && (!/^\d+$/.test(String(s.reps)) || Number(s.reps)<1)){alert("Írd be a tényleges ismétlésszámot vagy másodpercet.");return;}s.done=!s.done;if(s.done)startRest();renderWorkout()};
var prevExercise = function prevExercise(){if(state.current>0){state.current--;renderWorkout();window.scrollTo(0,0)}};
var nextExercise = function nextExercise(){if(state.current<state.session.exercises.length-1){state.current++;renderWorkout();window.scrollTo(0,0)}else finishWorkout()};
var finishWorkout = function finishWorkout(){if(!state.session.exercises.some(e=>e.sets.some(s=>s.done))){alert("Előbb jelölj legalább egy sorozatot teljesítettnek.");return;}if(state.session.exercises.some(e=>e.sets.some(s=>!s.done))&&!confirm("Vannak be nem fejezett sorozatok. Elmented a részleges edzést?"))return;const h=history();state.session.finished=new Date().toISOString();h.unshift(state.session);db.set("history",h);db.set("draft",null);const w=state.workout;stopTimer();state.session=null;state.workout=null;state.tab="home";render(shell(`<main><div class="hero"><h1>Kész ✓</h1><div class="muted">Full Body ${w} elmentve.</div><br><button class="btn block" onclick="go('home')">Kezdőlap</button></div></main>`))};
var formatSet = function formatSet(e,s){
 // Historical weights are deliberately not reinterpreted: old entries lacked metadata.
 if(!e.loadType)return `${s.weight||0} kg × ${s.reps||'—'} (régi súlyjelölés)`;
 const rep=e.repUnit==='mp'?`${s.reps||'—'} mp`:`${s.reps||'—'}${e.repUnit==='ism.'?' ism.':e.repUnit}`;
 return e.loadType==='bodyweight'?rep:`${s.weight||0} ${loadLabel(e.loadType)} × ${rep}`;
};
var historyScreen = function historyScreen(){
 const h=history();
 return shell(`<main>
   <div class="hero"><h1>Edzésnapló</h1><div class="muted">Az edzéseid részletesen, de átláthatóbban.</div></div>
   ${h.length ? h.map((x,hi)=>{
     const c=completedSets(x);
     return `<div class="history">
       <div class="history-head">
         <div class="history-title-row">
           <div><div class="history-title">Full Body ${x.workout}</div><div class="history-date">${fmtDate(x.started)}</div></div>
           <span class="badge">${x.exercises.length} gyakorlat</span>
         </div>
         <div class="history-badges">
           <span class="history-badge">⏱ ${sessionDuration(x)}</span>
           <span class="history-badge">✓ ${c.done}/${c.total} sorozat</span>
         </div>
       </div>
       <div class="history-body"><button class="btn secondary" onclick="calendarIntent(${hi})">Hozzáadás a naptárhoz</button>
         ${x.exercises.map(e=>`<div class="history-ex">
           <div class="history-ex-name">${esc(e.hu)}</div>
           <div class="en">${esc(e.en||"")}</div>
           <div class="history-setchips">
             ${(e.sets||[]).map(s=>`<span class="setchip">${esc(formatSet(e,s))}${s.done?" ✓":" • nincs kész"}</span>`).join("")}
           </div>
         </div>`).join("")}
       </div>
     </div>`;
   }).join("") : `<div class="muted">Még nincs elmentett edzés.</div>`}
 </main>`)
};
var weightScreen = function weightScreen(){const w=weights();return shell(`<main><div class="hero"><h1>Testsúly</h1><div class="muted">Egyszerű tömegnövelési napló.</div></div>
 <div class="setting"><label>Mai testsúly (kg)</label><input id="kg" class="field" type="number" step="0.1" min="30" max="250" placeholder="pl. 62.4"><br><br><button class="btn block" onclick="addWeight()">Mentés</button></div>
 <div class="section">Előzmények</div>${w.length?w.map(x=>`<div class="weight-row"><strong>${x.kg} kg</strong><div class="small muted">${fmtDate(x.date)}</div></div>`).join(""):`<div class="muted">Még nincs adat.</div>`}</main>`)};
var addWeight = function addWeight(){const kg=Number(String($("#kg").value).replace(",","."));if(!kg||kg<30||kg>250){alert("Adj meg érvényes testsúlyt.");return}const w=weights();w.unshift({kg,date:new Date().toISOString()});db.set("weights",w);render()};
var settingsScreen = function settingsScreen(){const s=settings();return shell(`<main><div class="hero"><h1>Beállítások</h1><div class="muted">Minden adat helyben tárolódik.</div></div>
 <div class="setting"><label>Pihenőidő (másodperc)</label><input id="rest" class="field" type="number" min="30" max="300" value="${s.rest}"><br><br><button class="btn block" onclick="saveSettings()">Mentés</button></div>
 <div class="setting"><label>Biztonsági mentés</label><p class="small muted">Válaszd ki, hová kerüljön a JSON-fájl. Androidon az app írás után visszaolvassa és ellenőrzi.</p><p class="small muted">${esc(backupStatus())}</p><div class="grid2"><button class="btn secondary" onclick="exportData()">Mentés fájlba</button><button class="btn secondary" onclick="chooseImport()">Visszatöltés</button></div><input id="importFile" type="file" accept=".json,application/json" hidden onchange="importData(this.files[0])"></div>
 ${cloudPanel()}${plannerPanel()}<div class="setting"><label>RepForge 1.1.5 • Edzésterv</label><p class="small muted">Excel-alapterv: 2–3 sorozat, plankkel. Kímélő indulás: a későbbi HU/EN Excel 2 sorozatos terve. A választás felülírja a saját tervedet.</p><button class="btn secondary block" onclick="resetStarter()">Kímélő kezdőterv (2 sorozat)</button><br><button class="btn danger block" onclick="resetPlan()">Terv visszaállítása</button></div>
 </main>`)};
var saveSettings = function saveSettings(){const rest=Math.max(30,Math.min(300,Number($("#rest").value)||90));db.set("settings",{rest});alert("Mentve.")};
var resetStarter = function resetStarter(){if(confirm("A saját terv helyére a 2 sorozatos kezdőterv kerül. Folytatod?")){db.set("exercises",STARTER_EXERCISES);db.set("plan",{...DEFAULT_PLAN,A:DEFAULT_PLAN.A.filter(id=>id!=="plank")});render()}};
var resetPlan = function resetPlan(){if(confirm("Visszaállítod az alapértelmezett A/B tervet?")){db.set("plan",DEFAULT_PLAN);db.set("exercises",DEFAULT_EXERCISES);render()}};
var tickRest = function tickRest(){
 if(!state.restEndAt)return;
 state.timer=Math.max(0,Math.ceil((state.restEndAt-Date.now())/1000));
 const e=$("#tv");if(e)e.textContent=tstr(state.timer);
 if(state.timer===0){stopTimer();document.querySelector(".timer")?.remove();if(navigator.vibrate)navigator.vibrate([200,100,200]);}
};
var startRest = function startRest(){stopTimer();state.timer=settings().rest;state.restEndAt=Date.now()+state.timer*1000;state.timerId=setInterval(tickRest,250)};
var stopTimer = function stopTimer(){if(state.timerId)clearInterval(state.timerId);state.timerId=null;state.restEndAt=null;state.timer=0;};
var skipTimer = function skipTimer(){stopTimer();renderWorkout()};
var tstr = function tstr(s){return `${Math.floor(s/60)}:${String(s%60).padStart(2,"0")}`};
var timerHtml = function timerHtml(){return `<div class="timer"><strong id="tv">${tstr(state.timer)}</strong><div class="muted" style="flex:1">Pihenő</div><button class="btn secondary" onclick="skipTimer()">Kihagyás</button></div>`};
var render = function render(custom){
 if(window.TrainPilotBoot?.loading)return;
 closeDemo();
 const root=$("#app"); if(custom){root.innerHTML=custom;return}
 if(state.tab==="home")root.innerHTML=home();
 else if(state.tab==="plan")root.innerHTML=planScreen();
 else if(state.tab==="history")root.innerHTML=historyScreen();
 else if(state.tab==="weight")root.innerHTML=weightScreen();
 else root.innerHTML=settingsScreen();
};
var motionDemo = function motionDemo(id){return demoCard(id)};
var sessionDuration = function sessionDuration(x){
  if(!x.started || !x.finished) return "—";
  const sec=Math.max(0,Math.round((new Date(x.finished)-new Date(x.started))/1000));
  const min=Math.floor(sec/60), rem=sec%60;
  return min ? `${min} p ${rem ? rem+" mp" : ""}` : `${rem} mp`;
};
var completedSets = function completedSets(x){
  let done=0,total=0;
  for(const e of x.exercises||[]) for(const s of e.sets||[]){total++; if(s.done)done++;}
  return {done,total};
};

const DEFAULT_EXERCISES = [
{id:"db-squat",hu:"Guggolás 2 kézisúlyzóval",en:"Dumbbell Squat",equipment:"2 kézisúlyzó",target:"Láb",notes:"Egy-egy súlyzó lógjon a tested mellett. Láb vállszélesség körül, csípő hátra és lefelé, majd állj fel. Törzs feszes.",sets:3,reps:"8–12",weight:5,unit:"kg/kez"},
{id:"db-floor-press",hu:"Földön fekve nyomás",en:"Dumbbell Floor Press",equipment:"2 kézisúlyzó",target:"Mell",notes:"Hanyatt fekve engedd a könyököt kontrolláltan a padlóig, majd nyomd vissza a súlyokat.",sets:3,reps:"8–12",weight:5,unit:"kg/kez"},
{id:"one-arm-row",hu:"Egykezes evezés",en:"One-Arm Dumbbell Row",equipment:"1 kézisúlyzó + támasz",target:"Hát",notes:"Stabil támasz mellett húzd a súlyt a csípő/bordák felé. Ne csavard a törzsed.",sets:3,reps:"8–12 / kar",weight:5,unit:"kg"},
{id:"rdl",hu:"Román felhúzás – kétkezes rúddal",en:"Barbell Romanian Deadlift (RDL)",equipment:"kétkezes rúd",target:"Combhajlító/far",notes:"Térd enyhén hajlítva, hát semleges, csípő hátra. Csak addig menj le, amíg kontrollált.",sets:3,reps:"8–12",weight:10,unit:"kg összesen"},
{id:"db-curl",hu:"Bicepszezés",en:"Dumbbell Biceps Curl",equipment:"2 kézisúlyzó",target:"Bicepsz",notes:"Könyök a test mellett, lendítés nélkül emeld, majd kontrolláltan engedd vissza.",sets:2,reps:"10–15",weight:5,unit:"kg/kez"},
{id:"reverse-lunge",hu:"Hátralépős kitörés",en:"Dumbbell Reverse Lunge",equipment:"2 kézisúlyzó",loadType:"per_hand",target:"Láb",notes:"Tarts egy-egy kézisúlyzót a tested mellett. Lépj hátra, engedd a hátsó térdet lefelé, majd az első lábbal told vissza magad. A súlyt karonként, az ismétlést lábanként add meg.",sets:3,reps:"8–12 / láb",weight:5,unit:"testsúly"},
{id:"db-ohp",hu:"Fej fölé nyomás",en:"Dumbbell Overhead Press",equipment:"2 kézisúlyzó",target:"Váll",notes:"Vállmagasságból nyomd a súlyokat a fej fölé, majd lassan engedd vissza.",sets:3,reps:"8–12",weight:5,unit:"kg/kez max."},
{id:"barbell-row",hu:"Döntött törzsű evezés – kétkezes rúddal",en:"Barbell Bent-Over Row",equipment:"kétkezes rúd",target:"Hát",notes:"Feszes törzs, semleges hát, húzd a rudat a has/bordák alsó része felé.",sets:3,reps:"8–12",weight:10,unit:"kg összesen"},
{id:"pushup",hu:"Fekvőtámasz",en:"Push-Up",equipment:"testsúly",target:"Mell/tricepsz",notes:"Test egyenes. Állj meg, amikor még kb. 2–3 szabályos ismétlés maradna.",sets:3,reps:"közel maximum",weight:0,unit:"testsúly"},
{id:"oh-triceps",hu:"Tricepsz fej fölött",en:"Dumbbell Overhead Triceps Extension",equipment:"1 kézisúlyzó",target:"Tricepsz",notes:"Egy könnyű súlyzót fogj két kézzel, engedd a fej mögé, majd nyújtsd vissza.",sets:2,reps:"10–15",weight:4,unit:"kg körül"},
{id:"crunch",hu:"Hasprés",en:"Crunch",equipment:"testsúly",target:"Has",notes:"Hanyatt fekve emeld el a lapockát a földtől. Nem kell teljesen felülni.",sets:3,reps:"15–20",weight:0,unit:"testsúly"}
,{id:"plank",hu:"Plank (alkartámasz)",en:"Forearm Plank",equipment:"testsúly",target:"Törzs",notes:"Alkar és lábujjak a talajon. Has és farizom feszes, a test egyenes; ne essen be a derekad.",sets:3,reps:"30–60 mp",weight:0,unit:"testsúly"}
];

const STARTER_EXERCISES = [
{id:"db-squat",hu:"Guggolás 2 kézisúlyzóval",en:"Dumbbell Squat",equipment:"2 kézisúlyzó",target:"Láb",notes:"Egy-egy súlyzó lógjon a tested mellett. Láb vállszélesség körül, csípő hátra és lefelé, majd állj fel. Törzs feszes.",sets:2,reps:"8–10",weight:5,unit:"kg/kez"},
{id:"db-floor-press",hu:"Földön fekve nyomás",en:"Dumbbell Floor Press",equipment:"2 kézisúlyzó",target:"Mell",notes:"Hanyatt fekve engedd a könyököt kontrolláltan a padlóig, majd nyomd vissza a súlyokat.",sets:2,reps:"8–10",weight:5,unit:"kg/kez"},
{id:"one-arm-row",hu:"Egykezes evezés",en:"One-Arm Dumbbell Row",equipment:"1 kézisúlyzó + támasz",target:"Hát",notes:"Stabil támasz mellett húzd a súlyt a csípő/bordák felé. Ne csavard a törzsed.",sets:2,reps:"8–10/kar",weight:5,unit:"kg"},
{id:"rdl",hu:"Román felhúzás – kétkezes rúddal",en:"Barbell Romanian Deadlift (RDL)",equipment:"kétkezes rúd",target:"Combhajlító/far",notes:"Térd enyhén hajlítva, hát semleges, csípő hátra. Csak addig menj le, amíg kontrollált.",sets:2,reps:"8–10",weight:10,unit:"kg összesen"},
{id:"db-curl",hu:"Bicepszezés",en:"Dumbbell Biceps Curl",equipment:"2 kézisúlyzó",target:"Bicepsz",notes:"Könyök a test mellett, lendítés nélkül emeld, majd kontrolláltan engedd vissza.",sets:2,reps:"8–10",weight:5,unit:"kg/kez"},
{id:"reverse-lunge",hu:"Hátralépős kitörés",en:"Dumbbell Reverse Lunge",equipment:"2 kézisúlyzó",loadType:"per_hand",target:"Láb",notes:"Tarts egy-egy kézisúlyzót a tested mellett. Lépj hátra, engedd a hátsó térdet lefelé, majd az első lábbal told vissza magad. A súlyt karonként, az ismétlést lábanként add meg.",sets:2,reps:"8/láb",weight:5,unit:"testsúly"},
{id:"db-ohp",hu:"Fej fölé nyomás",en:"Dumbbell Overhead Press",equipment:"2 kézisúlyzó",target:"Váll",notes:"Vállmagasságból nyomd a súlyokat a fej fölé, majd lassan engedd vissza.",sets:2,reps:"8",weight:5,unit:"kg/kez max."},
{id:"barbell-row",hu:"Döntött törzsű evezés – kétkezes rúddal",en:"Barbell Bent-Over Row",equipment:"kétkezes rúd",target:"Hát",notes:"Feszes törzs, semleges hát, húzd a rudat a has/bordák alsó része felé.",sets:2,reps:"8–10",weight:10,unit:"kg összesen"},
{id:"pushup",hu:"Fekvőtámasz",en:"Push-Up",equipment:"testsúly",target:"Mell/tricepsz",notes:"Test egyenes. Állj meg, amikor még kb. 2–3 szabályos ismétlés maradna.",sets:2,reps:"2–3 RIR",weight:0,unit:"testsúly"},
{id:"oh-triceps",hu:"Tricepsz fej fölött",en:"Dumbbell Overhead Triceps Extension",equipment:"1 kézisúlyzó",target:"Tricepsz",notes:"Egy könnyű súlyzót fogj két kézzel, engedd a fej mögé, majd nyújtsd vissza.",sets:2,reps:"8–10",weight:4,unit:"kg körül"},
{id:"crunch",hu:"Hasprés",en:"Crunch",equipment:"testsúly",target:"Has",notes:"Hanyatt fekve emeld el a lapockát a földtől. Nem kell teljesen felülni.",sets:2,reps:"10–15",weight:0,unit:"testsúly"}
];


const DEFAULT_PLAN = {A:["db-squat","db-floor-press","one-arm-row","rdl","db-curl","plank"],B:["reverse-lunge","db-ohp","barbell-row","pushup","oh-triceps","crunch"]};
const state={tab:"home",session:null,workout:null,current:0,timer:0,timerId:null};

const db={
 get(k,f){try{return JSON.parse(localStorage.getItem("repforge:"+k))??f}catch{return f}},
 set(k,v){localStorage.setItem("repforge:"+k,JSON.stringify(v));if(["history","weights","settings","plan","exercises","scheduled"].includes(k)&&typeof cloudChanged==="function")cloudChanged()}
};
// Apply the user's corrected equipment to the saved plan once; history/drafts stay intact.

migrateLunge114();


const byId=id=>{const e=exercises().find(x=>x.id===id);return typeof rf13Exercise==='function'?rf13Exercise(e):e;};
const $=s=>document.querySelector(s);
const esc=s=>String(s??"").replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[m]));


document.addEventListener('visibilitychange',()=>{if(!document.hidden)tickRest()});


render();

if(!window.TrainPilotBoot?.loading)initCloud();


// @endsection app.js

// @section v12.js
var rf12BuiltinPrograms = function rf12BuiltinPrograms(){return [
{id:'home-basic',name:'Otthoni A/B – Alap',location:'Otthon',level:'Kezdő',builtin:true,days:[{id:'A',name:'A',exercises:[...DEFAULT_PLAN.A]},{id:'B',name:'B',exercises:[...DEFAULT_PLAN.B]}]},
{id:'home-level2',name:'Otthoni A/B – 2. szint',location:'Otthon',level:'Középhaladó',builtin:true,days:[{id:'A',name:'A',exercises:['goblet-squat','db-floor-press','one-arm-row','rdl','hammer-curl','plank']},{id:'B',name:'B',exercises:['bulgarian-split-squat','db-ohp','db-pullover','close-pushup','lateral-raise','crunch']}]},
{id:'home-varied',name:'Otthoni változatos',location:'Otthon',level:'Középhaladó',builtin:true,days:[{id:'A',name:'Teljes test 1',exercises:['db-squat','db-floor-press','barbell-row','rdl','lateral-raise','crunch']},{id:'B',name:'Teljes test 2',exercises:['reverse-lunge','db-ohp','one-arm-row','pushup','hammer-curl','plank']}]},
{id:'gym-fullbody',name:'Konditermi Full Body',location:'Edzőterem',level:'Kezdő',builtin:true,days:[{id:'A',name:'Full Body',exercises:['leg-press','machine-chest-press','lat-pulldown','leg-curl','machine-shoulder-press','crunch']}]},
{id:'gym-ab',name:'Konditermi A/B',location:'Edzőterem',level:'Középhaladó',builtin:true,days:[{id:'A',name:'A',exercises:['leg-press','machine-chest-press','seated-cable-row','leg-curl','cable-curl','crunch']},{id:'B',name:'B',exercises:['rdl','lat-pulldown','machine-shoulder-press','reverse-lunge','cable-triceps','plank']}]}
];};
var programs = function programs(){return db.get('programs',rf12BuiltinPrograms());};
var activeProgramId = function activeProgramId(){return db.get('activeProgramId','home-basic');};
var activeProgram = function activeProgram(){return programs().find(p=>p.id===activeProgramId())||programs()[0];};
var programById = function programById(id){return programs().find(p=>p.id===id);};
var programDay = function programDay(program,id){return program?.days?.find(d=>d.id===id)||program?.days?.[0];};
var plannerSettings = function plannerSettings(){return db.get('plannerSettings',{mode:'alternate',time:'18:00',minutes:45,weeks:4,weekdays:[1,3,5]});};
var mergeExerciseLibrary = function mergeExerciseLibrary(saved){const all=[...DEFAULT_EXERCISES,...EXTRA_EXERCISES].map(exerciseMeta),map=new Map(all.map(e=>[e.id,e]));for(const e of saved||[]){const base=map.get(e.id)||{};map.set(e.id,exerciseMeta({...base,...e}));}return [...map.values()];};
var migrateTo12 = function migrateTo12(){
 if(db.get('schemaVersion',0)>=RF12_SCHEMA)return;
 const oldEx=db.get('exercises',DEFAULT_EXERCISES),oldPlan=db.get('plan',DEFAULT_PLAN),ps=rf12BuiltinPrograms();
 const basic=ps.find(p=>p.id==='home-basic');basic.days=[{id:'A',name:'A',exercises:[...(oldPlan.A||DEFAULT_PLAN.A)]},{id:'B',name:'B',exercises:[...(oldPlan.B||DEFAULT_PLAN.B)]}];
 db.set('exercises',mergeExerciseLibrary(oldEx));db.set('programs',ps);db.set('activeProgramId','home-basic');
 const migrated=scheduled().map(p=>({...p,programId:p.programId||'home-basic',dayId:p.dayId||p.workout||'A',workout:p.workout||p.dayId||'A',status:p.cancelled?'cancelled':(p.status||'planned')}));db.set('scheduled',migrated);
 db.set('plannerSettings',plannerSettings());db.set('schemaVersion',RF12_SCHEMA);
};
var migratePlank121 = function migratePlank121(){
 if(db.get('plank121',false))return;
 const ps=programs(),basic=ps.find(p=>p.id==='home-basic'),a=basic?.days.find(d=>d.id==='A');
 if(a&&!a.exercises.includes('plank')){a.exercises.splice(Math.min(5,a.exercises.length),0,'plank');db.set('programs',ps);}
 const legacy=db.get('plan',null);if(legacy?.A&&!legacy.A.includes('plank')){legacy.A.splice(Math.min(5,legacy.A.length),0,'plank');db.set('plan',legacy);}
 db.set('plank121',true);
};
var workoutTitle = function workoutTitle(programId,dayId){const p=programById(programId)||activeProgram(),d=programDay(p,dayId);return `${p.name} • ${d?.name||dayId}`;};
var nextPlanned = function nextPlanned(){const now=Date.now();return scheduled().filter(x=>!x.cancelled&&(x.status||'planned')==='planned'&&Date.parse(x.start)>=now&&!history().some(h=>h.scheduleId===x.id)).sort((a,b)=>Date.parse(a.start)-Date.parse(b.start))[0];};
var startScheduledById = function startScheduledById(id){const p=scheduled().find(x=>x.id===id);if(p)startWorkout(p.dayId||p.workout,p.id,p.programId||'home-basic');};
var localDateKey = function localDateKey(d){const y=d.getFullYear(),m=String(d.getMonth()+1).padStart(2,'0'),day=String(d.getDate()).padStart(2,'0');return `${y}-${m}-${day}`;};
var dateAt = function dateAt(date,time){const d=new Date(`${date}T${time||'18:00'}`);if(!Number.isFinite(d.getTime()))throw Error('Érvénytelen dátum vagy idő.');return d;};
var cycleDay = function cycleDay(program,index){return program.days[((index%program.days.length)+program.days.length)%program.days.length];};
var nextCycleIndex = function nextCycleIndex(programId){const p=programById(programId),items=[...scheduled().filter(x=>!x.cancelled&&(x.programId||'home-basic')===programId)].sort((a,b)=>Date.parse(a.start)-Date.parse(b.start));if(!items.length)return 0;const last=items[items.length-1],i=p.days.findIndex(d=>d.id===(last.dayId||last.workout));return i<0?0:i+1;};
var makeScheduleItem = function makeScheduleItem(program,date,time,minutes,dayId){const start=dateAt(date,time),d=programDay(program,dayId);return {id:crypto.randomUUID(),programId:program.id,dayId:d.id,workout:d.id,start:start.toISOString(),end:new Date(start.getTime()+minutes*60000).toISOString(),status:'planned',updatedAt:Date.now(),cancelled:false};};
var buildAlternateSchedule = function buildAlternateSchedule(date,time,count,minutes,firstDayId,programId=activeProgramId()){const p=programById(programId);if(!p||!/^\d{4}-\d{2}-\d{2}$/.test(date)||!/^\d{2}:\d{2}$/.test(time)||!Number.isInteger(count)||count<1||count>100||!Number.isInteger(minutes)||minutes<10||minutes>240)throw Error('Adj meg érvényes kezdést, időtartamot és alkalomszámot.');let idx=Math.max(0,p.days.findIndex(d=>d.id===firstDayId));const out=[];for(let n=0;n<count;n++){const d=dateAt(date,time);d.setDate(d.getDate()+n*2);out.push(makeScheduleItem(p,localDateKey(d),time,minutes,cycleDay(p,idx+n).id));}return out;};
var buildWeeklySchedule = function buildWeeklySchedule(date,time,days,weeks,minutes,firstDayId,programId=activeProgramId()){const p=programById(programId);if(!p||!days.length||days.some(x=>!Number.isInteger(x)||x<0||x>6)||!Number.isInteger(weeks)||weeks<1||weeks>24)throw Error('Válassz heti napokat és érvényes időtartamot.');const start=dateAt(date,time),out=[];let idx=Math.max(0,p.days.findIndex(d=>d.id===firstDayId));for(let n=0;n<weeks*7;n++){const d=new Date(start);d.setDate(start.getDate()+n);if(!days.includes(d.getDay()))continue;out.push(makeScheduleItem(p,localDateKey(d),time,minutes,cycleDay(p,idx++).id));}return out;};
var addScheduleBatch = function addScheduleBatch(items){const cur=scheduled(),unique=items.filter(x=>!cur.some(p=>!p.cancelled&&p.start===x.start&&(p.programId||'home-basic')===x.programId));if(cur.length+unique.length>1000)throw Error('Maximum 1000 tervezett alkalom tárolható.');db.set('scheduled',[...cur,...unique]);return unique.length;};
var savePlannerSettings = function savePlannerSettings(){const mode=$('#plannerMode')?.value||'alternate',time=$('#plannerTime')?.value||'18:00',minutes=Math.max(10,Math.min(240,Number($('#plannerMinutes')?.value)||45)),weeks=Math.max(1,Math.min(24,Number($('#plannerWeeks')?.value)||4)),weekdays=[...document.querySelectorAll('input[name=plannerWeekday]:checked')].map(e=>Number(e.value));db.set('plannerSettings',{mode,time,minutes,weeks,weekdays});};
var generatePlanner = function generatePlanner(){try{savePlannerSettings();const s=plannerSettings(),date=$('#plannerStart').value,first=$('#plannerFirst').value;let items;if(s.mode==='alternate')items=buildAlternateSchedule(date,s.time,Math.ceil(s.weeks*7/2),s.minutes,first);else if(s.mode==='weekly')items=buildWeeklySchedule(date,s.time,s.weekdays,s.weeks,s.minutes,first);else{alert('Egyéni módban koppints közvetlenül a naptár napjaira.');return;}const n=addScheduleBatch(items);render();alert(n?`${n} edzés megtervezve.`:'Ezek az alkalmak már szerepelnek.');}catch(e){alert(e.message);}};
var changeCalendarMonth = function changeCalendarMonth(delta){const [y,m]=state.calendarMonth.split('-').map(Number),d=new Date(y,m-1+delta,1);state.calendarMonth=`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`;render();};
var scheduleOnDate = function scheduleOnDate(date){return scheduled().filter(x=>!x.cancelled&&localDateKey(new Date(x.start))===date).sort((a,b)=>Date.parse(a.start)-Date.parse(b.start));};
var toggleCalendarDay = function toggleCalendarDay(date){const exists=scheduleOnDate(date).find(x=>(x.status||'planned')==='planned'&&!history().some(h=>h.scheduleId===x.id));if(exists){if(confirm('Eltávolítod ezt a tervezett edzést?')){const s=scheduled(),i=s.findIndex(x=>x.id===exists.id);s[i]={...s[i],cancelled:true,status:'cancelled',updatedAt:Date.now()};db.set('scheduled',s);render();}return;}const s=plannerSettings(),p=activeProgram(),idx=nextCycleIndex(p.id),item=makeScheduleItem(p,date,s.time,s.minutes,cycleDay(p,idx).id);addScheduleBatch([item]);render();};
var calendarGrid = function calendarGrid(){const [y,m]=state.calendarMonth.split('-').map(Number),first=new Date(y,m-1,1),last=new Date(y,m,0),lead=(first.getDay()+6)%7,cells=[];for(let i=0;i<lead;i++)cells.push('<div class="cal-cell empty"></div>');for(let day=1;day<=last.getDate();day++){const d=new Date(y,m-1,day),key=localDateKey(d),items=scheduleOnDate(key),h=history().filter(x=>localDateKey(new Date(x.started))===key),today=key===localDateKey(new Date()),item=items[0],done=item&&(item.status==='completed'||h.some(x=>x.scheduleId===item.id)),cls=['cal-cell',today?'today':'',item?'planned':'',done?'completed':'',item?.status==='skipped'?'skipped':''].join(' ');const label=item?esc(programDay(programById(item.programId||'home-basic'),item.dayId||item.workout)?.name||item.dayId||item.workout):h.length?'✓':'';cells.push(`<button class="${cls}" onclick="toggleCalendarDay('${key}')"><span>${day}</span>${label?`<b>${done?'✓ ':''}${label}</b>`:''}</button>`);}return cells.join('');};
var scheduleListHtml = function scheduleListHtml(){const items=scheduled().filter(x=>!x.cancelled).sort((a,b)=>Date.parse(a.start)-Date.parse(b.start)).slice(-120);return items.length?items.map(x=>{const p=programById(x.programId||'home-basic'),d=programDay(p,x.dayId||x.workout),done=x.status==='completed'||history().some(h=>h.scheduleId===x.id);return `<div class="card schedule-card"><div><strong>${esc(p?.name||'Program')} • ${esc(d?.name||x.dayId||x.workout)}</strong><div class="small muted">${esc(fmtDate(x.start))} • ${done?'Teljesítve':x.status==='skipped'?'Kihagyva':'Tervezett'}</div></div><div class="schedule-actions">${!done?`<button class="btn" onclick="startScheduledById('${x.id}')">Indítás</button>`:''}<button class="btn secondary" onclick="editSchedulePrompt('${x.id}')">Áthelyezés</button><button class="btn secondary" onclick="skipSchedule('${x.id}')">${x.status==='skipped'?'Vissza':'Kihagyás'}</button></div></div>`;}).join(''):'<div class="muted">Még nincs tervezett edzés.</div>';};
var editSchedulePrompt = function editSchedulePrompt(id){const s=scheduled(),i=s.findIndex(x=>x.id===id);if(i<0)return;const old=s[i],date=prompt('Új dátum (ÉÉÉÉ-HH-NN):',localDateKey(new Date(old.start)));if(date===null)return;const time=prompt('Új időpont (ÓÓ:PP):',new Date(old.start).toTimeString().slice(0,5));if(time===null)return;try{const st=dateAt(date,time),dur=Date.parse(old.end)-Date.parse(old.start);s[i]={...old,start:st.toISOString(),end:new Date(st.getTime()+dur).toISOString(),updatedAt:Date.now()};db.set('scheduled',s);render();}catch(e){alert(e.message);}};
var skipSchedule = function skipSchedule(id){const s=scheduled(),i=s.findIndex(x=>x.id===id);if(i<0)return;s[i]={...s[i],status:s[i].status==='skipped'?'planned':'skipped',updatedAt:Date.now()};db.set('scheduled',s);render();};
var calendarScreen = function calendarScreen(){const s=plannerSettings(),p=activeProgram(),days=p.days||[];return shell(`<main><div class="hero"><h1>Edzésnaptár</h1><div class="muted">Minden második nap, fix heti napok vagy teljesen egyéni kijelölés.</div></div><div class="setting"><label>Tervezési mód</label><select id="plannerMode" class="field"><option value="alternate" ${s.mode==='alternate'?'selected':''}>Minden második nap</option><option value="weekly" ${s.mode==='weekly'?'selected':''}>Fix heti napok</option><option value="custom" ${s.mode==='custom'?'selected':''}>Egyéni naptár</option></select><label>Kezdőnap</label><input id="plannerStart" class="field" type="date" value="${localDateKey(new Date())}"><label>Időpont</label><input id="plannerTime" class="field" type="time" value="${esc(s.time)}"><label>Időtartam (perc)</label><input id="plannerMinutes" class="field" type="number" min="10" max="240" value="${s.minutes}"><label>Hetek száma</label><input id="plannerWeeks" class="field" type="number" min="1" max="24" value="${s.weeks}"><label>Első programnap</label><select id="plannerFirst" class="field">${days.map(d=>`<option value="${esc(d.id)}">${esc(d.name)}</option>`).join('')}</select><div class="weekday-row">${['V','H','K','Sze','Cs','P','Szo'].map((d,i)=>`<label><input type="checkbox" name="plannerWeekday" value="${i}" ${s.weekdays.includes(i)?'checked':''}>${d}</label>`).join('')}</div><button class="btn block" onclick="generatePlanner()">Terv generálása</button><p class="small muted">Egyéni módban vagy utólag közvetlenül a naptár napjaira koppintva adhatsz hozzá/törölhetsz alkalmat.</p></div><div class="calendar-wrap"><div class="calendar-head"><button class="btn secondary" onclick="changeCalendarMonth(-1)">‹</button><strong>${new Date(state.calendarMonth+'-01T12:00').toLocaleDateString('hu-HU',{year:'numeric',month:'long'})}</strong><button class="btn secondary" onclick="changeCalendarMonth(1)">›</button></div><div class="cal-weekdays">${['H','K','Sze','Cs','P','Szo','V'].map(x=>`<span>${x}</span>`).join('')}</div><div class="calendar-grid">${calendarGrid()}</div><div class="cal-legend"><span>○ tervezett</span><span>✓ teljesített</span><span>– kihagyott</span></div></div><div class="section">Tervezett alkalmak</div>${scheduleListHtml()}</main>`);};
var activateProgram = function activateProgram(id){if(!programById(id))return;if(!confirm('Aktiválod ezt a programot? A korábbi napló és tervezett alkalmak megmaradnak.'))return;db.set('activeProgramId',id);render();};
var programCards = function programCards(){return programs().map(p=>{const active=p.id===activeProgramId();return `<div class="program-card rf103-program-card ${active?'active-program':''}"><div class="rf103-program-main"><div class="rf103-program-info"><span class="badge">${esc(p.location)}</span><span class="badge">${esc(p.level)}</span><h3>${esc(p.name)}</h3><p class="small muted">${p.days.length} napos ciklus • ${p.days.map(d=>d.exercises.length).join('/')} gyakorlat</p></div><button class="btn rf103-program-activate ${active?'secondary':''}" ${active?'disabled':''} onclick="activateProgram('${p.id}')">${active?'Aktív':'Aktiválás'}</button></div>${!p.builtin?`<div class="rf103-program-extra"><button class="btn secondary" onclick="editCustomProgram('${p.id}')">Szerkesztés</button><button class="btn danger" onclick="deleteCustomProgram('${p.id}')">Törlés</button></div>`:''}</div>`}).join('');};
var programsScreen = function programsScreen(){return shell(`<main><div class="hero"><h1>Edzésprogramok</h1><div class="muted">Otthoni, konditermi és saját programok ugyanabban a naplóban.</div></div>${programCards()}<div class="section">Saját program</div><button class="btn block" onclick="createCustomProgram()">Új saját program létrehozása</button><div class="section">Gyakorlatkönyvtár</div><div class="card"><strong>${exercises().length} gyakorlat</strong><p class="small muted">Otthoni és konditermi gyakorlatok közös könyvtára. A meglévő videók és technikai segítségek megmaradtak.</p></div></main>`);};
var createCustomProgram = function createCustomProgram(){const name=prompt('Program neve:','Saját program');if(!name)return;const count=Math.max(1,Math.min(7,Number(prompt('Hány napos ciklus? (1–7)','2'))||2)),p={id:'custom-'+crypto.randomUUID(),name:name.slice(0,80),location:'Egyéni',level:'Egyéni',builtin:false,days:Array.from({length:count},(_,i)=>({id:String.fromCharCode(65+i),name:String.fromCharCode(65+i),exercises:[DEFAULT_PLAN.A[0],DEFAULT_PLAN.A[1],DEFAULT_PLAN.A[2]]}))};db.set('programs',[...programs(),p]);editCustomProgram(p.id);};
var editCustomProgram = function editCustomProgram(id){const p=programById(id);if(!p||p.builtin)return;render(shell(`<main><button class="btn secondary" onclick="go('programs')">← Vissza</button><div class="hero"><h1>${esc(p.name)}</h1><div class="muted">Válassz gyakorlatokat és sorrendet minden programnaphoz.</div></div>${p.days.map((d,di)=>`<div class="setting"><label>${esc(d.name)} nap neve</label><input class="field" value="${esc(d.name)}" onchange="customDayName('${id}',${di},this.value)"><label>Gyakorlatok</label>${d.exercises.map((eid,ei)=>`<div class="custom-ex-row"><select class="field" onchange="customExercise('${id}',${di},${ei},this.value)">${exercises().map(e=>`<option value="${e.id}" ${e.id===eid?'selected':''}>${esc(e.hu)}</option>`).join('')}</select><button class="btn danger" onclick="removeCustomExercise('${id}',${di},${ei})">×</button></div>`).join('')}<button class="btn secondary block" onclick="addCustomExercise('${id}',${di})">+ Gyakorlat</button></div>`).join('')}</main>`));};
var updateProgram = function updateProgram(id,fn){const ps=programs(),i=ps.findIndex(p=>p.id===id);if(i<0)return;fn(ps[i]);db.set('programs',ps);};
var customDayName = function customDayName(id,di,v){updateProgram(id,p=>p.days[di].name=(v||p.days[di].id).slice(0,40));};
var customExercise = function customExercise(id,di,ei,eid){updateProgram(id,p=>p.days[di].exercises[ei]=eid);editCustomProgram(id);};
var addCustomExercise = function addCustomExercise(id,di){updateProgram(id,p=>{if(p.days[di].exercises.length<20)p.days[di].exercises.push(exercises()[0].id)});editCustomProgram(id);};
var removeCustomExercise = function removeCustomExercise(id,di,ei){updateProgram(id,p=>{if(p.days[di].exercises.length>1)p.days[di].exercises.splice(ei,1)});editCustomProgram(id);};
var deleteCustomProgram = function deleteCustomProgram(id){const p=programById(id);if(!p||p.builtin||!confirm('Törlöd ezt a saját programot? A korábbi naplóbejegyzések megmaradnak.'))return;db.set('programs',programs().filter(x=>x.id!==id));if(activeProgramId()===id)db.set('activeProgramId','home-basic');render();};
/* RepForge 1.2 feature layer: program library, flexible calendar planning, safe 1.1.5 migration. */
const RF12_SCHEMA=3;
const EXTRA_EXERCISES=[
{id:'goblet-squat',hu:'Goblet guggolás',en:'Goblet Squat',equipment:'1 kézisúlyzó',target:'Láb/far',notes:'Tarts egy kézisúlyzót a mellkas előtt. Ülj csípőből lefelé, térd kövesse a lábfej irányát, majd állj fel.',sets:3,reps:'8–12',weight:8,loadType:'single_dumbbell',repUnit:'ism.'},
{id:'bulgarian-split-squat',hu:'Bolgár guggolás',en:'Bulgarian Split Squat',equipment:'2 kézisúlyzó + támasz',target:'Láb/far',notes:'A hátsó láb legyen megtámasztva. Függőlegesen engedd a csípőt, az első lábbal told vissza magad. Ismétlés lábanként.',sets:3,reps:'8–12 / láb',weight:5,loadType:'per_hand',repUnit:'/láb'},
{id:'db-pullover',hu:'Kézisúlyzós pullover',en:'Dumbbell Pullover',equipment:'1 kézisúlyzó',target:'Hát/mell',notes:'Hanyatt fekve két kézzel fogd a súlyzót, kontrolláltan engedd a fej mögé, majd húzd vissza a mellkas fölé.',sets:3,reps:'10–15',weight:5,loadType:'single_dumbbell',repUnit:'ism.'},
{id:'lateral-raise',hu:'Oldalemelés',en:'Dumbbell Lateral Raise',equipment:'2 kézisúlyzó',target:'Váll',notes:'Enyhén hajlított könyökkel emeld oldalra a súlyzókat vállmagasság közeléig, lendítés nélkül.',sets:2,reps:'12–15',weight:2.5,loadType:'per_hand',repUnit:'ism.'},
{id:'hammer-curl',hu:'Kalapács bicepszezés',en:'Hammer Curl',equipment:'2 kézisúlyzó',target:'Bicepsz/alkar',notes:'Semleges fogással, a tenyerek egymás felé néznek. Könyök maradjon a test mellett.',sets:2,reps:'10–15',weight:5,loadType:'per_hand',repUnit:'ism.'},
{id:'close-pushup',hu:'Szűk fekvőtámasz',en:'Close-Grip Push-Up',equipment:'testsúly',target:'Tricepsz/mell',notes:'A kezek legyenek a vállszélességnél közelebb. Tartsd feszesen a törzset és kontrolláltan engedd le magad.',sets:3,reps:'közel maximum',weight:0,loadType:'bodyweight',repUnit:'ism.'},
{id:'leg-press',hu:'Lábtoló gép',en:'Leg Press',equipment:'lábtoló gép',target:'Láb/far',notes:'A derekad maradjon a támlán. Engedd kontrolláltan a terhelést, majd nyomd vissza a térd teljes zárása nélkül.',sets:3,reps:'8–12',weight:20,loadType:'total',repUnit:'ism.'},
{id:'machine-chest-press',hu:'Mellnyomó gép',en:'Machine Chest Press',equipment:'mellnyomó gép',target:'Mell/tricepsz',notes:'Lapocka legyen stabilan a támlán. Nyomd előre a fogantyút, majd kontrolláltan engedd vissza.',sets:3,reps:'8–12',weight:15,loadType:'total',repUnit:'ism.'},
{id:'lat-pulldown',hu:'Lehúzás mellhez',en:'Lat Pulldown',equipment:'csigás lehúzó',target:'Hát',notes:'Enyhén dőlj hátra, húzd a rudat a felső mellkas felé. Ne rántsd és ne húzd tarkó mögé.',sets:3,reps:'8–12',weight:20,loadType:'total',repUnit:'ism.'},
{id:'seated-cable-row',hu:'Ülő csigás evezés',en:'Seated Cable Row',equipment:'csigás gép',target:'Hát',notes:'Semleges törzzsel húzd a fogantyút a has felé, lapockákat zárd, majd kontrolláltan engedd vissza.',sets:3,reps:'8–12',weight:20,loadType:'total',repUnit:'ism.'},
{id:'leg-curl',hu:'Lábhajlító gép',en:'Leg Curl',equipment:'lábhajlító gép',target:'Combhajlító',notes:'A csípő maradjon stabil. Hajlítsd a térdet kontrolláltan, majd lassan engedd vissza.',sets:3,reps:'10–15',weight:15,loadType:'total',repUnit:'ism.'},
{id:'machine-shoulder-press',hu:'Vállnyomó gép',en:'Machine Shoulder Press',equipment:'vállnyomó gép',target:'Váll/tricepsz',notes:'A hát legyen megtámasztva. Nyomd a fogantyút felfelé, majd kontrolláltan engedd vissza.',sets:3,reps:'8–12',weight:10,loadType:'total',repUnit:'ism.'},
{id:'cable-triceps',hu:'Csigás tricepsz lenyomás',en:'Cable Triceps Pushdown',equipment:'csiga',target:'Tricepsz',notes:'Könyök maradjon a test mellett. Nyújtsd ki az alkart lefelé, majd kontrolláltan engedd vissza.',sets:2,reps:'10–15',weight:10,loadType:'total',repUnit:'ism.'},
{id:'cable-curl',hu:'Csigás bicepszezés',en:'Cable Curl',equipment:'csiga',target:'Bicepsz',notes:'Könyök maradjon a test mellett. Hajlítsd az alkart lendítés nélkül.',sets:2,reps:'10–15',weight:10,loadType:'total',repUnit:'ism.'}
];


// Make new data domains participate in cloud-change tracking.
const rf12DbSet=db.set.bind(db);db.set=function(k,v){rf12DbSet(k,v);if(['programs','activeProgramId','plannerSettings'].includes(k)&&typeof cloudChanged==='function')cloudChanged();};
migrateTo12();
// Restore the missing sixth exercise once, without resetting custom load/sets or history.

migratePlank121();


// Compatibility facade for the old A/B editor.
plan=function(){const p=activeProgram();const out={};for(const d of p.days||[])out[d.id]=d.exercises;return out;};
exercises=function(){return mergeExerciseLibrary(db.get('exercises',DEFAULT_EXERCISES));};
nextWorkout=function(){const p=activeProgram(),days=p.days||[];if(!days.length)return 'A';const h=history().find(x=>(x.programId||'home-basic')===p.id);if(!h)return days[0].id;const i=days.findIndex(d=>d.id===(h.dayId||h.workout));return days[(i<0?0:i+1)%days.length].id;};

shell=function(content){return `<div><div class="top"><div class="brand">REPFORGE</div><div class="tag">1.3.2 • rugalmas edzéstervező</div><div class="tabs tabs6">${tab('home','Kezdőlap')}${tab('plan','Edzés')}${tab('calendar','Naptár')}${tab('programs','Programok')}${tab('history','Napló')}${tab('settings','Beállítás')}</div></div>${content}${state.session&&state.timer>0?timerHtml():''}</div>`;};
go=function(t){if(state.session&&!confirm('Kilépsz a folyamatban lévő edzésből? A mentett vázlat megmarad, később folytathatod.'))return;stopTimer();state.tab=t;state.session=null;state.workout=null;render();};


home=function(){const h=history(),w=weights(),draft=db.get('draft',null),p=activeProgram(),np=nextPlanned(),next=nextWorkout();const latestW=w.length?w[0].kg:'—';return shell(`<main>${draft?`<div class="card"><strong>Félbehagyott edzés</strong><p class="small muted">${esc(draft.session?.programName||draft.session?.workout||'')} • a sorozatok mentve vannak.</p><button class="btn block" onclick="resumeDraft()">Edzés folytatása</button></div><br>`:''}<div class="hero"><span class="badge">Aktív program</span><h1>${esc(p.name)}</h1><div class="muted">${np?`Következő: ${esc(programDay(programById(np.programId),np.dayId)?.name||np.dayId)} • ${esc(fmtDate(np.start))}`:`Következő ciklusnap: ${esc(programDay(p,next)?.name||next)}`}</div><br>${np?`<button class="btn block" onclick="startScheduledById('${np.id}')">Tervezett edzés indítása</button>`:`<button class="btn block" onclick="startWorkout('${next}')">Edzés indítása</button>`}</div><div class="section">Gyors elérés</div><div class="grid2"><button class="btn secondary" onclick="go('calendar')">Naptár</button><button class="btn secondary" onclick="go('programs')">Programok</button></div></main>`);};

planScreen=function(){const p=activeProgram();return shell(`<main><div class="hero"><span class="badge">${esc(p.location)}</span><span class="badge">${esc(p.level)}</span><h1>${esc(p.name)}</h1><div class="muted">${p.days.length} napos ciklus • a sorrend az alkalmakat követi</div></div>${p.days.map(d=>`<div class="section">${esc(d.name)}</div>${d.exercises.map((id,i)=>exCard(byId(id),i,d.id)).join('')}<button class="btn block" onclick="startWorkout('${d.id}')">${esc(d.name)} indítása</button>`).join('')}</main>`);};
showWorkout=function(dayId){const p=activeProgram(),d=programDay(p,dayId);state.workout=dayId;state.tab='plan';render(shell(`<main><div class="hero"><h1>${esc(p.name)} • ${esc(d.name)}</h1><div class="muted">${d.exercises.length} gyakorlat</div></div>${d.exercises.map((id,i)=>exCard(byId(id),i,d.id)).join('')}<button class="btn block" onclick="startWorkout('${d.id}')">Edzés indítása</button></main>`));};
showExercise=function(dayId,i){const p=activeProgram(),d=programDay(p,dayId),e=byId(d.exercises[i]);state.tab='plan';render(shell(`<main><button class="btn secondary" onclick="showWorkout('${dayId}')">← Vissza</button><br><br><div class="card detail"><h2>${esc(e.hu)}</h2><div class="en">${esc(e.en)}</div><div class="meta">${e.sets} × ${esc(e.reps)} • ${e.weight?e.weight+' '+esc(e.unit):esc(e.unit)}</div><br><span class="badge">${esc(e.target)}</span><span class="badge">${esc(e.equipment)}</span>${motionDemo(e.id)}<p class="note">${esc(e.notes)}</p></div></main>`));};

startWorkout=function(dayId,scheduleId=null,programId=null){if(db.get('draft',null)&&!confirm('Új edzést indítasz? A félbehagyott edzés helyére ez kerül.'))return;const p=programById(programId)||activeProgram(),d=programDay(p,dayId);if(!d){alert('Nincs ilyen programnap.');return;}state.tab='plan';state.workout=d.id;state.current=0;state.session={programId:p.id,programName:p.name,dayId:d.id,workout:d.id,...(scheduleId?{scheduleId}:{}),started:new Date().toISOString(),exercises:d.exercises.map(id=>{const e=byId(id),prev=findLastExercise(id);return {id,hu:e.hu,en:e.en,loadType:e.loadType,repUnit:e.repUnit,sets:Array.from({length:e.sets},(_,i)=>({set:i+1,weight:prev?.loadType===e.loadType?(prev?.sets?.[i]?.weight??e.weight):e.weight,reps:'',done:false}))};})};renderWorkout();};

const rf12FinishWorkout=finishWorkout;finishWorkout=function(){if(!state.session)return;const scheduleId=state.session.scheduleId;rf12FinishWorkout();if(scheduleId){const s=scheduled(),i=s.findIndex(x=>x.id===scheduleId);if(i>=0){s[i]={...s[i],status:'completed',updatedAt:Date.now()};db.set('scheduled',s);}}};
renderWorkout=function(){persistDraft();const i=state.current,p=state.session.exercises.map(x=>x.id),e=byId(p[i]),se=state.session.exercises[i],pct=Math.round((i/p.length)*100);render(shell(`<main><div class="hero"><span class="badge">${esc(state.session.programName||'Edzés')} • ${esc(state.session.dayId||state.workout)}</span><h1>${i+1}/${p.length}</h1><div class="muted">${esc(e.hu)}</div><div class="progress"><div style="width:${pct}%"></div></div></div><div class="card detail"><h2>${esc(e.hu)}</h2><div class="en">${esc(e.en)}</div><div class="meta">Cél: ${e.sets} × ${esc(e.reps)}</div>${motionDemo(e.id)}<p class="note">${esc(e.notes)}</p></div><div class="section">Sorozatok</div>${se.sets.map((s,si)=>`<div class="row"><div class="num">${si+1}</div>${e.loadType==='bodyweight'?'<span class="small muted">Testsúly</span>':`<label class="small">${loadLabel(e.loadType)}<input class="field" inputmode="decimal" value="${s.weight}" oninput="upd(${i},${si},'weight',this.value)"></label>`}<label class="small">${e.repUnit}<input class="field" inputmode="numeric" placeholder="${esc(e.reps)}" value="${esc(s.reps)}" oninput="upd(${i},${si},'reps',this.value)"></label><button class="check ${s.done?'done':''}" onclick="toggleSet(${i},${si})">${s.done?'✓':'OK'}</button></div>`).join('')}<br><button class="btn block" onclick="nextExercise()">${i===p.length-1?'Edzés befejezése':'Következő gyakorlat'}</button><br><br><button class="btn secondary block" onclick="prevExercise()" ${i===0?'disabled':''}>Előző</button></main>`));};

// ----- Calendar planner -----
state.calendarMonth=state.calendarMonth||new Date().toISOString().slice(0,7);


// Backward-compatible test API.
buildSchedule=function(date,time,days,weeks,minutes,first){return buildWeeklySchedule(date,time,days,weeks,minutes,first,'home-basic');};


plannerPanel=function(){return `<div class="setting"><label>Edzésnaptár</label><p class="small muted">Az 1.2-ben a tervezés külön vizuális naptárban történik.</p><button class="btn block" onclick="go('calendar')">Naptár és tervező megnyitása</button></div>`;};

// ----- Program library and custom programs -----


// ----- Backup v3 -----
makeBackup=function(){return {app:'RepForge',version:3,appVersion:'1.2.1',schemaVersion:RF12_SCHEMA,exportedAt:new Date().toISOString(),history:history(),weights:weights(),settings:settings(),plan:db.get('plan',DEFAULT_PLAN),exercises:exercises(),scheduled:scheduled(),programs:programs(),activeProgramId:activeProgramId(),plannerSettings:plannerSettings()};};
const rf12OldValidateBackup=validateBackup;validateBackup=function(d){if(d?.version===3){const fail=()=>{throw Error('A fájl szerkezete nem érvényes RepForge-mentés.');};if(d.app!=='RepForge'||!Array.isArray(d.exercises)||!Array.isArray(d.history)||!Array.isArray(d.weights)||!Array.isArray(d.scheduled)||!Array.isArray(d.programs)||!d.programs.length||typeof d.activeProgramId!=='string'||!d.settings||typeof d.settings!=='object')fail();const ids=new Set(d.exercises.map(e=>e.id));for(const p of d.programs){if(!p||typeof p.id!=='string'||typeof p.name!=='string'||!Array.isArray(p.days)||!p.days.length||p.days.length>7)fail();for(const day of p.days)if(!day||typeof day.id!=='string'||!Array.isArray(day.exercises)||!day.exercises.length||day.exercises.some(id=>!ids.has(id)))fail();}for(const s of d.scheduled)if(!s||typeof s.id!=='string'||!Number.isFinite(Date.parse(s.start))||!Number.isFinite(Date.parse(s.end))||Date.parse(s.end)<=Date.parse(s.start)||!['planned','completed','skipped','cancelled'].includes(s.status|| (s.cancelled?'cancelled':'planned')))fail();return d;}return rf12OldValidateBackup(d);};
restoreText=function(text){if(text.length>20*1024*1024)throw Error('Maximum 20 MB.');const d=validateBackup(JSON.parse(text.replace(/^\uFEFF/,'')));if(!confirm(`${d.history.length} edzés és ${d.weights.length} testsúlyadat visszatöltése?\nEz lecseréli az app jelenlegi helyi adatait.`))return false;const oldKeys=['history','weights','settings','plan','exercises','scheduled','programs','activeProgramId','plannerSettings','draft'],old=oldKeys.map(k=>localStorage.getItem('repforge:'+k));try{for(const k of oldKeys){let v;if(k==='draft')v=null;else if(k==='programs')v=d.programs||rf12BuiltinPrograms();else if(k==='activeProgramId')v=d.activeProgramId||'home-basic';else if(k==='plannerSettings')v=d.plannerSettings||plannerSettings();else if(k==='scheduled')v=d.scheduled||[];else v=d[k];if(v!==undefined)db.set(k,v);}db.set('schemaVersion',RF12_SCHEMA);migrateTo12();}catch(e){oldKeys.forEach((k,i)=>{localStorage.removeItem('repforge:'+k);if(old[i]!=null)localStorage.setItem('repforge:'+k,old[i]);});throw Error('Nincs elég tárhely. A korábbi adatokat visszaállítottam.');}alert('Visszatöltés kész.');render();return true;};

// ----- Cloud v3 -----
canonicalSyncData=function(d){return canonical({history:d.history,weights:d.weights,settings:d.settings,exercises:d.exercises,plan:d.plan,scheduled:d.scheduled,programs:d.programs,activeProgramId:d.activeProgramId,plannerSettings:d.plannerSettings});};
syncData=function(){return makeBackup();};
validateSync=function(d){validateBackup(d);return d;};
mergeSync=function(local,remotes,base,choose){const out=JSON.parse(JSON.stringify(local));for(const kind of ['history','weights','scheduled']){const map=new Map((local[kind]||[]).map(x=>[recordKey(kind,x),x]));for(const remote of remotes)for(const x of remote[kind]||[]){const key=recordKey(kind,x),old=map.get(key);if(!old){map.set(key,x);continue;}if(canonical(old)===canonical(x))continue;if(kind==='scheduled'&&(old.updatedAt||0)!==(x.updatedAt||0)){map.set(key,(old.updatedAt||0)>(x.updatedAt||0)?old:x);continue;}map.set(key,choose(kind,old,x));}out[kind]=[...map.values()].sort((a,b)=>String(b.started||b.date||b.start).localeCompare(String(a.started||a.date||a.start)));}for(const key of ['settings','exercises','plan','programs','activeProgramId','plannerSettings']){let selected=local[key];for(const remote of remotes){const x=remote[key];if(x===undefined||canonical(selected)===canonical(x))continue;if(base&&canonical(x)===canonical(base[key]))continue;if(base&&canonical(selected)===canonical(base[key]))selected=x;else selected=choose(key,selected,x);}out[key]=selected;}return out;};
storeMerged=function(d){const keys=['history','weights','settings','exercises','plan','scheduled','programs','activeProgramId','plannerSettings'],old=keys.map(k=>localStorage.getItem('repforge:'+k));try{for(const k of keys)if(d[k]!==undefined)localStorage.setItem('repforge:'+k,JSON.stringify(d[k]));}catch(e){keys.forEach((k,i)=>{localStorage.removeItem('repforge:'+k);if(old[i]!=null)localStorage.setItem('repforge:'+k,old[i]);});throw Error('Nincs elég hely a szinkronadatoknak.');}};
calendarEvents=async function(){const events=new Map();for(const p of scheduled()){const id=await eventId('plan:'+p.id),prog=programById(p.programId||'home-basic'),day=programDay(prog,p.dayId||p.workout),cancel=p.cancelled||p.status==='cancelled';events.set(id,cancel?{id,cancelled:true}:{id,summary:`RepForge – ${prog?.name||'Edzés'} • ${day?.name||p.dayId||p.workout}${p.status==='skipped'?' (kihagyva)':''}`,description:p.status==='skipped'?'Kihagyott tervezett edzés':'Tervezett edzés',start:{dateTime:p.start},end:{dateTime:p.end},reminders:{useDefault:false,overrides:[{method:'popup',minutes:30}]}});}for(const h of history()){if(!Number.isFinite(Date.parse(h.finished))||!Number.isFinite(Date.parse(h.started))||Date.parse(h.finished)<=Date.parse(h.started))continue;const id=await eventId(h.scheduleId?'plan:'+h.scheduleId:'workout:'+recordKey('history',h));events.set(id,{id,summary:`✓ RepForge – ${h.programName||programById(h.programId)?.name||'Edzés'} • ${h.dayId||h.workout}`,description:workoutDescription(h),start:{dateTime:h.started},end:{dateTime:h.finished},reminders:{useDefault:false}});}return [...events.values()];};
calendarIntent=async function(index,planned=false){try{const x=planned?scheduled()[index]:history()[index];if(!x)throw Error('Nincs ilyen edzés.');const start=Date.parse(planned?x.start:x.started),end=Date.parse(planned?x.end:x.finished);if(!Number.isFinite(start)||!Number.isFinite(end)||end<=start)throw Error('Ehhez az edzéshez nincs érvényes kezdési/befejezési idő.');if(!isNative())throw Error('A naptárgomb Androidon használható.');const title=planned?workoutTitle(x.programId||'home-basic',x.dayId||x.workout):(x.programName||'RepForge')+' • '+(x.dayId||x.workout);await nativeFiles().addCalendarEvent({title:'RepForge – '+title,start,end,description:planned?'Tervezett edzés':workoutDescription(x)});alert('A naptár ablaka megnyílt. Ott külön mentsd el a kézi eseményt.');}catch(e){alert(e.message);}};

settingsScreen=function(){const s=settings();return shell(`<main><div class="hero"><h1>Beállítások</h1><div class="muted">RepForge 1.3.2 • helyi adatok + opcionális Google-szinkron.</div></div><div class="setting"><label>Pihenőidő (másodperc)</label><input id="rest" class="field" type="number" min="30" max="300" value="${s.rest}"><br><br><button class="btn block" onclick="saveSettings()">Mentés</button></div><div class="setting"><label>Biztonsági mentés</label><p class="small muted">A v3 mentés már a programokat és az új naptártervet is tartalmazza. Régi v1/v2 mentések továbbra is importálhatók.</p><p class="small muted">${esc(backupStatus())}</p><div class="grid2"><button class="btn secondary" onclick="exportData()">Mentés fájlba</button><button class="btn secondary" onclick="chooseImport()">Visszatöltés</button></div><input id="importFile" type="file" accept=".json,application/json" hidden onchange="importData(this.files[0])"></div>${cloudPanel()}${plannerPanel()}<div class="setting"><label>Edzésprofil</label><p class="small muted">Az eredeti Excel-alapterv az Otthoni A/B – Alap programban marad. A kímélő profil a gyakorlatok sorozatszámát módosítja.</p><button class="btn secondary block" onclick="resetStarter()">Kímélő kezdőprofil (2 sorozat)</button><br><button class="btn danger block" onclick="resetPlan()">Alapértékek visszaállítása</button></div></main>`);};
resetPlan=function(){if(!confirm('Visszaállítod az alapértelmezett Otthoni A/B – Alap programot és gyakorlatértékeket? A napló megmarad.'))return;db.set('exercises',mergeExerciseLibrary(DEFAULT_EXERCISES));const ps=programs(),i=ps.findIndex(p=>p.id==='home-basic');if(i>=0)ps[i]=rf12BuiltinPrograms()[0];db.set('programs',ps);render();};
resetStarter=function(){if(!confirm('A gyakorlatok alap sorozat/ismétlés értékei a kímélő profilra állnak. A napló megmarad.'))return;db.set('exercises',mergeExerciseLibrary(STARTER_EXERCISES));render();};

const rf12OldRender=render;render=function(custom){if(window.TrainPilotBoot?.loading)return;closeDemo();const root=$('#app');if(custom){root.innerHTML=custom;return;}if(state.tab==='home')root.innerHTML=home();else if(state.tab==='plan')root.innerHTML=planScreen();else if(state.tab==='calendar')root.innerHTML=calendarScreen();else if(state.tab==='programs')root.innerHTML=programsScreen();else if(state.tab==='history')root.innerHTML=historyScreen();else root.innerHTML=settingsScreen();};

// Last pass: migrated data and new UI.
migrateTo12();render();

// 1.2 draft/history polish loaded after the feature layer definitions.
resumeDraft=function(){const d=db.get('draft',null);if(!d?.session||!Array.isArray(d.session.exercises)||!d.session.exercises.length){alert('Nincs visszaállítható edzés.');return;}const p=programById(d.session.programId)||activeProgram(),day=programDay(p,d.session.dayId||d.workout);if(!day||d.session.exercises.some(e=>!byId(e.id))){alert('A félbehagyott edzés nem állítható vissza a jelenlegi gyakorlatkönyvtárral.');return;}stopTimer();state.tab='plan';state.session=d.session;state.workout=d.session.dayId||d.workout;state.current=Math.max(0,Math.min(d.current||0,d.session.exercises.length-1));if(d.restEndAt>Date.now()){state.restEndAt=d.restEndAt;state.timer=Math.ceil((d.restEndAt-Date.now())/1000);state.timerId=setInterval(tickRest,250);}renderWorkout();};
historyScreen=function(){const h=history();return shell(`<main><div class="hero"><h1>Edzésnapló</h1><div class="muted">Minden program korábbi edzése egy helyen marad.</div></div>${h.length?h.map((x,hi)=>{const c=completedSets(x),title=x.programName?`${x.programName} • ${x.dayId||x.workout}`:`Full Body ${x.workout}`;return `<div class="history"><div class="history-head"><div class="history-title-row"><div><div class="history-title">${esc(title)}</div><div class="history-date">${fmtDate(x.started)}</div></div><span class="badge">${x.exercises.length} gyakorlat</span></div><div class="history-badges"><span class="history-badge">⏱ ${sessionDuration(x)}</span><span class="history-badge">✓ ${c.done}/${c.total} sorozat</span></div></div><div class="history-body"><button class="btn secondary" onclick="calendarIntent(${hi})">Hozzáadás a naptárhoz</button>${x.exercises.map(e=>`<div class="history-ex"><div class="history-ex-name">${esc(e.hu)}</div><div class="en">${esc(e.en||'')}</div><div class="history-setchips">${(e.sets||[]).map(s=>`<span class="setchip">${esc(formatSet(e,s))}${s.done?' ✓':' • nincs kész'}</span>`).join('')}</div></div>`).join('')}</div></div>`;}).join(''):'<div class="muted">Még nincs elmentett edzés.</div>'}</main>`);};


// @endsection v12.js

// @section catalog131.js

/* 14 additions. Video URLs found on YouTube 2026-09-10; actual embedded playback requires device testing. */
const CATALOG131=[
['bodyweight-squat','Saját testsúlyos guggolás','Bodyweight Squat','testsúly','bodyweight',['legs','core'],'Állj stabil terpeszbe, engedd a csípőd lefelé, majd állj fel. A térd kövesse a lábfejet.','m0GcZ24pK6k','Bupa Health','bodyweight'],
['glute-bridge','Farizomhíd','Glute Bridge','testsúly','bodyweight',['legs','core'],'Hanyatt fekve, hajlított térddel emeld a csípőt. Feszítsd a farizmot, majd engedd vissza kontrolláltan.','L9KZfxT654Y','Runna','bodyweight'],
['dead-bug','Dead bug – ellentétes kar-láb nyújtás','Dead Bug','testsúly','bodyweight',['core'],'Hanyatt fekve emeld a karokat és a hajlított lábakat. Váltva nyújtsd az ellentétes kart és lábat, stabil törzzsel.','GbSC02oU3To','Hinge Health','bodyweight'],
['bird-dog','Bird dog – négykézláb kar-láb nyújtás','Bird Dog','testsúly','bodyweight',['core','back','legs'],'Négykézláb helyzetből nyújtsd az ellentétes kart és lábat. A medence maradjon vízszintben.','xo7Qpb_NTKE','Hospital for Special Surgery','bodyweight'],
['side-plank','Oldalsó plank','Side Plank','testsúly','bodyweight',['core','shoulders'],'Oldalt fekve támaszkodj az alkarodra, és emeld a csípőd. Mindkét oldalt végezd el; az időt oldalanként add meg.','Oe9Tp9SvTCE','YouTube – Side Plank','bodyweight'],
['calf-raise','Álló vádliemelés','Standing Bodyweight Calf Raise','testsúly + stabil támasz','bodyweight',['legs'],'Stabil támasz mellett emelkedj lábujjhegyre, majd lassan engedd vissza a sarkad.','Uyg2QR1WAq8','FeelFitastic','bodyweight'],
['db-step-up','Fellépés kézisúlyzókkal','Dumbbell Step-Up','2 kézisúlyzó + stabil fellépő','per_hand',['legs','core'],'Stabil fellépőre lépj fel az egyik lábbal, majd kontrolláltan lépj vissza. Ismétlés lábanként; a fellépő ne billenhessen meg.','hgkk12L_Umk','Coach Kelly','dumbbells'],
['db-rdl','Román felhúzás kézisúlyzókkal','Dumbbell Romanian Deadlift','2 kézisúlyzó','per_hand',['legs','back'],'Enyhén hajlított térddel told hátra a csípőt. A súlyok maradjanak közel a lábhoz, a hát semleges.','hQgFixeXdZo','YouTube – Dumbbell RDL','dumbbells'],
['db-bench-press','Kézisúlyzós fekvenyomás padon','Dumbbell Bench Press','2 kézisúlyzó + vízszintes pad','per_hand',['chest','triceps','shoulders'],'Stabil padon fekve nyomd a súlyzókat a mellkas fölé, majd kontrolláltan engedd vissza. A lábak maradjanak stabilan a talajon.','VmB1G1K7v94','ScottHermanFitness','dumbbells'],
['incline-db-press','Kézisúlyzós nyomás ferde padon','Incline Dumbbell Press','2 kézisúlyzó + állítható pad','per_hand',['chest','shoulders','triceps'],'Ferde padon megtámasztott háttal nyomd fel a súlyokat. Ne lendítsd és ne emeld el a csípőd.','8iPEnn-ltC8','ScottHermanFitness','dumbbells'],
['db-reverse-fly','Döntött törzsű oldalemelés','Dumbbell Reverse Fly','2 kézisúlyzó','per_hand',['shoulders','back'],'Döntött, stabil törzzsel emeld oldalra a könnyű súlyzókat. A könyök enyhén hajlított; ne lendíts a törzseddel.','d1QEddtoOq0','YouTube – Dumbbell Reverse Fly','dumbbells'],
['face-pull','Köteles húzás archoz','Cable Face Pull','csiga + kötél','total',['shoulders','back'],'A kötelet az arc felé húzd, a két kéz az arc mellé érkezzen. Kontrolláltan engedd vissza, törzsből ne lendíts.','rep-qVOkqgk','ScottHermanFitness','machine'],
['leg-extension','Lábnyújtó gép','Leg Extension','lábnyújtó gép','total',['legs'],'Állítsd a gépet a testméretedhez. Stabil ülésből nyújtsd a térdet kontrolláltan, majd lassan engedd vissza.','4ZDm5EbiFI8','PureGym','machine'],
['cable-fly','Álló csigás tárogatás','Standing Cable Fly','kétoldali csiga','per_hand',['chest','shoulders'],'Enyhén hajlított könyökkel közelítsd a karokat a mellkas előtt, majd kontrolláltan nyisd vissza. A súlyt egy csigaoldalra add meg.','PRw7ieDBLl4','James Harrison Coaching','machine']
];
for(const [id,hu,en,equipment,loadType,muscles,notes,video,credit,gear] of CATALOG131){
 EXTRA_EXERCISES.push({id,hu,en,equipment,loadType,target:({legs:'Láb / far',core:'Törzs',chest:'Mell',shoulders:'Váll'})[muscles[0]],notes,sets:2,reps:id==='side-plank'?'15–30 mp / oldal':['dead-bug','bird-dog','db-step-up'].includes(id)?'8–12 / oldal':'8–12',weight:0,repUnit:id==='side-plank'?'mp/oldal':['dead-bug','bird-dog','db-step-up'].includes(id)?'/oldal':'ism.',gear});
 DEMOS[id]=[null,'youtube',video,credit];
}


// @endsection catalog131.js

// @section v13.js
var muscleGroups = function muscleGroups(id){return MUSCLE_MAP[id]||[];};
var trainingProfile = function trainingProfile(){return settings().profile||null;};
var rf13Exercise = function rf13Exercise(e){if(!e)return e;const p=state.prescriptionProgramId?programById(state.prescriptionProgramId):(state.session?programById(state.session.programId):activeProgram());const rx=state.session?.exercises?.find(x=>x.id===e.id)?.prescription||p?.prescriptions?.[e.id];return rx?{...e,...rx,notes:e.notes+' Pihenő: '+rx.rest+' mp. Első alkalommal könnyű próbasorozattal válassz súlyt; az üres súlymezőt töltsd ki.'}:e;};
var profileSelect = function profileSelect(id,label,options,value){return `<label>${label}<select id="${id}" class="field">${Object.entries(options).map(([k,v])=>`<option value="${k}" ${k===value?'selected':''}>${v}</option>`).join('')}</select></label>`;};
var profileScreen = function profileScreen(){const p=state.profilePreview||trainingProfile()||{age:'',height:'',weight:'',goal:'fitness',experience:'beginner',activity:'mixed',minutes:45,location:'home',gear:[],cadence:'alternate',split:'auto',excluded:[]};render(shell(`<main><button class="btn secondary" onclick="go('home')">← Vissza</button><div class="hero"><h1>Segíts elkezdeni</h1><p>Válaszaidból szerkeszthető edzéstervet készítünk. Először átnézheted, csak utána aktiválod.</p></div><div class="setting profile-grid"><label>Életkor<input id="pfAge" class="field" type="number" min="18" max="100" value="${esc(p.age)}"></label><label>Magasság (cm)<input id="pfHeight" class="field" type="number" min="100" max="250" value="${esc(p.height)}"></label><label>Testsúly (kg)<input id="pfWeight" class="field" type="number" step="0.1" min="30" max="350" value="${esc(p.weight)}"></label>${profileSelect('pfGoal','Cél',{fitness:'Általános fittség',muscle:'Izomépítés',strength:'Erősödés',fatloss:'Fogyás támogatása'},p.goal)}${profileSelect('pfExperience','Tapasztalat',{beginner:'Most kezdem / újrakezdem',intermediate:'Rendszeresen edzek'},p.experience)}${profileSelect('pfActivity','Munka és napi aktivitás',{sedentary:'Többnyire ülök',mixed:'Vegyes / sok séta',physical:'Fizikai munka'},p.activity)}${typeof gearForm132==='function'?gearForm132(p):profileSelect('pfLocation','Felszerelés',{home:'Kézisúlyzó',gym:'Gépek és súlyzók'},p.location)}${profileSelect('pfMinutes','Idő egy edzésre',{'20':'20 perc','30':'30 perc','45':'45 perc','60':'60 perc'},String(p.minutes))}${profileSelect('pfCadence','Beosztás',{alternate:'Minden második nap',weekly:'Fix heti napok',custom:'Naptárban kijelölöm'},p.cadence)}${profileSelect('pfSplit','Edzésfelosztás',{auto:'Válasszon az app',full:'Teljes test A/B',upperlower:'Felsőtest / alsótest',ppl:'Toló / húzó / láb'},p.split)}<details><summary>Kerülendő vagy nem elérhető gyakorlatok</summary><p class="small muted">Jelöld a számodra nem megfelelő mozdulatokat. Ez nem sérülésdiagnosztika vagy rehabilitációs terv.</p>${exercises().map(e=>`<label class="exclude-row"><input type="checkbox" name="pfExclude" value="${e.id}" ${(p.excluded||[]).includes(e.id)?'checked':''}>${esc(e.hu)}</label>`).join('')}</details><p class="small muted">A profil a telefonon tárolódik. Bekapcsolt Google-szinkron esetén a beállításokkal együtt mentjük. Az órától beolvasott egészségadatokat nem mentjük oda.</p><button class="btn block" onclick="previewProfile()">Személyes terv összeállítása</button></div></main>`));};
var validateProfile = function validateProfile(p){if(!p||!Number.isInteger(p.age)||p.age<18||p.age>100||!Number.isFinite(p.height)||p.height<100||p.height>250||!Number.isFinite(p.weight)||p.weight<30||p.weight>350)throw Error('Adj meg érvényes felnőtt életkort, magasságot és testsúlyt.');for(const [k,allowed] of Object.entries({goal:['fitness','muscle','strength','fatloss'],experience:['beginner','intermediate'],activity:['sedentary','mixed','physical'],location:['home','gym'],cadence:['alternate','weekly','custom'],split:['auto','full','upperlower','ppl']}))if(!allowed.includes(p[k]))throw Error('Érvénytelen profilválasztás.');if(![20,30,45,60].includes(p.minutes)||!Array.isArray(p.excluded)||p.excluded.some(id=>!MUSCLE_MAP[id]))throw Error('Érvénytelen időtartam vagy kizárás.');return p;};
var generatePersonalProgram = function generatePersonalProgram(input){const p=validateProfile(input),gym=p.location==='gym';const pool=gym?{leg:['leg-press','reverse-lunge','goblet-squat'],hinge:['leg-curl','rdl'],push:['machine-chest-press','db-floor-press','pushup'],pull:['lat-pulldown','seated-cable-row','one-arm-row'],shoulder:['machine-shoulder-press','lateral-raise','db-ohp'],bi:['cable-curl','hammer-curl'],tri:['cable-triceps','oh-triceps'],core:typeof CATALOG131!=='undefined'?['plank','crunch','dead-bug','bird-dog']:['plank','crunch']}:{leg:['db-squat','goblet-squat','reverse-lunge'],hinge:typeof CATALOG131!=='undefined'?['db-rdl','glute-bridge']:['rdl'],push:['db-floor-press','pushup'],pull:['one-arm-row'],shoulder:['db-ohp','lateral-raise'],bi:['db-curl','hammer-curl'],tri:['oh-triceps','close-pushup'],core:typeof CATALOG131!=='undefined'?['plank','crunch','dead-bug','bird-dog']:['plank','crunch']};
 const split=p.split==='auto'?(p.experience==='beginner'?'full':'upperlower'):p.split;
 const layouts=split==='full'?[['Teljes test A',['leg','push','pull','core','hinge','bi']],['Teljes test B',['hinge','push','pull','core','leg','shoulder']]]:split==='upperlower'?[['Felsőtest',['push','pull','shoulder','bi','tri']],['Alsótest és törzs',['leg','hinge','core']]]:[['Toló: mell, váll, tricepsz',['push','shoulder','tri']],['Húzó: hát, bicepsz',['pull','bi','core']],['Láb és törzs',['leg','hinge','core']]];
 const sets=p.experience==='beginner'||p.activity==='physical'||p.minutes<=30?2:3,rest=p.goal==='strength'?120:90;
 const capacity=Math.max(3,Math.min(6,Math.floor((p.minutes-4)/(sets*(0.65+rest/60)))));
 const prescriptions={},days=layouts.map(([name,slots],di)=>{const selected=slots.slice(0,capacity).map(slot=>{const options=pool[slot].filter(id=>!p.excluded.includes(id));if(!options.length)throw Error(`Ehhez a tervhez nincs megfelelő ${ {leg:'láb',hinge:'combhajlító / csípő',push:'nyomó',pull:'húzó',core:'törzs',shoulder:'váll',bi:'bicepsz',tri:'tricepsz'}[slot]} gyakorlat. Módosítsd a kizárásokat vagy állíts össze saját programot.`);return options[di%options.length];});for(const id of selected)prescriptions[id]={sets,reps:id==='plank'?'15–30 mp':p.goal==='strength'?'6–10':'8–12',weight:0,rest,trial:true};return {id:String.fromCharCode(65+di),name,exercises:[...new Set(selected)]};});
 const reasons=[p.experience==='beginner'?'Kezdőként rövid, ismételhető gyakorlatsorral indulunk.':'A terv az általad választott felosztást és felszerelést követi.',`${p.minutes} perces keret: ${sets} munkasorozat, ${rest} mp javasolt pihenő. Az időtartam közelítő.`,p.activity==='physical'?'A fizikai munka miatt alacsonyabb kezdő sorozatszámot választottunk.':'A sorozatszám a tapasztalatot és az időkeretet követi.',p.goal==='fatloss'?'A terv az erősítő edzés része a célodnak; nem ígér meghatározott fogyást vagy kalóriaégetést.':'A terhelést a teljesített ismétlések és saját visszajelzésed alapján finomítjuk.','A magasság és testsúly profiladat: ezekből nem állapítunk meg emelhető súlyt.'];
 return {id:'personal-'+crypto.randomUUID(),name:'Személyes terv – '+({full:'Teljes test',upperlower:'Felső / alsó',ppl:'Toló / húzó / láb'}[split]),location:gym?'Edzőterem':'Otthon',level:p.experience==='beginner'?'Kezdő':'Középhaladó',builtin:false,generated:true,days,prescriptions,reasons,createdAt:new Date().toISOString()};};
var previewProfile = function previewProfile(){try{const p={age:Number($('#pfAge').value),height:Number($('#pfHeight').value),weight:Number($('#pfWeight').value),goal:$('#pfGoal').value,experience:$('#pfExperience').value,activity:$('#pfActivity').value,location:$('#pfLocation')?.value||'home',...(typeof gearForm132==='function'?{gear:[...document.querySelectorAll('input[name=pfGear]:checked')].map(x=>x.value)}:{}),minutes:Number($('#pfMinutes').value),cadence:$('#pfCadence').value,split:$('#pfSplit').value,excluded:[...document.querySelectorAll('input[name=pfExclude]:checked')].map(x=>x.value)};state.profilePreview=p;state.programPreview=generatePersonalProgram(p);showPersonalPreview();}catch(e){alert(e.message);}};
var showPersonalPreview = function showPersonalPreview(){const p=state.programPreview;render(shell(`<main><button class="btn secondary" onclick="profileScreen()">← Profil</button><div class="hero"><h1>${esc(p.name)}</h1><p>Javaslat • még nincs aktiválva</p></div><div class="card">${p.reasons.map(x=>`<p>${esc(x)}</p>`).join('')}<p>Az első alkalommal könnyű próbasorozattal válassz terhelést, és rögzítsd a súlyt. A mező kezdetben üres.</p></div>${p.days.map(d=>`<div class="section">${esc(d.name)}</div>${d.exercises.map(id=>{const e=byId(id),r=p.prescriptions[id];return `<div class="card"><strong>${esc(e.hu)}</strong><p>${r.sets} × ${r.reps} • ${r.rest} mp pihenő</p><span class="badge">${MUSCLES[muscleGroups(id)[0]]}</span>${demoCard(id)}</div>`;}).join('')}`).join('')}<button class="btn block" onclick="acceptPersonalProgram()">Terv mentése és aktiválása</button></main>`));};
var acceptPersonalProgram = function acceptPersonalProgram(){if(!state.programPreview)return;const p=state.programPreview,profile=state.profilePreview;const keys=['settings','programs','activeProgramId','plannerSettings'];const old=keys.map(k=>localStorage.getItem('repforge:'+k));try{db.set('settings',{...settings(),profile});db.set('programs',[...programs(),p]);db.set('activeProgramId',p.id);db.set('plannerSettings',{...plannerSettings(),mode:profile.cadence,minutes:profile.minutes});}catch(e){keys.forEach((k,i)=>old[i]===null?localStorage.removeItem('repforge:'+k):localStorage.setItem('repforge:'+k,old[i]));alert('Nem sikerült menteni. A korábbi terv megmaradt.');return;}state.programPreview=null;state.profilePreview=null;state.tab='calendar';render();alert('A terv kész. A naptárban válaszd ki a kezdőnapot és az alkalmakat.');};
var muscleLibrary = function muscleLibrary(group='all'){render(shell(`<main><button class="btn secondary" onclick="go('programs')">← Programok</button><div class="hero"><h1>Izomcsoportok</h1><p>A fő és segédizmok szerint is szűrhetsz.</p></div><div class="muscle-chips"><button class="btn secondary" onclick="muscleLibrary()">Összes</button>${Object.entries(MUSCLES).map(([k,v])=>`<button class="btn ${k===group?'':'secondary'}" onclick="muscleLibrary('${k}')">${v}</button>`).join('')}</div>${exercises().filter(e=>group==='all'||muscleGroups(e.id).includes(group)).map(e=>`<div class="card"><h3>${esc(e.hu)}</h3><p class="small">${esc(e.equipment)} • Fő: ${MUSCLES[muscleGroups(e.id)[0]]||esc(e.target)}</p><p class="small muted">${muscleGroups(e.id).slice(1).map(g=>MUSCLES[g]).join(' / ')}</p>${demoCard(e.id)}<p>${esc(e.notes)}</p></div>`).join('')}</main>`));};
var feedbackScreen = function feedbackScreen(started){const h=history().find(x=>x.started===started);if(!h)return;state.feedbackStarted=started;render(shell(`<main><div class="hero"><h1>Edzés elmentve ✓</h1><p>${esc(h.programName||'Edzés')} • ${sessionDuration(h)}</p></div><div class="card"><h2>Milyen volt?</h2><p>A válaszodból javaslat készül; a terv nem változik automatikusan.</p><div class="muscle-chips">${[['easy','Könnyű'],['right','Megfelelő'],['hard','Túl nehéz'],['pain','Fájdalom volt']].map(([k,v])=>`<button class="btn secondary" onclick="saveFeedback('${k}')">${v}</button>`).join('')}</div></div><button class="btn secondary block" onclick="healthScreen()">Health adatok</button><br><button class="btn block" onclick="go('home')">Kezdőlap</button></main>`));};
var progressionSuggestion = function progressionSuggestion(h,rating){const complete=h.exercises.length>0&&h.exercises.every(e=>e.sets.length&&e.sets.every(s=>s.done&&Number(s.reps)>0));if(rating==='pain')return {action:'none',text:'Fájdalmat jeleztél. Nem javasolunk terhelésemelést; a következő tervből ki tudod zárni az érintett gyakorlatot.'};if(rating==='hard')return {action:'reduce',text:'A következő alkalomra gyakorlatonként egy sorozattal kevesebbet javaslunk (legalább egy marad).'};if(rating==='easy'&&complete)return {action:'reps',text:'Minden sorozat elkészült, és könnyűnek érezted. Javaslat: a következő alkalommal gyakorlatonként egy ismétléssel több (planknál 5 másodperccel több). A súly változatlan.'};return {action:'none',text:complete?'Maradjon a mostani terv és terhelés.':'Az edzés részleges vagy hiányoznak ismétlések. Egyelőre nem javasolunk emelést.'};};
var saveFeedback = function saveFeedback(rating){if(!['easy','right','hard','pain'].includes(rating))return;const h=history(),x=h.find(x=>x.started===state.feedbackStarted);if(!x)return;if(x.feedback?.applied){alert("Ehhez az edzéshez a javaslatot már alkalmaztad.");return;}const suggestion=progressionSuggestion(x,rating);x.feedback={rating,suggestion,updatedAt:Date.now()};db.set('history',h);render(shell(`<main><div class="hero"><h1>Visszajelzés mentve</h1><p>${esc(suggestion.text)}</p></div>${suggestion.action!=='none'&&programById(x.programId)?.generated?'<button class="btn block" onclick="applyProgression()">Javaslat alkalmazása a programra</button>':'<p>A gyakorlatokat a program szerkesztésénél módosíthatod.</p>'}<br><button class="btn secondary block" onclick="go('home')">Kezdőlap</button></main>`));};
var applyProgression = function applyProgression(){const hs=history(),h=hs.find(x=>x.started===state.feedbackStarted),f=h?.feedback,p=programById(h?.programId);if(!f||f.applied||!p?.generated)return;if(f.suggestion.action==='none')return;for(const e of h.exercises){const r=p.prescriptions[e.id];if(!r)continue;if(f.suggestion.action==='reduce')r.sets=Math.max(1,r.sets-1);else if(f.suggestion.action==='reps'){const n=Math.max(...e.sets.map(s=>Number(s.reps)||0));r.reps=String(Math.min(String(e.repUnit||byId(e.id)?.repUnit).includes('mp')?120:30,n+(String(e.repUnit||byId(e.id)?.repUnit).includes('mp')?5:1)))+(String(e.repUnit||byId(e.id)?.repUnit).includes('mp')?' mp':'');}}f.applied=true;const oldPrograms=programs(),oldHistory=history();try{db.set('programs',oldPrograms.map(x=>x.id===p.id?p:x));db.set('history',hs);}catch(e){db.set('programs',oldPrograms);db.set('history',oldHistory);alert('A javaslatot nem sikerült menteni.');return;}alert('A következő edzés célértékei frissültek.');go('home');};
var healthPlugin = function healthPlugin(){if(!isNative())throw Error('A Health Connect kapcsolat az Android APK-ban használható.');const p=window.Capacitor?.Plugins?.HealthBridge;if(!p)throw Error('Ebben az appváltozatban nincs Health Connect modul.');return p;};
var healthScreen = function healthScreen(){state.healthView=true;const h=state.health,items=history().filter(x=>Date.parse(x.finished)>Date.parse(x.started));render(shell(`<main><button class="btn secondary" onclick="go('settings')">← Beállítások</button><div class="hero"><h1>Óra és egészségadatok</h1><p>Samsung Health, Google Health és más Health Connect-kompatibilis alkalmazások.</p></div><div class="card"><p>Az órád saját alkalmazásában engedélyezd a Health Connect adatmegosztást. Galaxy Watch 7 esetén: óra → Samsung Health → Health Connect → RepForge.</p><p>Android 14 vagy újabb szükséges. Az adatok szinkron után érkeznek; ez nem élő pulzusmérés. Az olvasási és edzésírási engedély külön kérhető.</p><p class="small muted">Csak a kiválasztott edzés időszakának aktív kalóriáját és pulzusát olvassuk. Nem tároljuk a mentésben vagy a Google Drive-on. Az adatforrások listájához az edzésadatok olvasása is szükséges.</p><div class="grid2"><button class="btn" onclick="healthPermission()" ${h.busy?'disabled':''}>Olvasás engedélyezése</button><button class="btn secondary" onclick="healthSettings()">Engedélyek kezelése</button></div><p role="status">${esc(h.message)}</p></div>${items.length?`<div class="setting"><label>Elvégzett edzés<select id="healthWorkout" class="field">${items.map(x=>`<option value="${esc(x.started)}" ${h.workout===x.started?'selected':''}>${esc(fmtDate(x.started))} • ${esc(x.programName||x.workout)}</option>`).join('')}</select></label><label>Adatforrás<select id="healthSource" class="field">${healthSourceOptions()}</select></label><p class="small muted">Az automatikus keresés megmutatja az edzés idején adatot szolgáltató alkalmazásokat. Egyszerre egy forrást számolunk, így nincs kettős kalóriaösszegzés.</p><button class="btn block" onclick="readHealthWorkout()" ${h.busy?'disabled':''}>Órás összesítés frissítése</button><br><button class="btn secondary block" onclick="exportHealthWorkout()" ${h.busy?'disabled':''}>RepForge-edzés hozzáadása a Health Connecthez</button><p class="small muted">Csak az időtartamot és a súlyzós edzés típusát exportáljuk. Ha az óra már rögzítette az edzést, ne exportáld külön is.</p></div>`:'<p>Az első elmentett edzés után itt kérheted le az órás adatokat.</p>'}${healthSummaryHtml()}<button class="btn secondary block" onclick="clearHealthView()">Beolvasott adatok elfelejtése</button></main>`));};
var healthSummaryHtml = function healthSummaryHtml(){const s=state.health.summary;if(!s)return '';return `<div class="card"><h2>Edzéshez tartozó adatok</h2><p>${esc(healthSourceName(s.source)||'Válassz adatforrást')}</p>${(s.sources||[]).map(x=>`<button class="btn secondary source-pick" data-source="${esc(x)}">${esc(healthSourceName(x))}</button>`).join('')}<div class="grid2"><div class="stat"><small>Becsült aktív kalória</small><strong>${s.activeCalories==null?'Nincs adat':Math.round(s.activeCalories)+' kcal'}</strong></div><div class="stat"><small>Átlag / max. pulzus</small><strong>${s.averageHeartRate==null?'Nincs adat':Math.round(s.averageHeartRate)+' / '+s.maxHeartRate}</strong></div></div><p class="small muted">${s.heartRateSamples||0} pulzusminta • Nem a teljes napi kalória. Az aktív kalória az edzés időablakára vonatkozó forrásadat; az óra becslése. Frissítve: ${esc(new Date().toLocaleTimeString('hu-HU'))}</p></div>`;};
var clearHealthView = function clearHealthView(){state.health.summary=null;state.health.source='';state.health.message='A beolvasott adatok törölve a nézetből.';healthScreen();};
var healthPermission = async function healthPermission(){await healthAction(async p=>{const s=await p.requestRead();return s.granted?'Olvasás engedélyezve.':'Nem minden olvasási engedélyt adtál meg. A megadott adattípusok továbbra is olvashatók.';});};
var healthSettings = async function healthSettings(){await healthAction(async p=>{await p.openSettings();return 'Az engedélyeket a rendszer beállításaiban módosíthatod.';});};
var healthAction = async function healthAction(fn){if(state.health.busy)return;state.health.busy=true;state.health.message='Kapcsolódás…';healthScreen();try{state.health.message=await fn(healthPlugin());}catch(e){state.health.message=e.message||'A Health Connect művelet nem sikerült.';}finally{state.health.busy=false;if(state.healthView)healthScreen();}};
var selectedHealthWorkout = function selectedHealthWorkout(){const started=$('#healthWorkout')?.value;const h=history().find(x=>x.started===started);if(!h)throw Error('Válassz elmentett edzést.');state.health.workout=started;return h;};
var readHealthWorkout = async function readHealthWorkout(){let h,source;try{h=selectedHealthWorkout();source=$('#healthSource').value.trim();state.health.source=source;state.health.summary=null;}catch(e){alert(e.message);return;}await healthAction(async p=>{const result=await p.readWorkout({start:h.started,end:h.finished,source});state.health.summary=result;state.health.source=result.source;return result.source?'Az időszak lekérdezése kész. Hiányzó adatnál ellenőrizd az óraszinkront és az engedélyeket.':result.sources?.length?'Válassz a megtalált források közül, majd frissítsd az összesítést.':'Ehhez az időszakhoz nem találtunk megosztott adatot. Szinkronizáld az órát, majd próbáld újra.';});};
var exportHealthWorkout = async function exportHealthWorkout(){let h;try{h=selectedHealthWorkout();}catch(e){alert(e.message);return;}await healthAction(async p=>{const permission=await p.requestWrite();if(!permission.granted)return 'Az edzésírási engedély nincs megadva.';await p.writeWorkout({start:h.started,end:h.finished,title:(h.programName||'RepForge')+' • '+(h.dayId||h.workout),clientId:'repforge:'+h.started});return 'Edzés exportálva. Az ismételt export nem hoz létre új RepForge-bejegyzést.';});};
var healthSourceName = function healthSourceName(source){return source==='com.sec.android.app.shealth'?'Samsung Health':(state.health.summary?.sourceLabels?.[source]||source);};
var healthSourceOptions = function healthSourceOptions(){const sources=[...new Set(['com.sec.android.app.shealth',...(state.health.summary?.sources||[]),...(state.health.source?[state.health.source]:[])])];return `<option value="" ${!state.health.source?'selected':''}>Automatikus keresés</option>`+sources.map(x=>`<option value="${esc(x)}" ${x===state.health.source?'selected':''}>${esc(healthSourceName(x))}</option>`).join('');};
var rfHistoryHealthCache=new Map();
var rfHistoryHealthWindow=function rfHistoryHealthWindow(h){if(!h||!h.started||!h.finished)return '';try{const a=new Date(h.started),b=new Date(h.finished),f=x=>x.toLocaleTimeString('hu-HU',{hour:'2-digit',minute:'2-digit'});return `${f(a)}–${f(b)} • ${sessionDuration(h)}`;}catch(_){return sessionDuration(h)||'';}};
var rfHistoryHealthNumber=function rfHistoryHealthNumber(v,d=0){const n=Number(v);return Number.isFinite(n)?n.toLocaleString('hu-HU',{minimumFractionDigits:d,maximumFractionDigits:d}):null;};
var rfHistoryHealthHtml=function rfHistoryHealthHtml(i){
 const h=history()[i];if(!h)return '';
 const q=rfHistoryHealthCache.get(h.started),windowText=rfHistoryHealthWindow(h);
 if(!q)return `<div class="rf-history-health-empty"><p class="small muted">Health Connect. A blokk csak ennek az edzésnek az időablakát olvassa ki.</p></div>`;
 if(q.loading)return `<div class="rf-history-health-loading"><p class="small muted">Health Connect lekérdezés…</p></div>`;
 if(q.error)return `<div class="rf-history-health-error"><p class="small">${esc(q.error)}</p></div>`;
 const x=q.summary||{},stats=[];
 const active=rfHistoryHealthNumber(x.activeCalories),total=rfHistoryHealthNumber(x.totalCalories),avg=rfHistoryHealthNumber(x.averageHeartRate),max=rfHistoryHealthNumber(x.maxHeartRate),dist=Number(x.distanceMeters),mins=rfHistoryHealthNumber(x.exerciseMinutes),sessions=rfHistoryHealthNumber(x.exerciseSessionCount);
 if(active!==null)stats.push(['Aktív kalória',active+' kcal']);
 if(total!==null)stats.push(['Összes energia',total+' kcal']);
 if(avg!==null||max!==null)stats.push(['Átlag / max. pulzus',(avg??'–')+' / '+(max??'–')+' bpm']);
 if(Number.isFinite(dist))stats.push(['Távolság',dist>=1000?rfHistoryHealthNumber(dist/1000,2)+' km':rfHistoryHealthNumber(dist)+' m']);
 if(mins!==null)stats.push(['Health edzésidő',mins+' perc']);
 if(sessions!==null)stats.push(['Edzésrekord',sessions+' db']);
 const src=x.source==='all'?'Health Connect • összes forrás':(x.sourceLabels?.[x.source]||x.source||'Health Connect');
 const loaded=q.loadedAt?new Date(q.loadedAt).toLocaleTimeString('hu-HU',{hour:'2-digit',minute:'2-digit'}):'';
 return `<div class="rf-history-health-result">${stats.length?`<div class="rf-history-health-grid">${stats.map(([k,v])=>`<div class="stat"><small>${esc(k)}</small><strong>${esc(v)}</strong></div>`).join('')}</div>`:'<p class="small muted">Ehhez az edzés-időablakhoz nem találtunk megosztott Health Connect adatot.</p>'}<p class="small muted">${esc(src)}${x.heartRateSamples!=null?' • '+esc(String(x.heartRateSamples))+' pulzusminta':''}${loaded?' • frissítve '+esc(loaded):''}</p></div>`;
};
var rfHistoryHealthPaint=function rfHistoryHealthPaint(i){const el=document.querySelector(`[data-rf-history-health="${i}"]`);if(el)el.innerHTML=rfHistoryHealthHtml(i);};
var rfHistoryHealthEnsure=function rfHistoryHealthEnsure(i){const h=history()[i];if(!h||rfHistoryHealthCache.has(h.started))return;healthFromHistory(i);};
var rfHistoryHealthToggle=function rfHistoryHealthToggle(i){const p=document.querySelector(`[data-rf-history-health-panel="${i}"]`);if(!p)return;const body=p.querySelector('.rf-history-health-body'),btn=p.querySelector('.rf-history-health-toggle'),closed=p.classList.toggle('collapsed');if(body)body.hidden=closed;if(btn)btn.setAttribute('aria-expanded',closed?'false':'true');};
var healthFromHistory = async function healthFromHistory(i){
 const h=history()[i];if(!h)return;
 if(!h.finished||!(Date.parse(h.finished)>Date.parse(h.started))){rfHistoryHealthCache.set(h.started,{error:'Ehhez az edzéshez nincs érvényes befejezési idő.'});rfHistoryHealthPaint(i);return;}
 rfHistoryHealthCache.set(h.started,{loading:true});rfHistoryHealthPaint(i);
 try{
  const result=await healthPlugin().readWorkout({start:h.started,end:h.finished,source:''});
  rfHistoryHealthCache.set(h.started,{summary:result||{},loadedAt:Date.now()});
 }catch(e){rfHistoryHealthCache.set(h.started,{error:e?.message||'A Health Connect adatok lekérése nem sikerült.'});}
 rfHistoryHealthPaint(i);
};
var feedbackFromHistory = function feedbackFromHistory(i){const h=history()[i];if(h?.feedback?.applied){alert(h.feedback.suggestion.text+' A javaslatot már alkalmaztad.');return;}if(h)feedbackScreen(h.started);};
/* RepForge 1.3: local rule-based onboarding, transparent prescriptions and Health Connect. */
const MUSCLES={chest:'Mell',back:'Hát',shoulders:'Váll',biceps:'Bicepsz',triceps:'Tricepsz',legs:'Láb / far',core:'Törzs'};
const MUSCLE_MAP={
'db-squat':['legs','core'],'goblet-squat':['legs','core'],'bulgarian-split-squat':['legs','core'],'reverse-lunge':['legs','core'],rdl:['legs','back'],
'db-floor-press':['chest','triceps','shoulders'],pushup:['chest','triceps','core'],'close-pushup':['triceps','chest'],
'one-arm-row':['back','biceps'],'barbell-row':['back','biceps'],'db-pullover':['back','chest'],'lat-pulldown':['back','biceps'],'seated-cable-row':['back','biceps'],
'db-ohp':['shoulders','triceps'],'lateral-raise':['shoulders'],'machine-shoulder-press':['shoulders','triceps'],
'db-curl':['biceps'],'hammer-curl':['biceps'],'cable-curl':['biceps'],'oh-triceps':['triceps'],'cable-triceps':['triceps'],
plank:['core'],crunch:['core'],'leg-press':['legs'],'leg-curl':['legs'],'machine-chest-press':['chest','triceps','shoulders']};
if(typeof CATALOG131!=='undefined')for(const row of CATALOG131)MUSCLE_MAP[row[0]]=row[5];


const rf13Home=home;home=function(){return rf13Home().replace('<main>',`<main><div class="card onboarding"><h2>${trainingProfile()?'Személyes edzéstervező':'Nem tudod, hogyan kezdd?'}</h2><p>Állítsunk össze egy hozzád illő, követhető tervet.</p><button class="btn block" onclick="profileScreen()">${trainingProfile()?'Profil és új terv':'Segíts elkezdeni'}</button></div>`);};
const rf13Programs=programsScreen;programsScreen=function(){return rf13Programs().replace('<main>','<main><div class="grid2"><button class="btn" onclick="profileScreen()">Személyes terv</button><button class="btn secondary" onclick="muscleLibrary()">Izomcsoportok</button></div>');};
const rf13Settings=settingsScreen;settingsScreen=function(){return rf13Settings().replace('<main>','<main><div class="grid2"><button class="btn" onclick="profileScreen()">Személyes profil</button><button class="btn secondary" onclick="healthScreen()">Óra és egészségadatok</button></div>');};
saveSettings=function(){db.set('settings',{...settings(),rest:Math.max(30,Math.min(300,Number($('#rest').value)||90))});alert('Mentve.');};
// Keep prescriptions in the program/session, never overwrite another program's exercise defaults.
const rf13Start=startWorkout;startWorkout=function(dayId,scheduleId=null,programId=null){const before=state.session;state.prescriptionProgramId=programId||activeProgramId();try{rf13Start(dayId,scheduleId,programId);}finally{state.prescriptionProgramId=null;}if(!state.session||state.session===before)return;const p=programById(state.session.programId);if(p?.prescriptions){state.session.exercises=state.session.exercises.map(e=>{const rx=p.prescriptions[e.id];if(!rx)return e;const previous=history().filter(h=>h.programId===p.id).flatMap(h=>h.exercises).find(x=>x.id===e.id);return {...e,prescription:{...rx},sets:Array.from({length:rx.sets},(_,i)=>({set:i+1,weight:previous?.sets[i]?.weight??(e.loadType==='bodyweight'?0:''),reps:'',done:false}))};});renderWorkout();}};
const rf13Rest=startRest;startRest=function(){rf13Rest();const rx=state.session?.exercises[state.current]?.prescription;if(rx){state.timer=rx.rest;state.restEndAt=Date.now()+state.timer*1000;}};
// Finish only marks the calendar after the history write really succeeded.
finishWorkout=function(){if(!state.session)return;const s=state.session;rf12FinishWorkout();if(state.session)return;if(s.scheduleId){const items=scheduled(),i=items.findIndex(x=>x.id===s.scheduleId);if(i>=0){items[i]={...items[i],status:'completed',updatedAt:Date.now()};db.set('scheduled',items);}}feedbackScreen(s.started);};


// Imported Health Connect records live only in memory and are excluded from backup/Drive.
state.health={status:null,summary:null,message:'',busy:false,source:''};


document.addEventListener('click',e=>{const b=e.target.closest?.('.source-pick');if(b){state.health.source=b.dataset.source;healthScreen();}});
const rf13Backup=makeBackup;makeBackup=function(){return {...rf13Backup(),appVersion:'1.3.1'};};


const rf13Go=go;go=function(t){state.healthView=false;rf13Go(t);};
const rf13History=historyScreen;historyScreen=function(){return rf13History().replace(/<button class="btn secondary" onclick="calendarIntent\((\d+)\)">Hozzáadás a naptárhoz<\/button>/g,(html,i)=>{const h=history()[Number(i)],rating={easy:'Könnyű',right:'Megfelelő',hard:'Túl nehéz',pain:'Fájdalom volt'}[h.feedback?.rating];return html+`<button class="btn secondary" onclick="healthFromHistory(${i})">Health adatok</button><button class="btn secondary" onclick="feedbackFromHistory(${i})">Visszajelzés</button>${rating?`<p class="small">Visszajelzés: ${rating}${h.feedback.applied?' • javaslat alkalmazva':''}</p>`:''}`;});};


const rf13ValidateBackup=validateBackup;validateBackup=function(d){const out=rf13ValidateBackup(d);if(d.settings?.profile)validateProfile(d.settings.profile);for(const p of d.programs||[]){if(p.prescriptions){if(typeof p.prescriptions!=='object'||Array.isArray(p.prescriptions))throw Error('Hibás személyes terv.');for(const [id,r] of Object.entries(p.prescriptions)){if(!MUSCLE_MAP[id]||!r||!Number.isInteger(r.sets)||r.sets<1||r.sets>10||typeof r.reps!=='string'||r.reps.length>40||!Number.isFinite(r.rest)||r.rest<30||r.rest>300||!Number.isFinite(r.weight)||r.weight<0||r.weight>1000)throw Error('Hibás személyes terhelési érték a mentésben.');}}}return out;};
render();


// @endsection v13.js

// @section library131.js
var libraryGear = function libraryGear(e){if(e.gear)return e.gear;if(e.loadType==='bodyweight')return 'bodyweight';if(/rúd/.test(e.equipment))return 'barbell';if(/gép|csiga|lehúzó/.test(e.equipment))return 'machine';return 'dumbbells';};
var libraryMatch = function libraryMatch(e,group,gear,q){const normal=x=>String(x).normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();return (group==='all'||muscleGroups(e.id).includes(group))&&(gear==='all'||libraryGear(e)===gear)&&normal(e.hu+' '+e.en+' '+e.equipment).includes(normal(q.trim()));};
var libraryResults = function libraryResults(group,gear,q){const es=exercises().filter(e=>libraryMatch(e,group,gear,q));return `<p class="muted">${es.length} / ${exercises().length} gyakorlat</p>${es.map(e=>`<div class="card"><h3>${esc(e.hu)}</h3><p class="small">${esc(e.equipment)} • Fő izomcsoport: ${esc(MUSCLES[muscleGroups(e.id)[0]]||e.target)}</p><p class="small muted">${muscleGroups(e.id).slice(1).map(x=>MUSCLES[x]).join(' / ')}</p>${demoCard(e.id)}<p>${esc(e.notes)}</p><button class="btn secondary block" onclick="chooseLibraryPlacement('${e.id}')">Hozzáadás saját programhoz</button></div>`).join('')||'<p>Nincs találat. Próbálj másik szűrőt vagy keresőkifejezést.</p>'}`;};
var filterLibrary = function filterLibrary(){const group=$('#libraryMuscle').value,gear=$('#libraryGear').value,q=$('#libraryQuery').value;state.libraryFilter={group,gear,q};$('#libraryResults').innerHTML=libraryResults(group,gear,q);};
var chooseLibraryPlacement = function chooseLibraryPlacement(id){if(!byId(id))return;state.libraryExercise=id;state.libraryTargets=programs().filter(p=>!p.builtin).flatMap(p=>p.days.map(d=>({programId:p.id,dayId:d.id,label:p.name+' • '+d.name})));render(shell(`<main><button class="btn secondary" onclick="muscleLibrary()">← Könyvtár</button><div class="hero"><h1>${esc(byId(id).hu)}</h1><p>Melyik saját programnaphoz adod hozzá?</p></div>${state.libraryTargets.length?`<select id="libraryTarget" class="field">${state.libraryTargets.map((t,i)=>`<option value="${i}">${esc(t.label)}</option>`).join('')}</select><br><button class="btn block" onclick="saveLibraryPlacement()">Hozzáadás</button>`:'<p>Előbb hozz létre saját vagy személyes programot.</p><button class="btn block" onclick="go(\'programs\')">Programok megnyitása</button>'}</main>`));};
var appendLibraryExercise = function appendLibraryExercise(programId,dayId,id){const ps=programs(),p=ps.find(x=>x.id===programId),d=p?.days.find(x=>x.id===dayId),e=exercises().find(x=>x.id===id);if(!p||p.builtin||!d||!e)throw Error('Nem szerkeszthető programnap.');if(d.exercises.includes(id))throw Error('Ez a gyakorlat már szerepel ezen a napon.');if(d.exercises.length>=20)throw Error('Egy programnap legfeljebb 20 gyakorlatot tartalmazhat.');d.exercises.push(id);if(p.generated){p.prescriptions=p.prescriptions||{};p.prescriptions[id]={sets:2,reps:e.reps,rest:90,weight:0,trial:true};}db.set('programs',ps);};
var saveLibraryPlacement = function saveLibraryPlacement(){const target=state.libraryTargets?.[Number($('#libraryTarget').value)];if(!target)return;try{appendLibraryExercise(target.programId,target.dayId,state.libraryExercise);alert('Gyakorlat hozzáadva.');muscleLibrary();}catch(e){alert(e.message);}};
/* Search and program placement for the expanded catalog. */


muscleLibrary=function(group='all'){state.libraryFilter={group,gear:'all',q:''};render(shell(`<main><button class="btn secondary" onclick="go('programs')">← Programok</button><div class="hero"><h1>Gyakorlatkönyvtár</h1><p>${exercises().length} gyakorlat • saját testsúly, kézisúlyzó, rúd és gépek</p></div><div class="setting"><label>Keresés<input id="libraryQuery" class="field" type="search" placeholder="Gyakorlat vagy felszerelés" oninput="filterLibrary()"></label><label>Izomcsoport<select id="libraryMuscle" class="field" onchange="filterLibrary()"><option value="all">Összes</option>${Object.entries(MUSCLES).map(([k,v])=>`<option value="${k}" ${group===k?'selected':''}>${v}</option>`).join('')}</select></label><label>Felszerelés<select id="libraryGear" class="field" onchange="filterLibrary()">${Object.entries({all:'Összes',bodyweight:'Saját testsúly',dumbbells:'Kézisúlyzó',barbell:'Kétkezes rúd',machine:'Gép / csiga'}).map(([k,v])=>`<option value="${k}">${v}</option>`).join('')}</select></label></div><div id="libraryResults">${libraryResults(group,'all','')}</div></main>`));};


render();


// @endsection library131.js

// @section onboarding132.js
var profileGear132 = function profileGear132(p){return Array.isArray(p.gear)?p.gear:p.location==='gym'?Object.keys(GEAR132):['dumbbells','support'];};
var gearForm132 = function gearForm132(p){const selected=profileGear132(p);return `<div class="setting"><h2>Mivel edzenél?</h2><p>A saját testsúlyos gyakorlatok mindig használhatók. Jelöld az elérhető eszközöket; több is választható.</p><div class="muscle-chips"><button type="button" class="btn secondary" onclick="gearPreset132('bodyweight')">Csak saját testsúly</button><button type="button" class="btn secondary" onclick="gearPreset132('dumbbells')">Kézisúlyzó + testsúly</button><button type="button" class="btn secondary" onclick="gearPreset132('mixed')">Gépek + szabad súlyok</button></div>${Object.entries(GEAR132).map(([k,v])=>`<label class="exclude-row"><input name="pfGear" type="checkbox" value="${k}" ${selected.includes(k)?'checked':''}>${v}</label>`).join('')}<p class="small muted">Kombinálhatod őket, például kézisúlyzó + pad vagy rúd + kézisúlyzó. A gépeknél a hiányzó konkrét gyakorlatokat lent kizárhatod. Gumiszalagos és húzódzkodórudas terv még nincs ebben a könyvtárban.</p></div>`;};
var gearPreset132 = function gearPreset132(preset){const chosen=preset==='bodyweight'?[]:preset==='dumbbells'?['dumbbells','support']:Object.keys(GEAR132);document.querySelectorAll('input[name=pfGear]').forEach(x=>x.checked=chosen.includes(x.value));};
var available132 = function available132(id,p){return Object.hasOwn(NEEDS132,id)&&NEEDS132[id].every(k=>profileGear132(p).includes(k)||(k==='dumbbell'&&profileGear132(p).includes('dumbbells')))&&!p.excluded.includes(id);};
/* Equipment combinations. Legacy location remains only as a backward-compatible stored field. */
const GEAR132={dumbbell:'1 kézisúlyzó',dumbbells:'2 kézisúlyzó',barbell:'Kétkezes rúd',bench:'Edzőpad',support:'Stabil támasz az evezéshez',step:'Stabil fellépő',machines:'Edzőgépek és csigák'};
const NEEDS132={
'db-squat':['dumbbells'],'goblet-squat':['dumbbell'],'reverse-lunge':['dumbbells'],'bulgarian-split-squat':['dumbbells','bench'],
'db-floor-press':['dumbbells'],'one-arm-row':['dumbbell','support'],rdl:['barbell'],'barbell-row':['barbell'],
'db-curl':['dumbbells'],'db-ohp':['dumbbells'],'oh-triceps':['dumbbell'],'db-pullover':['dumbbell'],
'lateral-raise':['dumbbells'],'hammer-curl':['dumbbells'],'db-rdl':['dumbbells'],'db-step-up':['dumbbells','step'],
'db-bench-press':['dumbbells','bench'],'incline-db-press':['dumbbells','bench'],'db-reverse-fly':['dumbbells'],
'leg-press':['machines'],'leg-curl':['machines'],'leg-extension':['machines'],'machine-chest-press':['machines'],'machine-shoulder-press':['machines'],'lat-pulldown':['machines'],'seated-cable-row':['machines'],'cable-curl':['machines'],'cable-triceps':['machines'],'face-pull':['machines'],'cable-fly':['machines'],
pushup:[],plank:[],crunch:[],'close-pushup':[],'bodyweight-squat':[],'glute-bridge':[],'dead-bug':[],'bird-dog':[],'side-plank':[],'calf-raise':['support']};


const validateProfileBefore132=validateProfile;
validateProfile=function(p){if(p?.gear!==undefined&&(!Array.isArray(p.gear)||p.gear.some(x=>!Object.hasOwn(GEAR132,x))||new Set(p.gear).size!==p.gear.length))throw Error('Érvénytelen felszereléslista.');return validateProfileBefore132({...p,location:p?.location||'home'});};

generatePersonalProgram=function(input){const p=validateProfile(input),gear=profileGear132(p),pools={leg:['leg-press','db-squat','goblet-squat','reverse-lunge','bodyweight-squat'],hinge:['leg-curl','db-rdl','rdl','glute-bridge'],push:['machine-chest-press','db-bench-press','incline-db-press','db-floor-press','pushup'],pull:['lat-pulldown','seated-cable-row','barbell-row','one-arm-row'],shoulder:['machine-shoulder-press','db-ohp','lateral-raise','db-reverse-fly'],bi:['cable-curl','db-curl','hammer-curl'],tri:['cable-triceps','oh-triceps','close-pushup'],core:['plank','dead-bug','crunch','side-plank'],stability:['bird-dog','dead-bug']};
 for(const k of Object.keys(pools))pools[k]=pools[k].filter(id=>available132(id,p));
 const hasPull=pools.pull.length>0,split=p.split==='auto'?(p.experience==='beginner'||!hasPull?'full':'upperlower'):p.split;
 if(split==='ppl'&&!hasPull)throw Error('A toló/húzó/láb felosztáshoz húzógyakorlat is kell. Jelölj rudat, gépeket vagy kézisúlyzót stabil támasszal; eszköz nélkül válassz teljes testes vagy felső/alsó alapot.');
 let layouts;
 if(split==='full')layouts=hasPull?[['A',['leg','push','pull','core','hinge','shoulder']],['B',['hinge','push','pull','core','leg','tri']]]:[['A',['leg','push','hinge','core','stability']],['B',['hinge','push','leg','stability','core']]];
 else if(split==='upperlower')layouts=hasPull?[['Felsőtest',['push','pull','shoulder','bi','tri']],['Alsótest és törzs',['leg','hinge','core','stability']]]:[['Felsőtest és törzs',['push','core','stability']],['Alsótest és törzs',['leg','hinge','core']]];
 else layouts=[['Toló',['push','shoulder','tri']],['Húzó',['pull','bi','core']],['Láb és törzs',['leg','hinge','core']]];
 const sets=p.experience==='beginner'||p.activity==='physical'||p.minutes<=30?2:3,rest=p.goal==='strength'?120:90,capacity=Math.max(3,Math.min(6,Math.floor((p.minutes-4)/(sets*(0.65+rest/60))))),prescriptions={};
 const days=layouts.map(([name,roles],di)=>{const ids=[];for(const role of roles.slice(0,capacity)){let options=pools[role].filter(id=>!ids.includes(id));if(!options.length){if(['shoulder','bi','tri'].includes(role))options=pools.core.filter(id=>!ids.includes(id));if(!options.length)throw Error('A kizárásokkal és felszerelésekkel nincs elég különböző gyakorlat ehhez a naphoz. Módosítsd a kizárásokat vagy válassz más felosztást.');}const id=options[di%options.length],e=exercises().find(x=>x.id===id),timed=e.repUnit.includes('mp');ids.push(id);prescriptions[id]={sets,reps:timed?'15–30 mp':p.goal==='strength'&&e.loadType!=='bodyweight'?'6–10':'8–12',weight:0,rest,trial:true};}return {id:String.fromCharCode(65+di),name,exercises:ids};});
 const equipment=gear.length?gear.map(x=>GEAR132[x]).join(' + '):'Saját testsúly';
 const reasons=[`Elérhető: ${equipment}. A terv csak ezekkel végezhető gyakorlatokat választ.`,`${p.minutes} perces keret: ${sets} munkasorozat, ${rest} mp javasolt pihenő. Az időtartam közelítő.`,p.activity==='physical'?'A fizikai munka mellett alacsonyabb kezdő sorozatszámot választottunk.':'A kezdő sorozatszám a tapasztalatot és az időkeretet követi.','A magasság és testsúly nem határoz meg biztonságosan emelhető kezdősúlyt. A terhelést próbasorozattal és visszajelzéssel állítsd be.'];
 if(!hasPull)reasons.push('Ez húzóeszköz nélküli alapozó terv. A bird dog törzsstabilizáló gyakorlat, nem helyettesíti az evezést vagy a lehúzást. A hát húzóterhelését később megfelelő felszereléssel lehet bővíteni.');
 if(p.goal==='fatloss')reasons.push('A terv az erősítő edzést támogatja; nem ígér meghatározott fogyást vagy kalóriaégetést.');
 return {id:'personal-'+crypto.randomUUID(),name:(gear.length?'Kombinált':'Saját testsúlyos')+' terv – '+({full:'A/B',upperlower:'Felső / alsó',ppl:'Toló / húzó / láb'}[split]),location:gear.length?'Választott felszerelés':'Saját testsúly',level:p.experience==='beginner'?'Kezdő':'Középhaladó',builtin:false,generated:true,gear:[...gear],days,prescriptions,reasons,createdAt:new Date().toISOString()};};
const backupBefore132=makeBackup;makeBackup=function(){return {...backupBefore132(),appVersion:'1.3.2'};};
render();


// @endsection onboarding132.js

// @section v14.js
var rf14Goals = function rf14Goals(id){const p=RF14_PATTERNS[id]||'';if(['elbow-flexion','elbow-extension','shoulder-isolation','rear-delt','chest-isolation','knee-extension','knee-flexion','calf'].includes(p))return ['muscle','fitness','fatloss'];return ['fitness','muscle','strength','fatloss'];};
var rf14Meta = function rf14Meta(e){if(!e)return e;const custom=e.custom===true;const pattern=e.movementPattern||RF14_PATTERNS[e.id]||'other',complexity=e.complexity||(RF14_ADVANCED.has(e.id)?'medium':'low'),difficulty=e.difficulty||(RF14_ADVANCED.has(e.id)?'intermediate':'beginner');return {...e,movementPattern:pattern,difficulty,complexity,beginnerSafe:e.beginnerSafe??(!RF14_ADVANCED.has(e.id)),compound:e.compound??RF14_COMPOUND.has(e.id),goalTags:e.goalTags||rf14Goals(e.id),alternatives:e.alternatives||[],custom,media:e.media||null};};
var rf14Alternatives = function rf14Alternatives(id,p){const src=rf14Meta(exercises().find(e=>e.id===id));if(!src)return[];return exercises().filter(e=>e.id!==id&&e.movementPattern===src.movementPattern&&(!p||typeof available132!=='function'||available132(e.id,p))).sort((a,b)=>Number(RF14_BEGINNER_FAVOR.has(b.id))-Number(RF14_BEGINNER_FAVOR.has(a.id)));};
var rf14Score = function rf14Score(id,p,role){const e=rf14Meta(exercises().find(x=>x.id===id));if(!e)return-999;let s=0;if(e.goalTags.includes(p.goal))s+=4;if(p.experience==='beginner'){s+=e.beginnerSafe?4:-10;s+=RF14_BEGINNER_FAVOR.has(id)?2:0;}else if(e.difficulty==='intermediate')s+=2;if(e.compound&&['leg','hinge','push','pull','shoulder'].includes(role))s+=2;if(p.activity==='physical'&&e.complexity!=='low')s-=2;return s;};
var rf14Pick = function rf14Pick(pool,p,role,used,dayIndex){return pool.filter(id=>!used.includes(id)).sort((a,b)=>rf14Score(b,p,role)-rf14Score(a,p,role)||a.localeCompare(b))[dayIndex%Math.max(1,pool.filter(id=>!used.includes(id)).length)];};
var rf14TargetTop = function rf14TargetTop(rx){const m=String(rx?.reps||'').match(/(\d+)\s*[–-]\s*(\d+)/);return m?Number(m[2]):null;};
var rf14ProgressionForExercise = function rf14ProgressionForExercise(h,e){const p=programById(h.programId),rx=p?.prescriptions?.[e.id];if(!rx)return{action:'none'};const done=(e.sets||[]).filter(s=>s.done);if(done.length!==(e.sets||[]).length||!done.length)return{action:'none'};const top=rf14TargetTop(rx),minRep=Math.min(...done.map(s=>Number(s.reps)||0));if(!top||minRep<top)return{action:'reps',step:1};const load=Math.max(...done.map(s=>Number(s.weight)||0));if(load<=0||byId(e.id)?.loadType==='bodyweight')return{action:'reps',step:1};const step=byId(e.id)?.loadType==='per_hand'||byId(e.id)?.loadType==='single_dumbbell'?0.5:2.5;return{action:'load',step,next:Math.round((load+step)*10)/10};};
var rf14ProgressionSummary = function rf14ProgressionSummary(h,rating){if(rating==='pain')return{action:'none',text:'Fájdalomjelzés miatt nincs automatikus terhelésemelés.'};if(rating==='hard')return{action:'reduce',text:'Túl nehéz volt: tartsd a terhelést vagy csökkents egy sorozatot.'};if(rating!=='easy'&&rating!=='right')return{action:'none',text:'A terv változatlan.'};const items=(h.exercises||[]).map(e=>({id:e.id,...rf14ProgressionForExercise(h,e)})).filter(x=>x.action!=='none');return{action:items.some(x=>x.action==='load')?'smart':'reps',items,text:items.some(x=>x.action==='load')?'A felső ismétléshatárt teljesítő gyakorlatoknál kis súlynövelés javasolt; a többinél előbb ismétlésben haladj.':'Előbb az ismétléstartomány felső határa felé haladj.'};};
var rf14WeeklyWeights = function rf14WeeklyWeights(){const map={};for(const x of weights()){const d=new Date(x.date);if(Number.isNaN(d.getTime()))continue;const monday=new Date(d);monday.setHours(0,0,0,0);monday.setDate(d.getDate()-((d.getDay()+6)%7));const k=monday.toISOString().slice(0,10);(map[k]??=[]).push(Number(x.kg));}return Object.entries(map).sort((a,b)=>b[0].localeCompare(a[0])).map(([week,a])=>({week,avg:a.reduce((s,x)=>s+x,0)/a.length,count:a.length}));};
var rf14WorkoutVolume = function rf14WorkoutVolume(h){let total=0;for(const e of h.exercises||[])for(const s of e.sets||[])if(s.done)total+=(Number(s.weight)||0)*(Number(s.reps)||0);return Math.round(total*10)/10;};
var rf14MuscleVolume = function rf14MuscleVolume(){const out={};for(const h of history()){for(const e of h.exercises||[]){const groups=typeof muscleGroups==='function'?muscleGroups(e.id):[];const v=(e.sets||[]).filter(s=>s.done).reduce((z,s)=>z+(Number(s.weight)||0)*(Number(s.reps)||0),0);for(const g of groups)out[g]=(out[g]||0)+v;}}return out;};
var statsScreen = function statsScreen(){const hs=history(),wa=rf14WeeklyWeights(),vol=hs.reduce((s,h)=>s+rf14WorkoutVolume(h),0),mv=rf14MuscleVolume();render(shell(`<main><button class="btn secondary" onclick="go('home')">← Vissza</button><div class="hero"><h1>Statisztikák</h1><p>Edzés- és testsúlytrendek a helyi naplóból.</p></div><div class="grid2"><div class="stat"><small>Edzések</small><strong>${hs.length}</strong></div><div class="stat"><small>Rögzített volumen</small><strong>${Math.round(vol)} kg·ism.</strong></div></div><div class="section">Heti testsúlyátlag</div>${wa.length?wa.slice(0,8).map(x=>`<div class="weight-row"><strong>${x.avg.toFixed(1)} kg</strong><div class="small muted">${x.week} • ${x.count} mérés</div></div>`).join(''):'<p class="muted">Még nincs elég testsúlyadat.</p>'}<div class="section">Volumen izomcsoport szerint</div>${Object.entries(mv).sort((a,b)=>b[1]-a[1]).map(([k,v])=>`<div class="weight-row"><strong>${esc((typeof MUSCLE_LABELS!=='undefined'&&MUSCLE_LABELS[k])||k)}</strong><div>${Math.round(v)} kg·ism.</div></div>`).join('')||'<p class="muted">Még nincs adat.</p>'}</main>`));};
var rf14RecoveryAdvice = function rf14RecoveryAdvice(){const hs=history().slice(0,4);if(!hs.length)return'Nincs még elég edzésadat.';const pain=hs.some(h=>h.feedback?.rating==='pain'),hard=hs.filter(h=>h.feedback?.rating==='hard').length;const recent=hs.filter(h=>Date.now()-Date.parse(h.finished||h.started)<48*3600000).length;if(pain)return'Korábbi fájdalomjelzés van: ne emelj automatikusan terhelést; szükség esetén hagyd ki az érintett gyakorlatot.';if(hard>=2||recent>=2)return'Az utóbbi terhelés magasabbnak tűnik. A következő alkalommal maradj a jelenlegi súlynál vagy válassz könnyebb napot.';return'Az edzésnapló alapján nincs külön terheléscsökkentési jelzés. Ez nem orvosi vagy regenerációs diagnózis.';};
var customExerciseScreen = function customExerciseScreen(){render(shell(`<main><button class="btn secondary" onclick="go('programs')">← Vissza</button><div class="hero"><h1>Saját gyakorlat</h1><p>Adj hozzá saját gyakorlatot a könyvtárhoz.</p></div><div class="setting"><label>Magyar név<input id="ceHu" class="field" maxlength="80"></label><label>Angol név<input id="ceEn" class="field" maxlength="80"></label><label>Felszerelés<input id="ceEq" class="field" maxlength="80" value="testsúly"></label><label>Mozgásminta<select id="cePattern" class="field">${['squat','hinge','lunge','horizontal-push','horizontal-pull','vertical-push','vertical-pull','core-bracing','core-stability','elbow-flexion','elbow-extension','other'].map(x=>`<option>${x}</option>`).join('')}</select></label><label>Nehézség<select id="ceDifficulty" class="field"><option value="beginner">Kezdő</option><option value="intermediate">Középhaladó</option></select></label><label>Technikai jegyzet<textarea id="ceNotes" class="field" maxlength="500"></textarea></label><button class="btn block" onclick="saveCustomExercise14()">Gyakorlat mentése</button></div></main>`));};
var saveCustomExercise14 = function saveCustomExercise14(){const hu=$('#ceHu').value.trim(),en=$('#ceEn').value.trim();if(hu.length<2||en.length<2){alert('Adj meg magyar és angol nevet.');return;}const id='custom-'+crypto.randomUUID(),e={id,hu,en,equipment:$('#ceEq').value.trim()||'testsúly',target:'Egyéni',notes:$('#ceNotes').value.trim(),sets:2,reps:'8–12',weight:0,loadType:'bodyweight',repUnit:'ism.',movementPattern:$('#cePattern').value,difficulty:$('#ceDifficulty').value,beginnerSafe:$('#ceDifficulty').value==='beginner',complexity:$('#ceDifficulty').value==='beginner'?'low':'medium',goalTags:['fitness','muscle'],custom:true};db.set('exercises',[...exercises(),e]);alert('Saját gyakorlat hozzáadva.');go('programs');};
var deleteCustomExercise14 = function deleteCustomExercise14(id){const e=exercises().find(x=>x.id===id);if(!e?.custom||!confirm('Törlöd ezt a saját gyakorlatot? A korábbi naplóbejegyzéseket nem módosítjuk.'))return;db.set('exercises',exercises().filter(x=>x.id!==id));const ps=programs().map(p=>({...p,days:p.days.map(d=>({...d,exercises:d.exercises.filter(x=>x!==id)}))}));db.set('programs',ps);render();};
var moveCustomExercise14 = function moveCustomExercise14(programId,dayIndex,exerciseIndex,delta){const ps=programs(),p=ps.find(x=>x.id===programId);if(!p||p.builtin)return;const a=p.days?.[dayIndex]?.exercises;if(!a)return;const j=exerciseIndex+delta;if(j<0||j>=a.length)return;[a[exerciseIndex],a[j]]=[a[j],a[exerciseIndex]];db.set('programs',ps);editCustomProgram(programId);};
/* RepForge 1.4.0: Exercise Engine 2.0, smarter planning, progression, recovery advice, statistics and custom exercises. */
const RF14_VERSION='1.4.0';
const RF14_PATTERNS={
 'db-squat':'squat','goblet-squat':'squat','bodyweight-squat':'squat','leg-press':'squat','reverse-lunge':'lunge','bulgarian-split-squat':'lunge','db-step-up':'lunge',
 rdl:'hinge','db-rdl':'hinge','glute-bridge':'hinge','leg-curl':'knee-flexion','leg-extension':'knee-extension','calf-raise':'calf',
 'db-floor-press':'horizontal-push','db-bench-press':'horizontal-push','incline-db-press':'horizontal-push','machine-chest-press':'horizontal-push',pushup:'horizontal-push','close-pushup':'horizontal-push','cable-fly':'chest-isolation',
 'one-arm-row':'horizontal-pull','barbell-row':'horizontal-pull','seated-cable-row':'horizontal-pull','lat-pulldown':'vertical-pull','db-pullover':'vertical-pull',
 'db-ohp':'vertical-push','machine-shoulder-press':'vertical-push','lateral-raise':'shoulder-isolation','db-reverse-fly':'rear-delt','face-pull':'rear-delt',
 'db-curl':'elbow-flexion','hammer-curl':'elbow-flexion','cable-curl':'elbow-flexion','oh-triceps':'elbow-extension','cable-triceps':'elbow-extension',
 plank:'core-bracing','side-plank':'core-bracing',crunch:'core-flexion','dead-bug':'core-stability','bird-dog':'core-stability'
};
const RF14_ADVANCED=new Set(['bulgarian-split-squat','incline-db-press','barbell-row']);
const RF14_BEGINNER_FAVOR=new Set(['bodyweight-squat','goblet-squat','glute-bridge','db-floor-press','pushup','one-arm-row','machine-chest-press','lat-pulldown','seated-cable-row','leg-press','dead-bug','bird-dog','plank']);
const RF14_COMPOUND=new Set(['db-squat','goblet-squat','bodyweight-squat','leg-press','reverse-lunge','bulgarian-split-squat','db-step-up','rdl','db-rdl','glute-bridge','db-floor-press','db-bench-press','incline-db-press','machine-chest-press','pushup','one-arm-row','barbell-row','seated-cable-row','lat-pulldown','db-ohp','machine-shoulder-press']);


const rf14ExercisesBase=exercises;exercises=function(){return rf14ExercisesBase().map(rf14Meta);};


const rf14Generate132=generatePersonalProgram;
generatePersonalProgram=function(input){const p=validateProfile(input),gear=profileGear132(p);const pools={leg:['leg-press','db-squat','goblet-squat','reverse-lunge','bodyweight-squat','db-step-up'],hinge:['leg-curl','db-rdl','rdl','glute-bridge'],push:['machine-chest-press','db-bench-press','incline-db-press','db-floor-press','pushup'],pull:['lat-pulldown','seated-cable-row','barbell-row','one-arm-row'],shoulder:['machine-shoulder-press','db-ohp','lateral-raise','db-reverse-fly','face-pull'],bi:['cable-curl','db-curl','hammer-curl'],tri:['cable-triceps','oh-triceps','close-pushup'],core:['plank','dead-bug','crunch','side-plank'],stability:['bird-dog','dead-bug']};for(const k in pools)pools[k]=pools[k].filter(id=>available132(id,p));const hasPull=pools.pull.length>0,split=p.split==='auto'?(p.experience==='beginner'||!hasPull?'full':'upperlower'):p.split;if(split==='ppl'&&!hasPull)throw Error('A toló/húzó/láb felosztáshoz valódi húzógyakorlat szükséges.');let layouts;if(split==='full')layouts=hasPull?[['A',['leg','push','pull','hinge','core','shoulder']],['B',['hinge','push','pull','leg','core','tri']]]:[['A',['leg','push','hinge','core','stability']],['B',['hinge','push','leg','stability','core']]];else if(split==='upperlower')layouts=hasPull?[['Felsőtest',['push','pull','shoulder','bi','tri']],['Alsótest és törzs',['leg','hinge','core','stability']]]:[['Felsőtest és törzs',['push','core','stability']],['Alsótest és törzs',['leg','hinge','core']]];else layouts=[['Toló',['push','shoulder','tri']],['Húzó',['pull','bi','core']],['Láb és törzs',['leg','hinge','core']]];const sets=p.experience==='beginner'||p.activity==='physical'||p.minutes<=30?2:(p.goal==='muscle'&&p.minutes>=45?3:3),rest=p.goal==='strength'?150:p.goal==='muscle'?90:75,capacity=Math.max(3,Math.min(7,Math.floor((p.minutes-4)/(sets*(0.65+rest/60))))),prescriptions={};const days=layouts.map(([name,roles],di)=>{const ids=[];for(const role of roles.slice(0,capacity)){let id=rf14Pick(pools[role]||[],p,role,ids,di);if(!id&&['shoulder','bi','tri'].includes(role))id=rf14Pick(pools.core,p,'core',ids,di);if(!id)throw Error('A választott felszereléssel/kizárásokkal nincs elég megfelelő gyakorlat.');const e=exercises().find(x=>x.id===id),timed=e.repUnit.includes('mp');ids.push(id);const reps=timed?'20–40 mp':p.goal==='strength'&&e.compound?'5–8':p.goal==='muscle'?'8–15':'8–12';prescriptions[id]={sets,reps,weight:0,rest,trial:true,rir:p.experience==='beginner'?3:2};}return{id:String.fromCharCode(65+di),name,exercises:ids};});const equipment=gear.length?gear.map(x=>GEAR132[x]).join(' + '):'Saját testsúly';return{id:'personal-'+crypto.randomUUID(),name:(gear.length?'Kombinált':'Saját testsúlyos')+' terv – '+({full:'A/B',upperlower:'Felső / alsó',ppl:'Toló / húzó / láb'}[split]),location:gear.length?'Választott felszerelés':'Saját testsúly',level:p.experience==='beginner'?'Kezdő':'Középhaladó',builtin:false,generated:true,generatorVersion:'1.4.0',gear:[...gear],days,prescriptions,reasons:[`Elérhető: ${equipment}.`,`Cél: ${{fitness:'általános fittség',muscle:'izomépítés',strength:'erő',fatloss:'fogyást támogató erősítés'}[p.goal]}. A gyakorlatok cél, tapasztalat és technikai összetettség szerint rangsoroltak.`,`${p.minutes} perc: legfeljebb ${capacity} gyakorlat/nap, ${sets} munkasorozat, ${rest} mp pihenő.`,p.experience==='beginner'?'Kezdőként az alacsonyabb technikai komplexitású gyakorlatok előnyt kapnak.':'Középhaladóként szélesebb gyakorlatválaszték használható.','A kezdősúlyt nem testmagasságból/testsúlyból számítjuk; próbasorozattal állítsd be.'],createdAt:new Date().toISOString()};};


progressionSuggestion=function(h,rating){return rf14ProgressionSummary(h,rating);};
const rf14ApplyOld=applyProgression;applyProgression=function(){const hs=history(),h=hs.find(x=>x.started===state.feedbackStarted),f=h?.feedback,p=programById(h?.programId);if(!f||f.applied||!p?.generated)return;if(f.suggestion.action!=='smart')return rf14ApplyOld();for(const item of f.suggestion.items||[]){const r=p.prescriptions[item.id];if(!r)continue;if(item.action==='load')r.weight=item.next;else if(item.action==='reps'){const m=String(r.reps).match(/(\d+)\s*[–-]\s*(\d+)/);if(m)r.reps=`${Math.min(Number(m[1])+1,Number(m[2]))}–${m[2]}`;}}f.applied=true;db.set('programs',programs().map(x=>x.id===p.id?p:x));db.set('history',hs);alert('A következő edzés célértékei frissültek.');go('home');};


const rf14ProgramsScreen=programsScreen;programsScreen=function(){return rf14ProgramsScreen().replace('<div class="section">Gyakorlatkönyvtár</div>',`<div class="section">Gyakorlatkezelés</div><div class="grid2"><button class="btn secondary" onclick="customExerciseScreen()">+ Saját gyakorlat</button><button class="btn secondary" onclick="statsScreen()">Statisztikák</button></div>${exercises().filter(e=>e.custom).map(e=>`<div class="custom-ex-row"><span>${esc(e.hu)}</span><button class="btn danger" onclick="deleteCustomExercise14('${e.id}')">Törlés</button></div>`).join('')}<div class="section">Gyakorlatkönyvtár</div>`);};
const rf14Home=home;home=function(){return rf14Home().replace('<div class="section">Gyors választás</div>',`<div class="card"><strong>Terhelési iránytű</strong><p class="small muted">${esc(rf14RecoveryAdvice())}</p><button class="btn secondary" onclick="statsScreen()">Statisztikák</button></div><div class="section">Gyors választás</div>`);};
const rf14DemoCard=demoCard;demoCard=function(id){const e=exercises().find(x=>x.id===id);if(e?.media)return `<div class="video-card"><span class="badge">OFFLINE BEMUTATÓ</span><video controls playsinline preload="metadata" src="${esc(e.media)}"></video></div>`+rf14DemoCard(id);return rf14DemoCard(id);};
const rf14Backup=makeBackup;makeBackup=function(){return {...rf14Backup(),appVersion:RF14_VERSION};};
const rf14Shell=shell;shell=function(content){return rf14Shell(content).replaceAll('1.3.2','1.4.0').replace('rugalmas edzéstervező','intelligens edzéstervező');};
render();

const rf14EditCustomProgram=editCustomProgram;editCustomProgram=function(id){rf14EditCustomProgram(id);const p=programById(id);if(!p||p.builtin)return;const rows=document.querySelectorAll('.custom-ex-row');let n=0;for(let di=0;di<p.days.length;di++)for(let ei=0;ei<p.days[di].exercises.length;ei++){const row=rows[n++];if(!row)continue;const up=document.createElement('button'),down=document.createElement('button');up.className=down.className='btn secondary';up.textContent='↑';down.textContent='↓';up.disabled=ei===0;down.disabled=ei===p.days[di].exercises.length-1;up.onclick=()=>moveCustomExercise14(id,di,ei,-1);down.onclick=()=>moveCustomExercise14(id,di,ei,1);row.insertBefore(up,row.lastElementChild);row.insertBefore(down,row.lastElementChild);}};


// @endsection v14.js

// @section v141.js
var rf141FmtDuration = function rf141FmtDuration(mins){
  const n=Number(mins); if(!Number.isFinite(n)||n<=0)return 'Nincs adat';
  const h=Math.floor(n/60),m=Math.round(n%60); return `${h} ó ${m} p`;
};
var rf141Median = function rf141Median(a){const v=a.filter(Number.isFinite).sort((x,y)=>x-y);if(!v.length)return null;const i=Math.floor(v.length/2);return v.length%2?v[i]:(v[i-1]+v[i])/2;};
var rf141RecoverySignals = function rf141RecoverySignals(s){
  if(!s)return [];
  const out=[];
  if(Number.isFinite(Number(s.sleepMinutes))) out.push({kind:'sleep',value:Number(s.sleepMinutes)});
  if(Number.isFinite(Number(s.hrvRmssdMs))) out.push({kind:'hrv',value:Number(s.hrvRmssdMs)});
  return out;
};
var rf141RecoveryText = function rf141RecoveryText(){
  const base=rf14RecoveryAdvice();
  const s=state.health?.recovery;
  if(!s)return base;
  const parts=[];
  if(Number.isFinite(Number(s.sleepMinutes))){
    const min=Number(s.sleepMinutes);
    if(min<360)parts.push('Az utolsó rögzített alvás rövid volt; ma érdemes visszafogottabb terheléssel kezdeni.');
    else if(min>=420)parts.push('Az utolsó rögzített alvás időtartama megfelelő tartományban van.');
    else parts.push('Az utolsó rögzített alvás közepes hosszúságú volt.');
  }
  if(Number.isFinite(Number(s.hrvRmssdMs))){
    const cur=Number(s.hrvRmssdMs),baseH=Number(s.hrvBaselineMs);
    if(Number.isFinite(baseH)&&baseH>0){
      const d=(cur-baseH)/baseH;
      if(d<=-0.20)parts.push('A legutóbbi HRV a saját közelmúltbeli alapértéked alatt van; ne erőltesd az automatikus terhelésemelést.');
      else if(d>=0.15)parts.push('A legutóbbi HRV a saját közelmúltbeli alapértéked felett van.');
      else parts.push('A legutóbbi HRV közel van a saját alapértékedhez.');
    } else parts.push('HRV adat elérhető; személyes alapértékhez több napnyi adat kell.');
  }
  if(!parts.length)return base;
  return `${base} ${parts.join(' ')} Ez tájékoztató edzésterhelési jelzés, nem orvosi diagnózis.`;
};
var readRecoveryHealth = async function readRecoveryHealth(){
  await healthAction(async p=>{
    const end=new Date(),start=new Date(end.getTime()-8*24*3600000);
    const result=await p.readRecovery({start:start.toISOString(),end:end.toISOString()});
    state.health.recovery=result;
    return (result.sleepMinutes!=null||result.hrvRmssdMs!=null)
      ?'Regenerációs adatok frissítve a Health Connectből.'
      :'Nem találtunk megosztott alvás/HRV adatot. Ellenőrizd a Samsung Health/Health Connect engedélyeket és szinkront.';
  });
};
var rf141RecoveryHtml = function rf141RecoveryHtml(){
  const s=state.health?.recovery;if(!s)return '<p class="small muted">Még nincs beolvasott alvás/HRV adat.</p>';
  return `<div class="grid2"><div class="stat"><small>Legutóbbi alvás</small><strong>${rf141FmtDuration(s.sleepMinutes)}</strong></div><div class="stat"><small>Legutóbbi HRV (RMSSD)</small><strong>${s.hrvRmssdMs==null?'Nincs adat':Math.round(s.hrvRmssdMs)+' ms'}</strong></div></div><p class="small muted">${s.hrvBaselineMs==null?'HRV alapértékhez több adat kell.':'7 napos HRV medián: '+Math.round(s.hrvBaselineMs)+' ms'}${s.sleepEnd?' • Alvás vége: '+esc(new Date(s.sleepEnd).toLocaleString('hu-HU')):''}</p>`;
};
/* RepForge 1.4.1: completes the 1.4 feature package; Health Connect recovery metrics (sleep + HRV), no bundled offline videos. */
const RF141_VERSION='1.4.1';


const rf141HealthScreen=healthScreen;
healthScreen=function(){
  rf141HealthScreen();
  const main=document.querySelector('main'); if(!main)return;
  const old=main.querySelector('#rf141RecoveryCard'); if(old)old.remove();
  const card=document.createElement('div');card.id='rf141RecoveryCard';card.className='card';
  card.innerHTML=`<h2>Regeneráció</h2><p class="small muted">Health Connectből: alvás és HRV (RMSSD). Az értékeket csak az aktuális nézetben tartjuk, nem kerülnek Drive-mentésbe.</p>${rf141RecoveryHtml()}<button class="btn secondary block" onclick="readRecoveryHealth()" ${state.health.busy?'disabled':''}>Alvás és HRV frissítése</button>`;
  const hero=main.querySelector('.hero'); if(hero)hero.insertAdjacentElement('afterend',card); else main.prepend(card);
};

const rf141ClearHealth=clearHealthView;
clearHealthView=function(){if(state.health)state.health.recovery=null;return rf141ClearHealth();};

const rf141Home=home;
home=function(){
  const html=rf141Home();
  return html.replace(/<div class="card"><strong>Terhelési iránytű<\/strong><p class="small muted">[\s\S]*?<\/p><button class="btn secondary" onclick="statsScreen\(\)">Statisztikák<\/button><\/div>/,
    `<div class="card"><strong>Terhelési iránytű</strong><p class="small muted">${esc(rf141RecoveryText())}</p><div class="grid2"><button class="btn secondary" onclick="statsScreen()">Statisztikák</button><button class="btn secondary" onclick="healthScreen()">Regeneráció frissítése</button></div></div>`);
};

// v1.4.1 intentionally keeps online/existing exercise demos only; no bundled offline video library.
const rf141DemoCard=demoCard;
demoCard=function(id){const html=rf141DemoCard(id);return html.replace('<span class="badge">OFFLINE BEMUTATÓ</span>','<span class="badge">HELYI BEMUTATÓ</span>');};

const rf141Backup=makeBackup;
makeBackup=function(){return {...rf141Backup(),appVersion:RF141_VERSION};};
const rf141Shell=shell;
shell=function(content){return rf141Shell(content).replaceAll('1.4.0','1.4.1');};

render();


// @endsection v141.js

// @section v142.js
var rf142LocalInputValue = function rf142LocalInputValue(iso){
  const d=new Date(iso); if(!Number.isFinite(d.getTime()))return '';
  const p=n=>String(n).padStart(2,'0');
  return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
};
var rf142IsoFromLocal = function rf142IsoFromLocal(value){
  const d=new Date(value); if(!value||!Number.isFinite(d.getTime()))throw Error('Adj meg érvényes dátumot és időpontot.');
  return d.toISOString();
};
var rf142WorkoutKey = function rf142WorkoutKey(x){return x.id||`${x.started}|${x.programId||''}|${x.dayId||x.workout||''}`;};
var rf142FindHistoryIndex = function rf142FindHistoryIndex(key){return history().findIndex(x=>rf142WorkoutKey(x)===key);};
var rf142ValidWindow = function rf142ValidWindow(started,finished){
  const a=Date.parse(started),b=Date.parse(finished),now=Date.now()+60000;
  if(!Number.isFinite(a)||!Number.isFinite(b))throw Error('Érvénytelen kezdési vagy befejezési idő.');
  if(b<=a)throw Error('A befejezésnek a kezdés után kell lennie.');
  if(b>now)throw Error('A befejezési idő nem lehet a jövőben.');
  const mins=Math.round((b-a)/60000);
  if(mins>24*60)throw Error('Egy edzés legfeljebb 24 órás lehet. Ellenőrizd az időpontokat.');
  return mins;
};
var editHistoryWorkout = function editHistoryWorkout(key){
  const i=rf142FindHistoryIndex(key),x=history()[i]; if(!x){alert('Az edzés nem található.');return;}
  render(shell(`<main><button class="btn secondary" onclick="go('history')">← Vissza</button><div class="hero"><h1>Edzés módosítása</h1><p>${esc(x.programName||'Edzés')} • ${esc(x.dayId||x.workout||'')}</p></div><div class="card"><label>Kezdés<input class="field" id="rf142Start" type="datetime-local" value="${esc(rf142LocalInputValue(x.started))}"></label><label>Befejezés<input class="field" id="rf142End" type="datetime-local" value="${esc(rf142LocalInputValue(x.finished))}"></label><p class="small muted">Mentés után az edzés időtartama újraszámolódik. Az órás adatokat az új időablakból lehet újra lekérni.</p><button class="btn block" onclick="saveHistoryWorkoutTime('${esc(key)}',false)">Időpont mentése</button><br><button class="btn secondary block" onclick="saveHistoryWorkoutTime('${esc(key)}',true)">Mentés + órás adatok frissítése</button></div><div class="card"><strong>Hibás edzés?</strong><p class="small muted">A törlés végleges ezen a készüléken. Drive szinkron esetén a következő szinkron a módosított helyi naplót használja.</p><button class="btn secondary block" onclick="deleteHistoryWorkout('${esc(key)}')">Edzés törlése</button></div></main>`));
};
var saveHistoryWorkoutTime = async function saveHistoryWorkoutTime(key,refreshHealth){
  try{
    const i=rf142FindHistoryIndex(key); if(i<0)throw Error('Az edzés nem található.');
    const old=history()[i],started=rf142IsoFromLocal($('#rf142Start')?.value),finished=rf142IsoFromLocal($('#rf142End')?.value);
    const mins=rf142ValidWindow(started,finished);
    const h=history();
    h[i]={...h[i],started,finished,editedAt:new Date().toISOString()};
    h.sort((a,b)=>String(b.started||'').localeCompare(String(a.started||'')));
    db.set('history',h);
    if(state.feedbackStarted===old.started)state.feedbackStarted=started;
    if(state.health){
      if(state.health.workout===old.started)state.health.workout=started;
      state.health.summary=null;
    }
    if(refreshHealth){
      const updated=h.find(x=>x.started===started&&x.finished===finished);
      await rf142ReadHealthForWorkout(updated);
      return;
    }
    alert(`Időpont mentve. Új időtartam: ${mins} perc.`);
    go('history');
  }catch(e){alert(e.message||'Az időpont mentése nem sikerült.');}
};
var rf142ReadHealthForWorkout = async function rf142ReadHealthForWorkout(h){
  if(!h){alert('Az edzés nem található.');return;}
  if(typeof isNative==='function'&&!isNative()){
    alert('Az időpont mentve. Az órás adatok frissítése Androidon érhető el.');go('history');return;
  }
  state.health.workout=h.started; state.health.summary=null;
  await healthAction(async p=>{
    let source=state.health.source||'';
    let result=await p.readWorkout({start:h.started,end:h.finished,source});
    if(!result.source&&result.sources?.length===1){
      source=result.sources[0];
      result=await p.readWorkout({start:h.started,end:h.finished,source});
    }
    state.health.summary=result;state.health.source=result.source||source;
    return result.source
      ?'Az edzés időpontja mentve, és az órás adatok az új időablakból frissültek.'
      :result.sources?.length
        ?'Az időpont mentve. Több adatforrás található; válassz forrást az Óra és egészségadatok képernyőn.'
        :'Az időpont mentve, de ehhez az időablakhoz még nincs megosztott órás adat. Szinkronizáld az órát, majd frissítsd újra.';
  });
  healthScreen();
};
var deleteHistoryWorkout = function deleteHistoryWorkout(key){
  const i=rf142FindHistoryIndex(key),h=history(); if(i<0){alert('Az edzés nem található.');return;}
  const x=h[i];
  if(!confirm(`Biztosan törlöd ezt az edzést?\n${fmtDate(x.started)} • ${sessionDuration(x)}`))return;
  h.splice(i,1);db.set('history',h);
  if(x.scheduleId){
    const s=scheduled(),si=s.findIndex(v=>v.id===x.scheduleId);
    if(si>=0&&s[si].status==='completed'){s[si]={...s[si],status:'planned',updatedAt:Date.now()};db.set('scheduled',s);}
  }
  if(state.health?.workout===x.started){state.health.workout='';state.health.summary=null;}
  alert('Az edzés törölve.');go('history');
};
/* RepForge 1.4.2: editable workout timestamps, safe deletion, and Health Connect re-read for corrected workout windows. */
const RF142_VERSION='1.4.2';


historyScreen=function(){
  const h=history();
  return shell(`<main><div class="hero"><h1>Edzésnapló</h1><div class="muted">Minden program korábbi edzése egy helyen marad.</div></div>${h.length?h.map((x,hi)=>{const c=completedSets(x),title=x.programName?`${x.programName} • ${x.dayId||x.workout}`:`Full Body ${x.workout}`,key=rf142WorkoutKey(x);return `<div class="history"><div class="history-head"><div class="history-title-row"><div><div class="history-title">${esc(title)}</div><div class="history-date">${fmtDate(x.started)}</div></div><span class="badge">${x.exercises.length} gyakorlat</span></div><div class="history-badges"><span class="history-badge">⏱ ${sessionDuration(x)}</span><span class="history-badge">✓ ${c.done}/${c.total} sorozat</span></div></div><div class="history-body"><div class="grid2"><button class="btn secondary" onclick="editHistoryWorkout('${esc(key)}')">Edzés módosítása</button><button class="btn secondary" onclick="healthFromHistory(${hi})">Health adatok</button></div><br><button class="btn secondary" onclick="calendarIntent(${hi})">Hozzáadás a naptárhoz</button>${x.exercises.map(e=>`<div class="history-ex"><div class="history-ex-name">${esc(e.hu)}</div><div class="en">${esc(e.en||'')}</div><div class="history-setchips">${(e.sets||[]).map(s=>`<span class="setchip">${esc(formatSet(e,s))}${s.done?' ✓':' • nincs kész'}</span>`).join('')}</div></div>`).join('')}</div></div>`;}).join(''):'<div class="muted">Még nincs elmentett edzés.</div>'}</main>`);
};

const rf142Backup=makeBackup;
makeBackup=function(){return {...rf142Backup(),appVersion:RF142_VERSION};};
const rf142Shell=shell;
shell=function(content){return rf142Shell(content).replaceAll('1.4.1','1.4.2');};

render();


// @endsection v142.js

// @section v143.js
var discardDraft143 = function discardDraft143(){const d=db.get('draft',null);if(!d){alert('Nincs félbehagyott edzés.');return;}tp2628Confirm('Biztosan törlöd a félbehagyott edzést? A naplóba nem kerül be.',{title:'Félbehagyott edzés törlése',confirmText:'Törlés',danger:true}).then(ok=>{if(!ok)return;db.set('draft',null);stopTimer();state.session=null;state.workout=null;state.current=0;state.restEndAt=null;state.timer=0;alert('A félbehagyott edzés törölve.');go('home');});};
/* RepForge 1.4.3: 60-exercise library, curated online demos, generator integration, and safe home-screen draft deletion. */
const RF143_VERSION='1.4.3';
const RF143_NEW_EXERCISES=[
{id:'incline-pushup',hu:'Emelt fekvőtámasz',en:'Incline Push-Up',equipment:'testsúly + stabil pad/támasz',loadType:'bodyweight',target:'Mell / tricepsz',notes:'Tartsd a törzset egyenes vonalban, a kezek stabil emelt felületen legyenek. Kontrolláltan engedd a mellkast a támasz felé.',sets:2,reps:'8–15',weight:0,repUnit:'ism.',gear:'bodyweight',movementPattern:'horizontal-push',difficulty:'beginner',complexity:'low',beginnerSafe:true,compound:true,goalTags:['fitness','muscle','fatloss']},
{id:'modified-pushup',hu:'Térdelős fekvőtámasz',en:'Modified Push-Up',equipment:'testsúly',loadType:'bodyweight',target:'Mell / tricepsz',notes:'Térdtámaszból tarts egyenes vonalat a térdtől a fejig. A könyök nagyjából 45 fokban haladjon.',sets:2,reps:'8–15',weight:0,repUnit:'ism.',gear:'bodyweight',movementPattern:'horizontal-push',difficulty:'beginner',complexity:'low',beginnerSafe:true,compound:true,goalTags:['fitness','muscle','fatloss']},
{id:'decline-pushup',hu:'Lábemelt fekvőtámasz',en:'Decline Push-Up',equipment:'testsúly + pad',loadType:'bodyweight',target:'Mell / váll / tricepsz',notes:'A láb stabil emelt felületen legyen. Tarts feszes törzset és kontrollált könyökhelyzetet.',sets:2,reps:'5–12',weight:0,repUnit:'ism.',gear:'bodyweight',movementPattern:'horizontal-push',difficulty:'intermediate',complexity:'medium',beginnerSafe:false,compound:true,goalTags:['fitness','muscle','strength']},
{id:'pike-pushup',hu:'Pike fekvőtámasz',en:'Pike Push-Up',equipment:'testsúly',loadType:'bodyweight',target:'Váll / tricepsz',notes:'Fordított V helyzetből közelítsd a fejet kontrolláltan a talajhoz, majd nyomd vissza magad.',sets:2,reps:'5–12',weight:0,repUnit:'ism.',gear:'bodyweight',movementPattern:'vertical-push',difficulty:'intermediate',complexity:'medium',beginnerSafe:false,compound:true,goalTags:['fitness','muscle','strength']},
{id:'plyo-pushup',hu:'Robbanékony fekvőtámasz',en:'Plyometric Push-Up',equipment:'testsúly',loadType:'bodyweight',target:'Mell / tricepsz',notes:'Csak stabil alap fekvőtámasz után. Robbanékonyan nyomd el magad, majd puhán, hajlított könyökkel érkezz.',sets:3,reps:'3–5',weight:0,repUnit:'ism.',gear:'bodyweight',movementPattern:'horizontal-push',difficulty:'advanced',complexity:'high',beginnerSafe:false,compound:true,goalTags:['fitness','strength']},
{id:'archer-pushup',hu:'Íjász fekvőtámasz',en:'Archer Push-Up',equipment:'testsúly',loadType:'bodyweight',target:'Mell / tricepsz / törzs',notes:'Széles kéztartásból terheld főleg az egyik kart, a másik nyúljon oldalra. Váltott oldalakkal dolgozz.',sets:2,reps:'5–10 / oldal',weight:0,repUnit:'/oldal',gear:'bodyweight',movementPattern:'horizontal-push',difficulty:'advanced',complexity:'high',beginnerSafe:false,compound:true,goalTags:['fitness','muscle','strength']},
{id:'prisoner-squat',hu:'Tarkóra tett kézzel guggolás',en:'Prisoner Squat',equipment:'testsúly',loadType:'bodyweight',target:'Láb / far / törzs',notes:'A kezek a fej mögött, mellkas nyitva. Guggolj kontrolláltan, a térd kövesse a lábfejet.',sets:2,reps:'10–15',weight:0,repUnit:'ism.',gear:'bodyweight',movementPattern:'squat',difficulty:'beginner',complexity:'low',beginnerSafe:true,compound:true,goalTags:['fitness','muscle','fatloss']},
{id:'single-leg-squat',hu:'Egylábas guggolás támasz mellett',en:'Single-Leg Squat',equipment:'testsúly + opcionális stabil támasz',loadType:'bodyweight',target:'Láb / far / törzs',notes:'Egy lábon, szükség esetén stabil támaszt fogva ereszkedj csak addig, amíg a térd és a medence kontrollált marad.',sets:2,reps:'5–10 / oldal',weight:0,repUnit:'/oldal',gear:'bodyweight',movementPattern:'squat',difficulty:'advanced',complexity:'high',beginnerSafe:false,compound:true,goalTags:['fitness','strength']},
{id:'squat-jump',hu:'Guggolásból felugrás',en:'Squat Jump',equipment:'testsúly',loadType:'bodyweight',target:'Láb / far',notes:'Guggolásból robbanékonyan ugorj fel, majd puhán, hajlított térddel érkezz. Ne végezd kifáradásig.',sets:3,reps:'5–8',weight:0,repUnit:'ism.',gear:'bodyweight',movementPattern:'squat',difficulty:'intermediate',complexity:'medium',beginnerSafe:false,compound:true,goalTags:['fitness','strength','fatloss']},
{id:'lunge-jump',hu:'Váltott kitörésből felugrás',en:'Lunge Jump',equipment:'testsúly',loadType:'bodyweight',target:'Láb / far',notes:'Kitörésből ugorj fel és cseréld a lábakat a levegőben. Puhán érkezz, a térd ne essen befelé.',sets:3,reps:'5–8 / oldal',weight:0,repUnit:'/oldal',gear:'bodyweight',movementPattern:'lunge',difficulty:'intermediate',complexity:'high',beginnerSafe:false,compound:true,goalTags:['fitness','strength','fatloss']},
{id:'jumping-jacks',hu:'Terpesz-zár szökdelés',en:'Jumping Jacks',equipment:'testsúly',loadType:'bodyweight',target:'Teljes test',notes:'Ritmusosan nyisd-zárd a lábakat és emeld a karokat. Puhán érkezz; alacsony terheléshez lépegetve is végezhető.',sets:2,reps:'30–60 mp',weight:0,repUnit:'mp',gear:'bodyweight',movementPattern:'conditioning',difficulty:'beginner',complexity:'low',beginnerSafe:true,compound:true,goalTags:['fitness','fatloss']},
{id:'box-jump',hu:'Dobozra felugrás',en:'Box Jump',equipment:'stabil fellépő/doboz',loadType:'bodyweight',target:'Láb / far',notes:'Csak stabil, nem billenő felületre ugorj. Puhán érkezz mindkét lábra, lefelé inkább lépj.',sets:3,reps:'3–5',weight:0,repUnit:'ism.',gear:'step',movementPattern:'squat',difficulty:'intermediate',complexity:'high',beginnerSafe:false,compound:true,goalTags:['fitness','strength']},
{id:'db-front-squat',hu:'Két kézisúlyzós elöl guggolás',en:'Dumbbell Front Squat',equipment:'2 kézisúlyzó',loadType:'per_hand',target:'Láb / far / törzs',notes:'Tarts egy-egy súlyzót vállmagasságban. Mellkas maradjon fent, a térdek kövessék a lábfejeket.',sets:2,reps:'8–12',weight:0,repUnit:'ism.',gear:'dumbbells',movementPattern:'squat',difficulty:'beginner',complexity:'low',beginnerSafe:true,compound:true,goalTags:['fitness','muscle','strength','fatloss']},
{id:'single-arm-db-press',hu:'Egykezes kézisúlyzós fekvenyomás',en:'Single-Arm Dumbbell Chest Press',equipment:'1 kézisúlyzó + pad',loadType:'single_dumbbell',target:'Mell / tricepsz / törzs',notes:'Padon fekve egy karral nyomj. Feszítsd a törzset, hogy a medence és a mellkas ne forduljon el.',sets:2,reps:'8–12 / oldal',weight:0,repUnit:'/oldal',gear:'dumbbell',movementPattern:'horizontal-push',difficulty:'intermediate',complexity:'medium',beginnerSafe:false,compound:true,goalTags:['fitness','muscle','strength']},
{id:'single-arm-incline-press',hu:'Egykezes ferdepados kézisúlyzós nyomás',en:'Single-Arm Incline Dumbbell Chest Press',equipment:'1 kézisúlyzó + állítható pad',loadType:'single_dumbbell',target:'Mell / váll / tricepsz',notes:'Ferde padon egy karral nyomj, a törzs ne forduljon el. Kezdj könnyű próbasúllyal.',sets:2,reps:'8–12 / oldal',weight:0,repUnit:'/oldal',gear:'dumbbell',movementPattern:'horizontal-push',difficulty:'intermediate',complexity:'medium',beginnerSafe:false,compound:true,goalTags:['fitness','muscle','strength']},
{id:'single-leg-press',hu:'Egylábas lábtoló',en:'Single-Leg Press',equipment:'lábtoló gép',loadType:'total',target:'Láb / far',notes:'Egy lábbal nyomd a platformot kontrolláltan. A térdet ne zárd ki és ne engedd befelé esni.',sets:2,reps:'8–12 / oldal',weight:0,repUnit:'/oldal',gear:'machine',movementPattern:'squat',difficulty:'intermediate',complexity:'medium',beginnerSafe:false,compound:true,goalTags:['fitness','muscle','strength']},
{id:'seated-leg-curl',hu:'Ülő lábhajlítás gépen',en:'Seated Leg Curl',equipment:'ülő lábhajlító gép',loadType:'total',target:'Combhajlító',notes:'Állítsd a párnát a boka fölé, a hát maradjon a támlán. Hajlíts és engedj vissza kontrolláltan.',sets:2,reps:'8–15',weight:0,repUnit:'ism.',gear:'machine',movementPattern:'knee-flexion',difficulty:'beginner',complexity:'low',beginnerSafe:true,compound:false,goalTags:['fitness','muscle','strength']},
{id:'single-leg-seated-curl',hu:'Egylábas ülő lábhajlítás',en:'Single-Leg Seated Leg Curl',equipment:'ülő lábhajlító gép',loadType:'total',target:'Combhajlító',notes:'Egy lábbal hajlíts kontrolláltan, a medence maradjon stabil. Az oldalakat azonos technikával végezd.',sets:2,reps:'8–12 / oldal',weight:0,repUnit:'/oldal',gear:'machine',movementPattern:'knee-flexion',difficulty:'intermediate',complexity:'medium',beginnerSafe:false,compound:false,goalTags:['fitness','muscle','strength']},
{id:'leg-press-calf',hu:'Vádliemelés lábtoló gépen',en:'Leg Press Calf Raise',equipment:'lábtoló gép',loadType:'total',target:'Vádli',notes:'Csak az előláb legyen stabilan a platformon. Bokából emeld és engedd a sarkat, a térd maradjon kontrollált.',sets:2,reps:'12–20',weight:0,repUnit:'ism.',gear:'machine',movementPattern:'calf',difficulty:'beginner',complexity:'low',beginnerSafe:true,compound:false,goalTags:['fitness','muscle','strength']},
{id:'close-grip-bench',hu:'Szűk fogású fekvenyomás',en:'Close Grip Bench Press',equipment:'kétkezes rúd + pad',loadType:'total',target:'Tricepsz / mell',notes:'A fogás legyen a normál fekvenyomásnál szűkebb, de ne extrém keskeny. Kontrolláltan engedd a rudat.',sets:2,reps:'6–12',weight:0,repUnit:'ism.',gear:'barbell',movementPattern:'horizontal-push',difficulty:'intermediate',complexity:'medium',beginnerSafe:false,compound:true,goalTags:['muscle','strength','fitness']}
];
const RF143_DEMOS={'incline-pushup':'0JUrOH--Kdk','modified-pushup':'PDr5B2jLUOw','decline-pushup':'DBz85WuXqMk','pike-pushup':'2b5t0Cu2nQI','plyo-pushup':'MH4gcTKQiEc','archer-pushup':'IDu6pRAPChg','prisoner-squat':'UYbsgiiZgao','single-leg-squat':'sSXnaFyhiZs','squat-jump':'tZSYZdtbONc','lunge-jump':'_5kDxC0flg0','jumping-jacks':'uLVt6u15L98','box-jump':'DXu-8TAJwi4','db-front-squat':'hZI8Yy5elZs','single-arm-db-press':'qFTnmyC-nf4','single-arm-incline-press':'iJ-GwVeUuCg','single-leg-press':'3aYsOsBA7ZE','seated-leg-curl':'_2Kd0d-JEUM','single-leg-seated-curl':'PXNJ71rksvU','leg-press-calf':'8k435cj30gc','close-grip-bench':'LJeqLAmJLfs'};
const RF143_MUSCLES={'incline-pushup':['chest','triceps','core'],'modified-pushup':['chest','triceps','core'],'decline-pushup':['chest','shoulders','triceps'],'pike-pushup':['shoulders','triceps','core'],'plyo-pushup':['chest','triceps','shoulders'],'archer-pushup':['chest','triceps','core'],'prisoner-squat':['legs','core'],'single-leg-squat':['legs','core'],'squat-jump':['legs','core'],'lunge-jump':['legs','core'],'jumping-jacks':['legs','shoulders','core'],'box-jump':['legs','core'],'db-front-squat':['legs','core'],'single-arm-db-press':['chest','triceps','core'],'single-arm-incline-press':['chest','shoulders','triceps'],'single-leg-press':['legs'],'seated-leg-curl':['legs'],'single-leg-seated-curl':['legs'],'leg-press-calf':['legs'],'close-grip-bench':['triceps','chest','shoulders']};
const RF143_NEEDS={'incline-pushup':[],'modified-pushup':[],'decline-pushup':['bench'],'pike-pushup':[],'plyo-pushup':[],'archer-pushup':[],'prisoner-squat':[],'single-leg-squat':[],'squat-jump':[],'lunge-jump':[],'jumping-jacks':[],'box-jump':['step'],'db-front-squat':['dumbbells'],'single-arm-db-press':['dumbbell','bench'],'single-arm-incline-press':['dumbbell','bench'],'single-leg-press':['machines'],'seated-leg-curl':['machines'],'single-leg-seated-curl':['machines'],'leg-press-calf':['machines'],'close-grip-bench':['barbell','bench']};
for(const e of RF143_NEW_EXERCISES){if(typeof EXTRA_EXERCISES!=='undefined'&&!EXTRA_EXERCISES.some(x=>x.id===e.id))EXTRA_EXERCISES.push({...e});if(typeof MUSCLE_MAP!=='undefined')MUSCLE_MAP[e.id]=RF143_MUSCLES[e.id]||[];if(typeof NEEDS132!=='undefined')NEEDS132[e.id]=RF143_NEEDS[e.id]||[];if(typeof RF14_PATTERNS!=='undefined')RF14_PATTERNS[e.id]=e.movementPattern;if(typeof RF14_COMPOUND!=='undefined'&&e.compound)RF14_COMPOUND.add(e.id);if(typeof RF14_ADVANCED!=='undefined'&&!e.beginnerSafe)RF14_ADVANCED.add(e.id);if(typeof RF14_BEGINNER_FAVOR!=='undefined'&&e.beginnerSafe)RF14_BEGINNER_FAVOR.add(e.id);if(typeof DEMOS!=='undefined')DEMOS[e.id]=[null,'youtube',RF143_DEMOS[e.id],'NASM'];}

const rf143Home=home;home=function(){let html=rf143Home();if(db.get('draft',null))html=html.replace('<button class="btn block" onclick="resumeDraft()">Edzés folytatása</button>','<div class="grid2"><button class="btn block" onclick="resumeDraft()">Edzés folytatása</button><button class="btn secondary" onclick="discardDraft143()">Félbehagyott edzés törlése</button></div>');return html;};
const rf143Generate=generatePersonalProgram;generatePersonalProgram=function(input){const p=validateProfile(input),out=rf143Generate(input),newPool=RF143_NEW_EXERCISES.filter(e=>available132(e.id,p)&&e.movementPattern!=='conditioning');const used=new Set(out.days.flatMap(d=>d.exercises));for(let di=0;di<out.days.length;di++){const day=out.days[di];for(let i=0;i<day.exercises.length;i++){const old=exercises().find(e=>e.id===day.exercises[i]);if(!old)continue;const candidates=newPool.filter(e=>e.movementPattern===old.movementPattern&&!used.has(e.id)&&(p.experience!=='beginner'||e.beginnerSafe));if(!candidates.length||(di+i)%2!==0)continue;const n=candidates[(di+i)%candidates.length],oldId=day.exercises[i],rx=out.prescriptions[oldId];day.exercises[i]=n.id;used.add(n.id);if(rx){out.prescriptions[n.id]={...rx,reps:n.repUnit.includes('mp')?'20–40 mp':rx.reps,weight:0,trial:true};delete out.prescriptions[oldId];}}}out.reasons=[...(out.reasons||[]),'A v1.4.3 kibővített, 60 gyakorlatos könyvtárából a felszerelésedhez és tapasztalatodhoz illő új variációk is bekerülhetnek.'];return out;};
const rf143Backup=makeBackup;makeBackup=function(){return {...rf143Backup(),appVersion:RF143_VERSION};};const rf143Shell=shell;shell=function(content){return rf143Shell(content).replaceAll('1.4.2','1.4.3');};render();


// @endsection v143.js

// @section v144.js
var rf144Favorites = function rf144Favorites(){const x=db.get(RF144_FAV_KEY,[]);return Array.isArray(x)?x:[];};
var isExerciseFavorite144 = function isExerciseFavorite144(id){return rf144Favorites().includes(id);};
var toggleExerciseFavorite144 = function toggleExerciseFavorite144(id){const s=new Set(rf144Favorites());s.has(id)?s.delete(id):s.add(id);db.set(RF144_FAV_KEY,[...s]);render();};
var rf144RenameLibrary = function rf144RenameLibrary(html){return typeof html==='string'?html.replaceAll('Izomcsoportok','Gyakorlatkönyvtár'):html;};
var rf144FavoriteButtons = function rf144FavoriteButtons(html){if(typeof html!=='string')return html;return html.replace(/(<button[^>]*onclick="demo\('([^']+)'\)"[^>]*>)/g,(m,button,id)=>`<button class="btn secondary" onclick="event.stopPropagation();toggleExerciseFavorite144('${id}')">${isExerciseFavorite144(id)?'★ Kedvenc':'☆ Kedvenc'}</button>${button}`);};
/* RepForge 1.4.4: exercise favorites and clearer Exercise Library naming. */
const RF144_VERSION='1.4.4';
const RF144_FAV_KEY='exerciseFavorites';


if(typeof muscleScreen==='function'){const old=muscleScreen;muscleScreen=function(...a){return rf144RenameLibrary(rf144FavoriteButtons(old.apply(this,a)));};}
if(typeof exerciseLibrary==='function'){const old=exerciseLibrary;exerciseLibrary=function(...a){return rf144RenameLibrary(rf144FavoriteButtons(old.apply(this,a)));};}
if(typeof exercisesScreen==='function'){const old=exercisesScreen;exercisesScreen=function(...a){return rf144RenameLibrary(rf144FavoriteButtons(old.apply(this,a)));};}
const rf144Shell=shell;shell=function(content){let h=rf144Shell(content).replaceAll('1.4.3','1.4.4');h=rf144RenameLibrary(h);return h;};
const rf144Backup=makeBackup;makeBackup=function(){const b={...rf144Backup(),appVersion:RF144_VERSION};b.exerciseFavorites=rf144Favorites();return b;};
render();


// @endsection v144.js

// @section v145.js
var rf145UpgradeHomePrograms = function rf145UpgradeHomePrograms(){
  if(db.get('homeFocus145',false))return;
  const ps=programs();
  let changed=false;
  for(const p of ps){
    const spec=RF145_HOME_PROGRAMS[p.id];
    if(!spec||p.builtin!==true)continue;
    for(const d of p.days||[]){
      if(spec[d.id]){d.exercises=[...spec[d.id]];changed=true;}
    }
  }
  if(changed)db.set('programs',ps);
  db.set('homeFocus145',true);
};
var rf145Groups = function rf145Groups(id){
  if(typeof muscleGroups==='function')return muscleGroups(id)||[];
  const e=typeof byId==='function'?byId(id):null;
  const p=e?.movementPattern||'';
  if(['horizontal-push','chest-isolation'].includes(p))return['chest'];
  if(['horizontal-pull','vertical-pull','rear-delt'].includes(p))return['back'];
  if(['vertical-push','shoulder-isolation'].includes(p))return['shoulders'];
  if(['squat','lunge','hinge','knee-flexion','knee-extension','calf'].includes(p))return['legs'];
  if(String(p).startsWith('core-'))return['core'];
  return[];
};
var rf145MuscleSets = function rf145MuscleSets(){
  const out={};
  for(const h of history())for(const e of h.exercises||[]){
    const done=(e.sets||[]).filter(s=>s.done).length;
    if(!done)continue;
    for(const g of rf145Groups(e.id))out[g]=(out[g]||0)+done;
  }
  return out;
};
var rf145MuscleLabel = function rf145MuscleLabel(k){
  if(typeof MUSCLES!=='undefined'&&MUSCLES[k])return MUSCLES[k];
  if(typeof MUSCLE_LABELS!=='undefined'&&MUSCLE_LABELS[k])return MUSCLE_LABELS[k];
  return ({chest:'Mell',back:'Hát',shoulders:'Váll',biceps:'Bicepsz',triceps:'Tricepsz',legs:'Láb / far',core:'Törzs'})[k]||k;
};
/* RepForge 1.4.5: chest/shoulder/core-focused home plans, full-body balance, better muscle statistics. */
const RF145_VERSION='1.4.5';

const RF145_HOME_PROGRAMS={
  'home-basic':{
    A:['db-squat','db-floor-press','one-arm-row','db-ohp','db-rdl','plank'],
    B:['reverse-lunge','pushup','barbell-row','lateral-raise','glute-bridge','crunch']
  },
  'home-level2':{
    A:['goblet-squat','db-floor-press','one-arm-row','db-ohp','db-rdl','side-plank'],
    B:['bulgarian-split-squat','pushup','db-pullover','lateral-raise','glute-bridge','dead-bug']
  },
  'home-varied':{
    A:['db-front-squat','incline-pushup','one-arm-row','pike-pushup','db-rdl','side-plank'],
    B:['prisoner-squat','pushup','barbell-row','lateral-raise','glute-bridge','dead-bug']
  }
};


rf145UpgradeHomePrograms();


const rf145GeneratePrev=generatePersonalProgram;
generatePersonalProgram=function(input){
  const p=validateProfile(input),plan=rf145GeneratePrev(input);
  plan.generatorVersion=RF145_VERSION;
  const isFull=(p.split==='full')||(p.split==='auto'&&plan.days?.some(d=>d.id==='A')&&plan.days?.some(d=>d.id==='B'));
  if(!isFull||p.minutes<35)return plan;
  const candidates={
    chest:['db-floor-press','db-bench-press','pushup','incline-pushup','modified-pushup','single-arm-db-press'],
    back:['one-arm-row','barbell-row','db-pullover','seated-cable-row','lat-pulldown'],
    legs:['db-squat','goblet-squat','reverse-lunge','db-rdl','bodyweight-squat','prisoner-squat','db-front-squat'],
    shoulders:['db-ohp','lateral-raise','pike-pushup','db-reverse-fly','face-pull'],
    core:['plank','dead-bug','crunch','side-plank','bird-dog']
  };
  const available=id=>exercises().some(e=>e.id===id)&&(!p.excluded||!p.excluded.includes(id))&&(typeof available132!=='function'||available132(id,p));
  for(const [di,d] of (plan.days||[]).entries()){
    const ids=[...(d.exercises||[])];
    const has=g=>ids.some(id=>rf145Groups(id).includes(g));
    for(const g of ['chest','back','legs','shoulders','core']){
      if(has(g))continue;
      const pool=(candidates[g]||[]).filter(available).filter(id=>!ids.includes(id));
      if(!pool.length)continue;
      const chosen=pool[di%pool.length];
      if(ids.length<7)ids.push(chosen);
      else{
        const replace=ids.findIndex(id=>!['chest','back','legs','shoulders','core'].some(x=>rf145Groups(id).includes(x)));
        if(replace>=0)ids[replace]=chosen;
      }
    }
    d.exercises=ids;
  }
  plan.reasons=[...(plan.reasons||[]),'1.4.5 fókusz: teljes testes A/B, kiemelt mell–váll–törzs munkával; hát és láb minden ciklusban megmarad.'];
  return plan;
};

statsScreen=function(){
  const hs=history(),wa=rf14WeeklyWeights(),vol=hs.reduce((s,h)=>s+rf14WorkoutVolume(h),0),mv=rf14MuscleVolume(),ms=rf145MuscleSets();
  render(shell(`<main><button class="btn secondary" onclick="go('home')">← Vissza</button><div class="hero"><h1>Statisztikák</h1><p>Edzés- és testsúlytrendek a helyi naplóból.</p></div><div class="grid2"><div class="stat"><small>Edzések</small><strong>${hs.length}</strong></div><div class="stat"><small>Súlyzós volumen</small><strong>${Math.round(vol)} kg·ism.</strong></div></div><div class="section">Heti testsúlyátlag</div>${wa.length?wa.slice(0,8).map(x=>`<div class="weight-row"><strong>${x.avg.toFixed(1)} kg</strong><div class="small muted">${x.week} • ${x.count} mérés</div></div>`).join(''):'<p class="muted">Még nincs elég testsúlyadat.</p>'}<div class="section">Munkasorozatok izomcsoportonként</div><p class="small muted">A testsúlyos és időalapú gyakorlatok is beleszámítanak.</p>${Object.entries(ms).sort((a,b)=>b[1]-a[1]).map(([k,v])=>`<div class="weight-row"><strong>${esc(rf145MuscleLabel(k))}</strong><div>${v} sorozat</div></div>`).join('')||'<p class="muted">Még nincs adat.</p>'}<div class="section">Súlyzós volumen izomcsoport szerint</div><p class="small muted">A kg × ismétlés csak terheléses gyakorlatok összevetésére alkalmas.</p>${Object.entries(mv).sort((a,b)=>b[1]-a[1]).map(([k,v])=>`<div class="weight-row"><strong>${esc(rf145MuscleLabel(k))}</strong><div>${Math.round(v)} kg·ism.</div></div>`).join('')||'<p class="muted">Még nincs adat.</p>'}</main>`));
};

const rf145ShellPrev=shell;
shell=function(content){return rf145ShellPrev(content).replaceAll('1.4.4','1.4.5');};
const rf145BackupPrev=makeBackup;
makeBackup=function(){const b={...rf145BackupPrev(),appVersion:RF145_VERSION};return b;};


// @endsection v145.js

// @section v146.js
var rf146EquipmentRank = function rf146EquipmentRank(id){if(RF146_BODYWEIGHT.has(id))return 2;if(RF146_BARBELL.has(id))return 1;return 0;};
var rf146Order = function rf146Order(ids){return [...ids].map((id,i)=>({id,i,r:rf146EquipmentRank(id)})).sort((a,b)=>a.r-b.r||a.i-b.i).map(x=>x.id);};
var rf146UpgradeHomePrograms = function rf146UpgradeHomePrograms(){
  if(db.get('homeEquipmentOrder146',false))return;
  const ps=programs(); let changed=false;
  for(const p of ps){
    if(p.builtin!==true||!['home-basic','home-level2','home-varied'].includes(p.id))continue;
    for(const d of p.days||[]){
      const source=p.id==='home-level2'&&RF146_LEVEL2[d.id]?RF146_LEVEL…162152 tokens truncated…-actions');
 if(!strip||!top||!source)return;
 let host=strip.querySelector?.('.tp152-brand-actions');
 if(!host){host=document.createElement('div');host.className='tp151-top-actions tp152-brand-actions';strip.appendChild(host)}
 host.innerHTML=source.innerHTML;
};
const tp152DeviceRenderBase=render;
render=function(custom){const r=tp152DeviceRenderBase(custom);tp152PolishHeader();return r};
tp152PolishHeader();
window.TrainPilot152.devicePolish=true;
// @endsection trainpilot-152-device-polish.js


// @section trainpilot-152-next-ui-functional.js
/* TrainPilot 1.5.2 follow-up UI / functional pass.
 * Scope: physical-device feedback only; no unrelated feature changes.
 */

Object.assign(RF200_THEMES,{
 classicBlue:{name:'Kék',accent:'#456f91',accent2:'#7297b3'},
 classicGreen:{name:'Zöld',accent:'#4d7860',accent2:'#769583'},
 classicRed:{name:'Piros',accent:'#965b5d',accent2:'#bb7a7d'},
 classicOrange:{name:'Narancs',accent:'#986a45',accent2:'#bd8d66'},
 classicPurple:{name:'Lila',accent:'#6d6087',accent2:'#9184a7'},
 graphite:{name:'Grafit',accent:'#68727f',accent2:'#929ba6'}
});
tp152ProgramAccent=function(p){
 const palette=['#557f9b','#64836e','#9b6764','#7b7092','#a07a53','#5f8582'];
 const key=String(p&& (p.id||p.name)||'trainpilot');let h=0;for(let i=0;i<key.length;i++)h=(h*31+key.charCodeAt(i))>>>0;
 return palette[h%palette.length];
};
rf200ApplyTheme();

var tp153ToastTimer=null;
var tp153Toast=function(message,ms){
 const value=ms==null?900:ms,text=String(message||'').trim();if(!text)return;
 let host=document.getElementById('tp153Toast');
 if(!host){host=document.createElement('div');host.id='tp153Toast';host.className='tp153-toast';host.setAttribute('role','status');host.setAttribute('aria-live','polite');document.body.appendChild(host)}
 host.textContent=text;host.classList.add('show');clearTimeout(tp153ToastTimer);
 tp153ToastTimer=setTimeout(function(){host.classList.remove('show');setTimeout(function(){if(!host.classList.contains('show'))host.remove()},180)},Math.max(500,Number(value)||900));
};
var tp153IsNonBlockingSuccess=function(message){
 const x=String(message||'').trim(),cloud=typeof tp149T==='function'?String(tp149T('cloud.connected')||'').trim():'';
 return ['A félbehagyott edzés törölve.','Az edzés törölve.','Visszatöltés kész.'].includes(x)||(cloud&&x===cloud);
};
const tp153AlertBase=(typeof window!=='undefined'&&typeof window.alert==='function')?window.alert.bind(window):function(){};
if(typeof window!=='undefined')window.alert=function(message){if(tp153IsNonBlockingSuccess(message)){tp153Toast(message);return}return tp153AlertBase(message)};

var tp153WorkoutVideo=function(id){
 const d=typeof demoInfo==='function'?demoInfo(id):null;if(!d)return '';
 const label=tp149T(d.provider?'video.openDemo':'video.openBrowser');
 return '<button type="button" class="btn secondary tp153-video-launch" onclick="openDemo(\''+esc(id)+'\')">'+rf260PlayIcon()+'<span>'+esc(label)+'</span></button>';
};

renderWorkout=function(){
 if(!state.session)return;
 persistDraft();
 const i=Math.max(0,Math.min(Number(state.current)||0,state.session.exercises.length-1)),se=state.session.exercises[i],e=byId(se.id)||se,total=state.session.exercises.length,pct=Math.round(((i+1)/Math.max(1,total))*100);
 const name=e&&e.custom?String(e.hu||e.en||e.id):tp149ExerciseName(e),note=e&&e.custom?String(e.notes||''):tp149ExerciseNote(e),target=(se.prescription&&se.prescription.reps)||(se.progressionPrescription&&se.progressionPrescription.reps)||e.reps||'',sets=se.sets||[],quick=typeof tp150IsQuick==='function'&&tp150IsQuick(),title=quick&&typeof tp150qwT==='function'?tp150qwT('title'):tp149WorkoutTitle();
 const rows=sets.map(function(s,si){
  const load=e.loadType==='bodyweight'?'<span class="small muted tp153-bodyweight">'+esc(tp149T('workout.bodyweight'))+'</span>':'<label class="small">'+esc(tp149LoadLabel(e.loadType))+'<input class="field" aria-label="'+esc(tp149LoadLabel(e.loadType))+'" inputmode="decimal" value="'+esc(s.weight==null?'':s.weight)+'" oninput="upd('+i+','+si+',\'weight\',this.value)"></label>';
  const unit=tp149RepUnit(e.repUnit);
  return '<div class="row tp153-set-row"><div class="num">'+(si+1)+'</div>'+load+'<label class="small">'+esc(unit)+'<input class="field" inputmode="numeric" aria-label="'+esc(unit)+'" placeholder="'+esc(target)+'" value="'+esc(s.reps==null?'':s.reps)+'" oninput="upd('+i+','+si+',\'reps\',this.value)"></label><button class="check '+(s.done?'done':'')+'" onclick="toggleSet('+i+','+si+')">'+(s.done?'✓':'OK')+'</button></div>';
 }).join('');
 const feedback=typeof rf152FeedbackHtml==='function'?rf152FeedbackHtml(se):'';
 const quickAdd=quick&&typeof tp150qwT==='function'?'<section class="card tp150-quick-add" id="tp150QuickAdd"><div class="tp150-quick-add-head"><strong>'+esc(tp150qwT('title'))+'</strong><span class="badge">'+esc(tp150qwT('exerciseCount',{count:state.session.exercises.length}))+'</span></div><button class="btn secondary block" onclick="tp150QuickPicker()">'+esc(tp150qwT('addAnother'))+'</button></section>':'';
 const demo=tp153WorkoutVideo(e.id),guide=(note||demo)?'<details class="tp153-workout-guide tp154-workout-guide"><summary><span class="tp154-workout-guide-title">▶ '+esc(tp149T('video.demoTitle'))+'</span><span class="tp152-chevron" aria-hidden="true">⌄</span></summary><div class="tp154-workout-guide-body">'+(note?'<p class="note tp153-workout-note">'+esc(note)+'</p>':'')+demo+'</div></details>':'';
 const body='<main class="tp149-workout tp153-workout"><section class="card tp153-workout-head"><div class="tp153-workout-topline"><span class="small muted">'+esc(title)+'</span><strong>'+(i+1)+'/'+total+'</strong></div><h1>'+esc(name)+'</h1><div class="tp153-workout-prescription"><span>'+esc(tp149T('workout.target'))+': <b>'+sets.length+' × '+esc(target)+'</b></span></div>'+guide+'<div class="progress"><div style="width:'+pct+'%"></div></div></section><div class="section tp153-sets-title">'+esc(tp149T('workout.sets'))+'</div>'+rows+feedback+quickAdd+'<div class="tp153-workout-actions"><button class="btn block" data-tp-workout-next onclick="nextExercise()">'+esc(tp149T(i===total-1?'workout.finish':'workout.next'))+'</button><button class="btn secondary block" onclick="prevExercise()" '+(i===0?'disabled':'')+'>'+esc(tp149T('workout.previous'))+'</button></div></main>';
 render(shell(body));
 if(typeof rf110DecorateTimedWorkout==='function')rf110DecorateTimedWorkout();
 if(typeof tp120AfterRender==='function')tp120AfterRender();
};

openDemo=function(id){
 const d=demoInfo(id);if(!d)return;
 if(navigator.onLine===false){alert(tp149T('video.offline'));return}
 if(!d.provider){openVideoLink(d.source);return}
 closeDemo();
 const e=byId(id),name=e?tp149ExerciseName(e):tp149T('video.demoTitle');
 const modal=document.createElement('div');modal.id='videoModal';modal.className='video-modal';
 const src=d.provider==='youtube'?'https://www.youtube.com/embed/'+d.videoId+'?playsinline=1&rel=0':'https://player.vimeo.com/video/'+d.videoId+'?playsinline=1';
 const external=d.provider==='youtube'?'https://www.youtube.com/watch?v='+d.videoId:d.source;
 modal.innerHTML='<div class="video-dialog" role="dialog" aria-modal="true" aria-label="'+esc(tp149T('video.dialogLabel'))+'"><button class="btn secondary video-close" data-tp-video-close type="button" onclick="closeDemo()">✕ '+esc(tp149T('common.close'))+'</button><h2>'+esc(name)+'</h2><div class="video-player"><iframe title="'+esc(name)+'" src="'+esc(src)+'" allow="autoplay; encrypted-media; fullscreen; picture-in-picture" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe></div><p class="small muted">'+esc(tp149T('video.source',{source:d.credit}))+'</p><button class="btn block" onclick="openVideoLink(\''+esc(external)+'\')">'+esc(tp149T('video.openExternal'))+'</button><button class="btn secondary block" onclick="openVideoLink(\''+esc(d.source)+'\')">'+esc(tp149T('video.sourceGuide'))+'</button></div>';
 document.body.appendChild(modal);modal.querySelector('[data-tp-video-close]')&&modal.querySelector('[data-tp-video-close]').focus();
};

var tp153OpenCoachTarget=function(){
 const p=rf233CoachPlan(),x=p.target;if(!x||!x.day)return go('calendar');
 if(state.session)return renderWorkout();
 if(typeof rf245CoachVisible!=='undefined')rf245CoachVisible=false;
 state.healthView=false;state.tab='plan';state.workout=x.day.id;render();window.scrollTo&&window.scrollTo(0,0);
 const dayName=x.program?tp149ProgramDayName(x.program,x.day):(x.day.name||x.day.id||'');
 const cards=[].slice.call(document.querySelectorAll('.tp152-day')),card=cards.find(function(el){return String(el.textContent||'').includes(dayName)})||cards.find(function(el){return String(el.textContent||'').includes(String(x.day.id||''))});
 if(card){cards.forEach(function(el){el.open=el===card});if(card.scrollIntoView)card.scrollIntoView({block:'center',behavior:'smooth'})}
};
var tp153CoachWhy=function(){const x=document.getElementById('tp153CoachWhy');if(x){if(x.scrollIntoView)x.scrollIntoView({block:'start',behavior:'smooth'});if(x.focus)x.focus({preventScroll:true})}};

var tp153CoachReturn='home';
rf233CoachScreen=function(){
 if(!state.healthView)tp153CoachReturn=state.tab==='health'?'health':'home';
 state.tab='health';state.healthView=true;if(typeof rf245CoachVisible!=='undefined')rf245CoachVisible=true;
 const t=rf233L(),p=rf233CoachPlan(),r=p.readiness,target=p.target,day=rf240Ledger()&&rf240Ledger().days?rf240Ledger().days[rf240DayKey(new Date())]||{}:{},name=target.day?(target.program?tp149ProgramDayName(target.program,target.day):(target.day.name||target.day.id||'—')):'—';
 const score=r.score==null?'—':tp149FormatNumber(r.score)+'/100';
 const action=target.day?'<button class="btn block" onclick="tp153OpenCoachTarget()">'+esc(tp149T('workout.start'))+'</button>':'<button class="btn secondary block" onclick="go(\'calendar\')">'+esc(t.calendar)+'</button>';
 render(shell('<main class="tp151-coach tp153-coach"><div class="tp151-back-row"><button class="btn secondary" onclick="go(\''+esc(tp153CoachReturn)+'\')">← '+esc(tp149T('common.back'))+'</button></div><div class="tp151-page-head"><h1>'+esc(t.coach)+'</h1></div><section class="card tp151-coach-recommendation"><div class="tp151-advice tp152-coach-advice"><span class="tp151-kicker">'+esc(tp151T('todayAdvice'))+'</span><strong>'+esc(p.decision.title)+'</strong><p>'+esc(p.decision.text)+'</p></div><div class="tp151-coach-target"><button type="button" class="tp153-coach-metric" onclick="tp153CoachWhy()"><small>'+esc(tp149T('coach.readiness'))+'</small><strong>'+esc(score)+'</strong></button><button type="button" class="tp153-coach-metric tp153-coach-next" onclick="tp153OpenCoachTarget()"><small>'+esc(rf233TargetTitle(target))+'</small><strong>'+esc(name)+'</strong></button></div><p class="small muted">'+esc(rf233TargetSub(target))+'</p>'+action+'</section><div class="card tp151-coach-context" id="tp153CoachWhy" tabindex="-1"><h2>'+esc(tp151T('why'))+'</h2><div class="rf233-signals">'+rf233SignalHtml(p)+'</div><div class="tp151-pulse"><span>'+esc(tp151T('pulse'))+'</span><strong>'+esc(tp151HeartLine(day))+'</strong></div><button class="btn secondary block" onclick="go(\'health\')">'+esc(tp149T('coach.openHealth'))+'</button></div><div class="section">'+esc(t.exercisePlan)+'</div>'+rf233ExerciseRows(p)+'<p class="small muted">'+esc(t.approx)+'</p>'+rf220ProgressHtml()+'</main>'));
};
rf220CoachScreen=rf233CoachScreen;

var tp153OpenRecovery=function(){
 const main=document.querySelector('main.rf263-health');if(!main)return;
 const details=[].slice.call(main.querySelectorAll('details')),d=details.find(function(x){return /Regenerációs előzmények|Recovery history|Regenerations|recuperare/i.test(String(x.querySelector('summary')&&x.querySelector('summary').textContent||''))});
 if(d){d.open=true;if(d.scrollIntoView)d.scrollIntoView({block:'start',behavior:'smooth'})}
};
var tp153EnhanceHealthLinks=function(){
 const main=document.querySelector('main.rf263-health');if(!main)return;
 main.querySelectorAll('.stat').forEach(function(stat){
  const label=String(stat.querySelector('small')&&stat.querySelector('small').textContent||'').trim();let action=null;
  if(/^(Testsúly|Body weight|Körpergewicht|Greutate)$/i.test(label))action='weight';
  else if(/^(Alvás|Sleep|Schlaf|Somn|HRV)$/i.test(label))action='recovery';
  if(!action)return;
  stat.classList.add('tp153-link-stat');stat.setAttribute('role','button');stat.setAttribute('tabindex','0');stat.dataset.tp153Link=action;
  const run=function(){return action==='weight'?rf215WeightScreen():tp153OpenRecovery()};
  stat.onclick=run;stat.onkeydown=function(e){if(e.key==='Enter'||e.key===' '){e.preventDefault();run()}};
 });
};

const tp153RenderBase=render;
render=function(custom){const r=tp153RenderBase(custom);tp153EnhanceHealthLinks();return r};

const tp153AndroidBackBase=window.TrainPilotAndroidBack;
window.TrainPilotAndroidBack=function(){
 try{if(document.getElementById('videoModal')){closeDemo();return true}return tp153AndroidBackBase()}
 catch(_){return tp153AndroidBackBase()}
};

(function(){
 const s=document.createElement('style');s.id='tp153FeedbackCss';
 s.textContent=[
 '.tp153-toast{position:fixed;left:50%;bottom:calc(22px + env(safe-area-inset-bottom,0px));z-index:20000;max-width:min(88vw,520px);transform:translate(-50%,12px);opacity:0;pointer-events:none;background:#222932;color:var(--text);border:1px solid var(--line);border-radius:12px;padding:9px 13px;font-weight:800;font-size:13px;box-shadow:0 10px 30px #0009;transition:opacity .16s ease,transform .16s ease}.tp153-toast.show{opacity:1;transform:translate(-50%,0)}',
 '.tp153-workout{padding-top:10px!important;padding-bottom:12px!important}.tp153-workout-head{padding:13px 15px!important;margin-bottom:10px!important}.tp153-workout-topline{display:flex;align-items:center;justify-content:space-between;gap:10px}.tp153-workout-topline>span{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.tp153-workout-topline>strong{font-size:19px;color:var(--accent2)}.tp153-workout-head h1{font-size:clamp(27px,7vw,38px);line-height:1.08;margin:7px 0 4px}.tp153-workout-prescription{font-size:14px;color:var(--accent2);margin-bottom:8px}.tp153-workout-head .progress{height:8px;margin-top:8px}',
 '.tp153-sets-title{margin:8px 0 7px!important;font-size:20px!important}.tp153-set-row{margin-bottom:7px!important}.tp153-workout #rf152Feedback{margin:9px 0!important;padding:11px!important}.tp153-workout #rf152Feedback p{margin:5px 0 8px!important}.tp153-workout-actions{display:grid;grid-template-columns:1.35fr .85fr;gap:8px;margin:9px 0}.tp153-workout-actions .btn{margin:0!important;min-height:44px}',
 '.tp153-workout-head .tp154-workout-guide{margin:6px 0 8px!important;padding:0!important;border:1px solid color-mix(in srgb,var(--accent) 24%,var(--line))!important;border-radius:12px!important;background:color-mix(in srgb,var(--accent) 4%,var(--card2))!important;overflow:hidden!important}.tp154-workout-guide>summary{list-style:none;display:flex;align-items:center;justify-content:space-between;gap:8px;min-height:36px;padding:5px 7px 5px 10px;cursor:pointer;user-select:none}.tp154-workout-guide>summary::-webkit-details-marker{display:none}.tp154-workout-guide-title{font-size:12px;font-weight:850;color:var(--accent2);letter-spacing:.01em}.tp154-workout-guide-body{padding:0 10px 9px;border-top:1px solid color-mix(in srgb,var(--accent) 12%,var(--line))}.tp153-workout-note{margin:8px 0 8px!important;font-size:12px;line-height:1.38}.tp153-video-launch{width:auto!important;min-width:0!important;min-height:32px!important;height:32px!important;border-radius:9px!important;padding:4px 9px!important;display:inline-flex!important;align-items:center!important;gap:7px!important;font-size:12px!important;font-weight:850!important;background:color-mix(in srgb,var(--accent) 9%,var(--card2))!important;border-color:color-mix(in srgb,var(--accent) 30%,var(--line))!important;color:var(--text)!important}.tp153-video-launch svg{width:16px;height:16px;fill:currentColor}',
 '.tp-play-btn,.tp-library-card .tp-play-btn,.tp146-exercise .tp-play-btn,.tp152-exercise .tp-play-btn,.tp152-quick-row .tp-play-btn{width:36px!important;height:28px!important;min-width:36px!important;min-height:28px!important;border-radius:8px!important;padding:0!important;background:color-mix(in srgb,var(--accent) 8%,var(--card2))!important;border-color:color-mix(in srgb,var(--accent) 36%,var(--line))!important;box-shadow:none!important}.tp-play-btn svg{width:16px!important;height:16px!important}',
 '.tp153-coach{width:min(100%,760px);margin-inline:auto}.tp153-coach .tp151-coach-recommendation,.rf235-health-coach{border-color:color-mix(in srgb,var(--accent) 44%,var(--line))!important}.tp153-coach-metric{appearance:none;border:0;background:rgba(255,255,255,.045);color:inherit;border-radius:12px;padding:9px;text-align:left;display:flex;flex-direction:column;gap:3px;font:inherit;cursor:pointer}.tp153-coach-metric strong{font-size:17px}.tp153-coach-metric:active,.tp153-link-stat:active{background:color-mix(in srgb,var(--accent) 10%,var(--card2))!important}.tp153-link-stat{cursor:pointer;outline:none}.tp153-link-stat:focus-visible,.tp153-coach-metric:focus-visible{outline:2px solid var(--accent);outline-offset:2px}',
 'html.tp-nav-compact .tp152-brand-actions{display:none!important}html.tp-nav-compact .top{display:flex!important;align-items:flex-start!important;gap:5px!important;padding:4px max(7px,env(safe-area-inset-right,0px)) 5px max(7px,env(safe-area-inset-left,0px))!important}html.tp-nav-compact .top>.tp151-nav-dock{order:1;flex:1 1 auto;min-width:0;margin:0!important;padding:3px!important;gap:1px!important}html.tp-nav-compact .top>.tp-sticky-actions-row{order:2;display:flex!important;flex:0 0 auto;width:auto!important;min-height:0!important;margin:0!important}html.tp-nav-compact .top>.tp-sticky-actions-row .tp151-top-actions{gap:3px!important}html.tp-nav-compact .top .tp151-nav-item{min-height:45px!important;padding:3px 1px!important;font-size:9px!important}html.tp-nav-compact .top .tp151-nav-icon{font-size:17px!important}html.tp-nav-compact .top .tp151-nav-label{font-size:8.5px!important}html.tp-nav-compact .top .tp151-top-icon{width:32px!important;height:32px!important;min-width:32px!important;border-radius:10px!important;padding:0!important}html:not(.tp-nav-compact) .tp152-brand-actions{display:flex!important}',
 'html[data-tp-theme-family="basic"] .btn:not(.secondary):not(.danger),html[data-tp-theme-family="basic"] .tab.active{filter:saturate(.78)!important}html[data-tp-theme-family="basic"] .tp151-nav-dock .active{box-shadow:none!important}',
 '@media(max-width:390px){.tp153-workout-head{padding:11px 12px!important}.tp153-workout-head h1{font-size:27px}.tp153-workout-actions{grid-template-columns:1fr 1fr}.tp153-set-row{grid-template-columns:48px minmax(0,1fr) minmax(0,1fr) 50px!important}html.tp-nav-compact .top .tp151-nav-label{font-size:8px!important}html.tp-nav-compact .top .tp151-nav-item{padding-inline:0!important}}'
 ].join('');
 document.head.appendChild(s);
})();
window.TrainPilot152NextUI={workoutCompact:true,videoBack:true,flatVideo:true,compactHeader:true,successToast:true,coachLinks:true,healthLinks:true,calmBasicThemes:true};
// @endsection trainpilot-152-next-ui-functional.js


// @section trainpilot-154-mobile-nav-polish.js
/* TrainPilot 1.5.4 mobile UI polish.
 * Physical-device feedback: 2x4 primary navigation, compact one-line brand,
 * contained video glyph and globally blended disclosure-chevron backgrounds.
 */
const TP154_UI_VERSION='1.5.4-dev';

var tp154DecorateChrome=function(){
 const strip=document.querySelector?.('.tp-brand-strip'),top=document.querySelector?.('.top');
 if(strip){
  strip.classList.add('tp154-brand-strip');
  const brand=strip.querySelector('.brand'),tag=strip.querySelector(':scope > .tag');
  if(brand)brand.classList.add('tp154-brand');
  if(tag)tag.classList.add('tp154-tagline');
 }
 if(!top)return;
 top.classList.add('tp154-nav-grid');
 const nav=top.querySelector('.tp151-nav-dock');
 if(nav){
  nav.classList.add('tp154-nav-source');
  [...nav.querySelectorAll('.tp151-nav-item')].forEach(function(btn,i){
   btn.classList.add('tp154-nav-cell');btn.style.order=String(i+1);
  });
 }
 const row=top.querySelector('.tp-sticky-actions-row'),actions=row?.querySelector('.tp151-top-actions')||top.querySelector('.tp151-top-actions');
 if(actions){
  actions.classList.add('tp154-action-source');
  const buttons=[...actions.querySelectorAll('.tp151-top-icon')];
  const coach=buttons.find(function(b){return !b.classList.contains('rf208-settings-btn')});
  const settings=buttons.find(function(b){return b.classList.contains('rf208-settings-btn')});
  if(coach){
   coach.classList.add('tp154-nav-cell','tp154-coach-action');coach.style.order='7';
   coach.innerHTML='<span class="tp154-nav-action-icon" aria-hidden="true">✦</span><span class="tp151-nav-label">'+esc(tp151T('coach'))+'</span>';
  }
  if(settings){
   settings.classList.add('tp154-nav-cell','tp154-settings-action');settings.style.order='8';
   settings.innerHTML='<span class="tp154-nav-action-icon" aria-hidden="true">⚙︎</span>';
  }
 }
};

const tp154RenderBase=render;
render=function(custom){const r=tp154RenderBase(custom);tp154DecorateChrome();return r};
tp154DecorateChrome();

(function(){
 const s=document.createElement('style');s.id='tp154MobilePolishCss';
 s.textContent=[
  '.tp-brand-strip.tp154-brand-strip{display:flex!important;align-items:baseline!important;gap:8px!important;min-width:0!important;padding-top:calc(8px + env(safe-area-inset-top,0px))!important;padding-bottom:6px!important}',
  '.tp154-brand-strip .tp-brand-only-row{display:contents!important}.tp154-brand-strip .tp154-brand{font-size:20px!important;line-height:1!important;letter-spacing:-.02em!important;white-space:nowrap!important}.tp154-brand-strip>.tp154-tagline{margin:0!important;min-width:0!important;flex:1 1 auto!important;font-size:11px!important;line-height:1.15!important;white-space:nowrap!important;overflow:hidden!important;text-overflow:ellipsis!important;opacity:.78!important}',
  'html .tp-brand-strip.tp154-brand-strip .tp152-brand-actions{display:none!important}',
  '.top.tp154-nav-grid{display:grid!important;grid-template-columns:repeat(4,minmax(0,1fr))!important;grid-auto-flow:row!important;gap:6px!important;margin-top:0!important;padding:7px max(9px,env(safe-area-inset-right,0px)) 8px max(9px,env(safe-area-inset-left,0px))!important;align-items:stretch!important}',
  '.top.tp154-nav-grid>.tp151-nav-dock,.top.tp154-nav-grid>.tp-sticky-actions-row,.top.tp154-nav-grid>.tp-sticky-actions-row>.tp151-top-actions{display:contents!important}',
  'html:not(.tp-nav-compact) .top.tp154-nav-grid>.tp-sticky-actions-row,html.tp-nav-compact .top.tp154-nav-grid>.tp-sticky-actions-row{display:contents!important}',
  '.top.tp154-nav-grid .tp154-nav-cell{width:auto!important;height:auto!important;min-width:0!important;min-height:52px!important;margin:0!important;border-radius:13px!important;padding:5px 3px!important;display:flex!important;flex-direction:column!important;align-items:center!important;justify-content:center!important;gap:3px!important;font:inherit!important;text-align:center!important}',
  '.top.tp154-nav-grid .tp151-nav-icon,.top.tp154-nav-grid .tp154-nav-action-icon{font-size:18px!important;line-height:1!important}.top.tp154-nav-grid .tp151-nav-label{font-size:10px!important;line-height:1.05!important;font-weight:780!important;white-space:nowrap!important;overflow:hidden!important;text-overflow:ellipsis!important;max-width:100%!important}',
  '.top.tp154-nav-grid .tp154-coach-action{background:var(--card2)!important;color:var(--text)!important;border:1px solid var(--line)!important;box-shadow:none!important}.top.tp154-nav-grid .tp154-coach-action.active{background:color-mix(in srgb,var(--accent) 20%,var(--card2))!important;border-color:color-mix(in srgb,var(--accent) 55%,var(--line))!important;color:var(--accent2)!important}',
  '.top.tp154-nav-grid .tp154-settings-action{background:var(--card2)!important;color:var(--text)!important;border:1px solid var(--line)!important;box-shadow:none!important}.top.tp154-nav-grid .tp154-settings-action.active{background:color-mix(in srgb,var(--accent) 20%,var(--card2))!important;border-color:color-mix(in srgb,var(--accent) 55%,var(--line))!important;color:var(--accent2)!important}.top.tp154-nav-grid .tp154-settings-action .tp154-nav-action-icon{font-size:22px!important}',
  'html.tp-nav-compact .top.tp154-nav-grid{display:grid!important;grid-template-columns:repeat(4,minmax(0,1fr))!important;gap:4px!important;padding-top:5px!important;padding-bottom:6px!important}html.tp-nav-compact .top.tp154-nav-grid .tp154-nav-cell{min-height:45px!important;padding:3px 2px!important}html.tp-nav-compact .top.tp154-nav-grid .tp151-nav-label{font-size:9px!important}html.tp-nav-compact .top.tp154-nav-grid .tp151-nav-icon,html.tp-nav-compact .top.tp154-nav-grid .tp154-nav-action-icon{font-size:17px!important}',
  '.tp-play-btn.tp152-play{display:inline-grid!important;place-items:center!important;overflow:hidden!important}.tp-play-btn.tp152-play .tp152-play-glyph{display:grid!important;place-items:center!important;width:22px!important;height:20px!important;min-width:22px!important;min-height:20px!important;border-radius:7px!important;font-size:11px!important;line-height:1!important;padding-left:1px!important;box-shadow:none!important}',
  ':where(.tp152-chevron,.rf103-history-chevron,.rf-history-health-chevron,.tp1511-training summary i){display:inline-grid!important;place-items:center!important;box-sizing:border-box!important;padding:0!important;width:28px!important;height:28px!important;min-width:28px!important;min-height:28px!important;max-width:28px!important;flex:0 0 28px!important;border:0!important;border-radius:10px!important;box-shadow:none!important;background:linear-gradient(90deg,transparent 0%,color-mix(in srgb,var(--accent) 7%,transparent) 38%,color-mix(in srgb,var(--accent) 14%,var(--card2)) 100%)!important;color:var(--accent2)!important;font-size:20px!important;line-height:1!important}',
  '.tp152-active-program>summary>.tp152-chevron,.tp152-program-chevron,.tp146-exercise-chevron{width:28px!important;height:28px!important;min-width:28px!important;border:0!important;background:linear-gradient(90deg,transparent 0%,color-mix(in srgb,var(--accent) 7%,transparent) 38%,color-mix(in srgb,var(--accent) 14%,var(--card2)) 100%)!important;box-shadow:none!important}',
  '.tp-library-card>summary::after{display:grid!important;place-items:center!important;width:28px!important;height:28px!important;min-width:28px!important;flex-basis:28px!important;border:0!important;border-radius:10px!important;box-shadow:none!important;background:linear-gradient(90deg,transparent 0%,color-mix(in srgb,var(--accent) 7%,transparent) 38%,color-mix(in srgb,var(--accent) 14%,var(--card2)) 100%)!important;color:var(--accent2)!important;font-size:20px!important}',
  '.tp-select-trigger::after{display:grid!important;place-items:center!important;width:28px!important;height:28px!important;min-width:28px!important;border-radius:10px!important;background:linear-gradient(90deg,transparent 0%,color-mix(in srgb,var(--accent) 7%,transparent) 38%,color-mix(in srgb,var(--accent) 14%,var(--card2)) 100%)!important;color:var(--accent2)!important;font-size:18px!important;box-shadow:none!important}',
  '.timer{pointer-events:none!important}.timer .btn{pointer-events:auto!important}',
  '@media(max-width:390px){.tp154-brand-strip .tp154-brand{font-size:19px!important}.tp154-brand-strip>.tp154-tagline{font-size:10px!important}.top.tp154-nav-grid{gap:5px!important;padding-inline:7px!important}.top.tp154-nav-grid .tp154-nav-cell{min-height:50px!important}.top.tp154-nav-grid .tp151-nav-label{font-size:9.5px!important}}'
 ].join('');
 document.head.appendChild(s);
})();

window.TrainPilot154UI={version:TP154_UI_VERSION,twoByFourNav:true,inlineBrand:true,containedVideoGlyph:true,blendedChevrons:true};
// @endsection trainpilot-154-mobile-nav-polish.js

// @section trainpilot-155-nav-consistency.js
/* TrainPilot 1.5.5 navigation consistency / disclosure polish. */
const TP155_UI_VERSION='1.5.5';
(function(){
 const s=document.createElement('style');s.id='tp155NavConsistencyCss';
 s.textContent=[
  '.top.tp154-nav-grid{grid-template-columns:repeat(4,minmax(0,1fr))!important;grid-auto-rows:minmax(56px,auto)!important;gap:7px!important;padding:7px max(9px,env(safe-area-inset-right,0px)) 8px max(9px,env(safe-area-inset-left,0px))!important}',
  '.top.tp154-nav-grid .tp154-nav-cell,.top.tp154-nav-grid .tp151-top-icon.tp154-nav-cell{box-sizing:border-box!important;width:100%!important;min-width:0!important;max-width:none!important;height:auto!important;min-height:56px!important;margin:0!important;padding:6px 4px!important;border-radius:14px!important;justify-self:stretch!important;align-self:stretch!important;display:flex!important;flex-direction:column!important;align-items:center!important;justify-content:center!important;gap:4px!important}',
  '.top.tp154-nav-grid .tp151-nav-icon,.top.tp154-nav-grid .tp154-nav-action-icon{font-size:20px!important;line-height:1!important}',
  '.top.tp154-nav-grid .tp151-nav-label{font-size:clamp(11.5px,3vw,13px)!important;line-height:1.08!important;font-weight:800!important;white-space:normal!important;overflow:visible!important;text-overflow:clip!important;display:-webkit-box!important;-webkit-box-orient:vertical!important;-webkit-line-clamp:2!important;line-clamp:2!important;max-width:100%!important;min-height:1.08em!important;text-align:center!important;overflow-wrap:anywhere!important;hyphens:auto!important}',
  '.top.tp154-nav-grid .tp154-settings-action .tp154-nav-action-icon{font-size:23px!important}',
  'html.tp-nav-compact .top.tp154-nav-grid{grid-template-columns:repeat(4,minmax(0,1fr))!important;grid-auto-rows:minmax(56px,auto)!important;gap:7px!important;padding:7px max(9px,env(safe-area-inset-right,0px)) 8px max(9px,env(safe-area-inset-left,0px))!important}',
  'html.tp-nav-compact .top.tp154-nav-grid .tp154-nav-cell,html.tp-nav-compact .top.tp154-nav-grid .tp151-top-icon.tp154-nav-cell{box-sizing:border-box!important;width:100%!important;min-width:0!important;max-width:none!important;height:auto!important;min-height:56px!important;margin:0!important;padding:6px 4px!important;border-radius:14px!important;justify-self:stretch!important;align-self:stretch!important}',
  'html.tp-nav-compact .top.tp154-nav-grid .tp151-nav-label{font-size:clamp(11.5px,3vw,13px)!important;line-height:1.08!important}html.tp-nav-compact .top.tp154-nav-grid .tp151-nav-icon,html.tp-nav-compact .top.tp154-nav-grid .tp154-nav-action-icon{font-size:20px!important}html.tp-nav-compact .top.tp154-nav-grid .tp154-settings-action .tp154-nav-action-icon{font-size:23px!important}',
  '.top.tp154-nav-grid>.tp151-nav-dock,.top.tp154-nav-grid>.tp-sticky-actions-row,.top.tp154-nav-grid>.tp-sticky-actions-row>.tp151-top-actions,html.tp-nav-compact .top.tp154-nav-grid>.tp-sticky-actions-row,html.tp-nav-compact .top.tp154-nav-grid>.tp-sticky-actions-row>.tp151-top-actions{display:contents!important}',
  'html body :is(.tp152-active-program>summary>.tp152-chevron,.tp152-program-card>summary>.tp152-program-chevron,.tp152-program-day>summary>.tp152-chevron,.tp152-day>summary .tp152-chevron,.tp152-exercise>summary .tp152-chevron,.tp146-exercise>summary .tp146-exercise-chevron,.tp152-quick-row>summary .tp152-chevron,.tp152-inline-add>summary>.tp152-chevron,.tp152-accordion>summary>.tp152-chevron,.rf103-history>summary .rf103-history-chevron,.rf-history-health-toggle .rf-history-health-chevron,.tp1511-training>summary i){box-sizing:border-box!important;display:inline-grid!important;place-items:center!important;width:18px!important;height:18px!important;min-width:18px!important;min-height:18px!important;max-width:18px!important;max-height:18px!important;flex:0 0 18px!important;margin-left:6px!important;padding:0!important;border:0!important;border-radius:0!important;background:none!important;box-shadow:none!important;color:transparent!important;font-size:0!important;line-height:0!important;transform:none!important;overflow:visible!important}',
  'html body details.tp146-exercise[open]>summary .tp146-exercise-chevron,html body details.tp152-exercise[open]>summary .tp152-chevron,html body details.tp152-day[open]>summary .tp152-chevron,html body details.tp152-active-program[open]>summary>.tp152-chevron,html body details.tp152-program-card[open]>summary>.tp152-program-chevron,html body details.tp152-program-day[open]>summary>.tp152-chevron,html body details.tp152-quick-row[open]>summary .tp152-chevron,html body details.tp152-inline-add[open]>summary>.tp152-chevron,html body details.tp152-accordion[open]>summary>.tp152-chevron,html body details.tp1511-training[open]>summary i,html body details.rf263-history[open]>summary .rf103-history-chevron{transform:none!important}',
  'html body :is(.tp152-active-program>summary>.tp152-chevron,.tp152-program-card>summary>.tp152-program-chevron,.tp152-program-day>summary>.tp152-chevron,.tp152-day>summary .tp152-chevron,.tp152-exercise>summary .tp152-chevron,.tp146-exercise>summary .tp146-exercise-chevron,.tp152-quick-row>summary .tp152-chevron,.tp152-inline-add>summary>.tp152-chevron,.tp152-accordion>summary>.tp152-chevron,.rf103-history>summary .rf103-history-chevron,.rf-history-health-toggle .rf-history-health-chevron,.tp1511-training>summary i)::before{content:""!important;display:block!important;width:9px!important;height:12px!important;background:var(--text)!important;clip-path:polygon(0 0,100% 50%,0 100%)!important;transform:rotate(0deg)!important;transform-origin:50% 50%!important;transition:transform .15s ease!important}',
  'html body details[open]>summary :is(.tp152-chevron,.tp152-program-chevron,.tp146-exercise-chevron,.rf103-history-chevron,.tp1511-training summary i)::before{transform:rotate(90deg)!important}',
  'html body .rf-history-health-toggle[aria-expanded="true"] .rf-history-health-chevron::before{transform:rotate(90deg)!important}html body .rf-history-health-toggle[aria-expanded="false"] .rf-history-health-chevron::before{transform:rotate(0deg)!important}',
  'html body .tp-library-card>summary::after{content:""!important;box-sizing:border-box!important;display:block!important;width:9px!important;height:12px!important;min-width:9px!important;min-height:12px!important;max-width:9px!important;max-height:12px!important;flex:0 0 9px!important;margin-left:8px!important;padding:0!important;border:0!important;border-radius:0!important;background:var(--text)!important;box-shadow:none!important;clip-path:polygon(0 0,100% 50%,0 100%)!important;transform:rotate(0deg)!important;transform-origin:50% 50%!important;transition:transform .15s ease!important}html body .tp-library-card[open]>summary::after{transform:rotate(90deg)!important}',
  'html body .tp-select-trigger::after{content:""!important;box-sizing:border-box!important;display:block!important;width:9px!important;height:12px!important;min-width:9px!important;min-height:12px!important;max-width:9px!important;max-height:12px!important;flex:0 0 9px!important;padding:0!important;border:0!important;border-radius:0!important;background:var(--text)!important;box-shadow:none!important;clip-path:polygon(0 0,100% 50%,0 100%)!important;transform:rotate(0deg)!important;transform-origin:50% 50%!important;transition:transform .15s ease!important}html body .tp-select.open .tp-select-trigger::after{transform:rotate(90deg)!important}',
  'html body .rf148-profile-accordion>summary::after{content:""!important;box-sizing:border-box!important;display:inline-block!important;float:right!important;width:9px!important;height:12px!important;min-width:9px!important;min-height:12px!important;max-width:9px!important;max-height:12px!important;margin:3px 0 0 8px!important;padding:0!important;border:0!important;border-radius:0!important;background:var(--text)!important;box-shadow:none!important;clip-path:polygon(0 0,100% 50%,0 100%)!important;transform:rotate(0deg)!important;transform-origin:50% 50%!important;transition:transform .15s ease!important}html body .rf148-profile-accordion[open]>summary::after{transform:rotate(90deg)!important}',
  '.tp151-calendar>.tp151-page-head,.tp151-health>.tp151-page-head{border:1px solid var(--line)!important;border-radius:16px!important;background:color-mix(in srgb,var(--card) 82%,transparent)!important;padding:10px 12px!important;margin:3px 0 10px!important}',
  '.tp152-program-card>summary{grid-template-columns:minmax(0,1fr) 38px!important}.tp152-program-day>summary{grid-template-columns:minmax(0,1fr) auto 38px!important}.tp152-quick-row>summary{grid-template-columns:44px minmax(0,1fr) 38px!important}',
  '@media(max-width:390px){.top.tp154-nav-grid,html.tp-nav-compact .top.tp154-nav-grid{gap:6px!important;padding-inline:7px!important;grid-auto-rows:minmax(54px,auto)!important}.top.tp154-nav-grid .tp154-nav-cell,html.tp-nav-compact .top.tp154-nav-grid .tp154-nav-cell{min-height:54px!important}.top.tp154-nav-grid .tp151-nav-label,html.tp-nav-compact .top.tp154-nav-grid .tp151-nav-label{font-size:11.5px!important}}'
 ].join('');
 document.head.appendChild(s);
})();
window.TrainPilot155UI={version:TP155_UI_VERSION,stableTwoByFourNav:true,largerNavLabels:true,unifiedDisclosure:true};
// @endsection trainpilot-155-nav-consistency.js

// @section trainpilot-155-coach-health-polish.js
/* TrainPilot 1.5.5: 2026-09-20 Health panel alignment + concise per-exercise Coach guidance. */
(function(){
 'use strict';
 const TP155_TODAY_POLISH_VERSION='1.5.5-2026-09-20';
 const COPY={
  hu:{coach:'Coach javaslat',target:'Célterhelés:',reps:'Cél: +1–2 szabályos ismétlés/sorozat.',hold:'Tartsd a jelenlegi terhelést; célozz +1 ismétlést.',light:'Kezdj könnyebben; ma ne erőltesd a progressziót.',pain:'Ne emelj; csak fájdalommentes végrehajtással folytasd.',start:'Kezdj kontrollált, kényelmes terheléssel.'},
  en:{coach:'Coach suggestion',target:'Target load:',reps:'Target: +1–2 clean reps per set.',hold:'Keep the current load; aim for +1 rep.',light:'Start lighter; do not force progression today.',pain:'Do not increase; continue only pain-free.',start:'Start with a controlled, comfortable load.'},
  de:{coach:'Coach-Hinweis',target:'Zielbelastung:',reps:'Ziel: +1–2 saubere Wdh. pro Satz.',hold:'Belastung halten; +1 Wdh. anstreben.',light:'Leichter starten; heute keine Progression erzwingen.',pain:'Nicht steigern; nur schmerzfrei fortsetzen.',start:'Kontrolliert mit angenehmer Belastung starten.'},
  ro:{coach:'Sugestie Coach',target:'Sarcină țintă:',reps:'Țintă: +1–2 repetări corecte pe serie.',hold:'Păstrează sarcina; urmărește +1 repetare.',light:'Începe mai ușor; nu forța progresia azi.',pain:'Nu crește sarcina; continuă doar fără durere.',start:'Începe controlat, cu o sarcină confortabilă.'},
  sk:{coach:'Odporúčanie Coach',target:'Cieľová záťaž:',reps:'Cieľ: +1–2 čisté opakovania na sériu.',hold:'Zachovaj záťaž; cieľ je +1 opakovanie.',light:'Začni ľahšie; dnes netlač progresiu.',pain:'Nezvyšuj záťaž; pokračuj iba bez bolesti.',start:'Začni kontrolovane s pohodlnou záťažou.'},
  pl:{coach:'Sugestia Coach',target:'Obciążenie docelowe:',reps:'Cel: +1–2 poprawne powtórzenia na serię.',hold:'Utrzymaj obciążenie; celuj w +1 powtórzenie.',light:'Zacznij lżej; dziś nie wymuszaj progresji.',pain:'Nie zwiększaj obciążenia; kontynuuj tylko bez bólu.',start:'Zacznij kontrolowanie, z komfortowym obciążeniem.'}
 };
 var lang=function(){try{return typeof rf212Lang==='function'?rf212Lang():'hu'}catch(_){return 'hu'}};
 var copy=function(){return COPY[lang()]||COPY.hu};
 window.tp155CoachShortAdvice=function(id,decision,existing){
  var advice=existing||null,rec=null;
  try{if(!advice&&typeof rf233ExerciseAdvice==='function')advice=rf233ExerciseAdvice(id,decision||rf233Decision(rf220Readiness()))}catch(_){}
  try{if(typeof rf152Recommendation==='function')rec=rf152Recommendation(id)}catch(_){}
  var action=advice&&advice.action||rec&&rec.action||'start',c=copy(),text=c.start;
  if(action==='pain')text=c.pain;
  else if(action==='easy')text=c.light;
  else if(action==='increase'){
   var w=Number(rec&&rec.weight);
   if(Number.isFinite(w)&&w>0){
    var unit='';try{unit=tp149LoadLabel((byId(id)||{}).loadType)||''}catch(_){}
    text=c.target+' '+String(Math.round(w*10)/10)+(unit?' '+unit:'');
   }else text=c.reps;
  }else if(action==='reps')text=c.reps;
  else if(action==='hold')text=c.hold;
  return {action:action,label:advice&&advice.label||'',text:text};
 };

 if(typeof rf233ExerciseRows==='function'){
  rf233ExerciseRows=function(plan){
   var t=rf233L();if(!plan||!plan.exercises||!plan.exercises.length)return '<p class="muted">'+esc(t.unknown)+'</p>';
   return plan.exercises.map(function(x){
    var a=window.tp155CoachShortAdvice(x.id,plan.decision,x.advice);
    return '<div class="card rf233-ex tp155-coach-ex"><div class="rf233-exhead"><strong>'+esc(x.name)+'</strong><span class="badge">'+esc(a.label)+'</span></div><div class="small muted tp155-coach-short">'+esc(a.text)+'</div></div>';
   }).join('');
  };
 }

 window.tp155NormalizeHealthBottomPanels=function(){
  var main=document.querySelector('main.rf263-health');if(!main)return;
  var panels=[].slice.call(main.querySelectorAll(':scope > details.card')).slice(-3);
  panels.forEach(function(panel){panel.classList.add('tp151-details','tp155-health-bottom-panel')});
 };
 if(typeof rf263HealthHub==='function'){
  const tp155HealthHubBase=rf263HealthHub;
  rf263HealthHub=function(){var r=tp155HealthHubBase.apply(this,arguments);window.tp155NormalizeHealthBottomPanels();return r};
 }

 window.tp155InjectWorkoutCoach=function(){
  var main=document.querySelector('main.tp153-workout'),head=main&&main.querySelector('.tp153-workout-head');
  if(!head||head.querySelector('.tp155-workout-coach')||!state.session)return;
  var i=Math.max(0,Math.min(Number(state.current)||0,(state.session.exercises||[]).length-1)),se=(state.session.exercises||[])[i];if(!se)return;
  var decision=null,advice=null;try{decision=rf233Decision(rf220Readiness());advice=rf233ExerciseAdvice(se.id,decision)}catch(_){}
  var a=window.tp155CoachShortAdvice(se.id,decision,advice),c=copy(),box=document.createElement('div');
  box.className='tp155-workout-coach';
  box.innerHTML='<span class="tp155-workout-coach-kicker">'+esc(c.coach)+'</span>'+(a.label?'<strong>'+esc(a.label)+'</strong>':'')+'<p>'+esc(a.text)+'</p>';
  var anchor=head.querySelector('.tp153-workout-prescription');if(anchor)anchor.insertAdjacentElement('afterend',box);else head.appendChild(box);
 };
 if(typeof renderWorkout==='function'){
  const tp155RenderWorkoutBase=renderWorkout;
  renderWorkout=function(){var r=tp155RenderWorkoutBase.apply(this,arguments);window.tp155InjectWorkoutCoach();return r};
 }

 const style=document.createElement('style');style.id='tp155TodayPolishCss';style.textContent=[
  'main.rf263-health>details.tp155-health-bottom-panel{box-sizing:border-box!important;width:100%!important;max-width:none!important;margin-left:0!important;margin-right:0!important;padding:10px!important;align-self:stretch!important}',
  'main.rf263-health>details.tp155-health-bottom-panel>summary{box-sizing:border-box!important;width:100%!important;min-height:30px!important;margin:0!important;padding:0!important;align-items:center!important}',
  '.tp155-coach-short{line-height:1.32!important}',
  '.tp155-workout-coach{margin:9px 0 2px;padding:9px 10px;border:1px solid color-mix(in srgb,var(--accent) 34%,var(--line));border-radius:12px;background:color-mix(in srgb,var(--accent) 10%,var(--card2));display:grid;gap:2px}',
  '.tp155-workout-coach-kicker{font-size:10px;font-weight:850;letter-spacing:.07em;text-transform:uppercase;color:var(--accent)}',
  '.tp155-workout-coach strong{font-size:13px}.tp155-workout-coach p{margin:0;font-size:12px;line-height:1.32;color:var(--muted)}'
 ].join('');
 document.head.appendChild(style);
 window.TrainPilotTodayPolish={version:TP155_TODAY_POLISH_VERSION,healthPanels:true,shortCoach:true,workoutCoach:true};
})();
// @endsection trainpilot-155-coach-health-polish.js

// @section trainpilot-155-home-calendar-round2.js
/* TrainPilot 1.5.5 test round 2:
 * - highlighted compact two-column active-program card on Home
 * - single, larger Calendar day-selection hint
 */
(function(){
 'use strict';
 const TP155_ROUND2='1.5.5-test-round2';

 var tp155Round2NextHtml=function(){
  try{
   const p=activeProgram(),np=typeof nextPlanned==='function'?nextPlanned():null,next=typeof nextWorkout==='function'?nextWorkout():null;
   const nextProgram=np?programById(np.programId):p;
   const nextDay=np?programDay(nextProgram,np.dayId):p?programDay(p,next):null;
   if(!nextProgram||!nextDay)return '';
   const title=typeof tp146ProgramDayTitle==='function'?tp146ProgramDayTitle(nextProgram,nextDay):((nextProgram.name||'')+' • '+(nextDay.name||nextDay.id||''));
   const meta=np&&np.start?tp149FormatDateTime(np.start):tp149Plural('exercise.count',(nextDay.exercises||[]).length);
   const label=typeof tp149T==='function'?tp149T('home.nextLabel'):'Következő';
   return '<div class="tp146-next tp155-home-next"><span class="small muted">'+esc(label)+'</span><strong class="tp146-next-title">'+esc(title)+'</strong><span class="small muted tp146-next-time">'+esc(meta)+'</span></div>';
  }catch(_){return ''}
 };

 window.tp155Round2EnhanceHome=function(){
  if(state.tab!=='home')return;
  const main=document.querySelector('main.rf221-home');if(!main)return;
  const hero=main.querySelector(':scope > .hero');if(!hero)return;
  hero.classList.add('tp155-home-active-card');

  let grid=hero.querySelector(':scope > .tp155-home-active-grid');
  if(!grid){
   const badge=hero.querySelector(':scope > .badge');
   const title=hero.querySelector(':scope > h1');
   if(!title)return;

   const left=document.createElement('div');left.className='tp155-home-program-left';
   if(badge)left.appendChild(badge);
   left.appendChild(title);

   let next=hero.querySelector(':scope > .tp146-next');
   if(!next){
    const directMuted=[...hero.children].find(function(el){return el.classList&&el.classList.contains('muted')&&el!==title});
    if(directMuted){next=directMuted;next.classList.add('tp155-home-next')}
   }
   if(!next){
    const wrap=document.createElement('div');wrap.innerHTML=tp155Round2NextHtml();next=wrap.firstElementChild;
   }
   const right=document.createElement('div');right.className='tp155-home-program-right';
   if(next)right.appendChild(next);

   grid=document.createElement('div');grid.className='tp155-home-active-grid';
   grid.appendChild(left);grid.appendChild(right);
   hero.insertBefore(grid,hero.firstChild);
   [...hero.children].forEach(function(el){if(el.tagName==='BR')el.remove()});
  }
 };

 if(typeof tp151CalendarDayPanel==='function'){
  const tp155Round2CalendarDayPanelBase=tp151CalendarDayPanel;
  tp151CalendarDayPanel=function(){
   const date=typeof rf2211Selected==='function'?rf2211Selected():(state.rf2211CalendarDate||'');
   if(!date)return '';
   return tp155Round2CalendarDayPanelBase.apply(this,arguments);
  };
 }

 const tp155Round2RenderBase=render;
 render=function(custom){
  const out=tp155Round2RenderBase.apply(this,arguments);
  window.tp155Round2EnhanceHome();
  return out;
 };

 const style=document.createElement('style');style.id='tp155Round2Css';style.textContent=[
  'main.rf221-home>.hero.tp155-home-active-card{box-sizing:border-box!important;border:2px solid color-mix(in srgb,var(--tp-program-accent,var(--accent)) 72%,var(--line))!important;background:color-mix(in srgb,var(--tp-program-accent,var(--accent)) 8%,var(--card))!important;border-radius:18px!important;padding:10px 12px!important}',
  '.tp155-home-active-grid{display:grid!important;grid-template-columns:minmax(0,1.08fr) minmax(0,.92fr)!important;gap:12px!important;align-items:center!important;width:100%!important}',
  '.tp155-home-program-left,.tp155-home-program-right{min-width:0!important}',
  '.tp155-home-program-left h1{margin:4px 0 0!important;font-size:clamp(22px,5.9vw,29px)!important;line-height:1.08!important}',
  '.tp155-home-program-left>.badge{border-color:color-mix(in srgb,var(--tp-program-accent,var(--accent)) 72%,var(--line))!important;background:color-mix(in srgb,var(--tp-program-accent,var(--accent)) 16%,var(--card2))!important}',
  '.tp155-home-program-right{border-left:1px solid color-mix(in srgb,var(--tp-program-accent,var(--accent)) 42%,var(--line))!important;padding-left:11px!important}',
  '.tp155-home-program-right .tp146-next{margin:0!important;padding:0!important;min-height:0!important}',
  '.tp155-home-program-right .tp146-next-title{display:block!important;font-size:18px!important;line-height:1.16!important;margin:2px 0!important}',
  '.tp155-home-program-right .tp146-next-time{display:block!important;font-size:11px!important;line-height:1.2!important}',
  'main.rf221-home>.hero.tp155-home-active-card>.btn{margin-top:8px!important;min-height:40px!important}',
  'main.tp151-calendar>.tp151-page-head p{font-size:16px!important;line-height:1.32!important;font-weight:750!important;color:var(--text)!important;opacity:.86!important;margin-top:5px!important}',
  '@media(max-width:350px){.tp155-home-active-grid{gap:8px!important}.tp155-home-program-right{padding-left:8px!important}.tp155-home-program-left h1{font-size:21px!important}.tp155-home-program-right .tp146-next-title{font-size:13px!important}}'
 ].join('');
 document.head.appendChild(style);

 window.TrainPilot155Round2={version:TP155_ROUND2,homeActiveProgram:true,calendarHint:true};
})();
// @endsection trainpilot-155-home-calendar-round2.js

// @section trainpilot-155-theme-family-polish.js
/* TrainPilot 1.5.5 Round 2: Basic themes are calm/flat; colored selection frames belong to Neon themes. */
(function(){
 'use strict';

 if(typeof TP1511_TX==='object'){
  TP1511_TX.sk=Object.assign({},TP1511_TX.sk||{},{vivid:'Neónové farby',themeHelp:'Základné farby zostávajú čisté a nenápadné; Neónové farby používajú výraznejšie farebné zvýraznenia.'});
  TP1511_TX.pl=Object.assign({},TP1511_TX.pl||{},{vivid:'Kolory neonowe',themeHelp:'Kolory podstawowe pozostają spokojne i proste; Kolory neonowe używają mocniejszych kolorowych wyróżnień.'});
 }

 Object.assign(RF200_THEMES,{
  classicBlue:{name:'Kék',accent:'#4f7db8',accent2:'#7ea5d3'},
  classicGreen:{name:'Zöld',accent:'#4f8a67',accent2:'#78ad8d'},
  classicRed:{name:'Piros',accent:'#b65b5b',accent2:'#d18181'},
  classicOrange:{name:'Narancs',accent:'#b87845',accent2:'#d49a68'},
  classicPurple:{name:'Lila',accent:'#76659a',accent2:'#9a8ab9'},
  graphite:{name:'Grafit',accent:'#778291',accent2:'#a2abb8'}
 });

 const tp155ThemeApplyBase=rf200ApplyTheme;
 rf200ApplyTheme=function(k=rf200ThemeKey()){
  const r=tp155ThemeApplyBase.apply(this,arguments);
  const el=typeof document!=='undefined'?document.documentElement:null;
  const family=TP1511_BASIC.includes(k)?'basic':'vivid';
  if(el?.dataset)el.dataset.tpThemeFamily=family;
  else el?.setAttribute?.('data-tp-theme-family',family);
  return r;
 };

 const style=document.createElement('style');style.id='tp155ThemeFamilyCss';style.textContent=[
  'html[data-tp-theme-family="basic"] :is(.card,.setting,.tp151-nav-dock,.tp151-calendar-frame,.tp151-day-panel,.tp152-active-program,.tp152-day,.tp152-program-card,.tp152-program-day,.tp152-exercise,.tp152-quick-row,.tp152-inline-add,.tp152-accordion){box-shadow:none!important;border-color:var(--line)!important}',
  'html[data-tp-theme-family="basic"] body main.rf221-home>.hero.tp155-home-active-card{border:1px solid var(--line)!important;background:var(--card)!important;box-shadow:none!important}',
  'html[data-tp-theme-family="basic"] .tp155-home-program-right{border-left-color:var(--line)!important}',
  'html[data-tp-theme-family="basic"] :is(.tp152-chevron,.tp146-exercise-chevron,.rf103-history-chevron,.rf-history-health-chevron,.tp1511-training summary i){border-color:transparent!important;background:transparent!important;box-shadow:none!important;color:var(--text)!important}',
  'html[data-tp-theme-family="basic"] :is(.tp-library-card .tp-play-btn,.tp146-exercise .tp-play-btn,.tp153-video-launch){border-color:var(--line)!important;background:var(--card2)!important;box-shadow:none!important;filter:none!important}',
  'html[data-tp-theme-family="basic"] :is(.tp151-nav-item.active,.tp151-top-icon.active,.tp-selected,[aria-pressed="true"]){box-shadow:none!important;filter:none!important;border-color:transparent!important}',
  'html[data-tp-theme-family="basic"] .tp151-nav-item.active{background:var(--card2)!important}',
  'html[data-tp-theme-family="basic"] .tp1511-theme-chip.active{border-color:var(--line)!important;box-shadow:none!important;background:color-mix(in srgb,var(--text) 7%,var(--card2))!important}',
  'html[data-tp-theme-family="basic"] :focus-visible{outline-color:var(--text)!important;box-shadow:none!important}',
  'html[data-tp-theme-family="vivid"] body main.rf221-home>.hero.tp155-home-active-card{box-shadow:0 0 18px color-mix(in srgb,var(--accent) 10%,transparent)!important}',
  'html[data-tp-theme-family="vivid"] .tp1511-theme-chip.active{border-color:var(--accent)!important;box-shadow:inset 0 0 0 1px var(--accent)!important}'
 ].join('');
 document.head.appendChild(style);

 rf200ApplyTheme(rf200ThemeKey());
 window.TrainPilot155ThemeFamily={version:'1.5.5-round2',basicFlat:true,neonHighlights:true};
})();
// @endsection trainpilot-155-theme-family-polish.js

// @section trainpilot-155-round3-ui-polish.js
/* TrainPilot 1.5.5 Round 3 test polish
 * - brighter matte Basic palette, no accent glow
 * - natural no-scroll Home when content fits
 * - larger nav iconography without taller 2x4 cells
 * - inline Journal date filter
 * - one-tap language dropdown
 * - upward time picker with blurred backdrop
 */
(function(){
 'use strict';
 const TP155_R3_VERSION='1.5.5-round3-test';

 // Brighter but deliberately non-neon Basic palette.
 Object.assign(RF200_THEMES,{
  classicBlue:{name:'Kék',accent:'#4d8fbe',accent2:'#7cb4d6'},
  classicGreen:{name:'Zöld',accent:'#4d946a',accent2:'#7ab18d'},
  classicRed:{name:'Piros',accent:'#b95f63',accent2:'#d58a8d'},
  classicOrange:{name:'Narancs',accent:'#c07d45',accent2:'#d9a06e'},
  classicPurple:{name:'Lila',accent:'#8567b1',accent2:'#a58cc9'},
  graphite:{name:'Grafit',accent:'#687989',accent2:'#96a4b1'},
  classicTeal:{name:'Türkiz',accent:'#3f918a',accent2:'#75b6b1'},
  classicRose:{name:'Rózsaszín',accent:'#b45f82',accent2:'#d38aa5'},
  classicAmber:{name:'Arany',accent:'#ae843c',accent2:'#cca45f'},
  classicCyan:{name:'Cián',accent:'#4c8fa5',accent2:'#77b0c0'}
 });
 if(typeof TP1511_BASIC!=='undefined'){
  TP1511_BASIC.splice(0,TP1511_BASIC.length,'classicBlue','classicGreen','classicRed','classicOrange','classicPurple','graphite','classicTeal','classicRose','classicAmber','classicCyan');
 }
 if(typeof TP1511_BASIC_NAMES==='object')Object.assign(TP1511_BASIC_NAMES,{
  classicTeal:{hu:'Türkiz',en:'Teal',de:'Türkis',ro:'Turcoaz',sk:'Tyrkysová',pl:'Turkusowy'},
  classicRose:{hu:'Rózsaszín',en:'Rose',de:'Rosé',ro:'Roz',sk:'Ružová',pl:'Różowy'},
  classicAmber:{hu:'Arany',en:'Amber',de:'Bernstein',ro:'Chihlimbar',sk:'Jantárová',pl:'Bursztynowy'},
  classicCyan:{hu:'Cián',en:'Cyan',de:'Cyan',ro:'Cian',sk:'Azúrová',pl:'Cyjan'}
 });

 // One-tap language selector: no outer accordion and no redundant system-language help.
 rf212LanguagePanel=function(){
  const cur=rf212LangSetting(),labels={system:tp1511T('system'),hu:'Magyar',en:'English',de:'Deutsch',ro:'Română',sk:'Slovenčina',pl:'Polski'};
  return '<div class="setting rf212-language tp1511-card tp155-language-direct"><div class="tp1511-head tp155-language-head"><b>'+(TP1511_FLAGS[cur]||'🌐')+'</b><div class="tp155-language-copy"><strong>'+esc(tp1511T('lang'))+'</strong><select class="field tp1511-lang tp155-language-select" aria-label="'+esc(tp1511T('lang'))+'" onchange="rf212SetLang(this.value)">'+Object.keys(TP1511_FLAGS).map(function(k){return '<option value="'+k+'" '+(cur===k?'selected':'')+'>'+(TP1511_FLAGS[k]||'')+' '+esc(labels[k]||k)+'</option>'}).join('')+'</select></div></div></div>';
 };

 // Journal: one expandable inline calendar for an exact-date filter.
 const TP155_R3_COPY={
  hu:{filter:'Szűrő dátum szerint',clear:'Szűrő törlése',today:'Ma'},
  en:{filter:'Filter by date',clear:'Clear filter',today:'Today'},
  de:{filter:'Nach Datum filtern',clear:'Filter löschen',today:'Heute'},
  ro:{filter:'Filtrare după dată',clear:'Șterge filtrul',today:'Astăzi'},
  sk:{filter:'Filtrovať podľa dátumu',clear:'Zrušiť filter',today:'Dnes'},
  pl:{filter:'Filtruj według daty',clear:'Wyczyść filtr',today:'Dzisiaj'}
 };
 var tp155R3Copy=function(){const l=typeof rf212Lang==='function'?rf212Lang():'hu';return TP155_R3_COPY[l]||TP155_R3_COPY.hu};
 var tp155JournalLocale=function(){try{return typeof rf233Locale==='function'?rf233Locale():'hu-HU'}catch(_){return 'hu-HU'}};
 var tp155JournalSelected=function(){return rf263HistoryFilter.from&&rf263HistoryFilter.from===rf263HistoryFilter.to?rf263HistoryFilter.from:''};
 var tp155JournalMonthStart=function(){
  const selected=rf263ParseDate(tp155JournalSelected()),stored=rf263ParseDate(state.tp155JournalMonth||'');
  const d=stored||selected||new Date();return new Date(d.getFullYear(),d.getMonth(),1,12);
 };
 var tp155JournalStoreMonth=function(d){state.tp155JournalMonth=rf263DateKey(new Date(d.getFullYear(),d.getMonth(),1,12))};
 window.tp155JournalFilterToggle=function(el){state.tp155JournalFilterOpen=!!el?.open};
 window.tp155JournalMoveMonth=function(delta){
  const m=tp155JournalMonthStart(),next=new Date(m.getFullYear(),m.getMonth()+Number(delta||0),1,12);tp155JournalStoreMonth(next);state.tp155JournalFilterOpen=true;render(historyScreen());
 };
 window.tp155JournalSetMonth=function(value,open=true){
  const d=rf263ParseDate(value);if(!d)return;tp155JournalStoreMonth(d);state.tp155JournalFilterOpen=!!open;render(historyScreen());
 };
 window.tp155JournalPickDate=function(value){
  if(value&&!rf263ParseDate(value))return;
  rf263HistoryFilter={from:value||'',to:value||''};state.tp155JournalFilterOpen=false;
  if(value){const d=rf263ParseDate(value);tp155JournalStoreMonth(d)}
  render(historyScreen());
 };
 rf263ClearHistoryFilter=function(){rf263HistoryFilter={from:'',to:''};state.tp155JournalFilterOpen=false;delete state.tp155JournalMonth;render(historyScreen())};
 var tp155JournalCalendarHtml=function(){
  const c=tp155R3Copy(),m=tp155JournalMonthStart(),y=m.getFullYear(),mo=m.getMonth(),selected=tp155JournalSelected(),today=rf263DateKey(new Date()),locale=tp155JournalLocale();
  const first=(new Date(y,mo,1,12).getDay()+6)%7,count=new Date(y,mo+1,0,12).getDate();
  let days='<span aria-hidden="true"></span>'.repeat(first);
  for(let d=1;d<=count;d++){
   const date=new Date(y,mo,d,12),value=rf263DateKey(date);
   days+='<button type="button" class="tp155-journal-day" data-date="'+value+'" aria-label="'+esc(date.toLocaleDateString(locale,{year:'numeric',month:'long',day:'numeric',weekday:'long'}))+'" aria-pressed="'+(value===selected?'true':'false')+'" '+(value===today?'aria-current="date"':'')+' onclick="event.preventDefault();event.stopPropagation();tp155JournalPickDate(\''+value+'\')">'+d+'</button>';
  }
  return '<div class="tp155-journal-calendar"><div class="tp155-journal-month"><button type="button" class="btn secondary" onclick="event.preventDefault();event.stopPropagation();tp155JournalMoveMonth(-1)">‹</button><strong>'+esc(m.toLocaleDateString(locale,{year:'numeric',month:'long'}))+'</strong><button type="button" class="btn secondary" onclick="event.preventDefault();event.stopPropagation();tp155JournalMoveMonth(1)">›</button></div><div class="tp155-journal-week" aria-hidden="true">'+['H','K','Sze','Cs','P','Szo','V'].map(function(x){return '<span>'+x+'</span>'}).join('')+'</div><div class="tp155-journal-days">'+days+'</div><div class="tp155-journal-actions"><button type="button" class="btn secondary" onclick="event.preventDefault();event.stopPropagation();rf263ClearHistoryFilter()" '+(selected?'':'disabled')+'>'+esc(c.clear)+'</button><button type="button" class="btn secondary" onclick="event.preventDefault();event.stopPropagation();tp155JournalPickDate(\''+today+'\')">'+esc(c.today)+'</button></div></div>';
 };
 historyScreen=function(){
  const all=history(),filtered=rf263FilteredHistory(),selected=tp155JournalSelected(),c=tp155R3Copy(),locale=tp155JournalLocale(),selectedLabel=selected?rf263ParseDate(selected).toLocaleDateString(locale):'';
  const filter='<details class="card tp155-journal-filter" '+(state.tp155JournalFilterOpen?'open':'')+' ontoggle="tp155JournalFilterToggle(this)"><summary><div><strong>'+esc(c.filter)+'</strong>'+(selectedLabel?'<span class="small muted">'+esc(selectedLabel)+'</span>':'')+'</div><span class="tp152-chevron" aria-hidden="true">⌄</span></summary>'+tp155JournalCalendarHtml()+'</details><p class="small muted rf263-history-count">'+esc(tp149T('journal.count',{shown:filtered.length,total:all.length}))+'</p>';
  const rows=filtered.length?filtered.map(function(row,i){return rf263HistoryItem(row.x,row.index,i)}).join(''):(all.length?'<div class="card muted">'+esc(tp149T('journal.noneRange'))+'</div>':'<div class="muted">'+esc(tp149T('journal.none'))+'</div>');
  return shell('<main class="tp150-journal-compact">'+filter+rows+'</main>');
 };

 // Time selector: scrollable lists, always opens upward from the trigger, blurred content behind it.
 window.tp155R3SetTime=function(part,value,button){
  rf260TemporalSetTime(part,Number(value),button);
  const p=rf260TemporalPicker,b=document.getElementById('tp155R3TimeSave');if(b&&p)b.textContent=rf260TemporalText().save+' • '+String(p.hour).padStart(2,'0')+':'+String(p.minute).padStart(2,'0');
 };
 rf260TemporalPaintTime=function(){
  const p=rf260TemporalPicker;if(!p)return;const t=rf260TemporalText(),hours=Array.from({length:24},function(_,i){return '<button type="button" class="tp155-time-option" data-tp-hour="'+i+'" aria-pressed="'+(i===p.hour?'true':'false')+'" onclick="tp155R3SetTime(\'hour\','+i+',this)">'+String(i).padStart(2,'0')+'</button>'}).join(''),mins=Array.from({length:60},function(_,i){return '<button type="button" class="tp155-time-option" data-tp-minute="'+i+'" aria-pressed="'+(i===p.minute?'true':'false')+'" onclick="tp155R3SetTime(\'minute\','+i+',this)">'+String(i).padStart(2,'0')+'</button>'}).join('');
  p.modal.classList.add('tp155-time-upward');
  p.modal.innerHTML='<div class="tp-modal-card tp-temporal-card tp155-time-card" onclick="event.stopPropagation()"><div class="tp-modal-head"><h2 id="tpTemporalTitle">'+esc(p.type==='datetime-local'?t.datetime:t.time)+'</h2><button type="button" class="btn secondary tp-modal-close" aria-label="'+esc(t.cancel)+'" onclick="rf260TemporalClose()">×</button></div>'+(p.type==='datetime-local'?'<button type="button" class="btn secondary block tp-temporal-back" onclick="rf260TemporalPicker.stage=\'date\';rf260TemporalPaintDate()">← '+esc(t.back)+'</button>':'')+'<div class="tp155-time-head"><strong>'+esc(t.hour)+'</strong><strong>'+esc(t.minute)+'</strong></div><div class="tp155-time-lists"><div class="tp155-time-list">'+hours+'</div><div class="tp155-time-list">'+mins+'</div></div><div class="tp-temporal-actions"><button type="button" class="btn secondary" onclick="rf260TemporalClose()">'+esc(t.cancel)+'</button><button id="tp155R3TimeSave" type="button" class="btn" onclick="rf260TemporalSaveTime()">'+esc(t.save)+' • '+String(p.hour).padStart(2,'0')+':'+String(p.minute).padStart(2,'0')+'</button></div></div>';
  requestAnimationFrame(function(){const h=p.modal.querySelector('[data-tp-hour][aria-pressed="true"]'),m=p.modal.querySelector('[data-tp-minute][aria-pressed="true"]');h?.scrollIntoView?.({block:'center'});m?.scrollIntoView?.({block:'center'});tp152PositionTemporal()});
 };
 tp152PositionTemporal=function(){
  const p=rf260TemporalPicker,card=p?.modal?.querySelector?.('.tp155-time-card,.tp152-time-card');if(!p||!card)return;const r=p.trigger?.getBoundingClientRect?.();if(!r)return;
  const gap=8,w=Math.min(380,Math.max(260,innerWidth-24)),maxH=Math.max(240,Math.min(innerHeight-24,620));
  card.style.width=w+'px';card.style.maxHeight=maxH+'px';card.style.overflowY='auto';card.style.position='fixed';card.style.margin='0';
  const left=Math.max(12,Math.min(innerWidth-w-12,r.left+r.width/2-w/2));card.style.left=left+'px';
  const h=Math.min(card.scrollHeight,maxH),top=Math.max(12,Math.min(innerHeight-h-12,r.top-h-gap));card.style.top=top+'px';
 };

 const style=document.createElement('style');style.id='tp155Round3Css';style.textContent=[
  /* Natural Home height: scroll exists only when content genuinely exceeds the viewport. */
  'html body.tp-home-view main.rf221-home{height:auto!important;min-height:0!important;max-height:none!important;overflow:visible!important}',

  /* Larger glyphs/text, identical 2x4 cell height. Hungarian label fit is the current target; other languages remain a later audit. */
  '.top.tp154-nav-grid .tp151-nav-icon,.top.tp154-nav-grid .tp154-nav-action-icon{font-size:22px!important}',
  '.top.tp154-nav-grid .tp154-settings-action .tp154-nav-action-icon{font-size:24px!important}',
  'html[lang="hu"] .top.tp154-nav-grid .tp151-nav-label{font-size:13.5px!important;line-height:1.04!important;font-weight:850!important}',
  'html body .top.tp154-nav-grid,html.tp-nav-compact body .top.tp154-nav-grid{grid-auto-rows:56px!important}',
  'html body .top.tp154-nav-grid .tp154-nav-cell,html body .top.tp154-nav-grid .tp151-top-icon.tp154-nav-cell,html.tp-nav-compact body .top.tp154-nav-grid .tp154-nav-cell,html.tp-nav-compact body .top.tp154-nav-grid .tp151-top-icon.tp154-nav-cell{height:56px!important;min-height:56px!important;max-height:56px!important}',
  '@media(max-width:390px){html body .top.tp154-nav-grid,html.tp-nav-compact body .top.tp154-nav-grid{grid-auto-rows:54px!important}html body .top.tp154-nav-grid .tp154-nav-cell,html body .top.tp154-nav-grid .tp151-top-icon.tp154-nav-cell,html.tp-nav-compact body .top.tp154-nav-grid .tp154-nav-cell,html.tp-nav-compact body .top.tp154-nav-grid .tp151-top-icon.tp154-nav-cell{height:54px!important;min-height:54px!important;max-height:54px!important}html[lang="hu"] .top.tp154-nav-grid .tp151-nav-label{font-size:12.5px!important}.top.tp154-nav-grid .tp151-nav-icon,.top.tp154-nav-grid .tp154-nav-action-icon{font-size:21px!important}}',

  /* Basic themes: matte accent borders/selections remain, only glow is removed. */
  'html[data-tp-theme-family="basic"] :is(.card,.setting,.tp151-nav-dock){box-shadow:none!important}',
  'html[data-tp-theme-family="basic"] body main.rf221-home>.hero.tp155-home-active-card{border:2px solid color-mix(in srgb,var(--accent) 66%,var(--line))!important;background:color-mix(in srgb,var(--accent) 7%,var(--card))!important;box-shadow:none!important}',
  'html[data-tp-theme-family="basic"] .tp155-home-program-right{border-left-color:color-mix(in srgb,var(--accent) 42%,var(--line))!important}',
  'html[data-tp-theme-family="basic"] :is(.btn.tp-primary,.btn.tp-emphasis,.tab.active,.tp151-nav-item.active,.tp151-top-icon.active,.tp-action-active,.cal-cell.tp-selected-day,.tp-select.open .tp-select-trigger,.tp1511-theme-chip.active,.rf130-labels .btn[aria-pressed="true"],.tp-temporal-day[aria-pressed="true"],.tp-time-option[aria-pressed="true"],.tp155-time-option[aria-pressed="true"]){box-shadow:none!important;filter:none!important}',
  'html[data-tp-theme-family="basic"] :is(.btn[aria-pressed="true"],.btn.tp-selected,.tp-select-option[aria-selected="true"],.rf232-effort .btn:not(.secondary),.rf232-pain:not(.secondary)){box-shadow:none!important;filter:none!important;border-color:color-mix(in srgb,var(--accent) 64%,var(--line))!important;background:color-mix(in srgb,var(--accent) 16%,var(--card2))!important;color:var(--text)!important}',
  'html[data-tp-theme-family="basic"] .rf208-settings-btn{box-shadow:none!important;border-color:color-mix(in srgb,var(--accent) 52%,var(--line))!important;background:color-mix(in srgb,var(--accent) 12%,var(--card2))!important}',
  'html[data-tp-theme-family="basic"] :is(.tp151-nav-item.active,.tp151-top-icon.active,.tp-action-active,.tp1511-theme-chip.active,.tp-select.open .tp-select-trigger){border:1px solid color-mix(in srgb,var(--accent) 64%,var(--line))!important}',
  'html[data-tp-theme-family="basic"] :is(.tp151-nav-item.active,.tp151-top-icon.active,.tp-action-active,.tp1511-theme-chip.active){background:color-mix(in srgb,var(--accent) 16%,var(--card2))!important;color:var(--text)!important}',
  'html[data-tp-theme-family="basic"] .cal-cell.tp-selected-day{border-color:var(--accent)!important;background:color-mix(in srgb,var(--accent) 17%,var(--card2))!important}',
  'html[data-tp-theme-family="basic"] :is(.tp-temporal-day[aria-pressed="true"],.tp-time-option[aria-pressed="true"],.tp155-time-option[aria-pressed="true"],.rf130-labels .btn[aria-pressed="true"]){background:color-mix(in srgb,var(--accent) 26%,var(--card2))!important;border-color:var(--accent)!important;color:var(--text)!important}',
  'html[data-tp-theme-family="basic"] :is(.tp152-active-program,.tp152-day,.tp152-program-card,.tp152-program-day,.tp152-exercise,.tp152-quick-row,.tp152-inline-add,.tp152-history-editor){box-shadow:none!important}',
  'html[data-tp-theme-family="basic"] :is(.tp152-active-program,.tp152-program-card){border-color:color-mix(in srgb,var(--tp-program-accent,var(--accent)) 58%,var(--line))!important}',
  'html[data-tp-theme-family="basic"] :is(.tp152-day,.tp152-exercise,.tp152-program-day,.tp152-quick-row,.tp152-inline-add){border-color:color-mix(in srgb,var(--tp-program-accent,var(--accent)) 34%,var(--line))!important}',

  /* Direct language dropdown. */
  '.tp155-language-direct{overflow:visible!important}.tp155-language-head{align-items:center!important}.tp155-language-copy{min-width:0;flex:1}.tp155-language-copy>strong{display:block;margin-bottom:5px}.tp155-language-direct .tp-select{margin:0!important}.tp155-language-direct .tp-select-trigger{min-height:42px!important}',

  /* Inline Journal calendar. */
  '.tp155-journal-filter{padding:0!important;overflow:hidden;margin:0 0 8px!important}.tp155-journal-filter>summary{list-style:none;display:flex;align-items:center;justify-content:space-between;gap:10px;padding:12px 13px;cursor:pointer}.tp155-journal-filter>summary::-webkit-details-marker{display:none}.tp155-journal-filter>summary>div{display:flex;flex-direction:column;gap:2px}.tp155-journal-calendar{padding:0 12px 12px;border-top:1px solid var(--line)}',
  '.tp155-journal-month{display:grid;grid-template-columns:42px minmax(0,1fr) 42px;gap:8px;align-items:center;margin:10px 0}.tp155-journal-month strong{text-align:center}.tp155-journal-month .btn{min-width:42px;min-height:42px;padding:6px}',
  '.tp155-journal-week,.tp155-journal-days{display:grid;grid-template-columns:repeat(7,minmax(0,1fr));gap:4px;text-align:center}.tp155-journal-week{font-size:11px;font-weight:800;color:var(--muted);margin-bottom:5px}.tp155-journal-day{min-width:0;min-height:40px;border:1px solid var(--line);border-radius:10px;background:var(--card2);color:var(--text);font:inherit;font-weight:750;padding:0}.tp155-journal-day[aria-current="date"]{border-color:color-mix(in srgb,var(--accent) 62%,var(--line))}.tp155-journal-day[aria-pressed="true"]{background:color-mix(in srgb,var(--accent) 24%,var(--card2));border-color:var(--accent);font-weight:900}',
  '.tp155-journal-actions{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:10px}.tp155-journal-actions .btn{min-height:42px}.tp150-journal-compact>.rf263-history-count{margin:4px 2px 10px!important}',

  /* Upward time list with blurred backdrop. */
  '.tp-temporal-modal.tp155-time-upward{background:rgba(4,7,11,.58)!important;backdrop-filter:blur(9px)!important;-webkit-backdrop-filter:blur(9px)!important}',
  '.tp155-time-card{transform-origin:bottom center}.tp155-time-head{display:grid;grid-template-columns:1fr 1fr;gap:10px;text-align:center;color:var(--muted);margin:2px 0 6px;font-size:12px;font-weight:850}.tp155-time-lists{display:grid;grid-template-columns:1fr 1fr;gap:10px}.tp155-time-list{display:grid;grid-template-columns:1fr;gap:4px;max-height:min(42dvh,300px);overflow:auto;overscroll-behavior:contain;padding:3px;border:1px solid var(--line);border-radius:13px;background:var(--card2)}.tp155-time-option{min-height:40px;border:1px solid transparent;border-radius:9px;background:var(--card);color:var(--text);font:inherit;font-weight:850}.tp155-time-option[aria-pressed="true"]{background:color-mix(in srgb,var(--accent) 28%,var(--card2));border-color:var(--accent)}'
 ].join('');
 document.head.appendChild(style);

 // Re-apply current theme after the Basic palette changes.
 rf200ApplyTheme(rf200ThemeKey());
 window.TrainPilot155Round3={version:TP155_R3_VERSION,matteBasic:true,expandedBasicPalette:true,naturalHomeHeight:true,largerHuNav:true,inlineJournalDate:true,directLanguage:true,upwardTimePicker:true,pendingI18nNavAudit:true,pendingDropdownStyleAudit:true};
})();
// @endsection trainpilot-155-round3-ui-polish.js

// @section trainpilot-155-round3-journal-range.js
/* TrainPilot 1.5.5 Round 3 phone fix: Journal two-date inclusive range filter. */
(function(){
 'use strict';
 const COPY={
  hu:{filter:'Szűrő dátum szerint',from:'Ettől',to:'Eddig',clear:'Szűrő törlése',today:'Ma',pickEnd:'Válaszd ki az időszak végét'},
  en:{filter:'Filter by date',from:'From',to:'To',clear:'Clear filter',today:'Today',pickEnd:'Select the end of the range'},
  de:{filter:'Nach Datum filtern',from:'Von',to:'Bis',clear:'Filter löschen',today:'Heute',pickEnd:'Ende des Zeitraums auswählen'},
  ro:{filter:'Filtrare după dată',from:'De la',to:'Până la',clear:'Șterge filtrul',today:'Astăzi',pickEnd:'Selectează sfârșitul intervalului'},
  sk:{filter:'Filtrovať podľa dátumu',from:'Od',to:'Do',clear:'Zrušiť filter',today:'Dnes',pickEnd:'Vyber koniec rozsahu'},
  pl:{filter:'Filtruj według daty',from:'Od',to:'Do',clear:'Wyczyść filtr',today:'Dzisiaj',pickEnd:'Wybierz koniec zakresu'}
 };
 const lang=()=>typeof rf212Lang==='function'?rf212Lang():'hu';
 const copy=()=>COPY[lang()]||COPY.hu;
 const locale=()=>{try{return typeof rf233Locale==='function'?rf233Locale():'hu-HU'}catch(_){return 'hu-HU'}};
 const valid=value=>!!(value&&typeof rf263ParseDate==='function'&&rf263ParseDate(value));
 const committed=()=>({from:rf263HistoryFilter?.from||'',to:rf263HistoryFilter?.to||''});
 const pending=()=>state.tp155JournalRangeStart||'';
 const monthStart=()=>{
  const stored=valid(state.tp155JournalMonth)?rf263ParseDate(state.tp155JournalMonth):null;
  const seed=stored||(valid(pending())?rf263ParseDate(pending()):null)||(valid(rf263HistoryFilter?.from)?rf263ParseDate(rf263HistoryFilter.from):null)||new Date();
  return new Date(seed.getFullYear(),seed.getMonth(),1,12);
 };
 const storeMonth=d=>{state.tp155JournalMonth=rf263DateKey(new Date(d.getFullYear(),d.getMonth(),1,12))};
 const fmt=value=>{const d=valid(value)?rf263ParseDate(value):null;return d?d.toLocaleDateString(locale(),{year:'numeric',month:'2-digit',day:'2-digit'}):'—'};

 window.tp155R4JournalMoveMonth=function(delta){
  const m=monthStart(),next=new Date(m.getFullYear(),m.getMonth()+Number(delta||0),1,12);
  storeMonth(next);state.tp155JournalFilterOpen=true;render(historyScreen());
 };
 window.tp155R4JournalPickDate=function(value){
  if(!valid(value))return;
  const start=pending(),active=committed();
  storeMonth(rf263ParseDate(value));
  if(!start||(active.from&&active.to)){
   state.tp155JournalRangeStart=value;
   rf263HistoryFilter={from:'',to:''};
   state.tp155JournalFilterOpen=true;
  }else{
   let from=start,to=value;
   if(to<from){const tmp=from;from=to;to=tmp}
   rf263HistoryFilter={from:from,to:to};
   delete state.tp155JournalRangeStart;
   state.tp155JournalFilterOpen=true;
  }
  render(historyScreen());
 };
 window.tp155R4JournalToday=function(){
  const value=rf263DateKey(new Date()),start=pending();
  storeMonth(new Date());
  if(start){
   let from=start,to=value;if(to<from){const tmp=from;from=to;to=tmp}
   rf263HistoryFilter={from:from,to:to};delete state.tp155JournalRangeStart;
  }else{
   rf263HistoryFilter={from:value,to:value};delete state.tp155JournalRangeStart;
  }
  state.tp155JournalFilterOpen=true;render(historyScreen());
 };
 window.tp155R4JournalClear=function(){
  rf263HistoryFilter={from:'',to:''};delete state.tp155JournalRangeStart;delete state.tp155JournalMonth;
  state.tp155JournalFilterOpen=false;render(historyScreen());
 };
 rf263ClearHistoryFilter=window.tp155R4JournalClear;

 const calendarHtml=function(){
  const c=copy(),m=monthStart(),y=m.getFullYear(),mo=m.getMonth(),today=rf263DateKey(new Date()),a=committed(),start=pending();
  const from=start||a.from||'',to=start?'':a.to||'';
  const first=(new Date(y,mo,1,12).getDay()+6)%7,count=new Date(y,mo+1,0,12).getDate(),weekdays=typeof tp149Weekdays==='function'?tp149Weekdays():['H','K','Sze','Cs','P','Szo','V'];
  let days='<span aria-hidden="true"></span>'.repeat(first);
  for(let d=1;d<=count;d++){
   const date=new Date(y,mo,d,12),value=rf263DateKey(date),isStart=value===from,isEnd=!!to&&value===to,inRange=!!(from&&to&&value>from&&value<to);
   const cls='tp155-journal-day'+(isStart?' is-start':'')+(isEnd?' is-end':'')+(inRange?' is-range':'');
   days+='<button type="button" class="'+cls+'" data-date="'+value+'" aria-label="'+esc(date.toLocaleDateString(locale(),{year:'numeric',month:'long',day:'numeric',weekday:'long'}))+'" aria-pressed="'+((isStart||isEnd)?'true':'false')+'" '+(value===today?'aria-current="date"':'')+' onclick="event.preventDefault();event.stopPropagation();tp155R4JournalPickDate(\''+value+'\')">'+d+'</button>';
  }
  const active=!!(start||a.from||a.to);
  return '<div class="tp155-journal-calendar tp155-r4-range-calendar">'+
   '<div class="tp155-journal-range-head"><div><span>'+esc(c.from)+'</span><strong>'+esc(fmt(from))+'</strong></div><div><span>'+esc(c.to)+'</span><strong>'+esc(fmt(to))+'</strong></div></div>'+
   (start?'<p class="small muted tp155-journal-range-help">'+esc(c.pickEnd)+'</p>':'')+
   '<div class="tp155-journal-month"><button type="button" class="btn secondary" onclick="event.preventDefault();event.stopPropagation();tp155R4JournalMoveMonth(-1)">‹</button><strong>'+esc(m.toLocaleDateString(locale(),{year:'numeric',month:'long'}))+'</strong><button type="button" class="btn secondary" onclick="event.preventDefault();event.stopPropagation();tp155R4JournalMoveMonth(1)">›</button></div>'+
   '<div class="tp155-journal-week" aria-hidden="true">'+weekdays.map(x=>'<span>'+esc(x)+'</span>').join('')+'</div><div class="tp155-journal-days">'+days+'</div>'+
   '<div class="tp155-journal-actions"><button type="button" class="btn secondary" onclick="event.preventDefault();event.stopPropagation();tp155R4JournalClear()" '+(active?'':'disabled')+'>'+esc(c.clear)+'</button><button type="button" class="btn secondary" onclick="event.preventDefault();event.stopPropagation();tp155R4JournalToday()">'+esc(c.today)+'</button></div></div>';
 };

 historyScreen=function(){
  const all=history(),filtered=rf263FilteredHistory(),a=committed(),start=pending(),c=copy(),from=start||a.from||'',to=start?'':a.to||'';
  const status=(from||to)?'<span class="small muted tp155-journal-range-summary">'+esc(c.from)+': '+esc(fmt(from))+' • '+esc(c.to)+': '+esc(fmt(to))+'</span>':'';
  const filter='<details class="card tp155-journal-filter '+((a.from&&a.to)?'tp155-range-active':'')+'" '+(state.tp155JournalFilterOpen?'open':'')+' ontoggle="state.tp155JournalFilterOpen=!!this.open"><summary><div><strong>'+esc(c.filter)+'</strong>'+status+'</div><span class="tp160-journal-disclosure" aria-hidden="true"></span></summary>'+calendarHtml()+'</details><p class="small muted rf263-history-count">'+esc(tp149T('journal.count',{shown:filtered.length,total:all.length}))+'</p>';
  const rows=filtered.length?filtered.map(function(row,i){return rf263HistoryItem(row.x,row.index,i)}).join(''):(all.length?'<div class="card muted">'+esc(tp149T('journal.noneRange'))+'</div>':'<div class="muted">'+esc(tp149T('journal.none'))+'</div>');
  return shell('<main class="tp150-journal-compact">'+filter+rows+'</main>');
 };

 const style=document.createElement('style');style.id='tp155Round3JournalRangeCss';style.textContent=[
  '.tp155-journal-range-head{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin:10px 0 4px}.tp155-journal-range-head>div{display:grid;gap:2px;padding:8px 10px;border:1px solid var(--line);border-radius:11px;background:var(--card2)}.tp155-journal-range-head span{font-size:10px;font-weight:850;text-transform:uppercase;letter-spacing:.05em;color:var(--muted)}.tp155-journal-range-head strong{font-size:13px}.tp155-journal-range-help{margin:5px 1px 8px!important}',
  '.tp155-journal-day.is-range{background:color-mix(in srgb,var(--accent) 11%,var(--card2));border-color:color-mix(in srgb,var(--accent) 34%,var(--line))}.tp155-journal-day.is-start,.tp155-journal-day.is-end{background:color-mix(in srgb,var(--accent) 25%,var(--card2));border-color:var(--accent);font-weight:900}',
  'html[data-tp-theme-family="basic"] .tp155-journal-filter.tp155-range-active{box-shadow:none!important;border-color:color-mix(in srgb,var(--accent) 58%,var(--line))!important;background:color-mix(in srgb,var(--accent) 6%,var(--card))!important}'
 ].join('');
 document.head.appendChild(style);
 window.TrainPilot155JournalRange={version:'1.5.5-round3-range',inclusive:true,twoDateSelection:true};
})();
// @endsection trainpilot-155-round3-journal-range.js

// @section trainpilot-155-round3-highlight-surfaces.js
/* TrainPilot 1.5.5 Round 3: shared active/high-priority accent surface language. */
(function(){
 'use strict';

 window.tp155R4ApplyHighlightSurfaces=function(root=document){
  const q=(sel,base=root)=>base?.querySelector?.(sel)||null;
  const qa=(sel,base=root)=>[...(base?.querySelectorAll?.(sel)||[])];

  // Home — active program.
  q('main.rf221-home>.hero.tp155-home-active-card')?.classList.add('tp155-r4-accent-surface');

  // Workout — current exercise + next unfinished set.
  const workout=q('main.tp153-workout');
  if(workout){
   q('.tp153-workout-head',workout)?.classList.add('tp155-r4-accent-surface');
   const rows=qa('.tp153-set-row',workout);
   rows.forEach(x=>x.classList.remove('tp155-r4-accent-surface-lite'));
   const sets=state.session?.exercises?.[Math.max(0,Math.min(Number(state.current)||0,(state.session?.exercises?.length||1)-1))]?.sets||[];
   let active=sets.findIndex(x=>!x?.done);if(active<0&&rows.length)active=rows.length-1;
   if(rows[active])rows[active].classList.add('tp155-r4-accent-surface-lite');
  }

  // Calendar — selected day details.
  q('.tp151-day-panel')?.classList.add('tp155-r4-accent-surface');

  // Journal — completed active range only.
  q('.tp155-journal-filter.tp155-range-active')?.classList.add('tp155-r4-accent-surface');

  // Programs — active program in the library.
  qa('.tp152-program-card.active-program').forEach(x=>x.classList.add('tp155-r4-accent-surface'));

  // Health — today's main summary / recovery state.
  const health=q('main.rf263-health');
  if(health){
   const today=q(':scope > .tp151-health-card',health);
   if(today)today.classList.add('tp155-r4-accent-surface');
  }

  // Coach — recommendation card remains the visual reference.
  q('.tp151-coach-recommendation')?.classList.add('tp155-r4-accent-surface');
 };

 const renderBase=render;
 render=function(custom){
  const out=renderBase.apply(this,arguments);
  window.tp155R4ApplyHighlightSurfaces(document);
  return out;
 };

 // renderWorkout has its own post-render wrappers, so re-apply after that path as well.
 if(typeof renderWorkout==='function'){
  const workoutBase=renderWorkout;
  renderWorkout=function(){
   const out=workoutBase.apply(this,arguments);
   window.tp155R4ApplyHighlightSurfaces(document);
   return out;
  };
 }

 const style=document.createElement('style');style.id='tp155Round3HighlightCss';style.textContent=[
  '.tp155-r4-accent-surface{box-sizing:border-box!important;border:1px solid color-mix(in srgb,var(--accent) 56%,var(--line))!important;background:linear-gradient(135deg,color-mix(in srgb,var(--accent) 11%,var(--card)) 0%,color-mix(in srgb,var(--accent2) 5%,var(--card)) 56%,var(--card) 100%)!important}',
  '.tp155-r4-accent-surface-lite{box-sizing:border-box!important;border-color:color-mix(in srgb,var(--accent) 43%,var(--line))!important;background:linear-gradient(90deg,color-mix(in srgb,var(--accent) 8%,var(--card2)),var(--card2))!important}',
  'html[data-tp-theme-family="basic"] .tp155-r4-accent-surface,html[data-tp-theme-family="basic"] .tp155-r4-accent-surface-lite{box-shadow:none!important;filter:none!important}',
  'html[data-tp-theme-family="basic"] .tp155-r4-accent-surface{border-color:color-mix(in srgb,var(--accent) 58%,var(--line))!important;background:linear-gradient(135deg,color-mix(in srgb,var(--accent) 9%,var(--card)) 0%,color-mix(in srgb,var(--accent2) 4%,var(--card)) 58%,var(--card) 100%)!important}',
  'html[data-tp-theme-family="basic"] .tp155-r4-accent-surface-lite{border-color:color-mix(in srgb,var(--accent) 40%,var(--line))!important;background:color-mix(in srgb,var(--accent) 7%,var(--card2))!important}',
  'html[data-tp-theme-family="vivid"] .tp155-r4-accent-surface{border-color:color-mix(in srgb,var(--accent) 76%,var(--line))!important;background:linear-gradient(135deg,color-mix(in srgb,var(--accent) 16%,var(--card)) 0%,color-mix(in srgb,var(--accent2) 8%,var(--card)) 60%,var(--card) 100%)!important;box-shadow:0 0 18px color-mix(in srgb,var(--accent) 16%,transparent)!important}',
  'html[data-tp-theme-family="vivid"] .tp155-r4-accent-surface-lite{box-shadow:0 0 12px color-mix(in srgb,var(--accent) 10%,transparent)!important}',
  '.tp153-set-row.tp155-r4-accent-surface-lite{border-style:solid!important;border-width:1px!important;border-radius:12px!important;padding-inline:5px!important}',
  '.tp151-day-panel.tp155-r4-accent-surface,.tp155-journal-filter.tp155-r4-accent-surface,.tp152-program-card.tp155-r4-accent-surface{border-radius:16px!important}'
 ].join('');
 document.head.appendChild(style);
 window.tp155R4ApplyHighlightSurfaces(document);
 window.TrainPilot155HighlightSurfaces={version:'1.5.5-round3-highlight',shared:true,basicMatte:true,neonGlow:true};
})();
// @endsection trainpilot-155-round3-highlight-surfaces.js

// @section trainpilot-155-round3-panel-navigation.js
/* TrainPilot 1.5.5 Round 3: reordered 2x4 navigation + Calendar/Coach/Settings panel surfaces. */
(function(){
 'use strict';

 let currentPanel='',panelHost=null,panelTrigger=null,bodyOverflow='',panelOpenScrollY=0;
 const panelDomSupported=(()=>{try{const x=document?.createElement?.('div');return !!(x&&typeof x.addEventListener==='function'&&document?.body&&typeof document.body.appendChild==='function')}catch(_){return false}})();
 const PANEL_ROUTES=new Set(['calendar','coach','settings','quick','exercises']);
 const FULL_ROUTES=new Set(['home','plan','health','programs','history']);

 const routeFromButton=function(btn){
  const oc=String(btn?.getAttribute?.('onclick')||'');
  const m=oc.match(/go\(['"]([^'"]+)['"]\)/);
  return m?m[1]:'';
 };
 const extractMain=function(html,selector='main'){
  const t=document.createElement('template');t.innerHTML=String(html||'').trim();
  const main=t.content.querySelector(selector)||t.content.querySelector('main');
  return main?main.outerHTML:'';
 };

 window.tp155R4DecorateNavigation=function(){
  const top=document.querySelector('.top.tp154-nav-grid');if(!top)return;
  const buttons=[...top.querySelectorAll('.tp151-nav-item')];
  const order={home:1,plan:2,health:3,programs:4,history:5,calendar:6};
  buttons.forEach(function(btn){
   const route=routeFromButton(btn);if(order[route])btn.style.order=String(order[route]);
   const active=!currentPanel&&(route===state.tab||(route==='history'&&state.tab==='stats')||(route==='home'&&state.tab==='profile'));
   btn.classList.toggle('active',!!active);
  });
  const coach=top.querySelector('.tp154-coach-action'),settings=top.querySelector('.tp154-settings-action');
  if(coach){
   coach.style.order='7';coach.classList.toggle('active',currentPanel==='coach');
   coach.setAttribute('aria-expanded',currentPanel==='coach'?'true':'false');
   coach.setAttribute('onclick',"tp155R4TogglePanel('coach',this)");
  }
  if(settings){
   settings.style.order='8';settings.classList.toggle('active',currentPanel==='settings');
   settings.setAttribute('aria-expanded',currentPanel==='settings'?'true':'false');
   settings.setAttribute('onclick',"tp155R4TogglePanel('settings',this)");
   if(!settings.querySelector('.tp151-nav-label'))settings.insertAdjacentHTML('beforeend','<span class="tp151-nav-label">'+esc(tp151T('settings'))+'</span>');
  }
  const calendar=buttons.find(x=>routeFromButton(x)==='calendar');
  if(calendar){
   calendar.classList.toggle('active',currentPanel==='calendar');
   calendar.setAttribute('aria-expanded',currentPanel==='calendar'?'true':'false');
  }
 };

 const calendarHtml=function(){
  const month=tp149FormatDate(new Date(state.calendarMonth+'-01T12:00'),{year:'numeric',month:'long'}),weekdays=tp149Weekdays();
  const day=typeof tp151CalendarDayPanel==='function'?tp151CalendarDayPanel():'';
  const planner=typeof rf2211Planner==='function'?rf2211Planner():'';
  return '<main class="tp151-calendar tp155-r4-calendar-panel-main">'+
   '<section class="tp151-calendar-frame tp155-r4-calendar-frame"><div class="calendar-head"><button class="btn secondary" onclick="changeCalendarMonth(-1)">‹</button><strong>'+esc(month)+'</strong><button class="btn secondary" onclick="changeCalendarMonth(1)">›</button></div><div class="cal-weekdays">'+weekdays.map(x=>'<span>'+esc(x)+'</span>').join('')+'</div><div class="calendar-grid">'+calendarGrid()+'</div></section>'+
   day+
   '<div class="tp155-r4-planner-bottom">'+planner+'</div>'+
   '</main>';
 };

 const settingsHtml=function(){
  try{return extractMain(settingsScreen(),'main.tp1511-settings,main.tp152-settings')}catch(_){return ''}
 };

 const coachBase=rf233CoachScreen;
 const coachHtml=function(){
  const oldTab=state.tab,oldHealth=state.healthView;
  const hadVisible=typeof rf245CoachVisible!=='undefined',oldVisible=hadVisible?rf245CoachVisible:undefined;
  const hadReturn=typeof tp153CoachReturn!=='undefined',oldReturn=hadReturn?tp153CoachReturn:undefined;
  const liveRender=render;let captured='';
  try{
   render=function(custom){captured=String(custom||'');return custom};
   coachBase();
  }catch(_){captured=''}
  finally{
   render=liveRender;state.tab=oldTab;state.healthView=oldHealth;
   if(hadVisible)rf245CoachVisible=oldVisible;
   if(hadReturn)tp153CoachReturn=oldReturn;
  }
  if(!captured)return '';
  const t=document.createElement('template');t.innerHTML=captured;
  const main=t.content.querySelector('main.tp151-coach')||t.content.querySelector('main');
  main?.querySelector('.tp151-back-row')?.remove();
  return main?main.outerHTML:'';
 };

 const panelHtml=function(type){
  if(type==='calendar')return calendarHtml();
  if(type==='settings')return settingsHtml();
  if(type==='coach')return coachHtml();
  if(type==='quick')return typeof window.tp155QuickPanelHtml==='function'?window.tp155QuickPanelHtml():'';
  if(type==='exercises')return typeof window.tp155ExercisePanelHtml==='function'?window.tp155ExercisePanelHtml():'';
  return '';
 };

 const positionPanel=function(){
  if(!panelHost)return;
  const top=document.querySelector('.top.tp154-nav-grid');
  const r=top?.getBoundingClientRect?.(),edge=r?Math.max(0,Math.min(innerHeight-80,r.bottom)):0;
  panelHost.style.top=Math.round(edge)+'px';
 };

 window.tp155R4RefreshPanel=function(){
  if(!currentPanel||!panelHost)return;
  const panel=panelHost.querySelector('.tp155-r4-panel'),content=panelHost.querySelector('.tp155-r4-panel-content');
  if(!panel||!content)return;
  const y=panel.scrollTop;
  content.innerHTML=panelHtml(currentPanel);
  try{if(typeof rf260EnhanceSelects==='function')rf260EnhanceSelects()}catch(_){}
  try{if(typeof rf260EnhanceTemporalFields==='function')rf260EnhanceTemporalFields()}catch(_){}
  try{if(typeof tp149TranslateFreshDom==='function')tp149TranslateFreshDom(content)}catch(_){}
  try{if(typeof window.tp155R4ApplyHighlightSurfaces==='function')window.tp155R4ApplyHighlightSurfaces(content)}catch(_){}
  panel.scrollTop=Math.min(y,Math.max(0,panel.scrollHeight-panel.clientHeight));
  positionPanel();window.tp155R4DecorateNavigation();
 };

 window.tp155R4ClosePanel=function(restoreFocus=true){
  if(!currentPanel&&!panelHost)return false;
  const trigger=panelTrigger,restoreY=panelOpenScrollY;
  currentPanel='';panelTrigger=null;panelOpenScrollY=0;
  panelHost?.remove();panelHost=null;
  document.documentElement.classList.remove('tp155-r4-panel-open');
  document.body.classList.remove('tp155-r4-panel-open');
  document.body.style.overflow=bodyOverflow;bodyOverflow='';
  if(restoreFocus&&Number.isFinite(restoreY)&&Math.abs((window.scrollY||0)-restoreY)>.5)try{window.scrollTo(0,restoreY)}catch(_){}
  window.tp155R4DecorateNavigation();
  if(restoreFocus&&trigger?.isConnected)try{trigger.focus({preventScroll:true})}catch(_){}
  return true;
 };

 window.tp155R4OpenPanel=function(type,trigger){
  if(!PANEL_ROUTES.has(type)||!panelDomSupported)return false;
  if(currentPanel===type&&panelHost)return true;
  if(!currentPanel){panelOpenScrollY=window.scrollY||window.pageYOffset||0;bodyOverflow=document.body.style.overflow||'';document.body.style.overflow='hidden'}
  currentPanel=type;panelTrigger=trigger||document.activeElement||null;
  if(!panelHost){
   panelHost=document.createElement('div');panelHost.id='tp155R4PanelHost';panelHost.className='tp155-r4-panel-host';
   panelHost.innerHTML='<section class="tp155-r4-panel" role="region" tabindex="-1" onclick="event.stopPropagation()"><button type="button" class="tp155-r4-panel-close" aria-label="'+esc(tp149T('common.close'))+'" onclick="tp155R4ClosePanel()">×</button><div class="tp155-r4-panel-content"></div></section>';
   panelHost.addEventListener('click',function(e){if(e.target===panelHost)window.tp155R4ClosePanel()});
   document.body.appendChild(panelHost);
  }
  document.documentElement.classList.add('tp155-r4-panel-open');document.body.classList.add('tp155-r4-panel-open');
  panelHost.dataset.panel=type;
  window.tp155R4RefreshPanel();
  panelHost.querySelector('.tp155-r4-panel')?.focus?.({preventScroll:true});
  return true;
 };

 window.tp155R4TogglePanel=function(type,trigger){
  if(currentPanel===type)return window.tp155R4ClosePanel();
  return window.tp155R4OpenPanel(type,trigger);
 };

 // Calendar remains represented by the existing nav button, but opens the panel instead of a route.
 const goBase=go;
 go=function(route){
  const key=String(route||'home'),target=window.TrainPilotRoutes?.[key]||key;
  if(panelDomSupported&&target==='calendar')return window.tp155R4TogglePanel('calendar',document.activeElement);
  if(panelDomSupported&&target==='settings')return window.tp155R4TogglePanel('settings',document.activeElement);
  if(currentPanel)window.tp155R4ClosePanel(false);
  return goBase.apply(this,arguments);
 };
 window.TrainPilotNavigate=go;

 // Coach is reached through direct rf233CoachScreen() calls across the app.
 rf233CoachScreen=function(){return panelDomSupported?window.tp155R4TogglePanel('coach',document.activeElement):coachBase.apply(this,arguments)};
 rf220CoachScreen=rf233CoachScreen;

 if(typeof tp153OpenCoachTarget==='function'){
  const targetBase=tp153OpenCoachTarget;
  tp153OpenCoachTarget=function(){window.tp155R4ClosePanel(false);return targetBase.apply(this,arguments)};
 }

 // Calendar/planner/settings actions commonly call render(); keep the underlying full page
 // and refresh the still-open panel from the new state.
 const renderBase=render;
 render=function(custom){
  const out=renderBase.apply(this,arguments);
  window.tp155R4DecorateNavigation();
  if(currentPanel)window.tp155R4RefreshPanel();
  return out;
 };

 const backBase=window.TrainPilotAndroidBack;
 window.TrainPilotAndroidBack=function(){
  if(currentPanel){
   if(panelHost?.querySelector?.('.tp-select.open')){try{rf260CloseSelects()}catch(_){}return true}
   if(typeof rf260TemporalPicker!=='undefined'&&rf260TemporalPicker)return typeof backBase==='function'?backBase.apply(this,arguments):true;
   if(document.querySelector?.('#tpTemporalPicker,#tp2628Dialog,.video-modal,.video-overlay'))return typeof backBase==='function'?backBase.apply(this,arguments):true;
   if(state?.rf151HistoryEdit||state?.tp151EditingScheduleId)return typeof backBase==='function'?backBase.apply(this,arguments):true;
   const opened=panelHost?[...panelHost.querySelectorAll('details[open]')].reverse():[];
   if(opened.length){opened[0].open=false;return true}
   return window.tp155R4ClosePanel();
  }
  return typeof backBase==='function'?backBase.apply(this,arguments):false;
 };

 window.addEventListener?.('resize',positionPanel,{passive:true});
 window.addEventListener?.('scroll',positionPanel,{passive:true});

 const style=document.createElement('style');style.id='tp155Round3PanelNavigationCss';style.textContent=[
  '.tp155-r4-panel-host{position:fixed;z-index:14500;left:0;right:0;bottom:0;padding:7px max(8px,env(safe-area-inset-right,0px)) max(8px,env(safe-area-inset-bottom,0px)) max(8px,env(safe-area-inset-left,0px));background:rgba(3,7,11,.46);display:flex;align-items:flex-start;justify-content:center;overflow:hidden}',
  '.tp155-r4-panel{position:relative;width:min(760px,100%);max-height:calc(100% - 2px);overflow:auto;overscroll-behavior:contain;border:1px solid color-mix(in srgb,var(--accent) 42%,var(--line));border-radius:0 0 18px 18px;background:color-mix(in srgb,var(--card) 97%,transparent);box-shadow:0 16px 46px rgba(0,0,0,.42);padding:10px 10px max(12px,env(safe-area-inset-bottom,0px));outline:none}',
  '.tp155-r4-panel-close{position:sticky;top:0;z-index:5;float:right;width:36px;height:36px;margin:0 0 -36px 8px;border:1px solid var(--line);border-radius:11px;background:color-mix(in srgb,var(--card2) 94%,transparent);color:var(--text);font:800 22px/1 sans-serif;display:grid;place-items:center}',
  '.tp155-r4-panel-content{clear:both}.tp155-r4-panel-content>main{margin:0!important;padding:2px 0 0!important;max-width:none!important;min-height:0!important}',
  '.tp155-r4-calendar-panel-main>.tp151-calendar-frame{margin-top:0!important}.tp155-r4-calendar-panel-main>.tp151-day-panel{margin-top:10px!important}.tp155-r4-planner-bottom{margin-top:10px}.tp155-r4-planner-bottom>.rf2211-planner{margin:0!important}',
  '.tp155-r4-calendar-panel-main .calendar-head{padding-right:40px}',
  'html[data-tp-theme-family="basic"] .tp155-r4-panel{box-shadow:0 14px 34px rgba(0,0,0,.28)!important;border-color:color-mix(in srgb,var(--accent) 42%,var(--line))!important}',
  '.top.tp154-nav-grid .tp154-settings-action .tp151-nav-label{display:block!important}',
  'body>:is(#tpTemporalPicker,#tp2628Dialog,#videoModal,#rf130PhotoModal,.tp-temporal-modal,.video-modal,.video-overlay){z-index:17000!important}',
  '@media(max-width:390px){.tp155-r4-panel-host{padding-inline:6px}.tp155-r4-panel{padding-inline:8px;border-radius:0 0 15px 15px}.tp155-r4-panel-close{width:34px;height:34px}}'
 ].join('');
 document.head.appendChild(style);

 window.tp155R4DecorateNavigation();
 window.TrainPilot155PanelNavigation={version:'1.5.5-round3-panels',order:['home','plan','health','programs','history','calendar','coach','settings'],calendarPanel:true,coachPanel:true,settingsSheet:true};
})();
// @endsection trainpilot-155-round3-panel-navigation.js

// @section trainpilot-160-light-navigation-motion.js
/* RepForge 1.6: lightweight page/panel reveal without forced synchronous layout. */
(function(){
 'use strict';
 const FULL=new Set(['home','plan','health','programs','history']);
 const goBase=go;
 go=function(route){
  const key=String(route||'home'),target=window.TrainPilotRoutes?.[key]||key;
  const out=goBase.apply(this,arguments);
  if(FULL.has(target)&&!state.session){
   document.querySelector('#app main')?.classList.add('tp160-page-reveal');
  }
  return out;
 };
 window.TrainPilotNavigate=go;

 const style=document.createElement('style');style.id='tp160LightNavigationMotionCss';style.textContent=[
  '@keyframes tp160PageReveal{from{opacity:.72;transform:translateY(-4px)}to{opacity:1;transform:translateY(0)}}',
  '@keyframes tp160PanelReveal{from{opacity:.78;transform:translateY(-5px)}to{opacity:1;transform:translateY(0)}}',
  '@keyframes tp160BackdropIn{from{background:rgba(3,7,11,0)}to{background:rgba(3,7,11,.46)}}',
  '#app main.tp160-page-reveal{animation:tp160PageReveal 110ms ease-out both}',
  '.tp155-r4-panel-host{animation:tp160BackdropIn 100ms ease-out both}',
  '.tp155-r4-panel{animation:tp160PanelReveal 110ms ease-out both}',
  '@media(prefers-reduced-motion:reduce){#app main.tp160-page-reveal,.tp155-r4-panel-host,.tp155-r4-panel{animation:none!important;transform:none!important;transition:none!important}}'
 ].join('');
 document.head.appendChild(style);
 window.RepForge160Motion={version:'1.6.0',forcedLayout:false,clipPath:false,backdropBlur:false};
})();
// @endsection trainpilot-160-light-navigation-motion.js

// @section trainpilot-155-round3-stopwatch-layout.js
/* TrainPilot 1.5.5 Round 3 phone fix:
 * timed / per-side stopwatch belongs above the workout action grid, never inside it.
 */
(function(){
 'use strict';

 window.tp155R4NormalizeStopwatchLayout=function(){
  const main=document.querySelector('main.tp153-workout,main.tp149-workout');
  if(!main)return false;
  const card=main.querySelector('#rf110Stopwatch'),actions=main.querySelector('.tp153-workout-actions'),firstSet=main.querySelector('.tp153-set-row,.row');
  if(!card||!actions)return false;
  const target=firstSet||actions;
  if(card.parentElement===actions||card.nextElementSibling!==target){
   target.parentNode.insertBefore(card,target);
  }
  card.classList.add('tp155-r4-stopwatch-fullrow');
  return true;
 };

 if(typeof renderWorkout==='function'){
  const workoutBase=renderWorkout;
  renderWorkout=function(){
   const out=workoutBase.apply(this,arguments);
   window.tp155R4NormalizeStopwatchLayout();
   return out;
  };
 }

 const style=document.createElement('style');style.id='tp155Round3StopwatchLayoutCss';style.textContent=[
  '.tp153-workout>.rf110-stopwatch.tp155-r4-stopwatch-fullrow{display:block!important;position:static!important;inset:auto!important;float:none!important;z-index:auto!important;transform:none!important;width:100%!important;max-width:100%!important;box-sizing:border-box!important;margin:10px 0 14px!important;grid-column:1/-1!important;overflow:visible!important}',
  '.tp153-workout-actions>.rf110-stopwatch{grid-column:1/-1!important;width:100%!important}',
  '.tp153-workout-actions{position:relative!important;z-index:1!important;clear:both!important}.tp153-workout-actions .btn{position:relative!important;z-index:1!important}',
  '.tp1481-side-stopwatch .tp1481-side-grid{min-width:0}',
  '.tp1481-side-stopwatch .tp1481-side-card{min-width:0;max-width:100%;box-sizing:border-box}',
  '.tp1481-side-stopwatch .tp1481-side-actions{min-width:0}',
  '.tp1481-side-stopwatch .tp1481-side-actions .btn{min-width:0;white-space:normal}',
  '@media(max-width:430px){.tp1481-side-stopwatch .tp1481-side-grid{grid-template-columns:1fr!important}.tp1481-side-stopwatch .tp1481-side-actions{grid-template-columns:minmax(0,1.2fr) minmax(0,1fr)!important}}'
 ].join('');
 document.head.appendChild(style);
 window.tp155R4NormalizeStopwatchLayout();
 window.TrainPilot155StopwatchLayout={version:'1.5.5-round3-stopwatch-layout',outsideActionGrid:true,perSideResponsive:true};
})();
// @endsection trainpilot-155-round3-stopwatch-layout.js

// @section trainpilot-155-round3-panel-select-anchor.js
/* TrainPilot 1.5.5 Round 3 phone fix:
 * fixed-position custom select menus must stay anchored to their trigger inside
 * Calendar / Coach / Settings panels. The panel reveal transform is removed
 * after animation so fixed descendants use viewport coordinates again.
 */
(function(){
 'use strict';

 let settleTimer=null;
 const settlePanel=function(panel){
  if(!panel)return;
  panel.classList.add('tp155-r4-panel-settled');
  clearTimeout(settleTimer);
  settleTimer=null;
  try{
   if(typeof rf260RepositionOpenSelects==='function')requestAnimationFrame(()=>rf260RepositionOpenSelects());
  }catch(_){}
 };

 if(typeof window.tp155R4OpenPanel==='function'){
  const openBase=window.tp155R4OpenPanel;
  window.tp155R4OpenPanel=function(){
   const out=openBase.apply(this,arguments);
   const panel=document.querySelector?.('#tp155R4PanelHost .tp155-r4-panel');
   if(panel){
    panel.classList.remove('tp155-r4-panel-settled');
    panel.addEventListener?.('animationend',()=>settlePanel(panel),{once:true});
    clearTimeout(settleTimer);
    settleTimer=setTimeout(()=>settlePanel(panel),230);
   }
   return out;
  };
 }

 if(typeof rf260PositionSelect==='function'){
  const positionBase=rf260PositionSelect;
  rf260PositionSelect=function(wrap){
   const panel=wrap?.closest?.('.tp155-r4-panel');
   if(panel&&!panel.classList.contains('tp155-r4-panel-settled'))settlePanel(panel);
   return positionBase.apply(this,arguments);
  };
 }

 const existing=document.querySelector?.('#tp155R4PanelHost .tp155-r4-panel');
 if(existing)settlePanel(existing);

 const style=document.createElement('style');style.id='tp155Round3PanelSelectAnchorCss';style.textContent=[
  '.tp155-r4-panel.tp155-r4-panel-settled{animation:none!important;transform:none!important;will-change:auto!important}',
  '.tp155-r4-panel .tp-select-menu{max-width:min(calc(100vw - 16px),720px)}'
 ].join('');
 document.head.appendChild(style);

 window.TrainPilot155PanelSelectAnchor={version:'1.5.5-round3-panel-select-anchor',viewportAnchored:true};
})();
// @endsection trainpilot-155-round3-panel-select-anchor.js

// @section trainpilot-155-round3-home-calendar-priority.js
/* TrainPilot 1.5.5 Round 3 phone fix:
 * Home must honor today's unfinished Calendar workout for the whole local day,
 * even after its planned clock time has passed.
 */
(function(){
 'use strict';

 const isDone=function(x){
  return !!(x&&(x.status==='completed'||history().some(function(h){return h.scheduleId===x.id})));
 };
 const isPlanned=function(x){
  return !!(x&&!x.cancelled&&(x.status||'planned')==='planned'&&!isDone(x));
 };

 const nextPlannedBase=nextPlanned;
 nextPlanned=function(){
  const all=scheduled().filter(isPlanned).sort(function(a,b){return Date.parse(a.start)-Date.parse(b.start)});
  const today=typeof localDateKey==='function'?localDateKey(new Date()):'';
  const todayItem=today?all.find(function(x){
   const d=new Date(x.start);
   return Number.isFinite(d.getTime())&&localDateKey(d)===today;
  }):null;
  if(todayItem)return todayItem;
  const now=Date.now();
  return all.find(function(x){return Date.parse(x.start)>=now})||null;
 };

 window.TrainPilot155HomeCalendarPriority={
  version:'1.5.5-round3-home-calendar-priority',
  todayScheduleWins:true,
  preservesPastClockTime:true,
  previous:nextPlannedBase
 };
})();
// @endsection trainpilot-155-round3-home-calendar-priority.js

// @section trainpilot-155-next-12-mods.js
/* Targeted phone-test follow-ups. Reuse the existing Round 3 panels, calendar
 * model, quick-workout builder, exercise cards and stopwatch persistence. */
(function(){
 'use strict';
 const lang=function(){return typeof rf212Lang==='function'?rf212Lang():'hu'};
 const text={
  hu:{done:'Elvégezve',log:'Edzésnapló',stats:'Statisztikák',edit:'Szerkesztés',closeEdit:'Szerkesztés bezárása',search:'Keresés',muscle:'Izomcsoport',gear:'Felszerelés',all:'Összes'},
  en:{done:'Completed',log:'Workout log',stats:'Statistics',edit:'Edit',closeEdit:'Close edit',search:'Search',muscle:'Muscle group',gear:'Equipment',all:'All'},
  de:{done:'Erledigt',log:'Trainingstagebuch',stats:'Statistiken',edit:'Bearbeiten',closeEdit:'Bearbeiten schließen',search:'Suche',muscle:'Muskelgruppe',gear:'Ausrüstung',all:'Alle'},
  ro:{done:'Finalizat',log:'Jurnal antrenamente',stats:'Statistici',edit:'Editează',closeEdit:'Închide editarea',search:'Căutare',muscle:'Grupă musculară',gear:'Echipament',all:'Toate'}
 };
 const t=function(key){return text[lang()]?.[key]||text.hu[key]||key};
 if(typeof window==='undefined'||typeof tp151ScheduleCard!=='function'||typeof window.tp155R4OpenPanel!=='function')return;

 /* #9: open the existing date picker directly; preserve time, identity and duration. */
 rf260OpenScheduleMove=function(id,button){
  const item=scheduled().find(function(x){return x.id===id&&!x.cancelled});if(!item)return false;
  const start=new Date(item.start),input=document.createElement('input');
  input.type='date';input.className='field';input.hidden=true;input.value=localDateKey(Number.isNaN(start.getTime())?new Date():start);input.dataset.tp152Temp='calendar';input.dataset.tp155MoveSchedule=id;
  document.body.appendChild(input);state.rf260MovingScheduleId=id;rf260SetMoveButtonState(id,true);
  input.addEventListener('change',function(){
   const date=input.value;input.remove();if(!date)return;
   const rows=scheduled(),i=rows.findIndex(function(x){return x.id===id&&!x.cancelled});if(i<0)return;
   const old=rows[i],oldStart=new Date(old.start),time=Number.isNaN(oldStart.getTime())?'18:00':oldStart.toTimeString().slice(0,5);
   try{
    const next=dateAt(date,time),duration=Math.max(60000,Date.parse(old.end)-Date.parse(old.start));
    rows[i]=Object.assign({},old,{start:next.toISOString(),end:new Date(next.getTime()+duration).toISOString(),updatedAt:Date.now()});
    db.set('scheduled',rows);state.rf260MovingScheduleId=null;rf260SetMoveButtonState(id,false);if(typeof cloudChanged==='function')cloudChanged();render();
   }catch(error){alert(error?.message||rf260MoveText().invalid)}
  },{once:true});
  rf260OpenTemporal(input,button||document.activeElement);return true;
 };
 const moveTemporalCloseBase=rf260TemporalClose;
 rf260TemporalClose=function(){
  const moveId=rf260TemporalPicker?.input?.dataset?.tp155MoveSchedule||state.rf260MovingScheduleId,out=moveTemporalCloseBase.apply(this,arguments);
  if(moveId){state.rf260MovingScheduleId=null;rf260SetMoveButtonState(moveId,false)}return out;
 };

 /* #10: correction action reconciles existing history and never creates a log. */
 window.tp155MarkScheduleCompleted=function(id){
  const rows=scheduled(),i=rows.findIndex(function(x){return x.id===id&&!x.cancelled});if(i<0)return false;
  const item=rows[i],date=localDateKey(new Date(item.start)),logs=history();
  let changedHistory=false,match=logs.find(function(h){return h.scheduleId===id});
  if(!match){
   match=logs.find(function(h){
    const sameDate=localDateKey(new Date(h.started))===date;
    const sameProgram=!item.programId||!h.programId||h.programId===item.programId;
    const sameDay=String(h.dayId||h.workout||'')===String(item.dayId||item.workout||'');
    return sameDate&&sameProgram&&sameDay;
   });
   if(match&&!match.scheduleId){match.scheduleId=id;changedHistory=true}
  }
  rows[i]=Object.assign({},item,{status:'completed',completedAt:item.completedAt||new Date().toISOString(),updatedAt:Date.now()});
  db.set('scheduled',rows);if(changedHistory)db.set('history',logs);if(typeof cloudChanged==='function')cloudChanged();render();return true;
 };
 const calendarCardBase=tp151ScheduleCard;
 tp151ScheduleCard=function(item){
  let html=String(calendarCardBase(item)),done=typeof rf209ScheduleDone==='function'&&rf209ScheduleDone(item);
  html=html.replace('onclick="rf260OpenScheduleMove(\''+item.id+'\',this)"','data-tp-move-id="'+esc(item.id)+'" onclick="rf260OpenScheduleMove(\''+esc(item.id)+'\',this)"');
  if(!done){
   const action='<button class="btn secondary tp155-complete-schedule" onclick="tp155MarkScheduleCompleted(\''+esc(item.id)+'\')">'+esc(t('done'))+'</button>';
   html=html.replace(/(<button class="btn secondary" onclick="skipSchedule)/,action+'$1');
  }
  return html;
 };

 /* #5: the existing Statistics view now has Journal tabs and parent navigation. */
 const journalTabs=function(active){return '<nav class="tp155-journal-tabs" aria-label="'+esc(t('log'))+'"><button class="'+(active==='log'?'active':'')+'" onclick="go(\'history\')">'+esc(t('log'))+'</button><button class="'+(active==='stats'?'active':'')+'" onclick="tp155OpenJournalStats()">'+esc(t('stats'))+'</button></nav>'};
 const historyBase=historyScreen;
 historyScreen=function(){return String(historyBase.apply(this,arguments)).replace(/(<main[^>]*>)/,function(m){return m+journalTabs('log')})};
 const statsBase=typeof tp2627OpenStats==='function'?tp2627OpenStats:function(){if(typeof rf221StatsScreen==='function')rf221StatsScreen()};
 window.tp155OpenJournalStats=function(){
  statsBase();const main=document.querySelector('#app main');if(main&&!main.querySelector('.tp155-journal-tabs'))main.insertAdjacentHTML('afterbegin',journalTabs('stats'));
  window.tp155R4DecorateNavigation?.();
 };
 window.tp2627OpenStats=window.tp155OpenJournalStats;

 /* #8: mount the existing inline Quick Workout choices in the shared panel. */
 window.tp155QuickPanelHtml=function(){
  const query=String(state.tp155QuickQuery||'');
  return '<main class="tp150-quick-picker tp155-quick-panel"><div class="hero"><span class="badge">'+esc(tp150qwT('quickBadge'))+'</span><h1>'+esc(tp150qwT('title'))+'</h1><p class="small muted">'+esc(tp150qwT('subtitle'))+'</p></div><div class="tp155-panel-sticky"><label>'+esc(t('search'))+'<input id="tp155QuickQuery" class="field" type="search" value="'+esc(query)+'" oninput="tp155QuickFilter(this.value)"></label></div><div id="tp150QuickResults" class="tp155-panel-scroll">'+tp150QuickResults(query)+'</div></main>';
 };
 window.tp155QuickFilter=function(value){state.tp155QuickQuery=String(value||'');const host=document.getElementById('tp150QuickResults');if(host)host.innerHTML=tp150QuickResults(state.tp155QuickQuery);try{rf260EnhanceSelects()}catch(_){}};
 const quickCardBase=tp150CompactQuickCard;
 tp150CompactQuickCard=function(){return String(quickCardBase()).replace('onclick="tp150QuickPicker()"','onclick="tp155R4TogglePanel(\'quick\',this)"')};

 /* #12: preserve the exercise filters/cards, changing only their outer shell. */
 const libraryState=function(){return state.libraryFilter||{group:'all',gear:'all',q:''}};
 window.tp155ExercisePanelHtml=function(){
  const f=libraryState();state.libraryFilter=f;
  const muscles=Object.keys(MUSCLES).map(function(key){return '<option value="'+esc(key)+'" '+(f.group===key?'selected':'')+'>'+esc(tp149MuscleGroupLabel(key))+'</option>'}).join('');
  const gears=Object.entries(TP149_LIBRARY_GEAR_ROWS).map(function(row){return '<option value="'+esc(row[0])+'" '+(f.gear===row[0]?'selected':'')+'>'+esc(tp149RowValue(row[1]))+'</option>'}).join('');
  return '<main class="tp155-exercise-panel"><div class="hero"><h1>'+esc(tp149T('exercise.libraryTitle'))+'</h1><p>'+esc(tp149T('exercise.librarySummary',{count:exercises().length}))+'</p></div><div class="setting tp155-library-filters"><label>'+esc(t('search'))+'<input id="libraryQuery" class="field" type="search" value="'+esc(f.q||'')+'" placeholder="'+esc(tp149T('exercise.searchPlaceholder'))+'" oninput="tp155LibraryFilter()"></label><div class="tp155-filter-row"><label>'+esc(t('muscle'))+'<select id="libraryMuscle" class="field" onchange="tp155LibraryFilter()"><option value="all">'+esc(t('all'))+'</option>'+muscles+'</select></label><label>'+esc(t('gear'))+'<select id="libraryGear" class="field" onchange="tp155LibraryFilter()">'+gears+'</select></label></div></div><div id="libraryResults" class="tp155-library-results">'+libraryResults(f.group||'all',f.gear||'all',f.q||'')+'</div></main>';
 };
 window.tp155LibraryFilter=function(){
  const f={group:document.getElementById('libraryMuscle')?.value||'all',gear:document.getElementById('libraryGear')?.value||'all',q:document.getElementById('libraryQuery')?.value||''};state.libraryFilter=f;
  const host=document.getElementById('libraryResults');if(host)host.innerHTML=libraryResults(f.group,f.gear,f.q);
 };
 muscleLibrary=function(group='all'){state.libraryFilter={group:group,gear:'all',q:''};return window.tp155R4OpenPanel('exercises',document.activeElement)};

 /* #3: reorder only the four existing Home blocks when a draft exists. */
 window.tp155OrderHomeCards=function(){
  if(state.tab!=='home')return;const main=document.querySelector('main.rf221-home');if(!main)return;
  const resume=main.querySelector('button[onclick*="resumeDraft"]'),draft=resume?.closest('.card'),coach=main.querySelector('#rf220CoachCard'),program=main.querySelector('.hero'),today=main.querySelector('#rf223Today');
  if(!draft||!coach||!program||!today)return;
  draft.dataset.tp155HomeOrder='1';coach.dataset.tp155HomeOrder='2';program.dataset.tp155HomeOrder='3';today.dataset.tp155HomeOrder='4';
  draft.after(coach);coach.after(program);program.after(today);
 };

 /* #4: keep persisted side fields; show manual correction only on demand. */
 window.tp155ToggleSideEdit=function(side,button){
  const card=button?.closest('.tp1481-side-card'),editing=!card?.classList.contains('tp155-side-editing');if(!card)return;
  card.classList.toggle('tp155-side-editing',editing);button.textContent=editing?t('closeEdit'):t('edit');if(editing)card.querySelector('input')?.focus?.();
 };
 const sideDecoratorBase=tp1481DecoratePerSide;
 tp1481DecoratePerSide=function(){
  const out=sideDecoratorBase.apply(this,arguments);document.querySelectorAll('.tp1481-side-card').forEach(function(card){
   const side=card.querySelector('[id^="tp1481SideTime-"]')?.id.split('-').pop(),label=card.querySelector('label.small');if(!side||!label||card.querySelector('.tp155-side-edit'))return;
   label.classList.add('tp155-side-manual');const button=document.createElement('button');button.type='button';button.className='btn secondary tp155-side-edit';button.textContent=t('edit');button.setAttribute('onclick','tp155ToggleSideEdit(\''+side+'\',this)');label.before(button);
  });return out;
 };

 const renderBase=render;
 render=function(){
  const transientPanel=document.querySelector('#tp155R4PanelHost[data-panel="quick"],#tp155R4PanelHost[data-panel="exercises"]');
  if(transientPanel)window.tp155R4ClosePanel(false);
  const out=renderBase.apply(this,arguments);window.tp155OrderHomeCards();setTimeout(window.tp155OrderHomeCards,0);
  document.querySelector('#rf2211First')?.closest('.tp-select')?.classList.add('tp155-attached-select');return out;
 };

 const style=document.createElement('style');style.id='tp155Next12ModsCss';style.textContent=[
  '.tp155-r4-panel{border-top:0!important;box-shadow:0 18px 42px rgba(0,0,0,.38),inset 0 9px 16px -18px rgba(var(--accent-rgb),.72)!important;margin-top:-2px}',
  '.tp155-r4-panel::before{content:"";position:absolute;left:10px;right:10px;top:0;height:10px;pointer-events:none;background:linear-gradient(180deg,color-mix(in srgb,var(--accent) 12%,transparent),transparent);opacity:.55}',
  '.rf2211-planbody label:has(#rf2211First) .tp-select-trigger::after,.rf2211-planbody label:has(#rf2211Time) .tp-temporal-icon{display:none!important}',
  '.tp155-attached-select{position:relative!important}.tp155-attached-select .tp-select-menu{position:absolute!important;left:0!important;right:auto!important;top:calc(100% + 4px)!important;bottom:auto!important;width:100%!important;max-height:220px!important}',
  '.tp-temporal-modal{display:grid!important;place-items:center!important;padding:max(14px,env(safe-area-inset-top,0px)) 12px max(14px,env(safe-area-inset-bottom,0px))!important}',
  '.tp-temporal-modal .tp-temporal-card{position:relative!important;inset:auto!important;left:auto!important;right:auto!important;top:auto!important;bottom:auto!important;margin:auto!important;max-height:min(82dvh,680px)!important}',
  '.tp-temporal-modal .tp152-time-card{max-height:min(62dvh,430px)!important}',
  '.tp155-journal-tabs{display:grid;grid-template-columns:1fr 1fr;gap:4px;margin:0 0 12px;padding:4px;border:1px solid var(--line);border-radius:14px;background:var(--card2)}',
  '.tp155-journal-tabs button{min-height:40px;border:0;border-radius:10px;background:transparent;color:var(--muted);font-weight:850}.tp155-journal-tabs button.active{background:color-mix(in srgb,var(--accent) 18%,var(--card));color:var(--text)}',
  '.tp155-quick-panel,.tp155-exercise-panel{display:flex!important;flex-direction:column;min-height:0!important;height:100%}.tp155-quick-panel .hero,.tp155-exercise-panel .hero{padding:8px 4px 6px;margin:0}',
  '#tp155R4PanelHost[data-panel="quick"] .tp155-r4-panel,#tp155R4PanelHost[data-panel="exercises"] .tp155-r4-panel{overflow:hidden!important;display:flex;flex-direction:column;height:calc(100% - 2px)}#tp155R4PanelHost[data-panel="quick"] .tp155-r4-panel-content,#tp155R4PanelHost[data-panel="exercises"] .tp155-r4-panel-content{display:flex;flex:1;min-height:0;overflow:hidden}#tp155R4PanelHost[data-panel="quick"] .tp155-r4-panel-content>main,#tp155R4PanelHost[data-panel="exercises"] .tp155-r4-panel-content>main{flex:1;min-height:0}',
  '.tp155-panel-sticky,.tp155-library-filters{position:sticky;top:0;z-index:4;margin:0 0 8px!important;padding:8px!important;background:color-mix(in srgb,var(--card) 96%,transparent);backdrop-filter:blur(8px)}',
  '.tp155-exercise-panel .tp155-library-filters{position:relative;top:auto;z-index:auto;flex:0 0 auto}.tp155-exercise-panel .tp155-library-results{position:relative;z-index:1;flex:1 1 auto;scroll-padding-top:8px}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-r4-panel-content{height:100%}.tp155-exercise-panel{display:grid!important;grid-template-rows:auto auto minmax(0,1fr);height:100%!important;overflow:hidden}',
  '@media(max-height:500px){#tp155R4PanelHost:is([data-panel="exercises"],[data-panel="quick"]){top:0!important}.tp155-exercise-panel>.hero{display:none}.tp155-exercise-panel{grid-template-rows:auto minmax(0,1fr)}.tp155-exercise-panel .tp155-library-filters{display:grid;grid-template-columns:1fr 2fr;gap:8px;padding-right:40px!important}.tp155-library-filters label{margin:0!important}.tp155-quick-panel>.hero{display:none}}',
  '.tp155-filter-row{display:grid;grid-template-columns:1fr 1fr;gap:8px}.tp155-library-filters label{font-size:12px}.tp155-library-filters .field,.tp155-library-filters .tp-select-trigger{min-height:38px!important;padding-block:7px!important}',
  '.tp155-panel-scroll,.tp155-library-results{min-height:0;overflow-y:auto;overscroll-behavior:contain;padding-bottom:8px}.tp155-library-results .tp-library-card{margin:7px 0!important}',
  '.tp1481-side-stopwatch{padding:10px!important}.tp1481-side-stopwatch .rf110-stopwatch-head{margin-bottom:4px}.tp1481-side-stopwatch .tp1481-side-grid{grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:7px!important;margin-top:7px!important}',
  '.tp1481-side-stopwatch .tp1481-side-card{padding:9px!important}.tp1481-side-stopwatch .rf110-stopwatch-time{font-size:30px!important;margin:5px 0!important}.tp1481-side-stopwatch .tp1481-side-actions{grid-template-columns:1fr 1fr!important;gap:5px!important;margin-top:6px!important}.tp1481-side-stopwatch .tp1481-side-actions .btn,.tp155-side-edit{min-height:38px!important;padding:7px 5px!important;font-size:12px!important}',
  '.rf110-stopwatch:not(.tp1481-side-stopwatch){padding:10px!important;margin:8px 0 10px!important}.rf110-stopwatch:not(.tp1481-side-stopwatch) .rf110-stopwatch-time{font-size:32px!important}.rf110-stopwatch:not(.tp1481-side-stopwatch) .rf110-stopwatch-actions{margin-top:7px!important;gap:7px!important}.rf110-stopwatch:not(.tp1481-side-stopwatch) .rf110-stopwatch-actions .btn{min-height:40px!important}',
  '.tp155-side-manual{display:none!important}.tp155-side-editing .tp155-side-manual{display:block!important}.tp155-side-edit{width:100%;margin-top:5px}',
  '@media(max-width:330px){.tp1481-side-stopwatch .tp1481-side-grid,.tp155-filter-row{grid-template-columns:1fr!important}}'
 ].join('');document.head.appendChild(style);
 window.TrainPilot155Next12={version:'1.5.5-next-12',calendarMove:true,manualCalendarComplete:true,sideStopwatchCompact:true,journalStatistics:true,quickPanel:true,exercisePanel:true};
})();
// @endsection trainpilot-155-next-12-mods.js

// @section trainpilot-155-phone-round4-fixes.js
/* TrainPilot 1.5.5 phone-test follow-ups after NEXT12.
 * Keep this layer deliberately small: reuse existing screens, panel shell, planner,
 * history deletion logic and calendar state instead of introducing new systems. */
(function(){
 'use strict';
 const lang=function(){return typeof rf212Lang==='function'?rf212Lang():'hu'};
 const copy={
  hu:{customHint:'Koppints a naptárban arra a napra, amelyikre edzést szeretnél tenni.',moveHint:'Áthelyezés: válaszd ki az új napot a fenti naptárban.',cancel:'Mégse',deleteWorkout:'Törlés'},
  en:{customHint:'Tap the calendar day where you want to place a workout.',moveHint:'Move workout: choose the new day in the calendar above.',cancel:'Cancel',deleteWorkout:'Delete'},
  de:{customHint:'Tippe im Kalender auf den Tag, an dem du trainieren möchtest.',moveHint:'Training verschieben: Wähle oben im Kalender den neuen Tag.',cancel:'Abbrechen',deleteWorkout:'Löschen'},
  ro:{customHint:'Atinge în calendar ziua în care vrei să programezi antrenamentul.',moveHint:'Mută antrenamentul: alege noua zi în calendarul de mai sus.',cancel:'Anulează',deleteWorkout:'Șterge'},
  sk:{customHint:'Ťukni v kalendári na deň, na ktorý chceš pridať tréning.',moveHint:'Presun tréningu: vyber nový deň v kalendári vyššie.',cancel:'Zrušiť',deleteWorkout:'Odstrániť'},
  pl:{customHint:'Dotknij dnia w kalendarzu, na który chcesz dodać trening.',moveHint:'Przenieś trening: wybierz nowy dzień w kalendarzu powyżej.',cancel:'Anuluj',deleteWorkout:'Usuń'}
 };
 const tx=function(key){const l=lang();return copy[l]?.[key]||copy.hu[key]||key};

 if(typeof rf221StatsScreen==='function'){
  const statsScreenBase=rf221StatsScreen;
  rf221StatsScreen=function(){
   const out=statsScreenBase.apply(this,arguments),main=document.querySelector('#app main')||document.querySelector('main');
   if(main){
    const directBack=[...main.children].find(function(el){return el.tagName==='BUTTON'&&String(el.getAttribute('onclick')||'').includes("go('home')")});
    directBack?.remove();
    const hero=[...main.children].find(function(el){return el.classList?.contains('hero')});hero?.remove();
   }
   return out;
  };
 }

 rf230PlannerMode=function(){const m=plannerSettings()?.mode;return ['daily','alternate','weekly','custom'].includes(m)?m:'alternate'};
 window.tp155SavePlannerBasics=function(){
  const s=plannerSettings(),mode=document.querySelector('#rf230Mode')?.value||rf230PlannerMode(),time=document.querySelector('#rf2211Time')?.value||s.time||'18:00';
  const minutes=Math.max(10,Math.min(240,Number(document.querySelector('#rf2211Minutes')?.value)||Number(s.minutes)||45));
  db.set('plannerSettings',Object.assign({},s,{mode:mode,time:time,minutes:minutes}));if(typeof cloudChanged==='function')cloudChanged();
 };
 rf230ModeChanged=function(){
  const mode=document.querySelector('#rf230Mode')?.value||'alternate';window.tp155SavePlannerBasics();
  document.querySelectorAll('.tp155-auto-plan-only').forEach(function(el){el.style.display=mode==='custom'?'none':''});
  const row=document.querySelector('#rf230Weekdays');if(row)row.style.display=mode==='weekly'?'flex':'none';
  const hint=document.querySelector('.tp155-custom-plan-hint');if(hint)hint.style.display=mode==='custom'?'block':'none';
 };
 rf2211Planner=function(){
  const s=plannerSettings(),p=activeProgram(),days=rf2211Days(),today=new Date().toISOString().slice(0,10),mode=rf230PlannerMode(),custom=mode==='custom';
  const customLabel=typeof tp149PlannerRow==='function'?tp149PlannerRow('cadence','custom'):'Naptárban kijelölöm';
  return '<details class="card rf2211-planner"><summary><strong>'+esc(tp149T('calendar.planningSettings'))+'</strong><span class="small muted"> • '+esc(tp149T('calendar.weeksAhead'))+'</span></summary><div class="rf2211-planbody"><p class="small muted">'+esc(tp149T('calendar.rhythmHelp'))+'</p><label class="small">'+esc(tp149T('calendar.rhythm'))+'<select class="field" id="rf230Mode" onchange="rf230ModeChanged()"><option value="alternate" '+(mode==='alternate'?'selected':'')+'>'+esc(tp149T('calendar.cadence.alternate'))+'</option><option value="daily" '+(mode==='daily'?'selected':'')+'>'+esc(tp149T('calendar.cadence.daily'))+'</option><option value="weekly" '+(mode==='weekly'?'selected':'')+'>'+esc(tp149T('calendar.cadence.weekly'))+'</option><option value="custom" '+(custom?'selected':'')+'>'+esc(customLabel)+'</option></select></label><div class="grid2"><label class="small tp155-auto-plan-only" style="'+(custom?'display:none':'')+'">'+esc(tp149T('calendar.startDate'))+'<input class="field" id="rf2211Start" type="date" value="'+today+'"></label><label class="small">'+esc(tp149T('calendar.time'))+'<input class="field" id="rf2211Time" type="time" value="'+esc(s.time)+'" onchange="tp155SavePlannerBasics()"></label><label class="small">'+esc(tp149T('calendar.duration'))+'<input class="field" id="rf2211Minutes" type="number" min="10" max="240" value="'+s.minutes+'" onchange="tp155SavePlannerBasics()"></label><label class="small tp155-auto-plan-only" style="'+(custom?'display:none':'')+'">'+esc(tp149T('calendar.weeks'))+'<input class="field" id="rf2211Weeks" type="number" min="1" max="12" value="4"></label></div><label class="small tp155-auto-plan-only" style="'+(custom?'display:none':'')+'">'+esc(tp149T('calendar.firstDay'))+'<select class="field" id="rf2211First">'+days.map(function(d,i){return '<option value="'+i+'">'+esc(tp149ProgramDayName(p,d))+'</option>';}).join('')+'</select></label>'+rf230WeekdayRow(mode)+'<p class="small muted tp155-custom-plan-hint" style="'+(custom?'display:block':'display:none')+'">'+esc(tx('customHint'))+'</p><button class="btn block tp155-auto-plan-only" style="'+(custom?'display:none':'')+'" onclick="rf2211PlanWeeks()">'+esc(tp149T('calendar.plan'))+'</button></div></details>';
 };
 const planWeeksBase=rf2211PlanWeeks;
 rf2211PlanWeeks=function(){if((document.querySelector('#rf230Mode')?.value||rf230PlannerMode())==='custom'){window.tp155SavePlannerBasics();return}return planWeeksBase.apply(this,arguments)};

 const calendarDayBase=toggleCalendarDay;
 rf260OpenScheduleMove=function(id){
  const item=scheduled().find(function(x){return x.id===id&&!x.cancelled});if(!item)return false;
  if(state.rf260MovingScheduleId===id){state.rf260MovingScheduleId=null;render();return true}
  state.rf260MovingScheduleId=id;state.tp151EditingScheduleId=null;state.rf2211CalendarDate=localDateKey(new Date(item.start));render();return true;
 };
 window.tp155CancelCalendarMove=function(){if(!state.rf260MovingScheduleId)return;state.rf260MovingScheduleId=null;render()};
 toggleCalendarDay=function(date){
  const id=state.rf260MovingScheduleId;
  if(!id)return calendarDayBase.apply(this,arguments);
  const rows=scheduled(),i=rows.findIndex(function(x){return x.id===id&&!x.cancelled});if(i<0){state.rf260MovingScheduleId=null;return calendarDayBase.apply(this,arguments)}
  const old=rows[i],oldDate=localDateKey(new Date(old.start));
  if(date===oldDate){state.rf260MovingScheduleId=null;state.rf2211CalendarDate=date;render();return}
  if(rows.some(function(x,j){return j!==i&&!x.cancelled&&localDateKey(new Date(x.start))===date})){alert(tp149T('calendar.error.noEmpty'));return}
  const oldStart=new Date(old.start),time=Number.isNaN(oldStart.getTime())?'18:00':oldStart.toTimeString().slice(0,5),duration=Math.max(60000,Date.parse(old.end)-Date.parse(old.start));
  try{
   const next=dateAt(date,time);rows[i]=Object.assign({},old,{start:next.toISOString(),end:new Date(next.getTime()+duration).toISOString(),updatedAt:Date.now()});
   db.set('scheduled',rows);state.rf260MovingScheduleId=null;state.rf2211CalendarDate=date;if(typeof cloudChanged==='function')cloudChanged();render();
  }catch(error){alert(error?.message||tp149T('calendar.error.dateTime'))}
 };

 if(typeof rf263HistoryItem==='function'){
  const historyItemBase=rf263HistoryItem;
  rf263HistoryItem=function(x,originalIndex,visibleIndex){
   const key=rf142WorkoutKey(x);let html=String(historyItemBase.apply(this,arguments));if(html.includes('tp155-history-delete'))return html;
   const marker='<div class="rf-history-health-panel"';
   const action='<button type="button" class="btn danger block tp155-history-delete" onclick="event.stopPropagation();deleteHistoryWorkout(\''+esc(key)+'\')">'+esc(typeof tp149T==='function'?tp149T('common.delete'):tx('deleteWorkout'))+'</button>';
   const i=html.indexOf(marker);if(i>=0)html=html.slice(0,i)+action+html.slice(i);return html;
  };
 }

 const decorate=function(){
  document.querySelector('#rf2211First')?.closest('.tp-select')?.classList.add('tp155-attached-select');
  document.querySelector('.tp155-language-select')?.closest('.tp-select')?.classList.add('tp155-attached-select','tp155-language-attached');
  const host=document.getElementById('tp155R4PanelHost');
  if(host?.dataset?.panel==='calendar'&&state.rf260MovingScheduleId){
   const main=host.querySelector('.tp155-r4-calendar-panel-main'),frame=main?.querySelector('.tp151-calendar-frame');
   if(frame&&!main.querySelector('.tp155-move-banner'))frame.insertAdjacentHTML('beforebegin','<div class="tp155-move-banner" role="status"><span>'+esc(tx('moveHint'))+'</span><button type="button" class="btn secondary" onclick="tp155CancelCalendarMove()">'+esc(tx('cancel'))+'</button></div>');
  }
 };
 if(typeof window.tp155R4RefreshPanel==='function'){
  const refreshBase=window.tp155R4RefreshPanel;window.tp155R4RefreshPanel=function(){const out=refreshBase.apply(this,arguments);decorate();return out};
 }
 if(typeof window.tp155R4ClosePanel==='function'){
  const closeBase=window.tp155R4ClosePanel;window.tp155R4ClosePanel=function(){state.rf260MovingScheduleId=null;return closeBase.apply(this,arguments)};
 }
 const renderPhoneBase=render;
 render=function(){const out=renderPhoneBase.apply(this,arguments);decorate();return out};
 decorate();

 const style=document.createElement('style');style.id='tp155PhoneRound4Css';style.textContent=[
  '.rf2211-planbody label:has(#rf2211Start) .tp-temporal-trigger::after,.rf2211-planbody label:has(#rf2211Time) .tp-temporal-trigger::after{display:none!important}',
  '#tp155R4PanelHost:is([data-panel="calendar"],[data-panel="coach"],[data-panel="settings"]) .tp155-r4-panel-close{display:none!important}',
  '.tp155-language-attached{position:relative!important}.tp155-language-attached .tp-select-menu{position:absolute!important;left:0!important;right:auto!important;top:calc(100% + 4px)!important;bottom:auto!important;width:100%!important;max-height:min(300px,44dvh)!important}',
  '.tp155-attached-select .tp-select-menu[data-placement="top"]{top:auto!important;bottom:calc(100% + 4px)!important}',
  '.tp155-move-banner{display:flex;align-items:center;justify-content:space-between;gap:10px;margin:0 0 10px;padding:9px 10px;border:1px solid color-mix(in srgb,var(--accent) 50%,var(--line));border-radius:12px;background:color-mix(in srgb,var(--accent) 8%,var(--card));font-size:13px;font-weight:700}.tp155-move-banner .btn{min-height:36px;padding:6px 10px;flex:0 0 auto}',
  '.timer{position:static!important;left:auto!important;right:auto!important;bottom:auto!important;transform:none!important;width:auto!important;max-width:none!important;margin:10px max(12px,env(safe-area-inset-left,0px)) calc(12px + env(safe-area-inset-bottom,0px))!important;z-index:auto!important;pointer-events:auto!important;box-shadow:0 6px 18px #0004!important}',
  '.tp155-history-delete{margin:10px 0!important}',
  '.tp155-custom-plan-hint{margin:8px 0 2px;padding:8px 10px;border-radius:10px;background:color-mix(in srgb,var(--accent) 6%,var(--card2))}'
 ].join('');document.head.appendChild(style);
 window.TrainPilot155PhoneRound4={version:'1.5.5-phone-round4',statsClean:true,calendarCustom:true,calendarMoveInPlace:true,panelCloseHidden:true,languageAttached:true,restTimerFlow:true,historyDelete:true};
})();
// @endsection trainpilot-155-phone-round4-fixes.js

// @section trainpilot-155-quick-filter-cleanup.js
/* Phone-test cleanup: Quick Workout keeps the shared panel X and useful controls.
 * Remove only the redundant Quick Workout header card; keep the collapsible
 * muscle + equipment filter next to the persistent search field. */
(function(){
 'use strict';
 const labels={
  hu:{filter:'Szűrő',search:'Keresés',muscle:'Izomcsoport',gear:'Felszerelés',all:'Összes'},
  en:{filter:'Filter',search:'Search',muscle:'Muscle group',gear:'Equipment',all:'All'},
  de:{filter:'Filter',search:'Suche',muscle:'Muskelgruppe',gear:'Ausrüstung',all:'Alle'},
  ro:{filter:'Filtru',search:'Căutare',muscle:'Grupă musculară',gear:'Echipament',all:'Toate'},
  sk:{filter:'Filter',search:'Hľadať',muscle:'Svalová skupina',gear:'Vybavenie',all:'Všetky'},
  pl:{filter:'Filtr',search:'Szukaj',muscle:'Grupa mięśniowa',gear:'Sprzęt',all:'Wszystkie'}
 };
 const qlang=function(){const l=typeof rf212Lang==='function'?rf212Lang():'hu';return labels[l]?l:'hu'};
 const qtext=function(k){return labels[qlang()]?.[k]||labels.hu[k]||k};
 const quickState=function(){
  return {
   q:String(state.tp155QuickQuery||''),
   group:String(state.tp155QuickGroup||'all'),
   gear:String(state.tp155QuickGear||'all')
  };
 };
 const quickResults=function(q,group,gear){
  const list=exercises().filter(function(e){
   if(typeof libraryMatch==='function')return libraryMatch(e,group,gear,q);
   const needle=String(q||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim();
   return (group==='all'||rf205PrimaryMuscle(e.id)===group)&&(gear==='all'||libraryGear(e)===gear)&&(!needle||tp150QuickFilterText(e).includes(needle));
  });
  if(!list.length)return '<div class="card muted">'+esc(tp150qwT('empty'))+'</div>';
  return list.map(function(e,i){
   const name=e.custom?String(e.hu||e.en||e.id):tp149ExerciseName(e),equipment=e.custom?String(e.equipment||''):tp149Equipment(e),target=e.custom?String(e.target||''):tp149Target(e);
   return '<details class="exercise tp150-quick-row tp152-quick-row" data-exercise-id="'+esc(e.id)+'"><summary><div class="num">'+(i+1)+'</div><div class="tp150-quick-copy"><div class="ex-name">'+esc(name)+'</div><div class="meta">'+esc([target,equipment].filter(Boolean).join(' • '))+'</div></div><div class="tp-exercise-actions">'+rf260VideoButton(e.id)+'<span class="tp152-chevron" aria-hidden="true">⌄</span></div></summary><div class="tp152-quick-body">'+tp152QuickFields(e)+'<button type="button" class="btn block tp150-quick-start" onclick="event.stopPropagation();tp152StartQuickInline(\''+esc(e.id)+'\')">'+esc(tp150QuickButtonLabel())+'</button></div></details>';
  }).join('');
 };
 window.tp155QuickPanelHtml=function(){
  const f=quickState();
  const muscles=Object.keys(MUSCLES).map(function(key){return '<option value="'+esc(key)+'" '+(f.group===key?'selected':'')+'>'+esc(tp149MuscleGroupLabel(key))+'</option>'}).join('');
  const gears=Object.entries(TP149_LIBRARY_GEAR_ROWS).map(function(row){return '<option value="'+esc(row[0])+'" '+(f.gear===row[0]?'selected':'')+'>'+esc(tp149RowValue(row[1]))+'</option>'}).join('');
  return '<main class="tp150-quick-picker tp155-quick-panel tp155-quick-clean"><div class="tp155-panel-sticky tp155-quick-controls"><div class="tp160-quick-head"><strong class="tp160-quick-context">'+esc(tp150qwT('title'))+'</strong><label class="tp160-quick-search"><span>'+esc(qtext('search'))+'</span><input id="tp155QuickQuery" class="field" type="search" value="'+esc(f.q)+'" placeholder="'+esc(qtext('search'))+'" oninput="tp155QuickFilter()"></label></div><details class="tp155-quick-filter"><summary>'+esc(qtext('filter'))+'</summary><div class="tp155-filter-row tp155-quick-filter-row"><label>'+esc(qtext('muscle'))+'<select id="tp155QuickMuscle" class="field" onchange="tp155QuickFilter()"><option value="all">'+esc(qtext('all'))+'</option>'+muscles+'</select></label><label>'+esc(qtext('gear'))+'<select id="tp155QuickGear" class="field" onchange="tp155QuickFilter()">'+gears+'</select></label></div></details></div><div id="tp150QuickResults" class="tp155-panel-scroll">'+quickResults(f.q,f.group,f.gear)+'</div></main>';
 };
 window.tp155QuickFilter=function(){
  const q=document.getElementById('tp155QuickQuery')?.value??state.tp155QuickQuery??'';
  const group=document.getElementById('tp155QuickMuscle')?.value??state.tp155QuickGroup??'all';
  const gear=document.getElementById('tp155QuickGear')?.value??state.tp155QuickGear??'all';
  state.tp155QuickQuery=String(q||'');state.tp155QuickGroup=String(group||'all');state.tp155QuickGear=String(gear||'all');
  const host=document.getElementById('tp150QuickResults');if(host)host.innerHTML=quickResults(state.tp155QuickQuery,state.tp155QuickGroup,state.tp155QuickGear);
  try{rf260EnhanceSelects()}catch(_){}
 };
 const style=document.createElement('style');style.id='tp155QuickFilterCleanupCss';style.textContent=[
  '#tp155R4PanelHost[data-panel="quick"] .tp155-r4-panel-content{clear:none!important}',
  '#tp155R4PanelHost[data-panel="quick"] .tp155-quick-panel>.hero,.tp155-quick-clean>.hero{display:none!important}',
  '.tp155-quick-clean{padding-top:0!important}',
  '.tp155-quick-controls{margin-top:0!important;padding-top:4px!important}',
  '.tp155-quick-filter{margin-top:8px;border:1px solid var(--line);border-radius:12px;background:var(--card2);overflow:visible}',
  '.tp155-quick-filter>summary{min-height:42px;display:flex;align-items:center;padding:8px 11px;font-weight:850;cursor:pointer;list-style:none}',
  '.tp155-quick-filter>summary::-webkit-details-marker{display:none}',
  '.tp155-quick-filter>summary::after{content:"";width:8px;height:11px;margin-left:auto;background:var(--text);clip-path:polygon(0 0,100% 50%,0 100%);transition:transform .15s ease}',
  '.tp155-quick-filter[open]>summary::after{transform:rotate(90deg)}',
  '.tp155-quick-filter-row{padding:0 8px 8px}',
  '@media(max-width:350px){.tp155-quick-filter-row{grid-template-columns:1fr!important}}'
 ].join('');document.head.appendChild(style);
 window.TrainPilot155QuickFilterCleanup={version:'1.5.5-quick-filter-clean',collapsibleFilter:true,noHeader:true,closeX:true};
})();
// @endsection trainpilot-155-quick-filter-cleanup.js

// @section trainpilot-155-main-panel-continuity.js
/* Persistent main panels, recoverable journal photos and native Back parity. */
(function(){
 'use strict';
 const style=document.createElement('style');style.id='tp155MainPanelContinuityCss';style.textContent=[
  '#tp155R4PanelHost:is([data-panel="calendar"],[data-panel="coach"],[data-panel="settings"]) .tp155-r4-panel{border:1px solid color-mix(in srgb,var(--line) 82%,var(--accent) 18%)!important;border-radius:18px!important;margin-top:6px!important;box-shadow:0 16px 36px rgba(5,10,20,.14)!important}',
  '#tp155R4PanelHost:is([data-panel="calendar"],[data-panel="coach"],[data-panel="settings"]) .tp155-r4-panel::before{display:none!important}',
  '#tp155R4PanelHost:is([data-panel="calendar"],[data-panel="coach"],[data-panel="settings"]) .tp155-r4-panel-close{display:inline-grid!important}'
 ].join('');document.head.appendChild(style);
 function openOrRefreshCoach(){const host=document.getElementById('tp155R4PanelHost');if(host?.dataset?.panel==='coach'&&typeof window.tp155R4RefreshPanel==='function'){window.tp155R4RefreshPanel();return true}if(typeof window.tp155R4OpenPanel==='function')return window.tp155R4OpenPanel('coach',document.activeElement);return false}
 window.rf233CoachScreen=openOrRefreshCoach;window.rf220CoachScreen=openOrRefreshCoach;
 const deletePhotoBase=rf130DeletePhoto;
 rf130DeletePhoto=async function(key,id){await deletePhotoBase.apply(this,arguments);const deleted=rf130FindPhoto?.(key,id)?.p?.deletedAt;if(!deleted)return;setTimeout(function(){const item=document.querySelector('[data-tp152-history-key="'+CSS.escape(String(key))+'"]');if(item){item.open=true;rf130LoadHistoryPhotos?.(item)}},0)};
 const androidBackBase=window.TrainPilotAndroidBack;
 window.TrainPilotAndroidBack=function(){if(document.getElementById('rf130PhotoModal')){rf130ClosePhotoModal?.();return true}return typeof androidBackBase==='function'?androidBackBase.apply(this,arguments):false};
 window.TrainPilot155MainPanelContinuity={version:'1.5.5-main-panel-continuity',persistentNavigation:true,closedFrame:true,recoverableClose:true,coachRefreshInPlace:true,androidBack:true,journalPhotoContinuity:true};
})();
// @endsection trainpilot-155-main-panel-continuity.js

// @section trainpilot-155-panel-close-system.js
/* Unified panel close rail and navigation layering. */
(function(){
 'use strict';
 const style=document.createElement('style');style.id='tp155PanelCloseSystemCss';style.textContent=[
  '.top.tp154-nav-grid{position:sticky!important;top:0!important;z-index:16000!important;isolation:isolate}',
  'html.tp155-r4-panel-open .top.tp154-nav-grid{position:fixed!important;left:0!important;right:0!important;top:0!important;width:auto!important}',
  '.tp155-r4-panel-host{z-index:14500!important}',
  '#tp155R4PanelHost .tp155-r4-panel{--tp155-close-size:36px;box-sizing:border-box!important}',
  '#tp155R4PanelHost[data-panel] .tp155-r4-panel-close{display:grid!important}',
  '#tp155R4PanelHost .tp155-r4-panel-close{box-sizing:border-box!important;position:sticky!important;top:0!important;right:auto!important;left:auto!important;float:none!important;align-self:flex-end!important;flex:0 0 var(--tp155-close-size)!important;width:var(--tp155-close-size)!important;height:var(--tp155-close-size)!important;min-width:var(--tp155-close-size)!important;min-height:var(--tp155-close-size)!important;max-width:var(--tp155-close-size)!important;max-height:var(--tp155-close-size)!important;margin:0 0 6px auto!important;padding:0!important;display:grid!important;place-items:center!important;z-index:8!important;border:1px solid #744048!important;border-radius:11px!important;background:#472529!important;color:#ffd9dc!important;box-shadow:none!important;font:900 22px/1 sans-serif!important}',
  '#tp155R4PanelHost .tp155-r4-panel-close:active{background:#a33c48!important;border-color:#d96873!important;color:#fff!important;transform:scale(.96)}',
  '#tp155R4PanelHost .tp155-r4-panel-content{clear:none!important;width:100%!important;max-width:100%!important;min-width:0!important;box-sizing:border-box!important}',
  '@media(max-width:390px){#tp155R4PanelHost .tp155-r4-panel{--tp155-close-size:36px}}'
 ].join('');document.head.appendChild(style);
 window.TrainPilot155PanelCloseSystem={version:'1.5.5-panel-close-system',uniform:true,quickClose:true,dangerStyle:true,navigationAbovePanels:true};
})();
// @endsection trainpilot-155-panel-close-system.js

// @section trainpilot-155-exercise-library-layout.js
/* Keep the exercise library centered and its filter menus above the results. */
(function(){
 'use strict';
 const style=document.createElement('style');style.id='tp155ExerciseLibraryLayoutCss';style.textContent=[
  '#tp155R4PanelHost[data-panel="exercises"],#tp155R4PanelHost[data-panel="exercises"] .tp155-r4-panel,#tp155R4PanelHost[data-panel="exercises"] .tp155-r4-panel-content,#tp155R4PanelHost[data-panel="exercises"] .tp155-exercise-panel{max-width:100%!important;min-width:0!important;box-sizing:border-box!important;overflow-x:hidden!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-exercise-panel>*{min-width:0!important;max-width:100%!important;box-sizing:border-box!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-library-filters{position:relative!important;z-index:20!important;overflow:visible!important;isolation:isolate}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-library-filters .tp-select{position:relative!important;z-index:21!important;min-width:0!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-library-filters .tp-select.open{z-index:40!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-library-filters .tp-select-menu{position:absolute!important;left:0!important;right:auto!important;top:calc(100% + 4px)!important;bottom:auto!important;width:100%!important;max-width:none!important;max-height:min(280px,44dvh)!important;z-index:41!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-library-filters .tp-select-menu[data-placement="top"]{top:auto!important;bottom:calc(100% + 4px)!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-library-results{position:relative!important;z-index:1!important;min-width:0!important;overflow-x:hidden!important}',
  '#tp155R4PanelHost[data-panel="exercises"] :is(img,video,canvas,svg){max-width:100%!important}'
 ].join('');document.head.appendChild(style);
 window.TrainPilot155ExerciseLibraryLayout={version:'1.5.5-exercise-library-layout',centered:true,noHorizontalDrift:true,filterMenusAboveResults:true};
})();
// @endsection trainpilot-155-exercise-library-layout.js

// @section trainpilot-155-settings-controls.js
/* Compact Settings controls: scrollable theme picker, sync checks and save state. */
(function(){
 'use strict';
 const themeRows=function(keys){const current=rf200ThemeKey();return keys.map(function(key){const theme=RF200_THEMES[key];return '<button type="button" class="tp155-theme-option '+(key===current?'active':'')+'" data-theme="'+esc(key)+'" aria-pressed="'+(key===current?'true':'false')+'" onclick="tp155ChooseTheme(\''+esc(key)+'\')"><span class="tp155-theme-swatch" style="background:'+esc(theme.accent)+'"></span><span>'+esc(tp1511ThemeName(key))+'</span><i aria-hidden="true">✓</i></button>'}).join('')};
 window.tp155ThemePickerHtml=function(){return '<div class="tp-modal-card tp-temporal-card tp155-theme-picker" role="dialog" aria-modal="true" aria-labelledby="tp155ThemeTitle" onclick="event.stopPropagation()"><div class="tp-modal-head"><h2 id="tp155ThemeTitle">'+esc(tp152T('theme'))+'</h2><button type="button" class="btn danger tp-modal-close" onclick="tp155CloseThemePicker()">×</button></div><div class="tp155-theme-list"><div class="small muted tp155-theme-group">'+esc(tp1511T('basic'))+'</div>'+themeRows(TP1511_BASIC)+'<div class="small muted tp155-theme-group">'+esc(tp1511T('vivid'))+'</div>'+themeRows(TP1511_VIVID)+'</div></div>'};
 window.tp155OpenThemePicker=function(){window.tp155CloseThemePicker();const modal=document.createElement('div');modal.id='tp155ThemePicker';modal.className='tp-modal tp-temporal-modal tp155-theme-modal';modal.innerHTML=window.tp155ThemePickerHtml();modal.addEventListener('click',function(e){if(e.target===modal)window.tp155CloseThemePicker()});document.body.appendChild(modal);modal.querySelector('.tp155-theme-option.active,.tp155-theme-option')?.focus?.()};
 window.tp155CloseThemePicker=function(){document.getElementById('tp155ThemePicker')?.remove()};
 window.tp155ChooseTheme=function(key){if(!RF200_THEMES[key])return;rf200SetTheme(key);const modal=document.getElementById('tp155ThemePicker');if(!modal)return;modal.innerHTML=window.tp155ThemePickerHtml();modal.querySelector('.tp155-theme-option.active')?.scrollIntoView?.({block:'nearest'});modal.querySelector('.tp155-theme-option.active')?.focus?.()};
 rf200ThemePanel=function(){const key=rf200ThemeKey(),theme=RF200_THEMES[key];return '<div class="setting tp1511-card tp155-theme-setting"><div class="tp1511-head"><b>◐</b><div><strong>'+esc(tp152T('theme'))+'</strong><p class="small muted">'+esc(tp1511T('themeHelp'))+'</p></div></div><button type="button" class="btn secondary block tp155-theme-open" onclick="tp155OpenThemePicker()"><span class="tp155-theme-swatch" style="background:'+esc(theme.accent)+'"></span><span>'+esc(tp1511ThemeName(key))+'</span><i aria-hidden="true">›</i></button></div>'};
 const cloudPanelBase=cloudPanel;
 cloudPanel=function(){return String(cloudPanelBase.apply(this,arguments)).replace(/<label><input type="checkbox"/g,'<label class="tp155-sync-check"><input type="checkbox"')};
 const backBase=window.TrainPilotAndroidBack;
 window.TrainPilotAndroidBack=function(){if(document.getElementById('tp155ThemePicker')){window.tp155CloseThemePicker();return true}return typeof backBase==='function'?backBase.apply(this,arguments):false};
 const style=document.createElement('style');style.id='tp155SettingsControlsCss';style.textContent=[
  '.tp155-theme-setting{width:100%!important;max-width:100%!important;margin-inline:0!important;box-sizing:border-box!important}',
  '.tp155-theme-open{display:grid!important;grid-template-columns:20px minmax(0,1fr) auto!important;align-items:center!important;gap:10px!important;text-align:left!important;margin-top:8px!important}',
  '.tp155-theme-open i{font-size:22px;font-style:normal;color:var(--accent2)}',
  '.tp155-theme-swatch{display:block;width:18px;height:18px;border-radius:50%;border:1px solid #ffffff3b;box-shadow:0 0 0 1px #0005}',
  'body>#tp155ThemePicker.tp155-theme-modal{position:fixed!important;inset:0!important;z-index:18000!important;display:grid!important;place-items:center!important;padding:max(14px,env(safe-area-inset-top,0px)) max(12px,env(safe-area-inset-right,0px)) max(14px,env(safe-area-inset-bottom,0px)) max(12px,env(safe-area-inset-left,0px))!important}.tp155-theme-picker{width:min(460px,100%)!important;max-height:min(82dvh,680px)!important;display:flex!important;flex-direction:column!important;overflow:hidden!important}',
  '.tp155-theme-list{min-height:0;overflow-y:auto;overscroll-behavior:contain;padding:6px 2px 2px}',
  '.tp155-theme-group{padding:8px 8px 5px;font-weight:850;text-transform:uppercase;letter-spacing:.04em}',
  '.tp155-theme-option{appearance:none;width:100%;min-height:44px;display:grid;grid-template-columns:22px minmax(0,1fr) 22px;align-items:center;gap:10px;padding:8px 10px;border:1px solid transparent;border-radius:11px;background:transparent;color:var(--text);font:inherit;font-size:14px;font-weight:750;line-height:1.2;text-align:left}',
  '.tp155-theme-option i{visibility:hidden;font-style:normal;color:var(--accent2);font-weight:950}.tp155-theme-option.active{border-color:color-mix(in srgb,var(--accent) 60%,var(--line));background:color-mix(in srgb,var(--accent) 14%,var(--card2))}.tp155-theme-option.active i{visibility:visible}',
  '.tp155-sync-check{display:flex!important;align-items:center!important;gap:9px!important;min-height:34px!important;margin:3px 0!important;padding:4px 6px!important;border-radius:10px;background:color-mix(in srgb,var(--card2) 76%,transparent);font-size:13px!important;font-weight:750!important}',
  '.tp155-sync-check input[type="checkbox"]{appearance:none!important;-webkit-appearance:none!important;display:grid!important;place-content:center!important;flex:0 0 22px!important;width:22px!important;height:22px!important;margin:0!important;border:1px solid color-mix(in srgb,var(--accent) 45%,var(--line))!important;border-radius:7px!important;background:var(--card)!important}',
  '.tp155-sync-check input[type="checkbox"]::before{content:"";width:10px;height:6px;border-left:3px solid #10151c;border-bottom:3px solid #10151c;transform:rotate(-45deg) scale(0);transition:transform .12s ease}',
  '.tp155-sync-check input[type="checkbox"]:checked{background:var(--accent)!important;border-color:var(--accent)!important}.tp155-sync-check input[type="checkbox"]:checked::before{transform:rotate(-45deg) scale(1)}',
  ':is(.tp152-settings,.tp1511-training) .btn[onclick*="saveSettings"],.tp1511-training .btn[onclick*="tp1511SaveTraining"]{background:#274b38!important;border-color:#3f7657!important;color:#dff7e8!important;box-shadow:none!important}',
  ':is(.tp152-settings,.tp1511-training) .btn[onclick*="saveSettings"]:active,.tp1511-training .btn[onclick*="tp1511SaveTraining"]:active{background:#39a866!important;border-color:#66d58d!important;color:#fff!important}'
 ].join('');document.head.appendChild(style);
 window.TrainPilot155SettingsControls={version:'1.5.5-settings-controls',scrollThemePicker:true,compactSyncChecks:true,greenSave:true};
})();
// @endsection trainpilot-155-settings-controls.js

// @section trainpilot-155-calendar-planner-compact.js
/* The calendar planner is always visible and uses the phone space efficiently. */
(function(){
 'use strict';
 rf2211Planner=function(){
  const s=plannerSettings(),p=activeProgram(),days=rf2211Days(),today=new Date().toISOString().slice(0,10),mode=rf230PlannerMode(),custom=mode==='custom';
  const customLabel=typeof tp149PlannerRow==='function'?tp149PlannerRow('cadence','custom'):'Naptárban kijelölöm';
  const customHint={hu:'Koppints a naptárban arra a napra, amelyikre edzést szeretnél tenni.',en:'Tap the calendar day where you want to place a workout.',de:'Tippe im Kalender auf den Tag, an dem du trainieren möchtest.',ro:'Atinge în calendar ziua în care vrei să programezi antrenamentul.',sk:'Ťukni v kalendári na deň, na ktorý chceš pridať tréning.',pl:'Dotknij dnia w kalendarzu, na który chcesz dodać trening.'};
  const hint=customHint[typeof rf212Lang==='function'?rf212Lang():'hu']||customHint.hu;
  return '<section class="card rf2211-planner tp155-planner-always-open"><div class="tp155-planner-title"><strong>'+esc(tp149T('calendar.planningSettings'))+'</strong></div><div class="rf2211-planbody"><label class="small">'+esc(tp149T('calendar.rhythm'))+'<select class="field" id="rf230Mode" onchange="rf230ModeChanged()"><option value="alternate" '+(mode==='alternate'?'selected':'')+'>'+esc(tp149T('calendar.cadence.alternate'))+'</option><option value="daily" '+(mode==='daily'?'selected':'')+'>'+esc(tp149T('calendar.cadence.daily'))+'</option><option value="weekly" '+(mode==='weekly'?'selected':'')+'>'+esc(tp149T('calendar.cadence.weekly'))+'</option><option value="custom" '+(custom?'selected':'')+'>'+esc(customLabel)+'</option></select></label><div class="grid2"><label class="small tp155-auto-plan-only" style="'+(custom?'display:none':'')+'">'+esc(tp149T('calendar.startDate'))+'<input class="field" id="rf2211Start" type="date" value="'+today+'"></label><label class="small">'+esc(tp149T('calendar.time'))+'<input class="field" id="rf2211Time" type="time" value="'+esc(s.time)+'" onchange="tp155SavePlannerBasics()"></label><label class="small">'+esc(tp149T('calendar.duration'))+'<input class="field" id="rf2211Minutes" type="number" min="10" max="240" value="'+s.minutes+'" onchange="tp155SavePlannerBasics()"></label><label class="small tp155-auto-plan-only" style="'+(custom?'display:none':'')+'">'+esc(tp149T('calendar.weeks'))+'<input class="field" id="rf2211Weeks" type="number" min="1" max="12" value="4"></label></div><label class="small tp155-auto-plan-only" style="'+(custom?'display:none':'')+'">'+esc(tp149T('calendar.firstDay'))+'<select class="field" id="rf2211First">'+days.map(function(d,i){return '<option value="'+i+'">'+esc(tp149ProgramDayName(p,d))+'</option>';}).join('')+'</select></label>'+rf230WeekdayRow(mode)+'<p class="small muted tp155-custom-plan-hint" style="'+(custom?'display:block':'display:none')+'">'+esc(hint)+'</p><button class="btn block tp155-auto-plan-only" style="'+(custom?'display:none':'')+'" onclick="rf2211PlanWeeks()">'+esc(tp149T('calendar.plan'))+'</button></div></section>';
 };
 const style=document.createElement('style');style.id='tp155CalendarPlannerCompactCss';style.textContent=[
  '.tp155-planner-always-open{padding:8px!important;margin:0!important;border-color:color-mix(in srgb,var(--accent) 28%,var(--line))!important}',
  '.tp155-planner-title{display:flex;align-items:center;min-height:26px;padding:0 2px 5px;font-size:14px}',
  '.tp155-planner-always-open .rf2211-planbody{display:grid;gap:6px;margin:0!important}',
  '.tp155-planner-always-open .rf2211-planbody>.grid2{gap:6px!important}',
  '.tp155-planner-always-open label.small{display:grid;gap:3px;font-size:11px!important;font-weight:750}',
  '.tp155-planner-always-open .field,.tp155-planner-always-open .tp-select-trigger,.tp155-planner-always-open .tp-temporal-trigger{min-height:36px!important;height:36px!important;padding-block:5px!important}',
  '.tp155-planner-always-open .rf2211-weekdays{gap:5px!important;margin:1px 0!important;flex-wrap:nowrap!important;justify-content:space-between}',
  '.tp155-planner-always-open .rf2211-weekdays label{gap:2px!important;font-size:10px!important}',
  '.tp155-planner-always-open .btn.block{min-height:38px!important;margin:0!important;padding:7px 10px!important}',
  '.tp155-planner-always-open .tp155-custom-plan-hint{margin:0!important;padding:6px 8px!important}'
 ].join('');document.head.appendChild(style);
 window.TrainPilot155CalendarPlanner={version:'1.5.5-calendar-planner',alwaysOpen:true,compact:true};
})();
// @endsection trainpilot-155-calendar-planner-compact.js

// @section trainpilot-155-workout-settings-compact.js
/* Compact, outlined workout settings without changing its fields or handlers. */
(function(){
 'use strict';
 const style=document.createElement('style');style.id='tp155WorkoutSettingsCompactCss';style.textContent=[
  '.tp1511-training{border:1px solid var(--accent)!important;background-color:var(--card)!important;background-image:linear-gradient(145deg,var(--card) 0%,var(--card2) 100%)!important;box-shadow:inset 3px 0 0 var(--accent)!important}',
  '.tp1511-training>summary{min-height:46px!important;padding:7px 10px!important;gap:8px}',
  '.tp1511-training>summary strong{font-size:13px}.tp1511-training>summary small{font-size:10px;line-height:1.2}',
  '.tp1511-training-body{display:grid!important;gap:6px!important;padding:7px 10px 9px!important;border-top-color:color-mix(in srgb,var(--accent) 26%,var(--line))!important}',
  '.tp1511-training-body>p{margin:0!important;line-height:1.25!important}',
  '.tp1511-training-body>label,.tp1511-training .tp1511-prog label{display:grid;gap:3px;font-size:11px!important;font-weight:750}',
  '.tp1511-training .field{min-height:36px!important;height:36px!important;padding-block:5px!important}',
  '.tp1511-training .tp1511-section{margin:2px 0 0!important;padding:6px 8px!important;border:1px solid color-mix(in srgb,var(--accent) 28%,var(--line))!important;border-radius:9px!important;background:color-mix(in srgb,var(--accent) 6%,var(--card2))!important;font-size:11px!important}',
  '.tp1511-training .tp1511-prog{gap:5px!important}',
  '.tp1511-training .btn.block{min-height:38px!important;margin-top:1px!important;padding:7px 10px!important}'
 ].join('');document.head.appendChild(style);
 const tp155DecorateWorkoutSettings=function(){const el=document.querySelector('.tp1511-training');if(!el)return;el.style.setProperty('border','1px solid var(--accent)','important');el.style.setProperty('background-color','var(--card)','important');el.style.setProperty('background-image','linear-gradient(145deg,var(--card) 0%,var(--card2) 100%)','important');el.style.setProperty('box-shadow','inset 3px 0 0 var(--accent)','important')};
 const tp155WorkoutSettingsRenderBase=render;render=function(){const out=tp155WorkoutSettingsRenderBase.apply(this,arguments);tp155DecorateWorkoutSettings();return out};tp155DecorateWorkoutSettings();
 window.TrainPilot155WorkoutSettings={version:'1.5.5-workout-settings',compact:true,colorOutline:true};
})();
// @endsection trainpilot-155-workout-settings-compact.js

// @section repforge-160-phone-release-polish.js
/* RepForge 1.6 phone fixes: compact Quick header, anchored filters, shared Journal arrow. */
(function(){
 'use strict';
 const style=document.createElement('style');style.id='repforge160PhoneReleaseCss';style.textContent=[
  '#tp155R4PanelHost[data-panel="quick"] .tp155-r4-panel-close{position:absolute!important;top:10px!important;right:10px!important;float:none!important;margin:0!important;z-index:8}',
  '#tp155R4PanelHost[data-panel="quick"] .tp155-quick-controls{padding:4px 0 0!important}',
  '.tp160-quick-head{display:grid;grid-template-columns:auto minmax(0,1fr);align-items:center;gap:8px;padding-right:44px}',
  '.tp160-quick-search{position:relative}.tp160-quick-search>span{position:absolute!important;width:1px!important;height:1px!important;padding:0!important;margin:-1px!important;overflow:hidden!important;clip:rect(0,0,0,0)!important;white-space:nowrap!important;border:0!important}',
  '.tp160-quick-context{align-self:center;font-size:12px;line-height:1.15;white-space:nowrap;color:var(--muted)}',
  '.tp160-quick-search{display:grid;gap:2px;min-width:0;font-size:10px;font-weight:800;color:var(--muted)}',
  '.tp160-quick-search .field{min-width:0;min-height:36px!important;height:36px!important;padding:6px 9px!important}',
  '.tp155-quick-filter-row>label{min-width:0}.tp155-quick-filter-row .tp-select{position:relative!important}',
  '.tp155-quick-filter-row .tp-select-menu{position:absolute!important;left:0!important;right:auto!important;top:calc(100% + 4px)!important;bottom:auto!important;width:100%!important;max-width:100%!important;max-height:min(240px,38dvh)!important}',
  '.tp160-journal-disclosure{box-sizing:border-box;display:inline-grid;place-items:center;width:18px;height:18px;min-width:18px;flex:0 0 18px;margin-left:6px}',
  '.tp160-journal-disclosure::before{content:"";display:block;width:9px;height:12px;background:var(--text);clip-path:polygon(0 0,100% 50%,0 100%);transform:rotate(0deg);transform-origin:50% 50%;transition:transform .15s ease}',
  '.tp155-journal-filter[open]>summary>.tp160-journal-disclosure::before{transform:rotate(90deg)}',
  '@media(max-width:350px){.tp160-quick-head{grid-template-columns:1fr;padding-right:42px;gap:3px}.tp160-quick-context{font-size:11px}}'
 ].join('');
 document.head.appendChild(style);
 window.RepForge160={version:'1.6.0',quickHeader:true,anchoredQuickFilters:true,journalDisclosure:true};
})();
// @endsection repforge-160-phone-release-polish.js

// @section trainpilot-161-minor-fixes.js
/* TrainPilot 1.6.1: Android panel/IME polish and bilateral stopwatch persistence/UI fixes. */
(function(){
 'use strict';

 const TP161_VERSION='1.6.1';

 // Keep Calendar / Coach / Settings closer to the fixed 2x4 navigation and
 // keep any open panel inside Android's visual viewport while the IME is visible.
 window.tp161AdjustPanel=function tp161AdjustPanel(){
  const host=document.getElementById('tp155R4PanelHost');if(!host)return;
  const nav=document.querySelector('.top.tp154-nav-grid'),rect=nav?.getBoundingClientRect?.();
  const vv=window.visualViewport;
  const layoutHeight=Math.max(0,Number(window.innerHeight)||0);
  const visualHeight=Math.max(0,Number(vv?.height)||layoutHeight);
  const visualTop=Math.max(0,Number(vv?.offsetTop)||0);
  const imeOpen=!!(vv&&layoutHeight>0&&(layoutHeight-visualHeight)>80);
  const visualBottom=imeOpen?Math.max(0,layoutHeight-(visualTop+visualHeight)):0;
  const compactPanel=/^(calendar|coach|settings)$/.test(String(host.dataset.panel||''));
  const lift=compactPanel?4:0;
  const navBottom=rect?Math.max(0,rect.bottom):0;
  host.style.setProperty('top',Math.round(Math.max(0,navBottom+(imeOpen?visualTop:0)-lift))+'px','important');
  host.style.setProperty('bottom',Math.round(visualBottom)+'px','important');
  host.classList.toggle('tp161-ime-open',imeOpen);
 };

 const refreshBase=window.tp155R4RefreshPanel;
 if(typeof refreshBase==='function')window.tp155R4RefreshPanel=function(){
  const out=refreshBase.apply(this,arguments);window.tp161AdjustPanel();return out;
 };
 const openBase=window.tp155R4OpenPanel;
 if(typeof openBase==='function')window.tp155R4OpenPanel=function(){
  const out=openBase.apply(this,arguments);window.tp161AdjustPanel();
  try{requestAnimationFrame(()=>window.tp161AdjustPanel())}catch(_){}
  return out;
 };

 const adjustSoon=function(){try{requestAnimationFrame(()=>window.tp161AdjustPanel())}catch(_){window.tp161AdjustPanel()}};
 window.addEventListener?.('resize',adjustSoon,{passive:true});
 window.addEventListener?.('scroll',adjustSoon,{passive:true});
 window.visualViewport?.addEventListener?.('resize',adjustSoon,{passive:true});
 window.visualViewport?.addEventListener?.('scroll',adjustSoon,{passive:true});
 document.addEventListener?.('focusin',function(e){
  if(document.getElementById('tp155R4PanelHost')?.contains(e.target))setTimeout(adjustSoon,0);
 });

 // Make bilateral timing visibly write back to the active workout set row as
 // well as to the draft data. This also gives the phone UI immediate feedback.
 window.tp161SyncPerSideRow=function tp161SyncPerSideRow(setIndex){
  const ex=state.session?.exercises?.[state.current];if(!ex||!tp1481IsPerSideTimed(ex))return;
  const set=ex.sets?.[setIndex];if(!set)return;
  const sides=tp1481SyncPerSideReps(set);
  const row=[...document.querySelectorAll('main .row')][setIndex];
  if(!row)return;
  const input=[...row.querySelectorAll('input.field')].at(-1);
  if(input&&document.activeElement!==input)input.value=set.reps||'';
  if(input){
   const left=sides.left||'—',right=sides.right||'—';
   input.setAttribute('aria-label',(typeof tp149T==='function'?tp149T('stopwatch.leftShort'):'Bal')+' '+left+' / '+(typeof tp149T==='function'?tp149T('stopwatch.rightShort'):'Jobb')+' '+right);
   if(!set.reps)input.placeholder=(typeof tp149T==='function'?tp149T('stopwatch.leftShort'):'Bal')+' '+left+' • '+(typeof tp149T==='function'?tp149T('stopwatch.rightShort'):'Jobb')+' '+right;
  }
 };

 if(typeof tp1481PersistSide==='function'){
  const persistSideBase=tp1481PersistSide;
  tp1481PersistSide=function(sec){
   const setIndex=tp1481SideStopwatch?.setIndex;
   const out=persistSideBase.apply(this,arguments);
   if(setIndex>=0)window.tp161SyncPerSideRow(setIndex);
   return out;
  };
 }
 if(typeof tp1481SetSideSeconds==='function'){
  const setSideBase=tp1481SetSideSeconds;
  tp1481SetSideSeconds=function(setIndex){
   const out=setSideBase.apply(this,arguments);window.tp161SyncPerSideRow(setIndex);return out;
  };
 }
 if(typeof tp1481ResetSide==='function'){
  const resetSideBase=tp1481ResetSide;
  tp1481ResetSide=function(){
   const ex=state.session?.exercises?.[state.current],setIndex=ex?.sets?.findIndex?.(s=>!s.done);
   const out=resetSideBase.apply(this,arguments);
   if(setIndex>=0)window.tp161SyncPerSideRow(setIndex);
   return out;
  };
 }
 if(typeof tp1481DecoratePerSide==='function'){
  const decorateSideBase=tp1481DecoratePerSide;
  tp1481DecoratePerSide=function(){
   const out=decorateSideBase.apply(this,arguments);
   const host=document.querySelector('#rf110Stopwatch.tp1481-side-stopwatch');
   if(host){
    host.classList.add('tp161-side-stopwatch');host.classList.remove('card');
    host.querySelectorAll('.tp1481-side-card').forEach(x=>x.classList.add('card','tp161-side-window'));
    const ex=state.session?.exercises?.[state.current],setIndex=ex?.sets?.findIndex?.(s=>!s.done);
    if(setIndex>=0)window.tp161SyncPerSideRow(setIndex);
   }
   return out;
  };
 }

 const style=document.createElement('style');style.id='tp161MinorFixCss';style.textContent=[
  '#tp155R4PanelHost:is([data-panel="calendar"],[data-panel="coach"],[data-panel="settings"]){padding-top:0!important}',
  '#tp155R4PanelHost:is([data-panel="calendar"],[data-panel="coach"],[data-panel="settings"]) .tp155-r4-panel-close{position:absolute!important;top:10px!important;right:10px!important;float:none!important;margin:0!important}',
  '#tp155R4PanelHost:is([data-panel="calendar"],[data-panel="coach"],[data-panel="settings"]) .tp155-r4-panel-content{clear:none!important}',
  '#tp155R4PanelHost[data-panel="calendar"] .calendar-head{padding-right:48px!important;min-height:44px}',
  '#tp155R4PanelHost[data-panel="coach"] .tp155-r4-panel-content>main>h1:first-child{margin-top:2px!important;padding-right:48px;min-height:40px;display:flex;align-items:center}',
  '#tp155R4PanelHost[data-panel="settings"] .tp155-r4-panel-content>main>.hero:first-child{margin-top:0!important;padding-right:48px!important}',
  '#tp155R4PanelHost.tp161-ime-open .tp155-r4-panel{max-height:100%!important}',
  '.tp161-side-stopwatch{border:0!important;background:transparent!important;box-shadow:none!important;padding:0!important}',
  '.tp161-side-stopwatch>.rf110-stopwatch-head{padding:0 2px 8px;margin:0}',
  '.tp161-side-stopwatch>.tp1481-side-grid{grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:10px;margin-top:0}',
  '.tp161-side-stopwatch .tp161-side-window{min-width:0;margin:0!important}',
  '.tp161-side-stopwatch>#tp1481SideStatus{margin:10px 2px 0}',
  '@media(max-width:340px){.tp161-side-stopwatch>.tp1481-side-grid{grid-template-columns:1fr!important}}'
 ].join('');
 document.head?.appendChild(style);

 window.RepForge161={version:TP161_VERSION,visualViewportIme:true,compactPanels:true,separateSideCards:true,perSidePersistence:true};
 window.tp161AdjustPanel();
})();
// @endsection trainpilot-161-minor-fixes.js


// @section trainpilot-162-minor-ui.js
/* TrainPilot 1.6.2: minor phone UI polish and Home Health-count consistency. */
(function(){
 'use strict';
 const TP162_VERSION='1.6.2';
 const dailyBase=rf223Daily;
 window.tp162VisibleHealthSessions=function tp162VisibleHealthSessions(d){
   const sessions=Array.isArray(d?.exerciseSessions)?d.exerciseSessions:[];
   const local=(typeof history==='function'?history():[]).filter(function(h){return Number.isFinite(Date.parse(h?.started))&&Number.isFinite(Date.parse(h?.finished));});
   const ownPackage='com.repforge.app',exactTolerance=1000;
   const stableIds=new Set(local.map(function(h){return h?.healthStableId?'trainpilot:'+h.healthStableId:'';}).filter(Boolean));
   return sessions.filter(function(s){
     if(String(s?.source||'')!==ownPackage)return true;
     const clientRecordId=String(s?.clientRecordId||'');
     if(clientRecordId.indexOf('trainpilot:tpw_')===0)return stableIds.has(clientRecordId);
     const start=Date.parse(s?.start),end=Date.parse(s?.end);if(!Number.isFinite(start)||!Number.isFinite(end))return false;
     return local.some(function(h){return Math.abs(Date.parse(h.started)-start)<=exactTolerance&&Math.abs(Date.parse(h.finished)-end)<=exactTolerance;});
   });
 };
 rf223Daily=function(){const d=dailyBase.apply(this,arguments);if(!d)return d;const visible=window.tp162VisibleHealthSessions(d);const minutes=visible.reduce(function(sum,s){return sum+Math.max(0,Number(s?.durationMinutes)||0);},0);return Object.assign({},d,{exerciseSessions:visible,exerciseSessionCount:visible.length,exerciseMinutes:minutes});};

 if(typeof window.tp155R4DecorateNavigation==='function'){
  const decorateNavBase=window.tp155R4DecorateNavigation;
  window.tp155R4DecorateNavigation=function(){
   const out=decorateNavBase.apply(this,arguments),host=document.getElementById('tp155R4PanelHost');
   if(host?.dataset?.panel==='exercises'){
    const buttons=[...document.querySelectorAll('.top.tp154-nav-grid .tp151-nav-item')];
    const programs=buttons.find(function(btn){return /go\(['"]programs['"]\)/.test(String(btn.getAttribute('onclick')||''))});
    if(programs){programs.classList.add('active');programs.setAttribute('aria-expanded','true')}
   }
   return out;
  };
 }

 const style=document.createElement('style');style.id='tp162MinorUiCss';style.textContent=[
  '.tp-brand-strip{display:none!important}',
  '.top.tp154-nav-grid{top:0!important}',
  'main.rf221-home>.onboarding{margin:0!important;padding:9px 11px 10px!important;display:grid!important;gap:6px!important}',
  'main.rf221-home>.onboarding h2{margin:0!important;line-height:1.15!important}',
  'main.rf221-home>.onboarding p{margin:0!important;line-height:1.25!important}',
  'main.rf221-home>.onboarding .btn{margin:0!important;min-height:40px!important;padding:8px 10px!important}',
  'main.rf221-home{min-height:0!important}',
  '#tp155R4PanelHost:is([data-panel="calendar"],[data-panel="coach"],[data-panel="settings"],[data-panel="exercises"]) .tp155-r4-panel-close{position:absolute!important;top:14px!important;right:14px!important;float:none!important;margin:0!important;z-index:9!important}',
  '#tp155R4PanelHost:is([data-panel="calendar"],[data-panel="coach"],[data-panel="settings"],[data-panel="exercises"]) .tp155-r4-panel-content{clear:none!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-exercise-panel>.hero{margin:0!important;padding:5px 50px 5px 4px!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-exercise-panel>.hero h1{margin:0 0 3px!important;line-height:1.12!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-exercise-panel>.hero p{margin:0!important;line-height:1.25!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-library-filters{margin:6px 0!important;padding:7px!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-library-results>p.muted{margin:4px 0 6px!important;line-height:1.2!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-library-results .tp-library-card{margin:4px 0!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp-library-card>summary{padding:8px 10px!important;min-height:52px!important;gap:8px!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp-library-card>summary h3{margin:0 0 2px!important;line-height:1.12!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp-library-card>summary p{margin:0!important;line-height:1.18!important}',
  '#tp155R4PanelHost[data-panel="exercises"] .tp155-library-results{scroll-padding-top:4px!important;padding-bottom:5px!important}',
  '#tp155R4PanelHost[data-panel="calendar"] .tp155-r4-calendar-frame{padding:7px!important}',
  '#tp155R4PanelHost[data-panel="calendar"] .calendar-head{min-height:40px!important;margin-bottom:6px!important;padding-right:50px!important;grid-template-columns:46px 1fr 46px!important;gap:6px!important}',
  '#tp155R4PanelHost[data-panel="calendar"] .calendar-head .btn{min-height:38px!important;padding:6px!important}',
  '#tp155R4PanelHost[data-panel="calendar"] .cal-weekdays{gap:2px!important}',
  '#tp155R4PanelHost[data-panel="calendar"] .cal-weekdays span{padding:3px 0!important}',
  '#tp155R4PanelHost[data-panel="calendar"] .calendar-grid{gap:2px!important}',
  '#tp155R4PanelHost[data-panel="calendar"] .cal-cell{min-height:42px!important;padding:3px!important;border-radius:10px!important}',
  '#tp155R4PanelHost[data-panel="calendar"] .tp155-r4-planner-bottom{margin-top:7px!important}',
  '#tp155R4PanelHost[data-panel="calendar"] .tp155-planner-always-open{padding:7px!important}',
  '@media(max-width:360px){#tp155R4PanelHost[data-panel="calendar"] .cal-cell{min-height:40px!important}#tp155R4PanelHost[data-panel="exercises"] .tp-library-card>summary{padding:7px 8px!important;min-height:50px!important}}'
 ].join('');
 document.head?.appendChild(style);
 window.TrainPilot162={version:TP162_VERSION,staleOwnHealthCountGuard:true,compactHome:true,cleanTopNav:true,compactCalendar:true,compactExerciseLibrary:true,parentProgramsActive:true,insetPanelClose:true};
})();
// @endsection trainpilot-162-minor-ui.js

// @section trainpilot-162-followup.js
/* TrainPilot 1.6.2 follow-up: isolated UI regression fixes. */
(function(){
 'use strict';

 const cleanupChrome=function(){
  document.querySelectorAll('.tp-brand-strip').forEach(function(el){el.remove()});
  if(state?.tab==='health'){
   const health=document.querySelector('main.rf263-health');
   health?.querySelector(':scope > .hero:first-child,:scope > .tp151-page-head:first-child')?.remove();
  }
 };
 window.tp162CleanupChrome=cleanupChrome;

 // Preserve parent navigation state for panel subviews.
 if(typeof window.tp155R4DecorateNavigation==='function'){
  const decorateBase=window.tp155R4DecorateNavigation;
  window.tp155R4DecorateNavigation=function(){
   const out=decorateBase.apply(this,arguments),host=document.getElementById('tp155R4PanelHost');
   if(host?.dataset?.panel==='quick'){
    const buttons=[...document.querySelectorAll('.top.tp154-nav-grid .tp151-nav-item')];
    const workout=buttons.find(function(btn){return /go\(['"]plan['"]\)/.test(String(btn.getAttribute('onclick')||''))});
    if(workout){workout.classList.add('active');workout.setAttribute('aria-expanded','true')}
   }
   return out;
  };
 }

 // Workout landing card keeps a single Quick Workout title (no duplicate badge/title pair).
 if(typeof tp150CompactQuickCard==='function'){
  tp150CompactQuickCard=function(){
   return '<div class="card tp150-quick-entry tp150-quick-entry-compact"><div class="tp150-quick-copy"><h2>'+esc(tp150qwT('cardTitle'))+'</h2><p class="small muted">'+esc(tp150qwT('cardHelp'))+'</p></div><button class="btn block" onclick="tp155R4TogglePanel(\'quick\',this)">'+esc(tp150qwT('start'))+'</button></div>';
  };
 }

 // Quick Workout keeps only the compact context title; legacy explanatory hero/page headers stay out.
 if(typeof window.tp155QuickPanelHtml==='function'){
  const quickHtmlBase=window.tp155QuickPanelHtml;
  window.tp155QuickPanelHtml=function(){
   const html=String(quickHtmlBase.apply(this,arguments)||''),tpl=document.createElement('template');tpl.innerHTML=html;
   const main=tpl.content.querySelector('main');
   main?.querySelectorAll(':scope > .hero,:scope > .tp151-page-head').forEach(function(el){el.remove()});
   return main?main.outerHTML:html;
  };
 }

 // Visible product branding only; legacy package/storage/sync identifiers remain untouched.
 if(typeof cloudPanel==='function'){
  const cloudPanelBase162=cloudPanel;
  cloudPanel=function(){return String(cloudPanelBase162.apply(this,arguments)).replace('RepForge → Google Naptár','TrainPilot → Google Naptár')};
 }

 // Theme picker: inline dropdown matching the language selector interaction.
 const themeOptions162=function(keys){
  const current=rf200ThemeKey();
  return keys.map(function(key){
   const theme=RF200_THEMES[key];
   return '<button type="button" class="tp155-theme-option '+(key===current?'active':'')+'" data-theme="'+esc(key)+'" aria-pressed="'+(key===current?'true':'false')+'" onclick="event.preventDefault();event.stopPropagation();tp162ChooseTheme(\''+esc(key)+'\')"><span class="tp155-theme-swatch" style="background:'+esc(theme.accent)+'"></span><span>'+esc(tp1511ThemeName(key))+'</span><i aria-hidden="true">✓</i></button>';
  }).join('');
 };
 window.tp162ThemeDropdownHtml=function(){
  const lang=typeof rf212Lang==='function'?rf212Lang():'hu';
  const basic=lang==='hu'?'Alap színek':tp1511T('basic'),vivid=lang==='hu'?'Élénk színek':tp1511T('vivid');
  return '<div class="tp155-theme-list tp162-theme-columns"><section class="tp162-theme-column"><div class="small muted tp155-theme-group">'+esc(basic)+'</div><div class="tp162-theme-options">'+themeOptions162(TP1511_BASIC)+'</div></section><section class="tp162-theme-column"><div class="small muted tp155-theme-group">'+esc(vivid)+'</div><div class="tp162-theme-options">'+themeOptions162(TP1511_VIVID)+'</div></section></div>';
 };
 window.tp162ThemeToggle=function(el){
  state.tp162ThemeOpen=!!el?.open;
  if(!el?.open)return;
  requestAnimationFrame(function(){
   const trigger=el.querySelector('summary'),menu=el.querySelector('.tp162-theme-columns');if(!trigger||!menu)return;
   const r=trigger.getBoundingClientRect(),vh=window.visualViewport?.height||window.innerHeight||720;
   const below=Math.max(0,vh-r.bottom-8),above=Math.max(0,r.top-8),openTop=below<220&&above>below,available=openTop?above:below;
   menu.dataset.placement=openTop?'top':'bottom';
   menu.style.setProperty('--tp162-theme-max-height',Math.max(120,Math.min(300,vh*.44,Math.max(120,available-8)))+'px');
  });
 };
 window.tp162ChooseTheme=function(key){if(!RF200_THEMES[key])return;state.tp162ThemeOpen=false;rf200SetTheme(key)};
 rf200ThemePanel=function(){
  const key=rf200ThemeKey(),theme=RF200_THEMES[key];
  return '<div class="setting tp1511-card tp155-theme-setting tp162-theme-setting"><div class="tp1511-head"><b>◐</b><div><strong>'+esc(tp152T('theme'))+'</strong><p class="small muted">'+esc(tp1511T('themeHelp'))+'</p></div></div><details class="tp162-theme-dropdown" '+(state.tp162ThemeOpen?'open':'')+' ontoggle="tp162ThemeToggle(this)"><summary class="btn secondary block tp155-theme-open"><span class="tp155-theme-swatch" style="background:'+esc(theme.accent)+'"></span><span>'+esc(tp1511ThemeName(key))+'</span><i aria-hidden="true">⌄</i></summary>'+window.tp162ThemeDropdownHtml()+'</details></div>';
 };
 window.tp162CloseThemeDropdown=function(){
  const d=document.querySelector('.tp162-theme-dropdown[open]');if(!d)return false;d.open=false;state.tp162ThemeOpen=false;return true;
 };
 document.addEventListener('click',function(ev){const d=document.querySelector('.tp162-theme-dropdown[open]');if(d&&!d.contains(ev.target)){d.open=false;state.tp162ThemeOpen=false}});
 if(typeof window.TrainPilotAndroidBack==='function'){
  const tp162AndroidBackBase=window.TrainPilotAndroidBack;
  window.TrainPilotAndroidBack=function(){if(tp162CloseThemeDropdown())return true;return tp162AndroidBackBase.apply(this,arguments)};
 }

 // Run after the established render stack so removed chrome does not reserve any layout height.
 const renderBase162=render;
 render=function(custom){
  const out=renderBase162.apply(this,arguments);
  cleanupChrome();
  return out;
 };
 cleanupChrome();

 const style=document.createElement('style');style.id='tp162FollowupCss';style.textContent=[
  '#tp155R4PanelHost:is([data-panel="calendar"],[data-panel="coach"],[data-panel="settings"],[data-panel="exercises"]) .tp155-r4-panel-close{position:sticky!important;top:8px!important;right:auto!important;float:right!important;margin:0 4px -36px 8px!important;z-index:20!important}',
  '#tp155R4PanelHost[data-panel="coach"] .tp151-coach-recommendation{margin-top:10px!important}',
  '#tp155R4PanelHost[data-panel="quick"] .tp155-quick-panel>:is(.hero,.tp151-page-head){display:none!important}',
  '#tp155R4PanelHost[data-panel="quick"] .tp160-quick-context{font-size:15px!important;font-weight:850!important;line-height:1.2!important}',
  'body.tp-home-view main.rf221-home{min-height:0!important;height:auto!important;max-height:none!important;padding-bottom:max(12px,env(safe-area-inset-bottom,0px))!important}',
  'main.rf221-home>.onboarding{padding:6px 11px 9px!important;gap:5px!important}',
  'main.rf221-home>.onboarding h2{margin:0!important}',
  'main.rf221-home>.onboarding p{margin:0!important}',
  '.tp162-theme-setting{overflow:visible!important}.tp162-theme-dropdown{position:relative;margin-top:8px;z-index:21}.tp162-theme-dropdown>summary{list-style:none;margin:0!important}.tp162-theme-dropdown>summary::-webkit-details-marker{display:none}.tp162-theme-dropdown[open] .tp155-theme-open{border-color:var(--accent)!important;box-shadow:0 0 0 2px rgba(var(--accent-rgb),.18)!important}.tp162-theme-dropdown[open] .tp155-theme-open i{transform:rotate(180deg)}',
  '.tp162-theme-columns{position:absolute!important;left:0!important;right:0!important;top:calc(100% + 4px)!important;z-index:10000!important;display:grid!important;grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:8px!important;max-height:var(--tp162-theme-max-height,min(300px,44dvh))!important;overflow-y:auto!important;overflow-x:hidden!important;padding:8px!important;background:var(--tp-popover,var(--card))!important;border:1px solid rgba(var(--accent-rgb),.42)!important;border-radius:14px!important;box-shadow:0 18px 46px #000c!important;overscroll-behavior:contain!important}',
  '.tp162-theme-columns[data-placement="top"]{top:auto!important;bottom:calc(100% + 4px)!important}',
  '.tp162-theme-column{min-width:0;border:1px solid var(--line);border-radius:12px;background:var(--card2);padding:7px}',
  '.tp162-theme-column .tp155-theme-group{margin:0 0 6px!important;padding:0 2px!important;font-size:12px!important;font-weight:800!important}',
  '.tp162-theme-options{display:grid;gap:5px}',
  '.tp162-theme-dropdown .tp155-theme-option{box-sizing:border-box;min-width:0!important;min-height:40px!important;padding:6px 7px!important;gap:6px!important;border:1px solid var(--line)!important;border-radius:10px!important;background:var(--card)!important;font-size:12px!important;line-height:1.15!important;white-space:normal!important;text-align:left!important}',
  '.tp162-theme-dropdown .tp155-theme-option>span:nth-child(2){min-width:0;overflow-wrap:anywhere}',
  '.tp162-theme-dropdown .tp155-theme-option.active{border-color:var(--accent)!important;box-shadow:inset 0 0 0 1px var(--accent)!important;background:color-mix(in srgb,var(--accent) 12%,var(--card))!important}',
  '.tp162-theme-dropdown .tp155-theme-option i{margin-left:auto!important}',
  '.tp162-theme-dropdown .tp155-theme-swatch{flex:0 0 14px!important;width:14px!important;height:14px!important}',
  '@media(max-width:340px){.tp162-theme-columns{gap:6px!important}.tp162-theme-column{padding:6px!important}.tp162-theme-dropdown .tp155-theme-option{padding:5px 6px!important;font-size:11.5px!important}}'
 ].join('');
 document.head?.appendChild(style);

 window.TrainPilot162Followup={stickyPanelClose:true,coachSpacing:true,quickWorkoutParent:true,quickTitleSingle:true,healthHeaderRemoved:true,brandStripRemoved:true,trainPilotCalendarBrand:true,inlineTwoColumnThemePicker:true,floatingScrollableThemePicker:true,calendarGeometryUntouched:true};
})();
// @endsection trainpilot-162-followup.js

// @section trainpilot-164-issue12-side-set-fields.js
/* TrainPilot issue #12: keep bilateral timed set rows as two permanent side fields.
 * The legacy compatibility reps value remains in session data for old consumers, but
 * it is no longer rendered as the editable row value for mp/oldal exercises.
 */
(function(){
 'use strict';

 const TP164_ISSUE12='1.6.4-issue12';

 var tp164SideLabel=function(side){
  const key=side==='right'?'stopwatch.right':'stopwatch.left';
  const fallback=side==='right'?'Jobb oldal':'Bal oldal';
  try{return typeof tp149T==='function'?tp149T(key):fallback}catch(_){return fallback}
 };
 var tp164SecondUnit=function(){
  try{return typeof tp149T==='function'?tp149T('unit.second.short'):'mp'}catch(_){return 'mp'}
 };
 var tp164Rows=function(){
  const main=document.querySelector('main.tp153-workout,main.tp149-workout,main');
  if(!main)return [];
  const modern=[...main.querySelectorAll('.tp153-set-row')];
  return modern.length?modern:[...main.querySelectorAll('.row')];
 };

 window.tp164SyncPerSideRow=function tp164SyncPerSideRow(setIndex){
  const ex=state.session?.exercises?.[state.current];if(!ex||!tp1481IsPerSideTimed(ex))return;
  const set=ex.sets?.[setIndex],row=tp164Rows()[setIndex];if(!set||!row)return;
  const sides=tp1481SyncPerSideReps(set);
  for(const side of ['left','right']){
   const input=row.querySelector('.tp164-side-set-input[data-side="'+side+'"]');
   const stored=side==='right'?sides.right:sides.left;
   if(input&&document.activeElement!==input)input.value=stored||'';
   if(input){
    input.setAttribute('aria-label',tp164SideLabel(side));
    input.placeholder='—';
   }
  }
  const check=row.querySelector('button.check');
  if(check){
   const ready=sides.left>0&&sides.right>0;
   check.disabled=!set.done&&!ready;
   check.setAttribute('aria-disabled',check.disabled?'true':'false');
   check.title=!set.done&&!ready?(typeof tp149T==='function'?tp149T('stopwatch.sideBothRequired'):'Mindkét oldal ideje szükséges.'):'';
  }
 };

 window.tp164SetSideSeconds=function tp164SetSideSeconds(setIndex,side,value){
  const ex=state.session?.exercises?.[state.current];if(!ex||!tp1481IsPerSideTimed(ex))return;
  const set=ex.sets?.[setIndex];if(!set)return;
  tp1481SetSideSeconds(setIndex,side,value);
  const sides=tp1481SyncPerSideReps(set);
  if(set.done&&(sides.left<1||sides.right<1)){set.done=false;persistDraft();}
  window.tp164SyncPerSideRow(setIndex);
 };

 window.tp164DecoratePerSideRows=function tp164DecoratePerSideRows(){
  const ex=state.session?.exercises?.[state.current];if(!ex||!tp1481IsPerSideTimed(ex))return false;
  const rows=tp164Rows();if(!rows.length)return false;
  rows.forEach(function(row,setIndex){
   const set=ex.sets?.[setIndex];if(!set)return;
   row.classList.add('tp164-per-side-row');
   let fields=row.querySelector('.tp164-side-set-fields');
   if(!fields){
    const repInput=[...row.querySelectorAll('input.field')].find(function(input){
     return input.getAttribute('inputmode')==='numeric'&&String(input.getAttribute('oninput')||'').includes("'reps'");
    })||row.querySelector('input.field[inputmode="numeric"]');
    const oldLabel=repInput?.closest('label');if(!oldLabel)return;
    fields=document.createElement('div');fields.className='tp164-side-set-fields';
    fields.innerHTML=['left','right'].map(function(side){
     const value=tp1481StoredSide(set,side)||'';
     return '<label class="small tp164-side-set-field"><span class="tp164-side-set-label">'+esc(tp164SideLabel(side))+'</span><span class="tp164-side-set-unit">'+esc(tp164SecondUnit())+'</span><input class="field tp164-side-set-input" data-side="'+side+'" inputmode="numeric" pattern="[0-9]*" min="0" step="1" aria-label="'+esc(tp164SideLabel(side))+'" placeholder="—" value="'+esc(value)+'" oninput="tp164SetSideSeconds('+setIndex+',\''+side+'\',this.value)"></label>';
    }).join('');
    oldLabel.replaceWith(fields);
   }
   window.tp164SyncPerSideRow(setIndex);
  });
  return true;
 };

 // Existing 1.6.1 timer wrappers call this function dynamically, so replacing it
 // prevents the old compatibility value from overwriting the visible right-side input.
 window.tp161SyncPerSideRow=window.tp164SyncPerSideRow;

 if(typeof renderWorkout==='function'){
  const tp164RenderWorkoutBase=renderWorkout;
  renderWorkout=function(){
   const out=tp164RenderWorkoutBase.apply(this,arguments);
   window.tp164DecoratePerSideRows();
   return out;
  };
 }

 const style=document.createElement('style');style.id='tp164Issue12Css';style.textContent=[
  '.tp153-set-row.tp164-per-side-row{grid-template-columns:44px minmax(0,1fr) 52px!important;grid-template-areas:"num meta check" "fields fields fields";gap:7px 8px!important;align-items:center!important;padding-block:8px!important}',
  '.tp164-per-side-row>.num{grid-area:num}.tp164-per-side-row>.tp153-bodyweight{grid-area:meta;min-width:0}.tp164-per-side-row>label:not(.tp164-side-set-field){grid-area:meta;min-width:0}.tp164-per-side-row>.check{grid-area:check;align-self:center}',
  '.tp164-side-set-fields{grid-area:fields;display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;width:100%;min-width:0}',
  '.tp164-side-set-field{min-width:0;display:grid;grid-template-columns:minmax(0,1fr) auto;grid-template-areas:"label unit" "input input";column-gap:6px;row-gap:5px;padding:8px 9px;border:1px solid color-mix(in srgb,var(--accent) 30%,var(--line));border-radius:11px;background:color-mix(in srgb,var(--accent) 5%,var(--card2))}',
  '.tp164-side-set-label{grid-area:label;min-width:0;font-weight:800;white-space:normal;overflow-wrap:anywhere;line-height:1.15}.tp164-side-set-unit{grid-area:unit;color:var(--muted);font-size:11px;align-self:center}.tp164-side-set-field>.field{grid-area:input;width:100%;min-width:0;margin:0!important;text-align:center;font-variant-numeric:tabular-nums}',
  '.tp164-per-side-row>.check:disabled{opacity:.42!important;filter:saturate(.55)!important;cursor:not-allowed!important}',
  '@media(max-width:340px){.tp164-side-set-fields{grid-template-columns:1fr}.tp153-set-row.tp164-per-side-row{grid-template-columns:40px minmax(0,1fr) 48px!important}}'
 ].join('');
 document.head?.appendChild(style);


 // Journal is a safe read-only detour during an active workout: persist the draft,
 // then leave the in-memory session and open Journal immediately.
 const tp164GoBase=go;
 go=function(route){
  const key=String(route||'home'),target=window.TrainPilotRoutes?.[key]||key;
  if(state.session&&target==='history'){
   try{persistDraft()}catch(_){}
   state.session=null;
   const out=tp164GoBase.apply(this,arguments);
   return out;
  }
  return tp164GoBase.apply(this,arguments);
 };
 window.TrainPilotNavigate=go;

window.TrainPilot164Issue12={version:TP164_ISSUE12,separateSetSideFields:true,noCompatibilityOverwrite:true,bothSidesRequired:true,journalDuringWorkout:true};
window.tp164DecoratePerSideRows();
})();
// @endsection trainpilot-164-issue12-side-set-fields.js

// @section trainpilot-165-journal-data-recovery.js
/* TrainPilot 1.6.5: legacy/partial workout rows must never prevent Journal opening. */
(function(){
 'use strict';

 window.tp165SafeHistoryRecord=function tp165SafeHistoryRecord(value,index){
  const source=value&&typeof value==='object'&&!Array.isArray(value)?value:{};
  const exercises=Array.isArray(source.exercises)?source.exercises:[];
  return Object.assign({},source,{
   started:source.started||source.finished||new Date(0).toISOString(),
   exercises:exercises.filter(function(e){return e&&typeof e==='object'}).map(function(e){
    return Object.assign({},e,{
     id:String(e.id||'legacy-exercise-'+index),
     hu:String(e.hu||e.name||e.en||'Korábbi gyakorlat'),
     en:String(e.en||''),
     sets:Array.isArray(e.sets)?e.sets.filter(function(s){return s&&typeof s==='object'}):[]
    });
   })
  });
 };

 const filteredBase=rf263FilteredHistory;
 rf263FilteredHistory=function(){
  try{
   const rows=filteredBase.apply(this,arguments);
   return Array.isArray(rows)?rows.map(function(row){return {x:window.tp165SafeHistoryRecord(row?.x,row?.index),index:Number.isInteger(row?.index)?row.index:0}}):[];
  }catch(_){
   const rows=typeof history==='function'?history():[];
   return (Array.isArray(rows)?rows:[]).map(function(x,index){return {x:window.tp165SafeHistoryRecord(x,index),index:index}});
  }
 };

 window.tp165DeleteBrokenHistory=function tp165DeleteBrokenHistory(index){
  const rows=typeof history==='function'?history():[];
  if(!Array.isArray(rows)||index<0||index>=rows.length)return;
  const remove=function(){const next=rows.slice();next.splice(index,1);db.set('history',next);if(typeof cloudChanged==='function')cloudChanged();render(historyScreen())};
  try{
   const ask=typeof tp2628Confirm==='function'?tp2628Confirm('Törlöd ezt a sérült naplóbejegyzést?',{title:'Naplóbejegyzés törlése',confirmText:'Törlés',danger:true}):Promise.resolve(confirm('Törlöd ezt a sérült naplóbejegyzést?'));
   Promise.resolve(ask).then(function(ok){if(ok)remove()});
  }catch(_){if(confirm('Törlöd ezt a sérült naplóbejegyzést?'))remove()}
 };

 const itemBase=rf263HistoryItem;
 rf263HistoryItem=function(x,originalIndex,visibleIndex){
  const safe=window.tp165SafeHistoryRecord(x,originalIndex);
  try{return itemBase.call(this,safe,originalIndex,visibleIndex)}catch(error){
   try{console.error('[TrainPilot] Journal row recovery',originalIndex,error)}catch(_){}
   const date=typeof fmtDate==='function'?fmtDate(safe.started):'';
   return '<div class="history card tp165-history-recovery" role="status"><strong>Korábbi naplóbejegyzés</strong><p class="small muted">'+esc(date)+'<br>Ez a régi bejegyzés hiányos, ezért egyszerűsített nézetben jelent meg. A többi naplóadat továbbra is elérhető.</p><button type="button" class="btn danger block" onclick="tp165DeleteBrokenHistory('+Number(originalIndex||0)+')">Sérült bejegyzés törlése</button></div>';
  }
 };

 window.TrainPilot165JournalRecovery={version:'1.6.5',legacyRows:true,partialRows:true,rowIsolation:true};
})();
// @endsection trainpilot-165-journal-data-recovery.js

// @section ready.js
window.TrainPilotBoot.finish();
// @endsection ready.js

