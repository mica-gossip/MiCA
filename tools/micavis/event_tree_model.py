import pygtk
pygtk.require('2.0')
import gtk
from gtk import gdk

from util import *

def gtk_fmt_tstamp(t):
    return str(t)

DEBUG = False
def noisy(f):
    if not DEBUG:
        return f
    else:
        def g(self,*args):
            a = ', '.join(map(repr,args))
            o = f(self,*args)
            print '%s: %s -> %s' % (f.__name__, a, `o`)
            return o
        return g

class EventTreeModel(gtk.GenericTreeModel):
    # events is a list of events
    # it is presumably immutable!
    _coltypes = [str,str,str,str]

    def __init__(self, events):
        gtk.GenericTreeModel.__init__(self)
        self.events = events

    def on_get_flags(self):
        return 0

    def on_get_n_columns(self):
        return len(self._coltypes)
        
    def on_get_column_type(self, index):
        return self._coltypes[index]

    @noisy
    def on_get_iter(self, path):
        '''returns the node corresponding to the given path.  In our
        case, the node is the path'''
        return tuple(path)

    @noisy
    def on_get_path(self, node):
        '''returns the tree path(a tuple of indices at the various
        levels) for a particular node.'''
        return tuple(node)
                
    @noisy
    def on_iter_next(self, node):
        '''returns the next node at this level of the tree'''
        
        assert node != None

        parent = node[:-1]
        nxt = node[-1]+1

        if nxt < self.on_iter_n_children(parent):
            return tuple(parent + (nxt,))
        else:
            return None
        
    @noisy
    def on_iter_children(self, node):
        '''returns the first child of this node'''
        if node is None: # top of tree
            return (0,)

        if not self.on_iter_has_child(node):
            return None
        else:
            return tuple(node + (0,))

    @noisy
    def on_iter_has_child(self, node):
        '''returns true if this node has children'''
        return self.on_iter_n_children(node) > 0

    @noisy
    def on_iter_n_children(self, node):
        '''returns the number of children of this node'''
        if node is None:
            return 0
        
        if len(node) == 0:
            return len(self.events)

        datakey, data = self._get_data(node)
        dd, recurse = self._data_display(data)
        if not recurse:
            return 0
        else:
            return len(self._get_data_keys(data))
        
        
    def _get_data_keys(self, data):
        if isinstance(data,dict):
            return sorted(data.keys())
        else:
            raise Exception

    @noisy
    def on_iter_nth_child(self, node, n):
        '''returns the nth child of this node'''
        if node == None:
            return (n,)

        if n < self.on_iter_n_children(node):
            return tuple(node + (n,))
        else:
            return None

    @noisy
    def on_iter_parent(self, node):
        '''returns the parent of this node'''
        assert node != None
        if len(node) == 1:
            return None
        else:
            return node[:-1]

    def on_get_value(self, node, column):
        return self._render_row(node)[column]

    def _get_event(self, node):
        return self.events[node[0]]

    def _get_data(self, node, prev=None):
        ''' returns keyname, data.  if supplied, prev is only the parent data '''
        # doesn't sanity check indices -- could request non-existent data
        if node is None:
            return ''

        if len(node) == 0:
            return 'ROOT?'

        if len(node) == 1:        
            event = self._get_event(node)
            key = ''
            try:
                data = event['data']
            except KeyError:
                data = ''
        else:
            if prev is None:
                pkey, prev = self._get_data(node[:-1])
            keys = sorted(prev.keys())
            key = keys[node[-1]]
            data = prev[key]

        while isinstance(data,dict) and data.keys() == ['data']:
            data = data['data']

        return key,data


    def _data_display(self,data):
        # returns (display:string, recurse:boolean)
        if isinstance(data,dict):
            sdata = str(data)
            return self._trunc_title(sdata)
        else:
            return str(data), False

    def _render_row(self, node):
        key, data = self._get_data(node)

        if len(node) == 1:
            event = self._get_event(node)
            tstamp = gtk_fmt_tstamp(event['timestamp'])        
            address = event['address']
            etype = event['event_type']
        else:
            tstamp = ''
            address = ''
            etype = key
        
        data_display_str, recurse = self._data_display(data)

        return [
            tstamp,
            address,
            etype,
            data_display_str
            ]

    # --------------- non-interface utils
    # returns (title, truncated)
    def _trunc_title(self, s, limit=35):
        if len(s) > limit:
            return '...', True
        else:
            return s, False

