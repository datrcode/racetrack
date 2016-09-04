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
package racetrack.util;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Used for a variety of purposes related to line intersections.  The code is based on the
 * write-ups found at the following site:  {@link http://www.topcoder.com/tc?module=Static&d1=tutorials&d2=geometry1}
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class LineIntersection {
  /**
   * Find the closest point on a line segment to the specified point.
   *
   *@param  px specific point's x coordinate
   *@param  py specific point's y coordinate
   *@param  x0 x coordinate for one end of line segment
   *@param  y0 y coordinate for one end of line segment
   *@param  x1 x coordinate for the other end of the line segment
   *@param  y1 y coordinate for the other end of the line segment
   *@return closest point on the line segment to the specified point
   */
  public static Point2D.Double intersectionFromPointToLineSegment(double px, double py,
                                                                  double x0, double y0,
                                                                  double x1, double y1) {

    if (x0 == x1 && y0 == y1) return new Point2D.Double(x0,y0);

    //
    // x = x0 + t0 * (x1 - x0) = x2 + t1 * (x3 - x2)
    // y = y0 + t0 * (y1 - y0) = y2 + t1 * (y3 - y2)
    //

    double x2 = px, y2 = py;
    double dx = x1 - x0, dy = y1 - y0;
    double x3 = x2 + dy, y3 = y2 - dx;

    double A = y3 - y2, B = x3 - x2, C = x1 - x0, D = y1 - y0;
    double denom = B * D - A * C;
    if (denom == 0.0) { 
      x3 = x2 - dy; 
      y3 = y2 + dx;
      A = y3 - y2; 
      B = x3 - x2; 
      C = x1 - x0; 
      D = y1 - y0;
      denom = B * D - A * C;
      if (denom == 0.0) {
        System.out.println("Still Bad Bad Bad"); System.exit(0);
      }
    }

    double t0 = (A*(x0 - x2) - B*(y0 - y2))/denom;
    if (t0 >= 0.0 && t0 <= 1.0) {
      double x    = x0 + t0 * (x1 - x0);
      double y    = y0 + t0 * (y1 - y0);
      return new Point2D.Double(x,y);
    } else if (Utils.distance(px,py,x0,y0) < Utils.distance(px,py,x1,y1)) {
      return new Point2D.Double(x0,y0);
    } else {
      return new Point2D.Double(x1,y1);
    }
  }

  /**
   * Recast version using Point2D objects.
   *
   *@param  point specified point
   *@param  end0  line segment end
   *@param  end1  line segment other end
   *@return       closest point on the line segment to specified point
   */
  public static Point2D.Double intersectionFromPointToLineSegment(Point2D.Double point,
                                                                  Point2D.Double end0,
                                                                  Point2D.Double end1) {
    return intersectionFromPointToLineSegment(point.getX(), point.getY(),
                                              end0.getX(),  end0.getY(),
                                              end1.getX(),  end1.getY());
  }

  /**
   * Return the closest distance from point to a line segment.
   *
   *@param  px   points x coordinate
   *@param  py   points y coordinate
   *@param  line line segment
   *@return      closest distance from the point to the line segment
   */
  public static double distanceFromPointToLineSegment(double px, double py, Line2D line) {
    return distanceFromPointToLineSegment(px,py,line.getX1(),line.getY1(),line.getX2(),line.getY2());
  }

  /**
   * Return the closest distance from point to a line segment.
   *
   *@param  px specific point's x coordinate
   *@param  py specific point's y coordinate
   *@param  x0 x coordinate for one end of line segment
   *@param  y0 y coordinate for one end of line segment
   *@param  x1 x coordinate for the other end of the line segment
   *@param  y1 y coordinate for the other end of the line segment
   *@return      closest distance from the point to the line segment
   */
  public static double distanceFromPointToLineSegment(double px, double py,
                                                      double x0, double y0,
                                                      double x1, double y1) {
    Point2D.Double point = intersectionFromPointToLineSegment(px,py,x0,y0,x1,y1);
    return distance(px,py,point.getX(),point.getY());
  }

  /**
   * Determine the distance between two points.
   *
   *@param  x0 x coord of point 0
   *@param  y0 y coord of point 0
   *@param  x1 x coord of point 1
   *@param  y1 y coord of point 1
   *@return    distance between point 0 and point 1
   */
  public static double distance(double x0, double y0, double x1, double y1) {
    double dx = x1 - x0, dy = y1 - y0; return Math.sqrt(dx*dx+dy*dy);
  }

  /**
   * Determine the distance between two points.
   *
   *@param  p0 point 0
   *@param  p1 point 1
   *@return    distance between p0 and p1
   */
  public static double distance(Point2D p0, Point2D p1) {
    return distance(p0.getX(),p0.getY(),p1.getX(),p1.getY());
  }

  /**
   * Calculate the dot product between two vectors
   *
   *@return cross product
   */
  public static double crossProduct(double ax, double ay, double bx, double by, double cx, double cy) {
    double ab0 = bx - ax,
           ab1 = by - ay,
           ac0 = cx - ax,
           ac1 = cy - ay;
    return ab0*ac1-ab1*ac0;
  }

  /**
   * Calculate the dot product between two vectors
   *
   *@return dot product
   */
  public static double dotProduct(double ax, double ay, double bx, double by, double cx, double cy) {
    double ab0 = bx - ax,
           ab1 = by - ay,
           bc0 = cx - bx,
           bc1 = cy - by;
    return ab0*bc0+ab1*bc1;
  }

  /**
   * Determine if two segments intersect.
   *
   *@param x0 segment 1 x coord
   *@param y0 segment 1 y coord
   *@param x1 segment 1 other x coord
   *@param y1 segment 1 other y coord
   *@param x2 segment 1 x coord
   *@param y2 segment 1 y coord
   *@param x3 segment 1 other x coord
   *@param y3 segment 1 other y coord
   *@return true if the segements intersect, false otherwise
   */
  public static boolean        intersects(double x0, double y0,
                                          double x1, double y1,
                                          double x2, double y2,
                                          double x3, double y3) {
    return intersectionPoint(x0,y0,x1,y1,x2,y2,x3,y3) != null;
  }

  /**
   * Calculate the intersection point between two line segments.
   *
   *@line0_0 segment 1 end
   *@line0_1 segment 1 other end
   *@line1_0 segment 2 end
   *@line1_1 segment 2 other end
   *@return intersection point if it exists, null otherwise
   */
  public static Point2D.Double intersectionPoint(Point2D.Double line0_0, Point2D.Double line0_1,
                                                 Point2D.Double line1_0, Point2D.Double line1_1) {
    return intersectionPoint(line0_0.getX(), line0_0.getY(),
                             line0_1.getX(), line0_1.getY(),
                             line1_0.getX(), line1_0.getY(),
                             line1_1.getX(), line1_1.getY());
  }

  /**
   * Calculate the intersection point between two line segments.
   *
   *@param x0 segment 1 x coord
   *@param y0 segment 1 y coord
   *@param x1 segment 1 other x coord
   *@param y1 segment 1 other y coord
   *@param x2 segment 1 x coord
   *@param y2 segment 1 y coord
   *@param x3 segment 1 other x coord
   *@param y3 segment 1 other y coord
   *@return intersection point if it exists, null otherwise
   */
  public static Point2D.Double intersectionPoint(double x0, double y0,
                                                 double x1, double y1,
                                                 double x2, double y2,
                                                 double x3, double y3) {
    //
    // x = x0 + t0 * (x1 - x0) = x2 + t1 * (x3 - x2)
    // y = y0 + t0 * (y1 - y0) = y2 + t1 * (y3 - y2)
    //
    double A = y3 - y2, B = x3 - x2, C = x1 - x0, D = y1 - y0;
    double denom = B * D - A * C;
    if (denom == 0.0) return null;

    double t0 = (A*(x0 - x2) - B*(y0 - y2))/denom;
    if (t0 >= 0.0 && t0 <= 1.0) {
      double x    = x0 + t0 * (x1 - x0);
      double y    = y0 + t0 * (y1 - y0);
      double t1   = (x - x2)/(x3 - x2);
      if (t1 >= 0.0 && t1 <= 1.0) return new Point2D.Double(x,y);
             t1   = (y - y2)/(y3 - y2);
      if (t1 >= 0.0 && t1 <= 1.0) return new Point2D.Double(x,y);
    }
    return null;
  }
}

