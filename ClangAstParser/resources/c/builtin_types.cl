#pragma OPENCL EXTENSION cl_khr_fp16 : enable

__kernel void builtin_types() {

	half anHalf;
	__local __fp16 armHalf;
	sampler_t sampler;
	__private event_t event;
	local float* local_float;
	typedef __local float* pointer_to_private_float;
	__private pointer_to_private_float* private_float;
	typedef __local float* pointer_to_private_float2;
	__global pointer_to_private_float2* global_float;
	//__private (__local float)* x3;
	//clk_event_t clockEvent;
	//queue_t queue;
	//reserve_id_t reserveId;
}
