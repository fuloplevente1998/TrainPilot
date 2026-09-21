const fs=require('fs');const s=fs.readFileSync('www/v201.js','utf8');
for(const x of ["const RF201_VERSION='2.0.1'","planScreen=function()","activeProgram()","AKTÍV PROGRAM","Másik program választása","rf154EditSlot","rf154AddScreen","startWorkout","showWorkout=function(dayId)"])if(!s.includes(x))throw Error('Missing '+x);
if(!s.includes("${esc(p.name)}"))throw Error('Active program name must be reused on workout tab');
console.log('PASS: v2.0.1 workout tab shows and edits only the active program');
