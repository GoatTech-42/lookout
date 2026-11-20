#!/usr/bin/env python3
# minimal 26.3 (protocol 777) offline bot: legit movement, then provoke phases.
import socket, struct, zlib, threading, time, math, uuid as uuidlib, sys, subprocess, os

HOST, PORT = '127.0.0.1', 25565
NAME = 'ProvokeBot'
def log(*a): print(time.strftime('%H:%M:%S'), *a, flush=True)

class Conn:
    def __init__(self):
        self.s = socket.create_connection((HOST, PORT), timeout=30)
        self.buf = b''; self.comp = None; self.lock = threading.Lock()
    def _fill(self, n):
        while len(self.buf) < n:
            d = self.s.recv(65536)
            if not d: raise ConnectionError('closed')
            self.buf += d
    def read(self, n):
        self._fill(n); r = self.buf[:n]; self.buf = self.buf[n:]; return r
    def varint(self):
        num = 0
        for i in range(5):
            b = self.read(1)[0]
            num |= (b & 0x7F) << (7 * i)
            if not b & 0x80: return num
        raise ValueError('varint')
    def packet(self):
        plen = self.varint()
        data = self.read(plen)
        if self.comp is not None:
            import io
            bio = io.BytesIO(data)
            def rv():
                b = bio.read(1)
                if not b: raise EOFError
                return b
            dl = 0
            for i in range(5):
                b = rv()[0]; dl |= (b & 0x7F) << (7 * i)
                if not b & 0x80: break
            rest = bio.read()
            data = zlib.decompress(rest) if dl else rest
        pid, off = dec_varint(data, 0)
        return pid, data, off
    def send(self, pid, payload=b''):
        body = enc_varint(pid) + payload
        if self.comp is not None:
            body = enc_varint(0) + body  # uncompressed inside compression frame
        with self.lock:
            self.s.sendall(enc_varint(len(body)) + body)

def enc_varint(v):
    out = b''
    v &= 0xFFFFFFFF
    while True:
        b = v & 0x7F; v >>= 7
        if v: out += bytes([b | 0x80])
        else: return out + bytes([b])
def dec_varint(data, off):
    num = 0
    for i in range(5):
        b = data[off]; off += 1
        num |= (b & 0x7F) << (7 * i)
        if not b & 0x80: return num, off
    raise ValueError('varint')
def enc_str(s):
    b = s.encode(); return enc_varint(len(b)) + b
def dec_str(data, off):
    n, off = dec_varint(data, off)
    return data[off:off+n].decode('utf8','replace'), off+n

c = Conn()
state = 'login'
pos = None; pos_lock = threading.Lock()
lookout_seen_at = None
disconnected = threading.Event()

# --- handshake + login start
c.send(0x00, enc_varint(777) + enc_str('127.0.0.1') + struct.pack('>H', PORT) + enc_varint(2))
u = uuidlib.uuid5(uuidlib.NAMESPACE_DNS, 'OfflinePlayer:' + NAME).bytes
c.send(0x00, enc_str(NAME) + u)  # Login Start

def client_information():
    return (enc_str('en_US') + struct.pack('b', 4) + enc_varint(0) + b'\x01' +
            b'\x7f' + enc_varint(1) + b'\x00' + b'\x01' + enc_varint(0))

