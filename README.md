# Clava
Clava is a C/C++/CUDA/OpenCL source-to-source compiler. It applies analysis and transformations written in TypeScript/JavaScript scripts.

If you have used Clava, please consider filling the [Clava User Experience Feedback form](https://forms.gle/SioZSAv1KL7XpQ5j6) (it's short, really!)

# Installing

Clava is currently distributed as an [NPM package](https://www.npmjs.com/package/@specs-feup/clava). Please check the [Clava template repository](https://github.com/specs-feup/clava-project-template) for instructions on how to run Clava.

To call Clava from within CMake, please click [here](https://github.com/specs-feup/clava/tree/master/CMake).

To build the Java part of Clava, please check the [ClavaWeaver](https://github.com/specs-feup/clava/tree/master/ClavaWeaver) project folder. The JAR can be used to generate configuration files.


# Compatibility

Currently Clava requires Java 17 and Node 18 or 20.

Although Clava is mainly a Java program, which should run on any compatible Java runtime, it also uses compiled binaries. These have operating system restrictions. Currently Clava is supported in Windows 10, Ubuntu 20.04, CentOS 7 and MacOS 11 (Big Sur).

# Quickstart

<!--
[Clava Tutorial - 2018 PACT](https://github.com/specs-feup/specs-lara/tree/master/2018-PACT) - Tutorial on how to use Clava (recommended parts 1-3).
-->

<!--
[LARA Reference Guide](https://web.fe.up.pt/~specs/wiki/doku.php?id=doc:lara) - Start using LARA with examples.
-->

[Clava Web-based Tutorial - 2022](https://specs.fe.up.pt/tools/clava/Clava%20Web-based%20Tutorial%20(2022).pdf) - Tutorial on how to use Clava, based on the online demo version.

[Language Specification](https://specs.fe.up.pt/tools/clava/language_specification.html) - List of all join points, attributes and actions available in Clava.

[API Documentation](https://specs-feup.github.io/clava/api/) - List of LARA APIs available in Clava.

[API Slides](https://drive.google.com/drive/u/1/folders/1IAqv7SpP8S-t5g3fpNO06cJ7J2j2aD7K) - Introduction to Clava and the LARA Framework APIs.

For a sample of Clava examples, please check the [Unit Tests](https://github.com/specs-feup/clava/tree/master/ClavaWeaver/resources/clava/test).

# Sample of supported transformations

Clava already supports the following transformations:

- [Automatic insertion of OpenMP pragmas](https://github.com/specs-feup/clava/blob/master/ClavaLaraApi/src-lara-clava/clava/clava/autopar/Parallelize.lara)
- [Function inlining](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/opt/Inlining.ts)
- [Function outlining](https://github.com/tiagolascasas/clava-code-transforms/blob/master/src/Outliner.ts)
- [Array flattening](https://github.com/tiagolascasas/clava-code-transforms/blob/master/src/ArrayFlattener.ts)
- [Constant folding and propagation](https://github.com/tiagolascasas/clava-code-transforms/blob/master/src/constfolding/FoldingPropagationCombiner.ts)
- [Struct decomposition](https://github.com/tiagolascasas/clava-code-transforms/blob/master/src/StructDecomposer.ts)
- [Function voidification](https://github.com/tiagolascasas/clava-code-transforms/blob/master/src/Voidifier.ts)
- [Normalizing code](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/opt/NormalizeToSubset.ts) to a subset of the language, including:
  - [Decomposition of complex statements into several, simpler statements](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/code/StatementDecomposer.ts)
  - [Converting static local variables to static global variables](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/pass/LocalStaticToGlobal.ts)
- [Conversion of switch statements to ifs](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/pass/TransformSwitchToIf.ts)
- Loop conversion ([for to while](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/code/ForToWhileStmt.ts), [do to while](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/code/DoToWhileStmt.ts))
- [Ensure there is a single return in a function](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/pass/SingleReturnFunction.ts)
- [Remove variable shadowing](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/code/RemoveShadowing.ts)
- [Simplify ternary operator](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/code/SimplifyTernaryOp.ts)
- [Simplify compound assignments](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/code/SimplifyAssignment.ts)

<!--
# NPM Development Flow

When adding new join points and attributes, besides the weaver generator (Java side), it is also necessary to run the join point generator (TS side). To do this, execute `npm run build-interfaces` on Clava-JS folder.
-->


<!--
A ZIP file with the last compiled JAR for Clava can be downloaded from [here](http://specs.fe.up.pt/tools/clava.zip) ([previous releases](https://drive.google.com/drive/folders/1X3JeYB783ZfqoqIYrCitqABDWhM5bV_Y?usp=sharing)).

There is an [installation script](http://specs.fe.up.pt/tools/clava/clava-update) for Linux that can be run locally and that will install the Clava binary in the script path (sudo is required for CMake module installation).

For an online demo version of Clava, please click [here](http://specs.fe.up.pt/tools/clava/).

-->

<!--
## CMake

Instructions for the Clava CMake plugin can be found [here](https://github.com/specs-feup/clava/tree/master/CMake).
-->

<!--
## GUI


Run the JAR with passing parameters, e.g.:

	java -jar Clava.jar


A video demonstrating the GUI can be found [here](https://www.youtube.com/watch?v=IFvNWYCivFA).

## Command Line


There are two main modes in command line, either passing all arguments (LARA file, parameters, etc...), or passing a configuration file that was built with the graphical user interface.



### Using parameters:

	java -jar Clava.jar <aspect.lara> -p <source_folder>

where <aspect.lara> is the LARA aspect you want to execute, and <source_folder> is the folder where the source code is.


There are more command-line options available, which can be consulted by running:

	java -jar Clava.jar --help


		
### Configuration file:

To pass a configuration file, use the flag -c:

	java -jar Clava.jar -c <config.clava>

where <config.clava> is the configuration file created with the GUI.
-->




# Troubleshooting

## Error 'Invalid or corrupt jarfile'

Check if you have at least Java 17 installed, this is the minimum version.

## Error 'missing libtinfo.so.5'

On Fedora >28 systems this can be solved by installing the package `ncurses-compat-libs`

## Error 'cannot find -lz'

Install libz package (e.g., in Ubuntu `sudo apt-get install libz-dev`)


# Citing Clava

If you want to reference Clava in your work, please use the following publication:

*João Bispo, and João MP Cardoso. Clava: C/C++ source-to-source compilation using LARA. SoftwareX, Volume 12, 2020, Article 100565.* [[ScienceDirect](https://www.sciencedirect.com/science/article/pii/S2352711019302122)] [[bibtex](http://specs.fe.up.pt/tools/clava/clava_softwarex2020.bib)]

# Acknowledgments

This work has been partially funded by the [ANTAREX](https://antarex.fe.up.pt) project through the EU H2020 FET-HPC program under grant no. 671623. João Bispo acknowledges the support provided by Fundação para a Ciência e a Tecnologia, Portugal, under Post-Doctoral grant SFRH/BPD/118211/2016.

# Contacts

[João Bispo](mailto:jbispo@fe.up.pt)
