//---------------------------------------------------------------------
// program FT
//---------------------------------------------------------------------

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <time.h>
#include <sys/time.h>


#if !defined(CLASS_W) && !defined(CLASS_S) && !defined(CLASS_A) && !defined(CLASS_B) && !defined(CLASS_C) && !defined(CLASS_D) && !defined(CLASS_E)
#   define CLASS_W
#endif

//----------
//  Class S:
//----------
#ifdef CLASS_S
#   define NX             64
#   define NY             64
#   define NZ             64
#   define MAXDIM         64
#   define NITER_DEFAULT  6
#   define NXP            65
#   define NYP            64
#   define NTOTAL         262144
#   define NTOTALP        266240
#endif
//----------
//  Class W:
//----------
#ifdef CLASS_W
#   define NX             128
#   define NY             128
#   define NZ             32
#   define MAXDIM         128
#   define NITER_DEFAULT  6
#   define NXP            129
#   define NYP            128
#   define NTOTAL         524288
#   define NTOTALP        528384
#endif
//----------
//  Class A:
//----------
#ifdef CLASS_A
#   define NX             256
#   define NY             256
#   define NZ             128
#   define MAXDIM         256
#   define NITER_DEFAULT  6
#   define NXP            257
#   define NYP            256
#   define NTOTAL         8388608
#   define NTOTALP        8421376
#endif
//----------
//  Class B:
//----------
#ifdef CLASS_B
#   define NX             512
#   define NY             256
#   define NZ             256
#   define MAXDIM         512
#   define NITER_DEFAULT  20
#   define NXP            513
#   define NYP            256
#   define NTOTAL         33554432
#   define NTOTALP        33619968
#endif
//----------
//  Class C:
//----------
#ifdef CLASS_C
#   define NX             512
#   define NY             512
#   define NZ             512
#   define MAXDIM         512
#   define NITER_DEFAULT  20
#   define NXP            513
#   define NYP            512
#   define NTOTAL         134217728
#   define NTOTALP        134479872
#endif
//----------
//  Class D:
//----------
#ifdef CLASS_D
#   define NX             2048
#   define NY             1024
#   define NZ             1024
#   define MAXDIM         2048
#   define NITER_DEFAULT  25
#   define NXP            2049
#   define NYP            1024
#   define NTOTAL         2147483648
#   define NTOTALP        2148532224
#endif
//----------
//  Class E:
//----------
#ifdef CLASS_E
#   define NX             4096
#   define NY             2048
#   define NZ             2048
#   define MAXDIM         4096
#   define NITER_DEFAULT  25
#   define NXP            4097
#   define NYP            2048
#   define NTOTAL         17179869184
#   define NTOTALP        17184063488
#endif


typedef struct
{
    double real;
    double imag;
} dcomplex;


#define min(x,y)    ((x) < (y) ? (x) : (y))
#define max(x,y)    ((x) > (y) ? (x) : (y))




#define FFTBLOCK_DEFAULT      16
#define FFTBLOCKPAD_DEFAULT   18
#define CACHESIZE             8192
#define BLOCKMAX              32

#define SEED                  314159265.0
#define A                     1220703125.0
#define PI                    3.141592653589793238
#define ALPHA                 1.0e-6

/* common /timerscomm/ */

dcomplex dcmplx_div(dcomplex z1, dcomplex z2)
{
    double a = z1.real;
    double b = z1.imag;
    double c = z2.real;
    double d = z2.imag;

    double divisor = c * c + d * d;
    double real = (a * c + b * d) / divisor;
    double imag = (b * c - a * d) / divisor;
    dcomplex result = (dcomplex) {real, imag};
    return result;
}

#define dcmplx(r,i)       (dcomplex){r, i}
#define dcmplx_add(a,b)   (dcomplex){(a).real+(b).real, (a).imag+(b).imag}
#define dcmplx_sub(a,b)   (dcomplex){(a).real-(b).real, (a).imag-(b).imag}
#define dcmplx_mul(a,b)   (dcomplex){((a).real*(b).real)-((a).imag*(b).imag),\
                                     ((a).real*(b).imag)+((a).imag*(b).real)}
#define dcmplx_mul2(a,b)  (dcomplex){(a).real*(b), (a).imag*(b)}

#define dcmplx_div2(a,b)  (dcomplex){(a).real/(b), (a).imag/(b)}
#define dcmplx_abs(x)     sqrt(((x).real*(x).real) + ((x).imag*(x).imag))

#define dconjg(x)         (dcomplex){(x).real, -1.0*(x).imag}

/* common /blockinfo/ */
int fftblock;

/* common /workarr/ */
dcomplex plane[(BLOCKMAX + 1)*MAXDIM];
dcomplex scr[MAXDIM][BLOCKMAX + 1];

// for checksum data
/* common /sumcomm/ */
dcomplex sums[NITER_DEFAULT + 1];

