package org.princehouse.mica.base.c1

import scala.collection.JavaConversions._
import scala.collection.immutable.Map
import scala.collection.immutable.Set
import org.princehouse.mica.base.model.CommunicationPatternAgent
import org.princehouse.mica.base.model.Compiler
import org.princehouse.mica.base.model.Protocol
import org.princehouse.mica.util.reflection.ReflectionUtil
import org.princehouse.mica.util.scala.SootUtils
import com.esotericsoftware.kryo.Kryo
import soot.options.Options
import soot.PackManager
import soot.Scene
import soot.SootClass
import soot.SootMethod
import java.lang.reflect.Field

class AnalysisResult(val kryo: Kryo) {}

object C1CompilerCache {
  var analysisCache: Map[java.lang.Class[_ <: Protocol], AnalysisResult] = Map()
}

class C1Compiler extends Compiler {

  def compile(p: Protocol): CommunicationPatternAgent = {
    val kryo: Kryo = doStaticAnalysis(p.getClass()).kryo
    return new C1CommunicationPatternAgent(kryo)
  }

  // doStaticAnalysis is a memoizing facade for doStaticAnalysisHelper
  private def doStaticAnalysis(pclass: java.lang.Class[_ <: Protocol]) = {
    C1CompilerCache.analysisCache.get(pclass) match {
      case Some(result) => result
      case None => {
        val result = doStaticAnalysisHelper(pclass)
        C1CompilerCache.analysisCache = C1CompilerCache.analysisCache + (pclass -> result)
        result
      }
    }
  }

  private def doStaticAnalysisHelper(pclass: java.lang.Class[_ <: Protocol]): AnalysisResult = {
    val sootArgs = "-w -W -app -f jimple -p jb use-original-names:true -p cg.spark on -p cg.spark simplify-offline:true -p jop.cse on -p wjop.smb on -p wjop.si off".split(" ")
    Options.v().parse(sootArgs)
    val c = SootUtils.forceResolveJavaClass(pclass, SootClass.BODIES)
    c.setApplicationClass()
    Scene.v().loadNecessaryClasses()
    
    val entryMethod: SootMethod = SootUtils.getInheritedMethodByName(c, "update") match {
      case Some(x) => x
      case None => throw new RuntimeException("No update method found in protocol")
    }

    Scene.v().setEntryPoints(List(entryMethod))
    PackManager.v.runPacks
    val pta = Scene.v().getPointsToAnalysis()
    val cg = Scene.v().getCallGraph()
    val usedFieldsSoot = SootUtils.getUsedFields(entryMethod, cg, mayRead=true, mayWrite=true);

    val protocolClasses = ReflectionUtil.getAllProtocolClasses()

    // might be null if there's a security exception trying to load the field's declaring class
    val usedFieldsScala = usedFieldsSoot.map(SootUtils.sootFieldToField).filter(_ != null)

    val usedFieldsJava: java.util.Set[Field] = usedFieldsScala

    dumpUsedGossipFields(usedFieldsScala, protocolClasses)

    val kryo = new Kryo()
    for (cls <- protocolClasses) {
      // TODO there's some scala build bug here.  If you get "C1Compiler not found" errors, comment then uncomment the following line...
      kryo.register(cls,new ExclusiveFieldSerializer(kryo, cls, usedFieldsJava))
          // This kryo.register line was causing a mysterious "illegal cyclic reference" scala build error.  Commented out and in 
      // and everything's fine now.      
     
    }

    return new AnalysisResult(kryo)
  }

  def dumpUsedGossipFields(usedFieldsScala:Set[Field],protocolClasses:java.util.Set[Class[_ <: Protocol]]) = {
	  for(pc <- protocolClasses) {
	    println("USED fields for class " + pc + ":")
	    for(f:Field <- usedFieldsScala) { 
	      if(pc == f.getDeclaringClass()) {
	        println("    " + f)
	      }
	    }
	  }
  }
}
    /*
    println("C1Compiler: analyze " + pclass.getName)

    val result = new AnalysisResult
    initializeSoot
    var sclass = SootUtils.forceResolveJavaClass(pclass, SootClass.BODIES)
    val Some(smethod) = SootUtils.getInheritedMethodByName(sclass, "update")

    sclass = SootUtils.scene.loadClassAndSupport(smethod.getDeclaringClass.getName)
    sclass.setApplicationClass()
    val body = smethod.retrieveActiveBody()

    // Load the control flow graph
    val cfg: ExceptionalUnitGraph = new ExceptionalUnitGraph(body)
    SootViz.exportSootDirectedGraphToDot(cfg, "debug/cfg.dot")

    // Get the PDG.  For info on this PDG implementation, see:
    // http://www.sable.mcgill.ca/soot/doc/soot/toolkits/graph/pdg/HashMutablePDG.html
    if (false) {
      val pdg = new HashMutablePDG(cfg)
      SootViz.exportSootDirectedGraphToDot(pdg, "debug/pdg.dot")
    }
    val pointsto = new BogoPointsToAnalysis(cfg)
    pointsto.go

    println("\n\n==================== Results")
    for (node <- cfg) {
      println("node: " + node)
      println("------------------------------------------")
      println("points-to before:")
      println(SootUtils.indentString(pointsto.getFlowBefore(node).toString))
      println("------------------------------------------")
      println("points-to after:")
      println(SootUtils.indentString(pointsto.getFlowAfter(node).toString))
      println("------------------------------------------\n\n")
    }
    result
*/

    // limit the singletons to this chunk of code, so they can possibly be replaced later with something more sustainable
    // -w is whole program mode for inter-procedural analysis
    // dump body causes errors -- does it terminate execution at the named phase??
    //val sootArgs = Array[String]("-v","-w","-f","jimple","-dump-body","jb")
    //val sootArgs = Array[String]("-w", "-f", "jimple")
    // note: -dump-cfg ALL prints out a lotta spam...  view with dotview script
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

    //pdg.constructPDG()
    // Initial flow info
    //val entryData = new UnitData()
    //entryData.addPath(new soot.jimple.ThisRef(smethod.getDeclaringClass().getType()), new Location("A"))
    //entryData.addPath(new soot.jimple.ParameterRef(smethod.getParameterType(0), 0), new Location("B"))
    //val flow = new TestDataFlow(cfg, entryData)
    //flow.go
  

 /* private var sootInitialized = false

  def initializeSoot: Unit = {
    //    PackManager.v().getPack("jtp").add(new
    //Transform("jpt.myanalysistagger",
    //MyAnalysisTagger.instance()));
    if (!sootInitialized) {
      sootInitialized = true
      // SootUtils.packManager.getPack("jap").add(new C1MayUseFieldAnalysis)
    }
  } */


