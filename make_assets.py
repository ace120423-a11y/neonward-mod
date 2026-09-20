from pathlib import Path
import json
from PIL import Image,ImageDraw
r=Path(__file__).resolve().parent/'resources'
def js(p,d):
 p=r/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2),encoding='utf-8')
for name in ['phone','bike_key','car_key']:
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 if name=='phone':
  d.rounded_rectangle((8,1,24,30),3,fill='#111b28',outline='#69ebe3',width=1);d.rectangle((10,5,22,24),fill='#163f52')
  d.line((11,21,15,13,20,17,22,8),fill='#ff5daf',width=2);d.rectangle((14,3,18,3),fill='#c4faf5');d.ellipse((15,26,17,28),fill='#5affef')
 else:
  color='#fa615b' if name=='bike_key' else '#64dee8'
  d.rounded_rectangle((6,3,24,17),3,fill='#1c252c',outline=color,width=2);d.rectangle((13,16,17,29),fill='#a9b5ba');d.rectangle((17,23,21,26),fill='#a9b5ba');d.rectangle((17,28,20,30),fill='#a9b5ba');d.line((10,9,20,9),fill=color,width=2)
 p=r/f'assets/neonward/textures/item/{name}.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
 js(Path(f'assets/neonward/models/item/{name}.json'),{'parent':'minecraft:item/generated','textures':{'layer0':f'neonward:item/{name}'}})
 js(Path(f'assets/neonward/items/{name}.json'),{'model':{'type':'minecraft:model','model':f'neonward:item/{name}'}})
js(Path('assets/neonward/lang/ja_jp.json'),{'item.neonward.phone':'NEON LINK スマホ','item.neonward.bike_key':'ナイトランナーのキー','item.neonward.car_key':'グリッドクーペのキー','entity.neonward.bike':'ナイトランナー','entity.neonward.car':'グリッドクーペ'})
js(Path('assets/neonward/lang/en_us.json'),{'item.neonward.phone':'NEON LINK Phone','item.neonward.bike_key':'Night Runner Key','item.neonward.car_key':'Grid Coupe Key','entity.neonward.bike':'Night Runner','entity.neonward.car':'Grid Coupe'})
for name,center in [('phone','minecraft:amethyst_shard'),('bike_key','minecraft:copper_ingot'),('car_key','minecraft:diamond')]:
 js(Path(f'data/neonward/recipe/{name}.json'),{'type':'minecraft:crafting_shaped','pattern':['IRI','ICI','III'],'key':{'I':'minecraft:iron_ingot','R':'minecraft:redstone','C':center},'result':{'id':f'neonward:{name}','count':1}})
