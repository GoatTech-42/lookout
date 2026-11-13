#!/usr/bin/env python3
"""Validate native-constructed abilities handoff, excluding wire/codec/UI/permissions."""
from pathlib import Path
import collections,re,sys

def verify(server,client,runner):
    assert 'ABILITIES NATIVE CONSTRUCTED HANDOFF ASSERTIONS PASS packets=2' in runner, 'runner incomplete'
    assert 'CLIENT_SEND_FROZEN_SERVER_QUEUE_DRAINED' in runner and 'CASE_BYTES_UNCHANGED True' in runner
    assert 'MOVEMENT QUALIFICATION PASS' in runner
    assert 'gated=179/179' in server and 'mode=LOG_ONLY veil=false,false,false' in server
    assert 'nativeVersion=26.3 serverVersion=V_26_3' in server
    assert '[ABILITIES-DRIVER] NATIVE_CONSTRUCTED_ONLY entry=ctx.fireChannelRead next=packet_handler eventLoop=true livePLAY=true distinct=true' in server
    assert 'Stopping the server' in server and 'Server closed' in client
    assert 'NATIVE_EXCEPTION' not in server and 'Internal Exception:' not in client
    assert 'DISCONNECT callback' not in server.split('Stopping the server')[0]
    assert server.count('[ABILITIES-OBSERVER] EVENT_META inbound=true currentPlayer=true')==2
    records=collections.defaultdict(list)
    for stage,packet,flying,thread in re.findall(r'\[ABILITIES-OBSERVER\] (DRIVER|ADMITTED_CHANNEL|DISPATCH|EVENT|HANDLER_HEAD|HANDLER_TAIL) packet=(\d+) flying=(true|false) thread=([^\n]+)',server):
        records[packet].append((stage,flying,thread.strip()))
    assert len(records)==2, 'two identities required'
    assert [e[0][1] for e in records.values()]==['true','false'], 'constructed field sequence changed'
    for packet,entries in records.items():
        assert [x[0] for x in entries]==['DRIVER','ADMITTED_CHANNEL','DISPATCH','EVENT','HANDLER_HEAD','HANDLER_HEAD','HANDLER_TAIL'], (packet,entries)
        assert len({x[1] for x in entries})==1, ('field changed',packet)
        assert all('Netty' in x[2] for x in entries[:5]) and all(x[2]=='Server thread' for x in entries[5:]), ('thread mismatch',packet)
    return len(records)

if __name__=='__main__':
    assert len(sys.argv)==4,'server.log client.log runner.log required'
    print('Native-constructed abilities trace PASS identities='+str(verify(*(Path(p).read_text() for p in sys.argv[1:]))))
