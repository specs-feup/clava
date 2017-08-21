#include <list>

using namespace std;

class BondMap {
   
   
   public:
      struct MAtom;
	  struct MBond {
           list<struct MAtom>::iterator atom1;
	  };
	
	  struct MAtom {
			int a;
      };
};


class Top {};
class Top2 {};

class Derived1 : public Top {};
class Derived2 : public virtual Top, public Top2 {};


//template<class... Mixins>
//class X : public Mixins... {

/*
// This code makes ClangAst crash, there is a .getTypePtr() that is being called in a null pointer
 public:
    X(const Mixins&... mixins) : Mixins(mixins)... { }
	*/
//};

/*
template<class Mixins>
class X : public Mixins {

 public:
    X(const Mixins& mixins) : Mixins(mixins) { }
	
};
*/