anchor = None
def reader():
    global state, pos, lookout_seen_at, anchor
    while not disconnected.is_set():
        try:
            pid, data, off = c.packet()
        except Exception as e:
            log('read end:', e); disconnected.set(); return
        try:
            if state == 'login':
                if pid == 0x03:  # Set Compression
                    c.comp, _ = dec_varint(data, off); log('compression', c.comp)
                elif pid == 0x02:  # Login Success
                    log('login success')
                    c.send(0x03)     # Login Acknowledged -> configuration
                    state = 'config'
                    c.send(0x00, client_information())  # config C->S 0x00
                    log('entered configuration')
                elif pid == 0x00:
                    log('LOGIN DISCONNECT:', data[off:off+200]); disconnected.set(); return
            elif state == 'config':
                if pid == 0x04:    # keep alive
                    c.send(0x04, data[off:off+8])
                elif pid == 0x05:  # ping
                    c.send(0x05, data[off:off+4])
                elif pid == 0x0F:  # Known Packs cb -> echo
                    n, o2 = dec_varint(data, off)
                    packs = data[off:]
                    c.send(0x07, packs)
                    log('known packs echoed', n)
                elif pid == 0x03:  # Finish Configuration
                    c.send(0x03)     # ack
                    state = 'play'
                    log('entered play')
                elif pid == 0x02:
                    log('CONFIG DISCONNECT:', data[off:off+200]); disconnected.set(); return
            else:  # play
                if pid == 0x2D:    # keep alive
                    c.send(0x1C, data[off:off+8])
                elif pid == 0x49:  # Synchronize Player Position
                    tid, o2 = dec_varint(data, off)
                    x, y, z = struct.unpack('>ddd', data[o2:o2+24])
                    with pos_lock:
                        pos = [x, y, z]
                    global anchor
                    anchor = [x, y, z]
                    yaw, pitch = 0.0, 0.0
                    try:
                        yw, pt = struct.unpack('>ff', data[o2+24+24:o2+24+32])
                        yaw, pitch = yw, pt
                    except Exception: pass
                    c.send(0x00, enc_varint(tid) + struct.pack('>dddff', x, y, z, yaw, pitch))
                    log('teleport', round(x,1), round(y,1), round(z,1), 'id', tid)
                elif pid == 0x47:  # Player Info Update -> detect LookoutTest
                    try:
                        o2 = off + 1   # action mask byte
                        n, o2 = dec_varint(data, o2)
                        actions = data[off]
                        for _ in range(n):
                            o2 += 16  # uuid
                            if actions & 0x01:
                                nm, o2 = dec_str(data, o2)
                                np_, o2 = dec_varint(data, o2)
                                for _ in range(np_):
                                    _, o2 = dec_str(data, o2); _, o2 = dec_str(data, o2)
                                    if data[o2]: o2 += 1; _, o2 = dec_str(data, o2)
                                    else: o2 += 1
                                if nm == 'LookoutTest' and lookout_seen_at is None:
                                    lookout_seen_at = time.time()
                                    log('LOOKOUT CLIENT SEEN -> silence clock starts')
                            if actions & 0x02: o2 += 1  # init chat: has session? (skip best effort)
                            if actions & 0x04: _, o2 = dec_varint(data, o2)
                            if actions & 0x08: o2 += 1
                            if actions & 0x10: _, o2 = dec_varint(data, o2)
                            if actions & 0x20:
                                if data[o2]: o2 += 1; ln, o2 = dec_varint(data, o2); o2 += ln
                                else: o2 += 1
                            if actions & 0x40: _, o2 = dec_varint(data, o2)
                            if actions & 0x80: o2 += 1
                    except Exception as e:
                        log('playerinfo parse (nonfatal):', e)
                elif pid == 0x01:  # Add Entity
                    try:
                        eid, o2 = dec_varint(data, off)
                        euuid = str(uuidlib.UUID(bytes=data[o2:o2+16])); o2 += 16
                        etype, o2 = dec_varint(data, o2)
                        ex, ey, ez = struct.unpack('>ddd', data[o2:o2+24])
                        log('add-entity: id', eid, 'type', etype, 'uuid', euuid, round(ex,1), round(ez,1))
                        if euuid in VUUID:
                            ents[eid] = [ex, ey, ez]
                            name_by_eid[eid] = VUUID[euuid]
                            log('victim seen:', VUUID[euuid], 'id', eid, round(ex,1), round(ez,1))
                    except Exception as e:
                        log('add-entity parse (nonfatal):', e)
                elif pid == 0x20:
                    log('PLAY DISCONNECT raw:', data[off:off+300]); disconnected.set(); return
        except Exception as e:
            log('handler error pid', hex(pid), e)

threading.Thread(target=reader, daemon=True).start()

