"""Code-native segmented shield mesh; no custom renderer or ticking entities."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'resources/assets/neonward'
parts=[]
def box(name,a,b,mat,glow=False):
    e={'name':name,'from':a,'to':b,'faces':{side:{'texture':'#'+mat,'uv':[0,0,16,16]} for side in ('north','south','east','west','up','down')}}
    if glow:e['light_emission']=12
    parts.append(e)
# Stepped, tapered silhouette with a raised front and an actual rear grip.
box('main ballistic shell',[2,0,7],[14,20,9],'dark')
box('shoulder plate',[3,20,7],[13,22,9],'edge')
box('lower tapered plate',[3,-2,7],[13,0,9],'edge')
box('heel',[5,-4,7],[11,-2,9],'armor')
for side,x in [('left',1),('right',14)]:
    box(side+' outer rail',[x,2,6.8],[x+1,19,9.3],'edge')
    xx=x+.32
    box(side+' cyan guide',[xx,3,6.58],[xx+.32,18,6.8],'cyan',True)
for row,(lo,hi) in enumerate([(1,6),(6.5,12),(12.5,18.5)]):
    for side,(loX,hiX) in enumerate([(2.4,7.4),(8.6,13.6)]):
        box(f'floating armor panel {row}-{side}',[loX,lo,6.45],[hiX,hi,7.05],'armor')
        box(f'panel seam {row}-{side}',[loX,lo,6.3],[hiX,lo+.16,6.46],'edge')
box('reactor central spine',[7.6,0,6.2],[8.4,20,6.7],'edge')
box('reactor outer housing',[5.5,8,5.85],[10.5,13,6.5],'dark')
box('reactor metal bezel',[6,8.5,5.55],[10,12.5,5.9],'edge')
box('reactor luminous core',[6.6,9.1,5.32],[9.4,11.9,5.58],'cyan',True)
box('reactor core iris',[7.3,9.8,5.2],[8.7,11.2,5.35],'dark')
box('reactor status diode',[7.75,10.25,5.1],[8.25,10.75,5.22],'cyan',True)
box('upper crest',[5.2,18.9,6.1],[10.8,20.3,6.5],'edge')
box('crest light',[6,19.3,5.95],[10,19.75,6.15],'cyan',True)
for x in [3,12.3]:
    for y in [2,7,14,18]:box(f'rivet {x}-{y}',[x,y,6.15],[x+.6,y+.6,6.5],'edge')
for i in range(3):
    box(f'warning stripe {i}',[4+i*.8,.3,6.1],[4.45+i*.8,1.1,6.45],'amber')
    box(f'vent {i}',[10.6,3+i*.75,6.1],[12.9,3.3+i*.75,6.5],'dark')
box('rear forearm pad',[4.5,5,9],[11.5,15,9.5],'armor')
for y in [6,13]:box(f'grip mount {y}',[7, y,9.5],[9,y+1,11.3],'edge')
box('rear vertical grip',[7.35,7,10.4],[8.65,13,11.7],'dark')
for y in [8,10,12]:box(f'grip rib {y}',[7.2,y,10.3],[8.8,y+.25,11.8],'armor')
def pose(rotation,translation,scale):return {'rotation':rotation,'translation':translation,'scale':[scale]*3}
display={
 'gui':pose([12,150,-8],[0,-1,0],.62),
 'ground':pose([0,0,0],[0,3,0],.35),
 'fixed':pose([0,180,0],[0,-1,0],.7),
 'thirdperson_righthand':pose([0,90,0],[1,3,0],.8),
 'thirdperson_lefthand':pose([0,90,0],[1,3,0],.8),
 'firstperson_righthand':pose([0,180,8],[-2,-1,-3],.85),
 'firstperson_lefthand':pose([0,180,8],[2,-1,-3],.85)}
model={'gui_light':'front','textures':{k:'neonward:item/arsenal_'+k for k in ['armor','edge','cyan','dark']},'display':display,'elements':parts}
model['textures'].update(amber='neonward:item/material_yellow_concrete',particle='neonward:item/arsenal_armor')
blocking={'parent':'neonward:item/sentinel_shield','display':{
 'thirdperson_righthand':pose([25,130,0],[-1,4,2],.8),
 'thirdperson_lefthand':pose([25,130,0],[1,4,2],.8),
 'firstperson_righthand':pose([0,180,-8],[-5,2,-5],.9),
 'firstperson_lefthand':pose([0,180,-8],[5,2,-5],.9)}}
item={'model':{'type':'minecraft:condition','property':'minecraft:using_item','on_true':{'type':'minecraft:model','model':'neonward:item/sentinel_shield_blocking'},'on_false':{'type':'minecraft:model','model':'neonward:item/sentinel_shield'}}}
for path,data in [('models/item/sentinel_shield.json',model),('models/item/sentinel_shield_blocking.json',blocking),('items/sentinel_shield.json',item)]:
    (ROOT/path).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
assert len(parts)<80
for part in parts:
    assert all(-16<=a<b<=32 for a,b in zip(part['from'],part['to']))
print(f'SHIELD_MODEL_PASS: {len(parts)} cuboids, front armor / rear grip / separate blocking pose')
