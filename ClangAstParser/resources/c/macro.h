#define declInt(x,y)    int x = y;

#define double_add(a,b)   (double){(a)+(b)}

#define data_type double

#define custom_size(type) sizeof(type)
//#define type_decl(var, type)	\
//  type var;

#define EXTERN_DECL(type, array)  extern type array[]

// comment before extern_a
EXTERN_DECL(int, extern_a); /* comment after extern_a */
EXTERN_DECL(int, extern_b); // comment after extern_b
EXTERN_DECL(int, extern_c);          
EXTERN_DECL(int, extern_d);   

#ifndef MACRO_TYPE
#  define MACRO_TYPE void
#endif

typedef MACRO_TYPE (*new_type) (int);



#define DECL(A, VALUE) int A = VALUE;

// Comment before
DECL(global_a, 10)
// Comment After


#define A1 int global2 = 20;
#define A2 A1


// Comment 2 before
A2
// Comment 2 After


// Comment 3 before
int global3 = 30;
// Comment 3 After


#define FOO poisoned_foo
#pragma GCC poison poisoned_foo
void FOO();