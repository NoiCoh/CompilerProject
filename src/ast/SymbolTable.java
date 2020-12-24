package ast;

import java.util.*;
import ast.*;

enum enumKind {
	method, var, field, arg, empty, method_extend, field_extend
};

enum scopeType {
	method, type_class, statement
};

public class SymbolTable {
	int curr_level; // nesting level of current scope
	public Scope curr_scope; // topmost procedure scope
	ArrayList<enumKind> methodsKind = new ArrayList<enumKind>();
	ArrayList<enumKind> fieldsKind = new ArrayList<enumKind>();
	
	public SymbolTable() {
		this.curr_scope = null; // new Scope();
		this.curr_level = 0;
		this.methodsKind.add(enumKind.method_extend);
		this.methodsKind.add(enumKind.method);
		this.fieldsKind.add(enumKind.field);
		this.fieldsKind.add(enumKind.field_extend);

	}

	public Scope findScope(String scope_name, scopeType type) {
		// Create a stack for DFS
		Stack<Scope> stack = new Stack<>();
		Scope scope = this.curr_scope;

		stack.push(scope);
		while (stack.empty() == false) {
			// Pop a vertex from stack and print it
			scope = stack.peek();
			if (0 == scope_name.compareTo(scope.name)) {
				return scope;
			}
			stack.pop();
			Iterator<Scope> itr = scope.next.iterator();
			while (itr.hasNext()) {
				Scope v = itr.next();
				if (0 == v.type.compareTo(type)) {
					stack.push(v);
				}
			}
		}
		return null;
	}

	public void printTable() {
		// Create a stack for DFS
		Stack<Scope> stack = new Stack<>();
		Scope scope = this.curr_scope;

		// Push the current source node
		stack.push(scope);

		while (stack.empty() == false) {
			// Pop a vertex from stack and print it
			scope = stack.peek();
			System.out.println("scope_name: " + scope.name + " Scope_type: " + scope.type.toString());
			if(scope.type == scopeType.type_class) {
				System.out.println("num of methods: " + scope.num_of_methods + " num of fields: " + scope.num_of_fields);
			}
			if(scope.type == scopeType.method) {
				System.out.println("num of args: " + scope.num_of_args);
			}
			scope.printLocals();
			stack.pop();

			Iterator<Scope> itr = scope.next.iterator();

			while (itr.hasNext()) {
				Scope v = itr.next();
				stack.push(v);
			}
		}
		System.out.println();
		System.out.println();
	}

	// open a new scope and make it the current scope (topScope)
	public void openScope(scopeType type, String name) {
		Scope new_scope = new Scope(this.curr_level++, type, name);
		new_scope.prev = curr_scope;
		if (curr_scope != null) {
			curr_scope.next.add(new_scope);
		}
		this.curr_scope = new_scope;
	}

	// close the current scope
	public void closeScope() {
		this.curr_scope = curr_scope.prev;
		this.curr_level--;
	}

	public Symb addSymbol(String name, String decl, enumKind kind, String extendFrom, int vtableindex) {
		Symb new_method = new Symb(name, decl, kind,extendFrom, vtableindex);
		if(this.curr_scope.addSymbol(new_method)) {
			return new_method;
		}
		return null;
	}

	// search the name in all open scopes and return its object node
	public Symb findSymbol(String name, enumKind kind, ArrayList<String> decls) {
		Scope scope = this.curr_scope;
		Symb symbol;
		while (scope != null) {
			symbol = scope.findSymbol(name, kind, decls);
			if (symbol != null) {
				return symbol;
			}
			scope = scope.prev;
		}
		return null;
	}

	public class Symb {
		String name;
		enumKind kind;
		String decl;
		String extendFrom;
		int vtableindex;


		public Symb(String name, String decl, enumKind kind, String extendFrom, int vtableindex) {
			this.name = name;
			this.decl = decl;
			this.kind = kind;
			this.extendFrom = extendFrom;
			this.vtableindex = vtableindex;
		}

