package io.stubbs.truth.generator.internal.model;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.internal.TruthGeneratorRuntimeException;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.model.ValuePair;
import org.jboss.forge.roaster.model.impl.AnnotationImpl;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.Import;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.util.List;
import java.util.Optional;

import static io.stubbs.truth.generator.internal.Utils.msg;

/**
 * A {@link MiddleClass} which is provided from discovered source code (not a compiled class from class path or from
 * source code we generated). I.e. the main way users will supply their own {@link Subject}s into the grpah.
 *
 * @see UserManagedTruth
 */
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString
public class UserSourceCodeManagedMiddleClass<T> extends RoasterMiddleClass<T> implements MiddleClass<T> {

    public UserSourceCodeManagedMiddleClass(JavaClassSource sourceCodeModel, MethodSource factory) {
        super(sourceCodeModel, factory);
    }

    @Override
    public String getClassUnderTestSimpleName() {
        AnnotationSource<JavaClassSource> userManaged = super.sourceCodeModel.getAnnotation(UserManagedTruth.class);
        final AnnotationImpl userManaged1 = (AnnotationImpl) userManaged;
        final AnnotationSource annotationValue = userManaged1.getAnnotationValue();
        final Object internal = userManaged1.getInternal();
        final String underTest = userManaged.getStringValue();
        final Import anImport = sourceCodeModel.getImport(underTest);

        final List<Import> imports = sourceCodeModel.getImports();
        final String anObject = StringUtils.removeEnd(underTest, ".class");
        final Optional<Import> first = imports.stream().filter(x -> x.getSimpleName().equals(anObject)).findFirst();

        final List<ValuePair> values = userManaged.getValues();

        final String literalValue = userManaged.getLiteralValue();

//        userManaged1.
//        sourceCodeModel.
        final Class<?> classValue = userManaged.getClassValue();

        if (StringUtils.countMatches(literalValue, ".") > 1)
            return anObject;
        else if (first.isEmpty())
            throw new TruthGeneratorRuntimeException(msg("Cannot resovle type {}", literalValue));
        else
            return first.get().getQualifiedName();

//        return underTest;
    }
}
