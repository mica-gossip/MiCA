#!/usr/bin/env python

import pygtk
pygtk.require('2.0')
import gtk
import os, sys
import time
import cairo
import igraph
from gtk import gdk
from util import *
import logs
from math import *

class GraphWindow(object):
    # graph = igraph instance
    # node_name_map = map from address_str => vertex id
    def __init__(self, gui, graph, node_name_map):
        
        window = gtk.Window()
        self.gui = gui
        vbox = gtk.VBox(False, 0)
        self.igraph_drawing_area = IGraphDrawingArea(gui, graph, node_name_map)
        vbox.pack_start(self.igraph_drawing_area, True, True, 0)

        self.gui.adj = gtk.Adjustment(value = 0, 
                             lower = 0,
                             upper = len(self.gui.events)-1, 
                             step_incr = 1, 
                             page_incr = 1, 
                             page_size = 0)

        self.gui.adj.connect("value_changed", self.gui.event_slider_changed)
        self.slider = gtk.HScale(self.gui.adj)
        self.slider.set_digits(0)
        vbox.pack_start(self.slider, False, False, 0)
 
        window.add(vbox)
        window.connect("destroy", gtk.main_quit)
        self.window = window
 
    def refresh_graph(self):
        self.igraph_drawing_area.redraw_canvas()
        
    def set_graph(self, graph):
        self.window.set_graph(graph)


