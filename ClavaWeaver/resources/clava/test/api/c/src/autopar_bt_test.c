//---------------------------------------------------------------------
// program BT
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
#   define PROBLEM_SIZE   12
#   define NITER_DEFAULT  60
#   define DT_DEFAULT     0.010
#endif
//----------
//  Class W:
//----------
#ifdef CLASS_W
#   define PROBLEM_SIZE   24
#   define NITER_DEFAULT  200
#   define DT_DEFAULT     0.0008
#endif
//----------
//  Class A:
//----------
#ifdef CLASS_A
#   define PROBLEM_SIZE   64
#   define NITER_DEFAULT  200
#   define DT_DEFAULT     0.0008
#endif
//----------
//  Class B:
//----------
#ifdef CLASS_B
#   define PROBLEM_SIZE   102
#   define NITER_DEFAULT  200
#   define DT_DEFAULT     0.0003
#endif
//----------
//  Class C:
//----------
#ifdef CLASS_C
#   define PROBLEM_SIZE   162
#   define NITER_DEFAULT  200
#   define DT_DEFAULT     0.0001
#endif
//----------
//  Class D:
//----------
#ifdef CLASS_D
#   define PROBLEM_SIZE   408
#   define NITER_DEFAULT  250
#   define DT_DEFAULT     0.00002
#endif
//----------
//  Class E:
//----------
#ifdef CLASS_E
#   define PROBLEM_SIZE   1020
#   define NITER_DEFAULT  250
#   define DT_DEFAULT     0.4e-5
#endif



#define AA            0
#define BB            1
#define CC            2
#define BLOCK_SIZE    5

#define IMAX      PROBLEM_SIZE
#define JMAX      PROBLEM_SIZE
#define KMAX      PROBLEM_SIZE
#define IMAXP     IMAX/2*2
#define JMAXP     JMAX/2*2


typedef struct
{
    double real;
    double imag;
} dcomplex;


#define min(x,y)    ((x) < (y) ? (x) : (y))
#define max(x,y)    ((x) > (y) ? (x) : (y))


/* common /global/ */
int grid_points[3];
int timeron;

/* common /constants/ */
double tx1, tx2, tx3, ty1, ty2, ty3, tz1, tz2, tz3,
       dx1, dx2, dx3, dx4, dx5, dy1, dy2, dy3, dy4,
       dy5, dz1, dz2, dz3, dz4, dz5, dssp, dt,
       ce[5][13], dxmax, dymax, dzmax, xxcon1, xxcon2,
       xxcon3, xxcon4, xxcon5, dx1tx1, dx2tx1, dx3tx1,
       dx4tx1, dx5tx1, yycon1, yycon2, yycon3, yycon4,
       yycon5, dy1ty1, dy2ty1, dy3ty1, dy4ty1, dy5ty1,
       zzcon1, zzcon2, zzcon3, zzcon4, zzcon5, dz1tz1,
       dz2tz1, dz3tz1, dz4tz1, dz5tz1, dnxm1, dnym1,
       dnzm1, c1c2, c1c5, c3c4, c1345, conz1, c1, c2,
       c3, c4, c5, c4dssp, c5dssp, dtdssp, dttx1,
       dttx2, dtty1, dtty2, dttz1, dttz2, c2dttx1,
       c2dtty1, c2dttz1, comz1, comz4, comz5, comz6,
       c3c4tx3, c3c4ty3, c3c4tz3, c2iv, con43, con16;

// to improve cache performance, grid dimensions padded by 1
// for even number sizes only.
/* common /fields/ */
double us     [KMAX][JMAXP + 1][IMAXP + 1];
double vs     [KMAX][JMAXP + 1][IMAXP + 1];
double ws     [KMAX][JMAXP + 1][IMAXP + 1];
double qs     [KMAX][JMAXP + 1][IMAXP + 1];
double rho_i  [KMAX][JMAXP + 1][IMAXP + 1];
double square [KMAX][JMAXP + 1][IMAXP + 1];
double forcing[KMAX][JMAXP + 1][IMAXP + 1][5];
double u      [KMAX][JMAXP + 1][IMAXP + 1][5];
double rhs    [KMAX][JMAXP + 1][IMAXP + 1][5];

/* common /work_1d/ */
double cuf[PROBLEM_SIZE + 1];
double q  [PROBLEM_SIZE + 1];
double ue [PROBLEM_SIZE + 1][5];
double buf[PROBLEM_SIZE + 1][5];

/* common /work_lhs/ */
double fjac[PROBLEM_SIZE + 1][5][5];
double njac[PROBLEM_SIZE + 1][5][5];
double lhs [PROBLEM_SIZE + 1][3][5][5];
double tmp1, tmp2, tmp3;


//-----------------------------------------------------------------------
// Timer constants
//-----------------------------------------------------------------------
#define t_total     1
#define t_rhsx      2
#define t_rhsy      3
#define t_rhsz      4
#define t_rhs       5
#define t_xsolve    6
#define t_ysolve    7
#define t_zsolve    8
#define t_rdis1     9
#define t_rdis2     10
#define t_add       11
#define t_last      11


void initialize();
void lhsinit(double lhs[][3][5][5], int size);
void exact_solution(double xi, double eta, double zeta, double dtemp[5]);
void exact_rhs();
void set_constants();
void adi();
void compute_rhs();
void x_solve();
void y_solve();
void matvec_sub(double ablock[5][5], double avec[5], double bvec[5]);
void matmul_sub(double ablock[5][5], double bblock[5][5], double cblock[5][5]);
void binvcrhs(double lhs[5][5], double c[5][5], double r[5]);
void binvrhs(double lhs[5][5], double r[5]);
void z_solve();
void add();
void error_norm(double rms[5]);
void rhs_norm(double rms[5]);
void verify(int no_time_steps, char *class, int *verified);

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
    int i, niter, step;
    double navg, mflops, n3;

    double tmax, t, trecs[t_last + 1];
    int verified;
    char Class;
    char *t_names[t_last + 1];

    //---------------------------------------------------------------------
    // Root node reads input file (if it exists) else takes
    // defaults from parameters
    //---------------------------------------------------------------------
    FILE *fp;
    if ((fp = fopen("timer.flag", "r")) != NULL)
    {
        timeron = 1;
        t_names[t_total] = "total";
        t_names[t_rhsx] = "rhsx";
        t_names[t_rhsy] = "rhsy";
        t_names[t_rhsz] = "rhsz";
        t_names[t_rhs] = "rhs";
        t_names[t_xsolve] = "xsolve";
        t_names[t_ysolve] = "ysolve";
        t_names[t_zsolve] = "zsolve";
        t_names[t_rdis1] = "redist1";
        t_names[t_rdis2] = "redist2";
        t_names[t_add] = "add";
        fclose(fp);
    }
    else
    {
        timeron = 0;
    }

    printf("\n\n NAS Parallel Benchmarks (NPB3.3-SER-C) - BT Benchmark\n\n");

    if ((fp = fopen("inputbt.data", "r")) != NULL)
    {
        int result;
        printf(" Reading from input file inputbt.data\n");
        result = fscanf(fp, "%d", &niter);
        while (fgetc(fp) != '\n');
        result = fscanf(fp, "%lf", &dt);
        while (fgetc(fp) != '\n');
        result = fscanf(fp, "%d%d%d\n",
                        &grid_points[0], &grid_points[1], &grid_points[2]);
        fclose(fp);
    }
    else
    {
        printf(" No input file inputbt.data. Using compiled defaults\n");
        niter = NITER_DEFAULT;
        dt    = DT_DEFAULT;
        grid_points[0] = PROBLEM_SIZE;
        grid_points[1] = PROBLEM_SIZE;
        grid_points[2] = PROBLEM_SIZE;
    }

    printf(" Size: %4dx%4dx%4d\n",
           grid_points[0], grid_points[1], grid_points[2]);
    printf(" Iterations: %4d    dt: %10.6f\n", niter, dt);
    printf("\n");

    if ( (grid_points[0] > IMAX) ||
            (grid_points[1] > JMAX) ||
            (grid_points[2] > KMAX) )
    {
        printf(" %d, %d, %d\n", grid_points[0], grid_points[1], grid_points[2]);
        printf(" Problem size too big for compiled array sizes\n");
        return 0;
    }

    set_constants();

    for (i = 1; i <= t_last; i++)
    {
        timer_clear(i);
    }

    initialize();

    exact_rhs();

    //---------------------------------------------------------------------
    // do one time step to touch all code, and reinitialize
    //---------------------------------------------------------------------
    adi();
    initialize();

    for (i = 1; i <= t_last; i++)
    {
        timer_clear(i);
    }
    timer_start(1);

    for (step = 1; step <= niter; step++)
    {
        if ((step % 20) == 0 || step == 1)
        {
            printf(" Time step %4d\n", step);
        }

        adi();
    }

    timer_stop(1);
    tmax = timer_read(1);

    verify(niter, &Class, &verified);

    n3 = 1.0 * grid_points[0] * grid_points[1] * grid_points[2];
    navg = (grid_points[0] + grid_points[1] + grid_points[2]) / 3.0;
    if (tmax != 0.0)
    {
        mflops = 1.0e-6 * (double)niter *
                 (3478.8 * n3 - 17655.7 * (navg * navg) + 28023.7 * navg)
                 / tmax;
    }
    else
    {
        mflops = 0.0;
    }
    print_results("BT", Class, grid_points[0],
                  grid_points[1], grid_points[2], niter,
                  tmax, mflops, "          floating point",
                  verified);

    //---------------------------------------------------------------------
    // More timers
    //---------------------------------------------------------------------
    if (timeron)
    {
        for (i = 1; i <= t_last; i++)
        {
            trecs[i] = timer_read(i);
        }

        if (tmax == 0.0) tmax = 1.0;
        printf("  SECTION   Time (secs)\n");
        for (i = 1; i <= t_last; i++)
        {
            printf("  %-8s:%9.3f  (%6.2f%%)\n",
                   t_names[i], trecs[i], trecs[i] * 100. / tmax);
            if (i == t_rhs)
            {
                t = trecs[t_rhsx] + trecs[t_rhsy] + trecs[t_rhsz];
                printf("    --> %8s:%9.3f  (%6.2f%%)\n", "sub-rhs", t, t * 100. / tmax);
                t = trecs[t_rhs] - t;
                printf("    --> %8s:%9.3f  (%6.2f%%)\n", "rest-rhs", t, t * 100. / tmax);
            }
            else if (i == t_zsolve)
            {
                t = trecs[t_zsolve] - trecs[t_rdis1] - trecs[t_rdis2];
                printf("    --> %8s:%9.3f  (%6.2f%%)\n", "sub-zsol", t, t * 100. / tmax);
            }
            else if (i == t_rdis2)
            {
                t = trecs[t_rdis1] + trecs[t_rdis2];
                printf("    --> %8s:%9.3f  (%6.2f%%)\n", "redist", t, t * 100. / tmax);
            }
        }
    }

    return 0;
}



void adi()
{
    compute_rhs();

    x_solve();

    y_solve();

    z_solve();

    add();
}
//---------------------------------------------------------------------
// addition of update to the vector u
//---------------------------------------------------------------------
void add()
{
    int i, j, k, m;

    if (timeron) timer_start(t_add);
    for (k = 1; k <= grid_points[2] - 2; k++)
    {
        for (j = 1; j <= grid_points[1] - 2; j++)
        {
            for (i = 1; i <= grid_points[0] - 2; i++)
            {
                for (m = 0; m < 5; m++)
                {
                    u[k][j][i][m] = u[k][j][i][m] + rhs[k][j][i][m];
                }
            }
        }
    }
    if (timeron) timer_stop(t_add);
}


//---------------------------------------------------------------------
// this function computes the norm of the difference between the
// computed solution and the exact solution
//---------------------------------------------------------------------
void error_norm(double rms[5])
{
    int i, j, k, m, d;
    double xi, eta, zeta, u_exact[5], add;

    for (m = 0; m < 5; m++)
    {
        rms[m] = 0.0;
    }

    for (k = 0; k <= grid_points[2] - 1; k++)
    {
        zeta = (double)(k) * dnzm1;
        for (j = 0; j <= grid_points[1] - 1; j++)
        {
            eta = (double)(j) * dnym1;
            for (i = 0; i <= grid_points[0] - 1; i++)
            {
                xi = (double)(i) * dnxm1;
                exact_solution(xi, eta, zeta, u_exact);

                for (m = 0; m < 5; m++)
                {
                    add = u[k][j][i][m] - u_exact[m];
                    rms[m] = rms[m] + add * add;
                }
            }
        }
    }

    for (m = 0; m < 5; m++)
    {
        for (d = 0; d < 3; d++)
        {
            rms[m] = rms[m] / (double)(grid_points[d] - 2);
        }
        rms[m] = sqrt(rms[m]);
    }
}


void rhs_norm(double rms[5])
{
    int i, j, k, d, m;
    double add;

    for (m = 0; m < 5; m++)
    {
        rms[m] = 0.0;
    }

    for (k = 1; k <= grid_points[2] - 2; k++)
    {
        for (j = 1; j <= grid_points[1] - 2; j++)
        {
            for (i = 1; i <= grid_points[0] - 2; i++)
            {
                for (m = 0; m < 5; m++)
                {
                    add = rhs[k][j][i][m];
                    rms[m] = rms[m] + add * add;
                }
            }
        }
    }

    for (m = 0; m < 5; m++)
    {
        for (d = 0; d < 3; d++)
        {
            rms[m] = rms[m] / (double)(grid_points[d] - 2);
        }
        rms[m] = sqrt(rms[m]);
    }
}


