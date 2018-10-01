#include <cstdint>

/** Example of the HDF5 type mapping problem **/

// Ordinary CPP structure
struct IndexInfo {
    long long timeCreated;
    signed char isDirectMapped;
    unsigned int numOfParts;
};

struct TestStruct {
    signed char signed_char_field;
    long long long_long_field;
	int int_field;
    char char_field;
    char char_array_field[30];
    float float_field;
};

class TestClass {
	
	// Private fields are not serialized
	int private_int;
	//double private_double;
	
public:
	short short_field;
	unsigned short unsigned_short_field;
	long long_field;
	unsigned long unsigned_long_field;
	unsigned long long unsigned_long_long_field;
	double double_field;
	long double long_double_field;
	//bool bool_field;
	
	int8_t int8_t_field;
	uint8_t uint8_t_field;
	int16_t int16_t_field;
	uint16_t uint16_t_field;
	int32_t int32_t_field;
	uint32_t uint32_t_field;
	int64_t int64_t_field;
	uint64_t uint64_t_field;
};
