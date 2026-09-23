"""Isolated v54->v55 access migration, obstruction preflight and actual restart.

Default source is the completed v54 QA world. --fresh copies production terrain
read-only and lets both installers run. Never builds/replaces the shared mod jar.
Successful build/sakura-access-*/world plus smoke.log is the visual QA handoff.
"""
from pathlib import Path
import argparse
import json
import os
import queue
import shutil
import subprocess
import threading
import time
import zipfile

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--fresh', action='store_true')
parser.add_argument('--source', type=Path, help='World directory; never modified')
opts = parser.parse_args()
source = (opts.source or (Path.home() / 'Documents/NeonWardServer/world' if opts.fresh else
                         root / 'build/sakura-integration-20260924-010616/world')).resolve()
assert source.is_dir(), source
receipt = source / 'neonward/sakura_town.json'
assert opts.fresh == (not receipt.exists()), 'Use a completed v54 world by default, or an unbuilt source with --fresh'
if receipt.exists():
    assert json.loads(receipt.read_text(encoding='utf-8'))['phase'] == 'complete'
assert not (source / 'neonward/sakura_access_v1.json').exists(), 'Source must precede access upgrade'
mc = Path(os.environ['APPDATA']) / '.minecraft'
jdk = Path(os.environ['LOCALAPPDATA']) / 'Packages/Microsoft.4297127D64EC6_8wekyb3d8bbwe/LocalCache/Local/runtime/java-runtime-epsilon/windows-x64/java-runtime-epsilon/bin'
test = root / ('build/sakura-access-' + time.strftime('%Y%m%d-%H%M%S') + ('-fresh' if opts.fresh else ''))
test.mkdir(parents=True, exist_ok=False)
print('Output:', test, flush=True)
(test / 'mods').mkdir()
shutil.copy2(root / 'build/neonward-0.1.0.jar', test / 'mods/neonward.jar')
shutil.copy2(mc / 'neonward-26.2/mods/fabric-api-0.160.0+26.2.jar', test / 'mods/fabric-api.jar')
qa = test / 'qa'
qa.mkdir()
cp = (root / 'build/javac.args').read_text(encoding='utf-8').splitlines()[6].strip('"') + ';' + str(root / 'build/classes')
names = ['WestLandIntegration', 'SakuraIntegration', 'ShrineServicesIntegration', 'ShrineRitualTimeline',
         'ShrineRitualIntegration', 'AccessoryEquipmentIntegration', 'SakuraAccessIntegration']
subprocess.run([str(jdk / 'javac.exe'), '-proc:none', '-encoding', 'UTF-8', '-cp', cp, '-d', str(qa),
                *[str(root / 'tests' / (n + '.java')) for n in names]], check=True)
with zipfile.ZipFile(test / 'mods/sakura-access-qa.jar', 'w') as archive:
    archive.writestr('fabric.mod.json', json.dumps(dict(schemaVersion=1, id='neon_sakura_access_qa', version='1',
        environment='server', depends={'neonward': '*'}, entrypoints=dict(main=['jp.neonward.SakuraAccessIntegration']))))
    for f in qa.rglob('*.class'):
        archive.write(f, f.relative_to(qa))
gamecp = [mc / 'versions/26.2/26.2.jar', *mc.joinpath('libraries').rglob('*.jar')]


def prepare(folder):
    folder.mkdir(parents=True, exist_ok=True)
    if folder != test:
        shutil.copytree(test / 'mods', folder / 'mods')
    shutil.copytree(source, folder / 'world', ignore=shutil.ignore_patterns('session.lock'))
    (folder / 'world/ACCESS_QA_COPY').write_text(str(source), encoding='utf-8')
    (folder / 'eula.txt').write_text('eula=true\n', encoding='utf-8')
    (folder / 'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\n'
        'pause-when-empty-seconds=0\nview-distance=2\nsimulation-distance=2\nmax-tick-time=-1\n', encoding='utf-8')


def run(folder, mode):
    args = ['-Xmx3G', '-Dfile.encoding=UTF-8', '-Dsakura.access.mode=' + mode,
            '-Dfabric.gameJarPath=' + str(gamecp[0]), '-cp', ';'.join(map(str, gamecp)),
            'net.fabricmc.loader.impl.launch.knot.KnotServer', '--nogui']
    argfile = folder / ('java-' + mode + '.args')
    argfile.write_text('\n'.join('"' + a.replace('\\', '/') + '"' for a in args), encoding='utf-8')
    p = subprocess.Popen([str(jdk / 'java.exe'), '@' + str(argfile)], cwd=folder,
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, encoding='utf-8', errors='replace', creationflags=subprocess.CREATE_NO_WINDOW)
    lines, output = [], queue.Queue()

    def read():
        for line in p.stdout:
            lines.append(line)
            output.put(line)

    reader = threading.Thread(target=read, daemon=True)
    reader.start()
    try:
        deadline = time.monotonic() + 600
        while time.monotonic() < deadline:
            try:
                line = output.get(timeout=1)
            except queue.Empty:
                if p.poll() is not None:
                    raise RuntimeError('QA server exited before completion')
                continue
            if 'SAKURA_' in line:
                print(line.rstrip(), flush=True)
            if 'SAKURA_ACCESS_TEST_FAILED' in line:
                raise RuntimeError('Access QA failed: ' + mode)
            if 'SAKURA_ACCESS_TEST_COMPLETE mode=' + mode in line:
                break
        else:
            raise TimeoutError('Access QA exceeded 600 seconds: ' + mode)
    finally:
        if p.poll() is None:
            p.stdin.write('stop\n')
            p.stdin.flush()
            try:
                p.wait(timeout=45)
            except subprocess.TimeoutExpired:
                p.terminate()
                p.wait(timeout=10)
        reader.join(timeout=5)
        (folder / ('smoke-' + mode + '.log')).write_text(''.join(lines), encoding='utf-8')
        with (test / 'smoke.log').open('a', encoding='utf-8') as log:
            log.write(''.join(lines))
    assert p.returncode == 0, f'Unclean server shutdown: {p.returncode}'


prepare(test)
run(test, 'fresh' if opts.fresh else 'upgrade')
run(test, 'restart')
if not opts.fresh:
    for mode in ('blocked', 'blocked-headroom'):
        blocked = test / (mode + '-copy')
        prepare(blocked)
        run(blocked, mode)
with (test / 'smoke.log').open('a', encoding='utf-8') as log:
    log.write('SAKURA_ACCESS_PASS mode=' + ('fresh' if opts.fresh else 'upgrade') + '\n')
print('SAKURA_ACCESS_PASS', test, flush=True)
