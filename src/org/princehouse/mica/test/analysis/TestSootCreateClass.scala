package org.princehouse.mica.test.analysis
import java.io._
import soot._
import java.util.Arrays
import collection.JavaConversions._
import soot.jimple._
import soot.util._

// example from http://www.sable.mcgill.ca/soot/tutorial/createclass/

object TestSootCreateClass {

  def main(args: Array[String]): scala.Unit = {
    val sClass = createSootClass

    val outfile = SourceLocator.v.getFileNameFor(sClass, soot.options.Options.output_format_class)
    println("Writing to " + outfile)

    val streamOut = new JasminOutputStream(new FileOutputStream(outfile))
    val writerOut = new PrintWriter(new OutputStreamWriter(streamOut))
    val jasminClass = new JasminClass(sClass)
    jasminClass.print(writerOut)
    writerOut.flush()
    streamOut.close()
  }

  def createSootClass = {
    // resolve dependencies
    Scene.v().loadClassAndSupport("java.lang.Object")
    Scene.v().loadClassAndSupport("java.lang.System")
    //Scene.v.loadClassAndSupport("java.io.PrintStream")

    // declare public class HelloWorld
    val sClass = new SootClass("HelloWorld", Modifier.PUBLIC)

    // 'extends Object'
    sClass.setSuperclass(Scene.v.getSootClass("java.lang.Object"))
    Scene.v.addClass(sClass)

    // create public static main
    val stringArrayType = ArrayType.v(RefType.v("java.lang.String"), 1)
    val typearg: List[Type] = List(stringArrayType)
    val method = new SootMethod("main", typearg,
      VoidType.v(), Modifier.PUBLIC | Modifier.STATIC)

    sClass.addMethod(method)

    // create method body
    val body = Jimple.v().newBody(method)
    method.setActiveBody(body)

    val units = body.getUnits()

    // add local l0
    val arg = Jimple.v().newLocal("l0", ArrayType.v(RefType.v("java.lang.String"), 1))
    body.getLocals().add(arg)

    // add local tmpref
    val tmpRef = Jimple.v.newLocal("tmpRef", RefType.v("java.io.PrintStream"))
    body.getLocals().add(tmpRef)

    // l0 = @parameter0
    units.add(Jimple.v().newIdentityStmt(arg,
      Jimple.v().newParameterRef(ArrayType.v(RefType.v("java.lang.String"), 1), 0)))

    // tmpRef = java.lang.System.out
    units.add(Jimple.v().newAssignStmt(tmpRef, Jimple.v().newStaticFieldRef(
      Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())))

    // tmpRef.println("hello world")
    val methodToCall = Scene.v.getMethod("<java.io.PrintStream: void println(java.lang.String)>")
    units.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(tmpRef, methodToCall.makeRef(), StringConstant.v("Hello world!"))));

    // return
    units.add(Jimple.v.newReturnVoidStmt)

    sClass
  }
}