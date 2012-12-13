import os, re, sys, json, tarfile, zipfile, operator
from math import *

# returns an array of "round buckets" and a function that 
# returns the bucket of a given event
def round_bucketer(micavis):
    round_ms = micavis.runtime_info.round_ms
    start_t = micavis.runtime_info.first_timestamp
    end_t = micavis.runtime_info.last_timestamp
    buckets = [0] * int(ceil(float(end_t - start_t) / round_ms))

    def bucketf(event):
        timestamp =  event['timestamp']
        return int(floor((timestamp - start_t) / float(round_ms)))
    
    return buckets, bucketf

# identify state events
def state_event_filter(event):
    return event['event_type'].startswith('mica-state-') and 'state' in event['data']


def EVENTS_TIMESTAMP_CMP(ev1, ev2): 
    return cmp(ev1['timestamp'], ev2['timestamp'])

def EVENTS_FILTER_EVENTTYPE(etype):
    return lambda e: e['event_type'] == etype

class LogCollection(object):
    def __init__(self, path):
        self.path = path

    def __str__(self):
        return self.path.split('/')[-1]
#        return "%s" % (self.__class__.__name__, self.path)

    def __repr__(self):
        return str(self)

    def logs(self):
        raise Exception('not implemented. should return a list of ReadableFilelike objects')

class ReadableFilelike(object):
    def __init__(self, path):
        self.path = path

    def __str__(self):
        return str(self.path).split('/')[-1]
#        return "%s %s" % (self.__class__.__name__, self.path)

    def __repr_(self):
        return "%s" % str(self)

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
    def __str__(self):
        # stupid stupid hack to print only the filename
        return str(self.path[1]).split(' ')[1].split('/')[-1][:-1] + " (compressed)"

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
    def __init__(self, events, filter_func=None, value_func=None, i=0, value_equality_func = operator.eq, default_value = None, debug_flag = False):
        self.events = events

        # if filter_func and value_func not specified, assume they are 
        # already defined by subclass
        if filter_func:
            self.filter_func = filter_func
        if value_func:
            self.value_func = value_func

        self.debug_flag = debug_flag
        self.default_value = default_value
        self.value_equality_func = value_equality_func
        self.values = {} # maps key -> (event_i, value),  where event_i <= i and no greater event_j <= i returns true for filter_func
             # returns (-1,default_value) if no suitable event sets the value
        self.set_i(i)

        # if the cursor is advanced <= forward_scan_limit events, then we progressively update the existing value 
        # cache by scanning the newly added interval.  Else, we delete the value cache and let it be passively
        # rebuilt
        self.forward_scan_limit = 20

    def debug(self, msg):
        if self.debug_flag:
            print msg

    def reset(self):
        self.values.clear()

    def set_i(self, i):
        # scanpoint = the place to resume the backwards scanning, i.e, invariant is that (scanpoint,self.i] has already been scanned.  
        # (inclusive of i?  yes     inclusive of scanpoint?  no)
        # IF scanpoint == i, then this will be rescanned upon get
        if not hasattr(self, 'i'):
            # initialize
            # advanced beyond the forward scan limit; clear the cache and let it get rebuilt passively
            self.values.clear()
            self.i = i
            self.scanpoint = i      
        elif i == self.i:
            return
        elif (i > self.i) and (i - self.i) <= self.forward_scan_limit:
            # progressive forward scan
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
                if j >= i:
                    del self.values[k]
            self.i = i
            self.scanpoint = i
        else:
            # advanced beyond the forward scan limit; clear the cache and let it get rebuilt passively
            self.values.clear()
            self.i = i
            self.scanpoint = i
            
        self.debug("(debug) scanpoint %s.  nvalues = %s" % (i,len(self.values)))
    
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
            if k not in self.values or self.values[k][0] < j:
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

    def finish(self):  # sets to end
        self.set_i(len(self.events)-1)
    
    # yields tuples (i, key, value) from the [start,end) range,
    # does not affect cursor!
    # if yield_events is true, yields events instead of index:
    #      (event, key, value)
    def enumerate(self, start=0, end=None, yield_events=False):
        if end is None:
            end = len(self.events)
        end = min(end, len(self.events))
        start = max(0, start)

        for i in xrange(start, end):
            e = self.events[i]
            if not self.filter_func(e):
                continue
            else:
                k,v = self.value_func(e)
                if yield_events:
                    yield (e, k, v)
                else:
                    yield (i, k, v)


