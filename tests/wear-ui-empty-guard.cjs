'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),os=require('node:os'),path=require('node:path'),{spawnSync}=require('node:child_process');
const script=path.resolve('android/wear/ci/run-round-ui.sh'),base=fs.mkdtempSync(path.join(os.tmpdir(),'trainpilot-round-guard-'));
try {
 for(const [name,tests,gradleExit,expected] of [['missing',null,0,1],['empty',0,0,1],['executed',5,0,0],['gradle-failed',5,3,3]]){
  const root=path.join(base,name),bin=path.join(root,'bin'),android=path.join(root,'android');fs.mkdirSync(bin,{recursive:true});fs.mkdirSync(android);
  fs.writeFileSync(path.join(bin,'adb'),'#!/bin/sh\nexit 0\n',{mode:0o755});
  fs.writeFileSync(path.join(android,'gradlew'),'#!/bin/sh\nprintf "%s\\n" "$@" > invocation.txt\nexit '+gradleExit+'\n',{mode:0o755});
  if(tests!==null){const reports=path.join(android,'wear/build/outputs/androidTest-results/connected/debug');fs.mkdirSync(reports,{recursive:true});fs.writeFileSync(path.join(reports,'TEST-example.xml'),'<testsuite tests="'+tests+'" failures="0" errors="0"/>');}
  const filter='com.repforge.app.wear.WearFullScreenTest#example[160dp-font1]';
  const run=spawnSync('bash',[script],{cwd:root,env:{...process.env,PATH:bin+path.delimiter+process.env.PATH,TRAINPILOT_WEAR_UI_FILTER:filter},encoding:'utf8'});
  assert.equal(run.status,expected,name+': '+run.stdout+run.stderr);
  assert.ok(fs.readFileSync(path.join(android,'invocation.txt'),'utf8').split('\n').includes('-Pandroid.testInstrumentationRunnerArguments.class='+filter),'JUnit filter stays one literal argument');
 }
 console.log('PASS round UI gate rejects missing/zero tests, preserves Gradle failure and accepts executed tests; exact parameterized filters remain literal arguments');
} finally {fs.rmSync(base,{recursive:true,force:true});}
