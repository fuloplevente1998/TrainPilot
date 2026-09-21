const fs=require('fs'),assert=require('assert');
const s=fs.readFileSync('www/v203.js','utf8');
for(const x of ["RF203_VERSION='2.0.3'","function rf203ExerciseLibrary()","function rf203ExerciseDetail(id)","onclick=\"rf203ExerciseLibrary()\"","Koppints a teljes lista megnyitásához","appVersion:RF203_VERSION"])assert(s.includes(x),`Missing ${x}`);
console.log('PASS: v2.0.3 clickable exercise library and details');