class ForwardFeedScanner(object):
    def __init__(self, events, initial_value_func = None):
        self.events = events
        if initial_value_func:
            self.initial_value_func = initial_value_func
        self.reset()
        
    def initial_value_func(self):
        return None
        
    def reset(self):
        self.i = 0
        self.lazy_i = 0
        self.value = self.initial_value_func()

    def set_i(self, i):
        self.lazy_i = i
        
    def realize(self):
        if self.lazy_i > self.i:
            self.advance(self.lazy_i)

        elif self.lazy_i < self.i:
            self.rewind(self.lazy_i)

    def advance(self, j):
        for self.i in xrange(self.i+1, j+1):
            self.value = self.feed(self.value, self.events[self.i])

    def rewind(self, j):
        # precondition: j < self.i
        # rewind the value to event index j
        # default behavior --- this can certainly be done smarter for some applications
        self.reset() 
        self.set_i(j)
        self.realize()
        
    # returns an updated value
    def feed(self, value, event):
        return value

    def get(self):
        self.realize()
        return self.value

class GossipExchangeTracker2(ForwardFeedScanner):
    
    # etype : (sender = T/F, receiver = T/F, start = T/F,  stop_current = T/F, stop_incoming = T/F)
    etypes = set(
        ['mica-select',
        'mica-state-preupdate',
        'mica-state-gossip-initiator',
        'mica-state-postupdate',
        'mica-error-internal',
        'mica-state-gossip-receiver',
        'mica-error-accept-connection']
        )

    def __init__(self, events):
        # mica-select events mark the beginning of an exchange, so we track them separately
        self.cache = {0:[]}
        
        self.select_tracker = CurrentValueTracker(events, 
                                                  EVENTS_FILTER_EVENTTYPE('mica-select'),
                                                  lambda e: (e['address'],e))
        ForwardFeedScanner.__init__(self, events, lambda: ({},{}))


    def set_i(self, i):
        super(GossipExchangeTracker2,self).set_i(i)
        self.select_tracker.set_i(i)
    
    def get_receiver(self, selevt):
        assert(selevt['event_type'] == 'mica-select')
        return selevt['data']['selected']

    # tuple generator: (sender,receiver,stage)
    def get_exchanges(self, micavis):
        if self.lazy_i in self.cache:
            return self.cache[self.lazy_i]

        self.realize()
        return self.build_exchanges()

    def build_exchanges(self):
        exchanges, receivers = self.value
        for sender, exchange in exchanges.iteritems():
            receiver = self.get_receiver(exchange[0])
            stage = exchange[-1]
            if stage != 'complete':
                stage = stage['event_type']
            yield (sender, receiver, stage)
        
    def dump(self, exchanges, receivers):
        print 'Exchanges'

        def digest(evt):
            if evt == 'complete':
                return evt

            return evt['address'][-4:]+"/"+evt['event_type']
        for a,x in sorted(exchanges.items()):
            evdux = ','.join([digest(v) for v in x])
            print '   ', a, evdux
        print "Receivers"
        for b,s in sorted(receivers.items()):
            print '   ', b, '   ', ','.join(s)

    def feed(self, (exchanges,receivers), event):

        for a in exchanges.keys():  # clean up finished exchanges
            if exchanges[a][-1] == 'complete':
                b = self.get_receiver(exchanges[a][0])
                del exchanges[a]
                if b in receivers and a in receivers[b]:
                    receivers[b].remove(a)
                    if len(receivers[b]) == 0:
                        del receivers[b]
                    
        etype = event['event_type']
        if etype not in self.etypes:
            self.cache[self.i] = self.cache[self.i-1]
            return (exchanges,receivers)  # irrelevant event
        
        addr = event['address']
        
        def add_normal_sender_event(addr,ev):
            if addr in exchanges:
                exchanges[addr] += [ev]
            else:
                # reconstruct -- we mistakenlty deleted the wrong exchange
                #   while handling an error event
                sel = self.select_tracker[addr]
                if sel is not None:
                    exchanges[addr] = [sel,ev]
                    # add to receivers
                    recvr = self.get_receiver(sel)
                    if recvr not in receivers:
                        receivers[recvr] = [addr]
                    else:
                        # FIXME should add event in correct chronological order
                        receivers[recvr].insert(0,addr)

        if etype == 'mica-select':
            # kill any previous outgoing exchanges
            #    (happens automatically when we replace exchanges[addr])
            #    still need to clean up receivers
            for r in receivers.keys():
                if addr in receivers[r]:
                    receivers[r].remove(addr)
                    if len(receivers[r]) == 0:
                        del receivers[r]
            # begin a new exchange 
            exchanges[addr] = [event]
            b = self.get_receiver(event)
            if b not in receivers:
                receivers[b] = [addr]
            else:
                receivers[b] += [addr]
            # kill any incoming exchanges
                # NO! incoming exchanges not necessarily finished
                # on the other end...
            #if addr in receivers:
            #    for x in receivers[addr]:
            #        exchanges[x] += ['complete']

        elif etype in 'mica-state-preupdate':
            # add to outgoing exchange
            add_normal_sender_event(addr,event)
        elif etype == 'mica-state-gossip-initiator':
            # add to outgoing exchange
            add_normal_sender_event(addr,event)
        elif etype == 'mica-state-postupdate':
            # add to outgoing exchange
            add_normal_sender_event(addr,event)
        elif etype == 'mica-state-gossip-receiver':
            # add to incoming exchange
            if addr in receivers:
                sender = receivers[addr][0]
                if sender in exchanges:
                    incoming_ex = exchanges[sender]
                    incoming_ex += [event]
        elif etype == 'mica-error-internal':
            # kill incoming and outgoing exchanges
            #    note: "Connection reset" indicates only outgoing killed

            if addr in exchanges:
                # kill outgoing
                exchanges[addr] += [event, 'complete']

            if event['data'] != 'Connection reset':
                if addr in receivers:
                    # kill incoming
                    for sender in receivers[addr]:
                        if sender in exchanges:
                            exchanges[sender] += [event, 'complete']

        elif etype == 'mica-error-accept-connection':
            if addr not in receivers:
                # something is screwed up with our bookkeeping
                pass
            else:
                if addr in exchanges:
                    # if current outgoing connection, kill oldest incoming exchange
                    killdex = 0
                else:
                    if len(receivers[addr]) > 1:
                        killdex = 1
                    else:
                        killdex = 0
                sender = receivers[addr][killdex]
                if sender in exchanges:
                    exchanges[sender] += [event, 'complete']

        self.value = (exchanges,receivers)
        self.cache[self.i] = list(self.build_exchanges())
        return (exchanges, receivers)
    
