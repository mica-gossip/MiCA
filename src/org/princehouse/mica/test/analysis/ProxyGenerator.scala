package org.princehouse.mica.test.analysis
import java.io._
import soot._
import java.util.Arrays
import collection.JavaConversions._
import soot.jimple._
import soot.util._
import soot.jimple.spark.ondemand.pautil.SootUtil
import org.princehouse.mica.util.scala.SootUtils

class MethodProxy(sourceMethod: SootMethod, proxiedValues: Set[Value], proxyReturnValue: Boolean, gen: ProxyGenerator) {
  val proxyMethodName = gen.methodNameGenerator.next
  val proxyType = gen.proxyClass.getType
  val proxyMethod = createProxiedMethod

  def createProxiedMethod: SootMethod = {
    // Currently only support instance methods.  No fundamental reason for that, just doing one thing at a time...
    if (sourceMethod.isStatic()) {
      throw new RuntimeException("static proxied methods not currently supported")
    }

    // Create the SootMethod, but do not create the body
    val boundClassType = sourceMethod.getDeclaringClass.getType

    // Converting the instance method into a static method, the declaring class instance will be passed as a new parameter
    // Added to the end of the param list to preserve parameter numbers for the others...
    var parameterTypes: List[Type] = Nil
    for (obj <- sourceMethod.getParameterTypes()) {
      parameterTypes = parameterTypes ::: List(obj match {
        case x: Type => x
        case _ => throw new RuntimeException("impossibility")
      })
    }
    parameterTypes = parameterTypes ::: List(boundClassType)

    // If any parameters are being proxied, change the parameter list accordingly  
    for (proxiedValue <- proxiedValues) {
      proxiedValue match {
        case p: ParameterRef =>
          parameterTypes = parameterTypes.patch(p.getIndex, Seq(proxyType), 1)
        case _ =>
      }
    }
    val returnType: Type = if (proxyReturnValue) proxyType else sourceMethod.getReturnType
    val method = new SootMethod(proxyMethodName, parameterTypes, returnType, Modifier.PUBLIC | Modifier.STATIC)
    // Add method to proxy class
    gen.proxyClass.addMethod(method)
    method
  }

  // to be called /after/ all proxied methods are added to the methodMap
  def createBody = {
    val body = Jimple.v().newBody(proxyMethod)
    proxyMethod.setActiveBody(body)
    val units = body.getUnits()
    // todo --- rewrite function with proxied values!
    units.add(Jimple.v.newReturnVoidStmt)

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
    val tmp = SootUtils.addNewUniqueLocal("l", method, ftype)

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

/*
 * refactorTargets maps sourceMethod -> (set of values to proxy, boolean)
 *    where the set of values are either local variables or parameters
 *    and the boolean indicates whether or not to proxy the return type
 *    
 *    Use of a mapping for refactorTargets reflects the fact that only one proxy method should be created for each source method.
 */
class ProxyGenerator(targetClass: SootClass, proxiedFields: Set[SootField], refactorTargets: Map[SootMethod, (Set[Value], Boolean)]) {
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
    units.add(j.newIdentityStmt(targetUncast, j.newParameterRef(objectType, 0)))
    // target = (TARGETCLASS) targetUncast
    units.add(j.newAssignStmt(target, j.newCastExpr(targetUncast, targetType)))

    // all of the code for boxing all fields
    units.addAll(fieldMap.values.map(_.box(method, thisproxy, target)).reduce(_ ::: _))

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
    for (sourceMethod <- refactorTargets.keys.toList.sortBy(m => m.getSignature())) {
      // you are here
      val (proxiedValues, proxyReturnValue) = refactorTargets(sourceMethod)
      val methodProxy = new MethodProxy(sourceMethod, proxiedValues, proxyReturnValue, this)
      methodMap.put(sourceMethod, methodProxy)
      println("create proxy method:\n  source: %s\n  dest: %s\n".format(sourceMethod, methodProxy))
    }

    for (methodProxy <- methodMap.values) {
      methodProxy.createBody
    }

    implementBoxMethod
    implementApplyDiffMethod
    implementConstructor
    implementUpdateMethod

    proxyClass
  }
}