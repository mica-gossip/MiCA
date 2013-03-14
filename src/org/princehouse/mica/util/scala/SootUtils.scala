package org.princehouse.mica.util.scala
import soot.SootMethod
import soot.SootClass
import soot.Scene
import soot.PackManager
import soot.options.Options
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
        println("leftBox = " + dumpValueBox(x.leftBox))
        println("rightBox = " + dumpValueBox(x.rightBox))
      case x: JBreakpointStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JEnterMonitorStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JExitMonitorStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JGotoStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JIdentityStmt =>
        println("  leftBox = " + dumpValueBox(x.leftBox))
        println("  rightBox = " + dumpValueBox(x.rightBox))
      case x: JIfStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JInvokeStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JLookupSwitchStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JNopStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JRetStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JReturnStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JReturnVoidStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JTableSwitchStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JThrowStmt =>
        throw new RuntimeException("unhandled unit type")
      case _ =>
        throw new RuntimeException("unknown unit class")
    })
    println("\n")
  }

  def dumpValueBox(b: soot.ValueBox): String = {
    val v: soot.Value = b.getValue()
    return "[" + b.getClass().getSimpleName() + "] getValue() = " + dumpValue(v) +
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

  def dumpValue(v: soot.Value): String = {
    return "<" + v.getClass().getSimpleName() + " = " + v.toString() + ">" +
      (v match {
        case _ =>
          ""
      })
  }
}