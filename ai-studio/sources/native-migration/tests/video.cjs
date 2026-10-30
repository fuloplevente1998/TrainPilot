const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict');
let modal=null,alerts=[];
const ctx=vm.createContext({document:{getElementById:()=>modal,createElement:()=>({innerHTML:'',querySelector:()=>({focus(){}}),remove(){modal=null}}),body:{appendChild:x=>modal=x},addEventListener(){}},navigator:{onLine:true},esc:s=>String(s).replaceAll('"','&quot;'),byId:()=>({hu:'Tesztgyakorlat'}),alert:s=>alerts.push(s),persistDraft(){},isNative:()=>false,window:{open(){}}});
vm.runInContext(fs.readFileSync('www/demos.js','utf8'),ctx);
const run=s=>vm.runInContext(s,ctx);
run("openDemo('barbell-row')");assert.match(modal.innerHTML,/youtube.com\/embed\/fpgfWbe7yhc/);assert.match(modal.innerHTML,/role="dialog"/);assert.doesNotMatch(modal.innerHTML,/autoplay=1/);
run("openDemo('db-squat')");assert.match(modal.innerHTML,/youtube.com\/embed\/v_c67Omje48/);
run('closeDemo()');assert.equal(modal,null);ctx.navigator.onLine=false;run("openDemo('db-squat')");assert.equal(modal,null);assert.match(alerts.at(-1),/internet/);
console.log('PASS: YouTube player URLs, dialog close, no autoplay, offline fallback. Actual playback requires device testing.');
