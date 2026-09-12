#!/usr/bin/env python3
"""Measure heap separately from performance trials, one JVM at a time."""
from pathlib import Path
import argparse,json,subprocess
from run import ROOT,CLAVA,FIXTURES

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--repeats',type=int,default=3);args=ap.parse_args()
    out=ROOT/'results/memory';out.mkdir(exist_ok=True)
    cp=str(ROOT/'build')+':'+str(CLAVA/'Clava-JS/java-binaries/lib/*')
    subprocess.run(['javac','-cp',cp,'-d',str(ROOT/'build'),str(ROOT/'CompleteMemory.java')],check=True)
    records=[]
    for name in ['nas','nas_lu']:
        for mode in ['text','eager','lazy']:
            for trial in range(args.repeats):
                stem=out/f'{name}-{mode}-{trial}'
                command=['/usr/bin/time','-f','%M','-o',str(stem)+'.rss','java','-Xmx4g','-cp',cp,'CompleteMemory',str(FIXTURES[name][0]),str(ROOT/'results'/(name+('.text' if mode=='text' else '.flat'))),mode]
                with Path(str(stem)+'.log').open('w') as log:
                    process=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT)
                records.append({'fixture':name,'mode':mode,'trial':trial,'returncode':process.returncode,'max_rss_kib':int(Path(str(stem)+'.rss').read_text().splitlines()[-1]),'log':Path(str(stem)+'.log').read_text()})
                (out/'summary.json').write_text(json.dumps(records,indent=2)+'\n')
                print(name,mode,trial,process.returncode,flush=True)
    if any(r['returncode'] for r in records):raise SystemExit(1)
if __name__=='__main__':main()
