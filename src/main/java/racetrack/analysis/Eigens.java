/* 

Copyright 2013 David Trimm

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/

package racetrack.analysis;

import java.io.PrintStream;

/**
 * Class to hold an eigen value and an eigen vector.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class Eigens {
  public double val;
  public double vec[];

  /**
   * Print out the values in this class;
   */
  public void print(PrintStream out) {
    if (vec != null) {
      out.println("Val = " + val);
      for (int i=0;i<vec.length;i++) out.println("V[" + i + "] = " + vec[i]);
    }
  }

  /**
   * Power iteration method...  assumes a lot but mostly that the results will be non-imaginary.
   * Based on wikipedia entry:  http://wikipedia.org/wiki/Power_iteration
   *
   *@param A matrix to derive the largest eigenvector
   *
   *@return eigen value and eigen vector
   */
  public static Eigens powerIterate(double A[][]) {
    int n = A.length;
    Eigens eigens = new Eigens(); eigens.vec = new double[n];

    // Ranomize eigens.vec (b in wiki) and normalize the value
    double norm_sq = 0.0;
    for (int i=0;i<n;i++) { double r = Math.random(); eigens.vec[i] = r; norm_sq = r*r; }
    norm_sq = Math.sqrt(norm_sq); for (int i=0;i<n;i++) { eigens.vec[i] /= norm_sq; }

    // Iterate until it converges
    int iterations = 0; double last_val = 0.0; boolean done = false; while (!done) {
      // Calculate the matrix-by-vector product Ab
      double tmp[] = new double[n];
      for (int i=0;i<n;i++) { tmp[i] = 0.0; for (int j=0;j<n;j++) tmp[i] += A[i][j] * eigens.vec[j]; }
      
      // Calculate the length of the resultant
      norm_sq = 0.0; for (int k=0;k<n;k++) norm_sq += tmp[k]*tmp[k];
      norm_sq = Math.sqrt(norm_sq);

      // Normalize and assign to the eigenvecs / value
      eigens.val = norm_sq;
      for (int i=0;i<n;i++) { eigens.vec[i] = tmp[i] / norm_sq; }

      // Run for x iterations or until the value doesn't change much
      iterations++; 
      if ((iterations > 1000) || (iterations > 3 && Math.abs(last_val - norm_sq) < 0.000000000001)) { done = true; }
      last_val = norm_sq;
      // eigens.print(System.out); // Debug
    }
    // System.err.println("Iterations = " + iterations);

    return eigens;
  }

  /**
   * Deflate the specified matrix by the specified eigen vector using the
   * Hotelling Deflation method for symmetrix matrices.
   *
   *@param A     matrix to deflate
   *@param eigen eigen vector for deflation
   */
  public static void hotellingDeflate(double A[][], Eigens eigen) {
    int n = A.length;
    for (int i=0;i<n;i++) 
      for (int j=0;j<n;j++)
        A[i][j] -= eigen.val * eigen.vec[i] * eigen.vec[j];
  }

  /**
   * Test method for eigen value calculations.
   */
  public static void main(String args[]) {
    //
    // First test
    //
    double A[][] = new double[3][3];
    A[0][0]           = 7;
    A[1][0] = A[0][1] = 4;
    A[2][0] = A[0][2] = 1;
    A[1][1]           = 4;
    A[1][2] = A[2][1] = 4;
    A[2][2]           = 7;
    
    System.out.println("**\n** Test 1 v=12, vec[x] = 1/sqrt(3)\n**");
    powerIterate(A).print(System.out); // Should converge to value of 12 and 1/sqrt(3) for all vec els

    //
    // Second test
    //
    A = new double[3][3];
    A[0][0] =  4; A[0][1] = -1; A[0][2] =  1;
    A[1][0] = -1; A[1][1] =  3; A[1][2] = -2;
    A[2][0] =  1; A[2][1] = -2; A[2][2] =  3;

    System.out.println("**\n** Test 2 v=6, vec = [1 -1 1]\n**");
    powerIterate(A).print(System.out); // Should converge to value of 5 and vec of [1 1 0 0]

    //
    // Second test
    //
    A = new double[3][3];
    A[0][0] =  2; A[0][1] = -1; A[0][2] = -1;
    A[1][0] = -1; A[1][1] =  2; A[1][2] =  1;
    A[2][0] = -1; A[2][1] =  1; A[2][2] =  4;

    System.out.println("**\n** Test 3 v=5, vec = [-1 1 2]\n**");
    powerIterate(A).print(System.out); // Should converge to value of 5 and vec of [1 1 0 0]
  }

}
