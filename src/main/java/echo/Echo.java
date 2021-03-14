package echo;

import org.princehouse.mica.base.BaseProtocol;
import org.princehouse.mica.base.LogFlag;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.sugar.annotations.View;
import org.princehouse.mica.lib.abstractions.Broadcast;
import org.princehouse.mica.lib.abstractions.Overlay;

public class Echo extends BaseProtocol implements Broadcast<String> {

  private static final long serialVersionUID = 1L;

  @View
  public Overlay overlay;

  public volatile String message;

  public Echo(Overlay overlay) {
    this.overlay = overlay;
    this.message = "hello from " + getAddress().toString();
  }

  @Override
  public void update(Protocol that) {
    try {
      Echo other = (Echo) that;
      receiveMessage("pull " + other.message);
      other.receiveMessage("push " + this.message);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  @Override
  public void sendMessage(String m) {
    this.message = m;
  }

  @Override
  public void receiveMessage(String m) {
    logJson(LogFlag.user, "echo-receive", m);
  }
}
