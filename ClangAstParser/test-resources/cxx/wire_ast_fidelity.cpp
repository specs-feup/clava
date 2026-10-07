template <typename T>
struct Box {};

template <typename T>
struct Box<T *> {};

template <class O>
struct Outer {
  template <class I>
  struct Inner;

  template <class I>
  struct Inner<I *> {
    O *outer;
    I *inner;
  };
};

template <class T, int N>
struct PartialMethod;

template <class T, int N>
struct PartialMethod<T *, N> {
  PartialMethod(T);
  ~PartialMethod();
  T value() const;
};

template <class T, int N>
PartialMethod<T *, N>::PartialMethod(T) {}

template <class X, int N>
X PartialMethod<X *, N>::value() const {
  return X{};
}

template <class T, int N>
PartialMethod<T *, N>::~PartialMethod() {}

template <class>
struct MemberPointerKind;

template <class R, class C, class... Args>
struct MemberPointerKind<R (C::*)(Args...)> {
  using result_type = R;
  using class_type = C;
};

template <class>
struct QualifiedMemberPointerKind;

template <class R, class C, class... Args>
struct QualifiedMemberPointerKind<R (C::*)(Args...) const & noexcept> {};

template <class>
struct MemberArrayPointerKind;

template <class T, class C, unsigned N>
struct MemberArrayPointerKind<T (C::*)[N]> {
  using element_type = T;
};

template <class>
struct FunctionSignature;

template <class R, class... Args>
struct FunctionSignature<R(Args...)> {
  R invoke(Args... args);
};

struct Point {
  int x;
  int y;
};

struct Value {
  int value;
  Value(int value) : value(value) {}
  ~Value() {}
};

using ValueArray = Value[1];

struct Holder {
  Value member;
  ~Holder() {}
};

template <class E>
struct PointerHolder {
  const E *p;
  const E *q;
  PointerHolder() : p(nullptr), q((nullptr)) {}
};

PointerHolder<int> pointer_holder;

Value make_value() {
  return Value(11);
}

template <typename T>
T make_from(T (*constructor)()) {
  return T{constructor()};
}

int main() {
  int values[2]{1, 2};
  Point aggregate{values[0], values[1]};
  Point direct = Point{3, 4};
  Value braced = Value{make_value()};
  Value parenthesized_value = Value(make_value());
  auto&& array_element = ValueArray{make_value()}[0];
  auto&& cast_reference = (&(const Holder&)Holder{make_value()})->member;
  auto init_captures = [copy = values[0], &reference = values[1]]() {
    return copy + reference;
  };
  PartialMethod<int *, 3> partial_method(11);
  MemberPointerKind<int (PartialMethod<int *, 3>::*)(int)> member_pointer_kind;
  FunctionSignature<int(int, int)> function_signature;
  Value dependent = make_from<Value>(&make_value);
  return aggregate.x + direct.y + braced.value + parenthesized_value.value
      + array_element.value + init_captures() + dependent.value;
}
