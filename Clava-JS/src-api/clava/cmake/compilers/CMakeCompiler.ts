/**
 * Interface that provides information about a CMake-supported compiler.
 *
 */
export default abstract class CMakeCompiler {
  abstract getCxxCommand(): string;

  abstract getCCommand(): string;

  /**
   * Generates the compiler-related arguments that are required when calling the CMake command.
   */
  getCommandArgs() {
    return `-DCMAKE_CXX_COMPILER=${this.getCxxCommand()} -DCMAKE_C_COMPILER=${this.getCCommand()}`;
  }
}
