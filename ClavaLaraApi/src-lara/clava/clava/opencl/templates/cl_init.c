cl_int <VAR_ERROR_CODE>;
cl_uint <VAR_NUM_PLATFORMS>;
cl_platform_id <VAR_PLATFORM_ID>;

// Check the number of platforms
<VAR_ERROR_CODE> = clGetPlatformIDs(0, NULL, &<VAR_NUM_PLATFORMS>);
if(<VAR_ERROR_CODE> != CL_SUCCESS) {
	fprintf(stderr, "[OpenCL] Error getting number of platforms\n");
	exit(1);
} else if(<VAR_NUM_PLATFORMS> == 0) {
	fprintf(stderr, "[OpenCL] No platforms found.\n");
	exit(1);
} else {
	printf("[OpenCL] Number of platforms is %d\n",<VAR_NUM_PLATFORMS>);
}

<VAR_ERROR_CODE> = clGetPlatformIDs(<DEVICE_ID>, &<VAR_PLATFORM_ID>, NULL);
if(<VAR_ERROR_CODE> != CL_SUCCESS) {
	fprintf(stderr, "[OpenCL] Error getting platform ID for device <DEVICE_ID>.\n");
	exit(1);
}
