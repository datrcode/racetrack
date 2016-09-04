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

import java.awt.Color;
import java.awt.Graphics2D;

import java.awt.image.BufferedImage;

import java.io.FileOutputStream;

import javax.imageio.ImageIO;


/**
 * Classical MDS implementation.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class ClassicalMDS {
  /**
   * Dissimilarity matrix
   */
  double d[][];

  /**
   * Two dimensional embedding of the data
   */
  double results[][];

  /**
   * Constructor.
   *
   *@param d dissimilarity matrix (distances) - should be symmetric (d will be unmodified)
   *
   */
  public ClassicalMDS(double d[][]) {
    this.d = d;
    // Square D and find the col/row and global means
    double row_means[] = new double[d.length];
    double col_means[] = new double[d[0].length];
    double sum         = 0.0;
    for (int y=0;y<d.length;y++) for (int x=0;x<d[y].length;x++) {
      double square = d[y][x] * d[y][x];
      col_means[x] += square;
      row_means[y] += square;
      sum += square;
    }
    for (int i=0;i<col_means.length;i++) col_means[i] /= col_means.length;
    for (int i=0;i<row_means.length;i++) row_means[i] /= row_means.length;
    sum /= (d.length * d[0].length);

    // Double center
    double b[][] = new double[d.length][d[0].length];
    for (int y=0;y<d.length;y++) for (int x=0;x<d[y].length;x++) {
      b[y][x] = -0.5 * (d[y][x]*d[y][x] - col_means[x] - row_means[y] + sum);
    }

    Eigens first  = Eigens.powerIterate(b); lam1 = first.val;  lam1_v = first.vec;
    Eigens.hotellingDeflate(b, first);
    Eigens second = Eigens.powerIterate(b); lam2 = second.val; lam2_v = second.vec;

    // Multiply through to get the coordinates
    results = new double[lam1_v.length][2]; 
    for (int i=0;i<results.length;i++) {
      results[i][0] = Math.sqrt(lam1) * lam1_v[i];
      results[i][1] = Math.sqrt(lam2) * lam2_v[i];
    }
  }

  /**
   * Largest eigenvalues
   */
  double lam1, lam2;

  /**
   * Largest eigenvectors
   */
  double lam1_v[], lam2_v[];

  /**
   * Return the nth largest eigenvalue.  Only works for n is {0,1}.
   *
   *@param n nth largest parameter (zero based)
   *
   *@return nth largest eigenvalue
   */
  public double   getEigenValue (int n) { if (n == 0) return lam1;   else return lam2;   }

  /**
   * Return the nth largest eigenvector.  Only works for n is {0,1}.
   *
   *@param n nth largest parameter (zero based)
   *
   *@return nth largest eigenvector
   */
  public double[] getEigenVector(int n) { if (n == 0) return lam1_v; else return lam2_v; }

  /**
   * Return the results.
   *
   *@return two dimensional array of coordinates
   */
  public double[][] getResults() { return results; }

  /**
   * Sample test main with US city distances.
   */
  public static void main(String args[]) {
    // Following derived from web information on airport locations
String info [][] = {{"32.6","85.4","Auburn","AL"},
                    {"33.5","86.7","Birmingham","AL"},
                    {"35.9","112.1","Grand Canyon","AZ"},
                    {"33.4","112.0","Phoenix","AZ"},
                    {"32.1","110.9","Tucson","AZ"},
                    {"35.2","92.3","Little Rock","AR"},
                    {"33.9","118.4","Los Angeles","CA"},
                    {"38.5","121.5","Sacramento","CA"},
                    {"37.7","122.6","San Francisco","CA"},
                    {"39.7","104.8","Denver","CO"},
                    {"41.7","72.6","Hartford","CT"},
                    {"39.1","75.4","Dover","DE"},
                    {"38.9","77.4","Washington/Dulles","DC"},
                    {"28.4","80.5","Cape Canaveral","FL"},
                    {"29.1","81.0","Daytona","Bch","FL"},
                    {"24.5","81.7","Key West","FL"},
                    {"25.6","80.4","Miami","FL"},
                    {"28.4","81.3","Orlando","FL"},
                    {"30.2","85.6","Panama","City","FL"},
                    {"30.4","87.2","Pensacola","FL"},
                    {"33.6","84.4","Atlanta","GA"},
                    {"32.1","81.2","Savannah","Mun","GA"},
                    {"41.9","87.6","Chicago","IL"},
                    {"29.9","90.2","New Orleans","LA"},
                    {"43.8","69.9","Brunswick","ME"},
                    {"43.6","70.3","Portland","ME"},
                    {"39.1","76.6","Baltimore","MD"},
                    {"42.3","71.0","Boston","MA"},
                    {"42.4","83.0","Detroit","MI"},
                    {"36.0","115.1","Las Vegas","NV"},
                    {"39.4","74.5","Atlantic City","NJ"},
                    {"35.0","106.6","Albuquerque","NM"},
                    {"40.7","73.9","New York","NY"},
                    {"45.6","122.6","Portland","OR"},
                    {"33.9","81.1","Columbia","SC"},
                    {"35.0","85.2","Chattanooga","TN"},
                    {"32.9","97.0","Dallas/FW","TX"},
                    {"37.5","77.3","Richmond","VA"},
                    {"47.4","122.3","Seattle","WA"},
                    {"42.9","87.9","Milwaukee","WI"} };
    // Create the distance matrix
    double d[][] = new double[info.length][info.length];
    for (int i=0;i<info.length;i++) { for (int j=0;j<info.length;j++) {
        if (i == j) continue;
        double x0 = Double.parseDouble(info[i][1]),
	       y0 = Double.parseDouble(info[i][0]),
	       x1 = Double.parseDouble(info[j][1]),
	       y1 = Double.parseDouble(info[j][0]);
        double dx = x1 - x0, 
	       dy = y1 - y0;
         d[i][j] = Math.sqrt(dx*dx + dy*dy);
    } }

    // Run the computation
    ClassicalMDS mds = new ClassicalMDS(d);

    // Get the results and calculate mins and maxes
    double coords[][] = mds.getResults();
    double x_min      = coords[0][0], y_min      = coords[0][1], x_max      = coords[0][0], y_max      = coords[0][1];
    for (int i=0;i<coords.length;i++) {
      if (x_min > coords[i][0]) x_min = coords[i][0]; if (y_min > coords[i][1]) y_min = coords[i][1];
      if (x_max < coords[i][0]) x_max = coords[i][0]; if (y_max < coords[i][1]) y_max = coords[i][1];
    }

    // Render an image and write it to disk
    try {
      BufferedImage bi  = new BufferedImage(512,512,BufferedImage.TYPE_INT_RGB);
      Graphics2D    g2d = (Graphics2D) bi.getGraphics();
      g2d.setColor(Color.white); g2d.fillRect(0,0,bi.getWidth(),bi.getHeight());
      g2d.setColor(Color.black); for (int i=0;i<coords.length;i++) {
        int sx = (int) ((bi.getWidth()  * (coords[i][0] - x_min))/(x_max - x_min)),
	    sy = (int) ((bi.getHeight() * (coords[i][1] - y_min))/(y_max - y_min));
        g2d.fillRect(sx,sy,2,2);
      }
      g2d.dispose();
      ImageIO.write(bi, "png", new FileOutputStream("classical_mds_test.png"));
    } catch (Throwable t) { System.err.println("Throwable: " + t); }
  }
}



