package io.stubbs.truth.generator;

import io.stubbs.truth.generator.internal.model.GeneratedMiddleClass;
import io.stubbs.truth.generator.internal.model.ParentClass;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.testModel.IdCard;
import io.stubbs.truth.generator.testModel.MyEmployee;
import lombok.experimental.UtilityClass;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.stubbs.truth.generator.testModel.IdCard.SecurityType.Type.FOB;
import static java.util.stream.Collectors.toList;

/**
 * @author Antony Stubbs
 */
@UtilityClass
public class TestModelUtils {

    private static final PodamFactory factory = new PodamFactoryImpl();

    public static MyEmployee createEmployee() {
        MyEmployee employeesBoss = factory.manufacturePojo(MyEmployee.class)
                .toBuilder()
                .name("boss-employee")
                .employmentState(MyEmployee.State.IS_A_BOSS)
                .build();

        var nonBossEmployee = factory.manufacturePojo(MyEmployee.class).toBuilder()
                .anniversary(ZonedDateTime.now().withYear(1983))
                .employmentState(MyEmployee.State.EMPLOLYED)
                .name("employee-one (not a boss)")
                .boss(employeesBoss)
                .card(createCard());

        return nonBossEmployee.build();
    }

    public static IdCard createCard() {
        IdCard idCard = factory.manufacturePojoWithFullData(IdCard.class);
        return idCard.toBuilder()
                .name("special-card-x")
                .epoch(4)
                .primarySecurityType(new IdCard.SecurityType(FOB))
                .build();
    }

    public static <T> T createInstance(Class<T> type) {
        return factory.manufacturePojoWithFullData(type);
    }


    public static MethodSource<JavaClassSource> findMethodWithNoParamsRoast(JavaClassSource generatedParent, String name) {
        List<MethodSource<JavaClassSource>> collect = generatedParent.getMethods().stream().filter(x -> x.getName().equals(name)).collect(toList());
        assertThat(collect).hasSize(1);
        return collect.get(0);
    }


    public static <T> Method findMethodWithNoParamsJReflect(Class<T> classType, String methodName) {
        return Arrays.stream(classType.getMethods()).filter(x -> x.getName().equals(methodName)).findFirst().get();
    }

    public static ThreeSystem<MyEmployee> createThreeSystem() {
        var employeeClass = MyEmployee.class;
        var parent = new ParentClass(Roaster.create(JavaClassSource.class));
        var middleGen = Roaster.create(JavaClassSource.class);
        var factoryMethod = middleGen.addMethod();
        var middle = new GeneratedMiddleClass<>(middleGen, factoryMethod, employeeClass);
        var childGen = Roaster.create(JavaClassSource.class);
        return new ThreeSystem<>(employeeClass, parent, middle, childGen);
    }

}
