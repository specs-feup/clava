
## Building Clava

Using [eclipse-build](http://specs.fe.up.pt/tools/eclipse-build.jar), copy the file clava_weaver.build to a folder and run the following command:

```
  java -jar eclipse-build.jar --config clava_weaver.build
```

This should create the file ClavaWeaver.jar. 

## Downloading Clava

Clava can be downloaded from the link specs.fe.up.pt/tools/clava.jar.



## Running Clava


Clava has two modes, command-line and GUI.


## GUI


Run the JAR with passing parameters, e.g.:

	java -jar Clava.jar



## Command Line


There are two main modes in command line, either passing all arguments (LARA file, parameters, etc...), or passing a configuration file that was built with the graphical user interface.



### Manually:

	`java -jar Clava.jar <aspect.lara> -p <source_folder>`

where <aspect.lara> is the LARA aspect you want to execute, and <source_folder> is the folder where the source code is.


There are more command-line options available, which can be consulted by running:

		`java -jar Clava.jar --help`


		
### Configuration file:

To pass a configuration file, use the flag -c:

	`java -jar Clava.jar -c <config.clava>`

where <config.clava> is the configuration file created with the GUI.

