from micavis.custom import CustomProtocol

class FindMinChattyProtocol(CustomProtocol):
    def get_node_label(self, vis, address, data):
        state = data['state']
        value = state.get('value',"?")
        return str(value)

protocol = FindMinChattyProtocol()
