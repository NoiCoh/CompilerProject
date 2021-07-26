
# Welcome to the compiler project 2020!
## Implemention of a compiler for an object-oriented language called MiniJava into LLVM assembly/IR

MiniJava is (almost exactly) a subset of Java, defined in the Appendix of Appel and Palsberg's Modern Compiler Implementation in Java, 2nd edition and described on the MiniJava web site- http://www.cambridge.org/resources/052182060X/

### Main Tasks:

**AST Analysis: Variable & Method Renaming**
Implemention of variable & method renaming on MiniJava ASTs (Abstract Syntax Trees).
- Understanding scoping and resolving variables' name.
- Traversing the class inheritance hierarchy.
- Resolving method calls according to the static type of the object.

**Code Generation:**
translates a MiniJava AST into equivalent code in the intermediate representation used by the LLVM compiler project.

**Semantic Checks:**
In this part of the project we validate a MiniJava AST to ensure that it conforms to the MiniJava specification, and in particular satisfies all the assumptions we have utilized for code generation.

**Lexing & Parsing:**
Generates ASTs from textual representations of MiniJava programs.

**********************************************************************************************************************

=== Structure ===

build.xml
	(directives for compiling the project using 'ant')

build/
	(temp directory created when you build)

examples/
	ast/
		(examples of AST XMLs representing Java programs)

	(more examples to come for each exercise)

schema/
	ast.xsd
		(XML schema for ASTs)

src/
	(where all your stuff is going to go)

	ast/*
		(Java representation of AST, including XML marshaling & unmarshaling, Visitor interface, and printing to Java. Some files to note:)

		AstXMLSerializer.java
			(for converting ASTs between XML <-> Java classes)

		AstPrintVisitor.java
			(printing AST as a Java program)

		Visitor.java
			(visitor interface)

		Program.java
			(the root of the AST)

	cup/
		Parser.cup
		(directives for CUP)

	jflex/
		Scanner.jfled
		(directives for JFlex)

	Main.java
		(main file, including a skeleton for the command line arguments we will use in the exercises. already does XML marshaling and unarmshaling and printing to Java)

	Lexer.java
		(generated when you build)

	Parser.java
		(generated when you build)

	sym.java
		(generated when you build)	

tools/*
	(third party JARs for lexing & parsing  and XML manipulation)

mjava.jar
	(*the* build)

README.md
	(<-- you are here)

=== Compiling the project ===
ant

=== Cleaning ===
ant clean

=== From AST XML to Java program ===
java -jar mjavac.jar unmarshal print examples/BinaryTree.xml res.java

=== From AST XML to... AST XML ===
java -jar mjavac.jar unmarshal marshal examples/BinaryTree.xml res.xml
