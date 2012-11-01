modules = {}

# modules are expected to define the following interface:
#
# def draw_state(vis, address, state)
#
#
# 


# returns a module if 
def load(stateType):
    modname = 'micavis.custom.' + stateType
    if modname in modules:
        return modules[modname]

    try:
        exec('import %s as mod' % modname)
        modules[modname] = mod
        return mod
    except ImportError, e:
        modules[modname] = None
        return None
