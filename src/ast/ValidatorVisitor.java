package ast;

import java.util.ArrayList;

import ast.SymbolTable.Scope;
import ast.SymbolTable.Symb;

public class ValidatorVisitor implements Visitor {

	private String result = "OK\n";
	private StringBuilder validator_msg = new StringBuilder();
	private ArrayList<SymbolTable> symbol_tables;
	
	String type;
	String curr_class;
	String curr_super_class;
	
	
	public ValidatorVisitor(ArrayList<SymbolTable> symbol_tables) {
		this.symbol_tables = symbol_tables;
	}
	
    public String getResult() {
        return result;
    }
	public String getValidatorMsg() {
		return validator_msg.toString();
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
        curr_super_class = classDecl.superName();
        for (var fieldDecl : classDecl.fields()) {
            fieldDecl.accept(this);
        }
        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
        }
        
		
	}

	@Override
	public void visit(MainClass mainClass) {
		
		
	}

	@Override
	public void visit(MethodDecl methodDecl) {
		String curr_method = methodDecl.name();
		int curr_num_of_args = 0;
		String ret_type = null;
		ArrayList<String> args = new ArrayList<String>();
		for (var formal : methodDecl.formals()) {
			formal.accept(this);
			curr_num_of_args++;
			args.add(type);
		}
		SymbolTable curr_symbol_table = returnCurrTable(curr_class);
		for (Symb symbol : curr_symbol_table.curr_scope.locals) {
			if(symbol.name.equals(curr_method)) {
				if(symbol.kind.equals(enumKind.method)) {
					ret_type = symbol.decl;
				}
			}
		}
		if(curr_super_class != null) {
			SymbolTable super_symbol_table = returnCurrTable(curr_super_class);
			for (Symb super_symbol : super_symbol_table.curr_scope.locals) {
				if(super_symbol.name.equals(curr_method)) {
					if(super_symbol.kind.equals(enumKind.method) ||  super_symbol.kind.equals(enumKind.method_extend)) {
						if(!ret_type.equals(super_symbol.decl)){
							if(!ret_type.equals("int") && !ret_type.equals("bool") && !ret_type.equals("int_array")) {
								Scope ret_type_scope = returnCurrTable(ret_type).curr_scope;
								boolean found_super_class = false;
								while(ret_type_scope.prev!=null) {
									ret_type_scope = ret_type_scope.prev;
									if((ret_type_scope.name).equals(super_symbol.decl)) {
										found_super_class = true;
										break;
									}
								}
								if(found_super_class == false) {
									result = "ERROR\n";
									validator_msg.append("override method with diffrent return type\n");
									return;
								}
							}
							else {
								result = "ERROR\n";
								validator_msg.append("override method with diffrent return type\n");
								return;
							}
						}
						else {
							Scope super_scope = super_symbol_table.curr_scope;
							if(super_symbol.kind.equals(enumKind.method_extend)) {
								while(super_scope.prev!=null) {
									super_scope = super_scope.prev;
									ArrayList<String> decl = new ArrayList<String>();
									decl.add(super_symbol.decl);
									if(null != super_scope.findSymbol(curr_method, enumKind.method, decl)) {
										break;
									}
								}
							}
							for(Scope scope : super_scope.next) {
								if(scope.type.equals(scopeType.method)) {
									if(scope.name.equals(curr_method)) {
										if(scope.num_of_args!=curr_num_of_args) {
											result = "ERROR\n";
											validator_msg.append("override method with diffrent number of arguments\n");
											return;
										}
										else {
											for(int i=0;i<curr_num_of_args;i++) {
												for( Symb local : scope.locals) {
													if(local.kind.equals(enumKind.arg)) {
														if(local.decl!=args.get(i)) {
															result = "ERROR\n";
															validator_msg.append("override method with diffrent type of arguments\n");
															return;
														}
													}
												}
											}
										}
									}
								}	
							}
						}
					}
				}
			}		
		}
        for (var varDecl : methodDecl.vardecls()) {
            varDecl.accept(this);
        }
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
            
        }
        methodDecl.ret().accept(this);
		
	}

	@Override
	public void visit(FormalArg formalArg) {
		formalArg.type().accept(this);
		if(!type.equals("int") && !type.equals("bool") && !type.equals("int_array")) {
			SymbolTable symbol_table = returnCurrTable(type);
			if(symbol_table == null) {
				result = "ERROR\n";
				validator_msg.append("type declaration of a reference type not defined\n");
			}
		}
	}

	@Override
	public void visit(VarDecl varDecl) {
		varDecl.type().accept(this);
		if(!type.equals("int") && !type.equals("bool") && !type.equals("int_array")) {
			SymbolTable symbol_table = returnCurrTable(type);
			if(symbol_table == null) {
				result = "ERROR\n";
				validator_msg.append("type declaration of a reference type not defined\n");
			}
		}
	}

	@Override
	public void visit(BlockStatement blockStatement) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(IfStatement ifStatement) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(WhileStatement whileStatement) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(SysoutStatement sysoutStatement) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(AssignStatement assignStatement) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(AssignArrayStatement assignArrayStatement) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(AndExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(LtExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(AddExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(SubtractExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(MultExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ArrayAccessExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ArrayLengthExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(MethodCallExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(IntegerLiteralExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(TrueExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(FalseExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(IdentifierExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ThisExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(NewIntArrayExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(NewObjectExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(NotExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(IntAstType t) {
		type ="int";
		
	}

	@Override
	public void visit(BoolAstType t) {
		type ="bool";
		
	}

	@Override
	public void visit(IntArrayAstType t) {
		type = "int_array";
		
	}

	@Override
	public void visit(RefType t) {
		type = t.id();
		
	}
    public SymbolTable returnCurrTable(String curr_class) {
		// init variables
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
