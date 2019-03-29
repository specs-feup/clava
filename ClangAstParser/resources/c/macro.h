#define declInt(x,y)    int x = y;

#define double_add(a,b)   (double){(a)+(b)}

#define data_type double

#define custom_size(type) sizeof(type)
//#define type_decl(var, type)	\
//  type var;

#define EXTERN_DECL(type, array)  extern type array[]

// These declarations will all have the same source location, 
// comments will not be aligned with declarations
EXTERN_DECL(int, extern_a); /* comment after extern_a */
EXTERN_DECL(int, extern_b); // comment after extern_b
EXTERN_DECL(int, extern_c);          
EXTERN_DECL(int, extern_d);   

#ifndef MACRO_TYPE
#  define MACRO_TYPE void
#endif

typedef MACRO_TYPE (*new_type) (int);

