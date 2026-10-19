const assert=require('node:assert/strict');
const runtime=require('./helpers/app-runtime.cjs')();
const {run}=runtime;
// Compatibility mode runs each canonical section as a separate script; load the new section explicitly if needed.
if(run('typeof tp197CoachInfo')==='undefined'){
 const fs=require('node:fs');
 run(fs.readFileSync('www/training-coach-197.js','utf8'));
}
assert.equal(run("tp197CoachInfo('goblet-squat','home-basic')"),null);
run("tp197TrainingModes=()=>({schemaVersion:1,programId:'home-basic',explicitChoice:true,source:'program',trainingStyle:'normal',progressionMode:'linear'})");
run("tp197ExercisePreview=()=>({handled:true,action:'increase',reasonCode:'linear-stable-performance',historyCount:2,baseline:{weight:10,sets:3},proposed:{weight:12,sets:3}})");
run("state.session={programId:'home-basic',started:'2026-10-20T10:00:00Z',timerOwner:'phone',exercises:[{id:'goblet-squat',sets:[{set:1,weight:10,reps:'',done:false},{set:2,weight:10,reps:'',done:false}]}]};state.current=0");
assert.equal(run("tp197CoachInfo('goblet-squat','home-basic').action"),'increase');
assert.equal(run("tp197CoachAllowed(state.session,state.session.exercises[0],tp197CoachInfo('goblet-squat','home-basic'))"),true);
const before=run("JSON.stringify(state.session.exercises[0].sets)");
assert.equal(run("tp197CoachRequest()"),true);
assert.equal(run("JSON.stringify(state.session.exercises[0].sets)"),before);
run("state.session.exercises[0].sets[0].reps='1'");
assert.equal(run("tp197CoachApply()"),false);
assert.equal(run("JSON.stringify(state.session.exercises[0].sets)").includes('"weight":12'),false);
run("state.session.exercises[0].sets[0].reps='';state.session.timerOwner='watch'");
assert.equal(run("tp197CoachAllowed(state.session,state.session.exercises[0],tp197CoachInfo('goblet-squat','home-basic'))"),false);
run("state.session.timerOwner='phone';state.session.exercises[0].sets[1].done=true");
assert.equal(run("tp197CoachAllowed(state.session,state.session.exercises[0],tp197CoachInfo('goblet-squat','home-basic'))"),false);
console.log('PASS #197 Coach preview, stale approval, watch ownership and completed-set guards');
