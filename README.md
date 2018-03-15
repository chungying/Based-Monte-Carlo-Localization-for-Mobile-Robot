# Pre-requirement:


## 1) Compile Java and generate Jar file  
```
$gradle build
$ls build/libs/mcls-all-1.00.00.jar
```

## 2) Dispatch jar files into HBase lib forlder in all hosts   
If no-password ssh is set up, you can use scp to trasfer any file.  
eg. I am going to transfer JAR.jar file to the folder, /HBASE/lib, at the computer named HOSTNAME via a user called USERNAME.  
```
$scp build/libs/mcls-all-1.00.00.jar USERNAME@HOSTNAME:/tmp
$ssh USERNAME@HOSTNAME 'sudo cp /home/USERNAME/mcls-all-1.00.00.jar /HBASE/lib/'
$ssh USERNAME@HOSTNAME 'sudo wget -nv https://raw.githubusercontent.com/chungying/MCL-Java-Simulator-with-Hadoop/master/jcommander-1.36-SNAPSHOT.jar -O /HBASE/lib/jcommander-1.36-SNAPSHOT.jar'
```
  
## 3) Modified HBase configuration file in order to setup OEWC Coprocessor  
In hbase-site.xml, add 
```
<property>  
    <name>hbase.coprocessor.region.classes</name>  
    <value>endpoint.services.OewcEndpoint2</value>  
</property>  
```
  
If there is any question aoubt the configuration, referring to Appendix A in HBase: The Definitive Guide.  
Or you could refer to XML folder for my previous system configuration.  
  
## 4) Prepare a known environment map for robots. The format of the map is JPEG.  
Such as map_8590.jpg, simmap.jpg, bigmap.jpg, or pgm files  
map_8590.jpg has 85 width and 90 height.  
simmap.jpg is 630x651.  
bigmap.jpg is 1220x1260.  
  
