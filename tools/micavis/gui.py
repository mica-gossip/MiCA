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
from visual import *
import matplotlib.pyplot as plt
import numpy as np
import micavis.ipython as ipython

import micavis.analysis as analysis

from event_tree_model import *

from trace import *

class GraphWindow(object):
    # graph = igraph instance
    # node_name_map = map from address_str => vertex id
    def __init__(self, micavis, graph, node_name_map):
        
        window = gtk.Window()
        self.micavis = micavis
        vbox = gtk.VBox(False, 0)
        self.igraph_drawing_area = IGraphDrawingArea(micavis, graph, node_name_map)

        menubar = self.create_display_menus()
        vbox.pack_start(menubar, False)

        vbox.pack_start(self.igraph_drawing_area, True, True, 0)

        self.micavis.adj = gtk.Adjustment(value = 0, 
                             lower = 0,
                             upper = len(self.micavis.events)-1, 
                             step_incr = 1,
                             page_incr = 1,
                             page_size = 0)

        self.micavis.adj.connect("value_changed", self.micavis.event_slider_changed)
        self.slider = gtk.HScale(self.micavis.adj)
        self.slider.set_digits(0)
        vbox.pack_start(self.slider, False, False, 0)
 
        window.add(vbox)
        window.connect("destroy", gtk.main_quit)
        self.window = window
 
    def refresh_graph(self):
        self.igraph_drawing_area.redraw_canvas()
        
    def set_graph(self, graph):
        self.window.set_graph(graph)

    def create_menu_item(self, menu, label, callback):
        item = gtk.MenuItem(label)
        item.connect("activate",callback)
        menu.append(item)
        item.show()

    def create_display_menus(self):
        
        # utility function

        vis = self.igraph_drawing_area

        bar = gtk.MenuBar()

        layers = gtk.MenuItem("Layers")
        layers.show()
        bar.append(layers)
        menu = gtk.Menu()
        layers.set_submenu(menu)
        for layer in vis.display_layers:
            checkitem = gtk.CheckMenuItem(layer.name)
            checkitem.set_active(layer.active)
            checkitem.connect("activate", layer.toggle)
            menu.append(checkitem)
            checkitem.show()
        menu.show()

        projection = gtk.MenuItem("Projection")
        projection.show()
        bar.append(projection)
        menu = gtk.Menu()
        projection.set_submenu(menu)
        self.projection_checkitems = {}

        for p,shortname in self.projection_hierarchy(): 
            checkitem = gtk.CheckMenuItem(shortname)
            if p == 'root':
                active = True
            else:
                active = False
            checkitem.set_active(active)
            checkitem.connect("activate", self.micavis.set_projection_gui, p)
            self.projection_checkitems[p] = checkitem
            menu.append(checkitem)
            checkitem.show()
        menu.show()
        
        analysis = gtk.MenuItem("Analysis")
        menu = gtk.Menu()
        menu.show()
        analysis_menu = menu
        self.analysis_menu = analysis_menu
        analysis.set_submenu(menu)
        analysis.show()
        bar.append(analysis)

        def ignore_widget(f):
            return lambda widget, *args: f(*args)

        def append_analysis_menuitem(label, callback):
            self.create_menu_item(analysis_menu, label, callback)

        append_analysis_menuitem(
            "State change graph", ignore_widget(self.micavis.plot_state_change_graph))

        append_analysis_menuitem(
            "State change graph (leaves)", ignore_widget(self.micavis.plot_state_change_graph_leaves))

        append_analysis_menuitem(
            "Gossip rate (leaves)", ignore_widget(self.micavis.plot_gossip_rate))

        append_analysis_menuitem(
            "Gossips per node" , ignore_widget(self.micavis.plot_gossip_per_node))

        for ne_suffix in self.micavis.notable_event_labels():
            append_analysis_menuitem(
                "Notable events: %s" % ne_suffix, 
                lambda widget,sfx=ne_suffix: self.micavis.plot_notable_events(sfx))

        for ne_suffix in self.micavis.notable_event_labels():
            append_analysis_menuitem(
                "Notable event timing histogram: %s" % ne_suffix, 
                lambda widget,sfx=ne_suffix: self.micavis.plot_notable_events_histogram(sfx))

        for ne_suffix in self.micavis.notable_event_labels():
            append_analysis_menuitem(
                "Notable event timing histogram (normalized): %s" % ne_suffix, 
                lambda widget,sfx=ne_suffix: self.micavis.plot_notable_events_histogram(sfx,normalize=True))

        for label, callback in self.micavis.temp_analysis_menus:
            append_analysis_menuitem(label, callback)

        layout = gtk.MenuItem("Layout")
        menu = gtk.Menu()
        menu.show()
        self.layout_menu = menu
        layout.set_submenu(menu)
        layout.show()
        self.create_menu_item(self.layout_menu, 
                              "Recompute layout from current view", 
                              self.recompute_layout_from_view)
        bar.append(layout)


        return bar

    def projection_hierarchy(self):
        projs = self.micavis.projections
        ancestors = [0]*len(projs)
        ancestors[0] = -1
        levels = [1]*len(projs)
        levels[0] = 0

        for i,p in enumerate(projs):
            for j,r in reversed(list(enumerate(projs[:i]))):
                if p.startswith(r):
                    ancestors[i] = j
                    levels[i] = levels[j]+1
                    break

        for i,p in enumerate(projs):
            if ancestors[i] <= 0:
                short = p
            else:
                short = p[len(projs[ancestors[i]])+1:]
            short = '    '*levels[i] + short
            yield p,short

    def recompute_layout_from_view(self, widget):
        ual = self.micavis.unique_addresses
        namemap = dict((n,i) for i,n in enumerate(ual))
        graph = igraph.Graph(directed=True)
        n = len(ual)
        graph.add_vertices(n)
        matrix = [ [0] * n for i in xrange(n) ]
        for addr in ual:
            view = self.micavis.current_node_view[addr]
            for peer, weight in view.iteritems():
                matrix[namemap[addr]][namemap[peer]] = weight
        edges = list(logs.matrix_edge_generator(matrix))
        graph.add_edges(edges)
        self.micavis.set_layout(graph, namemap)


        
        
        


