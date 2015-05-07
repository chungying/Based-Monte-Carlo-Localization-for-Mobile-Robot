package util.robot;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class ThreadExample {
	    private String str = "outer";
		public void setStr(String str) {
			this.str = str;
		}

		public Inner in;
	    public void print() {
	      System.out.println(str);
	    }
	    
	    public ThreadExample(String test){
	    	in = new Inner();
	    	this.str = test;
	    }
	    public ThreadExample(){
	    }

	    public class Inner extends ThreadExample{
			public Inner() {
				this.setStr("inner");
			}
	        public void redo1() {
	            print();
	        }
	        public void redo2() {
	            ThreadExample.this.print();
	            this.print();
	        }
	    }
	    
	    public static void main(String[] args) {
//	    	ThreadExample t = new ThreadExample("lalala");
//	    	//t.in.redo1();
//	    	t.in.redo2();
//	    	double t = 4.9e-324;
//	    	System.out.println( t);
//	    	System.out.println(t<0);
//	    	System.out.println(t==0);
//	    	System.out.println(t>0);
//	    	
	    	
	    	LinkedHashMap<String, Object> test = new LinkedHashMap<String, Object>();
	    	test.put("B",0.2f);
	    	test.put("F",0.5f);
	    	test.put("A",0.1f);
	    	for(Entry<String,Object> entry: test.entrySet()){
	    		System.out.print(entry.getKey()+"\t");
	    	}
	    	System.out.println();
	    	for(Entry<String,Object> entry: test.entrySet()){
	    		System.out.print(entry.getValue()+"\t");
	    	}
	    	
	    }
}