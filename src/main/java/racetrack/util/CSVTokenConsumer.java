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
 * Interface to consume tokens from the CSV Reader.
 */
public interface CSVTokenConsumer { 
  /**
   * Consume the tokens from the csv reader.
   *
   *@param tokens  array of strings separated by commas from the original file
   *@param line    original line from the file
   *@param line_no line number
   *
   *@return false indicates that the parser should halt parsing (optional)
   */
  public boolean consume(String tokens[], String line, int line_no); 

  /**
   * Process a comment line from the CSV File.
   *@param line    comment line
   */
  public void commentLine(String line);
}
