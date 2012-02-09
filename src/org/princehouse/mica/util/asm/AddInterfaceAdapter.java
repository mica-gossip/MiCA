package org.princehouse.mica.util.asm;


import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.princehouse.mica.util.Array;

// Adds an interface to the array of implemented interfaces
// Does NOT ensure that all appropriate methods/members are present
public class AddInterfaceAdapter extends ClassAdapter {

	private String interfaceName;
	
	public AddInterfaceAdapter(ClassVisitor arg0, String interfaceName) {
		super(arg0);
		this.interfaceName = interfaceName;
	}

	
	@Override
	public void visit(int version, 
			int access, 
			String name, 
			String signature, 
			String superName, 
			String[] interfaces) {
		
		interfaces = Array.append(interfaces, interfaceName);
		super.visit(version, access, name, signature, superName, interfaces);
	}
}
