/* TrainPilot global UI/UX: one decorated Hero per view and same-overlay
   toggle returns keyboard focus to the underlying unchanged main route. */
(function(){
 'use strict';
 const decorate=function(){
  const app=document.getElementById('app');
  const panel=document.getElementById('tp155R4PanelHost');
  for(const root of [app,panel])root?.querySelectorAll('.tp-ui-hero').forEach(el=>el.classList.remove('tp-ui-hero'));
  const main=app?.querySelector('main');
  if(main?.matches('.rf221-home')) {
    main.querySelector(':scope > .hero.tp155-home-active-card')?.classList.add('tp-ui-hero');
  } else if(main?.matches('.tp152-plan')) {
    (main.querySelector('.tp152-active-program')||main.querySelector('.tp150-quick-entry'))?.classList.add('tp-ui-hero');
  } else if(main?.matches('.tp153-workout')) {
    main.querySelector('.tp153-workout-head')?.classList.add('tp-ui-hero');
  } else if(main?.matches('.tp168-health,.rf263-health')) {
    (main.querySelector(':scope > .tp168-today-card')||main.querySelector(':scope > .tp151-health-card'))?.classList.add('tp-ui-hero');
  }
  if(panel?.dataset.panel==='coach') {
    panel.querySelector('.tp151-coach-recommendation')?.classList.add('tp-ui-hero');
  }
 };
 const install=function(){
  if(window.TrainPilotGlobalUi177)return;
  if(typeof render==='function'){
    const old=render;
    render=function(){const value=old.apply(this,arguments);decorate();return value;};
  }
  if(typeof renderWorkout==='function'){
    const old=renderWorkout;
    renderWorkout=function(){const value=old.apply(this,arguments);decorate();return value;};
  }
  for(const name of ['tp155R4OpenPanel','tp155R4RefreshPanel','tp155R4ClosePanel']){
    if(typeof window[name]!=='function')continue;
    const old=window[name];
    window[name]=function(){const value=old.apply(this,arguments);decorate();return value;};
  }
  if(typeof window.tp155R4TogglePanel==='function'){
    const old=window.tp155R4TogglePanel;
    window.tp155R4TogglePanel=function(type){
      const same=document.getElementById('tp155R4PanelHost')?.dataset.panel===String(type);
      const priorRoute=typeof state==='object'?state.tab:null;
      const result=old.apply(this,arguments);
      if(same&&!document.getElementById('tp155R4PanelHost')){
        const main=document.querySelector('#app main');
        if(main){
          main.setAttribute('tabindex','-1');
          try{main.focus({preventScroll:true})}catch(_){main.focus()}
        }
        if(typeof state==='object'&&state.tab!==priorRoute)console.warn('Overlay toggled unexpectedly changed its parent route.');
      }
      decorate();
      return result;
    };
  }
  window.TrainPilotGlobalUi177={version:'health-reference-preview',decorate,focusOnReToggle:true,oneHeroPerView:true};
  decorate();
 };
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install,{once:true});
 else install();
})();