# --- run 2 phases: silence -> speed -> step -> evasion -> combat
VICTIMS = {'VictimA': None, 'VictimB': None}
def _juuid(n):  # java UUID.nameUUIDFromBytes: namespace-less MD5
    import hashlib
    b = bytearray(hashlib.md5(('OfflinePlayer:' + n).encode()).digest())
    b[6] = (b[6] & 0x0f) | 0x30; b[8] = (b[8] & 0x3f) | 0x80
    return str(uuidlib.UUID(bytes=bytes(b)))
VUUID = {_juuid(n): n for n in VICTIMS}
ents = {}  # entityId -> [x,y,z]
name_by_eid = {}  # entityId -> victim name, from the spawn-time UUID match

# spec distances (verification spec P1/P2/P4): killaura + multiaura at 2.5, reach at 5.2
KA_DIST = 2.5
REACH_DIST = 5.2
MULTI_DIST = 2.5
CHOREO = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'choreo.py')
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import choreo
guard = choreo.ArmGuard(log=log)
SERVERDIR = os.environ.get('SERVERDIR', 'server')

def _choreo(*args):
    try:
        return subprocess.run([sys.executable, CHOREO] + list(args),
                              capture_output=True, text=True, timeout=60)
    except Exception as e:
        log('ARM GUARD FAIL: choreo call crashed:', e); sys.exit(1)

def choreo_health(name):
    r = _choreo('health', SERVERDIR, name)
    try:
        return float(r.stdout.strip())
    except ValueError:
        log('ARM GUARD FAIL: no health for', name, r.stdout.strip()[-120:]); sys.exit(1)

def guard_begin(arm, max_reach, min_reach=0.0):
    """Pre-arm: server-side geometry proof via choreo verify-arm; aborts the run
    when the band check fails. Returns victim Health before the arm."""
    args = ['verify-arm', SERVERDIR, 'VictimA', 'VictimB', NAME, str(max_reach)]
    if min_reach: args.append(str(min_reach))
    r = _choreo(*args)
    log('pre-arm guard', arm, 'rc', r.returncode)
    if r.returncode != 0:
        log('ARM GUARD FAIL pre-arm', arm, r.stdout.strip()[-200:]); sys.exit(1)
    return guard.begin(arm, {v: choreo_health(v) for v in ('VictimA', 'VictimB')})

def guard_end(arm, before, expect_sent, expect_damaged):
    """Post-arm via choreo.ArmGuard: per-arm DELTA sent counts plus the
    choreo receipt check per attacked victim (G3/G4, automatic)."""
    def receipt(a, v, health_before):
        r = _choreo('receipt', SERVERDIR, v, str(health_before), a)
        log(r.stdout.strip())
        return r.returncode == 0
    guard.end(arm, before, expect_sent, expect_damaged, receipt)


selfpos = [0.0, 0.0, 0.0]
def tick_send(x, y, z, onground, yaw=None, pitch=0.0):
    selfpos[0], selfpos[1], selfpos[2] = x, y, z
    c.send(0x0D)  # Client Tick End first, or the server kicks
    if yaw is None:
        c.send(0x1E, struct.pack('>ddd', x, y, z) + bytes([0x01 if onground else 0x00]))
    else:
        c.send(0x1F, struct.pack('>dddff', x, y, z, yaw, pitch) + bytes([0x01 if onground else 0x00]))

def attack(eid, victim=None):
    c.send(0x2E)  # punch (swing)
    c.send(0x01, enc_varint(eid))  # attack
    if victim: guard.hit(victim)

def walk_to(tx, ty, tz, yaw=None):
    while not disconnected.is_set():
        ax, ay, az = A()
        dx, dy, dz = tx-ax, ty-ay, tz-az
        d = math.sqrt(dx*dx + dy*dy + dz*dz)
        if d < 0.05:
            break
        step = min(0.9, d)
        tick_send(ax + dx/d*step, ay + dy/d*step, az + dz/d*step, True, yaw=yaw)
        time.sleep(0.06)
    tick_send(tx, ty, tz, True, yaw=yaw)

