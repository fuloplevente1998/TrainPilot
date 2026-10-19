'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const { chromium } = require('playwright');

const root = path.resolve(__dirname, '../../www');
const server = http.createServer((req, res) => {
  let p = new URL(req.url, 'http://local').pathname;
  if (p === '/') p = '/index.html';
  const file = path.resolve(root, '.' + p);
  if (!file.startsWith(root + path.sep)) {
    res.writeHead(403);
    res.end();
    return;
  }
  fs.readFile(file, (e, d) => {
    if (e) {
      res.writeHead(404);
      res.end();
      return;
    }
    res.setHeader(
      'Content-Type',
      file.endsWith('.js')
        ? 'text/javascript'
        : file.endsWith('.css')
          ? 'text/css'
          : file.endsWith('.html')
            ? 'text/html'
            : 'text/plain'
    );
    res.end(d);
  });
});

async function bootPage(page, base) {
  await page.addInitScript(() => {
    try {
      localStorage.setItem('repforge:onboarding128', JSON.stringify('skipped'));
    } catch (_) {}
  });
  await page.goto(base);
  await page.waitForFunction(() => window.TrainPilotBoot?.finished);
  await page.evaluate(() => {
    window.quickWatchProbe = [];
    window.Capacitor = {
      isNativePlatform: () => true,
      Plugins: {
        WearSync: {
          publish: async () => ({ revision: 1 }),
          publishHome: async () => ({}),
          pendingCommands: async () => ({ commands: [] }),
          ackCommand: async () => {},
          setWorkoutPause: async () => {},
          makeSnapshot: () => ({ workoutId: 'test-snapshot' })
        }
      }
    };
  });
}

