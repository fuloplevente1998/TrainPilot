/* #197 step 5/7: two-stage approval, safe persistence and one-step undo.
 * Separate from the canonical app.js section to avoid changing legacy runtime logic.
 * The Coach continues to use preview-only #197 recommendations until step 6/7. */
(function(){
 'use strict';
 if(typeof tp197UiPanel!=='function'||typeof tp197UiChoice!=='function')return;
 const words={
  hu:{save:'Mentés',cancel:'Mégse',approve:'Jóváhagyom a módváltást',undo:'Utolsó módváltás visszavonása',question:'Biztosan mented ezt az edzésmódot? A régi napló és az aktív edzés változatlan marad.',notice:'A választás csak külön jóváhagyás után mentődik. A Coach nem módosít automatikusan súlyt vagy sorozatot.',active:'Aktív edzés közben nem válthatsz módot. Fejezd be az edzést, majd próbáld újra.',stale:'A beállítás időközben megváltozott. Ellenőrizd újra az előnézetet.',error:'A módváltás nem menthető. Az eddigi adatok változatlanok.',saved:'Mód elmentve; a napló és az aktív edzés nem változott.',reverted:'Az előző mód visszaállítva.',noUndo:'Nincs visszavonható mentés.'},
  en:{save:'Save',cancel:'Cancel',approve:'Confirm mode change',undo:'Undo last mode change',question:'Save this training mode? Workout history and active sessions stay unchanged.',notice:'Selection is saved only after explicit confirmation. Coach never adjusts load or sets automatically.',active:'Cannot change modes during an active workout. Finish the workout and try again.',stale:'Settings changed in the meantime. Review the preview again.',error:'Could not save the mode change. Existing data are unchanged.',saved:'Mode saved; history and active workout unchanged.',reverted:'Previous mode restored.',noUndo:'No saved change to undo.'},
  de:{save:'Speichern',cancel:'Abbrechen',approve:'Moduswechsel bestätigen',undo:'Letzten Moduswechsel rückgängig machen',question:'Trainingsmodus speichern? Verlauf und aktives Training bleiben unverändert.',notice:'Die Auswahl wird erst nach ausdrücklicher Bestätigung gespeichert. Coach ändert keine Last oder Sätze automatisch.',active:'Während eines aktiven Trainings ist kein Moduswechsel möglich.',stale:'Die Einstellungen wurden geändert. Vorschau erneut prüfen.',error:'Moduswechsel konnte nicht gespeichert werden. Bestehende Daten bleiben unverändert.',saved:'Modus gespeichert; Trainingsverlauf unverändert.',reverted:'Vorheriger Modus wiederhergestellt.',noUndo:'Keine Änderung zum Rückgängigmachen.'},
  ro:{save:'Salvează',cancel:'Anulează',approve:'Confirmă schimbarea modului',undo:'Anulează ultima schimbare',question:'Salvezi acest mod? Istoricul și antrenamentul activ rămân neschimbate.',notice:'Opțiunea se salvează doar după confirmarea explicită. Coach nu modifică automat greutăți sau serii.',active:'Nu poți schimba modul în timpul unui antrenament activ.',stale:'Setările s-au schimbat între timp. Verifică din nou previzualizarea.',error:'Schimbarea nu a putut fi salvată. Datele existente nu s-au modificat.',saved:'Mod salvat; istoricul nu s-a modificat.',reverted:'Modul anterior a fost restabilit.',noUndo:'Nu există schimbări de anulat.'}
 };
 const approvals=Object.create(null),messages=Object.create(null);
 const textFor=()=>words[typeof rf212Lang==='function'?rf212Lang():'hu']||words.en;
 const valid=x=>!!x&&['normal','hit'].includes(x.trainingStyle)&&['double','linear','rir'].includes(x.progressionMode);
 const clone=x=>x===undefined?null:JSON.parse(JSON.stringify(x));
 const modeState=()=>settings()?.trainingModes197||null;
 const snapshot=()=>JSON.stringify(modeState());
 const isActive=()=>typeof state!=='undefined'&&!!state?.session;
 const scopeOk=(scope,pid)=>scope==='global'||scope==='program'&&typeof pid==='string'&&pid.length>0&&pid.length<256&&typeof programById==='function'&&!!programById(pid)&&typeof activeProgram==='function'&&activeProgram()?.id===pid;
 const k=(scope,pid)=>tp197UiKey(scope,pid);
 const invalidate=(scope,pid)=>{delete approvals[k(scope,pid)];delete messages[k(scope,pid)];};
 const alertFor=(scope,pid,name)=>{messages[k(scope,pid)]=name;tp197UiRefresh(scope,pid);return false;};
 const originalSet=tp197UiSet;
 tp197UiSet=function(scope,pid,field,value){invalidate(scope,pid);return originalSet(scope,pid,field,value);};
 tp197UiCancel=function(scope,pid){
  if(!scopeOk(scope,pid))return false;
  invalidate(scope,pid);delete tp197UiPending[k(scope,pid)];tp197UiRefresh(scope,pid);return true;
 };
 tp197UiRequestSave=function(scope,pid){
  if(!scopeOk(scope,pid))return false;
  if(isActive())return alertFor(scope,pid,'active');
  const choice=tp197UiChoice(scope,pid);
  if(!valid(choice))return alertFor(scope,pid,'error');
  approvals[k(scope,pid)]={snapshot:snapshot(),choice:clone(choice)};
  delete messages[k(scope,pid)];
  tp197UiRefresh(scope,pid);return true;
 };
 tp197UiApprove=function(scope,pid){
  if(!scopeOk(scope,pid))return false;
  if(isActive())return alertFor(scope,pid,'active');
  const key=k(scope,pid),approved=approvals[key],choice=tp197UiChoice(scope,pid);
  if(!approved||snapshot()!==approved.snapshot||!valid(choice)||
     JSON.stringify(choice)!==JSON.stringify(approved.choice)){
   delete approvals[key];return alertFor(scope,pid,'stale');
  }
  try{
   const old=clone(modeState()),previous=old&&typeof old==='object'?clone(old):null;
   if(previous)delete previous.undo; // bounded single-step undo, no recursive snapshots
   const base=old&&typeof old==='object'&&old.schemaVersion===1?old:{};
   const defaults=valid(base.defaults)?clone(base.defaults):{trainingStyle:'normal',progressionMode:'double'};
   const byProgram=Object.assign(Object.create(null),base.byProgram&&typeof base.byProgram==='object'?base.byProgram:{});
   const updatedAt=new Date().toISOString();
   const next={
    ...base,schemaVersion:1,
    defaults:scope==='global'?{trainingStyle:choice.trainingStyle,progressionMode:choice.progressionMode}:defaults,
    explicitChoice:scope==='global'?true:base.explicitChoice===true,
    byProgram,
    revision:Math.max(0,Number.isSafeInteger(base.revision)?base.revision:0)+1,
    updatedAt,
    undo:{scope:key,previous}
   };
   if(scope==='program')byProgram[pid]={trainingStyle:choice.trainingStyle,progressionMode:choice.progressionMode,explicitChoice:true,updatedAt};
   // Single settings write; never touch program recipes, history, session/draft or Wear.
   if(snapshot()!==approved.snapshot||isActive())return alertFor(scope,pid,'stale');
   db.set('settings',{...settings(),trainingModes197:next});
   delete approvals[key];delete tp197UiPending[key];
   messages[key]='saved';tp197UiRefresh(scope,pid);return true;
  }catch(_){return alertFor(scope,pid,'error');}
 };
 tp197UiUndo=function(scope,pid){
  if(!scopeOk(scope,pid))return false;
  if(isActive())return alertFor(scope,pid,'active');
  const current=modeState(),key=k(scope,pid);
  if(!current||current.undo?.scope!==key||!Object.prototype.hasOwnProperty.call(current.undo,'previous'))return alertFor(scope,pid,'noUndo');
  try{
   const next={...settings()},previous=clone(current.undo.previous);
   if(previous===null)delete next.trainingModes197;
   else{delete previous.undo;next.trainingModes197=previous;}
   if(isActive()||snapshot()!==JSON.stringify(current))return alertFor(scope,pid,'stale');
   db.set('settings',next);
   invalidate(scope,pid);delete tp197UiPending[key];messages[key]='reverted';
   tp197UiRefresh(scope,pid);return true;
  }catch(_){return alertFor(scope,pid,'error');}
 };
 const oldPanel=tp197UiPanel;
 tp197UiPanel=function(scope,pid,expanded){
  let html=oldPanel(scope,pid,expanded);
  if(!html)return html;
  const t=textFor(),key=k(scope,pid),arg=scope==='program'?"'program',"+esc(JSON.stringify(String(pid))):"'global',null";
  const button=(label,fn,extra='')=>'<button type="button" class="btn '+extra+'" onclick="'+fn+'('+arg+')">'+esc(label)+'</button>';
  const approved=approvals[key],undo=modeState()?.undo?.scope===key;
  const controls=approved
   ?'<div class="tp197-confirm" role="group" aria-label="'+esc(t.question)+'"><p class="small">'+esc(t.question)+'</p>'+button(t.approve,'tp197UiApprove')+button(t.cancel,'tp197UiCancel','secondary')+'</div>'
   :'<div class="tp197-actions">'+button(t.save,'tp197UiRequestSave')+button(t.cancel,'tp197UiCancel','secondary')+(undo?button(t.undo,'tp197UiUndo','secondary'):'')+'</div>';
  const message=messages[key]?'<p class="small tp197-status" role="status">'+esc(t[messages[key]]||t.error)+'</p>':'';
  html=html.replace(/<p class="small muted tp197-ui-notice">[\s\S]*?<\/p>/,
      '<p class="small muted tp197-ui-notice">'+esc(t.notice)+'</p>');
  return html.replace(/<button type="button" class="btn secondary tp197-ui-reset"[^>]*>[\s\S]*?<\/button>/,
      controls+message);
 };
 const css=document.createElement('style');
 css.id='tp197ConfirmStyle';
 css.textContent='.tp197-actions,.tp197-confirm{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;min-width:0}.tp197-actions button,.tp197-confirm button{min-width:0;min-height:44px;white-space:normal;overflow-wrap:anywhere}.tp197-actions button:last-child:nth-child(3){grid-column:span 2}.tp197-confirm p{grid-column:span 2;margin:2px 0 8px}.tp197-confirm{padding:12px;border:1px solid var(--line);border-radius:12px}.tp197-status{overflow-wrap:anywhere}';
 document.head.appendChild(css);
})();
