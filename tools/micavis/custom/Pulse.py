from micavis.custom import CustomProtocol
import micavis.logs as logs

class Protocol(CustomProtocol):
    def load(self, micavis):
        # append analysis menu item
        self.micavis = micavis
        micavis.add_analysis("Pulse: Pulse frequency over time", self.graph_pulses)
        
    def graph_pulses(self, widget):
        buckets, keyf = logs.round_bucketer(self.micavis)
        pulse_tracker = logs.CurrentValueTracker(
            self.micavis.events,
            filter_func = logs.EVENTS_FILTER_EVENTTYPE("pulse-pulse"),
            value_func = lambda e: (keyf(e), True),
            value_equality_func = lambda a,b: False)
        for i, key, value in pulse_tracker.enumerate():
            buckets[key] += 1
            
        x = range(len(buckets))
        y = buckets
        
        self.micavis.graph_window.analysis_plot_2d(x,y,
         xlabel =  "MiCA Rounds (%s ms)" % self.micavis.runtime_info.round_ms,
         ylabel = "Pulses per round")

    # draw the state of an individual node
    def get_node_label(self, vis, address, data):
        state = data['state']
        nreached = len(state.get('reached',[]))
        sname = state.get('state','???')
        roundn = state.get('round',-1)
        return "%s:%s = %s" % (roundn,nreached,sname)


protocol = Protocol()
