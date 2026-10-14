import importlib.util,sys,json,os,shutil,subprocess,time,getpass,re,hashlib,inspect,datetime
from pathlib import Path
ROOT=Path('/home/lmsousa/Documents/Projects/SPeCS/ast-flatbuffers');C=ROOT/'clava'
P=Path('/home/lmsousa/Documents/Projects/SPeCS/ast-protobuf/clava/experiments/protocol-comparison')
B=Path.home()/'.cache/ast-flatbuffers-release-validation/eager-optimization-build-20261006';CL=B/'clava';RT=CL/'ClavaWeaver/build/install/ClavaWeaver'
OUT=P.parents[1]/'experiments/protobuf/suite/results/updated-protobuf-20261008';OUT.mkdir(exist_ok=False,parents=True)
def load(name,p):
 sp=importlib.util.spec_from_file_location(name,p);m=importlib.util.module_from_spec(sp);sys.modules[name]=m;sp.loader.exec_module(m);return m
sys.path.insert(0,str(P));a=load('original_app',P/'app-build/run_app_build_matrix.py');b=load('original_overlay',P/'app-build/build_overlay.py');j=load('current_java',C/'experiments/flatbuffers/suite/run_java_runtime_comparison.py');comparison=a.comparison
execute_source=inspect.getsource(a.execute_cell).replace('        if stage["key"] == "protobuf":\n            command += [f"-PclangDumperRoot={stage[\'native_root\']}"]\n', '')
assert '-PclangDumperRoot' not in execute_source
exec(compile(execute_source,str(P/'app-build/run_app_build_matrix.py'),'exec'),a.__dict__)
s=load('js_controls',C/'experiments/flatbuffers/suite/run_runtime_comparison.py')
CACHE=Path.home()/'.cache/ast-flatbuffers-release-validation'
native=Path('/tmp/clang_ast_exe_lmsousa/clang-dumper')
roots={'protobuf':Path('/home/lmsousa/.cache/protobuf-production-validation/build-20261007')}
tools_by_stage={'protobuf':roots['protobuf']/'clang-dumper/build/tool'}
manifest=json.loads((roots['protobuf']/'clang-dumper/build/clang-dumper-release-manifest.json').read_text())
p=manifest['protocol'];t=manifest['toolchain']
ns='protobuf-v1-'+t['protoc_version']+'-'+t['protobuf_version']+'-4.28.3-4.28.3-'+p['descriptor_sha256']+'-'+p['schema_sha256']+'-'+a.sha256_file(tools_by_stage['protobuf'])
comparison.cache_namespace=lambda st:'clang-dumper-protobuf-ccache-v1/'+ns
original_environment=a.base_environment
def current_environment(st,*args,**kwargs):
 env=original_environment(st,*args,**kwargs)
 if st['key']=='flatbuffers':env['JAVA_TOOL_OPTIONS']=re.sub(r' -Dclava.astWire=\S+','',env['JAVA_TOOL_OPTIONS'])
 env.pop('FLAT_NATIVE',None)
 return env
