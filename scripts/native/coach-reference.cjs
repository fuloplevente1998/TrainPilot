#!/usr/bin/env node
// Offline reference fixture generation from the accepted main file, never from a working checkout.
const fs = require('node:fs');
const vm = require('node:vm');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const file = process.argv[2];
if (!file) throw Error('Pass www/app.js exported by git show 7473b30:www/app.js');
const bytes = fs.readFileSync(file);
const blob = crypto.createHash('sha1').update(Buffer.concat([Buffer.from(`blob ${bytes.length}\0`), bytes])).digest('hex');
assert.equal(blob, 'b5567576bed9a8f93608a6bf1ed8db1dee4723f4', 'Use the exact accepted main source');
const source = bytes.toString('utf8');
function blocks(prefix) {
  const escaped = prefix.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return [...source.matchAll(new RegExp('^' + escaped, 'gm'))].map(match => {
    const start = match.index, lineEnd = source.indexOf('\n', start), line = source.slice(start, lineEnd);
    if (line.trimEnd().endsWith(';')) return line;
    const end = source.indexOf('\n};', start);
    assert.ok(end >= 0, prefix);
    return source.slice(start, end + 3);
  });
}
function one(prefix, index = -1) {
  const found = blocks(prefix); assert.ok(found.length, prefix);
  return found.at(index);
}
const now = '2026-10-24T12:00:00Z';
const lib = {id:'press', loadType:'per_hand', repUnit:'ism.', measurementType:'reps', sets:2, reps:'8–12', weight:5};
const program = {id:'p', prescriptions:{press:{reps:'8–12', sets:2}}};
function history(count, options={}) {
  return Array.from({length:count}, (_,i) => {
    const finished = new Date(Date.parse(now)-(i+1)*86400000).toISOString();
    const e = {id:options.id||'press', loadType:options.loadType||'per_hand', repUnit:options.repUnit||'ism.',
      effort:options.effort||'good', sets:[0,1].map(j=>({done:true,reps:String(options.reps??12),weight:options.weight??10}))};
    if(options.variation)e.prescription={variation:options.variation};
    return {id:'h'+i,programId:options.programId||'p',started:finished,finished,exercises:[e]};
  });
}
const cases=[];
function add(name, h=[], opts={}) {
  cases.push({name,now,library:{...lib,...opts.library},program:opts.program||program,history:h,
    settings:opts.settings||{}, readiness:opts.readiness||{score:87,parts:2}, ...opts.extra});
}
add('no-history');
for(let n=1;n<=4;n++)add('upper-target-'+n,history(n));
let h=history(3); h[0].exercises[0].sets[1].done=false; add('partial',h);
h=history(3);h[1].exercises[0].effort='pain';add('recent-pain',h);
h=history(4);h[3].exercises[0].effort='pain';add('fourth-pain-outside-three-trend',h);
h=history(3);h[0].feedback={rating:'pain'};add('whole-workout-pain',h);
h=history(3);h[0].feedback={rating:'hard'};add('whole-workout-hard',h);
for(let n=2;n<=3;n++){h=history(n,{effort:'hard'});h.forEach((r,i)=>r.exercises[0].sets.forEach(s=>s.reps=String(5+i)));add('declining-'+n,h);}
h=history(3,{effort:'hard'});h[1].exercises[0].sets.forEach(s=>s.weight=8);add('mixed-loads-not-declining',h);
add('recovery-limits',history(3),{readiness:{score:54,parts:2}});
add('coach-moderates',history(3),{readiness:{score:74,parts:2}});
add('rir-is-not-rep-ceiling',history(3,{reps:3}),{program:{id:'p',prescriptions:{press:{reps:'2–3 RIR',sets:2}}}});
for(let n=2;n<=3;n++)add('body-plank-'+n,history(n,{id:'plank',loadType:'bodyweight',repUnit:'mp',reps:60,weight:0}),
  {library:{id:'plank',loadType:'bodyweight',repUnit:'mp',measurementType:'time',reps:'30–60 mp'},
    program:{id:'p',prescriptions:{plank:{reps:'30–60 mp',sets:2}}}});
add('body-rir',history(1,{id:'pushup',loadType:'bodyweight',weight:0,effort:'light',reps:8}),
  {library:{id:'pushup',loadType:'bodyweight'},program:{id:'p',prescriptions:{pushup:{reps:'2–3 RIR',sets:2}}}});
add('body-next-variation',history(3,{id:'pushup',loadType:'bodyweight',weight:0}),
  {library:{id:'pushup',loadType:'bodyweight'},program:{id:'p',prescriptions:{pushup:{reps:'8–12',sets:2}}}});
add('variation-mismatch',history(3,{variation:'paused'}));
add('load-mismatch',history(3,{loadType:'total'}));
add('metric-mismatch',history(3,{repUnit:'mp'}));
h=history(1);h[0].finished='2026-01-01T12:00:00Z';add('older-than-90-days',h);
h=history(1);h[0].finished='2026-10-25T12:00:00Z';add('future-excluded',h);
add('distance-light',history(3,{id:'running',repUnit:'km',loadType:'bodyweight',weight:0}),
  {library:{id:'running',repUnit:'km',loadType:'bodyweight',measurementType:'distance'},
    program:{id:'p',prescriptions:{}},readiness:{score:40,parts:2}});
