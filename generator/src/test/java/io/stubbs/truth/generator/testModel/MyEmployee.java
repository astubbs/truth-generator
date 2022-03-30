package io.stubbs.truth.generator.testModel;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

import static io.stubbs.truth.generator.testModel.MyEmployee.State.EMPLOLYED;
import static io.stubbs.truth.generator.testModel.MyEmployee.State.IS_A_BOSS;

/**
 * @author Antony Stubbs
 */
@SuperBuilder(toBuilder = true)
@Getter
@Setter(AccessLevel.PRIVATE)
@ToString
public class MyEmployee extends Person<String> {

    @Getter
    String sanity = "no";
    @Setter(AccessLevel.PRIVATE)
    boolean testBooleanIsFalse = false;
    private UUID id = UUID.randomUUID();
    private ZonedDateTime anniversary = ZonedDateTime.now();
    @Getter
    private MyEmployee boss = null;
    private String workNickName;
    private IdCard card = null;
    private List<Project> projectList = new ArrayList<>();
    private State employmentState = State.NEVER_EMPLOYED;
    private Optional<Double> weighting = Optional.empty();
    private Optional<Instant> startedAt = Optional.empty();
    private Map<String, Project> projectMap = new HashMap<>();
    private int[] intArray = {0, 1, 2};

    public MyEmployee(@Nonnull String name, long someLongAspect, @Nonnull ZonedDateTime birthday) {
        super(name, someLongAspect, birthday, Optional.of("an optional string"), "another string");
    }

    /**
     * Create collision with {@link #getBoss} derived Subject methods.
     */
    public boolean isBoss() {
        return getEmploymentState() == IS_A_BOSS;
    }

    public Person toPlainPerson() {
        return Person.builder().birthday(birthday).name(name).someLongAspect(someLongAspect).build();
    }

    /**
     * I know - the model doesn't need to make sense :)
     */
    public State[] toStateArray() {
        return new State[0];
    }

    public Object[] toProjectObjectArray() {
        return projectList.toArray();
    }

    /**
     * Package-private test
     */
    boolean isEmployed() {
        return this.employmentState == EMPLOLYED;
    }

    /**
     * Primitive vs Wrapper test
     */
    Boolean isEmployedWrapped() {
        return this.employmentState == EMPLOLYED;
    }

    /**
     * Overriding support in Subject
     */
    @Override
    public String getName() {
        return super.getName() + " ID: " + this.getId();
    }

    /**
     * A "getter" method that doesn't use the get prefix
     */
    public int legacyAccessMethod() {
        return 1;
    }

//  @Override
//  public String toString() {
//    return "MyEmployee{" +
//            ", name=" + getName() +
//            ", card=" + card +
//            ", employed=" + employmentState +
//            '}';
//  }

    public enum State {
        EMPLOLYED, PREVIOUSLY_EMPLOYED, NEVER_EMPLOYED, IS_A_BOSS
    }

}
