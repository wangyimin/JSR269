package com.wang.JSR269;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

//javac -cp "C:\Program Files\Java\jdk1.8.0_151\lib\tools.jar" GetterSetter*.java -d .
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.wang.JSR269.GetterSetter")
public class GetterSetterProcessor extends AbstractProcessor {
	private Messager messager;
	private JavacTrees javacTrees;
	private TreeMaker treeMaker;
	private Names names;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		this.messager = processingEnv.getMessager();
		this.javacTrees = JavacTrees.instance(processingEnv);
		Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
		this.treeMaker = TreeMaker.instance(context);
		this.names = Names.instance(context);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(GetterSetter.class);
		elementsAnnotatedWith.forEach(e -> {
			JCTree tree = javacTrees.getTree(e);
			tree.accept(new TreeTranslator() {
				@Override
				public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
					List<JCTree.JCVariableDecl> jcVariableDeclList = List.nil();
					for (JCTree jcTree : jcClassDecl.defs) {
						if (jcTree.getKind().equals(Tree.Kind.VARIABLE)) {
							JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) jcTree;
							jcVariableDeclList = jcVariableDeclList.append(jcVariableDecl);
						}
					}
					jcVariableDeclList.forEach(jcVariableDecl -> {
						messager.printMessage(Diagnostic.Kind.NOTE, "getter&setter for [" + jcVariableDecl.getName() + "] has been processed.");
						jcClassDecl.defs = jcClassDecl.defs.prepend(makeGetterMethodDecl(jcVariableDecl))
								.prepend(makeSetterMethodDecl(jcVariableDecl));
					});
					super.visitClassDef(jcClassDecl);
				}
			});
		});
		return true;
	}

	private JCTree.JCMethodDecl makeGetterMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {

		ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();

		// generate return value
		statements.append(treeMaker
				.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName())));
		JCTree.JCBlock body = treeMaker.Block(0, statements.toList());

		return treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC), getGetterMethodName(jcVariableDecl.getName()),
				jcVariableDecl.vartype, List.nil(), List.nil(), List.nil(), body, null);
	}

	private JCTree.JCMethodDecl makeSetterMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {
		ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
		// this.variable = argument
		JCTree.JCExpressionStatement aThis = makeAssignment(
				treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName()),
				treeMaker.Ident(jcVariableDecl.getName()));
		statements.append(aThis);
		JCTree.JCBlock block = treeMaker.Block(0, statements.toList());

		// generate argument
		JCTree.JCVariableDecl param = treeMaker.VarDef(treeMaker.Modifiers(Flags.PARAMETER), jcVariableDecl.getName(),
				jcVariableDecl.vartype, null);
		List<JCTree.JCVariableDecl> parameters = List.of(param);

		// generate return value
		JCTree.JCExpression methodType = treeMaker.Type(new Type.JCVoidType());
		return treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC), getSetterMethodName(jcVariableDecl.getName()),
				methodType, List.nil(), parameters, List.nil(), block, null);

	}

	private Name getGetterMethodName(Name name) {
		String s = name.toString();
		return names.fromString("get" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
	}

	private Name getSetterMethodName(Name name) {
		String s = name.toString();
		return names.fromString("set" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
	}

	private JCTree.JCExpressionStatement makeAssignment(JCTree.JCExpression lhs, JCTree.JCExpression rhs) {
		return treeMaker.Exec(treeMaker.Assign(lhs, rhs));
	}
}