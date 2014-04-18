import syntaxtree.*;
import visitor.*;
import java.util.*;

public class Typecheck {
	public static void main(String[] args) {
		try {
			Node root = new MiniJavaParser(System.in).Goal();
			//System.out.println("Parse successful");

			List<ClassType> classList = new ArrayList<ClassType>();
			ClassVisitor cv = new ClassVisitor();
			root.accept(cv, classList);
			ClassExtendsVisitor cev = new ClassExtendsVisitor();
			root.accept(cev, classList);

			MethodVisitor mv = new MethodVisitor(classList);
			root.accept(mv, null);

			SymbolTableVisitor stv = new SymbolTableVisitor(classList);
			root.accept(stv, -1);
			
			/*
			System.out.println("---CLASS DECLARATIONS---");
			for (ClassType ct : classList)
				System.out.println(ct.className);
			
			System.out.println("---METHOD DECLARATIONS---");
			for (ClassType ct : classList) {
				for (Method m : ct.methodList) {
					System.out.print(ct.className + ": " + m.returnType.toString() + m.name + " (");
					for (JavaType t : m.argumentTypes)
						System.out.print(t + ", ");
					System.out.println(")");
				}
			}

			System.out.println("---SYMBOLS---");
			for (int i = 0; i < stv.symbolTables.size(); i++) {
				System.out.print("Scope " + stv.symbolTables.get(i).enclosingClass  + " "  + i + ": ");
				for (String identifier : stv.symbolTables.get(i).table.keySet())
					System.out.print(identifier + ", ");
				System.out.println();
			}
			*/

			TypecheckVisitor tv = new TypecheckVisitor(stv.symbolTables, classList);
			root.accept(tv, null);

			System.out.println("Program type checked successfully");
			System.exit(0);
		} catch (ParseException e) {
			System.err.println(e);
			System.exit(1);
		}
	}

	public static void typeError (String error) {
		System.out.println("Type error");
		//System.out.println(error);
		System.exit(1);
	}
}

class JavaType {
	// Checks that b is compatible with a, NOT that a is compatible with b
	public static boolean isCompatibleTypes(JavaType a, JavaType b) {
		if (a instanceof IntType && b instanceof IntType) {
			return true;
		} else if (a instanceof BoolType && b instanceof BoolType) {
			return true;
		} else if (a instanceof IntArrayType && b instanceof IntArrayType) {
			return true;
		} else if (a instanceof ClassType && b instanceof ClassType) {
			ClassType ca = (ClassType)a;
			ClassType cb = (ClassType)b;
			
			boolean isCompatible = false;
			while (cb != null) {
				if (ca.className.equals(cb.className)) {
					isCompatible = true;
					break;
				}
				cb = cb.superClass;
			}
			
			return isCompatible;
		}

		return false;
	}

	public static JavaType getType(Type n, List<ClassType> classList) {
		JavaType jt = null;
		if (n.f0.choice instanceof IntegerType) {
			jt = new IntType();
		} else if (n.f0.choice instanceof BooleanType) {
			jt = new BoolType();
		} else if (n.f0.choice instanceof syntaxtree.ArrayType) { // Avoid conflict with Java's ArrayType
			jt = new IntArrayType();
		} else if (n.f0.choice instanceof Identifier) {
			jt = ClassType.getClassType(((Identifier)(n.f0.choice)).f0.toString(), classList);
		}
		
		return jt;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		return this.getClass().equals(obj.getClass());
	}
}
class IntType extends JavaType {
	@Override
	public int hashCode() {
		return "int".hashCode();
	}
}
class BoolType extends JavaType {
	@Override
	public int hashCode() {
		return "bool".hashCode();
	}
}
class IntArrayType extends JavaType {
	@Override
	public int hashCode() {
		return "intarray".hashCode();
	}
}
class ClassType extends JavaType {
	public String className;
	public ClassType superClass;
	public List<Method> methodList = new ArrayList<Method>();
	
	ClassType(String className) {
		this(className, null);
	}
	ClassType (String className, ClassType superClass) {
		this.className = className;
		this.superClass = superClass;
	}

