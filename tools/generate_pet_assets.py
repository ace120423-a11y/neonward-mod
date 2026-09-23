"""Generate only production pet model resources; no Minecraft launch or world access.

Native hierarchical models preserve the showroom's corn snake, black crow and
white/cyan robot silhouettes. Dog and tabby cat are articulated quadrupeds.
"""
import json
import struct
import zlib
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / 'resources/assets/neonward/pets'

def model(kind):
    parts = []
    def add(name, p, size=None, color=0xffffff, parent=None, anim=None):
        part = dict(name=name, p=p, color=color)
        if size is not None: part['size'] = size
        if parent is not None: part['parent'] = parent
        if anim is not None: part['anim'] = anim
        parts.append(part)
    if kind in ('dog', 'cat'):
        cat = kind == 'cat'
        fur, cream = (0xc58b45,0xf4d4a0) if cat else (0xe3ddd0,0x8d8e95)
        add('body',[0,.43,0],[.32 if cat else .4,.3,.65],fur)
        add('chest',[0,.48,-.26],[.34,.35,.18],cream)
        add('head',[0,.65,-.37],[.32,.3,.30],fur,anim='head')
        add('muzzle',[0,-.065,-.18],[.22,.12,.13],cream,'head')
        add('nose',[0,-.04,-.255],[.06,.045,.025],0x282632,'head')
        for side in (-1,1):
            add('ear'+str(side),[side*.12,.18,.015],[.09,.17,.10],fur,'head')
            add('ear_inner'+str(side),[side*.12,.18,-.04],[.05,.11,.012],0xdb9e9d,'head')
            add('eye'+str(side),[side*.105,.035,-.154],[.045,.05,.015],0x61cbae if cat else 0x30272a,'head')
        for i,(x,z) in enumerate(((-.13,-.23),(.13,-.23),(-.13,.23),(.13,.23))):
            add('hip'+str(i),[x,.34,z],anim='leg'+str((i+i//2)%2))
            add('leg'+str(i),[0,-.14,0],[.09,.28,.11],fur,'hip'+str(i))
            add('paw'+str(i),[0,-.28,-.035],[.12,.07,.17],cream,'hip'+str(i))
        add('tail',[0,.46,.34],[.09,.10,.38],cream,anim='tail')
        if cat:
            for i in range(4):add('stripe'+str(i),[0,.585,-.18+i*.12],[.33,.015,.045],0x85562d)
    elif kind == 'snake':
        add('head',[0,.15,-.18],[.28,.18,.29],0xce7735)
        add('jaw',[0,.08,-.21],[.23,.045,.23],0xf1d9ad)
        for side in (-1,1):add('eye'+str(side),[side*.14,.19,-.25],[.025,.045,.045],0x201e23)
        add('tongue',[0,.11,-.37],[.025,.018,.16],0xc54258)
        for i in range(12):
            width=.21*(1-i/15)
            add('link'+str(i),[0,.13 if i==0 else 0,.04 if i==0 else .11],[width,.12*(1-i/17),.15],0x9e4930 if i%3==0 else 0xce7735,None if i==0 else 'link'+str(i-1),'snake'+str(i))
    elif kind == 'crow':
        add('bird',[0,0,0],anim='bird')
        add('body',[0,.34,.02],[.28,.30,.45],0x222531,'bird')
        add('head',[0,.55,-.22],[.27,.26,.28],0x191c27,'bird', 'head')
        add('beak',[0,-.035,-.22],[.12,.09,.21],0x404450,'head')
        for side in (-1,1):
            add('eye'+str(side),[side*.139,.03,-.06],[.015,.035,.04],0xdde4ec,'head')
            add('wing'+str(side),[side*.13,.40,.02],parent='bird',anim='wing'+str(1 if side<0 else 0))
            for i in range(4):add('feather'+str(side)+'_'+str(i),[side*(.18+i*.025),0,-.12+i*.095],[.4-i*.04,.035,.08],0x252938 if i%2==0 else 0x343948,'wing'+str(side))
            add('leg'+str(side),[side*.08,.12,-.035],[.025,.2,.03],0x626879,'bird')
            add('claw'+str(side),[side*.08,.03,-.09],[.08,.025,.16],0x626879,'bird')
        add('tail',[0,.31,.32],[.2,.05,.28],0x292d3c,'bird','tail')
    else:
        add('body',[0,.52,0],[.54,.4,.42],0xe8f0ef)
        add('face',[0,.55,-.22],[.43,.21,.025],0x243142)
        for side in (-1,1):
            add('eye'+str(side),[side*.12,.57,-.238],[.085,.06,.018],0x42edee)
            add('panel'+str(side),[side*.28,.5,0],[.03,.26,.3],0x31bcbf)
            add('hip'+str(side),[side*.16,.34,0],anim='leg'+str(1 if side<0 else 0))
            add('leg'+str(side),[0,-.11,0],[.12,.22,.15],0x5e6c7a,'hip'+str(side))
            add('foot'+str(side),[0,-.26,-.04],[.21,.1,.3],0xd7e3e8,'hip'+str(side))
            add('arm'+str(side),[side*.35,.43,0],[.1,.22,.13],0xd7e3e8)
        add('antenna',[0,.82,0],[.04,.2,.04],0x68758c,anim='antenna')
        add('beacon',[0,.12,0],[.11,.07,.11],0x42edee,'antenna')
    return dict(parts=parts)

def generate():
    OUT.mkdir(parents=True,exist_ok=True)
    for kind in ('dog','cat','snake','crow','robot'):
        (OUT / (kind+'.json')).write_text(json.dumps(model(kind),indent=2)+'\n',encoding='utf-8')
    def chunk(name,data):return struct.pack('>I',len(data))+name+data+struct.pack('>I',zlib.crc32(name+data)&0xffffffff)
    png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',1,1,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b'\0\xff\xff\xff\xff'))+chunk(b'IEND',b'')
    (OUT/'white.png').write_bytes(png)
    print('Generated five native pet models and white texture in',OUT)

if __name__=='__main__':generate()
