class BaseKlass {
   public:
   virtual ~BaseKlass() {}
};

class DerivedKlass : BaseKlass {
   public:
      int f(int x) { return x + 1; }
};

BaseKlass* base = new BaseKlass();
DerivedKlass* derived = dynamic_cast<DerivedKlass*>(base);