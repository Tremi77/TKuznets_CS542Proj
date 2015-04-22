import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class Site0 {

	public static void main(String[] args) throws RemoteException {
		System.out.println("***********************************************");
		//System.out.println("**             Sites Container               **");
		System.out.println("**                  Site0                    **");
		System.out.println("***********************************************");
		
		
		SiteImpl site0 = new SiteImpl(0);

		site0 = new SiteImpl(0);
		
	    Site stub0 = (Site) UnicastRemoteObject.exportObject(site0, 0);
	    
	    Registry registry = LocateRegistry.getRegistry();

	    registry.rebind(site0.siteName, stub0);
		    
		SessionVector sessionVector[] = new SessionVector[3]; 
		Site stubs[] = new Site[3];
		
		sessionVector[0] = new SessionVector();
	    sessionVector[0].siteNum = site0.siteNum;
		sessionVector[0].siteName = site0.siteName;
		sessionVector[0].sessionNum = 1;
		stubs[0] = stub0;
		sessionVector[1] = new SessionVector();
		sessionVector[2] = new SessionVector();
		
		site0.initSVector(sessionVector);
		
		site0.initStubList(stubs);
		
		System.err.println("Site 0 UP");

	}

}
