package io.stubbs.truth.generator;

import io.stubbs.truth.generator.testModel.IdCard;
import io.stubbs.truth.generator.testModel.MyEmployee;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.time.ZonedDateTime;

import static io.stubbs.truth.generator.testModel.IdCard.SecurityType.Type.FOB;

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

}
