# Clava
Clava is a C/C++ source-to-source compiler. It applies analysis and transformations written in LARA scripts (which is based on JavaScript), and can be installed in under a minute.

If you have used Clava, please consider filling the [Clava User Experience Feedback form](https://forms.gle/SioZSAv1KL7XpQ5j6) (it's short, really!)

# Quickstart

Tutorial on how to use Clava: [Clava Tutorial - 2018 PACT](https://github.com/specs-feup/specs-lara/tree/master/2018-PACT).

Start using LARA with examples: [LARA Reference Guide](http://specs.fe.up.pt/tools/lara/doku.php?id=lara:docs:sheet).

For a sample of Clava examples, please click [here](https://github.com/specs-feup/clava-examples/).


# Resources

A ZIP file with the compiled JAR for Clava can be downloaded from [here](http://specs.fe.up.pt/tools/clava.zip).

There is an [installation script](http://specs.fe.up.pt/tools/clava/clava-update) for Linux that can be run locally and that will install the Clava binary in the script path (sudo is required for CMake module installation).

For an online demo version of Clava, please click [here](http://specs.fe.up.pt/tools/clava/).

To call Clava from within CMake, please click [here](https://github.com/specs-feup/clava/tree/master/CMake).

To build Clava, please check the [ClavaWeaver](https://github.com/specs-feup/clava/tree/master/ClavaWeaver) project folder.


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
