#!/usr/bin/env python3
"""Build the isolated Java experiment against the workspace's Clava distribution."""
from pathlib import Path
import os, subprocess
ROOT = Path(__file__).resolve().parent
WORKSPACE = ROOT.parents[2]
NATIVE = Path(os.environ.get('FLAT_NATIVE', str(WORKSPACE.parent / 'clang-dumper-ast-flatbuffers')))
SDK = Path(os.environ.get('FLATBUFFERS_ROOT', str(Path.home()/'.cache/ast-flatbuffers-planning/flatbuffers')))
FLATC = SDK/'build-make/flatc'
CLASSES = ROOT/'build/classes'
GENERATED = ROOT/'build/generated'
def main():
    for path in (CLASSES,GENERATED): path.mkdir(parents=True,exist_ok=True)
    subprocess.run([str(FLATC),'--java','-o',str(GENERATED),str(NATIVE/'wire/wire.fbs')],check=True)
    cp = str(CLASSES)+os.pathsep+str(WORKSPACE/'clava/Clava-JS/java-binaries/lib/*')
    sources = list((SDK/'java/src/main/java/com/google/flatbuffers').glob('*.java'))
    if not sources: sources=list((SDK/'java/com/google/flatbuffers').glob('*.java'))
    sources += list(GENERATED.rglob('*.java')) + list((ROOT/'maps/src').rglob('*.java'))
    sources += [WORKSPACE/('specs-java-libs/jOptions/src/org/suikasoft/jOptions/DataStore/'+name+'.java') for name in ['ListDataStore','MemoizedDataStore']]
    sources += list((ROOT/'java').glob('*.java'))
    subprocess.run(['javac','-d',str(CLASSES),'-cp',cp,*map(str,sources)],check=True)
    (ROOT/'build/classpath.txt').write_text(cp)
    print(cp)

if __name__ == "__main__": main()
