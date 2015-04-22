import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class SiteTest {

	/** The display name of the test suite. */
	private static final String TEST_NAME = "3 sites operation";
	
	/** Success value, for readability. */
	protected static final boolean PASS = true;

	/** Failure value, for readability. */
	protected static final boolean FAIL = false;
	
	public SiteImpl site0;
	public SiteImpl site1;
	public SiteImpl site2;
	
	public void init() throws RemoteException {

		site0 = new SiteImpl(0);
		site1 = new SiteImpl(1);
		site2 = new SiteImpl(2);

	    Site stub0 = (Site) UnicastRemoteObject.exportObject(site0, 0);
	    Site stub1 = (Site) UnicastRemoteObject.exportObject(site1, 0);
	    Site stub2 = (Site) UnicastRemoteObject.exportObject(site2, 0);
	    
	    Registry registry = LocateRegistry.getRegistry();

	    registry.rebind(site0.siteName, stub0);
	    registry.rebind(site1.siteName, stub1);
	    registry.rebind(site2.siteName, stub2);
		    
		SessionVector sessionVector[] = new SessionVector[3]; 
		Site stubs[] = new Site[3];
		
		sessionVector[0] = new SessionVector();
	    sessionVector[0].siteNum = site0.siteNum;
		sessionVector[0].siteName = site0.siteName;
		sessionVector[0].sessionNum = 1;
		stubs[0] = stub0;
		sessionVector[1] = new SessionVector();
		sessionVector[1].siteNum = site1.siteNum;
		sessionVector[1].siteName = site1.siteName;
		sessionVector[1].sessionNum = 1;
		stubs[1] = stub1;
		sessionVector[2] = new SessionVector();
		sessionVector[2].siteNum = site2.siteNum;
		sessionVector[2].siteName = site2.siteName;
		sessionVector[2].sessionNum = 1;
		stubs[2] = stub2;
		
		site0.initSVector(sessionVector);
		site1.initSVector(sessionVector);
		site2.initSVector(sessionVector);
		
		site0.initStubList(stubs);
		site1.initStubList(stubs);
		site2.initStubList(stubs);
		
		System.err.println("Sites 0, 1, 2 ready");

	}

	
	private boolean test1() {
		try {
			System.out.println("\n****************************************");
			System.out.println("******   Test 1: Read value   **********");
			System.out.println("****************************************\n");
			
			String[] params = new String[3];
			
			params[0] = Integer.toString(site0.sessionNum);
			params[1] = "X";
			
			System.out.println(site0.siteName + " read item "+ params[1] + ": " + site0.execute(Operation.READ, params));
			
			System.out.print("\n\nTest 1 completed without exception.");
			return PASS;
		} catch (Exception exc) {
	
			exc.printStackTrace(System.out);
			System.out.print("\n\nTest 1 terminated because of exception.");
			return FAIL;
	
		}
	}
	
	private boolean test2() {
		try {
			System.out.println("\n*********************************************************");
			System.out.println("******   Test 2: Site 1 fails and recorevers   **********");
			System.out.println("*********************************************************\n");
						
			site1.fail();
			
			System.out.println("*** TRY TO WRITE ***");
			String[] params = new String[3];
			//params[0] = site0.sessionNum;
			params[1] = "X"; // item
			params[2] = "1"; // value
						
			site0.execute(Operation.WRITE, params);
			//System.out.println(site0.sessionVector[1].toStringFailLock());
			
			System.out.println("*** X IS CHANGED; ALL SITES ARE NOTIFIED ABOUT FAILURE ***");
			
			System.out.println(" ");
			System.out.println("*** STATE BEFORE RECOVERING ***");
			site0.printValues();
			site1.printValues();			
			site2.printValues();
			System.out.println(" ");
			site0.printVector();
			site1.printVector();
			site2.printVector();
			
			site1.recover();
			
			System.out.println(" ");
			System.out.println("*** STATE AFTER RECOVERING ***");
			site0.printValues();
			site1.printValues();			
			site2.printValues();
			System.out.println(" ");
			site0.printVector();
			site1.printVector();
			site2.printVector();
			System.out.println(" ");
			
			
			try {
			    Thread.sleep(1000);                 //1000 milliseconds is one second.
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			System.out.print("\n\nTest 1 completed without exception.");
			return PASS;
		} catch (Exception exc) {
	
			exc.printStackTrace(System.out);
			System.out.print("\n\nTest 1 terminated because of exception.");
			return FAIL;
	
		}
	}
	
	// *********************** MAIN ***************************************
	public static void main(String[] args) throws RemoteException {

			SiteTest st = new SiteTest();
			st.init();
			
			try {
			    Thread.sleep(1000);                 //1000 milliseconds is one second.
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			
			boolean status = PASS;
			status &= st.test1();
			status &= st.test2();
			//status &= st.test3();

			// display the final results
			System.out.println();
			if (status != PASS) {
				System.out.println("Error(s) encountered during " + TEST_NAME + ".");
			} else {
				System.out.println("All " + TEST_NAME
						+ " completed; verify output for correctness.");
			}

	    }
	
}
