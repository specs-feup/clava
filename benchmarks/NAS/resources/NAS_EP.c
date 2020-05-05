//---------------------------------------------------------------------
// program EP
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
#   define M      24
#   define CLASS   'S'
#endif
//----------
//  Class W:
//----------
#ifdef CLASS_W
#   define M      25
#   define CLASS   'W'
#endif
//----------
//  Class A:
//----------
#ifdef CLASS_A
#   define M      28
#   define CLASS   'A'
#endif
//----------
//  Class B:
//----------
#ifdef CLASS_B
#   define M      30
#   define CLASS   'B'
#endif
//----------
//  Class C:
//----------
#ifdef CLASS_C
#   define M      32
#   define CLASS   'C'
#endif
//----------
//  Class D:
//----------
#ifdef CLASS_D
#   define M      36
#   define CLASS   'D'
#endif
//----------
//  Class E:
//----------
#ifdef CLASS_E
#   define M      40
#   define CLASS   'E'
#endif




typedef struct
{
    double real;
    double imag;
} dcomplex;


#define min(x,y)    ((x) < (y) ? (x) : (y))
#define max(x,y)    ((x) > (y) ? (x) : (y))



#define MAX(X,Y)  (((X) > (Y)) ? (X) : (Y))

#define MK        16
#define MM        (M - MK)
#define NN        (1 << MM)
#define NK        (1 << MK)
#define NQ        10
#define EPSILON   1.0e-8
#define A         1220703125.0
#define S         271828183.0


double randlc( double *x, double a );
void vranlc( int n, double *x, double a, double y[] );

void print_results(char *name, char class, int n1, int n2, int n3, int niter,
                   double t, double mops, char *optype, int verified);

double start[64], elapsed[64];
double elapsed_time( void );
void timer_clear( int n );
void timer_start( int n );
void timer_stop( int n );
double timer_read( int n );
void wtime(double *t);


