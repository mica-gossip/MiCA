package org.princehouse.mica.util.scala
import soot.SootMethod
import soot.SootClass
import soot.Scene
import soot.PackManager
import soot.options.Options
import soot.Value
import collection.JavaConversions._
import soot.jimple.internal._
import soot.jimple._

object SootUtils {

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
            case valuev:Value => println(" lookupValue = " + dumpValue(valuev))
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

  def subValuesLeaves(v: Value) : List[Value] = {
    val lvs = subValues(v)
    lvs match {
      case Nil => List(v)
      case _ => lvs.map(subValuesLeaves(_)).reduce(_:::_)
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
}