	public static ClassType getClassType(String className, List<ClassType> classList) {
		for (ClassType ct : classList)
			if (className.equals(ct.className))
				return ct;
		return null;
	}

	@Override
	public String toString() {
		return this.className;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;

		if (obj instanceof ClassType)
			return this.className.equals(((ClassType)obj).className);

		return false;
	}

	@Override
	public int hashCode() {
		return this.className.hashCode();
	}
}

class Method {
	String name;
	JavaType returnType;
	List<JavaType> argumentTypes;
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;

		if (obj instanceof Method) {
			Method m = (Method)obj;
			return this.name.equals(m.name) && this.argumentTypes.equals(m.argumentTypes) 
				&& this.returnType.equals(m.returnType);
		}

		return false;
	}

	@Override
	public int hashCode() {
		int hash = 17;
		hash = hash * 31 + this.name.hashCode();
		hash = hash * 31 + this.returnType.hashCode();
		hash = hash * 31 + this.argumentTypes.hashCode();
		return hash;
	}
}

class ClassVisitor extends GJVoidDepthFirst<List<ClassType>> {
	public void visit(MainClass n, List<ClassType> classList) {
		String cname = n.f1.f0.toString();
		if (ClassType.getClassType(cname, classList) != null)
			Typecheck.typeError("Duplicate class name " + cname);
		classList.add(new ClassType(cname));
	}

	public void visit(ClassDeclaration n, List<ClassType> classList) {
		String cname = n.f1.f0.toString();
		if (ClassType.getClassType(cname, classList) != null)
			Typecheck.typeError("Duplicate class name " + cname);
		classList.add(new ClassType(cname));
	}

	public void visit(ClassExtendsDeclaration n, List<ClassType> classList) {
		String cname = n.f1.f0.toString();
		if (ClassType.getClassType(cname, classList) != null)
			Typecheck.typeError("Duplicate class name " + cname);
		classList.add(new ClassType(cname));
	}
}

class ClassExtendsVisitor extends GJVoidDepthFirst<List<ClassType>> {
	public void visit(ClassExtendsDeclaration n, List<ClassType> classList) {
		String cname = n.f1.f0.toString();
		String parentName = n.f3.f0.toString();
		ClassType parent = ClassType.getClassType(parentName, classList);
		if (parent == null)
			Typecheck.typeError("Non existant class type " + parentName);
		ClassType ct = ClassType.getClassType(cname, classList);
		ct.superClass = parent;
	}
}

class MethodVisitor extends GJVoidDepthFirst<List<JavaType>> {
	private String className;
	public List<ClassType> classList;

	MethodVisitor (List<ClassType> classList) {
		this.classList = classList;
	}

	public void visit(Goal n, List<JavaType> methodArgs) {
		n.f1.accept(this, methodArgs);
	}

	public void visit(ClassDeclaration n, List<JavaType> methodArgs) {
		className = n.f1.f0.toString();
		n.f4.accept(this, methodArgs);
	}

	public void visit(ClassExtendsDeclaration n, List<JavaType> methodArgs) {
		className = n.f1.f0.toString();
		n.f6.accept(this, methodArgs);
	}
	
	public void visit(MethodDeclaration n, List<JavaType> methodArgs) {
		Method m = new Method();
		m.name = n.f2.f0.toString();
		m.returnType = JavaType.getType(n.f1, classList);

		List<JavaType> argTypes = new ArrayList<JavaType>();
		n.f4.accept(this, argTypes);
		m.argumentTypes = argTypes;

		ClassType currClass = ClassType.getClassType(className, classList);
		while (currClass != null) {
			for (Method other : currClass.methodList)
				if (!other.equals(m) && other.name.equals(m.name))
					Typecheck.typeError("Duplicate method: " + m.name);
			currClass = currClass.superClass;
		}

		/*
		if (enclosingClass.methodList.contains(m))
			Typecheck.typeError("Duplicate method: " + m.name);
		*/
		
		ClassType.getClassType(className, classList).methodList.add(m);
	}

	public void visit(Type n, List<JavaType> methodArgs) {
		JavaType jt = JavaType.getType(n, classList);
		methodArgs.add(jt);
	}
}

