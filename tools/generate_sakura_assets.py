"""Original Edo materials: procedural pixel textures, native block meshes, contact sheet.
Writes only sakura_* resources and build/sakura-check; never builds/opens a game world.
"""
from pathlib import Path
import json, math, random
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1];ASSET=ROOT/'resources/assets/neonward'
IDS=['plaster','dark_timber','board','stone_paving','kawara_tile','kawara_slope','kawara_ridge','lattice','shoji','paper_lantern','stone_lantern','saisen_box','chozu_basin','vermilion_timber','omamori_counter']
JA=['桜町の白漆喰','桜町の黒染め柱','桜町の杉板','桜町の石畳','桜町の丸瓦','桜町の勾配瓦','桜町の棟瓦','桜町の暖簾付き組子格子','桜町の障子','桜町の提灯','桜町の石灯籠','桜町の賽銭箱','桜町の手水鉢','桜町の朱塗り柱','桜町のお守り授与台']
EN=['Sakura White Plaster','Sakura Charred Timber','Sakura Cedar Boards','Sakura Stone Paving','Sakura Curved Kawara Tiles','Sakura Kawara Roof Slope','Sakura Kawara Ridge','Sakura Lattice and Noren','Sakura Shoji','Sakura Paper Lantern','Sakura Stone Lantern','Sakura Offering Box','Sakura Chozu Basin','Sakura Vermilion Timber','Sakura Omamori Counter']
COLORS={'plaster':(228,219,196),'dark_timber':(59,43,37),'board':(153,108,69),'stone_paving':(133,140,135),'kawara':(58,71,78),'kawara_edge':(92,105,109),'paper':(239,224,184),'lantern_paper':(246,194,108),'stone':(133,144,133),'moss':(89,114,65),'gold':(200,156,65),'bamboo':(152,164,75),'water':(59,129,147),'vermilion':(173,55,37),'indigo':(43,72,103),'rose':(187,92,113),'jade':(73,130,111),'ricecord':(210,183,124)}

IDS+=['shrine_roof_tile','shrine_roof_slope','shrine_roof_ridge','shrine_bracket','shrine_rope']
JA+=['桜宮の緑青銅瓦','桜宮の緑青屋根勾配','桜宮の緑青棟瓦','桜宮の金飾り組物','桜宮の注連縄と紙垂']
EN+=['Shrine Verdigris Tiles','Shrine Verdigris Slope','Shrine Verdigris Ridge','Shrine Gilded Bracket','Shrine Shimenawa and Shide']
COLORS.update({'verdigris':(73,119,100),'verdigris_edge':(96,139,117),'shrine_rope':(179,152,93),'shide':(246,239,214)})
IDS+=['saisen_left','saisen_right','chozu_front_left','chozu_front_right','chozu_back_left','chozu_back_center','chozu_back_right']
JA+=['桜宮の賽銭箱・左端','桜宮の賽銭箱・右端','桜宮の手水鉢・前左','桜宮の手水鉢・前右','桜宮の手水鉢・後左','桜宮の手水鉢・後中央','桜宮の手水鉢・後右']
EN+=['Offering Box Left','Offering Box Right','Chozu Front Left','Chozu Front Right','Chozu Back Left','Chozu Back Center','Chozu Back Right']
COLORS.update({'iron_fitting':(39,42,40),'water_glint':(116,163,160),'basin_stone':(83,91,88),'black_twine':(40,35,27)})
FIXTURE_CELLS={'saisen_left':(-16,0),'saisen_box':(0,0),'saisen_right':(16,0),'chozu_front_left':(-16,0),'chozu_basin':(0,0),'chozu_front_right':(16,0),'chozu_back_left':(-16,16),'chozu_back_center':(0,16),'chozu_back_right':(16,16)}

