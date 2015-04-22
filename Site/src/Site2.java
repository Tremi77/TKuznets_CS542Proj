import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class Site2 {

	public static void main(String[] args) throws RemoteException {
		System.out.println("***********************************************");
		//System.out.println("**             Sites Container               **");
		System.out.println("**                  Site2                    **");
		System.out.println("***********************************************");
		
		try {
		    Thread.sleep(1000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
		
		SiteImpl site2 = new SiteImpl(2);

		site2 = new SiteImpl(2);
		
	    Site stub2 = (Site) UnicastRemoteObject.exportObject(site2, 0);
	    
	    Registry registry = LocateRegistry.getRegistry();

	    registry.rebind(site2.siteName, stub2);
		
		SessionVector sessionVector[] = new SessionVector[3]; 
		Site stubs[] = new Site[3];
	    Site stub0;
	    Site stub1;
		try {
			stub0 = (Site) registry.lookup("Site0");
			stub1 = (Site) registry.lookup("Site1");
		
			sessionVector[0] = new SessionVector();
		    sessionVector[0].siteNum = 0;
			sessionVector[0].siteName = "Site0";
			sessionVector[0].sessionNum = 1;
			stubs[0] = stub0;
			sessionVector[1] = new SessionVector();
			sessionVector[1].siteNum = 1;
			sessionVector[1].siteName = "Site1";
			sessionVector[1].sessionNum = 1;
			stubs[1] = stub1;
			sessionVector[2] = new SessionVector();
			sessionVector[2].siteNum = site2.siteNum;
			sessionVector[2].siteName = site2.siteName;
			sessionVector[2].sessionNum = 1;
			stubs[2] = stub2;
		    
			stub0.initSVector(sessionVector);
			stub1.initSVector(sessionVector);
			site2.initSVector(sessionVector);
			
			stub0.initStubList(stubs);
			stub1.initStubList(stubs);
			site2.initStubList(stubs);
		
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		
		System.err.println("Site 2 UP");

	}

}
