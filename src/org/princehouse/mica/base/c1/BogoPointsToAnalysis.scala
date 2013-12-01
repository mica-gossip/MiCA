package org.princehouse.mica.base.c1

import soot.toolkits.scalar.ForwardFlowAnalysis
import collection.JavaConversions._
import collection.immutable.Map
import collection.immutable.Set
import soot.toolkits.graph.ExceptionalUnitGraph
import soot.jimple.internal._
import _root_.org.princehouse.mica.util.scala.SootUtils
import soot.ValueBox
import soot.Value
import soot.jimple.ThisRef
import soot.jimple.ParameterRef

// maps variables to a set of possible runtime objects
class BogoEnvironment[OBJ] {
  val chatty = true
  var source: Map[Object, Set[OBJ]] = Map()

  // transform x into an exact copy of this
  def copy(x: BogoEnvironment[OBJ]): Unit = {
    x.source = source
  }

  // non-destructive
  def merge(x: BogoEnvironment[OBJ]): BogoEnvironment[OBJ] = {
    val m = new BogoEnvironment[OBJ]
    val allkeys = x.source.keySet.union(source.keySet)

    def valueunion(k: Object): Set[OBJ] = {
      val empty: Set[OBJ] = Set()
      val s1: Set[OBJ] = source.getOrElse[Set[OBJ]](k, empty)
      val s2: Set[OBJ] = x.source.getOrElse[Set[OBJ]](k, empty)
      s1.union(s2)
    }
    m.source = m.source ++ allkeys.map((k) => (k, valueunion(k)))
    m
  }

  def addRuntimeObject(k: java.lang.Object, p: OBJ) = {
    source += ((k, source.getOrElse(k, Set[OBJ]()) + p))
    if (chatty) println("env add %s -> %s".format(k, p))
  }

  def getRuntimeObjects(k: java.lang.Object) = {
    // throws an exception if no entry for key
    source.get(k) match {
      case Some(x) => x
      case None => throw new RuntimeException("variable environment has no entry for %s".format(k))
    }
  }
  def setRuntimeObjects(k: java.lang.Object, p: Set[OBJ]) = {
    // overwrite other Ints.  used for assignment
    source += ((k, p))
    if (chatty) println("env set %s -> %s".format(k, p))
  }

  override def equals(o: Any): Boolean = {
    o match {
      case x: BogoEnvironment[OBJ] =>
        source.equals(x.source)
      case _ => false
    }
  }

  def dump(name: String): Unit = {
    println("------ " + name + "---------------------------")
    for ((k, objectSet) <- source) {
      println(k)
      for (p <- objectSet) {
        println("    " + p)
      }
    }
  }

  override def hashCode(): Int = {
    source.hashCode()
  }

  override def toString: String = {
    var s: List[String] = Nil
    for ((k, v) <- source.elements) {
      s = ("%3s:  %s".format(k, v)) :: s
    }
    s match {
      case Nil => "(empty)"
      case _ => s.reverse.reduceLeft(_ + "\n" + _)
    }
  }
}

// associates each expression with its own object
class BogoPointsToAnalysis(graph: ExceptionalUnitGraph) extends ForwardFlowAnalysis[soot.Unit, BogoEnvironment[Int]](graph) {

  val chatty = true

  var counter: Int = 1

  def newRuntimeObject: Int = {
    counter += 1
    counter
  }

  val identities: collection.mutable.Map[Object, Set[Int]] = collection.mutable.Map(
    "@this" -> Set(newRuntimeObject),
    "@parameter0" -> Set(newRuntimeObject))

  def getIdentityObject(s: String): Set[Int] = {
    identities.get(s) match {
      case Some(x) => x
      case None => throw new RuntimeException("identity not found " + s)
    }
  }

