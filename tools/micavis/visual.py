import pygtk
pygtk.require('2.0')
import gtk
import os, sys
import time
import cairo
import igraph
from gtk import gdk
from util import *
import custom

class DisplayLayer:
    def __init__(self, vis, name, active):
        # vis = an IGraphDrawingArea instance
        self.name = name
        self.active = active
        self.vis = vis

    def toggle(self, checkmenuitem):
        self.active = not self.active
        self.vis.redraw_canvas()

    def draw(self):
        pass

class NodesLayer(DisplayLayer):
    
    display_options = dict(
        vertex_color = "#cccccc",
        vertex_size = 10,
        vertex_label_dist = 3
        )

    def __init__(self, vis, active):
        DisplayLayer.__init__(self, vis, "Node Labels", active)
        self.blank_graph = self.create_blank_graph()

    def create_blank_graph(self):
        g = igraph.Graph(directed=True)
        g.add_vertices(len(self.vis.micavis.unique_addresses))
        return g

    def draw(self):
        vis = self.vis
        plot = vis.create_plot_func()
        temp = vis.node_name_map.items()
        temp.sort(key=lambda (a,b): b)
        names = [n[-4:] for n,nid in temp]
        plot.add(self.blank_graph, opacity=0.5, layout = vis.layout, vertex_label=names,**self.display_options)
        plot.redraw()

class CurrentEventApertureLayer(DisplayLayer):
    def __init__(self, vis, active):
        DisplayLayer.__init__(self, vis, "Current Event Aperture", active)

    def draw(self):
        vis = self.vis
        micavis = vis.micavis
       
        aperture_events = micavis.get_aperture_events()
        for i,e in enumerate(aperture_events):
            if i < len(aperture_events) - 1:
                color = vis.recent_node_color
            else:
                color = vis.current_node_color
            vis.draw_event(e)

class CurrentEventLayer(DisplayLayer):
    def __init__(self, vis, active):
        DisplayLayer.__init__(self, vis, "Current Event", active)

    def draw(self):
        vis = self.vis
        micavis = vis.micavis      
        current = micavis.get_current_event()
        color = vis.current_node_color
        vis.draw_event(current)
        
class CommunicationGraphLayer(DisplayLayer):
    display_options = dict(
        vertex_color = "#ccccfc",
        vertex_size = 10,
        edge_color = "#ccccfc"
        )

    def __init__(self, vis, active):
        DisplayLayer.__init__(self, vis, "Communication Graph", active)

    def draw(self):
        vis = self.vis
        plot = vis.create_plot_func()
        plot.add(vis.graph, opacity=0.5, layout = vis.layout, **self.display_options)
        plot.redraw()

class CurrentViewLayer(DisplayLayer):
    display_options = dict(
        vertex_color = "#ccccfc",
        vertex_size = 10,
        edge_color = "#acfcac",
        edge_width = 2
        )

    def __init__(self, vis, active):
        DisplayLayer.__init__(self, vis, "Current Views", active)

    def draw(self):
        viewgraph_edges = self.build_current_viewgraph()
        self.vis.draw_subgraph(viewgraph_edges, **self.display_options)

    def build_current_viewgraph(self):
        edges = []
        micavis = self.vis.micavis
        for addr in micavis.unique_addresses:
            view = micavis.current_node_view[addr]
            if view:
                for neighbor_addr in view.keys():
                    edges.append( (addr, neighbor_addr) )
        return edges

        # construct a graph of the current view, 
        # as derived from self.micavis.current_node_view
#        vis = self.vis
#        micavis = vis.micavis
#
#        g = igraph.Graph(directed=True)
#        g.add_vertices(len(micavis.unique_addresses))
#        
#        edges = []
#        for addr in micavis.unique_addresses:
#            view = micavis.current_node_view[addr]
#
#            src_nid = vis.node_name_map[addr]            
#            if view:
#                for neighbor_addr in view.keys():
#                    dst_nid = vis.node_name_map[neighbor_addr]
#                    edges.append( (src_nid, dst_nid) )
#        g.add_edges(edges)
#        return g

class GossipExchangeLayer(DisplayLayer):
    display_options = dict(
        vertex_color = "#0000ff",
        vertex_size = 10,
        edge_color = "#a000cf",
        edge_width = 2
        )

    def __init__(self, vis, active):
        DisplayLayer.__init__(self, vis, "Gossip Exchanges", active)

    def draw(self):
        vis = self.vis
        graph = self.build_current_exgraph()
        plot = vis.create_plot_func()
        plot.add(graph, layout = vis.layout, **self.display_options)
        plot.redraw()

    def build_current_exgraph(self):
        # construct a graph of the current view, 
        # as derived from self.micavis.current_node_view
        vis = self.vis
        micavis = vis.micavis

        g = igraph.Graph(directed=True)
        g.add_vertices(len(micavis.unique_addresses))
        
        edges = []
        for send,recv,stage in micavis.exchange_tracker.get_exchanges(micavis):
            src_nid = vis.node_name_map[send]            
            dst_nid = vis.node_name_map[recv]
            edges.append( (src_nid, dst_nid) )
        g.add_edges(edges)
        return g


