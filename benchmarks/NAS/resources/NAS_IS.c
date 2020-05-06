//---------------------------------------------------------------------
// program IS
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
#   define  TOTAL_KEYS_LOG_2    16
#   define  MAX_KEY_LOG_2       11
#   define  NUM_BUCKETS_LOG_2   9
#   define CLASS 'S'
#endif
//----------
//  Class W:
//----------
#ifdef CLASS_W
#   define  TOTAL_KEYS_LOG_2    20
#   define  MAX_KEY_LOG_2       16
#   define  NUM_BUCKETS_LOG_2   10
#   define CLASS 'W'
#endif
//----------
//  Class A:
//----------
#ifdef CLASS_A
#   define  TOTAL_KEYS_LOG_2    23
#   define  MAX_KEY_LOG_2       19
#   define  NUM_BUCKETS_LOG_2   10
#   define CLASS 'A'
#endif
//----------
//  Class B:
//----------
#ifdef CLASS_B
#   define  TOTAL_KEYS_LOG_2    25
#   define  MAX_KEY_LOG_2       21
#   define  NUM_BUCKETS_LOG_2   10
#   define CLASS 'B'
#endif
//----------
//  Class C:
//----------
#ifdef CLASS_C
#   define  TOTAL_KEYS_LOG_2    27
#   define  MAX_KEY_LOG_2       23
#   define  NUM_BUCKETS_LOG_2   10
#   define CLASS 'C'
#endif
//----------
//  Class D:
//----------
#ifdef CLASS_D
#   define  TOTAL_KEYS_LOG_2    31
#   define  MAX_KEY_LOG_2       27
#   define  NUM_BUCKETS_LOG_2   10
#   define CLASS 'D'
#endif


#if CLASS == 'D'
#define  TOTAL_KEYS          (1L << TOTAL_KEYS_LOG_2)
#else
#define  TOTAL_KEYS          (1 << TOTAL_KEYS_LOG_2)
#endif
#define  MAX_KEY             (1 << MAX_KEY_LOG_2)
#define  NUM_BUCKETS         (1 << NUM_BUCKETS_LOG_2)
#define  NUM_KEYS            TOTAL_KEYS
#define  SIZE_OF_BUFFERS     NUM_KEYS


#define  MAX_ITERATIONS      10
#define  TEST_ARRAY_SIZE     5

/*************************************/
/* Typedef: if necessary, change the */
/* size of int here by changing the  */
/* int type to, say, long            */
/*************************************/
#if CLASS == 'D'
typedef  long INT_TYPE;
#else
typedef  int  INT_TYPE;
#endif

typedef struct
{
    double real;
    double imag;
} dcomplex;


#define min(x,y)    ((x) < (y) ? (x) : (y))
#define max(x,y)    ((x) > (y) ? (x) : (y))



/********************/
/* Some global info */
/********************/
INT_TYPE *key_buff_ptr_global;         /* used by full_verify to get */
/* copies of rank info        */

int      passed_verification;


/************************************/
/* These are the three main arrays. */
/* See SIZE_OF_BUFFERS def above    */
/************************************/
INT_TYPE key_array[SIZE_OF_BUFFERS],
         key_buff1[MAX_KEY],
         key_buff2[SIZE_OF_BUFFERS],
         partial_verify_vals[TEST_ARRAY_SIZE];

#ifdef USE_BUCKETS
INT_TYPE bucket_size[NUM_BUCKETS],
         bucket_ptrs[NUM_BUCKETS];
#endif


/**********************/
/* Partial verif info */
/**********************/
INT_TYPE test_index_array[TEST_ARRAY_SIZE],
         test_rank_array[TEST_ARRAY_SIZE],

         S_test_index_array[TEST_ARRAY_SIZE] =
{48427, 17148, 23627, 62548, 4431},
S_test_rank_array[TEST_ARRAY_SIZE] =
{0, 18, 346, 64917, 65463},

W_test_index_array[TEST_ARRAY_SIZE] =
{357773, 934767, 875723, 898999, 404505},
W_test_rank_array[TEST_ARRAY_SIZE] =
{1249, 11698, 1039987, 1043896, 1048018},

A_test_index_array[TEST_ARRAY_SIZE] =
{2112377, 662041, 5336171, 3642833, 4250760},
A_test_rank_array[TEST_ARRAY_SIZE] =
{104, 17523, 123928, 8288932, 8388264},

B_test_index_array[TEST_ARRAY_SIZE] =
{41869, 812306, 5102857, 18232239, 26860214},
B_test_rank_array[TEST_ARRAY_SIZE] =
{33422937, 10244, 59149, 33135281, 99},

