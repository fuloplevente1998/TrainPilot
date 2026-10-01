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
const directVideos={
 'barbell-row':'kBWAon7ItDw','goblet-squat':'MeIiIdhvXT4','bulgarian-split-squat':'2C-uNgKwPLE','db-pullover':'ieFKuQAGYIA','cable-curl':'NFzTWp2qpiE',
 'side-plank':'XeN4pEZZJNI','db-rdl':'hQgFixeXdZo','db-reverse-fly':'lPt0GqwaqEw',
 'pullup':'9yVGh3XbJ34','chinup':'e1YSApl-QcM','negative-pullup':'koEwdlmUxoM','scapular-pullup':'1yxFXaH3rtE','inverted-row':'wRp2hCv_4ZQ','dip':'B6eMPQN7ej4','bench-dip':'WVeZDBhZwLA','diamond-pushup':'_4EGPVJuqfA','pseudo-planche-pushup':'i-gSmqe9tNw','hanging-knee-raise':'G6a5267YpHM','hanging-leg-raise':'Pr1ieGZ5atk','hollow-hold':'hK7QTWGRBu0','superman-hold':'g0Kr9Wd3CeQ','mountain-climber':'cnyTQDSE884',
 'band-row':'LSkyinhmA8k','band-lat-pulldown':'VxCt-KKvzBQ','band-chest-press':'Fj1WkCwt-nk','band-overhead-press':'0VjBZALMJD0','band-face-pull':'guG4eqBOfwA','band-biceps-curl':'A0yEHB67kbI','band-triceps-pushdown':'gjq9dmxU9fk','band-pallof-press':'BvSB4tvgLGc',
 'kb-goblet-squat':'aNDUbH_Uv4g','kb-deadlift':'MJPGkNqAXzg','kb-swing':'jYtimecuwuw','kb-clean':'pDtRBx1fm70','kb-press':'gjr-QAdsq4o','kb-row':'l5qelXL5nfs','kb-reverse-lunge':'y2jz7ot6r2I','kb-halo':'Sci3lijQBmk',
 'barbell-back-squat':'gcNh17Ckjgg','conventional-deadlift':'1nvAUJVmFZY','bench-press':'rT7DgCr-3pg','incline-bench-press':'BjGLs6KGWUc','barbell-overhead-press':'cGnhixvC8uA','assisted-pullup-machine':'gx0RWT7WbmA','pec-deck':'XZg28DIf1oc','cable-lateral-raise':'qitQHqNZbeM','hack-squat':'hrciyIRwFzs','standing-calf-raise':'KVZ3qtJA4AU'
};
const rf148Ids=[
 'pullup','chinup','negative-pullup','scapular-pullup','inverted-row','dip','bench-dip','diamond-pushup','pseudo-planche-pushup','hanging-knee-raise','hanging-leg-raise','hollow-hold','superman-hold','mountain-climber',
 'band-row','band-lat-pulldown','band-chest-press','band-overhead-press','band-face-pull','band-biceps-curl','band-triceps-pushdown','band-pallof-press',
 'kb-goblet-squat','kb-deadlift','kb-swing','kb-clean','kb-press','kb-row','kb-reverse-lunge','kb-halo',
 'barbell-back-squat','conventional-deadlift','bench-press','incline-bench-press','barbell-overhead-press','assisted-pullup-machine','pec-deck','cable-lateral-raise','hack-squat','standing-calf-raise'
];
assert.equal(rf148Ids.length,40);
for(const id of rf148Ids)records[id]={provider:null,videoId:null,credit:'YouTube keresés',source:'https://www.youtube.com/results?search_query='+encodeURIComponent(id)};
for(const id of ['side-plank','db-rdl','db-reverse-fly'])records[id]={provider:'youtube',credit:'YouTube – Previous',videoId:'old',source:'https://www.youtube.com/watch?v=old'};
for(const [id,videoId] of Object.entries(directVideos)){
 records[id] ||= {provider:'youtube',credit:'YouTube – Previous',videoId:'old'};
 assert.equal(get(id).provider,'youtube',id+' must resolve to a direct video');
 assert.equal(get(id).videoId,videoId,id+' exact video id');
 assert.equal(get(id).source,'https://www.youtube.com/watch?v='+videoId,id+' direct source');
 assert.equal(get(id).sourceVerified,true,id+' verified flag');assert.ok(!get(id).sourceKind,id+' must not fall back to search');
}
assert.equal(get('db-rdl').videoId,'hQgFixeXdZo','Dumbbell Romanian Deadlift must keep the user-approved direct tutorial');
assert.equal(rf148Ids.filter(id=>get(id)?.provider!=='youtube'||!get(id)?.videoId).length,0,'all 40 v1.4.8 exercises need direct videos');
assert.equal(get('incline-pushup').videoId,'0JUrOH--Kdk','existing named demos must not be blanket disabled');
assert.equal(get('db-ohp').provider,'youtube','existing named coach mapping preserved');
vm.runInContext("openDemo('unverified-other')",ctx);
assert.ok(modal.innerHTML.includes('Keep your back neutral.'));
assert.ok(!modal.innerHTML.includes('<iframe'),'unverified clip is never embedded');
assert.equal(opened.length,0,'no link opened without a click');
vm.runInContext("openDemo('db-ohp')",ctx);assert.deepEqual(baseCalls,['db-ohp']);
offline=true;vm.runInContext("openDemo('unverified-other')",ctx);
assert.ok(modal.innerHTML.includes('Offline'));assert.ok(!modal.innerHTML.includes('data-tp80-source'));
console.log('PASS #80 source provenance + restored direct videos + 40/40 v1.4.8 direct video coverage');