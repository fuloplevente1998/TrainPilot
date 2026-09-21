const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');

(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
  fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)});
 });
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:650},locale:'hu-HU'});
  const nativeDialogs=[];
  page.on('dialog',async d=>{nativeDialogs.push({type:d.type(),message:d.message()});await d.dismiss()});
  await page.goto(`http://127.0.0.1:${server.address().port}`);
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.waitForTimeout(120);

  // A manually recorded weight must be visible on the Health hub even when
  // Health Connect has no body-weight value.
  await page.evaluate(()=>{
   db.set('weights',[{kg:66.5,date:new Date().toISOString()}]);
   state.health=state.health||{};state.health.wellness={};
   go('health');
  });
  await page.waitForTimeout(80);
  const fitness=page.locator('main .card').filter({has:page.locator('h2', {hasText:/Test és fittség|Body|Fitness/i})}).filter({hasText:/66[,.]5 kg/}).first();
  assert.equal(await fitness.count(),1,'Test/Fitness card with manual weight must exist');
  const fitnessText=await fitness.innerText();
  assert.match(fitnessText,/66[,.]5 kg/,'manual 66.5 kg must appear in Health');
  assert.match(fitnessText,/Kézi adat|Local entry|Lokaler Eintrag|Înregistrare locală|Kézi testsúlynapló|Manual weight log|Manuelles Gewichtsprotokoll|Jurnal manual de greutate/,'manual weight fallback must identify its source in the active language');

  // Home -> Help me get started is a full scrollable subpage, never the compact
  // non-scrolling Home dashboard.
  await page.evaluate(()=>go('home'));
  await page.waitForTimeout(80);
  await page.getByRole('button',{name:/Segíts elkezdeni|Help me get started/i}).click();
  await page.waitForTimeout(80);
  const profileState=await page.evaluate(()=>({
   tab:state.tab,
   homeClass:document.querySelector('main')?.classList.contains('rf221-home'),
   overflow:getComputedStyle(document.body).overflow,
   active:[...document.querySelectorAll('.tab.active')].map(x=>x.querySelector('.tp151-nav-label')?.textContent?.trim()||x.textContent.trim())
  }));
  assert.equal(profileState.tab,'profile');
  assert.equal(profileState.homeClass,false);
  assert.notEqual(profileState.overflow,'hidden');
  assert.ok(profileState.active.some(x=>/^(Kezdőlap|Home)$/.test(x)),'Home tab remains highlighted for profile');
  const profileScroll=await page.evaluate(async()=>{
   const main=document.querySelector('main'),probe=document.createElement('div');probe.id='tp2628ProfileScrollProbe';probe.style.height='1400px';main.appendChild(probe);window.scrollTo(0,9999);await new Promise(r=>setTimeout(r,50));return window.scrollY;
  });
  assert.ok(profileScroll>0,'Help me get started must scroll');

  // Scheduled workout deletion must use TrainPilot UI, not native WebView confirm.
  const scheduleId=await page.evaluate(()=>{
   const id=crypto.randomUUID(),start=new Date(Date.now()+86400000),end=new Date(start.getTime()+45*60000);
   db.set('scheduled',[{id,programId:'home-basic',dayId:'A',workout:'A',start:start.toISOString(),end:end.toISOString(),updatedAt:Date.now(),cancelled:false,status:'planned'}]);
   state.session=null;go('calendar');return id;
  });
  await page.waitForTimeout(60);
  await page.evaluate(id=>rf209DeleteSchedule(id),scheduleId);
  await page.locator('#tp2628Dialog').waitFor({state:'visible'});
  assert.match(await page.locator('#tp2628Dialog').innerText(),/Tervezett edzés törlése/);
  await page.locator('#tp2628Dialog [data-tp2628-cancel]').click();
  assert.equal(await page.evaluate(id=>scheduled().find(x=>x.id===id)?.cancelled,scheduleId),false,'Cancel must keep scheduled workout');
  await page.evaluate(id=>rf209DeleteSchedule(id),scheduleId);
  await page.locator('#tp2628Dialog').waitFor({state:'visible'});
  await page.locator('#tp2628Dialog [data-tp2628-confirm]').click();
  await page.waitForTimeout(30);
  assert.equal(await page.evaluate(id=>scheduled().find(x=>x.id===id)?.cancelled,scheduleId),true,'Confirm must cancel scheduled workout');

  // Draft deletion uses the themed confirmation, then a themed notice.
  await page.evaluate(()=>{state.session=null;db.set('draft',{session:{programName:'Teszt',workout:'A'},workout:'A',current:0});go('home')});
  await page.waitForTimeout(50);
  await page.evaluate(()=>discardDraft143());
  await page.locator('#tp2628Dialog').waitFor({state:'visible'});
  assert.match(await page.locator('#tp2628Dialog').innerText(),/Félbehagyott edzés törlése/);
  await page.locator('#tp2628Dialog [data-tp2628-confirm]').click();
  await page.waitForTimeout(30);
  assert.equal(await page.evaluate(()=>db.get('draft',null)),null);
  if(await page.locator('#tp2628Dialog [data-tp2628-ok]').count())await page.locator('#tp2628Dialog [data-tp2628-ok]').click();

  // Leaving an active workout must not navigate before confirmation.
  await page.evaluate(()=>{
   state.tab='plan';state.workout='A';state.session={workout:'A',programId:'home-basic',programName:'Teszt',dayId:'A',started:new Date().toISOString(),exercises:[]};
   go('home');
  });
  await page.locator('#tp2628Dialog').waitFor({state:'visible'});
  assert.equal(await page.evaluate(()=>state.tab),'plan','route must wait for explicit confirmation');
  assert.ok(await page.evaluate(()=>!!state.session),'session must remain before confirmation');
  assert.match(await page.locator('#tp2628Dialog').innerText(),/Edzés elhagyása/);
  await page.locator('#tp2628Dialog [data-tp2628-confirm]').click();
  await page.waitForTimeout(80);
  assert.equal(await page.evaluate(()=>state.tab),'home');
  assert.equal(await page.evaluate(()=>state.session),null);

  assert.deepEqual(nativeDialogs,[],'2628 regression paths must not open native WebView dialogs');
  console.log('PASS: 2628 manual Health weight, profile scrolling and themed dialogs.');
 }finally{
  if(browser)await browser.close();
  await new Promise(r=>server.close(r));
 }
})().catch(e=>{console.error(e);process.exit(1)});