//---------------------------------------------------------------------
// compute the right hand side based on exact solution
//---------------------------------------------------------------------
void exact_rhs()
{
    double dtemp[5], xi, eta, zeta, dtpp;
    int m, i, j, k, ip1, im1, jp1, jm1, km1, kp1;

    //---------------------------------------------------------------------
    // initialize
    //---------------------------------------------------------------------
    for (k = 0; k <= grid_points[2] - 1; k++)
    {
        for (j = 0; j <= grid_points[1] - 1; j++)
        {
            for (i = 0; i <= grid_points[0] - 1; i++)
            {
                for (m = 0; m < 5; m++)
                {
                    forcing[k][j][i][m] = 0.0;
                }
            }
        }
    }

    //---------------------------------------------------------------------
    // xi-direction flux differences
    //---------------------------------------------------------------------
    for (k = 1; k <= grid_points[2] - 2; k++)
    {
        zeta = (double)(k) * dnzm1;
        for (j = 1; j <= grid_points[1] - 2; j++)
        {
            eta = (double)(j) * dnym1;

            for (i = 0; i <= grid_points[0] - 1; i++)
            {
                xi = (double)(i) * dnxm1;

                exact_solution(xi, eta, zeta, dtemp);
                for (m = 0; m < 5; m++)
                {
                    ue[i][m] = dtemp[m];
                }

                dtpp = 1.0 / dtemp[0];

                for (m = 1; m < 5; m++)
                {
                    buf[i][m] = dtpp * dtemp[m];
                }

                cuf[i]    = buf[i][1] * buf[i][1];
                buf[i][0] = cuf[i] + buf[i][2] * buf[i][2] + buf[i][3] * buf[i][3];
                q[i] = 0.5 * (buf[i][1] * ue[i][1] + buf[i][2] * ue[i][2] +
                              buf[i][3] * ue[i][3]);
            }

            for (i = 1; i <= grid_points[0] - 2; i++)
            {
                im1 = i - 1;
                ip1 = i + 1;

                forcing[k][j][i][0] = forcing[k][j][i][0] -
                                      tx2 * ( ue[ip1][1] - ue[im1][1] ) +
                                      dx1tx1 * (ue[ip1][0] - 2.0 * ue[i][0] + ue[im1][0]);

                forcing[k][j][i][1] = forcing[k][j][i][1] - tx2 * (
                                          (ue[ip1][1] * buf[ip1][1] + c2 * (ue[ip1][4] - q[ip1])) -
                                          (ue[im1][1] * buf[im1][1] + c2 * (ue[im1][4] - q[im1]))) +
                                      xxcon1 * (buf[ip1][1] - 2.0 * buf[i][1] + buf[im1][1]) +
                                      dx2tx1 * ( ue[ip1][1] - 2.0 * ue[i][1] + ue[im1][1]);

                forcing[k][j][i][2] = forcing[k][j][i][2] - tx2 * (
                                          ue[ip1][2] * buf[ip1][1] - ue[im1][2] * buf[im1][1]) +
                                      xxcon2 * (buf[ip1][2] - 2.0 * buf[i][2] + buf[im1][2]) +
                                      dx3tx1 * ( ue[ip1][2] - 2.0 * ue[i][2] + ue[im1][2]);

                forcing[k][j][i][3] = forcing[k][j][i][3] - tx2 * (
                                          ue[ip1][3] * buf[ip1][1] - ue[im1][3] * buf[im1][1]) +
                                      xxcon2 * (buf[ip1][3] - 2.0 * buf[i][3] + buf[im1][3]) +
                                      dx4tx1 * ( ue[ip1][3] - 2.0 * ue[i][3] + ue[im1][3]);

                forcing[k][j][i][4] = forcing[k][j][i][4] - tx2 * (
                                          buf[ip1][1] * (c1 * ue[ip1][4] - c2 * q[ip1]) -
                                          buf[im1][1] * (c1 * ue[im1][4] - c2 * q[im1])) +
                                      0.5 * xxcon3 * (buf[ip1][0] - 2.0 * buf[i][0] +
                                                      buf[im1][0]) +
                                      xxcon4 * (cuf[ip1] - 2.0 * cuf[i] + cuf[im1]) +
                                      xxcon5 * (buf[ip1][4] - 2.0 * buf[i][4] + buf[im1][4]) +
                                      dx5tx1 * ( ue[ip1][4] - 2.0 * ue[i][4] + ue[im1][4]);
            }

            //---------------------------------------------------------------------
            // Fourth-order dissipation
            //---------------------------------------------------------------------
            for (m = 0; m < 5; m++)
            {
                i = 1;
                forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                      (5.0 * ue[i][m] - 4.0 * ue[i + 1][m] + ue[i + 2][m]);
                i = 2;
                forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                      (-4.0 * ue[i - 1][m] + 6.0 * ue[i][m] -
                                       4.0 * ue[i + 1][m] +     ue[i + 2][m]);
            }

            for (i = 3; i <= grid_points[0] - 4; i++)
            {
                for (m = 0; m < 5; m++)
                {
                    forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                          (ue[i - 2][m] - 4.0 * ue[i - 1][m] +
                                           6.0 * ue[i][m] - 4.0 * ue[i + 1][m] + ue[i + 2][m]);
                }
            }

            for (m = 0; m < 5; m++)
            {
                i = grid_points[0] - 3;
                forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                      (ue[i - 2][m] - 4.0 * ue[i - 1][m] +
                                       6.0 * ue[i][m] - 4.0 * ue[i + 1][m]);
                i = grid_points[0] - 2;
                forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                      (ue[i - 2][m] - 4.0 * ue[i - 1][m] + 5.0 * ue[i][m]);
            }
        }
    }

    //---------------------------------------------------------------------
    // eta-direction flux differences
    //---------------------------------------------------------------------
    for (k = 1; k <= grid_points[2] - 2; k++)
    {
        zeta = (double)(k) * dnzm1;
        for (i = 1; i <= grid_points[0] - 2; i++)
        {
            xi = (double)(i) * dnxm1;

            for (j = 0; j <= grid_points[1] - 1; j++)
            {
                eta = (double)(j) * dnym1;

                exact_solution(xi, eta, zeta, dtemp);
                for (m = 0; m < 5; m++)
                {
                    ue[j][m] = dtemp[m];
                }

                dtpp = 1.0 / dtemp[0];

                for (m = 1; m < 5; m++)
                {
                    buf[j][m] = dtpp * dtemp[m];
                }

                cuf[j]    = buf[j][2] * buf[j][2];
                buf[j][0] = cuf[j] + buf[j][1] * buf[j][1] + buf[j][3] * buf[j][3];
                q[j] = 0.5 * (buf[j][1] * ue[j][1] + buf[j][2] * ue[j][2] +
                              buf[j][3] * ue[j][3]);
            }

            for (j = 1; j <= grid_points[1] - 2; j++)
            {
                jm1 = j - 1;
                jp1 = j + 1;

                forcing[k][j][i][0] = forcing[k][j][i][0] -
                                      ty2 * ( ue[jp1][2] - ue[jm1][2] ) +
                                      dy1ty1 * (ue[jp1][0] - 2.0 * ue[j][0] + ue[jm1][0]);

                forcing[k][j][i][1] = forcing[k][j][i][1] - ty2 * (
                                          ue[jp1][1] * buf[jp1][2] - ue[jm1][1] * buf[jm1][2]) +
                                      yycon2 * (buf[jp1][1] - 2.0 * buf[j][1] + buf[jm1][1]) +
                                      dy2ty1 * ( ue[jp1][1] - 2.0 * ue[j][1] + ue[jm1][1]);

                forcing[k][j][i][2] = forcing[k][j][i][2] - ty2 * (
                                          (ue[jp1][2] * buf[jp1][2] + c2 * (ue[jp1][4] - q[jp1])) -
                                          (ue[jm1][2] * buf[jm1][2] + c2 * (ue[jm1][4] - q[jm1]))) +
                                      yycon1 * (buf[jp1][2] - 2.0 * buf[j][2] + buf[jm1][2]) +
                                      dy3ty1 * ( ue[jp1][2] - 2.0 * ue[j][2] + ue[jm1][2]);

                forcing[k][j][i][3] = forcing[k][j][i][3] - ty2 * (
                                          ue[jp1][3] * buf[jp1][2] - ue[jm1][3] * buf[jm1][2]) +
                                      yycon2 * (buf[jp1][3] - 2.0 * buf[j][3] + buf[jm1][3]) +
                                      dy4ty1 * ( ue[jp1][3] - 2.0 * ue[j][3] + ue[jm1][3]);

                forcing[k][j][i][4] = forcing[k][j][i][4] - ty2 * (
                                          buf[jp1][2] * (c1 * ue[jp1][4] - c2 * q[jp1]) -
                                          buf[jm1][2] * (c1 * ue[jm1][4] - c2 * q[jm1])) +
                                      0.5 * yycon3 * (buf[jp1][0] - 2.0 * buf[j][0] +
                                                      buf[jm1][0]) +
                                      yycon4 * (cuf[jp1] - 2.0 * cuf[j] + cuf[jm1]) +
                                      yycon5 * (buf[jp1][4] - 2.0 * buf[j][4] + buf[jm1][4]) +
                                      dy5ty1 * (ue[jp1][4] - 2.0 * ue[j][4] + ue[jm1][4]);
            }

            //---------------------------------------------------------------------
            // Fourth-order dissipation
            //---------------------------------------------------------------------
            for (m = 0; m < 5; m++)
            {
                j = 1;
                forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                      (5.0 * ue[j][m] - 4.0 * ue[j + 1][m] + ue[j + 2][m]);
                j = 2;
                forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                      (-4.0 * ue[j - 1][m] + 6.0 * ue[j][m] -
                                       4.0 * ue[j + 1][m] +       ue[j + 2][m]);
            }

            for (j = 3; j <= grid_points[1] - 4; j++)
            {
                for (m = 0; m < 5; m++)
                {
                    forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                          (ue[j - 2][m] - 4.0 * ue[j - 1][m] +
                                           6.0 * ue[j][m] - 4.0 * ue[j + 1][m] + ue[j + 2][m]);
                }
            }

            for (m = 0; m < 5; m++)
            {
                j = grid_points[1] - 3;
                forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                      (ue[j - 2][m] - 4.0 * ue[j - 1][m] +
                                       6.0 * ue[j][m] - 4.0 * ue[j + 1][m]);
                j = grid_points[1] - 2;
                forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                      (ue[j - 2][m] - 4.0 * ue[j - 1][m] + 5.0 * ue[j][m]);
            }
        }
    }

    //---------------------------------------------------------------------
    // zeta-direction flux differences
    //---------------------------------------------------------------------
    for (j = 1; j <= grid_points[1] - 2; j++)
    {
        eta = (double)(j) * dnym1;
        for (i = 1; i <= grid_points[0] - 2; i++)
        {
            xi = (double)(i) * dnxm1;

            for (k = 0; k <= grid_points[2] - 1; k++)
            {
                zeta = (double)(k) * dnzm1;

                exact_solution(xi, eta, zeta, dtemp);
                for (m = 0; m < 5; m++)
                {
                    ue[k][m] = dtemp[m];
                }

                dtpp = 1.0 / dtemp[0];

                for (m = 1; m < 5; m++)
                {
                    buf[k][m] = dtpp * dtemp[m];
                }

                cuf[k]    = buf[k][3] * buf[k][3];
                buf[k][0] = cuf[k] + buf[k][1] * buf[k][1] + buf[k][2] * buf[k][2];
                q[k] = 0.5 * (buf[k][1] * ue[k][1] + buf[k][2] * ue[k][2] +
                              buf[k][3] * ue[k][3]);
            }

            for (k = 1; k <= grid_points[2] - 2; k++)
            {
                km1 = k - 1;
                kp1 = k + 1;

                forcing[k][j][i][0] = forcing[k][j][i][0] -
                                      tz2 * ( ue[kp1][3] - ue[km1][3] ) +
                                      dz1tz1 * (ue[kp1][0] - 2.0 * ue[k][0] + ue[km1][0]);

                forcing[k][j][i][1] = forcing[k][j][i][1] - tz2 * (
                                          ue[kp1][1] * buf[kp1][3] - ue[km1][1] * buf[km1][3]) +
                                      zzcon2 * (buf[kp1][1] - 2.0 * buf[k][1] + buf[km1][1]) +
                                      dz2tz1 * ( ue[kp1][1] - 2.0 * ue[k][1] + ue[km1][1]);

                forcing[k][j][i][2] = forcing[k][j][i][2] - tz2 * (
                                          ue[kp1][2] * buf[kp1][3] - ue[km1][2] * buf[km1][3]) +
                                      zzcon2 * (buf[kp1][2] - 2.0 * buf[k][2] + buf[km1][2]) +
                                      dz3tz1 * (ue[kp1][2] - 2.0 * ue[k][2] + ue[km1][2]);

                forcing[k][j][i][3] = forcing[k][j][i][3] - tz2 * (
                                          (ue[kp1][3] * buf[kp1][3] + c2 * (ue[kp1][4] - q[kp1])) -
                                          (ue[km1][3] * buf[km1][3] + c2 * (ue[km1][4] - q[km1]))) +
                                      zzcon1 * (buf[kp1][3] - 2.0 * buf[k][3] + buf[km1][3]) +
                                      dz4tz1 * ( ue[kp1][3] - 2.0 * ue[k][3] + ue[km1][3]);

                forcing[k][j][i][4] = forcing[k][j][i][4] - tz2 * (
                                          buf[kp1][3] * (c1 * ue[kp1][4] - c2 * q[kp1]) -
                                          buf[km1][3] * (c1 * ue[km1][4] - c2 * q[km1])) +
                                      0.5 * zzcon3 * (buf[kp1][0] - 2.0 * buf[k][0]
                                                      + buf[km1][0]) +
                                      zzcon4 * (cuf[kp1] - 2.0 * cuf[k] + cuf[km1]) +
                                      zzcon5 * (buf[kp1][4] - 2.0 * buf[k][4] + buf[km1][4]) +
                                      dz5tz1 * ( ue[kp1][4] - 2.0 * ue[k][4] + ue[km1][4]);
            }

            //---------------------------------------------------------------------
            // Fourth-order dissipation
            //---------------------------------------------------------------------
            for (m = 0; m < 5; m++)
            {
                k = 1;
                forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                      (5.0 * ue[k][m] - 4.0 * ue[k + 1][m] + ue[k + 2][m]);
                k = 2;
                forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                      (-4.0 * ue[k - 1][m] + 6.0 * ue[k][m] -
                                       4.0 * ue[k + 1][m] +       ue[k + 2][m]);
            }

            for (k = 3; k <= grid_points[2] - 4; k++)
            {
                for (m = 0; m < 5; m++)
                {
                    forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                          (ue[k - 2][m] - 4.0 * ue[k - 1][m] +
                                           6.0 * ue[k][m] - 4.0 * ue[k + 1][m] + ue[k + 2][m]);
                }
            }

            for (m = 0; m < 5; m++)
            {
                k = grid_points[2] - 3;
                forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                      (ue[k - 2][m] - 4.0 * ue[k - 1][m] +
                                       6.0 * ue[k][m] - 4.0 * ue[k + 1][m]);
                k = grid_points[2] - 2;
                forcing[k][j][i][m] = forcing[k][j][i][m] - dssp *
                                      (ue[k - 2][m] - 4.0 * ue[k - 1][m] + 5.0 * ue[k][m]);
            }

        }
    }

    //---------------------------------------------------------------------
    // now change the sign of the forcing function,
    //---------------------------------------------------------------------
    for (k = 1; k <= grid_points[2] - 2; k++)
    {
        for (j = 1; j <= grid_points[1] - 2; j++)
        {
            for (i = 1; i <= grid_points[0] - 2; i++)
            {
                for (m = 0; m < 5; m++)
                {
                    forcing[k][j][i][m] = -1.0 * forcing[k][j][i][m];
                }
            }
        }
    }
}

//---------------------------------------------------------------------
// this function returns the exact solution at point xi, eta, zeta
//---------------------------------------------------------------------
void exact_solution(double xi, double eta, double zeta, double dtemp[5])
{
    int m;

    for (m = 0; m < 5; m++)
    {
        dtemp[m] =  ce[m][0] +
                    xi * (ce[m][1] + xi * (ce[m][4] + xi * (ce[m][7] + xi * ce[m][10]))) +
                    eta * (ce[m][2] + eta * (ce[m][5] + eta * (ce[m][8] + eta * ce[m][11]))) +
                    zeta * (ce[m][3] + zeta * (ce[m][6] + zeta * (ce[m][9] +
                                               zeta * ce[m][12])));
    }
}

//---------------------------------------------------------------------
// This subroutine initializes the field variable u using
// tri-linear transfinite interpolation of the boundary values
//---------------------------------------------------------------------
void initialize()
{
    int i, j, k, m, ix, iy, iz;
    double xi, eta, zeta, Pface[2][3][5], Pxi, Peta, Pzeta, temp[5];

    //---------------------------------------------------------------------
    // Later (in compute_rhs) we compute 1/u for every element. A few of
    // the corner elements are not used, but it convenient (and faster)
    // to compute the whole thing with a simple loop. Make sure those
    // values are nonzero by initializing the whole thing here.
    //---------------------------------------------------------------------
    for (k = 0; k <= grid_points[2] - 1; k++)
    {
        for (j = 0; j <= grid_points[1] - 1; j++)
        {
            for (i = 0; i <= grid_points[0] - 1; i++)
            {
                for (m = 0; m < 5; m++)
                {
                    u[k][j][i][m] = 1.0;
                }
            }
        }
    }

    //---------------------------------------------------------------------
    // first store the "interpolated" values everywhere on the grid
    //---------------------------------------------------------------------
    for (k = 0; k <= grid_points[2] - 1; k++)
    {
        zeta = (double)(k) * dnzm1;
        for (j = 0; j <= grid_points[1] - 1; j++)
        {
            eta = (double)(j) * dnym1;
            for (i = 0; i <= grid_points[0] - 1; i++)
            {
                xi = (double)(i) * dnxm1;

                for (ix = 0; ix < 2; ix++)
                {
                    exact_solution((double)ix, eta, zeta, &Pface[ix][0][0]);
                }

                for (iy = 0; iy < 2; iy++)
                {
                    exact_solution(xi, (double)iy , zeta, &Pface[iy][1][0]);
                }

                for (iz = 0; iz < 2; iz++)
                {
                    exact_solution(xi, eta, (double)iz, &Pface[iz][2][0]);
                }

                for (m = 0; m < 5; m++)
                {
                    Pxi   = xi   * Pface[1][0][m] + (1.0 - xi)   * Pface[0][0][m];
                    Peta  = eta  * Pface[1][1][m] + (1.0 - eta)  * Pface[0][1][m];
                    Pzeta = zeta * Pface[1][2][m] + (1.0 - zeta) * Pface[0][2][m];

                    u[k][j][i][m] = Pxi + Peta + Pzeta -
                                    Pxi * Peta - Pxi * Pzeta - Peta * Pzeta +
                                    Pxi * Peta * Pzeta;
                }
            }
        }
    }

    //---------------------------------------------------------------------
    // now store the exact values on the boundaries
    //---------------------------------------------------------------------

    //---------------------------------------------------------------------
    // west face
    //---------------------------------------------------------------------
    i = 0;
    xi = 0.0;
    for (k = 0; k <= grid_points[2] - 1; k++)
    {
        zeta = (double)(k) * dnzm1;
        for (j = 0; j <= grid_points[1] - 1; j++)
        {
            eta = (double)(j) * dnym1;
            exact_solution(xi, eta, zeta, temp);
            for (m = 0; m < 5; m++)
            {
                u[k][j][i][m] = temp[m];
            }
        }
    }

    //---------------------------------------------------------------------
    // east face
    //---------------------------------------------------------------------
    i = grid_points[0] - 1;
    xi = 1.0;
    for (k = 0; k <= grid_points[2] - 1; k++)
    {
        zeta = (double)(k) * dnzm1;
        for (j = 0; j <= grid_points[1] - 1; j++)
        {
            eta = (double)(j) * dnym1;
            exact_solution(xi, eta, zeta, temp);
            for (m = 0; m < 5; m++)
            {
                u[k][j][i][m] = temp[m];
            }
        }
    }

    //---------------------------------------------------------------------
    // south face
    //---------------------------------------------------------------------
    j = 0;
    eta = 0.0;
    for (k = 0; k <= grid_points[2] - 1; k++)
    {
        zeta = (double)(k) * dnzm1;
        for (i = 0; i <= grid_points[0] - 1; i++)
        {
            xi = (double)(i) * dnxm1;
            exact_solution(xi, eta, zeta, temp);
            for (m = 0; m < 5; m++)
            {
                u[k][j][i][m] = temp[m];
            }
        }
    }

    //---------------------------------------------------------------------
    // north face
    //---------------------------------------------------------------------
    j = grid_points[1] - 1;
    eta = 1.0;
    for (k = 0; k <= grid_points[2] - 1; k++)
    {
        zeta = (double)(k) * dnzm1;
        for (i = 0; i <= grid_points[0] - 1; i++)
        {
            xi = (double)(i) * dnxm1;
            exact_solution(xi, eta, zeta, temp);
            for (m = 0; m < 5; m++)
            {
                u[k][j][i][m] = temp[m];
            }
        }
    }

    //---------------------------------------------------------------------
    // bottom face
    //---------------------------------------------------------------------
    k = 0;
    zeta = 0.0;
    for (j = 0; j <= grid_points[1] - 1; j++)
    {
        eta = (double)(j) * dnym1;
        for (i = 0; i <= grid_points[0] - 1; i++)
        {
            xi = (double)(i) * dnxm1;
            exact_solution(xi, eta, zeta, temp);
            for (m = 0; m < 5; m++)
            {
                u[k][j][i][m] = temp[m];
            }
        }
    }

    //---------------------------------------------------------------------
    // top face
    //---------------------------------------------------------------------
    k = grid_points[2] - 1;
    zeta = 1.0;
    for (j = 0; j <= grid_points[1] - 1; j++)
    {
        eta = (double)(j) * dnym1;
        for (i = 0; i <= grid_points[0] - 1; i++)
        {
            xi = (double)(i) * dnxm1;
            exact_solution(xi, eta, zeta, temp);
            for (m = 0; m < 5; m++)
            {
                u[k][j][i][m] = temp[m];
            }
        }
    }
}


void lhsinit(double lhs[][3][5][5], int size)
{
    int i, m, n;

    i = size;
    //---------------------------------------------------------------------
    // zero the whole left hand side for starters
    //---------------------------------------------------------------------
    for (n = 0; n < 5; n++)
    {
        for (m = 0; m < 5; m++)
        {
            lhs[0][0][n][m] = 0.0;
            lhs[0][1][n][m] = 0.0;
            lhs[0][2][n][m] = 0.0;
            lhs[i][0][n][m] = 0.0;
            lhs[i][1][n][m] = 0.0;
            lhs[i][2][n][m] = 0.0;
        }
    }

    //---------------------------------------------------------------------
    // next, set all diagonal values to 1. This is overkill, but convenient
    //---------------------------------------------------------------------
    for (m = 0; m < 5; m++)
    {
        lhs[0][1][m][m] = 1.0;
        lhs[i][1][m][m] = 1.0;
    }
}

