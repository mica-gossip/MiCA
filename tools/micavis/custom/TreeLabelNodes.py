from micavis.custom import CustomProtocol

class Protocol(CustomProtocol):
    # draw the state of an individual node
    def get_node_label(self, vis, address, data):
        state = data['state']
        return str(state['label'])


protocol = Protocol()
