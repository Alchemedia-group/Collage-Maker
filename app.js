(() => {
  'use strict';

  // ---------------------------------------------------------------- layouts
  // Cells are [x, y, w, h] rectangles in a unit square; they get stretched to
  // whatever aspect ratio is chosen.
  const grid = (cols, rows) => {
    const out = [];
    for (let j = 0; j < rows; j++) for (let i = 0; i < cols; i++) out.push([i / cols, j / rows, 1 / cols, 1 / rows]);
    return out;
  };
  const spans = (cols, rows, cells) =>
    cells.map(([x, y, w = 1, h = 1]) => [x / cols, y / rows, w / cols, h / rows]);

  const LAYOUTS = [
    { id: '1', name: '1 photo', cells: grid(1, 1) },
    { id: '2v', name: '2 side by side', cells: grid(2, 1) },
    { id: '2h', name: '2 stacked', cells: grid(1, 2) },
    { id: '3v', name: '3 columns', cells: grid(3, 1) },
    { id: '3h', name: '3 rows', cells: grid(1, 3) },
    { id: '4', name: '2 × 2', cells: grid(2, 2) },
    { id: 'l1r2', name: 'Big left, 2 right', cells: spans(2, 2, [[0, 0, 1, 2], [1, 0], [1, 1]]) },
    { id: 'l2r1', name: '2 left, big right', cells: spans(2, 2, [[0, 0], [0, 1], [1, 0, 1, 2]]) },
    { id: 't1b2', name: 'Big top, 2 below', cells: spans(2, 2, [[0, 0, 2, 1], [0, 1], [1, 1]]) },
    { id: 't2b1', name: '2 top, big below', cells: spans(2, 2, [[0, 0], [1, 0], [0, 1, 2, 1]]) },
    { id: 'l1r3', name: 'Big left, 3 right', cells: spans(3, 3, [[0, 0, 2, 3], [2, 0], [2, 1], [2, 2]]) },
    { id: 't1b3', name: 'Big top, 3 below', cells: spans(3, 2, [[0, 0, 3, 1], [0, 1], [1, 1], [2, 1]]) },
    { id: '3x2', name: '3 × 2', cells: grid(3, 2) },
    { id: '2x3', name: '2 × 3', cells: grid(2, 3) },
    { id: 'hero', name: 'Hero + 5', cells: spans(3, 3, [[0, 0, 2, 2], [2, 0], [2, 1], [0, 2], [1, 2], [2, 2]]) },
    {
      id: 'pinwheel', name: 'Pinwheel',
      cells: [[0, 0, .6, .4], [.6, 0, .4, .6], [.4, .6, .6, .4], [0, .4, .4, .6], [.4, .4, .2, .2]],
    },
    { id: '3x3', name: '3 × 3', cells: grid(3, 3) },
    { id: '4x3', name: '4 × 3', cells: grid(4, 3) },
  ];

  const RATIOS = [
    { id: '1:1', r: 1 },
    { id: '4:5', r: 4 / 5 },
    { id: '3:2', r: 3 / 2 },
    { id: '2:3', r: 2 / 3 },
    { id: '16:9', r: 16 / 9 },
    { id: '9:16', r: 9 / 16 },
  ];

  const SWATCHES = ['#ffffff', '#f6efe4', '#d9d9e3', '#1c1c28', '#000000', '#f7c6d0', '#bfe3d0', '#bcd4f6'];

  const MAX_ZOOM = 5;
  const MAX_EXPORT_PIXELS = 16384 * 16384 / 4; // stay well inside browser canvas limits

  // ------------------------------------------------------------------ state
  const emptyCell = () => ({ imgId: null, zoom: 1, px: 0, py: 0 });

  const state = {
    layoutId: '4',
    ratio: RATIOS[0],
    gap: 2,
    margin: 2,
    radius: 0,
    bg: '#ffffff',
    images: [],            // { id, bitmap, w, h, url, name }
    cells: grid(2, 2).map(emptyCell),
    selected: -1,
    swapping: false,
    dropTarget: -1,
  };
  let nextImageId = 1;

  const currentLayout = () => LAYOUTS.find((l) => l.id === state.layoutId);
  const imageById = (id) => state.images.find((i) => i.id === id);
  const clamp = (v, lo, hi) => Math.min(hi, Math.max(lo, v));

  // ------------------------------------------------------------ DOM lookups
  const $ = (id) => document.getElementById(id);
  const canvas = $('canvas');
  const ctx = canvas.getContext('2d');
  const stage = $('stage');

  // --------------------------------------------------------------- geometry
  function computeRects(W, H) {
    const short = Math.min(W, H);
    const m = (state.margin / 100) * short;
    const g = (state.gap / 100) * short;
    const cw = W - 2 * m;
    const ch = H - 2 * m;
    const eps = 1e-6;
    return currentLayout().cells.map(([x, y, w, h]) => {
      const l = x < eps ? 0 : g / 2;
      const t = y < eps ? 0 : g / 2;
      const r = x + w > 1 - eps ? 0 : g / 2;
      const b = y + h > 1 - eps ? 0 : g / 2;
      return {
        x: m + x * cw + l,
        y: m + y * ch + t,
        w: Math.max(1, w * cw - l - r),
        h: Math.max(1, h * ch - t - b),
      };
    });
  }

  function cornerRadius(rect) {
    return (state.radius / 100) * (Math.min(rect.w, rect.h) / 2);
  }

  function roundedRect(c, x, y, w, h, r) {
    r = Math.min(r, w / 2, h / 2);
    c.beginPath();
    if (r <= 0) { c.rect(x, y, w, h); return; }
    c.moveTo(x + r, y);
    c.arcTo(x + w, y, x + w, y + h, r);
    c.arcTo(x + w, y + h, x, y + h, r);
    c.arcTo(x, y + h, x, y, r);
    c.arcTo(x, y, x + w, y, r);
    c.closePath();
  }

  // Where the photo lands inside a cell, given zoom and pan.
  function photoBox(rect, cell, img) {
    const s = Math.max(rect.w / img.w, rect.h / img.h) * cell.zoom;
    const dw = img.w * s;
    const dh = img.h * s;
    const ox = (dw - rect.w) / 2;
    const oy = (dh - rect.h) / 2;
    // px/py of 0 centres the photo; ±1 pushes it until an edge meets the cell edge.
    return { dw, dh, ox, oy, dx: rect.x - ox + ox * cell.px, dy: rect.y - oy + oy * cell.py };
  }

  // -------------------------------------------------------------- rendering
  function render(c, W, H, ui) {
    c.fillStyle = state.bg;
    c.fillRect(0, 0, W, H);
    const rects = computeRects(W, H);
    const dpr = ui ? ui.dpr : 1;

    rects.forEach((rect, i) => {
      const cell = state.cells[i];
      const img = cell && cell.imgId != null ? imageById(cell.imgId) : null;
      const r = cornerRadius(rect);

      if (img) {
        c.save();
        roundedRect(c, rect.x, rect.y, rect.w, rect.h, r);
        c.clip();
        const b = photoBox(rect, cell, img);
        c.imageSmoothingQuality = 'high';
        c.drawImage(img.bitmap, b.dx, b.dy, b.dw, b.dh);
        c.restore();
      } else if (ui) {
        roundedRect(c, rect.x, rect.y, rect.w, rect.h, r);
        c.fillStyle = 'rgba(128,128,140,.18)';
        c.fill();
        c.save();
        c.setLineDash([6 * dpr, 5 * dpr]);
        c.lineWidth = 1.5 * dpr;
        c.strokeStyle = 'rgba(128,128,140,.7)';
        c.stroke();
        c.restore();
        const cx = rect.x + rect.w / 2;
        const cy = rect.y + rect.h / 2;
        const arm = Math.min(14 * dpr, Math.min(rect.w, rect.h) / 5);
        c.strokeStyle = 'rgba(128,128,140,.9)';
        c.lineWidth = 2 * dpr;
        c.lineCap = 'round';
        c.beginPath();
        c.moveTo(cx - arm, cy); c.lineTo(cx + arm, cy);
        c.moveTo(cx, cy - arm); c.lineTo(cx, cy + arm);
        c.stroke();
      }
    });

    if (!ui) return;
    const accent = getComputedStyle(document.documentElement).getPropertyValue('--accent').trim() || '#6d5efc';

    const outline = (i, width, dash) => {
      const rect = rects[i];
      if (!rect) return;
      c.save();
      roundedRect(c, rect.x, rect.y, rect.w, rect.h, cornerRadius(rect));
      c.lineWidth = width * dpr;
      c.strokeStyle = accent;
      if (dash) c.setLineDash([8 * dpr, 6 * dpr]);
      c.stroke();
      c.restore();
    };
    if (state.dropTarget >= 0) {
      const rect = rects[state.dropTarget];
      c.save();
      roundedRect(c, rect.x, rect.y, rect.w, rect.h, cornerRadius(rect));
      c.fillStyle = accent;
      c.globalAlpha = 0.28;
      c.fill();
      c.restore();
      outline(state.dropTarget, 3, false);
    }
    if (state.selected >= 0) outline(state.selected, 3, state.swapping);
  }

  let frame = 0;
  function redraw() {
    if (frame) return;
    frame = requestAnimationFrame(() => {
      frame = 0;
      const dpr = window.devicePixelRatio || 1;
      render(ctx, canvas.width, canvas.height, { dpr });
    });
  }

  function layoutCanvas() {
    const cs = getComputedStyle(stage);
    const availW = stage.clientWidth - parseFloat(cs.paddingLeft) - parseFloat(cs.paddingRight);
    const availH = stage.clientHeight - parseFloat(cs.paddingTop) - parseFloat(cs.paddingBottom);
    const ar = state.ratio.r;
    let w = availW;
    let h = w / ar;
    if (h > availH) { h = availH; w = h * ar; }
    w = Math.max(80, Math.floor(w));
    h = Math.max(80, Math.floor(h));
    const dpr = window.devicePixelRatio || 1;
    canvas.style.width = `${w}px`;
    canvas.style.height = `${h}px`;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
    redraw();
  }

  // ---------------------------------------------------------------- toasts
  let toastTimer = 0;
  function toast(msg) {
    const el = $('toast');
    el.textContent = msg;
    el.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove('show'), 3200);
  }

  // ------------------------------------------------------------ image intake
  function loadImageElement(url) {
    return new Promise((resolve, reject) => {
      const el = new Image();
      el.onload = () => resolve(el);
      el.onerror = () => reject(new Error('decode failed'));
      el.src = url;
    });
  }

  async function readFile(file) {
    const url = URL.createObjectURL(file);
    try {
      let bitmap;
      try {
        bitmap = await createImageBitmap(file);
      } catch {
        bitmap = await loadImageElement(url);
      }
      const w = bitmap.naturalWidth || bitmap.width;
      const h = bitmap.naturalHeight || bitmap.height;
      if (!w || !h) throw new Error('empty image');
      return { id: nextImageId++, bitmap, w, h, url, name: file.name };
    } catch (err) {
      URL.revokeObjectURL(url);
      throw err;
    }
  }

  // Adds files to the library. If `targetCell` is given, the first photo goes
  // there; every other new photo fills the next empty cell.
  async function addFiles(fileList, targetCell = -1) {
    const files = Array.from(fileList).filter((f) => f.type.startsWith('image/') || /\.(jpe?g|png|gif|webp|avif|bmp)$/i.test(f.name));
    if (!files.length) {
      if (fileList.length) toast('Those files are not images this browser can read.');
      return;
    }
    const added = [];
    const failed = [];
    for (const f of files) {
      try { added.push(await readFile(f)); } catch { failed.push(f.name); }
    }
    state.images.push(...added);
    added.forEach((img, i) => {
      if (i === 0 && targetCell >= 0 && state.cells[targetCell]) {
        state.cells[targetCell] = { ...emptyCell(), imgId: img.id };
      } else {
        const free = state.cells.findIndex((c) => c.imgId == null);
        if (free >= 0) state.cells[free] = { ...emptyCell(), imgId: img.id };
      }
    });
    if (failed.length) toast(`Couldn't read: ${failed.slice(0, 3).join(', ')}${failed.length > 3 ? '…' : ''}`);
    else if (added.length > state.cells.filter((c) => c.imgId != null).length) toast('Extra photos are in the tray. Pick a bigger layout or click one to swap it in.');
    refresh();
  }

  function removeImage(id) {
    const img = imageById(id);
    if (!img) return;
    state.images = state.images.filter((i) => i.id !== id);
    state.cells = state.cells.map((c) => (c.imgId === id ? emptyCell() : c));
    URL.revokeObjectURL(img.url);
    if (img.bitmap.close) img.bitmap.close();
    refresh();
  }

  function assignImage(cellIndex, imgId) {
    if (!state.cells[cellIndex]) return;
    state.cells[cellIndex] = { ...emptyCell(), imgId };
    state.selected = cellIndex;
    refresh();
  }

  // ---------------------------------------------------------------- actions
  function setLayout(id) {
    const old = state.cells;
    state.layoutId = id;
    const n = currentLayout().cells.length;
    state.cells = Array.from({ length: n }, (_, i) => old[i] || emptyCell());
    // Photos that lost their cell come back into any empty ones.
    const used = new Set(state.cells.map((c) => c.imgId));
    state.images.filter((img) => !used.has(img.id)).forEach((img) => {
      const free = state.cells.findIndex((c) => c.imgId == null);
      if (free >= 0) state.cells[free] = { ...emptyCell(), imgId: img.id };
    });
    if (state.selected >= n) state.selected = -1;
    state.swapping = false;
    refresh();
  }

  function shuffle() {
    if (!state.images.length) return;
    const ids = state.images.map((i) => i.id);
    for (let i = ids.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [ids[i], ids[j]] = [ids[j], ids[i]];
    }
    state.cells = state.cells.map((_, i) => (ids[i] != null ? { ...emptyCell(), imgId: ids[i] } : emptyCell()));
    refresh();
  }

  function swapCells(a, b) {
    [state.cells[a], state.cells[b]] = [state.cells[b], state.cells[a]];
  }

  function firstEmptyOrSelected() {
    if (state.selected >= 0) return state.selected;
    return state.cells.findIndex((c) => c.imgId == null);
  }

  // ---------------------------------------------------------------- export
  async function download() {
    if (!state.cells.some((c) => c.imgId != null)) {
      toast('Add some photos first.');
      return;
    }
    const long = Number($('size').value);
    const ar = state.ratio.r;
    let W = ar >= 1 ? long : Math.round(long * ar);
    let H = ar >= 1 ? Math.round(long / ar) : long;
    if (W * H > MAX_EXPORT_PIXELS) {
      const k = Math.sqrt(MAX_EXPORT_PIXELS / (W * H));
      W = Math.floor(W * k); H = Math.floor(H * k);
    }
    const out = document.createElement('canvas');
    out.width = W; out.height = H;
    render(out.getContext('2d'), W, H, null);
    const type = $('format').value;
    const blob = await new Promise((resolve) => out.toBlob(resolve, type, 0.93));
    if (!blob) { toast('Export failed. Try a smaller size.'); return; }
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `collage.${type === 'image/jpeg' ? 'jpg' : 'png'}`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(a.href), 10000);
    toast(`Saved ${W} × ${H} ${type === 'image/jpeg' ? 'JPEG' : 'PNG'}.`);
  }

  // --------------------------------------------------------------- UI sync
  function buildStaticUI() {
    // layouts
    const box = $('layouts');
    LAYOUTS.forEach((l) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'layout-btn';
      b.dataset.id = l.id;
      b.setAttribute('role', 'radio');
      b.title = l.name;
      b.setAttribute('aria-label', l.name);
      const rects = l.cells.map(([x, y, w, h]) =>
        `<rect x="${(2 + x * 36 + 1).toFixed(2)}" y="${(2 + y * 36 + 1).toFixed(2)}" width="${(w * 36 - 2).toFixed(2)}" height="${(h * 36 - 2).toFixed(2)}" rx="2" fill="currentColor"/>`).join('');
      b.innerHTML = `<svg viewBox="0 0 40 40" aria-hidden="true">${rects}</svg>`;
      b.addEventListener('click', () => setLayout(l.id));
      box.appendChild(b);
    });

    // ratios
    RATIOS.forEach((r) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'chip';
      b.dataset.id = r.id;
      b.textContent = r.id;
      b.setAttribute('role', 'radio');
      b.addEventListener('click', () => { state.ratio = r; layoutCanvas(); refresh(); });
      $('ratios').appendChild(b);
    });

    // background swatches
    SWATCHES.forEach((c) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'swatch';
      b.dataset.color = c;
      b.style.background = c;
      b.title = c;
      b.setAttribute('role', 'radio');
      b.setAttribute('aria-label', `Background ${c}`);
      b.addEventListener('click', () => { state.bg = c; refresh(); });
      $('swatches').appendChild(b);
    });
    const custom = document.createElement('label');
    custom.className = 'swatch swatch-custom';
    custom.title = 'Custom colour';
    custom.innerHTML = '<input type="color" id="customBg" aria-label="Custom background colour">';
    $('swatches').appendChild(custom);
    $('customBg').addEventListener('input', (e) => { state.bg = e.target.value; refresh(); });

    // sliders
    const bind = (id, key, fmt) => {
      const el = $(id);
      el.addEventListener('input', () => { state[key] = Number(el.value); refresh(); });
      el.fmt = fmt;
    };
    bind('gap', 'gap', (v) => `${v}%`);
    bind('margin', 'margin', (v) => `${v}%`);
    bind('radius', 'radius', (v) => `${v}%`);

    $('zoom').addEventListener('input', (e) => {
      const cell = state.cells[state.selected];
      if (!cell || cell.imgId == null) return;
      setZoom(state.selected, Number(e.target.value) / 100);
      refresh();
    });
  }

  function refresh() {
    document.querySelectorAll('.layout-btn').forEach((b) => b.setAttribute('aria-checked', String(b.dataset.id === state.layoutId)));
    document.querySelectorAll('.chip').forEach((b) => b.setAttribute('aria-checked', String(b.dataset.id === state.ratio.id)));
    document.querySelectorAll('.swatch[data-color]').forEach((b) => b.setAttribute('aria-checked', String(b.dataset.color === state.bg)));

    [['gap', state.gap], ['margin', state.margin], ['radius', state.radius]].forEach(([id, v]) => {
      $(id).value = v;
      $(`${id}Out`).textContent = `${v}%`;
    });
    if (/^#[0-9a-f]{6}$/i.test(state.bg)) $('customBg').value = state.bg;

    // cell panel
    const cell = state.cells[state.selected];
    const hasCell = !!cell;
    $('cellPanel').hidden = !hasCell;
    $('cellHint').hidden = hasCell;
    if (hasCell) {
      const filled = cell.imgId != null;
      $('zoom').disabled = !filled;
      $('zoom').value = Math.round(cell.zoom * 100);
      $('zoomOut').textContent = `${Math.round(cell.zoom * 100)}%`;
      $('resetCellBtn').disabled = !filled;
      $('clearCellBtn').disabled = !filled;
      $('swapBtn').disabled = !filled;
      $('swapBtn').setAttribute('aria-pressed', String(state.swapping));
      $('swapBtn').textContent = state.swapping ? 'Pick other cell…' : 'Swap with…';
    }

    // tray (rebuilt only when the photos or their usage change, so thumbnails don't flicker)
    const used = new Set(state.cells.map((c) => c.imgId));
    const trayKey = state.images.map((i) => `${i.id}${used.has(i.id) ? 'u' : ''}`).join(',');
    const list = $('trayList');
    if (trayKey !== list.dataset.key) {
      list.dataset.key = trayKey;
      buildTray(list, used);
    }
    const n = state.images.length;
    $('photoCount').textContent = n ? `(${n})` : '';
    $('clearAllBtn').hidden = n === 0;
    $('trayHint').hidden = n === 0;
    $('emptyCard').hidden = n > 0;

    // export info
    const long = Number($('size').value);
    const ar = state.ratio.r;
    const W = ar >= 1 ? long : Math.round(long * ar);
    const H = ar >= 1 ? Math.round(long / ar) : long;
    $('sizeInfo').textContent = `Output: ${W} × ${H} px`;

    redraw();
  }

  function buildTray(list, used) {
    list.textContent = '';
    state.images.forEach((img) => {
      const t = document.createElement('div');
      t.className = `thumb${used.has(img.id) ? ' used' : ''}`;
      t.draggable = true;
      t.tabIndex = 0;
      t.setAttribute('role', 'button');
      t.setAttribute('aria-label', `Use photo ${img.name}`);
      t.title = img.name;
      const im = document.createElement('img');
      im.src = img.url;
      im.alt = '';
      t.appendChild(im);
      const x = document.createElement('button');
      x.type = 'button';
      x.className = 'x';
      x.textContent = '×';
      x.setAttribute('aria-label', `Remove ${img.name}`);
      x.addEventListener('click', (e) => { e.stopPropagation(); removeImage(img.id); });
      t.appendChild(x);
      const use = () => {
        const target = firstEmptyOrSelected();
        if (target < 0) { toast('Select a cell first, then click a photo.'); return; }
        assignImage(target, img.id);
      };
      t.addEventListener('click', use);
      t.addEventListener('keydown', (e) => { if (e.target === t && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); use(); } });
      t.addEventListener('dragstart', (e) => { e.dataTransfer.setData('text/x-collage-img', String(img.id)); e.dataTransfer.effectAllowed = 'copy'; });
      list.appendChild(t);
    });
  }

  // ---------------------------------------------------------- canvas input
  function cellAt(clientX, clientY) {
    const box = canvas.getBoundingClientRect();
    const x = ((clientX - box.left) / box.width) * canvas.width;
    const y = ((clientY - box.top) / box.height) * canvas.height;
    const rects = computeRects(canvas.width, canvas.height);
    return rects.findIndex((r) => x >= r.x && x <= r.x + r.w && y >= r.y && y <= r.y + r.h);
  }

  function setZoom(i, zoom) {
    const cell = state.cells[i];
    cell.zoom = clamp(zoom, 1, MAX_ZOOM);
  }

  const pointers = new Map();
  let drag = null; // { cell, startPx, startPy, startX, startY }
  let pinch = null; // { cell, dist, zoom }

  function panFromDelta(i, dxCss, dyCss, start) {
    const cell = state.cells[i];
    const img = cell && imageById(cell.imgId);
    if (!img) return;
    const k = canvas.width / canvas.getBoundingClientRect().width;
    const rect = computeRects(canvas.width, canvas.height)[i];
    const b = photoBox(rect, cell, img);
    if (b.ox > 0.5) cell.px = clamp(start.px + (dxCss * k) / b.ox, -1, 1);
    if (b.oy > 0.5) cell.py = clamp(start.py + (dyCss * k) / b.oy, -1, 1);
  }

  canvas.addEventListener('pointerdown', (e) => {
    canvas.focus({ preventScroll: true });
    pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
    canvas.setPointerCapture(e.pointerId);

    if (pointers.size === 2 && drag) {
      const [p, q] = [...pointers.values()];
      pinch = { cell: drag.cell, dist: Math.hypot(p.x - q.x, p.y - q.y), zoom: state.cells[drag.cell].zoom };
      drag = null;
      return;
    }

    const i = cellAt(e.clientX, e.clientY);
    if (i < 0) { state.selected = -1; state.swapping = false; refresh(); return; }

    if ((e.shiftKey || state.swapping) && state.selected >= 0 && state.selected !== i) {
      swapCells(state.selected, i);
      state.selected = i;
      state.swapping = false;
      refresh();
      return;
    }
    state.swapping = false;
    state.selected = i;
    const cell = state.cells[i];
    if (cell.imgId != null) {
      drag = { cell: i, start: { px: cell.px, py: cell.py }, x: e.clientX, y: e.clientY };
      canvas.style.cursor = 'grabbing';
    }
    refresh();
  });

  canvas.addEventListener('pointermove', (e) => {
    if (pointers.has(e.pointerId)) pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
    if (pinch && pointers.size >= 2) {
      const [p, q] = [...pointers.values()];
      setZoom(pinch.cell, pinch.zoom * (Math.hypot(p.x - q.x, p.y - q.y) / pinch.dist));
      refresh();
    } else if (drag) {
      panFromDelta(drag.cell, e.clientX - drag.x, e.clientY - drag.y, drag.start);
      redraw();
    } else {
      const i = cellAt(e.clientX, e.clientY);
      canvas.style.cursor = i >= 0 && state.cells[i].imgId != null ? 'grab' : 'default';
    }
  });

  const endPointer = (e) => {
    pointers.delete(e.pointerId);
    if (pointers.size < 2) pinch = null;
    if (pointers.size === 0) { drag = null; canvas.style.cursor = 'default'; }
  };
  canvas.addEventListener('pointerup', endPointer);
  canvas.addEventListener('pointercancel', endPointer);

  canvas.addEventListener('wheel', (e) => {
    const i = cellAt(e.clientX, e.clientY);
    if (i < 0 || state.cells[i].imgId == null) return;
    e.preventDefault();
    state.selected = i;
    setZoom(i, state.cells[i].zoom * Math.exp(-e.deltaY * 0.0015));
    refresh();
  }, { passive: false });

  canvas.addEventListener('dblclick', (e) => {
    const i = cellAt(e.clientX, e.clientY);
    if (i >= 0) openCellPicker(i);
  });

  canvas.addEventListener('keydown', (e) => {
    const i = state.selected;
    if (i < 0) return;
    const cell = state.cells[i];
    if (e.key === 'Escape') { state.selected = -1; state.swapping = false; refresh(); return; }
    if (cell.imgId == null) return;
    const step = 0.08;
    const moves = { ArrowLeft: [-step, 0], ArrowRight: [step, 0], ArrowUp: [0, -step], ArrowDown: [0, step] };
    if (moves[e.key]) {
      e.preventDefault();
      cell.px = clamp(cell.px + moves[e.key][0], -1, 1);
      cell.py = clamp(cell.py + moves[e.key][1], -1, 1);
      redraw();
    } else if (e.key === '+' || e.key === '=') { setZoom(i, cell.zoom * 1.1); refresh(); }
    else if (e.key === '-') { setZoom(i, cell.zoom / 1.1); refresh(); }
    else if (e.key === 'Delete' || e.key === 'Backspace') { state.cells[i] = emptyCell(); refresh(); }
  });

  // drag & drop onto canvas (from the tray or from the desktop)
  canvas.addEventListener('dragover', (e) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';
    const i = cellAt(e.clientX, e.clientY);
    if (i !== state.dropTarget) { state.dropTarget = i; redraw(); }
  });
  canvas.addEventListener('dragleave', () => { state.dropTarget = -1; redraw(); });
  canvas.addEventListener('drop', (e) => {
    e.preventDefault();
    e.stopPropagation();
    hideDropOverlay();
    const i = cellAt(e.clientX, e.clientY);
    state.dropTarget = -1;
    const id = e.dataTransfer.getData('text/x-collage-img');
    if (id && i >= 0) assignImage(i, Number(id));
    else if (e.dataTransfer.files.length) addFiles(e.dataTransfer.files, i);
    else redraw();
  });

  // whole-window file drop
  let dragDepth = 0;
  const hasFiles = (e) => e.dataTransfer && Array.from(e.dataTransfer.types || []).includes('Files');
  const hideDropOverlay = () => { dragDepth = 0; $('dropOverlay').classList.remove('show'); };
  window.addEventListener('dragenter', (e) => { if (hasFiles(e)) { dragDepth++; $('dropOverlay').classList.add('show'); } });
  window.addEventListener('dragleave', (e) => { if (hasFiles(e) && --dragDepth <= 0) hideDropOverlay(); });
  window.addEventListener('dragover', (e) => { if (hasFiles(e)) e.preventDefault(); });
  window.addEventListener('drop', (e) => {
    if (!hasFiles(e)) return;
    e.preventDefault();
    hideDropOverlay();
    addFiles(e.dataTransfer.files);
  });

  // ----------------------------------------------------------- file pickers
  let pickerCell = -1;
  function openCellPicker(i) { pickerCell = i; $('cellFileInput').click(); }

  $('fileInput').addEventListener('change', (e) => { addFiles(e.target.files); e.target.value = ''; });
  $('cellFileInput').addEventListener('change', (e) => { addFiles(e.target.files, pickerCell); e.target.value = ''; });

  // --------------------------------------------------------------- buttons
  const openPicker = () => $('fileInput').click();
  $('addBtn').addEventListener('click', openPicker);
  $('emptyAddBtn').addEventListener('click', openPicker);
  $('shuffleBtn').addEventListener('click', shuffle);
  $('downloadBtn').addEventListener('click', download);
  $('downloadBtn2').addEventListener('click', download);
  $('clearAllBtn').addEventListener('click', () => {
    state.images.forEach((img) => { URL.revokeObjectURL(img.url); if (img.bitmap.close) img.bitmap.close(); });
    state.images = [];
    state.cells = state.cells.map(emptyCell);
    refresh();
  });
  $('resetCellBtn').addEventListener('click', () => {
    const c = state.cells[state.selected];
    if (c) { c.zoom = 1; c.px = 0; c.py = 0; refresh(); }
  });
  $('replaceBtn').addEventListener('click', () => { if (state.selected >= 0) openCellPicker(state.selected); });
  $('swapBtn').addEventListener('click', () => { state.swapping = !state.swapping; refresh(); });
  $('clearCellBtn').addEventListener('click', () => {
    if (state.selected >= 0) { state.cells[state.selected] = emptyCell(); refresh(); }
  });
  $('size').addEventListener('change', refresh);

  // ------------------------------------------------------------------ boot
  buildStaticUI();
  new ResizeObserver(layoutCanvas).observe(stage);
  window.addEventListener('resize', layoutCanvas);
  layoutCanvas();
  refresh();

  // Exposed for tests / debugging.
  window.__collage = { state, computeRects, LAYOUTS };
})();
