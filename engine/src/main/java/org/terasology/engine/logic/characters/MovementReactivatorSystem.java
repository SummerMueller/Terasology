package org.terasology.engine.logic.characters;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.input.binds.movement.ForwardsButton;
import org.terasology.engine.input.binds.movement.BackwardsButton;
import org.terasology.engine.input.binds.movement.LeftStrafeButton;
import org.terasology.engine.input.binds.movement.RightStrafeButton;
import org.terasology.engine.logic.characters.events.SetMovementModeEvent;
import org.terasology.engine.logic.characters.MovementMode;
import org.terasology.engine.network.ClientComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.nui.NUIManager;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.input.ButtonState;

@RegisterSystem
public class MovementReactivatorSystem extends BaseComponentSystem {

    @In
    private NUIManager nuiManager;

    @ReceiveEvent(components = ClientComponent.class)
    public void onMovementKeyPressed(ForwardsButton event, EntityRef entity) {
        tryReenableMovement(event, entity);
    }

    @ReceiveEvent(components = ClientComponent.class)
    public void onMovementKeyPressed(BackwardsButton event, EntityRef entity) {
        tryReenableMovement(event, entity);
    }

    @ReceiveEvent(components = ClientComponent.class)
    public void onMovementKeyPressed(LeftStrafeButton event, EntityRef entity) {
        tryReenableMovement(event, entity);
    }

    @ReceiveEvent(components = ClientComponent.class)
    public void onMovementKeyPressed(RightStrafeButton event, EntityRef entity) {
        tryReenableMovement(event, entity);
    }

    private void tryReenableMovement(ButtonState state, EntityRef entity) {
        if (!nuiManager.isOpen("engine:chat") && state == ButtonState.DOWN) {
            EntityRef character = entity.getComponent(ClientComponent.class).character;
            if (character.exists()) {
                character.send(new SetMovementModeEvent(MovementMode.WALKING));
            }
        }
    }

    // Overloaded helper for convenience
    private void tryReenableMovement(org.terasology.engine.input.BindButtonEvent event, EntityRef entity) {
        tryReenableMovement(event.getState(), entity);
    }
}