#    def oldest_exchange(self, ex1, ex2):
#        if cmp(ex1[0]['timestamp'],ex2[0]['timestamp']) == -1:
#            return ex1
#        else:
#            return ex2

#    def get_receiver_exchange(self, receiver, exchanges, receivers):
        # find the oldest open exchange with the specified receivers
#        return exchanges[receivers[receiver]]

class GossipExchangeTracker(object):
    # gossip exchange lifecycle:
    #   a: mica-select  (data/selected = b)
    #   a: mica-state-preupdate
    #   b: mica-state-gossip-receiver
    #   a: mica-state-gossip-initiator
    #   a: mica-rate [cancels transaction]
    def __init__(self, events):
        self.i = 0

        def etype_tracker(etype):
            return CurrentValueTracker(events, 
                                       EVENTS_FILTER_EVENTTYPE(etype),
                                       lambda e: (e['address'],e))

        send_etypes = [
            'mica-select',
            'mica-state-preupdate',
            'mica-state-gossip-initiator',
            'mica-rate',
            'mica-error-internal',
            ]
        
        recv_etypes = [
            'mica-state-gossip-receiver',
            'mica-error-internal',
            'mica-error-accept-connection'
            ]

        self.select_tracker = etype_tracker('mica-select')

        r = re.compile('|'.join(send_etypes))
        sendmatch = lambda e: r.match(e['event_type'])
        self.send_tracker = CurrentValueTracker(events, 
                                              sendmatch,
                                              lambda e: (e['address'],e))
        
        r = re.compile('|'.join(recv_etypes))
        recvmatch = lambda e: r.match(e['event_type'])
        self.recv_tracker = CurrentValueTracker(events, 
                                              recvmatch,
                                              lambda e: (e['address'],e))

    
    def set_i(self, i):
        self.i = i
        self.send_tracker.set_i(i)
        self.recv_tracker.set_i(i)
        self.select_tracker.set_i(i)

    final_stages = ('mica-state-gossip-receiver','mica-rate','mica-error-internal', 'mica-error-accept-connection')
    # tuple generator: (sender,receiver,stage)
    def get_exchanges(self, micavis):
        # DOESN'T CATCH ALL CLOSE EVENTS, NEED A DIFFERENT KIND OF SCANNER!
        exchanges = {}

        for addr in micavis.unique_addresses:
            send = self.send_tracker[addr]
            if send and send['event_type'] != 'mica-rate':
                se = self.select_tracker[addr]
                if se:
                    t = int(send['timestamp'])
                    b = se['data']['selected']
                    exchanges[addr] = [t, b, send['event_type']]
                else:
                    continue # can't figure out who recv is
                recv = self.recv_tracker[b]
                exchange = exchanges[addr]
                if recv:
                    rt = int(recv['timestamp'])
                    if rt >= exchanges[addr][0]:
                        exchange[0] = rt
                        exchange[2] = recv['event_type']
                
        for send,(t,recv,stage) in exchanges.iteritems():
            if stage in self.final_stages and self.i > t:
                continue
            yield (send,recv,stage)
        
