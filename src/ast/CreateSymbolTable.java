package ast;

import java.util.ArrayList;
import java.util.Map;

import ast.SymbolTable.Scope;
import ast.SymbolTable.Symb;

public class CreateSymbolTable implements Visitor {
	ArrayList<SymbolTable> symbol_tables ;
	SymbolTable curr_symbol_table;
	String curr_name = "";
	enumKind curr_kind = enumKind.empty;
	int method_vtable_index;
	int fields_vtable_index;
	String MainClassName;
	boolean validator = true;
	private StringBuilder validator_msg = new StringBuilder();
	
	public CreateSymbolTable(ArrayList<SymbolTable> symbol_tables) {
		this.symbol_tables = symbol_tables;
	}
	
	public String getValidatorMsg() {
		return validator_msg.toString();
	}
	
	public boolean getValidatorResult() {
		return validator;
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
    	int locals_size = 0;
    	method_vtable_index = 0;
    	fields_vtable_index = 0;
    	
    	validateClassName(classDecl.name());
        SymbolTable symbol_table = new SymbolTable();
        symbol_tables.add(symbol_table);
        curr_symbol_table = symbol_table;
        symbol_table.openScope(scopeType.type_class, classDecl.name());
        
        Scope super_scope=validateSuperClass(classDecl.superName(),classDecl.name());
        
        for (var methodDecl : classDecl.methoddecls()) {
        	locals_size = curr_symbol_table.curr_scope.locals.size();
        	methodDecl.accept(this);
        	method_vtable_index++;
        	validateFieldsAndMethodDefine(locals_size,curr_symbol_table.curr_scope.locals.size());
        }
        int numOfFields= classDecl.fields().size();
        int numOfMethod = classDecl.methoddecls().size();
        if (super_scope!=null) {
	        for(Symb entry :super_scope.locals) {
	        	ArrayList<String> decl = new ArrayList<String>();
				decl.add(entry.decl);
				if(entry.kind == enumKind.method || entry.kind == enumKind.method_extend) {
	        		if(null != curr_symbol_table.addSymbol(entry.name,entry.decl ,enumKind.method_extend, entry.extendFrom, method_vtable_index)) {
	        			numOfMethod++;
		        		method_vtable_index++;
	        		}
	        		else {
	        			ValidateOverloading(entry, super_scope, curr_symbol_table.curr_scope);
	        		}
				}
				if(entry.kind == enumKind.field || entry.kind == enumKind.field_extend) {
					if(null != curr_symbol_table.addSymbol(entry.name,entry.decl ,enumKind.field_extend, entry.extendFrom,entry.vtableindex)) {
						numOfFields++;
		        		fields_vtable_index +=entry.vtableindex;
					}	
				}
			}
        }
        if(fields_vtable_index == 0 && numOfMethod > 0) {
        	fields_vtable_index = 8;
        }
        for (var fieldDecl : classDecl.fields()) {
        	locals_size = curr_symbol_table.curr_scope.locals.size();
        	curr_kind = enumKind.field;
            fieldDecl.accept(this);
            validateFieldsAndMethodDefine(locals_size,curr_symbol_table.curr_scope.locals.size());
        }
        curr_symbol_table.curr_scope.setNumOfFields(numOfFields);
        curr_symbol_table.curr_scope.setNumOfMethods(numOfMethod);
        curr_symbol_table.curr_scope.setSizeOfObject(fields_vtable_index);
    }

    @Override
    public void visit(MainClass mainClass) {
    	MainClassName = mainClass.name();
    	SymbolTable symbol_table = new SymbolTable();
        symbol_tables.add(symbol_table);
        curr_symbol_table = symbol_table;
        symbol_table.openScope(scopeType.type_class, mainClass.name());
        //mainClass.scope = curr_symbol_table.curr_scope;
        mainClass.argsName();
        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
    	int num_of_args = 0;
    	curr_name = methodDecl.name();
    	curr_kind = enumKind.method;
        methodDecl.returnType().accept(this);
        curr_symbol_table.openScope(scopeType.method, methodDecl.name());
        //methodDecl.scope = curr_symbol_table.curr_scope;

        for (var formal : methodDecl.formals()) {
        	num_of_args++;
        	curr_kind = enumKind.arg;
            formal.accept(this);
            curr_kind = enumKind.var;
            formal.accept(this);
        }
        curr_symbol_table.curr_scope.setNumOfArgs(num_of_args);

        for (var varDecl : methodDecl.vardecls()) {
        	curr_kind = enumKind.var;
            varDecl.accept(this);
        }
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
            
        }

