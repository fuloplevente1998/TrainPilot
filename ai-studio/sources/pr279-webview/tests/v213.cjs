const fs=require('fs');
const s=fs.readFileSync('www/v213.js','utf8');
for(const x of ["RF213_VERSION='2.1.3'","Not sure how to start?","Workout calendar","Google account and sync","Dumbbell Squat","rf213EnglishExerciseCards","rf212Lang()==='en'"])if(!s.includes(x))throw Error('Missing '+x);
console.log('PASS: TrainPilot 2.1.3 English localization coverage.');
