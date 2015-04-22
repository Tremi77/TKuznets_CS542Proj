
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/*
 * To-Do list
 * 1. Stable storage
 * 1.1. Session Number
 * 1.2. Updates received 
 */

public class SiteImpl implements Serializable, Site {
	
	// ********   SETTINGS   ************/
	private boolean   LOGGING = true;
	private boolean   TIMER  = false;
	private boolean   RECOVERSLEEP = false;
	private String    OK = "OK";
	private String    FAILED = "FAILED";
	private String    NSNERR = "NSNERR"; // NS inconsistency
	private String    UNREAD = "UNREAD"; // unreadable data item
	private int       N = 12; // number of objects
	private String    SEP =  "\n";//System.lineSeparator();
	private int       sleep = 30000;
	private Timer     gTimer = new Timer();
	
	// *******     VARIABLES ************/
	public    String siteName;
	public    int    siteNum;
	private   int    nextNum;
	private   SiteState state = SiteState.UP;
	private   boolean actFLocks = true;
	private   boolean actVector = true;
	
	private   Queue<WaitingOperation> opQueue = new LinkedList<WaitingOperation>();
	
	// To-Do stored in file
	public int  sessionNum  = 1; // SessionNumber
	
	
	SessionVector sessionVector[] = new SessionVector[3];
	Site          stubList[] = new Site[3]; // list of RMI objects(sites)
	
	Map<String, Integer> map = new HashMap<String, Integer>();    // map logical object name to id 
	Map<Integer, String> maprev = new HashMap<Integer, String>(); // map logical id to name
	String[]     objectVal = new String[N];  // logical objects
	boolean[]    objectMap = new boolean[N]; // true - readable, false - unreadable


	// ******   INITIALIZATION *********************
	
