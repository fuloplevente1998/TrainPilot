/* PR #279: one compact, read-only Coach disclosure on active workout.
 * Reuses the existing #197 approval buttons and business guards. No session
 * mutation, suggestion auto-application or Wear ownership change. */
(function(){
 'use strict';
 const previous=window.TrainPilotWorkoutCoach;
 if(!previous||typeof previous.html!=='function')return;
 const strings={
  hu:{title:'Progresszió / Coach',today:'Mai javaslat',change:'Változtatás',why:'Mi alapján?',method:'Módszer',history:'Megfelelő előzmény',sets:'Tervezett munkasorozat',suggested:'Javasolt munkasorozat (még nincs alkalmazva)',longTerm:'Hosszabb távú cél (nem a mai utasítás)',pain:'Fájdalomjelzés – nincs terhelésnövelés',light:'Ma kímélő terhelés javasolt',unknown:'Nincs elegendő adat személyre szabott változtatáshoz.',notApplied:'Az ajánlás nem változtatja meg a sorozatokat vagy a súlyokat jóváhagyás nélkül.'},
  en:{title:'Progression / Coach',today:'Today’s advice',change:'Suggested change',why:'Why?',method:'Method',history:'Relevant history',sets:'Planned working sets',suggested:'Suggested working sets (not applied)',longTerm:'Long-term goal (not today’s instruction)',pain:'Pain reported – do not increase load',light:'Keep today’s training light',unknown:'Not enough data for a personalized change.',notApplied:'Suggestions never change your sets or loads without approval.'},
  de:{title:'Progression / Coach',today:'Heutiger Hinweis',change:'Vorgeschlagene Änderung',why:'Warum?',method:'Methode',history:'Passende Einheiten',sets:'Geplante Arbeitssätze',suggested:'Empfohlene Arbeitssätze (nicht übernommen)',longTerm:'Langfristiges Ziel (kein heutiger Auftrag)',pain:'Schmerzen gemeldet – Belastung nicht erhöhen',light:'Heute schonend trainieren',unknown:'Zu wenig Daten für eine persönliche Änderung.',notApplied:'Vorschläge ändern Sätze und Gewichte nicht ohne Zustimmung.'},
  ro:{title:'Progresie / Coach',today:'Recomandarea de azi',change:'Modificare propusă',why:'Pe ce se bazează?',method:'Metodă',history:'Istoric relevant',sets:'Serii de lucru planificate',suggested:'Serii propuse (neaplicate)',longTerm:'Obiectiv pe termen lung (nu instrucțiunea de azi)',pain:'Durere raportată – fără creșterea sarcinii',light:'Antrenament ușor astăzi',unknown:'Date insuficiente pentru o schimbare personalizată.',notApplied:'Recomandările nu modifică seriile sau greutățile fără aprobare.'}
 };
 const language=()=>typeof rf212Lang==='function'?rf212Lang():'hu';
 const words=()=>strings[language()]||strings.en;
 const safe=value=>String(value==null?'':value).replace(/[&<>"']/g,ch=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
 const line=(label,value)=>'<p class="tp279-coach-line"><span>'+safe(label)+'</span><strong>'+safe(value)+'</strong></p>';
 const item=(label,value)=>'<p class="tp279-coach-detail"><b>'+safe(label)+':</b> '+safe(value)+'</p>';
 function approvalControls(){
  // Take only the original, guard-protected buttons/status. Do not recreate
  // #197 request/fingerprint/approval rules or bypass their session checks.
  if(typeof tp197CoachPanel!=='function')return '';
  const wrap=document.createElement('div');
  wrap.innerHTML=tp197CoachPanel();
  const panel=wrap.querySelector('#tp197CoachAdvice');
  if(!panel)return '';
  const buttons=panel.querySelector('.tp197-coach-buttons');
  if(buttons)return buttons.outerHTML;
  const button=panel.querySelector(':scope > button');
  if(button)return button.outerHTML;
  const done=typeof tp197CoachT==='function'?tp197CoachT().done:'';
  if(done&&panel.textContent.includes(done))return '<p class="small muted">'+safe(done)+'</p>';
  return '';
 }
 function currentPlan(exercise,advice){
  const session=state.session,t=words();
  let readiness=null,decision=null,recommendation=null;
  try{readiness=rf220Readiness();decision=rf233Decision(readiness)}catch(_){}
  try{recommendation=rf152Recommendation(exercise.id,exercise)}catch(_){}
  const pain=exercise.effort==='pain'||advice?.action==='pain'||recommendation?.action==='pain'||decision?.pain===true;
  const light=!pain&&(advice?.action==='easy'||decision?.mode==='light'||decision?.mode==='reduced');
  const quick=!!(session?.quickWorkout||session?.type==='quick'||!session?.programId);
  let info=null;
  try{if(!quick&&!pain&&!light&&typeof tp197CoachInfo==='function')info=tp197CoachInfo(exercise.id,session.programId)}catch(_){}
  const headline=pain?t.pain:light?t.light:info?.title||advice?.label||recommendation?.text||t.unknown;
  const explanation=pain?(recommendation?.text||decision?.text||advice?.text||t.pain)
   :light?(decision?.text||advice?.text||t.light)
   :info?.reason||advice?.text||recommendation?.text||t.unknown;
  return {session,info,recommendation,quick,pain,light,headline,explanation};
 }
 function html(exercise,advice){
  if(!exercise||!state.session)return previous.html(exercise,advice);
  const t=words(),p=currentPlan(exercise,advice),info=p.info;
  let inside='<div class="tp279-coach-today"><span class="tp279-coach-caption">'+safe(t.today)+'</span><p>'+safe(p.explanation)+'</p></div>';
  if(info){
   const planSets=Array.isArray(exercise.sets)?exercise.sets.length:0;
   const proposed=Number(info.preview?.proposed?.sets);
   const hasProposal=Number.isInteger(proposed)&&proposed>0&&proposed!==planSets;
   const delta=info.detail||'';
   inside+='<div class="tp279-coach-change"><strong>'+safe(t.change)+'</strong>'+
    line(t.sets,planSets)+
    (hasProposal?line(t.suggested,proposed):'')+
    (delta?'<p class="small muted">'+safe(delta)+'</p>':'')+
    '<p class="small muted">'+safe(t.notApplied)+'</p>'+approvalControls()+'</div>';
   const method=info.method||'—',count=Number(info.historyCount)||0;
   inside+='<details class="tp279-coach-basis"><summary>'+safe(t.why)+'</summary>'+
    item(t.method,method)+item(t.history,count)+
    (p.recommendation?.text?item(t.longTerm,p.recommendation.text):'')+'</details>';
  }else{
   // Quick workouts have no invented program-day target. A safety warning
   // remains the only actionable advice in low readiness/pain cases.
   const reason=p.pain||p.light?'':p.recommendation?.text;
   if(reason&&reason!==p.explanation)inside+='<details class="tp279-coach-basis"><summary>'+safe(t.why)+'</summary><p class="small muted">'+safe(reason)+'</p></details>';
  }
  return '<summary><span class="tp155-workout-coach-kicker">'+safe(t.title)+'</span>'+
   '<strong>'+safe(p.headline)+'</strong><span aria-hidden="true">›</span></summary>'+
   '<div class="tp279-coach-unified">'+inside+'</div>';
 }
 window.TrainPilotWorkoutCoach={...previous,html,plan:currentPlan};
 const css=document.createElement('style');
 css.id='tp279UnifiedCoachStyle';
 css.textContent='.tp279-coach-unified{display:grid;gap:9px;padding:8px 0 3px;min-width:0}.tp279-coach-unified p{margin:3px 0;line-height:1.35;overflow-wrap:anywhere}.tp279-coach-caption{font-size:11px;font-weight:750;color:var(--accent)}.tp279-coach-today{padding:0 1px}.tp279-coach-change{display:grid;gap:5px;padding-top:8px;border-top:1px solid var(--line)}.tp279-coach-change>strong{font-size:13px}.tp279-coach-line{display:flex;gap:6px;justify-content:space-between;align-items:baseline;font-size:12px}.tp279-coach-line span{color:var(--muted)}.tp279-coach-line strong{text-align:right;font-size:12px}.tp279-coach-detail{font-size:12px;color:var(--muted)}.tp279-coach-basis>summary{min-height:44px}.tp279-coach-unified .tp197-coach-buttons{margin-top:4px}.tp279-coach-unified button{min-height:44px;white-space:normal;overflow-wrap:anywhere}@media(max-width:340px){.tp279-coach-line{flex-wrap:wrap}.tp279-coach-line strong{text-align:left}.tp279-coach-unified .tp197-coach-buttons{grid-template-columns:1fr}}';
 document.head.appendChild(css);
})();