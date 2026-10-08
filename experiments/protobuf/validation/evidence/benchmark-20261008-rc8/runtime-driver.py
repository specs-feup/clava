import importlib.util,sys,json,os,shutil,subprocess,time,getpass,re,hashlib,inspect,datetime
from urllib.parse import urlparse,unquote
from pathlib import Path
ROOT=Path.home()/'.cache/protobuf-production-validation/build-final-20261008'
B=ROOT;C=ROOT/'clava';CL=C;P=CL/'experiments/protocol-comparison';RT=CL/'ClavaWeaver/build/install/ClavaWeaver'
RELEASE_TAG_FILE=CL/'ClangAstParser/clang-dumper-release.tag';RELEASE_TAG=RELEASE_TAG_FILE.read_text().strip()
if not re.fullmatch(r'v[0-9]+(?:\.[0-9]+)+_[0-9]+-rc[0-9]+',RELEASE_TAG): raise SystemExit(f'snapshot selector is not an RC release tag: {RELEASE_TAG}')
ASSET_ROOT=ROOT/'clang-dumper/build';RELEASE_MANIFEST_PATH=ASSET_ROOT/'clang-dumper-release-manifest.json'
if not RELEASE_MANIFEST_PATH.is_file(): raise SystemExit(f'verified published {RELEASE_TAG} manifest has not been staged')
manifest_bytes=RELEASE_MANIFEST_PATH.read_bytes();manifest=json.loads(manifest_bytes)
protocol=manifest['protocol'];toolchain=manifest['toolchain'];assets=manifest['assets']
asset_by_name={entry['filename']:entry for entry in assets}
required_names=('clang-dumper-ast-wire.proto','clang-dumper-ast-wire.pb')
for name,field in zip(required_names,('schema_sha256','descriptor_sha256')):
    entry=asset_by_name.get(name);path=ASSET_ROOT/name
    if not entry or entry.get('kind')!='protocol' or entry.get('sha256')!=protocol.get(field) or not path.is_file(): raise SystemExit(f'verified release protocol asset missing: {name}')
    if hashlib.sha256(path.read_bytes()).hexdigest()!=entry['sha256']: raise SystemExit(f'release asset hash mismatch: {name}')
tool_candidates=[entry for entry in assets if entry.get('kind')=='tool' and entry.get('platform')=='linux' and entry.get('arch')=='x64']
if len(tool_candidates)!=1: raise SystemExit(f'expected exactly one Linux x64 tool in release manifest, found {len(tool_candidates)}')
tool_asset=tool_candidates[0];tool_path=ASSET_ROOT/tool_asset['filename']
if not tool_path.is_file() or hashlib.sha256(tool_path.read_bytes()).hexdigest()!=tool_asset['sha256']: raise SystemExit(f'verified {RELEASE_TAG} Linux x64 executable is missing or has the wrong hash')
java_gradle=(CL/'ClangAstParser/build.gradle').read_text()
java_protoc=re.search(r"def protocVersion = '([^']+)'",java_gradle).group(1)
java_runtime=re.search(r"def protobufVersion = '([^']+)'",java_gradle).group(1)
ns='protobuf-v1-'+toolchain['protoc_version']+'-'+toolchain['protobuf_version']+'-'+java_protoc+'-'+java_runtime+'-'+protocol['descriptor_sha256']+'-'+protocol['schema_sha256']+'-'+tool_asset['sha256']
RESOURCE_CACHE_INPUT=os.environ.get('PROTOBUF_VERIFIED_RESOURCE_CACHE')
if not RESOURCE_CACHE_INPUT: raise SystemExit('set PROTOBUF_VERIFIED_RESOURCE_CACHE to consumer_resume verified DUMPER_FOLDER/clang-dumper cache')
resource_input=Path(RESOURCE_CACHE_INPUT).expanduser().resolve()
resource_candidates=[resource_input,resource_input/'clang-dumper',resource_input.parent,resource_input.parent.parent]
RESOURCE_SEED_ROOT=next((candidate for candidate in resource_candidates if (candidate/'releases'/RELEASE_TAG/'clang-dumper-release-manifest.json').is_file()),None)
if RESOURCE_SEED_ROOT is None: raise SystemExit(f'verified resource cache has no releases/{RELEASE_TAG}/manifest: {resource_input}')
SEED_RELEASE_DIR=RESOURCE_SEED_ROOT/'releases'/RELEASE_TAG
if (SEED_RELEASE_DIR/'clang-dumper-release-manifest.json').read_bytes()!=manifest_bytes: raise SystemExit('consumer resource cache manifest differs byte-for-byte from the verified published manifest')
seed_tool=SEED_RELEASE_DIR/tool_asset['filename']
if not seed_tool.is_file() or hashlib.sha256(seed_tool.read_bytes()).hexdigest()!=tool_asset['sha256']: raise SystemExit(f'consumer resource cache tool does not match verified {RELEASE_TAG} asset')
OUT=CL/'experiments/protobuf/suite/results/final-protobuf-20261008-rc8-v2'
if OUT.exists(): raise SystemExit(f'refusing to reuse final output folder: {OUT}')
def load(name,p):
 sp=importlib.util.spec_from_file_location(name,p);m=importlib.util.module_from_spec(sp);sys.modules[name]=m;sp.loader.exec_module(m);return m
