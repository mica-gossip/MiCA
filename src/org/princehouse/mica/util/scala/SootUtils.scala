package org.princehouse.mica.util.scala

import collection.JavaConversions._
import soot.jimple.internal._
import soot.jimple._
import soot._
import soot.util._
import java.io._
import soot.options._
import soot.jimple.toolkits.callgraph.CallGraph
import soot.jimple.toolkits.callgraph.Edge

object SootUtils {

  // inter-procedural
  def getUsedFields(method: SootMethod, cg: CallGraph, visited: collection.mutable.Set[SootMethod] = collection.mutable.Set[SootMethod]()): Set[SootField] = {
    // precondition: method is not in visited

    visited += method

    if (!method.hasActiveBody()) {
      println("  [[getUsedFields: " + method + "]]   NO ACTIVE BODY")
      return Set[SootField]()
    } else {
      println("  [[getUsedFields: " + method + "]]")
    }

    var fields = getUsedFields(method)

    for (edge: Edge <- cg.edgesOutOf(method)) {
      val sm = edge.getTgt().method()
      if (!visited.contains(sm)) {
        fields = fields.union(getUsedFields(sm, cg, visited))
      }
    }

    fields
  }

  // obj can be:  SootMethod, soot.Unit
  // not inter-procedural
  def getUsedFields(obj: Any): Set[SootField] = {
    var fields = Set[SootField]()

    println("getUsedFields: " + obj)
    obj match {
      case method: SootMethod =>
        val body = method.getActiveBody()
        for (u <- body.getUnits()) {
          fields = fields.union(getUsedFields(u))
        }
      case unit: soot.Unit =>
        for (valbox <- unit.getUseBoxes()) {
          for (value <- subValues(valbox.getValue())) {
            fields = fields.union(getUsedFields(value))
          }
        }
      case value: soot.Value =>
        value match {
          case fr: InstanceFieldRef =>
            fields = fields + fr.getField()
          case _ =>
            for (v <- subValues(value)) {
              fields = fields.union(getUsedFields(v))
            }
        }
      case _ =>
        throw new RuntimeException("unrecognized input type for getUsedFields")
    }
    fields
  }
  // compile a jimple class into a .class file
  def writeSootClass(sClass: SootClass): scala.Unit = {
    val outfile = SourceLocator.v.getFileNameFor(sClass, soot.options.Options.output_format_class)

    // ensure existence of parent directory
    val parentDir = new File(outfile).getParentFile()
    parentDir.mkdirs()

    println("Writing to " + outfile)

    val streamOut = new JasminOutputStream(new FileOutputStream(outfile))
    val writerOut = new PrintWriter(new OutputStreamWriter(streamOut))
    val jasminClass = new JasminClass(sClass)
    jasminClass.print(writerOut)
    writerOut.flush()
    streamOut.close()
  }

  // wrap the soot singletons 
  def scene = Scene.v
  def options = Options.v
  def packManager = PackManager.v

  /**
   * A more scala-like implementation of getMethodByName
   */
  def getMethodByName(sc: SootClass, name: String): Option[SootMethod] = {
    try {
      Some(sc.getMethodByName(name))
    } catch {
      case e: RuntimeException => None
    }
  }

  def forceResolveJavaClass(klass: Class[_], level: Int): SootClass = {
    var className = klass.getCanonicalName()
    scene.forceResolve(className, level)
  }

  /**
   * Returns None if the supplied class is java.lang.Object.  Note that interfaces are classes, and are subclasses of Object
   */
  def getSuperclass(sc: SootClass): Option[SootClass] = {
    if (sc.hasSuperclass) {
      Some(sc.getSuperclass())
    } else {
      None
    }
  }

  /**
   * Like getMethodByName, but searches superclasses until it finds an implementation
   */
  def getInheritedMethodByName(sc: SootClass, name: String): Option[SootMethod] = {
    getMethodByName(sc, name) match {
      case Some(method) => Some(method)
      case None => {
        getSuperclass(sc) match {
          case Some(superclass) => getInheritedMethodByName(superclass, name)
          case None => None
        }
      }
    }
  }
  def dumpUnit(d: soot.Unit) = {
    println("unit[" + d.getClass.getSimpleName + "]:" + d)

    for (usebox <- d.getUseBoxes) {
      println("   use: " + usebox)
      usebox match {
        case x: soot.AbstractValueBox =>
          val v = x.getValue
          println("     getValue[" + v.getClass.getSimpleName + "]: " + v)
        case _ =>
      }
    }
    for (defbox <- d.getDefBoxes) {
      println("   def: " + defbox)
    }
    (d match {
      case x: JAssignStmt =>
        println(" leftBox = " + dumpValueBox(x.leftBox))
        println(" rightBox = " + dumpValueBox(x.rightBox))
      case x: JBreakpointStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JEnterMonitorStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JExitMonitorStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JGotoStmt =>
      // no extra info to print
      case x: JIdentityStmt =>
        println(" leftBox = " + dumpValueBox(x.leftBox))
        println(" rightBox = " + dumpValueBox(x.rightBox))
      case x: JIfStmt =>
        println(" condition = " + dumpValue(x.getCondition()))
      case x: JInvokeStmt =>
        println(" invocation = " + dumpValue(x.getInvokeExpr()))
      case x: JLookupSwitchStmt =>
        println(" key = " + dumpValue(x.getKey()))
        for (value <- x.getLookupValues) {
          value match {
            case valuev: Value => println(" lookupValue = " + dumpValue(valuev))
            case _ => throw new RuntimeException("casting snafu")
          }
        }
      case x: JNopStmt =>
      // no extra info to print
      case x: JRetStmt =>
        println(" stmtAddress = " + dumpValue(x.getStmtAddress()))
      case x: JReturnStmt =>
        println(" op = " + x.getOp())
      case x: JReturnVoidStmt =>
      // no extra info
      case x: JTableSwitchStmt =>
        println(" key = " + dumpValue(x.getKey()))
      case x: JThrowStmt =>
        println(" op = " + x.getOp())
      case _ =>
        throw new RuntimeException("unknown unit class")
    })
    println("\n")
  }

