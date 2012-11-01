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
    
    node_states = logs.CurrentValueTracker(
        micavis.events, 
        filter_func = lambda e: e['event_type'].startswith('mica-state-') and 'state' in e['data'],
        value_func = lambda e: (0, int(e['timestamp'])))

    for i,k,t in node_states.enumerate():
        buckets[bucket(t)] += 1
        
    x = range(len(buckets))
    y = buckets
    print "x", x
    print "y", y
    return x,y
