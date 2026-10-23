import sys, os, json, time, uuid
GAME = r'E:\Modrinth\profiles\Cave Horror Project 1'
BOX = os.path.join(GAME, 'kubejs', 'claude')

def call(ops, timeout=30):
    os.makedirs(BOX, exist_ok=True)
    rid = uuid.uuid4().hex
    tmp = os.path.join(BOX, 'inbox.tmp')
    with open(tmp, 'w', encoding='utf-8') as f:
        json.dump({'id': rid, 'ops': ops}, f)
    os.replace(tmp, os.path.join(BOX, 'inbox.json'))
    out = os.path.join(BOX, 'outbox.json')
    t0 = time.time()
    while time.time() - t0 < timeout:
        try:
            with open(out, encoding='utf-8') as f:
                r = json.load(f)
            if r.get('id') == rid:
                return r['results']
        except Exception:
            pass
        time.sleep(0.1)
    raise SystemExit('no reply (world closed or paused?)')

def status():
    call([{'cmd': 'cavemind dump'}])
    time.sleep(0.2)
    return open(os.path.join(GAME, 'cavemind_status.txt'), encoding='utf-8').read()

if __name__ == '__main__':
    args = sys.argv[1:]
    if not args or args[0] == 'status':
        print(status())
    elif args[0] == 'players':
        print(json.dumps(call([{'players': 1}]), indent=1))
    elif args[0] == 'chat':
        print(json.dumps(call([{'chat': 1}]), ensure_ascii=False, indent=1))
    else:
        cmds = [{'cmd': c} for c in args]
        print(json.dumps(call(cmds), ensure_ascii=False))
