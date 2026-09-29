
// @section issue79-health-active-intervals.js
/* #79 — Health Connect: interrupted/resumed workouts use active intervals only. */
(function(){
'use strict';
const MAX_WINDOW_MS=24*60*60*1000, TOLERANCE_MS=3*60*1000;
function isoMs(v){const n=Date.parse(v||'');return Number.isFinite(n)?n:null}
function intervals(h){
 const raw=Array.isArray(h&&h.activeIntervals)?h.activeIntervals:[];
 const finished=isoMs(h&&h.finished), out=[];
 for(const x of raw){
  const a=isoMs(x&&x.start),b=isoMs(x&&x.end);
  if(a==null||b==null||b<=a||b-a>MAX_WINDOW_MS)continue;
  const start=Math.max(a,isoMs(h&&h.started)??a),end=Math.min(b,finished??b);
  if(end>start)out.push({start,end});
 }
 out.sort((a,b)=>a.start-b.start);
 const merged=[];
 for(const x of out){
  const last=merged.at(-1);
  if(last&&x.start<=last.end)last.end=Math.max(last.end,x.end);
  else merged.push({...x});
 }
 return merged.map(x=>({start:new Date(x.start).toISOString(),end:new Date(x.end).toISOString(),ms:x.end-x.start}));
}
function hasMetric(x){return typeof rf242HasTrainingMetric==='function'?rf242HasTrainingMetric(x):!!x}
async function readOne(p,w){
 const exact=await p.readTrainingWindow({start:w.start,end:w.end});
 if(hasMetric(exact))return {...exact,sourceWindowStart:w.start,sourceWindowEnd:w.end,matchMode:'exact'};
 const start=new Date(Date.parse(w.start)-TOLERANCE_MS).toISOString(),end=new Date(Date.parse(w.end)+TOLERANCE_MS).toISOString();
 const padded=await p.readTrainingWindow({start,end});
 const session=typeof rf242BestSession==='function'?rf242BestSession(padded,{started:w.start,finished:w.end}):null;
 if(session){
  try{const matched=await p.readTrainingWindow({start:session.start,end:session.end});if(hasMetric(matched))return {...matched,sourceWindowStart:session.start,sourceWindowEnd:session.end,matchMode:'exercise-session'}}catch(_){}
 }
 return {...padded,sourceWindowStart:start,sourceWindowEnd:end,matchMode:hasMetric(padded)?'tolerance':'empty'};
}
function num(v){const n=Number(v);return Number.isFinite(n)?n:null}
function aggregate(parts,windows,errors){
 const sum=k=>parts.reduce((s,p)=>s+(num(p[k])??0),0);
 const present=k=>parts.some(p=>num(p[k])!=null);
 const weighted=k=>{let n=0,d=0;parts.forEach((p,i)=>{const v=num(p[k]);if(v!=null){const w=windows[i]?.ms||1;n+=v*w;d+=w}});return d?n/d:null};
 const max=k=>{const a=parts.map(p=>num(p[k])).filter(v=>v!=null);return a.length?Math.max(...a):null};
 const sources=[...new Set(parts.flatMap(p=>Array.isArray(p.sources)?p.sources:[]))];
 const labels=Object.assign({},...parts.map(p=>p.sourceLabels||{}));
 const warnings=[...parts.flatMap(p=>Array.isArray(p.warnings)?p.warnings:[]),...errors.map(e=>e.message||String(e))];
 return {
  averageHeartRate:weighted('averageHeartRate'),maxHeartRate:max('maxHeartRate'),
  activeCalories:present('activeCalories')?sum('activeCalories'):null,totalCalories:present('totalCalories')?sum('totalCalories'):null,
  distanceMeters:present('distanceMeters')?sum('distanceMeters'):null,averageSpeedMps:weighted('averageSpeedMps'),
  exerciseSessionCount:sum('exerciseSessionCount'),exerciseSessions:parts.flatMap(p=>Array.isArray(p.exerciseSessions)?p.exerciseSessions:[]),
  sources,sourceLabels:labels,warnings,permissions:Object.assign({},...parts.map(p=>p.permissions||{})),
  matchMode:'active-intervals',activeIntervals:windows.map(({start,end})=>({start,end})),
  activeDurationMs:windows.reduce((s,w)=>s+w.ms,0),sourceWindowStart:windows[0]?.start||null,sourceWindowEnd:windows.at(-1)?.end||null
 };
}
const validBase=rf240ValidWorkout;
rf240ValidWorkout=function(h){
 const a=isoMs(h&&h.started),b=isoMs(h&&h.finished);if(a==null||b==null||b<=a)return false;
 const ws=intervals(h);return ws.length?ws.every(w=>w.ms>0&&w.ms<=MAX_WINDOW_MS):validBase(h);
};
rf240SyncWorkout=async function(p,h,force=false){
 if(!rf240ValidWorkout(h))return false;
 const key=rf240WorkoutKey(h),old=h.health240,ws=intervals(h);
 if(!ws.length){
  const exact=await readOne(p,{start:h.started,end:h.finished,ms:Date.parse(h.finished)-Date.parse(h.started)});
  return rf242SaveMatchedWorkoutHealth(key,h,exact,{matchMode:exact.matchMode,sourceWindowStart:exact.sourceWindowStart,sourceWindowEnd:exact.sourceWindowEnd});
 }
 const signature=JSON.stringify(ws.map(w=>[w.start,w.end]));
 if(!force&&old?.activeIntervalSignature===signature&&Date.now()-Date.parse(old.syncedAt||0)<6*3600000&&hasMetric(old))return false;
 const parts=[],okWindows=[],errors=[];
 for(const w of ws){try{parts.push(await readOne(p,w));okWindows.push(w)}catch(e){errors.push(e)}}
 if(!parts.length)throw errors[0]||new Error('Health Connect: nincs olvasható aktív edzésszakasz.');
 const data=aggregate(parts,okWindows,errors);
 return rf242SaveMatchedWorkoutHealth(key,h,data,{matchMode:'active-intervals',activeIntervalSignature:signature,sourceWindowStart:data.sourceWindowStart,sourceWindowEnd:data.sourceWindowEnd});
};
window.TrainPilotIssue79={version:'79.1',intervals};
})();
// @endsection issue79-health-active-intervals.js