void compute_rhs()
{
    int i, j, k, m;
    double rho_inv, uijk, up1, um1, vijk, vp1, vm1, wijk, wp1, wm1;

    if (timeron) timer_start(t_rhs);
    //---------------------------------------------------------------------
    // compute the reciprocal of density, and the kinetic energy,
    // and the speed of sound.
    //---------------------------------------------------------------------
    for (k = 0; k <= grid_points[2] - 1; k++)
    {
        for (j = 0; j <= grid_points[1] - 1; j++)
        {
            for (i = 0; i <= grid_points[0] - 1; i++)
            {
                rho_inv = 1.0 / u[k][j][i][0];
                rho_i[k][j][i] = rho_inv;
                us[k][j][i] = u[k][j][i][1] * rho_inv;
                vs[k][j][i] = u[k][j][i][2] * rho_inv;
                ws[k][j][i] = u[k][j][i][3] * rho_inv;
                square[k][j][i] = 0.5 * (
                                      u[k][j][i][1] * u[k][j][i][1] +
                                      u[k][j][i][2] * u[k][j][i][2] +
                                      u[k][j][i][3] * u[k][j][i][3] ) * rho_inv;
                qs[k][j][i] = square[k][j][i] * rho_inv;
            }
        }
    }

    //---------------------------------------------------------------------
    // copy the exact forcing term to the right hand side;  because
    // this forcing term is known, we can store it on the whole grid
    // including the boundary
    //---------------------------------------------------------------------
    for (k = 0; k <= grid_points[2] - 1; k++)
    {
        for (j = 0; j <= grid_points[1] - 1; j++)
        {
            for (i = 0; i <= grid_points[0] - 1; i++)
            {
                for (m = 0; m < 5; m++)
                {
                    rhs[k][j][i][m] = forcing[k][j][i][m];
                }
            }
        }
    }

    if (timeron) timer_start(t_rhsx);
    //---------------------------------------------------------------------
    // compute xi-direction fluxes
    //---------------------------------------------------------------------
    for (k = 1; k <= grid_points[2] - 2; k++)
    {
        for (j = 1; j <= grid_points[1] - 2; j++)
        {
            for (i = 1; i <= grid_points[0] - 2; i++)
            {
                uijk = us[k][j][i];
                up1  = us[k][j][i + 1];
                um1  = us[k][j][i - 1];

                rhs[k][j][i][0] = rhs[k][j][i][0] + dx1tx1 *
                                  (u[k][j][i + 1][0] - 2.0 * u[k][j][i][0] +
                                   u[k][j][i - 1][0]) -
                                  tx2 * (u[k][j][i + 1][1] - u[k][j][i - 1][1]);

                rhs[k][j][i][1] = rhs[k][j][i][1] + dx2tx1 *
                                  (u[k][j][i + 1][1] - 2.0 * u[k][j][i][1] +
                                   u[k][j][i - 1][1]) +
                                  xxcon2 * con43 * (up1 - 2.0 * uijk + um1) -
                                  tx2 * (u[k][j][i + 1][1] * up1 -
                                         u[k][j][i - 1][1] * um1 +
                                         (u[k][j][i + 1][4] - square[k][j][i + 1] -
                                          u[k][j][i - 1][4] + square[k][j][i - 1]) *
                                         c2);

                rhs[k][j][i][2] = rhs[k][j][i][2] + dx3tx1 *
                                  (u[k][j][i + 1][2] - 2.0 * u[k][j][i][2] +
                                   u[k][j][i - 1][2]) +
                                  xxcon2 * (vs[k][j][i + 1] - 2.0 * vs[k][j][i] +
                                            vs[k][j][i - 1]) -
                                  tx2 * (u[k][j][i + 1][2] * up1 -
                                         u[k][j][i - 1][2] * um1);

                rhs[k][j][i][3] = rhs[k][j][i][3] + dx4tx1 *
                                  (u[k][j][i + 1][3] - 2.0 * u[k][j][i][3] +
                                   u[k][j][i - 1][3]) +
                                  xxcon2 * (ws[k][j][i + 1] - 2.0 * ws[k][j][i] +
                                            ws[k][j][i - 1]) -
                                  tx2 * (u[k][j][i + 1][3] * up1 -
                                         u[k][j][i - 1][3] * um1);

                rhs[k][j][i][4] = rhs[k][j][i][4] + dx5tx1 *
                                  (u[k][j][i + 1][4] - 2.0 * u[k][j][i][4] +
                                   u[k][j][i - 1][4]) +
                                  xxcon3 * (qs[k][j][i + 1] - 2.0 * qs[k][j][i] +
                                            qs[k][j][i - 1]) +
                                  xxcon4 * (up1 * up1 -       2.0 * uijk * uijk +
                                            um1 * um1) +
                                  xxcon5 * (u[k][j][i + 1][4] * rho_i[k][j][i + 1] -
                                            2.0 * u[k][j][i][4] * rho_i[k][j][i] +
                                            u[k][j][i - 1][4] * rho_i[k][j][i - 1]) -
                                  tx2 * ( (c1 * u[k][j][i + 1][4] -
                                           c2 * square[k][j][i + 1]) * up1 -
                                          (c1 * u[k][j][i - 1][4] -
                                           c2 * square[k][j][i - 1]) * um1 );
            }
        }

        //---------------------------------------------------------------------
        // add fourth order xi-direction dissipation
        //---------------------------------------------------------------------
        for (j = 1; j <= grid_points[1] - 2; j++)
        {
            i = 1;
            for (m = 0; m < 5; m++)
            {
                rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                  ( 5.0 * u[k][j][i][m] - 4.0 * u[k][j][i + 1][m] +
                                    u[k][j][i + 2][m]);
            }

            i = 2;
            for (m = 0; m < 5; m++)
            {
                rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                  (-4.0 * u[k][j][i - 1][m] + 6.0 * u[k][j][i][m] -
                                   4.0 * u[k][j][i + 1][m] + u[k][j][i + 2][m]);
            }
        }

        for (j = 1; j <= grid_points[1] - 2; j++)
        {
            for (i = 3; i <= grid_points[0] - 4; i++)
            {
                for (m = 0; m < 5; m++)
                {
                    rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                      (  u[k][j][i - 2][m] - 4.0 * u[k][j][i - 1][m] +
                                         6.0 * u[k][j][i][m] - 4.0 * u[k][j][i + 1][m] +
                                         u[k][j][i + 2][m] );
                }
            }
        }

        for (j = 1; j <= grid_points[1] - 2; j++)
        {
            i = grid_points[0] - 3;
            for (m = 0; m < 5; m++)
            {
                rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                  ( u[k][j][i - 2][m] - 4.0 * u[k][j][i - 1][m] +
                                    6.0 * u[k][j][i][m] - 4.0 * u[k][j][i + 1][m] );
            }

            i = grid_points[0] - 2;
            for (m = 0; m < 5; m++)
            {
                rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                  ( u[k][j][i - 2][m] - 4.*u[k][j][i - 1][m] +
                                    5.*u[k][j][i][m] );
            }
        }
    }
    if (timeron) timer_stop(t_rhsx);

    if (timeron) timer_start(t_rhsy);
    //---------------------------------------------------------------------
    // compute eta-direction fluxes
    //---------------------------------------------------------------------
    for (k = 1; k <= grid_points[2] - 2; k++)
    {
        for (j = 1; j <= grid_points[1] - 2; j++)
        {
            for (i = 1; i <= grid_points[0] - 2; i++)
            {
                vijk = vs[k][j][i];
                vp1  = vs[k][j + 1][i];
                vm1  = vs[k][j - 1][i];
                rhs[k][j][i][0] = rhs[k][j][i][0] + dy1ty1 *
                                  (u[k][j + 1][i][0] - 2.0 * u[k][j][i][0] +
                                   u[k][j - 1][i][0]) -
                                  ty2 * (u[k][j + 1][i][2] - u[k][j - 1][i][2]);
                rhs[k][j][i][1] = rhs[k][j][i][1] + dy2ty1 *
                                  (u[k][j + 1][i][1] - 2.0 * u[k][j][i][1] +
                                   u[k][j - 1][i][1]) +
                                  yycon2 * (us[k][j + 1][i] - 2.0 * us[k][j][i] +
                                            us[k][j - 1][i]) -
                                  ty2 * (u[k][j + 1][i][1] * vp1 -
                                         u[k][j - 1][i][1] * vm1);
                rhs[k][j][i][2] = rhs[k][j][i][2] + dy3ty1 *
                                  (u[k][j + 1][i][2] - 2.0 * u[k][j][i][2] +
                                   u[k][j - 1][i][2]) +
                                  yycon2 * con43 * (vp1 - 2.0 * vijk + vm1) -
                                  ty2 * (u[k][j + 1][i][2] * vp1 -
                                         u[k][j - 1][i][2] * vm1 +
                                         (u[k][j + 1][i][4] - square[k][j + 1][i] -
                                          u[k][j - 1][i][4] + square[k][j - 1][i])
                                         * c2);
                rhs[k][j][i][3] = rhs[k][j][i][3] + dy4ty1 *
                                  (u[k][j + 1][i][3] - 2.0 * u[k][j][i][3] +
                                   u[k][j - 1][i][3]) +
                                  yycon2 * (ws[k][j + 1][i] - 2.0 * ws[k][j][i] +
                                            ws[k][j - 1][i]) -
                                  ty2 * (u[k][j + 1][i][3] * vp1 -
                                         u[k][j - 1][i][3] * vm1);
                rhs[k][j][i][4] = rhs[k][j][i][4] + dy5ty1 *
                                  (u[k][j + 1][i][4] - 2.0 * u[k][j][i][4] +
                                   u[k][j - 1][i][4]) +
                                  yycon3 * (qs[k][j + 1][i] - 2.0 * qs[k][j][i] +
                                            qs[k][j - 1][i]) +
                                  yycon4 * (vp1 * vp1       - 2.0 * vijk * vijk +
                                            vm1 * vm1) +
                                  yycon5 * (u[k][j + 1][i][4] * rho_i[k][j + 1][i] -
                                            2.0 * u[k][j][i][4] * rho_i[k][j][i] +
                                            u[k][j - 1][i][4] * rho_i[k][j - 1][i]) -
                                  ty2 * ((c1 * u[k][j + 1][i][4] -
                                          c2 * square[k][j + 1][i]) * vp1 -
                                         (c1 * u[k][j - 1][i][4] -
                                          c2 * square[k][j - 1][i]) * vm1);
            }
        }

        //---------------------------------------------------------------------
        // add fourth order eta-direction dissipation
        //---------------------------------------------------------------------
        j = 1;
        for (i = 1; i <= grid_points[0] - 2; i++)
        {
            for (m = 0; m < 5; m++)
            {
                rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                  ( 5.0 * u[k][j][i][m] - 4.0 * u[k][j + 1][i][m] +
                                    u[k][j + 2][i][m]);
            }
        }

        j = 2;
        for (i = 1; i <= grid_points[0] - 2; i++)
        {
            for (m = 0; m < 5; m++)
            {
                rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                  (-4.0 * u[k][j - 1][i][m] + 6.0 * u[k][j][i][m] -
                                   4.0 * u[k][j + 1][i][m] + u[k][j + 2][i][m]);
            }
        }

        for (j = 3; j <= grid_points[1] - 4; j++)
        {
            for (i = 1; i <= grid_points[0] - 2; i++)
            {
                for (m = 0; m < 5; m++)
                {
                    rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                      (  u[k][j - 2][i][m] - 4.0 * u[k][j - 1][i][m] +
                                         6.0 * u[k][j][i][m] - 4.0 * u[k][j + 1][i][m] +
                                         u[k][j + 2][i][m] );
                }
            }
        }

        j = grid_points[1] - 3;
        for (i = 1; i <= grid_points[0] - 2; i++)
        {
            for (m = 0; m < 5; m++)
            {
                rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                  ( u[k][j - 2][i][m] - 4.0 * u[k][j - 1][i][m] +
                                    6.0 * u[k][j][i][m] - 4.0 * u[k][j + 1][i][m] );
            }
        }

        j = grid_points[1] - 2;
        for (i = 1; i <= grid_points[0] - 2; i++)
        {
            for (m = 0; m < 5; m++)
            {
                rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                  ( u[k][j - 2][i][m] - 4.*u[k][j - 1][i][m] +
                                    5.*u[k][j][i][m] );
            }
        }
    }
    if (timeron) timer_stop(t_rhsy);

    if (timeron) timer_start(t_rhsz);
    //---------------------------------------------------------------------
    // compute zeta-direction fluxes
    //---------------------------------------------------------------------
    for (k = 1; k <= grid_points[2] - 2; k++)
    {
        for (j = 1; j <= grid_points[1] - 2; j++)
        {
            for (i = 1; i <= grid_points[0] - 2; i++)
            {
                wijk = ws[k][j][i];
                wp1  = ws[k + 1][j][i];
                wm1  = ws[k - 1][j][i];

                rhs[k][j][i][0] = rhs[k][j][i][0] + dz1tz1 *
                                  (u[k + 1][j][i][0] - 2.0 * u[k][j][i][0] +
                                   u[k - 1][j][i][0]) -
                                  tz2 * (u[k + 1][j][i][3] - u[k - 1][j][i][3]);
                rhs[k][j][i][1] = rhs[k][j][i][1] + dz2tz1 *
                                  (u[k + 1][j][i][1] - 2.0 * u[k][j][i][1] +
                                   u[k - 1][j][i][1]) +
                                  zzcon2 * (us[k + 1][j][i] - 2.0 * us[k][j][i] +
                                            us[k - 1][j][i]) -
                                  tz2 * (u[k + 1][j][i][1] * wp1 -
                                         u[k - 1][j][i][1] * wm1);
                rhs[k][j][i][2] = rhs[k][j][i][2] + dz3tz1 *
                                  (u[k + 1][j][i][2] - 2.0 * u[k][j][i][2] +
                                   u[k - 1][j][i][2]) +
                                  zzcon2 * (vs[k + 1][j][i] - 2.0 * vs[k][j][i] +
                                            vs[k - 1][j][i]) -
                                  tz2 * (u[k + 1][j][i][2] * wp1 -
                                         u[k - 1][j][i][2] * wm1);
                rhs[k][j][i][3] = rhs[k][j][i][3] + dz4tz1 *
                                  (u[k + 1][j][i][3] - 2.0 * u[k][j][i][3] +
                                   u[k - 1][j][i][3]) +
                                  zzcon2 * con43 * (wp1 - 2.0 * wijk + wm1) -
                                  tz2 * (u[k + 1][j][i][3] * wp1 -
                                         u[k - 1][j][i][3] * wm1 +
                                         (u[k + 1][j][i][4] - square[k + 1][j][i] -
                                          u[k - 1][j][i][4] + square[k - 1][j][i])
                                         * c2);
                rhs[k][j][i][4] = rhs[k][j][i][4] + dz5tz1 *
                                  (u[k + 1][j][i][4] - 2.0 * u[k][j][i][4] +
                                   u[k - 1][j][i][4]) +
                                  zzcon3 * (qs[k + 1][j][i] - 2.0 * qs[k][j][i] +
                                            qs[k - 1][j][i]) +
                                  zzcon4 * (wp1 * wp1 - 2.0 * wijk * wijk +
                                            wm1 * wm1) +
                                  zzcon5 * (u[k + 1][j][i][4] * rho_i[k + 1][j][i] -
                                            2.0 * u[k][j][i][4] * rho_i[k][j][i] +
                                            u[k - 1][j][i][4] * rho_i[k - 1][j][i]) -
                                  tz2 * ( (c1 * u[k + 1][j][i][4] -
                                           c2 * square[k + 1][j][i]) * wp1 -
                                          (c1 * u[k - 1][j][i][4] -
                                           c2 * square[k - 1][j][i]) * wm1);
            }
        }
    }

    //---------------------------------------------------------------------
    // add fourth order zeta-direction dissipation
    //---------------------------------------------------------------------
    k = 1;
    for (j = 1; j <= grid_points[1] - 2; j++)
    {
        for (i = 1; i <= grid_points[0] - 2; i++)
        {
            for (m = 0; m < 5; m++)
            {
                rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                  ( 5.0 * u[k][j][i][m] - 4.0 * u[k + 1][j][i][m] +
                                    u[k + 2][j][i][m]);
            }
        }
    }

    k = 2;
    for (j = 1; j <= grid_points[1] - 2; j++)
    {
        for (i = 1; i <= grid_points[0] - 2; i++)
        {
            for (m = 0; m < 5; m++)
            {
                rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                  (-4.0 * u[k - 1][j][i][m] + 6.0 * u[k][j][i][m] -
                                   4.0 * u[k + 1][j][i][m] + u[k + 2][j][i][m]);
            }
        }
    }

    for (k = 3; k <= grid_points[2] - 4; k++)
    {
        for (j = 1; j <= grid_points[1] - 2; j++)
        {
            for (i = 1; i <= grid_points[0] - 2; i++)
            {
                for (m = 0; m < 5; m++)
                {
                    rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                      (  u[k - 2][j][i][m] - 4.0 * u[k - 1][j][i][m] +
                                         6.0 * u[k][j][i][m] - 4.0 * u[k + 1][j][i][m] +
                                         u[k + 2][j][i][m] );
                }
            }
        }
    }

    k = grid_points[2] - 3;
    for (j = 1; j <= grid_points[1] - 2; j++)
    {
        for (i = 1; i <= grid_points[0] - 2; i++)
        {
            for (m = 0; m < 5; m++)
            {
                rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                  ( u[k - 2][j][i][m] - 4.0 * u[k - 1][j][i][m] +
                                    6.0 * u[k][j][i][m] - 4.0 * u[k + 1][j][i][m] );
            }
        }
    }

    k = grid_points[2] - 2;
    for (j = 1; j <= grid_points[1] - 2; j++)
    {
        for (i = 1; i <= grid_points[0] - 2; i++)
        {
            for (m = 0; m < 5; m++)
            {
                rhs[k][j][i][m] = rhs[k][j][i][m] - dssp *
                                  ( u[k - 2][j][i][m] - 4.*u[k - 1][j][i][m] +
                                    5.*u[k][j][i][m] );
            }
        }
    }
    if (timeron) timer_stop(t_rhsz);

    for (k = 1; k <= grid_points[2] - 2; k++)
    {
        for (j = 1; j <= grid_points[1] - 2; j++)
        {
            for (i = 1; i <= grid_points[0] - 2; i++)
            {
                for (m = 0; m < 5; m++)
                {
                    rhs[k][j][i][m] = rhs[k][j][i][m] * dt;
                }
            }
        }
    }
    if (timeron) timer_stop(t_rhs);
}

void set_constants()
{
    ce[0][0]  = 2.0;
    ce[0][1]  = 0.0;
    ce[0][2]  = 0.0;
    ce[0][3]  = 4.0;
    ce[0][4]  = 5.0;
    ce[0][5]  = 3.0;
    ce[0][6]  = 0.5;
    ce[0][7]  = 0.02;
    ce[0][8]  = 0.01;
    ce[0][9]  = 0.03;
    ce[0][10] = 0.5;
    ce[0][11] = 0.4;
    ce[0][12] = 0.3;

    ce[1][0]  = 1.0;
    ce[1][1]  = 0.0;
    ce[1][2]  = 0.0;
    ce[1][3]  = 0.0;
    ce[1][4]  = 1.0;
    ce[1][5]  = 2.0;
    ce[1][6]  = 3.0;
    ce[1][7]  = 0.01;
    ce[1][8]  = 0.03;
    ce[1][9]  = 0.02;
    ce[1][10] = 0.4;
    ce[1][11] = 0.3;
    ce[1][12] = 0.5;

    ce[2][0]  = 2.0;
    ce[2][1]  = 2.0;
    ce[2][2]  = 0.0;
    ce[2][3]  = 0.0;
    ce[2][4]  = 0.0;
    ce[2][5]  = 2.0;
    ce[2][6]  = 3.0;
    ce[2][7]  = 0.04;
    ce[2][8]  = 0.03;
    ce[2][9]  = 0.05;
    ce[2][10] = 0.3;
    ce[2][11] = 0.5;
    ce[2][12] = 0.4;

    ce[3][0]  = 2.0;
    ce[3][1]  = 2.0;
    ce[3][2]  = 0.0;
    ce[3][3]  = 0.0;
    ce[3][4]  = 0.0;
    ce[3][5]  = 2.0;
    ce[3][6]  = 3.0;
    ce[3][7]  = 0.03;
    ce[3][8]  = 0.05;
    ce[3][9] = 0.04;
    ce[3][10] = 0.2;
    ce[3][11] = 0.1;
    ce[3][12] = 0.3;

    ce[4][0]  = 5.0;
    ce[4][1]  = 4.0;
    ce[4][2]  = 3.0;
    ce[4][3]  = 2.0;
    ce[4][4]  = 0.1;
    ce[4][5]  = 0.4;
    ce[4][6]  = 0.3;
    ce[4][7]  = 0.05;
    ce[4][8]  = 0.04;
    ce[4][9] = 0.03;
    ce[4][10] = 0.1;
    ce[4][11] = 0.3;
    ce[4][12] = 0.2;

    c1 = 1.4;
    c2 = 0.4;
    c3 = 0.1;
    c4 = 1.0;
    c5 = 1.4;

    dnxm1 = 1.0 / (double)(grid_points[0] - 1);
    dnym1 = 1.0 / (double)(grid_points[1] - 1);
    dnzm1 = 1.0 / (double)(grid_points[2] - 1);

    c1c2 = c1 * c2;
    c1c5 = c1 * c5;
    c3c4 = c3 * c4;
    c1345 = c1c5 * c3c4;

    conz1 = (1.0 - c1c5);

    tx1 = 1.0 / (dnxm1 * dnxm1);
    tx2 = 1.0 / (2.0 * dnxm1);
    tx3 = 1.0 / dnxm1;

    ty1 = 1.0 / (dnym1 * dnym1);
    ty2 = 1.0 / (2.0 * dnym1);
    ty3 = 1.0 / dnym1;

    tz1 = 1.0 / (dnzm1 * dnzm1);
    tz2 = 1.0 / (2.0 * dnzm1);
    tz3 = 1.0 / dnzm1;

    dx1 = 0.75;
    dx2 = 0.75;
    dx3 = 0.75;
    dx4 = 0.75;
    dx5 = 0.75;

    dy1 = 0.75;
    dy2 = 0.75;
    dy3 = 0.75;
    dy4 = 0.75;
    dy5 = 0.75;

    dz1 = 1.0;
    dz2 = 1.0;
    dz3 = 1.0;
    dz4 = 1.0;
    dz5 = 1.0;

    dxmax = max(dx3, dx4);
    dymax = max(dy2, dy4);
    dzmax = max(dz2, dz3);

    dssp = 0.25 * max(dx1, max(dy1, dz1) );

    c4dssp = 4.0 * dssp;
    c5dssp = 5.0 * dssp;

    dttx1 = dt * tx1;
    dttx2 = dt * tx2;
    dtty1 = dt * ty1;
    dtty2 = dt * ty2;
    dttz1 = dt * tz1;
    dttz2 = dt * tz2;

    c2dttx1 = 2.0 * dttx1;
    c2dtty1 = 2.0 * dtty1;
    c2dttz1 = 2.0 * dttz1;

    dtdssp = dt * dssp;

    comz1  = dtdssp;
    comz4  = 4.0 * dtdssp;
    comz5  = 5.0 * dtdssp;
    comz6  = 6.0 * dtdssp;

    c3c4tx3 = c3c4 * tx3;
    c3c4ty3 = c3c4 * ty3;
    c3c4tz3 = c3c4 * tz3;

    dx1tx1 = dx1 * tx1;
    dx2tx1 = dx2 * tx1;
    dx3tx1 = dx3 * tx1;
    dx4tx1 = dx4 * tx1;
    dx5tx1 = dx5 * tx1;

    dy1ty1 = dy1 * ty1;
    dy2ty1 = dy2 * ty1;
    dy3ty1 = dy3 * ty1;
    dy4ty1 = dy4 * ty1;
    dy5ty1 = dy5 * ty1;

    dz1tz1 = dz1 * tz1;
    dz2tz1 = dz2 * tz1;
    dz3tz1 = dz3 * tz1;
    dz4tz1 = dz4 * tz1;
    dz5tz1 = dz5 * tz1;

    c2iv  = 2.5;
    con43 = 4.0 / 3.0;
    con16 = 1.0 / 6.0;

    xxcon1 = c3c4tx3 * con43 * tx3;
    xxcon2 = c3c4tx3 * tx3;
    xxcon3 = c3c4tx3 * conz1 * tx3;
    xxcon4 = c3c4tx3 * con16 * tx3;
    xxcon5 = c3c4tx3 * c1c5 * tx3;

    yycon1 = c3c4ty3 * con43 * ty3;
    yycon2 = c3c4ty3 * ty3;
    yycon3 = c3c4ty3 * conz1 * ty3;
    yycon4 = c3c4ty3 * con16 * ty3;
    yycon5 = c3c4ty3 * c1c5 * ty3;

    zzcon1 = c3c4tz3 * con43 * tz3;
    zzcon2 = c3c4tz3 * tz3;
    zzcon3 = c3c4tz3 * conz1 * tz3;
    zzcon4 = c3c4tz3 * con16 * tz3;
    zzcon5 = c3c4tz3 * c1c5 * tz3;
}

