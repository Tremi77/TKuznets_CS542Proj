
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class SiteController {

	private static String delims = "[ ]+";
	private static String COMMANDLINE = ">>> ";
	private static String OK = "OK";
	private static String FAILED = "FAILED";
	private static String NSNERR = "NSNERR"; // NS inconsistency
	private static String UNREAD = "UNREAD"; // unreadable data item

    private SiteController() {}

	private static void sleep() {
		try {
		    Thread.sleep(1000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
	}
    
	private static void printHelp() {
		// To-Do print help
		System.out.println("Usage: [site_num] [command] [parameters]");
		System.out.println("where ");
		System.out.println("site_num in: 0, 1, 2");
		System.out.println("command format and description:   ");
		System.out.println("HELP                                 print help message");
		System.out.println("Q                                    exit");	
		System.out.println("<site_num> READ <data_item>          read a value");
		System.out.println("<site_num> WRITE <data_item> <value> write a new value");
		System.out.println("<site_num> FAIL                      simulate site failure");
		System.out.println("<site_num> WAKE_UP                   launch recovery process");
		System.out.println("<site_num> PRINT_ITEMS               print values of the data items");
		System.out.println("<site_num> PRINT_VECTOR              print vector");
		System.out.println("<site_num> PRINT_FLOCKS              print fail-lock table");
		System.out.println("<site_num> PRINT_UNREAD              print unreadable data items");
		System.out.println("<site_num> PRINT_SESSION             print sctual sesison number");
		System.out.println("<site_num> TIMER true|false          turn timer on/off");
		System.out.println("<site_num> LOG true|false            turn logging on/off");		
		System.out.println("<site_num> SLEEP true|false          turn sleeping mode on/off \n");
	}
	
    public static void main(String[] args) {
    	System.out.println("*********************************************");
    	System.out.println("**        Event Simulation Terminal        **");
    	System.out.println("*********************************************");
    	printHelp();
    	
    	String host = (args.length < 1) ? null : args[0];
    	try {
    		
    	    Registry registry = LocateRegistry.getRegistry(host);
    	    
    	    Site stub0 = (Site) registry.lookup("Site0");
    	    Site stub1 = (Site) registry.lookup("Site1");
    	    Site stub2 = (Site) registry.lookup("Site2");
    	    
    	    // read from command line
	    	String act = new String();
	    	while (true) {//(!act.equals(Command.Q.toString())) {
		        System.out.print(COMMANDLINE);
		        System.out.flush();
		        try {
			        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			        try {
			        	act = br.readLine();
			        } catch (IOException ioe) {
			           System.out.println("IO error trying to read your name!");
			           System.exit(1);
			        }
			        //-------------------------
			        String[] tokens = act.split(delims);
			        int site = -1;
			        Command com;
			        boolean swch = true;
			        
			        try {
			        	if (tokens[0].equals(Command.Q.toString())) System.exit(0);;
				        site = Integer.parseInt(tokens[0]);
					    com = Command.valueOf(tokens[1]);

				        if ((com == Command.WRITE && tokens.length != 4) || 
				            ((com == Command.READ || com == Command.TIMER || com == Command.LOG || com == Command.SLEEP) && tokens.length != 3) ||
					        	((com == Command.FAIL || com == Command.PRINT_FLOCKS || com == Command.PRINT_ITEMS || 
					        	  com == Command.PRINT_UNREAD || com == Command.PRINT_VECTOR || com == Command.WAKE_UP || com == Command.PRINT_SESSION) 
					        	  && tokens.length != 2)) 
					        	throw new Exception("Incorrect number of parameters");
				        if (com == Command.TIMER || com == Command.LOG || com == Command.SLEEP) swch = Boolean.parseBoolean(tokens[2]);
			        } catch (Exception e) {
			        	System.out.println(e.getMessage());
			        	//System.out.println("Unrecognized syntax. Try again. \n");
			        	printHelp();
			        	System.out.flush();
			        	continue;
			        }
			        
			        Site stub = null;
			        if (site == 0) stub = stub0;
			        if (site == 1) stub = stub1;
			        if (site == 2) stub = stub2;
			        
			        String[] params = new String[3];
			        switch (com) {
			        case FAIL: 
			        	stub.fail();
			        	break;
			        case WAKE_UP:
			        	stub.recover();
			        	break;
			        case WRITE:
			        	params[1] = tokens[2];
			        	params[2] = tokens[3];
			        	try {
			        		stub.execute(Operation.WRITE, params);
			        	} catch (RemoteException e) {
			        		System.err.println("Time's out. The site is down or recovering."); System.err.flush();
			        	}
			        	break;
			        case READ:
			        	params[1] = tokens[2];
			        	try {
			        		String res;
			        		res = stub.execute(Operation.READ, params);
			        		if (res == UNREAD) {
			        			System.err.println("Data item is unreadable."); System.err.flush();
			        		}
			        		System.out.println(res);
			        	} catch (RemoteException e) {
			        		e.printStackTrace();
			        		System.err.println("Time's out. The site is down or recovering."); System.err.flush();
			        	}
			        	break;
			        case PRINT_ITEMS:
			        	stub.printValues();
			        	break;
			        case PRINT_VECTOR:
			        	stub.printVector();
			        	break;
			        case PRINT_FLOCKS:
			        	stub.printFLocks();
			        	break;
			        case PRINT_UNREAD:
			        	stub.printUnreadable();
			        case PRINT_SESSION:
			        	stub.printSessionNumber();
			        	break;
			        case HELP:
			        	printHelp();
			        	break;
			        case TIMER:
			        	stub.setTimer(swch);
			        	break;
			        case LOG:
			        	stub.setLogging(swch);
			        	break;
			        case SLEEP:
			        	stub.setRecoverSleep(swch);
			        	break;
			        }
		        } catch (Exception e) {System.err.println(e.getStackTrace());}
		        //-------------------------
	    	}
	  
		} catch (Exception e) {
		    System.err.println("Client exception: " + e.toString());
		    e.printStackTrace();
		}

    }
}