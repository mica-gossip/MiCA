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
class Environment[OBJ] {
  var source: Map[Object, Set[OBJ]] = Map()

  // transform x into an exact copy of this
  def copy(x: Environment[OBJ]): Unit = {
    x.source = source
  }

  // non-destructive
  def merge(x: Environment[OBJ]): Environment[OBJ] = {
    val m = new Environment[OBJ]
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
  }

  def setRuntimeObjects(k: java.lang.Object, p: Set[OBJ]) = {
    // overwrite other Ints.  used for assignment
    source += ((k, p))
  }

  override def equals(o: Any): Boolean = {
    o match {
      case x: Environment[OBJ] =>
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
}

// associates each variable reference with an object
class PointsToAnalysis(graph: ExceptionalUnitGraph) extends ForwardFlowAnalysis[soot.Unit, Environment[Int]](graph) {

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
        x
    }
  }

  // run the analysis. doAnalysis is protected, so we need to wrap it to call externally
  def go = {
    doAnalysis
  }

  def getValueBoxVariable(b: ValueBox): java.lang.Object = {
    getValueVariable(b.getValue())
  }

  def getValueVariable(v: Value): java.lang.Object = {
    (v match {
      case x: JimpleLocal => x
      case x: JInstanceFieldRef => x // fixme is this right?  how to handle field assignments? should r0.x be treated as a separate variable from r0??
      case _ => throw new RuntimeException("todo:" + v.getClass().getName() + " == " + v)
    })
  }

  def getValueBoxObject(b: ValueBox, env: Environment[Int], unit: soot.Unit): Set[Int] = {
    getValueObject(b.getValue(), env, unit)
  }

  def getValueObject(v: Value, env: Environment[Int], unit: soot.Unit): Set[Int] = {
    (v match {
      case x: ThisRef => getIdentityObject(x.toString().split(":")(0))
      case x: ParameterRef => getOrCreateIdentityObject(x.toString().split(":")(0))
      case x: JCastExpr => getValueObject(x.getOp, env, unit)
      case x: JimpleLocal =>
        env.source.get(x) match {
          case Some(y) => y
          case None => throw new RuntimeException("variable->object not in environment map?!")
        }
      case x: JInstanceFieldRef =>
        // you are here
        (getOrCreateIdentityObject((unit, x.getField(),
          getValueObject(x.getBase(), env, unit))))
      case x: JVirtualInvokeExpr =>
        getOrCreateIdentityObject((unit, getValueObject(x.getBase(), env, unit), x.getMethod()))
      case x: JStaticInvokeExpr =>
        getOrCreateIdentityObject((unit, x.getMethod()))
      case x: JMulExpr =>
        getOrCreateIdentityObject((x.getSymbol(),
          getOrCreateIdentityObject(x.getOp1(), env, unit),
          getOrCreateIdentityObject(x.getOp2(), env, unit)))

      case _ => throw new RuntimeException("todo:" + v.getClass().getName() + "(str=" + v.toString() + ")")
    })
  }

  def flowThrough(in: Environment[Int], unit: soot.Unit, out: Environment[Int]): Unit = {

    println("\npointsTo.flowThrough:")
    SootUtils.dumpUnit(unit)
    in.copy(out)

    (unit match {
      case x: JAssignStmt =>

        out.setRuntimeObjects(getValueBoxVariable(x.leftBox),
          getValueBoxObject(x.rightBox, in, unit))

      case x: JBreakpointStmt =>
        throw new RuntimeException("unhandled unit type")

      case x: JEnterMonitorStmt =>
        throw new RuntimeException("unhandled unit type")

      case x: JExitMonitorStmt =>
        throw new RuntimeException("unhandled unit type")

      case x: JGotoStmt =>
        throw new RuntimeException("unhandled unit type")

      case x: JIdentityStmt =>
        out.setRuntimeObjects(getValueBoxVariable(x.leftBox),
          getValueBoxObject(x.rightBox, in, unit))

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
        throw new RuntimeException("unhandled unit type")

    })

  }

  def merge(in1: Environment[Int], in2: Environment[Int], out: Environment[Int]): Unit = {
    in1.merge(in2).copy(out)
  }

  def entryInitialFlow: Environment[Int] = {
    new Environment
  }

  def copy(source: Environment[Int], dest: Environment[Int]): Unit = {
    source.copy(dest)
  }

  def newInitialFlow: Environment[Int] = {
    new Environment
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