double inputInCast(int ldmx, int k, double v[][1][10][5]) {
	double (*vk)[ldmx / 2 * 2 + 1][5] = v[k];	
	return vk[0][0][0];
}

int main() {
	return 0;
}