const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const src=fs.readFileSync('www/issue80-safe-video-sources.js','utf8');
const map={bad:{credit:'Zi workout',provider:'youtube',videoId:'ai'},generic:{credit:'YouTube – Demo',provider:'youtube',videoId:'x'},good:{credit:'ScottHermanFitness',provider:'youtube',videoId:'real'}};
const ctx=vm.createContext({window:{},encodeURIComponent,byId:id=>({en:id}),demoInfo:id=>map[id]||null});vm.runInContext(src,ctx);
assert.equal(ctx.demoInfo('bad').provider,null);assert.match(ctx.demoInfo('bad').source,/youtube\.com\/results/);
assert.equal(ctx.demoInfo('generic').provider,null);assert.equal(ctx.demoInfo('good').provider,'youtube');assert.equal(ctx.demoInfo('good').verified,true);
console.log('PASS #80 blocks Zi/generic unverified embeds and preserves named sources');