const fs=require('fs');
const src=fs.readFileSync('www/v205.js','utf8');
for(const s of ["RF205_VERSION='2.0.5'","Gyakorlatok / Izomcsoportok","rf205PrimaryMuscle","rf205PrimaryMuscle(e.id)===group","libraryGear(e)===gear"])if(!src.includes(s))throw Error('Missing '+s);
if(src.includes("muscleGroups(e.id).includes(group)"))throw Error('Secondary-muscle filtering must not be reintroduced in v205.');
console.log('PASS: RepForge 2.0.5 primary muscle filter and library naming.');
