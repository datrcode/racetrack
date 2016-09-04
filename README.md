### How do I get set up? ###

#### For Linux ####

**You'll need openjdk, ant, and git installed on the linux system**

```
sudo apt-get install openjdk-7-jdk
sudo apt-get install ant
sudo apt-get install git
```

Determine where you want to place the RACETrack code base and change to that directory

```
cd ~
```

Clone the repository and change into that directory

```
git clone https://username@bitbucket.org/dcode/racetrack
cd racetrack
```
Get the external dependencies (place these into ~/racetrack/src/main/resources/lib)


* Jama Jar file from http://math.nist.gov/javanumerics/jama/ (Jama-1.0.3.jar)
* UASparser from https://github.com/chetan/UASparser/downloads (uasparser-0.4.0.jar)
* MaxMind geo library from http://dev.maxmind.com/geoip - click on geoip tab, then "GeoIP Legacy downloadable databases", then next to Java the "Maven" link and the the "jar" link under Download (should have a file named "geoip-api-1.2.14.jar")

Create an environment file (you'll replace $user with your username -- I named the file env.sh in the ~/racetrack directory)

```
export RT=/home/$user/racetrack
export RTLIB=$RT/src/main/resources/lib
export CLASSPATH=$RT/target/classes:$RTLIB/Jama-1.0.3.jar:$RTLIB/uasparser-0.4.0.jar:$RTLIB/geoip-api-1.2.14.jar:.
```

Create a directory that doesn't get created by default (not sure why):

```
mkdir src/test/java (from the ~/racetrack directory)
```

Set your environment to the previously created environment file:

```
. env.sh
```

Build the application via ant

```
ant
```

Run the application (the following command allocates 4Gigs to the system)

```
java -Xmx4g racetrack.gui.RT
```

### How do I format files? ###

#### Reserved Header Field Names ####

The following header fields are reserved and should only be used as the application expects:

- timestamp:  timestamp of the record
- beg:  same as timestamp
- timestamp_end:  ending timestamp of the record
- end:  same as timestamp_end
- tags:  special field for encoding multiple values that occur infrequently or have multiple values associated with the record

Note:  Please do not use pipes or other special characters in the header fields.

#### Scalar versus Categorical Columns ####

Scalar fields (integers that should be added arithmetically) are defined by the capitalization of the all of the characters in the header field.  Scalar elements are added together by addition operations.

Categorical fields are indicated by a header name that includes at least one lowercase letter.  Categorical fields are added together by set operations.

#### URL Encoding ####

Individual fields that contain special characters (i.e., commas and percent symbols) should encoded using URL encoding.  In this scheme, any special characters are represented by a percent symbol followed by the two-character hex value for that special character.

For unicode characters, please use a percent symbol followed by a 'u' followed by the four digit hex value for the unicode character.

#### How should timestamps be formatted? ####

Timestamps should be formatted as follows:

- 2014-12-16 14:50:01.452
- 2014-12-16 14:50:01
- 2014-12-16 14
- 2014-12-16

It is preferable to have all time stamps in GMT.

#### What is the tag field format? ####

A single tag entry can be composed of multiple tags.  Each tag should be separated by a pipe.  There are three types of supported tags:

- Simple:  a string without a pipe, equal symbol, or double colons
- Type-value:  color=blue
- Hierarchical:  highest::medium::lowest

### How does... ###

#### How does the expression field work? ####

Expressions allow records to be sorted by a text filter.  Example filters include:

- sip in {10.0.0.1,10.0.0.2} // keeps records where sip is either of the values in the list
- dpt = 80 // keeps records with dpt equal to 80
- dpt = 80 or dpt = 443 // keeps records with dpt equal to 80 or 443

### Known Issues and Limitations ###

#### Color and Logarithmic Scaling ####

Applying color to any of the logarithmic bars presents a false quantitative perception.  For example, applying color to a temporal chart that has log scaling.  The problem occurs because each pixel of the bar does not represent the same increase in values.  For example, the first 10 pixels may quantify 1 to 10 while the next 10 pixels encode 10 to 100.

Coloring is applied based on a percentage -- if fifty percent correspond to the blue and the other fifty percent correspond to red, then half of the bar will be red and half will be blue.  No inferences should be made quantitative based on the colors.

Affects:  Temporal and Histogram Components

#### Link Node Selection Expansion When Filtered ####

When the 'e' key is used within the link node component, the original underlying link node graph is used to calculate the expansion.  This may cause confusion when the view is filtered -- for example, the link that the expansion occurs across may not be rendered due to filtering.

Affects:  Link Node Component

#### Degree Selection With Self Referential Nodes / Filtered Views ####

Degree selection within the link node component also uses the underlying link node graph for the calculation.  The may cause confusion when the view is filtered -- for example, the link that shows the actual degree may not be rendered due to filtering.

Affects:  Link Node Component

#### Transform Misalignment After Window Resizing ####

The link node component's transform may become misaligned after the window is resized (or when the label side panel is closed).  The most recognizable symptom is that nodes moved by the mouse are placed in a different location.  To correct, re-fit the graph with a single middle mouse click.

Affects:  Link Node Component

#### Temporal Aliasing ####

Aliasing occurs within the temporal component.  For example, depending on the mapping and the width of the actual window, different spikes (or the lack of a spike) may occur as events are placed into two close time slices.

Affects:  Temporal Component

#### Stats Overlay - Different Formats ####

If you have different versions of netflow (or netflow-like) data, it's unlikely that the stats overlay functionality will work correctly.  Currently, the code only keeps a single mapping for the fields to what are considered 'canonical fields'.