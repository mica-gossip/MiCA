package org.princehouse.mica.base.c1

import org.princehouse.mica.base.model.Compiler
import org.princehouse.mica.base.model.CommunicationPatternAgent
import org.princehouse.mica.base.model.Protocol
import org.princehouse.mica.base.simple.SimpleCommunicationPatternAgent
import soot.options.Options
import soot.SootClass
import soot.Scene
import org.princehouse.mica.util.Functional
import soot.PackManager
import collection.JavaConversions._
import org.princehouse.mica.util.scala.SootUtils

class C1Compiler extends Compiler {

  private val pattern = new SimpleCommunicationPatternAgent;

  private var analysisCache: Map[java.lang.Class[_ <: Protocol], AnalysisResult] = Map()

  def compile(p: Protocol): CommunicationPatternAgent = {
    analyze(p.getClass)
    pattern
  }

  private def analyze(pclass: java.lang.Class[_ <: Protocol]) = {
    analysisCache.get(pclass) match {
      case Some(result) => result
      case None => {
        val result = doAnalysis(pclass)
        analysisCache = analysisCache + (pclass -> result)
        result
      }
    }
  }

  private def doAnalysis(pclass: java.lang.Class[_ <: Protocol]) = {
    println("C1Compiler: analyze " + pclass.getName)

    val result = new AnalysisResult

    // limit the singletons to this chunk of code, so they can possibly be replaced later with something more sustainable

    // -w is whole program mode for inter-procedural analysis

    // dump body causes errors -- does it terminate execution at the named phase??
    //val sootArgs = Array[String]("-v","-w","-f","jimple","-dump-body","jb")

    //val sootArgs = Array[String]("-w", "-f", "jimple")

    // note: -dump-cfg ALL prints out a lotta spam...  view with dotview script
    val sootArgs = Array[String]("-w", "-f", "jimple", "-dump-cfg","ALL")

    SootUtils.options.parse(sootArgs)

    val sclass = SootUtils.forceResolveJavaClass(pclass, SootClass.BODIES)

    val Some(smethod) = SootUtils.getInheritedMethodByName(sclass, "update")

    val mclass = smethod.getDeclaringClass()

    println("Declaring class:" + mclass)
    mclass.setApplicationClass()
    SootUtils.scene.loadNecessaryClasses()

    SootUtils.scene.setEntryPoints(List(smethod))

    try {
      SootUtils.packManager.runPacks()
    } catch {
      case e: RuntimeException =>
        e.printStackTrace()
      //println("Scene resolved classes at BODIES level:")
      //for(k <- SootUtils.scene.getClasses(SootClass.BODIES)) {
      //  println(k)
      //}
    }
    println("Done")
    result
  }

}

class AnalysisResult {

}