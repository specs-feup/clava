
struct struct_test {
	int struct_field_1;
	char struct_field_2;
};

struct struct_test_with_var {
	int struct_field_1;
	char struct_field_2;
} struct_test_var;

typedef struct struct_test_with_typedef {
	int struct_field_1;
	char struct_field_2;
} custom_struct;

struct {
	int struct_field_1;
	char struct_field_2;
} struct_anon_var, struct_anon_var_2;

union union_test {
	int union_field_1;
	char union_field_2;
};


struct struct_without_definition;
struct struct_without_definition {
	int struct_field_1;
	char struct_field_2;
};

union {
	int union_field_1;
	char union_field_2;
} union_anon_var;


typedef struct { int ch1; float ch2; } loic_struct;

struct aligned_struct { short f[3]; } __attribute__ ((aligned (8)));

int main() {
}