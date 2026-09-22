"""Validate articulated assets and deterministic structural generation."""
from pathlib import Path
import json,hashlib,subprocess,sys
root=Path(__file__).resolve().parents[1]
assets=root/'resources/assets/neonward'
names=['pulse_rifle','kestrel_pistol','oni_handcannon','wisp_compact','longwatch_sniper','storm_machinegun','ion_railgun','plasma_launcher','arc_caster','cryo_projector','tactical_crossbow']
paths=[assets/f'models/item/{n}.json' for n in names]+list((assets/'models/item/reload').glob('*.json'))+list((assets/'items/reload').glob('*.json'))
before={p:hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
subprocess.run([sys.executable,str(root/'tools/build_reload_models.py')],check=True)
assert all(hashlib.sha256(p.read_bytes()).hexdigest()==h for p,h in before.items()),'Generator must be idempotent'
for name in names:
    for suffix in ['body','part','aux','bolt','cover']:
        p=assets/f'models/item/reload/{name}_{suffix}.json'
        model=json.loads(p.read_text())
        assert 'elements' in model
        for e in model['elements']:
            assert all(a<b for a,b in zip(e['from'],e['to'])),(name,suffix,e)
        item=json.loads((assets/f'items/reload/{name}_{suffix}.json').read_text())
        assert item['model']['model']==f'neonward:item/reload/{name}_{suffix}'
    if name not in ['kestrel_pistol','oni_handcannon','wisp_compact','tactical_crossbow']:
        assert json.loads((assets/f'models/item/reload/{name}_bolt.json').read_text())['elements']
print('RELOAD_ASSETS_PASS: 11 structures, 55 articulated models, links, dimensions, deterministic generation')