/* common /mainarrays/ */
double twiddle[NZ][NY][NX + 1];
dcomplex xnt[NZ][NY][NX + 1];
dcomplex y[NZ][NY][NX + 1];


void appft(int niter, double *total_time, int *verified);
void CompExp(int n, dcomplex exponent[n]);
int ilog2(int n);
void CalculateChecksum(dcomplex *csum, int iterN, int d1, int d2, int d3,
                       dcomplex u[d3][d2][d1 + 1]);
void compute_initial_conditions(int d1, int d2, int d3,
                                dcomplex u0[d3][d2][d1 + 1]);
void evolve(int nx, int ny, int nz,
            dcomplex x[nz][ny][nx + 1], dcomplex y[nz][ny][nx + 1],
            double twiddle[nz][ny][nx + 1]);
void fftXYZ(int sign, int n1, int n2, int n3,
            dcomplex x[n3][n2][n1 + 1], dcomplex xout[(n1 + 1)*n2 * n3],
            dcomplex exp1[n1], dcomplex exp2[n2], dcomplex exp3[n3]);
void verify(int n1, int n2, int n3, int nt, dcomplex cksum[nt + 1],
            int *verified);

double randlc( double *x, double a );
void vranlc( int n, double *x, double a, double y[] );


char getclass();



void print_results(char *name, char class, int n1, int n2, int n3, int niter,
                   double t, double mops, char *optype, int verified);


double start[64], elapsed[64];
double elapsed_time( void );
void timer_clear( int n );
void timer_start( int n );
void timer_stop( int n );
double timer_read( int n );
void wtime(double *t);




int main(int argc, char *argv[])
{
    int niter;
    char Class;
    double total_time, mflops;
    int verified;


    niter = NITER_DEFAULT;

    printf("\n\n NAS Parallel Benchmarks (NPB3.3-SER-C) - FT Benchmark\n\n");
    printf(" Size                : %4dx%4dx%4d\n", NX, NY, NZ);
    printf(" Iterations          :     %10d\n", niter);
    printf("\n");

    Class = getclass();

    appft(niter, &total_time, &verified);

    if (total_time != 0.0)
    {
        mflops = 1.0e-6 * (double)NTOTAL *
                 (14.8157 + 7.19641 * log((double)NTOTAL)
                  + (5.23518 + 7.21113 * log((double)NTOTAL)) * niter)
                 / total_time;
    }
    else
    {
        mflops = 0.0;
    }

    print_results("FT", Class, NX, NY, NZ, niter,
                  total_time, mflops, "          floating point", verified);

  int exitValue = verified ? 0 : 1;
  return exitValue;
}


char getclass()
{
    if ((NX == 64) && (NY == 64) &&
            (NZ == 64) && (NITER_DEFAULT == 6))
    {
        return 'S';
    }
    else if ((NX == 128) && (NY == 128) &&
             (NZ == 32) && (NITER_DEFAULT == 6))
    {
        return 'W';
    }
    else if ((NX == 256) && (NY == 256) &&
             (NZ == 128) && (NITER_DEFAULT == 6))
    {
        return 'A';
    }
    else if ((NX == 512) && (NY == 256) &&
             (NZ == 256) && (NITER_DEFAULT == 20))
    {
        return 'B';
    }
    else if ((NX == 512) && (NY == 512) &&
             (NZ == 512) && (NITER_DEFAULT == 20))
    {
        return 'C';
    }
    else if ((NX == 2048) && (NY == 1024) &&
             (NZ == 1024) && (NITER_DEFAULT == 25))
    {
        return 'D';
    }
    else
    {
        return 'U';
    }
}




void appft(int niter, double *total_time, int *verified)
{
    int i, j, k, kt, n12, n22, n32, ii, jj, kk, ii2, ik2;
    double ap;

    dcomplex exp1[NX], exp2[NY], exp3[NZ];

    for (i = 1; i <= 15; i++)
    {
        timer_clear(i);
    }

    timer_start(2);
    compute_initial_conditions(NX, NY, NZ, xnt);

    CompExp(NX, exp1);
    CompExp(NY, exp2);
    CompExp(NZ, exp3);
    fftXYZ(1, NX, NY, NZ, xnt, (dcomplex *)y, exp1, exp2, exp3);
    timer_stop(2);

    timer_start(1);
	
	#pragma kernel
	{
    n12 = NX / 2;
    n22 = NY / 2;
    n32 = NZ / 2;
    ap = -4.0 * ALPHA * (PI * PI);

    for (i = 0; i < NZ; i++)
    {
        ii = i - (i / n32) * NZ;
        ii2 = ii * ii;
        for (k = 0; k < NY; k++)
        {
            kk = k - (k / n22) * NY;
            ik2 = ii2 + kk * kk;
            for (j = 0; j < NX; j++)
            {
                jj = j - (j / n12) * NX;
                twiddle[i][k][j] = exp(ap * (double)(jj * jj + ik2));
            }
        }
    }

    compute_initial_conditions(NX, NY, NZ, xnt);
    fftXYZ(1, NX, NY, NZ, xnt, (dcomplex *)y, exp1, exp2, exp3);

    for (kt = 1; kt <= niter; kt++)
    {
        evolve(NX, NY, NZ, xnt, y, twiddle);
        fftXYZ(-1, NX, NY, NZ, xnt, (dcomplex *)xnt, exp1, exp2, exp3);
        CalculateChecksum(&sums[kt], kt, NX, NY, NZ, xnt);
    }

    // Verification test.
    verify(NX, NY, NZ, niter, sums, verified);
	}
    timer_stop(1);

    *total_time = timer_read(1);
}


