package org.terasology.engine.logic.characters;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.input.binds.movement.ForwardsButton;
import org.terasology.engine.network.ClientComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.nui.NUIManager;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.input.ButtonState;


@RegisterSystem()
public class MovementInputSuppressorSystem extends BaseComponentSystem {

   @In
   private NUIManager nuiManager;

   @ReceiveEvent(components = ClientComponent.class)
   public void onInput(ForwardsButton event, EntityRef entity) {
      System.out.println("Tried to move");
      System.out.println(nuiManager.isOpen("engine:chat"));
      // Block movement if the chat UI is on top
      if (nuiManager.isOpen("engine:chat") && event.getState() == ButtonState.DOWN) {
         System.out.println("Tried to move when chatbox open");
         event.consume();  // Consuming the event blocks the movement input
      }
   }
}