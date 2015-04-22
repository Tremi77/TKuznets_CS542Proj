import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;

public interface Site extends Remote {
	
	public SessionVector[] getSessionVector() throws RemoteException;
	
	public String execute(Operation op, String[] args) throws RemoteException;
	
	public SiteState ping() throws RemoteException;
	
	public void printValues() throws RemoteException;
	
	public void printVector() throws RemoteException;
	
	public void printFLocks() throws RemoteException;
	
	public void printUnreadable() throws RemoteException;
	
	public void setFailLock(int site, String item) throws RemoteException;
	
	public void fail() throws RemoteException;
	
	public void recover() throws RemoteException;
	
	public HashMap<String, Date> getFailLock(int siteNum) throws RemoteException;
	
	public void clearFailLock(int siteNum) throws RemoteException;
	
	public void setTimer(boolean t) throws RemoteException;
	
	public void setLogging(boolean l) throws RemoteException;
	
	public void initSVector(SessionVector[] sessionVector) throws RemoteException;
	
	public void initStubList(Site[] stubs) throws RemoteException;
	
	public void setRecoverSleep(boolean s) throws RemoteException;
	
	public void printSessionNumber() throws RemoteException;
}
