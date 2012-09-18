import os, re, sys, json, tarfile, zipfile, operator

def EVENTS_TIMESTAMP_CMP(ev1, ev2): 
    return cmp(ev1['timestamp'], ev2['timestamp'])

def EVENTS_FILTER_EVENTTYPE(etype):
    return lambda e: e['event_type'] == etype

class LogCollection(object):
    def __init__(self, path):
        self.path = path

    def logs(self):
        raise Exception('not implemented. should return a list of ReadableFilelike objects')

class ReadableFilelike(object):
    def __init__(self, path):
        self.path = path

    def open(self):
        raise Exception('not implemented. should return an open file that can be closed')

class LogFile(ReadableFilelike):
    def open(self):
        return open(self.path,'r') 

class LogDir(LogCollection):
    def logs(self):
        logpaths = [os.path.join(self.path,x) 
                    for x in os.listdir(self.path) if x.endswith('log')]

        return [LogFile(p) for p in logpaths]

class LogTarFile(ReadableFilelike):
    def open(self):
        tf, member = self.path
        return tf.extractfile(member)

class LogDirTarFile(LogCollection):
    def logs(self):
        tf = tarfile.open(self.path,'r')
        members = tf.getmembers()
        logs = [tarinfo for tarinfo in members if tarinfo.name.endswith('.log')]
        return [LogTarFile((tf, tarinfo)) for tarinfo in logs]

class LogZipFile(ReadableFilelike):
    def open(self):
        tf, member = self.path
        return tf.open(member)

class LogDirZipFile(LogCollection):
    def logs(self):
        zf = zipfile.ZipFile(self.path,'r')
        members = zf.infolist()
        logs = [zipinfo for zipinfo in members if zipinfo.filename.endswith('.log')]
        return [LogZipFile((zf, zipinfo)) for zipinfo in logs]

class CurrentValueTracker(object):
    """
filter_func is a function:  event -> boolean
    value_func will only be run on events for which filter_func is true

value_func is a function:  event -> (K,T)
    where k is a key bucket and T is the type of value being recorded for each key

value_equality_func is a function: T x T -> boolean
    it says whether two values are equal or not. defaults to python '==' equality

default_value is the value assigned to a key if no suitable events have occurred
    """
    def __init__(self, events, filter_func, value_func, i=0, value_equality_func = operator.eq, default_value = None):
        self.events = events
        self.filter_func = filter_func
        self.value_func = value_func
        self.default_value = default_value
        self.value_equality_func = value_equality_func
        self.values = {} # maps key -> (event_i, value),  where event_i <= i and no greater event_j <= i returns true for filter_func
             # returns (-1,default_value) if no suitable event sets the value
        self.set_i(i)

        # if the cursor is advanced <= forward_scan_limit events, then we update the existing value 
        # cache by scanning the newly added interval.  Else, we delete the value cache and let it be passively
        # rebuild
        self.forward_scan_limit = 20


    def set_i(self, i):
        # scanpoint = the place to resume the backwards scanning, i.e, invariant is that [scanpoint,self.i] has already been scanned
        if not hasattr(self, 'i'):
            # advanced beyond the forward scan limit; clear the cache and let it get rebuilt passively
            self.values.clear()
            self.i = i
            self.scanpoint = i            
        elif i == self.i:
            return
        elif (i > self.i) and (i - self.i) <= self.forward_scan_limit:
            # active forward scan
            for j in xrange(self.i+1, i+1):
                e = self.events[j]
                if not self.filter_func(e):
                    continue
                k,v = self.value_func(e)
                self.values[k] = (j,v)  # overwrite older values, if they exist
            self.i = i 
            # scanpoint is unchanged
        elif i < self.i:
            # remove all values that were set in the future
            for k,(j,v) in self.values.items():
                if j > i:
                    del self.values[k]
            self.i = i
            self.scanpoint = i
        else:
            # advanced beyond the forward scan limit; clear the cache and let it get rebuilt passively
            self.values.clear()
            self.i = i
            self.scanpoint = i
            
    
    def __getitem__(self, key):
        j, val = self.get(key)
        return val

    def get(self, key):
        if key in self.values:
            return self.values[key]

        j = self.scanpoint
        
        while j >= 0:
            e = self.events[j]
            if not self.filter_func(e):
                j -= 1
                continue
            k,v = self.value_func(e)
            rval = (j,v)
            self.values[k] = rval
            if key == k:
                self.scanpoint = j
                return rval
            j -= 1

        # j < 0
        rval = (-1,self.default_value)
        self.values[key] = rval
        self.scanpoint = j
        return rval
    

