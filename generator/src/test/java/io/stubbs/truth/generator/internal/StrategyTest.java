package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.TestModelUtils;
import io.stubbs.truth.generator.internal.model.MiddleClass;
import io.stubbs.truth.generator.internal.model.ParentClass;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.testModel.MyEmployee;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;

public class StrategyTest {

    JavaClassSource generated = Roaster.create(JavaClassSource.class);
    MyEmployee employee = TestModelUtils.createEmployee();
    Class<? extends MyEmployee> employeeClass = employee.getClass();

    static {
        Options.setDefaultInstance();
    }

    protected ThreeSystem createThreeSystem(Class<?> employeeClass) {
        return new ThreeSystem(employeeClass, new ParentClass(Roaster.create(JavaClassSource.class)), MiddleClass.of(employeeClass), generated);
    }

}