//---------------------------------------------------------------------
// compute the roots-of-unity array that will be used for subsequent FFTs.
//---------------------------------------------------------------------
void CompExp(int n, dcomplex exponent[n])
{
    int m, nu, ku, i, j, ln;
    double t, ti;
    const double pi = 3.141592653589793238;

    nu = n;
    m = ilog2(n);
    exponent[0] = dcmplx(m, 0.0);
    ku = 2;
    ln = 1;
    for (j = 1; j <= m; j++)
    {
        t = pi / ln;

        for (i = 0; i <= ln - 1; i++)
        {
            ti = i * t;
            exponent[i + ku - 1] = dcmplx(cos(ti), sin(ti));
        }
        ku = ku + ln;
        ln = 2 * ln;
    }
}


int ilog2(int n)
{
    int nn, lg;
    if (n == 1) return 0;

    lg = 1;
    nn = 2;
    while (nn < n)
    {
        nn = nn * 2;
        lg = lg + 1;
    }
    return lg;
}


//---------------------------------------------------------------------
// compute a^exponent mod 2^46
//---------------------------------------------------------------------
double ipow46(double a, int exponent)
{
    double result, dummy, q, r;
    int n, n2;

    //---------------------------------------------------------------------
    // Use
    //   a^n = a^(n/2)*a^(n/2) if n even else
    //   a^n = a*a^(n-1)       if n odd
    //---------------------------------------------------------------------
    result = 1;
    if (exponent == 0) return result;
    q = a;
    r = 1;
    n = exponent;

    while (n > 1)
    {
        n2 = n / 2;
        if (n2 * 2 == n)
        {
            dummy = randlc(&q, q);
            n = n2;
        }
        else
        {
            dummy = randlc(&r, q);
            n = n - 1;
        }
    }
    dummy = randlc(&r, q);
    result = r;
    return result;
}


void CalculateChecksum(dcomplex *csum, int iterN, int d1, int d2, int d3,
                       dcomplex u[d3][d2][d1 + 1])
{
    int i, i1, ii, ji, ki;
    dcomplex csum_temp = dcmplx(0.0, 0.0);
    for (i = 1; i <= 1024; i++)
    {
        i1 = i;
        ii = i1 % d1;
        ji = 3 * i1 % d2;
        ki = 5 * i1 % d3;
        csum_temp = dcmplx_add(csum_temp, u[ki][ji][ii]);
    }
    csum_temp = dcmplx_div2(csum_temp, (double)(d1 * d2 * d3));
    printf(" T =%5d     Checksum =%22.12E%22.12E\n",
           iterN, csum_temp.real, csum_temp.imag);
    *csum = csum_temp;
}


void compute_initial_conditions(int d1, int d2, int d3,
                                dcomplex u0[d3][d2][d1 + 1])
{
    dcomplex tmp[MAXDIM];
    double x0, start, an, dummy;
    double RanStarts[MAXDIM];

    int i, j, k;
    const double seed = 314159265.0;
    const double a = 1220703125.0;

    start = seed;
    //---------------------------------------------------------------------
    // Jump to the starting element for our first plane.
    //---------------------------------------------------------------------
    an = ipow46(a, 0);
    dummy = randlc(&start, an);
    an = ipow46(a, 2 * d1 * d2);
    //---------------------------------------------------------------------
    // Go through by z planes filling in one square at a time.
    //---------------------------------------------------------------------
    RanStarts[0] = start;
    for (k = 1; k < d3; k++)
    {
        dummy = randlc(&start, an);
        RanStarts[k] = start;
    }

    for (k = 0; k < d3; k++)
    {
        x0 = RanStarts[k];
        for (j = 0; j < d2; j++)
        {
            vranlc(2 * d1, &x0, a, (double *)tmp);

            for (i = 0; i < d1; i++)
            {
                u0[k][j][i] = tmp[i];
            }
        }
    }
}


void evolve(int nx, int ny, int nz,
            dcomplex x[nz][ny][nx + 1], dcomplex y[nz][ny][nx + 1],
            double twiddle[nz][ny][nx + 1])
{
    int i, j, k;
    for (i = 0; i < nz; i++)
    {
        for (k = 0; k < ny; k++)
        {
            for (j = 0; j < nx; j++)
            {
                y[i][k][j] = dcmplx_mul2(y[i][k][j], twiddle[i][k][j]);
                x[i][k][j] = y[i][k][j];
            }
        }
    }
}

