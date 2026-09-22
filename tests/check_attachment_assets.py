from pathlib import Path
import json,zipfile
root=Path(__file__).resolve().parents[1]
assets=root/'resources/assets/neonward'
names=['reflex','holo','scope4','scope8','extended_mag','quick_mag','grip','muzzle']
with zipfile.ZipFile(root/'build/neonward-0.1.0.jar') as jar:
    assert jar.testzip() is None
    for name in names:
        for tier in range(5):
            model=f'assets/neonward/models/item/attachments/{name}_{tier}.json'
            item=f'assets/neonward/items/attachments/{name}_{tier}.json'
            data=json.loads(jar.read(model))
            assert data['elements'] and data['textures']['particle']
            for e in data['elements']:
                assert all(a<b for a,b in zip(e['from'],e['to']))
                for face in e['faces'].values(): assert face['texture'][1:] in data['textures']
            assert json.loads(jar.read(item))['model']['model']==f'neonward:item/attachments/{name}_{tier}'
    for name in ['pulse_rifle','longwatch_sniper']:
        for suffix in ['', '_body']:
            assert f'assets/neonward/items/attachments/bare_{name}{suffix}.json' in jar.namelist()
print('ATTACHMENT_ASSETS_PASS: 40 complete hardware models, item links, texture slots, scope replacement bases, ZIP')
