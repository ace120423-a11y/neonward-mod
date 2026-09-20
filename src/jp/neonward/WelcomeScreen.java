package jp.neonward;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
public final class WelcomeScreen extends Screen {
 static final String[] TITLES={"NEON WARDへようこそ","スマホと道案内","仕事と買い物","街の外で戦う","自分だけの暮らし"};
 static final String[][] TEXT={
 {"ネオンの街で暮らす、サバイバル生活。","街中とスラムは敵の出ない安全地区です。","建物は保護されています。まずは街を歩こう。"},
 {"スマホは専用枠に固定されています。","初期設定では P キーで開けます。","マップで施設を選び「道案内開始」。","Tab：右上の案内切替 / F8：プレイヤー一覧"},
 {"最初は企業タワーの受付へ。","簡単なクエストを受け、素材を換金できます。","店員に右クリックで買い物やサービスを利用。","クエストをこなして昇級試験に挑もう。"},
 {"城壁の外には敵、塔にはダンジョンがあります。","武器と回復アイテムを準備してから出発しよう。","銃は右クリック長押しで構え、左クリックで射撃。","強さはサイバーウェアで、服は見た目を楽しもう。"},
 {"不動産屋で自分の家を購入できます。","自室では家具を置けます。建物や水回りは固定。","スマホから車やバイクを呼び出せます。","迷ったら /tutorial でこの案内を見直せます。"}};
 int page,x,y,w,h;
 public WelcomeScreen(){super(Component.literal("WELCOME / NEON WARD"));}
 public boolean isPauseScreen(){return false;}
 protected void init(){w=Math.min(400,width-16);h=Math.min(220,height-16);x=(width-w)/2;y=(height-h)/2;
  addRenderableWidget(new PhoneScreen.NeonButton(x+10,y+h-28,70,18,"戻る",b->{if(page>0){page--;rebuildWidgets();}}));
  addRenderableWidget(new PhoneScreen.NeonButton(x+w-100,y+h-28,90,18,page==4?"街で暮らす":"次へ",b->{if(page<4){page++;rebuildWidgets();}else finish();}));
  addRenderableWidget(new PhoneScreen.NeonButton(x+w-94,y+10,84,18,"案内をスキップ",b->finish()));
 }
 void finish(){if(minecraft.player!=null)minecraft.player.connection.sendCommand("tutorial done");minecraft.gui.setScreen(null);}
 public void onClose(){finish();}
 public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){g.fill(x,y,x+w,y+h,0xf5091420);g.outline(x,y,w,h,0xff50fff0);g.fill(x,y,x+4,y+h,0xffee40b7);g.text(font,"FIRST STEPS / "+(page+1)+" / 5",x+12,y+15,0xff66fff0);g.text(font,TITLES[page],x+14,y+45,0xffff65cb);int yy=y+68;
  for(String line:TEXT[page]){for(var part:font.split(Component.literal(line),w-28)){g.text(font,part,x+14,yy,0xffd6e9ef);yy+=12;}yy+=4;}
  super.extractRenderState(g,mx,my,dt);
 }
}
