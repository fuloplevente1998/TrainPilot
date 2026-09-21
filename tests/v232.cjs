const fs=require('fs');const s=fs.readFileSync('www/v232.js','utf8');
function ok(x,m){if(!x)throw new Error(m)}
ok(s.includes("RF232_VERSION='2.3.2'"),'version');
ok(s.includes("['easy','light','good','challenging','hard']"),'five-step effort scale');
ok(s.includes("light:'Kicsit könnyű'"),'slightly easy option');
ok(s.includes("challenging:'Kicsit nehéz'"),'slightly hard option');
ok(s.includes("hard:'Túl nehéz'"),'too hard label');
ok(s.includes("pain:'Fájdalom / kellemetlenség'"),'pain remains separate');
ok(s.includes("effort!=='light'&&effort!=='challenging'"),'new effort states handled');
ok(s.includes("e.effortScore=rf232EffortScore(v)"),'numeric effort score stored');
ok(s.includes("challenging:-4"),'Coach handles slightly hard');
ok(s.includes("light:1"),'Coach handles slightly easy');
ok(s.includes('rf232-effort'),'compact five-button UI');
console.log('PASS: TrainPilot 2.3.2 five-step effort feedback and Coach effort integration.');
