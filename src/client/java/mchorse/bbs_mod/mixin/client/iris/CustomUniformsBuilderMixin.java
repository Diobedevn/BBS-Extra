package mchorse.bbs_mod.mixin.client.iris;

import mchorse.bbs_mod.utils.iris.ShaderCurves;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CustomUniforms.Builder.class)
public class CustomUniformsBuilderMixin
{
    @Inject(method = "build(Lnet/irisshaders/iris/uniforms/custom/CustomUniformFixedInputUniformsHolder;)Lnet/irisshaders/iris/uniforms/custom/CustomUniforms;", at = @At("RETURN"), cancellable = true, remap = false)
    public void onBuild(CallbackInfoReturnable<CustomUniforms> info)
    {
        if (info.getReturnValue() instanceof CustomUniformsAccessor accessor)
        {
            ShaderCurves.addUniforms(accessor.bbs$uniformOrder());
        }
    }
}