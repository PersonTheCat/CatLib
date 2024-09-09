package personthecat.catlib.config.fabric;

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
import personthecat.catlib.config.ConfigUtil;
import personthecat.catlib.config.ConfigValue;
import personthecat.catlib.config.Validation;
import personthecat.catlib.config.ValidationException;
import personthecat.catlib.config.Validations;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.util.LibStringUtils;
import personthecat.catlib.util.LibUtil;

import java.util.ArrayList;
import java.util.Collection;
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

@Environment(EnvType.CLIENT)
@SuppressWarnings({"unchecked", "rawtypes"})
public record ClothScreenGenerator(ClothConfigGenerator generator) {

    public Screen buildScreen(Screen parent) {
        return this.loadBuilder().setParentScreen(parent).build();
    }

    private ConfigBuilder loadBuilder() {
        final ConfigBuilder builder = ConfigBuilder.create();
        final ClothConfigGenerator generator = this.generator;
        builder.setTitle(Component.literal(generator.getMod().getName()));
        builder.setSavingRunnable(() -> {
            generator.fireOnConfigUpdated();
            generator.saveConfig();
        });
        final List<ConfigValue> defaultValues = new ArrayList<>();
        for (final ConfigValue value : generator.getConfig().values()) {
            if (value instanceof CategoryValue source) {
                final Object o = source.get(generator.getMod(), generator.getInstance());
                this.addRootCategory(builder, source.name(), source.comment(), o, source.values());
            } else {
                defaultValues.add(value);
            }
        }
        if (!defaultValues.isEmpty()) {
            this.addRootCategory(builder, "default", null, generator.getInstance(), defaultValues);
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
        final Object o = value.get(this.generator.getMod(), instance);
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
        final Validations validations = this.generator.getValidations(value);
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
            .skipValidations(Validation.Typed.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildEnumDropdown(EntryStub stub) {
        return stub.startBuilding((builder, value, current) ->
                builder.startDropdownMenu(
                        this.getTitleName(value),
                        DropdownMenuBuilder.TopCellElementBuilder.of(
                            (Enum) current,
                            s -> (Enum<?>) LibUtil.getEnumConstant(s, (Class<Enum>) value.type()).orElse(null),
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
        return stub.setInputMapper((value) -> value != null ? value : false)
            .startBuilding((builder, value, current) ->
                builder.startBooleanToggle(this.getTitleName(value), (Boolean) current))
            .skipValidations(Validation.Typed.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildInt(EntryStub stub, Validations validations) {
        return stub.setInputMapper(i -> i != null ? i : 0)
            .startBuilding((builder, value, current) ->
                builder.startIntField(this.getTitleName(value), ((Number) current).intValue()))
            .setBounds(resolveIntBounds(stub.value.type(), validations))
            .setOutputMapper((value, i) -> ConfigUtil.toCorrectPrimitive(value.type(), i))
            .skipValidations(Validation.Range.class, Validation.Typed.class, Validation.NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildLong(EntryStub stub, Validations validations) {
        return stub.setInputMapper(l -> l != null ? l : 0L)
            .startBuilding((builder, value, current) ->
                builder.startLongField(this.getTitleName(value), ((Number) current).longValue()))
            .setBounds(resolveLongBounds(validations))
            .skipValidations(Validation.Range.class, Validation.Typed.class, Validation.NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildFloat(EntryStub stub, Validations validations) {
        return stub.setInputMapper(f -> f != null ? f : 0F)
            .startBuilding((builder, value, current) ->
                builder.startFloatField(this.getTitleName(value), ((Number) current).floatValue()))
            .setBounds(resolveFloatBounds(validations))
            .skipValidations(Validation.DecimalRange.class, Validation.Typed.class, Validation.NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildDouble(EntryStub stub, Validations validations) {
        return stub.setInputMapper(f -> f != null ? f : 0D)
            .startBuilding((builder, value, current) ->
                builder.startDoubleField(this.getTitleName(value), ((Number) current).doubleValue()))
            .setBounds(resolveDoubleBounds(validations))
            .skipValidations(Validation.DecimalRange.class, Validation.Typed.class, Validation.NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildString(EntryStub stub) {
        return stub.startBuilding((builder, value, current) ->
                builder.startStrField(this.getTitleName(value), (String) current))
            .skipValidations(Validation.Typed.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildSet(EntryStub stub, Validations validations) {
        return this.buildAnyList(stub, validations, validations.genericType(), o -> new ArrayList<>((Set) o), HashSet::new);
    }

    private AbstractConfigListEntry<?> buildList(EntryStub stub, Validations validations) {
        return this.buildAnyList(stub, validations, validations.genericType(), o -> (List) o, (List o) -> o);
    }

    private AbstractConfigListEntry<?> buildCollection(EntryStub stub, Validations validations) {
        return this.buildAnyList(stub, validations, validations.genericType(), o -> new ArrayList<>((Collection) o), (List o) -> o);
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
        return stub.setInputMapper(toList)
            .startBuilding((builder, value, current) ->
                builder.startIntList(this.getTitleName(value), (List) current))
            .setBounds(resolveIntBounds(type, validations))
            .setOutputMapper((value, list) -> fromList.apply((List) list))
            .skipValidations(Validation.Range.class, Validation.Typed.class, Validation.NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildLongList(
            EntryStub stub, Validations validations, Function<Object, List> toList, Function<List, Object> fromList) {
        return stub.setInputMapper(toList)
            .startBuilding((builder, value, current) ->
                builder.startLongList(this.getTitleName(value), (List) current))
            .setBounds(resolveLongBounds(validations))
            .setOutputMapper((value, list) -> fromList.apply((List) list))
            .skipValidations(Validation.Range.class, Validation.Typed.class, Validation.NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildFloatList(
            EntryStub stub, Validations validations, Function<Object, List> toList, Function<List, Object> fromList) {
        return stub.setInputMapper(toList)
            .startBuilding((builder, value, current) ->
                builder.startFloatList(this.getTitleName(value), (List) current))
            .setBounds(resolveFloatBounds(validations))
            .setOutputMapper((value, list) -> fromList.apply((List) list))
            .skipValidations(Validation.DecimalRange.class, Validation.Typed.class, Validation.NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildDoubleList(
            EntryStub stub, Validations validations, Function<Object, List> toList, Function<List, Object> fromList) {
        return stub.setInputMapper(toList)
            .startBuilding((builder, value, current) ->
                builder.startDoubleList(this.getTitleName(value), (List) current))
            .setBounds(resolveDoubleBounds(validations))
            .setOutputMapper((value, list) -> fromList.apply((List) list))
            .skipValidations(Validation.DecimalRange.class, Validation.Typed.class, Validation.NotNull.class)
            .build();
    }

    private AbstractConfigListEntry<?> buildStringList(
            EntryStub stub, Function<Object, List> toList, Function<List, Object> fromList) {
        return stub.startBuilding((builder, value, current) ->
                builder.startStrList(this.getTitleName(value), toList.apply(current)))
            .setOutputMapper((value, list) -> fromList.apply((List) list))
            .skipValidations(Validation.Typed.class)
            .build();
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

    private static Bounds resolveIntBounds(Class<?> type, Validations validations) {
        final Validation.Range range = validations.get(Validation.Range.class, () -> Validation.range(type));
        return new Bounds(
            range.min() != Long.MIN_VALUE ? (int) range.min() : null,
            range.max() != Long.MAX_VALUE ? (int) range.max() : null);
    }

    private static Bounds resolveLongBounds(Validations validations) {
        final Validation.Range range = validations.get(Validation.Range.class);
        return new Bounds(
            range != null && range.min() != Long.MIN_VALUE ? range.min() : null,
            range != null && range.max() != Long.MAX_VALUE ? range.max() : null);
    }

    private static Bounds resolveFloatBounds(Validations validations) {
        final Validation.DecimalRange range = validations.get(Validation.DecimalRange.class, () -> Validation.FLOAT_RANGE);
        return new Bounds(
            range.min() != -Double.MAX_VALUE ? (float) range.min() : null,
            range.max() != Double.MAX_VALUE ? (float) range.max() : null);
    }

    private static Bounds resolveDoubleBounds(Validations validations) {
        final Validation.DecimalRange range = validations.get(Validation.DecimalRange.class);
        return new Bounds(
            range != null && range.min() != -Double.MAX_VALUE ? range.min() : null,
            range != null && range.max() != Double.MAX_VALUE ? range.max() : null);
    }

    private class EntryStub {
        private final ConfigBuilder builder;
        private final ConfigValue value;
        private final Object instance;
        private final Validations validations;
        private FieldBuilder fieldBuilder;
        private Function inputMapper;
        private BiFunction<ConfigValue, Object, Object> outputMapper;

        EntryStub(ConfigBuilder builder, ConfigValue value, Object instance, Validations validations) {
            this.builder = builder;
            this.value = value;
            this.instance = instance;
            this.validations = validations.cloneValidations();
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
            final Object o = this.value.get(generator.getMod(), this.instance);
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

        EntryStub skipValidations(Class<? extends Validation>... types) {
            for (final Class<? extends Validation> c : types) {
                this.validations.take(c);
            }
            return this;
        }

        EntryStub skipValidations() {
            this.validations.clear();
            return this;
        }

        @Nullable AbstractConfigListEntry<?> build() {
            Objects.requireNonNull(this.fieldBuilder, "Never started building");
            final ModDescriptor mod = generator.getMod();
            final Function<Object, Object> inputMapper = this.inputMapper;
            final BiFunction<ConfigValue, Object, Object> outputMapper = this.outputMapper;
            final ConfigValue value = this.value;
            final Object instance = this.instance;

            if (inputMapper != null) {
                this.applyDefaultValue(() -> inputMapper.apply(value.defaultValue()));
            } else {
                this.applyDefaultValue(value::defaultValue);
            }
            this.applyRequireRestart(value.needsWorldRestart());

            if (outputMapper == null) {
                this.applySaveConsumer(o -> value.set(mod, instance, o));
            } else {
                this.applySaveConsumer(o -> value.set(mod, instance, outputMapper.apply(value, o)));
            }
            if (!this.validations.isEmpty()) {
                final Map<Class<?>, Validation<?>> validations = ConfigUtil.isSupportedGenericType(value.type())
                    ? this.validations.map() : this.validations.entryValidations();
                this.applyErrorSupplier(new MemoizedErrorSupplier<>(value, validations.values()));
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
                    Validation.validate(this.validations, generator.filename(), this.value, t);
                    this.output = Optional.empty();
                } catch (final ValidationException e) {
                    this.output = Optional.of(e.getTitleMessage());
                }
            }
            return (Optional<Component>) this.output;
        }
    }
}
