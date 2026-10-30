// This architectural guard enforces the agreed migration boundary: one finish
// entry point, rather than silently adding a new wrapper on the next feature.
const fs=require('node:fs'),assert=require('node:assert/strict');
const source=fs.readFileSync('www/app.js','utf8');
assert.equal([...source.matchAll(/\b(?:window\.)?finishWorkout\s*=\s*function\b/g)].length,1,'one canonical finish entry point');
for(const loop of source.matchAll(/for\s*\(\s*const name of (\[[^\]]*\])/g))assert.ok(!loop[1].includes('finishWorkout'),'finish must not be installed by a dynamic action wrapper');
assert.ok(source.includes('var TrainPilotWorkoutFinalization='));
console.log('PASS canonical workout finalization architecture: one entry point, no dynamic finish wrappers.');
