'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),os=require('node:os'),path=require('node:path');
const {spawnSync}=require('node:child_process');
const workflow=fs.readFileSync('.github/workflows/wear-layout-ui.yml','utf8');
const runner=fs.readFileSync('android/wear/ci/run-round-ui.sh','utf8');
const verifier='android/wear/ci/check-round-ui-shards.py';
assert.match(workflow,/matrix:[\s\S]*shard: \$\{\{ fromJSON\(/);
assert.match(workflow,/fail-fast: false/);
assert.match(workflow,/round-display-complete:/);
assert.match(workflow,/actions\/download-artifact@v7/);
assert.match(workflow,/TRAINPILOT_WEAR_UI_MIN_TOTAL:/);
assert.match(workflow,/\|'183' \}\}/);
assert.match(runner,/testInstrumentationRunnerArguments\.numShards/);
assert.match(runner,/testInstrumentationRunnerArguments\.shardIndex/);
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'round-ui-shard-'));
function put(index,name,status=''){
 const dir=path.join(tmp,'Wear-round-display-123-abcd-shard-'+index,'android/wear/build/outputs/androidTest-results/connected');
 fs.mkdirSync(dir,{recursive:true});
 const xml='<testsuite tests="1"><testcase classname="WatchFullScreenTest" name="'+name+'">'+status+'</testcase></testsuite>';
 fs.writeFileSync(path.join(dir,'TEST-WearFullScreenTest.xml'),xml);
}
function check(count,min){
 return spawnSync('python3',[verifier,tmp],{encoding:'utf8',env:{...process.env,
 TRAINPILOT_WEAR_UI_SHARD_COUNT:String(count),TRAINPILOT_WEAR_UI_MIN_TOTAL:String(min)}});
}
try{
 put(0,'layout[small]');put(1,'layout[large]');put(2,'scroll[large]');
 assert.equal(check(3,3).status,0,'Three shards should aggregate unique passing cases');
 assert.notEqual(check(3,4).status,0,'Coverage lower than baseline must fail');
 put(2,'layout[small]');
 assert.notEqual(check(3,3).status,0,'Duplicate test case across shards must fail');
 put(2,'scroll[large]','<failure message="scroll failed"/>');
 assert.notEqual(check(3,3).status,0,'Failed instrumentation case must fail');
 fs.rmSync(path.join(tmp,'Wear-round-display-123-abcd-shard-2'),{recursive:true,force:true});
 assert.notEqual(check(3,3).status,0,'Missing shard must fail');
 assert.equal(check(2,2).status,0,'A filtered two-shard fixture still validates');
 console.log('PASS Wear round UI shards: full case accounting, no duplication, strict failures, manual filter');
}finally{fs.rmSync(tmp,{recursive:true,force:true})}
