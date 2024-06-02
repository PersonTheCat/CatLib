package personthecat.catlib.config.fabric;

import lombok.extern.log4j.Log4j2;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder;
import me.shedaniel.clothconfig2.impl.builders.AbstractListBuilder;
import me.shedaniel.clothconfig2.impl.builders.AbstractRangeFieldBuilder;
import me.shedaniel.clothconfig2.impl.builders.AbstractRangeListBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.config.CategoryValue;
import personthecat.catlib.config.Config;
import personthecat.catlib.config.ConfigUtil;
import personthecat.catlib.config.ConfigValue;
import personthecat.catlib.config.Validation;
import personthecat.catlib.config.Validation.DecimalRange;
import personthecat.catlib.config.Validation.NotNull;
import personthecat.catlib.config.Validation.Range;
import personthecat.catlib.config.Validation.Typed;
import personthecat.catlib.config.ValidationException;
import personthecat.catlib.config.ValidationMap;
import personthecat.catlib.config.Validations;
import personthecat.catlib.config.ValueException;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.exception.FormattedException;
import personthecat.catlib.serialization.json.XjsUtils;
import personthecat.catlib.util.LibStringUtils;
import personthecat.catlib.util.LibUtil;
import xjs.data.Json;
import xjs.data.JsonObject;
import xjs.data.JsonValue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Log4j2
@SuppressWarnings({"unchecked", "rawtypes"})
public class ClothConfigGenerator {
    private final ModDescriptor mod;
    private final File file;
    private final CategoryValue config;
    private final Object instance;
    private final ValidationMap validations;

    public ClothConfigGenerator(ModDescriptor mod, File file, CategoryValue config) {
        this.mod = mod;
        this.file = file;
        this.config = config;
        this.instance = config.parent().get(mod, null);
        this.validations = new ValidationMap();
    }

    public void loadConfig() {
        final JsonObject json = XjsUtils.readJson(this.file).orElse(null);
        if (json == null) {
            this.error(this.config, "Could not load file from disk.");
            return;
        }
        this.loadCategory(this.config, this.instance, json);
        this.fireOnConfigUpdated();
        this.saveConfig();
    }

    private void loadCategory(CategoryValue category, Object instance, JsonObject json) {
        for (final ConfigValue value : category.values()) {
            final Object o = value.get(this.mod, instance);
            final JsonValue j = json.get(value.name());
            if (j == null) {
                continue;
            }
            if (value instanceof CategoryValue c) {
                if (j.isObject()) {
                    this.loadCategory(c, o, j.asObject());
                } else {
                    this.warn(c, "Not an object. Expected category: " + j);
                }
            } else {
                this.setValue(value, instance, j);
            }
        }
    }

    private void setValue(ConfigValue value, Object instance, JsonValue j) {
        final Validations validations = this.getValidations(value);
        if (validations == null) {
            return;
        }
        final Object o;
        try {
            o = ConfigUtilImpl.remap(value.type(), validations.generics(), j.unwrap());
        } catch (final RuntimeException e) {
            this.warn(value, e.getMessage());
            return;
        }
        try {
            Validation.validate(validations.map().values(), this.filename(), value, o);
        } catch (final ValidationException e) {
            this.warn(e);
            return;
        }
        value.set(this.mod, instance, o);
    }

    private void saveConfig() {
        XjsUtils.writeJson(this.toJson(this.config, this.instance).asObject(), this.file)
            .ifErr(e -> this.error(this.config, "Could not save value to the disk."));
    }

    private JsonValue toJson(ConfigValue value, Object instance) {
        final String comment = this.getFullComment(value);
        final Object o = value.get(this.mod, instance);
        if (value instanceof CategoryValue category) {
            final JsonObject json = Json.object();
            if (comment != null) {
                json.setComment(comment);
            }
            for (final ConfigValue v : category.values()) {
                json.add(v.name(), toJson(v, o));
            }
            return json;
        }
        final JsonValue json = Json.any(o);
        if (comment != null) {
            json.setComment(comment);
        }
        return json;
    }

