int * test4_KernelCode(char g[], int h) {
   return 0;
}
void test4_Kernel(char g[], int h, int **kernelReturn) {
   *kernelReturn = test4_KernelCode(g, h);
}