'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const { partitionSuite, parseArgs, runSuite } = require('./test-ui.cjs');
const suite = require('../tests/browser/suite.json');

test('every canonical test runs exactly once across six deterministic shards', () => {
  const shards = partitionSuite(suite, 6);
  const files = shards.flatMap(s => s.entries.map(e => e.file));
  assert.equal(files.length, suite.length);
  assert.equal(new Set(files).size, suite.length);
  assert.deepEqual([...files].sort(), suite.map(e => e.file).sort());
  assert.deepEqual(partitionSuite(suite, 6), shards);
  assert.ok(shards.every(s => s.entries.length));
  assert.ok(Math.max(...shards.map(s => s.seconds)) <= Math.max(...suite.map(e => e.estimatedSeconds)) + suite.reduce((n,e) => n + e.estimatedSeconds, 0) / 6);
  assert.deepEqual(partitionSuite(suite, 1)[0].entries, suite);
});

test('invalid shard arguments and duplicate/missing manifest entries fail closed', () => {
  for (const args of [['--shard','0/6'], ['--shard','7/6'], ['--shard','1/0'], ['--shard','1/2x'], ['--shard'], ['--shard','1/6','--shard','2/6'], ['--other']]) {
    assert.throws(() => parseArgs(args));
  }
  for (const count of [0, -1, 1.5, suite.length + 1]) assert.throws(() => partitionSuite(suite, count));
  for (const entries of [[], [suite[0],suite[0]], [{file:'../outside.cjs',estimatedSeconds:1}], [{file:'tests/browser/a.cjs',estimatedSeconds:0}]]) {
    assert.throws(() => partitionSuite(entries, 1));
  }
});

test('a failed child fails its shard, later tests still run, and timings are saved', async () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'trainpilot-ui-runner-'));
  try {
    fs.mkdirSync(path.join(root, 'tests/browser'), { recursive: true });
    const fixtures = [['fails', 'process.exit(7)'], ['later', "require('node:fs').writeFileSync('ran-after-failure','yes')"]];
    const entries = fixtures.map(([name,source]) => {
      const file = `tests/browser/${name}.cjs`;
      fs.writeFileSync(path.join(root,file), source);
      return {file,estimatedSeconds:1};
    });
    const result = await runSuite({root,entries,stdio:'ignore',log:()=>{}});
    assert.equal(result.status,'failed');
    assert.deepEqual(result.results.map(r => r.exitCode), [7,0]);
    assert.equal(fs.readFileSync(path.join(root,'ran-after-failure'),'utf8'),'yes');
    assert.deepEqual(JSON.parse(fs.readFileSync(path.join(root,'ui-results/shard-1-of-1.json'))),result);
    assert.ok(result.results.every(r => r.durationSeconds > 0));
    fs.unlinkSync(path.join(root,entries[1].file));
    fs.unlinkSync(path.join(root,'ran-after-failure'));
    await assert.rejects(runSuite({root,entries,stdio:'ignore',log:()=>{}}));
    assert.ok(!fs.existsSync(path.join(root,'ran-after-failure')));
  } finally { fs.rmSync(root,{recursive:true,force:true}); }
});

test('the real CLI exits unsuccessfully on test failure or invalid selection', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'trainpilot-ui-cli-'));
  try {
    fs.mkdirSync(path.join(root, 'scripts'));
    fs.mkdirSync(path.join(root, 'tests/browser'), { recursive: true });
    fs.copyFileSync(path.join(__dirname, 'test-ui.cjs'), path.join(root, 'scripts/test-ui.cjs'));
    const entries = [{file:'tests/browser/fails.cjs',estimatedSeconds:1}];
    fs.writeFileSync(path.join(root, 'tests/browser/suite.json'), JSON.stringify(entries));
    fs.writeFileSync(path.join(root, entries[0].file), 'process.exit(5)');
    const run = args => spawnSync(process.execPath, ['scripts/test-ui.cjs', ...args], { cwd:root, encoding:'utf8' });
    assert.equal(run([]).status,1);
    assert.equal(JSON.parse(fs.readFileSync(path.join(root,'ui-results/shard-1-of-1.json'))).status,'failed');
    fs.rmSync(path.join(root,'ui-results'),{recursive:true});
    for (const args of [['--shard','2/1'],['--shard','1/6']]) {
      assert.equal(run(args).status,1);
      assert.ok(!fs.existsSync(path.join(root,'ui-results')));
    }
    fs.writeFileSync(path.join(root, entries[0].file), 'process.exit(0)');
    assert.equal(run([]).status,0);
  } finally { fs.rmSync(root,{recursive:true,force:true}); }
});
