"""Standalone five-feature QA. Uses a completed build snapshot, never builds/installs.

python tests/run_leisure_integration.py [--compile-only]
Creates only build/leisure-integration-*/; never reads a production world.
"""
from pathlib import Path
import argparse, hashlib, json, os, queue, shutil, subprocess, threading, time, zipfile

ROOT=Path(__file__).resolve().parents[1]
MC=Path(os.environ['APPDATA'])/'.minecraft'
JDK=Path(os.environ['LOCALAPPDATA'])/'Packages/Microsoft.4297127D64EC6_8wekyb3d8bbwe/LocalCache/Local/runtime/java-runtime-epsilon/windows-x64/java-runtime-epsilon/bin'

def snapshot(test):
    source=ROOT/'build/neonward-0.1.0.jar'
    assert source.exists(),'Parent must finish full build first'
    # Pet flight/Sodium checks must never silently run against the old pet implementation.
    for name in ('PetFlight','PetEntity','PetRenderer','PetCompanions','LeisureShop','LeisureSites'):
        assert source.stat().st_mtime >= (ROOT/f'src/jp/neonward/{name}.java').stat().st_mtime, f'Fresh full build required: {name}'
    before=hashlib.sha256(source.read_bytes()).hexdigest()
    dest=test/'mods/neonward.jar';dest.parent.mkdir(parents=True)
    shutil.copy2(source,dest)
    assert hashlib.sha256(dest.read_bytes()).hexdigest()==before==hashlib.sha256(source.read_bytes()).hexdigest(),'Build changed during snapshot'
    with zipfile.ZipFile(dest) as jar:
        assert jar.testzip() is None
        assert 'jp/neonward/PetFlight.class' in jar.namelist()
        for kind in ('dog','cat','snake','crow','robot'):
            parts=json.loads(jar.read(f'assets/neonward/pets/{kind}.json'))['parts'];seen=set()
            for part in parts:
                assert part['name'] not in seen and (part.get('parent') is None or part['parent'] in seen)
                assert 'size' not in part or min(part['size'])>0
                seen.add(part['name'])
        assert jar.read('assets/neonward/pets/white.png').startswith(b'\x89PNG\r\n\x1a\n')
    (test/'build-snapshot.json').write_text(json.dumps({'sha256':before,'source':str(source)},indent=2),encoding='utf-8')
    return dest

def main():
    args=argparse.ArgumentParser();args.add_argument('--compile-only',action='store_true');opt=args.parse_args()
    test=ROOT/('build/leisure-integration-'+time.strftime('%Y%m%d-%H%M%S'))
    test.mkdir(parents=True,exist_ok=False)
    artifact=snapshot(test)
    shutil.copy2(MC/'neonward-26.2/mods/fabric-api-0.160.0+26.2.jar',test/'mods/fabric-api.jar')
    lines=[s.strip().strip('"') for s in (ROOT/'build/javac.args').read_text(encoding='utf-8').splitlines()]
    cp=str(artifact)+';'+lines[lines.index('-classpath')+1]
    qa=test/'qa';qa.mkdir()
    subprocess.run([str(JDK/'javac.exe'),'-proc:none','-encoding','UTF-8','-cp',cp,'-d',str(qa),str(ROOT/'tests/WestLandIntegration.java'),str(ROOT/'tests/TrainingIntegration.java'),str(ROOT/'tests/LeisureIntegration.java')],check=True)
    with zipfile.ZipFile(test/'mods/leisure-qa.jar','w') as jar:
        jar.writestr('fabric.mod.json',json.dumps(dict(schemaVersion=1,id='neon_leisure_qa',version='1',environment='server',entrypoints=dict(main=['jp.neonward.LeisureIntegration']))))
        for f in qa.rglob('*.class'):jar.write(f,f.relative_to(qa))
    print('LEISURE_QA_LAUNCHABLE',test,flush=True)
    if opt.compile_only:return
    eula=Path.home()/'Documents/NeonWardServer/eula.txt'
    assert eula.exists() and 'eula=true' in eula.read_text().lower(),'Existing accepted server EULA required'
    shutil.copy2(eula,test/'eula.txt')
    (test/'LEISURE_QA_ONLY').write_text('isolated fixture world only\n',encoding='utf-8')
    (test/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nview-distance=2\nsimulation-distance=2\nmax-tick-time=-1\nlevel-type=minecraft:flat\nspawn-protection=0\n',encoding='utf-8')
    jars=[MC/'versions/26.2/26.2.jar',*MC.joinpath('libraries').rglob('*.jar')]
    options=['-Xmx2G','-Dfile.encoding=UTF-8','-Dneonward.leisure.qa.root='+str(test),'-Dfabric.gameJarPath='+str(jars[0]),'-cp',';'.join(map(str,jars)),'net.fabricmc.loader.impl.launch.knot.KnotServer','--nogui']
    argfile=test/'java.args';argfile.write_text('\n'.join('"'+a.replace('\\','/')+'"' for a in options),encoding='utf-8')
    p=subprocess.Popen([str(JDK/'java.exe'),'@'+str(argfile)],cwd=test,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace',creationflags=subprocess.CREATE_NO_WINDOW)
    output=[];q=queue.Queue()
    def read():
        for line in p.stdout:output.append(line);q.put(line)
    reader=threading.Thread(target=read,daemon=True);reader.start()
    try:
        deadline=time.monotonic()+240
        while time.monotonic()<deadline:
            try:line=q.get(timeout=1)
            except queue.Empty:
                if p.poll() is not None:raise RuntimeError('Isolated server exited')
                continue
            if 'LEISURE_' in line:print(line.strip(),flush=True)
            if 'LEISURE_TEST_FAILED' in line:raise RuntimeError('Leisure integration failed; inspect '+str(test/'smoke.log'))
            if 'LEISURE_TEST_COMPLETE' in line:break
        else:raise TimeoutError('Leisure integration timeout')
    finally:
        if p.poll() is None:
            try:p.stdin.write('stop\n');p.stdin.flush();p.wait(timeout=40)
            except (BrokenPipeError,subprocess.TimeoutExpired):p.terminate();p.wait(timeout=10)
        reader.join(timeout=3)
        (test/'smoke.log').write_text(''.join(output),encoding='utf-8')
        print('Log:',test/'smoke.log',flush=True)

if __name__=='__main__':main()
