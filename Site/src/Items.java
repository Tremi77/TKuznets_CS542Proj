
public enum Items {
	X, Y, Z, L, M, N, A, B, C, D, E, F;
	
	static public boolean isMember(String item) {
		Items[] items = Items.values();
	       for (Items i : items)
	           if (i.toString().equals(item))
	               return true;
	       return false;
	   }
}
