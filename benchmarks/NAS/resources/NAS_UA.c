//---------------------------------------------------------------------
// program UA
//---------------------------------------------------------------------

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>
#include <math.h>


#if !defined(CLASS_W) && !defined(CLASS_S) && !defined(CLASS_A) && !defined(CLASS_B) && !defined(CLASS_C) && !defined(CLASS_D) && !defined(CLASS_E)
#   define CLASS_W
#endif

//----------
//  Class S:
//----------
#ifdef CLASS_S
#   define LELT           250
#   define LMOR           11600
#   define REFINE_MAX     4
#   define FRE_DEFAULT    5
#   define NITER_DEFAULT  50
#   define NMXH_DEFAULT   10
#   define CLASS_DEFAULT  'S'
#   define ALPHA_DEFAULT  0.040e0
#endif
//----------
//  Class W:
//----------
#ifdef CLASS_W
#   define LELT           700
#   define LMOR           26700
#   define REFINE_MAX     5
#   define FRE_DEFAULT    5
#   define NITER_DEFAULT  100
#   define NMXH_DEFAULT   10
#   define CLASS_DEFAULT  'W'
#   define ALPHA_DEFAULT  0.060e0
#endif
//----------
//  Class A:
//----------
#ifdef CLASS_A
#   define LELT           2400
#   define LMOR           92700
#   define REFINE_MAX     6
#   define FRE_DEFAULT    5
#   define NITER_DEFAULT  200
#   define NMXH_DEFAULT   10
#   define CLASS_DEFAULT  'A'
#   define ALPHA_DEFAULT  0.076e0
#endif
//----------
//  Class B:
//----------
#ifdef CLASS_B
#   define LELT           8800
#   define LMOR           334600
#   define REFINE_MAX     7
#   define FRE_DEFAULT    5
#   define NITER_DEFAULT  200
#   define NMXH_DEFAULT   10
#   define CLASS_DEFAULT  'B'
#   define ALPHA_DEFAULT  0.076e0
#endif
//----------
//  Class C:
//----------
#ifdef CLASS_C
#   define LELT           33500
#   define LMOR           1262100
#   define REFINE_MAX     8
#   define FRE_DEFAULT    5
#   define NITER_DEFAULT  200
#   define NMXH_DEFAULT   10
#   define CLASS_DEFAULT  'C'
#   define ALPHA_DEFAULT  0.067e0
#endif
//----------
//  Class D:
//----------
#ifdef CLASS_D
#   define LELT           515000
#   define LMOR           19500000
#   define REFINE_MAX     10
#   define FRE_DEFAULT    5
#   define NITER_DEFAULT  250
#   define NMXH_DEFAULT   10
#   define CLASS_DEFAULT  'D'
#   define ALPHA_DEFAULT  0.046e0
#endif


typedef struct
{
    double real;
    double imag;
} dcomplex;


#define min(x,y)    ((x) < (y) ? (x) : (y))
#define max(x,y)    ((x) > (y) ? (x) : (y))


// Array dimensions
#define LX1       5
#define LNJE      2
#define NSIDES    6
#define NXYZ      (LX1*LX1*LX1)

/* common /usrdati/ */
int fre, niter, nmxh;

/* common /usrdatr/ */
double alpha, dlmin, dtime;

/* common /dimn/ */
int nelt, ntot, nmor, nvertex;

/* common /bench1/ */
double x0, _y0, z0, time;

#define VELX    3.0
#define VELY    3.0
#define VELZ    3.0
#define VISC    0.005
#define X00     (3.0/7.0)
#define Y00     (2.0/7.0)
#define Z00     (2.0/7.0)

// double arrays associated with collocation points
/* common /colldp/ */
double ta1   [LELT][LX1][LX1][LX1];
double ta2   [LELT][LX1][LX1][LX1];
double trhs  [LELT][LX1][LX1][LX1];
double t     [LELT][LX1][LX1][LX1];
double tmult [LELT][LX1][LX1][LX1];
double dpcelm[LELT][LX1][LX1][LX1];
double pdiff [LELT][LX1][LX1][LX1];
double pdiffp[LELT][LX1][LX1][LX1];

// double arrays associated with mortar points
/* common /mortdp/ */
double umor   [LMOR];
double mormult[LMOR];
double tmort  [LMOR];
double tmmor  [LMOR];
double rmor   [LMOR];
double dpcmor [LMOR];
double pmorx  [LMOR];
double ppmor  [LMOR];

// integer arrays associated with element faces
/* common/facein/ */
int idmo    [LELT][NSIDES][LNJE][LNJE][LX1][LX1];
int idel    [LELT][NSIDES][LX1][LX1];
int sje     [LELT][NSIDES][2][2];
int sje_new [LELT][NSIDES][2][2];
int ijel    [LELT][NSIDES][2];
int ijel_new[LELT][NSIDES][2];
int cbc     [LELT][NSIDES]; /**/
int cbc_new [LELT][NSIDES]; /**/

// integer array associated with vertices
/* common /vin/ */
int vassign[LELT][8];
int emo    [8 * LELT][8][2];
int nemo   [8 * LELT];

// integer array associated with element edges
/* common /edgein/ */
int diagn[LELT][12][2];

// integer arrays associated with elements
/* common /eltin/ */
int tree        [LELT];
int treenew     [LELT];
int mt_to_id    [LELT];
int mt_to_id_old[LELT];
int id_to_mt    [LELT];
int newc        [LELT]; /**/
int newi        [LELT]; /**/
int newe        [LELT]; /**/
int ref_front_id[LELT]; /**/
int ich         [LELT]; /**/
int size_e      [LELT];
int front       [LELT];
int action      [LELT];

// int arrays associated with vertices
/* common /vlg/ */
int ifpcmor[8 * LELT];

// int arrays associated with edge
/* common /edgelg/ */
int eassign  [LELT][12];
int ncon_edge[LELT][12];
int if_1_edge[LELT][12];

// int arrays associated with elements
/* common /facelg/ */
int skip    [LELT];
int ifcoa   [LELT];
int ifcoa_id[LELT];

// int arrays associated with element faces
/* common /masonl/ */
int fassign[LELT][NSIDES];
int edgevis[LELT][NSIDES][4];

// small arrays
/* common /transr/ */
double qbnew[2][LX1][LX1 - 2];
double bqnew[2][LX1 - 2][LX1 - 2];

/* common /pcr/ */
double pcmor_nc1[REFINE_MAX][2][2][LX1][LX1];
double pcmor_nc2[REFINE_MAX][2][2][LX1][LX1];
double pcmor_nc0[REFINE_MAX][2][2][LX1][LX1];
double pcmor_c  [REFINE_MAX][LX1][LX1];
double tcpre    [LX1][LX1];
double pcmor_cor[REFINE_MAX][8];

// gauss-labotto and gauss points
/* common /gauss/ */
double zgm1[LX1];

// weights
/* common /wxyz/ */
double wxm1[LX1];
double w3m1[LX1][LX1][LX1];

// coordinate of element vertices
/* common /coord/ */
double xc[LELT][8];
double yc[LELT][8];
double zc[LELT][8];
double xc_new[LELT][8];
double yc_new[LELT][8];
double zc_new[LELT][8];

// dr/dx, dx/dr  and Jacobian
/* common /giso/ */
double jacm1_s[REFINE_MAX][LX1][LX1][LX1];
double rxm1_s[REFINE_MAX][LX1][LX1][LX1];
double xrm1_s[REFINE_MAX][LX1][LX1][LX1];

// mass matrices (diagonal)
/* common /mass/ */
double bm1_s[REFINE_MAX][LX1][LX1][LX1];

// dertivative matrices d/dr
/* common /dxyz/ */
double dxm1[LX1][LX1];
double dxtm1[LX1][LX1];
double wdtdr[LX1][LX1];

// interpolation operators
/* common /ixyz/ */
double ixm31 [LX1 * 2 - 1][LX1];
double ixtm31[LX1][LX1 * 2 - 1];
double ixmc1 [LX1][LX1];
double ixtmc1[LX1][LX1];
double ixmc2 [LX1][LX1];
double ixtmc2[LX1][LX1];
double map2  [LX1];
double map4  [LX1];

// collocation location within an element
/* common /xfracs/ */
double xfrac[LX1];

// used in laplacian operator
/* common /gmfact/ */
double g1m1_s[REFINE_MAX][LX1][LX1][LX1];
double g4m1_s[REFINE_MAX][LX1][LX1][LX1];
double g5m1_s[REFINE_MAX][LX1][LX1][LX1];
double g6m1_s[REFINE_MAX][LX1][LX1][LX1];

// We store some tables of useful topoint constants
// These constants are intialized in a block data 'top_constants'
/* common /top_consts/ */
int f_e_ef[6][4];
int e_c[8][3];
int local_corner[6][8];
int cal_nnb[8][3];
int oplc[4];
int cal_iijj[4][2];
int cal_intempx[6][4];
int c_f[6][4];
int le_arr[3][2][4];
int jjface[6];
int e_face2[6][4];
int op[4];
int localedgenumber[12][6];
int edgenumber[6][4];
int f_c[8][3];
int e1v1[6][6];
int e2v1[6][6];
int e1v2[6][6];
int e2v2[6][6];
int children[6][4];
int iijj[4][2];
int v_end[2];
int face_l1[3];
int face_l2[3];
int face_ld[3];

// Timer parameters
/* common /timing/ */
#define t_total       1
#define t_init        2
#define t_convect     3
#define t_transfb_c   4
#define t_diffusion   5
#define t_transf      6
#define t_transfb     7
#define t_adaptation  8
#define t_transf2     9
#define t_add2        10
#define t_last        10


#define btest(i,p)  (i & (1 << p))


void do_coarsen(int *if_coarsen, int *icoarsen, int neltold);
void do_refine(int *ifmortar, int *irefine);
int ifcor(int n1, int n2, int i, int iface);
int icheck(int ie, int n);
void find_coarsen(int *if_coarsen, int neltold);
void find_refine(int *if_refine);
void check_refine(int *ifrepeat);
int iftouch(int iel);
void remap(double y[LX1][LX1][LX1], double y1[7][LX1][LX1][LX1],
           double x[LX1][LX1][LX1]);
void merging(int iela[8]);
void remap2(int iela[8], int ielnew);
void remapz(double x1[LX1][LX1][LX1], double x2[LX1][LX1][LX1],
            double y[LX1][LX1][LX1]);
void remapy(double x1[LX1][LX1][LX1], double x2[LX1][LX1][LX1],
            double y[LX1][LX1][LX1]);
void remapx(double x1[LX1][LX1][LX1], double x2[LX1][LX1][LX1],
            double y[LX1][LX1][LX1]);

void convect(int ifmortar);
void diffusion(int ifmortar);
void laplacian(double r[LX1][LX1][LX1], double u[LX1][LX1][LX1], int sizei);
void adaptation(int *ifmortar, int step);
void move();
void mortar();
int ifsame(int iel, int i, int ntemp, int j);
void setuppc();
void setpcmo_pre();
void setpcmo();
void reciprocal(double a[], int n);
void r_init(double a[], int n, double _cnst);
void nr_init(int a[], int n, int _cnst);
void l_init(int a[], int n, int _cnst);
void ncopy(int a[], int b[], int n);
void copy(double a[], double b[], int n);
void adds2m1(double a[], double b[], double c1, int n);
void adds1m1(double a[], double b[], double c1, int n);
void col2(double a[], double b[], int n);
void nrzero(int na[], int n);
void add2(double a[], double b[], int n);
double calc_norm();
void parallel_add(int frontier[]);
void dssum();
void facev(double a[LX1][LX1][LX1], int iface, double val);
void transf(double tmor[], double tx[]);
void transfb(double tmor[], double tx[]);
void transfb_cor_e(int n, double *tmor, double tx[LX1][LX1][LX1]);
void transfb_cor_f(int n, double *tmor, double tx[LX1][LX1][LX1]);
void transf_nc(double tmor[LX1][LX1], double tx[LX1][LX1]);
void transfb_nc0(double tmor[LX1][LX1], double tx[LX1][LX1][LX1]);
void transfb_nc2(double tmor[LX1][LX1], double tx[LX1][LX1]);
void transfb_nc1(double tmor[LX1][LX1], double tx[LX1][LX1]);
void transfb_c(double tx[]);
void transfb_c_2(double tx[]);
void verify(char *Class, int *verified);
void create_initial_grid();
void coef();
void geom1();
void setdef();
void prepwork();
void top_constants();

void get_emo(int ie, int n, int ng);
void mor_assign(int mor_v[3], int *count);
void mor_edge(int ie, int face, int iel, int mor_v[3]);
void edgecopy_s(int face, int iel);
void mor_s_e(int n, int face, int iel, int mor_s_v[2][4]);
void mor_s_e_nn(int n, int face, int iel, int mor_s_v[4], int nn);
void mortar_vertex(int i, int iel, int count);
void mor_ne(int mor_v[3], int nn, int edge, int face,
            int edge2, int face2, int ntemp, int iel);

void pc_corner(int imor);
void com_dpc(int iside, int iel, int enumber, int n, int isize);


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
    int step, ie, iside, i, j, k;
    double mflops, tmax, nelt_tot = 0.0;
    char Class;
    int ifmortar = 0, verified;

    double t2, trecs[t_last + 1];
    char *t_names[t_last + 1];

    //---------------------------------------------------------------------
    // Read input file (if it exists), else take
    // defaults from parameters
    //---------------------------------------------------------------------
    FILE *fp;


    printf("\n\n NAS Parallel Benchmarks (NPB3.3-SER-C) - UA Benchmark\n\n");

    if ((fp = fopen("inputua.data", "r")) != NULL)
    {
        int result;
        printf(" Reading from input file inputua.data\n");
        result = fscanf(fp, "%d", &fre);
        while (fgetc(fp) != '\n');
        result = fscanf(fp, "%d", &niter);
        while (fgetc(fp) != '\n');
        result = fscanf(fp, "%d", &nmxh);
        while (fgetc(fp) != '\n');
        result = fscanf(fp, "%lf", &alpha);
        Class = 'U';
        fclose(fp);
    }
    else
    {
        printf(" No input file inputua.data. Using compiled defaults\n");
        fre   = FRE_DEFAULT;
        niter = NITER_DEFAULT;
        nmxh  = NMXH_DEFAULT;
        alpha = ALPHA_DEFAULT;
        Class = CLASS_DEFAULT;
    }

    dlmin = pow(0.5, REFINE_MAX);
    dtime = 0.04 * dlmin;

    printf(" Levels of refinement: %8d\n", REFINE_MAX);
    printf(" Adaptation frequency: %8d\n", fre);
    printf(" Time steps:           %8d    dt: %15.6E\n", niter, dtime);
    printf(" CG iterations:        %8d\n", nmxh);
    printf(" Heat source radius:   %8.4f\n\n", alpha);

    top_constants();

    for (i = 1; i <= t_last; i++)
    {
        timer_clear(i);
    }

    // set up initial mesh (single element) and solution (all zero)
    create_initial_grid();

    r_init((double *)ta1, ntot, 0.0);
    nr_init((int *)sje, 4 * 6 * nelt, -1);

    // compute tables of coefficients and weights
    coef();
    geom1();

    // compute the discrete laplacian operators
    setdef();

    // prepare for the preconditioner
    setpcmo_pre();

    // refine initial mesh and do some preliminary work
    time = 0.0;
    mortar();
    prepwork();
    adaptation(&ifmortar, 0);

    timer_clear(1);

    time = 0.0;
	
	#pragma kernel
    for (step = 0; step <= niter; step++)
    {
        if (step == 1)
        {
            // reset the solution and start the timer, keep track of total no elms
            r_init((double *)ta1, ntot, 0.0);

            time = 0.0;
            nelt_tot = 0.0;
            for (i = 1; i <= t_last; i++)
            {
                if (i != t_init) timer_clear(i);
            }
            timer_start(1);
        }

        // advance the convection step
        convect(ifmortar);

        // prepare the intital guess for cg
        transf(tmort, (double *)ta1);

        // compute residual for diffusion term based on intital guess

        // compute the left hand side of equation, lapacian t
        for (ie = 0; ie < nelt; ie++)
        {
            laplacian(ta2[ie], ta1[ie], size_e[ie]);
        }

        // compute the residual

        for (ie = 0; ie < nelt; ie++)
        {
            for (k = 0; k < LX1; k++)
            {
                for (j = 0; j < LX1; j++)
                {
                    for (i = 0; i < LX1; i++)
                    {
                        trhs[ie][k][j][i] = trhs[ie][k][j][i] - ta2[ie][k][j][i];
                    }
                }
            }
        }

        // get the residual on mortar
        transfb(rmor, (double *)trhs);

        // apply boundary condition: zero out the residual on domain boundaries

        // apply boundary conidtion to trhs

        for (ie = 0; ie < nelt; ie++)
        {
            for (iside = 0; iside < NSIDES; iside++)
            {
                if (cbc[ie][iside] == 0)
                {
                    facev(trhs[ie], iside, 0.0);
                }
            }
        }
        // apply boundary condition to rmor
        col2(rmor, tmmor, nmor);

        // call the conjugate gradient iterative solver
        diffusion(ifmortar);

        // add convection and diffusion
        add2((double *)ta1, (double *)t, ntot);

        // perform mesh adaptation
        time = time + dtime;
        if ((step != 0) && (step / fre * fre == step))
        {
            if (step != niter)
            {
                adaptation(&ifmortar, step);
            }
        }
        else
        {
            ifmortar = 0;
        }
        nelt_tot = nelt_tot + (double)(nelt);
    }

    timer_stop(1);
    tmax = timer_read(1);

    verify(&Class, &verified);

    // compute millions of collocation points advanced per second.
    // diffusion: nmxh advancements, convection: 1 advancement
    mflops = nelt_tot * (double)(LX1 * LX1 * LX1 * (nmxh + 1)) / (tmax * 1.e6);

    print_results("UA", Class, REFINE_MAX, 0, 0, niter,
                  tmax, mflops, "    coll. point advanced",
                  verified);

  int exitValue = verified ? 0 : 1;
  return exitValue;
}





//-----------------------------------------------------------
// For 3-D mesh adaptation (refinement+ coarsening)
//-----------------------------------------------------------
void adaptation(int *ifmortar, int step)
{
    int if_coarsen, if_refine, ifrepeat;
    int iel, miel, irefine, icoarsen, neltold;

    *ifmortar = 0;
    // compute heat source center(x0,y0,z0)
    x0 = X00 + VELX * time;
    _y0 = Y00 + VELY * time;
    z0 = Z00 + VELZ * time;

    // Search elements to be refined. Check with restrictions. Perform
    // refinement repeatedly until all desired refinements are done.

    // ich[iel]=0 no grid change on element iel
    // ich[iel]=2 iel is marked to be coarsened
    // ich[iel]=4 iel is marked to be refined

    // irefine records how many elements got refined
    irefine = 0;

    // check whether elements need to be refined because they have overlap
    // with the  heat source
    while (1)
    {
        find_refine(&if_refine);

        if (if_refine)
        {
            ifrepeat = 1;
            while (ifrepeat)
            {
                // Check with restriction, unmark elements that cannot be refined.
                //Elements preventing desired refinement will be marked to be refined.
                check_refine(&ifrepeat);
            }
            // perform refinement
            do_refine(ifmortar, &irefine);
        }
        else
        {
            break;
        }
    }

    // Search for elements to be coarsened. Check with restrictions,
    // Perform coarsening repeatedly until all possible coarsening
    // is done.

    // icoarsen records how many elements got coarsened
    icoarsen = 0;

    // skip[iel]=1 indicates an element no longer exists (because it
    // got merged)
    l_init(skip, nelt, 0);

    neltold = nelt;

    // Check whether elements need to be coarsened because they don't have
    // overlap with the heat source. Only elements that don't have a larger
    // size neighbor can be marked to be coarsened

    while (1)
    {
        find_coarsen(&if_coarsen, neltold);

        if (if_coarsen)
        {
            // Perform coarsening, however subject to restriction. Only possible
            // coarsening will be performed. if_coarsen=1 indicates that
            // actual coarsening happened
            do_coarsen(&if_coarsen, &icoarsen, neltold);
            if (if_coarsen)
            {
                // ifmortar=1 indicates the grid changed, i.e. the mortar points
                // indices need to be regenerated on the new grid.
                *ifmortar = 1;
            }
            else
            {
                break;
            }
        }
    }

    printf("Step %4d: elements refined, merged, total:%6d %6d %6d\n",
           step, irefine, icoarsen, nelt);

    // mt_to_id[miel] takes as argument the morton index  and returns the actual
    //                element index
    // id_to_mt(iel)  takes as argument the actual element index and returns the
    //                morton index
    for (miel = 0; miel < nelt; miel++)
    {
        iel = mt_to_id[miel];
        id_to_mt[iel] = miel;
    }

    // Reorder the elements in the order of the morton curve. After the move
    // subroutine the element indices are  the same as the morton indices
    move();

    // if the grid changed, regenerate mortar indices and update variables
    // associated to grid.
    if (*ifmortar)
    {
        mortar();
        prepwork();
    }
}


//---------------------------------------------------------------
// Coarsening procedure:
// 1) check with restrictions
// 2) perform coarsening
//---------------------------------------------------------------
void do_coarsen(int *if_coarsen, int *icoarsen, int neltold)
{
    int test, test1, test2, test3;
    int iel, ntp[8], ic, parent, mielnew, miel;
    int i, index, num_coarsen;

    *if_coarsen = 0;

    // If an element has been merged, it will be skipped afterwards
    // skip[iel]=1 for elements that will be skipped.
    // ifcoa_id[iel]=1 indicates that element iel will be coarsened
    // ifcoa[miel]=1 refers to element miel(mortar index) will be
    //                  coarsened

    ncopy(mt_to_id_old, mt_to_id, nelt);
    nr_init(mt_to_id, nelt, -1);
    l_init(ifcoa_id, neltold, 0);

    // Check whether the potential coarsening will make neighbor,
    // and neighbor's neighbor....break grid restriction
    for (miel = 0; miel < nelt; miel++)
    {
        ifcoa[miel] = 0;
        front[miel] = 0;
        iel = mt_to_id_old[miel];
        // if an element is marked to be coarsened
        if (ich[iel] == 2)
        {

            // If the current  element is the "first" child (front-left-
            // bottom) of its parent (tree[iel] mod 8 equals 0), then
            // find all its neighbors. Check whether they are from the same
            // parent.

            ic = tree[iel];
            if (!btest(ic, 0) && !btest(ic, 1) && !btest(ic, 2))
            {
                ntp[0] = iel;
                ntp[1] = sje[iel][0][0][0];
                ntp[2] = sje[iel][2][0][0];
                ntp[3] = sje[ntp[2]][0][0][0];
                ntp[4] = sje[iel][4][0][0];
                ntp[5] = sje[ntp[4]][0][0][0];
                ntp[6] = sje[ntp[4]][2][0][0];
                ntp[7] = sje[ntp[6]][0][0][0];

                parent = tree[iel] >> 3;
                test = 0;

                test1 = 1;
                for (i = 0; i < 8; i++)
                {
                    if ((tree[ntp[i]] >> 3) != parent) test1 = 0;
                }

                // check whether all child elements are marked to be coarsened
                if (test1)
                {
                    test2 = 1;
                    for (i = 0; i < 8; i++)
                    {
                        if (ich[ntp[i]] != 2) test2 = 0;
                    }

                    // check whether all child elements can be coarsened or not.
                    if (test2)
                    {
                        test3 = 1;
                        for (i = 0; i < 8; i++)
                        {
                            if (!icheck(ntp[i], i)) test3 = 0;
                        }
                        if (test3) test = 1;
                    }
                }
                // if the eight child elements are eligible to be coarsened
                // mark the first children ifcoa[miel]=1
                // mark them all ifcoa_id[]=1
                // front[miel] will be used to calculate (potentially in parallel)
                //             how many elements with seuqnece numbers less than
                //             miel will be coarsened.
                // skip[]      marks that an element will no longer exist after merge.

                if (test)
                {
                    ifcoa[miel] = 1;
                    for (i = 0; i < 8; i++)
                    {
                        ifcoa_id[ntp[i]] = 1;
                    }
                    front[miel] = 1;
                    for (i = 0; i < 7; i++)
                    {
                        skip[ntp[i + 1]] = 1;
                    }
                    *if_coarsen = 1;
                }
            }
        }
    }

    // compute front[iel], how many elements will be coarsened before iel
    // (including iel)
    parallel_add(front);

    // num_coarsen is the total number of elements that will be coarsened
    num_coarsen = front[nelt - 1];

    // action[i] records the morton index of the i'th element (if it is an
    // element's front-left-bottom-child) to be coarsened.

    // create array mt_to_id to convert actual element index to morton index
    for (miel = 0; miel < nelt; miel++)
    {
        iel = mt_to_id_old[miel];
        if (!skip[iel])
        {
            if (ifcoa[miel])
            {
                action[front[miel] - 1] = miel;
                mielnew = miel - (front[miel] - 1) * 7;
            }
            else
            {
                mielnew = miel - front[miel] * 7;
            }
            mt_to_id[mielnew] = iel;
        }
    }

    // perform the coarsening procedure (potentially in parallel)
    for (index = 0; index < num_coarsen; index++)
    {
        miel = action[index];
        iel = mt_to_id_old[miel];
        // find eight child elements to be coarsened
        ntp[0] = iel;
        ntp[1] = sje[iel][0][0][0];
        ntp[2] = sje[iel][2][0][0];
        ntp[3] = sje[ntp[2]][0][0][0];
        ntp[4] = sje[iel][4][0][0];
        ntp[5] = sje[ntp[4]][0][0][0];
        ntp[6] = sje[ntp[4]][2][0][0];
        ntp[7] = sje[ntp[6]][0][0][0];
        // merge them to be the parent
        merging(ntp);
    }

    nelt = nelt - num_coarsen * 7;
    *icoarsen = *icoarsen + num_coarsen * 8;
}


//-------------------------------------------------------
// Refinement procedure
//--------------------------------------------------------
void do_refine(int *ifmortar, int *irefine)
{
    double xctemp[8], yctemp[8], zctemp[8], xleft, xright;
    double yleft, yright, zleft, zright, ta1temp[LX1][LX1][LX1];
    double xhalf, yhalf, zhalf;
    int iel, i, j, jface;
    int ntemp, ndir, facedir, k, le[4], ne[4], mielnew;
    int miel, num_refine, index, treetemp;
    int ijeltemp[6][2], sjetemp[6][2][2], n1, n2, nelttemp;
    int cb, cbctemp[6];

    // initialize
    ncopy(mt_to_id_old, mt_to_id, nelt);
    nr_init(mt_to_id, nelt, -1);
    nr_init(action, nelt, -1);


    for (miel = 0; miel < nelt; miel++)
    {
        if (ich[mt_to_id_old[miel]] != 4)
        {
            front[miel] = 0;
        }
        else
        {
            front[miel] = 1;
        }
    }

    // front[iel] records how many elements with sequence numbers less than
    // or equal to iel will be refined
    parallel_add(front);

    // num_refine is the total number of elements that will be refined
    num_refine = front[nelt - 1];

    // action[i] records the morton index of the  i'th element to be refined
    for (miel = 0; miel < nelt; miel++)
    {
        iel = mt_to_id_old[miel];
        if (ich[iel] == 4)
        {
            action[front[miel] - 1] = miel;
        }
    }

    // Compute array mt_to_id to convert the element index to morton index.
    // ref_front_id[iel] records how many elements with index less than
    // iel (actual element index, not morton index), will be refined.
    for (miel = 0; miel < nelt; miel++)
    {
        iel = mt_to_id_old[miel];
        if (ich[iel] == 4)
        {
            ntemp = (front[miel] - 1) * 7;
            mielnew = miel + ntemp;
        }
        else
        {
            ntemp = front[miel] * 7;
            mielnew = miel + ntemp;
        }

        mt_to_id[mielnew] = iel;
        ref_front_id[iel] = nelt + ntemp;
    }


    // Perform refinement (potentially in parallel):
    // - Cut an element into eight children.
    // - Assign them element index  as iel, nelt+1,...., nelt+7.
    // - Update neighboring information.

    nelttemp = nelt;

    if (num_refine > 0)
    {
        *ifmortar = 1;
    }

    for (index = 0; index < num_refine; index++)
    {
        // miel is old morton index and mielnew is new morton index after refinement.
        miel = action[index];
        mielnew = miel + (front[miel] - 1) * 7;
        iel = mt_to_id_old[miel];
        nelt = nelttemp + (front[miel] - 1) * 7;
        // save iel's information in a temporary array
        treetemp = tree[iel];
        copy(xctemp, xc[iel], 8);
        copy(yctemp, yc[iel], 8);
        copy(zctemp, zc[iel], 8);
        ncopy(cbctemp, cbc[iel], 6);
        ncopy((int *)ijeltemp, ijel[iel][0], 12);
        ncopy((int *)sjetemp, sje[iel][0][0], 24);
        copy((double *)ta1temp, ta1[iel][0][0], NXYZ);

        // zero out iel here
        tree[iel] = 0;
        nr_init(cbc[iel], 6, 0);
        nr_init(sje[iel][0][0], 24, -1);
        nr_init(ijel[iel][0], 12, -1);
        r_init(ta1[iel][0][0], NXYZ, 0.0);

        // initialize new child elements:iel and nelt+1~nelt+7
        for (j = 0; j < 7; j++)
        {
            mt_to_id[mielnew + j + 1] = nelt + j;
            tree[nelt + j] = 0;
            nr_init(cbc[nelt + j], 6, 0);
            nr_init(sje[nelt + j][0][0], 24, -1);
            nr_init(ijel[nelt + j][0], 12, -1);
            r_init(ta1[nelt + j][0][0], NXYZ, 0.0);
        }

        // update the tree[]
        ntemp = treetemp << 3;
        tree[iel] = ntemp;
        for (i = 0; i < 7; i++)
        {
            tree[nelt + i] = ntemp + ((i + 1) % 8);
        }
        // update the children's vertices' coordinates
        xhalf  = xctemp[0] + (xctemp[1] - xctemp[0]) / 2.0;
        xleft  = xctemp[0];
        xright = xctemp[1];
        yhalf  = yctemp[0] + (yctemp[2] - yctemp[0]) / 2.0;
        yleft  = yctemp[0];
        yright = yctemp[2];
        zhalf  = zctemp[0] + (zctemp[4] - zctemp[0]) / 2.0;
        zleft  = zctemp[0];
        zright = zctemp[4];

        for (j = 0; j < 7; j += 2)
        {
            for (i = 0; i < 7; i += 2)
            {
                xc[nelt + j][i]   = xhalf;
                xc[nelt + j][i + 1] = xright;
            }
        }

        for (j = 1; j < 6; j += 2)
        {
            for (i = 0; i < 7; i += 2)
            {
                xc[nelt + j][i]   = xleft;
                xc[nelt + j][i + 1] = xhalf;
            }
        }

        for (i = 0; i < 7; i += 2)
        {
            xc[iel][i] = xleft;
            xc[iel][i + 1] = xhalf;
        }

        for (i = 0; i < 2; i++)
        {
            yc[nelt + 0][i] = yleft;
            yc[nelt + 3][i] = yleft;
            yc[nelt + 4][i] = yleft;
            yc[nelt + 0][i + 4] = yleft;
            yc[nelt + 3][i + 4] = yleft;
            yc[nelt + 4][i + 4] = yleft;
        }
        for (i = 2; i < 4; i++)
        {
            yc[nelt + 0][i] = yhalf;
            yc[nelt + 3][i] = yhalf;
            yc[nelt + 4][i] = yhalf;
            yc[nelt + 0][i + 4] = yhalf;
            yc[nelt + 3][i + 4] = yhalf;
            yc[nelt + 4][i + 4] = yhalf;
        }
        for (j = 1; j < 3; j++)
        {
            for (i = 0; i < 2; i++)
            {
                yc[nelt + j][i] = yhalf;
                yc[nelt + j + 4][i] = yhalf;
                yc[nelt + j][i + 4] = yhalf;
                yc[nelt + j + 4][i + 4] = yhalf;
            }
            for (i = 2; i < 4; i++)
            {
                yc[nelt + j][i] = yright;
                yc[nelt + j + 4][i] = yright;
                yc[nelt + j][i + 4] = yright;
                yc[nelt + j + 4][i + 4] = yright;
            }
        }

        for (i = 0; i < 2; i++)
        {
            yc[iel][i] = yleft;
            yc[iel][i + 4] = yleft;
        }
        for (i = 2; i < 4; i++)
        {
            yc[iel][i] = yhalf;
            yc[iel][i + 4] = yhalf;
        }

        for (j = 0; j < 3; j++)
        {
            for (i = 0; i < 4; i++)
            {
                zc[nelt + j][i] = zleft;
                zc[nelt + j][i + 4] = zhalf;
            }
        }
        for (j = 3; j < 7; j++)
        {
            for (i = 0; i < 4; i++)
            {
                zc[nelt + j][i] = zhalf;
                zc[nelt + j][i + 4] = zright;
            }
        }
        for (i = 0; i < 4; i++)
        {
            zc[iel][i] = zleft;
            zc[iel][i + 4] = zhalf;
        }

        // update the children's neighbor information

        // ndir refers to the x,y,z directions, respectively.
        // facedir refers to the orientation of the face in each direction,
        // e.g. ndir=0, facedir=0 refers to face 1,
        // and ndir =0, facedir=1 refers to face 2.

        for (ndir = 0; ndir < 3; ndir++)
        {
            for (facedir = 0; facedir <= 1; facedir++)
            {
                i = 2 * ndir + facedir;
                jface = jjface[i];
                cb = cbctemp[i];

                // find the new element indices of the four children on each
                // face of the parent element
                for (k = 0; k < 4; k++)
                {
                    le[k] = le_arr[ndir][facedir][k] + nelt;
                    ne[k] = le_arr[ndir][1 - facedir][k] + nelt;
                }
                if (facedir == 0)
                {
                    le[0] = iel;
                }
                else
                {
                    ne[0] = iel;
                }
                // update neighbor information of the four child elements on each
                // face of the parent element
                for (k = 0; k < 4; k++)
                {
                    cbc[le[k]][i] = 2;
                    sje[le[k]][i][0][0] = ne[k];
                    ijel[le[k]][i][0] = 0;
                    ijel[le[k]][i][1] = 0;
                }

                // if the face type of the parent element is type 2
                if (cb == 2 )
                {
                    ntemp = sjetemp[i][0][0];

                    // if the neighbor ntemp is not marked to be refined
                    if (ich[ntemp] != 4)
                    {
                        cbc[ntemp][jface] = 3;
                        ijel[ntemp][jface][0] = 0;
                        ijel[ntemp][jface][1] = 0;

                        for (k = 0; k < 4; k++)
                        {
                            cbc[ne[k]][i] = 1;
                            sje[ne[k]][i][0][0] = ntemp;
                            if (k == 0)
                            {
                                ijel[ne[k]][i][0] = 0;
                                ijel[ne[k]][i][1] = 0;
                                sje[ntemp][jface][0][0] = ne[k];
                            }
                            else if (k == 1)
                            {
                                ijel[ne[k]][i][0] = 0;
                                ijel[ne[k]][i][1] = 1;
                                sje[ntemp][jface][1][0] = ne[k];
                            }
                            else if (k == 2)
                            {
                                ijel[ne[k]][i][0] = 1;
                                ijel[ne[k]][i][1] = 0;
                                sje[ntemp][jface][0][1] = ne[k];
                            }
                            else if (k == 3)
                            {
                                ijel[ne[k]][i][0] = 1;
                                ijel[ne[k]][i][1] = 1;
                                sje[ntemp][jface][1][1] = ne[k];
                            }
                        }

                        // if the neighbor ntemp is also marked to be refined
                    }
                    else
                    {
                        n1 = ref_front_id[ntemp];

                        for (k = 0; k < 4; k++)
                        {
                            cbc[ne[k]][i] = 2;
                            n2 = n1 + le_arr[ndir][facedir][k];
                            if (n2 == n1 + 7) n2 = ntemp;
                            sje[ne[k]][i][0][0] = n2;
                            ijel[ne[k]][i][0] = 0;
                        }
                    }
                    // if the face type of the parent element is type 3
                }
                else if (cb == 3)
                {
                    for (k = 0; k < 4; k++)
                    {
                        cbc[ne[k]][i] = 2;
                        if (k == 0)
                        {
                            ntemp = sjetemp[i][0][0];
                        }
                        else if (k == 1)
                        {
                            ntemp = sjetemp[i][1][0];
                        }
                        else if (k == 2)
                        {
                            ntemp = sjetemp[i][0][1];
                        }
                        else if (k == 3)
                        {
                            ntemp = sjetemp[i][1][1];
                        }
                        ijel[ne[k]][i][0] = 0;
                        ijel[ne[k]][i][1] = 0;
                        sje[ne[k]][i][0][0] = ntemp;
                        cbc[ntemp][jface] = 2;
                        sje[ntemp][jface][0][0] = ne[k];
                        ijel[ntemp][jface][0] = 0;
                        ijel[ntemp][jface][1] = 0;
                    }

                    // if the face type of the parent element is type 0
                }
                else if (cb == 0)
                {
                    for (k = 0; k < 4; k++)
                    {
                        cbc[ne[k]][i] = cb;
                    }
                }
            }
        }

        // map solution from parent element to children
        remap(ta1[iel], &ta1[ref_front_id[iel]], ta1temp);
    }

    nelt = nelttemp + num_refine * 7;
    *irefine = *irefine + num_refine;
    ntot = nelt * LX1 * LX1 * LX1;
}


