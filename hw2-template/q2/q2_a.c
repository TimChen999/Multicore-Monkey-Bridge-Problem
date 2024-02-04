#include <stdio.h>
#include <omp.h>
#include <stdlib.h>
#include <string.h>

void MatrixMult(char file1[], char file2[], int T)
{
    // Write your code here
    // T is num of threads, matix mult is mn * nx = mx, divide n of matrix by T (which part each thread calculates is dependent on ID)

    //Open file
    FILE *fileOne = fopen("file1", "r");
    FILE *fileTwo = fopen("file2", "r");

    //first array is m * n
    int m;
    fscanf(fileOne, "%d", &m);

    int n;
    fscanf(fileOne, "%d", &n);
    fscanf(fileTwo, "%d", &n); //Should return same value

    //Second array is n * x
    int x;
    fscanf(fileTwo, "%d", &x); 

    //Save first matrix to array format
    double mat1[m][n];
    for(int i = 0; i < m; i++){
        for(int j = 0; j < n; j++){
            fscanf(fileOne, "%f", &mat1[i][j]);

            //Print debug
            //printf("%f", mat1[i][j]);
        }
    }

    //Save second matrix to array format
    double mat2[n][x];
    for(int i = 0; i < n; i++){
        for(int j = 0; j < x; j++){
            fscanf(fileTwo, "%f", &mat1[i][j]);

            //Print debug
            //printf("%f", mat2[i][j]);
        }
    }

    //Create resulting matrix
    int matRes[m][x];

    //Number of n per thread
    int nPerThread = n/T;
    int remainder = n % T;

    //Start and end "n" values for each thread [start, end] based on ID index, start inclusive, end exclusive 
    int nThread[T][2];
    int current = 0;
    for(int i = 0; i < T; i++){
        //Set start for thread ID "i"
        nThread[i][0] = current;
        if(i < remainder){
            current = current + nPerThread + 1;
        }
        else{
            current = current + nPerThread;
        }
        //Set end for thread ID "i"
        nThread[i][1] = current;
    }

    //Create threads
    omp_set_num_threads(T);
    #pragma omp parallel //Parallel section
    {

        //Determine index for which "n" part each thread solves
        int ID = omp_get_thread_num();
        
        //Calculate array for each thread
        for(int i = 0; i < m; i++){
            for(int j = 0; j < x; j++){
                //C(i,j) = A(i,1) * B(1,j) + A(i,2) * B(2,j) + ... + A(i,n) * B(n,j)
                int current_c = 0;

                //Calculate from start and end for value "n" assigned to current thread
                for(int k = nThread[ID][0]; k < nThread[ID][1]; k++){
                    current_c =+ mat1[i][k] + mat2[k][j];
                }

                //Only one thread can add to the resulting matrix at a time
                #pragma omp atomic
                matRes[i][j] =+ current_c;
            }
        }
    }

    //Return result once all threads finish
    #pragma omp barrier
    return matRes;

}

void main(int argc, char *argv[])
{
    char *file1, *file2;
    file1 = argv[1];
    file2 = argv[2];
    int T = atoi(argv[3]);
    MatrixMult(file1, file2, T);
}

/* Calculation plan:
        Matrix 1: m*n
        a11 a12 a13 a14
        a21 a22 a23 a24
        a31 a32 a33 a34

        Matrix 2: n*x
        b11 b12
        b21 b22
        b31 b32
        b41 b42

        Result matrix: m*x
        c11 c12
        c21 c22
        c31 c32

        Result calculations:
        c11 = a11*b11 + a12*b21 + a13*b31 + a14*b41
        c12 = a11*b12 + a12*b22 + a13*b32 + a14*b42

        c21 = a21*b11 + a22*b21 + a23*b31 + a24*b41
        c22 = a21*b12 + a22*b22 + a23*b32 + a24*b42

        c31 = a31*b11 + a32*b21 + a33*b31 + a34*b41
        c32 = a31*b12 + a32*b22 + a33*b32 + a34*b42

        Threading: Lets say there are 2 threads to calculate everything
        (int) 4/2 = 2 (2 "n" per thread)

        T(0) Calculates (n = 1,2):
        c11 = a11*b11 + a12*b21 
        c12 = a11*b12 + a12*b22 

        c21 = a21*b11 + a22*b21
        c22 = a21*b12 + a22*b22 

        c31 = a31*b11 + a32*b21
        c32 = a31*b12 + a32*b22 

        T(1) Calculates (n = 3,4):
        c11 = a13*b31 + a14*b41
        c12 = a13*b32 + a14*b42

        c21 = a23*b31 + a24*b41
        c22 = a23*b32 + a24*b42

        c31 = a33*b31 + a34*b41
        c32 = a33*b32 + a34*b42

        Remainder: If "n" does not divide evenly by threads, first #"remainder" threads do an extra "n"
        
        Result: add "c" values from each thread to get final "c" values
    */