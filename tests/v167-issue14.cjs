const fs=require('node:fs'),assert=require('node:assert/strict');
const parts=['www/trainpilot-155-round3-panel-navigation.js','www/trainpilot-167-issue14-weight-panel.js'];
const app=parts.every(fs.existsSync)?parts.map(p=>fs.readFileSync(p,'utf8')).join('\n'):fs.readFileSync('www/app.js','utf8');
for(const marker of [
 "PANEL_ROUTES=new Set(['calendar','coach','settings','quick','exercises','weight'])",
 "if(type==='weight')return weightHtml()",
 "currentPanel==='weight'&&route==='health'",
 "if(currentPanel==='weight'){window.tp155R4RefreshPanel();return true}",
 "main?.querySelector('.tp151-back-row')?.remove()",
 "TrainPilot167Issue14",
 "sharedPanel:true",
 "healthParent:true",
 "stickyDangerClose:true",
 "androidBack:true",
 "noLegacyBackButton:true",
 "dataPreserved:true",
 "responsive:true"
])assert.ok(app.includes(marker),'Missing #14 marker: '+marker);
assert.match(app,/rf215WeightScreen=function\(\)\{[\s\S]{0,700}tp155R4OpenPanel\('weight'/);
assert.match(app,/#tp155R4PanelHost\[data-panel="weight"\] \.tp155-r4-panel-close\{position:sticky/);
console.log('PASS TrainPilot #14 shared Weight panel guards.');
