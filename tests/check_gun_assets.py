"""Validate original mono gun audio and its packaged references."""
from pathlib import Path
import sys,json,zipfile
root=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(root/'build/audio-tools'))
import soundfile as sf
import numpy as np
assets=root/'resources/assets/neonward'
events=json.loads((assets/'sounds.json').read_text())
assert len(events)==14
hashes=set()
with zipfile.ZipFile(root/'build/neonward-0.1.0.jar') as jar:
    assert jar.testzip() is None
    for key,event in events.items():
        entry=event['sounds'][0]
        assert entry['attenuation_distance']==24
        path=assets/'sounds'/(entry['name'].split(':')[1]+'.ogg')
        samples,rate=sf.read(path)
        assert rate==32000 and samples.ndim==1 and .1<=len(samples)/rate<=1.01
        assert np.isfinite(samples).all() and .01<float(np.sqrt(np.mean(samples*samples)))<.6
        assert float(np.abs(samples).max())<.95
        data=path.read_bytes();hashes.add(data)
        assert jar.read('assets/neonward/'+str(path.relative_to(assets)).replace('\\','/'))==data
assert len(hashes)==14
print('GUN_ASSET_PASS: 14 distinct original mono Oggs, levels, durations, 24-block attenuation, packaged references')
