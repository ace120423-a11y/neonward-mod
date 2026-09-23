"""Compile only pet work into build/pet-check and run isolated tests. Never package/install."""
from pathlib import Path
import os
import subprocess
import json

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/pet-check'
JDK=Path(os.environ['LOCALAPPDATA'])/'Packages/Microsoft.4297127D64EC6_8wekyb3d8bbwe/LocalCache/Local/runtime/java-runtime-epsilon/windows-x64/java-runtime-epsilon/bin'

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    args=[line.strip().strip('"') for line in (ROOT/'build/javac.args').read_text(encoding='utf-8').splitlines()]
    cp=args[args.index('-classpath')+1]+';'+str(ROOT/'build/classes')
    sources=list((ROOT/'src/jp/neonward').glob('Pet*.java'))+list((ROOT/'tests').glob('Pet*.java'))
    sources.append(ROOT/'src/jp/neonward/MarketLedger.java')
    compile_args=['-proc:none','-encoding','UTF-8','--release','25','-classpath',cp,'-sourcepath',str(OUT/'empty-sourcepath'),'-d',str(OUT/'classes')]+[str(s) for s in sources]
    argfile=OUT/'javac.args'
    argfile.write_text('\n'.join('"'+a.replace('\\','/')+'"' for a in compile_args),encoding='utf-8')
    subprocess.run([str(JDK/'javac.exe'),'@'+str(argfile)],check=True)
    subprocess.run([str(JDK/'java.exe'),'-cp',str(OUT/'classes')+';'+cp,'jp.neonward.PetPurchaseTest'],check=True)
    for name in ['dog','cat','snake','crow','robot']:
        parts=json.loads((ROOT/f'resources/assets/neonward/pets/{name}.json').read_text())['parts']
        seen=set()
        for part in parts:
            assert part['name'] not in seen
            assert part.get('parent') is None or part['parent'] in seen
            assert len(part['p'])==3
            assert 'size' not in part or (len(part['size'])==3 and min(part['size'])>0)
            seen.add(part['name'])
        print(f'PASS {name}: {len(parts)} native model parts, valid hierarchy')
    print('PASS isolated pet compile; no jar or world modified')

if __name__=='__main__':main()
