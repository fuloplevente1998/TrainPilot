'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
  fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(data)});
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:360,height:800},timezoneId:'Europe/Budapest',locale:'hu-HU'}),errors=[];
  page.on('pageerror',error=>errors.push(error.message));
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(()=>{
   db.set('history',[{id:'health-energy',workout:'A',dayId:'A',programName:'Kalória teszt',started:'2026-10-07T22:42:00Z',finished:'2026-10-08T00:08:00Z',exercises:[],activeIntervals:[{start:'2026-10-07T22:42:00Z',end:'2026-10-07T23:00:00Z'},{start:'2026-10-07T23:15:00Z',end:'2026-10-08T00:08:00Z'}]}]);
   rfHistoryHealthCache.clear();window.energyCalls=[];
   healthPlugin=()=>({readTrainingWindow:async args=>{energyCalls.push(args);const first=args.start==='2026-10-07T22:42:00.000Z';return {workoutEnergyVersion:1,workoutCalories:first?112:280,workoutEnergyCoverageMs:(first?14:40)*60000,workoutEnergyProrated:false,workoutEnergySources:['com.sec.android.app.shealth'],workoutEnergySourceLabels:{'com.sec.android.app.shealth':'Samsung Health'},workoutEnergyTypes:['total'],activeCalories:first?10:16,totalCalories:first?120:253,averageHeartRate:111,maxHeartRate:162,heartRateSamples:first?1000:1945,sources:['com.sec.android.app.shealth'],sourceLabels:{'com.sec.android.app.shealth':'Samsung Health'},warnings:[]}}});
   go('history');
  });
  await page.locator('details.rf263-history > summary').first().click();
  await page.waitForFunction(()=>document.querySelector('[data-rf-history-health]')?.textContent.includes('392'));
  const stats=await page.locator('.rf-history-health-grid .stat').evaluateAll(nodes=>Object.fromEntries(nodes.map(node=>[node.querySelector('small').textContent,node.querySelector('strong').textContent])));
  assert.equal(stats['Edzéskalória'],'392 kcal');assert.equal(stats['Aktív kalória'],'26 kcal');assert.equal(stats['Összes energia (nyugalmival)'],'373 kcal');assert.equal(stats['Átlag / max. pulzus'],'111 / 162 bpm');
  const stored=await page.evaluate(()=>history()[0].health240);
  assert.equal(stored.workoutCalories,392);assert.equal(stored.maxHeartRate,162);assert.equal(stored.workoutEnergyVersion,1);
  assert.equal(await page.evaluate(()=>energyCalls.length),2);
  assert.match(await page.locator('[data-rf-history-health]').innerText(),/Samsung Health/);
  // Persisted attachment renders after cache clear; a missing energy record never
  // borrows active/day calories or fabricates zero for the workout.
  await page.evaluate(()=>rfHistoryHealthCache.clear());
  assert.match(await page.evaluate(()=>rfHistoryHealthHtml(0)),/392 kcal/);
  await page.evaluate(async()=>{healthPlugin=()=>({readTrainingWindow:async()=>({workoutEnergyVersion:1,workoutCalories:null,activeCalories:26,totalCalories:373,maxHeartRate:162})});await healthFromHistory(0)});
  assert.doesNotMatch(await page.locator('.rf-history-health-grid').innerText(),/Edzéskalória/);
  assert.match(await page.locator('[data-rf-history-health]').innerText(),/nincs megosztott, illeszthető kalóriarekord/);
  assert.equal(await page.evaluate(()=>history()[0].health240.workoutCalories),null);
  assert.deepEqual(errors,[]);
  console.log('PASS phone workout calories: inline refresh reads exact paused/midnight windows, persists392 separately from active26/total373, retains maxHR162, missing calories stay missing');
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1});
