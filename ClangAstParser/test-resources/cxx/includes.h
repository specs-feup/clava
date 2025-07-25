#ifndef INCLUDES
#define INCLUDES

#include "includes2.h"

#define MACRO_1 1
#define MACRO_1_1 MACRO_2

#define DECL_1 int decl1 = 2
#define DECL_1_1 DECL_2

#define DECL_1_FULL int decl1_full = 3;

#define DECL_1_1_FULL DECL_2_FULL 

extern int includes1;

extern "C" void foo();

#endif