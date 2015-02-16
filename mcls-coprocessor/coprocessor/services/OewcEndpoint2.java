package coprocessor.services;

import java.io.IOException;

import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.coprocessor.CoprocessorException;
import org.apache.hadoop.hbase.coprocessor.CoprocessorService;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;

import coprocessor.services.generated.OewcProtos2;
import coprocessor.services.generated.OewcProtos2.OewcRequest;
import coprocessor.services.generated.OewcProtos2.OewcResponse;

public class OewcEndpoint2 extends OewcProtos2.OewcService
implements Coprocessor, CoprocessorService{

	@Override
	public Service getService() {
		return this;
	}

	private RegionCoprocessorEnvironment env;
	@Override
	public void start(CoprocessorEnvironment arg0) throws IOException {
		if(env instanceof RegionCoprocessorEnvironment){
			this.env = (RegionCoprocessorEnvironment)env;
		} else {
			throw new CoprocessorException("Must be loaded on a table region!");
		}	
	}

	@Override
	public void stop(CoprocessorEnvironment arg0) throws IOException {
	}

	@Override
	public void getRowCount(RpcController controller, OewcRequest request,
			RpcCallback<OewcResponse> done) {
		// TODO Auto-generated method stub
		
	}

}
