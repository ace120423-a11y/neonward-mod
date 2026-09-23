"""Backpack server integration in a new disposable world; never builds or installs.

Run after Main's completed build: python tests/run_backpack_integration.py
Use --compile-only to prepare/check the isolated QA addon without starting Minecraft.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import queue
import shutil
import subprocess
import threading
import time
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MC = Path(os.environ['APPDATA']) / '.minecraft'
JDK = Path(os.environ['LOCALAPPDATA']) / 'Packages/Microsoft.4297127D64EC6_8wekyb3d8bbwe/LocalCache/Local/runtime/java-runtime-epsilon/windows-x64/java-runtime-epsilon/bin'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--compile-only', action='store_true')
    options = parser.parse_args()
    source = ROOT / 'build/neonward-0.1.0.jar'
    assert source.is_file(), 'Main must complete the shared build first'
    for name in ('BackpackEquipment', 'BackpackMenu', 'BackpackShop'):
        assert source.stat().st_mtime >= (ROOT / f'src/jp/neonward/{name}.java').stat().st_mtime, f'Fresh build required: {name}'
    test = ROOT / ('build/backpack-integration-' + time.strftime('%Y%m%d-%H%M%S'))
    (test / 'mods').mkdir(parents=True, exist_ok=False)
    digest = hashlib.sha256(source.read_bytes()).hexdigest()
    artifact = test / 'mods/neonward.jar'
    shutil.copy2(source, artifact)
    assert digest == hashlib.sha256(artifact.read_bytes()).hexdigest() == hashlib.sha256(source.read_bytes()).hexdigest(), 'Build changed during snapshot'
    with zipfile.ZipFile(artifact) as jar:
        for name in ('BackpackEquipment', 'BackpackMenu', 'BackpackShop'):
            assert f'jp/neonward/{name}.class' in jar.namelist()
    (test / 'build-snapshot.json').write_text(json.dumps({'sha256': digest, 'source': str(source)}, indent=2), encoding='utf-8')
    shutil.copy2(MC / 'neonward-26.2/mods/fabric-api-0.160.0+26.2.jar', test / 'mods/fabric-api.jar')
    javac = [line.strip().strip('"') for line in (ROOT / 'build/javac.args').read_text(encoding='utf-8').splitlines()]
    cp = str(artifact) + ';' + javac[javac.index('-classpath') + 1]
    qa = test / 'qa'
    qa.mkdir()
    subprocess.run([str(JDK / 'javac.exe'), '-proc:none', '-encoding', 'UTF-8', '-cp', cp,
                    '-d', str(qa), str(ROOT / 'tests/BackpackIntegration.java')], check=True)
    with zipfile.ZipFile(test / 'mods/backpack-qa.jar', 'w') as jar:
        jar.writestr('fabric.mod.json', json.dumps(dict(schemaVersion=1, id='neon_backpack_qa', version='1',
            environment='server', depends={'neonward': '*'}, entrypoints=dict(main=['jp.neonward.BackpackIntegration']))))
        for path in qa.rglob('*.class'):
            jar.write(path, path.relative_to(qa))
    print('BACKPACK_QA_LAUNCHABLE', test, flush=True)
    if options.compile_only:
        return
    eula = Path.home() / 'Documents/NeonWardServer/eula.txt'
    assert eula.is_file() and 'eula=true' in eula.read_text().lower(), 'Existing EULA acceptance required'
    shutil.copy2(eula, test / 'eula.txt')
    (test / 'BACKPACK_QA_ONLY').write_text('Disposable fixture world, not production.\n', encoding='utf-8')
    (test / 'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\n'
        'pause-when-empty-seconds=0\nview-distance=2\nsimulation-distance=2\nmax-tick-time=-1\n'
        'level-type=minecraft:flat\nspawn-protection=0\n', encoding='utf-8')
    jars = [MC / 'versions/26.2/26.2.jar', *MC.joinpath('libraries').rglob('*.jar')]
    args = ['-Xmx2G', '-Dfile.encoding=UTF-8', '-Dneonward.backpack.qa.root=' + str(test),
            '-Dfabric.gameJarPath=' + str(jars[0]), '-cp', ';'.join(map(str, jars)),
            'net.fabricmc.loader.impl.launch.knot.KnotServer', '--nogui']
    argfile = test / 'java.args'
    argfile.write_text('\n'.join('"' + arg.replace('\\', '/') + '"' for arg in args), encoding='utf-8')
    process = subprocess.Popen([str(JDK / 'java.exe'), '@' + str(argfile)], cwd=test, stdin=subprocess.PIPE,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding='utf-8', errors='replace',
        creationflags=subprocess.CREATE_NO_WINDOW)
    lines, output = [], queue.Queue()

    def read():
        for line in process.stdout:
            lines.append(line)
            output.put(line)

    reader = threading.Thread(target=read, daemon=True)
    reader.start()
    try:
        deadline = time.monotonic() + 240
        while time.monotonic() < deadline:
            try:
                line = output.get(timeout=1)
            except queue.Empty:
                if process.poll() is not None:
                    raise RuntimeError('Backpack server exited before completion')
                continue
            if 'BACKPACK_' in line:
                print(line.rstrip(), flush=True)
            if 'BACKPACK_TEST_FAILED' in line:
                raise RuntimeError('Backpack integration failed; inspect ' + str(test / 'smoke.log'))
            if 'BACKPACK_TEST_COMPLETE' in line:
                break
        else:
            raise TimeoutError('Backpack integration exceeded 240 seconds')
    finally:
        if process.poll() is None:
            try:
                process.stdin.write('stop\n')
                process.stdin.flush()
                process.wait(timeout=40)
            except (BrokenPipeError, subprocess.TimeoutExpired):
                process.terminate()
                process.wait(timeout=10)
        reader.join(timeout=5)
        (test / 'smoke.log').write_text(''.join(lines), encoding='utf-8')
        print('Log:', test / 'smoke.log', flush=True)
    assert process.returncode == 0, 'Server did not shut down cleanly'


if __name__ == '__main__':
    main()
