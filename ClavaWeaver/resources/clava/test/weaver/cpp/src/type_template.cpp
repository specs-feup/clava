#include <vector>
#include <map>
#include <chrono>

int main ()
{
  std::vector<double> W;
  W[0] = 12.3;
  W[1] = 1.09;

  std::map<int, float> map;
  
  // TemplateArgumentExpr, not implemented yet
  auto nano = std::ratio<1, 1000000000>();
  
  return 0;
}
