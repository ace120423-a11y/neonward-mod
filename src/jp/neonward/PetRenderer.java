package jp.neonward;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Native articulated model parts. No display entities, invisible carriers, or per-part packets. */
public final class PetRenderer extends MobRenderer<PetEntity,PetRenderer.State,PetRenderer.Rig> {
 static class State extends LivingEntityRenderState {int kind;boolean flying;}
 static class Part {String name,parent,anim;float[] p,size;int color;}
 static class Shape {Part[] parts;}
 static final Identifier TEXTURE=NeonWard.id("pets/white.png");
 PetRenderer(EntityRendererProvider.Context context,Shape shape){super(context,new Rig(shape),.28f);}
 @Override public State createRenderState(){return new State();}
 @Override public Identifier getTextureLocation(State state){return TEXTURE;}
 @Override public void extractRenderState(PetEntity entity,State state,float partial){super.extractRenderState(entity,state,partial);state.kind=entity.kind();state.flying=entity.flying();}
 static final class Rig extends EntityModel<State> {
  final Shape shape;final java.util.function.Function<String,ModelPart> lookup;
  Rig(Shape shape){super(mesh(shape));this.shape=shape;lookup=root().createPartLookup();}
  @Override public void setupAnim(State state){
   super.setupAnim(state);float walk=Math.min(1,state.walkAnimationSpeed*3),phase=state.walkAnimationPos*1.4f;
   for(var p:shape.parts){
    var part=lookup.apply(p.name);String a=p.anim;if(a==null)continue;
    if(a.startsWith("leg"))part.xRot+=(float)Math.sin(phase+(a.endsWith("1")?Math.PI:0))*.6f*walk;
    else if(a.startsWith("snake")){int index=Integer.parseInt(a.substring(5));part.yRot+=(float)Math.sin(phase-index*.65)*.28f*walk;}
    else if(a.startsWith("wing")){float sign=a.endsWith("1")?-1:1;part.zRot+=sign*(state.flying?(float)Math.sin(state.ageInTicks*.65)*.65f:1.15f);}
    else if(a.equals("tail"))part.yRot+=(float)Math.sin(state.ageInTicks*.13)*(.08f+walk*.3f);
    else if(a.equals("antenna"))part.zRot+=(float)Math.sin(state.ageInTicks*.08)*.08f;
    else if(a.equals("head")){part.yRot+=state.yRot*.007f;part.xRot+=state.xRot*.005f;}
    else if(a.equals("bird")&&state.flying)part.xRot=-.1f;
   }
  }
 }
 static final class Cube extends ModelPart.Cube {
  final float[][] vertices;final int tint;
  // Same nonzero bounds + virtual compile pattern as SkyBossRenderer.MeshCube.
  // Sodium must retain the custom compiler rather than cache a degenerate box.
  Cube(Part p){super(0,0,-p.size[0]*8,-p.size[1]*8,-p.size[2]*8,p.size[0]*16,p.size[1]*16,p.size[2]*16,0,0,0,false,1,1,Set.of());tint=0xff000000|p.color;
   float x=p.size[0]/2,y=p.size[1]/2,z=p.size[2]/2;
   float[][] c={{-x,-y,-z},{x,-y,-z},{x,y,-z},{-x,y,-z},{-x,-y,z},{x,-y,z},{x,y,z},{-x,y,z}};
   int[][] f={{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
   float[][] n={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
   vertices=new float[24][6];for(int face=0;face<6;face++)for(int i=0;i<4;i++){var v=c[f[face][i]];vertices[face*4+i]=new float[]{v[0],v[1],v[2],n[face][0],n[face][1],n[face][2]};}
  }
  @Override public void compile(PoseStack.Pose pose,VertexConsumer out,int light,int overlay,int color){
   for(int q=0;q<vertices.length;q+=4)for(int i=3;i>=0;i--){var v=vertices[q+i];out.addVertex(pose,v[0],-v[1],v[2]).setColor(tint).setUv(.5f,.5f).setOverlay(overlay).setLight(light).setNormal(pose,v[3],-v[4],v[5]);}
  }
 }
 static ModelPart mesh(Shape shape){
  var nodes=new LinkedHashMap<String,ModelPart>();var children=new HashMap<String,Map<String,ModelPart>>();
  for(var p:shape.parts){var map=new LinkedHashMap<String,ModelPart>();children.put(p.name,map);var node=new ModelPart(p.size==null?List.of():List.of(new Cube(p)),map);var pose=PartPose.offset(p.p[0]*16,-p.p[1]*16,p.p[2]*16);node.setInitialPose(pose);node.loadPose(pose);nodes.put(p.name,node);}
  var top=new LinkedHashMap<String,ModelPart>();for(var p:shape.parts)(p.parent==null?top:children.get(p.parent)).put(p.name,nodes.get(p.name));
  var rig=new ModelPart(List.of(),top);var pose=PartPose.offset(0,24,0);rig.setInitialPose(pose);rig.loadPose(pose);return new ModelPart(List.of(),Map.of("rig",rig));
 }
 static Shape read(String id){try(var in=PetRenderer.class.getResourceAsStream("/assets/neonward/pets/"+id+".json")){
  if(in==null)throw new IOException("Missing pet mesh "+id);return new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),Shape.class);
 }catch(IOException e){throw new IllegalStateException(e);}}
 public static void init(){String[] ids={"dog","cat","snake","crow","robot"};for(int i=0;i<ids.length;i++){Shape shape=read(ids[i]);EntityRendererRegistry.register(PetCompanions.TYPES.get(i),context->new PetRenderer(context,shape));}}
}
