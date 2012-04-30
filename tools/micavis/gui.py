#!/usr/bin/env python

# example basictreeview.py

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

class GraphWindow():
    def __init__(self, gui, graph=None):
        window = gtk.Window()
        self.gui = gui
        vbox = gtk.VBox(False, 0)
        self.igraph_drawing_area = IGraphDrawingArea(gui, graph)
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
        print "redraw"
        self.igraph_drawing_area.redraw_canvas()
        
    def set_graph(self, graph):
        self.window.set_graph(graph)


class MicaVisGui:

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
        
    def update_selection(self):
        self.reset_tree_selection()
        self.reset_slider_selection()
        self.graph_window.refresh_graph()

    def get_current_i(self):
        return self.current

    def get_current_event(self):
        return self.events[self.current]

    def __init__(self, events):
        self.current = 0
        self.events = events
        self.create_event_window()
        self.create_graph_window()

        # Move cursor to start
        self.reset_tree_selection()

    def reset_slider_selection(self):
        i = self.get_current_i()
        j = self.get_current_slider_selection()
        if i != j:
            self.adj.set_value(i)
#            print "FIX SLIDER %s" % i

    def reset_tree_selection(self):
        i = self.get_current_i()
        j = self.get_current_tree_selection()
        if i != j:
            path = "%s"%i
            self.tree_selection.select_path(path)

        
    def create_graph_window(self):
        temp = self.create_default_graph()
        self.graph_window = GraphWindow(self, temp)
        self.graph_window.window.show_all()

    def create_default_graph(self):
        g = igraph.Graph(directed=True)
        ual = logs.query_unique_addresses(self.events)
        print "%s unique addresses" % len(ual)
        g.add_vertices(len(ual)-1)
        comm_matrix = logs.build_comm_matrix(ual,self.events)
        edges = list(logs.matrix_edge_generator(comm_matrix))
        g.add_edges(edges)
        return g

    def create_graph_window(self):
        self.graph_window = gtk.Window(gtk.WINDOW_TOPLEVEL)
        self.event_window.set_title("Communication Graph")
        self.event_window.set_size_request(500, 500)
        self.event_window.connect("delete_event", self.delete_event)

        self.graph_scrollwindow = gtk.ScrolledWindow()
        self.graph_scrollwindow.set_border_width(10)
        self.graph_scrollwindow.set_policy(gtk.POLICY_AUTOMATIC, gtk.POLICY_ALWAYS)

        self.drawing_area = gtk.DrawingArea()
        self.graph_scrollwindow.add(self.drawing_area)

        self.graph_vbox = gtk.VBox()
        self.graph_vbox.pack_start(self.graph_scrollwindow, expand=True, fill=True, padding=0)

        self.graph_nav_layout = gtk.Layout(hadjustment=None, vadjustment=None)

        adj = gtk.Adjustment(0,0,100,step_incr=1,page_incr=0,page_size=0)
        self.event_scale = gtk.HScale(adj)
        self.event_scale.set_size_request(200,30)
        self.graph_nav_layout.put(self.event_scale, 10,10)

        self.graph_vbox.pack_end(self.graph_nav_layout, expand=True, fill=True, padding=10)

        self.graph_window.add(self.graph_vbox)

        # set drawing area background color
        dw = self.drawing_area.window
#        gc = dw.new_gc(
#            background = "black"
#            )
        
        # YOU ARE HERE
        
        self.graph_window.show_all()

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

        
        def trunc(s, limit=50):
            return '...'
#            if len(s) > limit:
#                s = s[:limit] + '...'
#            return s

        def recpop(tstamp_original, parent, data):

            for key,data in sorted(data.items()):
                tstamp = tstamp_original
                address = ''

                if isinstance(data,dict):
                    recurse = True
                    datas = trunc(str(data))
                    address = data.get('address',address)
                    if 'timestamp' in data:
                        tstamp = fmt_tstamp(data['timestamp'])
                else: 
                    recurse = False
                    datas = str(data)
                        

                p2 = self.treestore.append(parent, [tstamp, address, key, datas])
                
                if recurse:
                    recpop(tstamp, p2, data)

                                      

        for event in self.events:
            data = event['data']
            tstamp = fmt_tstamp(event['timestamp'])

            if isinstance(data,dict):
                recurse = True
                datas = trunc(str(data))
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
    def __init__(self, gui, graph, plot_keywords=None):
        self.gui = gui
        gtk.DrawingArea.__init__(self)
        self.set_size_request(600, 600)
        self.connect("expose_event", self.expose)
        self.graph = graph # an igraph.Graph instance
        self.plot_keywords = plot_keywords

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
 
        surface = cairo.ImageSurface (cairo.FORMAT_ARGB32,
                        rect.width,
                        rect.height)
 
        g = self.graph
        
        plot = igraph.drawing.Plot(surface, (0, 0, rect.width, rect.height))
        if g is not  None:
            plot.add(g, **self.get_plot_keywords())
        plot.redraw()
        
        self.draw_event(self.gui.get_current_event(), context)

        context.set_source_surface (surface)
        context.paint()
 
        return False

    def draw_event(self, event, context):
        if event is None:
            return
        print "fixme: draw event %s" % repr(event)
    
    def get_plot_keywords(self):
        if self.graph == None:
            self.plot_keywords = None
        elif self.plot_keywords == None:
            self.plot_keywords = {
                'layout': self.graph.layout('fr')
                }
        return self.plot_keywords

    def set_plot_keywords(self, **keywords):
        self.plot_keywords = keywords

    def redraw_canvas(self):
        if self.window:
            alloc = self.get_allocation()
            rect = gdk.Rectangle(alloc.x, alloc.y, alloc.width, alloc.height)
            self.window.invalidate_rect(rect, True)
            self.window.process_updates(True)
 
    def set_graph(self, graph):
        self.graph = graph
        self.redraw_canvas()


def main(args=sys.argv):
    try:
        logdir = args[1]
    except:
        print >> sys.stderr, "Usage: %s <mica_log_dir>" % args
        sys.exit(1)

    import logs
    events = logs.read_mica_logs(logdir)
    print "Read %s log events" % len(events)
    gui = MicaVisGui(events)
    gtk.main()
    


