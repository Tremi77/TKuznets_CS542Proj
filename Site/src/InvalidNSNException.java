import java.rmi.RemoteException;


public class InvalidNSNException extends Exception {

	public InvalidNSNException()
    {
      super(); 
    }
	
	public InvalidNSNException(String message)
	    {
	      super(message); 
	    }
}
