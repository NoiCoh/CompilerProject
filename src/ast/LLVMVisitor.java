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
	String callerClass = "";
	int formalsStep = 0;
	int branchCounter = 0;
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
     
    public void printVtable() {
    	for( SymbolTable symbol_table : symbol_tables) {
    		Scope curr_class = symbol_table.curr_scope;
    		if(curr_class.type == scopeType.type_class && !(curr_class.name.equals("Main")) && curr_class.num_of_methods != 0) {
    			builder.append("@." + curr_class.name + "_vtable = global ["+ curr_class.num_of_methods + " x i8*] [");
    			Symb[] orderindex = new Symb[curr_class.num_of_methods];
    			for(Symb entry : curr_class.locals) {
    				if(entry.kind == enumKind.method || entry.kind == enumKind.method_extend) {
    					orderindex[entry.vtableindex] = entry;
    				}
    			}
    			for(int i=0; i<curr_class.num_of_methods ; i++) {
    				Symb entry = orderindex[i];
    				String convert_ret_type = typeConvertor(entry.decl);
    				Scope curr_method = null;
    				if(entry.extendFrom.equals(curr_class.name)) {
    					curr_method = symbol_table.findScope(entry.name, scopeType.method);
    				}
    				else {
    					SymbolTable extendSymbolTable = returnCurrTable(entry.extendFrom);
    					curr_method = extendSymbolTable.findScope(entry.name, scopeType.method);
    				}
    				if(curr_method != null) {
						builder.append("i8* bitcast (" + convert_ret_type + " (i8*");
		        	    for( Symb sym : curr_method.locals) {
		        	    	if(sym.kind == enumKind.arg) {
		        	    		builder.append(", ");
								String convert_arg_type = typeConvertor(sym.decl);
								builder.append(convert_arg_type);
		        	    	}
		        	    }
		        	    builder.append(")* @"+ entry.extendFrom + "."+ entry.name +" to i8*)");
			        	if(i < curr_class.num_of_methods - 1) {
			        		builder.append(", ");
		    			}
					}	
    			}
    			builder.append("]\n"); 
    		}
    	}
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
    
    private boolean isInt(String s){
    	try{
    		int i = Integer.parseInt(s);
    		return true;
    	}
    	catch(NumberFormatException er){
    		return false;
    	}
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
    	printVtable();
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
    	counter = 0;
    	curr_class = classDecl.name();

        indent++;
//        for (var fieldDecl : classDecl.fields()) {
//            fieldDecl.accept(this);
//            builder.append("\n");
//        }
        for (var methodDecl : classDecl.methoddecls()) {
        	counter = 0;
        	branchCounter = 0;
            methodDecl.accept(this);
            builder.append("\n");
        }
        indent--;
    }

    @Override
    public void visit(MainClass mainClass) {
    	curr_class = "";
    	builder.append("define i32 @main() {\n");
    	indent++;
        mainClass.mainStatement().accept(this);
        appendWithIndent("ret i32 0\n");
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
    		builder.append("* %" + formalArg.name() +"\n");
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
    	//Return the condition: br i1 %_1, label %if0, label %if1
        ifStatement.cond().accept(this);
        Entry e = registers_queue.pop();
        
        // Init the branches' names
        int trueBranch = branchCounter;
        branchCounter++;
        int falseBranch = branchCounter;
        branchCounter++;
        int afterBranch = branchCounter;
        branchCounter++;
        
        // True branch
        appendWithIndent("br " + e.getType() +" " + e.getVarName() + ", label %if"+ trueBranch + ", label %if"+ falseBranch+"\n");
        builder.append("if" + trueBranch + ":\n");
        ifStatement.thencase().accept(this);
        appendWithIndent("br label %if" + afterBranch + "\n");
        
        // False branch
        builder.append("if" + falseBranch + ":\n");
        ifStatement.elsecase().accept(this);
        appendWithIndent("br label %if" + afterBranch + "\n");
        
        // After..
        builder.append("if" + afterBranch +":\n");  
    }

    @Override
    public void visit(WhileStatement whileStatement) {
    	// Init the branches' names
        int condloop = branchCounter;
        branchCounter++;
        int trueloop = branchCounter;
        branchCounter++;
        int falseloop = branchCounter;
        branchCounter++;
        
    	// Cond loop
        appendWithIndent("br label %loop" + condloop +"\n");
        appendWithIndent("loop" + condloop +":\n");
        whileStatement.cond().accept(this);
        appendWithIndent("br i1 " + registers_queue.pop().getVarName() + ", label %loop" + trueloop +", label %loop" + falseloop + "\n");

        // True loop
        appendWithIndent("loop" + trueloop +":\n");
        whileStatement.body().accept(this);
        appendWithIndent("br label %loop"+ condloop +"\n");
        
        // False loop
        appendWithIndent("loop" + falseloop +":\n");
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
    	if(!registers_queue.empty()) {
	    	String rv_val = registers_queue.pop().getVarName();
	    	assignFieldOrVar(assignStatement.lv());
	    	Entry lv_val = registers_queue.pop();
			String type = lv_val.getType();
			appendWithIndent("store " + type+ " " + rv_val +", " + type + "* " + lv_val.getVarName() + "\n");
    	}
    	else {
    		System.out.println("problem in AssignStatement registers_queue is empty");
    	}
    }
    
    private void assignFieldOrVar(String var) {
    	String lv_type = "";
    	SymbolTable curr_symbol_table = returnCurrTable(curr_class);
    	if(curr_symbol_table != null) {
    		enumKind kind = enumKind.field;
    		lv_type = curr_symbol_table.curr_scope.findSymbolType(var, enumKind.field);
    		if(lv_type.equals("")) {
    			lv_type = curr_symbol_table.curr_scope.findSymbolType(var,enumKind.field_extend);
    			kind = enumKind.field_extend;
    		}
    		if(!lv_type.equals("")) {
    			//Get pointer to the byte where the field starts
    			/////////////////check if always %this//////////////////////
    			ArrayList<String> decl = new ArrayList<String>();
    			decl.add(lv_type);
    			Symb field = curr_symbol_table.curr_scope.findSymbol(var, kind, decl);
    			appendWithIndent("%_" + counter + " = getelementptr i8, i8* %this, i32 " + field.vtableindex +"\n");
    			registers_queue.add(new Entry("%_" + counter ,"i8*"));
    			counter++;
    			//Cast to a pointer to the field with the correct type
    			appendWithIndent("%_" + counter + " = bitcast i8* " + registers_queue.pop().getVarName() +" to " + typeConvertor(lv_type) + "*\n");
    			registers_queue.add(new Entry("%_" + counter , typeConvertor(lv_type)));
    			counter++;
    		}
    		else {
				// find if originalName defined in method
	    		Scope curr_method_scope = curr_symbol_table.findScope(curr_method,scopeType.method);
	    		if(curr_method_scope != null) {
	    			lv_type = curr_method_scope.findSymbolType(var, enumKind.var);
	    			registers_queue.add(new Entry("%" +var, typeConvertor(lv_type)));
	    		}
			}
    	}			
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
    	// lv handling
    	assignFieldOrVar(assignArrayStatement.lv());
    	Entry lv_val = registers_queue.pop();
    	appendWithIndent("%_" + counter + " = load i32*, i32** "+ lv_val.getVarName() +"\n");
    	registers_queue.add(new Entry("%_" + counter ,"i32"));
    	counter++;
    	
    	Entry array = registers_queue.pop();
    	
    	//index handling
    	assignArrayStatement.index().accept(this);
    	String index = registers_queue.pop().getVarName();
    	
    	// rv handling
    	assignArrayStatement.rv().accept(this);
    	Entry rv_val = registers_queue.pop();
    	
    	//Check that the index is greater than zero
    	StringBuilder arr_alloc = checkSizeArray(index,"0", true);
    	builder.append(arr_alloc.toString());
    	
    	//Load the size of the array (first integer of the array)
    	appendWithIndent("%_" + counter + " = getelementptr " + array.getType() + ", " + array.getType() + "* " + array.getVarName() +" , i32 0\n");
    	
    	registers_queue.add(new Entry("%_" + counter ,"i32"));
    	counter++;
    	Entry entry = registers_queue.pop();

    	appendWithIndent("%_" + counter + " = load i32, i32* " + entry.getVarName()+"\n");
    	registers_queue.add(new Entry("%_" + counter ,"i32"));
    	counter++;
    	entry = registers_queue.pop();
    	
    	//Check that the index is less than the size of the array
    	arr_alloc = checkSizeArray(entry.getVarName(),index,false);
    	builder.append(arr_alloc.toString());

    	//We'll be accessing our array at index + 1, since the first element holds the size
		appendWithIndent("%_" + counter + " = add i32 " +index +", 1\n");
		registers_queue.add(new Entry("%_" + counter ,"i32"));
		counter++;
	
		//Get pointer to the i + 1 element of the array
		entry = registers_queue.pop();
		appendWithIndent("%_" + counter + " = getelementptr i32, i32* " + array.getVarName()+", i32 " + entry.getVarName() +"\n");
		registers_queue.add(new Entry("%_" + counter ,"i32"));
		counter++;
		
    	entry = registers_queue.pop();
    	appendWithIndent("store i32 "+ rv_val.getVarName() + ", i32* "+ entry.getVarName() +"\n");

    	//assignArrayStatement.rv().accept(this);
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
  
    	appendWithIndent("%_" + counter + " = getelementptr i32, i32* " + array.getVarName() +", i32 0\n");
    	
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
    	
        int andBranch = branchCounter;
        branchCounter++;
        int e2_check = branchCounter;
        branchCounter++;
        int after_e2_check = branchCounter;
        branchCounter++;
        int ending_branch = branchCounter;
        branchCounter++;
        
        e.e1().accept(this);
        Entry e1 = registers_queue.pop();
        appendWithIndent("br label %andcond" + andBranch + "\n");
        builder.append("andcond" + andBranch + ":\n");
        appendWithIndent("br i1 " + e1.getVarName() +", label %andcond" + e2_check +", label %andcond" + ending_branch +"\n");
        
        builder.append("andcond" + e2_check + ":\n");
        e.e2().accept(this);
        Entry e2 = registers_queue.pop();
        appendWithIndent("br label %andcond" + after_e2_check + "\n");
        
        builder.append("andcond" + after_e2_check + ":\n");
        appendWithIndent("br label %andcond" + ending_branch + "\n");
        
        builder.append("andcond" + ending_branch + ":\n");
        appendWithIndent("%_" + counter + " = phi i1 [0 , %andcond" +andBranch + "], ["+ e2.getVarName() + ", %andcond" + after_e2_check +"]\n");
        registers_queue.add(new Entry("%_" + counter ,"i1"));
        counter++;
    }
    
    @Override
    public void visit(LtExpr e) { // %_1 = icmp slt i32 %_0, 1 
        e.e1().accept(this);
        e.e2().accept(this);
        appendWithIndent("%_" + counter + " = " + "icmp slt ");
        if(registers_queue.size() >= 2) {
        	Entry e2 = registers_queue.pop();
        	Entry e1 = registers_queue.pop();
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
        e.arrayExpr().accept(this);
        Entry arrayExpr = registers_queue.pop();
    	appendWithIndent("%_"+ counter + " = load i32, i32* "+ arrayExpr.getVarName() + "\n");
    	registers_queue.add(new Entry("%_" + counter, "i32"));
    	counter++;

    	
    }

    @Override
    public void visit(MethodCallExpr e) {
    	e.ownerExpr().accept(this);
    	
    	if(!registers_queue.empty()) {
    		
    		//required bitcasts, so that we can access the vtable pointer
    		Entry ownerMathod = registers_queue.pop();
        	appendWithIndent("%_"+ counter + " = bitcast i8* "+ ownerMathod.getVarName() +" to i8***\n");
        	registers_queue.add(new Entry("%_" + counter, "i8*"));
        	counter++;
        	
        	//Load vtable_ptr
    		Entry entry = registers_queue.pop();
    		appendWithIndent("%_" + counter + " = load " + entry.getType() + "*, "+ entry.getType() + "** " + entry.getVarName() + "\n");
    		registers_queue.add(new Entry("%_" + counter, entry.getType()));
    		counter++;
    		
    		//%_5 = getelementptr i8*, i8** %_4, i32 0
    		entry = registers_queue.pop();
    		appendWithIndent("%_" + counter + " = getelementptr " + entry.getType() +", " + entry.getType() +"* "+ entry.getVarName());
    		registers_queue.add(new Entry("%_" + counter, entry.getType()));
    		counter++;
    		Scope callerClassScope = returnCurrTable(callerClass).curr_scope;
    	    if(callerClassScope != null) {
    	    	//Get a pointer to the entry in the vtable  
    	    	ArrayList<enumKind> kind = new ArrayList<enumKind>();
    	    	kind.add(enumKind.method);
    	    	kind.add(enumKind.method_extend);
        		Symb curr_method_symb = callerClassScope.findSymbol(e.methodId(),kind);
        		String convert_ret_type = typeConvertor(curr_method_symb.decl);
        		builder.append(", " + "i32 " + curr_method_symb.vtableindex +"\n");
        		
        		//Read into the array to get the actual function pointer
        		entry = registers_queue.pop();
        		appendWithIndent("%_" + counter + " = load " + entry.getType() + ", " + entry.getType() +"* " + entry.getVarName() + "\n");
        		registers_queue.add(new Entry("%_" + counter, entry.getType()));
        		counter++;
        		
        		//Cast the function pointer from i8* to a function ptr type that matches the function's signature
        		entry = registers_queue.pop();
        		appendWithIndent("%_" + counter + " = bitcast " + entry.getType() + " " + entry.getVarName() + " to "+ convert_ret_type + " (i8*");
        		SymbolTable extendTable = returnCurrTable(curr_method_symb.extendFrom);
    	    	Scope curr_method_scope = extendTable.findScope(e.methodId(),scopeType.method);
    	    	// find the args of the function
    	    	if(curr_method_scope != null) {
	        	    for( Symb sym : curr_method_scope.locals) {
	        	    	if(sym.kind == enumKind.arg) {
	        	    		builder.append(", ");
	        	    		String convert_arg_type = typeConvertor(sym.decl);
	        	    		builder.append(convert_arg_type);
	        	    	}
	        	    }
	        	    builder.append(")*\n");
	        		registers_queue.add(new Entry("%_" + counter, entry.getType()));
	        		counter++;
	        	    
	                ArrayList<Entry> args = new ArrayList<Entry>();
	                for (Expr arg : e.actuals()) {
	                    arg.accept(this);
	                    if(!registers_queue.empty()) {
	                    	args.add(registers_queue.pop());
	                    }
	                }
	                
	                //Perform the call on the function pointer
	                if(!registers_queue.empty()) {
		        	    appendWithIndent("%_" + counter + " = call " + convert_ret_type + " " + registers_queue.pop().getVarName());
		        	    registers_queue.add(new Entry("%_" + counter, convert_ret_type));
		        	    counter++;
		        	    builder.append("(i8* " + ownerMathod.getVarName());
		        	    for(Entry arg : args) {
		        	    	builder.append(", ");
		        	    	builder.append(arg.getType() + " " + arg.getVarName());
		        	    }
		        	    builder.append(")\n");
	                }
	                else {
	                	System.out.println("problem in registers_queue is empty");
	                }
	    	    }
    	    	else {
    	    		System.out.println("problem in curr_method_scope is null");
    	    	}
	    	}
    	    else {
    	    	System.out.println("problem in callerClassScope is null");
    	    }
    	}
    }

    @Override
    public void visit(IntegerLiteralExpr e) {
    	registers_queue.add(new Entry("" + e.num(),"i32"));
    }

    @Override
    public void visit(TrueExpr e) {
    	registers_queue.add(new Entry("1","i1"));
    }

    @Override
    public void visit(FalseExpr e) {
    	registers_queue.add(new Entry("0","i1"));
    }

    @Override
    public void visit(IdentifierExpr e) {

    	SymbolTable curr_symbol_table = returnCurrTable(curr_class);
		String type = "";
		String val = "";
		if(curr_symbol_table != null) {
			enumKind kind = enumKind.field;
			type = curr_symbol_table.curr_scope.findSymbolType(e.id(), enumKind.field);
			if(type.equals("")) {
				type = curr_symbol_table.curr_scope.findSymbolType(e.id(), enumKind.field_extend);
				kind = enumKind.field_extend;
			}
			if(!type.equals("")) {
				//Get pointer to the byte where the field starts
				/////////////////check if always %this//////////////////////
				ArrayList<String> decl = new ArrayList<String>();
				decl.add(type);
				Symb field = curr_symbol_table.curr_scope.findSymbol(e.id(), kind, decl);
				appendWithIndent("%_" + counter + " = getelementptr i8, i8* %this, i32 " + field.vtableindex +"\n");
				registers_queue.add(new Entry("%_" + counter ,"i8*"));
				counter++;
				//Cast to a pointer to the field with the correct type
				appendWithIndent("%_" + counter + " = bitcast i8* " + registers_queue.pop().getVarName() +" to " + typeConvertor(type) + "*\n");
				registers_queue.add(new Entry("%_" + counter , typeConvertor(type) + "*"));
				counter++;
				val = registers_queue.pop().getVarName();
			}
			else {
				// find if originalName defined in method
		    	if(curr_symbol_table != null) {
		    		Scope curr_method_scope = curr_symbol_table.findScope(curr_method,scopeType.method);
		    		if(curr_method_scope != null) {
		    			type = curr_method_scope.findSymbolType(e.id(), enumKind.var);
		    			callerClass = type;
		    			val = "%" + e.id();
		    		}
				}
			}
			String convert_type = typeConvertor(type);
			appendWithIndent("%_"+ counter + " = load ");
			builder.append(convert_type+ ", " + convert_type+ "* " + val + "\n");
			registers_queue.add(new Entry("%_" + counter,convert_type));
			counter++;
		}
    }

    @Override
    public void visit(ThisExpr e) {
    	callerClass = curr_class;
    	registers_queue.add(new Entry("%this","i8*"));
    }

    @Override
    public void visit(NewIntArrayExpr e) {
    	e.lengthExpr().accept(this);
    	Entry len = registers_queue.pop();
    	
    	//Check that the size of the array is not negative
    	StringBuilder arr_alloc = checkSizeArray(len.getVarName(),"0" ,true);
    	builder.append(arr_alloc.toString());

		//We need an additional int worth of space, to store the size of the array.
		//Allocate sz + 1 integers (4 bytes each)
    	appendWithIndent("%_" + counter + " = add i32 " + len.getVarName() + ", 1\n");
    	registers_queue.add(new Entry("%_" + counter, "i32"));
    	counter++;
    	Entry entry = registers_queue.pop();
    	appendWithIndent("%_" + counter + " = call i8* @calloc(i32 4, i32 " + entry.getVarName() + ")\n");
    	registers_queue.add(new Entry("%_" + counter, "i8*"));
    	counter++;
    	entry = registers_queue.pop();
    	
    	//Cast the returned pointer
    	appendWithIndent("%_" + counter + " = bitcast i8* " + entry.getVarName() + " to i32*\n");
    	
    	//Store the size of the array in the first position of the array
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
    	
    	arr_alloc.append("\tbr i1 %_" + counter + ", label %arr_alloc" + branchCounter +", ");
    	counter++;
    	arr_alloc.append("label %arr_alloc" + (branchCounter + 1) +"\n");
    	arr_alloc.append("arr_alloc" + branchCounter + ":\n");
    	arr_alloc.append("\tcall void @throw_oob()\n");
    	branchCounter++;
    	arr_alloc.append("\tbr label %arr_alloc" + branchCounter + "\n");
    	
    	//All ok, we can proceed with the allocation
    	arr_alloc.append("arr_alloc" + branchCounter + ":\n");
    	branchCounter++;
    	return arr_alloc;
    }

    @Override
    public void visit(NewObjectExpr e) {
    	//allocate the required memory on heap - always 1 for object allocation
    	Scope currScope = returnCurrTable(e.classId()).curr_scope;
    	appendWithIndent("%_" + counter + " = call i8* @calloc(i32 1, i32 " + currScope.size_of_object +")\n");
    	registers_queue.add(new Entry("%_" + counter, "i8*"));
    	counter++;
    	
    	//set the vtable pointer to point to the correct vtable
    	Entry calloc_pointer = registers_queue.pop();
    	appendWithIndent("%_" + counter + " = bitcast i8* "+ calloc_pointer.getVarName() + " to i8***\n");
    	registers_queue.add(new Entry("%_" + counter, "i8*"));
    	counter++;
    	
    	// Get the address of the first element of the vtable
    	appendWithIndent("%_" + counter + " = getelementptr ["+ currScope.num_of_methods +" x i8*], ["+currScope.num_of_methods +" x i8*]* ");
    	builder.append("@." + e.classId() + "_vtable, i32 0, i32 0\n");
    	registers_queue.add(new Entry("%_" + counter, "i8*"));
    	counter++;
    	
    	//Set the vtable to the correct address.
    	Entry vtable_pointer = registers_queue.pop();
    	appendWithIndent("store i8** "+vtable_pointer.getVarName() +", i8*** " +registers_queue.pop().getVarName() +"\n");
    	
    	//save the allocate address
    	registers_queue.add(calloc_pointer);
    	callerClass = e.classId();
    }

    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
        appendWithIndent("%_" + counter + " = sub i1 1, " +registers_queue.pop().getVarName() + "\n");
        registers_queue.add(new Entry("%_" + counter, "i1"));
        counter++;
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
        builder.append("i8*");
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
    
    public String typeConvertor(String lv_type) {
    	if(lv_type.equals("int")) {
			return "i32";	
		}
		else if(lv_type.equals("bool")) {
			return "i1";	
		}
		else if(lv_type.equals("int_array")) {
			return "i32*";
		}
		return "i8*";
    }
    
}
