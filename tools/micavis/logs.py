import os, re, sys


def read_mica_logs(logdir, order_func = None):
    events = []
    for filename in [x for x in os.listdir(logdir) if x.endswith('log')]:
        with open(filename,'r') as f:
            for line in f.xreadlines():
               event = json.loads(line)
               events.append(event)
    return events
        
    
    
