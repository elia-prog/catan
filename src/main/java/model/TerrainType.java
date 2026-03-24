package model;

public enum TerrainType {
    FOREST(ResourceType.WOOD),
    HILLS(ResourceType.BRICK),
    PASTURE(ResourceType.SHEEP),
    FIELDS(ResourceType.WHEAT),
    MOUNTAINS(ResourceType.ORE),
    DESERT(ResourceType.NONE),
    WATER_TILE(ResourceType.NONE);

    private final ResourceType resource;

    // בנאי (Constructor) שמקשר בין השטח למשאב
    TerrainType(ResourceType resource) {
        this.resource = resource;
    }

    public ResourceType getResource() {
        return resource;
    }
}