sys.path.insert(0,str(P));a=load('final_app',P/'app-build/run_app_build_matrix.py');b=load('final_overlay',P/'app-build/build_overlay.py');HELPER_CL=Path.home()/'.cache/ast-flatbuffers-release-validation/eager-optimization-build-20261006/clava';j=load('final_java',HELPER_CL/'experiments/flatbuffers/suite/run_java_runtime_comparison.py');comparison=a.comparison
execute_source=inspect.getsource(a.execute_cell).replace('        if stage["key"] == "protobuf":\n            command += [f"-PclangDumperRoot={stage[\'native_root\']}"]\n', '')
assert '-PclangDumperRoot' not in execute_source
exec(compile(execute_source,str(P/'app-build/run_app_build_matrix.py'),'exec'),a.__dict__)
s=load('final_js_controls',HELPER_CL/'experiments/flatbuffers/suite/run_runtime_comparison.py')
s.CLAVA_ROOT=CL;s.CLAVA_JS_ROOT=CL/'Clava-JS'
native=ROOT/'clang-dumper'
roots={'protobuf':ROOT};assert set(roots)=={'protobuf'}
tools_by_stage={'protobuf':tool_path}
comparison.cache_namespace=lambda st:'clang-dumper-protobuf-ccache-v1/'+ns
RESOURCE_ASSET_PATHS={entry['filename']:ASSET_ROOT/entry['filename'] for entry in assets if (ASSET_ROOT/entry['filename']).is_file()}
for filename,path in RESOURCE_ASSET_PATHS.items():
    if hashlib.sha256(path.read_bytes()).hexdigest()!=asset_by_name[filename]['sha256']: raise SystemExit(f'verified release asset hash mismatch: {filename}')
