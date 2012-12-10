from micavis.custom import CustomProtocol
import micavis.custom as custom

class Protocol(CustomProtocol):
    def load(self, micavis):
        self.micavis = micavis

    def projections(self, data):
        for pjk, pjf in [('p',self.p)]:
            subname = pjf(data)['stateType']
            pjk += ":" + subname
            yield pjk, pjf
            subdata = pjf(data)
            for subk, subf in custom.get_module(self.micavis, subdata).projections(subdata):
                yield ("%s/%s" % (pjk,subk), lambda s,f=pjf,sf=subf: sf(f(s)))
    
    def p(self, data):
        return data['state']['p']
        
protocol = Protocol()
