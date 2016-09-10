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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.swing.JList;

import racetrack.framework.BundlesDT;
import racetrack.visualization.BrewerColorScale;
import racetrack.visualization.ColorScale;

/**
 * Mix of utility methods / convenience methods for the application.
 *
 *@author  D. Trimm
 *@version 1.0
 */
public class Utils {
  /**
   * Durations in milliseconds
   */
  public static final long SECONDS = 1000L,
                           MINUTES = 60*SECONDS,
			   HOURS   = 60*MINUTES,
			   DAYS    = 24*HOURS,
			   WEEKS   = 7*DAYS,
			   MONTHS  = 31*DAYS,  // Approximately...
			   YEARS   = 365*DAYS; // Approximately...

  /**
   * Conversion from degrees to radians.
   *
   *@param  in_deg degrees
   *
   *@return radians
   */
  public static double toRad(Double in_deg) { return (in_deg/360.0) * Math.PI * 2.0; }

  /**
   * Conversion from degrees to radians.
   *
   *@param  in_deg degrees
   *
   *@return radians
   */
  public static double toRad(int    in_deg) { return (in_deg/360.0) * Math.PI * 2.0; }

  /**
   * The color light blue
   */
  public static Color  lightBlue = new Color(0x00ccff);

  /**
   * Convert an integer version of an IPv4 address into its string equivalent.
   *
   *@param  ip integer rep of IPv4 address
   *
   *@return string representation
   */
  public static String intToIPString(int ip) {
    return "" + ((ip >> 24) & 0x00ff) + "." +
                ((ip >> 16) & 0x00ff) + "." +
                ((ip >>  8) & 0x00ff) + "." + 
                ((ip >>  0) & 0x00ff);
  }

  /**
   * Convert an IPv4 string into it's integer equivalent.
   *
   *@param  ip_str string rep of IPv4 address
   *
   *@return integer representation
   */
  public static int    strToIPInt(String ip_str) {
    StringTokenizer st = new StringTokenizer(ip_str, ".:");
    int ip  = (Integer.parseInt(st.nextToken()) & 0x00ff); ip = ip << 8;
        ip |= (Integer.parseInt(st.nextToken()) & 0x00ff); ip = ip << 8;
        ip |= (Integer.parseInt(st.nextToken()) & 0x00ff); ip = ip << 8;
        ip |= (Integer.parseInt(st.nextToken()) & 0x00ff);
    return ip;
  }

  /**
   * Return the text off of the clipboard.
   *
   *@param  component component to pull clipboard from
   *
   *@return clipboard text
   */
  public static String getClipboardText(Component component) {
    Clipboard    clipboard = component.getToolkit().getSystemClipboard();
    Transferable trans     = clipboard.getContents(component);
    if (trans != null && trans.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      try { 
        return (String) trans.getTransferData(DataFlavor.stringFlavor);
      } catch (UnsupportedFlavorException ufe) {
        System.err.println("genericClipboard failed...  unsupported data flavor : " + ufe);
        ufe.printStackTrace(System.err);
	return null;
      } catch (IOException ioe) {
        System.err.println("genericClipboard failed...  io exception : " + ioe);
        ioe.printStackTrace(System.err);
	return null;
      }
    } else return null;
  }

  /**
   * Put a string on the clipboard.
   *
   *@param str string to put on clipboard
   */
  public static void copyToClipboard(String str) {
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    StringSelection selection = new StringSelection(str);
    clipboard.setContents(selection, null);
  }