//-----------------------------------------------------------
// returns whether element n1's face i and element n2's
// jjface[iface] have intersections, i.e. whether n1 and
// n2 are neighbored by an edge.
//-----------------------------------------------------------
int ifcor(int n1, int n2, int i, int iface)
{
    int ret;

    ret = 0;

    if (ifsame(n1, e1v1[i][iface], n2, e2v1[i][iface]) ||
            ifsame(n1, e1v2[i][iface], n2, e2v2[i][iface]))
    {
        ret = 1;
    }

    return ret;
}


//-----------------------------------------------------------
// Check whether element ie's three faces (sharing vertex n)
// are nonconforming. This will prevent it from being coarsened.
// Also check ie's neighbors on those three faces, whether ie's
// neighbors by only an edge have a size smaller than ie's,
// which also prevents ie from being coarsened.
//-----------------------------------------------------------
int icheck(int ie, int n)
{
    int ntemp1, ntemp2, ntemp3, n1, n2, n3;
    int cb2_1, cb3_1, cb1_2, cb3_2, cb1_3, cb2_3;
    int ret;

    ret = 1;
    cb2_1 = 0;
    cb3_1 = 0;
    cb1_2 = 0;
    cb3_2 = 0;
    cb1_3 = 0;
    cb2_3 = 0;

    n1 = f_c[n][0];
    n2 = f_c[n][1];
    n3 = f_c[n][2];
    if ((cbc[ie][n1] == 3) || (cbc[ie][n2] == 3) || (cbc[ie][n3] == 3))
    {
        ret = 0;
    }
    else
    {
        ntemp1 = sje[ie][n1][0][0];
        ntemp2 = sje[ie][n2][0][0];
        ntemp3 = sje[ie][n3][0][0];
        if (ntemp1 != 0)
        {
            cb2_1 = cbc[ntemp1][n2];
            cb3_1 = cbc[ntemp1][n3];
        }
        if (ntemp2 != 0)
        {
            cb3_2 = cbc[ntemp2][n3];
            cb1_2 = cbc[ntemp2][n1];
        }
        if (ntemp3 != 0)
        {
            cb1_3 = cbc[ntemp3][n1];
            cb2_3 = cbc[ntemp3][n2];
        }
        if ((cbc[ie][n1] == 2 && (cb2_1 == 3 || cb3_1 == 3)) ||
                (cbc[ie][n2] == 2 && (cb3_2 == 3 || cb1_2 == 3)) ||
                (cbc[ie][n3] == 2 && (cb1_3 == 3 || cb2_3 == 3)))
        {
            ret = 0;
        }
    }

    return ret;
}


//-----------------------------------------------------------
// Search elements to be coarsened. Check with restrictions.
// This subroutine only checks the element itself, not its
// neighbors.
//-----------------------------------------------------------
void find_coarsen(int *if_coarsen, int neltold)
{
    int iftemp;
    int iel, i;

    *if_coarsen = 0;


    for (iel = 0; iel < neltold; iel++)
    {
        if (!skip[iel])
        {
            ich[iel] = 0;
            if (!iftouch(iel))
            {
                iftemp = 0;
                for (i = 0; i < NSIDES; i++)
                {
                    // if iel has a larger size than its face neighbors, it
                    // can not be coarsened
                    if (cbc[iel][i] == 3)
                    {
                        iftemp = 1;
                    }
                }
                if (!iftemp)
                {
                    *if_coarsen = 1;
                    ich[iel] = 2;
                }
            }
        }
    }
}


//-----------------------------------------------------------
// search elements to be refined based on whether they
// have overlap with the heat source
//-----------------------------------------------------------
void find_refine(int *if_refine)
{
    int iel;

    *if_refine = 0;


    for (iel = 0; iel < nelt; iel++)
    {
        ich[iel] = 0;
        if (iftouch(iel))
        {
            if ((xc[iel][1] - xc[iel][0]) > dlmin)
            {
                *if_refine = 1;
                ich[iel] = 4;
            }
        }
    }
}


//-----------------------------------------------------------------
// Check whether the potential refinement will violate the
// restriction. If so, mark the neighbor and unmark the
// original element, and set ifrepeat 1. i.e. this procedure
// needs to be repeated until no further check is needed
//-----------------------------------------------------------------
void check_refine(int *ifrepeat)
{
    int iel, iface, ntemp, nntemp, i, jface;

    *ifrepeat = 0;

    for (iel = 0; iel < nelt; iel++)
    {
        // if iel is marked to be refined
        if (ich[iel] == 4)
        {
            // check its six faces
            for (i = 0; i < NSIDES; i++)
            {
                jface = jjface[i];
                ntemp = sje[iel][i][0][0];
                // if one face neighbor is larger in size than iel
                if (cbc[iel][i] == 1)
                {
                    // unmark iel
                    ich[iel] = 0;
                    // the large size neighbor ntemp is marked to be refined
                    if (ich[ntemp] != 4)
                    {
                        *ifrepeat = 1;
                        ich[ntemp] = 4;
                    }
                    // check iel's neighbor, neighbored by an edge on face i, which
                    // must be a face neighbor of ntemp
                    for (iface = 0; iface < NSIDES; iface++)
                    {
                        if (iface != i && iface != jface)
                        {
                            //if edge neighbors are larger than iel, mark them to be refined
                            if (cbc[ntemp][iface] == 2)
                            {
                                nntemp = sje[ntemp][iface][0][0];
                                // ifcor is to make sure the edge neighbor exist
                                if (ich[nntemp] != 4 && ifcor(iel, nntemp, i, iface))
                                {
                                    ich[nntemp] = 4;
                                }
                            }
                        }
                    }
                    //if face neighbor are of the same size of iel, check edge neighbors
                }
                else if (cbc[iel][i] == 2)
                {
                    for (iface = 0; iface < NSIDES; iface++)
                    {
                        if (iface != i && iface != jface)
                        {
                            if (cbc[ntemp][iface] == 1)
                            {
                                nntemp = sje[ntemp][iface][0][0];
                                ich[nntemp] = 4;
                                ich[iel] = 0;
                                *ifrepeat = 1;
                            }
                        }
                    }
                }
            }
        }
    }
}


//-----------------------------------------------------------------
// check whether element iel has overlap with the heat source
//-----------------------------------------------------------------
int iftouch(int iel)
{
    double dis, dis1, dis2, dis3, alpha2;

    alpha2 = alpha * alpha;

    if (x0 < xc[iel][0])
    {
        dis1 = xc[iel][0] - x0;
    }
    else if (x0 > xc[iel][1])
    {
        dis1 = x0 - xc[iel][1];
    }
    else
    {
        dis1 = 0.0;
    }

    if (_y0 < yc[iel][0])
    {
        dis2 = yc[iel][0] - _y0;
    }
    else if (_y0 > yc[iel][2])
    {
        dis2 = _y0 - yc[iel][2];
    }
    else
    {
        dis2 = 0.0;
    }

    if (z0 < zc[iel][0])
    {
        dis3 = zc[iel][0] - z0;
    }
    else if (z0 > zc[iel][4])
    {
        dis3 = z0 - zc[iel][4];
    }
    else
    {
        dis3 = 0.0;
    }

    dis = dis1 * dis1 + dis2 * dis2 + dis3 * dis3;

    if (dis < alpha2)
    {
        return 1;
    }
    else
    {
        return 0;
    }
}


//-----------------------------------------------------------------
// After a refinement, map the solution  from the parent (x) to
// the eight children. y is the solution on the first child
// (front-bottom-left) and y1 is the solution on the next 7
// children.
//-----------------------------------------------------------------
void remap(double y[LX1][LX1][LX1], double y1[7][LX1][LX1][LX1],
           double x[LX1][LX1][LX1])
{
    double yone[2][LX1][LX1][LX1], ytwo[4][LX1][LX1][LX1];
    int i, iz, ii, jj, kk;

    r_init((double *)y, LX1 * LX1 * LX1, 0.0);
    r_init((double *)y1, LX1 * LX1 * LX1 * 7, 0.0);
    r_init((double *)yone, LX1 * LX1 * LX1 * 2, 0.0);
    r_init((double *)ytwo, LX1 * LX1 * LX1 * 4, 0.0);

    for (i = 0; i < LX1; i++)
    {
        for (kk = 0; kk < LX1; kk++)
        {
            for (jj = 0; jj < LX1; jj++)
            {
                for (ii = 0; ii < LX1; ii++)
                {
                    yone[0][i][jj][ii] = yone[0][i][jj][ii] + ixmc1[kk][ii] * x[i][jj][kk];
                    yone[1][i][jj][ii] = yone[1][i][jj][ii] + ixmc2[kk][ii] * x[i][jj][kk];
                }
            }
        }

        for (kk = 0; kk < LX1; kk++)
        {
            for (jj = 0; jj < LX1; jj++)
            {
                for (ii = 0; ii < LX1; ii++)
                {
                    ytwo[0][jj][i][ii] = ytwo[0][jj][i][ii] +
                                         yone[0][i][kk][ii] * ixtmc1[jj][kk];
                    ytwo[1][jj][i][ii] = ytwo[1][jj][i][ii] +
                                         yone[0][i][kk][ii] * ixtmc2[jj][kk];
                    ytwo[2][jj][i][ii] = ytwo[2][jj][i][ii] +
                                         yone[1][i][kk][ii] * ixtmc1[jj][kk];
                    ytwo[3][jj][i][ii] = ytwo[3][jj][i][ii] +
                                         yone[1][i][kk][ii] * ixtmc2[jj][kk];
                }
            }
        }
    }

    for (iz = 0; iz < LX1; iz++)
    {
        for (kk = 0; kk < LX1; kk++)
        {
            for (jj = 0; jj < LX1; jj++)
            {
                for (ii = 0; ii < LX1; ii++)
                {
                    y[jj][iz][ii] = y[jj][iz][ii] +
                                    ytwo[0][iz][kk][ii] * ixtmc1[jj][kk];
                    y1[0][jj][iz][ii] = y1[0][jj][iz][ii] +
                                        ytwo[2][iz][kk][ii] * ixtmc1[jj][kk];
                    y1[1][jj][iz][ii] = y1[1][jj][iz][ii] +
                                        ytwo[1][iz][kk][ii] * ixtmc1[jj][kk];
                    y1[2][jj][iz][ii] = y1[2][jj][iz][ii] +
                                        ytwo[3][iz][kk][ii] * ixtmc1[jj][kk];
                    y1[3][jj][iz][ii] = y1[3][jj][iz][ii] +
                                        ytwo[0][iz][kk][ii] * ixtmc2[jj][kk];
                    y1[4][jj][iz][ii] = y1[4][jj][iz][ii] +
                                        ytwo[2][iz][kk][ii] * ixtmc2[jj][kk];
                    y1[5][jj][iz][ii] = y1[5][jj][iz][ii] +
                                        ytwo[1][iz][kk][ii] * ixtmc2[jj][kk];
                    y1[6][jj][iz][ii] = y1[6][jj][iz][ii] +
                                        ytwo[3][iz][kk][ii] * ixtmc2[jj][kk];
                }
            }
        }
    }
}


//-----------------------------------------------------------------------
// This subroutine is to merge the eight child elements and map
// the solution from eight children to the  merged element.
// iela array records the eight elements to be merged.
//-----------------------------------------------------------------------
void merging(int iela[8])
{
    double x1, x2, y1, y2, z1, z2;
    int ielnew, i, ntemp, jface, ii, cb, ntempa[4], ielold, ntema[4];

    ielnew = iela[0];

    tree[ielnew] = tree[ielnew] >> 3;

    // element vertices
    x1 = xc[iela[0]][0];
    x2 = xc[iela[1]][1];
    y1 = yc[iela[0]][0];
    y2 = yc[iela[2]][2];
    z1 = zc[iela[0]][0];
    z2 = zc[iela[4]][4];

    for (i = 0; i < 7; i += 2)
    {
        xc[ielnew][i] = x1;
    }
    for (i = 1; i < 8; i += 2)
    {
        xc[ielnew][i] = x2;
    }
    for (i = 0; i < 2; i++)
    {
        yc[ielnew][i] = y1;
        yc[ielnew][i + 4] = y1;
    }
    for (i = 2; i < 4; i++)
    {
        yc[ielnew][i] = y2;
        yc[ielnew][i + 4] = y2;
    }
    for (i = 0; i < 4; i++)
    {
        zc[ielnew][i] = z1;
    }
    for (i = 4; i < 8; i++)
    {
        zc[ielnew][i] = z2;
    }

    // update neighboring information
    for (i = 0; i < NSIDES; i++)
    {
        jface = jjface[i];
        ielold = iela[children[i][0]];
        for (ii = 0; ii < 4; ii++)
        {
            ntempa[ii] = iela[children[i][ii]];
        }

        cb = cbc[ielold][i];

        if (cb == 2)
        {
            // if the neighbor elements also will be coarsened
            if (ifcoa_id[sje[ielold][i][0][0]])
            {
                if (i == 1 || i == 3 || i == 5)
                {
                    ntemp = sje[sje[ntempa[0]][i][0][0]][i][0][0];
                }
                else
                {
                    ntemp = sje[ntempa[0]][i][0][0];
                }
                sje[ielnew][i][0][0] = ntemp;
                ijel[ielnew][i][0] = 0;
                ijel[ielnew][i][1] = 0;
                cbc[ielnew][i] = 2;

                // if the neighbor elements will not be coarsened
            }
            else
            {
                for (ii = 0; ii < 4; ii++)
                {
                    ntema[ii] = sje[ntempa[ii]][i][0][0];
                    cbc[ntema[ii]][jface] = 1;
                    sje[ntema[ii]][jface][0][0] = ielnew;
                    ijel[ntema[ii]][jface][0] = iijj[ii][0];
                    ijel[ntema[ii]][jface][1] = iijj[ii][1];
                    sje[ielnew][i][iijj[ii][1]][iijj[ii][0]] = ntema[ii];
                    ijel[ielnew][i][0] = 0;
                    ijel[ielnew][i][1] = 0;
                }
                cbc[ielnew][i] = 3;
            }
        }
        else if (cb == 1)
        {

            ntemp = sje[ielold][i][0][0];
            cbc[ntemp][jface] = 2;
            ijel[ntemp][jface][0] = 0;
            ijel[ntemp][jface][1] = 0;
            sje[ntemp][jface][0][0] = ielnew;
            sje[ntemp][jface][1][0] = -1;
            sje[ntemp][jface][0][1] = -1;
            sje[ntemp][jface][1][1] = -1;

            cbc[ielnew][i] = 2;
            ijel[ielnew][i][0] = 0;
            ijel[ielnew][i][1] = 0;
            sje[ielnew][i][0][0] = ntemp;

        }
        else if (cb == 0)
        {
            cbc[ielnew][i] = 0;
            sje[ielnew][i][0][0] = -1;
            sje[ielnew][i][1][0] = -1;
            sje[ielnew][i][0][1] = -1;
            sje[ielnew][i][1][1] = -1;
        }
    }

    // map solution from children to the merged element
    remap2(iela, ielnew);
}


//-----------------------------------------------------------------
// Map the solution from the children to the parent.
// iela array records the eight elements to be merged.
// ielnew is the element index of the merged element.
//-----------------------------------------------------------------
void remap2(int iela[8], int ielnew)
{
    double temp1[LX1][LX1][LX1], temp2[LX1][LX1][LX1];
    double temp3[LX1][LX1][LX1], temp4[LX1][LX1][LX1];
    double temp5[LX1][LX1][LX1], temp6[LX1][LX1][LX1];

    remapx(ta1[iela[0]], ta1[iela[1]], temp1);
    remapx(ta1[iela[2]], ta1[iela[3]], temp2);
    remapx(ta1[iela[4]], ta1[iela[5]], temp3);
    remapx(ta1[iela[6]], ta1[iela[7]], temp4);
    remapy(temp1, temp2, temp5);
    remapy(temp3, temp4, temp6);
    remapz(temp5, temp6, ta1[ielnew]);
}


//-----------------------------------------------------------------
// z direction mapping after the merge.
// Map solution from x1 & x2 to y.
//-----------------------------------------------------------------
void remapz(double x1[LX1][LX1][LX1], double x2[LX1][LX1][LX1],
            double y[LX1][LX1][LX1])
{
    int ix, iy, ip;

    for (iy = 0; iy < LX1; iy++)
    {
        for (ix = 0; ix < LX1; ix++)
        {
            y[0][iy][ix] = x1[0][iy][ix];

            y[1][iy][ix] = 0.0;
            for (ip = 0; ip < LX1; ip++)
            {
                y[1][iy][ix] = y[1][iy][ix] + map2[ip] * x1[ip][iy][ix];
            }

            y[2][iy][ix] = x1[LX1 - 1][iy][ix];

            y[3][iy][ix] = 0.0;
            for (ip = 0; ip < LX1; ip++)
            {
                y[3][iy][ix] = y[3][iy][ix] + map4[ip] * x2[ip][iy][ix];
            }

            y[LX1 - 1][iy][ix] = x2[LX1 - 1][iy][ix];
        }
    }
}


//-----------------------------------------------------------------
// y direction mapping after the merge.
// Map solution from x1 & x2 to y.
//-----------------------------------------------------------------
void remapy(double x1[LX1][LX1][LX1], double x2[LX1][LX1][LX1],
            double y[LX1][LX1][LX1])
{
    int ix, iz, ip;

    for (iz = 0; iz < LX1; iz++)
    {
        for (ix = 0; ix < LX1; ix++)
        {
            y[iz][0][ix] = x1[iz][0][ix];

            y[iz][1][ix] = 0.0;
            for (ip = 0; ip < LX1; ip++)
            {
                y[iz][1][ix] = y[iz][1][ix] + map2[ip] * x1[iz][ip][ix];
            }

            y[iz][2][ix] = x1[iz][LX1 - 1][ix];

            y[iz][3][ix] = 0.0;
            for (ip = 0; ip < LX1; ip++)
            {
                y[iz][3][ix] = y[iz][3][ix] + map4[ip] * x2[iz][ip][ix];
            }

            y[iz][LX1 - 1][ix] = x2[iz][LX1 - 1][ix];
        }
    }
}


//-----------------------------------------------------------------
// x direction mapping after the merge.
// Map solution from x1 & x2 to y.
//-----------------------------------------------------------------
void remapx(double x1[LX1][LX1][LX1], double x2[LX1][LX1][LX1],
            double y[LX1][LX1][LX1])
{
    int iy, iz, ip;

    for (iz = 0; iz < LX1; iz++)
    {
        for (iy = 0; iy < LX1; iy++)
        {
            y[iz][iy][0] = x1[iz][iy][0];

            y[iz][iy][1] = 0.0;
            for (ip = 0; ip < LX1; ip++)
            {
                y[iz][iy][1] = y[iz][iy][1] + map2[ip] * x1[iz][iy][ip];
            }

            y[iz][iy][2] = x1[iz][iy][LX1 - 1];

            y[iz][iy][3] = 0.0;
            for (ip = 0; ip < LX1; ip++)
            {
                y[iz][iy][3] = y[iz][iy][3] + map4[ip] * x2[iz][iy][ip];
            }

            y[iz][iy][LX1 - 1] = x2[iz][iy][LX1 - 1];
        }
    }
}


//---------------------------------------------------------
// Advance the convection term using 4th order RK
// 1.ta1 is solution from last time step
// 2.the heat source is considered part of d/dx
// 3.trhs is right hand side for the diffusion equation
// 4.tmor is solution on mortar points, which will be used
//   as the initial guess when advancing the diffusion term
//---------------------------------------------------------
void convect(int ifmortar)
{
    double alpha2, tempa[LX1][LX1][LX1], rdtime, pidivalpha;
    double dtx1, dtx2, dtx3, src, rk1[LX1][LX1][LX1];
    double rk2[LX1][LX1][LX1], rk3[LX1][LX1][LX1], rk4[LX1][LX1][LX1];
    double temp[LX1][LX1][LX1], subtime[3], xx0[3], yy0[3], zz0[3];
    double dtime2, r2, sum, xloc[LX1], yloc[LX1], zloc[LX1];
    int k, iel, i, j, iside, isize, substep, ip;
    const double sixth = 1.0 / 6.0;

    pidivalpha = acos(-1.0) / alpha;
    alpha2     = alpha * alpha;
    dtime2     = dtime / 2.0;
    rdtime     = 1.0 / dtime;
    subtime[0] = time;
    subtime[1] = time + dtime2;
    subtime[2] = time + dtime;
    for (substep = 0; substep < 3; substep++)
    {
        xx0[substep] = X00 + VELX * subtime[substep];
        yy0[substep] = Y00 + VELY * subtime[substep];
        zz0[substep] = Z00 + VELZ * subtime[substep];
    }


    for (iel = 0; iel < nelt; iel++)
    {
        isize = size_e[iel];
        /*
        xloc[i] is the location of i'th collocation in x direction in an element.
        yloc[i] is the location of j'th collocation in y direction in an element.
        zloc[i] is the location of k'th collocation in z direction in an element.
        */
        for (i = 0; i < LX1; i++)
        {
            xloc[i] = xfrac[i] * (xc[iel][1] - xc[iel][0]) + xc[iel][0];
        }
        for (j = 0; j < LX1; j++)
        {
            yloc[j] = xfrac[j] * (yc[iel][3] - yc[iel][0]) + yc[iel][0];
        }
        for (k = 0; k < LX1; k++)
        {
            zloc[k] = xfrac[k] * (zc[iel][4] - zc[iel][0]) + zc[iel][0];
        }

        for (k = 0; k < LX1; k++)
        {
            for (j = 0; j < LX1; j++)
            {
                for (i = 0; i < LX1; i++)
                {
                    r2 = pow(xloc[i] - xx0[0], 2.0) + pow(yloc[j] - yy0[0], 2.0) +
                         pow(zloc[k] - zz0[0], 2.0);
                    if (r2 <= alpha2)
                    {
                        src = cos(sqrt(r2) * pidivalpha) + 1.0;
                    }
                    else
                    {
                        src = 0.0;
                    }
                    sum = 0.0;
                    for (ip = 0; ip < LX1; ip++)
                    {
                        sum = sum + dxm1[ip][i] * ta1[iel][k][j][ip];
                    }
                    dtx1 = -VELX * sum * xrm1_s[isize][k][j][i];
                    sum  = 0.0;
                    for (ip = 0; ip < LX1; ip++)
                    {
                        sum = sum + dxm1[ip][j] * ta1[iel][k][ip][i];
                    }
                    dtx2 = -VELY * sum * xrm1_s[isize][k][j][i];
                    sum = 0.0;
                    for (ip = 0; ip < LX1; ip++)
                    {
                        sum = sum + dxm1[ip][k] * ta1[iel][ip][j][i];
                    }
                    dtx3 = -VELZ * sum * xrm1_s[isize][k][j][i];

                    rk1[k][j][i] = dtx1 + dtx2 + dtx3 + src;
                    temp[k][j][i] = ta1[iel][k][j][i] + dtime2 * rk1[k][j][i];
                }
            }
        }

        for (k = 0; k < LX1; k++)
        {
            for (j = 0; j < LX1; j++)
            {
                for (i = 0; i < LX1; i++)
                {
                    r2 = pow(xloc[i] - xx0[1], 2.0) + pow(yloc[j] - yy0[1], 2.0) +
                         pow(zloc[k] - zz0[1], 2.0);
                    if (r2 <= alpha2)
                    {
                        src = cos(sqrt(r2) * pidivalpha) + 1.0;
                    }
                    else
                    {
                        src = 0.0;
                    }
                    sum = 0.0;
                    for (ip = 0; ip < LX1; ip++)
                    {
                        sum = sum + dxm1[ip][i] * temp[k][j][ip];
                    }
                    dtx1 = -VELX * sum * xrm1_s[isize][k][j][i];
                    sum = 0.0;
                    for (ip = 0; ip < LX1; ip++)
                    {
                        sum = sum + dxm1[ip][j] * temp[k][ip][i];
                    }
                    dtx2 = -VELY * sum * xrm1_s[isize][k][j][i];
                    sum = 0.0;
                    for (ip = 0; ip < LX1; ip++)
                    {
                        sum = sum + dxm1[ip][k] * temp[ip][j][i];
                    }
                    dtx3 = -VELZ * sum * xrm1_s[isize][k][j][i];

                    rk2[k][j][i] = dtx1 + dtx2 + dtx3 + src;
                    tempa[k][j][i] = ta1[iel][k][j][i] + dtime2 * rk2[k][j][i];
                }
            }
        }

        for (k = 0; k < LX1; k++)
        {
            for (j = 0; j < LX1; j++)
            {
                for (i = 0; i < LX1; i++)
                {
                    r2 = pow(xloc[i] - xx0[1], 2.0) + pow(yloc[j] - yy0[1], 2.0) +
                         pow(zloc[k] - zz0[1], 2.0);
                    if (r2 <= alpha2)
                    {
                        src = cos(sqrt(r2) * pidivalpha) + 1.0;
                    }
                    else
                    {
                        src = 0.0;
                    }
                    sum = 0.0;
                    for (ip = 0; ip < LX1; ip++)
                    {
                        sum = sum + dxm1[ip][i] * tempa[k][j][ip];
                    }
                    dtx1 = -VELX * sum * xrm1_s[isize][k][j][i];
                    sum = 0.0;
                    for (ip = 0; ip < LX1; ip++)
                    {
                        sum = sum + dxm1[ip][j] * tempa[k][ip][i];
                    }
                    dtx2 = -VELY * sum * xrm1_s[isize][k][j][i];
                    sum = 0.0;
                    for (ip = 0; ip < LX1; ip++)
                    {
                        sum = sum + dxm1[ip][k] * tempa[ip][j][i];
                    }
                    dtx3 = -VELZ * sum * xrm1_s[isize][k][j][i];

                    rk3[k][j][i] = dtx1 + dtx2 + dtx3 + src;
                    temp[k][j][i] = ta1[iel][k][j][i] + dtime * rk3[k][j][i];
                }
            }
        }

        for (k = 0; k < LX1; k++)
        {
            for (j = 0; j < LX1; j++)
            {
                for (i = 0; i < LX1; i++)
                {
                    r2 = pow(xloc[i] - xx0[2], 2.0) + pow(yloc[j] - yy0[2], 2.0) +
                         pow(zloc[k] - zz0[2], 2.0);
                    if (r2 <= alpha2)
                    {
                        src = cos(sqrt(r2) * pidivalpha) + 1.0;
                    }
                    else
                    {
                        src = 0.0;
                    }
                    sum = 0.0;
                    for (ip = 0; ip < LX1; ip++)
                    {
                        sum = sum + dxm1[ip][i] * temp[k][j][ip];
                    }
                    dtx1 = -VELX * sum * xrm1_s[isize][k][j][i];
                    sum = 0.0;
                    for (ip = 0; ip < LX1; ip++)
                    {
                        sum = sum + dxm1[ip][j] * temp[k][ip][i];
                    }
                    dtx2 = -VELY * sum * xrm1_s[isize][k][j][i];
                    sum = 0.0;
                    for (ip = 0; ip < LX1; ip++)
                    {
                        sum = sum + dxm1[ip][k] * temp[ip][j][i];
                    }
                    dtx3 = -VELZ * sum * xrm1_s[isize][k][j][i];

                    rk4[k][j][i] = dtx1 + dtx2 + dtx3 + src;
                    tempa[k][j][i] = sixth * (rk1[k][j][i] + 2.0 *
                                              rk2[k][j][i] + 2.0 * rk3[k][j][i] + rk4[k][j][i]);
                }
            }
        }

        // apply boundary condition
        for (iside = 0; iside < NSIDES; iside++)
        {
            if (cbc[iel][iside] == 0)
            {
                facev(tempa, iside, 0.0);
            }
        }

        for (k = 0; k < LX1; k++)
        {
            for (j = 0; j < LX1; j++)
            {
                for (i = 0; i < LX1; i++)
                {
                    trhs[iel][k][j][i] = bm1_s[isize][k][j][i] * (ta1[iel][k][j][i] * rdtime +
                                         tempa[k][j][i]);
                    ta1[iel][k][j][i] = ta1[iel][k][j][i] + tempa[k][j][i] * dtime;
                }
            }
        }
    }

    // get mortar for intial guess for CG
    if (ifmortar)
    {
        transfb_c_2((double *)ta1);
    }
    else
    {
        transfb_c((double *)ta1);
    }


    for (i = 0; i < nmor; i++)
    {
        tmort[i] = tmort[i] / mormult[i];
    }
}

