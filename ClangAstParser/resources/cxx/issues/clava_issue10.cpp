#include <iostream>

class ForwardDeclClass1;

namespace a {
   class ForwardDeclClass2;
   class RegularClass1 {};
   namespace b {
      class ForwardDeclClass3;
      class RegularClass2 {};
      namespace c {
         class ForwardDeclClass4;
         class RegularClass3 {};
      }
   }
}

int main(int argc, char *argv[]){
   std::cout << "Hello World!" << std::endl;
   return 0;
}