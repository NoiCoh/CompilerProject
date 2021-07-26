
# Welcome to the compiler project 2020!
## Implementation of a compiler for an object-oriented language.


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
