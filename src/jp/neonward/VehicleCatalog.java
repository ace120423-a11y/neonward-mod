package jp.neonward;
import java.util.*;
public final class VehicleCatalog {
 public record Spec(String id,String name,boolean bike,double top,double acceleration,int seats,int rows,String style){}
 public static final List<Spec> ALL=List.of(
  new Spec("car","GRID COUPE",false,.78,.021,2,2,"クーペ"),
  new Spec("razor","RAZOR GT",false,1.10,.031,2,1,"スポーツカー"),
  new Spec("executive","ONYX EXEC",false,.72,.020,4,3,"高級セダン"),
  new Spec("bulwark","BULWARK",false,.54,.012,4,4,"装甲SUV"),
  new Spec("hauler","RUST HAULER",false,.48,.011,2,6,"貨物トラック"),
  new Spec("nomad","NOMAD",false,.65,.018,4,5,"旅用ワゴン"),
  new Spec("bike","NIGHT RUNNER",true,.88,.030,1,1,"ストリートバイク"),
  new Spec("volt","VOLT RR",true,1.15,.038,1,1,"レーサー"),
  new Spec("chopper","IRON JACK",true,.76,.021,1,1,"アメリカン・チョッパー"),
  new Spec("cruiser","DUSK CRUISER",true,.69,.019,2,3,"アメリカン・ツーリング"),
  new Spec("scrambler","DUST HOUND",true,.80,.029,1,2,"スクランブラー"),
  new Spec("courier","BOX MULE",true,.58,.023,1,4,"配送バイク"),
  new Spec("duck","QUACK JACK",false,.66,.023,4,3,"限定・巨大アヒルカー"),
  new Spec("teapot","BOILER POT",false,.55,.016,2,5,"限定・蒸気ケトルカー"),
  new Spec("gyro","LUCKY GYRO",true,.96,.034,1,1,"限定・一輪風ジャイロ"),
  new Spec("coffin","GRAVE RIDER",true,.82,.024,1,3,"限定・棺桶チョッパー"));
 public static Spec get(String id){return ALL.stream().filter(s->s.id().equals(id)).findFirst().orElse(null);}
}
