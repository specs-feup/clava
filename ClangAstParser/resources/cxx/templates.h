/*
class ClassA
{

	public:
		struct InnerStruct;
		int a;
};
*/

template<typename T, typename A1>
inline T make_a(const A1& a1) {
  return T(a1);
}
