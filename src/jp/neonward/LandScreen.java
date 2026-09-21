package jp.neonward;
import java.util.*;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
public final class LandScreen extends Screen {
 final int plot,terminal;int token,x,y,page,selected=-1,count=1;long cash;boolean mine,pending;String owner,message,query="",category="全部";EditBox search;PhoneScreen.NeonButton confirm;final List<Integer> visible=new ArrayList<>();
 LandScreen(JsonObject o){super(Component.literal("WEST LAND"));plot=o.get("plot").getAsInt();terminal=o.get("terminal").getAsInt();receive(o);}
 public boolean isPauseScreen(){return false;}
 void receive(JsonObject o){token=o.get("token").getAsInt();cash=o.get("cash").getAsLong();mine=o.get("mine").getAsBoolean();owner=o.get("owner").getAsString();message=o.get("message").getAsString();pending=false;if(minecraft!=null)rebuildWidgets();}
 String name(int id){var p=LandCatalog.PRODUCTS.get(id);if(p.category().equals("家畜"))return switch(p.id()){case "cow"->"牛";case "pig"->"豚";default->"鶏";};return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(p.id()))).getHoverName().getString();}
 protected void init(){x=(width-350)/2;y=(height-254)/2;
  if(terminal!=0){search=new EditBox(font,x+10,y+43,220,18,Component.literal("検索"));search.setValue(query);search.setResponder(v->{query=v;page=0;refresh();});addRenderableWidget(search);
   var cats=new ArrayList<String>();cats.add("全部");for(var p:LandCatalog.PRODUCTS)if(LandCatalog.page(p,terminal)&&!cats.contains(p.category()))cats.add(p.category());addRenderableWidget(new PhoneScreen.NeonButton(x+236,y+43,104,18,category,b->{category=cats.get((cats.indexOf(category)+1)%cats.size());page=0;rebuildWidgets();}));
   for(int row=0;row<3;row++)for(int col=0;col<9;col++){final int slot=row*9+col;addRenderableWidget(new PhoneScreen.NeonButton(x+12+col*36,y+69+row*30,32,28,"",b->{if(slot<visible.size()){selected=visible.get(slot);pending=false;}}));}
   addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+162,42,18,"<",b->{page=Math.max(0,page-1);refresh();}));addRenderableWidget(new PhoneScreen.NeonButton(x+58,y+162,42,18,">",b->{page++;refresh();}));
   addRenderableWidget(new PhoneScreen.NeonButton(x+232,y+162,108,18,"数量 "+count,b->{count=terminal==2?1:count==1?16:count==16?64:1;rebuildWidgets();}));refresh();
  }
  confirm=addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+207,326,20,"表示金額で購入する",b->{if(pending||minecraft.player==null||terminal!=0&&selected<0)return;pending=true;confirm.active=false;minecraft.player.connection.sendCommand("neonland buy "+token+" "+(terminal==0?-1:selected)+" "+(terminal==0?1:count));}));
  addRenderableWidget(new PhoneScreen.NeonButton(x+12,y+231,326,18,"閉じる",b->onClose()));
 }
 void refresh(){visible.clear();var ids=new ArrayList<Integer>();for(int i=0;i<LandCatalog.PRODUCTS.size();i++){var p=LandCatalog.PRODUCTS.get(i);if(LandCatalog.page(p,terminal)&&(category.equals("全部")||category.equals(p.category()))&&(name(i).contains(query)||p.id().contains(query.toLowerCase(Locale.ROOT))))ids.add(i);}page=Math.min(page,Math.max(0,(ids.size()-1)/27));visible.addAll(ids.subList(Math.min(page*27,ids.size()),Math.min(page*27+27,ids.size())));}
 public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){g.fill(x-3,y-3,x+353,y+256,0xff08121d);g.outline(x-3,y-3,356,259,0xff5ff5ec);g.text(font,"WEST LAND "+(plot+1)+" / "+WestLand.title(terminal),x+12,y+8,0xff73fff0);g.text(font,"所有者: "+owner+" / "+cash+" Cr",x+12,y+25,0xffb5cbd0);confirm.active=!pending&&(terminal==0?owner.equals("未購入"):mine&&selected>=0);super.extractRenderState(g,mx,my,dt);
  if(terminal==0){g.text(font,"32×32マス / 20,000 Cr",x+20,y+70,0xffffd071);g.text(font,"建築可能: Y48～127 / 購入者本人のみ",x+20,y+93,0xffcbdde5);g.text(font,"家・畑など用途自由 / 道路は共有・編集不可",x+20,y+116,0xffcbdde5);g.text(font,"動物は1区画12頭まで / 武器・家具は別の店",x+20,y+139,0xffcbdde5);}
  else {for(int n=0;n<visible.size();n++){int id=visible.get(n),xx=x+12+(n%9)*36,yy=y+69+(n/9)*30;var p=LandCatalog.PRODUCTS.get(id);if(!p.category().equals("家畜"))g.item(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(p.id()))),xx+8,yy+5);else g.text(font,name(id),xx+7,yy+10,0xfff5d385);if(id==selected)g.outline(xx,yy,32,28,0xffffd071);}if(selected>=0){var p=LandCatalog.PRODUCTS.get(selected);g.text(font,font.plainSubstrByWidth(name(selected)+" ×"+count+" / "+(long)p.price()*count+" Cr",326),x+12,y+184,0xffffd071);}}
  g.text(font,font.plainSubstrByWidth(message,326),x+12,y+197,0xffabc0cb);
 }
}
