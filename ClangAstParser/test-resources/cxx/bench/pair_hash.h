#include <cstdlib> 
#include <functional> 
#include <utility> 

class A
{
private:

	struct PairHash {
	public:
		template <typename T, typename U>
		std::size_t operator()(const std::pair<T, U> &x) const
		{
			return std::hash<T>()(x.first) ^ std::hash<U>()(x.second);
		}
	};
};