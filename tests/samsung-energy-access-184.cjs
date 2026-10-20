'use strict';
const assert=require('node:assert/strict');
const {run}=require('./helpers/app-runtime.cjs')();
(async()=>{
 const web=require('./helpers/app-runtime.cjs')();
 web.run('window.__energyPaints=0;window.TrainPilotUIRefresh={refresh(){window.__energyPaints++}}');
 await web.run('window.TrainPilotEnergy184.refresh({force:true})');
 assert.equal(web.run('window.__energyPaints'),0,'No native score read may redraw web onboarding');
 run(`
  state.session=null;state.health={daily:{energyScore:98,energyScoreTime:new Date().toISOString(),energyScoreSource:'com.sec.android.app.shealth'},recovery:{sleepMinutes:480,sleepEnd:new Date().toISOString(),day:rf240DayKey(new Date())}};
  window.__energyTest={granted:false,requests:0,reads:0,mode:'ready',value:83};
  window.Capacitor={isNativePlatform:()=>true,Plugins:{SamsungHealth:{
   getStatus:async()=>{throw Error('Broad permission check must not be used');},
   getEnergyScoreStatus:async()=>({permissions:{READ_WEIGHT:true,READ_ENERGY_SCORE:__energyTest.granted}}),
   requestEnergyScoreRead:async()=>{__energyTest.requests++;__energyTest.granted=true;},
   readEnergyScoreDay:async args=>{__energyTest.reads++;if(__energyTest.mode==='error')throw Error('SDK unavailable');
    if(__energyTest.mode==='access')throw {code:'SAMSUNG_HEALTH_2003',message:'private native detail'};
    if(__energyTest.mode==='partial')return {day:args.day,failedFields:['energyScore'],errors:{energyScore:'SAMSUNG_HEALTH_3001'}};
    return {day:__energyTest.mode==='wrong-day'?'2000-01-01':args.day,energyScore:__energyTest.mode==='empty'?null:__energyTest.value,energyScoreTime:new Date().toISOString(),energyScoreSource:'com.sec.android.app.shealth'};}
  }}};
  var __energyTest=window.__energyTest;
  window.TrainPilotUIRefresh={refresh(){}};
 `);
 const provider=run('window.TrainPilotSamsungHealth.selected()');
 assert.equal(provider,'health_connect');
 await run('window.TrainPilotEnergy184.refresh({force:true})');
 assert.equal(run('__energyTest.requests'),0,'Automatic reads must never prompt for consent');
 assert.equal(run('__energyTest.reads'),0,'Weight permission is not Energy Score permission');
 assert.equal(run('rf220Readiness().energyScore'),null,'Revoked/missing permission cannot reuse an old score');
 assert.equal(run('rf220Readiness().score'),82,'Independent TrainPilot estimate remains available');
 assert.match(run('window.TrainPilotEnergy184.html()'),/Samsung energiapontszám/,'Missing data must still have a visible entry');
 await run('window.TrainPilotEnergy184.refresh({manual:true})');
 assert.equal(run('__energyTest.requests'),1);assert.equal(run('__energyTest.reads'),1);
 assert.equal(run('rf220Readiness().score'),83);assert.equal(run('rf220Readiness().trainpilotScore'),82);
 assert.equal(run('window.TrainPilotSamsungHealth.selected()'),provider,'Independent score must not change the steps/calories provider');
 assert.match(run('window.TrainPilotEnergy184.html()'),/83\/100/);
 await run('window.TrainPilotEnergy184.refresh()');
 assert.equal(run('__energyTest.reads'),1,'Automatic refreshes are bounded');
 for(const mode of ['empty','wrong-day','error','access','partial']){
  run(`__energyTest.mode=${JSON.stringify(mode)}`);
  await run('window.TrainPilotEnergy184.refresh({force:true})');
  assert.equal(run('rf220Readiness().energyScore'),null,mode);
  assert.equal(run('rf220Readiness().score'),82,mode+': keep the independent fallback');
  if(mode==='access'){assert.match(run('window.TrainPilotEnergy184.html()'),/alkalmazásszintű.*SAMSUNG_HEALTH_2003/);assert.doesNotMatch(run('window.TrainPilotEnergy184.html()'),/private native detail/);}
  if(mode==='partial')assert.match(run('window.TrainPilotEnergy184.html()'),/Frissítsd.*SAMSUNG_HEALTH_3001/);
 }
 run(`__energyTest.mode='ready';db.set('history',[{started:new Date(Date.now()-3600000).toISOString(),finished:new Date().toISOString(),exercises:[{id:'goblet-squat',effort:'pain'}]}]);`);
 await run('window.TrainPilotEnergy184.refresh({force:true})');
 assert.equal(run('rf220Readiness().energyScore'),83);assert.equal(run('rf220Readiness().score'),54,'Pain still limits the recommendation');
 run('__energyTest.value=54.968132');await run('window.TrainPilotEnergy184.refresh({force:true})');
 assert.equal(run('rf220Readiness().energyScore'),54.968132,'Keep the SDK value unmodified');
 assert.equal(run('rf220Readiness().score'),54,'Pain still limits Coach readiness');
 assert.match(run('window.TrainPilotEnergy184.html()'),/55\/100/,'Display score as a whole number');
 assert.doesNotMatch(run('window.TrainPilotEnergy184.html()'),/54\.968132/);
 assert.equal(run('tp184FormatScore(null)'),'—');
 run('__energyTest.granted=false');await run('window.TrainPilotEnergy184.refresh({force:true})');
 assert.equal(run('rf220Readiness().energyScore'),null);assert.equal(run('__energyTest.requests'),1);
 assert.equal(run('window.TrainPilotSamsungHealth.selected()'),provider);
 console.log('PASS #184 independent Energy Score: explicit permission, automatic source without provider change, visible missing state, stale/error fallback and pain cap');
})().catch(error=>{console.error(error);process.exitCode=1});
