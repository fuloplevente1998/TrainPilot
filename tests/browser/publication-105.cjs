const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
(async()=>{
 const root=path.resolve('www'),server=http.createServer((req,res)=>{const name=new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'),file=path.resolve(root,'.'+name);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);res.end();return;}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(data);});});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{browser=await chromium.launch({headless:true,args:['--no-sandbox']});
 for(const width of [320,360,393,412])for(const language of ['hu','en','de','ro']){
  const page=await browser.newPage({viewport:{width,height:800},locale:language});const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(lang=>{db.set('language',lang);go('settings');},language);
  await page.waitForSelector('.tp105-data');await page.locator('.tp105-data>summary').click();
  const result=await page.evaluate(()=>({text:document.querySelector('.tp105-data').textContent,checked:document.querySelector('.tp105-data input').checked,overflow:document.documentElement.scrollWidth>innerWidth+1,buttons:[...document.querySelectorAll('.tp105-data button')].map(x=>x.getAttribute('onclick'))}));
  assert.equal(result.checked,false,'health export must require consent');assert.equal(result.overflow,false,`${width}/${language} settings overflow`);assert.ok(result.buttons.includes('tp105Archive(false)'));assert.ok(result.buttons.includes('tp105Archive(true)'));assert.ok(result.text.includes(tpLabel(language)));
  await page.evaluate(()=>{window.tp155R4ClosePanel(false);go('history');go('home');rf200SetTheme('green');go('settings');});await page.waitForSelector('.tp105-data');assert.equal(await page.locator('.tp105-data input').isChecked(),false);assert.deepEqual(errors,[]);await page.close();
 }
 console.log('PASS publication UI: four languages at 320/360/393/412px, consent off, archive controls, navigation and theme re-entry');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exit(1)});
function tpLabel(lang){return {hu:'Teljes ZIP',en:'Full ZIP',de:'Vollständige ZIP',ro:'Copie ZIP'}[lang];}
