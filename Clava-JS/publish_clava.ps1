
npm run build-interfaces 
npm run build 
npm run java-dist 

cd .. 
cd ClavaWeaver 
gradle installDist 
cd .. 
cd Clava-JS 

$source = "../ClavaWeaver/build/install/ClavaWeaver/lib"
$destination = "./java-binaries"

Copy-Item -Path "$source\*" -Destination $destination -Recurse -Force
