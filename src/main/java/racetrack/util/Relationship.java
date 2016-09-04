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

/**
 * Relationship subclass of subtext.  Not implemented.
 */
public class Relationship extends SubText {
  /**
   * Construct a relationship subtext.  Not implemented.
   *
   *@param relationship_str string describing relationship (sub verb pred)
   *@param i0               initial index in text
   *@param i1               ending index in text
   */
  public Relationship(String full, String relationship_str, int i0, int i1) { super(full, relationship_str, i0, i1); }

  /**
   * Return the type of subtext.
   *
   *@return subtext type, i.e., "RELATIONSHIP"
   */
  @Override
  public String getType() { return "RELATIONSHIP"; }
}
