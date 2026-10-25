'use strict';
const fs = require('node:fs');
const {execFileSync} = require('node:child_process');
const vm = require('node:vm');
const ref = '7473b30:www/app.js';
const blob = execFileSync('git', ['rev-parse', ref], {encoding:'utf8'}).trim();
if (blob !== 'b5567576bed9a8f93608a6bf1ed8db1dee4723f4') throw Error('Unexpected main reference');
const source = execFileSync('git', ['show', ref], {encoding:'utf8',maxBuffer:8*1024*1024});
const rx = source.slice(source.indexOf('var tp128Rx=function'),source.indexOf('var tp128PrescriptionBase='));
const gear = source.match(/var profileGear132 = function[^\n]+/)[0];
const overrides = source.slice(source.indexOf("   if(advanced&&['side-plank','crunch']"),source.indexOf('\n  }\n  if(ids.length<3)',source.indexOf("   if(advanced&&['side-plank','crunch']")));
const base = {experience:'intermediate',goal:'fitness',minutes:60,activity:'office',gear:['dumbbells']};
const cases = [
 ['pushup',false,{}],['pushup',true,{}],['decline-pushup',true,{}],['plank',false,{}],
 ['side-plank',true,{}],['side-plank',true,{gear:[]}],['crunch',true,{}],['lateral-raise',false,{}],
 ['db-ohp',true,{}],['barbell-row',true,{goal:'strength'}],['barbell-row',false,{goal:'muscle'}],
 ['barbell-row',false,{goal:'muscle',activity:'physical'}],['barbell-row',false,{goal:'muscle',experience:'advanced'}],
 ['barbell-row',false,{goal:'muscle',minutes:59}]
].map(([id,advanced,change],index) => {
 const library = {id,compound:['pushup','decline-pushup','db-ohp','barbell-row'].includes(id),
  repUnit:['plank','side-plank'].includes(id)?'mp':'ism.',loadType:['barbell-row','db-ohp','lateral-raise'].includes(id)?'per_hand':'bodyweight'};
 const profile = {...base,...change};
 const context = vm.createContext({exercises:()=>[library],GEAR132:{},id,profile,advanced,prescriptions:{}});
 vm.runInContext(gear+'\n'+rx+'\nprescriptions[id]=tp128Rx(id,profile,advanced);\n'+overrides, context);
 const expected = JSON.parse(JSON.stringify(context.prescriptions[id]));
 if (!expected.loadType) expected.loadType=library.loadType; // Native schema materializes the effective load type.
 return {name:`main-rx-${index}-${id}`,library,profile,level2:advanced,expected};
});
fs.writeFileSync('android/app/src/test/resources/native-prescription-main-7473.json',JSON.stringify({ref,blob,cases},null,2)+'\n');
process.stdout.write(`Generated ${cases.length} recipe fixtures from pinned main; Kotlin tests not run.\n`);
