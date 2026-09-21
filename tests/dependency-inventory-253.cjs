const fs=require('fs');
const path=require('path');
const assert=require('node:assert/strict');
const www=path.join(process.cwd(),'www');
const manifest=fs.readFileSync(path.join(www,'runtime-manifest.txt'),'utf8').trim().split(/\r?\n/).filter(Boolean);
assert.equal(new Set(manifest).size,manifest.length,'duplicate runtime entry');
for(const file of manifest){assert.ok(fs.existsSync(path.join(www,file)),'missing runtime file '+file);}
const html=fs.readFileSync(path.join(www,'index.html'),'utf8');
const scripts=[];
for(const m of html.matchAll(/<script\b[^>]*\bsrc="([^"]+)"[^>]*>/g)){scripts.push(m[1]);}
assert.deepEqual(scripts,manifest,'index script order differs from runtime manifest');
const refs=[];
for(const m of html.matchAll(/(?:src|href)="([^"]+)"/g)){refs.push(m[1]);}
for(const ref of refs){if(ref.startsWith('http:')||ref.startsWith('https:')||ref.startsWith('data:')||ref.startsWith('#'))continue;assert.ok(fs.existsSync(path.join(www,ref)),'missing local asset '+ref);}
assert.equal(manifest[0],'startup.js','startup.js must remain first');
assert.ok(manifest.indexOf('v252.js')<manifest.indexOf('v253.js'),'v253 must load after v252');
assert.equal(manifest[manifest.length-1],'ready.js','ready.js must be final');
console.log('PASS dependency inventory: '+manifest.length+' ordered runtime scripts verified.');
