# Clava
Clava is a C/C++/CUDA/OpenCL source-to-source compiler. It applies analysis and transformations written in JavaScript scripts, and can be installed in under a minute.

If you have used Clava, please consider filling the [Clava User Experience Feedback form](https://forms.gle/SioZSAv1KL7XpQ5j6) (it's short, really!)

Clava also supports LARA files (`.lara`), but the LARA DSL is now considered legacy, please use JavaScript instead. 

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


# Installing

A ZIP file with the last compiled JAR for Clava can be downloaded from [here](http://specs.fe.up.pt/tools/clava.zip) ([previous releases](https://drive.google.com/drive/folders/1X3JeYB783ZfqoqIYrCitqABDWhM5bV_Y?usp=sharing)).

There is an [installation script](http://specs.fe.up.pt/tools/clava/clava-update) for Linux that can be run locally and that will install the Clava binary in the script path (sudo is required for CMake module installation).

For an online demo version of Clava, please click [here](http://specs.fe.up.pt/tools/clava/).

To call Clava from within CMake, please click [here](https://github.com/specs-feup/clava/tree/master/CMake).

To build Clava, please check the [ClavaWeaver](https://github.com/specs-feup/clava/tree/master/ClavaWeaver) project folder.


# Running Clava


Clava can be run from CMake, using a GUI, or as a command-line tool.


## CMake

Instructions for the Clava CMake plugin can be found [here](https://github.com/specs-feup/clava/tree/master/CMake).



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


## Server Mode

For short executions, most of the execution time is related to starting a JVM.

There is an experimental feature where you can call Clava in server mode. In this mode, Clava can run in the background and will wait for requests. Then you can use a compiled program that performs requests to the server.

To start Clava in server mode, use the flag -server:

	java -jar Clava.jar -server

The Linux instalation script provides the `clavaw` program, which sends a request to a Clava server:

        clavaw <Clava flags>

### Limitations

- Only absolute paths are supported in this mode, since the server will use its own folder as the working folder. However, files defined in config files should be handled automatically, they are converted to absolute paths, using the folder of the config file as the base folder. Additionally, if inside a Lara script you need to use files, you can use `Clava.getData().getContextFolder()` (`import clava.Clava;`) to return the folder of the config file.
- The output of the program will appear on the terminal of the server, not where `clavaw` runs-


# Troubleshooting

## Error 'Invalid or corrupt jarfile'

Check if you have at least Java 11 installed, this is the minimum version.

## Error 'missing libtinfo.so.5'

On Fedora >28 systems this can be solved by installing the package `ncurses-compat-libs`

## Error 'cannot find -lz'

Install libz package (e.g., in Ubuntu `sudo apt-get install libz-dev`)


# Citing Clava

If you want to reference Clava in your work, please use the following publication:

*João Bispo, and João MP Cardoso. Clava: C/C++ source-to-source compilation using LARA. SoftwareX, Volume 12, 2020, Article 100565.* [[ScienceDirect](https://www.sciencedirect.com/science/article/pii/S2352711019302122)] [[bibtex](http://specs.fe.up.pt/tools/clava/clava_softwarex2020.bib)]

# Acknowledgments

This work has been partially funded by the [ANTAREX project](http://antarex-project.eu) through the EU H2020 FET-HPC program under grant no. 671623. João Bispo acknowledges the support provided by Fundação para a Ciência e a Tecnologia, Portugal, under Post-Doctoral grant SFRH/BPD/118211/2016.

# Contacts

[João Bispo](mailto://jbispo@fe.up.pt)
