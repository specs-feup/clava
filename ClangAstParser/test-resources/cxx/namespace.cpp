#include "namespace.h"

namespace A {
	namespace B {
		void foo1(){};
	}
	
	namespace B2 {
		void foo1(){};
	}
	
	void B::foo2(){};
	void B2::foo2(){};
}

void A::B::foo3(){};
void A::B2::foo3(){};

