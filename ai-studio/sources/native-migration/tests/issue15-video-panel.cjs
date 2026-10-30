const fs=require('node:fs'),assert=require('node:assert/strict');
const index=fs.readFileSync('www/index.html','utf8'),css=fs.readFileSync('www/styles.css','utf8');
for(const token of [
 "window.TrainPilotDemo15={version:'15'",
 "window.openDemo=function(id)",
 "const openBase=window.openDemo",
 "const workoutBase=window.renderWorkout",
 "guide.replaceWith(button)",
 "window.closeDemo=function()",
 "tp15-demo-guide",
 "data-tp-video-close",
 "external.slice(1).forEach(button=>button.remove())",
 "const source=dialog.querySelector('.small.muted')",
 "modal.addEventListener('keydown'"
])assert.ok(index.includes(token),'Missing #15 runtime contract '+token);
for(const token of ['.tp15-demo-header','.tp15-demo-dialog','.tp15-demo-close','.tp15-demo-launch','.tp15-demo-guide','.tp15-demo-external','position:sticky','aspect-ratio:16/9'])assert.ok(css.includes(token),'Missing #15 style '+token);
const block=index.slice(index.indexOf('/* #15 — One-tap'),index.indexOf('window.TrainPilotDemo15='));
assert.ok(!block.includes("const DEMOS ="),'Demo source mappings must remain untouched');
assert.ok(!block.includes('openVideoLink=async'),'Native video fallback must remain canonical');
console.log('PASS #15 one-tap demo/panel static guards');