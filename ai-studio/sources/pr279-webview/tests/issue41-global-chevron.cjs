const assert=require('node:assert/strict'),fs=require('node:fs');
const index=fs.readFileSync('www/index.html','utf8'),css=fs.readFileSync('www/health-dashboard-168.css','utf8');
for(const marker of [
 '.tp-global-chevron{',
 '.tp-global-chevron::before{',
 '.tp-global-chevron-host::after{',
 'window.tpGlobalApplyChevrons=function',
 "'.tp1511-training summary i'",
 "'.tp168-status>b'",
 "'.tp169-pulse-summary>b'",
 'matteHealthBasic:true',
 'pulseSafeZone:true'
])assert.ok((index+css).includes(marker),'missing #41 marker: '+marker);
assert.ok(css.includes('html:not([data-tp-theme-family="vivid"]) body.tp168-health-view'),'basic Health matte rule missing');
assert.ok(css.includes('.tp169-pulse-summary>.tp5-trend-mini'),'pulse safe-zone rule missing');
console.log('PASS #41 static: global theme chevron + Health matte/vivid + pulse safe-zone.');