class MicaVisGui:

    def __init__(self, events):
        self.current = 0
        self.cursor_listeners = []   # functions that updated cursor ("current") values will be passed to, whenever it changes
        self.adj = None
        self.events = events
        self.create_event_window()
        self.create_graph_window()
        # Move cursor to start
        self.reset_tree_selection()

        if self.adj is None:
            print "Warning: gui.adj is None, graph window creation failed somehow"

        # current_node_state[addr] -> the latest state assigned to node "addr" w.r.t. the cursor self.get_current_i()
        self.current_node_state = logs.CurrentValueTracker(events, 
                                                           filter_func = lambda e: e['event_type'].startswith('state-'),
                                                           value_func = lambda e: (e['address'],e['data']) )
        self.add_cursor_listener(self.current_node_state.set_i)

        # current_node_state[addr] -> the latest state assigned to node "addr" w.r.t. the cursor self.get_current_i()
        self.current_node_view = logs.CurrentValueTracker(events,
                                                           filter_func = lambda e: e['event_type'] == 'view',
                                                           value_func = lambda e: (e['address'],e['data']))
        self.add_cursor_listener(self.current_node_view.set_i)

    def add_cursor_listener(self, f):
        # TODO currently no way to remove cursor listeners
        self.cursor_listeners.append(f)
        f(self.get_current_i())
                                                          
    # close the window and quit
    def delete_event(self, widget, event, data=None):
        gtk.main_quit()
        return False
    
    def event_slider_changed(self, adj):
        self.set_current_i(self.get_current_slider_selection())

    def get_current_slider_selection(self):
        return int(self.adj.value)

    # Set the current event
    def set_current_i(self, i):
        if self.current == i:
            return
        self.current = i
        self.update_selection()
        for l in self.cursor_listeners:
            l(i)

    def update_selection(self):
        self.reset_tree_selection()
        self.reset_slider_selection()
        self.graph_window.refresh_graph()

    def get_current_i(self):
        return self.current

    def get_current_event(self):
        return self.events[self.current]

    # Aperture is the range of events currently being drawn
    def get_aperture_events(self):
        start_event = max(0, self.current - 20)
        return self.events[start_event:self.current+1]
        
    def reset_slider_selection(self):
        i = self.get_current_i()
        j = self.get_current_slider_selection()
        if i != j:
            self.adj.set_value(i)

    def reset_tree_selection(self):
        i = self.get_current_i()
        j = self.get_current_tree_selection()
        if i != j:
            path = "%s"%i
            self.tree_selection.select_path(path)

    # Returns (igraph, namemap)
    # where igraph is an igraph object with nodes and edges of the comm graph
    # and namemap maps address_string => graph vertex id 
    def create_default_graph(self):
        g = igraph.Graph(directed=True)
        ual = logs.query_unique_addresses(self.events)
        self.unique_addresses = ual
        print "Logs report %s unique addresses" % len(ual)
        g.add_vertices(len(ual))
        namemap = dict((n,i) for i,n in enumerate(ual))
        comm_matrix = logs.build_comm_matrix(ual,self.events)
        edges = list(logs.matrix_edge_generator(comm_matrix))
        g.add_edges(edges)
        return g, namemap

    def create_graph_window(self):
        graph, namemap = self.create_default_graph()
        self.graph_window = GraphWindow(self, graph, namemap)
        self.graph_window.window.show_all()

    def create_event_window(self):
        # Create a new window
        self.event_window = gtk.Window(gtk.WINDOW_TOPLEVEL)
        self.event_window.set_title("Event Browser")
        self.event_window.set_size_request(500, 600)
        self.event_window.set_size_request(200, 200)
        self.event_window.connect("delete_event", self.delete_event)

        self.event_scrollwindow = gtk.ScrolledWindow()
        self.event_scrollwindow.set_border_width(10)
        self.event_scrollwindow.set_policy(gtk.POLICY_AUTOMATIC, gtk.POLICY_ALWAYS)
        self.event_vbox = gtk.VBox()
        self.event_vbox.pack_start(self.event_scrollwindow, True)

        # create a TreeStore with one string column to use as the model
        self.treestore = gtk.TreeStore(int, str, str, str)
        self.tree_model = self.treestore

        self.init_populate_treestore()

        # create the TreeView using treestore
        self.treeview = gtk.TreeView(self.treestore)
        self.tree_selection = self.treeview.get_selection()
        self.tree_selection.set_mode(gtk.SELECTION_SINGLE)
        self.tree_selection.connect("changed",self.event_tree_selection_changed)

        # create the TreeViewColumn to display the phdata
        self.timestamp_column = gtk.TreeViewColumn('Timestamp')
        self.address_column = gtk.TreeViewColumn('Address')
        self.event_type_column = gtk.TreeViewColumn('Type')
        self.data_column = gtk.TreeViewColumn('Data')        

        # add columns to treeview
        self.treeview.append_column(self.timestamp_column)
        self.treeview.append_column(self.address_column)        
        self.treeview.append_column(self.event_type_column)
        self.treeview.append_column(self.data_column)


        # create a CellRendererText to render the data
        self.cell = gtk.CellRendererText()

        # add the cell to the tvcolumn and allow it to expand
        self.timestamp_column.pack_start(self.cell, True)
        self.address_column.pack_start(self.cell, True)
        self.event_type_column.pack_start(self.cell, True)
        self.data_column.pack_start(self.cell, True)

        # set the cell "text" attribute to column 0 - retrieve text
        # from that column in treestore
        self.timestamp_column.add_attribute(self.cell, 'text', 0)
        self.address_column.add_attribute(self.cell, 'text', 1)
        self.event_type_column.add_attribute(self.cell, 'text', 2)
        self.data_column.add_attribute(self.cell, 'text', 3)

        # make it searchable
        self.treeview.set_search_column(0)

        # Allow sorting on the column
        self.timestamp_column.set_sort_column_id(0)
        self.address_column.set_sort_column_id(1)
        self.event_type_column.set_sort_column_id(2)
        self.data_column.set_sort_column_id(3)

        # Allow drag and drop reordering of rows
        self.treeview.set_reorderable(True)
        self.event_scrollwindow.add(self.treeview)
        self.event_window.add(self.event_vbox)
        self.event_window.show_all()


    def get_current_tree_selection(self):
        model, itr = self.tree_selection.get_selected()
        if itr:
            root_path = model.get_path(itr)[0]
        else:
            root_path = 0
        return root_path

    def event_tree_selection_changed(self, widget):
        self.set_current_i(self.get_current_tree_selection())
        
    def init_populate_treestore(self):
        # populate the table with events

        def fmt_tstamp(t):
            # STUPID HACK to work around 32-bit limitations in pygtk C gui
            if False and not isinstance(t,long):
                return t
            else:
                onetime_warning("Warning: Timestamps truncated because GUI elements restricted to 32-bit integers")
                t = t & 0x0FFFFFFF
                return int(t)

        
        # returns (truncated_string, truncated_status)
        #     true = truncated
        #     false = not truncated
        def trunc(s, limit=35):
            if len(s) > limit:
                return '...', True
            else:
                return s, False
        
        # recursively populate the tree data item
        def recpop(tstamp_original, parent, data):

            for key,data in sorted(data.items()):
                tstamp = tstamp_original
                address = ''

                if isinstance(data,dict):
                    if data.keys() == ['data']:
                        sdata = str(data.values()[0])
                    else:
                        sdata = str(data)
                    datas, recurse = trunc(sdata)
                    address = data.get('address',address)
                    if 'timestamp' in data:
                        tstamp = fmt_tstamp(data['timestamp'])
                else: 
                    recurse = False
                    datas = str(data)
                        

                p2 = self.treestore.append(parent, [tstamp, address, key, datas])
                
                if recurse:
                    recpop(tstamp, p2, data)
                    
        print "Reformat events for GUI in-memory database... (this can take a while)"
        for event in self.events:
            try:
                data = event['data']
            except KeyError:
                data = ''

            tstamp = fmt_tstamp(event['timestamp'])

            if isinstance(data,dict):

                if data.keys() == ['data']:
                    sdata = str(data.values()[0])
                else:
                    sdata = str(data)
                datas, recurse = trunc(sdata)
            else:
                recurse = False
                datas = str(data)

            parent = self.treestore.append(None, [
                    tstamp,
                    event['address'],
                    event['event_type'],
                    datas
                    ])

            if recurse:
                recpop(tstamp, parent, data)
            


