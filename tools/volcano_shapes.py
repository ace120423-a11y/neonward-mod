"""Thirty separately composed silhouettes. Only geometric primitives are shared."""
import math
from build_frost_bosses import Mesh,add,sub
PI=math.pi
ROCK,ASH,IRON,BRONZE,BLACK,RED,HOT,GOLD=range(8)
def eye(m,p,s=.08):m.orb(p,(s,s*.65,s*.45),HOT,3,6)
def limb(m,name,a,k,b,r=.14,mat=ROCK,anim='leg0',claws=False):
 m.bone(name,a,anim);m.beam(a,k,r,mat,r*.85,6);m.orb(k,(r*1.2,r*1.2,r*1.2),BRONZE,3,6);m.beam(k,b,r*.85,mat,r*.6,6)
 if claws:
  for i in range(3):m.spike(add(b,((i-1)*r*.6,0,0)),add(b,((i-1)*r*.8,-.05,-r*2)),r*.24,GOLD)
 m.root()
def fissure(m,a,b,r=.028):m.beam(a,b,r,HOT,r,4)
def horn(m,points,r=.15,mat=BRONZE):
 for i in range(len(points)-1):m.beam(points[i],points[i+1],r*(1-i/len(points)),mat,max(.008,r*(1-(i+1)/(len(points)-1))),6)
def animal_leg(m,name,x,z,y,r=.18):limb(m,name,(x,y,z),(x*.95,y*.48,z+.22),(x*.96,.15,z-.12),r,ROCK,'leg'+str(int(z>0)^int(x>0)),True)
def muzzle(m,c,w=.45,length=.7,mat=ROCK):
 m.orb(c,(w,w*.75,w),mat,4,8);m.box(add(c,(0,-.12,-length*.5)),(w*1.3,w*.5,length),mat)
 for s in [-1,1]:eye(m,add(c,(s*w*.7,.12,-w*.65)),.09)
 m.bone('jaw'+str(len(m.parts)),add(c,(0,-.12,.1)),'jaw');m.box(add(c,(0,-.3,-length*.5)),(w*1.2,.16,length),IRON)
 for s in [-1,1]:m.spike(add(c,(s*w*.45,-.18,-length*.85)),add(c,(s*w*.45,.04,-length*.85)),.075,GOLD)
 m.root()
def wing(m,name,a,b,c,mat=RED,side=1):
 m.bone(name,a,'wing'+str(side));m.beam(a,b,.08,BRONZE,.05,6);m.beam(b,c,.05,IRON,.018,6)
 for i in range(4):
  d=add(b,tuple((c[j]-b[j])*i/3 for j in range(3)));m.beam(a,d,.025,IRON,.018,4)
  if i<3:
   e=add(b,tuple((c[j]-b[j])*(i+1)/3 for j in range(3)));m.poly([a,d,e],mat)
 m.root()
