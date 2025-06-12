package personthecat.catlib.serialization.codec.capture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.junit.jupiter.api.Test;
import personthecat.catlib.command.annotations.Nullable;

import java.util.List;

import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.nullable;
import static personthecat.catlib.serialization.codec.capture.CapturingCodec.receiveType;
import static personthecat.catlib.serialization.codec.capture.CapturingCodec.suggestType;
import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.ofEnum;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaultTry;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.capture.CapturingCodec.capture;
import static personthecat.catlib.serialization.codec.capture.CapturingCodec.receive;
import static personthecat.catlib.serialization.codec.capture.CapturingCodec.supply;
import static personthecat.catlib.test.TestUtils.assertContains;
import static personthecat.catlib.test.TestUtils.assertError;
import static personthecat.catlib.test.TestUtils.assertSuccess;
import static personthecat.catlib.test.TestUtils.getMessage;
import static personthecat.catlib.test.TestUtils.parse;

public class CapturingCodecTest {

    @Test
    public void decode_withValueAtReceiver_returnsSuccess() {
        final var expected = new TestSubject(List.of(
            new Entry("a", "b")
        ));
        final var result = parse(TestSubject.CAPTURE, """
            entries: [
              { required: 'a', receiver: 'b' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withValueAtCaptor_returnsSuccess() {
        final var expected = new TestSubject(List.of(
            new Entry("c", "d")
        ));
        final var result = parse(TestSubject.CAPTURE, """
            captor: 'd'
            entries: [
              { required: 'c' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withValuesAtCaptorAndReceiver_usesValueAtReceiver() {
        final var expected = new TestSubject(List.of(
            new Entry("b", "c")
        ));
        final var result = parse(TestSubject.CAPTURE, """
            captor: 'a'
            entries: [
              { required: 'b', receiver: 'c' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withValueAtCaptor_whenValueIsNotReceived_returnsError() {
        final var result = parse(TestSubject.CAPTURE, """
            required: 'x'
            entries: [
              { receiver: 'y' }
            ]
            """);
        assertError(result);
        assertContains(getMessage(result), "No key required");
    }

    @Test
    public void decode_whenValuesAreMissing_returnsError() {
        final var result = parse(TestSubject.CAPTURE, """
            entries: [{}]
            """);
        assertError(result);
        assertContains(getMessage(result), "No key required");
        assertContains(getMessage(result), "No key receiver");
    }

    @Test
    public void decode_withValueAtCaptor_andMultipleReceivers_returnsSuccess() {
        final var expected = new TestSubject(List.of(
            new Entry("c", "d"),
            new Entry("b", "d")
        ));
        final var result = parse(TestSubject.CAPTURE, """
            captor: 'd'
            entries: [
              { required: 'c' }
              { required: 'b' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withValueSupplied_whenReceiverIsAbsent_usesSuppliedValue() {
        final var expected = new TestSubject(List.of(
            new Entry("x", TestSubject.SUPPLIED_VALUE)
        ));
        final var result = parse(TestSubject.SUPPLY, """
            entries: [
              { required: 'x' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withValueSupplied_whenReceiverIsPresent_usesGivenValue() {
        final var expected = new TestSubject(List.of(
            new Entry("y", "z")
        ));
        final var result = parse(TestSubject.SUPPLY, """
            entries: [
              { required: 'y', receiver: 'z' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withReceiver_withoutOverride_returnsError() {
        final var result = parse(TestSubject.CODEC, """
            entries: [
              { required: 'a' }
            ]
            """);
        assertError(result);
        assertContains(getMessage(result), "No key captor or default value supplied");
        assertContains(getMessage(result), "No key receiver");
    }

    @Test
    public void decode_withOverride_whenDefaultedIsAbsent_usesOverrideValue() {
        final var expected = new TestSubject(List.of(
            new Entry("q", "r", TestSubject.OVERRIDE_VALUE)
        ));
        final var result = parse(TestSubject.OVERRIDE, """
            entries: [
              { required: 'q', receiver: 'r' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withOverride_whenDefaultedIsPresent_usesGivenValue() {
        final var expected = new TestSubject(List.of(
            new Entry("s", "t", "u")
        ));
        final var result = parse(TestSubject.OVERRIDE, """
            entries: [
              { required: 's', receiver: 't', defaulted: 'u' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withDoubleOverride_whenDefaultedIsAbsent_usesTopOverride() {
        final var expected = new TestSubject(List.of(
            new Entry("g", "h", TestSubject.DOUBLE_OVERRIDE_VALUE)
        ));
        final var result = parse(TestSubject.DOUBLE_OVERRIDE, """
            entries: [
              { required: 'g', receiver: 'h' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withDoubleOverride_whenDefaultedIsPresent_usesGivenValue() {
        final var expected = new TestSubject(List.of(
            new Entry("p", "q", "r")
        ));
        final var result = parse(TestSubject.DOUBLE_OVERRIDE, """
            entries: [
              { required: 'p', receiver: 'q', defaulted: 'r' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withoutOverride_whenDefaultedIsAbsent_usesOriginalDefault() {
        final var expected = new TestSubject(List.of(
            new Entry("l", "m", Entry.DEFAULT_VALUE)
        ));
        final var result = parse(TestSubject.CODEC, """
            entries: [
              { required: 'l', receiver: 'm' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withCaptorOfDefaulted_withValueAtCaptor_usesGivenValue() {
        final var expected = new TestSubject(List.of(
            new Entry("h", "i", "j")
        ));
        final var result = parse(TestSubject.CAPTURE_OVERRIDE, """
            defaulted: 'j'
            entries: [
              { required: 'h', receiver: 'i' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withCaptorOfDefaulted_whenValueIsPresent_usesGivenValue() {
        final var expected = new TestSubject(List.of(
            new Entry("q", "r", "s")
        ));
        final var result = parse(TestSubject.CAPTURE_OVERRIDE, """
            entries: [
              { required: 'q', receiver: 'r', defaulted: 's' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withCaptorOfDefaulted_whenValueIsAbsent_usesOriginalDefault() {
        final var expected = new TestSubject(List.of(
            new Entry("t", "u", Entry.DEFAULT_VALUE)
        ));
        final var result = parse(TestSubject.CAPTURE_OVERRIDE, """
            entries: [
              { required: 't', receiver: 'u' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withTypeReceiver_withoutTypeOverride_whenTypeIsAbsent_returnsError() {
        final var result = parse(TypeTest.ANIMAL, """
            entries: [
              { animal: { name: 'MissingNo' } }
            ]
            """);
        assertError(result);
        assertContains(getMessage(result), "No default type suggested for key: animal");
    }

    @Test
    public void decode_withTypeOverride_whenTypeIsPresent_usesGivenType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Dog("Jonathan"))
        ));
        final var result = parse(TypeTest.TYPE_OVERRIDE, """
            entries: [
              { animal: { type: 'dog', name: 'Jonathan' } }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withTypeOverride_whenTypeIsAbsent_usesReceivedType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Cat("Billy"))
        ));
        final var result = parse(TypeTest.TYPE_OVERRIDE, """
            entries: [
              { animal: { name: 'Billy' } }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withTypeOverride_andMultipleReceivers_whenTypeIsAbsent_usesReceivedType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Cat("Bobby")),
            new AnimalEntry(new Cat("Jack"))
        ));
        final var result = parse(TypeTest.TYPE_OVERRIDE, """
            entries: [
              { animal: { name: 'Bobby' } }
              { animal: { name: 'Jack' } }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withTypeOverride_whenTypeIsAbsent_wrongKey_returnsError() {
        final var result = parse(TypeTest.TYPE_OVERRIDE, """
            entries: [
              { animal: { type: 'cat', name: 'Kitty' }, friend: { name: 'Cathrine' } }
            ]
            """);
        assertError(result);
        assertContains(getMessage(result), "Input does not contain a key");
    }

    @Test
    public void decode_withDoubleTypeOverride_whenTypeIsAbsent_usesTopOverride() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Dog("Jimmy"))
        ));
        final var result = parse(TypeTest.DOUBLE_TYPE_OVERRIDE, """
            entries: [
              { animal: { name: 'Jimmy' } }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withDoubleTypeOverride_whenTypeIsPresent_usesGivenType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Cat("Carol"))
        ));
        final var result = parse(TypeTest.DOUBLE_TYPE_OVERRIDE, """
            entries: [
              { animal: { type: 'cat', name: 'Carol' } }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withDefaultType_whenTypeIsPresent_usesGivenType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Dog("Karen"))
        ));
        // Type is suggested regardless of key (i.e. any animal object, regardless of field name)
        final var result = parse(TypeTest.DEFAULT_TYPE, """
            entries: [
              { animal: { type: 'dog', name: 'Karen' } }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withDefaultType_whenTypeIsAbsent_usesReceivedType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Cat("Griselda"))
        ));
        final var result = parse(TypeTest.DEFAULT_TYPE, """
            entries: [
              { animal: { name: 'Griselda' } }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withTypeOverride_ofListed_whenTypeIsPresent_usesGivenType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Dog("Benny"))
        ));
        final var result = parse(TypeTest.TYPE_OVERRIDE_LIST, """
            entries: [
              { animals: [{ type: 'dog', name: 'Benny' }] }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withTypeOverride_ofListed_whenTypeIsAbsent_usesReceivedType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Cat("Benjamin"))
        ));
        final var result = parse(TypeTest.TYPE_OVERRIDE_LIST, """
            entries: [
              { animals: [{ name: 'Benjamin' }] }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withTypeOverride_ofListed_andMultipleReceivers_whenTypeIsAbsent_usesReceivedType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Cat("Penelope")),
            new AnimalEntry(new Cat("Helga"))
        ));
        final var result = parse(TypeTest.TYPE_OVERRIDE_LIST, """
            entries: [
              { animals: [{ name: 'Penelope' }] }
              { animals: [{ name: 'Helga' }] }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withTypeOverride_ofListed_andNestedReceivers_whenTypeIsAbsent_usesReceivedType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(List.of(new Cat("Milly"), new Cat("Willy")))
        ));
        final var result = parse(TypeTest.TYPE_OVERRIDE_LIST, """
            entries: [
              { animals: [{ name: 'Milly' }, { name: 'Willy' }] }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withTypeOverride_ofListed_whenTypeIsAbsent_wrongKey_returnsError() {
        final var result = parse(TypeTest.TYPE_OVERRIDE_LIST, """
            entries: [
              { animals: [{ type: 'cat', name: 'Julius' }], friends: [{ name: 'Brutus' }] }
            ]
            """);
        assertError(result);
        assertContains(getMessage(result), "Input does not contain a key");
    }

    @Test
    public void decode_withDoubleTypeOverride_ofListed_whenTypeIsAbsent_usesTopOverride() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Dog("Frank"))
        ));
        final var result = parse(TypeTest.DOUBLE_TYPE_OVERRIDE_LIST, """
            entries: [
              { animals: [{ name: 'Frank' }] }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withDoubleTypeOverride_ofListed_whenTypeIsPresent_usesGivenType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Cat("Kali"))
        ));
        final var result = parse(TypeTest.DOUBLE_TYPE_OVERRIDE_LIST, """
            entries: [
              { animals: [{ type: 'cat', name: 'Kali' }] }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withDefaultType_ofListed_whenTypeIsPresent_usesGivenType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Dog("Ganesh"))
        ));
        // Type is suggested regardless of key (i.e. any animal object, regardless of field name)
        final var result = parse(TypeTest.DEFAULT_TYPE_LIST, """
            entries: [
              { animals: [{ type: 'dog', name: 'Ganesh' }] }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withDefaultType_ofListed_whenTypeIsAbsent_usesReceivedType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Cat("Vishnu"))
        ));
        final var result = parse(TypeTest.DEFAULT_TYPE_LIST, """
            entries: [
              { animals: [{ name: 'Vishnu' }] }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withTypeOverride_ofKeyless_whenTypeIsPresent_usesGivenType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Dog("Hank"))
        ));
        final var result = parse(TypeTest.TYPE_OVERRIDE_KEYLESS, """
            entries: [
              { animal: { type: 'dog', name: 'Hank' } }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withTypeOverride_ofKeyless_whenTypeIsAbsent_usesReceivedType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Cat("Marie"))
        ));
        final var result = parse(TypeTest.TYPE_OVERRIDE_KEYLESS, """
            entries: [
              { animal: { name: 'Marie' } }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withTypeOverride_ofKeyless_andMultipleReceivers_whenTypeIsAbsent_usesReceivedType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Cat("Heisenberg")),
            new AnimalEntry(new Cat("Pinkie"))
        ));
        final var result = parse(TypeTest.TYPE_OVERRIDE_KEYLESS, """
            entries: [
              { animal: { name: 'Heisenberg' } }
              { animal: { name: 'Pinkie' } }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withTypeOverride_ofKeyless_whenTypeIsAbsent_otherKey_usesReceivedType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Cat("Link"), new Cat("Zelda"))
        ));
        final var result = parse(TypeTest.TYPE_OVERRIDE_KEYLESS, """
            entries: [
              { animal: { type: 'cat', name: 'Link' }, friend: { name: 'Zelda' } }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withDoubleTypeOverride_ofKeyless_whenTypeIsAbsent_usesTopOverride() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Dog("Kim"))
        ));
        final var result = parse(TypeTest.DOUBLE_TYPE_OVERRIDE_KEYLESS, """
            entries: [
              { animal: { name: 'Kim' } }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withDoubleTypeOverride_ofKeyless_whenTypeIsPresent_usesGivenType() {
        final var expected = new TypeTest(List.of(
            new AnimalEntry(new Cat("Finger"))
        ));
        final var result = parse(TypeTest.DOUBLE_TYPE_OVERRIDE_KEYLESS, """
            entries: [
              { animal: { type: 'cat', name: 'Finger' } }
            ]
            """);
        assertSuccess(expected, result);
    }

    private record TestSubject(List<Entry> entries) {
        private static final String SUPPLIED_VALUE = "supplied_value";
        private static final String OVERRIDE_VALUE = "override_value";
        private static final String DOUBLE_OVERRIDE_VALUE = "double_override_value";
        private static final MapCodec<TestSubject> CODEC =
            Entry.CODEC.codec().listOf().fieldOf("entries").xmap(TestSubject::new, TestSubject::entries);

        private static final MapCodec<TestSubject> CAPTURE =
            CapturingCodec.builder().capturing(capture("captor", Codec.STRING)).build(CODEC);
        private static final MapCodec<TestSubject> SUPPLY =
            CapturingCodec.builder().capturing(supply("captor", SUPPLIED_VALUE)).build(CODEC);
        private static final MapCodec<TestSubject> OVERRIDE =
            CapturingCodec.builder().capturing(supply("defaulted", OVERRIDE_VALUE)).build(CODEC);
        private static final MapCodec<TestSubject> DOUBLE_OVERRIDE =
            CapturingCodec.builder().capturing(supply("defaulted", DOUBLE_OVERRIDE_VALUE)).build(OVERRIDE);
        private static final MapCodec<TestSubject> CAPTURE_OVERRIDE =
            CapturingCodec.builder().capturing(capture("defaulted", Codec.STRING)).build(CODEC);
    }

    private record Entry(String required, String receiver, String defaulted) {
        private static final String DEFAULT_VALUE = "default_value";
        private static final MapCodec<Entry> CODEC = codecOf(
            field(Codec.STRING, "required", Entry::required),
            defaultTry(Codec.STRING, "receiver", receive("captor"), Entry::receiver),
            defaultTry(Codec.STRING, "defaulted", receive("defaulted", DEFAULT_VALUE), Entry::defaulted),
            Entry::new
        );
        private Entry(String required, String receiver) {
            this(required, receiver, DEFAULT_VALUE);
        }
    }

    private record TypeTest(List<AnimalEntry> entries) {
        private static final MapCodec<TypeTest> ANIMAL =
            AnimalEntry.SINGLE.codec().listOf().fieldOf("entries").xmap(TypeTest::new, TypeTest::entries);
        private static final MapCodec<TypeTest> ANIMALS =
            AnimalEntry.MULTI.codec().listOf().fieldOf("entries").xmap(TypeTest::new, TypeTest::entries);
        private static final MapCodec<TypeTest> KEYLESS =
            AnimalEntry.KEYLESS.codec().listOf().fieldOf("entries").xmap(TypeTest::new, TypeTest::entries);

        private static final MapCodec<TypeTest> TYPE_OVERRIDE =
            CapturingCodec.builder().capturing(suggestType("animal", Animal.class, Cat.CODEC)).build(ANIMAL);
        private static final MapCodec<TypeTest> DOUBLE_TYPE_OVERRIDE =
            CapturingCodec.builder().capturing(suggestType("animal", Animal.class, Dog.CODEC)).build(TYPE_OVERRIDE);
        private static final MapCodec<TypeTest> DEFAULT_TYPE =
            CapturingCodec.builder().capturing(suggestType(Animal.class, Cat.CODEC)).build(ANIMAL);

        private static final MapCodec<TypeTest> TYPE_OVERRIDE_LIST =
            CapturingCodec.builder().capturing(suggestType("animals", Animal.class, Cat.CODEC)).build(ANIMALS);
        private static final MapCodec<TypeTest> DOUBLE_TYPE_OVERRIDE_LIST =
            CapturingCodec.builder().capturing(suggestType("animals", Animal.class, Dog.CODEC)).build(TYPE_OVERRIDE_LIST);
        private static final MapCodec<TypeTest> DEFAULT_TYPE_LIST =
            CapturingCodec.builder().capturing(suggestType(Animal.class, Cat.CODEC)).build(ANIMALS);

        private static final MapCodec<TypeTest> TYPE_OVERRIDE_KEYLESS =
            CapturingCodec.builder().capturing(suggestType(Animal.class, Cat.CODEC)).build(KEYLESS);
        private static final MapCodec<TypeTest> DOUBLE_TYPE_OVERRIDE_KEYLESS =
            CapturingCodec.builder().capturing(suggestType(Animal.class, Dog.CODEC)).build(TYPE_OVERRIDE_KEYLESS);
    }

    private record AnimalEntry(List<Animal> animals, List<Animal> friends) {
        private static final Codec<Animal> RECEIVER =
            receiveType("animal", Animal.CODEC);

        // deliberate single to cover .listOf() support
        private static final Codec<Animal> MULTI_RECEIVER =
            receiveType("animals", Animal.CODEC);

        // In a realistic application, this would directly wrap the original codec
        private static final Codec<Animal> KEYLESS_RECEIVER =
            receiveType(Animal.class).wrap(Animal.CODEC);

        private static final MapCodec<AnimalEntry> SINGLE = codecOf(
            field(RECEIVER, "animal", e -> e.animals.getFirst()),
            nullable(Animal.CODEC, "friend", e -> e.friends.getFirst()),
            AnimalEntry::new
        );
        private static final MapCodec<AnimalEntry> MULTI = codecOf(
            field(MULTI_RECEIVER.listOf(), "animals", AnimalEntry::animals),
            defaulted(Animal.CODEC.listOf(), "friends", List.of(), AnimalEntry::friends),
            AnimalEntry::new
        );
        private static final MapCodec<AnimalEntry> KEYLESS = codecOf(
            field(KEYLESS_RECEIVER, "animal", e -> e.animals.getFirst()),
            nullable(KEYLESS_RECEIVER, "friend", e -> e.friends.getFirst()),
            AnimalEntry::new
        );

        private AnimalEntry(List<Animal> animals) {
            this(animals, List.of());
        }

        private AnimalEntry(Animal animal) {
            this(List.of(animal), List.of());
        }

        private AnimalEntry(Animal animal, @Nullable Animal friend) {
            this(List.of(animal), friend != null ? List.of(friend) : List.of());
        }
    }

    private sealed interface Animal {
        Codec<Animal> CODEC =
            ofEnum(Type.class).dispatch(Animal::type, Animal::getCodec);

        Type type();

        static MapCodec<? extends Animal> getCodec(Type type) {
            return type == Type.CAT ? Cat.CODEC : Dog.CODEC;
        }

        enum Type { CAT, DOG }
    }

    record Cat(String name) implements Animal {
        static final MapCodec<Cat> CODEC =
            Codec.STRING.fieldOf("name").xmap(Cat::new, Cat::name);

        @Override
        public Type type() {
            return Type.CAT;
        }
    }

    record Dog(String name) implements Animal {
        static final MapCodec<Dog> CODEC =
            Codec.STRING.fieldOf("name").xmap(Dog::new, Dog::name);

        @Override
        public Type type() {
            return Type.DOG;
        }
    }
}
