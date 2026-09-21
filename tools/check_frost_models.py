from pathlib import Path
import hashlib,json,math
from PIL import Image
root=Path(__file__).resolve().parents[1]/'resources/assets/neonward'
catalog=json.loads((root/'bosses/frost_catalog.json').read_text(encoding='utf-8'))
assert len(catalog)==30
signatures=set()
for entry in catalog:
 data=(root/f"bosses/{entry['model']}.json").read_bytes();signatures.add(hashlib.sha256(data).hexdigest())
 parts=json.loads(data)['parts'];names=[p['name'] for p in parts]
 assert len(names)==len(set(names)),entry['floor']
 total=0
 for p in parts:
  assert p['parent'] is None or p['parent'] in names
  assert len(p['vertices'])%4==0
  for v in p['vertices']:
   assert len(v)==8 and all(math.isfinite(c) for c in v)
   assert 0<=v[6]<=1 and 0<=v[7]<=1
  total+=len(p['vertices'])//4
 assert 0<total<12000,(entry['floor'],total)
 assert entry['bounds'][1][1]<9
 print(f"{entry['floor']:02}: {len(parts)} parts / {total} quads / {entry['model']}")
assert len(signatures)==30
for name in ['frost_materials','frost_materials_glow']:
 with Image.open(root/f'textures/entity/{name}.png') as im:assert im.size==(1024,256) and im.mode=='RGBA'
print('PASS: 30 unique model files, valid hierarchy/UVs, bounded geometry, material atlases')