def plate(m,c,s,mat=ROCK):m.orb(c,s,mat,3,6)
def build(n):
 m=Mesh(n)
 if n==1: # Beetle: low split elytra, broad head, giant curved mandibles, six legs.
  for s in [-1,1]:
   plate(m,(s*.47,.92,.35),(.62,.57,1.15));fissure(m,(s*.07,1.3,-.35),(s*.07,1.25,1.25))
   for i in range(3):limb(m,f'leg{s}_{i}',(s*.65,.75,(i-1)*.7),(s*1.15,.65,(i-1)*.9),(s*1.45,.08,(i-1)*1.1),.11,BLACK,'leg'+str(i%2))
   m.bone('mandible'+str(s),(s*.4,.7,-.9),'hand'+str(s));horn(m,[(s*.4,.7,-.9),(s*1.0,.85,-1.4),(s*1.05,1.4,-2.0),(s*.35,1.6,-2.3)],.24,HOT);m.root()
  plate(m,(0,.65,-.8),(.8,.35,.5));eye(m,(-.55,.84,-1.08));eye(m,(.55,.84,-1.08))
 elif n==2: # Scorpion: flat segments, two wide articulated claws, arched tail.
  for i in range(4):plate(m,(0,.65,(i-1)*.45),(.63,.3,.32),IRON)
  for s in [-1,1]:
   for i in range(4):limb(m,f'leg{s}{i}',(s*.5,.6,(i-1.5)*.4),(s*.95,.7,(i-1.5)*.65),(s*1.3,.07,(i-1.5)*.8),.07,BLACK,'leg'+str(i%2))
   m.bone('claw'+str(s),(s*.5,.65,-.8),'hand'+str(s));horn(m,[(s*.5,.65,-.8),(s*1.2,.8,-1.1),(s*1.6,.65,-1.8)],.16,IRON);plate(m,(s*1.6,.65,-1.8),(.38,.24,.5),BRONZE)
   for j in [-1,1]:m.spike((s*1.6+j*.22,.65,-2),(s*1.6+j*.07,.7,-2.65),.16,BRONZE)
   m.root()
  prev=(0,.65,1.2)
  for i in range(8):p=(0,.8+math.sin(i/8*PI*.8)*2.3,1.3+math.cos(i/8*PI)*.9);m.bone('tail'+str(i),prev,'tail'+str(i));m.beam(prev,p,.22,IRON,.19,6);m.root();prev=p
  m.spike(prev,add(prev,(0,-.6,-.45)),.28,HOT)
  for x in [-.2,0,.2]:eye(m,(x,.9,-.55),.055)
 elif n==3: # Salamander: elongated low body and finned tail, six splayed feet.
  for i in range(4):plate(m,(0,.65,i*.55),(.72-i*.09,.38,.55),RED)
  for s in [-1,1]:
   for i in range(3):limb(m,f'leg{s}{i}',(s*.5,.55,i*.55),(s*.95,.35,i*.55-.2),(s*1.1,.1,i*.55-.5),.12,ROCK,'leg'+str(i%2),True)
  muzzle(m,(0,.65,-.6),.6,.9)
  for i in range(9):p=(math.sin(i*.3)*.5,.5,1.8+i*.35);m.bone('tail'+str(i),p,'tail'+str(i));plate(m,p,(max(.08,.45-i*.045),.25,.3),RED);m.spike(p,add(p,(0,.7-i*.04,.15)),.17,HOT);m.root()
 elif n==4: # Wolf: high narrow chest, low haunch, digitigrade legs, mane and long muzzle.
  plate(m,(0,1.5,-.4),(.5,.85,.65));plate(m,(0,1.3,.65),(.47,.55,.9));plate(m,(0,1.9,-.9),(.4,.55,.45),ASH)
  for s in [-1,1]:animal_leg(m,'leg'+str(s)+'front',s*.42,-.55,1.45,.16);animal_leg(m,'leg'+str(s)+'rear',s*.42,1.05,1.35,.2)
  muzzle(m,(0,2.15,-1.05),.4,.85,ASH)
  for s in [-1,1]:m.spike((s*.23,2.4,-.9),(s*.35,2.95,-.75),.16,ASH)
  for i in range(16):a=i*PI/8;m.spike((math.cos(a)*.35,1.7+math.sin(a)*.35,-.65),(math.cos(a)*.7,1.7+math.sin(a)*.8,-.3),.13,ASH)
  horn(m,[(0,1.4,1.1),(0,1.6,1.8),(0,1.2,2.4),(0,.8,2.65)],.26,ASH)
 elif n==5: # Boar: huge round barrel, very short legs, heavy protruding tusks.
  plate(m,(0,1.15,.25),(1,.9,1.5));plate(m,(0,1.3,-1.15),(.7,.7,.6));m.box((0,1.05,-1.8),(.9,.48,.45),IRON)
  for s in [-1,1]:
   for z in [-.6,1.1]:animal_leg(m,'leg'+str(s)+str(z),s*.65,z,.9,.29)
   horn(m,[(s*.45,.95,-1.5),(s*.9,.9,-1.9),(s*1.05,1.5,-2.15),(s*.8,2.1,-2.05)],.23,GOLD);eye(m,(s*.55,1.55,-1.55))
  for i in range(7):m.spike((0,1.8,-.7+i*.3),(0,2.5,-.6+i*.3),.17,BLACK)
 elif n==6: # Hermit crab: offset spiral vent shell and unequal claws, stalk eyes.
  plate(m,(0,.6,-.2),(.9,.3,.8),RED)
  for s in [-1,1]:
   for i in range(4):limb(m,f'leg{s}{i}',(s*.6,.55,(i-1)*.4),(s*(1.15+i*.04),.6,(i-1)*.55),(s*1.5,.08,(i-1)*.7),.1,RED,'leg'+str(i%2))
   m.beam((s*.3,.7,-.75),(s*.4,1.3,-1),.055,IRON);eye(m,(s*.4,1.3,-1.03),.11)
   m.bone('hand'+str(s),(s*.6,.6,-.7),'hand'+str(s));r=.5 if s==-1 else .3;plate(m,(s*1.1,.6,-1.3),(r,r*.6,r),BRONZE)
   for j in [-1,1]:m.spike((s*1.1+j*r*.6,.6,-1.4),(s*1.1+j*.07,.6,-2),r*.38,BRONZE)
   m.root()
  for i in range(8):a=i*.8;r=.85-i*.08;plate(m,(math.cos(a)*r,.95+i*.2,.5+math.sin(a)*r),(r*.75+.1,.35,r*.75+.1))
  m.ring((.35,2.5,.6),.23,.23,.1,BLACK,'y',10);m.box((.35,2.43,.6),(.3,.07,.3),HOT)
 elif n==7: # Bat: tiny hanging body, enormous ears, broad transparent-looking membranes.
  plate(m,(0,2,0),(.35,.75,.4),ASH);plate(m,(0,2.7,-.15),(.4,.35,.35))
  for s in [-1,1]:
   m.spike((s*.23,2.85,0),(s*.55,4,0),.22,ASH);eye(m,(s*.2,2.7,-.44));m.spike((s*.1,2.5,-.4),(s*.1,2.2,-.43),.07,GOLD)
   wing(m,'wing'+str(s),(s*.25,2.4,0),(s*2.7,3.3,.1),(s*2.0,1.3,1.1),RED,s);m.beam((s*.15,1.6,0),(s*.25,1.1,.1),.055,IRON)
 elif n==8: # Cobra: coiled ground-level body, continuous raised neck, broad solid hood.
  for i in range(20):a=i*.38;r=1.1-i*.025;p=(math.cos(a)*r,.32,math.sin(a)*r+.45);m.bone('tail'+str(i),p,'tail'+str(i));plate(m,p,(.32,.28,.32),BRONZE);m.root()
  for i in range(7):p=(0,.4+i*.36,-.3-i*.05);plate(m,p,(.3,.32,.32),BRONZE)
  for s in [-1,1]:
   for i in range(5):p=(s*(.35+math.sin(i*PI/5)*.55),1.15+i*.35,-.45);plate(m,p,(.35,.3,.16),GOLD);m.box(add(p,(0,0,-.15)),(.16,.15,.02),BLACK)
  muzzle(m,(0,2.9,-.6),.34,.5,BRONZE)
 elif n==9: # Mountain ram: high compact shoulders, split hooves, tight spiral horns.
  plate(m,(0,1.3,.2),(.65,.7,1));plate(m,(0,1.8,-.7),(.5,.6,.45));muzzle(m,(0,2.1,-1),.36,.55)
  for s in [-1,1]:
   for z in [-.55,.85]:animal_leg(m,'leg'+str(s)+str(z),s*.45,z,1.2,.19)
   points=[(s*(.3+.18*i),2.3+math.sin(i*.7)*.45,-.8+math.cos(i*.7)*.35) for i in range(7)];horn(m,points,.24,BRONZE)
 elif n==10: # Turtle: broad flat shell, low stubby flippers and a crater instead of a head tower.
  plate(m,(0,.8,.2),(1.8,.8,2));m.beam((0,.65,-1.2),(0,.6,-2.1),.26,ROCK);muzzle(m,(0,.6,-2.1),.37,.5)
  for s in [-1,1]:
   for z in [-.9,1.2]:m.bone('leg'+str(s)+str(z),(s*1.1,.55,z),'leg'+str(int(z>0)));plate(m,(s*1.5,.27,z),(.55,.25,.55));m.root()
  for i in range(6):m.ring((0,1+i*.26,.35),1.1-i*.13,1.1-i*.13,.23,ROCK,'y',8)
  m.box((0,2.3,.35),(.4,.12,.4),HOT)
 elif n==11: # Lance knight: thin articulated armor, pointed helmet, no monster face.
  for y,w in [(1.6,.45),(2,.6),(2.5,.7),(2.9,.8)]:plate(m,(0,y,0),(w,.3,.35),IRON)
  for s in [-1,1]:limb(m,'leg'+str(s),(s*.3,1.6,0),(s*.35,.9,.1),(s*.35,.12,-.1),.18,IRON,'leg'+str(int(s>0)),True);limb(m,'hand'+str(s),(s*.65,2.8,0),(s*.9,2.2,0),(s*.9,1.7,-.3),.16,IRON,'hand'+str(s))
  plate(m,(0,3.4,0),(.35,.5,.3),IRON);m.box((0,3.4,-.31),(.4,.07,.06),HOT);m.spike((0,3.75,0),(0,4.4,0),.2,BRONZE)
  m.bone('spear',(-.65,2.8,0),'hand-1');m.beam((-1,0,-.3),(-1,4.4,-.3),.055,BRONZE);m.spike((-1,4.4,-.3),(-1,5.3,-.3),.2,HOT);m.root()
 elif n==12: # Djinn: no legs or solid torso; floating mask, smoke rings, detached fists.
  for i in range(7):p=(math.sin(i)*.2,.35+i*.4,0);m.bone('smoke'+str(i),p,'hover');m.orb(p,(.16+i*.06,.3,.16+i*.06),ASH,4,6);m.root()
  m.box((0,3,0),(.7,.95,.22),BRONZE);m.box((0,3,-.13),(.15,.6,.08),HOT)
  for s in [-1,1]:m.box((s*.2,3.2,-.14),(.12,.2,.07),BLACK);m.bone('hand'+str(s),(s*1.1,2.2,0),'hand'+str(s));plate(m,(s*1.1,2.2,0),(.4,.5,.35),BRONZE);m.root()
 elif n==13: # Furnace: squat hollow firebox, heavy piston limbs, four smokestacks.
  m.box((0,1.8,.1),(2,2,.9),IRON);m.box((0,1.9,-.5),(1.45,1.4,.12),HOT)
  for i in range(7):m.box(((i-3)*.21,1.9,-.6),(.07,1.5,.12),BLACK)
  for s in [-1,1]:limb(m,'leg'+str(s),(s*.65,.9,0),(s*.7,.45,.1),(s*.7,.15,-.2),.35,IRON,'leg'+str(int(s>0)));limb(m,'hand'+str(s),(s*1.1,2.7,0),(s*1.45,1.8,0),(s*1.6,1.2,0),.32,IRON,'hand'+str(s))
  for i in range(4):m.beam(((i-1.5)*.42,2.5,.3),((i-1.5)*.42,3.7+(i%2)*.4,.3),.15,BLACK)
 elif n==14: # Spider: globular abdomen, tiny clustered eyes, eight long pointed legs.
  plate(m,(0,1.25,.65),(.85,.8,1),BLACK);plate(m,(0,1,-.5),(.5,.38,.5),BLACK)
  for s in [-1,1]:
   for i in range(4):limb(m,f'leg{s}{i}',(s*.45,1,(i-1.5)*.3),(s*(1.3+i*.1),1.8,(i-1.5)*.65),(s*2.0,.05,(i-1.5)*1.0),.11,BLACK,'leg'+str(i%2));eye(m,(s*(.12+i*.08),1.1,-.85),.05)
  for i in range(5):m.spike(((i-2)*.24,1.65,.6),((i-2)*.4,2.7,.5),.14,BLACK)
 elif n==15: # Centaur: four-legged horse rear, raised human forge front, huge hammer.
  plate(m,(0,1.1,.7),(.7,.6,1.35),IRON)
  for s in [-1,1]:
   for z in [-.2,1.6]:animal_leg(m,'leg'+str(s)+str(z),s*.5,z,1.1,.19)
  m.box((0,2,-.5),(1,1.5,.65),IRON);m.box((0,2.75,-.5),(1.5,.3,.65),BRONZE);m.box((0,3.15,-.5),(.5,.55,.4),BLACK);m.box((0,3.2,-.73),(.35,.1,.04),HOT)
  for s in [-1,1]:limb(m,'hand'+str(s),(s*.6,2.6,-.5),(s*1,2.2,-.5),(s*1.1,2,-.8),.2,IRON,'hand'+str(s))
  m.bone('hammer',(-.6,2.6,-.5),'hand-1');m.beam((-1.1,1.1,-.8),(-1.1,3.5,-.8),.08,BRONZE);m.box((-1.1,3.5,-.8),(1.3,.65,.65),IRON);m.root()
 elif n==16: # Actual hollow bell and hanging clapper, chains instead of humanoid limbs.
  m.bone('bell',(0,2.6,0),'hover')
  for i in range(8):m.ring((0,1.1+i*.22,0),1.05-i*.085,1.05-i*.085,.12,BRONZE,'y',12)
  m.beam((0,2.5,0),(0,.8,0),.09,IRON);plate(m,(0,.8,0),(.23,.32,.23),HOT);m.root()
  for s in [-1,1]:m.bone('hand'+str(s),(s*.2,2.9,0),'hand'+str(s));m.chain((s*.2,2.9,0),(s*1.6,.6,0),12);m.box((s*1.6,.6,0),(.4,.5,.4),IRON);m.root()
 elif n==17: # Stilt priest: long robe, narrow mask, brazier staff.
  m.cloth((0,3.5,0),(0,1,.2),1.5,ASH);m.spike((0,3.6,0),(0,4.3,0),.45,BLACK);m.box((0,3.55,-.26),(.18,.55,.1),BRONZE)
  for s in [-1,1]:limb(m,'leg'+str(s),(s*.25,1.8,0),(s*.4,.8,0),(s*.55,.08,0),.05,BRONZE,'leg'+str(int(s>0)));limb(m,'hand'+str(s),(s*.4,3.3,0),(s*.8,2.8,0),(s*.95,2.7,-.1),.1,ASH,'hand'+str(s))
  m.beam((1,.1,-.1),(1,4,-.1),.06,BRONZE);m.ring((1,3.9,-.1),.3,.3,.09,BRONZE,'y');m.spike((1,3.8,-.1),(1,4.6,-.1),.23,HOT)
 elif n==18: # Executioner: two toothed wheels, tall engine, asymmetric saw cleavers.
  m.box((0,1.9,0),(.75,1.6,.75),IRON);m.box((0,2.6,-.4),(.4,.12,.08),HOT)
  for s in [-1,1]:
   p=(s*.65,.7,0);m.bone('wheel'+str(s),p,'wheel');m.ring(p,.55,.55,.15,IRON)
   for i in range(14):a=i*PI/7;m.spike(add(p,(math.cos(a)*.55,math.sin(a)*.55,0)),add(p,(math.cos(a)*.8,math.sin(a)*.8,0)),.09,BRONZE)
   m.root();limb(m,'hand'+str(s),(s*.5,2.5,0),(s*1.1,2,0),(s*1.6,.8,0),.12,IRON,'hand'+str(s));m.bone('blade'+str(s),(s*.5,2.5,0),'hand'+str(s));m.box((s*1.5,1.1,-.12),(.65,1.4,.12),BRONZE)
   for i in range(6):m.spike((s*1.8,.5+i*.23,-.12),(s*2,.4+i*.23,-.12),.1,IRON)
   m.root()
 elif n==19: # Bull gatekeeper: huge shoulders, bull snout/horns, monumental left shield.
  plate(m,(0,2,0),(.95,1,.5));muzzle(m,(0,3.1,-.1),.48,.55)
  for s in [-1,1]:horn(m,[(s*.3,3.3,0),(s*.8,3.6,0),(s*1,4.1,-.2)],.22,BRONZE);animal_leg(m,'leg'+str(s),s*.5,0,1.5,.3);limb(m,'hand'+str(s),(s*.85,2.7,0),(s*1.1,2.1,0),(s*1.1,1.4,-.1),.25,ROCK,'hand'+str(s))
  m.box((1.35,1.8,-.5),(1.25,3.5,.38),BLACK)
  for x in [.9,1.35,1.8]:m.box((x,1.8,-.72),(.08,3.3,.08),BRONZE)
  m.box((1.35,2,-.78),(.45,.8,.04),HOT)
 elif n==20: # Four-arm temple automaton: stepped shrine head and separate elbowed arms.
  m.box((0,2.2,0),(1.4,1.9,.9),IRON)
  for i in range(4):m.box((0,3.15+i*.25,0),(.95-i*.19,.25,.7-i*.13),ROCK)
  m.box((0,3.3,-.4),(.18,.55,.1),HOT)
  for s in [-1,1]:
   animal_leg(m,'leg'+str(s),s*.45,0,1.3,.28)
   for j in range(2):limb(m,f'hand{s}{j}',(s*.7,2.2+j*.65,0),(s*1.35,1.8+j*.95,0),(s*1.9,2.3+j*1.1,-.1),.25,IRON,'hand'+str(s));m.box((s*1.9,2.3+j*1.1,-.1),(.5,.55,.5),BRONZE)
 elif n==21: # Skeletal dragon: long back and neck, articulated wing fingers, four claws.
  plate(m,(0,1.8,.5),(.65,.7,1.5),ASH);horn(m,[(0,2,-.7),(0,2.6,-1.2),(0,3,-1.7)],.28,ASH);muzzle(m,(0,3,-1.8),.4,.8,ASH)
  for s in [-1,1]:
   wing(m,'wing'+str(s),(s*.5,2.1,.25),(s*3.5,3.7,.9),(s*2.6,1.3,2.7),ASH,s)
   for z in [-.4,1.6]:limb(m,'leg'+str(s)+str(z),(s*.5,1.6,z),(s*.8,.8,z+.15),(s*.7,.2,z-.2),.13,ASH,'leg'+str(int(z>0)),True)
  for i in range(10):p=(math.sin(i*.3)*.4,1.5-i*.09,1.5+i*.35);m.bone('tail'+str(i),p,'tail'+str(i));plate(m,p,(.3-i*.023,.25-i*.015,.3),ASH);m.root()
 elif n==22: # Phoenix: feather fans and three long golden streamers, no bat membrane.
  plate(m,(0,2.3,0),(.4,.7,.45),ASH);plate(m,(0,3,-.15),(.25,.3,.25),ASH);m.spike((0,3,-.35),(0,2.9,-.9),.12,GOLD)
  for s in [-1,1]:
   eye(m,(s*.15,3.07,-.32),.05);m.bone('wing'+str(s),(s*.25,2.7,0),'wing'+str(s))
   for i in range(10):a=(s*.3,2.7,.05);b=(s*(1+i*.21),3.8-i*.15,.2+i*.12);m.beam(a,b,.085,ASH,.035,4);m.spike(b,add(b,(s*.4,-.6,.1)),.09,GOLD)
   m.root()
  for i in range(3):m.bone('tail'+str(i),((i-1)*.17,1.9,.2),'tail'+str(i));horn(m,[((i-1)*.17,1.9,.2),((i-1)*.45,1,.8),((i-1)*.6,.25,1.5),((i-1)*.8,.1,2.2)],.16,GOLD);m.root()
  for i in range(5):m.spike(((i-2)*.07,3.2,0),((i-2)*.15,3.85,.1),.065,GOLD)
 elif n==23: # Throwing giant: heavy rubble body, disproportionately large molten left arm.
  for y,w in [(1.8,.75),(2.5,1),(3.2,1.15)]:plate(m,(0,y,0),(w,.65,.65))
  m.box((0,3.75,-.05),(.75,.7,.6),ROCK);m.box((0,3.85,-.37),(.52,.09,.05),HOT)
  for s in [-1,1]:
   limb(m,'leg'+str(s),(s*.55,1.65,0),(s*.65,.85,.08),(s*.75,.25,-.3),.4,ROCK,'leg'+str(int(s>0)),True)
   a=(s*1,3.25,0);k=(s*1.55,2.3,0);b=(s*1.8,1.4,-.15);m.bone('hand_right' if s==-1 else 'hand_left',a,'hand'+str(s));r=.48 if s==-1 else .26;m.beam(a,k,r,RED if s==-1 else ROCK,r,6);m.beam(k,b,r,HOT if s==-1 else ROCK,r*.9,6);plate(m,b,(r*1.3,r,r),ROCK)
   for i in range(4):m.beam(add(b,((i-1.5)*r*.5,-.2,-.1)),add(b,((i-1.5)*r*.5,-.65,-.35)),r*.18,BLACK)
   m.root()
   if s==-1:
    c=add(b,(0,-.1,-.5));m.bone('held_rock',c,'heldrock');m.parts[-1]['parent']='hand_right';m.parts[-1]['p']=list(sub(c,a));plate(m,c,(.65,.65,.65));fissure(m,add(c,(-.4,0,-.5)),add(c,(.4,.2,-.5)),.08);m.root()
 elif n==24: # Monolith: broken diamond frame with empty gaps, core suspended centrally.
  m.bone('core',(0,2.4,0),'pulse');plate(m,(0,2.4,0),(.45,.8,.4),HOT);m.root()
  for i in range(8):a=i*PI/4;p=(math.cos(a)*1.25,2.4+math.sin(a)*1.6,0);m.bone('shard'+str(i),p,'hover');m.beam(add(p,(-.2,-.4,0)),add(p,(.2,.4,0)),.22,BLACK,.2,4);m.root()
 elif n==25: # Centipede: rising curved arch, many paired pointed legs, terminal mandibles.
  for i in range(18):
   t=i/17*PI*.9;p=(0,.6+math.sin(t)*2.5,i*.3-1.8);m.bone('tail'+str(i),p,'tail'+str(i));plate(m,p,(.55,.5,.38));fissure(m,add(p,(-.45,.3,-.2)),add(p,(.45,.3,-.2)),.035)
  # Each segment has its own leg silhouette; motion follows its body section.
   for s in [-1,1]:m.beam(add(p,(s*.4,0,0)),add(p,(s*.9,-.4,.1)),.065,BRONZE);m.spike(add(p,(s*.9,-.4,.1)),add(p,(s*1.1,-1,.2)),.06,BRONZE)
   m.spike(add(p,(0,.3,0)),add(p,(0,1,.1)),.16,BLACK);m.root()
  plate(m,(0,.7,-2),(.65,.5,.45))
  for s in [-1,1]:horn(m,[(s*.3,.7,-2.2),(s*.8,.5,-2.5),(s*.35,.4,-2.8)],.16,BRONZE);eye(m,(s*.3,.9,-2.4))
 elif n==26: # Moth: six separate broad diamond lobes with concentric eye spots.
  plate(m,(0,2.2,0),(.25,.8,.3),ASH)
  for s in [-1,1]:
   horn(m,[(s*.12,2.8,0),(s*.25,3.25,0),(s*.65,3.6,0)],.035,GOLD)
   for j in range(3):
    a=(s*.2,2.55-j*.25,0);b=(s*(2.5-j*.3),3.5-j*.95,.2);c=(s*(2.1-j*.2),2.3-j*.6,.8);wing(m,f'wing{s}{j}',a,b,c,BLACK,s)
    m.bone('eyeWing'+str(s)+str(j),a,'wing'+str(s));p=(s*(1.5-j*.1),2.8-j*.65,.43);m.ring(p,.27,.2,.07,GOLD);m.orb(p,(.12,.1,.025),RED,3,6);m.root()
 elif n==27: # Cerberus: three independent necks and long muzzles, massive front paws.
  plate(m,(0,1.35,.5),(.85,.8,1.2))
  for s in [-1,1]:
   for z in [-.4,1.2]:animal_leg(m,'leg'+str(s)+str(z),s*.6,z,1.3,.25)
  for i in range(3):c=((i-1)*.68,2.2+(i==1)*.45,-.85);m.beam(((i-1)*.35,1.45,-.3),c,.26,RED);muzzle(m,c,.36,.7)
  for s in [-1,1]:m.spike((s*.7,1.7,.3),(s*1.1,2.4,.65),.2,BLACK)
 elif n==28: # Emperor: faceless needle crown, long layered cloak, orbiting blade regalia.
  m.box((0,3,0),(.65,1.5,.5),BLACK)
  for i in range(12):a=i*PI/6;m.poly([(math.cos(a)*.3,3.5,math.sin(a)*.3),(math.cos(a)*1.15,.2,math.sin(a)*1.15),(math.cos(a+.45)*1.15,.2,math.sin(a+.45)*1.15),(math.cos(a+.45)*.3,3.5,math.sin(a+.45)*.3)],IRON if i%3==0 else BLACK)
  m.box((0,4,0),(.35,.85,.3),BLACK);m.box((0,4,-.17),(.045,.55,.04),HOT)
  for i in range(5):m.spike(((i-2)*.1,4.3,0),((i-2)*.15,5.2-abs(i-2)*.1,0),.075,BRONZE)
  for s in [-1,1]:limb(m,'hand'+str(s),(s*.4,3.5,0),(s*.8,3,0),(s*.9,2.5,-.2),.11,BRONZE,'hand'+str(s));m.bone('blade'+str(s),(s*1.7,2.5,0),'hover');m.spike((s*1.7,1,0),(s*1.7,4,0),.2,BRONZE);m.root()
 elif n==29: # Ancient wyrm: sinuous plated quadruped, long rising neck and branching antlers.
  for i in range(10):p=(math.sin(i*.5)*.5,1.0,1+i*.4);m.bone('tail'+str(i),p,'tail'+str(i));plate(m,p,(.7-i*.04,.6-i*.025,.4));m.spike(add(p,(0,.4,0)),add(p,(0,1,.1)),.14,BLACK);m.root()
  for s in [-1,1]:
   for z in [1,3]:animal_leg(m,'leg'+str(s)+str(z),s*.55,z,1,.18)
  horn(m,[(0,1.2,1),(0,2.1,.3),(0,3,-.1)],.4,ROCK);muzzle(m,(0,3,-.4),.5,.9)
  for s in [-1,1]:
   path=[(s*.3,3.2,-.1),(s*.6,3.65,.15),(s*1,4.1,.4),(s*1.4,4.5,.7)];horn(m,path,.12,BLACK)
   for p in path[1:]:m.spike(p,add(p,(s*.1,.55,-.35)),.09,BLACK)
 elif n==30: # Walking volcanic crown: four buttresses, six floating arms, open erupting crater.
  m.ring((0,3,0),1.35,1.35,.45,ROCK,'y',8);m.box((0,2.7,-1.1),(1.1,1.5,.4),HOT)
  for s in [-1,1]:
   for z in [-1,1]:limb(m,'leg'+str(s)+str(z),(s*.9,2.5,z*.9),(s*1.4,1.4,z*1.3),(s*2,.25,z*1.8),.42,ROCK,'leg'+str(int(z>0)));m.box((s*2,.2,z*1.8),(.85,.4,1),BLACK)
  for i in range(10):a=i*PI/5;p=(math.cos(a)*1.2,3,math.sin(a)*1.2);m.beam(p,add(p,(0,1.6+(i%3)*.4,0)),.23,BLACK);m.spike(add(p,(0,1.6,0)),add(p,(0,2.8+(i%3)*.35,0)),.25,BLACK)
  for i in range(6):s=-1 if i%2==0 else 1;p=(s*(2+i//2*.4),2+i//2*.7,0);m.bone('hand'+str(i),p,'hand'+str(s));m.beam(p,add(p,(s*.5,.7,0)),.23,ROCK);m.box(add(p,(s*.5,.8,-.1)),(.6,.55,.6),IRON);m.root()
  m.bone('core',(0,3.7,0),'pulse');plate(m,(0,3.7,0),(.7,.8,.7),HOT)
  for i in range(5):m.spike(((i-2)*.16,3.7,0),((i-2)*.25,5.8-abs(i-2)*.25,.1),.18,HOT)
  m.root()
 return m
