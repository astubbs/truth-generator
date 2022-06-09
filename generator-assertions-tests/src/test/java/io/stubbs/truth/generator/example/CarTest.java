package io.stubbs.truth.generator.example;

import org.junit.Test;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import static io.stubbs.truth.tests.ManagedTruth.assertThat;

public class CarTest {

    PodamFactory factory = new PodamFactoryImpl();
    Car car = factory.manufacturePojoWithFullData(Car.class);

    @Test
    public void test() {
        assertThat(car).hasColourId().isEqualTo(0);
    }

}
