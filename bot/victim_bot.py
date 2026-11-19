#!/usr/bin/env python3
# minimal 26.3 (protocol 777) offline bot: legit movement, then provoke phases.
import socket, struct, zlib, threading, time, math, uuid as uuidlib, sys

HOST, PORT = '127.0.0.1', 25565
import os as _os
NAME = _os.environ.get('BOTNAME','ProvokeBot')
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
                elif pid == 0x6A:  # Set Health
                    hp = struct.unpack('>f', data[off:off+4])[0]
                    log('HEALTH', round(hp,1))
                elif pid == 0x20:
                    log('PLAY DISCONNECT raw:', data[off:off+300]); disconnected.set(); return
        except Exception as e:
            log('handler error pid', hex(pid), e)

threading.Thread(target=reader, daemon=True).start()

# --- movement phases
def wait_state(s, timeout=60):
    t0 = time.time()
    while state != s and not disconnected.is_set():
        if time.time() - t0 > timeout: log('timeout waiting for', s); return False
        time.sleep(0.05)
    return state == s

import os
NOMOVE = os.environ.get('NOMOVE')
ONEMOVE = os.environ.get('ONEMOVE')
def tick_send(x, y, z, onground):
    if NOMOVE: return
    c.send(0x0D)  # Client Tick End: required before each new position packet in 26.3
    c.send(0x1E, struct.pack('>ddd', x, y, z) + bytes([0x01 if onground else 0x00]))

if not wait_state('play'): sys.exit(1)
while pos is None and not disconnected.is_set(): time.sleep(0.05)
if disconnected.is_set(): sys.exit(1)
anchor = list(pos)
def A():
    with pos_lock: return anchor[0], anchor[1], anchor[2]
log('anchor', *anchor)
t = 0
SILENCE = 600  # 10 min from lookout client seen
import os as _os
phase = 'post' if _os.environ.get('STILL') else 'legit'; phase_start = None
if ONEMOVE:
    time.sleep(3)
    with pos_lock: ax, ay, az = anchor
    mode = os.environ.get('MODE', 'one')
    if mode == 'rate':
        log('rate test: identical pos at 20Hz for 15s')
        for i in range(300):
            tick_send(ax, ay, az, True); time.sleep(0.05)
        log('survived rate test'); sys.exit(0)
    if mode == 'drift':
        log('drift test: 0.1/tick for 15s')
        for i in range(300):
            tick_send(ax + 0.1*i, ay, az, True); time.sleep(0.05)
        log('survived drift test'); sys.exit(0)
    log('sending one identical move')
    tick_send(ax, ay, az, True)
    time.sleep(20)
    log('still alive after one move'); sys.exit(0)
while not disconnected.is_set():
    time.sleep(0.05); t += 1
    if lookout_seen_at and phase_start is None:
        phase_start = time.time()
    el = (time.time() - phase_start) if phase_start else 0
    cx, gy, cz = A()
    if phase == 'legit':
        ang = t * 0.02
        tick_send(cx - 8 + 8*math.cos(ang), gy, cz + 8*math.sin(ang), True)
        if phase_start and el > SILENCE:
            phase = 'flight'; phase_start = time.time(); fly = gy
            log('PHASE flight')
    elif phase == 'flight':
        fly = min(fly + 0.08, gy + 6)
        ang = t * 0.02
        tick_send(cx - 6 + 6*math.cos(ang), fly, cz + 6*math.sin(ang), False)
        if el > 90: phase = 'gap1'; phase_start = time.time(); log('PHASE gap->speed')
    elif phase in ('gap1', 'gap2'):
        ang = t * 0.02
        tick_send(cx - 8 + 8*math.cos(ang), gy, cz + 8*math.sin(ang), True)
        if el > 20:
            phase = 'speed' if phase == 'gap1' else 'step'; phase_start = time.time(); log('PHASE', phase)
    elif phase == 'speed':
        d = min((el % 3.0) * 14.0, 25)
        k = int(el // 3.0) % 4; ang = k * math.pi / 2
        tick_send(cx + d*math.cos(ang), gy, cz + d*math.sin(ang), True)
        if el > 60: phase = 'gap2'; phase_start = time.time(); log('PHASE gap->step')
    elif phase == 'step':
        up = (t // 20) % 2 == 0
        tick_send(cx, gy + (1.2 if up else 0.0), cz, True)
        if el > 60: phase = 'post'; phase_start = time.time(); log('PHASE post-legit')
    elif phase == 'post':
        ang = t * 0.02
        tick_send(cx - 8 + 8*math.cos(ang), gy, cz + 8*math.sin(ang), True)
        if el > 60: log('PHASES DONE'); break
    if t % 1200 == 0: log('alive, phase', phase)
log('bot exiting')