OUT.mkdir(exist_ok=False,parents=True)
BASE_JAVA_INIT=P/'java-suite.init.gradle'
JAVA_SUITE_INIT=OUT/'java-suite.init.gradle'
base_java_init=BASE_JAVA_INIT.read_text()
extra_java_class='pt.up.fe.specs.clang.dumper.ClangFrameworkSearchTest'
if extra_java_class in base_java_init:raise RuntimeError(f'base benchmark init script already excludes the newly-added class: {extra_java_class}')
anchor="                excludeTestsMatching 'pt.up.fe.specs.clang.utils.ClassesServiceConcurrencyTest'\n"
if base_java_init.count(anchor)!=1:raise RuntimeError('could not locate the fixed Java selection filter insertion point')
base_java_init=base_java_init.replace(anchor,anchor+"                // Keep the frozen pre-framework 116-test benchmark cohort.\n                excludeTestsMatching 'pt.up.fe.specs.clang.dumper.ClangFrameworkSearchTest'\n",1)
JAVA_SUITE_INIT.write_text(base_java_init)
# App runner looks up its Java init file under SCRIPT_ROOT; route only its test
# selection to this run-local copy while keeping the source checkout untouched.
a.SCRIPT_ROOT=OUT
JAVA_SELECTION={'base_init_script_sha256':a.sha256_file(BASE_JAVA_INIT),'run_init_script':str(JAVA_SUITE_INIT),'run_init_script_sha256':a.sha256_file(JAVA_SUITE_INIT),'excluded_added_class':extra_java_class,'excluded_test_ids':['pt.up.fe.specs.clang.dumper.ClangFrameworkSearchTest#automaticClangSearchSkipsWrongVersionsAndKeepsExplicitOverride()','pt.up.fe.specs.clang.dumper.ClangFrameworkSearchTest#bundledFrameworkAndOtherIncludeRootsKeepTheirOwnFlags()','pt.up.fe.specs.clang.dumper.ClangFrameworkSearchTest#clang18FindsHeadersThroughGeneratedFrameworkArguments()']}
def tree_sha256(root):
 rows={path.relative_to(root).as_posix():a.sha256_file(path) for path in sorted(root.rglob('*')) if path.is_file()}
 return a.digest(rows)
def tracked_tree_sha256(repo,prefix):
 raw=subprocess.run(['git','-C',str(repo),'ls-files','-z','--',prefix],capture_output=True,check=True).stdout.split(b'\0')
 rows={}
 for item in raw:
  if item:
   rel=os.fsdecode(item);path=repo/rel
   if path.is_file():rows[rel[len(prefix.rstrip('/')+'/'):]]=a.sha256_file(path)
 return a.digest(rows)
original_environment=a.base_environment
def current_environment(st,*args,**kwargs):
 env=original_environment(st,*args,**kwargs)
 if st['key']=='flatbuffers':env['JAVA_TOOL_OPTIONS']=re.sub(r' -Dclava.astWire=\S+','',env['JAVA_TOOL_OPTIONS'])
 env.pop('FLAT_NATIVE',None)
 return env
