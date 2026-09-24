const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const { chromium } = require('playwright');
const root = path.resolve('www');
const server = http.createServer((req, res) => {
  let name = new URL(req.url, 'http://local').pathname;
  if (name === '/') name = '/index.html';
  const file = path.resolve(root, '.' + name);
  if (!file.startsWith(root + path.sep)) { res.writeHead(403); res.end(); return; }
  fs.readFile(file, (error, bytes) => {
    if (error) { res.writeHead(404); res.end(); return; }
    res.setHeader('Content-Type', file.endsWith('.js') ? 'text/javascript' : file.endsWith('.css') ? 'text/css' : file.endsWith('.html') ? 'text/html' : 'text/plain');
    res.end(bytes);
  });
});

(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  let browser;
  try {
    browser = await chromium.launch({ headless: true, executablePath: process.env.TRAINPILOT_CHROMIUM || undefined, args: ['--no-sandbox'] });
    for (const width of [320, 360, 393, 412]) {
      const page = await browser.newPage({ viewport: { width, height: 873 }, locale: 'hu-HU', timezoneId: 'Europe/Budapest' });
      const errors = [];
      page.on('pageerror', error => errors.push(String(error?.stack || error)));
      page.on('dialog', dialog => dialog.accept());
      await page.goto('http://127.0.0.1:' + server.address().port + '/');
      await page.waitForFunction(() => window.TrainPilotBoot?.finished);
      for (const theme of ['classicBlue', 'blue']) {
        await page.evaluate(themeName => {
          window.tp155R4ClosePanel?.(false);
          state.session = null; state.workout = null;
          rf200SetTheme(themeName);
          go('home');
        }, theme);
        await page.waitForSelector('.top.tp154-nav-grid .tp154-nav-cell.active');
        const initial = await page.evaluate(() => {
          const top = document.querySelector('.top.tp154-nav-grid');
          const cells = [...top.querySelectorAll('.tp154-nav-cell')];
          const styles = cells.map(el => {
            const cs = getComputedStyle(el);
            const visuals = cs.transitionProperty.split(',').map(s => s.trim());
            const child = el.querySelector('.tp151-nav-icon,.tp151-nav-label,.tp154-nav-action-icon');
            return { visualTransitions: visuals.filter(x => ['all', 'background', 'background-color', 'border', 'border-color', 'border-top-color', 'box-shadow', 'color', 'filter', 'opacity'].includes(x)), childTransition: child ? getComputedStyle(child).transitionDuration : '0s', maxDuration: Math.max(...cs.transitionDuration.split(',').map(s => parseFloat(s) * (s.includes('ms') ? 1 : 1000))) };
          });
          return { count: cells.length, active: cells.filter(el => el.classList.contains('active')).length, styles, family: document.documentElement.dataset.tpThemeFamily };
        });
        assert.equal(initial.count, 8, width + ': all navigation buttons present');
        assert.equal(initial.active, 1, width + ': exactly one active on Home');
        assert.equal(initial.family, theme === 'blue' ? 'vivid' : 'basic', width + '/' + theme + ': expected theme family');
        assert.ok(initial.styles.every(s => !s.visualTransitions.length && s.maxDuration <= 100 && s.childTransition === '0s'), width + '/' + theme + ': active background/border/glow must not animate: ' + JSON.stringify(initial.styles));

        // The three floating panel launchers must look alike even when idle.
        const panelCards = await page.evaluate(() => {
          const top = document.querySelector('.top.tp154-nav-grid');
          const selectors = {
            calendar: '.tp151-nav-item[onclick*="calendar"]',
            coach: '.tp154-coach-action',
            settings: '.tp154-settings-action'
          };
          return Object.fromEntries(Object.entries(selectors).map(([name, selector]) => {
            const el = top.querySelector(selector), cs = getComputedStyle(el);
            return [name, {
              active: el.classList.contains('active'),
              background: cs.backgroundColor,
              image: cs.backgroundImage,
              borderColor: cs.borderTopColor,
              borderStyle: cs.borderTopStyle,
              borderWidth: cs.borderTopWidth,
              radius: cs.borderTopLeftRadius,
              shadow: cs.boxShadow,
              color: cs.color
            }];
          }));
        });
        assert.equal(panelCards.calendar.active, false, width + '/' + theme + ': Calendar idle on Home');
        assert.deepEqual(panelCards.calendar, panelCards.coach, width + '/' + theme + ': Calendar card must match Coach');
        assert.deepEqual(panelCards.calendar, panelCards.settings, width + '/' + theme + ': Calendar card must match Settings');

        // Compare immediately computed colors/shadows to the settled state. A
        // lingering transition on the old active button fails this assertion.
        const snap = () => page.evaluate(() => [...document.querySelectorAll('.top.tp154-nav-grid .tp154-nav-cell')].map(el => {
          const cs = getComputedStyle(el);
          return { selected: el.classList.contains('active'), background: cs.backgroundColor, image: cs.backgroundImage, border: cs.borderTopColor, shadow: cs.boxShadow, color: cs.color };
        }));
        for (const route of ['history', 'programs', 'plan', 'home', 'history']) {
          await page.locator('.top.tp154-nav-grid .tp151-nav-item[onclick*="' + route + '"]').click();
          assert.equal(await page.evaluate(() => state.tab), route, width + '/' + theme + ': route ' + route);
          const now = await snap();
          assert.equal(now.filter(s => s.selected).length, 1, width + '/' + theme + ': one selected route');
          await page.waitForTimeout(180);
          assert.deepEqual(await snap(), now, width + '/' + theme + ': no fade trail for route ' + route);
        }
        for (const panel of ['calendar', 'coach', 'settings']) {
          const selector = panel === 'calendar' ? '.tp151-nav-item[onclick*="calendar"]' : '.tp154-' + panel + '-action';
          await page.locator('.top.tp154-nav-grid ' + selector).click();
          assert.equal(await page.locator('#tp155R4PanelHost').getAttribute('data-panel'), panel, width + '/' + theme + ': panel ' + panel);
          if (panel === 'coach') {
            const coachClose = await page.evaluate(() => {
              const host = document.querySelector('#tp155R4PanelHost[data-panel="coach"]');
              const wrapper = host.querySelector('.tp155-r4-panel').getBoundingClientRect();
              const x = host.querySelector('.tp155-r4-panel-close').getBoundingClientRect();
              const card = host.querySelector('.tp151-coach-recommendation').getBoundingClientRect();
              return { outerTopGap: x.top - wrapper.top, innerBottomGap: card.top - x.bottom, position: getComputedStyle(host.querySelector('.tp155-r4-panel-close')).position };
            });
            assert.equal(coachClose.position, 'sticky', width + '/' + theme + ': Coach X retains scroll behavior');
            assert.ok(coachClose.outerTopGap >= 8 && coachClose.outerTopGap <= 16, width + '/' + theme + ': Coach X above original position with outer clearance ' + JSON.stringify(coachClose));
            assert.ok(coachClose.innerBottomGap >= 6, width + '/' + theme + ': Coach X clears the recommendation frame ' + JSON.stringify(coachClose));
          }
          const now = await snap();
          assert.equal(now.filter(s => s.selected).length, 1, width + '/' + theme + ': one selected panel');
          await page.waitForTimeout(180);
          assert.deepEqual(await snap(), now, width + '/' + theme + ': no fade trail for panel ' + panel);
        }
        await page.locator('.top.tp154-nav-grid .tp154-settings-action').click();
        assert.equal(await page.locator('#tp155R4PanelHost').count(), 0, width + '/' + theme + ': second tap closes panel');
        assert.equal((await snap()).filter(s => s.selected).length, 1, width + '/' + theme + ': underlying route is selected after panel close');
      }
      assert.deepEqual(errors, [], width + ': no page errors');
      await page.close();
    }
    console.log('PASS #56: instant nav visuals + matching Calendar/Coach/Settings cards, first/repeated page+panel changes, 320/360/393/412px, basic/neon');
  } finally { await browser?.close(); server.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
