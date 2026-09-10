#!/usr/bin/env python3
"""Check generated native records, reconstructed wire bytes and actual Clava graphs/code."""
from pathlib import Path
import subprocess,json,re
from build import ROOT,NATIVE
RESULTS=ROOT/'results'
CP=(ROOT/'build/classpath.txt').read_text()

def normalize(data):
 ids={}
 def replace(match):
  value=match.group()
  return ids.setdefault(value,b'NODE_'+str(len(ids)).encode())
 return re.sub(rb'(?m)^(?:0x[0-9a-fA-F]+_[0-9]+|@[1-9][0-9]*)$',replace,data)

def main():
 records=[]
 for name,fixture in json.loads((RESULTS/'fixtures.json').read_text()).items():
  with (RESULTS/(name+'.validation.log')).open('w') as out:
   subprocess.run([str(NATIVE/'build/verify_flat_wire'),str(RESULTS/(name+'.flat'))],stdout=out,stderr=subprocess.STDOUT,check=True)
   subprocess.run(['java','-cp',CP,'FlatWireCheck',str(RESULTS/(name+'.flat')),str(RESULTS/(name+'.reconstructed'))],stdout=out,stderr=subprocess.STDOUT,check=True)
   original=normalize((RESULTS/(name+'.text')).read_bytes())
   rebuilt=normalize((RESULTS/(name+'.reconstructed')).read_bytes())
   if original!=rebuilt:raise AssertionError(name+' normalized wire mismatch')
   out.write('normalized_wire_equal bytes='+str(len(original))+'\n');out.flush()
   subprocess.run(['java','-Xms128m','-Xmx768m','-cp',CP,'FlatChecks',fixture['source'],str(RESULTS/(name+'.zstd')),str(RESULTS/(name+'.flat')),str(RESULTS/(name+'.flat')),'--text-zstd'],stdout=out,stderr=subprocess.STDOUT,check=True)
  records.append({'fixture':name,'wire_bytes':len(original),'wire_equal':True,'graph_equal':True,'code_equal':True})
  print(name,'passed',flush=True)
 (RESULTS/'validation.json').write_text(json.dumps(records,indent=2))
 subprocess.run(['java','-cp',CP,'MappedRecordsTest'],check=True)
if __name__=='__main__':main()
