"""Code-native attachment hardware, rarity accents and scope-free gun bases."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'resources/assets/neonward'
ids=['reflex','holo','scope4','scope8','extended_mag','quick_mag','grip','muzzle']
colors=['light_gray','lime','light_blue','purple','orange']
def box(lo,hi,texture='metal'):
    return dict(from_=lo,to=hi,faces={s:dict(uv=[0,0,16,16],texture='#'+texture) for s in ['north','south','east','west','up','down']})
def emit(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,indent=2).replace('"from_"','"from"')+'\n')
for kind,name in enumerate(ids):
    for tier in range(5):
        e=[]
        if kind<4:
            e+=[box([4,2,4],[12,4,12]),box([6,4,5],[10,6,11])]
            depth=4 if kind<2 else 13 if kind==2 else 18
            z=8-depth/2
            # Hollow optic body, inset lens, mounting feet, turret and fasteners.
            width=4 if kind==0 else 5
            for lo,hi in [([8-width,6,z],[9-width,14,z+depth]),([7+width,6,z],[8+width,14,z+depth]),([9-width,6,z],[7+width,7,z+depth]),([9-width,13,z],[7+width,14,z+depth])]:e.append(box(lo,hi))
            e.append(box([9-width,7,z+.2],[7+width,13,z+.35],'lens'))
            e.append(box([4,3,6],[4.6,4.6,7],'accent'));e.append(box([11.4,3,9],[12,4.6,10],'accent'))
            if kind>=2:e+=[box([6,14,6],[10,16,10]),box([12,9,6],[14,12,10]),box([7,16,7],[9,16.5,9],'accent')]
            else:e+=[box([5,13.1,z],[11,14,z+.5],'accent')]
        elif kind<6:
            e+=[box([4,0,5],[12,14,11]),box([3.5,-1,4.5],[12.5,1,11.5]),box([5,14,6],[11,15,10],'accent')]
            for yy in range(2,13,3):e.append(box([3.8,yy,5.5],[4.1,yy+.5,10.5],'accent'))
            if kind==5:e+=[box([4,-4,5],[5,0,11]),box([11,-4,5],[12,0,11]),box([5,-4,5],[11,-3,11])]
        elif kind==6:
            e+=[box([4,12,4],[12,14,12]),box([6,1,5],[10,12,11]),box([5,0,4.5],[11,2,11.5])]
            for yy in range(3,11,2):e.append(box([5.8,yy,5],[10.2,yy+.5,11],'accent'))
        else:
            for lo,hi in [([4,4,0],[6,12,16]),([10,4,0],[12,12,16]),([6,4,0],[10,6,16]),([6,10,0],[10,12,16])]:e.append(box(lo,hi))
            for zz in [2,6,10]:e.append(box([3.8,5,zz],[4.1,11,zz+1],'accent'))
        model=dict(textures=dict(particle='minecraft:block/gray_concrete',metal='minecraft:block/gray_concrete',accent='minecraft:block/'+colors[tier]+'_concrete',lens='minecraft:block/cyan_stained_glass'),elements=e,display=dict(gui=dict(rotation=[22,135,0],scale=[.8,.8,.8]),ground=dict(scale=[.4,.4,.4])))
        emit(root/f'models/item/attachments/{name}_{tier}.json',model)
        emit(root/f'items/attachments/{name}_{tier}.json',{'model':{'type':'minecraft:model','model':f'neonward:item/attachments/{name}_{tier}'}})
    emit(root/f'items/attachment_{name}.json',{'model':{'type':'minecraft:model','model':f'neonward:item/attachments/{name}_0'}})
for name,remove in [('pulse_rifle',[10,11]),('longwatch_sniper',list(range(39,51)))]:
    for suffix in ['', '_body']:
        source=root/f'models/item/{name}.json' if not suffix else root/f'models/item/reload/{name}_body.json'
        model=json.loads(source.read_text())
        original=json.loads((root/f'models/item/{name}.json').read_text())
        excluded=[original['elements'][i] for i in remove]
        model['elements']=[e for e in model['elements'] if e not in excluded]
        id=f'attachments/bare_{name}{suffix}'
        emit(root/f'models/item/{id}.json',model)
        emit(root/f'items/{id}.json',{'model':{'type':'minecraft:model','model':'neonward:item/'+id}})
print('Built 40 attachment models, matching item definitions, and four scope-free gun bases')
