const assert = require('node:assert/strict');
const appRuntime = require('./helpers/app-runtime.cjs');

const { run } = appRuntime();

// 1. Given Samsung Health provider with today's Energy Score = 83
run(`
  state.health = state.health || {};
  state.health.daily = {
    energyScore: 83,
    metricProviders: { energyScore: 'samsung_health' }
  };
  state.health.recovery = {
    day: '2026-10-15',
    energyScore: 83
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
assert.equal(readiness.score, 73); // 83 + (80-70)*0.2 - 12 = 73

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
