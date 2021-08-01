package personthecat.catlib.exception;

import lombok.Getter;

import static personthecat.catlib.util.Shorthand.f;

@Getter
public class JsonMappingException extends RuntimeException {
    private final String parent;
    private final String field;

    public JsonMappingException(final String parent, final String field) {
        super(f("{}.{} is required", parent, field));
        this.parent = parent;
        this.field = field;
    }
}