/*
 * 
 
class Path {}
case class Location(name: String) extends Path {}
case class Assignment(source: soot.Value) extends Path {}

class UnitData {
  var source: Map[Object, Set[Path]] = Map()

  // transform x into an exact copy of this
  def copy(x: UnitData): Unit = {
    x.source = source
  }

  def merge(x: UnitData): UnitData = {
    val m = new UnitData
    val allkeys = x.source.keySet.union(source.keySet)

    def valueunion(k: Object): Set[Path] = {
      val empty: Set[Path] = Set()
      val s1: Set[Path] = source.getOrElse[Set[Path]](k, empty)
      val s2: Set[Path] = x.source.getOrElse[Set[Path]](k, empty)
      s1.union(s2)
    }
    m.source = m.source ++ allkeys.map((k) => (k, valueunion(k)))
    m
  }

  def addPath(k: java.lang.Object, p: Path) = {
    source += ((k, source.getOrElse(k, Set[Path]()) + p))
  }

  def setPath(k: java.lang.Object, p: Path) = {
    // overwrite other paths.  used for assignment
    source += ((k, Set[Path](p)))
  }

  override def equals(o: Any): Boolean = {
    o match {
      case x: UnitData =>
        source.equals(x.source)
      case _ => false
    }
  }

  def dump(name: String): Unit = {
    println("------ " + name + "---------------------------")
    for ((k, paths) <- source) {
      println(k)
      for (p <- paths) {
        println("    " + p)
      }
    }
  }

  override def hashCode(): Int = {
    source.hashCode()
  }
}

class TestDataFlow(graph: ExceptionalUnitGraph, entryData: UnitData) extends ForwardFlowAnalysis[soot.Unit, UnitData](graph) {

  def go = {
    doAnalysis
  }

  def flowThrough(in: UnitData, d: soot.Unit, out: UnitData): Unit = {
    in.copy(out)

    // Unit cases
    (d match {
      case x: soot.jimple.internal.JIdentityStmt =>
        out.setPath(x.getLeftOpBox(), new Assignment(x.getRightOp()))
      case x: soot.jimple.internal.JAssignStmt =>
        out.setPath(x.getLeftOpBox(), new Assignment(x.getRightOp()))
      case x: soot.jimple.internal.JIfStmt =>
      // fixme --- use and def need locations
      // conditional control flow statement:
      //   should not modify unitdata unless we want to be path sensitive,
      //   but still needs to have a location assigned to it
      case x: soot.jimple.internal.JGotoStmt =>
      // non-conditional
      // control flow statement:
      //   should not modify unitdata unless we want to be path sensitive
      case x: soot.jimple.internal.JInvokeStmt =>
      // fixme --- invocation may modify use boxes
      case x: soot.jimple.internal.JReturnVoidStmt =>
      // exit node
      case _ =>
        println("[UNKNOWN FLOW UNIT]")
    })

    dumpUnit(d)

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

  def merge(in1: UnitData, in2: UnitData, out: UnitData): Unit = {
    in1.merge(in2).copy(out)
  }

  def entryInitialFlow: UnitData = {
    entryData
  }

  def copy(source: UnitData, dest: UnitData): Unit = {
    source.copy(dest)
  }

  def newInitialFlow: UnitData = {
    new UnitData
  }

}

class C1Transformer extends BodyTransformer {
  // internalTransform in class BodyTransformer of type (x$1: soot.Body, x$2: java.lang.String, x$3: java.util.Map[_, _])Unit
  def internalTransform(body: Body, phaseName: String, options: java.util.Map[_, _]): Unit = {
    println("internal transform called: phaseName = " + phaseName + "  class = " +
      body.getMethod.getDeclaringClass.getName + " method = " + body.getMethod.getName);

    println("   body is of type: " + body.getClass.getName)
    for (loc <- body.getLocals) {
      println("     local: " + loc.getName + ": " + loc.getType);
    }

    for (unit <- body.getUnits) {
      println("     unit: " + unit)
    }
  }

}
class C1MayUseFieldAnalysis extends Transform("jap.c1fieldanalysis", new C1Transformer) {

}
 */


/*
 * 
class ObjectReference {
}

// ------------ object modifications
// assign an object's field
class AssignField(ref: ObjectReference, fieldName: String) {
}

// non-specific modification
class Modify(ref: ObjectReference) {
}

// ------------- read from objects
// non-specific read
class Read(ref: ObjectReference) {
}

// read a field from object
class ReadField(ref: ObjectReference, fieldName: String) {
}

*/