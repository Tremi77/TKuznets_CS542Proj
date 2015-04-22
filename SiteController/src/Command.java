
public enum Command {
	Q, FAIL, WAKE_UP, WRITE, READ, PRINT_ITEMS, PRINT_VECTOR, PRINT_FLOCKS, PRINT_UNREAD, PRINT_SESSION, TIMER, LOG, HELP, SLEEP;
	
	static public boolean isMember(String com) {
		Command[] commands = Command.values();
	       for (Command command : commands)
	           if (command.equals(com))
	               return true;
	       return false;
	   }
}
