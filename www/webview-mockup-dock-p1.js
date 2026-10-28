/* TrainPilot PR #271, P1b: opt-in five-tab bottom dock for WebView mockup review.
 * Uses only canonical TrainPilot navigation and does not change workout engines,
 * data storage, Wear state, Health data or any timer. No global go/render wrapper. */
(function () {
  'use strict';
  const enabled = new URLSearchParams(location.search).get('tp271') === '1';
  if (!enabled) return;

  const labels = {
    hu: {home:'Kezdőlap',workout:'Edzés',log:'Napló',progress:'Fejlődés',health:'Egészség',nav:'Fő navigáció'},
    en: {home:'Home',workout:'Workout',log:'Journal',progress:'Progress',health:'Health',nav:'Main navigation'},
    de: {home:'Start',workout:'Training',log:'Tagebuch',progress:'Fortschritt',health:'Gesundheit',nav:'Hauptnavigation'},
    ro: {home:'Acasă',workout:'Antrenament',log:'Jurnal',progress:'Progres',health:'Sănătate',nav:'Navigare principală'}
  };
  const icons = {
    home:'<path d="m3 10 9-7 9 7v10H3Z"/><path d="M9 20v-7h6v7"/>',
    workout:'<path d="M6 8v8M3 10v4M18 8v8M21 10v4M6 12h12"/>',
    log:'<rect x="5" y="3" width="14" height="18" rx="2"/><path d="M8 8h8M8 12h8M8 16h5"/>',
    progress:'<path d="m3 17 6-6 4 3 8-9"/><path d="M16 5h5v5"/>',
    health:'<path d="M20.8 4.8a5.5 5.5 0 0 0-7.8 0L12 5.9l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.4a5.5 5.5 0 0 0 0-7.8Z"/>'
  };
  const keys=['home','workout','log','progress','health'];
  const getLang=()=>{
    try {
      const s=typeof window.rf212Lang==='function'?window.rf212Lang():document.documentElement.lang;
      return Object.prototype.hasOwnProperty.call(labels,s)?s:'hu';
    } catch (_) { return 'hu'; }
  };
  let dock=null,queued=false;

  function selectedKey(){
    if (typeof state==='undefined' || !state) return 'home';
    const tab=state.tab;
    if (tab==='health') return 'health';
    if (tab==='history') return state.tp177JournalView==='progress'?'progress':'log';
    if (tab==='plan'||tab==='workout') return 'workout';
    if (tab==='stats') return 'log';
    return 'home';
  }
  function activeWorkout(){
    try { return typeof state!=='undefined' && !!state.session; } catch (_) { return false; }
  }
  function sync(){
    queued=false;
    if (!dock || !dock.isConnected) return;
    const language=getLang(),t=labels[language],active=selectedKey();
    dock.setAttribute('aria-label',t.nav);
    for(const button of dock.querySelectorAll('button[data-tp271-target]')){
      const key=button.dataset.tp271Target,on=key===active;
      button.querySelector('.tp271-dock-label').textContent=t[key];
      button.setAttribute('aria-label',t[key]);
      button.setAttribute('aria-current',on?'page':'false');
      button.classList.toggle('tp271-selected',on);
    }
    document.documentElement.classList.toggle('tp271-has-workout',activeWorkout());
  }
  function scheduleSync(){
    if (queued) return;
    queued=true;requestAnimationFrame(sync);
  }
  function navigate(key){
    if (activeWorkout()) return; // dock is hidden; never override a live session
    if(key==='progress'){
      if(typeof window.tp177OpenProgress==='function')window.tp177OpenProgress();
      else if(typeof window.go==='function')window.go('progress');
    } else if(typeof window.go==='function'){
      window.go({workout:'plan',log:'history'}[key]||key);
    }
    scheduleSync();
  }
  function install(){
    if(dock) return;
    dock=document.createElement('nav');
    dock.id='tp271Dock';
    dock.className='tp271-dock';
    dock.setAttribute('aria-label',labels[getLang()].nav);
    // A single five-tab presentation bar; the old navigation nodes stay in
    // their original DOM so their canonical commands remain reachable in the
    // upper quick-action row and existing panel/selection handlers still work.
    for(const key of keys){
      const button=document.createElement('button');
      button.type='button';button.className='tp271-dock-button';
      button.dataset.tp271Target=key;
      const svg='<svg class="tp271-dock-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'+icons[key]+'</svg>';
      button.innerHTML=svg+'<span class="tp271-dock-label"></span>';
      button.addEventListener('click',()=>navigate(key));
      dock.appendChild(button);
    }
    document.body.appendChild(dock);
    document.documentElement.classList.add('tp271-mockup-dock');
    const app=document.getElementById('app');
    if(app&&typeof MutationObserver==='function'){
      // Observe only direct #app children. Each render swaps this root; no
      // full-subtree DOM scans, persistent history reads or event rebindings.
      const observer=new MutationObserver(scheduleSync);
      observer.observe(app,{childList:true});
    }
    if(typeof MutationObserver==='function'){
      new MutationObserver(scheduleSync).observe(document.documentElement,{attributes:true,attributeFilter:['lang']});
    }
    sync();
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install,{once:true});
  else install();

  window.TrainPilotMockup271={version:'P1b-preview',enabled:true,refresh:scheduleSync};
})();
