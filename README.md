Pre-requirement:

1) Compile Java and generate Jar file
Jdk version of mcls-all-2.8.3.jar is 1.7

Compile via commands
undergoing

2) Dispatch and Copy the jar file into HBase lib forlder in all computers
If no-password ssh is set up, you can use scp to trasfer any file.
eg. I am going to transfer JAR.jar file to the folder, /HOME/UBUNTU/HBASE/LIB, at the computer named HOSTNAME via a user called USERNAME.
scp mcls-all-2.8.3.jar USERNAME@HOSTNAME:/HOME/UBUNTU/

3) Modified HBase configuration file in order to setup OEWC Coprocessor
In hbase-site.xml, add
<property>
    <name>hbase.coprocessor.region.classes</name>
    <value>coprocessor.services.OewcEndpoint</value>
</property>

If there is any question aoubt the configuration, referring to Appendix A in HBase: The Definitive Guide

4) Prepare a known environment map for robots. The format of the map is JPEG.
Such as map_8590.jpg, simmap.jpg, or bigmap.jpg

5) Pre-define the split keys of energy grid map for HBase
These split key should be stored in paras.sh, a shell script exporting enviromental variables.
If a new map is build, a tool, , can be used to find the split keys.
eg. 

6) Upload map.jpg into HDFS
Google "hadoop command line interface" or refer to Hadoop: The definitive guide
hadoop help
hadoop hdfs upload ~/map.jpg /user/eeuser/map.jpg
hadoop hdfs -ls /user/eeuser

Off-line:

1) Export environment variables
source environment.sh
or
./environment

2) Create a Table
eg. execute the shell script, createTable.sh, to create a table, named TABLENAME.
./createTable TABLENAME

3) Execute pre-caching
hadoop jar image2hbaseo.jar image2hbase.File2Hbase -t map -i hdfs:///user/eeuser/map.jpg -r 40 -o 36

On-line:

1) Export environment variables
source environment.sh
or
./environment

2) Execute the localization program
hadoop jar JAR.jar IMCLROE.Main -i file:///home/ubuntu/map.jpg ...


Touble Shooting:
1) JRE Version Set Up
If there is some errors like that "Unsupported major.minor version 51.0", the vesion of Java Runtime Environment (JRE) and the JAR.jar file is different.
51.0 represents that the JAR.jar file is built by Java SE 7, 52.0 by Java SE 8, 50.0 by Java SE 6.
The current JRE version can be found out by the command.
$java -version

