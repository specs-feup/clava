<p align="center">
  <img src="docs/assets/clava.png" alt="clava logo" style="width:30%;">
</p>

Clava is a C/C++/CUDA/OpenCL source-to-source compiler, applying analysis and transformations written in TypeScript/JavaScript with a focus on composability and reusability. It is currently supported on Ubuntu, Windows and MacOS.

## Installation

There are three distributions of Clava:
- an [NPM package](https://www.npmjs.com/package/@specs-feup/clava) distribution (recommended),
- a [JAR file](http://specs.fe.up.pt/tools/clava.zip) distribution (legacy),
- and an [online demo](https://specs.fe.up.pt/tools/clava/) (for demonstration purposes only)

### NPM distribution (recommended)

Clava is currently distributed as an [NPM package](https://www.npmjs.com/package/@specs-feup/clava). It requires Node.js 20 or 22, and Java 17 or higher. Different OSses have different ways of installing these dependencies, but on Ubuntu you can run this:

```bash
apt-get update && apt-get install -y curl openjdk-17-jdk
curl -fsSL https://deb.nodesource.com/setup_22.x | bash - && apt-get install -y nodejs
```

Now, you have two options:

#### a. Clava as a standalone, system-level application

Choose this option if you just want to use Clava as-is. You can install it globally by running:

```bash
npm install -g @specs-feup/clava@latest
clava help # check if it was installed correctly 
```

To use Clava with the currently supported CLI you must use the command `clava classic`. You can check if this works by running: 

```bash
clava classic --help # check if CLI is supported 
```

#### b. Clava as a dependency of an NPM project

Choose this option if you want to use any of the several Clava extensions available, or write your own analysis and transformation scripts:

1. Clone the [Clava project template repository](https://github.com/specs-feup/clava-project-template), or create your own git repository using it as a template.
2. Open a terminal on that directory, and run the following:

```bash
npm install
npm run build
npx run run # run the hello world example that comes with the template
```

3. (optional) Add Clava packages to your project, e.g., `npm install @specs-feup/clava-code-transforms@latest`

Check [specs-feup NPM page](https://www.npmjs.com/org/specs-feup) to see what packages are available for Clava (they should have a `clava-` prefix).

See the [README](https://github.com/specs-feup/clava-project-template/blob/main/README.md) included in the template for further instructions on running, testing, automatic building, etc.

### JAR file distribution (legacy)

A Clava version based on LARA 2.x is available as a JAR file for legacy purposes, requiring Java 17 or higher. You can [download it directly](http://specs.fe.up.pt/tools/clava.zip), or you can run a script to install it at the system level:

* [Linux](https://raw.githubusercontent.com/specs-feup/clava/refs/heads/master/install/linux/clava-update)
* [Windows](https://raw.githubusercontent.com/specs-feup/clava/refs/heads/master/install/windows/clava-install.cmd)

### Online demo (demonstration only)

You can [use this online demo](https://specs.fe.up.pt/tools/clava/) to try out Clava without needing to install it.

Warning: currently the demo only supports legacy mode, we are currently working to support the recommended NPM mode.

## Using Clava

This is a non-exhaustive list of Clava documentation, usage examples and features:

* [API Documentation](https://specs-feup.github.io/modules/_specs_feup_clava.html) - up-to-date documentation of all Clava joinpoints, attributes and methods
* Getting started:
  * [Usage Examples](https://github.com/specs-feup/clava/tree/master/ClavaWeaver/resources/clava/test)
  <!--* [Clava Online Demo Tutorial (2022)](https://specs.fe.up.pt/tools/clava/Clava%20Web-based%20Tutorial%20(2022).pdf) -->
  * [Clava Libraries Showcase (FCCM 2025 Demo Night)](https://github.com/specs-feup/clava-fccm-2025-demo)
  * [Clava API Slides](https://drive.google.com/drive/u/1/folders/1IAqv7SpP8S-t5g3fpNO06cJ7J2j2aD7K) - Introduction to Clava and the LARA Framework APIs.
* Clava built-in features:
  * [CMake integration](https://github.com/specs-feup/clava/tree/master/CMake) - Allows Clava to be used in CMake-centered compilation flows
  * Code transformations:
    * [Automatic insertion of OpenMP pragmas](https://github.com/specs-feup/clava/blob/master/ClavaLaraApi/src-lara-clava/clava/clava/autopar/Parallelize.lara)
    * [Function inlining](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/opt/Inlining.ts)
    * [Normalizing code](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/opt/NormalizeToSubset.ts) to a subset of the language, including:
      * [Decomposition of complex statements into several, simpler statements](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/code/StatementDecomposer.ts)
      * [Converting static local variables to static global variables](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/pass/LocalStaticToGlobal.ts)
      * [Conversion of switch statements to ifs](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/pass/TransformSwitchToIf.ts)
      * Loop conversion ([for to while](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/code/ForToWhileStmt.ts), [do to while](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/code/DoToWhileStmt.ts))
      * [Ensure there is a single return in a function](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/pass/SingleReturnFunction.ts)
      * [Remove variable shadowing](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/code/RemoveShadowing.ts)
      * [Simplify ternary operator](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/code/SimplifyTernaryOp.ts)
      * [Simplify compound assignments](https://github.com/specs-feup/clava/blob/master/Clava-JS/src-api/clava/code/SimplifyAssignment.ts)
* Clava [NPM libraries](https://www.npmjs.com/org/specs-feup) (not supported by the JAR file legacy distribution):

Library | Description | Installation
---|---|---
[clava-code-transforms](github.com/specs-feup/clava-code-transforms) | A set of advanced code transformations for Clava (function outlining and voidification, array and struct flattening, constant folding and propagation, loop static iteration annotation) | `npm i @specs-feup/clava-code-transforms@latest`
[clava-misra](github.com/specs-feup/clava-misra) | Automatic detection and correction of C code patterns that violate the MISRA-C:2012 coding standard | `npm i @specs-feup/clava-misra@latest`
[clava-visualization](github.com/specs-feup/clava-visualization)  | Renders an interactive web view of the program's AST, in a breakpoint-like fashion | `npm i @specs-feup/clava-visualization@latest`
[clava-vitis-integration](github.com/specs-feup/clava-vitis-integration) | Integration with Vitis HLS, allowing for the automated synthesis and place-and-route of selected functions, and the programatic insertion and configurations of HLS directives | `npm i @specs-feup/clava-vitis-integration@latest`
[extended-task-graph](github.com/specs-feup/extended-task-graph) | Generates an Extended Task Graph (ETG) representation of an application, with flexible granularity changes through task merging and splitting |`npm i @specs-feup/extended-task-graph@latest`
[flow](github.com/specs-feup/flow) | Control-flow, Data-flow, static-call, and other graphs | `npm i @specs-feup/flow@latest`
[hoopa](github.com/specs-feup/hoopa) | Hoopa (<u>Ho</u>listic <u>o</u>ptimization and <u>p</u>artitioning <u>a</u>lgorithms) is a collection of HW/SW partitioning policies and algorithms that can partition and optimize an application for a CPU-FPGA system | `npm i @specs-feup/hoopa@latest`
[onnx-flow](github.com/specs-feup/onnx-flow)  | Converts an ONNX graph into a data-flow graph (DFG), decomposing its high-level operations into low-level operations and performing a set of optimizations | `npm i @specs-feup/onnx-flow@latest`

## Troubleshooting

* First, check if your problem is part of the [common issues list](docs/common_issues.md)
* If not, check if it has been reported on the [issue tracker](https://github.com/specs-feup/clava/issues)
* If not, then please [create an issue](https://github.com/specs-feup/clava/issues/new/choose) or otherwise [let us know directly](mailto:jbispo@fe.up.pt)

## Research Papers

If you found Clava useful, please consider citing it as follows:

```latex
@article{BISPO2020100565,
    title = {Clava: C/C++ source-to-source compilation using LARA},
    journal = {SoftwareX},
    volume = {12},
    pages = {100565},
    year = {2020},
    issn = {2352-7110},
    doi = {https://doi.org/10.1016/j.softx.2020.100565},
    url = {https://www.sciencedirect.com/science/article/pii/S2352711019302122},
    author = {João Bispo and João M.P. Cardoso},
    keywords = {Source-to-source, C/C++, LARA, Compilers}
}
```

Since the publication of the paper above, Clava has been used by these papers:

Year | Paper | Cite
---|---|---
2024 | T. Silva, P. G. Correia, L. Sousa, J. Bispo and T. Carvalho. 2024. Towards a Rust-Like Borrow Checker for C. ACM Trans. Embed. Comput. Syst. Just Accepted (October 2024). <https://doi.org/10.1145/3702229> | [[BibTex]](docs/citations/ecs2024silva.bib)
2024 | T. Santos, J. Bispo and J. M. P. Cardoso. 2024. A Flexible-Granularity Task Graph Representation and Its Generation from C Applications (WIP). In Proceedings of the 25th ACM SIGPLAN/SIGBED International Conference on Languages, Compilers, and Tools for Embedded Systems (LCTES 2024). Association for Computing Machinery, New York, NY, USA, 178–182. <https://doi.org/10.1145/3652032.3657580> | [[BibTex]](docs/citations/lctes2024santos.bib)
2024 | T. Silva, J. Bispo and T. Carvalho. 2024. Foundations for a Rust-Like Borrow Checker for C. In Proceedings of the 25th ACM SIGPLAN/SIGBED International Conference on Languages, Compilers, and Tools for Embedded Systems (LCTES 2024). Association for Computing Machinery, New York, NY, USA, 155–165. <https://doi.org/10.1145/3652032.3657579> | [[BibTex]](docs/citations/lctes2024silva.bib)
2024 | J. N. Matos, J. Bispo and L. M. Sousa. 2024. A C Subset for Ergonomic Source-to-Source Analyses and Transformations. In Proceedings of the 16th Workshop on Rapid Simulation and Performance Evaluation for Design (RAPIDO '24). Association for Computing Machinery, New York, NY, USA, 1–8. <https://doi.org/10.1145/3642921.3642922> | [[BibTex]](docs/citations/rapido2024matos.bib)
2023 | T. Santos, J. Bispo and J. M. P. Cardoso, "A CPU-FPGA Holistic Source-To-Source Compilation Approach for Partitioning and Optimizing C/C++ Applications," 2023 32nd International Conference on Parallel Architectures and Compilation Techniques (PACT), Vienna, Austria, 2023, pp. 320-322. <https://doi.org/10.1109/PACT58117.2023.00034> | [[BibTex]](docs/citations/pact2023santos.bib)
2023 | L. M. Sousa, J. Bispo and N. Paulino, "Retargeting Applications for Heterogeneous Systems with the Tribble Source-to-Source Framework," 2023 32nd International Conference on Parallel Architectures and Compilation Techniques (PACT), Vienna, Austria, 2023, pp. 329-331. <https://doi.org/10.1109/PACT58117.2023.00038> | [[BibTex]](docs/citations/pact2023sousa.bib)
2023 | J. Bispo, N. Paulino and L. M. Sousa. Challenges and Opportunities in C/C++ Source-To-Source Compilation (Invited Paper). In 14th Workshop on Parallel Programming and Run-Time Management Techniques for Many-Core Architectures and 12th Workshop on Design Tools and Architectures for Multicore Embedded Computing Platforms (PARMA-DITAM 2023). Open Access Series in Informatics (OASIcs), Volume 107, pp. 2:1-2:15, Schloss Dagstuhl – Leibniz-Zentrum für Informatik (2023) <https://doi.org/10.4230/OASIcs.PARMA-DITAM.2023.2> | [[BibTex]](docs/citations/parmaditam2023bispo.bib)
2022 | L. M. Sousa, N. Paulino, J. C. Ferreira and J. Bispo, "A Flexible HLS Hoeffding Tree Implementation for Runtime Learning on FPGA," 2022 IEEE 21st Mediterranean Electrotechnical Conference (MELECON), Palermo, Italy, 2022, pp. 972-977, <https://doi.org/10.1109/MELECON53508.2022.9843092> | [[BibTex]](docs/citations/melecon2022sousa.bib)
2021 | P. Pinto and J. M. P. Cardoso. 2021. A methodology and framework for software memoization of functions. In Proceedings of the 18th ACM International Conference on Computing Frontiers (CF '21). Association for Computing Machinery, New York, NY, USA, 93–101. <https://doi.org/10.1145/3457388.3458668> | [[BibTex]](docs/citations/cf2021pinto.bib)
2020 | T. Santos and J. M. P. Cardoso, "Automatic Selection and Insertion of HLS Directives Via a Source-to-Source Compiler," 2020 International Conference on Field-Programmable Technology (ICFPT), Maui, HI, USA, 2020, pp. 227-232. <https://doi.org/10.1109/ICFPT51103.2020.00039> | [[BibTex]](docs/citations/fpt2020santos.bib)
2020 | H. Arabnejad, J. Bispo, J. M. P. Cardoso and J. G. Barbosa, Source-to-source compilation targeting OpenMP-based automatic parallelization of C applications. J Supercomput 76, 6753–6785 (2020). <https://doi.org/10.1007/s11227-019-03109-9> | [[BibTex]](docs/citations/super2020arabnejad.bib)

## Acknowledgments

- This work has been partially funded by the [ANTAREX](https://antarex.fe.up.pt) project through the EU H2020 FET-HPC program under grant no. 671623.
- João Bispo acknowledges the support provided by Fundação para a Ciência e a Tecnologia, Portugal, under Post-Doctoral grant SFRH/BPD/118211/2016.
- Luís Sousa acknowledges the support provided by Fundação para a Ciência e a Tecnologia, Portugal, under Doctoral grant 2022.10002.BD.

## Contacts

[João Bispo](mailto:jbispo@fe.up.pt)
