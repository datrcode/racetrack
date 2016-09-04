import java.io.*;
import java.util.*;
import java.awt.geom.Point2D;
import racetrack.util.Utils;

public class EnrichWithGPS {
  class GPS implements Comparable<GPS> {
    long ts; int rec_no; double lat, lon; String line; 
    public GPS(String line, int rec_no, long ts, double lat, double lon) { this.line = line; this.ts = ts; this.lat = lat; this.lon = lon; this.rec_no = rec_no; }
    public GPS(GPS copy) { this.line = copy.line; this.ts = copy.ts; this.lat = copy.lat; this.lon = copy.lon; this.rec_no = copy.rec_no; }
    public int compareTo(GPS other) { if      (other.ts     > ts)     return -1; else if (other.ts     < ts)     return 1; 
                                      else if (other.rec_no > rec_no) return -1; else if (other.rec_no < rec_no) return 1; else return 0; }
  }

  Map<String,List<GPS>> email_lu = new HashMap<String,List<GPS>>();

  public Point2D.Double findLocation(String email, long ts) {
    if (email_lu.containsKey(email)) {
      List<GPS> ls = email_lu.get(email);
      if      (ts < ls.get(0).ts)           return new Point2D.Double(ls.get(0).lon,           ls.get(0).lat);
      else if (ts > ls.get(ls.size()-1).ts) return new Point2D.Double(ls.get(ls.size()-1).lon, ls.get(ls.size()-1).lat);
      else {
        GPS key = new GPS("", 0, ts, 0.0, 0.0);
        int index = Collections.binarySearch(ls, key);
        if (index >= 0) return new Point2D.Double(ls.get(index).lon, ls.get(index).lat); else {
	  index = -(index) - 1;
          // System.out.println("BF " + ls.get(index-1).ts + "\nTS " + ts + "\nAF " + ls.get(index).ts + "\n");
          long ts0 = ls.get(index - 1).ts, ts1 = ls.get(index).ts;
          double ts0_diff = Math.abs(ts - ts0), ts1_diff = Math.abs(ts - ts1);
          if (ts0_diff < ts1_diff) return new Point2D.Double(ls.get(index - 1).lon, ls.get(index - 1).lat);
          else                     return new Point2D.Double(ls.get(index).lon,     ls.get(index).lat);
	}
      }
    } else return null;
  }

