template <typename T>
struct Box {};

template <typename T>
struct Box<T *> {};

template <class O>
struct Outer {
  template <class I>
  struct Inner;

  template <class I>
  struct Inner<I *> {};
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
  Value dependent = make_from<Value>(&make_value);
  return aggregate.x + direct.y + braced.value + parenthesized_value.value
      + array_element.value + init_captures() + dependent.value;
}
