from pathlib import Path
import os,zipfile,subprocess,json,shutil
ROOT=Path(__file__).resolve().parent
MC=Path(os.environ['APPDATA'])/'.minecraft'
JDK=Path(os.environ['LOCALAPPDATA'])/'Packages/Microsoft.4297127D64EC6_8wekyb3d8bbwe/LocalCache/Local/runtime/java-runtime-epsilon/windows-x64/java-runtime-epsilon'
deps=ROOT/'build/deps';deps.mkdir(parents=True,exist_ok=True)
api=MC/'neonward-26.2/mods/fabric-api-0.160.0+26.2.jar'
with zipfile.ZipFile(api) as z:
 for n in z.namelist():
  if n.startswith('META-INF/jars/') and n.endswith('.jar'):(deps/Path(n).name).write_bytes(z.read(n))
cp=[*ROOT.joinpath('lib').glob('*.jar'),MC/'versions/26.2/26.2.jar',api,*deps.glob('*.jar'),*MC.joinpath('libraries').rglob('*.jar')]
classes=ROOT/'build/classes';classes.mkdir(parents=True,exist_ok=True)
args=['-proc:none','-encoding','UTF-8','--release','25','-classpath',';'.join(str(p) for p in cp),'-d',str(classes),*[str(p) for p in (ROOT/'src').rglob('*.java')]]
(ROOT/'build/javac.args').write_text('\n'.join('"'+a.replace('\\','/')+'"' for a in args),encoding='utf-8')
r=subprocess.run([str(JDK/'bin/javac.exe'),'@'+str(ROOT/'build/javac.args')]);assert r.returncode==0,'Compilation failed'
out=ROOT/'build/neonward-0.1.0.jar'
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
 for source in [classes,ROOT/'resources']:
  for p in source.rglob('*'):
   if p.is_file():z.write(p,p.relative_to(source))
 with_source=False
print(out)
if '--install' in __import__('sys').argv:
 shutil.copy2(out,MC/'neonward-26.2/mods'/out.name);print('Installed. Restart Minecraft to load the mod.')
