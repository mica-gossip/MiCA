#!/usr/bin/env python
# test-igraph-pygtk-cairo.py
 
# example pygtk application using igraph
 
# author: Matteo Zandi
# date: 5 December 2008
 
import gtk
from gtk import gdk
import cairo
import igraph
 
class IGraphDrawingArea(gtk.DrawingArea):
    def __init__(self, n_vertex):
        gtk.DrawingArea.__init__(self)
        self.set_size_request(300, 300)
        self.connect("expose_event", self.expose)
        self.n_vertex = n_vertex
 
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
 
        g = igraph.Graph.Barabasi(self.n_vertex, 15, directed=False)
        plot = igraph.drawing.Plot(surface, (0, 0, rect.width, rect.height))
        plot.add(g)
        plot.redraw()
 
        context.set_source_surface (surface)
        context.paint()
 
        return False
 
    def redraw_canvas(self):
        if self.window:
            alloc = self.get_allocation()
            rect = gdk.Rectangle(alloc.x, alloc.y, alloc.width, alloc.height)
            self.window.invalidate_rect(rect, True)
            self.window.process_updates(True)
 
    def change_n_vertex(self, n):
        self.n_vertex = n
        self.redraw_canvas()
 
class Demo():
    def __init__(self):
        window = gtk.Window()
        vbox = gtk.VBox(False, 0)
        self.igraph_drawing_area = IGraphDrawingArea(10)
        vbox.pack_start(self.igraph_drawing_area, True, True, 0)
 
        adj = gtk.Adjustment(10, 1, 100, 1, 1, 0)
        adj.connect("value_changed", self.cb_value_changed)
        self.slider = gtk.HScale(adj)
        self.slider.set_digits(0)
        vbox.pack_start(self.slider, False, False, 0)
 
        window.add(vbox)
        window.connect("destroy", gtk.main_quit)
        window.show_all()
 
        gtk.main()
 
    def cb_value_changed(self, adj):
        self.igraph_drawing_area.change_n_vertex(int(adj.value))
 
if __name__ == "__main__":
    Demo()
