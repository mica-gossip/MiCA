from micavis.custom import CustomProtocol

class MinAddressProtocol(CustomProtocol):
    def get_node_label(self, vis, address, data):
        state = data['state']
        leader = state.get('leader',"???")
        return "L=%s" % leader[-4:]

protocol = MinAddressProtocol()