		public boolean compareSymbol(Symb otherSymbol) {
			if (otherSymbol.name.equals(this.name)) {
				if (otherSymbol.decl.equals(this.decl)) {
					if(this.kind == enumKind.method || this.kind == enumKind.method_extend) {
						if (otherSymbol.kind.equals(enumKind.method_extend) || otherSymbol.kind.equals(enumKind.method)) {
							return true;
						}
					}
					else if(this.kind == enumKind.var || this.kind == enumKind.arg) {
						if (otherSymbol.kind.equals(enumKind.var) || otherSymbol.kind.equals(enumKind.arg)) {
							return true;
						}
					}
					else if(this.kind == enumKind.field || this.kind == enumKind.field_extend) {
						if (otherSymbol.kind.equals(enumKind.field) || otherSymbol.kind.equals(enumKind.field_extend)) {
							return true;
						}
					}
				}
			}
			return false;
		}
	}

	public class Scope {
		public Scope prev;
		public ArrayList<Scope> next = new ArrayList<Scope>();
		public scopeType type;
		public String name;
		public int frame_size, level, num_of_methods, num_of_fields, num_of_args, size_of_object;
		public ArrayList<Symb> locals = new ArrayList<Symb>(); // to locally declared objects
		ArrayList<enumKind> methodsKind = new ArrayList<enumKind>();
		ArrayList<enumKind> fieldsKind = new ArrayList<enumKind>();


		public Scope(int level, scopeType type, String name) {
			this.level = level;
			this.frame_size = 0;
			this.type = type;
			this.name = name;
			this.num_of_methods = 0;
			this.num_of_fields = 0;
			this.num_of_args = 0;
			this.methodsKind.add(enumKind.method_extend);
			this.methodsKind.add(enumKind.method);
			this.fieldsKind.add(enumKind.field);
			this.fieldsKind.add(enumKind.field_extend);
		}
		
		public void setNumOfMethods(int num) {
			this.num_of_methods = num;
		}
		public void setNumOfFields(int num) {
			this.num_of_fields = num;
		}
		public void setNumOfArgs(int num) {
			this.num_of_args = num;
		}
		public void setSizeOfObject(int num) {
			this.size_of_object = num;
		}

		public void printLocals() {
			for (Symb symbol_entry : this.locals) {
				String name = symbol_entry.name;
				System.out.println(name + " : " + symbol_entry.kind.toString() + " : " + symbol_entry.decl+ " : " + symbol_entry.extendFrom +" : " + symbol_entry.vtableindex);
			}
		}

		public boolean addSymbol(Symb sy) {
			for (Symb otherSymbol : this.locals) {
				if (sy.compareSymbol(otherSymbol)) {
					return false;
				}
			}
			this.locals.add(sy);
			return true;
		}
		
		public String findSymbolType(String name, enumKind kind) {
			for (Symb symbol_entry : this.locals) {
				String symbol_name = symbol_entry.name;
				enumKind symbol_kind = symbol_entry.kind;
				if (0 == name.compareTo(symbol_name)) {
					if (symbol_kind.equals(kind)) {
						return symbol_entry.decl;
					}
				}
			}
			return "";
		}

		public Symb findSymbol(String name, enumKind kind, ArrayList<String> decls) {
			for (Symb symbol_entry : this.locals) {
				String symbol_name = symbol_entry.name;
				enumKind symbol_kind = symbol_entry.kind;
				String symbol_decl = symbol_entry.decl;
				if (0 == name.compareTo(symbol_name)) {
					if (decls.contains(symbol_decl)) {
						if (this.methodsKind.contains(kind) && this.methodsKind.contains(symbol_kind)) {
							return symbol_entry;
						}
						else if(this.fieldsKind.contains(kind) && this.fieldsKind.contains(symbol_kind)) {
							return symbol_entry;
						}
					}
				}
			}
			return null;
		}
		public Symb findSymbol(String name, ArrayList<enumKind> kind) {
			for (Symb symbol_entry : this.locals) {
				String symbol_name = symbol_entry.name;
				enumKind symbol_kind = symbol_entry.kind;
				if (0 == name.compareTo(symbol_name)) {
					if (kind.contains(symbol_kind)) {
						return symbol_entry;
					}
				}
			}
			return null;
		}
	}
}
