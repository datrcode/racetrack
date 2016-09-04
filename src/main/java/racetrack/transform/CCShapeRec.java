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
package racetrack.transform;

import racetrack.visualization.ShapeRecord;

/**
 * Holding class to associate countries, country codes, and their
 * shape representation.
 *
 * @author  D. Trimm
 * @version 1.0
 */
public class CCShapeRec {
  /**
   * Country name
   */
  String       name;

  /**
   * ISO-2 country code
   */
  String       cc;

  /**
   * Country's shape record
   */
  ShapeRecord  shaperec;

  /**
   * Create the associated record for the country name, code, and shape.
   */
  public CCShapeRec(String name, String cc, ShapeRecord shaperec) {
    this.name = name; this.cc = cc; this.shaperec = shaperec;
  }

  /**
   * Return the country name.
   *
   * @return country name
   */
  public String      getName()     { return name; }

  /**
   * Return the country code.
   *
   * @return country code
   */
  public String      getCC()       { return cc;   }

  /**
   * Return the country's shape record.
   *
   * @return country shape record
   */
  public ShapeRecord getShapeRec() { return shaperec; }
}