//---------------------------------------------------------------------
// subtracts bvec=bvec - ablock*avec
//---------------------------------------------------------------------
void matvec_sub(double ablock[5][5], double avec[5], double bvec[5])
{
    //---------------------------------------------------------------------
    // rhs[kc][jc][ic][i] = rhs[kc][jc][ic][i]
    // $                  - lhs[ia][ablock][0][i]*
    //---------------------------------------------------------------------
    bvec[0] = bvec[0] - ablock[0][0] * avec[0]
              - ablock[1][0] * avec[1]
              - ablock[2][0] * avec[2]
              - ablock[3][0] * avec[3]
              - ablock[4][0] * avec[4];
    bvec[1] = bvec[1] - ablock[0][1] * avec[0]
              - ablock[1][1] * avec[1]
              - ablock[2][1] * avec[2]
              - ablock[3][1] * avec[3]
              - ablock[4][1] * avec[4];
    bvec[2] = bvec[2] - ablock[0][2] * avec[0]
              - ablock[1][2] * avec[1]
              - ablock[2][2] * avec[2]
              - ablock[3][2] * avec[3]
              - ablock[4][2] * avec[4];
    bvec[3] = bvec[3] - ablock[0][3] * avec[0]
              - ablock[1][3] * avec[1]
              - ablock[2][3] * avec[2]
              - ablock[3][3] * avec[3]
              - ablock[4][3] * avec[4];
    bvec[4] = bvec[4] - ablock[0][4] * avec[0]
              - ablock[1][4] * avec[1]
              - ablock[2][4] * avec[2]
              - ablock[3][4] * avec[3]
              - ablock[4][4] * avec[4];
}


//---------------------------------------------------------------------
// subtracts a(i,j,k) X b(i,j,k) from c(i,j,k)
//---------------------------------------------------------------------
void matmul_sub(double ablock[5][5], double bblock[5][5], double cblock[5][5])
{
    cblock[0][0] = cblock[0][0] - ablock[0][0] * bblock[0][0]
                   - ablock[1][0] * bblock[0][1]
                   - ablock[2][0] * bblock[0][2]
                   - ablock[3][0] * bblock[0][3]
                   - ablock[4][0] * bblock[0][4];
    cblock[0][1] = cblock[0][1] - ablock[0][1] * bblock[0][0]
                   - ablock[1][1] * bblock[0][1]
                   - ablock[2][1] * bblock[0][2]
                   - ablock[3][1] * bblock[0][3]
                   - ablock[4][1] * bblock[0][4];
    cblock[0][2] = cblock[0][2] - ablock[0][2] * bblock[0][0]
                   - ablock[1][2] * bblock[0][1]
                   - ablock[2][2] * bblock[0][2]
                   - ablock[3][2] * bblock[0][3]
                   - ablock[4][2] * bblock[0][4];
    cblock[0][3] = cblock[0][3] - ablock[0][3] * bblock[0][0]
                   - ablock[1][3] * bblock[0][1]
                   - ablock[2][3] * bblock[0][2]
                   - ablock[3][3] * bblock[0][3]
                   - ablock[4][3] * bblock[0][4];
    cblock[0][4] = cblock[0][4] - ablock[0][4] * bblock[0][0]
                   - ablock[1][4] * bblock[0][1]
                   - ablock[2][4] * bblock[0][2]
                   - ablock[3][4] * bblock[0][3]
                   - ablock[4][4] * bblock[0][4];
    cblock[1][0] = cblock[1][0] - ablock[0][0] * bblock[1][0]
                   - ablock[1][0] * bblock[1][1]
                   - ablock[2][0] * bblock[1][2]
                   - ablock[3][0] * bblock[1][3]
                   - ablock[4][0] * bblock[1][4];
    cblock[1][1] = cblock[1][1] - ablock[0][1] * bblock[1][0]
                   - ablock[1][1] * bblock[1][1]
                   - ablock[2][1] * bblock[1][2]
                   - ablock[3][1] * bblock[1][3]
                   - ablock[4][1] * bblock[1][4];
    cblock[1][2] = cblock[1][2] - ablock[0][2] * bblock[1][0]
                   - ablock[1][2] * bblock[1][1]
                   - ablock[2][2] * bblock[1][2]
                   - ablock[3][2] * bblock[1][3]
                   - ablock[4][2] * bblock[1][4];
    cblock[1][3] = cblock[1][3] - ablock[0][3] * bblock[1][0]
                   - ablock[1][3] * bblock[1][1]
                   - ablock[2][3] * bblock[1][2]
                   - ablock[3][3] * bblock[1][3]
                   - ablock[4][3] * bblock[1][4];
    cblock[1][4] = cblock[1][4] - ablock[0][4] * bblock[1][0]
                   - ablock[1][4] * bblock[1][1]
                   - ablock[2][4] * bblock[1][2]
                   - ablock[3][4] * bblock[1][3]
                   - ablock[4][4] * bblock[1][4];
    cblock[2][0] = cblock[2][0] - ablock[0][0] * bblock[2][0]
                   - ablock[1][0] * bblock[2][1]
                   - ablock[2][0] * bblock[2][2]
                   - ablock[3][0] * bblock[2][3]
                   - ablock[4][0] * bblock[2][4];
    cblock[2][1] = cblock[2][1] - ablock[0][1] * bblock[2][0]
                   - ablock[1][1] * bblock[2][1]
                   - ablock[2][1] * bblock[2][2]
                   - ablock[3][1] * bblock[2][3]
                   - ablock[4][1] * bblock[2][4];
    cblock[2][2] = cblock[2][2] - ablock[0][2] * bblock[2][0]
                   - ablock[1][2] * bblock[2][1]
                   - ablock[2][2] * bblock[2][2]
                   - ablock[3][2] * bblock[2][3]
                   - ablock[4][2] * bblock[2][4];
    cblock[2][3] = cblock[2][3] - ablock[0][3] * bblock[2][0]
                   - ablock[1][3] * bblock[2][1]
                   - ablock[2][3] * bblock[2][2]
                   - ablock[3][3] * bblock[2][3]
                   - ablock[4][3] * bblock[2][4];
    cblock[2][4] = cblock[2][4] - ablock[0][4] * bblock[2][0]
                   - ablock[1][4] * bblock[2][1]
                   - ablock[2][4] * bblock[2][2]
                   - ablock[3][4] * bblock[2][3]
                   - ablock[4][4] * bblock[2][4];
    cblock[3][0] = cblock[3][0] - ablock[0][0] * bblock[3][0]
                   - ablock[1][0] * bblock[3][1]
                   - ablock[2][0] * bblock[3][2]
                   - ablock[3][0] * bblock[3][3]
                   - ablock[4][0] * bblock[3][4];
    cblock[3][1] = cblock[3][1] - ablock[0][1] * bblock[3][0]
                   - ablock[1][1] * bblock[3][1]
                   - ablock[2][1] * bblock[3][2]
                   - ablock[3][1] * bblock[3][3]
                   - ablock[4][1] * bblock[3][4];
    cblock[3][2] = cblock[3][2] - ablock[0][2] * bblock[3][0]
                   - ablock[1][2] * bblock[3][1]
                   - ablock[2][2] * bblock[3][2]
                   - ablock[3][2] * bblock[3][3]
                   - ablock[4][2] * bblock[3][4];
    cblock[3][3] = cblock[3][3] - ablock[0][3] * bblock[3][0]
                   - ablock[1][3] * bblock[3][1]
                   - ablock[2][3] * bblock[3][2]
                   - ablock[3][3] * bblock[3][3]
                   - ablock[4][3] * bblock[3][4];
    cblock[3][4] = cblock[3][4] - ablock[0][4] * bblock[3][0]
                   - ablock[1][4] * bblock[3][1]
                   - ablock[2][4] * bblock[3][2]
                   - ablock[3][4] * bblock[3][3]
                   - ablock[4][4] * bblock[3][4];
    cblock[4][0] = cblock[4][0] - ablock[0][0] * bblock[4][0]
                   - ablock[1][0] * bblock[4][1]
                   - ablock[2][0] * bblock[4][2]
                   - ablock[3][0] * bblock[4][3]
                   - ablock[4][0] * bblock[4][4];
    cblock[4][1] = cblock[4][1] - ablock[0][1] * bblock[4][0]
                   - ablock[1][1] * bblock[4][1]
                   - ablock[2][1] * bblock[4][2]
                   - ablock[3][1] * bblock[4][3]
                   - ablock[4][1] * bblock[4][4];
    cblock[4][2] = cblock[4][2] - ablock[0][2] * bblock[4][0]
                   - ablock[1][2] * bblock[4][1]
                   - ablock[2][2] * bblock[4][2]
                   - ablock[3][2] * bblock[4][3]
                   - ablock[4][2] * bblock[4][4];
    cblock[4][3] = cblock[4][3] - ablock[0][3] * bblock[4][0]
                   - ablock[1][3] * bblock[4][1]
                   - ablock[2][3] * bblock[4][2]
                   - ablock[3][3] * bblock[4][3]
                   - ablock[4][3] * bblock[4][4];
    cblock[4][4] = cblock[4][4] - ablock[0][4] * bblock[4][0]
                   - ablock[1][4] * bblock[4][1]
                   - ablock[2][4] * bblock[4][2]
                   - ablock[3][4] * bblock[4][3]
                   - ablock[4][4] * bblock[4][4];
}


void binvcrhs(double lhs[5][5], double c[5][5], double r[5])
{
    double pivot, coeff;

    pivot = 1.00 / lhs[0][0];
    lhs[1][0] = lhs[1][0] * pivot;
    lhs[2][0] = lhs[2][0] * pivot;
    lhs[3][0] = lhs[3][0] * pivot;
    lhs[4][0] = lhs[4][0] * pivot;
    c[0][0] = c[0][0] * pivot;
    c[1][0] = c[1][0] * pivot;
    c[2][0] = c[2][0] * pivot;
    c[3][0] = c[3][0] * pivot;
    c[4][0] = c[4][0] * pivot;
    r[0]   = r[0]  * pivot;

    coeff = lhs[0][1];
    lhs[1][1] = lhs[1][1] - coeff * lhs[1][0];
    lhs[2][1] = lhs[2][1] - coeff * lhs[2][0];
    lhs[3][1] = lhs[3][1] - coeff * lhs[3][0];
    lhs[4][1] = lhs[4][1] - coeff * lhs[4][0];
    c[0][1] = c[0][1] - coeff * c[0][0];
    c[1][1] = c[1][1] - coeff * c[1][0];
    c[2][1] = c[2][1] - coeff * c[2][0];
    c[3][1] = c[3][1] - coeff * c[3][0];
    c[4][1] = c[4][1] - coeff * c[4][0];
    r[1]   = r[1]   - coeff * r[0];

    coeff = lhs[0][2];
    lhs[1][2] = lhs[1][2] - coeff * lhs[1][0];
    lhs[2][2] = lhs[2][2] - coeff * lhs[2][0];
    lhs[3][2] = lhs[3][2] - coeff * lhs[3][0];
    lhs[4][2] = lhs[4][2] - coeff * lhs[4][0];
    c[0][2] = c[0][2] - coeff * c[0][0];
    c[1][2] = c[1][2] - coeff * c[1][0];
    c[2][2] = c[2][2] - coeff * c[2][0];
    c[3][2] = c[3][2] - coeff * c[3][0];
    c[4][2] = c[4][2] - coeff * c[4][0];
    r[2]   = r[2]   - coeff * r[0];

    coeff = lhs[0][3];
    lhs[1][3] = lhs[1][3] - coeff * lhs[1][0];
    lhs[2][3] = lhs[2][3] - coeff * lhs[2][0];
    lhs[3][3] = lhs[3][3] - coeff * lhs[3][0];
    lhs[4][3] = lhs[4][3] - coeff * lhs[4][0];
    c[0][3] = c[0][3] - coeff * c[0][0];
    c[1][3] = c[1][3] - coeff * c[1][0];
    c[2][3] = c[2][3] - coeff * c[2][0];
    c[3][3] = c[3][3] - coeff * c[3][0];
    c[4][3] = c[4][3] - coeff * c[4][0];
    r[3]   = r[3]   - coeff * r[0];

    coeff = lhs[0][4];
    lhs[1][4] = lhs[1][4] - coeff * lhs[1][0];
    lhs[2][4] = lhs[2][4] - coeff * lhs[2][0];
    lhs[3][4] = lhs[3][4] - coeff * lhs[3][0];
    lhs[4][4] = lhs[4][4] - coeff * lhs[4][0];
    c[0][4] = c[0][4] - coeff * c[0][0];
    c[1][4] = c[1][4] - coeff * c[1][0];
    c[2][4] = c[2][4] - coeff * c[2][0];
    c[3][4] = c[3][4] - coeff * c[3][0];
    c[4][4] = c[4][4] - coeff * c[4][0];
    r[4]   = r[4]   - coeff * r[0];


    pivot = 1.00 / lhs[1][1];
    lhs[2][1] = lhs[2][1] * pivot;
    lhs[3][1] = lhs[3][1] * pivot;
    lhs[4][1] = lhs[4][1] * pivot;
    c[0][1] = c[0][1] * pivot;
    c[1][1] = c[1][1] * pivot;
    c[2][1] = c[2][1] * pivot;
    c[3][1] = c[3][1] * pivot;
    c[4][1] = c[4][1] * pivot;
    r[1]   = r[1]  * pivot;

    coeff = lhs[1][0];
    lhs[2][0] = lhs[2][0] - coeff * lhs[2][1];
    lhs[3][0] = lhs[3][0] - coeff * lhs[3][1];
    lhs[4][0] = lhs[4][0] - coeff * lhs[4][1];
    c[0][0] = c[0][0] - coeff * c[0][1];
    c[1][0] = c[1][0] - coeff * c[1][1];
    c[2][0] = c[2][0] - coeff * c[2][1];
    c[3][0] = c[3][0] - coeff * c[3][1];
    c[4][0] = c[4][0] - coeff * c[4][1];
    r[0]   = r[0]   - coeff * r[1];

    coeff = lhs[1][2];
    lhs[2][2] = lhs[2][2] - coeff * lhs[2][1];
    lhs[3][2] = lhs[3][2] - coeff * lhs[3][1];
    lhs[4][2] = lhs[4][2] - coeff * lhs[4][1];
    c[0][2] = c[0][2] - coeff * c[0][1];
    c[1][2] = c[1][2] - coeff * c[1][1];
    c[2][2] = c[2][2] - coeff * c[2][1];
    c[3][2] = c[3][2] - coeff * c[3][1];
    c[4][2] = c[4][2] - coeff * c[4][1];
    r[2]   = r[2]   - coeff * r[1];

    coeff = lhs[1][3];
    lhs[2][3] = lhs[2][3] - coeff * lhs[2][1];
    lhs[3][3] = lhs[3][3] - coeff * lhs[3][1];
    lhs[4][3] = lhs[4][3] - coeff * lhs[4][1];
    c[0][3] = c[0][3] - coeff * c[0][1];
    c[1][3] = c[1][3] - coeff * c[1][1];
    c[2][3] = c[2][3] - coeff * c[2][1];
    c[3][3] = c[3][3] - coeff * c[3][1];
    c[4][3] = c[4][3] - coeff * c[4][1];
    r[3]   = r[3]   - coeff * r[1];

    coeff = lhs[1][4];
    lhs[2][4] = lhs[2][4] - coeff * lhs[2][1];
    lhs[3][4] = lhs[3][4] - coeff * lhs[3][1];
    lhs[4][4] = lhs[4][4] - coeff * lhs[4][1];
    c[0][4] = c[0][4] - coeff * c[0][1];
    c[1][4] = c[1][4] - coeff * c[1][1];
    c[2][4] = c[2][4] - coeff * c[2][1];
    c[3][4] = c[3][4] - coeff * c[3][1];
    c[4][4] = c[4][4] - coeff * c[4][1];
    r[4]   = r[4]   - coeff * r[1];


    pivot = 1.00 / lhs[2][2];
    lhs[3][2] = lhs[3][2] * pivot;
    lhs[4][2] = lhs[4][2] * pivot;
    c[0][2] = c[0][2] * pivot;
    c[1][2] = c[1][2] * pivot;
    c[2][2] = c[2][2] * pivot;
    c[3][2] = c[3][2] * pivot;
    c[4][2] = c[4][2] * pivot;
    r[2]   = r[2]  * pivot;

    coeff = lhs[2][0];
    lhs[3][0] = lhs[3][0] - coeff * lhs[3][2];
    lhs[4][0] = lhs[4][0] - coeff * lhs[4][2];
    c[0][0] = c[0][0] - coeff * c[0][2];
    c[1][0] = c[1][0] - coeff * c[1][2];
    c[2][0] = c[2][0] - coeff * c[2][2];
    c[3][0] = c[3][0] - coeff * c[3][2];
    c[4][0] = c[4][0] - coeff * c[4][2];
    r[0]   = r[0]   - coeff * r[2];

    coeff = lhs[2][1];
    lhs[3][1] = lhs[3][1] - coeff * lhs[3][2];
    lhs[4][1] = lhs[4][1] - coeff * lhs[4][2];
    c[0][1] = c[0][1] - coeff * c[0][2];
    c[1][1] = c[1][1] - coeff * c[1][2];
    c[2][1] = c[2][1] - coeff * c[2][2];
    c[3][1] = c[3][1] - coeff * c[3][2];
    c[4][1] = c[4][1] - coeff * c[4][2];
    r[1]   = r[1]   - coeff * r[2];

    coeff = lhs[2][3];
    lhs[3][3] = lhs[3][3] - coeff * lhs[3][2];
    lhs[4][3] = lhs[4][3] - coeff * lhs[4][2];
    c[0][3] = c[0][3] - coeff * c[0][2];
    c[1][3] = c[1][3] - coeff * c[1][2];
    c[2][3] = c[2][3] - coeff * c[2][2];
    c[3][3] = c[3][3] - coeff * c[3][2];
    c[4][3] = c[4][3] - coeff * c[4][2];
    r[3]   = r[3]   - coeff * r[2];

    coeff = lhs[2][4];
    lhs[3][4] = lhs[3][4] - coeff * lhs[3][2];
    lhs[4][4] = lhs[4][4] - coeff * lhs[4][2];
    c[0][4] = c[0][4] - coeff * c[0][2];
    c[1][4] = c[1][4] - coeff * c[1][2];
    c[2][4] = c[2][4] - coeff * c[2][2];
    c[3][4] = c[3][4] - coeff * c[3][2];
    c[4][4] = c[4][4] - coeff * c[4][2];
    r[4]   = r[4]   - coeff * r[2];


    pivot = 1.00 / lhs[3][3];
    lhs[4][3] = lhs[4][3] * pivot;
    c[0][3] = c[0][3] * pivot;
    c[1][3] = c[1][3] * pivot;
    c[2][3] = c[2][3] * pivot;
    c[3][3] = c[3][3] * pivot;
    c[4][3] = c[4][3] * pivot;
    r[3]   = r[3]  * pivot;

    coeff = lhs[3][0];
    lhs[4][0] = lhs[4][0] - coeff * lhs[4][3];
    c[0][0] = c[0][0] - coeff * c[0][3];
    c[1][0] = c[1][0] - coeff * c[1][3];
    c[2][0] = c[2][0] - coeff * c[2][3];
    c[3][0] = c[3][0] - coeff * c[3][3];
    c[4][0] = c[4][0] - coeff * c[4][3];
    r[0]   = r[0]   - coeff * r[3];

    coeff = lhs[3][1];
    lhs[4][1] = lhs[4][1] - coeff * lhs[4][3];
    c[0][1] = c[0][1] - coeff * c[0][3];
    c[1][1] = c[1][1] - coeff * c[1][3];
    c[2][1] = c[2][1] - coeff * c[2][3];
    c[3][1] = c[3][1] - coeff * c[3][3];
    c[4][1] = c[4][1] - coeff * c[4][3];
    r[1]   = r[1]   - coeff * r[3];

    coeff = lhs[3][2];
    lhs[4][2] = lhs[4][2] - coeff * lhs[4][3];
    c[0][2] = c[0][2] - coeff * c[0][3];
    c[1][2] = c[1][2] - coeff * c[1][3];
    c[2][2] = c[2][2] - coeff * c[2][3];
    c[3][2] = c[3][2] - coeff * c[3][3];
    c[4][2] = c[4][2] - coeff * c[4][3];
    r[2]   = r[2]   - coeff * r[3];

    coeff = lhs[3][4];
    lhs[4][4] = lhs[4][4] - coeff * lhs[4][3];
    c[0][4] = c[0][4] - coeff * c[0][3];
    c[1][4] = c[1][4] - coeff * c[1][3];
    c[2][4] = c[2][4] - coeff * c[2][3];
    c[3][4] = c[3][4] - coeff * c[3][3];
    c[4][4] = c[4][4] - coeff * c[4][3];
    r[4]   = r[4]   - coeff * r[3];


    pivot = 1.00 / lhs[4][4];
    c[0][4] = c[0][4] * pivot;
    c[1][4] = c[1][4] * pivot;
    c[2][4] = c[2][4] * pivot;
    c[3][4] = c[3][4] * pivot;
    c[4][4] = c[4][4] * pivot;
    r[4]   = r[4]  * pivot;

    coeff = lhs[4][0];
    c[0][0] = c[0][0] - coeff * c[0][4];
    c[1][0] = c[1][0] - coeff * c[1][4];
    c[2][0] = c[2][0] - coeff * c[2][4];
    c[3][0] = c[3][0] - coeff * c[3][4];
    c[4][0] = c[4][0] - coeff * c[4][4];
    r[0]   = r[0]   - coeff * r[4];

    coeff = lhs[4][1];
    c[0][1] = c[0][1] - coeff * c[0][4];
    c[1][1] = c[1][1] - coeff * c[1][4];
    c[2][1] = c[2][1] - coeff * c[2][4];
    c[3][1] = c[3][1] - coeff * c[3][4];
    c[4][1] = c[4][1] - coeff * c[4][4];
    r[1]   = r[1]   - coeff * r[4];

    coeff = lhs[4][2];
    c[0][2] = c[0][2] - coeff * c[0][4];
    c[1][2] = c[1][2] - coeff * c[1][4];
    c[2][2] = c[2][2] - coeff * c[2][4];
    c[3][2] = c[3][2] - coeff * c[3][4];
    c[4][2] = c[4][2] - coeff * c[4][4];
    r[2]   = r[2]   - coeff * r[4];

    coeff = lhs[4][3];
    c[0][3] = c[0][3] - coeff * c[0][4];
    c[1][3] = c[1][3] - coeff * c[1][4];
    c[2][3] = c[2][3] - coeff * c[2][4];
    c[3][3] = c[3][3] - coeff * c[3][4];
    c[4][3] = c[4][3] - coeff * c[4][4];
    r[3]   = r[3]   - coeff * r[4];
}


