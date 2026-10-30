const fs=require('fs');
const s=fs.readFileSync('www/v154.js','utf8');
for(const needle of [
 "const RF154_VERSION='1.5.4'",
 "showWorkout=function(w)",
 "rf154EditSlot",
 "rf154Replace",
 "rf154Remove",
 "rf154AddScreen",
 "rf154Add",
 "+ Gyakorlat hozzáadása",
 "Gyakorlat törlése a programból"
]){
 if(!s.includes(needle))throw new Error('Missing v1.5.4 feature: '+needle);
}
console.log('v1.5.4 inline program editor tests passed');
