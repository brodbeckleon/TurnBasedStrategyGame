package Game;

import java.awt.*;

public class Base {
    private final int baseID;
    private final int maxHealthPoints;
    private int healthPoints;
    private int armorValue;
    private final Point position;
    private final int deploymentRadius = 2;

    public Base (int baseID, Point position) {
        this.baseID = baseID;
        this.maxHealthPoints = 10000;
        this.healthPoints = maxHealthPoints;
        this.armorValue = 20;
        this.position = position;
    }

    public int getBaseID() {
        return baseID;
    }

    public int getHealthPoints() {
        return healthPoints;
    }
    public int getMaxHealthPoints() {
        return maxHealthPoints;
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
    public Point getPosition() {
        return position;
    }
    public int getDeploymentRadius() {
        return deploymentRadius;
    }

    public String toString() {
        String string = "";
        string += "Base: \t\t\t(" + getPosition().x + ", " + getPosition().y + ")" + "\n";
        string += "\t Health: \t" + getHealthPoints() + "/" + getMaxHealthPoints() +"\n";
        return string;
    }

}