void binvrhs(double lhs[5][5], double r[5])
{
    double pivot, coeff;

    pivot = 1.00 / lhs[0][0];
    lhs[1][0] = lhs[1][0] * pivot;
    lhs[2][0] = lhs[2][0] * pivot;
    lhs[3][0] = lhs[3][0] * pivot;
    lhs[4][0] = lhs[4][0] * pivot;
    r[0]   = r[0]  * pivot;

    coeff = lhs[0][1];
    lhs[1][1] = lhs[1][1] - coeff * lhs[1][0];
    lhs[2][1] = lhs[2][1] - coeff * lhs[2][0];
    lhs[3][1] = lhs[3][1] - coeff * lhs[3][0];
    lhs[4][1] = lhs[4][1] - coeff * lhs[4][0];
    r[1]   = r[1]   - coeff * r[0];

    coeff = lhs[0][2];
    lhs[1][2] = lhs[1][2] - coeff * lhs[1][0];
    lhs[2][2] = lhs[2][2] - coeff * lhs[2][0];
    lhs[3][2] = lhs[3][2] - coeff * lhs[3][0];
    lhs[4][2] = lhs[4][2] - coeff * lhs[4][0];
    r[2]   = r[2]   - coeff * r[0];

    coeff = lhs[0][3];
    lhs[1][3] = lhs[1][3] - coeff * lhs[1][0];
    lhs[2][3] = lhs[2][3] - coeff * lhs[2][0];
    lhs[3][3] = lhs[3][3] - coeff * lhs[3][0];
    lhs[4][3] = lhs[4][3] - coeff * lhs[4][0];
    r[3]   = r[3]   - coeff * r[0];

    coeff = lhs[0][4];
    lhs[1][4] = lhs[1][4] - coeff * lhs[1][0];
    lhs[2][4] = lhs[2][4] - coeff * lhs[2][0];
    lhs[3][4] = lhs[3][4] - coeff * lhs[3][0];
    lhs[4][4] = lhs[4][4] - coeff * lhs[4][0];
    r[4]   = r[4]   - coeff * r[0];


    pivot = 1.00 / lhs[1][1];
    lhs[2][1] = lhs[2][1] * pivot;
    lhs[3][1] = lhs[3][1] * pivot;
    lhs[4][1] = lhs[4][1] * pivot;
    r[1]   = r[1]  * pivot;

    coeff = lhs[1][0];
    lhs[2][0] = lhs[2][0] - coeff * lhs[2][1];
    lhs[3][0] = lhs[3][0] - coeff * lhs[3][1];
    lhs[4][0] = lhs[4][0] - coeff * lhs[4][1];
    r[0]   = r[0]   - coeff * r[1];

    coeff = lhs[1][2];
    lhs[2][2] = lhs[2][2] - coeff * lhs[2][1];
    lhs[3][2] = lhs[3][2] - coeff * lhs[3][1];
    lhs[4][2] = lhs[4][2] - coeff * lhs[4][1];
    r[2]   = r[2]   - coeff * r[1];

    coeff = lhs[1][3];
    lhs[2][3] = lhs[2][3] - coeff * lhs[2][1];
    lhs[3][3] = lhs[3][3] - coeff * lhs[3][1];
    lhs[4][3] = lhs[4][3] - coeff * lhs[4][1];
    r[3]   = r[3]   - coeff * r[1];

    coeff = lhs[1][4];
    lhs[2][4] = lhs[2][4] - coeff * lhs[2][1];
    lhs[3][4] = lhs[3][4] - coeff * lhs[3][1];
    lhs[4][4] = lhs[4][4] - coeff * lhs[4][1];
    r[4]   = r[4]   - coeff * r[1];


    pivot = 1.00 / lhs[2][2];
    lhs[3][2] = lhs[3][2] * pivot;
    lhs[4][2] = lhs[4][2] * pivot;
    r[2]   = r[2]  * pivot;

    coeff = lhs[2][0];
    lhs[3][0] = lhs[3][0] - coeff * lhs[3][2];
    lhs[4][0] = lhs[4][0] - coeff * lhs[4][2];
    r[0]   = r[0]   - coeff * r[2];

    coeff = lhs[2][1];
    lhs[3][1] = lhs[3][1] - coeff * lhs[3][2];
    lhs[4][1] = lhs[4][1] - coeff * lhs[4][2];
    r[1]   = r[1]   - coeff * r[2];

    coeff = lhs[2][3];
    lhs[3][3] = lhs[3][3] - coeff * lhs[3][2];
    lhs[4][3] = lhs[4][3] - coeff * lhs[4][2];
    r[3]   = r[3]   - coeff * r[2];

    coeff = lhs[2][4];
    lhs[3][4] = lhs[3][4] - coeff * lhs[3][2];
    lhs[4][4] = lhs[4][4] - coeff * lhs[4][2];
    r[4]   = r[4]   - coeff * r[2];


    pivot = 1.00 / lhs[3][3];
    lhs[4][3] = lhs[4][3] * pivot;
    r[3]   = r[3]  * pivot;

    coeff = lhs[3][0];
    lhs[4][0] = lhs[4][0] - coeff * lhs[4][3];
    r[0]   = r[0]   - coeff * r[3];

    coeff = lhs[3][1];
    lhs[4][1] = lhs[4][1] - coeff * lhs[4][3];
    r[1]   = r[1]   - coeff * r[3];

    coeff = lhs[3][2];
    lhs[4][2] = lhs[4][2] - coeff * lhs[4][3];
    r[2]   = r[2]   - coeff * r[3];

    coeff = lhs[3][4];
    lhs[4][4] = lhs[4][4] - coeff * lhs[4][3];
    r[4]   = r[4]   - coeff * r[3];


    pivot = 1.00 / lhs[4][4];
    r[4]   = r[4]  * pivot;

    coeff = lhs[4][0];
    r[0]   = r[0]   - coeff * r[4];

    coeff = lhs[4][1];
    r[1]   = r[1]   - coeff * r[4];

    coeff = lhs[4][2];
    r[2]   = r[2]   - coeff * r[4];

    coeff = lhs[4][3];
    r[3]   = r[3]   - coeff * r[4];
}

//---------------------------------------------------------------------
// verification routine
//---------------------------------------------------------------------
void verify(int no_time_steps, char *Class, int *verified)
{
    double xcrref[5], xceref[5], xcrdif[5], xcedif[5];
    double epsilon, xce[5], xcr[5], dtref = 0.0;
    int m;

    //---------------------------------------------------------------------
    // tolerance level
    //---------------------------------------------------------------------
    epsilon = 1.0e-08;

    //---------------------------------------------------------------------
    // compute the error norm and the residual norm, and exit if not printing
    //---------------------------------------------------------------------
    error_norm(xce);
    compute_rhs();

    rhs_norm(xcr);

    for (m = 0; m < 5; m++)
    {
        xcr[m] = xcr[m] / dt;
    }

    *Class = 'U';
    *verified = 1;

    for (m = 0; m < 5; m++)
    {
        xcrref[m] = 1.0;
        xceref[m] = 1.0;
    }

    //---------------------------------------------------------------------
    // reference data for 12X12X12 grids after 60 time steps, with DT = 1.0e-02
    //---------------------------------------------------------------------
    if ( (grid_points[0] == 12) && (grid_points[1] == 12) &&
            (grid_points[2] == 12) && (no_time_steps == 60))
    {

        *Class = 'S';
        dtref = 1.0e-2;

        //---------------------------------------------------------------------
        // Reference values of RMS-norms of residual.
        //---------------------------------------------------------------------
        xcrref[0] = 1.7034283709541311e-01;
        xcrref[1] = 1.2975252070034097e-02;
        xcrref[2] = 3.2527926989486055e-02;
        xcrref[3] = 2.6436421275166801e-02;
        xcrref[4] = 1.9211784131744430e-01;

        //---------------------------------------------------------------------
        // Reference values of RMS-norms of solution error.
        //---------------------------------------------------------------------
        xceref[0] = 4.9976913345811579e-04;
        xceref[1] = 4.5195666782961927e-05;
        xceref[2] = 7.3973765172921357e-05;
        xceref[3] = 7.3821238632439731e-05;
        xceref[4] = 8.9269630987491446e-04;

        //---------------------------------------------------------------------
        // reference data for 24X24X24 grids after 200 time steps,
        // with DT = 0.8e-3
        //---------------------------------------------------------------------
    }
    else if ( (grid_points[0] == 24) && (grid_points[1] == 24) &&
              (grid_points[2] == 24) && (no_time_steps == 200) )
    {

        *Class = 'W';
        dtref = 0.8e-3;
        //---------------------------------------------------------------------
        // Reference values of RMS-norms of residual.
        //---------------------------------------------------------------------
        xcrref[0] = 0.1125590409344e+03;
        xcrref[1] = 0.1180007595731e+02;
        xcrref[2] = 0.2710329767846e+02;
        xcrref[3] = 0.2469174937669e+02;
        xcrref[4] = 0.2638427874317e+03;

        //---------------------------------------------------------------------
        // Reference values of RMS-norms of solution error.
        //---------------------------------------------------------------------
        xceref[0] = 0.4419655736008e+01;
        xceref[1] = 0.4638531260002e+00;
        xceref[2] = 0.1011551749967e+01;
        xceref[3] = 0.9235878729944e+00;
        xceref[4] = 0.1018045837718e+02;

        //---------------------------------------------------------------------
        // reference data for 64X64X64 grids after 200 time steps,
        // with DT = 0.8e-3
        //---------------------------------------------------------------------
    }
    else if ( (grid_points[0] == 64) && (grid_points[1] == 64) &&
              (grid_points[2] == 64) && (no_time_steps == 200) )
    {

        *Class = 'A';
        dtref = 0.8e-3;
        //---------------------------------------------------------------------
        // Reference values of RMS-norms of residual.
        //---------------------------------------------------------------------
        xcrref[0] = 1.0806346714637264e+02;
        xcrref[1] = 1.1319730901220813e+01;
        xcrref[2] = 2.5974354511582465e+01;
        xcrref[3] = 2.3665622544678910e+01;
        xcrref[4] = 2.5278963211748344e+02;

        //---------------------------------------------------------------------
        // Reference values of RMS-norms of solution error.
        //---------------------------------------------------------------------
        xceref[0] = 4.2348416040525025e+00;
        xceref[1] = 4.4390282496995698e-01;
        xceref[2] = 9.6692480136345650e-01;
        xceref[3] = 8.8302063039765474e-01;
        xceref[4] = 9.7379901770829278e+00;

        //---------------------------------------------------------------------
        // reference data for 102X102X102 grids after 200 time steps,
        // with DT = 3.0e-04
        //---------------------------------------------------------------------
    }
    else if ( (grid_points[0] == 102) && (grid_points[1] == 102) &&
              (grid_points[2] == 102) && (no_time_steps == 200) )
    {

        *Class = 'B';
        dtref = 3.0e-4;

        //---------------------------------------------------------------------
        // Reference values of RMS-norms of residual.
        //---------------------------------------------------------------------
        xcrref[0] = 1.4233597229287254e+03;
        xcrref[1] = 9.9330522590150238e+01;
        xcrref[2] = 3.5646025644535285e+02;
        xcrref[3] = 3.2485447959084092e+02;
        xcrref[4] = 3.2707541254659363e+03;

        //---------------------------------------------------------------------
        // Reference values of RMS-norms of solution error.
        //---------------------------------------------------------------------
        xceref[0] = 5.2969847140936856e+01;
        xceref[1] = 4.4632896115670668e+00;
        xceref[2] = 1.3122573342210174e+01;
        xceref[3] = 1.2006925323559144e+01;
        xceref[4] = 1.2459576151035986e+02;

        //---------------------------------------------------------------------
        // reference data for 162X162X162 grids after 200 time steps,
        // with DT = 1.0e-04
        //---------------------------------------------------------------------
    }
    else if ( (grid_points[0] == 162) && (grid_points[1] == 162) &&
              (grid_points[2] == 162) && (no_time_steps == 200) )
    {

        *Class = 'C';
        dtref = 1.0e-4;

        //---------------------------------------------------------------------
        // Reference values of RMS-norms of residual.
        //---------------------------------------------------------------------
        xcrref[0] = 0.62398116551764615e+04;
        xcrref[1] = 0.50793239190423964e+03;
        xcrref[2] = 0.15423530093013596e+04;
        xcrref[3] = 0.13302387929291190e+04;
        xcrref[4] = 0.11604087428436455e+05;

        //---------------------------------------------------------------------
        // Reference values of RMS-norms of solution error.
        //---------------------------------------------------------------------
        xceref[0] = 0.16462008369091265e+03;
        xceref[1] = 0.11497107903824313e+02;
        xceref[2] = 0.41207446207461508e+02;
        xceref[3] = 0.37087651059694167e+02;
        xceref[4] = 0.36211053051841265e+03;

        //---------------------------------------------------------------------
        // reference data for 408x408x408 grids after 250 time steps,
        // with DT = 0.2e-04
        //---------------------------------------------------------------------
    }
    else if ( (grid_points[0] == 408) && (grid_points[1] == 408) &&
              (grid_points[2] == 408) && (no_time_steps == 250) )
    {

        *Class = 'D';
        dtref = 0.2e-4;

        //---------------------------------------------------------------------
        // Reference values of RMS-norms of residual.
        //---------------------------------------------------------------------
        xcrref[0] = 0.2533188551738e+05;
        xcrref[1] = 0.2346393716980e+04;
        xcrref[2] = 0.6294554366904e+04;
        xcrref[3] = 0.5352565376030e+04;
        xcrref[4] = 0.3905864038618e+05;

        //---------------------------------------------------------------------
        // Reference values of RMS-norms of solution error.
        //---------------------------------------------------------------------
        xceref[0] = 0.3100009377557e+03;
        xceref[1] = 0.2424086324913e+02;
        xceref[2] = 0.7782212022645e+02;
        xceref[3] = 0.6835623860116e+02;
        xceref[4] = 0.6065737200368e+03;

        //---------------------------------------------------------------------
        // reference data for 1020x1020x1020 grids after 250 time steps,
        // with DT = 0.4e-05
        //---------------------------------------------------------------------
    }
    else if ( (grid_points[0] == 1020) && (grid_points[1] == 1020) &&
              (grid_points[2] == 1020) && (no_time_steps == 250) )
    {

        *Class = 'E';
        dtref = 0.4e-5;

        //---------------------------------------------------------------------
        // Reference values of RMS-norms of residual.
        //---------------------------------------------------------------------
        xcrref[0] = 0.9795372484517e+05;
        xcrref[1] = 0.9739814511521e+04;
        xcrref[2] = 0.2467606342965e+05;
        xcrref[3] = 0.2092419572860e+05;
        xcrref[4] = 0.1392138856939e+06;

        //---------------------------------------------------------------------
        // Reference values of RMS-norms of solution error.
        //---------------------------------------------------------------------
        xceref[0] = 0.4327562208414e+03;
        xceref[1] = 0.3699051964887e+02;
        xceref[2] = 0.1089845040954e+03;
        xceref[3] = 0.9462517622043e+02;
        xceref[4] = 0.7765512765309e+03;

    }
    else
    {
        *verified = 0;
    }

    //---------------------------------------------------------------------
    // verification test for residuals if gridsize is one of
    // the defined grid sizes above (*Class != 'U')
    //---------------------------------------------------------------------

    //---------------------------------------------------------------------
    // Compute the difference of solution values and the known reference values.
    //---------------------------------------------------------------------
    for (m = 0; m < 5; m++)
    {
        xcrdif[m] = fabs((xcr[m] - xcrref[m]) / xcrref[m]);
        xcedif[m] = fabs((xce[m] - xceref[m]) / xceref[m]);
    }

    //---------------------------------------------------------------------
    // Output the comparison of computed results to known cases.
    //---------------------------------------------------------------------
    if (*Class != 'U')
    {
        printf(" Verification being performed for class %c\n", *Class);
        printf(" accuracy setting for epsilon = %20.13E\n", epsilon);
        *verified = (fabs(dt - dtref) <= epsilon);
        if (!(*verified))
        {
            *Class = 'U';
            printf(" DT does not match the reference value of %15.8E\n", dtref);
        }
    }
    else
    {
        printf(" Unknown class\n");
    }

    if (*Class != 'U')
    {
        printf(" Comparison of RMS-norms of residual\n");
    }
    else
    {
        printf(" RMS-norms of residual\n");
    }

    for (m = 0; m < 5; m++)
    {
        if (*Class == 'U')
        {
            printf("          %2d%20.13E\n", m + 1, xcr[m]);
        }
        else if (xcrdif[m] <= epsilon)
        {
            printf("          %2d%20.13E%20.13E%20.13E\n",
                   m + 1, xcr[m], xcrref[m], xcrdif[m]);
        }
        else
        {
            *verified = 0;
            printf(" FAILURE: %2d%20.13E%20.13E%20.13E\n",
                   m + 1, xcr[m], xcrref[m], xcrdif[m]);
        }
    }

    if (*Class != 'U')
    {
        printf(" Comparison of RMS-norms of solution error\n");
    }
    else
    {
        printf(" RMS-norms of solution error\n");
    }

    for (m = 0; m < 5; m++)
    {
        if (*Class == 'U')
        {
            printf("          %2d%20.13E\n", m + 1, xce[m]);
        }
        else if (xcedif[m] <= epsilon)
        {
            printf("          %2d%20.13E%20.13E%20.13E\n",
                   m + 1, xce[m], xceref[m], xcedif[m]);
        }
        else
        {
            *verified = 0;
            printf(" FAILURE: %2d%20.13E%20.13E%20.13E\n",
                   m + 1, xce[m], xceref[m], xcedif[m]);
        }
    }

    if (*Class == 'U')
    {
        printf(" No reference values provided\n");
        printf(" No verification performed\n");
    }
    else if (*verified)
    {
        printf(" Verification Successful\n");
    }
    else
    {
        printf(" Verification failed\n");
    }
}

