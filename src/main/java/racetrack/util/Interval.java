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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interval subtext implementation.  Intervals represent two
 * separate timeframe defining a duration.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class Interval     extends SubText {
  /**
   * Beginning time in milliseconds
   */
  long ts0, 
  /**
   * Ending time in milliseconds
   */
       ts1;
  /**
   * Construct an interval object.  Parse the substring into timestamps.
   *
   *@param interval_str textual representation of two times.
   *@param i0           initial index in text
   *@param i1           ending index in text`
   */
  public Interval(String full, String interval_str, int i0, int i1) { 
    super(full, interval_str, i0, i1); 
    Pattern pattern = Pattern.compile(Utils.getTimeStampRegex());
    Matcher matcher = pattern.matcher(toString());
    matcher.find(); ts0 = Utils.parseTimeStamp(matcher.group());
    matcher.find(); ts1 = Utils.parseTimeStamp(matcher.group());
  }

  /**
   * Return the type of substring as a string.
   *
   *@return type, i.e., "INTERVAL"
   */
  @Override
  public String getType() { return "INTERVAL"; }

  /**
   * Return the early timestamp
   *
   *@return begin time in millis
   */
  public long   getMinTimeStamp() { return ts0; }

  /**
   * Return the later timestamp
   *
   *@return end time in millis
   */
  public long   getMaxTimeStamp() { return ts1; }
}