(async () => {
  await new Promise(r => server.listen(0, '127.0.0.1', r));
  const base = 'http://127.0.0.1:' + server.address().port;
  let browser;

  try {
    browser = await chromium.launch({
      headless: true,
      executablePath: process.env.TRAINPILOT_CHROMIUM || undefined,
      args: ['--no-sandbox']
    });

    const viewports = [
      { name: 'mobile', width: 393, height: 873 },
      { name: 'web-desktop', width: 1024, height: 768 }
    ];

    for (const vp of viewports) {
      const page = await browser.newPage({
        viewport: { width: vp.width, height: vp.height },
        locale: 'hu-HU'
      });
      const errors = [];
      page.on('pageerror', e => errors.push(e?.stack || e?.message || String(e)));
      page.on('dialog', d => d.accept());

      await bootPage(page, base);

      // Reset state for test isolation
      await page.evaluate(() => {
        state.session = null;
        state.workout = null;
        db.set('draft', null);
        db.set('history', []);
        db.set('language', 'hu');
        document.documentElement.lang = 'hu';
        state.tab = 'home';
        render();
      });

      // =========================================================================
      // 1. EDZÉSINDÍTÁS (WORKOUT START)
      // =========================================================================
      // Navigate to Plan tab
      await page.evaluate(() => {
        go('plan');
      });
      await page.waitForFunction(() => state.tab === 'plan');

      // Expand the first planned workout day accordion in active program
      const daySummary = page.locator('.tp152-day summary').first();
      await daySummary.waitFor({ state: 'visible' });
      await daySummary.click();

      // Click the start day workout button
      const startBtn = page.locator('.tp152-start-day').first();
      await startBtn.waitFor({ state: 'visible' });
      await startBtn.click();

      // Verify active workout session is started and rendered
      await page.waitForFunction(() => !!state.session);
      assert.ok(await page.evaluate(() => state.session !== null), `[${vp.name}] Workout session must be active`);
      assert.ok(await page.locator('.tp153-workout-head, .tp16-workout-head, .hero').count() > 0, `[${vp.name}] Active workout head must be present`);
      assert.ok(await page.locator('.tp153-set-row .check').count() > 0, `[${vp.name}] Set check buttons must be rendered`);

      // Verify draft persistence
      const draftAfterStart = await page.evaluate(() => db.get('draft', null));
      assert.ok(draftAfterStart && draftAfterStart.session, `[${vp.name}] Active workout draft must be saved to storage`);

      // =========================================================================
      // 2. SOROZAT KÉSZRE JELÖLÉSE (SET MARK COMPLETED)
      // =========================================================================
      const firstCheck = page.locator('.tp153-set-row .check').first();
      assert.equal(await firstCheck.innerText(), 'OK', `[${vp.name}] Uncompleted set button text must be OK`);
      assert.equal(await firstCheck.evaluate(e => e.classList.contains('done')), false, `[${vp.name}] Initial set must not have .done class`);

      // Fill custom weight and reps
      const firstRow = page.locator('.tp153-set-row').first();
      const weightField = firstRow.locator('input[aria-label*="kg"], input[inputmode="decimal"]').first();
      const repsField = firstRow.locator('input[inputmode="numeric"]').first();
      if (await weightField.count() > 0) {
        await weightField.fill('24');
      }
      await repsField.fill('12');

      // Click to complete set
      await firstCheck.click();
      await page.waitForFunction(() => state.session?.exercises[0]?.sets[0]?.done === true);

      // Verify UI changes
      assert.equal(await firstCheck.innerText(), '✓', `[${vp.name}] Completed set button text must be checkmark`);
      assert.equal(await firstCheck.evaluate(e => e.classList.contains('done')), true, `[${vp.name}] Completed set must have .done class`);

      // Verify draft reflects completion
      const draftAfterSet = await page.evaluate(() => db.get('draft', null));
      assert.equal(draftAfterSet.session.exercises[0].sets[0].done, true, `[${vp.name}] Draft must record set as completed`);
      assert.equal(draftAfterSet.session.exercises[0].sets[0].reps, '12', `[${vp.name}] Draft must retain recorded reps`);

      // =========================================================================
      // 3. VISSZAVONÁS (#188 UNDO)
      // =========================================================================
      const undoBtn = page.locator('[data-tp188-undo]');
      await page.waitForFunction(() => !document.querySelector('[data-tp188-undo]')?.hidden);
      assert.equal(await undoBtn.isVisible(), true, `[${vp.name}] Undo button must be visible after completing a set`);
      assert.match(await undoBtn.innerText(), /Visszavonás|Undo/i, `[${vp.name}] Undo button text must contain undo label`);

      // Click undo
      await undoBtn.click();
      await page.waitForFunction(() => state.session?.exercises[0]?.sets[0]?.done === false);

      // Verify set is reverted
      assert.equal(await firstCheck.innerText(), 'OK', `[${vp.name}] Set check button text must revert to OK`);
      assert.equal(await firstCheck.evaluate(e => e.classList.contains('done')), false, `[${vp.name}] Reverted set must not have .done class`);
      const draftAfterUndo = await page.evaluate(() => db.get('draft', null));
      assert.equal(draftAfterUndo.session.exercises[0].sets[0].done, false, `[${vp.name}] Draft must reflect undone set`);

      // Verify undo button is hidden again (single-use inverse)
      await page.waitForFunction(() => !!document.querySelector('[data-tp188-undo]')?.hidden);
      assert.equal(await undoBtn.isVisible(), false, `[${vp.name}] Undo button must hide after reverting`);

      // Mark set done again for workout finish flow
      await repsField.fill('10');
      await firstCheck.click();
      await page.waitForFunction(() => state.session?.exercises[0]?.sets[0]?.done === true);

      // =========================================================================
      // 4. EDZÉSBEFEJEZÉS KÖZVETLENÜL A NAPLÓBA (FINISH WORKOUT DIRECTLY TO LOG)
      // =========================================================================
      // Mark all sets of all exercises as completed and go to the final exercise
      await page.evaluate(() => {
        for (const e of state.session.exercises) {
          for (const s of e.sets) {
            s.done = true;
            s.reps = '10';
          }
        }
        state.current = state.session.exercises.length - 1;
        renderWorkout();
      });

      const finishBtn = page.locator('.tp16-workout-next');
      await finishBtn.waitFor({ state: 'visible' });
      assert.equal(await finishBtn.innerText(), 'Edzés befejezése', `[${vp.name}] Next button on final exercise must be Edzés befejezése`);
      await finishBtn.click();

      // Wait for session to finish and be persisted in history
      await page.waitForFunction(() => state.session === null && history().length >= 1);
      assert.equal(await page.evaluate(() => state.session), null, `[${vp.name}] Active session must be null after finish`);
      assert.equal(await page.evaluate(() => db.get('draft', null)), null, `[${vp.name}] Draft must be cleared after finish`);

      // Verify history has the finished workout
      const savedHistory = await page.evaluate(() => history());
      assert.ok(savedHistory.length >= 1, `[${vp.name}] History must contain at least 1 saved workout`);
      const latestWorkout = savedHistory[0];
      assert.ok(latestWorkout.finished, `[${vp.name}] Finished workout must have a finished timestamp`);
      assert.ok(latestWorkout.exercises.length > 0, `[${vp.name}] Finished workout must have exercises`);

      // Post-workout feedback screen appears; submit or skip to return home
      await page.waitForSelector('.tp122-feedback');
      const ratingBtn = page.locator('.tp122-feedback button:has-text("Pont jó"), .tp122-feedback button:has-text("Just right")').first();
      if (await ratingBtn.count() > 0) {
        await ratingBtn.click();
      } else {
        await page.locator('.tp122-feedback button:has-text("Most kihagyom"), .tp122-feedback button:has-text("Skip")').first().click();
      }
      await page.waitForFunction(() => state.tab === 'home');

      // Navigate to Napló (Journal)
      const journalTab = page.locator('.tab:has-text("Napló"), .tab[onclick="go(\'history\')"]').first();
      await journalTab.click();
      await page.waitForFunction(() => state.tab === 'history');

      // Verify the workout is directly present in the Journal (Napló) list
      await page.waitForSelector('details.rf263-history');
      const journalItem = page.locator('details.rf263-history').first();
      assert.ok(await journalItem.count() > 0, `[${vp.name}] Journal must display completed workout row`);

      // Expand journal entry
      await journalItem.locator('summary').click();
      await page.waitForSelector('details.rf263-history[open] .tp3-workout-summary');
      const summaryText = await page.locator('.tp3-workout-summary').innerText();
      assert.match(summaryText, /Edzés összegzés/i, `[${vp.name}] Summary must contain Edzés összegzés`);
      assert.match(summaryText, /Mit edzettél\?/i, `[${vp.name}] Summary must contain Mit edzettél? section`);
      assert.ok(await page.locator('.tp3-summary-metrics > div').count() >= 4, `[${vp.name}] Summary metrics must be present`);

      // =========================================================================
      // 5. NYELVVÁLTÁS (LANGUAGE CHANGE)
      // =========================================================================
      // Open Settings (opens settings panel in modern TrainPilot)
      await page.evaluate(() => {
        go('settings');
      });

      const langSelect = page.locator('#tp155R4PanelHost .rf212-language select');
      await langSelect.waitFor({ state: 'visible' });

      // Change to English
      await langSelect.selectOption('en');
      await page.waitForFunction(() => document.documentElement.lang === 'en');
      assert.equal(await page.evaluate(() => db.get('language')), 'en', `[${vp.name}] Database language must be en`);
      assert.equal(await page.locator('#tp155R4PanelHost h1').first().innerText(), 'Settings', `[${vp.name}] Settings header must be translated to English`);

      const navTextEn = await page.locator('.tabs').innerText();
      assert.match(navTextEn, /Home/i, `[${vp.name}] Tabs in EN must contain Home`);
      assert.match(navTextEn, /Workout/i, `[${vp.name}] Tabs in EN must contain Workout`);
      assert.match(navTextEn, /Log/i, `[${vp.name}] Tabs in EN must contain Log`);

      // Change to German
      await langSelect.selectOption('de');
      await page.waitForFunction(() => document.documentElement.lang === 'de');
      assert.equal(await page.evaluate(() => db.get('language')), 'de', `[${vp.name}] Database language must be de`);
      assert.equal(await page.locator('#tp155R4PanelHost h1').first().innerText(), 'Einstellungen', `[${vp.name}] Settings header must be translated to German`);

      const navTextDe = await page.locator('.tabs').innerText();
      assert.match(navTextDe, /Start/i, `[${vp.name}] Tabs in DE must contain Start`);
      assert.match(navTextDe, /Training/i, `[${vp.name}] Tabs in DE must contain Training`);
      assert.match(navTextDe, /Protokoll/i, `[${vp.name}] Tabs in DE must contain Protokoll`);

      // Change to Romanian
      await langSelect.selectOption('ro');
      await page.waitForFunction(() => document.documentElement.lang === 'ro');
      assert.equal(await page.evaluate(() => db.get('language')), 'ro', `[${vp.name}] Database language must be ro`);
      assert.equal(await page.locator('#tp155R4PanelHost h1').first().innerText(), 'Setări', `[${vp.name}] Settings header must be translated to Romanian`);

      const navTextRo = await page.locator('.tabs').innerText();
      assert.match(navTextRo, /Acasă/i, `[${vp.name}] Tabs in RO must contain Acasă`);
      assert.match(navTextRo, /Antrenament/i, `[${vp.name}] Tabs in RO must contain Antrenament`);
      assert.match(navTextRo, /Jurnal/i, `[${vp.name}] Tabs in RO must contain Jurnal`);

      // Revert to Hungarian
      await langSelect.selectOption('hu');
      await page.waitForFunction(() => document.documentElement.lang === 'hu');
      assert.equal(await page.evaluate(() => db.get('language')), 'hu', `[${vp.name}] Database language must be hu`);
      assert.equal(await page.locator('#tp155R4PanelHost h1').first().innerText(), 'Beállítások', `[${vp.name}] Settings header must be translated to Hungarian`);

      // Verify no uncaught page errors occurred
      assert.deepEqual(errors, [], `[${vp.name}] Page errors should be empty`);

      await page.close();
    }

    console.log('PASS: UI & E2E automation for core flows (start, set complete, #188 undo, finish to log, language switch) on mobile & web');
  } finally {
    if (browser) await browser.close();
    await new Promise(r => server.close(r));
  }
})().catch(e => {
  console.error(e);
  process.exitCode = 1;
});
