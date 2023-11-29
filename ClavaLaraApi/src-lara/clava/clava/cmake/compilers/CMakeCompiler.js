/**
 * Interface that provides information about a CMake-supported compiler.
 *
 */
export default class CMakeCompiler {
    /**
     * Generates the compiler-related arguments that are required when calling the CMake command.
     */
    getCommandArgs() {
        return `-DCMAKE_CXX_COMPILER=${this.getCxxCommand()} -DCMAKE_C_COMPILER=${this.getCCommand()}`;
    }
}
//# sourceMappingURL=CMakeCompiler.js.map