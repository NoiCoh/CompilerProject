package ast;

public class Entry implements Comparable<Entry> {
    private String var_name;
    private String type;

    public Entry(String var_name, String type) {
        this.var_name = var_name;
        this.type = type;
    }
    public String getVarName() {
    	return this.var_name;
    }
    public String getType() {
    	return this.type;
    }

    // getters

    @Override
    public int compareTo(Entry other) {
    	if (this.getVarName().equals(other.getVarName())) {
    		return 0;
    	}
        return 1;
    }
}
