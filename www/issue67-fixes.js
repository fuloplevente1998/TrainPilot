/* Issue #67: non-invasive 1.0.0 navigation bridge.
 * Kept separate from the canonical source until the 1.0.0 test branch is approved.
 */
(function(){
 'use strict';
 const labels={
  hu:{open:'Fejlődés megnyitása',all:'Teljes rekordlista',weight:'Súly',reps:'Ismétlés',time:'Idő',left:'Bal oldal',right:'Jobb oldal',previous:'Korábbi',current:'Új'},
  en:{open:'Open Progress',all:'All-time records',weight:'Weight',reps:'Reps',time:'Time',left:'Left side',right:'Right side',previous:'Previous',current:'New'},
  de:{open:'Fortschritt öffnen',all:'Alle Rekorde',weight:'Gewicht',reps:'Wiederholungen',time:'Zeit',left:'Links',right:'Rechts',previous:'Vorher',current:'Neu'},
  ro:{open:'Deschide Progresul',all:'Toate recordurile',weight:'Greutate',reps:'Repetări',time:'Timp',left:'Stânga',right:'Dreapta',previous:'Anterior',current:'Nou'},
  sk:{open:'Otvoriť pokrok',all:'Všetky rekordy'},pl:{open:'Otwórz postępy',all:'Wszystkie rekordy'}
 };
 const lang=()=>typeof rf212Lang==='function'?rf212Lang():'hu';
 const copy=()=>Object.assign({},labels.hu,labels[lang()]||{});
 const locale=()=>({hu:'hu-HU',en:'en-US',de:'de-DE',ro:'ro-RO',sk:'sk-SK',pl:'pl-PL'}[lang()]||'hu-HU');
 const stripStats=html=>String(html).replace(/(<nav\b[^>]*class="[^"]*\btp155-journal-tabs\b[^"]*"[^>]*>)([\s\S]*?)(<\/nav>)/gi,
  (all,head,inside,tail)=>head+inside.replace(/<button\b[^>]*onclick="tp155OpenJournalStats\(\)"[\s\S]*?<\/button>/gi,'')+tail);
 function removeLegacyTab(root){
  root?.querySelectorAll?.('.tp155-journal-tabs button').forEach(button=>{
   if((button.getAttribute('onclick')||'').includes('tp155OpenJournalStats()'))button.remove();
  });
 }
 function updateAllTimePr(){
  const panel=document.querySelector('#tp177MetricDetails'),trigger=document.querySelector('.tp177-metric[data-metric="pr"][aria-expanded="true"]');
  if(!panel||panel.hidden||!trigger||!window.TrainPilot177Model||typeof history!=='function')return;
  const model=window.TrainPilot177Model;
  const all=model.prepare(history(),{isDeleted:x=>typeof window.tp3HistoryIsDeleted==='function'&&window.tp3HistoryIsDeleted(x)})
   .flatMap(workout=>workout.pr||[]).sort((a,b)=>b.date-a.date);
  const t=copy(),l=locale(),header=panel.querySelector('h2')?.textContent||'PR';
  panel.replaceChildren();
  const heading=document.createElement('h2');heading.textContent=header;panel.appendChild(heading);
  const note=document.createElement('p');note.className='tp177-small';note.textContent=t.all+' · '+all.length;panel.appendChild(note);
  if(!all.length){const none=document.createElement('p');none.className='tp177-small';none.textContent=lang()==='hu'?'Még nincs személyes rekord.':'No personal records yet.';panel.appendChild(none);return}
  const list=document.createElement('div');list.className='tp177-inline-list';
  const fmt=value=>Number(value||0).toLocaleString(l,{maximumFractionDigits:1});
  for(const rec of all){
   const row=document.createElement('div');row.className='tp177-inline-record';
   const name=document.createElement('strong');name.textContent=typeof rf220ExerciseName==='function'?rf220ExerciseName(rec.id):rec.id;
   const date=document.createElement('span');date.textContent=(t[rec.kind]||rec.kind)+' · '+new Intl.DateTimeFormat(l,{year:'numeric',month:'short',day:'numeric'}).format(rec.date);
   const value=document.createElement('span');value.textContent=t.previous+': '+fmt(rec.previous)+' '+rec.unit+' → '+t.current+': '+fmt(rec.value)+' '+rec.unit;
   row.append(name,date,value);list.appendChild(row);
  }
  panel.appendChild(list);
 }
 function decorateCoach(root){
  if(!root||root.querySelector('.tp67-coach-progress'))return;
  const last=root.lastElementChild;
  if(!last?.matches?.('.card')||!last.querySelector('h2')||last.matches('.tp151-coach-recommendation,.tp151-coach-context'))return;
  const button=document.createElement('button');button.type='button';button.className='btn secondary block tp67-coach-progress';button.textContent=copy().open;
  button.addEventListener('click',()=>{
   window.tp155R4ClosePanel?.(false);
   window.tp177OpenProgress?.();
  });
  last.replaceWith(button);
 }
 window.addEventListener('DOMContentLoaded',()=>{
  if(typeof historyScreen!=='function'||typeof render!=='function'||!window.TrainPilot177Progress)return;
  const oldHistory=historyScreen;
  historyScreen=function(){return stripStats(oldHistory.apply(this,arguments))};
  const oldRender=render;
  render=function(){
   const result=oldRender.apply(this,arguments),main=document.querySelector('#app main');
   removeLegacyTab(main);
   if(main?.matches('main.tp151-coach'))decorateCoach(main);
   if(main?.matches('main.tp177-progress'))updateAllTimePr();
   return result;
  };
  window.render=render;
  const oldGo=go;
  go=function(route){
   if(route==='stats'||route==='progress')return window.tp177OpenProgress();
   return oldGo.apply(this,arguments);
  };
  window.TrainPilotNavigate=go;
  const oldToggle=window.tp177ToggleMetric;
  window.tp177ToggleMetric=function(key){
   const result=oldToggle.apply(this,arguments);
   if(key==='pr')updateAllTimePr();
   return result;
  };
  if(typeof window.tp155R4RefreshPanel==='function'){
   const oldRefresh=window.tp155R4RefreshPanel;
   window.tp155R4RefreshPanel=function(){
    const result=oldRefresh.apply(this,arguments);
    decorateCoach(document.querySelector('#tp155R4PanelHost[data-panel="coach"] main.tp151-coach'));
    return result;
   };
  }
  window.TrainPilot67={version:'issue67-rc2',twoJournalTabs:true,allTimePr:true,coachDeepLink:true,themedCheckboxes:true};
 });
})();
