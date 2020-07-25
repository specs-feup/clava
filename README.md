# Clava
Clava is a C/C++ source-to-source compiler. It applies analysis and transformations written in Javascript-like scripts, and can be installed in under a minute.


# Resources

A ZIP file with the compiled JAR for Clava can be downloaded from [here](http://specs.fe.up.pt/tools/clava.zip).

There is an [installation script](http://specs.fe.up.pt/tools/clava/clava-update) for Linux that can be run locally and that will install the Clava binary in the script path (sudo is required for CMake module installation).

For an online demo version of Clava, please click [here](http://specs.fe.up.pt/tools/clava/).

To call Clava from within CMake, please click [here](https://github.com/specs-feup/clava/tree/master/CMake).

For a tutorial on how to use Clava, please click [here](https://github.com/specs-feup/specs-lara/tree/master/2018-PACT).

For a sample of Clava examples, please click [here](https://github.com/specs-feup/clava-examples/).

To build Clava, please check the [ClavaWeaver](https://github.com/specs-feup/clava/tree/master/ClavaWeaver) project folder.


# Troubleshooting

## Error 'Invalid or corrupt jarfile'

Check if you have at least Java 11 installed, this is the minimum version.

## Error 'missing libtinfo.so.5'

On Fedora >28 systems this can be solved by installing the package `ncurses-compat-libs`





# acknowledgments

This work has been partially funded by the [ANTAREX project](http://antarex-project.eu) through the EU H2020 FET-HPC program under grant no. 671623. João Bispo acknowledges the support provided by Fundação para a Ciência e a Tecnologia, Portugal, under Post-Doctoral grant SFRH/BPD/118211/2016.
