namespace member_scope {
struct Owner {
    int left() & { return 1; }
    int right() && { return 2; }
    int qualified() const volatile & noexcept { return 3; }
};
template<class T> struct Box { int get() & { return 4; } };
}
int (member_scope::Owner::*left)() & = &member_scope::Owner::left;
int (member_scope::Owner::*right)() && = &member_scope::Owner::right;
int (member_scope::Owner::*qualified)() const volatile & noexcept = &member_scope::Owner::qualified;
int (member_scope::Box<int>::*specialized)() & = &member_scope::Box<int>::get;
int invoke(member_scope::Owner &owner) { return (owner.*left)(); }

namespace shadow_scope {
struct shadow_scope {};
struct Owner { int get() & { return 5; } };
int (Owner::*shadowed)() & = &Owner::get;
}
int local_class() {
    struct Local { int get() & { return 6; } };
    int (Local::*local)() & = &Local::get;
    Local owner;
    return (owner.*local)();
}
