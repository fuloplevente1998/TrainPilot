'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright'),root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const url=new URL(req.url,'http://local'),name=url.pathname,file=path.resolve(root,'.'+(name==='/'?'/index.html':name));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}
 fs.readFile(file,(error,body)=>{if(error){res.writeHead(404);return res.end()}
 res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(body)});
});
(async()=>{
 await new Promise(done=>server.listen(0,'127.0.0.1',done));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox'],executablePath:process.env.TRAINPILOT_CHROMIUM||undefined});
  const page=await browser.newPage({viewport:{width:320,height:760},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));page.on('dialog',d=>d.accept());
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port+'/');
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(()=>{
   const custom={id:'pr279-compact-test',name:'Hosszú saját testsúlyos edzésprogram, különleges célokkal',
    location:'Otthon',level:'beginner',goal:'fitness',builtin:false,
    days:[{id:'C',name:'Saját nap',exercises:['goblet-squat']}],prescriptions:{}};
   db.set('programs',[...programs().filter(x=>x.id!==custom.id),custom]);
   go('programs');
  });
  let cards=page.locator('#app details.tp277-program-card');
  assert.ok(await cards.count()>=2,'Every program has the compact class');
  assert.equal(await cards.count(),await page.locator('#app details.tp277-program-card .tp277-program-icon svg').count(),'Every program has a distinct SVG icon slot');
  const card=cards.filter({hasText:'Hosszú saját testsúlyos edzésprogram'});
  assert.equal(await card.count(),1,'Custom program included');
  const icon=await page.evaluate(()=>tp277ProgramIcon({id:'bw',name:'Saját testsúlyos program'}));
  assert.match(icon,/circle cx="12"/,'Bodyweight program uses a person icon');
  assert.notEqual(icon,await page.evaluate(()=>tp277ProgramIcon({id:'run',name:'Futás'})),'Different program types use different icons');
  for(const width of [320,360,393]){
   await page.setViewportSize({width,height:760});
   const geometry=await card.evaluate(node=>{
    const summary=node.querySelector(':scope > summary'),info=summary.querySelector('.rf103-program-info');
    const button=summary.querySelector('.rf103-program-activate'),arrow=summary.querySelector('.tp152-program-chevron');
    const bound=x=>{const r=x.getBoundingClientRect();return {left:r.left,right:r.right,width:r.width,height:r.height}};
    return {screen:document.documentElement.scrollWidth,body:document.body.scrollWidth,
     summary:bound(summary),info:bound(info),button:bound(button),arrow:bound(arrow),
     icon:bound(summary.querySelector('.tp277-program-icon'))};
   });
   assert.ok(geometry.button.height>=44,JSON.stringify({width,geometry}));
   assert.ok(geometry.button.left>=geometry.info.right-2,'Activation follows name without overlap at '+width);
   assert.ok(geometry.arrow.left>=geometry.button.right-2,'Chevron is immediately right of activate at '+width);
   assert.ok(geometry.summary.right<=width+1,'Program card stays within viewport at '+width);
   assert.ok(geometry.body<=width+1,'No horizontal overflow at '+width);
  }
  assert.equal(await card.getAttribute('open'),null);
  await card.locator(':scope > summary .tp152-program-chevron').click();
  assert.notEqual(await card.getAttribute('open'),null,'Disclosure toggles without activating program');
  await card.locator(':scope > summary .tp152-program-chevron').click();
  assert.equal(await card.getAttribute('open'),null);
  await page.evaluate(()=>{window.tp279ProgramActivated=null;activateProgram=id=>{window.tp279ProgramActivated=id}});
  await card.locator('button.rf103-program-activate').click();
  assert.equal(await page.evaluate(()=>window.tp279ProgramActivated),'pr279-compact-test','Activation keeps the original handler');
  assert.equal(await card.getAttribute('open'),null,'Activation does not open the disclosure');
  assert.deepEqual(errors,[]);
  console.log('PASS PR279: every program uses compact CSS + type-specific icon, separate activation/chevron, custom long titles, 320–393px');
 }finally{await browser?.close();await new Promise(done=>server.close(done))}
})().catch(err=>{console.error(err);process.exitCode=1});