class IGraphDrawingArea(gtk.DrawingArea):
    def __init__(self, gui, graph, node_name_map, plot_keywords=None):
        if graph is None:
            raise Exception

        # --- plot parameters
        self.config_draw_communication_graph = False
        self.config_draw_current_view_graph = True
        # ---

        self.gui = gui
        self.border = (15,15) # x,y blank pixel space on the display
        gtk.DrawingArea.__init__(self)
        self.set_size_request(600, 600)
        self.connect("expose_event", self.expose)
        self.plot_keywords = plot_keywords
        self.set_graph(graph, node_name_map)

    def expose(self, widget, event):
        context = widget.window.cairo_create() 
        # set a clip region for the expose event
        context.rectangle(event.area.x, event.area.y,
                          event.area.width, event.area.height)
        context.clip()
        self.draw(context)
        return False
 
    def draw(self, context):
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

        plot = igraph.drawing.Plot(cairo_surface, 
                                   (xborder, yborder, 
                                    rect.width - xborder*2, 
                                    rect.height-yborder*2))


        # Draw the graph
        assert(self.graph != None)

        if self.config_draw_communication_graph:
            plot.add(self.graph, layout = self.node_coordinates)

        if self.config_draw_current_view_graph:
            viewgraph = self.build_current_viewgraph()
            plot.add(viewgraph, layout = self.node_coordinates)


        plot.redraw()

        context.set_source_surface(cairo_surface)
        context.paint()
        
