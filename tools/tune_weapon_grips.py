"""Normalize gun barrel direction in held contexts; preserve meshes and GUI icons."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'resources/assets/neonward/models/item'
ids=['pulse_rifle','kestrel_pistol','oni_handcannon','wisp_compact','longwatch_sniper','storm_machinegun','ion_railgun','plasma_launcher','arc_caster','cryo_projector','tactical_crossbow']
for name in ids:
    path=root/(name+'.json');model=json.loads(path.read_text(encoding='utf-8'))
    for side in ('right','left'):
        model['display']['firstperson_'+side+'hand']={'rotation':[0,0,0],'translation':[0,0,-2],'scale':[.65]*3}
        model['display']['thirdperson_'+side+'hand']={'rotation':[0,0,0],'translation':[0,2,-1],'scale':[.65]*3}
    path.write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Aligned eleven gun grips and barrels for both hands.')
path=root/'reaper_scythe.json';model=json.loads(path.read_text(encoding='utf-8'))
# The head extends along negative X: the sword template pointed it back at the holder.
for side,sign in [('right',1),('left',-1)]:
    model['display']['firstperson_'+side+'hand']={'rotation':[0,-65*sign,-8*sign],'translation':[0,0,-3],'scale':[.55]*3}
    model['display']['thirdperson_'+side+'hand']={'rotation':[0,-90*sign,0],'translation':[0,1,-1],'scale':[.62]*3}
path.write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Turned the scythe blade away from the holder in both hands.')
