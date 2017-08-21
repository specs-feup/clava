#include <stdio.h>
#include <stdarg.h>

void test_print(char * fmt, ...) {
   va_list argptr;
   __builtin_va_start(argptr, fmt);
   __builtin_va_end(argptr);
   printf("%s", fmt);
}


int main() {
   int a = 0;
   printf("Hello:%d/n", a);
   test_print("Test/n");
   
   return 0;
}
