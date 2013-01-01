
from math import *
import logs

def deltas(sequence):
    sq = sorted(list(sequence))
    it = iter(sq)
    prev = it.next()
    try:
        while True:
            x = it.next()
            yield x - prev
            prev = x
    except StopIteration:
        pass


# returns a list of tuple-lists, where events have the form:
#  (timestamp, src, dst)    
#
# one list is returned for each leaf projection
def gossip_events(events):
    buckets = {}

    gossip_total = 0
    leaves_per_gossip = 0.

    leaf_sequences = []
    
    dval = {'true':True,'false':False}
    
    def value(leafstatus):
        return dval[leafstatus.split(':')[1]]

    for e in events:
        if e['event_type'] == 'merge-execute-subprotocols':
            buckets[e['address']] = [value(x) for x in e['data'].split(',')]
        if e['event_type'] == 'mica-gossip':
            bk = buckets.get(e['address'],[True])
            src, dst = e['data']
            tupl = (e['timestamp'], src, dst)
            gossip_total += 1
            if len(leaf_sequences) < len(bk):
                leaf_sequences += [[] for i in xrange(len(bk) - len(leaf_sequences))]
            for i,relevant in enumerate(bk):
                if relevant:
                    leaves_per_gossip += 1
                    leaf_sequences[i].append(tupl)

    leaves_per_gossip /= gossip_total
    print "Gossip statistics: %s top-level gossips, %s leaves per gossip, total gossip activity %s" % (gossip_total, leaves_per_gossip, leaves_per_gossip * gossip_total)
    return leaf_sequences


def compute_changes_per_round(trace, **frequency_count_keywords):
    
    def node_state_change_timestamp_generator():
        values = {}
        node_states = logs.CurrentValueTracker(
            trace.events, 
            filter_func =  logs.state_event_filter,
            value_func = lambda e,mv=trace: (e['address'], (e['timestamp'],mv.project(e['data']))) )
    
        for i,addr,(t,data) in node_states.enumerate(yield_events=True):
            if addr not in values or values[addr] != data:
                values[addr] = data
                yield t,addr

    return frequency_count(trace, 
                           node_state_change_timestamp_generator(), 
                           **frequency_count_keywords)


# timestamp_generator is an iterable of timestamps, OR an iterable of (timestamp,key) pairs,
# where each unique id can only increment a time bucket once:
#   e.g., pairs   (12039, "key1"),  (12040, "key1")  will only count as one if 12039 and 12040 fall into the
# same bucket
#
# return x, y lists for plotting
#  x = round bucket number
#  y = number of changes in bucket
def frequency_count(trace, timestamp_generator, bucket_size_ms = None, bucket_x = lambda i: i, normalize = False, bucket_scalar = 1.0, subdivisions=1):
    if bucket_size_ms is None:
        bucket_size_ms = trace.runtime_info.round_ms

    if subdivisions > 1:
        bucket_size_ms = int(float(bucket_size_ms)/subdivisions)
        bucket_x = lambda i,f=bucket_x: f(float(i)/subdivisions)
        bucket_scalar *= subdivisions

    start_t = trace.runtime_info.first_timestamp
    end_t = trace.runtime_info.last_timestamp
    buckets = [0] * int(ceil(float(end_t - start_t) / bucket_size_ms))

    def bucket(timestamp):
        return int(floor((timestamp - start_t) / float(bucket_size_ms)))

    keys = {}

    for t in timestamp_generator:
        try:
            t,key = t
            b = bucket(t)
#            print str((key,b,keys.get(key,-1),keys.get(key,-1)==b, buckets[b]))
            if key is not None:
                if keys.get(key,-1) != b:
                    keys[key] = b
                    buckets[b] += 1            
        except TypeError, e:  # t is a scalar
            b = bucket(t)
            buckets[b] += 1           

    if normalize:
        n = float(len(trace.unique_addresses))
        buckets = [v/n for v in buckets]
    
    if bucket_scalar != 1.0:
        buckets = [v*bucket_scalar for v in buckets]

    x_values = [bucket_x(i) for i in xrange(len(buckets))]
    return x_values, buckets
