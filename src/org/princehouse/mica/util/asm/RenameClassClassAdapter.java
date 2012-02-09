package org.princehouse.mica.util.asm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.*;

// See TODO blocks for places to fix.  Development on this is suspended since I've just discovered RemappingClassAdapter

public class RenameClassClassAdapter implements ClassVisitor {
	
	private String src, dst;
	private Pattern typeTransform;
	private ClassVisitor cv;

	
	public class RenameClassAnnotationAdapter implements AnnotationVisitor {

		private AnnotationVisitor av;
		public RenameClassAnnotationAdapter(AnnotationVisitor av) {
			this.av = av;
			throw new RuntimeException("RenameClassAnnotationAdapter not implemented");
		}
		@Override
		public void visit(String arg0, Object arg1) {
			// TODO Auto-generated method stub	
		}

		@Override
		public AnnotationVisitor visitAnnotation(String arg0, String arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public AnnotationVisitor visitArray(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void visitEnd() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void visitEnum(String arg0, String arg1, String arg2) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public class RenameClassMethodAdapter implements MethodVisitor {

		private MethodVisitor mv;
		
		public RenameClassMethodAdapter(MethodVisitor mv) {
			this.mv = mv;
		}
		
		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			desc = translateDescriptor(desc);
			return new RenameClassAnnotationAdapter(mv.visitAnnotation(desc, visible));
		}

		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			return new RenameClassAnnotationAdapter(mv.visitAnnotationDefault());
		}

		@Override
		public void visitAttribute(Attribute arg0) {
			// TODO
			throw new RuntimeException("not implemented");
		}

		@Override
		public void visitCode() {
			mv.visitCode();
		}

		@Override
		public void visitEnd() {
			mv.visitEnd();
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name,
				String desc) {
			owner = translateInternalName(owner);
			desc = translateDescriptor(desc);
			mv.visitFieldInsn(opcode, owner, name, desc);
		}

		@Override
		public void visitFrame(int type, int nLocal, Object[] local, int nStack,
				Object[] stack) {
			mv.visitFrame(type,nLocal,local,nStack, stack);
		}

		@Override
		public void visitIincInsn(int var, int increment) {
			mv.visitIincInsn(var,increment);
			
		}

		@Override
		public void visitInsn(int opcode) {
			mv.visitInsn(opcode);
		}

		@Override
		public void visitIntInsn(int opcode, int operand) {
			mv.visitIntInsn(opcode,operand);
		}

		@Override
		public void visitJumpInsn(int opcode, Label label) {
			mv.visitJumpInsn(opcode,label);
		}

		@Override
		public void visitLabel(Label label) {
			mv.visitLabel(label);
		}

		@Override
		public void visitLdcInsn(Object cst) {
			mv.visitLdcInsn(cst);
		}

		@Override
		public void visitLineNumber(int line, Label start) {
			mv.visitLineNumber(line,start);
		}

		@Override
		public void visitLocalVariable(String name, String desc, String signature,
				Label start, Label end, int index) {
			desc = translateDescriptor(desc);
			signature = translateSignature(signature);
			mv.visitLocalVariable(name,desc, signature, start, end, index);
		}

		@Override
		public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
			visitLookupSwitchInsn(dflt,keys,labels);
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			mv.visitMaxs(maxStack, maxLocals);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name,
				String desc) {
			owner = translateInternalName(owner);
			desc = translateDescriptor(desc);
			mv.visitMethodInsn(opcode,owner,name,desc);
		}

		@Override
		public void visitMultiANewArrayInsn(String desc, int dims) {
			desc = translateDescriptor(desc);
			mv.visitMultiANewArrayInsn(desc,dims);
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter,
				String desc, boolean visible) {
			desc = translateDescriptor(desc);
			return new RenameClassAnnotationAdapter(mv.visitParameterAnnotation(parameter, desc, visible));						
		}

		@Override
		public void visitTableSwitchInsn(int min, int max, Label dflt,
				Label[] labels) {
			mv.visitTableSwitchInsn(min, max, dflt, labels);			
		}

		@Override
		public void visitTryCatchBlock(Label start, Label end, Label handler,
				String type) {
			type = translateInternalName(type);
			mv.visitTryCatchBlock(start, end, handler, type);
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			type = translateInternalName(type);
			mv.visitTypeInsn(opcode, type);
		}

		@Override
		public void visitVarInsn(int opcode, int var) {
			mv.visitVarInsn(opcode, var);
		}
	
	}
	
	
	public class RenameClassFieldAdapter implements FieldVisitor {

		@Override
		public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void visitAttribute(Attribute arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void visitEnd() {
			// TODO Auto-generated method stub
			
		}
		
	}

	
	public RenameClassClassAdapter(String srcName, String dstName, ClassVisitor cv) {
		this.cv = cv;
		src = srcName; dst = dstName;
		// FIXME this is a hack; it does basic string substitution.  This will majorly fail
		// if one class name is a substring of another
		typeTransform = Pattern.compile(src);
	}


	private String searchAndReplace(String in) {
		// FIXME hack
		if(in == null) 
			return null;
		Matcher m = typeTransform.matcher(in);
		StringBuffer sb = new StringBuffer();
		boolean result = m.find();
		while(result) {
			m.appendReplacement(sb, dst);
			result = m.find();
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private String translateName(String in) {
		// FIXME hack
		return searchAndReplace(in);
	}
	
	private String translateDescriptor(String in) {
		// FIXME hack
		return searchAndReplace(in);
	}
	
	private String translateSignature(String in) {
		// FIXME hack
		return searchAndReplace(in);
	}
	
	private String translateInternalName(String in) {
		// FIXME hack
		return searchAndReplace(in);
	}
	@Override
	public void visit(int arg0, int arg1, String arg2, String arg3,
			String arg4, String[] arg5) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		// Desc is of form L%s;
		return null;
		//return new RenameClassAnnotationVisitor(cv.visitAnnotation(transformType(desc), visible));
	}

	@Override
	public void visitAttribute(Attribute arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitEnd() {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public void visitOuterClass(String arg0, String arg1, String arg2) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void visitSource(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public FieldVisitor visitField(int arg0, String arg1, String arg2,
			String arg3, Object arg4) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void visitInnerClass(String arg0, String arg1, String arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public MethodVisitor visitMethod(int arg0, String arg1, String arg2,
			String arg3, String[] arg4) {
		// TODO Auto-generated method stub
		return null;
	}

}
