'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{
 const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}
 fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data)});
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
  browser=await chromium.launch({args:['--no-sandbox']});const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];
  page.on('pageerror',error=>errors.push(error.message));page.on('dialog',dialog=>dialog.dismiss());
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>TrainPilotBoot.finished);
  await page.evaluate(()=>{
   const clone=x=>JSON.parse(JSON.stringify(x)),localDevice='11111111-1111-1111-1111-111111111111',remoteDevice='22222222-2222-2222-2222-222222222222',base=clone(syncData());base.themeAccent='classicBlue';
   const files=[],snapshots=new Map(),writes=[];
   const add=(id,device,data)=>{files.push({id,name:'repforge-sync-'+device+'-'+id+'.json',createdTime:new Date(Date.UTC(2026,9,11,10,files.length)).toISOString(),version:'1'});snapshots.set(id,{app:'RepForgeSync',schema:1,owner:'theme-owner',device,data:clone(data)})};
   add('old-blue',remoteDevice,base);
   window.Capacitor={isNativePlatform:()=>true,Plugins:{GoogleSync:{
    driveList:async()=>({files:clone(files)}),driveRead:async({id})=>({version:'1',data:JSON.stringify(snapshots.get(id))}),
    driveWrite:async({data})=>{const snap=JSON.parse(data),id='write-'+(writes.length+1);writes.push(snap);add(id,localDevice,snap.data);return {id,version:'1',verified:true}}
   }}};
   window.theme173={writes,remote(theme){const data=clone(base);data.themeAccent=theme;data.weights=[{id:'theme-remote-weight',kg:81,date:'2026-10-11T12:30:00Z'}];add('remote-'+files.length,remoteDevice,data)}};
   cloudProfile={sub:'theme-owner'};db.set('cloudDevice',localDevice);db.set('cloudBase:theme-owner',base);db.set('cloudPrefs',{drive:true,calendar:false});rf130SyncWorkoutPhotos=async()=>{};
   go('home');tp155R4OpenPanel('settings',document.activeElement);
  });
  await page.locator('[data-settings-group="appearance"] > summary').click();
  await page.locator('.tp162-theme-dropdown .tp-select-trigger').click();
  await page.locator('.tp155-theme-option[data-theme="classicGreen"]').click();
  assert.equal(await page.evaluate(()=>cloudTimer!==null),true,'actual theme selection schedules automatic sync');
  await page.evaluate(()=>{clearTimeout(cloudTimer);cloudTimer=null;db.set('cloudPrefs',{drive:false,calendar:false})});
  const theme=()=>page.evaluate(()=>({key:rf200ThemeKey(),accent:document.documentElement.style.getPropertyValue('--accent'),expected:RF200_THEMES[rf200ThemeKey()].accent,family:document.documentElement.dataset.tpThemeFamily}));
  await page.evaluate(()=>syncCloud(true));assert.equal((await theme()).key,'classicGreen');
  await page.evaluate(()=>{tp155R4ClosePanel(false);go('health')});
  await page.locator('.tp168-weight-summary').click();await page.locator('.tp169-weight-entry > summary').click();
  await page.locator('#tp168WeightKg').fill('79.4');await page.locator('.tp168-weight-save').click();
  await page.evaluate(()=>syncCloud(true));
  assert.equal((await theme()).key,'classicGreen','real measurement Save + Drive sync retains selected color');
  assert.equal((await theme()).accent,(await theme()).expected,'rendered CSS agrees with stored theme after sync');
  assert.equal((await theme()).family,'basic');
  assert.equal(await page.evaluate(()=>weights().some(row=>row.kg===79.4)),true,'measurement remains saved');
  await page.evaluate(()=>{theme173.remote('classicBlue')});await page.evaluate(()=>syncCloud(true));
  assert.equal((await theme()).key,'classicGreen','another device saving with its unchanged blue theme cannot revert the color');
  assert.equal(await page.evaluate(()=>weights().some(row=>row.id==='theme-remote-weight')),true,'remote data still arrives');
  await page.evaluate(()=>{theme173.remote('purple')});await page.evaluate(()=>syncCloud(true));
  assert.equal((await theme()).key,'purple','a genuinely changed remote color still syncs');
  assert.equal((await theme()).accent,(await theme()).expected);assert.equal((await theme()).family,'vivid');
  assert.equal(await page.evaluate(()=>makeBackup().themeAccent),'purple','file backup keeps the synchronized color');
  await page.reload();await page.waitForFunction(()=>TrainPilotBoot.finished);
  assert.equal((await theme()).key,'purple','the accepted color survives app restart');
  assert.equal((await theme()).accent,(await theme()).expected);assert.deepEqual(errors,[]);
  fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/theme-sync-173.png'});
  console.log('PASS #173 browser: real palette and measurement Save, automatic sync scheduling, repeated sync with old/new remote heads, CSS/family parity, real remote color, backup and restart');
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve))}
})().catch(error=>{console.error(error);process.exitCode=1});
