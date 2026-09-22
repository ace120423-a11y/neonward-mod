"""Runs against an isolated temporary server; never opens or edits the live world.
Requires the repository's existing Minecraft/Fabric installation and accepted EULA.
Run python build.py first. Logs and the generated test world remain under build/.
"""
from pathlib import Path
import os, shutil, subprocess, threading, queue, time, json, zipfile
root=Path(__file__).resolve().parents[1]
mc=Path(os.environ['APPDATA'])/'.minecraft'
jdk=Path(os.environ['LOCALAPPDATA'])/'Packages/Microsoft.4297127D64EC6_8wekyb3d8bbwe/LocalCache/Local/runtime/java-runtime-epsilon/windows-x64/java-runtime-epsilon/bin'
test=root/('build/land-integration-'+time.strftime('%Y%m%d-%H%M%S'))
(test/'mods').mkdir(parents=True)
shutil.copy2(root/'build/neonward-0.1.0.jar',test/'mods/neonward.jar')
shutil.copy2(mc/'neonward-26.2/mods/fabric-api-0.160.0+26.2.jar',test/'mods/fabric-api.jar')
shutil.copy2(Path.home()/'Documents/NeonWardServer/eula.txt',test/'eula.txt')
(test/'server.properties').write_text('server-port=0\nonline-mode=false\nview-distance=2\nsimulation-distance=2\nmax-tick-time=-1\nlevel-type=minecraft:flat\n',encoding='utf-8')
qa=test/'qa';qa.mkdir()
compile_cp=(root/'build/javac.args').read_text(encoding='utf-8').splitlines()[6].strip('"')+';'+str(root/'build/classes')
subprocess.run([str(jdk/'javac.exe'),'-proc:none','-encoding','UTF-8','-cp',compile_cp,'-d',str(qa),str(root/'tests/WestLandIntegration.java'),str(root/'tests/PairedHandsIntegration.java'),str(root/'tests/PairedEffectsIntegration.java'),str(root/'tests/ChainPullIntegration.java'),str(root/'tests/CasinoGachaIntegration.java'),str(root/'tests/HousingSalesIntegration.java'),str(root/'tests/LandTestAddon.java')],check=True)
with zipfile.ZipFile(test/'mods/land-qa.jar','w') as z:
 z.writestr('fabric.mod.json',json.dumps(dict(schemaVersion=1,id='neon_land_qa',version='1',environment='server',entrypoints=dict(main=['jp.neonward.LandTestAddon']))))
 for f in qa.rglob('*.class'):z.write(f,f.relative_to(qa))
cp=[mc/'versions/26.2/26.2.jar',*mc.joinpath('libraries').rglob('*.jar')]
args=['-Xmx2G','-Dfile.encoding=UTF-8','-Dfabric.gameJarPath='+str(cp[0]),'-cp',';'.join(map(str,cp)),'net.fabricmc.loader.impl.launch.knot.KnotServer','--nogui']
argfile=test/'java.args';argfile.write_text('\n'.join('"'+a.replace('\\','/')+'"' for a in args),encoding='utf-8')
p=subprocess.Popen([str(jdk/'java.exe'),'@'+str(argfile)],cwd=test,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace')
lines=[];q=queue.Queue()
def read():
 for line in p.stdout:lines.append(line);q.put(line)
threading.Thread(target=read,daemon=True).start()
try:
 deadline=time.monotonic()+180
 while time.monotonic()<deadline:
  try:line=q.get(timeout=1)
  except queue.Empty:
   if p.poll() is not None:raise RuntimeError('Test server exited')
   continue
  if 'LAND_TEST_FAILED' in line:raise RuntimeError('Land integration failed; inspect smoke.log')
  if 'LAND_TEST_COMPLETE' in line:print('PASS: land integration');break
 else:raise TimeoutError('Land integration timeout')
finally:
 if p.poll() is None:
  p.stdin.write('stop\n');p.stdin.flush()
  try:p.wait(timeout=40)
  except subprocess.TimeoutExpired:p.terminate();p.wait(timeout=10)
 (test/'smoke.log').write_text(''.join(lines),encoding='utf-8')
 print('Log:',test/'smoke.log')
