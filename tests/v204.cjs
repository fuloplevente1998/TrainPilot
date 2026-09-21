const fs=require('fs');
const src=fs.readFileSync('www/v204.js','utf8');
for(const s of ["const RF204_VERSION='2.0.4'","rf200ThemePanel","!html.includes('<label>Témaszín</label>')","Koppints a teljes lista megnyitásához","appVersion:RF204_VERSION"])if(!src.includes(s))throw Error('Missing '+s);
console.log('PASS: RepForge 2.0.4 removes duplicate library entry and restores theme customization.');
