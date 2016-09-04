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
 * Interface to implement a progress dialog without the
 * monitored method having to worry about gui methods or
 * dependencies.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public interface UpdateMonitor {
  /**
   * Issue an updated status message.
   *
   *@param str status message
   */
  public void updateStatusMessage(String str);
  /**
   * Update the progress on the dialog
   *
   *@param perc percentage finished
   */
  public void updateProgress(float perc);
  /**
   * Issue an error message to the dialog.
   *
   *@param error_msg error message
   */
  public void updateError(String error_msg);
}