//---------------------------------------------------------------------
//
// Performs line solves in X direction by first factoring
// the block-tridiagonal matrix into an upper triangular matrix,
// and then performing back substitution to solve for the unknow
// vectors of each line.
//
// Make sure we treat elements zero to cell_size in the direction
// of the sweep.
//
//---------------------------------------------------------------------
void x_solve()
{
    int i, j, k, m, n, isize;

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    if (timeron) timer_start(t_xsolve);

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    //---------------------------------------------------------------------
    // This function computes the left hand side in the xi-direction
    //---------------------------------------------------------------------

    isize = grid_points[0] - 1;

    //---------------------------------------------------------------------
    // determine a (labeled f) and n jacobians
    //---------------------------------------------------------------------
    for (k = 1; k <= grid_points[2] - 2; k++)
    {
        for (j = 1; j <= grid_points[1] - 2; j++)
        {
            for (i = 0; i <= isize; i++)
            {
                tmp1 = rho_i[k][j][i];
                tmp2 = tmp1 * tmp1;
                tmp3 = tmp1 * tmp2;
                //-------------------------------------------------------------------
                //
                //-------------------------------------------------------------------
                fjac[i][0][0] = 0.0;
                fjac[i][1][0] = 1.0;
                fjac[i][2][0] = 0.0;
                fjac[i][3][0] = 0.0;
                fjac[i][4][0] = 0.0;

                fjac[i][0][1] = -(u[k][j][i][1] * tmp2 * u[k][j][i][1])
                                + c2 * qs[k][j][i];
                fjac[i][1][1] = ( 2.0 - c2 ) * ( u[k][j][i][1] / u[k][j][i][0] );
                fjac[i][2][1] = - c2 * ( u[k][j][i][2] * tmp1 );
                fjac[i][3][1] = - c2 * ( u[k][j][i][3] * tmp1 );
                fjac[i][4][1] = c2;

                fjac[i][0][2] = - ( u[k][j][i][1] * u[k][j][i][2] ) * tmp2;
                fjac[i][1][2] = u[k][j][i][2] * tmp1;
                fjac[i][2][2] = u[k][j][i][1] * tmp1;
                fjac[i][3][2] = 0.0;
                fjac[i][4][2] = 0.0;

                fjac[i][0][3] = - ( u[k][j][i][1] * u[k][j][i][3] ) * tmp2;
                fjac[i][1][3] = u[k][j][i][3] * tmp1;
                fjac[i][2][3] = 0.0;
                fjac[i][3][3] = u[k][j][i][1] * tmp1;
                fjac[i][4][3] = 0.0;

                fjac[i][0][4] = ( c2 * 2.0 * square[k][j][i] - c1 * u[k][j][i][4] )
                                * ( u[k][j][i][1] * tmp2 );
                fjac[i][1][4] = c1 *  u[k][j][i][4] * tmp1
                                - c2 * ( u[k][j][i][1] * u[k][j][i][1] * tmp2 + qs[k][j][i] );
                fjac[i][2][4] = - c2 * ( u[k][j][i][2] * u[k][j][i][1] ) * tmp2;
                fjac[i][3][4] = - c2 * ( u[k][j][i][3] * u[k][j][i][1] ) * tmp2;
                fjac[i][4][4] = c1 * ( u[k][j][i][1] * tmp1 );

                njac[i][0][0] = 0.0;
                njac[i][1][0] = 0.0;
                njac[i][2][0] = 0.0;
                njac[i][3][0] = 0.0;
                njac[i][4][0] = 0.0;

                njac[i][0][1] = - con43 * c3c4 * tmp2 * u[k][j][i][1];
                njac[i][1][1] =   con43 * c3c4 * tmp1;
                njac[i][2][1] =   0.0;
                njac[i][3][1] =   0.0;
                njac[i][4][1] =   0.0;

                njac[i][0][2] = - c3c4 * tmp2 * u[k][j][i][2];
                njac[i][1][2] =   0.0;
                njac[i][2][2] =   c3c4 * tmp1;
                njac[i][3][2] =   0.0;
                njac[i][4][2] =   0.0;

                njac[i][0][3] = - c3c4 * tmp2 * u[k][j][i][3];
                njac[i][1][3] =   0.0;
                njac[i][2][3] =   0.0;
                njac[i][3][3] =   c3c4 * tmp1;
                njac[i][4][3] =   0.0;

                njac[i][0][4] = - ( con43 * c3c4
                                    - c1345 ) * tmp3 * (u[k][j][i][1] * u[k][j][i][1])
                                - ( c3c4 - c1345 ) * tmp3 * (u[k][j][i][2] * u[k][j][i][2])
                                - ( c3c4 - c1345 ) * tmp3 * (u[k][j][i][3] * u[k][j][i][3])
                                - c1345 * tmp2 * u[k][j][i][4];

                njac[i][1][4] = ( con43 * c3c4
                                  - c1345 ) * tmp2 * u[k][j][i][1];
                njac[i][2][4] = ( c3c4 - c1345 ) * tmp2 * u[k][j][i][2];
                njac[i][3][4] = ( c3c4 - c1345 ) * tmp2 * u[k][j][i][3];
                njac[i][4][4] = ( c1345 ) * tmp1;
            }
            //---------------------------------------------------------------------
            // now jacobians set, so form left hand side in x direction
            //---------------------------------------------------------------------
            lhsinit(lhs, isize);
            for (i = 1; i <= isize - 1; i++)
            {
                tmp1 = dt * tx1;
                tmp2 = dt * tx2;

                lhs[i][AA][0][0] = - tmp2 * fjac[i - 1][0][0]
                                   - tmp1 * njac[i - 1][0][0]
                                   - tmp1 * dx1;
                lhs[i][AA][1][0] = - tmp2 * fjac[i - 1][1][0]
                                   - tmp1 * njac[i - 1][1][0];
                lhs[i][AA][2][0] = - tmp2 * fjac[i - 1][2][0]
                                   - tmp1 * njac[i - 1][2][0];
                lhs[i][AA][3][0] = - tmp2 * fjac[i - 1][3][0]
                                   - tmp1 * njac[i - 1][3][0];
                lhs[i][AA][4][0] = - tmp2 * fjac[i - 1][4][0]
                                   - tmp1 * njac[i - 1][4][0];

                lhs[i][AA][0][1] = - tmp2 * fjac[i - 1][0][1]
                                   - tmp1 * njac[i - 1][0][1];
                lhs[i][AA][1][1] = - tmp2 * fjac[i - 1][1][1]
                                   - tmp1 * njac[i - 1][1][1]
                                   - tmp1 * dx2;
                lhs[i][AA][2][1] = - tmp2 * fjac[i - 1][2][1]
                                   - tmp1 * njac[i - 1][2][1];
                lhs[i][AA][3][1] = - tmp2 * fjac[i - 1][3][1]
                                   - tmp1 * njac[i - 1][3][1];
                lhs[i][AA][4][1] = - tmp2 * fjac[i - 1][4][1]
                                   - tmp1 * njac[i - 1][4][1];

                lhs[i][AA][0][2] = - tmp2 * fjac[i - 1][0][2]
                                   - tmp1 * njac[i - 1][0][2];
                lhs[i][AA][1][2] = - tmp2 * fjac[i - 1][1][2]
                                   - tmp1 * njac[i - 1][1][2];
                lhs[i][AA][2][2] = - tmp2 * fjac[i - 1][2][2]
                                   - tmp1 * njac[i - 1][2][2]
                                   - tmp1 * dx3;
                lhs[i][AA][3][2] = - tmp2 * fjac[i - 1][3][2]
                                   - tmp1 * njac[i - 1][3][2];
                lhs[i][AA][4][2] = - tmp2 * fjac[i - 1][4][2]
                                   - tmp1 * njac[i - 1][4][2];

                lhs[i][AA][0][3] = - tmp2 * fjac[i - 1][0][3]
                                   - tmp1 * njac[i - 1][0][3];
                lhs[i][AA][1][3] = - tmp2 * fjac[i - 1][1][3]
                                   - tmp1 * njac[i - 1][1][3];
                lhs[i][AA][2][3] = - tmp2 * fjac[i - 1][2][3]
                                   - tmp1 * njac[i - 1][2][3];
                lhs[i][AA][3][3] = - tmp2 * fjac[i - 1][3][3]
                                   - tmp1 * njac[i - 1][3][3]
                                   - tmp1 * dx4;
                lhs[i][AA][4][3] = - tmp2 * fjac[i - 1][4][3]
                                   - tmp1 * njac[i - 1][4][3];

                lhs[i][AA][0][4] = - tmp2 * fjac[i - 1][0][4]
                                   - tmp1 * njac[i - 1][0][4];
                lhs[i][AA][1][4] = - tmp2 * fjac[i - 1][1][4]
                                   - tmp1 * njac[i - 1][1][4];
                lhs[i][AA][2][4] = - tmp2 * fjac[i - 1][2][4]
                                   - tmp1 * njac[i - 1][2][4];
                lhs[i][AA][3][4] = - tmp2 * fjac[i - 1][3][4]
                                   - tmp1 * njac[i - 1][3][4];
                lhs[i][AA][4][4] = - tmp2 * fjac[i - 1][4][4]
                                   - tmp1 * njac[i - 1][4][4]
                                   - tmp1 * dx5;

                lhs[i][BB][0][0] = 1.0
                                   + tmp1 * 2.0 * njac[i][0][0]
                                   + tmp1 * 2.0 * dx1;
                lhs[i][BB][1][0] = tmp1 * 2.0 * njac[i][1][0];
                lhs[i][BB][2][0] = tmp1 * 2.0 * njac[i][2][0];
                lhs[i][BB][3][0] = tmp1 * 2.0 * njac[i][3][0];
                lhs[i][BB][4][0] = tmp1 * 2.0 * njac[i][4][0];

                lhs[i][BB][0][1] = tmp1 * 2.0 * njac[i][0][1];
                lhs[i][BB][1][1] = 1.0
                                   + tmp1 * 2.0 * njac[i][1][1]
                                   + tmp1 * 2.0 * dx2;
                lhs[i][BB][2][1] = tmp1 * 2.0 * njac[i][2][1];
                lhs[i][BB][3][1] = tmp1 * 2.0 * njac[i][3][1];
                lhs[i][BB][4][1] = tmp1 * 2.0 * njac[i][4][1];

                lhs[i][BB][0][2] = tmp1 * 2.0 * njac[i][0][2];
                lhs[i][BB][1][2] = tmp1 * 2.0 * njac[i][1][2];
                lhs[i][BB][2][2] = 1.0
                                   + tmp1 * 2.0 * njac[i][2][2]
                                   + tmp1 * 2.0 * dx3;
                lhs[i][BB][3][2] = tmp1 * 2.0 * njac[i][3][2];
                lhs[i][BB][4][2] = tmp1 * 2.0 * njac[i][4][2];

                lhs[i][BB][0][3] = tmp1 * 2.0 * njac[i][0][3];
                lhs[i][BB][1][3] = tmp1 * 2.0 * njac[i][1][3];
                lhs[i][BB][2][3] = tmp1 * 2.0 * njac[i][2][3];
                lhs[i][BB][3][3] = 1.0
                                   + tmp1 * 2.0 * njac[i][3][3]
                                   + tmp1 * 2.0 * dx4;
                lhs[i][BB][4][3] = tmp1 * 2.0 * njac[i][4][3];

                lhs[i][BB][0][4] = tmp1 * 2.0 * njac[i][0][4];
                lhs[i][BB][1][4] = tmp1 * 2.0 * njac[i][1][4];
                lhs[i][BB][2][4] = tmp1 * 2.0 * njac[i][2][4];
                lhs[i][BB][3][4] = tmp1 * 2.0 * njac[i][3][4];
                lhs[i][BB][4][4] = 1.0
                                   + tmp1 * 2.0 * njac[i][4][4]
                                   + tmp1 * 2.0 * dx5;

                lhs[i][CC][0][0] =  tmp2 * fjac[i + 1][0][0]
                                    - tmp1 * njac[i + 1][0][0]
                                    - tmp1 * dx1;
                lhs[i][CC][1][0] =  tmp2 * fjac[i + 1][1][0]
                                    - tmp1 * njac[i + 1][1][0];
                lhs[i][CC][2][0] =  tmp2 * fjac[i + 1][2][0]
                                    - tmp1 * njac[i + 1][2][0];
                lhs[i][CC][3][0] =  tmp2 * fjac[i + 1][3][0]
                                    - tmp1 * njac[i + 1][3][0];
                lhs[i][CC][4][0] =  tmp2 * fjac[i + 1][4][0]
                                    - tmp1 * njac[i + 1][4][0];

                lhs[i][CC][0][1] =  tmp2 * fjac[i + 1][0][1]
                                    - tmp1 * njac[i + 1][0][1];
                lhs[i][CC][1][1] =  tmp2 * fjac[i + 1][1][1]
                                    - tmp1 * njac[i + 1][1][1]
                                    - tmp1 * dx2;
                lhs[i][CC][2][1] =  tmp2 * fjac[i + 1][2][1]
                                    - tmp1 * njac[i + 1][2][1];
                lhs[i][CC][3][1] =  tmp2 * fjac[i + 1][3][1]
                                    - tmp1 * njac[i + 1][3][1];
                lhs[i][CC][4][1] =  tmp2 * fjac[i + 1][4][1]
                                    - tmp1 * njac[i + 1][4][1];

                lhs[i][CC][0][2] =  tmp2 * fjac[i + 1][0][2]
                                    - tmp1 * njac[i + 1][0][2];
                lhs[i][CC][1][2] =  tmp2 * fjac[i + 1][1][2]
                                    - tmp1 * njac[i + 1][1][2];
                lhs[i][CC][2][2] =  tmp2 * fjac[i + 1][2][2]
                                    - tmp1 * njac[i + 1][2][2]
                                    - tmp1 * dx3;
                lhs[i][CC][3][2] =  tmp2 * fjac[i + 1][3][2]
                                    - tmp1 * njac[i + 1][3][2];
                lhs[i][CC][4][2] =  tmp2 * fjac[i + 1][4][2]
                                    - tmp1 * njac[i + 1][4][2];

                lhs[i][CC][0][3] =  tmp2 * fjac[i + 1][0][3]
                                    - tmp1 * njac[i + 1][0][3];
                lhs[i][CC][1][3] =  tmp2 * fjac[i + 1][1][3]
                                    - tmp1 * njac[i + 1][1][3];
                lhs[i][CC][2][3] =  tmp2 * fjac[i + 1][2][3]
                                    - tmp1 * njac[i + 1][2][3];
                lhs[i][CC][3][3] =  tmp2 * fjac[i + 1][3][3]
                                    - tmp1 * njac[i + 1][3][3]
                                    - tmp1 * dx4;
                lhs[i][CC][4][3] =  tmp2 * fjac[i + 1][4][3]
                                    - tmp1 * njac[i + 1][4][3];

                lhs[i][CC][0][4] =  tmp2 * fjac[i + 1][0][4]
                                    - tmp1 * njac[i + 1][0][4];
                lhs[i][CC][1][4] =  tmp2 * fjac[i + 1][1][4]
                                    - tmp1 * njac[i + 1][1][4];
                lhs[i][CC][2][4] =  tmp2 * fjac[i + 1][2][4]
                                    - tmp1 * njac[i + 1][2][4];
                lhs[i][CC][3][4] =  tmp2 * fjac[i + 1][3][4]
                                    - tmp1 * njac[i + 1][3][4];
                lhs[i][CC][4][4] =  tmp2 * fjac[i + 1][4][4]
                                    - tmp1 * njac[i + 1][4][4]
                                    - tmp1 * dx5;
            }

            //---------------------------------------------------------------------
            //---------------------------------------------------------------------

            //---------------------------------------------------------------------
            // performs guaussian elimination on this cell.
            //
            // assumes that unpacking routines for non-first cells
            // preload C' and rhs' from previous cell.
            //
            // assumed send happens outside this routine, but that
            // c'(IMAX) and rhs'(IMAX) will be sent to next cell
            //---------------------------------------------------------------------

            //---------------------------------------------------------------------
            // outer most do loops - sweeping in i direction
            //---------------------------------------------------------------------

            //---------------------------------------------------------------------
            // multiply c[k][j][0] by b_inverse and copy back to c
            // multiply rhs(0) by b_inverse(0) and copy to rhs
            //---------------------------------------------------------------------
            binvcrhs( lhs[0][BB], lhs[0][CC], rhs[k][j][0] );

            //---------------------------------------------------------------------
            // begin inner most do loop
            // do all the elements of the cell unless last
            //---------------------------------------------------------------------
            for (i = 1; i <= isize - 1; i++)
            {
                //-------------------------------------------------------------------
                // rhs(i) = rhs(i) - A*rhs(i-1)
                //-------------------------------------------------------------------
                matvec_sub(lhs[i][AA], rhs[k][j][i - 1], rhs[k][j][i]);

                //-------------------------------------------------------------------
                // B(i) = B(i) - C(i-1)*A(i)
                //-------------------------------------------------------------------
                matmul_sub(lhs[i][AA], lhs[i - 1][CC], lhs[i][BB]);


                //-------------------------------------------------------------------
                // multiply c[k][j][i] by b_inverse and copy back to c
                // multiply rhs[k][j][0] by b_inverse[k][j][0] and copy to rhs
                //-------------------------------------------------------------------
                binvcrhs( lhs[i][BB], lhs[i][CC], rhs[k][j][i] );
            }

            //---------------------------------------------------------------------
            // rhs(isize) = rhs(isize) - A*rhs(isize-1)
            //---------------------------------------------------------------------
            matvec_sub(lhs[isize][AA], rhs[k][j][isize - 1], rhs[k][j][isize]);

            //---------------------------------------------------------------------
            // B(isize) = B(isize) - C(isize-1)*A(isize)
            //---------------------------------------------------------------------
            matmul_sub(lhs[isize][AA], lhs[isize - 1][CC], lhs[isize][BB]);

            //---------------------------------------------------------------------
            // multiply rhs() by b_inverse() and copy to rhs
            //---------------------------------------------------------------------
            binvrhs( lhs[isize][BB], rhs[k][j][isize] );

            //---------------------------------------------------------------------
            // back solve: if last cell, then generate U(isize)=rhs(isize)
            // else assume U(isize) is loaded in un pack backsub_info
            // so just use it
            // after u(istart) will be sent to next cell
            //---------------------------------------------------------------------
            for (i = isize - 1; i >= 0; i--)
            {
                for (m = 0; m < BLOCK_SIZE; m++)
                {
                    for (n = 0; n < BLOCK_SIZE; n++)
                    {
                        rhs[k][j][i][m] = rhs[k][j][i][m]
                                          - lhs[i][CC][n][m] * rhs[k][j][i + 1][n];
                    }
                }
            }
        }
    }
    if (timeron) timer_stop(t_xsolve);
}

