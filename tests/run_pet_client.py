"""Production-mod-set pet screenshot QA, in an isolated copy of a leisure test world.

Requires completed run_leisure_integration.py and a fresh parent build. Never installs.
"""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,time,zipfile
from run_leisure_integration import ROOT,MC,JDK,snapshot

def main():
    runs=sorted(ROOT.glob('build/leisure-integration-*'),key=lambda p:p.stat().st_mtime,reverse=True)
    source=next((p for p in runs if (p/'smoke.log').exists() and 'LEISURE_TEST_COMPLETE' in (p/'smoke.log').read_text(encoding='utf-8',errors='replace') and (p/'world/level.dat').exists()),None)
    assert source is not None,'Complete isolated leisure integration first'
    test=ROOT/('build/pet-client-'+time.strftime('%Y%m%d-%H%M%S'));test.mkdir()
    artifact=snapshot(test)
    mods=MC/'neonward-26.2/mods';manifest={}
    for file in mods.glob('*.jar'):
        with zipfile.ZipFile(file) as jar:mod=json.loads(jar.read('fabric.mod.json'))
        if mod['id']=='neonward':continue
        shutil.copy2(file,test/'mods'/file.name);manifest[mod['id']]={'file':file.name,'sha256':hashlib.sha256(file.read_bytes()).hexdigest()}
    assert 'sodium' in manifest and 'iris' in manifest,'Production renderer mods required'
    if (mods/'mcef-libraries').exists():shutil.copytree(mods/'mcef-libraries',test/'mods/mcef-libraries')
    (test/'production-mods.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
    shutil.copytree(source/'world',test/'saves/Pet-QA')
    (test/'options.txt').write_text('renderDistance:4\nsimulationDistance:4\nmaxFps:45\nguiScale:2\nfullscreen:false\npauseOnLostFocus:false\n',encoding='utf-8')
    libs=[MC/'versions/26.2/26.2.jar',*MC.joinpath('libraries').rglob('*.jar')]
    (test/'natives').mkdir()
    for file in libs:
        if 'natives-windows' not in file.name or 'arm64' in file.name or 'x86' in file.name:continue
        with zipfile.ZipFile(file) as jar:
            for name in jar.namelist():
                if name.endswith('.dll'):(test/'natives'/Path(name).name).write_bytes(jar.read(name))
    args=[s.strip().strip('"') for s in (ROOT/'build/javac.args').read_text(encoding='utf-8').splitlines()]
    cp=str(artifact)+';'+args[args.index('-classpath')+1];qa=test/'qa';qa.mkdir()
    subprocess.run([str(JDK/'javac.exe'),'-proc:none','-encoding','UTF-8','-cp',cp,'-d',str(qa),str(ROOT/'tests/PetClientQA.java')],check=True)
    with zipfile.ZipFile(test/'mods/pet-client-qa.jar','w') as jar:
        jar.writestr('fabric.mod.json',json.dumps(dict(schemaVersion=1,id='pet_client_qa',version='1',environment='client',entrypoints=dict(client=['jp.neonward.PetClientQA']))))
        for file in qa.rglob('*.class'):jar.write(file,file.relative_to(qa))
    index=json.loads((MC/'versions/26.2/26.2.json').read_text())['assetIndex']['id']
    args=['-Xmx2G','-Dfile.encoding=UTF-8','-Djava.library.path='+str(test/'natives'),'-Dfabric.gameJarPath='+str(libs[0]),'-cp',';'.join(map(str,libs)),'net.fabricmc.loader.impl.launch.knot.KnotClient','--username','PetQA','--uuid','e042c8622e224d33bcaa35e3c215f10a','--accessToken','0','--version','26.2','--gameDir',str(test),'--assetsDir',str(MC/'assets'),'--assetIndex',index,'--width','1100','--height','760','--quickPlaySingleplayer','Pet-QA']
    argfile=test/'java.args';argfile.write_text('\n'.join('"'+a.replace('\\','/')+'"' for a in args),encoding='utf-8')
    with (test/'client.log').open('w',encoding='utf-8') as log:
        proc=subprocess.Popen([str(JDK/'java.exe'),'@'+str(argfile)],cwd=test,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
        try:proc.wait(timeout=240)
        except subprocess.TimeoutExpired:proc.terminate();proc.wait(timeout=15);raise
    output=(test/'client.log').read_text(encoding='utf-8',errors='replace');print(output[-4500:]);print('Log:',test/'client.log')
    assert proc.returncode==0 and 'PET_CLIENT_QA_COMPLETE' in output,'Pet render QA failed'
    assert len(list((test/'screenshots').glob('*.png')))==10,'Expected two frames for every pet'
    print('PET_CLIENT_PASS',test/'screenshots','Inspect actual geometry in all ten images.')

if __name__=='__main__':main()
