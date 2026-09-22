package jp.neonward;
import com.google.gson.JsonObject;
/** Old snapshot compatibility; never exposes the retired farm purchase button. */
public class FarmScreen extends PhoneAppScreen {
 public FarmScreen(){super(8);page=2;}
 @Override void receive(JsonObject packet){if(packet.has("app")){super.receive(packet);return;}notice="個別農地は廃止しました。西側の土地をご利用ください";request("view");}
}
