const fs=require('fs');const s=fs.readFileSync('www/v200.js','utf8');
for(const x of ["RF200_VERSION='2.0.0'","themeAccent","Témaszín","Sárga","Kék","Zöld","Lila","Narancs","rf200CleanVersionLabels","rf200ApplyTheme"])if(!s.includes(x))throw Error('Missing '+x);
console.log('PASS: RepForge 2.0 theme selector and UI version-label cleanup');
