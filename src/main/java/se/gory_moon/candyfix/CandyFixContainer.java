package se.gory_moon.candyfix;

import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.client.FMLFileResourcePack;
import net.minecraftforge.fml.client.FMLFolderResourcePack;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.VersionParser;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CandyFixContainer extends DummyModContainer {

    public CandyFixContainer() {
        super(new ModMetadata());
        ModMetadata meta = getMetadata();
        meta.modId = "candyfix";
        meta.name = "Candy Fix";
        meta.version = "@MOD_VERSION@";
        meta.authorList = Collections.singletonList("GoryMoon");
        meta.description = "A mod that fixes some issues in CandyWorld";
        meta.logoFile = "logo.png";
    }

    @Override
    public Set<ArtifactVersion> getRequirements() {
        return Collections.singleton(VersionParser.parseVersionReference("candymod@1.1.4"));
    }

    @Override
    public List<ArtifactVersion> getDependants() {
        return Collections.singletonList(VersionParser.parseVersionReference("candymod@1.1.4"));
    }

    @Override
    public Class<?> getCustomResourcePackClass() {
        return getSource() == null || getSource().isDirectory() ? FMLFolderResourcePack.class : FMLFileResourcePack.class;
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        return true;
    }

    @Override
    public File getSource() {
        return CandyFixCore.modFile;
    }

    @Override
    public Object getMod() {
        return this;
    }
}
