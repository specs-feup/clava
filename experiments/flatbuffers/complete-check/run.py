#!/usr/bin/env python3
"""Compare complete v2 eager/lazy import against the current text importer."""
from pathlib import Path
import argparse, json, os, shutil, subprocess, sys, time
ROOT=Path(__file__).resolve().parent
CLAVA=ROOT.parents[2]
NATIVE=Path(os.environ.get('FLAT_NATIVE',str(CLAVA.parent.parent/'clang-dumper-ast-flatbuffers')))
sys.path.insert(0,str(ROOT.parent))
from prepare import FIXTURES
FIXTURES.update({
 'sizeof_qr':(CLAVA/'ClavaWeaver/resources/clava/test/api/c/src/qr.c','c11'),
 'sizeof_strcpy':(CLAVA/'ClavaWeaver/resources/clava/test/api/c/src/strcpy.c','c11'),
 'sizeof_pack':(ROOT/'fixtures/sizeof_pack.cpp','c++11'),
 'anonymous_designator':(ROOT/'fixtures/anonymous_designator.c','c11'),
 'using_namespace':(ROOT/'fixtures/using_namespace.cpp','c++11'),
})

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('fixtures',nargs='*',default=list(FIXTURES))
    args=parser.parse_args()
    build=ROOT/'build'
    if build.exists():shutil.rmtree(build)
    build.mkdir()
    results=ROOT/'results';results.mkdir(exist_ok=True)
    cp=str(build)+os.pathsep+str(CLAVA/'Clava-JS/java-binaries/lib/*')
    subprocess.run(['javac','-cp',cp,'-d',str(build),*map(str,ROOT.glob('*.java'))],check=True)
    outcomes={}
    for name in args.fixtures:
        source,std=FIXTURES[name]
        result={'source':str(source),'standard':std,'formats':{}}
        for fmt in ('text','flat'):
            dump=results/(name+'.'+fmt)
            command=[str(NATIVE/'build/tool'),'-c',str(source),'-id=1','-system-header-threshold=1','-o',str(dump)]
            if fmt=='flat':command+=['-ast-dump-format=flatbuffers-v2']
            command+=['--','-std='+std,'-Wno-unknown-pragmas']
            env={k:v for k,v in os.environ.items() if not k.startswith('AST_WIRE_')}
            start=time.perf_counter()
            with (results/(name+'.'+fmt+'.log')).open('w') as log:
                run=subprocess.run(command,env=env,stdout=log,stderr=subprocess.STDOUT)
            result['formats'][fmt]={'returncode':run.returncode,'seconds':time.perf_counter()-start,'bytes':dump.stat().st_size,'command':command}
        if all(v['returncode']==0 for v in result['formats'].values()):
            command=['java','-Xmx4g','-cp',cp,'CompleteChecks',str(source),str(results/(name+'.text')),str(results/(name+'.flat')),str(results/(name+'.flat'))]
            with (results/(name+'.check.log')).open('w') as log:
                run=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT)
            result['check_returncode']=run.returncode
        outcomes[name]=result
        (results/'summary.json').write_text(json.dumps(outcomes,indent=2)+'\n')
        print(name,result.get('check_returncode','producer failed'),flush=True)
    return 0 if all(r.get('check_returncode')==0 for r in outcomes.values()) else 1
if __name__=='__main__':raise SystemExit(main())
