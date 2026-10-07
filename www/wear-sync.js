(function(){
  'use strict';

  let lastPayload = null;
  let cleared = false;
  let busy = false;

  function plugin(){
    try{
      if(!window.Capacitor?.isNativePlatform?.()) return null;
      return window.Capacitor?.Plugins?.WearSync || null;
    }catch(_){
      return null;
    }
  }

  function language(){
    try{
      if(typeof db !== 'undefined' && db?.get) return String(db.get('language','hu') || 'hu');
    }catch(_){}
    return 'hu';
  }

  function exerciseName(exercise, lang){
    if(!exercise) return '';
    return String(exercise[lang] || exercise.hu || exercise.en || exercise.name || exercise.id || '');
  }

  function makeSnapshot(){
    try{
      if(typeof state === 'undefined' || !state?.session) return null;
      const session = state.session;
      const lang = language();
      const exercises = Array.isArray(session.exercises) ? session.exercises : [];
      const current = Number.isInteger(state.current) ? state.current : 0;
      let restSeconds = 90;
      try{
        if(typeof settings === 'function'){
          const configured = Number(settings()?.rest);
          if(Number.isFinite(configured) && configured > 0) restSeconds = configured;
        }
      }catch(_){}

      return {
        schema: 1,
        workoutId: String(session.workout || session.dayId || ''),
        programId: String(session.programId || ''),
        programName: String(session.programName || ''),
        started: String(session.started || ''),
        currentExercise: Math.max(0, Math.min(current, Math.max(0, exercises.length - 1))),
        restSeconds,
        language: lang,
        exercises: exercises.map(exercise => ({
          id: String(exercise?.id || ''),
          name: exerciseName(exercise, lang),
          loadType: String(exercise?.loadType || ''),
          sets: (Array.isArray(exercise?.sets) ? exercise.sets : []).map((set, index) => ({
            set: Number(set?.set || index + 1),
            reps: String(set?.reps ?? ''),
            weight: set?.weight ?? '',
            done: !!set?.done,
            leftSeconds: set?.leftSeconds ?? '',
            rightSeconds: set?.rightSeconds ?? ''
          }))
        }))
      };
    }catch(_){
      return null;
    }
  }

  async function syncNow(){
    if(busy) return;
    const api = plugin();
    if(!api) return;
    const snapshot = makeSnapshot();
    const payload = snapshot ? JSON.stringify(snapshot) : null;
    if(payload === lastPayload && (snapshot || cleared)) return;

    busy = true;
    try{
      if(snapshot){
        await api.publish({snapshot});
        lastPayload = payload;
        cleared = false;
      }else if(!cleared){
        await api.clear();
        lastPayload = null;
        cleared = true;
      }
    }catch(_){
      // Wear is optional. Phone workout behavior must never depend on sync success.
    }finally{
      busy = false;
    }
  }

  window.TrainPilotWearSync = {syncNow, makeSnapshot};

  window.addEventListener('focus', syncNow);
  document.addEventListener('visibilitychange', () => {
    if(!document.hidden) syncNow();
  });
  setInterval(syncNow, 1500);
  setTimeout(syncNow, 0);
})();