//---------------------------------------------------------------------
// Performs line solves in Y direction by first factoring
// the block-tridiagonal matrix into an upper triangular matrix,
// and then performing back substitution to solve for the unknow
// vectors of each line.
//
// Make sure we treat elements zero to cell_size in the direction
// of the sweep.
//---------------------------------------------------------------------
void y_solve()
{
    int i, j, k, m, n, jsize;

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    if (timeron) timer_start(t_ysolve);

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    //---------------------------------------------------------------------
    // This function computes the left hand side for the three y-factors
    //---------------------------------------------------------------------

    jsize = grid_points[1] - 1;

    //---------------------------------------------------------------------
    // Compute the indices for storing the tri-diagonal matrix;
    // determine a (labeled f) and n jacobians for cell c
    //---------------------------------------------------------------------
    for (k = 1; k <= grid_points[2] - 2; k++)
    {
        for (i = 1; i <= grid_points[0] - 2; i++)
        {
            for (j = 0; j <= jsize; j++)
            {
                tmp1 = rho_i[k][j][i];
                tmp2 = tmp1 * tmp1;
                tmp3 = tmp1 * tmp2;

                fjac[j][0][0] = 0.0;
                fjac[j][1][0] = 0.0;
                fjac[j][2][0] = 1.0;
                fjac[j][3][0] = 0.0;
                fjac[j][4][0] = 0.0;

                fjac[j][0][1] = - ( u[k][j][i][1] * u[k][j][i][2] ) * tmp2;
                fjac[j][1][1] = u[k][j][i][2] * tmp1;
                fjac[j][2][1] = u[k][j][i][1] * tmp1;
                fjac[j][3][1] = 0.0;
                fjac[j][4][1] = 0.0;

                fjac[j][0][2] = - ( u[k][j][i][2] * u[k][j][i][2] * tmp2)
                                + c2 * qs[k][j][i];
                fjac[j][1][2] = - c2 *  u[k][j][i][1] * tmp1;
                fjac[j][2][2] = ( 2.0 - c2 ) *  u[k][j][i][2] * tmp1;
                fjac[j][3][2] = - c2 * u[k][j][i][3] * tmp1;
                fjac[j][4][2] = c2;

                fjac[j][0][3] = - ( u[k][j][i][2] * u[k][j][i][3] ) * tmp2;
                fjac[j][1][3] = 0.0;
                fjac[j][2][3] = u[k][j][i][3] * tmp1;
                fjac[j][3][3] = u[k][j][i][2] * tmp1;
                fjac[j][4][3] = 0.0;

                fjac[j][0][4] = ( c2 * 2.0 * square[k][j][i] - c1 * u[k][j][i][4] )
                                * u[k][j][i][2] * tmp2;
                fjac[j][1][4] = - c2 * u[k][j][i][1] * u[k][j][i][2] * tmp2;
                fjac[j][2][4] = c1 * u[k][j][i][4] * tmp1
                                - c2 * ( qs[k][j][i] + u[k][j][i][2] * u[k][j][i][2] * tmp2 );
                fjac[j][3][4] = - c2 * ( u[k][j][i][2] * u[k][j][i][3] ) * tmp2;
                fjac[j][4][4] = c1 * u[k][j][i][2] * tmp1;

                njac[j][0][0] = 0.0;
                njac[j][1][0] = 0.0;
                njac[j][2][0] = 0.0;
                njac[j][3][0] = 0.0;
                njac[j][4][0] = 0.0;

                njac[j][0][1] = - c3c4 * tmp2 * u[k][j][i][1];
                njac[j][1][1] =   c3c4 * tmp1;
                njac[j][2][1] =   0.0;
                njac[j][3][1] =   0.0;
                njac[j][4][1] =   0.0;

                njac[j][0][2] = - con43 * c3c4 * tmp2 * u[k][j][i][2];
                njac[j][1][2] =   0.0;
                njac[j][2][2] =   con43 * c3c4 * tmp1;
                njac[j][3][2] =   0.0;
                njac[j][4][2] =   0.0;

                njac[j][0][3] = - c3c4 * tmp2 * u[k][j][i][3];
                njac[j][1][3] =   0.0;
                njac[j][2][3] =   0.0;
                njac[j][3][3] =   c3c4 * tmp1;
                njac[j][4][3] =   0.0;

                njac[j][0][4] = - (  c3c4
                                     - c1345 ) * tmp3 * (u[k][j][i][1] * u[k][j][i][1])
                                - ( con43 * c3c4
                                    - c1345 ) * tmp3 * (u[k][j][i][2] * u[k][j][i][2])
                                - ( c3c4 - c1345 ) * tmp3 * (u[k][j][i][3] * u[k][j][i][3])
                                - c1345 * tmp2 * u[k][j][i][4];

                njac[j][1][4] = (  c3c4 - c1345 ) * tmp2 * u[k][j][i][1];
                njac[j][2][4] = ( con43 * c3c4 - c1345 ) * tmp2 * u[k][j][i][2];
                njac[j][3][4] = ( c3c4 - c1345 ) * tmp2 * u[k][j][i][3];
                njac[j][4][4] = ( c1345 ) * tmp1;
            }

            //---------------------------------------------------------------------
            // now joacobians set, so form left hand side in y direction
            //---------------------------------------------------------------------
            lhsinit(lhs, jsize);
            for (j = 1; j <= jsize - 1; j++)
            {
                tmp1 = dt * ty1;
                tmp2 = dt * ty2;

                lhs[j][AA][0][0] = - tmp2 * fjac[j - 1][0][0]
                                   - tmp1 * njac[j - 1][0][0]
                                   - tmp1 * dy1;
                lhs[j][AA][1][0] = - tmp2 * fjac[j - 1][1][0]
                                   - tmp1 * njac[j - 1][1][0];
                lhs[j][AA][2][0] = - tmp2 * fjac[j - 1][2][0]
                                   - tmp1 * njac[j - 1][2][0];
                lhs[j][AA][3][0] = - tmp2 * fjac[j - 1][3][0]
                                   - tmp1 * njac[j - 1][3][0];
                lhs[j][AA][4][0] = - tmp2 * fjac[j - 1][4][0]
                                   - tmp1 * njac[j - 1][4][0];

                lhs[j][AA][0][1] = - tmp2 * fjac[j - 1][0][1]
                                   - tmp1 * njac[j - 1][0][1];
                lhs[j][AA][1][1] = - tmp2 * fjac[j - 1][1][1]
                                   - tmp1 * njac[j - 1][1][1]
                                   - tmp1 * dy2;
                lhs[j][AA][2][1] = - tmp2 * fjac[j - 1][2][1]
                                   - tmp1 * njac[j - 1][2][1];
                lhs[j][AA][3][1] = - tmp2 * fjac[j - 1][3][1]
                                   - tmp1 * njac[j - 1][3][1];
                lhs[j][AA][4][1] = - tmp2 * fjac[j - 1][4][1]
                                   - tmp1 * njac[j - 1][4][1];

                lhs[j][AA][0][2] = - tmp2 * fjac[j - 1][0][2]
                                   - tmp1 * njac[j - 1][0][2];
                lhs[j][AA][1][2] = - tmp2 * fjac[j - 1][1][2]
                                   - tmp1 * njac[j - 1][1][2];
                lhs[j][AA][2][2] = - tmp2 * fjac[j - 1][2][2]
                                   - tmp1 * njac[j - 1][2][2]
                                   - tmp1 * dy3;
                lhs[j][AA][3][2] = - tmp2 * fjac[j - 1][3][2]
                                   - tmp1 * njac[j - 1][3][2];
                lhs[j][AA][4][2] = - tmp2 * fjac[j - 1][4][2]
                                   - tmp1 * njac[j - 1][4][2];

                lhs[j][AA][0][3] = - tmp2 * fjac[j - 1][0][3]
                                   - tmp1 * njac[j - 1][0][3];
                lhs[j][AA][1][3] = - tmp2 * fjac[j - 1][1][3]
                                   - tmp1 * njac[j - 1][1][3];
                lhs[j][AA][2][3] = - tmp2 * fjac[j - 1][2][3]
                                   - tmp1 * njac[j - 1][2][3];
                lhs[j][AA][3][3] = - tmp2 * fjac[j - 1][3][3]
                                   - tmp1 * njac[j - 1][3][3]
                                   - tmp1 * dy4;
                lhs[j][AA][4][3] = - tmp2 * fjac[j - 1][4][3]
                                   - tmp1 * njac[j - 1][4][3];

                lhs[j][AA][0][4] = - tmp2 * fjac[j - 1][0][4]
                                   - tmp1 * njac[j - 1][0][4];
                lhs[j][AA][1][4] = - tmp2 * fjac[j - 1][1][4]
                                   - tmp1 * njac[j - 1][1][4];
                lhs[j][AA][2][4] = - tmp2 * fjac[j - 1][2][4]
                                   - tmp1 * njac[j - 1][2][4];
                lhs[j][AA][3][4] = - tmp2 * fjac[j - 1][3][4]
                                   - tmp1 * njac[j - 1][3][4];
                lhs[j][AA][4][4] = - tmp2 * fjac[j - 1][4][4]
                                   - tmp1 * njac[j - 1][4][4]
                                   - tmp1 * dy5;

                lhs[j][BB][0][0] = 1.0
                                   + tmp1 * 2.0 * njac[j][0][0]
                                   + tmp1 * 2.0 * dy1;
                lhs[j][BB][1][0] = tmp1 * 2.0 * njac[j][1][0];
                lhs[j][BB][2][0] = tmp1 * 2.0 * njac[j][2][0];
                lhs[j][BB][3][0] = tmp1 * 2.0 * njac[j][3][0];
                lhs[j][BB][4][0] = tmp1 * 2.0 * njac[j][4][0];

                lhs[j][BB][0][1] = tmp1 * 2.0 * njac[j][0][1];
                lhs[j][BB][1][1] = 1.0
                                   + tmp1 * 2.0 * njac[j][1][1]
                                   + tmp1 * 2.0 * dy2;
                lhs[j][BB][2][1] = tmp1 * 2.0 * njac[j][2][1];
                lhs[j][BB][3][1] = tmp1 * 2.0 * njac[j][3][1];
                lhs[j][BB][4][1] = tmp1 * 2.0 * njac[j][4][1];

                lhs[j][BB][0][2] = tmp1 * 2.0 * njac[j][0][2];
                lhs[j][BB][1][2] = tmp1 * 2.0 * njac[j][1][2];
                lhs[j][BB][2][2] = 1.0
                                   + tmp1 * 2.0 * njac[j][2][2]
                                   + tmp1 * 2.0 * dy3;
                lhs[j][BB][3][2] = tmp1 * 2.0 * njac[j][3][2];
                lhs[j][BB][4][2] = tmp1 * 2.0 * njac[j][4][2];

                lhs[j][BB][0][3] = tmp1 * 2.0 * njac[j][0][3];
                lhs[j][BB][1][3] = tmp1 * 2.0 * njac[j][1][3];
                lhs[j][BB][2][3] = tmp1 * 2.0 * njac[j][2][3];
                lhs[j][BB][3][3] = 1.0
                                   + tmp1 * 2.0 * njac[j][3][3]
                                   + tmp1 * 2.0 * dy4;
                lhs[j][BB][4][3] = tmp1 * 2.0 * njac[j][4][3];

                lhs[j][BB][0][4] = tmp1 * 2.0 * njac[j][0][4];
                lhs[j][BB][1][4] = tmp1 * 2.0 * njac[j][1][4];
                lhs[j][BB][2][4] = tmp1 * 2.0 * njac[j][2][4];
                lhs[j][BB][3][4] = tmp1 * 2.0 * njac[j][3][4];
                lhs[j][BB][4][4] = 1.0
                                   + tmp1 * 2.0 * njac[j][4][4]
                                   + tmp1 * 2.0 * dy5;

                lhs[j][CC][0][0] =  tmp2 * fjac[j + 1][0][0]
                                    - tmp1 * njac[j + 1][0][0]
                                    - tmp1 * dy1;
                lhs[j][CC][1][0] =  tmp2 * fjac[j + 1][1][0]
                                    - tmp1 * njac[j + 1][1][0];
                lhs[j][CC][2][0] =  tmp2 * fjac[j + 1][2][0]
                                    - tmp1 * njac[j + 1][2][0];
                lhs[j][CC][3][0] =  tmp2 * fjac[j + 1][3][0]
                                    - tmp1 * njac[j + 1][3][0];
                lhs[j][CC][4][0] =  tmp2 * fjac[j + 1][4][0]
                                    - tmp1 * njac[j + 1][4][0];

                lhs[j][CC][0][1] =  tmp2 * fjac[j + 1][0][1]
                                    - tmp1 * njac[j + 1][0][1];
                lhs[j][CC][1][1] =  tmp2 * fjac[j + 1][1][1]
                                    - tmp1 * njac[j + 1][1][1]
                                    - tmp1 * dy2;
                lhs[j][CC][2][1] =  tmp2 * fjac[j + 1][2][1]
                                    - tmp1 * njac[j + 1][2][1];
                lhs[j][CC][3][1] =  tmp2 * fjac[j + 1][3][1]
                                    - tmp1 * njac[j + 1][3][1];
                lhs[j][CC][4][1] =  tmp2 * fjac[j + 1][4][1]
                                    - tmp1 * njac[j + 1][4][1];

                lhs[j][CC][0][2] =  tmp2 * fjac[j + 1][0][2]
                                    - tmp1 * njac[j + 1][0][2];
                lhs[j][CC][1][2] =  tmp2 * fjac[j + 1][1][2]
                                    - tmp1 * njac[j + 1][1][2];
                lhs[j][CC][2][2] =  tmp2 * fjac[j + 1][2][2]
                                    - tmp1 * njac[j + 1][2][2]
                                    - tmp1 * dy3;
                lhs[j][CC][3][2] =  tmp2 * fjac[j + 1][3][2]
                                    - tmp1 * njac[j + 1][3][2];
                lhs[j][CC][4][2] =  tmp2 * fjac[j + 1][4][2]
                                    - tmp1 * njac[j + 1][4][2];

                lhs[j][CC][0][3] =  tmp2 * fjac[j + 1][0][3]
                                    - tmp1 * njac[j + 1][0][3];
                lhs[j][CC][1][3] =  tmp2 * fjac[j + 1][1][3]
                                    - tmp1 * njac[j + 1][1][3];
                lhs[j][CC][2][3] =  tmp2 * fjac[j + 1][2][3]
                                    - tmp1 * njac[j + 1][2][3];
                lhs[j][CC][3][3] =  tmp2 * fjac[j + 1][3][3]
                                    - tmp1 * njac[j + 1][3][3]
                                    - tmp1 * dy4;
                lhs[j][CC][4][3] =  tmp2 * fjac[j + 1][4][3]
                                    - tmp1 * njac[j + 1][4][3];

                lhs[j][CC][0][4] =  tmp2 * fjac[j + 1][0][4]
                                    - tmp1 * njac[j + 1][0][4];
                lhs[j][CC][1][4] =  tmp2 * fjac[j + 1][1][4]
                                    - tmp1 * njac[j + 1][1][4];
                lhs[j][CC][2][4] =  tmp2 * fjac[j + 1][2][4]
                                    - tmp1 * njac[j + 1][2][4];
                lhs[j][CC][3][4] =  tmp2 * fjac[j + 1][3][4]
                                    - tmp1 * njac[j + 1][3][4];
                lhs[j][CC][4][4] =  tmp2 * fjac[j + 1][4][4]
                                    - tmp1 * njac[j + 1][4][4]
                                    - tmp1 * dy5;
            }

            //---------------------------------------------------------------------
            //---------------------------------------------------------------------

            //---------------------------------------------------------------------
            // performs guaussian elimination on this cell.
            //
            // assumes that unpacking routines for non-first cells
            // preload C' and rhs' from previous cell.
            //
            // assumed send happens outside this routine, but that
            // c'(JMAX) and rhs'(JMAX) will be sent to next cell
            //---------------------------------------------------------------------

            //---------------------------------------------------------------------
            // multiply c[k][0][i] by b_inverse and copy back to c
            // multiply rhs(0) by b_inverse(0) and copy to rhs
            //---------------------------------------------------------------------
            binvcrhs( lhs[0][BB], lhs[0][CC], rhs[k][0][i] );

            //---------------------------------------------------------------------
            // begin inner most do loop
            // do all the elements of the cell unless last
            //---------------------------------------------------------------------
            for (j = 1; j <= jsize - 1; j++)
            {
                //-------------------------------------------------------------------
                // subtract A*lhs_vector(j-1) from lhs_vector(j)
                //
                // rhs(j) = rhs(j) - A*rhs(j-1)
                //-------------------------------------------------------------------
                matvec_sub(lhs[j][AA], rhs[k][j - 1][i], rhs[k][j][i]);

                //-------------------------------------------------------------------
                // B(j) = B(j) - C(j-1)*A(j)
                //-------------------------------------------------------------------
                matmul_sub(lhs[j][AA], lhs[j - 1][CC], lhs[j][BB]);

                //-------------------------------------------------------------------
                // multiply c[k][j][i] by b_inverse and copy back to c
                // multiply rhs[k][0][i] by b_inverse[k][0][i] and copy to rhs
                //-------------------------------------------------------------------
                binvcrhs( lhs[j][BB], lhs[j][CC], rhs[k][j][i] );
            }

            //---------------------------------------------------------------------
            // rhs(jsize) = rhs(jsize) - A*rhs(jsize-1)
            //---------------------------------------------------------------------
            matvec_sub(lhs[jsize][AA], rhs[k][jsize - 1][i], rhs[k][jsize][i]);

            //---------------------------------------------------------------------
            // B(jsize) = B(jsize) - C(jsize-1)*A(jsize)
            // matmul_sub(AA,i,jsize,k,c,
            // $              CC,i,jsize-1,k,c,BB,i,jsize,k)
            //---------------------------------------------------------------------
            matmul_sub(lhs[jsize][AA], lhs[jsize - 1][CC], lhs[jsize][BB]);

            //---------------------------------------------------------------------
            // multiply rhs(jsize) by b_inverse(jsize) and copy to rhs
            //---------------------------------------------------------------------
            binvrhs( lhs[jsize][BB], rhs[k][jsize][i] );

            //---------------------------------------------------------------------
            // back solve: if last cell, then generate U(jsize)=rhs(jsize)
            // else assume U(jsize) is loaded in un pack backsub_info
            // so just use it
            // after u(jstart) will be sent to next cell
            //---------------------------------------------------------------------
            for (j = jsize - 1; j >= 0; j--)
            {
                for (m = 0; m < BLOCK_SIZE; m++)
                {
                    for (n = 0; n < BLOCK_SIZE; n++)
                    {
                        rhs[k][j][i][m] = rhs[k][j][i][m]
                                          - lhs[j][CC][n][m] * rhs[k][j + 1][i][n];
                    }
                }
            }
        }
    }
    if (timeron) timer_stop(t_ysolve);
}

