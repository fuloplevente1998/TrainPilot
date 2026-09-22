const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
const listen=()=>new Promise(r=>server.listen(0,'127.0.0.1',r)),base=()=>'http://127.0.0.1:'+server.address().port+'/';
(async()=>{await listen();let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
 const errors=[];page.on('pageerror',e=>errors.push(String(e?.stack||e)));page.on('console',m=>{if(m.type()==='error')errors.push('console: '+m.text())});
 await page.goto(base());await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
 await page.evaluate(()=>{
   const e=byId('side-plank'),now=new Date().toISOString();if(!e)throw Error('side-plank missing');
   db.set('history',[{programId:'home-level2',programName:'Otthoni A/B – Haladó',dayId:'A',workout:'A',started:now,finished:now,exercises:[{id:e.id,hu:e.hu,en:e.en,loadType:e.loadType,repUnit:e.repUnit,effort:'good',sets:[{set:1,weight:0,reps:'16',leftSeconds:20,rightSeconds:16,done:true}]}]}]);
   state.tab='home';state.session=null;render();
 });
 const nav=page.getByRole('button',{name:'Napló',exact:true}).first();assert.equal(await nav.count(),1,'Napló navigation button missing');
 await nav.click();await page.waitForTimeout(100);
 assert.equal(errors.length,0,'opening Journal raised runtime error: '+errors.join('\n'));
 assert.equal(await page.getByText('Edzésnapló',{exact:true}).count()>0,true,'Journal title must render after clicking Napló');
 const main=await page.locator('main').innerText();assert.match(main,/Oldalsó plank/);assert.match(main,/Bal 20 mp/);assert.match(main,/Jobb 16 mp/);
 console.log('PASS: Journal opens from navigation with bilateral side-plank history.');
 await page.close();
}finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