//---------------------------------------------------------------------
// advance the diffusion term using CG iterations
//---------------------------------------------------------------------
void diffusion(int ifmortar)
{
    double rho_aux, rho1, rho2, beta, cona;
    int iter, ie, im, iside, i, j, k;

    // set up diagonal preconditioner
    if (ifmortar)
    {
        setuppc();
        setpcmo();
    }

    // arrays t and umor are accumlators of (am pm) in the CG algorithm
    // (see the specification)
    r_init((double *)t, ntot, 0.0);
    r_init((double *)umor, nmor, 0.0);

    // calculate initial am (see specification) in CG algorithm

    // trhs and rmor are combined to generate r0 in CG algorithm.
    // pdiff and pmorx are combined to generate q0 in the CG algorithm.
    // rho1 is  (qm,rm) in the CG algorithm.
    rho1 = 0.0;


    for (ie = 0; ie < nelt; ie++)
    {
        for (k = 0; k < LX1; k++)
        {
            for (j = 0; j < LX1; j++)
            {
                for (i = 0; i < LX1; i++)
                {
                    pdiff[ie][k][j][i] = dpcelm[ie][k][j][i] * trhs[ie][k][j][i];
                    rho1               = rho1 + trhs[ie][k][j][i] * pdiff[ie][k][j][i] *
                                         tmult[ie][k][j][i];
                }
            }
        }
    }


    for (im = 0; im < nmor; im++)
    {
        pmorx[im] = dpcmor[im] * rmor[im];
        rho1      = rho1 + rmor[im] * pmorx[im];
    }

    //.................................................................
    // commence conjugate gradient iteration
    //.................................................................
    for (iter = 1; iter <= nmxh; iter++)
    {
        if (iter > 1)
        {
            rho_aux = 0.0;
            // pdiffp and ppmor are combined to generate q_m+1 in the specification
            // rho_aux is (q_m+1,r_m+1)


            for (ie = 0; ie < nelt; ie++)
            {
                for (k = 0; k < LX1; k++)
                {
                    for (j = 0; j < LX1; j++)
                    {
                        for (i = 0; i < LX1; i++)
                        {
                            pdiffp[ie][k][j][i] = dpcelm[ie][k][j][i] * trhs[ie][k][j][i];
                            rho_aux = rho_aux + trhs[ie][k][j][i] * pdiffp[ie][k][j][i] *
                                      tmult[ie][k][j][i];
                        }
                    }
                }
            }


            for (im = 0; im < nmor; im++)
            {
                ppmor[im] = dpcmor[im] * rmor[im];
                rho_aux = rho_aux + rmor[im] * ppmor[im];
            }

            // compute bm (beta) in the specification
            rho2 = rho1;
            rho1 = rho_aux;
            beta = rho1 / rho2;

            // update p_m+1 in the specification
            adds1m1((double *)pdiff, (double *)pdiffp, beta, ntot);
            adds1m1(pmorx, ppmor, beta, nmor);
        }

        // compute matrix vector product: (theta pm) in the specification
        transf(pmorx, (double *)pdiff);

        // compute pdiffp which is (A theta pm) in the specification
        for (ie = 0; ie < nelt; ie++)
        {
            laplacian(pdiffp[ie], pdiff[ie], size_e[ie]);
        }

        // compute ppmor which will be used to compute (thetaT A theta pm)
        // in the specification
        transfb(ppmor, (double *)pdiffp);

        // apply boundary condition


        for (ie = 0; ie < nelt; ie++)
        {
            for (iside = 0; iside < NSIDES; iside++)
            {
                if (cbc[ie][iside] == 0)
                {
                    facev(pdiffp[ie], iside, 0.0);
                }
            }
        }

        // compute cona which is (pm,theta T A theta pm)
        cona = 0.0;


        for (ie = 0; ie < nelt; ie++)
        {
            for (k = 0; k < LX1; k++)
            {
                for (j = 0; j < LX1; j++)
                {
                    for (i = 0; i < LX1; i++)
                    {
                        cona = cona + pdiff[ie][k][j][i] *
                               pdiffp[ie][k][j][i] * tmult[ie][k][j][i];
                    }
                }
            }
        }


        for (im = 0; im < nmor; im++)
        {
            ppmor[im] = ppmor[im] * tmmor[im];
            cona = cona + pmorx[im] * ppmor[im];
        }

        // compute am
        cona = rho1 / cona;

        // compute (am pm)
        adds2m1((double *)t, (double *)pdiff, cona, ntot);
        adds2m1(umor, pmorx, cona, nmor);

        // compute r_m+1
        adds2m1((double *)trhs, (double *)pdiffp, -cona, ntot);
        adds2m1(rmor, ppmor,  -cona, nmor);
    }

    transf(umor, (double *)t);
}


//------------------------------------------------------------------
// compute  r = visc*[A]x +[B]x on a given element.
//------------------------------------------------------------------
void laplacian(double r[LX1][LX1][LX1], double u[LX1][LX1][LX1], int sizei)
{
    double rdtime;
    int i, j, k, iz;

    double tm1[LX1][LX1][LX1], tm2[LX1][LX1][LX1];

    rdtime = 1.0 / dtime;

    r_init((double *)tm1, NXYZ, 0.0);
    for (iz = 0; iz < LX1; iz++)
    {
        for (k = 0; k < LX1; k++)
        {
            for (j = 0; j < LX1; j++)
            {
                for (i = 0; i < LX1; i++)
                {
                    tm1[iz][j][i] = tm1[iz][j][i] + wdtdr[k][i] * u[iz][j][k];
                }
            }
        }
    }

    r_init((double *)tm2, NXYZ, 0.0);
    for (iz = 0; iz < LX1; iz++)
    {
        for (k = 0; k < LX1; k++)
        {
            for (j = 0; j < LX1; j++)
            {
                for (i = 0; i < LX1; i++)
                {
                    tm2[iz][j][i] = tm2[iz][j][i] + u[iz][k][i] * wdtdr[j][k];
                }
            }
        }
    }

    r_init((double *)r, NXYZ, 0.0);
    for (k = 0; k < LX1; k++)
    {
        for (iz = 0; iz < LX1; iz++)
        {
            for (j = 0; j < LX1; j++)
            {
                for (i = 0; i < LX1; i++)
                {
                    r[iz][j][i] = r[iz][j][i] + u[k][j][i] * wdtdr[iz][k];
                }
            }
        }
    }

    // collocate with remaining weights and sum to complete factorization.
    for (k = 0; k < LX1; k++)
    {
        for (j = 0; j < LX1; j++)
        {
            for (i = 0; i < LX1; i++)
            {
                r[k][j][i] = VISC * (tm1[k][j][i] * g4m1_s[sizei][k][j][i] +
                                     tm2[k][j][i] * g5m1_s[sizei][k][j][i] +
                                     r[k][j][i] * g6m1_s[sizei][k][j][i]) +
                             bm1_s[sizei][k][j][i] * rdtime * u[k][j][i];
            }
        }
    }
}

//-----------------------------------------------------------------
// generate mortar point index number
//-----------------------------------------------------------------
void mortar()
{
    int count, iel, jface, ntemp, i, ii, jj, ntemp1;
    int iii, jjj, face2, ne, ie, edge_g, ie2;
    int mor_v[3], cb, cb1, cb2, cb3, cb4, cb5, cb6;
    int space, sumcb, ij1, ij2, n1, n2, n3, n4, n5;

    n1 = LX1 * LX1 * 6 * 4 * nelt;
    nr_init((int *)idmo, n1, -1);

    n2 = 8 * nelt;
    nr_init(nemo, n2, -1);
    nr_init((int *)vassign, n2, -1);

    n3 = 2 * 64 * nelt;
    nr_init((int *)emo, n3, -1);

    n4 = 12 * nelt;
    l_init((int *)if_1_edge, n4, 0);

    n5 = 2 * 12 * nelt;
    nr_init((int *)diagn, n5, -1) ;

    // Mortar points indices are generated in two steps: first generate
    // them for all element vertices (corner points), then for conforming
    // edge and conforming face interiors. Each time a new mortar index
    // is generated for a mortar point, it is broadcast to all elements
    // sharing this mortar point.

    // VERTICES
    count = -1;

    // assign mortar point indices to element vertices


    for (iel = 0; iel < nelt; iel++)
    {

        // first calculate how many new mortar indices will be generated for
        // each element.

        // For each element, at least one vertex (vertex 7) will be new mortar
        // point. All possible new mortar points will be on face 1,3 or 5. By
        // checking the type of these three faces, we are able to tell
        // how many new mortar vertex points will be generated in each element.

        cb = cbc[iel][5];
        cb1 = cbc[iel][3];
        cb2 = cbc[iel][1];

        // For different combinations of the type of these three faces,
        // we group them into 27 configurations.
        // For different face types we assign the following integers:
        //        1 for type 2 or 3
        //        2 for type 0
        //        5 for type 1
        // By summing these integers for faces 1,3 and 5, sumcb will have
        // 10 different numbers indicating 10 different combinations.

        sumcb = 0;
        if (cb == 2 || cb == 3)
        {
            sumcb = sumcb + 1;
        }
        else if (cb == 0)
        {
            sumcb = sumcb + 2;
        }
        else if (cb == 1)
        {
            sumcb = sumcb + 5;
        }
        if (cb1 == 2 || cb1 == 3)
        {
            sumcb = sumcb + 1;
        }
        else if (cb1 == 0)
        {
            sumcb = sumcb + 2;
        }
        else if (cb1 == 1)
        {
            sumcb = sumcb + 5;
        }
        if (cb2 == 2 || cb2 == 3)
        {
            sumcb = sumcb + 1;
        }
        else if (cb2 == 0)
        {
            sumcb = sumcb + 2;
        }
        else if (cb2 == 1)
        {
            sumcb = sumcb + 5;
        }

        // compute newc[iel]
        // newc[iel] records how many new mortar indices will be generated
        //           for element iel
        // vassign[iel][i] records the element vertex of the i'th new mortar
        //           vertex point for element iel. e.g. vassign[iel][1]=8 means
        //           the 2nd new mortar vertex point generated on element
        //           iel is iel's 8th vertex.

        if (sumcb == 3)
        {
            // the three face types for face 1,3, and 5 are 2 2 2
            newc[iel] = 1;
            vassign[iel][0] = 7;

        }
        else if (sumcb == 4)
        {
            // the three face types for face 1,3 and 5 are 2 2 0 (not
            // necessarily in this order)
            newc[iel] = 2;
            if (cb == 0)
            {
                vassign[iel][0] = 3;
            }
            else if (cb1 == 0)
            {
                vassign[iel][0] = 5;
            }
            else if (cb2 == 0)
            {
                vassign[iel][0] = 6;
            }
            vassign[iel][1] = 7;

        }
        else if (sumcb == 7)
        {
            // the three face types for face 1,3 and 5 are 2 2 1 (not
            // necessarily in this order)
            if (cb == 1)
            {
                ij1 = ijel[iel][5][0];
                ij2 = ijel[iel][5][1];
                if (ij1 == 0 && ij2 == 0)
                {
                    newc[iel] = 2;
                    vassign[iel][0] = 3;
                    vassign[iel][1] = 7;
                }
                else if (ij1 == 0 && ij2 == 1)
                {
                    ntemp = sje[iel][5][0][0];
                    if (cbc[ntemp][0] == 3 && sje[ntemp][0][0][0] < iel)
                    {
                        newc[iel] = 1;
                        vassign[iel][0] = 7;
                    }
                    else
                    {
                        newc[iel] = 2;
                        vassign[iel][0] = 3;
                        vassign[iel][1] = 7;
                    }
                }
                else if (ij1 == 1 && ij2 == 0)
                {
                    ntemp = sje[iel][5][0][0];
                    if (cbc[ntemp][2] == 3 && sje[ntemp][2][0][0] < iel)
                    {
                        newc[iel] = 1;
                        vassign[iel][0] = 7;
                    }
                    else
                    {
                        newc[iel] = 2;
                        vassign[iel][0] = 3;
                        vassign[iel][1] = 7;
                    }
                }
                else
                {
                    newc[iel] = 1;
                    vassign[iel][0] = 7;
                }
            }
            else if (cb1 == 1)
            {
                ij1 = ijel[iel][3][0];
                ij2 = ijel[iel][3][1];
                if (ij1 == 0 && ij2 == 0)
                {
                    newc[iel] = 2;
                    vassign[iel][0] = 5;
                    vassign[iel][1] = 7;
                }
                else if (ij1 == 0 && ij2 == 1)
                {
                    ntemp = sje[iel][3][0][0];
                    if (cbc[ntemp][0] == 3 && sje[ntemp][0][0][0] < iel)
                    {
                        newc[iel] = 1;
                        vassign[iel][0] = 7;
                    }
                    else
                    {
                        newc[iel] = 2;
                        vassign[iel][0] = 5;
                        vassign[iel][1] = 7;
                    }
                }
                else if (ij1 == 1 && ij2 == 0)
                {
                    ntemp = sje[iel][3][0][0];
                    if (cbc[ntemp][4] == 3 && sje[ntemp][4][0][0] < iel)
                    {
                        newc[iel] = 1;
                        vassign[iel][0] = 7;
                    }
                    else
                    {
                        newc[iel] = 2;
                        vassign[iel][0] = 5;
                        vassign[iel][1] = 7;
                    }
                }
                else
                {
                    newc[iel] = 1;
                    vassign[iel][0] = 7;
                }

            }
            else if (cb2 == 1)
            {
                ij1 = ijel[iel][1][0];
                ij2 = ijel[iel][1][1];
                if (ij1 == 0 && ij2 == 0)
                {
                    newc[iel] = 2;
                    vassign[iel][0] = 6;
                    vassign[iel][1] = 7;
                }
                else if (ij1 == 0 && ij2 == 1)
                {
                    ntemp = sje[iel][1][0][0];
                    if (cbc[ntemp][2] == 3 && sje[ntemp][2][0][0] < iel)
                    {
                        newc[iel] = 1;
                        vassign[iel][0] = 7;
                    }
                    else
                    {
                        newc[iel] = 2;
                        vassign[iel][0] = 6;
                        vassign[iel][1] = 7;
                    }

                }
                else if (ij1 == 1 && ij2 == 0)
                {
                    ntemp = sje[iel][1][0][0];
                    if (cbc[ntemp][4] == 3 && sje[ntemp][4][0][0] < iel)
                    {
                        newc[iel] = 1;
                        vassign[iel][0] = 7;
                    }
                    else
                    {
                        newc[iel] = 2;
                        vassign[iel][0] = 6;
                        vassign[iel][1] = 7;
                    }
                }
                else
                {
                    newc[iel] = 1;
                    vassign[iel][0] = 7;
                }
            }

        }
        else if (sumcb == 5)
        {
            // the three face types for face 1,3 and 5 are 2/3 0 0 (not
            // necessarily in this order)
            newc[iel] = 4;
            if (cb == 2 || cb == 3)
            {
                vassign[iel][0] = 4;
                vassign[iel][1] = 5;
                vassign[iel][2] = 6;
                vassign[iel][3] = 7;
            }
            else if (cb1 == 2 || cb1 == 3)
            {
                vassign[iel][0] = 2;
                vassign[iel][1] = 3;
                vassign[iel][2] = 6;
                vassign[iel][3] = 7;
            }
            else if (cb2 == 2 || cb2 == 3)
            {
                vassign[iel][0] = 1;
                vassign[iel][1] = 3;
                vassign[iel][2] = 5;
                vassign[iel][3] = 7;
            }

        }
        else if (sumcb == 8)
        {
            // the three face types for face 1,3 and 5 are 2 0 1 (not
            // necessarily in this order)

            // if face 2 of type 1
            if (cb == 1)
            {
                if (cb1 == 2 || cb1 == 3)
                {
                    ij1 = ijel[iel][5][0];
                    if (ij1 == 0)
                    {
                        newc[iel] = 4;
                        vassign[iel][0] = 2;
                        vassign[iel][1] = 3;
                        vassign[iel][2] = 6;
                        vassign[iel][3] = 7;
                    }
                    else
                    {
                        ntemp = sje[iel][5][0][0];
                        if (cbc[ntemp][2] == 3 && sje[ntemp][2][0][0] < iel)
                        {
                            newc[iel] = 2;
                            vassign[iel][0] = 6;
                            vassign[iel][1] = 7;
                        }
                        else
                        {
                            newc[iel] = 3;
                            vassign[iel][0] = 3;
                            vassign[iel][1] = 6;
                            vassign[iel][2] = 7;
                        }
                    }

                }
                else if (cb2 == 2 || cb2 == 3)
                {
                    if (ijel[iel][5][1] == 0)
                    {
                        newc[iel] = 4;
                        vassign[iel][0] = 1;
                        vassign[iel][1] = 3;
                        vassign[iel][2] = 5;
                        vassign[iel][3] = 7;
                    }
                    else
                    {
                        ntemp = sje[iel][5][0][0];
                        if (cbc[ntemp][0] == 3 && sje[ntemp][0][0][0] < iel)
                        {
                            newc[iel] = 2;
                            vassign[iel][0] = 5;
                            vassign[iel][1] = 7;
                        }
                        else
                        {
                            newc[iel] = 3;
                            vassign[iel][0] = 3;
                            vassign[iel][1] = 5;
                            vassign[iel][2] = 7;
                        }
                    }
                }

                // if face 4 of type 1
            }
            else if (cb1 == 1)
            {
                if (cb == 2 || cb == 3)
                {
                    ij1 = ijel[iel][3][0];
                    ij2 = ijel[iel][3][1];

                    if (ij1 == 0 && ij2 == 0)
                    {
                        ntemp = sje[iel][3][0][0];
                        if (cbc[ntemp][1] == 3 && sje[ntemp][1][0][0] < iel)
                        {
                            newc[iel] = 3;
                            vassign[iel][0] = 5;
                            vassign[iel][1] = 6;
                            vassign[iel][2] = 7;
                        }
                        else
                        {
                            newc[iel] = 4;
                            vassign[iel][0] = 4;
                            vassign[iel][1] = 5;
                            vassign[iel][2] = 6;
                            vassign[iel][3] = 7;
                        }
                    }
                    else if (ij1 == 0 && ij2 == 1)
                    {
                        ntemp = sje[iel][3][0][0];
                        if (cbc[ntemp][0] == 3 && sje[ntemp][0][0][0] < iel)
                        {
                            newc[iel] = 3;
                            vassign[iel][0] = 4;
                            vassign[iel][1] = 6;
                            vassign[iel][2] = 7;
                        }
                        else
                        {
                            newc[iel] = 4;
                            vassign[iel][0] = 4;
                            vassign[iel][1] = 5;
                            vassign[iel][2] = 6;
                            vassign[iel][3] = 7;
                        }
                    }
                    else if (ij1 == 1 && ij2 == 0)
                    {
                        ntemp = sje[iel][3][0][0];
                        if (cbc[ntemp][4] == 3 && sje[ntemp][4][0][0] < iel)
                        {
                            newc[iel] = 2;
                            vassign[iel][0] = 6;
                            vassign[iel][1] = 7;
                        }
                        else
                        {
                            newc[iel] = 3;
                            vassign[iel][0] = 5;
                            vassign[iel][1] = 6;
                            vassign[iel][2] = 7;
                        }
                    }
                    else if (ij1 == 1 && ij2 == 1)
                    {
                        ntemp = sje[iel][3][0][0];
                        if (cbc[ntemp][4] == 3 && sje[ntemp][4][0][0] < iel)
                        {
                            newc[iel] = 2;
                            vassign[iel][0] = 6;
                            vassign[iel][1] = 7;
                        }
                        else
                        {
                            newc[iel] = 3;
                            vassign[iel][0] = 4;
                            vassign[iel][1] = 6;
                            vassign[iel][2] = 7;
                        }
                    }
                }
                else
                {
                    if (ijel[iel][3][1] == 0)
                    {
                        newc[iel] = 4;
                        vassign[iel][0] = 1;
                        vassign[iel][1] = 3;
                        vassign[iel][2] = 5;
                        vassign[iel][3] = 7;
                    }
                    else
                    {
                        ntemp = sje[iel][3][0][0];
                        if (cbc[ntemp][0] == 3 && sje[ntemp][0][0][0] < iel)
                        {
                            newc[iel] = 2;
                            vassign[iel][0] = 3;
                            vassign[iel][1] = 7;
                        }
                        else
                        {
                            newc[iel] = 3;
                            vassign[iel][0] = 3;
                            vassign[iel][1] = 5;
                            vassign[iel][2] = 7;
                        }
                    }
                }
                // if face 6 of type 1
            }
            else if (cb2 == 1)
            {
                if (cb == 2 || cb == 3)
                {
                    if (ijel[iel][1][0] == 0)
                    {
                        newc[iel] = 4;
                        vassign[iel][0] = 4;
                        vassign[iel][1] = 5;
                        vassign[iel][2] = 6;
                        vassign[iel][3] = 7;
                    }
                    else
                    {
                        ntemp = sje[iel][1][0][0];
                        if (cbc[ntemp][4] == 3 && sje[ntemp][4][0][0] < iel)
                        {
                            newc[iel] = 2;
                            vassign[iel][0] = 5;
                            vassign[iel][1] = 7;
                        }
                        else
                        {
                            newc[iel] = 3;
                            vassign[iel][0] = 5;
                            vassign[iel][1] = 6;
                            vassign[iel][2] = 7;
                        }
                    }
                }
                else
                {
                    if (ijel[iel][1][1] == 0)
                    {
                        newc[iel] = 4;
                        vassign[iel][0] = 2;
                        vassign[iel][1] = 3;
                        vassign[iel][2] = 6;
                        vassign[iel][3] = 7;
                    }
                    else
                    {
                        ntemp = sje[iel][1][0][0];
                        if (cbc[ntemp][2] == 3 && sje[ntemp][2][0][0] < iel)
                        {
                            newc[iel] = 2;
                            vassign[iel][0] = 3;
                            vassign[iel][1] = 7;
                        }
                        else
                        {
                            newc[iel] = 3;
                            vassign[iel][0] = 3;
                            vassign[iel][1] = 6;
                            vassign[iel][2] = 7;
                        }
                    }
                }
            }

        }
        else if (sumcb == 11)
        {
            // the three face type for face 2,4 and 6 are 2 1 1(not
            // necessarily in this order)
            if (cb == 2 || cb == 3)
            {
                if (ijel[iel][3][0] == 0)
                {
                    ntemp = sje[iel][3][0][0];
                    if (cbc[ntemp][1] == 3 && sje[ntemp][1][0][0] < iel)
                    {
                        newc[iel] = 3;
                        vassign[iel][0] = 5;
                        vassign[iel][1] = 6;
                        vassign[iel][2] = 7;
                    }
                    else
                    {
                        newc[iel] = 4;
                        vassign[iel][0] = 4;
                        vassign[iel][1] = 5;
                        vassign[iel][2] = 6;
                        vassign[iel][3] = 7;
                    }

                    // if ijel[iel][3][0]=1
                }
                else
                {
                    ntemp = sje[iel][1][0][0];
                    if (cbc[ntemp][4] == 3 && sje[ntemp][4][0][0] < iel)
                    {
                        ntemp1 = sje[iel][3][0][0];
                        if (cbc[ntemp1][4] == 3 && sje[ntemp1][4][0][0] < iel)
                        {
                            newc[iel] = 1;
                            vassign[iel][0] = 7;
                        }
                        else
                        {
                            newc[iel] = 2;
                            vassign[iel][0] = 5;
                            vassign[iel][1] = 7;
                        }
                    }
                    else
                    {
                        ntemp1 = sje[iel][3][0][0];
                        if (cbc[ntemp1][4] == 3 && sje[ntemp1][4][0][0] < iel)
                        {
                            newc[iel] = 2;
                            vassign[iel][0] = 6;
                            vassign[iel][1] = 7;
                        }
                        else
                        {
                            newc[iel] = 3;
                            vassign[iel][0] = 5;
                            vassign[iel][1] = 6;
                            vassign[iel][2] = 7;
                        }
                    }
                }
            }
            else if (cb1 == 2 || cb1 == 3)
            {
                if (ijel[iel][1][1] == 0)
                {
                    ntemp = sje[iel][1][0][0];
                    if (cbc[ntemp][5] == 3 && sje[ntemp][5][0][0] < iel)
                    {
                        newc[iel] = 3;
                        vassign[iel][0] = 3;
                        vassign[iel][1] = 6;
                        vassign[iel][2] = 7;
                    }
                    else
                    {
                        newc[iel] = 4;
                        vassign[iel][0] = 2;
                        vassign[iel][1] = 3;
                        vassign[iel][2] = 6;
                        vassign[iel][3] = 7;
                    }
                    // if ijel[iel][1][1]=1
                }
                else
                {
                    ntemp = sje[iel][1][0][0];
                    if (cbc[ntemp][2] == 3 && sje[ntemp][2][0][0] < iel)
                    {
                        ntemp1 = sje[iel][5][0][0];
                        if (cbc[ntemp1][2] == 3 && sje[ntemp1][2][0][0] < iel)
                        {
                            newc[iel] = 1;
                            vassign[iel][0] = 7;
                        }
                        else
                        {
                            newc[iel] = 2;
                            vassign[iel][0] = 3;
                            vassign[iel][1] = 7;
                        }
                    }
                    else
                    {
                        ntemp1 = sje[iel][5][0][0];
                        if (cbc[ntemp1][2] == 3 && sje[ntemp1][2][0][0] < iel)
                        {
                            newc[iel] = 2;
                            vassign[iel][0] = 6;
                            vassign[iel][1] = 7;
                        }
                        else
                        {
                            newc[iel] = 3;
                            vassign[iel][0] = 3;
                            vassign[iel][1] = 6;
                            vassign[iel][2] = 7;
                        }
                    }
                }
            }
            else if (cb2 == 2 || cb2 == 3)
            {
                if (ijel[iel][5][1] == 0)
                {
                    ntemp = sje[iel][3][0][0];
                    if (cbc[ntemp][5] == 3 && sje[ntemp][5][0][0] < iel)
                    {
                        newc[iel] = 3;
                        vassign[iel][0] = 3;
                        vassign[iel][1] = 5;
                        vassign[iel][2] = 7;
                    }
                    else
                    {
                        newc[iel] = 4;
                        vassign[iel][0] = 1;
                        vassign[iel][1] = 3;
                        vassign[iel][2] = 5;
                        vassign[iel][3] = 7;
                    }
                    // if ijel[iel][5][1]=1
                }
                else
                {
                    ntemp = sje[iel][3][0][0];
                    if (cbc[ntemp][0] == 3 && sje[ntemp][0][0][0] < iel)
                    {
                        ntemp1 = sje[iel][5][0][0];
                        if (cbc[ntemp1][0] == 3 && sje[ntemp1][0][0][0] < iel)
                        {
                            newc[iel] = 1;
                            vassign[iel][0] = 7;
                        }
                        else
                        {
                            newc[iel] = 2;
                            vassign[iel][0] = 3;
                            vassign[iel][1] = 7;
                        }
                    }
                    else
                    {
                        ntemp1 = sje[iel][5][0][0];
                        if (cbc[ntemp1][0] == 3 && sje[ntemp1][0][0][0] < iel)
                        {
                            newc[iel] = 2;
                            vassign[iel][0] = 5;
                            vassign[iel][1] = 7;
                        }
                        else
                        {
                            newc[iel] = 3;
                            vassign[iel][0] = 3;
                            vassign[iel][1] = 5;
                            vassign[iel][2] = 7;
                        }
                    }
                }
            }

        }
        else if (sumcb == 6)
        {
            // the three face type for face 1,3 and 5 are 0 0 0(not
            // necessarily in this order)
            newc[iel] = 8;
            vassign[iel][0] = 0;
            vassign[iel][1] = 1;
            vassign[iel][2] = 2;
            vassign[iel][3] = 3;
            vassign[iel][4] = 4;
            vassign[iel][5] = 5;
            vassign[iel][6] = 6;
            vassign[iel][7] = 7;

        }
        else if (sumcb == 9)
        {
            // the three face type for face 1,3 and 5 are 0 0 1(not
            // necessarily in this order)
            newc[iel] = 7;
            vassign[iel][0] = 1;
            vassign[iel][1] = 2;
            vassign[iel][2] = 3;
            vassign[iel][3] = 4;
            vassign[iel][4] = 5;
            vassign[iel][5] = 6;
            vassign[iel][6] = 7;

        }
        else if (sumcb == 12)
        {
            // the three face type for face 1,3 and 5 are 0 1 1(not
            // necessarily in this order)
            if (cb == 0)
            {
                ntemp = sje[iel][1][0][0];
                if (cbc[ntemp][3] == 3 && sje[ntemp][3][0][0] < iel)
                {
                    newc[iel] = 6;
                    vassign[iel][0] = 1;
                    vassign[iel][1] = 2;
                    vassign[iel][2] = 3;
                    vassign[iel][3] = 5;
                    vassign[iel][4] = 6;
                    vassign[iel][5] = 7;
                }
                else
                {
                    newc[iel] = 7;
                    vassign[iel][0] = 1;
                    vassign[iel][1] = 2;
                    vassign[iel][2] = 3;
                    vassign[iel][3] = 4;
                    vassign[iel][4] = 5;
                    vassign[iel][5] = 6;
                    vassign[iel][6] = 7;
                }
            }
            else if (cb1 == 0)
            {
                newc[iel] = 7;
                vassign[iel][0] = 1;
                vassign[iel][1] = 2;
                vassign[iel][2] = 3;
                vassign[iel][3] = 4;
                vassign[iel][4] = 5;
                vassign[iel][5] = 6;
                vassign[iel][6] = 7;
            }
            else if (cb2 == 0)
            {
                ntemp = sje[iel][3][0][0];
                if (cbc[ntemp][5] == 3 && sje[ntemp][5][0][0] < iel)
                {
                    newc[iel] = 6;
                    vassign[iel][0] = 2;
                    vassign[iel][1] = 3;
                    vassign[iel][2] = 4;
                    vassign[iel][3] = 5;
                    vassign[iel][4] = 6;
                    vassign[iel][5] = 7;
                }
                else
                {
                    newc[iel] = 7;
                    vassign[iel][0] = 1;
                    vassign[iel][1] = 2;
                    vassign[iel][2] = 3;
                    vassign[iel][3] = 4;
                    vassign[iel][4] = 5;
                    vassign[iel][5] = 6;
                    vassign[iel][6] = 7;
                }
            }

        }
        else if (sumcb == 15)
        {
            // the three face type for face 1,3 and 5 are 1 1 1(not
            // necessarily in this order)
            ntemp = sje[iel][3][0][0];
            ntemp1 = sje[iel][1][0][0];
            if (cbc[ntemp][5] == 3 && sje[ntemp][5][0][0] < iel)
            {
                if (cbc[ntemp][1] == 3 && sje[ntemp][1][0][0] < iel)
                {
                    if (cbc[ntemp1][5] == 3 && sje[ntemp1][5][0][0] < iel)
                    {
                        newc[iel] = 4;
                        vassign[iel][0] = 3;
                        vassign[iel][1] = 5;
                        vassign[iel][2] = 6;
                        vassign[iel][3] = 7;
                    }
                    else
                    {
                        newc[iel] = 5;
                        vassign[iel][0] = 2;
                        vassign[iel][1] = 3;
                        vassign[iel][2] = 5;
                        vassign[iel][3] = 6;
                        vassign[iel][4] = 7;
                    }
                }
                else
                {
                    if (cbc[ntemp1][5] == 3 && sje[ntemp1][5][0][0] < iel)
                    {
                        newc[iel] = 5;
                        vassign[iel][0] = 3;
                        vassign[iel][1] = 4;
                        vassign[iel][2] = 5;
                        vassign[iel][3] = 6;
                        vassign[iel][4] = 7;
                    }
                    else
                    {
                        newc[iel] = 6;
                        vassign[iel][0] = 2;
                        vassign[iel][1] = 3;
                        vassign[iel][2] = 4;
                        vassign[iel][3] = 5;
                        vassign[iel][4] = 6;
                        vassign[iel][5] = 7;
                    }
                }
            }
            else
            {
                if (cbc[ntemp][1] == 3 && sje[ntemp][1][0][0] < iel)
                {
                    if (cbc[ntemp1][5] == 3 && sje[ntemp1][5][0][0] < iel)
                    {
                        newc[iel] = 5;
                        vassign[iel][0] = 1;
                        vassign[iel][1] = 3;
                        vassign[iel][2] = 5;
                        vassign[iel][3] = 6;
                        vassign[iel][4] = 7;
                    }
                    else
                    {
                        newc[iel] = 6;
                        vassign[iel][0] = 1;
                        vassign[iel][1] = 2;
                        vassign[iel][2] = 3;
                        vassign[iel][3] = 5;
                        vassign[iel][4] = 6;
                        vassign[iel][5] = 7;
                    }
                }
                else
                {
                    if (cbc[ntemp1][5] == 3 && sje[ntemp1][5][0][0] < iel)
                    {
                        newc[iel] = 6;
                        vassign[iel][0] = 1;
                        vassign[iel][1] = 3;
                        vassign[iel][2] = 4;
                        vassign[iel][3] = 5;
                        vassign[iel][4] = 6;
                        vassign[iel][5] = 7;

                    }
                    else
                    {
                        newc[iel] = 7;
                        vassign[iel][0] = 1;
                        vassign[iel][1] = 2;
                        vassign[iel][2] = 3;
                        vassign[iel][3] = 4;
                        vassign[iel][4] = 5;
                        vassign[iel][5] = 6;
                        vassign[iel][6] = 7;
                    }
                }
            }
        }
    }

    // end computing how many new mortar vertex points will be generated
    // on each element.

    // Compute (potentially in parallel) front[iel], which records how many
    // new mortar point indices are to be generated from element 0 to iel.
    // front[iel]=newc[0]+newc[1]+...+newc[iel]

    ncopy(front, newc, nelt);

    parallel_add(front);

    // On each element, generate new mortar point indices and assign them
    // to all elements sharing this mortar point. Note, if a mortar point
    // is shared by several elements, the mortar point index of it will only
    // be generated on the element with the lowest element index.

    for (iel = 0; iel < nelt; iel++)
    {
        // compute the starting vertex mortar point index in element iel
        front[iel] = front[iel] - newc[iel];

        for (i = 0; i < newc[iel]; i++)
        {
            // count is the new mortar index number, which will be assigned
            // to a vertex of iel and broadcast to all other elements sharing
            // this vertex point.
            count = front[iel] + i;
            mortar_vertex(vassign[iel][i], iel, count);
        }
    }

    // nvertex records how many mortar indices are for element vertices.
    // It is used in the computation of the preconditioner.
    nvertex = count + 1;

    // CONFORMING EDGE AND FACE INTERIOR

    // find out how many new mortar point indices will be assigned to all
    // conforming edges and all conforming face interiors on each element


    // eassign[iel][i]=1   indicates that the i'th edge on iel will
    //                        generate new mortar points.
    // ncon_edge[iel][i]=1 indicates that the i'th edge on iel is
    //                        nonconforming
    n1 = 12 * nelt;
    l_init((int *)ncon_edge, n1, 0);
    l_init((int *)eassign, n1, 0);

    // fassign[iel][i]=1 indicates that the i'th face of iel will
    //                      generate new mortar points
    n2 = 6 * nelt;
    l_init((int *)fassign, n2, 0);

    // newe records how many new edges are to be assigned
    // diagn[iel][n][0] records the element index of neighbor element of iel,
    //            that shares edge n of iel
    // diagn[iel][n][1] records the neighbor element diagn[iel][n][0] shares
    //            which part of edge n of iel. diagn[iel][n][1]=0 refers to left
    //            or bottom half of the edge n, diagn[iel][n][1]=1 refers
    //            to the right or top part of edge n.
    // if_1_edge[iel][n]=1 indicates that the size of iel is smaller than
    //            that of its neighbor connected, neighbored by edge n only
    for (iel = 0; iel < nelt; iel++)
    {
        newc[iel] = 0;
        newe[iel] = 0;
        newi[iel] = 0;
        cb1 = cbc[iel][0];
        cb2 = cbc[iel][1];
        cb3 = cbc[iel][2];
        cb4 = cbc[iel][3];
        cb5 = cbc[iel][4];
        cb6 = cbc[iel][5];

        // on face 6

        if (cb6 == 0)
        {
            if (cb4 == 0 || cb4 == 1)
            {
                // if face 6 is of type 0 and face 4 is of type 0 or type 1, the edge
                // shared by face 4 and 6 (edge 10) will generate new mortar point
                // indices.
                newe[iel] = newe[iel] + 1;
                eassign[iel][10] = 1;
            }
            if (cb1 != 3)
            {
                // if face 1 is of type 3, the edge shared by face 6 and 1 (edge 0)
                // will generate new mortar points indices.
                newe[iel] = newe[iel] + 1;
                eassign[iel][0] = 1;
            }
            if (cb3 != 3)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][8] = 1;
            }
            if (cb2 == 0 || cb2 == 1)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][4] = 1;
            }
        }
        else if (cb6 == 1)
        {
            if (cb4 == 0)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][10] = 1;
            }
            else if (cb4 == 1)
            {

                // If face 6 and face 4 both are of type 1, ntemp is the neighbor
                // element on face 4.
                ntemp = sje[iel][3][0][0];

                // if ntemp's face 6 is not noncoforming or the neighbor element
                // of ntemp on face 6 has an element index larger than iel, the
                // edge shared by face 6 and 4 (edge 10) will generate new mortar
                // point indices.
                if (cbc[ntemp][5] != 3 || sje[ntemp][5][0][0] > iel)
                {

                    newe[iel] = newe[iel] + 1;
                    eassign[iel][10] = 1;
                    // if the face 6 of ntemp is of type 2
                    if (cbc[ntemp][5] == 2)
                    {
                        // The neighbor element of iel, neighbored by edge 10, is
                        // sje[ntemp][5][0][0] (the neighbor element of ntemp on ntemp's
                        // face 6).
                        diagn[iel][10][0] = sje[ntemp][5][0][0];
                        // The neighbor element of iel, neighbored by edge 10 shares
                        // the ijel[iel][5][1] part of edge 10 of iel
                        diagn[iel][10][1] = ijel[iel][5][1];
                        // edge 9 of element sje[ntemp][5][0][0] (the neighbor element of
                        // ntemp on ntemp's face 6) is a nonconforming edge
                        ncon_edge[sje[ntemp][5][0][0]][9] = 1;
                        // if_1_edge[iel][n]=1 indicates that iel is of a smaller
                        //size than its neighbor element, neighbored by edge n of iel only.
                        if_1_edge[iel][10] = 1;
                    }
                    if (cbc[ntemp][5] == 3 && sje[ntemp][5][0][0] > iel)
                    {
                        diagn[iel][10][0] = sje[ntemp][5][ijel[iel][5][1]][1];
                    }
                }
            }

            if (cb1 == 0)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][0] = 1;
            }
            else if (cb1 == 1)
            {
                ntemp = sje[iel][0][0][0];
                if (cbc[ntemp][5] != 3 || sje[ntemp][5][0][0] > iel)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][0] = 1;
                    if (cbc[ntemp][5] == 2)
                    {
                        diagn[iel][0][0] = sje[ntemp][5][0][0];
                        diagn[iel][0][1] = ijel[iel][5][0];
                        ncon_edge[sje[ntemp][5][0][0]][6] = 1;
                        if_1_edge[iel][0] = 1;
                    }
                    if (cbc[ntemp][5] == 3 && sje[ntemp][5][0][0] > iel)
                    {
                        diagn[iel][0][0] = sje[ntemp][5][0][ijel[iel][5][0]];
                    }
                }
            }
            else if (cb1 == 2)
            {
                if (ijel[iel][5][1] == 1)
                {
                    ntemp = sje[iel][0][0][0];
                    if (cbc[ntemp][5] == 1)
                    {
                        newe[iel] = newe[iel] + 1;
                        eassign[iel][0] = 1;
                        // if cbc[ntemp][5]=2
                    }
                    else
                    {
                        if (sje[ntemp][5][0][0] > iel)
                        {
                            newe[iel] = newe[iel] + 1;
                            eassign[iel][0] = 1;
                            diagn[iel][0][0] = sje[ntemp][5][0][0];
                        }
                    }
                }
                else
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][0] = 1;
                }
            }

            if (cb3 == 0)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][8] = 1;
            }
            else if (cb3 == 1)
            {
                ntemp = sje[iel][2][0][0];
                if (cbc[ntemp][5] != 3 || sje[ntemp][5][0][0] > iel)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][8] = 1;
                    if (cbc[ntemp][5] == 2)
                    {
                        diagn[iel][8][0] = sje[ntemp][5][0][0];
                        diagn[iel][8][1] = ijel[iel][5][1];
                        ncon_edge[sje[ntemp][5][0][0]][11] = 1;
                        if_1_edge[iel][8] = 1;
                    }
                    if (cbc[ntemp][5] == 3 && sje[ntemp][5][0][0] > iel)
                    {
                        diagn[iel][8][0] = sje[ntemp][5][ijel[iel][5][1]][1];
                    }
                }
            }
            else if (cb3 == 2)
            {
                if (ijel[iel][5][0] == 1)
                {
                    ntemp = sje[iel][2][0][0];
                    if (cbc[ntemp][5] == 1)
                    {
                        newe[iel] = newe[iel] + 1;
                        eassign[iel][8] = 1;
                        // if cbc[ntemp][5]=2
                    }
                    else
                    {
                        if (sje[ntemp][5][0][0] > iel)
                        {
                            newe[iel] = newe[iel] + 1;
                            eassign[iel][8] = 1;
                            diagn[iel][8][0] = sje[ntemp][5][0][0];
                        }
                    }
                }
                else
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][8] = 1;
                }
            }

            if (cb2 == 0)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][4] = 1;
            }
            else if (cb2 == 1)
            {
                ntemp = sje[iel][1][0][0];
                if (cbc[ntemp][5] != 3 || sje[ntemp][5][0][0] > iel)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][4] = 1;
                    if (cbc[ntemp][5] == 2)
                    {
                        diagn[iel][4][0] = sje[ntemp][5][0][0];
                        diagn[iel][4][1] = ijel[iel][5][0];
                        ncon_edge[sje[ntemp][5][0][0]][2] = 1;
                        if_1_edge[iel][4] = 1;
                    }
                    if (cbc[ntemp][5] == 3 && sje[ntemp][5][0][0] > iel)
                    {
                        diagn[iel][8][0] = sje[ntemp][5][ijel[iel][5][1]][1];
                    }
                }
            }
        }

        // one face 4
        if (cb4 == 0)
        {
            if (cb1 != 3)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][3] = 1;
            }
            if (cb5 != 3)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][11] = 1;
            }
            if (cb2 == 0 || cb2 == 1)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][7] = 1;
            }

        }
        else if (cb4 == 1)
        {
            if (cb1 == 2)
            {
                if (ijel[iel][3][1] == 0)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][3] = 1;
                }
                else
                {
                    ntemp = sje[iel][3][0][0];
                    if (cbc[ntemp][0] != 3 || sje[ntemp][0][0][0] > iel)
                    {
                        newe[iel] = newe[iel] + 1;
                        eassign[iel][3] = 1;
                        if (cbc[ntemp][0] == 3 && sje[ntemp][0][0][0] > iel)
                        {
                            diagn[iel][3][0] = sje[ntemp][0][1][ijel[iel][3][0]];
                        }
                    }
                }
            }
            else if (cb1 == 0)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][3] = 1;
            }
            else if (cb1 == 1)
            {
                ntemp = sje[iel][3][0][0];
                if (cbc[ntemp][0] != 3 || sje[ntemp][0][0][0] > iel)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][3] = 1;
                    if (cbc[ntemp][0] == 2)
                    {
                        diagn[iel][3][0] = sje[ntemp][0][0][0];
                        diagn[iel][3][1] = ijel[iel][3][0];
                        ncon_edge[sje[ntemp][0][0][0]][5] = 1;
                        if_1_edge[iel][3] = 1;
                    }
                    if (cbc[ntemp][0] == 3 && sje[ntemp][0][0][0] > iel)
                    {
                        diagn[iel][3][0] = sje[ntemp][0][1][ijel[iel][3][0]];
                    }
                }
            }
            if (cb5 == 2)
            {
                if (ijel[iel][3][0] == 0)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][11] = 1;
                }
                else
                {
                    ntemp = sje[iel][3][0][0];
                    if (cbc[ntemp][4] != 3 || sje[ntemp][4][0][0] > iel)
                    {
                        newe[iel] = newe[iel] + 1;
                        eassign[iel][11] = 1;
                        if (cbc[ntemp][4] == 3 && sje[ntemp][4][0][0] > iel)
                        {
                            diagn[iel][11][0] = sje[ntemp][4][ijel[iel][3][1]][1];
                        }
                    }
                }
            }
            else if (cb5 == 0)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][11] = 1;
            }
            else if (cb5 == 1)
            {
                ntemp = sje[iel][3][0][0];
                if (cbc[ntemp][4] != 3 || sje[ntemp][4][0][0] > iel)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][11] = 1;
                    if (cbc[ntemp][4] == 2)
                    {
                        diagn[iel][11][0] = sje[ntemp][4][0][0];
                        diagn[iel][11][1] = ijel[iel][3][1];
                        ncon_edge[sje[ntemp][4][0][0]][8] = 1;
                        if_1_edge[iel][11] = 1;
                    }
                    if (cbc[ntemp][4] == 3 && sje[ntemp][4][0][0] > iel)
                    {
                        diagn[iel][11][0] = sje[ntemp][4][ijel[iel][3][1]][1];
                    }
                }
            }
            if (cb2 == 0)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][7] = 1;
            }
            else if (cb2 == 1)
            {
                ntemp = sje[iel][3][0][0];
                if (cbc[ntemp][1] != 3 || sje[ntemp][1][0][0] > iel)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][7] = 1;
                    if (cbc[ntemp][1] == 2)
                    {
                        diagn[iel][7][0] = sje[ntemp][1][0][0];
                        diagn[iel][7][1] = ijel[iel][3][0];
                        ncon_edge[sje[ntemp][1][0][0]][1] = 1;
                        if_1_edge[iel][7] = 1;
                    }
                    if (cbc[ntemp][1] == 3 && sje[ntemp][1][0][0] > iel)
                    {
                        diagn[iel][7][0] = sje[ntemp][2][1][ijel[iel][3][0]];
                    }
                }
            }
        }

        // on face 2
        if (cb2 == 0)
        {
            if (cb3 != 3)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][5] = 1;
            }
            if (cb5 != 3)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][6] = 1;
            }
        }
        else if (cb2 == 1)
        {
            if (cb3 == 2)
            {
                if (ijel[iel][1][1] == 0)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][5] = 1;
                }
                else
                {
                    ntemp = sje[iel][1][0][0];
                    if (cbc[ntemp][2] != 3 || sje[ntemp][2][0][0] > iel)
                    {
                        newe[iel] = newe[iel] + 1;
                        eassign[iel][5] = 1;
                        if (cbc[ntemp][2] == 3 && sje[ntemp][2][0][0] > iel)
                        {
                            diagn[iel][5][0] = sje[ntemp][2][1][ijel[iel][1][0]];
                        }
                    }
                }
            }
            else if (cb3 == 0)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][5] = 1;
            }
            else if (cb3 == 1)
            {
                ntemp = sje[iel][1][0][0];
                if (cbc[ntemp][2] != 3 || sje[ntemp][2][0][0] > iel)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][5] = 1;
                    if (cbc[ntemp][2] == 2)
                    {
                        diagn[iel][5][0] = sje[ntemp][2][0][0];
                        diagn[iel][5][1] = ijel[iel][1][0];
                        ncon_edge[sje[ntemp][2][0][0]][3] = 1;
                        if_1_edge[iel][5] = 1;
                    }
                    if (cbc[ntemp][2] == 3 && sje[ntemp][2][0][0] > iel)
                    {
                        diagn[iel][5][0] = sje[ntemp][2][1][ijel[iel][3][0]];
                    }
                }
            }
            if (cb5 == 2)
            {
                if (ijel[iel][1][0] == 0)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][6] = 1;
                }
                else
                {
                    ntemp = sje[iel][1][0][0];
                    if (cbc[ntemp][4] != 3 || sje[ntemp][4][0][0] > iel)
                    {
                        newe[iel] = newe[iel] + 1;
                        eassign[iel][6] = 1;
                        if (cbc[ntemp][4] == 3 && sje[ntemp][4][0][0] > iel)
                        {
                            diagn[iel][6][0] = sje[ntemp][4][1][ijel[iel][1][1]];
                        }
                    }
                }
            }
            else if (cb5 == 0)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][6] = 1;
            }
            else if (cb5 == 1)
            {
                ntemp = sje[iel][1][0][0];
                if (cbc[ntemp][4] != 3 || sje[ntemp][4][0][0] > iel)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][6] = 1;
                    if (cbc[ntemp][4] == 2)
                    {
                        diagn[iel][6][0] = sje[ntemp][4][0][0];
                        diagn[iel][6][1] = ijel[iel][1][1];
                        ncon_edge[sje[ntemp][4][0][0]][0] = 1;
                        if_1_edge[iel][6] = 1;
                    }
                    if (cbc[ntemp][4] == 3 && sje[ntemp][4][0][0] > iel)
                    {
                        diagn[iel][6][0] = sje[ntemp][4][ijel[iel][3][1]][1];
                    }
                }
            }
        }

        // on face 1
        if (cb1 == 1)
        {
            newe[iel] = newe[iel] + 2;
            eassign[iel][1] = 1;
            if (cb3 == 1)
            {
                ntemp = sje[iel][0][0][0];
                if (cbc[ntemp][2] == 2)
                {
                    diagn[iel][1][0] = sje[ntemp][2][0][0];
                    diagn[iel][1][1] = ijel[iel][0][0];
                    ncon_edge[sje[ntemp][2][0][0]][7] = 1;
                    if_1_edge[iel][1] = 1;
                }
                else if (cbc[ntemp][2] == 3)
                {
                    diagn[iel][1][0] = sje[ntemp][2][0][ijel[iel][0][0]];
                }
            }
            else if (cb3 == 2)
            {
                ntemp = sje[iel][2][0][0];
                if (ijel[iel][0][1] == 1)
                {
                    if (cbc[ntemp][0] == 2)
                    {
                        diagn[iel][1][0] = sje[ntemp][0][0][0];
                    }
                }
            }

            eassign[iel][2] = 1;
            if (cb5 == 1)
            {
                ntemp = sje[iel][0][0][0];
                if (cbc[ntemp][4] == 2)
                {
                    diagn[iel][2][0] = sje[ntemp][4][0][0];
                    diagn[iel][2][1] = ijel[iel][0][1];
                    ncon_edge[sje[ntemp][4][0][0]][4] = 1;
                    if_1_edge[iel][2] = 1;
                }
                else if (cbc[ntemp][4] == 3)
                {
                    diagn[iel][2][0] = sje[ntemp][4][0][ijel[iel][0][1]];
                }
            }
            else if (cb5 == 2)
            {
                ntemp = sje[iel][4][0][0];
                if (ijel[iel][0][0] == 1)
                {
                    if (cbc[ntemp][0] == 2)
                    {
                        diagn[iel][2][0] = sje[ntemp][0][0][0];
                    }
                }

            }
        }
        else if (cb1 == 2)
        {
            if (cb3 == 2)
            {
                ntemp = sje[iel][0][0][0];
                if (cbc[ntemp][2] != 3)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][1] = 1;
                    if (cbc[ntemp][2] == 2)
                    {
                        diagn[iel][1][0] = sje[ntemp][2][0][0];
                    }
                }
            }
            else if (cb3 == 0 || cb3 == 1)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][1] = 1;
                if (cb3 == 1)
                {
                    ntemp = sje[iel][0][0][0];
                    if (cbc[ntemp][2] == 2)
                    {
                        diagn[iel][1][0] = sje[ntemp][2][0][0];
                    }
                }
            }
            if (cb5 == 2)
            {
                ntemp = sje[iel][0][0][0];
                if (cbc[ntemp][4] != 3)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][2] = 1;
                    if (cbc[ntemp][4] == 2)
                    {
                        diagn[iel][2][0] = sje[ntemp][4][0][0];
                    }
                }
            }
            else if (cb5 == 0 || cb5 == 1)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][2] = 1;
                if (cb5 == 1)
                {
                    ntemp = sje[iel][0][0][0];
                    if (cbc[ntemp][4] == 2)
                    {
                        diagn[iel][2][0] = sje[ntemp][4][0][0];
                    }
                }
            }
        }
        else if (cb1 == 0)
        {
            if (cb3 != 3)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][1] = 1;
            }
            if (cb5 != 3)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][2] = 1;
            }
        }

        // on face 3
        if (cb3 == 1)
        {
            newe[iel] = newe[iel] + 1;
            eassign[iel][9] = 1;
            if (cb5 == 1)
            {
                ntemp = sje[iel][2][0][0];
                if (cbc[ntemp][4] == 2)
                {
                    diagn[iel][9][0] = sje[ntemp][4][0][0];
                    diagn[iel][9][1] = ijel[iel][2][1];
                    ncon_edge[sje[ntemp][4][0][0]][10] = 1;
                    if_1_edge[iel][9] = 1;
                }
            }
            if (ijel[iel][2][0] == 1)
            {
                ntemp = sje[iel][2][0][0];
                if (cbc[ntemp][4] == 3)
                {
                    diagn[iel][9][0] = sje[ntemp][4][ijel[iel][2][1]][0];
                }
            }
        }
        else if (cb3 == 2)
        {
            if (cb5 == 2)
            {
                ntemp = sje[iel][2][0][0];
                if (cbc[ntemp][4] != 3)
                {
                    newe[iel] = newe[iel] + 1;
                    eassign[iel][9] = 1;
                    if (cbc[ntemp][4] == 2)
                    {
                        diagn[iel][9][0] = sje[ntemp][4][0][0];
                    }
                }
            }
            else if (cb5 == 0 || cb5 == 1)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][9] = 1;
                if (cb5 == 1)
                {
                    ntemp = sje[iel][2][0][0];
                    if (cbc[ntemp][4] == 2)
                    {
                        diagn[iel][9][0] = sje[ntemp][4][0][0];
                    }
                }
            }
        }
        else if (cb3 == 0)
        {
            if (cb5 != 3)
            {
                newe[iel] = newe[iel] + 1;
                eassign[iel][9] = 1;
            }
        }

        // CONFORMING FACE INTERIOR

        // find how many new mortar point indices will be assigned
        // to face interiors on all faces on each element

        // newi record how many new face interior points will be assigned

        // on face 6
        if (cb6 == 1 || cb6 == 0)
        {
            newi[iel] = newi[iel] + 9;
            fassign[iel][5] = 1;
        }
        // on face 4
        if (cb4 == 1 || cb4 == 0)
        {
            newi[iel] = newi[iel] + 9;
            fassign[iel][3] = 1;
        }
        // on face 2
        if (cb2 == 1 || cb2 == 0)
        {
            newi[iel] = newi[iel] + 9;
            fassign[iel][1] = 1;
        }
        // on face 1
        if (cb1 != 3)
        {
            newi[iel] = newi[iel] + 9;
            fassign[iel][0] = 1;
        }
        // on face 3
        if (cb3 != 3)
        {
            newi[iel] = newi[iel] + 9;
            fassign[iel][2] = 1;
        }
        // on face 5
        if (cb5 != 3)
        {
            newi[iel] = newi[iel] + 9;
            fassign[iel][4] = 1;
        }

        // newc is the total number of new mortar point indices
        // to be assigned to each element.
        newc[iel] = newe[iel] * 3 + newi[iel];
    }

    // Compute (potentially in parallel) front[iel], which records how
    // many new mortar point indices are to be assigned (to conforming
    // edges and conforming face interiors) from element 0 to iel.
    // front[iel]=newc[0]+newc[1]+...+newc[iel]

    ncopy(front, newc, nelt);

    parallel_add(front);

    // nmor is the total number or mortar points
    nmor = nvertex + front[nelt - 1];

    // Generate (potentially in parallel) new mortar point indices on
    // each conforming element face. On each face, first visit all
    // conforming edges, and then the face interior.
    for (iel = 0; iel < nelt; iel++)
    {
        front[iel] = front[iel] - newc[iel];
        count = nvertex + front[iel];
        for (i = 0; i < 6; i++)
        {
            cb1 = cbc[iel][i];
            if (i < 2)
            {
                ne = 4;
                space = 1;
            }
            else if (i < 4)
            {
                ne = 3;
                space = 2;

                // i loops over faces. Only 4 faces need to be examed for edge visit.
                // On face 1, edge 0,1,2 and 3 will be visited. On face 2, edge 4,5,6
                // and 7 will be visited. On face 3, edge 8 and 9 will be visited and
                // on face 4, edge 10 and 11 will be visited. The 12 edges can be
                // covered by four faces, there is no need to visit edges on face
                // 5 and 6.  So ne is set to be 0.
                // However, i still needs to loop over 4 and 5, since the interiors
                // of face 5 and 6 still need to be visited.

            }
            else
            {
                ne = 0;
                space = 1;
            }

            for (ie = 0; ie < ne; ie += space)
            {
                edge_g = edgenumber[i][ie];
                if (eassign[iel][edge_g])
                {
                    // generate the new mortar points index, mor_v
                    mor_assign(mor_v, &count);
                    // assign mor_v to local edge ie of face i on element iel
                    mor_edge(ie, i, iel, mor_v);

                    // Since this edge is shared by another face of element
                    // iel, assign mor_v to the corresponding edge on the other
                    // face also.

                    // find the other face
                    face2 = f_e_ef[i][ie];
                    // find the local edge index of this edge on the other face
                    ie2 = localedgenumber[edge_g][face2];
                    // asssign mor_v  to local edge ie2 of face face2 on element iel
                    mor_edge(ie2, face2, iel, mor_v);

                    // There are some neighbor elements also sharing this edge. Assign
                    // mor_v to neighbor element, neighbored by face i.
                    if (cbc[iel][i] == 2)
                    {
                        ntemp = sje[iel][i][0][0];
                        mor_edge(ie, jjface[i], ntemp, mor_v);
                        mor_edge(op[ie2], face2, ntemp, mor_v);
                    }

                    // assign mor_v  to neighbor element neighbored by face face2
                    if (cbc[iel][face2] == 2)
                    {
                        ntemp = sje[iel][face2][0][0];
                        mor_edge(ie2, jjface[face2], ntemp, mor_v);
                        mor_edge(op[ie], i, ntemp, mor_v);
                    }

                    // assign mor_v to neighbor element sharing this edge

                    // if the neighbor is of the same size of iel
                    if (!if_1_edge[iel][edgenumber[i][ie]])
                    {
                        if (diagn[iel][edgenumber[i][ie]][0] != -1)
                        {
                            ntemp = diagn[iel][edgenumber[i][ie]][0];
                            mor_edge(op[ie2], jjface[face2], ntemp, mor_v);
                            mor_edge(op[ie], jjface[i], ntemp, mor_v);
                        }

                        // if the neighbor has a size larger than iel's
                    }
                    else
                    {
                        if (diagn[iel][edgenumber[i][ie]][0] != -1)
                        {
                            ntemp = diagn[iel][edgenumber[i][ie]][0];
                            mor_ne(mor_v, diagn[iel][edgenumber[i][ie]][1],
                                   ie, i, ie2, face2, iel, ntemp);
                        }
                    }
                }
            }

            if (fassign[iel][i])
            {
                // generate new mortar points index in face interior.
                // if face i is of type 2 or iel doesn't have a neighbor element,
                // assign new mortar point indices to interior mortar points
                // of face i of iel.
                cb = cbc[iel][i];
                if (cb == 1 || cb == 0)
                {
                    for (jj = 1; jj < LX1 - 1; jj++)
                    {
                        for (ii = 1; ii < LX1 - 1; ii++)
                        {
                            idmo[iel][i][0][0][jj][ii] = count;
                            count = count + 1;
                        }
                    }

                    // if face i is of type 2, assign new mortar point indices
                    // to iel as well as to the neighboring element on face i
                }
                else if (cb == 2)
                {
                    if (idmo[iel][i][0][0][1][1] == -1)
                    {
                        ntemp = sje[iel][i][0][0];
                        jface = jjface[i];
                        for (jj = 1; jj < LX1 - 1; jj++)
                        {
                            for (ii = 1; ii < LX1 - 1; ii++)
                            {
                                idmo[iel][i][0][0][jj][ii] = count;
                                idmo[ntemp][jface][0][0][jj][ii] = count;
                                count = count + 1;
                            }
                        }
                    }
                }
            }
        }
    }

    // for edges on nonconforming faces, copy the mortar points indices
    // from neighbors.
    for (iel = 0; iel < nelt; iel++)
    {
        for (i = 0; i < 6; i++)
        {
            cb = cbc[iel][i];
            if (cb == 3)
            {
                // edges
                edgecopy_s(i, iel);
            }

            // face interior

            jface = jjface[i];
            if (cb == 3)
            {
                for (iii = 0; iii < 2; iii++)
                {
                    for (jjj = 0; jjj < 2; jjj++)
                    {
                        ntemp = sje[iel][i][jjj][iii];
                        for (jj = 0; jj < LX1; jj++)
                        {
                            for (ii = 0; ii < LX1; ii++)
                            {
                                idmo[iel][i][jjj][iii][jj][ii] =
                                    idmo[ntemp][jface][0][0][jj][ii];
                            }
                        }
                        idmo[iel][i][jjj][iii][0][0] = idmo[ntemp][jface][0][0][0][0];
                        idmo[iel][i][jjj][iii][0][LX1 - 1] = idmo[ntemp][jface][1][0][0][LX1 - 1];
                        idmo[iel][i][jjj][iii][LX1 - 1][0] = idmo[ntemp][jface][0][1][LX1 - 1][0];
                        idmo[iel][i][jjj][iii][LX1 - 1][LX1 - 1] =
                            idmo[ntemp][jface][1][1][LX1 - 1][LX1 - 1];
                    }
                }
            }
        }
    }
}


