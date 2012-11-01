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
import custom

from event_tree_model import *

class GraphWindow(object):
    # graph = igraph instance
    # node_name_map = map from address_str => vertex id
    def __init__(self, gui, graph, node_name_map):
        
        window = gtk.Window()
        self.gui = gui
        vbox = gtk.VBox(False, 0)
        self.igraph_drawing_area = IGraphDrawingArea(gui, graph, node_name_map)

        menubar = self.create_display_menu()
        vbox.pack_start(menubar, False)

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

    def create_display_menu(self):
        vis = self.igraph_drawing_area

        bar = gtk.MenuBar()

        layers = gtk.MenuItem("Layers")
        layers.show()
        bar.append(layers)

        menu = gtk.Menu()
        layers.set_submenu(menu)

        #    How to create a normal menu item:
        #def f(*args):
        #    print "ACTIVATE", args
        #item = gtk.MenuItem("menu item test")
        # can pass additional arguments after the callback 
        #item.connect("activate",f)
        #menu.append(item)
        #item.show()

        for layer in vis.display_layers:
            checkitem = gtk.CheckMenuItem(layer.name)
            checkitem.set_active(layer.active)
            checkitem.connect("activate", layer.toggle)
            menu.append(checkitem)
            checkitem.show()

        menu.show()
        return bar


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

        self.current_node_state = logs.CurrentValueTracker(
            events, 
            filter_func = lambda e: e['event_type'].startswith('mica-state-') and 'state' in e['data'],
            value_func = lambda e: (e['address'],e['data']) )

        self.add_cursor_listener(self.current_node_state.set_i)

        # current_node_state[addr] -> the latest state assigned to node "addr" w.r.t. the cursor self.get_current_i()
        self.current_node_view = logs.CurrentValueTracker(
            events,
            filter_func = lambda e: e['event_type'].startswith('mica-state-') and 'view' in e['data'],
            value_func = lambda e: (e['address'],e['data']['view']))

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

        self.dump_event(self.events[i])

    # print event info to console when an event is selected
    def dump_event(self, e):
        if 'address' not in e:
            return
        addr = e['address']
        state = self.current_node_state[addr]
        
        q = [ (0, ('root',state))]
        while q:
            i, (n,s) = q.pop()
            if s is None:
                s = {}
            #print "%s%s: %s" % ('  '*i,n,s.get('stateType','(None)'))
            q += [(i+1,ls) for ls in logs.subprotocols(s)]
        
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
        start_event = max(0, self.current - 100)
        start_event = self.current
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

    def create_layers_window(self):
        self.layers_window = create_layers_window()

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
        self.treemodel = self.initialize_treemodel()

        # create the TreeView using treestore
        self.treeview = gtk.TreeView(self.treemodel)
        self.tree_selection = self.treeview.get_selection()
        self.tree_selection.set_mode(gtk.SELECTION_SINGLE)
        self.tree_selection.connect("changed",self.event_tree_selection_changed)

        # set up treeview popup menu
        treeview_popup = gtk.Menu()
        copy_item = gtk.MenuItem(label="Copy Event")
        copy_item.connect("activate",self.treeview_menu_copy_data)
        treeview_popup.append(copy_item)
        copy_item.show()
        self.treeview.connect_object("button_press_event", 
                                     self.tree_button_press,
                                     treeview_popup)


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

    def treeview_menu_copy_data(self, widget):
        event = self.get_current_event()
        s = str(event)
        cb = gtk.clipboard_get()
        cb.set_text(s)
        cb.store()

    def tree_button_press(self, widget, event):
        if event.button != 3:
            return
        widget.popup(None, None, None, event.button, event.time)
        
        
    def get_current_tree_selection(self):
        model, itr = self.tree_selection.get_selected()
        if itr:
            root_path = model.get_path(itr)[0]
        else:
            root_path = 0
        return root_path

    def event_tree_selection_changed(self, widget):
        self.set_current_i(self.get_current_tree_selection())
        
    def initialize_treemodel_static(self):
        # populate the table with events
        ts = gtk.TreeStore(int, str, str, str)

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
                        tstamp = gtk_fmt_tstamp(data['timestamp'])
                else: 
                    recurse = False
                    datas = str(data)
                        

                p2 = ts.append(parent, [tstamp, address, key, datas])
                
                if recurse:
                    recpop(tstamp, p2, data)
                    
        print "Reformat events for GUI in-memory database... (this can take a while)"
        for event in self.events:
            try:
                data = event['data']
            except KeyError:
                data = ''

            tstamp = gtk_fmt_tstamp(event['timestamp'])

            if isinstance(data,dict):

                if data.keys() == ['data']:
                    sdata = str(data.values()[0])
                else:
                    sdata = str(data)
                datas, recurse = trunc(sdata)
            else:
                recurse = False
                datas = str(data)

            parent = ts.append(None, [
                    tstamp,
                    event['address'],
                    event['event_type'],
                    datas
                    ])

            if recurse:
                recpop(tstamp, parent, data)
            
        return ts

    def initialize_treemodel_dynamic(self):
        return EventTreeModel(self.events)


    initialize_treemodel = initialize_treemodel_dynamic



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
        g.add_vertices(len(self.vis.gui.unique_addresses))
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
        gui = vis.gui
       
        aperture_events = gui.get_aperture_events()
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
        gui = vis.gui      
        current = gui.get_current_event()
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
        vis = self.vis
        viewgraph = self.build_current_viewgraph()
        plot = vis.create_plot_func()
        plot.add(viewgraph, layout = vis.layout, **self.display_options)
        plot.redraw()

    def build_current_viewgraph(self):
        # construct a graph of the current view, 
        # as derived from self.gui.current_node_view
        vis = self.vis
        gui = vis.gui

        g = igraph.Graph(directed=True)
        g.add_vertices(len(gui.unique_addresses))
        
        edges = []
        for addr in gui.unique_addresses:
            view = gui.current_node_view[addr]
            src_nid = vis.node_name_map[addr]            
            if view:
                for neighbor_addr in view.keys():
                    dst_nid = vis.node_name_map[neighbor_addr]
                    edges.append( (src_nid, dst_nid) )
        g.add_edges(edges)
        return g

