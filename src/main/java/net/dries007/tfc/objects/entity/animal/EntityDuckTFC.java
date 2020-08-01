/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.objects.entity.animal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import net.dries007.tfc.ConfigTFC;
import net.dries007.tfc.Constants;
import net.dries007.tfc.api.capability.egg.CapabilityEgg;
import net.dries007.tfc.api.capability.egg.IEgg;
import net.dries007.tfc.api.types.ILivestock;
import net.dries007.tfc.client.TFCSounds;
import net.dries007.tfc.objects.LootTablesTFC;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.CalendarTFC;
import net.dries007.tfc.util.climate.BiomeHelper;
import net.dries007.tfc.world.classic.biomes.BiomesTFC;

/**
 * A Chicken of the colder regions!
 * Actually, ducks takes longer to reach maturity, but hey, they are cute!
 */
@ParametersAreNonnullByDefault
public class EntityDuckTFC extends EntityChickenTFC implements ILivestock
{
    //The last time(in ticks) this chicken has laid eggs
    private static final DataParameter<Long> LAID = EntityDataManager.createKey(EntityChickenTFC.class, Helpers.LONG_DATA_SERIALIZER);
    //Copy from vanilla's EntityChicken, used by renderer to properly handle wing flap
    public float wingRotation;
    public float destPos;
    public float oFlapSpeed;
    public float oFlap;
    public float wingRotDelta = 1.0F;

    public EntityDuckTFC(World worldIn)
    {
        this(worldIn, Gender.valueOf(Constants.RNG.nextBoolean()), getRandomGrowth(ConfigTFC.Animals.DUCK.adulthood, ConfigTFC.Animals.DUCK.elder));
    }

    public EntityDuckTFC(World worldIn, Gender gender, int birthDay)
    {
        super(worldIn, gender, birthDay);
        this.setSize(0.9F, 0.9F);
    }

    @Override
    public int getSpawnWeight(Biome biome, float temperature, float rainfall, float floraDensity, float floraDiversity)
    {
        BiomeHelper.BiomeType biomeType = BiomeHelper.getBiomeType(temperature, rainfall, floraDensity);
        if (!BiomesTFC.isOceanicBiome(biome) && !BiomesTFC.isBeachBiome(biome) &&
            (biomeType == BiomeHelper.BiomeType.PLAINS || biomeType == BiomeHelper.BiomeType.TEMPERATE_FOREST))
        {
            return ConfigTFC.Animals.DUCK.rarity;
        }
        return 0;
    }

    @Override
    public BiConsumer<List<EntityLiving>, Random> getGroupingRules()
    {
        return AnimalGroupingRules.MALE_AND_FEMALES;
    }

    @Override
    public int getMinGroupSize()
    {
        return 3;
    }

    @Override
    public int getMaxGroupSize()
    {
        return 5;
    }

    @Override
    public int getDaysToAdulthood()
    {
        return ConfigTFC.Animals.DUCK.adulthood;
    }

    @Override
    public int getDaysToElderly()
    {
        return ConfigTFC.Animals.DUCK.elder;
    }

    @Override
    public Type getType()
    {
        return Type.OVIPAROUS;
    }

    @Override
    public boolean isReadyForAnimalProduct()
    {
        // Is ready for laying eggs?
        return this.getFamiliarity() > 0.15f && hasEggs();
    }

    @Override
    public List<ItemStack> getProducts()
    {
        List<ItemStack> eggs = new ArrayList<>();
        ItemStack egg = new ItemStack(Items.EGG);
        if (this.isFertilized())
        {
            IEgg cap = egg.getCapability(CapabilityEgg.CAPABILITY, null);
            if (cap != null)
            {
                EntityDuckTFC chick = new EntityDuckTFC(this.world);
                chick.setFamiliarity(this.getFamiliarity() < 0.9F ? this.getFamiliarity() / 2.0F : this.getFamiliarity() * 0.9F);
                cap.setFertilized(chick, ConfigTFC.Animals.DUCK.hatch + CalendarTFC.PLAYER_TIME.getTotalDays());
            }
        }
        eggs.add(egg);
        return eggs;
    }

    @Override
    public void setProductsCooldown()
    {
        this.setLaidTicks(CalendarTFC.PLAYER_TIME.getTicks());
    }

    @Override
    public long getProductsCooldown()
    {
        return Math.max(0, ConfigTFC.Animals.DUCK.eggTicks + getLaidTicks() - CalendarTFC.PLAYER_TIME.getTicks());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return TFCSounds.ANIMAL_DUCK_HURT;
    }

    @Override
    protected SoundEvent getDeathSound()
    {
        return TFCSounds.ANIMAL_DUCK_DEATH;
    }

    @Override
    protected SoundEvent getAmbientSound()
    {
        return Constants.RNG.nextInt(100) < 5 ? TFCSounds.ANIMAL_DUCK_CRY : TFCSounds.ANIMAL_DUCK_SAY;
    }

    @Nullable
    protected ResourceLocation getLootTable()
    {
        return LootTablesTFC.ANIMALS_DUCK;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block blockIn)
    {
        // Same sound, no need to create another
        this.playSound(SoundEvents.ENTITY_CHICKEN_STEP, 0.15F, 1.0F);
    }

    @Override
    public double getOldDeathChance()
    {
        return ConfigTFC.Animals.DUCK.oldDeathChance;
    }

    @Override
    public void writeEntityToNBT(@Nonnull NBTTagCompound nbt)
    {
        super.writeEntityToNBT(nbt);
        nbt.setLong("laidTicks", getLaidTicks());
    }

    @Override
    public void readEntityFromNBT(@Nonnull NBTTagCompound nbt)
    {
        super.readEntityFromNBT(nbt);
        this.setLaidTicks(nbt.getLong("laidTicks"));
    }

    @Override
    protected void entityInit()
    {
        super.entityInit();
        getDataManager().register(LAID, 0L);
    }

    public long getLaidTicks()
    {
        return dataManager.get(LAID);
    }

    protected void setLaidTicks(long ticks)
    {
        dataManager.set(LAID, ticks);
    }
}
