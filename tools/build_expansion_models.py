"""Code-native low-poly weapon models, reusing the arsenal's material textures."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]/'resources/assets/neonward'
base=json.loads((ROOT/'models/item/kurosame_katana.json').read_text())
gun=json.loads((ROOT/'models/item/kestrel_pistol.json').read_text())
weapons={
 'volt_spear':('ヴォルト・スピア','Volt Spear',[(7,-12,7,9,19,9,'dark'),(7.4,-10,6.8,8.6,18,7,'cyan'),(5,17,6,11,19,10,'gold'),(6,19,7,10,27,9,'silver'),(7,27,7,9,31,9,'cyan')]),
 'chain_kusarigama':('チェイン・鎖鎌','Chain Kusarigama',[(7,-7,7,9,16,9,'wood'),(7,14,7,22,17,9,'silver'),(19,8,7,22,15,9,'edge'),(8,-10,7,13,-8,9,'gold'),(12,-13,7,14,-9,9,'silver'),(13,-15,6,19,-12,10,'dark')]),
 'neon_dualblades':('ネオン・双剣','Neon Twinblades',[(2,-7,7,4,2,9,'dark'),(0,2,6,6,4,10,'gold'),(2,4,7,4,24,9,'silver'),(1.5,4,7,2,23,9,'cyan'),(12,-3,7,14,6,9,'dark'),(10,6,6,16,8,10,'gold'),(12,8,7,14,28,9,'silver'),(14,8,7,14.5,27,9,'pink')]),
 'reaper_scythe':('リーパー・大鎌','Reaper Scythe',[(7,-12,7,9,23,9,'dark'),(7.4,-10,6.8,8.6,21,7,'pink'),(-8,21,7,9,25,9,'silver'),(-11,17,7,-6,23,9,'edge'),(-13,11,7,-10,19,9,'edge'),(-8,20.5,6.8,7,21,7,'pink')]),
 'impact_gauntlet':('インパクト・ガントレット','Impact Gauntlet',[(3,0,3,13,9,13,'dark'),(2,8,2,14,16,14,'armor'),(2,15,1,5,20,5,'silver'),(6,15,1,9,20,5,'silver'),(10,15,1,13,20,5,'silver'),(1,10,5,4,15,13,'gold'),(4,10,1.5,12,13,2,'cyan')]),
 'ion_railgun':('イオン・レールガン','Ion Railgun',[(5,5,-12,11,11,16,'dark'),(3,9,-14,5,12,11,'silver'),(11,9,-14,13,12,11,'silver'),(5,10,-13,6,12,8,'cyan'),(10,10,-13,11,12,8,'cyan'),(6,-2,10,10,7,14,'armor'),(6,11,6,10,15,12,'dark'),(7,12,5,9,14,6,'cyan')]),
 'plasma_launcher':('プラズマ・ランチャー','Plasma Launcher',[(3,5,-6,13,15,16,'armor'),(2,7,-12,14,13,-5,'dark'),(4,6,-13,12,14,-11,'pink'),(5,7,-13.5,11,13,-13,'cyan'),(6,-3,9,10,6,14,'dark'),(1,7,5,3,14,13,'gold'),(13,7,5,15,14,13,'gold')]),
 'arc_caster':('アーク・キャスター','Arc Caster',[(5,4,0,11,11,15,'dark'),(3,7,-10,5,10,5,'gold'),(11,7,-10,13,10,5,'gold'),(2,9,-12,6,14,-8,'cyan'),(10,9,-12,14,14,-8,'cyan'),(6,-3,10,10,6,14,'armor'),(6,11,4,10,15,11,'silver')]),
 'cryo_projector':('クライオ・冷凍銃','Cryo Projector',[(4,5,-3,12,12,15,'ivory'),(6,6,-13,10,11,0,'silver'),(3,5,-14,13,13,-11,'cyan'),(2,1,3,5,12,12,'cyan'),(11,1,3,14,12,12,'cyan'),(6,-3,10,10,5,14,'dark'),(5,12,7,11,14,13,'armor')]),
 'tactical_crossbow':('タクティカル・クロスボウ','Tactical Crossbow',[(6,6,-7,10,10,17,'dark'),(-8,7,-4,24,10,-1,'silver'),(-10,7,-1,-7,10,4,'armor'),(23,7,-1,26,10,4,'armor'),(-8,8,3,24,8.3,3.3,'cyan'),(7,10,-12,9,11,12,'gold'),(6,-2,10,10,7,14,'dark')]),
}
for name,(ja,en,parts) in weapons.items():
    elements=[]
    for x,y,z,X,Y,Z,texture in parts:
        assert all(-16<=v<=32 for v in (x,y,z,X,Y,Z))
        elements.append(dict(from_=[x,y,z],to=[X,Y,Z],faces={side:{'texture':'#'+texture,'uv':[2,2,14,14]} for side in ['up','down','north','south','east','west']}))
    elements=[{('from' if k=='from_' else k):v for k,v in e.items()} for e in elements]
    template=base if list(weapons).index(name)<5 else gun
    # material_* are item-atlas aliases, not missing PNG files. Keep these aliases:
    # mixing direct block-atlas sprites with item sprites breaks baking in 26.2.
    textures=dict(base['textures'])
    model={'textures':textures,'elements':elements,'display':template['display']}
    (ROOT/f'models/item/{name}.json').write_text(json.dumps(model,indent=2),encoding='utf-8')
    (ROOT/f'items/{name}.json').write_text(json.dumps({'model':{'type':'minecraft:model','model':f'neonward:item/{name}'}},indent=2),encoding='utf-8')
for lang,index in [('ja_jp',0),('en_us',1)]:
    path=ROOT/f'lang/{lang}.json'; data=json.loads(path.read_text(encoding='utf-8'))
    for name,details in weapons.items():data['item.neonward.'+name]=details[index]
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Generated ten distinct low-poly models and localization; existing materials reused.')
