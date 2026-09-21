// TrainPilot 1.2 keeps broad compatibility regressions and validates the shipped runtime directly.
const fs=require('node:fs'),path=require('node:path'),os=require('node:os');
const {spawnSync}=require('node:child_process');
const root=path.resolve(__dirname,'..');process.chdir(root);
const source=fs.readFileSync('www/app.js','utf8');
const sections=[...source.matchAll(/^\/\/ @section ([\w.-]+)\n([\s\S]*?)^\/\/ @endsection \1$/gm)];
if(!sections.length)throw Error('Canonical source sections missing');
const temp=fs.mkdtempSync(path.join(os.tmpdir(),'trainpilot-test-'));
let fail=0,count=0;
const obsolete=new Set(['single-source.cjs','runtime-120.cjs','startup-232.cjs','v112-paint.cjs','v112-product.cjs','v260-release.cjs','v260-responsive.cjs','v261.cjs','v262.cjs','v130-photos.cjs','trainpilot-144.cjs']);
try{
 fs.mkdirSync(path.join(temp,'www'));
 for(const [,name,code] of sections)fs.writeFileSync(path.join(temp,'www',name),code);
 for(const name of fs.readdirSync('www'))if(name!=='app.js')fs.copyFileSync(path.join('www',name),path.join(temp,'www',name));
 const scripts=sections.map(([,name])=>`<script ${name==='startup.js'?'':'defer '}src="${name}"></script>`).join('\n');
 const index=fs.readFileSync('www/index.html','utf8').replace(/<script defer src="app.js"><\/script>/,scripts);
 fs.writeFileSync(path.join(temp,'www/index.html'),index);
 fs.writeFileSync(path.join(temp,'www/runtime-manifest.txt'),sections.map(x=>x[1]).join('\n')+'\n');
 for(const name of ['android','.github','package.json','SOURCE_VERSION.json','capacitor.config.json'])fs.symlinkSync(path.join(root,name),path.join(temp,name));
 fs.cpSync('tests',path.join(temp,'tests'),{recursive:true});
 for(const file of fs.readdirSync('tests').filter(x=>x.endsWith('.cjs')&&!obsolete.has(x)).sort()){
  const r=spawnSync(process.execPath,[path.join(temp,'tests',file)],{cwd:temp,encoding:'utf8'});count++;
  if(r.status!==0){fail++;process.stderr.write(`${file}\n${r.stdout}${r.stderr}`)}
 }
 for(const file of ['tests/single-source.cjs','tests/runtime-120.cjs','tests/v130-photos.cjs','tests/trainpilot-144.cjs']){
  const r=spawnSync(process.execPath,[file],{cwd:root,encoding:'utf8'});count++;process.stdout.write(r.stdout);if(r.status!==0){fail++;process.stderr.write(`${file}\n${r.stderr}`)}
 }
 console.log(`PASS=${count-fail} FAIL=${fail} (${sections.length} compatibility sections + direct 1.2 runtime)`);
 process.exitCode=fail?1:0;
}finally{fs.rmSync(temp,{recursive:true,force:true})}
