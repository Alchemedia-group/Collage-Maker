/* Translates the demo's interface text. Text is looked up by its English wording in a dictionary built from the same
   tables as the Android app (i18n/*.py), so the demo and the app read the same in every language. */
(() => {
  'use strict';
  const D = /*__DICT__*/{};
  const NAMES = { en: 'English', es: 'Español', de: 'Deutsch', fr: 'Français', it: 'Italiano', pt: 'Português', hi: 'हिन्दी', id: 'Bahasa Indonesia' };
  const KEY = 'pc-lang';
  const esc = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  // places where one English word has a different translation depending on what it names
  const CTX = [['#edTextures', 'texture_'], ['#edFilters', 'filter_'], ['#edFrames', 'frame_'], ['#presetRow', 'preset_'], ['#gradRow', 'grad_'], ['#tplRow', 'tpl_'],
    ['#edSocial', 'social_'], ['#emojiCats', 'emoji_'], ['#fontCats', 'fontcat_'], ['#bCats, .lp-chips', 'lc_'], ['#dzShapes', 'shape_'], ['#dzTabs', 'dz_tab_']];
  const compile = (template) => {
    const parts = template.split(/\{(\d)\}/); // text, index, text, index ...
    const order = parts.filter((_, i) => i % 2).map(Number);
    return { re: new RegExp('^' + parts.map((p, i) => (i % 2 ? '(.+?)' : esc(p))).join('') + '$'), order };
  };
  const fmts = D.fmt.map((f) => ({ f, ...compile(f.en) }));
  const plurals = D.plurals.flatMap((p) => Object.entries(p.en).map(([q, tpl]) => ({ p, ...compile(tpl) })));
  let lang = 'en';
  const missing = new Set();

  const fill = (template, values) => template.replace(/\{(\d)\}/g, (_, i) => values[Number(i)] ?? '');
  function translate(text, el) {
    if (lang === 'en') return null;
    const t = text.trim();
    if (!t || /^[\d\s.,:;·×%°/+\-–—…()#]+$/.test(t)) return null;
    if (el) for (const [sel, prefix] of CTX) {
      if (D.ctx[prefix] && D.ctx[prefix][t] && el.closest && el.closest(sel)) return D.ctx[prefix][t][lang];
    }
    if (D.plain[t]) return D.plain[t][lang];
    for (const { f, re, order } of fmts) {
      const m = re.exec(t); if (!m) continue;
      const values = []; order.forEach((n, i) => { const raw = m[i + 1]; values[n] = (D.plain[raw] && D.plain[raw][lang]) || raw; });
      return fill(f[lang], values);
    }
    for (const { p, re, order } of plurals) {
      const m = re.exec(t); if (!m) continue;
      const n = Number(m[1]); const cat = new Intl.PluralRules(lang).select(n);
      const forms = p[lang]; return fill(forms[cat] || forms.other, [n]);
    }
    if (/[A-Za-z]{3,}/.test(t)) missing.add(t);
    return null;
  }

  const SKIP = 'script, style, textarea, input, select, [data-no-i18n], .guide, canvas';
  const sourceText = new WeakMap(); // text node -> the English it was last given
  const written = new WeakMap(); // text node -> what we last wrote into it
  function doNode(node) {
    const parent = node.parentElement; if (!parent || parent.closest(SKIP)) return;
    const cur = node.nodeValue;
    if (!written.has(node) || written.get(node) !== cur) sourceText.set(node, cur);
    const english = sourceText.get(node);
    const tr = translate(english, parent);
    const out = tr == null ? english : english.match(/^\s*/)[0] + tr + english.match(/\s*$/)[0];
    written.set(node, out);
    if (cur !== out) node.nodeValue = out;
  }
  const ATTRS = ['aria-label', 'title', 'placeholder'];
  function doAttrs(el) {
    if (el.closest && el.closest(SKIP)) return;
    el.__en = el.__en || {}; el.__w = el.__w || {};
    for (const a of ATTRS) {
      if (!el.hasAttribute(a)) continue;
      const cur = el.getAttribute(a);
      if (el.__w[a] !== cur) el.__en[a] = cur;
      const tr = translate(el.__en[a], el);
      const out = tr == null ? el.__en[a] : tr;
      el.__w[a] = out;
      if (cur !== out) el.setAttribute(a, out);
    }
  }
  function walk(root) {
    if (root.nodeType === 3) { doNode(root); return; }
    if (root.nodeType !== 1) return;
    if (root.closest(SKIP) && root.matches(SKIP)) return;
    doAttrs(root);
    const tw = document.createTreeWalker(root, NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT);
    let n; while ((n = tw.nextNode())) { if (n.nodeType === 3) doNode(n); else doAttrs(n); }
  }
  let busy = false;
  const observer = new MutationObserver((records) => {
    if (busy) return; busy = true;
    try {
      for (const r of records) {
        if (r.type === 'childList') r.addedNodes.forEach(walk);
        else if (r.type === 'characterData') doNode(r.target);
        else if (r.type === 'attributes') doAttrs(r.target);
      }
    } finally { busy = false; observer.takeRecords(); }
  });
  const $ = (id) => document.getElementById(id);
  function renderList() {
    const list = $('langList'); if (!list) return;
    list.textContent = '';
    const rows = [['', null], ...Object.entries(NAMES)];
    let stored = ''; try { stored = localStorage.getItem(KEY) || ''; } catch { /* ignore */ }
    rows.forEach(([code, name]) => {
      const b = document.createElement('button');
      b.type = 'button'; b.className = 'langrow'; b.setAttribute('role', 'radio');
      b.setAttribute('aria-checked', String(code ? stored === code : !stored));
      if (name) { b.setAttribute('data-no-i18n', ''); b.textContent = name; } else b.textContent = 'Phone language';
      b.addEventListener('click', () => { setLang(code || phoneLang(), code); const s = $('langScrim'); if (s) s.hidden = true; });
      list.append(b);
    });
  }
  const phoneLang = () => { const n = (navigator.language || 'en').slice(0, 2).toLowerCase(); return NAMES[n] ? n : 'en'; };
  /* choice is what the user picked ('' = follow the phone) */
  function setLang(next, choice) {
    if (!NAMES[next]) next = 'en';
    lang = next;
    try { if (choice !== undefined) { if (choice) localStorage.setItem(KEY, choice); else localStorage.removeItem(KEY); } } catch { /* storage unavailable */ }
    document.documentElement.lang = next;
    busy = true; walk(document.body); busy = false; observer.takeRecords();
    renderList();
  }
  function init() {
    let stored = '';
    try { stored = localStorage.getItem(KEY) || ''; } catch { /* ignore */ }
    observer.observe(document.body, { childList: true, subtree: true, characterData: true, attributes: true, attributeFilter: ATTRS });
    const open = $('homeLang'), scrim = $('langScrim');
    if (open && scrim) {
      open.addEventListener('click', () => { renderList(); scrim.hidden = false; });
      scrim.addEventListener('pointerdown', (e) => { if (e.target === scrim) scrim.hidden = true; });
      const close = $('langClose'); if (close) close.addEventListener('click', () => { scrim.hidden = true; });
    }
    setLang(NAMES[stored] ? stored : phoneLang(), undefined);
  }
  window.__i18n = { setLang: (l) => setLang(l, l), get lang() { return lang; }, missing: () => [...missing], clearMissing: () => missing.clear() };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else window.addEventListener('load', init);
})();