C_test_index_array[TEST_ARRAY_SIZE] =
{44172927, 72999161, 74326391, 129606274, 21736814},
C_test_rank_array[TEST_ARRAY_SIZE] =
{61147, 882988, 266290, 133997595, 133525895},

D_test_index_array[TEST_ARRAY_SIZE] =
{1317351170, 995930646, 1157283250, 1503301535, 1453734525},
D_test_rank_array[TEST_ARRAY_SIZE] =
{1, 36538729, 1978098519, 2145192618, 2147425337};



/***********************/
/* function prototypes */
/***********************/
double  randlc( double *X, double *A );

void full_verify( void );

void c_print_results( char *name, char class, int n1, int n2, int n3, int niter,
                      double t, double mops, char *optype, int passed_verification);


double start[64], elapsed[64];
double elapsed_time( void );
void timer_clear( int n );
void timer_start( int n );
void timer_stop( int n );
double timer_read( int n );
void wtime(double *t);





/*****************************************************************/
/*************           R  A  N  D  L  C             ************/
/*************                                        ************/
/*************    portable random number generator    ************/
/*****************************************************************/

double  randlc( double *X, double *A )
{
    int        KS = 0;
    double R23, R46, T23, T46;
    double        T1, T2, T3, T4;
    double        A1;
    double        A2;
    double        X1;
    double        X2;
    double        Z;
    int           i, j;

    if (KS == 0)
    {
        R23 = 1.0;
        R46 = 1.0;
        T23 = 1.0;
        T46 = 1.0;

        for (i = 1; i <= 23; i++)
        {
            R23 = 0.50 * R23;
            T23 = 2.0 * T23;
        }
        for (i = 1; i <= 46; i++)
        {
            R46 = 0.50 * R46;
            T46 = 2.0 * T46;
        }
        KS = 1;
    }

    /*  Break A into two parts such that A = 2^23 * A1 + A2 and set X = N.  */

    T1 = R23 * *A;
    j  = T1;
    A1 = j;
    A2 = *A - T23 * A1;

    /*  Break X into two parts such that X = 2^23 * X1 + X2, compute
        Z = A1 * X2 + A2 * X1  (mod 2^23), and then
        X = 2^23 * Z + A2 * X2  (mod 2^46).                            */

    T1 = R23 * *X;
    j  = T1;
    X1 = j;
    X2 = *X - T23 * X1;
    T1 = A1 * X2 + A2 * X1;

    j  = R23 * T1;
    T2 = j;
    Z = T1 - T23 * T2;
    T3 = T23 * Z + A2 * X2;
    j  = R46 * T3;
    T4 = j;
    *X = T3 - T46 * T4;
    return (R46 * *X);
}




/*****************************************************************/
/*************      C  R  E  A  T  E  _  S  E  Q      ************/
/*****************************************************************/

void    create_seq( double seed, double a )
{
    double x;
    INT_TYPE i, k;

    k = MAX_KEY / 4;

    for (i = 0; i < NUM_KEYS; i++)
    {
        x = randlc(&seed, &a);
        x += randlc(&seed, &a);
        x += randlc(&seed, &a);
        x += randlc(&seed, &a);

        key_array[i] = k * x;
    }
}




/*****************************************************************/
/*************    F  U  L  L  _  V  E  R  I  F  Y     ************/
/*****************************************************************/


void full_verify( void )
{
    INT_TYPE    i, j;



    /*  Now, finally, sort the keys:  */

#ifdef USE_BUCKETS

    /* key_buff2[] already has the proper information, so do nothing */

#else

    /*  Copy keys into work array; keys in key_array will be reassigned. */

    for ( i = 0; i < NUM_KEYS; i++ )
        key_buff2[i] = key_array[i];

#endif

    for ( i = 0; i < NUM_KEYS; i++ )
        key_array[--key_buff_ptr_global[key_buff2[i]]] = key_buff2[i];


    /*  Confirm keys correctly sorted: count incorrectly sorted keys, if any */

    j = 0;


    for ( i = 1; i < NUM_KEYS; i++ )
        if ( key_array[i - 1] > key_array[i] )
            j++;


    if ( j != 0 )
    {
        printf( "Full_verify: number of keys out of sort: %ld\n",
                (long)j );
    }
    else
        passed_verification++;


}




/*****************************************************************/
/*************             R  A  N  K             ****************/
/*****************************************************************/


