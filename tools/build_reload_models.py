"""Split existing code-native weapon meshes into body, removable magazine, and action.
Build permanent mechanisms and identical articulated reload components. No new items.
"""
from pathlib import Path
import json,copy
ROOT=Path(__file__).resolve().parents[1]/'resources/assets/neonward'
parts={'pulse_rifle':([8,9],[]),'kestrel_pistol':([20,21],[0,2,*range(26,36)]),'oni_handcannon':([20,21],[0,2,*range(26,36)]),'wisp_compact':([20,21],[0,2,*range(26,36)]),'longwatch_sniper':([51],[]),'storm_machinegun':(list(range(39,46)),[]),'ion_railgun':([6,7],[]),'plasma_launcher':([5,6],[]),'arc_caster':([6],[]),'cryo_projector':([3,4],[]),'tactical_crossbow':([4],[5])}
for name,(mag,action) in parts.items():
    original=json.loads((ROOT/f'models/item/{name}.json').read_text())
    original['elements']=[e for e in original['elements'] if not e.get('name','').startswith('reload_mechanism_')]
    # Only one cryogenic service tank is removed; the other stays fixed as the pressure reservoir.
    if name=='cryo_projector': mag=[3]
    def box(lo,hi):
        e=copy.deepcopy(original['elements'][0]);e['from']=lo;e['to']=hi;e.pop('rotation',None);return e
    mechanisms={
      'pulse_rifle':[box([10.8,9,3],[13,10,5])],
      'longwatch_sniper':[box([7,9,1],[9,10.8,7]),box([8,9,2],[12.5,10,3]),box([12,7.5,1.5],[13.5,10,3.5])],
      'storm_machinegun':[box([10.8,8,2],[14,9.5,4])],
      'ion_railgun':[box([5.5,14.5,7],[10.5,15.5,9])],
      'plasma_launcher':[box([12.8,10,8],[15.5,11,10])],
      'arc_caster':[box([6,15,5],[10,16,7])],
      'cryo_projector':[box([5,14,8],[11,15,10])],
    }
    if name=='longwatch_sniper': original['elements'][0]['to'][1]=9
    if name=='storm_machinegun': original['elements'][0]['to'][1]=10.5
    fixed=[]
    if name in ('pulse_rifle','longwatch_sniper','storm_machinegun'):
        fixed=[box([10.9,8.5,0.5],[11.3,8.9,7]),box([10.9,10.1,0.5],[11.3,10.5,7])]
    if name=='cryo_projector':
        fixed=[box([1.7,2.5,2.7],[5.3,3.2,12.3]),box([1.7,10.5,2.7],[5.3,11.2,12.3])]
    for suffix,indices in [('body',[i for i in range(len(original['elements'])) if i not in mag+action]),('part',mag),('aux',action)]:
        model=copy.deepcopy(original);model['elements']=[copy.deepcopy(original['elements'][i]) for i in indices]
        if suffix=='body':model['elements']+=copy.deepcopy(fixed)
        if suffix=='part' and name in ('kestrel_pistol','oni_handcannon','wisp_compact'):
            inner=copy.deepcopy(original['elements'][20]);inner['from']=[6.5,-1,0.5];inner['to']=[9.5,5.5,3.5];model['elements'].append(inner)
        target=ROOT/f'models/item/reload/{name}_{suffix}.json';target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(model,indent=2)+'\n')
        target=ROOT/f'items/reload/{name}_{suffix}.json';target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps({'model':{'type':'minecraft:model','model':f'neonward:item/reload/{name}_{suffix}'}},indent=2)+'\n')
    cover=[box([5,10.5,4],[11,11.8,13])] if name=='storm_machinegun' else []
    for suffix,elements in [('bolt',mechanisms.get(name,[])),('cover',cover)]:
        model=copy.deepcopy(original);model['elements']=elements
        (ROOT/f'models/item/reload/{name}_{suffix}.json').write_text(json.dumps(model,indent=2)+'\n')
        (ROOT/f'items/reload/{name}_{suffix}.json').write_text(json.dumps({'model':{'type':'minecraft:model','model':f'neonward:item/reload/{name}_{suffix}'}},indent=2)+'\n')
    for i,e in enumerate(copy.deepcopy(fixed+mechanisms.get(name,[])+cover)):
        e['name']=f'reload_mechanism_{i}';original['elements'].append(e)
    (ROOT/f'models/item/{name}.json').write_text(json.dumps(original,indent=2)+'\n')
print('Built 11 articulated sets and matching permanent gun mechanisms')