        methodDecl.ret().accept(this);
        
        curr_symbol_table.closeScope();

    }

    @Override
    public void visit(FormalArg formalArg) {
    	curr_name = formalArg.name();
        formalArg.type().accept(this);
    }

    @Override
    public void visit(VarDecl varDecl) {
        curr_name = varDecl.name();
        varDecl.type().accept(this);
    }

    @Override
    public void visit(BlockStatement blockStatement) {
    	curr_symbol_table.openScope(scopeType.statement, "block");
    	//blockStatement.scope = curr_symbol_table.curr_scope;
        for (var s : blockStatement.statements()) {
            s.accept(this);
        }
        curr_symbol_table.closeScope();
    }

    @Override
    public void visit(IfStatement ifStatement) {
    	curr_symbol_table.openScope(scopeType.statement, "if");
    	//ifStatement.scope = curr_symbol_table.curr_scope;
        ifStatement.cond().accept(this);
        ifStatement.thencase().accept(this);
        if(ifStatement.elsecase()!=null) {
        ifStatement.elsecase().accept(this);}
        curr_symbol_table.closeScope();
    }

    @Override
    public void visit(WhileStatement whileStatement) {
    	curr_symbol_table.openScope(scopeType.statement, "while");
    	//whileStatement.scope = curr_symbol_table.curr_scope;
        whileStatement.cond().accept(this);
        whileStatement.body().accept(this);
        curr_symbol_table.closeScope();
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        sysoutStatement.arg().accept(this);
    }

    @Override
    public void visit(AssignStatement assignStatement) {
    	assignStatement.rv().accept(this);
        assignStatement.lv();
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        assignArrayStatement.lv();
        assignArrayStatement.index().accept(this);
        assignArrayStatement.rv().accept(this);
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
        for (Expr arg : e.actuals()) {
            arg.accept(this);
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
        //System.out.println(e.id());
    }

    public void visit(ThisExpr e) {
    	
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        e.lengthExpr().accept(this);
    }

    @Override
    public void visit(NewObjectExpr e) {
    	//System.out.println(e.classId());
    }
    
    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
    }

    @Override
    public void visit(IntAstType t) {
    	int vtable_index = 0;
    	if(curr_kind==enumKind.arg) {
    		curr_name = "%" + curr_name;
    	}
    	if(curr_kind==enumKind.method) {
    		vtable_index = method_vtable_index;
    	}
    	else if(curr_kind==enumKind.field) {
    		vtable_index = fields_vtable_index;
    		fields_vtable_index+= 4;
    	}
    	Symb check =curr_symbol_table.addSymbol(curr_name, "int", curr_kind, curr_symbol_table.curr_scope.name,vtable_index);
    	if (check==null) {
    		validator = false;
        	validator_msg.append("2 same name cannot be used for the same var in the method "+curr_name+"\n");
    	}
    	curr_name = "";
    	curr_kind = enumKind.empty;
    }

    @Override
    public void visit(BoolAstType t) {
    	
    	int vtable_index = 0;
    	if(curr_kind==enumKind.arg) {
    		curr_name = "%" + curr_name;
    	}
    	if(curr_kind==enumKind.method) {
    		vtable_index = method_vtable_index;
    	}
    	else if(curr_kind==enumKind.field) {
    		vtable_index = fields_vtable_index;
    		fields_vtable_index+= 1;
    	}
    	Symb check =curr_symbol_table.addSymbol(curr_name, "bool", curr_kind, curr_symbol_table.curr_scope.name,vtable_index);
    	if (check==null) {
    		validator = false;
        	validator_msg.append(" 1 same name cannot be used for the same var in the method "+curr_name+"\n");
    	}
    	curr_name = "";
    	curr_kind = enumKind.empty;
    }

    @Override
    public void visit(IntArrayAstType t) {
    	int vtable_index = 0;
    	if(curr_kind==enumKind.arg) {
    		curr_name = "%" + curr_name;
    	}
    	if(curr_kind==enumKind.method) {
    		vtable_index = method_vtable_index;
    	}
    	else if(curr_kind==enumKind.field) {
    		vtable_index = fields_vtable_index;
    		fields_vtable_index+= 8;
    	}
    	Symb check =curr_symbol_table.addSymbol(curr_name, "int_array", curr_kind,curr_symbol_table.curr_scope.name,vtable_index);
    	if (check==null) {
    		validator = false;
        	validator_msg.append("3 same name cannot be used for the same var in the method "+curr_name+"\n");
    	}
    	curr_name = "";
    	curr_kind = enumKind.empty;
    }

    @Override
    public void visit(RefType t) {
    	int vtable_index = 0;
    	if(curr_kind==enumKind.arg) {
    		curr_name = "%" + curr_name;
    	}
    	if(curr_kind==enumKind.method) {
    		vtable_index = method_vtable_index;
    	}
    	else if(curr_kind==enumKind.field) {
    		vtable_index = fields_vtable_index;
    		fields_vtable_index+= 8;
    	}
    	Symb check=curr_symbol_table.addSymbol(curr_name, t.id(), curr_kind, curr_symbol_table.curr_scope.name,vtable_index);
    	if (check==null) {
    		validator = false;
        	validator_msg.append("4 same name cannot be used for the same var in the method "+curr_name+"\n");
    	}
    	curr_name = "";
    	curr_kind = enumKind.empty;
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
	
	//Validation - The same name cannot be used to name two classes
	public void validateClassName(String class_name) {
	    for(SymbolTable t:symbol_tables) {
			if (0==t.curr_scope.name.compareTo(class_name)) {
				validator = false;
				validator_msg.append("The same name cannot be used to name two classes\n");
			}
	    }
	}
	
	public Scope validateSuperClass(String super_name,String classDecl_name) {
		Scope super_scope=null;
		if (super_name != null) {
			//Validation - The main class cannot be extended
		    if(super_name.equals(MainClassName)) {
		    	validator=false;
		    	validator_msg.append("The main class cannot be extended\n");
		    }
		    if(super_name.equals(classDecl_name)) {
		    	validator=false;
		    	validator_msg.append("Class cannot extend himself\n");
		    }
		    else {
		    	for(SymbolTable t:symbol_tables) {
		    		if (0==t.curr_scope.name.compareTo(super_name)) {
		    			super_scope=t.curr_scope;
		    			if (super_scope!=null) {
		    				break;
		    			}	
		    		}
		    	}
		    	if (super_scope!=null) {
		            curr_symbol_table.curr_scope.prev=super_scope;
		        	super_scope.next.add(curr_symbol_table.curr_scope);
		        }
		    	else {
		    		//Validation -super class should be defined before the extended class;
		    		validator=false;
		    		validator_msg.append("super class should be defined before the extended class\n");
		    	}
		    }
		}
	    return super_scope;
	}
	
	public void validateFieldsAndMethodDefine(int locals_size_before, int local_size_after) {
        if(locals_size_before == local_size_after) {
        	validator = false;
        	validator_msg.append("same name cannot be used for the same field or method in one class\n");
        }
	}
	
	public void ValidateOverloading(Symb entry,Scope super_scope, Scope curr_scope) {
		Scope methodScope1 = null;
		Scope methodScope2 = null;
		// finding the  method scope of the super scope
		for( Scope e1 : super_scope.next) {
			if(e1.name.equals(entry.name) && e1.type == scopeType.method) {
				methodScope1 = e1;
				break;
			}
		}
		// finding the  method scope of the curr scope
		for( Scope e2 : curr_scope.next) {
			if(e2.name.equals(entry.name) && e2.type == scopeType.method) {
				methodScope2 = e2;
				break;
			}
		}
		ArrayList<String> args_types = new ArrayList<String>();
		if(methodScope1!=null && methodScope2!=null) {
			if(methodScope1.num_of_fields == methodScope2.num_of_fields) {
				for( Symb s : methodScope1.locals) {
					if(s.kind == enumKind.arg) {
						args_types.add(s.decl);
					}
				}
				for( Symb s : methodScope2.locals) {
					if(s.kind == enumKind.arg) {
						if(args_types.contains(s.decl)) {
							args_types.remove(s.decl);
						}
					}
				}
				if(!args_types.isEmpty()) {
					validator = false;
		        	validator_msg.append("overload method "+ entry.name +" in class " + curr_scope.name +"\n");
				}
			}
		}
	}
	
}
