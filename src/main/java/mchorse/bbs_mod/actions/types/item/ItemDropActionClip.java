package mchorse.bbs_mod.actions.types.item;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;

public class ItemDropActionClip extends ItemActionClip
{
    public final ValueDouble posX = new ValueDouble("x", 0D);
    public final ValueDouble posY = new ValueDouble("y", 0D);
    public final ValueDouble posZ = new ValueDouble("z", 0D);
    public final ValueFloat velocityX = new ValueFloat("vx", 0F);
    public final ValueFloat velocityY = new ValueFloat("vy", 0F);
    public final ValueFloat velocityZ = new ValueFloat("vz", 0F);

    public ItemDropActionClip()
    {
        super();

        this.add(this.posX);
        this.add(this.posY);
        this.add(this.posZ);
        this.add(this.velocityX);
        this.add(this.velocityY);
        this.add(this.velocityZ);
    }

    public void shift(double dx, double dy, double dz)
    {
        this.posX.set(this.posX.get() + dx);
        this.posY.set(this.posY.get() + dy);
        this.posZ.set(this.posZ.get() + dz);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        this.applyPositionRotation(player, replay, tick);

        ItemEntity entity = new ItemEntity(
            player.getServerWorld(),
            this.posX.get(), this.posY.get(), this.posZ.get(), this.itemStack.get().copy(),
            this.velocityX.get(), this.velocityY.get(), this.velocityZ.get()
        );

        entity.setToDefaultPickupDelay();
        player.getWorld().spawnEntity(entity);
    }

    @Override
    protected Clip create()
    {
        return new ItemDropActionClip();
    }
}