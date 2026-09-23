package jp.neonward;

import java.nio.file.*;
import java.util.*;
import com.google.gson.JsonObject;

/** Offline catalog/attempt-ledger checks; no server or world is created. */
public final class EateryDecorTest {
    static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        var themes = new HashSet<String>();
        for(var site:ExistingEateries.ALL) {
            var theme=EateryDecor.theme(site.name());
            themes.add(theme.color()+":"+theme.appliance());
            check(!theme.menu().isBlank(),"named menu for "+site.name());
        }
        check(themes.size()==10,"ten cuisine/name-specific themes");
        check(EateryDecor.theme("焼鳥 炭火屋 [3]").appliance().equals("campfire"),"yakitori grill");
        check(EateryDecor.theme("餃子とビール [4]").appliance().equals("iron_trapdoor"),"gyoza iron plate");
        check(EateryDecor.theme("もつ煮 三番地 [9]").appliance().equals("cauldron"),"stew pot");
        var read=EateryDecor.class.getDeclaredMethod("read",Path.class);read.setAccessible(true);
        var save=EateryDecor.class.getDeclaredMethod("save",Path.class,JsonObject.class);save.setAccessible(true);
        Path file=Files.createTempDirectory(Path.of("."),"eatery-decor-test-").resolve("eatery_decor.json");
        check(((JsonObject)read.invoke(null,file)).size()==0,"new ledger");
        var records=new JsonObject();
        for(int i=0;i<3;i++){var entry=new JsonObject();entry.addProperty("status",new String[]{"reserved","installed","skipped"}[i]);records.add(Integer.toString(i),entry);}
        save.invoke(null,file,records);
        check(records.equals(read.invoke(null,file)),"all terminal/attempt markers survive reload");
        var before=Files.readString(file);
        save.invoke(null,file,records);
        check(before.equals(Files.readString(file)),"stable repeated persistence");
        records.getAsJsonObject("0").addProperty("status","unknown");save.invoke(null,file,records);
        boolean rejected=false;
        try{read.invoke(null,file);}catch(java.lang.reflect.InvocationTargetException ex){rejected=ex.getCause() instanceof IllegalArgumentException;}
        check(rejected,"unknown ledger states fail closed");
        System.out.println("EATERY_DECOR_PASS: 50-site catalog, ten distinct themes, cuisine appliances, durable attempt markers, stable writes, invalid-state rejection");
    }
}
