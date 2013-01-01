import logs, analysis, custom, os, sys
import micavis.ipython as ipython
import matplotlib.pyplot as plt
import numpy as np

# fields created on initialization:
#
#   events : a list of events
#   projections : list of all projections
#   leaf_projections : list of leaf projections
#   current_node_state : state tracker (projection-aware)
#   current_node_view : view tracker (projection-aware)
# 
# functions:
#   set_projection(proj) 

class MicaTrace(object):
    
    # ignore. grandfathered in from gui for custom loading
    standard_display_options = dict(
        vertex_color = "#cccccc",
        vertex_size = 10,
        vertex_label_dist = 3
        )

    @classmethod
    def load(cls, log_location = None):
        if log_location is None:
            pydir = os.path.split(__file__)[0]
            micadir = os.path.abspath(pydir + "/../..")
            log_location = micadir + "/mica_log"
        return cls(log_location)

    def __init__(self, log_location):
        self.temp_analysis_menus = [] # used for graph window initialization
        custom.init(self)
        self.events = logs.read_mica_logs(log_location)
        print "Read %s log events" % len(self.events)

        # initialize event processing (one-time analysis on events)
        self.process_events()
        self.init_trackers()

    # note: doesn't update the display or menu; see set_projection_gui
    def set_projection(self, p):
        self.projection = p
        self.current_node_state.reset()
        self.current_node_view.reset()

    def get_projection(self):
        return self.projection

    def init_trackers(self):
        self.current_node_state = logs.CurrentValueTracker(
            self.events, 
            filter_func =  logs.state_event_filter,
            value_func = lambda e,mv=self: (e['address'],mv.project(e['data'])) )

        def view_value_func(data):
            # if view is empty, logging will omit it 
            return data.get('view',{})

        self.current_node_view = logs.CurrentValueTracker(
            self.events,
            filter_func = logs.view_event_filter,
            value_func = lambda e: (e['address'],
                                    view_value_func(self.project(e['data']))))

    # called on initialization to analyze events
    def process_events(self):
        self.unique_addresses = logs.query_unique_addresses(self.events)
        print " ... %s unique addresses" % len(self.unique_addresses)
        self.init_projections()
        self.runtime_info = logs.RuntimeInfoParser(self.events)

    def init_projections(self):
        # check for all the projections used
        print "Identify projections..."
        self.projection = 'root'
        self.projections = set([
            'root' 
            ])
        for e in self.events:
            if not logs.state_event_filter(e):
                continue
            data = e.get('data',None)
            if not data:
                continue
            if 'state' not in data:
                continue
            protocolName = data['stateType']
            module = custom.load(self,protocolName)
            if not module:
                continue
            for name, namefunc in module.projections(data):
                self.projections.add(name)
        self.projections.remove('root')
        self.projections = sorted(list(self.projections))
        self.projections = ['root'] + self.projections

        def compute_leaves(projections):
            temp = projections[:]
            temp.remove('root')
            temp.sort(key=lambda x: -len(x))
            lv = []
            def is_prefix(s):
                for s2 in lv:
                    if s2.startswith(s):
                        return True
                return False

            for t in temp:
                if not is_prefix(t):
                    lv += [t]
            return lv
                
        self.leaf_projections = compute_leaves(self.projections)


    def plot_state_change_graph(self):
        x,y = analysis.compute_changes_per_round(self)
        xlabel =  "Round"
        ylabel = "State changes per node per round"
        self.plot_2d(x,y, xlabel, ylabel)

    def plot_gossip_rate(self):
        sequences = analysis.gossip_events(self.events)
        for i in xrange(len(sequences)):
            # don't want to bucket by address
            sequences[i] = [t for t,src,dst in sequences[i]]

        legend = self.leaf_projections
        
        scalar = 1./len(self.unique_addresses)
        xyvals = [analysis.frequency_count(self, 
                                           sequence, 
                                           subdivisions=5, 
                                           bucket_scalar = scalar)  
                  for sequence in sequences]

        ylabel = "Fraction of nodes initiating gossip"
        xlabel = "Time (rounds)"
        ipython.analysis_plot_2d_multiple(xyvals, xlabel, ylabel, legend)

    def plot_notable_events(self, ne_suffix):
        import matplotlib.pyplot as plt
        import numpy as np

        ne_categories = {}  # key -> sequence

        for key, timestamp, address in self.notable_events(ne_suffix):
            if key not in ne_categories:
                ne_categories[key] = []
            ne_categories[key].append(timestamp)
        
        notable_sequences = sorted(ne_categories.items())

        xlabel =  "Round"
        ylabel = "Rate (%s events per round)" % ne_suffix
        legend_labels = []
        xyvals = []

        for key, sequence in notable_sequences:
            legend_labels += [key]
            xy = analysis.frequency_count(self, sequence, subdivisions=5)
            xyvals += [xy]

        ipython.analysis_plot_2d_multiple(xyvals, xlabel, ylabel, legend_labels)

    def plot_notable_events_histogram(self, ne_suffix, normalize=False):
        import matplotlib.pyplot as plt
        import numpy as np
        import analysis

        ne = {}  # key -> sequence

        ms_per_round = self.runtime_info.round_ms
        def round(ms):
            return float(ms)/ms_per_round

        for key, timestamp, address in self.notable_events(ne_suffix):
            if key not in ne:
                ne[key] = {}
            if address not in ne[key]:
                ne[key][address] = []
            ne[key][address].append(round(timestamp))

        collated = {}
        # fashion histograms
        for key, kdic in ne.items():
            for addr, seq in kdic.items():
                deltas = list(analysis.deltas(seq))
                if key not in collated:
                    collated[key] = []
                collated[key] += deltas
        
        for key,seq in collated.items():
            collated[key] = np.array(seq)

        maxval = max([d.max() for d in collated.itervalues()]) 
        minval = min([d.min() for d in collated.itervalues()]) 

        rng = maxval - minval

        nbins = 100

        collated = sorted(collated.items())

        xlabel =  "Interval length between %s events (rounds)" % ne_suffix
        ylabel = "Fraction of intervals"

        if normalize:
            legend_labels = ["%s" % k for k,v in collated]            
        else:
            legend_labels = ["%s (%s total)" % (k,len(v)) for k,v in collated]


        datasets = [v for k,v in collated]

        # manually layout bins so they're the same for all...
        binsize = (maxval-minval) / float(nbins)
        nbins = [minval + i*binsize for i in xrange(nbins+1)]

        ipython.analysis_plot_hist_multiple(datasets, 
                                         xlabel=xlabel, ylabel=ylabel,
                                         legends=legend_labels, nbins=nbins, normalize=normalize)



    def plot_state_change_graph_leaves(self):
        import matplotlib.pyplot as plt
        import numpy as np
        import analysis

        saved_projection = self.get_projection()

        n = len(self.unique_addresses)
        xyvals = []
        for leaf_projection in self.leaf_projections:
            self.set_projection(leaf_projection)
            xyvals += [analysis.compute_changes_per_round(self, subdivisions=1, bucket_scalar = 1./n)]

        # restore previous projection
        self.set_projection(saved_projection)
        xlabel =  "MiCA Rounds (%s ms)" % self.runtime_info.round_ms
        ylabel = "Fraction of nodes changed state"
        legend_labels = self.leaf_projections
        ipython.analysis_plot_2d_multiple(xyvals, xlabel, ylabel, legend_labels)


    def plot_2d(self, x, y, xlabel="x", ylabel = "y"):
        ipython.analysis_plot_2d_multiple([(x,y)], xlabel, ylabel, )

    def add_analysis(self, label, callback):
        # grandfathered-in feature from the GUI, called by custom modules
        # ignore, it will be overridden by the GUI
        pass