int main()
{
    double Mops, t1, t2, t3, t4, x1, x2;
    double sx, sy, tm, an, tt, gc;
    double sx_verify_value, sy_verify_value, sx_err, sy_err;
    int    np;
    int    i, ik, kk, l, k, nit;
    int    k_offset, j;
    int verified;

    double dum[3] = {1.0, 1.0, 1.0};
    char   size[16];


    double x[2 * NK];
    double q[NQ];

    FILE *fp;



    //--------------------------------------------------------------------
    //  Because the size of the problem is too large to store in a 32-bit
    //  integer for some classes, we put it into a string (for printing).
    //  Have to strip off the decimal point put in there by the floating
    //  point print statement (internal file)
    //--------------------------------------------------------------------

    sprintf(size, "%15.0lf", pow(2.0, M + 1));
    j = 14;
    if (size[j] == '.') j--;
    size[j + 1] = '\0';
    printf("\n\n NAS Parallel Benchmarks (NPB3.3-SER-C) - EP Benchmark\n");
    printf("\n Number of random numbers generated: %15s\n", size);

    verified = 0;

    //--------------------------------------------------------------------
    //  Compute the number of "batches" of random number pairs generated
    //  per processor. Adjust if the number of processors does not evenly
    //  divide the total number
    //--------------------------------------------------------------------

    np = NN;

    //--------------------------------------------------------------------
    //  Call the random number generator functions and initialize
    //  the x-array to reduce the effects of paging on the timings.
    //  Also, call all mathematical functions that are used. Make
    //  sure these initializations cannot be eliminated as dead code.
    //--------------------------------------------------------------------

    vranlc(0, &dum[0], dum[1], &dum[2]);
    dum[0] = randlc(&dum[1], dum[2]);

    for (i = 0; i < 2 * NK; i++)
    {
        x[i] = -1.0e99;
    }
    Mops = log(sqrt(fabs(MAX(1.0, 1.0))));

    timer_clear(0);
    timer_clear(1);
    timer_clear(2);
    timer_start(0);

    t1 = A;
    vranlc(0, &t1, A, x);

    //--------------------------------------------------------------------
    //  Compute AN = A ^ (2 * NK) (mod 2^46).
    //--------------------------------------------------------------------

    t1 = A;

    for (i = 0; i < MK + 1; i++)
    {
        t2 = randlc(&t1, t1);
    }

    an = t1;
    tt = S;
    gc = 0.0;
    sx = 0.0;
    sy = 0.0;

    for (i = 0; i < NQ; i++)
    {
        q[i] = 0.0;
    }

    //--------------------------------------------------------------------
    //  Each instance of this loop may be performed independently. We compute
    //  the k offsets separately to take into account the fact that some nodes
    //  have more numbers to generate than others
    //--------------------------------------------------------------------

    k_offset = -1;

    for (k = 1; k <= np; k++)
    {
        kk = k_offset + k;
        t1 = S;
        t2 = an;

        // Find starting seed t1 for this kk.

        for (i = 1; i <= 100; i++)
        {
            ik = kk / 2;
            if ((2 * ik) != kk) t3 = randlc(&t1, t2);
            if (ik == 0) break;
            t3 = randlc(&t2, t2);
            kk = ik;
        }

        //--------------------------------------------------------------------
        //  Compute uniform pseudorandom numbers.
        //--------------------------------------------------------------------
        vranlc(2 * NK, &t1, A, x);

        //--------------------------------------------------------------------
        //  Compute Gaussian deviates by acceptance-rejection method and
        //  tally counts in concentri//square annuli.  This loop is not
        //  vectorizable.
        //--------------------------------------------------------------------

        for (i = 0; i < NK; i++)
        {
            x1 = 2.0 * x[2 * i] - 1.0;
            x2 = 2.0 * x[2 * i + 1] - 1.0;
            t1 = x1 * x1 + x2 * x2;
            if (t1 <= 1.0)
            {
                t2   = sqrt(-2.0 * log(t1) / t1);
                t3   = (x1 * t2);
                t4   = (x2 * t2);
                l    = MAX(fabs(t3), fabs(t4));
                q[l] = q[l] + 1.0;
                sx   = sx + t3;
                sy   = sy + t4;
            }
        }

    }

    for (i = 0; i < NQ; i++)
    {
        gc = gc + q[i];
    }

    timer_stop(0);
    tm = timer_read(0);

    nit = 0;
    verified = 1;
    if (M == 24)
    {
        sx_verify_value = -3.247834652034740e+3;
        sy_verify_value = -6.958407078382297e+3;
    }
    else if (M == 25)
    {
        sx_verify_value = -2.863319731645753e+3;
        sy_verify_value = -6.320053679109499e+3;
    }
    else if (M == 28)
    {
        sx_verify_value = -4.295875165629892e+3;
        sy_verify_value = -1.580732573678431e+4;
    }
    else if (M == 30)
    {
        sx_verify_value =  4.033815542441498e+4;
        sy_verify_value = -2.660669192809235e+4;
    }
    else if (M == 32)
    {
        sx_verify_value =  4.764367927995374e+4;
        sy_verify_value = -8.084072988043731e+4;
    }
    else if (M == 36)
    {
        sx_verify_value =  1.982481200946593e+5;
        sy_verify_value = -1.020596636361769e+5;
    }
    else if (M == 40)
    {
        sx_verify_value = -5.319717441530e+05;
        sy_verify_value = -3.688834557731e+05;
    }
    else
    {
        verified = 0;
    }

    if (verified)
    {
        sx_err = fabs((sx - sx_verify_value) / sx_verify_value);
        sy_err = fabs((sy - sy_verify_value) / sy_verify_value);
        verified = ((sx_err <= EPSILON) && (sy_err <= EPSILON));
    }

    Mops = pow(2.0, M + 1) / tm / 1000000.0;

    printf("\nEP Benchmark Results:\n\n");
    printf("CPU Time =%10.4lf\n", tm);
    printf("N = 2^%5d\n", M);
    printf("No. Gaussian Pairs = %15.0lf\n", gc);
    printf("Sums = %25.15lE %25.15lE\n", sx, sy);
    printf("Counts: \n");
    for (i = 0; i < NQ; i++)
    {
        printf("%3d%15.0lf\n", i, q[i]);
    }

    print_results("EP", CLASS, M + 1, 0, 0, nit,
                  tm, Mops,
                  "Random numbers generated",
                  verified);

  int exitValue = verified ? 0 : 1;
  return exitValue;
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