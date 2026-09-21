package jp.neonward;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Shared validation keeps rejected links in the remote instead of silently closing it. */
public final class TelevisionUrl {
 public static final int MAX_LENGTH=8192;
 public static boolean twitch(String url){try{String h=URI.create(url).getHost();return h!=null&&Set.of("twitch.tv","www.twitch.tv","m.twitch.tv","clips.twitch.tv").contains(h.toLowerCase(Locale.ROOT));}catch(Exception ex){return false;}}
 public static String normalize(String input){
  String s=input.trim();
  if(s.length()>MAX_LENGTH)throw new IllegalArgumentException("URLが長すぎます。動画・配信の共有リンクを使ってね");
  if(s.equals("off"))return "about:blank";
  if(s.equals("home"))return "https://www.youtube.com";
  URI u;
  try{u=URI.create(s);}catch(Exception ex){throw new IllegalArgumentException("URLを全文貼り付けてね");}
  String h=u.getHost()==null?"":u.getHost().toLowerCase(Locale.ROOT);
  if(h.equals("bing.com")||h.equals("www.bing.com"))throw new IllegalArgumentException("Bingの「YouTubeで見る」→「共有」のURLを貼ってね");
  if(!"https".equalsIgnoreCase(u.getScheme())||u.getUserInfo()!=null||u.getPort()!=-1||!(Set.of("youtube.com","www.youtube.com","m.youtube.com","youtu.be").contains(h)||twitch(s)))throw new IllegalArgumentException("https:// で始まるYouTubeかTwitchのURLを貼ってね");
  if(twitch(s)){
   String path=u.getPath();if(path.endsWith("/"))path=path.substring(0,path.length()-1);
   if(h.equals("clips.twitch.tv")){
    if(!path.matches("/[A-Za-z0-9_-]+")||path.equals("/embed"))throw new IllegalArgumentException("Twitchクリップの共有リンクを貼ってね");
    return "https://clips.twitch.tv"+path;
   }
   if(!(path.isEmpty()||path.matches("/[A-Za-z0-9_]{1,25}")||path.matches("/videos/[0-9]+")||path.matches("/[A-Za-z0-9_]{1,25}/clip/[A-Za-z0-9_-]+")))throw new IllegalArgumentException("Twitchの配信・動画・クリップの共有リンクを貼ってね");
   String result="https://www.twitch.tv"+path;
   if(path.startsWith("/videos/")&&u.getRawQuery()!=null)for(String pair:u.getRawQuery().split("&"))if(pair.matches("t=(?:[0-9]+h)?(?:[0-9]+m)?(?:[0-9]+s?)?")&&pair.length()>2){result+="?"+pair;break;}
   return result;
  }
  Map<String,String> q=new HashMap<>();
  try{if(u.getRawQuery()!=null)for(String pair:u.getRawQuery().split("&")){String[] p=pair.split("=",2);if(p.length==2)q.putIfAbsent(p[0],URLDecoder.decode(p[1],StandardCharsets.UTF_8));}}catch(Exception ex){throw new IllegalArgumentException("URLを全文貼り付けてね");}
  String path=u.getPath(),id=q.get("v");
  if(h.equals("youtu.be"))id=path.isEmpty()?"":path.substring(1);
  else if(path.matches("/(shorts|live|embed)/[^/]+"))id=path.substring(path.lastIndexOf('/')+1);
  if(id!=null){
   if(!id.matches("[A-Za-z0-9_-]{11}"))throw new IllegalArgumentException("動画のURLが途中で切れています。もう一度コピーしてね");
   String result="https://www.youtube.com/watch?v="+id;
   String time=q.getOrDefault("t",q.get("start"));
   if(time!=null&&time.matches("(?:\\d+h)?(?:\\d+m)?(?:\\d+s?)?")&&!time.isEmpty())result+="&t="+time;
   return result;
  }
  if(path.equals("/watch"))throw new IllegalArgumentException("動画のURLが途中で切れています。もう一度コピーしてね");
  return u.toASCIIString();
 }
}
