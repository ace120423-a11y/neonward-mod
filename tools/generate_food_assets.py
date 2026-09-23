"""Native voxel food models and a hand-patterned material atlas. No jar/world access.

Run to regenerate only the eighteen StreetMeals assets and build/food-check previews.
No external images or vanilla model inheritance; all meshes use ordinary JSON elements.
"""
from pathlib import Path
import json, math, random
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'resources/assets/neonward'
IDS=['skewer','ramen','curry','tea','gyoza','motsu_stew','grilled_fish','seasonal_plate','oden','zero_beer','coffee','toast','beer','sake','shochu','highball','umeshu','cocktail']
LABELS=['Charcoal skewers','Yokocho ramen','Spice curry','Blend tea','Crispy gyoza','Motsu stew','Grilled fish set','Seasonal small plate','Street oden','Alcohol-free beer','Dark roast coffee','Cafe toast','Neon draft beer','Moonlight junmai sake','Shochu on the rocks','Amber highball','Green plum umeshu','Neon sunset cocktail']
# Each 16px tile has its own culinary surface pattern, not a flat solid swatch.
COLORS={
 'lacquer':(50,36,35),'red':(155,48,40),'cream':(232,222,194),'blue':(44,85,114),
 'bamboo':(186,136,69),'meat':(162,85,46),'char':(72,42,26),'pepper':(73,128,47),
 'rice':(246,239,216),'curry':(153,88,29),'carrot':(228,114,32),'potato':(231,187,99),
 'broth':(172,123,59),'noodle':(244,213,136),'pork':(190,119,104),'egg':(255,247,218),
 'yolk':(247,171,30),'nori':(37,61,37),'scallion':(107,171,68),'dumpling':(234,211,163),
 'stew':(132,76,44),'motsu':(210,171,134),'fish':(131,145,140),'fishgold':(184,130,69),
 'lemon':(250,217,76),'radish':(244,232,212),'pink':(214,104,111),'tea':(103,121,55),
 'amber':(213,146,31),'glass':(186,219,224),'coffee':(69,39,27),'crust':(173,102,47),
}

