const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
const listen=()=>new Promise(r=>server.listen(0,'127.0.0.1',r)),base=()=>'http://127.0.0.1:'+server.address().port+'/';
(async()=>{await listen();let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
 await page.goto(base());await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
 await page.evaluate(()=>{db.set('language','hu');state.tab='home';state.session=null;render();});
 await page.waitForFunction(()=>document.querySelector('#rf220CoachCard')?.classList.contains('tp166-home-coach'));
 const card=page.locator('#rf220CoachCard');
 assert.equal(await card.getAttribute('role'),'button');
 assert.equal(await card.getAttribute('tabindex'),'0');
 assert.equal(await card.locator('button').count(),0,'Home Coach must not contain separate action buttons');
 const huText=await card.innerText();assert.doesNotMatch(huText,/Részletek|Statisztikák/);
 assert.equal(await card.locator('.tp151-kicker').count(),1,'Home Coach must reuse Coach recommendation typography');
 assert.equal(await card.locator('.tp151-coach-target').count(),1,'Home Coach must reuse Coach target layout');
 const huOverflow=await card.evaluate(el=>el.scrollWidth>el.clientWidth+1);assert.equal(huOverflow,false,'Home Coach must not overflow horizontally');

 // A scroll-like pointer movement must not open Coach.
 const box=await card.boundingBox();assert.ok(box);
 await card.dispatchEvent('pointerdown',{clientX:box.x+30,clientY:box.y+25,pointerId:1,pointerType:'touch'});
 await card.dispatchEvent('pointermove',{clientX:box.x+30,clientY:box.y+70,pointerId:1,pointerType:'touch'});
 await card.dispatchEvent('click',{clientX:box.x+30,clientY:box.y+70});
 await page.waitForTimeout(30);
 assert.equal(await page.locator('#tp155R4PanelHost[data-panel="coach"]').count(),0,'scroll gesture must not open Coach');

 // A normal tap opens the same Coach panel used by the main navigation.
 await card.click();await page.waitForTimeout(80);
 assert.equal(await page.locator('#tp155R4PanelHost[data-panel="coach"]').count(),1,'whole Home Coach card must open Coach');
 assert.equal(await page.locator('#tp155R4PanelHost[data-panel="coach"] .tp151-coach-recommendation').count()>0,true,'Coach panel recommendation must remain available');
 await page.evaluate(()=>window.tp155R4ClosePanel?.(false));await page.waitForTimeout(30);

 // Journal statistics entry remains present.
 await page.evaluate(()=>go('history'));await page.waitForTimeout(60);
 const journalTabs=page.locator('.tp155-journal-tabs');assert.equal(await journalTabs.count(),1,'Journal tabs must remain');
 assert.match(await journalTabs.innerText(),/Statisztikák/);

 // Long German text at a narrow phone width must wrap without horizontal overflow.
 await page.setViewportSize({width:320,height:740});
 await page.evaluate(()=>{rf212SetLang('de');go('home');});await page.waitForTimeout(80);
 await page.waitForFunction(()=>document.querySelector('#rf220CoachCard')?.classList.contains('tp166-home-coach'));
 const deCard=page.locator('#rf220CoachCard');
 assert.equal(await deCard.locator('button').count(),0);
 assert.equal(await deCard.evaluate(el=>el.scrollWidth>el.clientWidth+1),false,'German Home Coach must fit 320px viewport');
 assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>document.documentElement.clientWidth+1),false,'page must have no horizontal overflow at 320px');

 console.log('PASS: #13 Home Coach uses Coach design, full-card tap, no extra buttons and responsive text.');
 await page.close();
}finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
