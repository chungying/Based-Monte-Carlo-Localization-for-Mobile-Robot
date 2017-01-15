Pre-requirement:

1) Compile Java and generate Jar file  
mcls-all-6.jar is built by Java SE 6  
mcls-all-7.jar is built by Java SE 7  
mcls-all-8.jar is built by Java SE 8  

Compile via commands  
undergoing...  

2) Dispatch and Copy the jar file into HBase lib forlder in all computers   
If no-password ssh is set up, you can use scp to trasfer any file.  
eg. I am going to transfer JAR.jar file to the folder, /HOME/UBUNTU/HBASE/LIB, at the computer named HOSTNAME via a user called USERNAME.  
$scp mcls-all-7.jar USERNAME@HOSTNAME:/HOME/UBUNTU/  
  
3) Modified HBase configuration file in order to setup OEWC Coprocessor  
In hbase-site.xml, add  
<property>  
    <name>hbase.coprocessor.region.classes</name>  
    <value>coprocessor.services.OewcEndpoint</value>  
</property>  
Noting that there are some sturcture symbols for xml files so it would be better read this document in raw data.  
  
If there is any question aoubt the configuration, referring to Appendix A in HBase: The Definitive Guide  
  
4) Prepare a known environment map for robots. The format of the map is JPEG.  
Such as map_8590.jpg, simmap.jpg, or bigmap.jpg  
map_8590.jpg has 85 width and 90 height.  
simmap.jpg is 630x651.  
bigmap.jpg is 1220x1260.  
  
5) Pre-define the split keys of energy grid map for HBase  
If there is a new map, following command can be used to find the split keys.  
Noting that this command should be read in raw data.
$SPLITKEYS=`hadoop jar mcls-all-7.jar util.metrics.Sampler -i file:///Users/ubuntu/jpg/simmap.jpg -o 18 --splitNumber 4`  
-i is the map image which will be use for localization.  
-o is the resolution of orientation.  
--splitNumber is the number of region nodes.  
   
6) Upload map.jpg into HDFS  
If you need more detailed instructions, access to the website ( http://hadoop.apache.org/docs/r2.7.2/hadoop-project-dist/hadoop-common/FileSystemShell.html ) or refer to the book, Hadoop: The definitive guide.  
The simple instructions could be obtained by typing the following command.  
$hadoop help  
  
To upload image file to the cloud types  
$hadoop fs -copyFromLocal map.jpg hdfs:///user/ubuntu/map.jpg  
$hadoop fs -ls hdfs:///user/ubuntu  
  
Off-line:  
  
1) Export environment variables  
Before executing mcls-all-7.jar, the environment variables must be set up correctly, especially for $HADOOP_CLASSPATH.  
Following command could automatically add routes of these jar files into $HADOOP_CLASSPATH.  
$source environment.sh ~/mcls-all-7.jar ~/jcommander-1.36-SNAPSHOT.jar  
   
Following for checking   
$echo $HADOOP_CLASSPATH   
   
2) Create a Table  
Before execute pre-caching, a HBase table has to be created.  
Please use these two commands to create a table, named TABLENAME.  
This command is as same as 5) in previous section, please read it in raw data.   
$export SPLITKEYS=`hadoop jar mcls-all-7.jar util.metrics.Sampler -i file:///home/ubuntu/simmap.jpg -o 18 --splitNumber 4`  
   
$./createTable TABLENAME   
TABLENAME is the name in HBase system.  
You could name it in any string format.  
For instance, I would call the map, simmap.jpg, with 18 orientation resolution split into 4 parts as "simmap-18-4".  
  
3) Execute pre-caching  
$hadoop jar mcls-all-7.jar mapreduce.file2hfile.File2Hfile -t simmap-18-4 -i hdfs:///user/ubuntu/simmap.jpg -m 40 -o 18   
-t is TABLENAME eg. simmap-18-4    
-i is image map stored in HDFS   
-m is the number of mappers   
-o is the resolution of orientation   
   
On-line:  
  
1) Export environment variables  
Before executing mcls-all-7.jar, the environment variables must be set up correctly, especially for $HADOOP_CLASSPATH.  
Following command could automatically add routes of these jar files into $HADOOP_CLASSPATH.  
$source environment.sh ~/mcls-all-7.jar ~/jcommander-1.36-SNAPSHOT.jar  
  
Following for checking  
$echo $HADOOP_CLASSPATH  
  
2) Execute the localization program  
$hadoop jar mcls-all-7.jar imclroe.Main -i file:///home/ubuntu/simmap.jpg -o 18 -cl -t simmap.18.4 -rx 250 -ry 170 --delta 0.0001 --xi 0.05   
-i is the route of map image  
-o is the resolution of the map  
If there is "-cl", it means this execution will use the cloud compute  
-t is TABLENAME  
-rx and -ry are not necessary, because the initial coordinate can be tuned via User Interface.
--delta is the parameter for the size of Similar Energy Region. This can be tuned via User Interface.  
--xi is the sensitivity for detecting kidnapping situation  
  
  
Touble Shooting:  
  
1) JRE Version Set Up  
If there is some errors like that "Unsupported major.minor version 51.0", the vesions of Java Runtime Environment (JRE) and the JAR.jar file are different.  
51.0 represents that the JAR.jar file is built by Java SE 7, 52.0 by Java SE 8, 50.0 by Java SE 6.  
The current JRE version can be found out by the command.  
$java -version  
  
2) When Pre-caching failed  
Hadoop Distribution File System (HDFS) is similar to the file system of linux. Both of them require permission.
Please check the folder in HDFS for storing HFiles, output of pre-caching, whether the permission is allowed to be read written by others.  
The command checks file status. Please find the folder where storing HFiles of HBase.  
$hadoop fs -ls hdfs:///user/hbase/...  

The command change the ownership.  
$hadoop fs -chown [-R] [OWNER][:[GROUP]] URI [URI]  
-R is recursive, which means, if URI is a folder, all of the sub-folders and files in URI will be changed.  
OWNER is the user who is going to own URI.  
GROUP is the group who is going to own URI.  
URI is the routes, such as hdfs:///user/hbase. If there are many URIs, use blank separate them.  

The command change the permission. Please refer to the instruction website.  
$hadoop fs -chmod ...   
