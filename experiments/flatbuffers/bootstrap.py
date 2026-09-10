#!/usr/bin/env python3
"""Fetch and build the pinned schema compiler outside the repositories."""
import subprocess
from build import SDK
PIN='7e163021e59cca4f8e1e35a7c828b5c6b7915953'
if not SDK.exists():
 subprocess.run(['git','clone','--depth','1','--branch','v25.12.19','https://github.com/google/flatbuffers.git',str(SDK)],check=True)
actual=subprocess.check_output(['git','-C',str(SDK),'rev-parse','HEAD'],text=True).strip()
if actual!=PIN:raise SystemExit('SDK checkout is not the pinned FlatBuffers revision: '+actual)
subprocess.run(['cmake','-S',str(SDK),'-B',str(SDK/'build-make'),'-DCMAKE_BUILD_TYPE=Release','-DFLATBUFFERS_BUILD_TESTS=OFF','-DFLATBUFFERS_BUILD_FLATLIB=OFF','-DFLATBUFFERS_BUILD_SHAREDLIB=OFF'],check=True)
subprocess.run(['cmake','--build',str(SDK/'build-make'),'--target','flatc','--parallel','4'],check=True)
