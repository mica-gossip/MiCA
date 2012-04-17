#!/usr/bin/env python

# example basictreeview.py

import pygtk
pygtk.require('2.0')
import gtk
import os, sys
import time
from util import *

class BasicTreeViewExample:

    # close the window and quit
    def delete_event(self, widget, event, data=None):
        gtk.main_quit()
        return False

    def __init__(self, events):
        self.events = events
        self.create_event_window()
        self.create_graph_window()

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
        self.event_window.set_size_request(200, 200)
        self.event_window.connect("delete_event", self.delete_event)

        self.event_scrollwindow = gtk.ScrolledWindow()
        self.event_scrollwindow.set_border_width(10)
        self.event_scrollwindow.set_policy(gtk.POLICY_AUTOMATIC, gtk.POLICY_ALWAYS)
        self.event_vbox = gtk.VBox()
        self.event_vbox.pack_start(self.event_scrollwindow, True)

        # create a TreeStore with one string column to use as the model
        self.treestore = gtk.TreeStore(int, str, str, str)

        self.init_populate_treestore()

        # create the TreeView using treestore
        self.treeview = gtk.TreeView(self.treestore)

        # create the TreeViewColumn to display the data
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
        self.event_window.maximize()

    def init_populate_treestore(self):
        # populate the table with events

        def fmt_tstamp(t):
            if not isinstance(t,long):
                return t
            else:
                onetime_warning("Warning: Timestamps truncated because GUI elements restricted to 32-bit integers")
                t = t & 0x0FFFFFFF
                return int(t)

        
        def trunc(s, limit=50):
            if len(s) > limit:
                s = s[:limit] + '...'
            return s

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
            

def main(args=sys.argv):
    try:
        logdir = args[1]
    except:
        print >> sys.stderr, "Usage: %s <mica_log_dir>" % args
        sys.exit(1)

    import logs
    events = logs.read_mica_logs(logdir)
    gui = BasicTreeViewExample(events)
    gtk.main()
    