//---------------------------------------------------------------------
// Computes NY N-point complex-to-complex FFTs of X using an algorithm due
// to Swarztrauber.  X is both the input and the output array, while Y is a
// scratch array.  It is assumed that N = 2^M.  Before calling
// Swarztrauber to
// perform FFTs
//---------------------------------------------------------------------
void Swarztrauber(int is, int m, int vlen, int n, int xd1,
                  void *ox, dcomplex exponent[n])
{
    dcomplex (*x)[xd1] = (dcomplex (*)[xd1])ox;

    int i, j, l;
    dcomplex u1, x11, x21;
    int k, n1, li, lj, lk, ku, i11, i12, i21, i22;

    //---------------------------------------------------------------------
    // Perform one variant of the Stockham FFT.
    //---------------------------------------------------------------------
    n1 = n / 2;
    lj = 1;
    li = 1 << m;
    for (l = 1; l <= m; l += 2)
    {
        lk = lj;
        lj = 2 * lk;
        li = li / 2;
        ku = li;

        for (i = 0; i <= li - 1; i++)
        {
            i11 = i * lk;
            i12 = i11 + n1;
            i21 = i * lj;
            i22 = i21 + lk;

            if (is >= 1)
            {
                u1 = exponent[ku + i];
            }
            else
            {
                u1 = dconjg(exponent[ku + i]);
            }
            for (k = 0; k <= lk - 1; k++)
            {

                for (j = 0; j < vlen; j++)
                {
                    x11 = x[i11 + k][j];
                    x21 = x[i12 + k][j];
                    scr[i21 + k][j] = dcmplx_add(x11, x21);
                    scr[i22 + k][j] = dcmplx_mul(u1, dcmplx_sub(x11, x21));
                }
            }
        }

        if (l == m)
        {

            for (k = 0; k < n; k++)
            {
                for (j = 0; j < vlen; j++)
                {
                    x[k][j] = scr[k][j];
                }
            }
        }
        else
        {
            lk = lj;
            lj = 2 * lk;
            li = li / 2;
            ku = li;

            for (i = 0; i <= li - 1; i++)
            {
                i11 = i * lk;
                i12 = i11 + n1;
                i21 = i * lj;
                i22 = i21 + lk;

                if (is >= 1)
                {
                    u1 = exponent[ku + i];
                }
                else
                {
                    u1 = dconjg(exponent[ku + i]);
                }
                for (k = 0; k <= lk - 1; k++)
                {

                    for (j = 0; j < vlen; j++)
                    {
                        x11 = scr[i11 + k][j];
                        x21 = scr[i12 + k][j];
                        x[i21 + k][j] = dcmplx_add(x11, x21);
                        x[i22 + k][j] = dcmplx_mul(u1, dcmplx_sub(x11, x21));
                    }
                }
            }
        }
    }
}


void fftXYZ(int sign, int n1, int n2, int n3,
            dcomplex x[n3][n2][n1 + 1], dcomplex xout[(n1 + 1)*n2 * n3],
            dcomplex exp1[n1], dcomplex exp2[n2], dcomplex exp3[n3])
{
    int i, j, k, log;
    int bls, ble;
    int len;
    int blkp;


    fftblock = CACHESIZE / n1;
    if (fftblock >= BLOCKMAX) fftblock = BLOCKMAX;
    blkp = fftblock + 1;
    log = ilog2(n1);

    for (k = 0; k < n3; k++)
    {
        for (bls = 0; bls < n2; bls += fftblock)
        {
            ble = bls + fftblock - 1;
            if (ble > n2) ble = n2 - 1;
            len = ble - bls + 1;

            for (j = bls; j <= ble; j++)
            {
                for (i = 0; i < n1; i++)
                {
                    plane[j - bls + blkp * i] = x[k][j][i];
                }
            }
            Swarztrauber(sign, log, len, n1, blkp, plane, exp1);

            for (j = bls; j <= ble; j++)
            {
                for (i = 0; i < n1; i++)
                {
                    x[k][j][i] = plane[j - bls + blkp * i];
                }
            }
        }
    }

    fftblock = CACHESIZE / n2;
    if (fftblock >= BLOCKMAX) fftblock = BLOCKMAX;
    blkp = fftblock + 1;
    log = ilog2(n2);
    for (k = 0; k < n3; k++)
    {
        for (bls = 0; bls < n1; bls += fftblock)
        {
            ble = bls + fftblock - 1;
            if (ble > n1) ble = n1 - 1;
            len = ble - bls + 1;
            Swarztrauber(sign, log, len, n2, n1 + 1, &x[k][0][bls], exp2);
        }
    }

    fftblock = CACHESIZE / n3;
    if (fftblock >= BLOCKMAX) fftblock = BLOCKMAX;
    blkp = fftblock + 1;
    log = ilog2(n3);

    for (k = 0; k < n2; k++)
    {
        for (bls = 0; bls < n1; bls += fftblock)
        {
            ble = bls + fftblock - 1;
            if (ble > n1) ble = n1 - 1;
            len = ble - bls + 1;

            for (i = 0; i < n3; i++)
            {
                for (j = bls; j <= ble; j++)
                {
                    plane[j - bls + blkp * i] = x[i][k][j];
                }
            }
            Swarztrauber(sign, log, len, n3, blkp, plane, exp3);

            for (i = 0; i <= n3 - 1; i++)
            {
                for (j = bls; j <= ble; j++)
                {
                    xout[j + (n1 + 1) * (k + n2 * i)] = plane[j - bls + blkp * i];
                }
            }
        }
    }
}

