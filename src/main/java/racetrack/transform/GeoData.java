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

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import racetrack.framework.BundlesDT;
import racetrack.gui.RTGraphPanel;
import racetrack.util.CSVReader;
import racetrack.util.CSVTokenConsumer;
import racetrack.util.CacheManager;
import racetrack.visualization.ShapeFile;
import racetrack.visualization.ShapeRecord;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

/**
 * Class to handle geo conversions on the various data types. Heavily leverages
 * maxmind's geo API interface for the IPv4 conversions. Class also handles
 * geospatial shapes for geospatial rendering components.
 *
 * @author D. Trimm
 * @version 1.0
 */
public class GeoData {

    /**
     * The singleton instance for the GeoData class. Used to conserve resources
     * and to guarantee that only one instance exists.
     */
    private static GeoData instance = null;
    /**
     * Shape files related to geospatial data (country borders).
     */
    private ShapeFile shapefile;
    /**
     * Lookup table to convert a country code to a geospatial shape.
     */
    private Map<String, Integer> cc_to_shaperec = new HashMap<String, Integer>();
    /**
     * Lookup table to convert a country code to the full country name.
     */
    private Map<String, String> cc_to_name = new HashMap<String, String>();
    /**
     * Lookup service for converting IPv4 address to geospatial coordinates and
     * country codes.
     */
    private LookupService ipv4_geo_service = null,
    /**
     * Lookup service for converting IPv4 addresses to organizations and
     * ASN's.
     */
            ipv4_asn_service = null;
    /**
     * Flag to indicate that the geo service should be enabled (if the data files are present)
     */
    private static boolean geo_service_enabled = true;

    /**
     * Disable the geo service.  Needs to be called prior to the creation of the service.
     */
    public static void disableGeoService() { geo_service_enabled = false; }

    /**
     * Cache for country code lookups
     */
    private Map<String, String> cc_lu = new HashMap<String, String>();

    /**
     * Cache for asn lookups
     */
    private Map<String, String> asn_lu = new HashMap<String, String>();

    /**
     * Cache for organization lookups
     */
    private Map<String, String> org_lu = new HashMap<String, String>();

    /**
     * Return the set of country codes that were loaded during the initiation of
     * the class.
     *
     * @return set of the country codes identified in the shape records
     */
    public Set<String> countryCodes() {
        if (shapefile == null) { loadShapeFile(); }
        return cc_to_shaperec.keySet();
    }

    /**
     * Method to delay the loading of the world shape files.  By delaying the load, the
     * shape files will only take up memory if the user needs this feature.
     */
    private synchronized void loadShapeFile() {
      if (shapefile != null) return; // Check to see if a blocked instance already loaded the shapes while this method was waiting
      try {
	    // Get the directories
            URI source_uri = RTGraphPanel.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File dir         = new File(source_uri);
            File shapes_dir  = new File(dir, "shapes");
            File shapes_file = new File(shapes_dir, "ne_10m_admin_0_countries.shp");
            File data_file   = new File(shapes_dir, "ne_10m_admin_0_countries.csv");

            // File shapes_file = new File(getClass().getResource("/shapes/ne_10m_admin_0_countries.shp").getFile());
	    if (shapes_file.exists() == false) { System.err.print(" [File Not Found] "); } else { System.err.print(" Exists "); }
            System.err.print("Shapes...  ");
            shapefile = new ShapeFile(shapes_file);

            // Load the country mappings
	    if (data_file.exists() == false) { System.err.println(" [File Not Found] "); } else { System.err.print(" Exists "); }
            System.err.print("CC Mapping...  ");
            new CSVReader(data_file, new Consumer(), "|");
      } catch (IOException ioe) {
            System.err.println("**\n** Error Loading World Shape Files...\n**");
            System.err.println("IOException: " + ioe); ioe.printStackTrace(System.err);
      } catch (Throwable t) {
            System.err.println("**\n** Error Loading World Shape Files...\n**");
            System.err.println("Throwable: " + t); t.printStackTrace(System.err);
      }
    }

