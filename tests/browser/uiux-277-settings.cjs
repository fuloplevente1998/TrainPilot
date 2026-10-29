'use strict';
// PR #277 phase 3 — Settings structure must retain real controls and callbacks.
const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const html=fs.readFileSync(path.join(root,'index.html'),'utf8');
const settingsJs=fs.readFileSync(path.join(root,'uiux-277-settings.js'),'utf8');
assert.ok(html.includes('defer src="uiux-277-settings.js"'));
assert.ok(settingsJs.includes('settingsScreen=function'));
assert.ok(!/(?:^|\n)\s*(?:render|go)\s*=/.test(settingsJs),'No render/go wrapper');
const server=http.createServer((req,res)=>{
 let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';
 const f=path.resolve(root,'.'+p);
 if(!f.startsWith(root+path.sep)){res.writeHead(403);return res.end()}
 fs.readFile(f,(e,d)=>{if(e){res.writeHead(404);return res.end()}
  res.setHeader('Content-Type',f.endsWith('.js')?'text/javascript':f.endsWith('.css')?'text/css':f.endsWith('.html')?'text/html':'text/plain');res.end(d);
 });
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  for(const width of [320,360,393]){
   const page=await browser.newPage({viewport:{width,height:873},locale:'hu-HU'});
   const errors=[];page.on('pageerror',e=>errors.push(e.message));page.on('dialog',d=>d.accept());
   await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
   await page.goto('http://127.0.0.1:'+server.address().port+'/');
   await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
   for(const theme of ['classicBlue','blue']){
    await page.evaluate(t=>{rf200SetTheme(t);go('home');window.tp155R4OpenPanel('settings')},theme);
    await page.waitForSelector('#tp155R4PanelHost[data-panel="settings"] main.tp277-settings');
    const snapshot=await page.evaluate(()=>{
     const host=document.querySelector('#tp155R4PanelHost[data-panel="settings"]');
     const main=host.querySelector('main.tp277-settings');
     const nav=document.querySelector('.top.tp154-nav-grid');
     const actionButtons=[...main.querySelectorAll('.tp152-settings-backup .btn,.tp152-settings-profile .btn')];
     return {heading:[...main.querySelectorAll(':scope > .tp277-settings-heading')].map(x=>x.textContent),
      lang:!!main.querySelector('.rf212-language select'),theme:!!main.querySelector('.tp162-theme-setting .tp-select-trigger'),
      backup:!!main.querySelector('.tp152-settings-backup #importFile'),
      export:!!main.querySelector('.tp152-settings-backup button[onclick="exportData()"]'),
      restore:!!main.querySelector('.tp152-settings-backup button[onclick="chooseImport()"]'),
      data:!!main.querySelector('.tp105-data'),cloud:!!main.querySelector('#cloudStatus'),
      profile:!!main.querySelector('.tp152-settings-profile button[onclick="resetPlan()"]'),
      mode:!!main.querySelector('.tp197-mode-panel'),
      nav:nav?.querySelectorAll('.tp154-nav-cell').length,
      buttons:actionButtons.map(x=>x.getBoundingClientRect().height),
      viewportOverflow:document.documentElement.scrollWidth-document.documentElement.clientWidth,
      wearableNote:main.querySelectorAll('.tp277-wear-note').length};
    });
    assert.ok(snapshot.heading.length>=3,width+'/'+theme+': semantic section labels present');
    assert.ok(snapshot.lang&&snapshot.theme,width+'/'+theme+': language and theme controls remain');
    assert.ok(snapshot.backup&&snapshot.export&&snapshot.restore,width+'/'+theme+': original file backup and restore actions');
    assert.ok(snapshot.data&&snapshot.cloud&&snapshot.profile,width+'/'+theme+': data/Drive/profile stay present');
    assert.ok(snapshot.mode,width+'/'+theme+': current workout mode settings preserved');
    assert.equal(snapshot.nav,8,width+'/'+theme+': original 2x4 nav');
    assert.ok(snapshot.buttons.every(h=>h>=43),width+'/'+theme+': keep usable touch targets');
    assert.ok(snapshot.viewportOverflow<=2,width+'/'+theme+': settings panel viewport overflow');
    assert.equal(snapshot.wearableNote,0,'No fake watch status in browser/PWA');
    await page.evaluate(()=>window.tp155R4ClosePanel());
   }
   assert.deepEqual(errors,[],'browser errors '+width);
   await page.close();
  }
  // The existing Settings wrapper must honor all four requested languages.
  const page=await browser.newPage({viewport:{width:393,height:873}});
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port+'/');
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  const expected={hu:'Megjelenés és nyelv',en:'Appearance and language',de:'Darstellung und Sprache',ro:'Aspect și limbă'};
  for(const [language,title] of Object.entries(expected)){
   const first=await page.evaluate(language=>{
    rf212SetLang(language);window.tp155R4OpenPanel('settings');
    return document.querySelector('#tp155R4PanelHost main.tp277-settings .tp277-settings-heading')?.textContent;
   },language);
   assert.equal(first,title,language+': localized Settings heading');
   await page.evaluate(()=>window.tp155R4ClosePanel());
  }
  await page.close();
  console.log('PASS PR277 grouped Settings preserve language/theme/backup/cloud/data/profile and 2x4 navigation');
 }finally{await browser?.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exitCode=1});
