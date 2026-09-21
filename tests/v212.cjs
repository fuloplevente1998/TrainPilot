const fs=require('fs');
const s=fs.readFileSync('www/v212.js','utf8');
for(const x of ["RF212_VERSION='2.1.2'","navigator.languages","navigator.language","system:{name:'Rendszer nyelve'}","en:{name:'English'}","de:{name:'Deutsch'}","ro:{name:'Română'}","rf212SetLang","rf212LanguagePanel","document.documentElement.lang","language:rf212LangSetting()"]){if(!s.includes(x))throw Error('Missing '+x);}
for(const x of ['Home','Settings','Gesundheit','Setări','Sănătate'])if(!s.includes(x))throw Error('Missing translation '+x);
console.log('PASS: TrainPilot 2.1.2 system language and manual language selection.');
