const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');

const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';
 const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
 fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}
  res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d);
 });
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'de-DE'});
  page.on('dialog',d=>d.accept());
  await page.goto('http://127.0.0.1:'+server.address().port+'/');
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(()=>{
   db.set('language','de');document.documentElement.lang=rf212Lang();
   db.set('activeProgramId','home-level2');startWorkout('A',null,'home-level2');
   const i=state.session.exercises.findIndex(e=>e.id==='side-plank');if(i<0)throw Error('side-plank missing');
   state.current=i;renderWorkout();
  });
  assert.equal(await page.locator('#tp1481SideToggle-left').count(),1,'localized left-side timer missing');
  assert.equal(await page.locator('#tp1481SideToggle-right').count(),1,'localized right-side timer missing');
  assert.equal(await page.locator('#rf110StopwatchToggle').count(),0,'legacy single timer must stay hidden for mp/oldal');
  const text=(await page.locator('#rf110Stopwatch').innerText()).replace(/\s+/g,' ');
  assert.match(text,/Linke Seite/);assert.match(text,/Rechte Seite/);
  assert.doesNotMatch(text,/Bal oldal|Jobb oldal|Másodperc|Nullázás/);
  const lowerLabels=await page.locator('main .tp153-set-row').first().locator('.tp164-side-set-label').allTextContents();
  assert.deepEqual(lowerLabels,['Linke Seite','Rechte Seite'],'lower bilateral fields must use the active app language');
  assert.equal(await page.locator('main .tp153-set-row').first().locator('.tp164-side-set-input').count(),2);
  const lowerOverflow=await page.locator('main .tp153-set-row').first().evaluate(row=>row.scrollWidth-row.clientWidth);
  assert.ok(lowerOverflow<=2,'localized bilateral set row must not overflow horizontally');
  console.log('PASS: 1.4.9 keeps the 1.4.8.1 per-side stopwatch and localizes its UI.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