//-----------------------------------------------------------------
// This subroutine fills array emo.
// emo  records all elements sharing the same mortar point
//              (only applies to element vertices) .
// emo[n][i][0] gives the element ID of the i'th element sharing
//              mortar point n. (emo[n][i][0]=ie), ie is element
//              index.
// emo[n][i][1] gives the vertex index of mortar point n on this
//              element (emo[n][i][1]=ng), ng is the vertex index.
// nemo[n] records the total number of elements sharing mortar
//              point n.
//-----------------------------------------------------------------
void get_emo(int ie, int n, int ng)
{
    int ntemp, i;
    int L1;

    L1 = 0;
    for (i = 0; i <= nemo[n]; i++)
    {
        if (emo[n][i][0] == ie) L1 = 1;
    }
    if (!L1)
    {
        ntemp = nemo[n] + 1;
        nemo[n] = ntemp;
        emo[n][ntemp][0] = ie;
        emo[n][ntemp][1] = ng;
    }
}


//-----------------------------------------------------------------
// Check whether the i's vertex of element iel is at the same
// location as j's vertex of element ntemp.
//-----------------------------------------------------------------
int ifsame(int iel, int i, int ntemp, int j)
{
    if (ntemp == -1 || iel == -1) return 0;
    if (xc[iel][i] == xc[ntemp][j] && yc[iel][i] == yc[ntemp][j] &&
            zc[iel][i] == zc[ntemp][j])
    {
        return 1;
    }
    return 0;
}


//-----------------------------------------------------------------
// Assign three consecutive numbers for mor_v, which will
// be assigned to the three interior points of an edge as the
// mortar point indices.
//-----------------------------------------------------------------
void mor_assign(int mor_v[3], int *count)
{
    int i;

    for (i = 0; i < 3; i++)
    {
        mor_v[i] = *count;
        *count = *count + 1;
    }
}


//-----------------------------------------------------------------
// Copy the mortar points index from mor_v to local
// edge ie of the face'th face on element iel.
// The edge is conforming.
//-----------------------------------------------------------------
void mor_edge(int ie, int face, int iel, int mor_v[3])
{
    int i, j, nn;

    if (ie == 0)
    {
        j = 0;
        for (nn = 1; nn < LX1 - 1; nn++)
        {
            idmo[iel][face][0][0][j][nn] = mor_v[nn - 1];
        }
    }
    else if (ie == 1)
    {
        i = LX1 - 1;
        for (nn = 1; nn < LX1 - 1; nn++)
        {
            idmo[iel][face][0][0][nn][i] = mor_v[nn - 1];
        }
    }
    else if (ie == 2)
    {
        j = LX1 - 1;
        for (nn = 1; nn < LX1 - 1; nn++)
        {
            idmo[iel][face][0][0][j][nn] = mor_v[nn - 1];
        }
    }
    else if (ie == 3)
    {
        i = 0;
        for (nn = 1; nn < LX1 - 1; nn++)
        {
            idmo[iel][face][0][0][nn][i] = mor_v[nn - 1];
        }
    }
}


//------------------------------------------------------------
// Copy mortar points index on edges from neighbor elements
// to an element face of the 3rd type.
//------------------------------------------------------------
void edgecopy_s(int face, int iel)
{
    int ntemp1, ntemp2, ntemp3, ntemp4;
    int edge_g, edge_l, face2, mor_s_v[2][4], i;

    // find four neighbors on this face (3rd type)
    ntemp1 = sje[iel][face][0][0];
    ntemp2 = sje[iel][face][1][0];
    ntemp3 = sje[iel][face][0][1];
    ntemp4 = sje[iel][face][1][1];

    // local edge 1

    // mor_s_v is the array of mortar indices to  be copied.
    nr_init((int *)mor_s_v, 4 * 2, -1);
    for (i = 1; i < LX1 - 1; i++)
    {
        mor_s_v[0][i - 1] = idmo[ntemp1][jjface[face]][0][0][0][i];
    }
    mor_s_v[0][LX1 - 2] = idmo[ntemp1][jjface[face]][1][0][0][LX1 - 1];
    for (i = 0; i < LX1 - 1; i++)
    {
        mor_s_v[1][i] = idmo[ntemp2][jjface[face]][0][0][0][i];
    }

    // copy mor_s_v to local edge 0 on this face
    mor_s_e(0, face, iel, mor_s_v);

    // copy mor_s_v to the corresponding edge on the other face sharing
    // local edge 0
    face2 = f_e_ef[face][0];
    edge_g = edgenumber[face][0];
    edge_l = localedgenumber[edge_g][face2];
    mor_s_e(edge_l, face2, iel, mor_s_v);

    // local edge 1
    for (i = 1; i < LX1 - 1; i++)
    {
        mor_s_v[0][i - 1] = idmo[ntemp2][jjface[face]][0][0][i][LX1 - 1];
    }
    mor_s_v[0][LX1 - 2] = idmo[ntemp2][jjface[face]][1][1][LX1 - 1][LX1 - 1];

    mor_s_v[1][0] = idmo[ntemp4][jjface[face]][1][0][0][LX1 - 1];
    for (i = 1; i < LX1 - 1; i++)
    {
        mor_s_v[1][i] = idmo[ntemp4][jjface[face]][0][0][i][LX1 - 1];
    }

    mor_s_e(1, face, iel, mor_s_v);
    face2 = f_e_ef[face][1];
    edge_g = edgenumber[face][1];
    edge_l = localedgenumber[edge_g][face2];
    mor_s_e(edge_l, face2, iel, mor_s_v);

    // local edge 2
    for (i = 1; i < LX1 - 1; i++)
    {
        mor_s_v[0][i - 1] = idmo[ntemp3][jjface[face]][0][0][LX1 - 1][i];
    }
    mor_s_v[0][LX1 - 2] = idmo[ntemp3][jjface[face]][1][1][LX1 - 1][LX1 - 1];

    mor_s_v[1][0] = idmo[ntemp4][jjface[face]][0][1][LX1 - 1][0];
    for (i = 1; i < LX1 - 1; i++)
    {
        mor_s_v[1][i] = idmo[ntemp4][jjface[face]][0][0][LX1 - 1][i];
    }

    mor_s_e(2, face, iel, mor_s_v);
    face2 = f_e_ef[face][2];
    edge_g = edgenumber[face][2];
    edge_l = localedgenumber[edge_g][face2];
    mor_s_e(edge_l, face2, iel, mor_s_v);

    // local edge 3
    for (i = 1; i < LX1 - 1; i++)
    {
        mor_s_v[0][i - 1] = idmo[ntemp1][jjface[face]][0][0][i][0];
    }
    mor_s_v[0][LX1 - 2] = idmo[ntemp1][jjface[face]][0][1][LX1 - 1][0];

    for (i = 0; i < LX1 - 1; i++)
    {
        mor_s_v[1][i] = idmo[ntemp3][jjface[face]][0][0][i][0];
    }

    mor_s_e(3, face, iel, mor_s_v);
    face2 = f_e_ef[face][3];
    edge_g = edgenumber[face][3];
    edge_l = localedgenumber[edge_g][face2];
    mor_s_e(edge_l, face2, iel, mor_s_v);
}