def bearing_yaw(ax, az, vx, vz):
    return math.degrees(math.atan2(-(vx - ax), vz - az))

def near(eid, dist):
    e = ents.get(eid)
    if not e: return None
    ax, ay, az = A()
    vx, vy, vz = e
    dx, dz = vx - ax, vz - az
    L = math.hypot(dx, dz) or 1
    return (vx - dx/L*dist, vy, vz - dz/L*dist, vx, vz)


def wait_state(s, timeout=60):
    t0 = time.time()
    while state != s and not disconnected.is_set():
        if time.time() - t0 > timeout: log('timeout waiting for', s); return False
        time.sleep(0.05)
    return state == s

if not wait_state('play'): sys.exit(1)
while pos is None and not disconnected.is_set(): time.sleep(0.05)
anchor = list(pos)
selfpos[0], selfpos[1], selfpos[2] = anchor[0], anchor[1], anchor[2]
def A():
    return selfpos[0], selfpos[1], selfpos[2]  # where we last put ourselves, not the join anchor
log('anchor', *anchor)
t = 0
cx0, gy, cz0 = A()

def run_phase(name, seconds, fn):
    global t
    log('PHASE', name)
    t0 = time.time()
    while time.time() - t0 < seconds and not disconnected.is_set():
        fn(t)
        t += 1
        time.sleep(0.05)

if not os.environ.get('COMBAT_ONLY'):
    _phases = True
