from micavis.custom import CustomProtocol

class Protocol(CustomProtocol):
    # called when loaded for the first time.  micavis is the "gui" main 
    # application instance; this can be used to create UI elements, etc.
    def load(self, micavis):
        print "verified: load TestStateMachine"

    # draw the state of an individual node
    def draw_node(self, vis, address, data):
        state = data['state']
        color = state.get('color',None)
        if color == 'RED':
            vertex_color = '#ff0000'
        elif color == 'GREEN':
            vertex_color = '#00ff00'
        else:
            vertex_color = '#ffff00'
        label = str(len(state.get('reached',[])))
        vis.draw_node(address,
                      vertex_color=vertex_color,
                      vertex_label=label,
                      vertex_label_dist = 4
                      )


protocol = Protocol()
