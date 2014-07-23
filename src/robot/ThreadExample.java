package robot;
public class ThreadExample {
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws InterruptedException{
		
//	    Thread t1 = new Thread(new HelloThread(), "T1");
//	    Thread t2 = new Thread(new HelloThread(t1), "T2");
//	    //t1.start();
//	    //t1.wait();
//	    t2.start();
	    // 取得目前執行緒數量
		double f = 1000.231;
		boolean b = false;
		String s = (b? "lock":"unlock");
	    System.out.println(s); 
	  }

	public static class HelloThread implements Runnable{
		public HelloThread(Thread t1){
			super();
			this.t = t1;
		}
		private Thread t = null;
		public HelloThread(){
			super();
		}
		
		public void run(){
			System.out.println("start to count");
			for(int i=1; i<10; i++){
				
				// 取得目前執行緒名稱
				String tName = Thread.currentThread().getName();       
				System.out.println(tName + ":" + i);
				
			}
			if(this.t!=null){
				System.out.println("wake up "+ this.t.getName());
				notifyAll();
			}
				
		}
	}
}