// FT verification routine.
void verify(int n1, int n2, int n3, int nt, dcomplex cksum[nt + 1],
            int *verified)
{
    // Local variables.
    int kt;
    dcomplex cexpd[25 + 1];
    double epsilon, err;

    // Initialize tolerance level and success flag.

    epsilon = 1.0e-12;
    *verified = 1;

    if ((n1 == 64) && (n2 == 64) && (n3 == 64) && (nt == 6))
    {
        // Class S reference values.
        cexpd[1] = dcmplx(554.6087004964, 484.5363331978);
        cexpd[2] = dcmplx(554.6385409189, 486.5304269511);
        cexpd[3] = dcmplx(554.6148406171, 488.3910722336);
        cexpd[4] = dcmplx(554.5423607415, 490.1273169046);
        cexpd[5] = dcmplx(554.4255039624, 491.7475857993);
        cexpd[6] = dcmplx(554.2683411902, 493.2597244941);
    }
    else if ((n1 == 128) && (n2 == 128) && (n3 == 32) && (nt == 6))
    {
        // Class W reference values.
        cexpd[1] = dcmplx(567.3612178944, 529.3246849175);
        cexpd[2] = dcmplx(563.1436885271, 528.2149986629);
        cexpd[3] = dcmplx(559.4024089970, 527.0996558037);
        cexpd[4] = dcmplx(556.0698047020, 526.0027904925);
        cexpd[5] = dcmplx(553.0898991250, 524.9400845633);
        cexpd[6] = dcmplx(550.4159734538, 523.9212247086);
    }
    else if ((n1 == 256) && (n2 == 256) && (n3 == 128) && (nt == 6))
    {
        // Class A reference values.
        cexpd[1] = dcmplx(504.6735008193, 511.4047905510);
        cexpd[2] = dcmplx(505.9412319734, 509.8809666433);
        cexpd[3] = dcmplx(506.9376896287, 509.8144042213);
        cexpd[4] = dcmplx(507.7892868474, 510.1336130759);
        cexpd[5] = dcmplx(508.5233095391, 510.4914655194);
        cexpd[6] = dcmplx(509.1487099959, 510.7917842803);
    }
    else if ((n1 == 512) && (n2 == 256) && (n3 == 256) && (nt == 20))
    {
        // Class B reference values.
        cexpd[1]  = dcmplx(517.7643571579, 507.7803458597);
        cexpd[2]  = dcmplx(515.4521291263, 508.8249431599);
        cexpd[3]  = dcmplx(514.6409228649, 509.6208912659);
        cexpd[4]  = dcmplx(514.2378756213, 510.1023387619);
        cexpd[5]  = dcmplx(513.9626667737, 510.3976610617);
        cexpd[6]  = dcmplx(513.7423460082, 510.5948019802);
        cexpd[7]  = dcmplx(513.5547056878, 510.7404165783);
        cexpd[8]  = dcmplx(513.3910925466, 510.8576573661);
        cexpd[9]  = dcmplx(513.2470705390, 510.9577278523);
        cexpd[10] = dcmplx(513.1197729984, 511.0460304483);
        cexpd[11] = dcmplx(513.0070319283, 511.1252433800);
        cexpd[12] = dcmplx(512.9070537032, 511.1968077718);
        cexpd[13] = dcmplx(512.8182883502, 511.2616233064);
        cexpd[14] = dcmplx(512.7393733383, 511.3203605551);
        cexpd[15] = dcmplx(512.6691062020, 511.3735928093);
        cexpd[16] = dcmplx(512.6064276004, 511.4218460548);
        cexpd[17] = dcmplx(512.5504076570, 511.4656139760);
        cexpd[18] = dcmplx(512.5002331720, 511.5053595966);
        cexpd[19] = dcmplx(512.4551951846, 511.5415130407);
        cexpd[20] = dcmplx(512.4146770029, 511.5744692211);

    }
    else if ((n1 == 512) && (n2 == 512) && (n3 == 512) && (nt == 20))
    {
        // Class C reference values.
        cexpd[1]  = dcmplx(519.5078707457, 514.9019699238);
        cexpd[2]  = dcmplx(515.5422171134, 512.7578201997);
        cexpd[3]  = dcmplx(514.4678022222, 512.2251847514);
        cexpd[4]  = dcmplx(514.0150594328, 512.1090289018);
        cexpd[5]  = dcmplx(513.7550426810, 512.1143685824);
        cexpd[6]  = dcmplx(513.5811056728, 512.1496764568);
        cexpd[7]  = dcmplx(513.4569343165, 512.1870921893);
        cexpd[8]  = dcmplx(513.3651975661, 512.2193250322);
        cexpd[9]  = dcmplx(513.2955192805, 512.2454735794);
        cexpd[10] = dcmplx(513.2410471738, 512.2663649603);
        cexpd[11] = dcmplx(513.1971141679, 512.2830879827);
        cexpd[12] = dcmplx(513.1605205716, 512.2965869718);
        cexpd[13] = dcmplx(513.1290734194, 512.3075927445);
        cexpd[14] = dcmplx(513.1012720314, 512.3166486553);
        cexpd[15] = dcmplx(513.0760908195, 512.3241541685);
        cexpd[16] = dcmplx(513.0528295923, 512.3304037599);
        cexpd[17] = dcmplx(513.0310107773, 512.3356167976);
        cexpd[18] = dcmplx(513.0103090133, 512.3399592211);
        cexpd[19] = dcmplx(512.9905029333, 512.3435588985);
        cexpd[20] = dcmplx(512.9714421109, 512.3465164008);
    }
    else if ((n1 == 2048) && (n2 == 1024) && (n3 == 1024) && (nt == 25))
    {
        // Class D reference values.
        cexpd[1]  = dcmplx(512.2230065252, 511.8534037109);
        cexpd[2]  = dcmplx(512.0463975765, 511.7061181082);
        cexpd[3]  = dcmplx(511.9865766760, 511.7096364601);
        cexpd[4]  = dcmplx(511.9518799488, 511.7373863950);
        cexpd[5]  = dcmplx(511.9269088223, 511.7680347632);
        cexpd[6]  = dcmplx(511.9082416858, 511.7967875532);
        cexpd[7]  = dcmplx(511.8943814638, 511.8225281841);
        cexpd[8]  = dcmplx(511.8842385057, 511.8451629348);
        cexpd[9]  = dcmplx(511.8769435632, 511.8649119387);
        cexpd[10] = dcmplx(511.8718203448, 511.8820803844);
        cexpd[11] = dcmplx(511.8683569061, 511.8969781011);
        cexpd[12] = dcmplx(511.8661708593, 511.9098918835);
        cexpd[13] = dcmplx(511.8649768950, 511.9210777066);
        cexpd[14] = dcmplx(511.8645605626, 511.9307604484);
        cexpd[15] = dcmplx(511.8647586618, 511.9391362671);
        cexpd[16] = dcmplx(511.8654451572, 511.9463757241);
        cexpd[17] = dcmplx(511.8665212451, 511.9526269238);
        cexpd[18] = dcmplx(511.8679083821, 511.9580184108);
        cexpd[19] = dcmplx(511.8695433664, 511.9626617538);
        cexpd[20] = dcmplx(511.8713748264, 511.9666538138);
        cexpd[21] = dcmplx(511.8733606701, 511.9700787219);
        cexpd[22] = dcmplx(511.8754661974, 511.9730095953);
        cexpd[23] = dcmplx(511.8776626738, 511.9755100241);
        cexpd[24] = dcmplx(511.8799262314, 511.9776353561);
        cexpd[25] = dcmplx(511.8822370068, 511.9794338060);
    }
    else if ((n1 == 4096) && (n2 == 2048) && (n3 == 2048) && (nt == 25))
    {
        // Class E reference values.
        cexpd[1]  = dcmplx(512.1601045346, 511.7395998266);
        cexpd[2]  = dcmplx(512.0905403678, 511.8614716182);
        cexpd[3]  = dcmplx(512.0623229306, 511.9074203747);
        cexpd[4]  = dcmplx(512.0438418997, 511.9345900733);
        cexpd[5]  = dcmplx(512.0311521872, 511.9551325550);
        cexpd[6]  = dcmplx(512.0226088809, 511.9720179919);
        cexpd[7]  = dcmplx(512.0169296534, 511.9861371665);
        cexpd[8]  = dcmplx(512.0131225172, 511.9979364402);
        cexpd[9]  = dcmplx(512.0104767108, 512.0077674092);
        cexpd[10] = dcmplx(512.0085127969, 512.0159443121);
        cexpd[11] = dcmplx(512.0069224127, 512.0227453670);
        cexpd[12] = dcmplx(512.0055158164, 512.0284096041);
        cexpd[13] = dcmplx(512.0041820159, 512.0331373793);
        cexpd[14] = dcmplx(512.0028605402, 512.0370938679);
        cexpd[15] = dcmplx(512.0015223011, 512.0404138831);
        cexpd[16] = dcmplx(512.0001570022, 512.0432068837);
        cexpd[17] = dcmplx(511.9987650555, 512.0455615860);
        cexpd[18] = dcmplx(511.9973525091, 512.0475499442);
        cexpd[19] = dcmplx(511.9959279472, 512.0492304629);
        cexpd[20] = dcmplx(511.9945006558, 512.0506508902);
        cexpd[21] = dcmplx(511.9930795911, 512.0518503782);
        cexpd[22] = dcmplx(511.9916728462, 512.0528612016);
        cexpd[23] = dcmplx(511.9902874185, 512.0537101195);
        cexpd[24] = dcmplx(511.9889291565, 512.0544194514);
        cexpd[25] = dcmplx(511.9876028049, 512.0550079284);
    }
    else
    {
        printf("  Verification test for FT not performed\n");
        *verified = 0;
    }

    // Verification test for results.
    if (*verified)
    {
        for (kt = 1; kt <= nt; kt++)
        {
            err = dcmplx_abs(dcmplx_div(dcmplx_sub(cksum[kt], cexpd[kt]),
                                        cexpd[kt]));
            if (!(err <= epsilon))
            {
                *verified = 0;
                break;
            }
        }

        if (*verified)
        {
            printf(" Verification test for FT successful\n");
        }
        else
        {
            printf(" Verification test for FT failed\n");
        }
    }
}



