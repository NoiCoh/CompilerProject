import ast.*;


import java.io.*;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        try {
            var inputMethod = args[0];
            var action = args[1];
            var filename = args[args.length - 2];
            var outfilename = args[args.length - 1];

            Program prog;
            if (inputMethod.equals("parse")) {
            	var outFile = new PrintWriter(outfilename);
            	FileReader fileReader = new FileReader(new File(filename));
            	Parser p = new Parser(new Lexer(fileReader));
            	prog = (Program) p.parse().value;
                AstPrintVisitor astPrinter = new AstPrintVisitor();
                prog.accept(astPrinter);
                System.out.println(astPrinter.getString());
            } else if (inputMethod.equals("unmarshal")) {
                AstXMLSerializer xmlSerializer = new AstXMLSerializer();
                prog = xmlSerializer.deserialize(new File(filename));
            } else {
                throw new UnsupportedOperationException("unknown input method " + inputMethod);
            }

            var outFile = new PrintWriter(outfilename);
            try {
                ArrayList<SymbolTable> symbol_tables = new ArrayList<SymbolTable>();
                CreateSymbolTable create = new CreateSymbolTable(symbol_tables);
                create.visit(prog);
//                for (SymbolTable symbol_table : symbol_tables) {
//                	symbol_table.printTable();
//                }

                if (action.equals("marshal")) {
                    AstXMLSerializer xmlSerializer = new AstXMLSerializer();
                    xmlSerializer.serialize(prog, outfilename);
                } else if (action.equals("print")) {
                    AstPrintVisitor astPrinter = new AstPrintVisitor();
                    astPrinter.visit(prog);
                    outFile.write(astPrinter.getString());

                } else if (action.equals("semantic")) {
                	// check if we found error while building the symbol table.
                	if(!create.getValidatorResult()) {
                		outFile.write("ERROR\n");
                		// for testing only - remove in submission.
                		System.out.println(create.getValidatorMsg());
                	}
                	else {
                		ValidatorVisitor validator = new ValidatorVisitor(symbol_tables);
                        validator.visit(prog);
                        // for testing only - remove in submission.
                        System.out.println(validator.getValidatorMsg());
                        
                        ValidatorInitialization validatorInit = new ValidatorInitialization(validator.getResult(), symbol_tables);
                        validatorInit.visit(prog);
                        outFile.write(validatorInit.getResult());
                        // for testing only - remove in submission.
                        System.out.println(validatorInit.getValidatorMsg());
                	}
                } else if (action.equals("compile")) {
                	LLVMVisitor llPrinter = new LLVMVisitor(symbol_tables);
                	llPrinter.visit(prog);
	                outFile.write(llPrinter.getString());

                } else if (action.equals("rename")) {
                    var type = args[2];
                    var originalName = args[3];
                    var originalLine = args[4];
                    var newName = args[5];

                    boolean isMethod;
                    	
                    if (type.equals("var")) {
                        isMethod = false;
                        RenameVarVisitor varVisitor = new RenameVarVisitor(originalName,newName, Integer.parseInt(originalLine), symbol_tables); 
                        varVisitor.visit(prog);

                    } else if (type.equals("method")) {
                        isMethod = true;
                        RenameMethodVisitor varVisitor = new RenameMethodVisitor(originalName,newName, Integer.parseInt(originalLine), symbol_tables); 
                        varVisitor.visit(prog);
                        
                    } else {
                        throw new IllegalArgumentException("unknown rename type " + type);
                    }
                    
                    AstXMLSerializer xmlSerializer = new AstXMLSerializer();
                    xmlSerializer.serialize(prog, outfilename);
                } else {
                    throw new IllegalArgumentException("unknown command line action " + action);
                }
            } finally {
                outFile.flush();
                outFile.close();
            }

        } catch (FileNotFoundException e) {
            System.out.println("Error reading file: " + e);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("General error: " + e);
            e.printStackTrace();
            

        }
    }
}
