const fs=require('fs');
const src=fs.readFileSync('www/v144.js','utf8');
for(const s of ["const RF144_VERSION='1.4.4'","exerciseFavorites","toggleExerciseFavorite144","isExerciseFavorite144","Gyakorlatkönyvtár","★ Kedvenc","☆ Kedvenc","appVersion:RF144_VERSION"])if(!src.includes(s))throw Error('Missing '+s);
console.log('PASS: RepForge 1.4.4 exercise favorites and Gyakorlatkönyvtár naming.');
