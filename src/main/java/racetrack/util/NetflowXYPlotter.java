/* 

Copyright 2016 David Trimm

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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import racetrack.framework.Bundle;
import racetrack.framework.Bundles;
import racetrack.framework.BundlesRecs;
import racetrack.framework.BundlesUtils;

import racetrack.visualization.StatsOverlay;

/**
 * Plot an XY scatter plot for the netflow files.
 */
public class NetflowXYPlotter {
  /**
   * Flag indicating that the low port should always be considered the destination
   */
  boolean low_port = false;

  /**
   * Model the duration of flows in the chart
   */
  boolean model_duration = false;

  /**
   * Slice the plot by minutes (force)
   */
  boolean minutes_scale   = false,
  /**
   * Slice the plot by seconds (force)
   */
          seconds_scale   = false,
  /**
   * Slice the plot by hours (force)
   */
	  hours_scale     = false,
  /**
   * Slice the plot by 4 hours (force)
   */
          hours4_scale    = false,
  /**
   * Slice the plot by 5 minutes (force)
   */
	  minutes5_scale  = false,
  /**
   * Slice the plot by 10 minutes (force)
   */
	  minutes10_scale = false,
  /**
   * Slice the plot by days (force)
   */
	  days_scale      = false;

  /**
   * Timestamp to start with (forced)
   */
  long    ts0_force = 0L,

  /**
   * Timestamp to end with (forced)
   */
          ts1_force = 0L;

  /**
   * Construct the plotter... parse the args for flag information
   */
  public NetflowXYPlotter(String args[]) throws IOException {
    List<File> files_al = new ArrayList<File>();

    int i = 0;
    while (i <args.length) {
      if        (args[i].equals("-lowport"))   { low_port       = true; i++;
      } else if (args[i].equals("-duration"))  { model_duration = true; i++;
      } else if (args[i].equals("-minutes"))   { minutes_scale  = true; i++; // Force granularity to be a minute
      } else if (args[i].equals("-seconds"))   { seconds_scale  = true; i++; // .. a second
      } else if (args[i].equals("-hours"))     { hours_scale    = true; i++; // .. an hour
      } else if (args[i].equals("-hours4"))    { hours4_scale   = true; i++; // .. 4 hours
      } else if (args[i].equals("-5minutes"))  { minutes5_scale = true; i++; // .. 5 minutes
      } else if (args[i].equals("-10minutes")) { minutes10_scale= true; i++; // .. 10 minutes
      } else if (args[i].equals("-days"))      { days_scale     = true; i++; // .. days...
      } else if (args[i].equals("-begin") && i <= (args.length-2) && Utils.isTimeStamp(args[i+1])) { ts0_force = Utils.parseTimeStamp(args[i+1]); i += 2;
      } else if (args[i].equals("-end")   && i <= (args.length-2) && Utils.isTimeStamp(args[i+1])) { ts1_force = Utils.parseTimeStamp(args[i+1]); i += 2;
      } else if ((new File(args[i])).exists()) { files_al.add(new File(args[i])); i++;
      } else throw new RuntimeException("Do Not Understand Argument \"" + args[i] + "\"");
    }

    // Copy files over to a fixed array
    files = new File[files_al.size()]; for (i=0;i<files.length;i++) files[i] = files_al.get(i);
  }

  /**
   * Files for parsing
   */
  File files[];

  /**
   * Return the valid files from the command line.
   *
   *@return files that were listed on the command line
   */
  public File[] validFiles() {
    File ret[] = new File[files.length]; System.arraycopy(files, 0, ret, 0, ret.length);
    return ret;
  }

  /**
   * Return a unique, consistent string representing an IP Pair.
   *
   *@param sip source ip
   *@param dip destination ip
   *
   *@return string combining the sip and dip
   */
  public String ipPairsKey(String sip, String dip) { return sip + " > " + dip; }

  /**
   * Aggregated information
   */
  Aggregator aggregator = null;

  /**
   * Aggregates state information about the data.  State is then used to derive the render context for a
   * second pass of the data.
   */
  class Aggregator implements CSVTokenConsumer {
    /**
     * Best flavor for all the files
     */
    String best_flavor = null;

    /**
     * Convert a string int an integer representing the quality of the netflow.
     *
     *@param f netflow favor as defined in the @StatsOverlay class
     *
     *@return integer representing netflow flavor in increasing order of quality
     */
    public int flavorInt(String f) {
      if (f != null) { if      (f.equals(StatsOverlay.NETFLOW_FULL))        return 100;
                       else if (f.equals(StatsOverlay.NETFLOW_MOSTLY_FULL)) return 80;
                       else if (f.equals(StatsOverlay.NETFLOW_VOLUME))      return 60;
                       else if (f.equals(StatsOverlay.NETFLOW_DEFAULT))     return 40;
                       else if (f.equals(StatsOverlay.NETFLOW_MINIMAL))     return 20;
                       else throw new RuntimeException("Do Not Understand Netflow Flavor \"" + f + "\""); } else return 0;
    }

    /**
     * Convert an integer into a netflow flavor.
     *
     *@param f_i integer netflow as returned by flavorInt()
     *
     *@return corresponding netflow string
     */
    public String intFlavor(int f_i) {
      switch (f_i) { case 100:  return StatsOverlay.NETFLOW_FULL;
                     case 80:   return StatsOverlay.NETFLOW_MOSTLY_FULL;
		     case 60:   return StatsOverlay.NETFLOW_VOLUME;
		     case 40:   return StatsOverlay.NETFLOW_DEFAULT;
		     case 20:   return StatsOverlay.NETFLOW_MINIMAL;
		     default:   return null; }
    }

    /**
     * return the lowest common flavor between the two.  Not really sure this will work correctly...
     * would need to do a little more analysis to make sure the netflow flavors are correctly ordered
     * and are subsets of one another.
     *
     *@param f1 flavor 1
     *@param f2 flavor 2
     *
     *@return lowest common flavor
     */
    public String lowestCommonFlavor(String f1, String f2) {
      int f1_i = flavorInt(f1), f2_i = flavorInt(f2);
      if (f1_i < f2_i) return intFlavor(f1_i);
      else             return intFlavor(f2_i);
    }

    /**
     * Given two netflow flavors (possibly including null values), return the better quality version string
     *
     *@param f1 flavor 1
     *@param f2 flavor 2
     *
     *@return best of the two flavors
     */
    public String chooseBestFlavor(String f1, String f2) {
      int f1_i = flavorInt(f1), f2_i = flavorInt(f2);
      if (f1_i > f2_i) return intFlavor(f1_i);
      else             return intFlavor(f2_i);
    }

    /**
     * Construct and run the aggregator...
     *
     *@param files files to consume for aggregation
     */
    public Aggregator(File files[]) throws IOException {
      // Determine the best overall flavor...
      System.err.println("**\n** Aggregator:  Determing Best Flavor...\n**");
      for (int file_i=0;file_i<files.length;file_i++) {
	// Load a small sample of the file
        File file = files[file_i];
	Bundles                        bundles = new BundlesRecs();
        Set<Bundle>                    set     = BundlesUtils.parse(bundles, file, 10);

        // Pick the best flavor ... implicit feeling that all the files have the same flavor... if not, bad things will transpire
        Map<String,Map<String,String>> flavors = StatsOverlay.dataFlavors(set); String local_best_flavor = null;
        System.err.print("For File \"" + file + "\"... Records Pre-Proc = " + bundles.bundleSet().size());
        Iterator<String> it = flavors.keySet().iterator(); while (it.hasNext()) {
          String flavor = it.next(); System.err.print(" | " + flavor);
          if (flavor.startsWith(StatsOverlay.NETFLOW_PREFIX)) local_best_flavor = chooseBestFlavor(flavor, local_best_flavor);
        }
	if (best_flavor == null) best_flavor = local_best_flavor;
	else                     best_flavor = lowestCommonFlavor(local_best_flavor, best_flavor);
        System.err.println();
      }

      // Keep track of the flavor mapping on a per file basis
      System.err.println("**\n** Aggregator:  Tracking Flavor Per File\n**");
      for (int file_i=0;file_i<files.length;file_i++) {
	// Load a small sample of the file
        File file = files[file_i];
	Bundles                        bundles = new BundlesRecs();
        Set<Bundle>                    set     = BundlesUtils.parse(bundles, file, 10);
        Map<String,Map<String,String>> flavors = StatsOverlay.dataFlavors(set); String local_best_flavor = null;
	if (flavors.containsKey(best_flavor)) flavor_lu.put(file, flavors.get(best_flavor));
	else                                  throw new RuntimeException("File \"" + file + "\" Cannot Be Flavor \"" + best_flavor + "\"");
      }

      // Parse the files and accumulate key information to construct the render context
      System.err.println("**\n** Aggregator:  Accumulating Info...\n**");
      for (int file_i=0;file_i<files.length;file_i++) {
        File file = files[file_i]; begin_file = true; trans = flavor_lu.get(file);
	System.err.println("Parsing File \"" + file + "\"...");
	new CSVReader(file,this);
	System.err.println("  Totals (SIPS:" + sips.size() + " , DIPS:" + dips.size() + ", PAIRS:" + pairs.size() + ") " + Utils.exactDate(ts0) + " to " + Utils.exactDate(ts1));
	file_trans_lu.put(file, trans_i);
      }
    }

    /**
     * Flavor translations on a per file basis
     */
    Map<File,Map<String,String>> flavor_lu = new HashMap<File,Map<String,String>>();

