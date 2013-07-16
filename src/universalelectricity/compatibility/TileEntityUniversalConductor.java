package universalelectricity.compatibility;

import ic2.api.Direction;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergyTile;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;
import universalelectricity.core.block.IConnector;
import universalelectricity.core.electricity.ElectricityPack;
import universalelectricity.core.grid.IElectricityNetwork;
import universalelectricity.core.vector.Vector3;
import universalelectricity.core.vector.VectorHelper;
import universalelectricity.prefab.tile.TileEntityConductor;
import buildcraft.api.power.IPowerProvider;
import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerProvider;

/**
 * A universal conductor class. 
 * 
 * Extend this class or use as a reference for your own implementation of compatible conductor tiles.
 * 
 * @author micdoodle8
 * 
 */
public abstract class TileEntityUniversalConductor extends TileEntityConductor implements IEnergySink, IPowerReceptor
{
    protected boolean addedToIC2Network = false;
    private DummyPowerProvider powerProvider;
    
    public TileEntityUniversalConductor()
    {
        this.powerProvider = new DummyPowerProvider(this.getNetwork(), this);
        this.powerProvider.configure(0, 0, 100, 0, 100);
    }

    @Override
    public void setNetwork(IElectricityNetwork network)
    {
        super.setNetwork(network);
        
        if (this.powerProvider != null)
        {
            this.powerProvider.network = network;
        }
    }

    @Override
    public void invalidate()
    {
        this.unloadTileIC2();
        super.invalidate();
    }

    @Override
    public void onChunkUnload()
    {
        this.unloadTileIC2();
        super.onChunkUnload();
    }

    private void unloadTileIC2()
    {
        if (this.addedToIC2Network && this.worldObj != null)
        {
            if (Compatibility.isIndustrialCraft2Loaded())
            {
                MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            }

            this.addedToIC2Network = false;
        }
    }

    @Override
    public boolean canUpdate()
    {
        return true;
    }
    
    @Override
    public void updateEntity()
    {
        super.updateEntity();
        
        if (!this.worldObj.isRemote && !this.addedToIC2Network)
        {
            if (Compatibility.isIndustrialCraft2Loaded())
            {
                MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            }
            
            this.addedToIC2Network = true;
        }
    }

    @Override
    public boolean acceptsEnergyFrom(TileEntity emitter, Direction direction)
    {
        return emitter instanceof IEnergyTile;
    }

    @Override
    public boolean isAddedToEnergyNet()
    {
        return this.addedToIC2Network;
    }

    @Override
    public void setPowerProvider(IPowerProvider provider)
    {
        if (provider instanceof DummyPowerProvider)
        {
            this.powerProvider = (DummyPowerProvider) provider;
        }
    }

    @Override
    public IPowerProvider getPowerProvider()
    {
        return this.powerProvider;
    }

    @Override
    public void doWork()
    {
        ;
    }

    @Override
    public int powerRequest(ForgeDirection from)
    {
        if (this.getNetwork() == null)
        {
            return 0;
        }
        
        return (int)Math.floor(Math.min(this.getNetwork().getRequest(VectorHelper.getTileEntityFromSide(this.worldObj, new Vector3(this), from)).getWatts() * Compatibility.TO_BC_RATIO, 100));
    }

    @Override
    public int demandsEnergy()
    {
        if (this.getNetwork() == null)
        {
            return 0;
        }
        
        return (int) Math.floor(Math.min(this.getNetwork().getRequest(this).getWatts() * Compatibility.TO_IC2_RATIO, 100));
    }

    @Override
    public int injectEnergy(Direction directionFrom, int amount)
    {
        if (this.getNetwork() == null)
        {
            return amount;
        }
        
        return (int) Math.floor(this.getNetwork().produce(ElectricityPack.getFromWatts(amount, 120), VectorHelper.getTileEntityFromSide(this.worldObj, new Vector3(this), directionFrom.toForgeDirection())));
    }

    @Override
    public int getMaxSafeInput()
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public TileEntity[] getAdjacentConnections()
    {
        List<TileEntity> adjecentConnections = new ArrayList<TileEntity>();

        for (byte i = 0; i < 6; i++)
        {
            ForgeDirection side = ForgeDirection.getOrientation(i);
            TileEntity tileEntity = VectorHelper.getTileEntityFromSide(this.worldObj, new Vector3(this), side);
            
            if (tileEntity instanceof IConnector)
            {
                if (((IConnector) tileEntity).canConnect(side.getOpposite()))
                {
                    adjecentConnections.add(tileEntity);
                }
            }
            else if (Compatibility.isIndustrialCraft2Loaded() && tileEntity instanceof IEnergyTile)
            {
                if (tileEntity instanceof IEnergyAcceptor)
                {
                    if (((IEnergyAcceptor) tileEntity).acceptsEnergyFrom(this, Direction.values()[(i + 2) % 6].getInverse()))
                    {
                        adjecentConnections.add(tileEntity);
                    }
                }
                else
                {
                    adjecentConnections.add(tileEntity);
                }
            }
            else if (Compatibility.isBuildcraftLoaded() && tileEntity instanceof IPowerReceptor)
            {
                adjecentConnections.add(tileEntity);
            }
        }

        return adjecentConnections.toArray(new TileEntity[0]);
    }
    
    private class DummyPowerProvider extends PowerProvider
    {
        public IElectricityNetwork network;
        private final TileEntityUniversalConductor conductor;
        
        public DummyPowerProvider(IElectricityNetwork network, TileEntityUniversalConductor conductor)
        {
            this.network = network;
            this.conductor = conductor;
        }
        
        @Override
        public void receiveEnergy(float quantity, ForgeDirection from)
        {
            if (this.network != null)
            {
                this.network.produce(ElectricityPack.getFromWatts(this.getEnergyStored(), 120), VectorHelper.getTileEntityFromSide(this.conductor.worldObj, new Vector3(this.conductor), from));
            }
        }
    }
}
