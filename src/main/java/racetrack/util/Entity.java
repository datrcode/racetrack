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

import racetrack.framework.BundlesDT;

/**
 * Entity subtext (extracted substring from a textblock).
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class Entity       extends SubText {
  /**
   * Datatype of entity
   */
  BundlesDT.DT datatype;

  /**
   * Constructor for the entity.
   *
   *@param entity   entity string
   *@param datatype application-layer datatype
   *@param i0       initial index of substring
   *@param i1       ending index of substring
   */
  public Entity(String full, String entity, BundlesDT.DT datatype, int i0, int i1) {
    super(full, entity, i0, i1); this.datatype = datatype;
  }

  /**
   * Get the string representation of the subtext type.
   *
   *@return subtext type, i.e., "ENTITY
   */
  @Override
  public String       getType()     { return "ENTITY"; }

  /**
   * Get the application-layer data type.
   *
   *@return datatype
   */
  public BundlesDT.DT getDataType() { return datatype; }
}
