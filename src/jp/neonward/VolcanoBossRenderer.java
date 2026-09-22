package jp.neonward;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
/** Keyframed charge, release and recovery, driven by synchronized server attack time. */
final class VolcanoBossRenderer extends MobRenderer<VolcanoBoss,VolcanoBossRenderer.State,VolcanoBossRenderer.Rig> {
 static class State extends LivingEntityRenderState {int attack,phase;float attackAge;}
 VolcanoBossRenderer(EntityRendererProvider.Context c,int f){super(c,new Rig(SkyBossRenderer.read(String.format("volcano_%02d",f))),1.2f);addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<State,Rig>(this){public net.minecraft.client.renderer.rendertype.RenderType renderType(){return net.minecraft.client.renderer.rendertype.RenderTypes.eyes(NeonWard.id("textures/entity/volcano_materials_glow.png"));}});}
 public State createRenderState(){return new State();}public Identifier getTextureLocation(State s){return NeonWard.id("textures/entity/volcano_materials.png");}
 @Override public void extractRenderState(VolcanoBoss e,State s,float dt){super.extractRenderState(e,s,dt);s.attack=e.action();s.attackAge=e.actionAge()+dt;s.phase=e.phase();}
 static float smooth(float t){t=Math.clamp(t,0,1);return t*t*(3-2*t);}
 static class Rig extends EntityModel<State>{
  final SkyBossRenderer.Shape shape;final java.util.function.Function<String,ModelPart> find;
  Rig(SkyBossRenderer.Shape s){super(SkyBossRenderer.mesh(s),net.minecraft.client.renderer.rendertype.RenderTypes::entityTranslucent);shape=s;find=root.createPartLookup();}
  @Override public void setupAnim(State s){super.setupAnim(s);float t=s.ageInTicks,a=s.attackAge;boolean casting=s.attack>0;float wind=smooth(a/24),release=smooth((a-24)/8),recover=1-smooth((a-54)/22);float pose=(wind-release*1.5f)*recover;
   for(var p:shape.parts){var m=find.apply(p.name);String anim=p.anim;if(anim==null)continue;
    if(anim.startsWith("leg")){m.xRot+=(float)Math.sin(t*.22+(anim.endsWith("1")?Math.PI:0))*.2f*(casting?.25f:1);if(s.attack==2||s.attack==6)m.xRot+=wind*.15f*recover;}
    else if(anim.startsWith("hand")){if(casting){if(s.attack==1)m.yRot+=pose*1.3f;else m.xRot-=pose*2.2f;m.zRot+=(anim.endsWith("-1")?-1:1)*wind*.2f*recover;}else m.xRot+=(float)Math.sin(t*.06)*.05f;}
    else if(anim.equals("heldrock"))m.visible=s.attack==6&&a>8&&a<32;
    else if(anim.equals("jaw"))m.xRot-=casting?wind*.5f*recover:0;
    else if(anim.equals("head")){if(s.attack==3&&a>=32)m.yRot+=(float)Math.sin((a-32)*Math.PI/32)*.45f;else if(casting)m.xRot+=pose*.2f;}
    else if(anim.startsWith("wing"))m.zRot+=(float)Math.sin(t*.14)*(anim.endsWith("-1")?-1:1)*.35f;
    else if(anim.startsWith("tail"))m.yRot+=(float)Math.sin(t*.08+p.name.hashCode()%7)*.14f+(s.attack==2?pose*.35f:0);
    else if(anim.equals("wheel"))m.zRot+=t*(s.attack==5?.35f:.04f);
    else if(anim.equals("hover"))m.y+=(float)Math.sin(t*.07+p.name.hashCode()%5)*1.8f;
    else if(anim.equals("pulse")){float scale=1+s.phase*.12f+(float)Math.sin(t*.1)*.04f;m.xScale=m.yScale=m.zScale=scale;}
   }
   var body=root.getChild("rig");if(casting){if(s.attack==6||s.attack==2){body.y+=wind*(1-release)*3;body.xRot+=pose*.1f;}if(s.attack==5)body.xRot+=wind*.15f*recover;}
  }
 }
 static void init(){for(int f=1;f<=30;f++){int floor=f;EntityRendererRegistry.register(VolcanoBosses.TYPES.get(f-1),c->new VolcanoBossRenderer(c,floor));}}
}
