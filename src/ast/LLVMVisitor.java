package ast;

import java.util.*;

import ast.SymbolTable.Scope;
import ast.SymbolTable.Symb;

public class LLVMVisitor implements Visitor {
    private StringBuilder builder = new StringBuilder();
    public static int counter;
    ArrayList<SymbolTable> symbol_tables;
    Stack<Entry> registers_queue  = new Stack<>();
    String curr_class  = "";
	String curr_method = "";
	int formalsStep = 0;
	int ifCounter = 0;
    
    private int indent = 0;
    

	public LLVMVisitor(ArrayList<SymbolTable> symbol_tables) {
		this.symbol_tables = symbol_tables;
	}
	
    public String getString() {
        return builder.toString();
    }

    private void appendWithIndent(String str) {
        builder.append("\t".repeat(indent));
        builder.append(str);
    }

    private void visitBinaryExpr(BinaryExpr e, String infixSymbol) {
    	e.e1().accept(this);
    	e.e2().accept(this);
    	Entry e2 = registers_queue.pop();
    	Entry e1 = registers_queue.pop();
    	
    	appendWithIndent("%_" + counter +" = ");
    	registers_queue.add(new Entry("%" + counter,"i32"));
    	counter++;
    	
    	builder.append(infixSymbol + " i32 ");
    	builder.append(e1.getVarName() +", " + e2.getVarName());     
        builder.append("\n");
    }


