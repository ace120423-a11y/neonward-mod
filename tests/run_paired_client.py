"""Isolated offline render QA. No production worlds, account tokens or installs."""
from pathlib import Path
import os,json,zipfile,shutil,subprocess,time,sys
focus=next((a.split('=',1)[1] for a in sys.argv if a.startswith('--focus=')),'')
motion='--motion' in sys.argv or bool(focus)
chain='--chain' in sys.argv
vfx='--vfx' in sys.argv
gacha='--gacha' in sys.argv
leisure='--leisure' in sys.argv
qa_class='AttachmentQA' if '--attachments' in sys.argv else 'GunReloadQA' if '--reload' in sys.argv else 'GunPresentationQA' if '--guns' in sys.argv else 'HousingSalesQA' if '--housing' in sys.argv else 'CasinoGachaQA' if gacha else 'ElementVfxQA' if vfx else 'ChainPullQA' if chain else 'WeaponMotionQA' if motion else 'PairedClientQA'
if '--volcano' in sys.argv:qa_class='VolcanoQA'
if '--volcano-tour' in sys.argv:qa_class='VolcanoTourQA'
if leisure:qa_class='LeisureQA'
root=Path(__file__).resolve().parents[1]
mc=Path(os.environ['APPDATA'])/'.minecraft'
jdk=Path(os.environ['LOCALAPPDATA'])/'Packages/Microsoft.4297127D64EC6_8wekyb3d8bbwe/LocalCache/Local/runtime/java-runtime-epsilon/windows-x64/java-runtime-epsilon/bin'
test=root/('build/paired-client-'+time.strftime('%Y%m%d-%H%M%S'))
(test/'mods').mkdir(parents=True);(test/'natives').mkdir()
shutil.copy2(root/'build/neonward-0.1.0.jar',test/'mods')
with zipfile.ZipFile(test/'mods/neonward-0.1.0.jar') as check:
 assert check.testzip() is None,'Build must finish before starting QA'
 if leisure:
  for name in ('LeisureShop','LeisureSites','PetRenderer','WeaponDisplayRenderer','TrainingRange'):
   assert 'jp/neonward/'+name+'.class' in check.namelist(),'Parent full build is required before leisure QA: '+name
shutil.copy2(mc/'neonward-26.2/mods/fabric-api-0.160.0+26.2.jar',test/'mods')
if leisure:
 sodium=list((mc/'neonward-26.2/mods').glob('sodium-*.jar'))
 assert len(sodium)==1,'Leisure render QA requires exactly one installed Sodium jar to test the custom pet render path'
 shutil.copy2(sodium[0],test/'mods')
 print('Leisure renderer compatibility:',sodium[0].name)
world=sorted(root.glob('build/land-integration-*/world'),key=lambda p:p.stat().st_mtime)[-1]
if leisure:
 candidates=[p for p in root.glob('build/travel-integration-*/world') if (p/'neonward/eatery_decor.json').is_file()]
 assert candidates,'Leisure terrain QA requires a completed decorated travel-world copy'
 world=max(candidates,key=lambda p:p.stat().st_mtime)
 print('Leisure original restaurant terrain:',world)
if '--volcano-tour' in sys.argv:world=sorted(root.glob('build/volcano-integration-*/world'),key=lambda p:p.stat().st_mtime)[-1]
shutil.copytree(world,test/'saves/Paired-QA')
(test/'options.txt').write_text('renderDistance:4\nsimulationDistance:4\nmaxFps:60\nguiScale:2\nfullscreen:false\npauseOnLostFocus:false\n',encoding='utf-8')
if leisure:
 (test/'options.txt').write_text('renderDistance:10\nsimulationDistance:8\nmaxFps:60\nguiScale:2\nfullscreen:false\npauseOnLostFocus:false\n',encoding='utf-8')
if '--left' in sys.argv:
 with (test/'options.txt').open('a',encoding='utf-8') as options:options.write('mainHand:left\n')
cp=[mc/'versions/26.2/26.2.jar',*mc.joinpath('libraries').rglob('*.jar')]
for archive in cp:
 if 'natives-windows' not in archive.name or 'arm64' in archive.name or 'x86' in archive.name:continue
 with zipfile.ZipFile(archive) as z:
  for entry in z.namelist():
   if entry.endswith('.dll'):(test/'natives'/Path(entry).name).write_bytes(z.read(entry))
