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

package racetrack.graph;

/**
 * Straightforward interface encapsulating transforms from world to screen.
 *
 *@author  D Trimm
 *@version 1.0
 */
public interface WorldToScreenTransform {
  /**
   * Transform the world x coordinate to screen space.
   *
   *@param wx world x coordinate
   *
   *@return screen x coordinate
   */
  public int wxToSx(double wx);

  /**
   * Transform the world y coordinate to screen space.
   *
   *@param wy world y coordinate
   *
   *@return screen y coordinate
   */
  public int wyToSy(double wy);
}
