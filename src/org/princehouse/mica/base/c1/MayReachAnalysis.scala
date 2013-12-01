package org.princehouse.mica.base.c1

import soot.jimple.internal._
import soot.toolkits.graph.ExceptionalUnitGraph
import soot.toolkits.scalar.ForwardFlowAnalysis
import collection.JavaConversions._

/**
 * A template for data flow on soot, using Scala.  Includes cases for soot units and expressions
 */
abstract class MayReachAnalysis[T](graph: ExceptionalUnitGraph, entryData: T) extends ForwardFlowAnalysis[soot.Unit, T](graph) {

  def flowThrough(in: T, d: soot.Unit, out: T): Unit = {
  }

  /*
   def merge(in1: T, in2: T, out: T): Unit = {
  }

  def entryInitialFlow: T = {
  }

  def copy(source: T, dest: T): Unit = {
  }

  def newInitialFlow: T = {
  }
*/

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

  // template for matching against all jimple units
  def matchJimpleUnit(u: soot.Unit): Unit = {
    (u match {
      case x: JAssignStmt =>
        // leftBox : ValueBox
        // rightBox : ValueBox
        throw new RuntimeException("unhandled unit type")
      case x: JBreakpointStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JEnterMonitorStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JExitMonitorStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JGotoStmt =>
        throw new RuntimeException("unhandled unit type")
      case x: JIdentityStmt =>
        throw new RuntimeException("unhandled unit type")
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
    println("\n")
  }

}