class RedundantEventEliminator(CurrentValueTracker):
    def __init__(self, filter_func, value_func, value_equality_func = operator.eq, adapter_func = lambda e: None):
        CurrentValueTracker.__init__(self, [], filter_func, value_func, 
                                     value_equality_func = value_equality_func)
        self.adapter_func = adapter_func
                         
    def __call__(self, event):
        if self.feed(event):
            return self.adapter_func(event)
        else:
            return event

    # for redundant event filtering mode: 
    #   returns True if the fed event is redundant and can be safely filtered, False otherwise
    def feed(self, event):
        if not self.filter_func(event):
            return False
        
        key, value = self.value_func(event)
        
        if key in self.values and self.value_equality_func(value, self.values[key]):
            return True
        else:
            self.values[key] = value
            return False


def ep_view_event_formatter(event):
    if event['event_type'] != 'view':
        return event
    view = event['data']
    if view:
        for k, v in view.items():
            view[k] = float(v)
    return event

def remove_redundant_state_update(event):
    del event['data']['state']
    event['data']['state-unchanged'] = True
    return event

def remove_redundant_view_update(event):
    del event['data']['view']
    event['data']['view-unchanged'] = True
    return event

ep_redundant_state_update_eliminator = RedundantEventEliminator(
    filter_func = lambda e: e['event_type'].startswith('state-'),
    value_func = lambda e: (e['address'], e['data']['state']),
    adapter_func = remove_redundant_state_update)

ep_redundant_view_update_eliminator = RedundantEventEliminator(
    filter_func = lambda e: e['event_type'].startswith('state-'),
    value_func = lambda e: (e['address'], e['data']['view']),
    adapter_func = remove_redundant_view_update)
    

default_event_processors = [
    ep_view_event_formatter,
    ep_redundant_state_update_eliminator,
    ep_redundant_view_update_eliminator,
]

# returns a list of events
def read_mica_logs(logdir, order_func = EVENTS_TIMESTAMP_CMP, 
                   filter_func = lambda e: True,
                   event_processors = default_event_processors):
    
    # event processors are functions:  event -> event,  possibly modifying the event

    if os.path.isdir(logdir):
        logdir = LogDir(logdir)
    elif tarfile.is_tarfile(logdir):
        logdir = LogDirTarFile(logdir)
    elif zipfile.is_zipfile(logdir):
        logdir = LogDirZipFile(logdir)
    else:
        raise Exception("unrecognized log file format %s" % logdir)

    events = []

    for logobj in logdir.logs():
        f = logobj.open()
        while True:
            # xreadlines not implemented by TarFile
            line = f.readline()
            if line == '':
                break  # EOF
            event = json.loads(line)
            if not filter_func(event):
                continue
            for event_processor in event_processors:
                event = event_processor(event)
                if not event: # event has been filtered
                    break
            if event:
                events.append(event)
        f.close()

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
    print "Building communication matrix"
    tot = 0
    n = len(unique_address_list)
    matrix = [ [0.] * n for i in xrange(n) ]
    index = dict((a,i) for i,a in enumerate(unique_address_list))
    selevts = filter(EVENTS_FILTER_EVENTTYPE("select"), events)
    assert(len(selevts) > 0)
    for e in selevts:
        tot += 1
        src = e['address']
        dst = e['data']['selected']
        matrix[index[src]][index[dst]] += 1.0

    for i in xrange(n):
        for j in xrange(n):
            matrix[i][j] /= tot

    return matrix


def matrix_edge_generator(comm_matrix):
    print "Identifying communication matrix edges"
    # matrix must be square!
    n = len(comm_matrix)
    for i in xrange(n):
        for j in xrange(n):
            if comm_matrix[i][j] > 0:
                yield (i,j)


def subprotocols(protocol_data):
    # yields a list of (key, sub_data) tuples 
    # the key must be 

    # data is a {state:  ...   view:  ...  stateType: ...} dict, as
    # produced by the custom json serialization
    
    # returns (state, {childname: childnode,  ...})

    if protocol_data:
        s = protocol_data.get('state',None)
        if s:
            for key in ('p1','p2'):
                if key in s:
                    yield (key,s[key])
