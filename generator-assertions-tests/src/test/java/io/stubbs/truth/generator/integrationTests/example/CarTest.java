package io.stubbs.truth.generator.integrationTests.example;

import io.stubbs.truth.generator.example.Car;
import io.stubbs.truth.generator.integrationTests.ManagedTruth;
import org.junit.ComparisonFailure;
import org.junit.Test;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;


/**
 * @author Antony Stubbs
 */
public class CarTest {

    PodamFactory factory = new PodamFactoryImpl();
    Car car = factory.manufacturePojoWithFullData(Car.class);

    @Test(expected = ComparisonFailure.class)
    public void test() {
        ManagedTruth.assertThat(car).hasColourId().isEqualTo(0);
    }

}
