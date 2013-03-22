package org.princehouse.mica.test.analysis
import java.io._
import soot._
import java.util.Arrays
import collection.JavaConversions._
import soot.jimple._
import soot.util._

class FieldProxy(originalField: SootField, proxyField: SootField, dirtyBitField: SootField, targetClass: SootClass) {
  val ftype = originalField.getType()
  var getter: SootMethod = null
  var setter: SootMethod = null

  def createGetter(proxyClass: SootClass) = {
    val j = Jimple.v()
    val method = new SootMethod("get%s".format(proxyField.getName()), List(), ftype, Modifier.PUBLIC)
    proxyClass.addMethod(method)
    val body = j.newBody(method)
    method.setActiveBody(body)

    val proxyBase = proxyClass.getSuperclass
    
    val targetField = proxyBase.getField("java.lang.Object target")
    val objectType = Scene.v().loadClassAndSupport("java.lang.Object").getType()
    val targetType = targetClass.getType()

    val units = body.getUnits()

    val loc = j.newLocal("this", proxyClass.getType())
    body.getLocals().add(loc)
    units.add(j.newIdentityStmt(loc, j.newThisRef(proxyClass.getType())))

    val tmp = j.newLocal("tmp", ftype)
    body.getLocals().add(tmp)

    val c = j.newLocal("c", BooleanType.v)
    body.getLocals().add(c)

    val targetUncast = j.newLocal("targetUncast", objectType) // before casting
    body.getLocals().add(targetUncast)

    val target = j.newLocal("target", targetType)
    body.getLocals().add(target)

    val isProxyField = proxyBase.getField("boolean isProxy")
    units.add(j.newAssignStmt(c, j.newInstanceFieldRef(loc, isProxyField.makeRef)))

    val isProxyBranch = j.newAssignStmt(tmp,
      j.newInstanceFieldRef(loc, proxyField.makeRef()))

    val finish = j.newReturnStmt(tmp)

    val cEqualsTrue = j.newEqExpr(c, IntConstant.v(1))
    
    units.add(j.newIfStmt(cEqualsTrue, isProxyBranch))

    {
      // conditional branch for !isProxy
      units.add(j.newAssignStmt(target, j.newCastExpr(
          j.newInstanceFieldRef(loc,targetField.makeRef()),
          targetType)))
      
      units.add(j.newAssignStmt(tmp,
        j.newInstanceFieldRef(target, originalField.makeRef())))
      units.add(j.newGotoStmt(finish))
    }
    {
      // conditional branch for isProxy
      units.add(isProxyBranch)
      units.add(j.newAssignStmt(tmp,
        j.newInstanceFieldRef(loc, proxyField.makeRef())))
    }

    units.add(finish)
    getter = method
  }

  def createSetter(proxyClass: SootClass) = {
    val j = Jimple.v()
    val method = new SootMethod("set%s".format(proxyField.getName()), List(ftype), VoidType.v(), Modifier.PUBLIC)
    proxyClass.addMethod(method)
    val body = j.newBody(method)
    method.setActiveBody(body)
    val units = body.getUnits()
    val loc = j.newLocal("this", proxyClass.getType())
    body.getLocals().add(loc)
    units.add(j.newIdentityStmt(loc, j.newThisRef(proxyClass.getType())))
    val tmp = j.newLocal("tmp", ftype)
    body.getLocals().add(tmp)
    units.add(j.newIdentityStmt(tmp, j.newParameterRef(ftype, 0)))
    units.add(j.newAssignStmt(j.newInstanceFieldRef(loc, proxyField.makeRef()), tmp))
    units.add(j.newReturnVoidStmt())
    setter = method
  }

}

class UIDGenerator(base: String) {
  var i = 0

  def next = {
    i += 1
    "%s%d".format(base, i)
  }
}

class ProxyGenerator(targetClass: SootClass, proxiedFields: Set[SootField], refactorTargets: Map[SootMethod, Set[Value]]) {
  val proxyBaseClassName = "org.princehouse.mica.base.c1.ProxyBase"
  val proxyBaseClass = Scene.v.loadClassAndSupport(proxyBaseClassName)
  val objectClass = Scene.v.loadClassAndSupport("java.lang.Object")
  val j = Jimple.v()

  val fieldNameGenerator = new UIDGenerator("field")
  val methodNameGenerator = new UIDGenerator("method")

  // maps soot fields to their proxy objects
  val fieldMap = collection.mutable.Map[SootField, FieldProxy]()

  // Name of the proxy class we're creating.  
  val proxyClassName = targetClass.getName() + "Proxy"

  // SootClass representing the proxy class. Call createMethodProxy to fill it out
  var proxyClass: SootClass = new SootClass(proxyClassName, Modifier.PUBLIC)

