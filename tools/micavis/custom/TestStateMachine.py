
def draw_state(vis, address, state):
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
