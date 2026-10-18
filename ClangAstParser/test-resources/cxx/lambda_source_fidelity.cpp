void capture_init_styles(int value) {
  auto direct_list = [item{value}] {};
  auto copy_init = [item = value] {};
  auto call_init = [item(value)] {};
}

template <class... T>
void capture_packs(T... values) {
  auto regular_copy_pack = [values...] {};
  auto regular_ref_pack = [&values...] {};
  auto init_capture_pack_call = [items(values...)] {};
  auto init_capture_pack_list = [items{values...}] {};
  auto init_copy_pack = [...items = values] {};
  auto init_ref_pack = [&...items = values] {};
}

void capture_defaults(int value, int other) {
  auto copy_default = [=] { return value; };
  auto ref_default = [&] { return value; };
  auto mixed_default = [=, &other] { return value + other; };
}
