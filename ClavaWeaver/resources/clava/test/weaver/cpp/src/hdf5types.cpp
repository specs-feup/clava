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