    /**
     * Translation for file to canonical header name to index in the column of the file
     */
    Map<File,Map<String,Integer>> file_trans_lu = new HashMap<File,Map<String,Integer>>();

    /**
     * Begin file flag... used to tell the parser to examine the header row
     */
    boolean begin_file = false;

    /**
     * Timestamps are ordered
     */
    boolean ordered_timestamps = true;

    /**
     * Last observed timestamp
     */
    long last_timestamp = 0L;

    /**
     * Translation information for this specific file parsing...
     */
    Map<String,String> trans = null;

    /**
     * Construct a map that converts canonical names into their integer index equivalents for the specific
     * file that is being parsed.
     *
     *@param tokens header line tokens
     */
    public void parseHeader(String tokens[]) {
      trans_i = new HashMap<String,Integer>();
      for (int i=0;i<tokens.length;i++) {
        if      (tokens[i].equals("beg") || tokens[i].equals("begin") || tokens[i].equals("timestamp"))     trans_i.put("ts",  i);
	else if (tokens[i].equals("end") ||                              tokens[i].equals("timestamp_end")) trans_i.put("tse", i);
	else {
          Iterator<String> it = trans.keySet().iterator(); while (it.hasNext()) {
	    String canon = it.next();
	    String local = trans.get(canon);
	    if (tokens[i].equals(local)) trans_i.put(canon, i);
	  }
	}
      }
      if (trans_i.keySet().size() != trans.keySet().size() + 2 &&
          trans_i.keySet().size() != trans.keySet().size() + 1) {
        System.err.println("  File translation mismatch..");
	System.err.print("  * Trans:  "); System.err.println("  " + trans);
	System.err.print("  * Trans I:"); System.err.println("  " + trans_i);
      }
    }

    /**
     * Translation information to convert canonical field into the integer index
     */
    Map<String,Integer> trans_i = null;

    /**
     * Consume the tokens from the csv reader.
     *
     *@param tokens  array of strings separated by commas from the original file
     *@param line    original line from the file
     *@param line_no line number
     *
     *@return false indicates that the parser should halt parsing (optional)
     */
    public boolean consume(String tokens[], String line, int line_no) {
      if (begin_file) { parseHeader(tokens); begin_file = false; } else {
        String sip   = tokens[trans_i.get(StatsOverlay.sip)],
	       spt   = tokens[trans_i.get(StatsOverlay.spt)],
	       pro   = tokens[trans_i.get(StatsOverlay.pro)],
	       dpt   = tokens[trans_i.get(StatsOverlay.dpt)],
	       dip   = tokens[trans_i.get(StatsOverlay.dip)];
        String ts    = tokens[trans_i.get("ts")];
	String tse   = null;
        if (trans_i.containsKey("tse")) tse = tokens[trans_i.get("tse")];
        else                            tse = ts; // should probably be a derivative of ts + DUR

	// Convert some to their long/integer versions...
	long   ts_l  = Utils.parseTimeStamp(ts),
	       tse_l = Utils.parseTimeStamp(tse);
        int    spt_i = Integer.parseInt(spt),
	       dpt_i = Integer.parseInt(dpt);

        // if forced time, make sure it's within those bounds...
        if (ts0_force != 0L && ts_l < ts0_force) return true;
	if (ts1_force != 0L && ts_l > ts1_force) return true;

        int DOCTS_i = 0, SOCTS_i = 0, DPKTS_i = 0, SPKTS_i = 0;
        if (trans_i.containsKey(StatsOverlay.DOCTS)) {
          DOCTS_i = Integer.parseInt(tokens[trans_i.get(StatsOverlay.DOCTS)]);
          SOCTS_i = Integer.parseInt(tokens[trans_i.get(StatsOverlay.SOCTS)]);
          DPKTS_i = Integer.parseInt(tokens[trans_i.get(StatsOverlay.DPKTS)]);
          SPKTS_i = Integer.parseInt(tokens[trans_i.get(StatsOverlay.SPKTS)]);
        }
	int OCTS_i  = 0, PKTS_i  = 0;
        if (trans_i.containsKey(StatsOverlay.OCTS))  {
          OCTS_i = Integer.parseInt(tokens[trans_i.get(StatsOverlay.OCTS)]);
          PKTS_i = Integer.parseInt(tokens[trans_i.get(StatsOverlay.PKTS)]);
	}

	// fix the porting...
        if (low_port && dpt_i > spt_i) {
          String pt = spt; spt = dpt; dpt = pt;
	  String ip = sip; sip = dip; dip = ip;
          spt_i = Integer.parseInt(spt);
	  dpt_i = Integer.parseInt(dpt);

	  int oct = DOCTS_i; DOCTS_i = SOCTS_i; SOCTS_i = oct;
	  int pkt = DPKTS_i; DPKTS_i = SPKTS_i; SPKTS_i = pkt;
	}
        // OCTS, PKTS, [SD]OCTS, [SD]PKTS

	sips.add(sip);
	dips.add(dip);
	socts.add(SOCTS_i);
	docts.add(DOCTS_i);
	octs.add(OCTS_i);
	pkts.add(PKTS_i);
	pkts.add(SPKTS_i + DPKTS_i);
	pairs.add(ipPairsKey(sip,dip));
	spts.add(spt_i);
	dpts.add(dpt_i);

        if (ts0 > ts_l)  ts0 = ts_l;
	if (ts1 < tse_l) ts1 = tse_l;

        if (ts0 < last_timestamp) ordered_timestamps = false;
	last_timestamp = ts0;
      }
      return true; // keep parsing...
    }

    /**
     * Packets... not going to break out for source versus destination
     */
    Set<Integer> pkts = new HashSet<Integer>(),

    /**
     * Source ports
     */
                 spts = new HashSet<Integer>(),

    /**
     * Destination ports
     */
                 dpts = new HashSet<Integer>();

    /**
     * Source IPs
     */
    Set<String> sips  = new HashSet<String>(),

    /**
     * Destination IPs
     */
                dips  = new HashSet<String>(),

    /**
     * Pairs...  SIP:DIP
     */
		pairs = new HashSet<String>();

    /**
     * Octets
     */
    Set<Integer> octs  = new HashSet<Integer>(),

    /**
     * Destination Octets
     */
                 docts = new HashSet<Integer>(),
    /**
     * Source Octets
     */
		 socts = new HashSet<Integer>();

    /**
     * Earliest timestamp across all the files
     */
    long        ts0 = Long.MAX_VALUE,

    /**
     * Latest timestamp across all the files
     */
                ts1 = Long.MIN_VALUE;
  
    /**
     * Process a comment line from the CSV File.
     *@param line    comment line
     */
    public void commentLine(String line) { }

    /**
     * Return a set of the source ports across all the files.
     *
     *@return set of src ports
     */
    public Set<Integer> getSrcPts() { return spts; }

    /**
     * Return a set of the destination ports across all the files.
     *
     *@return set of dest ports
     */
    public Set<Integer> getDstPts() { return dpts; }

    /**
     * Return a set of the source octets found across all the files.
     *
     *@return source octets set
     */
    public Set<Integer> getSrcOctets() { return socts; }

    /**
     * Return a set of the destination octets found across all the files.
     *
     *@return destination octets set
     */
    public Set<Integer> getDstOctets() { return docts; }

    /**
     * Return a set of the octets found across all the files
     *
     *@return octets set
     */
    public Set<Integer> getOctets()    { return octs; }

    /**
     * Return a set of the source IP addresses found across all the files.
     *
     *@return source IP set
     */
    public Set<String> getSrcIPs() { return sips; }

    /**
     * Return a set of the destination IP addresses found across all the files.
     *
     *@return destination IP set
     */
    public Set<String> getDstIPs() { return dips; }

    /**
     * Return a set of the source and destination IP address pairs across all the files.  IP pairs
     * are represented as source ip greater-than-symbol destination ip.
     *
     *@return source and destination IP address pairs
     */
    public Set<String> getPairs()  { return pairs; }

    /**
     * Return true if the timestamps are monotonically increasing in order.
     *
     *@return true for monotonic increasing
     */
    public boolean orderedTimestamps() { return ordered_timestamps; }

    /**
     * Return the earliest timestamp.
     *
     *@return earliest timestamp
     */
    public long        getTS0() { return ts0; }

    /**
     * Return the latest timestamp.  This is based off of the end flow timestamp.
     *
     *@return latest timestamp
     */
    public long        getTS1() { return ts1; }

    /**
     * Return the translations for the specified file.  The translations provide the index for the
     * canonical netflow name.
     *
     *@param file file to find translations for
     *
     *@return map translating canonical name to column indices
     */
    public Map<String,Integer> getFileTranslation(File file) { return file_trans_lu.get(file); }
  }

  /**
   * Parse the files and aggregate the information necessary to construct the plot parameters.
   *
   *@param files files to parse
   */
  public void aggregate(File files[]) throws IOException { aggregator = new Aggregator(files); }

  /**
   * Render context for the plot.  Requires the aggregator to understand the data.
   */
  class RenderContext implements CSVTokenConsumer {
    /**
     * Height of the pairs chart
     */
    final static int pairs_h = 512,

    /**
     * Height of the src ip chart
     */
                     srcip_h = 256,

    /**
     * Height of the src port chart
     */
                     srcpt_h = 128,

    /**
     * Height of the dst ip chart
     */
                     dstip_h = 256,

    /**
     * Height of the dst port chart
     */
                     dstpt_h = 128,

    /**
     * Height of the octets horizon chart
     */
                     octs_h  = 64,