    /**
     * Private constructor for this class. Created so that only a single
     * instance of this class will be loaded.
     */
    private GeoData() {
        try {
            // Start with the shape file
            System.err.print("Loading Geospatial Data...  ");

	    // Get the directories
            URI source_uri = RTGraphPanel.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File dir         = new File(source_uri);
            File geoip_dir   = new File(dir, "geoip");
            File geoip_file  = new File(geoip_dir, "GeoLiteCity.dat"); if (!geoip_file.exists())  System.err.println("No GeoIP  File: " + geoip_file);
            File geoasn_file = new File(geoip_dir, "GeoIPASNum.dat");  if (!geoasn_file.exists()) System.err.println("No GeoASN File: " + geoasn_file);

            // Create the IP lookup service
            System.err.print("GeoIP...  ");
            if (geoip_file.exists() && geo_service_enabled) {
                try {
                    ipv4_geo_service = new LookupService(geoip_file, LookupService.GEOIP_MEMORY_CACHE);
                } catch (NoClassDefFoundError ncdfe) {
                    System.err.print("ERROR-No GeoIP Service: " + ncdfe);
                }
            } else {
                System.err.print("ERROR-No GeoIP File  ");
            }
            // Create the Org lookup service
            System.err.print("GeoASN...  ");
            if (geoasn_file.exists() && geo_service_enabled) {
                try {
                    ipv4_asn_service = new LookupService(geoasn_file);
                } catch (NoClassDefFoundError ncdfe) {
                    System.err.print("ERROR-No GeoOrg Service: " + ncdfe);
                }
            } else {
                System.err.print("ERROR-No GeoOrg File  ");
            }
            // Register caches
	    CacheManager.registerCache("GeoIP ASN Lookup", asn_lu);
	    CacheManager.registerCache("GeoIP CC Lookup",  cc_lu);
	    CacheManager.registerCache("GeoIP Org Lookup", org_lu);

            System.err.println("  Done! (geospatial load)");
        } catch (IOException ioe) {
            System.err.println("GeoData Constructor:  Exception: " + ioe);
            ioe.printStackTrace(System.err);
        } catch (Throwable t) {
            System.err.println("GeoData Constructor:  Throwable: " + t);
            t.printStackTrace(System.err);
	}
    }

    /**
     * For a specific data type and string value, return the geolocation point.
     * These lookups are not cached because the point information is only looked
     * up in layout operations which are infrequent / only occur once.
     *
     * @param datatype datatype for the string parameter
     * @param str parameter to convert to geospatial coordinates
     * @return geospatial coordinates
     */
    public Point2D geoLocate(BundlesDT.DT datatype, String str) {
        if (datatype == BundlesDT.DT.IPv4 && ipv4_geo_service != null) {
            Location location = ipv4_geo_service.getLocation(str);
            if (location == null) {
                return null;
            }
            // System.err.println("Location For " + str + " : lon=" + location.longitude + " lat=" + location.latitude + " cc=" + location.countryCode);
            return new Point2D.Double(location.longitude, location.latitude);
        } else {
            return new Point2D.Double(0.0, 0.0);
        }
    }

    /**
     * For a specific data type and string value, return the country code
     * lookup. Note that lookups are cached...
     *
     * @param datatype datatype for the string parameter
     * @param str parameter to convert to geospatial coordinates
     * @return country code
     */
    public String geoCC(BundlesDT.DT datatype, String str) {
        if (cc_lu.containsKey(str) == false && datatype == BundlesDT.DT.IPv4 && ipv4_geo_service != null) {
            Location location = ipv4_geo_service.getLocation(str);
            if (location != null) {
                cc_lu.put(str, location.countryCode);
            } else {
                cc_lu.put(str, BundlesDT.NOTSET);
            }
        }
        return cc_lu.get(str);
    }

    /**
     * For a specific data type and string value, return the ASN... probably
     * only makes sense for IPv4, IPv4CIDR, and IPv6... Lookups are cached.
     *
     * @param datatype datatype for the string parameter
     * @param str parameter to convert to geospatial coordinates
     * @return autonomous system number (ASN)
     */
    public String geoASN(BundlesDT.DT datatype, String str) {
        if (asn_lu.containsKey(str) == false && datatype == BundlesDT.DT.IPv4 && ipv4_asn_service != null) {
            String asn = ipv4_asn_service.getOrg(str);
            if (asn != null && asn.indexOf(" ") >= 0) {
                asn_lu.put(str, asn.substring(0, asn.indexOf(" ")));
            } else {
                asn_lu.put(str, BundlesDT.NOTSET);
            }
        }
        return asn_lu.get(str);
    }

