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
package racetrack.visualization;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Interface for a shape record (for the {@link ShapeFile}) 
 */
public interface ShapeRecord { 
  /**
   * Return the boundaries for the shapes within this record.
   *
   *@return boundary
   */
  public Rectangle2D getBounds();
  /**
   * Return the number of shapes within this record.
   *
   *@return number of shapes
   */
  public int         getNumberOfShapes();
  /**
   * Return the shape at the specified index
   *
   *@param i index 
   *
   *@return shape at index
   */
  public Shape       getShape(int i);
  /**
   * Determine if a point falls within the shapes in this record.
   *
   *@param pt point to check
   *
   *@return true if  point is within shapes
   */
  public boolean     contains(Point2D pt);
  /**
   * Draw (not fill) the shapes using the specified graphics primitive.  Note that
   * transformations should be applied to the graphics object prior to callling this
   * method.
   *
   *@param g2d graphics primitive
   */
  public void        draw(Graphics2D g2d); 
  /**
   * Fill the shapes using the specified graphics primitive.  Note that
   * transformations should be applied to the graphics object prior to callling this
   * method.
   *
   *@param g2d graphics primitive
   */
  public void        fill(Graphics2D g2d);
}
