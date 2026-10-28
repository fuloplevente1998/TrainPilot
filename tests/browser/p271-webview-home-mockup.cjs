'use strict';
// PR #271 P1a: first real mockup-inspired WebView Home styling.
// Exercise the original app/router/callbacks: never seed demo metrics.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const { chromium } = require('playwright');

const root = path.resolve('www');
const server = http.createServer((request, response) => {
  const name = new URL(request.url, 'http://local').pathname;
  const file = path.resolve(root, '.' + (name === '/' ? '/index.html' : name));
  if (!file.startsWith(root + path.sep)) { response.writeHead(403); response.end(); return; }
  fs.readFile(file, (error, bytes) => {
    if (error) { response.writeHead(404); response.end(); return; }
    response.setHeader('Content-Type', file.endsWith('.js') ? 'text/javascript' : file.endsWith('.css') ? 'text/css' : file.endsWith('.html') ? 'text/html' : 'text/plain');
    response.end(bytes);
  });
});

(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  let browser;
  try {
    browser = await chromium.launch({
      headless: true, executablePath: process.env.TRAINPILOT_CHROMIUM || undefined,
      args: ['--no-sandbox']
    });
    fs.mkdirSync('ui-evidence', { recursive: true });

    for (const width of [320, 393]) {
      const page = await browser.newPage({
        viewport: { width, height: width === 320 ? 740 : 873 },
        locale: 'hu-HU', timezoneId: 'Europe/Budapest'
      });
      const errors = [];
      page.on('pageerror', error => errors.push(String(error?.message || error)));
      page.on('dialog', dialog => dialog.accept());
      await page.addInitScript(() => {
        try { localStorage.setItem('repforge:onboarding128', JSON.stringify('skipped')); } catch (_) {}
      });
      await page.goto('http://127.0.0.1:' + server.address().port + '/');
      await page.waitForFunction(() => window.TrainPilotBoot?.finished);

      for (const theme of ['classicBlue', 'yellow']) {
        await page.evaluate(name => {
          window.tp155R4ClosePanel?.(false);
          state.session = null; state.workout = null;
          db.set('draft', null);
          rf200SetTheme(name);
          go('home');
        }, theme);
        await page.waitForSelector('main.rf221-home #rf223Today.tp5-home-today');

        const comparison = await page.evaluate(async () => {
          const link = document.querySelector('link[href="webview-mockup-p1.css"]');
          assertOrThrow(!!link, 'the scoped P1 stylesheet must be linked');
          const main = document.querySelector('#app main.rf221-home');
          const today = main.querySelector('#rf223Today');
          const tile = today.querySelector('.tp5-today-metric');
          const coach = main.querySelector('#rf220CoachCard');
          assertOrThrow(!!tile && !!coach, 'existing true Home data cards remain');
          const elements = [main, ...main.querySelectorAll(':scope > .hero,:scope > .card,:scope > .onboarding'), today, coach, tile];
          function snap() {
            return elements.map(el => {
              const r = el.getBoundingClientRect();
              return { x:r.x, y:r.y, w:r.width, h:r.height, scrollWidth:el.scrollWidth, scrollHeight:el.scrollHeight };
            });
          }
          link.disabled = true;
          await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
          const before = snap(), oldBorder = getComputedStyle(today).borderTopColor;
          link.disabled = false;
          await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
          const after = snap(), newBorder = getComputedStyle(today).borderTopColor;
          return {
            before, after, oldBorder, newBorder,
            tiles: today.querySelectorAll('.tp5-today-metric').length,
            navButtons: document.querySelectorAll('#app .top.tp154-nav-grid .tp154-nav-cell').length,
            noOverflow: document.documentElement.scrollWidth <= innerWidth + 1
          };
          function assertOrThrow(ok, message) { if (!ok) throw new Error(message); }
        });

        assert.deepEqual(comparison.after, comparison.before, width + '/' + theme + ': design changes appearance, not box geometry');
        assert.notEqual(comparison.newBorder, comparison.oldBorder, width + '/' + theme + ': themed metric-card treatment is active');
        assert.equal(comparison.tiles, 6, width + '/' + theme + ': all six real unified summary metrics remain');
        assert.equal(comparison.navButtons, 8, width + '/' + theme + ': no original routes removed during P1a');
        assert.equal(comparison.noOverflow, true, width + '/' + theme + ': no horizontal overflow');
        if (width === 393 && theme === 'yellow') {
          await page.screenshot({ path: 'ui-evidence/p271-home-first-mockup-layer-393.png' });
        }

        await page.evaluate(() => go('health'));
        assert.equal(await page.evaluate(() => state.tab), 'health');
        await page.evaluate(() => go('history'));
        assert.equal(await page.evaluate(() => state.tab), 'history');
        await page.evaluate(() => go('home'));
        assert.equal(await page.evaluate(() => state.tab), 'home');
      }
      assert.deepEqual(errors, [], width + ': no JavaScript runtime errors');
      await page.close();
    }
    console.log('PASS PR271 P1a: actual WebView Home visual layer, 6 live tiles, 8 legacy links, unchanged geometry, navigation and theme at 320/393 px.');
  } finally {
    await browser?.close();
    await new Promise(resolve => server.close(resolve));
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