//------------------------------------------------------------
// Copy mortar points index from mor_s_v to local edge n
// on face "face" of element iel. The edge is nonconforming.
//------------------------------------------------------------
void mor_s_e(int n, int face, int iel, int mor_s_v[2][4])
{
    int i;

    if (n == 0)
    {
        for (i = 1; i < LX1; i++)
        {
            idmo[iel][face][0][0][0][i] = mor_s_v[0][i - 1];
        }
        for (i = 0; i < LX1 - 1; i++)
        {
            idmo[iel][face][1][0][0][i] = mor_s_v[1][i];
        }
    }
    else if (n == 1)
    {
        for (i = 1; i < LX1; i++)
        {
            idmo[iel][face][1][0][i][LX1 - 1] = mor_s_v[0][i - 1];
        }
        for (i = 0; i < LX1 - 1; i++)
        {
            idmo[iel][face][1][1][i][LX1 - 1] = mor_s_v[1][i];
        }
    }
    else if (n == 2)
    {
        for (i = 1; i < LX1; i++)
        {
            idmo[iel][face][0][1][LX1 - 1][i] = mor_s_v[0][i - 1];
        }
        for (i = 0; i < LX1 - 1; i++)
        {
            idmo[iel][face][1][1][LX1 - 1][i] = mor_s_v[1][i];
        }
    }
    else if (n == 3)
    {
        for (i = 1; i < LX1; i++)
        {
            idmo[iel][face][0][0][i][0] = mor_s_v[0][i - 1];
        }
        for (i = 0; i < LX1 - 1; i++)
        {
            idmo[iel][face][0][1][i][0] = mor_s_v[1][i];
        }
    }
}


//------------------------------------------------------------
// Copy mortar point indices from mor_s_v to local edge n
// on face "face" of element iel. nn is the edge mortar index,
// which indicates that mor_s_v  corresponds to left/bottom or
// right/top part of the edge.
//------------------------------------------------------------
void mor_s_e_nn(int n, int face, int iel, int mor_s_v[4], int nn)
{
    int i;

    if (n == 0)
    {
        if (nn == 0)
        {
            for (i = 1; i < LX1; i++)
            {
                idmo[iel][face][0][0][0][i] = mor_s_v[i - 1];
            }
        }
        else
        {
            for (i = 0; i < LX1 - 1; i++)
            {
                idmo[iel][face][1][0][0][i] = mor_s_v[i];
            }
        }
    }
    else if (n == 1)
    {
        if (nn == 0)
        {
            for (i = 1; i < LX1; i++)
            {
                idmo[iel][face][1][0][i][LX1 - 1] = mor_s_v[i - 1];
            }
        }
        else
        {
            for (i = 0; i < LX1 - 1; i++)
            {
                idmo[iel][face][1][1][i][LX1 - 1] = mor_s_v[i];
            }
        }
    }
    else if (n == 2)
    {
        if (nn == 0)
        {
            for (i = 1; i < LX1; i++)
            {
                idmo[iel][face][0][1][LX1 - 1][i] = mor_s_v[i - 1];
            }
        }
        else
        {
            for (i = 0; i < LX1 - 1; i++)
            {
                idmo[iel][face][1][1][LX1 - 1][i] = mor_s_v[i];
            }
        }
    }
    else if (n == 3)
    {
        if (nn == 0)
        {
            for (i = 1; i < LX1; i++)
            {
                idmo[iel][face][0][0][i][0] = mor_s_v[i - 1];
            }
        }
        else
        {
            for (i = 0; i < LX1 - 1; i++)
            {
                idmo[iel][face][0][1][i][0] = mor_s_v[i];
            }
        }
    }
}


//---------------------------------------------------------------
// Assign mortar point index "count" to iel's i'th vertex
// and also to all elements sharing this vertex.
//---------------------------------------------------------------
void mortar_vertex(int i, int iel, int count)
{
    int ntempx[8], ifntempx[8], lc_a[3], nnb[3];
    int face_a[3], itemp, ntemp, ii, jj, j[3];
    int iintempx[3], l, nbe, lc, temp;
    int if_temp;

    for (l = 0; l < 8; l++)
    {
        ntempx[l] = -1;
        ifntempx[l] = -1;
    }

    // face_a records the three faces sharing this vertex on iel.
    // lc_a gives the local corner number of this vertex on each
    // face in face_a.
    for (l = 0; l < 3; l++)
    {
        face_a[l] = f_c[i][l];
        lc_a[l] = local_corner[face_a[l]][i];
    }

    // each vertex is shared by at most 8 elements.
    // ntempx[j] gives the element index of a POSSIBLE element with its
    // j'th  vertex is iel's i'th vertex
    // ifntempx[i]=ntempx[i] means  ntempx[i] exists
    // ifntempx[i]=-1 means ntempx[i] does not exist.

    ntempx[7 - i] = iel;
    ifntempx[7 - i] = iel;

    // first find all elements sharing this vertex, ifntempx

    // find the three possible neighbors of iel, neighbored by faces
    // listed in array face_a

    for (itemp = 0; itemp < 3; itemp++)
    {
        // j[itemp] is the local corner number of this vertex on the
        // neighbor element on the corresponding face.
        j[itemp] = c_f[jjface[face_a[itemp]]][lc_a[itemp]];

        // iitempx[itemp] records the vertex index of i on the
        // neighbor element, neighborned by face_a[itemp]
        iintempx[itemp] = cal_intempx[face_a[itemp]][lc_a[itemp]];

        // ntemp refers the neighbor element
        ntemp = -1;

        // if the face is nonconforming, find out in which piece of the
        // mortar the vertex is located
        ii = cal_iijj[lc_a[itemp]][0];
        jj = cal_iijj[lc_a[itemp]][1];
        ntemp = sje[iel][face_a[itemp]][jj][ii];

        // if the face is conforming
        if (ntemp == -1)
        {
            ntemp = sje[iel][face_a[itemp]][0][0];
            // find the possible neighbor
            ntempx[iintempx[itemp]] = ntemp;
            // check whether this possible neighbor is a real neighbor or not
            if (ntemp != -1)
            {
                if (ifsame(ntemp, j[itemp], iel, i))
                {
                    ifntempx[iintempx[itemp]] = ntemp;
                }
            }

            // if the face is nonconforming
        }
        else
        {
            if (ntemp != -1)
            {
                if (ifsame(ntemp, j[itemp], iel, i))
                {
                    ifntempx[iintempx[itemp]] = ntemp;
                    ntempx[iintempx[itemp]] = ntemp;
                }
            }
        }
    }

    // find the possible three neighbors, neighbored by an edge only
    for (l = 0; l < 3; l++)
    {
        // find first existing neighbor of any of the faces in array face_a
        if_temp = 0;
        if (l == 0)
        {
            if_temp = 1;
        }
        else if (l == 1)
        {
            if (ifntempx[iintempx[l - 1]] == -1)
            {
                if_temp = 1;
            }
        }
        else if (l == 2)
        {
            if (ifntempx[iintempx[l - 1]] == -1 && ifntempx[iintempx[l - 2]] == -1)
            {
                if_temp = 1;
            }
        }

        if (if_temp)
        {
            if (ifntempx[iintempx[l]] != -1)
            {
                nbe = ifntempx[iintempx[l]];
                // if 1st neighor exists, check the neighbor's two neighbors in
                // the other two directions.
                // e.g. if l=0, check directions 1 and 2,i.e. itemp=1,2,1
                // if l=1, itemp=2,0,-2
                // if l=2, itemp=0,1,1

                itemp = face_l1[l];
                while ((l != 1 && itemp <= face_l2[l]) ||
                        (l == 1 && itemp >= face_l2[l]))
                {
                    //lc is the local corner number of this vertex on face face_a[itemp]
                    // on the neighbor element of iel, neighbored by a face face_a[l]
                    lc = local_corner[face_a[itemp]][j[l]];
                    // temp is the vertex index of this vertex on the neighbor element
                    // neighbored by an edge
                    temp = cal_intempx[face_a[itemp]][lc];
                    ii = cal_iijj[lc][0];
                    jj = cal_iijj[lc][1];
                    ntemp = sje[nbe][face_a[itemp]][jj][ii];

                    // if the face face_a[itemp] is conforming
                    if (ntemp == -1)
                    {
                        ntemp = sje[nbe][face_a[itemp]][0][0];
                        if (ntemp != -1)
                        {
                            if (ifsame(ntemp, c_f[jjface[face_a[itemp]]][lc], nbe, j[l]))
                            {
                                ntempx[temp] = ntemp;
                                ifntempx[temp] = ntemp;
                                // nnb[itemp] records the neighbor element neighbored by an
                                // edge only
                                nnb[itemp] = ntemp;
                            }
                        }

                        // if the face face_a[itemp] is nonconforming
                    }
                    else
                    {
                        if (ntemp != -1)
                        {
                            if (ifsame(ntemp, c_f[jjface[face_a[itemp]]][lc], nbe, j[l]))
                            {
                                ntempx[temp] = ntemp;
                                ifntempx[temp] = ntemp;
                                nnb[itemp] = ntemp;
                            }
                        }
                    }

                    itemp += face_ld[l];
                }

                // check the last neighbor element, neighbored by an edge

                // ifntempx[iintempx[l]] has been visited in the above, now
                // check another neighbor element(nbe) neighbored by a face

                // if the neighbor element is neighbored by face
                // face_a[face_l1[l]] exists
                if (ifntempx[iintempx[face_l1[l]]] != -1)
                {
                    nbe = ifntempx[iintempx[face_l1[l]]];
                    // itemp is the last direction other than l and face_l1[l]
                    itemp = face_l2[l];
                    lc = local_corner[face_a[itemp]][j[face_l1[l]]];
                    temp = cal_intempx[face_a[itemp]][lc];
                    ii = cal_iijj[lc][0];
                    jj = cal_iijj[lc][1];

                    // ntemp records the last neighbor element neighbored by an edge
                    // with element iel
                    ntemp = sje[nbe][face_a[itemp]][jj][ii];
                    // if conforming
                    if (ntemp == -1)
                    {
                        ntemp = sje[nbe][face_a[itemp]][0][0];
                        if (ntemp != -1)
                        {
                            if (ifsame(ntemp, c_f[jjface[face_a[itemp]]][lc], nbe,
                                       j[face_l1[l]]))
                            {
                                ntempx[temp] = ntemp;
                                ifntempx[temp] = ntemp;
                                nnb[l] = ntemp;
                            }
                        }
                        // if nonconforming
                    }
                    else
                    {
                        if (ntemp != -1)
                        {
                            if (ifsame(ntemp, c_f[jjface[face_a[itemp]]][lc], nbe,
                                       j[face_l1[l]]))
                            {
                                ntempx[temp] = ntemp;
                                ifntempx[temp] = ntemp;
                                nnb[l] = ntemp;
                            }
                        }
                    }

                    // if the neighbor element neighbored by face face_a[face_l2[l]]
                    // does not exist
                }
                else if (ifntempx[iintempx[face_l2[l]]] != -1)
                {
                    nbe = ifntempx[iintempx[face_l2[l]]];
                    itemp = face_l1[l];
                    lc = local_corner[face_a[itemp]][j[face_l2[l]]];
                    temp = cal_intempx[face_a[itemp]][lc];
                    ii = cal_iijj[lc][0];
                    jj = cal_iijj[lc][1];
                    ntemp = sje[nbe][face_a[itemp]][jj][ii];
                    if (ntemp == -1)
                    {
                        ntemp = sje[nbe][face_a[itemp]][0][0];
                        if (ntemp != -1)
                        {
                            if (ifsame(ntemp, c_f[jjface[face_a[itemp]]][lc], nbe,
                                       j[face_l2[l]]))
                            {
                                ntempx[temp] = ntemp;
                                ifntempx[temp] = ntemp;
                                nnb[l] = ntemp;
                            }
                        }
                    }
                    else
                    {
                        if (ntemp != -1)
                        {
                            if (ifsame(ntemp, c_f[jjface[face_a[itemp]]][lc], nbe,
                                       j[face_l2[l]]))
                            {
                                ntempx[temp] = ntemp;
                                ifntempx[temp] = ntemp;
                                nnb[l] = ntemp;
                            }
                        }
                    }
                }
            }
        }
    }

    // check the neighbor element, neighbored by a vertex only

    // nnb are the three possible neighbor elements neighbored by an edge

    nnb[0] = ifntempx[cal_nnb[i][0]];
    nnb[1] = ifntempx[cal_nnb[i][1]];
    nnb[2] = ifntempx[cal_nnb[i][2]];
    ntemp = -1;

    // the neighbor element neighbored by a vertex must be a neighbor of
    // a valid(non-negative) nnb[i], neighbored by a face

    if (nnb[0] != -1)
    {
        lc = oplc[local_corner[face_a[2]][i]];
        ii = cal_iijj[lc][0];
        jj = cal_iijj[lc][1];
        // ntemp records the neighbor of iel, neighbored by vertex i
        ntemp = sje[nnb[0]][face_a[2]][jj][ii];
        // temp is the vertex index of i on ntemp
        temp = cal_intempx[face_a[2]][lc];
        if (ntemp == -1)
        {
            ntemp = sje[nnb[0]][face_a[2]][0][0];
            if (ntemp != -1)
            {
                if (ifsame(ntemp, c_f[jjface[face_a[2]]][lc], iel, i))
                {
                    ntempx[temp] = ntemp;
                    ifntempx[temp] = ntemp;
                }
            }
        }
        else
        {
            if (ntemp != -1)
            {
                if (ifsame(ntemp, c_f[jjface[face_a[2]]][lc], iel, i))
                {
                    ntempx[temp] = ntemp;
                    ifntempx[temp] = ntemp;
                }
            }
        }
    }
    else if (nnb[1] != -1)
    {
        lc = oplc[local_corner[face_a[0]][i]];
        ii = cal_iijj[lc][0];
        jj = cal_iijj[lc][1];
        ntemp = sje[nnb[1]][face_a[0]][jj][ii];
        temp = cal_intempx[face_a[0]][lc];
        if (ntemp == -1)
        {
            ntemp = sje[nnb[1]][face_a[0]][0][0];
            if (ntemp != -1)
            {
                if (ifsame(ntemp, c_f[jjface[face_a[0]]][lc], iel, i))
                {
                    ntempx[temp] = ntemp;
                    ifntempx[temp] = ntemp;
                }
            }
        }
        else
        {
            if (ntemp != -1)
            {
                if (ifsame(ntemp, c_f[jjface[face_a[0]]][lc], iel, i))
                {
                    ntempx[temp] = ntemp;
                    ifntempx[temp] = ntemp;
                }
            }
        }
    }
    else if (nnb[2] != -1)
    {
        lc = oplc[local_corner[face_a[1]][i]];
        ii = cal_iijj[lc][0];
        jj = cal_iijj[lc][1];
        ntemp = sje[nnb[2]][face_a[1]][jj][ii];
        temp = cal_intempx[face_a[1]][lc];
        if (ntemp == -1)
        {
            ntemp = sje[nnb[2]][face_a[1]][0][0];
            if (ntemp != -1)
            {
                if (ifsame(ntemp, c_f[jjface[face_a[1]]][lc], iel, i))
                {
                    ifntempx[temp] = ntemp;
                    ntempx[temp] = ntemp;
                }
            }
        }
        else
        {
            if (ntemp != -1)
            {
                if (ifsame(ntemp, c_f[jjface[face_a[1]]][lc], iel, i))
                {
                    ifntempx[temp] = ntemp;
                    ntempx[temp] = ntemp;
                }
            }
        }
    }

    // ifntempx records all elements sharing this vertex, assign count
    // to all these elements.
    if (ifntempx[0] != -1)
    {
        idmo[ntempx[0]][0][1][1][LX1 - 1][LX1 - 1] = count;
        idmo[ntempx[0]][2][1][1][LX1 - 1][LX1 - 1] = count;
        idmo[ntempx[0]][4][1][1][LX1 - 1][LX1 - 1] = count;
        get_emo(ntempx[0], count, 7);
    }

    if (ifntempx[1] != -1)
    {
        idmo[ntempx[1]][1][1][1][LX1 - 1][LX1 - 1] = count;
        idmo[ntempx[1]][2][0][1][LX1 - 1][0] = count;
        idmo[ntempx[1]][4][0][1][LX1 - 1][0] = count;
        get_emo(ntempx[1], count, 6);
    }

    if (ifntempx[2] != -1)
    {
        idmo[ntempx[2]][0][0][1][LX1 - 1][0] = count;
        idmo[ntempx[2]][3][1][1][LX1 - 1][LX1 - 1] = count;
        idmo[ntempx[2]][4][1][0][0][LX1 - 1] = count;
        get_emo(ntempx[2], count, 5);
    }
    if (ifntempx[3] != -1)
    {
        idmo[ntempx[3]][1][0][1][LX1 - 1][0] = count;
        idmo[ntempx[3]][3][0][1][LX1 - 1][0] = count;
        idmo[ntempx[3]][4][0][0][0][0] = count;
        get_emo(ntempx[3], count, 4);
    }

    if (ifntempx[4] != -1)
    {
        idmo[ntempx[4]][0][1][0][0][LX1 - 1] = count;
        idmo[ntempx[4]][2][1][0][0][LX1 - 1] = count;
        idmo[ntempx[4]][5][1][1][LX1 - 1][LX1 - 1] = count;
        get_emo(ntempx[4], count, 3);
    }


    if (ifntempx[5] != -1)
    {
        idmo[ntempx[5]][1][1][0][0][LX1 - 1] = count;
        idmo[ntempx[5]][2][0][0][0][0] = count;
        idmo[ntempx[5]][5][0][1][LX1 - 1][0] = count;
        get_emo(ntempx[5], count, 2);
    }

    if (ifntempx[6] != -1)
    {
        idmo[ntempx[6]][0][0][0][0][0] = count;
        idmo[ntempx[6]][3][1][0][0][LX1 - 1] = count;
        idmo[ntempx[6]][5][1][0][0][LX1 - 1] = count;
        get_emo(ntempx[6], count, 1);
    }

    if (ifntempx[7] != -1)
    {
        idmo[ntempx[7]][1][0][0][0][0] = count;
        idmo[ntempx[7]][3][0][0][0][0] = count;
        idmo[ntempx[7]][5][0][0][0][0] = count;
        get_emo(ntempx[7], count, 0);
    }
}


//---------------------------------------------------------------
// Copy the mortar points index  (mor_v + vertex mortar point) from
// edge'th local edge on face'th face on element ntemp to iel.
// ntemp is iel's neighbor, neighbored by this edge only.
// This subroutine is for the situation that iel is of larger
// size than ntemp.
// face, face2 are face indices
// edge and edge2 are local edge numbers of this edge on face and face2
// nn is edge motar index, which indicate whether this edge
// corresponds to the left/bottom or right/top part of the edge
// on iel.
//---------------------------------------------------------------
void mor_ne(int mor_v[3], int nn, int edge, int face,
            int edge2, int face2, int ntemp, int iel)
{
    int i, mor_s_v[4] = {0,};

    // get mor_s_v which is the mor_v + vertex mortar
    if (edge == 2)
    {
        if (nn == 0)
        {
            for (i = 1; i < LX1 - 1; i++)
            {
                mor_s_v[i - 1] = mor_v[i - 1];
            }
            mor_s_v[3] = idmo[ntemp][face][1][1][LX1 - 1][LX1 - 1];
        }
        else
        {
            mor_s_v[0] = idmo[ntemp][face][0][1][LX1 - 1][0];
            for (i = 1; i < LX1 - 1; i++)
            {
                mor_s_v[i] = mor_v[i - 1];
            }
        }

    }
    else if (edge == 3)
    {
        if (nn == 0)
        {
            for (i = 1; i < LX1 - 1; i++)
            {
                mor_s_v[i - 1] = mor_v[i - 1];
            }
            mor_s_v[3] = idmo[ntemp][face][0][1][LX1 - 1][0];
        }
        else
        {
            mor_s_v[0] = idmo[ntemp][face][0][0][0][0];
            for (i = 1; i < LX1 - 1; i++)
            {
                mor_s_v[i] = mor_v[i - 1];
            }
        }

    }
    else if (edge == 0)
    {
        if (nn == 0)
        {
            for (i = 1; i < LX1 - 1; i++)
            {
                mor_s_v[i - 1] = mor_v[i - 1];
            }
            mor_s_v[3] = idmo[ntemp][face][1][0][0][LX1 - 1];
        }
        else
        {
            mor_s_v[0] = idmo[ntemp][face][0][0][0][0];
            for (i = 1; i < LX1 - 1; i++)
            {
                mor_s_v[i] = mor_v[i - 1];
            }
        }

    }
    else if (edge == 1)
    {
        if (nn == 0)
        {
            for (i = 1; i < LX1 - 1; i++)
            {
                mor_s_v[i - 1] = mor_v[i - 1];
            }
            mor_s_v[3] = idmo[ntemp][face][1][1][LX1 - 1][LX1 - 1];
        }
        else
        {
            mor_s_v[0] = idmo[ntemp][face][1][0][0][LX1 - 1];
            for (i = 1; i < LX1 - 1; i++)
            {
                mor_s_v[i] = mor_v[i - 1];
            }
        }
    }

    // copy mor_s_v to iel's local edge(op[edge]), on face jjface[face]
    mor_s_e_nn(op[edge], jjface[face], iel, mor_s_v, nn);
    // copy mor_s_v to iel's local edge(op[edge2]),  on face jjface[face2]
    // since this edge is shared by two faces on iel
    mor_s_e_nn(op[edge2], jjface[face2], iel, mor_s_v, nn);
}


//---------------------------------------------------------------
// move element to proper location in morton space filling curve
//---------------------------------------------------------------
void move()
{
    int i, iside, jface, iel, ntemp, ii1, ii2, n1, n2, cb;

    n2 = 2 * 6 * nelt;
    n1 = n2 * 2;
    nr_init((int *)sje_new, n1, -1);
    nr_init((int *)ijel_new, n2, -1);


    for (iel = 0; iel < nelt; iel++)
    {
        i = mt_to_id[iel];
        treenew[iel] = tree[i];
        copy(xc_new[iel], xc[i], 8);
        copy(yc_new[iel], yc[i], 8);
        copy(zc_new[iel], zc[i], 8);

        for (iside = 0; iside < NSIDES; iside++)
        {
            jface = jjface[iside];
            cb = cbc[i][iside];
            xc_new[iel][iside] = xc[i][iside];
            yc_new[iel][iside] = yc[i][iside];
            zc_new[iel][iside] = zc[i][iside];
            cbc_new[iel][iside] = cb;

            if (cb == 2)
            {
                ntemp = sje[i][iside][0][0];
                ijel_new[iel][iside][0] = 0;
                ijel_new[iel][iside][1] = 0;
                sje_new[iel][iside][0][0] = id_to_mt[ntemp];

            }
            else if (cb == 1)
            {
                ntemp = sje[i][iside][0][0];
                ijel_new[iel][iside][0] = ijel[i][iside][0];
                ijel_new[iel][iside][1] = ijel[i][iside][1];
                sje_new[iel][iside][0][0] = id_to_mt[ntemp];

            }
            else if (cb == 3)
            {
                for (ii2 = 0; ii2 < 2; ii2++)
                {
                    for (ii1 = 0; ii1 < 2; ii1++)
                    {
                        ntemp = sje[i][iside][ii2][ii1];
                        ijel_new[iel][iside][0] = 0;
                        ijel_new[iel][iside][1] = 0;
                        sje_new[iel][iside][ii2][ii1] = id_to_mt[ntemp];
                    }
                }

            }
            else if (cb == 0)
            {
                sje_new[iel][iside][0][0] = -1;
                sje_new[iel][iside][1][0] = -1;
                sje_new[iel][iside][0][1] = -1;
                sje_new[iel][iside][1][1] = -1;
            }
        }

        copy(ta2[iel][0][0], ta1[i][0][0], NXYZ);
    }

    copy((double *)xc, (double *)xc_new, 8 * nelt);
    copy((double *)yc, (double *)yc_new, 8 * nelt);
    copy((double *)zc, (double *)zc_new, 8 * nelt);
    ncopy((int *)sje, (int *)sje_new, 4 * 6 * nelt);
    ncopy((int *)ijel, (int *)ijel_new, 2 * 6 * nelt);
    ncopy((int *)cbc, (int *)cbc_new, 6 * nelt);
    ncopy((int *)tree, (int *)treenew, nelt);
    copy((double *)ta1, (double *)ta2, NXYZ * nelt);


    for (iel = 0; iel < nelt; iel++)
    {
        mt_to_id[iel] = iel;
        id_to_mt[iel] = iel;
    }
}

//------------------------------------------------------------------
// Generate diagonal preconditioner for CG.
// Preconditioner computed in this subroutine is correct only
// for collocation point in element interior, on conforming face
// interior and conforming edge.
//------------------------------------------------------------------
void setuppc()
{
    double dxtm1_2[LX1][LX1], rdtime;
    int ie, k, i, j, q, isize;

    for (j = 0; j < LX1; j++)
    {
        for (i = 0; i < LX1; i++)
        {
            dxtm1_2[j][i] = dxtm1[j][i] * dxtm1[j][i];
        }
    }

    rdtime = 1.0 / dtime;


    for (ie = 0; ie < nelt; ie++)
    {
        r_init(dpcelm[ie][0][0], NXYZ, 0.0);
        isize = size_e[ie];
        for (k = 0; k < LX1; k++)
        {
            for (j = 0; j < LX1; j++)
            {
                for (i = 0; i < LX1; i++)
                {
                    for (q = 0; q < LX1; q++)
                    {
                        dpcelm[ie][k][j][i] = dpcelm[ie][k][j][i] +
                                              g1m1_s[isize][k][j][q] * dxtm1_2[q][i] +
                                              g1m1_s[isize][k][q][i] * dxtm1_2[q][j] +
                                              g1m1_s[isize][q][j][i] * dxtm1_2[q][k];
                    }
                    dpcelm[ie][k][j][i] = VISC * dpcelm[ie][k][j][i] +
                                          rdtime * bm1_s[isize][k][j][i];
                }
            }
        }
    }

    // do the stiffness summation
    dssum();

    // take inverse.
    reciprocal((double *)dpcelm, ntot);

    // compute preconditioner on mortar points. NOTE:  dpcmor for
    // nonconforming cases will be corrected in subroutine setpcmo


    for (i = 0; i < nmor; i++)
    {
        dpcmor[i] = 1.0 / dpcmor[i];
    }
}


