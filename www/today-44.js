/* Phase 5 #44 — shared, local-day-bound Today summary on Home and Health. */
(function(){
 'use strict';
 const labels={
  hu:{today:'Mai állapot',sleep:'Alvás',hrv:'HRV',pulse:'Pulzus',steps:'Lépések',workout:'Mai edzés',recovery:'Regeneráció'},
  en:{today:'Today status',sleep:'Sleep',hrv:'HRV',pulse:'Heart rate',steps:'Steps',workout:'Today’s workouts',recovery:'Recovery'},
  de:{today:'Heutiger Status',sleep:'Schlaf',hrv:'HRV',pulse:'Puls',steps:'Schritte',workout:'Heutiges Training',recovery:'Regeneration'},
  ro:{today:'Starea de azi',sleep:'Somn',hrv:'HRV',pulse:'Puls',steps:'Pași',workout:'Antrenamente azi',recovery:'Recuperare'}
 };
 const paths={
  sleep:'M20 15.5A8.5 8.5 0 0 1 8.5 4 8.5 8.5 0 1 0 20 15.5Z',
  hrv:'M2 12h4l2-5 4 11 3-8 2 4h5',
  pulse:'M20 5a5 5 0 0 0-7 0l-1 1-1-1a5 5 0 0 0-7 7l8 8 8-8a5 5 0 0 0 0-7Z',
  steps:'M8 3c2 0 3 2 2 4l-1 4c-1 3-6 2-5-1l1-4c0-2 1-3 3-3Zm8 8c2 0 3 2 2 4l-1 4c-1 3-6 2-5-1l1-4c0-2 1-3 3-3Z',
  workout:'M3 9v6m3-8v10m3-6h6m3-4v10m3-8v6',
  recovery:'M20 4C12 4 6 7 5 12c-1 6 6 10 11 4 3-3 4-8 4-12ZM4 21c2-5 6-8 11-11'
 };
 const esc=v=>String(v==null?'':v).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const num=v=>v==null||v===''||!Number.isFinite(Number(v))?null:Number(v);
 const positive=v=>{const x=num(v);return x!=null&&x>0?x:null};
 const dayKey=()=>typeof rf240DayKey==='function'?rf240DayKey(new Date()):new Date().toLocaleDateString('en-CA');
 const lang=()=>{try{return typeof rf212Lang==='function'?rf212Lang():'hu'}catch(_){return 'hu'}};
 const locale=()=>({hu:'hu-HU',en:'en-US',de:'de-DE',ro:'ro-RO'}[lang()]||'hu-HU');
 const fmt=v=>v.toLocaleString(locale(),{maximumFractionDigits:1});
 const currentRecovery=key=>{
  const r=state?.health?.recovery;
  if(r&&(r.day===key||(!r.day&&r.sleepEnd&&typeof rf240DayKey==='function'&&rf240DayKey(r.sleepEnd)===key)))return r;
  try{return typeof rf229RecoveryHistory==='function'?rf229RecoveryHistory().find(x=>x?.day===key)||null:null}catch(_){return null}
 };
 window.tp5UnifiedTodayModel=function(){
  const key=dayKey(),day=typeof rf240Ledger==='function'?(rf240Ledger()?.days?.[key]||{}):{},r=currentRecovery(key);
  let count=0;
  try{
   const rows=typeof history==='function'?history():[];
   count=rows.filter(h=>(typeof rf240ValidWorkout==='function'?rf240ValidWorkout(h):Number.isFinite(Date.parse(h?.finished||'')))&&rf240DayKey(h.finished)===key).length;
  }catch(_){}
  const sleep=positive(r?.sleepMinutes),hrv=positive(r?.hrvRmssdMs),pulse=positive(day.averageHeartRate),steps=num(day.steps);
  let ready=null;
  try{
   const active=state?.health?.recovery;
   if(r&&active&&(active===r||active.day===key||(!active.day&&active.sleepEnd&&rf240DayKey(active.sleepEnd)===key))){
    const value=typeof rf220Readiness==='function'?rf220Readiness():null;
    if(value?.parts>0&&num(value.score)!=null)ready=value.score;
   }
  }catch(_){}
  return {day:key,values:{
   sleep:sleep==null?'—':fmt(Math.round(sleep/6)/10)+' h',
   hrv:hrv==null?'—':fmt(Math.round(hrv))+' ms',
   pulse:pulse==null?'—':fmt(Math.round(pulse))+' bpm',
   steps:steps==null||steps<0?'—':fmt(Math.round(steps)),
   workout:String(count),
   recovery:ready==null?'—':fmt(Math.round(ready))+'/100'
  }};
 };
 window.tp5TodayAction=function(action){
  if(action==='history')return typeof go==='function'?go('history'):undefined;
  if(action==='pulse')return window.tp168OpenPulse?.();
  if(action==='recovery')return window.tp153OpenRecovery?.();
  if(action==='health'&&state?.tab!=='health')return typeof go==='function'?go('health'):undefined;
 };
 window.tp5UnifiedTodayGridHtml=function(variant='health'){
  const model=window.tp5UnifiedTodayModel(),t=labels[lang()]||labels.hu,compact=variant==='home';
  const kinds=['sleep','hrv','pulse','steps','workout','recovery'];
  const actions=compact?['health','health','health','health','history','health']:['recovery','recovery','pulse',null,'history','recovery'];
  return '<div class="tp5-today-grid tp5-today-'+(compact?'compact':'full')+'" data-tp5-day="'+model.day+'">'+kinds.map((kind,i)=>{
   const action=actions[i],tag=action?'button':'div',attrs=action?' type="button" onclick="tp5TodayAction(\''+action+'\')"':'';
   return '<'+tag+attrs+' class="stat tp168-metric tp5-today-metric tp5-today-'+kind+'">'+
    '<span class="tp5-today-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="'+paths[kind]+'"/></svg></span>'+
    '<span class="tp5-today-label">'+esc(t[kind])+'</span><strong class="tp5-today-value">'+esc(model.values[kind])+'</strong>'+
    '</'+tag+'>';
  }).join('')+'</div>';
 };
 window.rf223StatusHtml=function(){
  const t=labels[lang()]||labels.hu;
  return '<div class="card rf223-today tp5-home-today" id="rf223Today"><div class="rf223-title"><strong>'+esc(t.today)+'</strong></div>'+window.tp5UnifiedTodayGridHtml('home')+'</div>';
 };
 window.tp5ReplaceHomeToday=function(){
  if(state?.tab!=='home')return;
  const card=document.querySelector('#rf223Today');
  if(card&&!card.classList.contains('tp5-home-today'))card.outerHTML=window.rf223StatusHtml();
 };
 const baseRender=window.render;
 if(typeof baseRender==='function')window.render=function(){
  const result=baseRender.apply(this,arguments);
  window.tp5ReplaceHomeToday();
  return result;
 };
 window.tp5ReplaceHomeToday();
})();