    private String getFullComment(ConfigValue value) {
        final String prefix = value.comment();
        if (prefix == null) {
            return null;
        }
        StringBuilder comment = new StringBuilder(prefix);
        for (final Validation<?> v : value.validations()) {
            if (v instanceof Range r) {
                comment.append(this.getRangeText(r));
                break;
            } else if (v instanceof DecimalRange r) {
                comment.append(this.getDecimalRangeText(r));
                break;
            }
        }
        if (value.type().isEnum()) {
            final String possible = Arrays.toString(value.type().getEnumConstants());
            comment.append("\nPossible values: ").append(possible);
        }
        return comment.toString();
    }

    private String getRangeText(Range r) {
        final long min = r.min();
        final long max = r.max();
        if (min == Long.MIN_VALUE) {
            if (max != Long.MAX_VALUE) {
                return "\nRange: < " + max;
            }
        } else if (max == Long.MAX_VALUE) {
            return "\nRange: > " + min;
        }
        return "\nRange: " + min + " ~ " + max;
    }

    private String getDecimalRangeText(DecimalRange r) {
        final double min = r.min();
        final double max = r.max();
        if (min == Double.MIN_VALUE) {
            if (max != Double.MAX_VALUE) {
                return "\nRange: < " + max;
            }
        } else if (max == Double.MAX_VALUE) {
            return "\nRange: > " + min;
        }
        return "\nRange: " + min + " ~ " + max;
    }

