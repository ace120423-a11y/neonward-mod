from pathlib import Path
import json, zipfile, os
root=Path(__file__).resolve().parents[1]
names=['volt_spear','chain_kusarigama','neon_dualblades','reaper_scythe','impact_gauntlet','ion_railgun','plasma_launcher','arc_caster','cryo_projector','tactical_crossbow']
with zipfile.ZipFile(root/'build/neonward-0.1.0.jar') as jar, zipfile.ZipFile(Path(os.environ['APPDATA'])/'.minecraft/versions/26.2/26.2.jar') as vanilla:
    assert jar.testzip() is None
    language=json.loads(jar.read('assets/neonward/lang/ja_jp.json'))
    aliases={s['sprite']:s['resource'] for s in json.loads(jar.read('assets/minecraft/atlases/items.json'))['sources'] if s['type']=='minecraft:single'}
    def check_texture(texture):
        assert texture.startswith('neonward:item/'),f'Mixed atlas texture: {texture}'
        source=aliases.get(texture,texture)
        ns,path=source.split(':')
        assert f'assets/{ns}/textures/{path}.png' in (vanilla if ns=='minecraft' else jar).namelist(),source
    for name in names:
        assert 'item.neonward.'+name in language
        definition=json.loads(jar.read(f'assets/neonward/items/{name}.json'))
        if name=='neon_dualblades':
            assert definition['model']['fallback']['model']=='neonward:item/'+name
            for side in ['left','right']:
                single=json.loads(jar.read(f'assets/neonward/models/item/neon_dualblade_{side}.json'))
                assert len(single['elements'])==4
                for texture in single['textures'].values():check_texture(texture)
        else:assert definition['model']['model']=='neonward:item/'+name
        model=json.loads(jar.read(f'assets/neonward/models/item/{name}.json'))
        assert model['elements'] and model['display']
        for texture in model['textures'].values():
            check_texture(texture)
        for element in model['elements']:
            assert all(-16<=n<=32 for n in element['from']+element['to'])
    shield=json.loads(jar.read('assets/neonward/models/item/sentinel_shield.json'))
    for texture in shield['textures'].values():check_texture(texture)
    blocking=json.loads(jar.read('assets/neonward/models/item/sentinel_shield_blocking.json'))
    assert blocking['parent']=='neonward:item/sentinel_shield'
    condition=json.loads(jar.read('assets/neonward/items/sentinel_shield.json'))['model']
    assert condition['on_true']['model']=='neonward:item/sentinel_shield_blocking'
    assert condition['on_false']['model']=='neonward:item/sentinel_shield'
    for cls in ['ArsenalExpansion','PhoneTravel','PhoneTravelScreen','TelevisionUrl','ClockworkDisplays']:
        assert f'jp/neonward/{cls}.class' in jar.namelist()
print('ASSET_QA_PASS: ten weapons + shield, item-atlas aliases, source textures, blocking variant, bounds and ZIP integrity')