    /**
     * For a specific data type and string value, return the associated
     * organization. May only make sense for IPv4, IPv6, and CIDR types...
     * Lookups are cached.
     *
     * @param datatype datatype for the string parameter
     * @param str parameter to convert to geospatial coordinates
     * @return organizational designator
     */
    public String geoOrg(BundlesDT.DT datatype, String str) {
        if (org_lu.containsKey(str) == false && datatype == BundlesDT.DT.IPv4 && ipv4_asn_service != null) {
            String asn = ipv4_asn_service.getOrg(str);
            if (asn != null && asn.indexOf(" ") >= 0) {
                org_lu.put(str, asn.substring(asn.indexOf(" ") + 1, asn.length()));
            } else {
                org_lu.put(str, BundlesDT.NOTSET);
            }
        }
        return org_lu.get(str);
    }

    /**
     * For a specific data type, return whether geodata is available.
     *
     * @param datatype datatype to lookup for geospatial data
     * @return true if geospatial data supports that datatype
     */
    public boolean geoDataAvailable(BundlesDT.DT datatype) {
        if (datatype == BundlesDT.DT.IPv4) {
            return (ipv4_geo_service != null);
        } else {
            return false;
        }
    }

    /**
     * Consumer for the CSVTokenConsumer interface for loading the lookups
     * between country codes, country names, and shapefile information.
     */
    class Consumer implements CSVTokenConsumer {

        /**
         * Original header from the file
         */
        String header[];
        /**
         * Monotonically increasing shape record numbers
         */
        int shape_rec_no = 1;
        /**
         * Column index for the country codes
         */
        int cc_i,
                /**
                 * Column index for the country names
                 */
                name_i;

        /**
         * Find the header index for strings that start with a specific
         * substring.
         *
         * @param starts_with substring to use for matches
         * @return column index of the first match
         */
        public int findHeader(String starts_with) {
            for (int i = 0; i < header.length; i++) {
                if (header[i].startsWith(starts_with)) {
                    return i;
                }
            }
            throw new RuntimeException("Could not find header that starts with \"" + starts_with + "\"... no geo available");
        }

        /**
         * Parse the tokens for the lookup tables.
         *
         * @param tokens string array from the csv tokenizer
         * @param line original line in the csv file
         * @param line_no line number
         */
        public boolean consume(String tokens[], String line, int line_no) {
            if (header == null) {
                header = tokens;
                name_i = findHeader("name,");
                cc_i = findHeader("iso_a2,");
            } else {
                String name = tokens[name_i];
                String cc = tokens[cc_i];
                if (cc.equals("-99")) {
                    // System.err.println("No Country Code For \"" + name + "\" ==> cc is \"" + cc + "\"");
                } else {
                    // Check some stuff
                    if (cc_to_shaperec.containsKey(cc)) {
                        System.err.println("Country \"" + name + "\n (" + cc + ") Already In Map...");
                    }
                    if (shapefile.getShape(shape_rec_no) == null) {
                        System.err.println("Shape for \"" + name + "\n (" + cc + ") Is Null...");
                    }

                    cc_to_shaperec.put(cc, shape_rec_no);
                    cc_to_name.put(cc, name);
                }
                shape_rec_no++;
            }
          return true; // Keep parsing
        }

        /**
         * Process comment line (none in this data stream...)
         *
         *@param line comment line
         */
        public void commentLine(String line) { }
    }

    /**
     * Return the singleton for the GeoData class. Used to prevent multiple
     * instances from being created and consuming too many resources.
     *
     * @return singleton instance of GeoData class
     */
    public static GeoData getInstance() {
        if (instance == null) {
            instance = new GeoData();
        }
        return instance;
    }

    /**
     * Check to see if a geodata instance has been loaded.
     *
     * @return true if the data has been loaded already
     */
    public static boolean isInstanceLoaded() {
        return (instance != null);
    }

    /**
     * Return the geospatial shape file. Useful when the entire map needs to be
     * drawn.
     *
     * @return {@link ShapeFile} representing all of the country borders
     */
    public ShapeFile getShapeFile() {
        if (shapefile == null) { loadShapeFile(); }
        return shapefile;
    }
    /**
     * Data structure to efficiently determine which country borders to check
     * for a given point.
     */
    Map<String, Set<String>> square_to_ccs = null;

