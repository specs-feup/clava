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

// Wide string
const wchar_t *wideString = L"a_wide_string";

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

// Class enum
enum class class_enum {
	ENUM_CLASS_1,
	ENUM_CLASS_2,
};


// enum with type
enum enum_with_type : footype {
	ENUM_WITH_TYPE_1,
	ENUM_WITH_TYPE_2,
};

// enum class with type and initialization
enum class enum_test : footype {
	FOO = 1,
	BAR = 2,
};

#endif