//---------------------------------------------------------------------
// Performs line solves in Z direction by first factoring
// the block-tridiagonal matrix into an upper triangular matrix,
// and then performing back substitution to solve for the unknow
// vectors of each line.
//
// Make sure we treat elements zero to cell_size in the direction
// of the sweep.
//---------------------------------------------------------------------
void z_solve()
{
    int i, j, k, m, n, ksize;

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    if (timeron) timer_start(t_zsolve);

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    //---------------------------------------------------------------------
    // This function computes the left hand side for the three z-factors
    //---------------------------------------------------------------------

    ksize = grid_points[2] - 1;

    //---------------------------------------------------------------------
    // Compute the indices for storing the block-diagonal matrix;
    // determine c (labeled f) and s jacobians
    //---------------------------------------------------------------------
    for (j = 1; j <= grid_points[1] - 2; j++)
    {
        for (i = 1; i <= grid_points[0] - 2; i++)
        {
            for (k = 0; k <= ksize; k++)
            {
                tmp1 = 1.0 / u[k][j][i][0];
                tmp2 = tmp1 * tmp1;
                tmp3 = tmp1 * tmp2;

                fjac[k][0][0] = 0.0;
                fjac[k][1][0] = 0.0;
                fjac[k][2][0] = 0.0;
                fjac[k][3][0] = 1.0;
                fjac[k][4][0] = 0.0;

                fjac[k][0][1] = - ( u[k][j][i][1] * u[k][j][i][3] ) * tmp2;
                fjac[k][1][1] = u[k][j][i][3] * tmp1;
                fjac[k][2][1] = 0.0;
                fjac[k][3][1] = u[k][j][i][1] * tmp1;
                fjac[k][4][1] = 0.0;

                fjac[k][0][2] = - ( u[k][j][i][2] * u[k][j][i][3] ) * tmp2;
                fjac[k][1][2] = 0.0;
                fjac[k][2][2] = u[k][j][i][3] * tmp1;
                fjac[k][3][2] = u[k][j][i][2] * tmp1;
                fjac[k][4][2] = 0.0;

                fjac[k][0][3] = - (u[k][j][i][3] * u[k][j][i][3] * tmp2 )
                                + c2 * qs[k][j][i];
                fjac[k][1][3] = - c2 *  u[k][j][i][1] * tmp1;
                fjac[k][2][3] = - c2 *  u[k][j][i][2] * tmp1;
                fjac[k][3][3] = ( 2.0 - c2 ) *  u[k][j][i][3] * tmp1;
                fjac[k][4][3] = c2;

                fjac[k][0][4] = ( c2 * 2.0 * square[k][j][i] - c1 * u[k][j][i][4] )
                                * u[k][j][i][3] * tmp2;
                fjac[k][1][4] = - c2 * ( u[k][j][i][1] * u[k][j][i][3] ) * tmp2;
                fjac[k][2][4] = - c2 * ( u[k][j][i][2] * u[k][j][i][3] ) * tmp2;
                fjac[k][3][4] = c1 * ( u[k][j][i][4] * tmp1 )
                                - c2 * ( qs[k][j][i] + u[k][j][i][3] * u[k][j][i][3] * tmp2 );
                fjac[k][4][4] = c1 * u[k][j][i][3] * tmp1;

                njac[k][0][0] = 0.0;
                njac[k][1][0] = 0.0;
                njac[k][2][0] = 0.0;
                njac[k][3][0] = 0.0;
                njac[k][4][0] = 0.0;

                njac[k][0][1] = - c3c4 * tmp2 * u[k][j][i][1];
                njac[k][1][1] =   c3c4 * tmp1;
                njac[k][2][1] =   0.0;
                njac[k][3][1] =   0.0;
                njac[k][4][1] =   0.0;

                njac[k][0][2] = - c3c4 * tmp2 * u[k][j][i][2];
                njac[k][1][2] =   0.0;
                njac[k][2][2] =   c3c4 * tmp1;
                njac[k][3][2] =   0.0;
                njac[k][4][2] =   0.0;

                njac[k][0][3] = - con43 * c3c4 * tmp2 * u[k][j][i][3];
                njac[k][1][3] =   0.0;
                njac[k][2][3] =   0.0;
                njac[k][3][3] =   con43 * c3 * c4 * tmp1;
                njac[k][4][3] =   0.0;

                njac[k][0][4] = - (  c3c4
                                     - c1345 ) * tmp3 * (u[k][j][i][1] * u[k][j][i][1])
                                - ( c3c4 - c1345 ) * tmp3 * (u[k][j][i][2] * u[k][j][i][2])
                                - ( con43 * c3c4
                                    - c1345 ) * tmp3 * (u[k][j][i][3] * u[k][j][i][3])
                                - c1345 * tmp2 * u[k][j][i][4];

                njac[k][1][4] = (  c3c4 - c1345 ) * tmp2 * u[k][j][i][1];
                njac[k][2][4] = (  c3c4 - c1345 ) * tmp2 * u[k][j][i][2];
                njac[k][3][4] = ( con43 * c3c4
                                  - c1345 ) * tmp2 * u[k][j][i][3];
                njac[k][4][4] = ( c1345 ) * tmp1;
            }

            //---------------------------------------------------------------------
            // now jacobians set, so form left hand side in z direction
            //---------------------------------------------------------------------
            lhsinit(lhs, ksize);
            for (k = 1; k <= ksize - 1; k++)
            {
                tmp1 = dt * tz1;
                tmp2 = dt * tz2;

                lhs[k][AA][0][0] = - tmp2 * fjac[k - 1][0][0]
                                   - tmp1 * njac[k - 1][0][0]
                                   - tmp1 * dz1;
                lhs[k][AA][1][0] = - tmp2 * fjac[k - 1][1][0]
                                   - tmp1 * njac[k - 1][1][0];
                lhs[k][AA][2][0] = - tmp2 * fjac[k - 1][2][0]
                                   - tmp1 * njac[k - 1][2][0];
                lhs[k][AA][3][0] = - tmp2 * fjac[k - 1][3][0]
                                   - tmp1 * njac[k - 1][3][0];
                lhs[k][AA][4][0] = - tmp2 * fjac[k - 1][4][0]
                                   - tmp1 * njac[k - 1][4][0];

                lhs[k][AA][0][1] = - tmp2 * fjac[k - 1][0][1]
                                   - tmp1 * njac[k - 1][0][1];
                lhs[k][AA][1][1] = - tmp2 * fjac[k - 1][1][1]
                                   - tmp1 * njac[k - 1][1][1]
                                   - tmp1 * dz2;
                lhs[k][AA][2][1] = - tmp2 * fjac[k - 1][2][1]
                                   - tmp1 * njac[k - 1][2][1];
                lhs[k][AA][3][1] = - tmp2 * fjac[k - 1][3][1]
                                   - tmp1 * njac[k - 1][3][1];
                lhs[k][AA][4][1] = - tmp2 * fjac[k - 1][4][1]
                                   - tmp1 * njac[k - 1][4][1];

                lhs[k][AA][0][2] = - tmp2 * fjac[k - 1][0][2]
                                   - tmp1 * njac[k - 1][0][2];
                lhs[k][AA][1][2] = - tmp2 * fjac[k - 1][1][2]
                                   - tmp1 * njac[k - 1][1][2];
                lhs[k][AA][2][2] = - tmp2 * fjac[k - 1][2][2]
                                   - tmp1 * njac[k - 1][2][2]
                                   - tmp1 * dz3;
                lhs[k][AA][3][2] = - tmp2 * fjac[k - 1][3][2]
                                   - tmp1 * njac[k - 1][3][2];
                lhs[k][AA][4][2] = - tmp2 * fjac[k - 1][4][2]
                                   - tmp1 * njac[k - 1][4][2];

                lhs[k][AA][0][3] = - tmp2 * fjac[k - 1][0][3]
                                   - tmp1 * njac[k - 1][0][3];
                lhs[k][AA][1][3] = - tmp2 * fjac[k - 1][1][3]
                                   - tmp1 * njac[k - 1][1][3];
                lhs[k][AA][2][3] = - tmp2 * fjac[k - 1][2][3]
                                   - tmp1 * njac[k - 1][2][3];
                lhs[k][AA][3][3] = - tmp2 * fjac[k - 1][3][3]
                                   - tmp1 * njac[k - 1][3][3]
                                   - tmp1 * dz4;
                lhs[k][AA][4][3] = - tmp2 * fjac[k - 1][4][3]
                                   - tmp1 * njac[k - 1][4][3];

                lhs[k][AA][0][4] = - tmp2 * fjac[k - 1][0][4]
                                   - tmp1 * njac[k - 1][0][4];
                lhs[k][AA][1][4] = - tmp2 * fjac[k - 1][1][4]
                                   - tmp1 * njac[k - 1][1][4];
                lhs[k][AA][2][4] = - tmp2 * fjac[k - 1][2][4]
                                   - tmp1 * njac[k - 1][2][4];
                lhs[k][AA][3][4] = - tmp2 * fjac[k - 1][3][4]
                                   - tmp1 * njac[k - 1][3][4];
                lhs[k][AA][4][4] = - tmp2 * fjac[k - 1][4][4]
                                   - tmp1 * njac[k - 1][4][4]
                                   - tmp1 * dz5;

                lhs[k][BB][0][0] = 1.0
                                   + tmp1 * 2.0 * njac[k][0][0]
                                   + tmp1 * 2.0 * dz1;
                lhs[k][BB][1][0] = tmp1 * 2.0 * njac[k][1][0];
                lhs[k][BB][2][0] = tmp1 * 2.0 * njac[k][2][0];
                lhs[k][BB][3][0] = tmp1 * 2.0 * njac[k][3][0];
                lhs[k][BB][4][0] = tmp1 * 2.0 * njac[k][4][0];

                lhs[k][BB][0][1] = tmp1 * 2.0 * njac[k][0][1];
                lhs[k][BB][1][1] = 1.0
                                   + tmp1 * 2.0 * njac[k][1][1]
                                   + tmp1 * 2.0 * dz2;
                lhs[k][BB][2][1] = tmp1 * 2.0 * njac[k][2][1];
                lhs[k][BB][3][1] = tmp1 * 2.0 * njac[k][3][1];
                lhs[k][BB][4][1] = tmp1 * 2.0 * njac[k][4][1];

                lhs[k][BB][0][2] = tmp1 * 2.0 * njac[k][0][2];
                lhs[k][BB][1][2] = tmp1 * 2.0 * njac[k][1][2];
                lhs[k][BB][2][2] = 1.0
                                   + tmp1 * 2.0 * njac[k][2][2]
                                   + tmp1 * 2.0 * dz3;
                lhs[k][BB][3][2] = tmp1 * 2.0 * njac[k][3][2];
                lhs[k][BB][4][2] = tmp1 * 2.0 * njac[k][4][2];

                lhs[k][BB][0][3] = tmp1 * 2.0 * njac[k][0][3];
                lhs[k][BB][1][3] = tmp1 * 2.0 * njac[k][1][3];
                lhs[k][BB][2][3] = tmp1 * 2.0 * njac[k][2][3];
                lhs[k][BB][3][3] = 1.0
                                   + tmp1 * 2.0 * njac[k][3][3]
                                   + tmp1 * 2.0 * dz4;
                lhs[k][BB][4][3] = tmp1 * 2.0 * njac[k][4][3];

                lhs[k][BB][0][4] = tmp1 * 2.0 * njac[k][0][4];
                lhs[k][BB][1][4] = tmp1 * 2.0 * njac[k][1][4];
                lhs[k][BB][2][4] = tmp1 * 2.0 * njac[k][2][4];
                lhs[k][BB][3][4] = tmp1 * 2.0 * njac[k][3][4];
                lhs[k][BB][4][4] = 1.0
                                   + tmp1 * 2.0 * njac[k][4][4]
                                   + tmp1 * 2.0 * dz5;

                lhs[k][CC][0][0] =  tmp2 * fjac[k + 1][0][0]
                                    - tmp1 * njac[k + 1][0][0]
                                    - tmp1 * dz1;
                lhs[k][CC][1][0] =  tmp2 * fjac[k + 1][1][0]
                                    - tmp1 * njac[k + 1][1][0];
                lhs[k][CC][2][0] =  tmp2 * fjac[k + 1][2][0]
                                    - tmp1 * njac[k + 1][2][0];
                lhs[k][CC][3][0] =  tmp2 * fjac[k + 1][3][0]
                                    - tmp1 * njac[k + 1][3][0];
                lhs[k][CC][4][0] =  tmp2 * fjac[k + 1][4][0]
                                    - tmp1 * njac[k + 1][4][0];

                lhs[k][CC][0][1] =  tmp2 * fjac[k + 1][0][1]
                                    - tmp1 * njac[k + 1][0][1];
                lhs[k][CC][1][1] =  tmp2 * fjac[k + 1][1][1]
                                    - tmp1 * njac[k + 1][1][1]
                                    - tmp1 * dz2;
                lhs[k][CC][2][1] =  tmp2 * fjac[k + 1][2][1]
                                    - tmp1 * njac[k + 1][2][1];
                lhs[k][CC][3][1] =  tmp2 * fjac[k + 1][3][1]
                                    - tmp1 * njac[k + 1][3][1];
                lhs[k][CC][4][1] =  tmp2 * fjac[k + 1][4][1]
                                    - tmp1 * njac[k + 1][4][1];

                lhs[k][CC][0][2] =  tmp2 * fjac[k + 1][0][2]
                                    - tmp1 * njac[k + 1][0][2];
                lhs[k][CC][1][2] =  tmp2 * fjac[k + 1][1][2]
                                    - tmp1 * njac[k + 1][1][2];
                lhs[k][CC][2][2] =  tmp2 * fjac[k + 1][2][2]
                                    - tmp1 * njac[k + 1][2][2]
                                    - tmp1 * dz3;
                lhs[k][CC][3][2] =  tmp2 * fjac[k + 1][3][2]
                                    - tmp1 * njac[k + 1][3][2];
                lhs[k][CC][4][2] =  tmp2 * fjac[k + 1][4][2]
                                    - tmp1 * njac[k + 1][4][2];

                lhs[k][CC][0][3] =  tmp2 * fjac[k + 1][0][3]
                                    - tmp1 * njac[k + 1][0][3];
                lhs[k][CC][1][3] =  tmp2 * fjac[k + 1][1][3]
                                    - tmp1 * njac[k + 1][1][3];
                lhs[k][CC][2][3] =  tmp2 * fjac[k + 1][2][3]
                                    - tmp1 * njac[k + 1][2][3];
                lhs[k][CC][3][3] =  tmp2 * fjac[k + 1][3][3]
                                    - tmp1 * njac[k + 1][3][3]
                                    - tmp1 * dz4;
                lhs[k][CC][4][3] =  tmp2 * fjac[k + 1][4][3]
                                    - tmp1 * njac[k + 1][4][3];

                lhs[k][CC][0][4] =  tmp2 * fjac[k + 1][0][4]
                                    - tmp1 * njac[k + 1][0][4];
                lhs[k][CC][1][4] =  tmp2 * fjac[k + 1][1][4]
                                    - tmp1 * njac[k + 1][1][4];
                lhs[k][CC][2][4] =  tmp2 * fjac[k + 1][2][4]
                                    - tmp1 * njac[k + 1][2][4];
                lhs[k][CC][3][4] =  tmp2 * fjac[k + 1][3][4]
                                    - tmp1 * njac[k + 1][3][4];
                lhs[k][CC][4][4] =  tmp2 * fjac[k + 1][4][4]
                                    - tmp1 * njac[k + 1][4][4]
                                    - tmp1 * dz5;
            }

            //---------------------------------------------------------------------
            //---------------------------------------------------------------------

            //---------------------------------------------------------------------
            // performs guaussian elimination on this cell.
            //
            // assumes that unpacking routines for non-first cells
            // preload C' and rhs' from previous cell.
            //
            // assumed send happens outside this routine, but that
            // c'(KMAX) and rhs'(KMAX) will be sent to next cell.
            //---------------------------------------------------------------------

            //---------------------------------------------------------------------
            // outer most do loops - sweeping in i direction
            //---------------------------------------------------------------------

            //---------------------------------------------------------------------
            // multiply c[0][j][i] by b_inverse and copy back to c
            // multiply rhs(0) by b_inverse(0) and copy to rhs
            //---------------------------------------------------------------------
            binvcrhs( lhs[0][BB], lhs[0][CC], rhs[0][j][i] );

            //---------------------------------------------------------------------
            // begin inner most do loop
            // do all the elements of the cell unless last
            //---------------------------------------------------------------------
            for (k = 1; k <= ksize - 1; k++)
            {
                //-------------------------------------------------------------------
                // subtract A*lhs_vector(k-1) from lhs_vector(k)
                //
                // rhs(k) = rhs(k) - A*rhs(k-1)
                //-------------------------------------------------------------------
                matvec_sub(lhs[k][AA], rhs[k - 1][j][i], rhs[k][j][i]);

                //-------------------------------------------------------------------
                // B(k) = B(k) - C(k-1)*A(k)
                // matmul_sub(AA,i,j,k,c,CC,i,j,k-1,c,BB,i,j,k)
                //-------------------------------------------------------------------
                matmul_sub(lhs[k][AA], lhs[k - 1][CC], lhs[k][BB]);

                //-------------------------------------------------------------------
                // multiply c[k][j][i] by b_inverse and copy back to c
                // multiply rhs[0][j][i] by b_inverse[0][j][i] and copy to rhs
                //-------------------------------------------------------------------
                binvcrhs( lhs[k][BB], lhs[k][CC], rhs[k][j][i] );
            }

            //---------------------------------------------------------------------
            // Now finish up special cases for last cell
            //---------------------------------------------------------------------

            //---------------------------------------------------------------------
            // rhs(ksize) = rhs(ksize) - A*rhs(ksize-1)
            //---------------------------------------------------------------------
            matvec_sub(lhs[ksize][AA], rhs[ksize - 1][j][i], rhs[ksize][j][i]);

            //---------------------------------------------------------------------
            // B(ksize) = B(ksize) - C(ksize-1)*A(ksize)
            // matmul_sub(AA,i,j,ksize,c,
            // $              CC,i,j,ksize-1,c,BB,i,j,ksize)
            //---------------------------------------------------------------------
            matmul_sub(lhs[ksize][AA], lhs[ksize - 1][CC], lhs[ksize][BB]);

            //---------------------------------------------------------------------
            // multiply rhs(ksize) by b_inverse(ksize) and copy to rhs
            //---------------------------------------------------------------------
            binvrhs( lhs[ksize][BB], rhs[ksize][j][i] );

            //---------------------------------------------------------------------
            //---------------------------------------------------------------------

            //---------------------------------------------------------------------
            // back solve: if last cell, then generate U(ksize)=rhs(ksize)
            // else assume U(ksize) is loaded in un pack backsub_info
            // so just use it
            // after u(kstart) will be sent to next cell
            //---------------------------------------------------------------------

            for (k = ksize - 1; k >= 0; k--)
            {
                for (m = 0; m < BLOCK_SIZE; m++)
                {
                    for (n = 0; n < BLOCK_SIZE; n++)
                    {
                        rhs[k][j][i][m] = rhs[k][j][i][m]
                                          - lhs[k][CC][n][m] * rhs[k + 1][j][i][n];
                    }
                }
            }
        }
    }
    if (timeron) timer_stop(t_zsolve);
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