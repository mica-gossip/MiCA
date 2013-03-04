package org.princehouse.mica.util.scala

import org.jgrapht._
import org.jgrapht.graph._
import org.jgrapht.ext.DOTExporter
import java.io.FileWriter
import scala.collection.JavaConversions._

object SootViz {
  //implicit def javaIteratorToScalaIterator[A](it: java.util.Iterator[A]) = new Wrapper(it)

  def exportJGraphToDot[V, E](graph: Graph[V, E], filename: String): Unit = {
    
    val exporter = new DOTExporter[V, E](
      new org.jgrapht.ext.VertexNameProvider[V]() {
        override def getVertexName(v: V): String = {
          v.hashCode().toString()
        }
      },
      new org.jgrapht.ext.VertexNameProvider[V]() {
        override def getVertexName(v: V): String = {
          dotEscapeString(v.toString())
        }
      }, null)
    val fw = new FileWriter(filename)
    exporter.export(fw, graph)
  }

  def exportSootDirectedGraphToDot[SV](ugraph: soot.toolkits.graph.DirectedGraph[SV], filename: String): Unit = {
    exportJGraphToDot(sootDirectedGraphToJGraphT(ugraph), filename)
  }

  // SV = source vertex class
  // DV = dest vertex class
  def sootDirectedGraphToJGraphT[SV, DV](
    ugraph: soot.toolkits.graph.DirectedGraph[SV], 
    unitToNode: (SV) => DV): Graph[DV, DefaultEdge] = {
    val jgraph = new DefaultDirectedGraph[DV, DefaultEdge](classOf[DefaultEdge])

    val map = new java.util.HashMap[SV, DV]()

    for (u <- ugraph) {
      val n = unitToNode(u)
      println("Add Vertex: " + n)
      map.put(u, n)
      jgraph.addVertex(n)
    }

    for (u <- ugraph) {
      val src = map.get(u)
      for (s <- ugraph.getSuccsOf(u)) {
        val dst = map.get(s)
        jgraph.addEdge(src, dst)
      }
    }
    jgraph
  }

  // by default, convert source vertex type to String by calling toString
  def sootDirectedGraphToJGraphT[SV](ugraph: soot.toolkits.graph.DirectedGraph[SV]): Graph[String, DefaultEdge] = {
    sootDirectedGraphToJGraphT[SV,String](ugraph, (u) => "[" + u.getClass.getSimpleName + "] " + u.toString())
  }

  def dotEscapeString(s:String) : String = {
    s.replaceAllLiterally("\n","\\n")
  }
}