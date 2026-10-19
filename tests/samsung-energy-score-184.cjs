const assert = require('node:assert/strict');
const appRuntime = require('./helpers/app-runtime.cjs');

const { run } = appRuntime();

// 1. Given Samsung Health provider with today's Energy Score = 83
run(`
  state.health = state.health || {};
  state.health.daily = {
    energyScore: 83,
    energyScoreTime: new Date().toISOString(),
    energyScoreSource: 'com.sec.android.app.shealth',
    metricProviders: { energyScore: 'samsung_health' }
  };
  state.health.recovery = {
    day: rf240DayKey(new Date()),
    energyScore: 83,
    energyScoreTime: new Date().toISOString(),
    energyScoreSource: 'com.sec.android.app.shealth'
  };
`);

let readiness = run(`rf220Readiness()`);
assert.equal(readiness.energyScore, 83);
assert.equal(readiness.score, 83);
assert.ok(readiness.confidence.includes('Samsung Energiapontszám (83/100)'));

let decision = run(`rf233Decision(rf220Readiness())`);
assert.equal(decision.mode, 'normal');

// 2. When recent pain is reported, pain penalty applies to readiness and holds progression
run(`
  db.set('history', [{
    id: 'h1',
    started: new Date(Date.now() - 3600000).toISOString(),
    finished: new Date().toISOString(),
    exercises: [{ id: 'ex1', effort: 'pain' }]
  }]);
`);

readiness = run(`rf220Readiness()`);
assert.equal(readiness.score, 54); // Pain caps even a high Samsung score

decision = run(`rf233Decision(rf220Readiness())`);
let advice = run(`rf233ExerciseAdvice('ex1', rf233Decision(rf220Readiness()))`);
assert.equal(advice.action, 'pain');

// 3. Fallback when energyScore is not present
run(`
  state.health.daily = {};
  state.health.recovery = {};
  db.set('history', []);
`);

readiness = run(`rf220Readiness()`);
assert.equal(readiness.energyScore == null, true);
assert.equal(readiness.score, null);

console.log('PASS #184 Samsung Energy Score readiness base and pain hold');


// A stale, unverifiable or non-Samsung score must never determine readiness.
run("state.health.daily={ energyScore:98, energyScoreTime:new Date(Date.now()-3*86400000).toISOString(), energyScoreSource:'com.sec.android.app.shealth' }; state.health.recovery={};");
readiness=run("rf220Readiness()");
assert.equal(readiness.energyScore,null);
assert.equal(readiness.score,null);
run("state.health.daily={ energyScore:98, energyScoreTime:new Date().toISOString(), energyScoreSource:'untrusted_provider' };");
readiness=run("rf220Readiness()");
assert.equal(readiness.energyScore,null);
run("state.health.daily={ energyScore:101, energyScoreTime:new Date().toISOString(), energyScoreSource:'com.sec.android.app.shealth' };");
readiness=run("rf220Readiness()");
assert.equal(readiness.energyScore,null);
run("state.health.daily={ energyScore:98, energyScoreSource:'com.sec.android.app.shealth' };");
readiness=run("rf220Readiness()");
assert.equal(readiness.energyScore,null);
run("state.health.daily={};state.health.recovery={energyScore:95,energyScoreTime:new Date(Date.now()-2*86400000).toISOString(),energyScoreSource:'com.sec.android.app.shealth',day:rf240DayKey(new Date(Date.now()-2*86400000))};");
readiness=run("rf220Readiness()");
assert.equal(readiness.energyScore,null);

// Both independent scores may be visible, but sleep must not be added to Samsung's score.
run("state.health.daily={energyScore:83,energyScoreTime:new Date().toISOString(),energyScoreSource:'com.sec.android.app.shealth'};state.health.recovery={sleepMinutes:480,sleepEnd:new Date().toISOString(),day:rf240DayKey(new Date())};");
readiness=run("rf220Readiness()");
assert.equal(readiness.energyScore,83);
assert.equal(readiness.score,83);
assert.equal(readiness.trainpilotScore,82);
assert.equal(readiness.readinessSource,'samsung');
console.log('PASS #184 provenance/freshness, independent estimates and conservative pain guard');
