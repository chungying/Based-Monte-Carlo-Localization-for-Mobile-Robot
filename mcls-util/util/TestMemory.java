package util;

import java.lang.Runtime;
/**
* Class: TestMemory
* @author: Viral Patel
* @description: Prints JVM memory utilization statistics
* http://viralpatel.net/blogs/getting-jvm-heap-size-used-memory-total-memory-using-java-runtime/
*/
public class TestMemory {
  
  public static void main(String [] args) {
    
    int mb = 1024*1024;
    
    //Getting the runtime reference from system
    Runtime runtime = Runtime.getRuntime();
    
    System.out.println("##### Heap utilization statistics #####");
    
    //Print used memory
    System.out.println("Used Memory:" 
      + (runtime.totalMemory() - runtime.freeMemory()) / mb + " MB");

    //Print free memory
    System.out.println("Free Memory:" 
      + runtime.freeMemory() / mb + " MB");
    
    //Print total available memory
    System.out.println("Total Memory:" + runtime.totalMemory() / mb + " MB");

    //Print Maximum available memory
    System.out.println("Max Memory:" + runtime.maxMemory() / mb + " MB");
  }
}