def texture(name,color):
    rng=random.Random('sakura/'+name);im=Image.new('RGB',(32,32));px=im.load()
    for y in range(32):
        for x in range(32):
            delta=rng.randint(-7,7)
            if name in ('dark_timber','board','vermilion'):
                delta+=int(math.sin(x*.9+math.sin(y*.22)) * 9)
                if x%8==0:delta-=16
            elif name=='stone_paving':
                if y%16==0 or (x+(8 if y//16%2 else 0))%16==0:delta=-32
                elif y%16==1 or (x+(8 if y//16%2 else 0))%16==1:delta=14
            elif name in ('paper','lantern_paper'):
                delta=rng.randint(-4,4)
                if name=='lantern_paper' and y%4==0:delta-=24
                if name=='paper' and (x*7+y*13)%53==0:delta-=13
            elif name.startswith('kawara'):
                delta+=int(math.cos(x*math.pi/16)*8)
                if y in (0,1,29,30):delta-=18
            elif name.startswith('verdigris'):
                # Broad, quiet patina blooms with occasional exposed copper seams.
                delta=int(5*math.sin(x*.29+y*.17)+4*math.cos(y*.41))+rng.randint(-3,3)
                if y in (0,31):delta-=12
                if (x*3+y*5)%97==0:delta+=12
            elif name=='shrine_rope':
                delta+=int(15*math.sin((x+y*2)*math.pi/8))
            elif name=='shide':delta=rng.randint(-2,2)
            elif name=='bamboo':
                if y in (0,1,15,16):delta=-28
                else:delta+=int(math.sin(x*.6)*10)
            elif name=='water':delta+=int(math.sin(x*.5+y*.3)*12)
            elif name=='gold':delta+=int((31-x-y)/3)
            px[x,y]=tuple(max(0,min(255,c+delta)) for c in color)
    path=ASSET/f'textures/block/sakura_{name}.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
    return np.asarray(im)

class Model:
    def __init__(self):self.elements=[];self.materials=[]
    def box(self,mat,x,y,z,X,Y,Z):
        faces={face:{'texture':'#'+mat,'uv':[0,0,16,16]} for face in ('up','down','north','south','east','west')}
        for face,bound,edge in [('west',x,0),('east',X,16),('down',y,0),('up',Y,16),('north',z,0),('south',Z,16)]:
            if bound==edge:faces[face]['cullface']=face
        self.elements.append({'from':[x,y,z],'to':[X,Y,Z],'faces':faces});self.materials.append(mat)
    def data(self):return {'ambientocclusion':True,'textures':{**{m:'neonward:block/sakura_'+m for m in set(self.materials)},'particle':'neonward:block/sakura_'+self.materials[0]},'elements':self.elements,'display':{'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[.62,.62,.62]},'ground':{'translation':[0,3,0],'scale':[.4,.4,.4]},'firstperson_righthand':{'rotation':[0,45,0],'translation':[0,0,0],'scale':[.5,.5,.5]},'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375,.375,.375]},'fixed':{'scale':[.55,.55,.55]}}}

def fixture(basin):
    """Authored continuous furniture; split into bounded native cells only at export."""
    m=Model();b=m.box
    if not basin:
        # Low continuous 3m chest, black forged corner straps and open coin grate.
        b('dark_timber',-16,0,1,32,2,15)
        b('board',-16,2,2,32,12,14)
        for y in (3,7,11):b('dark_timber',-16,y,1.85,32,y+.18,2)
        for z in (1,14):b('dark_timber',-16,12,z,32,16,z+1)
        for x in (-16,30.5):b('dark_timber',x,12,2,x+1.5,16,14)
        # Deep dark grate bed beneath separated lengthwise slats.
        b('iron_fitting',-14.5,12,2,30.5,12.3,14)
        for x in range(-14,30,2):b('board',x,12.3,2,x+.8,15.8,14)
        for x in (-15.9,29.7):
            for z in (1.65,14):b('iron_fitting',x,2,z,x+2.2,12,z+.35)
            for y in (3,10):
                for xx in (x+.4,x+1.5):b('gold',xx,y,1.5,xx+.22,y+.22,1.65)
        b('iron_fitting',3.5,4.5,1.5,12.5,10.5,1.85)
        b('gold',4,5,1.3,12,10,1.5)
        b('dark_timber',4.4,5.4,1.15,11.6,9.6,1.3)
        for x,y in ((7.2,5.8),(7.2,8),(5.8,6.9),(8.6,6.9)):b('gold',x,y,1,x+1.6,y+1.6,1.15)
    else:
        # Carved stone trough: no internal walls at tile boundaries, inset water.
        b('basin_stone',-16,0,0,32,3,32)
        for x in (-16,29):b('basin_stone',x,3,0,x+3,14,32)
        for z in (0,29):b('basin_stone',-13,3,z,29,14,z+3)
        b('water',-13,11.3,3,29,11.5,29)
        for x,z in ((-10,9),(-2,22),(7,6),(18,23),(25,10)):
            b('water_glint',x,11.51,z,x+2.5,11.53,z+.12)
        # Paired bamboo rails and repeated raised nodes; dark lashings at stone ends.
        for z in (8,14):
            b('bamboo',-16,14,z,32,14.7,z+.9)
            for x in range(-14,32,5):b('bamboo',x,13.95,z-.05,x+.18,14.8,z+.95)
            for x in (-15,30):
                for dx in (0,.3,.6):b('black_twine',x+dx,13.9,z-.12,x+dx+.15,14.9,z+1.02)
        for x in (-9,4,17):
            b('bamboo',x,14.9,3,x+.45,15.2,17.3)
            # Stepped octagonal hollow wooden bowl, not a filled square cup.
            cx=x+.225;cz=19
            b('board',cx-1.2,14.8,cz-1.7,cx+1.2,15,cz+1.7)
            b('board',cx-1.7,14.8,cz-1.2,cx-1.2,15,cz+1.2)
            b('board',cx+1.2,14.8,cz-1.2,cx+1.7,15,cz+1.2)
            for dz in (-1.7,1.4):b('board',cx-1.2,15,cz+dz,cx+1.2,16,cz+dz+.3)
            for dx in (-1.7,1.4):b('board',cx+dx,15,cz-1.2,cx+dx+.3,16,cz+1.2)
            for dx in (-1.4,1.1):
                for dz in (-1.4,1.1):b('board',cx+dx,15,cz+dz,cx+dx+.3,16,cz+dz+.3)
        # Existing spout motif, now on rear center, pouring into open water.
        b('bamboo',7,3,27,8,16,28)
        b('bamboo',7,15,21,8,16,27)
        b('water',7.3,11.5,21.2,7.55,15,21.45)
        for y in (5,10,14):b('black_twine',6.9,y,26.9,8.1,y+.25,28.1)
    return m

def fixture_cell(name):
    ox,oz=FIXTURE_CELLS[name];source=fixture(name.startswith('chozu'));m=Model()
    for e,mat in zip(source.elements,source.materials):
        a=[max(e['from'][0],ox)-ox,e['from'][1],max(e['from'][2],oz)-oz]
        b=[min(e['to'][0],ox+16)-ox,e['to'][1],min(e['to'][2],oz+16)-oz]
        if all(lo<hi for lo,hi in zip(a,b)):m.box(mat,*a,*b)
    return m

def make(name):
    if name in FIXTURE_CELLS:return fixture_cell(name)
    m=Model();b=m.box
    if name.startswith('shrine_roof_'):
        source=make(name.replace('shrine_roof_','kawara_'))
        for element,material in zip(source.elements,source.materials):
            b({'kawara':'verdigris','kawara_edge':'verdigris_edge'}[material],*element['from'],*element['to'])
    elif name=='shrine_bracket':
        # Layered tokyo-inspired capital; a restrained original four-petal gold crest.
        for coords in [(5,0,5,11,4,11),(3,4,3,13,7,13),(1,7,2,15,10,14),(0,10,1,16,13,15),(0,13,0,16,16,16)]:b('dark_timber',*coords)
        for x in (1,12):
            b('board',x,7.2,1.8,x+3,9.7,2)
            b('gold',x+.8,7.8,1.65,x+2.2,9.1,1.85)
        b('gold',0,13.2,0,16,13.65,.18)
        b('gold',0,13.2,15.82,16,13.65,16)
        b('gold',6.4,4.6,2.8,9.6,6.7,3)
        b('dark_timber',6.8,4.9,2.7,9.2,6.4,2.82)
        for x,y in ((7.5,5),(7.5,5.85),(6.9,5.45),(8.1,5.45)):b('gold',x,y,2.58,x+.8,y+.7,2.72)
    elif name=='shrine_rope':
        # Sagging braided rope, three straw tassels and folded zigzag paper strips.
        for i in range(8):
            y=11+abs(i-3.5)*.55
            b('shrine_rope',i*2,y,6.8,i*2+2,y+1.6,9.2)
            b('ricecord',i*2+.6,y+.2,6.65,i*2+1,y+1.3,6.85)
        for x in (2,7.6,13.2):
            for offset in (0,.4,.8):b('shrine_rope',x+offset,6,7.6,x+offset+.25,11.2,8.2)
        for x in (4,10):
            for j in range(4):
                xx=x+(j%2)*.9;yy=9.6-j*1.8
                b('shide',xx,yy,6.2,xx+1.7,yy+1.8,6.4)
    elif name in ('plaster','dark_timber','board','stone_paving','vermilion_timber'):
        mat='vermilion' if name=='vermilion_timber' else name;b(mat,0,0,0,16,16,16)
        # Fine end-grain bands/mortise accents on full structural timbers remain inset in the texture.
    elif name in ('kawara_tile','kawara_slope'):
        slope=name=='kawara_slope'
        rows=8 if slope else 4;depth=16/rows
        for row in range(rows):
            z=row*depth;h=1+row*2 if slope else 14
            b('kawara',0,0,z,16,h,z+depth)
            for x in (0,4,8,12):
                previous=0
                for off,width,rise in ((0,4,.25),(.5,3,.6),(1,2,.9),(1.5,1,1)):
                    b('kawara_edge' if row%2==0 and off==1.5 else 'kawara',x+off,h+previous,z+.05,x+off+width,h+rise,z+depth-.05);previous=rise
                b('kawara_edge',x+.5,h,z,x+3.5,h+.5,z+.2)
    elif name=='kawara_ridge':
        b('kawara',0,0,2,16,2,14)
        previous=2
        for i,(z,h) in enumerate(((4,5),(5,6.5),(6,7.5),(7,8))):b('kawara_edge' if i==3 else 'kawara',0,previous,z,16,h,16-z);previous=h
        for x in (0,4,8,12):b('kawara_edge',x,5,5,x+.25,7,11)
    elif name in ('lattice','shoji'):
        for x in (0,14.8):b('dark_timber',x,0,7,x+1.2,16,9)
        for y in (0,14.8):b('dark_timber',1.2,y,7,14.8,y+1.2,9)
        if name=='shoji':b('paper',1.2,1.2,7.8,14.8,14.8,8.2)
        for x in (3,6,9,12):b('board',x,1.2,7.2,x+.45,14.8,8.8)
        for y in (4,8,12):b('board',1.2,y,7.1,14.8,y+.45,8.9)
        if name=='lattice':
            b('bamboo',0,14.3,5.7,16,15,6.3)
            for x in (1,5.8,10.6):
                b('indigo',x,10.5,5.8,x+4.4,14.3,6.05)
                b('paper',x+.3,10.7,5.7,x+4.1,10.95,5.82)
            b('paper',7,11.5,5.62,8.7,13.4,5.78)
        else:b('dark_timber',12.9,6.8,6.6,13.7,9,7)
    elif name=='paper_lantern':
        b('dark_timber',7.5,14,7.5,8.5,16,8.5)
        b('dark_timber',5.5,12.8,5.5,10.5,14,10.5)
        b('lantern_paper',4,4,4,12,12,12)
        for y in (3,12):b('lantern_paper',5,y,5,11,y+1,11)
        for y in (4.4,6.3,8.2,10.1,11.7):
            for z in (3.9,11.95):b('ricecord',4,y,z,12,y+.15,z+.15)
            for x in (3.9,11.95):b('ricecord',x,y,4,x+.15,y+.15,12)
        b('dark_timber',5.5,1.5,5.5,10.5,3,10.5)
        b('vermilion',7.2,0,7.5,8.8,1.5,8.5)
        for y in (6,8.4):b('vermilion',6.5,y,3.8,9.5,y+.45,4)
        b('vermilion',7.6,5.5,3.75,8.2,10,4)
    elif name=='stone_lantern':
        b('stone',2,0,2,14,1.2,14);b('stone',3,1.2,3,13,2,13);b('stone',6,2,6,10,7,10)
        b('stone',4,7,4,12,7.7,12);b('lantern_paper',5,7.7,5,11,10.5,11)
        for x in (4,11):
            for z in (4,11):b('stone',x,7.7,z,x+1,11,z+1)
        b('stone',2,11,2,14,12,14);b('stone',3,12,3,13,13,13);b('stone',4.5,13,4.5,11.5,14,11.5)
        b('stone',7,14,7,9,16,9);b('moss',2.4,1.21,2.3,5.5,1.26,4.2)
    elif name=='saisen_box':
        b('dark_timber',1,0,2,15,2,14);b('board',3,2,3,13,10,13)
        for x in (2,13):b('dark_timber',x,2,2.8,x+1,10,13.2)
        for y in (2.5,8.5):b('dark_timber',2,y,2.7,14,y+.7,3)
        b('dark_timber',0,10,1,16,10.7,15)
        for x in range(1,15,2):b('board',x,10.7,2,x+.8,12,14)
        for z in (1,14):b('dark_timber',0,10.7,z,16,12,z+1)
        b('gold',6,4.3,2.5,10,7.7,2.75);b('dark_timber',6.45,4.7,2.4,9.55,7.3,2.52)
        for x,y in ((7.3,5.5),(7.3,6.5),(6.6,6),(8,6)):b('gold',x,y,2.3,x+.7,y+.65,2.43)
    elif name=='chozu_basin':
        b('stone',1,0,1,15,3,15)
        for x in (1,13):b('stone',x,3,1,x+2,9,15)
        for z in (1,13):b('stone',3,3,z,13,9,z+2)
        b('water',3,5.8,3,13,6,13)
        for x,z in ((4,4),(8,7),(6,10)):b('paper',x,6.01,z,x+2,6.025,z+.1)
        b('bamboo',12,9,12,13,16,13);b('bamboo',7,13,11,13,14,12)
        for y in (10,12.2,14.5):b('dark_timber',11.95,y,11.95,13.05,y+.15,13.05)
        b('water',7.2,6,11.3,7.45,13,11.55)
        b('bamboo',4.5,9.4,5,14,9.8,5.4)
        b('board',3,9,4,5,9.4,6)
        for x in (3,4.7):b('bamboo',x,9.4,4,x+.3,10.4,6)
        for z in (4,5.7):b('bamboo',3.3,9.4,z,4.7,10.4,z+.3)
        b('moss',1.1,3.1,1,2.4,4.2,1.08)
    elif name=='omamori_counter':
        b('dark_timber',1,0,2,15,2,14);b('board',1,2,2,15,9,14);b('dark_timber',0,9,1,16,11,15)
        for x in (1,4,7,10,13):b('dark_timber',x,2,1.8,x+.45,9,2)
        b('board',1,11,13,15,15,14);b('gold',2,13.3,12.8,14,13.6,13)
        for i,mat in enumerate(('rose','indigo','jade','vermilion')):
            x=2+i*3;b(mat,x,11,5,x+2,13,8);b('ricecord',x+.7,13,6,x+1.3,13.5,7)
            b('gold',x+.65,11.6,4.9,x+1.35,12.5,5.05)
        for i in range(3):b('paper',4+i*3,13.6,12.65,5.3+i*3,14.7,12.85)
    else:raise ValueError(name)
    return m

def write(path,data):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')

def preview(models,textures,filename='sakura-materials.png'):
    width=1280;height=math.ceil(len(models)/4)*325+32;im=Image.new('RGB',(width,height),(23,28,31));draw=ImageDraw.Draw(im)
    font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',18);small=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',13)
    for index,(name,m) in enumerate(models.items()):
        col=index%4;row=index//4;ox=col*320+160;oy=row*325+160
        draw.rounded_rectangle((col*320+10,row*325+10,col*320+310,row*325+313),radius=12,fill=(34,42,44))
        canvas=np.array(im);depth=np.full((height,width),-np.inf)
        bounds=np.array([e[k] for e in m.elements for k in ('from','to')]);lo=bounds.min(axis=0);hi=bounds.max(axis=0)
        scale=min(8,240/max(1,(hi-lo)[0]+(hi-lo)[2]));centerX=(lo[0]+hi[0])/2;centerZ=(lo[2]+hi[2])/2
        def project(v):x,y,z=v;return (ox+(x-centerX+z-centerZ)*scale,oy+(x-centerX-z+centerZ)*scale/2-y*scale+35)
        for e,mat in zip(m.elements,m.materials):
            x,y,z=e['from'];X,Y,Z=e['to']
            for pts,shade in [([(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],1.12),([(x,y,z),(X,y,z),(X,Y,z),(x,Y,z)],.82),([(X,y,z),(X,y,Z),(X,Y,Z),(X,Y,z)],.66)]:
                xy=np.array([project(v) for v in pts]);zz=np.array([v[0]+v[1]-v[2] for v in pts]);uv=np.array([[0,0],[1,0],[1,1],[0,1]])
                for inds in ((0,1,2),(0,2,3)):
                    vv=xy[list(inds)];zs=zz[list(inds)];tex=uv[list(inds)];left=max(0,int(vv[:,0].min()));right=min(width-1,int(vv[:,0].max())+1);top=max(0,int(vv[:,1].min()));bottom=min(height-1,int(vv[:,1].max())+1)
                    xx,yy=np.meshgrid(np.arange(left,right+1)+.5,np.arange(top,bottom+1)+.5);(ax,ay),(bx,by),(cx,cy)=vv;den=(by-cy)*(ax-cx)+(cx-bx)*(ay-cy)
                    if abs(den)<1e-9:continue
                    a=((by-cy)*(xx-cx)+(cx-bx)*(yy-cy))/den;b=((cy-ay)*(xx-cx)+(ax-cx)*(yy-cy))/den;c=1-a-b;d=a*zs[0]+b*zs[1]+c*zs[2];view=depth[top:bottom+1,left:right+1];mask=(a>=0)&(b>=0)&(c>=0)&(d>view)
                    u=a*tex[0,0]+b*tex[1,0]+c*tex[2,0];v=a*tex[0,1]+b*tex[1,1]+c*tex[2,1]
                    colors=np.clip(textures[mat][np.clip((v*31).astype(int),0,31),np.clip((u*31).astype(int),0,31)]*shade,0,255).astype(np.uint8)
                    canvas[top:bottom+1,left:right+1][mask]=colors[mask];view[mask]=d[mask]
        im=Image.fromarray(canvas);draw=ImageDraw.Draw(im)
        draw.text((col*320+22,row*325+256),EN[IDS.index(name)] if name in IDS else name,font=font,fill=(232,223,196));draw.text((col*320+22,row*325+284),f'{len(m.elements)} elements / sakura_{name}',font=small,fill=(144,170,160))
    draw.text((20,height-23),'Original native block geometry / Sakura quarter / software lighting preview',font=small,fill=(156,173,163));out=ROOT/'build/sakura-check';out.mkdir(parents=True,exist_ok=True);im.save(out/filename)

def main():
    textures={name:texture(name,color) for name,color in COLORS.items()};models={name:make(name) for name in IDS}
    # Splitting preserves every cuboid volume (including overlaps) without lost geometry.
    def volume(model):return sum(math.prod(b-a for a,b in zip(e['from'],e['to'])) for e in model.elements)
    for basin in (False,True):
        parts=[models[n] for n in FIXTURE_CELLS if n.startswith('chozu')==basin]
        assert len(parts)==(6 if basin else 3)
        assert math.isclose(sum(map(volume,parts)),volume(fixture(basin)),rel_tol=1e-10)
        for part in parts:
            for element in part.elements:
                a=element['from'];b=element['to'];original=(list(a),list(b))
                for turn in range(4):
                    a,b=[16-b[2],a[1],a[0]],[16-a[2],b[1],b[0]]
                    assert all(-1e-9<=lo<hi<=16+1e-9 for lo,hi in zip(a,b))
                assert np.allclose(a,original[0]) and np.allclose(b,original[1])
    print('PASS 9 fixture cells: complete split-volume conservation and four bounded rotations')
    for index,(name,m) in enumerate(models.items()):
        id='sakura_'+name
        for element in m.elements:
            assert all(0<=a<b<=16 for a,b in zip(element['from'],element['to'])),id
            assert set(element['faces'])=={'north','south','east','west','up','down'}
        data=m.data();write(ASSET/f'models/block/{id}.json',data)
        shaped=5<=index<=12 or index==14 or index>=16
        variants={f'facing={direction}':{'model':'neonward:block/'+id,'y':yaw} for direction,yaw in [('north',0),('east',90),('south',180),('west',270)]} if shaped else {'':{'model':'neonward:block/'+id}}
        write(ASSET/f'blockstates/{id}.json',{'variants':variants});write(ASSET/f'items/{id}.json',{'model':{'type':'minecraft:model','model':'neonward:block/'+id}})
        write(ROOT/f'resources/data/neonward/loot_table/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'neonward:'+id}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
        print('PASS',id,len(m.elements),'elements',len(variants),'variants')
    # Language insertion belongs to a targeted apply_patch so concurrent edits are preserved.
    for locale in ('ja_jp','en_us'):
        labels=json.loads((ASSET/f'lang/{locale}.json').read_text(encoding='utf-8'))
        assert all(labels.get('block.neonward.sakura_'+name) for name in IDS),locale
    for name,m in models.items():
        for material in m.materials:assert (ASSET/f'textures/block/sakura_{material}.png').is_file()
        for variant in json.loads((ASSET/f'blockstates/sakura_{name}.json').read_text())['variants'].values():
            assert (ASSET/('models/'+variant['model'].split(':')[1]+'.json')).is_file()
    preview(models,textures);print(f'PASS {len(models)} block/item/loot definitions, original texture links and JA/EN names')
    preview({'Offering box / 3 x 1 m':fixture(False),'Chozu basin / 3 x 2 m':fixture(True)},textures,'sakura-fixtures.png')
    print('Preview:',ROOT/'build/sakura-check/sakura-materials.png')

if __name__=='__main__':main()