class CurrentStateLayer(DisplayLayer):
    def __init__(self, vis, active):
        DisplayLayer.__init__(self, vis, "Current States", active)

    def draw(self):
        vis = self.vis
        gui = vis.gui
        
        for addr in gui.unique_addresses:
            state_thunk = lambda: gui.current_node_state[addr]
            vis.draw_node_state(addr,state_thunk)


class IGraphDrawingArea(gtk.DrawingArea):

    current_node_color = "#ffffff"
    recent_node_color = "#dddddd"
    failure_node_color = "#ff0000"
    select_color = "#ffffc0"

    def __init__(self, gui, graph, node_name_map, plot_keywords=None):
        if graph is None:
            raise Exception

        self.gui = gui
        self.border = (15,15) # x,y blank pixel space on the display
        gtk.DrawingArea.__init__(self)
        self.set_size_request(600, 600)
        self.connect("expose_event", self.expose)
        self.plot_keywords = plot_keywords
        self.set_graph(graph, node_name_map)

        # self is passed as the vis argument
        self.display_layers = [
            NodesLayer(self,True),
            CommunicationGraphLayer(self, True),
            CurrentViewLayer(self, True),
            CurrentStateLayer(self, False),
            CurrentEventApertureLayer(self, False),
            CurrentEventLayer(self, True)
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
        # construct a graph of the current view, as derived from self.gui.current_node_view
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

    # state_thunk is called with no args to produce state
    # this is because we don't want to call this function if
    # we're not really going to use the state...
    def draw_node_state(vis, address, state_thunk):
        # state keys:
        #   view or view-unchanged:True
        #   stateType: class name
        #   state or state-unchanged  
        # 
        #    note: the node state drawing layer will never have 'state-unchanged'
        state = state_thunk() # ... although the current implementation does             
        if not state:
            return

        module = custom.load(state['stateType'])
        if not module:
            return
        
        module.draw_state(vis, address, state['state'])
        
    # cr = cairo context
    def draw_event(vis, event):

        if event is None:
            return
        
        # Draw event node
        if 'address' in event:
            vis.draw_node(
                       event['address'], 
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
        address = event['address']
        select_event = event['data']  # a JSON-converted Logging.SelectEvent object
        selected = select_event['selected']
        if selected:
            vis.draw_edge(address, selected, 
                          vertex_color=vis.select_color,
                          vertex_size=0,
                          edge_color=vis.select_color,
                          edge_width=3)
        else:
            # draw null select
            vis.draw_event_failure(event)

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
    






