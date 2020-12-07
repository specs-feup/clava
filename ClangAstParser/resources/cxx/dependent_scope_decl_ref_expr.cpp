#include <limits>

template <typename FloatType> void compute_boundaries(FloatType value) {

  // Convert the IEEE representation into a diyfp.
  //
  // If v is denormal:
  //      value = 0.F * 2^(1 - bias) = (          F) * 2^(1 - bias - (p-1))
  // If v is normalized:
  //      value = 1.F * 2^(E - bias) = (2^(p-1) + F) * 2^(E - bias - (p-1))

  static_assert(std::numeric_limits<FloatType>::is_iec559,
                "internal error: dtoa_short requires an IEEE-754 "
                "floating-point implementation");
}