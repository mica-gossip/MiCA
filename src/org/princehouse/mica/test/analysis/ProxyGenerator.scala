package org.princehouse.mica.test.analysis
import java.io._
import soot._
import java.util.Arrays
import collection.JavaConversions._
import soot.jimple._
import soot.util._
import soot.jimple.spark.ondemand.pautil.SootUtil
import org.princehouse.mica.util.scala.SootUtils

class MethodProxy(sourceMethod: SootMethod, proxiedValues: Set[Value], gen: ProxyGenerator) {
  val proxyMethodName = gen.methodNameGenerator.next
  val proxyMethod = createProxiedMethod
  
  def createProxiedMethod : SootMethod = {
    // Create the SootMethod, but do not create the body
    var parameterTypes:List[Type] = Nil // fixme
    
    if(sourceMethod.isStatic()) {
      throw new RuntimeException("static proxied methods not currently supported")
    }
    
    val sourceParamTypes:List[Type] = sourceMethod.getParameterTypes().asInstanceOf[List[Type]]
    
    // Loop over sourceMethod parameters; replace any that are proxied with the proxy type
    //for(p <=  sourceParamTypes) {
      
    //}
    
    val returnType:Type = null // fixme
    val method = new SootMethod(proxyMethodName, parameterTypes, returnType, Modifier.PUBLIC | Modifier.STATIC)
    method
  }
  
  def createBody = {
    
  }
}

class FieldProxy(sourceField: SootField, proxyField: SootField, dirtyBitField: SootField, targetClass: SootClass) {
  val ftype = sourceField.getType()
  var getter: SootMethod = null
  var setter: SootMethod = null
  val j = Jimple.v

  def createGetter(proxyClass: SootClass) = {
    val method = new SootMethod("get%s".format(proxyField.getName()), List(), ftype, Modifier.PUBLIC)
    proxyClass.addMethod(method)
    val body = j.newBody(method)
    method.setActiveBody(body)
    val proxyBase = proxyClass.getSuperclass

    val targetField = proxyBase.getField("java.lang.Object target")
    val objectType = Scene.v().loadClassAndSupport("java.lang.Object").getType()
    val targetType = targetClass.getType()

    val units = body.getUnits()

    val proxy = SootUtils.addNewUniqueLocal("this", method, proxyClass.getType())
    units.add(j.newIdentityStmt(proxy, j.newThisRef(proxyClass.getType())))

    val tmp = SootUtils.addNewUniqueLocal("tmp", method, ftype)

    val targetUncast = SootUtils.addNewUniqueLocal("targetUncast", method, objectType)
    val target = SootUtils.addNewUniqueLocal("target", method, targetType)

    val isProxyBranch = j.newAssignStmt(tmp,
      j.newInstanceFieldRef(proxy, proxyField.makeRef()))

    // if isProxy ... else ..
    val c = SootUtils.addNewUniqueLocal("c", method, BooleanType.v)
    val isProxyField = proxyBase.getField("boolean isProxy")
    units.add(j.newAssignStmt(c, j.newInstanceFieldRef(proxy, isProxyField.makeRef)))
    units.addAll(SootUtils.ifThenElse(c,
      // !isProxy clause
      List[Unit](
        j.newAssignStmt(targetUncast, j.newInstanceFieldRef(proxy, targetField.makeRef())),
        j.newAssignStmt(target, j.newCastExpr(targetUncast, targetType)),
        j.newAssignStmt(tmp, j.newInstanceFieldRef(target, sourceField.makeRef()))),
      // isProxy clause
      j.newAssignStmt(tmp,
        j.newInstanceFieldRef(proxy, proxyField.makeRef())) :: setDirtyBitAst(proxy, SootUtils.TRUE)))

    units.add(j.newReturnStmt(tmp))

    getter = method

  }

  // emit jimple code for copying the target field to the proxied field
  def box(method: SootMethod, proxy: Local, target: Local): List[Unit] = {
    val tmp = SootUtils.addNewUniqueLocal("l",method,ftype)
    
    List(j.newAssignStmt(tmp, j.newInstanceFieldRef(target, sourceField.makeRef())),
        j.newAssignStmt(j.newInstanceFieldRef(proxy, proxyField.makeRef()), tmp)) ::: setDirtyBitAst(proxy, SootUtils.FALSE)
  }

  def setDirtyBitAst(proxy: Local, value: Value): List[Unit] = {
    val assignment = j.newAssignStmt(j.newInstanceFieldRef(proxy, dirtyBitField.makeRef()), value)
    List[Unit](assignment)
  }