add('configured-isolation-step',history(3,{id:'db-curl'}),{library:{id:'db-curl'},
  program:{id:'p',prescriptions:{'db-curl':{reps:'8–12',sets:2}}},settings:{progressionSteps:{isolation:.5}}});
add('missing-prescribed-sets',history(3),{program:{id:'p',prescriptions:{press:{reps:'8–12',sets:3}}}});
h=history(3,{programId:'old'});add('explicit-inherited-history',h,{program:{id:'p',prescriptions:{press:{reps:'8–12',sets:2}},
  history128:{entries:{press:{loadType:'per_hand',variation:'',metric:'reps',sources:h.map(r=>({programId:'old',key:r.id}))}}}}});
add('unrelated-history',history(3,{programId:'other'}));
add('uneven-reps',history(1,{reps:9}));
h=history(3);h.forEach((r,i)=>r.exercises[0].sets.forEach(s=>s.weight=10-i*.5));add('observed-small-step',h);
const output=[];
for(const input of cases) {
  const clock=Date.parse(now);class FixedDate extends Date {constructor(...args){super(...(args.length?args:[clock]));}static now(){return clock;}}
  const sandbox={Date:FixedDate,window:{},state:{session:null}, activeProgramId:()=>input.program.id,
    programById:id=>id===input.program.id?input.program:null,activeProgram:()=>input.program,
    byId:id=>id===input.library.id?input.library:null, exercises:()=>[input.library],history:()=>input.history,
    settings:()=>input.settings,rf220Readiness:()=>input.readiness,rf212Lang:()=> 'en',
    tp149T:key=>key,tp128T:key=>key,tp106T:key=>key,tp149FormatNumber:n=>String(n),
    tp149LoadLabel:load=>load,loadLabel:load=>load,tp149ProgressionNext:()=>'',tp149BodyGoal:()=>'',
    tp149ExerciseName:e=>e?.id||'',rf233L:()=>new Proxy({}, {get:(_,key)=>String(key)})};
  vm.createContext(sandbox);
  const activityStart=source.indexOf('const TP106_ACTIVITIES=[');
  const activityCode=source.slice(activityStart,source.indexOf('\n];',activityStart)+3);
  const snippets=[activityCode,one('const TP106_DISTANCE_IDS='),one('var tp106HasDistance='),one('const TP140_ISOLATION='),one('var tp140ParseTarget='),one('var tp140Program='),one('var tp140Prescription='),
    one('var tp140Recent='),one('var tp140Median='),one('var tp140Occurrence='),one('var tp140ObservedStep='),one('var tp140DefaultStep='),one('var tp140Step='),
    one('var tp140NextWeight='),one('var tp140NextRepTargets='),one('var tp128Finished='),one('var tp128RecentWorkout='),
    one('var tp128HistoryKey='),one('var tp128Metric='),one('var tp128Compatible='),one('var tp128BasisMatches='),one('var tp128Rows='),
    one('var tp128NextVariation='),one('var tp128VariationLabel='),one('var rf152DoneSets ='),one('var rf152BaseWeight ='),one('var rf152Last ='),
    one('tp140BodyRecommendation='),blocks('rf152Recommendation=function').filter(x=>x.includes('const lib=byId(id),programId=')).at(-1),
    'var tp106RecommendationBase=rf152Recommendation;',one('var tp106IsDistance='),
    blocks('rf152Recommendation=function').find(x=>x.includes('if(!tp106IsDistance')),
    'var tp128RecommendationBase=rf152Recommendation;',one('rf152Recommendation=function'),
    blocks('rf233ExerciseAdvice=function').find(x=>x.includes('const t=rf233L();let rec=')),
    'var tp106AdviceBase=rf233ExerciseAdvice;',blocks('rf233ExerciseAdvice=function').find(x=>x.includes('if(!tp106IsDistance')),
    'var tp128AdviceBase=rf233ExerciseAdvice;',one('rf233ExerciseAdvice=function')];
  snippets.forEach((code,i)=>{assert.equal(typeof code,'string','missing snippet '+i);vm.runInContext(code,sandbox,{timeout:1000});});
  sandbox._id=input.library.id;
  const result=vm.runInContext(`(()=>{const id=_id,rx=activeProgram().prescriptions[id]||{},target=tp140Prescription(id),rows=tp128Rows(id,activeProgramId(),4),r=rf152Recommendation(id),c=rf233ExerciseAdvice(id,{mode:rf220Readiness().score==null?'unknown':rf220Readiness().score<55?'light':rf220Readiness().score<75?'reduced':'normal'});return {target,rows,observed:tp140ObservedStep(id,rx.loadType||byId(id).loadType),step:tp140Step({...byId(id),...rx},rows[0]?tp140Occurrence(rows[0],target).weight:0),recommendation:{action:r.action,weight:r.weight,autoApply:r.autoApply,historyCount:r.historyCount,variation:r.nextVariation||null},coachAction:c.action,lastEffort:rf152Last(id)?.exercise?.effort||null};})()`,sandbox,{timeout:1000});
  output.push({...input,reference:JSON.parse(JSON.stringify(result))});
}
const destination=process.argv[3]||'android/app/src/test/resources/native-coach-main-7473.json';
fs.mkdirSync(require('node:path').dirname(destination),{recursive:true});
fs.writeFileSync(destination,JSON.stringify({source:{commit:'7473b30',path:'www/app.js',gitBlob:blob},cases:output},null,2)+'\n');
console.log(`${output.length} main reference scenarios written; no Android tests executed.`);
