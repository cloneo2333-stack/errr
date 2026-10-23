import re


class Q(str):
    pass


class _P:
    TOK = re.compile(r'[^\s,\]\}]+')
    KEY = re.compile(r'[A-Za-z0-9_\-.+]+')

    def __init__(self, t):
        self.t = t
        self.i = 0

    def ws(self):
        while self.i < len(self.t) and self.t[self.i] in ' \t\r\n,':
            self.i += 1

    def val(self):
        self.ws()
        c = self.t[self.i]
        if c == '{':
            self.i += 1
            d = {}
            while True:
                self.ws()
                if self.t[self.i] == '}':
                    self.i += 1
                    return d
                k = self.key()
                self.ws()
                assert self.t[self.i] == ':', self.t[self.i - 30:self.i + 30]
                self.i += 1
                d[k] = self.val()
        if c == '[':
            self.i += 1
            out = []
            while True:
                self.ws()
                if self.t[self.i] == ']':
                    self.i += 1
                    return out
                out.append(self.val())
        if c == '"':
            self.i += 1
            o = []
            while self.t[self.i] != '"':
                if self.t[self.i] == '\\':
                    self.i += 1
                o.append(self.t[self.i])
                self.i += 1
            self.i += 1
            return Q(''.join(o))
        m = self.TOK.match(self.t, self.i)
        self.i = m.end()
        return m.group(0)

    def key(self):
        if self.t[self.i] == '"':
            return str(self.val())
        m = self.KEY.match(self.t, self.i)
        self.i = m.end()
        return m.group(0)


def loads(text):
    return _P(text).val()


def load(path):
    with open(path, encoding='utf-8') as f:
        return loads(f.read())


def _esc(s):
    return '"' + s.replace('\\', '\\\\').replace('"', '\\"') + '"'


def _prim(v):
    if isinstance(v, Q):
        return _esc(v)
    if isinstance(v, bool):
        return 'true' if v else 'false'
    return str(v)


def _key(k):
    return k if re.fullmatch(r'[A-Za-z0-9_\-.+]+', k) else _esc(k)


def dumps(v, ind=0):
    tab = '\t'
    if isinstance(v, dict):
        if not v:
            return '{ }'
        lines = ['{']
        for k, x in v.items():
            lines.append(tab * (ind + 1) + _key(k) + ': ' + dumps(x, ind + 1))
        lines.append(tab * ind + '}')
        return '\n'.join(lines)
    if isinstance(v, list):
        if not v:
            return '[ ]'
        if all(not isinstance(x, (dict, list)) for x in v) and len(v) == 1:
            return '[' + _prim(v[0]) + ']'
        lines = ['[']
        for x in v:
            lines.append(tab * (ind + 1) + dumps(x, ind + 1))
        lines.append(tab * ind + ']')
        return '\n'.join(lines)
    return _prim(v)


def dump(v, path):
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(dumps(v) + '\n')
