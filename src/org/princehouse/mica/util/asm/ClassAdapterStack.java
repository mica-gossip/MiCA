package org.princehouse.mica.util.asm;

import java.util.Stack;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassAdapterStack extends Stack<ClassVisitor> implements ClassVisitor {

	public ClassAdapterStack() {}
	
	public ClassAdapterStack(ClassVisitor out) {
		push(out);
	}
	
	@Override
	public void visit(int arg0, int arg1, String arg2, String arg3,
			String arg4, String[] arg5) {
		peek().visit(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
		return peek().visitAnnotation(arg0, arg1);
	}

	@Override
	public void visitAttribute(Attribute arg0) {
		peek().visitAttribute(arg0);
	}

	@Override
	public void visitEnd() {
		peek().visitEnd();
	}

	@Override
	public FieldVisitor visitField(int arg0, String arg1, String arg2,
			String arg3, Object arg4) {
		return peek().visitField(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) {
		peek().visitInnerClass(arg0, arg1, arg2, arg3);
	}

	@Override
	public MethodVisitor visitMethod(int arg0, String arg1, String arg2,
			String arg3, String[] arg4) {
		return peek().visitMethod(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	public void visitOuterClass(String arg0, String arg1, String arg2) {
		peek().visitOuterClass(arg0, arg1, arg2);
	}

	@Override
	public void visitSource(String arg0, String arg1) {
		peek().visitSource(arg0,arg1);
	}

}
