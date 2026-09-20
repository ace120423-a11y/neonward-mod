package jp.neonward;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Saved-city atlas, north-up, with live position and proportional world coordinates. */
public class CityMapScreen extends Screen {
 static final Identifier ATLAS=NeonWard.id("textures/gui/city_map.png");
 static final String[] NAMES={"マイホーム","企業タワー","中央広場","武器屋","服屋","マーケット","北門","東門","南門","西門","飲み屋横丁","車屋","カジノ","家具屋","釣り堀・農地受付","薬局・調合台","不動産屋 NEON ESTATE","診療所 NEURO CLINIC"};
 static final double[][] POI={{108,237},{84,118},{160,154},{79,480},{244,493},{241,121},{160,-16},{560,154},{160,688},{-16,154},{160,827},{334,580},{510,468},{327,600},{327,634},{347,600},{378,640},{426,440}};
 int l,t,w,h,mx,my,mw,mh,selected=-1,page,rows;double cx=272,cz=484,zoom=1,scale;boolean dragging,follow=true,slum,initialized;
 public CityMapScreen(){super(Component.literal("NEON ATLAS"));}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){minecraft.gui.setScreen(new PhoneScreen());}
 boolean privateHome(){return minecraft.player!=null&&minecraft.player.level().dimension().equals(PrivateHomes.DIMENSION);}
 boolean mapped(){return minecraft.player!=null&&(minecraft.player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)||privateHome()||minecraft.player.level().dimension()==CompactShops.DIM);}
 int shopRoom(){return minecraft.player==null?-1:CompactShops.room(minecraft.player.level(),minecraft.player.blockPosition());} double ux(){int room=shopRoom();return room>=0?CompactShops.ALL[room].x()+1:privateHome()?221:minecraft.player.getX();}double uz(){int room=shopRoom();return room>=0?CompactShops.ALL[room].z()+3:privateHome()?949:minecraft.player.getZ();}
 boolean inSlum(){return mapped()&&ux()>=0&&ux()<=300&&uz()>=704&&uz()<=1000;}
 java.util.List<MapPlaces.Place> places(){if(slum)return MapPlaces.SLUM;var list=new java.util.ArrayList<MapPlaces.Place>();for(int i=0;i<POI.length;i++)if(i!=10)list.add(new MapPlaces.Place(NAMES[i],POI[i][0],POI[i][1]));return list;}
 double localZoom(){return slum?Math.min(mw/256.0,mh/240.0)/Math.min(mw/608.0,mh/1032.0):2.0;}
 void rebuild(){clearWidgets();init();}
 void region(boolean v){slum=v;follow=false;page=0;selected=-1;cx=v?160:272;cz=v?874:340;zoom=localZoom();rebuild();}
 void locate(){follow=true;slum=inSlum();page=0;selected=-1;zoom=localZoom();rebuild();}
 @Override protected void init(){
  w=Math.min(610,width-16);h=Math.min(390,height-16);l=(width-w)/2;t=(height-h)/2;
  mx=l+10;my=t+54;mw=w-150;mh=h-90;
  int sx=l+w-132;
  if(!initialized){slum=inSlum();zoom=localZoom();initialized=true;}
  rows=Math.max(3,(h-136)/19);var points=places();int pages=Math.max(1,(points.size()+rows-1)/rows);page=Math.min(page,pages-1);
  addRenderableWidget(new PhoneScreen.NeonButton(l+10,t+28,52,18,"街中",b->region(false)));
  addRenderableWidget(new PhoneScreen.NeonButton(l+66,t+28,52,18,"スラム",b->region(true)));
  addRenderableWidget(new PhoneScreen.NeonButton(l+122,t+28,65,18,"現在地追尾",b->locate()));
  addRenderableWidget(new PhoneScreen.NeonButton(l+195,t+28,68,18,"道案内開始",b->{if(selected>=0&&selected<places().size()){StreetNavigation.start(places().get(selected));minecraft.gui.setScreen(null);}}));
  addRenderableWidget(new PhoneScreen.NeonButton(l+267,t+28,60,18,"案内を終了",b->StreetNavigation.stop()));
  for(int i=0;i<rows&&page*rows+i<points.size();i++){final int n=page*rows+i;var place=points.get(n);addRenderableWidget(new PhoneScreen.NeonButton(sx,t+54+i*19,122,17,font.plainSubstrByWidth(place.name(),114),b->{selected=n;follow=false;cx=place.x();cz=place.z();zoom=Math.max(zoom,4);}));}
  addRenderableWidget(new PhoneScreen.NeonButton(sx,t+h-80,25,17,"<",b->{page=(page+pages-1)%pages;rebuild();}));
  addRenderableWidget(new PhoneScreen.NeonButton(sx+97,t+h-80,25,17,">",b->{page=(page+1)%pages;rebuild();}));
  addRenderableWidget(new PhoneScreen.NeonButton(l+10,t+h-25,28,18,"+",b->zoom=Math.min(6,zoom*1.5)));
  addRenderableWidget(new PhoneScreen.NeonButton(l+42,t+h-25,28,18,"-",b->zoom=Math.max(1,zoom/1.5)));
  addRenderableWidget(new PhoneScreen.NeonButton(l+76,t+h-25,52,18,"全体",b->{follow=false;cx=272;cz=484;zoom=1;selected=-1;}));
  addRenderableWidget(new PhoneScreen.NeonButton(l+134,t+h-25,62,18,"現在地",b->{locate();}));
  addRenderableWidget(new PhoneScreen.NeonButton(sx,t+h-25,102,18,"ホームに戻る",b->onClose()));
 }
 int px(double x){return mx+mw/2+(int)Math.round((x-cx)*scale);}
 int py(double z){return my+mh/2+(int)Math.round((z-cz)*scale);}
 boolean inside(double x,double y){return x>=mx&&x<mx+mw&&y>=my&&y<my+mh;}
 @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){if(super.mouseClicked(e,twice))return true;if(e.button()==0&&inside(e.x(),e.y())){for(int i=0;i<places().size();i++){var p=places().get(i);if(Math.abs(e.x()-px(p.x()))<7&&Math.abs(e.y()-py(p.z()))<7){selected=i;follow=false;return true;}}dragging=true;follow=false;return true;}return false;}
 @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(dragging){cx-=dx/scale;cz-=dy/scale;return true;}return super.mouseDragged(e,dx,dy);}
 @Override public boolean mouseReleased(MouseButtonEvent e){if(e.button()==0&&dragging){dragging=false;return true;}return super.mouseReleased(e);}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
  if(follow&&mapped()){cx=ux();cz=uz();if(slum!=inSlum()){slum=inSlum();page=0;selected=-1;zoom=localZoom();rebuild();}}
  scale=Math.min(mw/608.0,mh/1032.0)*zoom;
  g.fill(l-3,t-3,l+w+3,t+h+3,0xff07101a);g.outline(l-3,t-3,w+6,h+6,0xff56e7dc);
  g.text(font,"NEON ATLAS / "+(slum?"スラム":"街中"),l+12,t+12,0xff7ffff1);
  g.text(font,(follow?"現在地を追尾中":"ドラッグで移動"),l+w-110,t+12,0xffa4c4d1);
  g.fill(mx,my,mx+mw,my+mh,0xff0a1620);
  // Clip the texture's UVs, keeping the X and Z scales identical at every zoom.
  int ax=px(-32),ay=py(-32),bx=px(576),by=py(1000);
  int a=Math.max(mx,ax),b=Math.max(my,ay),c=Math.min(mx+mw,bx),d=Math.min(my+mh,by);
  if(c>a&&d>b)g.blit(ATLAS,a,b,c,d,(float)(a-ax)/(bx-ax),(float)(c-ax)/(bx-ax),(float)(b-ay)/(by-ay),(float)(d-ay)/(by-ay));
  for(var node:StreetNavigation.path){int rx=px(node.getX()+.5),rz=py(node.getZ()+.5);if(inside(rx,rz))g.fill(rx,rz,rx+1,rz+1,0xff62fff0);}
  var spots=places();var labels=new java.util.ArrayList<int[]>();int hover=-1;
  if(mapped()){int x=px(ux()),y=py(uz());labels.add(new int[]{x-8,y-23,x+55,y+12});}
  var order=new java.util.ArrayList<Integer>();if(selected>=0&&selected<spots.size())order.add(selected);for(int i=0;i<spots.size();i++)if(i!=selected)order.add(i);
  for(int i:order){var place=spots.get(i);int x=px(place.x()),y=py(place.z());if(!inside(x-5,y-5)||!inside(x+6,y+6))continue;
   int color=place.name().contains("ホーム")?0xffff66ac:slum&&i>=5?0xffffb751:0xff74f2e5;
   if(slum&&i>=5){g.fill(x-2,y-2,x+3,y+3,0xff08111b);g.fill(x-1,y-1,x+2,y+2,color);}else{g.fill(x-4,y-4,x+5,y+5,0xee08111b);g.outline(x-4,y-4,9,9,i==selected?0xffffffff:color);g.fill(x-1,y-1,x+2,y+2,color);}
   if(Math.abs(mouseX-x)<7&&Math.abs(mouseY-y)<7)hover=i;
   if(slum&&i>=5&&i!=selected)continue;
   String name=place.name();int tw=font.width(name);if(tw>mw-6)continue;
   int xx=Math.max(mx+2,Math.min(x+6,mx+mw-tw-3)),yy=Math.max(my+2,y-11);boolean overlap=false;
   for(var r:labels)if(xx<r[2]&&xx+tw+3>r[0]&&yy<r[3]&&yy+11>r[1]){overlap=true;break;}
   if(!overlap){g.fill(xx-1,yy-1,xx+tw+2,yy+10,0xe608111b);g.text(font,name,xx,yy,color);labels.add(new int[]{xx-2,yy-2,xx+tw+4,yy+12});}
  }
  int active=hover>=0?hover:selected;
  if(active>=0&&active<spots.size()){var dest=spots.get(active);String label=dest.name()+" / X "+(int)dest.x()+" Z "+(int)dest.z();if(mapped())label+=" / 約"+(int)Math.hypot(dest.x()-ux(),dest.z()-uz())+"m";g.fill(mx+2,my+mh-15,mx+mw-2,my+mh-2,0xee07101a);g.text(font,font.plainSubstrByWidth(label,mw-8),mx+4,my+mh-12,0xffffffff);}
  if(minecraft.player!=null){var p=minecraft.player;
   if(mapped()){
    int x=Math.clamp(px(ux()),mx+9,mx+mw-10),y=Math.clamp(py(uz()),my+23,my+mh-24);boolean visible=inside(px(ux()),py(uz()));
    g.fill(x-5,y-5,x+6,y+6,0xff11131a);g.outline(x-6,y-6,13,13,0xffffff63);g.fill(x-2,y-2,x+3,y+3,0xffffff63);
    double r=Math.toRadians(p.getYRot());for(int k=4;k<11;k++){int u=x-(int)(Math.sin(r)*k),v=y+(int)(Math.cos(r)*k);if(inside(u,v))g.fill(u,v,u+2,v+2,0xffffff63);}
    String label=visible?"現在地":"現在地は画面外";int xx=Math.clamp(x-12,mx+2,Math.max(mx+2,mx+mw-font.width(label)-2));g.fill(xx-1,y-19,xx+font.width(label)+1,y-8,0xee11131a);g.text(font,label,xx,y-18,0xffffff63);
   }
   String status=privateHome()?"自室内 / 入口を表示":slum&&p.getY()<60?"地下 B1 / 地上位置":!mapped()?"この次元は対象外":"黄：現在地・向き";
   g.text(font,status,l+w-132,t+h-58,0xffffff63);
   g.text(font,String.format("X %.0f / Z %.0f",mapped()?ux():p.getX(),mapped()?uz():p.getZ()),l+w-132,t+h-44,0xffe9edaa);
  }
  g.text(font,"N ↑",mx+6,my+6,0xffffffff);g.outline(mx,my,mw,mh,0xff346571);
  g.centeredText(font,(page+1)+" / "+Math.max(1,(places().size()+rows-1)/rows),l+w-71,t+h-75,0xffa4c4d1);

  super.extractRenderState(g,mouseX,mouseY,delta);
 }
}

