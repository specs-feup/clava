#include "patch.h"
TYPE_PATCH_00 pixacompReplacePixcomp(PIXAC   *pixac,
                       l_int32  index,
                       PIXC    *pixc)
{
l_int32  n;
PIXC    *pixct;

    PROCNAME("pixacompReplacePixcomp");

    if (!pixac)
        return ERROR_INT("pixac not defined", procName, 1);
    n = pixacompGetCount(pixac);
    if (index < 0 || index >= n)
        return ERROR_INT("array index out of bounds", procName, 1);
    if (!pixc)
        return ERROR_INT("pixc not defined", procName, 1);

    pixct = pixacompGetPixcomp(pixac, index);
    pixcompDestroy(&pixct);
    pixac->pixc[index] = pixc;  /* replace */

    return 0;
}