#        def scalefunc((x, y)):
#            return xborder + x * (rect.width - xborder*2), yborder + y * (rect.height - yborder*2),

        # Draw the selected event
        aperture_events = self.gui.get_aperture_events()
        for i,e in enumerate(aperture_events):
            if i < len(aperture_events) - 1:
                color = self.recent_node_color
            else:
                color = self.current_node_color

            self.draw_event(e, context, node_color = color)

 
        return False

    def build_current_viewgraph(self):
        # construct a graph of the current view, as derived from self.gui.current_node_view
        # TODO YOU ARE HERE
        g = igraph.Graph(directed=True)
        g.add_vertices(len(self.gui.unique_addresses))
        
        edges = []
        for addr in self.gui.unique_addresses:
            view = self.gui.current_node_view[addr]
            src_nid = self.node_name_map[addr]            
            if view:
                for neighbor_addr in view.keys():
                    dst_nid = self.node_name_map[neighbor_addr]
                    edges.append( (src_nid, dst_nid) )
        g.add_edges(edges)
        return g

    # ----------------- static drawing parameters
    current_node_color = (0.9, 0.9, 0.6) 
    recent_node_color = (0.7, 0.7, 0.4) 
    failure_node_color = (1., 0.3, 0.3) 
    node_outline_width = 4
    view_max_outline_width = 12
    select_edge_color = (1.0, 1.0, 0.5)
    view_edge_color = (0.4, 0.8, 1.0)

    # cr = cairo context
    def draw_event(self, event, cr, node_color = current_node_color):
        if event is None:
            return
        
        # Draw event node
        if 'address' in event:
            self.draw_node(cr, event['address'], 
                           fill_color=node_color)

        etype = event['event_type'].replace('-','_')
        fname = 'draw_event_%s' % etype
        # search for function draw_event_ETYPE(event, cairo_context)
        try:
            draw_func = getattr(self,fname)
        except AttributeError:
            print >> sys.stderr, "don't know how to draw event type %s" % etype
            return

        draw_func(event, cr)

    
    def draw_event_runtime_init(self, event, cr):
        # random_seed : long
        # round_ms : int
        # FIXME implement
        pass

    def draw_event_state(self, event, cr):
        # contents depend on protocol
        # FIXME implement
        pass

    def draw_event_state_initial(self, event, cr):
        self.draw_event_state(event,cr)

    def draw_event_state_gossip_initiator(self, event, cr):
        self.draw_event_state(event,cr)

    def draw_event_state_gossip_receiver(self, event, cr):
        self.draw_event_state(event,cr)

    def draw_event_state_pre_update(self, event, cr):
        self.draw_event_state(event,cr)

    def draw_event_state_post_update(self, event, cr):
        self.draw_event_state(event,cr)


    def draw_event_rate(self, event, cr):
        # data : float
        # FIXME implement
        pass

    def draw_event_merge_choose_subprotocol(self, event, cr):
        # data : BOTH / NEITHER / P1 / P2 / NA
        pass

    def draw_event_accept_lock_fail(self, event, cr):
        self.draw_event_failure(event,cr)

        
    def draw_event_accept_lock_succeed(self, event, cr):
        # data : {}
        # FIXME implement
        pass

    def draw_event_failure(self, event, cr):
        # data : None
        self.draw_node(cr, event['address'], fill_color=self.failure_node_color)

    def draw_event_failure_self_gossip_attempt(self, event, cr):
        self.draw_event_failure(event,cr)

    def draw_event_gossip_init_connection_failure(self, event, cr):
        self.draw_event_failure(event,cr)

    def draw_event_view(self, event, cr):
        if self.config_draw_current_view_graph:
            return # don't bother drawing if we're already drawing the current view graph

        view = event['data']
        if isinstance(view, dict):
            # view is a distribution, not null.  draw the distribution
            for neighbor_address, probability_mass in view.items():
                line_width = max(1.0, self.view_max_outline_width * probability_mass)
                cr.set_line_width(line_width)
                self.draw_arrow_between_nodes(cr, address, neighbor_address, color=self.view_edge_color)
        else:
            # draw null select
            self.draw_event_failure(event,cr)
            

    def draw_event_select(self, event, cr):
        # data : selected address
        address = event['address']
        select_event = event['data']  # a JSON-converted Logging.SelectEvent object
        selected = select_event['selected']
        if selected:
            self.draw_arrow_between_nodes(cr, address, selected, color=self.select_edge_color)
        else:
            # draw null select
            self.draw_event_failure(event,cr)

    # currently just draws a line between node centers
    def draw_arrow_between_nodes(self, cr, src_addr, dst_addr, color):
        src_nid = self.node_name_map[src_addr]
        dst_nid = self.node_name_map[dst_addr]
        src = self.node_center_coordinates(src_nid)
        dst = self.node_center_coordinates(dst_nid)
        cr.move_to(*src)
        cr.line_to(*dst)
        cr.set_source_rgb(*color)
        cr.stroke()

    def draw_node(self, cr, address, fill_color=(1.,1.,1.), 
                  outline_color=(0.,0.,0.)):
            nid = self.node_name_map[address]
            x,y = self.node_center_coordinates(nid) 
            cr.set_line_width(self.node_outline_width)
            cr.set_source_rgb(*outline_color)
            cr.arc(x, y, self.node_radius, 0, 2 * pi)
            cr.stroke_preserve()
            cr.set_source_rgb(*fill_color)
            cr.fill()
            

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


    def redraw_canvas(self):
        if self.window:
            alloc = self.get_allocation()
            rect = gdk.Rectangle(alloc.x, alloc.y, alloc.width, alloc.height)
            self.window.invalidate_rect(rect, True)
            self.window.process_updates(True)
 
    def set_graph(self, graph, node_name_map):
        self.graph = graph
        self.node_name_map = node_name_map
        self.node_coordinates = graph.layout('fr')


def main(args=sys.argv):
    try:
        logloc = args[1]
    except:
        print >> sys.stderr, "Usage: %s <mica_log_dir_or_file>" % args
        sys.exit(1)

    import logs
    events = logs.read_mica_logs(logloc)
    print "Read %s log events" % len(events)
    gui = MicaVisGui(events)
    gtk.main()
    






