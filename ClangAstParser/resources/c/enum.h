#ifndef TEST_MAIN_HPP_
#define TEST_MAIN_HPP_

/*
 * Typedef
 */

 // Simple typedef
typedef unsigned int footype;
//typedef long long footype;

// Typedef with other typedef
typedef footype footype2;

/*
 * Enums
 */

// Simple enum
enum simple_enum {
  ENUM1,
  ENUM2,
  ENUM3,
};

// Enum with initialization
enum simple_enum_with_init {
  ENUM_INIT_1 = 20,
  ENUM_INIT_2 = -3,
};

// Enum with typedef
typedef enum enum_with_typedef {
	ENUM_WITH_TYPEDEF_1,
	ENUM_WITH_TYPEDEF_2,
	ENUM_WITH_TYPEDEF_3,
	ENUM_WITH_TYPEDEF_4
} enum_with_typedef ;

// Enum without name in the declaration
typedef enum { false, true } logical;

// Anonymous enum
enum{BLACK,RED};

#endif
