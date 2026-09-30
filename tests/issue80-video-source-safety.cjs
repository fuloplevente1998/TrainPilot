const fs=require('node:fs'),assert=require('node:assert/strict'),vm=require('node:vm');
const src=fs.readFileSync('www/issue80-video-source-safety.js','utf8');
const records={
 'barbell-row':{provider:'youtube',videoId:'fpgfWbe7yhc',credit:'Zi workout',source:'https://www.youtube.com/watch?v=fpgfWbe7yhc'},
 'incline-pushup':{provider:'youtube',videoId:'0JUrOH--Kdk',credit:'NASM',source:'https://www.youtube.com/watch?v=0JUrOH--Kdk'},
 'db-pullover':{provider:'youtube',videoId:'a3cF2sS5v6I',credit:'YouTube – Dumbbell Pullover on Floor',source:'https://www.youtube.com/watch?v=a3cF2sS5v6I'},
 'unverified-other':{provider:'youtube',videoId:'unknown',credit:'YouTube – Unknown'},
 'db-ohp':{provider:'youtube',videoId:'Raemd3qWgJc',credit:'Renaissance Periodization',source:'https://www.youtube.com/watch?v=Raemd3qWgJc'}
};
let modal=null,opened=[],baseCalls=[],offline=false;
const node=()=>({innerHTML:'',querySelector:s=>({focus(){},addEventListener(type,fn){this.callback=fn}}),remove(){}});
const ctx=vm.createContext({
 window:{},navigator:{get onLine(){return !offline}},
 document:{createElement:node,body:{appendChild:m=>{modal=m}}},
 RF143_DEMOS:{'incline-pushup':'0JUrOH--Kdk'},
 demoInfo:id=>records[id],openDemo:id=>baseCalls.push(id),
 closeDemo:()=>{modal=null},openVideoLink:u=>opened.push(u),
 byId:id=>({en:id,hu:id,notes:'Keep your back neutral.'}),
 tp149ExerciseName:e=>e.hu,rf212Lang:()=> 'hu',
 esc:s=>String(s).replaceAll('&','&amp;').replaceAll('<','&lt;'),Date,JSON,Array,Object,String,encodeURIComponent
});
vm.runInContext(src,ctx);
const get=id=>vm.runInContext('demoInfo('+JSON.stringify(id)+')',ctx);
for(const [id,videoId] of Object.entries({'barbell-row':'kBWAon7ItDw','goblet-squat':'MeIiIdhvXT4','bulgarian-split-squat':'2C-uNgKwPLE','db-pullover':'ieFKuQAGYIA','cable-curl':'NFzTWp2qpiE'})){
 records[id] ||= {provider:'youtube',credit:'YouTube – Previous',videoId:'old'};
 assert.equal(get(id).provider,'youtube');assert.equal(get(id).videoId,videoId);
 assert.equal(get(id).source,'https://www.youtube.com/watch?v='+videoId);
 assert.equal(get(id).sourceVerified,true);assert.ok(!get(id).sourceKind);
}
assert.equal(get('incline-pushup').videoId,'0JUrOH--Kdk','existing named demos must not be blanket disabled');
assert.equal(get('db-ohp').provider,'youtube','existing named coach mapping preserved');
vm.runInContext("openDemo('unverified-other')",ctx);
assert.ok(modal.innerHTML.includes('Keep your back neutral.'));
assert.ok(!modal.innerHTML.includes('<iframe'),'unverified clip is never embedded');
assert.equal(opened.length,0,'no link opened without a click');
vm.runInContext("openDemo('db-ohp')",ctx);assert.deepEqual(baseCalls,['db-ohp']);
offline=true;vm.runInContext("openDemo('unverified-other')",ctx);
assert.ok(modal.innerHTML.includes('Offline'));assert.ok(!modal.innerHTML.includes('data-tp80-source'));
console.log('PASS #80 source provenance, verified guide, fallback search, existing videos and offline guide');