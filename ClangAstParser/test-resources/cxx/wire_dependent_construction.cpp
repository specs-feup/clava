template <class T>
T braced(T value) {
  return T{value};
}

template <class T>
T empty_braced() {
  return T{};
}

template <class T>
T parenthesized(T value) {
  return T(value);
}

int main() {
  return braced<int>(1) + empty_braced<int>() + parenthesized<int>(2);
}