	public SiteImpl(int num) {
		siteName = "Site" + num;
		siteNum = num;
		
		// initialize map of names and indices
		for (int i = 0; i < Items.values().length; i++) {
			map.put(Items.values()[i].toString(), i);
			maprev.put(i, Items.values()[i].toString());
		}
		// initialize data items
		for (int i = 0; i < objectVal.length; i++) {
			objectVal[i] = "9";
			objectMap[i] = true;
		}
		
		for (int i = 0; i < sessionVector.length; i++) {
			sessionVector[i] = new SessionVector();
		}
		// Create log file
		File file = new File("Log_"+this.siteName+".log");
		file.delete();
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/*
	 * Initialization of session vector at starting
	 * @see Site#initSVector(SessionVector[])
	 */
	public void initSVector(SessionVector[] sessionVector) throws RemoteException {
		for (int i = 0; i < sessionVector.length; i++) {
			this.sessionVector[i].copy(sessionVector[i]);
		}
	}
	/*
	 * Set list of remote objects
	 * @see Site#initStubList(Site[])
	 */
	public void initStubList(Site[] stubs) throws RemoteException {
		stubList = stubs;
	}
	
	//************************************************
	// Serialization
	/*
	private void storeObject(Object obj, String name) {

		FileOutputStream fileOut;
		try {
			fileOut = new FileOutputStream(name+".txt");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(obj);
			out.close();
			fileOut.close();
			log("Serialization successful");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private void recoverObject(Object obj, String name) {
		FileInputStream fileIn;
		try {
			fileIn = new FileInputStream(name+".txt");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			try {
				obj = in.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			in.close();
			fileIn.close();
			log("Deserialization successful");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	*/
	//********************** CHANGE MODE ************************
    /*
     * Set timer on/off
     * @see Site#setTimer(boolean)
     */
	public void setTimer(boolean t) throws RemoteException {
		TIMER = t;
		if (LOGGING) {
			if (t) log("Timer ON");
			else log("Timer OFF");
		}
		else {
			if (t) System.out.println(SEP + this.siteName + ": Timer ON");
			else System.out.println(SEP + this.siteName + ": Timer ON");
		}
	}
	
	/*
	 * Set logging on/off
	 * @see Site#setLogging(boolean)
	 */
	public void setLogging(boolean l) throws RemoteException {
		if (l) {
			LOGGING = l;
			log("Log ON");
		} 
		else {
			log("Log OFF");
			LOGGING = l;
		}
	}
	/*
	 * Set sleeping mode on/off
	 * @see Site#setRecoverSleep(boolean)
	 */
	public void setRecoverSleep(boolean s) throws RemoteException {
		RECOVERSLEEP = s;
		if (s) {
			log("Sleep mode ON");
		} else log("Sleep mode OFF");;
	}
	// ***************** GET SET VALUE  ***************************
	/*
	 * Set session number
	 */
	private void setSessionNumber(int sessionNum) {
		this.sessionNum = sessionNum;
		log("Set actual_session_number = " + this.sessionNum);
	}
	
	/*
	 * Set Nominal Session Number of a Site
	 */
	private void setNSN(int siteNum, int sessionNum) {
		sessionVector[siteNum].sessionNum = sessionNum;
		log("Set nominal_session_number for Site" + siteNum + " = " + sessionNum, 1);
	}
	/*
	 * Get logical object value
	 */
	private String getItem(String item) throws ItemNotExistException {
		if (!Items.isMember(item)) throw new ItemNotExistException("Data item " + item + " doesn't exist");
		return objectVal[map.get(item).intValue()]; 
	}
	
	/*
	 * Write logical object value
	 */
	private void setItem(String item, String value) throws ItemNotExistException {
		//stopWatch.start("Write value");
		if (!Items.isMember(item)) throw new ItemNotExistException("Data item " + item + " doesn't exist");
		int i = map.get(item);
		objectVal[i] = value;
		objectMap[i] = true;
		//stopWatch.stop();
		//storeObject(objectList, "objList");
	}
	
	/*
	 * Gets session vector
	 */
	public SessionVector[] getSessionVector() throws RemoteException {
		if (state == SiteState.DOWN) throw new java.rmi.RemoteException("Times out");
		if (state == SiteState.RECOVERING) throw new java.rmi.RemoteException("Recovering");
		log("Send session_vector");
		return sessionVector;
	}
	
	/*
	 * Set session vector
	 */
	public void setSessionVector(int siteNum, SessionVector sv) {
		this.sessionVector[siteNum].copy(sv);
	}
	
	// ************ STOPWATCH  *************************
	/*
	 * Timer wrap
	 */
	private class Timer extends StopWatch{
		
		public Timer() {
			super();
		}
		
		public Timer(String id) {
			super(id);
		}
		
		private void sTimer() {
			if (TIMER) super.start();
		}
		
		private void sTimer(String taskName) {
			if (TIMER) super.start(taskName);
		}
	
		private void eTimer() {
			if (TIMER) super.stop();
		}
		
		private void pprintTimer() {
			if (TIMER && !LOGGING) System.out.println(super.prettyPrint());
			else if (TIMER && !LOGGING) log(super.prettyPrint(), true);
		}
		
		private void printTimer() {
			if (TIMER && !LOGGING) System.out.println(super.shortSummary());
			else if (TIMER && LOGGING) log(super.shortSummary(), true);
		}
	}
	
	// ************ PRINT INFO *************************
	/*
	 * Print vector
	 * @see Site#printVector()
	 */
	public void printVector() throws RemoteException  {
		StringBuffer sb = new StringBuffer();
		
		if (state != SiteState.UP) sb.append(SEP + "Site is " + state.toString());
		sb.append(SEP + "Session vector ");
		if (state == SiteState.DOWN) sb.append("(obsolete)");
		
		sb.append("" + SEP);
		
		sb.append("|  S0  |  S1  |  S2  | " + SEP);
		sb.append("|------|------|------| " + SEP);
		sb.append("|  " + sessionVector[0] + "   |  " + sessionVector[1] + "   |  "+ sessionVector[2] + "   | ");
		
		log(sb.toString());
		
	}
	/*
	 * Print logical object values
	 * @see Site#printValues()
	 */
	public void printValues() throws RemoteException  {
		StringBuffer sb = new StringBuffer();
		
		if (state != SiteState.UP) sb.append(SEP + "Site is " + state.toString());
		sb.append(SEP + "Data items ");
		if (state == SiteState.DOWN) sb.append("(obsolete)");
		
		sb.append("" + SEP);
		
		sb.append("|  X  |  Y  |  Z  |  L  |  M  |  N  |  A  |  B  |  C  |  D  |  E  |  F  |" + SEP);
		sb.append("|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|" + SEP);
		sb.append("|  " + objectVal[0] + "  |  " + objectVal[1] + "  |  " + objectVal[2] + 
				"  |  " + objectVal[3] + "  |  " + objectVal[4] + "  |  " + objectVal[5] + 
				"  |  " + objectVal[6] + "  |  " + objectVal[7] + "  |  " + objectVal[8] + 
				"  |  " + objectVal[9] + "  |  " + objectVal[10] + "  |  " + objectVal[11] + "  | ");
		
		log(sb.toString());
	}
	/*
	 * Print fail-lock table
	 * @see Site#printFLocks()
	 */
	public void printFLocks() throws RemoteException {
		StringBuffer sb = new StringBuffer();
		
		if (state != SiteState.UP) sb.append(SEP + "Site is " + state.toString());
		sb.append(SEP + "Fail-locks ");
		if (state == SiteState.DOWN) sb.append("(obsolete)");
		
		sb.append("" + SEP);
		
		for (int i = 0; i < sessionVector.length; i++) {
			sb.append("-site" + i + ": "); 
			sb.append(sessionVector[i].toStringFailLock() + " " + SEP);
		}
		
		log(sb.toString());
	}
	
	public void printUnreadable() throws RemoteException {
		StringBuffer sb = new StringBuffer();

		if (state != SiteState.UP) sb.append(SEP + "Site is " + state.toString());
		sb.append(SEP + "Availability of data items ");
		
		sb.append("" + SEP);
		
		sb.append("|  X  |  Y  |  Z  |  L  |  M  |  N  |  A  |  B  |  C  |  D  |  E  |  F  | " + SEP);
		sb.append("|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----| " + SEP);
		sb.append("|" + objectMap[0] + " |" + objectMap[1] + " |" + objectMap[2] + 
			 	 " |" + objectMap[3] + " |" + objectMap[4] + "| " + objectMap[5] + 
			 	 " |" + objectMap[6] + " |" + objectMap[7] + " |" + objectMap[8] + 
			 	 " |" + objectMap[9] + " |" + objectMap[10] + " |" + objectMap[11] + " | ");
		
		log(sb.toString());
	}
	
	public void printSessionNumber() throws RemoteException {
		log("Actual session number: " + this.sessionNum);
	}
	
	public void printHeading() throws RemoteException {
		// To-Do print Heading and initial info
		StringBuffer sb = new StringBuffer();
		sb.append("***************************************************");
		sb.append("*                      " + this.siteName + "                      *");
		sb.append("***************************************************");
		log(sb.toString());
	}
	
	// *****************  LOG  ****************************
	
	private void log(String message, int depth, boolean time) {
		if (LOGGING) {
			
			System.out.println("");
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
			Date dt = new Date(System.currentTimeMillis());
			
			String content = new String();
			if (!time) content = sdf.format(dt) + " ";
			
			if (depth > 0) {
				for (int i = 0; i < depth; i++) {
					System.out.print("       ");
				}
			}
			else System.out.print(siteName + ": ");

			content = content + message;
			System.out.println(content);
			
			try {
				File file = new File("Log_"+this.siteName+".log");
				FileWriter fw = new FileWriter(file, true);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(content);
				bw.newLine();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	private void log(String message, boolean time) {
		log(message, 0, time);
	}

	private void log(String message, int depth) {
		log(message, depth, false);
	}
	
	private void log(String message) {
		log(message, 0, false);
	}
	
	// ****************************************************
	
	/*
	 * Excecute commands READ , WRITE, REPLICATE, UP, DOWN
	 * @return if read operation, then result else OK, UNREAD or NSNERR
	 * @see Site#execute(Operation, int[])
	 */
	public String execute(Operation op, String[] args) throws RemoteException {
		if (state == SiteState.DOWN) throw new SiteTimeOutException("Time's out: " + siteName + " is down");
		if (state == SiteState.RECOVERING) {
			switch (op) {
			case READ:
				log("** SITE IS RECOVERING **");
				return "** SITE IS RECOVERING **";
			case WRITE:
			case REPLICATE:
				opQueue.add(new WaitingOperation(op, args));
				return OK;
			}
			
		} 
		int site = 0; int ns = 0;
		
		// Log
		switch (op) {
		case UP:
			site = Integer.parseInt(args[0]);
			ns = Integer.parseInt(args[1]);
			log("Announcement about recovering of " + sessionVector[site].siteName + " is received");
			break;
		case DOWN:
			site = Integer.parseInt(args[0]);
			log("Announcement about failure of " + sessionVector[site].siteName + " is received from " + args[1]);
			break;
		//case WRITE:
		case REPLICATE:
		case READ:
			String val;
			if (op == Operation.READ) val = objectVal[map.get(args[1]).intValue()];
			else val = args[2];
			log("Execute " + op + " " + args[1] + " = " + val);
			break;
		}
		
		
		// Execute
		switch (op) {
		case UP: // 0 - site, 1 - ns[]
			setNSN(site, ns);
			break;
		case DOWN: // 0 - site
			setNSN(site, 0);
			break;
		case WRITE: //1 - data item, 2 - new value
			gTimer = new Timer("Write");
			gTimer.sTimer();
			
			try {
				setItem(args[1], args[2]);
				log("Execute " + op + " " + args[1] + " = " + args[2]);
			} catch (ItemNotExistException e) {
				log(e.getMessage());
				break;
			}
			writeAllAvailable(args);
			
			gTimer.eTimer();
			gTimer.printTimer();
			break;
		case REPLICATE: // 0 - ns[], 1 - data item, 2 - new value
			if (sessionNum != Integer.parseInt(args[0])) {
				//throw new InvalidNSNException("as[] = " + sessionNum + " and ns[] = " + args[0] + " don't match");
				log("session_number = " + sessionNum + " and nominal_session_number = " + args[0] + " don't match");
				return NSNERR;
			}
			setItem(args[1], args[2]);
			break;
		case READ: // 1 - data item
			gTimer = new Timer("Read");
			gTimer.sTimer();
			
			String val = new String(); 
			try {
				val = getItem(args[1]);
				if (objectMap[map.get(args[1]).intValue()] == false) return UNREAD;
			} catch (ItemNotExistException e) {
				log(e.getMessage());
				break;
			}
			
			gTimer.eTimer();
			gTimer.printTimer();
			
			return val;
		}
		
		return OK;
	}
	
	/*
	 * Set fail-lock on logical object
	 * @see Site#setFailLock(int, java.lang.String)
	 */
	public void setFailLock(int site, String item) throws RemoteException {
		if (state == SiteState.DOWN) throw new SiteTimeOutException("Time's out: " + siteName + " is down");
		log("Set fail-lock on item " + item + " for " + sessionVector[site].siteName);
		sessionVector[site].setFailLock(item);
	}
	
	/*
	 * Send fail-lock to available sites
	 */
	private void shareFailLock(int site, String item) throws RemoteException {
		Timer lTimer = new Timer("Share fail-locks");
		lTimer.start();
		this.setFailLock(site, item);
		log("Share fail-lock", 1);
		// share fail-locks
		for (int i = 0; i < sessionVector.length; i++) {
			
			if (this.siteNum != sessionVector[i].siteNum && sessionVector[i].sessionNum != 0) {
				try {
					stubList[i].setFailLock(site, item);
				} catch (RemoteException e) {
					announceFailure(sessionVector[i].siteNum);
				}
			}
		}	
		lTimer.stop();
		lTimer.printTimer();
	}
	
	/*
	 * Send logical object value to all available sites
	 */
	private void writeAllAvailable(String[] args) throws RemoteException {
		
		for (int i = 0; i < sessionVector.length; i++) {
			
			if (this.siteNum != sessionVector[i].siteNum && sessionVector[i].sessionNum != 0) {
				
				try {
					args[0] = Integer.toString(sessionVector[i].sessionNum);
					stubList[i].execute(Operation.REPLICATE, args);
				}
				catch (RemoteException e) {
					// write fail lock										
					announceFailure(sessionVector[i].siteNum);
					shareFailLock(i, args[1]);
				}
			}
			else if (sessionVector[i].sessionNum == 0) {
				shareFailLock(i, args[1]);
			}
		}
	}
	
	public SiteState ping() throws RemoteException  {
		return state;
	}
	
	/*
	 * Returns the set of failed locks to recovering site
	 */
	public HashMap<String, Date> getFailLock(int siteNum) throws RemoteException {		
		if (state == SiteState.DOWN) throw new SiteTimeOutException("Time's out: " + siteName + " is down");
		if (sessionVector[siteNum].getFailLockTab().isEmpty()) log("No fail-locks to send");
		else log("Send fail-locks to " + siteNum);
		return sessionVector[siteNum].getFailLockTab();
	}
	
	public void clearFailLock(int siteNum) throws RemoteException {
		if (state == SiteState.DOWN) throw new SiteTimeOutException("Time's out: " + siteName + " is down");
		sessionVector[siteNum].clearFailLocks();
	}
	
	// **************** CONTROL TOOLS ****************************
	
	private void sleep() {
		try {
		    Thread.sleep(sleep);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
	}
	
	public void fail() throws RemoteException {
		if (state != SiteState.UP) {
			log("* ALREADY DOWN OR RECOVERING *"); 
			return;
		}
		log(" *** FAILURE ***");
		state = SiteState.DOWN;
	}
	
	// ********************* CONTROL TRANSACTION ************************	
	/*
	 * Announce failure to all available sites
	 */
	private void announceFailure(int failedSite) throws RemoteException {
		log("Discovered that " + sessionVector[failedSite].siteName + " is down");
		
		Timer lTimer = new Timer("Announce failure");
		lTimer.start();
		
		setNSN(failedSite, 0);
		
		String[] args = new String[2];
		args[0] = Integer.toString(failedSite);
		args[1] = this.siteName;
		for (int i = 0; i < sessionVector.length; i++) {
			if (this.siteNum != sessionVector[i].siteNum && sessionVector[i].siteNum != failedSite && sessionVector[i].sessionNum > 0) {
				log("Announce failure of " + sessionVector[failedSite].siteName + " to " + sessionVector[i].siteName);	
				try {
					stubList[i].execute(Operation.DOWN, args);
				} catch (RemoteException e) {
					log("Time's out");
					announceFailure(i);
				}
			}
		}
		
		lTimer.stop();
		lTimer.printTimer();
	}

	/*
	 * Recover the site
	 * @see Site#recover()
	 */
	public void recover() throws RemoteException {
		gTimer = new Timer("Recovery");
		if (state != SiteState.DOWN) {
			log("* ALREADY UP OR RECOVERING *"); 
			return;
		}
		log(" *** START RECOVERING ***");
		if (LOGGING) {
			printValues();
			printVector();
		}
		gTimer.sTimer("initialize");		
		nextNum = sessionNum + 1; 
		setSessionNumber(0);
		state = SiteState.RECOVERING;
		gTimer.eTimer();
		
		// get new session vector
		// at least 1 site is up
		gTimer.sTimer("get vector");
		for (int i = 0; i < sessionVector.length; i++) {
			if (this.siteNum != sessionVector[i].siteNum) {
				try {
						log("Get session vector from " + sessionVector[i].siteName);
						SessionVector[] v = stubList[i].getSessionVector();
						for (int j = 0; j < sessionVector.length; j++) {
							sessionVector[j].copy(v[j]);
						}
						break;
				} catch (RemoteException e) {
					log("Time's out");
					//announceFailure(i);
				}
			}
		}
		gTimer.eTimer();
		if (LOGGING) printVector();
		// announce a new session number
		gTimer.sTimer("announce new session_number");	
		String[] args = new String[2];
		args[0] = Integer.toString(this.siteNum);
		args[1] = Integer.toString(nextNum);
		sessionVector[this.siteNum].sessionNum = nextNum;
		for (int i = 0; i < sessionVector.length; i++) {
			if (this.siteNum != sessionVector[i].siteNum && sessionVector[i].sessionNum != 0) {
				log("Announce recovering of " + this.siteName + " to " + sessionVector[i].siteName + " with session number " + nextNum);	
				try {
					stubList[i].execute(Operation.UP, args);
				} catch (RemoteException e) {
					log("Time's out");
					announceFailure(i);
				}
			}
		} // Sites can send updates now to recovering site
		gTimer.eTimer();	
		// collect fail locks
		log("Collect fail-locks");
		gTimer.sTimer("collect fail-locks");	
		HashMap<String, Date> flocktab = new HashMap<String, Date>();
		int alive = 0;
		HashMap<String, Date> tab;
		for (int i = 0; i < sessionVector.length; i++) {
			if (this.siteNum != sessionVector[i].siteNum && sessionVector[i].sessionNum != 0) {
				
				log("Get fail-locks from " + sessionVector[i].siteName);	
				tab = new HashMap<String, Date>();
				try {
					tab  = stubList[i].getFailLock(this.siteNum);
					alive++;
				} catch (RemoteException e) {
					log("Time's out");
					announceFailure(i);
				}
				for (String key : tab.keySet() ) {
					flocktab.put(key, tab.get(key));
				}

			}
		}
		gTimer.eTimer();
		// sleeping mode
		if (RECOVERSLEEP) sleep();
		
		// marks out-of-date data items
		log("Mark out-of-date data items");
		gTimer.sTimer("mark data items");	
		StringBuffer s = new StringBuffer("Logical objects marked: ");
		if (alive > 0) {
			for (String key : flocktab.keySet() ) {
				objectMap[map.get(key).intValue()] = false;
				s.append(key + " ");
			}
		} else { // mark all
			for (int i = 0; i < objectMap.length; i++) {
				objectMap[i] = false;
				s.append(maprev.get(i) + " ");
				//for (int j = 0; j < sessionVector.length; j++) {
				//	if (this.siteNum != sessionVector[j].siteNum && sessionVector[j].sessionNum == 0) {
				//		sessionVector[j].setFailLock(maprev.get(i));
				//	}
				//}
			}
		}
		gTimer.eTimer();
		if (LOGGING) printUnreadable();
		// update out-of-date items
		log("Update out-of-date items");
		alive = flocktab.size(); // store if locks exist
		gTimer.sTimer("update items");	
		for (int i = 0; i < sessionVector.length; i++) {
			if (this.siteNum != sessionVector[i].siteNum && sessionVector[i].sessionNum != 0) {
				Iterator itr = flocktab.keySet().iterator();
				while (itr.hasNext()) {
					String key = (String)itr.next();
					String[] params = new String[2];
					params[0] = Integer.toString(sessionVector[i].sessionNum);
					params[1] = key; 
					String res = new String();
					try {
						res = stubList[i].execute(Operation.READ, params);
						if (res != UNREAD) {
							setItem(key, res);
							itr.remove();
							log("UPDATE " + key + " = " + objectVal[map.get(key).intValue()] + " from " + sessionVector[i].siteName);
						}
					} catch (RemoteException e) {
						log("Time's out");
						announceFailure(i);
					}
					
				}				
				
			}
		}
		gTimer.eTimer();
		if (LOGGING) {
			printValues();
		}
		// clean fail locks 
		log("Clean fail-locks of " + this.siteName + " at all sites");
		gTimer.sTimer("clean fail-locks");	
		if (alive > 0) { // previously stored size from locks
			this.sessionVector[this.siteNum].clearFailLocks(); // clean itself then others
			for (int i = 0; i < sessionVector.length; i++) {
				if (this.siteNum != sessionVector[i].siteNum && sessionVector[i].sessionNum != 0) {
					try {
						stubList[i].clearFailLock(this.siteNum);
					} catch (RemoteException e) {
						log("Time's out");
						announceFailure(i);
					}
				}
			}
			
		}
		gTimer.eTimer();	
		// set new session number
		gTimer.sTimer("set a new session");
		setSessionNumber(nextNum);
		this.sessionVector[this.siteNum].sessionNum = nextNum;
		this.state = SiteState.UP;
		gTimer.eTimer();
		log(" *** UP ***");
		gTimer.pprintTimer();
		
		if (!opQueue.isEmpty()) {
			log("Perform operations from queue", 1);
			while (!opQueue.isEmpty()) {
				WaitingOperation op = opQueue.poll();
				this.execute(op.op, op.args);
			}
		}
		
		//log(SEP + stopWatch.prettyPrint() + SEP);
		//System.out.println(SEP + stopWatch.prettyPrint() + SEP);
		
	}
	
	// ************************ TEST ******************************
	
	public static void main(String[] args) throws RemoteException {
		
		System.out.println("***********************************************");
		System.out.println("**             Sites Container               **");
		System.out.println("**           Site0, Site1, Site2             **");
		System.out.println("***********************************************");
		
		
		SiteImpl site0 = new SiteImpl(0);
		SiteImpl site1 = new SiteImpl(1);
		SiteImpl site2 = new SiteImpl(2);
		
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
		
		System.err.println("Site 0 UP \nSite 1 UP \nSite 2 UP");
		
	}

}
