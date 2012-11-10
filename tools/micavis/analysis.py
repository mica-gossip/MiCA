from math import *
import logs

# return x, y lists for plotting
#  x = round bucket number
#  y = number of changes in bucket
def compute_changes_per_round(micavis):
    round_ms = micavis.runtime_info.round_ms
    start_t = micavis.runtime_info.first_timestamp
    end_t = micavis.runtime_info.last_timestamp
    buckets = [0] * int(ceil(float(end_t - start_t) / round_ms))

    def bucket(timestamp):
        return int(floor((timestamp - start_t) / float(round_ms)))

    values = {}
    node_states = logs.CurrentValueTracker(
            micavis.events, 
            filter_func =  logs.state_event_filter,
            value_func = lambda e,mv=micavis: (e['address'], (e['timestamp'],mv.project(e['data']))) )
    
    for i,addr,(t,data) in node_states.enumerate(yield_events=True):
        if addr not in values or values[addr] != data:
            values[addr] = data
            buckets[bucket(t)] += 1            
    
    n = float(len(micavis.unique_addresses))
    buckets = [v/n for v in buckets] # normalize

    x = range(len(buckets))
    y = buckets
    print "x", x
    print "y", y
    return x,y
