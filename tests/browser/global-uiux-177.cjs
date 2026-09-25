const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';
 const file=path.resolve(root,'.'+p);
 if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
 fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}
  res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d);
 });
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  for(const width of [320,360,393,412]){
   const page=await browser.newPage({viewport:{width,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'});
   const errors=[];page.on('pageerror',e=>errors.push(String(e?.stack||e)));page.on('dialog',d=>d.accept());
   await page.goto('http://127.0.0.1:'+server.address().port+'/');
   await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&!!window.TrainPilotGlobalUi177);
   for(const theme of ['classicBlue','blue']){
    await page.evaluate(t=>{state.session=null;state.workout=null;window.tp155R4ClosePanel?.(false);rf200SetTheme(t);go('home');},theme);
    const family=theme==='blue'?'vivid':'basic';
    assert.equal(await page.evaluate(()=>document.documentElement.dataset.tpThemeFamily),family);
    const tokens=await page.evaluate(()=>{
      const c=getComputedStyle(document.documentElement);
      return {bg:c.getPropertyValue('--tp-ui-bg').trim(),card:c.getPropertyValue('--tp-ui-card').trim(),border:c.getPropertyValue('--tp-ui-border').trim(),width:c.getPropertyValue('--tp-ui-border-size').trim()};
    });
    assert.deepEqual(tokens,{bg:'#0e1015',card:'#1a1e25',border:'#2c3440',width:'1px'});
    for(const route of ['home','plan','programs','history','health']){
      await page.evaluate(x=>go(x),route);
      const stateUi=await page.evaluate(()=>{
        const main=document.querySelector('#app main'),hero=[...main.querySelectorAll('.tp-ui-hero')],regular=main.querySelector('.card:not(.tp-ui-hero),.stat:not(.tp-ui-hero),.tp152-program-card:not(.tp-ui-hero)');
        const c=regular?getComputedStyle(regular):null,body=getComputedStyle(document.body);
        return {route:state.tab,hero:hero.length,bg:body.backgroundColor,surface:c?.backgroundColor,border:c?.borderTopColor,shadow:c?.boxShadow,heroShadow:hero[0]?getComputedStyle(hero[0]).boxShadow:null};
      });
      assert.equal(stateUi.route,route,width+'/'+theme+'/'+route+': route remains functional');
      assert.equal(stateUi.bg,'rgb(14, 16, 21)');
      assert.ok(stateUi.hero<=1,'Only one highlighted hero per route: '+JSON.stringify(stateUi));
      if(route==='health')assert.equal(stateUi.hero,1,'Health Today stays hero');
      if(route==='plan')assert.equal(stateUi.hero,1,'Plan main action stays hero');
      if(stateUi.surface){assert.equal(stateUi.surface,'rgb(26, 30, 37)',route+': shared surface');assert.equal(stateUi.border,'rgb(44, 52, 64)',route+': neutral border')}
      if(family==='basic'){assert.equal(stateUi.shadow,'none','Basic cards are matte');if(stateUi.hero)assert.equal(stateUi.heroShadow,'none','Basic hero has no glow')}
      if(family==='vivid'&&stateUi.hero)assert.notEqual(stateUi.heroShadow,'none','Vivid hero may glow');
    }
    await page.evaluate(()=>go('home'));
    let previousTab=await page.evaluate(()=>state.tab);
    let geom=[];
    for(const panel of ['calendar','coach','settings']){
      await page.evaluate(t=>window.tp155R4OpenPanel(t,document.activeElement),panel);
      const s=await page.evaluate(()=>{
        const host=document.querySelector('#tp155R4PanelHost'),p=host.querySelector('.tp155-r4-panel'),x=host.querySelector('.tp155-r4-panel-close'),c=getComputedStyle(x);
        return {panel:host.dataset.panel,width:x.getBoundingClientRect().width,height:x.getBoundingClientRect().height,radius:c.borderTopLeftRadius,background:c.backgroundColor,border:c.borderTopColor,color:c.color,top:x.getBoundingClientRect().top-p.getBoundingClientRect().top,hero:host.querySelectorAll('.tp-ui-hero').length};
      });
      assert.equal(s.panel,panel);
      assert.equal(s.width,36);assert.equal(s.height,36);
      assert.ok(s.top>=8&&s.top<=16,'Close X is safely inset: '+JSON.stringify(s));
      if(panel==='coach')assert.equal(s.hero,1,'Coach recommendation hero');
      geom.push(s);
      await page.evaluate(()=>window.tp155R4TogglePanel(document.querySelector('#tp155R4PanelHost').dataset.panel,document.activeElement));
      assert.equal(await page.locator('#tp155R4PanelHost').count(),0,'Retap closes '+panel);
      assert.equal(await page.evaluate(()=>state.tab),previousTab,'Underlying route retained');
      assert.equal(await page.evaluate(()=>document.activeElement?.tagName),'MAIN','Underlying main receives focus after retap');
    }
    for(const g of geom.slice(1)){
      const x=geom[0];for(const k of ['width','height','radius','background','border','color','top'])assert.equal(g[k],x[k],'All panel X geometry/tokens identical for '+k);
    }
    // Existing navigation selection remains instant and theme-aware.
    const transitions=await page.evaluate(()=>[...document.querySelectorAll('.top.tp154-nav-grid .tp154-nav-cell')].map(x=>getComputedStyle(x).transitionProperty));
    assert.ok(transitions.length===8&&transitions.every(x=>x==='transform'),'1.7.6 nav latency fix retained');
   }
   assert.deepEqual(errors,[],width+': no page errors');
   await page.close();
  }
  console.log('PASS #58 global Health-reference surfaces, hero count, basic/vivid glow, Programs borders, identical overlay X and toggle focus at 320/360/393/412px');
 }finally{await browser?.close();server.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
