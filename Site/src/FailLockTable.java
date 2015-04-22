import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

/* NOT USED */

public class FailLockTable {
	
	LinkedList<Lock> failLockTab = new LinkedList<Lock>();
	
	private class Lock {
		int     dataItemId;
		//boolean failLockBit;
		Date    timeStamp;		
		
		public void Lock(int itemId) {
			this.dataItemId = itemId;
			this.timeStamp = new Date();
		}
	}

	public void FailLockTable() {
		failLockTab = new LinkedList<Lock>();
	}
	
}
