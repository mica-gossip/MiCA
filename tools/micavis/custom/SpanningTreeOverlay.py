from micavis.custom import CustomProtocol

class SpanningTreeOverlay(CustomProtocol):

    def load(self, micavis):
        self.display_options = micavis.standard_display_options.copy()
        self.display_options['edge_color'] = '#00ffa0'

    def draw_nodes(self, vis, nodes):
        edges = []
        for addr, data in nodes:
            children = data['state']['children']
            for c in children:
                edges += [(addr,c)]
        vis.draw_subgraph(edges, **self.display_options)
        
        
protocol = SpanningTreeOverlay()
