// Load the kernel source code into the array
FILE *<VAR_FILE> = fopen("<KERNEL_FILE>", "r");
if (!<VAR_FILE>) {
	fprintf(stdout, "Failed to load kernel.\n");
	exit(1);
}
char *<VAR_SOURCE_STR> = (char*)malloc(<SOURCE_SIZE_BYTES>);
size_t <VAR_SOURCE_SIZE> = fread(<VAR_SOURCE_STR>, 1, <SOURCE_SIZE_BYTES>, <VAR_FILE>);
fclose(<VAR_FILE>);