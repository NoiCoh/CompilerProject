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
	String curr_method;
	boolean method_call=false;//if we are calling a function-method call is true
	boolean length_call=false;
	boolean array_call=false;
	int countErrors=0;//for test
	
	
	public ValidatorVisitor(ArrayList<SymbolTable> symbol_tables) {
		this.symbol_tables = symbol_tables;
	}
	
    public String getResult() {
        return result;
    }
	public String getValidatorMsg() {
		return validator_msg.toString();
	}
    private void visitBinaryExpr(BinaryExpr e,String infixSymbol) {//ex21
        e.e1().accept(this);
        String e1_type = type;
        e.e2().accept(this);
        String e2_type = type;
        if(infixSymbol.equals("&&")) {
        	if(!(e1_type.equals("bool") && e2_type.equals("bool"))) {
        		result = "ERROR\n";
				validator_msg.append("The arguments to the predefined operators are of the incorrect type.\n");
        	}
        	type = "bool";
        }
        else if(infixSymbol.equals("<")) {
        	if(!(e1_type.equals("int") && e2_type.equals("int"))) {
        		result = "ERROR\n";
				validator_msg.append("The arguments to the predefined operators are of the incorrect type.\n");
        	}
        	type ="bool";
        }
        else {
        	if(!(e1_type.equals("int") && e2_type.equals("int"))) {
        		result = "ERROR\n";
				validator_msg.append("The arguments to the predefined operators are of the incorrect type.\n");
        	}
        	type = "int";
        }
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
		//check sys args in mainClass
		mainClass.name();
	    mainClass.argsName();
	    mainClass.mainStatement().accept(this);
	}

	@Override
	public void visit(MethodDecl methodDecl) {
		curr_method = methodDecl.name();
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
				validator_msg.append("type declaration "+type+ " of a reference type not defined\n");
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
				validator_msg.append("type declaration "+type+ " of a reference type not defined\n");
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
		assignStatement.lv();
		assignStatement.rv().accept(this);
		
	}

	@Override
	public void visit(AssignArrayStatement assignArrayStatement) {
		//ex23
		Boolean found_var_in_method = false;
		SymbolTable curr_symbol_table = returnCurrTable(curr_class);
		if(curr_symbol_table != null) {
			Scope curr_scope = curr_symbol_table.curr_scope;
			Scope scopeMethod = findCurrMethodScope(curr_scope);
			if (scopeMethod!=null) {
				for (Symb local : scopeMethod.locals) {
					if (local.name.equals(assignArrayStatement.lv())) {
						found_var_in_method = true;
						callArrayVarValidation(local);
						break;
					}
				}
			}
			if(found_var_in_method == false) {
				for (Symb local : curr_scope.locals) {
					if (local.name.equals(assignArrayStatement.lv())) {
						callArrayFieldValidation(local);
						break;
					}
				}
			}
		}
		assignArrayStatement.index().accept(this);
		if(!type.equals("int")) {
			result = "ERROR\n";
			validator_msg.append("array index is not in type int\n");
		}
        assignArrayStatement.rv().accept(this);
        if(!type.equals("int")) {
			result = "ERROR\n";
			validator_msg.append("array assign is not in type int\n");
		}
		
	}

	@Override
	public void visit(AndExpr e) {
		visitBinaryExpr(e,"&&");
		
	}

	@Override
	public void visit(LtExpr e) {
		visitBinaryExpr(e,"<");
		
	}

	@Override
	public void visit(AddExpr e) {
		visitBinaryExpr(e,"+");
		
	}

	@Override
	public void visit(SubtractExpr e) {
		visitBinaryExpr(e,"-");
		
	}

	@Override
	public void visit(MultExpr e) {
		visitBinaryExpr(e,"*");
		
	}

	@Override
	public void visit(ArrayAccessExpr e) {
		array_call = true;
		e.arrayExpr().accept(this);
		array_call = false;
		e.indexExpr().accept(this);	
		if(!type.equals("int")) {
			result = "ERROR\n";
			validator_msg.append("array index is not in type int\n");
		}
	}

	@Override
	public void visit(ArrayLengthExpr e) {
		length_call = true;
		e.arrayExpr().accept(this);
		length_call = false;
		type = "int";
	}

	@Override
	public void visit(MethodCallExpr e) {
		method_call=true;
		e.ownerExpr().accept(this);
		//System.out.print(e.methodId());
		//System.out.print(e.ownerExpr());
		method_call=false;
		
		
	}

	@Override
	public void visit(IntegerLiteralExpr e) {
		e.num();
		type = "int";
		
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
		Boolean found_var_in_method = false;
		SymbolTable curr_symbol_table = returnCurrTable(curr_class);
		if(curr_symbol_table != null) {
			Scope curr_scope = curr_symbol_table.curr_scope;
			Scope scopeMethod = findCurrMethodScope(curr_scope);
			if (scopeMethod!=null) {
				for (Symb local : scopeMethod.locals) {
					if (local.name.equals(e.id())) {
						found_var_in_method = true;
						if (method_call==true) { //ex10
							//find the static type of object inside current function
							methodCallValidation(local);
						}
						else if(length_call==true){//ex13
							lengthArrayVarValidation(local);
						}
						else if(array_call==true) {//ex22
							callArrayVarValidation(local);
						}
						else {
							type = local.decl;
						}
						break;
					}
				}
				//need to do-if there is no such var or arg in the function, then we need to check if there is such a field in the class
				
				//or the class father..
				
				if(found_var_in_method == false) {
					for (Symb local : curr_scope.locals) {
						if (local.name.equals(e.id())) {
							if(length_call==true) {//ex13
								lengthArrayFieldValidation(local);
							}
							else if(array_call==true) {//ex22
								callArrayFieldValidation(local);
							}
							else {
								type = local.decl;
							}
							break;
						}
					}
				}
			}
		}
	}
	

	public void methodCallValidation(Symb local) {
		if(local.kind.equals(enumKind.arg) || local.kind.equals(enumKind.var)) {
			if(local.decl.equals("int") || local.decl.equals("bool") || local.decl.equals("int_array")) {
				//countErrors++;
				//System.out.println(countErrors);
				result = "ERROR\n";
				validator_msg.append("the static type of the object is not a reference type\n");
			}
		}
	}
	
	public void lengthArrayVarValidation(Symb local) {
		if(local.kind.equals(enumKind.var)) {
			if(!local.decl.equals("int_array")) {
				result = "ERROR\n";
				validator_msg.append("The static type of the object on which length invoked is int[]\n");
			}
		}
	}
	
	public void lengthArrayFieldValidation(Symb local) {
		if(local.kind.equals(enumKind.field) || local.kind.equals(enumKind.field_extend)) {
			if(!local.decl.equals("int_array")) {
				result = "ERROR\n";
				validator_msg.append("The static type of the object on which length invoked is int[]\n");
			}
		}
	}
	
	public void callArrayVarValidation(Symb local) {
		if(local.kind.equals(enumKind.var)) {
			if(!local.decl.equals("int_array")) {
				result = "ERROR\n";
				validator_msg.append("array access/assign is not of type int[]\n");
			}
		}
	}
	
	public void callArrayFieldValidation(Symb local) {
		if(local.kind.equals(enumKind.field) || local.kind.equals(enumKind.field_extend)) {
			if(!local.decl.equals("int_array")) {
				result = "ERROR\n";
				validator_msg.append("array access/assign is not of type int[]\n");
			}
		}
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
		//ex9
		SymbolTable curr_symbol_table = returnCurrTable(e.classId());
		if (curr_symbol_table==null) {
			result = "ERROR\n";
			validator_msg.append("new object "+e.classId()+" that is defined somewhere in the file \n");
			return;
		}
		else {
			if (!curr_symbol_table.curr_scope.type.equals(scopeType.type_class)) {
				result = "ERROR\n";
				validator_msg.append("new object "+e.classId()+" that is defined somewhere in the file \n");
				return;
			}
		}	
	}

	@Override
	public void visit(NotExpr e) {
		e.e().accept(this);
		String e_type = type;
		//ex21
    	if(!(e_type.equals("bool"))) {
    		result = "ERROR\n";
			validator_msg.append("The argument to the predefined operator is of the incorrect type.\n");
    	}
    	type = "bool";
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
    
    public Scope findCurrMethodScope(Scope class_scope) {
		Scope scopeMethod=null;
		for(Scope scope : class_scope.next) {
			if(scope.type.equals(scopeType.method)) {
				if(scope.name.equals(curr_method)) {
					scopeMethod=scope;
					break;
				}
			}
		}
		return scopeMethod;
    }

}
