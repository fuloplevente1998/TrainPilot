const fs=require('fs'),assert=require('node:assert/strict');
const s=fs.readFileSync('www/v228.js','utf8');
for(const needle of ["RF228_VERSION='2.2.8'",'transform:scale(.975)','Alvás és HRV frissítése…','A Health Connect nem válaszolt időben','rf225ReadRecovery'])assert.ok(s.includes(needle),needle);
console.log('PASS: 2.2.8 touch animation and Health recovery refresh watchdog present');
