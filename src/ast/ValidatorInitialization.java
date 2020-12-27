package ast;

import java.util.ArrayList;

public class ValidatorInitialization implements Visitor {
	private String result;
	private StringBuilder validator_msg = new StringBuilder();
	ArrayList<String> initVar;
	ArrayList<String> initVarMethod;
	boolean is_field = false;
	String curr_class;
	String curr_method;

    
	public ValidatorInitialization(String result) {
		this.result = result;
	}
	
    public String getResult() {
        return result;
    }
    
	public String getValidatorMsg() {
		return validator_msg.toString();
	}

    private void visitBinaryExpr(BinaryExpr e, String infixSymbol) {
        e.e1().accept(this);
        e.e2().accept(this);
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
    	initVar = new ArrayList<String>();
    	
        if (classDecl.superName() != null) {
            classDecl.superName();
        }
        for (var fieldDecl : classDecl.fields()) {
        	is_field = true;
            fieldDecl.accept(this);
            is_field = false;
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
    	curr_method = methodDecl.name();
    	initVarMethod = new ArrayList<String>(initVar);
    	
        methodDecl.returnType().accept(this);
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
    }

    @Override
    public void visit(FormalArg formalArg) {
        //formalArg.type().accept(this);
        initVarMethod.add(formalArg.name());
    }

    @Override
    public void visit(VarDecl varDecl) {
        varDecl.type().accept(this);
        if(is_field) {
        	initVar.add(varDecl.name());
        }
        else {
        	// in case there is a var in method with the same name as field.
        	if(initVarMethod.contains(varDecl.name())) {
        		initVarMethod.remove(varDecl.name());
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
    	ArrayList<String> before = new ArrayList<String>(initVarMethod);
        ifStatement.cond().accept(this);
        ifStatement.thencase().accept(this);
        ArrayList<String> after_if = new ArrayList<String>(initVarMethod);
        
        initVarMethod = new ArrayList<String>(before);
        ifStatement.elsecase().accept(this);
        ArrayList<String> after_else = new ArrayList<String>(initVarMethod);
        
        initVarMethod = joinArray(after_if, after_else);
    }

    @Override
    public void visit(WhileStatement whileStatement) {
    	ArrayList<String> before = new ArrayList<String>(initVarMethod);
        whileStatement.cond().accept(this);
        whileStatement.body().accept(this);
        ArrayList<String> after_while = new ArrayList<String>(initVarMethod);
        initVarMethod = joinArray(before, after_while);
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        sysoutStatement.arg().accept(this);
    }

    @Override
    public void visit(AssignStatement assignStatement) {
        assignStatement.rv().accept(this);
        if(!initVarMethod.contains(assignStatement.lv())) {
        	initVarMethod.add(assignStatement.lv());
        }  
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        if(!initVarMethod.contains(assignArrayStatement.lv())){
    		result = "ERROR\n";
			validator_msg.append(assignArrayStatement.lv() +" array is not initialized in " + curr_method + "\n");
        }
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
        e.methodId();

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
    	if(!initVarMethod.contains(e.id())) {
    		result = "ERROR\n";
			validator_msg.append(e.id() +" is not initialized in " + curr_method + "\n");
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
        e.classId();
    }

    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
    }

    @Override
    public void visit(IntAstType t) {
    }

    @Override
    public void visit(BoolAstType t) {
    }

    @Override
    public void visit(IntArrayAstType t) {
    }

    @Override
    public void visit(RefType t) {
        t.id();
    }
    
    public ArrayList<String> joinArray(ArrayList<String> list1, ArrayList<String> list2) {
    	ArrayList<String> list = new ArrayList<String>();

        for (String str : list1) {
            if(list2.contains(str)) {
                list.add(str);
            }
        }
        return list;
    }
    
}
