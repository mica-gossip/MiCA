import os, re, sys, json, tarfile, zipfile

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


# returns a list of event
def read_mica_logs(logdir, order_func = EVENTS_TIMESTAMP_CMP, 
                   filter_func = None):
    events = []    
    if os.path.isdir(logdir):
        logdir = LogDir(logdir)
    elif tarfile.is_tarfile(logdir):
        logdir = LogDirTarFile(logdir)
    elif zipfile.is_zipfile(logdir):
        logdir = LogDirZipFile(logdir)
    else:
        raise Exception("unrecognized log file format %s" % logdir)

    for logobj in logdir.logs():
        f = logobj.open()
        while True:
            # xreadlines not implemented by TarFile
            line = f.readline()
            if line == '':
                break  # EOF
            event = json.loads(line)
            events.append(event)
        f.close()


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

