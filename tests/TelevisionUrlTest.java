package jp.neonward;
public class TelevisionUrlTest {
 static void eq(String in,String expected){String actual=TelevisionUrl.normalize(in);if(!expected.equals(actual))throw new AssertionError(in+" -> "+actual);}
 static void reject(String in){try{TelevisionUrl.normalize(in);}catch(IllegalArgumentException ok){return;}throw new AssertionError("accepted: "+in);}
 public static void main(String[] args){
  eq("https://m.twitch.tv/twitchdev/?tt_content=channel","https://www.twitch.tv/twitchdev");
  eq("https://www.twitch.tv/videos/123456?t=1h2m3s&tracking=x","https://www.twitch.tv/videos/123456?t=1h2m3s");
  eq("https://www.twitch.tv/twitchdev/clip/Example-Clip_123?x=y","https://www.twitch.tv/twitchdev/clip/Example-Clip_123");
  eq("https://clips.twitch.tv/Example-Clip_123?x=y","https://clips.twitch.tv/Example-Clip_123");
  eq("https://www.twitch.tv","https://www.twitch.tv");
  eq("off","about:blank");eq("home","https://www.youtube.com");
  eq("https://youtu.be/abcdefghijk?t=12","https://www.youtube.com/watch?v=abcdefghijk&t=12");
  for(String bad:new String[]{"https://twitch.tv.evil.test/user","https://evil.test/twitch.tv","http://twitch.tv/user","https://a@twitch.tv/user","https://twitch.tv:443/user","javascript:alert(1)","https://twitch.tv/videos/not-a-number","https://clips.twitch.tv/embed?clip=abc","https://twitch.tv/a%2Fb","https://twitch.tv/../user","https://youtu.be/short"})reject(bad);
  reject("x".repeat(TelevisionUrl.MAX_LENGTH+1));
  System.out.println("TV_URL_PASS: Twitch channels/VOD/clips, YouTube, off, time and unsafe URL rejection");
 }
}
