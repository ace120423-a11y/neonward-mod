package jp.neonward.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import jp.neonward.TelevisionPlaybackClient;
import net.montoyo.wd.entity.*;
import net.montoyo.wd.utilities.browser.WDClientBrowser;
/** Optional client-only integration: no WebDisplays classes loaded on dedicated servers. */
@Pseudo
@Mixin(targets="net.montoyo.wd.entity.ScreenData",remap=false)
public class TelevisionBrowserMixin {
 @Unique private boolean neonClosed;
 @Inject(method="createBrowser",at=@At("HEAD"),cancellable=true)
 private void neonCreate(ScreenBlockEntity be,boolean animation,CallbackInfo ci){var data=(ScreenData)(Object)this;if(!TelevisionPlaybackClient.allowed(data,be)){data.closeBrowser();ci.cancel();return;}neonClosed=false;TelevisionPlaybackClient.track(data,be);}
 @Inject(method="closeBrowser",at=@At("HEAD"))
 private void neonClose(CallbackInfo ci){neonClosed=true;var data=(ScreenData)(Object)this;TelevisionPlaybackClient.forget(data);TelevisionPlaybackClient.halt(data.browser);}
 @Inject(method="lambda$createBrowser$0",at=@At("HEAD"),cancellable=true)
 private void neonLateBrowser(WDClientBrowser browser,CallbackInfo ci){if(neonClosed){TelevisionPlaybackClient.destroy(browser);ci.cancel();}}
 @ModifyVariable(method="setVolume",at=@At("HEAD"),argsOnly=true)
 private float neonVolumeLimit(float requested){float limit=((ScreenData)(Object)this).autoVolumeMaxLevel;return Float.isFinite(requested)&&Float.isFinite(limit)?Math.clamp(requested,0f,Math.clamp(limit,0f,.25f)):0f;}
}
