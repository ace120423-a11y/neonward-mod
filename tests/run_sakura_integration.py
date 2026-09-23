"""Build an isolated copy of the existing western terrain. Never changes the live save."""
from pathlib import Path
import os,shutil,subprocess,threading,queue,time,json,zipfile
root=Path(__file__).resolve().parents[1];mc=Path(os.environ['APPDATA'])/'.minecraft'
jdk=Path(os.environ['LOCALAPPDATA'])/'Packages/Microsoft.4297127D64EC6_8wekyb3d8bbwe/LocalCache/Local/runtime/java-runtime-epsilon/windows-x64/java-runtime-epsilon/bin'
test=root/('build/sakura-integration-'+time.strftime('%Y%m%d-%H%M%S'));(test/'mods').mkdir(parents=True)
shutil.copy2(root/'build/neonward-0.1.0.jar',test/'mods/neonward.jar')
shutil.copy2(mc/'neonward-26.2/mods/fabric-api-0.160.0+26.2.jar',test/'mods/fabric-api.jar')
live=Path.home()/'Documents/NeonWardServer';shutil.copy2(live/'eula.txt',test/'eula.txt')
regions=test/'world/dimensions/minecraft/overworld/region';regions.mkdir(parents=True)
for x,z in [(-1,0),(0,0),(0,1),(1,0),(1,1)]:
 src=live/f'world/dimensions/minecraft/overworld/region/r.{x}.{z}.mca'
 if src.exists():shutil.copy2(src,regions/src.name)
(test/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\npause-when-empty-seconds=0\nview-distance=2\nsimulation-distance=2\nmax-tick-time=-1\nlevel-type=minecraft:flat\n',encoding='utf-8')
qa=test/'qa';qa.mkdir();cp=(root/'build/javac.args').read_text(encoding='utf-8').splitlines()[6].strip('"')+';'+str(root/'build/classes')
sources=[root/'tests/WestLandIntegration.java',root/'tests/SakuraIntegration.java',root/'tests/ShrineServicesIntegration.java',root/'tests/ShrineRitualTimeline.java',root/'tests/ShrineRitualIntegration.java',root/'tests/SakuraTownPlanTest.java',root/'tests/AccessoryEquipmentIntegration.java']
subprocess.run([str(jdk/'javac.exe'),'-proc:none','-encoding','UTF-8','-cp',cp,'-d',str(qa),*map(str,sources)],check=True)
subprocess.run([str(jdk/'java.exe'),'-Xmx768M','-Dfile.encoding=UTF-8','-cp',str(qa)+';'+cp,'jp.neonward.SakuraTownPlanTest'],cwd=test,check=True)
with zipfile.ZipFile(test/'mods/sakura-qa.jar','w') as z:
 z.writestr('fabric.mod.json',json.dumps(dict(schemaVersion=1,id='neon_sakura_qa',version='1',environment='server',entrypoints=dict(main=['jp.neonward.SakuraIntegration']))))
 for f in qa.rglob('*.class'):z.write(f,f.relative_to(qa))
cp=[mc/'versions/26.2/26.2.jar',*mc.joinpath('libraries').rglob('*.jar')]
args=['-Xmx3G','-Dfile.encoding=UTF-8','-Dfabric.gameJarPath='+str(cp[0]),'-cp',';'.join(map(str,cp)),'net.fabricmc.loader.impl.launch.knot.KnotServer','--nogui']
argfile=test/'java.args';argfile.write_text('\n'.join('"'+a.replace('\\','/')+'"' for a in args),encoding='utf-8')
p=subprocess.Popen([str(jdk/'java.exe'),'@'+str(argfile)],cwd=test,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace',creationflags=subprocess.CREATE_NO_WINDOW)
lines=[];q=queue.Queue()
def read():
 for line in p.stdout:lines.append(line);q.put(line)
threading.Thread(target=read,daemon=True).start()
try:
 deadline=time.monotonic()+420
 while time.monotonic()<deadline:
  try:line=q.get(timeout=1)
  except queue.Empty:
   if p.poll() is not None:raise RuntimeError('QA server exited')
   continue
  if 'SAKURA_' in line:print(line.rstrip(),flush=True)
  if 'SAKURA_TEST_FAILED' in line:raise RuntimeError('Sakura test failed')
  if 'SAKURA_TEST_COMPLETE' in line:
   result=''.join(lines)
   assert 'SHRINE_RITUAL_SERVER_PASS' in result and 'SHRINE_BLESSING_COMBAT_PASS' in result and 'ACCESSORY_EQUIPMENT_PASS' in result,'Ritual/combat/accessory checks must complete before parent success'
   break
 else:raise TimeoutError('Sakura construction timed out')
finally:
 if p.poll() is None:
  p.stdin.write('stop\n');p.stdin.flush()
  try:p.wait(timeout=40)
  except subprocess.TimeoutExpired:p.terminate();p.wait(timeout=10)
 (test/'smoke.log').write_text(''.join(lines),encoding='utf-8');print('Log:',test/'smoke.log')
