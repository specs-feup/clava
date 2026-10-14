struct X {
  enum E { zero };
  E e : 2;
};

static_assert(sizeof(+X().e) == sizeof(int), "");
static_assert(sizeof(((X().e + 1))) == sizeof(int), "");

namespace nested {
int value = 0;
}; // namespace nested
