namespace outer {
namespace inner {
template<class T> struct Box { T value; };
}
}
namespace alias = outer::inner;
using namespace alias;
Box<int> box{3};
int read_box() { return box.value; }
