package calclavia.components;

import net.minecraft.creativetab.CreativeTabs;

public class ItemIngot extends ItemBase
{
	public ItemIngot(String name, int id)
	{
		super(name, id);
		this.setCreativeTab(CreativeTabs.tabMaterials);
	}
}