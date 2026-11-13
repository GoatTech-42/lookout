from pathlib import Path
import subprocess,time,os,uuid,hashlib,json
r=Path(__file__).parent;server=r/'server';client=r/'client';processes=[]
# This is a disposable offline test identity, not a saved account or access token.
name='MeshRigA';h=bytearray(hashlib.md5(('OfflinePlayer:'+name).encode()).digest());h[6]=(h[6]&15)|48;h[8]=(h[8]&63)|128;uid=str(uuid.UUID(bytes=bytes(h)))
(client/'options.txt').write_text('onboardAccessibility:false\nskipMultiplayerWarning:true\nrenderDistance:3\nsimulationDistance:3\nmaxFps:20\nsoundCategory_master:0.0\n')
log=(r/'lifecycle-server.log').open('w');s=subprocess.Popen([str(Path.home()/'jdk25/bin/java'),'-Xms256m','-Xmx1400m','-Dmixin.debug.verbose=true','-jar','fabric-server.jar','nogui'],cwd=server,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True);processes.append(s)
x=None;c=None;f=None
try:
 deadline=time.monotonic()+45
 while time.monotonic()<deadline:
  if 'Done (' in (r/'lifecycle-server.log').read_text():break
  if s.poll()!=None:raise RuntimeError('server exited')
  time.sleep(.5)
 else:raise RuntimeError('server ready timeout')
 x=subprocess.Popen(['Xvfb',':87','-screen','0','1024x768x24','+extension','GLX','+render','-noreset'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL);processes.append(x);time.sleep(1)
 env=os.environ.copy();env.update(DISPLAY=':87',LIBGL_ALWAYS_SOFTWARE='1',VK_ICD_FILENAMES='/usr/share/vulkan/icd.d/lvp_icd.x86_64.json')
 cp=(client/'classpath.txt').read_text();f=(r/'lifecycle-client.log').open('w')
 cmd=[str(Path.home()/'jdk25/bin/java'),'-Xms256m','-Xmx1800m','--enable-native-access=ALL-UNNAMED','--add-exports','java.base/jdk.internal.misc=ALL-UNNAMED','-cp',cp,'net.minecraft.client.main.Main','--username',name,'--uuid',uid,'--accessToken','test-only-no-account','--version','26.3','--gameDir',str(client),'--assetsDir',str(client/'assets'),'--assetIndex','34','--versionType','release','--quickPlayMultiplayer','127.0.0.1:25587','--width','960','--height','600']
 (r/'test-identity.json').write_text(json.dumps({'name':name,'offlineUUID':uid,'endpoint':'127.0.0.1:25587','realAccount':False},indent=2))
 c=subprocess.Popen(cmd,cwd=client,stdout=f,stderr=subprocess.STDOUT,env=env);processes.append(c)
 deadline=time.monotonic()+55
 while time.monotonic()<deadline:
  if c.poll()!=None:break
  if 'refCurrent=true gated=179/180' in (r/'lifecycle-server.log').read_text():
   time.sleep(3);break
  time.sleep(.5)
 subprocess.run(['import','-display',':87','-window','root',str(r/'lifecycle-client.png')],timeout=10)
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
 log.close()
 if f:f.close()
 print('SERVER TAIL');print((r/'lifecycle-server.log').read_text()[-15000:]);print('CLIENT TAIL');print((r/'lifecycle-client.log').read_text()[-7000:] if (r/'lifecycle-client.log').exists() else 'no client')