void print_results(char *name, char class, int n1, int n2, int n3, int niter,
                   double t, double mops, char *optype, int verified)
{
    char size[16];
    int j;

    printf( "\n\n %s Benchmark Completed.\n", name );
    printf( " Class           =             %12c\n", class );

    // If this is not a grid-based problem (EP, FT, CG), then
    // we only print n1, which contains some measure of the
    // problem size. In that case, n2 and n3 are both zero.
    // Otherwise, we print the grid size n1xn2xn3

    if ( ( n2 == 0 ) && ( n3 == 0 ) )
    {
        if ( ( name[0] == 'E' ) && ( name[1] == 'P' ) )
        {
            sprintf( size, "%15.0lf", pow(2.0, n1) );
            j = 14;
            if ( size[j] == '.' )
            {
                size[j] = ' ';
                j--;
            }
            size[j + 1] = '\0';
            printf( " Size            =          %15s\n", size );
        }
        else
        {
            printf( " Size            =             %12d\n", n1 );
        }
    }
    else
    {
        printf( " Size            =           %4dx%4dx%4d\n", n1, n2, n3 );
    }

    printf( " Iterations      =             %12d\n", niter );
    printf( " Time in seconds =             %12.2lf\n", t );
    printf( " Mop/s total     =          %15.2lf\n", mops );
    printf( " Operation type  = %24s\n", optype );
    if ( verified )
        printf( " Verification    =             %12s\n", "SUCCESSFUL" );
    else
        printf( " Verification    =             %12s\n", "UNSUCCESSFUL" );


}

