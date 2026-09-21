from pathlib import Path
import json, zipfile, os
root=Path(__file__).resolve().parents[1]
names=['volt_spear','chain_kusarigama','neon_dualblades','reaper_scythe','impact_gauntlet','ion_railgun','plasma_launcher','arc_caster','cryo_projector','tactical_crossbow']
with zipfile.ZipFile(root/'build/neonward-0.1.0.jar') as jar, zipfile.ZipFile(Path(os.environ['APPDATA'])/'.minecraft/versions/26.2/26.2.jar') as vanilla:
    assert jar.testzip() is None
    language=json.loads(jar.read('assets/neonward/lang/ja_jp.json'))
    for name in names:
        assert 'item.neonward.'+name in language
        definition=json.loads(jar.read(f'assets/neonward/items/{name}.json'))
        assert definition['model']['model']=='neonward:item/'+name
        model=json.loads(jar.read(f'assets/neonward/models/item/{name}.json'))
        assert model['elements'] and model['display']
        for texture in model['textures'].values():
            ns,path=texture.split(':')
            assert f'assets/{ns}/textures/{path}.png' in (vanilla if ns=='minecraft' else jar).namelist(),texture
        for element in model['elements']:
            assert all(-16<=n<=32 for n in element['from']+element['to'])
    for cls in ['ArsenalExpansion','PhoneTravel','PhoneTravelScreen','TelevisionUrl','ClockworkDisplays']:
        assert f'jp/neonward/{cls}.class' in jar.namelist()
print('ASSET_QA_PASS: ten localized models, texture references, bounds, new classes and ZIP integrity')
