package util.table;

import java.io.IOException;

public class WarmUp extends Base{

	@Override
	public void run() {
		try {
			this.setup();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
		
		
		
		try {
			this.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
	}

}
