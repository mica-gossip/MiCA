package org.princehouse.mica.util.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class NullClassVisitor implements ClassVisitor {
	public static class NullMethodVisitor implements MethodVisitor {

		@Override
		public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
			return new NullAnnotationVisitor();
		}

		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			return new NullAnnotationVisitor();
		}

		@Override
		public void visitAttribute(Attribute arg0) {

		}

		@Override
		public void visitCode() {
		}

		@Override
		public void visitEnd() {
		}

		@Override
		public void visitFieldInsn(int arg0, String arg1, String arg2, String arg3) {
		}

		@Override
		public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3,
				Object[] arg4) {
		}

		@Override
		public void visitIincInsn(int arg0, int arg1) {
		}

		@Override
		public void visitInsn(int arg0) {
		}

		@Override
		public void visitIntInsn(int arg0, int arg1) {
		}

		@Override
		public void visitJumpInsn(int arg0, Label arg1) {
		}

		@Override
		public void visitLabel(Label arg0) {
		}

		@Override
		public void visitLdcInsn(Object arg0) {
		}

		@Override
		public void visitLineNumber(int arg0, Label arg1) {
		}

		@Override
		public void visitLocalVariable(String arg0, String arg1, String arg2,
				Label arg3, Label arg4, int arg5) {
		}

		@Override
		public void visitLookupSwitchInsn(Label arg0, int[] arg1, Label[] arg2) {
		}

		@Override
		public void visitMaxs(int arg0, int arg1) {
		}

		@Override
		public void visitMethodInsn(int arg0, String arg1, String arg2, String arg3) {
		}

		@Override
		public void visitMultiANewArrayInsn(String arg0, int arg1) {
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int arg0, String arg1,
				boolean arg2) {
			return new NullAnnotationVisitor();
		}

		@Override
		public void visitTableSwitchInsn(int arg0, int arg1, Label arg2,
				Label[] arg3) {
		}

		@Override
		public void visitTryCatchBlock(Label arg0, Label arg1, Label arg2,
				String arg3) {
		}

		@Override
		public void visitTypeInsn(int arg0, String arg1) {
		}

		@Override
		public void visitVarInsn(int arg0, int arg1) {
		}

	}

	public static class NullFieldVisitor implements FieldVisitor {

		@Override
		public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
			return new NullAnnotationVisitor();
		}

		@Override
		public void visitAttribute(Attribute arg0) {
		}

		@Override
		public void visitEnd() {
		}

	}

	public static class NullAnnotationVisitor implements AnnotationVisitor {

		@Override
		public void visit(String arg0, Object arg1) {
		}

		@Override
		public AnnotationVisitor visitAnnotation(String arg0, String arg1) {
			return new NullAnnotationVisitor();
		}

		@Override
		public AnnotationVisitor visitArray(String arg0) {
			return new NullAnnotationVisitor();
		}

		@Override
		public void visitEnd() {
		}

		@Override
		public void visitEnum(String arg0, String arg1, String arg2) {
		}

	}

	@Override
	public void visit(int arg0, int arg1, String arg2, String arg3,
			String arg4, String[] arg5) {
	}

	@Override
	public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
		return new NullAnnotationVisitor();
	}

	@Override
	public void visitAttribute(Attribute arg0) {
	}

	@Override
	public void visitEnd() {
	}

	@Override
	public FieldVisitor visitField(int arg0, String arg1, String arg2,
			String arg3, Object arg4) {
		return new NullFieldVisitor();
	}

	@Override
	public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) {
	}

	@Override
	public MethodVisitor visitMethod(int arg0, String arg1, String arg2,
			String arg3, String[] arg4) {
		return new NullMethodVisitor();
	}

	@Override
	public void visitOuterClass(String arg0, String arg1, String arg2) {
	}

	@Override
	public void visitSource(String arg0, String arg1) {
	}

}