    /**
     * Height of the packets horizon chart
     */
                     pkts_h  = 64,

    /**
     * Height of the sessions horizon chart
     */
                     sess_h  = 64;

    /** 
     * y position of the pairs chart
     */
    int              pairs_y,

    /** 
     * y position of the source ip chart
     */
                     srcip_y,

    /** 
     * y position of the source port chart
     */
                     srcpt_y,

    /** 
     * y position of the destination ip chart
     */
		     dstip_y,

    /** 
     * y position of the destination port chart
     */
		     dstpt_y,

    /** 
     * y position of the octets horizon chart
     */
		     octs_y,

    /** 
     * y position of the packets horizon chart
     */
		     pkts_y,

    /** 
     * y position of the sessions horizon chart
     */
		     sess_y;

    /**
     * Width for most of the charts -- to include pairs, srcip, dstip, octs, pkts, and sess...
     */
    int              chart_w;

    /**
     * Text height
     */
    int              txt_h,

    /**
     * Maximum width for an IP address
     */
                     maxip_w;

    /**
     * Plot width
     */
    int              plot_w,

    /**
     * Plot height
     */
                     plot_h;

    /**
     * Final image
     */
    BufferedImage plot_bi,

    /**
     * Temporary plot to determine default fonts, etc.
     */
                  tmp_bi;

    /**
     * Slice for time in the plot -- determines the width
     */
    long          slice = 1000L;

    /**
     * Slice for time in the plot -- animated version
     */
    long          slice_anim = 1000L;

    /**
     * Maximum width of the plot (unless overridden by a flag)
     */
    final static int max_plot_w = 4096,

    /**
     * Right border
     */
                     rgt_border = 64,

    /**
     * Left border
     */
		     lft_border = 64,

    /**
     * Top border
     */
                     top_border = 24,

    /**
     * Bottom border
     */
                     bot_border = 24;

    /**
     * Transform a timestamp into an x coordinate
     *
     *@param ts timestamp -- should be between ts0 and ts1...
     *
     *@return x coordinate for the timestamp
     */
    public int xTransform(long ts) { return (int) ((ts - aggregator.getTS0())/slice); }

    /**
     * Formatter for a year
     */
    SimpleDateFormat yer_sdf = new SimpleDateFormat("yyyy"),

    /**
     * Formatter for a month
     */
                     mon_sdf = new SimpleDateFormat("MM"),

    /**
     * Formatter for a day
     */
		     day_sdf = new SimpleDateFormat("dd"),

    /**
     * Formatter for an hour
     */
		     hor_sdf = new SimpleDateFormat("dd"),

    /**
     * Formatter for a minute
     */
		     min_sdf = new SimpleDateFormat("mm");

    /**
     * Render the temporal labels for the plot.
     */
    private void renderTemporalLabels(Graphics2D g2d) {
      long ts0 = aggregator.getTS0(), ts1 = aggregator.getTS1();

      // Find the first human readable date in the timeframe...
      int yer = Integer.parseInt(yer_sdf.format(new Date(ts0))); 
      long yer_ms = Utils.parseTimeStamp(""+yer+"-01-01");                        long yer2_ms = yer_ms + 365*24*60*60*1000, yer5_ms = yer_ms + 5*365*24*60*60*1000;

      int mon = Integer.parseInt(mon_sdf.format(new Date(ts0))); 
      long mon_ms = Utils.parseTimeStamp(""+yer+"-"+mon+"-01");                   long mon2_ms = mon_ms + 30*24*60*60*1000,  mon5_ms = mon_ms + 5*30*24*60*60*1000;

      int day = Integer.parseInt(day_sdf.format(new Date(ts0))); 
      long day_ms = Utils.parseTimeStamp(""+yer+"-"+mon+"-"+day);                 long day2_ms = day_ms + 24*60*60*1000,     day5_ms = day_ms + 5*24*60*60*1000;

      int hor = Integer.parseInt(hor_sdf.format(new Date(ts0))); 
      long hor_ms = Utils.parseTimeStamp(""+yer+"-"+mon+"-"+day+"T"+hor);         long hor2_ms = hor_ms + 60*60*1000,        hor5_ms = hor_ms + 5*60*60*1000;

      int min = Integer.parseInt(min_sdf.format(new Date(ts0))); 
      long min_ms = Utils.parseTimeStamp(""+yer+"-"+mon+"-"+day+"T"+hor+":"+min); long min2_ms = min_ms + 60*1000,           min5_ms = min_ms + 5*60*1000;

      long start_ms = min_ms; int field = Calendar.MINUTE; int amount = 1; SimpleDateFormat sdf = null;
      if        ((yer_ms >= ts0 && yer_ms <= ts1) || (yer2_ms >= ts0 && yer2_ms <= ts1)) {
        start_ms = yer_ms; 
        if (yer5_ms >= ts0 && yer5_ms <= ts1) { field = Calendar.YEAR;        sdf = new SimpleDateFormat("yyyy");     }
	else                                  { field = Calendar.MONTH;       sdf = new SimpleDateFormat("yyyy-MMM"); }
      } else if ((mon_ms >= ts0 && mon_ms <= ts1) || (mon2_ms >= ts0 && mon2_ms <= ts1)) {
        start_ms = mon_ms; 
        if (mon5_ms >= ts0 && mon5_ms <= ts1) { field = Calendar.MONTH;       sdf = new SimpleDateFormat("MMM");    }
	else                                  { field = Calendar.DAY_OF_YEAR; sdf = new SimpleDateFormat("MMM dd"); }
      } else if ((day_ms >= ts0 && day_ms <= ts1) || (day2_ms >= ts0 && day2_ms <= ts1)) {
        start_ms = day_ms; 
	if (day5_ms >= ts0 && day5_ms <= ts1) { field = Calendar.DAY_OF_YEAR; sdf = new SimpleDateFormat("MMM dd"); }
        else                                  { field = Calendar.HOUR;        sdf = new SimpleDateFormat("hh:mm");  }
      } else if ((hor_ms >= ts0 && hor_ms <= ts1) || (hor2_ms >= ts0 && hor2_ms <= ts1)) {
        start_ms = hor_ms; 
	if (hor5_ms >= ts0 && hor5_ms <= ts1) { field = Calendar.HOUR;        sdf = new SimpleDateFormat("hh:mm");  }
	else                                  { field = Calendar.MINUTE;      sdf = new SimpleDateFormat("hh:mm");  }
      } else if ((min_ms >= ts0 && min_ms <= ts1) || (min2_ms >= ts0 && min2_ms <= ts1)) {
        start_ms = min_ms; field = Calendar.MINUTE;                           sdf = new SimpleDateFormat("hh:mm");
      }


      // Print the labels
      Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT")); calendar.setTimeInMillis(start_ms);
      while (calendar.getTimeInMillis() <= ts1) {
        String str = sdf.format(new Date(calendar.getTimeInMillis()));
	int x = xTransform(calendar.getTimeInMillis());
	if (x >= lft_border && x <= lft_border + plot_w) {
	  g2d.setColor(Color.darkGray);  g2d.drawLine(x, 5*txt_h, x, label_ys[label_ys.length-1] + 3*txt_h);
	  g2d.setColor(Color.lightGray); for (int i=0;i<label_ys.length;i++) g2d.drawString(str, x + 2, label_ys[i] + 3);
        }
        calendar.add(field,amount);
      }
    }

