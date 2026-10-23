import os, re, sys

SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'cavemind', 'src', 'main', 'java', 'vn', 'cavemind')


def skip_lit(s, i):
    q = s[i]
    i += 1
    while i < len(s):
        if s[i] == '\\':
            i += 2
            continue
        if s[i] == q:
            return i + 1
        i += 1
    return i


def match(s, i):
    o = s[i]
    c = {'(': ')', '{': '}', '[': ']'}[o]
    d = 0
    while i < len(s):
        ch = s[i]
        if ch in '"\'':
            i = skip_lit(s, i)
            continue
        if ch == '/' and s[i:i + 2] == '//':
            i = s.index('\n', i)
            continue
        if ch in '({[':
            d += 1
        elif ch in ')}]':
            d -= 1
            if d == 0:
                return i
        i += 1
    raise ValueError('unbalanced')


def stmt_end(s, i):
    while s[i].isspace():
        i += 1
    if s[i] == '{':
        return match(s, i) + 1
    if s.startswith('if', i) and not s[i + 2].isalnum():
        j = s.index('(', i)
        k = stmt_end(s, match(s, j) + 1)
        m = k
        while m < len(s) and s[m].isspace():
            m += 1
        if s.startswith('else', m) and not s[m + 4].isalnum():
            return stmt_end(s, m + 4)
        return k
    d = 0
    while i < len(s):
        ch = s[i]
        if ch in '"\'':
            i = skip_lit(s, i)
            continue
        if ch in '({[':
            d += 1
        elif ch in ')}]':
            d -= 1
        elif ch == ';' and d == 0:
            return i + 1
        i += 1
    raise ValueError('no end')


def expr_end(s, i):
    d = 0
    tern = 0
    while i < len(s):
        ch = s[i]
        if ch in '"\'':
            i = skip_lit(s, i)
            continue
        if ch in '({[':
            d += 1
        elif ch in ')}]':
            if d == 0:
                return i
            d -= 1
        elif d == 0 and ch in ';,':
            return i
        elif d == 0 and ch == '?':
            tern += 1
        elif d == 0 and ch == ':':
            if tern == 0:
                return i
            tern -= 1
        i += 1
    return i


def colon(s, i):
    d = 0
    tern = 0
    while i < len(s):
        ch = s[i]
        if ch in '"\'':
            i = skip_lit(s, i)
            continue
        if ch in '({[':
            d += 1
        elif ch in ')}]':
            d -= 1
        elif d == 0 and ch == '?':
            tern += 1
        elif d == 0 and ch == ':':
            if tern == 0:
                return i
            tern -= 1
        i += 1
    raise ValueError('no colon')


def line_bounds(s, a, b):
    ls = s.rfind('\n', 0, a) + 1
    le = s.find('\n', b)
    le = len(s) if le < 0 else le
    return ls, le


def drop(s, a, b, empty='{}'):
    ls, le = line_bounds(s, a, b)
    if not s[ls:a].strip() and not s[b:le].strip():
        nxt = s[le + 1:le + 200].lstrip()
        return s[:ls] + s[le + 1:]
    return s[:a] + empty + s[b:]


def ternaries(s):
    while True:
        m = re.search(r'Dev\.on\(\)\s*\?', s)
        if not m:
            return s
        a = m.end()
        c = colon(s, a)
        e = expr_end(s, c + 1)
        s = s[:m.start()] + s[c + 1:e].strip() + s[e:]


def ifs(s):
    pos = 0
    while True:
        m = re.compile(r'\bif\s*\(\s*(!?)Dev\.on\(\)\s*(&&[^)]*)?\)').search(s, pos)
        if not m:
            return s
        j = s.index('(', m.start())
        k = match(s, j)
        if s[j + 1:k].strip().startswith('!'):
            pos = k
            continue
        body_end = stmt_end(s, k + 1)
        n = body_end
        while n < len(s) and s[n] in ' \t\r\n':
            n += 1
        start = m.start()
        if s.startswith('else', n) and not s[n + 4].isalnum():
            rest_end = stmt_end(s, n + 4)
            rest = s[n + 4:rest_end].strip()
            s = s[:start] + rest + s[rest_end:]
        else:
            s = drop(s, start, body_end)
        pos = start


def calls(s, name):
    while True:
        m = re.search(r'\b' + re.escape(name) + r'\(', s)
        if not m:
            return s
        j = m.end() - 1
        k = match(s, j)
        e = k + 1
        while s[e] in ' \t':
            e += 1
        if s[e] == ';':
            s = drop(s, m.start(), e + 1)
        else:
            pre = s[:m.start()].rstrip()
            if pre.endswith('->'):
                s = s[:m.start()] + '{}' + s[k + 1:]
            else:
                raise ValueError('call used as value: ' + s[m.start() - 60:k + 20])


def empties(s):
    out = []
    lines = s.split('\n')
    for i, ln in enumerate(lines):
        st = ln.strip()
        nxt = lines[i + 1].strip() if i + 1 < len(lines) else ''
        if re.fullmatch(r'(else\s+)?(if|for)\s*\(.*\)\s*\{\}', st) and not nxt.startswith('else') and '=' not in re.sub(r'[=!<>]=|->', '', st.split('(', 1)[1]):
            continue
        out.append(ln)
    return '\n'.join(out)


def main():
    changed = []
    for root, _, files in os.walk(SRC):
        for f in files:
            if not f.endswith('.java'):
                continue
            p = os.path.join(root, f)
            s0 = s = open(p, encoding='utf-8').read()
            if 'Dev.' not in s:
                continue
            s = s.replace('vn.cavemind.dev.Dev.', 'Dev.')
            s = ternaries(s)
            s = re.sub(r'!Dev\.on\(\)\s*&&\s*', '', s)
            s = re.sub(r'\s*&&\s*!Dev\.on\(\)', '', s)
            s = re.sub(r'Dev\.on\(\)\s*\|\|\s*', '', s)
            s = re.sub(r'\s*\|\|\s*Dev\.on\(\)', '', s)
            s = ifs(s)
            s = calls(s, 'Dev.say')
            s = calls(s, 'Dev.glow')
            s = empties(s)
            s = s.replace('import vn.cavemind.dev.Dev;\n', '')
            if s != s0:
                open(p, 'w', encoding='utf-8').write(s)
                changed.append(os.path.relpath(p, SRC))
    print('changed', len(changed))
    for root, _, files in os.walk(SRC):
        for f in files:
            if f.endswith('.java'):
                t = open(os.path.join(root, f), encoding='utf-8').read()
                for m in re.finditer(r'Dev\.\w+', t):
                    print('left', f, t.count('\n', 0, m.start()) + 1, t[m.start():m.start() + 60].split('\n')[0])


main()
