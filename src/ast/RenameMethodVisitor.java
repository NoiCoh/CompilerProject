package ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

import ast.SymbolTable.*;

public class RenameMethodVisitor implements Visitor {

	ClassDecl curr_class;
	MethodDecl curr_method;
	MethodDecl des_method;
	ClassDecl des_class;
	
	boolean foundClass = false;
	String callVar;
	boolean callToFunc= false;
	boolean thiscall= false;
	boolean firstWalk =  true; 
	
	String return_val = "";
	String originalName;
	String newName;
	Integer line;
	ArrayList<SymbolTable> symbol_tables;
	
	ArrayList<String> extends_scopes = new ArrayList<String>();
	
	
	public RenameMethodVisitor(String originalName, String newName ,Integer line, ArrayList<SymbolTable> symbol_tables) {
		this.originalName = originalName;
		this.newName = newName;
		this.line = line;
		this.symbol_tables = symbol_tables;
	}

    private void visitBinaryExpr(BinaryExpr e, String infixSymbol) {
        e.e1().accept(this);
        e.e2().accept(this);
    }


    @Override
    public void visit(Program program) {
        for (ClassDecl classdecl : program.classDecls()) {
            classdecl.accept(this);
        }
        program.mainClass().accept(this);
        firstWalk = false;
        for (ClassDecl classdecl : program.classDecls()) {
            classdecl.accept(this);
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {
    	curr_class = classDecl;
        	
        if (classDecl.superName() != null) {
            classDecl.superName();
        }
        for (var fieldDecl : classDecl.fields()) {
            fieldDecl.accept(this);
        }
        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
        }
    }
   


    @Override
    public void visit(MainClass mainClass) {
        mainClass.name();
        mainClass.argsName();
        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
    	curr_method = methodDecl;
    	if(0 == methodDecl.name().compareTo(originalName)) {
    		if(firstWalk == true) {
    			if(methodDecl.lineNumber.equals(this.line)) {
    				des_method = curr_method;
    				des_class = curr_class;
					for (SymbolTable t : symbol_tables) {
						//System.out.println("scope name: " + symbol_table.curr_scope.name + "scope type: " + symbol_table.curr_scope.type);
						if(0 == t.curr_scope.type.compareTo(scopeType.type_class)) {
							if(0 == (t.curr_scope.name).compareTo(des_class.name())) {
								Scope k = t.curr_scope;
								
								ArrayList<String> decl = new ArrayList<String>();
								methodDecl.returnType().accept(this);
								decl.add(return_val);
								Scope last_seen = k;
								
								while(k.prev!=null) {
									k = k.prev;
									if(null != k.findSymbol(des_method.name(), enumKind.method, decl)) {
										last_seen = k;
									}
								}
								// Create a stack for DFS 
						    	Stack<Scope> stack = new Stack<>(); 
						    	Scope scope = last_seen;
						    	
						        stack.push(scope); 
						        while(stack.empty() == false) { 
						            // Pop a vertex from stack and print it 
						        	
						        	scope = stack.peek(); 
						        	if(0 == scope.type.compareTo(scopeType.type_class)) {
						        		extends_scopes.add(scope.name);
						        	}
						        	stack.pop();
						        	
						            Iterator<Scope> itr = scope.next.iterator(); 
						            while (itr.hasNext())  { 
						            	Scope v = itr.next(); 
						                stack.push(v); 
						            }
						        } 
							}	
						}
					}
					methodDecl.setName(newName);
				}
				//System.out.println("extends_scopes: " +extends_scopes);
			}
    		else {
    			if(extends_scopes.contains(curr_class.name())) {
    				methodDecl.setName(newName);
    			}
    		}
    	}
    	

        for (var formal : methodDecl.formals()) {
            formal.accept(this);
        }

        for (var varDecl : methodDecl.vardecls()) {
            varDecl.accept(this);
        }
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
            foundClass = false;
        }
        methodDecl.ret().accept(this);
    }

    @Override
    public void visit(FormalArg formalArg) {
    	formalArg.type().accept(this);  
    }

    @Override
    public void visit(VarDecl varDecl) {
    	varDecl.type().accept(this); 
    	foundClass = false;
    }

    @Override
    public void visit(BlockStatement blockStatement) {
        for (var s : blockStatement.statements()) {
            s.accept(this);
        }
    }

    @Override
    public void visit(IfStatement ifStatement) {
        ifStatement.cond().accept(this);
        ifStatement.thencase().accept(this);
        ifStatement.elsecase().accept(this);
    }

