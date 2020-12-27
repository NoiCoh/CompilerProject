package ast;

import java.util.ArrayList;

import ast.SymbolTable.Scope;
import ast.SymbolTable.Symb;

public class ValidatorVisitor implements Visitor {

	private String result = "OK\n";
	private StringBuilder validator_msg = new StringBuilder();
	private ArrayList<SymbolTable> symbol_tables;
	String type;
	String var_decl;
	String curr_class;
	String curr_super_class;
	String class_call;
	String curr_method;
	boolean while_or_if=false;
	boolean method_call=false;//if we are calling a function-method call is true
	boolean length_call=false;
	boolean array_call=false;
	String method_call_name;
	int countErrors=0;//for test
	ArrayList<String> method_call_args = new ArrayList<String>();
	int num_of_actuals;
	boolean isActual=false;
	boolean is_boolean_exp=false;
	boolean is_exp=false;
	String call_var_class;//decl of the var e in e.f() 
	String methodcall_type;
	boolean is_assignStatement=false;
	String lv_decl;
	String lv_name;
	boolean is_thisexp=false;
	boolean is_newIntArray=false;
	boolean is_error=false;
	
	
	
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
				validator_msg.append("in class: " + curr_class+" in method: "+curr_method+" the arguments to the predefined operators are of the incorrect type.\n");
        	}
        	else {
        		is_boolean_exp=true;
        	}
        	type = "bool";
        }
        else if(infixSymbol.equals("<")) {
        	if(!(e1_type.equals("int") && e2_type.equals("int"))) {
        		result = "ERROR\n";
				validator_msg.append("in class: " + curr_class+" in method: "+curr_method+" the arguments to the predefined operators are of the incorrect type.\n");
        	}
        	else {
        		is_boolean_exp=true;
        	}
        	type ="bool";
        }
        else {
        	if(!(e1_type.equals("int") && e2_type.equals("int"))) {
        		result = "ERROR\n";
				validator_msg.append("in class: " + curr_class+" in method: "+curr_method+" the arguments to the predefined operators are of the incorrect type.\n");
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
		curr_class="main";
		curr_method ="main";
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
			curr_num_of_args++;
			formal.accept(this);
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
									validator_msg.append("in class: " + curr_class+" in method: "+curr_method+" override method with diffrent return type\n");
									return;
								}
							}
							else {
								result = "ERROR\n";
								validator_msg.append("in class: " + curr_class+" in method: "+curr_method+" override method with diffrent return type\n");
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
							check_args(super_scope,curr_num_of_args,args,curr_method);
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
        methodDecl.ret().accept(this);//ex18
       
		if (!ret_type.equals(type)) {
			if (!(type.equals("bool")||type.equals("int")||type.equals("int_array"))) {
				if(!check_assignment_subtyping(ret_type)){
					result = "ERROR\n";
					validator_msg.append("in class: " + curr_class+" in method: "+curr_method+" different return type"+ret_type+" not equal to "+type+"\n");
				}
			}
			else {
				result = "ERROR\n";
				validator_msg.append("in class: " + curr_class+" in method: "+curr_method+" different return type"+ret_type+" not equal to "+type+"\n");
			}
		}
	}
	
	@Override
	public void visit(FormalArg formalArg) {
		formalArg.type().accept(this);
		if(!type.equals("int") && !type.equals("bool") && !type.equals("int_array")) {
			SymbolTable symbol_table = returnCurrTable(type);
			if(symbol_table == null) {
				result = "ERROR\n";
				validator_msg.append("in class: " + curr_class+" in method: "+curr_method+" type declaration "+type+ " of a reference type not defined\n");
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
				validator_msg.append("in class: " + curr_class+" in method: "+curr_method+" type declaration "+type+ " of a reference type not defined\n");
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
		while_or_if=true;
        ifStatement.cond().accept(this);
        if(!type.equals("bool")) {
        	result = "ERROR\n";
			validator_msg.append("conditon in if statment is not in type bool\n");
        }
        while_or_if=false;
        ifStatement.thencase().accept(this);
        if(ifStatement.elsecase()!=null) {
        	ifStatement.elsecase().accept(this);
        }
        
	}

	@Override
	public void visit(WhileStatement whileStatement) {
		while_or_if=true;
        whileStatement.cond().accept(this);
        if(!type.equals("bool")) {
        	result = "ERROR\n";
			validator_msg.append("cond in while statment is not in type bool\n");
        }
        while_or_if=false;
        whileStatement.body().accept(this);
	}

	@Override
	public void visit(SysoutStatement sysoutStatement) {
		sysoutStatement.arg().accept(this);
		if(type != null) {
	        if(!type.equals("int")) {
				result = "ERROR\n";
				validator_msg.append("in class: " + curr_class+" in method: "+curr_method+" the argument of System.out.println is not in type int\n");;
			}
		}
	}

	@Override
	public void visit(AssignStatement assignStatement) {
		is_assignStatement=true;
		lv_name=assignStatement.lv();
		boolean found_var_in_class=false;
		boolean found_var_in_method = false;
		SymbolTable curr_symbol_table = returnCurrTable(curr_class);
		if(curr_symbol_table != null) {
			Scope curr_scope = curr_symbol_table.curr_scope;
			Scope scopeMethod = findCurrMethodScope(curr_scope,curr_method);
			if (scopeMethod!=null) {
				for (Symb local : scopeMethod.locals) {
					if (local.name.equals(lv_name) && (local.kind.equals(enumKind.arg)||local.kind.equals(enumKind.var))) {
						found_var_in_method = true;
						lv_decl=local.decl;
						break;
					}
				}
				if (found_var_in_method==false) {
					for (Symb local : curr_scope.locals) {
						if (local.name.equals(lv_name)&&(local.kind.equals(enumKind.field)||local.kind.equals(enumKind.field_extend))) {
							found_var_in_class=true;
							lv_decl=local.decl;
						}
					}
				}	
			}
			if(found_var_in_method==false&&found_var_in_class==false) {
				result = "ERROR\n";
				validator_msg.append(curr_class+" "+curr_method+" lv"+lv_name+ " is not defiend\n");
				return;
			}
		}
		assignStatement.rv().accept(this);
		if(lv_decl!=null && (lv_decl.equals("int") || lv_decl.equals("bool") || lv_decl.equals("int_array"))) {
			if(!type.equals(lv_decl)) {
				result = "ERROR\n";
				validator_msg.append("in class: " + curr_class+" in method: "+curr_method+" lv and rv with different type\n");
			}
		}
		
		is_assignStatement=false;
		lv_decl=null;
		lv_name=null;
	}	
		

	@Override
	public void visit(AssignArrayStatement assignArrayStatement) {
		//ex23
		Boolean found_var_in_method = false;
		SymbolTable curr_symbol_table = returnCurrTable(curr_class);
		if(curr_symbol_table != null) {
			Scope curr_scope = curr_symbol_table.curr_scope;
			Scope scopeMethod = findCurrMethodScope(curr_scope,curr_method);
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
			validator_msg.append(curr_class+" "+curr_method+ "array index is not in type int\n");
		}
        assignArrayStatement.rv().accept(this);
        if(!type.equals("int")) {
			result = "ERROR\n";
			validator_msg.append(curr_class+" "+curr_method+" array assign is not in type int\n");
		}
		
	}

	@Override
	public void visit(AndExpr e) {
		is_exp=true;
		visitBinaryExpr(e,"&&");
		if (!is_boolean_exp) {
			result = "ERROR\n";
			validator_msg.append(curr_class+" "+curr_method+" && expression is not boolean\n");
		}
		is_boolean_exp=false;
		is_exp=false;
		
	}

	@Override
	public void visit(LtExpr e) {
		is_exp=true;
		visitBinaryExpr(e,"<");
		if (!is_boolean_exp) {
			result = "ERROR\n";
			validator_msg.append(curr_class+" "+curr_method+"< expression is not boolean\n");
		}
		is_boolean_exp=false;
		is_exp=false;
		
		
	}

	@Override
	public void visit(AddExpr e) {
		is_exp=true;
		visitBinaryExpr(e,"+");
		is_exp=false;
		
	}

	@Override
	public void visit(SubtractExpr e) {
		is_exp=true;
		visitBinaryExpr(e,"-");
		is_exp=false;
		
	}

	@Override
	public void visit(MultExpr e) {
		is_exp=true;
		visitBinaryExpr(e,"*");
		is_exp=false;
		
	}

	@Override
	public void visit(ArrayAccessExpr e) {
		array_call = true;
		e.arrayExpr().accept(this);
		array_call = false;
		e.indexExpr().accept(this);	
		if(!type.equals("int")) {
			result = "ERROR\n";
			validator_msg.append(curr_class+" "+curr_method+"array index is not in type int\n");
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
		method_call_args.clear();
		method_call_name=e.methodId();
		num_of_actuals=e.actuals().size();
		method_call=true;
		methodcall_type=null;
	    for (Expr arg : e.actuals()) {
	    	isActual=true;
	        arg.accept(this); 
	        isActual=false;
	    }
	    if(is_error) {
	    	method_call=false;
			call_var_class=null;
	    	return;
	    }
	    method_call=true;
		e.ownerExpr().accept(this);
		SymbolTable curr_symbol_table = returnCurrTable(call_var_class);
		if(curr_symbol_table != null) {
			Scope curr_scope = curr_symbol_table.curr_scope;
			for (Symb symbol : curr_scope.locals) {
				if(symbol.name.equals(e.methodId())) {
					if(symbol.kind.equals(enumKind.method)||symbol.kind.equals(enumKind.method_extend)) {
						methodcall_type = symbol.decl;
						type=methodcall_type;
						break;
					}
				}
			}
			if (while_or_if==true&&call_var_class!=null) {
				if(!methodcall_type.equals("bool")) {
					result = "ERROR\n";
					validator_msg.append("4 "+curr_class+" "+curr_method+ " cond in while or if statement is not boolean\n");
				}
			}
			if (isActual==true) {
				method_call_args.add(methodcall_type);
			}
			if (methodcall_type!=null) {
				if(is_assignStatement&&isActual==false&&is_newIntArray==false) {
					if(!methodcall_type.equals(lv_decl)) {
						if(lv_decl.equals("int") || lv_decl.equals("bool") || lv_decl.equals("int_array") ||
								methodcall_type.equals("int") || methodcall_type.equals("bool") || methodcall_type.equals("int_array")) {
							result = "ERROR\n";
							validator_msg.append("idenexp "+ curr_class+" "+curr_method+" not in this: lv and rv with different decl lv_name: " + lv_name+"\n");
							method_call=false;
							call_var_class=null;
							return;
						}
						else if(!check_assignment_subtyping(methodcall_type)){
							result = "ERROR\n";
							validator_msg.append("idenexp"+ curr_class+" "+curr_method+"not in this: lv and rv with different decl lv_name: " + lv_name+"\n");
						}
					}
				}
			}
			if(length_call) {
				if(!type.equals("int_array")) {
					result = "ERROR\n";
					validator_msg.append(curr_class+" "+curr_method+" The static type of the object on which length invoked is int[]\n");
				}
			}
		}
		method_call=false;
		call_var_class=null;
	}

	
	@Override
	public void visit(IntegerLiteralExpr e) {
		e.num();
		type = "int";
		if (method_call&&isActual) {
			method_call_args.add("int");
		}
		else if (method_call) {
			result = "ERROR\n";
			validator_msg.append("IntegerLiteralExpr "+ curr_class+" "+"the static type of the object is not a reference type \n");
		}
		if (is_assignStatement&&isActual==false&&is_newIntArray==false&&is_exp==false) {//check arrays
			if(!type.equals(lv_decl)) {
				result = "ERROR\n";
				validator_msg.append("IntegerLiteralExpr "+ curr_class+" "+curr_method+"not in this: lv and rv with different decl lv_name: " + lv_name+"\n");
			}	
		}
	}

	@Override
	public void visit(TrueExpr e) {
		//check
		type = "bool";
		if (method_call&&isActual) {
			method_call_args.add("bool");
		}
		else if (method_call) {
			result = "ERROR\n";
			validator_msg.append("TrueExpr "+ curr_class+" "+"the static type of the object is not a reference type \n");
		}
		if (is_assignStatement&&is_newIntArray==false) {
			if(!type.equals(lv_decl)) {
				result = "ERROR\n";
				validator_msg.append("trueexp "+ curr_class+" "+curr_method+"not in this: lv and rv with different decl lv_name: " + lv_name+"\n");
			}	
		}	
	}

	@Override
	public void visit(FalseExpr e) {
		//check
		type = "bool";
		if (method_call&&isActual) {
			method_call_args.add("bool");
		}
		else if (method_call) {
			result = "ERROR\n";
			validator_msg.append("FalseExpr "+ curr_class+" "+"the static type of the object is not a reference type \n");
			return;
		}
		if (is_assignStatement&&is_newIntArray==false) {
			if(!type.equals(lv_decl)) {
				result = "ERROR\n";
				validator_msg.append("falseexp"+ curr_class+" "+curr_method+" not in this: lv and rv with different decl lv_name: " + lv_name+"\n");
			}	
		}	
	}

	@Override
	public void visit(IdentifierExpr e) {
		SymbolTable var_class_symbol_table=null;
		boolean found_method=false;
		boolean found_var_in_class=false;
		boolean found_var_in_method = false;
		
		SymbolTable curr_symbol_table = returnCurrTable(curr_class);
		if(curr_symbol_table != null) {
			Scope curr_scope = curr_symbol_table.curr_scope;
			if (while_or_if&&method_call==true&&is_exp==false&&isActual==false) {//ex17
				for (Symb symbol : curr_scope.locals) {
					ValidateWhileOrIF(symbol, method_call_name);
				}
			}
			Scope scopeMethod = findCurrMethodScope(curr_scope,curr_method);
			if (scopeMethod!=null) {
				for (Symb local : scopeMethod.locals) {
					if (local.name.equals(e.id())){		
						if(local.kind.equals(enumKind.arg)||local.kind.equals(enumKind.var)) {
							found_var_in_method = true;
							//v_dec=local.decl;	
							if (isActual==true) {
								method_call_args.add(local.decl);
							}
							if (method_call==true && isActual==false) { //ex10
								//find the static type of object inside current function
								//System.out.println(curr_class+" "+curr_method+" "+method_call_name);
								call_var_class=methodCallValidation(local); 	
								if (call_var_class!=null) {
									var_class_symbol_table = returnCurrTable(call_var_class);//ex11-find the var's of argument's class
									if (var_class_symbol_table!=null) {
										Scope var_class_scope = var_class_symbol_table.curr_scope;
										found_method=false;
										for (Symb l : var_class_scope.locals) {// find the method call name in class
											if (l.kind.equals(enumKind.method) || l.kind.equals(enumKind.method_extend)) {
												if (l.name.equals(method_call_name)) {
													found_method=true;
													check_args(var_class_scope,num_of_actuals,method_call_args,method_call_name);
													break;
												}
											}
										}
									}
									if (found_method==false&&method_call==true ) {
										result = "ERROR\n";
										validator_msg.append("the function "+method_call_name+" is not defined in the class "+curr_class+"\n");
									}	
								}
							}
						}
						if(length_call==true&&method_call==false){//ex13
							lengthArrayVarValidation(local);
						}
						if(array_call==true) {//ex22
							callArrayVarValidation(local);
						}
						type = local.decl;
						call_var_class = local.decl;
						break;
					}
				}	
				if(found_var_in_method == false) {
					for (Symb local : curr_scope.locals) {
						if (local.name.equals(e.id())){
							if(local.kind.equals(enumKind.field)||local.kind.equals(enumKind.field_extend)) {
								found_var_in_class=true;
								//v_dec=local.decl;
								if (isActual==true) {
									method_call_args.add(local.decl);	
								}
								if (method_call==true&&isActual==false) {
									call_var_class=methodCallValidation(local);
									if (call_var_class!=null) {
										var_class_symbol_table = returnCurrTable(call_var_class);//ex11-find the var's or argument's class
									}
								}
								if (var_class_symbol_table!=null) {
									Scope var_class_scope = var_class_symbol_table.curr_scope;
									found_method=false;
									for (Symb l : var_class_scope.locals) {// find the method call name in class
										if (l.kind.equals(enumKind.method) || l.kind.equals(enumKind.method_extend)) {
											if (l.name.equals(method_call_name)) {
												found_method=true;
												check_args(var_class_scope,num_of_actuals,method_call_args,method_call_name);
												break;
											}
										}
									}
									if (found_method==false&&method_call==true) {
										result = "ERROR\n";
										validator_msg.append("the function "+method_call_name+" is not defined in the class "+curr_class+"\n");
									}
								}
							}
							if(array_call==true) {//ex22
								callArrayFieldValidation(local);
							}
							if(length_call==true&&method_call==false) {//ex13
								lengthArrayFieldValidation(local);
							}	
							type = local.decl;
							call_var_class = local.decl;
						}
					}
				}
			}
		}
		if (found_var_in_method==false &&found_var_in_class==false) { //ex14
			result = "ERROR\n";
			validator_msg.append(curr_class+" "+curr_method+"the var "+e.id()+" is not defined\n");
			is_error=true;
			return;
		}
		else {
			
		}
		if(is_assignStatement&method_call==false&&is_exp==false) { 
			if(!type.equals(lv_decl)) {
				if (!check_assignment_subtyping(lv_decl)){
					result = "ERROR\n";
					validator_msg.append("idenexp "+ curr_class+" "+curr_method+"not in this: lv and rv with different decl lv_name: " + lv_name+"\n");
				}	
			}
		}
	}
	

	public String methodCallValidation(Symb local) {
		if(local.decl.equals("int") || local.decl.equals("bool") || local.decl.equals("int_array")) {
			result = "ERROR\n";
			validator_msg.append(curr_class+" "+curr_method+ " the static type of the object "+local.name+" is not a reference type\n");
			return null;
		}
		else {
			return local.decl;
		}		
	}
	
	public void lengthArrayVarValidation(Symb local) {
		if(local.kind.equals(enumKind.var)) {
			if(!local.decl.equals("int_array")) {
				result = "ERROR\n";
				validator_msg.append(curr_class+" "+curr_method+" The static type of the object "+local.name+" on which length invoked is int[]\n");
			}
		}
	}
	
	public void lengthArrayFieldValidation(Symb local) {
		if(local.kind.equals(enumKind.field) || local.kind.equals(enumKind.field_extend)) {
			if(!local.decl.equals("int_array")) {
				result = "ERROR\n";
				validator_msg.append(curr_class+" "+curr_method+"The static type of the object on which length invoked is int[]\n");
			}
		}
	}
	
	public void callArrayVarValidation(Symb local) {
		if(local.kind.equals(enumKind.var)) {
			if(!local.decl.equals("int_array")) {
				result = "ERROR\n";
				validator_msg.append(curr_class+" "+curr_method+"array access/assign is not of type int[]\n");
			}
		}
	}
	
	public void callArrayFieldValidation(Symb local) {
		if(local.kind.equals(enumKind.field) || local.kind.equals(enumKind.field_extend)) {
			if(!local.decl.equals("int_array")) {
				result = "ERROR\n";
				validator_msg.append(curr_class+" "+curr_method+"array access/assign is not of type int[]\n");
			}
		}
	}
	

	@Override
	public void visit(ThisExpr e) {
		is_thisexp=true;
		if(method_call==true) {
			SymbolTable this_symbol_table=returnCurrTable(curr_class);
			boolean found_method=false;
			if (isActual) {
				method_call_args.add(curr_class);
			}
			else if(this_symbol_table != null) {
				found_method=check_args(this_symbol_table.curr_scope,num_of_actuals,method_call_args,method_call_name);
				if (found_method==false) {
					result = "ERROR\n";
					validator_msg.append(curr_class+" "+curr_method+" in this: the function "+method_call_name+" is not defined in the class "+curr_class+"\n");
				}	
			}		
		}
		if (is_assignStatement&method_call==false) {
			if(!curr_class.equals(lv_decl)) { //ex16 assignment with this a=this.func
				if (!check_assignment_subtyping(lv_decl)){
					result = "ERROR\n";
					validator_msg.append(curr_class+" "+curr_method+" in this: lv and rv decls are different\n");
				}
			}
		}
		is_thisexp=false;
		type = curr_class;
		call_var_class=curr_class;
	}
	
				
	@Override
	public void visit(NewIntArrayExpr e) {
		is_newIntArray=true;
		e.lengthExpr().accept(this);
		is_newIntArray=false;
		if(!(type.equals("int"))) {
			result = "ERROR\n";
			validator_msg.append(curr_class+" "+curr_method+"In an array allocation new int[e], e is not an int.\n");
		}
		if (is_assignStatement) {
			if(!lv_decl.equals("int_array")) {
				result = "ERROR\n";
				validator_msg.append("intexp "+ curr_class+" "+curr_method+" not in this: lv and rv with different decl lv_name: " + lv_name+"\n");
			}	
		}
		type="int_array";//check
	}

	@Override
	public void visit(NewObjectExpr e) {
		class_call = e.classId();
		boolean found_super_class=false;
		if(method_call&&isActual) {
			method_call_args.add(class_call);
			call_var_class=class_call;
		}
		if(is_assignStatement&&method_call==false) {
			if(!class_call.equals(lv_decl)) {//ex16 assignment with new
				SymbolTable new_class_sym_table = returnCurrTable(class_call);
				if (new_class_sym_table==null) {
					result = "ERROR\n";
					validator_msg.append(class_call+" is not defined somewhere in the file \n");
					return;
				}
				Scope s=new_class_sym_table.curr_scope;
				while(s.prev!=null) {
					s = s.prev;
					if((s.name).equals(lv_decl)) {
						found_super_class = true;
						break;
					}	
				}
				if (!found_super_class) {
					result = "ERROR\n";
					validator_msg.append(curr_class+" "+curr_method+" lv and rv with different decl lv_name:" + lv_name+"\n");		
				}
			}	
		}
		//ex9
		SymbolTable curr_symbol_table = returnCurrTable(e.classId());
		if (curr_symbol_table==null) {
			result = "ERROR\n";
			validator_msg.append(curr_class+" "+curr_method+ "new object "+e.classId()+" that is defined somewhere in the file \n");
			return;
		}
		else {
			if (!curr_symbol_table.curr_scope.type.equals(scopeType.type_class)) {
				result = "ERROR\n";
				validator_msg.append(curr_class+" "+curr_method+ "new object "+e.classId()+" that is defined somewhere in the file \n");
				return;
			}
		}
		type=class_call;//check
		call_var_class=class_call;
	}

	@Override
	public void visit(NotExpr e) {
		e.e().accept(this);
		String e_type = type;
		//ex21
    	if(!(e_type.equals("bool"))) {
    		result = "ERROR\n";
			validator_msg.append("not "+ curr_class+" "+curr_method+"The argument to the predefined operator is of the incorrect type.\n");
    	}
    	type = "bool";
	}

	@Override
	public void visit(IntAstType t) {
		type ="int";
		if (is_assignStatement) {
			if(!type.equals(lv_decl)) {
				result = "ERROR\n";
				validator_msg.append("intas_type "+ curr_class+" "+curr_method+"not in this: lv and rv with different decl lv_name: " + lv_name+"\n");
			}	
		}	
	}

	@Override
	public void visit(BoolAstType t) {
		type ="bool";
		if (method_call&&isActual) {
			method_call_args.add("bool");}
	}

	@Override
	public void visit(IntArrayAstType t) {
		type = "int_array";
		if (method_call&&isActual) {
			method_call_args.add("int_array");	}
	}

	@Override
	public void visit(RefType t) {
		type = t.id();
		if (method_call&isActual) {
			method_call_args.add(t.id());
		}
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
    
    public Scope findCurrMethodScope(Scope class_scope,String method_name) {
		Scope scopeMethod=null;
		for(Scope scope : class_scope.next) {
			if(scope.type.equals(scopeType.method)) {
				if(scope.name.equals(method_name)) {
					scopeMethod=scope;
					break;
				}
			}
		}
		return scopeMethod;
    }
    public boolean check_args(Scope super_scope,int curr_num_of_args,ArrayList<String> args,String method_name) {
    	boolean found_method = false;
    	Scope method_scope;
    	for (Symb local : super_scope.locals) {
    		if(local.kind.equals(enumKind.method)||(local.kind.equals(enumKind.method_extend))) {
    			if (local.name.equals(method_name)) {
    				found_method=true;
    				break;
    			}
    		}
    	}
    	if (found_method==false) {
    		result = "ERROR\n";
			validator_msg.append("in class: " + curr_class+" in method: "+curr_method+" the method: "+ method_name+ " is not defiened in " + super_scope.name + "\n");
			return found_method;
    	}
    		
		Scope s=super_scope;
		method_scope=findCurrMethodScope(s,method_name);
		if (method_scope==null) {
			while(s.prev!=null) {
				s = s.prev;
				method_scope=findCurrMethodScope(s,method_name);
				if (method_scope!=null) {
					break;
				}
			}	
		}
		if(method_scope.num_of_args!=curr_num_of_args) {
			result = "ERROR\n";
			validator_msg.append("in class: " + curr_class+" in method: "+curr_method+" override method with diffrent number of arguments\n");
			return found_method;
		}
		else if (curr_num_of_args!=0) {
			int i=0;
			while( i <curr_num_of_args) {
				for(Symb local : method_scope.locals) {
					if(local.kind.equals(enumKind.arg)) {
						if(!local.decl.equals(args.get(i))) {
							if (args.get(i).equals("bool")||args.get(i).equals("int_arr")||args.get(i).equals("int")) {
								result = "ERROR\n";
								validator_msg.append(curr_class+" "+curr_method+"override method with diffrent return type\n");
								return false;
							}
							else {
								Scope type_scope = returnCurrTable(args.get(i)).curr_scope;
								boolean found_super_class = false;
								while(type_scope.prev!=null) {
									type_scope = type_scope.prev;
									if((type_scope.name).equals(local.decl)) {
										found_super_class = true;
										break;
									}
								}
								if(found_super_class == false) {
									result = "ERROR\n";
									validator_msg.append(curr_class+" "+curr_method+"override method with diffrent return type\n");
									return false;
								}
								else {
									i++;
								}
							}
						}
						else {
							i++;
						}
					}
				}
			}
		}
		return found_method;
	}


	public boolean check_assignment_subtyping(String decl){
		boolean found_super_class=false;  
		if (is_thisexp) {
			if (curr_super_class==null) {
				return false;
			}
			if (curr_super_class != null) {
				if(!curr_super_class.equals(decl)) {
					SymbolTable super_symbol_table = returnCurrTable(curr_super_class);
					Scope s=super_symbol_table.curr_scope;
					while(s.prev!=null) {
						s = s.prev;
						if((s.name).equals(decl)) {
							found_super_class = true;
							break;
						}	
					}
					if(found_super_class==false) {
						return false;
						
					}
				}
			}
		}
		else if (!(type.equals("bool")||type.equals("int")||type.equals("int_array"))) {
			SymbolTable super_symbol_table = returnCurrTable(type);
			Scope s=super_symbol_table.curr_scope;
			while(s.prev!=null) {
				s = s.prev;
				if((s.name).equals(decl)) {
					found_super_class = true;
					break;
				}	
			}
			if(found_super_class==false) {
				return false;
			}
		}
		return true;
	}
	
	public void ValidateWhileOrIF(Symb symbol, String method_name) {
		if(symbol.name.equals(method_name)) {
			if(symbol.kind.equals(enumKind.method)||symbol.kind.equals(enumKind.method_extend)) {
				if(!symbol.decl.equals("bool")) {
					result = "ERROR\n";
					validator_msg.append("in class: " +curr_class+" in method: "+curr_method+ " cond in while or if statement is not boolean\n");
				}
			}
		}
	}
	
}


//else if(lv_decl!=null) {//error!!(function and assignment)
//	if(!type.equals(lv_decl)) {
//		if (!check_assignment_subtyping(lv_decl)){
//		result = "ERROR\n";
//		validator_msg.append(curr_class+" "+curr_method+"not in this: lv and rv with different decl lv_name: " + lv_name+"\n");
//		}	
//}
//	}