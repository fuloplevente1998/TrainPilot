/* TrainPilot #60 – v1.7.7 Fejlődés screen.
 * Additive Journal tab; no duplicate data store and no changes to the global app chrome.
 * Loaded after app.js and wired after the existing DOMContentLoaded UI decorators.
 */
window.addEventListener('DOMContentLoaded',function(){
 'use strict';
 const model=window.TrainPilot177Model;
 if(!model||typeof historyScreen!=='function'||typeof shell!=='function')return;
 const COPY={
  hu:{log:'Edzésnapló',stats:'Statisztikák',progress:'Fejlődés',volume:'Összes volumen',trend:'Volumen trend',
   days7:'7 nap',days30:'30 nap',months3:'3 hó',vs:'az előző időszakhoz képest',pr:'PR rekordok',
   workouts:'Edzések száma',average:'Átlagos volumen',averageSmall:'súlyzós edzésenként',
   sets:'Sorozatok száma',muscles:'Izomcsoport-terhelés',groupNote:'Az arányok a legnagyobb naplózott sorozatterheléshez viszonyítanak, nem fáradtságot mérnek.',
   coach:'Fejlődési javaslat',more:'Részletek',noData:'Ehhez az időszakhoz nincs elegendő naplózott adat.',
   noWeight:'Ebben az időszakban nincs mérhető súlyzós volumen.',openLog:'Megnyitás az Edzésnaplóban',
   close:'Bezárás',previous:'Korábbi',current:'Új',sessions:'Edzések',completed:'Befejezett edzések',
   hint:'Válassz egy oszlopot a részletekhez.',groupDetails:'Izomcsoport részletei',
   allStats:'Megnyitás a Statisztikákban',noChange:'Nincs összehasonlítható előzmény',
   suggestionEmpty:'Rögzíts további edzéseket a személyre szabott fejlődési összesítéshez.',
   suggestionGood:'A naplózott súlyzós volumen nőtt az előző azonos hosszúságú időszakhoz képest.',
   suggestionFocus:'A legtöbb sorozatterhelést ez az izomcsoport kapta: ',
   suggestionStable:'A következetességet az edzésszám és a volumen együtt mutatja.',
   coachOpen:'Részletek a Coachban',weight:'Súly',reps:'Ismétlés',time:'Idő',left:'Bal oldal',right:'Jobb oldal'},
  en:{log:'Workout log',stats:'Statistics',progress:'Progress',volume:'Total volume',trend:'Volume trend',
   days7:'7 days',days30:'30 days',months3:'3 mo',vs:'vs previous period',pr:'PR records',
   workouts:'Workouts',average:'Average volume',averageSmall:'per weighted workout',
   sets:'Completed sets',muscles:'Muscle group load',groupNote:'Relative to the most trained muscle group, not a fatigue measurement.',
   coach:'Progress insight',more:'Details',noData:'Not enough logged data for this period.',
   noWeight:'No measurable weighted volume in this period.',openLog:'Open in workout log',
   close:'Close',previous:'Previous',current:'New',sessions:'Workouts',completed:'Completed workouts',
   hint:'Tap a bar for details.',groupDetails:'Muscle group details',
   allStats:'Open Statistics',noChange:'No comparable previous data',
   suggestionEmpty:'Log more workouts for useful progress insights.',
   suggestionGood:'Your logged weighted volume increased compared with the previous period.',
   suggestionFocus:'Most recorded working-set load was assigned to: ',
   suggestionStable:'Workout count and volume together provide context for consistency.',
   coachOpen:'View in Coach',weight:'Weight',reps:'Reps',time:'Time',left:'Left side',right:'Right side'},
  de:{log:'Trainingstagebuch',stats:'Statistiken',progress:'Fortschritt',volume:'Gesamtvolumen',trend:'Volumentrend',
   days7:'7 Tage',days30:'30 Tage',months3:'3 Mon.',vs:'zum vorigen Zeitraum',pr:'Persönliche Rekorde',workouts:'Trainingseinheiten',
   average:'Ø Volumen',averageSmall:'pro Krafttraining',sets:'Sätze',muscles:'Muskelgruppenbelastung',coach:'Fortschrittsempfehlung'},
  ro:{log:'Jurnal de antrenament',stats:'Statistici',progress:'Progres',volume:'Volum total',trend:'Tendință volum',
   days7:'7 zile',days30:'30 zile',months3:'3 luni',vs:'față de perioada anterioară',pr:'Recorduri personale',
   workouts:'Antrenamente',average:'Volum mediu',averageSmall:'per antrenament cu greutăți',sets:'Serii',muscles:'Grupe musculare',coach:'Sugestie de progres'},
  sk:{log:'Tréningový denník',stats:'Štatistiky',progress:'Pokrok',volume:'Celkový objem',trend:'Trend objemu',
   days7:'7 dní',days30:'30 dní',months3:'3 mes.',vs:'oproti predošlému obdobiu',pr:'Osobné rekordy',
   workouts:'Tréningy',average:'Priemerný objem',averageSmall:'na silový tréning',sets:'Série',muscles:'Zaťaženie svalov',coach:'Odporúčanie'},
  pl:{log:'Dziennik treningowy',stats:'Statystyki',progress:'Postępy',volume:'Całkowita objętość',trend:'Trend objętości',
   days7:'7 dni',days30:'30 dni',months3:'3 mies.',vs:'względem poprzedniego okresu',pr:'Rekordy osobiste',
   workouts:'Treningi',average:'Śr. objętość',averageSmall:'na trening siłowy',sets:'Serie',muscles:'Obciążenie mięśni',coach:'Wskazówka'}};
 const lang=()=>{try{return typeof rf212Lang==='function'?rf212Lang():'hu'}catch(_){return 'hu'}};
 const tr=k=>(COPY[lang()]||COPY.hu)[k]||COPY.hu[k]||k;
 const locale=()=>({hu:'hu-HU',en:'en-US',de:'de-DE',ro:'ro-RO',sk:'sk-SK',pl:'pl-PL'}[lang()]||'hu-HU');
 const escapeHtml=v=>String(v==null?'':v).replace(/[&<>"']/g,ch=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
 const fmt=(v,digits=0)=>Number(v||0).toLocaleString(locale(),{maximumFractionDigits:digits});
 const dateFmt=(d,short=false)=>new Intl.DateTimeFormat(locale(),short?{month:'short',day:'numeric'}:{year:'numeric',month:'short',day:'numeric'}).format(d);
 const pct=v=>v==null?'<span class="tp177-neutral">—</span>':'<span class="'+(v<0?'tp177-negative':'tp177-positive')+'">'+(v>0?'▲ +':v<0?'▼ ':'')+fmt(v)+'%</span>';
 let cachedRaw=null,cachedDate=null,cachedRows=null,cachedModels={};
 function groupMap(e){
  if(typeof rf145Groups==='function'){const found=rf145Groups(e.id);if(Array.isArray(found)&&found.length)return found}
  return [e.muscleGroup,e.target];
 }
 function snapshot(){
  const raw=localStorage.getItem('repforge:history')||'[]',today=model.dayKey(new Date());
  if(raw!==cachedRaw||today!==cachedDate||!cachedRows){
   cachedRaw=raw;cachedDate=today;cachedModels={};
   cachedRows=model.prepare(history(),{groupMap,isDeleted:x=>typeof window.tp3HistoryIsDeleted==='function'&&window.tp3HistoryIsDeleted(x)});
  }
  const period=['7d','30d','3m'].includes(state.tp177Period)?state.tp177Period:'3m';
  return cachedModels[period]||(cachedModels[period]=model.build(cachedRows,period,new Date()));
 }
 function tabs(active){
  const entries=[['log',tr('log'),"go('history')"],['stats',tr('stats'),"tp155OpenJournalStats()"],
   ['progress',tr('progress'),"tp177OpenProgress()"]];
  return '<nav class="tp155-journal-tabs tp177-journal-tabs" role="tablist" aria-label="'+escapeHtml(tr('log'))+'">'+
   entries.map(x=>'<button type="button" role="tab" aria-selected="'+(active===x[0]?'true':'false')+'" class="'+(active===x[0]?'active':'')+'" onclick="'+x[2]+'">'+escapeHtml(x[1])+'</button>').join('')+'</nav>';
 }
 function replaceTabs(html,active){
  const replacement=tabs(active);
  return /<nav class="tp155-journal-tabs"[\s\S]*?<\/nav>/.test(html)
   ?String(html).replace(/<nav class="tp155-journal-tabs"[\s\S]*?<\/nav>/,replacement)
   :String(html).replace(/(<main[^>]*>)/,function(m){return m+replacement});
 }
 function selectPeriod(p){
  state.tp177Period=p;
  const buttons=[...document.querySelectorAll('.tp177-periods button')];
  if(buttons.length===3){buttons.forEach(b=>{const active=b.dataset.period===p;b.classList.toggle('active',active);b.setAttribute('aria-pressed',String(active))})}
  render();
 }
 window.tp177SetPeriod=selectPeriod;
 function metricCard(icon,label,value,delta,action,subtitle){
  return '<button type="button" class="tp177-metric" onclick="'+action+'"><span class="tp177-icon" aria-hidden="true">'+icon+'</span>'+
   '<span class="tp177-metric-body"><span class="tp177-metric-label">'+escapeHtml(label)+'</span>'+
   '<strong>'+value+'</strong><span class="tp177-delta">'+pct(delta)+'</span>'+
   (subtitle?'<span class="tp177-metric-caption">'+escapeHtml(subtitle)+'</span>':'')+'</span>'+
   '<span class="tp177-chevron" aria-hidden="true">›</span></button>';
 }
 function barChart(v){
  const max=Math.max(0,...v.buckets.map(b=>b.volume)),unit=max>0?Math.ceil(max/4/1000)*1000:1000;
  const ticks=[4,3,2,1,0].map(x=>'<span>'+fmt(unit*x)+'</span>').join('');
  const bars=v.buckets.map((b,i)=>{
   const height=max>0?Math.max(0,Math.round(b.volume/max*100)):0;
   return '<button type="button" class="tp177-chart-column" aria-label="'+escapeHtml(dateFmt(b.start)+'; '+fmt(b.volume)+' kg; '+b.workouts.length+' '+tr('sessions'))+
    '" onclick="tp177OpenBucket('+i+')"><span class="tp177-bar-rail"><span class="tp177-bar" style="height:'+height+'%"></span></span>'+
    '<span class="tp177-chart-date">'+escapeHtml(dateFmt(b.start,true))+'</span></button>';
  }).join('');
  return '<section class="tp177-panel tp177-trend" aria-label="'+escapeHtml(tr('trend'))+'"><h2>'+escapeHtml(tr('trend'))+'</h2>'+
   '<div class="tp177-chart-scroll"><div class="tp177-chart"><div class="tp177-chart-axis">'+ticks+'</div>'+
   '<div class="tp177-chart-columns" style="grid-template-columns:repeat('+v.buckets.length+',minmax(24px,1fr))">'+bars+'</div></div></div>'+
   '<p class="tp177-small">'+escapeHtml(tr('hint'))+'</p></section>';
 }
 // Original, self-contained front/back schematic. Load shares remain in the adjacent bars.
 function anatomy(){
  const figure=(x,back)=>{
   const color=back?'#cf795b':'#da8963';
   return '<g transform="translate('+x+',0)"><circle cx="44" cy="14" r="11" fill="#a2aab7"/>'+
    '<path d="M30 29 L58 29 66 48 60 97 52 113 50 158 43 180 36 157 33 113 25 97 21 48Z" fill="#687583" stroke="#aab4c3" stroke-width="1.3"/>'+
    '<path d="M27 38 C14 39 12 49 9 67 L4 95 13 98 21 72 30 52M61 38 C75 39 77 49 79 67 L84 95 75 98 67 72 58 52" fill="#87909e" stroke="#acb6c2" stroke-width="1"/>'+
    '<path d="M30 108 L33 147 36 176 42 178 44 127 46 178 52 176 56 145 59 108" fill="#8d96a3" stroke="#b4bdc9" stroke-width="1"/>'+
    '<ellipse cx="24" cy="43" rx="7" ry="12" fill="'+color+'" opacity=".93"/>'+
    '<ellipse cx="64" cy="43" rx="7" ry="12" fill="'+color+'" opacity=".93"/>'+
    (back?'<path d="M30 42 Q44 36 58 42 L57 78 Q44 94 31 78Z" fill="#e59c77"/><path d="M32 82L56 82 54 103 34 103Z" fill="#ba6f50"/>':
     '<path d="M29 41 Q43 36 43 50 L43 62 28 62Z M45 50Q46 36 60 41L61 62 45 62Z" fill="#f1a77c"/>'+
     '<path d="M34 65 L54 65 51 99 37 99Z" fill="#afbbc5" stroke="#c9d0d8" stroke-width=".7"/>')+
    '<path d="M29 111 L39 113 41 144 34 153 31 147Z M49 113L59 111 57 147 53 153 47 144Z" fill="'+color+'" opacity=".85"/></g>';
  };
  return '<svg class="tp177-anatomy-svg" viewBox="0 0 190 190" role="img" aria-label="Front and back muscle illustration">'+
   '<defs><linearGradient id="tp177-musc-grad" x1="0" x2="1"><stop stop-color="#c18c71"/><stop offset="1" stop-color="#f7ad78"/></linearGradient></defs>'+
   figure(0,false)+figure(98,true)+'</svg>';
 }
 function groupPanel(v){
  const rows=v.groups.map(g=>'<button type="button" class="tp177-muscle-row" onclick="tp177OpenGroup(\''+g.key+'\')" '+(g.sets?'':'disabled')+'>'+
   '<span class="tp177-muscle-label">'+escapeHtml(g.name)+'</span><span class="tp177-muscle-track"><span style="width:'+g.percent+'%"></span></span>'+
   '<span class="tp177-muscle-percent">'+(g.sets?fmt(g.percent)+'%':'—')+'</span></button>').join('');
  return '<section class="tp177-panel tp177-muscles"><h2>'+escapeHtml(tr('muscles'))+'</h2><div class="tp177-muscle-layout">'+
   '<div class="tp177-anatomy">'+anatomy()+'</div><div class="tp177-muscle-bars">'+rows+'</div></div>'+
   '<p class="tp177-small">'+escapeHtml(tr('groupNote'))+'</p></section>';
 }
 function suggestion(v){
  if(!v.currentMetrics.workouts)return tr('suggestionEmpty');
  if(v.changes.volume!==null&&v.changes.volume>0&&v.currentMetrics.volume>0)return tr('suggestionGood');
  const top=v.groups.filter(g=>g.sets>0).sort((a,b)=>b.sets-a.sets)[0];
  if(top)return tr('suggestionFocus')+top.name+'.';
  return tr('suggestionStable');
 }
 function progressHtml(){
  const v=snapshot(),m=v.currentMetrics,c=v.changes,period=v.period;
  const from=dateFmt(v.start),through=dateFmt(model.addDays(v.end,-1));
  const periodButtons=[['7d','days7'],['30d','days30'],['3m','months3']].map(a=>
   '<button type="button" data-period="'+a[0]+'" class="'+(period===a[0]?'active':'')+'" aria-pressed="'+(period===a[0]?'true':'false')+'" onclick="tp177SetPeriod(\''+a[0]+'\')">'+escapeHtml(tr(a[1]))+'</button>').join('');
  const avg=m.avgVolume!=null?fmt(m.avgVolume,1)+' kg':'—';
  return '<main class="tp177-progress">'+tabs('progress')+
   '<section class="tp177-panel tp177-summary"><div class="tp177-summary-head"><div><span class="tp177-small">'+escapeHtml(tr('volume'))+
   '</span><strong class="tp177-big">'+fmt(m.volume)+' kg</strong><span class="tp177-small">'+escapeHtml(from)+' – '+escapeHtml(through)+'</span></div>'+
   '<div class="tp177-hero-change">'+pct(c.volume)+'<span class="tp177-small">'+escapeHtml(tr('vs'))+'</span></div></div>'+
   '<div class="tp177-periods" role="group" aria-label="'+escapeHtml(tr('progress'))+'">'+periodButtons+'</div></section>'+
   barChart(v)+'<div class="tp177-metric-grid">'+
   metricCard('♜',tr('pr'),fmt(m.pr.length),c.pr,'tp177OpenRecords()','')+
   metricCard('◫',tr('workouts'),fmt(m.workouts),c.workouts,"tp177OpenStat('workouts')",'')+
   metricCard('↗',tr('average'),avg,c.avgVolume,'tp177OpenAverage()',tr('averageSmall'))+
   metricCard('▤',tr('sets'),fmt(m.sets),c.sets,"tp177OpenStat('sets')",'')+'</div>'+
   groupPanel(v)+'<button type="button" class="tp177-panel tp177-coach" onclick="tp177OpenCoach()">'+
   '<span class="tp177-icon" aria-hidden="true">✧</span><span class="tp177-coach-body"><strong>'+escapeHtml(tr('coach'))+'</strong>'+
   '<span>'+escapeHtml(suggestion(v))+'</span></span><span class="tp177-chevron" aria-hidden="true">›</span></button>'+
   '</main>';
 }
 function dialog(title,body){
  document.getElementById('tp177Dialog')?.remove();
  const host=document.createElement('div');host.id='tp177Dialog';host.className='tp177-dialog';
  host.innerHTML='<div class="tp177-dialog-backdrop" onclick="tp177CloseDialog()"></div>'+
   '<section class="tp177-dialog-card" role="dialog" aria-modal="true" aria-labelledby="tp177DialogTitle" tabindex="-1">'+
   '<header><h2 id="tp177DialogTitle">'+escapeHtml(title)+'</h2><button type="button" class="tp177-dialog-close" onclick="tp177CloseDialog()" aria-label="'+escapeHtml(tr('close'))+'">×</button></header>'+
   '<div class="tp177-dialog-content">'+body+'</div></section>';
  document.body.appendChild(host);host.querySelector('[role="dialog"]').focus();
 }
 window.tp177CloseDialog=function(){document.getElementById('tp177Dialog')?.remove()};
 function workoutRows(xs){
  if(!xs.length)return '<p class="tp177-small">'+escapeHtml(tr('noData'))+'</p>';
  return '<div class="tp177-detail-list">'+xs.slice().sort((a,b)=>b.dateMs-a.dateMs).map(w=>
   '<button class="tp177-detail-workout" type="button" data-tp177-key="'+escapeHtml(w.key)+'" onclick="tp177OpenWorkout(this.dataset.tp177Key)">'+
   '<strong>'+escapeHtml(dateFmt(w.date))+'</strong><span>'+fmt(w.volume)+' kg · '+fmt(w.sets)+' '+escapeHtml(tr('sets'))+'</span>'+
   '<span class="tp177-chevron">›</span></button>').join('')+'</div>';
 }
 window.tp177OpenBucket=function(index){
  const b=snapshot().buckets[index];if(!b)return;
  dialog(dateFmt(b.start)+' – '+dateFmt(model.addDays(b.end,-1)),
   '<p class="tp177-detail-totals"><strong>'+fmt(b.volume)+' kg</strong> · '+fmt(b.workouts.length)+' '+escapeHtml(tr('sessions'))+'</p>'+workoutRows(b.workouts));
 };
 window.tp177OpenRecords=function(){
  const xs=snapshot().currentMetrics.pr;
  const names={weight:tr('weight'),reps:tr('reps'),time:tr('time'),left:tr('left'),right:tr('right')};
  const rows=xs.length?'<div class="tp177-detail-list">'+xs.slice().sort((a,b)=>b.date-a.date).map(p=>
   '<button type="button" class="tp177-detail-record" data-tp177-key="'+escapeHtml(p.workoutKey)+'" onclick="tp177OpenWorkout(this.dataset.tp177Key)">'+
   '<strong>'+escapeHtml(typeof rf220ExerciseName==='function'?rf220ExerciseName(p.id):p.id)+'</strong>'+
   '<span>'+escapeHtml(names[p.kind]||p.kind)+' · '+escapeHtml(dateFmt(p.date))+'</span>'+
   '<span>'+escapeHtml(tr('previous'))+': '+fmt(p.previous,1)+' '+p.unit+' → '+escapeHtml(tr('current'))+': '+fmt(p.value,1)+' '+p.unit+'</span></button>').join('')+'</div>':
   '<p>'+escapeHtml(tr('noData'))+'</p>';
  dialog(tr('pr'),rows);
 };
 window.tp177OpenAverage=function(){
  const v=snapshot(),xs=v.current.filter(w=>w.weighted);
  dialog(tr('average'),
   '<p class="tp177-detail-totals"><strong>'+(v.currentMetrics.avgVolume==null?'—':fmt(v.currentMetrics.avgVolume,1)+' kg')+'</strong> · '+fmt(xs.length)+' '+escapeHtml(tr('sessions'))+'</p>'+
   (xs.length?workoutRows(xs):'<p>'+escapeHtml(tr('noWeight'))+'</p>'));
 };
 window.tp177OpenGroup=function(key){
  const v=snapshot(),g=v.groups.find(g=>g.key===key);if(!g||!g.sets)return;
  const xs=v.current.filter(w=>(w.groups[key]||0)>0);
  dialog(tr('groupDetails')+' – '+g.name,
   '<p class="tp177-detail-totals"><strong>'+fmt(g.sets,1)+'</strong> '+escapeHtml(tr('sets'))+' · '+g.percent+'%</p>'+
   workoutRows(xs));
 };
 window.tp177OpenWorkout=function(key){
  window.tp177CloseDialog();
  if(typeof rf263HistoryFilter!=='undefined')rf263HistoryFilter={from:'',to:''};
  state.tp177JournalView='log';
  go('history');
  const rows=[...document.querySelectorAll('details[data-tp152-history-key]')];
  const target=rows.find(el=>el.dataset.tp152HistoryKey===key);
  if(target){target.open=true;target.scrollIntoView?.({block:'center'})}
 };
 window.tp177OpenCoach=function(){
  if(typeof window.tp155R4TogglePanel==='function'){
   const button=document.querySelector('.top .tp154-coach-action');
   return window.tp155R4TogglePanel('coach',button||document.activeElement);
  }
  if(typeof go==='function')go('coach');
 };
 let statsFocus=null;
 function decorateStats(){
  const main=document.querySelector('#app main');
  if(!main)return;
  const nav=main.querySelector('nav.tp155-journal-tabs');
  if(nav)nav.outerHTML=tabs('stats');
  else main.insertAdjacentHTML('afterbegin',tabs('stats'));
  if(statsFocus){
   const v=snapshot(),metric=statsFocus==='sets'?v.currentMetrics.sets:v.currentMetrics.workouts;
   const title=statsFocus==='sets'?tr('sets'):tr('workouts');
   main.querySelector('#tp177StatsFocus')?.remove();
   const panel='<section id="tp177StatsFocus" class="tp177-panel tp177-stats-focus"><span class="tp177-small">'+escapeHtml(title)+
    ' · '+escapeHtml(dateFmt(v.start))+' – '+escapeHtml(dateFmt(model.addDays(v.end,-1)))+'</span>'+
    '<strong>'+fmt(metric)+'</strong></section>';
   main.querySelector('nav.tp155-journal-tabs')?.insertAdjacentHTML('afterend',panel);
   statsFocus=null;
  }
 }
 const journalBase=historyScreen;
 historyScreen=function(){
  if(state.tp177JournalView==='progress')return shell(progressHtml());
  return replaceTabs(journalBase.apply(this,arguments),'log');
 };
 const statsBase=window.tp155OpenJournalStats;
 if(typeof statsBase==='function'){
  window.tp155OpenJournalStats=function(){
   state.tp177JournalView='stats';
   const out=statsBase.apply(this,arguments);decorateStats();
   return out;
  };
  window.tp2627OpenStats=window.tp155OpenJournalStats;
 }
 window.tp177OpenStat=function(type){
  statsFocus=type;
  if(typeof window.tp155OpenJournalStats==='function')window.tp155OpenJournalStats();
 };
 let openingProgress=false;
 const goBase=go;
 go=function(route){
  if(String(route)==='history'&&!openingProgress)state.tp177JournalView='log';
  return goBase.apply(this,arguments);
 };
 window.TrainPilotNavigate=go;
 window.tp177OpenProgress=function(){
  openingProgress=true;state.tp177JournalView='progress';
  try{return go('history')}finally{openingProgress=false}
 };
 document.addEventListener('keydown',function(e){if(e.key==='Escape'&&document.getElementById('tp177Dialog'))window.tp177CloseDialog()});
 window.TrainPilot177Progress={version:'1.7.7-issue60',snapshot,prepare:model.prepare,build:model.build};
});
