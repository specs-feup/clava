#!/usr/bin/env python3
"""Measure Java process RSS and retained heap separately from timed runs."""
from pathlib import Path
import subprocess,json,random
from build import ROOT
RESULTS=ROOT/'results'
cp=(ROOT/'build/classpath.txt').read_text()
fixtures=json.loads((RESULTS/'fixtures.json').read_text())
rows=[]
for name in ['nas','wrap','templates','nas_lu']:
 for trial in range(3):
  modes=['text','eager','lazy'];random.Random(991+trial).shuffle(modes)
  for mode in modes:
   stem=RESULTS/f'{name}.memory.{mode}.{trial}'
   with stem.with_suffix(stem.suffix+'.log').open('w') as log:
    subprocess.run(['/usr/bin/time','-f','%M','-o',str(stem)+'.rss','java','-Xms128m','-Xmx512m','-cp',cp,'FlatMemory',fixtures[name]['source'],str(RESULTS/(name+('.zstd' if mode=='text' else '.flat'))),mode],stdout=log,stderr=subprocess.STDOUT,check=True)
   phases={};collected=False
   for line in Path(str(stem)+'.log').read_text().splitlines():
    if ',heap=' in line:
     fields=line.split(',');phases[fields[0]]={k:int(v) for k,v in (x.split('=') for x in fields[1:])}
    if line=='tu_collected=true':collected=True
   rows.append(dict(fixture=name,mode=mode,trial=trial,rss_kib=int(Path(str(stem)+'.rss').read_text()),phases=phases,tu_collected=collected))
 print(name,'memory complete',flush=True)
(RESULTS/'memory.json').write_text(json.dumps(rows,indent=2))
