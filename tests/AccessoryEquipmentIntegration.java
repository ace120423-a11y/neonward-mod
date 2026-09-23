package jp.neonward;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;

/** Real attachment/menu/engine tests in parent's disposable server only; no launch or registrations. */
public final class AccessoryEquipmentIntegration {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError("ACCESSORY: "+why);}
    static ServerPlayer probe(ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"AccessoryQA");
        var p=new ServerPlayer(level.getServer(),level,profile,ClientInformation.createDefault()){@Override public boolean hasDisconnected(){return false;}};
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public boolean hasClientLoaded(){return true;}
        };
        p.setGameMode(GameType.SURVIVAL);p.setPos(-174.5,71,368.5);return p;
    }
    static int equippedCount(ServerPlayer p){int n=0;for(int i=0;i<AccessoryEquipment.SLOTS;i++)n+=AccessoryEquipment.get(p,i).getCount();return n;}
    static int total(ServerPlayer p,AccessoryMenu menu){int n=equippedCount(p)+menu.getCarried().getCount();for(int i=0;i<36;i++)n+=p.getInventory().getItem(i).getCount();return n;}
    static void emptyInventory(ServerPlayer p){for(int i=0;i<36;i++)p.getInventory().setItem(i,ItemStack.EMPTY);}
    static List<Integer> accessorySlots(ServerPlayer p,AccessoryMenu menu){var result=new ArrayList<Integer>();for(int i=0;i<menu.slots.size();i++)if(menu.slots.get(i).container!=p.getInventory())result.add(i);check(result.size()==3,"exactly three non-player menu slots");return result;}
    static int inventorySlot(ServerPlayer p,AccessoryMenu menu,int slot){for(int i=0;i<menu.slots.size();i++)if(menu.slots.get(i).container==p.getInventory()&&menu.slots.get(i).getContainerSlot()==slot)return i;throw new AssertionError("Missing inventory menu slot "+slot);}
    static ItemStack named(int kind,int count){var s=new ItemStack(ShrineServices.AMULETS[kind],count);s.set(DataComponents.CUSTOM_NAME,Component.literal("QA component-preserving charm "+kind));return s;}
    static AccessoryMenu serverMenu(ServerPlayer p,int id){return new AccessoryMenu(id,p.getInventory(),new AccessoryEquipment.EquippedContainer(p));}
    static boolean equipHeld(ServerPlayer p,ItemStack stack){p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);return AccessoryEquipment.equip(p,p.getMainHandItem());}
    static void same(ItemStack a,ItemStack b,String why){check(a.getCount()==b.getCount()&&ItemStack.isSameItemSameComponents(a,b),why);}
    static void removeAll(ServerPlayer p,AccessoryMenu menu){emptyInventory(p);for(int slot:accessorySlots(p,menu))if(menu.slots.get(slot).hasItem())check(!menu.quickMoveStack(p,slot).isEmpty(),"unequip through real quickMove");check(equippedCount(p)==0,"all accessory slots empty");emptyInventory(p);}
    static void persistence(ServerPlayer p){
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());p.saveWithoutId(out);
        var loaded=probe(p.level());loaded.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),out.buildResult()));
        for(int i=0;i<3;i++)same(AccessoryEquipment.get(p,i),AccessoryEquipment.get(loaded,i),"entity NBT attachment roundtrip slot "+i);
        var respawn=probe(p.level());respawn.restoreFrom(p,false);
        // Fabric attachments transfer on AFTER_RESPAWN, not inside vanilla restoreFrom.
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(p,respawn,false);
        for(int i=0;i<3;i++)same(AccessoryEquipment.get(p,i),AccessoryEquipment.get(respawn,i),"real restoreFrom death copy slot "+i);
        // Mutating a respawned slot via its real menu must not mutate the old player's attachment.
        var menu=serverMenu(respawn,82);var slots=accessorySlots(respawn,menu);emptyInventory(respawn);
        check(!menu.quickMoveStack(respawn,slots.getFirst()).isEmpty(),"respawn accessory removable");
        check(equippedCount(p)==3&&equippedCount(respawn)==2,"death copy attachments are independent");
    }
    public static int run(ServerPlayer parent){
        checks=0;var p=probe(parent.level());var menu=serverMenu(p,81);
        check(AccessoryEquipment.SLOTS==3&&AccessoryEquipment.MENU!=null,"registered three-slot accessory menu");
        var slots=accessorySlots(p,menu);check(menu.slots.size()==39,"three accessory plus36 inventory slots");
        check(menu.stillValid(p),"accessory menu valid for owner");
        var initialEffects=List.of(new MobEffectInstance(MobEffects.SPEED,1200,2),new MobEffectInstance(MobEffects.RESISTANCE,1300,1),new MobEffectInstance(MobEffects.LUCK,1400,3));
        for(var effect:initialEffects)p.addEffect(new MobEffectInstance(effect));
        double speed=p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED),luck=p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.LUCK);
        for(int kind=0;kind<3;kind++){
            var stack=named(kind,4);check(AccessoryEquipment.kind(stack)==kind,"registered accessory kind "+kind);
            check(equipHeld(p,stack)&&stack.getCount()==3&&AccessoryEquipment.equipped(p,kind),"equip moves exactly one "+kind);
            same(AccessoryEquipment.get(p,kind),named(kind,1),"full ItemStack component retained "+kind);
            check(!AccessoryEquipment.equip(p,stack)&&stack.getCount()==3,"duplicate kind neither consumed nor stacked");
        }
        check(equippedCount(p)==3,"all three slots occupied once");
        check(Math.abs(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-speed*1.2)<.000001,"equipped travel adds20percent speed alongside potion");
        check(Math.abs(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.LUCK)-luck-1)<.000001,"equipped fortune adds1luck alongside potion");
        var weapon=new ItemStack(Items.DIAMOND_SWORD);check(AccessoryEquipment.kind(weapon)<0&&!AccessoryEquipment.equip(p,weapon)&&weapon.getCount()==1,"weapon rejected without loss");
        persistence(p);
        menu=serverMenu(p,83);slots=accessorySlots(p,menu);
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
        int full=total(p,menu);for(int slot:slots)check(menu.quickMoveStack(p,slot).isEmpty()&&total(p,menu)==full,"full inventory unequip leaves every item in place");
        removeAll(p,menu);
        check(Math.abs(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-speed)<.000001&&Math.abs(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.LUCK)-luck)<.000001,"unequip removes only accessory attributes");
        for(var effect:initialEffects){var actual=p.getEffect(effect.getEffect());check(actual!=null&&actual.getDuration()==effect.getDuration()&&actual.getAmplifier()==effect.getAmplifier(),"equip/unequip preserves unrelated potion buff");}
        p.removeAllEffects();
        // Vanilla pickup/cursor placement takes one, keeps the rest, and refuses a duplicate second slot.
        p.getInventory().setItem(0,named(0,2));int source=inventorySlot(p,menu,0);
        menu.clicked(source,0,ContainerInput.PICKUP,p);check(menu.getCarried().getCount()==2&&total(p,menu)==2,"pickup to vanilla cursor");
        menu.clicked(slots.get(0),0,ContainerInput.PICKUP,p);check(menu.getCarried().getCount()==1&&equippedCount(p)==1&&total(p,menu)==2,"cursor inserts one accessory");
        menu.clicked(slots.get(1),0,ContainerInput.PICKUP,p);check(menu.getCarried().getCount()==1&&equippedCount(p)==1&&total(p,menu)==2,"cursor duplicate refused");
        menu.removed(p);check(menu.getCarried().isEmpty()&&total(p,menu)==2,"closing menu returns carried item without loss");
        removeAll(p,menu);
        p.getInventory().setItem(0,named(1,3));check(!menu.quickMoveStack(p,source).isEmpty()&&p.getInventory().getItem(0).getCount()==2&&equippedCount(p)==1,"inventory quickMove equips one");
        check(menu.quickMoveStack(p,source).isEmpty()&&total(p,menu)==3,"duplicate quickMove preserves remainder");
        removeAll(p,menu);
        p.getInventory().setItem(0,weapon);menu.clicked(source,0,ContainerInput.PICKUP,p);menu.clicked(slots.get(0),0,ContainerInput.PICKUP,p);
        check(menu.getCarried().is(Items.DIAMOND_SWORD)&&equippedCount(p)==0&&total(p,menu)==1,"menu also rejects weapon cursor");menu.removed(p);emptyInventory(p);
        // Exact installed damage hook, no armor, guard, cyberware, or potion resistance.
        check(Cyberware.defense(p)==0&&p.getArmorValue()==0,"unequipped defense baseline");p.invulnerableTime=0;p.setHealth(20);float before=p.getHealth();
        check(p.hurtServer(p.level(),p.damageSources().generic(),5),"baseline incoming hit");float baseline=before-p.getHealth();
        var guard=named(1,1);check(equipHeld(p,guard),"guard accessory equipped for damage test");p.invulnerableTime=0;p.setHealth(20);before=p.getHealth();
        check(p.hurtServer(p.level(),p.damageSources().generic(),5),"accessory incoming hit");float reduced=before-p.getHealth();
        check(Math.abs(baseline-5)<.0001&&Math.abs(reduced-baseline*.8)<.0001,"actual guard damage5->4 (20% reduction)");
        menu=serverMenu(p,84);removeAll(p,menu);p.invulnerableTime=0;p.setHealth(20);before=p.getHealth();check(p.hurtServer(p.level(),p.damageSources().generic(),5)&&Math.abs(before-p.getHealth()-baseline)<.0001,"unequip immediately removes accessory defense");
        System.out.println("ACCESSORY_EQUIPMENT_PASS checks="+checks+" three slots, components, entity serialization/death copy, real cursor/quickMove, no item loss, unrelated buffs, actual defense5->4->5");return checks;
    }
}