  def createSetter(proxyClass: SootClass) = {
    val method = new SootMethod("set%s".format(proxyField.getName()), List(ftype), VoidType.v(), Modifier.PUBLIC)
    proxyClass.addMethod(method)
    val body = j.newBody(method)
    method.setActiveBody(body)
    val units = body.getUnits()
    val proxyBase = proxyClass.getSuperclass

    // this = @this
    val proxy = SootUtils.addNewUniqueLocal("this", method, proxyClass.getType())
    units.add(j.newIdentityStmt(proxy, j.newThisRef(proxyClass.getType())))

    // tmp = @parameter0
    val tmp = SootUtils.addNewUniqueLocal("tmp", method, ftype)
    units.add(j.newIdentityStmt(tmp, j.newParameterRef(ftype, 0)))

    val objectType = Scene.v().loadClassAndSupport("java.lang.Object").getType()
    val targetType = targetClass.getType()

    val targetUncast = SootUtils.addNewUniqueLocal("targetUncast", method, objectType)
    val target = SootUtils.addNewUniqueLocal("target", method, targetType)
    val targetField = proxyBase.getField("java.lang.Object target")

    // if isProxy ... else ..
    val c = SootUtils.addNewUniqueLocal("c", method, BooleanType.v)
    val isProxyField = proxyBase.getField("boolean isProxy")
    units.add(j.newAssignStmt(c, j.newInstanceFieldRef(proxy, isProxyField.makeRef)))
    units.addAll(SootUtils.ifThenElse(c,
      // !isProxy clause
      List[Unit](
        j.newAssignStmt(targetUncast, j.newInstanceFieldRef(proxy, targetField.makeRef())), // targetUncast = proxy.target
        j.newAssignStmt(target, j.newCastExpr(targetUncast, targetType)), // target = (PROXYCLASS) targetUncast
        j.newAssignStmt(j.newInstanceFieldRef(target, sourceField.makeRef()), tmp)), //
      // isProxy clause
      j.newAssignStmt(j.newInstanceFieldRef(proxy, proxyField.makeRef()), tmp) :: setDirtyBitAst(proxy, SootUtils.TRUE)))

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
  validityCheckInputs

  val proxyBaseClassName = "org.princehouse.mica.base.c1.ProxyBase"
  val proxyBaseClass = Scene.v.loadClassAndSupport(proxyBaseClassName)
  val objectClass = Scene.v.loadClassAndSupport("java.lang.Object")
  val j = Jimple.v()
  val objectType = Scene.v().loadClassAndSupport("java.lang.Object").getType()
  val targetType = targetClass.getType()
  val targetField = proxyBaseClass.getField("java.lang.Object target")

  val fieldNameGenerator = new UIDGenerator("field")
  val methodNameGenerator = new UIDGenerator("method")

  // maps soot fields to their proxy objects
  val fieldMap = collection.mutable.Map[SootField, FieldProxy]()
  val methodMap = collection.mutable.Map[SootMethod, MethodProxy]()
  
  // Name of the proxy class we're creating.  
  val proxyClassName = targetClass.getName() + "Proxy"

  // SootClass representing the proxy class. Call createMethodProxy to fill it out
  var proxyClass: SootClass = new SootClass(proxyClassName, Modifier.PUBLIC)

  def validityCheckInputs = {
    // static field proxies currently not allowed
    for (field <- proxiedFields) {
      if (SootUtils.isStatic(field)) {
        throw new RuntimeException("Error while proxying: Static field proxying is not allowed (%s.%s)".format(
          field.getDeclaringClass().getName(), field.getSignature()))
      }

    }
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


  def implementBoxMethod(): scala.Unit = {
    val method = new SootMethod("box", List[Type](objectClass.getType()),
      VoidType.v(), Modifier.PUBLIC)
    proxyClass.addMethod(method)
    val body = Jimple.v().newBody(method)
    method.setActiveBody(body)
    val units = body.getUnits()

    // this = @this
    val thisproxy = SootUtils.addNewUniqueLocal("this", method, proxyClass.getType)
    units.add(Jimple.v().newIdentityStmt(thisproxy,
      Jimple.v().newThisRef(proxyClass.getType)))

    val targetUncast = SootUtils.addNewUniqueLocal("targetUncast", method, objectType)
    val target = SootUtils.addNewUniqueLocal("target", method, targetType)

    // targetUncast = @parameter0
    units.add(j.newIdentityStmt(targetUncast, j.newParameterRef(objectType,0)))
    // target = (TARGETCLASS) targetUncast
    units.add(j.newAssignStmt(target, j.newCastExpr(targetUncast, targetType)))

    // all of the code for boxing all fields
    units.addAll(fieldMap.values.map(_.box(method, thisproxy, target)).reduce(_:::_))

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
      // you are here
      
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