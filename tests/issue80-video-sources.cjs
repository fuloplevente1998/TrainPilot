const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const all=fs.readFileSync('www/app.js','utf8');const m=all.match(/\/\/ @section issue80-safe-video-sources\.js\n([\s\S]*?)\/\/ @endsection issue80-safe-video-sources\.js/);assert.ok(m,'#80 section');
const map={bad:{credit:'Zi workout',provider:'youtube',videoId:'ai'},generic:{credit:'YouTube – Demo',provider:'youtube',videoId:'x'},good:{credit:'ScottHermanFitness',provider:'youtube',videoId:'real'}};
const ctx=vm.createContext({window:{},encodeURIComponent,byId:id=>({en:id}),demoInfo:id=>map[id]||null});vm.runInContext(m[1],ctx);
assert.equal(ctx.demoInfo('bad').provider,null);assert.match(ctx.demoInfo('bad').source,/youtube\.com\/results/);
assert.equal(ctx.demoInfo('generic').provider,null);assert.equal(ctx.demoInfo('good').provider,'youtube');assert.equal(ctx.demoInfo('good').verified,true);
console.log('PASS #80 blocks Zi/generic unverified embeds and preserves named sources');