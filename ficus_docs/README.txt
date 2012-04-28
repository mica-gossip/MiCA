Ficus README
------------

Q. What is Ficus?
A. Ficus is sensor network aggregation project using MiCA. It is not yet ready for use.


Design
------
Ficus is a multi-layered MiCA app.  The bottom layer is an arbitrary connected overlay.  The overlay
may change over time, but Ficus is slow to respond to this kind of change --- it will not adjust until
older tree pipeline layers have expired.

On top of this sits a divergent Dolev pipeline, running multiple copies of a broadcast-tree building protocol.
The protocol works as follows:
   1. Originating from a root (in this case, the node that issues a query), pick a random non-parent node 
   and forward the query.  Do this until all non-parent neighbors belong to the tree, and then stop.
   When you receive a query, the immediate source (i.e., who told you about it) becomes your parent.
   
   2. The Dolev pipeline passes weights to each copy of the broadcast-tree-builder.  These weights encourage
   two copies of a tree to choose different children on the same node.  This makes it likely (but does not
   guarantee)  
   
   3. Fault tolerance
      a. When a parent becomes unresponsive, use a different tree to broadcast a non-directional kill order.
         This will eventually cause the entire tree to die.  Other trees will continue to function.  A higher
         layer of the protocol will eventually replace the dead tree.
         
      b. When a child becomes unresponsive, the parent issues a non-directional broadcast from another tree.
         "Any node whose parent was $DEAD, your parent is now $GRANDPARENT." 
        
      These broadcasts make the system untenable for high churn situations, because each failure generates a
      system-wide broadcast.  TODO: look into a more efficient group pub-sub system
   
   
   4. Maintenance.  Each tree can perform two local balancing operations. 
      a. Swap parent/child.  This is done when the child represents a majority of the parent's subtree size.
      b. Push child up/down.  Move a child up to the parent or down to a different child. This is used to balance
         the branching degree of the tree. TODO: How do we determine which branching degree we want?  For now: Aim 
         for a pre-determined maximum degree.
         
      These maintenance issues are driven by standard queries (i.e., subtree size) issued to a higher level
      Raw local tree data is provided as a local data source that any query can use.

   5. The divergent pipeline may prioritize trees, so that one particular tree is the "primary" and gossips 
      at a much higher rate than the others.  If the primary dies, then a new tree becomes the primary.
   
Query layer
   There are different kinds of queries, but they have the following characteristics:
   1. Each query has one or more entries in a local routing table.  This routing table may be the trees
      described above, or may be a different overlay depending on the type of query.
   2. Each query defines an aggregation function to be performed at each node before forwarding data.
   
   
   
     
     