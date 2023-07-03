# Clava-JS

A rewrite of the Clava source-to-source compiler in JS for better compatibility.

## Installing dev environment

Execute the following commands to download all the required code:

```bash
mkdir workspace
cd workspace
touch package.json
git clone https://github.com/lm-sousa/Clava-JS.git
git clone https://github.com/lm-sousa/clavaAPI.git
git clone https://github.com/specs-feup/lara-framework.git
```

In the `workspace` directory, edit the `package.json` file and add the following:

```json
{
  "type": "module",
  "workspaces": [
    "Clava-JS",
    "clavaAPI"
    "lara-framework/Lara-JS",
  ]
}
```

Finally, starting from the `workspace` directory, execute the following commands:

```bash
npm install
cd lara-framework/Lara-JS
npm run build
cd ../../clavaAPI
npm run build
cd ../Clava-JS
npm run build
```

## Executing Clava-JS

You can execute Clava-JS by running the following on your terminal

```bash
npm run run -- <scriptfile.js> -- clang++ <c++ files>
```

Additionally, if you would like to see the help menu

```bash
npm run run -- --help
```

or run in watch mode

```bash
npm run run -- <scriptfile.js> -w <directory/file to watch> -- clang++ <c++ files>
```

## Debugging

You can get debugging information using a `DEBUG` environment variable.
This variable is used by the [debug](https://www.npmjs.com/package/debug) module to determine what to expose.

```bash
DEBUG="*" npm run run -- <scriptfile.js> -- clang++ <c++ files>
```
