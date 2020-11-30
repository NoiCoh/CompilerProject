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
	int branchCounter = 0;
	int arrAllocCounter = 0;
    
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
    
    public void printStart() {
    	builder.append("declare i8* @calloc(i32, i32)\n");
    	builder.append("declare i32 @printf(i8*, ...)\n");
    	builder.append("declare void @exit(i32)\n");
    	builder.append("\n");
    	builder.append("@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n");
    	builder.append("@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n");
    	builder.append("define void @print_int(i32 %i) {\n");
    	indent++;
    	appendWithIndent("%_str = bitcast [4 x i8]* @_cint to i8*\n");
    	appendWithIndent("call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n");
    	appendWithIndent("ret void\n");
    	indent--;
    	appendWithIndent("}\n");
    	builder.append("\n");
    	builder.append("define void @throw_oob() {\n");
    	indent++;
    	appendWithIndent("%_str = bitcast [15 x i8]* @_cOOB to i8*\n");
    	appendWithIndent("call i32 (i8*, ...) @printf(i8* %_str)\n");
    	appendWithIndent("call void @exit(i32 1)\n");
    	appendWithIndent("ret void\n");
    	indent--;
    	appendWithIndent("}\n");
    }

    private void visitBinaryExpr(BinaryExpr e, String infixSymbol) {
    	e.e1().accept(this);
    	e.e2().accept(this);
    	Entry e2 = registers_queue.pop();
    	Entry e1 = registers_queue.pop();
    	
    	appendWithIndent("%_" + counter +" = ");
    	registers_queue.add(new Entry("%_" + counter,"i32"));
    	counter++;
    	
    	builder.append(infixSymbol + " i32 ");
    	builder.append(e1.getVarName() +", " + e2.getVarName());     
        builder.append("\n");
    }


    @Override
    public void visit(Program program) {
    	printStart();
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
        builder.append("i8* %this");
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
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }
        
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
        sysoutStatement.arg().accept(this);
    	if(registers_queue.isEmpty()) {
    		return;
    	}
        Entry arg = registers_queue.pop();
        appendWithIndent("call void (i32) @print_int(i32 " + arg.getVarName() + ")\n");
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
    	appendWithIndent("%_" + counter + " = load i32*, i32** %"+ assignArrayStatement.lv() +"\n");
    	registers_queue.add(new Entry("%_" + counter ,"i32*"));
    	counter++;
    	//Check that the index is greater than zero
    	assignArrayStatement.index().accept(this);
    	Entry index = registers_queue.pop();
    	StringBuilder arr_alloc = checkSizeArray(index.getVarName(),"0", true);
    	builder.append(arr_alloc.toString());
    	
    	//Load the size of the array (first integer of the array)
    	Entry array = registers_queue.pop();
    	appendWithIndent("%_" + counter + " = getelementptr i32, " + array.getType() + " " + array.getVarName() +", i32 0\n");
    	
    	registers_queue.add(new Entry("%_" + counter ,"i32"));
    	counter++;
    	Entry entry = registers_queue.pop();

    	appendWithIndent("%_" + counter + " = load i32, i32* " + entry.getVarName()+"\n");
    	registers_queue.add(new Entry("%_" + counter ,"i32"));
    	counter++;
    	entry = registers_queue.pop();
    	
    	//Check that the index is less than the size of the array
    	arr_alloc = checkSizeArray(entry.getVarName(),index.getVarName(),false);
    	builder.append(arr_alloc.toString());

    	//We'll be accessing our array at index + 1, since the first element holds the size
    	appendWithIndent("%_" + counter + " = add i32 " +index.getVarName() +", 1\n");
    	registers_queue.add(new Entry("%_" + counter ,"i32"));
    	counter++;
    	
    	//Get pointer to the i + 1 element of the array
    	entry = registers_queue.pop();
    	appendWithIndent("%_" + counter + " = getelementptr i32, i32* " + array.getVarName()+", i32 " + entry.getVarName() +"\n");
    	registers_queue.add(new Entry("%_" + counter ,"i32"));
    	counter++;
    	
    	//And store 1 to that address
    	entry = registers_queue.pop();
    	appendWithIndent("store i32 "+ (Integer.parseInt(index.getVarName()) + 1) + ", i32* "+ entry.getVarName() +"\n");
        assignArrayStatement.rv().accept(this);
    }
    
    @Override
    public void visit(ArrayAccessExpr e) {
    	e.arrayExpr().accept(this);
    	
    	//Check that the index is greater than zero
    	e.indexExpr().accept(this);
    	Entry index = registers_queue.pop();
    	StringBuilder arr_alloc = checkSizeArray(index.getVarName(),"0", true);
    	builder.append(arr_alloc.toString());
    	
    	//Load the size of the array (first integer of the array)
    	Entry array = registers_queue.pop();
  
    	appendWithIndent("%_" + counter + " = getelementptr i32, " + array.getType() + " " + array.getVarName() +", i32 0\n");
    	
    	registers_queue.add(new Entry("%_" + counter ,"i32"));
    	counter++;
    	Entry entry = registers_queue.pop();

    	appendWithIndent("%_" + counter + " = load i32, i32* " + entry.getVarName()+"\n");
    	registers_queue.add(new Entry("%_" + counter ,"i32"));
    	counter++;
    	entry = registers_queue.pop();
    	
    	//Check that the index is less than the size of the array
    	arr_alloc = checkSizeArray(entry.getVarName(),index.getVarName(),false);
    	builder.append(arr_alloc.toString());
    	
    	//We'll be accessing our array at index + 1, since the first element holds the size
    	appendWithIndent("%_" + counter + " = add i32 " +index.getVarName() +", 1\n");
    	registers_queue.add(new Entry("%_" + counter ,"i32"));
    	counter++;
    	
    	//Get pointer to the i + 1 element of the array
    	entry = registers_queue.pop();
    	appendWithIndent("%_" + counter + " = getelementptr i32, i32* " + array.getVarName()+", i32 " + entry.getVarName() +"\n");
    	registers_queue.add(new Entry("%_" + counter ,"i32"));
    	counter++;
    	
    	//Load the value
    	entry = registers_queue.pop();
    	appendWithIndent("%_" + counter + " = load i32, i32* " + entry.getVarName()+ "\n");
    	registers_queue.add(new Entry("%_" + counter ,"i32"));
    	counter++;
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

    @Override
    public void visit(ThisExpr e) {
//    	%_3 = bitcast i8* %this to i8***
    	appendWithIndent("%_"+ counter + " = bitcast i8* %this to i8***\n");
    	registers_queue.add(new Entry("%_" + counter,"i8*"));
    	counter++;
    }

    @Override
    public void visit(NewIntArrayExpr e) {
    	e.lengthExpr().accept(this);
    	Entry len = registers_queue.pop();
    	
    	//Check that the size of the array is not negative
    	StringBuilder arr_alloc = checkSizeArray(len.getVarName(),"0" ,true);
    	builder.append(arr_alloc.toString());

//      We need an additional int worth of space, to store the size of the array.
//      Allocate sz + 1 integers (4 bytes each)
    	appendWithIndent("%_" + counter + " = add i32 " + len.getVarName() + ", 1\n");
    	registers_queue.add(new Entry("%_" + counter, "i32"));
    	counter++;
    	Entry entry = registers_queue.pop();
    	appendWithIndent("%_" + counter + " = call i8* @calloc(i32 4, i32 " + entry.getVarName() + ")\n");
    	registers_queue.add(new Entry("%_" + counter, "i8*"));
    	counter++;
    	entry = registers_queue.pop();
    	
//      Cast the returned pointer
    	appendWithIndent("%_" + counter + " = bitcast i8* " + entry.getVarName() + " to i32*\n");
    	
//    	Store the size of the array in the first position of the array
    	appendWithIndent("store i32 " + len.getVarName() +", i32* %_" + counter + "\n");
    	registers_queue.add(new Entry("%_" + counter, "i32*"));
    	counter++;
    }
    
    public StringBuilder checkSizeArray(String value1, String value2, boolean is_slt) {
    	StringBuilder arr_alloc = new StringBuilder();
    	if(is_slt == true) {
    		arr_alloc.append("\t%_" + counter + " = icmp slt i32 " + value1 + ", " + value2 +"\n");
    	}
    	else {
    		arr_alloc.append("\t%_" + counter + " = icmp sle i32 " + value1 + ", " + value2 +"\n");
    	}
    	
    	arr_alloc.append("\tbr i1 %_" + counter + ", label %arr_alloc" + arrAllocCounter +", ");
    	counter++;
    	arr_alloc.append("label %arr_alloc" + (arrAllocCounter + 1) +"\n");
    	arr_alloc.append("arr_alloc" + arrAllocCounter + ":\n");
    	arr_alloc.append("\tcall void @throw_oob()\n");
    	arrAllocCounter++;
    	arr_alloc.append("\tbr label %arr_alloc" + arrAllocCounter + "\n");
    	
    	//All ok, we can proceed with the allocation
    	arr_alloc.append("arr_alloc" + arrAllocCounter + ":\n");
    	arrAllocCounter++;
    	return arr_alloc;
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
