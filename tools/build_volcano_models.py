"""Original articulated low-poly meshes matching the approved numbered volcanic concept.
Uses the existing deterministic mesh writer, never regenerates Frost assets.
"""
from pathlib import Path
import json, random
from PIL import Image
import build_frost_bosses as geom
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'resources/assets/neonward';OUT=ROOT/'design/volcano/model-review';OUT.mkdir(parents=True,exist_ok=True)
COLORS=[(43,39,39),(150,132,112),(74,68,67),(172,91,36),(19,17,24),(134,32,19),(255,159,33),(226,194,69)]
NAMES=['熔角の大甲虫','灼尾のサソリ','火鱗のサラマンダー','灰牙の狼','黒曜の大猪','煙殻の宿蟹','黒翼のコウモリ','硫黄の大蛇','熔角の山羊','背山の大亀','熔鉄の槍騎士','噴煙の魔人','歩く溶鉱炉','黒曜の大蜘蛛','鍛冶のケンタウロス','鎖鐘の守護者','灰燼の司祭','鋸輪の処刑人','牛頭の門番','四腕の神殿守護者','灰翼の竜','白金の不死鳥','溶岩の投石巨人','浮遊する火核碑','地潜りの大百足','六翼の火蛾','三頭の熔岩猟犬','火山の皇帝','角冠の古竜','火山の主・カルデラ']
def main():
 from volcano_shapes import build
 im=Image.new('RGBA',(1024,256));rng=random.Random(23030)
 for y in range(256):
  for x in range(1024):
   k=x//128;noise=rng.randrange(-10,11);c=COLORS[k];im.putpixel((x,y),tuple(max(0,min(255,v+noise)) for v in c)+(255,))
 im.save(A/'textures/entity/volcano_materials.png');glow=Image.new('RGBA',im.size);glow.paste(im.crop((768,0,896,256)),(768,0));glow.save(A/'textures/entity/volcano_materials_glow.png')
 geom.COLORS=COLORS;geom.OUT=OUT;geom.GLASS=-1;geom.ENGLISH=[f'VOLCANIC BOSS {i}' for i in range(1,31)]
 catalog=[];reviews=[]
 for n in range(1,31):
  m=build(n);points=[p for f,_ in m.faces for p in f];lo=[min(p[i] for p in points) for i in range(3)];hi=[max(p[i] for p in points) for i in range(3)]
  for part in m.parts:part.pop('_key',None)
  (A/f'bosses/volcano_{n:02}.json').write_text(json.dumps({'parts':m.parts},separators=(',',':')),encoding='utf-8')
  catalog.append(dict(floor=n,name=NAMES[n-1],bounds=[lo,hi],parts=len(m.parts),quads=len(m.faces)));reviews.append(geom.preview(m))
 for start in range(0,30,10):
  sheet=Image.new('RGB',(2700,1320),(236,233,225))
  for j,img in enumerate(reviews[start:start+10]):sheet.paste(img,((j%5)*540,(j//5)*660))
  sheet.save(OUT/f'sheet-{start//10+1}.png')
 (A/'bosses/volcano_catalog.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2),encoding='utf-8')
 print('Volcano: 30 articulated meshes + atlases + 3 model review sheets')
if __name__=='__main__':main()
