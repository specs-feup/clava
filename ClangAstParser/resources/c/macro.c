#include "macro.h"

#define FOR_BB_BETWEEN(BB, FROM, TO, DIR) \
  for (BB = FROM; BB != TO; BB = BB + 1)

#define FOR_EACH_BB(BB) \
  FOR_BB_BETWEEN (BB, ENTRY, EXIT, next_bb)

int ENTRY = 0;  
int EXIT = 10;
  
// Macro that instantiates a declaration
declInt(a,0)

int main() {
	
	// Macro that uses compound literal expression
	double d1, d2;
	double d3 = double_add(d1, d2);

	// Macro in size_of
	int size = custom_size(data_type);

	int i;
	
	/**
	 * A comment
	 **/
	FOR_EACH_BB (i)
    {
		;
	}
	
	return 0;
}