class MicaVisMicavis(MicaTrace):

    def __init__(self, logloc, options):
        MicaTrace.__init__(self, logloc)
        self.current = 0
        self.options = options
        self.cursor_listeners = []   # functions that updated cursor ("current") values will be passed to, whenever it changes
        self.adj = None
        # initialize custom modules

        self.create_event_window()
        self.create_graph_window()
        # Move cursor to start
        self.reset_tree_selection()

        if self.adj is None:
            print "Warning: micavis.adj is None, graph window creation failed somehow"

        self.add_cursor_listener(self.current_node_state.set_i)

        # current_node_state[addr] -> the latest state assigned to node "addr" w.r.t. the cursor self.get_current_i()

        self.add_cursor_listener(self.current_node_view.set_i)

        self.exchange_tracker = logs.GossipExchangeTracker2(self.events)
        self.add_cursor_listener(self.exchange_tracker.set_i)

        # NOTE: This cursor listener should be last executed!
        self.add_cursor_listener(lambda i: self.graph_window.refresh_graph())

    def notable_event_labels(self):
        suffixes = set()
        for (suffix,data),timestamp,address in self.notable_events():
            suffixes.add(suffix)
        return sorted(list(suffixes))

    # yields (key,timestamp,address) triples
    # 
    # if restrict_netype is given, key is simply the "data" value
    # of the event.  If restrict_type is not given,
    # key is a pair of (ne_type,data) where ne_type is the suffix
    # from the event_type after notable-event-
    def notable_events(self,restrict_netype=None):
        for e in self.events:
            et = e['event_type']
            if not et.startswith('notable-event'):
                continue
            temp = et.split('-',2)
            if len(temp) == 2:
                ne_type = ""
            else:
                ne_type = temp[-1]
            
            if restrict_netype is not None and restrict_netype != ne_type:
                continue

            ne_name = e.get('data','')

            if restrict_netype is None:
                key = (ne_type,ne_name)
                yield key,e['timestamp'],e['address']
            else:
                key = ne_name
                yield key,e['timestamp'],e['address']
        
    def add_analysis(self, label, callback):
        # called by custom protocols before graph window is created
        # graph window will read this list and use it to build a menu
        self.temp_analysis_menus += [(label, callback)]
    
    def projected_filter(self, data_filter):
        def pf(e):
            return logs.state_event_filter(e) and data_filter(e['data'])
        return pf
    
    _inproj = False # set_active triggers set_projection to be called recursively
                    # _inproj is used to make that recursion fizzle out
    def set_projection_gui(self, widget, p):
        if self._inproj:
            return
        self._inproj = True
        print "set projection %s" % p

        # uncheck other projection menu options...
        for menu_name, checkitem in self.graph_window.projection_checkitems.items():
            if p != menu_name:
                checkitem.set_active(False)
            else:
                checkitem.set_active(True)
        self.set_projection(p)
        self.graph_window.refresh_graph()
        self._inproj = False

    def set_projection(self, p):
        MicaTrace.set_projection(self,p)
        if hasattr(self,'graph_window'):
            self.graph_window.refresh_graph()

    # return subdata) for current projection
    def project(self, data):
        subdata = custom.project(self, self.projection, data)
        return subdata

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
        # executed before cursor listeners
        self.reset_tree_selection()
        self.reset_slider_selection()

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
        if self.options['novis']:
            return None, None
        ual = self.unique_addresses
        g = igraph.Graph(directed=True)

        g.add_vertices(len(ual))
        namemap = dict((n,i) for i,n in enumerate(ual))

        matrix = logs.build_comm_matrix(ual,self.events)
        if matrix is None:
            # fall back to initial state view graph
            matrix = logs.build_final_view_matrix(ual,self.events, self.current_node_view)

        edges = list(logs.matrix_edge_generator(matrix))
        g.add_edges(edges)
        return g, namemap

    def create_graph_window(self):
        graph, namemap = self.create_default_graph()
        self.graph_window = GraphWindow(self, graph, namemap)
        self.graph_window.window.show_all()

    def create_layers_window(self):
        self.layers_window = create_layers_window()

    # graph is an igraph instance
    # name_map is an igraph Layout
    def set_layout(self, graph, name_map):
        self.graph_window.igraph_drawing_area.set_graph(graph, name_map)
        self.graph_window.refresh_graph()

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
                    
        print "Reformat events for MICAVIS in-memory database... (this can take a while)"
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




def main(args=sys.argv):
    options = {}
    try:
        args.remove('-novis')
        options['novis'] = True
    except ValueError:
        options['novis'] = False

    pydir = os.path.split(__file__)[0]
    micadir = os.path.abspath(pydir + "/../..")
    logdir = micadir + "/mica_log"

    if len(args) == 1:
        default_loc = logdir
        args += [default_loc]

    try:
        logloc = args[1]
    except:
        print >> sys.stderr, "Usage: %s <mica_log_dir_or_file>" % args
        sys.exit(1)

    micavis = MicaVisMicavis(logloc, options = options)
    gtk.main()
    






