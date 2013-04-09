package org.princehouse.mica.test.analysis

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import org.princehouse.mica.test.analysis.C1TestProtocol
import org.princehouse.mica.util.scala.SootUtils
import soot.options.Options
import soot.toolkits.graph.ExceptionalUnitGraph
import soot.Local
import soot.PackManager
import soot.Scene
import soot.SootClass
import soot.SootMethod
import org.princehouse.mica.util.scala.SootViz

object TestPointsToSpark {

  def main(args: Array[String]): scala.Unit = {

    val protocolClass = classOf[C1TestProtocol]

    val sootArgs = "-w -W -app -f jimple -p jb use-original-names:true -p cg.spark on -p cg.spark simplify-offline:true -p jop.cse on -p wjop.smb on -p wjop.si off".split(" ")
    Options.v().parse(sootArgs)

    val c = SootUtils.forceResolveJavaClass(protocolClass, SootClass.BODIES)
    c.setApplicationClass()
    Scene.v().loadNecessaryClasses()
    val entryMethod: SootMethod = c.getMethodByName("update")
    Scene.v().setEntryPoints(List(entryMethod))
    PackManager.v.runPacks

    val pta = Scene.v().getPointsToAnalysis()

    val body = entryMethod.retrieveActiveBody()
    val cfg: ExceptionalUnitGraph = new ExceptionalUnitGraph(body)

    println("------------------ points to sets-----------------")
    var allLocals = collection.mutable.Set[Local]()
    
    for (node <- cfg) {
      SootUtils.dumpUnit(node)

      for (supervalue <- node.getUseBoxes()) {
        for (value <- SootUtils.subValuesLeaves(supervalue.getValue())) {
          val pointsTo: Option[soot.PointsToSet] = value match {
            case x: soot.Local =>
              allLocals.add(x)
              Some(pta.reachingObjects(node, x))
            case x: soot.jimple.internal.JInstanceFieldRef =>
              val base = x.getBase()
              base match {
                case b:soot.Local =>
                  Some(pta.reachingObjects(node, b, x.getField()))
                case _ =>
                  println("confusion: don't know how to handle base of class %s".format(base.getClass().getName()))
                  None
              }
            case _ => None
          }
          
          pointsTo match {
            case Some(objSet) => 
            	println("[%s set] = %s".format(value,(if(objSet.isEmpty) "empty" else objSet)))
            case None => 
          }
        }
      }
      println("")
    }
    
    println("---------------------- all locals points-to info, no context")
    for(local:Local <- allLocals.toSet) {
      val rset = pta.reachingObjects(local)
      println("   %s : %s -->  %s ".format(local, rset.possibleTypes(), rset))
    }

    println("pta class is " + pta.getClass().getName())
    
    
    val cg = Scene.v().getCallGraph()
    
    println("---- writing call graph to sootOutput/callgraph.dot")
    SootViz.exportCallGraphToDot(cg,"sootOutput/callgraph.dot")
    
    println("number of call graph edges: %d\n".format( cg.size() ));
    
    
    // ------------------ analyze field usage
    
    
    // org.princehouse.mica.test.analysis

    //val sootArgs = Array[String]("-v","-w","-f","jimple","-dump-body","jb")
    //val sootArgs = Array[String]("-w", "-f", "jimple")
    // note: -dump-cfg ALL prints out a lotta spam...  view with dotview script

    //    PackManager.v().getPack("jtp").add(new
    //Transform("jpt.myanalysistagger",
    //MyAnalysisTagger.instance()));

    // SootUtils.packManager.getPack("jap").add(new C1MayUseFieldAnalysis)

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

    /*    var sclass = SootUtils.forceResolveJavaClass(pclass, SootClass.BODIES)
    val Some(smethod) = SootUtils.getInheritedMethodByName(sclass, "update")

    sclass = SootUtils.scene.loadClassAndSupport(smethod.getDeclaringClass.getName)
    sclass.setApplicationClass()
    val body = smethod.retrieveActiveBody()

    // Load the control flow graph
    val cfg: ExceptionalUnitGraph = new ExceptionalUnitGraph(body)
    SootViz.exportSootDirectedGraphToDot(cfg, "debug/cfg.dot")
*/
    // Get the PDG.  For info on this PDG implementation, see:
    // http://www.sable.mcgill.ca/soot/doc/soot/toolkits/graph/pdg/HashMutablePDG.html
    //   if (false) {
    //     val pdg = new HashMutablePDG(cfg)
    //     SootViz.exportSootDirectedGraphToDot(pdg, "debug/pdg.dot")
    //   }

    //pdg.constructPDG()
    // Initial flow info
    //val entryData = new UnitData()
    //entryData.addPath(new soot.jimple.ThisRef(smethod.getDeclaringClass().getType()), new Location("A"))
    //entryData.addPath(new soot.jimple.ParameterRef(smethod.getParameterType(0), 0), new Location("B"))

    //val flow = new TestDataFlow(cfg, entryData)
    //flow.go
  }
}


