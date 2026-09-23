const assert=require('node:assert/strict'),fs=require('node:fs');
const index=fs.readFileSync('www/index.html','utf8');
const start=index.indexOf('/* #16 — Active workout:');
const end=index.indexOf('window.TrainPilotIssue16=',start);
assert.ok(start>=0&&end>start,'Missing #16 runtime block');
const block=index.slice(start,end);
for(const token of [
 'tp16-workout-actions','tp16-workout-prev','tp16-workout-next','tp16-workout-finish',
 'tp16-rest-timer','tp16-rest-docked','tp16-rest-floating','visualViewport',
 'safe-area-inset-bottom','--tp16-actions-height','--tp16-vv-bottom','--tp16-rest-top',
 'min-height:52px','font-size:22px'
])assert.ok(block.includes(token),'Missing #16 contract '+token);
assert.match(block,/grid-template-areas:"prev next"/,'Previous must remain left of Next on normal phone width');
assert.match(block,/max-width:340px[^]*grid-template-areas:"prev" "next"/,'Narrow layout must preserve Previous then Next order');
for(const forbidden of [/nextExercise\s*=\s*function/,/prevExercise\s*=\s*function/,/skipTimer\s*=\s*function/,/startRest\s*=\s*function/,/stopTimer\s*=\s*function/]){
 assert.equal(forbidden.test(block),false,'#16 must not replace established workout/rest logic: '+forbidden);
}
console.log('PASS #16 static sticky-controls / floating-rest contract');
