package ast;

import java.util.ArrayList;

import ast.SymbolTable.Scope;
import ast.SymbolTable.Symb;

public class RenameVarVisitor implements Visitor {
	
	String originalName;
	String newName;
	Integer lineNumber;
	ArrayList<SymbolTable> symbol_tables;
	
	String curr_class  = "";
	ArrayList<String> des_class = new ArrayList<String>();
	String curr_method = "";
	String des_method  = "";
	
	Boolean insideMethod = false;
	Boolean DefineInMethod = null;
	ArrayList<String> varType = new ArrayList<String>();
	
	
	public RenameVarVisitor(String originalName, String newName ,Integer lineNumber, ArrayList<SymbolTable> symbol_tables) {
		this.originalName = originalName;
		this.newName = newName;
		this.lineNumber = lineNumber;
		this.symbol_tables = symbol_tables;
	}

    @Override
    public void visit(Program program) {
        program.mainClass().accept(this);
        for (ClassDecl classdecl : program.classDecls()) {
            classdecl.accept(this);
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {
    	curr_class = classDecl.name();
        //System.out.println("curr_class: " + curr_class);
        for (var fieldDecl : classDecl.fields()) {
        	curr_method = "";
            fieldDecl.accept(this);
        }
        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
        }
    }

    @Override
    public void visit(MainClass mainClass) {
    	curr_class = mainClass.name();
        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
    	
    	insideMethod = true;
    	
    	curr_method = methodDecl.name();
    	
    	
    	//System.out.println("curr_method: " + curr_method);
        
        for (var formal : methodDecl.formals()) {
            formal.accept(this);
        }

        for (var varDecl : methodDecl.vardecls()) {
            varDecl.accept(this);
        }
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }

        methodDecl.ret().accept(this);
        
        insideMethod = false;

    }

    @Override
    public void visit(FormalArg formalArg) {
    	if(0 == formalArg.name().compareTo(originalName)) {
    		if(formalArg.lineNumber.equals(this.lineNumber)) {
    			DefineInMethod = true;
    			des_method = curr_method;
    			des_class.add(curr_class);
    			formalArg.setName(newName);
    		}
    	}
    }

    @Override
    public void visit(VarDecl varDecl) {
    	//System.out.println("varDecl.name(): " + varDecl.name() + " line: " + varDecl.lineNumber);
    	if(0 == varDecl.name().compareTo(originalName)) {
    		if(varDecl.lineNumber.equals(this.lineNumber)) {
    			des_class.add(curr_class);
    			//System.out.println("des_class: " + des_class);
    			if(curr_method.isEmpty()) {
    				DefineInMethod = false;
    				// add all the inheritance classes
    				for (SymbolTable symbol_table : symbol_tables) {
    					//System.out.println("scope name: " + symbol_table.curr_scope.name + "scope type: " + symbol_table.curr_scope.type);
    					if(0 == symbol_table.curr_scope.type.compareTo(scopeType.type_class)) {
    						Scope prev_scope = symbol_table.curr_scope.prev;
    						if(prev_scope != null) {
    							if(des_class.contains(prev_scope.name)) {
        							des_class.add(symbol_table.curr_scope.name);
    							}
    						}	
    					}
    				}
    			}
    			else {
    				DefineInMethod = true;
    			}
    			des_method = curr_method;
    			varDecl.type().accept(this);
    			varDecl.setName(newName);
    		}
    	}        
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
    	//System.out.println("assignStatement.lv(): " + assignStatement.lv());
    	if (0 == (assignStatement.lv()).compareTo(originalName)) {
    		if(des_class.contains(curr_class)) {
    			if(DefineInMethod == false) {
    				if(insideMethod == true) {
    					SymbolTable curr_symbol_table = returnCurrTable();
    					Symb e_symbol = null;
    					
    			    	// find if originalName defined in method
    			    	if(curr_symbol_table != null) {
    			    		Scope curr_method_scope = curr_symbol_table.findScope(curr_method,scopeType.method);
    			    		if(curr_method_scope != null) {
    			    			e_symbol = curr_method_scope.findSymbol(originalName, enumKind.var, varType);
    			        		if(e_symbol == null) {
    			        			assignStatement.setLv(newName);
    			        		}
    			    		}
    			    	}
    				}
    				else {
    					assignStatement.setLv(newName);
    				}
    			}
    			else if (DefineInMethod == true){
    				if(0 == curr_method.compareTo(des_method)) {
    					assignStatement.setLv(newName);
    	    		}
    			}
    		}
    	}
    	assignStatement.rv().accept(this);
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
    	if (0 == (assignArrayStatement.lv()).compareTo(originalName)) {
    		if(des_class.contains(curr_class)) {
    			if(DefineInMethod == false) {
    				if(insideMethod == true) {
    					SymbolTable curr_symbol_table = returnCurrTable();
    					Symb e_symbol = null;
    			    	// find if originalName defined in method
    			    	if(curr_symbol_table != null) {
    			    		Scope curr_method_scope = curr_symbol_table.findScope(curr_method,scopeType.method);
    			    		if(curr_method_scope != null) {
    			    			e_symbol = curr_method_scope.findSymbol(originalName, enumKind.var, varType);
    			        		if(e_symbol == null) {
    			        			assignArrayStatement.setLv(newName);
    			        		}
    			    		}
    			    	}
    				}
    				else {
    					assignArrayStatement.setLv(newName);
    				}
    			}
    			else if (DefineInMethod == true){
    				if(0 == curr_method.compareTo(des_method)) {
    					assignArrayStatement.setLv(newName);
    	    		}
    			}
    		}
    	}
        assignArrayStatement.index().accept(this);
        assignArrayStatement.rv().accept(this);
    }