    private void fireOnConfigUpdated() {
        if (this.instance instanceof Config.Listener c) {
            try {
                c.onConfigUpdated();
            } catch (final ValidationException e) {
                LibErrorContext.error(this.mod, e);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public Screen createScreen(Screen parent) {
        return this.loadBuilder().setParentScreen(parent).build();
    }

    private ConfigBuilder loadBuilder() {
        final ConfigBuilder builder = ConfigBuilder.create();
        builder.setTitle(Component.literal(this.mod.getName()));
        builder.setSavingRunnable(() -> {
            this.fireOnConfigUpdated();
            this.saveConfig();
        });
        final List<ConfigValue> defaultValues = new ArrayList<>();
        for (final ConfigValue value : this.config.values()) {
            if (value instanceof CategoryValue source) {
                final Object o = source.get(this.mod, this.instance);
                this.addRootCategory(builder, source.name(), source.comment(), o, source.values());
            } else {
                defaultValues.add(value);
            }
        }
        if (!defaultValues.isEmpty()) {
            this.addRootCategory(builder, "default", null, this.instance, defaultValues);
        }
        return builder;
    }

    private void addRootCategory(
            ConfigBuilder builder, String name, @Nullable String description, Object instance, List<ConfigValue> values) {
        final ConfigCategory category = builder.getOrCreateCategory(this.getTitleName(name));
        if (description != null) {
            category.setDescription(new FormattedText[] { Component.literal(description) });
        }
        for (final ConfigValue value : values) {
            final AbstractConfigListEntry<?> entry = this.buildEntry(builder, instance, value);
            if (entry != null) {
                final String comment = value.comment();
                if (comment != null) {
                    category.addEntry(builder.entryBuilder().startTextDescription(Component.literal(comment)).build());
                }
                category.addEntry(entry);
            }
        }
    }

    private AbstractConfigListEntry<?> buildEntry(
            ConfigBuilder builder, Object instance, ConfigValue value) {
        final Object o = value.get(this.mod, instance);
        if (value instanceof CategoryValue source) {
            final SubCategoryBuilder category =
                builder.entryBuilder().startSubCategory(this.getTitleName(source));
            for (final ConfigValue v : source.values()) {
                final AbstractConfigListEntry<?> entry = this.buildEntry(builder, o, v);
                if (entry != null) {
                    final String comment = value.comment();
                    if (comment != null) {
                        category.add(builder.entryBuilder().startTextDescription(Component.literal(comment)).build());
                    }
                    category.add(entry);
                }
            }
            return category.build();
        }
        final Validations validations = this.getValidations(value);
        if (validations == null) return null;
        final EntryStub stub = new EntryStub(builder, value, instance, validations);
        if (value.type().isEnum()) return this.buildEnum(stub);
        if (value.type().isArray()) return this.buildArray(stub, validations);
        if (o == null) return null;
        if (ConfigUtil.isBoolean(value.type())) return this.buildBoolean(stub);
        if (ConfigUtil.isInteger(value.type())) return this.buildInt(stub, validations);
        if (ConfigUtil.isLong(value.type())) return this.buildLong(stub, validations);
        if (ConfigUtil.isFloat(value.type())) return this.buildFloat(stub, validations);
        if (ConfigUtil.isDouble(value.type())) return this.buildDouble(stub, validations);
        if (String.class.isAssignableFrom(value.type())) return this.buildString(stub);
        if (Set.class.isAssignableFrom(value.type())) return this.buildSet(stub, validations);
        if (List.class.isAssignableFrom(value.type())) return this.buildList(stub, validations);
        if (Collection.class.isAssignableFrom(value.type())) return this.buildCollection(stub, validations);
        return null;
    }

    private AbstractConfigListEntry<?> buildEnum(EntryStub stub) {
        if (stub.value.type().getEnumConstants().length < 4) {
            return this.buildEnumToggle(stub);
        }
        return this.buildEnumDropdown(stub);
    }

    private AbstractConfigListEntry<?> buildEnumToggle(EntryStub stub) {
        return stub.startBuilding((builder, value, current) ->
                builder.startEnumSelector(this.getTitleName(value), (Class<Enum>) value.type(), (Enum) current)
                    .setEnumNameProvider(this::getTitleName))
            .skipValidations(Typed.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildEnumDropdown(EntryStub stub) {
        return stub.startBuilding((builder, value, current) ->
                builder.startDropdownMenu(
                    this.getTitleName(value),
                    DropdownMenuBuilder.TopCellElementBuilder.of(
                        (Enum) current,
                        s -> (Enum) LibUtil.getEnumConstant(s, (Class<Enum>) value.type()).orElse(null),
                        this::getTitleName
                    ),
                    DropdownMenuBuilder.CellCreatorBuilder.of(this::getTitleName))
                .setSelections(List.of(((Class<Enum>) value.type()).getEnumConstants())))
            .skipValidations()
            .build();
    }

    private AbstractConfigListEntry<?> buildArray(EntryStub stub, Validations validations) {
        final Class<?> componentType = stub.value.type().componentType();
        return this.buildAnyList(stub, validations, componentType, ConfigUtil::arrayToList,
            l -> ConfigUtil.listToArray(l, componentType));
    }

    private AbstractConfigListEntry<?> buildBoolean(EntryStub stub) {
        return stub
            .setInputMapper((value) -> value != null ? value : false)
            .startBuilding((builder, value, current) ->
                builder.startBooleanToggle(this.getTitleName(value), (Boolean) current))
            .skipValidations(Typed.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildInt(EntryStub stub, Validations validations) {
        return stub.setInputMapper(i -> i != null ? i : 0)
            .startBuilding((builder, value, current) ->
                builder.startIntField(this.getTitleName(value), ((Number) current).intValue()))
            .setBounds(resolveIntBounds(stub.value.type(), validations))
            .setOutputMapper((value, i) -> ConfigUtil.toCorrectPrimitive(value.type(), i))
            .skipValidations(Range.class, Typed.class, NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildLong(EntryStub stub, Validations validations) {
        return stub.setInputMapper(l -> l != null ? l : 0)
            .startBuilding((builder, value, current) ->
                builder.startLongField(this.getTitleName(value), ((Number) current).longValue()))
            .setBounds(resolveLongBounds(validations))
            .skipValidations(Range.class, Typed.class, NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildFloat(EntryStub stub, Validations validations) {
        return stub.setInputMapper(f -> f != null ? f : 0)
            .startBuilding((builder, value, current) ->
                builder.startFloatField(this.getTitleName(value), ((Number) current).floatValue()))
            .setBounds(resolveFloatBounds(validations))
            .skipValidations(DecimalRange.class, Typed.class, NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildDouble(EntryStub stub, Validations validations) {
        return stub.setInputMapper(f -> f != null ? f : 0)
            .startBuilding((builder, value, current) ->
                builder.startDoubleField(this.getTitleName(value), ((Number) current).doubleValue()))
            .setBounds(resolveDoubleBounds(validations))
            .skipValidations(DecimalRange.class, Typed.class, NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildString(EntryStub stub) {
        return stub.startBuilding((builder, value, current) ->
                builder.startStrField(this.getTitleName(value), (String) current))
            .skipValidations(Typed.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildSet(EntryStub stub, Validations validations) {
        final Class<?> genericType = validations.generics()[0];
        return this.buildAnyList(stub, validations, genericType, o -> new ArrayList<>((Set) o), HashSet::new);
    }

    private AbstractConfigListEntry<?> buildList(EntryStub stub, Validations validations) {
        final Class<?> genericType = validations.generics()[0];
        return this.buildAnyList(stub, validations, genericType, o -> (List) o, (List o) -> o);
    }

    private AbstractConfigListEntry<?> buildCollection(EntryStub stub, Validations validations) {
        final Class<?> genericType = validations.generics()[0];
        return this.buildAnyList(stub, validations, genericType, o -> new ArrayList<>((Collection) o), (List o) -> o);
    }

    private AbstractConfigListEntry<?> buildAnyList(
            EntryStub stub, Validations validations, Class<?> type, Function<Object, List> toList, Function<List, Object> fromList) {
        if (ConfigUtil.isInteger(type))
            return this.buildIntList(stub, type, validations, toList, fromList);
        if (ConfigUtil.isLong(type))
            return this.buildLongList(stub, validations, toList, fromList);
        if (ConfigUtil.isFloat(type))
            return this.buildFloatList(stub, validations, toList, fromList);
        if (ConfigUtil.isDouble(type))
            return this.buildDoubleList(stub, validations, toList, fromList);
        if (String.class.isAssignableFrom(type))
            return this.buildStringList(stub, toList, fromList);
        return null;
    }

    private AbstractConfigListEntry<?> buildIntList(
            EntryStub stub, Class<?> type, Validations validations, Function<Object, List> toList, Function<List, Object> fromList) {
        return stub.startBuilding((builder, value, current) ->
                builder.startIntList(this.getTitleName(value), toList.apply(current)))
            .setBounds(resolveIntBounds(type, validations))
            .setOutputMapper((value, list) -> fromList.apply((List) list))
            .skipValidations(Range.class, Typed.class, NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildLongList(
            EntryStub stub, Validations validations, Function<Object, List> toList, Function<List, Object> fromList) {
        return stub.startBuilding((builder, value, current) ->
                builder.startLongList(this.getTitleName(value), toList.apply(current)))
            .setBounds(resolveLongBounds(validations))
            .setOutputMapper((value, list) -> fromList.apply((List) list))
            .skipValidations(Range.class, Typed.class, NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildFloatList(
            EntryStub stub, Validations validations, Function<Object, List> toList, Function<List, Object> fromList) {
        return stub.startBuilding((builder, value, current) ->
                builder.startFloatList(this.getTitleName(value), toList.apply(current)))
            .setBounds(resolveFloatBounds(validations))
            .setOutputMapper((value, list) -> fromList.apply((List) list))
            .skipValidations(DecimalRange.class, Typed.class, NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildDoubleList(
            EntryStub stub, Validations validations, Function<Object, List> toList, Function<List, Object> fromList) {
        return stub.startBuilding((builder, value, current) ->
                builder.startDoubleList(this.getTitleName(value), toList.apply(current)))
            .setBounds(resolveDoubleBounds(validations))
            .setOutputMapper((value, list) -> fromList.apply((List) list))
            .skipValidations(DecimalRange.class, Typed.class, NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildStringList(
            EntryStub stub, Function<Object, List> toList, Function<List, Object> fromList) {
        return stub.startBuilding((builder, value, current) ->
                builder.startStrList(this.getTitleName(value), toList.apply(current)))
            .setOutputMapper((value, list) -> fromList.apply((List) list))
            .skipValidations(Typed.class)
            .build();
    }

    private Validations getValidations(ConfigValue value) {
        if (this.validations.containsKey(value)) {
            return this.validations.get(value);
        }
        try {
            final Validations validations = Validations.fromValue(this.filename(), value);
            this.warnIfInvalid(value, validations);
            this.validations.put(value, validations);
            return validations;
        } catch (final ValueException e) {
            this.error(e);
            this.validations.put(value, null);
            return null;
        }
    }

    private void warnIfInvalid(ConfigValue value, Validations validations) {
        for (final Validation<?> v : validations.map().values()) {
            if (!v.isValidForType(value.type())) {
                this.warn(value, "Not valid for type: " + v + " on " + value.name());
            }
        }
    }

    private Component getTitleName(ConfigValue value) {
        return this.getTitleName(value.name());
    }

    private Component getTitleName(Enum<?> e) {
        return Component.literal(LibStringUtils.toTitleCase(e.name().toLowerCase(), false));
    }

    private Component getTitleName(String name) {
        return Component.literal(LibStringUtils.toTitleCase(name, true));
    }

    private String filename() {
        return this.config.parent().name();
    }

    private void error(ConfigValue value, String message) {
        this.error(new ValueException(message, this.filename(), value));
    }

    private void error(FormattedException e) {
        LibErrorContext.error(this.mod, e);
    }

    private void warn(ConfigValue value, String message) {
        this.warn(new ValueException(message, this.filename(), value));
    }

    private void warn(FormattedException e) {
        LibErrorContext.warn(this.mod, e);
    }

    private static Bounds resolveIntBounds(Class<?> type, Validations validations) {
        final Range range = validations.get(Range.class, () -> Validation.range(type));
        return new Bounds(
            range.min() != Long.MIN_VALUE ? (int) range.min() : null,
            range.max() != Long.MAX_VALUE ? (int) range.max() : null);
    }

    private static Bounds resolveLongBounds(Validations validations) {
        final Range range = validations.get(Range.class);
        return new Bounds(
            range != null && range.min() != Long.MIN_VALUE ? range.min() : null,
            range != null && range.max() != Long.MAX_VALUE ? range.max() : null);
    }

    private static Bounds resolveFloatBounds(Validations validations) {
        final DecimalRange range = validations.get(DecimalRange.class, () -> Validation.FLOAT_RANGE);
        return new Bounds(
            range.min() != Double.MIN_VALUE ? (float) range.min() : null,
            range.max() != Double.MAX_VALUE ? (float) range.max() : null);
    }

    private static Bounds resolveDoubleBounds(Validations validations) {
        final DecimalRange range = validations.get(DecimalRange.class);
        return new Bounds(
            range != null && range.min() != Double.MIN_VALUE ? range.min() : null,
            range != null && range.max() != Double.MAX_VALUE ? range.max() : null);
    }

    private class EntryStub {
        private final ConfigBuilder builder;
        private final ConfigValue value;
        private final Object instance;
        private final Map<Class<?>, Validation<?>> validationMap;
        private FieldBuilder fieldBuilder;
        private Function inputMapper;
        private BiFunction<ConfigValue, Object, Object> outputMapper;

        EntryStub(ConfigBuilder builder, ConfigValue value, Object instance, Validations validations) {
            this.builder = builder;
            this.value = value;
            this.instance = instance;
            this.validationMap = new HashMap<>(validations.map());
        }

        EntryStub setInputMapper(Function mapper) {
            this.inputMapper = mapper;
            return this;
        }

        EntryStub startBuilding(StartBuildingFunction f) {
            this.fieldBuilder = f.createBuilder(this.builder.entryBuilder(), this.value, this.getCurrent());
            return this;
        }

        Object getCurrent() {
            final Object o = this.value.get(ClothConfigGenerator.this.mod, this.instance);
            return this.inputMapper != null ? this.inputMapper.apply(o) : o;
        }

        EntryStub setOutputMapper(BiFunction<ConfigValue, Object, Object> mapper) {
            this.outputMapper = mapper;
            return this;
        }

        EntryStub setBounds(Bounds bounds) {
            Objects.requireNonNull(this.fieldBuilder, "Never started building");
            if (this.fieldBuilder instanceof AbstractRangeFieldBuilder rangeField) {
                rangeField.setMin(bounds.min);
                rangeField.setMax(bounds.max);
            } else if (this.fieldBuilder instanceof AbstractRangeListBuilder rangeList) {
                rangeList.setMin(bounds.min);
                rangeList.setMax(bounds.max);
            }
            return this;
        }

        EntryStub skipValidations(Class<?>... types) {
            for (final Class<?> c : types) {
                this.validationMap.remove(c);
            }
            return this;
        }

        EntryStub skipValidations() {
            this.validationMap.clear();
            return this;
        }

        @Nullable AbstractConfigListEntry<?> build() {
            Objects.requireNonNull(this.fieldBuilder, "Never started building");
            final ModDescriptor mod = ClothConfigGenerator.this.mod;
            final BiFunction<ConfigValue, Object, Object> outputMapper = this.outputMapper;
            final ConfigValue value = this.value;
            final Object instance = this.instance;

            this.applyDefaultValue(value::defaultValue);
            this.applyRequireRestart(value.needsWorldRestart());

            if (outputMapper == null) {
                this.applySaveConsumer(o -> value.set(mod, instance, o));
            } else {
                this.applySaveConsumer(o -> value.set(mod, instance, outputMapper.apply(value, o)));
            }
            if (!this.validationMap.isEmpty()) {
                final Collection<Validation<?>> validations = this.validationMap.values();
                this.applyErrorSupplier(new MemoizedErrorSupplier<>(value, validations));
            }
            return this.fieldBuilder.build();
        }

        // sadly, there is no common interface for any of these methods
        void applyDefaultValue(Supplier<Object> defaultValue) {
            if (this.fieldBuilder instanceof AbstractFieldBuilder afb) {
                afb.setDefaultValue(defaultValue);
            } else if (this.fieldBuilder instanceof DropdownMenuBuilder dmb) {
                dmb.setDefaultValue(defaultValue);
            }
        }

        void applyRequireRestart(boolean requireRestart) {
            this.fieldBuilder.requireRestart(requireRestart);
        }

        void applySaveConsumer(Consumer<Object> saveConsumer) {
            if (this.fieldBuilder instanceof AbstractFieldBuilder afb) {
                afb.setSaveConsumer(saveConsumer);
            } else if (this.fieldBuilder instanceof DropdownMenuBuilder dmb) {
                dmb.setSaveConsumer(saveConsumer);
            }
        }

        void applyErrorSupplier(Function<Object, Optional<Component>> errorSupplier) {
            if (this.fieldBuilder instanceof AbstractListBuilder alb) {
              alb.setCellErrorSupplier(errorSupplier);
            } else if (this.fieldBuilder instanceof AbstractFieldBuilder afb) {
                afb.setErrorSupplier(errorSupplier);
            } else if (this.fieldBuilder instanceof DropdownMenuBuilder dmb) {
                dmb.setErrorSupplier(errorSupplier);
            }
        }
    }

    @FunctionalInterface
    private interface StartBuildingFunction {
        FieldBuilder createBuilder(ConfigEntryBuilder builder, ConfigValue value, Object current);
    }

    private record Bounds(@Nullable Number min, @Nullable Number max) {}

    private class MemoizedErrorSupplier<T> implements Function<T, Optional<Component>> {
        private final ConfigValue value;
        private final Collection<Validation<?>> validations;
        private Object input;
        private Object output;

        MemoizedErrorSupplier(ConfigValue value, Collection<Validation<?>> validations) {
            this.value = value;
            this.validations = validations;
        }

        @Override
        public Optional<Component> apply(T t) {
            if (this.output == null || t != this.input) {
                this.input = t;
                try {
                    Validation.validate(this.validations, ClothConfigGenerator.this.filename(), this.value, t);
                    this.output = Optional.empty();
                } catch (final ValidationException e) {
                    this.output = Optional.of(e.getTitleMessage());
                }
            }
            return (Optional<Component>) this.output;
        }
    }
}
