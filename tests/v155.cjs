const fs=require('fs'),assert=require('assert');
const s=fs.readFileSync('www/v155.js','utf8');
for(const needle of ["db.set('programs',s.ps)","rf154Replace=function","rf154Remove=function","rf154Add=function","changePlan=function","1.5.5"])assert.ok(s.includes(needle),needle);
console.log('v1.5.5 active program editor persistence checks passed');