    /**
     * Return the finalized plot.
     *
     *@return final plot
     */
    public BufferedImage getPlot(String header) { 
      Graphics2D g2d = (Graphics2D) plot_bi.getGraphics(); g2d.setColor(Color.darkGray);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      renderTemporalLabels(g2d);

      g2d.setColor(Color.white); g2d.drawString(header, 5, txt_h + 4);
      String render_time = "Recs: "      + records + 
                           " || Slice: " + Utils.humanReadableDuration(slice) +
                           " || RTime: " + Utils.exactDate(System.currentTimeMillis()); g2d.drawString(render_time, plot_w - Utils.txtW(g2d,render_time) - 5, txt_h + 4);

      BufferedImage bi = null;;

      bi = horiz_sess.applyFinalRender(BrewerSimplified.seq7Color1());
      g2d.setColor(Color.lightGray); g2d.fillRect(lft_border - 1, sess_y - 1, bi.getWidth() + 2, bi.getHeight() + 2);
      g2d.drawImage(bi, lft_border, sess_y, null);
      g2d.setColor(Color.white); g2d.drawString("Sessions (Horizon)", lft_border + 2, sess_y - 3);

      bi = horiz_octs.applyFinalRender(BrewerSimplified.seq7Color2());
      g2d.setColor(Color.lightGray); g2d.fillRect(lft_border - 1, octs_y - 1, bi.getWidth() + 2, bi.getHeight() + 2);
      g2d.drawImage(bi, lft_border, octs_y, null);
      g2d.setColor(Color.white); g2d.drawString("Octets (Horizon)", lft_border + 2, octs_y - 3);

      bi = horiz_pkts.applyFinalRender(BrewerSimplified.seq7Color3());
      g2d.setColor(Color.lightGray); g2d.fillRect(lft_border - 1, pkts_y - 1, bi.getWidth() + 2, bi.getHeight() + 2);
      g2d.drawImage(bi, lft_border, pkts_y, null);
      g2d.setColor(Color.white); g2d.drawString("Packets (Horizon)", lft_border + 2, pkts_y - 3);

      bi = xy_pairs.applyFinalRender(accumulation_type);
      g2d.setColor(Color.lightGray); g2d.fillRect(lft_border - 1, pairs_y - 1, bi.getWidth() + 2, bi.getHeight() + 2);
      g2d.drawImage(bi, lft_border, pairs_y, null);
      g2d.setColor(Color.white); g2d.drawString("IP Pairs (SrcIP > DstIP) [" + aggregator.getPairs().size() + "]", lft_border + 2, pairs_y - 3);
      g2d.drawImage(xy_pairs.renderPrivateIPSpace(), lft_border - 4, pairs_y, null);

      bi = xy_srcip.applyFinalRender(accumulation_type);
      g2d.setColor(Color.lightGray); g2d.fillRect(lft_border - 1, srcip_y - 1, bi.getWidth() + 2, bi.getHeight() + 2);
      g2d.drawImage(bi, lft_border, srcip_y, null);
      g2d.setColor(Color.white); g2d.drawString("SrcIP [" + aggregator.getSrcIPs().size() + "]", lft_border + 2, srcip_y - 3);
      g2d.drawImage(xy_srcip.renderPrivateIPSpace(), lft_border - 4, srcip_y, null);

      bi = xy_srcpt.applyFinalRender(accumulation_type);
      g2d.setColor(Color.lightGray); g2d.fillRect(lft_border - 1, dstpt_y - 1, bi.getWidth() + 2, bi.getHeight() + 2);
      g2d.drawImage(bi, lft_border, srcpt_y, null);
      g2d.setColor(Color.white); g2d.drawString("Src Port", lft_border + 2, srcpt_y - 3);

      bi = xy_dstip.applyFinalRender(accumulation_type);
      g2d.setColor(Color.lightGray); g2d.fillRect(lft_border - 1, dstip_y - 1, bi.getWidth() + 2, bi.getHeight() + 2);
      g2d.drawImage(bi, lft_border, dstip_y, null);
      g2d.setColor(Color.white); g2d.drawString("DstIP [" + aggregator.getDstIPs().size() + "]", lft_border + 2, dstip_y - 3);
      g2d.drawImage(xy_dstip.renderPrivateIPSpace(), lft_border - 4, dstip_y, null);

      bi = xy_dstpt.applyFinalRender(accumulation_type);
      g2d.setColor(Color.lightGray); g2d.fillRect(lft_border - 1, dstpt_y - 1, bi.getWidth() + 2, bi.getHeight() + 2);
      g2d.drawImage(bi, lft_border, dstpt_y, null);
      g2d.setColor(Color.white); g2d.drawString("Dst Port", lft_border + 2, dstpt_y - 3);

      g2d.setColor(Color.white);
      String s = Utils.exactDate(aggregator.getTS0()); g2d.drawString(s, 5,                                           plot_bi.getHeight() - 2);
             s = Utils.exactDate(aggregator.getTS1()); g2d.drawString(s, plot_bi.getWidth() - 5 - Utils.txtW(g2d, s), plot_bi.getHeight() - 2);

      // Get the global maximum
      long global_max = histos[0].max(accumulation_type);
      for (int i=1;i<histos.length;i++) { if (global_max < histos[i].max(accumulation_type)) global_max = histos[i].max(accumulation_type); }

      // Render the histograms in the context of the global max
      for (int i=0;i<histos.length;i++) { histos[i].render(g2d, splits_x[i], sess_y + sess_h + txt_h, splits_x[i+1] - splits_x[i] - 5, accumulation_type, global_max); }

      g2d.dispose();

      return plot_bi; 
    }

    /**
     * Chart type -- only certain combos work with AccumulationType
     */
    XYChartType      chart_type;

    /**
     * Accumulation type for what gets added together per pixel -- only certain combos work with ChartType
     */
    AccumulationType accumulation_type;

    /**
     * Y locations for the timeframe labels
     */
    int label_ys[];

    /**
     * Construct the render context
     */
    public RenderContext(XYChartType chart_type, AccumulationType accumulation_type) {
      this.chart_type        = chart_type;
      this.accumulation_type = accumulation_type;

      // Determine the font info
      tmp_bi = new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB); Graphics2D g2d = (Graphics2D) tmp_bi.getGraphics(); 
        txt_h   = Utils.txtH(g2d, "0"); 
        maxip_w = Utils.txtW(g2d, "  xxx.xm 000.000.000.000"); // Make some space in the front for the actual count label
      g2d.dispose();

      // Determine the width of the plot... determine the timeslicing...
      long delta = aggregator.getTS1() - aggregator.getTS0() + 1L;
      if        (seconds_scale)   { slice = 1000L;              slice_anim = 1000L;
      } else if (minutes_scale)   { slice = 1000L*60L;          slice_anim = 1000L*5L;
      } else if (minutes5_scale)  { slice = 1000L*60L*5L;       slice_anim = 1000L*10L;
      } else if (minutes10_scale) { slice = 1000L*60L*10L;      slice_anim = 1000L*20L;
      } else if (hours_scale)     { slice = 1000L*60L*60L;      slice_anim = 1000L*60L;
      } else if (hours4_scale)    { slice = 1000L*60L*60L*4L;   slice_anim = 1000L*60L*4L;
      } else if (days_scale)      { slice = 1000L*60L*60L*24L;  slice_anim = 1000L*60L*6L;
      } else                      {
        if      ((delta/(1000L))            <= max_plot_w) { slice = 1000L;             slice_anim = 1000L;        }
        else if ((delta/(1000L*60L))        <= max_plot_w) { slice = 1000L*60L;         slice_anim = 1000L*5L;     }
        else if ((delta/(1000L*60L*5L))     <= max_plot_w) { slice = 1000L*60L*5L;      slice_anim = 1000L*10L;    }
        else if ((delta/(1000L*60L*10L))    <= max_plot_w) { slice = 1000L*60L*10L;     slice_anim = 1000L*20L;    }
        else if ((delta/(1000L*60L*60L))    <= max_plot_w) { slice = 1000L*60L*60L;     slice_anim = 1000L*60L;    }
        else if ((delta/(1000L*60L*60L*4L)) <= max_plot_w) { slice = 1000L*60L*60L*4L;  slice_anim = 1000L*60L*4L; }
        else                                               { slice = 1000L*60L*60L*24L; slice_anim = 1000L*60L*6L; }
      }

      // Calculate the widths
      chart_w = (int) (delta/slice + 1L);
      plot_w  = lft_border + chart_w + rgt_border;

      // Calculate the height
      plot_h  = top_border +
                2*txt_h + pairs_h + txt_h +
                2*txt_h + srcip_h + txt_h +
                2*txt_h + srcpt_h + txt_h +
		2*txt_h + dstip_h + txt_h +
                2*txt_h + dstpt_h + txt_h +
		2*txt_h + octs_h  + txt_h +
		2*txt_h + pkts_h  + txt_h +
		2*txt_h + sess_h  +
		12*(txt_h+4) + // Histograms
		bot_border;

      // Determine the label y locations
      label_ys = new int[5]; 
      label_ys[0] = top_border  + 2*txt_h + pairs_h + txt_h;
      label_ys[1] = label_ys[0] + 2*txt_h + srcip_h + txt_h;
      label_ys[2] = label_ys[1] + 2*txt_h + srcpt_h + txt_h;
      label_ys[3] = label_ys[2] + 2*txt_h + dstip_h + txt_h;
      label_ys[4] = label_ys[3] + 2*txt_h + dstpt_h + txt_h;

      // Print out info about the plot size
      System.err.println("Plot Size: " + plot_w + " x " + plot_h + " | slice = " + slice + " ms (" + Utils.humanReadableDuration(slice) + ")");

      // Calculate the y offsets per chart -- x positions are just the lft_border value...
      sess_y   = plot_h  - 12*(txt_h+4) - bot_border - sess_h;
      pkts_y   = sess_y  - 3*txt_h    - pkts_h;
      octs_y   = pkts_y  - 3*txt_h    - octs_h;
      dstpt_y  = octs_y  - 3*txt_h    - dstpt_h;
      dstip_y  = dstpt_y - 3*txt_h    - dstip_h;
      srcpt_y  = dstip_y - 3*txt_h    - srcpt_h;
      srcip_y  = srcpt_y - 3*txt_h    - srcip_h;
      pairs_y  = srcip_y - 3*txt_h    - pairs_h;

      // Allocate the buffered image
      plot_bi = new BufferedImage(plot_w, plot_h, BufferedImage.TYPE_INT_RGB);
      g2d     = (Graphics2D) plot_bi.getGraphics();

      // Get the sets, sort, then create maps to convert each item into a y coordinate
      List<String> list = new ArrayList<String>(); 

      list.clear(); list.addAll(aggregator.getPairs()); Collections.sort(list);
      for (int i=0;i<list.size();i++) pairs_lu.put(list.get(i), i); 

      list.clear(); list.addAll(aggregator.getSrcIPs()); Collections.sort(list);
      for (int i=0;i<list.size();i++) srcips_lu.put(list.get(i), i); 

      list.clear(); list.addAll(aggregator.getDstIPs()); Collections.sort(list);
      for (int i=0;i<list.size();i++) dstips_lu.put(list.get(i), i); 

      List<Integer> list_i = new ArrayList<Integer>();

      list_i.clear(); list_i.addAll(aggregator.getSrcPts()); Collections.sort(list_i);
      for (int i=0;i<list_i.size();i++) srcpts_lu.put(list_i.get(i), i);

      list_i.clear(); list_i.addAll(aggregator.getDstPts()); Collections.sort(list_i);
      for (int i=0;i<list_i.size();i++) dstpts_lu.put(list_i.get(i), i);