    @Override
    public void visit(Program program) {
        program.mainClass().accept(this);
        builder.append("\n");
        for (ClassDecl classdecl : program.classDecls()) {
            classdecl.accept(this);
            builder.append("\n");
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {
    	curr_class = classDecl.name();
        appendWithIndent("class ");
        builder.append(classDecl.name());
        if (classDecl.superName() != null) {
            builder.append(" extends ");
            builder.append(classDecl.superName());
        }
        builder.append(" {\n");

        indent++;
        for (var fieldDecl : classDecl.fields()) {
            fieldDecl.accept(this);
            builder.append("\n");
        }
        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
            builder.append("\n");
        }
        indent--;
        appendWithIndent("}\n");
    }

    @Override
    public void visit(MainClass mainClass) {
    	curr_class = "";
        appendWithIndent("class ");
        builder.append(mainClass.name());
        builder.append(" {\n");
        indent++;
        appendWithIndent("public static void main(String[] ");
        builder.append(mainClass.argsName());
        builder.append(") {");
        builder.append("\n");
        indent++;
        mainClass.mainStatement().accept(this);
        indent--;
        appendWithIndent("}\n");
        indent--;
        appendWithIndent("}\n");
    }

    @Override
    public void visit(MethodDecl methodDecl) {
    	curr_method = methodDecl.name();
    	
    	builder.append("define ");
        methodDecl.returnType().accept(this);
        
        builder.append(" @" + curr_class + "." + curr_method );
        builder.append("(");
        builder.append("i8* this");
        String delim = ", ";
        for (var formal : methodDecl.formals()) {
            builder.append(delim);
            formal.accept(this);
        }
        builder.append(") {\n");
        formalsStep++;
        for (var formal : methodDecl.formals()) {
            formal.accept(this);
        }
        formalsStep--;
        for (var varDecl : methodDecl.vardecls()) {
            varDecl.accept(this);
        }
        LLVMStatements llstat = new LLVMStatements(symbol_tables, curr_class);
        llstat.visit(methodDecl);
    	builder.append(llstat.getString());

        
        methodDecl.ret().accept(this);
        appendWithIndent("ret ");
        if(! registers_queue.isEmpty()) {
        	Entry entry = registers_queue.pop();
            builder.append(entry.getType());
            builder.append(" " + entry.getVarName());
        }
        builder.append("\n}");
    }

    @Override
    public void visit(FormalArg formalArg) {
    	if(formalsStep == 0) {
    		formalArg.type().accept(this);
            builder.append(" %.");
            builder.append(formalArg.name());
    	}
    	else {
    		appendWithIndent("%" + formalArg.name());
    		builder.append(" = alloca ");
    		formalArg.type().accept(this);
    		builder.append("\n");
    		appendWithIndent("store ");
    		formalArg.type().accept(this);
    		builder.append(" %." + formalArg.name() +", ");
    		formalArg.type().accept(this);
    		builder.append("* " + formalArg.name() +"\n");
    	}
        
    }

    @Override
    public void visit(VarDecl varDecl) {
    	appendWithIndent("%");
    	builder.append(varDecl.name());
    	builder.append(" = alloca ");
        varDecl.type().accept(this);
        builder.append("\n");
    }

    @Override
    public void visit(BlockStatement blockStatement) {
        appendWithIndent("{");
        for (var s : blockStatement.statements()) {
            builder.append("\n");
            s.accept(this);
        }
        builder.append("\n");
        appendWithIndent("}\n");
    }

    @Override
    public void visit(IfStatement ifStatement) {
    	//builder.append("if" + ifCounter + ":");
        ifStatement.cond().accept(this);
        builder.append(")\n");
        indent++;
        ifStatement.thencase().accept(this);
        indent--;
        appendWithIndent("else\n");
        indent++;
        ifStatement.elsecase().accept(this);
        indent--;
    }

    @Override
    public void visit(WhileStatement whileStatement) {
        appendWithIndent("while (");
        whileStatement.cond().accept(this);
        builder.append(") {");
        indent++;
        whileStatement.body().accept(this);
        indent--;
        builder.append("\n");
        appendWithIndent("}\n");
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        appendWithIndent("System.out.println(");
        sysoutStatement.arg().accept(this);
        builder.append(");\n");
    }

    @Override
    public void visit(AssignStatement assignStatement) {
    	appendWithIndent("store ");
    	
    	SymbolTable curr_symbol_table = returnCurrTable(curr_class);
		String lv_type = "";
		lv_type = curr_symbol_table.curr_scope.findSymbolType(assignStatement.lv(), enumKind.field);
		if(lv_type.equals("")) {
			// find if originalName defined in method
	    	if(curr_symbol_table != null) {
	    		Scope curr_method_scope = curr_symbol_table.findScope(curr_method,scopeType.method);
	    		if(curr_method_scope != null) {
	    			lv_type = curr_method_scope.findSymbolType(assignStatement.lv(), enumKind.var);
	    			String type = typeConvertor(lv_type);
	    			builder.append(type + " ");
	    			assignStatement.rv().accept(this);
	    			builder.append(", " + type+ "* ");	
	    		}
			}
		}
		else {
			String type = typeConvertor(lv_type);
			builder.append(type+ " ");
			assignStatement.rv().accept(this);
			builder.append(", " + type+ "* ");	
		}
        builder.append("%");
        builder.append(assignStatement.lv());
        builder.append("\n");
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        appendWithIndent("");
        builder.append(assignArrayStatement.lv());
        builder.append("[");
        assignArrayStatement.index().accept(this);
        builder.append("]");
        builder.append(" = ");
        assignArrayStatement.rv().accept(this);
        builder.append(";\n");
    }

    @Override
    public void visit(AndExpr e) {
        visitBinaryExpr(e, "and");
    }

    @Override
    public void visit(LtExpr e) { // <
        e.e1().accept(this);
        e.e2().accept(this);
    }

    @Override
    public void visit(AddExpr e) {
        visitBinaryExpr(e, "add");
    }

    @Override
    public void visit(SubtractExpr e) {
        visitBinaryExpr(e, "sub");
    }

    @Override
    public void visit(MultExpr e) {
        visitBinaryExpr(e, "mul");
    }

    @Override
    public void visit(ArrayAccessExpr e) {
        builder.append("(");
        e.arrayExpr().accept(this);
        builder.append(")");
        builder.append("[");
        e.indexExpr().accept(this);
        builder.append("]");
    }

    @Override
    public void visit(ArrayLengthExpr e) {
        builder.append("(");
        e.arrayExpr().accept(this);
        builder.append(")");
        builder.append(".length");
    }

    @Override
    public void visit(MethodCallExpr e) {
        builder.append("(");
        e.ownerExpr().accept(this);
        builder.append(")");
        builder.append(".");
        builder.append(e.methodId());
        builder.append("(");

        String delim = "";
        for (Expr arg : e.actuals()) {
            builder.append(delim);
            arg.accept(this);
            delim = ", ";
        }
        builder.append(")");
    }

    @Override
    public void visit(IntegerLiteralExpr e) {
    	registers_queue.add(new Entry("" + e.num(),"i32"));
    }

    @Override
    public void visit(TrueExpr e) {
        builder.append("true");
    }

    @Override
    public void visit(FalseExpr e) {
        builder.append("false");
    }

    @Override
    public void visit(IdentifierExpr e) {
       //%_0 = load i32, i32* %num
    	appendWithIndent("%_"+ counter + " = load ");
    	
    	SymbolTable curr_symbol_table = returnCurrTable(curr_class);
		String type = "";
		if(curr_symbol_table != null) {
			type = curr_symbol_table.curr_scope.findSymbolType(e.id(), enumKind.field);
			if(type.equals("")) {
				// find if originalName defined in method
		    	if(curr_symbol_table != null) {
		    		Scope curr_method_scope = curr_symbol_table.findScope(curr_method,scopeType.method);
		    		if(curr_method_scope != null) {
		    			type = curr_method_scope.findSymbolType(e.id(), enumKind.var);
		    		}
				}
			}
			String convert_type = typeConvertor(type);
			builder.append(convert_type+ ", " + convert_type+ "* ");
			builder.append("%" + e.id() + "\n");
			registers_queue.add(new Entry("%_" + counter,convert_type));
			counter++;
		}
    }


    public void visit(ThisExpr e) {
        builder.append("this");
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        builder.append("new int[");
        e.lengthExpr().accept(this);
        builder.append("]");
    }

    @Override
    public void visit(NewObjectExpr e) {
        builder.append("new ");
        builder.append(e.classId());
        builder.append("()");
    }

    @Override
    public void visit(NotExpr e) {
        builder.append("!(");
        e.e().accept(this);
        builder.append(")");
    }

    @Override
    public void visit(IntAstType t) {
        builder.append("i32");
    }

    @Override
    public void visit(BoolAstType t) {
        builder.append("i1");
    }

    @Override
    public void visit(IntArrayAstType t) {
        builder.append("i32*");
    }

    @Override
    public void visit(RefType t) {
        builder.append(t.id());
    }
    public SymbolTable returnCurrTable(String curr_class) {
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
    
    public String typeConvertor(String lv_type) {
    	if(lv_type.equals("int")) {
			return "i32";
			
		}
		else if(lv_type.equals("bool")) {
			return "i1";
			
		}
		else if(lv_type.equals("int_array")) {
			return "i32*"; // not sure
		}
    	
		// else - what to put??
		return "";
    }
}
