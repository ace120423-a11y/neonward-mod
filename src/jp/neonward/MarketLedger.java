package jp.neonward;
import java.util.*;

/** Fictional market. Integer credits and server-owned accounts; no real securities. */
public final class MarketLedger {
 public static class LandOwner {public String uuid,name;public LandOwner(String u,String n){uuid=u;name=n;}}
 public Map<Integer,LandOwner> westLand=new HashMap<>();public int westLandColumns,westLandRepairColumns,westLandTunnelColumns,westLandPerimeterColumns;
 public static final String[] SYMBOLS={"NOVA","CHRM","PULS","GRID","MEDX","NITE"};
 public static final String[] NAMES={"ノヴァ・重工","クローム武装","パルス通信","グリッド輸送","メディックス","ナイト電力"};
 public MahjongRound mahjong=new MahjongRound();public KoiRound koi=new KoiRound();public ParlorGames.DiceRound dice=new ParlorGames.DiceRound();
 public int casinoWheel;
 public int[] casinoReels={0,0,0};public String casinoSlotResult="100 Cr / 右クリックで遊ぶ";
 public static class SlotState {public boolean active;public String owner="";public long started;public int[] offsets={0,0,0};public boolean[] stopped={true,true,true};public int[] reels={0,0,0};public String result="100 Cr / START";}
 public Map<Integer,SlotState> casinoMachines=new HashMap<>();
 public SlotState slot(int id){return casinoMachines.computeIfAbsent(id,k->new SlotState());}
 public List<PhoneServices.Entry> phoneMessages=new ArrayList<>(),phonePosts=new ArrayList<>(),phoneTransfers=new ArrayList<>();
 public static class Account {public boolean tutorialDone;public StreetProgress street=new StreetProgress();public int cityFloor;public int phoneTitle;public List<String> phoneReceipts=new ArrayList<>();public String profileName="PLAYER",profileStatus="街で暮らす冒険者";public Set<String> friends=new LinkedHashSet<>(),friendRequests=new LinkedHashSet<>();public Set<String> casinoVehicles=new HashSet<>();public CasinoGames.Hand blackjack;public int homeSlot;public boolean homeReady;public int guildRank,guildRankReports,guildExamField,guildExamTower,guildExamBoss;public boolean guildExamActive;public long guildMealDay=-1;public Map<Integer,Integer> guildQuestRanks=new HashMap<>();public Map<Integer,Integer> guildQuests=new HashMap<>();public long cash=10000;public int[] shares=new int[6];public long[] cost=new long[6];public long realized;public Map<Integer,Integer> cyberRolls=new HashMap<>();public int cyberVersion;public List<Integer> cyberPending=new ArrayList<>();public Set<Integer> cyberOwned=new HashSet<>();public Map<Integer,Integer> cyberSlots=new HashMap<>();}
 public Map<String,HousingSales.State> housingSales=new HashMap<>();
 public int nextHomeSlot=1;public long step=0;public int[] prices={120,85,65,100,150,50};
 public List<List<Integer>> history=new ArrayList<>();public Map<String,Account> accounts=new HashMap<>();
 public String news="市場オープン / 初回口座に10,000 Cr";
 public MarketLedger(){for(int p:prices)history.add(new ArrayList<>(List.of(p)));}
 public Account account(String id){return accounts.computeIfAbsent(id,k->new Account());}
 public String trade(String id,int stock,int quantity,boolean buy,int quoted){
  if(stock<0||stock>=6||quantity<1||quantity>1000)return "注文数量が不正です";
  if(prices[stock]!=quoted)return "株価が更新されました。価格を確認して再注文してください";
  Account a=account(id);long total=(long)prices[stock]*quantity;
  if(buy){if(a.cash<total)return "残高が足りません";if(a.shares[stock]>1000000-quantity)return "保有上限です";a.cash-=total;a.shares[stock]+=quantity;a.cost[stock]+=total;}
  else{if(a.shares[stock]<quantity)return "保有株が足りません";long basis=a.cost[stock]*quantity/a.shares[stock];a.shares[stock]-=quantity;a.cost[stock]-=basis;a.cash+=total;a.realized+=total-basis;}
  return SYMBOLS[stock]+" "+quantity+"株を"+(buy?"購入":"売却")+" / "+total+" Cr";
 }
 public void advance(){
  step++;Random r=new Random(0x4e454f4eL+step*7919);int featured=r.nextInt(6),event=r.nextBoolean()?1:-1;
  news=NAMES[featured]+(event>0?"：大型契約の報道":"：供給遅延の報道");
  for(int i=0;i<6;i++){int old=prices[i];double move=(r.nextDouble()-.5)*.05+(i==featured?event*.035:0);prices[i]=Math.max(5,Math.min(10000,(int)Math.round(old*(1+move))));var h=history.get(i);h.add(prices[i]);while(h.size()>60)h.removeFirst();}
 }
 public void validate(){
  if(westLand==null||westLandColumns<0||westLandColumns>216||westLandRepairColumns<0||westLandRepairColumns>216||westLandTunnelColumns<0||westLandTunnelColumns>40||westLandPerimeterColumns<0||westLandPerimeterColumns>216)throw new IllegalStateException("Invalid west land save");
  for(var e:westLand.entrySet()){if(e.getKey()<0||e.getKey()>=8||e.getValue()==null||e.getValue().name==null)throw new IllegalStateException("Invalid land owner");UUID.fromString(e.getValue().uuid);}
  var cityFloors=new HashSet<Integer>();for(var a:accounts.values())if(a.cityFloor!=0&&(a.cityFloor<2||a.cityFloor>20||!cityFloors.add(a.cityFloor)))throw new IllegalStateException("Invalid city apartment ownership");
  var houseSlots=new HashSet<Integer>();for(var a:accounts.values())if(a.homeSlot<0||a.homeSlot>=262144||a.homeSlot>0&&!houseSlots.add(a.homeSlot))throw new IllegalStateException("Invalid home ownership");
  if(prices==null||prices.length!=6||history==null||history.size()!=6||accounts==null||step<0)throw new IllegalStateException("Invalid market save");
  for(int i=0;i<6;i++)if(prices[i]<5||prices[i]>10000||history.get(i)==null||history.get(i).isEmpty()||history.get(i).size()>60)throw new IllegalStateException("Invalid prices");
  for(Account a:accounts.values()){if(a.cash<0||a.shares==null||a.cost==null||a.shares.length!=6||a.cost.length!=6)throw new IllegalStateException("Invalid account");for(int i=0;i<6;i++)if(a.shares[i]<0||a.shares[i]>1000000||a.cost[i]<0)throw new IllegalStateException("Invalid holding");}
 }
}
