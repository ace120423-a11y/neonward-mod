from pathlib import Path
import os,zipfile,subprocess,shutil
root=Path(__file__).resolve().parent;mc=Path(os.environ['APPDATA'])/'.minecraft'
jdk=Path(os.environ['LOCALAPPDATA'])/'Packages/Microsoft.4297127D64EC6_8wekyb3d8bbwe/LocalCache/Local/runtime/java-runtime-epsilon/windows-x64/java-runtime-epsilon'
test=root/'build/startup-check';(test/'mods').mkdir(parents=True,exist_ok=True)
shutil.copy2(root/'build/neonward-0.1.0.jar',test/'mods')
shutil.copy2(mc/'neonward-26.2/mods/fabric-api-0.160.0+26.2.jar',test/'mods')
cp=[mc/'versions/26.2/26.2.jar',*mc.joinpath('libraries').rglob('*.jar')]
args=['-Dfile.encoding=UTF-8','-Dfabric.gameJarPath='+str(cp[0]),'-cp',';'.join(map(str,cp)),'net.fabricmc.loader.impl.launch.knot.KnotServer','--initSettings','--nogui']
argfile=test/'java.args';argfile.write_text('\n'.join('"'+a.replace('\\','/')+'"' for a in args),encoding='utf-8')
result=subprocess.run([str(jdk/'bin/java.exe'),'@'+str(argfile)],cwd=test,capture_output=True,timeout=90)
(test/'startup-output.txt').write_bytes(result.stdout+result.stderr)
print((result.stdout+result.stderr).decode('utf-8',errors='replace')[-6500:]);print('Exit:',result.returncode)
