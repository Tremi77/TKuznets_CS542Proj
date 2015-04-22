import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class Site1 {

	public static void main(String[] args) throws RemoteException {
		System.out.println("***********************************************");
		//System.out.println("**             Sites Container               **");
		System.out.println("**                  Site1                    **");
		System.out.println("***********************************************");
		
		SiteImpl site1 = new SiteImpl(1);

		site1 = new SiteImpl(1);
		
	    Site stub1 = (Site) UnicastRemoteObject.exportObject(site1, 0);
	    
	    Registry registry = LocateRegistry.getRegistry();

	    registry.rebind(site1.siteName, stub1);
		    
		SessionVector sessionVector[] = new SessionVector[3]; 
		Site stubs[] = new Site[3];
		
		sessionVector[0] = new SessionVector();
		sessionVector[1] = new SessionVector();
		sessionVector[1].siteNum = site1.siteNum;
		sessionVector[1].siteName = site1.siteName;
		sessionVector[1].sessionNum = 1;
		stubs[1] = stub1;
		sessionVector[2] = new SessionVector();
		
		site1.initSVector(sessionVector);
		
		site1.initStubList(stubs);
		
		System.err.println("Site 1 UP");

	}

}