    @Override
    public void visit(WhileStatement whileStatement) {
        whileStatement.cond().accept(this);
        whileStatement.body().accept(this);
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        sysoutStatement.arg().accept(this);
    }

    @Override
    public void visit(AssignStatement assignStatement) {
    	assignStatement.rv().accept(this);
    	
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        assignArrayStatement.lv();
        assignArrayStatement.index().accept(this);
        assignArrayStatement.rv().accept(this);
    }

    @Override
    public void visit(AndExpr e) {
        visitBinaryExpr(e, "&&");
    }

    @Override
    public void visit(LtExpr e) {
        visitBinaryExpr(e, "<");;
    }

    @Override
    public void visit(AddExpr e) {
        visitBinaryExpr(e, "+");;
    }

    @Override
    public void visit(SubtractExpr e) {
        visitBinaryExpr(e, "-");
    }

    @Override
    public void visit(MultExpr e) {
        visitBinaryExpr(e, "*");
    }

    @Override
    public void visit(ArrayAccessExpr e) {
        e.arrayExpr().accept(this);
        e.indexExpr().accept(this);
    }

    @Override
    public void visit(ArrayLengthExpr e) {
        e.arrayExpr().accept(this);
    }

    @Override
    public void visit(MethodCallExpr e) {

        e.ownerExpr().accept(this);
        if(0 == e.methodId().compareTo(originalName) && true == thiscall) {
        	if (extends_scopes.contains(curr_class.name())){
        		callToFunc=false;
        		//System.out.println(e.methodId());
        		e.setMethodId(newName);
        		//System.out.println(e.methodId());
        	}
        }
        if(foundClass == true) {
        	foundClass = false;
        	if(0 == e.methodId().compareTo(originalName)) {
        		e.setMethodId(newName);
        	}
        }
        for (Expr arg : e.actuals()) {
            arg.accept(this);
        }
        thiscall = false;
    }

    @Override
    public void visit(IntegerLiteralExpr e) {
        e.num();
    }

    @Override
    public void visit(IdentifierExpr e) {
    	//init variables
    	Scope curr_class_scope = null;
    	SymbolTable curr_symbol_table = null;
    	Symb e_symbol = null;
    	
    	// find the symbol table of the current class.
    	for (SymbolTable symbol_table : symbol_tables) {
    		curr_class_scope = symbol_table.curr_scope;
    		if(curr_class_scope.name.equals(curr_class.name())) {
    			curr_symbol_table = symbol_table;
    			break;
    		}
    	}
    	// find if e defined in method
    	if(curr_symbol_table != null) {
    		Scope curr_method_scope;
    		if(false == curr_method.getHasChanged()) {
    			curr_method_scope = curr_symbol_table.findScope(curr_method.name(),scopeType.method);
    		}
    		else {
    			curr_method_scope = curr_symbol_table.findScope(originalName,scopeType.method);
    		}
    		if(curr_method_scope != null) {
    			e_symbol = curr_method_scope.findSymbol(e.id(), enumKind.var, extends_scopes);
        		if(e_symbol != null) {
        			foundClass = true;
        			return ;
        		}
    		}
    		// find if e defined as global var in curr class or super classes
        	while (curr_class_scope != null) {
        		e_symbol = curr_class_scope.findSymbol(e.id(), enumKind.field, extends_scopes);
    			if(e_symbol != null) {
    				foundClass = true;
    				break;
    			}
    			else {
    				curr_class_scope = curr_class_scope.prev;
    			}
    		}
    	}		
    }

    public void visit(ThisExpr e) {
    	thiscall=true;
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        e.lengthExpr().accept(this);
    }

    @Override
    public void visit(NewObjectExpr e) {
	    if (des_class!=null){
	    	if(extends_scopes.contains(e.classId())) {
	    		foundClass = true;
	    	}
	        //System.out.println(e.classId());
	    }
    }
    @Override
    public void visit(RefType t) {
    	return_val = t.id();
    	if(extends_scopes.contains(t.id())) {
    		foundClass = true;
    	}
    }
    
    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
    }

    @Override
    public void visit(IntAstType t) {
    	return_val = "int";
    }

    @Override
    public void visit(BoolAstType t) {
    	return_val = "bool";
    }

    @Override
    public void visit(IntArrayAstType t) {
    	return_val = "int_array";
    }

    @Override
    public void visit(TrueExpr e) {
    }

    @Override
    public void visit(FalseExpr e) {
    }
}