void rank( int iteration )
{

    INT_TYPE    i, k;

    INT_TYPE    *key_buff_ptr, *key_buff_ptr2;

#ifdef USE_BUCKETS
    int shift = MAX_KEY_LOG_2 - NUM_BUCKETS_LOG_2;
    INT_TYPE    key;
#endif


    key_array[iteration] = iteration;
    key_array[iteration + MAX_ITERATIONS] = MAX_KEY - iteration;


    /*  Determine where the partial verify test keys are, load into  */
    /*  top of array bucket_size                                     */

    for ( i = 0; i < TEST_ARRAY_SIZE; i++ )
        partial_verify_vals[i] = key_array[test_index_array[i]];

#ifdef USE_BUCKETS

    /*  Initialize */

    for ( i = 0; i < NUM_BUCKETS; i++ )
        bucket_size[i] = 0;

    /*  Determine the number of keys in each bucket */
    for ( i = 0; i < NUM_KEYS; i++ )
        bucket_size[key_array[i] >> shift]++;


    /*  Accumulative bucket sizes are the bucket pointers */
    bucket_ptrs[0] = 0;
    for ( i = 1; i < NUM_BUCKETS; i++ )
        bucket_ptrs[i] = bucket_ptrs[i - 1] + bucket_size[i - 1];


    /*  Sort into appropriate bucket */
    for ( i = 0; i < NUM_KEYS; i++ )
    {
        key = key_array[i];
        key_buff2[bucket_ptrs[key >> shift]++] = key;
    }

    key_buff_ptr2 = key_buff2;

#else

    key_buff_ptr2 = key_array;

#endif

    /*  Clear the work array */
    for ( i = 0; i < MAX_KEY; i++ )
        key_buff1[i] = 0;


    /*  Ranking of all keys occurs in this section:                 */

    key_buff_ptr = key_buff1;

    /*  In this section, the keys themselves are used as their
        own indexes to determine how many of each there are: their
        individual population                                       */

    for ( i = 0; i < NUM_KEYS; i++ )
        key_buff_ptr[key_buff_ptr2[i]]++;  /* Now they have individual key   */
    /* population                     */

    /*  To obtain ranks of each key, successively add the individual key
        population                                                  */


    for ( i = 0; i < MAX_KEY - 1; i++ )
        key_buff_ptr[i + 1] += key_buff_ptr[i];


    /* This is the partial verify test section */
    /* Observe that test_rank_array vals are   */
    /* shifted differently for different cases */
    for ( i = 0; i < TEST_ARRAY_SIZE; i++ )
    {
        k = partial_verify_vals[i];          /* test vals were put here */
        if ( 0 < k  &&  k <= NUM_KEYS - 1 )
        {
            INT_TYPE key_rank = key_buff_ptr[k - 1];
            int failed = 0;

            switch ( CLASS )
            {
            case 'S':
                if ( i <= 2 )
                {
                    if ( key_rank != test_rank_array[i] + iteration )
                        failed = 1;
                    else
                        passed_verification++;
                }
                else
                {
                    if ( key_rank != test_rank_array[i] - iteration )
                        failed = 1;
                    else
                        passed_verification++;
                }
                break;
            case 'W':
                if ( i < 2 )
                {
                    if ( key_rank != test_rank_array[i] + (iteration - 2) )
                        failed = 1;
                    else
                        passed_verification++;
                }
                else
                {
                    if ( key_rank != test_rank_array[i] - iteration )
                        failed = 1;
                    else
                        passed_verification++;
                }
                break;
            case 'A':
                if ( i <= 2 )
                {
                    if ( key_rank != test_rank_array[i] + (iteration - 1) )
                        failed = 1;
                    else
                        passed_verification++;
                }
                else
                {
                    if ( key_rank != test_rank_array[i] - (iteration - 1) )
                        failed = 1;
                    else
                        passed_verification++;
                }
                break;
            case 'B':
                if ( i == 1 || i == 2 || i == 4 )
                {
                    if ( key_rank != test_rank_array[i] + iteration )
                        failed = 1;
                    else
                        passed_verification++;
                }
                else
                {
                    if ( key_rank != test_rank_array[i] - iteration )
                        failed = 1;
                    else
                        passed_verification++;
                }
                break;
            case 'C':
                if ( i <= 2 )
                {
                    if ( key_rank != test_rank_array[i] + iteration )
                        failed = 1;
                    else
                        passed_verification++;
                }
                else
                {
                    if ( key_rank != test_rank_array[i] - iteration )
                        failed = 1;
                    else
                        passed_verification++;
                }
                break;
            case 'D':
                if ( i < 2 )
                {
                    if ( key_rank != test_rank_array[i] + iteration )
                        failed = 1;
                    else
                        passed_verification++;
                }
                else
                {
                    if ( key_rank != test_rank_array[i] - iteration )
                        failed = 1;
                    else
                        passed_verification++;
                }
                break;
            }
            if ( failed == 1 )
                printf( "Failed partial verification: "
                        "iteration %d, test key %d\n",
                        iteration, (int)i );
        }
    }




    /*  Make copies of rank info for use by full_verify: these variables
        in rank are local; making them global slows down the code, probably
        since they cannot be made register by compiler                        */

    if ( iteration == MAX_ITERATIONS )
        key_buff_ptr_global = key_buff_ptr;

}


