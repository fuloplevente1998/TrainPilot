'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { spawn, spawnSync } = require('node:child_process');
const { performance } = require('node:perf_hooks');

function validateSuite(entries) {
  if (!Array.isArray(entries) || entries.length === 0) throw Error('UI suite must not be empty');
  const files = new Set();
  for (const entry of entries) {
    if (!/^tests\/browser\/[\w.-]+\.cjs$/.test(entry.file) || files.has(entry.file)) {
      throw Error('Invalid or duplicate UI test: ' + entry.file);
    }
    if (!Number.isFinite(entry.estimatedSeconds) || entry.estimatedSeconds <= 0) {
      throw Error('Invalid duration estimate: ' + entry.file);
    }
    files.add(entry.file);
  }
  return entries;
}

function partitionSuite(entries, count) {
  validateSuite(entries);
  if (!Number.isInteger(count) || count < 1 || count > entries.length) throw Error('Invalid shard count');
  const shards = Array.from({ length: count }, () => ({ seconds: 0, entries: [] }));
  // Longest first; stable ties keep the same plan on every independent runner.
  for (const entry of [...entries].sort((a, b) => b.estimatedSeconds - a.estimatedSeconds)) {
    const target = shards.reduce((best, current) => current.seconds < best.seconds ? current : best);
    target.entries.push(entry);
    target.seconds += entry.estimatedSeconds;
  }
  for (const shard of shards) shard.entries.sort((a, b) => entries.indexOf(a) - entries.indexOf(b));
  return shards;
}

function parseArgs(args) {
  let index = 1, count = 1, list = false;
  const seen = new Set();
  for (let i = 0; i < args.length; i++) {
    const flag = args[i];
    if (seen.has(flag)) throw Error('Duplicate option: ' + flag);
    seen.add(flag);
    if (flag === '--list') list = true;
    else if (flag === '--shard') {
      const match = /^(\d+)\/(\d+)$/.exec(args[++i] || '');
      if (!match) throw Error('Use --shard INDEX/COUNT');
      index = Number(match[1]); count = Number(match[2]);
      if (!Number.isSafeInteger(index) || !Number.isSafeInteger(count) || index < 1 || index > count) {
        throw Error('Shard index is outside its count');
      }
    } else throw Error('Unknown option: ' + flag);
  }
  return { index, count, list };
}

async function runSuite({ root, entries, index = 1, count = 1, stdio = 'inherit', log = console.log }) {
  const shards = partitionSuite(entries, count);
  if (!Number.isInteger(index) || index < 1 || index > count) throw Error('Invalid shard index');
  // Check the whole manifest before any shard starts, so a missing file cannot be silently omitted.
  for (const entry of entries) if (!fs.statSync(path.join(root, entry.file)).isFile()) {
    throw Error('Missing UI test: ' + entry.file);
  }
  const assigned = shards[index - 1].entries;
  const folder = path.join(root, 'ui-results');
  fs.mkdirSync(folder, { recursive: true });
  const output = path.join(folder, `shard-${index}-of-${count}.json`);
  const git = spawnSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' });
  const report = {
    sourceCommit: process.env.GITHUB_SHA || (git.status === 0 ? git.stdout.trim() : null),
    shard: index, shardCount: count, suiteCount: entries.length,
    assigned: assigned.map(e => e.file), startedAt: new Date().toISOString(),
    status: 'running', results: [], durationSeconds: 0
  };
  const persist = () => fs.writeFileSync(output, JSON.stringify(report, null, 2) + '\n');
  persist();
  const started = performance.now();
  for (const entry of assigned) {
    log(`UI START [${index}/${count}] ${entry.file}`);
    const start = performance.now();
    const result = await new Promise(resolve => {
      const child = spawn(process.execPath, [entry.file], { cwd: root, stdio });
      child.once('error', error => resolve({ exitCode: null, signal: null, error: error.message }));
      child.once('close', (exitCode, signal) => resolve({ exitCode, signal }));
    });
    const passed = result.exitCode === 0 && !result.error && !result.signal;
    const durationSeconds = Math.round((performance.now() - start) / 10) / 100;
    report.results.push({ file: entry.file, ...result, passed, durationSeconds });
    report.durationSeconds = Math.round((performance.now() - started) / 10) / 100;
    persist();
    log(`UI ${passed ? 'PASS' : 'FAIL'} [${index}/${count}] ${entry.file} (${durationSeconds}s)`);
  }
  report.status = report.results.every(r => r.passed) ? 'passed' : 'failed';
  persist();
  log(`UI SHARD ${index}/${count}: ${report.results.filter(r => r.passed).length}/${assigned.length} passed in ${report.durationSeconds}s`);
  return report;
}

async function main(args) {
  const options = parseArgs(args);
  const root = path.resolve(__dirname, '..');
  const entries = JSON.parse(fs.readFileSync(path.join(root, 'tests/browser/suite.json'), 'utf8'));
  const shards = partitionSuite(entries, options.count);
  if (options.list) {
    console.log(JSON.stringify(shards.map((s, i) => ({ shard: i + 1, estimatedSeconds: s.seconds, tests: s.entries.map(e => e.file) })), null, 2));
    return;
  }
  const report = await runSuite({ root, entries, ...options });
  if (report.status !== 'passed') process.exitCode = 1;
}

module.exports = { validateSuite, partitionSuite, parseArgs, runSuite };
if (require.main === module) main(process.argv.slice(2)).catch(error => {
  console.error(error);
  process.exitCode = 1;
});