run_phase('legit-silence', 0 if os.environ.get('COMBAT_ONLY') else 75, lambda t: tick_send(cx0 - 8 + 8*math.cos(t*0.02), gy, cz0 + 8*math.sin(t*0.02), True))
def speedmv(t):
    d = min((t % 60) * 0.45, 25); k = (t // 60) % 4; ang = k * math.pi / 2
    tick_send(cx0 + d*math.cos(ang), gy, cz0 + d*math.sin(ang), True)
run_phase('speed-0.45', 0 if os.environ.get('COMBAT_ONLY') else 45, speedmv)
run_phase('step', 0 if os.environ.get('COMBAT_ONLY') else 45, lambda t: tick_send(cx0, gy + (1.2 if (t // 20) % 2 == 0 else 0.0), cz0, True))
def evade(t):
    d = t * 0.45 + (4.1 if t % 4 == 3 else 0.0); k = (t // 240) % 4; ang = k * math.pi / 2
    tick_send(cx0 + d*math.cos(ang), gy, cz0 + d*math.sin(ang), True)
run_phase('evasion-periodic-jumps', 0 if os.environ.get('COMBAT_ONLY') else 20, evade)
run_phase('settle', 0 if os.environ.get('COMBAT_ONLY') else 15, lambda t: tick_send(cx0 - 8 + 8*math.cos(t*0.02), gy, cz0 + 8*math.sin(t*0.02), True))

_tw = time.time()
while len(ents) < 1 and time.time() - _tw < 6 and not disconnected.is_set():
    time.sleep(0.1)
if ents:
    # Identity is bound by spawn-time UUID, never by entity arrival order: the
    # first entity seen is not necessarily VictimA, and mislabeled attacks or
    # receipts invalidate the session. Both identities are required; a missing
    # or ambiguous one fails closed.
    try:
        _bound = choreo.bind_victims(name_by_eid)
    except ValueError as e:
        log('ARM GUARD FAIL: victim identity binding -', e)
        sys.exit(1)
    eidA, eidB = _bound['VictimA'], _bound['VictimB']
    posA = ents[eidA]
    log('combat victims bound:', _bound)
    if os.environ.get('DESCEND'):
        ty = posA[1]
        y = A()[1]
        log('descending from', round(y,1), 'to', round(ty,1))
        while y - ty > 1.5 and not disconnected.is_set():
            y -= 1.5
            tick_send(A()[0], y, A()[2], True)
            time.sleep(0.2)
        tick_send(A()[0], ty, A()[2], True)
        time.sleep(1.0)
        log('descend done at', ty)
    def stage_to(dist, yaw_off):
        spot = near(eidA, dist)
        if not spot:
            return False
        ax, ay, az, vx, vz = spot
        walk_to(ax, ay, az, yaw=bearing_yaw(ax, az, vx, vz) + yaw_off)
        return True

    def ka_attacks():
        for i in range(6):
            spot = near(eidA, KA_DIST)  # recompute from live ents: victims wander
            if not spot: break
            ax, ay, az, vx, vz = spot
            away = bearing_yaw(ax, az, vx, vz) + 100.0  # spec P1: ~100 deg off-facing
            tick_send(ax, ay, az, True, yaw=away)
            attack(eidA, 'VictimA')
            log('killaura hit', i + 1, 'off-facing from', KA_DIST)
            time.sleep(0.2)  # 4 ticks apart per spec

    choreo.run_arm('killaura',
                   lambda: stage_to(KA_DIST, 100.0),
                   lambda: guard_begin('killaura', 3.0),
                   ka_attacks,
                   lambda before: guard_end('killaura', before, {'VictimA': 6}, ('VictimA',)))
    _ap = float(os.environ.get('ARM_PAUSE', '0'))
    if _ap:
        log('ARM killaura DONE, pausing', _ap)
        time.sleep(_ap)

    if os.environ.get('KA_ONLY'):
        log('reach arm skipped by KA_ONLY')
    else:
        def reach_attacks():
            for i in range(6):
                spot = near(eidA, REACH_DIST)
                if not spot: break
                ax, ay, az, vx, vz = spot
                toward = bearing_yaw(ax, az, vx, vz)
                tick_send(ax, ay, az, True, yaw=toward)
                attack(eidA, 'VictimA')
                log('reach hit', i+1, 'from', REACH_DIST)
                time.sleep(0.2)  # 4 ticks apart per spec

        choreo.run_arm('reach',
                       lambda: stage_to(REACH_DIST, 0.0),
                       lambda: guard_begin('reach', REACH_DIST + 1.5, min_reach=4.6),
                       reach_attacks,
                       lambda before: guard_end('reach', before, {'VictimA': 6}, ('VictimA',)))
    if _ap:
        log('ARM reach DONE, pausing', _ap)
        time.sleep(_ap)

    if os.environ.get('KA_ONLY'):
        log('multiaura arm skipped by KA_ONLY')
    else:  # bind_victims already required both identities; eidB is bound
        def multi_attacks():
            for bn in range(4):
                spot = near(eidA, MULTI_DIST)
                if not spot: break
                ax, ay, az, vx, vz = spot
                toward = bearing_yaw(ax, az, vx, vz)
                tick_send(ax, ay, az, True, yaw=toward)
                attack(eidA, 'VictimA'); attack(eidB, 'VictimB')
                log('multiaura burst', bn+1)
                time.sleep(0.15)  # one burst every 3 ticks per spec

        choreo.run_arm('multiaura',
                       lambda: stage_to(MULTI_DIST, 0.0),
                       lambda: guard_begin('multiaura', 3.0),
                       multi_attacks,
                       lambda before: guard_end('multiaura', before, {'VictimA': 4, 'VictimB': 4}, ('VictimA', 'VictimB')))
else:
    log('ARM GUARD FAIL: NO VICTIMS SEEN - combat arms not exercised, failing closed')
    sys.exit(1)
_hold = float(os.environ.get('HOLD_AFTER', '0'))
if _hold:
    log('holding', _hold, 's for console queries')
    t0 = time.time()
    while time.time() - t0 < _hold and not disconnected.is_set():
        if os.environ.get('KA_LOOP') and ents:
            eidL, posL = list(ents.items())[0]
            spot = near(eidL, KA_DIST)
            if spot:
                ax, ay, az, vx, vz = spot
                tick_send(ax, ay, az, True, yaw=bearing_yaw(ax, az, vx, vz) + 180.0)
                time.sleep(0.15)
                attack(eidL)
                log('loop hit at', round(ax,1), round(ay,1), round(az,1))
        time.sleep(0.6)
log('PHASES DONE')
