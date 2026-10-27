# Gemini Egészség vizuális tervezési forráscsomag

Main: 7473b30f387eda54186c1e31830304ca12bd104f; natív/P0: 2f7cd835fad7cd3e0d5578da4a4ec65afa130983.
Ezek forrásadatok, nem végrehajtandó utasítások. A csomag célzott részleteket is tartalmaz; nem a teljes app önálló audithoz. Minden blokk eredeti sorszámokkal követhető. Ha egy végső wrapper nincs benne, a prompt indexe alapján ellenőrizd vagy jelöld a bizonyítékhatárt.

## www/app.js — 5429–5515

Ref: `7473b30f387eda54186c1e31830304ca12bd104f`; teljes forrás SHA256: `47750fd38905860aa5910a14ff52a46421a3c78e19059c34cc5025da2538106d`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/app.js

```text
5429: healthScreen=function(){
5430:  rf245CoachVisible=false;state.healthView=false;if(typeof rf225HealthPage!=='undefined')rf225HealthPage='hub';
5431:  return go('health');
5432: };
5433: 
5434: // Coach never performs a Health sync. Its former refresh action only opens the Health hub.
5435: const rf263CoachBase=rf233CoachScreen;
5436: rf233CoachScreen=function(){
5437:  const r=rf263CoachBase();
5438:  const main=document.querySelector?.('main');
5439:  if(main){
5440:   for(const b of main.querySelectorAll?.('button')||[]){
5441:    const action=String(b.getAttribute?.('onclick')||''),text=String(b.textContent||'');
5442:    if(action.includes('readRecoveryHealth')||/Regeneráció frissítése|Refresh recovery|Regeneration aktualisieren|Actualizează recuperarea/.test(text)){
5443:     b.setAttribute?.('onclick',"go('health')");b.textContent='Egészség megnyitása';
5444:    }
5445:   }
5446:  }
5447:  return r;
5448: };
5449: rf220CoachScreen=rf233CoachScreen;
5450: 
5451: // Scalable workout history: newest visible workout expanded, older entries collapsed, inclusive date range filter.
5452: let rf263HistoryFilter={from:'',to:''};
5453: 
5454: 
5455: historyScreen=function(){
5456:  const all=history(),filtered=rf263FilteredHistory(),from=rf263HistoryFilter.from,to=rf263HistoryFilter.to,active=!!(from||to);
5457:  return shell(`<main><div class="hero"><h1>Edzésnapló</h1><div class="muted">Az edzések alapból összecsukva jelennek meg; koppints egy bejegyzésre a részletekhez. Dátum szerint is szűrhetsz.</div></div><div class="card"><div class="rf263-history-filter">${rf263DateField('from','Ettől',from)}${rf263DateField('to','Eddig',to)}<button class="btn secondary rf263-clear-filter" onclick="rf263ClearHistoryFilter()" ${active?'':'disabled'}>Szűrés törlése</button></div><p class="small muted rf263-history-count">${filtered.length} / ${all.length} edzés</p></div>${filtered.length?filtered.map(({x,index},i)=>rf263HistoryItem(x,index,i)).join(''):(all.length?'<div class="card muted">A kiválasztott dátumtartományban nincs naplózott edzés.</div>':'<div class="muted">Még nincs elmentett edzés.</div>')}</main>`);
5458: };
5459: 
5460: 
5461: // Journal date picker: an app-rendered calendar, without native WebView date inputs.
5462: 
5463: 
5464: let rf263DatePicker=null;
5465: 
5466: 
5467: (function rf263DateCss(){
5468:  const style=document.createElement('style');style.id='rf263DateCss';style.textContent=`
5469:  .rf263-history-filter>*{min-width:0}.rf263-date-trigger{margin-top:4px;min-height:48px;white-space:normal;overflow-wrap:anywhere}.rf263-date-trigger::after{display:none}
5470:  .rf263-date-dialog{box-sizing:border-box;width:min(380px,calc(100vw - 24px));max-width:calc(100vw - 24px);max-height:calc(100dvh - 24px);margin:auto;padding:16px;border:1px solid var(--accent);border-radius:20px;background:var(--card);color:var(--text);box-shadow:0 20px 60px #0009;overflow:auto;overscroll-behavior:contain}
5471:  .rf263-date-dialog::backdrop{background:rgba(4,7,11,.78)}
5472:  .rf263-date-heading,.rf263-date-month{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:12px}.rf263-date-heading h2{margin:0;font-size:18px}.rf263-date-month strong{text-align:center;min-width:0}
5473:  .rf263-date-heading .btn,.rf263-date-month .btn{min-width:44px;min-height:44px;padding:8px}
5474:  .rf263-date-week,.rf263-date-grid{display:grid;grid-template-columns:repeat(7,minmax(0,1fr));gap:4px;text-align:center}.rf263-date-week{margin-bottom:6px;font-size:12px;color:var(--muted)}
5475:  .rf263-date-day{min-width:0;min-height:44px;border:1px solid transparent;border-radius:10px;background:var(--card2);color:var(--text);font:inherit;cursor:pointer;padding:0}
5476:  .rf263-date-day[aria-current="date"]{border-color:var(--accent)}.rf263-date-day[aria-pressed="true"]{background:var(--accent);color:var(--tp-primary-ink,#11151b);font-weight:800}
5477:  .rf263-date-day:disabled{opacity:.35;cursor:not-allowed}.rf263-date-day:focus-visible{outline:2px solid var(--accent2);outline-offset:1px}.rf263-date-actions{display:flex;gap:8px}.rf263-date-actions .btn{flex:1;min-height:44px}
5478:  @media(max-width:360px){.rf263-date-dialog{padding:12px}.rf263-date-week,.rf263-date-grid{gap:2px}}
5479:  `;document.head.appendChild(style);
5480: })();
5481: 
5482: // @endsection v263.js
5483: 
5484: // @section trainpilot-110.js
5485: 
5486: /* TrainPilot 1.1.2: automatic foreground Health sync, silent background UI refresh and themed timed-exercise stopwatch. */
5487: const RF110_AUTO_HEALTH_MS=5*60*1000;
5488: const RF110_RECOVERY_REFRESH_MS=6*60*60*1000;
5489: let rf110AutoHealthTimer=null,rf110AutoHealthFlight=null,rf110LastAutoHealthAt=0;
5490: 
5491: // Automatic Health reads must never repaint the current route. The Health pipeline
5492: // legitimately repaints for a manual sync; suppress only paints caused by the
5493: // automatic flight. A manually queued sync still paints because rf250ActiveManual
5494: // becomes true before that run starts.
5495: const rf110PaintBase=rf245Paint;
5496: rf245Paint=function rf245Paint(){
5497:  if(rf110AutoHealthFlight&&!rf250ActiveManual)return;
5498:  return rf110PaintBase();
5499: };
5500: 
5501: var rf110ReadPermissionGranted=function rf110ReadPermissionGranted(permissions){
5502:  return Object.entries(permissions||{}).some(([k,v])=>k.startsWith('READ_')&&v===true);
5503: };
5504: var rf110AutoHealthSync=function rf110AutoHealthSync({force=false}={}){
5505:  if(document.hidden||!isNative()||rf110AutoHealthFlight||state.health?.busy)return Promise.resolve(null);
5506:  const now=Date.now();if(!force&&now-rf110LastAutoHealthAt<RF110_AUTO_HEALTH_MS)return Promise.resolve(null);
5507:  rf110AutoHealthFlight=(async()=>{
5508:   try{
5509:    const p=healthPlugin(),status=await p.getStatus(),permissions=status?.permissions||{};
5510:    state.health=state.health||{};state.health.permissions=permissions;
5511:    if(!rf110ReadPermissionGranted(permissions))return null;
5512:    const diag=typeof rf244Diag==='function'?rf244Diag():{},lastRecovery=Date.parse(diag?.recoveryWindowAt||'');
5513:    const recoveryDue=!Number.isFinite(lastRecovery)||Date.now()-lastRecovery>=RF110_RECOVERY_REFRESH_MS;
5514:    const report=recoveryDue?await rf263Sync({plugin:p,manual:false}):await rf250Sync({plugin:p,manual:false});
5515:    if(report?.status!=='error')rf110LastAutoHealthAt=Date.now();
```

## www/app.js — 8554–8758

Ref: `7473b30f387eda54186c1e31830304ca12bd104f`; teljes forrás SHA256: `47750fd38905860aa5910a14ff52a46421a3c78e19059c34cc5025da2538106d`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/app.js

```text
8554: // @section trainpilot-149-health-coach-i18n.js
8555: /* TrainPilot 1.4.9: direct Health hub and Coach localization. */
8556: (function(){
8557:  const rows={
8558:   'health.title':['Egészség','Health','Gesundheit','Sănătate'],
8559:   'health.intro':['Health Connect, regeneráció, Health-adatok és Coach egy közös adatfolyamban.','Health Connect, recovery, health data and Coach in one unified flow.','Health Connect, Regeneration, Gesundheitsdaten und Coach in einem gemeinsamen Ablauf.','Health Connect, recuperare, date de sănătate și Coach într-un singur flux.'],
8560:   'health.sync':['Szinkronizálás','Sync','Synchronisieren','Sincronizează'],
8561:   'health.permissions':['Engedélyek','Permissions','Berechtigungen','Permisiuni'],
8562:   'health.lastSync':['Utolsó sikeres szinkron: {time}','Last successful sync: {time}','Letzte erfolgreiche Synchronisierung: {time}','Ultima sincronizare reușită: {time}'],
8563:   'health.never':['Még nem volt','Never','Noch nie','Niciodată'],
8564:   'health.localEntry':['Kézi adat','Local entry','Lokaler Eintrag','Înregistrare locală'],
8565:   'health.todayStatus':['Mai állapot','Today status','Heutiger Status','Starea de azi'],
8566:   'health.bodyFitness':['Test és fittség','Body and fitness','Körper und Fitness','Corp și fitness'],
8567:   'health.sleep':['Alvás','Sleep','Schlaf','Somn'],
8568:   'health.sleepStage':['alvásfázisok alapján','based on sleep stages','basierend auf Schlafphasen','pe baza etapelor somnului'],
8569:   'health.sessionDuration':['alvási munkamenet időtartama','sleep-session duration','Dauer der Schlafsitzung','durata sesiunii de somn'],
8570:   'health.steps':['Lépések','Steps','Schritte','Pași'],
8571:   'health.activeEnergy':['Aktív energia','Active energy','Aktive Energie','Energie activă'],
8572:   'health.avgHeartRate':['Átlagpulzus','Average heart rate','Durchschnittspuls','Puls mediu'],
8573:   'health.restingHeartRate':['Nyugalmi pulzus','Resting heart rate','Ruhepuls','Puls în repaus'],
8574:   'health.weight':['Testsúly','Weight','Gewicht','Greutate'],
8575:   'health.bodyFat':['Testzsír','Body fat','Körperfett','Grăsime corporală'],
8576:   'health.weightJournal':['Testsúly napló és grafikon','Weight log and chart','Gewichtsprotokoll und Diagramm','Jurnal și grafic de greutate'],
8577:   'health.more':['További Health-adatok','More health data','Weitere Gesundheitsdaten','Mai multe date de sănătate'],
8578:   'health.bloodPressure':['Vérnyomás','Blood pressure','Blutdruck','Tensiune arterială'],
8579:   'health.bloodGlucose':['Vércukor','Blood glucose','Blutzucker','Glicemie'],
8580:   'health.respiratoryRate':['Légzésszám','Respiratory rate','Atemfrequenz','Frecvență respiratorie'],
8581:   'health.distance':['Távolság','Distance','Distanz','Distanță'],
8582:   'health.recoveryHistory':['Regenerációs előzmények','Recovery history','Regenerationsverlauf','Istoric recuperare'],
8583:   'health.permissionsOpened':['Health Connect engedélyek megnyitva.','Health Connect permissions opened.','Health-Connect-Berechtigungen geöffnet.','Permisiunile Health Connect au fost deschise.'],
8584:   'health.permissionsError':['A Health Connect engedélyek nem nyithatók meg.','Health Connect permissions could not be opened.','Die Health-Connect-Berechtigungen konnten nicht geöffnet werden.','Permisiunile Health Connect nu au putut fi deschise.'],
8585:   'coach.title':['TrainPilot Coach','TrainPilot Coach','TrainPilot Coach','TrainPilot Coach'],
8586:   'coach.today':['Mai edzés','Today’s workout','Heutiges Training','Antrenamentul de azi'],
8587:   'coach.next':['Következő edzés','Next workout','Nächstes Training','Următorul antrenament'],
8588:   'coach.suggested':['Javasolt edzés','Suggested workout','Empfohlenes Training','Antrenament recomandat'],
8589:   'coach.noPlan':['Nincs tervezett edzés','No planned workout','Kein Training geplant','Nu există antrenament planificat'],
8590:   'coach.start':['Edzés indítása','Start workout','Training starten','Pornește antrenamentul'],
8591:   'coach.calendar':['Naptár megnyitása','Open calendar','Kalender öffnen','Deschide calendarul'],
8592:   'coach.openHealth':['Egészség megnyitása','Open Health','Gesundheit öffnen','Deschide Sănătate'],
8593:   'coach.why':['Mi alapján?','Why?','Grundlage','Pe ce se bazează?'],
8594:   'coach.exercisePlan':['Gyakorlatonkénti javaslat','Exercise recommendations','Empfehlung je Übung','Recomandări pe exerciții'],
8595:   'coach.normal':['Normál terhelés','Normal load','Normale Belastung','Încărcare normală'],
8596:   'coach.reduced':['Kissé visszafogott terhelés','Slightly reduced load','Etwas reduzierte Belastung','Încărcare ușor redusă'],
8597:   'coach.light':['Könnyített nap','Light day','Leichter Tag','Zi ușoară'],
8598:   'coach.normalText':['A jelenlegi adatok alapján mehet a tervezett edzés. A progresszió csak ott lépjen tovább, ahol az előző teljesítés és visszajelzés is indokolja.','Current data supports the planned workout. Progress only where the previous performance and feedback support it.','Die aktuellen Daten sprechen für das geplante Training. Progression nur dort, wo Leistung und Rückmeldung dafür sprechen.','Datele actuale susțin antrenamentul planificat. Progresează doar unde performanța și feedbackul anterior justifică asta.'],
8599:   'coach.reducedText':['Kezdj óvatosabban. A korábban nehéznek jelölt gyakorlatoknál tartsd a terhelést; súlyemelést csak jó bemelegítés után válassz.','Start more conservatively. Hold load on exercises previously marked hard; increase only after a good warm-up.','Etwas vorsichtiger starten. Bei zuletzt schweren Übungen Belastung halten; nur nach gutem Aufwärmen steigern.','Începe mai prudent. Păstrează greutatea la exercițiile marcate anterior ca dificile; crește doar după o încălzire bună.'],
8600:   'coach.lightText':['A mai jelek alapján ne erőltesd a progressziót. Könnyített edzés vagy pihenő is megfelelő választás lehet.','Do not force progression today. A lighter session or rest can be appropriate.','Heute keine Progression erzwingen. Leichteres Training oder Pause kann passend sein.','Nu forța progresia azi. Un antrenament mai ușor sau odihna pot fi potrivite.'],
8601:   'coach.painText':['A közelmúltban fájdalom / kellemetlenség jelzés volt. Az érintett gyakorlatnál ne emelj terhelést.','A recent pain/discomfort flag exists. Do not increase load on the affected exercise.','Kürzlich wurde Schmerz/Beschwerden gemeldet. Bei der betroffenen Übung nicht steigern.','Există un semnal recent de durere/disconfort. Nu crește greutatea la exercițiul afectat.'],
8602:   'coach.sleep':['Alvás','Sleep','Schlaf','Somn'],'coach.hrv':['HRV','HRV','HRV','HRV'],
8603:   'coach.load':['Edzésterhelés','Training load','Trainingsbelastung','Încărcare'],
8604:   'coach.dataAge':['Regenerációs adat','Recovery data','Regenerationsdaten','Date recuperare'],
8605:   'coach.daysOld':['napos','days old','Tage alt','zile vechi'],
8606:   'coach.todayWord':['ma','today','heute','astăzi'],
8607:   'coach.noRecovery':['Nincs friss alvás/HRV adat','No recent sleep/HRV data','Keine aktuellen Schlaf-/HRV-Daten','Nu există date recente de somn/HRV'],
8608:   'coach.hold':['Tartás','Hold','Halten','Menține'],'coach.increase':['Emelhető','Can increase','Steigerbar','Poți crește'],
8609:   'coach.reps':['Ismétlés +','Reps +','Wdh. +','Repetări +'],'coach.easyStart':['Könnyebb kezdés','Start lighter','Leichter starten','Începe mai ușor'],
8610:   'coach.startLighter':['indulj könnyebben','start lighter','leichter starten','începe mai ușor'],
8611:   'coach.pain':['Ne emelj','Do not increase','Nicht steigern','Nu crește'],
8612:   'coach.startLoad':['Kezdő terhelés','Starting load','Startbelastung','Greutate inițială'],
8613:   'coach.unknown':['Nincs elég előzmény','Not enough history','Nicht genug Verlauf','Istoric insuficient'],
8614:   'coach.readiness':['Mai készenlét','Today readiness','Heutige Bereitschaft','Pregătire azi'],
8615:   'coach.approx':['A Coach tájékoztató edzéstámogatás, nem orvosi értékelés.','Coach guidance is informational and is not medical advice.','Coach-Hinweise sind informativ und keine medizinische Bewertung.','Recomandările Coach sunt informative, nu evaluare medicală.']
8616:  };
8617:  for(const [key,row] of Object.entries(rows)){
8618:   TP149_KEY_ROWS[key]=row;
8619:   TP149_CATALOG.hu[key]=row[0];TP149_CATALOG.en[key]=row[1];TP149_CATALOG.de[key]=row[2];TP149_CATALOG.ro[key]=row[3];
8620:  }
8621: })();
8622: 
8623: var tp149HealthFmt=function tp149HealthFmt(value,digits,suffix){
8624:  const n=Number(value);if(!Number.isFinite(n))return '—';
8625:  return tp149FormatNumber(n,{minimumFractionDigits:digits,maximumFractionDigits:digits})+(suffix||'');
8626: };
8627: var tp149SleepText=function tp149SleepText(minutes){
8628:  const n=Number(minutes);if(!Number.isFinite(n))return '—';
8629:  const h=Math.floor(n/60),m=Math.round(n%60);
8630:  return (h?tp149FormatNumber(h)+' h ':'')+tp149FormatNumber(m)+' min';
8631: };
8632: 
8633: healthSettings=async function(){
8634:  try{
8635:   await healthPlugin().openSettings();
8636:   state.health=state.health||{};state.health.message=tp149T('health.permissionsOpened');
8637:  }catch(e){
8638:   state.health=state.health||{};state.health.message=e?.message||tp149T('health.permissionsError');
8639:   if(state.tab==='health'&&!state.session)rf263HealthHub();
8640:  }
8641: };
8642: 
8643: rf263HealthHub=function(){
8644:  state.tab='health';state.healthView=false;if(typeof rf225HealthPage!=='undefined')rf225HealthPage='hub';rf240ApplyLedger();
8645:  const ledger=rf240Ledger(),today=ledger.days?.[rf240DayKey(new Date())]||{},rec=state.health?.recovery||{},w=state.health?.wellness||{};
8646:  const manualWeight=(function(){const all=typeof rf215All==='function'?rf215All():[];const n=Number(all.at?.(-1)?.kg);return Number.isFinite(n)?n:null;})();
8647:  const healthWeight=w.weightKg==null?null:Number(w.weightKg),shownWeight=Number.isFinite(healthWeight)?healthWeight:manualWeight;
8648:  const weightSource=Number.isFinite(healthWeight)?'Health Connect':shownWeight!=null?tp149T('health.localEntry'):'';
8649:  const coach=typeof rf235HealthCoachCard==='function'?rf235HealthCoachCard():'';
8650:  const syncAt=ledger.lastSyncAt?tp149FormatDateTime(ledger.lastSyncAt):tp149T('health.never');
8651:  const sleepSub=rec.sleepMinutes==null?'':rec.sleepStageBased?tp149T('health.sleepStage'):(rec.sleepSource?tp149T('health.sessionDuration')+' • '+rf263FriendlySource(rec.sleepSource):tp149T('health.sessionDuration'));
8652:  const bp=w.bloodPressureSystolic==null||w.bloodPressureDiastolic==null?'—':tp149FormatNumber(Math.round(w.bloodPressureSystolic))+'/'+tp149FormatNumber(Math.round(w.bloodPressureDiastolic))+' mmHg';
8653:  const busy=!!state.health?.busy;
8654:  const body='<main class="rf263-health"><div class="hero"><h1>'+esc(tp149T('health.title'))+'</h1><div class="muted">'+esc(tp149T('health.intro'))+'</div></div><div class="card rf263-sync-card"><div class="grid2 rf263-sync-actions"><button class="btn block" data-tp-role="primary" onclick="rf244UnifiedSync()" '+(busy?'disabled':'')+'>'+esc(tp149T('health.sync'))+'</button><button class="btn secondary block" data-tp-role="secondary" onclick="healthSettings()" '+(busy?'disabled':'')+'>'+esc(tp149T('health.permissions'))+'</button></div><p class="small muted">'+esc(tp149T('health.lastSync',{time:syncAt}))+'</p>'+(state.health?.message?'<p class="small" role="status">'+esc(state.health.message)+'</p>':'')+'</div>'+coach+
8655:  '<div class="card"><h2>'+esc(tp149T('health.todayStatus'))+'</h2><div class="grid2">'+
8656:  rf244Stat(tp149T('health.sleep'),tp149SleepText(rec.sleepMinutes),sleepSub)+
8657:  rf244Stat('HRV',rec.hrvRmssdMs==null?'—':tp149FormatNumber(Math.round(rec.hrvRmssdMs))+' ms')+
8658:  rf244Stat(tp149T('health.steps'),today.steps==null?'—':tp149FormatNumber(Math.round(today.steps)))+
8659:  rf244Stat(tp149T('health.activeEnergy'),today.activeCalories==null?'—':tp149FormatNumber(Math.round(today.activeCalories))+' kcal')+
8660:  rf244Stat(tp149T('health.avgHeartRate'),today.averageHeartRate==null?'—':tp149FormatNumber(Math.round(today.averageHeartRate))+' bpm')+
8661:  rf244Stat(tp149T('health.restingHeartRate'),today.restingHeartRate==null?'—':tp149FormatNumber(Math.round(today.restingHeartRate))+' bpm')+
8662:  '</div></div><div class="card"><h2>'+esc(tp149T('health.bodyFitness'))+'</h2><div class="grid2">'+
8663:  rf244Stat(tp149T('health.weight'),tp149HealthFmt(shownWeight,1,' kg'),weightSource)+
8664:  rf244Stat(tp149T('health.bodyFat'),w.bodyFatPercent==null?'—':tp149HealthFmt(w.bodyFatPercent,1,' %'))+
8665:  rf244Stat('SpO₂',w.oxygenSaturationPercent==null?'—':tp149HealthFmt(w.oxygenSaturationPercent,1,' %'))+
8666:  rf244Stat('VO₂max',w.vo2Max==null?'—':tp149HealthFmt(w.vo2Max,1,''))+
8667:  '</div><button id="rf251WeightJournal" class="btn secondary block rf251-weight-btn" onclick="rf215WeightScreen()">'+esc(tp149T('health.weightJournal'))+'</button></div>'+
8668:  '<details class="card"><summary><strong>'+esc(tp149T('health.more'))+'</strong></summary><div class="grid2">'+
8669:  rf244Stat(tp149T('health.bloodPressure'),bp)+
8670:  rf244Stat(tp149T('health.bloodGlucose'),w.bloodGlucoseMmolL==null?'—':tp149HealthFmt(w.bloodGlucoseMmolL,1,' mmol/L'))+
8671:  rf244Stat(tp149T('health.respiratoryRate'),w.respiratoryRate==null?'—':tp149HealthFmt(w.respiratoryRate,1,' /min'))+
8672:  rf244Stat(tp149T('health.distance'),today.distanceMeters==null?'—':tp149HealthFmt(today.distanceMeters/1000,2,' km'))+
8673:  '</div></details><details class="card"><summary><strong>'+esc(tp149T('health.recoveryHistory'))+'</strong></summary>'+rf244RecoveryRows()+'</details>'+rf263DiagHtml()+'</main>';
8674:  render(shell(body));if(typeof tp146CompactHealthCards==='function')tp146CompactHealthCards();
8675: };
8676: 
8677: rf250HealthHub=rf263HealthHub;
8678: rf206HealthHub=rf263HealthHub;
8679: 
8680: rf233L=function(){
8681:  return {
8682:   coach:tp149T('coach.title'),today:tp149T('coach.today'),next:tp149T('coach.next'),suggested:tp149T('coach.suggested'),noPlan:tp149T('coach.noPlan'),
8683:   start:tp149T('coach.start'),calendar:tp149T('coach.calendar'),refresh:tp149T('coach.openHealth'),why:tp149T('coach.why'),exercisePlan:tp149T('coach.exercisePlan'),
8684:   normal:tp149T('coach.normal'),reduced:tp149T('coach.reduced'),light:tp149T('coach.light'),normalText:tp149T('coach.normalText'),reducedText:tp149T('coach.reducedText'),
8685:   lightText:tp149T('coach.lightText'),painText:tp149T('coach.painText'),sleep:tp149T('coach.sleep'),hrv:tp149T('coach.hrv'),load:tp149T('coach.load'),
8686:   dataAge:tp149T('coach.dataAge'),daysOld:tp149T('coach.daysOld'),noRecovery:tp149T('coach.noRecovery'),hold:tp149T('coach.hold'),increase:tp149T('coach.increase'),
8687:   reps:tp149T('coach.reps'),easyStart:tp149T('coach.easyStart'),startLighter:tp149T('coach.startLighter'),pain:tp149T('coach.pain'),startLoad:tp149T('coach.startLoad'),
8688:   unknown:tp149T('coach.unknown'),approx:tp149T('coach.approx')
8689:  };
8690: };
8691: 
8692: rf233ExerciseAdvice=function(id,decision){
8693:  const t=rf233L();let rec=null,last=null,base=0,effort=null;
8694:  try{rec=rf152Recommendation(id);last=rf152Last(id);effort=last?.exercise?.effort||null;base=last?.exercise?rf152BaseWeight(last.exercise):0;}catch(_){}
8695:  if(effort==='pain')return {action:'pain',label:t.pain,text:rec?.text||t.painText};
8696:  if(decision.mode==='light')return {action:'easy',label:t.easyStart,text:base>0?t.lightText+' ('+base+' '+loadLabel(byId(id)?.loadType)+' → '+t.startLighter+'.)':t.lightText};
8697:  if(decision.mode==='reduced'&&(effort==='hard'||effort==='challenging'))return {action:'hold',label:t.hold,text:rec?.text||t.reducedText};
8698:  if(decision.mode==='reduced'&&rec?.action==='increase')return {action:'hold',label:t.hold,text:t.reducedText};
8699:  if(rec?.action==='increase')return {action:'increase',label:t.increase,text:rec.text};
8700:  if(rec?.action==='reps')return {action:'reps',label:t.reps,text:rec.text};
8701:  if(rec?.action==='hold')return {action:'hold',label:t.hold,text:rec.text};
8702:  if(rec?.action==='start')return {action:'start',label:t.startLoad,text:rec.text};
8703:  return {action:'unknown',label:t.unknown,text:rec?.text||t.unknown};
8704: };
8705: 
8706: rf233CoachPlan=function(){
8707:  const readiness=rf220Readiness(),decision=rf233Decision(readiness),target=rf233TargetWorkout(),ids=target.day?.exercises||[];
8708:  return {readiness:readiness,decision:decision,target:target,exercises:ids.map(function(id){const e=byId(id);return {id:id,name:e?tp149ExerciseName(e):id,effort:rf233LastEffort(id),advice:rf233ExerciseAdvice(id,decision)};})};
8709: };
8710: 
8711: rf233SignalHtml=function(plan){
8712:  if(window.TrainPilot110)return window.TrainPilot110.coachReasonsHtml(plan);
8713:  const t=rf233L(),r=plan.readiness,age=rf233RecoveryAgeDays(),rows=[];
8714:  rows.push(t.sleep+': '+(r.sleep==null?'—':tp149FormatNumber(Math.round(r.sleep/6)/10,{maximumFractionDigits:1})+' h'));
8715:  rows.push(t.hrv+': '+(r.hrv==null?'—':tp149FormatNumber(Math.round(r.hrv))+' ms')+(r.hrv!=null&&r.base?' / '+tp149FormatNumber(Math.round(r.base))+' ms':''));
8716:  rows.push(t.load+': '+tp149FormatNumber(r.load)+'/100');
8717:  rows.push(age==null?t.noRecovery:t.dataAge+': '+(age===0?tp149T('coach.todayWord'):tp149FormatNumber(age)+' '+t.daysOld));
8718:  return rows.map(function(x){return '<div class="rf233-signal">'+esc(x)+'</div>';}).join('');
8719: };
8720: 
8721: rf233TargetSub=function(target){
8722:  if(!target.day)return '';
8723:  const p=target.program,bits=[p?tp149ProgramMeta(p,'name'):'',p?tp149ProgramDayName(p,target.day):(target.day?.name||target.day?.id||'')];
8724:  if(target.item?.start)bits.push(tp149FormatDateTime(target.item.start,{month:'short',day:'numeric',hour:'2-digit',minute:'2-digit'}));
8725:  return bits.filter(Boolean).join(' • ');
8726: };
8727: 
8728: rf233CoachScreen=function(){
8729:  state.tab='health';state.healthView=true;
8730:  if(typeof rf245CoachVisible!=='undefined')rf245CoachVisible=true;
8731:  const t=rf233L(),p=rf233CoachPlan(),r=p.readiness,target=p.target;
8732:  const button=target.kind==='today'?'<button class="btn block" onclick="rf233StartTarget()">'+esc(t.start)+'</button>':target.kind==='next'?'<button class="btn secondary block" onclick="rf233StartTarget()">'+esc(t.calendar)+'</button>':target.kind==='suggested'?'<button class="btn block" onclick="rf233StartTarget()">'+esc(t.start)+'</button>':'<button class="btn secondary block" onclick="go(\'calendar\')">'+esc(t.calendar)+'</button>';
8733:  const targetName=target.day?(target.program?tp149ProgramDayName(target.program,target.day):(target.day.name||target.day.id||'—')):'—';
8734:  render(shell('<main><button class="btn secondary" onclick="go(\'home\')">← '+esc(tp149T('common.back'))+'</button><div class="hero"><h1>'+esc(t.coach)+'</h1><div class="muted">'+esc(p.decision.text)+'</div></div><div class="card rf233-today"><div class="rf233-score"><div><small>'+esc(tp149T('coach.readiness'))+'</small><strong>'+tp149FormatNumber(r.score)+'/100</strong></div><div><small>'+esc(rf233TargetTitle(target))+'</small><strong>'+esc(targetName)+'</strong></div></div><p><b>'+esc(p.decision.title)+'</b></p><p class="small muted">'+esc(rf233TargetSub(target))+'</p>'+button+'</div><div class="card"><strong>'+esc(t.why)+'</strong><div class="rf233-signals">'+rf233SignalHtml(p)+'</div><br><button class="btn secondary block" onclick="go(\'health\')">'+esc(tp149T('coach.openHealth'))+'</button></div><div class="section">'+esc(t.exercisePlan)+'</div>'+rf233ExerciseRows(p)+'<p class="small muted">'+esc(t.approx)+'</p>'+rf220ProgressHtml()+'</main>'));
8735: };
8736: rf220CoachScreen=rf233CoachScreen;
8737: 
8738: rf233BindHome=function(){
8739:  if(window.TrainPilotBoot?.loading)return;
8740:  setTimeout(function(){
8741:   const main=document.querySelector('main');if(!main||main.querySelector('#rf220CoachCard'))return;
8742:   const p=rf233CoachPlan(),t=rf233L(),c=document.createElement('div'),targetName=p.target.day?(p.target.program?tp149ProgramDayName(p.target.program,p.target.day):(p.target.day.name||p.target.day.id||'—')):'—';
8743:   c.id='rf220CoachCard';c.className='card rf233-home';
8744:   c.innerHTML='<strong>'+esc(t.coach)+'</strong><div class="rf233-homegrid"><div><span class="small muted">'+esc(tp149T('coach.readiness'))+'</span><b>'+tp149FormatNumber(p.readiness.score)+'/100</b></div><div><span class="small muted">'+esc(rf233TargetTitle(p.target))+'</span><b>'+esc(targetName)+'</b></div></div><p class="small">'+esc(p.decision.title)+' • '+esc(p.decision.text)+'</p><button class="btn block" onclick="rf233CoachScreen()">'+esc(t.coach)+'</button>';
8745:   const hero=main.querySelector('.hero');if(hero)hero.insertAdjacentElement('afterend',c);else main.prepend(c);
8746:  },0);
8747: };
8748: rf220BindHome=rf233BindHome;
8749: 
8750: window.TrainPilotI18n.healthCoachAudit=function(){
8751:  const keys=['health.title','health.sync','health.permissions','health.todayStatus','health.bodyFitness','health.more','health.recoveryHistory','coach.title','coach.today','coach.next','coach.openHealth','coach.why','coach.exercisePlan','coach.approx'];
8752:  const missing={hu:[],en:[],de:[],ro:[]};
8753:  for(const lang of Object.keys(missing))for(const key of keys)if(!TP149_CATALOG[lang]?.[key])missing[lang].push(key);
8754:  return missing;
8755: };
8756: // @endsection trainpilot-149-health-coach-i18n.js
8757: 
8758: 
```

## www/app.js — 11483–11563

Ref: `7473b30f387eda54186c1e31830304ca12bd104f`; teljes forrás SHA256: `47750fd38905860aa5910a14ff52a46421a3c78e19059c34cc5025da2538106d`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/app.js

```text
11483: // @section trainpilot-155-coach-health-polish.js
11484: /* TrainPilot 1.5.5: 2026-09-20 Health panel alignment + concise per-exercise Coach guidance. */
11485: (function(){
11486:  'use strict';
11487:  const TP155_TODAY_POLISH_VERSION='1.5.5-2026-09-20';
11488:  const COPY={
11489:   hu:{coach:'Coach javaslat',target:'Célterhelés:',reps:'Cél: +1–2 szabályos ismétlés/sorozat.',hold:'Tartsd a jelenlegi terhelést; célozz +1 ismétlést.',light:'Kezdj könnyebben; ma ne erőltesd a progressziót.',pain:'Ne emelj; csak fájdalommentes végrehajtással folytasd.',start:'Kezdj kontrollált, kényelmes terheléssel.'},
11490:   en:{coach:'Coach suggestion',target:'Target load:',reps:'Target: +1–2 clean reps per set.',hold:'Keep the current load; aim for +1 rep.',light:'Start lighter; do not force progression today.',pain:'Do not increase; continue only pain-free.',start:'Start with a controlled, comfortable load.'},
11491:   de:{coach:'Coach-Hinweis',target:'Zielbelastung:',reps:'Ziel: +1–2 saubere Wdh. pro Satz.',hold:'Belastung halten; +1 Wdh. anstreben.',light:'Leichter starten; heute keine Progression erzwingen.',pain:'Nicht steigern; nur schmerzfrei fortsetzen.',start:'Kontrolliert mit angenehmer Belastung starten.'},
11492:   ro:{coach:'Sugestie Coach',target:'Sarcină țintă:',reps:'Țintă: +1–2 repetări corecte pe serie.',hold:'Păstrează sarcina; urmărește +1 repetare.',light:'Începe mai ușor; nu forța progresia azi.',pain:'Nu crește sarcina; continuă doar fără durere.',start:'Începe controlat, cu o sarcină confortabilă.'},
11493:   sk:{coach:'Odporúčanie Coach',target:'Cieľová záťaž:',reps:'Cieľ: +1–2 čisté opakovania na sériu.',hold:'Zachovaj záťaž; cieľ je +1 opakovanie.',light:'Začni ľahšie; dnes netlač progresiu.',pain:'Nezvyšuj záťaž; pokračuj iba bez bolesti.',start:'Začni kontrolovane s pohodlnou záťažou.'},
11494:   pl:{coach:'Sugestia Coach',target:'Obciążenie docelowe:',reps:'Cel: +1–2 poprawne powtórzenia na serię.',hold:'Utrzymaj obciążenie; celuj w +1 powtórzenie.',light:'Zacznij lżej; dziś nie wymuszaj progresji.',pain:'Nie zwiększaj obciążenia; kontynuuj tylko bez bólu.',start:'Zacznij kontrolowanie, z komfortowym obciążeniem.'}
11495:  };
11496:  var lang=function(){try{return typeof rf212Lang==='function'?rf212Lang():'hu'}catch(_){return 'hu'}};
11497:  var copy=function(){return COPY[lang()]||COPY.hu};
11498:  window.tp155CoachShortAdvice=function(id,decision,existing){
11499:   var advice=existing||null,rec=null;
11500:   try{if(!advice&&typeof rf233ExerciseAdvice==='function')advice=rf233ExerciseAdvice(id,decision||rf233Decision(rf220Readiness()))}catch(_){}
11501:   try{if(typeof rf152Recommendation==='function')rec=rf152Recommendation(id)}catch(_){}
11502:   var action=advice&&advice.action||rec&&rec.action||'start',c=copy(),text=c.start;
11503:   if(action==='pain')text=c.pain;
11504:   else if(action==='easy')text=c.light;
11505:   else if(action==='increase'){
11506:    var w=Number(rec&&rec.weight);
11507:    if(Number.isFinite(w)&&w>0){
11508:     var unit='';try{unit=tp149LoadLabel((byId(id)||{}).loadType)||''}catch(_){}
11509:     text=c.target+' '+String(Math.round(w*10)/10)+(unit?' '+unit:'');
11510:    }else text=c.reps;
11511:   }else if(action==='reps')text=c.reps;
11512:   else if(action==='hold')text=c.hold;
11513:   return {action:action,label:advice&&advice.label||'',text:text};
11514:  };
11515: 
11516:  if(typeof rf233ExerciseRows==='function'){
11517:   rf233ExerciseRows=function(plan){
11518:    var t=rf233L();if(!plan||!plan.exercises||!plan.exercises.length)return '<p class="muted">'+esc(t.unknown)+'</p>';
11519:    return plan.exercises.map(function(x){
11520:     var a=window.tp155CoachShortAdvice(x.id,plan.decision,x.advice);
11521:     return '<div class="card rf233-ex tp155-coach-ex"><div class="rf233-exhead"><strong>'+esc(x.name)+'</strong><span class="badge">'+esc(a.label)+'</span></div><div class="small muted tp155-coach-short">'+esc(a.text)+'</div></div>';
11522:    }).join('');
11523:   };
11524:  }
11525: 
11526:  window.tp155NormalizeHealthBottomPanels=function(){
11527:   var main=document.querySelector('main.rf263-health');if(!main)return;
11528:   var panels=[].slice.call(main.querySelectorAll(':scope > details.card')).slice(-3);
11529:   panels.forEach(function(panel){panel.classList.add('tp151-details','tp155-health-bottom-panel')});
11530:  };
11531:  if(typeof rf263HealthHub==='function'){
11532:   const tp155HealthHubBase=rf263HealthHub;
11533:   rf263HealthHub=function(){var r=tp155HealthHubBase.apply(this,arguments);window.tp155NormalizeHealthBottomPanels();return r};
11534:  }
11535: 
11536:  window.tp155InjectWorkoutCoach=function(){
11537:   var main=document.querySelector('main.tp153-workout'),head=main&&main.querySelector('.tp153-workout-head');
11538:   if(!head||head.querySelector('.tp155-workout-coach')||!state.session)return;
11539:   var i=Math.max(0,Math.min(Number(state.current)||0,(state.session.exercises||[]).length-1)),se=(state.session.exercises||[])[i];if(!se)return;
11540:   var decision=null,advice=null;try{decision=rf233Decision(rf220Readiness());advice=rf233ExerciseAdvice(se.id,decision)}catch(_){}
11541:   var a=window.tp155CoachShortAdvice(se.id,decision,advice),c=copy(),box=document.createElement('div');
11542:   box.className='tp155-workout-coach';
11543:   box.innerHTML='<span class="tp155-workout-coach-kicker">'+esc(c.coach)+'</span>'+(a.label?'<strong>'+esc(a.label)+'</strong>':'')+'<p>'+esc(a.text)+'</p>';
11544:   var anchor=head.querySelector('.tp153-workout-prescription');if(anchor)anchor.insertAdjacentElement('afterend',box);else head.appendChild(box);
11545:  };
11546:  if(typeof renderWorkout==='function'){
11547:   const tp155RenderWorkoutBase=renderWorkout;
11548:   renderWorkout=function(){var r=tp155RenderWorkoutBase.apply(this,arguments);window.tp155InjectWorkoutCoach();return r};
11549:  }
11550: 
11551:  const style=document.createElement('style');style.id='tp155TodayPolishCss';style.textContent=[
11552:   'main.rf263-health>details.tp155-health-bottom-panel{box-sizing:border-box!important;width:100%!important;max-width:none!important;margin-left:0!important;margin-right:0!important;padding:10px!important;align-self:stretch!important}',
11553:   'main.rf263-health>details.tp155-health-bottom-panel>summary{box-sizing:border-box!important;width:100%!important;min-height:30px!important;margin:0!important;padding:0!important;align-items:center!important}',
11554:   '.tp155-coach-short{line-height:1.32!important}',
11555:   '.tp155-workout-coach{margin:9px 0 2px;padding:9px 10px;border:1px solid color-mix(in srgb,var(--accent) 34%,var(--line));border-radius:12px;background:color-mix(in srgb,var(--accent) 10%,var(--card2));display:grid;gap:2px}',
11556:   '.tp155-workout-coach-kicker{font-size:10px;font-weight:850;letter-spacing:.07em;text-transform:uppercase;color:var(--accent)}',
11557:   '.tp155-workout-coach strong{font-size:13px}.tp155-workout-coach p{margin:0;font-size:12px;line-height:1.32;color:var(--muted)}'
11558:  ].join('');
11559:  document.head.appendChild(style);
11560:  window.TrainPilotTodayPolish={version:TP155_TODAY_POLISH_VERSION,healthPanels:true,shortCoach:true,workoutCoach:true};
11561: })();
11562: // @endsection trainpilot-155-coach-health-polish.js
11563: 
```

## www/app.js — 16036–16371

Ref: `7473b30f387eda54186c1e31830304ca12bd104f`; teljes forrás SHA256: `47750fd38905860aa5910a14ff52a46421a3c78e19059c34cc5025da2538106d`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/app.js

```text
16036: // @section health-journal-120.js
16037: (function(){
16038:  'use strict';
16039:  const texts={
16040:   hu:{title:'Egészségnapló',all:'Minden forrás',hc:'Health Connect',watch:'Közvetlen óra',legacy:'Korábbi napló',from:'Ettől',to:'Eddig',source:'Adatforrás',primary:'Lépésszám elsődleges forrása',auto:'Health Connect frissítése appmegnyitáskor',logWatch:'Megjegyzett óra lépéseinek naplózása',autoNote:'Előtérben frissít, ha már van engedély. A háttérkapcsolat külön fejlesztési lépés.',watchNote:'Az óra aktuális lépésleolvasását mentjük a telefon dátumával. Kalória, távolság és alvás óráról még nincs igazolva.',refresh:'Frissítés',syncWatch:'Óra frissítése',next:'Következő oldal',empty:'Ebben az időszakban nincs naplózott adat.',loading:'Napló betöltése…',partial:'Részleges / korábbi adat',stale:'Korábbi leolvasás',observed:'Leolvasás a telefon ideje szerint',aggregate:'Health Connect összesítés · forrásprioritással',records:'Mérési rekordok',days:'Napi összesítések',samples:'minta',session:'Alvási session időtartama',saved:'Beállítás mentve',failed:'A napló nem frissült. A korábbi adatok megmaradtak.',native:'A tartós egészségnapló az Android alkalmazásban érhető el.',noWatch:'Előbb válassz és jegyezz meg egy órát a Bluetooth-próbában.',busy:'Várd meg a futó művelet végét.',settings:'Napló beállításai',unknown:'Ismeretlen eredet',active:'Aktív kalória',total:'Teljes kalória',steps:'Lépések',sleep:'Alvás',pulse:'Pulzus',resting:'Nyugalmi pulzus',distance:'Távolság',speed:'Sebesség',weight:'Testsúly',fat:'Testzsír',respiratory:'Légzésszám',glucose:'Vércukor',pressure:'Vérnyomás',exercise:'Edzés'},
16041:   en:{title:'Health journal',all:'All sources',hc:'Health Connect',watch:'Direct watch',legacy:'Previous journal',from:'From',to:'Until',source:'Data source',primary:'Preferred steps source',auto:'Refresh Health Connect when opening the app',logWatch:'Log steps from the remembered watch',autoNote:'Refreshes in the foreground with existing permissions. Background connection is a separate development phase.',watchNote:'Current watch step readings use the phone date. Watch calories, distance and sleep are not verified yet.',refresh:'Refresh',syncWatch:'Refresh watch',next:'Next page',empty:'No logged data in this period.',loading:'Loading journal…',partial:'Partial / previous data',stale:'Earlier reading',observed:'Reading using phone time',aggregate:'Health Connect aggregate · source priority',records:'Measurement records',days:'Daily summaries',samples:'samples',session:'Sleep session duration',saved:'Setting saved',failed:'Journal did not refresh. Previous data is retained.',native:'The persistent health journal is available in the Android app.',noWatch:'Choose and remember a watch in the Bluetooth trial first.',busy:'Wait for the current operation.',settings:'Journal settings',unknown:'Unknown origin',active:'Active calories',total:'Total calories',steps:'Steps',sleep:'Sleep',pulse:'Heart rate',resting:'Resting heart rate',distance:'Distance',speed:'Speed',weight:'Weight',fat:'Body fat',respiratory:'Respiratory rate',glucose:'Blood glucose',pressure:'Blood pressure',exercise:'Exercise'},
16042:   de:{title:'Gesundheitsjournal',all:'Alle Quellen',hc:'Health Connect',watch:'Direkte Uhr',legacy:'Bisheriges Journal',from:'Von',to:'Bis',source:'Datenquelle',primary:'Bevorzugte Schrittquelle',auto:'Health Connect beim Öffnen aktualisieren',logWatch:'Schritte der gespeicherten Uhr protokollieren',autoNote:'Aktualisiert im Vordergrund mit bestehenden Berechtigungen. Die Hintergrundverbindung folgt separat.',watchNote:'Aktuelle Schrittwerte werden mit dem Telefondatum gespeichert. Kalorien, Distanz und Schlaf der Uhr sind noch nicht bestätigt.',refresh:'Aktualisieren',syncWatch:'Uhr aktualisieren',next:'Nächste Seite',empty:'Keine Daten in diesem Zeitraum.',loading:'Journal wird geladen…',partial:'Teilweise / bisherige Daten',stale:'Frühere Messung',observed:'Ablesung nach Telefonzeit',aggregate:'Health Connect Summe · Quellenpriorität',records:'Messdatensätze',days:'Tagessummen',samples:'Messungen',session:'Dauer der Schlafsitzung',saved:'Einstellung gespeichert',failed:'Journal nicht aktualisiert. Bisherige Daten bleiben erhalten.',native:'Das dauerhafte Journal ist in der Android-App verfügbar.',noWatch:'Zuerst eine Uhr im Bluetooth-Test auswählen und speichern.',busy:'Warte auf den aktuellen Vorgang.',settings:'Journal-Einstellungen',unknown:'Unbekannte Herkunft',active:'Aktive Kalorien',total:'Gesamtkalorien',steps:'Schritte',sleep:'Schlaf',pulse:'Puls',resting:'Ruhepuls',distance:'Distanz',speed:'Geschwindigkeit',weight:'Gewicht',fat:'Körperfett',respiratory:'Atemfrequenz',glucose:'Blutzucker',pressure:'Blutdruck',exercise:'Training'},
16043:   ro:{title:'Jurnal de sănătate',all:'Toate sursele',hc:'Health Connect',watch:'Ceas direct',legacy:'Jurnal anterior',from:'De la',to:'Până la',source:'Sursă',primary:'Sursa preferată pentru pași',auto:'Actualizează Health Connect la deschiderea aplicației',logWatch:'Înregistrează pașii ceasului memorat',autoNote:'Actualizează în prim-plan cu permisiunile existente. Conexiunea în fundal urmează separat.',watchNote:'Pașii actuali ai ceasului se salvează cu data telefonului. Caloriile, distanța și somnul ceasului nu sunt confirmate încă.',refresh:'Actualizează',syncWatch:'Actualizează ceasul',next:'Pagina următoare',empty:'Nu există date în această perioadă.',loading:'Se încarcă jurnalul…',partial:'Date parțiale / anterioare',stale:'Citire anterioară',observed:'Citire după ora telefonului',aggregate:'Total Health Connect · prioritatea surselor',records:'Înregistrări măsurate',days:'Totaluri zilnice',samples:'măsurători',session:'Durata sesiunii de somn',saved:'Setare salvată',failed:'Jurnalul nu s-a actualizat. Datele anterioare sunt păstrate.',native:'Jurnalul persistent este disponibil în aplicația Android.',noWatch:'Alege și memorează un ceas în testul Bluetooth mai întâi.',busy:'Așteaptă operațiunea curentă.',settings:'Setări jurnal',unknown:'Origine necunoscută',active:'Calorii active',total:'Calorii totale',steps:'Pași',sleep:'Somn',pulse:'Puls',resting:'Puls în repaus',distance:'Distanță',speed:'Viteză',weight:'Greutate',fat:'Grăsime corporală',respiratory:'Frecvență respiratorie',glucose:'Glicemie',pressure:'Tensiune arterială',exercise:'Antrenament'}
16044:  };
16045:  for(const [language,extra] of Object.entries({"hu": {"phone": "Telefonadatok", "dailySource": "Napi lépések, távolság és kalória forrása", "sourceAuto": "Legteljesebb napi lépésforrás (automatikus)", "sourcePriority": "Health Connect forrásprioritás", "sourceNote": "Automatikus módban a napra a legnagyobb teljes lépésszámot adó Health Connect-forrást használjuk a lépésekhez, távolsághoz és kalóriához. A forrásokat nem adjuk össze. Külön forrás továbbra is választható.", "bleBackground": "Óra kapcsolata a háttérben", "hcBackground": "Health Connect háttérfrissítése", "backgroundNote": "Óra: tartós kapcsolat, lépések 5 percenként, értesítésből leállítható. Health Connect: külön engedéllyel, időszakos Android-frissítés. A rendszer korlátozhatja; kényszerleállítás után nyisd meg az appot.", "backgroundUnavailable": "A Health Connect háttérolvasása ezen a rendszeren nem érhető el.", "summaryUnavailable": "A kiválasztott forrás összesítése nem frissült. Legfeljebb 31 napot válassz, és ellenőrizd a Health Connect engedélyeit.", "activitySource": "Lépések / távolság / kalória", "connected": "Óra kapcsolódva", "waiting": "Óra kapcsolódása / újracsatlakozás", "stopped": "Háttérkapcsolat kikapcsolva", "autoNote": "Megnyitáskor frissít, ha már van engedély. A háttérfrissítés külön kapcsolható."}, "en": {"phone": "Phone data", "dailySource": "Daily steps, distance and calories source", "sourceAuto": "Most complete daily step source (automatic)", "sourcePriority": "Health Connect source priority", "sourceNote": "Automatic mode uses the Health Connect origin with the largest complete daily step total for steps, distance and calories. Sources are never added together. You can still select a specific origin.", "bleBackground": "Keep watch connected in the background", "hcBackground": "Refresh Health Connect in the background", "backgroundNote": "Watch: persistent connection, steps every 5 minutes, stop from the notification. Health Connect: separate permission and periodic Android updates. The system may delay updates; reopen after force-stop.", "backgroundUnavailable": "Health Connect background reading is unavailable on this system.", "summaryUnavailable": "The selected source summary did not refresh. Choose up to 31 days and check Health Connect permissions.", "activitySource": "Steps / distance / calories", "connected": "Watch connected", "waiting": "Watch connecting / reconnecting", "stopped": "Background connection disabled", "autoNote": "Refreshes when opening with existing permission. Background refresh has its own switch."}, "de": {"phone": "Telefondaten", "dailySource": "Quelle für tägliche Schritte, Distanz und Kalorien", "sourceAuto": "Vollständigste tägliche Schrittquelle (automatisch)", "sourcePriority": "Health Connect Quellenpriorität", "sourceNote": "Im Automatikmodus wird die Health-Connect-Quelle mit der höchsten vollständigen Tagesschrittzahl für Schritte, Distanz und Kalorien verwendet. Quellen werden nicht addiert. Eine bestimmte Quelle bleibt auswählbar.", "bleBackground": "Uhr im Hintergrund verbunden halten", "hcBackground": "Health Connect im Hintergrund aktualisieren", "backgroundNote": "Uhr: dauerhafte Verbindung, Schritte alle 5 Minuten, Stopp über Benachrichtigung. Health Connect: separate Berechtigung und regelmäßige Android-Aktualisierung. Das System kann verzögern; nach erzwungenem Stopp App öffnen.", "backgroundUnavailable": "Health Connect kann auf diesem System nicht im Hintergrund gelesen werden.", "summaryUnavailable": "Die Quellensumme wurde nicht aktualisiert. Bis zu 31 Tage auswählen und Health Connect Berechtigungen prüfen.", "activitySource": "Schritte / Distanz / Kalorien", "connected": "Uhr verbunden", "waiting": "Uhr wird verbunden / erneut verbunden", "stopped": "Hintergrundverbindung deaktiviert", "autoNote": "Aktualisiert beim Öffnen mit vorhandener Berechtigung. Hintergrundaktualisierung separat einschalten."}, "ro": {"phone": "Date telefon", "dailySource": "Sursa zilnică pentru pași, distanță și calorii", "sourceAuto": "Sursa zilnică de pași cea mai completă (automat)", "sourcePriority": "Prioritatea surselor Health Connect", "sourceNote": "În modul automat se folosește sursa Health Connect cu cel mai mare total complet de pași zilnici pentru pași, distanță și calorii. Sursele nu se adună. Poți selecta în continuare o sursă explicită.", "bleBackground": "Păstrează ceasul conectat în fundal", "hcBackground": "Actualizează Health Connect în fundal", "backgroundNote": "Ceas: conexiune persistentă, pași la 5 minute, oprire din notificare. Health Connect: permisiune separată și actualizări periodice Android. Sistemul poate întârzia; redeschide după oprire forțată.", "backgroundUnavailable": "Citirea Health Connect în fundal nu este disponibilă pe acest sistem.", "summaryUnavailable": "Totalul sursei nu s-a actualizat. Alege până la 31 de zile și verifică permisiunile Health Connect.", "activitySource": "Pași / distanță / calorii", "connected": "Ceas conectat", "waiting": "Conectarea / reconectarea ceasului", "stopped": "Conexiune în fundal dezactivată", "autoNote": "Actualizează la deschidere cu permisiunile existente. Actualizarea în fundal se activează separat."}}))Object.assign(texts[language],extra);
16046:  for(const [language,extra] of Object.entries({"hu": {"dailySource": "Napi összesítés", "sourceAuto": "Automatikus", "sourcePriority": "Health Connect prioritás", "sourceNote": "Automatikusan a legteljesebb napi lépésszámú forrást használjuk. Az átfedő adatokat nem adjuk össze.", "backgroundNote": "Külön rendszerengedély szükséges. Az Android időszakosan frissít; kényszerleállítás után nyisd meg az appot."}, "en": {"dailySource": "Daily summary", "sourceAuto": "Automatic", "sourcePriority": "Health Connect priority", "sourceNote": "Automatically uses the most complete daily step source. Overlapping data is never added together.", "backgroundNote": "Requires separate system permission. Android refreshes periodically; reopen the app after force-stop."}, "de": {"dailySource": "Tagessumme", "sourceAuto": "Automatisch", "sourcePriority": "Health Connect Priorität", "sourceNote": "Verwendet automatisch die vollständigste tägliche Schrittquelle. Überlappende Daten werden nicht addiert.", "backgroundNote": "Separate Systemberechtigung erforderlich. Android aktualisiert regelmäßig; nach erzwungenem Stopp App öffnen."}, "ro": {"dailySource": "Total zilnic", "sourceAuto": "Automat", "sourcePriority": "Prioritatea Health Connect", "sourceNote": "Folosește automat sursa cu cei mai compleți pași zilnici. Datele suprapuse nu se adună.", "backgroundNote": "Necesită permisiune separată. Android actualizează periodic; redeschide aplicația după oprire forțată."}}))Object.assign(texts[language],extra);
16047:  for(const language of Object.keys(texts))texts[language].aggregate='Health Connect · '+texts[language].days;
16048:  const t=key=>(texts[typeof rf212Lang==='function'?rf212Lang():'hu']||texts.hu)[key]||window.TrainPilotSamsungHealth?.t(key)||key;
16049:  const bridge=()=>{if(window.Capacitor?.isNativePlatform?.()!==true)return null;return window.Capacitor?.Plugins?.HealthJournal||window.Capacitor?.registerPlugin?.('HealthJournal')||null;};
16050:  const background=()=>window.Capacitor?.Plugins?.HealthBackground||window.Capacitor?.registerPlugin?.('HealthBackground');
16051:  const root=()=>document.querySelector('#app .tp120-health-journal .tp120-journal');
16052:  const compact=ledger=>({...ledger,version:1,days:Object.fromEntries(Object.entries(ledger?.days||{}).sort(([a],[b])=>b.localeCompare(a)).slice(0,30))});
16053:  let initialized=null,cache=null,preferences={primary:'health_connect',autoOnOpen:false,logWatchSteps:false,hcDailySource:'auto',bleBackground:false,hcBackground:false},request=0,busy=false,autoRunning=false,lastAuto=0,nextCursor=0,nextTime=0,filters={from:null,to:null,};
16054:  const ledgerBase=rf240Ledger,saveBase=rf240SaveLedger;
16055:  rf240Ledger=function(){return cache||ledgerBase();};
16056:  rf240SaveLedger=function(value){if(!bridge())return saveBase(value);cache=compact(value);return saveBase(cache);};
16057:  function accept(value){cache=compact(value);preferences={...preferences,...value.preferences};saveBase(cache);rf245Recovery(cache);const recovered=rf229RecoveryHistory().filter(r=>Object.hasOwn(cache.days,r.day)).slice(0,30);for(const row of recovered){const legacy=cache.days[row.day]?.legacyRecovery;if(legacy)Object.assign(row,legacy);}db.set('recoveryHistory',recovered);rf229ApplyLatestRecovery();rf240ApplyLedger();return value;}
16058:  async function recover(p){
16059:   const pending=await p.pendingRestore();let marker;try{marker=JSON.parse(localStorage.getItem('repforge:healthRestore120')||'null');}catch(_){throw Error(t('failed'));}
16060:   if(pending?.token){await p.finishRestore({token:pending.token,commit:false});if(marker?.token===pending.token)for(const [k,v] of marker.old){if(v===null)localStorage.removeItem('repforge:'+k);else localStorage.setItem('repforge:'+k,v);}window.TrainPilotInvalidateStartupReads?.();}
16061:   localStorage.removeItem('repforge:healthRestore120');
16062:  }
16063:  async function ready(){
16064:   const p=bridge();if(!p)return null;
16065:   if(!initialized){initialized=(async()=>{if(window.TrainPilotRestoreRecovery)await window.TrainPilotRestoreRecovery;await recover(p);const current=await p.initialize({legacy:{...ledgerBase(),recoveryHistory:rf229RecoveryHistory()}});accept(current);if(window.TrainPilotBoot?.loading&&!window.TrainPilotRestore105Pending)window.TrainPilotBoot.finish();return p;})().catch(error=>{initialized=null;throw error;});}
16066:   return initialized;
16067:  }
16068:  async function refresh(){const p=await ready();if(p)accept(await p.getProjection());return cache;}
16069:  function legacySnapshot(data){const days=JSON.parse(JSON.stringify(data.healthLedger?.days||{}));for(const row of data.recoveryHistory||[]){if(!/^\d{4}-\d{2}-\d{2}$/.test(row.day))continue;const entry=days[row.day]||(days[row.day]={});entry.legacyRecovery={...row};if(entry.hrvRmssdMs==null&&row.hrvRmssdMs!=null)entry.hrvRmssdMs=row.hrvRmssdMs;}return {schemaVersion:1,records:[],days:Object.entries(days).map(([day,data])=>({day,channel:'legacy',source:'unknown',data})),deletions:[]};}
16070:  async function backup(){const data=makeBackup(),p=await ready();if(p&&tp105HealthConsent())data.healthJournal=await p.exportSnapshot();return tp105Project(data,tp105HealthConsent());}
16071:  for(const [lang,label] of Object.entries({hu:'Samsung energiapontszám',en:'Samsung Energy Score',de:'Samsung Energiepunkte',ro:'Scor de energie Samsung'}))texts[lang].energyScore=label;
16072:  const typeLabels={EnergyScoreRecord:'energyScore',SkeletalMuscleMass:'muscle',BodyFatMass:'fatMass',TotalBodyWater:'water',FatFreeMass:'lean',BasalMetabolicRate:'basal',StepsRecord:'steps',StepsSnapshot:'steps',SleepSessionRecord:'sleep',HeartRateRecord:'pulse',RestingHeartRateRecord:'resting',ActiveCaloriesBurnedRecord:'active',TotalCaloriesBurnedRecord:'total',DistanceRecord:'distance',SpeedRecord:'speed',WeightRecord:'weight',BodyFatRecord:'fat',BloodPressureRecord:'pressure',BloodGlucoseRecord:'glucose',RespiratoryRateRecord:'respiratory',HeartRateVariabilityRmssdRecord:'HRV',OxygenSaturationRecord:'SpO₂',Vo2MaxRecord:'VO₂max',ExerciseSessionRecord:'exercise'};
16073:  const format=(value,unit='')=>typeof value==='number'&&Number.isFinite(value)?tp149FormatNumber(value,{maximumFractionDigits:1})+(unit?' '+unit:''):'—';
16074:  function origin(channel,source,data={}){if(channel==='samsung_health')return window.TrainPilotSamsungHealth?.t('direct')||'Samsung Health';if(channel==='health_connect')return source==='@aggregate'?t('aggregate'):'Health Connect · '+({'com.sec.android.app.shealth':'Samsung Health','com.google.android.apps.fitness':'Google Fit'}[source]||(source.startsWith('com.android.healthconnect.phone.')?t('phone'):t('hc')));if(channel==='ble_watch')return t('watch')+(data.deviceName?' · '+data.deviceName:'');return t('legacy')+' · '+t('unknown');}
16075:  function recordHtml(row){
16076:   const data=row.data||{},samples=Array.isArray(data.samples)?data.samples:[];let value=data.steps??data.value,unit=data.unit||'';
16077:   if(samples.length){const valid=samples.map(x=>x.value).filter(x=>typeof x==='number'&&Number.isFinite(x));value=valid.length?valid.reduce((a,b)=>a+b,0)/valid.length:null;}
16078:   const reading=row.type==='BloodPressureRecord'?format(data.systolic)+' / '+format(data.diastolic,'mmHg'):format(value,unit);
16079:   return '<article class="tp120-record"><div><strong>'+esc(t(typeLabels[row.type]||row.type))+'</strong><b>'+esc(reading)+'</b></div><small>'+esc(tp149FormatDateTime(new Date(row.type==='SleepSessionRecord'?row.endMs:row.startMs)))+'</small><small class="muted">'+esc(row.channel==='samsung_health'?origin(row.channel,row.source,data):t('hc'))+'</small>'+(['health_connect','samsung_health'].includes(row.channel)?'<details class="tp120-origin-details"><summary>'+esc(t('source'))+'</summary><small>'+esc(origin(row.channel,row.source,data))+'</small><small class="muted">'+esc(row.source)+'</small></details>':'')+(samples.length?'<small>'+samples.length+' '+esc(t('samples'))+'</small>':'')+(data.dateBasis==='phone_observation'?'<small>'+esc(t('observed'))+'</small>':'')+(data.durationKind==='session'?'<small>'+esc(t('session'))+'</small>':'')+'</article>';
16080:  }
16081:  function dayHtml(day){
16082:   const metrics=[['energyScore',day.energyScore,'/100'],['steps',day.steps,''],['active',day.activeCalories,'kcal'],['total',day.totalCalories,'kcal'],['pulse',day.averageHeartRate,'bpm'],['resting',day.restingHeartRate,'bpm'],['distance',day.distanceMeters,'m'],['weight',day.weightKg,'kg'],['fat',day.bodyFatPercent,'%'],['muscle',day.skeletalMuscleMassKg,'kg'],['fatMass',day.bodyFatMassKg,'kg'],['water',day.totalBodyWaterLiters,'L'],['HRV',day.hrvRmssdMs,'ms'],['SpO₂',day.oxygenSaturationPercent,'%']].filter(([,value])=>value!=null);
16083:   const sessions=(day.sleepSessions||[]).filter(s=>rf240DayKey(s.end)===day.day),source=sessions.some(s=>s.source==='com.sec.android.app.shealth')?'com.sec.android.app.shealth':sessions.slice().sort((a,b)=>Date.parse(b.end)-Date.parse(a.end))[0]?.source;const spans=rf250MergeSpans(sessions.filter(s=>s.source===source).flatMap(rf250SleepSpans));if(spans.length)metrics.push(['sleep',spans.reduce((n,[a,b])=>n+(b-a)/60000,0),'min']);else if(day.legacyRecovery?.sleepMinutes!=null)metrics.push(['sleep',day.legacyRecovery.sleepMinutes,'min']);
16084:   const stale=day.partial||!day.readAt||Date.now()-Date.parse(day.readAt)>36*3600000;
16085:   return '<article class="tp120-day"><strong>'+esc(tp149FormatDate(new Date(day.day+'T12:00:00')))+'</strong><small>'+esc(origin(day.channel,day.source,day))+'</small><div class="tp120-metrics">'+metrics.map(([key,value,unit])=>'<div><small>'+esc(t(key))+'</small><b>'+esc(format(value,unit))+'</b>'+(day.channel==='samsung_health'&&day.metricProviders?.[key==='HRV'?'hrvRmssdMs':key]==='health_connect'?'<small>Health Connect</small>':'')+'</div>').join('')+'</div><small class="muted">'+esc(day.readAt?tp149FormatDateTime(day.readAt):'—')+(stale?' · '+esc(t(day.partial?'partial':'stale')):'')+'</small>'+(day.source==='@aggregate'&&day.activityOrigin?'<details class="tp120-origin-details"><summary>'+esc(t('source'))+'</summary><small>'+esc(origin('health_connect',day.activityOrigin))+'</small><small class="muted">'+esc(day.activityOrigin)+'</small></details>':'')+'</article>';
16086:  }
16087:  function dailySourceOptions(){return [['auto',t('sourceAuto')],['priority',t('sourcePriority')]].map(([value,label])=>'<option value="'+esc(value)+'" '+(preferences.hcDailySource===value?'selected':'')+'>'+esc(label)+'</option>').join('');}
16088:  function backgroundHtml(){return '<label class="tp120-check"><input type="checkbox" data-health-hcBackground '+(preferences.hcBackground?'checked':'')+' onchange="TrainPilotHealthJournal.backgroundPreference(\'hcBackground\',this.checked)"><span>'+esc(t('hcBackground'))+'</span></label><p class="small muted">'+esc(t('backgroundNote'))+'</p>';}
16089:  function html(){const today=rf240DayKey(new Date());return '<div class="tp120-journal"><div class="tp120-filters"><label>'+esc(t('from'))+'<input class="field" type="date" data-health-from value="'+(filters.from||today)+'" max="'+today+'" onchange="TrainPilotHealthJournal.load()"></label><label>'+esc(t('to'))+'<input class="field" type="date" data-health-to value="'+(filters.to||today)+'" max="'+today+'" onchange="TrainPilotHealthJournal.load()"></label></div><details class="card tp120-preferences"'+(root()?.querySelector('.tp120-preferences')?.open?' open':'')+'><summary>'+esc(t('settings'))+'</summary><label>'+esc(t('dailySource'))+'<select class="field" data-health-daily-source onchange="TrainPilotHealthJournal.preference(\'hcDailySource\',this.value)">'+dailySourceOptions()+'</select></label><p class="small muted">'+esc(t('sourceNote'))+'</p><label class="tp120-check"><input type="checkbox" data-health-auto '+(preferences.autoOnOpen?'checked':'')+' onchange="TrainPilotHealthJournal.preference(\'autoOnOpen\',this.checked)"><span>'+esc(t('auto'))+'</span></label><p class="small muted">'+esc(t('autoNote'))+'</p>'+backgroundHtml()+'</details><button type="button" class="btn secondary block" data-health-sync onclick="TrainPilotHealthJournal.sync()">'+esc(t('refresh'))+'</button><p data-health-status role="status" aria-live="polite"></p><section data-health-days></section><details class="card tp120-raw"><summary>'+esc(t('records'))+'</summary><section data-health-records></section><button type="button" class="btn secondary block" data-health-next hidden onclick="TrainPilotHealthJournal.load(true)">'+esc(t('next'))+'</button></details></div>';}
16090:  function feedback(message,error=false){const node=root()?.querySelector('[data-health-status]');if(!error&&message===t('saved')){window.TrainPilotFeedback?.success(message,node);return;}if(node){node.textContent=message;node.classList.toggle('tp120-error',error);}}
16091:  async function load(next=false){
16092:   const node=root();if(!node)return;const owner=++request;feedback(t('loading'));
16093:   try{const p=await ready();if(!p){feedback(t('native'));return;}const from=node.querySelector('[data-health-from]').value,to=node.querySelector('[data-health-to]').value;
16094:    filters={from,to};const channel='journal',source='';
16095:    const result=await p.readPage({from,to,channel,source,before:next?nextCursor:0,beforeTime:next?nextTime:0,limit:30});if(owner!==request||root()!==node)return;
16096:    const daily=node.querySelector('[data-health-daily-source]');if(daily){daily.innerHTML=dailySourceOptions();window.rf260SyncCustomSelect?.(daily);}
16097:    nextCursor=result.nextCursor||0;nextTime=result.nextTime||0;node.querySelector('[data-health-next]').hidden=!nextCursor;
16098:    const days=result.days||[],rows=result.records||[];node.querySelector('[data-health-days]').innerHTML=days.length?'<h3>'+esc(t('days'))+'</h3>'+days.map(dayHtml).join(''):'';node.querySelector('[data-health-records]').innerHTML=rows.length?rows.map(recordHtml).join(''):'';
16099:    feedback(result.sourceSummaryWarning?t('summaryUnavailable'):!days.length&&!rows.length?t('empty'):'',!!result.sourceSummaryWarning);void paintBackground();
16100:   }catch(error){if(owner===request&&root()===node)feedback(t('failed')+' '+(error?.message||''),true);}
16101:  }
16102:  async function preference(key,value){if(!['autoOnOpen','hcDailySource'].includes(key)||key==='hcDailySource'&&!['auto','priority'].includes(value))return;const p=await ready();if(!p)return;try{if(busy)throw Error(t('busy'));accept(await p.setPreferences({preferences:{[key]:value}}));feedback(t('saved'));if(key==='hcDailySource')await sync();}catch(error){feedback(error?.message||t('failed'),true);}finally{const input=root()?.querySelector(key==='hcDailySource'?'[data-health-daily-source]':'[data-health-auto]');if(input){if(key==='hcDailySource'){input.value=preferences[key];window.rf260SyncCustomSelect?.(input);}else input.checked=preferences[key];}}}
16103:  async function paintBackground(){const input=root()?.querySelector('[data-health-hcBackground]');if(input)input.checked=!!preferences.hcBackground;}
16104:  async function backgroundPreference(key,value){if(key!=='hcBackground')return;try{if(busy||window.TrainPilotBackupBusy)throw Error(t('busy'));const p=background();if(!p)throw Error(t('native'));if(key==='hcBackground'&&value){const hc=window.TrainPilotSamsungHealth?.healthConnectPlugin?.()||rf240Native(),status=await hc.getStatus();if(!status.backgroundSupported)throw Error(t('backgroundUnavailable'));if(!status.backgroundGranted){const result=await hc.requestBackground({language:rf212Lang()});if(!result.granted)throw Error(t('failed'));}}accept(await p.configure({channel:key,enabled:value,language:rf212Lang()}));feedback(t('saved'));}catch(error){feedback(error?.message||t('failed'),true);}finally{await refresh();void paintBackground();}}
16105:  async function sync(){if(busy){feedback(t('busy'));return;}busy=true;try{await ready();await rf250Sync({manual:true});await refresh();await load();}catch(error){feedback(t('failed'),true);}finally{busy=false;}}
16106:  async function auto(){
16107:   if(document.hidden||window.TrainPilotBackupBusy||autoRunning||state.session)return;
16108:   autoRunning=true;try{
16109:    if(Date.now()-lastAuto<10*60000){await refresh();window.TrainPilotUIRefresh.refresh();return;}
16110:    await ready();await refresh();if(!preferences.autoOnOpen)return;lastAuto=Date.now();
16111:    if(preferences.autoOnOpen){const p=rf240Native();if(p){const status=await p.getStatus();if(Object.entries(status.permissions||{}).some(([key,v])=>key.startsWith('READ_')&&v))await rf250Sync({plugin:p,manual:false});}}
16112:    window.TrainPilotUIRefresh.refresh();
16113:   }catch(_){/* Permission/network failures keep durable records and require no background dialogs. */}finally{autoRunning=false;}
16114:  }
16115:  function decorate(){
16116:   const home=document.querySelector('#app main.rf221-home');if(home&&!home.querySelector('.tp120-home-goals')){home.querySelector('.tp110-goal-mini')?.remove();const today=home.querySelector('#rf223Today'),button=document.createElement('button');button.type='button';button.className='btn secondary block tp120-home-goals';button.setAttribute('onclick',"tp155R4OpenPanel('goals',this)");const config=settings().activityGoals110||{},goal=config.weekWorkouts||config.monthKm?window.TrainPilot110Core.goalProgress(history(),config):null,row=goal?.week.target?goal.week:goal?.month.target?goal.month:null;button.innerHTML='<span>'+esc(window.TrainPilot110.t('setGoals'))+'</span>'+(row?'<small class="tp120-goal-progress">'+esc(tp149FormatNumber(row.current))+'/'+esc(tp149FormatNumber(row.target))+' '+esc(row===goal.week?window.TrainPilot110.t('units'):'km')+'</small>':'')+'<span class="tp120-chevron" aria-hidden="true"></span>';const oldSummary=home.querySelector(':scope > .grid2');if(oldSummary?.querySelector('.stat'))oldSummary.replaceWith(button);else if(today)today.after(button);else home.appendChild(button);}
16117:   const health=document.querySelector('#app main.tp168-health');if(health&&!health.querySelector('.tp120-health-journal')){
16118:    const disclosure=document.createElement('details');disclosure.className='card tp120-health-journal';
16119:    disclosure.dataset.language=rf212Lang();disclosure.dataset.provider=preferences.provider||'health_connect';
16120:    disclosure.innerHTML='<summary class="tp120-journal-entry"><span class="tp168-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M6 3h13a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1ZM3 7h4M3 12h4M3 17h4"/><path d="M12.5 16.5 9 13a2.3 2.3 0 0 1 3.5-3 2.3 2.3 0 0 1 3.5 3Z"/></svg></span><strong>'+esc(t('title'))+'</strong><span class="tp168-chevron tp-global-chevron" aria-hidden="true">›</span></summary><div data-health-journal-content></div>';
16121:    disclosure.addEventListener('toggle',()=>{if(disclosure.open&&!disclosure.querySelector('.tp120-journal')){disclosure.querySelector('[data-health-journal-content]').innerHTML=window.TrainPilotHealthJournal.html();rf260EnhanceSelects();rf260EnhanceTemporalFields();void load();}tp107FitViews();});
16122:    health.querySelector('.tp168-connect-panel')?.before(disclosure);
16123:   }
16124:  }
16125:  function open(){if(!document.querySelector('#app main.tp168-health'))go('health');const disclosure=document.querySelector('#app .tp120-health-journal');if(!disclosure)return false;disclosure.open=true;return true;}
16126:  window.TrainPilotHealthJournal={t,ready,refresh,backup,html,load,preference,backgroundPreference,sync,decorate,auto,compact,legacySnapshot,preferences:()=>({...preferences}),isSyncing:()=>busy||autoRunning||!!rf250Flight};
16127:  const pipelineBase=rf250Pipeline;rf250Pipeline=async function(p,options){const journal=await ready();if(journal){const dirty=await journal.getDirtyDays();options={...options,journalDirtyDays:dirty.days||[]};}const result=await pipelineBase(p,options);if(journal){await journal.saveSyncMeta({meta:Object.fromEntries(['lastSyncAt','lastFullSyncAt','lastAttemptAt'].map(key=>[key,rf240Ledger()[key]]))});await refresh();if(options?.manual)await load();}return result;};
16128:  window.addEventListener?.('DOMContentLoaded',()=>{
16129:   const renderBase=render;render=function(){
16130:    const retained=document.querySelector('#app .tp120-health-journal'),scroll=window.scrollY;
16131:    const result=renderBase.apply(this,arguments);decorate();const replacement=document.querySelector('#app .tp120-health-journal');
16132:    if(retained&&replacement&&retained.dataset.language===replacement.dataset.language&&retained.dataset.provider===replacement.dataset.provider){replacement.replaceWith(retained);}
16133:    else if(retained&&replacement&&retained.open)replacement.open=true;
16134:    // Common render fits once. Late Health decoration needs a follow-up only
16135:    // on Health routes, not another synchronous fit on the startup Home page.
16136:    if(document.querySelector('#app main.tp168-health'))tp107ScheduleFit();
16137:    if(retained?.open&&replacement)window.scrollTo(0,scroll);return result;
16138:   };window.render=render;
16139:   const openBase=window.tp155R4OpenPanel;window.tp155R4OpenPanel=function(type){return type==='health-journal'?open():openBase.apply(this,arguments);};
16140:   const refreshBase=window.tp155R4RefreshPanel;window.tp155R4RefreshPanel=function(){const result=refreshBase.apply(this,arguments);if(root())void load();return result;};
16141:   decorate();if(bridge())void ready().then(()=>{window.TrainPilotUIRefresh.refresh();setTimeout(auto,1500);}).catch(()=>{});
16142:   document.addEventListener('visibilitychange',()=>{if(!document.hidden)void auto();});
16143:  });
16144: })();
16145: // @endsection health-journal-120.js
16146: 
16147: // @section samsung-health-provider.js
16148: /* Read-only Samsung provider alongside the existing Health Connect path. */
16149: (function(){
16150:  'use strict';
16151:  if(!window.TrainPilotHealthJournal)return;
16152:  const HC='health_connect',SH='samsung_health',journal=()=>window.TrainPilotHealthJournal;
16153:  const words={
16154:   hu:{provider:'Egészségügyi adatforrás',direct:'Samsung Health · közvetlen',note:'A kiválasztott forrás az elsődleges. A hiányzó adatokat a már engedélyezett Health Connect egészítheti ki; az értékeket nem adjuk össze.',hrv:'A közvetlen Samsung-kapcsolat nem ad HRV-t. HRV csak akkor jelenik meg, ha a Health Connectben van megosztott RMSSD-adat.',body:'Samsung testösszetétel',muscle:'Vázizomtömeg',fatMass:'Testzsírtömeg',water:'Testvíz',lean:'Zsírmentes tömeg',basal:'Nyugalmi anyagcsere',busy:'Várd meg a futó szinkron végét.',denied:'Nem kaptunk Samsung Health olvasási engedélyt. A korábbi adatforrás maradt kiválasztva.',failed:'A Samsung Health kapcsolódás nem sikerült. Ellenőrizd a Samsung Health verzióját és a TrainPilot hozzáférését.',missing:'A közvetlen Samsung-kapcsolat ebben az appváltozatban nem érhető el.',permission:'Samsung Health engedélyek',auto:'Egészségadatok frissítése appmegnyitáskor'},
16155:   en:{provider:'Health data provider',direct:'Samsung Health · direct',note:'The selected provider takes priority. Missing data can be supplemented by already authorized Health Connect data; values are never added together.',hrv:'The direct Samsung connection does not provide HRV. HRV requires a shared RMSSD record in Health Connect.',body:'Samsung body composition',muscle:'Skeletal muscle',fatMass:'Fat mass',water:'Body water',lean:'Fat-free mass',basal:'Basal metabolic rate',busy:'Wait for the current sync.',denied:'Samsung Health read access was not granted. The previous provider remains selected.',failed:'Samsung Health connection failed. Check its version and TrainPilot access.',missing:'Direct Samsung Health is unavailable in this app version.',permission:'Samsung Health permissions',auto:'Refresh health data when opening the app'},
16156:   de:{provider:'Gesundheitsdatenquelle',direct:'Samsung Health · direkt',note:'Die gewählte Quelle hat Vorrang. Fehlende Daten können durch bereits freigegebene Health Connect Daten ergänzt werden; Werte werden nie addiert.',hrv:'Samsung liefert über diese direkte Verbindung keine HRV. Dafür ist ein geteilter RMSSD-Datensatz in Health Connect nötig.',body:'Samsung Körperzusammensetzung',muscle:'Skelettmuskelmasse',fatMass:'Fettmasse',water:'Körperwasser',lean:'Fettfreie Masse',basal:'Grundumsatz',busy:'Warte auf die laufende Synchronisierung.',denied:'Kein Samsung Health Lesezugriff. Die bisherige Quelle bleibt ausgewählt.',failed:'Samsung Health Verbindung fehlgeschlagen. Version und TrainPilot Zugriff prüfen.',missing:'Direktes Samsung Health ist in dieser App-Version nicht verfügbar.',permission:'Samsung Health Berechtigungen',auto:'Gesundheitsdaten beim Öffnen aktualisieren'},
16157:   ro:{provider:'Sursa datelor de sănătate',direct:'Samsung Health · direct',note:'Sursa selectată are prioritate. Datele lipsă pot fi completate din Health Connect deja autorizat; valorile nu se adună.',hrv:'Conexiunea directă Samsung nu oferă HRV. Este necesară o înregistrare RMSSD partajată în Health Connect.',body:'Compoziție corporală Samsung',muscle:'Masă musculară scheletică',fatMass:'Masă de grăsime',water:'Apă corporală',lean:'Masă fără grăsime',basal:'Metabolism bazal',busy:'Așteaptă sincronizarea curentă.',denied:'Accesul de citire Samsung Health nu a fost acordat. Sursa anterioară rămâne selectată.',failed:'Conectarea Samsung Health a eșuat. Verifică versiunea și accesul TrainPilot.',missing:'Samsung Health direct nu este disponibil în această versiune.',permission:'Permisiuni Samsung Health',auto:'Actualizează datele de sănătate la deschiderea aplicației'}
16158:  };
16159:  const t=key=>(words[typeof rf212Lang==='function'?rf212Lang():'hu']||words.hu)[key]||key;
16160:  const selected=()=>journal()?.preferences?.().provider||rf240Ledger()?.preferences?.provider||HC;
16161:  const hcPlugin=healthPlugin;
16162:  const samsung=()=>{if(!isNative())throw Error(t('missing'));const p=window.Capacitor?.Plugins?.SamsungHealth||window.Capacitor?.registerPlugin?.('SamsungHealth');if(!p)throw Error(t('missing'));return p;};
16163:  const hasRead=status=>Object.entries(status?.permissions||{}).some(([key,value])=>key.startsWith('READ_')&&value===true);
16164:  const numeric=value=>typeof value==='number'&&Number.isFinite(value);
16165:  async function hcRead(method,args){
16166:   try{const p=hcPlugin();if(!hasRead(await p.getStatus())||typeof p[method]!=='function')return null;return await p[method](args);}catch(_){return null;}
16167:  }
16168:  function mergeWorkout(direct,fallback){
16169:   const out={...direct,provider:SH,metricProviders:{}};
16170:   const groups=[['averageHeartRate','minHeartRate','maxHeartRate','heartRateSamples'],['steps'],['activeCalories'],['totalCalories'],['distanceMeters'],['averageSpeedMps','maxSpeedMps'],['exerciseSessions','exerciseSessionCount','exerciseMinutes']];
16171:   for(const fields of groups){const key=fields[0],available=Array.isArray(out[key])?out[key].length>0:numeric(out[key]);
16172:    const provider=available?SH:HC;
16173:    if(!available&&fallback)for(const field of fields)if(fallback[field]!=null)out[field]=fallback[field];
16174:    if(out[key]!=null)for(const field of fields)out.metricProviders[field]=provider;
16175:   }
16176:   if(!numeric(out.workoutCalories)&&numeric(fallback?.workoutCalories)){
16177:    for(const [key,value] of Object.entries(fallback))if(key==='workoutCalories'||key.startsWith('workoutEnergy'))out[key]=value;
16178:    out.workoutEnergyProvider=fallback.workoutEnergyProvider||HC;
16179:   }
16180:   if(numeric(out.workoutCalories))out.metricProviders.workoutCalories=out.workoutEnergyProvider||SH;
16181:   out.sources=[...new Set([...(direct.sources||[]),...(fallback?.sources||[])])];
16182:   out.sourceLabels={...(fallback?.sourceLabels||{}),...(direct.sourceLabels||{})};
16183:   if(fallback?.sources?.includes('com.sec.android.app.shealth')&&Object.values(out.metricProviders).includes(HC))out.sourceLabels['com.sec.android.app.shealth']=t('direct')+' / Health Connect';
16184:   // A failed SDK read remains visible even if an authorized fallback is available.
16185:   out.warnings=[...(direct.warnings||[])];
16186:   return out;
16187:  }
16188:  async function recovery(args){
16189:   const fallback=await hcRead('readRecovery',args)||{},start=Date.parse(args.start),end=Date.parse(args.end);
16190:   const sleeps=rf240UniqueSleep(rf240Ledger().days||{}).filter(s=>s.provider===SH&&Date.parse(s.end)>=start&&Date.parse(s.end)<end).sort((a,b)=>Date.parse(b.end)-Date.parse(a.end));
16191:   const sleep=sleeps[0];if(!sleep)return {...fallback,provider:SH};
16192:   const spans=rf250MergeSpans(rf250SleepSpans(sleep));
16193:   return {...fallback,provider:SH,sleepMinutes:spans.reduce((sum,[a,b])=>sum+(b-a)/60000,0),sleepStart:sleep.start,sleepEnd:sleep.end,sleepSource:sleep.source,sleepStageBased:!!sleep.stages?.length};
16194:  }
16195:  let adapter=null;
16196:  function plugin(){if(adapter)return adapter;const p=samsung();return adapter={
16197:   getStatus:()=>p.getStatus(),requestRead:()=>p.requestRead(),openSettings:()=>p.openSettings(),
16198:   readHealthDay:args=>p.readHealthDay(args),readWellness:args=>p.readWellness(args),
16199:   readRecovery:recovery,
16200:   readTrainingWindow:async args=>mergeWorkout(await p.readTrainingWindow(args),await hcRead('readTrainingWindow',args)),
16201:   readWorkout:async args=>adapter.readTrainingWindow(args)
16202:  };}
16203:  healthPlugin=function(){return selected()===SH?plugin():hcPlugin();};
16204:  async function choose(value){
16205:   if(![HC,SH].includes(value))return;
16206:   try{
16207:    if(journal()?.isSyncing?.()||state.health?.busy||window.TrainPilotBackupBusy)throw Error(t('busy'));
16208:    const bridge=await journal().ready();if(!bridge)throw Error(t('missing'));
16209:    if(value===SH){let granted=await samsung().getStatus();if(!hasRead(granted)||granted.permissions?.READ_ENERGY_SCORE===false)granted=await samsung().requestRead();if(!hasRead(granted))throw Error(t('denied'));}
16210:    await bridge.setPreferences({preferences:{provider:value}});await journal().refresh();
16211:    rfHistoryHealthCache?.clear();
16212:    const rows=history();for(const row of rows)if(row.health240)row.health240.syncedAt=null;db.set('history',rows);
16213:    adapter=null;render();await journal().sync();
16214:   }catch(error){state.health=state.health||{};state.health.message=error?.message||t('failed');render();}
16215:   finally{paintChoices();}
16216:  }
16217:  function choices(){return '<label>'+esc(t('provider'))+'<select class="field" data-health-provider onchange="TrainPilotSamsungHealth.choose(this.value)"><option value="'+HC+'" '+(selected()===HC?'selected':'')+'>Health Connect</option><option value="'+SH+'" '+(selected()===SH?'selected':'')+'>'+esc(t('direct'))+'</option></select></label><p class="small muted">'+esc(t('note'))+'</p>'+(selected()===SH?'<p class="small muted">'+esc(t('hrv'))+'</p>':'');}
16218:  function paintChoices(){for(const el of document.querySelectorAll('[data-health-provider]')){el.value=selected();el.disabled=!!state.health?.busy;window.rf260EnhanceSelect?.(el);window.rf260SyncCustomSelect?.(el);}}
16219:  const pipeline=rf250Pipeline;
16220:  rf250Pipeline=async function(p,options={}){
16221:   await journal().ready();
16222:    if(selected()!==SH)return pipeline(p,options);
16223:   const native=samsung(),report={provider:SH,startedAt:new Date().toISOString(),stages:[],days:0,workouts:0,exported:0,skipped:0,errors:[]};
16224:   let status=await native.getStatus();if(options.manual&&(!hasRead(status)||status.permissions?.READ_ENERGY_SCORE===false)){await native.requestRead();status=await native.getStatus();}
16225:   if(!hasRead(status))throw Error(t('denied'));
16226:   state.health.permissions=status.permissions;
16227:   const count=options.full||!db.get('samsungHealthLastFull',null)?30:2,days=rf240RecentDays(count).sort();
16228:   const startGenerationProvider=selected();
16229:   for(const [index,day] of days.entries()){
16230:    rf245Progress(t('direct')+': '+(index+1)+'/'+days.length);
16231:    try{const args=rf240DayBounds(day);await hcRead('readHealthDay',args);const result=await native.readHealthDay(args);
16232:     if(selected()!==startGenerationProvider)throw Error(t('busy'));
16233:     if(result.warnings?.length)report.errors.push(day+': '+result.warnings.join(' · '));else report.days++;
16234:    }catch(error){report.errors.push(day+': '+rf245Message(error));}
16235:   }
16236:   const bridge=await journal().ready(),at=new Date().toISOString();
16237:   await bridge.saveSyncMeta({meta:{lastAttemptAt:at,...(!report.errors.length?{lastSyncAt:at,...(count===30?{lastFullSyncAt:at}:{})}:{})}});
16238:   if(!report.errors.length&&count===30)db.set('samsungHealthLastFull',at);
16239:   await journal().refresh();if(options.manual)await journal().load();
16240:   report.stages.push({name:t('direct'),status:report.errors.length?'partial':'ok',detail:report.days+'/'+days.length});
16241:   for(const h of history().filter(rf240ValidWorkout).filter(h=>Date.parse(h.finished)>=Date.now()-30*86400000)){
16242:    try{if(await rf245ReadWorkout(plugin(),h,options.manual||h.health240?.provider!==SH))report.workouts++;}catch(error){report.errors.push('Workout: '+rf245Message(error));}
16243:   }
16244:   report.stages.push({name:'Health Connect export',status:'skipped',detail:'Samsung Health: read-only'});
16245:   report.finishedAt=at;report.status=report.errors.length?'partial':'ok';db.set(RF250_REPORT,report);db.set(RF245_REPORT,report);return report;
16246:  };
16247:  const apply=rf240ApplyLedger;
16248:  rf240ApplyLedger=function(){const ledger=apply();const body=state.health?.wellness;if(body){
16249:   for(const key of ['bodyFatMassKg','skeletalMuscleMassKg','fatFreeMassKg','totalBodyWaterLiters','basalMetabolicRateKcal','bodyMassIndex']){
16250:    const value=rf240Latest(ledger.days||{},key,null);body[key]=value?.value??null;
16251:   }
16252:   body.weightProvider=rf240Latest(ledger.days||{},'weightKg','weightTime')?.day;
16253:   body.weightProvider=body.weightProvider?ledger.days[body.weightProvider]?.metricProviders?.weightKg||HC:null;
16254:   db.set('wellnessLatest',body);
16255:  }return ledger;};
16256:  function decorate(){
16257:   const main=document.querySelector('#app main.tp168-health');
16258:   if(main){const connection=main.querySelector('.tp168-connect-panel');if(connection&&!connection.querySelector('[data-health-provider]')){const block=document.createElement('div');block.className='tp-health-provider';block.innerHTML=choices();connection.querySelector('summary')?.after(block);}
16259:   }
16260:   paintChoices();
16261:  }
16262:  const journalHtml=journal().html;journal().html=function(){return journalHtml().replace('<label>'+esc(journal().t('dailySource')),choices()+'<label>'+esc(journal().t('dailySource')));};
16263:  if(typeof rf110AutoHealthSync==='function'){
16264:   const auto=rf110AutoHealthSync;
16265:   rf110AutoHealthSync=function(options){return selected()===SH?journal().auto():auto(options);};
16266:  }
16267:  window.TrainPilotSamsungHealth={choose,selected,t,mergeWorkout,plugin,decorate,healthConnectPlugin:hcPlugin};
16268:  window.addEventListener?.('DOMContentLoaded',()=>{
16269:   const base=render;render=function(){const result=base.apply(this,arguments);decorate();return result;};window.render=render;
16270:   decorate();
16271:  });
16272: })();
16273: // @endsection samsung-health-provider.js
16274: 
16275: // @section health-details.js
16276: /* Dated metrics for six Today shortcuts and dynamically populated Health details. */
16277: (function(){
16278:  'use strict';
16279:  const labels={
16280:   daily:['Napi és egyéb adatok','Daily and other data','Tagesdaten und weitere Werte','Date zilnice și alte valori'],
16281:   body:['Testösszetétel','Body composition','Körperzusammensetzung','Compoziție corporală'],
16282:   emptyBody:['Még nincs testösszetételi mérés.','No body composition measurements yet.','Noch keine Messung der Körperzusammensetzung.','Nu există încă măsurători de compoziție corporală.'],
16283:   samples:['{n} pulzusminta','{n} heart-rate samples','{n} Pulsmesswerte','{n} măsurători de puls'],
16284:   source:['Adatforrás','Data source','Datenquelle','Sursa datelor'],
16285:   earlier:['Korábbi adat','Earlier data','Frühere Daten','Date anterioare'],
16286:   activeCalories:['Aktív kalória','Active calories','Aktive Kalorien','Calorii active'],
16287:   totalCalories:['Teljes kalória','Total calories','Gesamtkalorien','Calorii totale'],
16288:   muscle:['Vázizomtömeg','Skeletal muscle','Skelettmuskelmasse','Masă musculară scheletică'],
16289:   fatMass:['Testzsírtömeg','Fat mass','Fettmasse','Masă de grăsime'],
16290:   water:['Testvíz','Body water','Körperwasser','Apă corporală'],
16291:   lean:['Zsírmentes tömeg','Fat-free mass','Fettfreie Masse','Masă fără grăsime'],
16292:   basal:['Nyugalmi anyagcsere','Basal metabolic rate','Grundumsatz','Metabolism bazal'],
16293:   bmi:['BMI','BMI','BMI','IMC'],perDay:['kcal/nap','kcal/day','kcal/Tag','kcal/zi'],
16294:   min:['Min. pulzus','Min. heart rate','Min. Puls','Puls minim'],max:['Max. pulzus','Max. heart rate','Max. Puls','Puls maxim']
16295:  };
16296:  const language=()=>typeof rf212Lang==='function'?rf212Lang():'hu';
16297:  const text=key=>labels[key]?.[['hu','en','de','ro'].indexOf(language())]||labels[key]?.[0]||key;
16298:  const number=v=>v==null||v===''||!Number.isFinite(Number(v))?null:Number(v);
16299:  const definitions=[
16300:   ['steps','health.steps','',0,'daily','more','steps'],
16301:   ['activeCalories','activeCalories','kcal',0,'daily','more','calories'],
16302:   ['totalCalories','totalCalories','kcal',0,'daily','more','calories'],
16303:   ['distanceMeters','health.distance','km',2,'daily','more','steps',null,1000],
16304:   ['hrvRmssdMs','HRV','ms',0,'daily','more','hrv','hrvTime'],
16305:   ['bloodPressureSystolic','health.bloodPressure','mmHg',0,'daily','more','pulse','bloodPressureTime'],
16306:   ['bloodGlucoseMmolL','health.bloodGlucose','mmol/L',1,'daily','more','pulse','bloodGlucoseTime'],
16307:   ['respiratoryRate','health.respiratoryRate','/min',1,'daily','more','hrv','respiratoryRateTime'],
16308:   ['restingHeartRate','health.restingHeartRate','bpm',0,'pulse','pulse','pulse'],
16309:   ['minHeartRate','min','bpm',0,'pulse','pulse','pulse'],
16310:   ['maxHeartRate','max','bpm',0,'pulse','pulse','pulse'],
16311:   ['weightKg','health.weight','kg',1,'weight','weight','weight','weightTime'],
16312:   ['bodyFatPercent','health.bodyFat','%',1,'weight','weight','body','bodyFatTime'],
16313:   ['oxygenSaturationPercent','SpO₂','%',1,'weight','weight','pulse','oxygenSaturationTime'],
16314:   ['vo2Max','VO₂max','',1,'weight','weight','hrv','vo2MaxTime'],
16315:   ['skeletalMuscleMassKg','muscle','kg',1,'body','more','body','skeletalMuscleMassTime'],
16316:   ['bodyFatMassKg','fatMass','kg',1,'body','more','body','bodyFatMassTime'],
16317:   ['totalBodyWaterLiters','water','L',1,'body','more','body','totalBodyWaterTime'],
16318:   ['fatFreeMassKg','lean','kg',1,'body','more','body','fatFreeMassTime'],
16319:   ['basalMetabolicRateKcal','basal','perDay',0,'body','more','calories','basalMetabolicRateTime'],
16320:   ['bodyMassIndex','bmi','',1,'body','more','body','bodyMassIndexTime']
16321:  ];
16322:  function providerLabel(provider){return provider==='samsung_health'?'Samsung Health':provider==='health_connect'?'Health Connect':text('earlier');}
16323:  function rows(mode='details'){
16324:   const ledger=rf240Ledger(),days=ledger.days||{},today=rf240DayKey(new Date()),current=days[today]||{};
16325:   return definitions.map(([key,label,unit,digits,group,target,icon,time,divisor=1])=>{
16326:    const latest=mode==='today'||group==='pulse'||['steps','activeCalories','totalCalories','distanceMeters','hrvRmssdMs'].includes(key)?null:rf240Latest(days,key,time);
16327:    const rowDay=latest?.day||today,data=latest?days[rowDay]:current,value=number(data?.[key]);
16328:    const timeValue=data?.[time],measurementDay=(timeValue?rf240DayKey(timeValue):rowDay)||rowDay;
16329:    let valid=value!=null&&value>=0;
16330:    if(group==='pulse'||['weightKg','vo2Max','respiratoryRate','hrvRmssdMs','bloodPressureSystolic'].includes(key))valid=valid&&value>0;
16331:    let shown=valid?tp149FormatNumber(value/divisor,{maximumFractionDigits:digits})+(unit?' '+(unit==='perDay'?text(unit):unit):''):'—';
16332:    if(key==='bloodPressureSystolic'){
16333:     const diastolic=number(data?.bloodPressureDiastolic);valid=valid&&diastolic>0;
16334:     shown=valid?tp149FormatNumber(Math.round(value))+'/'+tp149FormatNumber(Math.round(diastolic))+' mmHg':'—';
16335:    }
16336:    if(key==='hrvRmssdMs'&&!valid){
16337:     const recovery=typeof rf229RecoveryHistory==='function'?rf229RecoveryHistory().find(r=>r.day===today):null;
16338:     const hrv=number(recovery?.hrvRmssdMs);if(hrv>0){valid=true;shown=tp149FormatNumber(Math.round(hrv))+' ms';}
16339:    }
16340:    if(mode==='today'&&measurementDay!==today)valid=false;
16341:    return {key,group,target,icon,label:label.startsWith('health.')?tp149T(label):labels[label]?text(label):label,
16342:     value,text:shown,valid,day:measurementDay,provider:data?.metricProviders?.[key]||(key==='hrvRmssdMs'?'health_connect':data?.provider||data?.channel||'health_connect')};
16343:   });
16344:  }
16345:  function meta(row){
16346:   if(!row.valid)return '';
16347:   const date=tp149FormatDate(new Date(row.day+'T12:00:00'),{month:'short',day:'numeric'});
16348:   return providerLabel(row.provider)+' · '+date;
16349:  }
16350:  function stat(row){return '<div class="stat'+(row.key==='hrvRmssdMs'?' tp5-more-hrv':'')+'" data-health-detail="'+row.key+'" tabindex="-1"><small>'+esc(row.label)+'</small><strong>'+esc(row.text)+'</strong>'+(row.valid?'<span class="tp-health-metric-meta">'+esc(meta(row))+'</span>':'')+'</div>';}
16351:  function decorate(more){
16352:   const all=rows(),bodyKeys=['weightKg','bodyFatPercent'],daily=all.filter(r=>(r.group==='daily'||['oxygenSaturationPercent','vo2Max'].includes(r.key))&&(r.valid||['bloodPressureSystolic','bloodGlucoseMmolL','respiratoryRate','distanceMeters','hrvRmssdMs'].includes(r.key))),body=all.filter(r=>(r.group==='body'||bodyKeys.includes(r.key))&&r.valid);
16353:   let content=more.querySelector(':scope > .tp-health-details');if(!content){more.querySelectorAll(':scope > .grid2').forEach(n=>n.remove());content=document.createElement('div');content.className='tp-health-details';more.appendChild(content);}
16354:   content.innerHTML='<section data-health-group="daily"><h3>'+esc(text('daily'))+'</h3><div class="grid2">'+daily.map(stat).join('')+'</div></section>'+
16355:    '<section class="tp-samsung-composition" data-health-group="body"><h3>'+esc(text('body'))+'</h3>'+(body.length?'<div class="grid2">'+body.map(stat).join('')+'</div>':'<p class="small muted">'+esc(text('emptyBody'))+'</p>')+'</section>';
16356:   const health=window.TrainPilot168Health;if(health){more.open=!!health.moreOpen;more.ontoggle=()=>{if(more.isConnected)health.moreOpen=more.open;};}
16357:  }
16358:  function open(key){
16359:   const row=rows().find(r=>r.key===key);if(!row)return;
16360:   if(row.target==='pulse')return window.tp168OpenPulse?.();
16361:   if(row.target==='weight')return window.rf215WeightScreen?.();
16362:   const more=document.querySelector('main.tp168-health .tp168-more-panel');if(!more)return;
16363:   more.open=true;if(window.TrainPilot168Health)window.TrainPilot168Health.moreOpen=true;
16364:   const target=more.querySelector('[data-health-detail="'+key+'"]')||more;
16365:   requestAnimationFrame(()=>{if(!target.isConnected)return;target.scrollIntoView?.({block:'center',behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'auto':'smooth'});target.focus?.({preventScroll:true});});
16366:  }
16367:  window.TrainPilotHealthDetails={rows,decorate,open,text,providerLabel,meta,stat};
16368: })();
16369: // @endsection health-details.js
16370: 
16371: 
```

## www/app.js — 20306–20398

Ref: `7473b30f387eda54186c1e31830304ca12bd104f`; teljes forrás SHA256: `47750fd38905860aa5910a14ff52a46421a3c78e19059c34cc5025da2538106d`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/app.js

```text
20306: // @section samsung-energy-access-184.js
20307: /* Samsung's daily score is an optional independent input, not a switch for
20308:  * the user's steps/calories provider. Automatic reads never open a consent UI. */
20309: (function(){
20310:  'use strict';
20311:  const words={
20312:   hu:{title:'Samsung energiapontszám',idle:'Az elérhető mai Samsung-pontszámot automatikusan használjuk. Ha nincs ilyen adat, marad a TrainPilot saját becslése.',permission:'Az energiapontszámhoz külön Samsung Health olvasási engedély szükséges.',empty:'Ma még nincs elérhető Samsung-pontszám. A TrainPilot saját becslése marad.',error:'A Samsung-pontszám olvasása nem sikerült. A TrainPilot saját becslése marad.',ready:'Mai Samsung Health adat. Ezt használjuk az ajánláshoz; fájdalom vagy nagy terhelés továbbra is korlátozhat.',loading:'Samsung-pontszám olvasása…',refresh:'Samsung-pontszám frissítése / engedélyezése',unavailable:'Ehhez a közvetlen Samsung-kapcsolatot támogató Android app szükséges.'},
20313:   en:{title:'Samsung Energy Score',idle:'Today’s available Samsung score is used automatically. Otherwise TrainPilot keeps its own estimate.',permission:'A separate Samsung Health read permission is required for Energy Score.',empty:'No Samsung score is available for today. TrainPilot keeps its own estimate.',error:'Samsung Energy Score could not be read. TrainPilot keeps its own estimate.',ready:'Today’s Samsung Health data is used for advice; pain or high load can still limit it.',loading:'Reading Samsung Energy Score…',refresh:'Refresh / allow Samsung Energy Score',unavailable:'This requires the Android app with direct Samsung support.'},
20314:   de:{title:'Samsung Energiepunkte',idle:'Der verfügbare heutige Samsung-Wert wird automatisch verwendet. Sonst bleibt die TrainPilot-Schätzung.',permission:'Für Energiepunkte ist eine separate Samsung Health Leseberechtigung erforderlich.',empty:'Heute ist kein Samsung-Wert verfügbar. Die TrainPilot-Schätzung bleibt.',error:'Samsung Energiepunkte konnten nicht gelesen werden. Die TrainPilot-Schätzung bleibt.',ready:'Heutiger Samsung Health Wert; Schmerz oder hohe Belastung können Empfehlungen weiter begrenzen.',loading:'Samsung Energiepunkte werden gelesen…',refresh:'Samsung Energiepunkte aktualisieren / erlauben',unavailable:'Hierfür wird die Android-App mit direkter Samsung-Unterstützung benötigt.'},
20315:   ro:{title:'Scor de energie Samsung',idle:'Scorul Samsung disponibil pentru azi este utilizat automat. Altfel rămâne estimarea TrainPilot.',permission:'Scorul de energie necesită o permisiune separată de citire Samsung Health.',empty:'Nu este disponibil un scor Samsung pentru azi. Rămâne estimarea TrainPilot.',error:'Scorul Samsung nu a putut fi citit. Rămâne estimarea TrainPilot.',ready:'Datele Samsung Health de azi sunt utilizate; durerea sau efortul ridicat pot limita recomandările.',loading:'Se citește scorul Samsung…',refresh:'Actualizează / permite scorul Samsung',unavailable:'Este necesară aplicația Android cu suport Samsung direct.'}
20316:  };
20317:  const t=key=>(words[typeof rf212Lang==='function'?rf212Lang():'hu']||words.hu)[key]||key;
20318:  let current=null,status='idle',attempted=false,flight=null,lastAttempt=0,lastDay='',failure='',recoveryOpen=false;
20319:  const today=()=>rf240DayKey(new Date());
20320:  const hints={
20321:   hu:{access:'A Samsung Health nem enged hozzáférést a TrainPilotnak ehhez az adathoz. Ez alkalmazásszintű hozzáférési korlát.',update:'Frissítsd a Samsung Health alkalmazást, majd próbáld újra.',open:'Nyisd meg a Samsung Health alkalmazást, és fejezd be a beállítását.',retry:'Nyisd meg a Samsung Health alkalmazást, majd próbáld újra.',detail:'Kapcsolati hibakód'},
20322:   en:{access:'Samsung Health does not allow TrainPilot to access this data. This is an app access restriction.',update:'Update Samsung Health, then try again.',open:'Open Samsung Health and complete its setup.',retry:'Open Samsung Health, then try again.',detail:'Connection error code'},
20323:   de:{access:'Samsung Health erlaubt TrainPilot keinen Zugriff auf diese Daten. Der App-Zugriff ist eingeschränkt.',update:'Samsung Health aktualisieren und erneut versuchen.',open:'Samsung Health öffnen und die Einrichtung abschließen.',retry:'Samsung Health öffnen und erneut versuchen.',detail:'Verbindungsfehlercode'},
20324:   ro:{access:'Samsung Health nu permite TrainPilot să acceseze aceste date. Accesul aplicației este restricționat.',update:'Actualizează Samsung Health și încearcă din nou.',open:'Deschide Samsung Health și finalizează configurarea.',retry:'Deschide Samsung Health și încearcă din nou.',detail:'Cod de eroare al conexiunii'}
20325:  };
20326:  const hint=key=>(hints[typeof rf212Lang==='function'?rf212Lang():'hu']||hints.hu)[key];
20327:  function fail(error){
20328:   const code=String(error?.code||error||'');
20329:   failure=/^SAMSUNG_HEALTH_(?:[0-9]{4}|UNAVAILABLE)$/.test(code)?code:'SAMSUNG_HEALTH_UNAVAILABLE';
20330:   status=failure==='SAMSUNG_HEALTH_2000'?'permission':failure==='SAMSUNG_HEALTH_2003'?'access':failure==='SAMSUNG_HEALTH_3001'?'update':/^SAMSUNG_HEALTH_300[023]$/.test(failure)?'open':'error';
20331:  }
20332:  function summary(){return ['access','update','open'].includes(status)?hint(status)+' '+t('error'):t(status==='ready'&&!tp184EnergyToday()?'empty':status);}
20333:  function html(){
20334:   const score=tp184EnergyToday();
20335:   return '<section class="card tp184-energy-card"><h3>'+esc(t('title'))+'</h3><p class="tp184-energy-value"><strong>'+esc(tp184FormatScore(score?.value))+'</strong></p><p class="small muted" role="status">'+esc(summary())+'</p>'+(failure?'<p class="small muted tp184-energy-error">'+esc(hint('detail')+': '+failure)+'</p>':'')+'<button type="button" class="btn secondary block" onclick="TrainPilotEnergy184.refresh({manual:true})" '+(flight?'disabled':'')+'>'+esc(t('refresh'))+'</button></section>';
20336:  }
20337:  function decorate(){
20338:   // Whole-page background refresh intentionally stops behind open disclosures.
20339:   // Update this read-only value in place, preserving panels, drafts and scroll.
20340:   const tiles=document.querySelectorAll('#app .tp5-today-recovery .tp5-today-value');
20341:   if(tiles.length&&typeof window.tp5UnifiedTodayModel==='function'){
20342:    const model=window.tp5UnifiedTodayModel();
20343:    tiles.forEach(node=>{node.textContent=model.values.recovery;});
20344:   }
20345:   const main=document.querySelector('#app main.tp168-health');if(!main)return;
20346:   const recovery=main.querySelector('.tp168-recovery-history');if(!recovery)return;
20347:   main.querySelectorAll('.tp184-energy-card').forEach(node=>node.remove());
20348:   recovery.querySelector(':scope > summary')?.insertAdjacentHTML('afterend',html());
20349:   if(recoveryOpen){recovery.open=true;main.querySelector('.tp169-pulse-journal')?.classList.add('tp169-open');main.querySelector('.tp169-pulse-summary')?.setAttribute('aria-expanded','true');}
20350:  }
20351:  function paint(){const panel=document.querySelector('.tp168-recovery-history');if(panel)recoveryOpen=panel.open;window.TrainPilotUIRefresh?.refresh?.();decorate();}
20352:  async function refresh(options={}){
20353:   const manual=options.manual===true;
20354:   if(document.hidden||window.TrainPilotBackupBusy||window.TrainPilotRestore105Pending||state.session||state.tp128Onboarding)return;
20355:   // A web-only page has no SDK. Do not redraw Home/onboarding just to
20356:   // announce an unavailable native provider.
20357:   if(!isNative()){status='unavailable';if(manual)decorate();return;}
20358:   if(flight)return manual?flight.then(()=>refresh({manual:true,force:true})):flight;
20359:   const day=today();if(!manual&&!options.force&&lastDay===day&&Date.now()-lastAttempt<10*60000)return;
20360:   lastAttempt=Date.now();lastDay=day;
20361:   flight=Promise.resolve().then(async()=>{
20362:    try{
20363:     if(!isNative()){status='unavailable';return;}
20364:     const p=window.Capacitor?.Plugins?.SamsungHealth||window.Capacitor?.registerPlugin?.('SamsungHealth');
20365:     if(!p){status='unavailable';return;}
20366:     failure='';status='loading';decorate();
20367:     const getStatus=()=>typeof p.getEnergyScoreStatus==='function'?p.getEnergyScoreStatus():p.getStatus();
20368:     let granted=await getStatus();
20369:     if(granted.permissions?.READ_ENERGY_SCORE!==true&&manual){
20370:      if(typeof p.requestEnergyScoreRead==='function')await p.requestEnergyScoreRead();else await p.requestRead();
20371:      granted=await getStatus();
20372:     }
20373:     attempted=true;current=null;
20374:     if(granted.permissions?.READ_ENERGY_SCORE!==true){status='permission';return;}
20375:     const result=await p.readEnergyScoreDay({...rf240DayBounds(day),day});
20376:     if(window.TrainPilotBackupBusy||window.TrainPilotRestore105Pending)return;
20377:     if(day!==today()||result.day!==day){status='empty';return;}
20378:     if(result.failedFields?.includes('energyScore')){fail(result.errors?.energyScore);return;}
20379:     current=result;status=tp184EnergyToday()?'ready':'empty';
20380:    }catch(error){attempted=true;current=null;fail(error);}
20381:    finally{flight=null;paint();}
20382:   });
20383:   return flight;
20384:  }
20385:  window.TrainPilotEnergy184={refresh,decorate,html,summary,reading:()=>current,hasResult:()=>attempted};
20386:  window.addEventListener?.('DOMContentLoaded',()=>{
20387:   const base=render;render=function(){const result=base.apply(this,arguments);decorate();return result;};window.render=render;
20388:   const syncBase=rf250Pipeline;rf250Pipeline=async function(){const result=await syncBase.apply(this,arguments);await refresh({force:true});return result;};
20389:   const recoveryBase=window.tp153OpenRecovery;
20390:   window.tp153OpenRecovery=function(){recoveryOpen=true;const result=recoveryBase?.apply(this,arguments);decorate();return result;};
20391:   if(window.TrainPilot168Health){const base=window.TrainPilot168Health.decorate;window.TrainPilot168Health.decorate=function(){const result=base.apply(this,arguments);decorate();return result;};}
20392:   decorate();void refresh();
20393:   document.addEventListener('visibilitychange',()=>{if(!document.hidden)void refresh();});
20394:  });
20395: })();
20396: // @endsection samsung-energy-access-184.js
20397: 
20398: 
```

## www/index.html — 140–360

Ref: `7473b30f387eda54186c1e31830304ca12bd104f`; teljes forrás SHA256: `9da5d0ef8746b53afe293d6ddd6e4238aa9953955740ff0e0a0687a9b7794ebb`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/index.html

```text
140:   weight:'<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 7.5A3.5 3.5 0 0 1 8.5 4h7A3.5 3.5 0 0 1 19 7.5V20H5V7.5Z"/><path d="M9 8.5a3 3 0 0 1 6 0M12 8.5l1.6-1.6"/></svg>'
141:  };
142: 
143:  const lang=()=>{try{return typeof rf212Lang==='function'?rf212Lang():(document.documentElement.lang||'hu').slice(0,2)}catch(_){return 'hu'}};
144:  const c=()=>COPY[lang()]||COPY.hu;
145:  const locale=()=>{try{return typeof rf233Locale==='function'?rf233Locale():({hu:'hu-HU',en:'en-US',de:'de-DE',ro:'ro-RO'}[lang()]||'hu-HU')}catch(_){return 'hu-HU'}};
146:  const escText=v=>String(v==null?'':v).replace(/[&<>"']/g,ch=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
147:  const n=v=>{v=Number(v);return Number.isFinite(v)?v:null};
148:  const positive=v=>{const x=n(v);return x!=null&&x>0?x:null};
149:  const fmt=v=>{if(v==null)return '—';try{return typeof tp149FormatNumber==='function'?tp149FormatNumber(v):new Intl.NumberFormat(locale()).format(v)}catch(_){return String(v)}};
150:  const ledger=()=>{try{return typeof rf240Ledger==='function'?rf240Ledger():{days:{}}}catch(_){return {days:{}}}};
151:  const day=()=>{try{const l=ledger(),k=typeof rf240DayKey==='function'?rf240DayKey(new Date()):new Date().toISOString().slice(0,10);return l.days?.[k]||{}}catch(_){return {}}};
152:  const rec=()=>state?.health?.recovery||{};
153: 
154:  function icon(name,extra=''){return '<span class="tp168-icon '+extra+'">'+(ICON[name]||ICON.health)+'</span>'}
155:  function metric(kind,label,value,action){const legacy=action==='recovery'?' data-tp153-link="recovery"':'';return '<div class="stat tp168-metric tp168-'+kind+'"'+legacy+(action?' role="button" tabindex="0" data-tp168-action="'+action+'"':'')+'>'+icon(kind)+'<span>'+escText(label)+'</span><strong>'+escText(value)+'</strong></div>'}
156:  function sleepText(r){try{return typeof tp149SleepText==='function'?tp149SleepText(r.sleepMinutes):(r.sleepMinutes==null?'—':(Math.round(r.sleepMinutes/6)/10)+' h')}catch(_){return '—'}}
157:  function readiness(){try{return typeof rf220Readiness==='function'?rf220Readiness():{score:null,level:'unknown'}}catch(_){return {score:null,level:'unknown'}}}
158:  function readinessLabel(r){if(r.score==null)return c().noData;try{const t=typeof rf220L==='function'?rf220L():{};return t[r.level]||r.level||''}catch(_){return ''}}
159:  function trendValues(){const days=ledger()?.days||{};return rf240RecentDays(7).reverse().map(k=>positive(days[k]?.averageHeartRate)).filter(v=>v!=null)}
160:  function smoothPath(pts){
161:   if(!pts||pts.length<2)return '';
162:   let d='M'+pts[0][0]+' '+pts[0][1];
163:   for(let i=1;i<pts.length;i++){
164:    const p0=pts[i-1],p1=pts[i],mx=((Number(p0[0])+Number(p1[0]))/2).toFixed(1);
165:    d+=' C'+mx+' '+p0[1]+' '+mx+' '+p1[1]+' '+p1[0]+' '+p1[1];
166:   }
167:   return d;
168:  }
169:  function curvePoints(values,w,h,p,minSpan){
170:   const a=(values||[]).map(Number).filter(Number.isFinite);if(a.length<2)return [];
171:   const mn=Math.min(...a),mx=Math.max(...a),span=Math.max(minSpan||1,mx-mn);
172:   return a.map((v,i)=>[(p+i*(w-2*p)/Math.max(1,a.length-1)).toFixed(1),(h-p-(v-mn)*(h-2*p)/span).toFixed(1)]);
173:  }
174:  function sparkline(values){
175:   const pts=curvePoints(values,132,34,3,1);if(pts.length<2)return '<div class="tp168-spark-empty">•••</div>';
176:   const d=smoothPath(pts),last=pts[pts.length-1];
177:   return '<svg class="tp168-spark tp169-smooth-chart" viewBox="0 0 132 34" preserveAspectRatio="none" aria-hidden="true"><path d="'+d+'"/><circle cx="'+last[0]+'" cy="'+last[1]+'" r="2.6"/></svg>';
178:  }
179:  function largeCurve(values,kind){
180:   const pts=curvePoints(values,300,108,10,kind==='weight'?.1:1);if(pts.length<2)return '<div class="tp168-weight-empty">'+escText(c().noData)+'</div>';
181:   const d=smoothPath(pts),last=pts[pts.length-1],cls=kind==='weight'?'tp169-weight-curve':'tp169-pulse-curve';
182:   return '<svg class="'+cls+' tp169-smooth-chart" viewBox="0 0 300 108" preserveAspectRatio="none" aria-hidden="true"><path d="'+d+'"/><circle cx="'+last[0]+'" cy="'+last[1]+'" r="3"/></svg>';
183:  }
184:  function dateText(){try{return new Intl.DateTimeFormat(locale(),{year:'numeric',month:'short',day:'numeric'}).format(new Date())}catch(_){return new Date().toLocaleDateString()}}
185:  function weightRows(){try{return typeof rf215All==='function'?rf215All():(weights()||[]).filter(x=>x&&x.date&&Number.isFinite(Number(x.kg))).slice().sort((a,b)=>new Date(a.date)-new Date(b.date))}catch(_){return []}}
186:  function weightRange(){try{return typeof rf215Range==='function'?rf215Range():(state.weightRange||'30')}catch(_){return '30'}}
187:  function weightData(){try{return typeof rf215Data==='function'?rf215Data():weightRows()}catch(_){return weightRows()}}
188:  function weightSparkline(values){const a=(values||[]).map(x=>n(x.kg)).filter(v=>v!=null),pts=curvePoints(a,132,34,3,.1);if(pts.length<2)return '<div class="tp168-weight-spark-empty">•••</div>';const d=smoothPath(pts),last=pts[pts.length-1];return '<svg class="tp168-weight-spark tp169-smooth-chart" viewBox="0 0 132 34" preserveAspectRatio="none" aria-hidden="true"><path d="'+d+'"/><circle cx="'+last[0]+'" cy="'+last[1]+'" r="2.6"/></svg>'}
189:  function captureBody(card){
190:   if(!card)return window.TrainPilot168Health?.bodyHtml||'';
191:   const clone=card.cloneNode(true),source=clone.querySelector(':scope > .tp168-collapse-content')||clone;
192:   source.querySelectorAll('h2,.rf251-weight-btn,#rf251WeightJournal,.tp168-row-head').forEach(x=>x.remove());
193:   source.querySelectorAll('button').forEach(x=>{if(/Testsúly\s*(napló|naplo)|Body weight log|Gewichtsprotokoll|Jurnal greutate/i.test(String(x.textContent||'')))x.remove()});
194:   source.querySelectorAll('.stat').forEach(x=>{const label=String(x.querySelector('small')?.textContent||'').trim();if(/^(Testsúly|Body weight|Körpergewicht|Greutate)$/i.test(label))x.remove()});
195:   const html=source.innerHTML.trim();if(html)window.TrainPilot168Health.bodyHtml=html;return html||window.TrainPilot168Health?.bodyHtml||'';
196:  }
197:  function recoveryHistoryHtml(){
198:   let rows=[];try{rows=typeof rf229RecoveryHistory==='function'?rf229RecoveryHistory().slice(0,7):[]}catch(_){}
199:   if(!rows.length)return '<div class="tp169-recovery-empty">'+escText(c().noData)+'</div>';
200:   return rows.map(x=>{
201:    let dayText=String(x.day||'');try{dayText=new Date(dayText+'T12:00:00').toLocaleDateString(locale(),{year:'numeric',month:'short',day:'numeric'})}catch(_){}
202:    const sl=x.sleepMinutes==null?'—':sleepText({sleepMinutes:x.sleepMinutes}),hrv=x.hrvRmssdMs==null?'—':fmt(Math.round(Number(x.hrvRmssdMs)))+' ms';
203:    return '<div class="tp169-recovery-row"><strong>'+escText(dayText)+'</strong><span><b>'+escText(c().sleep)+'</b>: '+escText(sl)+' <i>•</i> <b>'+escText(c().hrv)+'</b>: '+escText(hrv)+'</span></div>';
204:   }).join('');
205:  }
206:  function pulseJournal(d,trend,historyHtml){
207:   const open=!!window.TrainPilot168Health?.pulseOpen,vals=[[c().avg,positive(d.averageHeartRate)],[c().resting,positive(d.restingHeartRate)],[c().max,positive(d.maxHeartRate)],[c().min,positive(d.minHeartRate)]];
208:   return '<section class="tp169-pulse-journal '+(open?'tp169-open':'')+'"><button type="button" class="tp168-trend tp169-pulse-summary" aria-expanded="'+String(open)+'" onclick="tp168TogglePulse()">'+icon('trend')+'<span><strong>'+escText(c().trend)+'</strong><small>'+escText(trend.length>=3?c().trendEnough:c().trendFew)+'</small></span>'+sparkline(trend)+'<b aria-hidden="true">›</b></button><div class="tp169-pulse-body">'+
209:    '<div class="tp168-pulse-grid">'+vals.map(x=>'<div><small>'+escText(x[0])+'</small><strong>'+escText(x[1]==null?'—':fmt(Math.round(x[1]))+' bpm')+'</strong></div>').join('')+'</div>'+
210:    '<div class="tp169-pulse-chart">'+largeCurve(trend,'pulse')+'</div>'+
211:    '<details class="tp168-recovery-history"><summary><strong>'+escText(c().recoveryHistory)+'</strong><span class="tp168-chevron" aria-hidden="true">›</span></summary>'+String(historyHtml||'')+'</details>'+
212:   '</div></section>';
213:  }
214:  function weightJournal(bodyHtml){
215:   const healthWeight=window.TrainPilotHealthDetails?.rows().find(row=>row.key==='weightKg'&&row.valid);
216:   const a=weightRows(),d=weightData(),cur=healthWeight?.value??a.at(-1)?.kg??null,start=a[0]?.kg??null,mn=a.length?Math.min(...a.map(x=>Number(x.kg))):null,mx=a.length?Math.max(...a.map(x=>Number(x.kg))):null,diff=cur!=null&&start!=null?Math.round((cur-start)*10)/10:null,open=!!window.TrainPilot168Health?.weightOpen;
217:   if(healthWeight)bodyHtml='<div class="tp-health-weight-reading">'+window.TrainPilotHealthDetails.stat(healthWeight)+'</div>'+(bodyHtml||'');
218:   const diffText=diff==null?'—':(diff>0?'+':'')+fmt(diff)+' kg',range=weightRange();
219:   return '<section class="tp168-weight-journal '+(open?'tp168-open':'')+'"><button type="button" class="tp168-weight-summary" aria-expanded="'+String(open)+'" onclick="tp168ToggleWeight()">'+icon('weight')+'<span><strong>'+escText(c().weightLog)+'</strong><small>'+escText(cur==null?c().weightLogSub:fmt(cur)+' kg · '+c().weightLogSub)+'</small></span>'+weightSparkline(d)+'<b aria-hidden="true">›</b></button><div class="tp168-weight-body">'+
220:    (bodyHtml?'<div class="tp169-body-inline"><div class="tp169-inline-heading"><strong>'+escText(c().body)+'</strong><small>'+escText(c().bodySub)+'</small></div>'+bodyHtml+'</div>':'')+
221:    '<div class="tp168-weight-stats"><div><small>'+escText(c().change)+'</small><strong>'+escText(diffText)+'</strong></div><div><small>'+escText(c().minMax)+'</small><strong>'+escText(mn==null?'—':fmt(mn)+' / '+fmt(mx)+' kg')+'</strong></div></div>'+
222:    '<details class="tp169-weight-entry"><summary><strong>+ '+escText(c().newMeasurement)+'</strong><span class="tp168-chevron" aria-hidden="true">›</span></summary><div class="tp169-weight-entry-body"><div class="tp168-weight-form"><label>'+escText(c().weight)+'<input id="tp168WeightKg" class="field" type="number" inputmode="decimal" min="30" max="300" step="0.1" value="'+escText(cur??'')+'"></label><label>'+escText(c().date)+'<input id="tp168WeightDate" class="field" type="text" inputmode="numeric" pattern="[0-9]{4}-[0-9]{2}-[0-9]{2}" value="'+escText(typeof rf215Date==='function'?rf215Date():new Date().toISOString().slice(0,10))+'"></label><button class="btn tp168-weight-save" type="button" onclick="tp168SaveWeight()">'+escText(c().save)+'</button></div></div></details>'+
223:    '<div class="tp168-weight-range">'+[['7','7d'],['30','30d'],['90','3m'],['365','1y'],['all','All']].map(x=>'<button type="button" class="'+(range===x[0]?'active':'')+'" onclick="tp168SetWeightRange(\''+x[0]+'\')">'+x[1]+'</button>').join('')+'</div>'+
224:    '<div class="tp168-weight-chart">'+largeCurve(d.map(x=>Number(x.kg)),'weight')+'</div>'+
225:    (a.length?'<div class="tp168-weight-recent"><small>'+escText(c().recent)+'</small>'+a.slice().reverse().slice(0,3).map(x=>'<div><span><strong>'+escText(fmt(x.kg))+' kg</strong><small>'+escText(new Date(x.date).toLocaleDateString(locale()))+'</small></span><span><button type="button" onclick="tp168EditWeight(\''+escText(x.date)+'\')">'+escText(c().edit)+'</button><button type="button" class="danger" onclick="tp168DeleteWeight(\''+escText(x.date)+'\')">'+escText(c().del)+'</button></span></div>').join('')+'</div>':'<div class="tp168-weight-empty">'+escText(c().noData)+'</div>')+
226:   '</div></section>';
227:  }
228:  window.tp168ToggleWeight=function(){
229:   const health=window.TrainPilot168Health,next=!health.weightOpen,panel=document.querySelector('main.rf263-health .tp168-weight-journal');
230:   health.weightOpen=next;
231:   if(!panel){refreshHealth();return}
232:   panel.classList.toggle('tp168-open',next);
233:   panel.querySelector('.tp168-weight-summary')?.setAttribute('aria-expanded',String(next));
234:   if(next)requestAnimationFrame(()=>{if(panel.isConnected)panel.scrollIntoView?.({block:'nearest',behavior:'smooth'})});
235:  };
236:  window.tp168SetWeightRange=function(v){state.weightRange=v;window.TrainPilot168Health.weightOpen=true;refreshHealth()};
237:  window.tp168SaveWeight=function(){const kg=Number(String(document.querySelector('#tp168WeightKg')?.value||'').replace(',','.')),raw=String(document.querySelector('#tp168WeightDate')?.value||'').trim(),fallback=typeof rf215Date==='function'?rf215Date():new Date().toISOString().slice(0,10),d=/^\d{4}-\d{2}-\d{2}$/.test(raw)?raw:fallback;if(!kg||kg<30||kg>300)return alert('30–300 kg');const w=weights();w.unshift({kg:Math.round(kg*10)/10,date:new Date(d+'T12:00:00').toISOString()});db.set('weights',w);window.TrainPilot168Health.weightOpen=true;refreshHealth()};
238:  window.tp168EditWeight=function(date){window.TrainPilot168Health.weightOpen=true;return typeof rf215Edit==='function'?rf215Edit(date):undefined};
239:  window.tp168DeleteWeight=function(date){window.TrainPilot168Health.weightOpen=true;return typeof rf215Del==='function'?rf215Del(date):undefined};
240:  // Keep the already decorated 7-day trend subtree mounted during disclosure.
241:  // Rebuilding Today here briefly exposes the legacy pulse chart before Phase 5 decoration.
242:  window.tp168TogglePulse=function(){
243:   const health=window.TrainPilot168Health,next=!health.pulseOpen,panel=document.querySelector('main.rf263-health .tp169-pulse-journal');
244:   health.pulseOpen=next;
245:   if(!panel){refreshHealth();return}
246:   panel.classList.toggle('tp169-open',next);
247:   panel.querySelector('.tp169-pulse-summary')?.setAttribute('aria-expanded',String(next));
248:   if(next)requestAnimationFrame(()=>{if(panel.isConnected)panel.scrollIntoView?.({block:'nearest',behavior:'smooth'})});
249:  };
250:  window.tp168OpenPulse=function(){
251:   if(!window.TrainPilot168Health.pulseOpen)return window.tp168TogglePulse();
252:   const panel=document.querySelector('main.rf263-health .tp169-pulse-journal');
253:   if(panel)panel.scrollIntoView?.({block:'center',behavior:'smooth'});
254:   else refreshHealth();
255:  };
256:  const recoveryBase=window.tp153OpenRecovery;
257:  window.tp153OpenRecovery=function(){
258:   const panel=document.querySelector('main.rf263-health .tp169-pulse-journal');
259:   window.TrainPilot168Health.pulseOpen=true;
260:   if(panel){panel.classList.add('tp169-open');panel.querySelector('.tp169-pulse-summary')?.setAttribute('aria-expanded','true')}
261:   else refreshHealth();
262:   setTimeout(()=>{
263:    const nested=document.querySelector('.tp169-pulse-journal .tp168-recovery-history');
264:    if(nested){nested.open=true;if(nested.scrollIntoView)nested.scrollIntoView({block:'center',behavior:'smooth'});return}
265:    if(typeof recoveryBase==='function')recoveryBase.apply(this,arguments);
266:   },0);
267:  };
268:  function disclosureSummary(details,name,subtitle,which){
269:   const summary=details?.querySelector(':scope > summary');if(!summary)return;
270:   summary.classList.add('tp168-row-head');
271:   summary.innerHTML=icon(which)+'<span class="tp168-row-copy"><strong>'+escText(name)+'</strong><small>'+escText(subtitle)+'</small></span><span class="tp168-chevron" aria-hidden="true">›</span>';
272:  }
273:  function decorateBody(card){
274:   if(!card)return;card.classList.add('tp168-body-card');
275:   const h=card.querySelector(':scope > h2');if(!h)return;
276:   const nodes=[...card.childNodes].filter(x=>x!==h),head=document.createElement('div'),content=document.createElement('div');
277:   head.className='tp168-row-head tp168-body-head';head.setAttribute('role','button');head.setAttribute('tabindex','0');head.setAttribute('aria-expanded',String(!!window.TrainPilot168Health.bodyOpen));
278:   head.innerHTML=icon('body')+'<span class="tp168-row-copy"></span><span class="tp168-chevron" aria-hidden="true">›</span>';
279:   const copy=head.querySelector('.tp168-row-copy');copy.appendChild(h);const sub=document.createElement('small');sub.textContent=c().bodySub;copy.appendChild(sub);
280:   content.className='tp168-collapse-content';nodes.forEach(x=>content.appendChild(x));card.prepend(content);card.prepend(head);
281:   if(window.TrainPilot168Health.bodyOpen)card.classList.add('tp168-open');
282:   head.onclick=()=>window.tp168ToggleBody(head);head.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();window.tp168ToggleBody(head)}};
283:  }
284:  function decoratePulse(details,d){
285:   if(!details||details.classList.contains('tp168-pulse-panel'))return;details.classList.add('tp168-pulse-panel');
286:   const summary=details.querySelector(':scope > summary'),original=[...details.childNodes].filter(x=>x!==summary),nested=document.createElement('details');
287:   nested.className='tp168-recovery-history';nested.innerHTML='<summary><strong>'+escText(c().recoveryHistory)+'</strong></summary>';original.forEach(x=>nested.appendChild(x));
288:   const pulse=document.createElement('div');pulse.className='tp168-pulse-grid';
289:   const vals=[[c().avg,positive(d.averageHeartRate)],[c().resting,positive(d.restingHeartRate)],[c().max,positive(d.maxHeartRate)],[c().min,positive(d.minHeartRate)]];
290:   pulse.innerHTML=vals.map(x=>'<div><small>'+escText(x[0])+'</small><strong>'+escText(x[1]==null?'—':fmt(Math.round(x[1]))+' bpm')+'</strong></div>').join('');
291:   details.appendChild(pulse);details.appendChild(nested);disclosureSummary(details,c().pulse,c().pulseSub,'pulse');
292:  }
293:  function healthHeader(main){
294:   const h=document.createElement('header');h.className='tp168-health-head';
295:   h.innerHTML='<div><h1>'+escText(c().title)+'</h1><p>'+escText(c().subtitle)+'</p></div><div class="tp168-date"><span>'+escText(c().todayWord)+'</span><strong>'+escText(dateText())+'</strong></div>';
296:   main.prepend(h);
297:  }
298:  function rebuildToday(card,d,r,bodyHtml,pulseHistory){
299:   if(!card)return;card.classList.add('tp168-today-card');card.classList.remove('tp146-health-compact');
300:   const trend=trendValues();
301:   card.innerHTML='<div class="tp168-today-head">'+icon('health','tp168-health-heart')+'<div class="tp168-today-copy"><div class="tp169-today-titleline"><h2>'+escText(c().today)+'</h2><time class="tp168-today-date" datetime="'+escText(new Date().toISOString().slice(0,10))+'">'+escText(dateText())+'</time></div><p>'+escText(c().todaySub)+'</p></div></div>'+
302:    window.tp5UnifiedTodayGridHtml('health')+
303:    pulseJournal(d,trend,recoveryHistoryHtml())+weightJournal(bodyHtml);
304:   card.querySelectorAll('[data-tp168-action]').forEach(el=>{const run=()=>el.dataset.tp168Action==='recovery'?window.tp153OpenRecovery?.():window.tp168OpenPulse?.();el.onclick=run;el.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();run()}}});
305:  }
306:  // All entry points resolve the completed pulse/chevron pipeline at call time.
307:  // Scheduling the original decorator alone exposed the legacy design between tasks.
308:  const refreshHealth=()=>window.TrainPilot168Health?.decorate?.();
309:  function decorate(){
310:   const main=document.querySelector('main.rf263-health');
311:   const bodyClass=document.body&&document.body.classList;if(bodyClass){if(main)bodyClass.add('tp168-health-view');else bodyClass.remove('tp168-health-view');}
312:   unifyChevrons(document);
313:   if(!main)return;
314:   main.classList.add('tp168-health');
315:   main.querySelectorAll(':scope > .tp168-health-head,:scope > .tp168-health-note').forEach(x=>x.remove());
316: 
317:   const cards=[...main.querySelectorAll(':scope > .tp151-health-card')],today=cards[0],body=cards[1],d=day(),r=rec();
318:   const details=[...main.querySelectorAll(':scope > details.tp155-health-bottom-panel')];
319:   let more=details.find(x=>x.dataset.healthPanel==='more'||x.classList.contains('tp168-more-panel')||/További|More\b|Weitere|Mai multe/i.test(String(x.querySelector(':scope > summary')?.textContent||'')))||null;
320:   let recovery=details.find(x=>/Regenerációs|Recovery history|Regeneration|recuperare/i.test(String(x.querySelector(':scope > summary')?.textContent||'')))||null;
321:   let diag=details.find(x=>x.id==='rf263HealthDiagnostics')||null;
322: 
323:   const bodyHtml=captureBody(body);
324:   rebuildToday(today,d,r,bodyHtml);
325:   if(body)body.remove();
326:   if(recovery)recovery.remove();
327: 
328:   if(more){
329:    more.classList.add('tp168-more-panel');disclosureSummary(more,c().more,c().moreSub,'more');
330:    window.TrainPilotHealthDetails?.decorate(more);
331:   }
332:   if(diag){diag.classList.add('tp168-connect-panel');disclosureSummary(diag,c().connect,c().connectSub,'link')}
333:   const sync=main.querySelector(':scope > .rf263-sync-card');if(sync){sync.classList.add('tp168-sync-tail')}
334:   [more,main.querySelector(':scope > .tp120-health-journal'),diag].filter(Boolean).forEach(x=>main.appendChild(x));
335:   if(sync)main.appendChild(sync);
336:   const note=document.createElement('div');note.className='tp168-health-note';note.innerHTML='<span aria-hidden="true">ⓘ</span><p>'+escText(c().note)+'</p>';main.appendChild(note);
337:   window.tp107FitViews?.();
338:  }
339: 
340: 
341:  function unifyChevrons(root){
342:   const selectors=['.chev','.rf-history-health-chevron','.rf103-history-chevron','.tp146-exercise-chevron','.tp152-chevron','.tp152-program-chevron','.tp166-home-coach-chevron','.tp1511-training summary i'];
343:   root?.querySelectorAll?.(selectors.join(',')).forEach(el=>{if(el&&el.textContent!=='›')el.textContent='›'});
344:  }
345:  window.TrainPilot168Health={version:'1.6.9',weightOpen:false,pulseOpen:false,bodyHtml:'',decorate};
346:  const legacyWeightScreen=window.rf215WeightScreen;
347:  window.rf215WeightScreen=function(){
348:   window.TrainPilot168Health.weightOpen=true;
349:   if(state?.tab!=='health'||!document.querySelector('main.rf263-health')){
350:    if(typeof go==='function')go('health');else if(typeof rf206HealthHub==='function')rf206HealthHub();
351:   }else refreshHealth();
352:   setTimeout(()=>{document.querySelector('.tp168-weight-journal')?.scrollIntoView?.({block:'center',behavior:'smooth'})},0);
353:   return true;
354:  };
355: 
356:  // Legacy links can enter the hub directly without going through render().
357:  // Decorate once after the enclosing render, or immediately for a direct entry.
358:  let renderDepth=0;
359:  const healthHubBase=window.rf263HealthHub;
360:  if(typeof healthHubBase==='function')window.rf263HealthHub=function(){const out=healthHubBase.apply(this,arguments);if(!renderDepth)refreshHealth();return out};
```

## www/index.html — 1400–1608

Ref: `7473b30f387eda54186c1e31830304ca12bd104f`; teljes forrás SHA256: `9da5d0ef8746b53afe293d6ddd6e4238aa9953955740ff0e0a0687a9b7794ebb`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/index.html

```text
1400:   if(s.kind==='distance')return tp107CardioSummary(s);
1401:   if(s.kind==='bilateral')return t.bestSides+': '+fmt(s.maxLeft)+' / '+fmt(s.maxRight)+' mp';
1402:   if(s.kind==='timed')return t.bestTime+': '+fmt(s.maxSeconds)+' mp';
1403:   if(s.kind==='reps')return t.bestReps+': '+fmt(s.maxReps);
1404:   return s.maxWeight?(rf220L().maxWeight+': '+fmt(s.maxWeight)+' kg'):(t.bestReps+': '+fmt(s.maxReps));
1405:  };
1406:  const detail=s=>{
1407:   const t=copy(),label=(a,b)=>'<div class="stat"><small>'+esc(a)+'</small><strong>'+esc(b)+'</strong></div>';
1408:   if(s.kind==='distance')return tp107CardioDetail(s);
1409:   if(s.kind==='bilateral')return label(t.bestSides,fmt(s.maxLeft)+' / '+fmt(s.maxRight)+' mp')+label(t.last,fmt(s.latest.left)+' / '+fmt(s.latest.right)+' mp')+label(t.sets,fmt(s.count))+label(t.sessions,fmt(s.sessions));
1410:   if(s.kind==='timed')return label(t.bestTime,fmt(s.maxSeconds)+' mp')+label(t.last,fmt(s.latest.seconds)+' mp')+label(t.sets,fmt(s.count))+label(t.sessions,fmt(s.sessions));
1411:   if(s.kind==='reps')return label(t.bestReps,fmt(s.maxReps))+label(t.last,fmt(s.latest.r))+label(t.sets,fmt(s.count))+label(t.sessions,fmt(s.sessions));
1412:   return label(rf220L().maxWeight,s.maxWeight?fmt(s.maxWeight)+' kg':'—')+label(rf220L().e1rm,s.e1rm?fmt(s.e1rm)+' kg':'—')+label(rf220L().bestSet,fmt(s.best.w)+' kg × '+fmt(s.best.r))+label(rf220L().volume,s.maxVolume?fmt(s.maxVolume)+' kg·rep':'—');
1413:  };
1414:  rf220ProgressHtml=function(){
1415:   const t=rf220L(),rows=rf220ProgressRows(),x=copy();
1416:   if(!rows.length)return '<div class="card"><h2>'+esc(t.progress)+'</h2><p class="muted">'+esc(t.noData)+'</p></div>';
1417:   return '<section class="card tp3-stats-card tp4-stats-card"><h2>'+esc(t.progress)+' • '+esc(t.records)+'</h2><p class="small muted tp3-stats-hint">'+esc(x.statsHint)+'</p><div class="tp3-stats-list">'+rows.map(({id,s})=>
1418:    '<details class="tp3-stat-row tp4-stat-row" data-tp3-stat="'+esc(id)+'" data-tp4-kind="'+esc(s.kind)+'"><summary><div class="tp3-stat-copy"><strong>'+esc(rf220ExerciseName(id))+'</strong><span class="small muted">'+esc(summary(s))+'</span></div><span class="tp3-unified-chevron" aria-hidden="true">›</span></summary><div class="tp3-stat-detail"><div class="grid2">'+detail(s)+'</div><div class="small muted tp3-stat-note">'+esc(s.kind==='distance'?tp107T('rule'):t.approx)+'</div></div></details>'
1419:   ).join('')+'</div></section>';
1420:  };
1421: 
1422:  const adviceBase=rf233ExerciseAdvice;
1423:  rf233ExerciseAdvice=function(id,decision){
1424:   let a=null;try{a=adviceBase(id,decision)}catch(_){}
1425:   if(a&&a.action!=='unknown')return a;
1426:   const s=rf220ExerciseStats(id),t=rf233L(),x=copy();
1427:   if(!s)return a||{action:'unknown',label:t.unknown,text:x.noHistory};
1428:   let text='';
1429:   if(s.kind==='bilateral')text=x.last+': '+fmt(s.latest.left)+' / '+fmt(s.latest.right)+' mp • '+x.history+'.';
1430:   else if(s.kind==='timed')text=x.last+': '+fmt(s.latest.seconds)+' mp • '+x.history+'.';
1431:   else if(s.kind==='reps')text=x.last+': '+fmt(s.latest.r)+' • '+x.history+'.';
1432:   else text=summary(s)+' • '+x.history+'.';
1433:   return {action:'hold',label:t.hold||x.hold,text:text};
1434:  };
1435:  rf233CoachPlan=function(){
1436:   const readiness=rf220Readiness(),decision=rf233Decision(readiness),target=rf233TargetWorkout(),raw=target.day?.exercises||[],ids=[];
1437:   for(const v of raw){const id=typeof v==='string'?v:v?.id;if(id&&!ids.includes(id))ids.push(id)}
1438:   return {readiness,decision,target,exercises:ids.map(id=>{const e=byId(id);return {id,name:e?(typeof tp149ExerciseName==='function'?tp149ExerciseName(e):rf220ExerciseName(id)):id,effort:rf233LastEffort(id),advice:rf233ExerciseAdvice(id,decision)}})};
1439:  };
1440: 
1441:  const targetSummary=target=>{
1442:   if(!target?.day)return '—';
1443:   const p=target.program,programName=p?(typeof tp149ProgramMeta==='function'?tp149ProgramMeta(p,'name'):(p.name||'')):'';
1444:   const dayName=target.day.name||target.day.id||'';
1445:   const when=target.item?.start?(typeof tp149FormatDateTime==='function'?tp149FormatDateTime(target.item.start,{year:'numeric',month:'short',day:'numeric',hour:'2-digit',minute:'2-digit'}):new Date(target.item.start).toLocaleString()):'';
1446:   return [programName,dayName,when].filter(Boolean).join(' • ');
1447:  };
1448:  window.tp4PolishCoachTarget=function(root=document){
1449:   const host=root?.matches?.('.tp151-coach')?root:root?.querySelector?.('.tp151-coach');
1450:   if(!host)return false;
1451:   const strong=host.querySelector('.tp153-coach-next strong')||host.querySelector('.tp151-coach-target>:last-child strong');
1452:   if(!strong)return false;
1453:   const target=rf233CoachPlan().target,full=targetSummary(target);strong.textContent=full;strong.classList.add('tp4-next-summary');
1454:   const dup=host.querySelector?.('.tp151-coach-target + p.small.muted');if(dup){dup.textContent='';dup.classList.add('tp4-next-duplicate');dup.hidden=true}
1455:   return true;
1456:  };
1457:  if(typeof window.tp155R4RefreshPanel==='function'){
1458:   const panelRefreshBase=window.tp155R4RefreshPanel;
1459:   window.tp155R4RefreshPanel=function(){const out=panelRefreshBase.apply(this,arguments);const host=document.getElementById('tp155R4PanelHost');if(host?.dataset?.panel==='coach')window.tp4PolishCoachTarget(host);return out};
1460:  }
1461:  const renderBase=render;
1462:  render=function(){const out=renderBase.apply(this,arguments);window.tp4PolishCoachTarget(document);return out};
1463: 
1464:  const style=document.createElement('style');style.id='tp4CoachCoverageCss';style.textContent=[
1465:   '.tp153-coach-next{min-width:0!important}.tp153-coach-next strong.tp4-next-summary{font-size:13px!important;line-height:1.25!important;white-space:normal!important;overflow-wrap:anywhere!important;word-break:normal!important;max-width:100%!important}',
1466:   '.tp151-coach-target>*{min-width:0!important}.tp4-next-duplicate{display:none!important}',
1467:   '.tp4-stat-row .tp3-stat-copy span{white-space:normal!important;overflow-wrap:anywhere!important}',
1468:   '@media(max-width:412px){.tp151-coach-target{grid-template-columns:minmax(0,.72fr) minmax(0,1.28fr)!important;gap:6px!important}.tp153-coach-next{padding:7px!important}.tp153-coach-next strong.tp4-next-summary{font-size:12px!important}}',
1469:   '@media(max-width:340px){.tp151-coach-target{grid-template-columns:minmax(0,.64fr) minmax(0,1.36fr)!important}.tp153-coach-next strong.tp4-next-summary{font-size:11.5px!important}}'
1470:  ].join('');document.head.appendChild(style);
1471:  window.TrainPilotPhase4={version:TP4_VERSION,allHistoryExercises:true,allTargetExercises:true,timedStats:true,bilateralStats:true,nextWorkoutSummary:true};
1472: })();
1473: 
1474: 
1475: /* @section Phase 5 — #27 Health pulse trend source + persistent weight edit */
1476: (function(){
1477:  'use strict';
1478:  const TP5_VERSION='phase5-health-27-r1';
1479:  const COPY={
1480:   hu:{today:'Mai pulzus',trend:'7 napos trend',trendSub:'Elmúlt 7 naptári nap • napi átlagpulzus',few:'Elmúlt 7 naptári nap • még kevés mért adat',source:'Forrás: Health Connect szinkron → TrainPilot Health-napló (healthLedgerV1), napi averageHeartRate.',noData:'Nincs adat',weightEdit:'Testsúly szerkesztése',weight:'Testsúly',date:'Mérés dátuma',save:'Mentés',cancel:'Mégse',del:'Törlés',range:'30–300 kg közötti értéket adj meg.'},
1481:   en:{today:'Today heart rate',trend:'7-day trend',trendSub:'Last 7 calendar days • daily average heart rate',few:'Last 7 calendar days • not enough measured data yet',source:'Source: Health Connect sync → TrainPilot Health ledger (healthLedgerV1), daily averageHeartRate.',noData:'No data',weightEdit:'Edit body weight',weight:'Body weight',date:'Measurement date',save:'Save',cancel:'Cancel',del:'Delete',range:'Enter a value between 30 and 300 kg.'},
1482:   de:{today:'Heutiger Puls',trend:'7-Tage-Trend',trendSub:'Letzte 7 Kalendertage • täglicher Durchschnittspuls',few:'Letzte 7 Kalendertage • noch zu wenige Messdaten',source:'Quelle: Health-Connect-Sync → TrainPilot Health-Protokoll (healthLedgerV1), tägliches averageHeartRate.',noData:'Keine Daten',weightEdit:'Körpergewicht bearbeiten',weight:'Körpergewicht',date:'Messdatum',save:'Speichern',cancel:'Abbrechen',del:'Löschen',range:'Bitte einen Wert zwischen 30 und 300 kg eingeben.'},
1483:   ro:{today:'Pulsul de azi',trend:'Tendință 7 zile',trendSub:'Ultimele 7 zile calendaristice • puls mediu zilnic',few:'Ultimele 7 zile calendaristice • încă sunt puține date măsurate',source:'Sursă: sincronizare Health Connect → jurnalul Health TrainPilot (healthLedgerV1), averageHeartRate zilnic.',noData:'Fără date',weightEdit:'Editează greutatea',weight:'Greutate',date:'Data măsurării',save:'Salvează',cancel:'Anulează',del:'Șterge',range:'Introdu o valoare între 30 și 300 kg.'}
1484:  };
1485:  const lang=()=>{try{return typeof rf212Lang==='function'?rf212Lang():(document.documentElement.lang||'hu').slice(0,2)}catch(_){return 'hu'}};
1486:  const tx=()=>COPY[lang()]||COPY.hu;
1487:  const esc5=v=>String(v==null?'':v).replace(/[&<>"']/g,ch=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
1488:  const positive=v=>{const n=Number(v);return Number.isFinite(n)&&n>0?n:null};
1489:  const fmt=v=>{try{return typeof tp149FormatNumber==='function'?tp149FormatNumber(v,{maximumFractionDigits:1}):String(Math.round(Number(v)*10)/10)}catch(_){return String(v)}};
1490:  const dayKey=d=>{try{return typeof rf240DayKey==='function'?rf240DayKey(d):d.toISOString().slice(0,10)}catch(_){return d.toISOString().slice(0,10)}};
1491:  const ledger=()=>{try{return typeof rf240Ledger==='function'?rf240Ledger():{days:{}}}catch(_){return {days:{}}}};
1492:  const locale=()=>({hu:'hu-HU',en:'en-US',de:'de-DE',ro:'ro-RO'}[lang()]||'hu-HU');
1493: 
1494:  window.tp5PulseTrendRows=function(){
1495:   const days=ledger()?.days||{},today=new Date();today.setHours(12,0,0,0);const out=[];
1496:   for(let offset=6;offset>=0;offset--){
1497:    const d=new Date(today);d.setDate(today.getDate()-offset);const key=dayKey(d),value=positive(days?.[key]?.averageHeartRate);
1498:    const data=days[key]||{};
1499:    out.push({key,date:d,value,provider:data.metricProviders?.averageHeartRate||data.provider||data.channel||'health_connect',samples:positive(data.heartRateSamples)});
1500:   }
1501:   return out;
1502:  };
1503: 
1504:  function chart(rows,cls,w,h,p){
1505:   const valid=rows.map((r,i)=>({i,value:positive(r.value)})).filter(x=>x.value!=null);
1506:   if(!valid.length)return '<div class="tp5-pulse-empty">'+esc5(tx().noData)+'</div>';
1507:   const mn=Math.min(...valid.map(x=>x.value)),mx=Math.max(...valid.map(x=>x.value)),span=Math.max(1,mx-mn);
1508:   const pts=valid.map(x=>({i:x.i,x:p+x.i*(w-2*p)/6,y:h-p-(x.value-mn)*(h-2*p)/span}));
1509:   let d='M'+pts[0].x.toFixed(1)+' '+pts[0].y.toFixed(1);
1510:   for(let i=1;i<pts.length;i++){const a=pts[i-1],b=pts[i],mx2=((a.x+b.x)/2).toFixed(1);d+=b.i===a.i+1?' C'+mx2+' '+a.y.toFixed(1)+' '+mx2+' '+b.y.toFixed(1)+' '+b.x.toFixed(1)+' '+b.y.toFixed(1):' M'+b.x.toFixed(1)+' '+b.y.toFixed(1)}
1511:   const dots=pts.map(x=>'<circle cx="'+x.x.toFixed(1)+'" cy="'+x.y.toFixed(1)+'" r="'+(w<200?'2.1':'3')+'"/>').join('');
1512:   return '<svg class="'+cls+'" viewBox="0 0 '+w+' '+h+'" preserveAspectRatio="none" aria-hidden="true"><path d="'+d+'"/>'+dots+'</svg>';
1513:  }
1514: 
1515:  function dayCells(rows){
1516:   return rows.map(r=>{
1517:    let label='';try{label=new Intl.DateTimeFormat(locale(),{weekday:'short'}).format(r.date).replace('.','')}catch(_){label=r.key.slice(5)}
1518:    const source=r.value==null?tx().noData:window.TrainPilotHealthDetails.providerLabel(r.provider)+(r.samples?' · '+window.TrainPilotHealthDetails.text('samples').replace('{n}',fmt(r.samples)):'');
1519:    return '<div class="tp5-pulse-day" data-health-pulse-day="'+r.key+'" title="'+esc5(r.key+' · '+source)+'"><small>'+esc5(label)+'</small><strong>'+esc5(r.value==null?'—':Math.round(r.value))+'</strong></div>';
1520:   }).join('');
1521:  }
1522: 
1523:  function decoratePulse(){
1524:   const pulse=document.querySelector('main.rf263-health .tp169-pulse-journal');if(!pulse)return false;
1525:   const rows=window.tp5PulseTrendRows(),measured=rows.filter(x=>x.value!=null).length,t=tx();
1526:   const summary=pulse.querySelector('.tp169-pulse-summary'),small=summary?.querySelector('small');
1527:   if(small)small.textContent=measured>=3?t.trendSub:t.few;
1528:   if(summary){
1529:    summary.querySelectorAll('.tp168-spark,.tp168-spark-empty,.tp5-trend-mini').forEach(x=>x.remove());
1530:    const mini=document.createElement('span');mini.className='tp5-trend-mini';mini.innerHTML=chart(rows,'tp168-spark tp169-smooth-chart tp5-pulse-mini-svg',132,34,3);
1531:    const chev=summary.querySelector(':scope > b');summary.insertBefore(mini,chev||null);
1532:   }
1533:   const body=pulse.querySelector('.tp169-pulse-body'),grid=body?.querySelector('.tp168-pulse-grid');if(!body||!grid)return true;
1534:   let today=body.querySelector('.tp5-pulse-today');
1535:   if(!today){today=document.createElement('section');today.className='tp5-pulse-today';today.innerHTML='<h3>'+esc5(t.today)+'</h3>';body.insertBefore(today,grid);today.appendChild(grid)}
1536:   const details=window.TrainPilotHealthDetails,todayRow=rows.at(-1);
1537:   let meta=today.querySelector('.tp-health-pulse-meta');if(!meta){meta=document.createElement('p');meta.className='tp-health-pulse-meta';today.appendChild(meta);}
1538:   meta.textContent=todayRow.value==null?t.noData:details.providerLabel(todayRow.provider)+(todayRow.samples?' · '+details.text('samples').replace('{n}',fmt(todayRow.samples)):'');
1539:   let old=body.querySelector(':scope > .tp169-pulse-chart');if(old)old.style.display='none';
1540:   let trend=body.querySelector('.tp5-pulse-trend-section');
1541:   if(!trend){trend=document.createElement('section');trend.className='tp5-pulse-trend-section';const recovery=body.querySelector('.tp168-recovery-history');body.insertBefore(trend,recovery||null)}
1542:   trend.innerHTML='<div class="tp5-pulse-trend-head"><div><h3>'+esc5(t.trend)+'</h3><p>'+esc5(measured>=3?t.trendSub:t.few)+'</p></div><span>'+measured+'/7</span></div>'+
1543:    '<div class="tp5-pulse-chart">'+chart(rows,'tp5-pulse-large-svg',300,108,10)+'</div>'+
1544:    '<div class="tp5-pulse-days">'+dayCells(rows)+'</div>'+
1545:    '<p class="tp5-pulse-source">'+esc5(details.text('source')+': '+([...new Set(rows.filter(r=>r.value!=null).map(r=>details.providerLabel(r.provider)))].join(' / ')||t.noData))+'</p>';
1546:   return true;
1547:  }
1548:  window.tp5DecoratePulse=decoratePulse;
1549: 
1550:  let weightFocus=null;
1551:  function closeWeightEditor(){
1552:   const modal=document.querySelector('#tp2628Dialog[data-tp5-weight-edit="1"]');if(!modal)return;
1553:   modal.remove();const f=weightFocus;weightFocus=null;if(f&&f.isConnected)try{f.focus({preventScroll:true})}catch(_){}
1554:  }
1555:  function refreshWeight(){
1556:   if(window.TrainPilot168Health){window.TrainPilot168Health.weightOpen=true;window.TrainPilot168Health.decorate?.()}
1557:   decoratePulse();
1558:  }
1559:  window.tp5OpenWeightEditor=function(date){
1560:   const rows=typeof weights==='function'?weights():[],idx=rows.findIndex(x=>String(x?.date||'')===String(date||''));if(idx<0)return false;
1561:   document.getElementById('tp2628Dialog')?.remove();const row=rows[idx],t=tx();weightFocus=document.activeElement;
1562:   const modal=document.createElement('div');modal.id='tp2628Dialog';modal.dataset.tp5WeightEdit='1';modal.className='tp2628-dialog-backdrop tp5-weight-backdrop';
1563:   let shown='';try{shown=new Intl.DateTimeFormat(locale(),{year:'numeric',month:'short',day:'numeric'}).format(new Date(row.date))}catch(_){shown=String(row.date||'')}
1564:   modal.innerHTML='<div class="tp2628-dialog tp5-weight-dialog" role="dialog" aria-modal="true" aria-labelledby="tp5WeightTitle">'+
1565:    '<div class="tp5-weight-title"><div><h2 id="tp5WeightTitle">'+esc5(t.weightEdit)+'</h2><p>'+esc5(shown)+'</p></div><button type="button" class="tp5-weight-x" data-tp5-close aria-label="'+esc5(t.cancel)+'">×</button></div>'+
1566:    '<label class="tp5-weight-field">'+esc5(t.weight)+'<div class="tp5-weight-input-wrap"><input class="field" data-tp2629-input id="tp5WeightKg" type="number" inputmode="decimal" min="30" max="300" step="0.1" value="'+esc5(row.kg)+'"><span>kg</span></div></label>'+
1567:    '<div class="tp5-weight-date"><small>'+esc5(t.date)+'</small><strong>'+esc5(shown)+'</strong></div>'+
1568:    '<p class="tp5-weight-error" aria-live="polite"></p>'+
1569:    '<div class="tp2628-dialog-actions tp5-weight-actions"><button type="button" class="btn danger" data-tp5-delete>'+esc5(t.del)+'</button><button type="button" class="btn secondary" data-tp2628-cancel>'+esc5(t.cancel)+'</button><button type="button" class="btn" data-tp2628-confirm>'+esc5(t.save)+'</button></div>'+
1570:    '</div>';
1571:   const input=modal.querySelector('#tp5WeightKg'),error=modal.querySelector('.tp5-weight-error');
1572:   const cancel=()=>closeWeightEditor();
1573:   const save=()=>{
1574:    const kg=Number(String(input.value||'').replace(',','.'));
1575:    if(!Number.isFinite(kg)||kg<30||kg>300){error.textContent=t.range;input.focus();return}
1576:    const next=(typeof weights==='function'?weights():[]).slice(),i=next.findIndex(x=>String(x?.date||'')===String(date||''));if(i<0){closeWeightEditor();return}
1577:    next[i]={...next[i],kg:Math.round(kg*10)/10,updatedAt:Date.now()};db.set('weights',next);
1578:    try{if(typeof cloudChanged==='function')cloudChanged()}catch(_){}
1579:    closeWeightEditor();refreshWeight();
1580:   };
1581:   modal.querySelectorAll('[data-tp2628-cancel],[data-tp5-close]').forEach(b=>b.addEventListener('click',cancel));
1582:   modal.querySelector('[data-tp2628-confirm]').addEventListener('click',save);
1583:   modal.querySelector('[data-tp5-delete]').addEventListener('click',()=>{closeWeightEditor();if(typeof rf215Del==='function')rf215Del(date)});
1584:   modal.addEventListener('click',e=>{if(e.target===modal)cancel()});
1585:   modal.addEventListener('keydown',e=>{if(e.key==='Escape'){e.preventDefault();cancel()}else if(e.key==='Enter'&&e.target===input){e.preventDefault();save()}});
1586:   document.body.appendChild(modal);setTimeout(()=>{try{input.focus({preventScroll:true});input.select()}catch(_){input.focus?.()}},0);
1587:   return true;
1588:  };
1589:  window.tp168EditWeight=window.tp5OpenWeightEditor;
1590:  window.rf215Edit=window.tp5OpenWeightEditor;
1591: 
1592:  if(window.TrainPilot168Health?.decorate){
1593:   const base=window.TrainPilot168Health.decorate;
1594:   window.TrainPilot168Health.decorate=function(){const out=base.apply(this,arguments);decoratePulse();return out};
1595:  }
1596:  if(typeof window.tp168TogglePulse==='function'){
1597:   const base=window.tp168TogglePulse;window.tp168TogglePulse=function(){
1598:    const out=base.apply(this,arguments);
1599:    // The existing Pulse content is already upgraded; never rewrite the chart on each tap.
1600:    if(!document.querySelector('main.rf263-health .tp169-pulse-journal .tp5-pulse-trend-section'))decoratePulse();
1601:    return out;
1602:   };
1603:  }
1604:  if(typeof window.render==='function'){
1605:   const base=window.render;window.render=function(){const out=base.apply(this,arguments);decoratePulse();return out};
1606:  }
1607:  const style=document.createElement('style');style.id='tp5Health27Css';style.textContent=[
1608:   '.tp5-pulse-today,.tp5-pulse-trend-section{margin:8px 0;padding:10px;border:1px solid var(--line);border-radius:14px;background:color-mix(in srgb,var(--card2) 72%,transparent)}',
```

## www/health-dashboard-168.css — 1–736

Ref: `7473b30f387eda54186c1e31830304ca12bd104f`; teljes forrás SHA256: `7b4c277e7cd906354d407e53da2a86c2713c9c9ba365436e5716310d97054742`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/health-dashboard-168.css

```text
1: /* TrainPilot 1.6.8 — Health dashboard visual refresh. */
2: main.tp168-health{--tp168-blue:#58aaff;--tp168-blue-2:#2e84ff;--tp168-soft:#9fc9ff;--tp168-panel:#131c27;--tp168-panel2:#182433;--tp168-line:#2a4664;--tp168-muted:#91a4bd;display:flex;flex-direction:column;gap:10px;padding-top:11px!important}
3: body.tp168-health-view{background:radial-gradient(circle at 50% 0,rgba(35,108,182,.12),transparent 35%),var(--bg)}
4: body.tp168-health-view .top.tp154-nav-grid .tp154-nav-cell.active,body.tp168-health-view .top.tp154-nav-grid .tp151-nav-item.active{background:linear-gradient(145deg,rgba(75,159,255,.66),rgba(39,126,229,.78))!important;border:1px solid rgba(117,190,255,.62)!important;color:#f7fbff!important;box-shadow:0 0 18px rgba(56,146,255,.28)!important}
5: body.tp168-health-view .top.tp154-nav-grid .tp154-nav-cell.active :is(.tp151-nav-icon,.tp151-nav-label),body.tp168-health-view .top.tp154-nav-grid .tp151-nav-item.active :is(.tp151-nav-icon,.tp151-nav-label){color:#f7fbff!important}
6: main.tp168-health>.tp168-health-head{display:flex;align-items:flex-end;justify-content:space-between;gap:14px;padding:2px 4px 5px}
7: main.tp168-health>.tp168-health-head h1{margin:0;font-size:clamp(31px,8vw,42px);line-height:1.02;letter-spacing:-.025em}
8: main.tp168-health>.tp168-health-head p{margin:6px 0 0;color:var(--tp168-muted);font-size:14px;line-height:1.25}
9: .tp168-date{flex:0 0 auto;text-align:right;color:var(--tp168-muted);display:grid;gap:2px;font-size:11px}.tp168-date strong{font-size:12px;color:#c8d7e8;font-weight:750}
10: main.tp168-health>.card,main.tp168-health>details.card{background:linear-gradient(145deg,rgba(24,36,51,.98),rgba(17,26,38,.98))!important;border-color:var(--tp168-line)!important;box-shadow:inset 0 1px rgba(255,255,255,.018)}
11: main.tp168-health>.tp168-today-card{padding:14px!important;border:1px solid var(--tp168-blue)!important;border-radius:20px!important;background:radial-gradient(circle at 14% 0,rgba(51,129,213,.18),transparent 38%),linear-gradient(145deg,#172536,#101924)!important;box-shadow:0 0 0 1px rgba(88,170,255,.13),0 0 22px rgba(43,139,246,.19),inset 0 1px rgba(255,255,255,.03)!important}
12: .tp168-today-head{display:flex;align-items:center;gap:11px;margin-bottom:11px}.tp168-today-head h2{margin:0!important;font-size:19px!important;line-height:1.08}.tp168-today-head p{margin:4px 0 0;color:var(--tp168-muted);font-size:12px}
13: .tp168-icon{display:grid;place-items:center;width:34px;height:34px;flex:0 0 34px;border-radius:11px;border:1px solid rgba(88,170,255,.32);background:rgba(49,120,197,.13);color:#6bb6ff}.tp168-icon svg{width:21px;height:21px;fill:none;stroke:currentColor;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round}.tp168-health-heart{width:38px;height:38px;flex-basis:38px}
14: .tp168-metrics{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:7px}.tp168-metric{min-width:0;min-height:105px;padding:9px 7px;border:1px solid #293d54;border-radius:14px;background:linear-gradient(145deg,rgba(28,43,59,.94),rgba(20,31,44,.94));display:flex;flex-direction:column;align-items:flex-start;justify-content:flex-start;gap:4px;overflow:hidden}.tp168-metric[role=button]{cursor:pointer}.tp168-metric[role=button]:active{transform:scale(.985)}.tp168-metric .tp168-icon{width:27px;height:27px;flex-basis:27px;border:0;background:transparent}.tp168-metric .tp168-icon svg{width:22px;height:22px}.tp168-metric>span:not(.tp168-icon){margin-top:auto;color:#a8b8cc;font-size:10.5px;font-weight:700;white-space:nowrap;max-width:100%;overflow:hidden;text-overflow:ellipsis}.tp168-metric strong{font-size:clamp(14px,3.6vw,18px);line-height:1.05;white-space:nowrap;max-width:100%;overflow:hidden;text-overflow:ellipsis}
15: .tp168-sleep .tp168-icon{color:#7d83ff}.tp168-hrv .tp168-icon{color:#61a8ff}.tp168-pulse .tp168-icon,.tp168-resting .tp168-icon{color:#ff7798}.tp168-steps .tp168-icon{color:#64b2ff}
16: .tp168-status-grid{display:grid;grid-template-columns:1fr 1fr;gap:7px;margin-top:9px}.tp168-status{appearance:none;border:1px solid #293e56;border-radius:13px;background:rgba(24,38,53,.82);color:var(--text);min-height:68px;padding:9px 10px;display:grid;grid-template-columns:32px minmax(0,1fr) auto;align-items:center;gap:8px;text-align:left;cursor:pointer}.tp168-status .tp168-icon{width:30px;height:30px;flex-basis:30px;border:0;background:transparent}.tp168-status span{display:grid;min-width:0}.tp168-status small{font-size:10px;color:var(--tp168-muted)}.tp168-status strong{font-size:15px;line-height:1.15;margin-top:2px}.tp168-status em{font-size:10px;font-style:normal;color:#7fdaa7;margin-top:2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.tp168-status>b,.tp168-trend>b{font-size:25px;color:#7894af;font-weight:400}.tp168-recovery .tp168-icon{color:#59d998}.tp168-level-low em{color:#ffaf70}.tp168-level-unknown em{color:var(--tp168-muted)}
17: .tp168-trend{appearance:none;width:100%;margin-top:8px;border:1px solid #2d5f8d;border-radius:13px;background:linear-gradient(90deg,rgba(32,78,120,.2),rgba(21,35,49,.86));color:var(--text);padding:8px 9px;display:grid;grid-template-columns:32px minmax(0,1fr) minmax(88px,132px) auto;align-items:center;gap:8px;text-align:left;cursor:pointer}.tp168-trend .tp168-icon{width:30px;height:30px;flex-basis:30px;border:0;background:transparent}.tp168-trend>span{display:grid;gap:2px;min-width:0}.tp168-trend>span strong{font-size:12px;color:#74b7ff}.tp168-trend>span small{font-size:10px;color:var(--tp168-muted);line-height:1.2}.tp168-spark{width:100%;height:34px;overflow:visible}.tp168-spark path{fill:none;stroke:#62aaff;stroke-width:2.2;vector-effect:non-scaling-stroke}.tp168-spark circle{fill:#72b8ff;stroke:#bde0ff;stroke-width:1}.tp168-spark-empty{height:34px;display:grid;place-items:center;color:#55718e;letter-spacing:4px}
18: main.tp168-health>.tp168-body-card,main.tp168-health>details.tp168-pulse-panel,main.tp168-health>details.tp168-more-panel,main.tp168-health>details.tp168-connect-panel{padding:0!important;border-radius:16px!important;overflow:hidden;margin:0!important}
19: .tp168-row-head{list-style:none!important;display:grid!important;grid-template-columns:38px minmax(0,1fr) auto!important;align-items:center!important;gap:10px!important;width:100%!important;min-height:64px!important;padding:9px 11px!important;margin:0!important;cursor:pointer!important;user-select:none}.tp168-row-head::-webkit-details-marker{display:none}.tp168-row-head .tp168-icon{width:36px;height:36px;flex-basis:36px}.tp168-row-copy{display:grid;gap:2px;min-width:0}.tp168-row-copy h2,.tp168-row-copy strong{margin:0!important;font-size:17px!important;line-height:1.1}.tp168-row-copy small{color:var(--tp168-muted);font-size:10.5px;line-height:1.2}.tp168-chevron{display:grid;place-items:center;width:34px;height:34px;border-radius:10px;background:rgba(36,55,75,.76);color:#d8e8f8;font-size:20px;transition:transform .16s ease}.tp168-body-card.tp168-open .tp168-chevron,details[open]>.tp168-row-head .tp168-chevron{transform:rotate(180deg)}
20: .tp168-collapse-content{max-height:0;opacity:.15;overflow:hidden;padding:0 10px!important;pointer-events:none;transition:max-height .22s ease,opacity .18s ease,padding .18s ease}.tp168-body-card.tp168-open .tp168-collapse-content{max-height:560px;opacity:1;padding:0 10px 10px!important;pointer-events:auto}.tp168-body-card .grid2{gap:7px!important}.tp168-body-card .stat{padding:8px 9px!important}.tp168-body-card .rf251-weight-btn{margin:7px 0 0!important}
21: main.tp168-health>details.tp168-pulse-panel>.tp168-pulse-grid,main.tp168-health>details.tp168-pulse-panel>.tp168-recovery-history,main.tp168-health>details.tp168-more-panel>.grid2,main.tp168-health>details.tp168-connect-panel>p,main.tp168-health>details.tp168-connect-panel>.rf263-permission-list{margin-left:10px!important;margin-right:10px!important}main.tp168-health>details[open]{padding-bottom:10px!important}
22: .tp168-pulse-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:6px;margin-bottom:8px}.tp168-pulse-grid>div{padding:8px 9px;border:1px solid #283f58;border-radius:11px;background:rgba(14,23,33,.46);display:grid;gap:3px}.tp168-pulse-grid small{color:var(--tp168-muted);font-size:10px}.tp168-pulse-grid strong{font-size:14px}.tp168-recovery-history{border-top:1px solid #263d55;padding-top:7px}.tp168-recovery-history>summary{cursor:pointer;color:#b7cae0;font-size:12px;font-weight:800;padding:5px 1px}.tp168-recovery-history[open]>summary{color:#75b9ff}
23: .tp168-more-panel>.tp168-row-head .tp168-icon{color:#66aff7}.tp168-connect-panel>.tp168-row-head .tp168-icon{color:#6eb8ff}
24: main.tp168-health>.tp168-sync-tail{order:99;padding:9px 10px!important;margin:-11px 0 0!important;border-top:0!important;border-radius:0 0 16px 16px!important;background:#111b26!important;box-shadow:none!important}.tp168-connect-panel{border-radius:16px 16px 0 0!important}.tp168-connect-panel:not([open])+.tp168-sync-tail{margin-top:-11px!important}.tp168-sync-tail .rf263-sync-actions{gap:7px!important}.tp168-sync-tail .btn{min-height:38px!important;padding:8px 9px!important}.tp168-sync-tail>span,.tp168-sync-tail>p{display:block;margin:5px 0 0!important;font-size:10px!important;color:var(--tp168-muted)!important}
25: main.tp168-health>.tp168-health-note{order:100;display:grid;grid-template-columns:24px minmax(0,1fr);gap:8px;align-items:center;border:1px solid #2a4664;border-radius:14px;background:rgba(26,42,58,.64);padding:9px 11px;color:#9fb2c8}.tp168-health-note>span{font-size:20px;color:#6eb6ff}.tp168-health-note p{margin:0;font-size:10.5px;line-height:1.35}
26: main.tp168-health .tp155-r4-accent-surface.tp168-today-card{border-color:var(--tp168-blue)!important;background:radial-gradient(circle at 14% 0,rgba(51,129,213,.18),transparent 38%),linear-gradient(145deg,#172536,#101924)!important;box-shadow:0 0 0 1px rgba(88,170,255,.13),0 0 22px rgba(43,139,246,.19),inset 0 1px rgba(255,255,255,.03)!important}
27: @media(max-width:390px){main.tp168-health{gap:8px}.tp168-health-head{padding-inline:2px!important}.tp168-health-head p{font-size:12px!important}.tp168-date{max-width:104px}.tp168-metrics{gap:5px}.tp168-metric{min-height:97px;padding:7px 5px;border-radius:12px}.tp168-metric>span:not(.tp168-icon){font-size:9px}.tp168-metric strong{font-size:14px}.tp168-status-grid{gap:5px}.tp168-status{padding:8px 7px;grid-template-columns:28px minmax(0,1fr) auto;gap:6px}.tp168-status .tp168-icon{width:27px;height:27px;flex-basis:27px}.tp168-status strong{font-size:13px}.tp168-trend{grid-template-columns:28px minmax(0,1fr) minmax(64px,92px) auto;padding:7px 6px;gap:5px}.tp168-trend .tp168-icon{width:27px;height:27px;flex-basis:27px}.tp168-row-head{grid-template-columns:34px minmax(0,1fr) auto!important;gap:8px!important;padding:8px 9px!important;min-height:60px!important}.tp168-row-head .tp168-icon{width:32px;height:32px;flex-basis:32px}.tp168-row-copy h2,.tp168-row-copy strong{font-size:15px!important}}
28: @media(max-width:340px){.tp168-status-grid{grid-template-columns:1fr}.tp168-trend{grid-template-columns:28px minmax(0,1fr) auto}.tp168-trend .tp168-spark,.tp168-trend .tp168-spark-empty{display:none}.tp168-date{font-size:10px;max-width:86px}.tp168-date strong{font-size:10px}.tp168-metric{min-height:91px}.tp168-metric .tp168-icon{width:24px;height:24px;flex-basis:24px}.tp168-metric .tp168-icon svg{width:19px;height:19px}}
29: @media(prefers-reduced-motion:reduce){.tp168-collapse-content,.tp168-chevron{transition:none!important}}
30: 
31: main.tp168-health .tp168-metric.stat{display:flex!important;flex-direction:column!important;align-items:flex-start!important;justify-content:flex-start!important;padding:9px 7px!important;min-height:105px!important}
32: @media(max-width:390px){main.tp168-health .tp168-metric.stat{min-height:97px!important;padding:7px 5px!important}}
33: @media(max-width:340px){main.tp168-health .tp168-metric.stat{min-height:91px!important}}
34: 
35: 
36: /* TrainPilot 1.6.9 — theme-aware Health surfaces, compact inline weight journal, unified chevrons. */
37: main.tp168-health{
38:  --tp168-blue:var(--accent);
39:  --tp168-blue-2:var(--accent);
40:  --tp168-soft:var(--accent2);
41:  --tp168-line:color-mix(in srgb,var(--accent) 30%,var(--line));
42:  --tp168-muted:var(--muted);
43: }
44: body.tp168-health-view{background:var(--bg)!important}
45: html[data-tp-theme-family="vivid"] body.tp168-health-view{
46:  background:radial-gradient(circle at 50% 0,color-mix(in srgb,var(--accent) 14%,transparent),transparent 36%),var(--bg)!important
47: }
48: 
49: /* Every cell in the 2x4 navigation uses the same active language. Basic themes stay matte. */
50: html body .top.tp154-nav-grid .tp154-nav-cell.active,
51: html body .top.tp154-nav-grid .tp151-nav-item.active,
52: html body .top.tp154-nav-grid .tp151-top-icon.active{
53:  background:linear-gradient(145deg,color-mix(in srgb,var(--accent) 46%,var(--card2)),color-mix(in srgb,var(--accent) 30%,var(--card)))!important;
54:  border:1px solid color-mix(in srgb,var(--accent) 62%,var(--line))!important;
55:  color:var(--text)!important;
56:  box-shadow:none!important
57: }
58: html body .top.tp154-nav-grid .tp154-nav-cell.active :is(.tp151-nav-icon,.tp151-nav-label,.tp154-nav-action-icon),
59: html body .top.tp154-nav-grid .tp151-nav-item.active :is(.tp151-nav-icon,.tp151-nav-label,.tp154-nav-action-icon){
60:  color:var(--text)!important
61: }
62: html[data-tp-theme-family="vivid"] body .top.tp154-nav-grid .tp154-nav-cell.active,
63: html[data-tp-theme-family="vivid"] body .top.tp154-nav-grid .tp151-nav-item.active,
64: html[data-tp-theme-family="vivid"] body .top.tp154-nav-grid .tp151-top-icon.active{
65:  box-shadow:0 0 18px color-mix(in srgb,var(--accent) 34%,transparent)!important
66: }
67: 
68: /* The old standalone Health title/subtitle is intentionally gone. */
69: main.tp168-health>.tp168-health-head{display:none!important}
70: 
71: main.tp168-health>.tp168-today-card,
72: main.tp168-health .tp155-r4-accent-surface.tp168-today-card{
73:  border-color:color-mix(in srgb,var(--accent) 78%,var(--line))!important;
74:  background:
75:   radial-gradient(circle at 14% 0,color-mix(in srgb,var(--accent) 13%,transparent),transparent 40%),
76:   linear-gradient(145deg,color-mix(in srgb,var(--accent) 8%,var(--card2)),var(--card))!important;
77:  box-shadow:none!important
78: }
79: html[data-tp-theme-family="vivid"] main.tp168-health>.tp168-today-card,
80: html[data-tp-theme-family="vivid"] main.tp168-health .tp155-r4-accent-surface.tp168-today-card{
81:  box-shadow:0 0 0 1px color-mix(in srgb,var(--accent) 18%,transparent),0 0 22px color-mix(in srgb,var(--accent) 22%,transparent)!important
82: }
83: .tp168-today-head{display:grid!important;grid-template-columns:auto minmax(0,1fr) auto;align-items:center!important}
84: .tp168-today-copy{min-width:0}
85: .tp168-today-date{align-self:start;color:var(--accent2);font-size:11px;font-weight:800;white-space:nowrap;padding-top:2px}
86: .tp168-icon{
87:  border-color:color-mix(in srgb,var(--accent) 35%,var(--line))!important;
88:  background:color-mix(in srgb,var(--accent) 11%,transparent)!important;
89:  color:var(--accent2)!important
90: }
91: .tp168-metric{
92:  border-color:color-mix(in srgb,var(--accent) 18%,var(--line))!important;
93:  background:linear-gradient(145deg,color-mix(in srgb,var(--accent) 5%,var(--card2)),var(--card))!important
94: }
95: .tp168-metric .tp168-icon,
96: .tp168-sleep .tp168-icon,.tp168-hrv .tp168-icon,.tp168-pulse .tp168-icon,.tp168-resting .tp168-icon,.tp168-steps .tp168-icon,
97: .tp168-recovery .tp168-icon,.tp168-more-panel>.tp168-row-head .tp168-icon,.tp168-connect-panel>.tp168-row-head .tp168-icon{
98:  color:var(--accent2)!important
99: }
100: .tp168-status{
101:  border-color:color-mix(in srgb,var(--accent) 22%,var(--line))!important;
102:  background:color-mix(in srgb,var(--accent) 5%,var(--card))!important
103: }
104: .tp168-status>b,.tp168-trend>b,.tp168-weight-summary>b{color:color-mix(in srgb,var(--accent2) 72%,var(--muted))!important}
105: .tp168-trend{
106:  border-color:color-mix(in srgb,var(--accent) 46%,var(--line))!important;
107:  background:linear-gradient(90deg,color-mix(in srgb,var(--accent) 10%,var(--card2)),var(--card))!important
108: }
109: .tp168-trend>span strong{color:var(--accent2)!important}
110: .tp168-spark path,.tp168-weight-spark path{stroke:var(--accent)!important}
111: .tp168-spark circle,.tp168-weight-spark circle{fill:var(--accent)!important;stroke:var(--accent2)!important}
112: .tp168-chevron{
113:  background:color-mix(in srgb,var(--accent) 9%,var(--card2))!important;
114:  color:var(--accent2)!important;
115:  transform:none!important
116: }
117: .tp168-body-card.tp168-open .tp168-chevron,details[open]>.tp168-row-head .tp168-chevron{transform:rotate(90deg)!important}
118: .tp168-recovery-history[open]>summary{color:var(--accent2)!important}
119: main.tp168-health>.tp168-health-note{
120:  border-color:color-mix(in srgb,var(--accent) 28%,var(--line))!important;
121:  background:color-mix(in srgb,var(--accent) 5%,var(--card))!important
122: }
123: .tp168-health-note>span{color:var(--accent2)!important}
124: main.tp168-health>.tp168-sync-tail{background:color-mix(in srgb,var(--accent) 3%,var(--card))!important}
125: .tp168-body-card .rf251-weight-btn{display:none!important}
126: 
127: /* Compact weight journal lives inside Mai állapot; the legacy full page is no longer used. */
128: .tp168-weight-journal{
129:  margin-top:8px;
130:  border:1px solid color-mix(in srgb,var(--accent) 34%,var(--line));
131:  border-radius:13px;
132:  background:linear-gradient(90deg,color-mix(in srgb,var(--accent) 7%,var(--card2)),var(--card));
133:  overflow:hidden
134: }
135: .tp168-weight-summary{
136:  appearance:none;width:100%;border:0;background:transparent;color:var(--text);
137:  padding:8px 9px;display:grid;grid-template-columns:32px minmax(0,1fr) minmax(88px,132px) auto;align-items:center;gap:8px;text-align:left
138: }
139: .tp168-weight-summary .tp168-icon{width:30px;height:30px;flex-basis:30px;border:0!important;background:transparent!important}
140: .tp168-weight-summary>span{display:grid;gap:2px;min-width:0}
141: .tp168-weight-summary>span strong{font-size:12px;color:var(--accent2)}
142: .tp168-weight-summary>span small{font-size:10px;color:var(--tp168-muted);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
143: .tp168-weight-summary>b{font-size:25px;font-weight:400;transition:transform .16s ease}
144: .tp168-weight-journal.tp168-open .tp168-weight-summary>b{transform:rotate(90deg)}
145: .tp168-weight-body{display:none;padding:0 9px 10px;border-top:1px solid color-mix(in srgb,var(--accent) 22%,var(--line))}
146: .tp168-weight-journal.tp168-open .tp168-weight-body{display:block}
147: .tp168-weight-spark{width:100%;height:34px;overflow:visible}.tp168-weight-spark path{fill:none;stroke-width:2.2;vector-effect:non-scaling-stroke}.tp168-weight-spark circle{stroke-width:1}
148: .tp168-weight-spark-empty{height:34px;display:grid;place-items:center;color:var(--muted);letter-spacing:4px}
149: .tp168-weight-stats{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:6px;margin:9px 0}
150: .tp168-weight-stats>div{padding:7px 8px;border:1px solid color-mix(in srgb,var(--accent) 17%,var(--line));border-radius:11px;background:color-mix(in srgb,var(--accent) 3%,var(--card));min-width:0}
151: .tp168-weight-stats small{display:block;color:var(--muted);font-size:9.5px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
152: .tp168-weight-stats strong{display:block;margin-top:3px;font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
153: .tp168-weight-form{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr) auto;gap:6px;align-items:end}
154: .tp168-weight-form label{display:grid;gap:4px;font-size:10px;color:var(--muted);min-width:0}
155: .tp168-weight-form .field{min-height:38px;padding:8px 9px}
156: .tp168-weight-save{min-height:38px;padding:8px 12px;background:var(--accent)!important;color:#10151c!important}
157: .tp168-weight-range{display:flex;gap:5px;flex-wrap:wrap;margin:8px 0 2px}
158: .tp168-weight-range button{appearance:none;border:1px solid var(--line);border-radius:10px;background:var(--card2);color:var(--text);padding:6px 8px;font-weight:800;font-size:11px}
159: .tp168-weight-range button.active{border-color:color-mix(in srgb,var(--accent) 62%,var(--line));background:color-mix(in srgb,var(--accent) 17%,var(--card2));color:var(--accent2)}
160: .tp168-weight-chart{height:108px;overflow:hidden;margin-top:2px}
161: .tp168-weight-chart svg{width:100%!important;height:108px!important}.tp168-weight-chart svg text{font-size:18px}
162: .tp168-weight-recent{display:grid;gap:5px;margin-top:8px}.tp168-weight-recent>small{color:var(--muted);font-size:10px;font-weight:800}
163: .tp168-weight-recent>div{display:flex;justify-content:space-between;align-items:center;gap:8px;padding:7px 8px;border:1px solid var(--line);border-radius:10px;background:var(--card)}
164: .tp168-weight-recent>div>span:first-child{display:grid;gap:1px}.tp168-weight-recent strong{font-size:12px}.tp168-weight-recent small{font-size:9px;color:var(--muted)}
165: .tp168-weight-recent>div>span:last-child{display:flex;gap:5px}.tp168-weight-recent button{appearance:none;border:1px solid color-mix(in srgb,var(--accent) 32%,var(--line));border-radius:8px;background:color-mix(in srgb,var(--accent) 7%,var(--card2));color:var(--text);padding:5px 7px;font-size:9.5px;font-weight:800}
166: .tp168-weight-recent button.danger{border-color:color-mix(in srgb,var(--bad) 45%,var(--line));background:color-mix(in srgb,var(--bad) 10%,var(--card2));color:#ffd9dc}
167: .tp168-weight-empty{padding:16px 4px 6px;text-align:center;color:var(--muted);font-size:11px}
168: 
169: /* Use the Mai állapot right-chevron language throughout the app. */
170: .chev,.rf-history-health-chevron,.rf103-history-chevron,.tp146-exercise-chevron,.tp152-chevron,.tp152-program-chevron,.tp166-home-coach-chevron,.tp1511-training summary i{
171:  color:var(--accent2)!important
172: }
173: details:not([open])>summary :is(.rf103-history-chevron,.tp146-exercise-chevron,.tp152-chevron,.tp152-program-chevron),
174: details:not([open])>summary.tp152-day-summary>.tp152-chevron,
175: details:not([open])>summary>.tp152-chevron{transform:none!important}
176: details[open]>summary :is(.rf103-history-chevron,.tp146-exercise-chevron,.tp152-chevron,.tp152-program-chevron),
177: details[open]>summary.tp152-day-summary>.tp152-chevron,
178: details[open]>summary>.tp152-chevron{transform:rotate(90deg)!important}
179: .rf-history-health-panel .rf-history-health-chevron{transform:rotate(90deg)!important}
180: .rf-history-health-panel.collapsed .rf-history-health-chevron{transform:none!important}
181: .tp-library-card>summary::after{content:'›'!important;transform:none!important}
182: .tp-library-card[open]>summary::after{transform:rotate(90deg)!important}
183: 
184: @media(max-width:390px){
185:  .tp168-today-head{grid-template-columns:auto minmax(0,1fr)!important}
186:  .tp168-today-date{grid-column:2;margin-top:2px}
187:  .tp168-weight-summary{grid-template-columns:28px minmax(0,1fr) minmax(64px,92px) auto;padding:7px 6px;gap:5px}
188:  .tp168-weight-summary .tp168-icon{width:27px;height:27px;flex-basis:27px}
189:  .tp168-weight-form{grid-template-columns:1fr 1fr}.tp168-weight-save{grid-column:1/-1;width:100%}
190: }
191: @media(max-width:340px){
192:  .tp168-weight-summary{grid-template-columns:28px minmax(0,1fr) auto}
193:  .tp168-weight-summary .tp168-weight-spark,.tp168-weight-summary .tp168-weight-spark-empty{display:none}
194:  .tp168-weight-stats{grid-template-columns:1fr 1fr}.tp168-weight-stats>div:last-child{grid-column:1/-1}
195: }
196: 
197: 
198: /* 1.6.9 priority fix: override the older Health-only blue active-nav rule. */
199: html body.tp168-health-view .top.tp154-nav-grid .tp154-nav-cell.active,
200: html body.tp168-health-view .top.tp154-nav-grid .tp151-nav-item.active,
201: html body.tp168-health-view .top.tp154-nav-grid .tp151-top-icon.active{
202:  background:linear-gradient(145deg,color-mix(in srgb,var(--accent) 46%,var(--card2)),color-mix(in srgb,var(--accent) 30%,var(--card)))!important;
203:  border-color:color-mix(in srgb,var(--accent) 62%,var(--line))!important;
204:  color:var(--text)!important;
205:  box-shadow:none!important
206: }
207: html body.tp168-health-view .top.tp154-nav-grid .tp154-nav-cell.active :is(.tp151-nav-icon,.tp151-nav-label,.tp154-nav-action-icon),
208: html body.tp168-health-view .top.tp154-nav-grid .tp151-nav-item.active :is(.tp151-nav-icon,.tp151-nav-label,.tp154-nav-action-icon){
209:  color:var(--text)!important
210: }
211: html[data-tp-theme-family="vivid"] body.tp168-health-view .top.tp154-nav-grid .tp154-nav-cell.active,
212: html[data-tp-theme-family="vivid"] body.tp168-health-view .top.tp154-nav-grid .tp151-nav-item.active,
213: html[data-tp-theme-family="vivid"] body.tp168-health-view .top.tp154-nav-grid .tp151-top-icon.active{
214:  box-shadow:0 0 18px color-mix(in srgb,var(--accent) 34%,transparent)!important
215: }
216: 
217: 
218: /* 1.6.9 minor refresh — Pulse trend and Body/Fitness fold into the two dashboard disclosures. */
219: .tp169-pulse-journal{
220:  margin-top:8px;
221:  border:1px solid color-mix(in srgb,var(--accent) 46%,var(--line));
222:  border-radius:13px;
223:  background:linear-gradient(90deg,color-mix(in srgb,var(--accent) 10%,var(--card2)),var(--card));
224:  overflow:hidden
225: }
226: .tp169-pulse-journal>.tp168-trend{margin:0!important;border:0!important;border-radius:0!important;background:transparent!important}
227: .tp169-pulse-journal.tp169-open>.tp168-trend>b{transform:rotate(90deg)}
228: .tp169-pulse-body{display:none;padding:9px;border-top:1px solid color-mix(in srgb,var(--accent) 22%,var(--line))}
229: .tp169-pulse-journal.tp169-open>.tp169-pulse-body{display:block}
230: .tp169-pulse-chart{height:108px;margin:4px 0 8px;overflow:hidden}
231: .tp169-pulse-chart svg,.tp168-weight-chart svg{display:block;width:100%!important;height:108px!important}
232: .tp169-smooth-chart path{fill:none;stroke:var(--accent)!important;stroke-width:2.5;stroke-linecap:round;stroke-linejoin:round;vector-effect:non-scaling-stroke}
233: .tp169-smooth-chart circle{fill:var(--accent)!important;stroke:var(--accent2)!important;stroke-width:1.2}
234: .tp169-pulse-body .tp168-pulse-grid{margin:0 0 7px}
235: .tp169-pulse-body .tp168-pulse-grid>div{
236:  border-color:color-mix(in srgb,var(--accent) 18%,var(--line))!important;
237:  background:color-mix(in srgb,var(--accent) 3%,var(--card))!important
238: }
239: .tp169-pulse-body .tp168-recovery-history{
240:  margin-top:5px;
241:  border-top:1px solid color-mix(in srgb,var(--accent) 22%,var(--line))!important;
242:  padding-top:6px
243: }
244: .tp169-pulse-body .tp168-recovery-history>summary{
245:  list-style:none;display:flex;align-items:center;justify-content:space-between;gap:8px
246: }
247: .tp169-pulse-body .tp168-recovery-history>summary::-webkit-details-marker{display:none}
248: .tp169-pulse-body .tp168-recovery-history>summary .tp168-chevron{width:28px;height:28px}
249: .tp169-pulse-body .tp168-recovery-history[open]>summary .tp168-chevron{transform:rotate(90deg)!important}
250: 
251: .tp169-body-inline{
252:  margin:9px 0 8px;
253:  padding:8px;
254:  border:1px solid color-mix(in srgb,var(--accent) 20%,var(--line));
255:  border-radius:12px;
256:  background:color-mix(in srgb,var(--accent) 3%,var(--card))
257: }
258: .tp169-inline-heading{display:grid;gap:2px;margin-bottom:7px}
259: .tp169-inline-heading strong{font-size:12px;color:var(--accent2)}
260: .tp169-inline-heading small{font-size:9.5px;color:var(--muted)}
261: .tp169-body-inline .grid2{gap:6px!important}
262: .tp169-body-inline .stat{padding:7px 8px!important;min-height:0!important}
263: .tp169-body-inline .stat small{font-size:9.5px!important}
264: .tp169-body-inline .stat strong{font-size:13px!important}
265: .tp169-body-inline .rf251-weight-btn,#rf251WeightJournal{display:none!important}
266: 
267: /* Defensive: the absorbed legacy cards must never remain as duplicate standalone blocks. */
268: main.tp168-health>.tp168-body-card,
269: main.tp168-health>details.tp168-pulse-panel{display:none!important}
270: 
271: @media(max-width:390px){
272:  .tp169-pulse-journal>.tp168-trend{grid-template-columns:28px minmax(0,1fr) minmax(64px,92px) auto!important}
273:  .tp169-pulse-body{padding:7px}
274:  .tp169-body-inline{padding:7px}
275: }
276: @media(max-width:340px){
277:  .tp169-pulse-journal>.tp168-trend{grid-template-columns:28px minmax(0,1fr) auto!important}
278:  .tp169-pulse-journal>.tp168-trend .tp168-spark,.tp169-pulse-journal>.tp168-trend .tp168-spark-empty{display:none}
279: }
280: 
281: 
282: /* 1.6.9 minor 2 — compact weight journal, correct recovery history, inline Today date. */
283: .tp168-today-head{
284:  display:grid!important;
285:  grid-template-columns:auto minmax(0,1fr)!important;
286:  align-items:center!important
287: }
288: .tp168-today-copy{min-width:0}
289: .tp169-today-titleline{display:flex;align-items:baseline;gap:9px;min-width:0;flex-wrap:wrap}
290: .tp169-today-titleline h2{margin:0!important}
291: .tp168-today-date{
292:  position:static!important;
293:  align-self:auto!important;
294:  padding:0!important;
295:  font-size:14px!important;
296:  line-height:1.1!important;
297:  font-weight:850!important;
298:  color:var(--accent2)!important;
299:  white-space:nowrap
300: }
301: .tp168-today-copy>p{margin-top:3px!important}
302: 
303: .tp169-body-inline{padding:7px!important;margin:8px 0 7px!important}
304: .tp169-inline-heading{margin-bottom:6px!important}
305: .tp169-body-inline .grid2{
306:  display:grid!important;
307:  grid-template-columns:repeat(3,minmax(0,1fr))!important;
308:  gap:5px!important
309: }
310: .tp169-body-inline .stat{
311:  min-width:0!important;
312:  padding:7px 6px!important;
313:  border-radius:10px!important
314: }
315: .tp169-body-inline .stat small{
316:  white-space:nowrap;overflow:hidden;text-overflow:ellipsis
317: }
318: .tp169-body-inline .stat strong{font-size:12.5px!important}
319: .tp169-body-inline .stat .small{font-size:8.5px!important}
320: .tp169-body-inline button{display:none!important}
321: 
322: .tp168-weight-stats{
323:  grid-template-columns:repeat(2,minmax(0,1fr))!important;
324:  margin:7px 0!important
325: }
326: .tp169-weight-entry{
327:  margin:7px 0;
328:  border:1px solid color-mix(in srgb,var(--accent) 24%,var(--line));
329:  border-radius:11px;
330:  background:color-mix(in srgb,var(--accent) 4%,var(--card2));
331:  overflow:hidden
332: }
333: .tp169-weight-entry>summary{
334:  list-style:none;
335:  min-height:38px;
336:  padding:7px 9px;
337:  display:flex;
338:  align-items:center;
339:  justify-content:space-between;
340:  gap:8px;
341:  cursor:pointer;
342:  color:var(--accent2);
343:  font-size:11px;
344:  font-weight:850
345: }
346: .tp169-weight-entry>summary::-webkit-details-marker{display:none}
347: .tp169-weight-entry>summary .tp168-chevron{width:27px;height:27px;font-size:18px}
348: .tp169-weight-entry[open]>summary .tp168-chevron{transform:rotate(90deg)!important}
349: .tp169-weight-entry-body{
350:  padding:8px;
351:  border-top:1px solid color-mix(in srgb,var(--accent) 17%,var(--line))
352: }
353: .tp169-weight-entry .tp168-weight-form{margin:0!important}
354: .tp168-weight-range{margin-top:7px!important}
355: 
356: .tp169-recovery-empty{
357:  padding:9px 2px 5px;
358:  color:var(--muted);
359:  font-size:10px
360: }
361: .tp169-recovery-row{
362:  display:grid;
363:  grid-template-columns:minmax(92px,.8fr) minmax(0,1.7fr);
364:  align-items:center;
365:  gap:8px;
366:  padding:7px 2px;
367:  border-bottom:1px solid color-mix(in srgb,var(--accent) 12%,var(--line))
368: }
369: .tp169-recovery-row:last-child{border-bottom:0}
370: .tp169-recovery-row strong{font-size:10.5px;color:var(--text)}
371: .tp169-recovery-row span{font-size:10px;color:var(--muted);min-width:0}
372: .tp169-recovery-row b{color:var(--accent2);font-weight:800}
373: .tp169-recovery-row i{font-style:normal;opacity:.6;padding:0 2px}
374: 
375: /* Health Connect capability/permission content belongs only to its own panel. */
376: .tp169-pulse-journal .rf263-permission-list,
377: .tp169-pulse-journal #rf263HealthDiagnostics{display:none!important}
378: 
379: @media(max-width:360px){
380:  .tp169-today-titleline{gap:6px}
381:  .tp168-today-date{font-size:12.5px!important}
382:  .tp169-body-inline .grid2{grid-template-columns:repeat(2,minmax(0,1fr))!important}
383:  .tp169-body-inline .stat:last-child{grid-column:1/-1}
384:  .tp169-recovery-row{grid-template-columns:1fr;gap:2px}
385:  .tp168-weight-form{grid-template-columns:1fr 1fr!important}
386:  .tp168-weight-save{grid-column:1/-1!important;width:100%}
387: }
388: 
389: 
390: /* #41 — one global, theme-aware navigation/detail chevron language. */
391: .tp-global-chevron{
392:  box-sizing:border-box!important;
393:  display:inline-grid!important;
394:  place-items:center!important;
395:  flex:0 0 30px!important;
396:  width:30px!important;
397:  min-width:30px!important;
398:  max-width:30px!important;
399:  height:36px!important;
400:  min-height:36px!important;
401:  border:0!important;
402:  border-radius:0!important;
403:  background:transparent!important;
404:  box-shadow:none!important;
405:  filter:none!important;
406:  color:var(--accent2,var(--accent))!important;
407:  font-family:Arial,sans-serif!important;
408:  font-size:0!important;
409:  font-weight:900!important;
410:  line-height:1!important;
411:  text-shadow:none!important;
412:  transition:transform .15s ease!important
413: }
414: .tp-global-chevron::before{
415:  content:"›"!important;
416:  display:block!important;
417:  color:currentColor!important;
418:  font-size:36px!important;
419:  font-weight:900!important;
420:  line-height:.82!important
421: }
422: details[open]>summary .tp-global-chevron.tp-global-chevron-rotates,
423: details[open]>summary>.tp-global-chevron.tp-global-chevron-rotates,
424: .tp169-pulse-journal.tp169-open .tp169-pulse-summary>.tp-global-chevron.tp-global-chevron-rotates,
425: .tp168-weight-journal.tp168-open .tp168-weight-summary>.tp-global-chevron.tp-global-chevron-rotates,
426: .rf-history-health-panel:not(.collapsed) .tp-global-chevron.tp-global-chevron-rotates{
427:  transform:rotate(90deg)!important
428: }
429: details:not([open])>summary .tp-global-chevron.tp-global-chevron-rotates,
430: .tp169-pulse-journal:not(.tp169-open) .tp169-pulse-summary>.tp-global-chevron.tp-global-chevron-rotates,
431: .tp168-weight-journal:not(.tp168-open) .tp168-weight-summary>.tp-global-chevron.tp-global-chevron-rotates,
432: .rf-history-health-panel.collapsed .tp-global-chevron.tp-global-chevron-rotates{
433:  transform:none!important
434: }
435: 
436: /* Legacy pseudo-chevron hosts use the same token/component language. */
437: .tp-global-chevron-host{position:relative}
438: .tp-global-chevron-host::after{
439:  content:"›"!important;
440:  color:var(--accent2,var(--accent))!important;
441:  font-family:Arial,sans-serif!important;
442:  font-size:36px!important;
443:  font-weight:900!important;
444:  line-height:.82!important;
445:  text-shadow:none!important;
446:  filter:none!important;
447:  background:transparent!important;
448:  border:0!important;
449:  box-shadow:none!important;
450:  transform:none!important;
451:  transition:transform .15s ease!important
452: }
453: details[open]>.tp-global-chevron-host.tp-global-chevron-host-rotates::after,
454: details[open]>summary.tp-global-chevron-host.tp-global-chevron-host-rotates::after{
455:  transform:rotate(90deg)!important
456: }
457: 
458: /* Pulse sparkline gets a protected right-side lane before the global chevron. */
459: .tp169-pulse-summary>.tp5-trend-mini{
460:  box-sizing:border-box!important;
461:  min-width:0!important;
462:  max-width:118px!important;
463:  margin-right:6px!important;
464:  padding-right:8px!important;
465:  overflow:hidden!important
466: }
467: .tp169-pulse-summary>.tp5-trend-mini .tp5-pulse-mini-svg{
468:  display:block!important;
469:  width:100%!important;
470:  max-width:100%!important;
471:  overflow:hidden!important
472: }
473: @media(max-width:390px){
474:  .tp169-pulse-summary>.tp5-trend-mini{max-width:86px!important;margin-right:5px!important;padding-right:7px!important}
475: }
476: @media(max-width:340px){
477:  .tp169-pulse-summary>.tp5-trend-mini{display:none!important}
478: }
479: 
480: /* Matte/basic themes must stay matte on Health. Vivid themes may keep accent glow. */
481: html:not([data-tp-theme-family="vivid"]) body.tp168-health-view :is(
482:  .tp168-today-card,.tp168-metric,.tp168-status,.tp168-trend,.tp169-pulse-journal,
483:  .tp168-weight-journal,.tp168-more-panel,.tp168-connect-panel,.tp168-sync-tail,
484:  .tp168-health-note,.tp168-icon,.tp169-pulse-body,.tp169-body-inline,.tp169-weight-entry,
485:  .tp168-weight-recent>div,.tp5-pulse-today,.tp5-pulse-trend-section
486: ){
487:  box-shadow:none!important;
488:  filter:none!important;
489:  text-shadow:none!important
490: }
491: html:not([data-tp-theme-family="vivid"]) body.tp168-health-view main.tp168-health{
492:  filter:none!important
493: }
494: 
495: /* #41 follow-up: the global token wins over legacy mixed-color Health arrows. */
496: main.tp168-health :is(.tp168-status>b,.tp169-pulse-summary>b,.tp168-weight-summary>b).tp-global-chevron,
497: main.tp168-health .tp-global-chevron{
498:  color:var(--accent2,var(--accent))!important;
499:  text-shadow:none!important;
500:  filter:none!important
501: }
502: main.tp168-health .tp-global-chevron::before{
503:  color:currentColor!important
504: }
505: 
506: /* #41 workout override: older inline icon rules must not erase the shared glyph. */
507: html body .tp1511-training summary i.tp-global-chevron::before{
508:  content:"›"!important;
509:  display:block!important;
510:  font-size:36px!important;
511:  line-height:.82!important;
512:  color:currentColor!important;
513:  font-style:normal!important;
514:  opacity:1!important;
515:  visibility:visible!important
516: }
517: 
518: /* #41: Library disclosure uses the shared chevron, never the old clipped triangle. */
519: .tp-library-card>summary.tp-global-chevron-host::after{
520:  content:"›"!important;
521:  display:inline-grid!important;
522:  place-items:center!important;
523:  width:30px!important;
524:  height:36px!important;
525:  min-width:30px!important;
526:  min-height:36px!important;
527:  margin:0!important;
528:  padding:0!important;
529:  color:var(--accent2,var(--accent))!important;
530:  font:900 36px/.82 Arial,sans-serif!important;
531:  clip-path:none!important;
532:  background:none!important;
533:  border:0!important;
534:  box-shadow:none!important;
535:  filter:none!important;
536:  text-shadow:none!important
537: }
538: 
539: 
540: /* Phase 5 #41: final specificity guard against legacy white triangle pseudo-elements. */
541: html body #app .tp-global-chevron{
542:  color:var(--accent2,var(--accent))!important;
543:  width:30px!important;min-width:30px!important;max-width:30px!important;
544:  height:36px!important;min-height:36px!important;max-height:36px!important;
545:  flex:0 0 30px!important;clip-path:none!important;
546:  opacity:1!important;visibility:visible!important;
547:  background:transparent!important;border:0!important;box-shadow:none!important;
548:  overflow:visible!important;
549: }
550: html body #app .tp-global-chevron::before{
551:  content:"›"!important;display:block!important;
552:  width:auto!important;min-width:0!important;max-width:none!important;
553:  height:auto!important;min-height:0!important;max-height:none!important;
554:  clip-path:none!important;background:none!important;border:0!important;
555:  color:currentColor!important;font:900 36px/.82 Arial,sans-serif!important;
556:  opacity:1!important;visibility:visible!important;
557:  transform:none!important;text-shadow:none!important;filter:none!important;
558: }
559: html body #app details[open]>summary .tp-global-chevron.tp-global-chevron-rotates,
560: html body #app .tp169-pulse-journal.tp169-open .tp169-pulse-summary>.tp-global-chevron.tp-global-chevron-rotates,
561: html body #app .tp168-weight-journal.tp168-open .tp168-weight-summary>.tp-global-chevron.tp-global-chevron-rotates{
562:  transform:rotate(90deg)!important;
563: }
564: html body #app .tp-library-card>summary.tp-global-chevron-host::after{
565:  content:"›"!important;clip-path:none!important;
566:  background:none!important;color:var(--accent2,var(--accent))!important;
567:  display:inline-grid!important;place-items:center!important;
568:  width:30px!important;min-width:30px!important;max-width:30px!important;
569:  height:36px!important;min-height:36px!important;max-height:36px!important;
570:  font:900 36px/.82 Arial,sans-serif!important;
571:  border:0!important;box-shadow:none!important;filter:none!important;
572: }
573: html body #app .tp-library-card[open]>summary.tp-global-chevron-host::after{
574:  transform:rotate(90deg)!important;
575: }
576: 
577: /* Phase 5 #44 — shared Today six-card grid: compact Home, near-square Health. */
578: #app .tp5-today-grid{display:grid!important;grid-template-columns:repeat(3,minmax(0,1fr))!important;gap:7px!important;width:100%;min-width:0}
579: #app .tp5-today-metric{box-sizing:border-box;min-width:0!important;margin:0!important;display:flex!important;flex-direction:column!important;align-items:flex-start!important;justify-content:flex-end!important;gap:4px!important;overflow:hidden!important;text-align:left!important;color:var(--text)!important;border:1px solid color-mix(in srgb,var(--accent) 20%,var(--line))!important;border-radius:13px!important;background:color-mix(in srgb,var(--accent) 4%,var(--card2))!important;box-shadow:none!important;appearance:none;-webkit-appearance:none}
580: #app button.tp5-today-metric{cursor:pointer}#app button.tp5-today-metric:active{transform:scale(.985)}
581: #app .tp5-today-icon{width:24px;height:24px;flex:0 0 24px;display:grid;place-items:center;color:var(--accent2)}
582: #app .tp5-today-icon svg{width:23px;height:23px;display:block;overflow:visible}
583: #app .tp5-today-label{display:block;flex:0 0 auto;max-width:100%;min-width:0;margin-top:auto;font-weight:700;font-size:11px;line-height:1.12;white-space:normal;overflow-wrap:anywhere;color:var(--muted)}
584: #app .tp5-today-value{display:block;max-width:100%;min-width:0;font-size:clamp(13px,3.9vw,19px);line-height:1.15;font-weight:900;overflow-wrap:anywhere;color:var(--text)}
585: #app main.tp168-health .tp5-today-grid.tp5-today-full{margin:1px 0 11px!important}
586: #app main.tp168-health .tp5-today-full>.tp5-today-metric.stat{aspect-ratio:1/1;min-height:102px!important;padding:11px 9px!important}
587: #app main.rf221-home #rf223Today.tp5-home-today{padding:8px 10px!important;overflow:visible}
588: #app main.rf221-home #rf223Today .rf223-title{margin:0 0 7px!important;line-height:1.15}
589: #app main.rf221-home #rf223Today .tp5-today-grid.tp5-today-compact{gap:5px!important}
590: #app main.rf221-home #rf223Today .tp5-today-metric.stat{aspect-ratio:auto!important;min-height:62px!important;height:auto!important;padding:6px 7px!important;gap:2px!important}
591: #app main.rf221-home #rf223Today .tp5-today-icon{width:16px;height:16px;flex-basis:16px}
592: #app main.rf221-home #rf223Today .tp5-today-icon svg{width:16px;height:16px}
593: #app main.rf221-home #rf223Today .tp5-today-label{font-size:10px;line-height:1.06}
594: #app main.rf221-home #rf223Today .tp5-today-value{font-size:13px;line-height:1.08;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
595: @media(max-width:360px){#app .tp5-today-grid{gap:5px!important}#app main.tp168-health .tp5-today-full>.tp5-today-metric.stat{min-height:97px!important;padding:8px 6px!important}#app .tp5-today-label{font-size:10px}#app .tp5-today-value{font-size:13px}#app main.rf221-home #rf223Today .tp5-today-metric.stat{min-height:59px!important;padding:5px!important}#app main.rf221-home #rf223Today .tp5-today-label{font-size:9px}#app main.rf221-home #rf223Today .tp5-today-value{font-size:12px}}
596: 
597: /* Phase 5 #41 selective final guard + #44 compact Health.
598:    Only disclosure controls: leave play icons and non-disclosure graphics untouched. */
599: html body:not(#tp5-chevron-guard) .tp-select-trigger::after,
600: html body:not(#tp5-chevron-guard) .tp160-journal-disclosure::before,
601: html body:not(#tp5-chevron-guard) .rf148-profile-accordion>summary::after{
602:  content:"›"!important;display:inline-grid!important;place-items:center!important;
603:  box-sizing:border-box!important;width:24px!important;min-width:24px!important;max-width:24px!important;
604:  height:24px!important;min-height:24px!important;max-height:24px!important;
605:  flex:0 0 24px!important;margin:0!important;padding:0!important;
606:  clip-path:none!important;background:none!important;border:0!important;
607:  border-radius:0!important;box-shadow:none!important;
608:  -webkit-mask:none!important;mask:none!important;
609:  filter:none!important;text-shadow:none!important;overflow:visible!important;
610:  color:var(--accent2,var(--accent))!important;font:900 30px/1 Arial,sans-serif!important;
611: }
612: html body:not(#tp5-chevron-guard) .tp160-journal-disclosure{
613:  width:24px!important;min-width:24px!important;height:24px!important;min-height:24px!important;
614:  flex:0 0 24px!important;margin-left:6px!important;align-self:center!important;
615: }
616: html body:not(#tp5-chevron-guard) .tp-library-card>summary::after{
617:  content:"›"!important;display:inline-grid!important;place-items:center!important;
618:  box-sizing:border-box!important;width:30px!important;min-width:30px!important;max-width:30px!important;
619:  height:36px!important;min-height:36px!important;max-height:36px!important;
620:  flex:0 0 30px!important;padding:0!important;clip-path:none!important;
621:  background:none!important;border:0!important;border-radius:0!important;
622:  box-shadow:none!important;filter:none!important;text-shadow:none!important;
623:  -webkit-mask:none!important;mask:none!important;
624:  color:var(--accent2,var(--accent))!important;font:900 36px/.82 Arial,sans-serif!important;
625: }
626: /* Health 2x3 is deliberately flatter than the Home/Health initial square concept.
627:    Keep the Home compact grid unchanged and protect 320px layouts. */
628: #app main.tp168-health .tp5-today-grid.tp5-today-full{gap:6px!important;margin:1px 0 8px!important}
629: #app main.tp168-health .tp5-today-full>.tp5-today-metric.stat{
630:  aspect-ratio:auto!important;min-height:82px!important;height:82px!important;
631:  padding:8px 8px!important;gap:3px!important;justify-content:flex-end!important;
632: }
633: #app main.tp168-health .tp5-today-full .tp5-today-icon{width:19px;height:19px;flex-basis:19px}
634: #app main.tp168-health .tp5-today-full .tp5-today-icon svg{width:19px;height:19px}
635: #app main.tp168-health .tp5-today-full .tp5-today-label{font-size:10.5px;line-height:1.08}
636: #app main.tp168-health .tp5-today-full .tp5-today-value{font-size:clamp(13px,3.7vw,17px);line-height:1.1}
637: @media(max-width:360px){
638:  #app main.tp168-health .tp5-today-full>.tp5-today-metric.stat{
639:   min-height:78px!important;height:78px!important;padding:7px 5px!important;gap:2px!important
640:  }
641:  #app main.tp168-health .tp5-today-full .tp5-today-value{font-size:13px}
642: }
643: 
644: 
645: /* Phase 5 #41 phone-audit: remaining legacy triangles reported 2026-09-24. */
646: html body #app .tp155-journal-filter>summary>.tp160-journal-disclosure::before,
647: html body #tp155R4PanelHost .tp-select-trigger::after,
648: html body #tp155R4PanelHost[data-panel="quick"] .tp155-quick-filter>summary::after,
649: html body #tp155R4PanelHost[data-panel="quick"] .tp152-quick-row>summary .tp152-chevron::before{
650:  content:"›"!important;
651:  display:inline-grid!important;
652:  place-items:center!important;
653:  box-sizing:border-box!important;
654:  width:24px!important;
655:  min-width:24px!important;
656:  max-width:24px!important;
657:  height:24px!important;
658:  min-height:24px!important;
659:  max-height:24px!important;
660:  margin:0!important;
661:  padding:0!important;
662:  border:0!important;
663:  border-radius:0!important;
664:  background:none!important;
665:  box-shadow:none!important;
666:  clip-path:none!important;
667:  -webkit-clip-path:none!important;
668:  -webkit-mask:none!important;
669:  mask:none!important;
670:  filter:none!important;
671:  text-shadow:none!important;
672:  overflow:visible!important;
673:  color:var(--accent2,var(--accent))!important;
674:  font:900 30px/1 Arial,sans-serif!important;
675:  line-height:24px!important;
676:  transform:none!important;
677:  transform-origin:50% 50%!important;
678: }
679: html body #app .tp155-journal-filter[open]>summary>.tp160-journal-disclosure::before,
680: html body #tp155R4PanelHost .tp-select.open>.tp-select-trigger::after,
681: html body #tp155R4PanelHost[data-panel="quick"] .tp155-quick-filter[open]>summary::after,
682: html body #tp155R4PanelHost[data-panel="quick"] .tp152-quick-row[open]>summary .tp152-chevron::before{
683:  transform:rotate(90deg)!important;
684: }
685: html body #tp155R4PanelHost[data-panel="quick"] .tp155-quick-filter>summary::after{
686:  margin-left:auto!important;
687:  flex:0 0 24px!important;
688: }
689: html body #tp155R4PanelHost[data-panel="quick"] .tp152-quick-row>summary .tp152-chevron{
690:  color:var(--accent2,var(--accent))!important;
691:  width:24px!important;
692:  min-width:24px!important;
693:  max-width:24px!important;
694:  height:24px!important;
695:  min-height:24px!important;
696:  max-height:24px!important;
697:  flex:0 0 24px!important;
698:  margin-left:6px!important;
699: }
700: 
701: 
702: /* #49 — lazy Journal exercise picker: full-width branded menu on its first open.
703:    Scope these rules to the single expanded workout; no eager card hydration. */
704: #app .tp3-add-exercise-body{grid-template-columns:minmax(0,1fr)}
705: #app .tp3-add-exercise-body>.tp-select{grid-column:1;width:100%;min-width:0}
706: #app .tp3-add-exercise-body>.btn{grid-column:1;justify-self:stretch;width:100%!important}
707: #app .tp3-add-exercise-body .tp-select-option{align-items:flex-start;white-space:normal;min-height:44px}
708: #app .tp3-add-exercise-body .tp-select-menu .tp-select-label{
709:   min-width:0;overflow:visible;text-overflow:clip;white-space:normal;
710:   overflow-wrap:anywhere;line-height:1.35
711: }
712: /* Shared Health detail disclosure: one frame, two headings, existing theme tokens. */
713: #app main.tp168-health>.tp168-more-panel[open]{flex-shrink:0!important}
714: #app main.tp168-health>.tp168-more-panel>summary{list-style:none!important}
715: #app main.tp168-health>.tp168-more-panel>summary::marker{content:""}
716: #app main.tp168-health>.tp168-more-panel>summary::-webkit-details-marker{display:none}
717: #app main.tp168-health>.tp168-more-panel>summary::before,
718: #app main.tp168-health>.tp168-more-panel>summary::after{content:none!important;display:none!important}
719: #app .tp-health-details{padding:0 10px 10px}
720: #app .tp-health-details>section+section{margin-top:14px;padding-top:12px;border-top:1px solid var(--line)}
721: #app .tp-health-details h3{margin:0 0 9px;font-size:15px;font-weight:700;color:var(--text)}
722: #app .tp-health-details .grid2{gap:8px;margin:0}
723: #app .tp-health-details .stat{min-width:0;display:flex;flex-direction:column;gap:5px}
724: #app .tp-health-details .stat:focus{outline:2px solid var(--accent);outline-offset:2px}
725: #app .tp-health-details .stat strong{overflow-wrap:anywhere}
726: #app .tp-health-metric-meta,#app .tp-health-pulse-meta{color:var(--muted);font-size:11px;line-height:1.35;font-weight:400}
727: #app .tp-health-pulse-meta{margin:7px 0 0}
728: 
729: /* Health actions wrap as whole buttons instead of breaking Hungarian labels. */
730: .tp168-sync-tail .rf263-sync-actions{display:flex!important;flex-wrap:wrap;align-items:stretch}
731: .tp168-sync-tail .rf263-sync-actions .btn{flex:1 1 max-content;width:auto;min-width:0;overflow-wrap:normal!important;word-break:normal;white-space:normal}
732: html.tp110-large-text .tp168-sync-tail .rf263-sync-actions .btn{flex-basis:100%;width:100%}
733: @media(max-width:360px){
734:  html body #app main.tp168-health .tp168-sync-tail .rf263-sync-actions>.btn{flex-basis:calc((100% - 7px)/2);font-size:12px!important;padding:8px 6px!important}
735:  html.tp110-large-text body #app main.tp168-health .tp168-sync-tail .rf263-sync-actions>.btn{flex-basis:100%}
736: }
```

## www/global-uiux-177.css — 1–229

Ref: `7473b30f387eda54186c1e31830304ca12bd104f`; teljes forrás SHA256: `1bd8d7244ebbbee586ca989060d0ed3e79f4f74fce0fc0c62ceaa3cf899461ca`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/global-uiux-177.css

```text
1: /* #58 visual-only Health-reference pass. NO geometry, DOM, component or event changes.
2:    The active Health page uses the app's resolved palette, not its obsolete
3:    early 1.6.8 blue fallback. Keep the existing fonts, sizes, radii and layout. */
4: :root{
5:   --tp-ui-bg:#0e1015;
6:   --tp-ui-card:#1a1e25;
7:   --tp-ui-card-raised:#232933;
8:   --tp-ui-border:#2c3440;
9:   --tp-ui-border-size:1px; /* Reference thickness on every enclosing card. */
10:   --tp-ui-font:Inter,system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif;
11:   --tp-ui-hero-border:linear-gradient(120deg,
12:     color-mix(in srgb,var(--accent) 90%,var(--tp-ui-border)),
13:     color-mix(in srgb,var(--accent2) 42%,var(--tp-ui-border)) 62%,
14:     var(--tp-ui-border));
15:   --tp-ui-vivid-glow:0 0 18px color-mix(in srgb,var(--accent) 24%,transparent);
16: }
17: html,html body,html body.tp168-health-view,
18: html[data-tp-theme-family="vivid"] body.tp168-health-view{
19:   background:var(--tp-ui-bg)!important;
20: }
21: /* Same opaque secondary-card fill and neutral hairline from Health to
22:    Home, Workout, Programs, Journal, and all floating-panel interiors.
23:    Only presentation properties: never touch display, grid, padding, margin,
24:    positions, dimensions, click handlers or close-button geometry. */
25: html body :is(#app,#tp155R4PanelHost) :is(
26:   .card,.hero,.stat,.history,.setting,.weight-row,
27:   .tp151-calendar-frame,.tp151-day-panel,
28:   .tp152-active-program,.tp152-program-card,.tp152-program-day,
29:   .tp152-day,.tp152-exercise,.tp152-quick-row,.tp152-inline-add,
30:   .tp152-accordion,.tp152-history-editor,.tp1511-training,
31:   .tp155-journal-filter,.tp155-r4-panel,
32:   .tp168-metric,.tp168-status,.tp168-trend,.tp168-weight-journal,
33:   .tp169-pulse-journal,.tp168-health-note
34: ){
35:   background:var(--tp-ui-card)!important;
36:   border-color:var(--tp-ui-border)!important;
37:   box-shadow:none!important;
38: }
39: /* Preserve the original 3px/2px structural widths: color only. */
40: html body #app :is(.tp152-program-card,.tp152-active-program){
41:   border-left-color:var(--tp-ui-border)!important;
42: }
43: /* In the Program library, only the selected card gets a SOLID theme-
44:    colored hairline. The old per-program blue left stripe is removed. */
45: html body #app .tp152-program-card.active-program{
46:   border-color:color-mix(in srgb,var(--accent) 65%,var(--tp-ui-border))!important;
47: }
48: html body :is(#app,#tp155R4PanelHost) :is(
49:   .tp152-preview-ex,.history-head,.tp152-quick-body
50: ){background:var(--tp-ui-card-raised)!important;}
51: 
52: /* Explicit existing classes instead of runtime decorators: no DOM changes.
53:    Exactly one main hero per view (Health Today, Coach Today, Home active,
54:    Plan active or quick start, current Workout head). */
55: html body #app main.rf221-home>.hero.tp155-home-active-card,
56: html body #app main.rf221-home>#rf220CoachCard.tp166-home-coach,
57: html body #app main.tp152-plan>:is(.tp152-active-program,.tp124-active-program-frame),
58: html body #app main.tp152-plan>.tp150-quick-entry:first-child:not(:has(~ :is(.tp152-active-program,.tp124-active-program-frame))),
59: html body #app main.tp153-workout .tp153-workout-head,
60: html body #app main.tp168-health>.tp168-today-card,
61: html body #app main.rf263-health>.tp151-health-card,
62: html body #tp155R4PanelHost[data-panel="coach"] .tp151-coach-recommendation{
63:   border-color:transparent!important;
64:   background:linear-gradient(var(--tp-ui-card),var(--tp-ui-card)) padding-box,
65:     var(--tp-ui-hero-border) border-box!important;
66:   box-shadow:none!important;
67:   filter:none!important;
68: }
69: /* Vivid/neon ONLY; basic themes never get glow. */
70: html[data-tp-theme-family="vivid"] body #app main.rf221-home>.hero.tp155-home-active-card,
71: html[data-tp-theme-family="vivid"] body #app main.rf221-home>#rf220CoachCard.tp166-home-coach,
72: html[data-tp-theme-family="vivid"] body #app main.tp152-plan>:is(.tp152-active-program,.tp124-active-program-frame),
73: html[data-tp-theme-family="vivid"] body #app main.tp152-plan>.tp150-quick-entry:first-child:not(:has(~ :is(.tp152-active-program,.tp124-active-program-frame))),
74: html[data-tp-theme-family="vivid"] body #app main.tp153-workout .tp153-workout-head,
75: html[data-tp-theme-family="vivid"] body #app main.tp168-health>.tp168-today-card,
76: html[data-tp-theme-family="vivid"] body #app main.rf263-health>.tp151-health-card,
77: html[data-tp-theme-family="vivid"] body #tp155R4PanelHost[data-panel="coach"] .tp151-coach-recommendation{
78:   box-shadow:var(--tp-ui-vivid-glow)!important;
79: }
80: /* Basic palettes: flat button/cards/selection/progress treatment, leaving
81:    backgrounds, typography metrics and existing press/toggle logic intact. */
82: html[data-tp-theme-family="basic"] body :is(#app,#tp155R4PanelHost) :is(
83:   .btn,button,.tab,.card,.hero,.stat,.setting,.history,.progress,
84:   .tp-select-trigger,.tp-select-menu,.tp162-theme-columns,
85:   .tp155-r4-panel,.tp155-r4-panel-close,.tp152-program-card,
86:   .tp152-active-program,.tp152-day,.tp152-exercise,.tp152-quick-row,
87:   .tp155-journal-filter,.tp168-today-card,.tp168-metric,.tp168-status,
88:   .tp168-trend,.tp168-weight-journal,.tp169-pulse-journal,
89:   .tp151-coach-recommendation,.tp166-home-coach,.tp153-workout-head
90: ){
91:   box-shadow:none!important;
92:   filter:none!important;
93:   text-shadow:none!important;
94: }
95: html[data-tp-theme-family="basic"] body :is(
96:   .top.tp154-nav-grid,.tp-temporal-modal,.video-modal,.tp15-demo-dialog
97: ){box-shadow:none!important;filter:none!important;}
98: html[data-tp-theme-family="basic"] body :is(#app,#tp155R4PanelHost) :is(
99:   .progress,.progress>div
100: ){box-shadow:none!important;filter:none!important;}
101: 
102: /* #137 r4: theme families apply to every existing accent control, not just
103:    the hero. Static shadows only: no filter, extra overlay, animated shadow
104:    or permanently promoted layer. Neutral cards and semantic red/green
105:    actions retain their own appearance. The moving navigation marker copies
106:    its active button's style; never paint a second highlight on that button. */
107: html[data-tp-theme-family="basic"] body #app main.rf221-home>.hero.tp155-home-active-card,
108: html[data-tp-theme-family="basic"] body #app main.rf221-home>#rf220CoachCard.tp166-home-coach,
109: html[data-tp-theme-family="basic"] body #app main.tp152-plan>:is(.tp152-active-program,.tp124-active-program-frame),
110: html[data-tp-theme-family="basic"] body #app main.tp152-plan>.tp150-quick-entry:first-child:not(:has(~ :is(.tp152-active-program,.tp124-active-program-frame))),
111: html[data-tp-theme-family="basic"] body #app main.tp153-workout .tp153-workout-head,
112: html[data-tp-theme-family="basic"] body #app main.tp168-health>.tp168-today-card,
113: html[data-tp-theme-family="basic"] body #app main.rf263-health>.tp151-health-card,
114: html[data-tp-theme-family="basic"] body #tp155R4PanelHost[data-panel="coach"] .tp151-coach-recommendation{
115:   background:var(--tp-ui-card)!important;
116:   border-color:color-mix(in srgb,var(--accent) 65%,var(--tp-ui-border))!important;
117: }
118: html[data-tp-theme-family="basic"] body :is(
119:   .btn.tp-primary,.btn.tp-emphasis,.tp146-day-letter,.check.done,.progress>div,
120:   .tp155-theme-option.active,.tp1511-theme-chip.active,.tp-play-btn,
121:   .tp154-nav-cell.active,.tp154-nav-cell.tp-action-active,
122:   .tp177-journal-tabs .active,.tp107-statistics-tabs .active,.tp177-periods .active
123: ):not(.tp137-selected){
124:   background-image:none!important;
125:   box-shadow:none!important;
126:   filter:none!important;
127:   text-shadow:none!important;
128: }
129: html[data-tp-theme-family="basic"] body :is(
130:   .tp154-nav-cell.active,.tp154-nav-cell.tp-action-active,
131:   .tp177-journal-tabs .active,.tp107-statistics-tabs .active,.tp177-periods .active
132: ):not(.tp137-selected){
133:   background-color:color-mix(in srgb,var(--accent) 16%,var(--tp-ui-card-raised))!important;
134: }
135: html[data-tp-theme-family="basic"] body :is(
136:   .tp137-selection,.tp146-day-letter,.cal-cell>b,.exercise .num,
137:   .tp152-exercise .num,.tp152-exercise .meta,
138:   .tp-temporal-day,.rf263-date-day,.tp-time-option,.tp155-time-option
139: ){box-shadow:none!important;text-shadow:none!important;filter:none!important;}
140: /* The legacy Health-only navigation rules have higher specificity than
141:    ordinary controls. Resolve the family before the marker copies them. */
142: html[data-tp-theme-family="basic"] body .top.tp154-nav-grid .tp154-nav-cell:is(.active,.tp-action-active):not(.tp137-selected){
143:   background:color-mix(in srgb,var(--accent) 16%,var(--tp-ui-card-raised))!important;
144:   box-shadow:none!important;
145: }
146: html[data-tp-theme-family="vivid"] body .top.tp154-nav-grid .tp154-nav-cell:is(.active,.tp-action-active):not(.tp137-selected){
147:   box-shadow:var(--tp-ui-vivid-glow)!important;
148: }
149: html[data-tp-theme-family="vivid"] body :is(
150:   .btn.tp-primary:not(.danger):not(:disabled):not([onclick*="saveSettings"]),
151:   .btn.tp-emphasis:not(.danger):not(:disabled),
152:   .tp154-nav-cell.active,.tp154-nav-cell.tp-action-active,
153:   .tp177-journal-tabs .active,.tp107-statistics-tabs .active,.tp177-periods .active,
154:   .cal-cell.tp-selected-day,.tp-temporal-day[aria-pressed="true"],
155:   .rf263-date-day[aria-pressed="true"],.tp-time-option[aria-pressed="true"],
156:   .tp155-time-option[aria-pressed="true"],.tp1511-theme-chip.active,
157:   .tp155-theme-option.active,.tp162-theme-dropdown.open>.tp-select-trigger,.tp-play-btn,
158:   .tp-select.open .tp-select-trigger,.tp-select-option[aria-selected="true"],
159:   .btn.tp-selected:not(.danger),.rf232-effort .btn:not(.secondary),
160:   .rf232-pain:not(.secondary),
161:   .tp146-day-letter,.check.done,.progress>div
162: ):not(.tp137-selected){box-shadow:var(--tp-ui-vivid-glow)!important;}
163: html[data-tp-theme-family="vivid"] body #app .tp152-program-card.active-program{
164:   box-shadow:var(--tp-ui-vivid-glow)!important;
165: }
166: /* Small accent glyphs use a modest text halo rather than a shadow around
167:    the whole neutral exercise/calendar card. Icons keep their crisp shape. */
168: html[data-tp-theme-family="vivid"] body :is(#app,#tp155R4PanelHost) :is(
169:   .cal-cell>b,.tp146-day-letter,.exercise .num,.tp152-exercise .num,
170:   .tp152-exercise .meta
171: ){text-shadow:0 0 8px color-mix(in srgb,var(--accent) 40%,transparent)!important;}
172: /* Typography: retain the existing Inter/system stack and all sizes, line
173:    heights, font weights, wrapping and spacing; no new font downloads. */
174: 
175: /* #58 border-consistency follow-up: Health's 1px visible card outlines.
176:    Compensate the former 2px Home/Plan borders with padding; preserve the
177:    Program card's original 3px border BOX and paint only 1px of its edge. */
178: html body #app main.rf221-home > .hero.tp155-home-active-card {
179:   border-width:var(--tp-ui-border-size)!important;
180:   padding:11px 13px!important; /* Was 2px border + 10px 12px padding. */
181: }
182: html body #app main.tp152-plan > .tp152-active-program {
183:   border-width:var(--tp-ui-border-size)!important;
184:   padding:1px!important; /* Was 2px border + 0px padding on each side. */
185: }
186: /* Program rows have a legacy 3px left-border *box*; shrinking it
187:    moves the summary grid. Retain the invisible 3px geometry and paint just
188:    the first pixel via a border-box background gradient. All four VISIBLE
189:    edges are 1px, including selected rows; neither label nor hitbox moves. */
190: html body #app main.tp150-programs-compact .tp152-program-card {
191:   border-left-color:transparent!important;
192:   background:
193:     linear-gradient(to right,var(--tp-ui-border) 0 1px,transparent 1px),
194:     var(--tp-ui-card)!important;
195:   background-origin:border-box!important;
196:   background-clip:border-box!important;
197: }
198: html body #app main.tp150-programs-compact .tp152-program-card.active-program {
199:   border-left-color:transparent!important;
200:   background:
201:     linear-gradient(to right,color-mix(in srgb,var(--accent) 65%,var(--tp-ui-border)) 0 1px,transparent 1px),
202:     var(--tp-ui-card)!important;
203:   background-origin:border-box!important;
204:   background-clip:border-box!important;
205: }
206: /* A containing Programs card already has all four sides; unify its
207:    group outline too, but do not box the three separate action buttons. */
208: html body #app main.tp150-programs-compact .tp150-programs-frame {
209:   background:var(--tp-ui-card)!important;
210:   border-color:var(--tp-ui-border)!important;
211: }
212: 
213: /* Two currently unframed, meaningful interaction units can benefit from
214:    the same closed 1px outline. Outlines do NOT participate in the box model:
215:    the Workout set controls and Coach metric tap targets keep exact geometry. */
216: html body #app main.tp153-workout .tp153-set-row {
217:   outline:1px solid var(--tp-ui-border)!important;
218:   outline-offset:2px!important;
219:   border-radius:12px; /* Visual outline only; not a new container/padding. */
220: }
221: html body #tp155R4PanelHost[data-panel="coach"] .tp153-coach-metric:not(:focus-visible),
222: html body #tp155R4PanelHost[data-panel="coach"] .tp151-coach-target > span {
223:   outline:1px solid var(--tp-ui-border)!important;
224:   outline-offset:-1px!important;
225: }
226: /* Focus-visible retains the existing thicker accessibility focus ring.
227:    Inner Program-day rows remain single separators: double boxing them
228:    would crowd the hierarchy. Floating panels deliberately remain joined
229:    to the sticky header and must not gain an extra top border. */
```

## www/health-journal-120.css — 1–58

Ref: `7473b30f387eda54186c1e31830304ca12bd104f`; teljes forrás SHA256: `11d9631426f7b4b1502751d3cb73dc8bed16c75bbfee219548fc978a0362f1c6`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/health-journal-120.css

```text
1: /* Compact Home action and inline Health journal share the established page widths. */
2: html body #app main.rf221-home>.tp120-home-goals{display:flex!important;align-items:center;justify-content:space-between;gap:10px;min-height:38px!important;margin:0!important;padding:7px 12px!important;font-size:13px!important;line-height:1.2!important;border:1px solid var(--line);border-radius:17px;box-sizing:border-box;width:100%;background:var(--card)}
3: .tp120-chevron{width:8px;height:8px;border-right:3px solid var(--accent);border-top:3px solid var(--accent);transform:rotate(45deg);margin-right:5px;flex:0 0 auto}
4: .tp120-journal{display:grid;gap:10px;min-width:0}
5: html body #app main.tp168-health>.tp120-health-journal{padding:0!important;margin:0!important;border-radius:16px!important;min-width:0}
6: .tp120-health-journal>.tp120-journal-entry{display:flex;align-items:center;justify-content:space-between;gap:10px;min-height:44px;padding:5px 12px;box-sizing:border-box;cursor:pointer;list-style:none;font-size:14px;line-height:1.25}
7: .tp120-health-journal>.tp120-journal-entry>strong{flex:1;min-width:0}
8: .tp120-health-journal>.tp120-journal-entry .tp168-icon{width:30px;height:30px;flex-basis:30px;border-radius:10px}
9: .tp120-health-journal>.tp120-journal-entry .tp168-chevron{width:26px;height:26px}
10: .tp120-health-journal>.tp120-journal-entry::-webkit-details-marker{display:none}
11: .tp120-health-journal[open]>.tp120-journal-entry .tp168-chevron{transform:rotate(90deg)!important}
12: .tp120-health-journal .tp120-journal{margin:0 10px 10px;padding:0;width:auto;max-width:none}
13: .tp120-journal label{display:grid;gap:6px;min-width:0;font-size:13px;font-weight:700}
14: .tp120-filters{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}
15: .tp120-journal :is(input,select){box-sizing:border-box;min-width:0;max-width:100%}
16: .tp120-preferences{padding:10px!important;margin:0!important}
17: .tp120-preferences summary{cursor:pointer;list-style:none;display:flex;justify-content:space-between;align-items:center;min-height:32px}
18: .tp120-preferences summary::-webkit-details-marker{display:none}
19: .tp120-preferences summary::after{content:"";width:8px;height:8px;border-right:3px solid var(--accent);border-bottom:3px solid var(--accent);transform:rotate(-45deg);margin-right:5px;transition:transform .2s}
20: .tp120-preferences[open] summary::after{transform:rotate(45deg)}
21: .tp120-preferences>label{margin:10px 0}
22: .tp120-journal .tp120-check{display:flex;align-items:center;gap:10px;min-height:44px;line-height:1.35}
23: .tp120-check input{width:26px;height:26px;min-width:26px;flex:0 0 auto;accent-color:var(--accent)}
24: .tp120-journal :is(p,h3){margin:3px 0;overflow-wrap:anywhere}
25: .tp120-journal button{white-space:normal}
26: .tp120-record,.tp120-day{display:grid;gap:5px;border:1px solid var(--line);border-radius:17px;background:var(--card);padding:12px;margin:9px 0;overflow-wrap:anywhere;min-width:0}
27: .tp120-record>div{display:flex;justify-content:space-between;gap:10px;flex-wrap:wrap}
28: .tp120-record small,.tp120-day>small{color:var(--muted);font-size:12px}
29: .tp120-metrics{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;margin:4px 0}
30: .tp120-metrics>div{display:grid;gap:3px;padding:7px;border:1px solid var(--line);border-radius:12px}
31: .tp120-metrics small{font-size:11px;color:var(--muted)}
32: .tp120-error{color:var(--bad)}
33: @media(prefers-reduced-motion:reduce){.tp120-preferences summary::after{transition:none}}
34: 
35: .tp120-goal-progress{font-size:11px;line-height:1.2;color:var(--accent);margin-left:auto;text-align:right}
36: @media(max-width:340px){html body #app main.rf221-home:not(.tp-home-draft-compact)>.onboarding h2{font-size:23px!important;line-height:1.15!important}}
37: 
38: html body #app main.tp168-health .tp168-sync-tail .rf263-sync-actions{grid-template-columns:repeat(2,minmax(0,1fr))!important}
39: html body #app main.tp168-health .tp168-sync-tail .rf263-sync-actions>.btn{white-space:normal!important;overflow-wrap:anywhere;font-size:12px!important;line-height:1.2!important;padding:8px 6px!important;min-height:44px!important}
40: html.tp110-large-text body #app main.tp168-health .tp168-sync-tail .rf263-sync-actions{grid-template-columns:1fr!important}
41: /* Optional progress never changes the height of the fixed Home overview. */
42: html body #app main.rf221-home>.tp120-home-goals>span:first-child{flex:1;min-width:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;text-align:left}
43: html body #app main.rf221-home>.tp120-home-goals>.tp120-goal-progress{white-space:nowrap;flex:0 0 auto}
44: .tp120-raw{padding:10px!important;margin:0!important;min-width:0}
45: .tp120-raw>summary{cursor:pointer;list-style:none;display:flex;justify-content:space-between;align-items:center;min-height:40px;font-weight:700}
46: .tp120-raw>summary::-webkit-details-marker{display:none}
47: .tp120-raw>summary::after{content:"";width:8px;height:8px;border-right:3px solid var(--accent);border-bottom:3px solid var(--accent);transform:rotate(-45deg);margin-right:5px;transition:transform .2s}
48: .tp120-raw[open]>summary::after{transform:rotate(45deg)}
49: @media(prefers-reduced-motion:reduce){.tp120-raw>summary::after{transition:none}}
50: 
51: /* Provenance uses the same subtle disclosure arrow as journal settings. */
52: .tp120-origin-details{min-width:0;color:var(--muted);font-size:12px}
53: .tp120-origin-details>summary{cursor:pointer;list-style:none;display:flex;align-items:center;justify-content:space-between;min-height:32px;gap:10px}
54: .tp120-origin-details>summary::-webkit-details-marker{display:none}
55: .tp120-origin-details>summary::after{content:"";width:7px;height:7px;border-right:2px solid var(--accent);border-bottom:2px solid var(--accent);transform:rotate(-45deg);margin-right:5px;transition:transform .2s}
56: .tp120-origin-details[open]>summary::after{transform:rotate(45deg)}
57: .tp120-origin-details>small{display:block;overflow-wrap:anywhere}
58: @media(prefers-reduced-motion:reduce){.tp120-origin-details>summary::after{transition:none}}
```

## www/typography-137.css — 1–120

Ref: `7473b30f387eda54186c1e31830304ca12bd104f`; teljes forrás SHA256: `31e7b46701e6f5cb9c8032d29f6f02e29be1a6631302716eb9927ad29e10d7a9`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/typography-137.css

```text
1: /* #137 r5: ship the font instead of relying on the phone's system fallback.
2:    Inter v4.1 (rsms/inter), unmodified variable WOFF2; SIL OFL in inter-license.txt.
3:    One local upright font covers real weights and Hungarian/Romanian accents. */
4: @font-face{
5:   font-family:"TrainPilot Inter";
6:   src:url("trainpilot-inter.woff2") format("woff2");
7:   font-style:normal;
8:   font-weight:100 900;
9:   font-display:swap;
10: }
11: :root{
12:   --tp-ui-font:"TrainPilot Inter",Inter,system-ui,-apple-system,"Segoe UI",Roboto,Arial,sans-serif;
13:   --tp-type-page:24px;
14:   --tp-type-section:18px;
15:   --tp-type-item:16px;
16:   --tp-type-body:14px;
17:   --tp-type-control:15px;
18:   --tp-type-caption:12px;
19: }
20: html body{
21:   font-family:var(--tp-ui-font);
22:   font-size:var(--tp-type-body);
23:   font-weight:400;
24:   font-stretch:normal;
25: }
26: html body :is(#app,#tp155R4PanelHost,.tp-modal,.tp2628-dialog-backdrop,.tp3-planner-panel,.video-modal){
27:   font-family:var(--tp-ui-font)!important;
28: }
29: html body :is(button,input,select,textarea){font-family:var(--tp-ui-font)!important;}
30: html body :is(#app,#tp155R4PanelHost) main h1{
31:   font-size:var(--tp-type-page)!important;
32:   font-weight:800!important;
33:   line-height:1.2!important;
34:   letter-spacing:normal!important;
35: }
36: html body #app main .onboarding h2{
37:   font-size:var(--tp-type-page)!important;
38:   font-weight:800!important;
39:   line-height:1.2!important;
40: }
41: html body :is(#app,#tp155R4PanelHost) main :is(h2,.section){
42:   font-size:var(--tp-type-section)!important;
43:   font-weight:700!important;
44:   line-height:1.25!important;
45:   letter-spacing:normal!important;
46: }
47: html body :is(#app,#tp155R4PanelHost) main :is(
48:   h3,.ex-name,.history-title,.tp1511-head strong,.rf233-exhead strong
49: ){
50:   font-size:var(--tp-type-item)!important;
51:   font-weight:700!important;
52:   line-height:1.25!important;
53: }
54: html body :is(#app,#tp155R4PanelHost) main:not(.tp-home-draft-compact):not(.tp107-health-layout) p:not(.small):not(.tp177-small){
55:   font-size:var(--tp-type-body)!important;
56:   font-weight:400;
57:   line-height:1.45!important;
58: }
59: html body :is(#app,#tp155R4PanelHost) main :is(.small,.meta,.badge){
60:   font-size:var(--tp-type-caption)!important;
61:   line-height:1.3!important;
62: }
63: html body :is(#app,#tp155R4PanelHost) main .meta{font-weight:600!important;}
64: html body :is(#app,#tp155R4PanelHost) main :is(.btn:not([class*="close"]):not([onclick^="changeCalendarMonth"]),.tp-select-trigger,.tp-temporal-trigger){
65:   font-size:var(--tp-type-control)!important;
66:   font-weight:700!important;
67: }
68: html body :is(#app,#tp155R4PanelHost) main :is(.field,input,select,textarea){
69:   font-size:var(--tp-type-body)!important;
70: }
71: /* Longer localized picker values stay within their grid column. */
72: html body :is(#app,#tp155R4PanelHost) .rf2211-planbody .tp-select{width:100%!important;max-width:100%!important;min-width:0!important;}
73: html body :is(#app,#tp155R4PanelHost) .tp-select-trigger{max-width:100%!important;min-width:0!important;}
74: html body :is(#app,#tp155R4PanelHost) .tp-select-value{min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}
75: @media(max-width:340px) and (max-height:800px){
76:   /* Keep all six calendar rows and weekly controls reachable with Inter. */
77:   html body #tp155R4PanelHost[data-panel="calendar"].tp107-fit-calendar .rf2211-planbody{gap:1px!important;}
78: }
79: /* The same advice on Home and in Coach uses the same title/body hierarchy. */
80: html body :is(#app,#tp155R4PanelHost) :is(.tp151-advice,.tp166-home-coach) :is(.tp151-advice>strong,h2){
81:   font-size:var(--tp-type-section)!important;
82:   font-weight:700!important;
83:   line-height:1.25!important;
84: }
85: html body :is(#app,#tp155R4PanelHost) .tp153-coach-metric strong{font-weight:700!important;}
86: /* The fixed Home overview keeps its viewport height when goal metadata
87:    changes the advice text, instead of growing with the font's new metrics. */
88: html body #app main.tp107-fit-home{
89:   height:max(0px,calc(100dvh - var(--tp107-nav-height,132px) - 2px))!important;
90: }
91: /* Narrow summaries are metadata, not additional page headings. */
92: html body #app .tp146-next-title{font-size:var(--tp-type-body)!important;font-weight:600!important;}
93: html[lang] body .top.tp154-nav-grid .tp151-nav-label{
94:   font-size:13px!important;
95:   font-weight:700!important;
96: }
97: @media(max-width:390px){
98:   html[lang] body .top.tp154-nav-grid .tp151-nav-label{font-size:12.5px!important;}
99: }
100: 
101: /* Older compact rules changed icon sizes by 1px and moved centered labels.
102:    Keep identical icon metrics on either side of the scroll threshold. */
103: html[lang] body .top.tp154-nav-grid .tp154-nav-cell :is(.tp151-nav-icon,.tp154-nav-action-icon){font-size:21px!important;}
104: html[lang] body .top.tp154-nav-grid .tp154-settings-action .tp154-nav-action-icon{font-size:24px!important;}
105: 
106: /* Scroll must not shine/blur the page's yellow actions through the main menu.
107:    Keep the same opaque surface in both compact states, without changing its
108:    geometry or its existing theme-colored moving selection. */
109: html body .top.tp154-nav-grid,
110: html.tp-nav-compact body .top.tp154-nav-grid,
111: html:not(.tp-nav-compact) body .top.tp154-nav-grid{
112:   background:var(--tp-ui-bg,#0e1015)!important;
113:   backdrop-filter:none!important;
114:   -webkit-backdrop-filter:none!important;
115:   box-shadow:none!important;
116:   border-bottom-color:var(--tp-ui-border,#2c3440)!important;
117:   padding-top:3px!important;
118:   transition:none!important;
119:   will-change:auto!important;
120: }
```

## android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt — 1–772

Ref: `2f7cd835fad7cd3e0d5578da4a4ec65afa130983`; teljes forrás SHA256: `2b9b97dae78ffab6b723198cd2ac223804432ee9746a98b05870c079e38e94c3`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/2f7cd835fad7cd3e0d5578da4a4ec65afa130983/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt

```text
1: package com.repforge.app.ui.health
2: import android.app.Activity
3: import android.Manifest
4: import android.os.Build
5: import android.content.pm.PackageManager
6: import androidx.activity.compose.rememberLauncherForActivityResult
7: import androidx.activity.result.contract.ActivityResultContracts
8: import androidx.compose.ui.platform.testTag
9: import androidx.compose.ui.semantics.contentDescription
10: import androidx.compose.ui.semantics.semantics
11: import androidx.compose.ui.platform.LocalContext
12: import androidx.core.content.ContextCompat
13: import com.repforge.app.HealthConnectRepository
14: import androidx.compose.material3.TextButton
15: 
16: import androidx.compose.animation.AnimatedVisibility
17: import androidx.compose.foundation.background
18: import androidx.compose.foundation.border
19: import androidx.compose.foundation.clickable
20: import androidx.compose.foundation.layout.Arrangement
21: import androidx.compose.foundation.layout.Box
22: import androidx.compose.foundation.layout.Column
23: import androidx.compose.foundation.layout.Row
24: import androidx.compose.foundation.layout.Spacer
25: import androidx.compose.foundation.layout.fillMaxSize
26: import androidx.compose.foundation.layout.fillMaxWidth
27: import androidx.compose.foundation.layout.height
28: import androidx.compose.foundation.layout.padding
29: import androidx.compose.foundation.layout.size
30: import androidx.compose.foundation.layout.width
31: import androidx.compose.foundation.lazy.LazyColumn
32: import androidx.compose.foundation.lazy.items
33: import androidx.compose.foundation.shape.CircleShape
34: import androidx.compose.foundation.shape.RoundedCornerShape
35: import androidx.compose.material.icons.Icons
36: import androidx.compose.material.icons.filled.Bluetooth
37: import androidx.compose.material.icons.filled.BluetoothSearching
38: import androidx.compose.material.icons.filled.Check
39: import androidx.compose.material.icons.filled.Favorite
40: import androidx.compose.material.icons.filled.Refresh
41: import androidx.compose.material.icons.filled.Sync
42: import androidx.compose.material3.Button
43: import androidx.compose.material3.ButtonDefaults
44: import androidx.compose.material3.Card
45: import androidx.compose.material3.CardDefaults
46: import androidx.compose.material3.Icon
47: import androidx.compose.material3.IconButton
48: import androidx.compose.material3.LinearProgressIndicator
49: import androidx.compose.material3.OutlinedButton
50: import androidx.compose.material3.Switch
51: import androidx.compose.material3.SwitchDefaults
52: import androidx.compose.material3.Tab
53: import androidx.compose.material3.TabRow
54: import androidx.compose.material3.TabRowDefaults
55: import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
56: import androidx.compose.material3.Text
57: import androidx.compose.runtime.Composable
58: import androidx.compose.runtime.rememberCoroutineScope
59: import kotlinx.coroutines.launch
60: import androidx.lifecycle.withResumed
61: import androidx.compose.runtime.DisposableEffect
62: import androidx.compose.runtime.LaunchedEffect
63: import androidx.lifecycle.Lifecycle
64: import androidx.lifecycle.LifecycleEventObserver
65: import androidx.lifecycle.compose.LocalLifecycleOwner
66: import androidx.compose.runtime.getValue
67: import androidx.compose.runtime.mutableStateOf
68: import androidx.compose.runtime.setValue
69: import androidx.compose.runtime.saveable.rememberSaveable
70: import androidx.compose.ui.Alignment
71: import androidx.compose.ui.Modifier
72: import androidx.compose.ui.draw.clip
73: import androidx.compose.ui.graphics.Color
74: import androidx.compose.ui.text.font.FontWeight
75: import androidx.compose.ui.unit.dp
76: import androidx.compose.ui.unit.sp
77: import androidx.lifecycle.compose.collectAsStateWithLifecycle
78: import androidx.lifecycle.viewmodel.compose.viewModel
79: import androidx.navigation.NavController
80: import com.repforge.app.R
81: import com.repforge.app.ui.text.nativeResources
82: import androidx.compose.runtime.remember
83: import com.repforge.app.ui.theme.LocalTrainPilotColors
84: 
85: 
86: private fun backgroundReasonResource(reason: BackgroundBlock): Int = when (reason) {
87:     BackgroundBlock.READY -> com.repforge.app.R.string.health_background_ready
88:     BackgroundBlock.RESTORE -> com.repforge.app.R.string.health_background_restore
89:     BackgroundBlock.UNSUPPORTED -> com.repforge.app.R.string.health_background_unsupported
90:     BackgroundBlock.READ_PERMISSION -> com.repforge.app.R.string.health_background_read_permission
91:     BackgroundBlock.BACKGROUND_PERMISSION -> com.repforge.app.R.string.health_background_permission
92:     BackgroundBlock.WATCH_NOT_READY -> com.repforge.app.R.string.health_background_watch
93:     BackgroundBlock.BLUETOOTH_PERMISSION -> com.repforge.app.R.string.health_background_bluetooth_permission
94:     BackgroundBlock.NOTIFICATION_PERMISSION -> com.repforge.app.R.string.health_background_notification_permission
95:     BackgroundBlock.BLUETOOTH_OFF -> com.repforge.app.R.string.health_background_bluetooth_off
96: }
97: 
98: @Composable
99: fun HealthDashboardScreen(
100:     navController: NavController,
101:     onWeightJournal: () -> Unit,
102:     viewModel: HealthDashboardViewModel = viewModel()
103: ) {
104:     val state by viewModel.uiState.collectAsStateWithLifecycle()
105:     val colors = LocalTrainPilotColors.current
106:     val context = LocalContext.current
107:     val activity = context as? Activity
108:     val screenLifecycle = LocalLifecycleOwner.current
109:     DisposableEffect(screenLifecycle, activity, viewModel) {
110:         val observer = LifecycleEventObserver { _, event ->
111:             when (event) {
112:                 Lifecycle.Event.ON_RESUME -> viewModel.healthScreenVisible(activity)
113:                 // Samsung's explicit permission dialog can pause the activity briefly.
114:                 // Foreground SDK publication is independently guarded by RESUMED; do not
115:                 // cancel the in-flight permission request until STOP or destination disposal.
116:                 Lifecycle.Event.ON_STOP -> viewModel.healthScreenHidden(activity)
117:                 else -> Unit
118:             }
119:         }
120:         screenLifecycle.lifecycle.addObserver(observer)
121:         if (screenLifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
122:             viewModel.healthScreenVisible(activity)
123:         onDispose {
124:             screenLifecycle.lifecycle.removeObserver(observer)
125:             viewModel.healthScreenHidden(activity, force = true)
126:         }
127:     }
128:     val strings = remember(context, state.language) { nativeResources(context, state.language) }
129:     var detailType by rememberSaveable(state.selectedDay, state.historyChannel) { mutableStateOf<String?>(null) }
130:     LaunchedEffect(detailType) {
131:         viewModel.healthRecordDetailChanged(detailType != null)
132:     }
133:     var pendingFullSync by rememberSaveable { mutableStateOf<Boolean?>(null) }
134:     val permissionScope = rememberCoroutineScope()
135:     val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
136:         val full = pendingFullSync
137:         pendingFullSync = null
138:         permissionScope.launch {
139:             screenLifecycle.lifecycle.withResumed {
140:                 viewModel.healthScreenVisible(activity)
141:                 if (full == null) viewModel.refresh(activity) else viewModel.syncHealth(activity, full)
142:             }
143:         }
144:     }
145:     fun startSync(full: Boolean) {
146:         val permissions = HealthConnectRepository.ALL_RUNTIME_PERMISSIONS
147:         if (state.provider == "health_connect" && Build.VERSION.SDK_INT >= 34 &&
148:             permissions.none { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
149:             pendingFullSync = full
150:             permissionLauncher.launch(permissions)
151:         } else viewModel.syncHealth(activity, full)
152:     }
153:     val backgroundPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
154:         if (granted) viewModel.toggleHcBackground(true) else viewModel.refreshBackgroundStatus()
155:     }
156:     val bleBackgroundPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
157:         if (granted.isNotEmpty() && granted.values.all { it }) viewModel.toggleBleBackground(true)
158:         else viewModel.refreshBackgroundStatus()
159:     }
160: 
161:     Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
162:         TextButton(onClick = onWeightJournal, modifier = Modifier.testTag("health_weight_journal")) {
163:             Text(com.repforge.app.ui.text.nativeResources(context, state.language)
164:                 .getString(com.repforge.app.R.string.weight_open))
165:         }
166:         state.statusMessage?.let { Text(it, color = colors.muted, modifier = Modifier.padding(12.dp)) }
167:         if (state.backgroundWriteFailed) Text(
168:             com.repforge.app.ui.text.nativeResources(context, state.language)
169:                 .getString(com.repforge.app.R.string.health_background_error),
170:             color = colors.dangerText, modifier = Modifier.testTag("health_background_error").padding(12.dp)
171:         )
172:         // Két fül: Egészségnapló vs #111 BLE Eszközkereső
173:         TabRow(
174:             selectedTabIndex = if (state.activeTab == "journal") 0 else 1,
175:             containerColor = colors.card,
176:             contentColor = colors.text,
177:             indicator = {
178:                 TabRowDefaults.SecondaryIndicator(
179:                     modifier = Modifier.tabIndicatorOffset(it[if (state.activeTab == "journal") 0 else 1]),
180:                     color = colors.accent
181:                 )
182:             }
183:         ) {
184:             Tab(
185:                 selected = state.activeTab == "journal",
186:                 onClick = { viewModel.setTab("journal") },
187:                 text = { Text(com.repforge.app.ui.text.nativeResources(context, state.language).getString(com.repforge.app.R.string.health_history_title), fontWeight = FontWeight.Bold) }
188:             )
189:             Tab(
190:                 selected = state.activeTab == "ble",
191:                 onClick = { viewModel.setTab("ble") },
192:                 text = { Text(strings.getString(R.string.native_health_ble_tab), fontWeight = FontWeight.Bold) }
193:             )
194:         }
195: 
196:         if (state.activeTab == "journal") {
197:             HealthJournalTabContent(
198:                 state = state,
199:                 onSelectProvider = { viewModel.setProvider(it, activity) },
200:                 onAutoOnOpen = viewModel::setAutoOnOpen,
201:                 onToggleHcBackground = { enable ->
202:                     if (enable && state.hcBackgroundBlock == BackgroundBlock.BACKGROUND_PERMISSION)
203:                         backgroundPermissionLauncher.launch(HealthConnectRepository.BACKGROUND_READ_PERMISSION)
204:                     else viewModel.toggleHcBackground(enable)
205:                 },
206:                 onPreviousMonth = { viewModel.shiftHistoryMonth(-1) },
207:                 onNextMonth = { viewModel.shiftHistoryMonth(1) },
208:                 onDay = viewModel::selectHistoryDay,
209:                 onChannel = viewModel::selectHistoryChannel,
210:                 onToday = viewModel::historyToday,
211:                 onOpenRecords = { detailType = it },
212:                 onDailySource = viewModel::setDailySourcePreference,
213:                 onSync = { startSync(false) },
214:                 onFullSync = { startSync(true) },
215:                 onCancelSync = viewModel::cancelHealthSync,
216:                 onRetrySync = { startSync(state.syncRetryFull) },
217:                 onRefresh = {
218:                     val permissions = HealthConnectRepository.READ_PERMS.map { "android.permission.health.$it" }.toTypedArray()
219:                     if (state.provider == "health_connect" && Build.VERSION.SDK_INT >= 34 &&
220:                         permissions.none { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
221:                         permissionLauncher.launch(permissions)
222:                     } else viewModel.refresh(activity)
223:                 }
224:             )
225:         } else {
226:             BleDiscoveryTabContent(
227:                 state = state,
228:                 bleRepo = viewModel.bleRepo,
229:                 onToggleBleBackground = { enable ->
230:                     if (enable && state.bleBackgroundBlock in setOf(BackgroundBlock.BLUETOOTH_PERMISSION, BackgroundBlock.NOTIFICATION_PERMISSION)) {
231:                         val permissions = buildList {
232:                             if (Build.VERSION.SDK_INT >= 31 &&
233:                                 ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
234:                                 add(Manifest.permission.BLUETOOTH_CONNECT)
235:                             if (Build.VERSION.SDK_INT >= 33 &&
236:                                 ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
237:                                 add(Manifest.permission.POST_NOTIFICATIONS)
238:                         }
239:                         if (permissions.isNotEmpty()) bleBackgroundPermissionLauncher.launch(permissions.toTypedArray())
240:                         else viewModel.refreshBackgroundStatus()
241:                     } else viewModel.toggleBleBackground(enable)
242:                 }
243:             )
244:         }
245:     }
246:     detailType?.let { type ->
247:         HealthRecordBrowser(state.selectedDay, state.historyChannel, type, state.language,
248:             onClose = { detailType = null })
249:     }
250: }
251: 
252: @Composable
253: fun HealthJournalTabContent(
254:     state: HealthDashboardUiState,
255:     onSelectProvider: (String) -> Unit,
256:     onToggleHcBackground: (Boolean) -> Unit,
257:     onAutoOnOpen: (Boolean) -> Unit,
258:     onRefresh: () -> Unit,
259:     onPreviousMonth: () -> Unit,
260:     onNextMonth: () -> Unit,
261:     onDay: (String) -> Unit,
262:     onChannel: (String) -> Unit,
263:     onToday: () -> Unit,
264:     onOpenRecords: (String) -> Unit,
265:     onDailySource: (String) -> Unit,
266:     onSync: () -> Unit = {},
267:     onFullSync: () -> Unit = {},
268:     onCancelSync: () -> Unit = {},
269:     onRetrySync: () -> Unit = {}
270: ) {
271:     val colors = LocalTrainPilotColors.current
272:     val context = LocalContext.current
273:     val strings = remember(context, state.language) { nativeResources(context, state.language) }
274:     LazyColumn(
275:         modifier = Modifier.fillMaxSize().testTag("health_journal_list").padding(horizontal = 16.dp),
276:         verticalArrangement = Arrangement.spacedBy(14.dp)
277:     ) {
278:         item {
279:             val text = com.repforge.app.ui.text.nativeResources(LocalContext.current, state.language)
280:             Column(Modifier.fillMaxWidth().testTag("health_history")) {
281:                 Text(state.historyMonth, color = colors.text, fontWeight = FontWeight.Bold)
282:                 Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
283:                     TextButton(onClick = onPreviousMonth, modifier = Modifier.testTag("health_previous_month")) { Text(text.getString(com.repforge.app.R.string.health_history_previous)) }
284:                     TextButton(onClick = onNextMonth, enabled = state.historyMonth < java.time.YearMonth.now().toString()) { Text(text.getString(com.repforge.app.R.string.health_history_next)) }
285:                 }
286:                 TextButton(onClick = onToday) { Text(text.getString(com.repforge.app.R.string.health_history_today)) }
287:                 Text(text.getString(com.repforge.app.R.string.health_history_day, state.selectedDay), color = colors.text,
288:                     modifier = Modifier.testTag("health_selected_day"))
289:                 androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
290:                     items((state.availableDays + state.selectedDay).distinct().sortedDescending()) { day ->
291:                         TextButton(onClick = { onDay(day) }) { Text(day) }
292:                     }
293:                 }
294:                 androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
295:                     items(listOf("journal", "health_connect", "samsung_health", "legacy", com.repforge.app.HealthJournalStore.BLE)) { channel ->
296:                         val label = when (channel) { "journal" -> text.getString(com.repforge.app.R.string.health_history_auto)
297:                             "health_connect" -> "Health Connect"; "samsung_health" -> "Samsung Health"; com.repforge.app.HealthJournalStore.BLE -> "BLE"; else -> text.getString(com.repforge.app.R.string.health_history_legacy) }
298:                         androidx.compose.material3.FilterChip(selected = state.historyChannel == channel, onClick = { onChannel(channel) }, label = { Text(label) })
299:                     }
300:                 }
301:                 if (state.historyLoading) Text(text.getString(com.repforge.app.R.string.health_history_loading), color = colors.muted)
302:                 if (state.historyError) Text(text.getString(com.repforge.app.R.string.health_history_error), color = colors.dangerText)
303:                 TextButton(onClick = { onOpenRecords("") }, enabled = !state.historyLoading,
304:                     modifier = Modifier.testTag("health_open_records")) { Text(text.getString(com.repforge.app.R.string.health_records_title)) }
305:             }
306:         }
307:         item { Spacer(Modifier.height(8.dp)) }
308: 
309:         // Szolgáltató választó kártya (Health Connect vs Samsung Health)
310:         item {
311:             Card(
312:                 modifier = Modifier.fillMaxWidth(),
313:                 shape = RoundedCornerShape(16.dp),
314:                 colors = CardDefaults.cardColors(containerColor = colors.card),
315:                 border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
316:             ) {
317:                 Column(modifier = Modifier.padding(14.dp)) {
318:                     Row(
319:                         modifier = Modifier.fillMaxWidth(),
320:                         horizontalArrangement = Arrangement.SpaceBetween,
321:                         verticalAlignment = Alignment.CenterVertically
322:                     ) {
323:                         Text(strings.getString(R.string.native_health_source), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
324:                         IconButton(onClick = onRefresh, enabled = !state.healthRefreshing, modifier = Modifier.size(32.dp)) {
325:                             Icon(Icons.Default.Refresh, contentDescription = strings.getString(R.string.native_health_refresh), tint = colors.accent)
326:                         }
327:                     }
328:                     Spacer(Modifier.height(10.dp))
329:                     Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
330:                         ProviderChoiceChip(
331:                             label = "Health Connect",
332:                             isSelected = state.provider == "health_connect",
333:                             onClick = { onSelectProvider("health_connect") },
334:                             modifier = Modifier.weight(1f)
335:                         )
336:                         ProviderChoiceChip(
337:                             label = "Samsung Health",
338:                             isSelected = state.provider == "samsung_health",
339:                             onClick = { onSelectProvider("samsung_health") },
340:                             modifier = Modifier.weight(1f)
341:                         )
342:                     }
343:                     Spacer(Modifier.height(8.dp))
344:                     Text(
345:                         text = strings.getString(R.string.native_health_provider_help),
346:                         color = colors.muted,
347:                         fontSize = 11.sp
348:                     )
349:                     if (state.provider == com.repforge.app.HealthJournalStore.HC) {
350:                         val text = com.repforge.app.ui.text.nativeResources(LocalContext.current, state.language)
351:                         Text(text.getString(com.repforge.app.R.string.health_daily_source_title), color = colors.text)
352:                         androidx.compose.foundation.lazy.LazyRow(modifier = Modifier.testTag("health_daily_sources_row"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
353:                             items(listOf("auto", "priority") + state.dailySourceOptions, key = { it }) { choice ->
354:                                 androidx.compose.material3.FilterChip(selected = state.dailySourcePreference == choice,
355:                                     enabled = !state.sourceSaving && !state.historyLoading,
356:                                     onClick = { onDailySource(choice) }, modifier = Modifier.testTag("health_daily_$choice"),
357:                                     label = { Text(when (choice) {
358:                                         "auto" -> text.getString(com.repforge.app.R.string.health_daily_source_auto)
359:                                         "priority" -> text.getString(com.repforge.app.R.string.health_daily_source_priority)
360:                                         else -> choice
361:                                     }) })
362:                             }
363:                         }
364:                         if (state.dailySourcePreference !in (listOf("auto", "priority") + state.dailySourceOptions))
365:                             Text(state.dailySourcePreference, color = colors.muted)
366:                         Text(text.getString(com.repforge.app.R.string.health_daily_source_hint), color = colors.muted)
367:                         if (state.sourceSaveFailed) Text(text.getString(com.repforge.app.R.string.metadata_write_failed), color = colors.danger)
368:                     }
369:                     if (state.historyPartial) Text(com.repforge.app.ui.text.nativeResources(LocalContext.current, state.language)
370:                         .getString(com.repforge.app.R.string.health_daily_source_partial), color = colors.muted,
371:                         modifier = Modifier.testTag("health_daily_partial"))
372:                 }
373:             }
374:         }
375: 
376:         // Foreground auto-on-open is intentionally independent of Android background job permission.
377:         item {
378:             Card(
379:                 modifier = Modifier.fillMaxWidth().testTag("health_auto_refresh_card"),
380:                 shape = RoundedCornerShape(14.dp),
381:                 colors = CardDefaults.cardColors(containerColor = colors.card2)
382:             ) {
383:                 Row(
384:                     modifier = Modifier.fillMaxWidth().padding(14.dp),
385:                     horizontalArrangement = Arrangement.SpaceBetween,
386:                     verticalAlignment = Alignment.CenterVertically
387:                 ) {
388:                     Column(modifier = Modifier.weight(1f)) {
389:                         Text(strings.getString(R.string.native_health_auto_refresh_title),
390:                             color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
391:                         Text(strings.getString(R.string.native_health_auto_refresh_help),
392:                             color = colors.muted, fontSize = 11.sp)
393:                         if (state.autoOnOpenSaveFailed) Text(
394:                             strings.getString(R.string.native_health_auto_refresh_save_error),
395:                             color = colors.dangerText, fontSize = 11.sp,
396:                             modifier = Modifier.testTag("health_auto_refresh_save_error")
397:                         )
398:                     }
399:                     Switch(
400:                         checked = state.autoOnOpen,
401:                         enabled = !state.autoOnOpenSaving && !state.historyLoading,
402:                         onCheckedChange = onAutoOnOpen,
403:                         modifier = Modifier.testTag("health_auto_refresh_switch")
404:                             .semantics { contentDescription = strings.getString(R.string.native_health_auto_refresh_title) },
405:                         colors = SwitchDefaults.colors(
406:                             checkedThumbColor = colors.accent,
407:                             checkedTrackColor = colors.card
408:                         )
409:                     )
410:                 }
411:                 Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
412:                     Text(strings.getString(R.string.native_health_sync_last, state.lastSyncAt),
413:                         color = colors.muted, fontSize = 11.sp, modifier = Modifier.testTag("health_sync_last"))
414:                     if (state.healthRefreshing) {
415:                         Text(strings.getString(R.string.native_health_auto_refresh_running),
416:                             color = colors.muted, modifier = Modifier.testTag("health_auto_refresh_running"))
417:                         val progressText = when (state.syncStage) {
418:                             "CHANGES" -> strings.getString(R.string.native_health_sync_changes, state.syncPages)
419:                             "DAYS", "COMPLETE" -> strings.getString(R.string.native_health_sync_days, state.syncCompleted, state.syncTotal)
420:                             else -> strings.getString(R.string.native_health_sync_preparing)
421:                         }
422:                         Text(progressText, color = colors.muted, modifier = Modifier.testTag("health_sync_progress"))
423:                         TextButton(onClick = onCancelSync, modifier = Modifier.testTag("health_sync_cancel")) {
424:                             Text(strings.getString(R.string.native_health_sync_cancel))
425:                         }
426:                     } else {
427:                         TextButton(onClick = onSync, modifier = Modifier.testTag("health_sync_now")) {
428:                             Text(strings.getString(R.string.native_health_sync_now))
429:                         }
430:                         TextButton(onClick = onFullSync, modifier = Modifier.testTag("health_sync_full")) {
431:                             Text(strings.getString(R.string.native_health_sync_full))
432:                         }
433:                     }
434:                     if (state.syncFailed || state.syncPartial || state.syncCancelled) {
435:                         Text(strings.getString(when {
436:                             state.syncCancelled -> R.string.native_health_sync_cancelled
437:                             state.syncPartial -> R.string.native_health_sync_partial
438:                             else -> R.string.native_health_sync_failed
439:                         }), color = colors.dangerText, modifier = Modifier.testTag("health_sync_failure"))
440:                         if (!state.healthRefreshing) TextButton(onClick = onRetrySync, modifier = Modifier.testTag("health_sync_retry")) {
441:                             Text(strings.getString(R.string.native_health_sync_retry))
442:                         }
443:                     }
444:                 }
445:             }
446:         }
447: 
448:         // Alvásnapló összefoglaló
449:         item {
450:             Card(
451:                 modifier = Modifier.fillMaxWidth(),
452:                 shape = RoundedCornerShape(14.dp),
453:                 colors = CardDefaults.cardColors(containerColor = colors.card2)
454:             ) {
455:                 Column(modifier = Modifier.padding(12.dp)) {
456:                     Text(com.repforge.app.ui.text.nativeResources(LocalContext.current, state.language)
457:                         .getString(com.repforge.app.R.string.health_record_sleep), color = colors.accent2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
458:                     Spacer(Modifier.height(4.dp))
459:                     Text(state.sleepSessionsSummaryText, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
460:                     TextButton(onClick = { onOpenRecords("SleepSessionRecord") }, enabled = !state.historyLoading,
461:                         modifier = Modifier.testTag("health_open_sleep")) {
462:                         Text(com.repforge.app.ui.text.nativeResources(LocalContext.current, state.language)
463:                             .getString(com.repforge.app.R.string.health_sleep_details))
464:                     }
465:                 }
466:             }
467:         }
468: 
469:         // 26 egészségügyi metrika listája
470:         items(state.metrics, key = { it.key }) { m ->
471:             Card(
472:                 modifier = Modifier.fillMaxWidth().testTag("health_metric_${m.key}"),
473:                 shape = RoundedCornerShape(12.dp),
474:                 colors = CardDefaults.cardColors(containerColor = colors.card),
475:                 border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
476:             ) {
477:                 Row(
478:                     modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
479:                     horizontalArrangement = Arrangement.SpaceBetween,
480:                     verticalAlignment = Alignment.CenterVertically
481:                 ) {
482:                     Column(modifier = Modifier.weight(1f)) {
483:                         Text(m.label, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
484:                         Text(m.sourceText, color = colors.muted, fontSize = 10.sp)
485:                     }
486:                     Row(verticalAlignment = Alignment.Bottom) {
487:                         Text(m.valueText, color = if (m.isAvailable) colors.accent else colors.muted, fontSize = 17.sp, fontWeight = FontWeight.Bold)
488:                         if (m.unit.isNotBlank() && m.isAvailable) {
489:                             Spacer(Modifier.width(4.dp))
490:                             Text(m.unit, color = colors.muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
491:                         }
492:                     }
493:                 }
494:             }
495:         }
496: 
497:         // Háttérszinkron kapcsoló kártya
498:         item {
499:             Card(
500:                 modifier = Modifier.fillMaxWidth(),
501:                 shape = RoundedCornerShape(14.dp),
502:                 colors = CardDefaults.cardColors(containerColor = colors.card2)
503:             ) {
504:                 Row(
505:                     modifier = Modifier.fillMaxWidth().padding(14.dp),
506:                     horizontalArrangement = Arrangement.SpaceBetween,
507:                     verticalAlignment = Alignment.CenterVertically
508:                 ) {
509:                     Column(modifier = Modifier.weight(1f)) {
510:                         Text(strings.getString(R.string.native_health_background_title), color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
511:                         Text(strings.getString(R.string.native_health_background_help), color = colors.muted, fontSize = 11.sp)
512:                         val text = com.repforge.app.ui.text.nativeResources(LocalContext.current, state.language)
513:                         Text(text.getString(backgroundReasonResource(state.hcBackgroundBlock)),
514:                             color = if (state.hcBackgroundBlock == BackgroundBlock.READY) colors.muted else colors.dangerText,
515:                             fontSize = 11.sp, modifier = Modifier.testTag("health_hc_background_status"))
516:                         Text(text.getString(if (state.hcBackgroundScheduled && state.hcBackground) com.repforge.app.R.string.health_background_scheduled
517:                             else com.repforge.app.R.string.health_background_not_scheduled), color = colors.muted, fontSize = 11.sp)
518:                     }
519:                     Switch(
520:                         checked = state.hcBackground,
521:                         enabled = !state.backgroundSaving,
522:                         modifier = Modifier.testTag("health_hc_background_switch"),
523:                         onCheckedChange = onToggleHcBackground,
524:                         colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.card)
525:                     )
526:                 }
527:             }
528:         }
529: 
530:         item { Spacer(Modifier.height(80.dp)) }
531:     }
532: }
533: 
534: @Composable
535: fun BleDiscoveryTabContent(
536:     state: HealthDashboardUiState,
537:     bleRepo: com.repforge.app.BleDiscoveryRepository,
538:     onToggleBleBackground: (Boolean) -> Unit
539: ) {
540:     val colors = LocalTrainPilotColors.current
541:     val ble = state.bleState
542:     val context = LocalContext.current
543:     val strings = remember(context, state.language) { nativeResources(context, state.language) }
544: 
545:     LazyColumn(
546:         modifier = Modifier.fillMaxSize().testTag("health_ble_list").padding(horizontal = 16.dp),
547:         verticalArrangement = Arrangement.spacedBy(14.dp)
548:     ) {
549:         item { Spacer(Modifier.height(8.dp)) }
550: 
551:         // #111 Keresés vezérlő panel
552:         item {
553:             Card(
554:                 modifier = Modifier.fillMaxWidth(),
555:                 shape = RoundedCornerShape(16.dp),
556:                 colors = CardDefaults.cardColors(containerColor = colors.card),
557:                 border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
558:             ) {
559:                 Column(modifier = Modifier.padding(16.dp)) {
560:                     Row(
561:                         modifier = Modifier.fillMaxWidth(),
562:                         horizontalArrangement = Arrangement.SpaceBetween,
563:                         verticalAlignment = Alignment.CenterVertically
564:                     ) {
565:                         Column {
566:                             Text(strings.getString(R.string.native_health_ble_search), color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
567:                             Text(strings.getString(R.string.native_health_ble_state, ble.state, ble.code), color = colors.muted, fontSize = 11.sp)
568:                         }
569:                         if (ble.state == "scanning") {
570:                             Button(
571:                                 onClick = { bleRepo.stopScan() },
572:                                 colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
573:                             ) {
574:                                 Text(strings.getString(R.string.native_health_ble_stop), color = colors.dangerText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
575:                             }
576:                         } else {
577:                             Button(
578:                                 onClick = { bleRepo.startScan() },
579:                                 colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
580:                             ) {
581:                                 Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = colors.onAccent)
582:                                 Spacer(Modifier.width(4.dp))
583:                                 Text(strings.getString(R.string.native_health_ble_scan), color = colors.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
584:                             }
585:                         }
586:                     }
587: 
588:                     if (ble.state == "scanning") {
589:                         Spacer(Modifier.height(10.dp))
590:                         LinearProgressIndicator(
591:                             progress = { ble.scanRemainingSeconds / 60f },
592:                             modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
593:                             color = colors.accentText,
594:                             trackColor = colors.card2
595:                         )
596:                         Spacer(Modifier.height(4.dp))
597:                         Text(strings.getString(R.string.native_health_ble_remaining, ble.scanRemainingSeconds), color = colors.muted, fontSize = 10.sp)
598:                     }
599:                 }
600:             }
601:         }
602: 
603:         // Élő BPM & RDFit Teszt panel (ha csatlakoztatva van)
604:         if (ble.state in listOf("connected", "probing", "monitoring")) {
605:             item {
606:                 Card(
607:                     modifier = Modifier.fillMaxWidth(),
608:                     shape = RoundedCornerShape(16.dp),
609:                     colors = CardDefaults.cardColors(containerColor = colors.card2),
610:                     border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f))
611:                 ) {
612:                     Column(modifier = Modifier.padding(16.dp)) {
613:                         Row(
614:                             modifier = Modifier.fillMaxWidth(),
615:                             horizontalArrangement = Arrangement.SpaceBetween,
616:                             verticalAlignment = Alignment.CenterVertically
617:                         ) {
618:                             Text(ble.deviceName.ifBlank { strings.getString(R.string.native_health_ble_connected_watch) }, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
619:                             TextButton(onClick = { bleRepo.disconnect() }) {
620:                                 Text(strings.getString(R.string.native_health_ble_disconnect), color = colors.dangerText, fontSize = 11.sp)
621:                             }
622:                         }
623:                         Spacer(Modifier.height(10.dp))
624:                         Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
625:                             // 0x2A37 Élő Pulzus
626:                             Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = colors.card)) {
627:                                 Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
628:                                     Text(strings.getString(R.string.native_health_ble_heart_rate), color = colors.muted, fontSize = 10.sp)
629:                                     Spacer(Modifier.height(4.dp))
630:                                     Text(if (ble.liveHeartRateBpm != null) "${ble.liveHeartRateBpm} bpm" else "—", color = Color(0xFFEF5350), fontSize = 22.sp, fontWeight = FontWeight.Bold)
631:                                     if (ble.heartSupported && ble.state != "monitoring") {
632:                                         Spacer(Modifier.height(6.dp))
633:                                         OutlinedButton(onClick = { bleRepo.startHeartRate() }) {
634:                                             Text(strings.getString(R.string.native_health_ble_listen), fontSize = 10.sp)
635:                                         }
636:                                     }
637:                                 }
638:                             }
639:                             // RDFit Lépés & Akkumulátor
640:                             Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = colors.card)) {
641:                                 Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
642:                                     Text(strings.getString(R.string.native_health_ble_rd_fit), color = colors.muted, fontSize = 10.sp)
643:                                     Spacer(Modifier.height(4.dp))
644:                                     Text(if (ble.rdfitProbe.stepsCount != null) strings.getString(R.string.native_health_ble_steps_count, ble.rdfitProbe.stepsCount) else if (ble.rdfitProbe.batteryPercent != null) "${ble.rdfitProbe.batteryPercent}%" else "—", color = colors.accentText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
645:                                     if (ble.probeSupported && ble.state != "probing") {
646:                                         Spacer(Modifier.height(6.dp))
647:                                         OutlinedButton(onClick = { bleRepo.readRdfitData() }) {
648:                                             Text(strings.getString(R.string.native_health_ble_request), fontSize = 10.sp)
649:                                         }
650:                                     }
651:                                 }
652:                             }
653:                         }
654:                         Spacer(Modifier.height(10.dp))
655:                         Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
656:                             Button(
657:                                 onClick = { bleRepo.rememberWatch() },
658:                                 modifier = Modifier.weight(1f),
659:                                 colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
660:                             ) {
661:                                 Text(strings.getString(R.string.native_health_ble_remember), color = colors.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
662:                             }
663:                             if (ble.rememberedWatchPresent) {
664:                                 Button(
665:                                     onClick = { bleRepo.forgetWatch() },
666:                                     colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
667:                                 ) {
668:                                     Text(strings.getString(R.string.native_health_ble_forget), color = colors.dangerText, fontSize = 11.sp)
669:                                 }
670:                             }
671:                         }
672:                     }
673:                 }
674:             }
675:         }
676: 
677:         // Háttérkapcsolat (5 percenkénti lépéslekérés)
678:         item {
679:             Card(
680:                 modifier = Modifier.fillMaxWidth(),
681:                 shape = RoundedCornerShape(14.dp),
682:                 colors = CardDefaults.cardColors(containerColor = colors.card2)
683:             ) {
684:                 Row(
685:                     modifier = Modifier.fillMaxWidth().padding(14.dp),
686:                     horizontalArrangement = Arrangement.SpaceBetween,
687:                     verticalAlignment = Alignment.CenterVertically
688:                 ) {
689:                     Column(modifier = Modifier.weight(1f)) {
690:                         Text(strings.getString(R.string.native_health_ble_background), color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
691:                         Text(strings.getString(R.string.native_health_ble_background_help), color = colors.muted, fontSize = 11.sp)
692:                         val text = com.repforge.app.ui.text.nativeResources(LocalContext.current, state.language)
693:                         Text(text.getString(backgroundReasonResource(state.bleBackgroundBlock)),
694:                             color = if (state.bleBackgroundBlock == BackgroundBlock.READY) colors.muted else colors.dangerText,
695:                             fontSize = 11.sp, modifier = Modifier.testTag("health_ble_background_status"))
696:                         Text(text.getString(if (state.bleBackgroundRunning && state.bleBackground) com.repforge.app.R.string.health_background_connected
697:                             else com.repforge.app.R.string.health_background_not_connected), color = colors.muted, fontSize = 11.sp)
698:                     }
699:                     Switch(
700:                         checked = state.bleBackground,
701:                         enabled = !state.backgroundSaving,
702:                         modifier = Modifier.testTag("health_ble_background_switch"),
703:                         onCheckedChange = onToggleBleBackground,
704:                         colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.card)
705:                     )
706:                 }
707:             }
708:         }
709: 
710:         // Talált eszközök listája (#111 .tp111-ble)
711:         item {
712:             Text(strings.getString(R.string.native_health_ble_found, ble.devices.size), color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
713:         }
714:         items(ble.devices, key = { it.id }) { dev ->
715:             Card(
716:                 modifier = Modifier.fillMaxWidth().clickable { bleRepo.connect(dev.id) },
717:                 shape = RoundedCornerShape(12.dp),
718:                 colors = CardDefaults.cardColors(containerColor = colors.card),
719:                 border = androidx.compose.foundation.BorderStroke(1.dp, if (dev.remembered) colors.accent else colors.line)
720:             ) {
721:                 Row(
722:                     modifier = Modifier.fillMaxWidth().padding(14.dp),
723:                     horizontalArrangement = Arrangement.SpaceBetween,
724:                     verticalAlignment = Alignment.CenterVertically
725:                 ) {
726:                     Column(modifier = Modifier.weight(1f)) {
727:                         Text(dev.name.ifBlank { strings.getString(R.string.native_health_ble_unnamed_device) }, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
728:                         Text(strings.getString(R.string.native_health_ble_rssi, dev.displayAddress, dev.rssi), color = colors.muted, fontSize = 11.sp)
729:                         if (dev.remembered) {
730:                             Text(strings.getString(R.string.native_health_ble_saved), color = colors.accent2, fontSize = 10.sp, fontWeight = FontWeight.Bold)
731:                         }
732:                     }
733:                     Button(
734:                         onClick = { bleRepo.connect(dev.id) },
735:                         colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
736:                     ) {
737:                         Text(strings.getString(R.string.native_health_ble_connect), color = colors.accentText, fontSize = 11.sp)
738:                     }
739:                 }
740:             }
741:         }
742: 
743:         item { Spacer(Modifier.height(80.dp)) }
744:     }
745: }
746: 
747: @Composable
748: fun ProviderChoiceChip(
749:     label: String,
750:     isSelected: Boolean,
751:     onClick: () -> Unit,
752:     modifier: Modifier
753: ) {
754:     val colors = LocalTrainPilotColors.current
755:     Box(
756:         modifier = modifier
757:             .height(42.dp)
758:             .clip(RoundedCornerShape(10.dp))
759:             .background(if (isSelected) colors.accent else colors.card2)
760:             .border(1.dp, if (isSelected) colors.accent else colors.line, RoundedCornerShape(10.dp))
761:             .clickable(onClick = onClick)
762:             .padding(horizontal = 8.dp),
763:         contentAlignment = Alignment.Center
764:     ) {
765:         Text(
766:             text = label,
767:             color = if (isSelected) colors.onAccent else colors.text,
768:             fontSize = 12.sp,
769:             fontWeight = FontWeight.Bold
770:         )
771:     }
772: }
```

## android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt — 1–524

Ref: `2f7cd835fad7cd3e0d5578da4a4ec65afa130983`; teljes forrás SHA256: `37cdcf0fe846f79c94568f2d583b2f4253f0c0a47f783cb582a6ee8a66bc3a5a`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/2f7cd835fad7cd3e0d5578da4a4ec65afa130983/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt

```text
1: package com.repforge.app.ui.health
2: 
3: import android.app.Application
4: import android.app.Activity
5: import android.os.Build
6: import android.os.SystemClock
7: import java.lang.ref.WeakReference
8: import com.repforge.app.NativeDataReplacement
9: import com.repforge.app.NativeHealthSync
10: import com.repforge.app.NativeHealthRefresh
11: import kotlinx.coroutines.CancellationException
12: import kotlinx.coroutines.withContext
13: import androidx.lifecycle.AndroidViewModel
14: import androidx.lifecycle.viewModelScope
15: import com.repforge.app.BleDiscoveryRepository
16: import com.repforge.app.BleDiscoveryUiState
17: import com.repforge.app.HealthBackgroundJob
18: import com.repforge.app.HealthBackgroundService
19: import com.repforge.app.HealthConnectRepository
20: import com.repforge.app.HealthJournalStore
21: import com.repforge.app.SamsungHealthRepository
22: import kotlinx.coroutines.Dispatchers
23: import kotlinx.coroutines.Job
24: import kotlinx.coroutines.flow.MutableStateFlow
25: import kotlinx.coroutines.flow.StateFlow
26: import kotlinx.coroutines.flow.asStateFlow
27: import kotlinx.coroutines.flow.update
28: import kotlinx.coroutines.launch
29: import org.json.JSONObject
30: import java.time.LocalDate
31: import kotlin.math.roundToInt
32: 
33: data class HealthMetricItemUiModel(
34:     val key: String,
35:     val label: String,
36:     val valueText: String,
37:     val unit: String,
38:     val sourceText: String,
39:     val isAvailable: Boolean
40: )
41: 
42: data class HealthDashboardUiState(
43:     val activeTab: String = "journal", // "journal" | "ble"
44:     val provider: String = "health_connect", // "health_connect" | "samsung_health"
45:     val dailySourcePreference: String = "auto", // "auto" | "priority" | specific origin
46:     val dailySourceOptions: List<String> = emptyList(),
47:     val logWatchSteps: Boolean = false,
48:     val bleBackground: Boolean = false,
49:     val hcBackground: Boolean = false,
50:     val autoOnOpen: Boolean = false,
51:     val autoOnOpenSaving: Boolean = false,
52:     val autoOnOpenSaveFailed: Boolean = false,
53:     val healthRefreshing: Boolean = false,
54:     val syncCompleted: Int = 0,
55:     val syncTotal: Int = 0,
56:     val syncPages: Int = 0,
57:     val syncStage: String = "",
58:     val syncFailed: Boolean = false,
59:     val syncPartial: Boolean = false,
60:     val syncCancelled: Boolean = false,
61:     val syncRetryFull: Boolean = false,
62:     val hcBackgroundBlock: BackgroundBlock = BackgroundBlock.UNSUPPORTED,
63:     val bleBackgroundBlock: BackgroundBlock = BackgroundBlock.WATCH_NOT_READY,
64:     val hcBackgroundScheduled: Boolean = false,
65:     val bleBackgroundRunning: Boolean = false,
66:     val backgroundSaving: Boolean = false,
67:     val backgroundWriteFailed: Boolean = false,
68:     val metrics: List<HealthMetricItemUiModel> = emptyList(),
69:     val sleepSessionsSummaryText: String = "—",
70:     val lastSyncAt: String = "—",
71:     val bleState: BleDiscoveryUiState = BleDiscoveryUiState(),
72:     val hcSupported: Boolean = false,
73:     val hcGranted: Boolean = false,
74:     val samsungInstalled: Boolean = false,
75:     val samsungAvailable: Boolean = false,
76:     val statusMessage: String? = null,
77:     val language: String = "hu",
78:     val historyMonth: String = java.time.YearMonth.now().toString(),
79:     val selectedDay: String = LocalDate.now().toString(),
80:     val availableDays: List<String> = emptyList(),
81:     val historyChannel: String = "journal",
82:     val historyLoading: Boolean = false,
83:     val historyError: Boolean = false,
84:     val historyPartial: Boolean = false,
85:     val sourceSaving: Boolean = false,
86:     val sourceSaveFailed: Boolean = false
87: )
88: 
89: class HealthDashboardViewModel(application: Application) : AndroidViewModel(application) {
90:     private val context = application.applicationContext
91:     private val healthStore = HealthJournalStore.get(context)
92:     val bleRepo = BleDiscoveryRepository(context)
93: 
94:     private val _uiState = MutableStateFlow(HealthDashboardUiState())
95:     val uiState: StateFlow<HealthDashboardUiState> = _uiState.asStateFlow()
96: 
97:     private var historyQuery = NativeHealthQuery()
98:     private var historyJob: Job? = null
99:     private val refreshGate = NativeHealthAutoRefreshCoordinator.gate
100:     private var screenActivity: WeakReference<Activity>? = null
101:     private var screenVisible = false
102:     private var screenGeneration = 0L
103:     private var providerJob: Job? = null
104:     private var samsungPermissionPending = false
105:     private var manualSamsungPermissionVisible = false
106:     private var healthRecordDetailVisible = false
107: 
108:     fun cancelHealthSync() {
109:         providerJob?.cancel()
110:         _uiState.update { it.copy(syncCancelled = true) }
111:         loadHealthDashboard(allowAutoRefresh = false)
112:     }
113: 
114:     fun syncHealth(activity: Activity?, full: Boolean = false) {
115:         if (activity != null) {
116:             _uiState.update { it.copy(syncRetryFull = full) }
117:             launchRefresh(activity, NativeHealthAutoRefreshGate.Trigger.MANUAL,
118:                 requestSamsungPermission = _uiState.value.provider == HealthJournalStore.SAMSUNG, fullSync = full)
119:         }
120:     }
121: 
122:     fun healthRecordDetailChanged(visible: Boolean) {
123:         if (healthRecordDetailVisible == visible) return
124:         healthRecordDetailVisible = visible
125:         invalidateRefresh()
126:         if (!visible && screenVisible) screenActivity?.get()?.let {
127:             launchRefresh(it, NativeHealthAutoRefreshGate.Trigger.AUTO)
128:         }
129:     }
130: 
131:     private fun invalidateRefresh() {
132:         screenGeneration++
133:         refreshGate.invalidate()
134:         providerJob?.cancel()
135:     }
136: 
137:     /** Called from the HEALTH navigation destination's lifecycle (not MainActivity alone). */
138:     fun healthScreenVisible(activity: Activity?) {
139:         if (activity == null) return
140:         if (!screenVisible || screenActivity?.get() !== activity) {
141:             invalidateRefresh()
142:             screenVisible = true
143:             screenActivity = WeakReference(activity)
144:         }
145:         if (!_uiState.value.historyLoading && _uiState.value.activeTab == "journal" &&
146:             !healthRecordDetailVisible && !manualSamsungPermissionVisible) {
147:             launchRefresh(activity, NativeHealthAutoRefreshGate.Trigger.AUTO)
148:         }
149:     }
150: 
151:     fun healthScreenHidden(activity: Activity?, force: Boolean = false) {
152:         if (!screenVisible || (activity != null && screenActivity?.get() !== activity)) return
153:         // A Samsung permission dialog may STOP the activity. Preserve only this explicitly
154:         // requested consent; publication itself still requires a RESUMED activity.
155:         if (!force && manualSamsungPermissionVisible && providerJob?.isActive == true) return
156:         screenVisible = false
157:         screenActivity = null
158:         invalidateRefresh()
159:     }
160: 
161:     override fun onCleared() {
162:         screenVisible = false
163:         invalidateRefresh()
164:         super.onCleared()
165:     }
166: 
167:     init {
168:         observeBleState()
169:         loadHealthDashboard()
170:     }
171: 
172:     private fun observeBleState() {
173:         viewModelScope.launch {
174:             bleRepo.uiState.collect { bleState ->
175:                 _uiState.value = _uiState.value.copy(bleState = bleState)
176:             }
177:         }
178:     }
179: 
180:     fun setTab(tab: String) {
181:         if (tab != _uiState.value.activeTab) invalidateRefresh()
182:         _uiState.value = _uiState.value.copy(activeTab = tab)
183:         if (tab == "ble") bleRepo.refreshStatus()
184:         else screenActivity?.get()?.let { launchRefresh(it, NativeHealthAutoRefreshGate.Trigger.AUTO) }
185:         refreshBackgroundStatus()
186:     }
187: 
188:     fun shiftHistoryMonth(delta: Long) {
189:         runCatching { historyQuery.shift(delta) }.onSuccess { invalidateRefresh(); historyQuery = it; loadHealthDashboard() }
190:     }
191:     fun selectHistoryDay(day: String) {
192:         val parsed = runCatching { LocalDate.parse(day) }.getOrNull() ?: return
193:         if (java.time.YearMonth.from(parsed) != historyQuery.month || parsed > LocalDate.now()) return
194:         invalidateRefresh(); historyQuery = historyQuery.copy(day = parsed); loadHealthDashboard()
195:     }
196:     fun historyToday() { invalidateRefresh(); historyQuery = NativeHealthQuery(); loadHealthDashboard() }
197:     fun selectHistoryChannel(channel: String) {
198:         runCatching { historyQuery.copy(channel = channel) }.onSuccess { invalidateRefresh(); historyQuery = it; loadHealthDashboard() }
199:     }
200:     fun loadHealthDashboard(allowAutoRefresh: Boolean = true) {
201:         // init also calls this method; query initialization must precede init.
202:         val query = historyQuery
203:         historyJob?.cancel()
204:         _uiState.value = _uiState.value.copy(historyLoading = true, historyError = false, metrics = emptyList(),
205:             selectedDay = query.day.toString(), historyMonth = query.month.toString(), historyChannel = query.channel,
206:             availableDays = emptyList(), sleepSessionsSummaryText = "—", historyPartial = false)
207:         historyJob = viewModelScope.launch {
208:             try {
209:                 val loaded = withContext(Dispatchers.IO) {
210:                     val access = listOf(HealthConnectRepository.isSupported(context), HealthConnectRepository.anyReadAllowed(context),
211:                         SamsungHealthRepository.isInstalled(context), SamsungHealthRepository.isSdkAvailable())
212:                     val background = NativeHealthBackgroundPolicy.snapshot(context, healthStore)
213:                     com.repforge.app.NativeDataReplacement.mutate(context) {
214:                         val language = com.repforge.app.data.store.TrainPilotSettingsStore(context).current().language
215:                         val prefs = healthStore.preferences()
216:                         val page = NativeHealthHistory(healthStore).load(query)
217:                         Triple(
218:                             Triple(language, prefs, page),
219:                             access,
220:                             background to healthStore.nativeSyncLastSuccess()
221:                         )
222:                     }
223:                 }
224:                 val (language, prefs, page) = loaded.first
225:                 val access = loaded.second
226:                 val background = loaded.third.first
227:                 val text = com.repforge.app.ui.text.nativeResources(context, language)
228:                 _uiState.value = _uiState.value.copy(language = language, historyLoading = false, historyError = false,
229:                     availableDays = page.days, metrics = NativeHealthMetrics.parse(context, page.selected, language),
230:                     historyPartial = page.selected.optBoolean("partial"),
231:                     provider = prefs.optString("provider", HealthJournalStore.HC),
232:                     dailySourcePreference = prefs.optString("hcDailySource", "auto"),
233:                     dailySourceOptions = page.dailyOrigins,
234:                     logWatchSteps = prefs.optBoolean("logWatchSteps"), bleBackground = prefs.optBoolean("bleBackground"),
235:                     hcBackground = prefs.optBoolean("hcBackground"),
236:                     autoOnOpen = prefs.optBoolean("autoOnOpen", false),
237:                     hcBackgroundBlock = background.health, bleBackgroundBlock = background.ble,
238:                     hcBackgroundScheduled = background.healthScheduled,
239:                     bleBackgroundRunning = background.bleRunning,
240:                     sleepSessionsSummaryText = text.getString(com.repforge.app.R.string.health_history_sleep,
241:                         page.selected.optJSONArray("sleepSessions")?.length() ?: 0),
242:                     lastSyncAt = loaded.third.second,
243:                     hcSupported = access[0], hcGranted = access[1], samsungInstalled = access[2], samsungAvailable = access[3])
244:                 // The first visible open can occur while preferences are still loading.
245:                 if (allowAutoRefresh && screenVisible && _uiState.value.activeTab == "journal" && !healthRecordDetailVisible) {
246:                     screenActivity?.get()?.let { launchRefresh(it, NativeHealthAutoRefreshGate.Trigger.AUTO) }
247:                 }
248:             } catch (cancelled: CancellationException) { throw cancelled }
249:             catch (_: Exception) { _uiState.value = _uiState.value.copy(historyLoading = false, historyError = true,
250:                 metrics = emptyList(), dailySourceOptions = emptyList())
251:                 refreshBackgroundStatus()
252:             }
253:         }
254:     }
255: 
256:     fun setDailySourcePreference(value: String) {
257:         val state = _uiState.value
258:         if (state.sourceSaving || state.historyLoading || state.historyError ||
259:             (value != "auto" && value != "priority" && value !in state.dailySourceOptions)) return
260:         invalidateRefresh()
261:         _uiState.update { it.copy(sourceSaving = true, sourceSaveFailed = false) }
262:         viewModelScope.launch {
263:             try {
264:                 withContext(Dispatchers.IO) {
265:                     com.repforge.app.NativeDataReplacement.mutate(context) {
266:                         healthStore.setPreferences(JSONObject().put("hcDailySource", value))
267:                     }
268:                 }
269:                 loadHealthDashboard() // Read stored values/dirty state; no implicit SDK or permission request.
270:             } catch (cancelled: CancellationException) { throw cancelled }
271:             catch (_: Exception) { _uiState.update { it.copy(sourceSaveFailed = true) } }
272:             finally { _uiState.update { it.copy(sourceSaving = false) } }
273:         }
274:     }
275: 
276:     fun setProvider(newProvider: String, activity: Activity? = null) {
277:         if (newProvider !in setOf(HealthJournalStore.HC, HealthJournalStore.SAMSUNG)) return
278:         val previousRead = providerJob
279:         invalidateRefresh()
280:         if (newProvider == HealthJournalStore.SAMSUNG) samsungPermissionPending = true
281:         viewModelScope.launch {
282:             try {
283:                 withContext(Dispatchers.IO) {
284:                     NativeDataReplacement.mutate(context) {
285:                         healthStore.setPreferences(JSONObject().put("provider", newProvider))
286:                     }
287:                 }
288:                 _uiState.update { it.copy(provider = newProvider) }
289:                 loadHealthDashboard()
290:                 // Explicit provider selection owns the permission prompt even when an old
291:                 // automatic operation was cancelled while waiting for the SDK.
292:                 if (newProvider == HealthJournalStore.SAMSUNG) {
293:                     previousRead?.join()
294:                     samsungPermissionPending = false
295:                     refresh(activity, requestSamsungPermission = true)
296:                 }
297:             } catch (cancelled: CancellationException) { throw cancelled }
298:             catch (_: Exception) {
299:                 val text = com.repforge.app.ui.text.nativeResources(context, _uiState.value.language)
300:                 _uiState.update { it.copy(statusMessage = text.getString(com.repforge.app.R.string.coach_refresh_failed)) }
301:                 loadHealthDashboard()
302:             } finally {
303:                 samsungPermissionPending = false
304:             }
305:         }
306:     }
307: 
308:     fun setAutoOnOpen(enable: Boolean) {
309:         if (_uiState.value.autoOnOpenSaving) return
310:         invalidateRefresh()
311:         _uiState.update { it.copy(autoOnOpenSaving = true, autoOnOpenSaveFailed = false) }
312:         viewModelScope.launch {
313:             try {
314:                 withContext(Dispatchers.IO) {
315:                     NativeDataReplacement.mutate(context) {
316:                         healthStore.setPreferences(JSONObject().put("autoOnOpen", enable))
317:                     }
318:                 }
319:                 _uiState.update { it.copy(autoOnOpen = enable) }
320:                 if (enable) screenActivity?.get()?.let { launchRefresh(it, NativeHealthAutoRefreshGate.Trigger.AUTO) }
321:             } catch (cancelled: CancellationException) { throw cancelled }
322:             catch (_: Exception) { _uiState.update { it.copy(autoOnOpenSaveFailed = true) } }
323:             finally { _uiState.update { it.copy(autoOnOpenSaving = false) } }
324:         }
325:     }
326: 
327:     /** Manual refresh bypasses the auto timer, but shares the same single in-flight SDK gate. */
328:     fun refresh(activity: Activity?, requestSamsungPermission: Boolean = false) {
329:         if (activity != null) launchRefresh(activity, NativeHealthAutoRefreshGate.Trigger.MANUAL, requestSamsungPermission)
330:     }
331: 
332:     private fun launchRefresh(
333:         activity: Activity,
334:         trigger: NativeHealthAutoRefreshGate.Trigger,
335:         requestSamsungPermission: Boolean = false,
336:         fullSync: Boolean? = null
337:     ) {
338:         if (!screenVisible || screenActivity?.get() !== activity ||
339:             _uiState.value.activeTab != "journal" || healthRecordDetailVisible ||
340:             (trigger == NativeHealthAutoRefreshGate.Trigger.AUTO && samsungPermissionPending)) return
341:         val previous = providerJob
342:         if (previous != null && !previous.isCompleted) {
343:             // A canceled blocking SDK read can still be unwinding. Queue the new foreground
344:             // event behind its completion; never overlap two reads or silently lose re-entry.
345:             viewModelScope.launch {
346:                 previous.join()
347:                 if (screenVisible && screenActivity?.get() === activity)
348:                     launchRefresh(activity, trigger, requestSamsungPermission, fullSync)
349:             }
350:             return
351:         }
352:         providerJob = viewModelScope.launch {
353:             var ticket: NativeHealthAutoRefreshGate.Ticket? = null
354:             try {
355:                 val visibleGeneration = screenGeneration
356:                 val selected = historyQuery.day.toString()
357:                 // PhoneWorkoutExecution.store is MAIN-ONLY; never call it inside IO/mutate.
358:                 val executionSafe = NativeHealthForegroundAdmission.safeOnMain(context)
359:                 val prepared = withContext(Dispatchers.IO) {
360:                     val hcPermitted = Build.VERSION.SDK_INT >= 34 && HealthConnectRepository.isSupported(context) &&
361:                         HealthConnectRepository.anyReadAllowed(context)
362:                     val samsungPermitted = SamsungHealthRepository.isInstalled(context) && SamsungHealthRepository.isSdkAvailable()
363:                     NativeDataReplacement.mutate(context) {
364:                         val prefs = healthStore.preferences()
365:                         val provider = prefs.optString("provider", HealthJournalStore.HC)
366:                         val permitted = when (provider) {
367:                             HealthJournalStore.HC -> hcPermitted
368:                             HealthJournalStore.SAMSUNG -> samsungPermitted
369:                             else -> false
370:                         }
371:                         Triple(
372:                             NativeHealthAutoRefreshGate.Snapshot(provider,
373:                                 if (trigger == NativeHealthAutoRefreshGate.Trigger.AUTO) LocalDate.now().toString()
374:                                 else selected,
375:                                 healthStore.generation(), visibleGeneration),
376:                             prefs.optBoolean("autoOnOpen", false),
377:                             permitted && NativeHealthForegroundAdmission.safeOffMain(context)
378:                         )
379:                     }
380:                 }
381:                 val snapshot = prepared.first
382:                 if (!screenVisible || screenGeneration != visibleGeneration || historyQuery.day.toString() != selected) return@launch
383:                 ticket = refreshGate.begin(trigger, SystemClock.elapsedRealtime(), prepared.second,
384:                     prepared.third, executionSafe && prepared.third, snapshot)
385:                 if (ticket == null) {
386:                     if (trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL) _uiState.update { it.copy(syncFailed = true) }
387:                     return@launch
388:                 }
389:                 val activeTicket = ticket!!
390:                 manualSamsungPermissionVisible = trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL &&
391:                     requestSamsungPermission && snapshot.provider == HealthJournalStore.SAMSUNG
392:                 val day = if (trigger == NativeHealthAutoRefreshGate.Trigger.AUTO) LocalDate.now()
393:                     else historyQuery.day
394:                 _uiState.update { it.copy(healthRefreshing = true, syncFailed = false, syncPartial = false,
395:                     syncCancelled = false, syncCompleted = 0, syncTotal = 0, syncPages = 0, syncStage = "PREPARING") }
396:                 val guard = {
397:                     check(screenVisible && screenActivity?.get() === activity && _uiState.value.activeTab == "journal" &&
398:                         !healthRecordDetailVisible && historyQuery.day.toString() == selected &&
399:                         screenGeneration == visibleGeneration) {
400:                         "Health screen changed while refreshing"
401:                     }
402:                     // Synchronous guard runs on provider IO threads; main-only execution is
403:                     // checked by NativeHealthRefresh *before* each protected publication.
404:                     check(NativeHealthForegroundAdmission.safeOffMain(context)) {
405:                         "Workout ownership, restore or backup blocks Health refresh"
406:                     }
407:                     if (trigger == NativeHealthAutoRefreshGate.Trigger.AUTO)
408:                         check(day == LocalDate.now()) { "Local health day changed while refreshing" }
409:                     check(refreshGate.isCurrent(activeTicket, snapshot, true, true)) {
410:                         "Health refresh result is obsolete"
411:                     }
412:                     Unit
413:                 }
414:                 // The history query is never replaced by sync output. Manual historical reads remain callable.
415:                 val result = if (trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL && fullSync == null && day < LocalDate.now()) {
416:                     NativeHealthRefresh.readDay(context, activity, day, requestSamsungPermission, guard)
417:                 } else NativeHealthRefresh.sync(context, activity, fullSync == true,
418:                     requestSamsungPermission && trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL,
419:                     progress = { value ->
420:                         _uiState.update { it.copy(syncStage = value.stage.name, syncCompleted = value.completed,
421:                             syncTotal = value.total, syncPages = value.pages) }
422:                     }, onPermissionFinished = { manualSamsungPermissionVisible = false }, requireScreenGuard = guard)
423:                 val partial = result.getOrNull()?.optBoolean("partial") == true
424:                 _uiState.update { it.copy(syncFailed = result.isFailure, syncPartial = partial) }
425:                 if (screenVisible && screenGeneration == visibleGeneration && historyQuery.day.toString() == selected) {
426:                     if (trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL) {
427:                         val text = com.repforge.app.ui.text.nativeResources(context, _uiState.value.language)
428:                         _uiState.update { it.copy(statusMessage = text.getString(if (result.isSuccess && !partial)
429:                             com.repforge.app.R.string.health_read_completed else com.repforge.app.R.string.coach_refresh_failed)) }
430:                     }
431:                     loadHealthDashboard(allowAutoRefresh = false) // Durable partial days remain visible; history selection/source/filter are retained.
432:                 }
433:             } catch (cancelled: CancellationException) { throw cancelled }
434:             catch (_: Exception) {
435:                 _uiState.update { it.copy(syncFailed = true) }
436:                 if (trigger == NativeHealthAutoRefreshGate.Trigger.MANUAL && screenVisible) {
437:                     val text = com.repforge.app.ui.text.nativeResources(context, _uiState.value.language)
438:                     _uiState.update { it.copy(statusMessage = text.getString(com.repforge.app.R.string.coach_refresh_failed)) }
439:                 }
440:             } finally {
441:                 manualSamsungPermissionVisible = false
442:                 ticket?.let { refreshGate.finish(it) }
443:                 _uiState.update { it.copy(healthRefreshing = false) }
444:             }
445:         }
446:     }
447: 
448:     /** Refresh system permission/job status independently from the potentially large journal query. */
449:     fun refreshBackgroundStatus() {
450:         viewModelScope.launch {
451:             try {
452:                 val snapshot = withContext(Dispatchers.IO) {
453:                     NativeHealthBackgroundPolicy.snapshot(context, healthStore)
454:                 }
455:                 _uiState.update { it.copy(hcBackgroundBlock = snapshot.health,
456:                     bleBackgroundBlock = snapshot.ble, hcBackgroundScheduled = snapshot.healthScheduled,
457:                     bleBackgroundRunning = snapshot.bleRunning) }
458:             } catch (cancelled: CancellationException) { throw cancelled }
459:             catch (_: Exception) { _uiState.update { it.copy(backgroundWriteFailed = true) } }
460:         }
461:     }
462: 
463:     fun toggleBleBackground(enable: Boolean) {
464:         if (_uiState.value.backgroundSaving) return
465:         _uiState.update { it.copy(backgroundSaving = true, backgroundWriteFailed = false) }
466:         viewModelScope.launch {
467:             try {
468:                 withContext(Dispatchers.IO) {
469:                     com.repforge.app.NativeDataReplacement.mutate(context) {
470:                         if (enable && NativeHealthBackgroundPolicy.snapshot(context, healthStore).ble != BackgroundBlock.READY)
471:                             throw IllegalStateException("BLE background prerequisites missing")
472:                         val previous = healthStore.preferences()
473:                         val oldBg = previous.optBoolean("bleBackground")
474:                         val oldSteps = previous.optBoolean("logWatchSteps")
475:                         healthStore.setPreferences(JSONObject().put("bleBackground", enable).put("logWatchSteps", enable))
476:                         try {
477:                             if (enable) {
478:                                 check(HealthBackgroundService.resume(context)) { "BLE service could not start" }
479:                             } else HealthBackgroundService.stop(context)
480:                         } catch (failure: Exception) {
481:                             healthStore.setPreferences(JSONObject().put("bleBackground", oldBg).put("logWatchSteps", oldSteps))
482:                             throw failure
483:                         }
484:                     }
485:                 }
486:                 loadHealthDashboard()
487:             } catch (cancelled: CancellationException) { throw cancelled }
488:             catch (_: Exception) { _uiState.update { it.copy(backgroundWriteFailed = true) } }
489:             finally {
490:                 _uiState.update { it.copy(backgroundSaving = false) }
491:                 refreshBackgroundStatus()
492:             }
493:         }
494:     }
495: 
496:     fun toggleHcBackground(enable: Boolean) {
497:         if (_uiState.value.backgroundSaving) return
498:         _uiState.update { it.copy(backgroundSaving = true, backgroundWriteFailed = false) }
499:         viewModelScope.launch {
500:             try {
501:                 withContext(Dispatchers.IO) {
502:                     com.repforge.app.NativeDataReplacement.mutate(context) {
503:                         if (enable && NativeHealthBackgroundPolicy.snapshot(context, healthStore).health != BackgroundBlock.READY)
504:                             throw IllegalStateException("Health Connect background prerequisites missing")
505:                         val previous = healthStore.preferences().optBoolean("hcBackground")
506:                         healthStore.setPreferences(JSONObject().put("hcBackground", enable))
507:                         try { HealthBackgroundJob.schedule(context) }
508:                         catch (failure: Exception) {
509:                             healthStore.setPreferences(JSONObject().put("hcBackground", previous))
510:                             try { HealthBackgroundJob.schedule(context) } catch (_: Exception) {}
511:                             throw failure
512:                         }
513:                     }
514:                 }
515:                 loadHealthDashboard()
516:             } catch (cancelled: CancellationException) { throw cancelled }
517:             catch (_: Exception) { _uiState.update { it.copy(backgroundWriteFailed = true) } }
518:             finally {
519:                 _uiState.update { it.copy(backgroundSaving = false) }
520:                 refreshBackgroundStatus()
521:             }
522:         }
523:     }
524: }
```

## android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt — 1–105

Ref: `2f7cd835fad7cd3e0d5578da4a4ec65afa130983`; teljes forrás SHA256: `3630f1ea4b905a529caca60d2a877518ae67adf2b643b883ea25dea05e1eb081`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/2f7cd835fad7cd3e0d5578da4a4ec65afa130983/android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt

```text
1: package com.repforge.app.ui.theme
2: 
3: import androidx.compose.material3.MaterialTheme
4: import androidx.compose.material3.Typography
5: import androidx.compose.material3.darkColorScheme
6: import androidx.compose.runtime.Composable
7: import androidx.compose.runtime.CompositionLocalProvider
8: import androidx.compose.runtime.staticCompositionLocalOf
9: import androidx.compose.ui.graphics.Color
10: import androidx.compose.ui.text.TextStyle
11: import androidx.compose.ui.text.font.FontFamily
12: import androidx.compose.ui.text.font.FontWeight
13: import androidx.compose.ui.unit.sp
14: import com.repforge.app.data.store.TrainPilotUserSettings
15: 
16: val BgColor = Color(0xFF0E1015)
17: val SurfaceColor = Color(0xFF161920)
18: val CardColor = Color(0xFF1A1E25)
19: val Card2Color = Color(0xFF232933)
20: val LineColor = Color(0xFF2C3440)
21: val TextColor = Color(0xFFF6F8FB)
22: val MutedColor = Color(0xFF9AA5B6)
23: val DangerColor = Color(0xFF3B232A)
24: val DangerTextColor = Color(0xFFEFBCC3)
25: val SuccessColor = Color(0xFF3CA87D)
26: 
27: data class TrainPilotExtendedColors(
28:     val bg: Color = BgColor,
29:     val card: Color = CardColor,
30:     val card2: Color = Card2Color,
31:     val line: Color = LineColor,
32:     val text: Color = TextColor,
33:     val muted: Color = MutedColor,
34:     val accent: Color,
35:     val accent2: Color,
36:     val onAccent: Color = NativeThemeTokens.onFill(accent),
37:     val onAccent2: Color = NativeThemeTokens.onFill(accent2),
38:     val accentText: Color = NativeThemeTokens.readableAccent(accent),
39:     val selectedSurface: Color = Card2Color,
40:     val selectedBorder: Color = NativeThemeTokens.readableControlAccent(accent, selectedSurface),
41:     val emphasisBorder: Color = LineColor,
42:     val danger: Color = DangerColor,
43:     val dangerText: Color = DangerTextColor,
44:     val success: Color = SuccessColor
45: )
46: 
47: val LocalTrainPilotColors = staticCompositionLocalOf {
48:     TrainPilotExtendedColors(
49:         accent = Color(0xFF4D8FBE),
50:         accent2 = Color(0xFF7CB4D6)
51:     )
52: }
53: 
54: /** Shared roles from the accepted visual specification; colors come from the surface. */
55: private fun textStyle(size: Int, height: Int, weight: FontWeight = FontWeight.Normal,
56:     numeric: Boolean = false) = TextStyle(fontFamily = FontFamily.SansSerif,
57:     fontSize = size.sp, lineHeight = height.sp, fontWeight = weight,
58:     fontFeatureSettings = if (numeric) "tnum" else null)
59: 
60: val TrainPilotTypography = Typography(
61:     headlineLarge = textStyle(36, 42, FontWeight.Bold, numeric = true),
62:     headlineMedium = textStyle(24, 30, FontWeight.Bold),
63:     headlineSmall = textStyle(24, 30, FontWeight.SemiBold),
64:     titleLarge = textStyle(18, 24, FontWeight.SemiBold),
65:     titleMedium = textStyle(16, 22, FontWeight.SemiBold),
66:     titleSmall = textStyle(14, 20, FontWeight.SemiBold),
67:     bodyLarge = textStyle(16, 24),
68:     bodyMedium = textStyle(14, 20),
69:     bodySmall = textStyle(13, 18),
70:     labelLarge = textStyle(14, 20, FontWeight.SemiBold),
71:     labelMedium = textStyle(13, 18, FontWeight.Medium),
72:     labelSmall = textStyle(12, 16, FontWeight.Medium)
73: )
74: 
75: internal fun nativeMaterialColorScheme(colors: TrainPilotExtendedColors) = darkColorScheme(
76:     primary = colors.accent, secondary = colors.accent2,
77:     background = colors.bg, surface = colors.card, surfaceVariant = colors.card2,
78:     onPrimary = colors.onAccent, onSecondary = colors.onAccent2,
79:     primaryContainer = colors.selectedSurface, onPrimaryContainer = colors.text,
80:     onBackground = colors.text, onSurface = colors.text, onSurfaceVariant = colors.muted,
81:     error = colors.dangerText, onError = colors.danger,
82:     errorContainer = colors.danger, onErrorContainer = colors.dangerText,
83:     outline = NativeThemeTokens.readableControlAccent(colors.line, colors.selectedSurface),
84:     outlineVariant = colors.line
85: )
86: 
87: @Composable
88: fun TrainPilotTheme(
89:     settings: TrainPilotUserSettings,
90:     content: @Composable () -> Unit
91: ) {
92:     val colors = androidx.compose.runtime.remember(settings.themeAccent, settings.themeAccent2, settings.themeFamily) {
93:         NativeThemeTokens.resolve(settings)
94:     }
95:     val colorScheme = androidx.compose.runtime.remember(colors) { nativeMaterialColorScheme(colors) }
96: 
97:     CompositionLocalProvider(LocalTrainPilotColors provides colors) {
98:         MaterialTheme(
99:             colorScheme = colorScheme,
100:             typography = TrainPilotTypography,
101:             shapes = TrainPilotShapes,
102:             content = content
103:         )
104:     }
105: }
```

## android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotVisualSystem.kt — 1–27

Ref: `2f7cd835fad7cd3e0d5578da4a4ec65afa130983`; teljes forrás SHA256: `62cee74a87383d62dd62726acbeb56aa48e3be31e815188ae1bd319fdbc1ff2c`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/2f7cd835fad7cd3e0d5578da4a4ec65afa130983/android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotVisualSystem.kt

```text
1: package com.repforge.app.ui.theme
2: 
3: import androidx.compose.foundation.shape.RoundedCornerShape
4: import androidx.compose.material3.Shapes
5: import androidx.compose.ui.unit.dp
6: 
7: /** Shared presentation constants, independent of data and selected palette. */
8: object TrainPilotDimensions {
9:     val Tight = 4.dp
10:     val Small = 8.dp
11:     val Gap = 12.dp
12:     val Page = 16.dp
13:     val Section = 24.dp
14:     val Control = 48.dp
15:     val PrimaryAction = 52.dp
16:     val Icon = 24.dp
17:     val CardRadius = 16.dp
18:     val HeroRadius = 20.dp
19: }
20: 
21: val TrainPilotShapes = Shapes(
22:     extraSmall = RoundedCornerShape(8.dp),
23:     small = RoundedCornerShape(12.dp),
24:     medium = RoundedCornerShape(TrainPilotDimensions.CardRadius),
25:     large = RoundedCornerShape(TrainPilotDimensions.HeroRadius),
26:     extraLarge = RoundedCornerShape(24.dp)
27: )
```

## android/app/src/main/java/com/repforge/app/ui/components/TrainPilotSurfaces.kt — 1–58

Ref: `2f7cd835fad7cd3e0d5578da4a4ec65afa130983`; teljes forrás SHA256: `397d4648288c85a6f4a99b0df264c4f4e4bf0d1cbab54776d46f0094f89620ef`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/2f7cd835fad7cd3e0d5578da4a4ec65afa130983/android/app/src/main/java/com/repforge/app/ui/components/TrainPilotSurfaces.kt

```text
1: package com.repforge.app.ui.components
2: 
3: import androidx.compose.foundation.BorderStroke
4: import androidx.compose.foundation.layout.*
5: import androidx.compose.foundation.shape.RoundedCornerShape
6: import androidx.compose.material3.*
7: import androidx.compose.runtime.Composable
8: import androidx.compose.ui.Modifier
9: import androidx.compose.ui.semantics.Role
10: import androidx.compose.ui.semantics.role
11: import androidx.compose.ui.semantics.semantics
12: import androidx.compose.ui.semantics.selected
13: import androidx.compose.ui.unit.dp
14: import com.repforge.app.ui.theme.LocalTrainPilotColors
15: import com.repforge.app.ui.theme.TrainPilotDimensions
16: 
17: /** A presentation surface; callers own all state, labels, callbacks and test tags. */
18: @Composable
19: fun TrainPilotSurfaceCard(
20:     modifier: Modifier = Modifier,
21:     selected: Boolean = false,
22:     onClick: (() -> Unit)? = null,
23:     enabled: Boolean = true,
24:     content: @Composable ColumnScope.() -> Unit
25: ) {
26:     val colors = LocalTrainPilotColors.current
27:     val shape = RoundedCornerShape(TrainPilotDimensions.CardRadius)
28:     val border = BorderStroke(if (selected) 2.dp else 1.dp,
29:         if (selected) colors.selectedBorder else colors.line)
30:     val cardModifier = if (selected) modifier.semantics { this.selected = true } else modifier
31:     val fill = if (selected) colors.selectedSurface else colors.card
32:     val body: @Composable () -> Unit = {
33:         Column(Modifier.padding(TrainPilotDimensions.Page),
34:             verticalArrangement = Arrangement.spacedBy(TrainPilotDimensions.Small), content = content)
35:     }
36:     if (onClick == null) Surface(cardModifier, shape, color = fill,
37:         contentColor = colors.text, border = border, content = body)
38:     else Surface(onClick = onClick, modifier = cardModifier.semantics { role = Role.Button },
39:         enabled = enabled, shape = shape, color = fill, contentColor = colors.text,
40:         border = border, content = body)
41: }
42: 
43: @Composable
44: fun TrainPilotSectionHeader(title: String, modifier: Modifier = Modifier) {
45:     Text(title, modifier = modifier, style = MaterialTheme.typography.titleLarge,
46:         color = LocalTrainPilotColors.current.text)
47: }
48: 
49: @Composable
50: fun TrainPilotPrimaryButton(text: String, onClick: () -> Unit,
51:     modifier: Modifier = Modifier, enabled: Boolean = true) {
52:     val colors = LocalTrainPilotColors.current
53:     Button(onClick, modifier.heightIn(min = TrainPilotDimensions.PrimaryAction), enabled = enabled,
54:         shape = RoundedCornerShape(12.dp),
55:         colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)) {
56:         Text(text, style = MaterialTheme.typography.labelLarge)
57:     }
58: }
```

## android/app/src/main/java/com/repforge/app/ui/health/HealthRecordBrowser.kt — teljes kiegészítő megjelenítő
Ref: `2f7cd835fad7cd3e0d5578da4a4ec65afa130983`; SHA256 `df9067e8b0c9259b5b5e64ab9d1a5f94b11b5257af7df7938a99a1e892b3a5f4`.
Raw: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/2f7cd835fad7cd3e0d5578da4a4ec65afa130983/android/app/src/main/java/com/repforge/app/ui/health/HealthRecordBrowser.kt
```text
1: package com.repforge.app.ui.health
2: 
3: import android.app.DatePickerDialog
4: 
5: import androidx.compose.foundation.background
6: import androidx.compose.foundation.layout.Arrangement
7: import androidx.compose.foundation.layout.Column
8: import androidx.compose.foundation.layout.Row
9: import androidx.compose.foundation.layout.fillMaxSize
10: import androidx.compose.foundation.layout.fillMaxWidth
11: import androidx.compose.foundation.layout.heightIn
12: import androidx.compose.foundation.layout.padding
13: import androidx.compose.foundation.lazy.LazyColumn
14: import androidx.compose.foundation.lazy.LazyRow
15: import androidx.compose.foundation.lazy.items
16: import androidx.compose.material.icons.Icons
17: import androidx.compose.material.icons.filled.Close
18: import androidx.compose.material3.Card
19: import androidx.compose.material3.CardDefaults
20: import androidx.compose.material3.FilterChip
21: import androidx.compose.material3.Icon
22: import androidx.compose.material3.IconButton
23: import androidx.compose.material3.OutlinedButton
24: import androidx.compose.material3.Text
25: import androidx.compose.material3.TextButton
26: import androidx.compose.runtime.Composable
27: import androidx.compose.runtime.DisposableEffect
28: import androidx.compose.runtime.LaunchedEffect
29: import androidx.compose.runtime.getValue
30: import androidx.compose.runtime.key
31: import androidx.compose.runtime.mutableStateOf
32: import androidx.compose.runtime.remember
33: import androidx.compose.runtime.saveable.rememberSaveable
34: import androidx.compose.runtime.setValue
35: import androidx.compose.ui.Modifier
36: import androidx.compose.ui.platform.LocalContext
37: import androidx.compose.ui.platform.testTag
38: import androidx.compose.ui.text.font.FontWeight
39: import androidx.compose.ui.unit.dp
40: import androidx.compose.ui.window.Dialog
41: import androidx.compose.ui.window.DialogProperties
42: import com.repforge.app.HealthJournalStore
43: import com.repforge.app.NativeDataReplacement
44: import com.repforge.app.R
45: import com.repforge.app.ui.text.nativeResources
46: import com.repforge.app.ui.theme.LocalTrainPilotColors
47: import kotlinx.coroutines.CancellationException
48: import kotlinx.coroutines.Dispatchers
49: import kotlinx.coroutines.currentCoroutineContext
50: import kotlinx.coroutines.ensureActive
51: import kotlinx.coroutines.withContext
52: import org.json.JSONObject
53: import java.text.NumberFormat
54: import java.time.Instant
55: import java.time.LocalDate
56: import java.time.ZoneId
57: import java.time.format.DateTimeFormatter
58: import java.time.format.FormatStyle
59: import java.util.Locale
60: 
61: /** A read-only detail overlay: closing it never edits measurements or changes the selected summary. */
62: @Composable
63: internal fun HealthRecordBrowser(day: String, channel: String, type: String, language: String, onClose: () -> Unit) {
64:     val context = LocalContext.current
65:     val resources = nativeResources(context, language)
66:     val colors = LocalTrainPilotColors.current
67:     var query by remember(day, channel, type) { mutableStateOf(HealthRecordQuery(LocalDate.parse(day), channel, type = type)) }
68:     var previous by remember(day, channel, type) { mutableStateOf(emptyList<HealthRecordQuery>()) }
69:     var loaded by remember(day, channel, type) { mutableStateOf<Pair<HealthRecordQuery, HealthRecordPage>?>(null) }
70:     var failure by remember(day, channel, type) { mutableStateOf<Pair<HealthRecordQuery, Boolean>?>(null) }
71:     var reloadToken by remember(day, channel, type) { mutableStateOf(0) }
72:     var draftFrom by remember(day, channel, type) { mutableStateOf(LocalDate.parse(day)) }
73:     var draftTo by remember(day, channel, type) { mutableStateOf(LocalDate.parse(day)) }
74:     var activePicker by remember { mutableStateOf<DatePickerDialog?>(null) }
75:     DisposableEffect(context, language, day, channel, type) {
76:         onDispose { activePicker?.dismiss(); activePicker = null }
77:     }
78:     val validRange = !draftFrom.isAfter(draftTo) &&
79:         HealthJournalStore.validDay(draftFrom.toString()) && HealthJournalStore.validDay(draftTo.toString())
80:     val rangeChanged = draftFrom != query.fromDay || draftTo != query.toDay
81:     val page = loaded?.takeIf { it.first == query }?.second
82:     val error = failure?.takeIf { it.first == query }
83:     val loading = page == null && error == null
84: 
85:     LaunchedEffect(query, reloadToken) {
86:         val requested = query
87:         val revision = reloadToken
88:         try {
89:             val result = withContext(Dispatchers.IO) {
90:                 NativeDataReplacement.mutate(context) {
91:                     NativeHealthRecords(HealthJournalStore.get(context)).load(requested)
92:                 }
93:             }
94:             currentCoroutineContext().ensureActive()
95:             // IO may finish after the user changes filters/reloads. Never commit a stale page.
96:             if (query == requested && reloadToken == revision) {
97:                 loaded = requested to result
98:                 failure = null
99:             }
100:         } catch (cancelled: CancellationException) { throw cancelled }
101:         catch (error: Exception) {
102:             currentCoroutineContext().ensureActive()
103:             if (query == requested && reloadToken == revision)
104:                 failure = requested to (error is NativeHealthRecords.ReplacedJournal)
105:         }
106:     }
107: 
108:     fun clearPage() {
109:         previous = emptyList()
110:         loaded = null
111:         failure = null
112:         reloadToken++
113:     }
114: 
115:     fun restart(source: String = query.source) {
116:         // Re-selecting the same query must also trigger a fresh load after a read failure.
117:         query = query.withFilters(source = source)
118:         clearPage()
119:     }
120: 
121:     fun applyRange() {
122:         if (!validRange || !rangeChanged) return
123:         query = query.withRange(draftFrom, draftTo)
124:         clearPage()
125:     }
126: 
127:     fun pickDate(selected: LocalDate, update: (LocalDate) -> Unit) {
128:         activePicker?.dismiss()
129:         // Keep the Activity window token/theme while using the app-selected language.
130:         val pickerContext = android.view.ContextThemeWrapper(context, context.theme).apply {
131:             applyOverrideConfiguration(resources.configuration)
132:         }
133:         val picker = DatePickerDialog(pickerContext, { _, year, month, pickedDay ->
134:             runCatching { LocalDate.of(year, month + 1, pickedDay) }
135:                 .getOrNull()?.takeIf { HealthJournalStore.validDay(it.toString()) }?.let(update)
136:         }, selected.year, selected.monthValue - 1, selected.dayOfMonth)
137:         picker.setOnDismissListener { if (activePicker === picker) activePicker = null }
138:         activePicker = picker
139:         picker.show()
140:     }
141:     Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
142:         Column(Modifier.fillMaxSize().background(colors.bg).padding(16.dp).testTag("health_record_browser")) {
143:             Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
144:                 Column(Modifier.weight(1f)) {
145:                     Text(resources.getString(if (type == "SleepSessionRecord") R.string.health_record_sleep else R.string.health_records_title),
146:                         color = colors.text, fontWeight = FontWeight.Bold)
147:                     Text(if (query.fromDay == query.toDay) query.fromDay.toString()
148:                         else "${query.fromDay} – ${query.toDay}", color = colors.muted)
149:                     Text(healthChannelName(channel, resources), color = colors.muted)
150:                 }
151:                 IconButton(onClick = onClose, modifier = Modifier.testTag("health_records_close")) {
152:                     Icon(Icons.Default.Close, resources.getString(R.string.health_records_close), tint = colors.danger)
153:                 }
154:             }
155:             // Pick valid calendar dates first; apply both bounds atomically to the paged SQL query.
156:             Row(Modifier.fillMaxWidth().padding(top = 12.dp),
157:                 horizontalArrangement = Arrangement.spacedBy(8.dp)) {
158:                 Column(Modifier.weight(1f)) {
159:                     Text(resources.getString(R.string.health_range_from), color = colors.muted)
160:                     OutlinedButton(onClick = { pickDate(draftFrom) { draftFrom = it } },
161:                         modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("health_range_from")) {
162:                         Text(draftFrom.toString())
163:                     }
164:                 }
165:                 Column(Modifier.weight(1f)) {
166:                     Text(resources.getString(R.string.health_range_to), color = colors.muted)
167:                     OutlinedButton(onClick = { pickDate(draftTo) { draftTo = it } },
168:                         modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("health_range_to")) {
169:                         Text(draftTo.toString())
170:                     }
171:                 }
172:             }
173:             if (!validRange) Text(resources.getString(R.string.health_range_invalid),
174:                 color = colors.dangerText, modifier = Modifier.testTag("health_range_error"))
175:             TextButton(onClick = ::applyRange, enabled = validRange && rangeChanged,
176:                 modifier = Modifier.heightIn(min = 48.dp).testTag("health_range_apply")) {
177:                 Text(resources.getString(R.string.health_range_apply))
178:             }
179:             if (loading) Text(resources.getString(R.string.health_history_loading), color = colors.muted)
180:             if (error != null) {
181:                 Text(resources.getString(if (error.second) R.string.health_records_changed else R.string.health_history_error),
182:                     color = colors.danger, modifier = Modifier.testTag("health_records_error"))
183:             }
184:             TextButton(onClick = { restart(); }, enabled = !loading, modifier = Modifier.testTag("health_records_reload")) {
185:                 Text(resources.getString(R.string.health_records_reload))
186:             }
187:             page?.let { current ->
188:                 LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
189:                     item { FilterChip(selected = query.source.isEmpty(), onClick = { restart("") },
190:                         label = { Text(resources.getString(R.string.health_records_origins)) }) }
191:                     items(current.origins, key = { it.source }) { origin ->
192:                         FilterChip(selected = query.source == origin.source, onClick = { restart(origin.source) },
193:                             modifier = Modifier.testTag("health_origin_${origin.source}"),
194:                             label = { Text(if (origin.name.isNotBlank()) "${origin.name} (${origin.source})" else origin.source) })
195:                     }
196:                 }
197:                 Text(resources.getString(R.string.health_records_raw_notice), color = colors.muted)
198:                 Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
199:                     TextButton(enabled = previous.isNotEmpty(), modifier = Modifier.testTag("health_records_previous"), onClick = {
200:                         val last = previous.last()
201:                         previous = previous.dropLast(1)
202:                         query = last.copy(generation = current.generation)
203:                     }) { Text(resources.getString(R.string.health_records_newer)) }
204:                     Text(resources.getString(R.string.health_records_page, previous.size + 1), color = colors.text,
205:                         modifier = Modifier.padding(vertical = 12.dp))
206:                     TextButton(enabled = current.next != null, modifier = Modifier.testTag("health_records_next"), onClick = {
207:                         previous = previous + query
208:                         query = query.copy(cursor = current.next, generation = current.generation)
209:                     }) { Text(resources.getString(R.string.health_records_older)) }
210:                 }
211:                 key(query, reloadToken) {
212:                     LazyColumn(Modifier.weight(1f).testTag("health_records_list"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
213:                         if (current.rows.isEmpty()) item { Text(resources.getString(R.string.health_records_empty), color = colors.muted) }
214:                         items(current.rows, key = { "${it.channel}:${it.type}:${it.id}" }) { row -> HealthRecordCard(row, language) }
215:                     }
216:                 }
217:             }
218:         }
219:     }
220: }
221: 
222: @Composable
223: private fun HealthRecordCard(row: HealthRecordRow, language: String) {
224:     val text = nativeResources(LocalContext.current, language)
225:     val colors = LocalTrainPilotColors.current
226:     val data = remember(row.data) { JSONObject(row.data) }
227:     var expanded by rememberSaveable(row.channel, row.type, row.id) { mutableStateOf(false) }
228:     var offset by rememberSaveable(row.channel, row.type, row.id) { mutableStateOf(0) }
229:     val unit = (data.opt("unit") as? String).orEmpty().let { if (it == "steps") text.getString(R.string.health_records_steps) else it }
230:     val scalar = NativeHealthRecords.number(data, "value")
231:         ?: if (row.type == "SleepSessionRecord") NativeHealthRecords.number(data, "durationMinutes") else null
232:     val displayUnit = if (row.type == "SleepSessionRecord" && data.has("durationMinutes")) "min" else unit
233:     Card(Modifier.fillMaxWidth().testTag("health_record_${row.id}"), colors = CardDefaults.cardColors(containerColor = colors.card)) {
234:         Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
235:             Text(text.getString(NativeHealthRecords.title(row.type)), color = colors.text, fontWeight = FontWeight.Bold)
236:             Text(text.getString(R.string.health_record_source, "${healthChannelName(row.channel, text)} · ${row.source}"), color = colors.muted)
237:             Text(text.getString(R.string.health_record_start, healthRecordTime(row.startMs, language)), color = colors.muted)
238:             if (row.endMs != row.startMs) Text(text.getString(R.string.health_record_end, healthRecordTime(row.endMs, language)), color = colors.muted)
239:             Text(text.getString(R.string.health_record_modified, healthRecordTime(row.modifiedMs, language)), color = colors.muted)
240:             if (row.type == "BloodPressureRecord") {
241:                 Text("${healthRecordNumber(NativeHealthRecords.number(data, "systolic"), language)} / ${healthRecordNumber(NativeHealthRecords.number(data, "diastolic"), language)} $unit", color = colors.accentText)
242:             } else if (scalar != null) {
243:                 if (row.type == "SleepSessionRecord") Text(text.getString(
244:                     if (data.optString("durationKind") == "asleep_stages") R.string.health_sleep_asleep else R.string.health_sleep_duration), color = colors.muted)
245:                 Text("${healthRecordNumber(scalar, language)} $displayUnit", color = colors.accentText)
246:             }
247:             val count = data.optJSONArray("stages")?.length() ?: data.optJSONArray("samples")?.length() ?: 0
248:             if (count > 0) {
249:                 TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("health_record_expand_${row.id}")) {
250:                     Text(text.getString(if (row.type == "SleepSessionRecord") R.string.health_records_stages else R.string.health_records_samples, count))
251:                 }
252:                 if (expanded) {
253:                     val series = remember(row.data, offset) { NativeHealthRecordSeries.page(data, offset) }
254:                     series.items.forEach { item ->
255:                         Column {
256:                             Text(if (row.type == "SleepSessionRecord") text.getString(NativeHealthRecordSeries.stageLabel(item.stage))
257:                                 else "${healthRecordNumber(item.value, language)} $unit", color = colors.text)
258:                             val start = item.startMs?.let { healthRecordTime(it, language) } ?: "—"
259:                             val end = item.endMs?.let { " – ${healthRecordTime(it, language)}" }.orEmpty()
260:                             Text(start + end, color = colors.muted)
261:                         }
262:                     }
263:                     Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
264:                         TextButton(enabled = offset > 0, onClick = { offset = maxOf(0, offset - NativeHealthRecordSeries.PAGE_SIZE) }) {
265:                             Text(text.getString(R.string.health_records_newer))
266:                         }
267:                         TextButton(enabled = offset + NativeHealthRecordSeries.PAGE_SIZE < series.total,
268:                             onClick = { offset += NativeHealthRecordSeries.PAGE_SIZE }) { Text(text.getString(R.string.health_records_older)) }
269:                     }
270:                 }
271:             } else if (scalar == null && row.type != "BloodPressureRecord") Text(text.getString(R.string.health_history_no_data), color = colors.muted)
272:         }
273:     }
274: }
275: 
276: private fun healthRecordNumber(value: Double?, language: String): String = value?.let {
277:     NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = 2 }.format(it)
278: } ?: "—"
279: private fun healthRecordTime(value: Long, language: String): String = runCatching {
280:     DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(language))
281:         .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(value))
282: }.getOrDefault("—")
283: private fun healthChannelName(channel: String, text: android.content.res.Resources): String = when (channel) {
284:     HealthJournalStore.HC -> "Health Connect"
285:     HealthJournalStore.SAMSUNG -> "Samsung Health"
286:     HealthJournalStore.BLE -> "BLE"
287:     else -> text.getString(R.string.health_history_title)
288: }
```

## Health UI tényleges resource értékek — values
Ref: `2f7cd835fad7cd3e0d5578da4a4ec65afa130983`. Csak a HealthDashboardScreen R.string hivatkozásainak meglévő értékei.
```xml
android/app/src/main/res/values/native_health_auto_refresh.xml:3: <string name="native_health_auto_refresh_title">Automatikus egészségfrissítés</string>
android/app/src/main/res/values/native_health_auto_refresh.xml:4: <string name="native_health_auto_refresh_help">Bekapcsolva az Egészség megnyitásakor frissít a szolgáltatóból, legfeljebb 10 percenként. Nem kér automatikusan engedélyt.</string>
android/app/src/main/res/values/native_health_auto_refresh.xml:5: <string name="native_health_auto_refresh_save_error">A beállítás nem menthető. Próbáld újra.</string>
android/app/src/main/res/values/native_health_auto_refresh.xml:6: <string name="native_health_auto_refresh_running">Egészségadatok frissítése…</string>
android/app/src/main/res/values/native_health_sync.xml:3: <string name="native_health_sync_now">Szinkronizálás most</string>
android/app/src/main/res/values/native_health_sync.xml:4: <string name="native_health_sync_full">Teljes 30 nap újraolvasása</string>
android/app/src/main/res/values/native_health_sync.xml:5: <string name="native_health_sync_cancel">Szinkron megszakítása</string>
android/app/src/main/res/values/native_health_sync.xml:6: <string name="native_health_sync_retry">Újrapróba</string>
android/app/src/main/res/values/native_health_sync.xml:7: <string name="native_health_sync_preparing">Hozzáférés ellenőrzése…</string>
android/app/src/main/res/values/native_health_sync.xml:8: <string name="native_health_sync_changes">Feldolgozott változásoldalak: %1$d</string>
android/app/src/main/res/values/native_health_sync.xml:9: <string name="native_health_sync_days">Feldolgozott napok: %1$d / %2$d</string>
android/app/src/main/res/values/native_health_sync.xml:10: <string name="native_health_sync_partial">Részleges szinkron. A beérkezett adatok mentve; a hiányzó mezők korábbi értéke megmaradt. Ellenőrizd az engedélyeket, majd próbáld újra.</string>
android/app/src/main/res/values/native_health_sync.xml:11: <string name="native_health_sync_failed">A szinkron nem sikerült. Ellenőrizd a szolgáltató hozzáférését, majd próbáld újra.</string>
android/app/src/main/res/values/native_health_sync.xml:12: <string name="native_health_sync_cancelled">Szinkron megszakítva. A mentett adatok megmaradnak; a következő szinkron biztonságosan folytatja.</string>
android/app/src/main/res/values/native_health_sync.xml:13: <string name="native_health_sync_last">Utolsó sikeres szinkron: %1$s</string>
android/app/src/main/res/values/strings.xml:88: <string name="metadata_write_failed">A módosítást nem sikerült menteni. Próbáld újra az adatcsere befejezése után.</string>
android/app/src/main/res/values/strings.xml:171: <string name="health_history_title">Egészségnapló</string>
android/app/src/main/res/values/strings.xml:173: <string name="health_history_error">Az adatok nem tölthetők be. Próbáld újra.</string>
android/app/src/main/res/values/strings.xml:174: <string name="health_history_loading">Betöltés…</string>
android/app/src/main/res/values/strings.xml:175: <string name="health_history_today">Ma</string>
android/app/src/main/res/values/strings.xml:176: <string name="health_history_previous">Előző hónap</string>
android/app/src/main/res/values/strings.xml:177: <string name="health_history_next">Következő hónap</string>
android/app/src/main/res/values/strings.xml:178: <string name="health_history_day">Kiválasztott nap: %1$s</string>
android/app/src/main/res/values/strings.xml:180: <string name="health_history_auto">Napi összesítés</string>
android/app/src/main/res/values/strings.xml:181: <string name="health_history_legacy">Korábbi napló</string>
android/app/src/main/res/values/strings.xml:183: <string name="weight_open">Testsúlynapló megnyitása</string>
android/app/src/main/res/values/strings.xml:206: <string name="health_records_title">Egyedi egészségmérések</string>
android/app/src/main/res/values/strings.xml:221: <string name="health_record_sleep">Alvás</string>
android/app/src/main/res/values/strings.xml:238: <string name="health_sleep_details">Alvásrészletek</string>
android/app/src/main/res/values/strings.xml:239: <string name="health_daily_source_title">Napi aktivitás forrása</string>
android/app/src/main/res/values/strings.xml:240: <string name="health_daily_source_auto">Automatikus</string>
android/app/src/main/res/values/strings.xml:241: <string name="health_daily_source_priority">Health Connect prioritás</string>
android/app/src/main/res/values/strings.xml:242: <string name="health_daily_source_hint">A lépés-, távolság- és kalóriaadatok forrását befolyásolja. A váltás után a Frissítés gombbal kérhetsz új adatokat.</string>
android/app/src/main/res/values/strings.xml:243: <string name="health_daily_source_partial">Részleges napi adat: a korábbi értékek újraolvasásra várnak.</string>
android/app/src/main/res/values/strings.xml:244: <string name="health_background_scheduled">Ütemezett háttérfrissítés.</string>
android/app/src/main/res/values/strings.xml:245: <string name="health_background_not_scheduled">Nincs ütemezett háttérfrissítés.</string>
android/app/src/main/res/values/strings.xml:246: <string name="health_background_connected">A BLE háttérkapcsolat fut.</string>
android/app/src/main/res/values/strings.xml:247: <string name="health_background_not_connected">Nincs aktív BLE háttérkapcsolat.</string>
android/app/src/main/res/values/strings.xml:248: <string name="health_background_error">A háttérszinkron beállítása sikertelen. Az előző beállítás megmaradt.</string>
android/app/src/main/res/values/strings.xml:249: <string name="health_background_ready">A szükséges feltételek rendben vannak; ez önmagában nem igazolja a futást.</string>
android/app/src/main/res/values/strings.xml:250: <string name="health_background_restore">Adat-visszaállítás folyamatban: a háttérszinkron nem kapcsolható be.</string>
android/app/src/main/res/values/strings.xml:251: <string name="health_background_unsupported">Ez a háttérfunkció nem támogatott a készüléken.</string>
android/app/src/main/res/values/strings.xml:252: <string name="health_background_read_permission">Előbb engedélyezd a Health Connect adatok olvasását a Frissítés gombbal.</string>
android/app/src/main/res/values/strings.xml:253: <string name="health_background_permission">A háttérbeli Health Connect-olvasáshoz külön engedély szükséges.</string>
android/app/src/main/res/values/strings.xml:254: <string name="health_background_watch">Előbb jegyezz meg egy kompatibilis RDFit órát.</string>
android/app/src/main/res/values/strings.xml:255: <string name="health_background_bluetooth_permission">Bluetooth-csatlakozási engedély szükséges.</string>
android/app/src/main/res/values/strings.xml:256: <string name="health_background_notification_permission">Értesítési engedély szükséges a BLE előtérszolgáltatáshoz.</string>
android/app/src/main/res/values/strings.xml:257: <string name="health_background_bluetooth_off">Kapcsold be a Bluetooth-t.</string>
android/app/src/main/res/values/strings.xml:470: <string name="native_health_ble_tab">BLE Okosóra (#111)</string>
android/app/src/main/res/values/strings.xml:471: <string name="native_health_source">Egészségügyi forrás</string>
android/app/src/main/res/values/strings.xml:472: <string name="native_health_refresh">Frissítés</string>
android/app/src/main/res/values/strings.xml:473: <string name="native_health_provider_help">A kiválasztott szolgáltató adatai képezik a napi aggregáció alapját (duplikáció és összeadás nélkül).</string>
android/app/src/main/res/values/strings.xml:474: <string name="native_health_background_title">Health Connect háttérszinkron</string>
android/app/src/main/res/values/strings.xml:475: <string name="native_health_background_help">Periodikus 15 perces háttérfrissítés (ha támogatott és engedélyezett)</string>
android/app/src/main/res/values/strings.xml:476: <string name="native_health_ble_search">Bluetooth LE Keresés</string>
android/app/src/main/res/values/strings.xml:477: <string name="native_health_ble_state">Állapot: %1$s (%2$s)</string>
android/app/src/main/res/values/strings.xml:478: <string name="native_health_ble_stop">Leállítás</string>
android/app/src/main/res/values/strings.xml:479: <string name="native_health_ble_scan">Keresés (60 mp)</string>
android/app/src/main/res/values/strings.xml:480: <string name="native_health_ble_remaining">%1$d mp van hátra</string>
android/app/src/main/res/values/strings.xml:481: <string name="native_health_ble_disconnect">Bontás</string>
android/app/src/main/res/values/strings.xml:482: <string name="native_health_ble_heart_rate">Élő pulzus (0x2A37)</string>
android/app/src/main/res/values/strings.xml:483: <string name="native_health_ble_listen">Figyelés</string>
android/app/src/main/res/values/strings.xml:484: <string name="native_health_ble_rd_fit">RDFit mérés (0xED)</string>
android/app/src/main/res/values/strings.xml:485: <string name="native_health_ble_request">Lekérdezés</string>
android/app/src/main/res/values/strings.xml:486: <string name="native_health_ble_remember">Óra megjegyzése</string>
android/app/src/main/res/values/strings.xml:487: <string name="native_health_ble_forget">Elfelejtés</string>
android/app/src/main/res/values/strings.xml:488: <string name="native_health_ble_background">Háttérbeli lépésszinkron (BLE)</string>
android/app/src/main/res/values/strings.xml:489: <string name="native_health_ble_background_help">A kiválasztott RDFit óra lépésszámának 5 percenkénti frissítése</string>
android/app/src/main/res/values/strings.xml:490: <string name="native_health_ble_found">Felfedezett eszközök (%1$d)</string>
android/app/src/main/res/values/strings.xml:491: <string name="native_health_ble_rssi">%1$s • RSSI: %2$d dBm</string>
android/app/src/main/res/values/strings.xml:492: <string name="native_health_ble_saved">★ Megjegyzett óra</string>
android/app/src/main/res/values/strings.xml:493: <string name="native_health_ble_connect">Kapcsolódás</string>
android/app/src/main/res/values/strings.xml:651: <string name="native_health_ble_connected_watch">Csatlakoztatott óra</string>
android/app/src/main/res/values/strings.xml:652: <string name="native_health_ble_steps_count">%1$d lépés</string>
android/app/src/main/res/values/strings.xml:653: <string name="native_health_ble_unnamed_device">Névtelen eszköz</string>
```

## Health UI tényleges resource értékek — values-en
Ref: `2f7cd835fad7cd3e0d5578da4a4ec65afa130983`. Csak a HealthDashboardScreen R.string hivatkozásainak meglévő értékei.
```xml
android/app/src/main/res/values-en/native_health_auto_refresh.xml:3: <string name="native_health_auto_refresh_title">Automatic health refresh</string>
android/app/src/main/res/values-en/native_health_auto_refresh.xml:4: <string name="native_health_auto_refresh_help">When enabled, refreshes from the provider on opening Health, at most once every 10 minutes. Never requests permission automatically.</string>
android/app/src/main/res/values-en/native_health_auto_refresh.xml:5: <string name="native_health_auto_refresh_save_error">Could not save the setting. Please try again.</string>
android/app/src/main/res/values-en/native_health_auto_refresh.xml:6: <string name="native_health_auto_refresh_running">Refreshing health measurements…</string>
android/app/src/main/res/values-en/native_health_sync.xml:3: <string name="native_health_sync_now">Sync now</string>
android/app/src/main/res/values-en/native_health_sync.xml:4: <string name="native_health_sync_full">Read all 30 days</string>
android/app/src/main/res/values-en/native_health_sync.xml:5: <string name="native_health_sync_cancel">Cancel sync</string>
android/app/src/main/res/values-en/native_health_sync.xml:6: <string name="native_health_sync_retry">Retry</string>
android/app/src/main/res/values-en/native_health_sync.xml:7: <string name="native_health_sync_preparing">Checking access…</string>
android/app/src/main/res/values-en/native_health_sync.xml:8: <string name="native_health_sync_changes">Change pages applied: %1$d</string>
android/app/src/main/res/values-en/native_health_sync.xml:9: <string name="native_health_sync_days">Days processed: %1$d / %2$d</string>
android/app/src/main/res/values-en/native_health_sync.xml:10: <string name="native_health_sync_partial">Partial sync. Received data is saved; previous values are retained for missing fields. Check permissions and retry.</string>
android/app/src/main/res/values-en/native_health_sync.xml:11: <string name="native_health_sync_failed">Sync failed. Check provider access and retry.</string>
android/app/src/main/res/values-en/native_health_sync.xml:12: <string name="native_health_sync_cancelled">Sync cancelled. Saved data is retained; the next sync resumes safely.</string>
android/app/src/main/res/values-en/native_health_sync.xml:13: <string name="native_health_sync_last">Last successful sync: %1$s</string>
android/app/src/main/res/values-en/strings.xml:88: <string name="metadata_write_failed">The change could not be saved. Try again after data replacement finishes.</string>
android/app/src/main/res/values-en/strings.xml:171: <string name="health_history_title">Health journal</string>
android/app/src/main/res/values-en/strings.xml:173: <string name="health_history_error">Could not load data. Try again.</string>
android/app/src/main/res/values-en/strings.xml:174: <string name="health_history_loading">Loading…</string>
android/app/src/main/res/values-en/strings.xml:175: <string name="health_history_today">Today</string>
android/app/src/main/res/values-en/strings.xml:176: <string name="health_history_previous">Previous month</string>
android/app/src/main/res/values-en/strings.xml:177: <string name="health_history_next">Next month</string>
android/app/src/main/res/values-en/strings.xml:178: <string name="health_history_day">Selected day: %1$s</string>
android/app/src/main/res/values-en/strings.xml:180: <string name="health_history_auto">Daily summary</string>
android/app/src/main/res/values-en/strings.xml:181: <string name="health_history_legacy">Previous journal</string>
android/app/src/main/res/values-en/strings.xml:183: <string name="weight_open">Open weight journal</string>
android/app/src/main/res/values-en/strings.xml:206: <string name="health_records_title">Individual health measurements</string>
android/app/src/main/res/values-en/strings.xml:221: <string name="health_record_sleep">Sleep</string>
android/app/src/main/res/values-en/strings.xml:238: <string name="health_sleep_details">Sleep details</string>
android/app/src/main/res/values-en/strings.xml:239: <string name="health_daily_source_title">Daily activity source</string>
android/app/src/main/res/values-en/strings.xml:240: <string name="health_daily_source_auto">Automatic</string>
android/app/src/main/res/values-en/strings.xml:241: <string name="health_daily_source_priority">Health Connect priority</string>
android/app/src/main/res/values-en/strings.xml:242: <string name="health_daily_source_hint">Affects the source of steps, distance and calories. Use Refresh after changing this setting to read new data.</string>
android/app/src/main/res/values-en/strings.xml:243: <string name="health_daily_source_partial">Partial daily data: previous values are awaiting a fresh read.</string>
android/app/src/main/res/values-en/strings.xml:244: <string name="health_background_scheduled">Background refresh scheduled.</string>
android/app/src/main/res/values-en/strings.xml:245: <string name="health_background_not_scheduled">No background refresh scheduled.</string>
android/app/src/main/res/values-en/strings.xml:246: <string name="health_background_connected">BLE background connection is running.</string>
android/app/src/main/res/values-en/strings.xml:247: <string name="health_background_not_connected">No active BLE background connection.</string>
android/app/src/main/res/values-en/strings.xml:248: <string name="health_background_error">Background sync could not be configured. The previous setting was kept.</string>
android/app/src/main/res/values-en/strings.xml:249: <string name="health_background_ready">Prerequisites are satisfied; this alone does not prove the job is running.</string>
android/app/src/main/res/values-en/strings.xml:250: <string name="health_background_restore">Data restore in progress: background sync cannot be enabled.</string>
android/app/src/main/res/values-en/strings.xml:251: <string name="health_background_unsupported">This background feature is not supported on this device.</string>
android/app/src/main/res/values-en/strings.xml:252: <string name="health_background_read_permission">Allow Health Connect reads using Refresh before enabling background sync.</string>
android/app/src/main/res/values-en/strings.xml:253: <string name="health_background_permission">Background Health Connect reads require a separate permission.</string>
android/app/src/main/res/values-en/strings.xml:254: <string name="health_background_watch">Remember a compatible RDFit watch first.</string>
android/app/src/main/res/values-en/strings.xml:255: <string name="health_background_bluetooth_permission">Bluetooth connection permission is required.</string>
android/app/src/main/res/values-en/strings.xml:256: <string name="health_background_notification_permission">Notification permission is required for the BLE foreground service.</string>
android/app/src/main/res/values-en/strings.xml:257: <string name="health_background_bluetooth_off">Turn on Bluetooth.</string>
android/app/src/main/res/values-en/strings.xml:470: <string name="native_health_ble_tab">BLE smartwatch (#111)</string>
android/app/src/main/res/values-en/strings.xml:471: <string name="native_health_source">Health data source</string>
android/app/src/main/res/values-en/strings.xml:472: <string name="native_health_refresh">Refresh</string>
android/app/src/main/res/values-en/strings.xml:473: <string name="native_health_provider_help">The selected provider supplies daily totals (without duplication or adding overlapping data).</string>
android/app/src/main/res/values-en/strings.xml:474: <string name="native_health_background_title">Health Connect background sync</string>
android/app/src/main/res/values-en/strings.xml:475: <string name="native_health_background_help">Background refresh every 15 minutes (when supported and allowed)</string>
android/app/src/main/res/values-en/strings.xml:476: <string name="native_health_ble_search">Bluetooth LE scan</string>
android/app/src/main/res/values-en/strings.xml:477: <string name="native_health_ble_state">Status: %1$s (%2$s)</string>
android/app/src/main/res/values-en/strings.xml:478: <string name="native_health_ble_stop">Stop</string>
android/app/src/main/res/values-en/strings.xml:479: <string name="native_health_ble_scan">Scan (60 s)</string>
android/app/src/main/res/values-en/strings.xml:480: <string name="native_health_ble_remaining">%1$d s remaining</string>
android/app/src/main/res/values-en/strings.xml:481: <string name="native_health_ble_disconnect">Disconnect</string>
android/app/src/main/res/values-en/strings.xml:482: <string name="native_health_ble_heart_rate">Live heart rate (0x2A37)</string>
android/app/src/main/res/values-en/strings.xml:483: <string name="native_health_ble_listen">Listen</string>
android/app/src/main/res/values-en/strings.xml:484: <string name="native_health_ble_rd_fit">RDFit measurement (0xED)</string>
android/app/src/main/res/values-en/strings.xml:485: <string name="native_health_ble_request">Request</string>
android/app/src/main/res/values-en/strings.xml:486: <string name="native_health_ble_remember">Remember watch</string>
android/app/src/main/res/values-en/strings.xml:487: <string name="native_health_ble_forget">Forget</string>
android/app/src/main/res/values-en/strings.xml:488: <string name="native_health_ble_background">Background step sync (BLE)</string>
android/app/src/main/res/values-en/strings.xml:489: <string name="native_health_ble_background_help">Update step count from the selected RDFit watch every 5 minutes</string>
android/app/src/main/res/values-en/strings.xml:490: <string name="native_health_ble_found">Discovered devices (%1$d)</string>
android/app/src/main/res/values-en/strings.xml:491: <string name="native_health_ble_rssi">%1$s • RSSI: %2$d dBm</string>
android/app/src/main/res/values-en/strings.xml:492: <string name="native_health_ble_saved">★ Remembered watch</string>
android/app/src/main/res/values-en/strings.xml:493: <string name="native_health_ble_connect">Connect</string>
android/app/src/main/res/values-en/strings.xml:651: <string name="native_health_ble_connected_watch">Connected watch</string>
android/app/src/main/res/values-en/strings.xml:652: <string name="native_health_ble_steps_count">%1$d steps</string>
android/app/src/main/res/values-en/strings.xml:653: <string name="native_health_ble_unnamed_device">Unnamed device</string>
```

## Health UI tényleges resource értékek — values-de
Ref: `2f7cd835fad7cd3e0d5578da4a4ec65afa130983`. Csak a HealthDashboardScreen R.string hivatkozásainak meglévő értékei.
```xml
android/app/src/main/res/values-de/native_health_auto_refresh.xml:3: <string name="native_health_auto_refresh_title">Automatische Gesundheitsaktualisierung</string>
android/app/src/main/res/values-de/native_health_auto_refresh.xml:4: <string name="native_health_auto_refresh_help">Wenn aktiviert, werden die Daten beim Öffnen von Gesundheit höchstens alle 10 Minuten vom Anbieter aktualisiert. Berechtigungen werden nicht automatisch angefordert.</string>
android/app/src/main/res/values-de/native_health_auto_refresh.xml:5: <string name="native_health_auto_refresh_save_error">Die Einstellung konnte nicht gespeichert werden. Bitte erneut versuchen.</string>
android/app/src/main/res/values-de/native_health_auto_refresh.xml:6: <string name="native_health_auto_refresh_running">Gesundheitsdaten werden aktualisiert…</string>
android/app/src/main/res/values-de/native_health_sync.xml:3: <string name="native_health_sync_now">Jetzt synchronisieren</string>
android/app/src/main/res/values-de/native_health_sync.xml:4: <string name="native_health_sync_full">Alle 30 Tage erneut lesen</string>
android/app/src/main/res/values-de/native_health_sync.xml:5: <string name="native_health_sync_cancel">Synchronisierung abbrechen</string>
android/app/src/main/res/values-de/native_health_sync.xml:6: <string name="native_health_sync_retry">Erneut versuchen</string>
android/app/src/main/res/values-de/native_health_sync.xml:7: <string name="native_health_sync_preparing">Zugriff wird geprüft…</string>
android/app/src/main/res/values-de/native_health_sync.xml:8: <string name="native_health_sync_changes">Verarbeitete Änderungsseiten: %1$d</string>
android/app/src/main/res/values-de/native_health_sync.xml:9: <string name="native_health_sync_days">Verarbeitete Tage: %1$d / %2$d</string>
android/app/src/main/res/values-de/native_health_sync.xml:10: <string name="native_health_sync_partial">Teilweise synchronisiert. Empfangene Daten sind gespeichert; bisherige Werte bleiben für fehlende Felder erhalten. Berechtigungen prüfen und erneut versuchen.</string>
android/app/src/main/res/values-de/native_health_sync.xml:11: <string name="native_health_sync_failed">Synchronisierung fehlgeschlagen. Anbieterzugriff prüfen und erneut versuchen.</string>
android/app/src/main/res/values-de/native_health_sync.xml:12: <string name="native_health_sync_cancelled">Synchronisierung abgebrochen. Gespeicherte Daten bleiben erhalten; die nächste Synchronisierung setzt sicher fort.</string>
android/app/src/main/res/values-de/native_health_sync.xml:13: <string name="native_health_sync_last">Letzte erfolgreiche Synchronisierung: %1$s</string>
android/app/src/main/res/values-de/strings.xml:88: <string name="metadata_write_failed">Die Änderung konnte nicht gespeichert werden. Versuche es nach dem Datenaustausch erneut.</string>
android/app/src/main/res/values-de/strings.xml:171: <string name="health_history_title">Gesundheitsjournal</string>
android/app/src/main/res/values-de/strings.xml:173: <string name="health_history_error">Daten konnten nicht geladen werden. Erneut versuchen.</string>
android/app/src/main/res/values-de/strings.xml:174: <string name="health_history_loading">Laden…</string>
android/app/src/main/res/values-de/strings.xml:175: <string name="health_history_today">Heute</string>
android/app/src/main/res/values-de/strings.xml:176: <string name="health_history_previous">Vorheriger Monat</string>
android/app/src/main/res/values-de/strings.xml:177: <string name="health_history_next">Nächster Monat</string>
android/app/src/main/res/values-de/strings.xml:178: <string name="health_history_day">Ausgewählter Tag: %1$s</string>
android/app/src/main/res/values-de/strings.xml:180: <string name="health_history_auto">Tagesübersicht</string>
android/app/src/main/res/values-de/strings.xml:181: <string name="health_history_legacy">Bisheriges Journal</string>
android/app/src/main/res/values-de/strings.xml:183: <string name="weight_open">Gewichtsprotokoll öffnen</string>
android/app/src/main/res/values-de/strings.xml:206: <string name="health_records_title">Einzelne Gesundheitsmessungen</string>
android/app/src/main/res/values-de/strings.xml:221: <string name="health_record_sleep">Schlaf</string>
android/app/src/main/res/values-de/strings.xml:238: <string name="health_sleep_details">Schlafdetails</string>
android/app/src/main/res/values-de/strings.xml:239: <string name="health_daily_source_title">Quelle der Tagesaktivität</string>
android/app/src/main/res/values-de/strings.xml:240: <string name="health_daily_source_auto">Automatisch</string>
android/app/src/main/res/values-de/strings.xml:241: <string name="health_daily_source_priority">Health Connect-Priorität</string>
android/app/src/main/res/values-de/strings.xml:242: <string name="health_daily_source_hint">Beeinflusst die Quelle für Schritte, Entfernung und Kalorien. Lies nach einer Änderung über Aktualisieren neue Daten ein.</string>
android/app/src/main/res/values-de/strings.xml:243: <string name="health_daily_source_partial">Unvollständige Tagesdaten: frühere Werte müssen neu eingelesen werden.</string>
android/app/src/main/res/values-de/strings.xml:244: <string name="health_background_scheduled">Hintergrundaktualisierung eingeplant.</string>
android/app/src/main/res/values-de/strings.xml:245: <string name="health_background_not_scheduled">Keine Hintergrundaktualisierung eingeplant.</string>
android/app/src/main/res/values-de/strings.xml:246: <string name="health_background_connected">BLE-Hintergrundverbindung läuft.</string>
android/app/src/main/res/values-de/strings.xml:247: <string name="health_background_not_connected">Keine aktive BLE-Hintergrundverbindung.</string>
android/app/src/main/res/values-de/strings.xml:248: <string name="health_background_error">Hintergrundabgleich konnte nicht eingerichtet werden. Die bisherige Einstellung bleibt erhalten.</string>
android/app/src/main/res/values-de/strings.xml:249: <string name="health_background_ready">Voraussetzungen erfüllt; das beweist noch keinen laufenden Auftrag.</string>
android/app/src/main/res/values-de/strings.xml:250: <string name="health_background_restore">Wiederherstellung läuft: Hintergrundabgleich kann nicht aktiviert werden.</string>
android/app/src/main/res/values-de/strings.xml:251: <string name="health_background_unsupported">Diese Hintergrundfunktion wird auf diesem Gerät nicht unterstützt.</string>
android/app/src/main/res/values-de/strings.xml:252: <string name="health_background_read_permission">Erlaube zuerst den Health-Connect-Lesezugriff über Aktualisieren.</string>
android/app/src/main/res/values-de/strings.xml:253: <string name="health_background_permission">Für Health Connect im Hintergrund ist eine separate Berechtigung erforderlich.</string>
android/app/src/main/res/values-de/strings.xml:254: <string name="health_background_watch">Speichere zuerst eine kompatible RDFit-Uhr.</string>
android/app/src/main/res/values-de/strings.xml:255: <string name="health_background_bluetooth_permission">Bluetooth-Verbindungsberechtigung erforderlich.</string>
android/app/src/main/res/values-de/strings.xml:256: <string name="health_background_notification_permission">Für den BLE-Vordergrunddienst ist die Benachrichtigungsberechtigung erforderlich.</string>
android/app/src/main/res/values-de/strings.xml:257: <string name="health_background_bluetooth_off">Aktiviere Bluetooth.</string>
android/app/src/main/res/values-de/strings.xml:470: <string name="native_health_ble_tab">BLE-Smartwatch (#111)</string>
android/app/src/main/res/values-de/strings.xml:471: <string name="native_health_source">Gesundheitsdatenquelle</string>
android/app/src/main/res/values-de/strings.xml:472: <string name="native_health_refresh">Aktualisieren</string>
android/app/src/main/res/values-de/strings.xml:473: <string name="native_health_provider_help">Die gewählte Quelle liefert die Tageswerte (ohne Duplikate oder Addition überlappender Daten).</string>
android/app/src/main/res/values-de/strings.xml:474: <string name="native_health_background_title">Health Connect Hintergrund-Synchronisierung</string>
android/app/src/main/res/values-de/strings.xml:475: <string name="native_health_background_help">Hintergrundaktualisierung alle 15 Minuten (falls unterstützt und erlaubt)</string>
android/app/src/main/res/values-de/strings.xml:476: <string name="native_health_ble_search">Bluetooth-LE-Suche</string>
android/app/src/main/res/values-de/strings.xml:477: <string name="native_health_ble_state">Status: %1$s (%2$s)</string>
android/app/src/main/res/values-de/strings.xml:478: <string name="native_health_ble_stop">Stoppen</string>
android/app/src/main/res/values-de/strings.xml:479: <string name="native_health_ble_scan">Suche (60 s)</string>
android/app/src/main/res/values-de/strings.xml:480: <string name="native_health_ble_remaining">Noch %1$d s</string>
android/app/src/main/res/values-de/strings.xml:481: <string name="native_health_ble_disconnect">Trennen</string>
android/app/src/main/res/values-de/strings.xml:482: <string name="native_health_ble_heart_rate">Live-Puls (0x2A37)</string>
android/app/src/main/res/values-de/strings.xml:483: <string name="native_health_ble_listen">Mithören</string>
android/app/src/main/res/values-de/strings.xml:484: <string name="native_health_ble_rd_fit">RDFit-Messung (0xED)</string>
android/app/src/main/res/values-de/strings.xml:485: <string name="native_health_ble_request">Abfragen</string>
android/app/src/main/res/values-de/strings.xml:486: <string name="native_health_ble_remember">Uhr merken</string>
android/app/src/main/res/values-de/strings.xml:487: <string name="native_health_ble_forget">Vergessen</string>
android/app/src/main/res/values-de/strings.xml:488: <string name="native_health_ble_background">Schritte im Hintergrund synchronisieren (BLE)</string>
android/app/src/main/res/values-de/strings.xml:489: <string name="native_health_ble_background_help">Schrittzahl der ausgewählten RDFit-Uhr alle 5 Minuten aktualisieren</string>
android/app/src/main/res/values-de/strings.xml:490: <string name="native_health_ble_found">Gefundene Geräte (%1$d)</string>
android/app/src/main/res/values-de/strings.xml:491: <string name="native_health_ble_rssi">%1$s • RSSI: %2$d dBm</string>
android/app/src/main/res/values-de/strings.xml:492: <string name="native_health_ble_saved">★ Gespeicherte Uhr</string>
android/app/src/main/res/values-de/strings.xml:493: <string name="native_health_ble_connect">Verbinden</string>
android/app/src/main/res/values-de/strings.xml:651: <string name="native_health_ble_connected_watch">Verbundene Uhr</string>
android/app/src/main/res/values-de/strings.xml:652: <string name="native_health_ble_steps_count">%1$d Schritte</string>
android/app/src/main/res/values-de/strings.xml:653: <string name="native_health_ble_unnamed_device">Unbenanntes Gerät</string>
```

## Health UI tényleges resource értékek — values-ro
Ref: `2f7cd835fad7cd3e0d5578da4a4ec65afa130983`. Csak a HealthDashboardScreen R.string hivatkozásainak meglévő értékei.
```xml
android/app/src/main/res/values-ro/native_health_auto_refresh.xml:3: <string name="native_health_auto_refresh_title">Actualizare automată a datelor de sănătate</string>
android/app/src/main/res/values-ro/native_health_auto_refresh.xml:4: <string name="native_health_auto_refresh_help">Când este activată, actualizează datele de la furnizor la deschiderea ecranului Sănătate, cel mult o dată la 10 minute. Nu solicită automat permisiuni.</string>
android/app/src/main/res/values-ro/native_health_auto_refresh.xml:5: <string name="native_health_auto_refresh_save_error">Setarea nu a putut fi salvată. Încearcă din nou.</string>
android/app/src/main/res/values-ro/native_health_auto_refresh.xml:6: <string name="native_health_auto_refresh_running">Se actualizează datele de sănătate…</string>
android/app/src/main/res/values-ro/native_health_sync.xml:3: <string name="native_health_sync_now">Sincronizează acum</string>
android/app/src/main/res/values-ro/native_health_sync.xml:4: <string name="native_health_sync_full">Recitește toate cele 30 de zile</string>
android/app/src/main/res/values-ro/native_health_sync.xml:5: <string name="native_health_sync_cancel">Anulează sincronizarea</string>
android/app/src/main/res/values-ro/native_health_sync.xml:6: <string name="native_health_sync_retry">Reîncearcă</string>
android/app/src/main/res/values-ro/native_health_sync.xml:7: <string name="native_health_sync_preparing">Se verifică accesul…</string>
android/app/src/main/res/values-ro/native_health_sync.xml:8: <string name="native_health_sync_changes">Pagini de modificări procesate: %1$d</string>
android/app/src/main/res/values-ro/native_health_sync.xml:9: <string name="native_health_sync_days">Zile procesate: %1$d / %2$d</string>
android/app/src/main/res/values-ro/native_health_sync.xml:10: <string name="native_health_sync_partial">Sincronizare parțială. Datele primite sunt salvate; valorile anterioare ale câmpurilor lipsă sunt păstrate. Verifică permisiunile și reîncearcă.</string>
android/app/src/main/res/values-ro/native_health_sync.xml:11: <string name="native_health_sync_failed">Sincronizarea a eșuat. Verifică accesul la furnizor și reîncearcă.</string>
android/app/src/main/res/values-ro/native_health_sync.xml:12: <string name="native_health_sync_cancelled">Sincronizare anulată. Datele salvate sunt păstrate; următoarea sincronizare continuă în siguranță.</string>
android/app/src/main/res/values-ro/native_health_sync.xml:13: <string name="native_health_sync_last">Ultima sincronizare reușită: %1$s</string>
android/app/src/main/res/values-ro/strings.xml:88: <string name="metadata_write_failed">Modificarea nu a putut fi salvată. Încearcă din nou după încheierea înlocuirii datelor.</string>
android/app/src/main/res/values-ro/strings.xml:171: <string name="health_history_title">Jurnal de sănătate</string>
android/app/src/main/res/values-ro/strings.xml:173: <string name="health_history_error">Datele nu au putut fi încărcate. Încearcă din nou.</string>
android/app/src/main/res/values-ro/strings.xml:174: <string name="health_history_loading">Se încarcă…</string>
android/app/src/main/res/values-ro/strings.xml:175: <string name="health_history_today">Azi</string>
android/app/src/main/res/values-ro/strings.xml:176: <string name="health_history_previous">Luna precedentă</string>
android/app/src/main/res/values-ro/strings.xml:177: <string name="health_history_next">Luna următoare</string>
android/app/src/main/res/values-ro/strings.xml:178: <string name="health_history_day">Zi selectată: %1$s</string>
android/app/src/main/res/values-ro/strings.xml:180: <string name="health_history_auto">Rezumat zilnic</string>
android/app/src/main/res/values-ro/strings.xml:181: <string name="health_history_legacy">Jurnal anterior</string>
android/app/src/main/res/values-ro/strings.xml:183: <string name="weight_open">Deschide jurnalul de greutate</string>
android/app/src/main/res/values-ro/strings.xml:206: <string name="health_records_title">Măsurări individuale de sănătate</string>
android/app/src/main/res/values-ro/strings.xml:221: <string name="health_record_sleep">Somn</string>
android/app/src/main/res/values-ro/strings.xml:238: <string name="health_sleep_details">Detalii despre somn</string>
android/app/src/main/res/values-ro/strings.xml:239: <string name="health_daily_source_title">Sursa activității zilnice</string>
android/app/src/main/res/values-ro/strings.xml:240: <string name="health_daily_source_auto">Automat</string>
android/app/src/main/res/values-ro/strings.xml:241: <string name="health_daily_source_priority">Prioritate Health Connect</string>
android/app/src/main/res/values-ro/strings.xml:242: <string name="health_daily_source_hint">Influențează sursa pașilor, distanței și caloriilor. Folosește Actualizare după schimbare pentru a citi date noi.</string>
android/app/src/main/res/values-ro/strings.xml:243: <string name="health_daily_source_partial">Date zilnice parțiale: valorile anterioare așteaptă o citire nouă.</string>
android/app/src/main/res/values-ro/strings.xml:244: <string name="health_background_scheduled">Actualizarea în fundal este programată.</string>
android/app/src/main/res/values-ro/strings.xml:245: <string name="health_background_not_scheduled">Nu există actualizare în fundal programată.</string>
android/app/src/main/res/values-ro/strings.xml:246: <string name="health_background_connected">Conexiunea BLE în fundal rulează.</string>
android/app/src/main/res/values-ro/strings.xml:247: <string name="health_background_not_connected">Nu există conexiune BLE activă în fundal.</string>
android/app/src/main/res/values-ro/strings.xml:248: <string name="health_background_error">Sincronizarea în fundal nu a putut fi configurată. Setarea anterioară a fost păstrată.</string>
android/app/src/main/res/values-ro/strings.xml:249: <string name="health_background_ready">Condițiile sunt îndeplinite; aceasta nu confirmă executarea.</string>
android/app/src/main/res/values-ro/strings.xml:250: <string name="health_background_restore">Restaurarea datelor este în curs: sincronizarea în fundal nu poate fi activată.</string>
android/app/src/main/res/values-ro/strings.xml:251: <string name="health_background_unsupported">Această funcție în fundal nu este acceptată pe dispozitiv.</string>
android/app/src/main/res/values-ro/strings.xml:252: <string name="health_background_read_permission">Permite mai întâi citirea Health Connect folosind Actualizare.</string>
android/app/src/main/res/values-ro/strings.xml:253: <string name="health_background_permission">Citirea Health Connect în fundal necesită o permisiune separată.</string>
android/app/src/main/res/values-ro/strings.xml:254: <string name="health_background_watch">Memorează mai întâi un ceas RDFit compatibil.</string>
android/app/src/main/res/values-ro/strings.xml:255: <string name="health_background_bluetooth_permission">Este necesară permisiunea de conectare Bluetooth.</string>
android/app/src/main/res/values-ro/strings.xml:256: <string name="health_background_notification_permission">Este necesară permisiunea pentru notificări a serviciului BLE.</string>
android/app/src/main/res/values-ro/strings.xml:257: <string name="health_background_bluetooth_off">Activează Bluetooth.</string>
android/app/src/main/res/values-ro/strings.xml:470: <string name="native_health_ble_tab">Ceas inteligent BLE (#111)</string>
android/app/src/main/res/values-ro/strings.xml:471: <string name="native_health_source">Sursă date sănătate</string>
android/app/src/main/res/values-ro/strings.xml:472: <string name="native_health_refresh">Reîmprospătează</string>
android/app/src/main/res/values-ro/strings.xml:473: <string name="native_health_provider_help">Furnizorul selectat furnizează totalurile zilnice (fără duplicare sau însumarea datelor suprapuse).</string>
android/app/src/main/res/values-ro/strings.xml:474: <string name="native_health_background_title">Sincronizare Health Connect în fundal</string>
android/app/src/main/res/values-ro/strings.xml:475: <string name="native_health_background_help">Actualizare în fundal la fiecare 15 minute (dacă este acceptată și permisă)</string>
android/app/src/main/res/values-ro/strings.xml:476: <string name="native_health_ble_search">Căutare Bluetooth LE</string>
android/app/src/main/res/values-ro/strings.xml:477: <string name="native_health_ble_state">Stare: %1$s (%2$s)</string>
android/app/src/main/res/values-ro/strings.xml:478: <string name="native_health_ble_stop">Oprește</string>
android/app/src/main/res/values-ro/strings.xml:479: <string name="native_health_ble_scan">Caută (60 s)</string>
android/app/src/main/res/values-ro/strings.xml:480: <string name="native_health_ble_remaining">%1$d s rămase</string>
android/app/src/main/res/values-ro/strings.xml:481: <string name="native_health_ble_disconnect">Deconectează</string>
android/app/src/main/res/values-ro/strings.xml:482: <string name="native_health_ble_heart_rate">Puls în timp real (0x2A37)</string>
android/app/src/main/res/values-ro/strings.xml:483: <string name="native_health_ble_listen">Ascultă</string>
android/app/src/main/res/values-ro/strings.xml:484: <string name="native_health_ble_rd_fit">Măsurare RDFit (0xED)</string>
android/app/src/main/res/values-ro/strings.xml:485: <string name="native_health_ble_request">Solicită</string>
android/app/src/main/res/values-ro/strings.xml:486: <string name="native_health_ble_remember">Memorează ceasul</string>
android/app/src/main/res/values-ro/strings.xml:487: <string name="native_health_ble_forget">Uită</string>
android/app/src/main/res/values-ro/strings.xml:488: <string name="native_health_ble_background">Sincronizare pași în fundal (BLE)</string>
android/app/src/main/res/values-ro/strings.xml:489: <string name="native_health_ble_background_help">Actualizează pașii de pe ceasul RDFit selectat la fiecare 5 minute</string>
android/app/src/main/res/values-ro/strings.xml:490: <string name="native_health_ble_found">Dispozitive descoperite (%1$d)</string>
android/app/src/main/res/values-ro/strings.xml:491: <string name="native_health_ble_rssi">%1$s • RSSI: %2$d dBm</string>
android/app/src/main/res/values-ro/strings.xml:492: <string name="native_health_ble_saved">★ Ceas memorat</string>
android/app/src/main/res/values-ro/strings.xml:493: <string name="native_health_ble_connect">Conectează</string>
android/app/src/main/res/values-ro/strings.xml:651: <string name="native_health_ble_connected_watch">Ceas conectat</string>
android/app/src/main/res/values-ro/strings.xml:652: <string name="native_health_ble_steps_count">%1$d pași</string>
android/app/src/main/res/values-ro/strings.xml:653: <string name="native_health_ble_unnamed_device">Dispozitiv fără nume</string>
```
