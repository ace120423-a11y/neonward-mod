"""Read-only NBT survey for the separate western Edo district. No writes to saves."""
from pathlib import Path
from collections import Counter
import io, zlib, functools, nbtlib

REGION=Path.home()/'Documents/NeonWardServer/world/dimensions/minecraft/overworld/region'
@functools.lru_cache(None)
def region(rx,rz):
    p=REGION/f'r.{rx}.{rz}.mca'
    return p.read_bytes() if p.exists() else b''
@functools.lru_cache(None)
def chunk(cx,cz):
    raw=region(cx//32,cz//32)
    if not raw:return {}
    index=cx%32+32*(cz%32);offset=(int.from_bytes(raw[4*index:4*index+4],'big')>>8)*4096
    if not offset:return {}
    length=int.from_bytes(raw[offset:offset+4],'big')
    if raw[offset+4]!=2:raise ValueError('Unsupported chunk compression')
    return nbtlib.File.parse(io.BytesIO(zlib.decompress(raw[offset+5:offset+length+4])))
def states(cx,cz):
    for section in chunk(cx,cz).get('sections',[]):
        sy=int(section['Y'])*16
        if sy>128 or sy+15<64:continue
        state=section.get('block_states',{});palette=state.get('palette',[]);data=state.get('data',[])
        if not palette:continue
        bits=max(4,(len(palette)-1).bit_length());per=64//bits;mask=(1<<bits)-1
        for n in range(4096):
            y=sy+n//256
            if not 64<=y<=128:continue
            i=((int(data[n//per])&((1<<64)-1))>>((n%per)*bits))&mask if len(data) else 0
            yield cx*16+n%16,y,cz*16+(n//16)%16,str(palette[i]['Name'])
def main():
    import sys
    if '--selected' in sys.argv:
        counts=Counter();gate={};objects=[]
        for cx in range(-14,1):
            for cz in range(16,30):
                for be in chunk(cx,cz).get('block_entities',[]):
                    if 63<=int(be.get('y',0))<=110:objects.append((str(be.get('id')),int(be['x']),int(be['y']),int(be['z'])))
                for x,y,z,name in states(cx,cz):
                    if -216<=x<=-64 and 272<=z<=464 and y>=65 and name!='minecraft:air':counts[name]+=1
                    if -64<=x<=4 and 363<=z<=373 and y>=65 and name!='minecraft:air':
                        b=gate.setdefault(name,[x,y,z,x,y,z]);b[:]=[min(b[0],x),min(b[1],y),min(b[2],z),max(b[3],x),max(b[4],y),max(b[5],z)]
        print('SELECTED ABOVE',counts);print('GATE BOUNDS',gate);print('OBJECTS',objects);return
    inside=Counter();objects=[];gates={z:Counter() for z in (352,384,416,448,456,480,512,544)};ground=Counter();bounds={}
    for cx in range(-24,2):
        for cz in range(20,38):
            for be in chunk(cx,cz).get('block_entities',[]):
                if -384<=int(be.get('x',0))<=16 and 320<=int(be.get('z',0))<=592:objects.append((str(be.get('id')),int(be['x']),int(be['y']),int(be['z'])))
            for x,y,z,name in states(cx,cz):
                if -360<=x<=-80 and 344<=z<=568:
                    if y==64:ground[name]+=1
                    elif name!='minecraft:air':
                        inside[name]+=1
                        b=bounds.setdefault(name,[x,y,z,x,y,z]);b[:]=[min(b[0],x),min(b[1],y),min(b[2],z),max(b[3],x),max(b[4],y),max(b[5],z)]
                for center,count in gates.items():
                    if -40<=x<=8 and center-5<=z<=center+5 and 65<=y<=78 and name!='minecraft:air':count[name]+=1
    print('GROUND',ground);print('TOWN_ABOVE_GROUND',inside);print('BOUNDS',bounds);print('BLOCK_ENTITIES',objects[:100],len(objects));print('GATE_OPTIONS',gates)
if __name__=='__main__':main()