  public EnrichWithGPS() throws IOException {
    // Start by loading the GPS information -- make a table to lookup the location of an email @ a time
    BufferedReader in = new BufferedReader(new FileReader("rt_gps.csv")); int rec_no = 0; String hdr;
    String line = hdr = in.readLine(); while ((line = in.readLine()) != null) {
      String tokens[] = tokenize(line); rec_no++; if (tokens.length == 8) {
        long   ts    = Utils.parseTimeStamp(tokens[0]);
	double lat   = Double.parseDouble(tokens[2]),
	       lon   = Double.parseDouble(tokens[3]);
	String email = tokens[4]; if (email.equals("")) email = tokens[1]; // Hack... sets to car id if no email associated to the car
	if (email_lu.containsKey(email) == false) email_lu.put(email, new ArrayList<GPS>());
	email_lu.get(email).add(new GPS(line, rec_no, ts, lat, lon));
        
      } else throw new IOException("rt_gps.csv line has more then eight tokens...");
    } in.close();

    // Sort the records
    Iterator<String> it = email_lu.keySet().iterator();
    while (it.hasNext()) { 
      String email = it.next();
      Collections.sort(email_lu.get(email));
      // for (int i=0;i<20;i++) System.out.println(email_lu.get(email).get(i).ts + " :: " + email_lu.get(email).get(i).rec_no);
    }

    // Fix "Elsa.Orilla's" GPS -- it's messed up... try an average
    List<GPS> recs = email_lu.get("Elsa.Orilla@gastech.com.kronos"); List<GPS> new_recs = new ArrayList<GPS>();
    for (int i=0;i<recs.size();i++) {
      int start = i - 4; if (start < 0) start = 0; if (start > recs.size()-9) start = recs.size()-9;
      double new_lat = 0.0, new_lon = 0.0;
      for (int j=0;j<9;j++) { new_lat += recs.get(start+j).lat; new_lon += recs.get(start+j).lon; }
      GPS new_gps = new GPS(recs.get(i)); new_gps.lat = new_lat/9.0; new_gps.lon = new_lon/9.0; new_recs.add(new_gps);
    }
    email_lu.put("Elsa.Orilla@gastech.com.kronos", new_recs);


    // - Attempt to output with velocity and timestamp_end information
    PrintStream out = new PrintStream(new FileOutputStream("rt_gps.gps.csv"));
    out.println(hdr + ",timestamp_end,MPH");
    it = email_lu.keySet().iterator();
    while (it.hasNext()) {
      String email = it.next(); List<GPS> ls = email_lu.get(email);
      for (int i=0;i<ls.size()-1;i++) {
        GPS t0 = ls.get(i), t1 = ls.get(i+1);
        long   diff  = t1.ts - t0.ts; if (diff == 0L) diff = 500L; // If it's in the same second, make it half a second... didn't see more than an overlap of 2 in the data
        double miles = Utils.calcMiles(t0.lon, t0.lat, t1.lon, t1.lat);
        int    mph   = (int) (miles / (diff / (1000.0*60.0*60.0)));
        out.println(t0.line + "," + Utils.exactDate(t0.ts + diff) + "," + mph);
      } 
      out.println(ls.get(ls.size()-1).line + "," + Utils.exactDate(ls.get(ls.size()-1).ts + 1000L) + ",0");
    }

    // Now we'll enrich each of the additional files
    // - Email
    in = new BufferedReader(new FileReader("rt_email.csv")); out = new PrintStream(new FileOutputStream("rt_email.gps.csv"));
    line = in.readLine();
    out.println(line + ",latitude,longitude");
    while ((line = in.readLine()) != null) {
      String tokens[] = tokenize(line); String email = tokens[1]; long ts = Utils.parseTimeStamp(tokens[0]);
      Point2D.Double geo = findLocation(email,ts);
      if (geo == null) out.println(line + ",,"); else out.println(line + "," + geo.getY() + "," + geo.getX());
    }
    out.close(); in.close();

    // - Load the loyalty data
    Map<String,Map<String,Map<String,Set<String>>>> loyal_lu = new HashMap<String, Map<String, Map<String, Set<String>>>>();
    in = new BufferedReader(new FileReader("rt_loyal.csv"));
    line = in.readLine(); while ((line = in.readLine()) != null) {
      String tokens[] = tokenize(line); String email = tokens[1], timestamp = tokens[0], price = tokens[4], location = tokens[6];
      String yyyymmdd = (new StringTokenizer(timestamp, " ")).nextToken();
      if (loyal_lu.containsKey(email)                             == false) loyal_lu.put(email, new HashMap<String, Map<String, Set<String>>>());
      if (loyal_lu.get(email).containsKey(yyyymmdd)               == false) loyal_lu.get(email).put(yyyymmdd, new HashMap<String,Set<String>>());
      if (loyal_lu.get(email).get(yyyymmdd).containsKey(location) == false) loyal_lu.get(email).get(yyyymmdd).put(location, new HashSet<String>());

      if (loyal_lu.get(email).get(yyyymmdd).get(location).contains(price)) System.err.println("Price Already Exists...");

      loyal_lu.get(email).get(yyyymmdd).get(location).add(price);
    } in.close();

    // - Transform the credit card data
    in = new BufferedReader(new FileReader("rt_ccdat.csv")); out = new PrintStream(new FileOutputStream("rt_ccdat.gps.csv"));
    line = in.readLine(); out.println(line + ",loyalty,latitude,longitude");
    while ((line = in.readLine()) != null) {
      String tokens[] = tokenize(line); String email = tokens[1], timestamp = tokens[0], price = tokens[4], location = tokens[6];
      String yyyymmdd = (new StringTokenizer(timestamp, " ")).nextToken(); long ts = Utils.parseTimeStamp(timestamp);

      boolean loyalty_match = false;

      if (loyal_lu.containsKey(email) &&
          loyal_lu.get(email).containsKey(yyyymmdd) &&
          loyal_lu.get(email).get(yyyymmdd).containsKey(location) &&
          loyal_lu.get(email).get(yyyymmdd).get(location).contains(price)) {
        loyalty_match = true;
        loyal_lu.get(email).get(yyyymmdd).get(location).remove(price);
      }

      Point2D.Double geo = findLocation(email, ts);
      if (geo == null) out.println(line + "," + loyalty_match + ",,"); 
      else             out.println(line + "," + loyalty_match + "," + geo.getY() + "," + geo.getX());
    } out.close(); in.close();
  }

  /**
   *
   */
  public static void main(String args[]) { try { new EnrichWithGPS(); } catch (IOException ioe) { } }

  public static String[] tokenize(String line) {
    StringTokenizer st = new StringTokenizer(line, ",", true);
    List<String>    ls = new ArrayList<String>(); while (st.hasMoreTokens()) ls.add(st.nextToken());
    if (ls.size() == 0) return new String[0];

    if (ls.get(0).          equals(",")) ls.add(0, "");
    if (ls.get(ls.size()-1).equals(",")) ls.add("");
    int i = 0; while (i < ls.size()-1) {
      if (ls.get(i).equals(",") && ls.get(i+1).equals(",")) ls.add(i+1, "");
      i++;
    }
    String strs[] = new String[1 + ls.size()/2];
    for (i=0;i<strs.length;i++) strs[i] = Utils.decFmURL(ls.get(i*2));
    return strs;
  }
}
