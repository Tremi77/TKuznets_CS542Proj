import java.rmi.RemoteException;

public class SiteTimeOutException extends RemoteException {

	public SiteTimeOutException()
    {
      super(); 
    }
	
	public SiteTimeOutException(String message)
	    {
	      super(message); 
	    }
}
