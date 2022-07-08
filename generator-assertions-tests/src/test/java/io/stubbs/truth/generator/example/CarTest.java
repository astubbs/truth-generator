package io.stubbs.truth.generator.example;

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
        assertThat(car).hasColourId().isEqualTo(0);
    }

}
