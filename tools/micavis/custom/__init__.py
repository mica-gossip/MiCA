import os,re,sys

modules = {}

# Needs to be called by micavis application to give module a chance to 
# initialize
def init(micavis):
    customdir = os.path.split(__file__)[0]
    for f in os.listdir(customdir):
        if not f.endswith('.py') or f.startswith('_'):
            continue
        protocol_name = f.split('.')[0]
        load(micavis,protocol_name)


# modules are expected to define an object named "protocol" which defines
# the interface from the CustomProtocol class (below)

# returns a module if one exists, or None
#   calls the load(micavis) function in the module when it is 
#   loaded for the first time
def load(micavis, stateType):
    modname = 'micavis.custom.' + stateType
    if modname in modules:
        return modules[modname]

    try:
        exec('from %s import protocol' % modname)
        modules[modname] = protocol
        protocol.load(micavis)
        return protocol
    except ImportError, e:
        print "Error loading %s: %s" % (stateType, e)
        modules[modname] = default_protocol
        return default_protocol

# data is the 'data' portion of a state event,
# which embeds:
#   stateType
#   state
#   view 
def get_module(micavis, data):
    return load(micavis, data['stateType'])

# data is the 'data' payload of a state event
def project(micavis, projection_key, data):
    module = get_module(micavis, data)
    return module.project(projection_key, data)

class CustomProtocol(object):
    def load(self, micavis):
        # micavis is the "gui" main application instance
        # called once upon loading by .custom
        pass

    def draw_node(self, vis, address, data):
        node_label = self.get_node_label(vis, address, data)
        display_options = self.get_display_options(vis, address, data)
        draw = False

        if node_label != None:
            draw = True
            display_options['vertex_label'] = [node_label]
            display_options['vertex_label_dist'] = 6

        if draw:
            vis.draw_node(address, **display_options)

    def get_node_label(self, vis, address, data):
        return None

    def get_display_options(self, vis, address, data):
        return vis.micavis.standard_display_options.copy()

    # nodes is a list of (address, data) tuples
    # by default, calls draw_node individually
    def draw_nodes(self, vis, nodes):
        for addr, data in nodes:
            self.draw_node(vis, addr, data)
                   
    # state is the ['data'] value of a state event
    # it has keys:
    #   stateType
    #   state
    #   view
    #
    # returns a list of tuples (key, f)
    #   key is the projection's name
    #   f is a function that takes in the state  (event['data']['state'])
    #      and returns the projection's 'data' substructure
    def projections(self, data):
        return []

    def project(self, projection_key, data):
        if projection_key == 'root':
            return data # hardcoded identity function
        for pkey, pfunc in self.projections(data):
            if pkey == projection_key:
                return pfunc(data)

        print "Error: Projection %s not found, using root" % projection_key
        print "class is", self.__class__.__name__
        print "stateType is", data['stateType']
        print "   debug: available projections are:"

        for pkey, pfunc in self.projections(data):
            print "     ", pkey
        raise Exception

        return data

default_protocol = CustomProtocol()