      // Allocate the xy charts and horizon plots
      horiz_sess = new HorizonChart(this, sess_h);
      horiz_pkts = new HorizonChart(this, pkts_h);
      horiz_octs = new HorizonChart(this, octs_h);

      xy_dstpt = new XYStringsChart(chart_type, this, dstpts_lu, dstpt_h, true);
      xy_dstip = new XYStringsChart(chart_type, this, dstips_lu, dstip_h);
      xy_srcpt = new XYStringsChart(chart_type, this, srcpts_lu, srcpt_h, true);
      xy_srcip = new XYStringsChart(chart_type, this, srcips_lu, srcip_h);
      xy_pairs = new XYStringsChart(chart_type, this, pairs_lu,  pairs_h);

      // Split the plot based on the max ip width -- each split will tabulate a histogram for src_ip, dst_ip, and dst_pt
      splits_count = chart_w/maxip_w; if (splits_count == 0) splits_count = 1;
      splits_times = new long[splits_count+1]; splits_x = new int[splits_count+1];
      splits_times[0]                     = aggregator.getTS0(); splits_x[0]                 = lft_border;
      splits_times[splits_times.length-1] = aggregator.getTS1(); splits_x[splits_x.length-1] = lft_border + chart_w;
      for (int i=1;i<splits_times.length-1;i++) {
        splits_times[i] = aggregator.getTS0() + (i*(aggregator.getTS1() - aggregator.getTS0()))/(splits_times.length-1);
	splits_x[i]     = lft_border          + (i*chart_w)/(splits_x.length-1);
      }

        /* // FOR DEBUG
        for (int i=0;i<splits_times.length;i++) {
          System.err.print("Times["+i+"] | " + splits_times[i] + " | " + splits_x[i]);
	  if (i > 0) System.err.println(" ||deltas|| " + (splits_times[i]-splits_times[i-1]) + " " + (splits_x[i] - splits_x[i-1])); else System.err.println();
        }
        */

      // Allocate the histograms
      histos = new Histo[splits_times.length-1];
      for (int i=0;i<histos.length;i++) histos[i] = new Histo(splits_times[i], splits_times[i+1]);
    }

    /**
     * Number of splits
     */
    int splits_count = 1;

    /**
     * X coordinates for each of the splits
     */
    int splits_x[];

    /**
     * Timestamps for separating the splits
     */
    long splits_times[];

    /**
     * Histogram at split times
     */
    Histo histos[];

    /**
     * Horizon chart for sessions
     */
    HorizonChart horiz_sess,

    /**
     * Horizon chart for packets
     */
                 horiz_pkts,

    /**
     * Horizon chart for octets
     */
		 horiz_octs;

    /**
     * Destination IP XY Chart
     */
    XYStringsChart xy_dstip,

    /**
     * Source IP XY Chart
     */
                   xy_srcip,

    /**
     * IP Pairs XY Chart
     */
		   xy_pairs,

    /**
     * Source Port XY Chart
     */
		   xy_srcpt,

    /**
     * Desintaion Port XY Chart
     */
		   xy_dstpt;

    /**
     * Lookup to turn a srcpt into an integer index amongst all the other sorted src ports
     */
    Map<Integer,Integer> srcpts_lu = new HashMap<Integer,Integer>(),

    /**
     * Lookup to turn a dstpt into an integer index amongst all the other sorted dst ports
     */
                         dstpts_lu = new HashMap<Integer,Integer>();

    /**
     * Lookup to turn a srcip - dstip pair into an integer index amongst all the other sorted pairs
     */
    Map<String,Integer> pairs_lu = new HashMap<String,Integer>(),

    /**
     * Lookup to turn a srcip into an integer index amongst all the other sorted srcips
     */
                        srcips_lu = new HashMap<String,Integer>(),

    /**
     * Lookup to turn a dstip into an integer index amongst all the other sorted dstips
     */
			dstips_lu = new HashMap<String,Integer>();

    /** 
     * Set the file translation for the token reader.  Also, set the flag that indicates parsing of a new file is about to begin.
     *
     *@param translation translates canonical netflow names into column indexes
     */
    public void setFileTranslation(Map<String,Integer> translation) { this.file_translation = translation; begin_file = true; }

    /**
     * Translates canonical netflow names to their respective columns
     */
    Map<String,Integer> file_translation;

    /**
     * Identifies if the next parse line is the first line of the file.
     */
    boolean begin_file = true;

    /**
     * Total records processed
     */
    long    records = 0L;

    /**
     * CSVTokenConsumer override
     *
     *@param line line from file that is identified as a comment
     */
    public void commentLine(String line) { }

