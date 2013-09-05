package org.princehouse.mica.test.analysis

import scala.collection.JavaConversions.seqAsJavaList

import org.princehouse.mica.test.analysis.C1TestProtocol
import org.princehouse.mica.util.scala.SootUtils

import soot.Scene
import soot.SootClass
import soot.SootMethod

object TestFieldUseAnalysisLocal {

  def main(args: Array[String]): scala.Unit = {

    val protocolClass = classOf[C1TestProtocol]
    Scene.v().loadClassAndSupport(protocolClass.getName())
        val c = SootUtils.forceResolveJavaClass(protocolClass, SootClass.BODIES)
    c.setApplicationClass()
    Scene.v().loadNecessaryClasses()

    val entryMethod: SootMethod = c.getMethodByName("update")
    entryMethod.retrieveActiveBody()
    Scene.v().setEntryPoints(List(entryMethod))
   
    println("analyzing local used fields of entry method")
    for(field <- SootUtils.getUsedFields(entryMethod, mayRead=true, mayWrite=true)) {
      println("  field: " + field)
    }
   }
}


