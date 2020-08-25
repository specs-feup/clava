#include "patch.h"
void grd_FDIST (struct GRDMATH_INFO *info, float *stack[], GMT_LONG *constant, double *factor, GMT_LONG last)
/*OPERATOR: FDIST 3 1 F-distribution Q(F,n1,n2), with F = A, n1 = B, and n2 = C.  */
{
	GMT_LONG i, nu1, nu2, prev1, prev2;
	double F, chisq1, chisq2 = 1.0, prob;

	prev1 = last - 1;
	prev2 = last - 2;
	if (gmtdefs.verbose && constant[prev1] && factor[prev1] == 0.0) fprintf (stderr, "%s: Warning, operand two == 0 for FDIST!\n", GMT_program);
	if (gmtdefs.verbose && constant[last] && factor[last] == 0.0) fprintf (stderr, "%s: Warning, operand three == 0 for FDIST!\n", GMT_program);
	for (i = 0; i < info->nm; i++) {
		F = (constant[prev2]) ? factor[prev2] : stack[prev2][i];
		nu1 = (GMT_LONG)(irint ((double)((constant[prev1]) ? factor[prev1] : stack[prev1][i])));
		nu2 = (GMT_LONG)(irint ((double)((constant[last]) ? factor[last] : stack[last][i])));
		/* Since GMT_f_q needs chisq1 and chisq2, we set chisq2 = 1 and solve for chisq1 */
		chisq1 = F * nu1 / nu2;
		(void) GMT_f_q (chisq1, nu1, chisq2, nu2, &prob);
		stack[prev2][i] = (float)prob;
	}
}