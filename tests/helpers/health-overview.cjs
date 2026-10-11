'use strict';
const assert=require('node:assert/strict');

module.exports=async function assertHealthOverview(page,label){
 await page.evaluate(async()=>{await document.fonts.ready;window.scrollTo(0,0)});
 await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
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
  assert.ok(await page.evaluate(()=>scrollY)>0,label+' can actually scroll to overflowing content');
 }
 await page.locator('.tp168-health-note').evaluate(node=>node.scrollIntoView({block:'end',behavior:'instant'}));
 await page.waitForFunction(()=>{const r=document.querySelector('.tp168-health-note')?.getBoundingClientRect();return r&&r.top>=0&&r.bottom<=innerHeight+1},null,{timeout:2000});
 assert.ok(await page.locator('.tp168-health-note').evaluate(node=>{
  const rect=node.getBoundingClientRect();return rect.top>=0&&rect.bottom<=innerHeight+1;
 }),label+' footer remains reachable');
 await page.evaluate(()=>window.scrollTo(0,0));
};
