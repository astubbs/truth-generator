package io.stubbs.truth.generator;

import io.stubbs.truth.generator.testModel.IdCard;
import io.stubbs.truth.generator.testModel.MyEmployee;
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
public class TestModelUtils {
    static private final PodamFactory factory = new PodamFactoryImpl();

    public static MyEmployee createEmployee() {
        MyEmployee.MyEmployeeBuilder<?, ?> employee = factory.manufacturePojo(MyEmployee.class).toBuilder();
        employee.anniversary(ZonedDateTime.now().withYear(1983));
        MyEmployee boss = factory.manufacturePojo(MyEmployee.class).toBuilder().name("Lilan").build();
        employee = employee
                .boss(boss)
                .card(createCard());
        return employee.build();
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

}
