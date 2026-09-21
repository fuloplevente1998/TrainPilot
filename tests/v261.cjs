const fs=require('fs'),assert=require('node:assert/strict');
const pkg=require('../package.json'),srcMeta=require('../SOURCE_VERSION.json');
const ui=fs.readFileSync('www/v260.js','utf8'),themes=fs.readFileSync('www/v200.js','utf8');
assert.equal(pkg.version,'1.1.2');assert.equal(srcMeta.version,'1.1.2');assert.equal(srcMeta.versionCode,2609);
assert.ok(ui.includes("scheduled().filter(x=>!x.cancelled&&!isDone(x))"),'2.6.1 planned-list cleanup must remain');
assert.ok(ui.includes("'$1<b>✓</b>$2'"),'2.6.1 check-only calendar behavior must remain');
assert.ok((themes.match(/accent:'#[0-9a-fA-F]{6}'/g)||[]).length>=10,'2.6.1 expanded theme palette must remain');
console.log('PASS 2.6.1 compatibility on current 2.6.2 runtime.');