  def getOrCreateIdentityObject(key: Object): Set[Int] = {
    identities.get(key) match {
      case Some(x) => x
      case None =>
        val x = Set(newRuntimeObject)
        identities.put(key, x)
        if (chatty) println("identity put %s = %s".format(key, x))
        x
    }
  }

  // run the analysis. doAnalysis is protected, so we need to wrap it to call externally
  def go = {
    doAnalysis
  }

  def getValueVariable(v: Value): Option[Object] = {
    (v match {
      case x: JimpleLocal => Some(x)
      case x: JInstanceFieldRef => None // fixme is this right?  how to handle field assignments? should r0.x be treated as a separate variable from r0??
      case _ => None
    })
  }

  def getValueObject(v: Value, env: BogoEnvironment[Int], unit: soot.Unit): Set[Int] = {
    getValueVariable(v) match {
      case Some(variable) =>
        env.getRuntimeObjects(variable)
      case None =>
        v match {
          case x: ThisRef => getIdentityObject("@this")
          case x: ParameterRef => getIdentityObject("@parameter%d".format(x.getIndex()))
          case _ =>
            val subobjects = SootUtils.subValues(v).map(x => getValueObject(x, env, unit))
            val key = (unit, v, subobjects)
            getOrCreateIdentityObject(key)
        }
    }
  }

  def flowThrough(in: BogoEnvironment[Int], unit: soot.Unit, out: BogoEnvironment[Int]): Unit = {

    println("\npointsTo.flowThrough:")
    SootUtils.dumpUnit(unit)
    in.copy(out)

    (unit match {
      case x: JAssignStmt =>
        getValueVariable(x.getLeftOp) match {
          case Some(variable) =>
            out.setRuntimeObjects(variable,
              getValueObject(x.getRightOp(), in, unit))
          case None =>
        }

      case x: JBreakpointStmt =>
        throw new RuntimeException("unhandled unit type")

      case x: JEnterMonitorStmt =>
        throw new RuntimeException("unhandled unit type")

      case x: JExitMonitorStmt =>
        throw new RuntimeException("unhandled unit type")

      case x: JGotoStmt =>
      // no environment modification

      case x: JIdentityStmt =>
        getValueVariable(x.getLeftOp) match {
          case Some(variable) =>
            out.setRuntimeObjects(variable,
              getValueObject(x.getRightOp(), in, unit))
          case None =>
        }

      case x: JIfStmt =>
      // no environment modification

      case x: JInvokeStmt =>
      // no environment modification

      case x: JLookupSwitchStmt =>
        throw new RuntimeException("unhandled unit type")

      case x: JNopStmt =>
        throw new RuntimeException("unhandled unit type")

      case x: JRetStmt =>
        throw new RuntimeException("unhandled unit type")

      case x: JReturnStmt =>
      //

      case x: JReturnVoidStmt =>
      //

      case x: JTableSwitchStmt =>
        throw new RuntimeException("unhandled unit type")

      case x: JThrowStmt =>
        throw new RuntimeException("unhandled unit type")

      case _ =>
        throw new RuntimeException("unhandled unit type")

    })

  }

  def merge(in1: BogoEnvironment[Int], in2: BogoEnvironment[Int], out: BogoEnvironment[Int]): Unit = {
    in1.merge(in2).copy(out)
  }

  def entryInitialFlow: BogoEnvironment[Int] = {
    new BogoEnvironment
  }

  def copy(source: BogoEnvironment[Int], dest: BogoEnvironment[Int]): Unit = {
    source.copy(dest)
  }

  def newInitialFlow: BogoEnvironment[Int] = {
    new BogoEnvironment
  }

  def matchValueBox(v: soot.ValueBox): Unit = {
    v match {
      case x: IdentityRefBox =>
      case x: ImmediateBox =>
      case x: InvokeExprBox =>
      case x: JimpleLocalBox =>
      case x: VariableBox =>
      case _ =>
        throw new RuntimeException("unrecognized value box " + (v.getClass().getName()))
    }
  }

}