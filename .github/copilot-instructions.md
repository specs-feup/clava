# Copilot Instructions for the Clava Repository

## Project Overview

Clava is a modular source-to-source compiler for C, C++, CUDA, and OpenCL, supporting advanced code analysis and transformation. It is implemented using a combination of TypeScript/JavaScript (Bun.sh), Java, and C++. Clava is designed for composability and reusability, and integrates with the LARA DSL for custom code transformations.

## Development Environment & Setup

- **Bun.sh Version:** Any version
- **Java Version:** 17+ (required for Java components)
- **Build System:** Gradle for Java modules, Bun for TypeScript/JavaScript
- **IDE:** VSCode is recommended for development

## Architecture

- **Frontend (C++):**  
  The `ClangAstDumper` component extracts AST information from Clang-based codebases.
- **Middle-end (Java):**  
  Components like `ClangAstParser` and `ClavaWeaver` process ASTs and apply transformations.
- **API Layer (TypeScript/JavaScript):**  
  The `Clava-JS` module provides the main user-facing API and runtime, exposing Clava's features to Bun's environments.
- **Build Integration:**  
  The `CMake` package enables integration with CMake-based build systems.

### Related Projects
- **lara-framework**: Core framework providing weaver infrastructure and JavaScript APIs
- **specs-java-libs**: Java utility libraries used throughout the project

## Key Directories

- `Clava-JS/`: TypeScript/JavaScript API and runtime.
- `ClavaWeaver/`: Java-based weaving engine.
- `ClangAstDumper/`: C++ AST dumper using Clang.
- `ClangAstParser/`: Java AST parser.
- `ClavaAst/`, `ClavaHls/`, `ClavaLaraApi/`, `AntarexClavaApi/`: Supporting modules for AST, HLS, LARA API, and Antarex integration.
- `CMake/`: CMake integration scripts and utilities.
- `docs/`: Documentation, tutorials, and common issues.

## Build and Development

- **Java Components:** Use Gradle (`gradle installDist`) to build Java modules (e.g., ClavaWeaver).
- **TypeScript/JavaScript:** Use bun scripts (`bun install`, `bun run build`) in `Clava-JS`.
- **C++ Components:** Use CMake for building and integrating the Clang AST dumper.
- **Integration:** Copy built Java binaries into `Clava-JS/java-binaries` for full functionality.

## Usage

- **NPM Package:**  
  Install globally or as a project dependency:  
  `bun install -g @specs-feup/clava`
- **CLI:**  
  Run transformations via `bunx clava classic <scriptfile.js> -p "<source files>"`
- **CMake Integration:**  
  Use the `clava_weave` CMake command to apply LARA scripts to targets.

## Code Patterns and Conventions

- **Visitor Patterns:**  
  Used extensively in AST processing (see `ClangAstDumper.h`).
- **TypeScript API:**  
  Modular, with clear separation between API (`api/`) and code (`code/`).
- **Java:**  
  Follows standard Gradle project structure.
- **C++:**  
  Integrates with Clang/LLVM for AST extraction.

## Common Development Tasks

- Add new AST node support in `ClangAstDumper` and propagate through Java and JS layers.
- Extend the TypeScript API in `Clava-JS/api/`.
- Create new code transformations as LARA scripts or TypeScript modules.
- Use provided test scripts and Bun/Gradle test commands.

## Dependencies

- **Bun.sh** and **Java 17+** required.
- **Clang/LLVM** for AST extraction.
- **Bun** for JS/TS dependencies.
- **Gradle** for Java builds.
- **CMake** for build system integration.

## Troubleshooting

- See `docs/common_issues.md` for frequently encountered problems.
- Use the GitHub issue tracker for unresolved issues.

## References

- [Clava Documentation](https://specs-feup.github.io/modules/_specs_feup_clava.html)
- [Clava Project Template](https://github.com/specs-feup/clava-project-template)
- [Online Demo](https://specs.fe.up.pt/tools/clava/)
- [Main Repository](https://github.com/specs-feup/clava)

---

**For LLMs:**  
- Respect the modular structure and language boundaries.
- When adding features, ensure changes propagate through C++, Java, and JS layers as needed.
- Follow existing patterns for AST traversal and transformation.
- Use the provided build and test scripts for validation.
