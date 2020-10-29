#include <iostream>

class MyClass {     // The class
  public:           // Access specifier
    MyClass() {     // Constructor
      std::cout << "Hello World!";
    }
};

void expressionDecls() {
  MyClass myObj;    // Create an object of MyClass (this will call the constructor)

}

struct X { X(int, float); };

X temporaryExpressionDecl() {
  return X(1, 3.14f); // creates a CXXTemporaryObjectExpr
}