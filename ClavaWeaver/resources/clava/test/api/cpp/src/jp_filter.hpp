class A {
 int anInt;

public: 
 float aFloat;
};

class A2 {
public: 

 float x;
 int y;
};

struct A3 {
 float x;
 int y;
};

class B {
 int anInt;
 float aFloat;
};

struct C {
 int anInt;
 float aFloat;
};

struct C2 {
 int anInt;
 float aDouble;
};

void foo(A& a);

void foo(B& a);

void foo(C& a);

void foo2();