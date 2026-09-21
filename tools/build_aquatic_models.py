"""Original code-native cuboid models, normalized for bounded aquarium display."""
from pathlib import Path
import json, math
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'resources/assets/neonward'
species=[('neon_tetra','ネオンテトラ','cyan_concrete','red_concrete'),('clownfish','カクレクマノミ','orange_concrete','white_concrete'),('royal_betta','ロイヤルベタ','blue_concrete','magenta_concrete'),('angel_fish','エンゼルフィッシュ','yellow_concrete','black_concrete'),('kohaku_koi','紅白錦鯉','white_concrete','red_concrete'),('moon_jelly','ミズクラゲ','light_blue_stained_glass','pink_stained_glass')]
for index,(name,label,body,accent) in enumerate(species):
 elements=[]
 textures={'body':'minecraft:block/'+body,'accent':'minecraft:block/'+accent,'eye':'minecraft:block/black_concrete','shine':'minecraft:block/white_concrete','fin':'minecraft:block/'+('cyan_stained_glass' if index==0 else accent)}
 def box(x,y,z,w,h,d,tex='body'):
  elements.append({'from':[x,y,z],'to':[x+w,y+h,z+d],'faces':{side:{'texture':'#'+tex,'uv':[0,0,16,16]} for side in ['north','south','east','west','up','down']}})
 if index==5:
  # Stepped translucent bell, inner gonads, fine segmented curling tentacles.
  for y,r in [(9,5),(10,4.7),(11,4),(12,2.8),(13,1.3)]:
   for x in range(-5,5):
    for z in range(-5,5):
     if math.hypot(x+.5,z+.5)<r:box(8+x,y,8+z,1,1,1,'body')
  for x,z in [(6.7,6.7),(8.5,6.7),(6.7,8.5),(8.5,8.5)]:box(x,9, z,.8,1,.8,'accent')
  for j in range(8):
   a=j*math.tau/8
   for s in range(8):
    r=3+math.sin(s*.7+j)*.5;box(8+math.cos(a)*r,8-s*.8,8+math.sin(a)*r,.25,.9,.25,'accent' if j%2 else 'fin')
 else:
  # Tapered body slices and tiny scales; distinct fins and species markings.
  for x in range(4,13):
   f=math.sin((x-3)*math.pi/11);h=(6 if index==3 else 3.6)*f;d=(2 if index==3 else 3)*f
   box(x,8-h/2,8-d/2,1.05,h,d)
   if index==0:box(x,8.05,8-d/2-.04,1,.38,.08,'accent');box(x,8.05,8+d/2-.04,1,.38,.08,'fin')
   if index in (1,3) and x in (5,8,11):box(x,8-h/2-.04,8-d/2-.06,.65,h+.08,d+.12,'accent')
   if index==4 and x in (5,6,9,10):box(x,8+h/2-.07,8-d/2,.85,.15,d*.7,'accent')
  for z in (6.95,8.95):
   box(11.8,8.3,z,.65,.65,.20,'eye');box(11.98,8.65,z-.02,.19,.19,.24,'shine')
  box(12.75,7.6,7.65,.35,.25,.7,'accent')
  # Tail fan, dorsal and ventral rays (not just a rectangular body).
  for i in range(7):
   h=(i-3)*.62;box(1.2+i*.35,8+h,7.8,2.6-i*.25,.48,.4,'accent' if i%2==0 else 'fin')
  for i in range(6):
   h=(3.6 if index in (2,3) else 1.6)*math.sin((i+1)*math.pi/8)
   box(5+i,9.1,7.85,.65,h,.3,'fin');box(5+i,6.8-h*.55,7.85,.55,h*.65,.3,'fin')
  for side in (-1,1):
   for i in range(4):box(9-i*.4,7.3-i*.12,8+side*(1.1+i*.4),.9,.18,.45,'fin')
 # Normalize every axis around item origin; maximum 15 units (0.43125 blocks in tank).
 mins=[min(e['from'][a] for e in elements) for a in range(3)];maxs=[max(e['to'][a] for e in elements) for a in range(3)]
 scale=15/max(maxs[a]-mins[a] for a in range(3))
 for e in elements:
  for field in ('from','to'):e[field]=[round(8+(e[field][a]-(mins[a]+maxs[a])/2)*scale,4) for a in range(3)]
 textures={key:({'sprite':value,'force_translucent':True} if 'stained_glass' in value else value) for key,value in textures.items()}
 model={'textures':textures,'elements':elements,'display':{'gui':{'rotation':[15,-25,0],'scale':[.85,.85,.85]},'ground':{'scale':[.5,.5,.5]}}}
 (ASSETS/'models/item'/f'{name}.json').write_text(json.dumps(model,ensure_ascii=False),encoding='utf-8')
 (ASSETS/'items'/f'{name}.json').write_text(json.dumps({'model':{'type':'minecraft:model','model':'neonward:item/'+name}}),encoding='utf-8')
 assert all(.49<=v<=15.51 for e in elements for field in ('from','to') for v in e[field])
 print(name,len(elements),'parts; tank diameter <= 0.432 blocks')
for locale in ('ja_jp','en_us'):
 p=ASSETS/'lang'/f'{locale}.json';lang=json.loads(p.read_text(encoding='utf-8'))
 for name,label,_,_ in species:lang['item.neonward.'+name]=label if locale=='ja_jp' else name.replace('_',' ').title()
 p.write_text(json.dumps(lang,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
res=ROOT/'resources/data/neonward/residents.json';rows=json.loads(res.read_text(encoding='utf-8'))
for key,x,role,title in [('fishing_buyer',323,0,'釣果買取・ナギ'),('fishing_rods',331,2,'釣り竿販売・ハル')]:
 rows=[r for r in rows if r['key']!=key]
 rows.append(dict(key=key,x=x,y=65,z=637,role=role,name=title,shopStaff=True,yaw=0,job=key))
res.write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
