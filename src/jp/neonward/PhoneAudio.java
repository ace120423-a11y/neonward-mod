package jp.neonward;
import javax.sound.sampled.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;

/** Microphone opens only after an explicit record action or an accepted call. */
public final class PhoneAudio {
 static volatile float callGain=2.0f;
 static final AudioFormat FORMAT=new AudioFormat(16000,16,1,true,false);
 static volatile String state="idle",session="",name="",message="",error="";
 static volatile boolean muted,recording,playing,consent;static final java.util.concurrent.atomic.AtomicInteger queued=new java.util.concurrent.atomic.AtomicInteger();static volatile long recordingAt;
 static volatile TargetDataLine mic;static volatile SourceDataLine speaker;static volatile Clip clip;
 static int generation;static final ArrayBlockingQueue<byte[]> playback=new ArrayBlockingQueue<>(8);
 static void adjustVolume(float delta){callGain=Math.max(.5f,Math.min(4f,callGain+delta));}
 static byte[] amplify(byte[] pcm){byte[] out=pcm.clone();for(int i=0;i+1<out.length;i+=2){int sample=(short)((out[i]&255)|((out[i+1]&255)<<8));int boosted=Math.max(Short.MIN_VALUE,Math.min(Short.MAX_VALUE,Math.round(sample*callGain)));out[i]=(byte)boosted;out[i+1]=(byte)(boosted>>8);}return out;}
 static Path dir(){return Minecraft.getInstance().gameDirectory.toPath().resolve("neonward/recordings");}
 static List<Path> files(){try{Files.createDirectories(dir());try(var s=Files.list(dir())){return s.filter(p->Files.isRegularFile(p)&&p.getFileName().toString().endsWith(".wav")).sorted(Comparator.reverseOrder()).toList();}}catch(Exception e){error="録音フォルダを開けません";return List.of();}}
 static void init(){ClientPlayNetworking.registerGlobalReceiver(PhoneCalls.Audio.TYPE,(p,c)->{if(state.equals("active")&&session.equals(p.session())){byte[] audio=amplify(p.pcm());if(!playback.offer(audio)){playback.poll();playback.offer(audio);}}});ClientPlayConnectionEvents.DISCONNECT.register((h,m)->{state="idle";session="";consent=false;stopCapture();stopOutput();});
  HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,NeonWard.id("phone_call"),(g,d)->{if(state.equals("idle"))return;var mc=Minecraft.getInstance();String label=state.equals("incoming")?"着信："+name+" / スマホ→電話で応答":state.equals("outgoing")?name+"を呼び出し中":name+"と通話中 / "+(muted?"マイクOFF":"マイクON");g.fill(4,4,Math.min(g.guiWidth()-4,mc.font.width(label)+14),20,0xe8081724);g.text(mc.font,label,8,9,0xff70fff0);});
 }
 static void receive(JsonObject o){String old=session;String next=o.get("session").getAsString(),nextState=o.get("state").getAsString();boolean start=nextState.equals("active")&&(!state.equals("active")||!old.equals(next));state=nextState;session=next;name=o.get("name").getAsString();message=o.get("message").getAsString();if(start&&!consent){command("end");state="idle";session="";return;}if(state.equals("idle"))consent=false;if(start){stopCapture();stopOutput();muted=false;error="";startCall(next);}else if(!state.equals("active")&&!recording){stopCapture();stopOutput();}if(Minecraft.getInstance().gui.screen() instanceof PhoneAppScreen s&&s.app==1){if(state.equals("idle"))s.picking=true;s.refresh();}}
 static void command(String action){if(action.startsWith("dial ")||action.equals("accept"))consent=true;else if(action.equals("end"))consent=false;var p=Minecraft.getInstance().player;if(p!=null)p.connection.sendCommand("neoncall "+action);}
 static void daemon(String name,Runnable r){var t=new Thread(r,name);t.setDaemon(true);t.start();}
 static synchronized int nextGeneration(){return ++generation;}
 static void startCall(String token){int gen=nextGeneration();daemon("NEON microphone",()->capture(gen,token,null));daemon("NEON speaker",()->{SourceDataLine line=null;try{line=AudioSystem.getSourceDataLine(FORMAT);line.open(FORMAT,12800);if(gen!=generation)return;speaker=line;line.start();while(gen==generation&&state.equals("active")){byte[] b=playback.poll(200,TimeUnit.MILLISECONDS);if(b!=null)line.write(b,0,b.length);}}catch(Exception e){if(gen==generation)failCall("スピーカーを開けません。音声出力を確認してください");}finally{if(line!=null){line.stop();line.close();}if(speaker==line)speaker=null;}});}
 static void failCall(String text){error=text;Minecraft.getInstance().execute(()->{command("end");message=text;state="idle";stopCapture();stopOutput();});}
 static void capture(int gen,String token,ByteArrayOutputStream memo){TargetDataLine line=null;try{line=AudioSystem.getTargetDataLine(FORMAT);line.open(FORMAT,12800);if(gen!=generation)return;mic=line;line.start();byte[] b=new byte[1280];int at=0;while(gen==generation&&(memo==null?state.equals("active"):recording)){int n=line.read(b,at,b.length-at);if(n<=0)break;at+=n;if(at<b.length)continue;at=0;if(memo!=null){memo.write(b);if(memo.size()>=16000*2*120){recording=false;break;}}else if(!muted&&queued.get()<3){byte[] data=b.clone();queued.incrementAndGet();Minecraft.getInstance().execute(()->{try{if(gen==generation&&state.equals("active")&&session.equals(token)&&ClientPlayNetworking.canSend(PhoneCalls.Audio.TYPE))ClientPlayNetworking.send(new PhoneCalls.Audio(token,data));}finally{queued.decrementAndGet();}});}}}catch(Exception e){if(gen==generation){error="マイクを開けません。Windowsのマイク設定を確認してください";if(memo==null)failCall(error);}}finally{if(line!=null){line.stop();line.close();}if(mic==line)mic=null;if(memo!=null){recording=false;if(memo.size()>0)save(memo.toByteArray());}}}
 static void record(){if(!state.equals("idle")){error="通話を終了してから録音してください";return;}if(recording)return;stopOutput();error="";recording=true;recordingAt=System.currentTimeMillis();int gen=nextGeneration();daemon("NEON recorder",()->capture(gen,"",new ByteArrayOutputStream()));}
 static synchronized void stopCapture(){generation++;recording=false;var m=mic;mic=null;if(m!=null){m.stop();m.close();}}
 static void save(byte[] bytes){try{Files.createDirectories(dir());Path p=dir().resolve(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))+"_"+UUID.randomUUID().toString().substring(0,6)+".wav");try(var in=new AudioInputStream(new ByteArrayInputStream(bytes),FORMAT,bytes.length/2)){AudioSystem.write(in,AudioFileFormat.Type.WAVE,p.toFile());}message="ボイスメモを保存しました";}catch(Exception e){error="録音を保存できませんでした";}}
 static void play(Path p){if(recording||!state.equals("idle")){error="録音・通話を止めてから再生してください";return;}stopOutput();error="";try(var in=AudioSystem.getAudioInputStream(p.toFile())){clip=AudioSystem.getClip();clip.open(in);clip.start();playing=true;}catch(Exception e){error="この録音を再生できません";}}
 static void stopOutput(){playback.clear();var s=speaker;speaker=null;if(s!=null){s.stop();s.close();}var c=clip;clip=null;if(c!=null){c.stop();c.close();}playing=false;}
}
