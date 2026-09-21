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
const listen=()=>new Promise(r=>server.listen(0,'127.0.0.1',r));
const base=()=>'http://127.0.0.1:'+server.address().port+'/';

(async()=>{
 await listen();let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
  page.on('dialog',d=>d.accept());
  await page.goto(base());await page.waitForFunction(()=>window.TrainPilotBoot?.finished);

  await page.evaluate(()=>{
   startWorkout(activeProgram().days[0].id);
   state.session.exercises[0].sets[0].reps='10';
   persistDraft();
   state.session=null;state.workout=null;state.tab='home';render();
  });

  assert.equal(await page.locator('button', {hasText:'Edzés folytatása'}).count(),1,'resume button must be visible on Home');
  await page.locator('button',{hasText:'Edzés folytatása'}).click();
  await page.waitForTimeout(80);

  const state=await page.evaluate(()=>({tab:state.tab,hasSession:!!state.session,workout:state.workout,reps:state.session?.exercises?.[0]?.sets?.[0]?.reps}));
  assert.equal(state.tab,'plan','resuming a draft must switch to Workout tab');
  assert.equal(state.hasSession,true,'draft session must be restored');
  assert.equal(state.reps,'10','draft set data must be preserved');

  const activeTab=await page.locator('.tab.active').innerText();
  assert.match(activeTab,/Edzés/i,'Workout tab must be visibly active after resume');
  assert.equal(await page.locator('text=Félbehagyott edzés').count(),0,'Home draft card must no longer remain visible');
  assert.ok(await page.locator('.progress').count()>0,'workout screen must be rendered');

  await page.close();
  console.log('PASS: 1.4.8.1 Home draft resume enters Workout tab and restores the active session.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
