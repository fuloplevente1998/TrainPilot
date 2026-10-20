'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
function section(name){
 const file='www/'+name;if(fs.existsSync(file))return fs.readFileSync(file,'utf8');
 const source=fs.readFileSync('www/app.js','utf8'),begin='// @section '+name+'\n',end='// @endsection '+name;
 const at=source.indexOf(begin);assert.ok(at>=0,name);return source.slice(at+begin.length,source.indexOf(end,at));
}
const box={window:{},console,Date};vm.createContext(box);
vm.runInContext(section('trainpilot-177-progress-model.js'),box);
vm.runInContext(section('workout-records-226.js'),box);
const api=box.window.TrainPilotRecords226;
const row=(id,day,w,r,extra={})=>({id,started:day+'T10:00:00Z',finished:day+'T11:00:00Z',
 programId:'p',dayId:'A',exercises:[{id:'press',repUnit:'ism.',sets:[{weight:w,reps:r,done:true}]}],...extra});
const first=row('a','2026-10-01',10,10),equal=row('b','2026-10-02',10,10),better=row('c','2026-10-03',12,10);
let map=api.index([first,equal,better]);
assert.equal(map.get('a').length,0,'First baseline is not a PR');
assert.equal(map.get('b').length,0,'Equal value is not a PR');
assert.equal(map.get('c').length,1);assert.equal(map.get('c')[0].previous,10);assert.equal(map.get('c')[0].value,12);
assert.equal(map.get(better.started),map.get('c'),'Saved result can be resolved by original workout start');
assert.equal(api.index([first,{...better,status:'draft'}]).has('c'),false);
assert.equal(api.index([first,better],undefined,x=>x.id==='a').get('c').length,0,'Deleted baseline cannot award a PR');
const repeated=row('d','2026-10-04',12,12);
map=api.index([first,better,repeated]);
assert.ok(map.get('d').some(x=>x.kind==='reps'&&x.weight===12&&x.previous===10&&x.value===12),'Reps compare at the same load');
assert.equal(api.index([first,{...better,exercises:equal.exercises}]).get('c').length,0,'Editing result removes obsolete badge');
assert.equal(JSON.stringify(first),JSON.stringify(row('a','2026-10-01',10,10)),'Presentation does not mutate source history');
console.log('PASS #226 saved PR badges: canonical model, baseline/equality, saved identity, drafts, deletion, same-load reps and edited results');
