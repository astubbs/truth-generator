package io.stubbs.truth.generator;

import io.stubbs.truth.generator.example.Car;
import org.junit.Test;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import static io.stubbs.truth.generator.example.Car.Make.PLASTIC;
import static io.stubbs.truth.tests.ManagedTruth.assertThat;

public class CarTest {
    @Test
    public void example() {
        Car car = new PodamFactoryImpl().manufacturePojoWithFullData(Car.class);
        car.setColourId(5);

        assertThat(car).hasColourId().isAtLeast(8);
        assertThat(car).hasName().ignoringTrailingWhiteSpace().equalTo("sally");
        assertThat(car).hasMakeEqualTo(PLASTIC);
    }
}