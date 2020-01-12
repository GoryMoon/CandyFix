package se.gory_moon.candyfix;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

@IFMLLoadingPlugin.TransformerExclusions("se.gory_moon.candyfix")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class CandyFixCore implements IFMLLoadingPlugin {
    public static File modFile = null;

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {getAccessTransformerClass()};
    }

    @Override
    public String getModContainerClass() {
        return "se.gory_moon.candyfix.CandyFixContainer";
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        modFile = (File) data.get("coremodLocation");
    }

    @Override
    public String getAccessTransformerClass() {
        return "se.gory_moon.candyfix.CandyFixTransformer";
    }
}