    /**
     * Consume the tokens from the csv reader.
     *
     *@param tokens  array of strings separated by commas from the original file
     *@param line    original line from the file
     *@param line_no line number
     *
     *@return false indicates that the parser should halt parsing (optional)
     */
    public boolean consume(String tokens[], String line, int line_no) {
      if (begin_file) { begin_file = false; } else {
        records++;
        String sip   = tokens[file_translation.get(StatsOverlay.sip)],
	       spt   = tokens[file_translation.get(StatsOverlay.spt)],
	       pro   = tokens[file_translation.get(StatsOverlay.pro)],
	       dpt   = tokens[file_translation.get(StatsOverlay.dpt)],
	       dip   = tokens[file_translation.get(StatsOverlay.dip)];
        String ts    = tokens[file_translation.get("ts")];
	String tse   = null;
        if (file_translation.containsKey("tse")) tse = tokens[file_translation.get("tse")];
        else                            tse = ts; // should probably be a derivative of ts + DUR

	// Convert to integer representations
        int    spt_i = Integer.parseInt(spt),
	       dpt_i = Integer.parseInt(dpt);
	long   ts_l  = Utils.parseTimeStamp(ts),
	       tse_l = Utils.parseTimeStamp(tse);

        // if forced time, make sure it's within those bounds...
        if (ts0_force != 0L && ts_l < ts0_force) return true;
	if (ts1_force != 0L && ts_l > ts1_force) return true;

        int DOCTS_i = 0, SOCTS_i = 0, DPKTS_i = 0, SPKTS_i = 0;
        if (file_translation.containsKey(StatsOverlay.DOCTS)) {
          DOCTS_i = Integer.parseInt(tokens[file_translation.get(StatsOverlay.DOCTS)]);
          SOCTS_i = Integer.parseInt(tokens[file_translation.get(StatsOverlay.SOCTS)]);
          DPKTS_i = Integer.parseInt(tokens[file_translation.get(StatsOverlay.DPKTS)]);
          SPKTS_i = Integer.parseInt(tokens[file_translation.get(StatsOverlay.SPKTS)]);
        }
	int OCTS_i  = 0, PKTS_i  = 0;
        if (file_translation.containsKey(StatsOverlay.OCTS))  {
          OCTS_i = Integer.parseInt(tokens[file_translation.get(StatsOverlay.OCTS)]);
          PKTS_i = Integer.parseInt(tokens[file_translation.get(StatsOverlay.PKTS)]);
	}

	// fix the porting...
        if (low_port && dpt_i > spt_i) {
          String pt = spt; spt = dpt; dpt = pt;
	  String ip = sip; sip = dip; dip = ip;
          spt_i = Integer.parseInt(spt);
	  dpt_i = Integer.parseInt(dpt);

	  int oct = DOCTS_i; DOCTS_i = SOCTS_i; SOCTS_i = oct;
	  int pkt = DPKTS_i; DPKTS_i = SPKTS_i; SPKTS_i = pkt;
	}

	if (OCTS_i == 0 && (DOCTS_i + SOCTS_i) > 0) OCTS_i = DOCTS_i + SOCTS_i;
	if (PKTS_i == 0 && (DPKTS_i + SPKTS_i) > 0) PKTS_i = DPKTS_i + SPKTS_i;


        if (chart_type == XYChartType.long_sum) {
	  long inc = 1L;
          if        (accumulation_type == AccumulationType.octets)  inc = OCTS_i;
	  else if   (accumulation_type == AccumulationType.packets) inc = PKTS_i;

	  if (model_duration) {
            xy_dstip.accumulate(dip,                 ts_l, tse_l, inc);
            xy_dstpt.accumulate(dpt_i,               ts_l, tse_l, inc);
            xy_srcip.accumulate(sip,                 ts_l, tse_l, inc);
            xy_srcpt.accumulate(spt_i,               ts_l, tse_l, inc);
	    xy_pairs.accumulate(ipPairsKey(sip,dip), ts_l, tse_l, inc);
	    horiz_sess.accumulate(ts_l, tse_l, 1L);
	    horiz_octs.accumulate(ts_l, tse_l, OCTS_i);
	    horiz_pkts.accumulate(ts_l, tse_l, PKTS_i);
	  } else              {
            xy_dstip.accumulate(dip,                 ts_l, inc);
            xy_dstpt.accumulate(dpt_i,               ts_l, inc);
            xy_srcip.accumulate(sip,                 ts_l, inc);
            xy_srcpt.accumulate(spt_i,               ts_l, inc);
	    xy_pairs.accumulate(ipPairsKey(sip,dip), ts_l, inc);
	    horiz_sess.accumulate(ts_l, 1L);
	    horiz_octs.accumulate(ts_l, OCTS_i);
	    horiz_pkts.accumulate(ts_l, PKTS_i);
	  }
        }

	boolean histo_consumed = false;
        for (int i=0;i<histos.length-1;i++) { if (ts_l >= splits_times[i] && ts_l < splits_times[i+1]) { histos[i].accumulate(sip,dip,dpt_i,PKTS_i,OCTS_i); histo_consumed = true; } }
	if (histo_consumed == false) histos[histos.length-1].accumulate(sip,dip,dpt_i,PKTS_i,OCTS_i); // It's at the very final timestamp... accumulate in the last bin
      }
      return true;
    }
  }

  /**
   * Class to accumulate histogram information
   */
  class Histo {
    /**
     * Start timestamp for this histogram
     */
    long ts0,

    /**
     * End timestamp for this histogram
     */
         ts1;

    /**
     * Constructor... just record the timestamps
     *
     *@param ts0 start time for this histo bin
     *@param ts1 stop time for this histo bin
     */
    public Histo(long ts0, long ts1) { this.ts0 = ts0; this.ts1 = ts1; }

    /**
     * Accumulate the information for a single netflow record.
     */
    public void accumulate(String sip, String dip, int dpt_i, int PKTS_i, int OCTS_i) {
      long l;

      l = acc(sip_sess,sip,     1);       if (l > max_sess) max_sess = l;
      l = acc(sip_pkts,sip,     PKTS_i);  if (l > max_pkts) max_pkts = l;
      l = acc(sip_octs,sip,     OCTS_i);  if (l > max_octs) max_octs = l;

      l = acc(dip_sess,dip,     1);       if (l > max_sess) max_sess = l;
      l = acc(dip_pkts,dip,     PKTS_i);  if (l > max_pkts) max_pkts = l;
      l = acc(dip_octs,dip,     OCTS_i);  if (l > max_octs) max_octs = l;

      l = acc(dpt_sess,""+dpt_i,1);       if (l > max_sess) max_sess = l;
      l = acc(dpt_pkts,""+dpt_i,PKTS_i);  if (l > max_pkts) max_pkts = l;
      l = acc(dpt_octs,""+dpt_i,OCTS_i);  if (l > max_octs) max_octs = l;
    }

    /**
     * SrcIP Session Counts
     */
    Map<String,Long> sip_sess = new HashMap<String,Long>(),
    /**
     * DstIP Session Counts
     */
                     dip_sess = new HashMap<String,Long>(),
    /**
     * DstPort Session Counts
     */
		     dpt_sess = new HashMap<String,Long>(),
    /**
     * SrcIP Packet Counts
     */
		     sip_pkts = new HashMap<String,Long>(),
    /**
     * DstIP Packet Counts
     */
		     dip_pkts = new HashMap<String,Long>(),
    /**
     * DstPort Packet Counts
     */
		     dpt_pkts = new HashMap<String,Long>(),
    /**
     * SrcIP Octet Counts
     */
		     sip_octs = new HashMap<String,Long>(),
    /**
     * DstIP Octet Counts
     */
		     dip_octs = new HashMap<String,Long>(),
    /**
     * DstPort Octet Counts
     */
		     dpt_octs = new HashMap<String,Long>();

    /**
     * Maximum sessions
     */
    long             max_sess,

    /**
     * Maximum octs
     */
                     max_octs,

    /**
     * Maximum pkts
     */
		     max_pkts;

    /**
     * Simple adder for a map.
     *
     *@param map map to add to
     *@param key key to add to within the map
     *@param add amount to add to the key
     */
    public long acc(Map<String,Long> map, String key, long add) { if (map.containsKey(key) == false) map.put(key,add); else map.put(key,map.get(key)+add); return map.get(key); }

    /**
     * Return the maximum value within this histogram for the specified accumulation type.
     *
     *@param accumulation_type variable to use for counting
     *
     *@return local maximum
     */
    public long max(AccumulationType accumulation_type) {
      switch (accumulation_type) {
        case packets: return max_pkts;
	case octets:  return max_octs;
        default:      return max_sess;
      }
    }

    /**
     * Render the histograms at the specified coordinates.
     *
     *@param g2d               graphics object
     *@param x                 x coordinate as the upper left
     *@param y                 y coordinate as the upper left
     *@param w                 maximum width of the histograms
     *@param accumulation_type which variable to use for counting
     *@param global_max        global max across all histograms -- if negative, ignore and a local max is used
     */
    public void render(Graphics2D g2d, int x, int y, int w, AccumulationType accumulation_type, long global_max) {
      int txt_h = Utils.txtH(g2d, "0");
      // Choose the right maps based on the accumulation types
      Map<String,Long> sipm,dipm,dptm; long max;
      switch (accumulation_type) {
        case packets: sipm = sip_pkts; dipm = dip_pkts; dptm = dpt_pkts; max = max_pkts; break;
	case octets:  sipm = sip_octs; dipm = dip_octs; dptm = dpt_octs; max = max_octs; break;
        default:      sipm = sip_sess; dipm = dip_sess; dptm = dpt_sess; max = max_sess; break;
      }
      if (global_max >= 0) { max = global_max; }

      // For the top three, make histograms...
      String top_three[];
      top_three = topThree(sipm); y = render(g2d, x, y, w, sipm, max, top_three, accumulation_type) + txt_h;
      top_three = topThree(dipm); y = render(g2d, x, y, w, dipm, max, top_three, accumulation_type) + txt_h;
      top_three = topThree(dptm); y = render(g2d, x, y, w, dptm, max, top_three, accumulation_type) + txt_h;
    }

    /**
     * Render the top three as a histogram.
     *
     *@param g2d graphics object
     *@param x   x coordinate to start the rendering
     *@param y   y coordinate to start the rendering
     *@param w   maximum possible width to use
     *@param map contains the looks for the entity counts
     *@param max the overall max value to use for scaling (normalized across all the histograms)
     *@param top the top elements to plot
     *@param accumulation_type way to accumulate...
     */
    public int render(Graphics2D g2d, int x, int y, int w, Map<String,Long> map, long max, String top[], AccumulationType accumulation_type) {
      int txt_h = Utils.txtH(g2d, "0"); for (int i=0;i<top.length;i++) {
        int bar_w = (int) ((w * map.get(top[i]))/max);
        Color color = Color.darkGray; color = new Color(accumulationColor(1, accumulation_type));
        g2d.setColor(color);          g2d.fillRect(x,y,bar_w,txt_h+2);
        String hr = Utils.humanReadable(map.get(top[i]));
	g2d.setColor(Color.darkGray); g2d.drawString(hr, x - 3 - Utils.txtW(g2d, hr), y + txt_h - 1);
	g2d.setColor(Color.white);    g2d.drawString(top[i], x + 2, y + txt_h -1);
        y += txt_h + 3;
      }
      return y;
    }

    /**
     * Find the top three items in a map.
     */
    public String[] topThree(Map<String,Long> map) {
      String ret[] = new String[0];
      if        (map.keySet().size() >= 3) { ret = new String[3]; Iterator<String> it = map.keySet().iterator();
                                             ret[0] = it.next(); ret[1] = it.next(); ret[2] = it.next(); sort(ret,map);
                                             while (it.hasNext()) { insertAndSort(it.next(), ret, map); }
      } else if (map.keySet().size() == 2) { ret = new String[2]; Iterator<String> it = map.keySet().iterator(); 
                                             ret[0] = it.next(); ret[1] = it.next();
				             if (map.get(ret[0]) < map.get(ret[1])) { String s = ret[0]; ret[0] = ret[1]; ret[1] = s; }
      } else if (map.keySet().size() == 1) { ret = new String[1]; 
                                             ret[0] = map.keySet().iterator().next(); 
      }
      return ret;
    }
    private void sort(String top[],Map<String,Long> map) {
      // Get top[0] to be the highest of the three
      if        (map.get(top[0]) >= map.get(top[1]) && map.get(top[0]) >= map.get(top[2])) { // fine - no changes
      } else if (map.get(top[1]) >= map.get(top[0]) && map.get(top[1]) >= map.get(top[2])) { String s = top[0]; top[0] = top[1]; top[1] = s;
      } else if (map.get(top[2]) >= map.get(top[0]) && map.get(top[2]) >= map.get(top[1])) { String s = top[0]; top[0] = top[2]; top[2] = s; }

      // Sort top[1] and top[2]
      if (map.get(top[2]) > map.get(top[1])) { String s = top[1]; top[1] = top[2]; top[2] = s; }
    }
    private void insertAndSort(String s, String top[], Map<String,Long> map) {
      if (map.get(s) > map.get(top[2])) { top[2] = s; sort(top,map); }
    }
  }

  /**
   * Accumulation types
   */
  enum AccumulationType { sessions, octets, packets };

  /**
   * Render context instance
   */
  RenderContext render_context;

  /**
   * Construct the render context for the plot.
   */
  public void constructPlot(XYChartType chart_type, AccumulationType accumulation_type) { render_context = new RenderContext(chart_type, accumulation_type); }

  /**
   * Parse the files and plot the information.
   *
   *@param files files to parse
   */
  public BufferedImage render(File files[], String header) throws IOException {
    if (aggregator == null || render_context == null) { throw new RuntimeException("Call aggregate() then constructPlot() prior to calling render()"); }

    for (int file_i=0;file_i<files.length;file_i++) {
      File file = files[file_i]; 
      System.err.println("Rendering File \"" + file + "\"...");
      render_context.setFileTranslation(aggregator.getFileTranslation(file));
      new CSVReader(file,render_context);
    }

    return render_context.getPlot(header);
  }

  /**
   * Visualization accumulation model
   */
  enum XYChartType { string_set, integer_set, long_sum };

  /**
   * XY Chart based on y-axis string categories.
   */
  class XYStringsChart {
    /**
     * Type of chart
     */
    XYChartType chart_type;

    /**
     * Render context -- contains information on how to make the rendering from the first file pass
     */
    RenderContext rc;

    /**
     * Y-like coordinate to transform the y string into an integer that can then be used to calculate the y coordinate
     */
    Map<String,Integer> y_lu;

    /**
     * Y-like coordinate to transform the y integer into an integer that can then be used to calculate the y coordinate
     */
    Map<Integer,Integer> y_lu_i;

    /**
     * Height of the chart
     */
    int h;

    /**
     * Two dimensional array for accumulating the string or integer sets
     */
    Object[][] objs;

    /**
     * Two dimensional array for accumulating the long sums
     */
    long[][] sums;

    /**
     * Construct the XY chart for a string-based y coordinate system
     */
    public XYStringsChart(XYChartType chart_type, RenderContext rc, Map<String,Integer> y_lu, int h) {
      this.chart_type = chart_type;
      this.rc         = rc;
      this.y_lu       = y_lu;
      this.h          = h;

      if (chart_type == XYChartType.string_set || chart_type == XYChartType.integer_set) {
        objs = new Object[h][rc.chart_w];
        switch (chart_type) {
          case string_set:   for (int y=0;y<objs.length;y++) for (int x=0;x<objs[y].length;x++) objs[y][x] = new HashSet<String>();  break;
	  case integer_set:  for (int y=0;y<objs.length;y++) for (int x=0;x<objs[y].length;x++) objs[y][x] = new HashSet<Integer>(); break;
	  default:  System.err.println("Should Not Be Here -- XYStringsChart.chart_type = default");
        }
      } else sums = new long[h][rc.chart_w];
    }

    /**
     * Construct the XY chart for an integer-based y coordinate system
     */
    public XYStringsChart(XYChartType chart_type, RenderContext rc, Map<Integer,Integer> y_lu_i, int h, boolean for_integers) {
      this.chart_type = chart_type;
      this.rc         = rc;
      this.y_lu_i     = y_lu_i;
      this.h          = h;

      if (chart_type == XYChartType.string_set || chart_type == XYChartType.integer_set) {
        objs = new Object[h][rc.chart_w];
        switch (chart_type) {
          case string_set:   for (int y=0;y<objs.length;y++) for (int x=0;x<objs[y].length;x++) objs[y][x] = new HashSet<String>();  break;
	  case integer_set:  for (int y=0;y<objs.length;y++) for (int x=0;x<objs[y].length;x++) objs[y][x] = new HashSet<Integer>(); break;
	  default:  System.err.println("Should Not Be Here -- XYStringsChart.chart_type = default");
        }
      } else sums = new long[h][rc.chart_w];
    }

    /**
     * Apply the final rendering phase to the accumulated/tabulated data.
     *
     *@return final rendering
     */
    public BufferedImage applyFinalRender(AccumulationType accumulation_type) {
      BufferedImage bi  = new BufferedImage(rc.chart_w, h, BufferedImage.TYPE_INT_RGB);
      Graphics2D    g2d = (Graphics2D) bi.getGraphics();

      if        (objs != null) {

      } else if (sums != null) {
        for (int y=0;y<sums.length;y++) for (int x=0;x<sums[y].length;x++) {
          if (sums[y][x] <= 0L) bi.setRGB(x,y,0x00);
	  else                  bi.setRGB(x,y,accumulationColor(sums[y][x], accumulation_type));
	}
      } else throw new RuntimeException("Nothing To Plot... Bailing Out");
      g2d.dispose();
      return bi;
    }


    /**
     * Render the private IP space.
     *
     *@return image that shows which IP ranges start with private space
     */
    public BufferedImage renderPrivateIPSpace() {
      BufferedImage bi  = new BufferedImage(2,h,BufferedImage.TYPE_INT_RGB);
      Graphics2D    g2d = (Graphics2D) bi.getGraphics();
      g2d.setColor(Color.black); g2d.fillRect(0,0,bi.getWidth(),bi.getHeight());
      if (y_lu != null) {
        Iterator<String> it = y_lu.keySet().iterator(); while (it.hasNext()) {
	  String s = it.next(); if (startsWithPrivateIP(s)) {
	    int y = yTransform(s);
	    g2d.setColor(Color.white);
	    g2d.fillRect(0,y,2,1);
	  }
	}
      }
      g2d.dispose();
      return bi;
    }


    /**
     * Transform a string into its y coordinate
     *
     *@param s string to transform
     *
     *@return y coordinate
     */
    public int yTransform(String s) { return (int) ((y_lu.get(s) * (h-1)) / y_lu.keySet().size()); }

    /**
     * Transform an integer into its y coordinate
     *
     *@param i integer to transform
     *
     *@return y coordinate
     */
    public int yTransform(int i) { return (int) ((y_lu_i.get(i) * (h-1)) / y_lu_i.keySet().size()); }

    /**
     * Accumulate for String sets
     *
     *@param y_str   string for calculating the y coordinate -- must be in the y_lu map
     *@param ts      timestamp used to calculate the x coordinate
     *@param set_str string for sets
     */
    // public void accumulate(String y_str, long ts, String set_str) { int y = yTransform(y_str), x = rc.xTransform(ts); ((Set<String>) objs[y][x]).add(set_str); }

    /**
     * Accumulate for integer sets
     *
     *@param y_str   string for calculating the y coordinate -- must be in the y_lu map
     *@param ts      timestamp used to calculate the x coordinate
     *@param set_int integer for sets
     */
    // public void accumulate(String y_str, long ts, Integer set_int) { int y = yTransform(y_str), x = rc.xTransform(ts); ((Set<Integer>) objs[y][x]).add(set_int); }

    /**
     * Accumulate for long values (sum)
     *
     *@param y_str   string for calculating the y coordinate -- must be in the y_lu map
     *@param ts      timestamp used to calculate the x coordinate
     *@param val     value to sum up
     */
    public void accumulate(String y_str, long ts, long val) { int y = yTransform(y_str), x = rc.xTransform(ts); sums[y][x] += val; }

    /**
     * Accumulate for long values (sum)
     *
     *@param y_str   string for calculating the y coordinate -- must be in the y_lu map
     *@param ts0     timestamp used to calculate the init x coordinate
     *@param ts1     timestamp used to calculate the final x coordinate
     *@param val     value to sum up
     */
    public void accumulate(String y_str, long ts0, long ts1, long val) { 
      int  y  = yTransform(y_str), 
           x0 = rc.xTransform(ts0),
	   x1 = rc.xTransform(ts1);
      int  bins    = x1 - x0 + 1;
      long bin_amt = val/bins; if (bin_amt == 0L) bin_amt = 1L;
      for (int x=x0;x<=x1;x++) sums[y][x] += bin_amt;
    }

    /**
     * Accumulate for String sets
     *
     *@param y_i     integer for calculating the y coordinate -- must be in the y_lu_i map
     *@param ts      timestamp used to calculate the x coordinate
     *@param set_str string for sets
     */
    // public void accumulate(int y_i, long ts, String set_str) { int y = yTransform(y_i), x = rc.xTransform(ts); ((Set<String>) objs[y][x]).add(set_str); }

    /**
     * Accumulate for integer sets
     *
     *@param y_i     integer for calculating the y coordinate -- must be in the y_lu_i map
     *@param ts      timestamp used to calculate the x coordinate
     *@param set_int integer for sets
     */
    // public void accumulate(int y_i, long ts, Integer set_int) { int y = yTransform(y_i), x = rc.xTransform(ts); ((Set<Integer>) objs[y][x]).add(set_int); }

    /**
     * Accumulate for long values (sum)
     *
     *@param y_i     integer for calculating the y coordinate -- must be in the y_lu_i map
     *@param ts      timestamp used to calculate the x coordinate
     *@param val     value to sum up
     */
    public void accumulate(int y_i, long ts, long val) { int y = yTransform(y_i), x = rc.xTransform(ts); sums[y][x] += val; }

    /**
     * Accumulate for long values (sum)
     *
     *@param y_i     integer for calculating the y coordinate -- must be in the y_lu_i map
     *@param ts0     timestamp used to calculate the init x coordinate
     *@param ts1     timestamp used to calculate the final x coordinate
     *@param val     value to sum up
     */
    public void accumulate(int y_i, long ts0, long ts1, long val) { 
      int  y  = yTransform(y_i), 
           x0 = rc.xTransform(ts0),
	   x1 = rc.xTransform(ts1);
      int  bins    = x1 - x0 + 1;
      long bin_amt = val/bins; if (bin_amt == 0L) bin_amt = 1L;
      for (int x=x0;x<=x1;x++) sums[y][x] += bin_amt;
    }
  }

  /**
   * Return the appropriate color (as an integer) for the specified value and accumulation type.
   *
   *@param value value to adjust the color for
   *@param type  accumulation type
   *
   *@return integer color (rgb)
   */
  public int accumulationColor(long value, AccumulationType type) {
    if (value == 0L) return 0x00; // black
    double base = 10; int brewer[] = null;
    switch (type) { case sessions: base = 3;  brewer = BrewerSimplified.seq7Color1(); break; 
                    case octets:   base = 10; brewer = BrewerSimplified.seq7Color2(); break; 
                    case packets:  base = 5;  brewer = BrewerSimplified.seq7Color3(); break; }
    int l = (int) log(base, value); if (l <= 1) l = 1; if (l >= brewer.length) l = brewer.length - 1;
    return brewer[l];
  }

  /**
   * Calculate the log with a different base.
   *
   *@param b base
   *@param x value
   *
   *@return log of x in base b
   */
  public double log(double b, double x) { return Math.log10(x)/Math.log10(b); }

  /**
   * Horizon chart - way to make a small height have multiple layers through the use of sequential colors
   */
  class HorizonChart {
    /**
     * Render context
     */
    RenderContext render_context;

    /**
     * Final height of the chart
     */
    int h;

    /**
     * Sums lined up with timeframs
     */
    long sums[];

    /**
     * Maximum sum found
     */
    long max_sum = 0L;

    /**
     * Construct the horizon chart.
     *
     *@param render_context render context
     *@param h              final height of the chart
     */
    public HorizonChart(RenderContext render_context, int h) {
      this.render_context = render_context;
      this.h              = h;
      sums = new long[render_context.chart_w];
    }

    /**
     * Accumulate a value into the appropriate chart location.
     *
     *@param ts  timestamp -- determines the x coordinate
     *@param val value to accumulate
     */
    public void accumulate(long ts, long val) { int x = render_context.xTransform(ts); sums[x] += val; if (sums[x] > max_sum) max_sum = sums[x]; }

    /**
     * Accumulate a value into a range of chart locations (spread over time).
     *
     *@param ts0  timestamp -- determines the init x coordinate
     *@param ts1  timestmap -- determines the final x coordate
     *@param val value to accumulate
     */
    public void accumulate(long ts0, long ts1, long val) {
      int  x0      = render_context.xTransform(ts0),
           x1      = render_context.xTransform(ts1);
      int  bins    = x1 - x0 + 1;
      long bin_amt = val/bins; if (bin_amt == 0L) bin_amt = 1L;
      for (int x=x0;x<=x1;x++) {
        sums[x] += bin_amt;
	if (sums[x] >max_sum) max_sum = sums[x];
      }
    }

    /**
     * Perform the final render for this chart.  Use the specified colors.
     *
     *@param colors colors to use -- zero is the background color...
     *
     *@return final rendering as an image object
     */
    public BufferedImage applyFinalRender(int colors[]) {
      BufferedImage bi  = new BufferedImage(sums.length, h, BufferedImage.TYPE_INT_RGB);
      Graphics2D    g2d = (Graphics2D) bi.getGraphics();

      Color cs[] = new Color[colors.length]; for (int i=0;i<cs.length;i++) cs[i] = new Color((colors[i] >> 16)&0x00ff,(colors[i] >> 8)&0x00ff,(colors[i] >> 0)&0x00ff);

      long sevenths = max_sum/7;

      for (int i=0;i<sums.length;i++) {
        if (sums[i] == 0L) { g2d.setColor(cs[0]); g2d.fillRect(i,0,1,h);
	} else             {
	  int pri = (int) (sums[i]/sevenths); if (pri < 1) pri = 1; if (pri >= cs.length) pri = cs.length - 1;
	  int sec = pri - 1;
	  g2d.setColor(cs[sec]); g2d.fillRect(i,0,1,h);
	  g2d.setColor(cs[pri]);
	  int bar_h = (int) ((h * (sums[i]%sevenths))/sevenths);
	  g2d.fillRect(i, h - bar_h, 1, bar_h);
	}
      }

      g2d.dispose();
      return bi;
    }
  }

  /**
   * Print the usage for this program.
   *
   *@param out output stream
   */
  public static void printUsage(PrintStream out) {
    out.println("java racetrack.utils.NetflowXYPlotter [options] [csv-files...]");
    out.println("");
    out.println("  Basic Options");
    out.println("  =============");
    out.println("  -lowport");
    out.println("  -duration");
    out.println("");
    out.println("  Time Override");
    out.println("  =============");
    out.println("  -minutes");
    out.println("  -seconds");
    out.println("  -hours");
    out.println("  -hours4");
    out.println("  -5minutes");
    out.println("  -10minutes");
    out.println("  -days");
    out.println("");
    out.println(" Start/Stop Times");
    out.println(" ================");
    out.println(" -begin YYYY-MM-DDTHH:MM:SS");
    out.println(" -end   YYYY-MM-DDTHH:MM:SS");
  }

  /**
   * Standalone class for plotting too much netflow to analyze interactively.
   *
   *@Param args command line arguments
   */
  public static void main(String args[]) {
    // Print out the apache license info...
    System.out.println("License Information");
    System.out.println("");
    System.out.println("Copyright 2016 David Trimm");
    System.out.println("");
    System.out.println("Licensed under the Apache License, Version 2.0 (the \"License\");");
    System.out.println("you may not use this file except in compliance with the License.");
    System.out.println("You may obtain a copy of the License at");
    System.out.println("");
    System.out.println("http://www.apache.org/licenses/LICENSE-2.0");
    System.out.println("");
    System.out.println("Unless required by applicable law or agreed to in writing, software");
    System.out.println("distributed under the License is distributed on an \"AS IS\" BASIS,");
    System.out.println("WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.");
    System.out.println("See the License for the specific language governing permissions and");
    System.out.println("limitations under the License.");
    System.out.println("");

    try {
      if (args.length == 0) { printUsage(System.err); System.exit(0); }

      String header = "racetrack.utils.NetflowXYPlotter"; for (int i=0;i<args.length;i++) header += " " + args[i];

      NetflowXYPlotter plotter   = new NetflowXYPlotter(args);
      File             files[]   = plotter.validFiles();
      String           file_base = "" + System.currentTimeMillis();

      //
      // Phase 1 Aggregates the information in the files
      //
      plotter.aggregate(files);

      //
      // Construct the plot
      //
      // chart_type      accumulation_type
      // ----------      -----------------
      // long_sum        sessions
      // long_sum        octets
      // long_sum        packets
      //
      BufferedImage plot = null;

      // Sessions
      plotter.constructPlot(XYChartType.long_sum, AccumulationType.sessions);
      // Phase 2 Rereads the files and does the plots
      plot = plotter.render(files, header);
      // Write to an image file
      ImageIO.write(plot, "PNG", new FileOutputStream(new File("xy_sess_" + file_base + ".png")));

      // Sessions
      plotter.constructPlot(XYChartType.long_sum, AccumulationType.octets);
      // Phase 2 Rereads the files and does the plots
      plot = plotter.render(files, header);
      // Write to an image file
      ImageIO.write(plot, "PNG", new FileOutputStream(new File("xy_octss_" + file_base + ".png")));

      // Sessions
      plotter.constructPlot(XYChartType.long_sum, AccumulationType.packets);
      // Phase 2 Rereads the files and does the plots
      plot = plotter.render(files, header);
      // Write to an image file
      ImageIO.write(plot, "PNG", new FileOutputStream(new File("xy_pkts_" + file_base + ".png")));

    } catch (Throwable t) {
      System.err.println("Throwable: " + t);
      t.printStackTrace(System.err);
    }
  }

  /**
   * Return true if the string starts with a private IP address prefix.
   *
   *@return true for private ip address
   */
  public static boolean startsWithPrivateIP(String s) {
    return (s.startsWith("10.") ||
            s.startsWith("192.168.") ||
	    s.startsWith("172.16.") || s.startsWith("172.17.") || s.startsWith("172.18.") || s.startsWith("172.19.") ||
	    s.startsWith("172.20.") || s.startsWith("172.21.") || s.startsWith("172.22.") || s.startsWith("172.23.") ||
	    s.startsWith("172.24.") || s.startsWith("172.25.") || s.startsWith("172.26.") || s.startsWith("172.27.") ||
	    s.startsWith("172.28.") || s.startsWith("172.29.") || s.startsWith("172.30.") || s.startsWith("172.31."));
  }
}

