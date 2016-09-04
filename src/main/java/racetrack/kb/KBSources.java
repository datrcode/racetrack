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
package racetrack.kb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import racetrack.framework.BundlesDT;

/**
 *
 */
public class KBSources {
  /**
   *
   */
  private static Map<BundlesDT.DT, Map<String, KBSource>> source_lu = new HashMap<BundlesDT.DT, Map<String, KBSource>>();

  /**
   * Initialize the knowledge base sources.
   */
  public static void initializeDataSources() {
  }

  /**
   * Get a list of sources by datatype.
   *
   *@param datatype looking for sources that satisfy this specific data type
   *
   *@return list of data sources for that data type
   */
  public static String[] getSourceList(BundlesDT.DT datatype) {
    if (source_lu.containsKey(datatype)) {
      List<String>     list = new ArrayList<String>();
      Iterator<String> it   = source_lu.get(datatype).keySet().iterator();
      while (it.hasNext()) list.add(it.next());
      String strs[] = new String[list.size()];
      for (int i=0;i<strs.length;i++) strs[i] = list.get(i);
      return strs;
    } else return new String[0];
  }
  
  /**
   * Return the specific data source by name.
   *
   *@param datatype    particular datatype
   *@param source_name name of source
   *
   *@return data source
   */
  public static KBSource getSource(String source_name, BundlesDT.DT datatype) {
    if (source_lu.containsKey(datatype)) {
      if (source_lu.get(datatype).containsKey(source_name)) {
        return source_lu.get(datatype).get(source_name);
      } else return null;
    } else return null;
  }
}

interface KBSource {
}

