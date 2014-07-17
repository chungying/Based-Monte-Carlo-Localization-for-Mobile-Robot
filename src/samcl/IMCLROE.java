package samcl;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.ServerRpcController;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

import coprocessor.services.OewcEndpoint;
import coprocessor.services.generated.OewcProtos.OewcRequest;
import coprocessor.services.generated.OewcProtos.OewcResponse;
import util.metrics.Particle;

public class IMCLROE extends SAMCL{
	

	@Override
	public void batchWeight(List<Particle> src, float[] robotMeasurements)
			throws ServiceException, Throwable {
		Batch.Call<OewcEndpoint,Long> b = new Batch.Call<OewcEndpoint, Long>(){

			@Override
			public Long call(OewcEndpoint endpoint) throws IOException {
				// TODO IMCLROE
				BlockingRpcCallback<OewcResponse> done = new BlockingRpcCallback<OewcResponse>();
				OewcRequest request = null;
				RpcController controller = new ServerRpcController();

				endpoint.getRowCount(controller, request, done);
				return done.get().getCount();
			}
			
		};
		this.table.coprocessorService(OewcEndpoint.class, null, null, b);
	}

	public IMCLROE(boolean cloud, int orientation, String map_filename,
			float delta_energy, int nt, float xI, float aLPHA,
			int tournament_presure) throws IOException {
		super(cloud, orientation, map_filename, delta_energy, nt, xI, aLPHA,
				tournament_presure);
		// TODO Auto-generated constructor stub
	}

}
