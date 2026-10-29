/* PR #277 / Settings: presentation grouping only.
   This wraps settingsScreen, not render/go/panel routing. All existing controls,
   IDs, handlers, cloud credentials and confirmation dialogs stay intact. */
(function(){
 'use strict';
 if(typeof settingsScreen!=='function'||!document?.createElement)return;
 const language=()=>typeof rf212Lang==='function'?rf212Lang():'hu';
 const copy={
  hu:{appearance:'Megjelenés és nyelv',training:'Edzésbeállítások',data:'Mentés, adatok és szinkron',profile:'Profil és alaphelyzet',wearTitle:'Wear OS',wearText:'Az órás edzésátadás a meglévő szinkronon keresztül működik. A párosítás vagy az akkuszint állapota itt nem ellenőrizhető.'},
  en:{appearance:'Appearance and language',training:'Workout settings',data:'Backup, data and sync',profile:'Profile and reset',wearTitle:'Wear OS',wearText:'Watch workouts use the existing synchronization. Pairing and battery status cannot be verified here.'},
  de:{appearance:'Darstellung und Sprache',training:'Trainingseinstellungen',data:'Sicherung, Daten und Sync',profile:'Profil und Zurücksetzen',wearTitle:'Wear OS',wearText:'Trainingsdaten werden über die vorhandene Synchronisierung an die Uhr übertragen. Kopplung und Akkustand können hier nicht geprüft werden.'},
  ro:{appearance:'Aspect și limbă',training:'Setări de antrenament',data:'Backup, date și sincronizare',profile:'Profil și resetare',wearTitle:'Wear OS',wearText:'Antrenamentele folosesc sincronizarea existentă cu ceasul. Starea asocierii și a bateriei nu pot fi verificate aici.'}
 };
 const previous=settingsScreen;
 settingsScreen=function(){
  const original=previous.apply(this,arguments);
  if(typeof original!=='string')return original;
  const template=document.createElement('template');template.innerHTML=original;
  const main=template.content.querySelector('main.tp1511-settings');
  if(!main||main.classList.contains('tp277-settings'))return original;
  const t=copy[language()]||copy.en;
  const children=[...main.children];
  const find=predicate=>children.find(predicate);
  const appearance=find(n=>n.classList.contains('rf212-language'));
  const training=find(n=>n.classList.contains('tp197-mode-panel'));
  const backup=find(n=>n.classList.contains('tp152-settings-backup'));
  const profile=find(n=>n.classList.contains('tp152-settings-profile'));
  // Keep the original data, Cloud and Health consent controls in place.
  const heading=(before,title)=>{
   if(!before)return;
   const h=document.createElement('div');
   h.className='tp277-settings-heading';
   h.setAttribute('role','heading');h.setAttribute('aria-level','2');
   h.textContent=title;
   main.insertBefore(h,before);
  };
  main.classList.add('tp277-settings');
  heading(appearance,t.appearance);
  heading(training,t.training);
  heading(backup,t.data);
  heading(profile,t.profile);
  // The native watch bridge has no verified pairing/battery status API.
  // An informational row must never imply that a watch is connected.
  const cloud=find(n=>!!n.querySelector?.('#cloudStatus'));
  if(cloud&&typeof isNative==='function'&&isNative()&&typeof window.TrainPilotWearSync?.syncNow==='function'){
   const note=document.createElement('div');note.className='tp277-wear-note';
   const label=document.createElement('strong');label.textContent=t.wearTitle;
   const text=document.createElement('p');text.className='small muted';text.textContent=t.wearText;
   note.append(label,text);main.insertBefore(note,cloud);
  }
  return template.innerHTML;
 };
})();
