'use strict';
const assert=require('node:assert/strict');

module.exports=async function assertHealthOverview(page,label){
 await page.evaluate(async()=>{await document.fonts.ready;window.scrollTo(0,0)});
 // Disclosure toggle/resize handlers can still change the fit classes after
 // two frames. Inspect one settled layout, then keep each scroll assertion
 // inside the awaited predicate instead of taking a second, racing snapshot.
 await page.evaluate(()=>new Promise((resolve,reject)=>{
  let previous='',stable=0,frames=0;
  const tick=()=>{
   const main=document.querySelector('#app main.tp168-health'),r=main?.getBoundingClientRect();
   const signature=[r?.height,main?.scrollHeight,document.body.className,innerHeight].join('|');
   stable=signature===previous?stable+1:0;previous=signature;
   if(stable>=6)return resolve();
   if(++frames>=120)return reject(new Error('Health overview layout did not settle'));
   requestAnimationFrame(tick);
  };requestAnimationFrame(tick);
 }));
 const layout=await page.evaluate(()=>{
  const main=document.querySelector('#app main.tp168-health');
  const clipped=[...main.querySelectorAll(':scope>details:not([open])')].filter(node=>{
   const row=node.querySelector('summary').getBoundingClientRect(),card=node.getBoundingClientRect();
   return row.top<card.top-1||row.bottom>card.bottom+1;
  }).map(node=>node.className);
  return {fixed:document.body.classList.contains('tp107-fixed-view'),clipped,
   footer:main.querySelector('.tp168-health-note').getBoundingClientRect().bottom,
   viewport:innerHeight,horizontal:document.documentElement.scrollWidth-innerWidth};
 });
 assert.deepEqual(layout.clipped,[],label+' retains complete disclosure headings');
 assert.ok(layout.horizontal<=1,label+' has no horizontal overflow');
 if(layout.fixed){
  assert.ok(layout.footer<=layout.viewport+1,label+' only locks scrolling when the footer fits');
 }else if(layout.footer>layout.viewport+1){
  await page.mouse.move(150,layout.viewport-50);await page.mouse.wheel(0,400);await page.waitForFunction(()=>scrollY>0,null,{timeout:2000});
 }
 await page.locator('.tp168-health-note').evaluate(node=>node.scrollIntoView({block:'end',behavior:'instant'}));
 await page.waitForFunction(()=>{const r=document.querySelector('.tp168-health-note')?.getBoundingClientRect();return r&&r.top>=0&&r.bottom<=innerHeight+1},null,{timeout:2000});
 await page.evaluate(()=>window.scrollTo(0,0));
};
