import java.awt.*;

public class Base {
    private final int maxHealthPoints;
    private int healthPoints;
    private int armorValue;
    private final int maxResourcePoints;
    private int resourcePoints;
    private final Point position;
    public Base (Point position) {
        this.maxHealthPoints = 10000;
        this.healthPoints = maxHealthPoints;
        this.armorValue = 20;
        this.maxResourcePoints = 100;
        this.resourcePoints = maxResourcePoints;
        this.position = position;
    }

    public int getHealthPoints() {
        return healthPoints;
    }

    public void setHealthPoints(int healthPoints) {
        this.healthPoints = healthPoints;
    }

    public int getArmorValue() {
        return armorValue;
    }

    public void setArmorValue(int armorValue) {
        this.armorValue = armorValue;
    }

    public int getResourcePoints() {
        return resourcePoints;
    }

    public void setResourcePoints(int resourcePoints) {
        this.resourcePoints = resourcePoints;
    }
}
