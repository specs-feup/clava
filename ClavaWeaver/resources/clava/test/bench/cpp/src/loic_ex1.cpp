

#include <string>
#include <stdlib.h>

using namespace std;

class Vector{
 protected:
  int sz;
  double *elem;
public:
  Vector(int n)  {
    sz = n; elem = new double[n];
    for(int i=0;i<n;++i) elem[i]=0;
  }
  ~Vector() {delete[] elem;}
  void affectation(int indice, double element) 
  {
    elem[indice]=element;
  }

  double acces(int indice) {return elem[indice];}
  int taille() const {
    return sz;
}

  void affichesomme() {
    int res=0;
    for(int i=0;i<sz;i++)
      res+=elem[i];
    // cout<<" sum = "<< res << "\n";
  }
};




