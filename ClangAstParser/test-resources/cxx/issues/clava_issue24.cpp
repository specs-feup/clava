namespace clt {

   struct enclose {
       struct inner {
           static int x;
           void f();
       };
   };
   int enclose::inner::x = 1; // static variable definition
   void enclose::inner::f() {} // member function definition
}