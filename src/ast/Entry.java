package ast;

public class Entry implements Comparable<Entry> {
    private int key;
    private String value;

    public Entry(int key, String value) {
        this.key = key;
        this.value = value;
    }
    public int getKey() {
    	return this.key;
    }
    public String getVal() {
    	return this.value;
    }

    // getters

    @Override
    public int compareTo(Entry other) {
    	if (this.getKey() == other.getKey()) {
    		return 0;
    	}
        return 1;
    }
}