class SymbolTable {
	public int parentIndex; // Represents the parent scope
	public String enclosingClass;
	public Map<String,JavaType> table = new HashMap<String,JavaType>();
	SymbolTable(int parentIndex, String enclosingClass) {
		this.parentIndex = parentIndex;
		this.enclosingClass = enclosingClass;
	}
}

class SymbolTableVisitor extends GJVoidDepthFirst<Integer> {
	public List<SymbolTable> symbolTables = new ArrayList<SymbolTable>();
	public List<ClassType> classList;
	public String className = "";
	
	SymbolTableVisitor (List<ClassType> classList) {
		this.classList = classList;
	}

	public void visit(Goal n, Integer index) {
		symbolTables.add(new SymbolTable(index, null));
		
		int goalIndex = symbolTables.size() - 1;
		n.f0.accept(this, goalIndex);
		n.f1.accept(this, goalIndex);
	}

	public void visit(MainClass n, Integer index) {
		symbolTables.add(new SymbolTable(index, n.f1.f0.toString()));
		className = n.f1.f0.toString();

		int mainClassIndex = symbolTables.size() - 1;
		n.f14.accept(this, mainClassIndex);
		n.f15.accept(this, mainClassIndex);
	}

	public void visit(ClassDeclaration n, Integer index) {
		symbolTables.add(new SymbolTable(index, n.f1.f0.toString()));
		className = n.f1.f0.toString();

		int classDeclarationIndex = symbolTables.size() - 1;
		n.f3.accept(this, classDeclarationIndex);
		n.f4.accept(this, classDeclarationIndex);
	}

	public void visit(ClassExtendsDeclaration n, Integer index) {
		symbolTables.add(new SymbolTable(index, n.f1.f0.toString()));
		className = n.f1.f0.toString();
		
		int classExtendsDeclarationIndex = symbolTables.size() - 1;
		n.f5.accept(this, classExtendsDeclarationIndex);
		n.f6.accept(this, classExtendsDeclarationIndex);
	}

	public void visit(MethodDeclaration n, Integer index) {
		symbolTables.add(new SymbolTable(index, className));

		int methodDeclarationIndex = symbolTables.size() - 1;
		n.f4.accept(this, methodDeclarationIndex);
		n.f7.accept(this, methodDeclarationIndex);
	}

	public void visit(FormalParameter n, Integer index) {
		JavaType jt = JavaType.getType(n.f0, classList);
		if (jt == null)
			Typecheck.typeError("Non existant type");

		symbolTables.get(index).table.put(n.f1.f0.toString(), jt);
	}

	public void visit(VarDeclaration n, Integer index) {
		JavaType jt = JavaType.getType(n.f0, classList);
		if (jt == null)
			Typecheck.typeError("Non existant type");

		symbolTables.get(index).table.put(n.f1.f0.toString(), jt);
	}
}

class TypecheckVisitor extends GJDepthFirst<JavaType, List<JavaType>> {
	int scopeIndex = 0;
	public List<SymbolTable> symbolTables;
	public List<ClassType> classList;

	private JavaType getTypeByIdentifier(int scopeIndex, String identifier) {
		JavaType jt = null;
		int index = scopeIndex;
		while (index > -1) {
			jt = symbolTables.get(index).table.get(identifier);
			if (jt != null)
				return jt;
			index = symbolTables.get(index).parentIndex;
		}

		ClassType pc = ClassType.getClassType(symbolTables.get(scopeIndex).enclosingClass, classList).superClass;
		while (pc != null) {
			for (SymbolTable st : symbolTables) {
				if (st.enclosingClass != null && st.enclosingClass.equals(pc.className)) {
					JavaType jj = st.table.get(identifier);
					if (jj != null)
						return jj;
					break;
				}
			}
			pc = pc.superClass;
		}

		return null;
	}

	TypecheckVisitor (List<SymbolTable> symbolTables, List<ClassType> classList) {
		this.symbolTables = symbolTables;
		this.classList = classList;
	}

