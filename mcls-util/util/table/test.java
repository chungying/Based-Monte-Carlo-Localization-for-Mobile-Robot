package util.table;

import java.io.IOException;
import java.util.TreeMap;

import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.coprocessor.example.generated.ExampleProtos;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.ServerRpcController;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.coprocessor.example.generated.ExampleProtos.RowCountService;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;


public class test extends Base{
	private static test ts;

	static public void main(String... args){
		String[] args2 = {
				"-t", "test6.18.split",
				""
		};
		if(args.length == 0)
			args = args2;
		
		ts = new test();
				
		try {
			ts.setup(args);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ts.run();
				
	}

	@Override
	public void run() {
		
		
		Batch.Call<RowCountService, Long> call = new Batch.Call<RowCountService, Long>(){

			@Override
			public Long call(RowCountService arg0) throws IOException {
				BlockingRpcCallback<ExampleProtos.CountResponse> done = new BlockingRpcCallback<ExampleProtos.CountResponse>(); 
				RpcController controller = new ServerRpcController();
				ExampleProtos.CountRequest request = ExampleProtos.CountRequest.newBuilder().build();
				arg0.getKeyValueCount(controller, request, done);
				return done.get().getCount();
			}
			
		};
		
		final TreeMap<String, Long> map = new TreeMap<String, Long>();
		Batch.Callback<Long> callback = new Batch.Callback<Long>() {

			@Override
			public void update(byte[] region, byte[] row, Long result) {
				map.put(Bytes.toString(region), result);
			}
			
		};
		
		try {
			ts.table.coprocessorService(RowCountService.class, null, null, call, callback);
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(map);
	}
}