double randlc( double *x, double a )
{
    //--------------------------------------------------------------------
    //
    //  This routine returns a uniform pseudorandom double precision number in the
    //  range (0, 1) by using the linear congruential generator
    //
    //  x_{k+1} = a x_k  (mod 2^46)
    //
    //  where 0 < x_k < 2^46 and 0 < a < 2^46.  This scheme generates 2^44 numbers
    //  before repeating.  The argument A is the same as 'a' in the above formula,
    //  and X is the same as x_0.  A and X must be odd double precision integers
    //  in the range (1, 2^46).  The returned value RANDLC is normalized to be
    //  between 0 and 1, i.e. RANDLC = 2^(-46) * x_1.  X is updated to contain
    //  the new seed x_1, so that subsequent calls to RANDLC using the same
    //  arguments will generate a continuous sequence.
    //
    //  This routine should produce the same results on any computer with at least
    //  48 mantissa bits in double precision floating point data.  On 64 bit
    //  systems, double precision should be disabled.
    //
    //  David H. Bailey     October 26, 1990
    //
    //--------------------------------------------------------------------

    // r23 = pow(0.5, 23.0);
    ////  pow(0.5, 23.0) = 1.1920928955078125e-07
    // r46 = r23 * r23;
    // t23 = pow(2.0, 23.0);
    ////  pow(2.0, 23.0) = 8.388608e+06
    // t46 = t23 * t23;

    const double r23 = 1.1920928955078125e-07;
    const double r46 = r23 * r23;
    const double t23 = 8.388608e+06;
    const double t46 = t23 * t23;

    double t1, t2, t3, t4, a1, a2, x1, x2, z;
    double r;

    //--------------------------------------------------------------------
    //  Break A into two parts such that A = 2^23 * A1 + A2.
    //--------------------------------------------------------------------
    t1 = r23 * a;
    a1 = (int) t1;
    a2 = a - t23 * a1;

    //--------------------------------------------------------------------
    //  Break X into two parts such that X = 2^23 * X1 + X2, compute
    //  Z = A1 * X2 + A2 * X1  (mod 2^23), and then
    //  X = 2^23 * Z + A2 * X2  (mod 2^46).
    //--------------------------------------------------------------------
    t1 = r23 * (*x);
    x1 = (int) t1;
    x2 = *x - t23 * x1;
    t1 = a1 * x2 + a2 * x1;
    t2 = (int) (r23 * t1);
    z = t1 - t23 * t2;
    t3 = t23 * z + a2 * x2;
    t4 = (int) (r46 * t3);
    *x = t3 - t46 * t4;
    r = r46 * (*x);

    return r;
}


