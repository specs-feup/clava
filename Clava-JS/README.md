# Clava-JS

Clava source-to-source compiler running on top of Node.js.

## Installing dev environment

Execute the following commands to build Clava-JS in a folder called `workspace`:

```bash
mkdir workspace
cd workspace
touch package.json
git clone https://github.com/specs-feup/specs-java-libs.git
git clone https://github.com/specs-feup/lara-framework.git
git clone https://github.com/specs-feup/clava.git
```

In the `workspace` directory, edit the `package.json` file and add the following:

```json
{
  "type": "module",
  "workspaces": [
    "clava/Clava-JS",
    "lara-framework/Lara-JS"
  ]
}
```

Starting from the `workspace` directory, execute the following commands to build Clava-JS:

```bash
npm install
npm run build -w lara-framework/Lara-JS
npm run build -w clava/Clava-JS
npm install
cd clava/ClavaWeaver
gradle installDist
cd ../..
```

Finally, copy the JARs in the folder `./clava/ClavaWeaver/build/install/ClavaWeaver/lib` into a new folder called java-binaries in `./clava/Clava-JS`:

```bash
mkdir clava/Clava-JS/java-binaries
cp -r ./clava/ClavaWeaver/build/install/ClavaWeaver/lib ./clava/Clava-JS/java-binaries
```

Install the package globally:

```bash
npm install -g @specs-feup/clava
```

It should now be available as a command in the terminal:

```bash
npx clava classic <your CLI options, pass a non-existing flag, such as -dummy, to check the options>
```

If you want to reflect local changes in Clava-JS (or Lara-JS) in the installed command, use the `link` option:

```bash
npm link @specs-feup/clava
```

## Executing Clava-JS

You can execute Clava-JS by running the following on your terminal

```bash
npx clava classic <scriptfile.js> -p "<c++ files or folders>"
```

Additionally, if you would like to see the help menu

```bash
npx clava --help
```

or run in watch mode

```bash
npx clava classic <scriptfile.js> -w <directory/file to watch> -c <clava config file>
```

To create a Clava config file, launch the Java-based GUI:

```bash
./clava/ClavaWeaver/build/install/ClavaWeaver/bin/ClavaWeaver
```

## Debugging

You can get debugging information using a `DEBUG` environment variable.
This variable is used by the [debug](https://www.npmjs.com/package/debug) module to determine what to expose.

```bash
DEBUG="*" npx clava classic <scriptfile.js> <your CLI options>
```

## CMake

Clava has a [CMake package](https://github.com/specs-feup/clava/tree/staging/CMake), check the link for more details on how to use it.
