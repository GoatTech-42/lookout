from pathlib import Path
import subprocess,time,os,uuid,hashlib,json
# Loopback-only network namespace setup. No route to the public network.
import socket,fcntl,struct
probe=socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
flags=struct.unpack('16sh',fcntl.ioctl(probe.fileno(),0x8913,struct.pack('16sh',b'lo',0))[:18])[1]
fcntl.ioctl(probe.fileno(),0x8914,struct.pack('16sh',b'lo',flags|1));probe.close()
r=Path(__file__).parent
(r/'move-network-routes.txt').write_text(Path('/proc/net/route').read_text())
assert len(Path('/proc/net/route').read_text().strip().splitlines())==1
server=r/'server';client=r/'client';processes=[]
import signal,ctypes
ctypes.CDLL(None).prctl(36,1,0,0,0)
# This is a disposable offline test identity, not a saved account or access token.
name='MeshRigA';h=bytearray(hashlib.md5(('OfflinePlayer:'+name).encode()).digest());h[6]=(h[6]&15)|48;h[8]=(h[8]&63)|128;uid=str(uuid.UUID(bytes=bytes(h)))
(client/'options.txt').write_text('onboardAccessibility:false\nskipMultiplayerWarning:true\nrenderDistance:3\nsimulationDistance:3\nmaxFps:20\nsoundCategory_master:0.0\n')
cases=server/'config/meshac/cases.json'
if not cases.exists():
 candidates=list((server/'config').rglob('*cases*.json'));assert len(candidates)==1,candidates;cases=candidates[0]
casebytes=cases.read_bytes();(r/'move-case-before.json').write_bytes(casebytes)
log=(r/'move-server.log').open('w');s=subprocess.Popen([str(Path.home()/'jdk25/bin/java'),'-Xms256m','-Xmx1400m','-Dmixin.debug.verbose=true','-jar','fabric-server.jar','nogui'],cwd=server,start_new_session=True,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True);processes.append(s)
x=None;c=None;f=None
try:
 deadline=time.monotonic()+45
 while time.monotonic()<deadline:
  if 'Done (' in (r/'move-server.log').read_text():break
  if s.poll()!=None:raise RuntimeError('server exited')
  time.sleep(.5)
 else:raise RuntimeError('server ready timeout')
 x=subprocess.Popen(['Xvfb',':87','-screen','0','1024x768x24','+extension','GLX','+render','-noreset'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,start_new_session=True);processes.append(x);time.sleep(1)
 env=os.environ.copy();env.update(DISPLAY=':87',LIBGL_ALWAYS_SOFTWARE='1',VK_ICD_FILENAMES='/usr/share/vulkan/icd.d/lvp_icd.x86_64.json')
 cp=(client/'classpath.txt').read_text();f=(r/'move-client.log').open('w')
 cmd=[str(Path.home()/'jdk25/bin/java'),'-Xms256m','-Xmx1800m','--enable-native-access=ALL-UNNAMED','--add-exports','java.base/jdk.internal.misc=ALL-UNNAMED','-cp',cp,'net.minecraft.client.main.Main','--username',name,'--uuid',uid,'--accessToken','test-only-no-account','--version','26.3','--gameDir',str(client),'--assetsDir',str(client/'assets'),'--assetIndex','34','--versionType','release','--quickPlayMultiplayer','127.0.0.1:25587','--width','960','--height','600']
 (r/'test-identity.json').write_text(json.dumps({'name':name,'offlineUUID':uid,'endpoint':'127.0.0.1:25587','realAccount':False},indent=2))
 c=subprocess.Popen(cmd,cwd=client,start_new_session=True,stdout=f,stderr=subprocess.STDOUT,env=env);processes.append(c)
 deadline=time.monotonic()+55
 while time.monotonic()<deadline:
  if c.poll()!=None:break
  if '[ANCHOR-OBSERVER] PONG_POST_HANDLE' in (r/'move-server.log').read_text() and 'handshake=true' in (r/'move-server.log').read_text():
   time.sleep(12);subprocess.run(['xdotool','keydown','w'],env=env,check=True);time.sleep(2);subprocess.run(['xdotool','keyup','w'],env=env,check=True);time.sleep(4);break
  time.sleep(.5)
 subprocess.run(['import','-display',':87','-window','root',str(r/'move-client.png')],timeout=10)
 print('client_exit',c.poll(),'server_exit',s.poll())
finally:
 if s.poll()==None:
  s.stdin.write('stop\n');s.stdin.flush()
  try:s.wait(12)
  except subprocess.TimeoutExpired:s.terminate()
 for p in reversed(processes):
  if p.poll()==None:
   p.terminate()
   try:p.wait(5)
   except subprocess.TimeoutExpired:p.kill();p.wait()
 for p in processes:
  try:os.killpg(p.pid,signal.SIGTERM)
  except ProcessLookupError:pass
 time.sleep(.3)
 for p in processes:
  try:os.killpg(p.pid,signal.SIGKILL)
  except ProcessLookupError:pass
 while True:
  try:
   pid,status=os.waitpid(-1,os.WNOHANG)
   if pid==0:break
  except ChildProcessError:break
 log.close()
 if f:f.close()
 (r/'move-case-after.json').write_bytes(cases.read_bytes())
 print('CASE_BYTES_UNCHANGED',casebytes==cases.read_bytes())
 print('SERVER TAIL');print((r/'move-server.log').read_text()[-15000:]);print('CLIENT TAIL');print((r/'move-client.log').read_text()[-7000:] if (r/'move-client.log').exists() else 'no client')

text=(r/'move-server.log').read_text()
assert '[ANCHOR-OBSERVER] PONG_POST_HANDLE' in text,'No observed anchor PONG'
assert any('PONG_POST_HANDLE' in line and 'handshake=true' in line and 'listenerConnectionSame=true' in line and 'playerRefSame=true' in line for line in text.splitlines()),'Anchor reply/identity not established'
assert 'nativeVersion=26.3 serverVersion=V_26_3' in text,'Native version initialization not established'
assert 'gated=179/179' in text and 'mode=LOG_ONLY veil=false,false,false' in text,'Containment/gates not established'
assert casebytes==cases.read_bytes(),'Case store changed'
print('BOUNDED ANCHOR RUNTIME ASSERTIONS PASS; visual inspection separate')