    /**
     * Create a map to limit which country codes are checked for a given point.
     * For example, larger countries entirely fill up geospatial squares and it
     * doesn't make sense to exhaustively check all country codes for a given
     * point.
     */
    private void createSpaceMap() {
        square_to_ccs = new HashMap<String, Set<String>>();
        for (int x = -180; x < 180; x += 10) {
            for (int y = -90; y < 180; y += 10) {
                String key = x + "," + y;
                Rectangle2D rect = new Rectangle2D.Double(x - 1, y - 1, 12, 12); // Make it a little bigger... for coverage
                Iterator<String> it_cc = cc_to_shaperec.keySet().iterator();
                while (it_cc.hasNext()) {
                    String cc = it_cc.next();
                    int shape_rec_no = cc_to_shaperec.get(cc);
                    ShapeRecord shape_rec = shapefile.getShape(shape_rec_no);
                    if (shape_rec.getBounds().intersects(rect)) {
                        if (square_to_ccs.containsKey(key) == false) {
                            square_to_ccs.put(key, new HashSet<String>());
                        }
                        square_to_ccs.get(key).add(cc);
                    }
                }
            }
        }
    }

    /**
     * For a set of points, return the country code shapes that contain those
     * points. Useful for only drawing countries that matter for] the data set
     * at hand.
     *
     * @param map map of string to points to lookup
     * @return set of shape records that contain the specified points
     */
    public Set<CCShapeRec> containingCountries(Map<String, Point2D> map) {
        if (shapefile == null) { loadShapeFile(); }
        // Create the space map (one time only)
        if (square_to_ccs == null) {
            createSpaceMap();
        }
        // Create the temporary set of cc
        Set<String> ccs = new HashSet<String>();
        // Go through the map finding ccs that match
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            Point2D pt = map.get(key);
            pt = new Point2D.Double(pt.getX(), -pt.getY()); // Have to make it reverse upside-down...
            if (pt.getX() >= -180 && pt.getX() <= 180 && pt.getY() >= -90 && pt.getY() <= 90) {
                int x = (int) pt.getX(), y = (int) pt.getY();
                // System.err.print("  Before (" + x + "," + y + ")...  mods=" + (x%10) + "," + (y%10) + "... ");
                if (x >= 0) {
                    x = x - (x % 10);
                } else {
                    x = x - (10 + x % 10);
                }
                if (y >= 0) {
                    y = y - (y % 10);
                } else {
                    y = y - (10 + y % 10);
                }
                // System.err.println(" => " + x + "," + y);
                String squares_key = x + "," + y;
                if (square_to_ccs.containsKey(squares_key)) {
                    Iterator<String> it_cc = square_to_ccs.get(squares_key).iterator();
                    while (it_cc.hasNext()) {
                        String cc = it_cc.next();
                        if (ccs.contains(cc)) {
                            continue;
                        }
                        if (shapefile.getShape(cc_to_shaperec.get(cc)).contains(pt)) {
                            ccs.add(cc);
                        }
                    }
                }
            }
        }
        // Convert the ccs into shaperecs...
        Set<CCShapeRec> set = new HashSet<CCShapeRec>();
        Iterator<String> it_cc = ccs.iterator();
        while (it_cc.hasNext()) {
            String cc = it_cc.next();
            int shape_rec_no = cc_to_shaperec.get(cc);
            ShapeRecord shape_rec = shapefile.getShape(shape_rec_no);
            String name = cc_to_name.get(cc);
            set.add(new CCShapeRec(name, cc, shape_rec));
        }
        return set;
    }

    /**
     * For a specific country code, return the shape record that represents that
     * country.
     *
     * @param cc country code
     * @return corresponding shape record
     */
    public CCShapeRec getCCShapeRec(String cc) {
        if (shapefile == null) { loadShapeFile(); }
        if (cc_to_shaperec.containsKey(cc) == false) {
            return null;
        }
        int shape_rec_no = cc_to_shaperec.get(cc);
        ShapeRecord shape_rec = shapefile.getShape(shape_rec_no);
        String name = cc_to_name.get(cc);
        return new CCShapeRec(name, cc, shape_rec);
    }
}
