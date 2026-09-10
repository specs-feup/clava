#!/usr/bin/env python3
"""Produce native fixtures with one executable and an explicitly selected output mode."""
from pathlib import Path
import os,subprocess,json,time
from build import ROOT,WORKSPACE,NATIVE
RESULTS=ROOT/'results'
FIXTURES={
 'nas':(NATIVE/'test/inputs/nas_bt.c','c11'),
 'wrap':(WORKSPACE/'clava/ClavaWeaver/resources/clava/test/weaver/cpp/src/wrap.cpp','c++17'),
 'templates':(NATIVE/'test/inputs/templates.cpp','c++17'),
 'fidelity_c':(ROOT/'fixtures/fidelity.c','c11'),
 'fidelity_cpp':(ROOT/'fixtures/fidelity.cpp','c++17'),
 'nas_lu':(WORKSPACE/'clava/ClangAstParser/test-resources/c/bench/nas_lu.c','c11'),
}
def main():
 RESULTS.mkdir(exist_ok=True)
 metadata={}
 for name,(source,std) in FIXTURES.items():
  if not source.is_file():continue
  metadata[name]={'source':str(source),'std':std}
  for mode in ['text','zstd','dense','flat']:
   env={k:v for k,v in os.environ.items() if not k.startswith('AST_WIRE_')}
   if mode=='dense':env['AST_WIRE_DENSE_TEXT']='1'
   if mode=='flat':env['AST_WIRE_FLAT']='1'
   args=[str(NATIVE/'build/tool'),'-c',str(source),'-id=1','-system-header-threshold=1','-o',str(RESULTS/(name+'.'+mode))]
   if mode=='zstd':args.append('-ast-dump-compression=zstd')
   args+=['--','-std='+std,'-Wno-unknown-pragmas']
   with (RESULTS/(name+'.'+mode+'.log')).open('w') as log:subprocess.run(args,env=env,stdout=log,stderr=subprocess.STDOUT,check=True)
  print(name,flush=True)
 (RESULTS/'fixtures.json').write_text(json.dumps(metadata,indent=2))
if __name__=='__main__':main()