a.base_environment=current_environment
stages={}
record={'created_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'status':'preparing','java_selection':JAVA_SELECTION,'methodology':'Protobuf only; four serial rounds of App and separate uninstrumented commands; no Text or FlatBuffers controls rerun','sources':{},'runtime_jars':{},'native_sha256':{},'overlay_provenance':{},'frontend_fixtures':{},'observations':[],'preflight':[],'seeds':[],'resource_staging':[],'config_patch':'SHOW_EXEC_INFO default false in each isolated benchmark build, matching original diagnostics-off command policy','driver_sha256':a.sha256_file(Path(__file__)),'selected_release':{'tag':RELEASE_TAG,'manifest_sha256':hashlib.sha256(manifest_bytes).hexdigest(),'manifest_path':str(RELEASE_MANIFEST_PATH),'asset_sha256':{name:asset_by_name[name]['sha256'] for name in sorted(RESOURCE_ASSET_PATHS)},'tool_asset':tool_asset['filename'],'cache_namespace':ns},'verified_resource_seed':{'root':str(RESOURCE_SEED_ROOT),'manifest_sha256':hashlib.sha256((SEED_RELEASE_DIR/'clang-dumper-release-manifest.json').read_bytes()).hexdigest(),'tool_sha256':tool_asset['sha256'],'tree_sha256':tree_sha256(RESOURCE_SEED_ROOT)},'workload_contract':{'java_pinned_test_ids':116,'js_tests':164,'js_passed':158,'js_skipped':6,'java_app_calls':216,'js_app_calls':170,'js_syntax_only_calls':130,'repeats':4,'workers':1,'stages':['protobuf']},'runtime_tree_sha256':{'clava_js_dist':tree_sha256(CL/'Clava-JS/dist'),'lara_js_dist':tree_sha256(ROOT/'lara-framework/Lara-JS/dist')}}

def save(): (OUT/'results.json').write_text(json.dumps(record,indent=2,default=str)+'\n')
save()

# Build emitted package entrypoints and resolve/install the selected release runtime before timed commands.
build_env=os.environ.copy();build_env.update({'SPECS_JAVA_LIBS_HOME':str(ROOT/'specs-java-libs'),'LARA_FRAMEWORK_HOME':str(ROOT/'lara-framework')})
for package in (ROOT/'lara-framework/Lara-JS',CL/'Clava-JS'):
 subprocess.run(['npm','run','build'],cwd=package,env=build_env,stdout=(OUT/f'npm-build-{package.parent.name}.log').open('w'),stderr=subprocess.STDOUT,check=True)
# Resolve every runtime module through package exports after emission. The source
# tests import these same specifiers, so benchmark configs must share their dist
# module identity instead of importing parallel source modules by file URL.
runtime_specifiers={
 'lara_config':'@specs-feup/lara/vitest/weaverVitestConfig.ts',
 'lara_environment':'@specs-feup/lara/vitest/weaverEnvironment.ts',
 'clava_config':'@specs-feup/clava/code/WeaverConfiguration.ts',
 'clava_joinpoints':'@specs-feup/clava/api/Joinpoints.ts',
 'clava_side_effects':'@specs-feup/clava/code/sideEffects.ts',
}
resolve_js='const specs='+json.dumps(runtime_specifiers)+'; console.log(JSON.stringify(Object.fromEntries(Object.entries(specs).map(([key,spec])=>[key,import.meta.resolve(spec)]))))'
resolved_runtime=json.loads(subprocess.run(['node','--input-type=module','-e',resolve_js],cwd=CL/'Clava-JS',env=build_env,capture_output=True,text=True,check=True).stdout)
for key,url in resolved_runtime.items():
 if not url.startswith('file:') or '/dist/' not in url or not url.endswith('.js'):
  raise RuntimeError(f'package export {runtime_specifiers[key]} did not resolve to emitted JavaScript: {url}')
 if not Path(unquote(urlparse(url).path)).is_file():
  raise RuntimeError(f'emitted package runtime module is missing: {url}')
record['js_runtime_exports']={'specifiers':runtime_specifiers,'resolved_urls':resolved_runtime}
save()
install_cmd=['gradle','--no-daemon','--offline','-p',str(CL/'ClavaWeaver'),'installDist']
subprocess.run(install_cmd,cwd=CL,env=build_env,stdout=(OUT/'installdist-protobuf.log').open('w'),stderr=subprocess.STDOUT,check=True)
testclasses_cmd=['gradle','--no-daemon','--offline','--init-script',str(JAVA_SUITE_INIT),'-p',str(CL/'ClangAstParser'),'testClasses']
subprocess.run(testclasses_cmd,cwd=CL,env=build_env,stdout=(OUT/'testclasses-protobuf.log').open('w'),stderr=subprocess.STDOUT,check=True)
runtime=CL/'ClavaWeaver/build/install/ClavaWeaver'
if not (runtime/'lib/ClangAstParser.jar').is_file():raise RuntimeError(f'installDist did not produce parser runtime: {runtime}')
probe_source=CL/'experiments/protobuf/validation/java/ValidationProbe.java'
probe_classes=CL/'ClangAstParser/build/protobuf-validation-classes'
if probe_classes.exists():shutil.rmtree(probe_classes)
probe_classes.mkdir(parents=True,exist_ok=True)
javac_cmd=['javac','-cp',str(runtime/'lib/*'),'-d',str(probe_classes),str(probe_source)]
subprocess.run(javac_cmd,cwd=CL,env=build_env,stdout=(OUT/'validation-probe-javac.log').open('w'),stderr=subprocess.STDOUT,check=True)
if not (probe_classes/'ValidationProbe.class').is_file():raise RuntimeError(f'ValidationProbe compile did not produce its entry class: {probe_classes}')
record['runtime_tree_sha256']={'clava_js_dist':tree_sha256(CL/'Clava-JS/dist'),'lara_js_dist':tree_sha256(ROOT/'lara-framework/Lara-JS/dist')}
resolved_manifest_path=CL/'ClangAstParser/build/generated/proto-release/main/resolved-manifest.json'
record['resolved_consumer_manifest']={'path':str(resolved_manifest_path),'sha256':a.sha256_file(resolved_manifest_path),'content':json.loads(resolved_manifest_path.read_text())}
record['build_environment']={'node':subprocess.run(['node','--version'],capture_output=True,text=True,check=True).stdout.strip(),'npm':subprocess.run(['npm','--version'],capture_output=True,text=True,check=True).stdout.strip()}
record['preparation']={'npm_builds':'passed','installDist':'passed','testClasses':'passed','ValidationProbe_javac':'passed','ValidationProbe_classes':str(probe_classes),'release_assets_from_published_tag':RELEASE_TAG}
save()
for key,root in roots.items():
 cl=root/'clava';rt=cl/'ClavaWeaver/build/install/ClavaWeaver';tool=tools_by_stage[key]
 st={'key':key,'root':root,'clava':cl,'runtime':rt,'runtime_lib':rt/'lib','native_root':root/'clang-dumper' if key=='protobuf' else Path('/home/lmsousa/Documents/Projects/SPeCS/clang-dumper-ccache') if key=='ccache-text' else native,'dumper':tool,'wire':'flat-eager' if key=='flatbuffers' else 'protobuf' if key=='protobuf' else 'text','cache':True,'cache_enabled':True,'toolsha':a.sha256_file(tool)}
 st['overlay']=b.build_overlay(key,cl,rt,OUT/'overlays'/key)
 if key=='flatbuffers':st['frontend_clava']=C
 else:
  js,manifest=s.stage_control_workspace(OUT,key,root);st['frontend_clava']=js.parent;record['frontend_fixtures'][key]=manifest
 stages[key]=st
 source_snapshot=j.stage_snapshot(key,cl)
 source_snapshot['selected_release'].update({'tag':RELEASE_TAG,'manifest_sha256':hashlib.sha256(manifest_bytes).hexdigest(),'tool_sha256':st['toolsha'],'tool_asset':tool_asset})
 record['sources'][key]=source_snapshot
 record['sources'][key]['clava_js_source_sha256']=tracked_tree_sha256(cl,'Clava-JS');record['sources'][key]['lara_js_source_sha256']=tracked_tree_sha256(ROOT/'lara-framework','Lara-JS');record['sources'][key]['validation_probe_sha256']=a.sha256_file(cl/'experiments/protobuf/validation/java/ValidationProbe.java')
 if key!='flatbuffers':record['sources'][key]['native_repository']=j.git_snapshot(st['native_root'])
 record['runtime_jars'][key]={p.name:a.sha256_file(p) for p in (rt/'lib').glob('*.jar')};record['native_sha256'][key]=st['toolsha'];record['overlay_provenance'][key]=json.loads(Path(st['overlay']['provenance']).read_text())
assert set(stages)=={'protobuf'}
if (OUT/'source-overlays').exists():(OUT/'source-overlays/node_modules').symlink_to(ROOT/'node_modules',target_is_directory=True)
expected_java=j.load_pinned_test_ids();assert len(expected_java)==116;identities={};workloads={};contexts={}
sequencer=OUT/'original-file-order.ts'
source_js_config=a.write_js_config
def emitted_js_config(*args):
 p=source_js_config(*args);t=p.read_text();clava=args[1]['clava']
 source_urls={
  (clava.parent/'node_modules/@specs-feup/lara/vitest/weaverVitestConfig.ts').as_uri():resolved_runtime['lara_config'],
  (clava/'Clava-JS/code/WeaverConfiguration.ts').as_uri():resolved_runtime['clava_config'],
  (clava/'Clava-JS/api/Joinpoints.ts').as_uri():resolved_runtime['clava_joinpoints'],
  (clava/'Clava-JS/code/sideEffects.ts').as_uri():resolved_runtime['clava_side_effects'],
 }
 for source_url,emitted_url in source_urls.items():
  needle=json.dumps(source_url)
  if needle not in t:raise RuntimeError(f'expected source runtime import missing from App config: {source_url}')
  t=t.replace(needle,json.dumps(emitted_url))
 if any(json.dumps(url) in t for url in source_urls):raise RuntimeError(f'App config still names a source runtime module: {p}')
 environment_shim=p.parent/'appBuildWeaverEnvironment.ts'
 shim_text=environment_shim.read_text()
 source_environment=(clava.parent/'node_modules/@specs-feup/lara/vitest/weaverEnvironment.ts').as_uri()
 if json.dumps(source_environment) not in shim_text:raise RuntimeError(f'expected source Lara environment import missing from App shim: {source_environment}')
 shim_text=shim_text.replace(json.dumps(source_environment),json.dumps(resolved_runtime['lara_environment']))
 if source_environment in shim_text:raise RuntimeError(f'App shim still names the source Lara environment: {environment_shim}')
 environment_shim.write_text(shim_text)
 p.write_text(t);return p
def ordered_js_config(*args):
 p=emitted_js_config(*args);t=p.read_text().replace('export default { ...baseConfig,','import OriginalOrder from '+json.dumps(sequencer.as_uri())+';\nexport default { ...baseConfig,').replace('test: { ...baseConfig.test, environment:', 'test: { ...baseConfig.test, sequence: {sequencer:OriginalOrder}, environment:');p.write_text(t);return p
a.write_js_config=ordered_js_config
def setup_resources(phase,suite,run_key):
 pr=OUT/phase
 temp=pr/'temp'/suite/stage['key'];temp.mkdir(parents=True,exist_ok=True)
 parent=temp/f'clang_ast_exe_{getpass.getuser()}' if suite=='java' else pr/'cache'/suite/stage['key']/'@specs-feup/clava'
 parent.mkdir(parents=True,exist_ok=True)
 dest=parent/'clang-dumper'
 if not dest.exists():
  shutil.copytree(RESOURCE_SEED_ROOT,dest)
  if tree_sha256(dest)!=tree_sha256(RESOURCE_SEED_ROOT):raise RuntimeError(f'isolated DUMPER_FOLDER copy differs from verified seed: {dest}')
 release_dir=dest/'releases'/RELEASE_TAG
 release_dir.mkdir(parents=True,exist_ok=True)
 for filename in ('clang-dumper-release-manifest.json',tool_asset['filename'],'clang-dumper-ast-wire.proto','clang-dumper-ast-wire.pb'):
  source=SEED_RELEASE_DIR/filename
  if not source.is_file():source=ASSET_ROOT/filename
  if not source.is_file():raise RuntimeError(f'missing verified {RELEASE_TAG} resource asset {filename}')
  expected=hashlib.sha256(source.read_bytes()).hexdigest() if filename=='clang-dumper-release-manifest.json' else asset_by_name[filename]['sha256']
  if hashlib.sha256(source.read_bytes()).hexdigest()!=expected:raise RuntimeError(f'verified {RELEASE_TAG} resource asset hash mismatch: {filename}')
  target=release_dir/filename
  if target.is_file() and hashlib.sha256(target.read_bytes()).hexdigest()!=expected:raise RuntimeError(f'isolated DUMPER_FOLDER has stale resource asset: {target}')
  if not target.exists():shutil.copy2(source,target)
 if (release_dir/'clang-dumper-release-manifest.json').read_bytes()!=manifest_bytes:raise RuntimeError(f'isolated DUMPER_FOLDER changed verified {RELEASE_TAG} manifest bytes')
 staged_hashes={filename:hashlib.sha256((release_dir/filename).read_bytes()).hexdigest() for filename in ('clang-dumper-release-manifest.json',tool_asset['filename'],'clang-dumper-ast-wire.proto','clang-dumper-ast-wire.pb')}
 record['resource_staging'].append({'phase':phase,'suite':suite,'run':run_key,'dumper_folder':str(dest),'cache_tree_sha256':tree_sha256(dest),'release_tag':RELEASE_TAG,'manifest_sha256':staged_hashes['clang-dumper-release-manifest.json'],'asset_sha256':staged_hashes})
 save()
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
 pr,temp=setup_resources('app',suite,f'{ordinal:03d}-{stage["key"]}-{mode}-r{rnd}')
 selected_stage={**stage,'clava':stage['frontend_clava']} if suite=='clava-js' else stage
 return validate(a.execute_cell(selected_stage,suite,mode,rnd,pr,ordinal,measured),'app')

def wall_run(suite,mode,rnd,ordinal,measured):
 pr,temp=setup_resources('wall',suite,f'{ordinal:03d}-{stage["key"]}-{mode}-r{rnd}');run=pr/('measured' if measured else 'preflight')/suite/f'{ordinal:03d}-{stage["key"]}-{mode}-r{rnd}';run.mkdir(parents=True)
 env=a.base_environment(stage,suite,mode,rnd,run,run/'unused-metrics',temp,Path(stage['overlay']['overlay_jar']),pr)
 for k in list(env):
  if k.startswith('APP_BUILD_') or k=='FLAT_NATIVE':env.pop(k,None)
 cache=a.cache_dir_for(stage,suite,pr);probe=comparison.install_direct_ccache_probe(run,env) if mode=='direct' else None;comparison.configure_cache_mode(stage,mode,measured,env,cache)
 if suite=='java':
  init=a.write_gradle_overlay_init(run);s=init.read_text();s=s.replace('      classpath = files(System.getenv(\'APP_BUILD_OVERLAY_JAR\')) + classpath\n','');assert 'classpath =' not in s;init.write_text(s)
  cmd=['gradle','--no-daemon','--offline','--init-script',str(JAVA_SUITE_INIT),'--init-script',str(init),'-p','ClangAstParser','test'];cwd=stage['clava']
  # Release-selected schemas come from the manifest, without a sibling-checkout override.
  testresults=stage['clava']/'ClangAstParser/build/test-results/test'
  if testresults.exists():shutil.rmtree(testresults)
 else:
  frontend=stage['frontend_clava']
  wall_side_effects=[resolved_runtime['clava_joinpoints'],resolved_runtime['clava_side_effects']]
  cfg=run/'vitest.wall.config.ts'
  wall_config=(
   'import {createWeaverVitestConfig} from '+json.dumps(resolved_runtime['lara_config'])+';\n'
   'import {weaverConfig} from '+json.dumps(resolved_runtime['clava_config'])+';\n'
   'const base=createWeaverVitestConfig({...weaverConfig,jarPath:'+json.dumps(str(stage['runtime']))+',importForSideEffects:'+json.dumps(wall_side_effects)+'});\n'
   'import OriginalOrder from '+json.dumps(sequencer.as_uri())+';\n'
   'export default {...base,test:{...base.test,sequence:{sequencer:OriginalOrder}},root:'+json.dumps(str(frontend/'Clava-JS'))+'};\n'
  )
  cfg.write_text(wall_config)
  if any(source in wall_config for source in ('Clava-JS/api/Joinpoints.ts','Clava-JS/code/sideEffects.ts','Clava-JS/code/WeaverConfiguration.ts','weaverVitestConfig.ts')):
   raise RuntimeError(f'wall config still imports a source runtime module: {cfg}')
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
 expected={(phase,suite,mode,rnd) for phase in ('app','wall') for suite in ('clava-js','java') for mode in ('direct','cold','warm') for rnd in range(1,5)}
 actual={(row['phase'],row['suite'],row['mode'],row['repeat']) for row in record['observations']}
 if len(record['observations'])!=48 or actual!=expected or any(row.get('stage')!='protobuf' or not row.get('valid') for row in record['observations']):raise RuntimeError(f'final measurement matrix mismatch: rows={len(record["observations"])} missing={sorted(expected-actual)} extra={sorted(actual-expected)}')
 record['completed_utc']=datetime.datetime.now(datetime.timezone.utc).isoformat();record['status']='complete';save()
except Exception as e:
 record['status']='failed';record['error']=repr(e);save();raise

