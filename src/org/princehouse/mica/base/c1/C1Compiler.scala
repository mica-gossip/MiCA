package org.princehouse.mica.base.c1

import org.princehouse.mica.base.model.Compiler
import org.princehouse.mica.base.model.CommunicationPatternAgent
import org.princehouse.mica.base.model.Protocol
import org.princehouse.mica.base.simple.SimpleCommunicationPatternAgent
import soot.options.Options
import soot.SootClass
import soot.Scene
import soot.Body
import soot.Transform
import soot.toolkits.graph.UnitGraph
import soot.toolkits.graph.ExceptionalUnitGraph
import soot.BodyTransformer
import org.princehouse.mica.util.Functional
import soot.PackManager
import collection.JavaConversions._
import collection.mutable.Map
import collection.mutable.Set
import org.princehouse.mica.util.scala.SootUtils
import soot.toolkits.scalar.ForwardFlowAnalysis
import soot.toolkits.graph.DirectedGraph

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
    initializeSoot
   //val sootArgs = Array[String]("-w", "-f", "jimple")
    /*
    val sootArgs = Array[String]("-f", "jimple")
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
    }
    println("Done") */
    
    var sclass = SootUtils.forceResolveJavaClass(pclass, SootClass.BODIES)
    val Some(smethod) = SootUtils.getInheritedMethodByName(sclass, "update")
    
    sclass = SootUtils.scene.loadClassAndSupport(smethod.getDeclaringClass.getName)
    sclass.setApplicationClass()
    val body = smethod.retrieveActiveBody()
    val graph : ExceptionalUnitGraph = new ExceptionalUnitGraph(body)
    val flow = new TestDataFlow(graph)
    flow.go
    result
  }

  private var sootInitialized = false

  def initializeSoot: Unit = {
    //    PackManager.v().getPack("jtp").add(new
    //Transform("jpt.myanalysistagger",
    //MyAnalysisTagger.instance()));
    if (!sootInitialized) {
      sootInitialized = true
      SootUtils.packManager.getPack("jap").add(new C1MayUseFieldAnalysis)
    }
  }
}

/*
class Skub[X,Y](zub:X) {
  def bubu(arz : X) = {
    
  }
}
*/

class Path {
}

class UnitData {
	var source:Map[Object,Set[Path]] = Map()
	
	// transform x into an exact copy of this
	def copy(x:UnitData) : Unit = {
	  x.source.clear()
	  for((k,v) <- source) {
	    val ns:Set[Path] = Set()
	    for(p <- v) {
	      ns.add(p)
	    } 
	    x.source(k) = ns
	  } 
	}
}

class TestDataFlow(graph:ExceptionalUnitGraph) extends ForwardFlowAnalysis[soot.Unit,UnitData](graph) {
    
  
  def go = {
    doAnalysis
  }
  
  def flowThrough(in:UnitData, d:soot.Unit, out:UnitData) : Unit = {
    // fixme
    //println("\nflowThrough. in:" + in + " d["+d.getClass.getSimpleName+"]:" + d + " out:" + out)
    println("\nflow unit["+d.getClass.getSimpleName+"]:" + d)

    for(usebox <- d.getUseBoxes) {
      println("   use: " + usebox)
    }
    for(defbox <- d.getDefBoxes) {
      println("   def: " + defbox)
    }
  }
  
  def merge(in1:UnitData, in2:UnitData, out:UnitData) : Unit = {
    println("merge. in1:"+in1+" in2:"+in2+" out:"+out)
  }
  
  def entryInitialFlow : UnitData = {
    new UnitData
  }
  
  def copy(source:UnitData, dest:UnitData) : Unit = {
    source.copy(dest)
  }
  
  def newInitialFlow : UnitData = {
    new UnitData
  }
  
  
  
}


class C1Transformer extends BodyTransformer {
  // internalTransform in class BodyTransformer of type (x$1: soot.Body, x$2: java.lang.String, x$3: java.util.Map[_, _])Unit
  def internalTransform(body: Body, phaseName: String, options: java.util.Map[_, _]): Unit = {
    println("internal transform called: phaseName = " + phaseName + "  class = " +
      body.getMethod.getDeclaringClass.getName + " method = " + body.getMethod.getName);
    
    println("   body is of type: " + body.getClass.getName)
    for(loc <- body.getLocals) {
      println("     local: " + loc.getName  + ": " + loc.getType);
    }
    
    for(unit <- body.getUnits) {
      println("     unit: " + unit)
    }
  }
  
}
class C1MayUseFieldAnalysis extends Transform("jap.c1fieldanalysis", new C1Transformer) {

}

class AnalysisResult {

}


class ObjectReference { 
}

// ------------ object modifications
// assign an object's field
class AssignField(ref:ObjectReference, fieldName:String) {
}

// non-specific modification
class Modify(ref:ObjectReference) {
}

// ------------- read from objects
// non-specific read
class Read(ref:ObjectReference) {
}

// read a field from object
class ReadField(ref:ObjectReference, fieldName:String) {
}

