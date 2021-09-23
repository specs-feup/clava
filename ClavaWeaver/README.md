
# Installation

## Building Clava

Download [eclipse-build](http://specs.fe.up.pt/tools/eclipse-build.jar) (source code can be found [here](https://github.com/specs-feup/specs-java-tools/tree/master/EclipseBuild)) and run the following command:

`java -jar eclipse-build.jar --config https://raw.githubusercontent.com/specs-feup/clava/master/ClavaWeaver/eclipse.build`

This should create the file ClavaWeaver.jar. 

## Downloading Clava

A zip file with a precompiled Clava JAR can be downloaded from this [link](http://specs.fe.up.pt/tools/clava.zip).

If you are using Linux there is an [instalation script](https://specs.fe.up.pt/tools/clava/clava-update). Running the script will install Clava in the folder where the script is, e.g. running the script from /usr/local/bin should make the command `clava` available system-wide. If you run the script with `sudo`, it will install the Clava package for CMake.

## Clava Online Demo Version

There is a demo online version of Clava available [here](http://specs.fe.up.pt/tools/clava).


# configuring eclipse

Please check the README file in repository [specs-java-libs](https://github.com/specs-feup/specs-java-libs).



