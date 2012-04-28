import os, re, sys, json

def EVENTS_TIMESTAMP_CMP(ev1, ev2): 
    return cmp(ev1['timestamp'], ev2['timestamp'])

def EVENTS_FILTER_EVENTTYPE(etype):
    return lambda e: e['event_type'] == etype

# returns a list of event
def read_mica_logs(logdir, order_func = EVENTS_TIMESTAMP_CMP, filter_func = None):
    events = []
    for filename in [os.path.join(logdir,x) for x in os.listdir(logdir) if x.endswith('log')]:
        with open(filename,'r') as f:
            for line in f.xreadlines():
               event = json.loads(line)
               events.append(event)

    if filter_func != None:
        events = filter(filter_func, events)

    if order_func:
        events.sort(cmp=order_func)

    return events
            
# return a list of all unique addresses that appear in an event list
def query_unique_addresses(events):
    return list(set((e['address'] for e in events)))
        

# returns a dict of addresss->(x,y) coordinates for all nodes
# x and y are in the range [0,1)
def assign_addresses(events):
    # FIXME: this is just a hack to get things working
    # returns random placements
    import random
    d = {}
    for addr in query_unique_addresses(events):
        x = random.random()
        y = random.random()
        d[addr] = (x, y)
    return d
    
# return (min, max) timestamps in the given events
def query_timestamp_range(events):
    assert(len(events) > 0)
    stamps = [e['timestamp'] for e in events]
    stamps.sort()
    return stamps[0], stamps[-1]


# returns a matrix weighted by the (relatively normalized) number of times each
# address selected each other address
#   graph[src][dst]
def build_comm_matrix(unique_address_list, events):
    tot = 0
    n = len(unique_address_list)
    matrix = [ [0.] * n for i in xrange(n) ]
    index = dict((a,i) for i,a in enumerate(unique_address_list))
    selevts = filter(EVENTS_FILTER_EVENTTYPE("select"), events)
    assert(len(selevts) > 0)
    for e in selevts:
        tot += 1
        src = e['address']
        dst = e['data']
        matrix[index[src]][index[dst]] += 1.0

    for i in xrange(n):
        for j in xrange(n):
            matrix[i][j] /= tot

    return matrix


def matrix_edge_generator(comm_matrix):
    # matrix must be square!
    n = len(comm_matrix)
    for i in xrange(n):
        for j in xrange(n):
            if comm_matrix[i][j] > 0:
                yield (i,j)


