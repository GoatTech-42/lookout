#!/usr/bin/env python3
"""Validate the finite native held-item scenario, not invalid-slot policy."""
from pathlib import Path
import collections
import re
import sys

def verify(server, client, runner):
    assert 'HELD FINITE NATIVE ASSERTIONS PASS' in runner, 'runner did not complete held assertions'
    assert 'CLIENT_SEND_FROZEN_SERVER_QUEUE_DRAINED' in runner, 'shutdown drain not established'
    assert 'MOVEMENT QUALIFICATION PASS' in runner and 'OUTBOUND FINITE NATIVE ASSERTIONS PASS' in runner
    assert 'CASE_BYTES_UNCHANGED True' in runner
    assert 'gated=179/179' in server and 'mode=LOG_ONLY veil=false,false,false' in server
    assert 'nativeVersion=26.3 serverVersion=V_26_3' in server
    assert 'Stopping the server' in server and 'Server closed' in client
    assert 'NATIVE_EXCEPTION' not in server and 'Internal Exception:' not in client
    assert 'DISCONNECT callback' not in server.split('Stopping the server')[0]
    records = collections.defaultdict(list)
    for stage, packet, slot, thread in re.findall(r'\[HELD-OBSERVER\] (ADMITTED_CHANNEL|DISPATCH|CONVERT|HANDLER_HEAD|HANDLER_TAIL) packet=(\d+) slot=(\d+) thread=([^\n]+)', server):
        records[packet].append((stage, int(slot), thread.strip()))
    assert len(records) == 4, ('expected four driven hotbar packets', records)
    assert [r[0][1] for r in records.values()] == [1, 2, 4, 0], 'driven slot sequence changed'
    for packet, entries in records.items():
        assert [x[0] for x in entries] == ['ADMITTED_CHANNEL', 'DISPATCH', 'CONVERT', 'HANDLER_HEAD', 'HANDLER_HEAD', 'HANDLER_TAIL'], (packet, entries)
        assert len({x[1] for x in entries}) == 1, ('slot changed between stages', packet)
        assert all('Netty' in x[2] for x in entries[:4]), ('ingress not on network thread', packet)
        assert all(x[2] == 'Server thread' for x in entries[4:]), ('server replay not on server thread', packet)
    return len(records)

if __name__ == '__main__':
    assert len(sys.argv) == 4, 'server.log client.log runner.log required'
    print('Finite native held trace PASS packets=' + str(verify(*(Path(p).read_text() for p in sys.argv[1:]))))
