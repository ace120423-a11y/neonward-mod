"""Deterministic articulated meshes for the approved Frost Citadel concept sheets.

All geometry is original to this roster. Coordinates are metres, Y up, facing -Z.
Run from any directory. Generates game assets plus orthographic review sheets.
"""
from pathlib import Path
import json, math, random
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'resources/assets/neonward'
OUT=ROOT/'design/frost-bosses/model-review'
OUT.mkdir(parents=True,exist_ok=True)
PI=math.pi
ICE,SNOW,IRON,GOLD,NAVY,RED,LIGHT,GLASS=range(8)
COLORS=[(99,170,196),(218,229,230),(64,76,85),(154,130,85),(31,48,71),(101,37,48),(128,237,255),(155,197,208)]
NAMES=['鍵束の番人','雪喰いの大口','灯籠の白狐','弔鐘の修道士','霜の料理長','硝子の兎','鎖の侍女','吹雪のスノードーム','象牙の大猪','歩く城門','鏡の公爵夫人','死者の雪橇','歩くパイプオルガン','氷翼の風車','空飛ぶ絨毯エイ','双子の氷棺','王冠蜘蛛','蝋燭の聖歌隊','雹砲の大亀','饗宴の王','極光の大蛾','氷の操り人形','城塞の白鯨','茨の花嫁','砂時計の裁判官','凍れる大聖堂','日蝕の雄鹿','嵐のゆりかご','冠なき女王','冬の心臓']
ENGLISH=['KEY WARDEN','SNOW MOUTH','LANTERN FOX','BELL MONK','RIME COOK','GLASS HARE','CHAIN MAID','SNOWGLOBE','IVORY BOAR','PORTCULLIS','MIRROR DUCHESS','SLEIGH REAPER','PIPE ORGAN','FROST MILL','CARPET RAY','COFFIN TWINS','CROWN SPIDER','WAX CHOIR','HAIL TURTLE','BANQUET KING','AURORA MOTH','ICE MARIONETTE','WINTER WHALE','THORN BRIDE','HOURGLASS JUDGE','FROZEN CATHEDRAL','ECLIPSE STAG','STORM CRADLE','CROWNLESS QUEEN','THE WINTER HEART']

def add(a,b):return tuple(x+y for x,y in zip(a,b))
def sub(a,b):return tuple(x-y for x,y in zip(a,b))
def mul(a,s):return tuple(x*s for x in a)
def cross(a,b):return (a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])
def unit(a):return mul(a,1/max(1e-8,math.sqrt(sum(x*x for x in a))))