a.base_environment=current_environment
stages={}
record={'created_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'status':'preparing','methodology':'updated Protobuf only; four serial rounds of App and separate uninstrumented commands; existing Text/FlatBuffers observations retained from their earlier session; no new paired comparisons','sources':{},'runtime_jars':{},'native_sha256':{},'overlay_provenance':{},'frontend_fixtures':{},'observations':[],'preflight':[],'seeds':[],'config_patch':'SHOW_EXEC_INFO default false in each isolated benchmark build, matching original diagnostics-off command policy','driver_sha256':a.sha256_file(Path(__file__))}
for key,root in roots.items():
 cl=root/'clava';rt=cl/'ClavaWeaver/build/install/ClavaWeaver';tool=tools_by_stage[key]
 st={'key':key,'root':root,'clava':cl,'runtime':rt,'runtime_lib':rt/'lib','native_root':root/'clang-dumper' if key=='protobuf' else Path('/home/lmsousa/Documents/Projects/SPeCS/clang-dumper-ccache') if key=='ccache-text' else native,'dumper':tool,'wire':'flat-eager' if key=='flatbuffers' else 'protobuf' if key=='protobuf' else 'text','cache':True,'cache_enabled':True,'toolsha':a.sha256_file(tool)}
 st['overlay']=b.build_overlay(key,cl,rt,OUT/'overlays'/key)
 if key=='flatbuffers':st['frontend_clava']=C
 else:
  js,manifest=s.stage_control_workspace(OUT,key,root);st['frontend_clava']=js.parent;record['frontend_fixtures'][key]=manifest
 stages[key]=st;record['sources'][key]=j.stage_snapshot(key,cl)
 if key!='flatbuffers':record['sources'][key]['native_repository']=j.git_snapshot(st['native_root'])
 record['runtime_jars'][key]={p.name:a.sha256_file(p) for p in (rt/'lib').glob('*.jar')};record['native_sha256'][key]=st['toolsha'];record['overlay_provenance'][key]=json.loads(Path(st['overlay']['provenance']).read_text())
if (OUT/'source-overlays').exists():(OUT/'source-overlays/node_modules').symlink_to(ROOT/'node_modules',target_is_directory=True)
expected_java=j.load_pinned_test_ids();identities={};workloads={};contexts={}
sequencer=OUT/'original-file-order.ts'
original_js_config=a.write_js_config
def ordered_js_config(*args):
 p=original_js_config(*args);t=p.read_text().replace('export default { ...baseConfig,','import OriginalOrder from '+json.dumps(sequencer.as_uri())+';\nexport default { ...baseConfig,').replace('test: { ...baseConfig.test, environment:', 'test: { ...baseConfig.test, sequence: {sequencer:OriginalOrder}, environment:');p.write_text(t);return p
a.write_js_config=ordered_js_config
def save(): (OUT/'results.json').write_text(json.dumps(record,indent=2,default=str)+'\n')
def setup_resources(phase,suite):
 pr=OUT/phase
 temp=pr/'temp'/suite/stage['key'];temp.mkdir(parents=True,exist_ok=True)
 parent=temp/f'clang_ast_exe_{getpass.getuser()}' if suite=='java' else pr/'cache'/suite/stage['key']/'@specs-feup/clava'
 parent.mkdir(parents=True,exist_ok=True)
 dest=parent/'clang-dumper'
 if not dest.exists():dest.symlink_to(native,target_is_directory=True)
 return pr,temp

def validate(row,phase):
 suite=row['suite'];run=Path(row['run_dir'])
 if suite=='java':
  result=j.parse_junit_results(run/'junit-results');ids=result['identities'];assert ids==expected_java
  states=j.parse_task_states(run/'run.log');assert all(v in ['UP-TO-DATE','NO-SOURCE','FROM-CACHE'] for v in states.values()),states
  row['junit_aggregate_s']=result['testcase_duration_s'];row['compile_task_states']=states
 else:
  report=json.loads((run/'vitest.json').read_text());ids=a.js_test_identities(report)
  file_order=[x['name'].split('/Clava-JS/')[1] for x in sorted(report['testResults'],key=lambda x:x['startTime'])];assert file_order==original_order,file_order
  row['js_file_order']=file_order
 if suite not in identities:identities[suite]=ids
 assert ids==identities[suite],(suite,'test identity mismatch')
 row['test_identity_sha256']=a.digest(ids)
 if phase=='app':
  captures=[json.loads(x) for x in Path(row['metrics']).read_text().splitlines() if x.strip()]
  wanted=216 if suite=='java' else 170;assert row['app_calls']==wanted
  if suite=='clava-js':assert row['syntax_only_calls']==130
  payload=a.workload_counter(captures) if hasattr(a,'workload_counter') else sorted((x['group_fingerprint'],x.get('elapsed_ms') is not None) for x in captures)
  context=a.context_pattern(captures)
  wk=(stage['key'],suite)
  if wk not in workloads:workloads[wk]=payload;contexts[wk]=context
  assert payload==workloads[wk],(suite,'App workload changed')
  assert context==contexts[wk],(suite,'context sharing changed')
  row['actual_jvm_args']=captures[0]['jvm_input_arguments'];row['actual_max_heap_bytes']=captures[0]['jvm_max_memory_bytes']
 assert a.sha256_file(stage['dumper'])==stage['toolsha']
 row['phase']=phase
 return row

def app_run(suite,mode,rnd,ordinal,measured):
 pr,temp=setup_resources('app',suite)
 selected_stage={**stage,'clava':stage['frontend_clava']} if suite=='clava-js' else stage
 return validate(a.execute_cell(selected_stage,suite,mode,rnd,pr,ordinal,measured),'app')

def wall_run(suite,mode,rnd,ordinal,measured):
 pr,temp=setup_resources('wall',suite);run=pr/('measured' if measured else 'preflight')/suite/f'{ordinal:03d}-{stage["key"]}-{mode}-r{rnd}';run.mkdir(parents=True)
 env=a.base_environment(stage,suite,mode,rnd,run,run/'unused-metrics',temp,Path(stage['overlay']['overlay_jar']),pr)
 for k in list(env):
  if k.startswith('APP_BUILD_') or k=='FLAT_NATIVE':env.pop(k,None)
 cache=a.cache_dir_for(stage,suite,pr);probe=comparison.install_direct_ccache_probe(run,env) if mode=='direct' else None;comparison.configure_cache_mode(stage,mode,measured,env,cache)
 if suite=='java':
  init=a.write_gradle_overlay_init(run);s=init.read_text();s=s.replace('      classpath = files(System.getenv(\'APP_BUILD_OVERLAY_JAR\')) + classpath\n','');assert 'classpath =' not in s;init.write_text(s)
  cmd=['gradle','--no-daemon','--offline','--init-script',str(P/'java-suite.init.gradle'),'--init-script',str(init),'-p','ClangAstParser','test'];cwd=stage['clava']
  # Release-selected schemas come from the manifest, without a sibling-checkout override.
  testresults=stage['clava']/'ClangAstParser/build/test-results/test'
  if testresults.exists():shutil.rmtree(testresults)
 else:
  frontend=stage['frontend_clava']
  helper=ROOT/'node_modules/@specs-feup/lara/vitest/weaverVitestConfig.ts'
  cfg=run/'vitest.wall.config.ts';cfg.write_text(f'import {{createWeaverVitestConfig}} from {json.dumps(helper.as_uri())};\nimport {{weaverConfig}} from {json.dumps((frontend/"Clava-JS/code/WeaverConfiguration.ts").as_uri())};\nconst base=createWeaverVitestConfig({{...weaverConfig,jarPath:{json.dumps(str(stage['runtime']))},importForSideEffects:{json.dumps([(frontend/"Clava-JS/api/Joinpoints.ts").as_uri(),(frontend/"Clava-JS/code/sideEffects.ts").as_uri()])}}});\nimport OriginalOrder from {json.dumps(sequencer.as_uri())};\nexport default {{...base,test:{{...base.test,sequence:{{sequencer:OriginalOrder}}}},root:{json.dumps(str(frontend/"Clava-JS"))}}};\n')
  cmd=['npm','exec','--workspace','@specs-feup/clava','--','vitest','run','--config',str(cfg),'--reporter=json','--outputFile',str(run/'vitest.json'),'-t',comparison.JS_TEST_FILTER];cwd=frontend/'Clava-JS'
 (run/'command.json').write_text(json.dumps({'argv':cmd,'cwd':str(cwd)},indent=2)+'\n')
 start=time.perf_counter()
 with (run/'run.log').open('w') as log:p=subprocess.run(cmd,cwd=cwd,env=env,stdout=log,stderr=subprocess.STDOUT)
 elapsed=time.perf_counter()-start
 if suite=='java' and testresults.exists():shutil.copytree(testresults,run/'junit-results')
 counts=a.check_test_counts(suite,run,p.returncode);cs=comparison.cache_validation(stage,mode,measured,cache,probe);assert cs['passed'],cs
 row={'suite':suite,'stage':stage['key'],'mode':mode,'repeat':rnd,'measured':measured,'valid':True,'return_code':p.returncode,'wall_s':elapsed,'test_counts':counts,'cache_validation':cs,'run_dir':str(run)}
 return validate(row,'wall')


def set_order(phase):
 global original_order
 original_order=json.loads(Path('/tmp/flatbuffers-original-'+('wall-' if phase=='wall' else '')+'js-file-order.json').read_text())
 sequencer.write_text('import {BaseSequencer} from "vitest/node";\nconst order='+json.dumps(original_order)+';\nexport default class OriginalOrder extends BaseSequencer {async sort(files) {const key=f=>f.moduleId.split("/Clava-JS/")[1];for(const f of files) if(!order.includes(key(f)))throw new Error("Unexpected test file "+key(f));return [...files].sort((a,b)=>order.indexOf(key(a))-order.indexOf(key(b)));}}\n')
 record.setdefault('js_file_orders',{})[phase]=original_order
# All builds and compilation precede any measured command.
for stage in stages.values():
 cmd=['gradle','--no-daemon','--offline','--init-script',str(P/'java-suite.init.gradle'),'-p',str(stage['clava']/'ClangAstParser'),'testClasses']
 # Release-selected schemas come from the manifest, without a sibling-checkout override.
 subprocess.run(cmd,stdout=(OUT/f'testclasses-{stage["key"]}.log').open('w'),stderr=subprocess.STDOUT,check=True)
save();ordinal=0
try:
 for phase,runfun in [('app',app_run),('wall',wall_run)]:
  set_order(phase);seeds={}
  for key in stages:
   stage=stages[key]
   for suite in ['clava-js','java']:
    ordinal+=1;row=runfun(suite,'direct',0,ordinal,False);record['preflight'].append(row);save();print(json.dumps({'preflight':phase,'stage':key,'suite':suite,'valid':True}),flush=True)
    ordinal+=1;row=runfun(suite,'warm',0,ordinal,False);record['seeds'].append(row)
    cache=a.cache_dir_for(stage,suite,OUT/phase);snapshot=OUT/phase/'seeds'/key/suite;snapshot.parent.mkdir(parents=True,exist_ok=True);shutil.copytree(cache,snapshot);seeds[key,suite]=snapshot;save()
  record['status']='running';save()
  keys=list(stages)
  for rnd in range(1,5):
   off=(rnd-1)%len(keys);order=keys[off:]+keys[:off]
   for position,key in enumerate(order):
    stage=stages[key];modes=['direct','cold','warm'];off=(rnd-1+position)%3;modes=modes[off:]+modes[:off]
    for ix,mode in enumerate(modes):
     suites=['clava-js','java'] if (rnd+position+ix)%2==0 else ['java','clava-js']
     for suite in suites:
      if mode=='warm':
       cache=a.cache_dir_for(stage,suite,OUT/phase)
       if cache.exists():shutil.rmtree(cache)
       shutil.copytree(seeds[key,suite],cache)
      ordinal+=1;row=runfun(suite,mode,rnd,ordinal,True);record['observations'].append(row);save();print(json.dumps({k:row.get(k) for k in ['phase','stage','suite','mode','repeat','app_elapsed_ms','wall_s','valid']}),flush=True)
 record['completed_utc']=datetime.datetime.now(datetime.timezone.utc).isoformat();record['status']='complete';save()
except Exception as e:
 record['status']='failed';record['error']=repr(e);save();raise
