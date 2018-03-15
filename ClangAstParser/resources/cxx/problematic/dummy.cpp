#include <array>

class A
{
 public:
 static constexpr int number_of_sizes = 3;
 static constexpr std::array<int, number_of_sizes> SPACE{{512, 256, 128}};
};

constexpr std::array<int, A::number_of_sizes> A::SPACE;
