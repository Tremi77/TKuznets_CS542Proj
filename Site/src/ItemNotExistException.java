import java.rmi.RemoteException;


public class ItemNotExistException extends RemoteException {

	public ItemNotExistException()
    {
      super(); 
    }
	
	public ItemNotExistException(String message)
	    {
	      super(message); 
	    }
}