	public JavaType visit(MainClass n, List<JavaType> index) {
		scopeIndex++;
		n.f15.accept(this, index);
		return null;
	}

	public JavaType visit(ClassDeclaration n, List<JavaType> index) {
		scopeIndex++;
		n.f4.accept(this, index);
		return null;
	}

	public JavaType visit(ClassExtendsDeclaration n, List<JavaType> index) {
		scopeIndex++;
		n.f6.accept(this, index);
		return null;
	}

	public JavaType visit(MethodDeclaration n, List<JavaType> index) {
		scopeIndex++;
		n.f8.accept(this, index);
		JavaType returnType = JavaType.getType(n.f1, classList);
		JavaType rType = n.f10.accept(this, index);
		if (!JavaType.isCompatibleTypes(returnType, rType))
			Typecheck.typeError("Incorrect return value in method " + n.f2.f0.toString());

		return null;
	}

	public JavaType visit(AssignmentStatement n, List<JavaType> index) {
		JavaType lhs = n.f0.accept(this, index);
		JavaType rhs = n.f2.accept(this, index);
		
		if (lhs == null || !JavaType.isCompatibleTypes(lhs, rhs))
			Typecheck.typeError("Incompatible types in assignment statement");
		
		return null;
	}

	public JavaType visit(ArrayAssignmentStatement n, List<JavaType> index) {
		JavaType a = n.f0.accept(this, index);
		JavaType b = n.f2.accept(this, index);
		JavaType c = n.f5.accept(this, index);

		if ( !(a instanceof IntArrayType && b instanceof IntType && c instanceof IntType) )
			Typecheck.typeError("Expected IntArrayType, IntType, IntType");

		return null;
	}

	public JavaType visit(IfStatement n, List<JavaType> index) {
		JavaType a = n.f2.accept(this, index);
		
		if (!(a instanceof BoolType))
			Typecheck.typeError("Expected BoolType");

		n.f4.accept(this, index);
		n.f6.accept(this, index);
		return null;
	}

	public JavaType visit(WhileStatement n, List<JavaType> index) {
		JavaType a = n.f2.accept(this, index);
		
		if (!(a instanceof BoolType))
			Typecheck.typeError("Expected BoolType");

		n.f4.accept(this, index);
		return null;
	}

	public JavaType visit(PrintStatement n, List<JavaType> index) {
		JavaType a = n.f2.accept(this, index);
		
		if (!(a instanceof IntType))
			Typecheck.typeError("Expected IntType");

		return null;
	}

	public JavaType visit(Expression n, List<JavaType> index) {
		return n.f0.accept(this, index);
	}

	public JavaType visit(AndExpression n, List<JavaType> index) {
		JavaType a = n.f0.accept(this, index);
		JavaType b = n.f2.accept(this, index);
		if ( !(a instanceof BoolType && b instanceof BoolType) )
			Typecheck.typeError("Expected two booleans");
		
		return new BoolType();
	}

	public JavaType visit(CompareExpression n, List<JavaType> index) {
		JavaType a = n.f0.accept(this, index);
		JavaType b = n.f2.accept(this, index);
		if ( !(a instanceof IntType && b instanceof IntType) )
			Typecheck.typeError("Expected two ints");

		return new BoolType();
	}

	public JavaType visit(PlusExpression n, List<JavaType> index) {
		JavaType a = n.f0.accept(this, index);
		JavaType b = n.f2.accept(this, index);
		if ( !(a instanceof IntType && b instanceof IntType) )
			Typecheck.typeError("Expected two ints");

		return new IntType();
	}

	public JavaType visit(MinusExpression n, List<JavaType> index) {
		JavaType a = n.f0.accept(this, index);
		JavaType b = n.f2.accept(this, index);
		if ( !(a instanceof IntType && b instanceof IntType) )
			Typecheck.typeError("Expected two ints");

		return new IntType();
	}

	public JavaType visit(TimesExpression n, List<JavaType> index) {
		JavaType a = n.f0.accept(this, index);
		JavaType b = n.f2.accept(this, index);
		if ( !(a instanceof IntType && b instanceof IntType) )
			Typecheck.typeError("Expected two ints");

		return new IntType();
	}

