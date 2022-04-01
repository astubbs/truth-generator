package io.stubbs.truth.generator;

import io.stubbs.truth.generator.testModel.IdCard;
import io.stubbs.truth.generator.testModel.MyEmployee;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryMethod;
import org.eclipse.jdt.internal.compiler.env.IBinaryTypeAnnotation;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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


    public static <T> Method findMethodWithNoParamsJRflect(Class<T> classType, String methodName) {
        return Arrays.stream(classType.getMethods()).filter(x -> x.getName().equals(methodName)).findFirst().get();
    }

    public static Optional<MethodInfo> findMethodWithNoParamsJA(ClassFile classRepresentation, String name) {
        return findMethodJA(classRepresentation, name, 0);
    }

    public static Optional<MethodInfo> findMethodJA(ClassFile classRepresentation, String name, int paramCount) {
        List<MethodInfo> methods = classRepresentation.getMethods();
        return methods.stream().filter(x -> {
            if (x.getName().equals(name)) {

                List<AttributeInfo> attributes = x.getAttributes();

                if (attributes.isEmpty() && paramCount == 0) {
                    return true;
                }

                String descriptor = x.getDescriptor();
                String params = StringUtils.substringBetween(descriptor, "(", ")");
                int hackyParamCount = StringUtils.countMatches(params, ';');
                return hackyParamCount == paramCount;

            }
            return false;
        }).findFirst();
    }

    public static Optional<IBinaryMethod> findMethodWithNoParamsEclipse(ClassFileReader classRepresentation, String methodName) {
        IBinaryMethod[] methods = classRepresentation.getMethods();

        return Arrays.stream(methods).filter(x1 -> {

            char[] selector = x1.getSelector();
            String s1 = String.valueOf(selector);


            if (s1.equals(methodName)) {
                char[] methodDescriptor = x1.getMethodDescriptor();
                IBinaryAnnotation[] annotations = x1.getAnnotations();
                char[][] exceptionTypeNames = x1.getExceptionTypeNames();
                char[] genericSignature = x1.getGenericSignature();
                long tagBits = x1.getTagBits();
                char[][] argumentNames = x1.getArgumentNames();
                Object defaultValue = x1.getDefaultValue();
                IBinaryTypeAnnotation[] typeAnnotations = x1.getTypeAnnotations();


                return s1.equals(methodName) && x1.getAnnotatedParametersCount() == 0;
            }

            return false;

        }).findFirst();

    }
}
