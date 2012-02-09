package org.princehouse.mica.util.asm;


import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;


import org.objectweb.asm.*;
import org.objectweb.asm.util.*;
import org.princehouse.mica.util.ByteClassLoader;
import org.princehouse.mica.util.ClassUtils;

public class Bytecode {
	public static String getInternalClassName(byte[] bytecode) {
		// note: internal classname
		assert(bytecode != null);
		ClassReader r = new ClassReader(bytecode);
		return r.getClassName();
	}
	
	public static String getExternalClassName(byte[] bytecode) {
		return getInternalClassName(bytecode).replace('/','.');
	}

	public static void writeClassToFile(byte[] bytecode, File baseDir, boolean deleteOnExit) throws IOException {
		// TODO Auto-generated method stub
		assert(baseDir.exists());
		assert(baseDir.isDirectory());
		String classname = Bytecode.getInternalClassName(bytecode);
		String[] dirs = classname.split("/");
		File temp = baseDir;
		for(int i = 0; i < dirs.length - 1; i++) {
			temp = new File(baseDir.getPath() + File.separator + dirs[i]);
			if(!temp.exists())
				temp.mkdir();
			if(deleteOnExit) 
				temp.deleteOnExit();
		}	
		File classFile = new File(temp.getPath() + File.separator + dirs[dirs.length-1] + ".class");
		classFile.deleteOnExit();
		FileOutputStream fos = new FileOutputStream(classFile);
		DataOutputStream dos = new DataOutputStream(fos);
		dos.write(bytecode);
		dos.close();
	}
	
	public static Class<?> bytecodeToClass(byte[] bytes) {
		String name = getExternalClassName(bytes);
		ByteClassLoader bcl = new ByteClassLoader();
		return bcl.classFromBytes(name, bytes);
	}
	
	public static boolean classImplements(byte[] bytes, Class<?> iface) {
		Class<?> k = bytecodeToClass(bytes);
		return ClassUtils.classImplements(k, iface);
	}

	public static boolean classExtends(byte[] bytes,
			Class<?> parentClass) {
		Class<?> k = bytecodeToClass(bytes);
		return ClassUtils.classExtends(k, parentClass);
	}

	public static void printTrace(byte[] dst) {
		printTrace(dst, System.out);
	}
	
	public static void printTrace(byte[] dst, PrintStream out) {
		PrintWriter pw = new PrintWriter(out);
		ClassVisitor cv = new TraceClassVisitor(pw);
		ClassReader cr = new ClassReader(dst);
		cr.accept(cv, 0);
	}
	
	public static void printASM(byte[] dst) {
		printASM(dst, System.out);
	}
	
	public static void printASM(byte[] dst, PrintStream out) {
		PrintWriter pw = new PrintWriter(out);
		ClassVisitor cv = new ASMifierClassVisitor(pw);
		ClassReader cr = new ClassReader(dst);
		cr.accept(cv, 0);
	}
	
	public static void printTrace(byte[] src, String filename) throws FileNotFoundException {
		PrintStream ps = new PrintStream(new FileOutputStream(filename));
		Bytecode.printTrace(src,ps);
		ps.close();
	}
	

	public static void printASM(byte[] src, String filename) throws FileNotFoundException {
		PrintStream ps = new PrintStream(new FileOutputStream(filename));
		Bytecode.printTrace(src,ps);
		ps.close();
	}
}
