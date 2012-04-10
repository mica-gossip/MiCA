#!/usr/bin/python
import sys
sys.path.insert(0,"../..")
logdir = "../../../mica_log"

from micavis.logs import *

events = read_mica_logs(logdir)

adrs = query_unique_addresses(events)

print query_select_graph(adrs, events)

