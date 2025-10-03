#include <iostream>
#include <cmath>
#include <stdlib.h>

 using namespace std;
class Atom {  // extracted from miniapp.
   double r[3];
   basic_string<char> const * name;
   basic_string<char> stype;
      
   public:
   //! Atom constructor from position name and type
   Atom(double r[3]) {
      this->clear();
      this->r[0] = r[0];
      this->r[1] = r[1];
      this->r[2] = r[2];
	  
   };
   
   Atom(double r[3], basic_string<char> const * name) : stype("") {
      this->clear();
      this->r[0] = r[0];
      this->r[1] = r[1];
      this->r[2] = r[2];
	  this->name = name;
   };
   
  void clear();
 };
   

int main (int argc, char *argv[])
{
  double YY[3] = { 23.20, 25.06, 1.009};
  Atom * ErrObj = new Atom(YY);
  return 0;
}

 
