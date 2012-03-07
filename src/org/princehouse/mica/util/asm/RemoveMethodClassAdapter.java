package org.princehouse.mica.util.asm;


import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class RemoveMethodClassAdapter extends ClassAdapter {

	private Set<String> methodsToRemove = new HashSet<String>();
	
	public RemoveMethodClassAdapter(ClassVisitor cv, String[] methodNames) {
		super(cv);
		for(String s : methodNames) {
			methodsToRemove.add(s);
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, 
			String signature, String[] exceptions) {
		if(methodsToRemove.contains(name)) {
			methodsToRemove.remove(name);
			return new NullClassVisitor.NullMethodVisitor();
		} else 
			return super.visitMethod(access, name, desc, signature, exceptions);
	}
}
