'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const app=fs.existsSync('www/app.js')?fs.readFileSync('www/app.js','utf8'):'',context=vm.createContext({window:{addEventListener(){}},console});
for(const name of ['bilateral-192','intervals-162','automatic-time-162']){
 const path='www/'+name+'.js',code=fs.existsSync(path)?fs.readFileSync(path,'utf8'):app.match(new RegExp('// @section '+name+'\\.js\\n([\\s\\S]*?)// @endsection '+name+'\\.js'))[1];vm.runInContext(code,context);
}
const bi=context.window.TrainPilotBilateral192Core,core=context.window.TrainPilotInterval162Core,time=context.window.TrainPilotTime162Core;
for(const first of ['left','right']){
 const config={schema:1,first,leftSeconds:30,rightSeconds:40,sideRestSeconds:10},second=first==='left'?'right':'left';
 const row={exerciseId:'side-plank',setId:'a',work:30,rest:60,rounds:1,bilateral192:config,baseLeftSeconds:0,baseRightSeconds:0},steps=[row,{...row,setId:'b'}];
 const firstWork=config[first+'Seconds']*1000,secondWork=config[second+'Seconds']*1000;
 for(const [ms,phase,side,kind] of [[0,'work',first,''],[firstWork-1,'work',first,''],[firstWork,'rest',second,'side'],[firstWork+10000,'work',second,''],[80000,'rest',first,'set'],[140000,'work',first,''],[220000,'complete',undefined,undefined]]){
  const p=core.phase(steps,true,ms);assert.equal(p.phase,phase);assert.equal(p.side192,side);assert.equal(p.restKind192,kind);assert.equal(p.totalMs,220000);
 }
 const run={key:'side-'+first,steps,autoAdvance:true};const log=time.sample(run,220000,220000,false);
 assert.equal(log.schema,2);assert.equal(log.workMs,140000);assert.equal(log.restMs,80000);assert.equal(log.steps[0].leftWorkMs,30000);assert.equal(log.steps[0].rightWorkMs,40000);
 assert.equal(log.steps[0].sideRestMs,10000);assert.equal(log.steps[0].setRestMs,60000);assert.equal(log.steps[1].setRestMs,0);
 assert.equal(time.valid(log),true);const value=bi.values(row,log.steps[0],run.key);assert.equal(value.done,true);assert.equal(value.reps,'30');assert.equal(value.leftSeconds,30);assert.equal(value.rightSeconds,40);
 const session={};time.merge(session,{[log.key]:log});time.merge(session,{[log.key]:log});assert.equal(session.automaticTime162.workMs,140000);
 const forged=JSON.parse(JSON.stringify(log));forged.steps[0].leftWorkMs++;assert.equal(time.valid(forged),false,'side counters cannot contradict measured work');
 const rounds=[{...row,rounds:2}];assert.equal(core.phase(rounds,true,140000).round,2);assert.equal(core.phase(rounds,true,220000).phase,'complete');
 const zero=[{...row,rest:0,bilateral192:{...config,sideRestSeconds:0},rounds:2}];assert.equal(core.phase(zero,true,firstWork).side192,second);assert.equal(core.phase(zero,true,70000).round,2);assert.equal(core.phase(zero,true,140000).phase,'complete');
 const paused={key:'pause-'+first,steps:[row],autoAdvance:true};time.sample(paused,firstWork,firstWork,false);time.sample(paused,firstWork,firstWork+4000,true);
 time.sample(paused,firstWork+2000,firstWork+6000,true);time.sample(paused,firstWork+10000,firstWork+6000,true,false,true);
 const partial=time.sample(paused,firstWork+10000+secondWork/2,firstWork+6000+secondWork/2,false,true);
 assert.equal(partial.pausedMs,4000);assert.equal(partial.restMs,2000);assert.equal(partial.steps[0].sideRestMs,2000);assert.equal(bi.values(row,partial.steps[0],paused.key).done,false);
}
for(const value of [{schema:2,first:'left',leftSeconds:30,rightSeconds:30,sideRestSeconds:0},{schema:1,first:'other',leftSeconds:30,rightSeconds:30,sideRestSeconds:0},{schema:1,first:'left',leftSeconds:0,rightSeconds:30,sideRestSeconds:0},{schema:1,first:'left',leftSeconds:30,rightSeconds:30,sideRestSeconds:0.5}])assert.equal(bi.config(value),null);
console.log('PASS #192: both side orders, unequal work, exact boundaries, pair/round rest, final and zero rest, partial work, pause, actual skipped rest and versioned idempotent logs');
