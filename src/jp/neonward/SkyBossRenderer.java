package jp.neonward;

import java.util.*;
import java.io.*;
import com.google.gson.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Native articulated entity meshes, with no hidden zombie body or floating display entities. */
public class SkyBossRenderer extends MobRenderer<SkyBoss,SkyBossRenderer.State,SkyBossRenderer.Rig> {
 static class State extends LivingEntityRenderState {int casting,phase;}
 static class Part {String name,anim,parent;float[] p,size,rot;float[][] vertices;int mat;}
 static class Shape {Part[] parts;}
 static final Map<String,Shape> SHAPES=new HashMap<>();
 private final Identifier texture;
 SkyBossRenderer(EntityRendererProvider.Context c,SkyRoster.Kind k){
  super(c,new Rig(mesh(SHAPES.get(k.id())),SHAPES.get(k.id())),k.width()*.35f);texture=NeonWard.id("textures/entity/"+SkyRoster.model(k)+".png");
  addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<State,Rig>(this){public net.minecraft.client.renderer.rendertype.RenderType renderType(){return net.minecraft.client.renderer.rendertype.RenderTypes.eyes(NeonWard.id("textures/entity/"+SkyRoster.model(k)+"_glow.png"));}});
 }
 public State createRenderState(){return new State();}
 public Identifier getTextureLocation(State s){return texture;}
 @Override public void extractRenderState(SkyBoss e,State s,float partial){super.extractRenderState(e,s,partial);s.casting=e.castVisual();s.phase=e.phase();}
 static class Rig extends EntityModel<State> {
  final Shape shape;final java.util.function.Function<String,ModelPart> lookup;
  Rig(ModelPart root,Shape shape){super(root);this.shape=shape;this.lookup=root.createPartLookup();}
  @Override public void setupAnim(State s){super.setupAnim(s);float t=s.ageInTicks;
   for(var p:shape.parts){var part=lookup.apply(p.name);String a=p.anim;if(a==null||a.isEmpty())continue;
    if(a.equals("rotor"))part.yRot+=t*.35f;
    else if(a.equals("hand"))part.zRot+=t*.045f;
    else if(a.startsWith("wing"))part.zRot+=(float)Math.sin(t*.09)*(a.endsWith("-1")?-.13f:.13f);
    else if(a.startsWith("tail"))part.zRot+=(float)Math.sin(t*.07+Math.abs(a.hashCode()%7))*.14f;
    else if(a.startsWith("leg"))part.xRot+=(float)Math.sin(t*.18+Math.abs(a.hashCode()%2)*Math.PI)*.18f;
    else if(a.equals("jaw"))part.xRot+=s.casting==1?-.25f:(float)Math.sin(t*.05)*.04f;
    else if(a.equals("pulse")){float scale=1+(float)Math.sin(t*.12)*.035f;part.xScale=part.yScale=part.zScale=scale;}
   }
  }
 }
 static Shape read(String id){try(var in=SkyBossRenderer.class.getResourceAsStream("/assets/neonward/bosses/"+id+".json")){if(in==null)throw new IOException(id);return new Gson().fromJson(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8),Shape.class);}catch(Exception e){throw new IllegalStateException("Missing boss rig "+id,e);}}
 // A virtual compile override also keeps Sodium's cached box renderer from replacing curved surfaces.
 static final class MeshCube extends ModelPart.Cube {
  final float[][] vertices;
  MeshCube(Part p){super(0,0,-p.size[0]*8,-p.size[1]*8,-p.size[2]*8,p.size[0]*16,p.size[1]*16,p.size[2]*16,0,0,0,false,1024,256,java.util.Set.of());vertices=p.vertices;}
  @Override public void compile(PoseStack.Pose pose,VertexConsumer out,int light,int overlay,int color){
   for(int q=0;q<vertices.length;q+=4)for(int i=3;i>=0;i--){var v=vertices[q+i];out.addVertex(pose,v[0],-v[1],v[2]).setColor(color).setUv(v[6],v[7]).setOverlay(overlay).setLight(light).setNormal(pose,v[3],-v[4],v[5]);}
  }
 }
 static ModelPart mesh(Shape shape){
  var nodes=new HashMap<String,ModelPart>();var children=new HashMap<String,Map<String,ModelPart>>();
  for(var p:shape.parts){var c=new LinkedHashMap<String,ModelPart>();children.put(p.name,c);var part=new ModelPart(p.vertices.length==0?List.of():List.of(new MeshCube(p)),c);var pose=PartPose.offsetAndRotation(p.p[0]*16,-p.p[1]*16,p.p[2]*16,-p.rot[0],p.rot[1],-p.rot[2]);part.setInitialPose(pose);part.loadPose(pose);nodes.put(p.name,part);}
  var top=new LinkedHashMap<String,ModelPart>();for(var p:shape.parts)(p.parent==null?top:children.get(p.parent)).put(p.name,nodes.get(p.name));
  var rig=new ModelPart(List.of(),top);rig.setInitialPose(PartPose.offset(0,24,0));rig.loadPose(rig.getInitialPose());return new ModelPart(List.of(),Map.of("rig",rig));
 }
 static Shape skyShape(SkyRoster.Kind k){
  Shape shape=read(SkyRoster.model(k));var parts=new ArrayList<Part>(Arrays.asList(shape.parts));
  // Dedicated orbiting crystalline fins give the second tower variants their own silhouette.
  int count=3+k.floor()%5;
  for(int i=0;i<count;i++){
   Part p=new Part();p.name="sky_fin_"+i;p.parent=null;p.anim="pulse";p.mat=3;
   double angle=i*Math.PI*2/count;
   p.p=new float[]{(float)Math.cos(angle)*k.width()*.48f,k.height()*.7f,(float)Math.sin(angle)*k.width()*.48f};
   p.rot=new float[]{0,(float)-angle,.25f};p.size=new float[]{.18f,.65f,.35f};
   float[][] corners={{-.09f,-.325f,-.175f},{.09f,-.325f,-.175f},{.09f,.325f,-.175f},{-.09f,.325f,-.175f},{-.09f,-.325f,.175f},{.09f,-.325f,.175f},{.09f,.325f,.175f},{-.09f,.325f,.175f}};
   int[][] faces={{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
   float[][] normals={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
   p.vertices=new float[24][8];
   for(int face=0;face<6;face++)for(int v=0;v<4;v++){var c=corners[faces[face][v]];p.vertices[face*4+v]=new float[]{c[0],c[1],c[2],normals[face][0],normals[face][1],normals[face][2],.88f+(v%2)*.04f,.4f+(v/2)*.1f};}
   parts.add(p);
  }
  shape.parts=parts.toArray(Part[]::new);return shape;
 }
 public static void init(){for(var k:SkyRoster.ALL){SHAPES.put(k.id(),skyShape(k));EntityRendererRegistry.register(SkyBosses.TYPES.get(k.id()),c->new SkyBossRenderer(c,k));}}
}