    @Override
    public void visit(AndExpr e) { 
    	e.e1().accept(this);
    	e.e2().accept(this);
    }

    @Override
    public void visit(LtExpr e) {  
    	e.e1().accept(this);
    	e.e2().accept(this);
    }

    @Override
    public void visit(AddExpr e) {  
    	e.e1().accept(this);
    	e.e2().accept(this);
    }

    @Override
    public void visit(SubtractExpr e) {
    	e.e1().accept(this);
    	e.e2().accept(this);
    }

    @Override
    public void visit(MultExpr e) {
    	e.e1().accept(this);
    	e.e2().accept(this);
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
    	if(des_class.contains(curr_class)) {
    		e.ownerExpr().accept(this);
    		for (Expr arg : e.actuals()) {
                arg.accept(this);
            }
    	}    
    }

    @Override
    public void visit(IntegerLiteralExpr e) {
        e.num();
    }

    @Override
    public void visit(TrueExpr e) {
    }

    @Override
    public void visit(FalseExpr e) {
    }

    @Override
    public void visit(IdentifierExpr e) {
    	if (0 == (e.id()).compareTo(originalName)) {
    		if(des_class.contains(curr_class)) {
    			if(DefineInMethod == false) {
    				if(insideMethod == true) {
    					SymbolTable curr_symbol_table = returnCurrTable();
    					Symb e_symbol = null;
    					
    			    	// find if originalName defined in method
    			    	if(curr_symbol_table != null) {
    			    		Scope curr_method_scope = curr_symbol_table.findScope(curr_method,scopeType.method);
    			    		if(curr_method_scope != null) {
    			    			e_symbol = curr_method_scope.findSymbol(originalName, enumKind.var, varType);
    			        		if(e_symbol == null) {
    			        			e.setId(newName);
    			        		}
    			    		}
    			    	}
    				}
    				else {
    					e.setId(newName);
    				}
    			}
    			else if (DefineInMethod == true){
    				if(0 == curr_method.compareTo(des_method)) {
    					e.setId(newName);
    	    		}
    			}
    		}
    	}
    }

    public void visit(ThisExpr e) {
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        e.lengthExpr().accept(this);
    }

    @Override
    public void visit(NewObjectExpr e) {
    }
    
    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
    }

    @Override
    public void visit(IntAstType t) {
    	varType.add("int");
    }

    @Override
    public void visit(BoolAstType t) {
    	varType.add("bool");
    }

    @Override
    public void visit(IntArrayAstType t) {
    	varType.add("int_array");
    }

    @Override
    public void visit(RefType t) {
    	varType.add(t.id());
    }
    public SymbolTable returnCurrTable() {
		//init variables
    	Scope curr_class_scope = null;
    	SymbolTable curr_symbol_table = null;
    	
    	// find the symbol table of the current class.
    	for (SymbolTable symbol_table : symbol_tables) {
    		curr_class_scope = symbol_table.curr_scope;
    		if(curr_class_scope.name.equals(curr_class)) {
    			curr_symbol_table = symbol_table;
    			break;
    		}
    	}
    	return curr_symbol_table;
    }
}