import os, re, shutil, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from strip_dev import match, SRC

METHODS = {
    'damned/Haunt.java': ['status', 'devLine', 'eye'],
    'day/Daylight.java': ['status', 'devLine', 'eye'],
    'director/Director.java': ['eye'],
    'hero/Herobrine.java': ['eye', 'devLine', 'status'],
    'lore/Lore.java': ['status', 'force'],
    'man/ManMind.java': ['status', 'devLine', 'eye'],
    'man/Schemes.java': ['status', 'devLine'],
    'Events.java': ['hud'],
}


def drop_method(s, name):
    m = re.search(r'\n([ \t]*)(public |private |protected )?(static )?[\w.<>\[\], ]+\s+' + re.escape(name) + r'\s*\([^)]*\)\s*\{', s)
    if not m:
        print('  not found', name)
        return s
    b = s.index('{', m.end() - 1)
    e = match(s, b)
    start = m.start()
    pre = s.rfind('\n', 0, start)
    if s[pre + 1:start].strip() == '':
        start = pre if pre >= 0 else start
    return s[:start] + s[e + 1:]


def sub(path, a, b):
    p = os.path.join(SRC, path)
    s = open(p, encoding='utf-8').read()
    if a not in s:
        print('  missing in', path, ':', a[:70])
        return
    open(p, 'w', encoding='utf-8').write(s.replace(a, b))


def main():
    for path, names in METHODS.items():
        p = os.path.join(SRC, path)
        s = open(p, encoding='utf-8').read()
        for n in names:
            s = drop_method(s, n)
        open(p, 'w', encoding='utf-8').write(s)
        print('ok', path)
    sub('Events.java', '        if (now % 20 == 0 && Dev.on()) hud(p);\n', '')
    sub('Events.java', """        try {
            vn.cavemind.eye.Eye.tick(p, w, now);
        } catch (Throwable t) {
            CaveMind.LOG.warn("[CaveMind] eye tick failed", t);
        }
""", '')
    sub('Events.java', '        vn.cavemind.eye.Eye.open(e.getServer());\n', '')
    sub('Events.java', '        vn.cavemind.eye.Eye.close();\n', '')
    sub('Events.java', '            vn.cavemind.eye.Eye.MANAGED.remove(e.getEntity().getUUID());\n', '')
    sub('CaveMind.java', '        vn.cavemind.eye.Eye.event(line);\n', '')
    sub('director/Director.java', '        boolean dev = Dev.on();\n', '')
    sub('director/Director.java', '(dev ? 400 : 3600)', '3600')
    sub('man/Pack.java', 'if (!Dev.on()) p.hurt(', 'p.hurt(')
    sub('compat/Compat.java', ' && !TEST.contains(p.getUUID()) && !Cfg.DEV.get()', '')
    sub('compat/Compat.java', '    public static final java.util.Set<java.util.UUID> TEST = new java.util.HashSet<>();\n', '')
    for d in ('dev', 'eye'):
        shutil.rmtree(os.path.join(SRC, d), ignore_errors=True)


main()
