#!/usr/bin/env python3
"""Run controlled modes serially; retain observations, cache counters and summaries."""
from pathlib import Path
import csv,json,statistics,subprocess,tempfile,sys
from build import ROOT,NATIVE
RESULTS=ROOT/'results'
CP=(ROOT/'build/classpath.txt').read_text()

def summarize(path):
 with path.open() as f:rows=list(csv.DictReader(line for line in f if not line.startswith('#')))
 result={}
 for mode in sorted({x['mode'] for x in rows}):
  selected=[x for x in rows if x['mode']==mode];stats={'n':len(selected)}
  for key in selected[0]:
   if key.endswith('_ms') or key in ['bytes','typed','materialized','deferred','materialized_read','materialized_tu','materialized_query','materialized_codegen','native_rss_kib']:
    vals=[float(x[key]) for x in selected];stats[key]={'median':statistics.median(vals),'min':min(vals),'max':max(vals)}
  result[mode]=stats
 path.with_suffix('.summary.json').write_text(json.dumps(result,indent=2))
 return result

def main():
 fixtures=json.loads((RESULTS/'fixtures.json').read_text())
 for name in sys.argv[1:] or ['nas','wrap','templates','nas_lu']:
  source=fixtures[name]['source']
  for kind in ['consumer','endtoend']:
   csvfile=RESULTS/(name+'.'+kind+'.csv')
   args=['java','-Xms128m','-Xmx512m','-cp',CP]
   if kind=='consumer':args+=['FlatBenchmark',name,source,'text,eager,lazy',str(RESULTS/(name+'.zstd')),str(RESULTS/(name+'.flat')),str(RESULTS/(name+'.flat')),'--text-zstd']
   else:
    run=Path(tempfile.mkdtemp(prefix=name+'-cache-',dir=RESULTS));(RESULTS/(name+'.cache-dir.txt')).write_text(str(run))
    args+=['FlatEndToEnd',str(NATIVE/'build/tool'),source,str(run),'12']
   with csvfile.open('w') as out,(RESULTS/(name+'.'+kind+'.log')).open('w') as log:subprocess.run(args,stdout=out,stderr=log,check=True)
   summary=summarize(csvfile)
   print(name,kind,{m:round(s['total_ms']['median'],3) for m,s in summary.items()},flush=True)
   if kind=='endtoend':
    for cache in ['text-cache','flat-cache','packed-cache','text-cold-11','eager-cold-11','lazy-cold-11','packed-cold-11']:
     for command,suffix in [('--show-stats','stats'),('--show-compression','compression')]:
      p=subprocess.run(['ccache','-d',str(run/cache),command],capture_output=True,text=True,check=True)
      (run/(cache+'.'+suffix)).write_text(p.stdout)
if __name__=='__main__':main()