class CurrentStateLayer(DisplayLayer):
    def __init__(self, vis, active):
        DisplayLayer.__init__(self, vis, "Current States", active)

    def draw(self):
        vis = self.vis
        micavis = vis.micavis

        # map module -> [ (addr1,state1), (addr2,state2), ... ]
        modules = {}
        for addr in micavis.unique_addresses:
            pdata = micavis.current_node_state[addr]
            if not pdata:
                continue
            module = custom.get_module(vis.micavis, pdata)
            if module not in modules:
                modules[module] = []
            modules[module] += [(addr,pdata)]

        for module, nodes in sorted(modules.items()):
            module.draw_nodes(vis, nodes)
            
class IGraphDrawingArea(gtk.DrawingArea):

    current_node_color = "#ffffff"
    recent_node_color = "#dddddd"
    failure_node_color = "#ff0000"
    select_color = "#ffffc0"

    def __init__(self, micavis, graph, node_name_map, plot_keywords=None):
        self.micavis = micavis
        self.border = (15,15) # x,y blank pixel space on the display
        gtk.DrawingArea.__init__(self)
        self.set_size_request(600, 600)
        self.connect("expose_event", self.expose)
        self.plot_keywords = plot_keywords
        self.set_graph(graph, node_name_map)
        
        # if true, disable all visualization
        enable = True
        disable = False

        # self is passed as the vis argument
        if self.micavis.options['novis']:
            self.display_layers = []
        else:
            self.display_layers = [
                NodesLayer(self,enable),
                CommunicationGraphLayer(self, disable),
                CurrentViewLayer(self, enable),
                GossipExchangeLayer(self, disable),
                CurrentStateLayer(self, enable),
                CurrentEventApertureLayer(self, disable),
                CurrentEventLayer(self, disable)
            ]

    def expose(self, widget, event):
        context = widget.window.cairo_create() 
        # set a clip region for the expose event
        context.rectangle(event.area.x, event.area.y,
                          event.area.width, event.area.height)
        context.clip()
        self.draw(context)
        return False
 
    def draw(self, context):
        if self.graph is None:
            return
    
        rect = self.get_allocation()
 
        cairo_surface = cairo.ImageSurface(cairo.FORMAT_ARGB32,
                        rect.width,
                        rect.height)
 
        xborder, yborder = self.border

        
        # Set dimensional info and other drawing parameters
        self.drawing_bbox = (xborder,
                             yborder,
                             rect.width-xborder*2,
                             rect.height-yborder*2)

        self.node_coordinates.fit_into(self.drawing_bbox, 
                                       keep_aspect_ratio = False)
        self.node_radius = rect.width / 60.


        def create_plot():
            return igraph.drawing.Plot(cairo_surface, 
                                       (xborder, yborder, 
                                        rect.width - xborder*2, 
                                        rect.height-yborder*2))

        # Draw the graph
        assert(self.graph != None)

        # Drawing context elements --- for use by drawing layers
        self.layout = self.node_coordinates  # current graph layout; igraph.layout.Layout instance
        self.context = context  # ?? used for graph plotting
        self.cairo_surface = cairo_surface # cairo surface for plotting
        self.create_plot_func = create_plot  # function to create an igraph plot --- has baked in scaling constants

        for layer in self.display_layers:
            if layer.active:
                layer.draw()

        context.set_source_surface(cairo_surface)
        context.paint()
        return False

    def build_current_viewgraph(self):
        # construct a graph of the current view, as derived from self.micavis.current_node_view
        g = igraph.Graph(directed=True)
        g.add_vertices(len(self.micavis.unique_addresses))
        
        edges = []
        for addr in self.micavis.unique_addresses:
            view = self.micavis.current_node_view[addr]
            src_nid = self.node_name_map[addr]            
            if view:
                for neighbor_addr in view.keys():
                    dst_nid = self.node_name_map[neighbor_addr]
                    edges.append( (src_nid, dst_nid) )
        g.add_edges(edges)
        return g


    # cr = cairo context
    def draw_event(vis, event):

        if event is None:
            return
        
        # Draw event node
        if 'address' in event:
            vis.draw_node(
                       event['address'], 
                       vertex_size = 10,
                       vertex_color=vis.current_node_color)

        etype = event['event_type'].replace('-','_')

        if etype.startswith('mica_state'):
            etype = 'mica_state'

        fname = 'draw_event_%s' % etype
        # search for function draw_event_ETYPE(event, cairo_context)

        
        try:
            draw_func = getattr(vis,fname)
        except AttributeError:
            onetime_warning("warning: don't know how to draw event type %s" % etype)
            return

        draw_func(event)
    
    def draw_event_mica_runtime_init(vis, event):
        # random_seed : long
        # round_ms : int
        # FIXME implement
        pass

    def draw_event_mica_state(vis, event):
        # contents depend on protocol
        # FIXME implement
        pass

    def draw_event_mica_rate(vis, event):
        # data : float
        # FIXME implement
        pass

    def draw_event_mica_error_lock_fail(vis, event):
        vis.draw_event_mica_error(event)

    def draw_event_mica_error_handler(vis, event):
        vis.draw_event_mica_error(event)

    def draw_event_mica_error_internal(vis, event):
        vis.draw_event_mica_error(event)

    def draw_event_mica_error(vis, event):
        # data : None
        vis.draw_node(event['address'], 
                       vertex_color=vis.failure_node_color)

    def draw_event_mica_select(vis, event):
        # data : selected address
        pass

    def draw_edge(vis, src_addr, dst_addr, **plot_options):
        src_nid = vis.node_name_map[src_addr]
        dst_nid = vis.node_name_map[dst_addr]
        g = igraph.Graph(directed=True)
        layout = vis.layout.__copy__()
        g.add_vertices(2)
        layout[0] = vis.node_coordinates[src_nid]
        layout[1] = vis.node_coordinates[dst_nid]
        g.add_edges([(0,1)])
        plot = vis.create_plot_func()
        plot.add(g,layout=layout, **plot_options)
        plot.redraw()

    def draw_node(vis, address, **graph_plot_options):
        g = igraph.Graph(directed=True)
        nid = vis.node_name_map[address]
        layout = vis.layout.__copy__()
        layout[0] = vis.layout[nid]
        g.add_vertex(1)
        plot = vis.create_plot_func()
        plot.add(g,layout=layout, **graph_plot_options)
        plot.redraw()

    # currently just draws a line between node centers
