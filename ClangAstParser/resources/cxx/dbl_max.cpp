#include <cfloat>		// DBL_MAX
#include <limits>       // std::numeric_limits

double test_macro = DBL_MAX;
double test_std = std::numeric_limits<double>::max();