void vranlc( int n, double *x, double a, double y[] )
{
    //--------------------------------------------------------------------
    //
    //  This routine generates N uniform pseudorandom double precision numbers in
    //  the range (0, 1) by using the linear congruential generator
    //
    //  x_{k+1} = a x_k  (mod 2^46)
    //
    //  where 0 < x_k < 2^46 and 0 < a < 2^46.  This scheme generates 2^44 numbers
    //  before repeating.  The argument A is the same as 'a' in the above formula,
    //  and X is the same as x_0.  A and X must be odd double precision integers
    //  in the range (1, 2^46).  The N results are placed in Y and are normalized
    //  to be between 0 and 1.  X is updated to contain the new seed, so that
    //  subsequent calls to VRANLC using the same arguments will generate a
    //  continuous sequence.  If N is zero, only initialization is performed, and
    //  the variables X, A and Y are ignored.
    //
    //  This routine is the standard version designed for scalar or RISC systems.
    //  However, it should produce the same results on any single processor
    //  computer with at least 48 mantissa bits in double precision floating point
    //  data.  On 64 bit systems, double precision should be disabled.
    //
    //--------------------------------------------------------------------

    // r23 = pow(0.5, 23.0);
    ////  pow(0.5, 23.0) = 1.1920928955078125e-07
    // r46 = r23 * r23;
    // t23 = pow(2.0, 23.0);
    ////  pow(2.0, 23.0) = 8.388608e+06
    // t46 = t23 * t23;

    const double r23 = 1.1920928955078125e-07;
    const double r46 = r23 * r23;
    const double t23 = 8.388608e+06;
    const double t46 = t23 * t23;

    double t1, t2, t3, t4, a1, a2, x1, x2, z;

    int i;

    //--------------------------------------------------------------------
    //  Break A into two parts such that A = 2^23 * A1 + A2.
    //--------------------------------------------------------------------
    t1 = r23 * a;
    a1 = (int) t1;
    a2 = a - t23 * a1;

    //--------------------------------------------------------------------
    //  Generate N results.   This loop is not vectorizable.
    //--------------------------------------------------------------------
    for ( i = 0; i < n; i++ )
    {
        //--------------------------------------------------------------------
        //  Break X into two parts such that X = 2^23 * X1 + X2, compute
        //  Z = A1 * X2 + A2 * X1  (mod 2^23), and then
        //  X = 2^23 * Z + A2 * X2  (mod 2^46).
        //--------------------------------------------------------------------
        t1 = r23 * (*x);
        x1 = (int) t1;
        x2 = *x - t23 * x1;
        t1 = a1 * x2 + a2 * x1;
        t2 = (int) (r23 * t1);
        z = t1 - t23 * t2;
        t3 = t23 * z + a2 * x2;
        t4 = (int) (r46 * t3) ;
        *x = t3 - t46 * t4;
        y[i] = r46 * (*x);
    }

    return;
}



void wtime(double *t)
{
    static int sec = -1;
    struct timeval tv;
    gettimeofday(&tv, (void *)0);
    if (sec < 0) sec = tv.tv_sec;
    *t = (tv.tv_sec - sec) + 1.0e-6 * tv.tv_usec;
}
/*****************************************************************/
/******         E  L  A  P  S  E  D  _  T  I  M  E          ******/
/*****************************************************************/
double elapsed_time( void )
{
    double t;
    wtime( &t );
    return ( t );
}
/*****************************************************************/
/******            T  I  M  E  R  _  C  L  E  A  R          ******/
/*****************************************************************/
void timer_clear( int n )
{
    elapsed[n] = 0.0;
}
/*****************************************************************/
/******            T  I  M  E  R  _  S  T  A  R  T          ******/
/*****************************************************************/
void timer_start( int n )
{
    start[n] = elapsed_time();
}
/*****************************************************************/
/******            T  I  M  E  R  _  S  T  O  P             ******/
/*****************************************************************/
void timer_stop( int n )
{
    double t, now;
    now = elapsed_time();
    t = now - start[n];
    elapsed[n] += t;
}
/*****************************************************************/
/******            T  I  M  E  R  _  R  E  A  D             ******/
/*****************************************************************/
double timer_read( int n )
{
    return ( elapsed[n] );
}