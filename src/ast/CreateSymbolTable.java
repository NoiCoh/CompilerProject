package ast;

import java.util.ArrayList;

import ast.SymbolTable.Scope;

public class CreateSymbolTable implements Visitor {
	ArrayList<SymbolTable> symbol_tables ;
	SymbolTable curr_symbol_table;
	String curr_name = "";
	enumKind curr_kind = enumKind.empty;
	int vtable_index;
	
	public CreateSymbolTable(ArrayList<SymbolTable> symbol_tables) {
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
    	vtable_index = 0;
        SymbolTable symbol_table = new SymbolTable();
        symbol_tables.add(symbol_table);
        curr_symbol_table = symbol_table;
        symbol_table.openScope(scopeType.type_class, classDecl.name(),0);
        //classDecl.scope = curr_symbol_table.curr_scope;
        if (classDecl.superName() != null) {
        	String super_name=classDecl.superName();
        	Scope super_scope=null;
        	for(SymbolTable t:symbol_tables) {
        		if (0==t.curr_scope.name.compareTo(super_name)) {
        			super_scope=t.curr_scope;
        			if (super_scope!=null) {
        				break;
        			}
        	}}
        	if (super_scope!=null) {
	            curr_symbol_table.curr_scope.prev=super_scope;
	        	super_scope.next.add(curr_symbol_table.curr_scope);}
        		//System.out.println("Super :"+ classDecl.superName());
        	}
        for (var fieldDecl : classDecl.fields()) {
        	curr_kind = enumKind.field;
            fieldDecl.accept(this);
        }
        for (var methodDecl : classDecl.methoddecls()) {
        	methodDecl.accept(this);
        }
    }

    @Override
    public void visit(MainClass mainClass) {
    	
    	SymbolTable symbol_table = new SymbolTable();
        symbol_tables.add(symbol_table);
        curr_symbol_table = symbol_table;
        symbol_table.openScope(scopeType.type_class, mainClass.name(), 0);
        //mainClass.scope = curr_symbol_table.curr_scope;
        mainClass.argsName();
        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
    	curr_name = methodDecl.name();
    	curr_kind = enumKind.method;
        methodDecl.returnType().accept(this);
        curr_symbol_table.openScope(scopeType.method, methodDecl.name(),vtable_index);
        vtable_index++;
        //methodDecl.scope = curr_symbol_table.curr_scope;

        for (var formal : methodDecl.formals()) {
        	curr_kind = enumKind.arg;
            formal.accept(this);
            curr_kind = enumKind.var;
            formal.accept(this);
        }

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
    	curr_symbol_table.openScope(scopeType.statement, "block",0);
    	//blockStatement.scope = curr_symbol_table.curr_scope;
        for (var s : blockStatement.statements()) {
            s.accept(this);
        }
        curr_symbol_table.closeScope();
    }

    @Override
    public void visit(IfStatement ifStatement) {
    	curr_symbol_table.openScope(scopeType.statement, "if",0);
    	//ifStatement.scope = curr_symbol_table.curr_scope;
        ifStatement.cond().accept(this);
        ifStatement.thencase().accept(this);
        ifStatement.elsecase().accept(this);
        curr_symbol_table.closeScope();
    }

    @Override
    public void visit(WhileStatement whileStatement) {
    	curr_symbol_table.openScope(scopeType.statement, "while",0);
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
    	if(curr_kind==enumKind.arg) {
    		curr_name = "%" + curr_name;
    	}
    	curr_symbol_table.addSymbol(curr_name, "int", curr_kind);
    	curr_name = "";
    	curr_kind = enumKind.empty;
    }

    @Override
    public void visit(BoolAstType t) {
    	if(curr_kind==enumKind.arg) {
    		curr_name = "%" + curr_name;
    	}
    	curr_symbol_table.addSymbol(curr_name, "bool", curr_kind);
    	curr_name = "";
    	curr_kind = enumKind.empty;
    }

    @Override
    public void visit(IntArrayAstType t) {
    	if(curr_kind==enumKind.arg) {
    		curr_name = "%" + curr_name;
    	}
    	curr_symbol_table.addSymbol(curr_name, "int_array", curr_kind);
    	curr_name = "";
    	curr_kind = enumKind.empty;
    }

    @Override
    public void visit(RefType t) {
    	if(curr_kind==enumKind.arg) {
    		curr_name = "%" + curr_name;
    	}
    	curr_symbol_table.addSymbol(curr_name, t.id(), curr_kind);
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
}