  /**
   * Put an image on the clipboard.  Based on description from
   * "http://www.coderanch.com/t/333565/GUI/java/BufferedImage-System-Clipboard"
   * Does not seem to work correctly across platforms.
   *
   *@param image image to place on clipboard
   */
  public static void copyToClipboard(Image image) {
    TransferableImage trans = new TransferableImage(image);
    ClipboardOwner    owner = new ClipboardOwner() { public void lostOwnership(Clipboard clipboard, Transferable contents) { System.err.println("lostOwnership();"); } };
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(trans,owner);
  }
  static class TransferableImage implements Transferable {
    Image image; public TransferableImage(Image image) { this.image = image; }
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
      if (flavor.equals(DataFlavor.imageFlavor) && image != null) return image; else throw new UnsupportedFlavorException(flavor);
    }
    public DataFlavor[] getTransferDataFlavors() {
      DataFlavor[] flavors = new DataFlavor[1];
      flavors[0] = DataFlavor.imageFlavor;
      return flavors;
    }
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      DataFlavor[] flavors = getTransferDataFlavors();
      for (int i=0;i<flavors.length;i++) if (flavor.equals(flavors[i])) return true;
      return false;
    }
  }

  /**
   * Extract the cidr mask from a cidr string
   *
   *@param  cidr_str IPv4 CIDR string
   *
   *@return bits set in the mask
   */
  public static  int      cidrMask(String cidr_str) {
    StringTokenizer st = new StringTokenizer(cidr_str,"/"); st.nextToken(); int size = Integer.parseInt(st.nextToken());
    int mask = 0x00;
    for (int i=0;i<size;i++) {
      mask = mask >> 1;
      mask = mask | 0x80000000;
    }
    return mask;
  }
  /**
   * Return the length of the cidr mask.
   *
   *@param  cidr_str IPv4 CIDR string
   *
   *@return length in bits
   */
  public static  int      cidrBits(String cidr_str) {
    StringTokenizer st = new StringTokenizer(cidr_str,"/"); return ipAddrToInt(st.nextToken()) & cidrMask(cidr_str);
  }

  /**
   * Determine if an IPv4 address matches a cidr mask.
   *
   *@param  ipv4      IPv4 address to check
   *@param  cidr_bits length of CIDR mask
   *@param  cidr_mask CIDR mask bits
   *
   *@return true if the IPv4 address matches
   */
  public static  boolean  cidrMatch(int ipv4, int cidr_bits, int cidr_mask) {
    return (ipv4 & cidr_mask) == (cidr_bits & cidr_mask);
  }

  /**
   * Determine if an IPv4 address matches a cidr mask.
   *
   *@param ip_str   IPv4 address to check
   *@param cidr_str IPv4 CIDR string
   *
   *@return true if the IPv4 address matches
   */
  public static boolean ipMatchesCIDR(String ip_str, String cidr_str) {
    try {
      // Convert to integers
      int ip      = strToIPInt(ip_str);
      StringTokenizer st = new StringTokenizer(cidr_str,"/");
      int cidr_ip = strToIPInt(st.nextToken());
      int mask    = Integer.parseInt(st.nextToken());
      // Sets the bits where they differ
      ip = ip ^ cidr_ip;
      for (int i=0;i<mask;i++) {
        if ((ip & 0x80000000) != 0) return false;
	ip = ip << 1;
      }
      return true;
    } catch (NumberFormatException nfe) { return false; }
  }

  /**
   * Return the /8 representation of the specified IP address.
   *
   *@param str IPv4 address string
   *
   *@return /8 representation (as a string)
   */
  public static String ipv4CIDR08(String str)  { return str.substring(0,str.indexOf(".")) + ".0.0.0"; } 

  /**
   * Return the /16 representation of the specified IP address.
   *
   *@param str IPv4 address string
   *
   *@return /16 representation (as a string)
   */
  public static String ipv4CIDR16(String str)  { StringTokenizer st = new StringTokenizer(str, "."); return st.nextToken() + "." + st.nextToken() + ".0.0"; } 

  /**
   * Return the /24 representation of the specified IP address.
   *
   *@param str IPv4 address string
   *
   *@return /24 representation (as a string)
   */
  public static String ipv4CIDR24(String str)  { return str.substring(0,str.lastIndexOf(".")) + ".0"; } 

  /**
   * Return the most significant octet of an IPv4 address
   *
   *@param str IPv4 address string
   *
   *@return most significant octet as a string
   */
  public static String ipv4Octet00(String str) { return str.substring(0,str.indexOf(".")); }

  /**
   * Return the second most significant octet of an IPv4 address
   *
   *@param str IPv4 address string
   *
   *@return second most significant octet as a string
   */
  public static String ipv4Octet01(String str) { StringTokenizer st = new StringTokenizer(str,"."); st.nextToken(); return st.nextToken(); }

  /**
   * Return the third most significant octet of an IPv4 address
   *
   *@param str IPv4 address string
   *
   *@return third most significant octet as a string
   */
  public static String ipv4Octet02(String str) { StringTokenizer st = new StringTokenizer(str,"."); st.nextToken(); st.nextToken(); return st.nextToken(); }

  /**
   * Return the least significant octet of an IPv4 address
   *
   *@param str IPv4 address string
   *
   *@return least significant octet as a string
   */
  public static String ipv4Octet03(String str) { return str.substring(str.lastIndexOf(".")+1,str.length()); }

  /**
   * Return the top-level domain for a domain string.
   *
   *@param  str domain
   *
   *@return tld string for the domain
   */
  public static String domainTLD(String str)   { return str.substring(str.lastIndexOf(".")+1,str.length()); } 

  /**
   * Regular expressions to match a relationship string.  Not implemented.
   */
  static String  vars_regex           = "([a-zA-Z0-9 \\-\\.:])",
                 entity_regex         = vars_regex + "([|]" + vars_regex + "){0,1}",
                 tag_regex            = vars_regex,
		 type_tag_regex       = vars_regex + "[=]" + vars_regex,
	 	 hier_tag_regex       = vars_regex + "[:]" + vars_regex + "([:]" + vars_regex + ")*",
                 tags_regex           = "([|][|]("+tag_regex+")|("+type_tag_regex+")|("+hier_tag_regex+"))*",
		 relationship_regex   = entity_regex + tags_regex + "[\\|][:][:][\\|]" +
		                        entity_regex + tags_regex + "[\\|][:][:][\\|]" +
		                        entity_regex + tags_regex;
  static Pattern relationship_pattern = Pattern.compile("^" + relationship_regex + "$");
  public static  String  getRelationshipRegex()     { return relationship_regex; }
  public static  boolean isRelationship(String str) { return relationship_pattern.matcher(str).matches(); }

  /**
   * Floating point regular expression
   */
  public static String float_regex = "[+-]{0,1}[0-9]+[.][0-9]+";

  /**
   * Return the regular expression for a floating point number
   *
   *@return float regex
   */
  public static String getFloatRegex() { return float_regex; }

  /**
   * Integer regular expression
   */
  public static String integer_regex = "[+-]{0,1}[0-9]+";

  /**
   * Return the regular expression for an integer.
   *
   *@return integer regular expression
   */
  public static String getIntegerRegex() { return integer_regex; }

  /**
   * Regular expression for md5
   */
  static String md5_regex = "[a-fA-F0-9]{32}($|[ \t\r\n])";
  // static String md5_regex = "[a-fA-F0-9]{32}"; // This one also found any hashes that were longer than an md5

  /**
   * Pattern for md5
   */
  static Pattern md5_pattern = Pattern.compile("^" + md5_regex + "$");

  /**
   * Return the regular expression for an md5 hash.
   *
   *@return regex for md5
   */
  public static String getMD5Regex() { return md5_regex; }

  /**
   * Determine if the string is an md5 hash.
   *
   *@param str string to check
   *
   *@return true if string matches md5 pattern
   */
  public static boolean isMD5(String str) { return md5_pattern.matcher(str).matches(); }

  /**
   * Regular expression for email
   */
  static String email_regex = "[a-zA-Z0-9.-]+[@]([a-zA-Z]+[.])+[a-zA-Z0-9]+";

  /**
   * Pattern for email regular expression
   */
  static Pattern email_pattern = Pattern.compile("^" + email_regex + "$");

  /**
   * Return the regular expression for an email.
   *
   *@return email regular expression
   */
  public static String getEmailRegex() { return email_regex; }

  /**
   * Determines if the string is an email address.
   *
   *@param  str string to check
   *
   *@return true if the string matches the email regular expression
   */
  public static boolean isEmail(String str) { return email_pattern.matcher(str).matches(); }

  /**
   * Return the domain portion of an email address.
   *
   *@param email email address---must contain @ symbol
   *
   *@return domain portion of email address
   */
  public static String emailDomain(String email) {
    if (email.indexOf("@") >= 0) return email.substring(email.indexOf("@") + 1, email.length());
    else return email;
  }

  /**
   * Regular expression for a domain.  Very generic...  needs to have the lowest precedence for matches.
   */
  // static String  domain_regex   = "([a-zA-Z0-9-]+[.])+([a-zA-Z0-9-]+)";

  /**
   * User agent regular expression.  Probably needs to be re-written.
   */
  static String  useragent_regex   =     "(([a-zA-Z]+)[/][0-9]+[.][0-9]+[ ](\\[.+\\][ ]){0,1}[(].+[)].*)" +
                                     "|" + "(Lynx[/].*)" +
				     "|" + "(Mozil.*[/].*)" +
				     "|" + "(Outlook.*)" +
				     "|" + "(.*[(]compatible.*[)].*)";

  /**
   *  Pattern for the user agent regular expression
   */
  static Pattern useragent_pattern = Pattern.compile("^" + useragent_regex + "$");

  /**
   * Return the regular expression for user agent strings.
   *
   *@return user agent regular expression
   */
  public static String  getUserAgentRegex()     { return useragent_regex; }

  /**
   * Determines if the string is a user agent.
   *
   *@param  str string to check
   *
   *@return true if the string matches the user agent regular expression
   */
  public static boolean isUserAgent(String str) { return useragent_pattern.matcher(str).matches(); }

  /**
   * Regular expression for a domain.  Very generic...  needs to have the lowest precedence for matches.
   */
  // static String  domain_regex   = "([a-zA-Z0-9-]+[.])+([a-zA-Z0-9-]+)";
  // static String  domain_regex   = "((([a-zA-Z0-9]+[a-zA-Z0-9]*[a-zA-Z0-9]+)|[a-zA-Z0-9])[.])+([a-zA-Z0-9]*[a-zA-Z]+[a-zA-Z0-9]*)";
  // ---> Above pattern dies for the following "musicnews.bono.cobain.britney.sade.snocoretix.alanis.bjork.metallica%40artistdirect"
  static String domain_regex = "[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})";

  /**
   * Pattern for domain regular expression
   */
  static Pattern domain_pattern = Pattern.compile("^" +domain_regex + "$");

  /**
   * Return the regular expression for a domain.
   *
   *@return regular expression for a domain
   */
  public static String  getDomainRegex()     { return domain_regex; }

  /**
   * Regex for mac addresses
   */
  static String mac_regex = "(([a-fA-F0-9]{2}[:-]){5}([a-fA-F0-9]{2}))|([a-fA-F0-9]{12})";

  /**
   * Return the regular expression for a MAC address
   *
   *@return regular expression for a MAC address
   */
  public static String  getMACRegex() { return mac_regex; }

  /**
   * Check a string to see if it matches the domain regular expression.
   *
   *@param  str string to check
   *
   *@return true if string matches domain regular expression
   */
  public static boolean isDomain(String str) { return domain_pattern.matcher(str).matches(); }

  /**
   * Regular expression to match a URL. From http://regexlib.com/Search.aspx?k=URL&AspxAutoDetectCookieSupport=1
   */
  static String  url_regex   = "([Hh][Tt][Tt][Pp]|[Ff][Tt][Pp]|[Hh][Tt][Tt][Pp][Ss]):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?";

  /**
   * Pattern for URL regular expression
   */
  static Pattern url_pattern = Pattern.compile("^" + url_regex + "$");

  /**
   * Return the regular expression for a URL.
   *
   *@return URL regular expression
   */
  public static String  getURLRegex()         { return url_regex; }

  /**
   * Check a string to see if it matches the URL regular expression.
   *
   *@param  str string to check
   *
   *@return true if string matches URL regular expression
   */
  public static boolean isURL(String str)     { return url_pattern.matcher(str).matches(); }

  /**
   * Extract the domain from the URL string.
   *
   *@param  url URL string
   *
   *@return domain
   */
  public static String  urlDomain(String url) {
    int doubles = url.indexOf("//");
    if (doubles >= 0) { int next = url.indexOf("/",doubles+2);
      if (next >= 0) return url.substring(doubles+2,next);
      else           return url.substring(doubles+2,url.length());
    }
    return url;
  }

  /** 
   * Extract the page from the URL string
   *
   *@param url URL string
   *
   *@return page
   */
  public static String  urlPage(String url)   {
    int doubles = url.indexOf("//");
    int last    = url.lastIndexOf("/");
    if      (doubles < 0 || last < 0) return url;
    else if (last > doubles+2)        return url.substring(last,url.length());
    else                              return "[default]";
  }

  /**
   * Extract the path from the URL string
   *
   *@param url URL string
   *
   *@return path
   */
  public static String  urlPath(String url)   {
    int doubles = url.indexOf("//");
    if (doubles >= 0) { 
      int next = url.indexOf("/",doubles+2);
      if (next >= 0) {
        int last = url.lastIndexOf("/");
	if (last > doubles+2) return url.substring(next,last);
	else                  return "[root]";
      }
    }
    return url;
  }


  /**
   * Regular expression for an octet.
   * From http://answers.oreilly.com/topic/318-how-to-match-ipv4-addresses-with-regular-expressions/
   */
  static String octet_regex = "(25[0-5]|2[0-4][0-9]|[01][0-9][0-9]|[0-9][0-9]|[0-9])";

  /**
   * Regular expression for an IPv4 address
   * From http://answers.oreilly.com/topic/318-how-to-match-ipv4-addresses-with-regular-expressions/
   */
  static String ipv4_regex  = octet_regex + "[.]" + octet_regex + "[.]" + octet_regex + "[.]" + octet_regex;

  /**
   * Pattern for an IPv4 address
   */
  static Pattern ipv4_pattern = Pattern.compile("^" + ipv4_regex + "$");

  /**
   * Return the regular expression for an IPv4 address.
   *
   *@return IPv4 regular expression
   */
  public static String  getIPv4Regex()     { return ipv4_regex; }

  /**
   * Check a string for a match against the IPv4 regular expression.
   *
   *@param str string to check
   *
   *@return true if string match IPv4 regular expression
   */
  public static boolean isIPv4(String str) { return ipv4_pattern.matcher(str).matches(); }

  /**
   * Regular express for an IPv4 CIDR mask
   */
  static String ipv4cidr_regex = ipv4_regex + "[/](3[0-2]|[0-2][0-9]|[0-9])";

  /**
   * Pattern for an IPv4 CIDR mask
   */
  static Pattern ipv4cidr_pattern = Pattern.compile("^" + ipv4cidr_regex + "$");

  /**
   * Return the regular expression for an IPv4 CIDR mask
   *
   *@return IPv4 CIDR regular expression
   */
  public static String  getIPv4CIDRRegex()     { return ipv4cidr_regex; }

  /**
   * Check a string for a match against the IPv4 CIDR regular expression.  Note that this
   * method does not match an IPv4 address against a CIDR mask.
   *
   *@param str string to check
   *
   *@return true if string matches IPv4 CIDR mask
   */
  public static boolean isIPv4CIDR(String str) { return ipv4cidr_pattern.matcher(str).matches(); }

  /**
   * Regular express for the IPv6 address.  
   * From http://forums.intermapper.com/viewtopic.php?t=452
   * Has not been tested.
   */
  static String  ipv6_regex   = "\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))(%.+)?\\s*";

  /**
   * Pattern for an IPv6 address
   */
  static Pattern ipv6_pattern = Pattern.compile("^" + ipv6_regex + "$");

  /**
   * Return the regular expression for an IPv6 address.
   *
   *@return IPv6 regular expression
   */
  public static String  getIPv6Regex()     { return ipv6_regex; }

  /**
   * Check a string for a match against the IPv6 regular expression.
   *
   *@param str string to check
   *
   *@return true if string matches IPv6 regular expression
   */
  public static boolean isIPv6(String str) { return ipv6_pattern.matcher(str).matches(); }

  /**
   * Regular expression for a timestamp.  Timestamps are from most significant to 
   * least significant order: YYYY-MM-DD HH:MM:SS.mmmZ.
   */
  static String  timestamp_regex   = "(" +
                                         "[0-9]{4}[-./](1[0-2]|0[1-9])[-./](3[01]|[12][0-9]|[0][1-9])" +
                                         "([ T]" +
				         "(2[0-3]|[01][0-9])([:]([0-5][0-9])([:]([0-5][0-9])([.][0-9]*){0,1}[Z]{0,1}){0,1}){0,1}){0,1}" +
                                     ")|(" +
				       "[0-9]{4}(12|11|10|0[1-9])(0[1-9]|[12][0-9]|3[01])" +
				     ")|(" +
				       "[0-9]{4}(12|11|10|0[1-9])(0[1-9]|[12][0-9]|3[01])" + 
				       "[ ]" +
				       "([01][0-9]|[2][0-3])[0-5][0-9][Z]" +
				     ")|(" +
				       "[0-9]{4}(12|11|10|0[1-9])(0[1-9]|[12][0-9]|3[01])" + 
				       "[ ]" +
				       "([01][0-9]|[2][0-3])[0-5][0-9][0-5][0-9][Z]" +
				     ")";

  /**
   * Pattern for timestamp regular expression
   */
  static Pattern timestamp_pattern = Pattern.compile("^" + timestamp_regex + "$");

  /**
   * Return the regular expression for a timestamp.
   *
   *@return timestamp regular expression
   */
  public static String   getTimeStampRegex()     { return timestamp_regex; }

  /**
   * Check a string for a match against the timestamp regular expression.
   *
   *@param str string to check
   *
   *@return true if string matches timestamp regular expression
   */
  public static boolean  isTimeStamp(String str) { return timestamp_pattern.matcher(str).matches(); }

  /**
   * Test two generic shapes for an intersection.  Method is optimized to use rectangular
   * conversion if possible.  If not, then an area match is performed.
   *
   *@param s0 first shape
   *@param s1 second shape
   *
   *@return true if shapes overlap
   */
  public static boolean genericIntersects(Shape s0, Shape s1) {
    if      (s0 instanceof Rectangle2D) return s1.intersects((Rectangle2D) s0);
    else if (s1 instanceof Rectangle2D) return s0.intersects((Rectangle2D) s1);
    else {
      if (s0.intersects(s1.getBounds2D()) && s1.intersects(s0.getBounds2D())) {
        Area a0 = new Area(s0), a1 = new Area(s1); a0.intersect(a1);
        return a0.isEmpty() == false;
      } else return false;
    }
  }

  /**
   * Compute the distance between two points.
   *
   *@param p0 first point
   *@param p1 second point
   *
   *@return distance between the two points
   */
  public static double distance(Point2D p0, Point2D p1) {
    double dx = p0.getX() - p1.getX();
    double dy = p0.getX() - p1.getY();
    return (double) Math.sqrt(dx*dx + dy*dy);
  }

  /**
   * Compute the cross product for vectors
   * From http://www.topcoder.com/tc?module=Static&d1=tutorials&d2=geometry1
   *
   *@param a  base point for the vectors
   *@param b  vector 1 direction
   *@param c  vector 2 direction
   *
   *return cross product
   */
  public static double crossProduct(Point2D.Double a, Point2D.Double b, Point2D.Double c) {
    double ab0 = b.x - a.x;
    double ab1 = b.y - a.y;
    double ac0 = c.x - a.x;
    double ac1 = c.y - a.y;
    return ab0*ac1-ab1*ac0;
  }

  /**
   * Compute the dot product for vectors
   * From http://www.topcoder.com/tc?module=Static&d1=tutorials&d2=geometry1
   *
   *@param a  base point for the vectors
   *@param b  vector 1 direction
   *@param c  vector 2 direction
   *
   *@return dot product
   */
  public static double dotProduct(Point2D.Double a, Point2D.Double b, Point2D.Double c) {
    double ab0 = b.x - a.x;
    double ab1 = b.y - a.y;
    double bc0 = c.x - b.x;
    double bc1 = c.y - b.y;
    return ab0*bc0+ab1*bc1;
  }

  /**
   * Return the length of a vector.  Typically for normalizing.
   *
   *@param deltax x component of vector
   *@param deltay y component of vector
   *
   *@return length of vector
   */
  public static double length(double deltax, double deltay) {
    return Math.sqrt(deltax*deltax + deltay*deltay);
  }

  /**
   * Return the distance between two points.
   *
   *@param x0 x coordinate of first point
   *@param y0 y coordinate of first point
   *@param x1 x coordinate of second point
   *@param y1 y coordinate of second point
   *
   *@return distance between two points
   */
  public static double distance(double x0, double y0, double x1, double y1) {
    double dx = x1 - x0, dy = y1 -y0;
    return length(dx,dy);
  }

  /**
   * Return the direction of a vector in radians.
   *
   *@param deltax x component of vector
   *@param deltay y component of vector
   *
   *@return radians
   */
  public static double direction(double deltax, double deltay) {
    double length = Math.sqrt(deltax*deltax + deltay*deltay);
    double n_vx   = 0.0, n_vy = 0.0, angle = 0.0;

    if (length > 0.0) { n_vx = deltax/length; n_vy = deltay/length; }

    if      (n_vx >= 0.0f && n_vy >= 0.0f) angle = (double) (Math.acos(n_vx));
    else if (n_vx >= 0.0f && n_vy <  0.0f) angle = (double) (2f*Math.PI - Math.acos( n_vx));
    else if (                n_vy >= 0.0f) angle = (double) (Math.PI    - Math.acos(-n_vx));
    else                                   angle = (double) (Math.PI    + Math.acos(-n_vx));
    return angle;
  }

  /**
   * Returns the smaller angle between the two radian angles.  Seems
   * to always return positives.
   *
   *@param a0 angle one
   *@param a1 angle two
   *
   *@return smallest angle difference
   */
  public static double smallestAngleDifference(double a0, double a1) {
    a0 = normalizeAngle(a0);
    a1 = normalizeAngle(a1);
    if      (a0 == a1) return 0.0;
    else if (a0 >  a1) {
      double d0 = a0 - a1;
      double d1 = (a1+2*Math.PI) - a0;
      return (d0 < d1) ? d0 : d1;
    } else { // a1 > a0
      double d0 = a1 - a0;
      double d1 = (a0+2*Math.PI) - a1;
      return (d0 < d1) ? d0 : d1;
    }
  }

  /**
   *
   */
  public static int smallestAngleDir(double a0, double a1) {
    a0 = normalizeAngle(a0);
    a1 = normalizeAngle(a1);
    if      (a0 == a1) return 0; // No direction...
    else if (a0 >  a1) {
      double d0 = a0 - a1;
      double d1 = (a1+2*Math.PI) - a0;
      return (d0 < d1) ? -1 : +1;
    } else { // a1 > a0
      double d0 = a1 - a0;
      double d1 = (a0+2*Math.PI) - a1;
      return (d0 < d1) ? +1 : -1;
    }
  }

  /**
   * Normalize an angle so that it falls within 0...2pi.
   *
   *@param angle angle in radians
   *
   *@return normalized angle
   */
  public static double normalizeAngle(double angle) {
    while (angle <  0.0)       angle += 2*Math.PI;
    while (angle >= 2*Math.PI) angle -= 2*Math.PI;
    return angle;
  }

  public static double posAngleTo(double a0, double a1) {
    a0 = normalizeAngle(a0); a1 = normalizeAngle(a1);
    return normalizeAngle(a1 + 2*Math.PI - a0);
  }
  public static double negAngleTo(double a0, double a1) {
    a0 = normalizeAngle(a0); a1 = normalizeAngle(a1);
    return -normalizeAngle(a0 + 2*Math.PI - a1);
  }

  /**
   * Provide the shorest angle between two vectors.
   *
   *@param dx0 x component of first vector
   *@param dy0 y component of first vector
   *@param dx1 x component of second vector
   *@param dy1 y component of second vector
   *
   *@return shortest angle between two vectors in radians
   */
  public static double shortestAngle(double dx0, double dy0, double dx1, double dy1) {
    double a0 = direction(dx0,dy0);
    double a1 = direction(dx1,dy1);
    double pos_angle = posAngleTo(a0, a1);
    double neg_angle = negAngleTo(a0, a1);
    if (Math.abs(pos_angle) > Math.abs(neg_angle)) return neg_angle;
    else                                           return pos_angle;
  }

  /**
   * String quotes from around a string.  Examples include:
   * - Take "this" and produce this
   * - Take "this  and produce this
   * - Take this"  and produce this
   * - Take this   and produce this
   *
   *@param with_quotes string to process
   *
   *@return string without quotes at beginning or end
   */
  public static String stripQuotes(String with_quotes) {
    if (with_quotes.charAt(0)   == '\"') with_quotes = with_quotes.substring(1);
    int l = with_quotes.length();
    if (with_quotes.charAt(l-1) == '\"') with_quotes = with_quotes.substring(0,l-1);
    return with_quotes;
  }

  /**
   * Remove spaces at the beginning and end of the string.
   *
   *@param str string to remove the spaces from
   *
   *@return string without beginning or ending whitespaces
   */
  public static String stripSpaces(String str) {
    while (str.length() > 0 && (str.charAt(0)              == ' '  ||
                                str.charAt(0)              == '\t' || 
                                str.charAt(0)              == '\r' ||
                                str.charAt(0)              == '\n')) str = str.substring(1);
    while (str.length() > 0 && (str.charAt(str.length()-1) == ' '  ||
                                str.charAt(str.length()-1) == '\t' ||
                                str.charAt(str.length()-1) == '\r' ||
                                str.charAt(str.length()-1) == '\n')) str = str.substring(0,str.length()-1);
    return str;
  }

  /**
   * Return the string boundaries based on the current graphic primitive.
   *
   *@param str string for boundaries
   *@param g2d graphics primitive
   *
   *@return rectangle representing the bounds of the string
   */
  public static Rectangle2D getStringBounds(String str, Graphics2D g2d) {
    return g2d.getFont().getStringBounds(str, g2d.getFontRenderContext());
  }


  /**
   * Take a date and produce the millisecond equivalent
   *
   *@param year  year as an integer
   *@param month month as an integer (has to be subtracted by 1 to be zero based)
   *@param day day of the month
   *@param hour hour of the day
   *@param minute minute of the hour
   *@param second second of the minute
   *
   *@return time in milliseconds since the epoch
   */
  public static long toMillis(int year, int month, int day, int hour, int minute, int second) {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.set(Calendar.MILLISECOND, 0);
    cal.set(year, month-1, day, hour, minute, second);
    return cal.getTimeInMillis();
  }

  /**
   * Take a date and produce the millisecond equivalent
   *
   *@param year  year as an integer
   *@param month month as an integer (has to be subtracted by 1 to be zero based)
   *@param day day of the month
   *@param hour hour of the day
   *@param minute minute of the hour
   *
   *@return time in milliseconds since the epoch
   */
  public static long toMillis(int year, int month, int day, int hour, int minute) {
    return toMillis(year, month, day, hour, minute, 0);
  }

  /**
   * Calculate the point where two segments intersect.
   *
   *@param l0 first segment
   *@param l1 second segment
   *
   *@return point of intersection or null if they do not cross.
   */
  public static Point2D.Double segmentIntersection(Line2D l0, Line2D l1) {
    if (l0.intersectsLine(l1)) {
      double x0  = (double) l0.getX1(), y0 = (double) l0.getY1(),
             x1  = (double) l1.getX1(), y1 = (double) l1.getY1();

      double dx0 = (double) (l0.getX2() - x0),
             dy0 = (double) (l0.getY2() - y0),
             dx1 = (double) (l1.getX2() - x1),
             dy1 = (double) (l1.getY2() - y1);

      double denom = dx1*dy0 - dx0*dy1;
      if (denom == 0.0f) return null;
      double t1 = (dx0*(y1-y0) + dy0*(x0-x1))/denom;

      double x_inter = x1 + dx1 * t1;
      double y_inter = y1 + dy1 * t1;

      return new Point2D.Double(x_inter, y_inter);
    } else return null;
  }

  /**
   * Arbitrarily try to compare two random objects...
   *
   *@param o1 object one
   *@param o2 object two
   *
   *@return minimum object (whatever that means...)
   */
  public static Object giveMin(Object o1, Object o2) {
    if        (o1 instanceof Comparable) {
      try {
        int result = ((Comparable) o1).compareTo(o2);
        if      (result <  0) return o1;
        else if (result == 0) return o1;
        else                  return o2;
      } catch (ClassCastException cce) { return o1; }
    } else if (o2 instanceof Comparable) {
      try {
        int result = ((Comparable) o2).compareTo(o1);
        if      (result <  0) return o2;
        else if (result == 0) return o2;
        else                  return o1;
      } catch (ClassCastException cce) { return o1; }
    } else return o1;
  }

  /**
   * Arbitrarily try to compare two random objects...
   *
   *@param o1 object one
   *@param o2 object two
   *
   *@return maximum object (whatever that means...)
   */
  public static Object giveMax(Object o1, Object o2) {
    if        (o1 instanceof Comparable) {
      try {
        int result = ((Comparable) o1).compareTo(o2);
        if      (result >  0) return o1;
        else if (result == 0) return o1;
        else                  return o2;
      } catch (ClassCastException cce) { return o1; }
    } else if (o2 instanceof Comparable) {
      try {
        int result = ((Comparable) o2).compareTo(o1);
        if      (result >  0) return o2;
        else if (result == 0) return o2;
        else                  return o1;
      } catch (ClassCastException cce) { return o1; }
    } else return o1;
  }

  /**
   * Determine if two sets of objects overlap.
   *
   *@param s0 set one
   *@param s1 set two
   *
   *@return true if sets share at least one element
   */
  public static boolean overlap(Set s0, Set s1) {
    if (s0 == null || s1 == null) return false;
    if (s0.size() > s1.size()) { Set tmp = s0; s0 = s1; s1 = tmp; }
    Iterator it = s0.iterator();
    while (it.hasNext()) if (s1.contains(it.next())) return true;
    return false;
  }

  /**
   * Calculate geospatial distance.  Based on code from http://geocoder.us/blog/2006/04/21/calculating-distances/
   *
   *@param wx0 first point longitude
   *@param wy0 first point latitude
   *@param wx1 second point longitude
   *@param wy1 second point latitude
   *
   *@return distance in miles
   */
  public static double calcMiles(double wx0, double wy0, double wx1, double wy1) {
    double dx = Math.abs(wx1 - wx0), dy = Math.abs(wy1 - wy0);
    double lat_rads = (double) Math.toRadians(((wy0+wy1)/2f));
    return (double) Math.sqrt( dx*Math.cos(lat_rads)*dx*Math.cos(lat_rads) + dy*dy)*69;
  }

  /**
   * Main routine for testing conversions and calculations.
   *
   *@param args command line arguments
   */
  public static void main(String args[]) {
    String timestamps[] = { "2014-05-06 12:32:23",
                            "2014-05-06 12:32:23.1",
                            "2014-05-06 12:32:23.34",
                            "2014-05-06 12:32:23.999",
                            "2014-05-06 12:32:23.99999",
                            "2014-05-06 12:32:23.99999233",
                            "20140506 1232Z",
                            "20140506 123223Z",
                            "20140506" };

    for (int i=0;i<timestamps.length;i++) System.out.println("Timestamp \"" + timestamps[i] + "\" => " + parseTimeStamp(timestamps[i]) + " => \"" + exactDate(parseTimeStamp(timestamps[i])) + "\"");



    String str = "Style%2b%252d%2bSolid";
    System.out.println("decFmURL(\"" + str + "\") = \"" + decFmURL(str) + "\"");

    str = "\u2343 this is a test... \u23a3";
    System.out.println("original = \"" + str + "\"");
    System.out.println("encoded  = \"" + encToURL(str) + "\"");
    System.out.println("restore  = \"" + decFmURL(encToURL(str)) + "\"");

    double dc_lon   = -(77f +  0f/60f + 58f/3600f),
          dc_lat   =  (38f + 54f/60f + 18f/3600f),
          balt_lon = -(76f + 36f/60f + 38f/3600f),
          balt_lat =  (39f + 18f/60f +  3f/3600f);
    System.out.println("Distance Between Baltimore and Washington DC is " +
      calcMiles(dc_lon, dc_lat, balt_lon, balt_lat));
    System.out.println("  Should be 35 miles (56km)");

    String tests[] = { "1.2.3.4", " 1.2.3.4",    "256.23.22.1", "127.0.0.1",      "0.0.0.0", "127.352.2.2",
                       "cnn.com", "www.cnn.com", " cnn.com",    "  abcnews.com ", "123.test.joe.smith.edu",
		       "some.gov.it", "whatever.gov.cn",
		       "some.different.endings", "another-different-ending.newdomain", "s-o-m-e-thing.tld",
                       "www.cnn.com", "www.abcnews.com", "www.abc-news.com", "www.-news.com", "www.a.b.c.d.com",
                       "www..cnn..com", "192.168.0.10", "123.100.com", "n.com", "1.com", "1.2.3.4", "1.2.3.4a", "www.news-.com",
                       "www.news-org.com", "-www.news.org", "www-.news.org", "www.cnn.com www.nbc.com",
		       "http://this.com/get.html", "2000-01-02T12:34:23", "2000-01-02 12:34:11", 
		       "2012-15-60 50:45:34.2344",
		       "2012-12-60 50:45:34.2344",
		       "2012-11-32 50:45:34.2344",
		       "2012-10-31 50:45:34.2344",
		       "2012-09-01 24:45:34.2344",
		       "2012-00-10 23:45:34.2344",
		       "2012-01-00 00:00:00",
		       "2012-11-10 00:45:34.2344",
		       "2012-01-01 50:65:34.2344",
		       "2012-05-02 50:45:74.2344",
                       "20140506 1232Z",
                       "20140506 123223Z",
                       "20140506",
		       "20150002",
		       "20151202",
		       "20141303",
		       "20149902",
		       "20141232",
		       "20131231",
		       "20131231 12Z",
		       "20131231 2300Z",
		       "20131231 2359Z",
		       "20131231 2360Z",
		       "20131231 0000Z",
		       "20131231 2400Z",
		       "20131231 3000Z",
		       "Mozilla/4.7 [en] (WinNT; U)",
		       "Mozilla/4.0 (compatible; MSIE 5.01; Windows NT)",
		       "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0; T312461; .NET CLR 1.1.4322)",
		       "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT 4.0) Opera 5.11 [en]",
		       "Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.0.2) Gecko/20030208 Netscape/7.02",
		       "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.6) Gecko/20040612 Firefox/0.8",
		       "Mozilla/5.0 (compatible; Konqueror/3.2; Linux) (KHTML, like Gecko)",
		       "Lynx/2.8.4rel.1 libwww-FM/2.14 SSL-MM/1.4.1 OpenSSL/0.9.6h",
		       "test@test.com", "some-where-over@the.rainbow.com", "not@email", "this.one.has.dots@dots.place",
                       "954a29a0eae9695d0e94e9f54677089f", "954a29aeae9695d0e94e9f54677089f",
                       "954A29a0Eae9695d0E94e9f54677089f", "954a29a0eaae9695d0e94e9f54677089f",
                       "954z29a0Eae9695d0E94e9f54677089f", "aaa.29a0eaae9695d0e94ef54677089f",
                       "954z29a0Eae9695d0E94e9f54677089f", "aaa.29a0eaae9695d0e94ef54677089fabab"
		       };

    for (int i=0;i<tests.length;i++) {
      System.out.print("\"" + tests[i] + "\"");
      if (isIPv4(tests[i]))      System.out.print(" IPv4 ");
      if (isDomain(tests[i]))    System.out.print(" DOMAIN ");
      if (isEmail(tests[i]))     System.out.print(" EMAIL ");
      if (isURL(tests[i]))       System.out.print(" URL ");
      if (isTimeStamp(tests[i])) System.out.print(" TIMESTAMP ");
      if (isUserAgent(tests[i])) System.out.print(" USERAGENT ");
      if (isMD5(tests[i]))       System.out.print(" MD5 ");
      System.out.println("");
    }

    // Test the tag normalizations
    testTagNormalization("simple|complex|difficult", "complex|simple|difficult");
    testTagNormalization("simple|complex|difficult", "complex|simple|difficult|difficult|simple");
    testTagNormalization("color=red|color=blue|color=green", "color=red,green|color=blue");
    testTagNormalization("color=gray|color=black", "color=black,gray");
    testTagNormalization("color=gray,black", "color=black,gray");
    testTagNormalization("simple|grandparent::parent::child", "grandparent::parent::child|simple");
    // This one should succeed... but fails... because algorithm doesn't look for longest ancestor chain...
    testTagNormalization("a::b::c|a::b", "a::b::c");
  }

  /**
   * Provide a simple mechanism to test tag normalization.
   *
   *@param s0 first tag
   *@param s1 second tag
   */
  protected static void testTagNormalization(String s0, String s1) {
    String norm_s0 = normalizeTag(s0), norm_s1 = normalizeTag(s1);
    String norm_x2 = normalizeTag(normalizeTag(normalizeTag(s0)));
    System.out.println("" + (norm_s0.equals(norm_s1)) + " - " + (norm_s0.equals(norm_x2)) + " ... \"" + norm_s0 + "\" vs \"" + norm_s1 + "\"");
  }

  /**
   * Write an integer to an output stream.
   *
   *@param out output stream
   *@param i   integer to write
   */
  public static void writeInt(OutputStream out, int i) throws IOException {
    out.write((i>>24) & 0x00ff);
    out.write((i>>16) & 0x00ff);
    out.write((i>> 8) & 0x00ff);
    out.write((i>> 0) & 0x00ff);
  }

  /**
   * Read an integer from an input stream.
   *
   *@param in input stream
   *
   *@return integer
   */
  public static int  readInt(InputStream in) throws IOException {
    int i =     ((in.read() & 0x00ff) << 24);
        i = i | ((in.read() & 0x00ff) << 16);
        i = i | ((in.read() & 0x00ff) <<  8);
        i = i | ((in.read() & 0x00ff) <<  0);
    return i;
  }

  /**
   * Write a long to an output stream.
   *
   *@param out output stream
   *@param l   long value to write
   */
  public static void writeLong(OutputStream out, long l) throws IOException {
    Utils.writeInt(out, (int) ((l>>32) & 0x00ffffffffL));
    Utils.writeInt(out, (int) ((l>> 0) & 0x00ffffffffL));
  }

  /**
   * Read a long from an input stream.
   *
   *@param in input stream
   *
   *@return long
   */
  public static long readLong(InputStream in) throws IOException {
    long top = Utils.readInt(in) & 0x00ffffffffL;
    long bot = Utils.readInt(in) & 0x00ffffffffL;
    return (top << 32) | (bot << 0);
  }

  /**
   * Write a string to an output stream.  Encode the length as an integer.
   *
   *@param out output stream
   *@param s   string to write
   */
  public static void writeString(OutputStream out, String s) throws IOException {
    Utils.writeInt(out, s.length());
    out.write(s.getBytes());
  }

  /**
   * Read a string from an input stream.
   *
   *@param in input stream
   *
   *@return string
   */
  public static String readString(InputStream in) throws IOException {
    int length = Utils.readInt(in);
    StringBuffer sb = new StringBuffer();
    for (int i=0;i<length;i++) {
      sb.append((char) (in.read() & 0x00ff));
    }
    return sb.toString();
  }

  /**
   * Write a double to an output stream.
   *
   *@param out output stream
   *@param f   double to write
   */
  public static void writeDouble(OutputStream out, double f) throws IOException {
    Utils.writeLong(out, Double.doubleToLongBits(f));
  }

  /**
   * Read a double from an input stream.
   *
   *@param in input stream
   *
   *@return double
   */
  public static double readDouble(InputStream in) throws IOException {
    return Double.longBitsToDouble(readLong(in));
  }

  /**
   * Encode a string into a URL encoded form.  This method does not strictly follow the URL
   * encoding specifications.  However, its most important function is to ensure that delimiters
   * and other special characters are not part of the string.
   *
   *@param str string to encode
   *
   *@return encoded string
   */
  public static String encToURL(String str) {
    if (str == null) { System.err.println("encToURL(null); -- returning empty string"); return ""; }
    StringBuffer sb = new StringBuffer();
    for (int i=0;i<str.length();i++) {
      char c = str.charAt(i);
      if ((c >= 'a' && c <= 'z') || 
          (c >= 'A' && c <= 'Z') || 
          (c >= '0' && c <= '9') ||
	  (c == '.')             || (c == '=')) sb.append(c);
      else if (c == ' ')                        sb.append('+');
      else {
        int j = (int) ((c >> 0) & 0x00ff);
        int k = (int) ((c >> 8) & 0x00ff);
        String s = Integer.toString(j,16);
        if (s.length() == 1) s = "0" + s;
        if (k != 0) {
          String t = Integer.toString(k,16);
	  if (t.length() == 1) t = "0" + t;
          s = "u" + t + s;
        }
        sb.append("%" + s);
      }
    }
    return sb.toString();
  }

  /** 
   * Decode a URL encoded string.  Not entirely to spec.
   *
   *@param str URL encoded string
   *
   *@return decoded string
   */
  public static String decFmURL(String str) {
    StringBuffer sb = new StringBuffer();
    int i = 0;
    while (i < str.length()) {
      if (str.charAt(i) == '%' && (i+1) < str.length()) {
	if (str.charAt(i+1) == 'u' && (i+5) < str.length()) {
	  String as_hex = "" + str.charAt(i+2) + str.charAt(i+3) + str.charAt(i+4) + str.charAt(i+5);
          char c = (char) (0x0ffff & Integer.parseInt(as_hex,16));
          sb.append(c);
          i += 6;
	} else if ((i+2) < str.length()) {
	  String as_hex = "" + str.charAt(i+1) + str.charAt(i+2);
          char c = (char) (0x00ff & Integer.parseInt(as_hex,16));
          sb.append(c);
          i += 3;
	} else {
          sb.append(str.charAt(i));
	  i++;
	}
      } else if (str.charAt(i) == '+') {
        sb.append(" ");
	i++;
      } else {
        sb.append(str.charAt(i));
	i++;
      }
    }
    return sb.toString();
  }

  /**
   * Convert an integer IP address to a string.  DUPLICATIVE...
   *
   *@param ip integer-based IPv4 address
   *
   *@return string representation
   */
  public static String ipAddrToString(int ip)    {
    return "" + ((ip>>24)&0x00ff) + "." +
                ((ip>>16)&0x00ff) + "." +
                ((ip>> 8)&0x00ff) + "." +
                ((ip>> 0)&0x00ff);
  }

  /**
   * Convert a string-based IPv4 address into an integer.
   *
   *@param ip_str IPv4 string
   *
   *@return integer representation
   */
  public static int    ipAddrToInt   (String ip_str) {
    StringTokenizer st = new StringTokenizer(ip_str,".");
    int ip = 0;
    for (int i=0;i<4;i++) {
      ip = ip << 8;
      ip = ip | (Integer.parseInt(st.nextToken()) & 0x00ff);
    }
    return ip;
  }

  /**
   * Return a (mostly) unique color for an IPv4 address.
   *
   *@param ip IPv4 address as an integer
   *
   *@return (mostly) unique color
   */
  public static Color ipColor(int ip) {
    int color = ((ip & 0xff000000)  >> 8) |  // Red & Green have good dynamic range
                ((ip & 0x00ff0000)  >> 8) |
                (((ip & 0x0000ff00) >> 8) ^  // Blue...  not so much...
                 ((ip & 0x000000ff)));
    color = color & 0x00ffffff; // clear the top octet
    color = color | 0x00202020; // Make sure it's somewhat light...
    return new Color(color);
  }

  /**
   * Return a unique color for TCP/UDP ports.
   *
   *@param port port
   *
   *@return unique color
   */
  public static Color portColor(int port) {
    if      (port == 80  || port == 443)                return Color.green;
    else if (port == 53)                                return Color.orange;
    else if (port == 20  || port == 21)                 return Color.red;
    else if (port == 22  || port == 23)                 return Color.green;
    else if (port == 25  || port == 109)                return Color.magenta;
    else if (port == 137 || port == 138 || port == 139) return Color.pink;
    else                                                return intColor(port);
  }

  /**
   * Return a unique color for a protocol.
   *
   *@param protocol protocol
   *
   *@return unique color
   */
  public static Color protocolColor(int protocol) {
    if      (protocol == 1)  return Color.green;
    else if (protocol == 6)  return Color.orange;
    else if (protocol == 17) return Color.yellow;
    else                     return Color.white;
  }
  /**
   * Calculate a hash for an integer.  From http://www.concentric.net/~ttwang/tech/inthash.htm
   *
   *@param a integer to hash
   *
   *@return hash result
   */
  public static int   robertJenkins32BitIntegerHash(int a) {
    a = (a+0x7ed55d16) + (a<<12);
    a = (a^0xc761c23c) ^ (a>>19);
    a = (a+0x165667b1) + (a<<5);
    a = (a+0xd3a2646c) ^ (a<<9);
    a = (a+0xfd7046c5) + (a<<3);
    a = (a^0xb55a4f09) ^ (a>>16);
    return a;
  }

  /**
   * Return a pastel (mostly) unique color for an integer.
   *
   *@param i integer to colorize
   *
   *@return pastel color
   */
  public static Color intColorPastel(int i) {
    int    hc   = robertJenkins32BitIntegerHash(i);
    int mask = 0x00;
    if ((hc & 0x80000000) != 0) mask |= 0x00800000; else if ((hc & 0x40000000) != 0) mask |= 0x00400000; else mask |= 0x00200000;
    if ((hc & 0x20000000) != 0) mask |= 0x00008000; else if ((hc & 0x10000000) != 0) mask |= 0x00004000; else mask |= 0x00002000;
    if ((hc & 0x08000000) != 0) mask |= 0x00000080; else if ((hc & 0x04000000) != 0) mask |= 0x00000040; else mask |= 0x00000020;
    return new Color((0x00ffffff & hc) | (mask));
  }

  /**
   * Return a color for an integer after hashing it.
   *
   *@param i integer to colorize
   *
   *@return colorized integer
   */
  public static Color intColor(int i) {
    int hc = robertJenkins32BitIntegerHash(i);
    return new Color((hc ^ (hc>>8)) | 0x00201000);
  }

  /**
   * Cache for converting strings to colors
   */
  static Map<String,Color> str_color_lu;
  static {
    str_color_lu = new HashMap<String,Color>();
    CacheManager.registerCache("String Color Lookup Cache", str_color_lu);
  }

  /**
   * Colorize a string.  Pay special attention to ranges.
   *
   *@param str string to colorize
   *
   *@return color for a string
   */
  public static Color strColor(String str) {
    if (str_color_lu.containsKey(str) == false) {
      String base_str = "abc"; if (str.indexOf(" ") >= 0) { StringTokenizer st = new StringTokenizer(str," "); if (st.hasMoreTokens()) base_str = st.nextToken(); }
      Color color = null; int var = (base_str.hashCode() & 0x7fffffff)%3;
      if      (str.endsWith("< 0"))         return doubleColor(     -1.0);
      else if (str.endsWith("= 0"))         return doubleColor(      0.0);
      else if (str.endsWith("= 1"))         return doubleColor(      1.0);
      else if (str.endsWith("\u2264 10"))   return doubleColor(     10.0);
      else if (str.endsWith("\u2264 100"))  return doubleColor(    100.0);
      else if (str.endsWith("\u2264 1K"))   return doubleColor(   1000.0);
      else if (str.endsWith("\u2264 10K"))  return doubleColor(  10000.0);
      else if (str.endsWith("\u2264 100K")) return doubleColor( 100000.0);
      else if (str.endsWith("> 100K"))      return doubleColor(1000000.0);
      else color = intColor(str.hashCode());
      str_color_lu.put(str,color);
    }
    return str_color_lu.get(str);
  }

  /**
   * Convert an integer into a log bins strings.
   *
   *@param int_str integer as a string
   *
   *@return log bins as a string
   */
  public static String integerLogBins(String int_str) {
    int as_int = Integer.parseInt(int_str);
    if      (as_int <  0)       return "< 0";
    else if (as_int == 0)       return "= 0";
    else if (as_int == 1)       return "= 1";
    else if (as_int <= 10)      return "\u2264 10";
    else if (as_int <= 100)     return "\u2264 100";
    else if (as_int <= 1000)    return "\u2264 1K";
    else if (as_int <= 10000)   return "\u2264 10K";
    else if (as_int <= 100000)  return "\u2264 100K";
    else                        return "> 100K";
  }

  /**
   * Brewer Color scale for nicer colors in sequential order.  See brewercolor2.org.
   */
  static BrewerColorScale dc_bcs = new BrewerColorScale(BrewerColorScale.BrewerType.SEQUENTIAL, 9, 0);

  /**
   * Return the brewer color at a specific index.
   *
   *@param i index of brewer color
   *
   *@return specified brewer color
   */
  public static Color brewerColor(int i) { return dc_bcs.atIndex(i); }

  /**
   * Return a color that is appropriate for logarithmic size bins.
   *
   *@param  d value
   *
   *@return color denoting logarithmic scaling
   */
  public static Color doubleColor(double d) {
      if      (d <  0.0)        return Color.darkGray;
      else if (d == 0.0)        return dc_bcs.atIndex(0);
      else if (d <  1.0)        return dc_bcs.atIndex(1);
      else if (d <= 1.0)        return dc_bcs.atIndex(2);
      else if (d <= 10.0)       return dc_bcs.atIndex(3);
      else if (d <= 100.0)      return dc_bcs.atIndex(4);
      else if (d <= 1000.0)     return dc_bcs.atIndex(5);
      else if (d <= 10000.0)    return dc_bcs.atIndex(6);
      else if (d <= 100000.0)   return dc_bcs.atIndex(7);
      else                      return dc_bcs.atIndex(8);
  }

  /**
   * Return a color to represent the degree of a linknode node.
   *
   *@param  deg degree of node
   *
   *@return sequential color derived empirically by degree thresholds
   */
  public static Color degreeColor(int deg) {
      if      (deg <= 1)  return Color.darkGray;
      else if (deg <  3)  return dc_bcs.atIndex(0);
      else if (deg <  4)  return dc_bcs.atIndex(1);
      else if (deg <  5)  return dc_bcs.atIndex(2);
      else if (deg <  6)  return dc_bcs.atIndex(3);
      else if (deg <  10) return dc_bcs.atIndex(4);
      else if (deg <  15) return dc_bcs.atIndex(5);
      else if (deg <  20) return dc_bcs.atIndex(6);
      else if (deg < 100) return dc_bcs.atIndex(7);
      else                return dc_bcs.atIndex(8);
  }

  /**
   * Return the text height of a string based on the current graphic primitive.
   *
   *@param g2d graphic primitive
   *@param str string
   *
   *@return height of string in pixels
   */
  public static int txtH(Graphics2D g2d, String str) {
    if (str == null) str = "1";
    return (int) Math.ceil(g2d.getFont().getStringBounds(str, g2d.getFontRenderContext()).getHeight());
  }

  /**
   * Return the text width of a string based on the current graphic primitive.
   *
   *@param g2d graphic primitive
   *@param str string
   *
   *@return width of string in pixels
   */
  public static int txtW(Graphics2D g2d, String str) {
    if (str == null) str = "1";
    return (int) Math.ceil(g2d.getFont().getStringBounds(str, g2d.getFontRenderContext()).getWidth());
  }

  /**
   * Fix a text string so that it fits within the specified width based on
   * the current graphics primitive.  This implementation assumes that all
   * characters are the same width.
   *
   *@param g2d      graphics primitive
   *@param str      string to adjust for width
   *@param target_w maximum width of the return string
   *
   *@return string shorted (if necessary) to match the target width
   */
  public static String fitTxt(Graphics2D g2d, String str, int target_w) {
    if (str == null || str.length() == 0) return "ft:NULL";
    int max_w = txtW(g2d,str);
    if (max_w <= target_w) return str;
    int avg_char_w = max_w/str.length();  if (avg_char_w < 1) avg_char_w = 1;
    int chars_left = target_w/avg_char_w; if (chars_left > str.length()) chars_left = str.length();
    if (chars_left <= 3) return "...";
    else                 return str.substring(0,chars_left) + "...";
  }

  /**
   * Calculate the average of an array.
   *
   *@param al list of values
   *
   *@return average of values
   */
  public static double calculateAverage(List<Double> al) {
    double as_array[] = new double[al.size()];
    for (int i=0;i<as_array.length;i++) as_array[i] = al.get(i);
    return calculateAverage(as_array);
  }

  /**
   * Calculate the average of an array.
   *
   *@param v list of values
   *
   *@return average of values
   */
  public static double calculateAverage(double v[]) {
    double sum = 0.0;
    for (int i=0;i<v.length;i++) sum += v[i];
    return sum / v.length;
  }

  /**
   * Calculate the standard deviation of an array.
   *
   *@param al list of values
   *
   *@return standard deviation of values
   */
  public static double calculateStandardDeviation(List<Double> al) {
    double as_array[] = new double[al.size()];
    for (int i=0;i<as_array.length;i++) as_array[i] = al.get(i);
    return calculateStandardDeviation(as_array);
  }

  /**
   * Calculate the standard deviation of an array.
   *
   *@param v list of values
   *
   *@return standard deviation of values
   */
  public static double calculateStandardDeviation(double v[]) {
    double avg = calculateAverage(v);
    double sum = 0.0;
    for (int i=0;i<v.length;i++) sum += (v[i] - avg) * (v[i] - avg);
    return Math.sqrt(sum/v.length);
  }

  /**
   * Calculate the standard deviation of an array.
   *
   *@param al  list of values
   *@param avg average of the array
   *
   *@return standard deviation of values
   */
  public static double calculateStandardDeviation(List<Integer> al, double avg) {
    double sum = 0.0;
    for (int i=0;i<al.size();i++) sum += (al.get(i) - avg) * (al.get(i) - avg);
    return Math.sqrt(sum/al.size());
  }

  /**
   * Exact date formatter.  The most critical aspect is to set the timezone to GMT.
   */
  static SimpleDateFormat exact_sdf; static { exact_sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'"); exact_sdf.setTimeZone(TimeZone.getTimeZone("GMT")); }

  /**
   * Return a string that describes the exact date of a timestamp down to the millisecond.
   *
   *@param ms timestamp in millis
   *
   *@return exact date to millisecond
   */
  public static String exactDate(long ms) { return exact_sdf.format(new Date(ms)); }

  /**
   * Date formatter for human readable consumption.  GMT!
   */
  static SimpleDateFormat sdf; static { sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss'Z'"); sdf.setTimeZone(TimeZone.getTimeZone("GMT")); }

  /**
   * Return a string that describes the date of a timestamp down to the second
   *
   *@param ms timestamp in millis
   *
   *@return date to second
   */
  public static String humanReadableDate(long ms) { return sdf.format(new Date(ms)); }

  /**
   * Double formatter down to one decimal point
   */
  static DecimalFormat    human_readable_format = new DecimalFormat("###,###,###.#");

  /**
   * Return a string that describes the double to one decimal point.  Useful for labels especially when
   * combined with m, k, g --- e.g., 1.2g.
   *
   *@param d double value
   *
   *@return string to one decimal point
   */
  public static String humanReadableDouble(double d) { return human_readable_format.format(d); }

  /**
   * Date formatter for making valid filenames out of date strings.
   */
  static SimpleDateFormat file_sdf; static { file_sdf = new SimpleDateFormat("yyyyMMdd_HHmmss"); file_sdf.setTimeZone(TimeZone.getTimeZone("GMT")); }

  /**
   * Return a file date string based on the current time that can be used as a filename.
   *
   *@return file string based on current time
   */
  public static String fileDateStr()        { return fileDateStr(System.currentTimeMillis()); }

  /**
   * Return a file date string based on the specified time that can be used as a filename.
   *
   *@param ms time to base filename on
   *
   *@return file string based on specified time
   */
  public static String fileDateStr(long ms) { return file_sdf.format(new Date(ms)); }

  /**
   * Date formatter for shorter date string - removes dashes, colons, etc.
   */
  static SimpleDateFormat day_sdf;   static { day_sdf = new SimpleDateFormat("yyyy-MM-dd"); day_sdf.setTimeZone(TimeZone.getTimeZone("GMT")); }

  /**
   * Return a string that represents a shortened version down to the minute.
   *
   *@param ms time to format
   *
   *@return shorted date string down to the minute
   */
  public static String dayDateStr(long ms) { return day_sdf.format(new Date(ms)); }

  /**

  /**
   * Date formatter for shorter date string - removes dashes, colons, etc.
   */
  static SimpleDateFormat short_sdf; static { short_sdf = new SimpleDateFormat("yyyyMMdd HHmm"); short_sdf.setTimeZone(TimeZone.getTimeZone("GMT")); }

  /**
   * Return a string that represents a shortened version down to the minute.
   *
   *@param ms time to format
   *
   *@return shorted date string down to the minute
   */
  public static String shortDateStr(long ms) { return short_sdf.format(new Date(ms)); }

  /**
   * Convert a duration (not a timestamp) to a human readable aggregate.
   *
   *@param ms duration in milliseconds
   *
   *@return duration string based on nearest aggregate
   */
  public static String humanReadableDuration(long ms) {
    if      (ms > 1000L * 60L * 60L * 24L) return "" + (ms/(1000L * 60L * 60L * 24L)) + " days";
    else if (ms > 1000L * 60L * 60L)       return "" + (ms/(1000L * 60L * 60L))       + " hours";
    else if (ms > 1000L * 60L)             return "" + (ms/(1000L * 60L))             + " mins";
    else if (ms > 1000L)                   return "" + (ms/1000L)                     + " secs";
    else                                   return "" + (ms)                           + " ms";
  }

  /**
   * Create a human readable number in the form of the closest 1000 suffix.  DUPLICATIVE.
   *
   *@param num number to format
   *
   *@return decimal value with appended 1000 suffix
   */
  public static String huRe(long num) { return humanReadable(num); }

  /**
   * Create a human readable number in the form of the closest 1000 suffix.
   *
   *@param num number to format
   *
   *@return decimal value with appended 1000 suffix
   */
  public static String humanReadable(long num) {
    if        (num >= 1000L * 1000L * 1000L * 1000L) { /* GB */ double d = num/(1000.0 * 1000 * 1000 * 1000); return "" + human_readable_format.format(d) + "T";
    } else if (num >= 1000L * 1000L * 1000L)         { /* GB */ double d = num/(1000.0 * 1000 * 1000); return "" + human_readable_format.format(d) + "G";
    } else if (num >= 1000L * 1000L)                 { /* MB */ double d = num/(1000.0 * 1000); return "" + human_readable_format.format(d) + "M";
    } else if (num >= 1000L)                         { /* KB */ double d = num/(1000.0); return "" + human_readable_format.format(d) + "K";
    } else                                           { return "" + num; }
  }

  /**
   * Convert an integer into a time of day.  In this case, the integer represents the number
   * of seconds since the beginning of the day.
   *
   *@param tod number of seconds since beginning of day
   *
   *@return hour, minute string for day
   */
  public static String humanReadableTimeOfDay(int tod) {
    int min  = tod%60;
    int hour = tod/60;
    String min_str  = "" + min;  if (min_str.length()  == 1) min_str  = "0" + min_str;
    String hour_str = "" + hour; if (hour_str.length() == 1) hour_str = "0" + hour_str;
    return hour_str + ":" + min_str;
  }

  /**
   * Prepend a string to the beginning of an array of strings.
   *
   *@param first string to prepend
   *@param rest  array of strings
   *
   *@return new array with first string prepended
   */
  public static String[] prepend(String first, String rest[]) {
    String strs[] = new String[rest.length + 1];
    strs[0] = first;
    System.arraycopy(rest, 0, strs, 1, rest.length);
    return strs;
  }

  /**
   * Prepend strings to the beginning of an array of strings.
   *
   *@param first array of strings to prepend
   *@param rest  array of strings
   *
   *@return new array with first strings prepended
   *
   */
  public static String[] prepend(String first[], String last[]) {
    String strs[] = new String[first.length + last.length];
    System.arraycopy(first, 0, strs, 0,            first.length);
    System.arraycopy(last,  0, strs, first.length, last.length);
    return strs;
  }

  /**
   * Append a string to the end of an array of strings.
   *
   *@param first  array of strings
   *@param last   string to append
   *
   *@return new array with last string appended
   */
  public static String[] append(String first[], String last) {
    String strs[] = new String[first.length + 1];
    System.arraycopy(first, 0, strs, 0, first.length);
    strs[strs.length-1] = last;
    return strs;
  }

  /**
   * Append an array of strings to the end of a {@link List}.
   *
   *@param al   list to append to
   *@param strs strings to append
   */
  public static void     append(List<String> al, String strs[]) {
    for (int i=0;i<strs.length;i++) al.add(strs[i]);
  }

  /**
   * Draw a string rotated by 90 degrees.  Useful for vertical lines.
   *
   *@param g2d graphics primitve
   *@param str string to draw
   *@param x   x-position of string
   *@param y   y-position of string
   */
  public static void drawRotatedString(Graphics2D g2d, String str, int x, int y) {
    AffineTransform orig_trans = g2d.getTransform();
    g2d.translate(x,y); g2d.rotate(-Math.PI/2); g2d.drawString(str,5,-2);
    g2d.setTransform(orig_trans);
  }

  /**
   * Draw a string rotated by 90 degrees.  Useful for vertical lines.  This version draws the
   * outline of a background around the characters first.  It requires nine times as long
   * but produces more readable strings on cluttered backgrounds.
   *
   *@param g2d graphics primitve
   *@param str string to draw
   *@param x   x-position of string
   *@param y   y-position of string
   *@param fg  foreground color
   *@param bg  background color
   */
  public static void drawRotatedString(Graphics2D g2d, String str, int x, int y, Color fg, Color bg) {
    g2d.setColor(bg);
    for (int dx=-1;dx<=1;dx++) for (int dy=-1;dy<=1;dy++) drawRotatedString(g2d, str, x-dx, y-dy);
    g2d.setColor(fg);
    drawRotatedString(g2d, str, x,    y);
  }

  /**
   * Dump a buffer of bytes to a readable format on stderr.  Used for debugging.
   *
   *@param buffer buffer to output
   *@param str    string to proceed the buffer by (useful for grepping debug)
   */
  public static void dumpBuffer(byte buffer[], String str) {
    System.err.println(""); System.err.println("=== %< === %< === %< === %< ==="); System.err.println("");
    boolean cr_needed = false;
    System.err.print(str);
    for (int i=0;i<buffer.length;i++) {
      if      (buffer[i] == '\n' && i < buffer.length - 1) { System.err.println("_n_"); System.err.print(str); cr_needed = true;  }
      else if (buffer[i] == '\n'                         ) { System.err.println("_n_");                        cr_needed = false; }
      else if (buffer[i] == '\r')                          { System.err.print("_r_");                          cr_needed = true;  }
      else {
        char c = (char) (buffer[i] & 0x00ff);
        if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
            (c == '<')  || (c == '>')  || (c == '/') || (c == '\\') || (c == ':') ||
            (c == ';')  || (c == '\"') || (c == ' ') || (c == '-')  || (c == '_') ||
            (c == '(')  || (c == ')')  || (c == '[') || (c == ']')  ||
            (c == '\'')) System.err.print(c);
        else System.err.print(".");
        cr_needed = true;
      }
    }
    if (cr_needed) System.err.println("");
    System.err.println(""); System.err.println("=== %< === %< === %< === %< ==="); System.err.println("");
  }

  /**
   * Determine if a string is all uppercase.
   *
   *@param str string to check
   *
   *@return true if none of the characters are lower case
   */
  public static boolean isAllUpper(String str) {
    for (int i=0;i<str.length();i++) if (str.charAt(i) >= 'a' && str.charAt(i) <= 'z') return false;
    return true;
  }

  /**
   * Determine if a string only has alpha characters.
   *
   *@param str string to check
   *
   *@return true if characters only fall between a...z and A...Z
   */
  public static boolean allAlpha(String str) {
    for (int i=0;i<str.length();i++) {
      char c = str.charAt(i);
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) { } else return false;
    }
    return true;
  }

  /**
   * Calendar used to parse timestamp strings.  Critical that it be set to GMT!
   */
  static Calendar gmtcal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

  /**
   * Attempt to parse a timestamp into the milliseconds since the epoch.  LIBRARY
   * Examples of times handled include the following (I think).
   *
   * 2010/01/01 12:34:00.11
   * 1/1/2005 5:32:00
   * 05/Apr/2012 17:53:26
   *
   *@param str string to convert
   *
   *@return milliseconds since the epoch
   */
  public static long parseTimeStamp(String str) {
    StringTokenizer st = new StringTokenizer(str, "-/\\:Tt .Zz");
    String tokens[] = new String[st.countTokens()]; for (int i=0;i<tokens.length;i++) tokens[i] = st.nextToken(); 
    // - convert to ints
    int    asint[]  = new int[tokens.length]; 
    for (int i=0;i<tokens.length;i++) {
      if (allAlpha(tokens[i])) {
        if      (tokens[i].toLowerCase().equals("jan")) asint[i] = 1;
        else if (tokens[i].toLowerCase().equals("feb")) asint[i] = 2;
        else if (tokens[i].toLowerCase().equals("mar")) asint[i] = 3;
        else if (tokens[i].toLowerCase().equals("apr")) asint[i] = 4;
        else if (tokens[i].toLowerCase().equals("may")) asint[i] = 5;
        else if (tokens[i].toLowerCase().equals("jun")) asint[i] = 6;
        else if (tokens[i].toLowerCase().equals("jul")) asint[i] = 7;
        else if (tokens[i].toLowerCase().equals("aug")) asint[i] = 8;
        else if (tokens[i].toLowerCase().equals("sep")) asint[i] = 9;
        else if (tokens[i].toLowerCase().equals("oct")) asint[i] = 10;
        else if (tokens[i].toLowerCase().equals("nov")) asint[i] = 11;
        else if (tokens[i].toLowerCase().equals("dec")) asint[i] = 12;
        else throw new RuntimeException("Cannot Convert \"" + tokens[i] + "\" To Month Integer...");
      } else asint[i] = Integer.parseInt(tokens[i]);
    }

    int yer = 0, mon = 0, day = 0, hor = 0, min = 0, sec = 0, ms = 0;
    // - Check for the smallest versions of timestamps
    if (tokens.length < 3) {
      if        (tokens.length == 1 && tokens[0].length() == 8)                            {
        yer = Integer.parseInt(tokens[0].substring(0,4)); mon = Integer.parseInt(tokens[0].substring(4,6)); day = Integer.parseInt(tokens[0].substring(6,8));
      } else if (tokens.length == 2 && tokens[0].length() == 8 && tokens[1].length() == 4) {
        yer = Integer.parseInt(tokens[0].substring(0,4)); mon = Integer.parseInt(tokens[0].substring(4,6)); day = Integer.parseInt(tokens[0].substring(6,8));
	hor = Integer.parseInt(tokens[1].substring(0,2)); min = Integer.parseInt(tokens[1].substring(2,4));
      } else if (tokens.length == 2 && tokens[0].length() == 8 && tokens[1].length() == 6) {
        yer = Integer.parseInt(tokens[0].substring(0,4)); mon = Integer.parseInt(tokens[0].substring(4,6)); day = Integer.parseInt(tokens[0].substring(6,8));
	hor = Integer.parseInt(tokens[1].substring(0,2)); min = Integer.parseInt(tokens[1].substring(2,4)); sec = Integer.parseInt(tokens[1].substring(4,6));
      } else throw new RuntimeException("parseTimeStamp(" + str + ") -- not enough tokens");
    } else {
      if        (tokens[0].length() == 4) { yer = asint[0];      mon = asint[1]; day = asint[2]; 
      } else if (tokens[2].length() == 4) {
        if (tokens[1].length() == 3)      { yer = asint[2];      mon = asint[1]; day = asint[0];
        } else                            { yer = asint[2];      mon = asint[0]; day = asint[1]; }
      } else                              { yer = asint[2]+2000; mon = asint[0]; day = asint[1]; }
      // Parse the hour minute seconds...
      if (tokens.length >= 4) hor = asint[3]; 
      if (tokens.length >= 5) min = asint[4]; 
      if (tokens.length >= 6) sec = asint[5];
      // Next the milliseconds
      if (tokens.length >= 7)  {
        if      (tokens[6].length() >  3) { tokens[6] = tokens[6].substring(0,3); asint[6] = Integer.parseInt(tokens[6]); }

        if      (tokens[6].length() == 1) ms = 100 * asint[6];
        else if (tokens[6].length() == 2) ms = 10  * asint[6];
        else if (tokens[6].length() == 3) ms = 1   * asint[6];
        else throw new RuntimeException("parseTimeStamp(" + str + ") -- milliseconds incorrect");
      }
    }
    gmtcal.set(yer,mon-1,day,hor,min,sec); gmtcal.set(Calendar.MILLISECOND, ms);
    return gmtcal.getTimeInMillis();
  }

  /**
   * Return a string indicating the format for tag separation and tag types.
   *
   *@return tag hints
   */
  public static String getTagToolTip() {
    return "type=value|parent::child::grandchild[::nextlevel]|simpletag|multivar=value2,value3";
  }

  /**
   * Separate a string into the distinct tags.
   *
   *@param str delimited tag characters
   *
   *@return tokenized tags
   */
  public static List<String> tokenizeTags(String str) {
    List<String> strs = new ArrayList<String>();
    StringTokenizer st = new StringTokenizer(str, "|");
    while (st.hasMoreTokens()) {
      String tag = st.nextToken();
      if (tag.indexOf("=") >= 0) {
        String type   = tag.substring(0,tag.indexOf("=")),
               values = tag.substring(tag.indexOf("=")+1,tag.length());
        if (values.indexOf(",") >= 0) {
          StringTokenizer st2 = new StringTokenizer(values, ",");
          while (st2.hasMoreTokens()) {
            String value = st2.nextToken();
            strs.add(decFmURL(type) + "=" + decFmURL(value));
          }
        } else strs.add(decFmURL(type) + "=" + decFmURL(values));
      } else strs.add(decFmURL(tag));
    }
    return strs;
  }

  /**
   * Normalize a tag.  Put it into a standard form so that re-ordering doesn't matter.
   * Won't actually handle duplicates in hierarchical tags...
   *
   *@param  tag_str original tag string
   *
   *@return normalized tag string
   */
  public static String normalizeTag(String tag_str) { 
    // Break the tag string into their individual tags
    List<String> tags = tokenizeTags(tag_str); 
    Set<String> tags_set = new HashSet<String>(); tags_set.addAll(tags); // Make unique...

    if (tags_set.size() > 1 && tags_set.contains(BundlesDT.NOTSET)) tags_set.remove(BundlesDT.NOTSET); // Make sure a "not set" isn't in the list if it has a valid tag

    // Sort them alphabetically
    tags.clear(); tags.addAll(tags_set);
    Collections.sort(tags);

    // Recombine them
    return combineTags(tags);
  }


  /**
   * Combine the collection of individual tags back into a string representation.
   *
   *@param tags collection of individual tags
   *
   *@return string representing all of the individual tags
   */
  public static String combineTags(Collection<String> tags) {
    StringBuffer sb = new StringBuffer(); Set<String> tags_set = new HashSet<String>(); Iterator<String> it = tags.iterator();
    while (it.hasNext()) {
      // Get the tag and uniquify
      String tag = it.next(); if (tags_set.contains(tag)) continue; else tags_set.add(tag);
      // Append the delimiter
      if (sb.length() > 0) sb.append("|");
      // Add the tag... pay special attention to type=value
      if        (tagIsTypeValue(tag))    { String strs[] = separateTypeValueTag(tag);
                                           sb.append(encToURL(strs[0]) + "=" + encToURL(strs[1]));
      } else if (tagIsHierarchical(tag)) { 
        while (tag.indexOf("::") >= 0) {
          String part = tag.substring(0,tag.indexOf("::"));
                 tag  = tag.substring(part.length()+2,tag.length());
          sb.append(encToURL(part) + "::");
        }
        sb.append(encToURL(tag));
      } else                             { sb.append(encToURL(tag)); }
    }
    // System.out.println("Norm(\"" + tag_str + "\") ==> \"" + sb.toString() + "\"");
    return sb.toString();
  }

  /**
   * Determine if a tag is simple -- i.e., not type-value or hierarchical.
   *
   *@param tag tag to examine
   *
   *@return true if tag is simple
   */
  public static boolean tagIsSimple       (String tag) { 
    for (int i=0;i<tag.length();i++) {
      char c = tag.charAt(i);
      if ((c >= 'a' && c <= 'z') ||
          (c >= 'A' && c <= 'Z') ||
          (c >= '0' && c <= '9') ||
          (c == '_') || (c == '-')) { } else { return false; }
    }
    return true;
  }

  /**
   * Determine if a tag is type-value, e.g., type=value.
   *
   *@param tag tag to examing
   *
   *@return true if tag is type-value
   */
  public static boolean tagIsTypeValue    (String tag) { 
    if (tag.indexOf("=") <= 0) return false; else {
      String type = tag.substring(0,tag.indexOf("=")),
             val  = tag.substring(tag.indexOf("=")+1,tag.length());
      if (tagIsSimple(type) == false) return false;
      for (int i=0;i<val.length();i++) {
        char c = val.charAt(i);
        if ((c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            (c >= '0' && c <= '9') ||
            (c == '_') || (c == '-') || (c == ':') || (c == '\\') || 
            (c == '/') || (c == '?') || (c == '&') || (c == '*')  ||
            (c == '!') || (c == '@') || (c == '#') || (c == '$')  ||
            (c == '^') || (c == '(') || (c == ')') || (c == '+')  ||
            (c == '[') || (c == ']') || (c == '{') || (c == '}')  ||
            (c == '<') || (c == '>') || (c == '.') || (c == ' ')) { } else { return false; }
      }
    }
    return true;
  }

  /**
   * Determine if a tag is hierarchical, e.g., grandparent::parent::child.
   *
   *@param tag tag to examine
   *
   *@return true if tag is hierarchical
   */
  public static boolean tagIsHierarchical (String tag) { 
    if (tag.indexOf("::") <= 0) return false; else {
      List<String> breaks = new ArrayList<String>();
      while (tag.indexOf("::") >= 0) { breaks.add(tag.substring(0,tag.indexOf("::"))); tag = tag.substring(tag.indexOf("::")+2,tag.length()); }
      breaks.add(tag);
      for (int i=0;i<breaks.size();i++) {
        if (breaks.get(i).equals("")) { return false; } else {
          if (verifyHierarchicalString(breaks.get(i)) == false) return false;
	}
      }
      return true;
    }
  }

  /**
   * Verify that a hierarchical substring is correct.
   *
   *@param str hierarchical substring
   *
   *@return true if it conforms the the legitimate characters
   */
  public static boolean verifyHierarchicalString(String str) {
    for (int i=0;i<str.length();i++) {
      char c = str.charAt(i);
      if ((c >= 'a' && c <= 'z') ||
          (c >= 'A' && c <= 'Z') ||
          (c >= '0' && c <= '9') ||
          (c == '_') || (c == '-') || (c == ':') || (c == '\\') || 
          (c == '/') || (c == '?') || (c == '&') || (c == '*')  ||
          (c == '!') || (c == '@') || (c == '#') || (c == '$')  ||
          (c == '^') || (c == '(') || (c == ')') || (c == '+')  ||
          (c == '[') || (c == ']') || (c == '{') || (c == '}')  ||
          (c == '<') || (c == '>') || (c == '.')) { } else { return false; }
    }
    return true;
  }

  /**
   * Separate a type-value tag into its two components.
   *
   *@param tag type-value tag to separate
   *
   *@return two element array, first element is type, second is value
   */
  public static String[] separateTypeValueTag(String tag) {
    String ar[] = new String[2];
    ar[0] = tag.substring(0,tag.indexOf("="));
    ar[1] = tag.substring(tag.indexOf("=")+1,tag.length());
    return ar;
  }

  /**
   * Decompose a hierarhical tag into its components.
   *
   *@param tag hierarhical tag
   *
   *@return array with each distinct component
   */
  public static String[] tagDecomposeHierarchical(String tag) {
    StringTokenizer st = new StringTokenizer(tag,":");
    String ar[] = new String[st.countTokens()];
    for (int i=0;i<ar.length;i++) if (i == 0) ar[i] = st.nextToken(); else ar[i] = ar[i-1] + "::" + st.nextToken();
    return ar;
  }

  /**
   * Determine if a string represents a java integer (not just an integer---e.g., a long
   * value is not a java integer because it cannot be parsed).
   *
   *@param str string to check
   *
   *@return true if the string can be parsed into a java integer
   */
  public static boolean isInteger(String str) {
    try { Integer.parseInt(str); return true; } catch (Throwable t) { return false; }
  }

  /**
   * Determine if a string appears to be an integer.
   *
   *@param str string to check
   *
   *@return true if all characters are between '0' and '9'
   */
  public static boolean appearsToBeInteger(String str) {
    if (str.length() == 0) return false;
    if (str.charAt(0) == '-' || str.charAt(0) == '+') str = str.substring(1,str.length());
    if (str.length() == 0) return false;
    for (int i=0;i<str.length();i++) {
      char c = str.charAt(i);
      if (c < '0' || c > '9') return false;
    }
    return true;
  }

  /**
   * Create the shape of a diamond at the specified coordinates with the specified dimensions.
   *
   *@param x0 x coordinate of center of shape
   *@param y0 y coordinate of center of shape
   *@param w0 width of shape
   *@param h0 height of shape
   *
   *@return diamond shape
   */
  public static GeneralPath createDiamond(float x0, float y0, float w0, float h0) {
    GeneralPath gp = new GeneralPath();
    gp.moveTo((x0 + w0/2), (y0));
    gp.lineTo((x0 + w0  ), (y0 + h0/2));
    gp.lineTo((x0 + w0/2), (y0 + h0));
    gp.lineTo((x0),        (y0 + h0/2));
    gp.closePath();
    return gp;
  }

  /**
   * Create the shape of a triangle at the specified coordinates with the specified dimensions.
   *
   *@param x0 x coordinate of center of shape
   *@param y0 y coordinate of center of shape
   *@param w0 width of shape
   *@param h0 height of shape
   *
   *@return triangle shape
   */
  public static GeneralPath createTriangle(float x0, float y0, float w0, float h0) {
    GeneralPath gp = new GeneralPath();
    gp.moveTo((x0 + w0/2), (y0));
    gp.lineTo((x0 + w0  ), (y0 + h0));
    gp.lineTo((x0),        (y0 + h0));
    gp.closePath();
    return gp;
  }

  /**
   * Create the shape of a star at the specified coordinates with the specified dimensions.
   *
   *@param x0 x coordinate of center of shape
   *@param y0 y coordinate of center of shape
   *@param w0 width of shape
   *@param h0 height of shape
   *
   *@return star shape
   */
  public static GeneralPath createStar(float x0, float y0, float w0, float h0) {
    GeneralPath gp = new GeneralPath();
    double angle_inc = 2*Math.PI/10.0; double r0 = w0/3, r1 = w0, angle = -angle_inc;
    double cx = x0 + w0/2, cy = y0 + h0/2;
    gp.moveTo(cx + r0 * Math.cos(angle), cy + r0 * Math.sin(angle));
    for (int i=0;i<5;i++) {
      angle += angle_inc; gp.lineTo(cx + r1 * Math.cos(angle), cy + r1 * Math.sin(angle));
      angle += angle_inc; gp.lineTo(cx + r0 * Math.cos(angle), cy + r0 * Math.sin(angle));
    }
    gp.closePath();
    return gp;
  }

  /**
   * Create the shape of a hour glass at the specified coordinates with the specified dimensions.
   *
   *@param x0 x coordinate of center of shape
   *@param y0 y coordinate of center of shape
   *@param w0 width of shape
   *@param h0 height of shape
   *
   *@return hour glass shape
   */
  public static GeneralPath createHourglass(float x0, float y0, float w0, float h0) {
    GeneralPath gp = new GeneralPath(); x0 = x0 + w0/2; y0 = y0 + h0/2;
    gp.moveTo(x0 - w0/6, y0); gp.lineTo(x0 - w0/2, y0 - h0/2); gp.lineTo(x0 + w0/2, y0 - h0/2);
    gp.lineTo(x0 + w0/6, y0); gp.lineTo(x0 + w0/2, y0 + h0/2); gp.lineTo(x0 - w0/2, y0 + h0/2);
    gp.closePath(); return gp;
  }

  /**
   * Create the shape of a spiky thing (?) at the specified coordinates with the specified dimensions.
   * Probably a failed attempt to create something else... DELETABLE
   *
   *@param x0 x coordinate of center of shape
   *@param y0 y coordinate of center of shape
   *@param w0 width of shape
   *@param h0 height of shape
   *
   *@return spiky thing shape
   */
  public static GeneralPath createSpikeyThing(float x0, float y0, float w0, float h0) {
    GeneralPath gp = new GeneralPath();
    gp.moveTo(x0,y0); 
    
    float xu = w0/3, yu=h0/3;
    gp.lineTo(x0-xu,y0-0.5f*yu); gp.lineTo(x0-4*xu,y0); gp.lineTo(x0-xu,y0+0.5f*yu); gp.lineTo(x0,y0);
    gp.lineTo(x0+xu,y0-0.5f*yu); gp.lineTo(x0+4*xu,y0); gp.lineTo(x0+xu,y0+0.5f*yu); gp.lineTo(x0,y0);

    gp.lineTo(x0+0.5f*xu,y0-yu); gp.lineTo(x0,y0-4*yu); gp.lineTo(x0-0.5f*xu,y0-yu); gp.lineTo(x0,y0);
    gp.lineTo(x0+0.5f*xu,y0+yu); gp.lineTo(x0,y0+4*yu); gp.lineTo(x0-0.5f*xu,y0+yu); gp.lineTo(x0,y0);

    gp.closePath();

    gp.moveTo(x0-1*xu,y0-1*yu);  gp.lineTo(x0-3*xu,y0-1*yu); gp.lineTo(x0-1*xu,y0-3*yu); gp.closePath();
    gp.moveTo(x0+1*xu,y0+1*yu);  gp.lineTo(x0+3*xu,y0+1*yu); gp.lineTo(x0+1*xu,y0+3*yu); gp.closePath();
    gp.moveTo(x0+1*xu,y0-1*yu);  gp.lineTo(x0+3*xu,y0-1*yu); gp.lineTo(x0+1*xu,y0-3*yu); gp.closePath();
    gp.moveTo(x0-1*xu,y0+1*yu);  gp.lineTo(x0-3*xu,y0+1*yu); gp.lineTo(x0-1*xu,y0+3*yu); gp.closePath();

    return gp;
  }

  /**
   * Create the shape of a clover at the specified coordinates with the specified dimensions.  Believe
   * this was an initial attempt at creating a cloud.
   *
   *@param x0 x coordinate of center of shape
   *@param y0 y coordinate of center of shape
   *@param w0 width of shape
   *@param h0 height of shape
   *
   *@return clover shape
   */
  public static GeneralPath createClover(float x0, float y0, float w0, float h0) {
    GeneralPath gp = new GeneralPath();
    float xoff = w0/10, yoff = h0/10;
    gp.moveTo(x0 - w0 + xoff, y0 - yoff); gp.lineTo(x0 - w0, y0 - yoff); gp.curveTo(x0 - w0, y0 - h0, x0 - w0, y0 - h0, x0 - xoff, y0 - h0); gp.lineTo(x0 - xoff, y0 - h0 + yoff); gp.closePath();
    gp.moveTo(x0 + w0 - xoff, y0 - yoff); gp.lineTo(x0 + w0, y0 - yoff); gp.curveTo(x0 + w0, y0 - h0, x0 + w0, y0 - h0, x0 + xoff, y0 - h0); gp.lineTo(x0 + xoff, y0 - h0 + yoff); gp.closePath();
    gp.moveTo(x0 - w0 + xoff, y0 + yoff); gp.lineTo(x0 - w0, y0 + yoff); gp.curveTo(x0 - w0, y0 + h0, x0 - w0, y0 + h0, x0 - xoff, y0 + h0); gp.lineTo(x0 - xoff, y0 + h0 - yoff); gp.closePath();
    gp.moveTo(x0 + w0 - xoff, y0 + yoff); gp.lineTo(x0 + w0, y0 + yoff); gp.curveTo(x0 + w0, y0 + h0, x0 + w0, y0 + h0, x0 + xoff, y0 + h0); gp.lineTo(x0 + xoff, y0 + h0 - yoff); gp.closePath();

    xoff = w0/3; yoff = h0/3;
    gp.moveTo(x0 - xoff, y0); gp.lineTo(x0, y0 - yoff); gp.lineTo(x0 + xoff, y0); gp.lineTo(x0, y0 + yoff); gp.closePath();
    return gp;
  }

  /**
   * Create a cloud (third attempt)...
   *
   *@param x0 x coordinate of center of shape
   *@param y0 y coordinate of center of shape
   *@param w0 width of shape
   *@param h0 height of shape
   *
   *@return clover shape
   */
  public static GeneralPath createCloverExperiment(float x0, float y0, float w0, float h0) {
    GeneralPath gp = new GeneralPath();
    return gp;
  }

  /**
   * Create the shape of a happy face
   *
   *@param x0 x coordinate of center of shape
   *@param y0 y coordinate of center of shape
   *@param w0 width of shape
   *@param h0 height of shape
   *
   *@return happy face shape
   */
  public static Shape createHappyFace(float x0, float y0, float w0, float h0) {
    return new Ellipse2D.Float(x0-w0/2,y0-h0/2,w0,h0);
  }

  /**
   * Create the shape of a document.
   *
   *@param x0 x coordinate of center of shape
   *@param y0 y coordinate of center of shape
   *@param w0 width of shape
   *@param h0 height of shape
   *
   *@return envelope shape
   */
  public static Shape createDocumentShape(float x0, float y0, float w0, float h0) {
    GeneralPath gp = new GeneralPath();
    float ten_perc = w0/6;
    gp.moveTo(x0,y0); gp.lineTo(x0+w0-ten_perc,y0); gp.lineTo(x0+w0,y0+ten_perc); gp.lineTo(x0+w0,y0+h0); gp.lineTo(x0,y0+h0); gp.closePath();
    gp.moveTo(x0+w0-ten_perc,y0); gp.lineTo(x0+w0-ten_perc,y0+ten_perc); gp.lineTo(x0+w0,y0+ten_perc);
    for (int i=1;i<4;i++) { gp.moveTo(x0+ten_perc,y0+h0*i/4); gp.lineTo(x0+w0-ten_perc,y0+h0*i/4); }
    return gp;
  }

  /**
   * Create the shape of a envelope.
   *
   *@param x0 x coordinate of center of shape
   *@param y0 y coordinate of center of shape
   *@param w0 width of shape
   *@param h0 height of shape
   *
   *@return envelope shape
   */
  public static Shape createEnvelope(float x0, float y0, float w0, float h0) {
    GeneralPath gp = new GeneralPath();
    x0 = x0 + w0/2;
    // y0 = y0 + h0/2;
    // gp.moveTo(x0-w0,y0-w0); gp.lineTo(x0+w0,y0+w0); gp.closePath();
    // gp.moveTo(x0+w0,y0-w0); gp.lineTo(x0-w0,y0+w0); gp.closePath();

    gp.moveTo(x0-w0,   y0);
    gp.lineTo(x0+w0,   y0);
    gp.lineTo(x0,      y0+h0/3);
    gp.closePath();

    gp.moveTo(x0-w0,   y0+h0/8);
    gp.lineTo(x0,      y0+h0/3+h0/8);
    gp.lineTo(x0+w0,   y0+h0/8);
    gp.lineTo(x0+w0,   y0+h0);
    gp.lineTo(x0-w0,   y0+h0);
    gp.closePath();

    return gp;
  }

  /**
   * Create the shape of a person (simple).
   *
   *@param x0 x coordinate of center of shape
   *@param y0 y coordinate of center of shape
   *@param w0 width of shape
   *@param h0 height of shape
   *
   *@return simple person shape
   */
  public static Shape createPerson(float x0, float y0, float w0, float h0) {
    Area area = new Area();
    float head_x = x0+w0/2, head_y = y0,      head_w = 3*w0/4, head_h = 3*h0/4;
    float body_x = x0+w0/2, body_y = y0 + h0, body_w = w0,     body_h = h0;

    area.add(new Area(new Ellipse2D.Float  (head_x-head_w/2,head_y-head_h/2,head_w,head_h)));
    area.add(new Area(new Ellipse2D.Float  (body_x-body_w/2,body_y-body_w/2,body_w,body_h)));
    area.add(new Area(new Rectangle2D.Float(body_x-body_w/2,body_y,         body_w,body_h)));
    // return new Ellipse2D.Float(x0-w0/2,y0-h0/2,w0,h0);
    return area;
  }

  /**
   * Create the shape of a silo...
   *
   *@param x0 x coordinate of center of shape
   *@param y0 y coordinate of center of shape
   *@param w0 width of shape
   *@param h0 height of shape
   *
   *@return silo shape
   */
  public static Shape createSilo(float x0, float y0, float w0, float h0) {
    Area area = new Area();
    float head_x = x0+w0/2, head_y = y0 + h0/4, head_w = w0/2, head_h = h0/2;
    float body_x = x0+w0/2, body_y = y0 + h0/2, body_w = w0,   body_h = h0;

    area.add(new Area(new Ellipse2D.Float  (head_x-head_w/2,head_y-head_h/2,head_w,head_h)));
    area.add(new Area(new Ellipse2D.Float  (body_x-body_w/2,body_y-body_w/2,body_w,body_h)));
    area.add(new Area(new Rectangle2D.Float(body_x-body_w/2,body_y,         body_w,body_h)));
    // return new Ellipse2D.Float(x0-w0/2,y0-h0/2,w0,h0);
    return area;
  }

  /**
   * Create the shape of a plus at the specified coordinates with the specified dimensions.
   *
   *@param x0 x coordinate of center of shape
   *@param y0 y coordinate of center of shape
   *@param w0 width of shape
   *@param h0 height of shape
   *
   *@return plus shape
   */
  public static GeneralPath createPlus(float x0, float y0, float w0, float h0) {
    GeneralPath gp = new GeneralPath();
    float dx = w0/2, dy = h0/2, cx = x0 + dx, cy = y0 + dy, ix = dx/8, iy = dy/8;
    gp.moveTo(cx - ix, y0); 
    gp.lineTo(cx + ix, y0);      gp.lineTo(cx + ix, cy - iy); gp.lineTo(x0 + w0, cy - iy); gp.lineTo(x0 + w0, cy + iy);
    gp.lineTo(cx + ix, cy + iy); gp.lineTo(cx + ix, y0 + h0); gp.lineTo(cx - ix, y0 + h0); gp.lineTo(cx - ix, cy + iy);
    gp.lineTo(x0,      cy + iy); gp.lineTo(x0,      cy - iy); gp.lineTo(cx - ix, cy - iy);
    gp.closePath();
    return gp;
  }

  /**
   * Create the shape of an x at the specified coordinates with the specified dimensions.
   *
   *@param x x coordinate of center of shape
   *@param y y coordinate of center of shape
   *@param w width of shape
   *@param h height of shape
   *
   *@return plus shape
   */
  public static GeneralPath createX(float x, float y, float w, float h) {
    GeneralPath gp = new GeneralPath();
    float dx = w/2, dy = h/2, cx = x + dx, cy = y + dy, ix = dx/2, iy = dy/2;
    float ulx = cx - dx, uly = cy - dy, urx = cx + dx, ury = cy - dy, lrx = cx + dx, lry = cy + dy, llx = cx - dx, lly = cy + dy;
    gp.moveTo(ulx-ix,uly-iy); gp.lineTo(lrx+ix,lry-iy); gp.lineTo(lrx+ix,lry+iy); gp.lineTo(ulx-ix,uly+iy); gp.closePath();
    gp.moveTo(llx-ix,lly-iy); gp.lineTo(urx+ix,ury-iy); gp.lineTo(urx+ix,ury+iy); gp.lineTo(llx-ix,lly+iy); gp.closePath();
    return gp;
  }

  /**
   * From a set of IP addresses, calculate the CIDR string that would include all of the
   * IP addresses.  Useful for labeling aggregates in the link-node display.
   *
   *@param ips set of IP addresses
   *
   *@return CIDR string
   */
  public static String calculateCIDR(Set<String> ips) {
    int ip_count = 0, non_ip_count = 0; long min_ip, max_ip; min_ip = max_ip = -1L;
    Iterator<String> it = ips.iterator();
    while (it.hasNext()) {
      String str =it.next();
      if (str.indexOf(BundlesDT.DELIM) >= 0) { StringTokenizer st = new StringTokenizer(str,BundlesDT.DELIM); while (st.hasMoreTokens()) str = st.nextToken(); } // Take the last token if there are delims...
      if (isIPv4(str)) { ip_count++;
        long ip = 0x00FFFFFFFFL & strToIPInt(str);
	if (min_ip == -1L) min_ip = max_ip = ip;
	else {
          if (min_ip > ip) min_ip = ip;
	  if (max_ip < ip) max_ip = ip;
	}
      } else non_ip_count++;
    }

    StringBuffer sb = new StringBuffer();
    if (ip_count > 0) {
      if (min_ip == max_ip) sb.append(intToIPString((int) min_ip));
      else {
        int min_oct3 = (int) ((min_ip & 0x00ff000000) >> 24), min_oct2 = (int) ((min_ip & 0x0000ff0000) >> 16),
            min_oct1 = (int) ((min_ip & 0x000000ff00) >>  8), min_oct0 = (int) ((min_ip & 0x00000000ff) >>  0),
            max_oct3 = (int) ((max_ip & 0x00ff000000) >> 24), max_oct2 = (int) ((max_ip & 0x0000ff0000) >> 16),
            max_oct1 = (int) ((max_ip & 0x000000ff00) >>  8), max_oct0 = (int) ((max_ip & 0x00000000ff) >>  0);
        if (min_oct3 == max_oct3) {
	  sb.append(min_oct3 + ".");

	  if      (min_oct2 == max_oct2) sb.append(min_oct2 + "."); 
	  else if (min_oct2 >  max_oct2) sb.append("[" + max_oct2 + "-" + min_oct2 + "].");
	  else                           sb.append("[" + min_oct2 + "-" + max_oct2 + "].");

	  if      (min_oct1 == max_oct1) sb.append(min_oct1 + "."); 
	  else if (min_oct1 >  max_oct1) sb.append("[" + max_oct1 + "-" + min_oct1 + "].");
	  else                           sb.append("[" + min_oct1 + "-" + max_oct1 + "].");

	  if      (min_oct0 == max_oct0) sb.append(min_oct0);
	  else if (min_oct0 >  max_oct0) sb.append("[" + max_oct0 + "-" + min_oct0 + "]");
	  else                           sb.append("[" + min_oct0 + "-" + max_oct0 + "]");

	} else sb.append(intToIPString((int) min_ip) + " - " + intToIPString((int) max_ip));
      }
    }
    if (non_ip_count > 0) {
      if (ip_count > 0) sb.append(" + ");
      sb.append("" + non_ip_count + " Elements");
    }
    return sb.toString();
  }

  /**
   * Create a UUID from a string.  Since a java hashcode has significantly less bits than
   * a UUID, do some funky things to get more bits out of the string.  Should be rewritten
   * to use a hash function.  LIBRARY
   *
   *@param str string to uniquify
   *
   *@return unique UUID for string
   */
  public static UUID getUUID(String str) {
    long   hc0 = str.hashCode()                     & 0x00ffffffffL, hc1 = ("R"    + str + "T").hashCode() & 0x00ffffffffL,
	   hc2 = ("Rr.." + str + "Tt..").hashCode() & 0x00ffffffffL, hc3 = ("RrR#" + str + "TtT#").hashCode() & 0x00ffffffffL;
    long   lo  = (hc0 << 32) | hc1,
           hi  = (hc2 << 32) | hc3;
    // Make it conform to Version 4 Variant (Random) -- see http://en.wikipedia.org/wiki/Universally_unique_identifier
    hi = hi & 0xffffffffffff0fffL; hi = hi | 0x0000000000004000L;
    lo = lo & 0x0fffffffffffffffL; lo = lo | 0xa000000000000000L;
    return new UUID(hi,lo);
  }

  /**
   * Create a UUID from two long values representing the most and least significant
   * bits in the UUID.
   *
   *@param hi most significant bits
   *@param lo least significant bits
   *
   *@return UUID with the hi and lo components
   */
  public static UUID getUUID(long hi, long lo) {
    // Make it conform to Version 4 Variant (Random) -- see http://en.wikipedia.org/wiki/Universally_unique_identifier
    hi = hi & 0xffffffffffff0fffL; hi = hi | 0x0000000000004000L;
    lo = lo & 0x0fffffffffffffffL; lo = lo | 0xa000000000000000L;
    return new UUID(hi,lo);
  }

  /**
   * From a set of strings, produce a sorted array of those strings
   *
   *@param set set of strings
   *
   *@return sorted array of set strings
   */
  public static String[] asSortedArray(Set<String> set) {
    if (set == null || set.size() == 0) return new String[0];
    String strs[] = new String[set.size()];
    Iterator<String> it = set.iterator(); for (int i=0;i<strs.length;i++) strs[i] = it.next();
    Arrays.sort(strs);
    return strs;
  }

  /**
   * Return an array of strings as a delimited set of strings.  Should probably
   * check to ensure delimiter does not occur within the elements of the array...
   * DELETABLE
   *
   *@param array array of strings to place into delimited string
   *@param delim delimiter
   *
   *@return delimited string with the array elements
   */
  public static String asDelimitedString(String array[], String delim) {
    Arrays.sort(array);
    StringBuffer sb = new StringBuffer();
    if (array.length > 0) sb.append(array[0]);
    for (int i=1;i<array.length;i++) sb.append(delim + array[i]);
    return sb.toString();
  }

  /**
   * Return a UUID as a hex string.
   *
   *@param uuid UUID to convert
   *
   *@return hex string representing UUID
   */
  public static String hexUUID(UUID uuid) { 
    String str = hexLong(uuid.getMostSignificantBits()) +
                 hexLong(uuid.getLeastSignificantBits()); 
    return str;
  }

  /**
   * Return a long value as a hex string.
   *
   *@param l long to convert
   *
   *@return hex string representing the long value
   */
  public static String hexLong(long l) {
    String hex = Long.toHexString(l);
    while (hex.length() < 16) hex = "0" + hex;
    return hex;
  }

  /**
   * Return a byte array as a hex string.
   *
   *@param bytes bytes to convert into a hex string
   *
   *@return hex string representing the bytes
   */
  public static String hexBytes(byte bytes[]) {
    StringBuffer sb = new StringBuffer();
    for (int i=0;i<bytes.length;i++) {
      String str = Integer.toString(bytes[i] & 0x00ff, 16);
      if (str.length() == 1) str = "0" + str;
      sb.append(str);
    }
    return sb.toString();
  }

  /**
   * Make a string SQL safe by replacing delimiters with appropriate escapes.
   *
   *@param str string to make safe
   *
   *@return SQL safe string
   */
  public static String sqlString(String str) {
    StringBuffer sb = new StringBuffer();
    for (int i=0;i<str.length();i++) {
      char c = str.charAt(i);
      if      (c == '\'') sb.append("''"); else sb.append(c);
    }
    return sb.toString();
  }

  /**
   * Enlarge a shapes bounds by the specified amount.  Useful for making a more grabble
   * feature in the GUI.
   *
   *@param shape shape to enlarge
   *@param by    additional size in pixels
   *
   *@return rectangle representing enlarged bounds
   */
  public static Rectangle2D enlargeBounds(Shape shape, int by) {
    Rectangle2D bounds = shape.getBounds();
    return new Rectangle2D.Double(bounds.getMinX() - by, bounds.getMinY() - by, bounds.getWidth() + 2*by, bounds.getHeight() + 2*by);
  }

  /**
   * Tokenize a line by the specified delimiter.  Ensures that blank fields are inserted back into the tokens.
   *
   *@param line   line to tokenize
   *@param delims set of delimiters to use
   *
   *@return tokens (including blanks)
   */
  public static String[] tokenize(String line, String delims) {
     List<String> al = new ArrayList<String>();
     StringTokenizer st = new StringTokenizer(line, delims, true); 
     while (st.hasMoreTokens()) al.add(st.nextToken());
     if (al.size() > 0) {
       if (al.get(0).length()           == 1 && delims.indexOf(al.get(0))           >= 0) al.add(0,"");
       if (al.get(al.size()-1).length() == 1 && delims.indexOf(al.get(al.size()-1)) >= 0) al.add("");
       int i = 1;
       while (i < al.size()-1) {
         if ((al.get(i).  length() == 1 && delims.indexOf(al.get(i))   >= 0) &&
             (al.get(i+1).length() == 1 && delims.indexOf(al.get(i+1)) >= 0)) al.add(i+1,"");
         i++; } } else return new String[0];
    String tokens[] = new String[al.size()/2 + 1];
    for (int i=0;i<tokens.length;i++) tokens[i] = al.get(2*i);
    return tokens;
  }

  /**
   * Convert a single string into an array.  Used as a convenience method.
   *
   *@param str string to place into the array
   *
   *@return one element string array
   */
  public static String[] toArray(String str) { String arr[] = new String[1]; arr[0] = str; return arr; }

  /**
   * Wrapper for a jlist to return a list of strings.  Needed to compile 
   * on Java 1.6 due to incompatibilities with the GUI library.
   *
   *@param jlist jlist to get selected values from
   *
   *@return list of selected values
   */
  public static java.util.List<String> jListGetValuesWrapper(JList jlist) {
    List<String> al = new ArrayList<String>();
    Object objects[] = jlist.getSelectedValues();
    for (int i=0;i<objects.length;i++) al.add(objects[i].toString());
    return al;
  }

  /**
   * Return the number of days in the specified month.  Month string should look like "2013-06".
   *
   *@param month_str year-month string
   *
   *@return number of days in specified month
   */
  public static int daysInMonth(String month_str) {
    StringTokenizer st = new StringTokenizer(month_str, "-");
    int year  = Integer.parseInt(st.nextToken()),
        month = Integer.parseInt(st.nextToken());
    switch (month) {
      case 1:  return 31;
      case 2:  if (leapYear(year)) return 29; else return 28;
      case 3:  return 31;
      case 4:  return 30;
      case 5:  return 31;
      case 6:  return 30;
      case 7:  return 31;
      case 8:  return 31;
      case 9:  return 30;
      case 10: return 31;
      case 11: return 30;
      case 12: return 31;
      default: throw new RuntimeException("daysInMonth(\"" + month_str + "\") Failed");
    }
  }

  /**
   * Return if the specified year is a leapyear.  Uses algorithm from the following page:
   * http://en.wikipedia.org/wiki/Leap_year
   *
   *@param year year
   *
   *@return true if the specified year is a leap year
   */
  public static boolean leapYear(int year) {
    if      ((year%400) == 0) return true;
    else if ((year%100) == 0) return false;
    else if ((year%4)   == 0) return true;
    else                      return false;
  }

  /**
   * Return the number of days in the specified year.  Year string should look like "2013".
   *
   *@param year_str year string
   *
   *@return number of days in specific year
   */
  public static int daysInYear(String year_str) {
    int year = Integer.parseInt(year_str);
    return leapYear(year) ? 366 : 365;
  }

  /**
   * Shorten a string to the desired length
   *
   *@param str string to shorten
   *@param len length to shorten to
   *
   *@return shortened string
   */
  public static String shorten(String str, int len) {
    if (str.length() > len) return str.substring(0,len) + "..."; else return str;
  }

  /** 
   * Shape strings for node shapes
   */
  public final static String SQUARE_STR = "Square",
		      CIRCLE_STR     = "Circle",
		      TRIANGLE_STR   = "Triangle",
		      DIAMOND_STR    = "Diamond",
		      STAR_STR       = "Star",
		      HOUR_STR       = "Hourglass",
                      PERSON_STR     = "Person",
                      SILO_STR       = "Silo",
                      ENVELOPE_STR   = "Envelope",
		      HAPPY_STR      = "Happy",
		      PLUS_STR       = "Plus",
                      X_STR          = "X",
                      DOCUMENT_STR   = "Document";

  /**
   * Array of shape strings for rendering nodes
   */
  public final static String SHAPE_STRS[] = { 
    SQUARE_STR,
    CIRCLE_STR,
    TRIANGLE_STR,
    DIAMOND_STR,
    STAR_STR,
    HOUR_STR,
    PERSON_STR,
    SILO_STR,
    ENVELOPE_STR,
    HAPPY_STR,
    PLUS_STR,
    X_STR,
    DOCUMENT_STR };

  /**
   * Enumeration for shape strings for rendering nodes
   */
  public enum Symbol { SQUARE, CIRCLE, TRIANGLE, DIAMOND, STAR, HOUR, PLUS, X, HAPPY, ENVELOPE, PERSON, SILO, DOCUMENT };

  /**
   * Map to convert shape string to shape enumeration
   */
  static Map<String,Symbol> str_to_sym;
  static {
    str_to_sym = new HashMap<String,Symbol>();
    str_to_sym.put(SQUARE_STR,   Symbol.SQUARE);
    str_to_sym.put(CIRCLE_STR,   Symbol.CIRCLE);
    str_to_sym.put(TRIANGLE_STR, Symbol.TRIANGLE);
    str_to_sym.put(DIAMOND_STR,  Symbol.DIAMOND);
    str_to_sym.put(STAR_STR,     Symbol.STAR);
    str_to_sym.put(HOUR_STR,     Symbol.HOUR);
    str_to_sym.put(PLUS_STR,     Symbol.PLUS);
    str_to_sym.put(X_STR,        Symbol.X);
    str_to_sym.put(ENVELOPE_STR, Symbol.ENVELOPE);
    str_to_sym.put(HAPPY_STR,    Symbol.HAPPY);
    str_to_sym.put(PERSON_STR,   Symbol.PERSON);
    str_to_sym.put(SILO_STR,     Symbol.SILO);
    str_to_sym.put(DOCUMENT_STR, Symbol.DOCUMENT);
  }

  /**
   * Method to convert string to enumeration for the rendered node shapes.
   *
   *@param  str shape string
   *
   *@return shape enumeration
   */
  public static Symbol parseSymbol(String str) { if (str_to_sym.containsKey(str)) return str_to_sym.get(str); else return Symbol.CIRCLE; }

      /**
       * Shape helper function to convert an enumeration into a geometrical shape.
       *
       *@param symbol enumerated shape
       *@param x0     screen x coordinate
       *@param y0     screen y coordinate
       *@param size   size of shape
       *
       *@return shape matching the parameter criteria
       */
      public static Shape shape(Utils.Symbol symbol, float x0, float y0, float size) {
        switch (symbol) {
          case SQUARE:   return new Rectangle2D.Float(x0,y0,size,size);
          case DIAMOND:  return Utils.createDiamond(x0,y0,size,size);
          case TRIANGLE: return Utils.createTriangle(x0,y0,size,size);
          case STAR:     return Utils.createStar(x0,y0,size,size);
	  case HOUR:     return Utils.createHourglass(x0,y0,size,size);
	  case PLUS:     return Utils.createPlus(x0,y0,size,size);
	  case X:        return Utils.createX(x0,y0,size,size);
	  case ENVELOPE: return Utils.createEnvelope(x0,y0,size,size);
	  case HAPPY:    return Utils.createHappyFace(x0,y0,size,size);
	  case PERSON:   return Utils.createPerson(x0,y0,size,size);
	  case SILO:     return Utils.createSilo(x0,y0,size,size);
          case DOCUMENT: return Utils.createDocumentShape(x0,y0,size,size);
	  case CIRCLE:   default:  return new Ellipse2D.Float(x0,y0,size,size);
        }
      }


  /**
   *
   */
  public static BufferedImage render(double ds[][], ColorScale cs) {
    BufferedImage bi = new BufferedImage(ds[0].length, ds.length, BufferedImage.TYPE_INT_RGB);

    // Find the mins and maes
    double min, max; min = max = ds[0][0];
    for (int y=0;y<ds.length;y++) for (int x=0;x<ds[y].length;x++) {
      if (ds[y][x] > max) max = ds[y][x];
      if (ds[y][x] < min) min = ds[y][x];
    }

    // Make sure it's not zero...
    if (min == max) max = min+1;

    // Set the pixel colors
    for (int y=0;y<ds.length;y++) for (int x=0;x<ds[y].length;x++) {
      double v = (ds[y][x] - min)/(max - min);
      Color  color = cs.at((float) v);
      bi.setRGB(x, y, color.getRGB());
    }

    return bi;
  }

  /**
   * Convert integer array to long array.  Useful for converting the application values (integers)
   * into those needed by the xy-scatterplot.  Longs are used by xy to encompass timestamps and the
   * primary/alternate y axis fields.
   *
   *@param  ints integers to convert
   *
   *@return long array
   */
  public static long[] toLongs(int ints[]) { long longs[] = new long[ints.length]; for (int i=0;i<longs.length;i++) longs[i] = (long) ints[i]; return longs; }

  /**
   * Make a string safe for a filename.
   *
   *@param str string to make safe
   *
   *@return string that is safe to use as a filename
   */
  public static String makeSafeForFilename(String str) {
    StringBuffer sb = new StringBuffer(); if (str == null || str.length() == 0) sb.append("_");
    for (int i=0;i<str.length();i++) {
      char c = str.charAt(i);
      if ((c >= 'a' && c <= 'z') ||
          (c >= 'A' && c <= 'Z') ||
	  (c >= '0' && c <= '9')) sb.append(c); else sb.append("_");
    }
    return sb.toString();
  }

  /**
   * Make a string escaped for excel-like output.
   *
   *@param str string to add doublequotes to if it contains either a comma, doublequote, and/or newline
   *
   *@return string that is properly escaped
   */
  public static String doubleQuotify(String str) {
    if (str == null) return null; // hopefully upstream function will fix...
    if (str.indexOf(",")  >= 0 || str.indexOf("\"") >= 0 || str.indexOf("\n") >= 0) {
      StringBuffer sb = new StringBuffer(); sb.append("\"");
      for (int i=0;i<str.length();i++) { if (str.charAt(i) == '\"') sb.append("\"\""); else sb.append(str.charAt(i)); }
      sb.append("\""); return sb.toString();
    } else return str;
  }
}

