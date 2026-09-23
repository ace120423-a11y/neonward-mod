"""Bounded offline accessory screen QA. Copies the latest completed SakuraIntegration world.

Never builds the mod, reads live-world terrain, installs, publishes, or modifies a source world.
Run only after the parent's full asset build and SakuraIntegration completion.
"""
from pathlib import Path
import hashlib,json,os,re,shutil,subprocess,time,zipfile

root=Path(__file__).resolve().parents[1]
mc=Path(os.environ['APPDATA'])/'.minecraft'
jdk=Path(os.environ['LOCALAPPDATA'])/'Packages/Microsoft.4297127D64EC6_8wekyb3d8bbwe/LocalCache/Local/runtime/java-runtime-epsilon/windows-x64/java-runtime-epsilon/bin'
candidates=[]
for directory in root.glob('build/sakura-integration-*'):
    log=directory/'smoke.log'; marker=directory/'world/neonward/sakura_town.json'
    if not log.is_file() or not marker.is_file():continue
    text=log.read_text(encoding='utf-8',errors='replace')
    if 'SAKURA_TEST_COMPLETE' in text and 'ACCESSORY_EQUIPMENT_PASS' in text and 'SAKURA_TEST_FAILED' not in text and json.loads(marker.read_text(encoding='utf-8'))['phase']=='complete':candidates.append(directory)
assert candidates,'A completed SakuraIntegration terrain copy is required first'
source_world=max(candidates,key=lambda p:p.name)/'world'
artifact=root/'build/neonward-0.1.0.jar'
for name in ('SakuraMaterials','SakuraTownPlan','SakuraTown','ShrineServices','ShrineScreen','ShrineRituals','ShrineBlessings','AccessoryEquipment','AccessoryMenu','AccessoryScreen'):
    assert artifact.stat().st_mtime >= (root/f'src/jp/neonward/{name}.java').stat().st_mtime,f'Parent full build required: {name}'
test=root/('build/accessory-client-'+time.strftime('%Y%m%d-%H%M%S'))
(test/'mods').mkdir(parents=True);(test/'natives').mkdir();(test/'qa').mkdir()
digest=lambda path:hashlib.sha256(path.read_bytes()).hexdigest()
assert digest(artifact)==digest(source_world.parent/'mods/neonward.jar'),'Run integration with this exact artifact before visual QA'
before=digest(artifact);shutil.copy2(artifact,test/'mods/neonward.jar')
assert before==digest(artifact)==digest(test/'mods/neonward.jar'),'Artifact changed during snapshot'
with zipfile.ZipFile(test/'mods/neonward.jar') as jar:
    assert jar.testzip() is None
    for name in ('SakuraMaterials','SakuraTownPlan','SakuraTown','ShrineServices','ShrineScreen','ShrineRituals','ShrineBlessings','AccessoryEquipment','AccessoryMenu','AccessoryScreen'):assert f'jp/neonward/{name}.class' in jar.namelist()
    for kind in ('travel','guard','fortune'):
        item=f'shrine_amulet_{kind}'
        assert json.loads(jar.read(f'assets/neonward/items/{item}.json'))['model']['model']==f'neonward:item/{item}'
        assert json.loads(jar.read(f'assets/neonward/models/item/{item}.json'))['elements']
shutil.copy2(mc/'neonward-26.2/mods/fabric-api-0.160.0+26.2.jar',test/'mods')
sodium=list((mc/'neonward-26.2/mods').glob('sodium-*.jar'))
assert len(sodium)==1,'Require exactly one Sodium build for native model compatibility QA'
shutil.copy2(sodium[0],test/'mods')
shutil.copytree(source_world,test/'saves/Sakura-QA')
(test/'ACCESSORY_VISUAL_ONLY').write_text('Isolated disposable world copy only\n',encoding='utf-8')
(test/'build-snapshot.json').write_text(json.dumps(dict(sha256=before,source=str(artifact),world=str(source_world),sodium=sodium[0].name),indent=2),encoding='utf-8')
(test/'options.txt').write_text('renderDistance:12\nsimulationDistance:8\nmaxFps:60\nguiScale:2\nfullscreen:false\npauseOnLostFocus:false\ntutorialStep:none\n',encoding='utf-8')
cp=[mc/'versions/26.2/26.2.jar',*mc.joinpath('libraries').rglob('*.jar')]
for archive in cp:
    if 'natives-windows' not in archive.name or 'arm64' in archive.name or 'x86' in archive.name:continue
    with zipfile.ZipFile(archive) as jar:
        for entry in jar.namelist():
            if entry.endswith('.dll'):(test/'natives'/Path(entry).name).write_bytes(jar.read(entry))
# Compile the addon against the snapshotted release, not potentially stale/shared class output.
compile_cp=str(test/'mods/neonward.jar')+';'+(root/'build/javac.args').read_text(encoding='utf-8').splitlines()[6].strip('"')
subprocess.run([str(jdk/'javac.exe'),'-proc:none','-encoding','UTF-8','-cp',compile_cp,'-d',str(test/'qa'),str(root/'tests/AccessoryVisualQA.java')],check=True)
with zipfile.ZipFile(test/'mods/accessory-visual-qa.jar','w') as jar:
    jar.writestr('fabric.mod.json',json.dumps(dict(schemaVersion=1,id='accessory_visual_qa',version='1',environment='client',entrypoints=dict(client=['jp.neonward.AccessoryVisualQA']))))
    for file in (test/'qa').rglob('*.class'):jar.write(file,file.relative_to(test/'qa'))
index=json.loads((mc/'versions/26.2/26.2.json').read_text())['assetIndex']['id']
args=['-Xmx3G','-Dfile.encoding=UTF-8','-Dneonward.accessory.visual.root='+str(test),'-Djava.library.path='+str(test/'natives'),'-Dfabric.gameJarPath='+str(cp[0]),'-cp',';'.join(map(str,cp)),'net.fabricmc.loader.impl.launch.knot.KnotClient','--username','SakuraQA','--uuid','79bbf5e728a14501846bcb2f028183d8','--accessToken','0','--version','26.2','--gameDir',str(test),'--assetsDir',str(mc/'assets'),'--assetIndex',index,'--width','1280','--height','800','--quickPlaySingleplayer','Sakura-QA']
argfile=test/'java.args';argfile.write_text('\n'.join('"'+arg.replace('\\','/')+'"' for arg in args),encoding='utf-8')
print('ACCESSORY_VISUAL_START',test,'source:',source_world,flush=True)
with (test/'client.log').open('w',encoding='utf-8') as log:
    proc=subprocess.Popen([str(jdk/'java.exe'),'@'+str(argfile)],cwd=test,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    try:proc.wait(timeout=300)
    except subprocess.TimeoutExpired:
        proc.terminate();proc.wait(timeout=20);raise
text=(test/'client.log').read_text(encoding='utf-8',errors='replace')
print(text[-5000:]);print('Client log:',test/'client.log')
assert proc.returncode==0 and 'ACCESSORY_CLIENT_QA_COMPLETE captures=4' in text and 'ACCESSORY_CLIENT_QA_FAILED' not in text,'Sakura visual QA failed'
labels=re.findall(r'ACCESSORY_SCREENSHOT ([^\r\n]+)',text)
images=sorted((test/'screenshots').glob('*.png'),key=lambda p:p.stat().st_mtime_ns)
assert len(labels)==len(set(labels))==len(images)==4,'Missing/duplicate capture'
manifest=[dict(label=label,file=str(file)) for label,file in zip(labels,images)]
(test/'accessory-screenshots.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
print('ACCESSORY_CLIENT_PASS',test/'accessory-screenshots.json')
