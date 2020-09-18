
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <iostream>
#include <string>

using namespace std;

template<class T, int v>
void foo(T p1) {
  for (int i=0; i< v; i++) {
    cout << " foo " << p1 << endl;
  } 
}


int main ( ) {

float ep1float = 123.89;
foo<float, 2> (ep1float);

bool ep1bool = true;
foo<bool, 4> (ep1bool);

 string ep1str=string("I am a string");
 foo<string, 6> (ep1str);

}


