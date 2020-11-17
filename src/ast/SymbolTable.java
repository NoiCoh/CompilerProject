package ast;

import java.util.*;
import ast.*;

enum enumKind {
	method, var, field, empty
};

enum scopeType {
	method, type_class, statement
};

public class SymbolTable {
	int curr_level; // nesting level of current scope
	public Scope curr_scope; // topmost procedure scope

	public SymbolTable() {
		this.curr_scope = null; // new Scope();
		this.curr_level = 0;

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

	// TODO: should the store location come from the parser?
	public Symb addSymbol(String name, String decl, enumKind kind) {
		Symb new_method = new Symb(name, decl, kind);
		this.curr_scope.addSymbol(new_method);
		return new_method;
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

		public Symb(String name, String decl, enumKind kind) {
			this.name = name;
			this.decl = decl;
			this.kind = kind;

		}

		public boolean compareSymbol(Symb otherSymbol) {
			if (otherSymbol.name.equals(this.name)) {
				if (otherSymbol.kind.equals(this.kind)) {
					if (otherSymbol.decl.equals(this.decl)) {
						return true;
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
		public int frame_size, level;

		private HashMap<String, Symb> locals = new HashMap<String, Symb>(); // to locally declared objects


		public Scope(int level, scopeType type, String name) {
			this.level = level;
			this.frame_size = 0;
			this.type = type;
			this.name = name;
		}

		public void printLocals() {
			for (Map.Entry<String, Symb> symbol_entry : this.locals.entrySet()) {
				String name = symbol_entry.getKey();
				Symb var = symbol_entry.getValue();
				System.out.println(name + " : " + var.kind.toString() + " : " + var.decl);
			}
		}

		public void addSymbol(Symb sy) {
			for (Symb otherSymbol : this.locals.values()) {
				if (sy.compareSymbol(otherSymbol)) {
					return;
				}
			}
			this.locals.put(sy.name, sy);
		}

		public Symb findSymbol(String name, enumKind kind, ArrayList<String> decls) {
			for (Map.Entry<String, Symb> symbol_entry : this.locals.entrySet()) {
				String symbol_name = symbol_entry.getKey();
				Symb symbol_value = symbol_entry.getValue();
				enumKind symbol_kind = symbol_value.kind;
				String symbol_decl = symbol_value.decl;
				if (0 == name.compareTo(symbol_name)) {
					if (symbol_kind.equals(kind)) {
						if (decls.contains(symbol_decl)) {
							return symbol_value;
						}
					}
				}
			}
			return null;
		}
	}
}