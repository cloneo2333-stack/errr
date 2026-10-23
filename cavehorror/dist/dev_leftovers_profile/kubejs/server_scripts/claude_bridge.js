// Claude bridge: reads kubejs/claude/inbox.json, runs ops, writes kubejs/claude/outbox.json
var CB = { dir: 'kubejs/claude', lastId: null, job: null, chat: [], tick: 0 }

function cbRead(name) {
  try { return JsonIO.read(CB.dir + '/' + name) } catch (e) { return null }
}

function cbWrite(name, obj) {
  try { JsonIO.write(CB.dir + '/' + name, obj) } catch (e) { console.error('claude_bridge write ' + e) }
}

function cbPlayers(server) {
  var out = []
  server.players.forEach(function (p) {
    var o = {
      name: String(p.username), x: p.x, y: p.y, z: p.z, yaw: p.yaw, pitch: p.pitch,
      dim: String(p.level.dimension)
    }
    try {
      var hit = p.rayTrace(96, false)
      if (hit && hit.block) o.looking_at = { x: hit.block.x, y: hit.block.y, z: hit.block.z, id: String(hit.block.id), face: String(hit.facing) }
    } catch (e) { }
    try { o.held = String(p.mainHandItem.id) } catch (e) { }
    out.push(o)
  })
  return out
}

function cbBlockStr(b) {
  var id = String(b.id)
  try {
    var props = b.properties
    var keys = []
    for (var k in props) keys.push(k + '=' + props[k])
    if (keys.length) id += '[' + keys.join(',') + ']'
  } catch (e) { }
  return id
}

function cbGet(server, op) {
  var r = op.region
  var level = server.getLevel(op.dim || 'minecraft:overworld')
  var x1 = Math.min(r[0], r[3]), x2 = Math.max(r[0], r[3])
  var y1 = Math.min(r[1], r[4]), y2 = Math.max(r[1], r[4])
  var z1 = Math.min(r[2], r[5]), z2 = Math.max(r[2], r[5])
  var vol = (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1)
  if (vol > 65536) return { error: 'region too big: ' + vol + ' > 65536' }
  var pal = [], idx = {}, runs = [], last = -1, count = 0
  for (var y = y1; y <= y2; y++) for (var z = z1; z <= z2; z++) for (var x = x1; x <= x2; x++) {
    var s = cbBlockStr(level.getBlock(x, y, z))
    var i = idx[s]
    if (i === undefined) { i = pal.length; idx[s] = i; pal.push(s) }
    if (i === last) count++
    else { if (last >= 0) runs.push(last + '*' + count); last = i; count = 1 }
  }
  if (last >= 0) runs.push(last + '*' + count)
  return { from: [x1, y1, z1], to: [x2, y2, z2], order: 'y,z,x', palette: pal, rle: runs.join(',') }
}

function cbOp(server, op) {
  if (op.cmd !== undefined) {
    var c = String(op.cmd)
    if (c.charAt(0) === '/') c = c.substring(1)
    if (op.dim) c = 'execute in ' + op.dim + ' run ' + c
    return { cmd: c, result: server.runCommandSilent(c) }
  }
  if (op.region !== undefined) return cbGet(server, op)
  if (op.raw !== undefined) {
    var lvr = server.getLevel(op.dim || 'minecraft:overworld')
    var okc = 0, errs = []
    var items = op.raw
    for (var j = 0; j < items.length; j++) {
      var it = items[j]
      try {
        var stt = Utils.parseBlockState(String(it[3]))
        lvr.getBlock(it[0], it[1], it[2]).setBlockState(stt, op.flags || 18)
        okc++
      } catch (e) { if (errs.length < 10) errs.push(String(it) + ' ' + e) }
    }
    return { raw: okc, errors: errs }
  }
  if (op.probe !== undefined) {
    var lv = server.getLevel(op.dim || 'minecraft:overworld')
    var res = []
    var pts = op.probe
    for (var i = 0; i < pts.length; i++) {
      var q = pts[i]
      res.push(cbBlockStr(lv.getBlock(q[0], q[1], q[2])))
    }
    return { probe: res }
  }
  if (op.players !== undefined) return { players: cbPlayers(server) }
  if (op.chat !== undefined) { var c2 = CB.chat; CB.chat = []; return { chat: c2 } }
  return { error: 'unknown op' }
}

ServerEvents.tick(function (event) {
  var server = event.server
  CB.tick++
  if (!CB.job && CB.tick % 4 === 0) {
    var req = cbRead('inbox.json')
    if (req && req.id && String(req.id) !== CB.lastId) {
      CB.lastId = String(req.id)
      CB.job = { id: CB.lastId, ops: req.ops || [], i: 0, results: [] }
    }
  }
  if (!CB.job) return
  var job = CB.job, t0 = Date.now()
  while (job.i < job.ops.length && Date.now() - t0 < 25) {
    var r
    try { r = cbOp(server, job.ops[job.i]) } catch (e) { r = { error: String(e) } }
    job.results.push(r)
    job.i++
  }
  if (job.i >= job.ops.length) {
    cbWrite('outbox.json', { id: job.id, done: true, results: job.results })
    CB.job = null
  } else if (CB.tick % 20 === 0) {
    cbWrite('progress.json', { id: job.id, done: job.i, total: job.ops.length })
  }
})

PlayerEvents.chat(function (event) {
  CB.chat.push({ name: String(event.player.username), msg: String(event.message), t: Date.now() })
  if (CB.chat.length > 200) CB.chat.shift()
})

ServerEvents.loaded(function (event) {
  cbWrite('status.json', { loaded: Date.now(), note: 'claude bridge ready' })
})
