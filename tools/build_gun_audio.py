"""Original, deterministic synthesized gun foley; no sampled third-party audio.
Run with soundfile + numpy (or repository build/audio-tools).
"""
from pathlib import Path
import sys, json
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'build/audio-tools'))
import numpy as np
import soundfile as sf
RATE=32000
NAMES=['pulse','pistol','handcannon','compact','sniper','machinegun','rail','plasma','arc','cryo','crossbow','charge','impact','detonate']
out=ROOT/'resources/assets/neonward/sounds/guns'
out.mkdir(parents=True,exist_ok=True)
events={}
for i,name in enumerate(NAMES):
    duration=1.0 if name in ('cryo','charge') else [.38,.22,.7,.16,.95,.2,.8,.7,.45,1,.32,1,.16,.8][i]
    t=np.arange(int(RATE*duration))/RATE
    rng=np.random.default_rng(92700+i)
    noise=rng.normal(0,1,len(t))
    low=np.convolve(noise,np.ones(19)/19,mode='same')
    if name=='cryo':
        # Seamless one-second pressurized hiss, not repeated gunshots.
        v=(noise*.13+low*.6)*(1+.08*np.sin(2*np.pi*7*t))
        fade=240;v[:fade]=v[:fade]*np.arange(fade)/fade+v[-fade:]*np.arange(fade,0,-1)/fade
    elif name=='charge':
        v=.25*np.sin(2*np.pi*(180*t+1100*t*t))*(.2+.8*t)+noise*.015
    elif name in ('rail','plasma','arc','pulse'):
        f={'rail':1700,'plasma':160,'arc':950,'pulse':650}[name]
        v=(.5*np.sin(2*np.pi*(f*t-0.35*f*t*t/duration))+.25*np.sin(2*np.pi*f*1.51*t)+noise*.17)*np.exp(-t*(6 if name=='plasma' else 11))
        if name=='arc':v+=noise*.3*np.exp(-t*8)*(np.sin(t*160)>0)
        if name=='rail':v+=low*2*np.exp(-t*5)
    elif name=='crossbow':
        v=.45*np.sin(2*np.pi*145*t)*np.exp(-t*18)+noise*.3*np.exp(-t*120)
        v+=noise*.08*np.exp(-((t-.07)/.012)**2)
    else:
        bass={'handcannon':65,'sniper':48,'machinegun':115,'compact':200,'pistol':150,'impact':700,'detonate':38}.get(name,130)
        tail=5 if name in ('handcannon','sniper','detonate') else 28
        v=.55*np.sin(2*np.pi*(bass*t+20*(1-np.exp(-t*12))))*np.exp(-t*tail)+noise*.36*np.exp(-t*70)+low*np.exp(-t*tail)
        v+=noise*.08*np.exp(-((t-.038)/.006)**2)
    if name!='cryo':v*=np.minimum(t/.001,1)*np.minimum((duration-t)/.025,1)
    v=np.tanh(v*1.3);v*=.72/max(.72,float(np.abs(v).max()))
    sf.write(out/(name+'.ogg'),v,RATE,format='OGG',subtype='VORBIS')
    events['gun.'+name]={'sounds':[{'name':'neonward:guns/'+name,'attenuation_distance':24}]}
(ROOT/'resources/assets/neonward/sounds.json').write_text(json.dumps(events,indent=2)+'\n',encoding='utf-8')
print('Generated',len(events),'original mono sound assets')
