modules = {}

# modules are expected to define the following interface:
#
# draws the state of an individual node
# def draw_node_state(vis, address, state)
#
#
# def load(micavis)  -- micavis is the "gui" main application instance
#     called once upon loading


# returns a module if one exists, or None
#   calls the load(micavis) function in the module when it is 
#   loaded for the first time
def load(micavis, stateType):
    modname = 'micavis.custom.' + stateType
    if modname in modules:
        return modules[modname]

    try:
        exec('import %s as mod' % modname)
        modules[modname] = mod
        mod.load(micavis)
        return mod
    except ImportError, e:
        modules[modname] = None
        return None
