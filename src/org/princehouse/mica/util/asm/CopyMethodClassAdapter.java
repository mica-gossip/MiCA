package org.princehouse.mica.util.asm;


import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.princehouse.mica.util.ClassUtils;

public class CopyMethodClassAdapter extends ClassAdapterStack {

	private static final long serialVersionUID = 1L;

	public static class MethodsNotFoundException extends Exception {
		private static final long serialVersionUID = 1L;
		private Iterable<String> methods;
		public MethodsNotFoundException(Iterable<String> methods) {
			this.methods = methods;
		}
		public Iterable<String> getMethods() { return methods; }
	}
	
	public enum State {SEARCH, COPY}
	
	private Set<String> methodsToFind = new HashSet<String>();
	private State state = State.COPY;
	private ClassReader source;
	
	public CopyMethodClassAdapter(ClassVisitor output, ClassReader source, String[] methodNames) {
		super(output);
		this.source = source;
		for(String s : methodNames) {
			methodsToFind.add(s);
		}
	}
	
	public CopyMethodClassAdapter(ClassVisitor output, Class<?> source, String[] methodNames) throws IOException {
		this(output, 
				new ClassReader(ClassUtils.getClassBytecode(source)),
				methodNames);
	}

	
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, 
			String signature, String[] exceptions) {
		if(state == State.SEARCH && methodsToFind.contains(name)) {
			ClassVisitor cv = pop();
			methodsToFind.remove(name);
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			push(cv);
			return mv;
		} else {
			return super.visitMethod(access, name, desc, signature, exceptions);	
		}
	}

	@Override
	public void visitEnd() {
		if(state == State.COPY)
			try {
				copyMethods();
			} catch (MethodsNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException("Some methods not found (fix this error message for more info!)");
			}
		super.visitEnd();
	}
	
	private void copyMethods() throws MethodsNotFoundException {
		state = State.SEARCH;
		push(new NullClassVisitor());
		source.accept(this, ClassReader.EXPAND_FRAMES);

		pop();
		state = State.COPY;
		if(methodsToFind.size() > 0) {
			throw new MethodsNotFoundException(methodsToFind);
		}
	}
	
}
