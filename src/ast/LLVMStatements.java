package ast;

import java.util.*;

import ast.SymbolTable.Scope;
import ast.SymbolTable.Symb;

public class LLVMStatements extends LLVMVisitor {

	private StringBuilder builder = new StringBuilder();
    ArrayList<SymbolTable> symbol_tables;
    String curr_class;
    int branchCounter = 0;
    
    Stack<Entry> registers_queue  = new Stack<>();
	String curr_method = "";
    
    private int indent = 0;
    
    public LLVMStatements(ArrayList<SymbolTable> symbol_tables, String curr_class) {
		super(symbol_tables);
		this.curr_class = curr_class;
		// TODO Auto-generated constructor stub
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
    public void visit(MethodDecl methodDecl) {
    	indent++;
    	curr_method = methodDecl.name();
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }
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
        for (var s : blockStatement.statements()) {
            s.accept(this);
        }
        builder.append("\n");
    }

    @Override
    public void visit(IfStatement ifStatement) {
    	//br i1 %_1, label %if0, label %if1
        ifStatement.cond().accept(this);
        Entry e = registers_queue.pop();
        appendWithIndent("br " + e.getType() +" " + e.getVarName() + ", label %if"+ branchCounter + ", ");
        branchCounter++;
        builder.append("label %if"+ branchCounter+"\n");
        branchCounter--;
        builder.append("if" + branchCounter + ":\n");
        branchCounter++;
        ifStatement.thencase().accept(this);
        branchCounter++;
        // br label %if2
        appendWithIndent("br label %if" + branchCounter + "\n");
        branchCounter--;
        builder.append("if" + branchCounter + ":\n");
        ifStatement.elsecase().accept(this);
        branchCounter++;
        // br label %if2
        appendWithIndent("br label %if" + branchCounter + "\n");
        builder.append("if" + branchCounter +":\n");
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
    	assignStatement.rv().accept(this);
    	//store i32 %_11, i32* %num_aux	
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
	    		}
			}
		}
		String type = typeConvertor(lv_type);
		builder.append(type+ " ");
		if(!registers_queue.empty()) {
			String register = registers_queue.pop().getVarName();
			builder.append(register +", " + type+ "* ");	
		    builder.append("%");
		    builder.append(assignStatement.lv());
		    builder.append("\n");
		}
		else {
			System.out.println("problem in visit(AssignStatement assignStatement)");
		}
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
    public void visit(LtExpr e) { // %_1 = icmp slt i32 %_0, 1
        e.e1().accept(this);
        e.e2().accept(this);
        appendWithIndent("%_" + counter + " = " + "icmp slt ");
        if(registers_queue.size() >= 2) {
        	Entry e1 = registers_queue.pop();
        	Entry e2 = registers_queue.pop();
        	builder.append(e1.getType() + " " + e1.getVarName() +", " + e2.getVarName() +"\n");
        	registers_queue.add(new Entry("%_" + counter,"i1"));
        	counter++;
        }      
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
    	e.ownerExpr().accept(this);
    	
//		%_4 = load i8**, i8*** %_3
    	if(!registers_queue.empty()) {
    		Entry entry = registers_queue.pop();
    		appendWithIndent("%_" + counter + " = load " + entry.getType() + "*, "+ entry.getType() + "** " + entry.getVarName() + "\n");
    		counter++;
    		
//    	%_5 = getelementptr i8*, i8** %_4, i32 0
    		appendWithIndent("%_" + counter + " = getelementptr " + entry.getType()+", " + entry.getType() +"* ");
    		counter--;
    		SymbolTable curr_symbol_table = returnCurrTable(curr_class);
    		
    	    if(curr_symbol_table != null) {
        		String curr_method_ret = curr_symbol_table.curr_scope.findSymbolType(e.methodId(),enumKind.method);
        		String convert_ret_type = typeConvertor(curr_method_ret);
    	    	
    	    	Scope curr_method_scope = curr_symbol_table.findScope(e.methodId(),scopeType.method);
    	    	builder.append("%_" + counter + ", " + "i32 " + curr_method_scope.vtable_index + "\n");
//    	    	%_6 = load i8*, i8** %_5
        	    counter +=2;
        	    appendWithIndent("%_" + counter + " = load " + entry.getType() + ", " + entry.getType() +"* ");
        	    counter--;
        	    builder.append("%_" + counter + "\n");
        	    counter +=2;
//        		%_7 = bitcast i8* %_6 to i32 (i8*, i32)*
        	    appendWithIndent("%_" + counter + " = bitcast " + entry.getType());
        	    registers_queue.add(new Entry("%_" + counter, entry.getType()));
        	    counter--;
        	    builder.append(" %_" + counter + " to "+ convert_ret_type + " (i8*");
        	    for( Symb sym : curr_method_scope.locals.values()) {
        	    	if(sym.kind == enumKind.arg) {
        	    		builder.append(", ");
        	    		String convert_arg_type = typeConvertor(sym.decl);
        	    		builder.append(convert_arg_type);
        	    	}
        	    }
        	    builder.append(")*\n");
        	    counter +=2;
        	    
//        		%_8 = load i32, i32* %num
//            	%_9 = sub i32 %_8, 1
                String delim = "";
                ArrayList<Entry> args= new ArrayList<Entry>();
                for (Expr arg : e.actuals()) {
                    builder.append(delim);
                    arg.accept(this);
                    delim = ", ";
                    if(!registers_queue.empty()) {
                    	args.add(registers_queue.pop());
                    }
                }
//              %_10 = call i32 %_7(i8* %this, i32 %_9)
                
        	    appendWithIndent("%_" + counter + " = call " + convert_ret_type + " " + registers_queue.pop().getVarName());
        	    registers_queue.add(new Entry("%_" + counter, convert_ret_type));
        	    counter++;
        	    builder.append("(i8* %this");
        	    for(Entry arg : args) {
        	    	builder.append(", ");
        	    	builder.append(arg.getType() + " " + arg.getVarName());
        	    }
        	    builder.append(")\n");
        	    
    	    }
    	}
        
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

    public void visit(ThisExpr e) {
//    	%_3 = bitcast i8* %this to i8***
    	appendWithIndent("%_"+ counter + " = bitcast i8* %this to i8***\n");
    	registers_queue.add(new Entry("%_" + counter,"i8*"));
    	counter++;
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

	@Override
	public void visit(Program program) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ClassDecl classDecl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(MainClass mainClass) {
		// TODO Auto-generated method stub
		
	}
}
