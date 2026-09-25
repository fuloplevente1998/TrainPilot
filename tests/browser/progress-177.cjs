'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';
 const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}
 fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}
  res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d);
 });
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e.message||e)));page.on('dialog',d=>d.accept());
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(()=>{
   db.set('language','hu');state.session=null;state.workout=null;db.set('draft',null);
   const day=(offset,hour=17)=>{const d=new Date();d.setHours(hour,0,0,0);d.setDate(d.getDate()+offset);return d.toISOString()};
   db.set('history',[
    {id:'tp177-recent',workout:'A',dayId:'A',programId:'home-basic',programName:'Teszt',started:day(-2,16),finished:day(-2,17),exercises:[
     {id:'db-floor-press',repUnit:'ism.',sets:[{set:1,weight:12,reps:10,done:true}]},
     {id:'one-arm-row',repUnit:'ism.',sets:[{set:1,weight:10,reps:8,done:true}]},
     {id:'side-plank',repUnit:'mp/oldal',sets:[{set:1,leftSeconds:36,rightSeconds:32,reps:'32',done:true}]}
    ],photos:[]},
    {id:'tp177-baseline',workout:'A',dayId:'A',programId:'home-basic',programName:'Teszt',started:day(-10,16),finished:day(-10,17),exercises:[
     {id:'db-floor-press',repUnit:'ism.',sets:[{set:1,weight:10,reps:10,done:true}]},
     {id:'one-arm-row',repUnit:'ism.',sets:[{set:1,weight:8,reps:8,done:true}]},
     {id:'side-plank',repUnit:'mp/oldal',sets:[{set:1,leftSeconds:30,rightSeconds:30,reps:'30',done:true}]}
    ],photos:[]}
   ]);
   go('history');
  });
  await page.waitForSelector('.tp177-journal-tabs');
  assert.equal(await page.locator('.tp177-journal-tabs button').count(),3,'Journal must expose Log / Statistics / Progress');
  assert.equal((await page.locator('.tp177-journal-tabs button').allInnerTexts()).join('|'),'Edzésnapló|Statisztikák|Fejlődés');
  await page.locator('.tp177-journal-tabs button').nth(2).click();
  await page.waitForSelector('main.tp177-progress');
  assert.equal(await page.locator('.tp177-metric').count(),4,'four progress metric cards');
  assert.equal(await page.locator('.tp177-muscle-row').count(),6,'six muscle group rows');
  assert.equal(await page.locator('.tp177-anatomy-svg').count(),1,'front/back anatomy schematic');
  const text=await page.locator('main.tp177-progress').innerText();
  assert.match(text,/Összes volumen/);assert.match(text,/Átlagos volumen/);
  assert.doesNotMatch(text,/Átlag.?intenzitás|8[,.]4\s*\/\s*10/i,'must not invent workout intensity');
  assert.equal(await page.locator('.tp177-periods button.active').innerText(),'3 hó');
  await page.locator('.tp177-periods button[data-period="7d"]').click();
  assert.equal(await page.locator('.tp177-chart-column').count(),7,'7-day trend needs seven vertical day columns');
  const geometry=await page.locator('.tp177-chart-column').evaluateAll(nodes=>nodes.map(n=>{
   const rail=n.querySelector('.tp177-bar-rail').getBoundingClientRect(),bar=n.querySelector('.tp177-bar').getBoundingClientRect();
   return {railH:rail.height,barW:bar.width,barH:bar.height,label:n.getAttribute('aria-label')};
  }));
  assert.ok(geometry.every(x=>x.railH>100&&x.barW<=40),'trend must render vertical columns, not horizontal bars');
  const nonZero=geometry.findIndex(x=>x.barH>0);assert.ok(nonZero>=0,'seeded recent workout must create a visible volume bar');
  await page.locator('.tp177-chart-column').nth(nonZero).click();await page.waitForSelector('#tp177Dialog .tp177-dialog-card');
  assert.match(await page.locator('#tp177Dialog').innerText(),/kg/);await page.locator('.tp177-dialog-close').click();
  await page.locator('.tp177-metric').first().click();await page.waitForSelector('#tp177Dialog');
  assert.match(await page.locator('#tp177Dialog').innerText(),/PR rekordok|Súly|Bal oldal|Jobb oldal/);await page.locator('.tp177-dialog-close').click();
  await page.locator('.tp177-metric').nth(1).click();await page.waitForSelector('#tp177StatsFocus');
  assert.equal(await page.locator('.tp177-journal-tabs button[aria-selected="true"]').innerText(),'Statisztikák');
  assert.match(await page.locator('#tp177StatsFocus').innerText(),/Edzések száma/);
  await page.locator('.tp177-journal-tabs button').nth(2).click();await page.waitForSelector('main.tp177-progress');
  await page.locator('.tp177-periods button[data-period="30d"]').click();
  assert.equal(await page.locator('.tp177-chart-column').count(),5,'30-day view groups into readable weekly columns');
  await page.locator('.tp177-periods button[data-period="3m"]').click();
  assert.equal(await page.locator('.tp177-chart-column').count(),13,'3-month view uses weekly vertical columns');
  for(const width of [320,393,412]){
   await page.setViewportSize({width,height:873});await page.waitForTimeout(20);
   const layout=await page.evaluate(()=>({body:document.documentElement.scrollWidth,viewport:document.documentElement.clientWidth,
    main:document.querySelector('main.tp177-progress')?.getBoundingClientRect().width,navActive:[...document.querySelectorAll('.top.tp154-nav-grid .tp151-nav-item.active')].map(x=>x.textContent.trim())}));
   assert.ok(layout.body<=layout.viewport+2,'Progress must not create page-level horizontal overflow at '+width);
   assert.ok(layout.main<=width,'Progress main must fit viewport at '+width);
  }
  assert.deepEqual(errors,[],'page errors: '+errors.join('\n'));
  console.log('PASS #60 Progress UI: 3 Journal tabs, vertical 7/30/3m chart, four cards, drilldowns, muscle view, no invented intensity, 320–412px.');
 }finally{await browser?.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