#    def draw_arrow_between_nodes_old(self, cr, cairo_surface, create_plot_func, src_addr, dst_addr, color):
#        src_nid = self.node_name_map[src_addr]
#        dst_nid = self.node_name_map[dst_addr]
#        src = self.node_center_coordinates(src_nid)
#        dst = self.node_center_coordinates(dst_nid)
#        cr.move_to(*src)
#        cr.line_to(*dst)
#        cr.set_source_rgb(*color)
#        cr.stroke()

    # use cairo primitives to draw a node
#    def deprecated_draw_node(self, cr, cairo_surface, 
#                             create_plot_func, address, 
#                             fill_color=(1.,1.,1.), 
#                             outline_color=(0.,0.,0.)):
#            nid = self.node_name_map[address]
#            x,y = self.node_center_coordinates(nid) 
#            cr.set_line_width(self.node_outline_width)
#            cr.set_source_rgb(*outline_color)
#            cr.arc(x, y, self.node_radius, 0, 2 * pi)
#            cr.stroke_preserve()
#            cr.set_source_rgb(*fill_color)
#            cr.fill()

# view no longer a separate event
#    def draw_event_view(self, event, cr, cairo_surface, create_plot_func):
#        if self.config_draw_current_view_graph:
#            return # don't bother drawing if we're already drawing the current view graph
#        view = event['data']
#        if isinstance(view, dict):
#            # view is a distribution, not null.  draw the distribution
#            for neighbor_address, probability_mass in view.items():
#                line_width = max(1.0, self.view_max_outline_width * probability_mass)
#                cr.set_line_width(line_width)
#                self.draw_arrow_between_nodes(cr, cairo_surface, create_plot_func, address, neighbor_address, color=self.view_edge_color)
#        else:
            # draw null select
#            self.draw_event_failure(event,cr, cairo_surface, create_plot_func)

    def node_center_coordinates(self, nid):
        x,y = self.node_coordinates[nid]
        return x,y 

    def get_plot_keywords(self):
        if self.graph == None:
            self.plot_keywords = None
        elif self.plot_keywords == None:
            self.plot_keywords = {
                'layout': self.node_coordinates
                }
        return self.plot_keywords

    def draw_subgraph(self, edges, **plot_options):
        # edges are (src_addr, dst_addr)
        graph, layout = self.create_igraph_subgraph(edges)
        plot = self.create_plot_func()
        plot.add(graph, layout = layout, **plot_options)
        plot.redraw()

    # returns (igraph, layout)
    def create_igraph_subgraph(self, edges):
        # edges: (src_addr, dst_addr) pairs
        g = igraph.Graph(directed=True)
#        vertices = set()
#        for s,d in edges:
#            vertices.add(s)
#            vertices.add(d)
#        vertices = list(vertices)
        vertices = self.micavis.unique_addresses
        vmap = dict( (addr, i) 
                     for i,addr in enumerate(vertices) ) 
        g.add_vertices(len(vertices))
        edges_g = [ (vmap[s],vmap[d]) for s,d in edges ]
        g.add_edges(edges_g)
        layout = self.layout.__copy__()
        for vaddr in vertices:
            nid = self.node_name_map[vaddr]             
            layout[vmap[vaddr]] = self.layout[nid]
        return g, layout

    def redraw_canvas(self):
        if self.window:
            alloc = self.get_allocation()
            rect = gdk.Rectangle(alloc.x, alloc.y, alloc.width, alloc.height)
            self.window.invalidate_rect(rect, True)
            self.window.process_updates(True)
 
    def set_graph(self, graph, node_name_map):
        self.graph = graph
        self.node_name_map = node_name_map
        if graph:
            self.node_coordinates = graph.layout('fr')

