package universalelectricity.simulator.dc

import universalelectricity.api.core.grid.IUpdate
import universalelectricity.simulator.dc.component.SeriesComponent
import universalelectricity.simulator.grid.LinkedGrid

/**
 * Basic network of parts that function together to simulate a collection of co-existing tiles.
 * @author Darkguardsman, Calclavia
 */
class DCGrid extends LinkedGrid[DCNode](classOf[DCNode]) with IUpdate
{
  private var circuit: SeriesComponent = _

  /** Called each update to simulate changes */
  def updateSimulation()
  {
    circuit.solve()
  }
}