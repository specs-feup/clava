struct struct1 {
	int s1a;
};

struct struct1 struct1_var;

struct struct2 {
	int b;
} struct2_var;

struct struct3 {
	int c;
} struct3_var1, struct3_var2;

struct struct4 {
	int c2;
} struct4_var1;

int anInt;

struct struct4 struct4_var2;

union union1 {
	int d;
} d_var;


enum enum1 {
	A,
	B
} enum_var;

struct foo {
    struct {
        int x;
    } a;
};

typedef struct
{
    double real1;
    double imag1;
} struct_with_typedef_var;

typedef struct
{
    double real2;
    double imag2;
};

typedef enum enum_with_typedef {
	C,
	D
} enum_with_typedef_var;