## 5) Pre-define the split keys of energy grid map for HBase  
If there is a new map, following command can be used to find the split keys.  
Noting that this command should be read in raw data.  
```
$export SPLITKEYS=`hadoop jar mcls-all-1.00.00.jar util.Sampler -i file:///Users/USERNAME/MAPIMAGE -o 18 --splitNumber 4`  
```
-i is the map image which will be use for localization.  
-o is the resolution of orientation.  
--splitNumber is the number of region nodes.  
   
## 6) Upload MAPIMAGE into HDFS 
If you need more detailed instructions, access to [the website](http://hadoop.apache.org/docs/r2.7.2/hadoop-project-dist/hadoop-common/FileSystemShell.html) or refer to the book, Hadoop: The definitive guide.  
The simple instructions could be obtained by typing the following command.  
$hadoop help  
  
To upload image file to the cloud types  
```
$hadoop fs -copyFromLocal MAPIMAGE hdfs:///user/USERNAME/
$hadoop fs -ls hdfs:///user/USERNAME
```
If the folder doesn't exist, create it with superuser permission, eg. ```hdfs```.
```
$sudo -u hdfs hdfs dfs -mkdir /user/USERNAME
$sudo -u hdfs hdfs dfs -chown -R USERNAME:hadoop /user/USERNAME
```
  
# Off-line:  
  
## 1) Export environment variables  
$HADOOP_CLASSPATH is required for ```hadoop jar JARFILE CLASS [GENERICOPTIONS] [ARGS...]``` command.
Following could add paths of jar files into $HADOOP_CLASSPATH.  
```
$source environment.sh build/libs/mcls-all-1.00.00.jar jcommander-1.36-SNAPSHOT.jar
```
For checking the variable, type
```$echo $HADOOP_CLASSPATH```
or
```$hadoop classpath```
If permanent setting is needed, update /etc/profile on all hosts manully.
```
[USERNAME@HOSTNAME ~]$echo 'export HADOOP_CLASSPATH=${HADOOP_CLASSPATH}:/HBASE/lib:/HBASE/conf' | sudo tee -a /HADOOP/etc/hadoop/hadoop-env.sh
```
/HBASE is absolute HBase folder, and /HADOOP is absolute Hadoop folder.

If HDP is used, HADOOP_CLASSPATH can be updated via Ambari in ```hadoop-env.sh``` template. Ambari will update for all hosts.
```
# added to the HADOOP_CLASSPATH
if [ -d "/usr/hdp/current/hbase-client" ]; then
  if [ -d "/etc/hbase/conf/" ]; then
    # When using versioned RPMs, the hbase-client will be a symlink to the current folder of tez in HDP.
    export HADOOP_CLASSPATH=${HADOOP_CLASSPATH}:/usr/hdp/current/hbase-client/lib/*:/etc/hbase/conf
  fi
fi
```
   
## 2) Create a Table  
Before execute pre-caching, a HBase table has to be created.  
Please use these two commands to create a table named TABLENAME.  
This command is as same as 5) in previous section, please read it in raw data.
```
$export SPLITKEYS=`hadoop jar mcls-all-7.jar util.Sampler -i file:///home/USERNAME/MAPIMAGE -o 18 --splitNumber 4`  
$./createTable TABLENAME
```
You could name the table in any string format.  
For instance, I would call the map, simmap.jpg, with 18 orientation resolution split into 4 parts as "simmap-18-4".  
For checking if a table is created, find the table from the results of following command.
```
$echo list|hbase shell
```
  
## 3) Execute pre-caching  
```
$hadoop jar mcls-all-1.00.00.jar mapreduce.file2hfile.File2Hfile -t simmap-18-4 -i hdfs:///user/USERNAME/MAPIMAGE -m 40 -o 18   
```
-t is TABLENAME eg. simmap-18-4    
-i is image map stored in HDFS   
-m is the number of mappers   
-o is the resolution of orientation   
   
# On-line:  
  
## 1) Checking environment variables  
Make sure ```mcls-all-1.00.00.jar``` and ```jcommander-1.36-SNAPSHOT.jar``` are in or withing the floders of ```$hadoop classpath```. Otherwise read the subsection of [Export environment variables](README.md#1-export-environment-variables) for more information.
  
## 2) Execute the localization program  
```
$hadoop jar mcls-all-1.00.00.jar imclroe.Main -i file:///home/USERNAME/MAPIMAGE -o 18 -cl true -t simmap-18-4 -rx 250 -ry 170 --samcldelta 0.0001 --samclxi 0.05 -n 200  
```
-i is the route of map image  
-o is the resolution of the map  
-cl refers whether this execution uses cloud computation
-t is TABLENAME  
-rx and -ry are not necessary, because the initial coordinate can be tuned via User Interface.
--samcldelta is the parameter for the size of Similar Energy Region. This can be tuned via User Interface.  
--samclxi is the sensitivity for detecting kidnapping situation  
-n is the number of particles
  
  
# Touble Shooting:  
  
## 1) JRE Version Set Up  
If there is some errors like that "Unsupported major.minor version 51.0", the vesions of Java Runtime Environment (JRE) and the JAR.jar file are different.  
51.0 represents that the JAR.jar file is built by Java SE 7, 52.0 by Java SE 8, 50.0 by Java SE 6.  
The current JRE version can be found out by the command.  
```
$java -version  
```
  
## 2) When Pre-caching failed  
Hadoop Distribution File System (HDFS) is similar to the file system of linux. Both of them require permission.
Please check the folder in HDFS for storing HFiles, output of pre-caching, whether the permission is allowed to be read written by others.  
The command checks file status. Please find the folder where storing HFiles of HBase.  
```
$hadoop fs -ls /user/hbase/...  
```

The command change the ownership.  
```
$ hadoop fs -chown [-R] [OWNER][:[GROUP]] URI [URI]
``` 
-R is recursive, which means, if URI is a folder, all of the sub-folders and files in URI will be changed.  
OWNER is the user who is going to own URI.  
GROUP is the group who is going to own URI.  
URI is the routes, such as hdfs:///user/hbase. If there are many URIs, use blank separate them.  

The command change the permission. Please refer to the [instruction website](https://hadoop.apache.org/docs/r2.7.1/hadoop-project-dist/hadoop-common/FileSystemShell.html#chmod).  
```
$hadoop fs -chmod ...   
```
  
## 3) Deleting HBase Tables
```
$./removeTable.sh TABLENAME
```
  
## 4) Making shell scripts executable  
```
$chmod a+x createTable.sh removeTable.sh  
```

# Further Reading and Video
The video shows how user can control via user interface and the performing of IMCLROE requesting computation resources on 8-node cloud server.

https://youtu.be/gl0YwFDF094

https://doi.org/10.1109/CEC.2016.7744365