class RuntimeInfoParser(CurrentValueTracker):
    # publicly exposed properties:
    round_ms = property(lambda self: int(self.get_data()['round_ms']))
    # first_timestamp
    # last_timestamp
    def __init__(self, events):
        CurrentValueTracker.__init__(self, events)
        self.finish()
        # FIXME assumes sorted by timestamp
        self.first_timestamp = int(events[0]['timestamp'])
        self.last_timestamp = int(events[-1]['timestamp'])

    def filter_func(self, e):
        return e['event_type'] == 'mica-runtime-init'
    
    def value_func(self, e):
        # key is the same for all runtimes ---
        # currently assuming that all runtime inits are the same
        # TODO: verify this assumption
        return (0,{'round_ms':e['data']['round_ms']})

    def get_data(self):
        return self[0]



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
    try:
        if event['event_type'] != 'view':
            return event
    except Exception, e:
        print >> sys.stderr, "event == %s" % repr(event)
        raise e

    view = event['data']
    if view:
        for k, v in view.items():
            view[k] = float(v)
    return event

def remove_redundant_state_update(event):
    try:
        del event['data']['state']
    except KeyError:
        pass
    event['data']['state-unchanged'] = True
    return event

def remove_redundant_view_update(event):
    try:
        del event['data']['view']
    except KeyError:
        pass
    event['data']['view-unchanged'] = True
    return event

ep_redundant_state_update_eliminator = RedundantEventEliminator(
    filter_func = lambda e: e['event_type'].startswith('mica-state-'),
    value_func = lambda e: (e['address'], e['data'].get('state')),
    adapter_func = remove_redundant_state_update)

ep_redundant_view_update_eliminator = RedundantEventEliminator(
    filter_func = lambda e: e['event_type'].startswith('mica-state-'),
    value_func = lambda e: (e['address'], e['data'].get('view')),
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
        print "Parsing log %s" % logobj
        while True:
            # xreadlines not implemented by TarFile
            line = f.readline()
            if line == '':
                break  # EOF
            try:
                event = json.loads(line)
                # unterminated line will break -- this happens if system exits while writing log, probably due to a fatal error
                # we'll just ignore this last malformed line
            except:
                break  # EOF

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
    selevts = filter(EVENTS_FILTER_EVENTTYPE("mica-select"), events)
    
    for e in selevts:
        tot += 1
        src = e['address']
        dst = e['data']['selected']
        matrix[index[src]][index[dst]] += 1.0

    if tot > 0:
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
