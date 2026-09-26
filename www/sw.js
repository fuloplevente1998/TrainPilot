const C='trainpilot-v177-ui2';
const CORE=['./','./index.html','./styles.css','./health-dashboard-168.css','./global-uiux-177.css','./progress-177.css','./progress-anatomy-177.webp','./manifest.json','./runtime-manifest.txt'];

async function runtimeAssets(){
  const r=await fetch('./runtime-manifest.txt',{cache:'no-store'});
  if(!r.ok)throw new Error(`runtime-manifest.txt: HTTP ${r.status}`);
  const text=await r.text();
  return text.split(/\r?\n/).map(x=>x.trim()).filter(x=>x&&!x.startsWith('#')).map(x=>`./${x}`);
}

self.addEventListener('install',e=>e.waitUntil((async()=>{
  const cache=await caches.open(C);
  await cache.addAll(CORE);
  const assets=await runtimeAssets();
  await cache.addAll([...new Set(assets)]);
  await self.skipWaiting();
})()));

self.addEventListener('activate',e=>e.waitUntil(
  caches.keys()
    .then(ks=>Promise.all(ks.filter(k=>(k.startsWith('repforge-')||k.startsWith('trainpilot-'))&&k!==C).map(k=>caches.delete(k))))
    .then(()=>self.clients.claim())
));

self.addEventListener('fetch',e=>{
  if(e.request.method!=='GET')return;
  e.respondWith(caches.match(e.request).then(r=>r||fetch(e.request)));
});
