from micavis.custom import CustomProtocol
import micavis.custom as custom

class Protocol(CustomProtocol):
    def load(self, micavis):
        self.micavis = micavis
    

    def projections(self, data):
        for pjk, pjf in [('p1',self.p1),('p2',self.p2)]:
            subname = pjf(data)['stateType']
            pjk += ":" + subname
            yield pjk, pjf
            subdata = pjf(data)
            for subk, subf in custom.get_module(self.micavis, subdata).projections(subdata):
                yield ("%s/%s" % (pjk,subk), lambda s,f=pjf,sf=subf: sf(f(s)))
    
    def p1(self, data):
        return data['state']['p1']

    def p2(self, data):
        return data['state']['p2']
        
protocol = Protocol()
