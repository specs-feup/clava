#include "patch.h"
void LZe_fill_distance_prices( struct LZ_encoder * const encoder )
  {
  int dis, dis_state;
  for( dis = start_dis_model; dis < modeled_distances; ++dis )
    {
    const int dis_slot = dis_slots[dis];
    const int direct_bits = ( dis_slot >> 1 ) - 1;
    const int base = ( 2 | ( dis_slot & 1 ) ) << direct_bits;
    const int price =
      price_symbol_reversed( encoder->bm_dis + base - dis_slot - 1,
                             dis - base, direct_bits );
    for( dis_state = 0; dis_state < max_dis_states; ++dis_state )
      encoder->dis_prices[dis_state][dis] = price;
    }

  for( dis_state = 0; dis_state < max_dis_states; ++dis_state )
    {
    int * const dsp = encoder->dis_slot_prices[dis_state];
    int * const dp = encoder->dis_prices[dis_state];
    const Bit_model * const bmds = encoder->bm_dis_slot[dis_state];
    int slot = 0;
    for( ; slot < end_dis_model && slot < encoder->num_dis_slots; ++slot )
      dsp[slot] = price_symbol( bmds, slot, dis_slot_bits );
    for( ; slot < encoder->num_dis_slots; ++slot )
      dsp[slot] = price_symbol( bmds, slot, dis_slot_bits ) +
                  (((( slot >> 1 ) - 1 ) - dis_align_bits ) << price_shift_bits );

    for( dis = 0; dis < start_dis_model; ++dis )
      dp[dis] = dsp[dis];
    for( ; dis < modeled_distances; ++dis )
      dp[dis] += dsp[dis_slots[dis]];
    }
  }