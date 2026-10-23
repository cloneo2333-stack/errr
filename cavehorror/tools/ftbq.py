import os, re, sys, json, shutil
Q = r'E:/Modrinth/profiles/Cave Horror Project 1/config/ftbquests/quests'
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'viet', 'ftbq')
STR = re.compile(r'"((?:[^"\\]|\\.)*)"')
def files():
    for d in ('chapters', 'reward_tables'):
        for f in sorted(os.listdir(os.path.join(Q, d))):
            yield os.path.join(Q, d, f)
def spans(text):
    for m in re.finditer(r'\b(title|subtitle)\s*:\s*"((?:[^"\\]|\\.)*)"', text):
        yield m.start(2), m.end(2), m.group(2)
    for m in re.finditer(r'\bdescription\s*:\s*\[(.*?)\]', text, re.S):
        base = m.start(1)
        for s in STR.finditer(m.group(1)):
            yield base + s.start(1), base + s.end(1), s.group(1)
def extract():
    os.makedirs(OUT, exist_ok=True)
    uniq = []
    for p in files():
        for a, b, s in spans(open(p, encoding='utf-8').read()):
            if s.strip() and re.search('[A-Za-z]', s) and s not in uniq:
                uniq.append(s)
    json.dump(uniq, open(os.path.join(OUT, 'src.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
    open(os.path.join(OUT, 'src.txt'), 'w', encoding='utf-8').write('\n'.join('%d\t%s' % (i, s) for i, s in enumerate(uniq)) + '\n')
    print(len(uniq), sum(len(s) for s in uniq))
def apply():
    src = json.load(open(os.path.join(OUT, 'src.json'), encoding='utf-8'))
    vi = {}
    for line in open(os.path.join(OUT, 'vi.txt'), encoding='utf-8'):
        line = line.rstrip('\n')
        if '\t' in line:
            i, t = line.split('\t', 1); vi[src[int(i)]] = t
    bak = Q + '_en_backup'
    if not os.path.exists(bak): shutil.copytree(Q, bak)
    n = 0
    for p in files():
        text = open(p, encoding='utf-8').read()
        for a, b, s in sorted(spans(text), key=lambda x: -x[0]):
            if s in vi:
                text = text[:a] + vi[s] + text[b:]; n += 1
        open(p, 'w', encoding='utf-8').write(text)
    print('replaced', n, 'missing', len([s for s in src if s not in vi]))
if __name__ == '__main__':
    {'extract': extract, 'apply': apply}[sys.argv[1]]()
