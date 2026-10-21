const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const app=fs.readFileSync('www/app.js','utf8'),index=fs.readFileSync('www/index.html','utf8');
const pkg=require('../package.json'),meta=require('../SOURCE_VERSION.json');
const gradle=fs.readFileSync('android/app/build.gradle','utf8'),sw=fs.readFileSync('www/sw.js','utf8');
assert.equal(meta.version,pkg.version);assert.equal(meta.displayVersion,pkg.version);assert.ok(Number.isSafeInteger(meta.versionCode)&&meta.versionCode>0);
assert.match(gradle,new RegExp('versionCode\\s+'+meta.versionCode+'\\b'));assert.ok(gradle.includes('versionName "'+pkg.version+'"'));assert.match(sw,/trainpilot-v1371/);
assert.deepEqual([...index.matchAll(/<script[^>]*src="([^"]+)"/g)].map(m=>m[1]),['app.js']);
assert.equal(fs.readFileSync('www/runtime-manifest.txt','utf8').trim(),'app.js');
assert.ok(app.includes("const TP120_VERSION='1.2.0'"),'1.2 clean runtime marker missing');
assert.ok(!app.includes('rf112GoBase'),'1.1.2 route wrapper still present');
assert.ok(!app.includes('tp-route-switching #app'),'route paint mask still present');
assert.ok(!app.includes('frame(()=>frame(reveal))'),'double-frame legacy reveal still present');
assert.ok(!app.includes('new MutationObserver'),'legacy async DOM observer must be removed from clean runtime');
assert.ok(app.includes('shell=function(content){'),'direct final shell missing');
assert.ok(app.includes('One route => one #app innerHTML write'),'direct render marker missing');
assert.ok(app.includes('No route-mask and no nested go() wrapper chain'),'direct router marker missing');

const values=new Map();let writes=0,html='';
const root={get innerHTML(){return html},set innerHTML(v){writes++;html=v}};
const classList={add(){},remove(){},contains(){return false},toggle(){}};
const node=()=>({innerHTML:'',textContent:'',style:{setProperty(){}},dataset:{},classList:{...classList},children:[],parentNode:null,querySelector:()=>null,querySelectorAll:()=>[],appendChild(){},prepend(){},insertBefore(){},insertAdjacentElement(){},insertAdjacentHTML(){},remove(){},setAttribute(){},removeAttribute(){},closest(){return null},addEventListener(){},focus(){}});
const documentElement={...node(),classList:{...classList},scrollTop:0};
const body=node(),head=node();
const doc={hidden:false,documentElement,body,head,querySelector:s=>s==='#app'?root:null,querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){},createElement:node};
let tasks=[],cloudChecks=0;
const ctx=vm.createContext({localStorage:{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},document:doc,window:{Capacitor:{isNativePlatform:()=>true,Plugins:{GoogleSync:{status:async()=>{cloudChecks++;return {profile:null}}}}},addEventListener(){},scrollTo(){},open(){}},navigator:{onLine:true,languages:['hu-HU'],language:'hu-HU'},performance:{now:()=>1},requestAnimationFrame:fn=>{fn();return 1},setInterval:()=>1,clearInterval(){},setTimeout:(fn,ms)=>{if(!ms)tasks.push(fn);return 1},clearTimeout(){},Date,crypto:require('node:crypto').webcrypto,TextEncoder,Blob,URL,alert(){},confirm:()=>true,prompt:()=>null,console});
vm.runInContext(app,ctx,{filename:'app.js'});
const run=s=>vm.runInContext(s,ctx);
assert.equal(run('makeBackup().appVersion'),require('../package.json').version);
assert.equal(writes,1,'startup must have exactly one app-root write');
for(const route of ['home','plan','programs','history','health']){
 const before=writes;run(`go('${route}')`);assert.equal(writes,before+1,`${route} full page must render the app root exactly once`);assert.ok(html.includes('<main'),`${route} main screen missing`);
}
for(const route of ['calendar','settings']){
 const before=writes;assert.equal(run(`go('${route}')`),true,`${route} must open its Round 3 panel surface`);assert.equal(writes,before,`${route} panel must not replace the underlying app root`);run('window.tp155R4ClosePanel(false)');
}
{const before=writes;assert.equal(run('rf233CoachScreen()'),true,'Coach must open its Round 3 panel surface');assert.equal(writes,before,'Coach panel must not replace the underlying app root');run('window.tp155R4ClosePanel(false)')}
assert.equal(run("shell('<main>probe</main>').includes('rf208-nav-style')"),false,'nav CSS must not be injected into every shell render');
assert.equal(run("shell('<main>probe</main>').includes('TrainPilot')"),true);
assert.equal(run("typeof window.TrainPilotNavigate"),'function');
console.log('PASS 1.2 clean runtime: direct shell, single-write routes, no legacy route mask, metadata consistent.');

// Exercise the real startup/ready sections with an interactive document. A
// native restore may ask to finish before the DCL modules have been installed.
for(const pendingRestore of [false,true])for(const welcome of [false,true]){
 const callbacks=[],frames=[],stored=new Map();let rootWrites=0,inScope=false,scopes=0;
 if(pendingRestore)stored.set('repforge:healthRestore120','pending');
 const slot={innerHTML:''},startupDoc={...doc,readyState:'interactive',querySelector:s=>s==='#app'?slot:null};
 const startupWindow={Capacitor:{isNativePlatform:()=>false},addEventListener:(type,fn)=>{if(type==='DOMContentLoaded')callbacks.push(fn);}};
 const startupContext=vm.createContext({window:startupWindow,document:startupDoc,localStorage:{getItem:k=>stored.get(k)??null},
  render(){assert.equal(inScope,true,'first render must share reads across all final decorators');rootWrites++;slot.innerHTML='<main>final</main>';},
  performance:{now:()=>1},requestAnimationFrame:fn=>{frames.push(fn);return 1;},setTimeout:()=>1,clearTimeout(){},console});
 const section=name=>app.match(new RegExp('// @section '+name.replaceAll('.','\\.')+'\\n([\\s\\S]*?)// @endsection '+name.replaceAll('.','\\.')))[1];
 vm.runInContext(section('startup.js'),startupContext);vm.runInContext(section('ready.js'),startupContext);
 startupWindow.TrainPilotBoot.finish();assert.equal(rootWrites,0,'early native readiness cannot render before modules');
 startupWindow.tp7WithHistorySnapshot=fn=>{scopes++;inScope=true;try{return fn();}finally{inScope=false;}};
 if(welcome)startupWindow.TrainPilotBoot.renderInitialScreen=()=>{assert.equal(inScope,true);rootWrites++;slot.innerHTML='<main class="tp128-welcome">Welcome</main>';};
 callbacks.forEach(fn=>fn());assert.equal(rootWrites,pendingRestore?0:1,'DCL preserves the pending restore barrier');
 if(pendingRestore){stored.clear();startupWindow.TrainPilotBoot.finish();}
 assert.equal(rootWrites,1);assert.equal(scopes,1);assert.equal(inScope,false,'the first-render read scope is released');
 assert.equal(slot.innerHTML.includes('tp128-welcome'),welcome,'the selected first screen survives boot and restore');
 frames.forEach(fn=>fn());assert.equal(startupWindow.TrainPilotBoot.finished,true);
 startupWindow.TrainPilotBoot.finish();assert.equal(rootWrites,1,'repeated readiness never replays first render');
}
console.log('PASS cold startup module barrier: early native readiness, scoped final render, pending restore and idempotent reveal');
