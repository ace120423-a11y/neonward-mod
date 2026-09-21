package jp.neonward;

import java.util.Set;

/** Per-connection limiter; called only on the server thread. Never grants permissions. */
public final class UiCommandLimiter {
 private static final Set<String> ROOTS=Set.of("neongacha","neonarms","neonslot","neoncasino",
  "neontravel","neonfish","neoncyber","neonfarm","neonfashion","neonfriends","neongarage","neonmotor",
  "neonguild","neongun","neonhome","interiors","neonlift","neonmed","neonphone",
  "parlor","neoncall","neonmarket","neontv","interiortv","neonvend","tutorial","underworld");
 private long nextUi, nextGun;
 public static boolean isUi(String command){
  int space=command.indexOf(' ');
  return ROOTS.contains(space<0?command:command.substring(0,space));
 }
 public boolean accept(String command,long now){
  if(!isUi(command))return false;
  boolean gun=command.equals("neongun fire") || command.startsWith("neongun fire ");
  if(now<(gun?nextGun:nextUi))return false;
  if(gun)nextGun=now+100_000_000L;
  else nextUi=now+150_000_000L;
  return true;
 }
}
