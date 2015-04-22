import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

public class SessionVector implements Serializable{
	
	public  int              siteNum;
	public  String 			 siteName; // for RMI
	public  int 			 sessionNum;
	public  Site             stub;// + stub for sites
	private HashMap<String, Date> failLockTab;
	
	/*private class Lock {
		String     dataItemId;
		boolean    failLockBit;
		Date       timeStamp;		
	}*/
	
	public SessionVector() {
		failLockTab = new HashMap<String, Date>();
	}
	
	public String toString() {
		return Integer.toString(sessionNum);
		//return "SiteNum: " + siteNum + ", SessionNum: " + sessionNum;
	}
	
	public HashMap<String, Date> getFailLockTab() {
		return failLockTab;
	}
	
	public void printFailLockTab() {
		System.out.println(failLockTab.toString());
	}

	public void setFailLock(String item) {
		failLockTab.put(item, new Date());
	}
	
	public void clearFailLocks() {
		failLockTab.clear();
	}
	
	public void copy(SessionVector v) {
		this.siteNum = v.siteNum;
		this.siteName = v.siteName;
		this.sessionNum = v.sessionNum;
		this.stub = v.stub;
	}
}