  def dumpValueBox(b: soot.ValueBox): String = {
    val v: Value = b.getValue()
    return "[" + b.getClass().getSimpleName() + "] " + dumpValue(v) +
      (b match {
        case x: JimpleLocalBox =>
          ""
        case x: IdentityRefBox =>
          ""
        case _ =>
          val classname = b.getClass().getName()
          (classname match {
            case "soot.jimple.internal.JAssignStmt$LinkedVariableBox" =>
              ""
            case "soot.jimple.internal.JAssignStmt$LinkedRValueBox" =>
              ""
            case _ =>
              throw new RuntimeException("unhandled value box class " + classname)
          })
      })
  }

  def dumpValue(v: Value): String = {
    var s = "<" + v.getClass().getSimpleName() + " = " + v.toString() + ">"
    for (sv <- subValues(v)) {
      s = s + "\n" + indentString(dumpValue(sv))
    }
    s
  }

  def indentString(s: String): String = {
    (s.split("\n").map("  " + _)).reduceLeft(_ + "\n" + _)
  }

  def subValuesLeaves(v: Value): List[Value] = {
    val lvs = subValues(v)
    lvs match {
      case Nil => List(v)
      case _ => lvs.map(subValuesLeaves(_)).reduce(_ ::: _)
    }
  }

  def subValues(v: Value): List[Value] = {
    var r: List[Value] = Nil
    for (o <- v.getUseBoxes()) {
      val temp = (o match {
        case x: soot.ValueBox => x.getValue
        case _ => throw new RuntimeException("uh oh")
      })
      r = temp :: r
    }
    r
    /*v match {
      case x: AbstractBinopExpr => List(x.getOp1, x.getOp2)
      case x: Constant => Nil
      case x: ThisRef => Nil
      case x: StaticFieldRef => Nil
      case x: ParameterRef => Nil
      case x: JArrayRef => List(x.getBase(), x.getIndex())
      case _ =>
        throw new RuntimeException("unrecognized value class for subValues(): " + v.getClass().getName())
    }*/
  }

  def isStatic(sootThing: java.lang.Object): Boolean = {
    val modifiers = sootThing match {
      case x: SootMethod => x.getModifiers
      case x: SootField => x.getModifiers
      case _ =>
        throw new RuntimeException("Unsupported parameter type (%s) for isStatic.  Only SootMethod and SootField are supported".format(sootThing.getClass().getName()))
    }
    ((modifiers & Modifier.STATIC) == Modifier.STATIC)
  }

  val TRUE = IntConstant.v(1)
  val FALSE = IntConstant.v(0)

  // create a new local variable with a unique name and add it to the soot body
  def addNewUniqueLocal(nameTemplate: String, method: SootMethod, ltype: Type): Local = {
    val j = Jimple.v
    val locals = method.getActiveBody.getLocals()
    // find a unique name of the form l%d
    val localNames = locals.map(_.getName).toSet
    var i = 0
    def name: String = {
      if (i == 0) { nameTemplate } else { "%s%s".format(nameTemplate, i) }
    }
    while (localNames.contains(name)) { i += 1 }
    val local = j.newLocal(name, ltype)
    locals.add(local)
    local
  }

  /* condition: Either a BooleanType (in which case it will be replaced with (condition==true)), or a conditional operator expr (==,>,<,etc)
   * thenStmts: Either a unit or a list of units
   * elseStmts: Either a unit or a list of units
   */
  def ifThenElse(condition: Value, thenClause: Any, elseClause: Any): List[Unit] = {
    val j = Jimple.v

    val finished = j.newNopStmt()

    def asUnitList(v: Any) = {
      v match {
        case x: Unit => List(x)
        case x: List[Unit] => x
        case _ => throw new RuntimeException("unrecognized type for interpretation as List[Unit]")
      }
    }

    val binopCondition = condition match {
      case x: AbstractJimpleIntBinopExpr => x
      case _ =>
        condition.getType() match {
          case t: BooleanType =>
            j.newEqExpr(condition, TRUE)
          case _ =>
            throw new RuntimeException("unrecognized type for BinopExpr casting")
        }
    }

    val elseList = asUnitList(elseClause)

    j.newIfStmt(binopCondition, elseList.head) :: asUnitList(thenClause) ::: List(j.newGotoStmt(finished)) ::: elseList ::: List(finished)
  }

}