//--------------------------------------------------------------
// pre-compute elemental contribution to preconditioner
// for all situations
//--------------------------------------------------------------
void setpcmo_pre()
{
    int element_size, i, j, ii, jj, col;
    double p[LX1][LX1][LX1], p0[LX1][LX1][LX1], mtemp[LX1][LX1];
    double temp[LX1][LX1][LX1], temp1[LX1][LX1], tmp[LX1][LX1], tig[LX1];

    // corners on face of type 3
    r_init((double *)tcpre, LX1 * LX1, 0.0);
    r_init((double *)tmp, LX1 * LX1, 0.0);
    r_init(tig, 5, 0.0);
    tig[0]    = 1.0;
    tmp[0][0] = 1.0;

    // tcpre results from mapping a unit spike field (unity at
    // collocation point (0,0), zero elsewhere) on an entire element
    // face to the (0,0) segment of a nonconforming face
    for (i = 1; i < LX1 - 1; i++)
    {
        for (j = 0; j < LX1; j++)
        {
            tmp[0][i] = tmp[0][i] + qbnew[0][j][i - 1] * tig[j];
        }
    }

    for (col = 0; col < LX1; col++)
    {
        tcpre[0][col] = tmp[0][col];

        for (j = 1; j < LX1 - 1; j++)
        {
            for (i = 0; i < LX1; i++)
            {
                tcpre[j][col] = tcpre[j][col] + qbnew[0][i][j - 1] * tmp[i][col];
            }
        }
    }

    for (element_size = 0; element_size < REFINE_MAX; element_size++)
    {
        // for conforming cases

        // pcmor_c[element_size][j][i] records the intermediate value
        // (preconditioner=1/pcmor_c) of the preconditor on collocation
        // point (i,j) on a conforming face of an element of size
        // element_size.

        for (j = 0; j < LX1 / 2 + 1; j++)
        {
            for (i = j; i < LX1 / 2 + 1; i++)
            {
                r_init((double *)p, NXYZ, 0.0);
                p[0][j][i] = 1.0;
                laplacian(temp, p, element_size);
                pcmor_c[element_size][j][i]             = temp[0][j][i];
                pcmor_c[element_size][j][LX1 - 1 - i]       = temp[0][j][i];
                pcmor_c[element_size][i][j]             = temp[0][j][i];
                pcmor_c[element_size][i][LX1 - 1 - j]       = temp[0][j][i];
                pcmor_c[element_size][LX1 - 1 - i][j]       = temp[0][j][i];
                pcmor_c[element_size][LX1 - 1 - i][LX1 - 1 - j] = temp[0][j][i];
                pcmor_c[element_size][LX1 - 1 - j][i]       = temp[0][j][i];
                pcmor_c[element_size][LX1 - 1 - j][LX1 - 1 - i] = temp[0][j][i];
            }
        }

        // for nonconforming cases

        // nonconforming face interior

        // pcmor_nc1[element_size][jj][ii][j][i] records the intermediate
        // preconditioner value on collocation point (i,j) on mortar
        // (ii,jj)  on a nonconforming face of an element of size element_
        // size
        for (j = 1; j < LX1; j++)
        {
            for (i = j; i < LX1; i++)
            {
                r_init((double *)mtemp, LX1 * LX1, 0.0);
                r_init((double *)p, NXYZ, 0.0);
                mtemp[j][i] = 1.0;
                // when i, j=LX1-1, mortar points are duplicated, so mtemp needs
                // to be doubled.
                if (i == (LX1 - 1)) mtemp[j][i] = mtemp[j][i] * 2.0;
                if (j == (LX1 - 1)) mtemp[j][i] = mtemp[j][i] * 2.0;
                transf_nc(mtemp, (double (*)[LX1])p);
                laplacian(temp, p, element_size);
                transfb_nc1(temp1, (double (*)[LX1])temp);

                // values at points (i,j) and (j,i) are the same
                pcmor_nc1[element_size][0][0][j][i] = temp1[j][i];
                pcmor_nc1[element_size][0][0][i][j] = temp1[j][i];
            }

            // when i, j=LX1-1, mortar points are duplicated. so pcmor_nc1 needs
            // to be doubled on those points
            pcmor_nc1[element_size][0][0][j][LX1 - 1] =
                pcmor_nc1[element_size][0][0][j][LX1 - 1] * 2.0;
            pcmor_nc1[element_size][0][0][LX1 - 1][j] =
                pcmor_nc1[element_size][0][0][j][LX1 - 1];
        }
        pcmor_nc1[element_size][0][0][LX1 - 1][LX1 - 1] =
            pcmor_nc1[element_size][0][0][LX1 - 1][LX1 - 1] * 2.0;

        // nonconforming edges
        j = 0;
        for (i = 1; i < LX1; i++)
        {
            r_init((double *)mtemp, LX1 * LX1, 0.0);
            r_init((double *)p, NXYZ, 0.0);
            r_init((double *)p0, NXYZ, 0.0);
            mtemp[j][i] = 1.0;
            if (i == (LX1 - 1)) mtemp[j][i] = 2.0;
            transf_nc(mtemp, (double (*)[LX1])p);
            laplacian(temp, p, element_size);
            transfb_nc1(temp1, (double (*)[LX1])temp);
            pcmor_nc1[element_size][0][0][j][i] = temp1[j][i];
            pcmor_nc1[element_size][0][0][i][j] = temp1[j][i];

            for (ii = 0; ii < LX1; ii++)
            {
                // p0 is for the case that a nonconforming edge is shared by
                // two conforming faces
                p0[0][0][ii] = p[0][0][ii];
                for (jj = 0; jj < LX1; jj++)
                {
                    // now p is for the case that a nonconforming edge is shared
                    // by nonconforming faces
                    p[jj][0][ii] = p[0][jj][ii];
                }
            }

            laplacian(temp, p, element_size);
            transfb_nc2(temp1, (double (*)[LX1])temp);

            // pcmor_nc2[element_size][jj][ii][j][i] gives the intermediate
            // preconditioner value on collocation point (i,j) on a
            // nonconforming face of an element with size size_element

            pcmor_nc2[element_size][0][0][j][i] = temp1[j][i] * 2.0 ;
            pcmor_nc2[element_size][0][0][i][j] =
                pcmor_nc2[element_size][0][0][j][i];

            laplacian(temp, p0, element_size);
            transfb_nc0(temp1, temp);

            // pcmor_nc0[element_size][jj][ii][j][i] gives the intermediate
            // preconditioner value on collocation point (i,j) on a
            // conforming face of an element, which shares a nonconforming
            // edge with another conforming face
            pcmor_nc0[element_size][0][0][j][i] = temp1[j][i];
            pcmor_nc0[element_size][0][0][i][j] = temp1[j][i];
        }
        pcmor_nc1[element_size][0][0][j][LX1 - 1] =
            pcmor_nc1[element_size][0][0][j][LX1 - 1] * 2.0;
        pcmor_nc1[element_size][0][0][LX1 - 1][j] =
            pcmor_nc1[element_size][0][0][j][LX1 - 1];
        pcmor_nc2[element_size][0][0][j][LX1 - 1] =
            pcmor_nc2[element_size][0][0][j][LX1 - 1] * 2.0;
        pcmor_nc2[element_size][0][0][LX1 - 1][j] =
            pcmor_nc2[element_size][0][0][j][LX1 - 1];
        pcmor_nc0[element_size][0][0][j][LX1 - 1] =
            pcmor_nc0[element_size][0][0][j][LX1 - 1] * 2.0;
        pcmor_nc0[element_size][0][0][LX1 - 1][j] =
            pcmor_nc0[element_size][0][0][j][LX1 - 1];

        // symmetrical copy
        for (i = 0; i < LX1 - 1; i++)
        {
            pcmor_nc1[element_size][1][0][j][i] =
                pcmor_nc1[element_size][0][0][j][LX1 - 1 - i];
            pcmor_nc0[element_size][1][0][j][i] =
                pcmor_nc0[element_size][0][0][j][LX1 - 1 - i];
            pcmor_nc2[element_size][1][0][j][i] =
                pcmor_nc2[element_size][0][0][j][LX1 - 1 - i];
        }

        for (j = 1; j < LX1; j++)
        {
            for (i = 0; i < LX1 - 1; i++)
            {
                pcmor_nc1[element_size][1][0][j][i] =
                    pcmor_nc1[element_size][0][0][j][LX1 - 1 - i];
            }
            i = LX1 - 1;
            pcmor_nc1[element_size][1][0][j][i] =
                pcmor_nc1[element_size][0][0][j][LX1 - 1 - i];
            pcmor_nc0[element_size][1][0][j][i] =
                pcmor_nc0[element_size][0][0][j][LX1 - 1 - i];
            pcmor_nc2[element_size][1][0][j][i] =
                pcmor_nc2[element_size][0][0][j][LX1 - 1 - i];
        }

        j = 0;
        i = 0;
        pcmor_nc1[element_size][0][1][j][i] =
            pcmor_nc1[element_size][0][0][LX1 - 1 - j][i];
        pcmor_nc0[element_size][0][1][j][i] =
            pcmor_nc0[element_size][0][0][LX1 - 1 - j][i];
        pcmor_nc2[element_size][0][1][j][i] =
            pcmor_nc2[element_size][0][0][LX1 - 1 - j][i];
        for (j = 1; j < LX1 - 1; j++)
        {
            i = 0;
            pcmor_nc1[element_size][0][1][j][i] =
                pcmor_nc1[element_size][0][0][LX1 - 1 - j][i];
            pcmor_nc0[element_size][0][1][j][i] =
                pcmor_nc0[element_size][0][0][LX1 - 1 - j][i];
            pcmor_nc2[element_size][0][1][j][i] =
                pcmor_nc2[element_size][0][0][LX1 - 1 - j][i];
            for (i = 1; i < LX1; i++)
            {
                pcmor_nc1[element_size][0][1][j][i] =
                    pcmor_nc1[element_size][0][0][LX1 - 1 - j][i];
            }
        }

        j = LX1 - 1;
        for (i = 1; i < LX1; i++)
        {
            pcmor_nc1[element_size][0][1][j][i] =
                pcmor_nc1[element_size][0][0][LX1 - 1 - j][i];
            pcmor_nc0[element_size][0][1][j][i] =
                pcmor_nc0[element_size][0][0][LX1 - 1 - j][i];
            pcmor_nc2[element_size][0][1][j][i] =
                pcmor_nc2[element_size][0][0][LX1 - 1 - j][i];
        }

        j = 0;
        i = LX1 - 1;
        pcmor_nc1[element_size][1][1][j][i] =
            pcmor_nc1[element_size][0][0][LX1 - 1 - j][LX1 - 1 - i];
        pcmor_nc0[element_size][1][1][j][i] =
            pcmor_nc0[element_size][0][0][LX1 - 1 - j][LX1 - 1 - i];
        pcmor_nc2[element_size][1][1][j][i] =
            pcmor_nc2[element_size][0][0][LX1 - 1 - j][LX1 - 1 - i];

        for (j = 1; j < LX1 - 1; j++)
        {
            for (i = 1; i < LX1 - 1; i++)
            {
                pcmor_nc1[element_size][1][1][j][i] =
                    pcmor_nc1[element_size][0][0][LX1 - 1 - j][LX1 - 1 - i];
            }
            i = LX1 - 1;
            pcmor_nc1[element_size][1][1][j][i] =
                pcmor_nc1[element_size][0][0][LX1 - 1 - j][LX1 - 1 - i];
            pcmor_nc0[element_size][1][1][j][i] =
                pcmor_nc0[element_size][0][0][LX1 - 1 - j][LX1 - 1 - i];
            pcmor_nc2[element_size][1][1][j][i] =
                pcmor_nc2[element_size][0][0][LX1 - 1 - j][LX1 - 1 - i];
        }
        j = LX1 - 1;
        for (i = 1; i < LX1 - 1; i++)
        {
            pcmor_nc1[element_size][1][1][j][i] =
                pcmor_nc1[element_size][0][0][LX1 - 1 - j][LX1 - 1 - i];
            pcmor_nc0[element_size][1][1][j][i] =
                pcmor_nc0[element_size][0][0][LX1 - 1 - j][LX1 - 1 - i];
            pcmor_nc2[element_size][1][1][j][i] =
                pcmor_nc2[element_size][0][0][LX1 - 1 - j][LX1 - 1 - i];
        }


        // vertices shared by at least one nonconforming face or edge

        // Among three edges and three faces sharing a vertex on an element
        // situation 1: only one edge is nonconforming
        // situation 2: two edges are nonconforming
        // situation 3: three edges are nonconforming
        // situation 4: one face is nonconforming
        // situation 5: one face and one edge are nonconforming
        // situation 6: two faces are nonconforming
        // situation 7: three faces are nonconforming

        r_init((double *)p0, NXYZ, 0.0);
        p0[0][0][0] = 1.0;
        laplacian(temp, p0, element_size);
        pcmor_cor[element_size][7] = temp[0][0][0];

        // situation 1
        r_init((double *)p0, NXYZ, 0.0);
        for (i = 0; i < LX1; i++)
        {
            p0[0][0][i] = tcpre[0][i];
        }
        laplacian(temp, p0, element_size);
        transfb_cor_e(1, &pcmor_cor[element_size][0], temp);

        // situation 2
        r_init((double *)p0, NXYZ, 0.0);
        for (i = 0; i < LX1; i++)
        {
            p0[0][0][i] = tcpre[0][i];
            p0[0][i][0] = tcpre[0][i];
        }
        laplacian(temp, p0, element_size);
        transfb_cor_e(2, &pcmor_cor[element_size][1], temp);

        // situation 3
        r_init((double *)p0, NXYZ, 0.0);
        for (i = 0; i < LX1; i++)
        {
            p0[0][0][i] = tcpre[0][i];
            p0[0][i][0] = tcpre[0][i];
            p0[i][0][0] = tcpre[0][i];
        }
        laplacian(temp, p0, element_size);
        transfb_cor_e(3, &pcmor_cor[element_size][2], temp);

        // situation 4
        r_init((double *)p0, NXYZ, 0.0);
        for (j = 0; j < LX1; j++)
        {
            for (i = 0; i < LX1; i++)
            {
                p0[0][j][i] = tcpre[j][i];
            }
        }
        laplacian(temp, p0, element_size);
        transfb_cor_f(4, &pcmor_cor[element_size][3], temp);

        // situation 5
        r_init((double *)p0, NXYZ, 0.0);
        for (j = 0; j < LX1; j++)
        {
            for (i = 0; i < LX1; i++)
            {
                p0[0][j][i] = tcpre[j][i];
            }
        }
        for (i = 0; i < LX1; i++)
        {
            p0[i][0][0] = tcpre[0][i];
        }
        laplacian(temp, p0, element_size);
        transfb_cor_f(5, &pcmor_cor[element_size][4], temp);

        // situation 6
        r_init((double *)p0, NXYZ, 0.0);
        for (j = 0; j < LX1; j++)
        {
            for (i = 0; i < LX1; i++)
            {
                p0[0][j][i] = tcpre[j][i];
                p0[j][0][i] = tcpre[j][i];
            }
        }
        laplacian(temp, p0, element_size);
        transfb_cor_f(6, &pcmor_cor[element_size][5], temp);

        // situation 7
        for (j = 0; j < LX1; j++)
        {
            for (i = 0; i < LX1; i++)
            {
                p0[0][j][i] = tcpre[j][i];
                p0[j][0][i] = tcpre[j][i];
                p0[j][i][0] = tcpre[j][i];
            }
        }
        laplacian(temp, p0, element_size);
        transfb_cor_f(7, &pcmor_cor[element_size][6], temp);
    }
}


//------------------------------------------------------------------------
// compute the preconditioner by identifying its geometry configuration
// and sum the values from the precomputed elemental contributions
//------------------------------------------------------------------------
void setpcmo()
{
    int face2, nb1, nb2, sizei, imor, _enum, i, j, iel, iside, nn1, nn2;

    l_init(ifpcmor, nvertex, 0);
    l_init((int *)edgevis, 24 * nelt, 0);

    for (iel = 0; iel < nelt; iel++)
    {
        for (iside = 0; iside < NSIDES; iside++)
        {
            // for nonconforming faces
            if (cbc[iel][iside] == 3)
            {
                sizei = size_e[iel];

                // vertices

                // ifpcmor[imor] = 1 indicates that mortar point imor has
                // been visited
                imor = idmo[iel][iside][0][0][0][0];
                if (!ifpcmor[imor])
                {
                    // compute the preconditioner on mortar point imor
                    pc_corner(imor);
                    ifpcmor[imor] = 1;
                }

                imor = idmo[iel][iside][1][0][0][LX1 - 1];
                if (!ifpcmor[imor])
                {
                    pc_corner(imor);
                    ifpcmor[imor] = 1;
                }

                imor = idmo[iel][iside][0][1][LX1 - 1][0];
                if (!ifpcmor[imor])
                {
                    pc_corner(imor);
                    ifpcmor[imor] = 1;
                }

                imor = idmo[iel][iside][1][1][LX1 - 1][LX1 - 1];
                if (!ifpcmor[imor])
                {
                    pc_corner(imor);
                    ifpcmor[imor] = 1;
                }

                // edges on nonconforming faces, _enum is local edge number
                for (_enum = 0; _enum < 4; _enum++)
                {
                    // edgevis[iel][iside][_enum]=1 indicates that local edge
                    // _enum of face iside of iel has been visited
                    if (!edgevis[iel][iside][_enum])
                    {
                        edgevis[iel][iside][_enum] = 1;

                        // Examing neighbor element information,
                        // calculateing the preconditioner value.
                        face2 = f_e_ef[iside][_enum];
                        if (cbc[iel][face2] == 2)
                        {
                            nb1 = sje[iel][face2][0][0];
                            if (cbc[nb1][iside] == 2)
                            {

                                // Compute the preconditioner on local edge _enum on face
                                // iside of element iel, 1 is neighborhood information got
                                // by examing neighbors(nb1). For detailed meaning of 1,
                                // see subroutine com_dpc.

                                com_dpc(iside, iel, _enum, 1, sizei);
                                nb2 = sje[nb1][iside][0][0];
                                edgevis[nb2][jjface[face2]][op[e_face2[iside][_enum]]] = 1;

                            }
                            else if (cbc[nb1][iside] == 3)
                            {
                                com_dpc(iside, iel, _enum, 2, sizei);
                                edgevis[nb1][iside][op[_enum]] = 1;
                            }

                        }
                        else if (cbc[iel][face2] == 3)
                        {
                            edgevis[iel][face2][e_face2[iside][_enum]] = 1;
                            nb1 = sje[iel][face2][1][0];
                            if (cbc[nb1][iside] == 1)
                            {
                                com_dpc(iside, iel, _enum, 3, sizei);
                                nb2 = sje[nb1][iside][0][0];
                                edgevis[nb2][jjface[iside]][op[_enum]] = 1;
                                edgevis[nb2][jjface[face2]][op[e_face2[iside][_enum]]] = 1;
                            }
                            else if (cbc[nb1][iside] == 2)
                            {
                                com_dpc(iside, iel, _enum, 4, sizei);
                            }
                        }
                        else if (cbc[iel][face2] == 0)
                        {
                            com_dpc(iside, iel, _enum, 0, sizei);
                        }
                    }
                }

                // mortar element interior (not edge of mortar)
                for (nn1 = 0; nn1 < 2; nn1++)
                {
                    for (nn2 = 0; nn2 < 2; nn2++)
                    {
                        for (j = 1; j < LX1 - 1; j++)
                        {
                            for (i = 1; i < LX1 - 1; i++)
                            {
                                imor = idmo[iel][iside][nn2][nn1][j][i];
                                dpcmor[imor] = 1.0 / (pcmor_nc1[sizei][nn2][nn1][j][i] +
                                                      pcmor_c[sizei + 1][j][i]);
                            }
                        }
                    }
                }

                // for i,j=LX1-1 there are duplicated mortar points, so
                // pcmor_c needs to be doubled or quadrupled
                i = LX1 - 1;
                for (j = 1; j < LX1 - 1; j++)
                {
                    imor = idmo[iel][iside][0][0][j][i];
                    dpcmor[imor] = 1.0 / (pcmor_nc1[sizei][0][0][j][i] +
                                          pcmor_c[sizei + 1][j][i] * 2.0);
                    imor = idmo[iel][iside][0][1][j][i];
                    dpcmor[imor] = 1.0 / (pcmor_nc1[sizei][0][1][j][i] +
                                          pcmor_c[sizei + 1][j][i] * 2.0);
                }

                j = LX1 - 1;
                imor = idmo[iel][iside][0][0][j][i];
                dpcmor[imor] = 1.0 / (pcmor_nc1[sizei][0][0][j][i] +
                                      pcmor_c[sizei + 1][j][i] * 4.0);
                for (i = 1; i < LX1 - 1; i++)
                {
                    imor = idmo[iel][iside][0][0][j][i];
                    dpcmor[imor] = 1.0 / (pcmor_nc1[sizei][0][0][j][i] +
                                          pcmor_c[sizei + 1][j][i] * 2.0);
                    imor = idmo[iel][iside][1][0][j][i];
                    dpcmor[imor] = 1.0 / (pcmor_nc1[sizei][1][0][j][i] +
                                          pcmor_c[sizei + 1][j][i] * 2.0);
                }
            }
        }
    }
}


//------------------------------------------------------------------------
// calculate preconditioner value for vertex with mortar index imor
//------------------------------------------------------------------------
void pc_corner(int imor)
{
    double tmortemp;
    int inemo, ie, sizei, cornernumber;
    int sface, sedge, iiface, iface, iiedge, iedge, n = 0;

    tmortemp = 0.0;
    // loop over all elements sharing this vertex


    for (inemo = 0; inemo <= nemo[imor]; inemo++)
    {
        ie = emo[imor][inemo][0];
        sizei = size_e[ie];
        cornernumber = emo[imor][inemo][1];
        sface = 0;
        sedge = 0;
        for (iiface = 0; iiface < 3; iiface++)
        {
            iface = f_c[cornernumber][iiface];
            // sface sums the number of nonconforming faces sharing this vertex on
            // one element
            if (cbc[ie][iface] == 3)
            {
                sface = sface + 1;
            }
        }
        // sedge sums the number of nonconforming edges sharing this vertex on
        // one element
        for (iiedge = 0; iiedge < 3; iiedge++)
        {
            iedge = e_c[cornernumber][iiedge];
            if (ncon_edge[ie][iedge]) sedge = sedge + 1;
        }

        // each n indicates how many nonconforming faces and nonconforming
        // edges share this vertex on an element,

        if (sface == 0)
        {
            if (sedge == 0)
            {
                n = 7;
            }
            else if (sedge == 1)
            {
                n = 0;
            }
            else if (sedge == 2)
            {
                n = 1;
            }
            else if (sedge == 3)
            {
                n = 2;
            }
        }
        else if (sface == 1)
        {
            if (sedge == 1)
            {
                n = 4;
            }
            else
            {
                n = 3;
            }
        }
        else if (sface == 2)
        {
            n = 5;
        }
        else if (sface == 3)
        {
            n = 6;
        }

        // sum the intermediate pre-computed preconditioner values for
        // all elements
        tmortemp = tmortemp + pcmor_cor[sizei][n];
    }

    // dpcmor[imor] is the value of the preconditioner on mortar point imor
    dpcmor[imor] = 1.0 / tmortemp;
}


//------------------------------------------------------------------------
// Compute preconditioner for local edge enumber of face iside
// on element iel.
// isize is element size,
// n is one of five different configurations
// anc1, ac, anc2, anc0 are coefficients for different edges.
// nc0 refers to nonconforming edge shared by two conforming faces
// nc1 refers to nonconforming edge shared by one nonconforming face
// nc2 refers to nonconforming edges shared by two nonconforming faces
// c refers to conforming edge
//------------------------------------------------------------------------
void com_dpc(int iside, int iel, int enumber, int n, int isize)
{
    int nn1start, nn1end, nn2start;
    int nn2end, jstart, jend, istart, iend, i, j, nn1, nn2, imor = 0;
    double anc1, ac, anc2, anc0, temp = 0.0;

    // different local edges have different loop ranges
    if (enumber == 0)
    {
        nn1start = 1;
        nn1end = 1;
        nn2start = 1;
        nn2end = 2;
        jstart = 1;
        jend = 1;
        istart = 2;
        iend = LX1 - 1;
    }
    else if (enumber == 1)
    {
        nn1start = 1;
        nn1end = 2;
        nn2start = 2;
        nn2end = 2;
        jstart = 2;
        jend = LX1 - 1;
        istart = LX1;
        iend = LX1;
    }
    else if (enumber == 2)
    {
        nn1start = 2;
        nn1end = 2;
        nn2start = 1;
        nn2end = 2;
        jstart = LX1;
        jend = LX1;
        istart = 2;
        iend = LX1 - 1;
    }
    else if (enumber == 3)
    {
        nn1start = 1;
        nn1end = 2;
        nn2start = 1;
        nn2end = 1;
        jstart = 2;
        jend = LX1 - 1;
        istart = 1;
        iend = 1;
    }
    else
    {
        // MUST NOT reachable!!
		// Commenting assert to make tests portable, macro hard-codes file location
        //assert(0);
        nn1start = 0;
        nn1end = 0;
        nn2start = 0;
        nn2end = 0;
        jstart = 0;
        jend = LX1 - 1;
        istart = 0;
        iend = 0;
    }

    // among the four elements sharing this edge

    // one has a smaller size
    if (n == 1)
    {
        anc1 = 2.0;
        ac = 1.0;
        anc0 = 1.0;
        anc2 = 0.0;

        // two (neighbored by a face) are of  smaller size
    }
    else if (n == 2)
    {
        anc1 = 2.0;
        ac = 2.0;
        anc0 = 0.0;
        anc2 = 0.0;

        // two (neighbored by an edge) are of smaller size
    }
    else if (n == 3)
    {
        anc2 = 2.0;
        ac = 2.0;
        anc1 = 0.0;
        anc0 = 0.0;

        // three are of smaller size
    }
    else if (n == 4)
    {
        anc1 = 0.0;
        ac = 3.0;
        anc2 = 1.0;
        anc0 = 0.0;

        // on the boundary
    }
    else if (n == 0)
    {
        anc1 = 1.0;
        ac = 1.0;
        anc2 = 0.0;
        anc0 = 0.0;
    }
    else
    {
        // MUST NOT reachable!!
		// Commenting assert to make tests portable, macro hard-codes file location		
        //assert(0);
        anc1 = 0.0;
        ac = 0.0;
        anc2 = 0.0;
        anc0 = 0.0;
    }

    // edge interior
    for (nn2 = nn2start - 1; nn2 < nn2end; nn2++)
    {
        for (nn1 = nn1start - 1; nn1 < nn1end; nn1++)
        {
            for (j = jstart - 1; j < jend; j++)
            {
                for (i = istart - 1; i < iend; i++)
                {
                    imor = idmo[iel][iside][nn2][nn1][j][i];
                    temp = anc1 * pcmor_nc1[isize][nn2][nn1][j][i] +
                           ac *  pcmor_c[isize + 1][j][i] +
                           anc0 *  pcmor_nc0[isize][nn2][nn1][j][i] +
                           anc2 * pcmor_nc2[isize][nn2][nn1][j][i];
                    dpcmor[imor] = 1.0 / temp;
                }
            }
        }
    }

    // local edge 0
    if (enumber == 0)
    {
        imor = idmo[iel][iside][0][0][0][LX1 - 1];
        temp = anc1 * pcmor_nc1[isize][0][0][0][LX1 - 1] +
               ac  * pcmor_c[isize + 1][0][LX1 - 1] * 2.0 +
               anc0 * pcmor_nc0[isize][0][0][0][LX1 - 1] +
               anc2 * pcmor_nc2[isize][0][0][0][LX1 - 1];
        // local edge 1
    }
    else if (enumber == 1)
    {
        imor = idmo[iel][iside][1][0][LX1 - 1][LX1 - 1];
        temp = anc1 * pcmor_nc1[isize][1][0][LX1 - 1][LX1 - 1] +
               ac  * pcmor_c[isize + 1][LX1 - 1][LX1 - 1] * 2.0 +
               anc0 * pcmor_nc0[isize][1][0][LX1 - 1][LX1 - 1] +
               anc2 * pcmor_nc2[isize][1][0][LX1 - 1][LX1 - 1];
        // local edge 2
    }
    else if (enumber == 2)
    {
        imor = idmo[iel][iside][0][1][LX1 - 1][LX1 - 1];
        temp = anc1 * pcmor_nc1[isize][0][1][LX1 - 1][LX1 - 1] +
               ac  * pcmor_c[isize + 1][LX1 - 1][LX1 - 1] * 2.0 +
               anc0 * pcmor_nc0[isize][0][1][LX1 - 1][LX1 - 1] +
               anc2 * pcmor_nc2[isize][0][1][LX1 - 1][LX1 - 1];
        // local edge 3
    }
    else if (enumber == 3)
    {
        imor = idmo[iel][iside][0][0][LX1 - 1][0];
        temp = anc1 * pcmor_nc1[isize][0][0][LX1 - 1][0] +
               ac  * pcmor_c[isize + 1][LX1 - 1][0] * 2.0 +
               anc0 * pcmor_nc0[isize][0][0][LX1 - 1][0] +
               anc2 * pcmor_nc2[isize][0][0][LX1 - 1][0];
    }

    dpcmor[imor] = 1.0 / temp;
}


void create_initial_grid()
{
    int i;

    nelt = 1;
    ntot = nelt * LX1 * LX1 * LX1;
    tree[0] = 1;
    mt_to_id[0] = 0;
    for (i = 0; i < 7; i += 2)
    {
        xc[0][i] = 0.0;
        xc[0][i + 1] = 1.0;
    }

    for (i = 0; i < 2; i++)
    {
        yc[0][i] = 0.0;
        yc[0][2 + i] = 1.0;
        yc[0][4 + i] = 0.0;
        yc[0][6 + i] = 1.0;
    }

    for (i = 0; i < 4; i++)
    {
        zc[0][i] = 0.0;
        zc[0][4 + i] = 1.0;
    }

    for (i = 0; i < 6; i++)
    {
        cbc[0][i] = 0;
    }
}


//-----------------------------------------------------------------
//
// generate
//
//        - collocation points
//        - weights
//        - derivative matrices
//        - projection matrices
//        - interpolation matrices
//
// associated with the
//
//        - gauss-legendre lobatto mesh (suffix m1)
//
//----------------------------------------------------------------
void coef()
{
    int i, j, k;

    // for gauss-legendre lobatto mesh (suffix m1)
    // generate collocation points and weights
    zgm1[0] = -1.0;
    zgm1[1] = -0.65465367070797710;
    zgm1[2] = 0.0;
    zgm1[3] =  0.65465367070797710;
    zgm1[4] = 1.0;
    wxm1[0] = 0.10;
    wxm1[1] = 49.0 / 90.0;
    wxm1[2] = 32.0 / 45.0;
    wxm1[3] = wxm1[1];
    wxm1[4] = 0.1;

    for (k = 0; k < LX1; k++)
    {
        for (j = 0; j < LX1; j++)
        {
            for (i = 0; i < LX1; i++)
            {
                w3m1[k][j][i] = wxm1[i] * wxm1[j] * wxm1[k];
            }
        }
    }

    // generate derivative matrices
    dxm1[0][0] = -5.0;
    dxm1[0][1] = -1.240990253030982;
    dxm1[0][2] =  0.375;
    dxm1[0][3] = -0.2590097469690172;
    dxm1[0][4] =  0.5;
    dxm1[1][0] =  6.756502488724238;
    dxm1[1][1] =  0.0;
    dxm1[1][2] = -1.336584577695453;
    dxm1[1][3] =  0.7637626158259734;
    dxm1[1][4] = -1.410164177942427;
    dxm1[2][0] = -2.666666666666667;
    dxm1[2][1] =  1.745743121887939;
    dxm1[2][2] =  0.0;
    dxm1[2][3] = -dxm1[2][1];
    dxm1[2][4] = -dxm1[2][0];
    for (j = 3; j < LX1; j++)
    {
        for (i = 0; i < LX1; i++)
        {
            dxm1[j][i] = -dxm1[LX1 - 1 - j][LX1 - 1 - i];
        }
    }
    for (j = 0; j < LX1; j++)
    {
        for (i = 0; i < LX1; i++)
        {
            dxtm1[j][i] = dxm1[i][j];
        }
    }

    // generate projection (mapping) matrices
    qbnew[0][0][0] = -0.1772843218615690;
    qbnew[0][0][1] = 9.375e-02;
    qbnew[0][0][2] = -3.700139242414530e-02;
    qbnew[0][1][0] =  0.7152146412463197;
    qbnew[0][1][1] = -0.2285757930375471;
    qbnew[0][1][2] =  8.333333333333333e-02;
    qbnew[0][2][0] =  0.4398680650316104;
    qbnew[0][2][1] =  0.2083333333333333;
    qbnew[0][2][2] = -5.891568407922938e-02;
    qbnew[0][3][0] =  8.333333333333333e-02;
    qbnew[0][3][1] =  0.3561799597042137;
    qbnew[0][3][2] = -4.854797457965334e-02;
    qbnew[0][4][0] =  0.0;
    qbnew[0][4][1] = 7.03125e-02;
    qbnew[0][4][2] = 0.0;

    for (j = 0; j < LX1; j++)
    {
        for (i = 0; i < 3; i++)
        {
            qbnew[1][j][i] = qbnew[0][LX1 - 1 - j][2 - i];
        }
    }

    // generate interpolation matrices for mesh refinement
    ixtmc1[0][0] = 1.0;
    ixtmc1[0][1] = 0.0;
    ixtmc1[0][2] = 0.0;
    ixtmc1[0][3] = 0.0;
    ixtmc1[0][4] = 0.0;
    ixtmc1[1][0] =  0.3385078435248143;
    ixtmc1[1][1] =  0.7898516348912331;
    ixtmc1[1][2] = -0.1884018684471238;
    ixtmc1[1][3] =  9.202967302175333e-02;
    ixtmc1[1][4] = -3.198728299067715e-02;
    ixtmc1[2][0] = -0.1171875;
    ixtmc1[2][1] =  0.8840317166357952;
    ixtmc1[2][2] =  0.3125;
    ixtmc1[2][3] = -0.118406716635795;
    ixtmc1[2][4] =  0.0390625;
    ixtmc1[3][0] = -7.065070066767144e-02;
    ixtmc1[3][1] =  0.2829703269782467;
    ixtmc1[3][2] =  0.902687582732838;
    ixtmc1[3][3] = -0.1648516348912333;
    ixtmc1[3][4] =  4.984442584781999e-02;
    ixtmc1[4][0] = 0.0;
    ixtmc1[4][1] = 0.0;
    ixtmc1[4][2] = 1.0;
    ixtmc1[4][3] = 0.0;
    ixtmc1[4][4] = 0.0;
    for (j = 0; j < LX1; j++)
    {
        for (i = 0; i < LX1; i++)
        {
            ixmc1[j][i] = ixtmc1[i][j];
        }
    }

    for (j = 0; j < LX1; j++)
    {
        for (i = 0; i < LX1; i++)
        {
            ixtmc2[j][i] = ixtmc1[LX1 - 1 - j][LX1 - 1 - i];
        }
    }

    for (j = 0; j < LX1; j++)
    {
        for (i = 0; i < LX1; i++)
        {
            ixmc2[j][i] = ixtmc2[i][j];
        }
    }

    // solution interpolation matrix for mesh coarsening
    map2[0] = -0.1179652785083428;
    map2[1] =  0.5505046330389332;
    map2[2] =  0.7024534364259963;
    map2[3] = -0.1972224518285866;
    map2[4] =  6.222966087199998e-02;

    for (i = 0; i < LX1; i++)
    {
        map4[i] = map2[LX1 - 1 - i];
    }
}


//-------------------------------------------------------------------
//
// routine to generate elemental geometry information on mesh m1,
// (gauss-legendre lobatto mesh).
//
//     xrm1_s   -   dx/dr, dy/dr, dz/dr
//     rxm1_s   -   dr/dx, dr/dy, dr/dz
//     g1m1_s  geometric factors used in preconditioner computation
//     g4m1_s  g5m1_s  g6m1_s :
//     geometric factors used in lapacian opertor
//     jacm1    -   jacobian
//     bm1      -   mass matrix
//     xfrac    -   will be used in prepwork for calculating collocation
//                      coordinates
//     idel     -   collocation points index on element boundaries
//------------------------------------------------------------------
void geom1()
{
    double temp, temp1, temp2, dtemp;
    int isize, i, j, k, ntemp, iel;

    for (i = 0; i < LX1; i++)
    {
        xfrac[i] = zgm1[i] * 0.5 + 0.5;
    }

    for (isize = 0; isize < REFINE_MAX; isize++)
    {
        temp = pow(2.0, (-isize - 2));
        dtemp = 1.0 / temp;
        temp1 = temp * temp * temp;
        temp2 = temp * temp;
        for (k = 0; k < LX1; k++)
        {
            for (j = 0; j < LX1; j++)
            {
                for (i = 0; i < LX1; i++)
                {
                    xrm1_s[isize][k][j][i] = dtemp;
                    jacm1_s[isize][k][j][i] = temp1;
                    rxm1_s[isize][k][j][i] = temp2;
                    g1m1_s[isize][k][j][i] = w3m1[k][j][i] * temp;
                    bm1_s[isize][k][j][i] = w3m1[k][j][i] * temp1;
                    g4m1_s[isize][k][j][i] = g1m1_s[isize][k][j][i] / wxm1[i];
                    g5m1_s[isize][k][j][i] = g1m1_s[isize][k][j][i] / wxm1[j];
                    g6m1_s[isize][k][j][i] = g1m1_s[isize][k][j][i] / wxm1[k];
                }
            }
        }
    }


    for (iel = 0; iel < LELT; iel++)
    {
        ntemp = LX1 * LX1 * LX1 * iel;
        for (j = 0; j < LX1; j++)
        {
            for (i = 0; i < LX1; i++)
            {
                idel[iel][0][j][i] = ntemp + i * LX1 + j * LX1 * LX1 + LX1 - 1;
                idel[iel][1][j][i] = ntemp + i * LX1 + j * LX1 * LX1;
                idel[iel][2][j][i] = ntemp + i * 1 + j * LX1 * LX1 + LX1 * (LX1 - 1);
                idel[iel][3][j][i] = ntemp + i * 1 + j * LX1 * LX1;
                idel[iel][4][j][i] = ntemp + i * 1 + j * LX1 + LX1 * LX1 * (LX1 - 1);
                idel[iel][5][j][i] = ntemp + i * 1 + j * LX1;
            }
        }
    }
}


//------------------------------------------------------------------
// compute the discrete laplacian operators
//------------------------------------------------------------------
void setdef()
{
    int i, j, ip;

    r_init(wdtdr[0], LX1 * LX1, 0.0);

    for (i = 0; i < LX1; i++)
    {
        for (j = 0; j < LX1; j++)
        {
            for (ip = 0; ip < LX1; ip++)
            {
                wdtdr[j][i] = wdtdr[j][i] + wxm1[ip] * dxm1[i][ip] * dxm1[j][ip];
            }
        }
    }
}


