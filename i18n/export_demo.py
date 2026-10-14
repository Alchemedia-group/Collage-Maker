#!/usr/bin/env python3
"""Writes the translation data the web demo uses: i18n/demo_i18n.json.

The demo looks strings up by their English text, so this inverts the key-based tables used by the app.
Run it after build.py. demo_extra.py holds strings that only the demo has.
"""
import importlib, json, os, re, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import en
try:
    import demo_extra
except ImportError:
    demo_extra = None

LANGS = ['es', 'de', 'fr', 'it', 'pt', 'hi', 'id']
# keys that start with these belong to one kind of item, so the same English word can read differently in each place
PREFIXES = ['texture_', 'filter_', 'frame_', 'preset_', 'grad_', 'tpl_', 'social_', 'emoji_', 'fontcat_', 'lc_', 'shape_', 'dz_tab_', 'layout_']
PH = re.compile(r'%(\d)\$[sd]')


def to_template(s):
    return PH.sub(lambda m: '{' + str(int(m.group(1)) - 1) + '}', s)


def main():
    mods = {l: importlib.import_module(l) for l in LANGS}
    plain, ctx, fmt, plurals = {}, {p: {} for p in PREFIXES}, [], []
    entries = dict(en.EN)
    entries.update({'layout_' + k: v for k, v in en.LAYOUT_EN.items()})
    for key, text in entries.items():
        def tr(l):
            return mods[l].T[key] if key in mods[l].T else mods[l].L[key[len('layout_'):]]
        if '%' in text:
            fmt.append({'en': to_template(text), **{l: to_template(tr(l)) for l in LANGS}})
            continue
        for p in PREFIXES:
            if key.startswith(p):
                ctx[p][text] = {l: tr(l) for l in LANGS}
        # the first key with this English text wins for places with no context
        plain.setdefault(text, {l: tr(l) for l in LANGS})
    for key, forms in en.PLURALS_EN.items():
        plurals.append({'en': {q: to_template(s.replace('%d', '%1$d')) for q, s in forms.items()},
                        **{l: {q: to_template(s.replace('%d', '%1$d')) for q, s in mods[l].P[key].items()} for l in LANGS}})
    if demo_extra:
        for english, tr in demo_extra.PLAIN.items():
            plain[english] = {l: tr[i] for i, l in enumerate(LANGS)}
        for english, tr in getattr(demo_extra, 'FMT', {}).items():
            fmt.append({'en': english, **{l: tr[i] for i, l in enumerate(LANGS)}})
    out = {'langs': LANGS, 'plain': plain, 'ctx': {p: d for p, d in ctx.items() if d}, 'fmt': fmt, 'plurals': plurals}
    with open(os.path.join(HERE, 'demo_i18n.json'), 'w', encoding='utf-8') as f:
        json.dump(out, f, ensure_ascii=False, separators=(',', ':'))
    print('demo dictionary:', len(plain), 'strings,', len(fmt), 'formats,', len(plurals), 'plurals,', os.path.getsize(os.path.join(HERE, 'demo_i18n.json')) // 1024, 'KB')


if __name__ == '__main__':
    main()