def atlas():
    im=Image.new('RGBA',(128,64));draw=ImageDraw.Draw(im)
    for index,(name,color) in enumerate(COLORS.items()):
        x0=(index%8)*16;y0=(index//8)*16;rng=random.Random(1500+index)
        for y in range(16):
            for x in range(16):
                delta=rng.randint(-9,9)
                if name in ('rice','egg','radish','noodle'):delta=rng.randint(-5,5)
                if name in ('bamboo','lacquer','crust') and y%4==0:delta-=15
                if name in ('fish','fishgold') and (x+y//3*2)%5==0:delta+=30
                if name=='meat' and (x+2*y)%11<2:delta-=35
                if name=='dumpling' and x%4==0:delta-=22
                if name in ('blue','red','cream') and y in (1,14):delta+=24
                im.putpixel((x0+x,y0+y),tuple(max(0,min(255,c+delta)) for c in color)+(255,))
        if name=='rice':
            for _ in range(12):
                x=x0+rng.randrange(2,14);y=y0+rng.randrange(2,14);draw.line((x,y,x+1,y),fill=(255,251,235))
    path=ASSETS/'textures/item/foods/materials.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
    return im

class Dish:
    def __init__(self):self.elements=[];self.materials=[]
    def box(self,material,x,y,z,w,h,d):
        index=list(COLORS).index(material);u=(index%8)*2;v=(index//8)*4
        faces={side:dict(texture='#food',uv=[u+.125,v+.25,u+1.875,v+3.75]) for side in ('north','south','east','west','up','down')}
        self.elements.append({'from':[round(x,3),round(y,3),round(z,3)],'to':[round(x+w,3),round(y+h,3),round(z+d,3)],'faces':faces})
        self.materials.append(material)
    def tray(self,color='lacquer'):
        self.box(color,1,1,2,14,.65,12)
        for x in (1,14.4):self.box(color,x,1.65,2,.6,.6,12)
        for z in (2,13.4):self.box(color,1.6,1.65,z,12.8,.6,.6)
    def plate(self,color='cream'):
        self.box(color,3,1,3,10,.5,10);self.box(color,2,1.5,4,12,.55,8);self.box(color,4,1.5,2,8,.55,12)
        for x in (2,13.4):self.box(color,x,2,4,.6,.4,8)
        for z in (2,13.4):self.box(color,4,2,z,8,.4,.6)
    def bowl(self,color='red',liquid='broth'):
        self.box(color,5,1,5,6,1,6)
        self.box(color,4,2,4,8,1,8)
        self.box(color,3,3,5,10,1.4,6);self.box(color,5,3,3,6,1.4,2);self.box(color,5,3,11,6,1.4,2)
        self.box(liquid,4,4.4,5,8,.25,6)
        for z in (4,11):self.box(liquid,5,4.4,z,6,.25,1)
        # Notched octagonal profile distinguishes bowls from rectangular lacquer trays.
        for x in (3,12.3):self.box(color,x,4.1,5,.7,1.2,6)
        for z in (3,12.3):self.box(color,5,4.1,z,6,1.2,.7)
        for x in (3.6,10.7):
            for z in (3.6,10.7):self.box(color,x,3,z,1.7,2.3,1.7)
        for x in (3.1,12.8):self.box('cream',x,4.45,6,.12,.2,4)
    def mug(self,color,liquid,glass=False):
        self.box(color,4,1,4,7,.8,7)
        self.box(liquid,4.6,1.8,4.6,5.8,6.7,5.8)
        for x in (4,10.5):self.box(color,x,1.8,4,.5,7.2,7)
        for z in (4,10.5):self.box(color,4.5,1.8,z,6,.5 if glass else 7.2,.5)
        if glass:
            for z in (4,10.5):
                for x in (4.5,10):self.box(color,x,2.3,z,.5,6.7,.5)
        for x,y,z,w,h,d in [(11,7.2,6,2.7,.8,3),(13,3.2,6,.7,4,3),(11,2.5,6,2.7,.7,3)]:self.box(color,x,y,z,w,h,d)
        for x in (4,10.5):self.box(color,x,9,4,.5,.45,7)
        for z in (4,10.5):self.box(color,4.5,9,z,6,.45,.5)
    def garnish(self,y=5):
        for i in range(5):self.box('scallion',5.2+i*.9,y,8.7+(i%2)*.5,.5,.25,.7)
    def json(self):
        return dict(ambientocclusion=False,gui_light='front',textures={'food':'neonward:item/foods/materials','particle':'neonward:item/foods/materials'},elements=self.elements,display={
          'gui':{'rotation':[30,225,0],'translation':[0,1,0],'scale':[.85,.85,.85]},
          'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.5,.5,.5]},
          'fixed':{'rotation':[90,0,0],'translation':[0,0,0],'scale':[.75,.75,.75]},
          'firstperson_righthand':{'rotation':[10,-20,0],'translation':[0,1,-1],'scale':[.6,.6,.6]},
          'firstperson_lefthand':{'rotation':[10,20,0],'translation':[0,1,-1],'scale':[.6,.6,.6]},
          'thirdperson_righthand':{'rotation':[65,0,0],'translation':[0,2,1],'scale':[.45,.45,.45]},
          'thirdperson_lefthand':{'rotation':[65,0,0],'translation':[0,2,1],'scale':[.45,.45,.45]}})

def dish(kind):
    d=Dish();b=d.box
    if kind=='skewer':
        d.tray();
        for x in (4,8,12):
            b('bamboo',x-.18,2.3,1,.36,.36,13)
            for i,z in enumerate((4,7,10)):
                b('meat',x-1.15,2.7,z,2.3,1.6,2.2);b('char',x-1.1,4.32,z+.4,2.2,.09,.3)
                if i<2:b('pepper',x-.8,2.7,z+2.35,1.6,1.3,.55)
    elif kind=='ramen':
        d.bowl()
        for i in range(6):b('noodle',4.6,4.7,4.6+i*1.05,6.6,.3,.35)
        for i in range(3):b('noodle',5.2+i*1.5,4.95,5.2,.32,.2,4.5)
        b('pork',4.4,5.2,5,3.1,.55,2.2);b('cream',4.8,5.77,5.5,2.2,.08,.4)
        b('egg',8.2,5.2,7.3,2.5,.55,2.8);b('yolk',8.8,5.77,8,1.25,.1,1.35)
        b('nori',9.8,4.8,4.2,1.5,2.4,.3);d.garnish(5.7)
        for x in (3,3.65):b('bamboo',x,5.6,1.2,.28,.3,12.6)
    elif kind=='curry':
        d.plate();b('rice',3.2,2.1,4,4.7,1.2,7.8);b('rice',4,3.3,4.6,3.3,.5,6.5)
        b('curry',8,2.1,4,4.7,.65,7.8)
        for x,z in ((9,5),(10.6,8),(9,10)):b('potato',x,2.8,z,1.25,.65,1.15)
        for x,z in ((10.7,5.9),(8.4,8.3)):b('carrot',x,2.8,z,1,.6,1)
        b('pink',6,2.2,11.5,1.5,.55,1.2)
    elif kind in ('tea','coffee','zero_beer'):
        if kind=='tea':
            d.mug('blue','tea');b('cream',4.8,3,3.85,3.9,2.2,.15);b('scallion',6.2,3.5,3.65,1.1,1.2,.2)
        elif kind=='coffee':
            d.plate('blue');d.mug('cream','coffee')
            b('crust',5.7,8.51,6.1,3.4,.06,.35);b('crust',8.7,8.51,6.1,.4,.06,2.1)
        else:
            d.mug('glass','amber',True);b('egg',4.5,8.2,4.5,6,1,6)
            for x,z in ((5,5),(8.5,5.5),(6.8,8.4)):b('egg',x,9.2,z,1.5,.5,1.2)
            b('cream',5.7,3.1,3.85,3.6,2.4,.16);b('blue',6.7,3.5,3.65,1.4,1.6,.15)
    elif kind=='gyoza':
        d.tray('blue')
        for x,z in ((3,4),(7,4),(11,4),(5,8.8),(9,8.8)):
            b('char',x-.2,2.1,z,2.8,.25,1.9);b('dumpling',x,2.35,z,2.4,1.25,1.9);b('dumpling',x+.35,3.6,z+.35,1.7,.5,1.2)
            for xx in (x+.4,x+1,x+1.6):b('cream',xx,4.1,z+.45,.18,.25,1)
        b('red',2,2.2,11,2.5,.65,1.6);b('coffee',2.25,2.86,11.25,2,.08,1.1)
    elif kind=='motsu_stew':
        d.bowl('lacquer','stew')
        for x in (1,13):b('lacquer',x,3.4,6,2,1,4)
        for x,z in ((4.8,5),(8.2,5.2),(6.2,8),(9.4,9)):
            b('motsu',x,4.7,z,1.7,.7,1.5);b('broth',x+.5,5.42,z+.4,.6,.08,.7)
        b('radish',4.6,4.8,9.8,1.5,.8,1.4);d.garnish(5.7)
    elif kind=='grilled_fish':
        d.tray();b('cream',2,2,4.5,9,.5,6.7)
        b('fishgold',3,2.5,6,6.5,1,2.4);b('fish',4,3.5,6.5,4.7,.35,1.2)
        b('fish',2,2.6,6.2,1.1,.75,2);b('char',2.3,3.36,6.5,.3,.08,.3)
        b('fish',9.5,2.6,5.5,1,.7,3.4)
        for x in (4.2,5.8,7.4):b('char',x,3.88,6.35,.28,.06,1.6)
        b('lemon',3.8,2.55,9.1,2.5,.4,1);b('egg',4.2,2.96,9.35,1.7,.06,.35)
        b('blue',11.6,2.1,4.5,2.4,.9,3);b('rice',11.8,3,4.7,2,1,2.6)
        b('radish',11.7,2.2,10,1.8,1.1,1.8);b('scallion',11.8,3.4,10.5,1.5,.15,.6)
    elif kind=='seasonal_plate':
        d.plate('blue')
        for x,z in ((4.3,4.6),(6,4.4),(5,6)):b('pepper',x,2.1,z,1.3,1.1,1.5)
        for x,z in ((8,4),(10,5.2),(9,6.7)):b('carrot',x,2.1,z,1.2,.9,1.3)
        b('radish',4.2,2.1,9,3,1.2,2.5);b('pink',8.6,2.1,9,3,1,2)
        d.garnish(3.5)
    elif kind=='oden':
        d.bowl('cream','broth');b('radish',4.7,4.7,4.7,2.9,.95,2.9)
        b('egg',8.8,4.7,4.8,2.1,1.15,2.6);b('egg',9.1,5.85,5.1,1.5,.3,2)
        b('dumpling',4.8,4.7,8.7,2.5,.95,2);b('broth',5.5,5.67,9.2,1.1,.07,1)
        b('fishgold',8.4,4.8,8.5,2.7,.8,2.4);b('bamboo',9.55,5.7,7.5,.25,.25,6)
        b('lemon',11.4,5,10.6,.65,.4,.65)
    elif kind=='beer':
        d.mug('glass','amber',True);b('egg',4.5,8.3,4.5,6,1.2,6)
        for x,z in ((5,5),(8,7),(6,9)):b('egg',x,9.5,z,1.5,.45,1)
        b('egg',4.4,7,3.82,1,.95,.18)
        b('red',5.5,3,3.83,4,2.7,.18);b('cream',6.3,3.65,3.6,2.4,1.25,.2)
    elif kind=='sake':
        d.tray('lacquer')
        b('cream',3.5,2,5,4.5,1,5);b('cream',3,3,4.5,5.5,4,6)
        b('cream',3.7,7,5.2,4.1,1.2,4.6);b('cream',4.8,8.2,6.3,1.9,3,2.4)
        b('blue',4.55,11.2,6.05,2.4,.55,2.9);b('tea',5,11.76,6.5,1.5,.08,2)
        b('blue',3.85,4,4.28,3.8,1.1,.2);b('blue',4.9,5.2,4.28,1.4,1,.2)
        b('blue',10.1,2,6,3.5,.6,3.5);b('cream',9.8,2.6,5.7,4.1,1.9,4.1)
        b('blue',10.15,4.51,6.05,3.4,.1,3.4);b('tea',10.65,4.62,6.55,2.4,.06,2.4)
    elif kind in ('shochu','highball','umeshu'):
        tall=kind=='highball';h=9 if tall else 5;liquid='amber' if kind!='shochu' else 'cream'
        b('glass',4,1,4,8,.8,8);b(liquid,4.6,1.8,4.6,6.8,h-.5,6.8)
        for x in (4,11.5):
            for z in (4,11.5):b('glass',x,1.8,z,.5,h,.5)
        for z in (4,11.5):b('glass',4.5,h+1.8,z,7,.45,.5)
        for x in (4,11.5):b('glass',x,h+1.8,4,.5,.45,8)
        for x,z in ((5.2,5.5),(8.3,8.2)):
            b('glass',x,h+.7,z,2,1.15,2);b('egg',x+.2,h+1.86,z+.2,1.6,.06,.22)
        if tall:
            b('lemon',10.9,9.6,6,1,3.6,3.6);b('egg',11.91,10.1,6.5,.07,2.6,2.6)
            for y in (3,5,7):b('egg',6.2,y,4.4,.3,.35,.15)
        elif kind=='umeshu':
            b('pepper',5.5,5.3,8.5,2.3,1.8,2.3);b('scallion',5.9,7.1,8.9,1.5,.5,1.5)
            b('bamboo',6.5,7.6,9.5,.25,.6,.25)
    elif kind=='cocktail':
        b('glass',4,1,4,8,.55,8);b('glass',7.45,1.55,7.45,1.1,4.7,1.1)
        b('glass',6.1,6.2,6.1,3.8,.6,3.8);b('pink',6.4,6.8,6.4,3.2,1.2,3.2)
        b('glass',4.9,8,4.9,6.2,.4,6.2);b('pink',5.2,8.4,5.2,5.6,1.2,5.6)
        b('glass',3.7,9.6,3.7,8.6,.4,8.6);b('carrot',4,10,4,8,.65,8)
        for z in (3.7,11.8):b('glass',3.7,10.65,z,8.6,.4,.5)
        for x in (3.7,11.8):b('glass',x,10.65,4.2,.5,.4,7.6)
        b('pink',5,10.7,5,1.3,1.1,1.3);b('bamboo',5.6,11.8,5.6,.18,.8,.18)
        b('lemon',10.9,10.3,7,.8,3.4,3.2);b('egg',11.71,10.8,7.5,.07,2.4,2.2)
    elif kind=='toast':
        d.plate();b('crust',4,2.1,3.5,8,1.5,9);b('rice',4.5,3.61,4,7,.15,8)
        for x in (6,8,10):b('crust',x,3.78,4.4,.2,.06,7.2)
        for z in (6,8,10):b('crust',4.9,3.78,z,6.2,.06,.2)
        b('potato',7,3.86,7,2,.5,2);b('cream',7.2,4.38,7.2,1.6,.07,1.6)
    return d

def preview(dishes):
    # Software contact sheet from the actual element bounds; in-game lighting can differ.
    out=ROOT/'build/food-check';out.mkdir(parents=True,exist_ok=True)
    height=math.ceil(len(dishes)/4)*325+35
    image=Image.new('RGB',(1280,height),(20,25,32));draw=ImageDraw.Draw(image)
    font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',19);small=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',14)
    for index,(name,d) in enumerate(dishes.items()):
        col=index%4;row=index//4;cx=col*320+160;cy=row*325+173
        draw.rounded_rectangle((col*320+10,row*325+12,col*320+310,row*325+310),radius=14,fill=(30,37,46))
        faces=[]
        def project(v):x,y,z=v;return (cx+(x-z)*10,cy+(x+z-16)*5-y*10)
        for e,material in zip(d.elements,d.materials):
            x,y,z=e['from'];X,Y,Z=e['to'];color=COLORS[material]
            for points,shade in [([(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],1.08),([(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)],.8),([(X,y,z),(X,y,Z),(X,Y,Z),(X,Y,z)],.63)]:
                faces.append((points,shade,material))
        # Per-pixel depth is necessary: painter sorting hides toppings behind a large tray.
        canvas=np.array(image);depth=np.full((height,1280),-np.inf)
        texture=np.array(Image.open(ASSETS/'textures/item/foods/materials.png').convert('RGB'))
        for points,shade,material in faces:
            xy=np.array([project(v) for v in points]);zz=np.array([sum(v) for v in points])
            tile=list(COLORS).index(material);uv=np.array([[0,0],[1,0],[1,1],[0,1]])
            for indices in ((0,1,2),(0,2,3)):
                verts=xy[list(indices)];zs=zz[list(indices)];tex=uv[list(indices)]
                left=max(0,int(verts[:,0].min()));right=min(1279,int(verts[:,0].max())+1)
                top=max(0,int(verts[:,1].min()));bottom=min(height-1,int(verts[:,1].max())+1)
                xx,yy=np.meshgrid(np.arange(left,right+1)+.5,np.arange(top,bottom+1)+.5)
                (ax,ay),(bx,by),(cx1,cy1)=verts
                den=(by-cy1)*(ax-cx1)+(cx1-bx)*(ay-cy1)
                if abs(den)<1e-9:continue
                a=((by-cy1)*(xx-cx1)+(cx1-bx)*(yy-cy1))/den
                b=((cy1-ay)*(xx-cx1)+(ax-cx1)*(yy-cy1))/den;c=1-a-b
                z=a*zs[0]+b*zs[1]+c*zs[2];view=depth[top:bottom+1,left:right+1]
                mask=(a>=0)&(b>=0)&(c>=0)&(z>view)
                u=a*tex[0,0]+b*tex[1,0]+c*tex[2,0];v=a*tex[0,1]+b*tex[1,1]+c*tex[2,1]
                tx=np.clip((u*13+1).astype(int),0,15)+tile%8*16;ty=np.clip((v*13+1).astype(int),0,15)+tile//8*16
                colors=np.clip(texture[ty,tx]*shade,0,255).astype(np.uint8)
                canvas[top:bottom+1,left:right+1][mask]=colors[mask];view[mask]=z[mask]
        image=Image.fromarray(canvas);draw=ImageDraw.Draw(image)
        draw.text((col*320+24,row*325+246),f'{index:02}  {LABELS[index]}',font=font,fill=(240,233,213))
        draw.text((col*320+24,row*325+277),f'{len(d.elements)} elements | {name}',font=small,fill=(142,167,179))
    draw.text((24,height-22),'Native JSON geometry preview | NeonWard food catalog | lighting simplified',font=small,fill=(150,166,174))
    image.save(out/'food-catalog.png')

def main():
    atlas();dishes={name:dish(name) for name in IDS}
    for name,d in dishes.items():
        data=d.json()
        for element in data['elements']:
            assert all(0<=a<b<=16 for a,b in zip(element['from'],element['to'])),name
            assert len(element['faces'])==6
        path=ASSETS/f'models/item/foods/street_{name}.json';path.parent.mkdir(parents=True,exist_ok=True)
        path.write_text(json.dumps(data,separators=(',',':'))+'\n',encoding='utf-8')
        item=ASSETS/f'items/street_{name}.json';item.write_text(json.dumps({'model':{'type':'minecraft:model','model':f'neonward:item/foods/street_{name}'}},separators=(',',':'))+'\n',encoding='utf-8')
        print(f'PASS street_{name}: {len(d.elements)} bounded native elements')
    preview(dishes)
    print('Preview:',ROOT/'build/food-check/food-catalog.png')

if __name__=='__main__':main()
