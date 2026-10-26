#!/usr/bin/env node
'use strict';

// Narrow reference probe: evaluate the ACTUAL final main validateProfile,
// not an invented replacement generator. Requires git object 7473b30 locally.
// Run from the repository: node android/app/src/test/reference/planner-main-7473-exclusion.cjs
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const { execFileSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const commit = '7473b30f387eda54186c1e31830304ca12bd104f';
const appBlob = 'b5567576bed9a8f93608a6bf1ed8db1dee4723f4';
const root = execFileSync('git', ['rev-parse', '--show-toplevel'], { encoding: 'utf8' }).trim();
const source = execFileSync('git', ['show', commit + ':www/app.js'], {
    cwd: root, maxBuffer: 4 * 1024 * 1024
});
const actualBlob = crypto.createHash('sha1')
    .update('blob ' + source.length + '\0').update(source).digest('hex');
assert.equal(actualBlob, appBlob, 'the pinned main source blob must match');
const js = source.toString('utf8');

// Use the FINAL validateProfile override (the prior implementations are not authoritative).
const begin = js.lastIndexOf('validateProfile=function(p){');
assert.ok(begin >= 0, 'final validateProfile not found');
const end = js.indexOf('\n};', begin);
assert.ok(end > begin, 'final validator boundary not found');
const finalValidator = js.slice(begin, end + 3);
assert.ok(finalValidator.includes('planner.errorExclude'), 'expected final validation contract');

const policy = JSON.parse(fs.readFileSync(path.join(root,
    'android/app/src/main/resources/native-planner-main-7473.json'), 'utf8'));
assert.equal(policy.source.appBlob, appBlob);
const fixtures = JSON.parse(fs.readFileSync(path.join(root,
    'android/app/src/test/resources/native-planner-cases-main-7473.json'), 'utf8'));
const profile = fixtures.cases.find(x => x.name === 'main-plan-7').profile;

// Catalog stub: IDs from the pinned main's literal declarations, augmented by its
// source-derived native policy snapshot. This does NOT simulate exercises(), custom
// storage, browser state, or the generator; it proves rejection at validation entry.
const ids = new Set([
    ...Array.from(js.matchAll(/\bid\s*:\s*(['"])([a-z][a-z0-9-]*)\1/g), m => m[2]),
    ...Object.keys(policy.needs)
]);
assert.deepEqual(profile.excluded.filter(id => !ids.has(id)), ['squat', 'air-squat']);

const context = {
    validateProfile: null,
    exercises: () => Array.from(ids, id => ({ id })),
    profileGear132: p => p.gear || [],
    GEAR132: {},
    TP149_PLANNER_ROWS: { focus: { balanced: 'balanced' }, area: {} },
    tp149T: key => key, // label/error localization is irrelevant to acceptance
    rf148BackupValidationIds: null // normal generation, not backup import
};
vm.runInNewContext(finalValidator, context, { filename: 'main7473-validateProfile.js', timeout: 2000 });
assert.equal(typeof context.validateProfile, 'function');
let error = null;
try { context.validateProfile(profile); } catch (e) { error = e; }
assert.equal(error?.message, 'planner.errorExclude',
    'main must reject the invalid exclusion identifiers');
const corrected = {
    ...profile,
    excluded: ['pushup', 'modified-pushup', 'incline-pushup']
};
assert.ok(context.validateProfile(corrected), 'known exercise IDs should pass validation');
console.log('PASS: pinned main validateProfile rejects main-plan-7 invalid exclusions');
console.log('Catalog stub IDs:', ids.size, '| app blob:', actualBlob);
console.log('Scope: validator only; NOT a full generator, browser, or JVM test.');