//------------------------------------------------------------------
// mesh information preparations: calculate refinement levels of
// each element, mask matrix for domain boundary and element
// boundaries
//------------------------------------------------------------------
void prepwork()
{
    int i, j, iel, iface, cb;
    double rdlog2;

    ntot = nelt * NXYZ;
    rdlog2 = 1.0 / log(2.0);

    // calculate the refinement levels of each element


    for (iel = 0; iel < nelt; iel++)
    {
        size_e[iel] = (int)(-log(xc[iel][1] - xc[iel][0]) * rdlog2 + 1.e-8) - 1;
    }

    // mask matrix for element boundary


    for (iel = 0; iel < nelt; iel++)
    {
        r_init(tmult[iel][0][0], NXYZ, 1.0);
        for (iface = 0; iface < NSIDES; iface++)
        {
            facev(tmult[iel], iface, 0.0);
        }
    }

    // masks for domain boundary at mortar
    r_init(tmmor, nmor, 1.0);

    for (iel = 0; iel < nelt; iel++)
    {
        for (iface = 0; iface < NSIDES; iface++)
        {
            cb = cbc[iel][iface];
            if (cb == 0)
            {
                for (j = 1; j < LX1 - 1; j++)
                {
                    for (i = 1; i < LX1 - 1; i++)
                    {
                        tmmor[idmo[iel][iface][0][0][j][i]] = 0.0;
                    }
                }

                j = 0;
                for (i = 0; i < LX1 - 1; i++)
                {
                    tmmor[idmo[iel][iface][0][0][j][i]] = 0.0;
                }

                if (idmo[iel][iface][0][0][0][LX1 - 1] == -1)
                {
                    tmmor[idmo[iel][iface][1][0][0][LX1 - 1]] = 0.0;
                }
                else
                {
                    tmmor[idmo[iel][iface][0][0][0][LX1 - 1]] = 0.0;
                    for (i = 0; i < LX1; i++)
                    {
                        tmmor[idmo[iel][iface][1][0][j][i]] = 0.0;
                    }
                }

                i = LX1 - 1;
                if (idmo[iel][iface][1][0][1][LX1 - 1] == -1)
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        tmmor[idmo[iel][iface][0][0][j][i]] = 0.0;
                    }
                    tmmor[idmo[iel][iface][1][1][LX1 - 1][LX1 - 1]] = 0.0;
                }
                else
                {
                    for (j = 1; j < LX1; j++)
                    {
                        tmmor[idmo[iel][iface][1][0][j][i]] = 0.0;
                    }
                    for (j = 0; j < LX1; j++)
                    {
                        tmmor[idmo[iel][iface][1][1][j][i]] = 0.0;
                    }
                }

                j = LX1 - 1;
                tmmor[idmo[iel][iface][0][1][LX1 - 1][0]] = 0.0;
                if (idmo[iel][iface][0][1][LX1 - 1][1] == -1)
                {
                    for (i = 1; i < LX1 - 1; i++)
                    {
                        tmmor[idmo[iel][iface][0][0][j][i]] = 0.0;
                    }
                }
                else
                {
                    for (i = 1; i < LX1; i++)
                    {
                        tmmor[idmo[iel][iface][0][1][j][i]] = 0.0;
                    }
                    for (i = 0; i < LX1 - 1; i++)
                    {
                        tmmor[idmo[iel][iface][1][1][j][i]] = 0.0;
                    }
                }

                i = 0;
                for (j = 1; j < LX1 - 1; j++)
                {
                    tmmor[idmo[iel][iface][0][0][j][i]] = 0.0;
                }
                if (idmo[iel][iface][0][0][LX1 - 1][0] != -1)
                {
                    tmmor[idmo[iel][iface][0][0][LX1 - 1][i]] = 0.0;
                    for (j = 0; j < LX1 - 1; j++)
                    {
                        tmmor[idmo[iel][iface][0][1][j][i]] = 0.0;
                    }
                }
            }
        }
    }
}


//------------------------------------------------------------------
// We store some tables of useful topoint constants
//------------------------------------------------------------------
void top_constants()
{
    //f_e_ef[f][e] returns the other face sharing the e'th local edge of face f.
    f_e_ef[0][0] = 5;
    f_e_ef[0][1] = 2;
    f_e_ef[0][2] = 4;
    f_e_ef[0][3] = 3;
    f_e_ef[1][0] = 5;
    f_e_ef[1][1] = 2;
    f_e_ef[1][2] = 4;
    f_e_ef[1][3] = 3;
    f_e_ef[2][0] = 5;
    f_e_ef[2][1] = 0;
    f_e_ef[2][2] = 4;
    f_e_ef[2][3] = 1;
    f_e_ef[3][0] = 5;
    f_e_ef[3][1] = 0;
    f_e_ef[3][2] = 4;
    f_e_ef[3][3] = 1;
    f_e_ef[4][0] = 3;
    f_e_ef[4][1] = 0;
    f_e_ef[4][2] = 2;
    f_e_ef[4][3] = 1;
    f_e_ef[5][0] = 3;
    f_e_ef[5][1] = 0;
    f_e_ef[5][2] = 2;
    f_e_ef[5][3] = 1;

    // e_c[j][n] returns n'th edge sharing the vertex j of an element
    e_c[0][0] = 4;
    e_c[0][1] = 7;
    e_c[0][2] = 10;
    e_c[1][0] = 0;
    e_c[1][1] = 3;
    e_c[1][2] = 10;
    e_c[2][0] = 4;
    e_c[2][1] = 5;
    e_c[2][2] = 8;
    e_c[3][0] = 0;
    e_c[3][1] = 1;
    e_c[3][2] = 8;
    e_c[4][0] = 6;
    e_c[4][1] = 7;
    e_c[4][2] = 11;
    e_c[5][0] = 2;
    e_c[5][1] = 3;
    e_c[5][2] = 11;
    e_c[6][0] = 5;
    e_c[6][1] = 6;
    e_c[6][2] = 9;
    e_c[7][0] = 1;
    e_c[7][1] = 2;
    e_c[7][2] = 9;

    // local_corner[i][n] returns the local corner index of vertex n on face i
    local_corner[0][0] = -1;
    local_corner[0][1] = 0;
    local_corner[0][2] = -1;
    local_corner[0][3] = 1;
    local_corner[0][4] = -1;
    local_corner[0][5] = 2;
    local_corner[0][6] = -1;
    local_corner[0][7] = 3;
    local_corner[1][0] = 0;
    local_corner[1][1] = -1;
    local_corner[1][2] = 1;
    local_corner[1][3] = -1;
    local_corner[1][4] = 2;
    local_corner[1][5] = -1;
    local_corner[1][6] = 3;
    local_corner[1][7] = -1;
    local_corner[2][0] = -1;
    local_corner[2][1] = -1;
    local_corner[2][2] = 0;
    local_corner[2][3] = 1;
    local_corner[2][4] = -1;
    local_corner[2][5] = -1;
    local_corner[2][6] = 2;
    local_corner[2][7] = 3;
    local_corner[3][0] = 0;
    local_corner[3][1] = 1;
    local_corner[3][2] = -1;
    local_corner[3][3] = -1;
    local_corner[3][4] = 2;
    local_corner[3][5] = 3;
    local_corner[3][6] = -1;
    local_corner[3][7] = -1;
    local_corner[4][0] = -1;
    local_corner[4][1] = -1;
    local_corner[4][2] = -1;
    local_corner[4][3] = -1;
    local_corner[4][4] = 0;
    local_corner[4][5] = 1;
    local_corner[4][6] = 2;
    local_corner[4][7] = 3;
    local_corner[5][0] = 0;
    local_corner[5][1] = 1;
    local_corner[5][2] = 2;
    local_corner[5][3] = 3;
    local_corner[5][4] = -1;
    local_corner[5][5] = -1;
    local_corner[5][6] = -1;
    local_corner[5][7] = -1;

    // cal_nnb[i][n] returns the neighbor elements neighbored by n'th edge
    // among the three edges sharing vertex i
    // the elements are the eight children elements ordered as 0 to 7.
    cal_nnb[0][0] = 4;
    cal_nnb[0][1] = 1;
    cal_nnb[0][2] = 2;
    cal_nnb[1][0] = 5;
    cal_nnb[1][1] = 0;
    cal_nnb[1][2] = 3;
    cal_nnb[2][0] = 6;
    cal_nnb[2][1] = 3;
    cal_nnb[2][2] = 0;
    cal_nnb[3][0] = 7;
    cal_nnb[3][1] = 2;
    cal_nnb[3][2] = 1;
    cal_nnb[4][0] = 0;
    cal_nnb[4][1] = 5;
    cal_nnb[4][2] = 6;
    cal_nnb[5][0] = 1;
    cal_nnb[5][1] = 4;
    cal_nnb[5][2] = 7;
    cal_nnb[6][0] = 2;
    cal_nnb[6][1] = 7;
    cal_nnb[6][2] = 4;
    cal_nnb[7][0] = 3;
    cal_nnb[7][1] = 6;
    cal_nnb[7][2] = 5;

    // returns the opposite local corner index: 0-3,1-2
    oplc[0] = 3;
    oplc[1] = 2;
    oplc[2] = 1;
    oplc[3] = 0;

    // cal_iijj[n][i] returns the location of local corner number n on a face
    // i =0  to get ii, i=1 to get jj
    // (ii,jj) is defined the same as in mortar location (ii,jj)
    cal_iijj[0][0] = 0;
    cal_iijj[0][1] = 0;
    cal_iijj[1][0] = 0;
    cal_iijj[1][1] = 1;
    cal_iijj[2][0] = 1;
    cal_iijj[2][1] = 0;
    cal_iijj[3][0] = 1;
    cal_iijj[3][1] = 1;

    // returns the adjacent(neighbored by a face) element's children,
    // assumming a vertex is shared by eight child elements 0-7.
    // index n is local corner number on the face which is being
    // assigned the mortar index number
    cal_intempx[0][0] = 7;
    cal_intempx[0][1] = 5;
    cal_intempx[0][2] = 3;
    cal_intempx[0][3] = 1;
    cal_intempx[1][0] = 6;
    cal_intempx[1][1] = 4;
    cal_intempx[1][2] = 2;
    cal_intempx[1][3] = 0;
    cal_intempx[2][0] = 7;
    cal_intempx[2][1] = 6;
    cal_intempx[2][2] = 3;
    cal_intempx[2][3] = 2;
    cal_intempx[3][0] = 5;
    cal_intempx[3][1] = 4;
    cal_intempx[3][2] = 1;
    cal_intempx[3][3] = 0;
    cal_intempx[4][0] = 7;
    cal_intempx[4][1] = 6;
    cal_intempx[4][2] = 5;
    cal_intempx[4][3] = 4;
    cal_intempx[5][0] = 3;
    cal_intempx[5][1] = 2;
    cal_intempx[5][2] = 1;
    cal_intempx[5][3] = 0;

    // c_f[f][i] returns the vertex number of i'th local corner on face f
    c_f[0][0] = 1;
    c_f[0][1] = 3;
    c_f[0][2] = 5;
    c_f[0][3] = 7;
    c_f[1][0] = 0;
    c_f[1][1] = 2;
    c_f[1][2] = 4;
    c_f[1][3] = 6;
    c_f[2][0] = 2;
    c_f[2][1] = 3;
    c_f[2][2] = 6;
    c_f[2][3] = 7;
    c_f[3][0] = 0;
    c_f[3][1] = 1;
    c_f[3][2] = 4;
    c_f[3][3] = 5;
    c_f[4][0] = 4;
    c_f[4][1] = 5;
    c_f[4][2] = 6;
    c_f[4][3] = 7;
    c_f[5][0] = 0;
    c_f[5][1] = 1;
    c_f[5][2] = 2;
    c_f[5][3] = 3;

    // on each face of the parent element, there are four children element.
    //le_arr[n][j][i] returns the i'th elements among the four children elements
    // n refers to the direction: 1 for x, 2 for y and 3 for z direction.
    // j refers to positive(0) or negative(1) direction on x, y or z direction.
    // n=1,j=0 refers to face 1 and n=1, j=1 refers to face 2, n=2,j=0 refers to
    // face 3....
    // The current eight children are ordered as 8,1,2,3,4,5,6,7
    le_arr[0][0][0] = 7;
    le_arr[0][0][1] = 1;
    le_arr[0][0][2] = 3;
    le_arr[0][0][3] = 5;
    le_arr[0][1][0] = 0;
    le_arr[0][1][1] = 2;
    le_arr[0][1][2] = 4;
    le_arr[0][1][3] = 6;
    le_arr[1][0][0] = 7;
    le_arr[1][0][1] = 0;
    le_arr[1][0][2] = 3;
    le_arr[1][0][3] = 4;
    le_arr[1][1][0] = 1;
    le_arr[1][1][1] = 2;
    le_arr[1][1][2] = 5;
    le_arr[1][1][3] = 6;
    le_arr[2][0][0] = 7;
    le_arr[2][0][1] = 0;
    le_arr[2][0][2] = 1;
    le_arr[2][0][3] = 2;
    le_arr[2][1][0] = 3;
    le_arr[2][1][1] = 4;
    le_arr[2][1][2] = 5;
    le_arr[2][1][3] = 6;

    // jjface[n] returns the face opposite to face n
    jjface[0] = 1;
    jjface[1] = 0;
    jjface[2] = 3;
    jjface[3] = 2;
    jjface[4] = 5;
    jjface[5] = 4;

    // edgeface[f][n] returns OTHER face which shares local edge n on face f
    // int edgeface[6][4];
    //  edgeface[0][0] = 5;
    //  edgeface[0][1] = 2;
    //  edgeface[0][2] = 4;
    //  edgeface[0][3] = 3;
    //  edgeface[1][0] = 5;
    //  edgeface[1][1] = 2;
    //  edgeface[1][2] = 4;
    //  edgeface[1][3] = 3;
    //  edgeface[2][0] = 5;
    //  edgeface[2][1] = 0;
    //  edgeface[2][2] = 4;
    //  edgeface[2][3] = 1;
    //  edgeface[3][0] = 5;
    //  edgeface[3][1] = 0;
    //  edgeface[3][2] = 4;
    //  edgeface[3][3] = 1;
    //  edgeface[4][0] = 3;
    //  edgeface[4][1] = 0;
    //  edgeface[4][2] = 2;
    //  edgeface[4][3] = 1;
    //  edgeface[5][0] = 3;
    //  edgeface[5][1] = 0;
    //  edgeface[5][2] = 2;
    //  edgeface[5][3] = 1;

    // e_face2[f][n] returns the local edge number of edge n on the
    // other face sharing local edge n on face f
    e_face2[0][0] = 1;
    e_face2[0][1] = 1;
    e_face2[0][2] = 1;
    e_face2[0][3] = 1;
    e_face2[1][0] = 3;
    e_face2[1][1] = 3;
    e_face2[1][2] = 3;
    e_face2[1][3] = 3;
    e_face2[2][0] = 2;
    e_face2[2][1] = 1;
    e_face2[2][2] = 2;
    e_face2[2][3] = 1;
    e_face2[3][0] = 0;
    e_face2[3][1] = 3;
    e_face2[3][2] = 0;
    e_face2[3][3] = 3;
    e_face2[4][0] = 2;
    e_face2[4][1] = 2;
    e_face2[4][2] = 2;
    e_face2[4][3] = 2;
    e_face2[5][0] = 0;
    e_face2[5][1] = 0;
    e_face2[5][2] = 0;
    e_face2[5][3] = 0;

    // op[n] returns the local edge number of the edge which
    // is opposite to local edge n on the same face
    op[0] = 2;
    op[1] = 3;
    op[2] = 0;
    op[3] = 1;

    // localedgenumber[e][f] returns the local edge number for edge e
    // on face f. A minus result value signifies illegal input
    localedgenumber[0][0] = 0;
    localedgenumber[0][1] = -1;
    localedgenumber[0][2] = -1;
    localedgenumber[0][3] = -1;
    localedgenumber[0][4] = -1;
    localedgenumber[0][5] = 1;
    localedgenumber[1][0] = 1;
    localedgenumber[1][1] = -1;
    localedgenumber[1][2] = 1;
    localedgenumber[1][3] = -1;
    localedgenumber[1][4] = -1;
    localedgenumber[1][5] = -1;
    localedgenumber[2][0] = 2;
    localedgenumber[2][1] = -1;
    localedgenumber[2][2] = -1;
    localedgenumber[2][3] = -1;
    localedgenumber[2][4] = 1;
    localedgenumber[2][5] = -1;
    localedgenumber[3][0] = 3;
    localedgenumber[3][1] = -1;
    localedgenumber[3][2] = -1;
    localedgenumber[3][3] = 1;
    localedgenumber[3][4] = -1;
    localedgenumber[3][5] = -1;
    localedgenumber[4][0] = -1;
    localedgenumber[4][1] = 0;
    localedgenumber[4][2] = -1;
    localedgenumber[4][3] = -1;
    localedgenumber[4][4] = -1;
    localedgenumber[4][5] = 3;
    localedgenumber[5][0] = -1;
    localedgenumber[5][1] = 1;
    localedgenumber[5][2] = 3;
    localedgenumber[5][3] = -1;
    localedgenumber[5][4] = -1;
    localedgenumber[5][5] = -1;
    localedgenumber[6][0] = -1;
    localedgenumber[6][1] = 2;
    localedgenumber[6][2] = -1;
    localedgenumber[6][3] = -1;
    localedgenumber[6][4] = 3;
    localedgenumber[6][5] = -1;
    localedgenumber[7][0] = -1;
    localedgenumber[7][1] = 3;
    localedgenumber[7][2] = -1;
    localedgenumber[7][3] = 3;
    localedgenumber[7][4] = -1;
    localedgenumber[7][5] = -1;
    localedgenumber[8][0] = -1;
    localedgenumber[8][1] = -1;
    localedgenumber[8][2] = 0;
    localedgenumber[8][3] = -1;
    localedgenumber[8][4] = -1;
    localedgenumber[8][5] = 2;
    localedgenumber[9][0] = -1;
    localedgenumber[9][1] = -1;
    localedgenumber[9][2] = 2;
    localedgenumber[9][3] = -1;
    localedgenumber[9][4] = 2;
    localedgenumber[9][5] = -1;
    localedgenumber[10][0] = -1;
    localedgenumber[10][1] = -1;
    localedgenumber[10][2] = -1;
    localedgenumber[10][3] = 0;
    localedgenumber[10][4] = -1;
    localedgenumber[10][5] = 0;
    localedgenumber[11][0] = -1;
    localedgenumber[11][1] = -1;
    localedgenumber[11][2] = -1;
    localedgenumber[11][3] = 2;
    localedgenumber[11][4] = 0;
    localedgenumber[11][5] = -1;

    // edgenumber[f][e] returns the edge index of local edge e on face f
    edgenumber[0][0] = 0;
    edgenumber[0][1] = 1;
    edgenumber[0][2] = 2;
    edgenumber[0][3] = 3;
    edgenumber[1][0] = 4;
    edgenumber[1][1] = 5;
    edgenumber[1][2] = 6;
    edgenumber[1][3] = 7;
    edgenumber[2][0] = 8;
    edgenumber[2][1] = 1;
    edgenumber[2][2] = 9;
    edgenumber[2][3] = 5;
    edgenumber[3][0] = 10;
    edgenumber[3][1] = 3;
    edgenumber[3][2] = 11;
    edgenumber[3][3] = 7;
    edgenumber[4][0] = 11;
    edgenumber[4][1] = 2;
    edgenumber[4][2] = 9;
    edgenumber[4][3] = 6;
    edgenumber[5][0] = 10;
    edgenumber[5][1] = 0;
    edgenumber[5][2] = 8;
    edgenumber[5][3] = 4;

    // f_c[n][c] returns the face index of i'th face sharing vertex n
    f_c[0][0] = 1;
    f_c[0][1] = 3;
    f_c[0][2] = 5;
    f_c[1][0] = 0;
    f_c[1][1] = 3;
    f_c[1][2] = 5;
    f_c[2][0] = 1;
    f_c[2][1] = 2;
    f_c[2][2] = 5;
    f_c[3][0] = 0;
    f_c[3][1] = 2;
    f_c[3][2] = 5;
    f_c[4][0] = 1;
    f_c[4][1] = 3;
    f_c[4][2] = 4;
    f_c[5][0] = 0;
    f_c[5][1] = 3;
    f_c[5][2] = 4;
    f_c[6][0] = 1;
    f_c[6][1] = 2;
    f_c[6][2] = 4;
    f_c[7][0] = 0;
    f_c[7][1] = 2;
    f_c[7][2] = 4;

    // if two elements are neighbor by one edge,
    // e1v1[f2][f1] returns the smaller index of the two vertices on this
    // edge on one element
    // e1v2 returns the larger index of the two vertices of this edge on
    // on element. exfor a vertex on element
    // e2v1 returns the smaller index of the two vertices on this edge on
    // another element
    // e2v2 returns the larger index of the two vertiex on this edge on
    // another element
    //e1v1
    e1v1[0][0] = -1;
    e1v1[0][1] = -1;
    e1v1[0][2] = 3;
    e1v1[0][3] = 1;
    e1v1[0][4] = 5;
    e1v1[0][5] = 1;
    e1v1[1][0] = -1;
    e1v1[1][1] = -1;
    e1v1[1][2] = 2;
    e1v1[1][3] = 0;
    e1v1[1][4] = 4;
    e1v1[1][5] = 0;
    e1v1[2][0] = 3;
    e1v1[2][1] = 2;
    e1v1[2][2] = -1;
    e1v1[2][3] = -1;
    e1v1[2][4] = 6;
    e1v1[2][5] = 2;
    e1v1[3][0] = 1;
    e1v1[3][1] = 0;
    e1v1[3][2] = -1;
    e1v1[3][3] = -1;
    e1v1[3][4] = 4;
    e1v1[3][5] = 0;
    e1v1[4][0] = 5;
    e1v1[4][1] = 4;
    e1v1[4][2] = 6;
    e1v1[4][3] = 4;
    e1v1[4][4] = -1;
    e1v1[4][5] = -1;
    e1v1[5][0] = 1;
    e1v1[5][1] = 0;
    e1v1[5][2] = 2;
    e1v1[5][3] = 0;
    e1v1[5][4] = -1;
    e1v1[5][5] = -1;

    //e2v1
    e2v1[0][0] = -1;
    e2v1[0][1] = -1;
    e2v1[0][2] = 0;
    e2v1[0][3] = 2;
    e2v1[0][4] = 0;
    e2v1[0][5] = 4;
    e2v1[1][0] = -1;
    e2v1[1][1] = -1;
    e2v1[1][2] = 1;
    e2v1[1][3] = 3;
    e2v1[1][4] = 1;
    e2v1[1][5] = 5;
    e2v1[2][0] = 0;
    e2v1[2][1] = 1;
    e2v1[2][2] = -1;
    e2v1[2][3] = -1;
    e2v1[2][4] = 0;
    e2v1[2][5] = 4;
    e2v1[3][0] = 2;
    e2v1[3][1] = 3;
    e2v1[3][2] = -1;
    e2v1[3][3] = -1;
    e2v1[3][4] = 2;
    e2v1[3][5] = 6;
    e2v1[4][0] = 0;
    e2v1[4][1] = 1;
    e2v1[4][2] = 0;
    e2v1[4][3] = 2;
    e2v1[4][4] = -1;
    e2v1[4][5] = -1;
    e2v1[5][0] = 4;
    e2v1[5][1] = 5;
    e2v1[5][2] = 4;
    e2v1[5][3] = 6;
    e2v1[5][4] = -1;
    e2v1[5][5] = -1;

    //e1v2
    e1v2[0][0] = -1;
    e1v2[0][1] = -1;
    e1v2[0][2] = 7;
    e1v2[0][3] = 5;
    e1v2[0][4] = 7;
    e1v2[0][5] = 3;
    e1v2[1][0] = -1;
    e1v2[1][1] = -1;
    e1v2[1][2] = 6;
    e1v2[1][3] = 4;
    e1v2[1][4] = 6;
    e1v2[1][5] = 2;
    e1v2[2][0] = 7;
    e1v2[2][1] = 6;
    e1v2[2][2] = -1;
    e1v2[2][3] = -1;
    e1v2[2][4] = 7;
    e1v2[2][5] = 3;
    e1v2[3][0] = 5;
    e1v2[3][1] = 4;
    e1v2[3][2] = -1;
    e1v2[3][3] = -1;
    e1v2[3][4] = 5;
    e1v2[3][5] = 1;
    e1v2[4][0] = 7;
    e1v2[4][1] = 6;
    e1v2[4][2] = 7;
    e1v2[4][3] = 5;
    e1v2[4][4] = -1;
    e1v2[4][5] = -1;
    e1v2[5][0] = 3;
    e1v2[5][1] = 2;
    e1v2[5][2] = 3;
    e1v2[5][3] = 1;
    e1v2[5][4] = -1;
    e1v2[5][5] = -1;

    //e2v2
    e2v2[0][0] = -1;
    e2v2[0][1] = -1;
    e2v2[0][2] = 4;
    e2v2[0][3] = 6;
    e2v2[0][4] = 2;
    e2v2[0][5] = 6;
    e2v2[1][0] = -1;
    e2v2[1][1] = -1;
    e2v2[1][2] = 5;
    e2v2[1][3] = 7;
    e2v2[1][4] = 3;
    e2v2[1][5] = 7;
    e2v2[2][0] = 4;
    e2v2[2][1] = 5;
    e2v2[2][2] = -1;
    e2v2[2][3] = -1;
    e2v2[2][4] = 1;
    e2v2[2][5] = 5;
    e2v2[3][0] = 6;
    e2v2[3][1] = 7;
    e2v2[3][2] = -1;
    e2v2[3][3] = -1;
    e2v2[3][4] = 3;
    e2v2[3][5] = 7;
    e2v2[4][0] = 2;
    e2v2[4][1] = 3;
    e2v2[4][2] = 1;
    e2v2[4][3] = 3;
    e2v2[4][4] = -1;
    e2v2[4][5] = -1;
    e2v2[5][0] = 6;
    e2v2[5][1] = 7;
    e2v2[5][2] = 5;
    e2v2[5][3] = 7;
    e2v2[5][4] = -1;
    e2v2[5][5] = -1;

    // children[n][n1] returns the four elements among the eight children
    // elements to be merged on face n of the parent element
    // the IDs for the eight children are 0,1,2,3,4,5,6,7
    children[0][0] = 1;
    children[0][1] = 3;
    children[0][2] = 5;
    children[0][3] = 7;
    children[1][0] = 0;
    children[1][1] = 2;
    children[1][2] = 4;
    children[1][3] = 6;
    children[2][0] = 2;
    children[2][1] = 3;
    children[2][2] = 6;
    children[2][3] = 7;
    children[3][0] = 0;
    children[3][1] = 1;
    children[3][2] = 4;
    children[3][3] = 5;
    children[4][0] = 4;
    children[4][1] = 5;
    children[4][2] = 6;
    children[4][3] = 7;
    children[5][0] = 0;
    children[5][1] = 1;
    children[5][2] = 2;
    children[5][3] = 3;

    // iijj[n][n1] returns the location of n's mortar on an element face
    // n1=0 refers to x direction location and n1=1 refers to y direction
    iijj[0][0] = 0;
    iijj[0][1] = 0;
    iijj[1][0] = 0;
    iijj[1][1] = 1;
    iijj[2][0] = 1;
    iijj[2][1] = 0;
    iijj[3][0] = 1;
    iijj[3][1] = 1;

    // v_end[n] returns the index of collocation points at two ends of each
    // direction
    v_end[0] = 0;
    v_end[1] = LX1 - 1;

    //face_l1,face_l2,face_ld return for start,end,stride for a loop over faces
    // used on subroutine  mortar_vertex
    face_l1[0] = 1;
    face_l1[1] = 2;
    face_l1[2] = 0;

    face_l2[0] = 2;
    face_l2[1] = 0;
    face_l2[2] = 1;

    face_ld[0] = 1;
    face_ld[1] = -2;
    face_ld[2] = 1;
}

//------------------------------------------------------------------
// Map values from mortar(tmor) to element(tx)
//------------------------------------------------------------------
void transf(double tmor[], double tx[])
{
    double tmp[2][LX1][LX1];
    int ig1, ig2, ig3, ig4, ie, iface, il1, il2, il3, il4;
    int nnje, ije1, ije2, col, i, j, ig, il;

    // zero out tx on element boundaries
    col2(tx, (double *)tmult, ntot);

    for (ie = 0; ie < nelt; ie++)
    {
        for (iface = 0; iface < NSIDES; iface++)
        {
            // get the collocation point index of the four local corners on the
            // face iface of element ie
            il1 = idel[ie][iface][0][0];
            il2 = idel[ie][iface][0][LX1 - 1];
            il3 = idel[ie][iface][LX1 - 1][0];
            il4 = idel[ie][iface][LX1 - 1][LX1 - 1];

            // get the mortar indices of the four local corners
            ig1 = idmo[ie][iface][0][0][0][0];
            ig2 = idmo[ie][iface][1][0][0][LX1 - 1];
            ig3 = idmo[ie][iface][0][1][LX1 - 1][0];
            ig4 = idmo[ie][iface][1][1][LX1 - 1][LX1 - 1];

            // copy the value from tmor to tx for these four local corners
            tx[il1] = tmor[ig1];
            tx[il2] = tmor[ig2];
            tx[il3] = tmor[ig3];
            tx[il4] = tmor[ig4];

            // nnje=1 for conforming faces, nnje=2 for nonconforming faces
            if (cbc[ie][iface] == 3)
            {
                nnje = 2;
            }
            else
            {
                nnje = 1;
            }

            // for nonconforming faces
            if (nnje == 2)
            {
                // nonconforming faces have four pieces of mortar, first map them to
                // two intermediate mortars, stored in tmp
                r_init((double *)tmp, LX1 * LX1 * 2, 0.0);


                for (ije1 = 0; ije1 < nnje; ije1++)
                {
                    for (ije2 = 0; ije2 < nnje; ije2++)
                    {
                        for (col = 0; col < LX1; col++)
                        {
                            // in each row col, when coloumn i=1 or LX1, the value
                            // in tmor is copied to tmp
                            i = v_end[ije2];
                            ig = idmo[ie][iface][ije2][ije1][col][i];
                            tmp[ije1][col][i] = tmor[ig];

                            // in each row col, value in the interior three collocation
                            // points is computed by apply mapping matrix qbnew to tmor
                            for (i = 1; i < LX1 - 1; i++)
                            {
                                il = idel[ie][iface][col][i];
                                for (j = 0; j < LX1; j++)
                                {
                                    ig = idmo[ie][iface][ije2][ije1][col][j];
                                    tmp[ije1][col][i] = tmp[ije1][col][i] +
                                                        qbnew[ije2][j][i - 1] * tmor[ig];
                                }
                            }
                        }
                    }
                }

                // mapping from two pieces of intermediate mortar tmp to element
                // face tx
                for (ije1 = 0; ije1 < nnje; ije1++)
                {
                    // the first column, col=0, is an edge of face iface.
                    // the value on the three interior collocation points, tx, is
                    // computed by applying mapping matrices qbnew to tmp.
                    // the mapping result is divided by 2, because there will be
                    // duplicated contribution from another face sharing this edge.
                    col = 0;
                    for (i = 1; i < LX1 - 1; i++)
                    {
                        il = idel[ie][iface][i][col];
                        for (j = 0; j < LX1; j++)
                        {
                            tx[il] = tx[il] + qbnew[ije1][j][i - 1] *
                                     tmp[ije1][j][col] * 0.5;
                        }
                    }

                    // for column 1 ~ lx-2
                    for (col = 1; col < LX1 - 1; col++)
                    {
                        //when i=0 or LX1-1, the collocation points are also on an edge of
                        // the face, so the mapping result also needs to be divided by 2
                        i = v_end[ije1];
                        il = idel[ie][iface][i][col];
                        tx[il] = tx[il] + tmp[ije1][i][col] * 0.5;

                        // compute the value at interior collocation points in
                        // columns 1 ~ LX1-1
                        for (i = 1; i < LX1 - 1; i++)
                        {
                            il = idel[ie][iface][i][col];
                            for (j = 0; j < LX1; j++)
                            {
                                tx[il] = tx[il] + qbnew[ije1][j][i - 1] * tmp[ije1][j][col];
                            }
                        }
                    }

                    // same as col=0
                    col = LX1 - 1;
                    for (i = 1; i < LX1 - 1; i++)
                    {
                        il = idel[ie][iface][i][col];
                        for (j = 0; j < LX1; j++)
                        {
                            tx[il] = tx[il] + qbnew[ije1][j][i - 1] *
                                     tmp[ije1][j][col] * 0.5;
                        }
                    }
                }

                // for conforming faces
            }
            else
            {
                // face interior
                for (col = 1; col < LX1 - 1; col++)
                {
                    for (i = 1; i < LX1 - 1; i++)
                    {
                        il = idel[ie][iface][col][i];
                        ig = idmo[ie][iface][0][0][col][i];
                        tx[il] = tmor[ig];
                    }
                }

                // edges of conforming faces

                // if local edge 0 is a nonconforming edge
                if (idmo[ie][iface][0][0][0][LX1 - 1] != -1)
                {
                    for (i = 1; i < LX1 - 1; i++)
                    {
                        il = idel[ie][iface][0][i];
                        for (ije1 = 0; ije1 < 2; ije1++)
                        {
                            for (j = 0; j < LX1; j++)
                            {
                                ig = idmo[ie][iface][ije1][0][0][j];
                                tx[il] = tx[il] + qbnew[ije1][j][i - 1] * tmor[ig] * 0.5;
                            }
                        }
                    }

                    // if local edge 0 is a conforming edge
                }
                else
                {
                    for (i = 1; i < LX1 - 1; i++)
                    {
                        il = idel[ie][iface][0][i];
                        ig = idmo[ie][iface][0][0][0][i];
                        tx[il] = tmor[ig];
                    }
                }

                // if local edge 1 is a nonconforming edge
                if (idmo[ie][iface][1][0][1][LX1 - 1] != -1)
                {
                    for (i = 1; i < LX1 - 1; i++)
                    {
                        il = idel[ie][iface][i][LX1 - 1];
                        for (ije1 = 0; ije1 < 2; ije1++)
                        {
                            for (j = 0; j < LX1; j++)
                            {
                                ig = idmo[ie][iface][1][ije1][j][LX1 - 1];
                                tx[il] = tx[il] + qbnew[ije1][j][i - 1] * tmor[ig] * 0.5;
                            }
                        }
                    }

                    // if local edge 1 is a conforming edge
                }
                else
                {
                    for (i = 1; i < LX1 - 1; i++)
                    {
                        il = idel[ie][iface][i][LX1 - 1];
                        ig = idmo[ie][iface][0][0][i][LX1 - 1];
                        tx[il] = tmor[ig];
                    }
                }

                // if local edge 2 is a nonconforming edge
                if (idmo[ie][iface][0][1][LX1 - 1][1] != -1)
                {
                    for (i = 1; i < LX1 - 1; i++)
                    {
                        il = idel[ie][iface][LX1 - 1][i];
                        for (ije1 = 0; ije1 < 2; ije1++)
                        {
                            for (j = 0; j < LX1; j++)
                            {
                                ig = idmo[ie][iface][ije1][1][LX1 - 1][j];
                                tx[il] = tx[il] + qbnew[ije1][j][i - 1] * tmor[ig] * 0.5;
                            }
                        }
                    }

                    // if local edge 2 is a conforming edge
                }
                else
                {
                    for (i = 1; i < LX1 - 1; i++)
                    {
                        il = idel[ie][iface][LX1 - 1][i];
                        ig = idmo[ie][iface][0][0][LX1 - 1][i];
                        tx[il] = tmor[ig];
                    }
                }

                // if local edge 3 is a nonconforming edge
                if (idmo[ie][iface][0][0][LX1 - 1][0] != -1)
                {
                    for (i = 1; i < LX1 - 1; i++)
                    {
                        il = idel[ie][iface][i][0];
                        for (ije1 = 0; ije1 < 2; ije1++)
                        {
                            for (j = 0; j < LX1; j++)
                            {
                                ig = idmo[ie][iface][0][ije1][j][0];
                                tx[il] = tx[il] + qbnew[ije1][j][i - 1] * tmor[ig] * 0.5;
                            }
                        }
                    }
                    // if local edge 3 is a conforming edge
                }
                else
                {
                    for (i = 1; i < LX1 - 1; i++)
                    {
                        il = idel[ie][iface][i][0];
                        ig = idmo[ie][iface][0][0][i][0];
                        tx[il] = tmor[ig];
                    }
                }
            }
        }
    }
}


