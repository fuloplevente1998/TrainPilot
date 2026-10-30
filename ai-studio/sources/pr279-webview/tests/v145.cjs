const fs=require('fs');
const src=fs.readFileSync('www/v145.js','utf8');
for(const s of [
  "const RF145_VERSION='1.4.5'",
  "'home-basic'",
  "'home-level2'",
  "'home-varied'",
  "function rf145UpgradeHomePrograms()",
  "function rf145MuscleSets()",
  "Munkasorozatok izomcsoportonként",
  "Súlyzós volumen izomcsoport szerint",
  "generatePersonalProgram=function(input)",
  "plan.generatorVersion=RF145_VERSION",
  "mell–váll–törzs"
])if(!src.includes(s))throw Error('Missing '+s);
const expected={
 'home-basic':{A:['db-squat','db-floor-press','one-arm-row','db-ohp','db-rdl','plank'],B:['reverse-lunge','pushup','barbell-row','lateral-raise','glute-bridge','crunch']},
 'home-level2':{A:['goblet-squat','db-floor-press','one-arm-row','db-ohp','db-rdl','side-plank'],B:['bulgarian-split-squat','pushup','db-pullover','lateral-raise','glute-bridge','dead-bug']},
 'home-varied':{A:['db-front-squat','incline-pushup','one-arm-row','pike-pushup','db-rdl','side-plank'],B:['prisoner-squat','pushup','barbell-row','lateral-raise','glute-bridge','dead-bug']}
};
for(const [id,days] of Object.entries(expected))for(const [day,ids] of Object.entries(days))for(const x of ids)if(!src.includes(`'${x}'`))throw Error(`${id}/${day} missing ${x}`);
console.log('PASS: RepForge 1.4.5 focused home plans, full-body generator guardrails, Hungarian muscle labels and working-set statistics.');
