#!/usr/bin/env python3
"""Adds (or refreshes) the language switch in design/photo-collage-demo.html.

  python3 i18n/build_demo.py [path-to-demo.html]
It runs export_demo.py first, then writes the dictionary and engine between the i18n markers, and the language
picker into the page's guide column. Safe to run again.
"""
import json, os, re, subprocess, sys
HERE = os.path.dirname(os.path.abspath(__file__))
subprocess.run([sys.executable, os.path.join(HERE, 'export_demo.py')], check=True)
path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, '..', 'design', 'photo-collage-demo.html')
html = open(path, encoding='utf-8').read()
data = open(os.path.join(HERE, 'demo_i18n.json'), encoding='utf-8').read()
engine = open(os.path.join(HERE, 'demo_engine.js'), encoding='utf-8').read().replace('/*__DICT__*/{}', data)
block = '<!-- i18n:start -->\n<script>\n' + engine + '\n</script>\n<!-- i18n:end -->\n'
html = re.sub(r'<!-- i18n:start -->.*?<!-- i18n:end -->\n', '', html, flags=re.S)
anchor = '<script>\n(() => {\n  \'use strict\';'
assert html.count(anchor) == 1, 'main script not found'
html = html.replace(anchor, block + anchor)
# the language switch lives in the phone's Home bar (a globe) and opens a sheet
globe = '<button type="button" class="ibtn" id="homeLang" aria-label="Language" title="Language"><svg class="i" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c2.5 2.5 3.8 5.5 3.8 9s-1.3 6.5-3.8 9c-2.5-2.5-3.8-5.5-3.8-9S9.5 5.5 12 3z"/></svg></button>\n        '
html = re.sub(r'<button type="button" class="ibtn" id="homeLang".*?</button>\n        ', '', html, flags=re.S)
anchor_h = '<button type="button" class="ibtn" id="homeHistory"'
assert html.count(anchor_h) == 1, 'history button not found'
html = html.replace(anchor_h, globe + anchor_h)
sheet = '''    <!-- Language -->
    <div class="scrim" id="langScrim" hidden style="z-index:12">
      <div class="sheet" role="dialog" aria-modal="true" aria-labelledby="langTitle">
        <div class="grab"></div>
        <h3 id="langTitle">Language</h3>
        <div class="langlist" id="langList" role="radiogroup" aria-label="Language"></div>
        <button type="button" class="btn ghost" id="langClose">Cancel</button>
      </div>
    </div>

'''
html = re.sub(r'    <!-- Language -->\n    <div class="scrim" id="langScrim".*?</div>\n    </div>\n\n', '', html, flags=re.S)
m2 = '    <!-- Collage menu -->'
assert html.count(m2) == 1, 'collage menu marker not found'
html = html.replace(m2, sheet + m2)
html = re.sub(r'    <div data-no-i18n>\n      <h2>Language</h2>.*?</div>\n', '', html, flags=re.S)
css = '''  .langlist { display: flex; flex-direction: column; gap: 6px; max-height: 330px; overflow-y: auto; }
  .langrow { display: flex; align-items: center; justify-content: space-between; width: 100%; text-align: left; border: 1px solid var(--line); background: var(--card); color: var(--ink); border-radius: 14px; padding: 13px 16px; font-size: 16px; font-weight: 600; }
  .langrow[aria-checked="true"] { background: var(--tint); border-color: var(--accent); color: var(--on-tint); font-weight: 800; }
  .langrow[aria-checked="true"]::after { content: ""; width: 18px; height: 18px; background: currentColor; -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='black' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 12l4 4 8-9'/%3E%3C/svg%3E") center/contain no-repeat; mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='black' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 12l4 4 8-9'/%3E%3C/svg%3E") center/contain no-repeat; }
'''
if '.langlist' not in html:
    html = html.replace('  /* export sheet */', css + '\n  /* export sheet */', 1)
open(path, 'w', encoding='utf-8').write(html)
print('updated', os.path.normpath(path), len(html) // 1024, 'KB')