class Mesh:
 def __init__(self,n):self.n=n;self.parts=[];self.faces=[];self.group=None;self.origin=(0,0,0)
 def bone(self,name,p,anim=''):
  self.group=name;self.origin=p
  self.parts.append(dict(name=name,p=p,size=[0,0,0],rot=[0,0,0],mat=0,anim=anim,parent=None,vertices=[]))
 def root(self):self.group=None;self.origin=(0,0,0)
 def poly(self,points,mat):
  if len(points)==3:points=points+[points[-1]]
  normal=unit(cross(sub(points[1],points[0]),sub(points[2],points[0])))
  verts=[]
  for i,p in enumerate(points):
   uv=((mat+.07+(i in (1,2))*.86)/8,.07+(i>=2)*.86)
   verts.append([*sub(p,self.origin),*normal,*uv])
  # Each material/bone pair is batched into one mesh part rather than hundreds of cubes.
  key=(self.group,mat)
  part=next((p for p in reversed(self.parts) if p.get('_key')==key),None)
  if part is None:
   part=dict(name=f'mesh_{len(self.parts)}',p=[0,0,0],size=[0,0,0],rot=[0,0,0],mat=mat,anim='',parent=self.group,vertices=[],_key=key)
   self.parts.append(part)
  part['vertices'].extend(verts);self.faces.append((points,mat))
 def box(self,c,s,m):
  x,y,z=c;a,b,d=[v/2 for v in s]
  v=[(x-a,y-b,z-d),(x+a,y-b,z-d),(x+a,y+b,z-d),(x-a,y+b,z-d),(x-a,y-b,z+d),(x+a,y-b,z+d),(x+a,y+b,z+d),(x-a,y+b,z+d)]
  for face in [(0,3,2,1),(4,5,6,7),(0,4,7,3),(1,2,6,5),(3,7,6,2),(0,1,5,4)]:self.poly([v[i] for i in face],m)
 def beam(self,a,b,r,m,r2=None,sides=8):
  axis=unit(sub(b,a));u=unit(cross(axis,(0,1,0) if abs(axis[1])<.9 else (1,0,0)));v=cross(axis,u);r2=r if r2 is None else r2
  rings=[[add(c,add(mul(u,rad*math.cos(i*2*PI/sides)),mul(v,rad*math.sin(i*2*PI/sides)))) for i in range(sides)] for c,rad in [(a,r),(b,r2)]]
  for i in range(sides):
   j=(i+1)%sides;self.poly([rings[0][i],rings[0][j],rings[1][j],rings[1][i]],m)
   self.poly([a,rings[0][j],rings[0][i]],m);self.poly([b,rings[1][i],rings[1][j]],m)
 def orb(self,c,s,m,rings=6,segments=12):
  def point(j,i):
   t=j*PI/rings;a=i*2*PI/segments
   return add(c,(s[0]*math.sin(t)*math.cos(a),s[1]*math.cos(t),s[2]*math.sin(t)*math.sin(a)))
  for j in range(rings):
   for i in range(segments):self.poly([point(j,i),point(j,i+1),point(j+1,i+1),point(j+1,i)],m)
 def ring(self,c,rx,ry,r,m,axis='z',segments=20):
  def p(i):
   a=i*2*PI/segments;d=(rx*math.cos(a),ry*math.sin(a),0) if axis=='z' else (rx*math.cos(a),0,ry*math.sin(a))
   return add(c,d)
  for i in range(segments):self.beam(p(i),p(i+1),r,m,sides=5)
 def spike(self,a,b,r=.12,m=ICE):self.beam(a,b,r,m,.008,6)
 def eye(self,c,r=.075):self.orb(c,(r,r,r*.5),LIGHT,3,8)
 def chain(self,a,b,count=8):
  for i in range(count):
   p=add(a,mul(sub(b,a),(i+.5)/count));self.ring(p,.075,.11,.027,IRON,'z' if i%2 else 'y',8)
 def icicles(self,a,b,count=9,length=.35):
  for i in range(count):
   p=add(a,mul(sub(b,a),i/max(1,count-1)));self.spike(p,add(p,(0,-length*(.5+(i*7%9)/9),0)),.065,ICE)
 def crown(self,c,r=.5,n=7):
  self.ring(c,r,r,.07,GOLD,'y')
  for i in range(n):
   a=i*2*PI/n;p=add(c,(r*math.cos(a),0,r*math.sin(a)));self.spike(p,add(p,(0,.4+(i%2)*.2,0)),.09,GOLD);self.eye(add(p,(0,.2,0)),.04)
 def tower(self,x,y,z,h=.9,w=.35):
  self.box((x,y+h/2,z),(w,h,w),SNOW);self.spike((x,y+h,z),(x,y+h+w*2,z),w*.75,ICE)
  self.box((x,y+h*.65,z-w*.51),(w*.33,h*.27,.02),NAVY)
 def lantern(self,c,s=.25):
  self.box(c,(s*.65,s,s*.65),LIGHT)
  for dx in [-1,1]:
   for dz in [-1,1]:self.beam(add(c,(dx*s*.4,-s*.6,dz*s*.4)),add(c,(dx*s*.4,s*.6,dz*s*.4)),.025,IRON)
  self.box(add(c,(0,-s*.6,0)),(s,.06,s),IRON);self.spike(add(c,(0,s*.5,0)),add(c,(0,s,0)),s*.7,IRON)
 def legs(self,c,width,length,height,count=4,mat=IRON):
  for i in range(count):
   side=1 if i%2 else -1;z=c[2]+(-length/2+length*(i//2)/max(1,count//2-1));a=(c[0]+side*width*.35,c[1],z);k=(c[0]+side*width*.65,height*.55,z+.12);b=(c[0]+side*width*.72,.13,z-.12)
   self.bone('leg_'+str(len(self.parts)),a,'leg'+str(i%2));self.beam(a,k,.12,mat,.09);self.orb(k,(.14,.14,.14),GOLD,3,8);self.beam(k,b,.09,mat,.045);self.box(b,(.25,.14,.4),mat);self.root()
 def face(self,c,w=.4):
  self.orb(c,(w,w*1.15,w*.65),SNOW,5,10)
  for s in [-1,1]:
   self.orb(add(c,(s*w*.4,.05,-w*.58)),(w*.22,w*.25,w*.08),NAVY,3,7);self.eye(add(c,(s*w*.4,.05,-w*.67)),w*.09)
  self.spike(add(c,(0,-w*.2,-w*.63)),add(c,(0,-w*.05,-w*.7)),w*.09,IRON)
  for i in range(5):self.box(add(c,((i-2)*w*.13,-w*.48,-w*.58)),(w*.09,w*.17,w*.13),SNOW)
 def cloth(self,top,bottom,width,m,folds=10):
  for i in range(folds):
   x0=(i/folds-.5)*width;x1=((i+1)/folds-.5)*width;depth=.09 if i%2 else -.09
   self.poly([add(top,(x0*.4,0,0)),add(bottom,(x0,0,depth)),add(bottom,(x1,0,-depth)),add(top,(x1*.4,0,0))],m)

def build(n):
 m=Mesh(n)
 if n==1:
  m.legs((0,.9,0),1,.55,.9);m.box((0,1.05,0),(.8,.6,.6),IRON);m.ring((0,1.65,0),.45,.45,.1,IRON)
  m.beam((0,1.8,0),(0,3.1,0),.13,IRON);m.box((.22,2.9,0),(.6,.17,.2),IRON);m.box((.32,2.7,0),(.16,.4,.2),IRON)
  for i in range(7):
   a=i*2*PI/7;p=(math.cos(a)*.62,1.1+math.sin(a)*.35,-.35);m.ring(p,.12,.14,.04,GOLD);m.beam(add(p,(0,-.1,0)),add(p,(0,-.5,0)),.05,IRON);m.box(add(p,(.06,-.45,0)),(.19,.07,.08),IRON)
  m.box((0,1.08,-.34),(.3,.32,.09),GOLD);m.eye((0,1.12,-.4),.07)
 elif n==2:
  m.orb((0,1.35,.2),(1.2,1.5,.7),SNOW);m.orb((0,1.5,-.47),(.63,1.1,.09),NAVY)
  for s in [-1,1]:
   for i in range(8):m.spike((s*(.5-i*.035),.45+i*.27,-.55),(s*.15,.58+i*.24,-.67),.13,ICE)
   for i in range(5):m.spike((s*.8,.4+i*.48,.12),(s*(1.15+i*.06),.8+i*.5,.1),.25,ICE)
  m.eye((0,2.7,-.35),.09)
 elif n in (3,9,12,19,27):
  if n==3:
   m.orb((0,1.1,0),(.38,.42,.9),SNOW);m.legs((0,1,0),.65,1.25,1,6,SNOW);m.orb((0,1.55,-.8),(.32,.3,.4),SNOW);m.spike((0,1.48,-.95),(0,1.43,-1.5),.22,SNOW)
   for s in [-1,1]:m.spike((s*.23,1.7,-.7),(s*.4,2.3,-.65),.17,SNOW);m.eye((s*.23,1.6,-1.03),.065)
   for i in range(3):
    a=(0,1.1,.65);b=((i-1)*.8,2.2,.95);c=((i-1)*1.1,2.7,.05);m.bone('tail'+str(i),a,'tail'+str(i));m.beam(a,b,.18,SNOW,.25);m.beam(b,c,.25,SNOW,.04);m.chain(c,add(c,(0,-.45,0)),4);m.lantern(add(c,(0,-.6,0)));m.root()
  elif n==9:
   m.orb((0,1.15,0),(.85,.72,1.05),SNOW);m.legs((0,1,0),1.3,1.3,1,4,IRON);m.orb((0,1,-1),(.6,.5,.5),IRON);m.box((0,.85,-1.48),(.7,.3,.18),IRON)
   for s in [-1,1]:m.beam((s*.4,.7,-1.1),(s*.65,.65,-1.65),.16,SNOW,.11);m.spike((s*.65,.65,-1.65),(s*.65,1.2,-1.8),.11,SNOW);m.eye((s*.45,1.18,-1.3))
   for i in range(9):m.spike((math.sin(i)*.6,1.5,i*.2-.8),(math.sin(i)*.75,2.15,i*.2-.7),.27,ICE)
  elif n==12:
   m.legs((0,1.1,-.5),.7,1.3,1.1,4,SNOW);m.beam((0,1.2,-1.3),(0,1.2,.1),.18,SNOW);m.beam((0,1.1,-1),(0,2,-1.2),.15,SNOW);m.face((0,2.15,-1.4),.23)
   antlers(m,(0,2.35,-1.35),.8);m.box((0,.65,.7),(1,.15,1.6),GOLD);m.box((0,1.15,1.35),(1,.9,.15),RED)
   for s in [-1,1]:m.beam((s*.55,.25,-1.8),(s*.55,.25,1.6),.06,IRON);m.spike((s*.55,.25,-1.8),(s*.55,.55,-2),.08,IRON)
  elif n==19:
   m.legs((0,.6,0),1.8,1.4,.6,4,IRON);m.orb((0,.9,0),(1.1,.8,1.25),IRON);m.face((0,.65,-1.25),.35);m.orb((0,1.6,.15),(.7,.85,.75),ICE,5,9)
   for a in range(0,360,45):
    t=a*PI/180;m.box((math.cos(t)*.9,1.05,math.sin(t)*.95),(.3,.3,.3),GOLD)
  else:
   m.orb((0,1.7,0),(.4,.6,.9),IRON);m.legs((0,1.7,0),.9,1.25,1.7,4,IRON);m.beam((0,1.9,-.6),(0,2.7,-.9),.22,IRON,.14);m.face((0,2.8,-1),.3);antlers(m,(0,3,-.8),1.4)
   m.orb((0,4.1,-.8),(.45,.45,.12),NAVY);m.ring((0,4.1,-.85),.5,.5,.03,LIGHT)
 elif n==4:
  m.legs((0,.45,0),1.1,.2,.45,2,IRON)
  for i in range(8):m.beam((0,.55+i*.18,0),(0,.73+i*.18,0),.95-i*.08,IRON,.95-(i+1)*.08,16)
  m.ring((0,.55,0),1,1,.08,GOLD,'y');m.face((0,.7,-.35),.23);m.ring((0,2.35,0),.22,.25,.07,IRON);m.cloth((-.4,1.8,-.42),(-.75,.65,-.85),.35,RED)
 elif n==5:
  m.legs((0,.55,0),1,.2,.55,2,IRON);m.orb((0,1.15,0),(.75,.65,.65),IRON);m.ring((0,1.6,0),.72,.6,.07,GOLD,'y');m.face((0,1.95,0),.32)
  m.beam((0,2.15,0),(0,2.65,0),.34,SNOW,.4);m.orb((0,2.7,0),(.45,.2,.4),SNOW)
  m.beam((-.55,1.55,0),(-1,1,-.3),.12,IRON);m.box((-1,1.4,-.3),(.1,.7,.5),IRON);m.cloth((.15,1.6,-.65),(.15,.7,-.7),.75,SNOW)
 elif n==6:
  m.orb((0,1,0),(.4,.6,.45),GLASS,4,7);m.legs((0,.7,0),.7,.6,.7,4,GLASS);m.orb((0,1.8,-.25),(.32,.4,.28),GLASS,4,7)
  for s in [-1,1]:m.beam((s*.16,2,-.25),(s*.6,3.1,-.15),.15,GLASS,.025,4);m.eye((s*.17,1.85,-.48));m.beam((s*.32,1.35,-.1),(s*.4,.8,-.55),.065,IRON)
 elif n==7:
  m.beam((0,.5,0),(0,1.65,0),.8,NAVY,.3,12);m.cloth((0,1.65,-.3),(0,.55,-.72),1.2,SNOW);m.box((0,1.85,0),(.55,.4,.3),NAVY)
  for s in [-1,1]:m.bone('chain_arm'+str(s),(s*.3,1.95,0),'hand');m.chain((s*.3,1.95,0),(s*.85,1.2,-.1),7);m.orb((s*.85,1.2,-.1),(.12,.13,.1),SNOW);m.root()
  m.beam((-.9,.6,-.1),(-.9,2.4,-.1),.035,GOLD)
  for i in range(7):m.spike((-.9,2.35,-.1),(-.9+math.sin(i)*.3,2.8,math.cos(i)*.2-.1),.09,SNOW)
 elif n==8:
  m.legs((0,.6,0),1.5,.8,.6,6,IRON);m.beam((0,.45,0),(0,.8,0),.7,GOLD,.55);m.ring((0,1.6,0),.9,.95,.055,GLASS)
  m.ring((0,1.6,0),.9,.9,.055,GLASS,'y')
  for a in [-.7,0,.7]:m.tower(a*.6,1,0,1-abs(a)*.5,.23)
  for i in range(16):m.orb((math.sin(i*2.1)*.65,1+((i*7)%15)/12,math.cos(i*2.1)*.55),(.035,.035,.035),SNOW,2,4)
 elif n==10:
  m.legs((0,.85,0),1.7,.1,.85,2,IRON);m.box((0,2,0),(1.8,1.65,.45),SNOW);m.box((0,2,-.25),(1.3,1.35,.05),NAVY)
  for x in [-.55,-.27,0,.27,.55]:m.beam((x,1.25,-.32),(x,2.7,-.32),.055,IRON);m.spike((x,1.3,-.32),(x,.95,-.32),.09,IRON)
  for y in [1.5,2,2.5]:m.beam((-.65,y,-.32),(.65,y,-.32),.04,GOLD)
  for x in [-.9,0,.9]:m.tower(x,2.65,0,.4,.38)
 elif n==11:
  m.orb((0,1.7,0),(.72,1.15,.08),GLASS,8,16);m.ring((0,1.7,-.05),.78,1.2,.11,IRON);m.face((0,1.9,-.12),.22);m.crown((0,2.85,0),.3)
  for i in range(9):m.spike((math.sin(i*2)*.6,.8,0),(math.sin(i*2)*.9,.1+(i%3)*.12,0),.13,ICE)
 elif n==13:
  m.legs((0,.65,0),1.7,1,.65,8,IRON);m.box((0,1.15,0),(1.6,1,.75),RED)
  for i in range(11):
   x=(i-5)*.14;h=1+1.2*(1-abs(i-5)/6);m.beam((x,1.5,.1),(x,1.5+h,.1),.065,GOLD);m.spike((x,1.5+h,.1),(x,1.65+h,.1),.085,SNOW)
   m.box((x,1.15,-.45),(.08,.6,.15),IRON);m.box((x,1.55,-.5),(.12,.07,.25),SNOW)
 elif n==14:
  m.legs((0,.65,0),1.2,.2,.65,2,IRON);m.beam((0,.5,0),(0,2.1,0),.65,SNOW,.25,4);m.bone('wind_sails',(0,2,-.5),'wheel')
  for i in range(4):
   a=i*PI/2+.65;u=(math.cos(a),math.sin(a),0);v=(-u[1],u[0],0);a0=add((0,2,-.5),mul(u,.2));a1=add((0,2,-.5),mul(u,1.5));m.beam(a0,a1,.05,IRON);m.poly([a0,add(a0,mul(v,.35)),add(a1,mul(v,.5)),a1],ICE)
  m.root()
 elif n==15:
  m.bone('carpet',(0,1.4,0),'hover')
  m.poly([(-1.3,1.6,0),(0,1.4,-1),(1.3,1.6,0),(0,1.3,1)],NAVY)
  for s in [-1,1]:m.beam((0,1.4,-1),(s*1.3,1.6,0),.035,GOLD);m.beam((s*1.3,1.6,0),(0,1.3,1),.035,GOLD)
  m.eye((0,1.45,-.4),.22)
  for i in range(9):m.chain((0,1.3,1),(math.sin(i)*.55,.4,1.8+i*.05),5)
  m.root()
 elif n==16:
  for s in [-1,1]:
   m.bone('coffin'+str(s),(s*.6,1.3,0),'hover');m.box((s*.6,1.3,0),(.65,2,.35),ICE);m.box((s*.6,1.3,-.2),(.45,1.7,.06),NAVY);m.face((s*.6,1.9,-.26),.17);m.cloth((s*.6,1.65,-.25),(s*.6,.55,-.26),.45,RED);m.crown((s*.6,2.35,0),.27);m.root()
  m.chain((-.3,1.5,0),(.3,1.3,0),7)
 elif n==17:
  m.legs((0,.8,0),2,1,.8,6,IRON);m.crown((0,1.1,0),.85,11);m.orb((0,.9,0),(.6,.25,.6),NAVY)
 elif n==18:
  m.beam((0,.1,0),(0,1.3,0),.9,SNOW,.45,12)
  for i in range(7):
   x=(i-3)*.2;y=1.5+(3-abs(i-3))*.32;z=(i%2)*.25;m.beam((x,.5,z),(x,y,z),.15,SNOW,.1);m.face((x,y-.17,z-.04),.11);m.spike((x,y+.05,z),(x,y+.4,z),.075,LIGHT)
  for s in [-1,1]:m.beam((s*.55,1.2,0),(s*1.05,.4,0),.14,SNOW,.04)
 elif n==20:
  m.legs((0,.85,0),2,1.2,.85,4,IRON);m.box((0,1.05,0),(2.1,.2,1.4),IRON);m.cloth((0,1.2,-.7),(0,.55,-.8),2.2,RED)
  for i in range(9):m.spike((i*.22-.9,.95,-.83),(i*.22-.9,.7,-.88),.07,SNOW)
  for x in [-.7,0,.7]:m.ring((x,1.18,-.2),.2,.2,.04,SNOW,'y');m.beam((x,1.2,.35),(x,1.55,.35),.045,GOLD);m.spike((x,1.55,.35),(x,1.75,.35),.06,LIGHT)
  m.box((0,1.7,.6),(.8,1,.15),RED);m.crown((0,2.2,.6),.38)
 elif n==21:
  m.orb((0,1.6,0),(.2,.65,.25),IRON)
  for s in [-1,1]:
   m.bone('wing'+str(s),(0,1.7,0),'wing'+str(s))
   for upper in [True,False]:
    pts=[(s*.12,1.65,0),(s*1.7,3 if upper else .6,.1),(s*1.4,1.7 if upper else .35,.2),(s*.2,1.3,0)]
    m.poly(pts,ICE if upper else GLASS)
    for p in pts[1:3]:m.beam(pts[0],p,.035,GOLD)
    m.ring((s*1.1,2.1 if upper else .9,-.02),.15,.2,.05,NAVY);m.eye((s*1.1,2.1 if upper else .9,-.05),.08)
   m.root()
  m.face((0,2.25,0),.17)
 elif n==22:
  m.box((0,3.1,0),(1.15,.3,.6),ICE)
  for i in range(5):
   x=(i-2)*.22;m.beam((x,3.1,-.15),(x,2.85,-.5),.09,ICE);m.beam((x,2.85,-.5),(x*.6,1.8,-.4),.012,IRON)
  m.bone('doll',(0,1.1,0),'hover');m.face((0,1.9,0),.22);m.box((0,1.45,0),(.3,.4,.2),SNOW);m.beam((0,.45,0),(0,1.3,0),.48,NAVY,.18,8)
  for s in [-1,1]:m.beam((s*.15,1.65,0),(s*.6,1.25,0),.065,SNOW);m.beam((s*.12,.7,0),(s*.18,.2,0),.07,SNOW)
  m.root()
 elif n==23:
  m.orb((0,1.5,0),(1,.75,2),SNOW,7,16);m.orb((0,1.3,-1.3),(.9,.45,.7),NAVY,5,12)
  for s in [-1,1]:m.poly([(s*.6,1.2,0),(s*1.9,.7,.9),(s*.65,1,.65)],ICE);m.poly([(0,1.6,1.8),(s*1.4,2.2,2.6),(s*.5,1.5,2.4)],SNOW);m.eye((s*.75,1.7,-1.3),.1)
  for i in range(7):m.tower((i%2-.5)*.6,2, i*.38-1.2,.5+(i%3)*.25,.25)
  m.icicles((-.7,1.25,-1.8),(.7,1.25,-1.8),15,.65)
 elif n==24:
  m.beam((0,.1,0),(0,2.1,0),.95,SNOW,.2,12);m.box((0,2.25,0),(.4,.4,.25),IRON);m.face((0,2.6,0),.2)
  antlers(m,(0,2.6,0),1.2)
  for i in range(9):
   y=.3+i*.23;x=math.sin(i*2)*(.8-y*.2);m.beam((x,y,-.3),(x+.15,y+.25,-.3),.04,IRON);m.orb((x,y,-.43),(.12,.1,.07),RED,3,7)
  m.orb((0,2.95,-.05),(.32,.28,.2),RED,4,9)
 elif n==25:
  m.legs((0,.55,0),1.2,.3,.55,2,IRON)
  for y in [.7,2.8]:m.beam((0,y,0),(0,y+.1,0),.7,GOLD,sides=12)
  m.beam((0,.8,0),(0,1.7,0),.55,GLASS,.04,12);m.beam((0,1.7,0),(0,2.8,0),.04,GLASS,.55,12)
  for s in [-1,1]:
   m.beam((s*.6,.8,0),(s*.6,2.8,0),.05,GOLD);m.beam((s*.6,2.3,0),(s*1.3,2.3,0),.06,GOLD);m.chain((s*1.3,2.3,0),(s*1.3,1.4,0));m.beam((s*1.3,1.35,0),(s*1.3,1.45,0),.3,GOLD,.25)
 elif n==26:
  m.legs((0,.65,0),1.8,1.5,.65,4,SNOW);m.box((0,1.3,0),(1.5,1.3,1.7),SNOW);m.ring((0,1.7,-.89),.4,.4,.07,GOLD);m.orb((0,1.7,-.87),(.34,.34,.025),ICE,3,12)
  for x in [-.75,.75]:
   for z in [-.75,.75]:m.tower(x,.8,z,2,.35)
  m.tower(0,1.8,.1,1.7,.45);m.icicles((-.7,1,-.9),(.7,1,-.9))
 elif n==28:
  m.bone('cradle',(0,1.5,0),'hover');m.box((0,1.25,0),(1.6,.15,1),IRON)
  for s in [-1,1]:
   m.box((s*.85,1.7,0),(.15,1,1),SNOW);m.ring((s*.85,1.2,0),.6,.4,.06,IRON,'y')
   for i in range(5):m.beam((i*.32-.64,1.2,s*.5),(i*.32-.64,2,s*.5),.035,GOLD)
  for i in range(7):m.ring((0,1.3+i*.13,0),.55-i*.045,.4-i*.035,.035,ICE,'y',12)
  m.root()
 elif n==29:
  m.beam((0,.05,0),(0,2.4,0),.8,ICE,.16,8);m.box((0,2.55,0),(.4,.5,.23),SNOW);m.face((0,2.95,0),.22)
  for s in [-1,1]:m.spike((s*.2,2.7,0),(s*.5,2.95,0),.15,ICE);m.beam((s*.25,2.55,0),(s*.6,1.9,0),.065,IRON);m.orb((s*.8,1.6,0),(.1,.2,.07),SNOW)
  for i in range(6):
   a=i*2*PI/6;p=(math.cos(a)*1.2,2.8+math.sin(a)*.9,.4);m.box(p,(.28,.8,.06),GOLD);m.box(add(p,(0,0,-.04)),(.22,.7,.02),GLASS)
 elif n==30:
  m.bone('heart',(0,2,0),'pulse');m.orb((-.23,2.25,0),(.4,.45,.35),LIGHT,4,8);m.orb((.23,2.25,0),(.4,.45,.35),ICE,4,8);m.spike((0,2.1,0),(0,1.2,0),.55,ICE);m.root()
  for s in [-1,1]:
   for i in range(6):
    y=1+i*.35;m.beam((s*.3,y,.35),(s*.95,y+.15,0),.07,SNOW);m.beam((s*.95,y+.15,0),(s*.75,y-.05,-.4),.07,SNOW)
   for j in range(2):
    a=(s*.8,2.7-j*.9,0);m.bone('arm'+str(s)+str(j),a,'hand');m.beam(a,(s*1.6,2.4-j*.8,0),.13,SNOW);m.beam((s*1.6,2.4-j*.8,0),(s*1.4,1.2-j*.65,-.3),.09,IRON);m.root()
   m.tower(s*.65,2.8,.2,.8,.3)
  m.crown((0,4.1,0),.7,9)
 detail(m,n)
 return m

def detail(m,n):
 # Details use actual mesh geometry: silhouettes, seams, rivets, carved bands and frost.
 m.root()
 def rivets(a,b,count=9):
  for i in range(count):m.orb(add(a,mul(sub(b,a),i/max(1,count-1))),(.025,.025,.025),GOLD,2,5)
 def frame(c,w,h,mat=GOLD):
  for s in [-1,1]:m.beam(add(c,(s*w/2,-h/2,0)),add(c,(s*w/2,h/2,0)),.025,mat);m.beam(add(c,(-w/2,s*h/2,0)),add(c,(w/2,s*h/2,0)),.025,mat)
 def hem(y,r,mat=GOLD):m.ring((0,y,0),r,r,.035,mat,'y',24)
 if n==1:
  for s in [-1,1]:
   for i in range(9):m.spike((s*.6,.5,(i-4)*.05),(s*(.68+(i%3)*.03),.25,(i-4)*.06),.07,SNOW)
  frame((0,1.08,-.4),.4,.4);m.box((0,1.07,-.42),(.045,.16,.025),IRON)
  for y in [1.95,2.2,2.48,2.9]:m.ring((0,y,0),.16,.16,.035,GOLD,'y',12)
 elif n==2:
  for i in range(30):
   a=i*2.4;x=math.sin(a)*.8;y=.3+(i%9)*.27;z=.15+math.cos(a)*.4;m.orb((x,y,z),(.28,.22,.2),SNOW,3,7)
  for s in [-1,1]:
   m.beam((s*.95,1.3,0),(s*1.3,.85,-.1),.07,IRON)
   for i in range(3):m.spike((s*1.3,.85,-.1),(s*(1.45+i*.08),.65,-.3-i*.12),.04,IRON)
 elif n==3:
  for s in [-1,1]:
   for i in range(9):m.spike((s*.2,1.45-i*.035,-.55),(s*(.55-i*.02),1.15-i*.04,-.48),.1,SNOW)
   m.cloth((s*.27,1.45,-.72),(s*.38,.65,-.65),.18,NAVY)
  m.ring((0,1.4,-.65),.34,.32,.025,GOLD,'y',14)
 elif n==4:
  for y,r in [(.72,.87),(1,.75),(1.35,.58),(1.75,.38)]:m.ring((0,y,0),r,r,.025,GOLD,'y',24)
  for i in range(12):
   a=i*PI/6;x=math.cos(a)*.83;z=math.sin(a)*.83;m.box((x,.68,z),(.1,.17,.1),SNOW)
  m.cloth((.35,1.85,-.3),(.65,.65,-.8),.28,SNOW);m.icicles((-.6,.65,-.6),(.6,.65,-.6),8,.2)
 elif n==5:
  for s in [-1,1]:
   m.ring((s*.7,1.25,0),.2,.22,.055,GOLD)
   for i in range(6):m.box((s*.48,.75+i*.13,-.45),(.045,.045,.045),GOLD)
  for i in range(8):m.beam(((i-3.5)*.08,2.35,-.3),((i-3.5)*.1,2.7,-.25),.02,IRON)
  m.icicles((-.5,1.55,-.45),(.5,1.55,-.45),9,.45);frame((.15,1.1,-.73),.55,.55,IRON)
 elif n==6:
  for s in [-1,1]:
   for i in range(5):
    t=i/5;p=(s*(.18+t*.38),2+t, -.26+t*.1);m.beam(add(p,(-.05,0,0)),add(p,(.05,.08,0)),.025,GOLD)
   m.box((s*.18,1.55,-.35),(.08,.08,.08),GOLD)
  m.orb((0,.65,.4),(.19,.22,.2),SNOW,3,6)
 elif n==7:
  hem(.5,.82,SNOW)
  for i in range(18):
   a=i*2*PI/18;m.orb((math.cos(a)*.75,.62,math.sin(a)*.75),(.08,.08,.07),SNOW,2,6)
  for s in [-1,1]:m.poly([(0,1.7,-.32),(s*.25,1.87,-.32),(s*.2,1.58,-.34)],SNOW)
  for y in [1.78,1.9,2]:m.orb((0,y,-.17),(.025,.025,.025),GOLD,2,5)
 elif n==8:
  m.orb((0,1.6,0),(.9,.95,.9),GLASS,12,24)
  for i in range(16):
   a=i*PI/8;m.orb((math.cos(a)*.6,.7,math.sin(a)*.6),(.07,.08,.06),GOLD,3,6)
  m.ring((0,.8,0),.58,.58,.04,IRON,'y')
 elif n==9:
  for i in range(18):
   z=(i//3)*.3-.7;x=(i%3-1)*.43;m.orb((x,1.7-abs(x)*.22,z),(.28,.14,.22),SNOW,3,6)
  for s in [-1,1]:m.cloth((s*.7,1.6,0),(s*.86,.7,0),.35,RED);m.eye((s*.46,1.15,-1.35),.07)
 elif n==10:
  for s in [-1,1]:
   m.beam((s*.6,.7,0),(s*.6,1.4,0),.18,SNOW);m.beam((s*.95,2.2,0),(s*1.3,1.4,0),.16,IRON);m.spike((s*1.3,1.4,0),(s*1.4,.8,0),.18,SNOW)
   m.cloth((s*.9,2.6,-.3),(s*.9,1.5,-.33),.26,RED)
  for y in [1.3,1.7,2.1,2.5]:rivets((-.8,y,-.29),(.8,y,-.29),7)
 elif n==11:
  m.ring((0,1.7,-.07),.86,1.28,.035,GOLD,segments=28)
  for i in range(18):
   a=i*2*PI/18;p=(math.cos(a)*.85,1.7+math.sin(a)*1.25,0);m.spike(p,add(p,(math.cos(a)*.16,math.sin(a)*.16,0)),.09,IRON)
  m.cloth((0,1.7,-.15),(0,1,-.15),.4,SNOW)
 elif n==12:
  for i in range(6):
   z=-1+i*.19;m.ring((0,1.1,z),.27,.35,.035,SNOW,segments=12)
  for s in [-1,1]:
   m.beam((s*.5,.8,.1),(s*.5,1.2,1.35),.06,GOLD);m.ring((s*.5,1,1.25),.16,.2,.04,GOLD);frame((0,1.2,1.44),.8,.6)
 elif n==13:
  for s in [-1,1]:
   m.tower(s*.82,.65,0,1.4,.25);m.cloth((s*.65,1.8,-.45),(s*.65,.8,-.46),.2,NAVY)
  for i in range(9):
   x=(i-4)*.16;m.box((x,.85,-.45),(.06,.5,.04),GOLD)
  rivets((-.75,.7,-.45),(.75,.7,-.45),13)
 elif n==14:
  for i in range(4):
   a=i*PI/2+.65;u=(math.cos(a),math.sin(a),0);v=(-u[1],u[0],0)
   m.bone('sail_detail'+str(i),(0,2,-.5),'wheel')
   for j in range(1,5):
    p=add((0,2,-.53),mul(u,j*.3));m.beam(p,add(p,mul(v,.3+j*.04)),.025,IRON)
   m.root()
  for y in [.9,1.3,1.7]:m.box((0,y,-.34),(.15,.18,.05),NAVY)
 elif n==15:
  # Embroidered inset diamond, eye and fringe follow the carpet's floating bone.
  m.bone('embroidery',(0,1.4,0),'hover')
  for scale in [.75,.9]:
   v=[(-1.3*scale,1.61,0),(0,1.41,-scale),(1.3*scale,1.61,0),(0,1.31,scale)]
   for i in range(4):m.beam(v[i],v[(i+1)%4],.018,GOLD)
  for s in [-1,1]:
   for i in range(9):
    p=(s*(1.3-i*.13),1.6-i*.025,i*.1);m.beam(p,add(p,(s*.04,-.2,0)),.016,GOLD)
  m.root()
 elif n==16:
  for s in [-1,1]:
   m.bone('coffin_detail'+str(s),(s*.6,1.3,0),'hover');frame((s*.6,1.3,-.24),.54,1.82)
   for j in range(5):m.beam((s*.6-.14,1.5-j*.12,-.29),(s*.6+.14,1.5-j*.12,-.29),.025,SNOW)
   for side in [-1,1]:m.beam((s*.6+side*.23,1.7,-.24),(s*.6+side*.35,1.05,-.25),.035,SNOW)
   m.root()
 elif n==17:
  for i in range(12):
   a=i*PI/6;p=(math.cos(a)*.85,1.2,math.sin(a)*.85);m.orb(p,(.07,.11,.07),LIGHT if i%3==0 else RED,3,6)
  m.ring((0,1,0),.85,.85,.035,SNOW,'y')
  for s in [-1,1]:m.spike((s*.15,.85,-.55),(s*.25,.5,-.8),.12,IRON)
 elif n==18:
  for i in range(28):
   a=i*2*PI/28;r=.7+(i%3)*.035;y=.45+(i%7)*.12;m.beam((math.cos(a)*r,.1,math.sin(a)*r),(math.cos(a)*r*.7,y,math.sin(a)*r*.7),.055,SNOW,.09)
  m.cloth((0,1.2,-.4),(0,.3,-.81),.35,RED)
 elif n==19:
  for i in range(15):
   a=i*2.4;r=.65;y=1+abs(math.cos(a))*.28;m.box((math.sin(a)*r,y,math.cos(a)*r),(.32,.17,.32),SNOW)
  m.ring((0,1.45,0),.65,.7,.08,GOLD,'y');m.icicles((-.7,.85,-.75),(.7,.85,-.75),10,.3)
 elif n==20:
  for s in [-1,1]:
   m.cloth((s*.94,1.18,0),(s*1.1,.65,0),.7,RED)
   for i in range(3):m.beam((s*.7,1.45,.35),(s*.7+(i-1)*.15,1.65,.35),.025,GOLD);m.spike((s*.7+(i-1)*.15,1.65,.35),(s*.7+(i-1)*.15,1.85,.35),.045,LIGHT)
  frame((0,1.75,.5),.65,.8);m.eye((-.65,1.05,-.76),.1);m.eye((.65,1.05,-.76),.1)
 elif n==21:
  for s in [-1,1]:
   m.bone('wing_detail'+str(s),(0,1.7,0),'wing'+str(s))
   for i in range(1,7):
    t=i/7;m.beam((s*.15,1.65,0),(s*(.2+1.35*t),1.65+1.15*t,-.015),.012,GOLD)
    m.spike((s*(.8+t*.65),1.3-t*.7,.1),(s*(.8+t*.65),.7-t*.6,.1),.045,GLASS)
   m.root()
  for s in [-1,1]:m.beam((s*.1,2.35,0),(s*.4,2.85,0),.025,IRON)
 elif n==22:
  for s in [-1,1]:m.orb((s*.23,1.62,0),(.09,.09,.09),GOLD,3,8);m.chain((s*.35,3,-.35),(s*.6,1.3,0),14)
  for i in range(12):
   a=i*PI/6;m.spike((math.cos(a)*.4,.5,math.sin(a)*.4),(math.cos(a)*.45,.25,math.sin(a)*.45),.065,ICE)
 elif n==23:
  for i in range(9):
   z=-1.3+i*.3
   for s in [-1,1]:m.beam((s*.85,1.45,z),(s*.65,.75,z+.08),.04,ICE)
  for i in range(14):m.tower(math.sin(i)*.4,2.05,(i/14)*2.6-1.3,.3+(i%4)*.12,.12)
 elif n==24:
  for s in [-1,1]:
   prev=(s*.25,2.5,0)
   for i in range(10):
    y=2.4-i*.2;p=(s*(.3+i*.055),y,math.sin(i)*.1);m.beam(prev,p,.035,IRON);m.spike(p,add(p,(s*.15,.12,-.1)),.04,IRON);prev=p
  for i in range(12):
   a=i*2.4;m.orb((math.sin(a)*.62,.4+(i%5)*.3,-.4),(.13,.07,.05),RED,3,8)
 elif n==25:
  for y in [.78,2.78]:
   m.ring((0,y,0),.7,.7,.03,SNOW,'y')
   for i in range(12):a=i*PI/6;m.box((math.cos(a)*.66,y,math.sin(a)*.66),(.1,.12,.1),IRON)
  m.spike((0,.9,0),(0,1.3,0),.35,SNOW);m.beam((0,1.1,0),(0,2.45,0),.025,SNOW)
 elif n==26:
  for s in [-1,1]:
   for z in [-.4,0,.4]:m.box((s*.76,1.4,z),(.03,.6,.14),NAVY);m.beam((s*.78,1.1,z),(s*.78,1.7,z),.015,GOLD)
   for i in range(3):m.beam((s*.5,2,-.5+i*.5),(s*1,1,-.5+i*.5),.055,SNOW)
  for i in range(8):
   a=i*PI/4;m.beam((0,1.7,-.93),(math.cos(a)*.36,1.7+math.sin(a)*.36,-.93),.018,GOLD)
  m.tower(0,2.8,.1,1,.15)
 elif n==27:
  for s in [-1,1]:
   for i in range(8):
    p=(s*(.25+i*.1),2.95+i*.18,-.8);m.spike(p,add(p,(s*.16,.3,0)),.045,IRON)
   for i in range(9):m.spike((s*.25,1.7-i*.05,-.6),(s*.4,1.3-i*.08,-.5),.065,SNOW)
 elif n==28:
  m.bone('cradle_detail',(0,1.5,0),'hover')
  for s in [-1,1]:
   frame((s*.85,1.7,-.51),.1,.8)
   m.chain((s*.7,1.25,.4),(s*.8,.25,.5),9);m.crown((s*.85,2.2,0),.25,5)
  for i in range(10):
   a=i*2.3;m.orb((math.sin(a)*.45,1.4+(i%4)*.13,math.cos(a)*.4),(.05,.04,.05),LIGHT,2,5)
  m.root()
 elif n==29:
  for i in range(14):
   a=i*PI/7;m.spike((math.cos(a)*.48,.8,math.sin(a)*.48),(math.cos(a)*.85,.05,math.sin(a)*.85),.09,GLASS)
  for s in [-1,1]:
   for i in range(4):m.beam((s*.8,1.5,0),(s*.8+(i-1.5)*.04,1.28,-.05),.018,SNOW)
  m.spike((0,3,0),(0,3.4,0),.12,ICE)
 elif n==30:
  for s in [-1,1]:
   m.beam((s*.65,.65,.3),(s*.65,3.2,.3),.1,IRON)
   for i in range(6):m.tower(s*(.45+(i%2)*.35),2.65+i*.08,.25+(i//2)*.2,.4+(i%3)*.22,.13)
   for j in range(2):
    for i in range(4):m.spike((s*1.4,1.2-j*.65,-.3),(s*(1.4+(i-1.5)*.09),.8-j*.6,-.4),.035,SNOW)
  for i in range(8):a=i*PI/4;m.spike((math.cos(a)*.8,.8,math.sin(a)*.5),(math.cos(a)*.9,.3,math.sin(a)*.6),.1,ICE)

def antlers(m,c,scale):
 for s in [-1,1]:
  prev=c
  for i in range(1,5):
   p=add(c,(s*i*.2*scale,i*.23*scale,(i%2)*.1));m.beam(prev,p,.055*scale,IRON,.04*scale)
   m.spike(p,add(p,(s*.2*scale,.3*scale,-.15)),.04*scale,IRON);prev=p

def atlas():
 rng=random.Random(31230);im=Image.new('RGBA',(1024,256));px=im.load()
 for y in range(256):
  for x in range(1024):
   k=x//128;c=COLORS[k];noise=rng.randint(-8,8)
   if k in (ICE,GLASS):noise+=18 if (x*3+y)%53<2 else 0
   if k in (IRON,GOLD):noise+=12 if y%32<2 or x%32<2 else 0
   if k in (NAVY,RED):noise+=6 if (x+y)%5==0 else 0
   px[x,y]=tuple(max(0,min(255,v+noise)) for v in c)+(65 if k==GLASS else 255,)
 glow=Image.new('RGBA',im.size,(0,0,0,0));glow.paste(im.crop((768,0,896,256)),(768,0))
 path=ASSETS/'textures/entity';im.save(path/'frost_materials.png');glow.save(path/'frost_materials_glow.png')

def project(p):return (p[0]*.86-p[2]*.5,-p[1]+p[0]*.18+p[2]*.3)
def preview(mesh):
 im=Image.new('RGB',(540,660),(236,233,225));draw=ImageDraw.Draw(im)
 projected=[project(p) for f,_ in mesh.faces for p in f];xmin=min(p[0] for p in projected);xmax=max(p[0] for p in projected);ymin=min(p[1] for p in projected);ymax=max(p[1] for p in projected);scale=min(460/(xmax-xmin),535/(ymax-ymin))
 for f,mat in sorted(mesh.faces,key=lambda fm:sum(p[2]*.86+p[0]*.5 for p in fm[0])/len(fm[0]),reverse=True):
  n=unit(cross(sub(f[1],f[0]),sub(f[2],f[0])));light=.67+.33*max(0,sum(a*b for a,b in zip(n,(-.4,.7,-.6))));color=tuple(int(c*light) for c in COLORS[mat])
  pts=[((project(p)[0]-(xmin+xmax)/2)*scale+270,(project(p)[1]-ymin)*scale+25) for p in f]
  if mat==GLASS:
   layer=Image.new('RGBA',im.size);ld=ImageDraw.Draw(layer);ld.polygon(pts,fill=(*color,55),outline=(*color,100));im.paste(layer,(0,0),layer);draw=ImageDraw.Draw(im)
  else:draw.polygon(pts,fill=color,outline=tuple(max(0,v-20) for v in color))
 try:font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',20)
 except OSError:font=ImageFont.load_default()
 draw.text((20,610),f'{mesh.n:02}  {ENGLISH[mesh.n-1]}',font=font,fill=(30,45,55))
 im.save(OUT/f'{mesh.n:02}.png');return im

if __name__=='__main__':
 atlas();reviews=[];manifest=[]
 for n in range(1,31):
  m=build(n);allpoints=[p for face,_ in m.faces for p in face]
  lo=[min(p[i] for p in allpoints) for i in range(3)];hi=[max(p[i] for p in allpoints) for i in range(3)]
  for part in m.parts:
   part.pop('_key',None)
   part['vertices']=[[round(v,6) for v in vert] for vert in part['vertices']]
  (ASSETS/f'bosses/frost_{n:02}.json').write_text(json.dumps({'parts':m.parts},separators=(',',':')),encoding='utf-8')
  manifest.append(dict(floor=n,name=NAMES[n-1],model=f'frost_{n:02}',bounds=[lo,hi],parts=len(m.parts),quads=len(m.faces)))
  reviews.append(preview(m))
 for i in range(3):
  sheet=Image.new('RGB',(2700,1320),(236,233,225))
  for j,im in enumerate(reviews[i*10:i*10+10]):sheet.paste(im,((j%5)*540,(j//5)*660))
  sheet.save(OUT/f'sheet-{i+1}.png')
 (ASSETS/'bosses/frost_catalog.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
 for locale in ['ja_jp','en_us']:
  path=ASSETS/f'lang/{locale}.json';data=json.loads(path.read_text(encoding='utf-8-sig'))
  for i in range(30):data[f'entity.neonward.sky_guardian_{i+1}']=NAMES[i] if locale=='ja_jp' else ENGLISH[i].title()
  path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print('Generated 30 dedicated rigs, material/glow atlases, 30 previews and 3 review sheets.')
