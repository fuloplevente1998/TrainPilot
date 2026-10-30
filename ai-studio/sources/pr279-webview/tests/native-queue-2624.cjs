const assert=require('node:assert/strict'),fs=require('node:fs');
const g=fs.readFileSync('android/app/src/main/java/com/repforge/app/GoogleSyncPlugin.java','utf8');
assert.ok(g.includes('ExecutorService ioExecutor'));
assert.ok(g.includes('TrainPilot-GoogleSync'));
assert.ok(g.includes('ioExecutor.shutdownNow()'));
assert.ok(!g.includes('getBridge().execute(()->{try{'));
console.log('PASS 2628 GoogleSync network isolated from Capacitor bridge queue');
