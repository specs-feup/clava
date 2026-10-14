#define VALUE 7
#define CAT_IMPL(left, right) left##right
#define CAT(left, right) CAT_IMPL(left, right)
#define DECL(name) int name = VALUE;

DECL(CAT(macro_, value))
int ordinary = 0;
int foobar = 1;
int pasted_reference = CAT(foo, bar);

namespace std {
using uint8_t = unsigned char;
template <class T> class vector;
} // namespace std

template <class BinaryType = std::vector<std::uint8_t>>
class Holder {};
