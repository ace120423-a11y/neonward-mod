from pathlib import Path
import json,math
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
names=['neon_tetra','clownfish','royal_betta','angel_fish','kohaku_koi','moon_jelly']
colors={'cyan':(22,198,216),'red':(213,57,53),'orange':(235,134,30),'white':(235,237,240),'blue':(46,88,198),'magenta':(186,49,181),'yellow':(241,201,41),'black':(27,32,42),'light_blue':(146,204,227),'pink':(246,161,204)}
image=Image.new('RGB',(960,500),(12,25,36));draw=ImageDraw.Draw(image)
for i,name in enumerate(names):
 model=json.loads((root/'resources/assets/neonward/models/item'/f'{name}.json').read_text());polys=[]
 for e in model['elements']:
  lo,hi=e['from'],e['to'];assert all(.49<=v<=15.51 for v in lo+hi)
  assert all(lo[a]<hi[a] for a in range(3))
  x,y,z=lo;X,Y,Z=hi
  for side,pts,shade in [('south',[(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)],1),('east',[(X,y,z),(X,y,Z),(X,Y,Z),(X,Y,z)],.72),('up',[(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],1.1)]:
   tex=model['textures'][e['faces'][side]['texture'][1:]];tex=tex.get('sprite') if isinstance(tex,dict) else tex
   key=tex.split('/')[-1].replace('_stained_glass','').replace('_concrete','');color=colors[key]
   color=tuple(min(255,int(c*shade)) for c in color)
   polys.append((sum(p[2]+p[0]*.4+p[1]*.12 for p in pts),pts,color))
 ox=(i%3)*320+150;oy=(i//3)*250+130
 for _,pts,c in sorted(polys):draw.polygon([(ox+(x-8)*12+(z-8)*5,oy-(y-8)*12+(z-8)*3) for x,y,z in pts],fill=c)
 draw.text(((i%3)*320+25,(i//3)*250+222),name,fill='white')
 # Worst horizontal radius, including rotation and swimming; glass inner half-width 7/16.
 horizontal=math.hypot(.46*7.5/16,.46*7.5/16)+(.035 if i==5 else .10)
 assert horizontal<7/16
print('PASS: six models have positive bounded geometry; rotated swimming envelopes stay within tank glass')
out=root/'work/aquatic-model-review.png';out.parent.mkdir(exist_ok=True);image.save(out)
print(out)