  // prerequisite: "obj" must be of the proxy type
  def exprGetField(obj: Local, field: SootField): Value = {
    null // TODO FIXME
  }

  // implement a proxied field in the proxy class
  def createFieldProxy(field: SootField): scala.Unit = {
    val fieldProxyName = fieldNameGenerator.next
    val fieldDirtyBitName = fieldProxyName + "dirtyBit"
    println("Proxy field  %s.%s -> %s : %s".format(field.getDeclaringClass().getName(),
      field.getName(), fieldProxyName, field.getType()))
    // create proxy field
    val proxyField = new SootField(fieldProxyName, field.getType(), Modifier.PUBLIC)
    proxyClass.addField(proxyField)
    val dirtyField = new SootField(fieldDirtyBitName, BooleanType.v());
    proxyClass.addField(dirtyField)
    val proxy = new FieldProxy(field, proxyField, dirtyField, targetClass)
    fieldMap.put(field, proxy)

    proxy.createGetter(proxyClass)
    proxy.createSetter(proxyClass)
  }

  // implement a refactored method in the proxy class
  def createMethodProxy(method: SootMethod): scala.Unit = {
    // values must be either local, parameter, thisref, or rvalue -- essentially, variables whose type must be changed to proxy
    // TODO
    val methodProxyName = methodNameGenerator.next
    println("Proxy method  %s -> %s".format(method.getName(), methodProxyName))
  }

  def implementBoxMethod(): scala.Unit = {

    val method = new SootMethod("box", List[Type](objectClass.getType()),
      VoidType.v(), Modifier.PUBLIC)
    proxyClass.addMethod(method)
    val body = Jimple.v().newBody(method)
    method.setActiveBody(body)
    val units = body.getUnits()

    // assign this to a local variable
    val thisproxy = Jimple.v.newLocal("this", proxyClass.getType)
    body.getLocals().add(thisproxy)
    units.add(Jimple.v().newIdentityStmt(thisproxy,
      Jimple.v().newThisRef(proxyClass.getType)))

    // TODO -- implement all of the actual boxing.  This is just a stub right now

    units.add(Jimple.v.newReturnVoidStmt)
  }

  def implementApplyDiffMethod(): scala.Unit = {
    // TODO
  }

  def implementUpdateMethod: scala.Unit = {
    // TODO
  }

  def implementConstructor: scala.Unit = {
    val constructor = new SootMethod("<init>", List(),
      VoidType.v(), Modifier.PUBLIC)
    proxyClass.addMethod(constructor)
    val body = Jimple.v().newBody(constructor)
    constructor.setActiveBody(body)
    val units = body.getUnits()

    // call super()
    // assign this to a local variable
    val local = Jimple.v.newLocal("this", proxyClass.getType)
    body.getLocals().add(local)

    // local = @this
    units.add(Jimple.v().newIdentityStmt(local,
      Jimple.v().newThisRef(proxyClass.getType)))

    for (m <- proxyBaseClass.getMethods()) {
      println("(debug) ProxyBase method sub-signature \"%s\"".format(m.getSubSignature()))
    }
    // super()      
    val superInit = proxyBaseClass.getMethod("void <init>()").makeRef()
    units.add(j.newInvokeStmt(j.newSpecialInvokeExpr(local, superInit)))

    // return
    units.add(Jimple.v.newReturnVoidStmt)
  }

  // each refactor target is a method and a set of local/parameter/rvalues from that method that may be proxied and need to be refactored.
  // a new version of each of these methods will be created
  // each proxied field may be from anywhere in the inheritance list of targetClass 
  def createProxyClass: SootClass = {
    // resolve dependencies
    Scene.v.loadClassAndSupport(proxyBaseClassName)
    // declare public class HelloWorld
    proxyClass = new SootClass(proxyClassName, Modifier.PUBLIC)

    println("Generating proxy class " + proxyClassName)
    // 'extends ProxyBase'
    proxyClass.setSuperclass(proxyBaseClass)
    Scene.v.addClass(proxyClass)

    // create field proxies  (need to be in sorted order to get consistent naming)
    for (f <- proxiedFields.toList.sortBy(f => f.getName())) {
      createFieldProxy(f)
    }

    // refactor methods (need to be in sorted order to get consistent naming)
    for (method <- refactorTargets.keys.toList.sortBy(m => m.getSignature())) {
      createMethodProxy(method)
    }

    implementBoxMethod
    implementApplyDiffMethod
    implementConstructor
    implementUpdateMethod

    // create public static main
    /*
    val stringArrayType = ArrayType.v(RefType.v("java.lang.String"), 1)
    val typearg: List[Type] = List(stringArrayType)
    val method = new SootMethod("main", typearg,
      VoidType.v(), Modifier.PUBLIC | Modifier.STATIC)
    */

    /*
    proxyClass.addMethod(method)

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
*/

    proxyClass
  }
}