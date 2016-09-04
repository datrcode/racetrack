/**
 * Most of implementation from "the-lost-beauty.blogspot.com/2009/04/moore-penrose-pseudoinverse-in-jama.html"
 * - Ahmed Abdelkader
 */
package racetrack.graph;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 *
 */
class ResistiveDistance {
  Matrix moore_penrose_inv;

  /**
   * Version for graph
   */
  public ResistiveDistance(MyGraph graph) {
    // Initialize the matrix
    int n = graph.getNumberOfEntities();
    double G[][] = new double[n][n];

    // Add the connections
    for (int i=0;i<n;i++) for (int j=0;j<graph.getNumberOfNeighbors(i);j++) {
      int nbor = graph.getNeighbor(i,j);
      G[i][nbor] = -graph.getConnectionWeight(i,nbor);
    }

    // Set the diagonals
    for (int i=0;i<n;i++) {
      double sum = 0;
      for (int j=0;j<n;j++) {
        if (i == j) continue;
        sum += (-G[i][j]);
      }
      G[i][i] = sum;
    }

    // Calculate the Moore-Pensore Pseudoinverse
    moore_penrose_inv = pinv(new Matrix(G));
  }

  /**
   * Version for distance matrix
   */
  public ResistiveDistance(double dist[][]) {
    int n = dist.length; double G[][] = new double[n][n];
    for (int i=0;i<n;i++) {
      for (int j=0;j<n;j++) {
        if      (i == j)                        G[i][j] = 0.0;
	else if (Double.isInfinite(dist[i][j])) G[i][j] = 0.0;
	else                                    G[i][j] = dist[i][j];
      }
    }

    // Set the diagonals
    for (int i=0;i<n;i++) {
      double sum = 0;
      for (int j=0;j<n;j++) {
        if (i == j) continue;
        sum += (-G[i][j]);
      }
      G[i][i] = sum;
    }

    // Calculate the Moore-Pensore Pseudoinverse
    moore_penrose_inv = pinv(new Matrix(G));
  }

  /**
   * From "the-lost-beauty.blogspot.com/2009/04/moore-penrose-pseudoinverse-in-jama.html"
   * - Ahmed Abdelkader
   */
  /**
   * The difference between 1 and the smallest exactly representable number
   * greater than one. Gives an upper bound on the relative error due to
   * rounding of floating point numbers.
   */
  public static double MACHEPS = 2E-16;
  /**
   * Updates MACHEPS for the executing machine.
   */
  public static void updateMacheps() {
   MACHEPS = 1;
   do
    MACHEPS /= 2;
   while (1 + MACHEPS / 2 != 1);
  }
  /**
   * Computes the Moore–Penrose pseudoinverse using the SVD method.
   * 
   * Modified version of the original implementation by Kim van der Linde.
   */
  public static Matrix pinv(Matrix x) {
   if (x.rank() < 1)                                 return null;
   if (x.getColumnDimension() > x.getRowDimension()) return pinv(x.transpose()).transpose();
   SingularValueDecomposition svdX = new SingularValueDecomposition(x);
   double[] singularValues = svdX.getSingularValues();
   double tol = Math.max(x.getColumnDimension(), x.getRowDimension()) * singularValues[0] * MACHEPS;
   double[] singularValueReciprocals = new double[singularValues.length];
   for (int i = 0; i < singularValues.length; i++)
    singularValueReciprocals[i] = Math.abs(singularValues[i]) < tol ? 0 : (1.0 / singularValues[i]);
   double[][] u = svdX.getU().getArray();
   double[][] v = svdX.getV().getArray();
   int min = Math.min(x.getColumnDimension(), u[0].length);
   double[][] inverse = new double[x.getColumnDimension()][x.getRowDimension()];
   for (int i = 0; i < x.getColumnDimension(); i++)
    for (int j = 0; j < u.length; j++)
     for (int k = 0; k < min; k++)
      inverse[i][j] += v[i][k] * singularValueReciprocals[k] * u[j][k];
   return new Matrix(inverse);
  }

  /**
   *
   */
  public double d(int i,int j) {
    return Math.abs(moore_penrose_inv.get(i,i) + moore_penrose_inv.get(j,j) - 2 * moore_penrose_inv.get(i,j));
  }

  /**
   *
   */
  public static void main(String args[]) {
    //
    // Construct the graph from Page 212 of the Cohen Paper
    //
    System.out.println("**\n** Example From Page 212 of Cohen Paper...\n**");

    SimpleMyGraph smg = new SimpleMyGraph();
    smg.addNeighbor("A","B",2.0); smg.addNeighbor("B","A",2.0);
    smg.addNeighbor("A","C",2.0); smg.addNeighbor("C","A",2.0);
    smg.addNeighbor("B","C",1.0); smg.addNeighbor("C","B",1.0);
    smg.addNeighbor("B","D",3.0); smg.addNeighbor("D","B",3.0);

    ResistiveDistance rd = new ResistiveDistance(smg);
    System.out.println("B ==> D = " + rd.d(smg.getEntityIndex("B"), smg.getEntityIndex("D")) + " (Should Be 1/3)");
    System.out.println("B ==> C = " + rd.d(smg.getEntityIndex("B"), smg.getEntityIndex("C")) + " (Should Be 1/2)");

    //
    // Construct an example of a node with one degree neighbors
    //
    System.out.println("**\n** Constructing Simple One Degree Neighbor Graph...\n**");

    smg = new SimpleMyGraph();
    smg.addNeighbor("A","B",10.0); smg.addNeighbor("B","A",10.0);
    smg.addNeighbor("B","C", 8.0); smg.addNeighbor("C","B", 8.0);
    smg.addNeighbor("B","D", 5.5); smg.addNeighbor("D","B", 5.5);
    smg.addNeighbor("C","E",11.0); smg.addNeighbor("E","C",11.0);
    smg.addNeighbor("D","E",13.0); smg.addNeighbor("E","D",13.0);
    smg.addNeighbor("E","F", 2.0); smg.addNeighbor("F","E", 2.0);

    rd = new ResistiveDistance(smg);
    System.out.println("A => B = " + rd.d(smg.getEntityIndex("A"), smg.getEntityIndex("B")));
    System.out.println("A => C = " + rd.d(smg.getEntityIndex("A"), smg.getEntityIndex("C")));
    System.out.println("A => D = " + rd.d(smg.getEntityIndex("A"), smg.getEntityIndex("D")));
    System.out.println("A => E = " + rd.d(smg.getEntityIndex("A"), smg.getEntityIndex("E")));
    System.out.println("A => F = " + rd.d(smg.getEntityIndex("A"), smg.getEntityIndex("F")));

    System.out.println("B => C = " + rd.d(smg.getEntityIndex("B"), smg.getEntityIndex("C")));
    System.out.println("B => D = " + rd.d(smg.getEntityIndex("B"), smg.getEntityIndex("D")));
    System.out.println("B => E = " + rd.d(smg.getEntityIndex("B"), smg.getEntityIndex("E")));
    System.out.println("B => F = " + rd.d(smg.getEntityIndex("B"), smg.getEntityIndex("F")));
  }
}

