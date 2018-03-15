package endpoint.services;

import java.io.IOException;

import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.coprocessor.CoprocessorService;
import org.apache.hadoop.hbase.coprocessor.example.generated.ExampleProtos;
import org.apache.hadoop.hbase.coprocessor.example.generated.ExampleProtos.CountRequest;
import org.apache.hadoop.hbase.coprocessor.example.generated.ExampleProtos.CountResponse;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;

public class CellCountEndpoint extends ExampleProtos.RowCountService
implements Coprocessor, CoprocessorService{

	@Override
	public Service getService() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void start(CoprocessorEnvironment env) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop(CoprocessorEnvironment env) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getRowCount(RpcController controller, CountRequest request,
			RpcCallback<CountResponse> done) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getKeyValueCount(RpcController controller,
			CountRequest request, RpcCallback<CountResponse> done) {
		// TODO Auto-generated method stub
		
	}

}
