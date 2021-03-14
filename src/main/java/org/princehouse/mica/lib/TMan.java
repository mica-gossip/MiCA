package org.princehouse.mica.lib;

import java.util.HashSet;
import java.util.Set;
import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.sugar.annotations.View;
import org.princehouse.mica.lib.abstractions.PeerSamplingService;

public abstract class TMan extends BaseProtocol {

  private static final long serialVersionUID = 1L;

  public PeerSamplingService peerSampler = null;

  @View
  public Set<Address> view = new HashSet<Address>();

  public TMan(PeerSamplingService peerSampler) {
    this.peerSampler = peerSampler;
  }

  @Override
  public void update(Protocol that) {
    Set<Address> buffer = new HashSet<Address>();
    Address thisDescriptor = getAddress();
    buffer.add(thisDescriptor);
    buffer.addAll(view);
    buffer.addAll(peerSampler.getRandomPeers());
  }

  public abstract Set<Address> selectView(Set<Address> buffer);

}
