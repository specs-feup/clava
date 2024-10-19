#include "lara_common_language.h"

class A;

class A {
};

void foo();

void foo();

void foo() {
	
	for(int i=0; i < 10; i++) {
	}
	
}

void fooOnlyDecl();

void fooOnlyDecl();

class classOnlyDecl;

class classOnlyDecl;

int main() {
	
	return 0;
}


namespace b {
	
	class B;
	
}


namespace b {
	
	class B;	
	
	class B {
		public:
		void bar();
	};
	
}

namespace c {
	namespace cc {
		class C;
	}
}

namespace c {
	namespace cc {
		class C {
			public:
			void bar();
		};
	}
}


namespace d {
	
	class D;
	
	class D {
		void bar(b::B b, c::cc::C c);
	};
}

void d::D::bar(b::B b, c::cc::C c) {
	b.bar();
	c.bar();	
}