	public JavaType visit(ArrayLookup n, List<JavaType> index) {
		JavaType a = n.f0.accept(this, index);
		JavaType b = n.f2.accept(this, index);

		if ( !(a instanceof IntArrayType && b instanceof IntType) )
			Typecheck.typeError("Expected int array followed by int");

		return new IntType();
	}

	public JavaType visit(ArrayLength n, List<JavaType> index) {
		JavaType a = n.f0.accept(this, index);
		if (!(a instanceof IntArrayType))
			Typecheck.typeError("Expected int array");

		return new IntType();
	}

	public JavaType visit(MessageSend n, List<JavaType> index) {
		JavaType ctype = n.f0.accept(this, null);
		if (!(ctype instanceof ClassType))
			Typecheck.typeError("Expected ClassType");

		List<JavaType> jtl = new ArrayList<JavaType>();
		n.f4.accept(this, jtl);

		Method m = new Method();
		m.name = n.f2.f0.toString();
		m.argumentTypes = jtl;

		Method mm = null;
		ClassType classT = (ClassType)ctype;
		while (classT != null) {
			for (Method cm : classT.methodList) {
				boolean validArguments = true;
				if (m.name.equals(cm.name) && m.argumentTypes.size() == cm.argumentTypes.size()) {
					for (int i = 0; i < cm.argumentTypes.size(); i++) {
						if (!JavaType.isCompatibleTypes(cm.argumentTypes.get(i), m.argumentTypes.get(i))) {
							validArguments = false;
							break;
						}
					}
					if (validArguments) {
						mm = cm;
						break;
					}
				}
			}
			if (mm != null)
				return mm.returnType;
			
			classT = classT.superClass;
		}
		if (mm == null)
			Typecheck.typeError("Non existant method " + m.name);

		return mm.returnType;
	}

	public JavaType visit(ExpressionList n, List<JavaType> ljt) {
		JavaType jt = n.f0.accept(this, null);
		ljt.add(jt);
		n.f1.accept(this, ljt);
		return null;
	}

	public JavaType visit(ExpressionRest n, List<JavaType> ljt) {
		JavaType jt = n.f1.accept(this, null);
		ljt.add(jt);
		return null;
	}

	public JavaType visit(PrimaryExpression n, List<JavaType> index) {
		return n.f0.accept(this, index);
	}

	public JavaType visit(IntegerLiteral n, List<JavaType> index) {
		return new IntType();
	}

	public JavaType visit(TrueLiteral n, List<JavaType> index) {
		return new BoolType();
	}

	public JavaType visit(FalseLiteral n, List<JavaType> index) {
		return new BoolType();
	}

	public JavaType visit(Identifier n, List<JavaType> index) {
		JavaType jt = getTypeByIdentifier(scopeIndex, n.f0.toString());
		if (jt == null)
			Typecheck.typeError("Non existant identifier: " + n.f0.toString());

		return jt;
	}

	public JavaType visit(ThisExpression n, List<JavaType> index) {
		String enclosingClass = symbolTables.get(scopeIndex).enclosingClass;
		if (enclosingClass == null)
			Typecheck.typeError("Unexpected this");

		JavaType jt = ClassType.getClassType(enclosingClass, classList);
		if (jt == null)
			Typecheck.typeError("this failed unexpectedly");
		return jt;
	}
	
	public JavaType visit(ArrayAllocationExpression n, List<JavaType> index) {
		JavaType jt = n.f3.accept(this, index);
		if (!(jt instanceof IntType))
			Typecheck.typeError("Expected type int");

		return new IntArrayType();
	}
	
	public JavaType visit(AllocationExpression n, List<JavaType> index) {
		JavaType jt = ClassType.getClassType(n.f1.f0.toString(), classList);
		if (jt == null)
			Typecheck.typeError("Non existant class type: " + n.f1.f0.toString());

		return jt;
	}

	public JavaType visit(NotExpression n, List<JavaType> index) {
		return n.f1.accept(this, index);
	}

	public JavaType visit(BracketExpression n, List<JavaType> index) {
		return n.f1.accept(this, index);
	}
}