//------------------------------------------------------------------
// Map from element(tx) to mortar(tmor).
// tmor sums contributions from all elements.
//------------------------------------------------------------------
void transfb(double tmor[], double tx[])
{
    const double third = 1.0 / 3.0;
    int shift;

    double tmp, tmp1, temp[2][LX1][LX1], top[2][LX1];
    int il1, il2, il3, il4, ig1, ig2, ig3, ig4, ie, iface, nnje;
    int ije1, ije2, col, i, j, ije, ig, il;

    r_init(tmor, nmor, 0.0);

    for (ie = 0; ie < nelt; ie++)
    {
        for (iface = 0; iface < NSIDES; iface++)
        {
            // nnje=1 for conforming faces, nnje=2 for nonconforming faces
            if (cbc[ie][iface] == 3)
            {
                nnje = 2;
            }
            else
            {
                nnje = 1;
            }

            // get collocation point index of four local corners on the face
            il1 = idel[ie][iface][0][0];
            il2 = idel[ie][iface][0][LX1 - 1];
            il3 = idel[ie][iface][LX1 - 1][0];
            il4 = idel[ie][iface][LX1 - 1][LX1 - 1];

            // get the mortar indices of the four local corners
            ig1 = idmo[ie][iface][0][0][0][0];
            ig2 = idmo[ie][iface][1][0][0][LX1 - 1];
            ig3 = idmo[ie][iface][0][1][LX1 - 1][0];
            ig4 = idmo[ie][iface][1][1][LX1 - 1][LX1 - 1];

            // sum the values from tx to tmor for these four local corners
            // only 1/3 of the value is summed, since there will be two duplicated
            // contributions from the other two faces sharing this vertex
            tmor[ig1] = tmor[ig1] + tx[il1] * third;
            tmor[ig2] = tmor[ig2] + tx[il2] * third;
            tmor[ig3] = tmor[ig3] + tx[il3] * third;
            tmor[ig4] = tmor[ig4] + tx[il4] * third;

            // for nonconforming faces
            if (nnje == 2)
            {
                r_init((double *)temp, LX1 * LX1 * 2, 0.0);

                // nonconforming faces have four pieces of mortar, first map tx to
                // two intermediate mortars stored in temp
                for (ije2 = 0; ije2 < nnje; ije2++)
                {
                    shift = ije2;
                    for (col = 0; col < LX1; col++)
                    {
                        // For mortar points on face edge (top and bottom), copy the
                        // value from tx to temp
                        il = idel[ie][iface][v_end[ije2]][col];
                        temp[ije2][v_end[ije2]][col] = tx[il];

                        // For mortar points on face edge (top and bottom), calculate
                        // the interior points' contribution to them, i.e. top()
                        j = v_end[ije2];
                        tmp = 0.0;
                        for (i = 1; i < LX1 - 1; i++)
                        {
                            il = idel[ie][iface][i][col];
                            tmp = tmp + qbnew[ije2][j][i - 1] * tx[il];
                        }

                        top[ije2][col] = tmp;

                        // Use mapping matrices qbnew to map the value from tx to temp
                        // for mortar points not on the top bottom face edge.


                        for (j = 2 - shift - 1; j < LX1 - shift; j++)
                        {
                            tmp = 0.0;
                            for (i = 1; i < LX1 - 1; i++)
                            {
                                il = idel[ie][iface][i][col];
                                tmp = tmp + qbnew[ije2][j][i - 1] * tx[il];
                            };
                            temp[ije2][j][col] = tmp + temp[ije2][j][col];
                        }
                    }
                }

                // mapping from temp to tmor
                for (ije1 = 0; ije1 < nnje; ije1++)
                {
                    shift = ije1;
                    for (ije2 = 0; ije2 < nnje; ije2++)
                    {

                        // for each column of collocation points on a piece of mortar
                        for (col = 2 - shift - 1; col < LX1 - shift; col++)
                        {

                            // For the end point, which is on an edge (local edge 1,3),
                            // the contribution is halved since there will be duplicated
                            // contribution from another face sharing this edge.

                            ig = idmo[ie][iface][ije2][ije1][col][v_end[ije2]];
                            tmor[ig] = tmor[ig] + temp[ije1][col][v_end[ije2]] * 0.5;

                            // In each row of collocation points on a piece of mortar,
                            // sum the contributions from interior collocation points
                            // (i=1,LX1-2)
                            for (j = 0; j < LX1; j++)
                            {
                                tmp = 0.0;
                                for (i = 1; i < LX1 - 1; i++)
                                {
                                    tmp = tmp + qbnew[ije2][j][i - 1] * temp[ije1][col][i];
                                }
                                ig = idmo[ie][iface][ije2][ije1][col][j];
                                tmor[ig] = tmor[ig] + tmp;
                            }
                        }

                        // For tmor on local edge 0 and 2, tmp is the contribution from
                        // an edge, so it is halved because of duplicated contribution
                        // from another face sharing this edge. tmp1 is contribution
                        // from face interior.

                        col = v_end[ije1];
                        ig = idmo[ie][iface][ije2][ije1][col][v_end[ije2]];
                        tmor[ig] = tmor[ig] + top[ije1][v_end[ije2]] * 0.5;
                        for (j = 0; j < LX1; j++)
                        {
                            tmp = 0.0;
                            tmp1 = 0.0;
                            for (i = 1; i < LX1 - 1; i++)
                            {
                                tmp  = tmp  + qbnew[ije2][j][i - 1] * temp[ije1][col][i];
                                tmp1 = tmp1 + qbnew[ije2][j][i - 1] * top[ije1][i];
                            }
                            ig = idmo[ie][iface][ije2][ije1][col][j];
                            tmor[ig] = tmor[ig] + tmp * 0.5 + tmp1;
                        }
                    }
                }

                // for conforming faces
            }
            else
            {

                // face interior
                for (col = 1; col < LX1 - 1; col++)
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][col][j];
                        ig = idmo[ie][iface][0][0][col][j];
                        tmor[ig] = tmor[ig] + tx[il];
                    }
                }

                // edges of conforming faces

                // if local edge 0 is a nonconforming edge
                if (idmo[ie][iface][0][0][0][LX1 - 1] != -1)
                {
                    for (ije = 0; ije < 2; ije++)
                    {
                        for (j = 0; j < LX1; j++)
                        {
                            tmp = 0.0;
                            for (i = 1; i < LX1 - 1; i++)
                            {
                                il = idel[ie][iface][0][i];
                                tmp = tmp + qbnew[ije][j][i - 1] * tx[il];
                            }
                            ig = idmo[ie][iface][ije][0][0][j];
                            tmor[ig] = tmor[ig] + tmp * 0.5;
                        }
                    }

                    // if local edge 0 is a conforming edge
                }
                else
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][0][j];
                        ig = idmo[ie][iface][0][0][0][j];
                        tmor[ig] = tmor[ig] + tx[il] * 0.5;
                    }
                }

                // if local edge 1 is a nonconforming edge
                if (idmo[ie][iface][1][0][1][LX1 - 1] != -1)
                {
                    for (ije = 0; ije < 2; ije++)
                    {
                        for (j = 0; j < LX1; j++)
                        {
                            tmp = 0.0;
                            for (i = 1; i < LX1 - 1; i++)
                            {
                                il = idel[ie][iface][i][LX1 - 1];
                                tmp = tmp + qbnew[ije][j][i - 1] * tx[il];
                            }
                            ig = idmo[ie][iface][1][ije][j][LX1 - 1];
                            tmor[ig] = tmor[ig] + tmp * 0.5;
                        }
                    }

                    // if local edge 1 is a conforming edge
                }
                else
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][j][LX1 - 1];
                        ig = idmo[ie][iface][0][0][j][LX1 - 1];
                        tmor[ig] = tmor[ig] + tx[il] * 0.5;
                    }
                }

                // if local edge 2 is a nonconforming edge
                if (idmo[ie][iface][0][1][LX1 - 1][1] != -1)
                {
                    for (ije = 0; ije < 2; ije++)
                    {
                        for (j = 0; j < LX1; j++)
                        {
                            tmp = 0.0;
                            for (i = 1; i < LX1 - 1; i++)
                            {
                                il = idel[ie][iface][LX1 - 1][i];
                                tmp = tmp + qbnew[ije][j][i - 1] * tx[il];
                            }
                            ig = idmo[ie][iface][ije][1][LX1 - 1][j];
                            tmor[ig] = tmor[ig] + tmp * 0.5;
                        }
                    }

                    // if local edge 2 is a conforming edge
                }
                else
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][LX1 - 1][j];
                        ig = idmo[ie][iface][0][0][LX1 - 1][j];
                        tmor[ig] = tmor[ig] + tx[il] * 0.5;
                    }
                }

                // if local edge 3 is a nonconforming edge
                if (idmo[ie][iface][0][0][LX1 - 1][0] != -1)
                {
                    for (ije = 0; ije < 2; ije++)
                    {
                        for (j = 0; j < LX1; j++)
                        {
                            tmp = 0.0;
                            for (i = 1; i < LX1 - 1; i++)
                            {
                                il = idel[ie][iface][i][0];
                                tmp = tmp + qbnew[ije][j][i - 1] * tx[il];
                            }
                            ig = idmo[ie][iface][0][ije][j][0];
                            tmor[ig] = tmor[ig] + tmp * 0.5;
                        }
                    }

                    // if local edge 3 is a conforming edge
                }
                else
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][j][0];
                        ig = idmo[ie][iface][0][0][j][0];
                        tmor[ig] = tmor[ig] + tx[il] * 0.5;
                    }
                }
            } //nnje=1
        }
    }
}


//--------------------------------------------------------------
// This subroutine performs the edge to mortar mapping and
// calculates the mapping result on the mortar point at a vertex
// under situation 1,2, or 3.
// n refers to the configuration of three edges sharing a vertex,
// n = 1: only one edge is nonconforming
// n = 2: two edges are nonconforming
// n = 3: three edges are nonconforming
//-------------------------------------------------------------------
void transfb_cor_e(int n, double *tmor, double tx[LX1][LX1][LX1])
{
    double tmp;
    int i;

    tmp = tx[0][0][0];

    for (i = 1; i < LX1 - 1; i++)
    {
        tmp = tmp + qbnew[0][0][i - 1] * tx[0][0][i];
    }

    if (n > 1)
    {
        for (i = 1; i < LX1 - 1; i++)
        {
            tmp = tmp + qbnew[0][0][i - 1] * tx[0][i][0];
        }
    }

    if (n == 3)
    {
        for (i = 1; i < LX1 - 1; i++)
        {
            tmp = tmp + qbnew[0][0][i - 1] * tx[i][0][0];
        }
    }

    *tmor = tmp;
}


//--------------------------------------------------------------
// This subroutine performs the mapping from face to mortar.
// Output tmor is the mapping result on a mortar vertex
// of situations of three edges and three faces sharing a vertex:
// n=4: only one face is nonconforming
// n=5: one face and one edge are nonconforming
// n=6: two faces are nonconforming
// n=7: three faces are nonconforming
//--------------------------------------------------------------
void transfb_cor_f(int n, double *tmor, double tx[LX1][LX1][LX1])
{
    double temp[LX1], tmp;
    int col, i;

    r_init(temp, LX1, 0.0);

    for (col = 0; col < LX1; col++)
    {
        temp[col] = tx[0][0][col];
        for (i = 1; i < LX1 - 1; i++)
        {
            temp[col] = temp[col] + qbnew[0][0][i - 1] * tx[0][i][col];
        }
    }
    tmp = temp[0];

    for (i = 1; i < LX1 - 1; i++)
    {
        tmp = tmp + qbnew[0][0][i - 1] * temp[i];
    }

    if (n == 5)
    {
        for (i = 1; i < LX1 - 1; i++)
        {
            tmp = tmp + qbnew[0][0][i - 1] * tx[i][0][0];
        }
    }

    if (n >= 6)
    {
        r_init(temp, LX1, 0.0);
        for (col = 0; col < LX1; col++)
        {
            for (i = 1; i < LX1 - 1; i++)
            {
                temp[col] = temp[col] + qbnew[0][0][i - 1] * tx[i][0][col];
            }
        }
        tmp = tmp + temp[0];
        for (i = 1; i < LX1 - 1; i++)
        {
            tmp = tmp + qbnew[0][0][i - 1] * temp[i];
        }
    }

    if (n == 7)
    {
        r_init(temp, LX1, 0.0);
        for (col = 1; col < LX1 - 1; col++)
        {
            for (i = 1; i < LX1 - 1; i++)
            {
                temp[col] = temp[col] + qbnew[0][0][i - 1] * tx[i][col][0];
            }
        }
        for (i = 1; i < LX1 - 1; i++)
        {
            tmp = tmp + qbnew[0][0][i - 1] * temp[i];
        }
    }

    *tmor = tmp;
}


//------------------------------------------------------------------------
// Perform mortar to element mapping on a nonconforming face.
// This subroutin is used when all entries in tmor are zero except
// one tmor[j][i]=1. So this routine is simplified. Only one piece of
// mortar  (tmor only has two indices) and one piece of intermediate
// mortar (tmp) are involved.
//------------------------------------------------------------------------
void transf_nc(double tmor[LX1][LX1], double tx[LX1][LX1])
{
    double tmp[LX1][LX1];
    int col, i, j;

    r_init((double *)tmp, LX1 * LX1, 0.0);
    for (col = 0; col < LX1; col++)
    {
        i = 0;
        tmp[col][i] = tmor[col][i];
        for (i = 1; i < LX1 - 1; i++)
        {
            for (j = 0; j < LX1; j++)
            {
                tmp[col][i] = tmp[col][i] + qbnew[0][j][i - 1] * tmor[col][j];
            }
        }
    }

    for (col = 0; col < LX1; col++)
    {
        i = 0;
        tx[i][col] = tx[i][col] + tmp[i][col];
        for (i = 1; i < LX1 - 1; i++)
        {
            for (j = 0; j < LX1; j++)
            {
                tx[i][col] = tx[i][col] + qbnew[0][j][i - 1] * tmp[j][col];
            }
        }
    }
}


//------------------------------------------------------------------------
// Performs mapping from element to mortar when the nonconforming
// edges are shared by two conforming faces of an element.
//------------------------------------------------------------------------
void transfb_nc0(double tmor[LX1][LX1], double tx[LX1][LX1][LX1])
{
    int i, j;

    r_init((double *)tmor, LX1 * LX1, 0.0);
    for (j = 0; j < LX1; j++)
    {
        for (i = 1; i < LX1 - 1; i++)
        {
            tmor[0][j] = tmor[0][j] + qbnew[0][j][i - 1] * tx[0][0][i];
        }
    }
}


//------------------------------------------------------------------------
// Maps values from element to mortar when the nonconforming edges are
// shared by two nonconforming faces of an element.
// Although each face shall have four pieces of mortar, only value in
// one piece (location (0,0)) is used in the calling routine so only
// the value in the first mortar is calculated in this subroutine.
//------------------------------------------------------------------------
void transfb_nc2(double tmor[LX1][LX1], double tx[LX1][LX1])
{
    double bottom[LX1], temp[LX1][LX1];
    int col, j, i;

    r_init((double *)tmor, LX1 * LX1, 0.0);
    r_init((double *)temp, LX1 * LX1, 0.0);
    tmor[0][0] = tx[0][0];

    // mapping from tx to intermediate mortar temp + bottom
    for (col = 0; col < LX1; col++)
    {
        temp[0][col] = tx[0][col];
        j = 0;
        bottom[col] = 0.0;;
        for (i = 1; i < LX1 - 1; i++)
        {
            bottom[col] = bottom[col] + qbnew[0][j][i - 1] * tx[i][col];
        }

        for (j = 1; j < LX1; j++)
        {
            for (i = 1; i < LX1 - 1; i++)
            {
                temp[j][col] = temp[j][col] + qbnew[0][j][i - 1] * tx[i][col];
            }
        }
    }

    // from intermediate mortar to mortar

    // On the nonconforming edge, temp is divided by 2 as there will be
    // a duplicate contribution from another face sharing this edge
    col = 0;
    for (j = 0; j < LX1; j++)
    {
        for (i = 1; i < LX1 - 1; i++)
        {
            tmor[col][j] = tmor[col][j] + qbnew[0][j][i - 1] * bottom[i] +
                           qbnew[0][j][i - 1] * temp[col][i] * 0.5;
        }
    }

    for (col = 1; col < LX1; col++)
    {
        tmor[col][0] = tmor[col][0] + temp[col][0];
        for (j = 0; j < LX1; j++)
        {
            for (i = 1; i < LX1 - 1; i++)
            {
                tmor[col][j] = tmor[col][j] + qbnew[0][j][i - 1] * temp[col][i];
            }
        }
    }
}


//------------------------------------------------------------------------
// Maps values from element to mortar when the nonconforming edges are
// shared by a nonconforming face and a conforming face of an element
//------------------------------------------------------------------------
void transfb_nc1(double tmor[LX1][LX1], double tx[LX1][LX1])
{
    double bottom[LX1], temp[LX1][LX1];
    int col, j, i;

    r_init((double *)tmor, LX1 * LX1, 0.0);
    r_init((double *)temp, LX1 * LX1, 0.0);

    tmor[0][0] = tx[0][0];
    // Contribution from the nonconforming faces
    // Since the calling subroutine is only interested in the value on the
    // mortar (location (0,0)), only this piece of mortar is calculated.

    for (col = 0; col < LX1; col++)
    {
        temp[0][col] = tx[0][col];
        j = 0;
        bottom[col] = 0.0;
        for (i = 1; i < LX1 - 1; i++)
        {
            bottom[col] = bottom[col] + qbnew[0][j][i - 1] * tx[i][col];
        }

        for (j = 1; j < LX1; j++)
        {
            for (i = 1; i < LX1 - 1; i++)
            {
                temp[j][col] = temp[j][col] + qbnew[0][j][i - 1] * tx[i][col];
            }
        }
    }

    col = 0;
    tmor[col][0] = tmor[col][0] + bottom[0];
    for (j = 0; j < LX1; j++)
    {
        for (i = 1; i < LX1 - 1; i++)
        {
            // temp is not divided by 2 here. It includes the contribution
            // from the other conforming face.
            tmor[col][j] = tmor[col][j] + qbnew[0][j][i - 1] * bottom[i] +
                           qbnew[0][j][i - 1] * temp[col][i];
        }
    }

    for (col = 1; col < LX1; col++)
    {
        tmor[col][0] = tmor[col][0] + temp[col][0];
        for (j = 0; j < LX1; j++)
        {
            for (i = 1; i < LX1 - 1; i++)
            {
                tmor[col][j] = tmor[col][j] + qbnew[0][j][i - 1] * temp[col][i];
            }
        }
    }
}


//-------------------------------------------------------------------
// Prepare initial guess for cg. All values from conforming
// boundary are copied and summed on tmor.
//-------------------------------------------------------------------
void transfb_c(double tx[])
{
    const double third = 1.0 / 3.0;
    int il1, il2, il3, il4, ig1, ig2, ig3, ig4, ie, iface, col, j, ig, il;

    r_init(tmort, nmor, 0.0);

    for (ie = 0; ie < nelt; ie++)
    {
        for (iface = 0; iface < NSIDES; iface++)
        {
            if (cbc[ie][iface] != 3)
            {
                il1 = idel[ie][iface][0][0];
                il2 = idel[ie][iface][0][LX1 - 1];
                il3 = idel[ie][iface][LX1 - 1][0];
                il4 = idel[ie][iface][LX1 - 1][LX1 - 1];
                ig1 = idmo[ie][iface][0][0][0][0];
                ig2 = idmo[ie][iface][1][0][0][LX1 - 1];
                ig3 = idmo[ie][iface][0][1][LX1 - 1][0];
                ig4 = idmo[ie][iface][1][1][LX1 - 1][LX1 - 1];

                tmort[ig1] = tmort[ig1] + tx[il1] * third;
                tmort[ig2] = tmort[ig2] + tx[il2] * third;
                tmort[ig3] = tmort[ig3] + tx[il3] * third;
                tmort[ig4] = tmort[ig4] + tx[il4] * third;

                for (col = 1; col < LX1 - 1; col++)
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][col][j];
                        ig = idmo[ie][iface][0][0][col][j];
                        tmort[ig] = tmort[ig] + tx[il];
                    }
                }

                if (idmo[ie][iface][0][0][0][LX1 - 1] == -1)
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][0][j];
                        ig = idmo[ie][iface][0][0][0][j];
                        tmort[ig] = tmort[ig] + tx[il] * 0.5;
                    }
                }

                if (idmo[ie][iface][1][0][1][LX1 - 1] == -1)
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][j][LX1 - 1];
                        ig = idmo[ie][iface][0][0][j][LX1 - 1];
                        tmort[ig] = tmort[ig] + tx[il] * 0.5;
                    }
                }

                if (idmo[ie][iface][0][1][LX1 - 1][1] == -1)
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][LX1 - 1][j];
                        ig = idmo[ie][iface][0][0][LX1 - 1][j];
                        tmort[ig] = tmort[ig] + tx[il] * 0.5;
                    }
                }

                if (idmo[ie][iface][0][0][LX1 - 1][0] == -1)
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][j][0];
                        ig = idmo[ie][iface][0][0][j][0];
                        tmort[ig] = tmort[ig] + tx[il] * 0.5;
                    }
                }
            }
        }
    }
}


//-------------------------------------------------------------------
// Prepare initial guess for CG. All values from conforming
// boundary are copied and summed in tmort.
// mormult is multiplicity, which is used to average tmort.
//-------------------------------------------------------------------
void transfb_c_2(double tx[])
{
    const double third = 1.0 / 3.0;
    int il1, il2, il3, il4, ig1, ig2, ig3, ig4, ie, iface, col, j, ig, il;

    r_init(tmort, nmor, 0.0);
    r_init(mormult, nmor, 0.0);

    for (ie = 0; ie < nelt; ie++)
    {
        for (iface = 0; iface < NSIDES; iface++)
        {

            if (cbc[ie][iface] != 3)
            {
                il1 = idel[ie][iface][0][0];
                il2 = idel[ie][iface][0][LX1 - 1];
                il3 = idel[ie][iface][LX1 - 1][0];
                il4 = idel[ie][iface][LX1 - 1][LX1 - 1];
                ig1 = idmo[ie][iface][0][0][0][0];
                ig2 = idmo[ie][iface][1][0][0][LX1 - 1];
                ig3 = idmo[ie][iface][0][1][LX1 - 1][0];
                ig4 = idmo[ie][iface][1][1][LX1 - 1][LX1 - 1];

                tmort[ig1] = tmort[ig1] + tx[il1] * third;
                tmort[ig2] = tmort[ig2] + tx[il2] * third;
                tmort[ig3] = tmort[ig3] + tx[il3] * third;
                tmort[ig4] = tmort[ig4] + tx[il4] * third;
                mormult[ig1] = mormult[ig1] + third;
                mormult[ig2] = mormult[ig2] + third;
                mormult[ig3] = mormult[ig3] + third;
                mormult[ig4] = mormult[ig4] + third;

                for (col = 1; col < LX1 - 1; col++)
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][col][j];
                        ig = idmo[ie][iface][0][0][col][j];
                        tmort[ig] = tmort[ig] + tx[il];
                        mormult[ig] = mormult[ig] + 1.0;
                    }
                }

                if (idmo[ie][iface][0][0][0][LX1 - 1] == -1)
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][0][j];
                        ig = idmo[ie][iface][0][0][0][j];
                        tmort[ig] = tmort[ig] + tx[il] * 0.5;
                        mormult[ig] = mormult[ig] + 0.5;
                    }
                }

                if (idmo[ie][iface][1][0][1][LX1 - 1] == -1)
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][j][LX1 - 1];
                        ig = idmo[ie][iface][0][0][j][LX1 - 1];
                        tmort[ig] = tmort[ig] + tx[il] * 0.5;
                        mormult[ig] = mormult[ig] + 0.5;
                    }
                }

                if (idmo[ie][iface][0][1][LX1 - 1][1] == -1)
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][LX1 - 1][j];
                        ig = idmo[ie][iface][0][0][LX1 - 1][j];
                        tmort[ig] = tmort[ig] + tx[il] * 0.5;
                        mormult[ig] = mormult[ig] + 0.5;
                    }
                }

                if (idmo[ie][iface][0][0][LX1 - 1][0] == -1)
                {
                    for (j = 1; j < LX1 - 1; j++)
                    {
                        il = idel[ie][iface][j][0];
                        ig = idmo[ie][iface][0][0][j][0];
                        tmort[ig] = tmort[ig] + tx[il] * 0.5;
                        mormult[ig] = mormult[ig] + 0.5;
                    }
                }
            }
        }
    }
}


//------------------------------------------------------------------
// initialize double precision array a with length of n
//------------------------------------------------------------------
void reciprocal(double a[], int n)
{
    int i;


    for (i = 0; i < n; i++)
    {
        a[i] = 1.0 / a[i];
    }
}


//------------------------------------------------------------------
// initialize double precision array a with length of n
//------------------------------------------------------------------
void r_init(double a[], int n, double _cnst)
{
    int i;


    for (i = 0; i < n; i++)
    {
        a[i] = _cnst;
    }
}


//------------------------------------------------------------------
// initialize integer array a with length of n
//------------------------------------------------------------------
void nr_init(int a[], int n, int _cnst)
{
    int i;


    for (i = 0; i < n; i++)
    {
        a[i] = _cnst;
    }
}


//------------------------------------------------------------------
// initialize int array a with length of n
//------------------------------------------------------------------
void l_init(int a[], int n, int _cnst)
{
    int i;


    for (i = 0; i < n; i++)
    {
        a[i] = _cnst;
    }
}


//------------------------------------------------------------------
// copy array of integers b to a, the length of array is n
//------------------------------------------------------------------
void ncopy(int a[], int b[], int n)
{
    int i;


    for (i = 0; i < n; i++)
    {
        a[i] = b[i];
    }
}


//------------------------------------------------------------------
// copy double precision array b to a, the length of array is n
//------------------------------------------------------------------
void copy(double a[], double b[], int n)
{
    int i;


    for (i = 0; i < n; i++)
    {
        a[i] = b[i];
    }
}


//-----------------------------------------------------------------
// a=b*c1
//-----------------------------------------------------------------
void adds2m1(double a[], double b[], double c1, int n)
{
    int i;


    for (i = 0; i < n; i++)
    {
        a[i] = a[i] + c1 * b[i];
    }
}


//-----------------------------------------------------------------
// a=c1*a+b
//-----------------------------------------------------------------
void adds1m1(double a[], double b[], double c1, int n)
{
    int i;


    for (i = 0; i < n; i++)
    {

        a[i] = c1 * a[i] + b[i];

    }
}


//------------------------------------------------------------------
// a=a*b
//------------------------------------------------------------------
void col2(double a[], double b[], int n)
{
    int i;


    for (i = 0; i < n; i++)
    {
        a[i] = a[i] * b[i];
    }
}


//------------------------------------------------------------------
// zero out array of integers
//------------------------------------------------------------------
void nrzero(int na[], int n)
{
    int i;


    for (i = 0; i < n; i++)
    {
        na[i] = 0;
    }
}


//------------------------------------------------------------------
// a=a+b
//------------------------------------------------------------------
void add2(double a[], double b[], int n)
{
    int i;


    for (i = 0; i < n; i++)
    {
        a[i] = a[i] + b[i];
    }
}


//------------------------------------------------------------------
// calculate the integral of ta1 over the whole domain
//------------------------------------------------------------------
double calc_norm()
{
    double total, ieltotal;
    int iel, k, j, i, isize;

    total = 0.0;


    for (iel = 0; iel < nelt; iel++)
    {
        ieltotal = 0.0;
        isize = size_e[iel];
        for (k = 0; k < LX1; k++)
        {
            for (j = 0; j < LX1; j++)
            {
                for (i = 0; i < LX1; i++)
                {
                    ieltotal = ieltotal + ta1[iel][k][j][i] * w3m1[k][j][i]
                               * jacm1_s[isize][k][j][i];
                }
            }
        }
        total = total + ieltotal;
    }

    return total;
}


//-----------------------------------------------------------------
// input array frontier, perform (potentially) parallel add so that
// the output frontier[i] has sum of frontier[1]+frontier[2]+...+frontier[i]
//-----------------------------------------------------------------
void parallel_add(int frontier[])
{
    int nellog, i, ahead, ii, ntemp, n1, ntemp1, n2, iel;

    if (nelt <= 1) return;

    nellog = 0;
    iel = 1;
    do
    {
        iel = iel * 2;
        nellog = nellog + 1;
    }
    while (iel < nelt);

    ntemp = 1;
    for (i = 0; i < nellog; i++)
    {
        n1 = ntemp * 2;
        n2 = n1;
        for (iel = n1; iel <= nelt; iel += n1)
        {
            ahead = frontier[iel - ntemp - 1];


            for (ii = ntemp - 1; ii >= 0; ii--)
            {
                frontier[iel - ii - 1] = frontier[iel - ii - 1] + ahead;
            }
            n2 = iel;
        }
        if (n2 <= nelt) n2 = n2 + n1;

        ntemp1 = n2 - nelt;
        if (ntemp1 < ntemp)
        {
            ahead = frontier[n2 - ntemp - 1];


            for (ii = ntemp - 1; ii >= ntemp1; ii--)
            {
                frontier[n2 - ii - 1] = frontier[n2 - ii - 1] + ahead;
            }
        }

        ntemp = n1;
    }
}



//------------------------------------------------------------------
// Perform stiffness summation: element-mortar-element mapping
//------------------------------------------------------------------
void dssum()
{
    transfb(dpcmor, (double *)dpcelm);
    transf (dpcmor, (double *)dpcelm);
}


//------------------------------------------------------------------
// assign the value val to face(iface,iel) of array a.
//------------------------------------------------------------------
void facev(double a[LX1][LX1][LX1], int iface, double val)
{
    int kx1, kx2, ky1, ky2, kz1, kz2, ix, iy, iz;

    kx1 = 1;
    ky1 = 1;
    kz1 = 1;
    kx2 = LX1;
    ky2 = LX1;
    kz2 = LX1;
    if (iface == 0) kx1 = LX1;
    if (iface == 1) kx2 = 1;
    if (iface == 2) ky1 = LX1;
    if (iface == 3) ky2 = 1;
    if (iface == 4) kz1 = LX1;
    if (iface == 5) kz2 = 1;

    for (ix = kx1 - 1; ix < kx2; ix++)
    {
        for (iy = ky1 - 1; iy < ky2; iy++)
        {
            for (iz = kz1 - 1; iz < kz2; iz++)
            {
                // test 1
                a[iz][iy][ix] = val;
                // test 2
            }
        }
    }
}

void verify(char *Class, int *verified)
{
    double norm, epsilon, norm_dif, norm_ref;

    // tolerance level
    epsilon = 1.0e-08;

    // compute the temperature integral over the whole domain
    norm = calc_norm();

    *verified = 1;
    if ( *Class == 'S' )
    {
        norm_ref = 0.1890013110962E-02;
    }
    else if ( *Class == 'W' )
    {
        norm_ref = 0.2569794837076E-04;
    }
    else if ( *Class == 'A' )
    {
        norm_ref = 0.8939996281443E-04;
    }
    else if ( *Class == 'B' )
    {
        norm_ref = 0.4507561922901E-04;
    }
    else if ( *Class == 'C' )
    {
        norm_ref = 0.1544736587100E-04;
    }
    else if ( *Class == 'D' )
    {
        norm_ref = 0.1577586272355E-05;
    }
    else
    {
        *Class = 'U';
        norm_ref = 1.0;
        *verified = 0;
    }

    norm_dif = fabs((norm - norm_ref) / norm_ref);

    //---------------------------------------------------------------------
    // Output the comparison of computed results to known cases.
    //---------------------------------------------------------------------
    printf("\n");

    if (*Class != 'U')
    {
        printf(" Verification being performed for class %c\n", *Class);
        printf(" accuracy setting for epsilon = %20.13E\n", epsilon);
    }
    else
    {
        printf(" Unknown class\n");
    }

    if (*Class != 'U')
    {
        printf(" Comparison of temperature integrals\n");
    }
    else
    {
        printf(" Temperature integral\n");
    }

    if (*Class == 'U')
    {
        printf("          %20.13E\n", norm);
    }
    else if (norm_dif <= epsilon)
    {
        printf("          %20.13E%20.13E%20.13E\n", norm, norm_ref, norm_dif);
    }
    else
    {
        *verified = 0;
        printf(" FAILURE: %20.13E%20.13E%20.13E\n", norm, norm_ref, norm_dif);
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