compile_cp=(root/'build/javac.args').read_text().splitlines()[6].strip('"')+';'+str(root/'build/classes')
(test/'qa').mkdir()
subprocess.run([str(jdk/'javac.exe'),'-proc:none','-encoding','UTF-8','-cp',compile_cp,'-d',str(test/'qa'),str(root/('tests/'+qa_class+'.java'))],check=True)
with zipfile.ZipFile(test/'mods/paired-client-qa.jar','w') as z:
 z.writestr('fabric.mod.json',json.dumps(dict(schemaVersion=1,id='paired_client_qa',version='1',environment='client',entrypoints=dict(client=['jp.neonward.'+qa_class]))))
 for f in (test/'qa').rglob('*.class'):z.write(f,f.relative_to(test/'qa'))
index=json.loads((mc/'versions/26.2/26.2.json').read_text())['assetIndex']['id']
args=['-Xmx2G','-Dfile.encoding=UTF-8','-Djava.library.path='+str(test/'natives'),'-Dfabric.gameJarPath='+str(cp[0]),'-cp',';'.join(map(str,cp)),'net.fabricmc.loader.impl.launch.knot.KnotClient','--username','PairedQA','--uuid','8f28429a60694c33971b12d8e1fdaa75','--accessToken','0','--version','26.2','--gameDir',str(test),'--assetsDir',str(mc/'assets'),'--assetIndex',index,'--width','1100','--height','760','--quickPlaySingleplayer','Paired-QA']
args.insert(0,'-Dneonward.qa.focus='+focus)
argfile=test/'java.args';argfile.write_text('\n'.join('"'+a.replace('\\','/')+'"' for a in args),encoding='utf-8')
with (test/'client.log').open('w',encoding='utf-8') as log:
 proc=subprocess.Popen([str(jdk/'java.exe'),'@'+str(argfile)],cwd=test,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
 try:proc.wait(timeout=240 if motion or leisure else 150)
 except subprocess.TimeoutExpired:proc.terminate();proc.wait();raise
text=(test/'client.log').read_text(encoding='utf-8',errors='replace')
print('Client log:',test/'client.log')
print(text[-4500:])
assert proc.returncode==0 and 'PAIRED_CLIENT_QA_COMPLETE' in text,'Client QA failed'
if leisure:
 import re
 plans=re.findall(r'LEISURE_CAPTURE_PLAN captures=(\d+) catalog=(\d+)',text)
 assert len(plans)==1,'Missing/duplicate leisure capture plan'
 leisure_count,catalog_count=map(int,plans[0])
 assert catalog_count==18 and leisure_count==18+(catalog_count+3)//4,'Expected complete eighteen-item catalog coverage'
assert len(list((test/'screenshots').glob('*.png')))==(leisure_count if leisure else 33 if '--volcano' in sys.argv else 12 if '--attachments' in sys.argv else 44 if '--reload' in sys.argv else 12 if '--guns' in sys.argv else 10 if vfx else 6 if chain or gacha else 4*len(focus.split(',')) if focus else 100 if motion else 4)
if leisure:
 labels=[line.split('LEISURE_SCREENSHOT ',1)[1].strip() for line in text.splitlines() if 'LEISURE_SCREENSHOT ' in line]
 images=sorted((test/'screenshots').glob('*.png'),key=lambda p:p.stat().st_mtime_ns)
 assert len(labels)==leisure_count and len(set(labels))==leisure_count,'Missing/duplicate leisure screenshot labels'
 assert labels[18:]==[f'{19+first//4:02d}-food-icons-{first+1:02d}-{min(first+4,catalog_count):02d}' for first in range(0,catalog_count,4)],'Incomplete icon-page manifest'
 manifest=[dict(label=label,file=str(path)) for label,path in zip(labels,images)]
 (test/'leisure-screenshots.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
 print('Visual review manifest:',test/'leisure-screenshots.json')
print('PAIRED_CLIENT_PASS',test/'screenshots')
