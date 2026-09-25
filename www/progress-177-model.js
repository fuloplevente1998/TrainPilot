/* TrainPilot #60 – v1.7.7 Progress data model.
 * Standalone, deterministic and independently testable. No DOM or persistent writes.
 * Uses completed Journal rows only; plans/scheduled items and drafts are not inputs.
 */
(function(factory){
  const api=factory();
  if(typeof module!=='undefined'&&module.exports)module.exports=api;
  if(typeof window!=='undefined')window.TrainPilot177Model=api;
})(function(){
 'use strict';
 const PERIODS={'7d':7,'30d':30,'3m':91};
 const GROUPS=['chest','back','shoulders','legs','arms','core'];
 const LABELS={chest:'Mell',back:'Hát',shoulders:'Váll',legs:'Láb',arms:'Kar',core:'Core'};
 const canonicalGroup=value=>{
  const k=String(value||'').trim().toLowerCase();
  if(['chest','mell','pectorals'].includes(k))return 'chest';
  if(['back','hát','hat'].includes(k))return 'back';
  if(['shoulders','shoulder','váll','vall','delts'].includes(k))return 'shoulders';
  if(['legs','leg','láb','lab','glutes','far','láb / far'].includes(k))return 'legs';
  if(['arms','arm','kar','biceps','triceps','forearms'].includes(k))return 'arms';
  if(['core','abs','törzs','torzs','has'].includes(k))return 'core';
  return null;
 };
 const localStart=date=>new Date(date.getFullYear(),date.getMonth(),date.getDate());
 const addDays=(date,delta)=>new Date(date.getFullYear(),date.getMonth(),date.getDate()+delta);
 const dayKey=date=>date.getFullYear()+'-'+String(date.getMonth()+1).padStart(2,'0')+'-'+String(date.getDate()).padStart(2,'0');
 const number=value=>{const n=Number(value);return Number.isFinite(n)&&n>0?n:0};
 const signedPercent=(current,previous)=>previous>0?Math.round((current-previous)*100/previous):null;
 const dateOf=workout=>{
  const value=workout.finished||workout.endedAt||workout.finishedAt||workout.completedAt;
  const n=Date.parse(value||'');
  return Number.isFinite(n)?new Date(n):null;
 };
 const groupsOf=(exercise,groupMap)=>{
  let raw=[];
  try{raw=groupMap?groupMap(exercise):[]}catch(_){raw=[]}
  if(!Array.isArray(raw))raw=[];
  if(!raw.length)raw=[exercise.muscleGroup,exercise.target];
  return [...new Set(raw.map(canonicalGroup).filter(Boolean))];
 };
 const addBest=(records,previous,key,value,record)=>{
  if(!(value>0))return;
  const old=previous.get(key);
  if(old!==undefined&&value>old+1e-6)records.push(record);
  if(old===undefined||value>old)previous.set(key,value);
 };
 const prepare=(raw,options)=>{
  const isDeleted=options&&options.isDeleted;
  const groupMap=options&&options.groupMap;
  const cleaned=[];
  for(const w of Array.isArray(raw)?raw:[]){
   if(!w||typeof w!=='object'||w.status==='draft'||w.cancelled||w.deleted)continue;
   if(isDeleted&&isDeleted(w))continue;
   const date=dateOf(w);if(!date)continue;
   const groups=Object.fromEntries(GROUPS.map(g=>[g,0]));
   let sets=0,volume=0,weighted=false;
   const performances=[];
   for(const e of Array.isArray(w.exercises)?w.exercises:[]){
    if(!e||typeof e!=='object')continue;
    const muscles=groupsOf(e,groupMap),done=(Array.isArray(e.sets)?e.sets:[]).filter(s=>s&&s.done===true);
    if(!done.length)continue;
    const id=String(e.id||e.hu||e.en||'unknown');
    const unit=String(e.repUnit||'').trim().toLowerCase();
    const timed=/mp|sec|second/.test(unit);
    const bilateral=/oldal|side/.test(unit);
    for(const s of done){
     sets++;
     const share=muscles.length>1?0.7:1;
     muscles.forEach((g,i)=>{groups[g]+=i===0?share:0.3/(muscles.length-1)});
     const weight=number(s.weight),reps=number(s.reps);
     if(!timed&&weight>0&&reps>0){volume+=weight*reps;weighted=true}
     performances.push({id,weight,reps,seconds:timed&&!bilateral?reps:0,
      left:bilateral?number(s.leftSeconds):0,right:bilateral?number(s.rightSeconds):0,
      timed,bilateral});
    }
   }
   cleaned.push({source:w,key:String(w.id||w.started||date.toISOString()),date,dateMs:date.getTime(),
    sets,volume:Math.round(volume*100)/100,weighted,groups,performances});
  }
  cleaned.sort((a,b)=>a.dateMs-b.dateMs);
  const best=new Map();
  for(const w of cleaned){
   const localBest=new Map();
   for(const s of w.performances){
    if(s.weight>0&&s.reps>0){
     const k=s.id+'|weight';
     const prior=localBest.get(k);
     if(!prior||s.weight>prior.value)localBest.set(k,{key:k,value:s.weight,kind:'weight',id:s.id,unit:'kg'});
     // Repetition records are comparable only at the same load.
     const kr=s.id+'|reps@'+s.weight;
     const prevRep=localBest.get(kr);
     if(!prevRep||s.reps>prevRep.value)localBest.set(kr,{key:kr,value:s.reps,kind:'reps',id:s.id,unit:'ism.',weight:s.weight});
    }else if(!s.timed&&s.reps>0){
     const k=s.id+'|bodyreps';
     const prev=localBest.get(k);
     if(!prev||s.reps>prev.value)localBest.set(k,{key:k,value:s.reps,kind:'reps',id:s.id,unit:'ism.'});
    }
    if(s.timed&&!s.bilateral&&s.seconds>0){
     const k=s.id+'|time';const prev=localBest.get(k);
     if(!prev||s.seconds>prev.value)localBest.set(k,{key:k,value:s.seconds,kind:'time',id:s.id,unit:'mp'});
    }
    if(s.bilateral&&s.left>0&&s.right>0){
     // Track both sides, using distinct comparable records.
     for(const side of ['left','right']){
      const k=s.id+'|'+side,prev=localBest.get(k),v=s[side];
      if(!prev||v>prev.value)localBest.set(k,{key:k,value:v,kind:side,id:s.id,unit:'mp'});
     }
    }
   }
   w.pr=[];
   localBest.forEach(record=>addBest(w.pr,best,record.key,record.value,{
    id:record.id,kind:record.kind,weight:record.weight||0,previous:best.get(record.key),
    value:record.value,unit:record.unit,date:w.date,workoutKey:w.key
   }));
  }
  return cleaned;
 };
 const build=(prepared,period,today)=>{
  const span=PERIODS[period]||PERIODS['3m'];
  const now=today instanceof Date&&!Number.isNaN(today.getTime())?today:new Date();
  const end=addDays(localStart(now),1),start=addDays(end,-span),previousStart=addDays(start,-span);
  const rows=(Array.isArray(prepared)?prepared:[]).filter(w=>w.date>=previousStart&&w.date<end);
  const inRange=(from,to)=>rows.filter(w=>w.date>=from&&w.date<to);
  const current=inRange(start,end),prior=inRange(previousStart,start);
  const sum=(xs,key)=>xs.reduce((n,w)=>n+w[key],0);
  const metric=xs=>({workouts:xs.length,sets:sum(xs,'sets'),volume:sum(xs,'volume'),
    weightedCount:xs.filter(w=>w.weighted).length,pr:xs.flatMap(w=>w.pr)});
  const a=metric(current),b=metric(prior);
  a.avgVolume=a.weightedCount?a.volume/a.weightedCount:null;
  b.avgVolume=b.weightedCount?b.volume/b.weightedCount:null;
  const groupScores=Object.fromEntries(GROUPS.map(g=>[g,0]));
  current.forEach(w=>GROUPS.forEach(g=>{groupScores[g]+=w.groups[g]||0}));
  const maxScore=Math.max(0,...Object.values(groupScores));
  const groups=GROUPS.map(g=>({key:g,name:LABELS[g],sets:groupScores[g],
   percent:maxScore?Math.round(groupScores[g]*100/maxScore):0}));
  const bucketDays=period==='7d'?1:7;
  const bucketCount=Math.ceil(span/bucketDays),buckets=[];
  for(let i=0;i<bucketCount;i++){
   const from=addDays(start,i*bucketDays),to=addDays(from,bucketDays);
   const xs=current.filter(w=>w.date>=from&&w.date<to);
   buckets.push({start:from,end:to<end?to:end,day:dayKey(from),volume:sum(xs,'volume'),workouts:xs});
  }
  const changes={
   volume:signedPercent(a.volume,b.volume),workouts:signedPercent(a.workouts,b.workouts),
   sets:signedPercent(a.sets,b.sets),pr:signedPercent(a.pr.length,b.pr.length),
   avgVolume:signedPercent(a.avgVolume||0,b.avgVolume||0)
  };
  return {period,span,start,end,previousStart,current,prior,buckets,groups,maxGroupScore:maxScore,
   currentMetrics:a,previousMetrics:b,changes};
 };
 return {PERIODS,GROUPS,LABELS,canonicalGroup,localStart,addDays,dayKey,number,signedPercent,prepare,build};
});