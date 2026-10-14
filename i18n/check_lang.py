import re, sys, importlib
import en
def check(mod):
    m = importlib.import_module(mod)
    bad = False
    for k in en.EN:
        if k not in m.T: print(mod, 'missing', k); bad = True; continue
        a = sorted(re.findall(r'%\d\$[sd]', en.EN[k])); b = sorted(re.findall(r'%\d\$[sd]', m.T[k]))
        if a != b: print(mod, 'placeholder mismatch', k, en.EN[k], '|', m.T[k]); bad = True
    for k in m.T:
        if k not in en.EN: print(mod, 'extra', k); bad = True
    for k, forms in en.PLURALS_EN.items():
        if k not in m.P: print(mod, 'missing plural', k); bad = True; continue
        if 'other' not in m.P[k]: print(mod, 'plural without other', k); bad = True
        for q, s in m.P[k].items():
            if s.count('%d') != 1: print(mod, 'plural needs one %d', k, q, s); bad = True
    for k in en.LAYOUT_EN:
        if k not in m.L: print(mod, 'missing layout', k); bad = True
    for k in m.L:
        if k not in en.LAYOUT_EN: print(mod, 'extra layout', k); bad = True
    return not bad
if __name__ == '__main__':
    ok = all(check(m) for m in sys.argv[1:])
    print('OK' if ok else 'PROBLEMS')
