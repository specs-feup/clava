typedef int& LRI;
typedef int&& RRI;

typedef LRI& r1;
typedef const LRI& r2;
typedef const LRI&& r3;
typedef RRI& r4;
typedef RRI&& r5;

int value;
int& lvalue = value;

struct Box {
    int& member;
};

Box box{value};

void reference_bindings() {
    auto&& from_lvalue = lvalue;
    auto&& from_xvalue = static_cast<RRI&&>(value);
    auto&& from_member = box.member;
}
