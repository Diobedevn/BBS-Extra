package mchorse.bbs_mod.mixin.client.iris;

import mchorse.bbs_mod.utils.iris.ShaderCurves;
import net.irisshaders.iris.shaderpack.preprocessor.JcppProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(JcppProcessor.class)
public class JcppProcessorMixin
{
    @ModifyVariable(method = "glslPreprocessSource", at = @At("HEAD"), ordinal = 0, remap = false)
    private static String returnClean(String source)
    {
        return ShaderCurves.processSource(source);
    }
}