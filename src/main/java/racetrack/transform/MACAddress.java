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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import racetrack.framework.BundlesDT;
import racetrack.util.CacheManager;

/**
 * Class to handle MAC address conversions.
 *
 * @author D. Trimm
 * @version 0.1
 */
public class MACAddress {
    /**
     * The singleton instance for the MACAddress class. Used to conserve resources
     * and to guarantee that only one instance exists.
     */
    private static MACAddress instance = null;

    /**
     * Lookup table to convert six hex digits to organization
     */
    private Map<String, String> oui_lu = new HashMap<String, String>();

    /**
     * Private constructor for this class. Created so that only a single
     * instance of this class will be loaded.
     */
    private MACAddress() {
        try {
            // Start with the shape file
            System.err.print("Loading MAC Address Data...  ");

	    // Get the directories
            URI source_uri = MACAddress.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File dir         = new File(source_uri);
            File macs_dir  = new File(dir,      "macs");
            File oui_file  = new File(macs_dir, "oui.psv");

            if (oui_file.exists() == false) { System.err.println("OUI File Not Found \"" + oui_file + "\""); return; }

	    BufferedReader in = null;
	    try {
	     in = new BufferedReader(new FileReader(oui_file));
	     String line;
	     while ((line = in.readLine()) != null) {
               if (line.indexOf("|") > 0) {
	         String mac = line.substring(0,line.indexOf("|"));
		 String org = line.substring(line.indexOf("|")+1,line.length());
		 oui_lu.put(mac.toLowerCase(),org);
	       } else System.err.println("MACAddress Lookup Loader:  Ignoring Line \"" + line + "\"");
	     }
	    } finally { if (in != null) in.close(); }
            System.err.println("Done!");
        } catch (IOException ioe) {
            System.err.println("MACAddress Constructor:  Exception: " + ioe);
            ioe.printStackTrace(System.err);
        } catch (Throwable t) {
            System.err.println("MACAddress Constructor:  Throwable: " + t);
            t.printStackTrace(System.err);
	} finally {
            // Register caches
	    // CacheManager.registerCache("MAC Address Lookup", oui_lu); // The cache never gets filled again 2015-06-11
        }
    }

    /**
     * Return the registered organization for a mac address.
     * Source: http://standards.ieee.org/develop/regauth/oui/oui.txt
     *
     *@param mac mac address (6 hex pairs)
     *
     *@return organizational designator
     */
    public String getOrg(String mac) {
      StringTokenizer st = new StringTokenizer(mac, "-:");

      String first3 = null;
      if      (st.countTokens() ==  6) first3 = st.nextToken() + st.nextToken() + st.nextToken();
      else if (mac.length()     == 12) first3 = mac.substring(0,6);
      else                             return BundlesDT.NOTSET;

      first3 = first3.toLowerCase();
      if (oui_lu.containsKey(first3)) return oui_lu.get(first3); 
      else                            return BundlesDT.NOTSET;
    }

    /**
     * Return the singleton for the MACAddress class. Used to prevent multiple
     * instances from being created and consuming too many resources.
     *
     * @return singleton instance of MACAddress class
     */
    public static MACAddress getInstance() {
        if (instance == null) { instance = new MACAddress(); }
        return instance;
    }
}