/**
 * Simplified colors from ColorBrewer2.org
 */
class BrewerSimplified {
  static int seq7c1[], // sequential 7 color scheme 1
             seq7c2[], // sequential 7 color scheme 2
	     seq7c3[]; // sequential 7 color scheme 3
  static {
    seq7c1 = new int[8];
      seq7c1[7] = 0x00f6eff7;
      seq7c1[6] = 0x00d0d1e6;
      seq7c1[5] = 0x00a6bddb;
      seq7c1[4] = 0x0067a9cf;
      seq7c1[3] = 0x003690c0;
      seq7c1[2] = 0x0002818a;
      seq7c1[1] = 0x00016450;
      seq7c1[0] = 0x00000000; // Use black as the bottom to match the background... i.e., make it look transparent

    seq7c2 = new int[8];
      seq7c2[7] = 0x00fef0d9;
      seq7c2[6] = 0x00fdd49e;
      seq7c2[5] = 0x00fdbb84;
      seq7c2[4] = 0x00fc8d59;
      seq7c2[3] = 0x00ef6548;
      seq7c2[2] = 0x00d7301f;
      seq7c2[1] = 0x00990000;
      seq7c2[0] = 0x00000000; // Use black as the bottom to match the background... i.e., make it look transparent

    seq7c3 = new int[8];
      seq7c3[7] = 0x00feebe2;
      seq7c3[6] = 0x00fcc5c0;
      seq7c3[5] = 0x00fa9fb5;
      seq7c3[4] = 0x00f768a1;
      seq7c3[3] = 0x00dd3497;
      seq7c3[2] = 0x00ae017e;
      seq7c3[1] = 0x007a0177;
      seq7c3[0] = 0x00000000; // Use black as the bottom to match the background... i.e., make it look transparent
  }
  public static int[] seq7Color1() { return seq7c1; }
  public static int[] seq7Color2() { return seq7c2; }
  public static int[] seq7Color3() { return seq7c3; }
}

