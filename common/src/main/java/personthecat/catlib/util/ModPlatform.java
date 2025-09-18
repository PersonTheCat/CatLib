package personthecat.catlib.util;

public enum ModPlatform {
    FABRIC,
    FORGE,
    QUILT,
    NEO_FORGE;

    public String id() {
        return this.name().toLowerCase();
    }

    public String formatted() {
        return switch (this) {
            case FABRIC -> "Fabric";
            case FORGE -> "Forge";
            case QUILT -> "Quilt";
            case NEO_FORGE -> "NeoForge";
        };
    }

    public boolean isFabricLike() {
        return !this.isForgeLike();
    }

    public boolean isForgeLike() {
        return switch (this) {
            case FORGE, NEO_FORGE -> true;
            default -> false;
        };
    }
}
