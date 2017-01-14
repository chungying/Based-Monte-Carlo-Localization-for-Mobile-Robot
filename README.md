Pre-requirement:

1) Compile Java and generate Jar file
 jdk version of mcls-all-2.8.3.jar is 1.7
 Commands
 undergoing

2) Dispatch and Copy the jar file into all computers
If no password ssh is set up, you can use scp to trasfer any file.
eg. I am going to transfer JAR.jar file to the folder, /HOME/UBUNTU, at the computer named HOSTNAME via a user called USERNAME.
scp JAR.jar USERNAME@HOSTNAME:/HOME/UBUNTU

3) Modified HBase configuration file in order to setup OEWC Coprocessor
In hbase-site.xml, add
<property>
    <name>hbase.coprocessor.region.classes</name>
    <value>coprocessor.services.OewcEndpoint</value>
</property>

If there is any question aoubt the configuration, referring to Appendix A in HBase: The Definitive Guide

4) Prepare a known environment map for robots. The format of the map is JPEG.
Such as map.jpg

5) Pre-define the split keys of energy grid map for HBase
These split key are stored in paras.sh, a shell script exporting enviromental variables.

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