/*****************************************************************/
/*************             M  A  I  N             ****************/
/*****************************************************************/

int main( int argc, char **argv )
{

    int             i, iteration;

    double          timecounter;

    FILE            *fp;


    /*  Initialize timers  */

    timer_clear( 0 );


    /*  Initialize the verification arrays if a valid class */
    for ( i = 0; i < TEST_ARRAY_SIZE; i++ )
        switch ( CLASS )
        {
        case 'S':
            test_index_array[i] = S_test_index_array[i];
            test_rank_array[i]  = S_test_rank_array[i];
            break;
        case 'A':
            test_index_array[i] = A_test_index_array[i];
            test_rank_array[i]  = A_test_rank_array[i];
            break;
        case 'W':
            test_index_array[i] = W_test_index_array[i];
            test_rank_array[i]  = W_test_rank_array[i];
            break;
        case 'B':
            test_index_array[i] = B_test_index_array[i];
            test_rank_array[i]  = B_test_rank_array[i];
            break;
        case 'C':
            test_index_array[i] = C_test_index_array[i];
            test_rank_array[i]  = C_test_rank_array[i];
            break;
        case 'D':
            test_index_array[i] = D_test_index_array[i];
            test_rank_array[i]  = D_test_rank_array[i];
            break;
        };



    /*  Printout initial NPB info */
    printf
    ( "\n\n NAS Parallel Benchmarks (NPB3.3-SER) - IS Benchmark\n\n" );
    printf( " Size:  %ld  (class %c)\n", (long)TOTAL_KEYS, CLASS );
    printf( " Iterations:   %d\n", MAX_ITERATIONS );


    /*  Generate random number sequence and subsequent keys on all procs */
    create_seq( 314159265.00,                    /* Random number gen seed */
                1220703125.00 );                 /* Random number gen mult */


    /*  Do one interation for free (i.e., untimed) to guarantee initialization of
        all data and code pages and respective tables */
    rank( 1 );

    /*  Start verification counter */
    passed_verification = 0;

    if ( CLASS != 'S' ) printf( "\n   iteration\n" );

    /*  Start timer  */
    timer_start( 0 );


    /*  This is the main iteration */
	#pragma kernel
    for ( iteration = 1; iteration <= MAX_ITERATIONS; iteration++ )
    {
        if ( CLASS != 'S' ) printf( "        %d\n", iteration );
        rank( iteration );
    }


    /*  End of timing, obtain maximum time of all processors */
    timer_stop( 0 );
    timecounter = timer_read( 0 );


    /*  This tests that keys are in sequence: sorting of last ranked key seq
        occurs here, but is an untimed operation                             */
    full_verify();



    /*  The final printout  */
    if ( passed_verification != 5 * MAX_ITERATIONS + 1 )
        passed_verification = 0;
    c_print_results( "IS",
                     CLASS,
                     (int)(TOTAL_KEYS / 64),
                     64,
                     0,
                     MAX_ITERATIONS,
                     timecounter,
                     ((double) (MAX_ITERATIONS * TOTAL_KEYS))
                     / timecounter / 1000000.,
                     "keys ranked",
                     passed_verification);




  int exitValue = passed_verification ? 0 : 1;
  return exitValue;
}
/**************************/
/*  E N D  P R O G R A M  */
/**************************/



void c_print_results( char   *name,
                      char   class,
                      int    n1,
                      int    n2,
                      int    n3,
                      int    niter,
                      double t,
                      double mops,
                      char   *optype,
                      int    passed_verification )
{
    printf( "\n\n %s Benchmark Completed\n", name );

    printf( " Class           =                        %c\n", class );

    if ( n3 == 0 )
    {
        long nn = n1;
        if ( n2 != 0 ) nn *= n2;
        printf( " Size            =             %12ld\n", nn );   /* as in IS */
    }
    else
        printf( " Size            =             %4dx%4dx%4d\n", n1, n2, n3 );

    printf( " Iterations      =             %12d\n", niter );

    printf( " Time in seconds =             %12.2f\n", t );

    printf( " Mop/s total     =             %12.2f\n", mops );

    printf( " Operation type  = %24s\n", optype);

    if ( passed_verification < 0 )
        printf( " Verification    =            NOT PERFORMED\n" );
    else if ( passed_verification )
        printf( " Verification    =               SUCCESSFUL\n" );
    else
        printf( " Verification    =             UNSUCCESSFUL\n" );



#ifdef SMP
    evalue = getenv("MP_SET_NUMTHREADS");
    printf( "   MULTICPUS = %s\n", evalue );
#endif


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