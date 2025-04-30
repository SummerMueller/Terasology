package org.terasology.engine.logic.players;

import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.engine.logic.characters.CharacterComponent;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.logic.players.LocalPlayer;
import org.terasology.engine.registry.In;

@RegisterSystem
public class PlayerTrackingSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final Logger logger = LoggerFactory.getLogger(PlayerTrackingSystem.class);

    @In
    private LocalPlayer localPlayer;

    @In
    private Time time;

    private float timeAccumulator = 0;

    @Override
    public void update(float delta) {
        timeAccumulator += delta;
        if (timeAccumulator < 1.0f) {
            return;  // log once per second
        }
        timeAccumulator = 0;

        EntityRef player = localPlayer.getCharacterEntity();
        if (!player.exists() || !player.hasComponent(LocationComponent.class)) return;

        LocationComponent loc = player.getComponent(LocationComponent.class);
        Vector3f pos = loc.getWorldPosition(new Vector3f());  // fixed to pass Vector3f
        Quaternionf rot = loc.getWorldRotation(new Quaternionf());  // fixed to pass Quaternionf

        Vector3f forward = rot.transform(new Vector3f(0, 0, -1)); // Facing direction
        String direction = getCompassDirection(forward);

        logger.info(String.format("Position: x = %.1f, y = %.1f, z = %.1f | Facing: %s", pos.x, pos.y, pos.z, direction));
    }

    private String getCompassDirection(Vector3f forward) {
        float angle = (float) Math.toDegrees(Math.atan2(forward.x, forward.z));
        if (angle < 0) angle += 360;

        if (angle >= 45 && angle < 135) return "East";
        else if (angle >= 135 && angle < 225) return "South";
        else if (angle >= 225 && angle < 315) return "West";
        else return "North";
    }
}
